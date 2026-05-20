package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak_state")
data class StreakState(
    @PrimaryKey val id: Int = 1, // Only one row for user profile
    val currentStreak: Int = 0,
    val lastActiveTimestamp: Long = 0L,
    val totalPoints: Int = 0,
    val entertainmentPoints: Int = 0,
    val mindfulnessPoints: Int = 0,
    val totalCompletedCompleted: Int = 0
)

@Entity(tableName = "mindful_logs")
data class MindfulLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val userResponse: String,
    val mood: String, // e.g. "Calm", "Anxious", "Excited", "Tired", "Happy"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey val badgeId: String, // e.g. "STREAK_3", "ZEN_MASTER", "TRIVIA_CHAMP"
    val title: String,
    val description: String,
    val type: String, // "Mindful" or "Entertainment"
    val timestampEarned: Long = System.currentTimeMillis()
)

@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val score: Int,
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
