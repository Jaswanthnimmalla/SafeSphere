package com.runanywhere.startup_hackathon20.share

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * SafeSphere Share Repository - Manages file sharing operations
 *
 * Features:
 * - Device discovery (Bluetooth + Wi-Fi Direct)
 * - File selection and management
 * - Transfer operations
 * - History tracking
 * - State management
 */
class SafeSphereShareRepository private constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = context.getSharedPreferences("safesphere_share", Context.MODE_PRIVATE)

    // Wi-Fi Direct Service
    private val wifiDirectService = WiFiDirectService(context)

    // State flows
    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _discoveredDevices = MutableStateFlow<List<NearbyDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<NearbyDevice>> = _discoveredDevices

    private val _selectedFiles = MutableStateFlow<List<ShareFile>>(emptyList())
    val selectedFiles: StateFlow<List<ShareFile>> = _selectedFiles

    private val _currentTransfer = MutableStateFlow<TransferSession?>(null)
    val currentTransfer: StateFlow<TransferSession?> = _currentTransfer

    private val _transferHistory = MutableStateFlow<List<ShareHistoryItem>>(emptyList())
    val transferHistory: StateFlow<List<ShareHistoryItem>> = _transferHistory

    private val _incomingRequest = MutableStateFlow<TransferRequest?>(null)
    val incomingRequest: StateFlow<TransferRequest?> = _incomingRequest

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        setupWiFiDirectCallbacks()
        loadTransferHistory()
    }

    /**
     * Setup Wi-Fi Direct callbacks
     */
    private fun setupWiFiDirectCallbacks() {
        wifiDirectService.onDeviceDiscovered = { device ->
            handleDeviceDiscovered(device)
        }

        wifiDirectService.onConnectionEstablished = { info ->
            Log.d(TAG, "✅ Connection established: ${info.groupOwnerAddress}")
            updateConnectionState()
        }

        wifiDirectService.onFileReceived = { file ->
            Log.d(TAG, "📥 File received: ${file.name}")
            handleFileReceived(file)
        }

        wifiDirectService.onTransferComplete = {
            Log.d(TAG, "🎉 Transfer complete")
            handleTransferComplete()
        }

        wifiDirectService.onError = { error ->
            Log.e(TAG, "❌ WiFi Direct error: $error")
            _errorMessage.value = error
        }
    }

    /**
     * Start discovering nearby devices
     */
    fun startDiscovery() {
        Log.d(TAG, "🔍 Starting device discovery")

        _connectionState.value = _connectionState.value.copy(isDiscovering = true)
        _errorMessage.value = null // Clear previous errors
        wifiDirectService.startDiscovery()

        // Auto-stop discovery after 30 seconds
        scope.launch {
            delay(30000)
            if (_connectionState.value.isDiscovering) {
                stopDiscovery()
            }
        }
    }

    /**
     * Stop device discovery
     */
    fun stopDiscovery() {
        Log.d(TAG, "🛑 Stopping device discovery")
        wifiDirectService.stopDiscovery()
        _connectionState.value = _connectionState.value.copy(isDiscovering = false)
    }

    /**
     * Handle discovered device
     */
    private fun handleDeviceDiscovered(device: WifiP2pDevice) {
        val nearbyDevice = NearbyDevice(
            deviceId = device.deviceAddress,
            deviceName = device.deviceName ?: "Unknown Device",
            deviceModel = null,
            isAvailable = device.status == WifiP2pDevice.AVAILABLE,
            signalStrength = 80, // Estimate based on discovery
            connectionType = ConnectionType.WIFI_DIRECT,
            distance = DeviceDistance.NEARBY
        )

        // Add to discovered devices if not already present
        val currentDevices = _discoveredDevices.value.toMutableList()
        val existingIndex = currentDevices.indexOfFirst { it.deviceId == nearbyDevice.deviceId }

        if (existingIndex != -1) {
            currentDevices[existingIndex] = nearbyDevice
        } else {
            currentDevices.add(nearbyDevice)
        }

        _discoveredDevices.value = currentDevices
        Log.d(TAG, "📱 Device discovered: ${nearbyDevice.deviceName}")
    }

    /**
     * Connect to a device
     */
    fun connectToDevice(device: NearbyDevice) {
        Log.d(TAG, "🔗 Connecting to ${device.deviceName}")

        // Find Wi-Fi P2P device
        val wifiDevice = wifiDirectService.discoveredDevices.value.find {
            it.deviceAddress == device.deviceId
        }

        if (wifiDevice != null) {
            wifiDirectService.connectToDevice(wifiDevice)
            _connectionState.value = _connectionState.value.copy(
                connectedDevice = device,
                connectionType = ConnectionType.WIFI_DIRECT
            )
        } else {
            Log.e(TAG, "❌ Device not found in Wi-Fi Direct list")
        }
    }

    /**
     * Disconnect from current device
     */
    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting")
        wifiDirectService.disconnect()
        _connectionState.value = _connectionState.value.copy(
            connectedDevice = null,
            connectionType = null
        )
    }

    /**
     * Select files from URI
     */
    fun selectFiles(uris: List<Uri>) {
        scope.launch {
            val files = uris.mapNotNull { uri ->
                getFileInfo(uri)
            }

            _selectedFiles.value = files
            Log.d(TAG, "📂 Selected ${files.size} file(s)")
        }
    }

    /**
     * Get file info from URI
     */
    private fun getFileInfo(uri: Uri): ShareFile? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

                    val name = if (nameIndex != -1) it.getString(nameIndex) else "Unknown"
                    val size = if (sizeIndex != -1) it.getLong(sizeIndex) else 0L

                    val mimeType =
                        context.contentResolver.getType(uri) ?: "application/octet-stream"
                    val fileType = FileType.fromMimeType(mimeType)

                    ShareFile(
                        uri = uri,
                        name = name,
                        size = size,
                        mimeType = mimeType,
                        fileType = fileType,
                        thumbnail = if (fileType == FileType.IMAGE) uri else null
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file info: ${e.message}")
            null
        }
    }

    /**
     * Load recent photos
     */
    suspend fun loadRecentPhotos(limit: Int = 20): List<ShareFile> = withContext(Dispatchers.IO) {
        try {
            val photos = mutableListOf<ShareFile>()
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE
            )

            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )

            cursor?.use {
                var count = 0
                while (it.moveToNext() && count < limit) {
                    val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    val name =
                        it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                    val size = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                    val mimeType =
                        it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))

                    val uri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )

                    photos.add(
                        ShareFile(
                            uri = uri,
                            name = name,
                            size = size,
                            mimeType = mimeType,
                            fileType = FileType.IMAGE,
                            thumbnail = uri
                        )
                    )
                    count++
                }
            }

            photos
        } catch (e: Exception) {
            Log.e(TAG, "Error loading photos: ${e.message}")
            emptyList()
        }
    }

    /**
     * Send selected files to connected device
     */
    suspend fun sendFiles(): Boolean {
        val files = _selectedFiles.value
        if (files.isEmpty()) {
            Log.e(TAG, "❌ No files selected")
            return false
        }

        val device = _connectionState.value.connectedDevice
        if (device == null) {
            Log.e(TAG, "❌ No device connected")
            return false
        }

        Log.d(TAG, "📤 Sending ${files.size} file(s) to ${device.deviceName}")

        // Create transfer session
        val totalSize = files.sumOf { it.size }
        val session = TransferSession(
            files = files,
            receiver = device,
            sender = null,
            status = TransferStatus.TRANSFERRING,
            totalBytes = totalSize,
            isSender = true
        )
        _currentTransfer.value = session

        // Send files via Wi-Fi Direct
        return try {
            val success = wifiDirectService.sendFiles(files)

            if (success) {
                _currentTransfer.value = session.copy(
                    status = TransferStatus.COMPLETED,
                    progress = 1f,
                    endTime = System.currentTimeMillis()
                )

                // Add to history
                addToHistory(session.copy(status = TransferStatus.COMPLETED))

                Log.d(TAG, "✅ Transfer successful")
                true
            } else {
                _currentTransfer.value = session.copy(
                    status = TransferStatus.FAILED,
                    errorMessage = "Transfer failed"
                )
                Log.e(TAG, "❌ Transfer failed")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Transfer error: ${e.message}")
            _currentTransfer.value = session.copy(
                status = TransferStatus.FAILED,
                errorMessage = e.message
            )
            false
        }
    }

    /**
     * Handle file received
     */
    private fun handleFileReceived(file: File) {
        // Update current transfer if active
        val current = _currentTransfer.value
        if (current != null && current.status == TransferStatus.TRANSFERRING) {
            _currentTransfer.value = current.copy(
                currentFileIndex = current.currentFileIndex + 1
            )
        }
    }

    /**
     * Handle transfer complete
     */
    private fun handleTransferComplete() {
        val current = _currentTransfer.value
        if (current != null) {
            val completedSession = current.copy(
                status = TransferStatus.COMPLETED,
                progress = 1f,
                endTime = System.currentTimeMillis()
            )
            _currentTransfer.value = completedSession

            // Add to history
            addToHistory(completedSession)
        }
    }

    /**
     * Cancel current transfer
     */
    fun cancelTransfer() {
        Log.d(TAG, "❌ Cancelling transfer")

        val current = _currentTransfer.value
        if (current != null) {
            _currentTransfer.value = current.copy(
                status = TransferStatus.CANCELLED
            )
        }

        disconnect()
    }

    /**
     * Clear selected files
     */
    fun clearSelectedFiles() {
        _selectedFiles.value = emptyList()
    }

    /**
     * Add session to history
     */
    private fun addToHistory(session: TransferSession) {
        val historyItem = ShareHistoryItem(
            id = session.id,
            deviceName = (session.receiver?.deviceName ?: session.sender?.deviceName) ?: "Unknown",
            files = session.files,
            totalSize = session.totalBytes,
            timestamp = session.startTime,
            status = session.status,
            isSent = session.isSender,
            duration = (session.endTime ?: System.currentTimeMillis()) - session.startTime
        )

        _transferHistory.value = listOf(historyItem) + _transferHistory.value.take(99)
        saveTransferHistory()
    }

    /**
     * Save transfer history to storage
     */
    private fun saveTransferHistory() {
        scope.launch {
            try {
                val json = JSONArray()
                _transferHistory.value.take(50).forEach { item ->
                    json.put(JSONObject().apply {
                        put("id", item.id)
                        put("deviceName", item.deviceName)
                        put("totalSize", item.totalSize)
                        put("timestamp", item.timestamp)
                        put("status", item.status.name)
                        put("isSent", item.isSent)
                        put("duration", item.duration)
                        put("fileCount", item.files.size)
                    })
                }
                prefs.edit().putString("transfer_history", json.toString()).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving history: ${e.message}")
            }
        }
    }

    /**
     * Load transfer history from storage
     */
    private fun loadTransferHistory() {
        scope.launch {
            try {
                val historyJson = prefs.getString("transfer_history", null)
                if (historyJson != null) {
                    val json = JSONArray(historyJson)
                    val history = mutableListOf<ShareHistoryItem>()

                    for (i in 0 until json.length()) {
                        val item = json.getJSONObject(i)
                        history.add(
                            ShareHistoryItem(
                                id = item.getString("id"),
                                deviceName = item.getString("deviceName"),
                                files = emptyList(), // Files not stored
                                totalSize = item.getLong("totalSize"),
                                timestamp = item.getLong("timestamp"),
                                status = TransferStatus.valueOf(item.getString("status")),
                                isSent = item.getBoolean("isSent"),
                                duration = item.getLong("duration")
                            )
                        )
                    }

                    _transferHistory.value = history
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading history: ${e.message}")
            }
        }
    }

    /**
     * Update connection state from Wi-Fi Direct service
     */
    private fun updateConnectionState() {
        scope.launch {
            _connectionState.value = _connectionState.value.copy(
                isWiFiDirectEnabled = wifiDirectService.isEnabled.value
            )
        }
    }

    /**
     * Format file size
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    /**
     * Format transfer speed
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
            bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024} KB/s"
            else -> "${bytesPerSecond / (1024 * 1024)} MB/s"
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Cleanup
     */
    fun cleanup() {
        wifiDirectService.cleanup()
        scope.cancel()
    }

    companion object {
        private const val TAG = "ShareRepository"

        @Volatile
        private var INSTANCE: SafeSphereShareRepository? = null

        fun getInstance(context: Context): SafeSphereShareRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SafeSphereShareRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
