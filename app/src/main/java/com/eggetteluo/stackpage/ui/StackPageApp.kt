package com.eggetteluo.stackpage.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.eggetteluo.stackpage.navigation.Library
import com.eggetteluo.stackpage.navigation.Reader
import com.eggetteluo.stackpage.ui.library.LibraryScreen
import com.eggetteluo.stackpage.ui.reader.ReaderScreen

@Composable
fun StackPageApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Library
    ) {
        // 书架页
        composable<Library>(
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) }
        ) {
            LibraryScreen(
                onNavigateToReader = { id ->
                    navController.navigate(Reader(bookId = id))
                }
            )
        }

        // 阅读页
        composable<Reader>(
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(800)
                ) + fadeIn()
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(800)
                ) + fadeOut()
            }
        ) { backStackEntry ->
            val readerRoute = backStackEntry.toRoute<Reader>()
            ReaderScreen(
                bookId = readerRoute.bookId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}