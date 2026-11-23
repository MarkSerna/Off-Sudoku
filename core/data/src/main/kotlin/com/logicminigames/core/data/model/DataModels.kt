package com.logicminigames.core.data.model

import com.logicminigames.core.sudoku.engine.ErrorCheckMode
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.ui.theme.AppThemePalette
import com.logicminigames.core.ui.theme.BoardFontScale
import com.logicminigames.core.ui.theme.ThemeMode

enum class HintMode {
    EXPLAIN_FIRST,
    REVEAL_DIRECTLY,
}

enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    SPANISH("es"),
    ENGLISH("en"),
    PORTUGUESE("pt"),
    ;

    companion object {
        fun fromTag(tag: String): AppLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: SYSTEM
    }
}

data class GamePreferences(
    val themePalette: AppThemePalette = AppThemePalette.CLASSIC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorblindMode: Boolean = false,
    val boardFontScale: BoardFontScale = BoardFontScale.NORMAL,
    val highlightPeers: Boolean = true,
    val highlightSameNumbers: Boolean = true,
    val autoClearNotes: Boolean = true,
    val showRemainingCount: Boolean = true,
    val errorCheckMode: ErrorCheckMode = ErrorCheckMode.IMMEDIATE,
    val hintLimit: Int = 3,
    val hintMode: HintMode = HintMode.EXPLAIN_FIRST,
    val soundEnabled: Boolean = false,
    val hapticsEnabled: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
)

data class ActiveGameSummary(
    val difficulty: SudokuDifficulty,
    val elapsedSeconds: Long,
    val filledCells: Int,
    val isDaily: Boolean,
)

enum class SudokuAchievement(val id: String) {
    FIRST_WIN("first_win"),
    NO_HINTS_WIN("no_hints_win"),
    FLAWLESS_WIN("flawless_win"),
    SPEED_SOLVER("speed_solver"),
    DAILY_STREAK_7("daily_streak_7"),
    ALL_DIFFICULTIES("all_difficulties"),
    EXPERT_FLAWLESS("expert_flawless"),
    VETERAN_25("veteran_25"),
    ;

    companion object {
        fun fromId(id: String): SudokuAchievement? = entries.firstOrNull { it.id == id }
    }
}

data class VictoryExtras(
    val isNewBestTime: Boolean = false,
    val unlockedAchievements: List<SudokuAchievement> = emptyList(),
)

data class DifficultyStats(
    val difficulty: SudokuDifficulty,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val bestTimeSeconds: Long? = null,
    val totalWinTimeSeconds: Long = 0L,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val flawlessWins: Int = 0,
    val noHintWins: Int = 0,
) {
    val winRatePercent: Int
        get() = if (gamesPlayed == 0) 0 else ((gamesWon * 100f) / gamesPlayed).toInt().coerceIn(0, 100)

    val averageTimeSeconds: Long?
        get() = if (gamesWon == 0) null else totalWinTimeSeconds / gamesWon
}

data class DailyChallengeRecord(
    val epochDay: Long,
    val difficulty: SudokuDifficulty,
    val elapsedSeconds: Long,
    val mistakes: Int,
    val hintsUsed: Int,
)

data class DailyStatsSummary(
    val totalCompleted: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val completedEpochDays: Set<Long> = emptySet(),
)

data class AchievementStatus(
    val achievement: SudokuAchievement,
    val unlockedAtEpochMillis: Long?,
) {
    val isUnlocked: Boolean get() = unlockedAtEpochMillis != null
}
