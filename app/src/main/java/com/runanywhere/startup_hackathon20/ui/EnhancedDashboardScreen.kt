package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced Dashboard Screen - Pro Level
 * Real-time analytics, beautiful dark theme, comprehensive overview
 */
@Composable
fun EnhancedDashboardScreen(viewModel: SafeSphereViewModel) {
    val vaultItems by viewModel.vaultItems.collectAsState()
    val stats by viewModel.storageStats.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val passwordRepository = remember {
        com.runanywhere.startup_hackathon20.data.PasswordVaultRepository.getInstance(context)
    }
    val savedPasswords by passwordRepository.passwords.collectAsState()

    // Real-time analytics
    val analytics = remember(vaultItems, savedPasswords) {
        DashboardAnalytics(
            totalVaultItems = vaultItems.size,
            encryptedItems = vaultItems.count { it.isEncrypted },
            totalPasswords = savedPasswords.size,
            weakPasswords = savedPasswords.count { it.strengthScore < 60 },
            securityScore = stats.securityScore,
            storageUsed = stats.totalSize
        )
    }

    // Password health analysis
    val passwordHealth = remember(vaultItems) {
        if (vaultItems.isEmpty()) null
        else com.runanywhere.startup_hackathon20.utils.PasswordAnalyzer.analyzePasswords(vaultItems) {
            com.runanywhere.startup_hackathon20.security.SecurityManager.decrypt(it)
        }
    }

    val breachedCount =
        passwordHealth?.passwordDetails?.count { it.breachResult?.isBreached == true } ?: 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SafeSphereColors.Background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Welcome Section
        item {
            WelcomeSection()
        }

        // Security Overview Card
        item {
            SecurityOverviewCard(
                securityScore = analytics.securityScore,
                encryptedItems = analytics.encryptedItems,
                totalItems = analytics.totalVaultItems
            )
        }

        // Critical Alerts (if any)
        if (breachedCount > 0 || analytics.weakPasswords > 0) {
            item {
                CriticalAlertsCard(
                    breachedCount = breachedCount,
                    weakCount = analytics.weakPasswords,
                    onFixClick = { viewModel.navigateToScreen(SafeSphereScreen.PASSWORD_HEALTH) }
                )
            }
        }

        // Real-Time Stats Grid
        item {
            Text(
                text = "📊 Real-Time Analytics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            RealTimeStatsGrid(analytics = analytics)
        }

        // Auto-Scrolling Feature Carousel
        item {
            Text(
                text = "✨ Discover Features",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            AutoScrollingFeatureCarousel(viewModel = viewModel)
        }

        // Features Grid
        item {
            Text(
                text = "🚀 Features",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            FeaturesGrid(viewModel = viewModel, analytics = analytics)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

/**
 * Welcome Section
 */
@Composable
private fun WelcomeSection() {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val currentDate = remember { dateFormat.format(Date()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to SafeSphere",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SafeSphereColors.TextPrimary
        )
        Text(
            text = currentDate,
            fontSize = 14.sp,
            color = SafeSphereColors.TextSecondary
        )
    }
}

/**
 * Security Overview Card with Circular Progress
 */
@Composable
private fun SecurityOverviewCard(
    securityScore: Int,
    encryptedItems: Int,
    totalItems: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = securityScore / 100f,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutCubic)
    )

    val scoreColor = when {
        securityScore >= 90 -> Color(0xFF4CAF50)
        securityScore >= 70 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SafeSphereColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            scoreColor.copy(alpha = 0.15f),
                            SafeSphereColors.Surface
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🛡️ Security Status",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                // Circular Progress
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = 1f,
                        modifier = Modifier.fillMaxSize(),
                        color = SafeSphereColors.SurfaceVariant,
                        strokeWidth = 14.dp
                    )

                    CircularProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.fillMaxSize(),
                        color = scoreColor,
                        strokeWidth = 14.dp
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$securityScore",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                        Text(
                            text = when {
                                securityScore >= 90 -> "Excellent"
                                securityScore >= 70 -> "Good"
                                else -> "Needs Work"
                            },
                            fontSize = 16.sp,
                            color = scoreColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Encryption Status
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔒",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$encryptedItems of $totalItems items encrypted",
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Critical Alerts Card
 */
@Composable
private fun CriticalAlertsCard(
    breachedCount: Int,
    weakCount: Int,
    onFixClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SafeSphereColors.Surface),
        border = BorderStroke(2.dp, Color(0xFFF44336).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🚨 Critical Alerts",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF44336),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (breachedCount > 0) {
                AlertRow(
                    icon = "🚨",
                    text = "$breachedCount Leaked Password${if (breachedCount > 1) "s" else ""} - Act Now!",
                    color = Color(0xFFF44336)
                )
            }

            if (weakCount > 0) {
                AlertRow(
                    icon = "⚠️",
                    text = "$weakCount Weak Password${if (weakCount > 1) "s" else ""} Detected",
                    color = Color(0xFFFF9800)
                )
            }

            Button(
                onClick = onFixClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fix Security Issues", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Alert Row
 */
@Composable
private fun AlertRow(icon: String, text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SafeSphereColors.SurfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = icon, fontSize = 24.sp)
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

/**
 * Real-Time Stats Grid
 */
@Composable
private fun RealTimeStatsGrid(analytics: DashboardAnalytics) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = "🔐",
                value = "${analytics.totalVaultItems}",
                label = "Vault Items",
                color = SafeSphereColors.Primary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = "🗝️",
                value = "${analytics.totalPasswords}",
                label = "Passwords",
                color = SafeSphereColors.Secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = "🔒",
                value = "${analytics.encryptedItems}",
                label = "Encrypted",
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = "💾",
                value = formatBytes(analytics.storageUsed),
                label = "Storage",
                color = SafeSphereColors.Accent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Stat Card
 */
@Composable
private fun StatCard(
    icon: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SafeSphereColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Auto-Scrolling Feature Carousel
 * Continuously loops cards from left to right with smooth animation
 */
@Composable
private fun AutoScrollingFeatureCarousel(viewModel: SafeSphereViewModel) {
    val features = remember {
        listOf(
            FeatureHighlight(
                icon = "🔐",
                title = "Military-Grade Encryption",
                description = "AES-256-GCM keeps data secure",
                gradient = listOf(Color(0xFF2196F3), Color(0xFF1976D2)),
                screen = SafeSphereScreen.PRIVACY_VAULT
            ),
            FeatureHighlight(
                icon = "🤖",
                title = "Offline AI Assistant",
                description = "Privacy AI runs on-device",
                gradient = listOf(Color(0xFF00BCD4), Color(0xFF0097A7)),
                screen = SafeSphereScreen.AI_CHAT
            ),
            FeatureHighlight(
                icon = "🗝️",
                title = "Password Manager",
                description = "Breach detection & autofill",
                gradient = listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2)),
                screen = SafeSphereScreen.PASSWORDS
            ),
            FeatureHighlight(
                icon = "🛡️",
                title = "Threat Simulation",
                description = "Interactive security training",
                gradient = listOf(Color(0xFFFF5722), Color(0xFFE64A19)),
                screen = SafeSphereScreen.THREAT_SIMULATION
            ),
            FeatureHighlight(
                icon = "📊",
                title = "Data Visualization",
                description = "Beautiful encrypted insights",
                gradient = listOf(Color(0xFFFFC107), Color(0xFFFFA000)),
                screen = SafeSphereScreen.DATA_MAP
            ),
            FeatureHighlight(
                icon = "🔍",
                title = "Password Health",
                description = "Real-time breach detection",
                gradient = listOf(Color(0xFF4CAF50), Color(0xFF388E3C)),
                screen = SafeSphereScreen.PASSWORD_HEALTH
            ),
            FeatureHighlight(
                icon = "🎯",
                title = "AI Risk Predictor",
                description = "Predict future threats with ML",
                gradient = listOf(Color(0xFFE91E63), Color(0xFFC2185B)),
                screen = SafeSphereScreen.AI_PREDICTOR
            ),
            FeatureHighlight(
                icon = "🧠",
                title = "Offline AI Models",
                description = "Manage models locally",
                gradient = listOf(Color(0xFF3F51B5), Color(0xFF303F9F)),
                screen = SafeSphereScreen.MODELS
            )
        )
    }

    // Create very large infinite list for seamless scrolling (10x repetition)
    val infiniteFeatures = remember {
        buildList {
            repeat(10) {
                addAll(features)
            }
        }
    }

    val scrollState = rememberLazyListState(initialFirstVisibleItemIndex = features.size * 5)

    // Smooth continuous pixel-by-pixel auto-scroll (LEFT to RIGHT)
    LaunchedEffect(Unit) {
        while (true) {
            // Scroll BACKWARD (negative) for left-to-right visual movement
            // 2 pixels at a time for smooth slow scrolling
            scrollState.scrollBy(-2f)

            // Check if we need to loop back
            if (scrollState.firstVisibleItemIndex < features.size) {
                // Jump to safe position in the middle
                scrollState.scrollToItem(
                    index = infiniteFeatures.size - features.size * 3,
                    scrollOffset = 0
                )
            }

            // Control speed - 30ms delay = smooth slow continuous motion
            kotlinx.coroutines.delay(30)
        }
    }

    LazyRow(
        state = scrollState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxWidth(),
        userScrollEnabled = false // Disable user scroll for auto-scroll only
    ) {
        items(infiniteFeatures.size) { index ->
            val feature = infiniteFeatures[index]
            FeatureCarouselCard(
                feature = feature,
                onClick = { viewModel.navigateToScreen(feature.screen) }
            )
        }
    }
}

/**
 * Feature Carousel Card with Enhanced UI - Smaller & Centered
 */
@Composable
private fun FeatureCarouselCard(
    feature: FeatureHighlight,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(140.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SafeSphereColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = feature.gradient.map { it.copy(alpha = 0.15f) }
                        )
                    )
            )

            // Decorative pattern overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                feature.gradient[0].copy(alpha = 0.1f),
                                Color.Transparent,
                                feature.gradient[1].copy(alpha = 0.05f)
                            ),
                            center = androidx.compose.ui.geometry.Offset(300f, 150f),
                            radius = 400f
                        )
                    )
            )

            // Content - All Centered
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Centered icon with gradient background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(colors = feature.gradient)
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = feature.icon,
                        fontSize = 28.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Centered title
                Text(
                    text = feature.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Centered description
                Text(
                    text = feature.description,
                    fontSize = 11.sp,
                    color = SafeSphereColors.TextSecondary,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom gradient bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.horizontalGradient(colors = feature.gradient)
                    )
            )

            // Shine effect overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.25f)
                    .fillMaxHeight()
                    .align(Alignment.TopStart)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Feature Highlight Data Class
 */
private data class FeatureHighlight(
    val icon: String,
    val title: String,
    val description: String,
    val gradient: List<Color>,
    val screen: SafeSphereScreen
)

/**
 * Features Grid
 */
@Composable
private fun FeaturesGrid(viewModel: SafeSphereViewModel, analytics: DashboardAnalytics) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                icon = "🔐",
                title = "Privacy Vault",
                description = "${analytics.totalVaultItems} items",
                color = SafeSphereColors.Primary,
                onClick = { viewModel.navigateToScreen(SafeSphereScreen.PRIVACY_VAULT) },
                modifier = Modifier.weight(1f)
            )
            FeatureCard(
                icon = "🗝️",
                title = "Passwords",
                description = "${analytics.totalPasswords} saved",
                color = SafeSphereColors.Secondary,
                onClick = { viewModel.navigateToScreen(SafeSphereScreen.PASSWORDS) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                icon = "💬",
                title = "AI Assistant",
                description = "Offline chat",
                color = SafeSphereColors.Accent,
                onClick = { viewModel.navigateToScreen(SafeSphereScreen.AI_CHAT) },
                modifier = Modifier.weight(1f)
            )
            FeatureCard(
                icon = "📊",
                title = "Data Insights",
                description = "Visualize data",
                color = Color(0xFFFFC107),
                onClick = { viewModel.navigateToScreen(SafeSphereScreen.DATA_MAP) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                icon = "🛡️",
                title = "Threats",
                description = "Learn security",
                color = Color(0xFFFF5722),
                onClick = { viewModel.navigateToScreen(SafeSphereScreen.THREAT_SIMULATION) },
                modifier = Modifier.weight(1f)
            )
            FeatureCard(
                icon = "🤖",
                title = "AI Predictor",
                description = "Risk analysis",
                color = Color(0xFF9C27B0),
                onClick = { viewModel.navigateToScreen(SafeSphereScreen.AI_PREDICTOR) },
                modifier = Modifier.weight(1f)
            )
        }

        // Full width card for Models
        FeatureCard(
            icon = "🧠",
            title = "AI Models",
            description = "Manage offline AI models",
            color = SafeSphereColors.Info,
            onClick = { viewModel.navigateToScreen(SafeSphereScreen.MODELS) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Feature Card
 */
@Composable
private fun FeatureCard(
    icon: String,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SafeSphereColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.15f),
                            SafeSphereColors.Surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = icon, fontSize = 32.sp)
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
                        fontWeight = FontWeight.Bold,
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
}

/**
 * Dashboard Analytics Data Class
 */
private data class DashboardAnalytics(
    val totalVaultItems: Int,
    val encryptedItems: Int,
    val totalPasswords: Int,
    val weakPasswords: Int,
    val securityScore: Int,
    val storageUsed: Long
)

