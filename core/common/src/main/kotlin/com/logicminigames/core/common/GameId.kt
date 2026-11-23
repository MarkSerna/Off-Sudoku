package com.logicminigames.core.common

/**
 * Strongly-typed identifier for each minigame in the offline logic games collection.
 */
@JvmInline
value class GameId(val value: String) {
    init {
        require(value.isNotBlank()) { "GameId must not be blank" }
    }
}

/**
 * Registry of known minigame identifiers.
 */
object KnownGames {
    val SUDOKU = GameId("sudoku")
}
