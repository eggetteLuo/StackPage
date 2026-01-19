package com.eggetteluo.stackpage.data.entity

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.eggetteluo.stackpage.data.dao.ReadingDao

@Database(
    entities = [BookEntity::class, ChapterEntity::class, ProgressEntity::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ],
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun readingDao(): ReadingDao
}