package com.runanywhere.startup_hackathon20.messenger

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Bluetooth Service - High-speed, reliable Bluetooth messaging
 *
 * Features:
 * - Automatic device discovery and connection
 * - Bidirectional communication
 * - Message queuing and retry
 * - Connection health monitoring
 * - Fast chunked file transfer
 */
@SuppressLint("MissingPermission")
class BluetoothService(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Service UUID for SafeSphere Messenger
    private val SERVICE_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    private val APP_NAME = "SafeSphere"

    // Connection management
    private val activeConnections = ConcurrentHashMap<String, ConnectionHandler>()
    private var serverSocket: BluetoothServerSocket? = null
    private var isRunning = false

    // State flows
    private val _connectedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val connectedDevices: StateFlow<List<BluetoothDeviceInfo>> = _connectedDevices

    // Callbacks
    var onMessageReceived: ((BluetoothPacket) -> Unit)? = null
    var onConnectionEstablished: ((String) -> Unit)? = null
    var onConnectionLost: ((String) -> Unit)? = null

    /**
     * Start the Bluetooth service
     */
    fun startService() {
        try {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Log.e(TAG, "❌ Bluetooth not available or not enabled")
                return
            }

            Log.d(TAG, "🚀 Starting Bluetooth service...")
            isRunning = true

            // Start server to accept incoming connections - MUST START FIRST
            scope.launch {
                try {
                    Log.d(TAG, "🎧 Starting server socket...")
                    startServer()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Server start error: ${e.message}", e)
                }
            }

            // Give server time to start, then connect to paired devices
            scope.launch {
                try {
                    Log.d(TAG, "⏳ Waiting 2 seconds for server to start...")
                    delay(2000)
                    Log.d(TAG, "🔌 Now connecting to paired devices...")
                    connectToPairedDevices()

                    // Retry connection every 10 seconds
                    while (isRunning) {
                        delay(10000)
                        if (activeConnections.isEmpty()) {
                            Log.d(TAG, "🔄 No connections, retrying...")
                            connectToPairedDevices()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Connection start error: ${e.message}", e)
                }
            }

            // Health check every 30 seconds
            scope.launch {
                try {
                    while (isRunning) {
                        delay(30000)
                        checkConnectionHealth()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Health check error: ${e.message}", e)
                }
            }

            Log.d(TAG, "✅ Bluetooth service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start service: ${e.message}", e)
        }
    }

    /**
     * Start Bluetooth server socket
     */
    private suspend fun startServer() {
        var acceptAttempts = 0
        while (isRunning) {
            try {
                if (serverSocket == null) {
                    serverSocket = bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(
                        APP_NAME,
                        SERVICE_UUID
                    )
                    Log.d(TAG, "✅ Server socket created successfully")
                }

                Log.d(TAG, "📡 Server listening for connections (attempt ${++acceptAttempts})...")

                val socket = withTimeoutOrNull(5000) {
                    serverSocket?.accept()
                }

                if (socket != null) {
                    Log.d(
                        TAG,
                        "✅ Incoming connection from ${socket.remoteDevice.name ?: socket.remoteDevice.address}"
                    )
                    handleNewConnection(socket)
                } else {
                    // Timeout - just loop again
                    Log.d(TAG, "⏱ Accept timeout, retrying...")
                }
            } catch (e: IOException) {
                if (isRunning) {
                    Log.w(TAG, "⚠️ Server socket error: ${e.message}")
                    // Recreate server socket
                    try {
                        serverSocket?.close()
                    } catch (ex: Exception) {
                    }
                    serverSocket = null
                    delay(2000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Server error: ${e.message}", e)
                delay(2000)
            }
        }

        Log.d(TAG, "🛑 Server stopped")
    }

    /**
     * Connect to all paired devices
     */
    private suspend fun connectToPairedDevices() {
        val pairedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()

        Log.d(TAG, "📱 Found ${pairedDevices.size} paired device(s)")

        for (device in pairedDevices) {
            Log.d(TAG, "Paired device: ${device.name} (${device.address})")
            if (!activeConnections.containsKey(device.address)) {
                scope.launch {
                    // Try multiple times with delays
                    repeat(3) { attempt ->
                        if (!activeConnections.containsKey(device.address)) {
                            Log.d(TAG, "Connection attempt ${attempt + 1}/3 to ${device.name}")
                            connectToDevice(device)
                            delay(2000)
                        }
                    }
                }
            }
        }
    }

    /**
     * Connect to a specific device
     */
    private suspend fun connectToDevice(device: BluetoothDevice) {
        if (activeConnections.containsKey(device.address)) {
            Log.d(TAG, "✅ Already connected to ${device.address}")
            return
        }

        var socket: BluetoothSocket? = null

        try {
            // Method 1: Try insecure connection FIRST (works best for peer-to-peer)
            Log.d(
                TAG,
                "🔌 [Method 1] Trying insecure connection to ${device.name ?: device.address}..."
            )
            socket = device.createInsecureRfcommSocketToServiceRecord(SERVICE_UUID)

            withTimeoutOrNull(5000) {
                socket?.connect()
            }

            if (socket?.isConnected == true) {
                Log.d(TAG, "✅ Connected (insecure) to ${device.name} (${device.address})")
                handleNewConnection(socket)
                return
            } else {
                socket?.close()
                throw IOException("Connection timeout")
            }

        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Method 1 failed: ${e.message}")
            try {
                socket?.close()
            } catch (ex: Exception) {
            }

            try {
                // Method 2: Try secure connection
                Log.d(
                    TAG,
                    "🔌 [Method 2] Trying secure connection to ${device.name ?: device.address}..."
                )
                socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)

                withTimeoutOrNull(5000) {
                    socket?.connect()
                }

                if (socket?.isConnected == true) {
                    Log.d(TAG, "✅ Connected (secure) to ${device.name} (${device.address})")
                    handleNewConnection(socket)
                    return
                } else {
                    socket?.close()
                    throw IOException("Connection timeout")
                }

            } catch (e2: Exception) {
                Log.w(TAG, "⚠️ Method 2 failed: ${e2.message}")
                try {
                    socket?.close()
                } catch (ex: Exception) {
                }

                try {
                    // Method 3: Try reflection method as last resort
                    Log.d(
                        TAG,
                        "🔌 [Method 3] Trying reflection method to ${device.name ?: device.address}..."
                    )
                    val method = device.javaClass.getMethod(
                        "createRfcommSocket",
                        Int::class.javaPrimitiveType
                    )
                    socket = method.invoke(device, 1) as BluetoothSocket?

                    withTimeoutOrNull(5000) {
                        socket?.connect()
                    }

                    if (socket?.isConnected == true) {
                        Log.d(TAG, "✅ Connected (reflection) to ${device.name} (${device.address})")
                        handleNewConnection(socket)
                        return
                    } else {
                        socket?.close()
                        throw IOException("Connection timeout")
                    }

                } catch (e3: Exception) {
                    Log.e(
                        TAG,
                        "❌ All 3 connection methods failed for ${device.name}: ${e3.message}"
                    )
                    try {
                        socket?.close()
                    } catch (ex: Exception) {
                    }
                }
            }
        }
    }

    /**
     * Handle new Bluetooth connection
     */
    private fun handleNewConnection(socket: BluetoothSocket) {
        val deviceAddress = socket.remoteDevice.address
        val deviceName = socket.remoteDevice.name ?: "Unknown"

        // Avoid duplicate connections
        if (activeConnections.containsKey(deviceAddress)) {
            Log.w(TAG, "Duplicate connection attempt from $deviceAddress, closing new socket")
            try {
                socket.close()
            } catch (e: Exception) {
            }
            return
        }

        val handler = ConnectionHandler(socket)
        activeConnections[deviceAddress] = handler

        Log.d(TAG, "🎉 Connection established! Device: $deviceName ($deviceAddress)")
        Log.d(TAG, "📊 Total active connections: ${activeConnections.size}")

        // Start listening for messages
        scope.launch {
            handler.listen()
        }

        // Update UI
        updateConnectedDevices()
        onConnectionEstablished?.invoke(deviceAddress)
    }

    /**
     * Send a packet to all connected devices
     */
    fun sendPacket(packet: BluetoothPacket): Boolean {
        val connectionCount = activeConnections.size

        Log.d(TAG, "📤 ========================================")
        Log.d(TAG, "📤 SEND PACKET ATTEMPT")
        Log.d(TAG, "📤 Active connections: $connectionCount")

        if (connectionCount == 0) {
            Log.e(TAG, "❌ NO ACTIVE CONNECTIONS!")
            Log.e(TAG, "❌ Cannot send packet")
            Log.e(TAG, "⚠️ Please ensure:")
            Log.e(TAG, "   1. Both devices are Bluetooth paired in Settings")
            Log.e(TAG, "   2. Both devices have Offline Messenger open")
            Log.e(TAG, "   3. Wait 10-15 seconds for connection to establish")
            Log.e(TAG, "📤 ========================================")
            return false
        }

        val json = packetToJson(packet)
        var successCount = 0
        var failCount = 0

        Log.d(TAG, "📨 Sending to $connectionCount device(s)...")
        Log.d(TAG, "📨 Message ID: ${packet.messageId}")
        Log.d(TAG, "📨 Content: ${packet.payload}")

        for ((address, handler) in activeConnections) {
            val deviceName = handler.socket.remoteDevice.name ?: "Unknown"
            Log.d(TAG, "📡 Attempting to send to: $deviceName ($address)")

            val sent = handler.send(json)

            if (sent) {
                successCount++
                Log.d(TAG, "   ✅ SUCCESS - Sent to $deviceName ($address)")
            } else {
                failCount++
                Log.e(TAG, "   ❌ FAILED - Could not send to $deviceName ($address)")
            }
        }

        val overallSuccess = successCount > 0

        Log.d(TAG, "📊 SEND RESULT: $successCount succeeded, $failCount failed")
        if (overallSuccess) {
            Log.d(TAG, "✅ Message sent successfully")
        } else {
            Log.e(TAG, "❌ Message failed to send to all devices")
        }
        Log.d(TAG, "📤 ========================================")

        return overallSuccess
    }

    /**
     * Get connected device count
     */
    fun getConnectionCount(): Int = activeConnections.size

    /**
     * Check if Bluetooth is available
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    /**
     * Check connection health and remove dead connections
     */
    private fun checkConnectionHealth() {
        val deadConnections = mutableListOf<String>()

        for ((address, handler) in activeConnections) {
            if (!handler.isConnected()) {
                deadConnections.add(address)
            }
        }

        for (address in deadConnections) {
            activeConnections.remove(address)?.close()
            onConnectionLost?.invoke(address)
        }

        if (deadConnections.isNotEmpty()) {
            updateConnectedDevices()
            Log.d(TAG, "🧹 Removed ${deadConnections.size} dead connection(s)")
        }
    }

    /**
     * Update connected devices state
     */
    private fun updateConnectedDevices() {
        val devices = activeConnections.map { (address, handler) ->
            BluetoothDeviceInfo(
                address = address,
                name = handler.socket.remoteDevice.name,
                isConnected = handler.isConnected(),
                connectionTime = System.currentTimeMillis()
            )
        }
        _connectedDevices.value = devices
    }

    /**
     * Convert packet to JSON
     */
    private fun packetToJson(packet: BluetoothPacket): String {
        return JSONObject().apply {
            put("type", packet.type.name)
            put("senderId", packet.senderId)
            put("receiverId", packet.receiverId)
            put("messageId", packet.messageId)
            put("timestamp", packet.timestamp)
            put("payload", packet.payload)
            put("chunkIndex", packet.chunkIndex)
            put("totalChunks", packet.totalChunks)

            packet.fileMetadata?.let { metadata ->
                put("fileMetadata", JSONObject().apply {
                    put("messageId", metadata.messageId)
                    put("fileName", metadata.fileName)
                    put("fileSize", metadata.fileSize)
                    put("mimeType", metadata.mimeType)
                    put("messageType", metadata.messageType.name)
                    put("caption", metadata.caption)
                })
            }
        }.toString()
    }

    /**
     * Convert JSON to packet
     */
    private fun jsonToPacket(json: String): BluetoothPacket? {
        return try {
            val obj = JSONObject(json)

            val fileMetadata = if (obj.has("fileMetadata")) {
                val meta = obj.getJSONObject("fileMetadata")
                FileMetadata(
                    messageId = meta.getString("messageId"),
                    fileName = meta.getString("fileName"),
                    fileSize = meta.getLong("fileSize"),
                    mimeType = meta.getString("mimeType"),
                    messageType = MessageType.valueOf(meta.getString("messageType")),
                    caption = meta.optString("caption", "")
                )
            } else null

            BluetoothPacket(
                type = PacketType.valueOf(obj.getString("type")),
                senderId = obj.getString("senderId"),
                receiverId = obj.getString("receiverId"),
                messageId = obj.getString("messageId"),
                timestamp = obj.getLong("timestamp"),
                payload = obj.getString("payload"),
                chunkIndex = obj.optInt("chunkIndex", 0),
                totalChunks = obj.optInt("totalChunks", 1),
                fileMetadata = fileMetadata
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse packet: ${e.message}")
            null
        }
    }

    /**
     * Connection Handler - Manages a single Bluetooth connection
     */
    private inner class ConnectionHandler(val socket: BluetoothSocket) {
        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream
        private var connected = true

        /**
         * Listen for incoming messages
         */
        suspend fun listen() {
            val buffer = ByteArray(4096)
            var messageBuffer = StringBuilder()

            try {
                while (connected) {
                    val bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        val chunk = String(buffer, 0, bytes)
                        messageBuffer.append(chunk)

                        // Process complete messages (delimited by newline)
                        var newlineIndex = messageBuffer.indexOf("\n")
                        while (newlineIndex != -1) {
                            val message = messageBuffer.substring(0, newlineIndex)
                            messageBuffer.delete(0, newlineIndex + 1)

                            // Parse and handle message
                            jsonToPacket(message)?.let { packet ->
                                onMessageReceived?.invoke(packet)
                            }

                            newlineIndex = messageBuffer.indexOf("\n")
                        }
                    }
                }
            } catch (e: IOException) {
                Log.w(TAG, "Connection lost: ${e.message}")
                connected = false
                activeConnections.remove(socket.remoteDevice.address)
                updateConnectedDevices()
                onConnectionLost?.invoke(socket.remoteDevice.address)
            }
        }

        /**
         * Send message
         */
        fun send(message: String): Boolean {
            return try {
                outputStream.write("$message\n".toByteArray())
                outputStream.flush()
                true
            } catch (e: IOException) {
                Log.e(TAG, "Send failed: ${e.message}")
                connected = false
                false
            }
        }

        /**
         * Check if connected
         */
        fun isConnected(): Boolean = connected && socket.isConnected

        /**
         * Close connection
         */
        fun close() {
            connected = false
            try {
                socket.close()
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Stop the service
     */
    fun stopService() {
        isRunning = false

        // Close all connections
        for ((_, handler) in activeConnections) {
            handler.close()
        }
        activeConnections.clear()

        // Close server socket
        try {
            serverSocket?.close()
        } catch (e: Exception) {
        }

        scope.cancel()

        Log.d(TAG, "Bluetooth service stopped")
    }

    companion object {
        private const val TAG = "BluetoothService"
    }
}
