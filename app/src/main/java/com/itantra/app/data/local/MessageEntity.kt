package com.itantra.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for local message persistence.
 *
 * Stores all messages (sent + received) for conversation history.
 * Does NOT store raw audio — only text content.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val messageId: String,
    val senderId: String,
    val receiverId: String,
    val sessionId: String,
    val timestamp: Long,
    val sequenceNumber: Int,
    val messageType: String,
    val priority: String,
    val language: String,
    val content: String,
    val status: String
)
