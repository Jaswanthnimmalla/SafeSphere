package com.runanywhere.startup_hackathon20.viewmodels

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.startup_hackathon20.data.*
import com.runanywhere.startup_hackathon20.security.AuthenticationManager
import com.runanywhere.startup_hackathon20.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * SafeSphereViewModel - Main state management for SafeSphere app
 *
 * Responsibilities:
 * - Privacy Vault management
 * - Offline AI chat
 * - Data Map visualization
 * - Threat simulation
 * - Security settings
 */
class SafeSphereViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PrivacyVaultRepository.getInstance(application)
    private val authManager = AuthenticationManager.getInstance(application)

    // Vault items state
    val vaultItems = repository.vaultItems.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    // Storage statistics
    private val _storageStats = MutableStateFlow(StorageStats(0, 0, 0, emptyMap(), 100))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    // Chat messages
    private val _chatMessages = MutableStateFlow<List<SafeSphereChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<SafeSphereChatMessage>> = _chatMessages.asStateFlow()

    // Chat streaming state
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Threat events
    private val _threatEvents = MutableStateFlow<List<ThreatEvent>>(emptyList())
    val threatEvents: StateFlow<List<ThreatEvent>> = _threatEvents.asStateFlow()

    // Real-time monitoring state
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    // Network status
    private val _networkStatus = MutableStateFlow("Checking...")
    val networkStatus: StateFlow<String> = _networkStatus.asStateFlow()

    // Threats blocked count
    private val _threatsBlocked = MutableStateFlow(0)
    val threatsBlocked: StateFlow<Int> = _threatsBlocked.asStateFlow()

    // Current screen - start with LOGIN or DASHBOARD based on auth status
    private val _currentScreen = MutableStateFlow(
        if (authManager.isLoggedIn()) SafeSphereScreen.DASHBOARD
        else SafeSphereScreen.LOGIN
    )
    val currentScreen: StateFlow<SafeSphereScreen> = _currentScreen.asStateFlow()

    // Navigation stack for back gesture support
    private val navigationStack = mutableListOf<SafeSphereScreen>()

    // Current user
    private val _currentUser = MutableStateFlow(authManager.getCurrentUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Online/offline status (always offline for SafeSphere)
    private val _isOfflineMode = MutableStateFlow(true)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    // UI messages
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    companion object {
        private const val TAG = "SafeSphereVM"
    }

    init {
        updateStorageStats()
        initializeWelcomeMessage()
        startThreatSimulation()
        startRealTimeMonitoring()
    }

    /**
     * Initialize welcome message in AI chat
     */
    private fun initializeWelcomeMessage() {
        val welcomeMessage = SafeSphereChatMessage(
            content = "🔐 Welcome to SafeSphere's Privacy Advisor!\n\n" +
                    "I'm your offline AI assistant running entirely on your device. " +
                    "I can help you with:\n\n" +
                    "• Understanding data privacy\n" +
                    "• Encryption best practices\n" +
                    "• Threat prevention tips\n" +
                    "• Secure storage advice\n\n" +
                    "Your conversations never leave your device. What would you like to know?",
            isUser = false,
            timestamp = System.currentTimeMillis()
        )
        _chatMessages.value = listOf(welcomeMessage)
    }

    // ==================== VAULT OPERATIONS ====================

    /**
     * Add new item to Privacy Vault
     */
    fun addVaultItem(title: String, content: String, category: VaultCategory) {
        viewModelScope.launch {
            val result = repository.addItem(title, content, category)
            if (result.isSuccess) {
                showMessage("✅ Item encrypted and saved to vault")
                updateStorageStats()
            } else {
                showMessage("❌ Failed to save item: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Get decrypted vault item
     */
    fun getDecryptedItem(id: String, onResult: (Result<DecryptedVaultItem>) -> Unit) {
        viewModelScope.launch {
            val result = repository.getDecryptedItem(id)
            onResult(result)
        }
    }

    /**
     * Update vault item
     */
    fun updateVaultItem(id: String, title: String, content: String, category: VaultCategory) {
        viewModelScope.launch {
            val result = repository.updateItem(id, title, content, category)
            if (result.isSuccess) {
                showMessage("✅ Item updated successfully")
                updateStorageStats()
            } else {
                showMessage("❌ Failed to update item")
            }
        }
    }

    /**
     * Delete vault item
     */
    fun deleteVaultItem(id: String) {
        viewModelScope.launch {
            val result = repository.deleteItem(id)
            if (result.isSuccess) {
                showMessage("✅ Item deleted from vault")
                updateStorageStats()
            } else {
                showMessage("❌ Failed to delete item")
            }
        }
    }

    /**
     * Initialize demo data
     */
    fun initializeDemoData() {
        viewModelScope.launch {
            repository.initializeDemoData()
            updateStorageStats()
            showMessage("✅ Demo data loaded - ${vaultItems.value.size} encrypted items")
        }
    }

    /**
     * Clear all vault data
     */
    fun clearVault() {
        viewModelScope.launch {
            val result = repository.clearVault()
            if (result.isSuccess) {
                updateStorageStats()
                showMessage("✅ Vault cleared")
            }
        }
    }

    // ==================== AI CHAT OPERATIONS ====================

    /**
     * Send message to offline AI
     */
    fun sendChatMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Add user message
                val userChatMessage = SafeSphereChatMessage(
                    content = userMessage,
                    isUser = true,
                    timestamp = System.currentTimeMillis()
                )
                _chatMessages.value = _chatMessages.value + userChatMessage

                // Start generation
                _isGenerating.value = true

                // Privacy-focused system prompt
                val systemPrompt = """You are a privacy and security advisor for SafeSphere, 
                    |an offline-first privacy protection app. You help users understand:
                    |- Data encryption and privacy
                    |- Offline-first security benefits
                    |- Threat prevention
                    |- Best practices for protecting personal data
                    |Keep responses concise, helpful, and focused on privacy education.""".trimMargin()

                val fullPrompt = "$systemPrompt\n\nUser: $userMessage\n\nAssistant:"

                // Create AI response message
                val aiMessageId = System.currentTimeMillis().toString()
                var aiResponse = ""

                val aiMessage = SafeSphereChatMessage(
                    id = aiMessageId,
                    content = "",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                _chatMessages.value = _chatMessages.value + aiMessage

                // Stream response from local AI model using RunAnywhere SDK
                RunAnywhere.generateStream(fullPrompt).collect { token ->
                    aiResponse += token
                    // Update message with streaming content
                    _chatMessages.value = _chatMessages.value.map {
                        if (it.id == aiMessageId) it.copy(content = aiResponse.trim())
                        else it
                    }
                }

                _isGenerating.value = false

            } catch (e: Exception) {
                Log.e(TAG, "Chat error: ${e.message}", e)
                _isGenerating.value = false

                val errorMessage = SafeSphereChatMessage(
                    content = "⚠️ AI model not loaded. Please download a model first:\n" +
                            "1. Tap 'Models' tab\n" +
                            "2. Download 'SafeSphere Privacy Advisor'\n" +
                            "3. Tap 'Load' on the model\n" +
                            "4. Return here to chat",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                _chatMessages.value = _chatMessages.value + errorMessage
            }
        }
    }

    /**
     * Clear chat history
     */
    fun clearChat() {
        _chatMessages.value = emptyList()
        initializeWelcomeMessage()
    }

    // ==================== DATA MAP & STATISTICS ====================

    /**
     * Update storage statistics
     */
    private fun updateStorageStats() {
        _storageStats.value = repository.getStorageStats()
    }

    /**
     * Get storage stats manually
     */
    fun refreshStorageStats() {
        updateStorageStats()
    }

    // ==================== THREAT SIMULATION ====================

    /**
     * Start threat simulation for educational purposes
     */
    private fun startThreatSimulation() {
        // Initialize with some educational threat scenarios
        val initialThreats = listOf(
            ThreatEvent(
                type = ThreatType.CLOUD_EXPOSURE,
                severity = ThreatSeverity.HIGH,
                description = "Cloud storage breach: 500M user records exposed. SafeSphere keeps your data offline and safe.",
                mitigated = true
            ),
            ThreatEvent(
                type = ThreatType.MAN_IN_MIDDLE,
                severity = ThreatSeverity.MEDIUM,
                description = "Public WiFi interception attempt detected. Your data is encrypted locally.",
                mitigated = true
            )
        )
        _threatEvents.value = initialThreats
        _threatsBlocked.value = initialThreats.size
    }

    /**
     * Start real-time security monitoring
     */
    private fun startRealTimeMonitoring() {
        viewModelScope.launch {
            _isMonitoring.value = true

            while (_isMonitoring.value) {
                // Monitor network status
                checkNetworkStatus()

                // Check for potential threats
                checkSystemSecurity()

                // Update stats
                _threatsBlocked.value = _threatEvents.value.count { it.mitigated }

                // Wait 5 seconds before next check
                delay(5000)
            }
        }
    }

    /**
     * Check network status in real-time
     */
    private fun checkNetworkStatus() {
        try {
            val connectivityManager = getApplication<Application>()
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val network = connectivityManager.activeNetwork
            val capabilities = network?.let {
                connectivityManager.getNetworkCapabilities(it)
            }

            when {
                capabilities == null -> {
                    _networkStatus.value = "✅ Offline (Secure)"
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    _networkStatus.value = "⚠️ WiFi Detected"
                    // Auto-generate threat about public WiFi
                    addRealTimeThreat(
                        type = ThreatType.MAN_IN_MIDDLE,
                        severity = ThreatSeverity.MEDIUM,
                        description = "WiFi connection detected. Public WiFi can expose data. SafeSphere encryption protects you."
                    )
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    _networkStatus.value = "⚠️ Mobile Data Active"
                    addRealTimeThreat(
                        type = ThreatType.CLOUD_EXPOSURE,
                        severity = ThreatSeverity.LOW,
                        description = "Network connection active. Cloud services vulnerable to breaches. Your data stays offline."
                    )
                }

                else -> {
                    _networkStatus.value = "✅ Offline Mode"
                }
            }
        } catch (e: Exception) {
            _networkStatus.value = "✅ Offline (Secure)"
        }
    }

    /**
     * Check system security in real-time
     */
    private fun checkSystemSecurity() {
        try {
            val securityStatus = SecurityManager.getSecurityStatus()

            // Check encryption status
            if (!securityStatus.aesKeyPresent) {
                addRealTimeThreat(
                    type = ThreatType.DATA_BREACH,
                    severity = ThreatSeverity.CRITICAL,
                    description = "Encryption key not found. Initializing AES-256 encryption for data protection."
                )
            }

            // Check hardware security
            if (!securityStatus.hardwareBacked) {
                addRealTimeThreat(
                    type = ThreatType.UNAUTHORIZED_ACCESS,
                    severity = ThreatSeverity.MEDIUM,
                    description = "Hardware security unavailable. Using software encryption. Still secure but not hardware-backed."
                )
            }

            // Simulate random threats based on time
            val random = kotlin.random.Random(System.currentTimeMillis())
            if (random.nextInt(100) < 10) { // 10% chance every check
                val randomThreats = listOf(
                    ThreatEvent(
                        type = ThreatType.PHISHING,
                        severity = ThreatSeverity.LOW,
                        description = "Phishing attempt detected from fake cloud service. Offline mode prevents connection.",
                        mitigated = true
                    ),
                    ThreatEvent(
                        type = ThreatType.MALWARE,
                        severity = ThreatSeverity.HIGH,
                        description = "Malicious app scan detected. SafeSphere data isolated in encrypted vault.",
                        mitigated = true
                    ),
                    ThreatEvent(
                        type = ThreatType.UNAUTHORIZED_ACCESS,
                        severity = ThreatSeverity.HIGH,
                        description = "App permission scan detected. Your data remains encrypted and inaccessible.",
                        mitigated = true
                    )
                )

                _threatEvents.value =
                    listOf(randomThreats.random()) + _threatEvents.value.take(19) // Keep last 20
            }
        } catch (e: Exception) {
            Log.e(TAG, "Security check error: ${e.message}")
        }
    }

    /**
     * Add real-time threat (check for duplicates)
     */
    private fun addRealTimeThreat(type: ThreatType, severity: ThreatSeverity, description: String) {
        // Check if similar threat already exists in last 5 events
        val recentSimilar = _threatEvents.value.take(5).any {
            it.type == type && it.description.startsWith(description.take(20))
        }

        if (!recentSimilar) {
            val newThreat = ThreatEvent(
                type = type,
                severity = severity,
                description = description,
                mitigated = true
            )
            _threatEvents.value = listOf(newThreat) + _threatEvents.value.take(19) // Keep last 20
        }
    }

    /**
     * Simulate new threat for educational purposes
     */
    fun simulateThreat() {
        val threats = listOf(
            ThreatEvent(
                type = ThreatType.DATA_BREACH,
                severity = ThreatSeverity.CRITICAL,
                description = "Simulated: Third-party app attempts to access unencrypted data. SafeSphere encryption blocked access.",
                mitigated = true
            ),
            ThreatEvent(
                type = ThreatType.PHISHING,
                severity = ThreatSeverity.MEDIUM,
                description = "Simulated: Phishing attempt via fake cloud sync request. Offline mode prevents connection.",
                mitigated = true
            ),
            ThreatEvent(
                type = ThreatType.UNAUTHORIZED_ACCESS,
                severity = ThreatSeverity.HIGH,
                description = "Simulated: Unauthorized device access attempt. Hardware-backed encryption prevented data access.",
                mitigated = true
            ),
            ThreatEvent(
                type = ThreatType.CLOUD_EXPOSURE,
                severity = ThreatSeverity.CRITICAL,
                description = "Simulated: Cloud provider breach exposing 2.4B records. Your offline data is completely safe.",
                mitigated = true
            ),
            ThreatEvent(
                type = ThreatType.MALWARE,
                severity = ThreatSeverity.HIGH,
                description = "Simulated: Malware scanning for credential files. SafeSphere vault is encrypted and protected.",
                mitigated = true
            )
        )

        val newThreat = threats.random()
        _threatEvents.value = listOf(newThreat) + _threatEvents.value.take(19) // Keep last 20
        showMessage("🛡️ Threat simulated and blocked!")
    }

    /**
     * Toggle real-time monitoring
     */
    fun toggleMonitoring() {
        _isMonitoring.value = !_isMonitoring.value
        if (_isMonitoring.value) {
            startRealTimeMonitoring()
            showMessage("🔍 Real-time monitoring enabled")
        } else {
            showMessage("⏸️ Monitoring paused")
        }
    }

    /**
     * Clear threat events
     */
    fun clearThreats() {
        _threatEvents.value = emptyList()
        _threatsBlocked.value = 0
    }

    // ==================== NAVIGATION ====================

    /**
     * Navigate to a specific screen
     */
    fun navigateToScreen(screen: SafeSphereScreen) {
        // Don't add to stack if it's the same screen
        if (_currentScreen.value != screen) {
            // Add current screen to stack before navigating
            navigationStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    /**
     * Navigate back to previous screen using back gesture or button
     * Returns true if navigation happened, false if at root screen
     */
    fun navigateBack(): Boolean {
        return when {
            // If stack is not empty, pop and navigate
            navigationStack.isNotEmpty() -> {
                val previousScreen = navigationStack.removeAt(navigationStack.lastIndex)
                _currentScreen.value = previousScreen
                true
            }
            // If on secondary screens, go back to dashboard
            _currentScreen.value !in listOf(
                SafeSphereScreen.DASHBOARD,
                SafeSphereScreen.LOGIN,
                SafeSphereScreen.REGISTER,
                SafeSphereScreen.ONBOARDING
            ) -> {
                _currentScreen.value = SafeSphereScreen.DASHBOARD
                true
            }
            // At root screen (Dashboard or Login), can't go back
            else -> false
        }
    }

    /**
     * Clear navigation stack (used after logout or login)
     */
    private fun clearNavigationStack() {
        navigationStack.clear()
    }

    // ==================== AUTHENTICATION ====================

    /**
     * Login user
     */
    suspend fun login(credentials: LoginCredentials): AuthResult {
        return authManager.login(credentials).also { result ->
            if (result is AuthResult.Success) {
                _currentUser.value = result.user
                // DON'T navigate here - let the UI show save dialog first
                // _currentScreen.value = SafeSphereScreen.DASHBOARD  // REMOVED
                clearNavigationStack()
                showMessage("✅ Welcome back, ${result.user.name}!")
            }
        }
    }

    /**
     * Register new user
     */
    suspend fun register(data: RegistrationData): AuthResult {
        return authManager.register(data).also { result ->
            if (result is AuthResult.Success) {
                _currentUser.value = result.user
                // DON'T navigate here - let the UI show save dialog first
                // _currentScreen.value = SafeSphereScreen.ONBOARDING  // REMOVED
                clearNavigationStack()
                showMessage("✅ Welcome to SafeSphere, ${result.user.name}!")
            }
        }
    }

    /**
     * Logout user
     */
    fun logout() {
        authManager.logout()
        _currentUser.value = null
        _currentScreen.value = SafeSphereScreen.LOGIN
        clearNavigationStack()
        showMessage("👋 Logged out successfully")
    }

    // ==================== UI MESSAGES ====================

    private fun showMessage(message: String) {
        _uiMessage.value = message
    }

    fun clearMessage() {
        _uiMessage.value = null
    }
}

/**
 * Chat message data class
 */
data class SafeSphereChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long
)

/**
 * App screens
 */
enum class SafeSphereScreen {
    LOGIN,
    REGISTER,
    ONBOARDING,
    DASHBOARD,
    PRIVACY_VAULT,
    PASSWORDS,
    AI_CHAT,
    DATA_MAP,
    THREAT_SIMULATION,
    SETTINGS,
    MODELS,
    NOTIFICATIONS,
    PASSWORD_HEALTH
}
