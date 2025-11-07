package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.runanywhere.startup_hackathon20.utils.PasswordAnalyzer
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen

/**
 * Password Health Analyzer Screen
 * Shows comprehensive password security analysis
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
                // PROPER DECRYPTION using SecurityManager
                try {
                    com.runanywhere.startup_hackathon20.security.SecurityManager.decrypt(
                        encryptedContent
                    )
                } catch (e: Exception) {
                    // Fallback to encrypted content if decryption fails
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
                title = { Text("🔐 Password Health") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Analyzing passwords...")
                }
            }
        } else if (healthReport == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No password data available")
            }
        } else {
            PasswordHealthContent(
                report = healthReport!!,
                viewModel = viewModel,
                onFixPassword = { passwordDetail ->
                    // Generate new strong password and update vault item
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
 * Health Score Card - Large visual score display
 */
@Composable
private fun HealthScoreCard(score: Int) {
    val (color, label, emoji) = when {
        score >= 85 -> Triple(Color(0xFF388E3C), "Excellent", "🎉")
        score >= 70 -> Triple(Color(0xFF7CB342), "Good", "✅")
        score >= 50 -> Triple(Color(0xFFFBC02D), "Fair", "⚠️")
        score >= 30 -> Triple(Color(0xFFF57C00), "Poor", "🔴")
        else -> Triple(Color(0xFFD32F2F), "Critical", "🚨")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Animated score
            Text(
                text = "$score",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = "/ 100",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = score / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Issues Summary Card
 */
@Composable
private fun IssuesSummaryCard(report: PasswordAnalyzer.HealthReport) {
    val breachedCount = report.passwordDetails.count { it.breachResult?.isBreached == true }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚠️ Issues Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (breachedCount > 0) {
                IssueRow(
                    icon = "🚨",
                    text = "$breachedCount Leaked Password${if (breachedCount > 1) "s" else ""} - Change Now!"
                )
            }

            if (report.weakPasswords > 0) {
                IssueRow(
                    icon = "🔴",
                    text = "${report.weakPasswords} Weak Password${if (report.weakPasswords > 1) "s" else ""}"
                )
            }

            if (report.duplicatePasswords > 0) {
                IssueRow(
                    icon = "❌",
                    text = "${report.duplicatePasswords} Duplicate Password${if (report.duplicatePasswords > 1) "s" else ""}"
                )
            }

            if (report.commonPasswords > 0) {
                IssueRow(
                    icon = "⚠️",
                    text = "${report.commonPasswords} Common Password${if (report.commonPasswords > 1) "s" else ""}"
                )
            }

            if (report.oldPasswords > 0) {
                IssueRow(
                    icon = "⏰",
                    text = "${report.oldPasswords} Old Password${if (report.oldPasswords > 1) "s" else ""} (>1 year)"
                )
            }
        }
    }
}

/**
 * Issue Row
 */
@Composable
private fun IssueRow(icon: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = icon, style = MaterialTheme.typography.bodyLarge)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Password Detail Card
 */
@Composable
private fun PasswordDetailCard(
    detail: PasswordAnalyzer.PasswordDetail,
    onFix: () -> Unit
) {
    val strengthColor = Color(detail.strength.color)
    val hasIssues = detail.issues.isNotEmpty()
    val isBreached = detail.breachResult?.isBreached == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isBreached) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            } else if (hasIssues) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isBreached) {
            BorderStroke(2.dp, Color(0xFFD32F2F))
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title and Strength
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = detail.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Breach Warning (Most Prominent)
                    detail.breachResult?.let { breach ->
                        if (breach.isBreached) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = Color(breach.severity.color),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "🚨 LEAKED",
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = breach.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(breach.severity.color),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = strengthColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = detail.strength.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Issues
            if (detail.issues.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    detail.issues.forEach { issue ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "•", color = MaterialTheme.colorScheme.error)
                            Text(
                                text = issue,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Age info
            if (detail.ageInDays > 0) {
                Text(
                    text = "Password age: ${detail.ageInDays} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Fix button if has issues OR breached
            if (hasIssues || isBreached) {
                Button(
                    onClick = onFix,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBreached) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isBreached) "Change Immediately!" else "Generate Strong Password")
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "✅", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Strong & Secure",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF388E3C)
                    )
                }
            }
        }
    }
}
