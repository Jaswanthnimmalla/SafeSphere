package com.runanywhere.startup_hackathon20.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.runanywhere.startup_hackathon20.data.P2PMessageRepository
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.isNotEmpty

/**
 * 💬 P2P Offline Chat Screen
 *
 * Features:
 * - Offline peer-to-peer messaging (WiFi Direct + Bluetooth)
 * - Auto-detects contacts from phone
 * - Discovers nearby SafeSphere users
 * - Search engine for contacts
 * - Real message persistence
 * - No internet required!
 */
@Composable
fun P2PChatScreen(
    viewModel: SafeSphereViewModel,
    onNavigateBack: () -> Unit
) {
    val colors = SafeSphereThemeColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Get message repository
    val messageRepository = remember { P2PMessageRepository.getInstance(context) }
    val conversations by messageRepository.conversations.collectAsState()
    val unreadCounts by messageRepository.unreadCounts.collectAsState()

    // Current user phone (in real app, get from user profile)
    val currentUserPhone = remember {
        context.getSharedPreferences("safesphere_prefs", Context.MODE_PRIVATE)
            .getString("user_phone", "+1234567890") ?: "+1234567890"
    }

    // States
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<ContactInfo>>(emptyList()) }
    var nearbyUsers by remember { mutableStateOf<List<NearbyUser>>(emptyList()) }
    var selectedContact by remember { mutableStateOf<ContactInfo?>(null) }
    var isDiscovering by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
        if (isGranted) {
            scope.launch {
                contacts = loadContacts(context)
            }
        }
    }

    // Load contacts on permission grant
    LaunchedEffect(hasContactsPermission) {
        if (hasContactsPermission) {
            contacts = loadContacts(context)
            // Simulate discovering nearby users
            isDiscovering = true
            delay(2000)
            nearbyUsers = simulateNearbyUsers()
            isDiscovering = false
        }
    }

    // Filter contacts based on search
    val filteredContacts = remember(contacts, searchQuery, nearbyUsers) {
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

            // Hero Header
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
                                text = "P2P Offline Chat",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Message nearby users without internet",
                                fontSize = 15.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00BCD4))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "WiFi Direct • Bluetooth • 100% Offline",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Stats Row
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
                        icon = "👥",
                        value = "${nearbyUsers.size}",
                        label = "Nearby",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = "💬",
                        value = "${conversations.size}",
                        label = "Chats",
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search Bar
            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search by name or number...",
                    colors = colors
                )
            }

            // Nearby Users Section
            if (nearbyUsers.isNotEmpty()) {
                item {
                    Text(
                        text = "📡 Nearby SafeSphere Users (${nearbyUsers.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                items(nearbyUsers) { user ->
                    val unreadCount = unreadCounts[user.phone] ?: 0
                    NearbyUserCard(
                        user = user,
                        unreadCount = unreadCount,
                        onClick = {
                            // Find contact or create temp
                            val contact = contacts.find { it.phone == user.phone }
                                ?: ContactInfo(
                                    id = user.id,
                                    name = user.name,
                                    phone = user.phone
                                )
                            selectedContact = contact
                            showChatDialog = true
                        },
                        colors = colors
                    )
                }
            }

            // Contacts Section
            if (hasContactsPermission) {
                item {
                    Text(
                        text = "📇 Your Contacts (${filteredContacts.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                items(filteredContacts) { contact ->
                    val unreadCount = unreadCounts[contact.phone] ?: 0
                    ContactCard(
                        contact = contact,
                        isOnline = nearbyUsers.any { it.phone == contact.phone },
                        unreadCount = unreadCount,
                        onClick = {
                            selectedContact = contact
                            showChatDialog = true
                        },
                        colors = colors
                    )
                }
            } else {
                // Permission request
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
                                text = "We need permission to find SafeSphere users in your contacts",
                                fontSize = 14.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
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
            isOnline = nearbyUsers.any { it.phone == selectedContact!!.phone },
            currentUserPhone = currentUserPhone,
            messageRepository = messageRepository,
            onDismiss = {
                showChatDialog = false
            },
            colors = colors
        )
    }
}

/**
 * Search Bar Component
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
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
                                text = placeholder,
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

/**
 * Stat Card Component
 */
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

/**
 * Nearby User Card
 */
@Composable
fun NearbyUserCard(
    user: NearbyUser,
    unreadCount: Int,
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
                                Color(0xFF4CAF50).copy(alpha = 0.3f),
                                Color(0xFF4CAF50).copy(alpha = 0.1f)
                            )
                        )
                    )
                    .border(
                        2.dp,
                        Color(0xFF4CAF50).copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(1).uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = user.phone,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${user.distance}m away • ${user.connectionType}",
                        fontSize = 11.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Chat",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unreadCount.toString(),
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Contact Card
 */
@Composable
fun ContactCard(
    contact: ContactInfo,
    isOnline: Boolean,
    unreadCount: Int,
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
                    )
                    .border(
                        2.dp,
                        if (isOnline) Color(0xFF4CAF50) else Color(0xFF2196F3).copy(alpha = 0.5f),
                        CircleShape
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

            if (isOnline) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🟢 Online",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unreadCount.toString(),
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Chat Dialog
 */
@Composable
fun ChatDialog(
    contact: ContactInfo,
    isOnline: Boolean,
    currentUserPhone: String,
    messageRepository: P2PMessageRepository,
    onDismiss: () -> Unit,
    colors: ThemeColors
) {
    val scope = rememberCoroutineScope()
    
    // Get messages from repository
    val allConversations by messageRepository.conversations.collectAsState()
    val messages = remember(allConversations) {
        allConversations[contact.phone] ?: emptyList()
    }
    
    var message by remember { mutableStateOf("") }

    // Mark as read when opening
    LaunchedEffect(contact.phone) {
        messageRepository.markAsRead(contact.phone)
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
                                text = if (isOnline) "🟢 Online - P2P Active" else "⚪ Offline - Mesh Delivery",
                                fontSize = 12.sp,
                                color = if (isOnline) Color(0xFF4CAF50) else Color(0xFFFF9800)
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
                    items(messages.asReversed()) { msg ->
                        MessageBubble(msg, colors, msg.fromPhone == currentUserPhone)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Empty state
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
                                    text = "Start chatting offline via mesh network!",
                                    fontSize = 13.sp,
                                    color = Color(0xFF00BCD4),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.surface)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = message,
                            onValueChange = { message = it },
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(Color(0xFF00BCD4)),
                            decorationBox = { innerTextField ->
                                if (message.isEmpty()) {
                                    Text(
                                        text = "Type a message (offline delivery)...",
                                        fontSize = 15.sp,
                                        color = colors.textSecondary.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = true // Always enabled for offline messaging
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FloatingActionButton(
                        onClick = {
                            if (message.isNotBlank()) {
                                // Send message using repository
                                messageRepository.sendMessage(contact.phone, message)
                                message = ""
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = if (message.isNotBlank()) Color(0xFF00BCD4) else colors.surfaceVariant,
                        contentColor = if (message.isNotBlank()) Color.White else colors.textSecondary
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

/**
 * Message Bubble
 */
@Composable
fun MessageBubble(
    message: com.runanywhere.startup_hackathon20.data.P2PMessage,
    colors: ThemeColors,
    isSent: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isSent) 16.dp else 4.dp,
                        bottomEnd = if (isSent) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isSent)
                        Color(0xFF00BCD4)
                    else
                        colors.surfaceVariant
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    color = if (isSent) Color.White else colors.textPrimary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat(
                        "HH:mm",
                        Locale.getDefault()
                    ).format(Date(message.timestamp)),
                    fontSize = 10.sp,
                    color = if (isSent) Color.White.copy(alpha = 0.7f) else colors.textSecondary
                )
            }
        }
    }
}

/**
 * Load contacts from device
 */
private fun loadContacts(context: Context): List<ContactInfo> {
    val contacts = mutableListOf<ContactInfo>()

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
                        ContactInfo(
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
 * Simulate nearby SafeSphere users (in production, use WiFi Direct/Bluetooth discovery)
 */
private fun simulateNearbyUsers(): List<NearbyUser> {
    return listOf(
        NearbyUser(
            id = "1",
            name = "Sarah Johnson",
            phone = "+1234567890",
            distance = 45,
            connectionType = "WiFi Direct",
            isOnline = true
        ),
        NearbyUser(
            id = "2",
            name = "Mike Chen",
            phone = "+9876543210",
            distance = 120,
            connectionType = "Bluetooth",
            isOnline = true
        ),
        NearbyUser(
            id = "3",
            name = "Emma Davis",
            phone = "+1122334455",
            distance = 8,
            connectionType = "WiFi Direct",
            isOnline = true
        )
    )
}

/**
 * Data Classes
 */
data class ContactInfo(
    val id: String,
    val name: String,
    val phone: String
)

data class NearbyUser(
    val id: String,
    val name: String,
    val phone: String,
    val distance: Int,
    val connectionType: String,
    val isOnline: Boolean
)

data class ChatMessage(
    val content: String,
    val sender: String,
    val timestamp: Long
)
