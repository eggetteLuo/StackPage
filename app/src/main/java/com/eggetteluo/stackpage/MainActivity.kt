package com.eggetteluo.stackpage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.eggetteluo.stackpage.ui.StackPageApp
import com.eggetteluo.stackpage.ui.library.LibraryViewModel
import com.eggetteluo.stackpage.ui.theme.StackPageTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val libraryViewModel: LibraryViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            StackPageTheme {
                StackPageApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        val data: Uri? = intent.data

        // 检查是否是查看文件的请求
        if (Intent.ACTION_VIEW == action && data != null) {
            libraryViewModel.importBook(applicationContext, data)
        }
    }
}