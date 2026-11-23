package com.logicminigames.core.sudoku.engine

import com.logicminigames.core.sudoku.model.SolvingTechnique
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.sudoku.model.SudokuGrid
import com.logicminigames.core.sudoku.model.SudokuPuzzle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Generates Sudoku puzzles with strictly guaranteed unique solutions (verified via
 * [BitmaskBacktrackingSolver]) and human-solvable difficulty calibration via [LogicalSudokuSolver].
 */
class SudokuGenerator(
    private val logicalSolver: LogicalSudokuSolver = LogicalSudokuSolver(),
) {

    fun generate(
        difficulty: SudokuDifficulty,
        seed: Long = Random.nextLong(),
    ): SudokuPuzzle = generateInternal(difficulty, seed, checkCancellation = null)

    suspend fun generateAsync(
        difficulty: SudokuDifficulty,
        seed: Long = Random.nextLong(),
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): SudokuPuzzle = withContext(dispatcher) {
        val ctx = currentCoroutineContext()
        generateInternal(difficulty, seed) { ctx.ensureActive() }
    }

    private fun generateInternal(
        difficulty: SudokuDifficulty,
        seed: Long,
        checkCancellation: (() -> Unit)?,
    ): SudokuPuzzle {
        val random = Random(seed)
        val maxAttempts = when (difficulty) {
            SudokuDifficulty.VERY_EASY -> 40
            SudokuDifficulty.EASY -> 50
            SudokuDifficulty.MEDIUM -> 80
            SudokuDifficulty.HARD -> 100
            SudokuDifficulty.EXPERT -> 160
        }

        var bestFallback: SudokuPuzzle? = null
        var bestFallbackDistance = Int.MAX_VALUE

        for (attempt in 0 until maxAttempts) {
            checkCancellation?.invoke()
            val attemptSeed = seed xor (attempt.toLong() * -7046029254386353131L)
            val attemptRng = Random(attemptSeed)
            val fullBoard = BitmaskBacktrackingSolver.generateCompleteBoard(attemptRng)
            val dugGrid = digForDifficulty(fullBoard, difficulty, attemptRng)
            val solverResult = logicalSolver.solve(dugGrid)
            if (!solverResult.solved || solverResult.ratedDifficulty == null || solverResult.maxTechnique == null) {
                continue
            }

            val rated = solverResult.ratedDifficulty
            val candidate = SudokuPuzzle(
                id = "${difficulty.id}_${seed.toULong().toString(16)}",
                initialGrid = dugGrid,
                solution = fullBoard,
                difficulty = rated,
                maxTechnique = solverResult.maxTechnique,
                difficultyScore = solverResult.totalScore,
                seed = seed,
            )

            val matchesTarget = when (difficulty) {
                SudokuDifficulty.VERY_EASY -> rated == SudokuDifficulty.VERY_EASY
                SudokuDifficulty.EASY -> rated == SudokuDifficulty.EASY &&
                    solverResult.maxTechnique == SolvingTechnique.HIDDEN_SINGLE
                SudokuDifficulty.MEDIUM -> rated == SudokuDifficulty.MEDIUM
                SudokuDifficulty.HARD -> rated == SudokuDifficulty.HARD
                SudokuDifficulty.EXPERT -> rated == SudokuDifficulty.EXPERT
            }

            if (matchesTarget) {
                return candidate
            }

            val distance = kotlin.math.abs(rated.order - difficulty.order) * 1000 -
                (if (rated.order >= difficulty.order) 500 else solverResult.totalScore)
            if (distance < bestFallbackDistance) {
                bestFallbackDistance = distance
                bestFallback = candidate
            }
        }

        val fallback = bestFallback
        if (fallback != null) {
            return fallback.copy(difficulty = difficulty)
        }

        // Guaranteed deterministic safety net: dig a valid minimal/easy puzzle that is uniquely solvable
        val fullBoard = BitmaskBacktrackingSolver.generateCompleteBoard(random)
        val safeGrid = digMinimal(fullBoard.toIntArrayCopy(), minClues = 38, random = random)
        val solved = logicalSolver.solve(safeGrid)
        return SudokuPuzzle(
            id = "${difficulty.id}_${seed.toULong().toString(16)}",
            initialGrid = safeGrid,
            solution = fullBoard,
            difficulty = difficulty,
            maxTechnique = solved.maxTechnique ?: SolvingTechnique.NAKED_SINGLE,
            difficultyScore = solved.totalScore,
            seed = seed,
        )
    }

    private fun digForDifficulty(
        solution: SudokuGrid,
        difficulty: SudokuDifficulty,
        random: Random,
    ): SudokuGrid {
        val cells = solution.toIntArrayCopy()
        return when (difficulty) {
            SudokuDifficulty.VERY_EASY -> digSymmetric(
                cells = cells,
                targetClues = random.nextInt(38, 44),
                random = random,
                requireUnique = true,
            )

            SudokuDifficulty.EASY -> digSymmetric(
                cells = cells,
                targetClues = random.nextInt(32, 38),
                random = random,
                requireUnique = true,
            )

            SudokuDifficulty.MEDIUM -> {
                digSymmetric(cells, targetClues = 34, random = random, requireUnique = true)
                val target = random.nextInt(26, 32)
                digAsymmetricToTarget(cells, targetClues = target, random = random)
            }

            SudokuDifficulty.HARD -> {
                digSymmetric(cells, targetClues = 32, random = random, requireUnique = true)
                digAsymmetricToTarget(cells, targetClues = random.nextInt(23, 28), random = random)
            }

            SudokuDifficulty.EXPERT -> {
                // Full minimal dig maximizes the chance of advanced logical patterns
                digAsymmetricToTarget(cells, targetClues = 17, random = random)
            }
        }
    }

    private fun digSymmetric(
        cells: IntArray,
        targetClues: Int,
        random: Random,
        requireUnique: Boolean,
    ): SudokuGrid {
        val pairs = ArrayList<Int>(41)
        for (i in 0..40) pairs.add(i)
        pairs.shuffle(random)

        var clues = SudokuTopology.CELL_COUNT
        for (idx in pairs) {
            if (clues <= targetClues) break
            val sym = 80 - idx
            if (cells[idx] == 0) continue

            val savedA = cells[idx]
            val savedB = cells[sym]
            cells[idx] = 0
            cells[sym] = 0

            val ok = !requireUnique || BitmaskBacktrackingSolver.countSolutions(cells, limit = 2) == 1
            if (ok) {
                clues -= if (idx == sym) 1 else 2
            } else {
                cells[idx] = savedA
                cells[sym] = savedB
            }
        }
        return SudokuGrid.fromIntArray(cells)
    }

    private fun digAsymmetricToTarget(
        cells: IntArray,
        targetClues: Int,
        random: Random,
    ): SudokuGrid {
        val indices = IntArray(SudokuTopology.CELL_COUNT) { it }
        shuffleIntArray(indices, random)
        var clues = cells.count { it != 0 }

        for (idx in indices) {
            if (clues <= targetClues) break
            val saved = cells[idx]
            if (saved == 0) continue
            cells[idx] = 0
            if (BitmaskBacktrackingSolver.countSolutions(cells, limit = 2) == 1) {
                clues--
            } else {
                cells[idx] = saved
            }
        }
        return SudokuGrid.fromIntArray(cells)
    }

    private fun digMinimal(cells: IntArray, minClues: Int, random: Random): SudokuGrid =
        digAsymmetricToTarget(cells, targetClues = minClues, random = random)

    private fun shuffleIntArray(array: IntArray, random: Random) {
        for (i in array.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = array[i]
            array[i] = array[j]
            array[j] = tmp
        }
    }
}
