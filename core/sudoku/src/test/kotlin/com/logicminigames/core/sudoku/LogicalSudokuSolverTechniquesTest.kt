package com.logicminigames.core.sudoku

import com.logicminigames.core.sudoku.engine.LogicalSudokuSolver
import com.logicminigames.core.sudoku.engine.SudokuTopology
import com.logicminigames.core.sudoku.model.CellCoord
import com.logicminigames.core.sudoku.model.SolvingTechnique
import com.logicminigames.core.sudoku.model.SudokuGrid
import com.logicminigames.core.sudoku.model.UnitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicalSudokuSolverTechniquesTest {

    private val solver = LogicalSudokuSolver()

    private fun masksAllAllowed(): IntArray = IntArray(81) { SudokuTopology.ALL_DIGITS_MASK }

    private fun maskOf(vararg digits: Int): Int {
        var m = 0
        for (d in digits) m = m or SudokuTopology.digitBit(d)
        return m
    }

    @Test
    fun `detects NAKED_SINGLE on real board`() {
        val grid = SudokuGrid.fromString(
            "034678912" +
                "672195348" +
                "198342567" +
                "859761423" +
                "426853791" +
                "713924856" +
                "961537284" +
                "287419635" +
                "345286179",
        )
        val step = solver.findNextStep(grid)
        assertNotNull(step)
        assertEquals(SolvingTechnique.NAKED_SINGLE, step!!.technique)
        assertEquals(CellCoord(0, 0), step.targetCell)
        assertEquals(5, step.digit)
    }

    @Test
    fun `detects HIDDEN_SINGLE when no naked single exists`() {
        val masks = masksAllAllowed()
        // Only cell (0,0) in Box 0 allows digit 9, while (0,0) itself has candidates {8,9}
        for (idx in SudokuTopology.BOX_UNITS[0]) {
            if (idx != 0) {
                masks[idx] = SudokuTopology.ALL_DIGITS_MASK xor SudokuTopology.digitBit(9)
            }
        }
        masks[0] = maskOf(8, 9)
        val step = solver.findNextStepWithCandidates(SudokuGrid.EMPTY, masks)
        assertNotNull(step)
        assertEquals(SolvingTechnique.HIDDEN_SINGLE, step!!.technique)
        assertEquals(CellCoord(0, 0), step.targetCell)
        assertEquals(9, step.digit)
        assertEquals(UnitType.BOX, step.unitType)
    }

    @Test
    fun `detects POINTING_CANDIDATES box to row`() {
        val masks = masksAllAllowed()
        // In Box 0, digit 5 only appears in (0,0) and (0,1); row 0 still allows 5 outside Box 0
        for (idx in SudokuTopology.BOX_UNITS[0]) {
            if (idx != 0 && idx != 1) {
                masks[idx] = masks[idx] and SudokuTopology.digitBit(5).inv()
            }
        }
        val step = solver.findNextStepWithCandidates(SudokuGrid.EMPTY, masks)
        assertNotNull(step)
        assertEquals(SolvingTechnique.POINTING_CANDIDATES, step!!.technique)
        assertEquals(5, step.digit)
        assertEquals(6, step.eliminations.size)
        assertTrue(step.eliminations.all { it.cell.row == 0 && it.cell.col >= 3 && it.digit == 5 })
    }

    @Test
    fun `detects BOX_LINE_REDUCTION row to box`() {
        val masks = masksAllAllowed()
        // In row 0, digit 4 only appears inside Box 0 ((0,0), (0,1), (0,2))
        for (col in 3..8) {
            masks[col] = masks[col] and SudokuTopology.digitBit(4).inv()
        }
        val step = solver.findNextStepWithCandidates(SudokuGrid.EMPTY, masks)
        assertNotNull(step)
        assertEquals(SolvingTechnique.BOX_LINE_REDUCTION, step!!.technique)
        assertEquals(4, step.digit)
        assertEquals(6, step.eliminations.size)
        assertTrue(step.eliminations.all { it.cell.box == 0 && it.cell.row != 0 && it.digit == 4 })
    }

    @Test
    fun `detects NAKED_PAIR`() {
        val masks = masksAllAllowed()
        masks[0] = maskOf(2, 7)
        masks[1] = maskOf(2, 7)
        val step = solver.findNextStepWithCandidates(SudokuGrid.EMPTY, masks)
        assertNotNull(step)
        assertEquals(SolvingTechnique.NAKED_PAIR, step!!.technique)
        assertTrue(step.eliminations.isNotEmpty())
        assertTrue(step.eliminations.all { it.digit == 2 || it.digit == 7 })
    }

    @Test
    fun `detects HIDDEN_PAIR`() {
        val masks = masksAllAllowed()
        // In row 0, digits 8 and 9 only appear in (0,0) and (0,4) (different boxes to avoid Box-Line Reduction)
        val clear89 = (SudokuTopology.digitBit(8) or SudokuTopology.digitBit(9)).inv()
        for (col in 0..8) {
            if (col != 0 && col != 4) {
                masks[col] = masks[col] and clear89
            }
        }
        val step = solver.findNextStepWithCandidates(SudokuGrid.EMPTY, masks)
        assertNotNull(step)
        assertEquals(SolvingTechnique.HIDDEN_PAIR, step!!.technique)
        assertTrue(step.eliminations.all { it.cell.row == 0 && (it.cell.col == 0 || it.cell.col == 4) })
        assertTrue(step.eliminations.none { it.digit == 8 || it.digit == 9 })
    }

    @Test
    fun `detects NAKED_TRIPLE`() {
        val masks = masksAllAllowed()
        // Subset {1,2,3} across (0,0), (0,4), (0,8) in row 0 (different boxes to avoid Pointing)
        masks[0] = maskOf(1, 2)
        masks[4] = maskOf(2, 3)
        masks[8] = maskOf(1, 3)
        val step = solver.findNextStepWithCandidates(SudokuGrid.EMPTY, masks)
        assertNotNull(step)
        assertEquals(SolvingTechnique.NAKED_TRIPLE, step!!.technique)
        assertTrue(step.eliminations.all { it.digit in 1..3 && it.cell.row == 0 })
    }

    @Test
    fun `detects HIDDEN_TRIPLE`() {
        val masks = masksAllAllowed()
        // In row 0, digits 1, 2, 3 only appear across (0,0), (0,4), (0,8)
        val clear123 = (maskOf(1, 2, 3)).inv()
        for (col in 0..8) {
            if (col != 0 && col != 4 && col != 8) {
                masks[col] = masks[col] and clear123
            }
        }
        masks[0] = maskOf(1, 2, 4, 5)
        masks[4] = maskOf(2, 3, 6, 7)
        masks[8] = maskOf(1, 3, 8, 9)
        val step = solver.findNextStepWithCandidates(SudokuGrid.EMPTY, masks)
        assertNotNull(step)
        assertEquals(SolvingTechnique.HIDDEN_TRIPLE, step!!.technique)
        assertTrue(step.eliminations.all { it.cell.row == 0 && it.cell.col in setOf(0, 4, 8) })
        assertTrue(step.eliminations.none { it.digit in 1..3 })
    }

    @Test
    fun `detects X_WING`() {
        val masks = masksAllAllowed()
        val bit6Inv = SudokuTopology.digitBit(6).inv()
        // Rows 0 and 3 only allow digit 6 in columns 0 and 4 (different boxes on each row)
        for (col in 0..8) {
            if (col != 0 && col != 4) {
                masks[0 * 9 + col] = masks[0 * 9 + col] and bit6Inv
                masks[3 * 9 + col] = masks[3 * 9 + col] and bit6Inv
            }
        }
        val step = solver.findNextStepWithCandidates(SudokuGrid.EMPTY, masks)
        assertNotNull(step)
        assertEquals(SolvingTechnique.X_WING, step!!.technique)
        assertEquals(6, step.digit)
        assertEquals(14, step.eliminations.size)
        assertTrue(step.eliminations.all { (it.cell.col == 0 || it.cell.col == 4) && it.cell.row != 0 && it.cell.row != 3 })
    }

    @Test
    fun `detects XY_WING`() {
        val masks = masksAllAllowed()
        masks[0 * 9 + 0] = maskOf(1, 2) // pivot
        masks[0 * 9 + 4] = maskOf(1, 3) // pincer 1
        masks[4 * 9 + 0] = maskOf(2, 3) // pincer 2
        val step = solver.findNextStepWithCandidates(SudokuGrid.EMPTY, masks)
        assertNotNull(step)
        assertEquals(SolvingTechnique.XY_WING, step!!.technique)
        assertEquals(3, step.digit)
        assertEquals(1, step.eliminations.size)
        assertEquals(CellCoord(4, 4), step.eliminations.single().cell)
    }

    @Test
    fun `detects NAKED_QUAD`() {
        val masks = masksAllAllowed()
        // Subset {1,2,3,4} across 4 cells in row 0
        masks[0] = maskOf(1, 2)
        masks[3] = maskOf(2, 3)
        masks[6] = maskOf(3, 4)
        masks[8] = maskOf(1, 4)
        val step = solver.findNextStepWithCandidates(SudokuGrid.EMPTY, masks)
        assertNotNull(step)
        assertEquals(SolvingTechnique.NAKED_QUAD, step!!.technique)
        assertTrue(step.eliminations.all { it.digit in 1..4 && it.cell.row == 0 })
    }

    @Test
    fun `detects SWORDFISH`() {
        val masks = masksAllAllowed()
        val bit7Inv = SudokuTopology.digitBit(7).inv()
        // Rows 0, 3, 6 only allow digit 7 in two columns each across {0, 3, 6}
        for (col in 0..8) {
            if (col != 0 && col != 3) masks[0 * 9 + col] = masks[0 * 9 + col] and bit7Inv
            if (col != 3 && col != 6) masks[3 * 9 + col] = masks[3 * 9 + col] and bit7Inv
            if (col != 0 && col != 6) masks[6 * 9 + col] = masks[6 * 9 + col] and bit7Inv
        }
        val step = solver.findNextStepWithCandidates(SudokuGrid.EMPTY, masks)
        assertNotNull(step)
        assertEquals(SolvingTechnique.SWORDFISH, step!!.technique)
        assertEquals(7, step.digit)
        assertEquals(18, step.eliminations.size)
        assertTrue(
            step.eliminations.all {
                it.cell.col in setOf(0, 3, 6) && it.cell.row !in setOf(0, 3, 6)
            },
        )
    }

    @Test
    fun `solves complete classic real-world puzzle logically`() {
        val puzzle = SudokuGrid.fromString(
            "003020600" +
                "900305001" +
                "001806400" +
                "008102900" +
                "700000008" +
                "006708200" +
                "002609500" +
                "800203009" +
                "005010300",
        )
        val result = solver.solve(puzzle)
        assertTrue(result.solved)
        assertNotNull(result.solution)
        assertTrue(result.solution!!.isValidSolution())
        assertTrue(result.steps.isNotEmpty())
    }
}
