package com.eggetteluo.stackpage.data.dao

import androidx.room.*
import com.eggetteluo.stackpage.data.entity.BookEntity
import com.eggetteluo.stackpage.data.entity.BookWithProgress
import com.eggetteluo.stackpage.data.entity.ChapterEntity
import com.eggetteluo.stackpage.data.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {

    // --- 书籍基本操作 (BookEntity) ---

    /**
     * 插入新书籍。如果书籍已存在（冲突），则覆盖原数据。
     * @return 返回插入成功的行 ID。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    /**
     * 获取带阅读进度的书籍列表。
     * 排序规则：优先按最后阅读时间降序，其次按添加时间降序（最近读过的在前）。
     * @Transaction 确保关联查询（Book + Progress）的原子性。
     */
    @Transaction
    @Query("SELECT * FROM books ORDER BY lastReadTime DESC, addTime DESC")
    fun getAllBooksWithProgress(): Flow<List<BookWithProgress>>

    /**
     * 根据书籍 ID 查询单本书籍详情。
     */
    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): BookEntity?

    /**
     * 更新书籍信息。常用于更新总章节数、文件路径或元数据。
     */
    @Update
    suspend fun updateBook(book: BookEntity)

    /**
     * 根据文件名和文件大小查找书籍。常用于导入时判断书籍是否已存在，避免重复导入。
     */
    @Query("SELECT * FROM books WHERE title = :title AND size = :size LIMIT 1")
    suspend fun findBookByNameAndSize(title: String, size: Long): BookEntity?

    /**
     * 从数据库中删除书籍记录。由于配置了 ForeignKey.CASCADE，相关的章节和进度会自动删除。
     */
    @Delete
    suspend fun deleteBook(book: BookEntity)

    // --- 章节解析与操作 (ChapterEntity) ---

    /**
     * 批量插入章节信息（通常在首次解析 TXT 文件后调用）。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    /**
     * 根据书籍 ID 获取该书的所有章节列表，按章节序号升序排列。
     */
    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    suspend fun getChaptersByBook(bookId: Long): List<ChapterEntity>

    // --- 阅读进度管理 (ProgressEntity) ---

    /**
     * 保存或更新阅读进度（记录当前读到的章节索引和字节偏移量）。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ProgressEntity)

    /**
     * 根据书籍 ID 获取单本书及其对应的阅读进度。
     */
    @Transaction
    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookWithProgressById(bookId: Long): BookWithProgress?

    /**
     * 快速更新书籍的最后阅读时间戳。
     * 每次关闭阅读器或切换章节时调用，用于书架的“最近阅读”排序。
     */
    @Query("UPDATE books SET lastReadTime = :timestamp WHERE id = :bookId")
    suspend fun updateLastReadTime(bookId: Long, timestamp: Long)
}