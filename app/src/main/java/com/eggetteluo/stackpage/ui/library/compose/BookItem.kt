package com.eggetteluo.stackpage.ui.library.compose

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.eggetteluo.stackpage.data.entity.BookWithProgress
import com.eggetteluo.stackpage.util.BookColorUtils

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BookItem(item: BookWithProgress, onClick: () -> Unit) {
    // 1. 进度计算逻辑优化
    val progressRatio = remember(item.progress, item.book.totalChapters) {
        val total = item.book.totalChapters
        val current = item.progress?.lastChapterIndex ?: -1

        when {
            // 如果没有进度记录或者总章节为0，显示0%
            item.progress == null || total <= 0 -> 0f
            // 如果当前索引是最后一章，或者超过了，显示100% (可选)
            else -> (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }
    }

    val progressPercent = (progressRatio * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 封面图层
            val coverPath = item.book.coverPath
            if (!coverPath.isNullOrEmpty()) {
                SubcomposeAsyncImage(
                    model = coverPath,
                    contentDescription = item.book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { BookPlaceholder(item.book.title) },
                    error = { BookPlaceholder(item.book.title) }
                )
            } else {
                BookPlaceholder(item.book.title)
            }

            // 底部文字保护遮罩 (增强对比度)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 350f
                        )
                    )
            )

            // 信息展示层
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = item.book.title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        shadow = androidx.compose.ui.graphics.Shadow(blurRadius = 4f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // --- 三段式状态显示 ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (statusText, statusColor) = when {
                        progressPercent == 0 -> {
                            "未读" to Color.White.copy(alpha = 0.7f)
                        }

                        progressPercent >= 100 -> {
                            "已读完" to MaterialTheme.colorScheme.primaryContainer
                        }

                        else -> {
                            "已读 $progressPercent%" to Color.White.copy(alpha = 0.9f)
                        }
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = if (progressPercent >= 100) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 进度条
                LinearProgressIndicator(
                    progress = { progressRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * 无封面时的默认占位 UI
 */
@Composable
fun BookPlaceholder(title: String) {
    val backgroundColor = remember(title) {
        BookColorUtils.getBackgroundColorForBook(title)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Book,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = Color.Black.copy(alpha = 0.15f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
                color = Color.Black.copy(alpha = 0.6f)
            ),
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}