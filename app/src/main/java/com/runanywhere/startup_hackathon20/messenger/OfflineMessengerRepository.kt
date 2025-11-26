package com.runanywhere.startup_hackathon20.messenger

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

/**
 * Offline Messenger Repository - Complete business logic
 *
 * Features:
 * - Automatic contact loading
 * - Real-time messaging with status tracking
 * - Unread badge management (auto-increment/reset)
 * - Media file transfer with compression
 * - Local storage persistence
 * - Bluetooth communication
 */
class OfflineMessengerRepository private constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = context.getSharedPreferences("offline_messenger", Context.MODE_PRIVATE)

    // Bluetooth service
    private val bluetoothService = BluetoothService(context)

    // State flows
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private val _contacts = MutableStateFlow<List<MessengerContact>>(emptyList())
    val contacts: StateFlow<List<MessengerContact>> = _contacts

    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages

    private val _connectedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val connectedDevices: StateFlow<List<BluetoothDeviceInfo>> = _connectedDevices

    // Current open chat (for badge management)
    private var currentOpenChatId: String? = null

    init {
        setupBluetoothCallbacks()
        loadFromStorage()
        startBluetoothService()
    }

    /**
     * Setup Bluetooth callbacks
     */
    private fun setupBluetoothCallbacks() {
        bluetoothService.onMessageReceived = { packet ->
            handleIncomingPacket(packet)
        }

        bluetoothService.onConnectionEstablished = { address ->
            updateConnectedDevices()
        }

        bluetoothService.onConnectionLost = { address ->
            updateConnectedDevices()
        }
    }

    /**
     * Start Bluetooth service
     */
    fun startBluetoothService() {
        bluetoothService.startService()
    }

    /**
     * Handle incoming Bluetooth packet
     */
    private fun handleIncomingPacket(packet: BluetoothPacket) {
        scope.launch {
            when (packet.type) {
                PacketType.TEXT_MESSAGE -> handleTextMessage(packet)
                PacketType.FILE_METADATA -> handleFileMetadata(packet)
                PacketType.FILE_CHUNK -> handleFileChunk(packet)
                PacketType.DELIVERY_RECEIPT -> handleDeliveryReceipt(packet)
                PacketType.READ_RECEIPT -> handleReadReceipt(packet)
                PacketType.TYPING_INDICATOR -> handleTypingIndicator(packet)
                else -> {}
            }
        }
    }

    /**
     * Handle incoming text message
     */
    private suspend fun handleTextMessage(packet: BluetoothPacket) {
        val message = ChatMessage(
            id = packet.messageId,
            conversationId = normalizePhone(packet.senderId),
            content = packet.payload,
            timestamp = packet.timestamp,
            senderId = packet.senderId,
            receiverId = packet.receiverId,
            isSent = false,
            status = MessageStatus.DELIVERED,
            messageType = MessageType.TEXT
        )

        addMessage(message)

        // Send delivery receipt
        sendDeliveryReceipt(packet.senderId, packet.messageId)

        // Update unread count if chat is not open
        if (currentOpenChatId != normalizePhone(packet.senderId)) {
            incrementUnreadCount(normalizePhone(packet.senderId))
        } else {
            // Auto-mark as read if chat is open
            sendReadReceipt(packet.senderId, packet.messageId)
            updateMessageStatus(
                normalizePhone(packet.senderId),
                packet.messageId,
                MessageStatus.READ
            )
        }
    }

    /**
     * Handle delivery receipt
     */
    private suspend fun handleDeliveryReceipt(packet: BluetoothPacket) {
        updateMessageStatus(
            normalizePhone(packet.senderId),
            packet.messageId,
            MessageStatus.DELIVERED
        )
    }

    /**
     * Handle read receipt
     */
    private suspend fun handleReadReceipt(packet: BluetoothPacket) {
        updateMessageStatus(
            normalizePhone(packet.senderId),
            packet.messageId,
            MessageStatus.READ
        )
    }

    /**
     * Handle typing indicator
     */
    private fun handleTypingIndicator(packet: BluetoothPacket) {
        // TODO: Implement typing indicator UI update
    }

    /**
     * Handle file metadata
     */
    private suspend fun handleFileMetadata(packet: BluetoothPacket) {
        // TODO: Implement file metadata handling
        Log.d(TAG, "File metadata received: ${packet.fileMetadata?.fileName}")
    }

    /**
     * Handle file chunk
     */
    private suspend fun handleFileChunk(packet: BluetoothPacket) {
        // TODO: Implement file chunk assembly
        Log.d(TAG, "File chunk received: ${packet.chunkIndex}/${packet.totalChunks}")
    }

    /**
     * Send a text message
     */
    suspend fun sendMessage(conversationId: String, content: String): Result<ChatMessage> {
        return withContext(Dispatchers.IO) {
            try {
                val myPhone = getUserPhone()
                val message = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    senderId = myPhone,
                    receiverId = conversationId,
                    isSent = true,
                    status = MessageStatus.SENDING,
                    messageType = MessageType.TEXT
                )

                // Add to local storage
                addMessage(message)

                // Send via Bluetooth
                val packet = BluetoothPacket(
                    type = PacketType.TEXT_MESSAGE,
                    senderId = myPhone,
                    receiverId = conversationId,
                    messageId = message.id,
                    timestamp = message.timestamp,
                    payload = content
                )

                val sent = bluetoothService.sendPacket(packet)

                // Update status
                val newStatus = if (sent) MessageStatus.SENT else MessageStatus.FAILED
                updateMessageStatus(conversationId, message.id, newStatus)

                Result.success(message)
            } catch (e: Exception) {
                Log.e(TAG, "Send message error: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * Send delivery receipt
     */
    private fun sendDeliveryReceipt(toPhone: String, messageId: String) {
        val packet = BluetoothPacket(
            type = PacketType.DELIVERY_RECEIPT,
            senderId = getUserPhone(),
            receiverId = toPhone,
            messageId = messageId,
            timestamp = System.currentTimeMillis(),
            payload = "delivered"
        )
        bluetoothService.sendPacket(packet)
    }

    /**
     * Send read receipt
     */
    private fun sendReadReceipt(toPhone: String, messageId: String) {
        val packet = BluetoothPacket(
            type = PacketType.READ_RECEIPT,
            senderId = getUserPhone(),
            receiverId = toPhone,
            messageId = messageId,
            timestamp = System.currentTimeMillis(),
            payload = "read"
        )
        bluetoothService.sendPacket(packet)
    }

    /**
     * Mark conversation as read (resets unread badge)
     */
    suspend fun markConversationAsRead(conversationId: String) {
        currentOpenChatId = conversationId

        // Reset unread count
        val updatedConversations = _conversations.value.map { conv ->
            if (conv.id == conversationId) {
                conv.copy(unreadCount = 0)
            } else {
                conv
            }
        }
        _conversations.value = updatedConversations

        // Send read receipts for all unread messages
        val messages = _messages.value[conversationId] ?: emptyList()
        messages.filter { !it.isSent && it.status != MessageStatus.READ }.forEach { msg ->
            sendReadReceipt(msg.senderId, msg.id)
            updateMessageStatus(conversationId, msg.id, MessageStatus.READ)
        }

        saveToStorage()
    }

    /**
     * Mark conversation as closed (enable badge increment)
     */
    fun markConversationAsClosed(conversationId: String) {
        if (currentOpenChatId == conversationId) {
            currentOpenChatId = null
        }
    }

    /**
     * Increment unread count for conversation
     */
    private fun incrementUnreadCount(conversationId: String) {
        val updatedConversations = _conversations.value.map { conv ->
            if (conv.id == conversationId) {
                conv.copy(unreadCount = conv.unreadCount + 1)
            } else {
                conv
            }
        }
        _conversations.value = updatedConversations
        saveToStorage()
    }

    /**
     * Add message to conversation
     */
    private fun addMessage(message: ChatMessage) {
        val currentMessages =
            _messages.value[message.conversationId]?.toMutableList() ?: mutableListOf()
        currentMessages.add(message)

        _messages.value = _messages.value.toMutableMap().apply {
            put(message.conversationId, currentMessages)
        }

        updateConversationList()
        saveToStorage()
    }

    /**
     * Update message status
     */
    private fun updateMessageStatus(
        conversationId: String,
        messageId: String,
        status: MessageStatus
    ) {
        val messages = _messages.value[conversationId]?.map { msg ->
            if (msg.id == messageId) {
                msg.copy(status = status)
            } else {
                msg
            }
        } ?: return

        _messages.value = _messages.value.toMutableMap().apply {
            put(conversationId, messages)
        }

        updateConversationList()
        saveToStorage()
    }

    /**
     * Update conversation list
     */
    private fun updateConversationList() {
        val conversations = mutableListOf<Conversation>()

        for ((conversationId, messages) in _messages.value) {
            if (messages.isEmpty()) continue

            val contact = _contacts.value.find { it.normalizedPhone == conversationId }
            val lastMessage = messages.lastOrNull()
            val unreadCount = messages.count { !it.isSent && it.status != MessageStatus.READ }

            val existingConv = _conversations.value.find { it.id == conversationId }

            conversations.add(
                Conversation(
                    id = conversationId,
                    contactName = contact?.name ?: conversationId,
                    contactPhone = conversationId,
                    contactAvatar = contact?.avatar,
                    lastMessage = lastMessage,
                    unreadCount = existingConv?.unreadCount ?: unreadCount,
                    lastActivity = lastMessage?.timestamp ?: 0L,
                    isPinned = existingConv?.isPinned ?: false,
                    isMuted = existingConv?.isMuted ?: false,
                    isArchived = existingConv?.isArchived ?: false
                )
            )
        }

        _conversations.value = conversations.sortedByDescending { it.lastActivity }
    }

    /**
     * Load contacts from phone
     */
    suspend fun loadContactsFromPhone() = withContext(Dispatchers.IO) {
        try {
            val contactsList = mutableListOf<MessengerContact>()

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex =
                    it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val id = it.getString(idIndex) ?: ""
                    val name = it.getString(nameIndex) ?: "Unknown"
                    val phone = it.getString(numberIndex) ?: ""

                    if (phone.isNotEmpty()) {
                        contactsList.add(
                            MessengerContact(
                                id = id,
                                name = name,
                                phoneNumber = phone,
                                normalizedPhone = normalizePhone(phone)
                            )
                        )
                    }
                }
            }

            _contacts.value = contactsList.distinctBy { it.normalizedPhone }
            Log.d(TAG, "Loaded ${_contacts.value.size} contacts")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load contacts: ${e.message}")
        }
    }

    /**
     * Get or create conversation
     */
    fun getOrCreateConversation(contactPhone: String): Conversation {
        val normalizedPhone = normalizePhone(contactPhone)
        val existing = _conversations.value.find { it.id == normalizedPhone }

        if (existing != null) {
            return existing
        }

        // Create new conversation
        val contact = _contacts.value.find { it.normalizedPhone == normalizedPhone }
        val newConversation = Conversation(
            id = normalizedPhone,
            contactName = contact?.name ?: normalizedPhone,
            contactPhone = normalizedPhone,
            contactAvatar = contact?.avatar,
            lastMessage = null,
            unreadCount = 0,
            lastActivity = System.currentTimeMillis()
        )

        _conversations.value = _conversations.value + newConversation
        saveToStorage()

        return newConversation
    }

    /**
     * Get user's phone number
     */
    private fun getUserPhone(): String {
        var phone = prefs.getString("user_phone", null)
        if (phone == null) {
            // Auto-generate using device ID
            phone = "USER_${
                android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
            }"
            prefs.edit().putString("user_phone", phone).apply()
        }
        return phone
    }

    /**
     * Normalize phone number
     */
    private fun normalizePhone(phone: String): String {
        val cleaned = phone.replace(Regex("[^0-9+]"), "")
        val digits = cleaned.replace("+", "")
        return if (digits.length > 10) {
            digits.takeLast(10)
        } else {
            digits
        }
    }

    /**
     * Update connected devices
     */
    private fun updateConnectedDevices() {
        scope.launch {
            _connectedDevices.value = bluetoothService.connectedDevices.value
        }
    }

    /**
     * Get connection count
     */
    fun getConnectionCount(): Int = bluetoothService.getConnectionCount()

    /**
     * Save to storage
     */
    private fun saveToStorage() {
        scope.launch {
            try {
                // Save messages
                val messagesJson = JSONObject()
                for ((conversationId, messages) in _messages.value) {
                    val messagesArray = JSONArray()
                    messages.forEach { msg ->
                        messagesArray.put(JSONObject().apply {
                            put("id", msg.id)
                            put("conversationId", msg.conversationId)
                            put("content", msg.content)
                            put("timestamp", msg.timestamp)
                            put("senderId", msg.senderId)
                            put("receiverId", msg.receiverId)
                            put("isSent", msg.isSent)
                            put("status", msg.status.name)
                            put("messageType", msg.messageType.name)
                        })
                    }
                    messagesJson.put(conversationId, messagesArray)
                }
                prefs.edit().putString("messages", messagesJson.toString()).apply()

                // Save conversations
                val conversationsArray = JSONArray()
                _conversations.value.forEach { conv ->
                    conversationsArray.put(JSONObject().apply {
                        put("id", conv.id)
                        put("unreadCount", conv.unreadCount)
                        put("isPinned", conv.isPinned)
                        put("isMuted", conv.isMuted)
                        put("isArchived", conv.isArchived)
                    })
                }
                prefs.edit().putString("conversations", conversationsArray.toString()).apply()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to save: ${e.message}")
            }
        }
    }

    /**
     * Load from storage
     */
    private fun loadFromStorage() {
        scope.launch {
            try {
                // Load messages
                val messagesJson = prefs.getString("messages", null)
                if (messagesJson != null) {
                    val json = JSONObject(messagesJson)
                    val messagesMap = mutableMapOf<String, List<ChatMessage>>()

                    json.keys().forEach { conversationId ->
                        val messagesArray = json.getJSONArray(conversationId)
                        val messages = mutableListOf<ChatMessage>()

                        for (i in 0 until messagesArray.length()) {
                            val msgJson = messagesArray.getJSONObject(i)
                            messages.add(
                                ChatMessage(
                                    id = msgJson.getString("id"),
                                    conversationId = msgJson.getString("conversationId"),
                                    content = msgJson.getString("content"),
                                    timestamp = msgJson.getLong("timestamp"),
                                    senderId = msgJson.getString("senderId"),
                                    receiverId = msgJson.getString("receiverId"),
                                    isSent = msgJson.getBoolean("isSent"),
                                    status = MessageStatus.valueOf(msgJson.getString("status")),
                                    messageType = MessageType.valueOf(msgJson.getString("messageType"))
                                )
                            )
                        }

                        messagesMap[conversationId] = messages
                    }

                    _messages.value = messagesMap
                }

                updateConversationList()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load: ${e.message}")
            }
        }
    }

    /**
     * Cleanup
     */
    fun cleanup() {
        bluetoothService.stopService()
        scope.cancel()
    }

    companion object {
        private const val TAG = "OfflineMessengerRepo"

        @Volatile
        private var INSTANCE: OfflineMessengerRepository? = null

        fun getInstance(context: Context): OfflineMessengerRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OfflineMessengerRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
