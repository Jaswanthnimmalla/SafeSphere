package com.runanywhere.startup_hackathon20.backup

import android.content.Context
import android.net.Uri
import com.runanywhere.startup_hackathon20.data.PasswordVaultRepository
import com.runanywhere.startup_hackathon20.data.PrivacyVaultRepository
import com.runanywhere.startup_hackathon20.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * BackupManager - Secure Backup & Restore System
 *
 * Features:
 * - Export encrypted vault to file
 * - Restore from encrypted backup
 * - Password-protected backups
 * - Supports passwords + privacy vault
 * - No cloud dependency
 */
class BackupManager(private val context: Context) {

    private val passwordRepo = PasswordVaultRepository.getInstance(context)
    private val vaultRepo = PrivacyVaultRepository.getInstance(context)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    companion object {
        private const val BACKUP_VERSION = 1
        private const val SALT_LENGTH = 32
        private const val IV_LENGTH = 16
        private const val KEY_LENGTH = 256
        private const val ITERATION_COUNT = 100000

        @Volatile
        private var instance: BackupManager? = null

        fun getInstance(context: Context): BackupManager {
            return instance ?: synchronized(this) {
                instance ?: BackupManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Create encrypted backup file
     */
    suspend fun createBackup(
        password: String,
        includePasswords: Boolean = true,
        includeVault: Boolean = true,
        outputUri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Collect data
            val backupData = BackupData(
                version = BACKUP_VERSION,
                timestamp = System.currentTimeMillis(),
                passwords = if (includePasswords) {
                    passwordRepo.passwords.first().map { entry ->
                        BackupPasswordEntry(
                            id = entry.id,
                            service = entry.service,
                            username = entry.username,
                            encryptedPassword = entry.encryptedPassword,
                            url = entry.url,
                            notes = entry.notes,
                            category = entry.category.name,
                            strengthScore = entry.strengthScore,
                            createdAt = entry.createdAt,
                            updatedAt = entry.modifiedAt
                        )
                    }
                } else emptyList(),
                vaultItems = if (includeVault) {
                    vaultRepo.vaultItems.first().map { item ->
                        BackupVaultItem(
                            id = item.id,
                            title = item.title,
                            encryptedContent = item.encryptedContent,
                            category = item.category.name,
                            size = item.size.toLong(),
                            isEncrypted = item.isEncrypted,
                            createdAt = item.createdAt,
                            updatedAt = item.createdAt
                        )
                    }
                } else emptyList()
            )

            // Serialize to JSON
            val jsonData = json.encodeToString(backupData)

            // Encrypt with user password
            val encryptedData = encryptWithPassword(jsonData, password)

            // Write to file
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                outputStream.write(encryptedData)
            } ?: throw IOException("Cannot open output stream")

            val itemCount = backupData.passwords.size + backupData.vaultItems.size
            Result.success("Backup created: $itemCount items")

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restore from encrypted backup
     */
    suspend fun restoreBackup(
        password: String,
        inputUri: Uri,
        mergeMode: Boolean = false // true = merge, false = replace
    ): Result<RestoreResult> = withContext(Dispatchers.IO) {
        try {
            // Read encrypted data
            val encryptedData =
                context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                    inputStream.readBytes()
                } ?: throw IOException("Cannot open input stream")

            // Decrypt with user password
            val jsonData = decryptWithPassword(encryptedData, password)

            // Deserialize
            val backupData = json.decodeFromString<BackupData>(jsonData)

            // Validate version
            if (backupData.version > BACKUP_VERSION) {
                throw IllegalStateException("Backup version too new. Update SafeSphere app.")
            }

            var passwordsRestored = 0
            var vaultItemsRestored = 0

            // Clear existing data if replace mode
            if (!mergeMode) {
                passwordRepo.clearAllPasswords()
                vaultRepo.clearVault()
            }

            // Restore passwords
            backupData.passwords.forEach { entry ->
                try {
                    passwordRepo.savePasswordFromBackup(
                        id = entry.id,
                        service = entry.service,
                        username = entry.username,
                        encryptedPassword = entry.encryptedPassword,
                        url = entry.url,
                        notes = entry.notes,
                        category = entry.category,
                        strengthScore = entry.strengthScore,
                        createdAt = entry.createdAt,
                        updatedAt = entry.updatedAt
                    )
                    passwordsRestored++
                } catch (e: Exception) {
                    // Skip duplicates in merge mode
                    if (!mergeMode) throw e
                }
            }

            // Restore vault items
            backupData.vaultItems.forEach { item ->
                try {
                    vaultRepo.restoreItemFromBackup(
                        id = item.id,
                        title = item.title,
                        encryptedContent = item.encryptedContent,
                        category = item.category,
                        size = item.size,
                        isEncrypted = item.isEncrypted,
                        createdAt = item.createdAt
                    )
                    vaultItemsRestored++
                } catch (e: Exception) {
                    // Skip duplicates in merge mode
                    if (!mergeMode) throw e
                }
            }

            Result.success(
                RestoreResult(
                    passwordsRestored = passwordsRestored,
                    vaultItemsRestored = vaultItemsRestored,
                    backupDate = Date(backupData.timestamp)
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate backup filename
     */
    fun generateBackupFilename(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
        val timestamp = dateFormat.format(Date())
        return "SafeSphere_Backup_$timestamp.safesphere"
    }

    /**
     * Encrypt data with password-based encryption
     */
    private fun encryptWithPassword(data: String, password: String): ByteArray {
        // Generate random salt
        val salt = ByteArray(SALT_LENGTH)
        java.security.SecureRandom().nextBytes(salt)

        // Generate random IV
        val iv = ByteArray(IV_LENGTH)
        java.security.SecureRandom().nextBytes(iv)

        // Derive key from password using PBKDF2
        val keySpec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = keyFactory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        // Encrypt data
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        // Combine: salt + iv + encrypted data
        val outputStream = ByteArrayOutputStream()
        outputStream.write(salt)
        outputStream.write(iv)
        outputStream.write(encryptedBytes)

        return outputStream.toByteArray()
    }

    /**
     * Decrypt data with password-based encryption
     */
    private fun decryptWithPassword(encryptedData: ByteArray, password: String): String {
        val inputStream = ByteArrayInputStream(encryptedData)

        // Read salt
        val salt = ByteArray(SALT_LENGTH)
        inputStream.read(salt)

        // Read IV
        val iv = ByteArray(IV_LENGTH)
        inputStream.read(iv)

        // Read encrypted data
        val encryptedBytes = inputStream.readBytes()

        // Derive key from password
        val keySpec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = keyFactory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        // Decrypt data
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        val decryptedBytes = cipher.doFinal(encryptedBytes)

        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Validate backup file
     */
    suspend fun validateBackup(inputUri: Uri, password: String): Result<BackupInfo> =
        withContext(Dispatchers.IO) {
            try {
                val encryptedData =
                    context.contentResolver.openInputStream(inputUri)?.use { it.readBytes() }
                        ?: throw IOException("Cannot open file")

                val jsonData = decryptWithPassword(encryptedData, password)
                val backupData = json.decodeFromString<BackupData>(jsonData)

                Result.success(
                    BackupInfo(
                        version = backupData.version,
                        timestamp = Date(backupData.timestamp),
                        passwordCount = backupData.passwords.size,
                        vaultItemCount = backupData.vaultItems.size
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

/**
 * Backup data structure
 */
@Serializable
data class BackupData(
    val version: Int,
    val timestamp: Long,
    val passwords: List<BackupPasswordEntry>,
    val vaultItems: List<BackupVaultItem>
)

@Serializable
data class BackupPasswordEntry(
    val id: String,
    val service: String,
    val username: String,
    val encryptedPassword: String,
    val url: String,
    val notes: String,
    val category: String,
    val strengthScore: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BackupVaultItem(
    val id: String,
    val title: String,
    val encryptedContent: String,
    val category: String,
    val size: Long,
    val isEncrypted: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Restore result
 */
data class RestoreResult(
    val passwordsRestored: Int,
    val vaultItemsRestored: Int,
    val backupDate: Date
)

/**
 * Backup info (for validation)
 */
data class BackupInfo(
    val version: Int,
    val timestamp: Date,
    val passwordCount: Int,
    val vaultItemCount: Int
)
