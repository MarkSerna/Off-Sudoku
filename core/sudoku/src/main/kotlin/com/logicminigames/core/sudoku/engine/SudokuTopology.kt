package com.logicminigames.core.sudoku.engine

import com.logicminigames.core.sudoku.model.UnitType

/**
 * Precomputed Sudoku 9x9 topology, units, peer indices, and 9-bit candidate mask helpers.
 */
object SudokuTopology {
    const val GRID_SIZE = 9
    const val CELL_COUNT = 81
    const val BOX_SIZE = 3
    const val ALL_DIGITS_MASK = 0x1FF

    data class UnitRef(
        val type: UnitType,
        val index: Int,
        val cells: IntArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is UnitRef) return false
            return type == other.type && index == other.index && cells.contentEquals(other.cells)
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + index
            result = 31 * result + cells.contentHashCode()
            return result
        }
    }

    val ROW_UNITS: Array<IntArray> = Array(GRID_SIZE) { row ->
        IntArray(GRID_SIZE) { col -> row * GRID_SIZE + col }
    }

    val COL_UNITS: Array<IntArray> = Array(GRID_SIZE) { col ->
        IntArray(GRID_SIZE) { row -> row * GRID_SIZE + col }
    }

    val BOX_UNITS: Array<IntArray> = Array(GRID_SIZE) { box ->
        val startRow = (box / BOX_SIZE) * BOX_SIZE
        val startCol = (box % BOX_SIZE) * BOX_SIZE
        IntArray(GRID_SIZE) { offset ->
            val r = startRow + offset / BOX_SIZE
            val c = startCol + offset % BOX_SIZE
            r * GRID_SIZE + c
        }
    }

    val ALL_UNITS: List<UnitRef> = buildList(27) {
        for (i in 0 until GRID_SIZE) {
            add(UnitRef(UnitType.BOX, i, BOX_UNITS[i]))
        }
        for (i in 0 until GRID_SIZE) {
            add(UnitRef(UnitType.ROW, i, ROW_UNITS[i]))
        }
        for (i in 0 until GRID_SIZE) {
            add(UnitRef(UnitType.COLUMN, i, COL_UNITS[i]))
        }
    }

    val PEERS: Array<IntArray> = Array(CELL_COUNT) { idx ->
        val r = idx / GRID_SIZE
        val c = idx % GRID_SIZE
        val b = boxOf(idx)
        val set = LinkedHashSet<Int>(20)
        for (peer in ROW_UNITS[r]) if (peer != idx) set.add(peer)
        for (peer in COL_UNITS[c]) if (peer != idx) set.add(peer)
        for (peer in BOX_UNITS[b]) if (peer != idx) set.add(peer)
        set.toIntArray().also { it.sort() }
    }

    fun rowOf(index: Int): Int = index / GRID_SIZE

    fun colOf(index: Int): Int = index % GRID_SIZE

    fun boxOf(index: Int): Int {
        val r = index / GRID_SIZE
        val c = index % GRID_SIZE
        return (r / BOX_SIZE) * BOX_SIZE + (c / BOX_SIZE)
    }

    fun digitBit(digit: Int): Int = 1 shl (digit - 1)

    fun maskToDigitSet(mask: Int): Set<Int> {
        if (mask == 0) return emptySet()
        val result = LinkedHashSet<Int>(Integer.bitCount(mask))
        forEachDigitInMask(mask) { result.add(it) }
        return result
    }

    inline fun forEachDigitInMask(mask: Int, action: (Int) -> Unit) {
        var m = mask and ALL_DIGITS_MASK
        while (m != 0) {
            val bit = Integer.numberOfTrailingZeros(m)
            action(bit + 1)
            m = m and (m - 1)
        }
    }
}
