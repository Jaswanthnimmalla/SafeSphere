package com.runanywhere.startup_hackathon20.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * SecureCameraManager - Handles secure photo encryption/decryption
 *
 * Features:
 * - In-app photo encryption
 * - Instant AES-256 encryption
 * - Secure storage
 * - Thumbnail generation
 */
class SecureCameraManager(private val context: Context) {

    /**
     * Encrypt captured bitmap
     */
    fun encryptBitmap(
        bitmap: Bitmap,
        onSuccess: (EncryptedPhoto) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Compress to JPEG
            val jpegBytes = bitmapToJpeg(bitmap)

            // Convert to Base64 for encryption
            val base64Data = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

            // Encrypt using SecurityManager
            val encryptedData = SecurityManager.encrypt(base64Data)

            // Create photo metadata
            val photo = EncryptedPhoto(
                encryptedData = encryptedData,
                width = bitmap.width,
                height = bitmap.height,
                timestamp = System.currentTimeMillis(),
                sizeBytes = jpegBytes.size
            )

            onSuccess(photo)

        } catch (e: Exception) {
            onError("Failed to encrypt image: ${e.message}")
        }
    }

    /**
     * Decrypt and load photo
     */
    fun decryptPhoto(encryptedPhoto: EncryptedPhoto): Bitmap? {
        return try {
            // Decrypt using SecurityManager
            val base64Data = SecurityManager.decrypt(encryptedPhoto.encryptedData)

            // Decode from Base64
            val jpegBytes = Base64.decode(base64Data, Base64.NO_WRAP)

            // Decode JPEG to Bitmap
            BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert bitmap to JPEG bytes
     */
    private fun bitmapToJpeg(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Get thumbnail for preview
     */
    fun getThumbnail(encryptedPhoto: EncryptedPhoto, maxSize: Int = 200): Bitmap? {
        val fullBitmap = decryptPhoto(encryptedPhoto) ?: return null

        val maxDimension =
            if (fullBitmap.width > fullBitmap.height) fullBitmap.width else fullBitmap.height
        val scale = maxSize.toFloat() / maxDimension
        val newWidth = (fullBitmap.width * scale).toInt()
        val newHeight = (fullBitmap.height * scale).toInt()

        return Bitmap.createScaledBitmap(fullBitmap, newWidth, newHeight, true)
    }
}

/**
 * Encrypted photo data
 */
data class EncryptedPhoto(
    val encryptedData: String, // Encrypted Base64 JPEG
    val width: Int,
    val height: Int,
    val timestamp: Long,
    val sizeBytes: Int
) {
    fun getFormattedSize(): String {
        return when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            else -> "${"%.1f".format(sizeBytes / (1024f * 1024f))} MB"
        }
    }
}
