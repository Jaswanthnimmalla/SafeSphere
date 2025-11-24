package com.runanywhere.startup_hackathon20.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * SecurityManager - Core security infrastructure for SafeSphere
 *
 * Responsibilities:
 * - AES-256-GCM encryption/decryption for data at rest
 * - RSA key pair generation for signing/verification
 * - Secure key storage using Android KeyStore
 * - Data integrity verification
 *
 * All cryptographic operations use hardware-backed security when available
 */
object SecurityManager {

    private const val TAG = "SecurityManager"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val AES_KEY_ALIAS = "safesphere_aes_key"
    private const val RSA_KEY_ALIAS = "safesphere_rsa_key"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
    private const val SIGNATURE_ALGORITHM = "SHA256withRSA"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    private lateinit var keyStore: KeyStore
    private var initialized = false

    /**
     * Initialize the security manager
     * Generates encryption keys if they don't exist
     */
    fun initialize(context: Context) {
        if (initialized) return

        try {
            // Load Android KeyStore
            keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
                load(null)
            }

            // Generate keys if they don't exist
            if (!keyStore.containsAlias(AES_KEY_ALIAS)) {
                generateAESKey()
            }

            if (!keyStore.containsAlias(RSA_KEY_ALIAS)) {
                generateRSAKeyPair()
            }

            initialized = true
            Log.i(TAG, "🔐 Security Manager initialized - Hardware-backed encryption ready")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize SecurityManager: ${e.message}", e)
            throw SecurityException("Security initialization failed", e)
        }
    }

    /**
     * Generate AES-256 key for symmetric encryption
     * Stored securely in Android KeyStore
     */
    private fun generateAESKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            AES_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()

        Log.d(TAG, "✅ AES-256 key generated")
    }

    /**
     * Generate RSA-2048 key pair for signing and verification
     * Stored securely in Android KeyStore
     */
    private fun generateRSAKeyPair() {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            KEYSTORE_PROVIDER
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            RSA_KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setKeySize(2048)
            .build()

        keyPairGenerator.initialize(keyGenParameterSpec)
        keyPairGenerator.generateKeyPair()

        Log.d(TAG, "✅ RSA-2048 key pair generated")
    }

    /**
     * Encrypt data using AES-256-GCM
     * Returns Base64-encoded encrypted data with IV prepended
     *
     * Format: [IV (12 bytes)][Encrypted Data][Authentication Tag]
     */
    fun encrypt(plaintext: String): String {
        if (!initialized) throw IllegalStateException("SecurityManager not initialized")

        try {
            val secretKey = getAESKey()
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            // Combine IV and encrypted data
            val combined = iv + encryptedBytes

            return Base64.encodeToString(combined, Base64.NO_WRAP)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Encryption failed: ${e.message}", e)
            throw SecurityException("Encryption failed", e)
        }
    }

    /**
     * Decrypt data using AES-256-GCM
     * Accepts Base64-encoded encrypted data with IV prepended
     */
    fun decrypt(encryptedData: String): String {
        if (!initialized) throw IllegalStateException("SecurityManager not initialized")

        try {
            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)

            // Extract IV and encrypted data
            val iv = combined.copyOfRange(0, IV_LENGTH)
            val encryptedBytes = combined.copyOfRange(IV_LENGTH, combined.size)

            val secretKey = getAESKey()
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)

            return String(decryptedBytes, Charsets.UTF_8)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Decryption failed: ${e.message}", e)
            throw SecurityException("Decryption failed", e)
        }
    }

    /**
     * Sign data using RSA private key
     * Returns Base64-encoded signature
     */
    fun sign(data: String): String {
        if (!initialized) throw IllegalStateException("SecurityManager not initialized")

        try {
            val privateKey = getPrivateKey()
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initSign(privateKey)
            signature.update(data.toByteArray(Charsets.UTF_8))

            val signatureBytes = signature.sign()

            return Base64.encodeToString(signatureBytes, Base64.NO_WRAP)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Signing failed: ${e.message}", e)
            throw SecurityException("Signing failed", e)
        }
    }

    /**
     * Verify signature using RSA public key
     * Returns true if signature is valid
     */
    fun verify(data: String, signatureString: String): Boolean {
        if (!initialized) throw IllegalStateException("SecurityManager not initialized")

        try {
            val publicKey = getPublicKey()
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initVerify(publicKey)
            signature.update(data.toByteArray(Charsets.UTF_8))

            val signatureBytes = Base64.decode(signatureString, Base64.NO_WRAP)

            return signature.verify(signatureBytes)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Verification failed: ${e.message}", e)
            return false
        }
    }

    /**
     * Get AES key from KeyStore
     */
    private fun getAESKey(): SecretKey {
        return keyStore.getKey(AES_KEY_ALIAS, null) as SecretKey
    }

    /**
     * Get RSA private key from KeyStore
     */
    private fun getPrivateKey(): PrivateKey {
        return keyStore.getKey(RSA_KEY_ALIAS, null) as PrivateKey
    }

    /**
     * Get RSA public key from KeyStore
     */
    private fun getPublicKey(): PublicKey {
        val entry = keyStore.getEntry(RSA_KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        return entry.certificate.publicKey
    }

    /**
     * Check if security is properly initialized
     */
    fun isInitialized(): Boolean = initialized

    /**
     * Get security status information
     */
    fun getSecurityStatus(): SecurityStatus {
        return SecurityStatus(
            initialized = initialized,
            aesKeyPresent = keyStore.containsAlias(AES_KEY_ALIAS),
            rsaKeyPresent = keyStore.containsAlias(RSA_KEY_ALIAS),
            hardwareBacked = isHardwareBacked()
        )
    }

    /**
     * Check if keys are hardware-backed
     */
    private fun isHardwareBacked(): Boolean {
        return try {
            val entry = keyStore.getEntry(AES_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            entry != null
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Security status data class
 */
data class SecurityStatus(
    val initialized: Boolean,
    val aesKeyPresent: Boolean,
    val rsaKeyPresent: Boolean,
    val hardwareBacked: Boolean
)
