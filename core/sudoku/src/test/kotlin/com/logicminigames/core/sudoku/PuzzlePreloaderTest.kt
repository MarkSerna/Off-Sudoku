package com.logicminigames.core.sudoku

import com.logicminigames.core.sudoku.engine.InMemoryPuzzleCacheStore
import com.logicminigames.core.sudoku.engine.PuzzlePreloader
import com.logicminigames.core.sudoku.engine.SudokuGenerator
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzlePreloaderTest {

    @Test
    fun `warmUp populates cache and acquire pops cached puzzle then replenishes`() = runTest {
        val cache = InMemoryPuzzleCacheStore()
        val preloader = PuzzlePreloader(
            generator = SudokuGenerator(),
            cache = cache,
            dispatcher = Dispatchers.Default,
        )

        withContext(Dispatchers.Default) {
            preloader.warmUp(
                scope = this,
                difficulties = listOf(SudokuDifficulty.VERY_EASY, SudokuDifficulty.EASY),
                targetPerDifficulty = 1,
            )
        }

        assertEquals(1, cache.count(SudokuDifficulty.VERY_EASY))
        assertEquals(1, cache.count(SudokuDifficulty.EASY))

        val acquired = withContext(Dispatchers.Default) {
            preloader.acquire(SudokuDifficulty.VERY_EASY, replenishmentScope = this)
        }
        assertEquals(SudokuDifficulty.VERY_EASY, acquired.difficulty)
        assertTrue(acquired.solution.isValidSolution())
        assertEquals(1, cache.count(SudokuDifficulty.VERY_EASY))
    }
}
