package com.logicminigames.feature.sudoku

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.logicminigames.core.common.GameOutcome
import com.logicminigames.core.common.KnownGames
import com.logicminigames.core.data.model.SudokuAchievement
import com.logicminigames.core.data.repository.InMemoryActiveAndStatsPort
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.feature.sudoku.stats.StatsTab
import com.logicminigames.feature.sudoku.stats.StatsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

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
    fun `exposes per-difficulty metrics, daily streaks, and unlocked achievements`() = runTest(testDispatcher) {
        try {
            val port = InMemoryActiveAndStatsPort()
            val fixedDate = LocalDate.of(2026, 11, 20)
            val vm = StatsViewModel(statsPort = port, dateProvider = { fixedDate })
            ViewModelProvider(
                viewModelStore,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = vm as T
                },
            )[StatsViewModel::class.java]

            val collectJob = launch { vm.uiState.collect() }
            testScheduler.runCurrent()

            port.recordGameStarted(SudokuDifficulty.HARD, abandonedPrevious = false)
            port.recordCompletedGame(
                outcome = GameOutcome(
                    gameId = KnownGames.SUDOKU,
                    difficultyId = SudokuDifficulty.HARD.id,
                    elapsedSeconds = 210L,
                    mistakes = 0,
                    hintsUsed = 0,
                    isDaily = true,
                    won = true,
                    completedAtEpochMillis = 5_000L,
                ),
                dailyEpochDay = fixedDate.toEpochDay(),
            )

            vm.selectDifficulty(SudokuDifficulty.HARD)
            vm.selectTab(StatsTab.ACHIEVEMENTS)
            testScheduler.runCurrent()

            val state = vm.uiState.value
            assertEquals(StatsTab.ACHIEVEMENTS, state.selectedTab)
            assertEquals(1, state.activeDifficultyStats.gamesWon)
            assertEquals(210L, state.activeDifficultyStats.bestTimeSeconds)
            assertEquals(100, state.activeDifficultyStats.winRatePercent)
            assertEquals(1, state.dailySummary.currentStreak)
            assertTrue(
                state.achievements.first { it.achievement == SudokuAchievement.FIRST_WIN }.isUnlocked,
            )

            collectJob.cancel()
        } finally {
            viewModelStore.clear()
        }
    }
}
