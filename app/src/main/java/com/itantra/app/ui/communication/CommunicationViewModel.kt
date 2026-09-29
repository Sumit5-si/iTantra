package com.itantra.app.ui.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.ITantraApplication
import com.itantra.app.core.logging.ITantraLogger
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.ConnectionType
import com.itantra.app.domain.model.Language
import com.itantra.app.domain.model.MessagePacket
import com.itantra.app.domain.model.MessageStatus
import com.itantra.app.domain.model.MessageType
import com.itantra.app.domain.model.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for the Communication Screen.
 *
 * Manages:
 *  - Persistent message list loaded from local Room DB
 *  - Real text sending over offline link (Wi-Fi Direct / Bluetooth)
 *  - Real packet reception and auto-persistence
 *  - Connection state monitoring
 */

data class UIMessage(
    val id: String,
    val text: String,
    val timestamp: Long,
    val isSent: Boolean,
    val status: MessageStatus,
    val priority: Priority = Priority.NORMAL
)

class CommunicationViewModel : ViewModel() {

    companion object {
        private const val TAG = "CommViewModel"
        private const val CHAT_SESSION_ID = "chat_session"
    }

    private val app = ITantraApplication.instance
    private val connectionManager = app.connectionManager
    private val messageRepository = app.messageRepository
    private val ttsEngine = app.ttsEngine

    private val _messages = MutableStateFlow<List<UIMessage>>(emptyList())
    val messages: StateFlow<List<UIMessage>> = _messages.asStateFlow()

    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState

    val connectedDeviceName: StateFlow<String> = connectionManager.connectedDevice
        .map { it?.deviceName ?: "Connected Peer" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Connected Peer")

    val connectionType: StateFlow<ConnectionType?> = connectionManager.connectionType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var sequenceNumber = 0

    init {
        // 1. Observe persistent messages from local Room Database
        viewModelScope.launch {
            messageRepository.getMessagesForSession(CHAT_SESSION_ID).collect { packets ->
                _messages.value = packets.map { packet ->
                    UIMessage(
                        id = packet.messageId,
                        text = packet.payload,
                        timestamp = packet.timestamp,
                        isSent = packet.senderId == "self",
                        status = MessageStatus.DELIVERED,
                        priority = packet.priority
                    )
                }
            }
        }

        // 2. Collect incoming packets in real-time
        viewModelScope.launch {
            connectionManager.receive().collect { packet ->
                if (packet.sessionId == CHAT_SESSION_ID || packet.messageType == MessageType.TEXT) {
                    onMessageReceived(packet)
                }
            }
        }
    }

    fun updateTextInput(text: String) {
        _textInput.value = text
    }

    /**
     * Send a text message over the active offline socket link and persist in Room DB.
     */
    fun sendMessage() {
        val text = _textInput.value.trim()
        if (text.isEmpty()) return

        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // Immediate optimistic UI update
        val optimisticMessage = UIMessage(
            id = messageId,
            text = text,
            timestamp = timestamp,
            isSent = true,
            status = MessageStatus.SENDING
        )
        _messages.value = _messages.value + optimisticMessage
        _textInput.value = ""

        viewModelScope.launch {
            val packet = MessagePacket(
                protocolVersion = 1,
                messageId = messageId,
                senderId = "self",
                receiverId = connectedDeviceName.value,
                sessionId = CHAT_SESSION_ID,
                timestamp = timestamp,
                sequenceNumber = ++sequenceNumber,
                messageType = MessageType.TEXT,
                priority = Priority.NORMAL,
                language = Language.DEFAULT.code,
                payload = text
            )

            try {
                messageRepository.saveMessage(packet, MessageStatus.SENDING)
            } catch (e: Exception) {
                ITantraLogger.w(TAG, "Failed to save message to Room: ${e.message}")
            }

            val result = connectionManager.send(packet)
            if (result.isSuccess) {
                try {
                    messageRepository.updateMessageStatus(messageId, MessageStatus.DELIVERED)
                } catch (_: Exception) {}
                updateLocalStatus(messageId, MessageStatus.DELIVERED)
            } else {
                try {
                    messageRepository.updateMessageStatus(messageId, MessageStatus.FAILED)
                } catch (_: Exception) {}
                updateLocalStatus(messageId, MessageStatus.FAILED)
            }
        }
    }

    /**
     * Called when a message packet is received from the transport layer.
     */
    private fun onMessageReceived(packet: MessagePacket) {
        viewModelScope.launch {
            try {
                messageRepository.saveMessage(packet, MessageStatus.DELIVERED)
            } catch (e: Exception) {
                ITantraLogger.w(TAG, "Error saving received message: ${e.message}")
            }

            val uiMessage = UIMessage(
                id = packet.messageId,
                text = packet.payload,
                timestamp = packet.timestamp,
                isSent = false,
                status = MessageStatus.DELIVERED,
                priority = packet.priority
            )
            if (_messages.value.none { it.id == packet.messageId }) {
                _messages.value = _messages.value + uiMessage
            }
        }
    }

    /**
     * Retry sending a failed message.
     */
    fun retryMessage(messageId: String) {
        val msg = _messages.value.find { it.id == messageId } ?: return
        updateLocalStatus(messageId, MessageStatus.SENDING)
        viewModelScope.launch {
            val packet = MessagePacket(
                protocolVersion = 1,
                messageId = messageId,
                senderId = "self",
                receiverId = connectedDeviceName.value,
                sessionId = CHAT_SESSION_ID,
                timestamp = System.currentTimeMillis(),
                sequenceNumber = ++sequenceNumber,
                messageType = MessageType.TEXT,
                priority = Priority.NORMAL,
                language = Language.DEFAULT.code,
                payload = msg.text
            )
            val result = connectionManager.send(packet)
            if (result.isSuccess) {
                try {
                    messageRepository.updateMessageStatus(messageId, MessageStatus.DELIVERED)
                } catch (_: Exception) {}
                updateLocalStatus(messageId, MessageStatus.DELIVERED)
            } else {
                try {
                    messageRepository.updateMessageStatus(messageId, MessageStatus.FAILED)
                } catch (_: Exception) {}
                updateLocalStatus(messageId, MessageStatus.FAILED)
            }
        }
    }

    private fun updateLocalStatus(messageId: String, status: MessageStatus) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == messageId) msg.copy(status = status) else msg
        }
    }
}
