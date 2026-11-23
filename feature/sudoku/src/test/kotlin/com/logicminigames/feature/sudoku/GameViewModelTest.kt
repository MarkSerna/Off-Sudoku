package com.logicminigames.feature.sudoku

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.logicminigames.core.data.model.GamePreferences
import com.logicminigames.core.data.model.HintMode
import com.logicminigames.core.data.model.SudokuAchievement
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
import com.logicminigames.feature.sudoku.game.GameEffect
import com.logicminigames.feature.sudoku.game.GameEvent
import com.logicminigames.feature.sudoku.game.GameLaunchMode
import com.logicminigames.feature.sudoku.game.GameViewModel
import com.logicminigames.feature.sudoku.game.HintBannerUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val solver = LogicalSudokuSolver()
    private val generator = SudokuGenerator(solver)
    private lateinit var preferencesPort: InMemoryPreferencesPort
    private lateinit var activeGamePort: InMemoryActiveAndStatsPort
    private lateinit var viewModelStore: ViewModelStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        preferencesPort = InMemoryPreferencesPort(
            GamePreferences(
                errorCheckMode = ErrorCheckMode.IMMEDIATE,
                hintLimit = 2,
                hintMode = HintMode.EXPLAIN_FIRST,
            ),
        )
        activeGamePort = InMemoryActiveAndStatsPort(preferencesPort)
        viewModelStore = ViewModelStore()
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        mode: GameLaunchMode = GameLaunchMode.NEW,
        difficulty: SudokuDifficulty = SudokuDifficulty.VERY_EASY,
    ): GameViewModel {
        val handle = SavedStateHandle(
            mapOf(
                GameViewModel.ARG_MODE to mode.name,
                GameViewModel.ARG_DIFFICULTY to difficulty.id,
            ),
        )
        val preloader = PuzzlePreloader(
            generator = generator,
            cache = InMemoryPuzzleCacheStore(),
            dispatcher = testDispatcher,
        )
        val vm = GameViewModel(
            savedStateHandle = handle,
            preloader = preloader,
            dailyGenerator = DailyPuzzleGenerator(generator),
            logicalSolver = solver,
            activeGamePort = activeGamePort,
            preferencesPort = preferencesPort,
            defaultDispatcher = testDispatcher,
            clockTickMs = 1000L,
        )
        ViewModelProvider(
            viewModelStore,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = vm as T
            },
        )[GameViewModel::class.java]
        return vm
    }

    private fun TestScope.flushLoader() {
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(50)
        testScheduler.runCurrent()
    }

    @Test
    fun `initializes game, highlights peers and same numbers, and pauses-resumes timer`() = runTest(testDispatcher) {
        try {
            val vm = createViewModel()
            flushLoader()

            assertFalse(vm.uiState.value.isLoading)
            assertEquals(81, vm.uiState.value.cells.size)

            val fixedCell = vm.uiState.value.cells.first { it.isFixed }
            vm.onEvent(GameEvent.SelectCell(fixedCell.index))
            testScheduler.runCurrent()

            val selected = vm.uiState.value.cells[fixedCell.index]
            assertTrue(selected.isSelected)

            testScheduler.advanceTimeBy(3000)
            testScheduler.runCurrent()
            assertEquals(3L, vm.uiState.value.elapsedSeconds)

            vm.onEvent(GameEvent.PauseFromLifecycle)
            testScheduler.runCurrent()
            assertTrue(vm.uiState.value.isPaused)

            testScheduler.advanceTimeBy(5000)
            testScheduler.runCurrent()
            assertEquals(3L, vm.uiState.value.elapsedSeconds)

            vm.onEvent(GameEvent.TogglePause)
            testScheduler.advanceTimeBy(2000)
            testScheduler.runCurrent()
            assertFalse(vm.uiState.value.isPaused)
            assertEquals(5L, vm.uiState.value.elapsedSeconds)
        } finally {
            viewModelStore.clear()
        }
    }

    @Test
    fun `supports notes, mistakes, unlimited undo-redo, and hint limits`() = runTest(testDispatcher) {
        try {
            val knownPuzzle = generator.generate(SudokuDifficulty.VERY_EASY, seed = 999L)
            activeGamePort.saveActiveGame(SudokuGameReducer.newGame(knownPuzzle))
            val vm = createViewModel(mode = GameLaunchMode.RESUME)
            flushLoader()

            val effects = mutableListOf<GameEffect>()
            val collectJob = launch { vm.effects.collect { effects.add(it) } }
            testScheduler.runCurrent()

            val emptyIdx = (0 until SudokuTopology.CELL_COUNT).first { knownPuzzle.initialGrid[it] == 0 }
            val correctDigit = knownPuzzle.solution[emptyIdx]
            val wrongDigit = (correctDigit % 9) + 1

            vm.onEvent(GameEvent.SelectCell(emptyIdx))
            vm.onEvent(GameEvent.ToggleNotesMode)
            vm.onEvent(GameEvent.EnterDigit(correctDigit))
            testScheduler.runCurrent()
            assertTrue(correctDigit in vm.uiState.value.cells[emptyIdx].notes)

            vm.onEvent(GameEvent.ToggleNotesMode)
            vm.onEvent(GameEvent.EnterDigit(wrongDigit))
            testScheduler.runCurrent()
            assertEquals(1, vm.uiState.value.mistakes)
            assertTrue(vm.uiState.value.cells[emptyIdx].isError)

            vm.onEvent(GameEvent.Undo)
            testScheduler.runCurrent()
            assertEquals(0, vm.uiState.value.cells[emptyIdx].value)
            assertTrue(correctDigit in vm.uiState.value.cells[emptyIdx].notes)

            vm.onEvent(GameEvent.Redo)
            testScheduler.runCurrent()
            assertEquals(wrongDigit, vm.uiState.value.cells[emptyIdx].value)

            vm.onEvent(GameEvent.EraseSelected)
            vm.onEvent(GameEvent.RequestHint)
            vm.onEvent(GameEvent.ChooseExplainHint)
            testScheduler.runCurrent()
            assertNotNull(vm.uiState.value.activeHint)
            assertTrue(vm.uiState.value.activeHint is HintBannerUiModel.Logical)

            vm.onEvent(GameEvent.ApplyActiveHint)
            testScheduler.runCurrent()
            assertEquals(1, vm.uiState.value.hintsUsed)
            assertEquals(1, vm.uiState.value.hintsRemaining)

            vm.onEvent(GameEvent.RequestHint)
            vm.onEvent(GameEvent.ChooseRevealHint)
            testScheduler.runCurrent()
            assertEquals(2, vm.uiState.value.hintsUsed)
            assertEquals(0, vm.uiState.value.hintsRemaining)

            vm.onEvent(GameEvent.RequestHint)
            testScheduler.runCurrent()
            assertTrue(GameEffect.HintsExhausted in effects)

            collectJob.cancel()
        } finally {
            viewModelStore.clear()
        }
    }

    @Test
    fun `completing final cell triggers victory state, records achievements, and emits GameWon`() = runTest(testDispatcher) {
        try {
            val knownPuzzle = generator.generate(SudokuDifficulty.VERY_EASY, seed = 2026L)
            var almostDone = SudokuGameReducer.newGame(knownPuzzle)
            val empties = (0 until SudokuTopology.CELL_COUNT).filter { knownPuzzle.initialGrid[it] == 0 }
            for (idx in empties.dropLast(1)) {
                almostDone = SudokuGameReducer.enterDigit(
                    almostDone,
                    idx,
                    knownPuzzle.solution[idx],
                    ErrorCheckMode.IMMEDIATE,
                )
            }
            activeGamePort.saveActiveGame(almostDone)

            val vm = createViewModel(mode = GameLaunchMode.RESUME)
            flushLoader()

            val effects = mutableListOf<GameEffect>()
            val collectJob = launch { vm.effects.collect { effects.add(it) } }
            testScheduler.runCurrent()

            val lastCell = empties.last()
            vm.onEvent(GameEvent.SelectCell(lastCell))
            vm.onEvent(GameEvent.EnterDigit(knownPuzzle.solution[lastCell]))
            testScheduler.runCurrent()

            assertTrue(vm.uiState.value.isCompleted)
            assertTrue(GameEffect.GameWon in effects)
            assertTrue(SudokuAchievement.FIRST_WIN in vm.uiState.value.unlockedAchievements)
            assertEquals(null, activeGamePort.loadActiveGame())

            collectJob.cancel()
        } finally {
            viewModelStore.clear()
        }
    }
}
