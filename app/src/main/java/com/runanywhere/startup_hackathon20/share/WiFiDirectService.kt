package com.runanywhere.startup_hackathon20.share

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.net.wifi.p2p.*
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * WiFi Direct Service - High-speed file transfer
 *
 * Uses WiFi Direct (Wi-Fi P2P) for blazing-fast file transfers
 * Speeds: 10-250 Mbps (vs Bluetooth's 1-3 Mbps)
 */
@SuppressLint("MissingPermission")
class WiFiDirectService(private val context: Context) {

    private val wifiP2pManager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val wifiManager: WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var channel: WifiP2pManager.Channel? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // State
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled

    private val _discoveredDevices = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<WifiP2pDevice>> = _discoveredDevices

    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo

    private val _transferProgress = MutableStateFlow<TransferProgress?>(null)
    val transferProgress: StateFlow<TransferProgress?> = _transferProgress

    private val _discoveryState = MutableStateFlow<DiscoveryState>(DiscoveryState.IDLE)
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState

    // Callbacks
    var onDeviceDiscovered: ((WifiP2pDevice) -> Unit)? = null
    var onConnectionEstablished: ((WifiP2pInfo) -> Unit)? = null
    var onFileReceived: ((File) -> Unit)? = null
    var onTransferComplete: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    // Constants
    private val PORT = 8888
    private val BUFFER_SIZE = 65536 // 64KB chunks for fast transfer

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var isServerRunning = false
    private var isDiscovering = false

    init {
        initializeWiFiDirect()
    }

    /**
     * Initialize WiFi Direct
     */
    private fun initializeWiFiDirect() {
        try {
            if (wifiP2pManager == null) {
                Log.e(TAG, "❌ WiFi Direct not supported on this device")
                onError?.invoke("WiFi Direct not supported on this device")
                return
            }

            channel = wifiP2pManager.initialize(context, context.mainLooper) {
                Log.d(TAG, "📡 WiFi Direct channel disconnected")
                _isEnabled.value = false
            }

            if (channel == null) {
                Log.e(TAG, "❌ Failed to initialize WiFi Direct channel")
                onError?.invoke("Failed to initialize WiFi Direct")
                return
            }

            registerReceivers()

            // Check initial WiFi state
            checkWiFiState()

            Log.d(TAG, "✅ WiFi Direct initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing WiFi Direct: ${e.message}", e)
            onError?.invoke("Failed to initialize: ${e.message}")
        }
    }

    /**
     * Check if WiFi is enabled
     */
    private fun isWiFiEnabled(): Boolean {
        return try {
            wifiManager?.isWifiEnabled ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking WiFi state: ${e.message}")
            false
        }
    }

    /**
     * Check WiFi state and update accordingly
     */
    private fun checkWiFiState() {
        val wifiEnabled = isWiFiEnabled()
        Log.d(TAG, "📡 WiFi state: ${if (wifiEnabled) "ENABLED" else "DISABLED"}")

        if (!wifiEnabled) {
            _isEnabled.value = false
            _discoveryState.value = DiscoveryState.WIFI_DISABLED
            onError?.invoke("WiFi is disabled. Please enable WiFi to use SafeSphere Share.")
        } else {
            _isEnabled.value = true
            if (_discoveryState.value == DiscoveryState.WIFI_DISABLED) {
                _discoveryState.value = DiscoveryState.IDLE
            }
        }
    }

    /**
     * Register broadcast receivers for WiFi P2P events
     */
    private fun registerReceivers() {
        try {
            val intentFilter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    wifiP2pReceiver,
                    intentFilter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(wifiP2pReceiver, intentFilter)
            }
            Log.d(TAG, "✅ WiFi Direct receivers registered")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error registering receivers: ${e.message}", e)
            onError?.invoke("Failed to register receivers: ${e.message}")
        }
    }

    /**
     * Broadcast Receiver for WiFi P2P events
     */
    private val wifiP2pReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    val p2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    Log.d(
                        TAG,
                        "📡 WiFi P2P state: ${if (p2pEnabled) "ENABLED" else "DISABLED"}"
                    )

                    // Double check actual WiFi state
                    if (p2pEnabled && !isWiFiEnabled()) {
                        Log.w(TAG, "⚠️ WiFi P2P enabled but WiFi is disabled")
                        _isEnabled.value = false
                        _discoveryState.value = DiscoveryState.WIFI_DISABLED
                        onError?.invoke("WiFi is disabled. Please enable WiFi.")
                    } else {
                        _isEnabled.value = p2pEnabled
                        if (!p2pEnabled) {
                            _discoveryState.value = DiscoveryState.WIFI_DISABLED
                            onError?.invoke("WiFi Direct is disabled. Please enable WiFi.")
                        } else if (isDiscovering) {
                            _discoveryState.value = DiscoveryState.DISCOVERING
                        }
                    }
                }

                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    Log.d(TAG, "👥 Peers changed, requesting peer list")
                    requestPeers()
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    Log.d(TAG, "🔗 Connection changed")
                    handleConnectionChanged(intent)
                }

                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            WifiP2pManager.EXTRA_WIFI_P2P_DEVICE,
                            WifiP2pDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    }
                    Log.d(TAG, "📱 This device: ${device?.deviceName ?: "Unknown"}")
                }

                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    val wifiState = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE,
                        WifiManager.WIFI_STATE_UNKNOWN
                    )
                    Log.d(TAG, "📡 WiFi state changed: ${getWiFiStateString(wifiState)}")

                    // Update state when WiFi changes
                    if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                        Log.d(TAG, "✅ WiFi enabled - SafeSphere Share available")
                        checkWiFiState()
                    } else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                        Log.d(TAG, "❌ WiFi disabled - SafeSphere Share unavailable")
                        _isEnabled.value = false
                        _discoveryState.value = DiscoveryState.WIFI_DISABLED
                        if (isDiscovering) {
                            stopDiscovery()
                        }
                        onError?.invoke("WiFi was disabled. Please enable WiFi to use SafeSphere Share.")
                    }
                }
            }
        }
    }

    private fun getWiFiStateString(state: Int): String {
        return when (state) {
            WifiManager.WIFI_STATE_ENABLED -> "ENABLED"
            WifiManager.WIFI_STATE_DISABLED -> "DISABLED"
            WifiManager.WIFI_STATE_ENABLING -> "ENABLING"
            WifiManager.WIFI_STATE_DISABLING -> "DISABLING"
            WifiManager.WIFI_STATE_UNKNOWN -> "UNKNOWN"
            else -> "INVALID"
        }
    }

    /**
     * Handle connection changed event
     */
    private fun handleConnectionChanged(intent: Intent) {
        val networkInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                WifiP2pManager.EXTRA_NETWORK_INFO,
                android.net.NetworkInfo::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
        }

        if (networkInfo?.isConnected == true) {
            wifiP2pManager?.requestConnectionInfo(channel) { info ->
                _connectionInfo.value = info
                onConnectionEstablished?.invoke(info)
                Log.d(
                    TAG,
                    "✅ Connection established - Group Owner: ${info.isGroupOwner}"
                )

                // Start server or client based on role
                if (info.groupFormed) {
                    if (info.isGroupOwner) {
                        Log.d(TAG, "🖥️ Starting as server (Group Owner)")
                        startServer()
                    } else {
                        Log.d(TAG, "📱 Starting as client (peer)")
                    }
                }
            }
        } else {
            Log.d(TAG, "🔌 Disconnected from peer")
            _connectionInfo.value = null
        }
    }

    /**
     * Start discovering nearby devices
     */
    fun startDiscovery() {
        // Check system state first
        val systemState = SystemState.check(context)

        if (!systemState.isWiFiEnabled) {
            Log.e(TAG, "❌ Cannot discover: WiFi is disabled")
            _discoveryState.value = DiscoveryState.WIFI_DISABLED
            _isEnabled.value = false
            onError?.invoke("WiFi is disabled. Please turn on WiFi to discover nearby devices.")
            return
        }

        if (!systemState.isLocationEnabled) {
            Log.e(TAG, "❌ Cannot discover: Location is disabled")
            _discoveryState.value = DiscoveryState.WIFI_DISABLED
            _isEnabled.value = false
            onError?.invoke("Location is disabled. Please turn on device location to discover nearby devices.")
            return
        }

        if (!_isEnabled.value) {
            Log.e(TAG, "❌ Cannot discover: WiFi Direct is not available")
            _discoveryState.value = DiscoveryState.WIFI_DISABLED
            onError?.invoke("WiFi Direct is not available. Please ensure WiFi is enabled.")
            return
        }

        if (isDiscovering) {
            Log.d(TAG, "⚠️ Discovery already in progress")
            return
        }

        Log.d(TAG, "🔍 Starting device discovery...")
        isDiscovering = true
        _discoveryState.value = DiscoveryState.DISCOVERING
        _discoveredDevices.value = emptyList() // Clear old devices

        wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "✅ Discovery started successfully")
                _discoveryState.value = DiscoveryState.DISCOVERING
            }

            override fun onFailure(reason: Int) {
                val errorMsg = getFailureReason(reason)
                Log.e(TAG, "❌ Discovery failed: $errorMsg")
                isDiscovering = false
                _discoveryState.value = DiscoveryState.ERROR

                // Provide specific error message based on failure reason
                val userMessage = when (reason) {
                    WifiP2pManager.P2P_UNSUPPORTED -> "Your device doesn't support WiFi Direct"
                    WifiP2pManager.BUSY -> "Device is busy. Please try again in a moment"
                    WifiP2pManager.ERROR -> "Internal error. Make sure WiFi and Location are enabled"
                    else -> "Discovery failed: $errorMsg. Check WiFi and Location settings"
                }
                onError?.invoke(userMessage)
            }
        })

        // Auto-stop discovery after 30 seconds
        scope.launch {
            delay(30000)
            if (isDiscovering) {
                stopDiscovery()
            }
        }
    }

    /**
     * Stop discovery
     */
    fun stopDiscovery() {
        if (!isDiscovering) return

        wifiP2pManager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "✅ Discovery stopped")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "⚠️ Failed to stop discovery: ${getFailureReason(reason)}")
            }
        })

        isDiscovering = false
        _discoveryState.value = DiscoveryState.IDLE
        Log.d(TAG, "🛑 Discovery stopped")
    }

    /**
     * Request list of discovered peers
     */
    private fun requestPeers() {
        wifiP2pManager?.requestPeers(channel) { peerList ->
            val devices = peerList.deviceList.toList()
            _discoveredDevices.value = devices

            if (devices.isNotEmpty()) {
                _discoveryState.value = DiscoveryState.DEVICES_FOUND
                Log.d(TAG, "👥 Found ${devices.size} device(s):")
                devices.forEach { device ->
                    Log.d(
                        TAG,
                        "   - ${device.deviceName} (${device.deviceAddress}) - Status: ${
                            getDeviceStatus(
                                device.status
                            )
                        }"
                    )
                    onDeviceDiscovered?.invoke(device)
                }
            } else if (isDiscovering) {
                _discoveryState.value = DiscoveryState.DISCOVERING
                Log.d(TAG, "👥 No devices found yet, continuing discovery...")
            }
        }
    }

    /**
     * Get device status string
     */
    private fun getDeviceStatus(status: Int): String {
        return when (status) {
            WifiP2pDevice.CONNECTED -> "CONNECTED"
            WifiP2pDevice.INVITED -> "INVITED"
            WifiP2pDevice.FAILED -> "FAILED"
            WifiP2pDevice.AVAILABLE -> "AVAILABLE"
            WifiP2pDevice.UNAVAILABLE -> "UNAVAILABLE"
            else -> "UNKNOWN"
        }
    }

    /**
     * Connect to a specific device
     */
    fun connectToDevice(device: WifiP2pDevice) {
        Log.d(TAG, "🔗 Connecting to ${device.deviceName}...")

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = android.net.wifi.WpsInfo.PBC
        }

        wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "✅ Connection initiated to ${device.deviceName}")
                _discoveryState.value = DiscoveryState.CONNECTING
            }

            override fun onFailure(reason: Int) {
                val errorMsg = getFailureReason(reason)
                Log.e(TAG, "❌ Connection failed: $errorMsg")
                _discoveryState.value = DiscoveryState.ERROR
                onError?.invoke("Connection failed: $errorMsg")
            }
        })
    }

    /**
     * Start server (Group Owner) to receive files
     */
    private fun startServer() {
        if (isServerRunning) {
            Log.d(TAG, "⚠️ Server already running")
            return
        }

        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                isServerRunning = true
                Log.d(TAG, "🖥️ Server started on port $PORT")

                while (isServerRunning) {
                    try {
                        Log.d(TAG, "⏳ Waiting for client connection...")
                        val client = serverSocket?.accept()

                        if (client != null) {
                            Log.d(TAG, "✅ Client connected: ${client.inetAddress.hostAddress}")
                            handleClient(client)
                        }
                    } catch (e: Exception) {
                        if (isServerRunning) {
                            Log.e(TAG, "❌ Server error: ${e.message}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start server: ${e.message}", e)
                onError?.invoke("Failed to start server: ${e.message}")
            }
        }
    }

    /**
     * Handle incoming client connection
     */
    private suspend fun handleClient(client: Socket) = withContext(Dispatchers.IO) {
        try {
            val inputStream = client.getInputStream()
            val dataInputStream = DataInputStream(inputStream)

            // Read file count
            val fileCount = dataInputStream.readInt()
            Log.d(TAG, "📦 Receiving $fileCount file(s)")

            repeat(fileCount) { index ->
                // Read file metadata
                val fileName = dataInputStream.readUTF()
                val fileSize = dataInputStream.readLong()

                Log.d(TAG, "📥 Receiving file ${index + 1}/$fileCount: $fileName ($fileSize bytes)")

                // Create output file
                val outputFile =
                    File(context.getExternalFilesDir(null), "SafeSphereShare/$fileName")
                outputFile.parentFile?.mkdirs()

                // Receive file with progress
                FileOutputStream(outputFile).use { fos ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesReceived = 0L
                    var lastProgressUpdate = System.currentTimeMillis()
                    val startTime = System.currentTimeMillis()

                    while (bytesReceived < fileSize) {
                        val remaining = (fileSize - bytesReceived).toInt()
                        val toRead = minOf(remaining, BUFFER_SIZE)
                        val read = inputStream.read(buffer, 0, toRead)

                        if (read == -1) break

                        fos.write(buffer, 0, read)
                        bytesReceived += read

                        // Update progress every 100ms
                        val now = System.currentTimeMillis()
                        if (now - lastProgressUpdate >= 100) {
                            val progress = bytesReceived.toFloat() / fileSize
                            val elapsed = now - startTime
                            val speed = if (elapsed > 0) bytesReceived * 1000 / elapsed else 0

                            _transferProgress.value = TransferProgress(
                                currentFile = fileName,
                                currentFileIndex = index,
                                totalFiles = fileCount,
                                progress = progress,
                                bytesTransferred = bytesReceived,
                                totalBytes = fileSize,
                                speed = speed
                            )

                            lastProgressUpdate = now
                        }
                    }
                }

                Log.d(TAG, "✅ File received: $fileName")
                onFileReceived?.invoke(
                    File(
                        context.getExternalFilesDir(null),
                        "SafeSphereShare/$fileName"
                    )
                )
            }

            Log.d(TAG, "🎉 All files received successfully")
            onTransferComplete?.invoke()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling client: ${e.message}", e)
        } finally {
            client.close()
        }
    }

    /**
     * Send files to connected device (as client)
     */
    suspend fun sendFiles(files: List<ShareFile>) = withContext(Dispatchers.IO) {
        try {
            val info = _connectionInfo.value
            if (info == null || !info.groupFormed) {
                Log.e(TAG, "❌ Not connected to any device")
                return@withContext false
            }

            // Connect to group owner
            val hostAddress = info.groupOwnerAddress.hostAddress
            Log.d(TAG, "📤 Connecting to server at $hostAddress:$PORT")

            clientSocket = Socket()
            clientSocket?.connect(InetSocketAddress(info.groupOwnerAddress, PORT), 5000)

            Log.d(TAG, "✅ Connected to server")

            val outputStream = clientSocket?.getOutputStream()
            val dataOutputStream = DataOutputStream(outputStream)

            // Send file count
            dataOutputStream.writeInt(files.size)
            Log.d(TAG, "📦 Sending ${files.size} file(s)")

            files.forEachIndexed { index, shareFile ->
                // Open file
                val inputStream = context.contentResolver.openInputStream(shareFile.uri)
                    ?: throw IOException("Cannot open file: ${shareFile.name}")

                // Send file metadata
                dataOutputStream.writeUTF(shareFile.name)
                dataOutputStream.writeLong(shareFile.size)

                Log.d(
                    TAG,
                    "📤 Sending file ${index + 1}/${files.size}: ${shareFile.name} (${shareFile.size} bytes)"
                )

                // Send file data with progress
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesSent = 0L
                var lastProgressUpdate = System.currentTimeMillis()
                val startTime = System.currentTimeMillis()

                inputStream.use { input ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        outputStream?.write(buffer, 0, read)
                        bytesSent += read

                        // Update progress every 100ms
                        val now = System.currentTimeMillis()
                        if (now - lastProgressUpdate >= 100) {
                            val progress = bytesSent.toFloat() / shareFile.size
                            val elapsed = now - startTime
                            val speed = if (elapsed > 0) bytesSent * 1000 / elapsed else 0

                            _transferProgress.value = TransferProgress(
                                currentFile = shareFile.name,
                                currentFileIndex = index,
                                totalFiles = files.size,
                                progress = progress,
                                bytesTransferred = bytesSent,
                                totalBytes = shareFile.size,
                                speed = speed
                            )

                            lastProgressUpdate = now
                        }
                    }
                }

                outputStream?.flush()
                Log.d(TAG, "✅ File sent: ${shareFile.name}")
            }

            Log.d(TAG, "🎉 All files sent successfully")
            onTransferComplete?.invoke()

            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending files: ${e.message}", e)
            return@withContext false
        } finally {
            clientSocket?.close()
        }
    }

    /**
     * Disconnect from current connection
     */
    fun disconnect() {
        wifiP2pManager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "✅ Disconnected successfully")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "❌ Disconnect failed: ${getFailureReason(reason)}")
            }
        })

        _connectionInfo.value = null
        stopServer()
    }

    /**
     * Stop server
     */
    private fun stopServer() {
        isServerRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server: ${e.message}")
        }
        serverSocket = null
        Log.d(TAG, "🛑 Server stopped")
    }

    /**
     * Get failure reason string
     */
    private fun getFailureReason(reason: Int): String {
        return when (reason) {
            WifiP2pManager.P2P_UNSUPPORTED -> "WiFi Direct not supported"
            WifiP2pManager.ERROR -> "Internal error"
            WifiP2pManager.BUSY -> "Device busy, try again"
            else -> "Unknown error ($reason)"
        }
    }

    /**
     * Cleanup
     */
    fun cleanup() {
        try {
            context.unregisterReceiver(wifiP2pReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }

        stopDiscovery()
        stopServer()
        disconnect()
        scope.cancel()

        Log.d(TAG, "🧹 WiFi Direct service cleaned up")
    }

    companion object {
        private const val TAG = "WiFiDirectService"
    }
}

/**
 * Transfer Progress - Real-time transfer progress
 */
data class TransferProgress(
    val currentFile: String,
    val currentFileIndex: Int,
    val totalFiles: Int,
    val progress: Float, // 0.0 to 1.0
    val bytesTransferred: Long,
    val totalBytes: Long,
    val speed: Long // bytes per second
)

/**
 * Discovery State - Current discovery status
 */
enum class DiscoveryState {
    IDLE,               // Not discovering
    WIFI_DISABLED,      // WiFi is off
    DISCOVERING,        // Actively discovering
    DEVICES_FOUND,      // Found devices
    CONNECTING,         // Connecting to device
    CONNECTED,          // Connected successfully
    ERROR               // Error occurred
}
