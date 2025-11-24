package com.runanywhere.startup_hackathon20.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.startup_hackathon20.backup.BackupManager
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Backup & Restore Screen
 *
 * Features:
 * - Create encrypted backups
 * - Restore from backups
 * - Password-protected files
 * - Choose what to backup (passwords, vault, or both)
 * - Merge or replace mode for restore
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: SafeSphereViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val backupManager = remember { BackupManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    var backupPassword by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    var includePasswords by remember { mutableStateOf(true) }
    var includeVault by remember { mutableStateOf(true) }
    var mergeMode by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var operationInProgress by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // File picker for creating backup
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            operationInProgress = true
            scope.launch {
                val result = backupManager.createBackup(
                    password = backupPassword,
                    includePasswords = includePasswords,
                    includeVault = includeVault,
                    outputUri = uri
                )
                operationInProgress = false
                result.fold(
                    onSuccess = { message ->
                        resultMessage = "✅ $message"
                        isError = false
                        backupPassword = ""
                        showBackupDialog = false
                    },
                    onFailure = { error ->
                        resultMessage = "❌ Backup failed: ${error.message}"
                        isError = true
                    }
                )
            }
        }
    }

    // File picker for restoring backup
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            operationInProgress = true
            scope.launch {
                val result = backupManager.restoreBackup(
                    password = restorePassword,
                    inputUri = uri,
                    mergeMode = mergeMode
                )
                operationInProgress = false
                result.fold(
                    onSuccess = { restoreResult ->
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
                        resultMessage = """
                            ✅ Restore successful!
                            • ${restoreResult.passwordsRestored} passwords restored
                            • ${restoreResult.vaultItemsRestored} vault items restored
                            • Backup date: ${dateFormat.format(restoreResult.backupDate)}
                        """.trimIndent()
                        isError = false
                        restorePassword = ""
                        showRestoreDialog = false
                    },
                    onFailure = { error ->
                        resultMessage = "❌ Restore failed: ${error.message}"
                        isError = true
                    }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Backup & Restore",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SafeSphereColors.Surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SafeSphereColors.Background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SafeSphereColors.Primary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💾", fontSize = 40.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Secure Your Data",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeSphereColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Create encrypted backups and restore anytime",
                                fontSize = 13.sp,
                                color = SafeSphereColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // Result message
            if (resultMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isError)
                                Color(0xFFF44336).copy(alpha = 0.1f)
                            else
                                Color(0xFF4CAF50).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = resultMessage!!,
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                color = SafeSphereColors.TextPrimary
                            )
                            IconButton(
                                onClick = { resultMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    "Dismiss",
                                    tint = SafeSphereColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Create Backup Section
            item {
                Text(
                    "📤 Create Backup",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBackupDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = SafeSphereColors.Surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
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
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Export Encrypted Backup",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeSphereColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Save all your data to a secure file",
                                fontSize = 13.sp,
                                color = SafeSphereColors.TextSecondary
                            )
                        }

                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = SafeSphereColors.TextSecondary
                        )
                    }
                }
            }

            // Restore Backup Section
            item {
                Text(
                    "📥 Restore Backup",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.TextPrimary
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRestoreDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = SafeSphereColors.Surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            SafeSphereColors.Accent,
                                            SafeSphereColors.Info
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Import from Backup",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeSphereColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Restore your data from a backup file",
                                fontSize = 13.sp,
                                color = SafeSphereColors.TextSecondary
                            )
                        }

                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = SafeSphereColors.TextSecondary
                        )
                    }
                }
            }

            // Security Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SafeSphereColors.Info.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔒", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Security Features",
                                fontWeight = FontWeight.Bold,
                                color = SafeSphereColors.TextPrimary
                            )
                        }
                        Text(
                            "• Military-grade AES-256 encryption\n" +
                                    "• Password-protected backups\n" +
                                    "• No cloud storage required\n" +
                                    "• Local storage only\n" +
                                    "• Portable across devices",
                            fontSize = 12.sp,
                            color = SafeSphereColors.TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    // Create Backup Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Create Encrypted Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("Backup Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { includePasswords = !includePasswords }
                    ) {
                        Checkbox(
                            checked = includePasswords,
                            onCheckedChange = { includePasswords = it }
                        )
                        Text("Include Passwords")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { includeVault = !includeVault }
                    ) {
                        Checkbox(
                            checked = includeVault,
                            onCheckedChange = { includeVault = it }
                        )
                        Text("Include Privacy Vault")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (backupPassword.length < 8) {
                            resultMessage = "❌ Password must be at least 8 characters"
                            isError = true
                            return@Button
                        }
                        val filename = backupManager.generateBackupFilename()
                        createBackupLauncher.launch(filename)
                    },
                    enabled = backupPassword.isNotEmpty() && !operationInProgress
                ) {
                    if (operationInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Create Backup")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Backup Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore from Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text("Backup Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { mergeMode = !mergeMode }
                    ) {
                        RadioButton(
                            selected = !mergeMode,
                            onClick = { mergeMode = false }
                        )
                        Text("Replace all data")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { mergeMode = !mergeMode }
                    ) {
                        RadioButton(
                            selected = mergeMode,
                            onClick = { mergeMode = true }
                        )
                        Text("Merge with existing")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restorePassword.isEmpty()) {
                            resultMessage = "❌ Please enter backup password"
                            isError = true
                            return@Button
                        }
                        restoreBackupLauncher.launch(arrayOf("*/*"))
                    },
                    enabled = restorePassword.isNotEmpty() && !operationInProgress
                ) {
                    if (operationInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Select Backup File")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
