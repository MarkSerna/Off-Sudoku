package com.logicminigames.core.data

import com.logicminigames.core.common.GameOutcome
import com.logicminigames.core.common.KnownGames
import com.logicminigames.core.data.logic.AchievementEvaluator
import com.logicminigames.core.data.model.DifficultyStats
import com.logicminigames.core.data.model.SudokuAchievement
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEvaluatorTest {

    private fun baseStats(winsPerTier: Map<SudokuDifficulty, Int> = emptyMap()): List<DifficultyStats> =
        SudokuDifficulty.entries.map { tier ->
            val wins = winsPerTier[tier] ?: 0
            DifficultyStats(difficulty = tier, gamesPlayed = wins, gamesWon = wins)
        }

    @Test
    fun `unlocks expected achievements on first fast flawless no-hint win without duplicating existing unlocks`() {
        val outcome = GameOutcome(
            gameId = KnownGames.SUDOKU,
            difficultyId = SudokuDifficulty.EASY.id,
            elapsedSeconds = 180L,
            mistakes = 0,
            hintsUsed = 0,
            isDaily = false,
            won = true,
            completedAtEpochMillis = 1_000L,
        )

        val unlocked = AchievementEvaluator.evaluateNewUnlocks(
            alreadyUnlockedIds = emptySet(),
            outcome = outcome,
            statsAfterWin = baseStats(mapOf(SudokuDifficulty.EASY to 1)),
            dailyCurrentStreak = 1,
        )

        assertEquals(
            setOf(
                SudokuAchievement.FIRST_WIN,
                SudokuAchievement.NO_HINTS_WIN,
                SudokuAchievement.FLAWLESS_WIN,
                SudokuAchievement.SPEED_SOLVER,
            ),
            unlocked.toSet(),
        )

        // Idempotency: second evaluation with those already unlocked yields empty
        val reEvaluated = AchievementEvaluator.evaluateNewUnlocks(
            alreadyUnlockedIds = unlocked.mapTo(HashSet()) { it.id },
            outcome = outcome,
            statsAfterWin = baseStats(mapOf(SudokuDifficulty.EASY to 2)),
            dailyCurrentStreak = 1,
        )
        assertTrue(reEvaluated.isEmpty())
    }

    @Test
    fun `unlocks streak, all-difficulties, expert flawless, and veteran achievements`() {
        val outcome = GameOutcome(
            gameId = KnownGames.SUDOKU,
            difficultyId = SudokuDifficulty.EXPERT.id,
            elapsedSeconds = 600L,
            mistakes = 0,
            hintsUsed = 0,
            isDaily = true,
            won = true,
            completedAtEpochMillis = 2_000L,
        )
        val allStats = baseStats(
            mapOf(
                SudokuDifficulty.VERY_EASY to 5,
                SudokuDifficulty.EASY to 5,
                SudokuDifficulty.MEDIUM to 5,
                SudokuDifficulty.HARD to 5,
                SudokuDifficulty.EXPERT to 5,
            ),
        )

        val unlocked = AchievementEvaluator.evaluateNewUnlocks(
            alreadyUnlockedIds = setOf(
                SudokuAchievement.FIRST_WIN.id,
                SudokuAchievement.NO_HINTS_WIN.id,
                SudokuAchievement.FLAWLESS_WIN.id,
            ),
            outcome = outcome,
            statsAfterWin = allStats,
            dailyCurrentStreak = 7,
        )

        assertEquals(
            setOf(
                SudokuAchievement.DAILY_STREAK_7,
                SudokuAchievement.ALL_DIFFICULTIES,
                SudokuAchievement.EXPERT_FLAWLESS,
                SudokuAchievement.VETERAN_25,
            ),
            unlocked.toSet(),
        )
    }
}
