package com.logicminigames.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveGameDao {
    @Query("SELECT * FROM active_games WHERE gameId = :gameId LIMIT 1")
    fun observe(gameId: String): Flow<ActiveGameEntity?>

    @Query("SELECT * FROM active_games WHERE gameId = :gameId LIMIT 1")
    suspend fun get(gameId: String): ActiveGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ActiveGameEntity)

    @Query("DELETE FROM active_games WHERE gameId = :gameId")
    suspend fun delete(gameId: String)
}

@Dao
interface PreloadedPuzzleDao {
    @Query(
        "SELECT * FROM preloaded_puzzles WHERE gameId = :gameId AND difficultyId = :difficultyId ORDER BY id ASC LIMIT 1",
    )
    suspend fun peekOldest(gameId: String, difficultyId: String): PreloadedPuzzleEntity?

    @Query("DELETE FROM preloaded_puzzles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    suspend fun popNext(gameId: String, difficultyId: String): PreloadedPuzzleEntity? {
        val next = peekOldest(gameId, difficultyId) ?: return null
        deleteById(next.id)
        return next
    }

    @Insert
    suspend fun insert(entity: PreloadedPuzzleEntity)

    @Query("SELECT COUNT(*) FROM preloaded_puzzles WHERE gameId = :gameId AND difficultyId = :difficultyId")
    suspend fun count(gameId: String, difficultyId: String): Int

    @Query("DELETE FROM preloaded_puzzles WHERE gameId = :gameId")
    suspend fun deleteAllForGame(gameId: String)
}

@Dao
interface DifficultyStatsDao {
    @Query("SELECT * FROM difficulty_stats WHERE gameId = :gameId")
    fun observeAll(gameId: String): Flow<List<DifficultyStatsEntity>>

    @Query("SELECT * FROM difficulty_stats WHERE gameId = :gameId")
    suspend fun getAll(gameId: String): List<DifficultyStatsEntity>

    @Query("SELECT * FROM difficulty_stats WHERE gameId = :gameId AND difficultyId = :difficultyId LIMIT 1")
    suspend fun get(gameId: String, difficultyId: String): DifficultyStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DifficultyStatsEntity)

    @Query("DELETE FROM difficulty_stats WHERE gameId = :gameId")
    suspend fun deleteAllForGame(gameId: String)
}

@Dao
interface DailyRecordDao {
    @Query("SELECT * FROM daily_records WHERE gameId = :gameId ORDER BY epochDay DESC")
    fun observeAll(gameId: String): Flow<List<DailyRecordEntity>>

    @Query("SELECT epochDay FROM daily_records WHERE gameId = :gameId")
    suspend fun getCompletedEpochDays(gameId: String): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM daily_records WHERE gameId = :gameId AND epochDay = :epochDay)")
    suspend fun isCompleted(gameId: String, epochDay: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: DailyRecordEntity)

    @Query("DELETE FROM daily_records WHERE gameId = :gameId")
    suspend fun deleteAllForGame(gameId: String)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE gameId = :gameId")
    fun observeAll(gameId: String): Flow<List<AchievementEntity>>

    @Query("SELECT achievementId FROM achievements WHERE gameId = :gameId")
    suspend fun getUnlockedIds(gameId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<AchievementEntity>)

    @Query("DELETE FROM achievements WHERE gameId = :gameId")
    suspend fun deleteAllForGame(gameId: String)
}
