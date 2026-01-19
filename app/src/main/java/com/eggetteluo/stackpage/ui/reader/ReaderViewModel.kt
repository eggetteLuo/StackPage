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

            // 获取进度中的 章节索引 和 滚动像素位置
            val startChapterIndex = bookWithProgress.progress?.lastChapterIndex ?: 0
            val startPosition = bookWithProgress.progress?.lastPosition?.toInt() ?: 0

            // 第一次加载时，传入保存的像素位置
            loadChapter(startChapterIndex, startPosition)
        }
    }

    /**
     * 加载指定索引的章节内容
     * @param scrollPos 初始滚动到的像素位置，默认为 0（切换章节时使用）
     */
    fun loadChapter(index: Int, scrollPos: Int = 0) {
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
                    totalChapters = allChapters.size,
                    initialScrollPos = scrollPos
                )

                // 更新数据库进度
                saveProgress(index, scrollPos.toLong())
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

    private suspend fun saveProgress(chapterIndex: Int, position: Long) {
        dao.saveProgress(
            ProgressEntity(
                bookId = bookId,
                lastChapterIndex = chapterIndex,
                lastPosition = position,
                updateTime = System.currentTimeMillis()
            )
        )
    }

    /**
     * 保存当前阅读位置
     * @param position 当前滚动的像素值
     */
    fun saveReadingProgress(position: Int) {
        val state = _uiState.value
        if (state is ReaderUiState.Success) {
            viewModelScope.launch(Dispatchers.IO) {
                saveProgress(state.chapterIndex, position.toLong())
            }
        }
    }

}

sealed class ReaderUiState {
    object Loading : ReaderUiState()
    data class Success(
        val title: String,
        val content: String,
        val chapterIndex: Int,
        val totalChapters: Int,
        val initialScrollPos: Int
    ) : ReaderUiState()

    data class Error(val message: String) : ReaderUiState()
}