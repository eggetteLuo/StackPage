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

/**
 * 阅读器业务逻辑处理类
 */
class ReaderViewModel(
    private val dao: ReadingDao,
    private val bookId: Long
) : ViewModel() {

    private var currentBook: BookEntity? = null
    private var allChapters: List<ChapterEntity> = emptyList()

    // 内存中维护的已加载章节列表（可能是连续的几章）
    private val loadedChapters = mutableListOf<ChapterContent>()

    // 加载锁：防止快速滑动时重复触发加载请求
    private var isFetchingChapter = false

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState

    init {
        initReader()
    }

    /**
     * 初始化阅读器配置，读取书籍信息和历史进度
     */
    private fun initReader() {
        viewModelScope.launch(Dispatchers.IO) {
            val bookWithProgress = dao.getBookWithProgressById(bookId) ?: run {
                _uiState.value = ReaderUiState.Error("书籍不存在")
                return@launch
            }
            currentBook = bookWithProgress.book
            allChapters = dao.getChaptersByBook(bookId)

            if (allChapters.isEmpty()) {
                _uiState.value = ReaderUiState.Error("章节内容尚未解析")
                return@launch
            }

            // 获取上次阅读的进度
            val startIdx = bookWithProgress.progress?.lastChapterIndex ?: 0
            val startPos = bookWithProgress.progress?.lastPosition?.toInt() ?: 0

            // 加载当前章节
            loadChapter(startIdx, startPos, isAppend = false)

            // 自动静默预加载上一章，让用户往回翻也有内容
            if (startIdx > 0) {
                loadPrependChapter(startIdx - 1)
            }
        }
    }

    /**
     * 加载章节内容
     * @param index 章节全局索引
     * @param scrollPos 初始滚动位置
     * @param isAppend true表示拼接到末尾，false表示清空当前列表并跳转
     */
    fun loadChapter(index: Int, scrollPos: Int = 0, isAppend: Boolean = false) {
        if (index !in allChapters.indices || isFetchingChapter) return
        if (isAppend && loadedChapters.any { it.index == index }) return

        viewModelScope.launch(Dispatchers.IO) {
            isFetchingChapter = true
            try {
                val newChapter = fetchChapterContent(index) ?: return@launch

                if (isAppend) {
                    loadedChapters.add(newChapter)
                } else {
                    loadedChapters.clear()
                    loadedChapters.add(newChapter)
                }

                updateSuccessState(index, newChapter.title, scrollPos)
            } catch (e: Exception) {
                _uiState.value = ReaderUiState.Error("加载失败: ${e.message}")
            } finally {
                isFetchingChapter = false
            }
        }
    }

    /**
     * 专门用于向列表顶部（上方）添加章节
     * @param index 需要加载的前一章索引
     */
    fun loadPrependChapter(index: Int) {
        if (index !in allChapters.indices || isFetchingChapter) return
        if (loadedChapters.any { it.index == index }) return

        viewModelScope.launch(Dispatchers.IO) {
            isFetchingChapter = true
            try {
                fetchChapterContent(index)?.let { newChapter ->
                    // 插入到列表首位
                    loadedChapters.add(0, newChapter)

                    val currentState = _uiState.value as? ReaderUiState.Success
                    if (currentState != null) {
                        _uiState.value = currentState.copy(
                            chapters = loadedChapters.toList()
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFetchingChapter = false
            }
        }
    }

    /**
     * 内部方法：从文件读取并清洗文本
     */
    private fun fetchChapterContent(index: Int): ChapterContent? {
        val book = currentBook ?: return null
        val metadata = allChapters[index]
        val rawText = readTextFromFile(book, metadata)
        val cleanedText = cleanChapterContent(rawText, metadata.title)
        return ChapterContent(index, metadata.title, cleanedText)
    }

    /**
     * 更新UI成功状态的辅助方法
     */
    private fun updateSuccessState(index: Int, title: String, scrollPos: Int) {
        _uiState.value = ReaderUiState.Success(
            chapters = loadedChapters.toList(),
            totalChapters = allChapters.size,
            currentChapterIndex = index,
            currentTitle = title,
            initialScrollPos = scrollPos
        )
    }

    /**
     * 从物理文件读取字节并转码
     */
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
     * 文本清洗：去除首行重复标题，并增加段落缩进
     */
    private fun cleanChapterContent(rawText: String, title: String): String {
        val lines = rawText.lines()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .toMutableList()

        if (lines.isNotEmpty()) {
            val firstLine = lines[0].trim()
            // 如果第一行和章节名高度重合，则视为多余标题，予以移除
            if (firstLine.contains(title) || title.contains(firstLine)) {
                lines.removeAt(0)
            }
        }

        // 使用全角空格缩进，并用双换行分隔段落，提升阅读呼吸感
        return lines.joinToString("\n\n") { "　　${it.trim()}" }
    }

    /**
     * 当UI层滚动监测到视觉重心变化时，更新当前的章节索引和标题
     */
    fun updateCurrentActiveChapter(index: Int) {
        val currentState = _uiState.value as? ReaderUiState.Success ?: return
        if (currentState.currentChapterIndex != index) {
            val activeChapter = currentState.chapters.find { it.index == index }
            if (activeChapter != null) {
                _uiState.value = currentState.copy(
                    currentChapterIndex = index,
                    currentTitle = activeChapter.title
                )
            }
        }
    }

    /**
     * 持久化阅读进度
     */
    fun saveReadingProgress(chapterIndex: Int, position: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            dao.saveProgress(
                ProgressEntity(bookId, chapterIndex, position.toLong(), currentTime)
            )
            dao.updateLastReadTime(bookId, currentTime)
        }
    }
}

/**
 * UI 状态封装
 */
sealed class ReaderUiState {
    object Loading : ReaderUiState()
    data class Success(
        val chapters: List<ChapterContent>, // 已加载的章节数据
        val totalChapters: Int,            // 总章节数
        val currentChapterIndex: Int,      // 当前视觉对应的章节索引
        val currentTitle: String,          // 顶部显示的标题
        val initialScrollPos: Int = 0      // 初始跳转位置
    ) : ReaderUiState()
    data class Error(val message: String) : ReaderUiState()
}

/**
 * 章节内容实体
 */
data class ChapterContent(
    val index: Int,
    val title: String,
    val content: String
)