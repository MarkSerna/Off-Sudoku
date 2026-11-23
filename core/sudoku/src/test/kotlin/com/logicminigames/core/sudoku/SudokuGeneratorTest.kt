package com.logicminigames.core.sudoku

import com.logicminigames.core.sudoku.engine.BitmaskBacktrackingSolver
import com.logicminigames.core.sudoku.engine.LogicalSudokuSolver
import com.logicminigames.core.sudoku.engine.SudokuGenerator
import com.logicminigames.core.sudoku.engine.SudokuTopology
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuGeneratorTest {

    private val solver = LogicalSudokuSolver()
    private val generator = SudokuGenerator(solver)

    @Test
    fun `generates unique logically-solvable puzzles for all 5 difficulties within fast time bounds`() {
        val samplesPerDifficulty = 3
        val summary = StringBuilder()

        for (difficulty in SudokuDifficulty.entries) {
            var totalNanos = 0L
            for (sample in 0 until samplesPerDifficulty) {
                val seed = 10_000L + difficulty.order * 100L + sample
                val start = System.nanoTime()
                val puzzle = generator.generate(difficulty = difficulty, seed = seed)
                val elapsedNanos = System.nanoTime() - start
                totalNanos += elapsedNanos

                assertTrue(
                    "Solution invalid for $difficulty",
                    puzzle.solution.isValidSolution(),
                )
                assertTrue(
                    "Puzzle $difficulty does not have unique solution",
                    BitmaskBacktrackingSolver.hasUniqueSolution(puzzle.initialGrid),
                )
                for (idx in 0 until SudokuTopology.CELL_COUNT) {
                    val clue = puzzle.initialGrid[idx]
                    if (clue != 0) {
                        assertEquals(
                            "Clue at $idx mismatches solution",
                            puzzle.solution[idx],
                            clue,
                        )
                    }
                }

                val logicalResult = solver.solve(puzzle.initialGrid)
                assertTrue(
                    "Generated puzzle $difficulty must be logically solvable",
                    logicalResult.solved,
                )
                assertEquals(puzzle.solution, logicalResult.solution)
                assertEquals(
                    "Expected rated difficulty $difficulty, got ${logicalResult.ratedDifficulty} (maxTech=${logicalResult.maxTechnique}, score=${logicalResult.totalScore}, clues=${puzzle.initialGrid.filledCount()})",
                    difficulty,
                    logicalResult.ratedDifficulty,
                )
            }

            val avgMs = (totalNanos / samplesPerDifficulty) / 1_000_000.0
            summary.appendLine("$difficulty avgMs=${"%.2f".format(avgMs)}")
            val maxAvgMs = when (difficulty) {
                SudokuDifficulty.VERY_EASY, SudokuDifficulty.EASY -> 300.0
                SudokuDifficulty.MEDIUM -> 800.0
                SudokuDifficulty.HARD -> 1500.0
                SudokuDifficulty.EXPERT -> 3000.0
            }
            assertTrue(
                "Generation too slow for $difficulty: avg=$avgMs ms (limit=$maxAvgMs ms)",
                avgMs <= maxAvgMs,
            )
        }
        println("SudokuGenerator benchmark:\n$summary")
    }

    @Test
    fun `generation is deterministic for identical seed and difficulty`() {
        val first = generator.generate(SudokuDifficulty.MEDIUM, seed = 777_777L)
        val second = generator.generate(SudokuDifficulty.MEDIUM, seed = 777_777L)
        assertEquals(first.initialGrid, second.initialGrid)
        assertEquals(first.solution, second.solution)
        assertEquals(first.difficulty, second.difficulty)
    }
}
