package com.logicminigames.core.common

/**
 * Common contract implemented by each minigame's difficulty tiers so shared
 * persistence, stats, and hub UI can handle difficulties uniformly.
 */
interface DifficultyTier {
    val id: String
    val order: Int
}
