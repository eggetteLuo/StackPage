package com.eggetteluo.stackpage.ui.library.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.eggetteluo.stackpage.data.entity.BookWithProgress
import com.eggetteluo.stackpage.util.BookColorUtils

@Composable
fun BookItem(item: BookWithProgress, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 封面逻辑
            val coverPath = item.book.coverPath
            if (!coverPath.isNullOrEmpty()) {
                SubcomposeAsyncImage(
                    model = coverPath,
                    contentDescription = item.book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = { BookPlaceholder(item.book.title) }
                )
            } else {
                BookPlaceholder(item.book.title)
            }

            // 底部文字遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                            startY = 400f
                        )
                    )
            )

            Text(
                text = item.book.title,
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

/**
 * 无封面时的默认占位 UI
 */
@Composable
fun BookPlaceholder(title: String) {
    // 获取基于书名的固定随机背景色
    val backgroundColor = remember(title) {
        BookColorUtils.getBackgroundColorForBook(title)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Book,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = Color.Black.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
                color = Color.Black.copy(alpha = 0.6f)
            ),
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}