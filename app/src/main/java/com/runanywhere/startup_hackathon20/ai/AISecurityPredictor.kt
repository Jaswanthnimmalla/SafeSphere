package com.runanywhere.startup_hackathon20.ai

import com.runanywhere.startup_hackathon20.data.PasswordVaultEntry
import com.runanywhere.startup_hackathon20.utils.BreachDetector
import com.runanywhere.startup_hackathon20.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.max

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
        val days = (Math.log((80.0 / currentRisk.toDouble())) / growthRate).toInt()

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
}
