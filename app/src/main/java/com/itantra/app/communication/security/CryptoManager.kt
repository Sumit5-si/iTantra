package com.itantra.app.communication.security

import com.itantra.app.core.logging.ITantraLogger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptographic manager for iTantra message security.
 *
 * Provides:
 *  - ECDH key exchange for session key derivation
 *  - AES-256-GCM authenticated encryption
 *  - Unique IV per message (replay protection)
 *
 * NEVER logs: keys, secrets, plaintext.
 * Uses standard javax.crypto / java.security — no custom crypto.
 */
class CryptoManager {

    companion object {
        private const val TAG = "Crypto"
        private const val EC_ALGORITHM = "EC"
        private const val EC_KEY_SIZE = 256
        private const val AES_ALGORITHM = "AES"
        private const val AES_GCM_CIPHER = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12   // bytes
        private const val GCM_TAG_LENGTH = 128  // bits
    }

    private val secureRandom = SecureRandom()

    /**
     * Generate an ECDH key pair for key exchange.
     */
    fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(EC_ALGORITHM)
        keyPairGenerator.initialize(EC_KEY_SIZE, secureRandom)
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Derive a shared secret from local private key and remote public key using ECDH.
     * Returns AES-256 key derived from the shared secret.
     */
    fun deriveSessionKey(localKeyPair: KeyPair, remotePublicKeyBytes: ByteArray): SecretKey {
        val keyFactory = java.security.KeyFactory.getInstance(EC_ALGORITHM)
        val remotePublicKey = keyFactory.generatePublic(
            java.security.spec.X509EncodedKeySpec(remotePublicKeyBytes)
        )

        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(localKeyPair.private)
        keyAgreement.doPhase(remotePublicKey, true)

        val sharedSecret = keyAgreement.generateSecret()

        // Use first 32 bytes of shared secret as AES-256 key
        // In production, use HKDF for proper key derivation
        val keyBytes = sharedSecret.copyOf(32)
        return SecretKeySpec(keyBytes, AES_ALGORITHM)
    }

    /**
     * Encrypt plaintext using AES-256-GCM.
     *
     * Returns: [12 bytes IV] + [ciphertext + GCM tag]
     */
    fun encrypt(plaintext: ByteArray, sessionKey: SecretKey): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(AES_GCM_CIPHER)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, sessionKey, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext)

        // Prepend IV to ciphertext
        return iv + ciphertext
    }

    /**
     * Decrypt ciphertext using AES-256-GCM.
     *
     * Input format: [12 bytes IV] + [ciphertext + GCM tag]
     * Throws exception if authentication fails (tampered data).
     */
    fun decrypt(encryptedData: ByteArray, sessionKey: SecretKey): ByteArray {
        require(encryptedData.size > GCM_IV_LENGTH) { "Encrypted data too short" }

        val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)

        val cipher = Cipher.getInstance(AES_GCM_CIPHER)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, sessionKey, gcmSpec)

        return cipher.doFinal(ciphertext)
    }

    /**
     * Get the public key bytes for transmission during key exchange.
     */
    fun getPublicKeyBytes(keyPair: KeyPair): ByteArray {
        return keyPair.public.encoded
    }
}
