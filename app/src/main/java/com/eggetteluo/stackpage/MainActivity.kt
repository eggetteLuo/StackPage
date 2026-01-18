package com.eggetteluo.stackpage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.eggetteluo.stackpage.ui.StackPageApp
import com.eggetteluo.stackpage.ui.theme.StackPageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StackPageTheme {
                StackPageApp()
            }
        }
    }
}