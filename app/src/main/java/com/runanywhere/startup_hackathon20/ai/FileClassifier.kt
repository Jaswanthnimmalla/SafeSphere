package com.runanywhere.startup_hackathon20.ai

import android.content.Context
import com.runanywhere.startup_hackathon20.data.PrivacyVaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FileClassifier - AI-driven automatic file classification
 *
 * Features:
 * - Automatic content sensitivity detection
 * - Multi-category classification
 * - Confidence scoring
 * - Privacy risk assessment
 */
class FileClassifier(private val context: Context) {

    enum class SensitivityLevel {
        PUBLIC,        // Safe to share publicly
        INTERNAL,      // Organization internal only
        CONFIDENTIAL,  // Restricted access
        SECRET,        // Highly sensitive
        TOP_SECRET     // Maximum security
    }

    data class ClassificationResult(
        val category: String,
        val sensitivityLevel: SensitivityLevel,
        val confidence: Float, // 0.0 to 1.0
        val riskScore: Int,     // 0 to 100
        val reasons: List<String>,
        val recommendedActions: List<String>
    )

    /**
     * Classify content and assign sensitivity level
     */
    suspend fun classifyContent(content: String, fileName: String): ClassificationResult =
        withContext(Dispatchers.Default) {
            val keywords = extractKeywords(content)
            val patterns = detectPatterns(content)

            val sensitivityLevel = determineSensitivity(keywords, patterns, fileName)
            val category = determineCategory(keywords, fileName)
            val riskScore = calculateRiskScore(sensitivityLevel, patterns)
            val confidence = calculateConfidence(keywords, patterns)
            val reasons = generateReasons(keywords, patterns, fileName)
            val actions = generateRecommendations(sensitivityLevel, category)

            ClassificationResult(
                category = category,
                sensitivityLevel = sensitivityLevel,
                confidence = confidence,
                riskScore = riskScore,
                reasons = reasons,
                recommendedActions = actions
            )
        }

    /**
     * Classify a vault item (requires decrypted content)
     */
    suspend fun classifyItem(decryptedContent: String, fileName: String): ClassificationResult =
        withContext(Dispatchers.Default) {
            classifyContent(decryptedContent, fileName)
        }

    /**
     * Batch classify multiple items
     */
    suspend fun classifyBatch(itemsWithContent: List<Pair<String, String>>): Map<Int, ClassificationResult> =
        withContext(Dispatchers.Default) {
            itemsWithContent.mapIndexed { index, (fileName, content) ->
                index to classifyContent(content, fileName)
            }.toMap()
        }

    /**
     * Get sensitivity score (0-100) from decrypted content
     */
    fun getSensitivityScore(content: String): Int {
        val lowerContent = content.lowercase()
        var score = 0

        // Check for sensitive keywords
        if (lowerContent.contains("password") || lowerContent.contains("secret")) score += 30
        if (lowerContent.contains("confidential") || lowerContent.contains("private")) score += 25
        if (lowerContent.contains("ssn") || lowerContent.contains("credit card")) score += 40
        if (lowerContent.contains("bank") || lowerContent.contains("account")) score += 20
        if (lowerContent.contains("medical") || lowerContent.contains("health")) score += 25

        return minOf(100, score)
    }

    /**
     * Automatically identify sensitive items that need encryption
     */
    suspend fun identifySensitiveItems(itemsWithContent: List<Pair<String, String>>): List<Int> =
        withContext(Dispatchers.Default) {
            itemsWithContent.mapIndexedNotNull { index, (fileName, content) ->
                val result = classifyContent(content, fileName)
                if (result.sensitivityLevel in listOf(
                        SensitivityLevel.CONFIDENTIAL,
                        SensitivityLevel.SECRET,
                        SensitivityLevel.TOP_SECRET
                    )
                ) index else null
            }
        }

    /**
     * Private classification methods
     */

    private fun extractKeywords(content: String): Set<String> {
        val lowercaseContent = content.lowercase()
        val sensitiveKeywords = setOf(
            "password", "secret", "confidential", "private", "ssn", "social security",
            "credit card", "bank account", "medical", "health", "passport", "license",
            "api key", "token", "credentials", "pin", "secure", "encrypted",
            "financial", "salary", "income", "tax", "insurance", "legal"
        )

        return sensitiveKeywords.filter { lowercaseContent.contains(it) }.toSet()
    }

    private fun detectPatterns(content: String): Map<String, Boolean> {
        return mapOf(
            "credit_card" to Regex("""\b\d{4}[-\s]?\d{4}[-\s]?\d{4}[-\s]?\d{4}\b""").containsMatchIn(
                content
            ),
            "ssn" to Regex("""\b\d{3}-\d{2}-\d{4}\b""").containsMatchIn(content),
            "email" to Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b""").containsMatchIn(
                content
            ),
            "phone" to Regex("""\b\d{3}[-.]?\d{3}[-.]?\d{4}\b""").containsMatchIn(content),
            "ip_address" to Regex("""\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b""").containsMatchIn(
                content
            ),
            "url" to Regex("""https?://[^\s]+""").containsMatchIn(content)
        )
    }

    private fun determineSensitivity(
        keywords: Set<String>,
        patterns: Map<String, Boolean>,
        fileName: String
    ): SensitivityLevel {
        var score = 0

        // High sensitivity keywords
        val topSecretKeywords = setOf("password", "secret", "api key", "token", "credentials")
        if (keywords.any { it in topSecretKeywords }) score += 40

        // Moderate sensitivity keywords
        val confidentialKeywords = setOf("confidential", "private", "secure", "encrypted")
        if (keywords.any { it in confidentialKeywords }) score += 25

        // Pattern detection
        if (patterns["credit_card"] == true) score += 35
        if (patterns["ssn"] == true) score += 40
        if (patterns["email"] == true) score += 10

        return when {
            score >= 60 -> SensitivityLevel.TOP_SECRET
            score >= 40 -> SensitivityLevel.SECRET
            score >= 25 -> SensitivityLevel.CONFIDENTIAL
            score >= 10 -> SensitivityLevel.INTERNAL
            else -> SensitivityLevel.PUBLIC
        }
    }

    private fun determineCategory(keywords: Set<String>, fileName: String): String {
        val lowerFileName = fileName.lowercase()

        return when {
            keywords.any { it in setOf("password", "credentials", "token", "api key") }
                -> "Credentials"

            keywords.any { it in setOf("financial", "bank", "credit card", "salary", "income") }
                -> "Financial"

            keywords.any { it in setOf("medical", "health", "insurance") }
                -> "Medical"

            keywords.any { it in setOf("legal", "contract", "agreement") }
                -> "Legal"

            lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".png")
                -> "Media"

            lowerFileName.endsWith(".doc") || lowerFileName.endsWith(".pdf")
                -> "Documents"

            else -> "Personal"
        }
    }

    private fun calculateRiskScore(
        sensitivityLevel: SensitivityLevel,
        patterns: Map<String, Boolean>
    ): Int {
        var risk = when (sensitivityLevel) {
            SensitivityLevel.TOP_SECRET -> 90
            SensitivityLevel.SECRET -> 70
            SensitivityLevel.CONFIDENTIAL -> 50
            SensitivityLevel.INTERNAL -> 30
            SensitivityLevel.PUBLIC -> 10
        }

        // Increase risk if PII patterns detected
        if (patterns["credit_card"] == true) risk = minOf(100, risk + 10)
        if (patterns["ssn"] == true) risk = minOf(100, risk + 10)

        return risk
    }

    private fun calculateConfidence(
        keywords: Set<String>,
        patterns: Map<String, Boolean>
    ): Float {
        val keywordConfidence = (keywords.size * 0.15f).coerceAtMost(0.7f)
        val patternConfidence = (patterns.values.count { it } * 0.15f).coerceAtMost(0.3f)

        return (keywordConfidence + patternConfidence).coerceIn(0f, 1f)
    }

    private fun generateReasons(
        keywords: Set<String>,
        patterns: Map<String, Boolean>,
        fileName: String
    ): List<String> {
        val reasons = mutableListOf<String>()

        if (keywords.isNotEmpty()) {
            reasons.add("Contains sensitive keywords: ${keywords.take(3).joinToString()}")
        }

        patterns.filter { it.value }.forEach { (pattern, _) ->
            reasons.add("Detected pattern: ${pattern.replace("_", " ")}")
        }

        if (reasons.isEmpty()) {
            reasons.add("No sensitive content detected")
        }

        return reasons
    }

    private fun generateRecommendations(
        sensitivityLevel: SensitivityLevel,
        category: String
    ): List<String> {
        val recommendations = mutableListOf<String>()

        when (sensitivityLevel) {
            SensitivityLevel.TOP_SECRET, SensitivityLevel.SECRET -> {
                recommendations.add("🔒 Enable maximum encryption")
                recommendations.add("⚠️ Restrict access to authorized personnel only")
                recommendations.add("📅 Schedule regular security audits")
            }

            SensitivityLevel.CONFIDENTIAL -> {
                recommendations.add("🔐 Enable encryption")
                recommendations.add("👁️ Monitor access patterns")
            }

            SensitivityLevel.INTERNAL -> {
                recommendations.add("🔑 Use standard encryption")
                recommendations.add("📊 Track usage statistics")
            }

            else -> {
                recommendations.add("✅ Current security level is appropriate")
            }
        }

        if (category == "Financial") {
            recommendations.add("💳 Consider using financial data vault")
        }
        if (category == "Medical") {
            recommendations.add("🏥 Ensure HIPAA compliance")
        }

        return recommendations
    }
}
