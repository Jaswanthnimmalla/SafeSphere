package com.runanywhere.startup_hackathon20.ui

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.messenger.*
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Offline Messenger Screen - Complete WhatsApp-like UI with error handling
 */
@Composable
fun OfflineMessengerScreen(
    viewModel: SafeSphereViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Safe repository initialization with error handling
    val repository = remember {
        try {
            OfflineMessengerRepository.getInstance(context)
        } catch (e: Exception) {
            Log.e("OfflineMessenger", "Failed to initialize repository: ${e.message}")
            null
        }
    }

    // Show error if repository failed to initialize
    if (repository == null) {
        ErrorScreen(
            message = "Failed to initialize messenger. Please restart the app.",
            onNavigateBack = onNavigateBack
        )
        return
    }

    // State
    val conversations by repository.conversations.collectAsState()
    val contacts by repository.contacts.collectAsState()
    val connectedDevices by repository.connectedDevices.collectAsState()

    var selectedConversation by remember { mutableStateOf<Conversation?>(null) }
    var showContactsDialog by remember { mutableStateOf(false) }
    var hasPermissions by remember { mutableStateOf(false) }

    // Request permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        hasPermissions = granted
        if (granted) {
            try {
                repository.startBluetoothService()
            } catch (e: Exception) {
                Log.e("OfflineMessenger", "Failed to start Bluetooth: ${e.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            // Request Bluetooth permissions
            val perms = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            }
            permissionLauncher.launch(perms)

            // Load contacts with error handling
            delay(500)
            try {
                repository.loadContactsFromPhone()
            } catch (e: Exception) {
                Log.e("OfflineMessenger", "Failed to load contacts: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("OfflineMessenger", "Initialization error: ${e.message}")
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B141A))) {
        if (selectedConversation == null) {
            // Conversation list
            ConversationListScreen(
                conversations = conversations,
                connectedDevices = connectedDevices,
                onConversationClick = { selectedConversation = it },
                onNewChatClick = {
                    if (contacts.isNotEmpty()) {
                        showContactsDialog = true
                    }
                },
                onNavigateBack = onNavigateBack
            )
        } else {
            // Chat screen
            ChatScreen(
                conversation = selectedConversation!!,
                repository = repository,
                onBack = {
                    try {
                        repository.markConversationAsClosed(selectedConversation!!.id)
                    } catch (e: Exception) {
                        Log.e("OfflineMessenger", "Error closing conversation: ${e.message}")
                    }
                    selectedConversation = null
                }
            )
        }

        // Contacts dialog
        if (showContactsDialog) {
            ContactsDialog(
                contacts = contacts,
                onContactSelected = { contact ->
                    try {
                        val conversation =
                            repository.getOrCreateConversation(contact.normalizedPhone)
                        selectedConversation = conversation
                    } catch (e: Exception) {
                        Log.e("OfflineMessenger", "Error creating conversation: ${e.message}")
                    }
                    showContactsDialog = false
                },
                onDismiss = { showContactsDialog = false }
            )
        }
    }
}

/**
 * Conversation List Screen
 */
@Composable
private fun ConversationListScreen(
    conversations: List<Conversation>,
    connectedDevices: List<BluetoothDeviceInfo>,
    onConversationClick: (Conversation) -> Unit,
    onNewChatClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF202C33),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "SafeSphere",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${connectedDevices.size} connected",
                    fontSize = 12.sp,
                    color = if (connectedDevices.isNotEmpty()) Color(0xFF25D366) else Color.Gray
                )
            }
        }

        // Conversations
        if (conversations.isEmpty()) {
            EmptyState(onNewChatClick)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(conversations) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        onClick = { onConversationClick(conversation) }
                    )
                }
            }
        }

        // FAB
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                onClick = onNewChatClick,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                containerColor = Color(0xFF25D366)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Color.White)
            }
        }
    }
}

/**
 * Conversation Item
 */
@Composable
private fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Color(0xFF0B141A)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape)
                    .background(Color(0xFF25D366)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.contactName.take(1).uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = conversation.contactName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    conversation.lastMessage?.let {
                        Text(
                            text = formatMessageTime(it.timestamp),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                conversation.lastMessage?.let { lastMsg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lastMsg.content,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (conversation.unreadCount > 0) {
                            Box(
                                modifier = Modifier.size(24.dp).clip(CircleShape)
                                    .background(Color(0xFF25D366)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = conversation.unreadCount.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chat Screen
 */
@Composable
private fun ChatScreen(
    conversation: Conversation,
    repository: OfflineMessengerRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val messages by repository.messages.collectAsState()
    val conversationMessages = messages[conversation.id] ?: emptyList()
    val listState = rememberLazyListState()

    var messageText by remember { mutableStateOf("") }

    // Mark as read when opened
    LaunchedEffect(conversation.id) {
        try {
            repository.markConversationAsRead(conversation.id)
        } catch (e: Exception) {
            Log.e("OfflineMessenger", "Error marking conversation as read: ${e.message}")
        }
    }

    // Scroll to bottom
    LaunchedEffect(conversationMessages.size) {
        if (conversationMessages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF202C33),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(Color(0xFF25D366)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        conversation.contactName.take(1).uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = conversation.contactName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Messages
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .background(Color(0xFF0B141A))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true,
                contentPadding = PaddingValues(16.dp)
            ) {
                items(conversationMessages.asReversed()) { message ->
                    MessageBubble(message)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Input bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF202C33)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF2A3942)).padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = SolidColor(Color(0xFF25D366)),
                        decorationBox = { innerTextField ->
                            if (messageText.isEmpty()) {
                                Text("Type a message", color = Color.Gray, fontSize = 16.sp)
                            }
                            innerTextField()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            scope.launch {
                                try {
                                    repository.sendMessage(conversation.id, messageText)
                                } catch (e: Exception) {
                                    Log.e("OfflineMessenger", "Error sending message: ${e.message}")
                                }
                                messageText = ""
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = Color(0xFF25D366)
                ) {
                    Icon(Icons.Default.Send, "Send", tint = Color.White)
                }
            }
        }
    }
}

/**
 * Message Bubble
 */
@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isSent) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier.widthIn(max = 280.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (message.isSent) Color(0xFF005C4B) else Color(0xFF202C33))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    fontSize = 15.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(Date(message.timestamp)),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    if (message.isSent) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (message.status) {
                                MessageStatus.SENDING -> "⏱"
                                MessageStatus.SENT -> "✓"
                                MessageStatus.DELIVERED -> "✓✓"
                                MessageStatus.READ -> "✓✓"
                                MessageStatus.FAILED -> "✗"
                            },
                            fontSize = 16.sp,
                            color = if (message.status == MessageStatus.READ)
                                Color(0xFF34B7F1) else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

/**
 * Contacts Dialog
 */
@Composable
private fun ContactsDialog(
    contacts: List<MessengerContact>,
    onContactSelected: (MessengerContact) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Contact") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                items(contacts) { contact ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onContactSelected(contact) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(Color(0xFF25D366)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                contact.name.take(1).uppercase(),
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(contact.name, fontWeight = FontWeight.Bold)
                            Text(contact.phoneNumber, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Empty State
 */
@Composable
private fun EmptyState(onNewChatClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💬", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No conversations yet", fontSize = 20.sp, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Start chatting offline", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNewChatClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Chat")
        }
    }
}

/**
 * Error Screen
 */
@Composable
private fun ErrorScreen(
    message: String,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B141A)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNavigateBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
        ) {
            Text("Go Back")
        }
    }
}

/**
 * Format timestamp
 */
private fun formatMessageTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Now"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
