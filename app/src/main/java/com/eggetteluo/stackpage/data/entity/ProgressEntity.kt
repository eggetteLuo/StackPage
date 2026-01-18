package com.eggetteluo.stackpage.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 阅读进度表
 */
@Entity(tableName = "reading_progress")
data class ProgressEntity(
    @PrimaryKey val bookId: Long,
    var lastChapterIndex: Int = 0,  // 上次读到的章节序号
    var lastPosition: Long = 0,      // 章节内的具体偏移（用于精准恢复）
    var updateTime: Long = System.currentTimeMillis()
)
