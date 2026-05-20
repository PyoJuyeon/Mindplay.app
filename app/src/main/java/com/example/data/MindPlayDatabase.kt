package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [StreakState::class, MindfulLog::class, Badge::class, LeaderboardEntry::class],
    version = 1,
    exportSchema = false
)
abstract class MindPlayDatabase : RoomDatabase() {
    abstract fun mindPlayDao(): MindPlayDao

    companion object {
        @Volatile
        private var INSTANCE: MindPlayDatabase? = null

        fun getDatabase(context: Context): MindPlayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MindPlayDatabase::class.java,
                    "mindplay_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
