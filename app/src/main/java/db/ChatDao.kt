package com.example.realtalkai.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChatDao { //
    @Insert //
    suspend fun insert(message: ChatEntity) //

    // --- MODIFIED: Get all messages for a SPECIFIC conversation ---
    @Query("SELECT * FROM chat_table WHERE conversationId = :convoId ORDER BY id ASC")
    suspend fun getMessagesForConversation(convoId: Long): List<ChatEntity>

    // --- NEW: Get a list of all unique conversations, ordered by the newest first ---
    // We'll use this to build the history list in the drawer later.
    @Query("SELECT * FROM chat_table GROUP BY conversationId ORDER BY MAX(timestamp) DESC")
    suspend fun getRecentConversations(): List<ChatEntity>

    // --- MODIFIED: Clear messages for a SPECIFIC conversation ---
    @Query("DELETE FROM chat_table WHERE conversationId = :convoId")
    suspend fun clearChat(convoId: Long)
}