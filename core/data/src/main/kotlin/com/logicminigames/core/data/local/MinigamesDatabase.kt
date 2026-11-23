package com.logicminigames.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ActiveGameEntity::class,
        PreloadedPuzzleEntity::class,
        DifficultyStatsEntity::class,
        DailyRecordEntity::class,
        AchievementEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MinigamesDatabase : RoomDatabase() {
    abstract fun activeGameDao(): ActiveGameDao
    abstract fun preloadedPuzzleDao(): PreloadedPuzzleDao
    abstract fun difficultyStatsDao(): DifficultyStatsDao
    abstract fun dailyRecordDao(): DailyRecordDao
    abstract fun achievementDao(): AchievementDao
}
