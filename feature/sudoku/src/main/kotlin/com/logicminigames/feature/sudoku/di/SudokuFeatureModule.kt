package com.logicminigames.feature.sudoku.di

import com.logicminigames.core.sudoku.engine.DailyPuzzleGenerator
import com.logicminigames.core.sudoku.engine.LogicalSudokuSolver
import com.logicminigames.core.sudoku.engine.PuzzlePreloader
import com.logicminigames.core.sudoku.engine.SudokuGenerator
import com.logicminigames.feature.sudoku.game.GameViewModel
import com.logicminigames.feature.sudoku.home.HomeViewModel
import com.logicminigames.feature.sudoku.settings.SettingsViewModel
import com.logicminigames.feature.sudoku.stats.StatsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sudokuFeatureModule = module {
    single { LogicalSudokuSolver() }
    single { SudokuGenerator(logicalSolver = get()) }
    single { DailyPuzzleGenerator(generator = get()) }
    single { PuzzlePreloader(generator = get(), cache = get()) }

    viewModel {
        HomeViewModel(
            activeGamePort = get(),
            statsPort = get(),
            dailyGenerator = get(),
        )
    }
    viewModel {
        GameViewModel(
            savedStateHandle = get(),
            preloader = get(),
            dailyGenerator = get(),
            logicalSolver = get(),
            activeGamePort = get(),
            preferencesPort = get(),
        )
    }
    viewModel { StatsViewModel(statsPort = get()) }
    viewModel { SettingsViewModel(preferencesPort = get(), statsPort = get()) }
}
