package com.eggetteluo.stackpage.data.entity

import androidx.room.Database
import androidx.room.RoomDatabase
import com.eggetteluo.stackpage.data.dao.ReadingDao

@Database(
    entities = [BookEntity::class, ChapterEntity::class, ProgressEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun readingDao(): ReadingDao
}