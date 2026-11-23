package com.logicminigames.core.sudoku.model

import com.logicminigames.core.common.DifficultyTier
import com.logicminigames.core.sudoku.engine.SudokuTopology

enum class SudokuDifficulty(
    override val id: String,
    override val order: Int,
) : DifficultyTier {
    VERY_EASY("very_easy", 0),
    EASY("easy", 1),
    MEDIUM("medium", 2),
    HARD("hard", 3),
    EXPERT("expert", 4),
    ;

    companion object {
        fun fromId(id: String): SudokuDifficulty? = entries.firstOrNull { it.id == id }
    }
}

data class CellCoord(
    val row: Int,
    val col: Int,
) {
    init {
        require(row in 0..8 && col in 0..8) { "CellCoord out of bounds: ($row, $col)" }
    }

    val index: Int get() = row * SudokuTopology.GRID_SIZE + col
    val box: Int get() = (row / 3) * 3 + (col / 3)

    companion object {
        fun fromIndex(index: Int): CellCoord {
            require(index in 0 until SudokuTopology.CELL_COUNT) { "Cell index out of bounds: $index" }
            return CellCoord(index / SudokuTopology.GRID_SIZE, index % SudokuTopology.GRID_SIZE)
        }
    }
}

/**
 * Immutable 9x9 Sudoku grid storing values 0..9 (0 = empty).
 */
class SudokuGrid private constructor(
    private val cells: IntArray,
) {
    operator fun get(index: Int): Int = cells[index]

    operator fun get(row: Int, col: Int): Int {
        require(row in 0..8 && col in 0..8) { "Coordinates out of bounds: ($row, $col)" }
        return cells[row * SudokuTopology.GRID_SIZE + col]
    }

    operator fun get(coord: CellCoord): Int = cells[coord.index]

    fun withValue(index: Int, value: Int): SudokuGrid {
        require(index in 0 until SudokuTopology.CELL_COUNT) { "Index out of bounds: $index" }
        require(value in 0..9) { "Cell value must be 0..9, got $value" }
        if (cells[index] == value) return this
        val copy = cells.copyOf()
        copy[index] = value
        return SudokuGrid(copy)
    }

    fun withValue(coord: CellCoord, value: Int): SudokuGrid = withValue(coord.index, value)

    fun toIntArrayCopy(): IntArray = cells.copyOf()

    fun toFlatString(): String = buildString(SudokuTopology.CELL_COUNT) {
        for (value in cells) {
            append(('0'.code + value).toChar())
        }
    }

    fun filledCount(): Int {
        var count = 0
        for (v in cells) if (v != 0) count++
        return count
    }

    fun isComplete(): Boolean {
        for (v in cells) if (v == 0) return false
        return true
    }

    fun isValidPlacement(index: Int, digit: Int): Boolean {
        require(index in 0 until SudokuTopology.CELL_COUNT) { "Index out of bounds: $index" }
        require(digit in 1..9) { "Digit must be 1..9, got $digit" }
        for (peer in SudokuTopology.PEERS[index]) {
            if (cells[peer] == digit) return false
        }
        return true
    }

    fun hasConflicts(): Boolean {
        for (unit in SudokuTopology.ALL_UNITS) {
            var seenMask = 0
            for (idx in unit.cells) {
                val value = cells[idx]
                if (value == 0) continue
                val bit = SudokuTopology.digitBit(value)
                if ((seenMask and bit) != 0) return true
                seenMask = seenMask or bit
            }
        }
        return false
    }

    fun isValidSolution(): Boolean = isComplete() && !hasConflicts()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SudokuGrid) return false
        return cells.contentEquals(other.cells)
    }

    override fun hashCode(): Int = cells.contentHashCode()

    override fun toString(): String = toFlatString()

    companion object {
        val EMPTY = SudokuGrid(IntArray(SudokuTopology.CELL_COUNT))

        fun fromIntArray(cells: IntArray): SudokuGrid {
            require(cells.size == SudokuTopology.CELL_COUNT) {
                "SudokuGrid requires 81 cells, got ${cells.size}"
            }
            for (i in cells.indices) {
                require(cells[i] in 0..9) { "Invalid cell value ${cells[i]} at index $i" }
            }
            return SudokuGrid(cells.copyOf())
        }

        fun fromString(raw: String): SudokuGrid {
            val normalized = raw.filterNot { it.isWhitespace() }
            require(normalized.length == SudokuTopology.CELL_COUNT) {
                "Sudoku string must contain 81 cells, got ${normalized.length}"
            }
            val parsed = IntArray(SudokuTopology.CELL_COUNT) { idx ->
                when (val ch = normalized[idx]) {
                    '.', '0' -> 0
                    in '1'..'9' -> ch - '0'
                    else -> throw IllegalArgumentException("Invalid Sudoku char '$ch' at index $idx")
                }
            }
            return SudokuGrid(parsed)
        }
    }
}

/**
 * Immutable 9x9 pencil-marks grid storing 9-bit candidate masks per cell.
 */
class NotesGrid private constructor(
    private val masks: IntArray,
) {
    fun maskAt(index: Int): Int = masks[index]

    fun notesAt(index: Int): Set<Int> = SudokuTopology.maskToDigitSet(masks[index])

    fun hasNote(index: Int, digit: Int): Boolean {
        require(digit in 1..9) { "Digit must be 1..9, got $digit" }
        return (masks[index] and SudokuTopology.digitBit(digit)) != 0
    }

    fun toggleNote(index: Int, digit: Int): NotesGrid {
        require(index in 0 until SudokuTopology.CELL_COUNT) { "Index out of bounds: $index" }
        require(digit in 1..9) { "Digit must be 1..9, got $digit" }
        val copy = masks.copyOf()
        copy[index] = copy[index] xor SudokuTopology.digitBit(digit)
        return NotesGrid(copy)
    }

    fun withMask(index: Int, mask: Int): NotesGrid {
        require(index in 0 until SudokuTopology.CELL_COUNT) { "Index out of bounds: $index" }
        val sanitized = mask and SudokuTopology.ALL_DIGITS_MASK
        if (masks[index] == sanitized) return this
        val copy = masks.copyOf()
        copy[index] = sanitized
        return NotesGrid(copy)
    }

    fun clearCell(index: Int): NotesGrid {
        require(index in 0 until SudokuTopology.CELL_COUNT) { "Index out of bounds: $index" }
        if (masks[index] == 0) return this
        val copy = masks.copyOf()
        copy[index] = 0
        return NotesGrid(copy)
    }

    fun removeDigitFromCells(indices: IntArray, digit: Int): NotesGrid {
        require(digit in 1..9) { "Digit must be 1..9, got $digit" }
        val clearBitInv = SudokuTopology.digitBit(digit).inv()
        var changed = false
        for (idx in indices) {
            if ((masks[idx] and clearBitInv.inv()) != 0) {
                changed = true
                break
            }
        }
        if (!changed) return this
        val copy = masks.copyOf()
        for (idx in indices) {
            copy[idx] = copy[idx] and clearBitInv
        }
        return NotesGrid(copy)
    }

    fun toSerializedString(): String = masks.joinToString(",")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NotesGrid) return false
        return masks.contentEquals(other.masks)
    }

    override fun hashCode(): Int = masks.contentHashCode()

    companion object {
        val EMPTY = NotesGrid(IntArray(SudokuTopology.CELL_COUNT))

        fun fromMasks(masks: IntArray): NotesGrid {
            require(masks.size == SudokuTopology.CELL_COUNT) {
                "NotesGrid requires 81 masks, got ${masks.size}"
            }
            val copy = IntArray(SudokuTopology.CELL_COUNT) { idx ->
                masks[idx] and SudokuTopology.ALL_DIGITS_MASK
            }
            return NotesGrid(copy)
        }

        fun fromSerializedString(raw: String): NotesGrid {
            if (raw.isBlank()) return EMPTY
            val parts = raw.split(',')
            require(parts.size == SudokuTopology.CELL_COUNT) {
                "NotesGrid serialized string requires 81 entries, got ${parts.size}"
            }
            val parsed = IntArray(SudokuTopology.CELL_COUNT) { idx ->
                parts[idx].trim().toInt() and SudokuTopology.ALL_DIGITS_MASK
            }
            return NotesGrid(parsed)
        }
    }
}

enum class UnitType {
    ROW,
    COLUMN,
    BOX,
}

enum class SolvingTechnique(val weight: Int) {
    NAKED_SINGLE(1),
    HIDDEN_SINGLE(3),
    POINTING_CANDIDATES(10),
    BOX_LINE_REDUCTION(12),
    NAKED_PAIR(16),
    HIDDEN_PAIR(22),
    NAKED_TRIPLE(26),
    HIDDEN_TRIPLE(32),
    X_WING(45),
    XY_WING(55),
    NAKED_QUAD(60),
    SWORDFISH(70),
}

data class CandidateElimination(
    val cell: CellCoord,
    val digit: Int,
)

data class LogicalStep(
    val technique: SolvingTechnique,
    val targetCell: CellCoord? = null,
    val digit: Int? = null,
    val eliminations: List<CandidateElimination> = emptyList(),
    val relatedCells: Set<CellCoord> = emptySet(),
    val unitType: UnitType? = null,
    val unitIndex: Int? = null,
) {
    val isPlacement: Boolean get() = targetCell != null && digit != null
}

data class SolverResult(
    val solved: Boolean,
    val solution: SudokuGrid?,
    val steps: List<LogicalStep>,
    val maxTechnique: SolvingTechnique?,
    val totalScore: Int,
    val ratedDifficulty: SudokuDifficulty?,
)

data class SudokuPuzzle(
    val id: String,
    val initialGrid: SudokuGrid,
    val solution: SudokuGrid,
    val difficulty: SudokuDifficulty,
    val maxTechnique: SolvingTechnique,
    val difficultyScore: Int,
    val seed: Long,
    val dailyEpochDay: Long? = null,
)
