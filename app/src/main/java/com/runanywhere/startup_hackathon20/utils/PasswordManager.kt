package com.runanywhere.startup_hackathon20.utils

import com.runanywhere.startup_hackathon20.data.*
import java.security.SecureRandom
import kotlin.math.ln
import kotlin.math.pow

/**
 * PasswordManager - Core password management utilities
 *
 * Handles:
 * - Strong password generation
 * - Password strength analysis
 * - Common password detection (offline breach check)
 * - Password security scoring
 */
object PasswordManager {

    private val secureRandom = SecureRandom()

    // Character sets for password generation
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val NUMBERS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    private const val SIMILAR_CHARS = "il1Lo0O"
    private const val AMBIGUOUS_SYMBOLS = "{}[]()/\\'\"`~,;:.<>"

    // Common passwords for breach checking (subset for demonstration)
    private val commonPasswords = setOf(
        "password", "123456", "12345678", "qwerty", "abc123", "monkey",
        "1234567", "letmein", "trustno1", "dragon", "baseball", "iloveyou",
        "master", "sunshine", "ashley", "bailey", "passw0rd", "shadow",
        "123123", "654321", "superman", "qazwsx", "michael", "football",
        "password1", "welcome", "admin", "Password1", "Password123"
    )

    /**
     * Generate a strong password based on configuration
     */
    fun generatePassword(config: PasswordGeneratorConfig = PasswordGeneratorConfig()): String {
        var charPool = ""

        if (config.includeUppercase) charPool += UPPERCASE
        if (config.includeLowercase) charPool += LOWERCASE
        if (config.includeNumbers) charPool += NUMBERS
        if (config.includeSymbols) {
            charPool += if (config.excludeAmbiguous) {
                SYMBOLS.filterNot { it in AMBIGUOUS_SYMBOLS }
            } else {
                SYMBOLS
            }
        }

        if (config.excludeSimilar) {
            charPool = charPool.filterNot { it in SIMILAR_CHARS }
        }

        if (charPool.isEmpty()) {
            charPool = UPPERCASE + LOWERCASE + NUMBERS // Fallback
        }

        // Generate password ensuring at least one char from each enabled set
        val password = StringBuilder()

        // Add one character from each required set
        if (config.includeUppercase) {
            password.append(UPPERCASE[secureRandom.nextInt(UPPERCASE.length)])
        }
        if (config.includeLowercase) {
            password.append(LOWERCASE[secureRandom.nextInt(LOWERCASE.length)])
        }
        if (config.includeNumbers) {
            password.append(NUMBERS[secureRandom.nextInt(NUMBERS.length)])
        }
        if (config.includeSymbols) {
            val symbolSet = if (config.excludeAmbiguous) {
                SYMBOLS.filterNot { it in AMBIGUOUS_SYMBOLS }
            } else {
                SYMBOLS
            }
            password.append(symbolSet[secureRandom.nextInt(symbolSet.length)])
        }

        // Fill remaining length with random characters
        repeat(config.length - password.length) {
            password.append(charPool[secureRandom.nextInt(charPool.length)])
        }

        // Shuffle to avoid predictable patterns
        return password.toString().toList().shuffled(secureRandom).joinToString("")
    }

    /**
     * Analyze password strength comprehensively
     */
    fun analyzePasswordStrength(password: String): PasswordStrength {
        val length = password.length
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasNumbers = password.any { it.isDigit() }
        val hasSymbols = password.any { !it.isLetterOrDigit() }
        val isCommon = password.lowercase() in commonPasswords

        // Calculate entropy
        var charSetSize = 0
        if (hasUppercase) charSetSize += 26
        if (hasLowercase) charSetSize += 26
        if (hasNumbers) charSetSize += 10
        if (hasSymbols) charSetSize += 32

        val entropy = if (charSetSize > 0) {
            length * (ln(charSetSize.toDouble()) / ln(2.0))
        } else {
            0.0
        }

        // Calculate base score (0-100)
        var score = when {
            length < 6 -> 0
            length < 8 -> 20
            length < 10 -> 40
            length < 12 -> 60
            length < 16 -> 80
            else -> 90
        }

        // Adjust score based on character variety
        val varietyBonus =
            listOf(hasUppercase, hasLowercase, hasNumbers, hasSymbols).count { it } * 5
        score += varietyBonus

        // Penalize common passwords
        if (isCommon) {
            score = (score * 0.3).toInt()
        }

        // Check for patterns
        if (hasSequentialChars(password)) score -= 10
        if (hasRepeatingChars(password)) score -= 10

        score = score.coerceIn(0, 100)

        // Determine strength level
        val level = when (score) {
            in 0..20 -> PasswordStrengthLevel.VERY_WEAK
            in 21..40 -> PasswordStrengthLevel.WEAK
            in 41..60 -> PasswordStrengthLevel.FAIR
            in 61..75 -> PasswordStrengthLevel.GOOD
            in 76..90 -> PasswordStrengthLevel.STRONG
            else -> PasswordStrengthLevel.VERY_STRONG
        }

        // Generate suggestions
        val suggestions = mutableListOf<String>()
        if (length < 12) suggestions.add("Use at least 12 characters")
        if (!hasUppercase) suggestions.add("Add uppercase letters")
        if (!hasLowercase) suggestions.add("Add lowercase letters")
        if (!hasNumbers) suggestions.add("Add numbers")
        if (!hasSymbols) suggestions.add("Add special symbols")
        if (isCommon) suggestions.add("This is a commonly used password - choose something unique")
        if (hasSequentialChars(password)) suggestions.add("Avoid sequential characters")
        if (hasRepeatingChars(password)) suggestions.add("Avoid repeating characters")

        // Estimate crack time
        val crackTime = estimateCrackTime(entropy)

        return PasswordStrength(
            score = score,
            level = level,
            suggestions = suggestions,
            crackTimeEstimate = crackTime,
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasNumbers = hasNumbers,
            hasSymbols = hasSymbols,
            length = length,
            isCommon = isCommon
        )
    }

    /**
     * Check if password is in breach database (offline common passwords)
     */
    fun checkPasswordBreach(password: String): PasswordBreachInfo {
        val isBreached = password.lowercase() in commonPasswords

        return if (isBreached) {
            PasswordBreachInfo(
                isBreached = true,
                timesFound = 1000, // Approximate
                recommendation = "⚠️ This password appears in common password lists. " +
                        "It's highly vulnerable to dictionary attacks. Generate a new strong password."
            )
        } else {
            PasswordBreachInfo(
                isBreached = false,
                timesFound = 0,
                recommendation = "✅ Password not found in common password lists"
            )
        }
    }

    /**
     * Estimate password crack time based on entropy
     */
    private fun estimateCrackTime(entropy: Double): String {
        // Assume 1 billion guesses per second (modern GPU)
        val guessesPerSecond = 1_000_000_000.0
        val possibleCombinations = 2.0.pow(entropy)
        val secondsToCrack = possibleCombinations / (2 * guessesPerSecond) // Average case

        return when {
            secondsToCrack < 1 -> "Instant"
            secondsToCrack < 60 -> "${secondsToCrack.toInt()} seconds"
            secondsToCrack < 3600 -> "${(secondsToCrack / 60).toInt()} minutes"
            secondsToCrack < 86400 -> "${(secondsToCrack / 3600).toInt()} hours"
            secondsToCrack < 31536000 -> "${(secondsToCrack / 86400).toInt()} days"
            secondsToCrack < 3153600000 -> "${(secondsToCrack / 31536000).toInt()} years"
            else -> "Centuries"
        }
    }

    /**
     * Check for sequential characters (abc, 123, etc.)
     */
    private fun hasSequentialChars(password: String): Boolean {
        if (password.length < 3) return false

        for (i in 0 until password.length - 2) {
            val char1 = password[i].code
            val char2 = password[i + 1].code
            val char3 = password[i + 2].code

            // Check if three consecutive chars are sequential
            if (char2 == char1 + 1 && char3 == char2 + 1) {
                return true
            }
            // Check reverse sequential
            if (char2 == char1 - 1 && char3 == char2 - 1) {
                return true
            }
        }

        return false
    }

    /**
     * Check for repeating characters (aaa, 111, etc.)
     */
    private fun hasRepeatingChars(password: String): Boolean {
        if (password.length < 3) return false

        for (i in 0 until password.length - 2) {
            if (password[i] == password[i + 1] && password[i + 1] == password[i + 2]) {
                return true
            }
        }

        return false
    }

    /**
     * Generate memorable passphrase (diceware-style)
     */
    fun generatePassphrase(wordCount: Int = 5, separator: String = "-"): String {
        val words = listOf(
            "correct", "horse", "battery", "staple", "mountain", "river", "ocean",
            "forest", "sunset", "eagle", "thunder", "crystal", "phoenix", "dragon",
            "wizard", "cipher", "shield", "fortress", "guardian", "shadow", "silver",
            "golden", "mystic", "cosmic", "stellar", "nebula", "quantum", "infinity",
            "enigma", "phoenix", "titan", "atlas", "vortex", "zenith", "apex"
        )

        return (1..wordCount)
            .map { words[secureRandom.nextInt(words.size)] }
            .joinToString(separator)
            .replaceFirstChar { it.uppercase() }
    }

    /**
     * Find duplicate passwords in vault
     */
    fun findDuplicatePasswords(passwords: List<String>): List<String> {
        return passwords.groupBy { it }
            .filter { it.value.size > 1 }
            .keys
            .toList()
    }

    /**
     * Check if password is older than specified days
     */
    fun isPasswordOld(createdAt: Long, thresholdDays: Int = 90): Boolean {
        val ageInDays = (System.currentTimeMillis() - createdAt) / (1000 * 60 * 60 * 24)
        return ageInDays > thresholdDays
    }

    /**
     * Calculate overall vault security score
     */
    fun calculateVaultSecurityScore(stats: PasswordVaultStats): Int {
        if (stats.totalPasswords == 0) return 100

        val strongRatio = stats.strongPasswords.toDouble() / stats.totalPasswords
        val duplicateRatio = stats.duplicatePasswords.toDouble() / stats.totalPasswords
        val oldRatio = stats.oldPasswords.toDouble() / stats.totalPasswords

        var score = 100

        // Penalty for weak passwords
        score -= ((1 - strongRatio) * 40).toInt()

        // Penalty for duplicates
        score -= (duplicateRatio * 30).toInt()

        // Penalty for old passwords
        score -= (oldRatio * 20).toInt()

        return score.coerceIn(0, 100)
    }
}
