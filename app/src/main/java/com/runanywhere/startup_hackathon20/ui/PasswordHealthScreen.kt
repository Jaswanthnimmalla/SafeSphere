package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.utils.PasswordAnalyzer
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen

/**
 * Password Health Analyzer Screen - Dark Theme
 * Shows comprehensive password security analysis with SafeSphere styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordHealthScreen(
    viewModel: SafeSphereViewModel,
    onNavigateBack: () -> Unit
) {
    val vaultItems by viewModel.vaultItems.collectAsState()

    var healthReport by remember { mutableStateOf<PasswordAnalyzer.HealthReport?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

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
                title = { Text("🔐 Password Health", color = SafeSphereColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = SafeSphereColors.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SafeSphereColors.Surface
                )
            )
        },
        containerColor = SafeSphereColors.Background
    ) { padding ->
        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(SafeSphereColors.Background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = SafeSphereColors.Primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Analyzing passwords...", color = SafeSphereColors.TextPrimary)
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
                Text("No password data available", color = SafeSphereColors.TextSecondary)
            }
        } else {
            PasswordHealthContent(
                report = healthReport!!,
                viewModel = viewModel,
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
 * Password Health Content
 */
@Composable
private fun PasswordHealthContent(
    report: PasswordAnalyzer.HealthReport,
    viewModel: SafeSphereViewModel,
    onFixPassword: (PasswordAnalyzer.PasswordDetail) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SafeSphereColors.Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overall Score Card
        item {
            HealthScoreCard(score = report.overallScore)
        }

        // Issues Summary
        if (report.weakPasswords + report.duplicatePasswords + report.commonPasswords + report.oldPasswords > 0) {
            item {
                IssuesSummaryCard(report = report)
            }
        }

        // Password Details Header
        item {
            Text(
                text = "Password Details (${report.totalPasswords} total)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )
        }

        // Individual Password Cards
        items(report.passwordDetails) { passwordDetail ->
            PasswordDetailCard(
                detail = passwordDetail,
                onFix = { onFixPassword(passwordDetail) }
            )
        }
    }
}

/**
 * Health Score Card - Dark Theme with Glass Effect
 */
@Composable
private fun HealthScoreCard(score: Int) {
    val (color, label, emoji) = when {
        score >= 85 -> Triple(SafeSphereColors.Success, "Excellent", "🎉")
        score >= 70 -> Triple(Color(0xFF7CB342), "Good", "✅")
        score >= 50 -> Triple(SafeSphereColors.Warning, "Fair", "⚠️")
        score >= 30 -> Triple(Color(0xFFF57C00), "Poor", "🔴")
        else -> Triple(SafeSphereColors.Error, "Critical", "🚨")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        SafeSphereColors.Primary.copy(alpha = 0.5f),
                        SafeSphereColors.Secondary.copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = SafeSphereColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SafeSphereColors.Primary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "$score",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = "/ 100",
                fontSize = 20.sp,
                color = SafeSphereColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = color,
                trackColor = SafeSphereColors.SurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Issues Summary Card - Dark Theme
 */
@Composable
private fun IssuesSummaryCard(report: PasswordAnalyzer.HealthReport) {
    val breachedCount = report.passwordDetails.count { it.breachResult?.isBreached == true }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = SafeSphereColors.Error.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = SafeSphereColors.Surface
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
                            SafeSphereColors.Error.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⚠️ Issues Found",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.Error
            )

            if (breachedCount > 0) {
                IssueRow(
                    icon = "🚨",
                    text = "$breachedCount Leaked Password${if (breachedCount > 1) "s" else ""} - Change Now!",
                    color = SafeSphereColors.Error
                )
            }

            if (report.weakPasswords > 0) {
                IssueRow(
                    icon = "🔴",
                    text = "${report.weakPasswords} Weak Password${if (report.weakPasswords > 1) "s" else ""}",
                    color = Color(0xFFFF9800)
                )
            }

            if (report.duplicatePasswords > 0) {
                IssueRow(
                    icon = "❌",
                    text = "${report.duplicatePasswords} Duplicate Password${if (report.duplicatePasswords > 1) "s" else ""}",
                    color = Color(0xFFFF9800)
                )
            }

            if (report.commonPasswords > 0) {
                IssueRow(
                    icon = "⚠️",
                    text = "${report.commonPasswords} Common Password${if (report.commonPasswords > 1) "s" else ""}",
                    color = SafeSphereColors.Warning
                )
            }

            if (report.oldPasswords > 0) {
                IssueRow(
                    icon = "⏰",
                    text = "${report.oldPasswords} Old Password${if (report.oldPasswords > 1) "s" else ""} (>1 year)",
                    color = SafeSphereColors.Warning
                )
            }
        }
    }
}

/**
 * Issue Row - Dark Theme
 */
@Composable
private fun IssueRow(icon: String, text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SafeSphereColors.SurfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = icon, fontSize = 24.sp)
        Text(
            text = text,
            fontSize = 14.sp,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Password Detail Card - Dark Theme with Glass Effect
 */
@Composable
private fun PasswordDetailCard(
    detail: PasswordAnalyzer.PasswordDetail,
    onFix: () -> Unit
) {
    val strengthColor = Color(detail.strength.color)
    val hasIssues = detail.issues.isNotEmpty()
    val isBreached = detail.breachResult?.isBreached == true

    // Dark theme border color
    val borderColor = when {
        isBreached -> SafeSphereColors.Error
        detail.strength.label == "Weak" || detail.strength.label == "Very Weak" -> Color(0xFFFF9800)
        detail.strength.label == "Fair" -> SafeSphereColors.Warning
        detail.strength.label == "Good" -> SafeSphereColors.Primary
        else -> SafeSphereColors.Success
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isBreached) 3.dp else 2.dp,
                color = borderColor.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = SafeSphereColors.Surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isBreached) 8.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            borderColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title and Strength Badge
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = SafeSphereColors.Error,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "🚨 LEAKED",
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = breach.message,
                                    fontSize = 12.sp,
                                    color = SafeSphereColors.Error,
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
                                detail.strength.label.contains("Strong") -> "💪"
                                detail.strength.label == "Good" -> "👍"
                                detail.strength.label == "Fair" -> "⚠️"
                                else -> "🔴"
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

            // Visual Strength Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(SafeSphereColors.SurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(detail.strength.score / 100f)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    strengthColor.copy(alpha = 0.7f),
                                    strengthColor
                                )
                            )
                        )
                )
            }

            // Issues
            if (detail.issues.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SafeSphereColors.SurfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️ Issues:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.Warning
                    )
                    detail.issues.forEach { issue ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "•",
                                color = SafeSphereColors.Error,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = issue,
                                fontSize = 13.sp,
                                color = SafeSphereColors.TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Age info
            if (detail.ageInDays > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "📅", fontSize = 16.sp)
                    Text(
                        text = "Password age: ${detail.ageInDays} days",
                        fontSize = 13.sp,
                        color = SafeSphereColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Fix button if has issues OR breached
            if (hasIssues || isBreached) {
                Button(
                    onClick = onFix,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBreached) SafeSphereColors.Error else SafeSphereColors.Warning
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBreached) "🚨 Change Immediately!" else "🔄 Generate Strong Password",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SafeSphereColors.Success.copy(alpha = 0.2f))
                        .border(
                            width = 1.dp,
                            color = SafeSphereColors.Success.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "✅", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Strong & Secure",
                        fontSize = 14.sp,
                        color = SafeSphereColors.Success,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
