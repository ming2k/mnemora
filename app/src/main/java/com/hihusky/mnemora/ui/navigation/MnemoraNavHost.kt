package com.hihusky.mnemora.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hihusky.mnemora.ui.screens.bookdetail.BookDetailScreen
import com.hihusky.mnemora.ui.screens.collectiondetail.CollectionDetailScreen
import com.hihusky.mnemora.ui.screens.home.HomeScreen
import com.hihusky.mnemora.ui.screens.practice.PracticeScreen
import com.hihusky.mnemora.ui.screens.review.ReviewScreen
import com.hihusky.mnemora.ui.screens.settings.SettingsScreen
import com.hihusky.mnemora.ui.screens.test.TestScreen

object Routes {
    const val HOME = "home"
    const val PRACTICE = "practice/{bookId}?nodeId={nodeId}&collectionId={collectionId}&filter={filter}"
    const val REVIEW = "review/{bookId}"
    const val TEST = "test/{bookId}?sessionId={sessionId}"
    const val PREVIEW = "preview/{bookId}?mode={mode}"
    const val SETTINGS = "settings"
    const val BOOK_DETAIL = "bookDetail/{bookId}"
    const val COLLECTION_DETAIL = "collectionDetail/{collectionId}"

    fun practice(
        bookId: Int,
        nodeId: String? = null,
        collectionId: Int? = null,
        filter: String? = null
    ): String {
        var route = "practice/$bookId"
        val params = mutableListOf<String>()
        if (nodeId != null) params.add("nodeId=$nodeId")
        if (collectionId != null) params.add("collectionId=$collectionId")
        if (filter != null) params.add("filter=$filter")
        if (params.isNotEmpty()) route += "?" + params.joinToString("&")
        return route
    }
    fun review(bookId: Int) = "review/$bookId"
    fun test(bookId: Int, sessionId: Long? = null): String {
        return if (sessionId != null) "test/$bookId?sessionId=$sessionId" else "test/$bookId"
    }
    fun preview(bookId: Int) = "preview/$bookId?mode=Preview"
    fun bookDetail(bookId: Int) = "bookDetail/$bookId"
    fun collectionDetail(collectionId: Int) = "collectionDetail/$collectionId"
}

@Composable
fun MnemoraNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToPractice = { bookId, nodeId ->
                    navController.navigate(Routes.practice(bookId, nodeId))
                },
                onNavigateToReview = { navController.navigate(Routes.review(it)) },
                onNavigateToTest = { bookId, sessionId ->
                    navController.navigate(Routes.test(bookId, sessionId))
                },
                onNavigateToPreview = { navController.navigate(Routes.preview(it)) },
                onNavigateToBookDetail = { navController.navigate(Routes.bookDetail(it)) },

            )
        }
        composable(
            route = Routes.PRACTICE,
            arguments = listOf(
                navArgument("bookId") { type = NavType.IntType },
                navArgument("nodeId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("collectionId") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("filter") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            PracticeScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.REVIEW,
            arguments = listOf(navArgument("bookId") { type = NavType.IntType })
        ) {
            ReviewScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.TEST,
            arguments = listOf(
                navArgument("bookId") { type = NavType.IntType },
                navArgument("sessionId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            TestScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.PREVIEW,
            arguments = listOf(
                navArgument("bookId") { type = NavType.IntType },
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "Preview"
                }
            )
        ) {
            PracticeScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.BOOK_DETAIL,
            arguments = listOf(navArgument("bookId") { type = NavType.IntType })
        ) {
            BookDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPractice = { bookId, nodeId, filter ->
                    navController.navigate(Routes.practice(bookId, nodeId = nodeId, filter = filter))
                },
                onNavigateToCollection = { collectionId ->
                    navController.navigate(Routes.collectionDetail(collectionId))
                },
                onResumeSession = { bookId, mode, sessionId ->
                    when (mode) {
                        "Practice" -> navController.navigate(Routes.practice(bookId))
                        "Review" -> navController.navigate(Routes.review(bookId))
                        "Test" -> navController.navigate(Routes.test(bookId, sessionId))
                        "Preview" -> navController.navigate(Routes.preview(bookId))
                        else -> navController.navigate(Routes.practice(bookId))
                    }
                }
            )
        }
        composable(
            route = Routes.COLLECTION_DETAIL,
            arguments = listOf(navArgument("collectionId") { type = NavType.IntType })
        ) {
            CollectionDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPractice = { bookId, collectionId ->
                    navController.navigate(Routes.practice(bookId, collectionId = collectionId))
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
