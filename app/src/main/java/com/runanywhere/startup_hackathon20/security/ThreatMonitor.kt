package com.runanywhere.startup_hackathon20.security

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * ThreatMonitor - Real-time security threat monitoring system
 * Tracks and monitors various security threats across the application
 */
class ThreatMonitor private constructor(private val context: Context) {

    private val _threats = MutableStateFlow<List<SecurityThreat>>(emptyList())
    val threats: StateFlow<List<SecurityThreat>> = _threats.asStateFlow()

    private val _criticalAlertCount = MutableStateFlow(0)
    val criticalAlertCount: StateFlow<Int> = _criticalAlertCount.asStateFlow()

    private val activeThreatMap = ConcurrentHashMap<String, SecurityThreat>()

    companion object {
        @Volatile
        private var instance: ThreatMonitor? = null

        fun getInstance(context: Context): ThreatMonitor {
            return instance ?: synchronized(this) {
                instance ?: ThreatMonitor(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        // Initialize with some default monitoring
        startMonitoring()
    }

    /**
     * Start continuous monitoring
     */
    private fun startMonitoring() {
        // This would be called periodically by a background service
        // For now, we'll update when explicitly called
    }

    /**
     * Add or update a security threat
     */
    fun reportThreat(threat: SecurityThreat) {
        activeThreatMap[threat.id] = threat
        updateThreats()
    }

    /**
     * Remove a resolved threat
     */
    fun resolveThreat(threatId: String) {
        activeThreatMap.remove(threatId)
        updateThreats()
    }

    /**
     * Clear all threats of a specific type
     */
    fun clearThreatsOfType(type: ThreatType) {
        activeThreatMap.entries.removeIf { it.value.type == type }
        updateThreats()
    }

    /**
     * Update threat list and critical count
     */
    private fun updateThreats() {
        val threatList = activeThreatMap.values
            .sortedByDescending { it.severity.ordinal }
            .toList()

        _threats.value = threatList
        _criticalAlertCount.value = threatList.count {
            it.severity == ThreatSeverity.CRITICAL || it.severity == ThreatSeverity.HIGH
        }
    }

    /**
     * Get threats by severity
     */
    fun getThreatsBySeverity(severity: ThreatSeverity): List<SecurityThreat> {
        return _threats.value.filter { it.severity == severity }
    }

    /**
     * Get critical and high severity threats
     */
    fun getCriticalThreats(): List<SecurityThreat> {
        return _threats.value.filter {
            it.severity == ThreatSeverity.CRITICAL || it.severity == ThreatSeverity.HIGH
        }
    }

    /**
     * Check for weak passwords
     */
    fun checkWeakPasswords(weakCount: Int) {
        if (weakCount > 0) {
            reportThreat(
                SecurityThreat(
                    id = "weak_passwords",
                    type = ThreatType.WEAK_PASSWORD,
                    severity = if (weakCount > 5) ThreatSeverity.HIGH else ThreatSeverity.MEDIUM,
                    title = "$weakCount Weak Password${if (weakCount > 1) "s" else ""} Detected",
                    description = "Weak passwords are vulnerable to brute-force attacks. Update them immediately.",
                    timestamp = System.currentTimeMillis(),
                    actionRequired = true
                )
            )
        } else {
            resolveThreat("weak_passwords")
        }
    }

    /**
     * Check for breached passwords
     */
    fun checkBreachedPasswords(breachedCount: Int) {
        if (breachedCount > 0) {
            reportThreat(
                SecurityThreat(
                    id = "breached_passwords",
                    type = ThreatType.DATA_BREACH,
                    severity = ThreatSeverity.CRITICAL,
                    title = "$breachedCount Leaked Password${if (breachedCount > 1) "s" else ""} - Act Now!",
                    description = "These passwords have been exposed in data breaches. Change them immediately to prevent account compromise.",
                    timestamp = System.currentTimeMillis(),
                    actionRequired = true
                )
            )
        } else {
            resolveThreat("breached_passwords")
        }
    }

    /**
     * Check for reused passwords
     */
    fun checkReusedPasswords(reusedCount: Int) {
        if (reusedCount > 0) {
            reportThreat(
                SecurityThreat(
                    id = "reused_passwords",
                    type = ThreatType.REUSED_PASSWORD,
                    severity = ThreatSeverity.HIGH,
                    title = "$reusedCount Reused Password${if (reusedCount > 1) "s" else ""}",
                    description = "Password reuse increases risk. If one account is compromised, all accounts using the same password are at risk.",
                    timestamp = System.currentTimeMillis(),
                    actionRequired = true
                )
            )
        } else {
            resolveThreat("reused_passwords")
        }
    }

    /**
     * Check for unencrypted data
     */
    fun checkUnencryptedData(unencryptedCount: Int) {
        if (unencryptedCount > 0) {
            reportThreat(
                SecurityThreat(
                    id = "unencrypted_data",
                    type = ThreatType.UNENCRYPTED_DATA,
                    severity = ThreatSeverity.MEDIUM,
                    title = "$unencryptedCount Item${if (unencryptedCount > 1) "s" else ""} Not Encrypted",
                    description = "Unencrypted data can be accessed if your device is compromised. Encrypt all sensitive data.",
                    timestamp = System.currentTimeMillis(),
                    actionRequired = true
                )
            )
        } else {
            resolveThreat("unencrypted_data")
        }
    }

    /**
     * Check for outdated app version
     */
    fun checkAppVersion(isOutdated: Boolean) {
        if (isOutdated) {
            reportThreat(
                SecurityThreat(
                    id = "outdated_app",
                    type = ThreatType.OUTDATED_SOFTWARE,
                    severity = ThreatSeverity.LOW,
                    title = "App Update Available",
                    description = "A newer version of SafeSphere is available with security improvements and bug fixes.",
                    timestamp = System.currentTimeMillis(),
                    actionRequired = false
                )
            )
        } else {
            resolveThreat("outdated_app")
        }
    }

    /**
     * Report suspicious activity
     */
    fun reportSuspiciousActivity(activity: String) {
        reportThreat(
            SecurityThreat(
                id = "suspicious_${System.currentTimeMillis()}",
                type = ThreatType.SUSPICIOUS_ACTIVITY,
                severity = ThreatSeverity.HIGH,
                title = "Suspicious Activity Detected",
                description = activity,
                timestamp = System.currentTimeMillis(),
                actionRequired = true
            )
        )
    }

    /**
     * Check for missing biometric authentication
     */
    fun checkBiometricAuth(isEnabled: Boolean, hasBiometricHardware: Boolean) {
        if (!isEnabled && hasBiometricHardware) {
            reportThreat(
                SecurityThreat(
                    id = "no_biometric",
                    type = ThreatType.WEAK_AUTHENTICATION,
                    severity = ThreatSeverity.LOW,
                    title = "Biometric Authentication Not Enabled",
                    description = "Enable fingerprint or face authentication for enhanced security.",
                    timestamp = System.currentTimeMillis(),
                    actionRequired = false
                )
            )
        } else {
            resolveThreat("no_biometric")
        }
    }

    /**
     * Report authentication failure
     */
    fun reportAuthFailure(failureCount: Int) {
        if (failureCount >= 3) {
            reportThreat(
                SecurityThreat(
                    id = "auth_failures",
                    type = ThreatType.BRUTE_FORCE_ATTEMPT,
                    severity = if (failureCount >= 5) ThreatSeverity.CRITICAL else ThreatSeverity.HIGH,
                    title = "Multiple Failed Login Attempts",
                    description = "$failureCount failed login attempts detected. Someone may be trying to access your account.",
                    timestamp = System.currentTimeMillis(),
                    actionRequired = true
                )
            )
        } else {
            resolveThreat("auth_failures")
        }
    }

    /**
     * Clear all threats
     */
    fun clearAllThreats() {
        activeThreatMap.clear()
        updateThreats()
    }
}

/**
 * Security Threat Data Model
 */
data class SecurityThreat(
    val id: String,
    val type: ThreatType,
    val severity: ThreatSeverity,
    val title: String,
    val description: String,
    val timestamp: Long,
    val actionRequired: Boolean
)

/**
 * Threat Types
 */
enum class ThreatType {
    WEAK_PASSWORD,
    DATA_BREACH,
    REUSED_PASSWORD,
    UNENCRYPTED_DATA,
    SUSPICIOUS_ACTIVITY,
    OUTDATED_SOFTWARE,
    WEAK_AUTHENTICATION,
    BRUTE_FORCE_ATTEMPT,
    NETWORK_ATTACK,
    MALWARE_DETECTED
}

/**
 * Threat Severity Levels
 */
enum class ThreatSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
