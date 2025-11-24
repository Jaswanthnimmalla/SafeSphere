package com.runanywhere.startup_hackathon20.security

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.*

/**
 * AuditLogger - Blockchain-style immutable audit trail
 *
 * Features:
 * - Hash chain linking for tamper detection
 * - Chronological action logging
 * - Chain-of-custody tracking
 * - Integrity verification
 */
class AuditLogger(private val context: Context) {

    private val auditFile = File(context.filesDir, "audit_log.json")
    private val hashAlgorithm = "SHA-256"

    data class AuditEntry(
        val id: String,
        val timestamp: Long,
        val action: String,
        val itemId: String?,
        val actor: String,
        val result: String,
        val details: String?,
        val previousHash: String,
        val currentHash: String
    )

    enum class Action {
        VAULT_CREATED,
        VAULT_OPENED,
        VAULT_CLOSED,
        ITEM_ADDED,
        ITEM_ACCESSED,
        ITEM_MODIFIED,
        ITEM_DELETED,
        ITEM_ENCRYPTED,
        ITEM_DECRYPTED,
        MODEL_LOADED,
        MODEL_UNLOADED,
        MODEL_VERIFIED,
        KEY_ROTATED,
        BACKUP_CREATED,
        BACKUP_RESTORED,
        POLICY_APPLIED,
        THREAT_DETECTED,
        SYNC_INITIATED,
        SYNC_COMPLETED
    }

    /**
     * Log an action to the audit trail
     */
    suspend fun logAction(
        action: Action,
        itemId: String? = null,
        result: String = "SUCCESS",
        details: String? = null,
        actor: String = "USER"
    ): AuditEntry = withContext(Dispatchers.IO) {
        val previousHash = getLastHash()
        val timestamp = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()

        val dataToHash =
            "$id|$timestamp|${action.name}|$itemId|$actor|$result|$details|$previousHash"
        val currentHash = computeHash(dataToHash)

        val entry = AuditEntry(
            id = id,
            timestamp = timestamp,
            action = action.name,
            itemId = itemId,
            actor = actor,
            result = result,
            details = details,
            previousHash = previousHash,
            currentHash = currentHash
        )

        appendEntry(entry)
        entry
    }

    /**
     * Get complete chain of custody for an item
     */
    suspend fun getChainOfCustody(itemId: String): List<AuditEntry> = withContext(Dispatchers.IO) {
        getAllEntries().filter { it.itemId == itemId }
    }

    /**
     * Verify integrity of entire audit chain
     */
    suspend fun verifyIntegrity(): VerificationResult = withContext(Dispatchers.IO) {
        val entries = getAllEntries()
        if (entries.isEmpty()) {
            return@withContext VerificationResult(true, "No entries to verify")
        }

        var previousHash = "GENESIS"
        for ((index, entry) in entries.withIndex()) {
            // Verify previous hash matches
            if (entry.previousHash != previousHash) {
                return@withContext VerificationResult(
                    false,
                    "Hash chain broken at entry $index (ID: ${entry.id})"
                )
            }

            // Verify current hash is correct
            val dataToHash =
                "${entry.id}|${entry.timestamp}|${entry.action}|${entry.itemId}|${entry.actor}|${entry.result}|${entry.details}|${entry.previousHash}"
            val expectedHash = computeHash(dataToHash)

            if (entry.currentHash != expectedHash) {
                return@withContext VerificationResult(
                    false,
                    "Entry tampering detected at index $index (ID: ${entry.id})"
                )
            }

            previousHash = entry.currentHash
        }

        VerificationResult(true, "Audit chain verified: ${entries.size} entries intact")
    }

    /**
     * Get all audit entries
     */
    suspend fun getAllEntries(): List<AuditEntry> = withContext(Dispatchers.IO) {
        if (!auditFile.exists()) return@withContext emptyList()

        try {
            val content = auditFile.readText()
            val jsonArray = org.json.JSONArray(content)

            (0 until jsonArray.length()).map { i ->
                val json = jsonArray.getJSONObject(i)
                AuditEntry(
                    id = json.getString("id"),
                    timestamp = json.getLong("timestamp"),
                    action = json.getString("action"),
                    itemId = json.optString("itemId").takeIf { it.isNotEmpty() },
                    actor = json.getString("actor"),
                    result = json.getString("result"),
                    details = json.optString("details").takeIf { it.isNotEmpty() },
                    previousHash = json.getString("previousHash"),
                    currentHash = json.getString("currentHash")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get recent entries (last N)
     */
    suspend fun getRecentEntries(count: Int = 50): List<AuditEntry> = withContext(Dispatchers.IO) {
        getAllEntries().takeLast(count)
    }

    /**
     * Export audit log as JSON
     */
    suspend fun exportLog(outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (auditFile.exists()) {
                auditFile.copyTo(outputFile, overwrite = true)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get statistics
     */
    suspend fun getStatistics(): AuditStatistics = withContext(Dispatchers.IO) {
        val entries = getAllEntries()
        val actionCounts = entries.groupBy { it.action }.mapValues { it.value.size }
        val successCount = entries.count { it.result == "SUCCESS" }
        val failureCount = entries.count { it.result == "FAILURE" }

        AuditStatistics(
            totalEntries = entries.size,
            actionCounts = actionCounts,
            successCount = successCount,
            failureCount = failureCount,
            firstEntry = entries.firstOrNull()?.timestamp,
            lastEntry = entries.lastOrNull()?.timestamp
        )
    }

    // Private helper methods

    private fun getLastHash(): String {
        if (!auditFile.exists()) return "GENESIS"

        try {
            val content = auditFile.readText()
            val jsonArray = org.json.JSONArray(content)
            if (jsonArray.length() == 0) return "GENESIS"

            val lastEntry = jsonArray.getJSONObject(jsonArray.length() - 1)
            return lastEntry.getString("currentHash")
        } catch (e: Exception) {
            return "GENESIS"
        }
    }

    private fun appendEntry(entry: AuditEntry) {
        val jsonArray = if (auditFile.exists()) {
            org.json.JSONArray(auditFile.readText())
        } else {
            org.json.JSONArray()
        }

        val entryJson = JSONObject().apply {
            put("id", entry.id)
            put("timestamp", entry.timestamp)
            put("action", entry.action)
            put("itemId", entry.itemId)
            put("actor", entry.actor)
            put("result", entry.result)
            put("details", entry.details)
            put("previousHash", entry.previousHash)
            put("currentHash", entry.currentHash)
        }

        jsonArray.put(entryJson)
        auditFile.writeText(jsonArray.toString(2))
    }

    private fun computeHash(data: String): String {
        val digest = MessageDigest.getInstance(hashAlgorithm)
        val hashBytes = digest.digest(data.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    data class VerificationResult(
        val isValid: Boolean,
        val message: String
    )

    data class AuditStatistics(
        val totalEntries: Int,
        val actionCounts: Map<String, Int>,
        val successCount: Int,
        val failureCount: Int,
        val firstEntry: Long?,
        val lastEntry: Long?
    )
}
