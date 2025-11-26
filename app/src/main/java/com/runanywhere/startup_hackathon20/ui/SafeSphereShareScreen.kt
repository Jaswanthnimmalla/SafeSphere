package com.runanywhere.startup_hackathon20.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.runanywhere.startup_hackathon20.share.*
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * SafeSphere Share Screen - Nearby Share style offline file sharing
 *
 * Features:
 * - Device discovery with signal strength
 * - File selection (photos, videos, documents, apps)
 * - Real-time transfer progress with speed/ETA
 * - Transfer history
 * - Beautiful Material 3 design
 */
@Composable
fun SafeSphereShareScreen(
    viewModel: SafeSphereViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = SafeSphereThemeColors

    // Repository
    val repository = remember { SafeSphereShareRepository.getInstance(context) }

    // State
    val discoveredDevices by repository.discoveredDevices.collectAsState()
    val selectedFiles by repository.selectedFiles.collectAsState()
    val currentTransfer by repository.currentTransfer.collectAsState()
    val connectionState by repository.connectionState.collectAsState()
    val transferHistory by repository.transferHistory.collectAsState()
    val errorMessage by repository.errorMessage.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showFilePickerOptions by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(false) }
    var showLocationPrompt by remember { mutableStateOf(false) }

    // Show error message as snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            repository.clearError()
        }
    }

    // Required permissions based on Android version
    val requiredPermissions = remember {
        buildList {
            // Location is required for WiFi Direct on all Android versions
            add(Manifest.permission.ACCESS_FINE_LOCATION)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                // Android 12 and below
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionsGranted = allGranted

        if (allGranted) {
            viewModel.addNotification(
                title = "✅ Permissions Granted",
                message = "SafeSphere Share is ready to discover nearby devices",
                type = com.runanywhere.startup_hackathon20.viewmodels.NotificationType.SUCCESS,
                category = com.runanywhere.startup_hackathon20.viewmodels.NotificationCategory.SYSTEM
            )
        } else {
            showPermissionRationale = true
        }
    }

    // Location settings launcher
    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showLocationPrompt = false
        }
    }

    // Check permissions on start
    LaunchedEffect(Unit) {
        val hasAllPermissions = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
        permissionsGranted = hasAllPermissions

        if (!hasAllPermissions) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }

        val locationManager = context.getSystemService(LocationManager::class.java)
        if (!locationManager.isLocationEnabled) {
            showLocationPrompt = true
        }
    }

    // Cleanup on exit
    DisposableEffect(Unit) {
        onDispose {
            repository.stopDiscovery()
        }
    }

    // File picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            repository.selectFiles(uris)
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "SafeSphere Share",
                onBackClick = onNavigateBack
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Show permission rationale if needed
            if (!permissionsGranted && showPermissionRationale) {
                PermissionRationaleCard(
                    onRequestPermissions = {
                        permissionLauncher.launch(requiredPermissions.toTypedArray())
                        showPermissionRationale = false
                    },
                    onOpenSettings = {
                        val intent =
                            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.fromParts("package", context.packageName, null)
                        context.startActivity(intent)
                    }
                )
            }

            // Show location prompt if needed
            if (showLocationPrompt) {
                LocationPromptCard(
                    onEnableLocation = {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        locationSettingsLauncher.launch(intent)
                    }
                )
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = colors.surface,
                contentColor = colors.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Send") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Receive") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("History") }
                )
            }

            // Tab content - only show if permissions granted
            if (permissionsGranted && !showLocationPrompt) {
                when (selectedTab) {
                    0 -> SendTab(
                        selectedFiles = selectedFiles,
                        discoveredDevices = discoveredDevices,
                        connectionState = connectionState,
                        currentTransfer = currentTransfer,
                        repository = repository,
                        onSelectFiles = { filePickerLauncher.launch("*/*") },
                        onClearFiles = { repository.clearSelectedFiles() }
                    )

                    1 -> ReceiveTab(
                        connectionState = connectionState,
                        currentTransfer = currentTransfer,
                        repository = repository
                    )

                    2 -> HistoryTab(
                        history = transferHistory,
                        repository = repository
                    )
                }
            } else {
                // Show permission required message
                PermissionRequiredScreen(
                    onRequestPermissions = {
                        permissionLauncher.launch(requiredPermissions.toTypedArray())
                    }
                )
            }
        }
    }

    // File picker options dialog
    if (showFilePickerOptions) {
        FilePickerOptionsDialog(
            onDismiss = { showFilePickerOptions = false },
            onPickFiles = {
                filePickerLauncher.launch("*/*")
                showFilePickerOptions = false
            },
            onPickImages = {
                filePickerLauncher.launch("image/*")
                showFilePickerOptions = false
            },
            onPickVideos = {
                filePickerLauncher.launch("video/*")
                showFilePickerOptions = false
            }
        )
    }
}

/**
 * Permission Rationale Card
 */
@Composable
fun PermissionRationaleCard(
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = SafeSphereThemeColors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.warning.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⚠️", fontSize = 24.sp)
                Text(
                    text = "Permissions Required",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.warning
                )
            }

            Text(
                text = "SafeSphere Share needs permissions to:",
                fontSize = 14.sp,
                color = colors.textPrimary
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PermissionItem("📡", "Discover nearby devices via Wi-Fi Direct")
                PermissionItem("📂", "Access files you want to share")
                PermissionItem("📥", "Receive files from nearby devices")
            }

            Text(
                text = "Your privacy: All transfers happen offline, peer-to-peer. No internet or cloud involved.",
                fontSize = 12.sp,
                color = colors.textSecondary,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Open Settings")
                }

                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary
                    )
                ) {
                    Text("Grant Permissions")
                }
            }
        }
    }
}

/**
 * Location Prompt Card
 */
@Composable
fun LocationPromptCard(onEnableLocation: () -> Unit) {
    val colors = SafeSphereThemeColors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.warning.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📍", fontSize = 24.sp)
                Text(
                    text = "Location Required",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.warning
                )
            }

            Text(
                text = "Total: ${repository.formatFileSize(totalSize)}",
                fontSize = 14.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // File list
            files.take(3).forEach { file ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(text = getFileIcon(file.fileType), fontSize = 20.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            fontSize = 14.sp,
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = repository.formatFileSize(file.size),
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            if (files.size > 3) {
                Text(
                    text = "+ ${files.size - 3} more files",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Device card
 */
@Composable
fun DeviceCard(device: NearbyDevice, onClick: () -> Unit) {
    val colors = SafeSphereThemeColors

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Device icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "📱", fontSize = 24.sp)
                }

                Column {
                    Text(
                        text = device.deviceName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )

                    // Signal strength indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(5) { index ->
                            Box(
                                modifier = Modifier
                                    .size(width = 4.dp, height = ((index + 1) * 3).dp)
                                    .background(
                                        if (index < device.signalStrength / 20) colors.success
                                        else colors.textSecondary.copy(alpha = 0.3f)
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = device.distance.displayName,
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            // Send button
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary
                )
            ) {
                Text("Enable Location")
            }
        }
    }
}

/**
 * Transfer progress card
 */
@Composable
fun TransferProgressCard(
    transfer: TransferSession,
    repository: SafeSphereShareRepository,
    onCancel: () -> Unit
) {
    val colors = SafeSphereThemeColors
    val deviceName = (transfer.receiver?.deviceName ?: transfer.sender?.deviceName) ?: "Unknown"

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = if (transfer.isSender) "Sending to $deviceName" else "Receiving from $deviceName",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current file
            if (transfer.files.isNotEmpty()) {
                val currentFile = transfer.files.getOrNull(transfer.currentFileIndex)
                if (currentFile != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = getFileIcon(currentFile.fileType), fontSize = 28.sp)
                        Text(
                            text = currentFile.name,
                            fontSize = 14.sp,
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = transfer.progress,
                modifier = Modifier.fillMaxWidth(),
                color = colors.primary,
                trackColor = colors.textSecondary.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(transfer.progress * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Text(
                    text = "${repository.formatFileSize(transfer.bytesTransferred)} / ${
                        repository.formatFileSize(
                            transfer.totalBytes
                        )
                    }",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "⚡ ${repository.formatSpeed(transfer.speed)}",
                    fontSize = 12.sp,
                    color = colors.success
                )

                val eta = if (transfer.speed > 0) {
                    val remaining = transfer.totalBytes - transfer.bytesTransferred
                    remaining / transfer.speed
                } else 0L

                Text(
                    text = "⏱ ${eta}s remaining",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel Transfer")
            }
        }
    }
}

/**
 * History item card
 */
@Composable
fun HistoryItemCard(item: ShareHistoryItem, repository: SafeSphereShareRepository) {
    val colors = SafeSphereThemeColors
    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (item.status) {
                            TransferStatus.COMPLETED -> colors.success.copy(alpha = 0.2f)
                            TransferStatus.FAILED -> colors.error.copy(alpha = 0.2f)
                            else -> colors.textSecondary.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (item.isSent) "📤" else "📥",
                    fontSize = 24.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (item.isSent) "Sent to ${item.deviceName}" else "Received from ${item.deviceName}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                Text(
                    text = "${item.files.size} file(s) • ${repository.formatFileSize(item.totalSize)}",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )

                Text(
                    text = dateFormat.format(Date(item.timestamp)),
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }

            // Status
            Text(
                text = when (item.status) {
                    TransferStatus.COMPLETED -> "✅"
                    TransferStatus.FAILED -> "❌"
                    else -> "⏳"
                },
                fontSize = 20.sp
            )
        }
    }
}

/**
 * File picker options dialog
 */
@Composable
fun FilePickerOptionsDialog(
    onDismiss: () -> Unit,
    onPickFiles: () -> Unit,
    onPickImages: () -> Unit,
    onPickVideos: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Files") },
        text = {
            Column {
                TextButton(onClick = onPickFiles) {
                    Text("📁 All Files")
                }
                TextButton(onClick = onPickImages) {
                    Text("📷 Photos")
                }
                TextButton(onClick = onPickVideos) {
                    Text("🎥 Videos")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Top bar
 */
@Composable
fun TopBar(title: String, onBackClick: () -> Unit) {
    val colors = SafeSphereThemeColors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.primary)
            }

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }
    }
}

/**
 * Get file icon based on type
 */
fun getFileIcon(fileType: FileType): String {
    return when (fileType) {
        FileType.IMAGE -> "📷"
        FileType.VIDEO -> "🎥"
        FileType.DOCUMENT -> "📄"
        FileType.APK -> "📦"
        FileType.AUDIO -> "🎵"
        FileType.OTHER -> "📁"
    }
}
