package com.logicminigames.core.sudoku.engine

import com.logicminigames.core.common.DailySeed
import com.logicminigames.core.common.KnownGames
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.sudoku.model.SudokuPuzzle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Offline deterministic daily Sudoku generator seeded by calendar date.
 */
class DailyPuzzleGenerator(
    private val generator: SudokuGenerator = SudokuGenerator(),
) {

    fun difficultyForDate(date: LocalDate): SudokuDifficulty = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> SudokuDifficulty.EASY
        DayOfWeek.TUESDAY -> SudokuDifficulty.MEDIUM
        DayOfWeek.WEDNESDAY -> SudokuDifficulty.MEDIUM
        DayOfWeek.THURSDAY -> SudokuDifficulty.HARD
        DayOfWeek.FRIDAY -> SudokuDifficulty.HARD
        DayOfWeek.SATURDAY -> SudokuDifficulty.EXPERT
        DayOfWeek.SUNDAY -> SudokuDifficulty.MEDIUM
    }

    fun generateForDate(date: LocalDate): SudokuPuzzle {
        val epochDay = date.toEpochDay()
        val seed = DailySeed.dailySeed(KnownGames.SUDOKU, epochDay)
        val difficulty = difficultyForDate(date)
        return generator.generate(difficulty = difficulty, seed = seed).copy(
            id = "daily_$date",
            dailyEpochDay = epochDay,
        )
    }

    suspend fun generateForDateAsync(
        date: LocalDate,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): SudokuPuzzle {
        val epochDay = date.toEpochDay()
        val seed = DailySeed.dailySeed(KnownGames.SUDOKU, epochDay)
        val difficulty = difficultyForDate(date)
        return generator.generateAsync(
            difficulty = difficulty,
            seed = seed,
            dispatcher = dispatcher,
        ).copy(
            id = "daily_$date",
            dailyEpochDay = epochDay,
        )
    }
}
