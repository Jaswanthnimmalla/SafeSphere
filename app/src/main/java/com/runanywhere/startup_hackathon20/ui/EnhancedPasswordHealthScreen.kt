package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.utils.PasswordAnalyzer
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen
import kotlin.math.min

/**
 * Enhanced Password Health Analyzer Screen - Pro Level
 * Advanced security analytics with beautiful visualizations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedPasswordHealthScreen(
    viewModel: SafeSphereViewModel,
) {
    val vaultItems by viewModel.vaultItems.collectAsState()

    var healthReport by remember { mutableStateOf<PasswordAnalyzer.HealthReport?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var selectedView by remember { mutableStateOf("overview") } // overview, issues, timeline

    // Analyze passwords when screen loads with PROPER decryption
    LaunchedEffect(vaultItems) {
        isAnalyzing = true
        try {
            healthReport = PasswordAnalyzer.analyzePasswords(vaultItems) { encryptedContent ->
                try {
                    com.runanywhere.startup_hackathon20.security.SecurityManager.decrypt(
                        encryptedContent
                    )
                } catch (e: Exception) {
                    encryptedContent
                }
            }
        } finally {
            isAnalyzing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SafeSphereColors.Background
                )
            )
        }
    ) { padding ->
        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(SafeSphereColors.Background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = SafeSphereColors.Primary,
                        strokeWidth = 6.dp
                    )
                    Text(
                        "🔍 Analyzing Password Security...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )
                    Text(
                        "Checking breaches, strength, and patterns",
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }
            }
        } else if (healthReport == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(SafeSphereColors.Background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("🔐", fontSize = 64.sp)
                    Text(
                        "No Password Data",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )
                    Text(
                        "Add passwords to your vault to analyze",
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }
            }
        } else {
            EnhancedPasswordHealthContent(
                report = healthReport!!,
                selectedView = selectedView,
                onViewChange = { selectedView = it },
                onFixPassword = { passwordDetail ->
                    val newPassword = PasswordAnalyzer.generateStrongPassword()
                    viewModel.updateVaultItem(
                        id = passwordDetail.itemId,
                        title = passwordDetail.title,
                        content = newPassword,
                        category = com.runanywhere.startup_hackathon20.data.VaultCategory.PASSWORDS
                    )
                },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

/**
 * Enhanced Password Health Content with Advanced Analytics
 */
@Composable
private fun EnhancedPasswordHealthContent(
    report: PasswordAnalyzer.HealthReport,
    selectedView: String,
    onViewChange: (String) -> Unit,
    onFixPassword: (PasswordAnalyzer.PasswordDetail) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SafeSphereColors.Background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Spacer
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Circular Health Score
        item {
            CircularHealthScore(score = report.overallScore)
        }

        // 4-Card Analytics Grid
        item {
            SecurityMetricsGrid(report = report)
        }

        // Strength Distribution Chart
        item {
            StrengthDistributionCard(report = report)
        }

        // Issues Alerts (if any)
        val breachedCount = report.passwordDetails.count { it.breachResult?.isBreached == true }
        if (report.weakPasswords + report.duplicatePasswords + report.commonPasswords + report.oldPasswords + breachedCount > 0) {
            item {
                SecurityAlertsCard(report = report, breachedCount = breachedCount)
            }
        }

        // View Toggle Tabs
        item {
            ViewToggleTabs(selectedView = selectedView, onViewChange = onViewChange)
        }

        // Password Details Section
        item {
            Text(
                text = when (selectedView) {
                    "issues" -> "🚨 Passwords Requiring Attention"
                    "timeline" -> "📅 Password Age Timeline"
                    else -> "🔐 All Passwords (${report.totalPasswords})"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filter passwords based on view
        val filteredPasswords = when (selectedView) {
            "issues" -> report.passwordDetails.filter {
                it.issues.isNotEmpty() || it.breachResult?.isBreached == true
            }

            "timeline" -> report.passwordDetails.sortedByDescending { it.ageInDays }
            else -> report.passwordDetails
        }

        // Individual Password Cards
        items(filteredPasswords) { passwordDetail ->
            EnhancedPasswordCard(
                detail = passwordDetail,
                showTimeline = selectedView == "timeline",
                onFix = { onFixPassword(passwordDetail) }
            )
        }

        // Bottom Spacer
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

/**
 * Circular Health Score with animated progress
 */
@Composable
private fun CircularHealthScore(score: Int) {
    val (color, label, emoji) = when {
        score >= 85 -> Triple(Color(0xFF4CAF50), "Excellent", "🎉")
        score >= 70 -> Triple(Color(0xFF8BC34A), "Good", "✅")
        score >= 50 -> Triple(Color(0xFFFFC107), "Fair", "⚠️")
        score >= 30 -> Triple(Color(0xFFFF9800), "Poor", "🔴")
        else -> Triple(Color(0xFFF44336), "Critical", "🚨")
    }

    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutCubic)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(
                width = 3.dp,
                color = color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = SafeSphereColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.15f),
                            SafeSphereColors.Surface
                        )
                    )
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Circular Progress Indicator
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background circle
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.fillMaxSize(),
                    color = SafeSphereColors.SurfaceVariant,
                    strokeWidth = 16.dp
                )

                // Animated progress circle
                CircularProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.fillMaxSize(),
                    color = color,
                    strokeWidth = 16.dp
                )

                // Center content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 40.sp
                    )
                    Text(
                        text = "$score",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }
            }
        }
    }
}

/**
 * Security Metrics Grid - 4 Cards
 */
@Composable
private fun SecurityMetricsGrid(report: PasswordAnalyzer.HealthReport) {
    val breachedCount = report.passwordDetails.count { it.breachResult?.isBreached == true }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            icon = "🔐",
            value = "${report.totalPasswords}",
            label = "Total",
            color = SafeSphereColors.Primary,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            icon = "💪",
            value = "${report.totalPasswords - report.weakPasswords}",
            label = "Strong",
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            icon = "⚠️",
            value = "${report.weakPasswords}",
            label = "Weak",
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            icon = "🚨",
            value = "$breachedCount",
            label = "Leaked",
            color = Color(0xFFD32F2F),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Metric Card Component
 */
@Composable
private fun MetricCard(
    icon: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = 2.dp,
                color = color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = SafeSphereColors.SurfaceVariant
        ),
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
            Text(
                text = icon,
                fontSize = 32.sp
            )
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SafeSphereColors.TextSecondary
            )
        }
    }
}

/**
 * Strength Distribution Visualization
 */
@Composable
private fun StrengthDistributionCard(report: PasswordAnalyzer.HealthReport) {
    val strongCount = report.passwordDetails.count { it.strength.score >= 75 }
    val goodCount = report.passwordDetails.count { it.strength.score in 50..74 }
    val weakCount = report.passwordDetails.count { it.strength.score < 50 }
    val total = report.totalPasswords.toFloat().coerceAtLeast(1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SafeSphereColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📊 Strength Distribution",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Stacked bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SafeSphereColors.SurfaceVariant)
            ) {
                if (strongCount > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(strongCount / total)
                            .background(Color(0xFF4CAF50))
                    )
                }
                if (goodCount > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(goodCount / total)
                            .background(Color(0xFFFBC02D))
                    )
                }
                if (weakCount > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(weakCount / total)
                            .background(Color(0xFFFF5722))
                    )
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("💪", "Strong", "$strongCount", Color(0xFF4CAF50))
                LegendItem("👍", "Good", "$goodCount", Color(0xFFFFC107))
                LegendItem("⚠️", "Weak", "$weakCount", Color(0xFFFF5722))
            }
        }
    }
}

/**
 * Legend Item for Distribution Chart
 */
@Composable
private fun LegendItem(icon: String, label: String, count: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SafeSphereColors.TextSecondary
            )
        }
        Text(
            text = count,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Security Alerts Card
 */
@Composable
private fun SecurityAlertsCard(report: PasswordAnalyzer.HealthReport, breachedCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SafeSphereColors.Surface
        ),
        border = BorderStroke(2.dp, Color(0xFFF44336).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🚨 Security Alerts",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (breachedCount > 0) {
                AlertRow(
                    icon = "🚨",
                    text = "$breachedCount Leaked Password${if (breachedCount > 1) "s" else ""} - Change Immediately!",
                    color = Color(0xFFD32F2F)
                )
            }

            if (report.weakPasswords > 0) {
                AlertRow(
                    icon = "⚠️",
                    text = "${report.weakPasswords} Weak Password${if (report.weakPasswords > 1) "s" else ""}",
                    color = Color(0xFFFF9800)
                )
            }

            if (report.duplicatePasswords > 0) {
                AlertRow(
                    icon = "🔄",
                    text = "${report.duplicatePasswords} Duplicate Password${if (report.duplicatePasswords > 1) "s" else ""}",
                    color = Color(0xFFFF5722)
                )
            }

            if (report.commonPasswords > 0) {
                AlertRow(
                    icon = "📋",
                    text = "${report.commonPasswords} Common Password${if (report.commonPasswords > 1) "s" else ""}",
                    color = Color(0xFFF57C00)
                )
            }

            if (report.oldPasswords > 0) {
                AlertRow(
                    icon = "⏰",
                    text = "${report.oldPasswords} Old Password${if (report.oldPasswords > 1) "s" else ""} (>1 year)",
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

/**
 * Alert Row Component
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
        Text(
            text = icon,
            fontSize = 24.sp
        )
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

/**
 * View Toggle Tabs with Dark Theme
 */
@Composable
private fun ViewToggleTabs(selectedView: String, onViewChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SafeSphereColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton(
                text = "📊 All",
                isSelected = selectedView == "overview",
                onClick = { onViewChange("overview") },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "🚨 Issues",
                isSelected = selectedView == "issues",
                onClick = { onViewChange("issues") },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "📅 Age",
                isSelected = selectedView == "timeline",
                onClick = { onViewChange("timeline") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Tab Button with Dark Theme
 */
@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) SafeSphereColors.Primary else SafeSphereColors.SurfaceVariant,
            contentColor = if (isSelected) Color.White else SafeSphereColors.TextSecondary
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/**
 * Enhanced Password Card with Timeline View Option
 */
@Composable
private fun EnhancedPasswordCard(
    detail: PasswordAnalyzer.PasswordDetail,
    showTimeline: Boolean,
    onFix: () -> Unit
) {
    val strengthColor = Color(detail.strength.color)
    val hasIssues = detail.issues.isNotEmpty()
    val isBreached = detail.breachResult?.isBreached == true

    val backgroundColor = SafeSphereColors.Surface

    val borderColor = when {
        isBreached -> Color(0xFFF44336)
        detail.strength.score >= 75 -> Color(0xFF4CAF50)
        detail.strength.score >= 50 -> Color(0xFFFFC107)
        else -> Color(0xFFFF9800)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isBreached) 3.dp else 2.dp,
                color = borderColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isBreached) 8.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = detail.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )

                    // Breach Warning
                    detail.breachResult?.let { breach ->
                        if (breach.isBreached) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFFD32F2F),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "🚨 LEAKED: ${breach.message}",
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Strength Badge
                Surface(
                    color = strengthColor,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when {
                                isBreached -> "🚨"
                                detail.strength.score >= 75 -> "💪"
                                detail.strength.score >= 50 -> "👍"
                                else -> "⚠️"
                            },
                            fontSize = 18.sp
                        )
                        Text(
                            text = detail.strength.label,
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Strength Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Strength",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SafeSphereColors.TextSecondary
                    )
                    Text(
                        text = "${detail.strength.score}/100",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = strengthColor
                    )
                }
                LinearProgressIndicator(
                    progress = detail.strength.score / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = strengthColor,
                    trackColor = SafeSphereColors.SurfaceVariant
                )
            }

            // Timeline View - Age Display
            if (showTimeline && detail.ageInDays > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SafeSphereColors.SurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "📅", fontSize = 24.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Password Age",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SafeSphereColors.TextSecondary
                            )
                            Text(
                                text = "${detail.ageInDays} days old",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (detail.ageInDays > 365) Color(0xFFFF9800) else SafeSphereColors.TextPrimary
                            )
                        }
                    }
                }
            }

            // Issues
            if (detail.issues.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SafeSphereColors.SurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚠️ Issues Found",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                        detail.issues.forEach { issue ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "•",
                                    color = Color(0xFFF44336),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = issue,
                                    fontSize = 13.sp,
                                    color = Color(0xFFF44336),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Action Button
            if (hasIssues || isBreached) {
                Button(
                    onClick = onFix,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBreached) Color(0xFFF44336) else Color(0xFFFF9800)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBreached) "🚨 Change Now!" else "🔄 Generate Strong Password",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✅", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Strong & Secure",
                            fontSize = 14.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

