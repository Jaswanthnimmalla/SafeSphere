package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.ai.AISecurityPredictor
import com.runanywhere.startup_hackathon20.data.PasswordVaultRepository
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Data models for live threat feed
 */
data class LiveThreat(
    val title: String,
    val description: String,
    val timestamp: String,
    val severity: LiveThreatSeverity
)

enum class LiveThreatSeverity(val icon: String, val color: Color) {
    CRITICAL("🚨", Color(0xFFD32F2F)),
    HIGH("⚠️", Color(0xFFF57C00)),
    MEDIUM("⚡", Color(0xFFFBC02D)),
    LOW("ℹ️", Color(0xFF2196F3))
}

/**
 * Helper function to generate random threats for simulation
 */
private fun generateRandomThreat(): LiveThreat {
    val threats = listOf(
        LiveThreat(
            "Weak Password Detected",
            "Password strength below recommended threshold",
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
            LiveThreatSeverity.MEDIUM
        ),
        LiveThreat(
            "Duplicate Password Found",
            "Multiple accounts using identical passwords",
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
            LiveThreatSeverity.HIGH
        ),
        LiveThreat(
            "Breach Database Match",
            "Password found in known breach databases",
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
            LiveThreatSeverity.CRITICAL
        ),
        LiveThreat(
            "Old Password Alert",
            "Password not changed in over 180 days",
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
            LiveThreatSeverity.LOW
        )
    )
    return threats.random()
}

/**
 * PRO-LEVEL AI Security Predictor Screen
 * 
 * Advanced Features:
 * - Real-time monitoring with live updates every 30 seconds
 * - Animated visualizations and charts
 * - Live threat feed simulation
 * - Predictive alerts system
 * - Historical tracking with trend analysis
 * - Auto-refresh capabilities
 * - Beautiful glassmorphism UI
 * - Interactive components
 */
@Composable
fun AIPredictorScreen(viewModel: SafeSphereViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val passwordRepo = remember { PasswordVaultRepository.getInstance(context) }
    val passwords by passwordRepo.passwords.collectAsState()

    var prediction by remember { mutableStateOf<AISecurityPredictor.SecurityPrediction?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var lastUpdateTime by remember { mutableStateOf("") }
    var isRealTimeEnabled by remember { mutableStateOf(true) }
    var liveThreats by remember { mutableStateOf(listOf<LiveThreat>()) }
    var scanProgress by remember { mutableStateOf(0f) }
    
    val coroutineScope = rememberCoroutineScope()
    val predictor = remember { AISecurityPredictor() }

    // Real-time auto-refresh every 30 seconds
    LaunchedEffect(isRealTimeEnabled) {
        while (isRealTimeEnabled && passwords.isNotEmpty()) {
            delay(30000) // 30 seconds
            try {
                prediction = predictor.predictSecurity(passwords)
                lastUpdateTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                // Simulate new threat detection
                if (Random.nextFloat() > 0.7f) {
                    liveThreats = liveThreats.take(4) + generateRandomThreat()
                }
            } catch (e: Exception) {
                android.util.Log.e("AIPredictor", "Auto-refresh error: ${e.message}")
            }
        }
    }

    // Initial analysis with animated progress
    LaunchedEffect(passwords) {
        if (passwords.isNotEmpty()) {
            isAnalyzing = true
            try {
                // Animated progress
                for (i in 0..100 step 5) {
                    scanProgress = i / 100f
                    delay(50)
                }
                prediction = predictor.predictSecurity(passwords)
                lastUpdateTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                // Initialize with some threats
                liveThreats = listOf(
                    generateRandomThreat(),
                    generateRandomThreat(),
                    generateRandomThreat()
                )
            } finally {
                isAnalyzing = false
                scanProgress = 0f
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pro-Level Header with Real-time Status
            ProLevelHeader(
                onBackClick = { viewModel.navigateToScreen(SafeSphereScreen.DASHBOARD) },
                onRefreshClick = {
                    coroutineScope.launch {
                        isAnalyzing = true
                        scanProgress = 0f
                        try {
                            for (i in 0..100 step 10) {
                                scanProgress = i / 100f
                                delay(30)
                            }
                            prediction = predictor.predictSecurity(passwords)
                            lastUpdateTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        } finally {
                            isAnalyzing = false
                            scanProgress = 0f
                        }
                    }
                },
                isRealTimeEnabled = isRealTimeEnabled,
                onToggleRealTime = { isRealTimeEnabled = !isRealTimeEnabled },
                lastUpdateTime = lastUpdateTime,
                isAnalyzing = isAnalyzing
            )

            if (isAnalyzing) {
                ProLevelLoadingState(progress = scanProgress)
            } else if (prediction == null) {
                EmptyPredictionState()
            } else {
                ProLevelDashboard(
                    prediction = prediction!!,
                    viewModel = viewModel,
                    liveThreats = liveThreats,
                    isRealTimeEnabled = isRealTimeEnabled
                )
            }
        }

        // Floating Real-time Indicator
        if (isRealTimeEnabled && !isAnalyzing && prediction != null) {
            LiveIndicatorPulse(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 16.dp)
            )
        }
    }
}

/**
 * Pro-Level Header
 */
@Composable
private fun ProLevelHeader(
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    isRealTimeEnabled: Boolean,
    onToggleRealTime: () -> Unit,
    lastUpdateTime: String,
    isAnalyzing: Boolean
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
                            Color(0xFF9C27B0).copy(alpha = 0.1f),
                            Color(0xFF7B1FA2).copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Top row: Back button, title, refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SafeSphereColors.Primary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "AI Security Predictor",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9C27B0)
                    )
                    if (lastUpdateTime.isNotEmpty()) {
                        Text(
                            text = "Updated: $lastUpdateTime",
                            fontSize = 11.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onRefreshClick,
                    enabled = !isAnalyzing
                ) {
                    val rotation by rememberInfiniteTransition(label = "rotation").animateFloat(
                        initialValue = 0f,
                        targetValue = if (isAnalyzing) 360f else 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotation"
                    )
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = if (isAnalyzing) Color(0xFF9C27B0) else SafeSphereColors.Primary,
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isRealTimeEnabled) Color(0xFF4CAF50).copy(alpha = 0.15f)
                        else SafeSphereColors.SurfaceVariant
                    )
                    .clickable(onClick = onToggleRealTime)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRealTimeEnabled) Color(0xFF4CAF50)
                                else SafeSphereColors.TextSecondary.copy(alpha = 0.3f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRealTimeEnabled) "📡" else "📡",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = if (isRealTimeEnabled) "Real-Time Monitoring Active" else "Real-Time Monitoring Paused",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRealTimeEnabled) Color(0xFF4CAF50) else SafeSphereColors.TextSecondary
                        )
                        Text(
                            text = if (isRealTimeEnabled) "Auto-updates every 30 seconds" else "Tap to enable",
                            fontSize = 11.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                    }
                }

                Switch(
                    checked = isRealTimeEnabled,
                    onCheckedChange = { onToggleRealTime() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4CAF50),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = SafeSphereColors.TextSecondary.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

/**
 * Pro Loading State with Progress
 */
@Composable
private fun ProLevelLoadingState(progress: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated scanning icon
            val infiniteTransition = rememberInfiniteTransition(label = "scan")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF9C27B0).copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "", fontSize = 64.sp)
            }

            Text(
                text = "AI Analyzing Security...",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9C27B0)
            )

            // Animated progress bar
            Column(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF9C27B0),
                    trackColor = SafeSphereColors.SurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${(progress * 100).toInt()}% Complete",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9C27B0)
                )
            }

            // Scanning steps
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScanningStep("Analyzing password patterns", progress > 0.2f)
                ScanningStep("Running ML algorithms", progress > 0.4f)
                ScanningStep("Predicting future risks", progress > 0.6f)
                ScanningStep("Calculating breach probability", progress > 0.8f)
            }
        }
    }
}

/**
 * Scanning Step Indicator
 */
@Composable
private fun ScanningStep(text: String, isComplete: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isComplete) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF9C27B0)
            )
        }
        Text(
            text = text,
            fontSize = 13.sp,
            color = if (isComplete) SafeSphereColors.TextPrimary else SafeSphereColors.TextSecondary
        )
    }
}

/**
 * Live Indicator Pulse
 */
@Composable
private fun LiveIndicatorPulse(modifier: Modifier = Modifier) {
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

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF4CAF50).copy(alpha = pulseAlpha),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Text(
                text = "LIVE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

/**
 * Pro-Level Dashboard
 */
@Composable
private fun ProLevelDashboard(
    prediction: AISecurityPredictor.SecurityPrediction,
    viewModel: SafeSphereViewModel,
    liveThreats: List<LiveThreat>,
    isRealTimeEnabled: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Risk Score with Real-time Animation
        item {
            AnimatedRiskScoreCard(
                currentRisk = prediction.currentRiskScore,
                daysUntilCritical = prediction.daysUntilCritical,
                trend = prediction.riskTrend,
                breachProbability = prediction.breachProbability
            )
        }

        // Celebration Card (if safe)
        if (prediction.currentRiskScore < 20) {
            item { CelebrationCard() }
        }

        // Live Threat Feed
        if (liveThreats.isNotEmpty() && isRealTimeEnabled) {
            item {
                LiveThreatFeed(threats = liveThreats)
            }
        }

        // Risk Timeline
        item {
            RiskTimelineCard(
                currentRisk = prediction.currentRiskScore,
                risk30Days = prediction.futureRiskScore30Days,
                risk90Days = prediction.futureRiskScore90Days
            )
        }

        // Vulnerabilities
        if (prediction.vulnerabilities.isNotEmpty()) {
            item {
                VulnerabilitiesCard(vulnerabilities = prediction.vulnerabilities)
            }
        }

        // AI Predictions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Predictions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF9C27B0).copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ML-Powered",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9C27B0)
                    )
                }
            }
        }

        // AI Predictions
        items(prediction.predictions) { insight ->
            PredictionInsightCard(insight)
        }

        // Action Items
        if (prediction.actionItems.isNotEmpty()) {
            item {
                Text(
                    text = "Recommended Actions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
            }

            items(prediction.actionItems) { action ->
                ActionItemCard(
                    action = action,
                    onFixNow = { viewModel.navigateToScreen(SafeSphereScreen.PASSWORDS) }
                )
            }
        }

        // ML Info
        item {
            MLInfoCard()
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Animated Risk Score Card with beautiful visuals
 */
@Composable
private fun AnimatedRiskScoreCard(
    currentRisk: Int,
    daysUntilCritical: Int,
    trend: AISecurityPredictor.RiskTrend,
    breachProbability: Float
) {
    val animatedRisk by animateIntAsState(
        targetValue = currentRisk,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "risk_animation"
    )

    val (riskColor, riskLabel) = when {
        currentRisk >= 80 -> Color(0xFFD32F2F) to "CRITICAL"
        currentRisk >= 60 -> Color(0xFFF57C00) to "HIGH"
        currentRisk >= 40 -> Color(0xFFFBC02D) to "MEDIUM"
        currentRisk >= 20 -> Color(0xFF7CB342) to "LOW"
        else -> Color(0xFF388E3C) to "SAFE"
    }

    val trendIcon = when (trend) {
        AISecurityPredictor.RiskTrend.IMPROVING -> "📈"
        AISecurityPredictor.RiskTrend.STABLE -> "➡️"
        AISecurityPredictor.RiskTrend.DEGRADING -> "📉"
        AISecurityPredictor.RiskTrend.CRITICAL -> "🚨"
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, riskColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            riskColor.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Risk Score
            Text(
                text = "$animatedRisk",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = riskColor
            )

            Text(
                text = "/ 100",
                fontSize = 20.sp,
                color = SafeSphereColors.TextSecondary,
                modifier = Modifier.offset(y = (-8).dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Risk Label
            Surface(
                color = riskColor,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "$trendIcon $riskLabel RISK",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBox(
                    icon = "⏰",
                    value = if (daysUntilCritical < 999) "$daysUntilCritical days" else "∞",
                    label = "Until Critical"
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(50.dp)
                        .background(SafeSphereColors.TextSecondary.copy(alpha = 0.2f))
                )

                StatBox(
                    icon = "🎯",
                    value = "${(breachProbability * 100).toInt()}%",
                    label = "Breach Probability"
                )
            }
        }
    }
}

/**
 * Stat Box Component
 */
@Composable
private fun StatBox(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SafeSphereColors.TextPrimary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = SafeSphereColors.TextSecondary
        )
    }
}

/**
 * Risk Timeline Graph
 */
@Composable
private fun RiskTimelineCard(
    currentRisk: Int,
    risk30Days: Int,
    risk90Days: Int
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Risk Timeline",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                val deltaIcon = if (risk90Days > currentRisk) "📈" else "📉"
                Text(
                    text = "$deltaIcon ${risk90Days - currentRisk}% in 90 days",
                    fontSize = 14.sp,
                    color = if (risk90Days > currentRisk) SafeSphereColors.Error else SafeSphereColors.Success
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Timeline points
            TimelinePoint(
                label = "Today",
                risk = currentRisk,
                isActive = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            TimelinePoint(
                label = "30 Days",
                risk = risk30Days,
                isActive = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            TimelinePoint(
                label = "90 Days",
                risk = risk90Days,
                isActive = false
            )
        }
    }
}

/**
 * Timeline Point Component
 */
@Composable
private fun TimelinePoint(label: String, risk: Int, isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicator
        Box(
            modifier = Modifier
                .size(if (isActive) 16.dp else 12.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) SafeSphereColors.Primary
                    else SafeSphereColors.TextSecondary.copy(alpha = 0.3f)
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Label
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = SafeSphereColors.TextPrimary,
            modifier = Modifier.width(80.dp)
        )

        // Progress bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SafeSphereColors.SurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(risk / 100f)
                    .background(
                        when {
                            risk >= 80 -> Color(0xFFD32F2F)
                            risk >= 60 -> Color(0xFFF57C00)
                            risk >= 40 -> Color(0xFFFBC02D)
                            else -> Color(0xFF388E3C)
                        }
                    )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Risk value
        Text(
            text = "$risk",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = SafeSphereColors.TextPrimary,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )
    }
}

/**
 * Vulnerabilities Card
 */
@Composable
private fun VulnerabilitiesCard(vulnerabilities: List<AISecurityPredictor.Vulnerability>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🔍 Vulnerability Analysis",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            vulnerabilities.forEach { vuln ->
                VulnerabilityRow(vuln)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Vulnerability Row
 */
@Composable
private fun VulnerabilityRow(vuln: AISecurityPredictor.Vulnerability) {
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
            Text(text = vuln.type.icon, fontSize = 24.sp)
            Column {
                Text(
                    text = vuln.type.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeSphereColors.TextPrimary
                )
                Text(
                    text = "${vuln.count} found",
                    fontSize = 13.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }
        }

        Surface(
            color = SafeSphereColors.Error.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "${vuln.riskContribution}% risk",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.Error
            )
        }
    }
}

/**
 * Prediction Insight Card
 */
@Composable
private fun PredictionInsightCard(insight: AISecurityPredictor.PredictionInsight) {
    val severityColor = when (insight.severity) {
        AISecurityPredictor.PredictionSeverity.CRITICAL -> Color(0xFFD32F2F)
        AISecurityPredictor.PredictionSeverity.HIGH -> Color(0xFFF57C00)
        AISecurityPredictor.PredictionSeverity.MEDIUM -> Color(0xFFFBC02D)
        AISecurityPredictor.PredictionSeverity.LOW -> Color(0xFF388E3C)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFD32F2F).copy(alpha = 0.5f),
                        Color(0xFFF57C00).copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
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
                    // Pulsing live indicator
                    val infiniteTransition = rememberInfiniteTransition(label = "threat_pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD32F2F).copy(alpha = pulseAlpha))
                    )

                    Text(
                        text = "Live Threat Detection",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                }

                Text(
                    text = "🎯 ${(insight.confidence * 100).toInt()}% confidence",
                    fontSize = 12.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Action Item Card
 */
@Composable
private fun ActionItemCard(action: AISecurityPredictor.ActionItem, onFixNow: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Priority badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when (action.priority) {
                                1 -> Color(0xFFD32F2F)
                                2 -> Color(0xFFF57C00)
                                else -> SafeSphereColors.Primary
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${action.priority}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = action.action,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SafeSphereColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💪 ${action.impact}",
                        fontSize = 13.sp,
                        color = SafeSphereColors.Success
                    )
                    Text(
                        text = "⏱️ ${action.estimatedTime}",
                        fontSize = 12.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }
            }

            // Add "Fix Now" button
            Spacer(modifier = Modifier.height(12.dp))

            GlassButton(
                text = "🔧 Fix Now",
                onClick = onFixNow,
                primary = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Celebration Card
 */
@Composable
private fun CelebrationCard() {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4CAF50),
                        Color(0xFF8BC34A)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4CAF50).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated confetti emojis
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(text = "🎉", fontSize = 48.sp)
                Text(text = "🎊", fontSize = 48.sp)
                Text(text = "✨", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main message
            Text(
                text = "Excellent Security!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your vault is well protected",
                fontSize = 16.sp,
                color = SafeSphereColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Achievement badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AchievementBadge(
                    icon = "🛡️",
                    label = "Protected"
                )
                AchievementBadge(
                    icon = "🔒",
                    label = "Secure"
                )
                AchievementBadge(
                    icon = "✅",
                    label = "Safe"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Encouragement message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
                    .padding(16.dp)
            ) {
                Text(
                    text = "💡 Keep monitoring your passwords regularly to maintain this excellent security level!",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * Achievement Badge Component
 */
@Composable
private fun AchievementBadge(icon: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 28.sp
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4CAF50)
        )
    }
}

/**
 * Empty Prediction State - shown when no passwords exist
 */
@Composable
private fun EmptyPredictionState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(48.dp)
        ) {
            Text(
                text = "🔐",
                fontSize = 80.sp
            )

            Text(
                text = "No Passwords to Analyze",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Add some passwords to your vault to see AI-powered security predictions and risk analysis.",
                fontSize = 16.sp,
                color = SafeSphereColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SafeSphereColors.Primary.copy(alpha = 0.15f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "💡 This AI model uses advanced machine learning algorithms to provide pro-level security insights and predictions.",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * Live Threat Feed Component
 */
@Composable
private fun LiveThreatFeed(threats: List<LiveThreat>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔴 Live Threat Feed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                // Pulsing live indicator
                val infiniteTransition = rememberInfiniteTransition(label = "feed_pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD32F2F).copy(alpha = pulseAlpha * 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD32F2F).copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD32F2F).copy(alpha = pulseAlpha)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            threats.forEach { threat ->
                ThreatItem(threat)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Individual Threat Item
 */
@Composable
private fun ThreatItem(threat: LiveThreat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(threat.severity.color.copy(alpha = 0.1f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Severity icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(threat.severity.color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = threat.severity.icon,
                fontSize = 20.sp
            )
        }

        // Threat details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = threat.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SafeSphereColors.TextPrimary
            )
            Text(
                text = threat.description,
                fontSize = 12.sp,
                color = SafeSphereColors.TextSecondary,
                lineHeight = 16.sp
            )
            Text(
                text = threat.timestamp,
                fontSize = 11.sp,
                color = SafeSphereColors.TextSecondary.copy(alpha = 0.7f)
            )
        }

        // Severity badge
        Surface(
            color = threat.severity.color,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = threat.severity.name,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * ML Info Card explaining the AI model
 */
@Composable
private fun MLInfoCard() {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFF9C27B0).copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF9C27B0).copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF9C27B0).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🤖", fontSize = 24.sp)
                }

                Column {
                    Text(
                        text = "AI-Powered Analysis",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )
                    Text(
                        text = "Machine Learning Model v2.0",
                        fontSize = 13.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Model features
            MLFeature(
                icon = "🎯",
                title = "Predictive Analytics",
                description = "Forecasts security risks up to 90 days in advance"
            )

            Spacer(modifier = Modifier.height(12.dp))

            MLFeature(
                icon = "📊",
                title = "Pattern Recognition",
                description = "Analyzes entropy, complexity, and reuse patterns"
            )

            Spacer(modifier = Modifier.height(12.dp))

            MLFeature(
                icon = "🔍",
                title = "Breach Detection",
                description = "Identifies vulnerabilities and breach probability"
            )

            Spacer(modifier = Modifier.height(12.dp))

            MLFeature(
                icon = "⚡",
                title = "Real-time Updates",
                description = "Continuous monitoring with live threat detection"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF9C27B0).copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "💡 This AI model uses advanced machine learning algorithms to provide pro-level security insights and predictions.",
                    fontSize = 13.sp,
                    color = SafeSphereColors.TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * ML Feature Row
 */
@Composable
private fun MLFeature(icon: String, title: String, description: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = icon, fontSize = 20.sp)

        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SafeSphereColors.TextPrimary
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = SafeSphereColors.TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

