package com.runanywhere.startup_hackathon20.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.listAvailableModels
import com.runanywhere.sdk.models.ModelInfo
import com.runanywhere.startup_hackathon20.data.*
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereChatMessage
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * AI Chat Screen - Offline privacy advisor
 */
@Composable
fun AIChatScreen(viewModel: SafeSphereViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SafeSphereHeader(
            title = "Privacy Advisor",
            subtitle = "Offline AI • 100% Private",
            onBackClick = { viewModel.navigateToScreen(SafeSphereScreen.DASHBOARD) }
        )

        // Chat messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { message ->
                ChatMessageBubble(message)
            }

            if (isGenerating) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp)
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = SafeSphereColors.Primary
                                )
                                Text(
                                    text = "Thinking...",
                                    fontSize = 14.sp,
                                    color = SafeSphereColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Input area
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    textStyle = TextStyle(
                        color = SafeSphereColors.TextPrimary,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(SafeSphereColors.Primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "Ask about privacy and security...",
                                    fontSize = 16.sp,
                                    color = SafeSphereColors.TextSecondary.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank())
                                SafeSphereColors.Primary
                            else
                                SafeSphereColors.SurfaceVariant
                        )
                        .clickable(enabled = inputText.isNotBlank() && !isGenerating) {
                            viewModel.sendChatMessage(inputText)
                            inputText = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↑",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (inputText.isNotBlank())
                            Color.White
                        else
                            SafeSphereColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: SafeSphereChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .padding(
                    start = if (message.isUser) 48.dp else 0.dp,
                    end = if (message.isUser) 0.dp else 48.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!message.isUser) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SafeSphereColors.Primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🤖",
                        fontSize = 18.sp
                    )
                }
            }

            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = message.content,
                        fontSize = 15.sp,
                        color = SafeSphereColors.TextPrimary,
                        lineHeight = 22.sp
                    )
                }
            }

            if (message.isUser) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SafeSphereColors.Accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👤",
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Data Map Screen - Storage visualization
 */
@Composable
fun DataMapScreen(viewModel: SafeSphereViewModel) {
    val stats by viewModel.storageStats.collectAsState()
    val vaultItems by viewModel.vaultItems.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SafeSphereHeader(
            title = "Data Map",
            subtitle = "Storage & Security Visualization",
            onBackClick = { viewModel.navigateToScreen(SafeSphereScreen.DASHBOARD) }
        )

        Column(modifier = Modifier.padding(16.dp)) {
            // Security Overview
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Security Overview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SecurityMetric(
                        label = "Total Items",
                        value = "${stats.totalItems}",
                        icon = "📦"
                    )

                    SecurityMetric(
                        label = "Encrypted Items",
                        value = "${stats.encryptedItems}",
                        icon = "🔒"
                    )

                    SecurityMetric(
                        label = "Storage Used",
                        value = formatBytes(stats.totalSize),
                        icon = "💾"
                    )

                    SecurityMetric(
                        label = "Encryption",
                        value = "AES-256-GCM",
                        icon = "🛡️"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Breakdown
            Text(
                text = "Category Breakdown",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            stats.categoryBreakdown.forEach { (category, count) ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category.icon,
                                fontSize = 24.sp
                            )
                            Text(
                                text = category.displayName,
                                fontSize = 16.sp,
                                color = SafeSphereColors.TextPrimary
                            )
                        }

                        Text(
                            text = "$count items",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SafeSphereColors.Primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy Score
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Privacy Protection",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PrivacyFeature("🔐", "Encrypted")
                        PrivacyFeature("📴", "Offline")
                        PrivacyFeature("🛡️", "Signed")
                        PrivacyFeature("🔒", "Locked")
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityMetric(label: String, value: String, icon: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Text(
                text = label,
                fontSize = 15.sp,
                color = SafeSphereColors.TextSecondary
            )
        }

        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = SafeSphereColors.TextPrimary
        )
    }
}

@Composable
fun PrivacyFeature(icon: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 32.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = SafeSphereColors.TextSecondary
        )
    }
}

/**
 * Threat Simulation Screen - Real-time Security Monitoring
 */
@Composable
fun ThreatSimulationScreen(viewModel: SafeSphereViewModel) {
    val threats by viewModel.threatEvents.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()
    val threatsBlocked by viewModel.threatsBlocked.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        SafeSphereHeader(
            title = "Security Monitor",
            subtitle = "Real-Time Threat Protection",
            onBackClick = { viewModel.navigateToScreen(SafeSphereScreen.DASHBOARD) },
            onActionClick = { viewModel.simulateThreat() },
            actionIcon = "⚡"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Real-time monitoring dashboard
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Real-Time Monitoring",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeSphereColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isMonitoring) SafeSphereColors.Success
                                            else SafeSphereColors.TextSecondary
                                        )
                                )
                                Text(
                                    text = if (isMonitoring) "Active" else "Paused",
                                    fontSize = 13.sp,
                                    color = SafeSphereColors.TextSecondary
                                )
                            }
                        }

                        // Toggle button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isMonitoring) SafeSphereColors.Success.copy(alpha = 0.2f)
                                    else SafeSphereColors.TextSecondary.copy(alpha = 0.2f)
                                )
                                .clickable { viewModel.toggleMonitoring() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isMonitoring) "⏸" else "▶",
                                fontSize = 20.sp,
                                color = if (isMonitoring) SafeSphereColors.Success
                                else SafeSphereColors.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Threats Blocked
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SafeSphereColors.Success.copy(alpha = 0.15f))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🛡️",
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$threatsBlocked",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeSphereColors.Success
                            )
                            Text(
                                text = "Blocked",
                                fontSize = 11.sp,
                                color = SafeSphereColors.TextSecondary
                            )
                        }

                        // Network Status
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SafeSphereColors.Primary.copy(alpha = 0.15f))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📡",
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = networkStatus.split(" ").take(2).joinToString(" "),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SafeSphereColors.TextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ℹ️",
                        fontSize = 24.sp
                    )
                    Column {
                        Text(
                            text = "How SafeSphere Protects You",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SafeSphereColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Real-time monitoring detects network connections, system threats, and security risks. All threats are automatically mitigated through encryption and offline storage.",
                            fontSize = 14.sp,
                            color = SafeSphereColors.TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Threats",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                if (threats.isNotEmpty()) {
                    Text(
                        text = "Last ${threats.size}",
                        fontSize = 13.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (threats.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🛡️",
                        fontSize = 64.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No threats detected",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isMonitoring)
                            "Monitoring active • Your data is protected"
                        else
                            "Tap ▶ to start monitoring",
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Threat list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(threats) { threat ->
                        EnhancedThreatCard(threat)
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedThreatCard(threat: ThreatEvent) {
    val severityColor = when (threat.severity) {
        ThreatSeverity.LOW -> SafeSphereColors.Success
        ThreatSeverity.MEDIUM -> SafeSphereColors.Warning
        ThreatSeverity.HIGH -> Color(0xFFFF6B6B)
        ThreatSeverity.CRITICAL -> SafeSphereColors.Accent
    }

    // Format relative time
    val timeAgo = remember(threat.timestamp) {
        val diff = System.currentTimeMillis() - threat.timestamp
        when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> "${diff / 86400_000}d ago"
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(severityColor)
                        )

                        Text(
                            text = threat.type.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SafeSphereColors.TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = threat.description,
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Severity badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(severityColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = threat.severity.displayName.uppercase(),
                                fontSize = 11.sp,
                                color = severityColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (threat.mitigated) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✓",
                                    fontSize = 16.sp,
                                    color = SafeSphereColors.Success
                                )
                                Text(
                                    text = "Blocked",
                                    fontSize = 12.sp,
                                    color = SafeSphereColors.Success,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Time
                        Text(
                            text = timeAgo,
                            fontSize = 11.sp,
                            color = SafeSphereColors.TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Settings Screen
 */
@Composable
fun SettingsScreen(viewModel: SafeSphereViewModel) {
    val securityStatus =
        remember { com.runanywhere.startup_hackathon20.security.SecurityManager.getSecurityStatus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SafeSphereHeader(
            title = "Settings",
            subtitle = "Privacy & Security",
            onBackClick = { viewModel.navigateToScreen(SafeSphereScreen.DASHBOARD) }
        )

        Column(modifier = Modifier.padding(16.dp)) {
            // Security Status
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Security Status",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingItem(
                        label = "Encryption",
                        value = if (securityStatus.aesKeyPresent) "AES-256-GCM ✓" else "Not configured",
                        icon = "🔐"
                    )

                    SettingItem(
                        label = "Digital Signatures",
                        value = if (securityStatus.rsaKeyPresent) "RSA-2048 ✓" else "Not configured",
                        icon = "✍️"
                    )

                    SettingItem(
                        label = "Hardware Security",
                        value = if (securityStatus.hardwareBacked) "Enabled ✓" else "Software only",
                        icon = "🛡️"
                    )

                    SettingItem(
                        label = "Network Mode",
                        value = "Offline Only ✓",
                        icon = "📴"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy Options
            Text(
                text = "Privacy Options",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SettingToggle(
                        label = "Offline Mode",
                        description = "All operations run locally",
                        enabled = true,
                        onToggle = {}
                    )

                    Divider(
                        color = SafeSphereColors.TextSecondary.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    SettingToggle(
                        label = "Auto Lock",
                        description = "Lock vault when app is closed",
                        enabled = true,
                        onToggle = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data Management
            Text(
                text = "Data Management",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            GlassButton(
                text = "Clear All Vault Data",
                onClick = { viewModel.clearVault() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            GlassButton(
                text = "Clear Chat History",
                onClick = { viewModel.clearChat() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            GlassButton(
                text = "Load Demo Data",
                onClick = { viewModel.initializeDemoData() },
                primary = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // About
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔐",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SafeSphere",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )
                    Text(
                        text = "Privacy-First Security Platform",
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Powered by RunAnywhere SDK",
                        fontSize = 12.sp,
                        color = SafeSphereColors.Primary
                    )
                }
            }
        }
    }
}

@Composable
fun SettingItem(label: String, value: String, icon: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Text(
                text = label,
                fontSize = 15.sp,
                color = SafeSphereColors.TextSecondary
            )
        }

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = SafeSphereColors.Primary
        )
    }
}

@Composable
fun SettingToggle(
    label: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SafeSphereColors.TextPrimary
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = SafeSphereColors.TextSecondary
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SafeSphereColors.Primary,
                checkedTrackColor = SafeSphereColors.Primary.copy(alpha = 0.5f)
            )
        )
    }
}

/**
 * Models Screen - AI model management
 */
@Composable
fun ModelsScreen(viewModel: SafeSphereViewModel) {
    var models by remember { mutableStateOf<List<ModelInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var downloadingModel by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            models = listAvailableModels()
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SafeSphereHeader(
            title = "AI Models",
            subtitle = "Offline Intelligence",
            onBackClick = { viewModel.navigateToScreen(SafeSphereScreen.DASHBOARD) }
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SafeSphereColors.Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "ℹ️",
                                fontSize = 24.sp
                            )
                            Column {
                                Text(
                                    text = "Privacy AI Models",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SafeSphereColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Download models to enable offline AI chat. All inference runs locally on your device.",
                                    fontSize = 14.sp,
                                    color = SafeSphereColors.TextSecondary,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                items(models) { model ->
                    ModelCard(
                        model = model,
                        isDownloading = downloadingModel == model.id,
                        downloadProgress = if (downloadingModel == model.id) downloadProgress else 0f,
                        onDownload = {
                            downloadingModel = model.id
                            coroutineScope.launch {
                                try {
                                    RunAnywhere.downloadModel(model.id).collect { progress ->
                                        downloadProgress = progress
                                    }
                                    downloadingModel = null
                                } catch (e: Exception) {
                                    downloadingModel = null
                                }
                            }
                        },
                        onLoad = {
                            coroutineScope.launch {
                                RunAnywhere.loadModel(model.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ModelCard(
    model: ModelInfo,
    isDownloading: Boolean,
    downloadProgress: Float,
    onDownload: () -> Unit,
    onLoad: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = model.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Offline AI Model",
                fontSize = 14.sp,
                color = SafeSphereColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isDownloading) {
                LinearProgressIndicator(
                    progress = downloadProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = SafeSphereColors.Primary,
                    trackColor = SafeSphereColors.SurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Downloading: ${(downloadProgress * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = SafeSphereColors.Primary
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!model.isDownloaded) {
                        GlassButton(
                            text = "Download",
                            onClick = onDownload,
                            primary = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        GlassButton(
                            text = "Downloaded ✓",
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        )

                        GlassButton(
                            text = "Load Model",
                            onClick = onLoad,
                            primary = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// Utility functions
fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
