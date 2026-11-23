package com.logicminigames.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "active_games")
data class ActiveGameEntity(
    @PrimaryKey val gameId: String,
    val puzzleId: String,
    val difficultyId: String,
    val initialGrid: String,
    val solutionGrid: String,
    val currentGrid: String,
    val notesSerialized: String,
    val undoStackJson: String,
    val redoStackJson: String,
    val maxTechnique: String,
    val difficultyScore: Int,
    val seed: Long,
    val dailyEpochDay: Long?,
    val mistakes: Int,
    val hintsUsed: Int,
    val elapsedSeconds: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "preloaded_puzzles",
    indices = [Index(value = ["gameId", "difficultyId"])],
)
data class PreloadedPuzzleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val gameId: String,
    val difficultyId: String,
    val puzzleId: String,
    val initialGrid: String,
    val solutionGrid: String,
    val maxTechnique: String,
    val difficultyScore: Int,
    val seed: Long,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "difficulty_stats",
    primaryKeys = ["gameId", "difficultyId"],
)
data class DifficultyStatsEntity(
    val gameId: String,
    val difficultyId: String,
    val gamesPlayed: Int,
    val gamesWon: Int,
    val bestTimeSeconds: Long?,
    val totalWinTimeSeconds: Long,
    val currentWinStreak: Int,
    val bestWinStreak: Int,
    val flawlessWins: Int,
    val noHintWins: Int,
)

@Entity(
    tableName = "daily_records",
    primaryKeys = ["gameId", "epochDay"],
)
data class DailyRecordEntity(
    val gameId: String,
    val epochDay: Long,
    val difficultyId: String,
    val elapsedSeconds: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val completedAtEpochMillis: Long,
)

@Entity(
    tableName = "achievements",
    primaryKeys = ["gameId", "achievementId"],
)
data class AchievementEntity(
    val gameId: String,
    val achievementId: String,
    val unlockedAtEpochMillis: Long,
)
