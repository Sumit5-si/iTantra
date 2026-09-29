package com.itantra.app.domain.repository

import com.itantra.app.domain.model.MessagePacket
import com.itantra.app.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for message operations.
 * Abstracts data source (Room) from domain/UI layers.
 */
interface MessageRepository {
    fun getMessagesForSession(sessionId: String): Flow<List<MessagePacket>>
    suspend fun saveMessage(packet: MessagePacket, status: MessageStatus)
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)
    suspend fun deleteSessionMessages(sessionId: String)
    suspend fun deleteAllMessages()
}
