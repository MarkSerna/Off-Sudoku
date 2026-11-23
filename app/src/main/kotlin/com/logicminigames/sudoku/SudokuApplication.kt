package com.logicminigames.sudoku

import android.app.Application
import com.logicminigames.core.data.di.dataModule
import com.logicminigames.core.sudoku.engine.PuzzlePreloader
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.feature.sudoku.di.sudokuFeatureModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SudokuApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SudokuApplication)
            modules(dataModule, sudokuFeatureModule)
        }
        val preloader: PuzzlePreloader = get()
        preloader.warmUp(
            scope = appScope,
            difficulties = listOf(
                SudokuDifficulty.VERY_EASY,
                SudokuDifficulty.EASY,
                SudokuDifficulty.MEDIUM,
            ),
            targetPerDifficulty = 1,
        )
    }
}
