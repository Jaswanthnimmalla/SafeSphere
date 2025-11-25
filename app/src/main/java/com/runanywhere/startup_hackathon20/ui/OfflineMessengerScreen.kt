package com.runanywhere.startup_hackathon20.ui
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.runanywhere.startup_hackathon20.data.Message
import com.runanywhere.startup_hackathon20.data.MessageType
import com.runanywhere.startup_hackathon20.data.OfflineMessengerRepository
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Offline Messenger - Enhanced with Media Sharing
 *
 * Features:
 * - Chat with your contacts offline
 * - Share images, documents, videos
 * - WhatsApp-style modern UI
 * - End-to-end encrypted storage
 * - File attachments support
 */
@Composable
fun OfflineMessengerScreen(
    viewModel: SafeSphereViewModel,
    onNavigateBack: () -> Unit
) {
    val colors = SafeSphereThemeColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Use repository with safe initialization
    val repository = remember {
        try {
            OfflineMessengerRepository.getInstance(context)
        } catch (e: Exception) {
            android.util.Log.e("OfflineMessenger", "Failed to init repository: ${e.message}", e)
            null
        }
    }

    val conversations by remember(repository) {
        repository?.conversations ?: MutableStateFlow(emptyMap())
    }.collectAsState()

    // Advanced features state
    val nearbyDevices by remember(repository) {
        repository?.nearbyDevices ?: MutableStateFlow(emptyList())
    }.collectAsState()

    var pinnedChats by remember { mutableStateOf(setOf<String>()) }
    var archivedChats by remember { mutableStateOf(setOf<String>()) }
    var blockedContacts by remember { mutableStateOf(setOf<String>()) }
    var typingUsers by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var onlineUsers by remember { mutableStateOf(setOf<String>()) }
    var showNearbyDevices by remember { mutableStateOf(false) }
    var showArchivedChats by remember { mutableStateOf(false) }

    // Simple state management
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var showChatDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var bluetoothStatus by remember { mutableStateOf("⚪ Bluetooth Disabled") }

    // Permission state
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasBluetoothPermission by remember {
        mutableStateOf(false)
    }

    // Start Bluetooth server when permissions granted (only if repository available)
    LaunchedEffect(hasBluetoothPermission, repository) {
        if (repository != null && hasBluetoothPermission) {
            try {
                if (repository.isBluetoothAvailable()) {
                    repository.startBluetoothServer()
                    bluetoothStatus = "🟢 Bluetooth Active"
                } else {
                    bluetoothStatus = "⚪ Bluetooth Disabled"
                }
            } catch (e: Exception) {
                android.util.Log.e("OfflineMessenger", "Bluetooth error: ${e.message}")
                bluetoothStatus = "⚪ Bluetooth Disabled"
            }
        }
    }

    // Setup message received callback
    LaunchedEffect(repository) {
        repository?.onMessageReceived = { phone, message ->
            android.util.Log.d("OfflineMessenger", "Message received from $phone: $message")
        }
    }

    // Load contacts from phone
    LaunchedEffect(hasContactsPermission) {
        if (hasContactsPermission) {
            try {
                contacts = loadContactsFromPhone(context)
            } catch (e: Exception) {
                android.util.Log.e("OfflineMessenger", "Failed to load contacts: ${e.message}", e)
                contacts = emptyList()
            }
        }
    }

    // Permission launchers
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasBluetoothPermission = permissions.values.all { it }
    }

    // Request Bluetooth permissions on start (but don't crash if fails)
    LaunchedEffect(Unit) {
        try {
            if (!hasBluetoothPermission) {
                val permissions =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_ADVERTISE
                        )
                    } else {
                        arrayOf(
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    }
                bluetoothPermissionLauncher.launch(permissions)
            }
        } catch (e: Exception) {
            android.util.Log.e(
                "OfflineMessenger",
                "Failed to request Bluetooth permissions: ${e.message}",
                e
            )
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            // Don't cleanup repository as it's singleton
        }
    }

    // Filter contacts
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isEmpty()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.phone.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.background,
                        colors.backgroundDark
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Header
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00BCD4).copy(alpha = 0.15f),
                                        Color(0xFF2196F3).copy(alpha = 0.1f)
                                    )
                                )
                            )
                            .padding(28.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF00BCD4),
                                                Color(0xFF2196F3)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "💬", fontSize = 52.sp)
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Offline Messenger",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Chat with contacts • No internet needed",
                                fontSize = 15.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Bluetooth status indicator
                                Icon(
                                    imageVector = Icons.Default.Star, // Represents Bluetooth
                                    contentDescription = "Bluetooth",
                                    tint = if (bluetoothStatus.contains("Active")) Color(0xFF4CAF50) else Color.Gray,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = bluetoothStatus,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (bluetoothStatus.contains("Active")) Color(
                                                0xFF4CAF50
                                            ) else Color.Gray
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Encrypted Storage",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = "📱",
                        value = "${contacts.size}",
                        label = "Contacts",
                        color = Color(0xFF00BCD4),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = "💬",
                        value = "${conversations.size}",
                        label = "Chats",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = "📝",
                        value = "${conversations.values.sumOf { it.size }}",
                        label = "Messages",
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Nearby Devices Section
            if (hasBluetoothPermission && bluetoothStatus.contains("Active")) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNearbyDevices = !showNearbyDevices }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF4CAF50).copy(alpha = 0.1f),
                                            Color(0xFF00BCD4).copy(alpha = 0.1f)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "📡", fontSize = 20.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Nearby Devices",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "${repository?.getPairedDevices()?.size ?: 0} devices found",
                                            fontSize = 12.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (showNearbyDevices) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle",
                                    tint = colors.textSecondary
                                )
                            }

                            // Device list
                            if (showNearbyDevices) {
                                Spacer(modifier = Modifier.height(12.dp))
                                repository?.getPairedDevices()?.forEach { device ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4CAF50))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = device.name ?: "Unknown Device",
                                                    fontSize = 14.sp,
                                                    color = colors.textPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = device.address,
                                                    fontSize = 11.sp,
                                                    color = colors.textSecondary
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Connected",
                                            fontSize = 11.sp,
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            if (hasContactsPermission) {
                item {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        colors = colors
                    )
                }
            }

            // Recent Conversations
            if (conversations.isNotEmpty()) {
                item {
                    Text(
                        text = "💬 Recent Chats (${conversations.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                items(conversations.entries.toList()) { (phone, messages) ->
                    val contact = contacts.find { it.phone == phone }
                        ?: Contact("Unknown", phone, phone)
                    val lastMessage = messages.lastOrNull()

                    ConversationCard(
                        contact = contact,
                        lastMessage = lastMessage,
                        unreadCount = messages.count { !it.isRead && !it.isSent },
                        onClick = {
                            selectedContact = contact
                            showChatDialog = true
                        },
                        colors = colors,
                        isTyping = typingUsers[phone] ?: false,
                        isOnline = onlineUsers.contains(phone),
                        isPinned = pinnedChats.contains(phone)
                    )
                }
            }

            // Contacts List
            if (hasContactsPermission) {
                item {
                    Text(
                        text = "📇 All Contacts (${filteredContacts.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                items(filteredContacts) { contact ->
                    val hasConversation = conversations.containsKey(contact.phone)
                    ContactCard(
                        contact = contact,
                        hasConversation = hasConversation,
                        onClick = {
                            selectedContact = contact
                            showChatDialog = true
                        },
                        colors = colors
                    )
                }
            } else {
                // Permission Request
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "📱", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Access Your Contacts",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Grant contacts permission to start messaging your friends offline",
                                fontSize = 14.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00BCD4)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Chat Dialog
    if (showChatDialog && selectedContact != null) {
        ChatDialog(
            contact = selectedContact!!,
            messages = conversations[selectedContact!!.phone] ?: emptyList(),
            repository = repository,
            onDismiss = {
                showChatDialog = false
                repository?.markAsRead(selectedContact!!.phone)
            },
            colors = colors
        )
    }
}

// ==================== COMPOSABLE COMPONENTS ====================

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    colors: ThemeColors
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.5f))
            .border(
                1.dp,
                Color(0xFF00BCD4).copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(
                color = colors.textPrimary,
                fontSize = 16.sp
            ),
            cursorBrush = SolidColor(Color(0xFF00BCD4)),
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search contacts...",
                                fontSize = 16.sp,
                                color = colors.textSecondary.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatCard(
    icon: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = SafeSphereThemeColors
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.15f),
                            colors.surface
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun ConversationCard(
    contact: Contact,
    lastMessage: Message?,
    unreadCount: Int,
    onClick: () -> Unit,
    colors: ThemeColors,
    isTyping: Boolean = false,
    isOnline: Boolean = false,
    isPinned: Boolean = false
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isPinned) Color(0xFFFF9800).copy(alpha = 0.05f) else Color.Transparent
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00BCD4).copy(alpha = 0.3f),
                                    Color(0xFF00BCD4).copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00BCD4)
                    )
                }

                // Online indicator
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                            .border(2.dp, colors.surface, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Pinned",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = contact.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        isTyping -> "✍️ typing..."
                        lastMessage != null -> lastMessage.content.take(30)
                        else -> "No messages yet"
                    },
                    fontSize = 13.sp,
                    color = if (isTyping) Color(0xFF00BCD4) else colors.textSecondary,
                    maxLines = 1,
                    fontStyle = if (isTyping) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                )
            }

            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unreadCount.toString(),
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactCard(
    contact: Contact,
    hasConversation: Boolean,
    onClick: () -> Unit,
    colors: ThemeColors
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2196F3).copy(alpha = 0.3f),
                                Color(0xFF2196F3).copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.take(1).uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.phone,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }

            Icon(
                imageVector = if (hasConversation) Icons.Default.Send else Icons.Default.Add,
                contentDescription = "Chat",
                tint = if (hasConversation) Color(0xFF00BCD4) else colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ChatDialog(
    contact: Contact,
    messages: List<Message>,
    repository: OfflineMessengerRepository?,
    onDismiss: () -> Unit,
    colors: ThemeColors
) {
    var messageText by remember { mutableStateOf("") }
    var showAttachmentOptions by remember { mutableStateOf(false) }
    var selectedMessageId by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingMessageText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // File picker launchers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val fileName = getFileName(context, it)
                    val fileSize = getFileSize(context, it)
                    val mimeType = context.contentResolver.getType(it) ?: "image/*"

                    repository?.sendMessageWithFile(
                        phone = contact.phone,
                        content = "",
                        messageType = MessageType.IMAGE,
                        fileUri = it.toString(),
                        fileName = fileName,
                        fileSize = fileSize,
                        mimeType = mimeType
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ChatDialog", "Failed to send image: ${e.message}")
                }
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val fileName = getFileName(context, it)
                    val fileSize = getFileSize(context, it)
                    val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"

                    repository?.sendMessageWithFile(
                        phone = contact.phone,
                        content = "",
                        messageType = MessageType.DOCUMENT,
                        fileUri = it.toString(),
                        fileName = fileName,
                        fileSize = fileSize,
                        mimeType = mimeType
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ChatDialog", "Failed to send document: ${e.message}")
                }
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00BCD4).copy(alpha = 0.2f),
                                    colors.surface
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00BCD4).copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.name.take(1).uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00BCD4)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "📱 ${contact.phone}",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.textPrimary
                            )
                        }
                    }
                }

                // Messages
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    reverseLayout = true
                ) {
                    items(messages.asReversed()) { message ->
                        Box(
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        if (message.isSent) { // Only own messages
                                            selectedMessageId = message.id
                                        }
                                    }
                                )
                        ) {
                            MessageBubble(
                                message = message,
                                colors = colors,
                                isSelected = selectedMessageId == message.id
                            )
                            if (selectedMessageId == message.id && message.isSent) {
                                // Actions popup
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.18f))
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { selectedMessageId = null }
                                )
                                Row(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            // Edit
                                            selectedMessageId = message.id
                                            editingMessageText = message.content
                                            showEditDialog = true
                                        },
                                        modifier = Modifier
                                            .background(
                                                colors.surface,
                                                shape = CircleShape
                                            )
                                            .size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = colors.textPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            // Delete
                                            scope.launch {
                                                repository?.deleteMessage(contact.phone, message.id)
                                                selectedMessageId = null
                                            }
                                        },
                                        modifier = Modifier
                                            .background(
                                                Color.Red.copy(alpha = 0.07f),
                                                shape = CircleShape
                                            )
                                            .size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Red
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (messages.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "💬", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No messages yet",
                                    fontSize = 16.sp,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "Start chatting offline!",
                                    fontSize = 13.sp,
                                    color = Color(0xFF00BCD4),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Edit message dialog
                if (showEditDialog && selectedMessageId != null) {
                    androidx.compose.ui.window.Dialog(onDismissRequest = {
                        showEditDialog = false
                        selectedMessageId = null
                    }) {
                        GlassCard(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Edit Message",
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                BasicTextField(
                                    value = editingMessageText,
                                    onValueChange = { editingMessageText = it },
                                    textStyle = TextStyle(
                                        color = colors.textPrimary,
                                        fontSize = 15.sp
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF00BCD4)),
                                    modifier = Modifier
                                        .background(
                                            colors.surfaceVariant,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(12.dp)
                                        .fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            showEditDialog = false
                                            selectedMessageId = null
                                        }
                                    ) {
                                        Text("Cancel")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                repository?.editMessage(
                                                    contact.phone,
                                                    selectedMessageId!!,
                                                    editingMessageText
                                                )
                                                showEditDialog = false
                                                selectedMessageId = null
                                            }
                                        },
                                        enabled = editingMessageText.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF00BCD4)
                                        )
                                    ) {
                                        Text("Save", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Attachment options popup
                AnimatedVisibility(
                    visible = showAttachmentOptions,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceVariant.copy(alpha = 0.5f))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Image button
                        AttachmentButton(
                            icon = Icons.Default.Favorite,
                            label = "Image",
                            color = Color(0xFFE91E63),
                            onClick = {
                                imagePickerLauncher.launch("image/*")
                                showAttachmentOptions = false
                            }
                        )

                        // Document button
                        AttachmentButton(
                            icon = Icons.Default.Info,
                            label = "Document",
                            color = Color(0xFF2196F3),
                            onClick = {
                                documentPickerLauncher.launch("*/*")
                                showAttachmentOptions = false
                            }
                        )

                        // Camera button (future)
                        AttachmentButton(
                            icon = Icons.Default.Face,
                            label = "Camera",
                            color = Color(0xFF4CAF50),
                            onClick = {
                                // TODO: Implement camera
                                showAttachmentOptions = false
                            }
                        )
                    }
                }

                // Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceVariant.copy(alpha = 0.3f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment button
                    IconButton(
                        onClick = { showAttachmentOptions = !showAttachmentOptions },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach",
                            tint = Color(0xFF00BCD4),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.surface)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(Color(0xFF00BCD4)),
                            decorationBox = { innerTextField ->
                                if (messageText.isEmpty()) {
                                    Text(
                                        text = "Type a message...",
                                        fontSize = 15.sp,
                                        color = colors.textSecondary.copy(alpha = 0.5f)
                                    )
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
                                val textToSend = messageText // ← Capture text BEFORE clearing
                                messageText = "" // ← Clear UI immediately
                                scope.launch {
                                    repository?.sendMessage(
                                        contact.phone,
                                        textToSend
                                    ) // ← Send captured text
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = if (messageText.isNotBlank()) Color(0xFF00BCD4) else colors.surfaceVariant,
                        contentColor = if (messageText.isNotBlank()) Color.White else colors.textSecondary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val colors = SafeSphereThemeColors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    colors: ThemeColors,
    isSelected: Boolean = false
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isSent) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isSent) 16.dp else 4.dp,
                        bottomEnd = if (message.isSent) 4.dp else 16.dp
                    )
                )
                .background(
                    when {
                        isSelected -> Color.Yellow.copy(alpha = 0.2f)
                        message.isSent -> Color(0xFF075E54)
                        else -> Color.White
                    }
                )
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) Color(0xFFFFC107) else Color.Transparent,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isSent) 16.dp else 4.dp,
                        bottomEnd = if (message.isSent) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                // Message content based on type
                when (message.messageType) {
                    MessageType.IMAGE -> {
                        // Image message
                        message.fileUri?.let { uri ->
                            // Convert file path to File if it's an absolute path
                            val imageModel = if (uri.startsWith("content://")) {
                                Uri.parse(uri)
                            } else {
                                // It's a file path from internal storage
                                java.io.File(uri)
                            }

                            AsyncImage(
                                model = imageModel,
                                contentDescription = "Image",
                                modifier = Modifier
                                    .heightIn(max = 200.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        // Open image in full screen
                                        try {
                                            val viewUri = if (uri.startsWith("content://")) {
                                                Uri.parse(uri)
                                            } else {
                                                // Use FileProvider for files from internal storage
                                                androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    java.io.File(uri)
                                                )
                                            }
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(viewUri, "image/*")
                                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.util.Log.e(
                                                "MessageBubble",
                                                "Failed to open image: ${e.message}"
                                            )
                                        }
                                    },
                                contentScale = ContentScale.Crop
                            )
                            if (message.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.content,
                                    fontSize = 14.sp,
                                    color = if (message.isSent) Color.White else Color.Black
                                )
                            }
                        } ?: run {
                            // Fallback if image URI is null
                            Text(
                                text = "📷 Image: ${message.fileName ?: "photo.jpg"}",
                                fontSize = 14.sp,
                                color = if (message.isSent) Color.White else Color.Black
                            )
                        }
                    }

                    MessageType.DOCUMENT -> {
                        // Document message
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    message.fileUri?.let { uri ->
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(
                                                    Uri.parse(uri),
                                                    message.mimeType ?: "*/*"
                                                )
                                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            }
                                            context.startActivity(
                                                Intent.createChooser(
                                                    intent,
                                                    "Open with"
                                                )
                                            )
                                        } catch (e: Exception) {
                                            android.util.Log.e(
                                                "MessageBubble",
                                                "Failed to open document: ${e.message}"
                                            )
                                        }
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (message.isSent)
                                            Color.White.copy(alpha = 0.2f)
                                        else
                                            Color(0xFF2196F3).copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Document",
                                    tint = if (message.isSent) Color.White else Color(0xFF2196F3),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.fileName ?: "Document",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (message.isSent) Color.White else Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                message.fileSize?.let { size ->
                                    Text(
                                        text = formatFileSize(size),
                                        fontSize = 12.sp,
                                        color = if (message.isSent)
                                            Color.White.copy(alpha = 0.7f)
                                        else
                                            Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    MessageType.VOICE, MessageType.VIDEO -> {
                        // Voice/Video message placeholder
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (message.messageType == MessageType.VOICE)
                                    Icons.Default.Star
                                else
                                    Icons.Default.Star,
                                contentDescription = null,
                                tint = if (message.isSent) Color.White else Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (message.messageType == MessageType.VOICE) "Voice message" else "Video message",
                                fontSize = 14.sp,
                                color = if (message.isSent) Color.White else Color.Black
                            )
                        }
                    }

                    else -> {
                        // Text message
                        if (message.content.isNotBlank()) {
                            Text(
                                text = message.content,
                                fontSize = 15.sp,
                                color = if (message.isSent) Color.White else Color.Black,
                                lineHeight = 20.sp
                            )
                        } else {
                            // Fallback for empty messages
                            Text(
                                text = "Message",
                                fontSize = 15.sp,
                                color = if (message.isSent) Color.White.copy(alpha = 0.7f) else Color.Gray,
                                lineHeight = 20.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Show "edited" label if message was edited
                    if (message.isEdited) {
                        Text(
                            text = "edited • ",
                            fontSize = 10.sp,
                            color = if (message.isSent) Color.White.copy(alpha = 0.6f) else Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }

                    Text(
                        text = SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                        ).format(Date(message.timestamp)),
                        fontSize = 11.sp,
                        color = if (message.isSent) Color.White.copy(alpha = 0.7f) else Color.Gray
                    )

                    if (message.isSent) {
                        Spacer(modifier = Modifier.width(4.dp))
                        // WhatsApp-style tick marks
                        // ✓ = Sent (single gray tick)
                        // ✓✓ = Delivered (double white ticks)  
                        // ✓✓ = Read (double BLUE ticks)
                        Text(
                            text = when {
                                message.isRead -> "✓✓" // Read by receiver
                                message.isDelivered -> "✓✓" // Delivered to receiver's device
                                else -> "✓" // Just sent from your device
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                message.isRead -> Color(0xFF34B7F1) // Blue ticks (read)
                                message.isDelivered -> Color.White // White ticks (delivered)
                                else -> Color.Gray.copy(alpha = 0.6f) // Gray tick (sent only)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==================== DATA MODELS ====================

data class Contact(
    val name: String,
    val phone: String,
    val id: String
)

// ==================== HELPER FUNCTIONS ====================

private fun loadContactsFromPhone(context: Context): List<Contact> {
    val contacts = mutableListOf<Contact>()

    try {
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
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val name = it.getString(nameIndex) ?: "Unknown"
                val phone = it.getString(numberIndex) ?: ""

                if (phone.isNotEmpty()) {
                    contacts.add(
                        Contact(
                            id = id,
                            name = name,
                            phone = phone.replace(Regex("[^0-9+]"), "")
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return contacts.distinctBy { it.phone }
}

/**
 * Get file name from URI
 */
private fun getFileName(context: Context, uri: Uri): String {
    var fileName = "unknown"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                fileName = it.getString(nameIndex)
            }
        }
    }
    return fileName
}

/**
 * Get file size from URI
 */
private fun getFileSize(context: Context, uri: Uri): Long {
    var fileSize = 0L
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0) {
                fileSize = it.getLong(sizeIndex)
            }
        }
    }
    return fileSize
}

/**
 * Format file size to human-readable format
 */
private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}
