package com.runanywhere.startup_hackathon20.security

import kotlin.math.log2
import kotlin.math.pow

/**
 * PasswordHealthAnalyzer - Analyzes password security
 *
 * Features:
 * - Password strength calculation (entropy)
 * - Duplicate detection
 * - Reused password alerts
 * - Common password checking
 * - Pattern detection
 */
class PasswordHealthAnalyzer {

    /**
     * Analyze password strength
     */
    fun analyzePassword(password: String): PasswordAnalysis {
        val strength = calculateStrength(password)
        val entropy = calculateEntropy(password)
        val issues = mutableListOf<PasswordIssue>()

        // Check length
        if (password.length < 8) {
            issues.add(PasswordIssue.TooShort)
        }

        // Check for common passwords
        if (isCommonPassword(password)) {
            issues.add(PasswordIssue.CommonPassword)
        }

        // Check for patterns
        if (hasKeyboardPattern(password)) {
            issues.add(PasswordIssue.KeyboardPattern)
        }

        // Check for sequential characters
        if (hasSequentialChars(password)) {
            issues.add(PasswordIssue.Sequential)
        }

        // Check for date patterns
        if (hasDatePattern(password)) {
            issues.add(PasswordIssue.DatePattern)
        }

        // Check complexity
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        val complexityCount = listOf(hasUpper, hasLower, hasDigit, hasSpecial).count { it }
        if (complexityCount < 3) {
            issues.add(PasswordIssue.LowComplexity)
        }

        return PasswordAnalysis(
            password = password,
            strength = strength,
            entropy = entropy,
            issues = issues,
            score = calculateScore(strength, entropy, issues)
        )
    }

    /**
     * Calculate password strength
     */
    private fun calculateStrength(password: String): PasswordStrength {
        val entropy = calculateEntropy(password)
        return when {
            entropy < 28 -> PasswordStrength.VeryWeak
            entropy < 36 -> PasswordStrength.Weak
            entropy < 60 -> PasswordStrength.Fair
            entropy < 128 -> PasswordStrength.Strong
            else -> PasswordStrength.VeryStrong
        }
    }

    /**
     * Calculate password entropy (bits)
     */
    private fun calculateEntropy(password: String): Double {
        if (password.isEmpty()) return 0.0

        val charsetSize = getCharsetSize(password)
        return password.length * log2(charsetSize.toDouble())
    }

    /**
     * Get character set size
     */
    private fun getCharsetSize(password: String): Int {
        var size = 0
        if (password.any { it.isLowerCase() }) size += 26
        if (password.any { it.isUpperCase() }) size += 26
        if (password.any { it.isDigit() }) size += 10
        if (password.any { !it.isLetterOrDigit() }) size += 32
        return size
    }

    /**
     * Calculate overall score (0-100)
     */
    private fun calculateScore(
        strength: PasswordStrength,
        entropy: Double,
        issues: List<PasswordIssue>
    ): Int {
        var score = when (strength) {
            PasswordStrength.VeryWeak -> 20
            PasswordStrength.Weak -> 40
            PasswordStrength.Fair -> 60
            PasswordStrength.Strong -> 80
            PasswordStrength.VeryStrong -> 100
        }

        // Deduct points for issues
        score -= issues.size * 10

        return score.coerceIn(0, 100)
    }

    /**
     * Check if password is common
     */
    private fun isCommonPassword(password: String): Boolean {
        val commonPasswords = setOf(
            "password", "123456", "12345678", "qwerty", "abc123",
            "monkey", "1234567", "letmein", "trustno1", "dragon",
            "baseball", "111111", "iloveyou", "master", "sunshine",
            "ashley", "bailey", "passw0rd", "shadow", "123123",
            "654321", "superman", "qazwsx", "michael", "football"
        )
        return password.lowercase() in commonPasswords
    }

    /**
     * Check for keyboard patterns (qwerty, asdf, etc.)
     */
    private fun hasKeyboardPattern(password: String): Boolean {
        val patterns = listOf(
            "qwerty", "asdfgh", "zxcvbn", "qwertyuiop",
            "asdfghjkl", "zxcvbnm", "12345", "123456"
        )
        val lower = password.lowercase()
        return patterns.any { lower.contains(it) }
    }

    /**
     * Check for sequential characters (abc, 123, etc.)
     */
    private fun hasSequentialChars(password: String): Boolean {
        for (i in 0 until password.length - 2) {
            val char1 = password[i]
            val char2 = password[i + 1]
            val char3 = password[i + 2]

            if (char2.code == char1.code + 1 && char3.code == char2.code + 1) {
                return true
            }
        }
        return false
    }

    /**
     * Check for date patterns (2023, 1990, etc.)
     */
    private fun hasDatePattern(password: String): Boolean {
        val yearPattern = Regex("(19|20)\\d{2}")
        return yearPattern.containsMatchIn(password)
    }

    /**
     * Find duplicate passwords
     */
    fun findDuplicates(passwords: List<String>): List<DuplicateGroup> {
        return passwords
            .groupBy { it }
            .filter { it.value.size > 1 }
            .map { DuplicateGroup(it.key, it.value.size) }
    }

    /**
     * Find reused passwords
     */
    fun findReused(passwords: List<String>): Int {
        return passwords.groupBy { it }.count { it.value.size > 1 }
    }
}

/**
 * Password analysis result
 */
data class PasswordAnalysis(
    val password: String,
    val strength: PasswordStrength,
    val entropy: Double,
    val issues: List<PasswordIssue>,
    val score: Int
) {
    fun getRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()

        issues.forEach { issue ->
            when (issue) {
                is PasswordIssue.TooShort ->
                    recommendations.add("Use at least 12 characters")

                is PasswordIssue.CommonPassword ->
                    recommendations.add("Avoid common passwords")

                is PasswordIssue.KeyboardPattern ->
                    recommendations.add("Avoid keyboard patterns like 'qwerty'")

                is PasswordIssue.Sequential ->
                    recommendations.add("Avoid sequential characters like 'abc' or '123'")

                is PasswordIssue.DatePattern ->
                    recommendations.add("Avoid using dates in passwords")

                is PasswordIssue.LowComplexity ->
                    recommendations.add("Mix uppercase, lowercase, numbers, and symbols")
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("✅ Strong password! Keep it up!")
        }

        return recommendations
    }
}

/**
 * Password strength levels
 */
sealed class PasswordStrength(val level: String, val color: String) {
    object VeryWeak : PasswordStrength("Very Weak", "#FF0000")
    object Weak : PasswordStrength("Weak", "#FF6B6B")
    object Fair : PasswordStrength("Fair", "#FFA500")
    object Strong : PasswordStrength("Strong", "#90EE90")
    object VeryStrong : PasswordStrength("Very Strong", "#00FF00")
}

/**
 * Password issues
 */
sealed class PasswordIssue(val description: String) {
    object TooShort : PasswordIssue("Password is too short")
    object CommonPassword : PasswordIssue("Common password detected")
    object KeyboardPattern : PasswordIssue("Keyboard pattern detected")
    object Sequential : PasswordIssue("Sequential characters detected")
    object DatePattern : PasswordIssue("Date pattern detected")
    object LowComplexity : PasswordIssue("Low character variety")
}

/**
 * Duplicate password group
 */
data class DuplicateGroup(
    val password: String,
    val count: Int
)

/**
 * Password health report for all vault items
 */
data class PasswordHealthReport(
    val totalPasswords: Int,
    val weakPasswords: Int,
    val reusedPasswords: Int,
    val strongPasswords: Int,
    val averageStrength: Double,
    val overallScore: Int,
    val issues: List<PasswordHealthIssue>
)

/**
 * Health issues found in vault
 */
sealed class PasswordHealthIssue {
    data class WeakPassword(val title: String, val score: Int) : PasswordHealthIssue()
    data class ReusedPassword(val password: String, val count: Int) : PasswordHealthIssue()
    data class CommonPassword(val title: String) : PasswordHealthIssue()
}
