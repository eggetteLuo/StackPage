package com.eggetteluo.stackpage.ui.reader

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eggetteluo.stackpage.ui.reader.compose.BottomMenu
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: Long,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = koinViewModel { parametersOf(bookId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    val readerBgColor = Color(0xFFF4ECD8)
    val readerTextColor = Color(0xFF2C2C2C)

    val context = LocalContext.current
    val window = (context as? Activity)?.window
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current


    // 滚动状态与位置恢复标记
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope() // 协程作用域
    val hasRestoredPosition = remember(bookId) { mutableStateOf(false) }

    // 状态栏控制器
    val controller = remember {
        window?.let { WindowCompat.getInsetsController(it, view) }
    }

    // 动态计算当前应该显示的标题
    val currentDisplayTitle by remember {
        derivedStateOf {
            val state = uiState as? ReaderUiState.Success
            if (state != null && state.chapters.isNotEmpty()) {
                val index = listState.firstVisibleItemIndex
                // 容错处理：确保索引不越界
                val safeIndex = index.coerceIn(0, state.chapters.size - 1)
                state.chapters[safeIndex].title
            } else {
                "加载中..."
            }
        }
    }

    // 首次进入位置恢复逻辑
    LaunchedEffect(uiState) {
        if (uiState is ReaderUiState.Success && !hasRestoredPosition.value) {
            val state = uiState as ReaderUiState.Success
            // LazyColumn 恢复：第一个 item 是当前的章节，offset 是像素偏移
            if (state.initialScrollPos > 0) {
                listState.scrollToItem(0, state.initialScrollPos)
            }
            hasRestoredPosition.value = true
        }
    }

    // 监听菜单显示状态栏
    LaunchedEffect(showMenu) {
        controller?.let {
            if (showMenu) {
                // 显示状态栏和导航栏
                it.show(WindowInsetsCompat.Type.systemBars())
            } else {
                // 隐藏状态栏
                it.hide(WindowInsetsCompat.Type.statusBars())
                // 行为：滑动时临时显示
                it.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    // 沉浸式状态栏处理
    DisposableEffect(Unit) {
        onDispose {
            controller?.show(WindowInsetsCompat.Type.statusBars())
            // 退出时保存位置
            saveCurrentProgress(uiState, listState, viewModel)
        }
    }

    // 生命周期自动保存
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                saveCurrentProgress(uiState, listState, viewModel)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 监听物理返回键，如果菜单开着先关闭菜单
    BackHandler(showMenu) {
        showMenu = false
    }

    // UI 布局
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerBgColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showMenu = !showMenu }
        ) {
            when (val state = uiState) {
                is ReaderUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                is ReaderUiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        items(state.chapters, key = { it.index }) { chapter ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // 章节标题（流式阅读中可选，根据个人喜好）
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = readerTextColor.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(vertical = 24.dp)
                                )
                                Text(
                                    text = chapter.content,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 18.sp,
                                        lineHeight = 32.sp,
                                        color = readerTextColor,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Spacer(Modifier.height(48.dp))
                                Text(
                                    "--- ${chapter.title} 完 ---",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = readerTextColor.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        // 底部加载占位符
                        item {
                            val lastChapter = state.chapters.lastOrNull()
                            if (lastChapter != null && lastChapter.index < state.totalChapters - 1) {
                                // 使用 SideEffect 触发加载，确保在绘制时执行
                                LaunchedEffect(lastChapter.index) {
                                    viewModel.loadChapter(lastChapter.index + 1, isAppend = true)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            } else {
                                Text(
                                    "全书完",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = readerTextColor.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }

                is ReaderUiState.Error -> {
                    Text(state.message, Modifier.align(Alignment.Center), color = Color.Red)
                }
            }
        }

        // 顶部返回栏
        AnimatedVisibility(
            visible = showMenu,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = readerBgColor.copy(alpha = 0.95f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding() // 自动处理状态栏高度
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                    Text(
                        text = currentDisplayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 底部控制栏
        AnimatedVisibility(
            visible = showMenu,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = readerBgColor.copy(alpha = 0.95f),
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    BottomMenu(
                        uiState = uiState,
                        onToggleChapter = { targetIndex ->
                            // 点击菜单跳转时，isAppend 传 false，表示重新开启一段流
                            viewModel.loadChapter(targetIndex, scrollPos = 0, isAppend = false)
                            // 同时让列表滚回顶部
                            scope.launch { listState.scrollToItem(0) }
                        }
                    )
                }
            }
        }
    }
}

// 提取保存进度的公共方法
private fun saveCurrentProgress(
    uiState: ReaderUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    viewModel: ReaderViewModel
) {
    val state = uiState as? ReaderUiState.Success ?: return
    val visibleItems = listState.layoutInfo.visibleItemsInfo
    if (visibleItems.isNotEmpty()) {
        val firstVisible = visibleItems.first()
        // 映射回原始章节索引
        val chapterIdx = state.chapters[firstVisible.index].index
        viewModel.saveReadingProgress(chapterIdx, listState.firstVisibleItemScrollOffset)
    }
}