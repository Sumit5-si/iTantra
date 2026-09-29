package com.itantra.app.data.repository

import com.itantra.app.data.local.MessageDao
import com.itantra.app.data.local.MessageEntity
import com.itantra.app.domain.model.MessagePacket
import com.itantra.app.domain.model.MessageStatus
import com.itantra.app.domain.model.MessageType
import com.itantra.app.domain.model.Priority
import com.itantra.app.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of MessageRepository using Room for local persistence.
 */
class MessageRepositoryImpl(
    private val messageDao: MessageDao
) : MessageRepository {

    override fun getMessagesForSession(sessionId: String): Flow<List<MessagePacket>> {
        return messageDao.getMessagesForSession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveMessage(packet: MessagePacket, status: MessageStatus) {
        messageDao.insertMessage(packet.toEntity(status))
    }

    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        messageDao.updateStatus(messageId, status.name)
    }

    override suspend fun deleteSessionMessages(sessionId: String) {
        messageDao.deleteSessionMessages(sessionId)
    }

    override suspend fun deleteAllMessages() {
        messageDao.deleteAllMessages()
    }

    private fun MessageEntity.toDomain(): MessagePacket {
        return MessagePacket(
            messageId = messageId,
            senderId = senderId,
            receiverId = receiverId,
            sessionId = sessionId,
            timestamp = timestamp,
            sequenceNumber = sequenceNumber,
            messageType = MessageType.valueOf(messageType),
            priority = Priority.valueOf(priority),
            language = language,
            payload = content
        )
    }

    private fun MessagePacket.toEntity(status: MessageStatus): MessageEntity {
        return MessageEntity(
            messageId = messageId,
            senderId = senderId,
            receiverId = receiverId,
            sessionId = sessionId,
            timestamp = timestamp,
            sequenceNumber = sequenceNumber,
            messageType = messageType.name,
            priority = priority.name,
            language = language,
            content = payload,
            status = status.name
        )
    }
}
