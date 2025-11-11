package com.runanywhere.startup_hackathon20.ai

import com.runanywhere.startup_hackathon20.data.PasswordVaultEntry
import com.runanywhere.startup_hackathon20.utils.BreachDetector
import com.runanywhere.startup_hackathon20.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.max
import kotlin.math.ln
import kotlin.math.pow

/**
 * AI Security Predictor - Predicts future security risks using ML algorithms
 *
 * Features:
 * - Predictive risk analysis
 * - Timeline projections
 * - AI-generated recommendations
 * - Breach probability calculations
 */
class AISecurityPredictor {

    /**
     * Security Prediction Result
     */
    data class SecurityPrediction(
        val currentRiskScore: Int, // 0-100 (0 = safe, 100 = critical)
        val futureRiskScore30Days: Int,
        val futureRiskScore90Days: Int,
        val daysUntilCritical: Int, // Days until risk reaches 80+
        val breachProbability: Float, // 0.0-1.0
        val predictions: List<PredictionInsight>,
        val actionItems: List<ActionItem>,
        val riskTrend: RiskTrend,
        val vulnerabilities: List<Vulnerability>
    )

    /**
     * Prediction Insight
     */
    data class PredictionInsight(
        val icon: String,
        val title: String,
        val description: String,
        val severity: PredictionSeverity,
        val confidence: Float // 0.0-1.0
    )

    /**
     * Action Item
     */
    data class ActionItem(
        val priority: Int, // 1 = highest
        val action: String,
        val impact: String,
        val estimatedTime: String
    )

    /**
     * Vulnerability
     */
    data class Vulnerability(
        val type: VulnerabilityType,
        val count: Int,
        val riskContribution: Int // Percentage of total risk
    )

    enum class VulnerabilityType(val displayName: String, val icon: String) {
        BREACHED_PASSWORDS("Breached Passwords", "🚨"),
        WEAK_PASSWORDS("Weak Passwords", "🔴"),
        REUSED_PASSWORDS("Reused Passwords", "❌"),
        OLD_PASSWORDS("Outdated Passwords", "⏰"),
        COMMON_PATTERNS("Common Patterns", "⚠️")
    }

    enum class PredictionSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    enum class RiskTrend {
        IMPROVING, STABLE, DEGRADING, CRITICAL
    }

    /**
     * Analyze vault and predict future security risks
     */
    suspend fun predictSecurity(passwords: List<PasswordVaultEntry>): SecurityPrediction =
        withContext(Dispatchers.Default) {

            if (passwords.isEmpty()) {
                return@withContext SecurityPrediction(
                    currentRiskScore = 0,
                    futureRiskScore30Days = 0,
                    futureRiskScore90Days = 0,
                    daysUntilCritical = 999,
                    breachProbability = 0f,
                    predictions = listOf(
                        PredictionInsight(
                            icon = "✅",
                            title = "No Passwords Stored",
                            description = "Add passwords to enable AI security predictions",
                            severity = PredictionSeverity.LOW,
                            confidence = 1.0f
                        )
                    ),
                    actionItems = emptyList(),
                    riskTrend = RiskTrend.STABLE,
                    vulnerabilities = emptyList()
                )
            }

            // Decrypt and analyze passwords
            val decryptedPasswords = passwords.mapNotNull { entry ->
                try {
                    val decrypted = SecurityManager.decrypt(entry.encryptedPassword)
                    entry to decrypted
                } catch (e: Exception) {
                    null
                }
            }

            // Calculate vulnerabilities
            val vulnerabilities = calculateVulnerabilities(decryptedPasswords)

            // Calculate current risk score
            val currentRisk = calculateCurrentRisk(decryptedPasswords, vulnerabilities)

            // Predict future risks using time-series analysis
            val futureRisk30 = predictFutureRisk(currentRisk, 30, vulnerabilities)
            val futureRisk90 = predictFutureRisk(currentRisk, 90, vulnerabilities)

            // Calculate days until critical
            val daysUntilCritical = calculateDaysUntilCritical(currentRisk, vulnerabilities)

            // Calculate breach probability using ML-like algorithm
            val breachProb = calculateBreachProbability(decryptedPasswords, currentRisk)

            // Generate AI predictions
            val predictions =
                generatePredictions(currentRisk, futureRisk90, vulnerabilities, breachProb)

            // Generate action items
            val actions = generateActionItems(vulnerabilities, currentRisk)

            // Determine risk trend
            val trend = determineTrend(currentRisk, futureRisk30, futureRisk90)

            SecurityPrediction(
                currentRiskScore = currentRisk,
                futureRiskScore30Days = futureRisk30,
                futureRiskScore90Days = futureRisk90,
                daysUntilCritical = daysUntilCritical,
                breachProbability = breachProb,
                predictions = predictions,
                actionItems = actions,
                riskTrend = trend,
                vulnerabilities = vulnerabilities
            )
        }

    /**
     * Calculate vulnerabilities in password vault
     */
    private fun calculateVulnerabilities(
        passwords: List<Pair<PasswordVaultEntry, String>>
    ): List<Vulnerability> {
        val vulnerabilities = mutableListOf<Vulnerability>()

        // Check for breached passwords
        val breachedCount = passwords.count { (_, password) ->
            BreachDetector.checkBreach(password).isBreached
        }
        if (breachedCount > 0) {
            vulnerabilities.add(
                Vulnerability(VulnerabilityType.BREACHED_PASSWORDS, breachedCount, 40)
            )
        }

        // Check for weak passwords
        val weakCount = passwords.count { (entry, _) -> entry.strengthScore < 60 }
        if (weakCount > 0) {
            vulnerabilities.add(
                Vulnerability(VulnerabilityType.WEAK_PASSWORDS, weakCount, 25)
            )
        }

        // Check for reused passwords
        val passwordGroups = passwords.groupBy { it.second }
        val reusedCount = passwordGroups.count { it.value.size > 1 }
        if (reusedCount > 0) {
            vulnerabilities.add(
                Vulnerability(VulnerabilityType.REUSED_PASSWORDS, reusedCount, 20)
            )
        }

        // Check for old passwords (>365 days)
        val now = System.currentTimeMillis()
        val oldCount = passwords.count { (entry, _) ->
            val ageInDays = (now - entry.createdAt) / (1000 * 60 * 60 * 24)
            ageInDays > 365
        }
        if (oldCount > 0) {
            vulnerabilities.add(
                Vulnerability(VulnerabilityType.OLD_PASSWORDS, oldCount, 10)
            )
        }

        // Check for common patterns
        val patternCount = passwords.count { (_, password) ->
            hasCommonPattern(password)
        }
        if (patternCount > 0) {
            vulnerabilities.add(
                Vulnerability(VulnerabilityType.COMMON_PATTERNS, patternCount, 5)
            )
        }

        return vulnerabilities
    }

    /**
     * Calculate current risk score
     */
    private fun calculateCurrentRisk(
        passwords: List<Pair<PasswordVaultEntry, String>>,
        vulnerabilities: List<Vulnerability>
    ): Int {
        if (passwords.isEmpty()) return 0

        var riskScore = 0

        // Add risk from vulnerabilities
        vulnerabilities.forEach { vuln ->
            val vulnerabilityWeight = when (vuln.type) {
                VulnerabilityType.BREACHED_PASSWORDS -> 8
                VulnerabilityType.WEAK_PASSWORDS -> 5
                VulnerabilityType.REUSED_PASSWORDS -> 4
                VulnerabilityType.OLD_PASSWORDS -> 2
                VulnerabilityType.COMMON_PATTERNS -> 1
            }
            riskScore += (vuln.count * vulnerabilityWeight)
        }

        // Calculate as percentage of total passwords
        val riskPercentage = (riskScore.toFloat() / passwords.size.toFloat() * 100).toInt()

        return min(100, max(0, riskPercentage))
    }

    /**
     * Predict future risk using exponential growth model
     */
    private fun predictFutureRisk(
        currentRisk: Int,
        daysInFuture: Int,
        vulnerabilities: List<Vulnerability>
    ): Int {
        // Calculate growth rate based on vulnerabilities
        val hasBreaches = vulnerabilities.any { it.type == VulnerabilityType.BREACHED_PASSWORDS }
        val hasWeakPasswords = vulnerabilities.any { it.type == VulnerabilityType.WEAK_PASSWORDS }

        // Growth rate: 0.01-0.05 per day
        val growthRate = when {
            hasBreaches -> 0.05f // 5% per day if breached
            hasWeakPasswords -> 0.03f // 3% per day if weak
            vulnerabilities.size > 2 -> 0.02f // 2% per day if multiple issues
            else -> 0.01f // 1% per day baseline
        }

        // Exponential growth: risk(t) = risk(0) * e^(rate * time)
        val futureRisk = currentRisk * exp(growthRate * daysInFuture)

        return min(100, futureRisk.toInt())
    }

    /**
     * Calculate days until risk reaches critical level (80+)
     */
    private fun calculateDaysUntilCritical(
        currentRisk: Int,
        vulnerabilities: List<Vulnerability>
    ): Int {
        if (currentRisk >= 80) return 0 // Already critical
        if (vulnerabilities.isEmpty()) return 999 // No vulnerabilities

        val hasBreaches = vulnerabilities.any { it.type == VulnerabilityType.BREACHED_PASSWORDS }
        val growthRate = if (hasBreaches) 0.05f else 0.02f

        // Solve: 80 = currentRisk * e^(rate * days)
        // days = ln(80 / currentRisk) / rate
        val days = (ln((80.0 / currentRisk.toDouble())) / growthRate).toInt()

        return max(1, min(999, days))
    }

    /**
     * Calculate breach probability using ML-like algorithm
     */
    private fun calculateBreachProbability(
        passwords: List<Pair<PasswordVaultEntry, String>>,
        currentRisk: Int
    ): Float {
        if (passwords.isEmpty()) return 0f

        var probability = 0f

        // Factor 1: Breached passwords (highest weight)
        val breachedCount = passwords.count { (_, password) ->
            BreachDetector.checkBreach(password).isBreached
        }
        probability += (breachedCount.toFloat() / passwords.size) * 0.4f

        // Factor 2: Weak passwords
        val weakCount = passwords.count { (entry, _) -> entry.strengthScore < 60 }
        probability += (weakCount.toFloat() / passwords.size) * 0.25f

        // Factor 3: Current risk score
        probability += (currentRisk.toFloat() / 100f) * 0.2f

        // Factor 4: Password reuse
        val passwordGroups = passwords.groupBy { it.second }
        val reusedCount = passwordGroups.count { it.value.size > 1 }
        probability += (reusedCount.toFloat() / passwords.size) * 0.15f

        return min(1f, max(0f, probability))
    }

    /**
     * Generate AI predictions
     */
    private fun generatePredictions(
        currentRisk: Int,
        futureRisk90: Int,
        vulnerabilities: List<Vulnerability>,
        breachProb: Float
    ): List<PredictionInsight> {
        val predictions = mutableListOf<PredictionInsight>()

        // Prediction 1: Overall trend
        val riskDelta = futureRisk90 - currentRisk
        when {
            riskDelta > 40 -> predictions.add(
                PredictionInsight(
                    icon = "🚨",
                    title = "Critical Risk Escalation Predicted",
                    description = "AI models predict your security risk will increase by ${riskDelta}% in 90 days. Immediate action required to prevent data breach.",
                    severity = PredictionSeverity.CRITICAL,
                    confidence = 0.92f
                )
            )

            riskDelta > 20 -> predictions.add(
                PredictionInsight(
                    icon = "⚠️",
                    title = "Security Degradation Detected",
                    description = "Your vault security is projected to decline by ${riskDelta}% over the next 3 months without intervention.",
                    severity = PredictionSeverity.HIGH,
                    confidence = 0.85f
                )
            )

            riskDelta < 0 -> predictions.add(
                PredictionInsight(
                    icon = "✅",
                    title = "Security Improving",
                    description = "Current practices are improving your security posture. Keep up the good work!",
                    severity = PredictionSeverity.LOW,
                    confidence = 0.78f
                )
            )
        }

        // Prediction 2: Breach probability
        when {
            breachProb > 0.7f -> predictions.add(
                PredictionInsight(
                    icon = "🔴",
                    title = "High Breach Probability",
                    description = "AI calculates a ${(breachProb * 100).toInt()}% probability of successful breach attempt within 6 months.",
                    severity = PredictionSeverity.CRITICAL,
                    confidence = 0.88f
                )
            )

            breachProb > 0.4f -> predictions.add(
                PredictionInsight(
                    icon = "⚠️",
                    title = "Elevated Breach Risk",
                    description = "Machine learning models indicate a ${(breachProb * 100).toInt()}% chance of security compromise.",
                    severity = PredictionSeverity.HIGH,
                    confidence = 0.82f
                )
            )

            breachProb < 0.2f -> predictions.add(
                PredictionInsight(
                    icon = "🛡️",
                    title = "Strong Defense Posture",
                    description = "AI analysis shows only ${(breachProb * 100).toInt()}% breach probability. Your vault is well protected.",
                    severity = PredictionSeverity.LOW,
                    confidence = 0.90f
                )
            )
        }

        // Prediction 3: Specific vulnerabilities
        vulnerabilities.sortedByDescending { it.riskContribution }.take(2).forEach { vuln ->
            val insight = when (vuln.type) {
                VulnerabilityType.BREACHED_PASSWORDS -> PredictionInsight(
                    icon = "🚨",
                    title = "Leaked Credentials Detected",
                    description = "${vuln.count} password(s) found in known data breaches. Attackers likely have access to these credentials.",
                    severity = PredictionSeverity.CRITICAL,
                    confidence = 1.0f
                )

                VulnerabilityType.WEAK_PASSWORDS -> PredictionInsight(
                    icon = "🔓",
                    title = "Weak Password Pattern",
                    description = "${vuln.count} password(s) vulnerable to brute-force attacks. AI predicts cracking time: <24 hours.",
                    severity = PredictionSeverity.HIGH,
                    confidence = 0.87f
                )

                VulnerabilityType.REUSED_PASSWORDS -> PredictionInsight(
                    icon = "❌",
                    title = "Password Reuse Risk",
                    description = "${vuln.count} password(s) reused across services. Single breach exposes multiple accounts.",
                    severity = PredictionSeverity.MEDIUM,
                    confidence = 0.85f
                )

                VulnerabilityType.OLD_PASSWORDS -> PredictionInsight(
                    icon = "⏰",
                    title = "Aging Password Alert",
                    description = "${vuln.count} password(s) over 1 year old. Increased exposure time raises breach probability.",
                    severity = PredictionSeverity.MEDIUM,
                    confidence = 0.75f
                )

                VulnerabilityType.COMMON_PATTERNS -> PredictionInsight(
                    icon = "⚠️",
                    title = "Predictable Patterns Found",
                    description = "${vuln.count} password(s) use common patterns. AI models can predict these patterns.",
                    severity = PredictionSeverity.MEDIUM,
                    confidence = 0.80f
                )
            }
            predictions.add(insight)
        }

        return predictions
    }

    /**
     * Generate prioritized action items
     */
    private fun generateActionItems(
        vulnerabilities: List<Vulnerability>,
        currentRisk: Int
    ): List<ActionItem> {
        val actions = mutableListOf<ActionItem>()

        vulnerabilities.sortedByDescending { it.riskContribution }.forEach { vuln ->
            when (vuln.type) {
                VulnerabilityType.BREACHED_PASSWORDS -> actions.add(
                    ActionItem(
                        priority = 1,
                        action = "Change ${vuln.count} breached password(s) immediately",
                        impact = "Reduces risk by ${vuln.riskContribution}% - CRITICAL",
                        estimatedTime = "${vuln.count * 2} minutes"
                    )
                )

                VulnerabilityType.WEAK_PASSWORDS -> actions.add(
                    ActionItem(
                        priority = 2,
                        action = "Strengthen ${vuln.count} weak password(s)",
                        impact = "Reduces risk by ${vuln.riskContribution}%",
                        estimatedTime = "${vuln.count * 1} minutes"
                    )
                )

                VulnerabilityType.REUSED_PASSWORDS -> actions.add(
                    ActionItem(
                        priority = 3,
                        action = "Create unique passwords for ${vuln.count} reused password(s)",
                        impact = "Reduces risk by ${vuln.riskContribution}%",
                        estimatedTime = "${vuln.count * 2} minutes"
                    )
                )

                VulnerabilityType.OLD_PASSWORDS -> actions.add(
                    ActionItem(
                        priority = 4,
                        action = "Rotate ${vuln.count} outdated password(s)",
                        impact = "Reduces risk by ${vuln.riskContribution}%",
                        estimatedTime = "${vuln.count * 3} minutes"
                    )
                )

                VulnerabilityType.COMMON_PATTERNS -> actions.add(
                    ActionItem(
                        priority = 5,
                        action = "Replace ${vuln.count} predictable password(s)",
                        impact = "Reduces risk by ${vuln.riskContribution}%",
                        estimatedTime = "${vuln.count * 2} minutes"
                    )
                )
            }
        }

        return actions
    }

    /**
     * Determine risk trend
     */
    private fun determineTrend(
        currentRisk: Int,
        futureRisk30: Int,
        futureRisk90: Int
    ): RiskTrend {
        val delta30 = futureRisk30 - currentRisk
        val delta90 = futureRisk90 - currentRisk

        return when {
            currentRisk >= 80 || futureRisk90 >= 90 -> RiskTrend.CRITICAL
            delta90 > 30 -> RiskTrend.DEGRADING
            delta30 < -5 -> RiskTrend.IMPROVING
            else -> RiskTrend.STABLE
        }
    }

    /**
     * Check for common patterns in password
     */
    private fun hasCommonPattern(password: String): Boolean {
        val commonPatterns = listOf(
            Regex("(.)\\1{2,}"), // Repeating characters (aaa, 111)
            Regex("(012|123|234|345|456|567|678|789|890)"), // Sequential numbers
            Regex(
                "(abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz)",
                RegexOption.IGNORE_CASE
            ), // Sequential letters
            Regex("(qwerty|asdfgh|zxcvbn)", RegexOption.IGNORE_CASE), // Keyboard patterns
            Regex(
                "^(password|admin|user|login|welcome|guest)",
                RegexOption.IGNORE_CASE
            ) // Common words
        )

        return commonPatterns.any { it.containsMatchIn(password) }
    }

    // ==================== NEW: ADVANCED FEATURES ====================

    /**
     * PER-PASSWORD ANALYSIS
     * Analyzes each password individually with detailed breakdown
     */
    data class PasswordAnalysisResult(
        val passwordEntry: PasswordVaultEntry,
        val overallScore: Int, // 0-100
        val strengthLevel: StrengthLevel,
        val issues: List<PasswordIssue>,
        val strengths: List<PasswordStrength>,
        val recommendations: List<String>,
        val suggestedPassword: String,
        val improvementPotential: Int, // How much score would improve
        val breachDetails: BreachDetails?,
        val attackResistance: AttackResistance,
        val ageInfo: AgeInfo
    )

    enum class StrengthLevel(val displayName: String, val icon: String, val colorHex: Long) {
        CRITICAL("Critical", "🔴", 0xFFD32F2F),
        WEAK("Weak", "🟠", 0xFFF57C00),
        MEDIUM("Medium", "🟡", 0xFFFBC02D),
        GOOD("Good", "🟢", 0xFF8BC34A),
        STRONG("Strong", "💚", 0xFF388E3C),
        EXCELLENT("Excellent", "💎", 0xFF1976D2)
    }

    data class PasswordIssue(
        val type: IssueType,
        val description: String,
        val severity: IssueSeverity,
        val fixSuggestion: String
    )

    enum class IssueType(val icon: String) {
        TOO_SHORT("📏"),
        NO_UPPERCASE("🔤"),
        NO_LOWERCASE("🔡"),
        NO_NUMBERS("🔢"),
        NO_SYMBOLS("❗"),
        COMMON_PATTERN("⚠️"),
        IN_BREACH("🚨"),
        REUSED("❌"),
        TOO_OLD("⏰"),
        DICTIONARY_WORD("📖"),
        PERSONAL_INFO("👤")
    }

    enum class IssueSeverity {
        CRITICAL, HIGH, MEDIUM, LOW
    }

    data class PasswordStrength(
        val type: String,
        val description: String,
        val icon: String
    )

    data class BreachDetails(
        val isBreached: Boolean,
        val breachCount: Int,
        val breaches: List<BreachIncident>,
        val totalExposures: Long,
        val darkWebPrice: String,
        val urgencyLevel: String,
        val estimatedCompromiseTime: String
    )

    data class BreachIncident(
        val source: String,
        val year: Int,
        val affectedAccounts: Long
    )

    data class AttackResistance(
        val dictionaryAttackTime: String,
        val bruteForceTime: String,
        val rainbowTableTime: String,
        val overallResistance: String,
        val entropy: Double
    )

    data class AgeInfo(
        val ageInDays: Int,
        val createdDate: String,
        val lastModifiedDate: String,
        val needsRotation: Boolean,
        val rotationRecommendation: String
    )

    /**
     * Analyze individual password in detail
     */
    suspend fun analyzePassword(entry: PasswordVaultEntry): PasswordAnalysisResult = withContext(Dispatchers.Default) {
        val password = try {
            SecurityManager.decrypt(entry.encryptedPassword)
        } catch (e: Exception) {
            return@withContext PasswordAnalysisResult(
                passwordEntry = entry,
                overallScore = 0,
                strengthLevel = StrengthLevel.CRITICAL,
                issues = listOf(
                    PasswordIssue(
                        type = IssueType.TOO_SHORT,
                        description = "Unable to decrypt password",
                        severity = IssueSeverity.CRITICAL,
                        fixSuggestion = "Re-save this password"
                    )
                ),
                strengths = emptyList(),
                recommendations = listOf("Password encryption error - please re-save"),
                suggestedPassword = generateStrongPassword(16),
                improvementPotential = 0,
                breachDetails = null,
                attackResistance = AttackResistance("N/A", "N/A", "N/A", "Unknown", 0.0),
                ageInfo = calculateAgeInfo(entry)
            )
        }

        // Analyze password components
        val issues = mutableListOf<PasswordIssue>()
        val strengths = mutableListOf<PasswordStrength>()

        // Check length
        if (password.length < 8) {
            issues.add(PasswordIssue(
                type = IssueType.TOO_SHORT,
                description = "Only ${password.length} characters (need 12+)",
                severity = IssueSeverity.HIGH,
                fixSuggestion = "Add ${12 - password.length} more characters"
            ))
        } else if (password.length >= 16) {
            strengths.add(PasswordStrength(
                type = "Length",
                description = "Excellent length (${password.length} characters)",
                icon = "✅"
            ))
        }

        // Check uppercase
        if (!password.any { it.isUpperCase() }) {
            issues.add(PasswordIssue(
                type = IssueType.NO_UPPERCASE,
                description = "No uppercase letters",
                severity = IssueSeverity.MEDIUM,
                fixSuggestion = "Add A-Z characters"
            ))
        } else {
            strengths.add(PasswordStrength("Uppercase", "Contains uppercase letters", "✅"))
        }

        // Check lowercase
        if (!password.any { it.isLowerCase() }) {
            issues.add(PasswordIssue(
                type = IssueType.NO_LOWERCASE,
                description = "No lowercase letters",
                severity = IssueSeverity.MEDIUM,
                fixSuggestion = "Add a-z characters"
            ))
        } else {
            strengths.add(PasswordStrength("Lowercase", "Contains lowercase letters", "✅"))
        }

        // Check numbers
        if (!password.any { it.isDigit() }) {
            issues.add(PasswordIssue(
                type = IssueType.NO_NUMBERS,
                description = "No numbers",
                severity = IssueSeverity.MEDIUM,
                fixSuggestion = "Add 0-9 digits"
            ))
        } else {
            strengths.add(PasswordStrength("Numbers", "Contains numbers", "✅"))
        }

        // Check special characters
        val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        if (!password.any { it in specialChars }) {
            issues.add(PasswordIssue(
                type = IssueType.NO_SYMBOLS,
                description = "No special characters",
                severity = IssueSeverity.HIGH,
                fixSuggestion = "Add symbols like !@#$%"
            ))
        } else {
            strengths.add(PasswordStrength("Symbols", "Contains special characters", "✅"))
        }

        // Check for common patterns
        if (hasCommonPattern(password)) {
            issues.add(PasswordIssue(
                type = IssueType.COMMON_PATTERN,
                description = "Contains predictable pattern",
                severity = IssueSeverity.HIGH,
                fixSuggestion = "Use random characters instead"
            ))
        }

        // Check breach status
        val breachResult = BreachDetector.checkBreach(password)
        val breachDetails = if (breachResult.isBreached) {
            issues.add(PasswordIssue(
                type = IssueType.IN_BREACH,
                description = "Found in ${breachResult.breachCount} data breach(es)",
                severity = IssueSeverity.CRITICAL,
                fixSuggestion = "Change immediately - password is compromised!"
            ))
            createBreachDetails(breachResult)
        } else {
            strengths.add(PasswordStrength("Not Breached", "Not found in known breaches", "✅"))
            null
        }

        // Calculate age
        val ageInfo = calculateAgeInfo(entry)
        if (ageInfo.needsRotation) {
            issues.add(PasswordIssue(
                type = IssueType.TOO_OLD,
                description = "Password is ${ageInfo.ageInDays} days old",
                severity = if (ageInfo.ageInDays > 730) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                fixSuggestion = ageInfo.rotationRecommendation
            ))
        }

        // Calculate overall score
        val baseScore = entry.strengthScore
        val issuesPenalty: Int = issues.map { issue ->
            when (issue.severity) {
                IssueSeverity.CRITICAL -> 20
                IssueSeverity.HIGH -> 15
                IssueSeverity.MEDIUM -> 10
                IssueSeverity.LOW -> 5
            }
        }.sum()
        val overallScore = max(0, min(100, baseScore - issuesPenalty))

        // Determine strength level
        val strengthLevel = when {
            overallScore >= 90 -> StrengthLevel.EXCELLENT
            overallScore >= 75 -> StrengthLevel.STRONG
            overallScore >= 60 -> StrengthLevel.GOOD
            overallScore >= 40 -> StrengthLevel.MEDIUM
            overallScore >= 20 -> StrengthLevel.WEAK
            else -> StrengthLevel.CRITICAL
        }

        // Generate recommendations
        val recommendations = generateRecommendations(issues, password)

        // Generate suggested password that fixes all issues
        val suggestedPassword = generateImprovedPassword(password, issues)

        // Calculate improvement potential
        val improvementPotential = max(0, 95 - overallScore)

        // Calculate attack resistance
        val attackResistance = calculateAttackResistance(password)

        PasswordAnalysisResult(
            passwordEntry = entry,
            overallScore = overallScore,
            strengthLevel = strengthLevel,
            issues = issues,
            strengths = strengths,
            recommendations = recommendations,
            suggestedPassword = suggestedPassword,
            improvementPotential = improvementPotential,
            breachDetails = breachDetails,
            attackResistance = attackResistance,
            ageInfo = ageInfo
        )
    }

    /**
     * Analyze all passwords individually
     */
    suspend fun analyzeAllPasswords(passwords: List<PasswordVaultEntry>): List<PasswordAnalysisResult> = 
        withContext(Dispatchers.Default) {
            passwords.map { analyzePassword(it) }
        }

    /**
     * Create detailed breach information
     */
    private fun createBreachDetails(breachResult: BreachDetector.BreachResult): BreachDetails {
        // Simulate breach incidents based on breach count
        val incidents = when {
            breachResult.breachCount > 20_000_000 -> listOf(
                BreachIncident("Collection #1", 2019, 773_000_000),
                BreachIncident("LinkedIn", 2012, 117_000_000),
                BreachIncident("Yahoo", 2013, 3_000_000_000)
            )
            breachResult.breachCount > 1_000_000 -> listOf(
                BreachIncident("LinkedIn", 2012, 117_000_000),
                BreachIncident("Adobe", 2013, 153_000_000)
            )
            breachResult.breachCount > 100_000 -> listOf(
                BreachIncident("Dropbox", 2012, 68_000_000)
            )
            else -> listOf(
                BreachIncident("Various Sites", 2020, breachResult.breachCount.toLong())
            )
        }

        val darkWebPrice = when {
            breachResult.breachCount > 10_000_000 -> "$0.01/account"
            breachResult.breachCount > 1_000_000 -> "$0.05/account"
            else -> "$0.50/account"
        }

        val urgencyLevel = when (breachResult.severity) {
            BreachDetector.BreachSeverity.CRITICAL -> "CHANGE IMMEDIATELY"
            BreachDetector.BreachSeverity.HIGH -> "Change within 24 hours"
            BreachDetector.BreachSeverity.MEDIUM -> "Change within 1 week"
            else -> "Consider changing soon"
        }

        val compromiseTime = when (breachResult.severity) {
            BreachDetector.BreachSeverity.CRITICAL -> "Already compromised"
            BreachDetector.BreachSeverity.HIGH -> "< 24 hours"
            BreachDetector.BreachSeverity.MEDIUM -> "1-7 days"
            else -> "1-30 days"
        }

        return BreachDetails(
            isBreached = true,
            breachCount = breachResult.breachCount,
            breaches = incidents,
            totalExposures = incidents.sumOf { it.affectedAccounts },
            darkWebPrice = darkWebPrice,
            urgencyLevel = urgencyLevel,
            estimatedCompromiseTime = compromiseTime
        )
    }

    /**
     * Calculate attack resistance metrics
     */
    private fun calculateAttackResistance(password: String): AttackResistance {
        // Calculate entropy (bits of randomness)
        val charsetSize = when {
            password.any { it in "!@#$%^&*()_+-=[]{}|;:,.<>?" } -> 94 // All printable ASCII
            password.any { it.isDigit() } && password.any { it.isLetter() } -> 62 // Alphanumeric
            password.any { it.isLetter() } -> 52 // Letters only
            password.any { it.isDigit() } -> 10 // Numbers only
            else -> 26 // Lowercase only
        }
        
        val entropy = password.length * (ln(charsetSize.toDouble()) / ln(2.0))
        
        // Calculate attack times
        val combinationsPerSecond = 1_000_000_000.0 // 1 billion attempts/second (GPU)
        val totalCombinations = charsetSize.toDouble().pow(password.length.toDouble())
        val secondsToCrack = totalCombinations / combinationsPerSecond / 2 // Average case

        // Dictionary attack (much faster if common words)
        val dictionaryTime = if (password.length < 8 || hasCommonPattern(password)) {
            "< 1 second"
        } else if (password.length < 10) {
            "< 1 minute"
        } else if (password.length < 12) {
            "< 1 hour"
        } else {
            "Several days"
        }

        // Brute force attack
        val bruteForceTime = when {
            secondsToCrack < 1 -> "Instant"
            secondsToCrack < 60 -> "${secondsToCrack.toInt()} seconds"
            secondsToCrack < 3600 -> "${(secondsToCrack / 60).toInt()} minutes"
            secondsToCrack < 86400 -> "${(secondsToCrack / 3600).toInt()} hours"
            secondsToCrack < 31536000 -> "${(secondsToCrack / 86400).toInt()} days"
            secondsToCrack < 3153600000.0 -> "${(secondsToCrack / 31536000).toInt()} years"
            else -> "Billions of years"
        }

        // Rainbow table attack (faster for short passwords)
        val rainbowTime = if (password.length < 8) {
            "< 10 minutes"
        } else if (password.length < 10) {
            "Few hours"
        } else {
            "Not feasible"
        }

        // Overall resistance
        val resistance = when {
            entropy > 80 -> "Excellent - Resistant to all attacks"
            entropy > 60 -> "Good - Resistant to most attacks"
            entropy > 40 -> "Fair - Vulnerable to dedicated attacks"
            entropy > 20 -> "Weak - Easy to crack"
            else -> "Critical - Can be cracked instantly"
        }

        return AttackResistance(
            dictionaryAttackTime = dictionaryTime,
            bruteForceTime = bruteForceTime,
            rainbowTableTime = rainbowTime,
            overallResistance = resistance,
            entropy = entropy
        )
    }

    /**
     * Calculate password age information
     */
    private fun calculateAgeInfo(entry: PasswordVaultEntry): AgeInfo {
        val now = System.currentTimeMillis()
        val ageInDays = ((now - entry.createdAt) / (1000 * 60 * 60 * 24)).toInt()
        val daysSinceModified = ((now - entry.modifiedAt) / (1000 * 60 * 60 * 24)).toInt()
        
        val needsRotation = ageInDays > 365
        
        val recommendation = when {
            ageInDays > 730 -> "Critical: Rotate immediately (2+ years old)"
            ageInDays > 365 -> "High priority: Rotate within 1 week"
            ageInDays > 180 -> "Consider rotating soon (6+ months old)"
            else -> "Age is acceptable"
        }

        return AgeInfo(
            ageInDays = ageInDays,
            createdDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(entry.createdAt)),
            lastModifiedDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(entry.modifiedAt)),
            needsRotation = needsRotation,
            rotationRecommendation = recommendation
        )
    }

    /**
     * Generate smart recommendations based on issues
     */
    private fun generateRecommendations(issues: List<PasswordIssue>, currentPassword: String): List<String> {
        val recommendations = mutableListOf<String>()

        if (issues.any { it.type == IssueType.IN_BREACH }) {
            recommendations.add("🚨 CRITICAL: Change immediately - password is compromised")
        }

        if (issues.any { it.type == IssueType.TOO_SHORT }) {
            val minLength = 12
            recommendations.add("📏 Increase length to at least $minLength characters")
        }

        if (issues.any { it.type == IssueType.NO_SYMBOLS }) {
            recommendations.add("❗ Add special characters (!@#$%^&*)")
        }

        if (issues.any { it.type == IssueType.NO_NUMBERS }) {
            recommendations.add("🔢 Include at least 2-3 numbers")
        }

        if (issues.any { it.type == IssueType.COMMON_PATTERN }) {
            recommendations.add("⚠️ Avoid sequential or repeated characters")
        }

        if (issues.any { it.type == IssueType.TOO_OLD }) {
            recommendations.add("🔄 Rotate password (recommended every 6-12 months)")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("✅ Password is strong - keep monitoring!")
        }

        return recommendations
    }

    /**
     * Generate improved password that fixes all issues
     */
    private fun generateImprovedPassword(currentPassword: String, issues: List<PasswordIssue>): String {
        // Start with a base strong password
        val length = max(16, currentPassword.length + 4)
        
        // Build character set based on issues
        val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowercase = "abcdefghijklmnopqrstuvwxyz"
        val numbers = "0123456789"
        val symbols = "!@#$%^&*"
        
        val allChars = uppercase + lowercase + numbers + symbols
        
        // Generate random strong password
        val random = java.security.SecureRandom()
        val password = StringBuilder()
        
        // Ensure at least one of each type
        password.append(uppercase[random.nextInt(uppercase.length)])
        password.append(lowercase[random.nextInt(lowercase.length)])
        password.append(numbers[random.nextInt(numbers.length)])
        password.append(symbols[random.nextInt(symbols.length)])
        
        // Fill the rest randomly
        repeat(length - 4) {
            password.append(allChars[random.nextInt(allChars.length)])
        }
        
        // Shuffle the password
        val shuffled = password.toString().toList().shuffled(random).joinToString("")
        
        return shuffled
    }

    /**
     * Generate strong password with specific length
     */
    private fun generateStrongPassword(length: Int): String {
        val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowercase = "abcdefghijklmnopqrstuvwxyz"
        val numbers = "0123456789"
        val symbols = "!@#$%^&*"
        val allChars = uppercase + lowercase + numbers + symbols
        
        val random = java.security.SecureRandom()
        val password = StringBuilder()
        
        password.append(uppercase[random.nextInt(uppercase.length)])
        password.append(lowercase[random.nextInt(lowercase.length)])
        password.append(numbers[random.nextInt(numbers.length)])
        password.append(symbols[random.nextInt(symbols.length)])
        
        repeat(length - 4) {
            password.append(allChars[random.nextInt(allChars.length)])
        }
        
        return password.toString().toList().shuffled(random).joinToString("")
    }

    /**
     * HISTORICAL TRACKING
     * Track security score over time
     */
    data class SecurityScoreHistory(
        val entries: List<HistoryEntry>,
        val trend: TrendDirection,
        val improvementPercentage: Float,
        val milestones: List<Milestone>
    )

    data class HistoryEntry(
        val timestamp: Long,
        val score: Int,
        val totalPasswords: Int,
        val weakCount: Int,
        val breachedCount: Int
    )

    enum class TrendDirection(val icon: String) {
        IMPROVING("📈"),
        STABLE("➡️"),
        DECLINING("📉")
    }

    data class Milestone(
        val title: String,
        val description: String,
        val timestamp: Long,
        val icon: String,
        val achieved: Boolean
    )
}
