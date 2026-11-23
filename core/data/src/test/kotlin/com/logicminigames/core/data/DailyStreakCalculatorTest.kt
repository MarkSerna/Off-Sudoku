package com.logicminigames.core.data

import com.logicminigames.core.data.logic.DailyStreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyStreakCalculatorTest {

    @Test
    fun `returns zero streaks when no daily puzzles completed`() {
        assertEquals(0 to 0, DailyStreakCalculator.calculate(emptySet(), todayEpochDay = 20_000L))
    }

    @Test
    fun `counts streak ending today or yesterday while preserving historical best`() {
        val today = 20_100L
        // Historical 4-day streak (10..13), gap, then active 3-day streak ending yesterday (97..99)
        val days = setOf(10L, 11L, 12L, 13L, 97L, 98L, 99L)
        assertEquals(3 to 4, DailyStreakCalculator.calculate(days.map { 20_000L + it }.toSet(), today))

        // Completing today bumps current and equals best
        val withToday = days.map { 20_000L + it }.toSet() + today
        assertEquals(4 to 4, DailyStreakCalculator.calculate(withToday, today))
    }

    @Test
    fun `resets current streak to zero when yesterday and today were both missed`() {
        val today = 20_100L
        val days = setOf(today - 4L, today - 3L, today - 2L)
        assertEquals(0 to 3, DailyStreakCalculator.calculate(days, today))
    }
}
