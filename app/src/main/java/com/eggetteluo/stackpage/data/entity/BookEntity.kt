package com.eggetteluo.stackpage.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 书籍基本信息表
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,               // 书名
    val author: String? = "未知",     // 作者
    val filePath: String,            // 文件路径 (Uri string)
    val coverPath: String? = null,   // 封面图片路径
    val format: String,              // "txt", "epub"
    val encoding: String = "UTF-8",  // 编码格式
    val addTime: Long = System.currentTimeMillis()
)
