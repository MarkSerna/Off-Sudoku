package com.logicminigames.feature.sudoku

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.logicminigames.core.data.model.GamePreferences
import com.logicminigames.core.data.repository.InMemoryActiveAndStatsPort
import com.logicminigames.core.data.repository.InMemoryPreferencesPort
import com.logicminigames.core.sudoku.engine.DailyPuzzleGenerator
import com.logicminigames.core.sudoku.engine.ErrorCheckMode
import com.logicminigames.core.sudoku.engine.InMemoryPuzzleCacheStore
import com.logicminigames.core.sudoku.engine.LogicalSudokuSolver
import com.logicminigames.core.sudoku.engine.PuzzlePreloader
import com.logicminigames.core.sudoku.engine.SudokuGameReducer
import com.logicminigames.core.sudoku.engine.SudokuGenerator
import com.logicminigames.core.sudoku.engine.SudokuTopology
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.ui.feedback.NoOpGameFeedback
import com.logicminigames.core.ui.theme.MinigamesTheme
import com.logicminigames.feature.sudoku.game.GameLaunchMode
import com.logicminigames.feature.sudoku.game.GameScreen
import com.logicminigames.feature.sudoku.game.GameViewModel
import com.logicminigames.feature.sudoku.home.HomeScreen
import com.logicminigames.feature.sudoku.home.HomeViewModel
import com.logicminigames.feature.sudoku.settings.SettingsScreen
import com.logicminigames.feature.sudoku.settings.SettingsViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-xxhdpi")
class SudokuMainFlowUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val viewModelStore = ViewModelStore()

    @After
    fun tearDown() {
        viewModelStore.clear()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    private fun compactWindowSize(): WindowSizeClass =
        WindowSizeClass.calculateFromSize(DpSize(411.dp, 914.dp))

    private fun <T : ViewModel> attach(vm: T, clazz: Class<T>): T =
        ViewModelProvider(
            viewModelStore,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <V : ViewModel> create(modelClass: Class<V>): V = vm as V
            },
        )[clazz]

    @Test
    fun homeScreen_rendersQuickPlay_andOpensDifficultySheet() {
        val prefsPort = InMemoryPreferencesPort()
        val statsPort = InMemoryActiveAndStatsPort(prefsPort)
        val dailyGen = DailyPuzzleGenerator(SudokuGenerator())
        val homeVm = attach(
            HomeViewModel(
                activeGamePort = statsPort,
                statsPort = statsPort,
                dailyGenerator = dailyGen,
                dateProvider = { LocalDate.of(2026, 11, 21) },
            ),
            HomeViewModel::class.java,
        )

        var startedDifficulty: SudokuDifficulty? = null

        composeRule.setContent {
            MinigamesTheme {
                HomeScreen(
                    viewModel = homeVm,
                    windowSizeClass = compactWindowSize(),
                    onContinueGame = {},
                    onStartNewGame = { startedDifficulty = it },
                    onPlayDaily = {},
                    onOpenStats = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("Sudoku").assertIsDisplayed()
        composeRule.onNodeWithText("Play now").assertIsDisplayed()
        composeRule.onNodeWithText("New game").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Very Easy").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals(SudokuDifficulty.VERY_EASY, startedDifficulty)
    }

    @Test
    fun gameScreen_mainFlow_selectPlaceNotesUndoPauseResume() {
        val prefsPort = InMemoryPreferencesPort(
            GamePreferences(errorCheckMode = ErrorCheckMode.IMMEDIATE),
        )
        val activePort = InMemoryActiveAndStatsPort(prefsPort)
        val solver = LogicalSudokuSolver()
        val generator = SudokuGenerator(solver)
        val puzzle = generator.generate(SudokuDifficulty.VERY_EASY, seed = 4242L)
        runBlocking {
            activePort.saveActiveGame(SudokuGameReducer.newGame(puzzle))
        }

        val empties = (0 until SudokuTopology.CELL_COUNT).filter { puzzle.initialGrid[it] == 0 }
        val firstEmpty = empties[0]
        val secondEmpty = empties[1]
        val firstAnswer = puzzle.solution[firstEmpty]
        val noteDigit = puzzle.solution[secondEmpty]

        val gameVm = attach(
            GameViewModel(
                savedStateHandle = SavedStateHandle(
                    mapOf(
                        GameViewModel.ARG_MODE to GameLaunchMode.RESUME.name,
                        GameViewModel.ARG_DIFFICULTY to SudokuDifficulty.VERY_EASY.id,
                    ),
                ),
                preloader = PuzzlePreloader(generator, InMemoryPuzzleCacheStore()),
                dailyGenerator = DailyPuzzleGenerator(generator),
                logicalSolver = solver,
                activeGamePort = activePort,
                preferencesPort = prefsPort,
            ),
            GameViewModel::class.java,
        )

        composeRule.setContent {
            MinigamesTheme {
                GameScreen(
                    viewModel = gameVm,
                    windowSizeClass = compactWindowSize(),
                    feedback = NoOpGameFeedback,
                    onBackToHome = {},
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            !gameVm.uiState.value.isLoading
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sudoku_board").assertIsDisplayed()

        // Select empty cell, verify TalkBack metadata, and enter correct digit
        val row = firstEmpty / 9 + 1
        val col = firstEmpty % 9 + 1
        composeRule.onNodeWithTag("cell_$firstEmpty")
            .assertContentDescriptionContains("Row $row, column $col, empty")
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("num_key_$firstAnswer")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        assertEquals(firstAnswer, gameVm.uiState.value.cells[firstEmpty].value)

        // Toggle notes mode, select second empty cell, add a note, then undo
        composeRule.onNodeWithTag("action_notes").performScrollTo().performClick()
        composeRule.onNodeWithTag("cell_$secondEmpty").performScrollTo().performClick()
        composeRule.onNodeWithTag("num_key_$noteDigit").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(noteDigit in gameVm.uiState.value.cells[secondEmpty].notes)

        composeRule.onNodeWithTag("action_undo").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(gameVm.uiState.value.cells[secondEmpty].notes.isEmpty())

        // Pause hides the board behind privacy curtain; Resume brings it back
        composeRule.onNodeWithContentDescription("Pause game").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pause_overlay").assertIsDisplayed()
        composeRule.onNodeWithText("Board hidden while paused").assertIsDisplayed()

        composeRule.onNodeWithText("Resume").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pause_overlay").assertDoesNotExist()
        composeRule.onNodeWithTag("sudoku_board").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_togglesColorblindAndHaptics() {
        val prefsPort = InMemoryPreferencesPort()
        val statsPort = InMemoryActiveAndStatsPort(prefsPort)
        val settingsVm = attach(
            SettingsViewModel(preferencesPort = prefsPort, statsPort = statsPort),
            SettingsViewModel::class.java,
        )

        composeRule.setContent {
            MinigamesTheme {
                SettingsScreen(
                    viewModel = settingsVm,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Colorblind-friendly palette")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Haptic vibration")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        assertTrue(prefsPort.preferences.value.colorblindMode)
        assertTrue(prefsPort.preferences.value.hapticsEnabled)
    }
}
