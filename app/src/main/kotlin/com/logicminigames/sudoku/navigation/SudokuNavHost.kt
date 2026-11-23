package com.logicminigames.sudoku.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.logicminigames.core.ui.feedback.GameFeedback
import com.logicminigames.feature.sudoku.game.GameLaunchMode
import com.logicminigames.feature.sudoku.game.GameScreen
import com.logicminigames.feature.sudoku.game.GameViewModel
import com.logicminigames.feature.sudoku.home.HomeScreen
import com.logicminigames.feature.sudoku.home.HomeViewModel
import com.logicminigames.feature.sudoku.settings.SettingsScreen
import com.logicminigames.feature.sudoku.settings.SettingsViewModel
import com.logicminigames.feature.sudoku.stats.StatsScreen
import com.logicminigames.feature.sudoku.stats.StatsViewModel
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
data object HomeRoute

@Serializable
data class GameRoute(
    val mode: String,
    val difficultyId: String? = null,
)

@Serializable
data object StatsRoute

@Serializable
data object SettingsRoute

@Composable
fun SudokuNavHost(
    windowSizeClass: WindowSizeClass,
    feedback: GameFeedback,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() },
    ) {
        composable<HomeRoute> {
            val homeViewModel: HomeViewModel = koinViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                windowSizeClass = windowSizeClass,
                onContinueGame = {
                    navController.navigate(GameRoute(mode = GameLaunchMode.RESUME.name))
                },
                onStartNewGame = { difficulty ->
                    navController.navigate(
                        GameRoute(mode = GameLaunchMode.NEW.name, difficultyId = difficulty.id),
                    )
                },
                onPlayDaily = {
                    navController.navigate(GameRoute(mode = GameLaunchMode.DAILY.name))
                },
                onOpenStats = { navController.navigate(StatsRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }

        composable<GameRoute> {
            val gameViewModel: GameViewModel = koinViewModel()
            GameScreen(
                viewModel = gameViewModel,
                windowSizeClass = windowSizeClass,
                feedback = feedback,
                onBackToHome = { navController.popBackStack() },
            )
        }

        composable<StatsRoute> {
            val statsViewModel: StatsViewModel = koinViewModel()
            StatsScreen(
                viewModel = statsViewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable<SettingsRoute> {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
