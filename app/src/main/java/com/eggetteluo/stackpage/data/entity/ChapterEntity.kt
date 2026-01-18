package com.eggetteluo.stackpage.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 章节索引表：记录章节在文件中的物理偏移量
 */
@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val chapterId: Long = 0,
    val bookId: Long,
    val title: String,          // 章节名
    val startPos: Long,         // 字节起始位置
    val endPos: Long,           // 字节结束位置
    val chapterIndex: Int       // 排序序号
)