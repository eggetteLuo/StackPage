package com.eggetteluo.stackpage.ui.reader.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eggetteluo.stackpage.ui.reader.ReaderUiState

/**
 * 阅读器菜单覆盖层：包含顶部导航和底部控制菜单
 * @param isVisible 是否显示菜单
 * @param title 顶部显示的章节标题
 * @param uiState 当前阅读器的 UI 状态
 * @param onBack 点击返回
 * @param onToggleChapter 切换章节回调
 * @param onMenuToggle 用于点击空白处或物理返回键关闭菜单
 */
@Composable
fun MenuOverlay(
    isVisible: Boolean,
    title: String,
    uiState: ReaderUiState,
    onBack: () -> Unit,
    onToggleChapter: (Int) -> Unit,
    onMenuToggle: (Boolean) -> Unit
) {
    val readerBgColor = Color(0xFFF4ECD8)

    // 处理物理返回键：如果菜单开启，点击返回键先关闭菜单
    BackHandler(isVisible) {
        onMenuToggle(false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- 顶部导航栏 ---
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = readerBgColor.copy(alpha = 0.98f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }
        }

        // --- 底部控制栏 ---
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = readerBgColor.copy(alpha = 0.98f),
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    BottomMenu(
                        uiState = uiState,
                        onToggleChapter = { targetIndex ->
                            onToggleChapter(targetIndex)
                        }
                    )
                }
            }
        }
    }
}