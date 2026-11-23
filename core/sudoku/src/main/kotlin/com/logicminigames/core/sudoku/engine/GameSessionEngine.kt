package com.logicminigames.core.sudoku.engine

import com.logicminigames.core.sudoku.model.CellCoord
import com.logicminigames.core.sudoku.model.LogicalStep
import com.logicminigames.core.sudoku.model.NotesGrid
import com.logicminigames.core.sudoku.model.SudokuGrid
import com.logicminigames.core.sudoku.model.SudokuPuzzle

enum class ErrorCheckMode {
    IMMEDIATE,
    ON_DEMAND,
    OFF,
}

data class BoardSnapshot(
    val values: SudokuGrid,
    val notes: NotesGrid,
)

data class SudokuGameState(
    val puzzle: SudokuPuzzle,
    val currentGrid: SudokuGrid,
    val notes: NotesGrid = NotesGrid.EMPTY,
    val undoStack: List<BoardSnapshot> = emptyList(),
    val redoStack: List<BoardSnapshot> = emptyList(),
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val elapsedSeconds: Long = 0L,
    val isCompleted: Boolean = false,
)

sealed interface HintResult {
    data class LogicalHint(val step: LogicalStep) : HintResult
    data class IncorrectCellsFound(val cellIndices: Set<Int>) : HintResult
    data class FallbackReveal(val cell: CellCoord, val digit: Int) : HintResult
    data object BoardAlreadySolved : HintResult
}

/**
 * Pure immutable state reducer for active Sudoku gameplay.
 */
object SudokuGameReducer {

    fun newGame(puzzle: SudokuPuzzle): SudokuGameState = SudokuGameState(
        puzzle = puzzle,
        currentGrid = puzzle.initialGrid,
        notes = NotesGrid.EMPTY,
    )

    fun isFixedCell(state: SudokuGameState, index: Int): Boolean =
        state.puzzle.initialGrid[index] != 0

    fun enterDigit(
        state: SudokuGameState,
        index: Int,
        digit: Int,
        errorCheckMode: ErrorCheckMode,
        autoClearNotes: Boolean = true,
    ): SudokuGameState {
        require(digit in 1..9) { "Digit must be 1..9, got $digit" }
        if (state.isCompleted || isFixedCell(state, index)) return state
        if (state.currentGrid[index] == digit) return state

        val snapshot = BoardSnapshot(state.currentGrid, state.notes)
        val newGrid = state.currentGrid.withValue(index, digit)
        var newNotes = state.notes.clearCell(index)
        if (autoClearNotes) {
            newNotes = newNotes.removeDigitFromCells(SudokuTopology.PEERS[index], digit)
        }

        val isWrong = digit != state.puzzle.solution[index]
        val newMistakes = if (isWrong && errorCheckMode != ErrorCheckMode.OFF) {
            state.mistakes + 1
        } else if (isWrong) {
            // Still record mistake count silently so Victory stats are accurate, without leaking UI highlights
            state.mistakes + 1
        } else {
            state.mistakes
        }

        val completed = newGrid == state.puzzle.solution
        return state.copy(
            currentGrid = newGrid,
            notes = newNotes,
            undoStack = state.undoStack + snapshot,
            redoStack = emptyList(),
            mistakes = newMistakes,
            isCompleted = completed,
        )
    }

    fun toggleNote(
        state: SudokuGameState,
        index: Int,
        digit: Int,
    ): SudokuGameState {
        require(digit in 1..9) { "Digit must be 1..9, got $digit" }
        if (state.isCompleted || isFixedCell(state, index)) return state
        if (state.currentGrid[index] != 0) return state

        val snapshot = BoardSnapshot(state.currentGrid, state.notes)
        val newNotes = state.notes.toggleNote(index, digit)
        return state.copy(
            notes = newNotes,
            undoStack = state.undoStack + snapshot,
            redoStack = emptyList(),
        )
    }

    fun clearCell(
        state: SudokuGameState,
        index: Int,
    ): SudokuGameState {
        if (state.isCompleted || isFixedCell(state, index)) return state
        val hasValue = state.currentGrid[index] != 0
        val hasNotes = state.notes.maskAt(index) != 0
        if (!hasValue && !hasNotes) return state

        val snapshot = BoardSnapshot(state.currentGrid, state.notes)
        val newGrid = if (hasValue) state.currentGrid.withValue(index, 0) else state.currentGrid
        val newNotes = if (hasNotes) state.notes.clearCell(index) else state.notes
        return state.copy(
            currentGrid = newGrid,
            notes = newNotes,
            undoStack = state.undoStack + snapshot,
            redoStack = emptyList(),
        )
    }

    fun undo(state: SudokuGameState): SudokuGameState {
        if (state.isCompleted || state.undoStack.isEmpty()) return state
        val previous = state.undoStack.last()
        val currentSnapshot = BoardSnapshot(state.currentGrid, state.notes)
        return state.copy(
            currentGrid = previous.values,
            notes = previous.notes,
            undoStack = state.undoStack.dropLast(1),
            redoStack = state.redoStack + currentSnapshot,
        )
    }

    fun redo(state: SudokuGameState): SudokuGameState {
        if (state.isCompleted || state.redoStack.isEmpty()) return state
        val next = state.redoStack.last()
        val currentSnapshot = BoardSnapshot(state.currentGrid, state.notes)
        return state.copy(
            currentGrid = next.values,
            notes = next.notes,
            undoStack = state.undoStack + currentSnapshot,
            redoStack = state.redoStack.dropLast(1),
            isCompleted = next.values == state.puzzle.solution,
        )
    }

    fun findRuleConflicts(grid: SudokuGrid): Set<Int> {
        val conflicts = HashSet<Int>()
        for (unit in SudokuTopology.ALL_UNITS) {
            val firstSeen = IntArray(10) { -1 }
            for (idx in unit.cells) {
                val value = grid[idx]
                if (value == 0) continue
                val prev = firstSeen[value]
                if (prev == -1) {
                    firstSeen[value] = idx
                } else {
                    conflicts.add(prev)
                    conflicts.add(idx)
                }
            }
        }
        return conflicts
    }

    fun findWrongCells(state: SudokuGameState): Set<Int> {
        val wrong = HashSet<Int>()
        for (idx in 0 until SudokuTopology.CELL_COUNT) {
            val value = state.currentGrid[idx]
            if (value != 0 && !isFixedCell(state, idx) && value != state.puzzle.solution[idx]) {
                wrong.add(idx)
            }
        }
        return wrong
    }

    /**
     * Computes remaining digit counts (1..9).
     * Under [ErrorCheckMode.IMMEDIATE], counts only correctly placed occurrences so wrong placements
     * don't prematurely disable a digit. Under [ErrorCheckMode.ON_DEMAND] or [ErrorCheckMode.OFF],
     * counts all placed digits on the board so it never leaks whether an entry is wrong.
     */
    fun remainingDigitCounts(
        state: SudokuGameState,
        errorCheckMode: ErrorCheckMode,
    ): Map<Int, Int> {
        val counts = IntArray(10)
        for (idx in 0 until SudokuTopology.CELL_COUNT) {
            val value = state.currentGrid[idx]
            if (value == 0) continue
            if (errorCheckMode == ErrorCheckMode.IMMEDIATE) {
                if (value == state.puzzle.solution[idx]) {
                    counts[value]++
                }
            } else {
                counts[value]++
            }
        }
        return buildMap(9) {
            for (d in 1..9) {
                put(d, (9 - counts[d]).coerceAtLeast(0))
            }
        }
    }

    fun computeLogicalHint(
        state: SudokuGameState,
        logicalSolver: LogicalSudokuSolver,
    ): HintResult {
        if (state.isCompleted || state.currentGrid == state.puzzle.solution) {
            return HintResult.BoardAlreadySolved
        }
        val wrong = findWrongCells(state)
        if (wrong.isNotEmpty()) {
            return HintResult.IncorrectCellsFound(wrong)
        }
        val step = logicalSolver.findNextStep(state.currentGrid)
        if (step != null) {
            return HintResult.LogicalHint(step)
        }
        // Fallback when board has no remaining simple pattern before backtracking
        for (idx in 0 until SudokuTopology.CELL_COUNT) {
            if (state.currentGrid[idx] == 0) {
                return HintResult.FallbackReveal(
                    cell = CellCoord.fromIndex(idx),
                    digit = state.puzzle.solution[idx],
                )
            }
        }
        return HintResult.BoardAlreadySolved
    }

    fun applyLogicalHintStep(
        state: SudokuGameState,
        step: LogicalStep,
        autoClearNotes: Boolean = true,
    ): SudokuGameState {
        if (state.isCompleted) return state
        return if (step.isPlacement) {
            val idx = step.targetCell!!.index
            val digit = step.digit!!
            val snapshot = BoardSnapshot(state.currentGrid, state.notes)
            val newGrid = state.currentGrid.withValue(idx, digit)
            var newNotes = state.notes.clearCell(idx)
            if (autoClearNotes) {
                newNotes = newNotes.removeDigitFromCells(SudokuTopology.PEERS[idx], digit)
            }
            state.copy(
                currentGrid = newGrid,
                notes = newNotes,
                undoStack = state.undoStack + snapshot,
                redoStack = emptyList(),
                hintsUsed = state.hintsUsed + 1,
                isCompleted = newGrid == state.puzzle.solution,
            )
        } else {
            state.copy(hintsUsed = state.hintsUsed + 1)
        }
    }

    fun applyRevealHint(
        state: SudokuGameState,
        preferredIndex: Int? = null,
        autoClearNotes: Boolean = true,
    ): Pair<SudokuGameState, Int>? {
        if (state.isCompleted) return null

        val targetIndex = when {
            preferredIndex != null &&
                !isFixedCell(state, preferredIndex) &&
                state.currentGrid[preferredIndex] != state.puzzle.solution[preferredIndex] -> preferredIndex
            else -> {
                (0 until SudokuTopology.CELL_COUNT).firstOrNull { idx ->
                    state.currentGrid[idx] == 0
                } ?: (0 until SudokuTopology.CELL_COUNT).firstOrNull { idx ->
                    !isFixedCell(state, idx) && state.currentGrid[idx] != state.puzzle.solution[idx]
                }
            }
        } ?: return null

        val digit = state.puzzle.solution[targetIndex]
        val snapshot = BoardSnapshot(state.currentGrid, state.notes)
        val newGrid = state.currentGrid.withValue(targetIndex, digit)
        var newNotes = state.notes.clearCell(targetIndex)
        if (autoClearNotes) {
            newNotes = newNotes.removeDigitFromCells(SudokuTopology.PEERS[targetIndex], digit)
        }

        val updated = state.copy(
            currentGrid = newGrid,
            notes = newNotes,
            undoStack = state.undoStack + snapshot,
            redoStack = emptyList(),
            hintsUsed = state.hintsUsed + 1,
            isCompleted = newGrid == state.puzzle.solution,
        )
        return updated to targetIndex
    }
}
