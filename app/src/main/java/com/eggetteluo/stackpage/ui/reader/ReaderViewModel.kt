package com.eggetteluo.stackpage.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eggetteluo.stackpage.data.dao.ReadingDao
import com.eggetteluo.stackpage.data.entity.BookEntity
import com.eggetteluo.stackpage.data.entity.ChapterEntity
import com.eggetteluo.stackpage.data.entity.ProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset

class ReaderViewModel(
    private val dao: ReadingDao,
    private val bookId: Long
) : ViewModel() {

    private var currentBook: BookEntity? = null
    private var allChapters: List<ChapterEntity> = emptyList()

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState

    init {
        initReader()
    }

    private fun initReader() {
        viewModelScope.launch(Dispatchers.IO) {
            // 获取书籍信息和进度
            val bookWithProgress = dao.getBookWithProgressById(bookId)
            if (bookWithProgress == null) {
                _uiState.value = ReaderUiState.Error("书籍不存在")
                return@launch
            }
            currentBook = bookWithProgress.book

            // 获取章节索引
            allChapters = dao.getChaptersByBook(bookId)
            if (allChapters.isEmpty()) {
                _uiState.value = ReaderUiState.Error("章节内容尚未解析")
                return@launch
            }

            // 开始加载章节: 优先使用保存的进度，否则从第 0 章开始
            val startChapterIndex = bookWithProgress.progress?.lastChapterIndex ?: 0
            loadChapter(startChapterIndex)
        }
    }

    /**
     * 加载指定索引的章节内容
     */
    fun loadChapter(index: Int) {
        if (index !in allChapters.indices) return

        viewModelScope.launch(Dispatchers.IO) {
            val book = currentBook ?: return@launch
            val chapter = allChapters[index]

            try {
                val content = readTextFromFile(book, chapter)
                _uiState.value = ReaderUiState.Success(
                    title = chapter.title,
                    content = content,
                    chapterIndex = index,
                    totalChapters = allChapters.size
                )

                // 每次成功加载新章节时，顺便更新一下数据库里的进度
                saveProgress(index)

            } catch (e: Exception) {
                _uiState.value = ReaderUiState.Error("读取失败: ${e.message}")
            }
        }
    }

    private fun readTextFromFile(book: BookEntity, chapter: ChapterEntity): String {
        val file = File(book.filePath)
        val length = (chapter.endPos - chapter.startPos).toInt()
        val bytes = ByteArray(length)

        RandomAccessFile(file, "r").use { raf ->
            raf.seek(chapter.startPos)
            raf.readFully(bytes)
        }
        return String(bytes, Charset.forName(book.encoding)).trimStart()
    }

    private suspend fun saveProgress(chapterIndex: Int) {
        dao.saveProgress(
            ProgressEntity(
                bookId = bookId,
                lastChapterIndex = chapterIndex,
                lastPosition = 0, // 暂时存章节首
                updateTime = System.currentTimeMillis()
            )
        )
    }

}

sealed class ReaderUiState {
    object Loading : ReaderUiState()
    data class Success(
        val title: String,
        val content: String,
        val chapterIndex: Int,
        val totalChapters: Int
    ) : ReaderUiState()

    data class Error(val message: String) : ReaderUiState()
}