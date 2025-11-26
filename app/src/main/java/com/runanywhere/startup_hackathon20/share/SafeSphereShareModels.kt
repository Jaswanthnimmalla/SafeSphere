package com.runanywhere.startup_hackathon20.share

import android.net.Uri
import androidx.compose.runtime.Stable

/**
 * Transfer Status - Track file transfer state
 */
enum class TransferStatus {
    DISCOVERING,      // Discovering nearby devices
    CONNECTING,       // Establishing connection
    WAITING,          // Waiting for acceptance
    TRANSFERRING,     // File transfer in progress
    COMPLETED,        // Transfer successful
    FAILED,           // Transfer failed
    CANCELLED,        // Transfer cancelled
    PAUSED            // Transfer paused
}

/**
 * File Type - Supported file types
 */
enum class FileType {
    IMAGE,
    VIDEO,
    DOCUMENT,
    APK,
    AUDIO,
    OTHER;

    companion object {
        fun fromMimeType(mimeType: String): FileType {
            return when {
                mimeType.startsWith("image/") -> IMAGE
                mimeType.startsWith("video/") -> VIDEO
                mimeType.startsWith("audio/") -> AUDIO
                mimeType.startsWith("application/pdf") -> DOCUMENT
                mimeType.startsWith("application/msword") -> DOCUMENT
                mimeType.startsWith("application/vnd.") -> DOCUMENT
                mimeType == "application/vnd.android.package-archive" -> APK
                else -> OTHER
            }
        }
    }
}

/**
 * Nearby Device - Discovered device info
 */
@Stable
data class NearbyDevice(
    val deviceId: String,
    val deviceName: String,
    val deviceModel: String?,
    val isAvailable: Boolean = true,
    val signalStrength: Int = 0, // 0-100
    val lastSeen: Long = System.currentTimeMillis(),
    val connectionType: ConnectionType = ConnectionType.BLUETOOTH,
    val distance: DeviceDistance = DeviceDistance.NEARBY
)

/**
 * Connection Type - How devices are connected
 */
enum class ConnectionType {
    BLUETOOTH,        // Discovery only
    WIFI_DIRECT,      // High-speed transfer
    HOTSPOT,          // Fallback option
    COMBINED          // BT discovery + WiFi Direct transfer
}

/**
 * Device Distance - Estimated distance
 */
enum class DeviceDistance(val displayName: String) {
    VERY_CLOSE("Very Close"),
    NEARBY("Nearby"),
    FAR("Far"),
    UNKNOWN("Unknown")
}

/**
 * File to Share - File metadata
 */
@Stable
data class ShareFile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val fileType: FileType,
    val thumbnail: Uri? = null,
    val selected: Boolean = false
)

/**
 * Transfer Session - Complete transfer details
 */
@Stable
data class TransferSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val files: List<ShareFile>,
    val receiver: NearbyDevice?,
    val sender: NearbyDevice?,
    val status: TransferStatus,
    val progress: Float = 0f, // 0.0 to 1.0
    val bytesTransferred: Long = 0L,
    val totalBytes: Long,
    val currentFileIndex: Int = 0,
    val speed: Long = 0L, // bytes per second
    val timeRemaining: Long = 0L, // milliseconds
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val errorMessage: String? = null,
    val isSender: Boolean = true
)

/**
 * Transfer Request - Incoming transfer request
 */
@Stable
data class TransferRequest(
    val id: String,
    val senderDevice: NearbyDevice,
    val files: List<ShareFile>,
    val totalSize: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 60000, // 1 minute
    val message: String? = null
)

/**
 * Share History Item - Past transfers
 */
@Stable
data class ShareHistoryItem(
    val id: String,
    val deviceName: String,
    val files: List<ShareFile>,
    val totalSize: Long,
    val timestamp: Long,
    val status: TransferStatus,
    val isSent: Boolean,
    val duration: Long // milliseconds
)

/**
 * Connection State - Current connection status
 */
@Stable
data class ConnectionState(
    val isBluetoothEnabled: Boolean = false,
    val isWiFiDirectEnabled: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val connectedDevice: NearbyDevice? = null,
    val connectionType: ConnectionType? = null,
    val isDiscovering: Boolean = false,
    val discoveredDevices: List<NearbyDevice> = emptyList()
)

/**
 * Share Packet - Network transmission unit
 */
data class SharePacket(
    val type: PacketType,
    val sessionId: String,
    val senderId: String,
    val receiverId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val payload: ByteArray,
    val metadata: Map<String, String> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SharePacket

        if (type != other.type) return false
        if (sessionId != other.sessionId) return false
        if (senderId != other.senderId) return false
        if (receiverId != other.receiverId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + sessionId.hashCode()
        result = 31 * result + senderId.hashCode()
        result = 31 * result + receiverId.hashCode()
        return result
    }
}

/**
 * Packet Type - Different packet types
 */
enum class PacketType {
    DISCOVERY_REQUEST,
    DISCOVERY_RESPONSE,
    TRANSFER_REQUEST,
    TRANSFER_ACCEPT,
    TRANSFER_REJECT,
    FILE_METADATA,
    FILE_CHUNK,
    TRANSFER_COMPLETE,
    TRANSFER_CANCEL,
    HEARTBEAT,
    ERROR
}
