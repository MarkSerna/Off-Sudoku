package com.logicminigames.core.common

/**
 * Metadata describing a minigame registered in the collection hub.
 */
data class MinigameDescriptor(
    val id: GameId,
    val route: String,
    val supportedDifficulties: List<DifficultyTier>,
)
