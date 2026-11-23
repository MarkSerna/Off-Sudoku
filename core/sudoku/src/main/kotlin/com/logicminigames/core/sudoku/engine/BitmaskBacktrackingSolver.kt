package com.logicminigames.core.sudoku.engine

import com.logicminigames.core.sudoku.model.SudokuGrid
import kotlin.random.Random

/**
 * Exact bitmask backtracking solver with Minimum Remaining Values (MRV) heuristic.
 * Used for fast complete-board generation and strict solution uniqueness verification.
 */
object BitmaskBacktrackingSolver {

    fun countSolutions(grid: IntArray, limit: Int = 2): Int {
        require(grid.size == SudokuTopology.CELL_COUNT) { "Grid must have 81 cells" }
        if (limit <= 0) return 0
        val work = grid.copyOf()
        val rowMask = IntArray(SudokuTopology.GRID_SIZE)
        val colMask = IntArray(SudokuTopology.GRID_SIZE)
        val boxMask = IntArray(SudokuTopology.GRID_SIZE)
        if (!initState(work, rowMask, colMask, boxMask)) return 0
        return countRecursive(work, rowMask, colMask, boxMask, limit)
    }

    fun hasUniqueSolution(grid: SudokuGrid): Boolean =
        countSolutions(grid.toIntArrayCopy(), limit = 2) == 1

    fun solve(grid: IntArray, random: Random? = null): IntArray? {
        require(grid.size == SudokuTopology.CELL_COUNT) { "Grid must have 81 cells" }
        val work = grid.copyOf()
        val rowMask = IntArray(SudokuTopology.GRID_SIZE)
        val colMask = IntArray(SudokuTopology.GRID_SIZE)
        val boxMask = IntArray(SudokuTopology.GRID_SIZE)
        if (!initState(work, rowMask, colMask, boxMask)) return null
        val solved = if (random == null) {
            solveDeterministic(work, rowMask, colMask, boxMask)
        } else {
            val digitScratch = IntArray(SudokuTopology.GRID_SIZE)
            solveRandomized(work, rowMask, colMask, boxMask, random, digitScratch)
        }
        return if (solved) work else null
    }

    fun generateCompleteBoard(random: Random): SudokuGrid {
        val empty = IntArray(SudokuTopology.CELL_COUNT)
        val solved = solve(empty, random)
            ?: throw IllegalStateException("Failed to generate a complete Sudoku board")
        return SudokuGrid.fromIntArray(solved)
    }

    private fun initState(
        work: IntArray,
        rowMask: IntArray,
        colMask: IntArray,
        boxMask: IntArray,
    ): Boolean {
        for (idx in 0 until SudokuTopology.CELL_COUNT) {
            val value = work[idx]
            if (value == 0) continue
            if (value !in 1..9) return false
            val r = SudokuTopology.rowOf(idx)
            val c = SudokuTopology.colOf(idx)
            val b = SudokuTopology.boxOf(idx)
            val bit = SudokuTopology.digitBit(value)
            if (((rowMask[r] or colMask[c] or boxMask[b]) and bit) != 0) {
                return false
            }
            rowMask[r] = rowMask[r] or bit
            colMask[c] = colMask[c] or bit
            boxMask[b] = boxMask[b] or bit
        }
        return true
    }

    private fun findBestEmptyCell(
        work: IntArray,
        rowMask: IntArray,
        colMask: IntArray,
        boxMask: IntArray,
    ): Long {
        var bestIndex = -1
        var bestCandidates = 0
        var minCount = 10
        for (idx in 0 until SudokuTopology.CELL_COUNT) {
            if (work[idx] != 0) continue
            val r = SudokuTopology.rowOf(idx)
            val c = SudokuTopology.colOf(idx)
            val b = SudokuTopology.boxOf(idx)
            val candidates = SudokuTopology.ALL_DIGITS_MASK and (rowMask[r] or colMask[c] or boxMask[b]).inv()
            val count = Integer.bitCount(candidates)
            if (count == 0) return -1L // Dead end
            if (count < minCount) {
                minCount = count
                bestIndex = idx
                bestCandidates = candidates
                if (count == 1) break
            }
        }
        if (bestIndex == -1) return Long.MAX_VALUE // Solved
        return (bestIndex.toLong() shl 16) or bestCandidates.toLong()
    }

    private fun countRecursive(
        work: IntArray,
        rowMask: IntArray,
        colMask: IntArray,
        boxMask: IntArray,
        limit: Int,
    ): Int {
        val packed = findBestEmptyCell(work, rowMask, colMask, boxMask)
        if (packed == -1L) return 0
        if (packed == Long.MAX_VALUE) return 1

        val idx = (packed ushr 16).toInt()
        var candidates = (packed and 0xFFFFL).toInt()
        val r = SudokuTopology.rowOf(idx)
        val c = SudokuTopology.colOf(idx)
        val b = SudokuTopology.boxOf(idx)

        var total = 0
        while (candidates != 0) {
            val lsb = candidates and -candidates
            val digit = Integer.numberOfTrailingZeros(lsb) + 1
            candidates = candidates xor lsb

            work[idx] = digit
            rowMask[r] = rowMask[r] or lsb
            colMask[c] = colMask[c] or lsb
            boxMask[b] = boxMask[b] or lsb

            total += countRecursive(work, rowMask, colMask, boxMask, limit - total)

            work[idx] = 0
            rowMask[r] = rowMask[r] xor lsb
            colMask[c] = colMask[c] xor lsb
            boxMask[b] = boxMask[b] xor lsb

            if (total >= limit) return total
        }
        return total
    }

    private fun solveDeterministic(
        work: IntArray,
        rowMask: IntArray,
        colMask: IntArray,
        boxMask: IntArray,
    ): Boolean {
        val packed = findBestEmptyCell(work, rowMask, colMask, boxMask)
        if (packed == -1L) return false
        if (packed == Long.MAX_VALUE) return true

        val idx = (packed ushr 16).toInt()
        var candidates = (packed and 0xFFFFL).toInt()
        val r = SudokuTopology.rowOf(idx)
        val c = SudokuTopology.colOf(idx)
        val b = SudokuTopology.boxOf(idx)

        while (candidates != 0) {
            val lsb = candidates and -candidates
            val digit = Integer.numberOfTrailingZeros(lsb) + 1
            candidates = candidates xor lsb

            work[idx] = digit
            rowMask[r] = rowMask[r] or lsb
            colMask[c] = colMask[c] or lsb
            boxMask[b] = boxMask[b] or lsb

            if (solveDeterministic(work, rowMask, colMask, boxMask)) return true

            work[idx] = 0
            rowMask[r] = rowMask[r] xor lsb
            colMask[c] = colMask[c] xor lsb
            boxMask[b] = boxMask[b] xor lsb
        }
        return false
    }

    private fun solveRandomized(
        work: IntArray,
        rowMask: IntArray,
        colMask: IntArray,
        boxMask: IntArray,
        random: Random,
        digitScratch: IntArray,
    ): Boolean {
        val packed = findBestEmptyCell(work, rowMask, colMask, boxMask)
        if (packed == -1L) return false
        if (packed == Long.MAX_VALUE) return true

        val idx = (packed ushr 16).toInt()
        var candidates = (packed and 0xFFFFL).toInt()
        val r = SudokuTopology.rowOf(idx)
        val c = SudokuTopology.colOf(idx)
        val b = SudokuTopology.boxOf(idx)

        var n = 0
        while (candidates != 0) {
            val lsb = candidates and -candidates
            digitScratch[n++] = Integer.numberOfTrailingZeros(lsb) + 1
            candidates = candidates xor lsb
        }
        // Shuffle local candidate slice; copy locally because recursion reuses digitScratch
        val localOrder = IntArray(n) { digitScratch[it] }
        for (i in n - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = localOrder[i]
            localOrder[i] = localOrder[j]
            localOrder[j] = tmp
        }

        for (digit in localOrder) {
            val bit = SudokuTopology.digitBit(digit)
            work[idx] = digit
            rowMask[r] = rowMask[r] or bit
            colMask[c] = colMask[c] or bit
            boxMask[b] = boxMask[b] or bit

            if (solveRandomized(work, rowMask, colMask, boxMask, random, digitScratch)) return true

            work[idx] = 0
            rowMask[r] = rowMask[r] xor bit
            colMask[c] = colMask[c] xor bit
            boxMask[b] = boxMask[b] xor bit
        }
        return false
    }
}
