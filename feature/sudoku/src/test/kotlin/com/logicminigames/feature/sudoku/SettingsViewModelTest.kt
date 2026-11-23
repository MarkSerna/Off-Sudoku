package com.logicminigames.feature.sudoku

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.logicminigames.core.common.GameOutcome
import com.logicminigames.core.common.KnownGames
import com.logicminigames.core.data.model.AppLanguage
import com.logicminigames.core.data.model.HintMode
import com.logicminigames.core.data.repository.InMemoryActiveAndStatsPort
import com.logicminigames.core.data.repository.InMemoryPreferencesPort
import com.logicminigames.core.sudoku.engine.ErrorCheckMode
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.ui.theme.AppThemePalette
import com.logicminigames.core.ui.theme.BoardFontScale
import com.logicminigames.core.ui.theme.ThemeMode
import com.logicminigames.feature.sudoku.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModelStore: ViewModelStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModelStore = ViewModelStore()
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `updates preferences and supports resetting stats or all data`() = runTest(testDispatcher) {
        try {
            val prefsPort = InMemoryPreferencesPort()
            val statsPort = InMemoryActiveAndStatsPort(prefsPort)
            val vm = SettingsViewModel(preferencesPort = prefsPort, statsPort = statsPort)
            ViewModelProvider(
                viewModelStore,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = vm as T
                },
            )[SettingsViewModel::class.java]

            // Defaults: sound and haptics are OFF
            assertFalse(vm.preferences.value.soundEnabled)
            assertFalse(vm.preferences.value.hapticsEnabled)

            vm.setThemePalette(AppThemePalette.PAPER)
            vm.setThemeMode(ThemeMode.DARK)
            vm.setColorblindMode(true)
            vm.setBoardFontScale(BoardFontScale.LARGE)
            vm.setErrorCheckMode(ErrorCheckMode.ON_DEMAND)
            vm.setHintLimit(-1)
            vm.setHintMode(HintMode.REVEAL_DIRECTLY)
            vm.setLanguage(AppLanguage.PORTUGUESE)
            vm.setHapticsEnabled(true)
            testScheduler.runCurrent()

            val updated = vm.preferences.value
            assertEquals(AppThemePalette.PAPER, updated.themePalette)
            assertEquals(ThemeMode.DARK, updated.themeMode)
            assertTrue(updated.colorblindMode)
            assertEquals(BoardFontScale.LARGE, updated.boardFontScale)
            assertEquals(ErrorCheckMode.ON_DEMAND, updated.errorCheckMode)
            assertEquals(-1, updated.hintLimit)
            assertEquals(HintMode.REVEAL_DIRECTLY, updated.hintMode)
            assertEquals(AppLanguage.PORTUGUESE, updated.language)
            assertTrue(updated.hapticsEnabled)

            statsPort.recordCompletedGame(
                GameOutcome(
                    gameId = KnownGames.SUDOKU,
                    difficultyId = SudokuDifficulty.EASY.id,
                    elapsedSeconds = 100L,
                    mistakes = 0,
                    hintsUsed = 0,
                    isDaily = false,
                    won = true,
                    completedAtEpochMillis = 1_000L,
                ),
                dailyEpochDay = null,
            )

            // Reset stats keeps preferences intact
            vm.resetStats()
            testScheduler.runCurrent()
            assertEquals(0, statsPort.observeDifficultyStats().first().sumOf { it.gamesWon })
            assertEquals(AppThemePalette.PAPER, vm.preferences.value.themePalette)

            // Reset all restores defaults too
            vm.resetAllData()
            testScheduler.runCurrent()
            assertEquals(AppThemePalette.CLASSIC, vm.preferences.value.themePalette)
            assertFalse(vm.preferences.value.hapticsEnabled)
        } finally {
            viewModelStore.clear()
        }
    }
}
