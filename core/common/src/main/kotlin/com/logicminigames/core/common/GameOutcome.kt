package com.logicminigames.core.common

/**
 * Shared outcome record produced when a minigame session finishes, used to
 * update per-game statistics, streaks, and local achievements.
 */
data class GameOutcome(
    val gameId: GameId,
    val difficultyId: String,
    val elapsedSeconds: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val isDaily: Boolean,
    val won: Boolean,
    val completedAtEpochMillis: Long,
)
