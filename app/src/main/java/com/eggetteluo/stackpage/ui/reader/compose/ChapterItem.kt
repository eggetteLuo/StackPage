package com.eggetteluo.stackpage.ui.reader.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggetteluo.stackpage.ui.reader.ChapterContent

/**
 * 章节内容渲染组件
 */
@Composable
fun ChapterItem(chapter: ChapterContent, textColor: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = chapter.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
            color = textColor.copy(alpha = 0.9f),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 40.dp)
        )
        Text(
            text = chapter.content,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 20.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.5.sp,
                color = textColor
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(80.dp)) // 章节底部分隔
    }
}