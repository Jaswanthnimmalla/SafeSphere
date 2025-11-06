package com.runanywhere.startup_hackathon20.data

import java.util.UUID

/**
 * PrivacyVaultItem - Represents encrypted data stored in the Privacy Vault
 *
 * Each item contains:
 * - Unique ID for identification
 * - Title for display
 * - Encrypted content (the actual sensitive data)
 * - Category for organization
 * - Metadata like creation/modification timestamps
 * - Signature for integrity verification
 */
data class PrivacyVaultItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val encryptedContent: String, // Base64-encoded encrypted data
    val category: VaultCategory,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val signature: String = "", // RSA signature for integrity
    val isEncrypted: Boolean = true,
    val size: Int = 0 // Size in bytes
)

/**
 * Vault categories for organizing encrypted data
 */
enum class VaultCategory(val displayName: String, val icon: String) {
    PERSONAL("Personal Info", "👤"),
    FINANCIAL("Financial", "💳"),
    PASSWORDS("Passwords", "🔑"),
    DOCUMENTS("Documents", "📄"),
    MEDICAL("Medical", "🏥"),
    NOTES("Secure Notes", "📝"),
    OTHER("Other", "📦")
}

/**
 * Decrypted vault item for display/editing
 * Used temporarily in memory, never persisted
 */
data class DecryptedVaultItem(
    val id: String,
    val title: String,
    val content: String, // Plaintext content
    val category: VaultCategory,
    val createdAt: Long,
    val modifiedAt: Long
)

/**
 * Storage statistics for Data Map visualization
 */
data class StorageStats(
    val totalItems: Int,
    val encryptedItems: Int,
    val totalSize: Long, // in bytes
    val categoryBreakdown: Map<VaultCategory, Int>,
    val securityScore: Int // 0-100
)

/**
 * Threat event for threat simulation
 */
data class ThreatEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: ThreatType,
    val severity: ThreatSeverity,
    val description: String,
    val mitigated: Boolean = false
)

enum class ThreatType(val displayName: String) {
    DATA_BREACH("Data Breach Attempt"),
    UNAUTHORIZED_ACCESS("Unauthorized Access"),
    MAN_IN_MIDDLE("Man-in-the-Middle Attack"),
    CLOUD_EXPOSURE("Cloud Data Exposure"),
    MALWARE("Malware Detection"),
    PHISHING("Phishing Attempt")
}

enum class ThreatSeverity(val displayName: String, val color: String) {
    LOW("Low", "#4CAF50"),
    MEDIUM("Medium", "#FF9800"),
    HIGH("High", "#F44336"),
    CRITICAL("Critical", "#9C27B0")
}
