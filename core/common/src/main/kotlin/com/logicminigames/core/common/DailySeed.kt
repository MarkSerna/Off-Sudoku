package com.logicminigames.core.common

/**
 * Deterministic 64-bit offline seed generator for daily puzzles using FNV-1a and SplitMix64.
 */
object DailySeed {
    private const val FNV_OFFSET_BASIS = -3750763034362895579L // 0xcbf29ce484222325UL
    private const val FNV_PRIME = 1099511628211L
    private const val GOLDEN_GAMMA = -7046029254386353131L // 0x9e3779b97f4a7c15UL

    /**
     * Derives a stable, deterministic seed for [gameId] on calendar day [epochDay].
     */
    fun dailySeed(gameId: GameId, epochDay: Long): Long {
        val gameHash = fnv1a64(gameId.value)
        val combined = gameHash xor (epochDay * GOLDEN_GAMMA)
        return splitMix64(combined)
    }

    private fun fnv1a64(text: String): Long {
        var hash = FNV_OFFSET_BASIS
        for (i in text.indices) {
            val ch = text[i].code.toLong()
            hash = hash xor (ch and 0xFFL)
            hash *= FNV_PRIME
            hash = hash xor ((ch ushr 8) and 0xFFL)
            hash *= FNV_PRIME
        }
        return hash
    }

    private fun splitMix64(input: Long): Long {
        var z = input + GOLDEN_GAMMA
        z = (z xor (z ushr 30)) * -4658895280553007687L // 0xbf58476d1ce4e5b9UL
        z = (z xor (z ushr 27)) * -7723592293110705685L // 0x94d049bb133111ebUL
        return z xor (z ushr 31)
    }
}
