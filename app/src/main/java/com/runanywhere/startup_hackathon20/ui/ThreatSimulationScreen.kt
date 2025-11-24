package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen
import com.runanywhere.startup_hackathon20.data.ThreatEvent
import com.runanywhere.startup_hackathon20.data.ThreatSeverity
import com.runanywhere.startup_hackathon20.data.ThreatType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * PRO-LEVEL THREAT SIMULATION SCREEN
 *
 * Advanced Features:
 * - Real-time attack simulations
 * - Live network monitoring
 * - Interactive security demos
 * - Educational attack scenarios
 * - Animated threat visualizations
 * - Security quiz system
 * - Best practices tips
 * - Attack timeline
 * - Threat heatmap
 * - Defense success rate
 */
@Composable
fun AdvancedThreatSimulationScreen(viewModel: SafeSphereViewModel) {
    val threats by viewModel.threatEvents.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val threatsBlocked by viewModel.threatsBlocked.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()

    var activeTab by remember { mutableStateOf(ThreatTab.LIVE_MONITOR) }
    var selectedDemo by remember { mutableStateOf<ThreatDemo?>(null) }
    var showQuiz by remember { mutableStateOf(false) }

    // REAL-TIME STATS FROM ACTUAL DATA (NOT RANDOM)
    val attacksPerMinute = remember(threats) {
        // Calculate real attack frequency based on threat timestamps
        val now = System.currentTimeMillis()
        val oneMinuteAgo = now - 60_000
        threats.count { it.timestamp > oneMinuteAgo }
    }

    val defenseRate = remember(threats, threatsBlocked) {
        if (threats.isNotEmpty()) {
            (threatsBlocked.toFloat() / threats.size * 100)
        } else 100f
    }

    val riskLevel = remember(threats) {
        val unmitigatedThreats = threats.count { !it.mitigated }
        when {
            unmitigatedThreats >= 5 -> RiskLevel.CRITICAL
            unmitigatedThreats >= 3 -> RiskLevel.HIGH
            unmitigatedThreats >= 1 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    val totalThreatsToday = remember(threats) {
        val today = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        threats.count { it.timestamp > today }
    }

    val criticalThreats = remember(threats) {
        threats.count { it.severity == ThreatSeverity.CRITICAL && !it.mitigated }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Advanced Header with Live Status
        AdvancedThreatHeader(
            onBackClick = { viewModel.navigateToScreen(SafeSphereScreen.DASHBOARD) },
            isMonitoring = isMonitoring,
            riskLevel = riskLevel,
            onToggleMonitoring = { viewModel.toggleMonitoring() }
        )

        // Tab Bar
        ThreatTabBar(
            selectedTab = activeTab,
            onTabSelected = { activeTab = it }
        )

        // Content based on selected tab
        when (activeTab) {
            ThreatTab.LIVE_MONITOR -> LiveMonitorTab(
                viewModel = viewModel,
                threats = threats,
                isMonitoring = isMonitoring,
                threatsBlocked = threatsBlocked,
                attacksPerMinute = attacksPerMinute,
                defenseRate = defenseRate,
                riskLevel = riskLevel,
                networkStatus = networkStatus,
                totalThreatsToday = totalThreatsToday,
                criticalThreats = criticalThreats
            )

            ThreatTab.ATTACK_DEMOS -> AttackDemosTab(
                onDemoSelected = { selectedDemo = it },
                viewModel = viewModel
            )

            ThreatTab.SECURITY_TIPS -> SecurityTipsTab()

            ThreatTab.QUIZ -> SecurityQuizTab(viewModel = viewModel)
        }
    }

    // Demo Modal
    selectedDemo?.let { demo ->
        InteractiveDemo(
            demo = demo,
            onDismiss = { selectedDemo = null },
            viewModel = viewModel
        )
    }
}

enum class ThreatTab(val title: String, val icon: String) {
    LIVE_MONITOR("Live Monitor", "📡"),
    ATTACK_DEMOS("Attack Demos", "🎯"),
    SECURITY_TIPS("Security Tips", "💡"),
    QUIZ("Security Quiz", "🧠")
}

enum class RiskLevel(val displayName: String, val color: Color) {
    LOW("Low Risk", Color(0xFF4CAF50)),
    MEDIUM("Medium Risk", Color(0xFFFBC02D)),
    HIGH("High Risk", Color(0xFFFF6B6B)),
    CRITICAL("Critical Risk", Color(0xFFD32F2F))
}

/**
 * Advanced Threat Header with Real-time Status
 */
@Composable
private fun AdvancedThreatHeader(
    onBackClick: () -> Unit,
    isMonitoring: Boolean,
    riskLevel: RiskLevel,
    onToggleMonitoring: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SafeSphereColors.Surface.copy(alpha = 0.95f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            riskLevel.color.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Top row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = SafeSphereColors.Primary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Threat Simulation",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pulsing indicator
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse"
                        )

                        if (isMonitoring) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(riskLevel.color.copy(alpha = pulseAlpha))
                            )
                        }
                        Text(
                            text = if (isMonitoring) riskLevel.displayName else "Paused",
                            fontSize = 11.sp,
                            color = if (isMonitoring) riskLevel.color else SafeSphereColors.TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onToggleMonitoring,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isMonitoring) riskLevel.color.copy(alpha = 0.15f)
                            else SafeSphereColors.SurfaceVariant
                        )
                ) {
                    Text(
                        text = if (isMonitoring) "⏸" else "▶",
                        fontSize = 20.sp,
                        color = if (isMonitoring) riskLevel.color else SafeSphereColors.TextPrimary
                    )
                }
            }
        }
    }
}

/**
 * Tab Bar for different sections
 */
@Composable
private fun ThreatTabBar(
    selectedTab: ThreatTab,
    onTabSelected: (ThreatTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SafeSphereColors.Surface.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThreatTab.values().forEach { tab ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selectedTab == tab) SafeSphereColors.Primary.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .border(
                        width = if (selectedTab == tab) 2.dp else 1.dp,
                        color = if (selectedTab == tab) SafeSphereColors.Primary
                        else SafeSphereColors.TextSecondary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = tab.icon,
                        fontSize = 20.sp
                    )
                    Text(
                        text = tab.title,
                        fontSize = 10.sp,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == tab) SafeSphereColors.Primary
                        else SafeSphereColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Live Monitor Tab - Real-time Threat Monitoring
 */
@Composable
private fun LiveMonitorTab(
    viewModel: SafeSphereViewModel,
    threats: List<ThreatEvent>,
    isMonitoring: Boolean,
    threatsBlocked: Int,
    attacksPerMinute: Int,
    defenseRate: Float,
    riskLevel: RiskLevel,
    networkStatus: String,
    totalThreatsToday: Int,
    criticalThreats: Int
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real-time Stats Dashboard
        item {
            RealTimeStatsDashboard(
                threatsBlocked = threatsBlocked,
                attacksPerMinute = attacksPerMinute,
                defenseRate = defenseRate,
                riskLevel = riskLevel,
                isMonitoring = isMonitoring,
                networkStatus = networkStatus,
                totalThreatsToday = totalThreatsToday,
                criticalThreats = criticalThreats
            )
        }

        // Quick Actions
        item {
            QuickActionsPanel(viewModel = viewModel, isMonitoring = isMonitoring)
        }

        // Live Threats Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔴 Live Threats",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                if (threats.isNotEmpty()) {
                    Text(
                        text = "${threats.size} detected",
                        fontSize = 13.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }
            }
        }

        if (threats.isEmpty()) {
            item {
                EmptyThreatsState(isMonitoring = isMonitoring)
            }
        } else {
            items(threats) { threat ->
                EnhancedThreatCard(threat = threat)
            }
        }
    }
}

/**
 * Real-time Stats Dashboard - ENHANCED PROFESSIONAL DESIGN
 * DISPLAYS ACTUAL REAL-TIME DATA FROM USER ACTIVITY TRACKING
 */
@Composable
private fun RealTimeStatsDashboard(
    threatsBlocked: Int,
    attacksPerMinute: Int,
    defenseRate: Float,
    riskLevel: RiskLevel,
    isMonitoring: Boolean,
    networkStatus: String,
    totalThreatsToday: Int,
    criticalThreats: Int
) {
    // Get real-time activity data from ViewModel
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            "safesphere_realtime",
            android.content.Context.MODE_PRIVATE
        )
    }

    // REAL-TIME METRICS (persisted across app sessions)
    var totalAccesses by remember { mutableStateOf(prefs.getInt("total_accesses", 0)) }
    var totalPasswordViews by remember { mutableStateOf(prefs.getInt("total_password_views", 0)) }
    var totalVaultAccesses by remember { mutableStateOf(prefs.getInt("total_vault_accesses", 0)) }
    var sessionStartTime by remember {
        mutableStateOf(
            prefs.getLong(
                "session_start",
                System.currentTimeMillis()
            )
        )
    }

    // Calculate session duration in minutes
    val sessionDurationMinutes = remember(sessionStartTime) {
        val elapsedMs = System.currentTimeMillis() - sessionStartTime
        (elapsedMs / 1000 / 60).toInt().coerceAtLeast(1)
    }

    // Real activity rate (accesses per minute)
    val realActivityRate = remember(totalAccesses, sessionDurationMinutes) {
        (totalAccesses.toFloat() / sessionDurationMinutes).toInt()
    }

    // Update stats periodically
    LaunchedEffect(Unit) {
        while (true) {
            // Refresh stats every 5 seconds
            delay(5000)
            totalAccesses = prefs.getInt("total_accesses", 0)
            totalPasswordViews = prefs.getInt("total_password_views", 0)
            totalVaultAccesses = prefs.getInt("total_vault_accesses", 0)
        }
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, riskLevel.color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            riskLevel.color.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(24.dp)
        ) {
            // Header with title and monitoring badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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
                            text = "📊",
                            fontSize = 20.sp
                        )
                    }

                    Column {
                        Text(
                            text = "Real-Time Statistics",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.TextPrimary
                        )
                        if (isMonitoring) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Animated pulsing dot
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val pulseScale by infiniteTransition.animateFloat(
                                    initialValue = 0.8f,
                                    targetValue = 1.2f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulse"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(SafeSphereColors.Success)
                                )

                                Text(
                                    text = "Live Monitoring Active",
                                    fontSize = 11.sp,
                                    color = SafeSphereColors.Success,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Stats Grid (2x2) - REAL DATA
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: Blocked & Real Activity Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EnhancedStatCard(
                        icon = "🛡️",
                        value = "$threatsBlocked",
                        label = "Threats Blocked",
                        color = SafeSphereColors.Success,
                        gradient = listOf(
                            SafeSphereColors.Success.copy(alpha = 0.2f),
                            SafeSphereColors.Success.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    EnhancedStatCard(
                        icon = "⚡",
                        value = "$realActivityRate",
                        subtitle = "per min",
                        label = "Activity Rate",
                        color = SafeSphereColors.Warning,
                        gradient = listOf(
                            SafeSphereColors.Warning.copy(alpha = 0.2f),
                            SafeSphereColors.Warning.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Total Accesses & Vault Accesses (REAL DATA)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EnhancedStatCard(
                        icon = "📈",
                        value = "$totalAccesses",
                        label = "Total Actions",
                        color = SafeSphereColors.Secondary,
                        gradient = listOf(
                            SafeSphereColors.Secondary.copy(alpha = 0.2f),
                            SafeSphereColors.Secondary.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    EnhancedStatCard(
                        icon = "🚨",
                        value = "$criticalThreats",
                        label = "Critical Alerts",
                        color = if (criticalThreats > 0) Color(0xFFFF6B6B) else SafeSphereColors.Success,
                        gradient = if (criticalThreats > 0) listOf(
                            Color(0xFFFF6B6B).copy(alpha = 0.2f),
                            Color(0xFFFF6B6B).copy(alpha = 0.05f)
                        ) else listOf(
                            SafeSphereColors.Success.copy(alpha = 0.2f),
                            SafeSphereColors.Success.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Session Duration & Statistics (NEW - REAL DATA)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Session Duration
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SafeSphereColors.Primary.copy(alpha = 0.1f))
                        .padding(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⏱️",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${sessionDurationMinutes}m",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.Primary
                        )
                        Text(
                            text = "Session",
                            fontSize = 10.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                    }
                }

                // Password Views
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF9C27B0).copy(alpha = 0.1f))
                        .padding(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🔑",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$totalPasswordViews",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9C27B0)
                        )
                        Text(
                            text = "Passwords",
                            fontSize = 10.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                    }
                }

                // Vault Accesses
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF00BCD4).copy(alpha = 0.1f))
                        .padding(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🔐",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$totalVaultAccesses",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00BCD4)
                        )
                        Text(
                            text = "Vault Items",
                            fontSize = 10.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Defense Rate Progress with enhanced design
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SafeSphereColors.SurfaceVariant.copy(alpha = 0.3f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Defense Success Rate",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SafeSphereColors.TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SafeSphereColors.Success.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${defenseRate.toInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.Success
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Animated progress bar with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(SafeSphereColors.SurfaceVariant)
                ) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = defenseRate / 100f,
                        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        label = "defense_progress"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        SafeSphereColors.Success,
                                        SafeSphereColors.Primary
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Network Status Banner with enhanced design
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (networkStatus.contains("Offline") || networkStatus.contains("Secure"))
                            SafeSphereColors.Success.copy(alpha = 0.1f)
                        else
                            SafeSphereColors.Warning.copy(alpha = 0.1f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (networkStatus.contains("Offline") || networkStatus.contains("Secure"))
                            SafeSphereColors.Success.copy(alpha = 0.3f)
                        else
                            SafeSphereColors.Warning.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (networkStatus.contains("Offline") || networkStatus.contains("Secure"))
                                    SafeSphereColors.Success.copy(alpha = 0.2f)
                                else
                                    SafeSphereColors.Warning.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📡",
                            fontSize = 18.sp
                        )
                    }

                    Column {
                        Text(
                            text = "Network Status",
                            fontSize = 12.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                        Text(
                            text = networkStatus,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (networkStatus.contains("Offline") || networkStatus.contains(
                                    "Secure"
                                )
                            )
                                SafeSphereColors.Success
                            else
                                SafeSphereColors.Warning
                        )
                    }
                }

                // Status indicator
                if (networkStatus.contains("Offline") || networkStatus.contains("Secure")) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SafeSphereColors.Success.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "SECURE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.Success
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SafeSphereColors.Warning.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "MONITORING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.Warning
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enhanced Stat Card with professional design
 */
@Composable
private fun EnhancedStatCard(
    icon: String,
    value: String,
    label: String,
    color: Color,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(colors = gradient))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon with background
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 22.sp
                )
            }

            // Value with animation
            val animatedValue by animateFloatAsState(
                targetValue = value.toIntOrNull()?.toFloat() ?: 0f,
                animationSpec = tween(durationMillis = 800),
                label = "value_animation"
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (value.toIntOrNull() != null) "${animatedValue.toInt()}" else value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = SafeSphereColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            // Label
            Text(
                text = label,
                fontSize = 12.sp,
                color = SafeSphereColors.TextSecondary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Quick Actions Panel
 */
@Composable
private fun QuickActionsPanel(viewModel: SafeSphereViewModel, isMonitoring: Boolean) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚡ Quick Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = "🎯",
                    label = "Simulate Attack",
                    onClick = { viewModel.simulateThreat() },
                    modifier = Modifier.weight(1f)
                )

                QuickActionButton(
                    icon = "🧹",
                    label = "Clear Threats",
                    onClick = { /* Clear threats */ },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SafeSphereColors.Primary.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 24.sp)
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = SafeSphereColors.Primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Empty Threats State
 */
@Composable
private fun EmptyThreatsState(isMonitoring: Boolean) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🛡️", fontSize = 64.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Threats Detected",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isMonitoring)
                    "SafeSphere is actively monitoring your device"
                else
                    "Start monitoring to detect and block threats",
                fontSize = 14.sp,
                color = SafeSphereColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class ThreatDemo(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val severity: ThreatSeverity,
    val steps: List<String>
)

@Composable
fun AttackDemosTab(onDemoSelected: (ThreatDemo) -> Unit, viewModel: SafeSphereViewModel) {
    val demoCategories = remember {
        listOf(
            DemoCategory(
                title = "Common Attacks",
                icon = "⚔️",
                color = Color(0xFFE91E63),
                demos = listOf(
                    ThreatDemo(
                        id = "phishing",
                        title = "Phishing Attack",
                        description = "Learn how hackers steal credentials through fake websites and emails",
                        icon = "🎣",
                        severity = ThreatSeverity.HIGH,
                        steps = listOf(
                            "Attacker creates fake login page",
                            "Victim receives convincing email",
                            "User enters credentials",
                            "Attacker captures sensitive data",
                            "Defense: Always verify URLs and enable 2FA"
                        )
                    ),
                    ThreatDemo(
                        id = "malware",
                        title = "Malware Installation",
                        description = "See how malicious software infiltrates devices",
                        icon = "🦠",
                        severity = ThreatSeverity.CRITICAL,
                        steps = listOf(
                            "User downloads infected file",
                            "Malware executes on device",
                            "Data exfiltration begins",
                            "System access compromised",
                            "Defense: Use antivirus and scan downloads"
                        )
                    ),
                    ThreatDemo(
                        id = "mitm",
                        title = "Man-in-the-Middle",
                        description = "Understand how attackers intercept communications",
                        icon = "📡",
                        severity = ThreatSeverity.HIGH,
                        steps = listOf(
                            "Attacker positions between devices",
                            "Traffic is intercepted",
                            "Sensitive data captured",
                            "Credentials stolen",
                            "Defense: Use HTTPS and VPNs"
                        )
                    )
                )
            ),
            DemoCategory(
                title = "Advanced Threats",
                icon = "💀",
                color = Color(0xFFFF5722),
                demos = listOf(
                    ThreatDemo(
                        id = "ransomware",
                        title = "Ransomware Attack",
                        description = "Learn about data encryption extortion",
                        icon = "🔐",
                        severity = ThreatSeverity.CRITICAL,
                        steps = listOf(
                            "Ransomware infects system",
                            "Files are encrypted",
                            "Ransom demand displayed",
                            "Payment requested in crypto",
                            "Defense: Regular backups and updates"
                        )
                    ),
                    ThreatDemo(
                        id = "sql_injection",
                        title = "SQL Injection",
                        description = "Database exploitation technique",
                        icon = "💉",
                        severity = ThreatSeverity.HIGH,
                        steps = listOf(
                            "Attacker finds vulnerable input",
                            "Malicious SQL code injected",
                            "Database accessed illegally",
                            "Data extracted or modified",
                            "Defense: Input validation and prepared statements"
                        )
                    ),
                    ThreatDemo(
                        id = "ddos",
                        title = "DDoS Attack",
                        description = "Service disruption through traffic flooding",
                        icon = "🌊",
                        severity = ThreatSeverity.MEDIUM,
                        steps = listOf(
                            "Botnet activated",
                            "Massive traffic flood sent",
                            "Server overwhelmed",
                            "Service becomes unavailable",
                            "Defense: Load balancing and traffic filtering"
                        )
                    )
                )
            ),
            DemoCategory(
                title = "Social Engineering",
                icon = "🎭",
                color = Color(0xFF9C27B0),
                demos = listOf(
                    ThreatDemo(
                        id = "pretexting",
                        title = "Pretexting",
                        description = "Creating fake scenarios to extract information",
                        icon = "🎬",
                        severity = ThreatSeverity.MEDIUM,
                        steps = listOf(
                            "Attacker researches target",
                            "Convincing story created",
                            "Trust established",
                            "Sensitive info requested",
                            "Defense: Verify identities always"
                        )
                    ),
                    ThreatDemo(
                        id = "baiting",
                        title = "Baiting Attack",
                        description = "Luring victims with enticing offers",
                        icon = "🎁",
                        severity = ThreatSeverity.MEDIUM,
                        steps = listOf(
                            "Malicious USB left in public",
                            "Victim plugs in device",
                            "Malware auto-executes",
                            "System compromised",
                            "Defense: Never use unknown devices"
                        )
                    )
                )
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFE91E63),
                                    Color(0xFFFF5722)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎯", fontSize = 40.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Attack Demonstrations",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                Text(
                    text = "Interactive security education",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }
        }

        // Demo categories
        demoCategories.forEach { category ->
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(category.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = category.icon, fontSize = 20.sp)
                        }

                        Column {
                            Text(
                                text = category.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeSphereColors.TextPrimary
                            )
                            Text(
                                text = "${category.demos.size} demonstrations",
                                fontSize = 12.sp,
                                color = SafeSphereColors.TextSecondary
                            )
                        }
                    }

                    // Demos in category
                    category.demos.forEach { demo ->
                        DemoCard(
                            demo = demo,
                            categoryColor = category.color,
                            onClick = { onDemoSelected(demo) }
                        )
                    }
                }
            }
        }

        // Warning footer
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = SafeSphereColors.Warning.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SafeSphereColors.Warning.copy(alpha = 0.1f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⚠️", fontSize = 32.sp)

                    Column {
                        Text(
                            text = "Educational Purpose Only",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.Warning
                        )
                        Text(
                            text = "These demonstrations are for learning. Never use these techniques for malicious purposes.",
                            fontSize = 12.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

data class DemoCategory(
    val title: String,
    val icon: String,
    val color: Color,
    val demos: List<ThreatDemo>
)

@Composable
private fun DemoCard(
    demo: ThreatDemo,
    categoryColor: Color,
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
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            categoryColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                categoryColor.copy(alpha = 0.25f),
                                categoryColor.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = categoryColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = demo.icon, fontSize = 28.sp)
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = demo.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )

                    // Severity badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (demo.severity) {
                                    ThreatSeverity.LOW -> SafeSphereColors.Success.copy(alpha = 0.2f)
                                    ThreatSeverity.MEDIUM -> SafeSphereColors.Warning.copy(alpha = 0.2f)
                                    ThreatSeverity.HIGH -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                    ThreatSeverity.CRITICAL -> SafeSphereColors.Error.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = demo.severity.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (demo.severity) {
                                ThreatSeverity.LOW -> SafeSphereColors.Success
                                ThreatSeverity.MEDIUM -> SafeSphereColors.Warning
                                ThreatSeverity.HIGH -> Color(0xFFFF9800)
                                ThreatSeverity.CRITICAL -> SafeSphereColors.Error
                            }
                        )
                    }
                }

                Text(
                    text = demo.description,
                    fontSize = 13.sp,
                    color = SafeSphereColors.TextSecondary,
                    lineHeight = 18.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📚",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${demo.steps.size} steps",
                        fontSize = 12.sp,
                        color = categoryColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Arrow indicator
            Text(
                text = "→",
                fontSize = 24.sp,
                color = categoryColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * SECURITY TIPS TAB - Professional Security Best Practices
 * Curated tips to improve your digital security
 */
@Composable
fun SecurityTipsTab() {
    val tipCategories = remember {
        listOf(
            TipCategory(
                title = "Password Security",
                icon = "🔑",
                color = Color(0xFF00BCD4),
                tips = listOf(
                    SecurityTip(
                        title = "Use Strong Passwords",
                        description = "Create passwords with at least 12 characters, mixing uppercase, lowercase, numbers, and symbols.",
                        importance = TipImportance.CRITICAL,
                        icon = "💪"
                    ),
                    SecurityTip(
                        title = "Enable Two-Factor Authentication",
                        description = "Add an extra layer of security with 2FA. Even if someone steals your password, they can't access your account.",
                        importance = TipImportance.CRITICAL,
                        icon = "🔐"
                    ),
                    SecurityTip(
                        title = "Don't Reuse Passwords",
                        description = "Use unique passwords for each account. If one gets compromised, others remain safe.",
                        importance = TipImportance.HIGH,
                        icon = "♻️"
                    ),
                    SecurityTip(
                        title = "Use a Password Manager",
                        description = "Let SafeSphere or similar tools generate and store complex passwords securely.",
                        importance = TipImportance.HIGH,
                        icon = "🗝️"
                    )
                )
            ),
            TipCategory(
                title = "Online Safety",
                icon = "🌐",
                color = Color(0xFF4CAF50),
                tips = listOf(
                    SecurityTip(
                        title = "Verify Website URLs",
                        description = "Always check for HTTPS and verify the exact domain before entering sensitive information.",
                        importance = TipImportance.CRITICAL,
                        icon = "🔍"
                    ),
                    SecurityTip(
                        title = "Beware of Phishing",
                        description = "Never click suspicious links in emails. Verify sender authenticity before responding.",
                        importance = TipImportance.CRITICAL,
                        icon = "🎣"
                    ),
                    SecurityTip(
                        title = "Use VPN on Public WiFi",
                        description = "Encrypt your connection when using public networks to prevent data interception.",
                        importance = TipImportance.HIGH,
                        icon = "🛡️"
                    ),
                    SecurityTip(
                        title = "Clear Browsing Data",
                        description = "Regularly clear cookies and cache to remove tracking data.",
                        importance = TipImportance.MEDIUM,
                        icon = "🧹"
                    )
                )
            ),
            TipCategory(
                title = "Device Security",
                icon = "📱",
                color = Color(0xFFFF9800),
                tips = listOf(
                    SecurityTip(
                        title = "Keep Software Updated",
                        description = "Install updates immediately. They often contain critical security patches.",
                        importance = TipImportance.CRITICAL,
                        icon = "🔄"
                    ),
                    SecurityTip(
                        title = "Enable Device Encryption",
                        description = "Encrypt your device to protect data if it's lost or stolen.",
                        importance = TipImportance.HIGH,
                        icon = "🔐"
                    ),
                    SecurityTip(
                        title = "Use Biometric Locks",
                        description = "Enable fingerprint or face recognition for quick, secure access.",
                        importance = TipImportance.HIGH,
                        icon = "👆"
                    ),
                    SecurityTip(
                        title = "Backup Regularly",
                        description = "Keep encrypted backups to protect against ransomware and data loss.",
                        importance = TipImportance.MEDIUM,
                        icon = "💾"
                    )
                )
            ),
            TipCategory(
                title = "Data Privacy",
                icon = "🔒",
                color = Color(0xFF9C27B0),
                tips = listOf(
                    SecurityTip(
                        title = "Review App Permissions",
                        description = "Only grant necessary permissions. Apps don't need access to everything.",
                        importance = TipImportance.HIGH,
                        icon = "⚙️"
                    ),
                    SecurityTip(
                        title = "Disable Tracking",
                        description = "Turn off ad tracking and location services when not needed.",
                        importance = TipImportance.MEDIUM,
                        icon = "📍"
                    ),
                    SecurityTip(
                        title = "Use Private Browsers",
                        description = "Browse in incognito/private mode to reduce tracking.",
                        importance = TipImportance.MEDIUM,
                        icon = "🕵️"
                    ),
                    SecurityTip(
                        title = "Read Privacy Policies",
                        description = "Understand how your data is collected and used before agreeing.",
                        importance = TipImportance.MEDIUM,
                        icon = "📄"
                    )
                )
            ),
            TipCategory(
                title = "Social Media",
                icon = "📱",
                color = Color(0xFFE91E63),
                tips = listOf(
                    SecurityTip(
                        title = "Check Privacy Settings",
                        description = "Regularly review who can see your posts and personal information.",
                        importance = TipImportance.HIGH,
                        icon = "👁️"
                    ),
                    SecurityTip(
                        title = "Limit Personal Information",
                        description = "Don't share sensitive details like phone numbers, addresses, or travel plans publicly.",
                        importance = TipImportance.HIGH,
                        icon = "🚫"
                    ),
                    SecurityTip(
                        title = "Verify Friend Requests",
                        description = "Be cautious of fake profiles. Verify identity before accepting.",
                        importance = TipImportance.MEDIUM,
                        icon = "✅"
                    ),
                    SecurityTip(
                        title = "Avoid Oversharing",
                        description = "Think before posting. Once online, content can be saved forever.",
                        importance = TipImportance.MEDIUM,
                        icon = "🤐"
                    )
                )
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFBC02D),
                                    Color(0xFFFF9800)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💡", fontSize = 40.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Security Best Practices",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                Text(
                    text = "Essential tips for digital safety",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }
        }

        // Stats overview
        item {
            val totalTips = tipCategories.sumOf { it.tips.size }
            val criticalTips =
                tipCategories.flatMap { it.tips }.count { it.importance == TipImportance.CRITICAL }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TipStatCard(
                    icon = "📚",
                    value = "$totalTips",
                    label = "Total Tips",
                    color = SafeSphereColors.Primary,
                    modifier = Modifier.weight(1f)
                )
                TipStatCard(
                    icon = "🚨",
                    value = "$criticalTips",
                    label = "Critical",
                    color = SafeSphereColors.Error,
                    modifier = Modifier.weight(1f)
                )
                TipStatCard(
                    icon = "📂",
                    value = "${tipCategories.size}",
                    label = "Categories",
                    color = SafeSphereColors.Success,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Tip categories
        tipCategories.forEach { category ->
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(category.color.copy(alpha = 0.1f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(category.color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = category.icon, fontSize = 22.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeSphereColors.TextPrimary
                            )
                            Text(
                                text = "${category.tips.size} tips",
                                fontSize = 12.sp,
                                color = SafeSphereColors.TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(category.color)
                        )
                    }

                    // Tips in category
                    category.tips.forEach { tip ->
                        SecurityTipCard(
                            tip = tip,
                            categoryColor = category.color
                        )
                    }
                }
            }
        }

        // Action footer
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = SafeSphereColors.Primary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SafeSphereColors.Primary.copy(alpha = 0.1f))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SafeSphereColors.Primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎯", fontSize = 28.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Test Your Knowledge",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.TextPrimary
                        )
                        Text(
                            text = "Take the security quiz to check your understanding",
                            fontSize = 13.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                    }

                    Text(
                        text = "→",
                        fontSize = 24.sp,
                        color = SafeSphereColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class TipCategory(
    val title: String,
    val icon: String,
    val color: Color,
    val tips: List<SecurityTip>
)

data class SecurityTip(
    val title: String,
    val description: String,
    val importance: TipImportance,
    val icon: String
)

enum class TipImportance(val displayName: String, val color: Color) {
    CRITICAL("Critical", Color(0xFFD32F2F)),
    HIGH("High", Color(0xFFFF9800)),
    MEDIUM("Medium", Color(0xFFFBC02D)),
    LOW("Low", Color(0xFF4CAF50))
}

@Composable
private fun TipStatCard(
    icon: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.08f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = SafeSphereColors.TextSecondary
            )
        }
    }
}

@Composable
private fun SecurityTipCard(
    tip: SecurityTip,
    categoryColor: Color
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            categoryColor.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryColor.copy(alpha = 0.15f))
                        .border(
                            width = 1.dp,
                            color = categoryColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = tip.icon, fontSize = 24.sp)
                }

                // Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tip.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.TextPrimary
                        )

                        // Importance badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(tip.importance.color.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = tip.importance.displayName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = tip.importance.color
                            )
                        }
                    }

                    Text(
                        text = tip.description,
                        fontSize = 13.sp,
                        color = SafeSphereColors.TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * SECURITY QUIZ TAB - Interactive Knowledge Testing
 * Test your security awareness with fun quizzes
 */
@Composable
fun SecurityQuizTab(viewModel: SafeSphereViewModel) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var quizCompleted by remember { mutableStateOf(false) }
    var answeredQuestions by remember { mutableStateOf(setOf<Int>()) }

    // Quiz questions
    val quizQuestions = remember {
        listOf(
            QuizQuestion(
                question = "What makes a password strong?",
                options = listOf(
                    "A) Using your name and birthday",
                    "B) At least 12 characters with mixed case, numbers, and symbols",
                    "C) A simple word from the dictionary",
                    "D) Your phone number"
                ),
                correctAnswer = 1,
                explanation = "Strong passwords use a combination of uppercase, lowercase, numbers, and special characters with at least 12 characters."
            ),
            QuizQuestion(
                question = "What is phishing?",
                options = listOf(
                    "A) A type of fishing sport",
                    "B) Sending fake emails to steal personal information",
                    "C) A computer virus",
                    "D) A firewall technique"
                ),
                correctAnswer = 1,
                explanation = "Phishing is a social engineering attack where attackers send fraudulent emails pretending to be from legitimate sources to steal sensitive information."
            ),
            QuizQuestion(
                question = "What should you do if you receive a suspicious email?",
                options = listOf(
                    "A) Click all links to investigate",
                    "B) Reply with your personal information",
                    "C) Delete it and report as spam",
                    "D) Forward it to all your contacts"
                ),
                correctAnswer = 2,
                explanation = "Never click links or provide information from suspicious emails. Delete them and mark as spam to prevent future similar emails."
            ),
            QuizQuestion(
                question = "What is two-factor authentication (2FA)?",
                options = listOf(
                    "A) Using two different passwords",
                    "B) An extra security layer requiring a second verification method",
                    "C) Logging in twice",
                    "D) Having two email accounts"
                ),
                correctAnswer = 1,
                explanation = "2FA adds an extra layer of security by requiring a second form of verification (like a code sent to your phone) in addition to your password."
            ),
            QuizQuestion(
                question = "Which network is safest for sensitive transactions?",
                options = listOf(
                    "A) Public Wi-Fi at a coffee shop",
                    "B) Your secure home network with VPN",
                    "C) Free airport Wi-Fi",
                    "D) Any open network"
                ),
                correctAnswer = 1,
                explanation = "Your secure home network with a VPN is the safest option. Public Wi-Fi networks are vulnerable to man-in-the-middle attacks."
            ),
            QuizQuestion(
                question = "What is ransomware?",
                options = listOf(
                    "A) Free software",
                    "B) Malware that encrypts your files and demands payment",
                    "C) A type of antivirus",
                    "D) A backup service"
                ),
                correctAnswer = 1,
                explanation = "Ransomware is malicious software that encrypts your files and demands a ransom payment to decrypt them. Regular backups are the best defense."
            ),
            QuizQuestion(
                question = "How often should you update your passwords?",
                options = listOf(
                    "A) Never",
                    "B) Every 3-6 months or immediately if a breach occurs",
                    "C) Every 10 years",
                    "D) Only when you forget them"
                ),
                correctAnswer = 1,
                explanation = "Update passwords every 3-6 months and immediately if there's a data breach. Use unique passwords for each account."
            ),
            QuizQuestion(
                question = "What does HTTPS in a website URL indicate?",
                options = listOf(
                    "A) The website is slow",
                    "B) The connection is encrypted and secure",
                    "C) The website has ads",
                    "D) The website is free"
                ),
                correctAnswer = 1,
                explanation = "HTTPS indicates that the connection between your browser and the website is encrypted, protecting data from interception."
            ),
            QuizQuestion(
                question = "What should you do before downloading an app?",
                options = listOf(
                    "A) Check reviews, permissions, and developer reputation",
                    "B) Download from any source",
                    "C) Ignore the permissions requested",
                    "D) Share your credit card immediately"
                ),
                correctAnswer = 0,
                explanation = "Always verify the app developer, read reviews, and check what permissions the app requests before downloading."
            ),
            QuizQuestion(
                question = "What is the best way to back up important data?",
                options = listOf(
                    "A) Don't back up at all",
                    "B) Use the 3-2-1 rule: 3 copies, 2 different media, 1 offsite",
                    "C) Email it to yourself",
                    "D) Keep only one copy on your computer"
                ),
                correctAnswer = 1,
                explanation = "The 3-2-1 backup rule is best: Keep 3 copies of data on 2 different types of media with 1 copy stored offsite for disaster recovery."
            )
        )
    }

    val currentQuestion = quizQuestions.getOrNull(currentQuestionIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (quizCompleted) {
            // Quiz Completed - Show Results
            val percentage = (score.toFloat() / quizQuestions.size * 100).toInt()
            val grade = when {
                percentage >= 90 -> "🏆 Excellent!"
                percentage >= 80 -> "⭐ Great Job!"
                percentage >= 70 -> "👍 Good!"
                percentage >= 60 -> "✓ Passed"
                else -> "📚 Keep Learning"
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Trophy animation
            Text(
                text = "🎯",
                fontSize = 80.sp,
                modifier = Modifier.padding(16.dp)
            )

            Text(
                text = "Quiz Complete!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Score card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SafeSphereColors.Surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$score / ${quizQuestions.size}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            percentage >= 80 -> Color(0xFF4CAF50)
                            percentage >= 60 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )

                    Text(
                        text = "$percentage%",
                        fontSize = 24.sp,
                        color = SafeSphereColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = grade,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Retry button
            Button(
                onClick = {
                    currentQuestionIndex = 0
                    selectedAnswer = null
                    showResult = false
                    score = 0
                    quizCompleted = false
                    answeredQuestions = setOf()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafeSphereColors.Primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Try Again",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

        } else if (currentQuestion != null) {
            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question ${currentQuestionIndex + 1}/${quizQuestions.size}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextSecondary
                )
                Text(
                    text = "Score: $score",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.Primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress indicator
            LinearProgressIndicator(
                progress = (currentQuestionIndex + 1).toFloat() / quizQuestions.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = SafeSphereColors.Primary,
                trackColor = SafeSphereColors.SurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Question card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SafeSphereColors.Surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = currentQuestion.question,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Answer options
                    currentQuestion.options.forEachIndexed { index, option ->
                        val isSelected = selectedAnswer == index
                        val isCorrect = index == currentQuestion.correctAnswer
                        val showCorrectness = showResult

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable(enabled = !showResult) {
                                    selectedAnswer = index
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    showCorrectness && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    showCorrectness && isSelected && !isCorrect -> Color(0xFFF44336).copy(
                                        alpha = 0.2f
                                    )

                                    isSelected -> SafeSphereColors.Primary.copy(alpha = 0.1f)
                                    else -> SafeSphereColors.SurfaceVariant
                                }
                            ),
                            border = BorderStroke(
                                width = 2.dp,
                                color = when {
                                    showCorrectness && isCorrect -> Color(0xFF4CAF50)
                                    showCorrectness && isSelected && !isCorrect -> Color(0xFFF44336)
                                    isSelected -> SafeSphereColors.Primary
                                    else -> Color.Transparent
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 16.sp,
                                    color = SafeSphereColors.TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )

                                if (showCorrectness) {
                                    if (isCorrect) {
                                        Text(
                                            text = "✓",
                                            fontSize = 24.sp,
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else if (isSelected) {
                                        Text(
                                            text = "✗",
                                            fontSize = 24.sp,
                                            color = Color(0xFFF44336),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Explanation (shown after answering)
                    if (showResult) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "💡 ",
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = currentQuestion.explanation,
                                    fontSize = 14.sp,
                                    color = SafeSphereColors.TextSecondary,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action button
            Button(
                onClick = {
                    if (!showResult) {
                        // Show result
                        if (selectedAnswer == currentQuestion.correctAnswer) {
                            score++
                        }
                        showResult = true
                        answeredQuestions = answeredQuestions + currentQuestionIndex
                    } else {
                        // Next question or complete
                        if (currentQuestionIndex < quizQuestions.size - 1) {
                            currentQuestionIndex++
                            selectedAnswer = null
                            showResult = false
                        } else {
                            quizCompleted = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 32.dp),
                enabled = selectedAnswer != null,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafeSphereColors.Primary,
                    disabledContainerColor = SafeSphereColors.SurfaceVariant
                )
            ) {
                Text(
                    text = if (!showResult) "Submit Answer" else if (currentQuestionIndex < quizQuestions.size - 1) "Next Question" else "View Results",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String,
    val category: String = "General"
)

@Composable
private fun QuizHeader(
    currentQuestion: Int,
    totalQuestions: Int,
    progress: Float,
    score: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SafeSphereColors.Surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Security Quiz 🧠",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
                Text(
                    text = "Question $currentQuestion of $totalQuestions",
                    fontSize = 13.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(SafeSphereColors.Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.Primary
                    )
                    Text(
                        text = "score",
                        fontSize = 9.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SafeSphereColors.SurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                SafeSphereColors.Primary,
                                SafeSphereColors.Secondary
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun QuestionCard(
    question: QuizQuestion,
    questionNumber: Int,
    selectedAnswer: Int?,
    showResult: Boolean,
    onAnswerSelected: (Int) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Category badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SafeSphereColors.Primary.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = question.category,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.Primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Question text
            Text(
                text = question.question,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Answer options
            question.options.forEachIndexed { index, option ->
                AnswerOption(
                    option = option,
                    index = index,
                    isSelected = selectedAnswer == index,
                    isCorrect = index == question.correctAnswer,
                    showResult = showResult,
                    onClick = { onAnswerSelected(index) }
                )
                if (index < question.options.size - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun AnswerOption(
    option: String,
    index: Int,
    isSelected: Boolean,
    isCorrect: Boolean,
    showResult: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        showResult && isCorrect -> SafeSphereColors.Success.copy(alpha = 0.15f)
        showResult && isSelected && !isCorrect -> SafeSphereColors.Error.copy(alpha = 0.15f)
        isSelected -> SafeSphereColors.Primary.copy(alpha = 0.15f)
        else -> SafeSphereColors.SurfaceVariant.copy(alpha = 0.5f)
    }

    val borderColor = when {
        showResult && isCorrect -> SafeSphereColors.Success
        showResult && isSelected && !isCorrect -> SafeSphereColors.Error
        isSelected -> SafeSphereColors.Primary
        else -> SafeSphereColors.SurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !showResult, onClick = onClick)
            .padding(16.dp)
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
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(borderColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ('A' + index).toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = borderColor
                    )
                }

                Text(
                    text = option,
                    fontSize = 15.sp,
                    color = SafeSphereColors.TextPrimary,
                    lineHeight = 20.sp
                )
            }

            if (showResult) {
                Text(
                    text = if (isCorrect) "✓" else if (isSelected) "✗" else "",
                    fontSize = 24.sp,
                    color = if (isCorrect) SafeSphereColors.Success else SafeSphereColors.Error
                )
            }
        }
    }
}

@Composable
private fun ExplanationCard(
    isCorrect: Boolean,
    explanation: String
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (isCorrect) SafeSphereColors.Success else SafeSphereColors.Warning,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isCorrect) SafeSphereColors.Success.copy(alpha = 0.1f)
                    else SafeSphereColors.Warning.copy(alpha = 0.1f)
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isCorrect) "✅" else "💡",
                fontSize = 32.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isCorrect) "Correct!" else "Not Quite",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) SafeSphereColors.Success else SafeSphereColors.Warning
                )
                Text(
                    text = explanation,
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextPrimary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun QuizResultsScreen(
    score: Int,
    totalQuestions: Int,
    onRetake: () -> Unit
) {
    val percentage = (score.toFloat() / totalQuestions * 100).toInt()
    val grade = when {
        percentage >= 90 -> Grade("Expert", "🏆", SafeSphereColors.Success)
        percentage >= 70 -> Grade("Advanced", "🎯", SafeSphereColors.Primary)
        percentage >= 50 -> Grade("Intermediate", "📚", SafeSphereColors.Warning)
        else -> Grade("Beginner", "📖", SafeSphereColors.Error)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(modifier = Modifier.height(20.dp)) }

        // Celebration Icon
        item {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                grade.color.copy(alpha = 0.3f),
                                grade.color.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = grade.icon, fontSize = 64.sp)
            }
        }

        // Results Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = grade.color.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(grade.color.copy(alpha = 0.08f))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Quiz Complete!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your Score",
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "$score / $totalQuestions",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = grade.color
                    )

                    Text(
                        text = "$percentage%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SafeSphereColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(grade.color.copy(alpha = 0.2f))
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "${grade.level} Level",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = grade.color
                        )
                    }
                }
            }
        }

        // Performance breakdown
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Performance Breakdown",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ResultStatRow("Correct Answers", "$score", SafeSphereColors.Success)
                    Spacer(modifier = Modifier.height(10.dp))
                    ResultStatRow(
                        "Incorrect Answers",
                        "${totalQuestions - score}",
                        SafeSphereColors.Error
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ResultStatRow("Accuracy", "$percentage%", SafeSphereColors.Primary)
                }
            }
        }

        // Action buttons
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassButton(
                    text = "Retake Quiz",
                    onClick = onRetake,
                    primary = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "💡 Review Security Tips to improve your score",
                    fontSize = 13.sp,
                    color = SafeSphereColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

data class Grade(
    val level: String,
    val icon: String,
    val color: Color
)

@Composable
private fun ResultStatRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = SafeSphereColors.TextSecondary
        )

        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun InteractiveDemo(demo: ThreatDemo, onDismiss: () -> Unit, viewModel: SafeSphereViewModel) {
    Text(
        text = "This is the interactive demo for ${demo.title}",
        fontSize = 16.sp,
        color = Color(0xFF333333)
    )
}

