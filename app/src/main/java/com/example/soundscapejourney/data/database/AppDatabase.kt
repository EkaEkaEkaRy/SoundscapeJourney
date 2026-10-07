package com.example.soundscapejourney.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.soundscapejourney.data.database.dao.RemixDao
import com.example.soundscapejourney.data.database.entities.RemixEntity
import com.example.soundscapejourney.data.database.entities.SoundEntity

@Database(entities = [RemixEntity::class, SoundEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun remixDao(): RemixDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "soundscape_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
