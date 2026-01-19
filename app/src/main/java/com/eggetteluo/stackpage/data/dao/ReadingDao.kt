package com.eggetteluo.stackpage.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.eggetteluo.stackpage.data.entity.BookEntity
import com.eggetteluo.stackpage.data.entity.BookWithProgress
import com.eggetteluo.stackpage.data.entity.ChapterEntity
import com.eggetteluo.stackpage.data.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {

    // --- 书籍操作 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Query("SELECT * FROM books ORDER BY addTime DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Transaction
    @Query("SELECT * FROM books ORDER BY lastReadTime DESC, addTime DESC")
    fun getAllBooksWithProgress(): Flow<List<BookWithProgress>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): BookEntity?

    /**
     * 更新书籍信息（用于更新总章节数、最后阅读时间等）
     */
    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("SELECT * FROM books WHERE title = :title AND size = :size LIMIT 1")
    suspend fun findBookByNameAndSize(title: String, size: Long): BookEntity?

    @Delete
    suspend fun deleteBook(book: BookEntity)

    // --- 章节操作 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    suspend fun getChaptersByBook(bookId: Long): List<ChapterEntity>

    // --- 进度操作 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ProgressEntity)

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    suspend fun getProgress(bookId: Long): ProgressEntity?

    @Transaction
    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookWithProgressById(bookId: Long): BookWithProgress?

    // 更新书籍的最后阅读时间
    @Query("UPDATE books SET lastReadTime = :timestamp WHERE id = :bookId")
    suspend fun updateLastReadTime(bookId: Long, timestamp: Long)

}