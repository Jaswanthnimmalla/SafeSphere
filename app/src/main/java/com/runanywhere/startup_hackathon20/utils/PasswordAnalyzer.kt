package com.runanywhere.startup_hackathon20.utils

import com.runanywhere.startup_hackathon20.data.PrivacyVaultItem
import com.runanywhere.startup_hackathon20.data.VaultCategory
import java.util.concurrent.TimeUnit
import kotlin.collections.map
import kotlin.math.log2

/**
 * Password Health Analyzer
 * Analyzes password strength, duplicates, common passwords, and age
 */
object PasswordAnalyzer {

    /**
     * Password Strength Levels
     */
    enum class PasswordStrength(val score: Int, val label: String, val color: Long) {
        VERY_WEAK(0, "Very Weak", 0xFFD32F2F),
        WEAK(25, "Weak", 0xFFF57C00),
        MEDIUM(50, "Medium", 0xFFFBC02D),
        STRONG(75, "Strong", 0xFF7CB342),
        VERY_STRONG(100, "Very Strong", 0xFF388E3C)
    }

    /**
     * Password Health Report
     */
    data class HealthReport(
        val overallScore: Int,
        val totalPasswords: Int,
        val weakPasswords: Int,
        val duplicatePasswords: Int,
        val commonPasswords: Int,
        val oldPasswords: Int,
        val passwordDetails: List<PasswordDetail>
    )

    /**
     * Individual Password Detail
     */
    data class PasswordDetail(
        val itemId: String,
        val title: String,
        val strength: PasswordStrength,
        val isDuplicate: Boolean,
        val isCommon: Boolean,
        val ageInDays: Int,
        val issues: List<String>,
        val suggestions: List<String>,
        val breachResult: BreachDetector.BreachResult? = null
    )

    /**
     * Analyze all vault items and generate health report
     */
    fun analyzePasswords(
        vaultItems: List<PrivacyVaultItem>,
        decryptPassword: (String) -> String
    ): HealthReport {
        if (vaultItems.isEmpty()) {
            return HealthReport(100, 0, 0, 0, 0, 0, emptyList())
        }

        // Only analyze password category items
        val passwordItems = vaultItems.filter { it.category == VaultCategory.PASSWORDS }

        if (passwordItems.isEmpty()) {
            return HealthReport(100, 0, 0, 0, 0, 0, emptyList())
        }

        // Decrypt all passwords
        val decryptedPasswords = passwordItems.map { item ->
            item to decryptPassword(item.encryptedContent)
        }

        // Find duplicates
        val passwordGroups = decryptedPasswords.groupBy { it.second }
        val duplicatePasswords = passwordGroups.filter { it.value.size > 1 }.keys

        // Analyze each password
        val passwordDetails = decryptedPasswords.map { (item, password) ->
            analyzePassword(item, password, duplicatePasswords.contains(password))
        }

        // Calculate statistics
        val weakCount = passwordDetails.count {
            it.strength == PasswordStrength.VERY_WEAK || it.strength == PasswordStrength.WEAK
        }
        val duplicateCount = passwordDetails.count { it.isDuplicate }
        val commonCount = passwordDetails.count { it.isCommon }
        val oldCount = passwordDetails.count { it.ageInDays > 365 }

        // Calculate overall score
        val avgStrength = passwordDetails.map { it.strength.score }.average()
        val duplicatePenalty = (duplicateCount * 5).coerceAtMost(20)
        val commonPenalty = (commonCount * 10).coerceAtMost(30)
        val oldPenalty = (oldCount * 3).coerceAtMost(15)

        val overallScore = (avgStrength - duplicatePenalty - commonPenalty - oldPenalty)
            .toInt()
            .coerceIn(0, 100)

        return HealthReport(
            overallScore = overallScore,
            totalPasswords = passwordItems.size,
            weakPasswords = weakCount,
            duplicatePasswords = duplicateCount,
            commonPasswords = commonCount,
            oldPasswords = oldCount,
            passwordDetails = passwordDetails
        )
    }

    /**
     * Analyze individual password
     */
    private fun analyzePassword(
        item: PrivacyVaultItem,
        password: String,
        isDuplicate: Boolean
    ): PasswordDetail {
        val strength = calculatePasswordStrength(password)
        val isCommon = isCommonPassword(password)
        val ageInDays = calculatePasswordAge(item.createdAt)
        val issues = mutableListOf<String>()
        val suggestions = mutableListOf<String>()

        // Identify issues
        if (password.length < 8) {
            issues.add("Too short (${password.length} characters)")
            suggestions.add("Use at least 12 characters")
        }
        if (!password.any { it.isUpperCase() }) {
            issues.add("No uppercase letters")
            suggestions.add("Add uppercase letters (A-Z)")
        }
        if (!password.any { it.isLowerCase() }) {
            issues.add("No lowercase letters")
            suggestions.add("Add lowercase letters (a-z)")
        }
        if (!password.any { it.isDigit() }) {
            issues.add("No numbers")
            suggestions.add("Add numbers (0-9)")
        }
        if (!password.any { !it.isLetterOrDigit() }) {
            issues.add("No special characters")
            suggestions.add("Add special characters (!@#$%)")
        }
        if (isDuplicate) {
            issues.add("Used on multiple sites")
            suggestions.add("Use unique password for each site")
        }
        if (isCommon) {
            issues.add("Common password (easily guessed)")
            suggestions.add("Use a randomly generated password")
        }
        if (ageInDays > 365) {
            issues.add("Password is ${ageInDays / 365} year(s) old")
            suggestions.add("Change password regularly (every 6-12 months)")
        }

        return PasswordDetail(
            itemId = item.id,
            title = item.title,
            strength = strength,
            isDuplicate = isDuplicate,
            isCommon = isCommon,
            ageInDays = ageInDays,
            issues = issues,
            suggestions = suggestions,
            breachResult = BreachDetector.checkBreach(password) // NEW: Breach detection
        )
    }

    /**
     * Calculate password strength
     */
    fun calculatePasswordStrength(password: String): PasswordStrength {
        var score = 0

        // Length score
        when {
            password.length >= 16 -> score += 30
            password.length >= 12 -> score += 25
            password.length >= 8 -> score += 15
            else -> score += 5
        }

        // Character variety
        if (password.any { it.isUpperCase() }) score += 15
        if (password.any { it.isLowerCase() }) score += 15
        if (password.any { it.isDigit() }) score += 15
        if (password.any { !it.isLetterOrDigit() }) score += 20

        // Entropy (randomness)
        val entropy = calculateEntropy(password)
        score += (entropy / 3).toInt().coerceAtMost(25)

        // Penalize common patterns
        if (hasCommonPatterns(password)) score -= 20

        return when {
            score >= 85 -> PasswordStrength.VERY_STRONG
            score >= 65 -> PasswordStrength.STRONG
            score >= 45 -> PasswordStrength.MEDIUM
            score >= 25 -> PasswordStrength.WEAK
            else -> PasswordStrength.VERY_WEAK
        }
    }

    /**
     * Calculate password entropy (randomness)
     */
    private fun calculateEntropy(password: String): Double {
        val charSetSize = when {
            password.any { !it.isLetterOrDigit() } -> 94 // All printable ASCII
            password.any { it.isDigit() } && password.any { it.isLetter() } -> 62 // Alphanumeric
            password.any { it.isLetter() } -> 52 // Letters only
            else -> 10 // Numbers only
        }
        return password.length * log2(charSetSize.toDouble())
    }

    /**
     * Check for common patterns
     */
    private fun hasCommonPatterns(password: String): Boolean {
        val commonPatterns = listOf(
            "123", "abc", "qwerty", "password", "admin",
            "111", "000", "aaa", "zzz"
        )
        return commonPatterns.any { password.lowercase().contains(it) }
    }

    /**
     * Check if password is in common passwords list
     */
    fun isCommonPassword(password: String): Boolean {
        return COMMON_PASSWORDS.contains(password.lowercase())
    }

    /**
     * Calculate password age in days
     */
    private fun calculatePasswordAge(createdAt: Long): Int {
        val currentTime = System.currentTimeMillis()
        val ageMillis = currentTime - createdAt
        return TimeUnit.MILLISECONDS.toDays(ageMillis).toInt()
    }

    /**
     * Generate strong password
     */
    fun generateStrongPassword(length: Int = 16): String {
        val uppercase = ('A'..'Z')
        val lowercase = ('a'..'z')
        val digits = ('0'..'9')
        val special = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        val allChars = uppercase + lowercase + digits + special.toList()

        val password = StringBuilder()

        // Ensure at least one of each type
        password.append(uppercase.random())
        password.append(lowercase.random())
        password.append(digits.random())
        password.append(special.random())

        // Fill the rest randomly
        repeat(length - 4) {
            password.append(allChars.random())
        }

        // Shuffle the password
        return password.toList().shuffled().joinToString("")
    }

    /**
     * Top 100 most common passwords (offline database)
     * In production, this would be a larger list (10,000+)
     */
    private val COMMON_PASSWORDS = setOf(
        "123456", "password", "123456789", "12345678", "12345",
        "1234567", "password1", "123123", "1234567890", "000000",
        "abc123", "password123", "qwerty", "111111", "iloveyou",
        "monkey", "dragon", "master", "666666", "123321",
        "654321", "sunshine", "welcome", "login", "admin",
        "princess", "solo", "qwerty123", "starwars", "letmein",
        "trustno1", "1234", "football", "baseball", "whatever",
        "freedom", "ninja", "mustang", "michael", "jordan",
        "superman", "batman", "hello", "shadow", "696969",
        "qwertyuiop", "123qwe", "1q2w3e4r", "charlie", "donald",
        "ashley", "bailey", "passw0rd", "flower", "hottie",
        "loveme", "zaq1zaq1", "password1", "fuckyou", "soccer",
        "aa123456", "internet", "test", "test123", "computer",
        "secret", "1qaz2wsx", "access", "killer", "1q2w3e",
        "Password", "password!", "P@ssw0rd", "master123", "azerty",
        "mypass", "mypassword", "pass", "pass123", "temp",
        "temp123", "1234qwer", "qwer1234", "default", "admin123",
        "root", "toor", "pass1234", "q1w2e3r4", "1qazxsw2",
        "zxcvbnm", "asdfghjkl", "qazwsx", "121212", "000000",
        "1q2w3e4r5t", "password12", "mynoob", "123abc", "password2"
    )
}
