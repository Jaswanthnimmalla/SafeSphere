package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.data.User
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import kotlinx.coroutines.launch

/**
 * SafeSphere Navigation Drawer
 *
 * Beautiful side navigation with:
 * - User profile section
 * - Welcome message
 * - Navigation menu items
 * - Logout with confirmation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeSphereDrawerContent(
    currentUser: User?,
    currentScreen: SafeSphereScreen,
    onNavigate: (SafeSphereScreen) -> Unit,
    onLogout: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = {
                showLogoutDialog = false
            }
        )
    }

    ModalDrawerSheet(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(),
        drawerContainerColor = SafeSphereColors.Surface
    ) {
        // Drawer content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SafeSphereColors.Surface,
                            SafeSphereColors.Background
                        )
                    )
                )
        ) {
            // Profile Section - Centered
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SafeSphereColors.Primary.copy(alpha = 0.15f),
                                SafeSphereColors.Surface
                            )
                        )
                    )
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar/Logo (Centered)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    SafeSphereColors.Primary,
                                    SafeSphereColors.Secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🛡️",
                        fontSize = 40.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User Name
                Text(
                    text = currentUser?.name ?: "Guest User",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // User Email
                Text(
                    text = currentUser?.email ?: "guest@safesphere.com",
                    fontSize = 13.sp,
                    color = SafeSphereColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Items
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // Home
                NavigationDrawerItem(
                    icon = Icons.Filled.Home,
                    label = "Home",
                    selected = currentScreen == SafeSphereScreen.DASHBOARD,
                    onClick = {
                        onNavigate(SafeSphereScreen.DASHBOARD)
                    }
                )

                // About Us
                NavigationDrawerItem(
                    iconEmoji = "ℹ️",
                    label = "About Us",
                    selected = currentScreen == SafeSphereScreen.ABOUT_US,
                    onClick = {
                        onNavigate(SafeSphereScreen.ABOUT_US)
                    }
                )

                // Blogs
                NavigationDrawerItem(
                    iconEmoji = "📝",
                    label = "Blogs",
                    selected = currentScreen == SafeSphereScreen.BLOGS,
                    onClick = {
                        onNavigate(SafeSphereScreen.BLOGS)
                    }
                )

                // Contact Us
                NavigationDrawerItem(
                    iconEmoji = "📧",
                    label = "Contact Us",
                    selected = currentScreen == SafeSphereScreen.CONTACT_US,
                    onClick = {
                        onNavigate(SafeSphereScreen.CONTACT_US)
                    }
                )

                // Settings
                NavigationDrawerItem(
                    icon = Icons.Filled.Settings,
                    label = "Settings",
                    selected = currentScreen == SafeSphereScreen.SETTINGS,
                    onClick = {
                        onNavigate(SafeSphereScreen.SETTINGS)
                    }
                )

                // Theme Toggle
                ThemeToggleItem(
                    isDark = isDarkTheme,
                    onToggle = onToggleTheme
                )
            }

            // Logout Button at Bottom
            Divider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = SafeSphereColors.TextSecondary.copy(alpha = 0.2f)
            )

            NavigationDrawerItem(
                iconEmoji = "🚪",
                label = "Logout",
                selected = false,
                onClick = {
                    showLogoutDialog = true
                },
                isLogout = true
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Navigation Drawer Item
 */
@Composable
fun NavigationDrawerItem(
    icon: ImageVector? = null,
    iconEmoji: String? = null,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isLogout: Boolean = false
) {
    val backgroundColor = when {
        isLogout -> Color.Transparent
        selected -> SafeSphereColors.Primary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val contentColor = when {
        isLogout -> SafeSphereColors.Error
        selected -> SafeSphereColors.Primary
        else -> SafeSphereColors.TextSecondary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            } else if (iconEmoji != null) {
                Text(
                    text = iconEmoji,
                    fontSize = 24.sp,
                    color = contentColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

/**
 * Theme Toggle Item
 */
@Composable
fun ThemeToggleItem(
    isDark: Boolean,
    onToggle: () -> Unit
) {
    val iconEmoji = if (isDark) "☀️" else "🌙"
    val label = if (isDark) "Light Mode" else "Dark Mode"

    NavigationDrawerItem(
        iconEmoji = iconEmoji,
        label = label,
        selected = false,
        onClick = onToggle,
        isLogout = false
    )
}

/**
 * Logout Confirmation Dialog
 */
@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SafeSphereColors.Surface,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SafeSphereColors.Error.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🚪",
                    fontSize = 32.sp
                )
            }
        },
        title = {
            Text(
                text = "Logout?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Are you sure you want to logout from SafeSphere?",
                    fontSize = 16.sp,
                    color = SafeSphereColors.TextPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Your data will remain encrypted and secure.",
                    fontSize = 13.sp,
                    color = SafeSphereColors.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafeSphereColors.Error
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.48f)
            ) {
                Text(
                    text = "Yes, Logout",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SafeSphereColors.Primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth(0.48f)
            ) {
                Text(
                    text = "Cancel",
                    color = SafeSphereColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

/**
 * Menu Icon Button for opening drawer
 */
@Composable
fun MenuIconButton(
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(SafeSphereColors.Primary.copy(alpha = 0.1f))
    ) {
        Icon(
            imageVector = Icons.Filled.Menu,
            contentDescription = "Menu",
            tint = SafeSphereColors.Primary
        )
    }
}

/**
 * Notifications Screen - Enhanced with filters, search, and real-time updates
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: SafeSphereViewModel) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    
    var selectedFilter by remember { mutableStateOf(com.runanywhere.startup_hackathon20.viewmodels.NotificationFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }

    // Filter and search notifications
    val filteredNotifications = remember(notifications, selectedFilter, searchQuery) {
        val filtered = when (selectedFilter) {
            com.runanywhere.startup_hackathon20.viewmodels.NotificationFilter.ALL -> notifications
            com.runanywhere.startup_hackathon20.viewmodels.NotificationFilter.UNREAD -> 
                notifications.filter { !it.isRead }
            com.runanywhere.startup_hackathon20.viewmodels.NotificationFilter.SECURITY -> 
                notifications.filter { it.category == com.runanywhere.startup_hackathon20.viewmodels.NotificationCategory.SECURITY }
            com.runanywhere.startup_hackathon20.viewmodels.NotificationFilter.ACTIVITY -> 
                notifications.filter { 
                    it.category == com.runanywhere.startup_hackathon20.viewmodels.NotificationCategory.ACTIVITY ||
                    it.category == com.runanywhere.startup_hackathon20.viewmodels.NotificationCategory.VAULT
                }
            com.runanywhere.startup_hackathon20.viewmodels.NotificationFilter.THREATS -> 
                notifications.filter { it.category == com.runanywhere.startup_hackathon20.viewmodels.NotificationCategory.THREAT }
        }
        
        // Apply search filter
        if (searchQuery.isNotBlank()) {
            filtered.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.message.contains(searchQuery, ignoreCase = true)
            }
        } else {
            filtered
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SafeSphereColors.Background,
                        SafeSphereColors.BackgroundDark
                    )
                )
            )
    ) {
        // Header with Stats and Actions
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title and Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Notifications",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.TextPrimary
                        )
                        Text(
                            text = if (unreadCount > 0) "$unreadCount unread" else "All caught up!",
                            fontSize = 13.sp,
                            color = if (unreadCount > 0) SafeSphereColors.Primary else SafeSphereColors.TextSecondary
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Search button
                        IconButton(
                            onClick = { showSearchBar = !showSearchBar },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (showSearchBar) SafeSphereColors.Primary.copy(alpha = 0.2f)
                                    else SafeSphereColors.Surface
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = if (showSearchBar) SafeSphereColors.Primary else SafeSphereColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        // Mark all as read button
                        if (unreadCount > 0) {
                            IconButton(
                                onClick = { viewModel.markAllNotificationsAsRead() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SafeSphereColors.Success.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = "Mark all as read",
                                    tint = SafeSphereColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        // Clear all button
                        if (notifications.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearAllNotifications() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SafeSphereColors.Error.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Clear all",
                                    tint = SafeSphereColors.Error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                
                // Search Bar
                AnimatedVisibility(
                    visible = showSearchBar,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = "Search notifications...",
                                    color = SafeSphereColors.TextSecondary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = SafeSphereColors.TextSecondary
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear search",
                                            tint = SafeSphereColors.TextSecondary
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SafeSphereColors.Primary,
                                unfocusedBorderColor = SafeSphereColors.TextSecondary.copy(alpha = 0.3f),
                                focusedTextColor = SafeSphereColors.TextPrimary,
                                unfocusedTextColor = SafeSphereColors.TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EnhancedNotificationStat(
                        icon = "🔔",
                        count = unreadCount,
                        label = "Unread",
                        color = SafeSphereColors.Primary,
                        isHighlighted = unreadCount > 0
                    )
                    EnhancedNotificationStat(
                        icon = "📝",
                        count = notifications.size,
                        label = "Total",
                        color = SafeSphereColors.Info,
                        isHighlighted = false
                    )
                    EnhancedNotificationStat(
                        icon = "🛡️",
                        count = notifications.count { 
                            it.category == com.runanywhere.startup_hackathon20.viewmodels.NotificationCategory.SECURITY 
                        },
                        label = "Security",
                        color = SafeSphereColors.Warning,
                        isHighlighted = false
                    )
                    EnhancedNotificationStat(
                        icon = "🚨",
                        count = notifications.count { 
                            it.category == com.runanywhere.startup_hackathon20.viewmodels.NotificationCategory.THREAT 
                        },
                        label = "Threats",
                        color = SafeSphereColors.Error,
                        isHighlighted = false
                    )
                }
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.runanywhere.startup_hackathon20.viewmodels.NotificationFilter.values().forEach { filter ->
                val filterCount = when (filter) {
                    com.runanywhere.startup_hackathon20.viewmodels.NotificationFilter.ALL -> notifications.size
                    com.runanywhere.startup_hackathon20.viewmodels.NotificationFilter.UNREAD -> unreadCount
                    com.runanywhere.startup_hackathon20.viewmodels.NotificationFilter.SECURITY -> 
                        notifications.count { it.category == com.runanywhere.startup_hackathon20.viewmodels.NotificationCategory.SECURITY }
                    com.runanywhere.startup_hackathon20.viewmodels.NotificationFilter.ACTIVITY -> 
                        notifications.count { 
                            it.category == com.runanywhere.startup_hackathon20.viewmodels.NotificationCategory.ACTIVITY ||
                            it.category == com.runanywhere.startup_hackathon20.viewmodels.NotificationCategory.VAULT
                        }
                    com.runanywhere.startup_hackathon20.viewmodels.NotificationFilter.THREATS -> 
                        notifications.count { it.category == com.runanywhere.startup_hackathon20.viewmodels.NotificationCategory.THREAT }
                }
                
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = filter.label,
                                fontSize = 13.sp,
                                fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                            )
                            if (filterCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedFilter == filter) 
                                        Color.White.copy(alpha = 0.3f) 
                                    else SafeSphereColors.Primary.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    Text(
                                        text = filterCount.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedFilter == filter) Color.White else SafeSphereColors.Primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    leadingIcon = {
                        Text(text = filter.icon, fontSize = 14.sp)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SafeSphereColors.Primary,
                        selectedLabelColor = Color.White,
                        containerColor = SafeSphereColors.Surface,
                        labelColor = SafeSphereColors.TextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedFilter == filter,
                        borderColor = if (selectedFilter == filter) 
                            SafeSphereColors.Primary 
                        else SafeSphereColors.TextSecondary.copy(alpha = 0.2f),
                        selectedBorderColor = SafeSphereColors.Primary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 2.dp
                    )
                )
            }
        }

        // Results info
        if (searchQuery.isNotEmpty() || selectedFilter != com.runanywhere.startup_hackathon20.viewmodels.NotificationFilter.ALL) {
            Text(
                text = "${filteredNotifications.size} ${if (filteredNotifications.size == 1) "result" else "results"}",
                fontSize = 12.sp,
                color = SafeSphereColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        // Notifications List
        if (filteredNotifications.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "🔍" else "📭",
                    fontSize = 64.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (searchQuery.isNotEmpty()) "No results found" else "No notifications",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (searchQuery.isNotEmpty()) 
                        "Try different search terms"
                    else "You're all caught up!",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                
                if (searchQuery.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { searchQuery = "" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafeSphereColors.Primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Search")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredNotifications.forEach { notification ->
                    item {
                        EnhancedNotificationCard(
                            notification = notification,
                            onMarkAsRead = { viewModel.markNotificationAsRead(notification.id) },
                            onDelete = { viewModel.deleteNotification(notification.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enhanced Notification Card with borders, colors, and actions
 */
@Composable
fun EnhancedNotificationCard(
    notification: com.runanywhere.startup_hackathon20.viewmodels.AppNotification,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    
    val borderColor = Color(notification.type.color).copy(alpha = 0.5f)
    val backgroundColor = Color(notification.type.color).copy(alpha = 0.05f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!notification.isRead) onMarkAsRead()
                showActions = !showActions
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) 
                SafeSphereColors.Surface 
            else backgroundColor
        ),
        border = BorderStroke(
            width = if (notification.isRead) 1.dp else 2.dp,
            color = if (notification.isRead) 
                SafeSphereColors.TextSecondary.copy(alpha = 0.2f)
            else borderColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 2.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Icon with category color
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(notification.type.color).copy(alpha = 0.15f))
                        .border(
                            width = 2.dp,
                            color = Color(notification.type.color).copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notification.category.icon,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = notification.title,
                                    fontSize = 15.sp,
                                    fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                                    color = SafeSphereColors.TextPrimary
                                )
                                
                                // Category badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(notification.type.color).copy(alpha = 0.15f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = Color(notification.type.color).copy(alpha = 0.3f)
                                    )
                                ) {
                                    Text(
                                        text = notification.category.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(notification.type.color),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Unread indicator
                        if (!notification.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(SafeSphereColors.Primary)
                                    .border(
                                        width = 2.dp,
                                        color = SafeSphereColors.Primary.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = notification.message,
                        fontSize = 13.sp,
                        color = SafeSphereColors.TextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Timestamp
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = null,
                                tint = SafeSphereColors.TextSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = formatTimestamp(notification.timestamp),
                                fontSize = 11.sp,
                                color = SafeSphereColors.TextSecondary.copy(alpha = 0.7f)
                            )
                        }
                        
                        // Priority indicator
                        if (notification.priority == com.runanywhere.startup_hackathon20.viewmodels.NotificationPriority.HIGH ||
                            notification.priority == com.runanywhere.startup_hackathon20.viewmodels.NotificationPriority.CRITICAL) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SafeSphereColors.Error.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, SafeSphereColors.Error.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = if (notification.priority == com.runanywhere.startup_hackathon20.viewmodels.NotificationPriority.CRITICAL) 
                                        "🚨 CRITICAL" else "⚡ HIGH",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SafeSphereColors.Error,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Action buttons
            AnimatedVisibility(
                visible = showActions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        color = SafeSphereColors.TextSecondary.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!notification.isRead) {
                            OutlinedButton(
                                onClick = {
                                    onMarkAsRead()
                                    showActions = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SafeSphereColors.Success
                                ),
                                border = BorderStroke(1.dp, SafeSphereColors.Success.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mark Read", fontSize = 12.sp)
                            }
                        }
                        
                        OutlinedButton(
                            onClick = {
                                onDelete()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SafeSphereColors.Error
                            ),
                            border = BorderStroke(1.dp, SafeSphereColors.Error.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Enhanced Notification Stat with highlight
 */
@Composable
fun EnhancedNotificationStat(
    icon: String,
    count: Int,
    label: String,
    color: Color,
    isHighlighted: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isHighlighted) color.copy(alpha = 0.1f) else Color.Transparent
            )
            .border(
                width = if (isHighlighted) 2.dp else 0.dp,
                color = if (isHighlighted) color.copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Text(
            text = icon,
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) color else SafeSphereColors.TextPrimary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = SafeSphereColors.TextSecondary,
            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Notification Stat
 */
@Composable
fun NotificationStat(
    icon: String,
    count: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = SafeSphereColors.TextSecondary
        )
    }
}

/**
 * Format timestamp to relative time
 */
fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 1 -> "$days days ago"
        days == 1L -> "Yesterday"
        hours > 1 -> "$hours hours ago"
        hours == 1L -> "1 hour ago"
        minutes > 1 -> "$minutes minutes ago"
        minutes == 1L -> "1 minute ago"
        else -> "Just now"
    }
}

/**
 * Notification Data Models
 */
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: NotificationType,
    val isRead: Boolean
)

enum class NotificationType(val icon: String, val color: Color) {
    SUCCESS("✅", SafeSphereColors.Success),
    INFO("ℹ️", SafeSphereColors.Info),
    WARNING("⚠️", SafeSphereColors.Warning),
    ERROR("❌", SafeSphereColors.Error)
}

enum class NotificationFilter(val label: String, val icon: String) {
    ALL("All", "📋"),
    UNREAD("Unread", "🔔"),
    SECURITY("Security", "🛡️"),
    ACTIVITY("Activity", "📊"),
    THREATS("Threats", "🚨")
}
