package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DesignProjectEntity::class],
    version = 1,
    exportSchema = false
)
abstract class KanvoDatabase : RoomDatabase() {
    abstract fun designProjectDao(): DesignProjectDao

    companion object {
        @Volatile
        private var INSTANCE: KanvoDatabase? = null

        fun getInstance(context: Context): KanvoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KanvoDatabase::class.java,
                    "kanvo_studio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
