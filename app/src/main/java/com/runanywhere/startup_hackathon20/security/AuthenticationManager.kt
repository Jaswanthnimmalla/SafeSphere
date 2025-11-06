package com.runanywhere.startup_hackathon20.security

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.runanywhere.startup_hackathon20.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Authentication Manager - Handles user registration, login, and session management
 *
 * Features:
 * - Password hashing with PBKDF2 (secure alternative to Argon2/bcrypt)
 * - Encrypted user storage
 * - Session management with auto-timeout
 * - Email validation
 * - Password strength validation
 */
class AuthenticationManager private constructor(private val context: Context) {

    private val gson = Gson()
    private val usersFile = File(context.filesDir, "users.enc")
    private val sessionFile = File(context.filesDir, "session.enc")

    private var currentUser: User? = null
    private var currentSession: UserSession? = null

    companion object {
        private const val TAG = "AuthManager"

        @Volatile
        private var instance: AuthenticationManager? = null

        fun getInstance(context: Context): AuthenticationManager {
            return instance ?: synchronized(this) {
                instance ?: AuthenticationManager(context.applicationContext).also {
                    instance = it
                }
            }
        }

        // Password hashing constants
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 10000
        private const val HASH_BYTES = 32
        private const val SALT_BYTES = 16
    }

    init {
        loadSession()
    }

    // ==================== REGISTRATION ====================

    /**
     * Register new user
     */
    suspend fun register(registrationData: RegistrationData): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                // Validate input
                val validationErrors = validateRegistration(registrationData)
                if (validationErrors.isNotEmpty()) {
                    return@withContext AuthResult.Error(validationErrors.first().message)
                }

                // Check if email already exists
                val existingUsers = loadUsers()
                if (existingUsers.any {
                        it.email.equals(
                            registrationData.email,
                            ignoreCase = true
                        )
                    }) {
                    return@withContext AuthResult.Error("Email already registered")
                }

                // Hash password
                val passwordHash = hashPassword(registrationData.password)

                // Create user
                val user = User(
                    name = registrationData.name,
                    email = registrationData.email.lowercase(),
                    passwordHash = passwordHash,
                    profilePhotoUri = registrationData.profilePhotoUri,
                    createdAt = System.currentTimeMillis()
                )

                // Save user
                val updatedUsers = existingUsers + user
                saveUsers(updatedUsers)

                // Create session
                val session = createSession(user)

                Log.d(TAG, "✅ User registered: ${user.email}")
                AuthResult.Success(user, session)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Registration failed: ${e.message}", e)
                AuthResult.Error("Registration failed: ${e.message}")
            }
        }

    /**
     * Validate registration data
     */
    private fun validateRegistration(data: RegistrationData): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Name validation
        if (data.name.isBlank()) {
            errors.add(ValidationError("name", "Name is required"))
        } else if (data.name.length < 2) {
            errors.add(ValidationError("name", "Name must be at least 2 characters"))
        }

        // Email validation
        if (data.email.isBlank()) {
            errors.add(ValidationError("email", "Email is required"))
        } else if (!isValidEmail(data.email)) {
            errors.add(ValidationError("email", "Invalid email format"))
        }

        // Password validation
        val passwordValidation = validatePasswordStrength(data.password)
        if (passwordValidation != null) {
            errors.add(ValidationError("password", passwordValidation))
        }

        // Confirm password
        if (data.password != data.confirmPassword) {
            errors.add(ValidationError("confirmPassword", "Passwords do not match"))
        }

        return errors
    }

    // ==================== LOGIN ====================

    /**
     * Login user
     */
    suspend fun login(credentials: LoginCredentials): AuthResult = withContext(Dispatchers.IO) {
        try {
            // Find user by email
            val users = loadUsers()
            val user = users.find { it.email.equals(credentials.email, ignoreCase = true) }

            if (user == null) {
                return@withContext AuthResult.Error("Invalid email or password")
            }

            // Verify password
            if (!verifyPassword(credentials.password, user.passwordHash)) {
                return@withContext AuthResult.Error("Invalid email or password")
            }

            // Update last login
            val updatedUser = user.copy(lastLoginAt = System.currentTimeMillis())
            val updatedUsers = users.map { if (it.id == user.id) updatedUser else it }
            saveUsers(updatedUsers)

            // Create session
            val session = createSession(updatedUser)

            Log.d(TAG, "✅ User logged in: ${user.email}")
            AuthResult.Success(updatedUser, session)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Login failed: ${e.message}", e)
            AuthResult.Error("Login failed: ${e.message}")
        }
    }

    // ==================== SESSION MANAGEMENT ====================

    /**
     * Create new session
     */
    private fun createSession(user: User): UserSession {
        val session = UserSession(userId = user.id)
        currentUser = user
        currentSession = session
        saveSession(session)
        return session
    }

    /**
     * Get current logged-in user
     */
    fun getCurrentUser(): User? = currentUser

    /**
     * Get current session
     */
    fun getCurrentSession(): UserSession? = currentSession

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        val session = currentSession ?: return false

        // Check if session expired
        if (System.currentTimeMillis() > session.expiresAt) {
            logout()
            return false
        }

        return session.isActive && currentUser != null
    }

    /**
     * Refresh session (extend timeout)
     */
    fun refreshSession() {
        currentSession?.let { session ->
            val refreshedSession = session.copy(
                expiresAt = System.currentTimeMillis() + UserSession.SESSION_TIMEOUT
            )
            currentSession = refreshedSession
            saveSession(refreshedSession)
        }
    }

    /**
     * Logout user
     */
    fun logout() {
        currentUser = null
        currentSession = null

        // Delete session file
        if (sessionFile.exists()) {
            sessionFile.delete()
        }

        Log.d(TAG, "✅ User logged out")
    }

    // ==================== PASSWORD HASHING ====================

    /**
     * Hash password using PBKDF2
     */
    private fun hashPassword(password: String): String {
        // Generate random salt
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)

        // Hash password
        val hash = pbkdf2(password, salt, PBKDF2_ITERATIONS, HASH_BYTES)

        // Store as: iterations:salt:hash (all base64 encoded)
        val saltBase64 = android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)
        val hashBase64 = android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)

        return "$PBKDF2_ITERATIONS:$saltBase64:$hashBase64"
    }

    /**
     * Verify password against hash
     */
    private fun verifyPassword(password: String, storedHash: String): Boolean {
        try {
            val parts = storedHash.split(":")
            if (parts.size != 3) return false

            val iterations = parts[0].toInt()
            val salt = android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP)
            val hash = android.util.Base64.decode(parts[2], android.util.Base64.NO_WRAP)

            // Hash the input password with same salt
            val testHash = pbkdf2(password, salt, iterations, hash.size)

            // Compare hashes
            return hash.contentEquals(testHash)

        } catch (e: Exception) {
            Log.e(TAG, "Password verification failed: ${e.message}")
            return false
        }
    }

    /**
     * PBKDF2 key derivation
     */
    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int, bytes: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, bytes * 8)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    // ==================== VALIDATION ====================

    /**
     * Validate email format
     */
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }

    /**
     * Validate password strength
     */
    private fun validatePasswordStrength(password: String): String? {
        return when {
            password.length < 8 -> "Password must be at least 8 characters"
            !password.any { it.isUpperCase() } -> "Password must contain uppercase letter"
            !password.any { it.isLowerCase() } -> "Password must contain lowercase letter"
            !password.any { it.isDigit() } -> "Password must contain number"
            !password.any { !it.isLetterOrDigit() } -> "Password must contain special character"
            else -> null
        }
    }

    // ==================== PERSISTENCE ====================

    /**
     * Load users from encrypted file
     */
    private fun loadUsers(): List<User> {
        return try {
            if (!usersFile.exists()) return emptyList()

            val encryptedData = usersFile.readText()
            if (encryptedData.isBlank()) return emptyList()

            val decryptedJson = SecurityManager.decrypt(encryptedData)
            val type = object : TypeToken<List<User>>() {}.type
            gson.fromJson(decryptedJson, type) ?: emptyList()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load users: ${e.message}")
            emptyList()
        }
    }

    /**
     * Save users to encrypted file
     */
    private fun saveUsers(users: List<User>) {
        try {
            val json = gson.toJson(users)
            val encryptedData = SecurityManager.encrypt(json)
            usersFile.writeText(encryptedData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save users: ${e.message}", e)
        }
    }

    /**
     * Load session
     */
    private fun loadSession() {
        try {
            if (!sessionFile.exists()) return

            val encryptedData = sessionFile.readText()
            if (encryptedData.isBlank()) return

            val decryptedJson = SecurityManager.decrypt(encryptedData)
            val session: UserSession = gson.fromJson(decryptedJson, UserSession::class.java)

            // Check if session is still valid
            if (System.currentTimeMillis() < session.expiresAt) {
                currentSession = session

                // Load user
                val users = loadUsers()
                currentUser = users.find { it.id == session.userId }

                Log.d(TAG, "✅ Session restored for user: ${currentUser?.email}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load session: ${e.message}")
        }
    }

    /**
     * Save session
     */
    private fun saveSession(session: UserSession) {
        try {
            val json = gson.toJson(session)
            val encryptedData = SecurityManager.encrypt(json)
            sessionFile.writeText(encryptedData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session: ${e.message}", e)
        }
    }

    /**
     * Check if any users exist
     */
    fun hasUsers(): Boolean {
        return loadUsers().isNotEmpty()
    }
}
