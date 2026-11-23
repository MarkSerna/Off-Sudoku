package com.logicminigames.feature.sudoku

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.logicminigames.core.common.GameOutcome
import com.logicminigames.core.common.KnownGames
import com.logicminigames.core.data.repository.InMemoryActiveAndStatsPort
import com.logicminigames.core.sudoku.engine.DailyPuzzleGenerator
import com.logicminigames.core.sudoku.engine.SudokuGameReducer
import com.logicminigames.core.sudoku.engine.SudokuGenerator
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.feature.sudoku.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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
    fun `exposes active game summary, daily puzzle completion state, and daily streak`() = runTest(testDispatcher) {
        try {
            val port = InMemoryActiveAndStatsPort()
            val generator = SudokuGenerator()
            val dailyGenerator = DailyPuzzleGenerator(generator)
            val fixedDate = LocalDate.of(2026, 11, 20)

            val vm = HomeViewModel(
                activeGamePort = port,
                statsPort = port,
                dailyGenerator = dailyGenerator,
                dateProvider = { fixedDate },
            )
            ViewModelProvider(
                viewModelStore,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = vm as T
                },
            )[HomeViewModel::class.java]

            testScheduler.runCurrent()
            assertNull(vm.uiState.value.activeGame)
            assertEquals(SudokuDifficulty.HARD, vm.uiState.value.todayDifficulty)
            assertFalse(vm.uiState.value.isDailyCompleted)
            assertEquals(0, vm.uiState.value.dailyCurrentStreak)

            val dailyPuzzle = dailyGenerator.generateForDate(fixedDate)
            port.saveActiveGame(SudokuGameReducer.newGame(dailyPuzzle).copy(elapsedSeconds = 42L))
            testScheduler.runCurrent()

            val summary = vm.uiState.value.activeGame
            assertNotNull(summary)
            assertEquals(42L, summary!!.elapsedSeconds)
            assertTrue(summary.isDaily)

            port.recordCompletedGame(
                outcome = GameOutcome(
                    gameId = KnownGames.SUDOKU,
                    difficultyId = dailyPuzzle.difficulty.id,
                    elapsedSeconds = 120L,
                    mistakes = 0,
                    hintsUsed = 0,
                    isDaily = true,
                    won = true,
                    completedAtEpochMillis = 1_000L,
                ),
                dailyEpochDay = dailyPuzzle.dailyEpochDay,
            )
            port.clearActiveGame()
            vm.refresh()
            testScheduler.runCurrent()

            assertNull(vm.uiState.value.activeGame)
            assertTrue(vm.uiState.value.isDailyCompleted)
            assertEquals(1, vm.uiState.value.dailyCurrentStreak)
        } finally {
            viewModelStore.clear()
        }
    }
}
