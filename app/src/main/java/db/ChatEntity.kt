package com.example.realtalkai.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_table")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // --- NEW: Add a field to group messages by conversation ---
    val conversationId: Long,
    val text: String, //
    val sender: String, //
    val timestamp: Long = System.currentTimeMillis() //
)