package com.logicminigames.core.data.repository

import com.logicminigames.core.common.GameOutcome
import com.logicminigames.core.data.model.AchievementStatus
import com.logicminigames.core.data.model.ActiveGameSummary
import com.logicminigames.core.data.model.AppLanguage
import com.logicminigames.core.data.model.DailyStatsSummary
import com.logicminigames.core.data.model.DifficultyStats
import com.logicminigames.core.data.model.GamePreferences
import com.logicminigames.core.data.model.HintMode
import com.logicminigames.core.data.model.VictoryExtras
import com.logicminigames.core.sudoku.engine.ErrorCheckMode
import com.logicminigames.core.sudoku.engine.SudokuGameState
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.ui.theme.AppThemePalette
import com.logicminigames.core.ui.theme.BoardFontScale
import com.logicminigames.core.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

interface PreferencesPort {
    val preferences: StateFlow<GamePreferences>
    suspend fun setThemePalette(palette: AppThemePalette)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setColorblindMode(enabled: Boolean)
    suspend fun setBoardFontScale(scale: BoardFontScale)
    suspend fun setHighlightPeers(enabled: Boolean)
    suspend fun setHighlightSameNumbers(enabled: Boolean)
    suspend fun setAutoClearNotes(enabled: Boolean)
    suspend fun setShowRemainingCount(enabled: Boolean)
    suspend fun setErrorCheckMode(mode: ErrorCheckMode)
    suspend fun setHintLimit(limit: Int)
    suspend fun setHintMode(mode: HintMode)
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun resetToDefaults()
}

interface ActiveGamePort {
    val activeGameSummary: StateFlow<ActiveGameSummary?>
    suspend fun loadActiveGame(): SudokuGameState?
    suspend fun saveActiveGame(state: SudokuGameState)
    suspend fun clearActiveGame()
    suspend fun recordGameStarted(difficulty: SudokuDifficulty, abandonedPrevious: Boolean)
    suspend fun recordCompletedGame(outcome: GameOutcome, dailyEpochDay: Long?): VictoryExtras
    suspend fun isDailyCompleted(date: LocalDate): Boolean
}

interface StatsPort {
    fun observeDifficultyStats(): Flow<List<DifficultyStats>>
    fun observeDailySummary(todayProvider: () -> LocalDate = { LocalDate.now() }): Flow<DailyStatsSummary>
    fun observeAchievements(): Flow<List<AchievementStatus>>
    suspend fun resetStatsAndAchievements()
    suspend fun resetAllData()
}
