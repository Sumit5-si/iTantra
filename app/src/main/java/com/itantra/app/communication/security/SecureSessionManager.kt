package com.itantra.app.communication.security

import com.itantra.app.core.logging.ITantraLogger
import java.security.KeyPair
import javax.crypto.SecretKey

/**
 * Manages secure communication sessions between devices.
 *
 * Lifecycle:
 *  1. Generate local key pair
 *  2. Exchange public keys with peer
 *  3. Derive shared session key
 *  4. All messages encrypted/decrypted with session key
 *  5. Session destroyed on disconnect
 *
 * Replay protection: monotonic sequence numbers + timestamp validation.
 */
class SecureSessionManager {

    companion object {
        private const val TAG = "SecSession"
        private const val TIMESTAMP_WINDOW_MS = 60_000L // 1 minute window
    }

    private val cryptoManager = CryptoManager()
    private var localKeyPair: KeyPair? = null
    private var sessionKey: SecretKey? = null
    private var lastReceivedSequence = -1
    private var isSessionEstablished = false

    /**
     * Initialize a new session. Generates local key pair.
     * Returns: public key bytes to send to peer.
     */
    fun initSession(): ByteArray {
        localKeyPair = cryptoManager.generateKeyPair()
        isSessionEstablished = false
        lastReceivedSequence = -1
        ITantraLogger.i(TAG, "Session initialized, key pair generated")
        return cryptoManager.getPublicKeyBytes(localKeyPair!!)
    }

    /**
     * Complete session setup with peer's public key.
     * Derives the shared session key via ECDH.
     */
    fun establishSession(remotePublicKeyBytes: ByteArray) {
        val kp = localKeyPair ?: throw IllegalStateException("Session not initialized")
        sessionKey = cryptoManager.deriveSessionKey(kp, remotePublicKeyBytes)
        isSessionEstablished = true
        ITantraLogger.i(TAG, "Session established with peer")
    }

    /**
     * Encrypt a message payload.
     */
    fun encrypt(plaintext: ByteArray): ByteArray {
        val key = sessionKey ?: throw IllegalStateException("Session not established")
        return cryptoManager.encrypt(plaintext, key)
    }

    /**
     * Decrypt a message payload.
     * Validates sequence number for replay protection.
     */
    fun decrypt(encryptedData: ByteArray, sequenceNumber: Int, timestamp: Long): ByteArray {
        val key = sessionKey ?: throw IllegalStateException("Session not established")

        // Replay protection: reject old sequence numbers
        if (sequenceNumber <= lastReceivedSequence) {
            throw SecurityException("Replay attack detected: seq=$sequenceNumber <= last=$lastReceivedSequence")
        }

        // Timestamp validation: reject messages outside acceptable window
        val now = System.currentTimeMillis()
        if (kotlin.math.abs(now - timestamp) > TIMESTAMP_WINDOW_MS) {
            ITantraLogger.w(TAG, "Message timestamp outside window: delta=${now - timestamp}ms")
        }

        val plaintext = cryptoManager.decrypt(encryptedData, key)
        lastReceivedSequence = sequenceNumber

        return plaintext
    }

    fun isEstablished(): Boolean = isSessionEstablished

    /**
     * Destroy the session. Clears all keying material.
     */
    fun destroySession() {
        localKeyPair = null
        sessionKey = null
        isSessionEstablished = false
        lastReceivedSequence = -1
        ITantraLogger.i(TAG, "Session destroyed")
    }
}
