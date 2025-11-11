package com.runanywhere.startup_hackathon20.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * P2P Message Repository
 * Manages offline message queue and delivery
 */
class P2PMessageRepository private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "p2p_messages_prefs",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    // All conversations (map of contact phone -> list of messages)
    private val _conversations = MutableStateFlow<Map<String, List<P2PMessage>>>(emptyMap())
    val conversations: StateFlow<Map<String, List<P2PMessage>>> = _conversations.asStateFlow()

    // Pending messages (not yet delivered)
    private val _pendingMessages = MutableStateFlow<List<P2PMessage>>(emptyList())
    val pendingMessages: StateFlow<List<P2PMessage>> = _pendingMessages.asStateFlow()

    // Unread message count per contact
    private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadCounts: StateFlow<Map<String, Int>> = _unreadCounts.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: P2PMessageRepository? = null

        fun getInstance(context: Context): P2PMessageRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: P2PMessageRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    init {
        loadConversations()
        loadPendingMessages()
        updateUnreadCounts()
    }

    /**
     * Send message to contact
     */
    fun sendMessage(
        toPhone: String,
        fromPhone: String,
        content: String,
        senderName: String
    ): P2PMessage {
        val message = P2PMessage(
            id = generateMessageId(),
            fromPhone = fromPhone,
            toPhone = toPhone,
            content = content,
            timestamp = System.currentTimeMillis(),
            isSent = true,
            isDelivered = false,
            isRead = false,
            senderName = senderName
        )

        // Add to conversations
        addMessageToConversation(toPhone, message)

        // Add to pending queue
        addToPendingQueue(message)

        // Simulate delivery after delay
        simulateMessageDelivery(message)

        return message
    }

    /**
     * Send message (simplified)
     */
    fun sendMessage(toPhone: String, content: String) {
        val currentUserPhone = "+1234567890" // In real app, get from user profile
        sendMessage(toPhone, currentUserPhone, content, "You")
    }

    /**
     * Receive message from contact
     */
    fun receiveMessage(message: P2PMessage) {
        // Mark as delivered
        val deliveredMessage = message.copy(
            isDelivered = true,
            isSent = false
        )

        // Add to conversations
        addMessageToConversation(message.fromPhone, deliveredMessage)

        // Update unread count
        incrementUnreadCount(message.fromPhone)

        // Remove from pending queue
        removeFromPendingQueue(message.id)
    }

    /**
     * Mark conversation as read
     */
    fun markAsRead(contactPhone: String) {
        // Update all messages as read
        val conversations = _conversations.value.toMutableMap()
        val messages = conversations[contactPhone]?.map { it.copy(isRead = true) }
        if (messages != null) {
            conversations[contactPhone] = messages
            _conversations.value = conversations
            saveConversations()
        }

        // Reset unread count
        val counts = _unreadCounts.value.toMutableMap()
        counts[contactPhone] = 0
        _unreadCounts.value = counts
        saveUnreadCounts()
    }

    /**
     * Get messages for specific contact
     */
    fun getMessages(contactPhone: String): List<P2PMessage> {
        return _conversations.value[contactPhone] ?: emptyList()
    }

    /**
     * Get total unread count
     */
    fun getTotalUnreadCount(): Int {
        return _unreadCounts.value.values.sum()
    }

    /**
     * Clear conversation
     */
    fun clearConversation(contactPhone: String) {
        val conversations = _conversations.value.toMutableMap()
        conversations.remove(contactPhone)
        _conversations.value = conversations
        saveConversations()

        val counts = _unreadCounts.value.toMutableMap()
        counts.remove(contactPhone)
        _unreadCounts.value = counts
        saveUnreadCounts()
    }

    // Private helper methods

    private fun addMessageToConversation(contactPhone: String, message: P2PMessage) {
        val conversations = _conversations.value.toMutableMap()
        val messages = conversations[contactPhone]?.toMutableList() ?: mutableListOf()
        messages.add(message)
        conversations[contactPhone] = messages
        _conversations.value = conversations
        saveConversations()
    }

    private fun addToPendingQueue(message: P2PMessage) {
        val pending = _pendingMessages.value.toMutableList()
        pending.add(message)
        _pendingMessages.value = pending
        savePendingMessages()
    }

    private fun removeFromPendingQueue(messageId: String) {
        val pending = _pendingMessages.value.filter { it.id != messageId }
        _pendingMessages.value = pending
        savePendingMessages()
    }

    private fun incrementUnreadCount(contactPhone: String) {
        val counts = _unreadCounts.value.toMutableMap()
        counts[contactPhone] = (counts[contactPhone] ?: 0) + 1
        _unreadCounts.value = counts
        saveUnreadCounts()
    }

    private fun updateUnreadCounts() {
        val counts = mutableMapOf<String, Int>()
        _conversations.value.forEach { (phone, messages) ->
            counts[phone] = messages.count { !it.isRead && !it.isSent }
        }
        _unreadCounts.value = counts
        saveUnreadCounts()
    }

    private fun simulateMessageDelivery(message: P2PMessage) {
        // In real implementation, this would use WiFi Direct/Bluetooth
        // For now, we simulate with a delay
        kotlinx.coroutines.GlobalScope.launch {
            kotlinx.coroutines.delay(3000) // 3 second delivery delay

            // Mark as delivered
            val conversations = _conversations.value.toMutableMap()
            val messages = conversations[message.toPhone]?.map {
                if (it.id == message.id) it.copy(isDelivered = true) else it
            }
            if (messages != null) {
                conversations[message.toPhone] = messages
                _conversations.value = conversations
                saveConversations()
            }

            // Remove from pending
            removeFromPendingQueue(message.id)
        }
    }

    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    // Persistence methods

    private fun loadConversations() {
        val json = prefs.getString("conversations", null) ?: return
        try {
            val type = object : TypeToken<Map<String, List<P2PMessage>>>() {}.type
            _conversations.value = gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveConversations() {
        val json = gson.toJson(_conversations.value)
        prefs.edit().putString("conversations", json).apply()
    }

    private fun loadPendingMessages() {
        val json = prefs.getString("pending_messages", null) ?: return
        try {
            val type = object : TypeToken<List<P2PMessage>>() {}.type
            _pendingMessages.value = gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun savePendingMessages() {
        val json = gson.toJson(_pendingMessages.value)
        prefs.edit().putString("pending_messages", json).apply()
    }

    private fun saveUnreadCounts() {
        val json = gson.toJson(_unreadCounts.value)
        prefs.edit().putString("unread_counts", json).apply()
    }
}

/**
 * P2P Message data class
 */
data class P2PMessage(
    val id: String,
    val fromPhone: String,
    val toPhone: String,
    val content: String,
    val timestamp: Long,
    val isSent: Boolean, // true if sent by current user
    val isDelivered: Boolean = false,
    val isRead: Boolean = false,
    val senderName: String = ""
)
