package com.logicminigames.core.data.logic

import com.logicminigames.core.common.GameOutcome
import com.logicminigames.core.data.model.DifficultyStats
import com.logicminigames.core.data.model.SudokuAchievement
import com.logicminigames.core.sudoku.engine.BoardSnapshot
import com.logicminigames.core.sudoku.model.NotesGrid
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.sudoku.model.SudokuGrid
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object BoardStateSerializer {

    @Serializable
    data class SerializedSnapshot(
        val values: String,
        val notes: String,
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun encodeSnapshots(snapshots: List<BoardSnapshot>): String {
        if (snapshots.isEmpty()) return "[]"
        val payload = snapshots.map {
            SerializedSnapshot(
                values = it.values.toFlatString(),
                notes = it.notes.toSerializedString(),
            )
        }
        return json.encodeToString(payload)
    }

    fun decodeSnapshots(raw: String): List<BoardSnapshot> {
        if (raw.isBlank() || raw == "[]") return emptyList()
        return runCatching {
            json.decodeFromString<List<SerializedSnapshot>>(raw).map {
                BoardSnapshot(
                    values = SudokuGrid.fromString(it.values),
                    notes = NotesGrid.fromSerializedString(it.notes),
                )
            }
        }.getOrDefault(emptyList())
    }
}

object DailyStreakCalculator {

    /**
     * Returns (currentStreak, bestStreak) for [completedEpochDays] evaluated on [todayEpochDay].
     * A streak remains active if today is completed or if yesterday was completed (today pending).
     */
    fun calculate(completedEpochDays: Set<Long>, todayEpochDay: Long): Pair<Int, Int> {
        if (completedEpochDays.isEmpty()) return 0 to 0

        val sorted = completedEpochDays.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1] + 1L) {
                run++
                if (run > best) best = run
            } else {
                run = 1
            }
        }

        val anchor = when {
            todayEpochDay in completedEpochDays -> todayEpochDay
            (todayEpochDay - 1L) in completedEpochDays -> todayEpochDay - 1L
            else -> return 0 to best
        }

        var current = 0
        var cursor = anchor
        while (cursor in completedEpochDays) {
            current++
            cursor--
        }
        return current to best
    }
}

object AchievementEvaluator {

    private const val SPEED_LIMIT_SECONDS = 300L
    private const val DAILY_STREAK_TARGET = 7
    private const val VETERAN_WIN_TARGET = 25

    fun evaluateNewUnlocks(
        alreadyUnlockedIds: Set<String>,
        outcome: GameOutcome,
        statsAfterWin: List<DifficultyStats>,
        dailyCurrentStreak: Int,
    ): List<SudokuAchievement> {
        if (!outcome.won) return emptyList()

        val unlocked = ArrayList<SudokuAchievement>()
        val totalWins = statsAfterWin.sumOf { it.gamesWon }

        fun check(achievement: SudokuAchievement, condition: Boolean) {
            if (condition && achievement.id !in alreadyUnlockedIds) {
                unlocked.add(achievement)
            }
        }

        check(SudokuAchievement.FIRST_WIN, totalWins >= 1)
        check(SudokuAchievement.NO_HINTS_WIN, outcome.hintsUsed == 0)
        check(SudokuAchievement.FLAWLESS_WIN, outcome.mistakes == 0)
        check(SudokuAchievement.SPEED_SOLVER, outcome.elapsedSeconds < SPEED_LIMIT_SECONDS)
        check(SudokuAchievement.DAILY_STREAK_7, dailyCurrentStreak >= DAILY_STREAK_TARGET)

        val wonAllDifficulties = SudokuDifficulty.entries.all { tier ->
            statsAfterWin.any { it.difficulty == tier && it.gamesWon > 0 }
        }
        check(SudokuAchievement.ALL_DIFFICULTIES, wonAllDifficulties)

        check(
            SudokuAchievement.EXPERT_FLAWLESS,
            outcome.difficultyId == SudokuDifficulty.EXPERT.id &&
                outcome.mistakes == 0 &&
                outcome.hintsUsed == 0,
        )
        check(SudokuAchievement.VETERAN_25, totalWins >= VETERAN_WIN_TARGET)

        return unlocked
    }
}
