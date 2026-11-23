package com.logicminigames.core.sudoku.engine

import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.sudoku.model.SudokuPuzzle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface PuzzleCacheStore {
    suspend fun pop(difficulty: SudokuDifficulty): SudokuPuzzle?
    suspend fun put(puzzle: SudokuPuzzle)
    suspend fun count(difficulty: SudokuDifficulty): Int
    suspend fun clear()
}

class InMemoryPuzzleCacheStore : PuzzleCacheStore {
    private val mutex = Mutex()
    private val buckets = mutableMapOf<SudokuDifficulty, ArrayDeque<SudokuPuzzle>>()

    override suspend fun pop(difficulty: SudokuDifficulty): SudokuPuzzle? = mutex.withLock {
        buckets[difficulty]?.removeFirstOrNull()
    }

    override suspend fun put(puzzle: SudokuPuzzle) {
        mutex.withLock {
            val queue = buckets.getOrPut(puzzle.difficulty) { ArrayDeque() }
            queue.addLast(puzzle)
        }
    }

    override suspend fun count(difficulty: SudokuDifficulty): Int = mutex.withLock {
        buckets[difficulty]?.size ?: 0
    }

    override suspend fun clear() {
        mutex.withLock {
            buckets.clear()
        }
    }
}

/**
 * Coordinates background puzzle pre-generation so starting a new Sudoku board returns
 * near-instantly while replenishing the next puzzle off the main thread.
 */
class PuzzlePreloader(
    private val generator: SudokuGenerator,
    private val cache: PuzzleCacheStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val jobMutex = Mutex()
    private val activeJobs = mutableMapOf<SudokuDifficulty, Job>()

    suspend fun acquire(
        difficulty: SudokuDifficulty,
        replenishmentScope: CoroutineScope? = null,
    ): SudokuPuzzle {
        val cached = cache.pop(difficulty)
        if (cached != null) {
            replenishmentScope?.let { scope ->
                warmUp(scope, listOf(difficulty), targetPerDifficulty = 1)
            }
            return cached
        }
        val fresh = generator.generateAsync(difficulty, dispatcher = dispatcher)
        replenishmentScope?.let { scope ->
            warmUp(scope, listOf(difficulty), targetPerDifficulty = 1)
        }
        return fresh
    }

    fun warmUp(
        scope: CoroutineScope,
        difficulties: List<SudokuDifficulty> = SudokuDifficulty.entries,
        targetPerDifficulty: Int = 1,
    ) {
        for (difficulty in difficulties) {
            scope.launch(dispatcher) {
                ensureReplenishJob(scope, difficulty, targetPerDifficulty)
            }
        }
    }

    private suspend fun ensureReplenishJob(
        scope: CoroutineScope,
        difficulty: SudokuDifficulty,
        targetPerDifficulty: Int,
    ) {
        val jobToJoin = jobMutex.withLock {
            val existing = activeJobs[difficulty]
            if (existing != null && existing.isActive) {
                return@withLock existing
            }
            val created = scope.launch(dispatcher) {
                try {
                    while (cache.count(difficulty) < targetPerDifficulty) {
                        val puzzle = generator.generateAsync(difficulty, dispatcher = dispatcher)
                        cache.put(puzzle)
                    }
                } finally {
                    jobMutex.withLock {
                        activeJobs.remove(difficulty)
                    }
                }
            }
            activeJobs[difficulty] = created
            created
        }
        jobToJoin.join()
    }
}
