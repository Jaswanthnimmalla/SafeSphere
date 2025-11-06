package com.runanywhere.startup_hackathon20.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.runanywhere.startup_hackathon20.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PrivacyVaultRepository - Manages encrypted local storage
 *
 * Architecture:
 * - All data stored in encrypted JSON files
 * - Each item encrypted individually with AES-256
 * - Integrity verified with RSA signatures
 * - No cloud storage, no external dependencies
 * - Fully offline operation
 *
 * This is the core data privacy layer of SafeSphere
 */
class PrivacyVaultRepository(private val context: Context) {

    private val gson = Gson()
    private val vaultFile = File(context.filesDir, "safesphere_vault.json")

    private val _vaultItems = MutableStateFlow<List<PrivacyVaultItem>>(emptyList())
    val vaultItems: StateFlow<List<PrivacyVaultItem>> = _vaultItems.asStateFlow()

    companion object {
        private const val TAG = "PrivacyVault"

        @Volatile
        private var instance: PrivacyVaultRepository? = null

        fun getInstance(context: Context): PrivacyVaultRepository {
            return instance ?: synchronized(this) {
                instance ?: PrivacyVaultRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    init {
        loadVaultItems()
    }

    /**
     * Load all vault items from encrypted storage
     */
    private fun loadVaultItems() {
        try {
            if (!vaultFile.exists()) {
                Log.d(TAG, "Vault file doesn't exist, initializing empty vault")
                _vaultItems.value = emptyList()
                return
            }

            val json = vaultFile.readText()
            if (json.isBlank()) {
                _vaultItems.value = emptyList()
                return
            }

            val type = object : TypeToken<List<PrivacyVaultItem>>() {}.type
            val items: List<PrivacyVaultItem> = gson.fromJson(json, type)

            _vaultItems.value = items
            Log.i(TAG, "✅ Loaded ${items.size} encrypted items from vault")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load vault: ${e.message}", e)
            _vaultItems.value = emptyList()
        }
    }

    /**
     * Save vault items to encrypted storage
     */
    private suspend fun saveVaultItems() = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(_vaultItems.value)
            vaultFile.writeText(json)
            Log.d(TAG, "✅ Vault saved to disk")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save vault: ${e.message}", e)
            throw e
        }
    }

    /**
     * Add new item to vault with encryption
     */
    suspend fun addItem(
        title: String,
        content: String,
        category: VaultCategory
    ): Result<PrivacyVaultItem> = withContext(Dispatchers.IO) {
        try {
            // Encrypt the content
            val encryptedContent = SecurityManager.encrypt(content)

            // Sign the encrypted content for integrity
            val signature = SecurityManager.sign(encryptedContent)

            // Create vault item
            val item = PrivacyVaultItem(
                title = title,
                encryptedContent = encryptedContent,
                category = category,
                signature = signature,
                isEncrypted = true,
                size = encryptedContent.length
            )

            // Add to list
            val currentItems = _vaultItems.value.toMutableList()
            currentItems.add(item)
            _vaultItems.value = currentItems

            // Persist to disk
            saveVaultItems()

            Log.i(TAG, "✅ Added encrypted item: $title (${item.size} bytes)")
            Result.success(item)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to add item: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieve and decrypt item by ID
     */
    suspend fun getDecryptedItem(id: String): Result<DecryptedVaultItem> =
        withContext(Dispatchers.IO) {
            try {
                val item = _vaultItems.value.find { it.id == id }
                    ?: return@withContext Result.failure(Exception("Item not found"))

                // Verify signature first
                if (!SecurityManager.verify(item.encryptedContent, item.signature)) {
                    Log.w(TAG, "⚠️ Signature verification failed for item: ${item.title}")
                    return@withContext Result.failure(SecurityException("Data integrity check failed"))
                }

                // Decrypt content
                val decryptedContent = SecurityManager.decrypt(item.encryptedContent)

                val decryptedItem = DecryptedVaultItem(
                    id = item.id,
                    title = item.title,
                    content = decryptedContent,
                    category = item.category,
                    createdAt = item.createdAt,
                    modifiedAt = item.modifiedAt
                )

                Log.d(TAG, "✅ Decrypted item: ${item.title}")
                Result.success(decryptedItem)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to decrypt item: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Update existing vault item
     */
    suspend fun updateItem(
        id: String,
        title: String,
        content: String,
        category: VaultCategory
    ): Result<PrivacyVaultItem> = withContext(Dispatchers.IO) {
        try {
            val currentItems = _vaultItems.value.toMutableList()
            val index = currentItems.indexOfFirst { it.id == id }

            if (index == -1) {
                return@withContext Result.failure(Exception("Item not found"))
            }

            // Encrypt new content
            val encryptedContent = SecurityManager.encrypt(content)
            val signature = SecurityManager.sign(encryptedContent)

            // Update item
            val updatedItem = currentItems[index].copy(
                title = title,
                encryptedContent = encryptedContent,
                category = category,
                modifiedAt = System.currentTimeMillis(),
                signature = signature,
                size = encryptedContent.length
            )

            currentItems[index] = updatedItem
            _vaultItems.value = currentItems

            // Persist
            saveVaultItems()

            Log.i(TAG, "✅ Updated item: $title")
            Result.success(updatedItem)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update item: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete item from vault
     */
    suspend fun deleteItem(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentItems = _vaultItems.value.toMutableList()
            val removed = currentItems.removeIf { it.id == id }

            if (!removed) {
                return@withContext Result.failure(Exception("Item not found"))
            }

            _vaultItems.value = currentItems
            saveVaultItems()

            Log.i(TAG, "✅ Deleted item from vault")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete item: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get storage statistics for Data Map
     */
    fun getStorageStats(): StorageStats {
        val items = _vaultItems.value
        val categoryBreakdown = items.groupBy { it.category }
            .mapValues { it.value.size }

        val totalSize = items.sumOf { it.size.toLong() }
        val encryptedCount = items.count { it.isEncrypted }

        // Calculate security score (0-100)
        val securityScore = when {
            items.isEmpty() -> 100
            encryptedCount == items.size -> 100
            encryptedCount > items.size * 0.8 -> 80
            encryptedCount > items.size * 0.5 -> 60
            else -> 40
        }

        return StorageStats(
            totalItems = items.size,
            encryptedItems = encryptedCount,
            totalSize = totalSize,
            categoryBreakdown = categoryBreakdown,
            securityScore = securityScore
        )
    }

    /**
     * Clear all vault data (for testing/reset)
     */
    suspend fun clearVault(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _vaultItems.value = emptyList()
            if (vaultFile.exists()) {
                vaultFile.delete()
            }
            Log.i(TAG, "✅ Vault cleared")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to clear vault: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Initialize vault with demo data for showcasing features
     */
    suspend fun initializeDemoData() {
        if (_vaultItems.value.isNotEmpty()) {
            Log.d(TAG, "Vault already has data, skipping demo initialization")
            return
        }

        Log.i(TAG, "Initializing demo data...")

        addItem(
            "Social Security Number",
            "123-45-6789",
            VaultCategory.PERSONAL
        )

        addItem(
            "Credit Card",
            "Card Number: 4532-1234-5678-9010\nCVV: 123\nExpiry: 12/25",
            VaultCategory.FINANCIAL
        )

        addItem(
            "Gmail Password",
            "MySecureP@ssw0rd2024!",
            VaultCategory.PASSWORDS
        )

        addItem(
            "Medical Records",
            "Blood Type: O+\nAllergies: Penicillin\nLast Checkup: 2024-01-15",
            VaultCategory.MEDICAL
        )

        addItem(
            "Private Notes",
            "Remember to update security protocols quarterly. Current encryption: AES-256-GCM",
            VaultCategory.NOTES
        )

        Log.i(TAG, "✅ Demo data initialized with ${_vaultItems.value.size} encrypted items")
    }
}
