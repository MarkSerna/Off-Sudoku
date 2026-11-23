package com.logicminigames.core.data.repository

import com.logicminigames.core.common.GameOutcome
import com.logicminigames.core.data.logic.AchievementEvaluator
import com.logicminigames.core.data.logic.DailyStreakCalculator
import com.logicminigames.core.data.model.AchievementStatus
import com.logicminigames.core.data.model.ActiveGameSummary
import com.logicminigames.core.data.model.AppLanguage
import com.logicminigames.core.data.model.DailyStatsSummary
import com.logicminigames.core.data.model.DifficultyStats
import com.logicminigames.core.data.model.GamePreferences
import com.logicminigames.core.data.model.HintMode
import com.logicminigames.core.data.model.SudokuAchievement
import com.logicminigames.core.data.model.VictoryExtras
import com.logicminigames.core.sudoku.engine.ErrorCheckMode
import com.logicminigames.core.sudoku.engine.SudokuGameState
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.ui.theme.AppThemePalette
import com.logicminigames.core.ui.theme.BoardFontScale
import com.logicminigames.core.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

class InMemoryPreferencesPort(
    initial: GamePreferences = GamePreferences(),
) : PreferencesPort {
    private val mutable = MutableStateFlow(initial)
    override val preferences: StateFlow<GamePreferences> = mutable.asStateFlow()

    override suspend fun setThemePalette(palette: AppThemePalette) = mutable.update { it.copy(themePalette = palette) }
    override suspend fun setThemeMode(mode: ThemeMode) = mutable.update { it.copy(themeMode = mode) }
    override suspend fun setColorblindMode(enabled: Boolean) = mutable.update { it.copy(colorblindMode = enabled) }
    override suspend fun setBoardFontScale(scale: BoardFontScale) = mutable.update { it.copy(boardFontScale = scale) }
    override suspend fun setHighlightPeers(enabled: Boolean) = mutable.update { it.copy(highlightPeers = enabled) }
    override suspend fun setHighlightSameNumbers(enabled: Boolean) = mutable.update { it.copy(highlightSameNumbers = enabled) }
    override suspend fun setAutoClearNotes(enabled: Boolean) = mutable.update { it.copy(autoClearNotes = enabled) }
    override suspend fun setShowRemainingCount(enabled: Boolean) = mutable.update { it.copy(showRemainingCount = enabled) }
    override suspend fun setErrorCheckMode(mode: ErrorCheckMode) = mutable.update { it.copy(errorCheckMode = mode) }
    override suspend fun setHintLimit(limit: Int) = mutable.update { it.copy(hintLimit = limit) }
    override suspend fun setHintMode(mode: HintMode) = mutable.update { it.copy(hintMode = mode) }
    override suspend fun setSoundEnabled(enabled: Boolean) = mutable.update { it.copy(soundEnabled = enabled) }
    override suspend fun setHapticsEnabled(enabled: Boolean) = mutable.update { it.copy(hapticsEnabled = enabled) }
    override suspend fun setLanguage(language: AppLanguage) = mutable.update { it.copy(language = language) }
    override suspend fun resetToDefaults() {
        mutable.value = GamePreferences()
    }
}

class InMemoryActiveAndStatsPort(
    private val preferencesPort: InMemoryPreferencesPort = InMemoryPreferencesPort(),
) : ActiveGamePort, StatsPort {

    private val mutex = Mutex()
    private var storedState: SudokuGameState? = null
    private val _summary = MutableStateFlow<ActiveGameSummary?>(null)
    private val _stats = MutableStateFlow(SudokuDifficulty.entries.map { DifficultyStats(difficulty = it) })
    private val _dailyDays = MutableStateFlow<Set<Long>>(emptySet())
    private val _achievements = MutableStateFlow(
        SudokuAchievement.entries.map { AchievementStatus(it, null) },
    )

    override val activeGameSummary: StateFlow<ActiveGameSummary?> = _summary.asStateFlow()

    override suspend fun loadActiveGame(): SudokuGameState? = mutex.withLock { storedState }

    override suspend fun saveActiveGame(state: SudokuGameState) {
        mutex.withLock {
            if (state.isCompleted) {
                storedState = null
                _summary.value = null
            } else {
                storedState = state
                _summary.value = ActiveGameSummary(
                    difficulty = state.puzzle.difficulty,
                    elapsedSeconds = state.elapsedSeconds,
                    filledCells = state.currentGrid.filledCount(),
                    isDaily = state.puzzle.dailyEpochDay != null,
                )
            }
        }
    }

    override suspend fun clearActiveGame() {
        mutex.withLock {
            storedState = null
            _summary.value = null
        }
    }

    override suspend fun recordGameStarted(difficulty: SudokuDifficulty, abandonedPrevious: Boolean) {
        mutex.withLock {
            val prev = storedState
            _stats.update { list ->
                list.map { item ->
                    var updated = item
                    if (abandonedPrevious && prev != null && item.difficulty == prev.puzzle.difficulty) {
                        updated = updated.copy(currentWinStreak = 0)
                    }
                    if (item.difficulty == difficulty) {
                        updated = updated.copy(gamesPlayed = updated.gamesPlayed + 1)
                    }
                    updated
                }
            }
        }
    }

    override suspend fun recordCompletedGame(
        outcome: GameOutcome,
        dailyEpochDay: Long?,
    ): VictoryExtras = mutex.withLock {
        var isNewBest = false
        _stats.update { list ->
            list.map { item ->
                if (item.difficulty.id != outcome.difficultyId) return@map item
                val newWon = item.gamesWon + if (outcome.won) 1 else 0
                val newPlayed = maxOf(item.gamesPlayed, newWon)
                isNewBest = outcome.won &&
                    (item.bestTimeSeconds == null || outcome.elapsedSeconds < item.bestTimeSeconds)
                val newBest = when {
                    !outcome.won -> item.bestTimeSeconds
                    item.bestTimeSeconds == null -> outcome.elapsedSeconds
                    else -> minOf(item.bestTimeSeconds, outcome.elapsedSeconds)
                }
                val newStreak = if (outcome.won) item.currentWinStreak + 1 else 0
                item.copy(
                    gamesPlayed = newPlayed,
                    gamesWon = newWon,
                    bestTimeSeconds = newBest,
                    totalWinTimeSeconds = item.totalWinTimeSeconds + if (outcome.won) outcome.elapsedSeconds else 0L,
                    currentWinStreak = newStreak,
                    bestWinStreak = maxOf(item.bestWinStreak, newStreak),
                    flawlessWins = item.flawlessWins + if (outcome.won && outcome.mistakes == 0) 1 else 0,
                    noHintWins = item.noHintWins + if (outcome.won && outcome.hintsUsed == 0) 1 else 0,
                )
            }
        }

        if (outcome.won && dailyEpochDay != null) {
            _dailyDays.update { it + dailyEpochDay }
        }

        val refDay = dailyEpochDay ?: LocalDate.now().toEpochDay()
        val (dailyStreak, _) = DailyStreakCalculator.calculate(_dailyDays.value, refDay)
        val alreadyIds = _achievements.value.filter { it.isUnlocked }.mapTo(HashSet()) { it.achievement.id }
        val newlyUnlocked = AchievementEvaluator.evaluateNewUnlocks(
            alreadyUnlockedIds = alreadyIds,
            outcome = outcome,
            statsAfterWin = _stats.value,
            dailyCurrentStreak = dailyStreak,
        )
        if (newlyUnlocked.isNotEmpty()) {
            val now = outcome.completedAtEpochMillis
            _achievements.update { current ->
                current.map { status ->
                    if (status.achievement in newlyUnlocked) status.copy(unlockedAtEpochMillis = now) else status
                }
            }
        }
        VictoryExtras(isNewBestTime = isNewBest, unlockedAchievements = newlyUnlocked)
    }

    override suspend fun isDailyCompleted(date: LocalDate): Boolean = mutex.withLock {
        date.toEpochDay() in _dailyDays.value
    }

    override fun observeDifficultyStats(): Flow<List<DifficultyStats>> = _stats.asStateFlow()

    override fun observeDailySummary(todayProvider: () -> LocalDate): Flow<DailyStatsSummary> =
        _dailyDays.map { days ->
            val (current, best) = DailyStreakCalculator.calculate(days, todayProvider().toEpochDay())
            DailyStatsSummary(
                totalCompleted = days.size,
                currentStreak = current,
                bestStreak = best,
                completedEpochDays = days,
            )
        }

    override fun observeAchievements(): Flow<List<AchievementStatus>> = _achievements.asStateFlow()

    override suspend fun resetStatsAndAchievements() {
        mutex.withLock {
            _stats.value = SudokuDifficulty.entries.map { DifficultyStats(difficulty = it) }
            _dailyDays.value = emptySet()
            _achievements.value = SudokuAchievement.entries.map { AchievementStatus(it, null) }
        }
    }

    override suspend fun resetAllData() {
        resetStatsAndAchievements()
        clearActiveGame()
        preferencesPort.resetToDefaults()
    }
}
