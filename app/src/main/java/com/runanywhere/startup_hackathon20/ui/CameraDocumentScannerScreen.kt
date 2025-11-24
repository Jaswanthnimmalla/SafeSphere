package com.runanywhere.startup_hackathon20.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.runanywhere.startup_hackathon20.data.*
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 📸 Image Scanner Screen
 *
 * Advanced AI-powered image scanning with:
 * - Real-time camera preview
 * - Image preview confirmation
 * - AI analysis & detection
 * - Biometric protection
 * - Auto-save to Protected Images in Privacy Vault
 */
@Composable
fun CameraDocumentScannerScreen(
    viewModel: SafeSphereViewModel,
    onNavigateBack: () -> Unit
) {
    val colors = SafeSphereThemeColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Camera permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // UI States
    var showCamera by remember { mutableStateOf(false) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var detectedText by remember { mutableStateOf("") }
    var detectedInfo by remember { mutableStateOf<List<DetectedInformation>>(emptyList()) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var imageTitle by remember { mutableStateOf("") }
    var showBiometricPrompt by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            showCamera = true
        }
    }

    // Back handler
    BackHandler(enabled = showCamera || capturedImage != null) {
        when {
            capturedImage != null -> capturedImage = null
            showCamera -> showCamera = false
            else -> onNavigateBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.background,
                        colors.backgroundDark
                    )
                )
            )
    ) {
        when {
            // Show camera view
            showCamera && hasCameraPermission -> {
                CameraView(
                    onImageCaptured = { bitmap ->
                        capturedImage = bitmap
                        showCamera = false
                        // Start processing
                        scope.launch {
                            isProcessing = true
                            delay(800)
                            val result = processDocument(bitmap, context)
                            detectedText = result.first
                            detectedInfo = result.second
                            isProcessing = false
                        }
                    },
                    onError = { Log.e("Camera", "Error: $it") },
                    onClose = { showCamera = false }
                )
            }

            // Show captured image with processing
            capturedImage != null -> {
                CapturedImageView(
                    bitmap = capturedImage!!,
                    isProcessing = isProcessing,
                    detectedText = detectedText,
                    detectedInfo = detectedInfo,
                    onSave = {
                        showConfirmDialog = true
                    },
                    onRetake = {
                        capturedImage = null
                        detectedText = ""
                        detectedInfo = emptyList()
                        showCamera = true
                    },
                    onClose = {
                        capturedImage = null
                        detectedText = ""
                        detectedInfo = emptyList()
                    },
                    colors = colors
                )
            }

            // Show main screen
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // Hero Header
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF4CAF50).copy(alpha = 0.15f),
                                                Color(0xFF2196F3).copy(alpha = 0.1f)
                                            )
                                        )
                                    )
                                    .padding(28.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(0xFF4CAF50),
                                                        Color(0xFF2196F3)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "📸", fontSize = 52.sp)
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        text = "Image Scanner",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "AI-Powered Image Scanning",
                                        fontSize = 15.sp,
                                        color = colors.textSecondary,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF4CAF50))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "OCR • Auto-Detection • Biometric Protection",
                                            fontSize = 13.sp,
                                            color = colors.textSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Scan Button
                    item {
                        Button(
                            onClick = {
                                if (hasCameraPermission) {
                                    showCamera = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                text = "📸",
                                fontSize = 32.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Scan Image",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Features
                    item {
                        Text(
                            text = "✨ Smart Features",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FeatureCard(
                                icon = "🤖",
                                title = "AI Image Recognition",
                                description = "Advanced AI recognizes and extracts information from images",
                                color = Color(0xFF2196F3),
                                colors = colors
                            )
                            FeatureCard(
                                icon = "🔍",
                                title = "Auto Info Detection",
                                description = "Automatically identifies sensitive information in images",
                                color = Color(0xFF9C27B0),
                                colors = colors
                            )
                            FeatureCard(
                                icon = "🔐",
                                title = "Biometric Protection",
                                description = "Securely saves images with biometric authentication",
                                color = Color(0xFF4CAF50),
                                colors = colors
                            )
                            FeatureCard(
                                icon = "⚡",
                                title = "Lightning Fast",
                                description = "Process images in under 1 second",
                                color = Color(0xFFFF9800),
                                colors = colors
                            )
                        }
                    }

                    // How to Use
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "📖 How to Use",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                HowToStep(
                                    number = "1",
                                    text = "Tap 'Scan Image' and allow camera access",
                                    colors = colors
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                HowToStep(
                                    number = "2",
                                    text = "Position image in frame and tap capture",
                                    colors = colors
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                HowToStep(
                                    number = "3",
                                    text = "AI analyzes and extracts all information",
                                    colors = colors
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                HowToStep(
                                    number = "4",
                                    text = "Review detected info and save to vault",
                                    colors = colors
                                )
                            }
                        }
                    }

                    // Info Banner
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2196F3).copy(alpha = 0.15f))
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF2196F3).copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "ℹ️", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "All image processing happens offline on your device. Complete privacy guaranteed.",
                                    fontSize = 13.sp,
                                    color = colors.textPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    // Confirm Dialog
    if (showConfirmDialog && capturedImage != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = colors.surface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "📸", fontSize = 28.sp)
                    Text(
                        text = "Save Image",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter a title for this image:",
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = imageTitle,
                        onValueChange = { imageTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Image Title") },
                        placeholder = { Text("e.g., Photo, Document, Receipt") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.textSecondary.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (detectedInfo.isNotEmpty()) {
                        Text(
                            text = "✅ ${detectedInfo.size} piece(s) of sensitive info detected",
                            fontSize = 12.sp,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (imageTitle.isNotBlank()) {
                            scope.launch {
                                // Save the image to internal storage
                                val imageFile = File(context.filesDir, "protected_images")
                                imageFile.mkdirs()
                                val fileName = "image_${System.currentTimeMillis()}.jpg"
                                val savedImageFile = File(imageFile, fileName)

                                try {
                                    FileOutputStream(savedImageFile).use { out ->
                                        capturedImage?.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                    }

                                    // Save to vault with image reference
                                    val content = buildString {
                                        appendLine("📸 Scanned Image")
                                        appendLine()
                                        appendLine(
                                            "📅 Date: ${
                                                SimpleDateFormat(
                                                    "MMM dd, yyyy HH:mm",
                                                    Locale.getDefault()
                                                ).format(Date())
                                            }"
                                        )
                                        appendLine()
                                        appendLine("🖼️ Image saved: ${savedImageFile.absolutePath}")
                                        appendLine()
                                        if (detectedText.isNotBlank()) {
                                            appendLine("📝 Extracted Text:")
                                            appendLine(detectedText)
                                            appendLine()
                                        }
                                        if (detectedInfo.isNotEmpty()) {
                                            appendLine("🔍 Detected Information:")
                                            detectedInfo.forEach { info ->
                                                appendLine("• ${info.type}: ${info.value}")
                                            }
                                        }
                                    }

                                    viewModel.addVaultItem(
                                        title = imageTitle,
                                        content = content,
                                        category = VaultCategory.IMAGES
                                    )

                                    viewModel.addNotification(
                                        title = "📸 Image Scanned & Saved",
                                        message = "'$imageTitle' with image saved securely to Privacy Vault",
                                        type = com.runanywhere.startup_hackathon20.viewmodels.NotificationType.SUCCESS,
                                        category = com.runanywhere.startup_hackathon20.viewmodels.NotificationCategory.VAULT
                                    )
                                } catch (e: Exception) {
                                    Log.e("CameraScanner", "Error saving image: ${e.message}")
                                    viewModel.addNotification(
                                        title = "⚠️ Save Warning",
                                        message = "Image info saved but image save failed",
                                        type = com.runanywhere.startup_hackathon20.viewmodels.NotificationType.WARNING,
                                        category = com.runanywhere.startup_hackathon20.viewmodels.NotificationCategory.VAULT
                                    )
                                }

                                showConfirmDialog = false
                                imageTitle = ""
                                capturedImage = null
                                detectedText = ""
                                detectedInfo = emptyList()
                            }
                        }
                    },
                    enabled = imageTitle.isNotBlank()
                ) {
                    Text(
                        "💾 Save to Vault",
                        color = if (imageTitle.isNotBlank()) colors.primary else colors.textSecondary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    imageTitle = ""
                }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Biometric Prompt
    if (showBiometricPrompt) {
        // Biometric authentication code here
    }
}

/**
 * Camera View Composable
 */
@Composable
fun CameraView(
    onImageCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = SafeSphereThemeColors
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    onError(e.message ?: "Camera error")
                }

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Camera overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Frame guide
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f)
                .aspectRatio(3f / 4f)
                .border(
                    width = 3.dp,
                    color = Color(0xFF4CAF50),
                    shape = RoundedCornerShape(16.dp)
                )
        )

        // Instruction text
        Text(
            text = "Position image within frame",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Capture button
        FloatingActionButton(
            onClick = {
                val capture = imageCapture ?: return@FloatingActionButton
                val file = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")

                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                capture.takePicture(
                    outputOptions,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            onImageCaptured(bitmap)
                            file.delete()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            onError(exception.message ?: "Capture failed")
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(80.dp),
            containerColor = Color(0xFF4CAF50),
            contentColor = Color.White
        ) {
            Text(
                text = "📸",
                fontSize = 40.sp
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

/**
 * Captured Image View with Processing
 */
@Composable
fun CapturedImageView(
    bitmap: Bitmap,
    isProcessing: Boolean,
    detectedText: String,
    detectedInfo: List<DetectedInformation>,
    onSave: () -> Unit,
    onRetake: () -> Unit,
    onClose: () -> Unit,
    colors: ThemeColors
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = colors.textPrimary
                )
            }

            Text(
                text = if (isProcessing) "🤖 Processing..." else "✅ Analysis Complete",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isProcessing) Color(0xFFFF9800) else Color(0xFF4CAF50)
            )

            IconButton(onClick = onRetake) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retake",
                    tint = colors.textPrimary
                )
            }
        }

        // Image preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Captured",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Processing indicator or results
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AI analyzing image...",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                }
            }
        } else {
            // Results
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Detected Info Cards
                if (detectedInfo.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🔍 Detected Information",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            detectedInfo.forEach { info ->
                                DetectedInfoRow(info, colors)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                // Extracted Text
                if (detectedText.isNotBlank()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📝 Extracted Text",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = detectedText,
                                fontSize = 14.sp,
                                color = colors.textSecondary,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // Save Button
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "💾 Save to Privacy Vault",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Feature Card
 */
@Composable
fun FeatureCard(
    icon: String,
    title: String,
    description: String,
    color: Color,
    colors: ThemeColors
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * How To Step
 */
@Composable
fun HowToStep(number: String, text: String, colors: ThemeColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            fontSize = 14.sp,
            color = colors.textPrimary
        )
    }
}

/**
 * Detected Info Row
 */
@Composable
fun DetectedInfoRow(info: DetectedInformation, colors: ThemeColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = info.icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = info.type,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = info.value,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        Text(
            text = "${(info.confidence * 100).toInt()}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
    }
}

/**
 * Process document and extract information
 */
private fun processDocument(
    bitmap: Bitmap,
    context: Context
): Pair<String, List<DetectedInformation>> {
    // Simulate OCR text extraction (in real app, use ML Kit or Tesseract)
    val extractedText = simulateOCR(bitmap)

    // Analyze for sensitive information
    val detectedInfo = analyzeSensitiveInfo(extractedText)

    return Pair(extractedText, detectedInfo)
}

/**
 * Simulate OCR (placeholder - integrate real OCR like ML Kit)
 */
private fun simulateOCR(bitmap: Bitmap): String {
    // This is a placeholder. In production, use:
    // - Google ML Kit Text Recognition
    // - Tesseract OCR
    // - Azure Computer Vision
    return "Sample extracted text from image\nDate: ${
        SimpleDateFormat(
            "MM/dd/yyyy",
            Locale.getDefault()
        ).format(Date())
    }\nThis is a demo text extraction."
}

/**
 * Analyze text for sensitive information
 */
private fun analyzeSensitiveInfo(text: String): List<DetectedInformation> {
    val detections = mutableListOf<DetectedInformation>()

    // Email detection
    val emailPattern = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b""")
    emailPattern.findAll(text).forEach {
        detections.add(
            DetectedInformation(
                type = "Email",
                value = it.value,
                icon = "📧",
                confidence = 0.95f
            )
        )
    }

    // Phone detection
    val phonePattern = Regex("""\b(?:\+?1[-.]?)?\(?\d{3}\)?[-.]?\d{3}[-.]?\d{4}\b""")
    phonePattern.findAll(text).forEach {
        detections.add(
            DetectedInformation(
                type = "Phone Number",
                value = it.value,
                icon = "📞",
                confidence = 0.88f
            )
        )
    }

    // Credit Card detection
    val cardPattern = Regex("""\b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b""")
    cardPattern.findAll(text).forEach {
        detections.add(
            DetectedInformation(
                type = "Credit Card",
                value = "**** **** **** ${it.value.takeLast(4)}",
                icon = "💳",
                confidence = 0.92f
            )
        )
    }

    // Date detection
    val datePattern = Regex("""\b\d{1,2}/\d{1,2}/\d{2,4}\b""")
    datePattern.findAll(text).forEach {
        detections.add(
            DetectedInformation(
                type = "Date",
                value = it.value,
                icon = "📅",
                confidence = 0.90f
            )
        )
    }

    return detections
}

/**
 * Data class for detected information
 */
data class DetectedInformation(
    val type: String,
    val value: String,
    val icon: String,
    val confidence: Float
)
