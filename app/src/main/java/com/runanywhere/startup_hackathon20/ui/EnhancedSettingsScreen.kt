package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen

/**
 * Enhanced Settings Screen - Pro Level
 * Real-time analytics, beautiful dark theme, comprehensive control panel
 */
@Composable
fun EnhancedSettingsScreen(viewModel: SafeSphereViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vaultItems by viewModel.vaultItems.collectAsState()
    val stats by viewModel.storageStats.collectAsState()

    val passwordRepository = remember {
        com.runanywhere.startup_hackathon20.data.PasswordVaultRepository.getInstance(context)
    }
    val savedPasswords by passwordRepository.passwords.collectAsState()

    // Real-time security status
    val securityStatus = remember {
        com.runanywhere.startup_hackathon20.security.SecurityManager.getSecurityStatus()
    }

    // Settings state
    var isBiometricEnabled by remember {
        mutableStateOf(
            context.getSharedPreferences("safesphere_prefs", android.content.Context.MODE_PRIVATE)
                .getBoolean("biometric_enabled", false)
        )
    }

    var isVoiceControlEnabled by remember {
        mutableStateOf(
            context.getSharedPreferences("safesphere_prefs", android.content.Context.MODE_PRIVATE)
                .getBoolean("voice_control_enabled", false)
        )
    }

    val biometricAvailable = remember {
        com.runanywhere.startup_hackathon20.security.BiometricAuthManager.isBiometricAvailable(
            context
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SafeSphereColors.Background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // App Overview Card
        item {
            AppOverviewCard(
                vaultItems = vaultItems.size,
                passwords = savedPasswords.size,
                securityScore = stats.securityScore,
                storageUsed = stats.totalSize
            )
        }

        // Security Status
        item {
            Text(
                text = "🔐 Security Status",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            SecurityStatusGrid(securityStatus = securityStatus)
        }

        // Privacy Controls
        item {
            Text(
                text = "🛡️ Privacy Controls",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            PrivacyControlsCard(
                isBiometricEnabled = isBiometricEnabled,
                isVoiceControlEnabled = isVoiceControlEnabled,
                biometricAvailable = biometricAvailable,
                context = context,
                onBiometricToggle = { enabled ->
                    if (biometricAvailable is com.runanywhere.startup_hackathon20.security.BiometricAvailability.Available) {
                        val prefs = context.getSharedPreferences(
                            "safesphere_prefs",
                            android.content.Context.MODE_PRIVATE
                        )
                        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
                        if (!enabled) {
                            com.runanywhere.startup_hackathon20.security.BiometricAuthManager.disableBiometricLogin(
                                context
                            )
                        }
                        isBiometricEnabled = enabled
                    }
                },
                onVoiceToggle = { enabled ->
                    val prefs = context.getSharedPreferences(
                        "safesphere_prefs",
                        android.content.Context.MODE_PRIVATE
                    )
                    prefs.edit().putBoolean("voice_control_enabled", enabled).apply()
                    isVoiceControlEnabled = enabled
                }
            )
        }

        // Quick Actions
        item {
            Text(
                text = "⚡ Quick Actions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            QuickActionsGrid(viewModel = viewModel)
        }

        // About
        item {
            AboutCard()
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

/**
 * App Overview Card with Real-Time Data
 */
@Composable
private fun AppOverviewCard(
    vaultItems: Int,
    passwords: Int,
    securityScore: Int,
    storageUsed: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SafeSphereColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🔐",
                fontSize = 48.sp
            )

            Text(
                text = "SafeSphere",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Text(
                text = "Privacy-First Security Platform",
                fontSize = 14.sp,
                color = SafeSphereColors.TextSecondary
            )

            Divider(
                color = SafeSphereColors.TextSecondary.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(icon = "📦", value = "$vaultItems", label = "Items")
                StatItem(icon = "🗝️", value = "$passwords", label = "Passwords")
                StatItem(icon = "🛡️", value = "$securityScore", label = "Score")
            }
        }
    }
}

/**
 * Stat Item
 */
@Composable
private fun StatItem(icon: String, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = icon, fontSize = 28.sp)
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SafeSphereColors.Primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = SafeSphereColors.TextSecondary
        )
    }
}

/**
 * Security Status Grid
 */
@Composable
private fun SecurityStatusGrid(securityStatus: com.runanywhere.startup_hackathon20.security.SecurityStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecurityCard(
                icon = "🔐",
                title = "Encryption",
                value = if (securityStatus.aesKeyPresent) "AES-256" else "Disabled",
                isActive = securityStatus.aesKeyPresent,
                modifier = Modifier.weight(1f)
            )
            SecurityCard(
                icon = "✍️",
                title = "Signatures",
                value = if (securityStatus.rsaKeyPresent) "RSA-2048" else "Disabled",
                isActive = securityStatus.rsaKeyPresent,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecurityCard(
                icon = "🛡️",
                title = "Hardware",
                value = if (securityStatus.hardwareBacked) "Secure" else "Software",
                isActive = securityStatus.hardwareBacked,
                modifier = Modifier.weight(1f)
            )
            SecurityCard(
                icon = "📴",
                title = "Network",
                value = "Offline",
                isActive = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Security Card
 */
@Composable
private fun SecurityCard(
    icon: String,
    title: String,
    value: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isActive) Color(0xFF4CAF50) else Color(0xFFFF9800)

    Card(
        modifier = modifier
            .border(
                width = 2.dp,
                color = color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = SafeSphereColors.SurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.15f),
                            SafeSphereColors.SurfaceVariant
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, fontSize = 32.sp)
            Text(
                text = title,
                fontSize = 12.sp,
                color = SafeSphereColors.TextSecondary
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Privacy Controls Card
 */
@Composable
private fun PrivacyControlsCard(
    isBiometricEnabled: Boolean,
    isVoiceControlEnabled: Boolean,
    biometricAvailable: com.runanywhere.startup_hackathon20.security.BiometricAvailability,
    context: android.content.Context,
    onBiometricToggle: (Boolean) -> Unit,
    onVoiceToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SafeSphereColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Biometric Toggle
            SettingToggleRow(
                icon = "👆",
                title = "Biometric Login",
                description = if (biometricAvailable is com.runanywhere.startup_hackathon20.security.BiometricAvailability.Available) {
                    "Use fingerprint or face ID to unlock"
                } else {
                    "Not available on this device"
                },
                enabled = isBiometricEnabled,
                onToggle = onBiometricToggle
            )

            Divider(color = SafeSphereColors.TextSecondary.copy(alpha = 0.1f))

            // Voice Control Toggle
            SettingToggleRow(
                icon = "🎤",
                title = "Voice Control",
                description = "Control SafeSphere with voice commands",
                enabled = isVoiceControlEnabled,
                onToggle = onVoiceToggle
            )

            Divider(color = SafeSphereColors.TextSecondary.copy(alpha = 0.1f))

            // Offline Mode (Always On)
            SettingToggleRow(
                icon = "📴",
                title = "Offline Mode",
                description = "All operations run locally",
                enabled = true,
                onToggle = {},
                disabled = true
            )

            Divider(color = SafeSphereColors.TextSecondary.copy(alpha = 0.1f))

            // Auto Lock (Always On)
            SettingToggleRow(
                icon = "🔒",
                title = "Auto Lock",
                description = "Lock vault when app closes",
                enabled = true,
                onToggle = {},
                disabled = true
            )
        }
    }
}

/**
 * Setting Toggle Row
 */
@Composable
private fun SettingToggleRow(
    icon: String,
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    disabled: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = icon, fontSize = 32.sp)

            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = SafeSphereColors.TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Switch(
            checked = enabled,
            onCheckedChange = if (!disabled) onToggle else {
                {}
            },
            enabled = !disabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SafeSphereColors.Primary,
                checkedTrackColor = SafeSphereColors.Primary.copy(alpha = 0.5f),
                uncheckedThumbColor = SafeSphereColors.TextSecondary,
                uncheckedTrackColor = SafeSphereColors.SurfaceVariant
            )
        )
    }
}

/**
 * Quick Actions Grid
 */
@Composable
private fun QuickActionsGrid(viewModel: SafeSphereViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionButton(
            icon = "📊",
            title = "Load Demo Data",
            description = "Test SafeSphere with sample data",
            color = SafeSphereColors.Primary,
            onClick = { viewModel.initializeDemoData() }
        )

        ActionButton(
            icon = "💬",
            title = "Clear Chat History",
            description = "Delete all AI chat messages",
            color = SafeSphereColors.Secondary,
            onClick = { viewModel.clearChat() }
        )

        ActionButton(
            icon = "🗑️",
            title = "Clear Vault Data",
            description = "Delete all encrypted vault items",
            color = Color(0xFFF44336),
            onClick = { viewModel.clearVault() }
        )
    }
}

/**
 * Action Button
 */
@Composable
private fun ActionButton(
    icon: String,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SafeSphereColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.15f),
                            SafeSphereColors.Surface
                        )
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 24.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * About Card
 */
@Composable
private fun AboutCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SafeSphereColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🔐",
                fontSize = 48.sp
            )

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

            Text(
                text = "100% Offline • Zero Tracking • Military-Grade Encryption",
                fontSize = 11.sp,
                color = SafeSphereColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}
