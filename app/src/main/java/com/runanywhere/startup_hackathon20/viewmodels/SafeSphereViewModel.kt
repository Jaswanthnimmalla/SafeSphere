package com.runanywhere.startup_hackathon20.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.startup_hackathon20.data.*
import com.runanywhere.startup_hackathon20.security.SecurityManager
import kotlinx.coroutines.Dispatchers
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

    // Current screen
    private val _currentScreen = MutableStateFlow(SafeSphereScreen.ONBOARDING)
    val currentScreen: StateFlow<SafeSphereScreen> = _currentScreen.asStateFlow()

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
            )
        )

        val newThreat = threats.random()
        _threatEvents.value = listOf(newThreat) + _threatEvents.value
        showMessage("🛡️ Threat simulated: ${newThreat.type.displayName}")
    }

    /**
     * Clear threat events
     */
    fun clearThreats() {
        _threatEvents.value = emptyList()
    }

    // ==================== NAVIGATION ====================

    fun navigateToScreen(screen: SafeSphereScreen) {
        _currentScreen.value = screen
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
    ONBOARDING,
    DASHBOARD,
    PRIVACY_VAULT,
    AI_CHAT,
    DATA_MAP,
    THREAT_SIMULATION,
    SETTINGS,
    MODELS
}
