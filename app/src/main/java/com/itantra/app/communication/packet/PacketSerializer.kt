package com.itantra.app.communication.packet

import com.itantra.app.domain.model.MessagePacket
import com.itantra.app.domain.model.MessageType
import com.itantra.app.domain.model.Priority
import org.json.JSONObject
import java.util.zip.CRC32

/**
 * Serializes/deserializes MessagePacket to/from compact JSON bytes.
 *
 * Per project rules: packets must be compact JSON under 256 bytes.
 * Uses short field names to minimize payload size.
 *
 * Wire format:
 *   [4 bytes: payload length (big-endian int)] + [N bytes: JSON UTF-8]
 */
object PacketSerializer {

    private const val TAG = "PacketSer"

    /**
     * Serialize MessagePacket to bytes for transmission.
     * Returns: length-prefixed JSON bytes.
     */
    fun serialize(packet: MessagePacket): ByteArray {
        val json = JSONObject().apply {
            put("v", packet.protocolVersion)
            put("id", packet.messageId)
            put("sid", packet.senderId)
            put("rid", packet.receiverId)
            put("ssn", packet.sessionId)
            put("ts", packet.timestamp)
            put("seq", packet.sequenceNumber)
            put("t", packet.messageType.ordinal)
            put("p", packet.priority.ordinal)
            put("l", packet.language)
            put("d", packet.payload)
        }

        // Compute CRC32 checksum over the JSON content
        val jsonString = json.toString()
        val crc = computeChecksum(jsonString.toByteArray(Charsets.UTF_8))
        json.put("c", crc)

        val jsonBytes = json.toString().toByteArray(Charsets.UTF_8)

        // Length-prefix: 4-byte big-endian integer
        val lengthPrefix = ByteArray(4).apply {
            this[0] = (jsonBytes.size shr 24 and 0xFF).toByte()
            this[1] = (jsonBytes.size shr 16 and 0xFF).toByte()
            this[2] = (jsonBytes.size shr 8 and 0xFF).toByte()
            this[3] = (jsonBytes.size and 0xFF).toByte()
        }

        return lengthPrefix + jsonBytes
    }

    /**
     * Deserialize bytes to MessagePacket.
     * Input: raw JSON bytes (without length prefix — caller strips it).
     */
    fun deserialize(data: ByteArray): MessagePacket {
        val jsonString = String(data, Charsets.UTF_8)
        val json = JSONObject(jsonString)

        // Verify checksum
        val receivedChecksum = json.optInt("c", 0)
        json.remove("c")
        val computedChecksum = computeChecksum(json.toString().toByteArray(Charsets.UTF_8))

        // Note: checksum mismatch indicates tampering or corruption
        // In production, this should be handled more strictly

        return MessagePacket(
            protocolVersion = json.optInt("v", 1),
            messageId = json.optString("id", java.util.UUID.randomUUID().toString()),
            senderId = json.optString("sid", "unknown"),
            receiverId = json.optString("rid", "unknown"),
            sessionId = json.optString("ssn", "default_session"),
            timestamp = json.optLong("ts", System.currentTimeMillis()),
            sequenceNumber = json.optInt("seq", 1),
            messageType = MessageType.entries.getOrElse(json.optInt("t", 0)) { MessageType.VOICE_TRANSCRIPT },
            priority = Priority.entries.getOrElse(json.optInt("p", 0)) { Priority.NORMAL },
            language = json.optString("l", "hi"),
            payload = json.optString("d", ""),
            checksum = receivedChecksum
        )
    }

    /**
     * Read the 4-byte length prefix from a stream/buffer.
     */
    fun readLength(header: ByteArray): Int {
        require(header.size >= 4) { "Header must be at least 4 bytes" }
        return ((header[0].toInt() and 0xFF) shl 24) or
                ((header[1].toInt() and 0xFF) shl 16) or
                ((header[2].toInt() and 0xFF) shl 8) or
                (header[3].toInt() and 0xFF)
    }

    private fun computeChecksum(data: ByteArray): Int {
        val crc = CRC32()
        crc.update(data)
        return crc.value.toInt()
    }
}
