package com.logicminigames.core.sudoku

import com.logicminigames.core.sudoku.engine.BitmaskBacktrackingSolver
import com.logicminigames.core.sudoku.engine.DailyPuzzleGenerator
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyPuzzleGeneratorTest {

    private val dailyGenerator = DailyPuzzleGenerator()

    @Test
    fun `daily puzzle is deterministic for the same calendar date`() {
        val date = LocalDate.of(2026, 11, 20)
        val first = dailyGenerator.generateForDate(date)
        val second = dailyGenerator.generateForDate(date)

        assertEquals(first.id, second.id)
        assertEquals(first.initialGrid, second.initialGrid)
        assertEquals(first.solution, second.solution)
        assertEquals(date.toEpochDay(), first.dailyEpochDay)
        assertTrue(BitmaskBacktrackingSolver.hasUniqueSolution(first.initialGrid))
    }

    @Test
    fun `consecutive 7 days produce distinct valid puzzles following weekly difficulty schedule`() {
        val monday = LocalDate.of(2026, 11, 16)
        val expectedDifficulties = listOf(
            SudokuDifficulty.EASY,
            SudokuDifficulty.MEDIUM,
            SudokuDifficulty.MEDIUM,
            SudokuDifficulty.HARD,
            SudokuDifficulty.HARD,
            SudokuDifficulty.EXPERT,
            SudokuDifficulty.MEDIUM,
        )

        val grids = HashSet<String>()
        for (offset in 0..6) {
            val day = monday.plusDays(offset.toLong())
            val puzzle = dailyGenerator.generateForDate(day)
            assertEquals(expectedDifficulties[offset], puzzle.difficulty)
            assertTrue(puzzle.solution.isValidSolution())
            assertTrue(BitmaskBacktrackingSolver.hasUniqueSolution(puzzle.initialGrid))
            grids.add(puzzle.initialGrid.toFlatString())
        }
        assertEquals(7, grids.size)
    }
}
