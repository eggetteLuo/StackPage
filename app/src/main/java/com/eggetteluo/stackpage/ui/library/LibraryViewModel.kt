package com.eggetteluo.stackpage.ui.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eggetteluo.stackpage.data.dao.ReadingDao
import com.eggetteluo.stackpage.data.entity.BookEntity
import com.eggetteluo.stackpage.data.entity.BookWithProgress
import com.eggetteluo.stackpage.data.entity.ChapterEntity
import com.eggetteluo.stackpage.data.entity.ProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.universalchardet.UniversalDetector
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class LibraryViewModel(private val dao: ReadingDao) : ViewModel() {

    private val _importState = MutableSharedFlow<ImportUiState>()
    val importState: SharedFlow<ImportUiState> = _importState.asSharedFlow()

    val booksState: StateFlow<List<BookWithProgress>> = dao.getAllBooksWithProgress()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * 删除书籍：同步清理物理文件与数据库记录
     */
    fun deleteBook(book: BookEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // 删除本地物理文件
            val file = File(book.filePath)
            if (file.exists()) file.delete()
            // 删除数据库记录
            dao.deleteBook(book)
        }
    }

    /**
     * 导入书籍流程：拷贝 -> 探测编码 -> 存库 -> 解析章节
     * @param context 用于访问内容解析器和文件目录
     * @param uri 用户选择的外部文件 Uri
     */
    fun importBook(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _importState.emit(ImportUiState.Loading)

                // 获取基础信息
                val fileName =
                    getFileName(context, uri) ?: "Unknown_${System.currentTimeMillis()}.txt"
                val fileSize = getFileSize(context, uri)
                val title = fileName.substringBeforeLast(".")

                // 精准查重逻辑
                val existingBook = dao.findBookByNameAndSize(title, fileSize)
                if (existingBook != null) {
                    _importState.emit(ImportUiState.Error("书籍已存在"))
                    return@launch
                }

                // 物理拷贝
                val internalFile = File(context.filesDir, "imported_books/$fileName")
                if (!internalFile.parentFile!!.exists()) internalFile.parentFile?.mkdirs()
                copyFileToInternal(context, uri, internalFile)

                // 编码探测与元数据准备
                val detectedCharset = detectCharset(internalFile)
                val extension = fileName.substringAfterLast(".", "txt").lowercase()

                // 存入数据库
                val book = BookEntity(
                    title = title,
                    filePath = internalFile.absolutePath,
                    format = extension,
                    encoding = detectedCharset.name(),
                    size = fileSize
                )
                val bookId = dao.insertBook(book)

                // 章节解析 (如果是 TXT)
                if (extension == "txt") {
                    parseTxtChapters(bookId, internalFile, detectedCharset)
                }

                // 初始化进度
                dao.saveProgress(ProgressEntity(bookId = bookId))

                // 发送成功信号
                _importState.emit(ImportUiState.Success(title))

            } catch (e: Exception) {
                e.printStackTrace()
                _importState.emit(ImportUiState.Error("导入失败: ${e.message}"))
            }
        }
    }

    /**
     * 通过 ContentResolver 从 Uri 中查询原始文件名
     */
    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    /**
     * 获取文件大小
     */
    private fun getFileSize(context: Context, uri: Uri): Long {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) cursor.getLong(sizeIndex) else 0L
        } ?: 0L
    }

    /**
     * 物理拷贝：将外部输入流写入内部存储输出流
     */
    private fun copyFileToInternal(context: Context, uri: Uri, destFile: File) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream) // 拷贝流
            }
        }
    }

    /**
     * 精准解析 TXT 章节：基于字节偏移量记录位置
     */
    private suspend fun parseTxtChapters(bookId: Long, file: File, charset: Charset) {
        val chapters = mutableListOf<ChapterEntity>()
        val regex = Regex("""^\s*(第?\s*[0-9一二三四五六七八九十百千万]+\s*[章节回集部卷\s.].*)""")

        withContext(Dispatchers.IO) {
            try {
                // 自动检测换行符长度
                val lineSeparatorLength = detectLineSeparatorLength(file, charset)

                FileInputStream(file).bufferedReader(charset).use { reader ->
                    var currentBytePos = 0L       // 维护全局字节指针
                    var lastChapterTitle = "前言"  // 起始内容默认归入前言
                    var lastChapterStartPos = 0L  // 当前记录章节的起点
                    var chapterIndex = 0

                    reader.lineSequence().forEach { line ->
                        val lineByteLength =
                            line.toByteArray(charset).size.toLong() + lineSeparatorLength

                        if (regex.containsMatchIn(line)) {
                            // 发现新章节：封闭上一个章节的坐标区间
                            if (currentBytePos > 0) {
                                chapters.add(
                                    ChapterEntity(
                                        bookId = bookId,
                                        title = lastChapterTitle,
                                        startPos = lastChapterStartPos,
                                        endPos = currentBytePos, // 本章结束 = 下章起点
                                        chapterIndex = chapterIndex++
                                    )
                                )
                            }
                            lastChapterTitle = line.trim()
                            lastChapterStartPos = currentBytePos
                        }
                        // 累加位置
                        currentBytePos += lineByteLength
                    }

                    // 封闭最后一个章节
                    chapters.add(
                        ChapterEntity(
                            bookId = bookId,
                            title = lastChapterTitle,
                            startPos = lastChapterStartPos,
                            endPos = file.length(),
                            chapterIndex = chapterIndex
                        )
                    )
                }

                // 批量入库，减少事务开销
                if (chapters.isNotEmpty()) {
                    dao.insertChapters(chapters)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 使用 juniversalchardet 库自动探测文本编码
     * 逻辑：取样前 4KB 数据进行统计学特征匹配
     */
    private fun detectCharset(file: File): Charset {
        val detector = UniversalDetector(null)
        val buffer = ByteArray(4096)

        FileInputStream(file).use { fis ->
            var nread: Int
            // 读取文件片段喂给探测器，通常前 4KB 就足够判断
            while (fis.read(buffer).also { nread = it } > 0 && !detector.isDone) {
                detector.handleData(buffer, 0, nread)
            }
        }
        detector.dataEnd()

        val encoding = detector.detectedCharset
        detector.reset()

        return if (encoding != null) {
            try {
                Charset.forName(encoding)
            } catch (_: Exception) {
                StandardCharsets.UTF_8 // 解析异常则降级到 UTF-8
            }
        } else {
            // 无法自动识别时，针对中文 TXT 场景，GBK 通常比 UTF-8 容错率更高
            Charset.forName("GBK")
        }
    }

    /**
     * 辅助函数：通过读取文件开头片段，判断换行符占几个字节
     */
    private fun detectLineSeparatorLength(file: File, charset: Charset): Int {
        return try {
            val buffer = ByteArray(4096) // 读取 4KB 足够判断
            FileInputStream(file).use { it.read(buffer) }
            val content = String(buffer, charset)
            if (content.contains("\r\n")) 2 else 1 // Windows 为 2 (\r\n), Unix 为 1 (\n)
        } catch (_: Exception) {
            1 // 默认 1
        }
    }

}

sealed class ImportUiState {
    object Idle : ImportUiState()
    object Loading : ImportUiState()
    data class Success(val title: String) : ImportUiState()
    data class Error(val message: String) : ImportUiState()
}