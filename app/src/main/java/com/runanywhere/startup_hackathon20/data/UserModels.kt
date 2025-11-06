package com.runanywhere.startup_hackathon20.data

import java.util.UUID

/**
 * User authentication data models
 */

/**
 * User account (stored encrypted)
 */
data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val passwordHash: String, // Argon2 or bcrypt hash
    val profilePhotoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long? = null,
    val biometricEnabled: Boolean = false
)

/**
 * User session
 */
data class UserSession(
    val userId: String,
    val sessionId: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + SESSION_TIMEOUT,
    val isActive: Boolean = true
) {
    companion object {
        const val SESSION_TIMEOUT = 30 * 60 * 1000L // 30 minutes
    }
}

/**
 * Login credentials
 */
data class LoginCredentials(
    val email: String,
    val password: String,
    val useBiometric: Boolean = false
)

/**
 * Registration data
 */
data class RegistrationData(
    val name: String,
    val email: String,
    val password: String,
    val confirmPassword: String,
    val profilePhotoUri: String? = null
)

/**
 * Authentication result
 */
sealed class AuthResult {
    data class Success(val user: User, val session: UserSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Validation errors
 */
data class ValidationError(
    val field: String,
    val message: String
)
