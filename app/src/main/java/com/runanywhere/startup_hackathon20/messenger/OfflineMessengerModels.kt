package com.runanywhere.startup_hackathon20.messenger

import androidx.compose.runtime.Stable

/**
 * Message Type - Supports various media types
 */
enum class MessageType {
    TEXT,
    IMAGE,
    VOICE,
    DOCUMENT,
    VIDEO
}

/**
 * Message Status - Complete delivery tracking
 */
enum class MessageStatus {
    SENDING,      // Currently being sent
    SENT,         // Sent from device
    DELIVERED,    // Delivered to receiver
    READ,         // Read by receiver
    FAILED        // Failed to send
}

/**
 * Chat Message - Core message entity
 */
@Stable
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val content: String,
    val timestamp: Long,
    val senderId: String,
    val receiverId: String,
    val isSent: Boolean,
    val status: MessageStatus,
    val messageType: MessageType = MessageType.TEXT,

    // Media fields
    val mediaUri: String? = null,
    val mediaFileName: String? = null,
    val mediaFileSize: Long? = null,
    val mediaMimeType: String? = null,
    val mediaThumbnail: String? = null,

    // Voice message fields
    val voiceDuration: Int? = null, // in seconds

    // Metadata
    val isStarred: Boolean = false,
    val replyToMessageId: String? = null
)

/**
 * Conversation - Represents a chat with a contact
 */
@Stable
data class Conversation(
    val id: String, // Contact's normalized phone
    val contactName: String,
    val contactPhone: String,
    val contactAvatar: String? = null,
    val lastMessage: ChatMessage? = null,
    val unreadCount: Int = 0,
    val lastActivity: Long = 0L,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false
)

/**
 * Contact - Phone contact with messenger availability
 */
@Stable
data class MessengerContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val normalizedPhone: String,
    val avatar: String? = null,
    val isOnline: Boolean = false
)

/**
 * Bluetooth Device Info - Connected device details
 */
@Stable
data class BluetoothDeviceInfo(
    val address: String,
    val name: String?,
    val isConnected: Boolean = false,
    val connectionTime: Long = 0L
)

/**
 * Bluetooth Packet - Data transmission unit
 */
data class BluetoothPacket(
    val type: PacketType,
    val senderId: String,
    val receiverId: String,
    val messageId: String,
    val timestamp: Long,
    val payload: String,

    // For chunked file transfer
    val chunkIndex: Int = 0,
    val totalChunks: Int = 1,
    val fileMetadata: FileMetadata? = null
)

/**
 * Packet Type - Different packet types for transmission
 */
enum class PacketType {
    TEXT_MESSAGE,
    FILE_METADATA,
    FILE_CHUNK,
    DELIVERY_RECEIPT,
    READ_RECEIPT,
    TYPING_INDICATOR,
    PRESENCE
}

/**
 * File Metadata - Information about file being transferred
 */
data class FileMetadata(
    val messageId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val messageType: MessageType,
    val caption: String = ""
)

/**
 * File Transfer Progress - Track file upload/download
 */
@Stable
data class FileTransferProgress(
    val messageId: String,
    val fileName: String,
    val progress: Float, // 0.0 to 1.0
    val isUploading: Boolean,
    val bytesTransferred: Long,
    val totalBytes: Long
)
