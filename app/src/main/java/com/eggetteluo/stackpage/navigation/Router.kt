package com.eggetteluo.stackpage.navigation

import kotlinx.serialization.Serializable

/**
 * 定义书架页路由
 * 使用 object 因为书架页不需要参数
 */
@Serializable
object Library

/**
 * 定义阅读页路由
 * 使用 data class 因为需要传递参数
 * @param bookId 书籍在数据库中的唯一 ID
 */
@Serializable
data class Reader(val bookId: Long)