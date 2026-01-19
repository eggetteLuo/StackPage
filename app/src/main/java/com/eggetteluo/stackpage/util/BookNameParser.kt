package com.eggetteluo.stackpage.util

object BookNameParser {
    fun parseTitle(fileName: String): String {
        // 去掉路径，只留文件名
        val nameWithoutPath = fileName.substringAfterLast("/")

        // 正则表达式解释：
        // 《(.+?)》  -> 匹配中文书名号
        // |          -> 或者
        // \"(.+?)\"  -> 匹配英文双引号 (需要转义)
        // |          -> 或者
        // \[(.+?)\]  -> 匹配英文中括号
        val regex = Regex("《(.+?)》|\"(.+?)\"|\\[(.+?)\\]")
        val matchResult = regex.find(nameWithoutPath)

        return if (matchResult != null) {
            // matchResult.groupValues 会包含所有捕获组
            // 我们需要找到那个不为 null 且不为空的组
            val extracted = matchResult.groupValues.drop(1).firstOrNull { it.isNotBlank() }
            extracted?.trim() ?: fallbackName(nameWithoutPath)
        } else {
            fallbackName(nameWithoutPath)
        }
    }

    private fun fallbackName(fileName: String): String {
        // 如果没有任何匹配，去掉后缀并返回
        return fileName.substringBeforeLast(".").trim()
    }
}