package com.logicminigames.core.data.local

import androidx.room.withTransaction
import com.logicminigames.core.common.GameOutcome
import com.logicminigames.core.common.KnownGames
import com.logicminigames.core.data.logic.AchievementEvaluator
import com.logicminigames.core.data.logic.BoardStateSerializer
import com.logicminigames.core.data.logic.DailyStreakCalculator
import com.logicminigames.core.data.model.AchievementStatus
import com.logicminigames.core.data.model.ActiveGameSummary
import com.logicminigames.core.data.model.DailyStatsSummary
import com.logicminigames.core.data.model.DifficultyStats
import com.logicminigames.core.data.model.SudokuAchievement
import com.logicminigames.core.data.model.VictoryExtras
import com.logicminigames.core.data.repository.ActiveGamePort
import com.logicminigames.core.data.repository.PreferencesPort
import com.logicminigames.core.data.repository.StatsPort
import com.logicminigames.core.sudoku.engine.PuzzleCacheStore
import com.logicminigames.core.sudoku.engine.SudokuGameState
import com.logicminigames.core.sudoku.model.NotesGrid
import com.logicminigames.core.sudoku.model.SolvingTechnique
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.sudoku.model.SudokuGrid
import com.logicminigames.core.sudoku.model.SudokuPuzzle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class RoomPuzzleCacheStore(
    private val dao: PreloadedPuzzleDao,
) : PuzzleCacheStore {

    private val gameId = KnownGames.SUDOKU.value

    override suspend fun pop(difficulty: SudokuDifficulty): SudokuPuzzle? {
        val entity = dao.popNext(gameId, difficulty.id) ?: return null
        return runCatching {
            SudokuPuzzle(
                id = entity.puzzleId,
                initialGrid = SudokuGrid.fromString(entity.initialGrid),
                solution = SudokuGrid.fromString(entity.solutionGrid),
                difficulty = SudokuDifficulty.fromId(entity.difficultyId) ?: difficulty,
                maxTechnique = runCatching { SolvingTechnique.valueOf(entity.maxTechnique) }
                    .getOrDefault(SolvingTechnique.NAKED_SINGLE),
                difficultyScore = entity.difficultyScore,
                seed = entity.seed,
                dailyEpochDay = null,
            )
        }.getOrNull()
    }

    override suspend fun put(puzzle: SudokuPuzzle) {
        dao.insert(
            PreloadedPuzzleEntity(
                gameId = gameId,
                difficultyId = puzzle.difficulty.id,
                puzzleId = puzzle.id,
                initialGrid = puzzle.initialGrid.toFlatString(),
                solutionGrid = puzzle.solution.toFlatString(),
                maxTechnique = puzzle.maxTechnique.name,
                difficultyScore = puzzle.difficultyScore,
                seed = puzzle.seed,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun count(difficulty: SudokuDifficulty): Int =
        dao.count(gameId, difficulty.id)

    override suspend fun clear() {
        dao.deleteAllForGame(gameId)
    }
}

class RoomSudokuDataRepository(
    private val db: MinigamesDatabase,
    private val preferencesPort: PreferencesPort,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) : ActiveGamePort, StatsPort {

    private val gameId = KnownGames.SUDOKU.value
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val activeGameSummary: StateFlow<ActiveGameSummary?> =
        db.activeGameDao().observe(gameId)
            .map { entity -> entity?.toSummary() }
            .stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun loadActiveGame(): SudokuGameState? {
        val entity = db.activeGameDao().get(gameId) ?: return null
        return runCatching { entity.toDomainState() }.getOrNull()
    }

    override suspend fun saveActiveGame(state: SudokuGameState) {
        if (state.isCompleted) {
            db.activeGameDao().delete(gameId)
            return
        }
        val entity = ActiveGameEntity(
            gameId = gameId,
            puzzleId = state.puzzle.id,
            difficultyId = state.puzzle.difficulty.id,
            initialGrid = state.puzzle.initialGrid.toFlatString(),
            solutionGrid = state.puzzle.solution.toFlatString(),
            currentGrid = state.currentGrid.toFlatString(),
            notesSerialized = state.notes.toSerializedString(),
            undoStackJson = BoardStateSerializer.encodeSnapshots(state.undoStack),
            redoStackJson = BoardStateSerializer.encodeSnapshots(state.redoStack),
            maxTechnique = state.puzzle.maxTechnique.name,
            difficultyScore = state.puzzle.difficultyScore,
            seed = state.puzzle.seed,
            dailyEpochDay = state.puzzle.dailyEpochDay,
            mistakes = state.mistakes,
            hintsUsed = state.hintsUsed,
            elapsedSeconds = state.elapsedSeconds,
            updatedAtEpochMillis = clockMillis(),
        )
        db.activeGameDao().upsert(entity)
    }

    override suspend fun clearActiveGame() {
        db.activeGameDao().delete(gameId)
    }

    override suspend fun recordGameStarted(difficulty: SudokuDifficulty, abandonedPrevious: Boolean) {
        db.withTransaction {
            val statsDao = db.difficultyStatsDao()
            if (abandonedPrevious) {
                val previous = db.activeGameDao().get(gameId)
                if (previous != null) {
                    val prevStats = statsDao.get(gameId, previous.difficultyId)
                    if (prevStats != null && prevStats.currentWinStreak != 0) {
                        statsDao.upsert(prevStats.copy(currentWinStreak = 0))
                    }
                }
            }
            val current = statsDao.get(gameId, difficulty.id) ?: emptyStatsEntity(difficulty.id)
            statsDao.upsert(current.copy(gamesPlayed = current.gamesPlayed + 1))
        }
    }

    override suspend fun recordCompletedGame(
        outcome: GameOutcome,
        dailyEpochDay: Long?,
    ): VictoryExtras = db.withTransaction {
        val statsDao = db.difficultyStatsDao()
        val existing = statsDao.get(gameId, outcome.difficultyId) ?: emptyStatsEntity(outcome.difficultyId)
        val newWon = existing.gamesWon + if (outcome.won) 1 else 0
        val newPlayed = maxOf(existing.gamesPlayed, newWon)
        val isNewBest = outcome.won &&
            (existing.bestTimeSeconds == null || outcome.elapsedSeconds < existing.bestTimeSeconds)
        val newBestTime = when {
            !outcome.won -> existing.bestTimeSeconds
            existing.bestTimeSeconds == null -> outcome.elapsedSeconds
            else -> minOf(existing.bestTimeSeconds, outcome.elapsedSeconds)
        }
        val newCurrentStreak = if (outcome.won) existing.currentWinStreak + 1 else 0
        val newBestStreak = maxOf(existing.bestWinStreak, newCurrentStreak)

        statsDao.upsert(
            existing.copy(
                gamesPlayed = newPlayed,
                gamesWon = newWon,
                bestTimeSeconds = newBestTime,
                totalWinTimeSeconds = existing.totalWinTimeSeconds + if (outcome.won) outcome.elapsedSeconds else 0L,
                currentWinStreak = newCurrentStreak,
                bestWinStreak = newBestStreak,
                flawlessWins = existing.flawlessWins + if (outcome.won && outcome.mistakes == 0) 1 else 0,
                noHintWins = existing.noHintWins + if (outcome.won && outcome.hintsUsed == 0) 1 else 0,
            ),
        )

        if (outcome.won && dailyEpochDay != null) {
            db.dailyRecordDao().insertIfAbsent(
                DailyRecordEntity(
                    gameId = gameId,
                    epochDay = dailyEpochDay,
                    difficultyId = outcome.difficultyId,
                    elapsedSeconds = outcome.elapsedSeconds,
                    mistakes = outcome.mistakes,
                    hintsUsed = outcome.hintsUsed,
                    completedAtEpochMillis = outcome.completedAtEpochMillis,
                ),
            )
        }

        val allStats = mapStatsEntities(statsDao.getAll(gameId))
        val completedDays = db.dailyRecordDao().getCompletedEpochDays(gameId).toSet()
        val refEpochDay = dailyEpochDay ?: LocalDate.now().toEpochDay()
        val (dailyStreak, _) = DailyStreakCalculator.calculate(completedDays, refEpochDay)

        val alreadyUnlocked = db.achievementDao().getUnlockedIds(gameId).toSet()
        val newlyUnlocked = AchievementEvaluator.evaluateNewUnlocks(
            alreadyUnlockedIds = alreadyUnlocked,
            outcome = outcome,
            statsAfterWin = allStats,
            dailyCurrentStreak = dailyStreak,
        )

        if (newlyUnlocked.isNotEmpty()) {
            val now = clockMillis()
            db.achievementDao().insertAll(
                newlyUnlocked.map { ach ->
                    AchievementEntity(
                        gameId = gameId,
                        achievementId = ach.id,
                        unlockedAtEpochMillis = now,
                    )
                },
            )
        }

        VictoryExtras(
            isNewBestTime = isNewBest,
            unlockedAchievements = newlyUnlocked,
        )
    }

    override suspend fun isDailyCompleted(date: LocalDate): Boolean =
        db.dailyRecordDao().isCompleted(gameId, date.toEpochDay())

    override fun observeDifficultyStats(): Flow<List<DifficultyStats>> =
        db.difficultyStatsDao().observeAll(gameId).map(::mapStatsEntities)

    override fun observeDailySummary(todayProvider: () -> LocalDate): Flow<DailyStatsSummary> =
        db.dailyRecordDao().observeAll(gameId).map { records ->
            val days = records.mapTo(HashSet(records.size)) { it.epochDay }
            val (current, best) = DailyStreakCalculator.calculate(days, todayProvider().toEpochDay())
            DailyStatsSummary(
                totalCompleted = days.size,
                currentStreak = current,
                bestStreak = best,
                completedEpochDays = days,
            )
        }

    override fun observeAchievements(): Flow<List<AchievementStatus>> =
        db.achievementDao().observeAll(gameId).map { entities ->
            val unlockedMap = entities.associate { it.achievementId to it.unlockedAtEpochMillis }
            SudokuAchievement.entries.map { ach ->
                AchievementStatus(
                    achievement = ach,
                    unlockedAtEpochMillis = unlockedMap[ach.id],
                )
            }
        }

    override suspend fun resetStatsAndAchievements() {
        db.withTransaction {
            db.difficultyStatsDao().deleteAllForGame(gameId)
            db.dailyRecordDao().deleteAllForGame(gameId)
            db.achievementDao().deleteAllForGame(gameId)
        }
    }

    override suspend fun resetAllData() {
        db.withTransaction {
            db.activeGameDao().delete(gameId)
            db.preloadedPuzzleDao().deleteAllForGame(gameId)
            db.difficultyStatsDao().deleteAllForGame(gameId)
            db.dailyRecordDao().deleteAllForGame(gameId)
            db.achievementDao().deleteAllForGame(gameId)
        }
        preferencesPort.resetToDefaults()
    }

    private fun mapStatsEntities(entities: List<DifficultyStatsEntity>): List<DifficultyStats> {
        val byId = entities.associateBy { it.difficultyId }
        return SudokuDifficulty.entries.map { tier ->
            val e = byId[tier.id]
            if (e == null) {
                DifficultyStats(difficulty = tier)
            } else {
                DifficultyStats(
                    difficulty = tier,
                    gamesPlayed = e.gamesPlayed,
                    gamesWon = e.gamesWon,
                    bestTimeSeconds = e.bestTimeSeconds,
                    totalWinTimeSeconds = e.totalWinTimeSeconds,
                    currentWinStreak = e.currentWinStreak,
                    bestWinStreak = e.bestWinStreak,
                    flawlessWins = e.flawlessWins,
                    noHintWins = e.noHintWins,
                )
            }
        }
    }

    private fun emptyStatsEntity(difficultyId: String) = DifficultyStatsEntity(
        gameId = gameId,
        difficultyId = difficultyId,
        gamesPlayed = 0,
        gamesWon = 0,
        bestTimeSeconds = null,
        totalWinTimeSeconds = 0L,
        currentWinStreak = 0,
        bestWinStreak = 0,
        flawlessWins = 0,
        noHintWins = 0,
    )

    private fun ActiveGameEntity.toSummary(): ActiveGameSummary {
        val diff = SudokuDifficulty.fromId(difficultyId) ?: SudokuDifficulty.EASY
        val filled = currentGrid.count { it in '1'..'9' }
        return ActiveGameSummary(
            difficulty = diff,
            elapsedSeconds = elapsedSeconds,
            filledCells = filled,
            isDaily = dailyEpochDay != null,
        )
    }

    private fun ActiveGameEntity.toDomainState(): SudokuGameState {
        val diff = SudokuDifficulty.fromId(difficultyId) ?: SudokuDifficulty.EASY
        val puzzle = SudokuPuzzle(
            id = puzzleId,
            initialGrid = SudokuGrid.fromString(initialGrid),
            solution = SudokuGrid.fromString(solutionGrid),
            difficulty = diff,
            maxTechnique = runCatching { SolvingTechnique.valueOf(maxTechnique) }
                .getOrDefault(SolvingTechnique.NAKED_SINGLE),
            difficultyScore = difficultyScore,
            seed = seed,
            dailyEpochDay = dailyEpochDay,
        )
        return SudokuGameState(
            puzzle = puzzle,
            currentGrid = SudokuGrid.fromString(currentGrid),
            notes = NotesGrid.fromSerializedString(notesSerialized),
            undoStack = BoardStateSerializer.decodeSnapshots(undoStackJson),
            redoStack = BoardStateSerializer.decodeSnapshots(redoStackJson),
            mistakes = mistakes,
            hintsUsed = hintsUsed,
            elapsedSeconds = elapsedSeconds,
            isCompleted = false,
        )
    }
}
