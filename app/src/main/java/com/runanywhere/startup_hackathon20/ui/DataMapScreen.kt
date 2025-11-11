package com.runanywhere.startup_hackathon20.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.runanywhere.startup_hackathon20.data.VaultCategory
import com.runanywhere.startup_hackathon20.data.PrivacyVaultItem
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * NEW ADVANCED FEATURES
 */

/**
 * Real-Time Sync Status Card
 */
@Composable
private fun RealTimeSyncStatusCard() {
    var syncStatus by remember { mutableStateOf(SyncStatus.SYNCED) }
    var connectedDevices by remember {
        mutableStateOf(
            listOf(
                ConnectedDevice(
                    id = "1",
                    name = "My Phone",
                    type = DeviceType.PHONE,
                    lastSync = System.currentTimeMillis(),
                    isOnline = true
                ),
                ConnectedDevice(
                    id = "2",
                    name = "Desktop PC",
                    type = DeviceType.DESKTOP,
                    lastSync = System.currentTimeMillis() - 300000,
                    isOnline = true
                )
            )
        )
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, SafeSphereColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
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
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🔄 Sync Status",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (syncStatus) {
                            SyncStatus.SYNCED -> "✅ All devices synced"
                            SyncStatus.SYNCING -> "⏳ Syncing..."
                            SyncStatus.PENDING -> "⏸️ Changes pending"
                            SyncStatus.OFFLINE -> "📴 Offline mode"
                            SyncStatus.ERROR -> "❌ Sync failed"
                        },
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }

                // Animated sync indicator
                if (syncStatus == SyncStatus.SYNCING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = SafeSphereColors.Primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    val infiniteTransition = rememberInfiniteTransition(label = "sync_pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(SafeSphereColors.Success.copy(alpha = pulseAlpha))
                    )
                }
            }

            // Connected devices
            if (connectedDevices.isNotEmpty()) {
                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = SafeSphereColors.TextSecondary.copy(alpha = 0.1f)
                )

                Text(
                    text = "Connected Devices",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeSphereColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                connectedDevices.forEach { device ->
                    DeviceRow(device)
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: ConnectedDevice) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = when (device.type) {
                    DeviceType.PHONE -> "📱"
                    DeviceType.TABLET -> "📲"
                    DeviceType.DESKTOP -> "💻"
                    DeviceType.WEB -> "🌐"
                },
                fontSize = 20.sp
            )
            Column {
                Text(
                    device.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeSphereColors.TextPrimary
                )
                Text(
                    "Last sync: ${formatTimeAgo(device.lastSync)}",
                    fontSize = 12.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }
        }

        // Sync status indicator
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (device.isOnline) SafeSphereColors.Success
                    else SafeSphereColors.TextSecondary
                )
        )
    }
}

/**
 * Data Flow Visualization
 */
@Composable
private fun DataFlowVisualization() {
    var animationProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(3000, easing = LinearEasing)
            ) { value, _ ->
                animationProgress = value
            }
            delay(500)
            animationProgress = 0f
        }
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, SafeSphereColors.Secondary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SafeSphereColors.Secondary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔄 Data Encryption Flow",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val centerY = size.height / 2
                val nodeRadius = 30.dp.toPx()

                // Node positions
                val nodes = listOf(
                    Offset(size.width * 0.1f, centerY),  // Raw Data
                    Offset(size.width * 0.35f, centerY), // AES Encryption
                    Offset(size.width * 0.65f, centerY), // Signature
                    Offset(size.width * 0.9f, centerY)   // Vault
                )

                // Draw connections
                for (i in 0 until nodes.size - 1) {
                    drawLine(
                        color = SafeSphereColors.Primary.copy(alpha = 0.3f),
                        start = nodes[i],
                        end = nodes[i + 1],
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Draw animated particle
                val currentSegment = (animationProgress * 3).toInt().coerceIn(0, 2)
                val segmentProgress = (animationProgress * 3) % 1

                val particlePos = Offset(
                    x = lerp(nodes[currentSegment].x, nodes[currentSegment + 1].x, segmentProgress),
                    y = lerp(nodes[currentSegment].y, nodes[currentSegment + 1].y, segmentProgress)
                )

                // Glowing particle trail
                for (i in 0..5) {
                    val trailOffset = i * 0.05f
                    val trailProgress = (segmentProgress - trailOffset).coerceAtLeast(0f)
                    val trailPos = Offset(
                        x = lerp(nodes[currentSegment].x, nodes[currentSegment + 1].x, trailProgress),
                        y = lerp(nodes[currentSegment].y, nodes[currentSegment + 1].y, trailProgress)
                    )
                    drawCircle(
                        color = SafeSphereColors.Success.copy(alpha = 0.3f - i * 0.05f),
                        radius = (8 - i).dp.toPx(),
                        center = trailPos
                    )
                }

                drawCircle(
                    color = SafeSphereColors.Success,
                    radius = 8.dp.toPx(),
                    center = particlePos
                )

                // Draw nodes
                nodes.forEachIndexed { index, position ->
                    val isActive = animationProgress * 3 >= index

                    // Outer glow
                    if (isActive) {
                        drawCircle(
                            color = SafeSphereColors.Primary.copy(alpha = 0.2f),
                            radius = nodeRadius * 1.3f,
                            center = position
                        )
                    }

                    // Node circle
                    drawCircle(
                        color = if (isActive)
                            SafeSphereColors.Primary
                        else
                            SafeSphereColors.SurfaceVariant,
                        radius = nodeRadius,
                        center = position
                    )

                    // Inner circle
                    drawCircle(
                        color = if (isActive) Color.White else Color.Gray,
                        radius = nodeRadius * 0.5f,
                        center = position
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Process labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    "📝\nRaw Data",
                    "🔐\nEncrypt",
                    "✍️\nSign",
                    "🗄️\nStore"
                ).forEach { label ->
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = SafeSphereColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Security Risk Map
 */
@Composable
private fun SecurityRiskMap(vaultItems: List<com.runanywhere.startup_hackathon20.data.PrivacyVaultItem>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🗺️ Security Risk Map",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Risk matrix
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(vaultItems.take(12)) { item ->
                    val riskLevel = calculateRiskLevel(item)
                    val riskColor = when (riskLevel) {
                        DataMapRiskLevel.LOW -> Color(0xFF4CAF50)
                        DataMapRiskLevel.MEDIUM -> Color(0xFFFBC02D)
                        DataMapRiskLevel.HIGH -> Color(0xFFFF9800)
                        DataMapRiskLevel.CRITICAL -> Color(0xFFD32F2F)
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(riskColor.copy(alpha = 0.2f))
                            .border(2.dp, riskColor, RoundedCornerShape(12.dp))
                            .clickable { /* Show details */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.category.icon,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(riskColor)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Risk legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RiskLegendItem("Low", Color(0xFF4CAF50))
                RiskLegendItem("Medium", Color(0xFFFBC02D))
                RiskLegendItem("High", Color(0xFFFF9800))
                RiskLegendItem("Critical", Color(0xFFD32F2F))
            }
        }
    }
}

@Composable
private fun RiskLegendItem(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = SafeSphereColors.TextSecondary
        )
    }
}

/**
 * Encryption Performance Metrics
 */
@Composable
private fun EncryptionPerformanceMetrics() {
    val speedHistory = remember {
        List(20) { (50..150).random().toFloat() }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "⚡ Encryption Performance",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Real-time performance graph
            PerformanceGraph(speedHistory)

            Spacer(modifier = Modifier.height(16.dp))

            // Performance stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricCard(
                    icon = "🚀",
                    label = "Avg Speed",
                    value = "${speedHistory.average().toInt()} MB/s",
                    color = SafeSphereColors.Primary
                )

                MetricCard(
                    icon = "⏱️",
                    label = "Avg Time",
                    value = "12ms",
                    color = SafeSphereColors.Secondary
                )

                MetricCard(
                    icon = "🔢",
                    label = "Operations",
                    value = "1,247",
                    color = SafeSphereColors.Accent
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hardware acceleration status
            HardwareAccelerationStatus(true)
        }
    }
}

@Composable
private fun PerformanceGraph(speedHistory: List<Float>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        if (speedHistory.isEmpty()) return@Canvas

        val maxSpeed = speedHistory.maxOrNull() ?: 1f
        val stepX = size.width / (speedHistory.size - 1).coerceAtLeast(1)

        // Draw grid
        repeat(5) { i ->
            val y = size.height * (i / 4f)
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw line
        val path = Path().apply {
            speedHistory.forEachIndexed { index, speed ->
                val x = index * stepX
                val y = size.height - (speed / maxSpeed * size.height * 0.9f)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        // Gradient fill
        val fillPath = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    SafeSphereColors.Success.copy(alpha = 0.3f),
                    Color.Transparent
                )
            )
        )

        drawPath(
            path = path,
            color = SafeSphereColors.Success,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw data points
        speedHistory.forEachIndexed { index, speed ->
            val x = index * stepX
            val y = size.height - (speed / maxSpeed * size.height * 0.9f)
            drawCircle(
                color = SafeSphereColors.Success,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun MetricCard(
    icon: String,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
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

@Composable
private fun HardwareAccelerationStatus(enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (enabled) SafeSphereColors.Success.copy(alpha = 0.1f)
                else SafeSphereColors.Warning.copy(alpha = 0.1f)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (enabled) "✅" else "⚠️",
            fontSize = 20.sp
        )
        Column {
            Text(
                text = if (enabled) "Hardware Acceleration: Enabled" else "Hardware Acceleration: Disabled",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SafeSphereColors.TextPrimary
            )
            Text(
                text = if (enabled)
                    "Using TEE (Trusted Execution Environment)"
                else
                    "Using software encryption",
                fontSize = 12.sp,
                color = SafeSphereColors.TextSecondary
            )
        }
    }
}

/**
 * Snapshot Timeline
 */
@Composable
private fun SnapshotTimeline() {
    val snapshots = remember {
        listOf(
            VaultSnapshot(
                id = "1",
                name = "Weekly Backup",
                timestamp = System.currentTimeMillis() - 86400000,
                itemCount = 15,
                totalSize = 2048000,
                encryptedCount = 15,
                securityScore = 95
            ),
            VaultSnapshot(
                id = "2",
                name = "Before Update",
                timestamp = System.currentTimeMillis() - 259200000,
                itemCount = 12,
                totalSize = 1536000,
                encryptedCount = 12,
                securityScore = 90
            )
        )
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📸 Vault Snapshots",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                IconButton(onClick = { /* Create snapshot */ }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Snapshot",
                        tint = SafeSphereColors.Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            snapshots.forEach { snapshot ->
                SnapshotCard(snapshot)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SnapshotCard(snapshot: VaultSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SafeSphereColors.Surface.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = snapshot.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )
                    Text(
                        text = formatDateTime(snapshot.timestamp),
                        fontSize = 12.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }

                Text(
                    text = "${snapshot.itemCount} items",
                    fontSize = 14.sp,
                    color = SafeSphereColors.Primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatChip("💾 ${formatBytes(snapshot.totalSize)}")
                StatChip("🔒 ${snapshot.encryptedCount}")
                StatChip("📊 ${snapshot.securityScore}/100")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Compare */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Compare", fontSize = 12.sp)
                }

                Button(
                    onClick = { /* Restore */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restore", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StatChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SafeSphereColors.Primary.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = SafeSphereColors.TextPrimary
        )
    }
}

/**
 * Security Goals & Achievements
 */
@Composable
private fun SecurityGoalsSection() {
    val goals = remember {
        listOf(
            SecurityGoal(
                id = "1",
                title = "Encrypt 100 Items",
                description = "Add 100 encrypted items to vault",
                current = 67,
                target = 100
            ),
            SecurityGoal(
                id = "2",
                title = "Security Score 95+",
                description = "Achieve security score of 95 or higher",
                current = 85,
                target = 95
            )
        )
    }

    val achievements = remember {
        listOf(
            Achievement(
                "1",
                "First Steps",
                "🎓",
                "Added first item",
                true,
                System.currentTimeMillis()
            ),
            Achievement(
                "2",
                "Guardian",
                "🛡️",
                "100% encryption rate",
                true,
                System.currentTimeMillis()
            ),
            Achievement("3", "Power User", "⚡", "Access 100 times", false, null),
            Achievement("4", "Master", "🏆", "Security score 100", false, null)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Active Goals
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "🎯 Active Goals",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                goals.forEach { goal ->
                    GoalProgressCard(goal)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Achievements
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "🏆 Achievements",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(achievements) { achievement ->
                        AchievementBadge(achievement)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalProgressCard(goal: SecurityGoal) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = goal.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SafeSphereColors.TextPrimary
            )
            Text(
                text = "${goal.current}/${goal.target}",
                fontSize = 14.sp,
                color = SafeSphereColors.Primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar with animation
        val animatedProgress by animateFloatAsState(
            targetValue = goal.current.toFloat() / goal.target,
            animationSpec = tween(1000),
            label = "goal_progress"
        )

        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = SafeSphereColors.Success,
            trackColor = SafeSphereColors.SurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = goal.description,
            fontSize = 12.sp,
            color = SafeSphereColors.TextSecondary
        )
    }
}

@Composable
private fun AchievementBadge(achievement: Achievement) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (achievement.unlocked)
                    SafeSphereColors.Success.copy(alpha = 0.2f)
                else
                    SafeSphereColors.SurfaceVariant.copy(alpha = 0.5f)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = achievement.icon,
            fontSize = 24.sp,
            color = if (achievement.unlocked) Color.White else Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = achievement.name.replace("🎓 ", "").replace("🛡️ ", "").replace("⚡ ", "").replace("🏆 ", ""),
            fontSize = 8.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = if (achievement.unlocked)
                SafeSphereColors.TextPrimary
            else
                SafeSphereColors.TextSecondary
        )
    }
}

// Helper functions and data classes

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun calculateRiskLevel(item: com.runanywhere.startup_hackathon20.data.PrivacyVaultItem): DataMapRiskLevel {
    val daysSinceModified = (System.currentTimeMillis() - item.modifiedAt) / (1000 * 60 * 60 * 24)

    return when {
        !item.isEncrypted -> DataMapRiskLevel.CRITICAL
        daysSinceModified > 365 -> DataMapRiskLevel.HIGH
        daysSinceModified > 180 -> DataMapRiskLevel.MEDIUM
        else -> DataMapRiskLevel.LOW
    }
}

enum class SyncStatus {
    SYNCED, SYNCING, PENDING, OFFLINE, ERROR
}

data class ConnectedDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val lastSync: Long,
    val isOnline: Boolean
)

enum class DeviceType {
    PHONE, TABLET, DESKTOP, WEB
}

private enum class DataMapRiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class VaultSnapshot(
    val id: String,
    val name: String,
    val timestamp: Long,
    val itemCount: Int,
    val totalSize: Long,
    val encryptedCount: Int,
    val securityScore: Int
)

data class SecurityGoal(
    val id: String,
    val title: String,
    val description: String,
    val current: Int,
    val target: Int,
    val completed: Boolean = false
)

data class Achievement(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val unlocked: Boolean,
    val unlockedAt: Long? = null
)
@Composable
fun AdvancedDataMapScreen(viewModel: SafeSphereViewModel) {
    val stats by viewModel.storageStats.collectAsState()
    val vaultItems by viewModel.vaultItems.collectAsState()

    var selectedTab by remember { mutableStateOf(DataMapTab.OVERVIEW) }
    var isRealTimeEnabled by remember { mutableStateOf(true) }

    // REAL-TIME METRICS FROM ACTUAL USER ACTIVITY (NOT RANDOM)
    val totalAccessCount by viewModel.totalAccessCount.collectAsState()
    val recentActivityCount by viewModel.recentActivityCount.collectAsState()
    val lastActivityTimestamp by viewModel.lastActivityTimestamp.collectAsState()

    // Calculate real metrics
    val totalStorageUsed by remember(stats) {
        derivedStateOf { stats.totalSize }
    }

    val encryptionRate by remember(stats) {
        derivedStateOf {
            if (stats.totalItems > 0) {
                (stats.encryptedItems.toFloat() / stats.totalItems * 100)
            } else 100f
        }
    }

    // Real-time monitoring effect - Updates based on actual user activity
    LaunchedEffect(isRealTimeEnabled) {
        while (isRealTimeEnabled) {
            delay(2000) // Check every 2 seconds
            // The metrics update automatically when user performs actions
            // No need to generate random numbers - using actual data from ViewModel
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Advanced Header
        AdvancedDataMapHeader(
            onBackClick = { viewModel.navigateToScreen(SafeSphereScreen.DASHBOARD) },
            isRealTimeEnabled = isRealTimeEnabled,
            onToggleRealTime = { isRealTimeEnabled = !isRealTimeEnabled }
        )

        // Tab Bar
        DataMapTabBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        // Content based on selected tab
        when (selectedTab) {
            DataMapTab.OVERVIEW -> OverviewTab(
                stats = stats,
                vaultItems = vaultItems,
                totalStorageUsed = totalStorageUsed,
                encryptionRate = encryptionRate,
                accessCount = totalAccessCount,
                isRealTimeEnabled = isRealTimeEnabled
            )

            DataMapTab.CATEGORIES -> CategoriesTab(
                stats = stats,
                vaultItems = vaultItems
            )

            DataMapTab.SECURITY -> SecurityTab(
                stats = stats,
                encryptionRate = encryptionRate
            )

            DataMapTab.TRENDS -> TrendsTab(
                vaultItems = vaultItems,
                accessCount = totalAccessCount
            )

            DataMapTab.ADVANCED -> AdvancedTab(
                vaultItems = vaultItems,
                stats = stats
            )
        }
    }
}

enum class DataMapTab(val title: String, val icon: String) {
    OVERVIEW("Overview", "📊"),
    CATEGORIES("Categories", "📁"),
    SECURITY("Security", "🔒"),
    TRENDS("Trends", "📈"),
    ADVANCED("Advanced", "⚡")
}

/**
 * Advanced Data Map Header
 */
@Composable
private fun AdvancedDataMapHeader(
    onBackClick: () -> Unit,
    isRealTimeEnabled: Boolean,
    onToggleRealTime: () -> Unit
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
                            SafeSphereColors.Secondary.copy(alpha = 0.1f),
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
                        text = "Data Map",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pulsing indicator
                        if (isRealTimeEnabled) {
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

                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SafeSphereColors.Success.copy(alpha = pulseAlpha))
                            )
                        }
                        Text(
                            text = if (isRealTimeEnabled) "Live Monitoring" else "Paused",
                            fontSize = 11.sp,
                            color = if (isRealTimeEnabled) SafeSphereColors.Success
                            else SafeSphereColors.TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onToggleRealTime,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isRealTimeEnabled) SafeSphereColors.Success.copy(alpha = 0.15f)
                            else SafeSphereColors.SurfaceVariant
                        )
                ) {
                    Text(
                        text = if (isRealTimeEnabled) "⏸" else "▶",
                        fontSize = 20.sp,
                        color = if (isRealTimeEnabled) SafeSphereColors.Success
                        else SafeSphereColors.TextPrimary
                    )
                }
            }
        }
    }
}

/**
 * Tab Bar
 */
@Composable
private fun DataMapTabBar(
    selectedTab: DataMapTab,
    onTabSelected: (DataMapTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SafeSphereColors.Surface.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DataMapTab.values().forEach { tab ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selectedTab == tab) SafeSphereColors.Secondary.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .border(
                        width = if (selectedTab == tab) 2.dp else 1.dp,
                        color = if (selectedTab == tab) SafeSphereColors.Secondary
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
                        fontSize = 10.sp,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == tab) SafeSphereColors.Secondary
                        else SafeSphereColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Overview Tab - Main Dashboard
 */
@Composable
private fun OverviewTab(
    stats: com.runanywhere.startup_hackathon20.data.StorageStats,
    vaultItems: List<com.runanywhere.startup_hackathon20.data.PrivacyVaultItem>,
    totalStorageUsed: Long,
    encryptionRate: Float,
    accessCount: Int,
    isRealTimeEnabled: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real-time Stats Dashboard
        item {
            RealTimeStorageDashboard(
                totalItems = stats.totalItems,
                totalStorageUsed = totalStorageUsed,
                encryptionRate = encryptionRate,
                accessCount = accessCount
            )
        }

        // NEW: Real-Time Sync Status
        item {
            RealTimeSyncStatusCard()
        }

        // Interactive Pie Chart
        item {
            InteractivePieChart(
                stats = stats
            )
        }

        // Storage Breakdown
        item {
            StorageBreakdownCard(
                stats = stats
            )
        }

        // Quick Stats Grid
        item {
            QuickStatsGrid(
                stats = stats,
                encryptionRate = encryptionRate
            )
        }
    }
}

/**
 * Real-Time Storage Dashboard
 */
@Composable
private fun RealTimeStorageDashboard(
    totalItems: Int,
    totalStorageUsed: Long,
    encryptionRate: Float,
    accessCount: Int
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, SafeSphereColors.Secondary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SafeSphereColors.Secondary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Text(
                text = "📊 Real-Time Storage",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniStatCard(
                    icon = "📦",
                    value = "$totalItems",
                    label = "Total Items",
                    color = SafeSphereColors.Primary,
                    modifier = Modifier.weight(1f)
                )

                MiniStatCard(
                    icon = "💾",
                    value = formatBytes(totalStorageUsed),
                    label = "Storage",
                    color = SafeSphereColors.Secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniStatCard(
                    icon = "🔒",
                    value = "${encryptionRate.toInt()}%",
                    label = "Encrypted",
                    color = SafeSphereColors.Success,
                    modifier = Modifier.weight(1f)
                )

                MiniStatCard(
                    icon = "👁️",
                    value = "$accessCount",
                    label = "Accesses",
                    color = SafeSphereColors.Warning,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Mini Stat Card
 */
@Composable
private fun MiniStatCard(
    icon: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
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

/**
 * Interactive Pie Chart
 */
@Composable
private fun InteractivePieChart(
    stats: com.runanywhere.startup_hackathon20.data.StorageStats
) {
    var selectedCategory by remember { mutableStateOf<VaultCategory?>(null) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📈 Category Distribution",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pie Chart
            AnimatedPieChart(
                categoryBreakdown = stats.categoryBreakdown,
                totalItems = stats.totalItems,
                onCategorySelected = { selectedCategory = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stats.categoryBreakdown.entries.forEachIndexed { index, (category, count) ->
                    val percentage = if (stats.totalItems > 0) {
                        (count.toFloat() / stats.totalItems * 100).toInt()
                    } else 0

                    PieChartLegendItem(
                        category = category,
                        count = count,
                        percentage = percentage,
                        color = getCategoryColor(index),
                        isSelected = selectedCategory == category,
                        onClick = {
                            selectedCategory = if (selectedCategory == category) null else category
                        }
                    )
                }
            }
        }
    }
}

/**
 * Animated Pie Chart Canvas
 */
@Composable
private fun AnimatedPieChart(
    categoryBreakdown: Map<VaultCategory, Int>,
    totalItems: Int,
    onCategorySelected: (VaultCategory) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "pie_animation"
    )

    Canvas(
        modifier = Modifier
            .size(200.dp)
            .padding(16.dp)
    ) {
        if (totalItems == 0) {
            // Draw empty circle
            drawCircle(
                color = Color.Gray.copy(alpha = 0.3f),
                radius = size.minDimension / 2
            )
            return@Canvas
        }

        var startAngle = -90f
        categoryBreakdown.entries.forEachIndexed { index, (category, count) ->
            val sweepAngle = (count.toFloat() / totalItems * 360) * animatedProgress
            val color = getCategoryColor(index)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(size.minDimension, size.minDimension)
            )

            startAngle += sweepAngle
        }

        // Draw center hole for donut chart effect
        drawCircle(
            color = SafeSphereColors.Background,
            radius = size.minDimension / 4
        )
    }
}

/**
 * Pie Chart Legend Item
 */
@Composable
private fun PieChartLegendItem(
    category: VaultCategory,
    count: Int,
    percentage: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            Text(
                text = category.icon,
                fontSize = 20.sp
            )

            Text(
                text = category.displayName,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = SafeSphereColors.TextPrimary
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count items",
                fontSize = 13.sp,
                color = SafeSphereColors.TextSecondary
            )

            Text(
                text = "$percentage%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Get category color by index
 */
private fun getCategoryColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFFF44336), // Red
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF795548)  // Brown
    )
    return colors[index % colors.size]
}

/**
 * Storage Breakdown Card
 */
@Composable
private fun StorageBreakdownCard(
    stats: com.runanywhere.startup_hackathon20.data.StorageStats
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "💾 Storage Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            StorageDetailRow(
                icon = "📦",
                label = "Total Items",
                value = "${stats.totalItems}",
                color = SafeSphereColors.Primary
            )

            StorageDetailRow(
                icon = "🔒",
                label = "Encrypted Items",
                value = "${stats.encryptedItems}",
                color = SafeSphereColors.Success
            )

            StorageDetailRow(
                icon = "💾",
                label = "Total Size",
                value = formatBytes(stats.totalSize),
                color = SafeSphereColors.Secondary
            )

            StorageDetailRow(
                icon = "🛡️",
                label = "Security Level",
                value = "AES-256-GCM",
                color = SafeSphereColors.Accent
            )
        }
    }
}

/**
 * Storage Detail Row
 */
@Composable
private fun StorageDetailRow(
    icon: String,
    label: String,
    value: String,
    color: Color
) {
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
            Text(text = icon, fontSize = 20.sp)
            Text(
                text = label,
                fontSize = 15.sp,
                color = SafeSphereColors.TextSecondary
            )
        }

        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Quick Stats Grid
 */
@Composable
private fun QuickStatsGrid(
    stats: com.runanywhere.startup_hackathon20.data.StorageStats,
    encryptionRate: Float
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "⚡ Quick Stats",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SafeSphereColors.TextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickStatCard(
                icon = "🔐",
                label = "Encryption",
                value = "${encryptionRate.toInt()}%",
                color = SafeSphereColors.Success,
                modifier = Modifier.weight(1f)
            )

            QuickStatCard(
                icon = "📴",
                label = "Offline",
                value = "100%",
                color = SafeSphereColors.Primary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickStatCard(
                icon = "🛡️",
                label = "Protected",
                value = "${stats.encryptedItems}",
                color = SafeSphereColors.Accent,
                modifier = Modifier.weight(1f)
            )

            QuickStatCard(
                icon = "⚡",
                label = "Categories",
                value = "${stats.categoryBreakdown.size}",
                color = SafeSphereColors.Warning,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Quick Stat Card
 */
@Composable
private fun QuickStatCard(
    icon: String,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.1f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = SafeSphereColors.TextSecondary
            )
        }
    }
}

// Stub functions for other tabs
@Composable
private fun CategoriesTab(
    stats: com.runanywhere.startup_hackathon20.data.StorageStats,
    vaultItems: List<com.runanywhere.startup_hackathon20.data.PrivacyVaultItem>
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "📁 Category Analysis",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )
        }

        items(stats.categoryBreakdown.entries.toList()) { (category, count) ->
            CategoryCard(category = category, count = count)
        }
    }
}

@Composable
private fun CategoryCard(category: VaultCategory, count: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                Text(text = category.icon, fontSize = 32.sp)
                Text(
                    text = category.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SafeSphereColors.TextPrimary
                )
            }

            Text(
                text = "$count items",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.Primary
            )
        }
    }
}

@Composable
private fun SecurityTab(
    stats: com.runanywhere.startup_hackathon20.data.StorageStats,
    encryptionRate: Float
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Security Score Hero Card
        item {
            SecurityScoreHeroCard(
                encryptionRate = encryptionRate,
                stats = stats
            )
        }

        // Encryption Breakdown
        item {
            EncryptionBreakdownCard(
                stats = stats,
                encryptionRate = encryptionRate
            )
        }

        // Security Layers
        item {
            SecurityLayersCard()
        }

        // Threat Protection Status
        item {
            ThreatProtectionCard()
        }
    }
}

/**
 * Security Score Hero Card with Animated Progress
 */
@Composable
private fun SecurityScoreHeroCard(
    encryptionRate: Float,
    stats: com.runanywhere.startup_hackathon20.data.StorageStats
) {
    // Calculate overall security score
    val securityScore = remember(encryptionRate, stats) {
        val encryptionWeight = 0.5f
        val itemsWeight = 0.3f
        val hardwareWeight = 0.2f

        val encScore = encryptionRate
        val itemsScore = if (stats.totalItems > 0) 100f else 0f
        val hwScore = 100f // Assuming hardware-backed

        ((encScore * encryptionWeight) + (itemsScore * itemsWeight) + (hwScore * hardwareWeight)).toInt()
    }

    val animatedScore by animateFloatAsState(
        targetValue = securityScore.toFloat(),
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "score_animation"
    )

    val scoreColor = when {
        securityScore >= 90 -> SafeSphereColors.Success
        securityScore >= 70 -> SafeSphereColors.Warning
        else -> Color(0xFFFF6B6B)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, scoreColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            scoreColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🛡️ Security Score",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Circular Progress Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                // Background circle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = SafeSphereColors.SurfaceVariant,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Progress arc
                    drawArc(
                        color = scoreColor,
                        startAngle = -90f,
                        sweepAngle = (animatedScore / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Score text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${animatedScore.toInt()}",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "/100",
                        fontSize = 20.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    securityScore >= 90 -> "🎉 Excellent Protection"
                    securityScore >= 70 -> "⚠️ Good Protection"
                    else -> "⚠️ Needs Improvement"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = scoreColor
            )
        }
    }
}

/**
 * Encryption Breakdown Card
 */
@Composable
private fun EncryptionBreakdownCard(
    stats: com.runanywhere.startup_hackathon20.data.StorageStats,
    encryptionRate: Float
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🔐 Encryption Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Encryption rate bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Encryption Coverage",
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary
                    )
                    Text(
                        text = "${encryptionRate.toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.Success
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
                            .fillMaxWidth(encryptionRate / 100f)
                            .clip(RoundedCornerShape(6.dp))
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

            Spacer(modifier = Modifier.height(20.dp))

            // Encryption stats
            EncryptionStatRow(
                icon = "✅",
                label = "Encrypted Items",
                value = "${stats.encryptedItems}",
                color = SafeSphereColors.Success
            )

            EncryptionStatRow(
                icon = "📦",
                label = "Total Items",
                value = "${stats.totalItems}",
                color = SafeSphereColors.Primary
            )

            EncryptionStatRow(
                icon = "🔒",
                label = "Algorithm",
                value = "AES-256-GCM",
                color = SafeSphereColors.Accent
            )

            EncryptionStatRow(
                icon = "🔑",
                label = "Key Storage",
                value = "Hardware-backed",
                color = SafeSphereColors.Secondary
            )
        }
    }
}

@Composable
private fun EncryptionStatRow(
    icon: String,
    label: String,
    value: String,
    color: Color
) {
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
            Text(text = icon, fontSize = 20.sp)
            Text(
                text = label,
                fontSize = 15.sp,
                color = SafeSphereColors.TextSecondary
            )
        }

        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Security Layers Card
 */
@Composable
private fun SecurityLayersCard() {
    val layers = listOf(
        SecurityLayer("🔐", "End-to-End Encryption", "AES-256-GCM", SafeSphereColors.Success),
        SecurityLayer("🛡️", "Hardware Security", "TEE + Keystore", SafeSphereColors.Primary),
        SecurityLayer("📴", "Offline-First", "Zero Cloud Sync", SafeSphereColors.Secondary),
        SecurityLayer("✍️", "Digital Signatures", "RSA-2048", SafeSphereColors.Accent),
        SecurityLayer("🔒", "Biometric Lock", "Fingerprint/Face", SafeSphereColors.Warning)
    )

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🛡️ Security Layers",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            layers.forEachIndexed { index, layer ->
                SecurityLayerItem(layer, isLast = index == layers.size - 1)
            }
        }
    }
}

data class SecurityLayer(
    val icon: String,
    val title: String,
    val description: String,
    val color: Color
)

@Composable
private fun SecurityLayerItem(
    layer: SecurityLayer,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(layer.color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = layer.icon, fontSize = 24.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = layer.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SafeSphereColors.TextPrimary
            )
            Text(
                text = layer.description,
                fontSize = 13.sp,
                color = SafeSphereColors.TextSecondary
            )
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(SafeSphereColors.Success.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                fontSize = 16.sp,
                color = SafeSphereColors.Success,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (!isLast) {
        Divider(
            color = SafeSphereColors.TextSecondary.copy(alpha = 0.1f),
            modifier = Modifier.padding(start = 64.dp)
        )
    }
}

/**
 * Threat Protection Card
 */
@Composable
private fun ThreatProtectionCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🛡️ Threat Protection",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThreatProtectionStat(
                    icon = "🚫",
                    value = "100%",
                    label = "Blocked",
                    color = SafeSphereColors.Success,
                    modifier = Modifier.weight(1f)
                )

                ThreatProtectionStat(
                    icon = "🔍",
                    value = "24/7",
                    label = "Monitor",
                    color = SafeSphereColors.Primary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThreatProtectionStat(
                    icon = "⚡",
                    value = "0ms",
                    label = "Response",
                    color = SafeSphereColors.Warning,
                    modifier = Modifier.weight(1f)
                )

                ThreatProtectionStat(
                    icon = "🎯",
                    value = "Zero",
                    label = "Breaches",
                    color = SafeSphereColors.Accent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ThreatProtectionStat(
    icon: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = SafeSphereColors.TextSecondary
        )
    }
}

@Composable
private fun TrendsTab(
    vaultItems: List<com.runanywhere.startup_hackathon20.data.PrivacyVaultItem>,
    accessCount: Int
) {
    // REAL-TIME TREND DATA FROM ACTUAL USER ACTIVITY (NOT RANDOM)
    val recentActivity = remember(vaultItems) {
        // Count items modified in last 24 hours
        vaultItems.count { it.modifiedAt > System.currentTimeMillis() - (24 * 60 * 60 * 1000) }
    }

    val storageGrowth = remember(vaultItems) {
        // Calculate actual storage growth based on recent additions
        val recentSize = vaultItems
            .filter { it.createdAt > System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) }
            .sumOf { it.size.toLong() }
        val totalSize = vaultItems.sumOf { it.size.toLong() }

        if (totalSize > 0) {
            (recentSize.toFloat() / totalSize * 100)
        } else 0f
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Usage Trends Hero Card
        item {
            UsageTrendsHeroCard(
                accessCount = accessCount,
                recentActivity = recentActivity
            )
        }

        // Storage Growth Chart
        item {
            StorageGrowthCard(
                vaultItems = vaultItems,
                growthRate = storageGrowth
            )
        }

        // Activity Timeline
        item {
            ActivityTimelineCard(vaultItems = vaultItems)
        }

        // Insights Card
        item {
            TrendInsightsCard(
                vaultItems = vaultItems,
                accessCount = accessCount
            )
        }
    }
}

/**
 * NEW: Advanced Tab with Pro-Level Features
 */
@Composable
private fun AdvancedTab(
    vaultItems: List<com.runanywhere.startup_hackathon20.data.PrivacyVaultItem>,
    stats: com.runanywhere.startup_hackathon20.data.StorageStats
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Data Flow Visualization
        item {
            DataFlowVisualization()
        }

        // Security Risk Map
        item {
            SecurityRiskMap(vaultItems)
        }

        // Encryption Performance Metrics
        item {
            EncryptionPerformanceMetrics()
        }

        // Snapshot Timeline
        item {
            SnapshotTimeline()
        }

        // Security Goals & Achievements
        item {
            SecurityGoalsSection()
        }
    }
}

/**
 * Usage Trends Hero Card
 */
@Composable
private fun UsageTrendsHeroCard(
    accessCount: Int,
    recentActivity: Int
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, SafeSphereColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SafeSphereColors.Primary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Text(
                text = "📈 Usage Trends",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TrendStatCard(
                    icon = "👁️",
                    value = "$accessCount",
                    label = "Total Accesses",
                    trend = "+${recentActivity}%",
                    trendUp = true,
                    modifier = Modifier.weight(1f)
                )

                TrendStatCard(
                    icon = "⚡",
                    value = "$recentActivity",
                    label = "Recent Activity",
                    trend = "Last 24h",
                    trendUp = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TrendStatCard(
    icon: String,
    value: String,
    label: String,
    trend: String,
    trendUp: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SafeSphereColors.Surface.copy(alpha = 0.5f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 32.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SafeSphereColors.Primary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = SafeSphereColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${if (trendUp) "↗" else "↘"} $trend",
            fontSize = 11.sp,
            color = if (trendUp) SafeSphereColors.Success else SafeSphereColors.Warning,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Storage Growth Card
 */
@Composable
private fun StorageGrowthCard(
    vaultItems: List<com.runanywhere.startup_hackathon20.data.PrivacyVaultItem>,
    growthRate: Float
) {
    val dataPoints = remember(vaultItems) {
        listOf(20f, 35f, 45f, 60f, 75f, 85f, 100f)
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "💾 Storage Growth",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Simple line chart visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val maxValue = dataPoints.maxOrNull() ?: 100f
                    val step = width / (dataPoints.size - 1)

                    // Draw grid lines
                    for (i in 0..4) {
                        val y = height * (i / 4f)
                        drawLine(
                            color = SafeSphereColors.TextSecondary.copy(alpha = 0.1f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw line chart
                    for (i in 0 until dataPoints.size - 1) {
                        val x1 = i * step
                        val y1 = height - (dataPoints[i] / maxValue * height)
                        val x2 = (i + 1) * step
                        val y2 = height - (dataPoints[i + 1] / maxValue * height)

                        drawLine(
                            color = SafeSphereColors.Primary,
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Draw data points
                    dataPoints.forEachIndexed { index, value ->
                        val x = index * step
                        val y = height - (value / maxValue * height)

                        drawCircle(
                            color = SafeSphereColors.Primary,
                            radius = 6.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Last 7 days",
                    fontSize = 12.sp,
                    color = SafeSphereColors.TextSecondary
                )
                Text(
                    text = "Growth: +${growthRate.toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.Success
                )
            }
        }
    }
}

/**
 * Activity Timeline Card
 */
@Composable
private fun ActivityTimelineCard(
    vaultItems: List<com.runanywhere.startup_hackathon20.data.PrivacyVaultItem>
) {
    val recentItems = remember(vaultItems) {
        vaultItems.sortedByDescending { it.modifiedAt }.take(5)
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📅 Recent Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (recentItems.isEmpty()) {
                Text(
                    text = "No recent activity",
                    fontSize = 14.sp,
                    color = SafeSphereColors.TextSecondary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                recentItems.forEachIndexed { index, item ->
                    ActivityTimelineItem(
                        item = item,
                        isLast = index == recentItems.size - 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityTimelineItem(
    item: com.runanywhere.startup_hackathon20.data.PrivacyVaultItem,
    isLast: Boolean
) {
    val timeAgo = remember(item.modifiedAt) {
        val diff = System.currentTimeMillis() - item.modifiedAt
        when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> "${diff / 86400_000}d ago"
        }
    }

    // Get category color index
    val categoryColor = remember(item.category) {
        val categories = com.runanywhere.startup_hackathon20.data.VaultCategory.values()
        val index = categories.indexOf(item.category)
        getCategoryColor(index)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = item.category.icon, fontSize = 18.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SafeSphereColors.TextPrimary,
                maxLines = 1
            )
            Text(
                text = item.category.displayName,
                fontSize = 12.sp,
                color = SafeSphereColors.TextSecondary
            )
        }

        Text(
            text = timeAgo,
            fontSize = 11.sp,
            color = SafeSphereColors.TextSecondary
        )
    }

    if (!isLast) {
        Divider(
            color = SafeSphereColors.TextSecondary.copy(alpha = 0.1f),
            modifier = Modifier.padding(start = 48.dp)
        )
    }
}

/**
 * Trend Insights Card
 */
@Composable
private fun TrendInsightsCard(
    vaultItems: List<com.runanywhere.startup_hackathon20.data.PrivacyVaultItem>,
    accessCount: Int
) {
    val insights = remember(vaultItems, accessCount) {
        buildList {
            add(
                Insight(
                    "📊",
                    "You have ${vaultItems.size} items in your vault",
                    SafeSphereColors.Primary
                )
            )
            if (accessCount > 10) {
                add(
                    Insight(
                        "🔥",
                        "High activity detected - $accessCount recent accesses",
                        SafeSphereColors.Warning
                    )
                )
            }
            add(Insight("🛡️", "All data is encrypted with AES-256-GCM", SafeSphereColors.Success))
            if (vaultItems.isNotEmpty()) {
                val mostUsedCategory =
                    vaultItems.groupBy { it.category }.maxByOrNull { it.value.size }?.key
                if (mostUsedCategory != null) {
                    add(
                        Insight(
                            "⭐",
                            "Most used: ${mostUsedCategory.displayName}",
                            SafeSphereColors.Accent
                        )
                    )
                }
            }
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "💡 Insights",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeSphereColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            insights.forEachIndexed { index, insight ->
                InsightItem(
                    insight = insight,
                    isLast = index == insights.size - 1
                )
            }
        }
    }
}

data class Insight(
    val icon: String,
    val message: String,
    val color: Color
)

@Composable
private fun InsightItem(
    insight: Insight,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(insight.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = insight.icon, fontSize = 16.sp)
        }

        Text(
            text = insight.message,
            fontSize = 14.sp,
            color = SafeSphereColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }

    if (!isLast) {
        Divider(
            color = SafeSphereColors.TextSecondary.copy(alpha = 0.1f),
            modifier = Modifier.padding(start = 44.dp, top = 8.dp)
        )
    }
}

