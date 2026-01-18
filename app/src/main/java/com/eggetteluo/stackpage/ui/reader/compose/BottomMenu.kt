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
import androidx.compose.ui.unit.dp
import com.eggetteluo.stackpage.ui.reader.ReaderUiState
import com.eggetteluo.stackpage.ui.reader.ReaderViewModel

@Composable
fun BottomMenu(uiState: ReaderUiState, viewModel: ReaderViewModel) {
    Surface(
        tonalElevation = 8.dp,
        color = Color(0xFFF4ECD8).copy(alpha = 0.9f)
    ) {
        Column(modifier = Modifier
            .padding(16.dp)
            .navigationBarsPadding()) {
            if (uiState is ReaderUiState.Success) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { viewModel.loadChapter(uiState.chapterIndex - 1) },
                        enabled = uiState.chapterIndex > 0
                    ) {
                        Text("上一章")
                    }

                    Text(
                        "第 ${uiState.chapterIndex + 1} / ${uiState.totalChapters} 章",
                        style = MaterialTheme.typography.labelMedium
                    )

                    TextButton(
                        onClick = { viewModel.loadChapter(uiState.chapterIndex + 1) },
                        enabled = uiState.chapterIndex < uiState.totalChapters - 1
                    ) {
                        Text("下一章")
                    }
                }
            }
        }
    }
}