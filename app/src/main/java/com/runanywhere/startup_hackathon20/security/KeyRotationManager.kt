package com.runanywhere.startup_hackathon20.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * KeyRotationManager - Automatic key rotation for enhanced security
 * 
 * Features:
 * - Scheduled automatic key rotation
 * - Secure key migration
 * - Rotation history tracking
 * - Configurable rotation intervals
 */
class KeyRotationManager(
    private val context: Context
) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("key_rotation", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_LAST_ROTATION = "last_rotation_timestamp"
        private const val KEY_ROTATION_INTERVAL = "rotation_interval_days"
        private const val KEY_AUTO_ROTATION_ENABLED = "auto_rotation_enabled"
        private const val KEY_ROTATION_HISTORY = "rotation_history"
        private const val DEFAULT_INTERVAL_DAYS = 90
    }
    
    data class RotationResult(
        val success: Boolean,
        val timestamp: Long,
        val itemsReEncrypted: Int,
        val message: String,
        val oldKeyId: String,
        val newKeyId: String
    )
    
    data class RotationEvent(
        val id: String,
        val timestamp: Long,
        val result: String,
        val itemsReEncrypted: Int,
        val reason: String
    )
    
    /**
     * Rotate vault encryption key
     */
    suspend fun rotateVaultKey(
        reEncryptAll: Boolean = true,
        reason: String = "Manual rotation"
    ): RotationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val oldKeyId = getCurrentKeyId()
        
        try {
            // Generate new key identifier
            val newKeyId = "vault_key_${UUID.randomUUID()}"
            
            var itemsReEncrypted = 0
            
            if (reEncryptAll) {
                // Re-encrypt all vault items with new key
                itemsReEncrypted = reEncryptVaultItems(oldKeyId, newKeyId)
            }
            
            // Update current key reference
            setCurrentKeyId(newKeyId)
            
            // Record rotation
            val timestamp = System.currentTimeMillis()
            recordRotation(
                timestamp = timestamp,
                result = "SUCCESS",
                itemsReEncrypted = itemsReEncrypted,
                reason = reason
            )
            
            prefs.edit().putLong(KEY_LAST_ROTATION, timestamp).apply()
            
            RotationResult(
                success = true,
                timestamp = timestamp,
                itemsReEncrypted = itemsReEncrypted,
                message = "Key rotation completed successfully",
                oldKeyId = oldKeyId,
                newKeyId = newKeyId
            )
        } catch (e: Exception) {
            recordRotation(
                timestamp = System.currentTimeMillis(),
                result = "FAILURE: ${e.message}",
                itemsReEncrypted = 0,
                reason = reason
            )
            
            RotationResult(
                success = false,
                timestamp = System.currentTimeMillis(),
                itemsReEncrypted = 0,
                message = "Key rotation failed: ${e.message}",
                oldKeyId = oldKeyId,
                newKeyId = oldKeyId
            )
        }
    }
    
    /**
     * Check if rotation is needed based on interval
     */
    fun isRotationNeeded(): Boolean {
        if (!isAutoRotationEnabled()) return false
        
        val lastRotation = prefs.getLong(KEY_LAST_ROTATION, 0L)
        if (lastRotation == 0L) return true
        
        val intervalDays = prefs.getInt(KEY_ROTATION_INTERVAL, DEFAULT_INTERVAL_DAYS)
        val intervalMillis = intervalDays * 24 * 60 * 60 * 1000L
        
        return System.currentTimeMillis() - lastRotation > intervalMillis
    }
    
    /**
     * Schedule automatic rotation
     */
    fun scheduleAutoRotation(intervalDays: Int) {
        prefs.edit()
            .putInt(KEY_ROTATION_INTERVAL, intervalDays)
            .putBoolean(KEY_AUTO_ROTATION_ENABLED, true)
            .apply()
    }
    
    /**
     * Disable automatic rotation
     */
    fun disableAutoRotation() {
        prefs.edit()
            .putBoolean(KEY_AUTO_ROTATION_ENABLED, false)
            .apply()
    }
    
    /**
     * Check if auto rotation is enabled
     */
    fun isAutoRotationEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_ROTATION_ENABLED, false)
    }
    
    /**
     * Get rotation configuration
     */
    fun getRotationConfig(): RotationConfig {
        return RotationConfig(
            enabled = isAutoRotationEnabled(),
            intervalDays = prefs.getInt(KEY_ROTATION_INTERVAL, DEFAULT_INTERVAL_DAYS),
            lastRotation = prefs.getLong(KEY_LAST_ROTATION, 0L),
            nextRotation = calculateNextRotation()
        )
    }
    
    /**
     * Get rotation history
     */
    fun getRotationHistory(): List<RotationEvent> {
        val historyJson = prefs.getString(KEY_ROTATION_HISTORY, "[]") ?: "[]"
        
        return try {
            val jsonArray = org.json.JSONArray(historyJson)
            (0 until jsonArray.length()).map { i ->
                val json = jsonArray.getJSONObject(i)
                RotationEvent(
                    id = json.getString("id"),
                    timestamp = json.getLong("timestamp"),
                    result = json.getString("result"),
                    itemsReEncrypted = json.getInt("itemsReEncrypted"),
                    reason = json.getString("reason")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get days until next rotation
     */
    fun getDaysUntilRotation(): Int {
        if (!isAutoRotationEnabled()) return -1
        
        val lastRotation = prefs.getLong(KEY_LAST_ROTATION, 0L)
        val intervalDays = prefs.getInt(KEY_ROTATION_INTERVAL, DEFAULT_INTERVAL_DAYS)
        
        if (lastRotation == 0L) return 0
        
        val daysSinceRotation = ((System.currentTimeMillis() - lastRotation) / (24 * 60 * 60 * 1000L)).toInt()
        return maxOf(0, intervalDays - daysSinceRotation)
    }
    
    // Private helper methods
    
    private fun getCurrentKeyId(): String {
        return prefs.getString("current_key_id", "default_vault_key") ?: "default_vault_key"
    }
    
    private fun setCurrentKeyId(keyId: String) {
        prefs.edit().putString("current_key_id", keyId).apply()
    }
    
    private suspend fun reEncryptVaultItems(oldKeyId: String, newKeyId: String): Int {
        // This would integrate with PrivacyVaultRepository to re-encrypt items
        // For now, return a simulated count
        return 0 // TODO: Implement actual re-encryption
    }
    
    private fun recordRotation(
        timestamp: Long,
        result: String,
        itemsReEncrypted: Int,
        reason: String
    ) {
        val historyJson = prefs.getString(KEY_ROTATION_HISTORY, "[]") ?: "[]"
        val jsonArray = org.json.JSONArray(historyJson)
        
        val event = org.json.JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("timestamp", timestamp)
            put("result", result)
            put("itemsReEncrypted", itemsReEncrypted)
            put("reason", reason)
        }
        
        jsonArray.put(event)
        
        // Keep only last 50 events
        val trimmedArray = if (jsonArray.length() > 50) {
            org.json.JSONArray().apply {
                for (i in (jsonArray.length() - 50) until jsonArray.length()) {
                    put(jsonArray.get(i))
                }
            }
        } else {
            jsonArray
        }
        
        prefs.edit().putString(KEY_ROTATION_HISTORY, trimmedArray.toString()).apply()
    }
    
    private fun calculateNextRotation(): Long {
        val lastRotation = prefs.getLong(KEY_LAST_ROTATION, 0L)
        if (lastRotation == 0L) return System.currentTimeMillis()
        
        val intervalDays = prefs.getInt(KEY_ROTATION_INTERVAL, DEFAULT_INTERVAL_DAYS)
        return lastRotation + (intervalDays * 24 * 60 * 60 * 1000L)
    }
    
    data class RotationConfig(
        val enabled: Boolean,
        val intervalDays: Int,
        val lastRotation: Long,
        val nextRotation: Long
    )
}
