package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class MindPlayRepository(private val dao: MindPlayDao) {

    val streakState: Flow<StreakState?> = dao.getStreakState()
    val mindfulLogs: Flow<List<MindfulLog>> = dao.getAllMindfulLogs()
    val badges: Flow<List<Badge>> = dao.getAllBadges()
    val leaderboard: Flow<List<LeaderboardEntry>> = dao.getLeaderboard()

    suspend fun updateStreakAndPoints(pointsEarned: Int, isMindful: Boolean) {
        val current = streakState.firstOrNull() ?: StreakState()
        val now = System.currentTimeMillis()
        
        var newStreak = current.currentStreak
        if (current.lastActiveTimestamp == 0L) {
            newStreak = 1
        } else {
            val lastActiveCal = Calendar.getInstance().apply { timeInMillis = current.lastActiveTimestamp }
            val nowCal = Calendar.getInstance().apply { timeInMillis = now }

            val diffDays = (nowCal.get(Calendar.DAY_OF_YEAR) - lastActiveCal.get(Calendar.DAY_OF_YEAR)) + 
                           365 * (nowCal.get(Calendar.YEAR) - lastActiveCal.get(Calendar.YEAR))

            if (diffDays == 1) {
                newStreak += 1
            } else if (diffDays > 1) {
                newStreak = 1
            }
        }

        val updatedState = current.copy(
            currentStreak = newStreak,
            lastActiveTimestamp = now,
            totalPoints = current.totalPoints + pointsEarned,
            entertainmentPoints = current.entertainmentPoints + if (!isMindful) pointsEarned else 0,
            mindfulnessPoints = current.mindfulnessPoints + if (isMindful) pointsEarned else 0,
            totalCompletedCompleted = current.totalCompletedCompleted + 1
        )
        dao.insertStreakState(updatedState)

        // Check for streak milestones or point milestone badges
        checkBadges(updatedState)
    }

    private suspend fun checkBadges(state: StreakState) {
        if (state.currentStreak >= 3) {
            dao.insertBadge(
                Badge(
                    badgeId = "STREAK_3",
                    title = "Streak Starter",
                    description = "Maintain a 3-day consistency streak.",
                    type = "Mindful"
                )
            )
        }
        if (state.totalPoints >= 100) {
            dao.insertBadge(
                Badge(
                    badgeId = "POINTS_100",
                    title = "Point Champion",
                    description = "Accumulate over 100 total MindPlay points.",
                    type = "Entertainment"
                )
            )
        }
        if (state.mindfulnessPoints >= 50) {
            dao.insertBadge(
                Badge(
                    badgeId = "ZEN_MASTER",
                    title = "Zen Master",
                    description = "Earn 50 mindfulness balance points.",
                    type = "Mindful"
                )
            )
        }
        if (state.entertainmentPoints >= 50) {
            dao.insertBadge(
                Badge(
                    badgeId = "CHALLENGE_PRO",
                    title = "Mind Challenger",
                    description = "Earn 50 dynamic entertainment points.",
                    type = "Entertainment"
                )
            )
        }
    }

    suspend fun insertLog(log: MindfulLog) {
        dao.insertMindfulLog(log)
    }

    suspend fun addLeaderboardEntry(name: String, score: Int, isUser: Boolean = false) {
        dao.insertLeaderboardEntry(LeaderboardEntry(name = name, score = score, isUser = isUser))
    }

    suspend fun prepopulateLeaderboardIfEmpty() {
        val currentList = dao.getLeaderboard().firstOrNull() ?: emptyList()
        if (currentList.isEmpty()) {
            dao.insertLeaderboardEntry(LeaderboardEntry(name = "Aria Clear", score = 120))
            dao.insertLeaderboardEntry(LeaderboardEntry(name = "Kaelen Calm", score = 95))
            dao.insertLeaderboardEntry(LeaderboardEntry(name = "Seraphina", score = 80))
            dao.insertLeaderboardEntry(LeaderboardEntry(name = "ZenSpark", score = 65))
            dao.insertLeaderboardEntry(LeaderboardEntry(name = "PixelBreather", score = 40))
        }
    }
}
