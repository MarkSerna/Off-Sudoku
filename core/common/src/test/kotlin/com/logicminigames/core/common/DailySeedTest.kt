package com.logicminigames.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DailySeedTest {

    @Test
    fun `dailySeed is deterministic for identical gameId and epochDay`() {
        val first = DailySeed.dailySeed(KnownGames.SUDOKU, 20_500L)
        val second = DailySeed.dailySeed(GameId("sudoku"), 20_500L)
        assertEquals(first, second)
    }

    @Test
    fun `dailySeed differs across consecutive days`() {
        val seeds = (20_500L..20_530L).map { DailySeed.dailySeed(KnownGames.SUDOKU, it) }.toSet()
        assertEquals(31, seeds.size)
    }

    @Test
    fun `dailySeed differs across distinct games on the same day`() {
        val sudokuSeed = DailySeed.dailySeed(KnownGames.SUDOKU, 20_500L)
        val nonogramSeed = DailySeed.dailySeed(GameId("nonogram"), 20_500L)
        val minesSeed = DailySeed.dailySeed(GameId("minesweeper"), 20_500L)
        assertNotEquals(sudokuSeed, nonogramSeed)
        assertNotEquals(sudokuSeed, minesSeed)
        assertNotEquals(nonogramSeed, minesSeed)
    }
}
