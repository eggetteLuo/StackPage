package com.eggetteluo.stackpage.ui.reader.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eggetteluo.stackpage.ui.reader.ReaderUiState

@Composable
fun BottomMenu(
    uiState: ReaderUiState,
    onToggleChapter: (Int) -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        color = Color(0xFFF4ECD8).copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            if (uiState is ReaderUiState.Success) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "本章：${uiState.currentTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 交互控制行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 上一章按钮
                    TextButton(
                        onClick = { onToggleChapter(uiState.currentChapterIndex - 1) },
                        enabled = uiState.currentChapterIndex > 0
                    ) {
                        Text("上一章")
                    }

                    // 进度文字
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "第 ${uiState.currentChapterIndex + 1} 章",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "共 ${uiState.totalChapters} 章",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }

                    // 下一章按钮
                    TextButton(
                        onClick = { onToggleChapter(uiState.currentChapterIndex + 1) },
                        enabled = uiState.currentChapterIndex < uiState.totalChapters - 1
                    ) {
                        Text("下一章")
                    }
                }
            }
        }
    }
}