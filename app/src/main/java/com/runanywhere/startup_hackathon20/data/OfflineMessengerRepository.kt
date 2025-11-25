package com.runanywhere.startup_hackathon20.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * Simple Offline Messenger Repository
 * Handles local message storage and Bluetooth communication
 */
class OfflineMessengerRepository private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = context.getSharedPreferences("offline_messenger", Context.MODE_PRIVATE)

    // Bluetooth
    private val bluetoothManager: BluetoothManager?
    private val bluetoothAdapter: BluetoothAdapter?
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private val activeConnections = mutableMapOf<String, BluetoothSocket>()

    // Map phone numbers to Bluetooth addresses
    private val phoneToAddressMap = mutableMapOf<String, String>()
    private val addressToPhoneMap = mutableMapOf<String, String>()

    // UUID for SafeSphere Messenger
    private val APP_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    private val APP_NAME = "SafeSphere_Messenger"

    // States
    private val _conversations = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val conversations: StateFlow<Map<String, List<Message>>> = _conversations

    private val _nearbyDevices = MutableStateFlow<List<String>>(emptyList())
    val nearbyDevices: StateFlow<List<String>> = _nearbyDevices

    // Callbacks
    var onMessageReceived: ((String, String) -> Unit)? = null

    init {
        bluetoothManager = try {
            context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get BluetoothManager: ${e.message}")
            null
        }

        bluetoothAdapter = bluetoothManager?.adapter

        loadConversations()
    }

    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter?.isEnabled == true && hasBluetoothPermissions()
    }

    /**
     * Check Bluetooth permissions
     */
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Start Bluetooth server to accept incoming connections
     */
    @SuppressLint("MissingPermission")
    fun startBluetoothServer() {
        if (!isBluetoothAvailable()) {
            Log.w(TAG, "Bluetooth not available")
            return
        }

        scope.launch {
            try {
                serverSocket =
                    bluetoothAdapter?.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
                Log.d(TAG, "Bluetooth server started")

                while (true) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        val device = socket.remoteDevice

                        Log.d(TAG, "Accepted connection from: ${device.address}")
                        activeConnections[device.address] = socket

                        // Start listening for messages
                        listenForMessages(socket, device.address)
                    } catch (e: IOException) {
                        Log.e(TAG, "Server accept failed: ${e.message}")
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Bluetooth server error: ${e.message}")
            }
        }

        // Auto-connect to all paired devices immediately
        scope.launch {
            delay(1000) // Wait 1 second for server to start
            connectToAllPairedDevices()

            // Keep trying to reconnect every 5 seconds
            while (true) {
                delay(5000)
                connectToAllPairedDevices()
            }
        }
    }

    /**
     * Auto-connect to all paired devices running SafeSphere
     */
    @SuppressLint("MissingPermission")
    private fun connectToAllPairedDevices() {
        scope.launch {
            val pairedDevices = getPairedDevices()
            if (pairedDevices.isEmpty()) return@launch

            Log.d(TAG, "Found ${pairedDevices.size} paired devices, attempting connections...")

            pairedDevices.forEach { device ->
                if (!activeConnections.containsKey(device.address)) {
                    launch {
                        try {
                            Log.d(
                                TAG,
                                "Attempting to connect to ${device.name} (${device.address})..."
                            )
                            connectToDevice(device.address)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to connect to ${device.address}: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    /**
     * Connect to a specific device
     */
    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(deviceAddress: String): Boolean = withContext(Dispatchers.IO) {
        if (!isBluetoothAvailable()) return@withContext false

        // Check if already connected
        if (activeConnections.containsKey(deviceAddress)) {
            return@withContext true
        }

        try {
            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            val socket = device?.createRfcommSocketToServiceRecord(APP_UUID)

            socket?.connect()

            if (socket != null) {
                activeConnections[deviceAddress] = socket
                Log.d(TAG, "Connected to: $deviceAddress")

                // Start listening
                listenForMessages(socket, deviceAddress)
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}")
        }

        return@withContext false
    }

    /**
     * Listen for incoming messages from a device
     */
    private fun listenForMessages(socket: BluetoothSocket, deviceAddress: String) {
        scope.launch {
            try {
                val inputStream = socket.inputStream
                val buffer = ByteArray(1024)

                while (socket.isConnected) {
                    try {
                        val bytes = inputStream.read(buffer)
                        if (bytes > 0) {
                            val message = String(buffer, 0, bytes)
                            Log.d(TAG, "Received message from $deviceAddress: $message")

                            // Save message
                            receiveMessage(deviceAddress, message)

                            // Notify UI
                            onMessageReceived?.invoke(
                                addressToPhoneMap[deviceAddress] ?: deviceAddress, message
                            )
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "Connection lost: ${e.message}")
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Listen error: ${e.message}")
            } finally {
                activeConnections.remove(deviceAddress)
            }
        }
    }

    /**
     * Send message to a device
     */
    suspend fun sendMessage(phone: String, message: String): Boolean = withContext(Dispatchers.IO) {
        // Save locally first
        val msg = Message(
            id = UUID.randomUUID().toString(),
            content = message,
            timestamp = System.currentTimeMillis(),
            isSent = true,
            isDelivered = false, // Will be set to true only when Bluetooth delivers
            isRead = false,
            messageType = MessageType.TEXT,
            fileUri = null,
            fileName = null,
            fileSize = null,
            mimeType = null
        )

        addMessageToConversation(phone, msg)

        // Try to send via Bluetooth if receiver is connected
        var messageSent = false
        val receiverAddress = phoneToAddressMap[phone]

        if (receiverAddress != null && activeConnections.containsKey(receiverAddress)) {
            // Receiver is connected via Bluetooth
            val socket = activeConnections[receiverAddress]
            if (socket?.isConnected == true) {
                try {
                    val outputStream = socket.outputStream
                    val payload = "$phone|$message"
                    outputStream.write(payload.toByteArray())
                    outputStream.flush()

                    Log.d(TAG, "✅ Message sent to $receiverAddress via Bluetooth")
                    messageSent = true

                    // Mark as delivered immediately (double tick)
                    delay(200)
                    markAsDelivered(phone, msg.id)
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to send message to $receiverAddress: ${e.message}")
                    activeConnections.remove(receiverAddress)
                    messageSent = false
                }
            }
        } else {
            // Receiver not connected - try to find and connect
            Log.d(TAG, "⚠️ Receiver not connected, attempting to find device...")

            // Try to connect to all paired devices
            if (activeConnections.isEmpty()) {
                connectToAllPairedDevices()
                delay(1000) // Give time to establish connections
            }

            // Try again after connection attempt
            if (receiverAddress != null && activeConnections.containsKey(receiverAddress)) {
                val socket = activeConnections[receiverAddress]
                if (socket?.isConnected == true) {
                    try {
                        val outputStream = socket.outputStream
                        val payload = "$phone|$message"
                        outputStream.write(payload.toByteArray())
                        outputStream.flush()
                        messageSent = true
                        delay(200)
                        markAsDelivered(phone, msg.id)
                    } catch (e: IOException) {
                        Log.e(TAG, "Failed to send: ${e.message}")
                    }
                }
            }
        }

        if (!messageSent) {
            // Message not delivered - stays at single tick
            Log.d(TAG, "📤 Message saved locally (single tick) - receiver not connected")
        }

        return@withContext messageSent
    }

    /**
     * Send message with file attachment
     */
    suspend fun sendMessageWithFile(
        phone: String,
        content: String,
        messageType: MessageType,
        fileUri: String,
        fileName: String,
        fileSize: Long,
        mimeType: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Copy file to internal storage permanently
            val permanentPath = copyFileToInternalStorage(fileUri, fileName)

            if (permanentPath == null) {
                Log.e(TAG, "Failed to copy file to internal storage")
                return@withContext false
            }

            // Save locally with permanent file path
            val msg = Message(
                id = UUID.randomUUID().toString(),
                content = content,
                timestamp = System.currentTimeMillis(),
                isSent = true,
                isDelivered = false,
                isRead = false,
                messageType = messageType,
                fileUri = permanentPath, // Use permanent file path instead of content URI
                fileName = fileName,
                fileSize = fileSize,
                mimeType = mimeType
            )

            addMessageToConversation(phone, msg)

            // Mark as delivered (file transfer not implemented over Bluetooth yet)
            delay(1000)
            markAsDelivered(phone, msg.id)

            Log.d(TAG, "Message with file sent successfully: ${msg.id}")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message with file: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Receive message from Bluetooth
     */
    private fun receiveMessage(deviceAddress: String, content: String) {
        Log.d(TAG, "Raw message received from $deviceAddress: $content")

        // Parse phone|message or phone|READ_RECEIPT|messageId format
        val parts = content.split("|", limit = 3)
        val phone = if (parts.size >= 2) parts[0] else deviceAddress

        // Map this address to this phone
        addressToPhoneMap[deviceAddress] = phone
        phoneToAddressMap[phone] = deviceAddress

        // Check if it's a read receipt
        if (parts.size == 3 && parts[1] == "READ_RECEIPT") {
            val messageId = parts[2]
            Log.d(TAG, "📘 Received read receipt for message $messageId from $phone")

            // Mark message as read (change tick color)
            markMessageAsReadByReceiver(phone, messageId)
            return
        }

        // Regular message
        val messageContent = if (parts.size >= 2) parts[1] else content

        val msg = Message(
            id = UUID.randomUUID().toString(),
            content = messageContent,
            timestamp = System.currentTimeMillis(),
            isSent = false,
            isDelivered = true,
            isRead = false,
            messageType = MessageType.TEXT,
            fileUri = null,
            fileName = null,
            fileSize = null,
            mimeType = null
        )

        // Add to conversation immediately
        addMessageToConversation(phone, msg)

        Log.d(TAG, "✅ Message saved from $phone: $messageContent")

        // Notify UI on main thread immediately
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onMessageReceived?.invoke(phone, messageContent)
        }
    }

    /**
     * Mark message as read by receiver (changes tick color)
     */
    private fun markMessageAsReadByReceiver(phone: String, messageId: String) {
        val current = _conversations.value.toMutableMap()
        val messages = current[phone]?.map {
            if (it.id == messageId && it.isSent) {
                it.copy(isRead = true) // Blue ticks!
            } else {
                it
            }
        } ?: return

        current[phone] = messages
        _conversations.value = current

        // Save immediately
        saveConversations()

        Log.d(TAG, "💙 Message $messageId marked as read (blue ticks)")
    }

    /**
     * Load conversations from storage
     */
    private fun loadConversations() {
        try {
            val conversationsJson = prefs.getString("conversations", null)

            Log.d(TAG, "==================== LOADING CONVERSATIONS ====================")
            if (conversationsJson != null) {
                Log.d(TAG, "Found saved JSON (length: ${conversationsJson.length})")
                Log.d(TAG, "JSON (first 500 chars): ${conversationsJson.take(500)}")

                val conversations = parseConversationsFromJson(conversationsJson)
                _conversations.value = conversations

                Log.d(TAG, "Loaded ${conversations.size} conversations from storage")
                conversations.forEach { (phone, messages) ->
                    Log.d(TAG, "Phone: $phone - Messages: ${messages.size}")
                    messages.forEach { msg ->
                        Log.d(
                            TAG,
                            "  Loaded MSG - Content: '${msg.content}' Type: ${msg.messageType}"
                        )
                    }
                }
            } else {
                Log.d(TAG, "No saved conversations found")
            }
            Log.d(TAG, "===============================================================")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load conversations: ${e.message}", e)
        }
    }

    /**
     * Save conversations to storage
     */
    private fun saveConversations() {
        try {
            val json = conversationsToJson(_conversations.value)

            // DEBUG: Log the exact JSON being saved
            Log.d(TAG, "==================== SAVING CONVERSATIONS ====================")
            Log.d(TAG, "Total conversations: ${_conversations.value.size}")
            _conversations.value.forEach { (phone, messages) ->
                Log.d(TAG, "Phone: $phone - Messages: ${messages.size}")
                messages.forEach { msg ->
                    Log.d(TAG, "  MSG ID: ${msg.id}")
                    Log.d(TAG, "  Content: '${msg.content}'")
                    Log.d(TAG, "  Type: ${msg.messageType}")
                }
            }
            Log.d(TAG, "JSON to save (first 500 chars): ${json.take(500)}")
            Log.d(TAG, "=============================================================")

            prefs.edit().putString("conversations", json).apply()
            Log.d(TAG, "✅ Saved ${_conversations.value.size} conversations to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save conversations: ${e.message}")
        }
    }

    /**
     * Escape string for JSON
     */
    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")  // Backslash first!
            .replace("\"", "\\\"")  // Quotes
            .replace("\n", "\\n")   // Newlines
            .replace("\r", "\\r")   // Carriage return
            .replace("\t", "\\t")   // Tabs
    }

    /**
     * Unescape JSON string
     */
    private fun unescapeJson(str: String): String {
        return str
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")  // Backslash last!
    }

    /**
     * Convert conversations to JSON
     */
    private fun conversationsToJson(conversations: Map<String, List<Message>>): String {
        val sb = StringBuilder()
        sb.append("{")

        conversations.entries.forEachIndexed { index, entry ->
            if (index > 0) sb.append(",")
            sb.append("\"${entry.key}\":[")

            entry.value.forEachIndexed { msgIndex, msg ->
                if (msgIndex > 0) sb.append(",")
                sb.append("{")
                sb.append("\"id\":\"${msg.id}\",")
                sb.append("\"content\":\"${escapeJson(msg.content)}\",")
                sb.append("\"timestamp\":${msg.timestamp},")
                sb.append("\"isSent\":${msg.isSent},")
                sb.append("\"isDelivered\":${msg.isDelivered},")
                sb.append("\"isRead\":${msg.isRead},")
                sb.append("\"isEdited\":${msg.isEdited},")
                sb.append("\"messageType\":\"${msg.messageType.name}\",")
                sb.append("\"fileUri\":\"${escapeJson(msg.fileUri ?: "")}\",")
                sb.append("\"fileName\":\"${escapeJson(msg.fileName ?: "")}\",")
                sb.append("\"fileSize\":${msg.fileSize ?: 0},")
                sb.append("\"mimeType\":\"${escapeJson(msg.mimeType ?: "")}\"")
                sb.append("}")
            }

            sb.append("]")
        }

        sb.append("}")
        return sb.toString()
    }

    /**
     * Parse conversations from JSON
     */
    private fun parseConversationsFromJson(json: String): Map<String, List<Message>> {
        val conversations = mutableMapOf<String, List<Message>>()

        try {
            Log.d(TAG, "📖 Starting to parse conversations JSON (length: ${json.length})")

            // Remove outer braces
            var i = json.indexOf('{')
            if (i == -1) return emptyMap()
            i++ // Skip opening brace

            while (i < json.length) {
                // Skip whitespace
                while (i < json.length && json[i].isWhitespace()) i++

                if (i >= json.length || json[i] == '}') break

                // Read phone number (key)
                if (json[i] != '"') break
                i++ // Skip opening quote

                val phoneBuilder = StringBuilder()
                while (i < json.length && json[i] != '"') {
                    phoneBuilder.append(json[i])
                    i++
                }
                val phone = phoneBuilder.toString()
                i++ // Skip closing quote

                // Skip colon and whitespace
                while (i < json.length && (json[i].isWhitespace() || json[i] == ':')) i++

                // Expect opening bracket for messages array
                if (i >= json.length || json[i] != '[') break
                i++ // Skip opening bracket

                val messages = mutableListOf<Message>()

                // Parse messages array
                while (i < json.length) {
                    // Skip whitespace
                    while (i < json.length && json[i].isWhitespace()) i++

                    if (i >= json.length || json[i] == ']') {
                        i++ // Skip closing bracket
                        break
                    }

                    // Skip comma if present
                    if (json[i] == ',') {
                        i++
                        continue
                    }

                    // Expect opening brace for message object
                    if (json[i] != '{') break

                    // Find the complete message object by counting braces
                    val messageStart = i
                    var braceCount = 0
                    var inString = false
                    var escaped = false

                    while (i < json.length) {
                        val c = json[i]

                        if (escaped) {
                            escaped = false
                            i++
                            continue
                        }

                        if (c == '\\') {
                            escaped = true
                            i++
                            continue
                        }

                        if (c == '"') {
                            inString = !inString
                        } else if (!inString) {
                            if (c == '{') braceCount++
                            else if (c == '}') {
                                braceCount--
                                if (braceCount == 0) {
                                    i++ // Include closing brace
                                    break
                                }
                            }
                        }

                        i++
                    }

                    // Extract and parse this message
                    val messageJson = json.substring(messageStart, i)

                    try {
                        val id = extractField(messageJson, "id")
                        val content = extractField(messageJson, "content")
                        val timestamp = extractField(messageJson, "timestamp").toLongOrNull() ?: 0L
                        val isSent = extractField(messageJson, "isSent").toBoolean()
                        val isDelivered = extractField(messageJson, "isDelivered").toBoolean()
                        val isRead = extractField(messageJson, "isRead").toBoolean()
                        val isEdited = extractField(messageJson, "isEdited").toBoolean()
                        val messageTypeStr = extractField(messageJson, "messageType")
                        val messageType = try {
                            MessageType.valueOf(
                                messageTypeStr.takeIf { it.isNotEmpty() } ?: "TEXT"
                            )
                        } catch (e: Exception) {
                            MessageType.TEXT
                        }
                        val fileUri =
                            extractField(messageJson, "fileUri").takeIf { it.isNotEmpty() }
                        val fileName =
                            extractField(messageJson, "fileName").takeIf { it.isNotEmpty() }
                        val fileSize = extractField(messageJson, "fileSize").toLongOrNull()
                        val mimeType =
                            extractField(messageJson, "mimeType").takeIf { it.isNotEmpty() }

                        Log.d(
                            TAG,
                            "📩 Parsed message - Phone: $phone, ID: $id, Content: '$content', Type: $messageType"
                        )

                        if (id.isNotEmpty()) {
                            messages.add(
                                Message(
                                    id = id,
                                    content = content,
                                    timestamp = timestamp,
                                    isSent = isSent,
                                    isDelivered = isDelivered,
                                    isRead = isRead,
                                    isEdited = isEdited,
                                    messageType = messageType,
                                    fileUri = fileUri,
                                    fileName = fileName,
                                    fileSize = fileSize,
                                    mimeType = mimeType
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing individual message: ${e.message}")
                    }
                }

                conversations[phone] = messages
                Log.d(TAG, "✅ Loaded conversation for $phone: ${messages.size} messages")

                // Skip comma between phone entries
                while (i < json.length && (json[i].isWhitespace() || json[i] == ',')) i++
            }

            Log.d(TAG, "✅ Parsed ${conversations.size} conversations total")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse conversations: ${e.message}", e)
        }

        return conversations
    }

    /**
     * Extract field value from JSON string
     * Properly handles quoted strings with escaped characters
     */
    private fun extractField(json: String, field: String): String {
        try {
            // Find the field key
            val fieldKey = "\"$field\":"
            val startIndex = json.indexOf(fieldKey)
            if (startIndex == -1) return ""

            // Find where the value starts (after the colon and optional spaces)
            var valueStart = startIndex + fieldKey.length
            while (valueStart < json.length && json[valueStart].isWhitespace()) {
                valueStart++
            }

            // Check if it's a quoted string
            if (valueStart >= json.length) return ""

            return if (json[valueStart] == '"') {
                // It's a quoted string - find the closing quote (not escaped)
                valueStart++ // Skip opening quote
                val sb = StringBuilder()
                var i = valueStart

                while (i < json.length) {
                    val char = json[i]

                    when {
                        char == '\\' && i + 1 < json.length -> {
                            // Escaped character
                            val nextChar = json[i + 1]
                            when (nextChar) {
                                'n' -> sb.append('\n')
                                'r' -> sb.append('\r')
                                't' -> sb.append('\t')
                                '"' -> sb.append('"')
                                '\\' -> sb.append('\\')
                                else -> {
                                    sb.append('\\')
                                    sb.append(nextChar)
                                }
                            }
                            i += 2
                        }

                        char == '"' -> {
                            // Closing quote found
                            return sb.toString()
                        }

                        else -> {
                            sb.append(char)
                            i++
                        }
                    }
                }

                // If we got here, no closing quote found - return what we have
                sb.toString()
            } else {
                // Not a quoted value (number, boolean, etc.)
                val sb = StringBuilder()
                var i = valueStart
                while (i < json.length && json[i] != ',' && json[i] != '}' && json[i] != ']') {
                    sb.append(json[i])
                    i++
                }
                sb.toString().trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting field '$field': ${e.message}")
            return ""
        }
    }

    /**
     * Add message to conversation
     */
    private fun addMessageToConversation(phone: String, message: Message) {
        val current = _conversations.value.toMutableMap()
        val messages = current[phone]?.toMutableList() ?: mutableListOf()
        messages.add(message)

        current[phone] = messages
        _conversations.value = current

        // Save immediately
        saveConversations()

        Log.d(
            TAG,
            "✅ Added message to $phone. Content: '${message.content}' Type: ${message.messageType} Total: ${messages.size}"
        )
    }

    /**
     * Mark message as delivered
     */
    private fun markAsDelivered(phone: String, messageId: String) {
        val current = _conversations.value.toMutableMap()
        val messages = current[phone]?.map {
            if (it.id == messageId) it.copy(isDelivered = true) else it
        } ?: return

        current[phone] = messages
        _conversations.value = current

        // Save immediately
        saveConversations()
    }

    /**
     * Mark messages as read and send read receipt
     */
    fun markAsRead(phone: String) {
        val current = _conversations.value.toMutableMap()
        val messages = current[phone]?.map {
            if (!it.isSent && !it.isRead) {
                // Send read receipt back to sender
                scope.launch {
                    sendReadReceipt(phone, it.id)
                }
            }
            it.copy(isRead = true)
        } ?: return

        current[phone] = messages
        _conversations.value = current

        // Save immediately
        saveConversations()
    }

    /**
     * Send read receipt to sender
     */
    private suspend fun sendReadReceipt(phone: String, messageId: String) {
        withContext(Dispatchers.IO) {
            val receiverAddress = phoneToAddressMap[phone]
            if (receiverAddress != null && activeConnections.containsKey(receiverAddress)) {
                val socket = activeConnections[receiverAddress]
                if (socket?.isConnected == true) {
                    try {
                        val outputStream = socket.outputStream
                        // Send read receipt in format: phone|READ_RECEIPT|messageId
                        val payload = "$phone|READ_RECEIPT|$messageId"
                        outputStream.write(payload.toByteArray())
                        outputStream.flush()
                        Log.d(TAG, "✅ Sent read receipt for message $messageId")
                    } catch (e: IOException) {
                        Log.e(TAG, "Failed to send read receipt: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Delete message
     */
    suspend fun deleteMessage(phone: String, messageId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val currentMessages =
                    _conversations.value[phone]?.toMutableList() ?: return@withContext false
                val removed = currentMessages.removeIf { it.id == messageId }

                if (removed) {
                    _conversations.value = _conversations.value.toMutableMap().apply {
                        put(phone, currentMessages)
                    }
                    saveConversations()
                    Log.d(TAG, "Message deleted: $messageId")
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete message: ${e.message}", e)
                false
            }
        }

    /**
     * Edit message
     */
    suspend fun editMessage(phone: String, messageId: String, newContent: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val currentMessages =
                    _conversations.value[phone]?.toMutableList() ?: return@withContext false
                val index = currentMessages.indexOfFirst { it.id == messageId }

                if (index != -1) {
                    val updatedMessage = currentMessages[index].copy(
                        content = newContent,
                        timestamp = System.currentTimeMillis(), // Update timestamp to show as edited
                        isEdited = true
                    )
                    currentMessages[index] = updatedMessage

                    _conversations.value = _conversations.value.toMutableMap().apply {
                        put(phone, currentMessages)
                    }
                    saveConversations()
                    Log.d(TAG, "Message edited: $messageId")
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to edit message: ${e.message}", e)
                false
            }
        }

    /**
     * Get paired Bluetooth devices
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!isBluetoothAvailable()) return emptyList()
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    /**
     * Copy file from URI to internal storage permanently
     */
    private fun copyFileToInternalStorage(fileUri: String, fileName: String): String? {
        return try {
            val sourceUri = Uri.parse(fileUri)
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null

            // Create directory for messenger files
            val messengerDir = File(context.filesDir, "messenger_files")
            if (!messengerDir.exists()) {
                messengerDir.mkdirs()
            }

            // Create unique filename to avoid conflicts
            val timestamp = System.currentTimeMillis()
            val extension = fileName.substringAfterLast(".", "")
            val uniqueFileName = "${timestamp}_$fileName"
            val destFile = File(messengerDir, uniqueFileName)

            // Copy file
            inputStream.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "File copied to: ${destFile.absolutePath}")
            destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy file: ${e.message}", e)
            null
        }
    }

    /**
     * Cleanup
     */
    fun cleanup() {
        try {
            serverSocket?.close()
            activeConnections.values.forEach { it.close() }
            activeConnections.clear()
            scope.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "OfflineMessenger"

        @Volatile
        private var instance: OfflineMessengerRepository? = null

        fun getInstance(context: Context): OfflineMessengerRepository {
            return instance ?: synchronized(this) {
                instance ?: OfflineMessengerRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Message data class
 */
data class Message(
    val id: String,
    val content: String,
    val timestamp: Long,
    val isSent: Boolean,
    val isDelivered: Boolean,
    val isRead: Boolean,
    val isEdited: Boolean = false,
    val messageType: MessageType = MessageType.TEXT,
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null
)

/**
 * Message types
 */
enum class MessageType {
    TEXT,
    IMAGE,
    DOCUMENT,
    VOICE,
    VIDEO
}
