package com.logicminigames.core.sudoku

import com.logicminigames.core.sudoku.engine.ErrorCheckMode
import com.logicminigames.core.sudoku.engine.HintResult
import com.logicminigames.core.sudoku.engine.LogicalSudokuSolver
import com.logicminigames.core.sudoku.engine.SudokuGameReducer
import com.logicminigames.core.sudoku.engine.SudokuGenerator
import com.logicminigames.core.sudoku.engine.SudokuTopology
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuGameReducerTest {

    private val solver = LogicalSudokuSolver()
    private val puzzle = SudokuGenerator(solver).generate(SudokuDifficulty.VERY_EASY, seed = 1234L)

    private fun firstEmptyIndex(): Int =
        (0 until SudokuTopology.CELL_COUNT).first { puzzle.initialGrid[it] == 0 }

    private fun firstFixedIndex(): Int =
        (0 until SudokuTopology.CELL_COUNT).first { puzzle.initialGrid[it] != 0 }

    @Test
    fun `fixed cells cannot be overwritten or cleared`() {
        val state = SudokuGameReducer.newGame(puzzle)
        val fixedIdx = firstFixedIndex()
        val originalValue = state.currentGrid[fixedIdx]

        val afterDigit = SudokuGameReducer.enterDigit(state, fixedIdx, (originalValue % 9) + 1, ErrorCheckMode.IMMEDIATE)
        assertEquals(state, afterDigit)

        val afterClear = SudokuGameReducer.clearCell(state, fixedIdx)
        assertEquals(state, afterClear)
    }

    @Test
    fun `placing digit clears peer notes and undo-redo restores exact values and notes`() {
        var state = SudokuGameReducer.newGame(puzzle)
        val emptyIdx = firstEmptyIndex()
        val correctDigit = puzzle.solution[emptyIdx]
        val emptyPeerIdx = SudokuTopology.PEERS[emptyIdx].first { puzzle.initialGrid[it] == 0 }

        // Add note on emptyIdx and peer note matching correctDigit
        state = SudokuGameReducer.toggleNote(state, emptyIdx, correctDigit)
        state = SudokuGameReducer.toggleNote(state, emptyPeerIdx, correctDigit)
        assertTrue(state.notes.hasNote(emptyIdx, correctDigit))
        assertTrue(state.notes.hasNote(emptyPeerIdx, correctDigit))

        // Enter correct digit -> auto-clears cell notes & peer note
        val placed = SudokuGameReducer.enterDigit(state, emptyIdx, correctDigit, ErrorCheckMode.IMMEDIATE)
        assertEquals(correctDigit, placed.currentGrid[emptyIdx])
        assertFalse(placed.notes.hasNote(emptyIdx, correctDigit))
        assertFalse(placed.notes.hasNote(emptyPeerIdx, correctDigit))

        // Undo restores both empty cell and peer's erased note
        val undone = SudokuGameReducer.undo(placed)
        assertEquals(0, undone.currentGrid[emptyIdx])
        assertTrue(undone.notes.hasNote(emptyIdx, correctDigit))
        assertTrue(undone.notes.hasNote(emptyPeerIdx, correctDigit))

        // Redo re-applies digit placement & note cleanup
        val redone = SudokuGameReducer.redo(undone)
        assertEquals(correctDigit, redone.currentGrid[emptyIdx])
        assertFalse(redone.notes.hasNote(emptyPeerIdx, correctDigit))
    }

    @Test
    fun `mistakes, rule conflicts, and remaining digit counts behave correctly across error modes`() {
        var state = SudokuGameReducer.newGame(puzzle)
        val emptyIdx = firstEmptyIndex()
        val correctDigit = puzzle.solution[emptyIdx]
        val wrongDigit = (correctDigit % 9) + 1

        state = SudokuGameReducer.enterDigit(state, emptyIdx, wrongDigit, ErrorCheckMode.IMMEDIATE)
        assertEquals(1, state.mistakes)
        assertEquals(setOf(emptyIdx), SudokuGameReducer.findWrongCells(state))

        val immediateCounts = SudokuGameReducer.remainingDigitCounts(state, ErrorCheckMode.IMMEDIATE)
        val offCounts = SudokuGameReducer.remainingDigitCounts(state, ErrorCheckMode.OFF)
        // Under OFF, wrong placement reduces displayed count; under IMMEDIATE it does not
        assertEquals(offCounts.getValue(wrongDigit) + 1, immediateCounts.getValue(wrongDigit))

        // Also test rule conflicts when duplicating a peer's digit
        val peerDigit = SudokuTopology.PEERS[emptyIdx]
            .map { puzzle.initialGrid[it] }
            .first { it != 0 }
        val conflicted = SudokuGameReducer.enterDigit(
            SudokuGameReducer.newGame(puzzle),
            emptyIdx,
            peerDigit,
            ErrorCheckMode.IMMEDIATE,
        )
        assertTrue(emptyIdx in SudokuGameReducer.findRuleConflicts(conflicted.currentGrid))
    }

    @Test
    fun `logical hint reports wrong cells first and completes board on final cell`() {
        var state = SudokuGameReducer.newGame(puzzle)
        val emptyIdx = firstEmptyIndex()
        val wrongDigit = (puzzle.solution[emptyIdx] % 9) + 1

        val withMistake = SudokuGameReducer.enterDigit(state, emptyIdx, wrongDigit, ErrorCheckMode.ON_DEMAND)
        val hintOnMistake = SudokuGameReducer.computeLogicalHint(withMistake, solver)
        assertTrue(hintOnMistake is HintResult.IncorrectCellsFound)
        assertTrue(emptyIdx in (hintOnMistake as HintResult.IncorrectCellsFound).cellIndices)

        val cleanHint = SudokuGameReducer.computeLogicalHint(state, solver)
        assertTrue(cleanHint is HintResult.LogicalHint)

        // Fill all but last empty cell, then complete with reveal hint
        val empties = (0 until SudokuTopology.CELL_COUNT).filter { puzzle.initialGrid[it] == 0 }
        for (idx in empties.dropLast(1)) {
            state = SudokuGameReducer.enterDigit(state, idx, puzzle.solution[idx], ErrorCheckMode.IMMEDIATE)
        }
        assertFalse(state.isCompleted)

        val revealed = SudokuGameReducer.applyRevealHint(state, empties.last())
        assertNotNull(revealed)
        val (wonState, revealedCell) = revealed!!
        assertEquals(empties.last(), revealedCell)
        assertTrue(wonState.isCompleted)
        assertEquals(1, wonState.hintsUsed)
    }
}
