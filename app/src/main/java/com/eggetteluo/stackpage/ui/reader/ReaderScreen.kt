package com.eggetteluo.stackpage.ui.reader

import android.app.Activity
import android.util.Log
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.yield
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
    // 控制菜单（顶部栏和底部栏）的显示隐藏
    var showMenu by remember { mutableStateOf(false) }

    // 护眼配色方案
    val readerBgColor = Color(0xFFF4ECD8) // 羊皮纸色
    val readerTextColor = Color(0xFF2C2C2C) // 深灰色文字

    val context = LocalContext.current
    val window = (context as? Activity)?.window
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 滚动状态
    val scrollState = rememberScrollState()

    // 标记位，确保每章只在刚加载时恢复一次位置，防止翻页后又跳回老位置
    var hasRestoredPosition by remember(bookId) { mutableStateOf(false) }

    var triggerScroll by remember { mutableStateOf(false) }

    // 进入阅读页面隐藏状态栏
    DisposableEffect(Unit) {
        window?.let {
            val controller = WindowCompat.getInsetsController(it, view)
            // 隐藏状态栏和导航栏（可选）
            controller.hide(WindowInsetsCompat.Type.statusBars())
            // 设置行为：向内滑动时临时显示，不点击则自动隐藏
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            window?.let {
                val controller = WindowCompat.getInsetsController(it, view)
                // 退出阅读页时务必恢复状态栏显示
                controller.show(WindowInsetsCompat.Type.statusBars())
            }

            // 退出时保存当前滚动位置
            viewModel.saveReadingProgress(scrollState.value)
        }
    }

    // 监听 showMenu 变化来切换状态栏
    LaunchedEffect(showMenu) {
        window?.let {
            val controller = WindowCompat.getInsetsController(it, view)
            if (showMenu) {
                controller.show(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    // 物理返回键处理：如果菜单开着，先关菜单
    BackHandler(showMenu) {
        showMenu = false
    }

    LaunchedEffect(uiState) {
        if (uiState is ReaderUiState.Success) {
            // 每当 uiState 变成 Success，标记需要触发滚动
            triggerScroll = true
        }
    }

    // 处理滚动逻辑
    if (triggerScroll && uiState is ReaderUiState.Success) {
        val state = uiState as ReaderUiState.Success
        LaunchedEffect(state.initialScrollPos, scrollState.maxValue) {
            // 如果是恢复进度
            if (!hasRestoredPosition) {
                if (scrollState.maxValue >= state.initialScrollPos && scrollState.maxValue > 0) {
                    scrollState.scrollTo(state.initialScrollPos)
                    hasRestoredPosition = true
                    triggerScroll = false // 滚动完成，关闭触发器
                }
            } else {
                // 如果是手动切换章节
                scrollState.scrollTo(0)
                triggerScroll = false
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                // 手机切到后台、锁屏、或者弹出多任务时自动保存
                viewModel.saveReadingProgress(scrollState.value)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerBgColor)
    ) {
        // 文本内容
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showMenu = !showMenu
                }
        ) {
            when (val state = uiState) {
                is ReaderUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                is ReaderUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = state.content,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                lineHeight = 30.sp,
                                color = readerTextColor,
                                letterSpacing = 0.5.sp
                            )
                        )

                        // 章节末尾提示
                        Spacer(Modifier.height(32.dp))
                        Text(
                            text = "--- 本章完 ---",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = readerTextColor.copy(alpha = 0.5f)
                        )
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
                    if (uiState is ReaderUiState.Success) {
                        Text(
                            text = (uiState as ReaderUiState.Success).title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                    BottomMenu(uiState, viewModel)
                }
            }
        }
    }
}