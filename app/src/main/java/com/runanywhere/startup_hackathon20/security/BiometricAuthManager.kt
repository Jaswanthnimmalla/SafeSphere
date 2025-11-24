package com.runanywhere.startup_hackathon20.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat

/**
 * Biometric Authentication Manager
 * Handles fingerprint, face ID, and other biometric authentication
 */
object BiometricAuthManager {

    /**
     * Check if biometric authentication is available on device
     */
    fun isBiometricAvailable(context: Context): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)

        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        )) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                BiometricAvailability.Available

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                BiometricAvailability.NoHardware

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                BiometricAvailability.HardwareUnavailable

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                BiometricAvailability.NoneEnrolled

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                BiometricAvailability.SecurityUpdateRequired

            else -> BiometricAvailability.Unknown
        }
    }

    /**
     * Authenticate using biometrics
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Biometric Authentication",
        subtitle: String = "Use your fingerprint or face to unlock SafeSphere",
        negativeButtonText: String = "Use Password",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errorMessage: String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Get biometric type description
     */
    fun getBiometricType(context: Context): String {
        val biometricManager = BiometricManager.from(context)

        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Check if face or fingerprint (this is device-specific)
                // Most devices will show appropriate UI automatically
                "Fingerprint or Face ID"
            }

            else -> "Biometric"
        }
    }

    /**
     * Save user credentials for biometric login
     */
    fun saveCredentialsForBiometric(context: Context, email: String, password: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences("biometric_auth", Context.MODE_PRIVATE)

            // Encrypt credentials before storing
            val encryptedEmail = SecurityManager.encrypt(email)
            val encryptedPassword = SecurityManager.encrypt(password)

            prefs.edit()
                .putString("biometric_email", encryptedEmail)
                .putString("biometric_password", encryptedPassword)
                .putBoolean("biometric_enabled", true)
                .apply()

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get saved credentials for biometric login
     */
    fun getSavedCredentials(context: Context): Pair<String, String>? {
        return try {
            val prefs = context.getSharedPreferences("biometric_auth", Context.MODE_PRIVATE)

            if (!prefs.getBoolean("biometric_enabled", false)) {
                return null
            }

            val encryptedEmail = prefs.getString("biometric_email", null) ?: return null
            val encryptedPassword = prefs.getString("biometric_password", null) ?: return null

            // Decrypt credentials
            val email = SecurityManager.decrypt(encryptedEmail)
            val password = SecurityManager.decrypt(encryptedPassword)

            Pair(email, password)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if biometric login is enabled
     */
    fun isBiometricLoginEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("biometric_auth", Context.MODE_PRIVATE)
        return prefs.getBoolean("biometric_enabled", false)
    }

    /**
     * Disable biometric login
     */
    fun disableBiometricLogin(context: Context) {
        val prefs = context.getSharedPreferences("biometric_auth", Context.MODE_PRIVATE)
        prefs.edit()
            .clear()
            .apply()
    }
}

/**
 * Biometric availability status
 */
sealed class BiometricAvailability {
    object Available : BiometricAvailability()
    object NoHardware : BiometricAvailability()
    object HardwareUnavailable : BiometricAvailability()
    object NoneEnrolled : BiometricAvailability()
    object SecurityUpdateRequired : BiometricAvailability()
    object Unknown : BiometricAvailability()
}
