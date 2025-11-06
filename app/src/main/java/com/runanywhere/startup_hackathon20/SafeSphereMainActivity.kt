package com.runanywhere.startup_hackathon20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.data.*
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen
import com.runanywhere.startup_hackathon20.ui.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * SafeSphere - Privacy-First Mobile Application
 *
 * Main Activity containing all UI screens and navigation
 *
 * Features:
 * - Onboarding & Awareness
 * - Dashboard with quick access
 * - Privacy Vault (encrypted storage)
 * - Offline AI Chat
 * - Data Map (visualization)
 * - Threat Simulation (educational)
 * - Settings & Consent
 * - Model Management
 */
class SafeSphereMainActivity : ComponentActivity() {

    private val viewModel: SafeSphereViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SafeSphereTheme {
                SafeSphereApp(viewModel)
            }
        }
    }
}

/**
 * Main app container with navigation
 */
@Composable
fun SafeSphereApp(viewModel: SafeSphereViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SafeSphereColors.Background
    ) {
        // Background gradient effect
        Box(
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
            // Main content
            when (currentScreen) {
                SafeSphereScreen.ONBOARDING -> OnboardingScreen(viewModel)
                SafeSphereScreen.DASHBOARD -> DashboardScreen(viewModel)
                SafeSphereScreen.PRIVACY_VAULT -> PrivacyVaultScreen(viewModel)
                SafeSphereScreen.AI_CHAT -> AIChatScreen(viewModel)
                SafeSphereScreen.DATA_MAP -> DataMapScreen(viewModel)
                SafeSphereScreen.THREAT_SIMULATION -> ThreatSimulationScreen(viewModel)
                SafeSphereScreen.SETTINGS -> SettingsScreen(viewModel)
                SafeSphereScreen.MODELS -> ModelsScreen(viewModel)
            }

            // Show UI messages as snackbar
            uiMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = SafeSphereColors.Primary,
                    contentColor = Color.White
                ) {
                    Text(message)
                }
                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.clearMessage()
                }
            }
        }
    }
}

/**
 * Onboarding Screen - Explains privacy concepts
 */
@Composable
fun OnboardingScreen(viewModel: SafeSphereViewModel) {
    var currentPage by remember { mutableStateOf(0) }
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to SafeSphere",
            description = "Your privacy fortress. All data stays on your device, encrypted with military-grade AES-256.",
            icon = "🔐",
            color = SafeSphereColors.Primary
        ),
        OnboardingPage(
            title = "Offline AI Power",
            description = "AI runs entirely on your device. No cloud, no tracking, no data leaks. Complete privacy.",
            icon = "🤖",
            color = SafeSphereColors.Secondary
        ),
        OnboardingPage(
            title = "Hardware Encryption",
            description = "Your encryption keys are stored in secure hardware. Even if someone steals your phone, they can't access your data.",
            icon = "🛡️",
            color = SafeSphereColors.Accent
        ),
        OnboardingPage(
            title = "You're in Control",
            description = "No backdoors. No cloud sync. No tracking. Your data belongs to you, and only you.",
            icon = "✨",
            color = SafeSphereColors.Success
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Page indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(pages.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentPage) 32.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentPage) SafeSphereColors.Primary
                            else SafeSphereColors.TextSecondary.copy(alpha = 0.3f)
                        )
                        .animateContentSize()
                )
            }
        }

        // Content
        val page = pages[currentPage]
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = page.icon,
                fontSize = 80.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = page.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = page.description,
                fontSize = 16.sp,
                color = SafeSphereColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentPage > 0) {
                GlassButton(
                    text = "Back",
                    onClick = { currentPage-- },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.width(16.dp))

            GlassButton(
                text = if (currentPage == pages.size - 1) "Get Started" else "Next",
                onClick = {
                    if (currentPage == pages.size - 1) {
                        viewModel.initializeDemoData()
                        viewModel.navigateToScreen(SafeSphereScreen.DASHBOARD)
                    } else {
                        currentPage++
                    }
                },
                primary = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: String,
    val color: Color
)

/**
 * Dashboard Screen - Central hub
 */
@Composable
fun DashboardScreen(viewModel: SafeSphereViewModel) {
    val isOffline by viewModel.isOfflineMode.collectAsState()
    val stats by viewModel.storageStats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        SafeSphereHeader(
            title = "SafeSphere",
            subtitle = if (isOffline) "🔒 Offline Secure Mode" else "Online",
            onSettingsClick = { viewModel.navigateToScreen(SafeSphereScreen.SETTINGS) }
        )

        Column(modifier = Modifier.padding(16.dp)) {
            // Security Score Card
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Security Score",
                        fontSize = 16.sp,
                        color = SafeSphereColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = stats.securityScore / 100f,
                            modifier = Modifier.size(120.dp),
                            strokeWidth = 12.dp,
                            color = when {
                                stats.securityScore >= 90 -> SafeSphereColors.Success
                                stats.securityScore >= 70 -> SafeSphereColors.Warning
                                else -> SafeSphereColors.Error
                            },
                            trackColor = SafeSphereColors.TextSecondary.copy(alpha = 0.1f)
                        )

                        Text(
                            text = "${stats.securityScore}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${stats.encryptedItems} of ${stats.totalItems} items encrypted",
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Access Grid
            Text(
                text = "Quick Access",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DashboardCard(
                        title = "Privacy Vault",
                        icon = "🔐",
                        description = "${stats.totalItems} items",
                        color = SafeSphereColors.Primary,
                        onClick = { viewModel.navigateToScreen(SafeSphereScreen.PRIVACY_VAULT) },
                        modifier = Modifier.weight(1f)
                    )

                    DashboardCard(
                        title = "AI Chat",
                        icon = "💬",
                        description = "Offline advisor",
                        color = SafeSphereColors.Secondary,
                        onClick = { viewModel.navigateToScreen(SafeSphereScreen.AI_CHAT) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DashboardCard(
                        title = "Data Map",
                        icon = "📊",
                        description = "Visualize storage",
                        color = SafeSphereColors.Accent,
                        onClick = { viewModel.navigateToScreen(SafeSphereScreen.DATA_MAP) },
                        modifier = Modifier.weight(1f)
                    )

                    DashboardCard(
                        title = "Threats",
                        icon = "🛡️",
                        description = "Simulation zone",
                        color = SafeSphereColors.Warning,
                        onClick = { viewModel.navigateToScreen(SafeSphereScreen.THREAT_SIMULATION) },
                        modifier = Modifier.weight(1f)
                    )
                }

                DashboardCard(
                    title = "Manage AI Models",
                    icon = "🤖",
                    description = "Download offline AI",
                    color = SafeSphereColors.Info,
                    onClick = { viewModel.navigateToScreen(SafeSphereScreen.MODELS) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    icon: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = icon,
                    fontSize = 32.sp
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeSphereColors.TextPrimary
                )

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Privacy Vault Screen - Encrypted storage management
 */
@Composable
fun PrivacyVaultScreen(viewModel: SafeSphereViewModel) {
    val vaultItems by viewModel.vaultItems.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<PrivacyVaultItem?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        SafeSphereHeader(
            title = "Privacy Vault",
            subtitle = "${vaultItems.size} encrypted items",
            onBackClick = { viewModel.navigateToScreen(SafeSphereScreen.DASHBOARD) },
            onActionClick = { showAddDialog = true },
            actionIcon = "+"
        )

        if (vaultItems.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🔐",
                    fontSize = 64.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your vault is empty",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Add sensitive data to store it encrypted",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                GlassButton(
                    text = "Add First Item",
                    onClick = { showAddDialog = true },
                    primary = true
                )
            }
        } else {
            // Vault items list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(vaultItems) { item ->
                    VaultItemCard(
                        item = item,
                        onClick = { selectedItem = item }
                    )
                }
            }
        }
    }

    // Add/Edit Dialog
    if (showAddDialog) {
        AddVaultItemDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, content, category ->
                viewModel.addVaultItem(title, content, category)
                showAddDialog = false
            }
        )
    }

    // View Item Dialog
    selectedItem?.let { item ->
        ViewVaultItemDialog(
            item = item,
            viewModel = viewModel,
            onDismiss = { selectedItem = null }
        )
    }
}

@Composable
fun VaultItemCard(
    item: PrivacyVaultItem,
    onClick: () -> Unit
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
            // Category icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SafeSphereColors.Primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.category.icon,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeSphereColors.TextPrimary
                )

                Text(
                    text = item.category.displayName,
                    fontSize = 12.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }

            if (item.isEncrypted) {
                Text(
                    text = "🔒",
                    fontSize = 20.sp
                )
            }
        }
    }
}

// ... Continue in next part ...
