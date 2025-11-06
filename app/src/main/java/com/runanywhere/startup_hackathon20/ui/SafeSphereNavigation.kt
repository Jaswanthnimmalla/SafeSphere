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
    onLogout: () -> Unit
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
            // Profile Section
            NavigationDrawerHeader(currentUser)

            // Navigation Items
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                NavigationDrawerItem(
                    icon = Icons.Filled.Home,
                    label = "Dashboard",
                    selected = currentScreen == SafeSphereScreen.DASHBOARD,
                    onClick = {
                        onNavigate(SafeSphereScreen.DASHBOARD)
                    }
                )

                NavigationDrawerItem(
                    icon = Icons.Filled.Lock,
                    label = "Privacy Vault",
                    selected = currentScreen == SafeSphereScreen.PRIVACY_VAULT,
                    onClick = {
                        onNavigate(SafeSphereScreen.PRIVACY_VAULT)
                    }
                )

                NavigationDrawerItem(
                    iconEmoji = "💬",
                    label = "AI Chat",
                    selected = currentScreen == SafeSphereScreen.AI_CHAT,
                    onClick = {
                        onNavigate(SafeSphereScreen.AI_CHAT)
                    }
                )

                NavigationDrawerItem(
                    iconEmoji = "📊",
                    label = "Data Map",
                    selected = currentScreen == SafeSphereScreen.DATA_MAP,
                    onClick = {
                        onNavigate(SafeSphereScreen.DATA_MAP)
                    }
                )

                NavigationDrawerItem(
                    iconEmoji = "🛡️",
                    label = "Threat Simulation",
                    selected = currentScreen == SafeSphereScreen.THREAT_SIMULATION,
                    onClick = {
                        onNavigate(SafeSphereScreen.THREAT_SIMULATION)
                    }
                )

                NavigationDrawerItem(
                    icon = Icons.Filled.Notifications,
                    label = "Notifications",
                    selected = currentScreen == SafeSphereScreen.NOTIFICATIONS,
                    onClick = {
                        onNavigate(SafeSphereScreen.NOTIFICATIONS)
                    }
                )

                NavigationDrawerItem(
                    icon = Icons.Filled.Settings,
                    label = "Settings",
                    selected = currentScreen == SafeSphereScreen.SETTINGS,
                    onClick = {
                        onNavigate(SafeSphereScreen.SETTINGS)
                    }
                )

                NavigationDrawerItem(
                    iconEmoji = "🤖",
                    label = "AI Models",
                    selected = currentScreen == SafeSphereScreen.MODELS,
                    onClick = {
                        onNavigate(SafeSphereScreen.MODELS)
                    }
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
 * Navigation Drawer Header with User Profile
 */
@Composable
fun NavigationDrawerHeader(currentUser: User?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SafeSphereColors.Primary.copy(alpha = 0.2f),
                        SafeSphereColors.Surface
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            // App Logo/Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
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
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // App Name
            Text(
                text = "SafeSphere",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // User Info
            if (currentUser != null) {
                Text(
                    text = "Welcome, ${currentUser.name}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = SafeSphereColors.Primary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = currentUser.email,
                    fontSize = 12.sp,
                    color = SafeSphereColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Your Privacy Guardian",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Security Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SafeSphereColors.Success.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔐",
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Secured",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeSphereColors.Success
                )
            }
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

            if (selected && !isLogout) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SafeSphereColors.Primary)
                )
            }
        }
    }
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
 * Notifications Screen - Security alerts and activity log
 */
@Composable
fun NotificationsScreen(viewModel: SafeSphereViewModel) {
    val notificationItems = remember {
        listOf(
            NotificationItem(
                id = "1",
                title = "🔐 Vault Item Added",
                message = "New encrypted item saved to Privacy Vault",
                timestamp = System.currentTimeMillis() - 3600000,
                type = NotificationType.SUCCESS,
                isRead = false
            ),
            NotificationItem(
                id = "2",
                title = "🛡️ Security Scan Complete",
                message = "All ${10} items encrypted with AES-256",
                timestamp = System.currentTimeMillis() - 7200000,
                type = NotificationType.INFO,
                isRead = true
            ),
            NotificationItem(
                id = "3",
                title = "⚠️ Threat Mitigated",
                message = "Cloud exposure attempt blocked by offline mode",
                timestamp = System.currentTimeMillis() - 14400000,
                type = NotificationType.WARNING,
                isRead = true
            ),
            NotificationItem(
                id = "4",
                title = "✅ Backup Successful",
                message = "Local encrypted backup created",
                timestamp = System.currentTimeMillis() - 86400000,
                type = NotificationType.SUCCESS,
                isRead = true
            ),
            NotificationItem(
                id = "5",
                title = "🔒 Login Detected",
                message = "New login from this device",
                timestamp = System.currentTimeMillis() - 172800000,
                type = NotificationType.INFO,
                isRead = true
            )
        )
    }

    var selectedFilter by remember { mutableStateOf(NotificationFilter.ALL) }

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
        // Stats Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NotificationStat(
                    icon = "🔔",
                    count = notificationItems.count { !it.isRead },
                    label = "New",
                    color = SafeSphereColors.Primary
                )
                NotificationStat(
                    icon = "📝",
                    count = notificationItems.size,
                    label = "Total",
                    color = SafeSphereColors.Info
                )
                NotificationStat(
                    icon = "🛡️",
                    count = notificationItems.count { it.type == NotificationType.WARNING },
                    label = "Alerts",
                    color = SafeSphereColors.Warning
                )
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NotificationFilter.values().forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = filter.label,
                            fontSize = 13.sp,
                            fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Text(text = filter.icon, fontSize = 14.sp)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SafeSphereColors.Primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Notifications List
        val filteredNotifications = when (selectedFilter) {
            NotificationFilter.ALL -> notificationItems
            NotificationFilter.UNREAD -> notificationItems.filter { !it.isRead }
            NotificationFilter.SECURITY -> notificationItems.filter {
                it.type == NotificationType.WARNING || it.type == NotificationType.ERROR
            }

            NotificationFilter.ACTIVITY -> notificationItems.filter {
                it.type == NotificationType.INFO || it.type == NotificationType.SUCCESS
            }
        }

        if (filteredNotifications.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "📭", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No notifications",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You're all caught up!",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredNotifications.forEach { notification ->
                    item {
                        NotificationCard(notification)
                    }
                }
            }
        }
    }
}

/**
 * Notification Card
 */
@Composable
fun NotificationCard(notification: NotificationItem) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Mark as read */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(notification.type.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = notification.type.icon,
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
                    Text(
                        text = notification.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SafeSphereColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SafeSphereColors.Primary)
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

                Text(
                    text = formatTimestamp(notification.timestamp),
                    fontSize = 11.sp,
                    color = SafeSphereColors.TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
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
    ACTIVITY("Activity", "📊")
}
