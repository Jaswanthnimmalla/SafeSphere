package com.runanywhere.startup_hackathon20.data

import android.graphics.Bitmap
import java.io.Serializable

/**
 * Screenshot Guardian - Data Models
 *
 * Represents the screenshot analysis and protection system
 */

/**
 * Screenshot analysis result
 */
data class ScreenshotAnalysis(
    val id: String = System.currentTimeMillis().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val screenshotPath: String,
    val threatLevel: ThreatLevel,
    val detectedSensitiveInfo: List<SensitiveInfoDetection>,
    val confidence: Float, // 0.0 to 1.0
    val action: ProtectionAction = ProtectionAction.NONE,
    val isProtected: Boolean = false,
    val analysisTimeMs: Long = 0
) : Serializable

/**
 * Types of sensitive information that can be detected
 */
enum class SensitiveInfoType(val displayName: String, val icon: String, val risk: String) {
    PASSWORD("Password", "🔐", "CRITICAL"),
    CREDIT_CARD("Credit Card", "💳", "CRITICAL"),
    BANK_ACCOUNT("Bank Account", "🏦", "CRITICAL"),
    SSN("Social Security", "🆔", "CRITICAL"),
    PHONE_NUMBER("Phone Number", "📞", "MEDIUM"),
    EMAIL("Email Address", "📧", "MEDIUM"),
    ADDRESS("Physical Address", "🏠", "MEDIUM"),
    PERSONAL_ID("Personal ID", "🪪", "HIGH"),
    API_KEY("API Key/Token", "🔑", "HIGH"),
    PRIVATE_MESSAGE("Private Message", "💬", "MEDIUM"),
    DATE_OF_BIRTH("Date of Birth", "📅", "MEDIUM"),
    OTHER("Other Sensitive", "⚠️", "LOW")
}

/**
 * Individual detection of sensitive information
 */
data class SensitiveInfoDetection(
    val type: SensitiveInfoType,
    val content: String, // Redacted or partial content
    val confidence: Float,
    val boundingBox: DetectionBox? = null
) : Serializable

/**
 * Bounding box for detected sensitive info (for blur feature)
 */
data class DetectionBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) : Serializable

/**
 * Threat level based on analysis
 */
enum class ThreatLevel(val displayName: String, val color: Long, val icon: String) {
    SAFE("Safe", 0xFF4CAF50, "✅"),
    LOW("Low Risk", 0xFFFBC02D, "⚠️"),
    MEDIUM("Medium Risk", 0xFFFF9800, "⚠️"),
    HIGH("High Risk", 0xFFFF5722, "🚨"),
    CRITICAL("Critical Risk", 0xFFD32F2F, "🚨")
}

/**
 * Protection actions available to user
 */
enum class ProtectionAction(val displayName: String, val icon: String) {
    NONE("No Action", "✓"),
    BLUR("Blur Sensitive", "🌫️"),
    DELETE("Delete Screenshot", "🗑️"),
    ENCRYPT_VAULT("Move to Vault", "🔐"),
    KEEP_ANYWAY("Keep Original", "⚠️")
}

/**
 * Screenshot Guardian statistics
 */
data class GuardianStats(
    val totalScreenshots: Int = 0,
    val threatsDetected: Int = 0,
    val threatsBlocked: Int = 0,
    val passwordsDetected: Int = 0,
    val creditCardsDetected: Int = 0,
    val personalInfoDetected: Int = 0,
    val screenshotsProtected: Int = 0,
    val lastScanTime: Long = 0,
    val avgAnalysisTimeMs: Long = 0
) : Serializable

/**
 * Protected screenshot record
 */
data class ProtectedScreenshot(
    val id: String,
    val originalPath: String,
    val protectedPath: String?,
    val analysis: ScreenshotAnalysis,
    val timestamp: Long,
    val action: ProtectionAction
) : Serializable

/**
 * Guardian settings
 */
data class GuardianSettings(
    val isEnabled: Boolean = false,
    val autoAnalyze: Boolean = true,
    val autoBlur: Boolean = false,
    val autoDelete: Boolean = false,
    val autoVault: Boolean = false,
    val showNotifications: Boolean = true,
    val minConfidence: Float = 0.7f,
    val excludedApps: List<String> = emptyList()
) : Serializable
