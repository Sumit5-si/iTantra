package com.itantra.app.domain.model

import java.util.UUID

/**
 * Core message packet transmitted between devices.
 *
 * Per project rules: packets must be compact JSON under 256 bytes.
 * Raw audio is NEVER transmitted — only text payloads.
 */
data class MessagePacket(
    val protocolVersion: Int = 1,
    val messageId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val receiverId: String,
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sequenceNumber: Int,
    val messageType: MessageType,
    val priority: Priority,
    val language: String,
    val payload: String,
    val checksum: Int = 0
)

enum class MessageType {
    TEXT,
    VOICE_TRANSCRIPT,
    ALERT,
    SYSTEM
}

enum class Priority {
    NORMAL,
    HIGH,
    EMERGENCY
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    FAILED
}

enum class CommunicationMode {
    WALKIE,
    PHONE
}

enum class ConnectionType {
    WIFI_DIRECT,
    BLUETOOTH
}

enum class ConnectionState {
    DISCONNECTED,
    DISCOVERING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}

/**
 * Voice button UI states for visual feedback.
 */
enum class VoiceButtonState {
    IDLE,
    LISTENING,
    PROCESSING,
    SENDING,
    ERROR
}
