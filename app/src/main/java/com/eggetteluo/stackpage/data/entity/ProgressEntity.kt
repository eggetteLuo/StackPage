package com.eggetteluo.stackpage.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 阅读进度表
 */
@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE // 级联删除：书删，进度随之删除
        )
    ],
    indices = [Index("bookId")] // 增加索引，优化关联查询性能
)
data class ProgressEntity(
    @PrimaryKey val bookId: Long,   // 既是主键也是外键
    var lastChapterIndex: Int = 0,  // 上次读到的章节序号
    var lastPosition: Long = 0,     // 章节内的具体偏移（用于精准恢复）
    var updateTime: Long = System.currentTimeMillis()
)