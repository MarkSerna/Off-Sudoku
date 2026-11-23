package com.logicminigames.core.sudoku.engine

import com.logicminigames.core.sudoku.model.CandidateElimination
import com.logicminigames.core.sudoku.model.CellCoord
import com.logicminigames.core.sudoku.model.LogicalStep
import com.logicminigames.core.sudoku.model.SolverResult
import com.logicminigames.core.sudoku.model.SolvingTechnique
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.sudoku.model.SudokuGrid
import com.logicminigames.core.sudoku.model.UnitType

/**
 * Human-style logical Sudoku solver supporting 12 deductive techniques in strict ascending
 * difficulty order. Drives both puzzle difficulty calibration and live explanatory hints.
 */
class LogicalSudokuSolver {

    fun solve(grid: SudokuGrid): SolverResult {
        if (grid.hasConflicts()) {
            return unsolvedResult(emptyList())
        }
        val values = grid.toIntArrayCopy()
        val candidates = computeLegalCandidates(values)
        if (!hasConsistentEmptyCells(values, candidates)) {
            return unsolvedResult(emptyList())
        }

        val steps = ArrayList<LogicalStep>(96)
        while (!isBoardComplete(values)) {
            val step = findNextStepInternal(values, candidates) ?: break
            steps.add(step)
            if (step.isPlacement) {
                val idx = step.targetCell!!.index
                val digit = step.digit!!
                placeDigit(values, candidates, idx, digit)
            } else {
                for (elim in step.eliminations) {
                    val idx = elim.cell.index
                    candidates[idx] = candidates[idx] and SudokuTopology.digitBit(elim.digit).inv()
                }
            }
            if (!hasConsistentEmptyCells(values, candidates)) {
                return unsolvedResult(steps)
            }
        }

        val solved = isBoardComplete(values)
        val solutionGrid = if (solved) SudokuGrid.fromIntArray(values) else null
        val maxTechnique = steps.maxByOrNull { it.technique.weight }?.technique
        val totalScore = steps.sumOf { it.technique.weight }
        val ratedDifficulty = if (solved && solutionGrid != null && solutionGrid.isValidSolution()) {
            DifficultyRater.rate(
                clueCount = grid.filledCount(),
                steps = steps,
                maxTechnique = maxTechnique,
                totalScore = totalScore,
            )
        } else {
            null
        }

        return SolverResult(
            solved = solved && ratedDifficulty != null,
            solution = solutionGrid,
            steps = steps,
            maxTechnique = maxTechnique,
            totalScore = totalScore,
            ratedDifficulty = ratedDifficulty,
        )
    }

    fun findNextStep(grid: SudokuGrid): LogicalStep? =
        findNextStepWithCandidates(grid, customCandidateMasks = null)

    /**
     * Finds the easiest valid logical step from [grid].
     * When [customCandidateMasks] is provided (e.g. unit testing isolated candidate patterns),
     * legal candidates for empty cells are intersected with non-zero custom masks.
     */
    fun findNextStepWithCandidates(
        grid: SudokuGrid,
        customCandidateMasks: IntArray? = null,
    ): LogicalStep? {
        if (grid.hasConflicts()) return null
        val values = grid.toIntArrayCopy()
        val candidates = computeLegalCandidates(values)
        if (customCandidateMasks != null) {
            require(customCandidateMasks.size == SudokuTopology.CELL_COUNT) {
                "customCandidateMasks must have length 81"
            }
            for (idx in 0 until SudokuTopology.CELL_COUNT) {
                if (values[idx] == 0 && customCandidateMasks[idx] != 0) {
                    candidates[idx] = candidates[idx] and customCandidateMasks[idx]
                }
            }
        }
        if (!hasConsistentEmptyCells(values, candidates)) return null
        return findNextStepInternal(values, candidates)
    }

    private fun unsolvedResult(steps: List<LogicalStep>): SolverResult = SolverResult(
        solved = false,
        solution = null,
        steps = steps,
        maxTechnique = steps.maxByOrNull { it.technique.weight }?.technique,
        totalScore = steps.sumOf { it.technique.weight },
        ratedDifficulty = null,
    )

    private fun computeLegalCandidates(values: IntArray): IntArray {
        val candidates = IntArray(SudokuTopology.CELL_COUNT)
        for (idx in 0 until SudokuTopology.CELL_COUNT) {
            if (values[idx] != 0) continue
            var used = 0
            for (peer in SudokuTopology.PEERS[idx]) {
                val peerVal = values[peer]
                if (peerVal != 0) {
                    used = used or SudokuTopology.digitBit(peerVal)
                }
            }
            candidates[idx] = SudokuTopology.ALL_DIGITS_MASK and used.inv()
        }
        return candidates
    }

    private fun hasConsistentEmptyCells(values: IntArray, candidates: IntArray): Boolean {
        for (idx in 0 until SudokuTopology.CELL_COUNT) {
            if (values[idx] == 0 && candidates[idx] == 0) return false
        }
        return true
    }

    private fun isBoardComplete(values: IntArray): Boolean {
        for (v in values) if (v == 0) return false
        return true
    }

    private fun placeDigit(values: IntArray, candidates: IntArray, idx: Int, digit: Int) {
        values[idx] = digit
        candidates[idx] = 0
        val clearBitInv = SudokuTopology.digitBit(digit).inv()
        for (peer in SudokuTopology.PEERS[idx]) {
            if (values[peer] == 0) {
                candidates[peer] = candidates[peer] and clearBitInv
            }
        }
    }

    private fun findNextStepInternal(values: IntArray, candidates: IntArray): LogicalStep? {
        return findNakedSingle(values, candidates)
            ?: findHiddenSingle(values, candidates)
            ?: findPointingCandidates(values, candidates)
            ?: findBoxLineReduction(values, candidates)
            ?: findNakedSubset(values, candidates, size = 2, SolvingTechnique.NAKED_PAIR)
            ?: findHiddenSubset(values, candidates, size = 2, SolvingTechnique.HIDDEN_PAIR)
            ?: findNakedSubset(values, candidates, size = 3, SolvingTechnique.NAKED_TRIPLE)
            ?: findHiddenSubset(values, candidates, size = 3, SolvingTechnique.HIDDEN_TRIPLE)
            ?: findFish(values, candidates, size = 2, SolvingTechnique.X_WING)
            ?: findXyWing(values, candidates)
            ?: findNakedSubset(values, candidates, size = 4, SolvingTechnique.NAKED_QUAD)
            ?: findFish(values, candidates, size = 3, SolvingTechnique.SWORDFISH)
    }

    private fun findNakedSingle(values: IntArray, candidates: IntArray): LogicalStep? {
        for (idx in 0 until SudokuTopology.CELL_COUNT) {
            if (values[idx] != 0) continue
            val mask = candidates[idx]
            if (Integer.bitCount(mask) == 1) {
                val digit = Integer.numberOfTrailingZeros(mask) + 1
                val coord = CellCoord.fromIndex(idx)
                return LogicalStep(
                    technique = SolvingTechnique.NAKED_SINGLE,
                    targetCell = coord,
                    digit = digit,
                    relatedCells = setOf(coord),
                )
            }
        }
        return null
    }

    private fun findHiddenSingle(values: IntArray, candidates: IntArray): LogicalStep? {
        for (unit in SudokuTopology.ALL_UNITS) {
            for (digit in 1..9) {
                val bit = SudokuTopology.digitBit(digit)
                var count = 0
                var targetIdx = -1
                var alreadyPlaced = false
                for (cellIdx in unit.cells) {
                    if (values[cellIdx] == digit) {
                        alreadyPlaced = true
                        break
                    }
                    if (values[cellIdx] == 0 && (candidates[cellIdx] and bit) != 0) {
                        count++
                        targetIdx = cellIdx
                        if (count > 1) break
                    }
                }
                if (!alreadyPlaced && count == 1) {
                    val coord = CellCoord.fromIndex(targetIdx)
                    val related = unit.cells.mapTo(LinkedHashSet(9)) { CellCoord.fromIndex(it) }
                    return LogicalStep(
                        technique = SolvingTechnique.HIDDEN_SINGLE,
                        targetCell = coord,
                        digit = digit,
                        relatedCells = related,
                        unitType = unit.type,
                        unitIndex = unit.index,
                    )
                }
            }
        }
        return null
    }

    private fun findPointingCandidates(values: IntArray, candidates: IntArray): LogicalStep? {
        for (box in 0 until SudokuTopology.GRID_SIZE) {
            val boxCells = SudokuTopology.BOX_UNITS[box]
            for (digit in 1..9) {
                val bit = SudokuTopology.digitBit(digit)
                var rowMask = 0
                var colMask = 0
                var count = 0
                val pointingCells = ArrayList<CellCoord>(3)
                for (idx in boxCells) {
                    if (values[idx] == 0 && (candidates[idx] and bit) != 0) {
                        count++
                        rowMask = rowMask or (1 shl SudokuTopology.rowOf(idx))
                        colMask = colMask or (1 shl SudokuTopology.colOf(idx))
                        pointingCells.add(CellCoord.fromIndex(idx))
                    }
                }
                if (count < 2) continue

                if (Integer.bitCount(rowMask) == 1) {
                    val row = Integer.numberOfTrailingZeros(rowMask)
                    val elims = collectLineEliminationsOutsideBox(
                        lineCells = SudokuTopology.ROW_UNITS[row],
                        excludedBox = box,
                        digit = digit,
                        bit = bit,
                        values = values,
                        candidates = candidates,
                    )
                    if (elims.isNotEmpty()) {
                        return LogicalStep(
                            technique = SolvingTechnique.POINTING_CANDIDATES,
                            digit = digit,
                            eliminations = elims,
                            relatedCells = pointingCells.toSet(),
                            unitType = UnitType.BOX,
                            unitIndex = box,
                        )
                    }
                }

                if (Integer.bitCount(colMask) == 1) {
                    val col = Integer.numberOfTrailingZeros(colMask)
                    val elims = collectLineEliminationsOutsideBox(
                        lineCells = SudokuTopology.COL_UNITS[col],
                        excludedBox = box,
                        digit = digit,
                        bit = bit,
                        values = values,
                        candidates = candidates,
                    )
                    if (elims.isNotEmpty()) {
                        return LogicalStep(
                            technique = SolvingTechnique.POINTING_CANDIDATES,
                            digit = digit,
                            eliminations = elims,
                            relatedCells = pointingCells.toSet(),
                            unitType = UnitType.BOX,
                            unitIndex = box,
                        )
                    }
                }
            }
        }
        return null
    }

    private fun collectLineEliminationsOutsideBox(
        lineCells: IntArray,
        excludedBox: Int,
        digit: Int,
        bit: Int,
        values: IntArray,
        candidates: IntArray,
    ): List<CandidateElimination> {
        val elims = ArrayList<CandidateElimination>()
        for (idx in lineCells) {
            if (SudokuTopology.boxOf(idx) != excludedBox &&
                values[idx] == 0 &&
                (candidates[idx] and bit) != 0
            ) {
                elims.add(CandidateElimination(CellCoord.fromIndex(idx), digit))
            }
        }
        return elims
    }

    private fun findBoxLineReduction(values: IntArray, candidates: IntArray): LogicalStep? {
        for (row in 0 until SudokuTopology.GRID_SIZE) {
            val step = findBoxLineInUnit(
                unitCells = SudokuTopology.ROW_UNITS[row],
                unitType = UnitType.ROW,
                unitIndex = row,
                values = values,
                candidates = candidates,
            )
            if (step != null) return step
        }
        for (col in 0 until SudokuTopology.GRID_SIZE) {
            val step = findBoxLineInUnit(
                unitCells = SudokuTopology.COL_UNITS[col],
                unitType = UnitType.COLUMN,
                unitIndex = col,
                values = values,
                candidates = candidates,
            )
            if (step != null) return step
        }
        return null
    }

    private fun findBoxLineInUnit(
        unitCells: IntArray,
        unitType: UnitType,
        unitIndex: Int,
        values: IntArray,
        candidates: IntArray,
    ): LogicalStep? {
        for (digit in 1..9) {
            val bit = SudokuTopology.digitBit(digit)
            var boxMask = 0
            var count = 0
            val lineCells = ArrayList<CellCoord>(3)
            for (idx in unitCells) {
                if (values[idx] == 0 && (candidates[idx] and bit) != 0) {
                    count++
                    boxMask = boxMask or (1 shl SudokuTopology.boxOf(idx))
                    lineCells.add(CellCoord.fromIndex(idx))
                }
            }
            if (count in 2..3 && Integer.bitCount(boxMask) == 1) {
                val box = Integer.numberOfTrailingZeros(boxMask)
                val elims = ArrayList<CandidateElimination>()
                for (boxCell in SudokuTopology.BOX_UNITS[box]) {
                    val sameLine = if (unitType == UnitType.ROW) {
                        SudokuTopology.rowOf(boxCell) == unitIndex
                    } else {
                        SudokuTopology.colOf(boxCell) == unitIndex
                    }
                    if (!sameLine && values[boxCell] == 0 && (candidates[boxCell] and bit) != 0) {
                        elims.add(CandidateElimination(CellCoord.fromIndex(boxCell), digit))
                    }
                }
                if (elims.isNotEmpty()) {
                    return LogicalStep(
                        technique = SolvingTechnique.BOX_LINE_REDUCTION,
                        digit = digit,
                        eliminations = elims,
                        relatedCells = lineCells.toSet(),
                        unitType = unitType,
                        unitIndex = unitIndex,
                    )
                }
            }
        }
        return null
    }

    private fun findNakedSubset(
        values: IntArray,
        candidates: IntArray,
        size: Int,
        technique: SolvingTechnique,
    ): LogicalStep? {
        for (unit in SudokuTopology.ALL_UNITS) {
            val eligible = IntArray(9)
            var eligibleCount = 0
            for (idx in unit.cells) {
                if (values[idx] == 0) {
                    val cCount = Integer.bitCount(candidates[idx])
                    if (cCount in 2..size) {
                        eligible[eligibleCount++] = idx
                    }
                }
            }
            if (eligibleCount < size) continue

            val chosen = IntArray(size)
            val found = searchNakedSubset(
                unit = unit,
                eligible = eligible,
                eligibleCount = eligibleCount,
                size = size,
                startPos = 0,
                depth = 0,
                unionMask = 0,
                chosen = chosen,
                values = values,
                candidates = candidates,
                technique = technique,
            )
            if (found != null) return found
        }
        return null
    }

    private fun searchNakedSubset(
        unit: SudokuTopology.UnitRef,
        eligible: IntArray,
        eligibleCount: Int,
        size: Int,
        startPos: Int,
        depth: Int,
        unionMask: Int,
        chosen: IntArray,
        values: IntArray,
        candidates: IntArray,
        technique: SolvingTechnique,
    ): LogicalStep? {
        if (depth == size) {
            if (Integer.bitCount(unionMask) != size) return null
            val elims = ArrayList<CandidateElimination>()
            for (cellIdx in unit.cells) {
                if (values[cellIdx] != 0) continue
                var isInSubset = false
                for (i in 0 until size) {
                    if (chosen[i] == cellIdx) {
                        isInSubset = true
                        break
                    }
                }
                if (isInSubset) continue
                val overlap = candidates[cellIdx] and unionMask
                if (overlap != 0) {
                    val coord = CellCoord.fromIndex(cellIdx)
                    SudokuTopology.forEachDigitInMask(overlap) { d ->
                        elims.add(CandidateElimination(coord, d))
                    }
                }
            }
            if (elims.isNotEmpty()) {
                val related = chosen.mapTo(LinkedHashSet(size)) { CellCoord.fromIndex(it) }
                return LogicalStep(
                    technique = technique,
                    eliminations = elims,
                    relatedCells = related,
                    unitType = unit.type,
                    unitIndex = unit.index,
                )
            }
            return null
        }

        val limit = eligibleCount - (size - depth)
        for (i in startPos..limit) {
            val cellIdx = eligible[i]
            val nextUnion = unionMask or candidates[cellIdx]
            if (Integer.bitCount(nextUnion) > size) continue
            chosen[depth] = cellIdx
            val step = searchNakedSubset(
                unit = unit,
                eligible = eligible,
                eligibleCount = eligibleCount,
                size = size,
                startPos = i + 1,
                depth = depth + 1,
                unionMask = nextUnion,
                chosen = chosen,
                values = values,
                candidates = candidates,
                technique = technique,
            )
            if (step != null) return step
        }
        return null
    }

    private fun findHiddenSubset(
        values: IntArray,
        candidates: IntArray,
        size: Int,
        technique: SolvingTechnique,
    ): LogicalStep? {
        for (unit in SudokuTopology.ALL_UNITS) {
            val digitPosMasks = IntArray(10)
            for (pos in 0 until 9) {
                val idx = unit.cells[pos]
                if (values[idx] != 0) continue
                SudokuTopology.forEachDigitInMask(candidates[idx]) { digit ->
                    digitPosMasks[digit] = digitPosMasks[digit] or (1 shl pos)
                }
            }

            val eligibleDigits = IntArray(9)
            var eligibleCount = 0
            for (d in 1..9) {
                val count = Integer.bitCount(digitPosMasks[d])
                if (count in 2..size) {
                    eligibleDigits[eligibleCount++] = d
                }
            }
            if (eligibleCount < size) continue

            val chosenDigits = IntArray(size)
            val step = searchHiddenSubset(
                unit = unit,
                digitPosMasks = digitPosMasks,
                eligibleDigits = eligibleDigits,
                eligibleCount = eligibleCount,
                size = size,
                startIdx = 0,
                depth = 0,
                posUnion = 0,
                chosenDigits = chosenDigits,
                candidates = candidates,
                technique = technique,
            )
            if (step != null) return step
        }
        return null
    }

    private fun searchHiddenSubset(
        unit: SudokuTopology.UnitRef,
        digitPosMasks: IntArray,
        eligibleDigits: IntArray,
        eligibleCount: Int,
        size: Int,
        startIdx: Int,
        depth: Int,
        posUnion: Int,
        chosenDigits: IntArray,
        candidates: IntArray,
        technique: SolvingTechnique,
    ): LogicalStep? {
        if (depth == size) {
            if (Integer.bitCount(posUnion) != size) return null
            var keepMask = 0
            for (i in 0 until size) {
                keepMask = keepMask or SudokuTopology.digitBit(chosenDigits[i])
            }
            val elims = ArrayList<CandidateElimination>()
            val related = LinkedHashSet<CellCoord>(size)
            for (pos in 0 until 9) {
                if ((posUnion and (1 shl pos)) == 0) continue
                val cellIdx = unit.cells[pos]
                val coord = CellCoord.fromIndex(cellIdx)
                related.add(coord)
                val extraMask = candidates[cellIdx] and keepMask.inv()
                if (extraMask != 0) {
                    SudokuTopology.forEachDigitInMask(extraMask) { d ->
                        elims.add(CandidateElimination(coord, d))
                    }
                }
            }
            if (elims.isNotEmpty()) {
                return LogicalStep(
                    technique = technique,
                    eliminations = elims,
                    relatedCells = related,
                    unitType = unit.type,
                    unitIndex = unit.index,
                )
            }
            return null
        }

        val limit = eligibleCount - (size - depth)
        for (i in startIdx..limit) {
            val digit = eligibleDigits[i]
            val nextPosUnion = posUnion or digitPosMasks[digit]
            if (Integer.bitCount(nextPosUnion) > size) continue
            chosenDigits[depth] = digit
            val step = searchHiddenSubset(
                unit = unit,
                digitPosMasks = digitPosMasks,
                eligibleDigits = eligibleDigits,
                eligibleCount = eligibleCount,
                size = size,
                startIdx = i + 1,
                depth = depth + 1,
                posUnion = nextPosUnion,
                chosenDigits = chosenDigits,
                candidates = candidates,
                technique = technique,
            )
            if (step != null) return step
        }
        return null
    }

    private fun findFish(
        values: IntArray,
        candidates: IntArray,
        size: Int,
        technique: SolvingTechnique,
    ): LogicalStep? {
        for (digit in 1..9) {
            val bit = SudokuTopology.digitBit(digit)
            findFishOrientation(
                digit = digit,
                bit = bit,
                size = size,
                technique = technique,
                byRow = true,
                values = values,
                candidates = candidates,
            )?.let { return it }

            findFishOrientation(
                digit = digit,
                bit = bit,
                size = size,
                technique = technique,
                byRow = false,
                values = values,
                candidates = candidates,
            )?.let { return it }
        }
        return null
    }

    private fun findFishOrientation(
        digit: Int,
        bit: Int,
        size: Int,
        technique: SolvingTechnique,
        byRow: Boolean,
        values: IntArray,
        candidates: IntArray,
    ): LogicalStep? {
        val baseLines = IntArray(9)
        val crossMasks = IntArray(9)
        var baseCount = 0

        for (primary in 0 until 9) {
            var crossMask = 0
            val lineCells = if (byRow) SudokuTopology.ROW_UNITS[primary] else SudokuTopology.COL_UNITS[primary]
            for (secondary in 0 until 9) {
                val idx = lineCells[secondary]
                if (values[idx] == 0 && (candidates[idx] and bit) != 0) {
                    crossMask = crossMask or (1 shl secondary)
                }
            }
            val cCount = Integer.bitCount(crossMask)
            if (cCount in 2..size) {
                baseLines[baseCount] = primary
                crossMasks[baseCount] = crossMask
                baseCount++
            }
        }
        if (baseCount < size) return null

        val chosenIdx = IntArray(size)
        return searchFish(
            digit = digit,
            bit = bit,
            size = size,
            technique = technique,
            byRow = byRow,
            baseLines = baseLines,
            crossMasks = crossMasks,
            baseCount = baseCount,
            startPos = 0,
            depth = 0,
            crossUnion = 0,
            chosenIdx = chosenIdx,
            values = values,
            candidates = candidates,
        )
    }

    private fun searchFish(
        digit: Int,
        bit: Int,
        size: Int,
        technique: SolvingTechnique,
        byRow: Boolean,
        baseLines: IntArray,
        crossMasks: IntArray,
        baseCount: Int,
        startPos: Int,
        depth: Int,
        crossUnion: Int,
        chosenIdx: IntArray,
        values: IntArray,
        candidates: IntArray,
    ): LogicalStep? {
        if (depth == size) {
            if (Integer.bitCount(crossUnion) != size) return null
            var baseMask = 0
            for (i in 0 until size) {
                baseMask = baseMask or (1 shl baseLines[chosenIdx[i]])
            }

            val elims = ArrayList<CandidateElimination>()
            for (secondary in 0 until 9) {
                if ((crossUnion and (1 shl secondary)) == 0) continue
                val crossUnit = if (byRow) SudokuTopology.COL_UNITS[secondary] else SudokuTopology.ROW_UNITS[secondary]
                for (primary in 0 until 9) {
                    if ((baseMask and (1 shl primary)) != 0) continue
                    val idx = crossUnit[primary]
                    if (values[idx] == 0 && (candidates[idx] and bit) != 0) {
                        elims.add(CandidateElimination(CellCoord.fromIndex(idx), digit))
                    }
                }
            }

            if (elims.isNotEmpty()) {
                val related = LinkedHashSet<CellCoord>()
                for (i in 0 until size) {
                    val primary = baseLines[chosenIdx[i]]
                    val mask = crossMasks[chosenIdx[i]]
                    for (secondary in 0 until 9) {
                        if ((mask and (1 shl secondary)) != 0) {
                            val cellIdx = if (byRow) primary * 9 + secondary else secondary * 9 + primary
                            related.add(CellCoord.fromIndex(cellIdx))
                        }
                    }
                }
                return LogicalStep(
                    technique = technique,
                    digit = digit,
                    eliminations = elims,
                    relatedCells = related,
                )
            }
            return null
        }

        val limit = baseCount - (size - depth)
        for (i in startPos..limit) {
            val nextUnion = crossUnion or crossMasks[i]
            if (Integer.bitCount(nextUnion) > size) continue
            chosenIdx[depth] = i
            val step = searchFish(
                digit = digit,
                bit = bit,
                size = size,
                technique = technique,
                byRow = byRow,
                baseLines = baseLines,
                crossMasks = crossMasks,
                baseCount = baseCount,
                startPos = i + 1,
                depth = depth + 1,
                crossUnion = nextUnion,
                chosenIdx = chosenIdx,
                values = values,
                candidates = candidates,
            )
            if (step != null) return step
        }
        return null
    }

    private fun findXyWing(values: IntArray, candidates: IntArray): LogicalStep? {
        val bivalue = IntArray(SudokuTopology.CELL_COUNT)
        var bivalueCount = 0
        for (idx in 0 until SudokuTopology.CELL_COUNT) {
            if (values[idx] == 0 && Integer.bitCount(candidates[idx]) == 2) {
                bivalue[bivalueCount++] = idx
            }
        }
        if (bivalueCount < 3) return null

        for (i in 0 until bivalueCount) {
            val pivot = bivalue[i]
            val pivotMask = candidates[pivot]
            val peers = SudokuTopology.PEERS[pivot]

            for (a in peers.indices) {
                val p1 = peers[a]
                if (values[p1] != 0 || Integer.bitCount(candidates[p1]) != 2) continue
                val m1 = candidates[p1]
                if (m1 == pivotMask || Integer.bitCount(pivotMask and m1) != 1) continue

                for (b in (a + 1) until peers.size) {
                    val p2 = peers[b]
                    if (values[p2] != 0 || Integer.bitCount(candidates[p2]) != 2) continue
                    val m2 = candidates[p2]
                    if (m2 == pivotMask || m2 == m1 || Integer.bitCount(pivotMask and m2) != 1) continue

                    // p1 and p2 must pair with the two distinct digits of pivot, and share the third digit z
                    val pivotCovered = (pivotMask and m1) or (pivotMask and m2)
                    if (pivotCovered != pivotMask) continue
                    val sharedNonPivot = (m1 and m2) and pivotMask.inv()
                    if (Integer.bitCount(sharedNonPivot) != 1) continue

                    val zDigit = Integer.numberOfTrailingZeros(sharedNonPivot) + 1
                    val elims = ArrayList<CandidateElimination>()
                    for (target in SudokuTopology.PEERS[p1]) {
                        if (target != pivot &&
                            target != p2 &&
                            values[target] == 0 &&
                            (candidates[target] and sharedNonPivot) != 0 &&
                            sees(target, p2)
                        ) {
                            elims.add(CandidateElimination(CellCoord.fromIndex(target), zDigit))
                        }
                    }
                    if (elims.isNotEmpty()) {
                        return LogicalStep(
                            technique = SolvingTechnique.XY_WING,
                            digit = zDigit,
                            eliminations = elims,
                            relatedCells = setOf(
                                CellCoord.fromIndex(pivot),
                                CellCoord.fromIndex(p1),
                                CellCoord.fromIndex(p2),
                            ),
                        )
                    }
                }
            }
        }
        return null
    }

    private fun sees(a: Int, b: Int): Boolean {
        if (a == b) return false
        return SudokuTopology.rowOf(a) == SudokuTopology.rowOf(b) ||
            SudokuTopology.colOf(a) == SudokuTopology.colOf(b) ||
            SudokuTopology.boxOf(a) == SudokuTopology.boxOf(b)
    }
}

object DifficultyRater {
    fun rate(
        clueCount: Int,
        steps: List<LogicalStep>,
        maxTechnique: SolvingTechnique?,
        totalScore: Int,
    ): SudokuDifficulty {
        val maxTech = maxTechnique ?: SolvingTechnique.NAKED_SINGLE
        val intermediateSteps = steps.count {
            it.technique == SolvingTechnique.POINTING_CANDIDATES ||
                it.technique == SolvingTechnique.BOX_LINE_REDUCTION ||
                it.technique == SolvingTechnique.NAKED_PAIR
        }
        val hardSubsetSteps = steps.count {
            it.technique == SolvingTechnique.HIDDEN_PAIR ||
                it.technique == SolvingTechnique.NAKED_TRIPLE ||
                it.technique == SolvingTechnique.HIDDEN_TRIPLE
        }

        return when (maxTech) {
            SolvingTechnique.NAKED_SINGLE -> {
                if (clueCount >= 36) SudokuDifficulty.VERY_EASY else SudokuDifficulty.EASY
            }

            SolvingTechnique.HIDDEN_SINGLE -> {
                SudokuDifficulty.EASY
            }

            SolvingTechnique.POINTING_CANDIDATES,
            SolvingTechnique.BOX_LINE_REDUCTION,
            SolvingTechnique.NAKED_PAIR,
            -> {
                if (intermediateSteps >= 4 || (clueCount <= 28 && totalScore >= 130)) {
                    SudokuDifficulty.HARD
                } else {
                    SudokuDifficulty.MEDIUM
                }
            }

            SolvingTechnique.HIDDEN_PAIR,
            SolvingTechnique.NAKED_TRIPLE,
            SolvingTechnique.HIDDEN_TRIPLE,
            -> {
                if ((hardSubsetSteps >= 2 && totalScore >= 170) ||
                    (clueCount <= 26 && totalScore >= 180)
                ) {
                    SudokuDifficulty.EXPERT
                } else {
                    SudokuDifficulty.HARD
                }
            }

            SolvingTechnique.X_WING,
            SolvingTechnique.XY_WING,
            SolvingTechnique.NAKED_QUAD,
            SolvingTechnique.SWORDFISH,
            -> SudokuDifficulty.EXPERT
        }
    }
}
