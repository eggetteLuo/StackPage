package com.eggetteluo.stackpage.ui.reader

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eggetteluo.stackpage.ui.reader.compose.ChapterItem
import com.eggetteluo.stackpage.ui.reader.compose.FooterLoadingView
import com.eggetteluo.stackpage.ui.reader.compose.MenuOverlay
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

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

    val listState = rememberLazyListState()
    val context = LocalContext.current
    val window = (context as? Activity)?.window
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 状态锁，用于初始位置恢复和跳转定位
    val hasRestoredPosition = remember(bookId) { mutableStateOf(false) }

    // 状态栏控制器
    val controller = remember {
        window?.let { WindowCompat.getInsetsController(it, view) }
    }

    // --- 定位恢复逻辑 ---
    LaunchedEffect(uiState) {
        val state = uiState as? ReaderUiState.Success ?: return@LaunchedEffect
        if (!hasRestoredPosition.value) {
            val targetIdx = state.chapters.indexOfFirst { it.index == state.currentChapterIndex }
            if (targetIdx != -1) {
                listState.scrollToItem(targetIdx, state.initialScrollPos)
                hasRestoredPosition.value = true
            }
        }
    }

    // --- 预加载与同步监听 ---
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@snapshotFlow null
            val state = uiState as? ReaderUiState.Success ?: return@snapshotFlow null

            // 获取视觉中心章节 Key
            val activeItem = visibleItems.maxByOrNull { item ->
                val start = maxOf(item.offset, 0)
                val end = minOf(item.offset + item.size, layoutInfo.viewportEndOffset)
                end - start
            }
            val currentReadingIdx = activeItem?.key as? Int

            // 加载判定逻辑
            val lastItem = visibleItems.last()
            val remainingBottom = (lastItem.offset + lastItem.size) - layoutInfo.viewportEndOffset
            val shouldLoadNext = lastItem.index >= state.chapters.size - 1 && remainingBottom < 1500
            val shouldLoadPrev =
                visibleItems.first().index == 0 && visibleItems.first().offset > -1000

            Triple(currentReadingIdx, shouldLoadNext, shouldLoadPrev)
        }
            .distinctUntilChanged()
            .collect { result ->
                val (activeIdx, loadNext, loadPrev) = result ?: return@collect
                val state = uiState as? ReaderUiState.Success ?: return@collect

                activeIdx?.let { viewModel.updateCurrentActiveChapter(it) }

                if (loadNext) {
                    state.chapters.lastOrNull()?.let {
                        if (it.index < state.totalChapters - 1) viewModel.loadChapter(
                            it.index + 1,
                            isAppend = true
                        )
                    }
                }

                if (loadPrev) {
                    state.chapters.firstOrNull()?.let {
                        if (it.index > 0) viewModel.loadPrependChapter(it.index - 1)
                    }
                }
            }
    }

    // --- 沉浸式 UI 控制 ---
    LaunchedEffect(showMenu) {
        controller?.let {
            if (showMenu) it.show(WindowInsetsCompat.Type.systemBars())
            else {
                it.hide(WindowInsetsCompat.Type.statusBars())
                it.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    // 保存进度逻辑
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                saveCurrentProgress(uiState, listState, viewModel)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            saveCurrentProgress(uiState, listState, viewModel)
            controller?.show(WindowInsetsCompat.Type.statusBars())
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // --- 监听滚动即隐藏菜单 ---
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && showMenu) {
            showMenu = false
        }
    }

    // --- 界面布局 ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerBgColor)
    ) {
        // 阅读内容层
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
                is ReaderUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ReaderUiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        items(state.chapters, key = { it.index }) { chapter ->
                            ChapterItem(chapter, readerTextColor)
                        }

                        // 底部加载状态
                        item(key = "FOOTER") {
                            FooterLoadingView(
                                isLastChapter = state.chapters.lastOrNull()?.index == state.totalChapters - 1,
                                textColor = readerTextColor
                            )
                        }
                    }
                }

                is ReaderUiState.Error -> Text(
                    state.message,
                    Modifier.align(Alignment.Center),
                    color = Color.Red
                )
            }
        }

        // 菜单层
        MenuOverlay(
            isVisible = showMenu,
            title = (uiState as? ReaderUiState.Success)?.currentTitle ?: "正在加载",
            uiState = uiState,
            onBack = onBack,
            onToggleChapter = { targetIndex ->
                hasRestoredPosition.value = false // 重置定位标记
                viewModel.loadChapter(targetIndex, scrollPos = 0, isAppend = false)
            },
            onMenuToggle = { showMenu = it }
        )
    }
}

private fun saveCurrentProgress(
    uiState: ReaderUiState,
    listState: LazyListState,
    viewModel: ReaderViewModel
) {
    val state = uiState as? ReaderUiState.Success ?: return
    val currentKey = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.key as? Int
        ?: state.currentChapterIndex
    viewModel.saveReadingProgress(currentKey, listState.firstVisibleItemScrollOffset)
}