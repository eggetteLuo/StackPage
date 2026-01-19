package com.eggetteluo.stackpage.ui.reader.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 列表底部视图
 */
@Composable
fun FooterLoadingView(isLastChapter: Boolean, textColor: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(48.dp), contentAlignment = Alignment.Center
    ) {
        if (isLastChapter) {
            Text(
                "— 全书完 —",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(0.3f)
            )
        } else {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}