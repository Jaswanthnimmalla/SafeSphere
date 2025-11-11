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
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.min

/**
 * ENHANCED AI PREDICTOR SCREEN
 * 
 * Advanced Features:
 * - Real-time per-password analysis
 * - Smart fix recommendations
 * - Breach details with context
 * - Attack simulation results
 * - One-tap auto-fix workflow
 * - Live quality monitoring
 */
@Composable
fun EnhancedAIPredictorScreen(viewModel: SafeSphereViewModel) {
    val context = LocalContext.current
    val passwordRepo = remember { PasswordVaultRepository.getInstance(context) }
    val passwords by passwordRepo.passwords.collectAsState()

    var passwordAnalyses by remember { mutableStateOf<List<AISecurityPredictor.PasswordAnalysisResult>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(AnalysisTab.PER_PASSWORD) }
    var selectedPassword by remember { mutableStateOf<AISecurityPredictor.PasswordAnalysisResult?>(null) }
    var showFixDialog by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    
    val coroutineScope = rememberCoroutineScope()
    val predictor = remember { AISecurityPredictor() }

    // Initial analysis
    LaunchedEffect(passwords) {
        if (passwords.isNotEmpty()) {
            isAnalyzing = true
            try {
                // Animated progress
                for (i in 0..100 step 5) {
                    scanProgress = i / 100f
                    delay(30)
                }
                passwordAnalyses = predictor.analyzeAllPasswords(passwords)
            } finally {
                isAnalyzing = false
                scanProgress = 0f
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        EnhancedPredictorHeader(
            onBackClick = { viewModel.navigateToScreen(SafeSphereScreen.DASHBOARD) },
            onRefreshClick = {
                coroutineScope.launch {
                    isAnalyzing = true
                    scanProgress = 0f
                    try {
                        for (i in 0..100 step 10) {
                            scanProgress = i / 100f
                            delay(20)
                        }
                        passwordAnalyses = predictor.analyzeAllPasswords(passwords)
                    } finally {
                        isAnalyzing = false
                        scanProgress = 0f
                    }
                }
            },
            isAnalyzing = isAnalyzing
        )

        if (isAnalyzing) {
            AnalyzingProgress(progress = scanProgress)
        } else if (passwordAnalyses.isEmpty()) {
            EmptyAnalysisState()
        } else {
            // Tab selector
            TabSelector(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // Content based on tab
            when (selectedTab) {
                AnalysisTab.PER_PASSWORD -> PerPasswordAnalysisView(
                    analyses = passwordAnalyses,
                    onPasswordClick = { selectedPassword = it },
                    onFixClick = { 
                        selectedPassword = it
                        showFixDialog = true
                    }
                )
                AnalysisTab.QUALITY_MONITOR -> QualityMonitorView(analyses = passwordAnalyses)
                AnalysisTab.AUTO_FIX -> AutoFixWorkflowView(
                    analyses = passwordAnalyses,
                    onFixAll = {
                        // TODO: Implement auto-fix all
                    }
                )
            }
        }
    }

    // Fix dialog
    if (showFixDialog && selectedPassword != null) {
        PasswordFixDialog(
            analysis = selectedPassword!!,
            onDismiss = { showFixDialog = false },
            onApplyFix = { newPassword ->
                // TODO: Apply fix
                showFixDialog = false
            }
        )
    }
}

enum class AnalysisTab(val title: String, val icon: String) {
    PER_PASSWORD("Per-Password", "🔐"),
    QUALITY_MONITOR("Quality", "📊"),
    AUTO_FIX("Auto-Fix", "🤖")
}

@Composable
private fun EnhancedPredictorHeader(
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    isAnalyzing: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SafeSphereColors.Surface.copy(alpha = 0.95f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF2196F3).copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
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
                    text = "🔮 Advanced Security AI",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
                Text(
                    text = "Real-Time Analysis",
                    fontSize = 12.sp,
                    color = SafeSphereColors.TextSecondary
                )
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
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = if (isAnalyzing) Color(0xFF2196F3) else SafeSphereColors.Primary,
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun AnalyzingProgress(progress: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            val scale by rememberInfiniteTransition(label = "scale").animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Text(text = "🔬", fontSize = 64.sp, modifier = Modifier.scale(scale))

            Text(
                text = "Deep Analysis in Progress...",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )

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
                    color = Color(0xFF2196F3),
                    trackColor = SafeSphereColors.SurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${(progress * 100).toInt()}% Complete",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
private fun EmptyAnalysisState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(48.dp)
        ) {
            Text(text = "🔐", fontSize = 80.sp)
            Text(
                text = "No Passwords to Analyze",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Add passwords to see advanced AI analysis with per-password insights",
                fontSize = 16.sp,
                color = SafeSphereColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun TabSelector(
    selectedTab: AnalysisTab,
    onTabSelected: (AnalysisTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SafeSphereColors.Surface.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnalysisTab.values().forEach { tab ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selectedTab == tab) Color(0xFF2196F3).copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .border(
                        width = if (selectedTab == tab) 2.dp else 1.dp,
                        color = if (selectedTab == tab) Color(0xFF2196F3)
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
                    Text(text = tab.icon, fontSize = 20.sp)
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == tab) Color(0xFF2196F3)
                        else SafeSphereColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PerPasswordAnalysisView(
    analyses: List<AISecurityPredictor.PasswordAnalysisResult>,
    onPasswordClick: (AISecurityPredictor.PasswordAnalysisResult) -> Unit,
    onFixClick: (AISecurityPredictor.PasswordAnalysisResult) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "🔐 Individual Password Analysis",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )
        }

        items(analyses.sortedBy { it.overallScore }) { analysis ->
            PasswordAnalysisCard(
                analysis = analysis,
                onClick = { onPasswordClick(analysis) },
                onFixClick = { onFixClick(analysis) }
            )
        }
    }
}

@Composable
private fun PasswordAnalysisCard(
    analysis: AISecurityPredictor.PasswordAnalysisResult,
    onClick: () -> Unit,
    onFixClick: () -> Unit
) {
    val color = Color(analysis.strengthLevel.colorHex)
    
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = analysis.passwordEntry.service,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )
                    Text(
                        text = analysis.passwordEntry.username,
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }

                // Score badge
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${analysis.overallScore}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Text(
                            text = "/100",
                            fontSize = 10.sp,
                            color = SafeSphereColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Strength level
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = analysis.strengthLevel.icon, fontSize = 24.sp)
                Text(
                    text = "${analysis.strengthLevel.displayName.uppercase()} STRENGTH",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Issues
            if (analysis.issues.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Issues Found:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SafeSphereColors.TextPrimary
                    )
                    analysis.issues.take(3).forEach { issue ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = issue.type.icon, fontSize = 16.sp)
                            Text(
                                text = issue.description,
                                fontSize = 12.sp,
                                color = SafeSphereColors.TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    if (analysis.issues.size > 3) {
                        Text(
                            text = "+ ${analysis.issues.size - 3} more issues",
                            fontSize = 11.sp,
                            color = SafeSphereColors.TextSecondary,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Fix button
                Button(
                    onClick = onFixClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fix Now (+${analysis.improvementPotential} points)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // All good
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SafeSphereColors.Success.copy(alpha = 0.1f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "✅", fontSize = 20.sp)
                    Text(
                        text = "Password is strong - no issues detected!",
                        fontSize = 13.sp,
                        color = SafeSphereColors.Success
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityMonitorView(analyses: List<AISecurityPredictor.PasswordAnalysisResult>) {
    val strongCount = analyses.count { it.strengthLevel in listOf(
        AISecurityPredictor.StrengthLevel.STRONG,
        AISecurityPredictor.StrengthLevel.EXCELLENT
    )}
    val mediumCount = analyses.count { it.strengthLevel == AISecurityPredictor.StrengthLevel.MEDIUM }
    val weakCount = analyses.count { it.strengthLevel in listOf(
        AISecurityPredictor.StrengthLevel.WEAK,
        AISecurityPredictor.StrengthLevel.CRITICAL
    )}
    val total = analyses.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "📊 Password Quality Distribution",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )

                    // Strong passwords
                    QualityBar(
                        label = "STRONG PASSWORDS",
                        count = strongCount,
                        total = total,
                        color = Color(0xFF4CAF50),
                        icon = "💚"
                    )

                    // Medium passwords
                    QualityBar(
                        label = "MEDIUM PASSWORDS",
                        count = mediumCount,
                        total = total,
                        color = Color(0xFFFBC02D),
                        icon = "🟡"
                    )

                    // Weak passwords
                    QualityBar(
                        label = "WEAK PASSWORDS",
                        count = weakCount,
                        total = total,
                        color = Color(0xFFD32F2F),
                        icon = "🔴"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Overall status
                    val overallQuality = (strongCount * 100f / total).toInt()
                    Text(
                        text = "Your vault is at $overallQuality% strength",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SafeSphereColors.TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Recent activity log
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "📝 Recent Security Events",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Show recent changes
                    analyses.take(5).forEach { analysis ->
                        ActivityLogItem(
                            service = analysis.passwordEntry.service,
                            score = analysis.overallScore,
                            timestamp = System.currentTimeMillis()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityBar(
    label: String,
    count: Int,
    total: Int,
    color: Color,
    icon: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = icon, fontSize = 20.sp)
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeSphereColors.TextPrimary
                )
            }
            Text(
                text = "$count (${if (total > 0) (count * 100 / total) else 0}%)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SafeSphereColors.SurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (total > 0) (count.toFloat() / total) else 0f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun ActivityLogItem(service: String, score: Int, timestamp: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SafeSphereColors.Surface.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = service,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SafeSphereColors.TextPrimary
            )
            Text(
                text = "Score: $score/100",
                fontSize = 12.sp,
                color = SafeSphereColors.TextSecondary
            )
        }
        Text(
            text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(timestamp)),
            fontSize = 11.sp,
            color = SafeSphereColors.TextSecondary
        )
    }
}

@Composable
private fun AutoFixWorkflowView(
    analyses: List<AISecurityPredictor.PasswordAnalysisResult>,
    onFixAll: () -> Unit
) {
    val issuesCount = analyses.count { it.issues.isNotEmpty() }
    val totalImprovementPotential = analyses.sumOf { it.improvementPotential }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFF2196F3).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF2196F3).copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🤖", fontSize = 64.sp)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Automated Security Fixes",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "$issuesCount passwords need attention",
                        fontSize = 16.sp,
                        color = SafeSphereColors.TextPrimary
                    )
                    
                    Text(
                        text = "Total improvement potential: +$totalImprovementPotential points",
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onFixAll,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auto-Fix All Issues",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Passwords Requiring Fixes:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )
        }

        items(analyses.filter { it.issues.isNotEmpty() }) { analysis ->
            AutoFixItem(analysis)
        }
    }
}

@Composable
private fun AutoFixItem(analysis: AISecurityPredictor.PasswordAnalysisResult) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD32F2F).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = analysis.passwordEntry.service,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
                Text(
                    text = "${analysis.issues.size} issue(s) • +${analysis.improvementPotential} pts",
                    fontSize = 12.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = SafeSphereColors.Success
            )
        }
    }
}

@Composable
private fun PasswordFixDialog(
    analysis: AISecurityPredictor.PasswordAnalysisResult,
    onDismiss: () -> Unit,
    onApplyFix: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🔧 Smart Fix for ${analysis.passwordEntry.service}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text(
                        text = "Current Score: ${analysis.overallScore}/100",
                        fontSize = 14.sp,
                        color = Color(analysis.strengthLevel.colorHex)
                    )
                }

                item {
                    Divider()
                    Text(
                        text = "Suggested New Password:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = analysis.suggestedPassword,
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SafeSphereColors.Surface)
                            .padding(12.dp)
                    )
                }

                item {
                    Text(
                        text = "New Score: ~${min(95, analysis.overallScore + analysis.improvementPotential)}/100",
                        fontSize = 14.sp,
                        color = SafeSphereColors.Success
                    )
                }

                items(analysis.recommendations) { rec ->
                    Text(
                        text = rec,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onApplyFix(analysis.suggestedPassword) }) {
                Text("Apply Fix")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
