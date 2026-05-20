package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MindPlayDao {
    @Query("SELECT * FROM streak_state WHERE id = 1")
    fun getStreakState(): Flow<StreakState?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreakState(state: StreakState)

    // Logs
    @Query("SELECT * FROM mindful_logs ORDER BY timestamp DESC")
    fun getAllMindfulLogs(): Flow<List<MindfulLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMindfulLog(log: MindfulLog)

    // Badges
    @Query("SELECT * FROM badges")
    fun getAllBadges(): Flow<List<Badge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: Badge)

    // Leaderboard
    @Query("SELECT * FROM leaderboard ORDER BY score DESC, timestamp ASC")
    fun getLeaderboard(): Flow<List<LeaderboardEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntry(entry: LeaderboardEntry)
}
