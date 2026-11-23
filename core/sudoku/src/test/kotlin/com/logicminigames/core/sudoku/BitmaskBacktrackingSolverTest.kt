package com.logicminigames.core.sudoku

import com.logicminigames.core.sudoku.engine.BitmaskBacktrackingSolver
import com.logicminigames.core.sudoku.model.SudokuGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BitmaskBacktrackingSolverTest {

    @Test
    fun `solves classic board and verifies unique solution`() {
        val grid = SudokuGrid.fromString(
            "530070000" +
                "600195000" +
                "098000060" +
                "800060003" +
                "400803001" +
                "700020006" +
                "060000280" +
                "000419005" +
                "000080079",
        )
        assertTrue(BitmaskBacktrackingSolver.hasUniqueSolution(grid))
        val solved = BitmaskBacktrackingSolver.solve(grid.toIntArrayCopy())
        assertNotNull(solved)
        assertTrue(SudokuGrid.fromIntArray(solved!!).isValidSolution())
    }

    @Test
    fun `detects multiple solutions on under-constrained board`() {
        val ambiguous = SudokuGrid.fromString(
            "000000000" +
                "000000000" +
                "000000000" +
                "859761423" +
                "426853791" +
                "713924856" +
                "961537284" +
                "287419635" +
                "345286179",
        )
        // Rows 0..2 are completely empty within the top band -> multiple valid permutations exist
        val count = BitmaskBacktrackingSolver.countSolutions(ambiguous.toIntArrayCopy(), limit = 2)
        assertEquals(2, count)
        assertFalse(BitmaskBacktrackingSolver.hasUniqueSolution(ambiguous))
    }

    @Test
    fun `returns zero solutions and null for conflicting or unsolvable grids`() {
        val conflicted = SudokuGrid.EMPTY.withValue(0, 9).withValue(1, 9)
        assertEquals(0, BitmaskBacktrackingSolver.countSolutions(conflicted.toIntArrayCopy(), limit = 2))
        assertNull(BitmaskBacktrackingSolver.solve(conflicted.toIntArrayCopy()))
    }

    @Test
    fun `generateCompleteBoard produces valid solved boards`() {
        val rng = Random(42L)
        repeat(10) {
            val board = BitmaskBacktrackingSolver.generateCompleteBoard(rng)
            assertTrue(board.isValidSolution())
        }
    }
}
