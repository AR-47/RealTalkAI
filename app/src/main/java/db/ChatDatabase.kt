package com.example.realtalkai.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// --- MODIFICATION 1: Increment the version number from 1 to 2 ---
@Database(entities = [ChatEntity::class], version = 2) //
abstract class ChatDatabase : RoomDatabase() { //
    abstract fun chatDao(): ChatDao //

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null
        fun getDatabase(context: Context): ChatDatabase { //
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, //
                    ChatDatabase::class.java, //
                    "chat_database"
                )
                    // --- MODIFICATION 2: Add this to handle the upgrade without crashing ---
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}