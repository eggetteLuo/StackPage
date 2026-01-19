package com.eggetteluo.stackpage.util

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

object BookColorUtils {
    // 预设一组柔和的莫兰迪色系
    private val placeholderColors = listOf(
        Color(0xFFEF9A9A), // 浅红
        Color(0xFF90CAF9), // 浅蓝
        Color(0xFFA5D6A7), // 浅绿
        Color(0xFFFFF59D), // 浅黄
        Color(0xFFB39DDB), // 浅紫
        Color(0xFFFFCC80), // 浅橙
        Color(0xFF80CBC4), // 蓝绿
        Color(0xFFCE93D8), // 淡紫
        Color(0xFFBCAAA4), // 浅褐
        Color(0xFFB0BEC5)  // 蓝灰
    )

    /**
     * 根据书名生成固定的随机颜色
     */
    fun getBackgroundColorForBook(title: String): Color {
        if (title.isEmpty()) return placeholderColors[0]
        // 使用 hashCode 取模，确保同一本书永远对应同一种颜色
        val index = abs(title.hashCode()) % placeholderColors.size
        return placeholderColors[index]
    }
}