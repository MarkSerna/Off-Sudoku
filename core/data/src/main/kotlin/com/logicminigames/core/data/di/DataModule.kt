package com.logicminigames.core.data.di

import androidx.room.Room
import com.logicminigames.core.data.local.DataStorePreferencesRepository
import com.logicminigames.core.data.local.MinigamesDatabase
import com.logicminigames.core.data.local.RoomPuzzleCacheStore
import com.logicminigames.core.data.local.RoomSudokuDataRepository
import com.logicminigames.core.data.local.userPreferencesDataStore
import com.logicminigames.core.data.repository.ActiveGamePort
import com.logicminigames.core.data.repository.PreferencesPort
import com.logicminigames.core.data.repository.StatsPort
import com.logicminigames.core.sudoku.engine.PuzzleCacheStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            MinigamesDatabase::class.java,
            "minigames.db",
        ).build()
    }

    single { get<MinigamesDatabase>().preloadedPuzzleDao() }

    single<PreferencesPort> {
        DataStorePreferencesRepository(androidContext().userPreferencesDataStore)
    }

    single<PuzzleCacheStore> { RoomPuzzleCacheStore(dao = get()) }

    single {
        RoomSudokuDataRepository(
            db = get(),
            preferencesPort = get(),
        )
    }

    single<ActiveGamePort> { get<RoomSudokuDataRepository>() }
    single<StatsPort> { get<RoomSudokuDataRepository>() }
}
