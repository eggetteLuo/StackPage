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

    // 已加载章节
    private val loadedChapters = mutableListOf<ChapterContent>()

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
            loadChapter(startChapterIndex, startPosition, isAppend = false)
        }
    }

    /**
     * 加载指定索引的章节内容
     * @param index 章节索引
     * @param scrollPos 初始滚动到的像素位置，默认为 0（切换章节时使用）
     * @param isAppend true 表示拼接在后面（无限滚动），false 表示替换全部（跳转章节）
     */
    fun loadChapter(index: Int, scrollPos: Int = 0, isAppend: Boolean = false) {
        if (index !in allChapters.indices) return

        // 防止重复加载
        if (isAppend && loadedChapters.any { it.index == index }) return

        viewModelScope.launch(Dispatchers.IO) {
            val book = currentBook ?: return@launch
            val chapterMetadata = allChapters[index]

            try {
                val text = readTextFromFile(book, chapterMetadata)
                val newChapter = ChapterContent(index, chapterMetadata.title, text)

                if (isAppend) {
                    loadedChapters.add(newChapter)
                } else {
                    loadedChapters.clear()
                    loadedChapters.add(newChapter)
                }

                _uiState.value = ReaderUiState.Success(
                    chapters = loadedChapters.toList(),
                    totalChapters = allChapters.size,
                    currentChapterIndex = index,
                    currentTitle = newChapter.title,
                    initialScrollPos = scrollPos
                )
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

    /**
     * 保存当前阅读位置
     * @param position 当前滚动的像素值
     */
    fun saveReadingProgress(chapterIndex: Int, position: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.saveProgress(
                ProgressEntity(
                    bookId = bookId,
                    lastChapterIndex = chapterIndex,
                    lastPosition = position.toLong(),
                    updateTime = System.currentTimeMillis()
                )
            )
        }
    }

}

sealed class ReaderUiState {
    object Loading : ReaderUiState()
    data class Success(
        val chapters: List<ChapterContent>,
        val totalChapters: Int,
        val currentChapterIndex: Int, // 当前正在阅读的章节
        val currentTitle: String,     // 顶部栏显示的标题
        val initialScrollPos: Int = 0
    ) : ReaderUiState()

    data class Error(val message: String) : ReaderUiState()
}

data class ChapterContent(
    val index: Int,
    val title: String,
    val content: String
)