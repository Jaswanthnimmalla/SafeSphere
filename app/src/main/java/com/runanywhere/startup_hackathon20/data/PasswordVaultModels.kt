package com.runanywhere.startup_hackathon20.data

import java.util.UUID

/**
 * Password Vault Models - Local password manager data structures
 *
 * SafeSphere Password Manager stores credentials locally with AES-256-GCM encryption
 * No cloud sync, no Google Password Manager integration
 */

/**
 * Encrypted password entry stored in vault
 */
data class PasswordVaultEntry(
    val id: String = UUID.randomUUID().toString(),
    val service: String, // App/website name (e.g., "Gmail", "Facebook")
    val username: String, // Email or username
    val encryptedPassword: String, // AES-256-GCM encrypted password
    val url: String = "", // Optional website URL
    val notes: String = "", // Optional encrypted notes
    val category: PasswordCategory = PasswordCategory.WEB,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null,
    val signature: String = "", // RSA signature for integrity
    val iconUrl: String = "", // Service icon URL
    val isFavorite: Boolean = false,
    val strengthScore: Int = 0 // 0-100 password strength
)

/**
 * Decrypted password for display/use (in memory only)
 */
data class DecryptedPassword(
    val id: String,
    val service: String,
    val username: String,
    val password: String, // Plaintext password (use carefully)
    val url: String,
    val notes: String,
    val category: PasswordCategory,
    val createdAt: Long,
    val modifiedAt: Long,
    val lastUsedAt: Long?,
    val strengthScore: Int
)

/**
 * Password categories
 */
enum class PasswordCategory(val displayName: String, val icon: String) {
    WEB("Web Services", "🌐"),
    APP("Mobile Apps", "📱"),
    EMAIL("Email Accounts", "📧"),
    SOCIAL("Social Media", "👥"),
    BANKING("Banking & Finance", "🏦"),
    SHOPPING("E-Commerce", "🛒"),
    WORK("Work & Professional", "💼"),
    ENTERTAINMENT("Entertainment", "🎮"),
    OTHER("Other", "🔑")
}

/**
 * Password generation configuration
 */
data class PasswordGeneratorConfig(
    val length: Int = 16,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true,
    val excludeSimilar: Boolean = true, // Exclude similar chars like 0/O, 1/l
    val excludeAmbiguous: Boolean = true // Exclude ambiguous symbols
)

/**
 * Password strength analysis result
 */
data class PasswordStrength(
    val score: Int, // 0-100
    val level: PasswordStrengthLevel,
    val suggestions: List<String>,
    val crackTimeEstimate: String,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasNumbers: Boolean,
    val hasSymbols: Boolean,
    val length: Int,
    val isCommon: Boolean
)

enum class PasswordStrengthLevel(val displayName: String, val color: String) {
    VERY_WEAK("Very Weak", "#F44336"),
    WEAK("Weak", "#FF9800"),
    FAIR("Fair", "#FFC107"),
    GOOD("Good", "#8BC34A"),
    STRONG("Strong", "#4CAF50"),
    VERY_STRONG("Very Strong", "#2196F3")
}

/**
 * Autofill request data
 */
data class AutofillRequest(
    val packageName: String,
    val domain: String,
    val fieldIds: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Password vault statistics
 */
data class PasswordVaultStats(
    val totalPasswords: Int,
    val strongPasswords: Int,
    val weakPasswords: Int,
    val duplicatePasswords: Int,
    val oldPasswords: Int, // Not changed in 90+ days
    val categoryBreakdown: Map<PasswordCategory, Int>,
    val securityScore: Int, // 0-100
    val lastBackup: Long?
)

/**
 * Password save prompt data
 */
data class PasswordSavePrompt(
    val id: String = UUID.randomUUID().toString(),
    val service: String,
    val username: String,
    val password: String,
    val detectedFrom: String, // Package name or URL
    val timestamp: Long = System.currentTimeMillis(),
    val autoDetected: Boolean = true
)

/**
 * Password history entry (for tracking changes)
 */
data class PasswordHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val passwordId: String, // Reference to PasswordVaultEntry
    val encryptedOldPassword: String,
    val changedAt: Long = System.currentTimeMillis(),
    val reason: String = "" // Optional reason for change
)

/**
 * Password breach check result (offline check against common passwords)
 */
data class PasswordBreachInfo(
    val isBreached: Boolean,
    val timesFound: Int = 0,
    val recommendation: String
)

/**
 * Password export data (encrypted)
 */
data class PasswordExportData(
    val exportId: String = UUID.randomUUID().toString(),
    val exportDate: Long = System.currentTimeMillis(),
    val encryptedData: String, // All passwords encrypted with user-provided passphrase
    val passwordCount: Int,
    val checksum: String // SHA-256 checksum for integrity
)
