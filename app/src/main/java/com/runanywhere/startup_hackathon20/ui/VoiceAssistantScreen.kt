package com.runanywhere.startup_hackathon20.ui

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.runanywhere.startup_hackathon20.viewmodels.SafeSphereViewModel
import com.runanywhere.startup_hackathon20.voice.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Voice Assistant Screen
 *
 * Features:
 * - Multi-language voice commands
 * - Real-time speech recognition
 * - Visual feedback
 * - Command history
 * - Language selection
 */
@Composable
fun VoiceAssistantScreen(viewModel: SafeSphereViewModel) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val scope = rememberCoroutineScope()

    // Permission state
    var hasPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            android.util.Log.d("VoiceAssistant", "Microphone permission granted")
        } else {
            android.util.Log.e("VoiceAssistant", "Microphone permission denied")
        }
    }

    // Voice command system - only create if permission granted
    val voiceSystem = remember(hasPermission) {
        if (hasPermission && android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
            VoiceCommandSystem(context)
        } else {
            null
        }
    }

    val isListening by (voiceSystem?.isListening ?: MutableStateFlow(false)).collectAsState()
    val lastCommand by (voiceSystem?.lastCommand ?: MutableStateFlow(null)).collectAsState()
    val voiceResponse by (voiceSystem?.voiceResponse ?: MutableStateFlow(null)).collectAsState()
    val currentLanguage by (voiceSystem?.currentLanguage
        ?: MutableStateFlow(SupportedLanguage.ENGLISH)).collectAsState()
    val isReady by (voiceSystem?.isReady ?: MutableStateFlow(false)).collectAsState()

    // Command history
    val commandHistory = remember { mutableStateListOf<VoiceCommand>() }

    // Show language selector
    var showLanguageSelector by remember { mutableStateOf(false) }

    // Error state
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // TTS data available check
    var ttsDataAvailable by remember { mutableStateOf(true) }

    // Check device capabilities on mount
    LaunchedEffect(Unit) {
        if (!android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
            errorMessage = "Speech recognition is not available on this device"
        } else if (!hasPermission) {
            errorMessage =
                "Microphone permission required. Tap the button below to grant permission."
        }
        
        // Check if TTS data is available
        val ttsIntent = android.content.Intent()
        ttsIntent.action = android.speech.tts.TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        val ttsDataCheck = context.packageManager.queryIntentActivities(ttsIntent, 0)
        android.util.Log.d("VoiceAssistant", "TTS engines available: ${ttsDataCheck.size}")
        
        if (ttsDataCheck.isEmpty()) {
            ttsDataAvailable = false
            errorMessage = "No TTS engine found. Please install Google Text-to-Speech from Play Store."
        }
        
        // Request audio focus to ensure sound can play
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        val currentVolume = audioManager?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        android.util.Log.d("VoiceAssistant", "Media volume: $currentVolume / $maxVolume")
        
        if (currentVolume == 0) {
            errorMessage = "Media volume is muted. Please turn up your device volume."
        }
    }

    // Listen for commands and execute actions
    LaunchedEffect(lastCommand) {
        lastCommand?.let { command ->
            commandHistory.add(0, command)

            // Execute command action
            when (command.action) {
                VoiceAction.OPEN_PASSWORD -> {
                    // Search for password
                    viewModel.navigateToScreen(com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen.PASSWORDS)
                }

                VoiceAction.GENERATE_PASSWORD -> {
                    viewModel.navigateToScreen(com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen.PASSWORDS)
                }

                VoiceAction.CHECK_SECURITY,
                VoiceAction.SECURITY_SCORE -> {
                    viewModel.navigateToScreen(com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen.PASSWORD_HEALTH)
                }

                VoiceAction.AI_PREDICTOR -> {
                    viewModel.navigateToScreen(com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen.AI_PREDICTOR)
                }

                VoiceAction.LIST_PASSWORDS,
                VoiceAction.SEARCH_PASSWORD -> {
                    viewModel.navigateToScreen(com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen.PASSWORDS)
                }

                VoiceAction.VAULT_STATUS -> {
                    viewModel.navigateToScreen(com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen.PRIVACY_VAULT)
                }

                VoiceAction.NAVIGATE_DASHBOARD -> {
                    viewModel.navigateToScreen(com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen.DASHBOARD)
                }

                VoiceAction.NAVIGATE_PASSWORDS -> {
                    viewModel.navigateToScreen(com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen.PASSWORDS)
                }

                VoiceAction.NAVIGATE_VAULT -> {
                    viewModel.navigateToScreen(com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen.PRIVACY_VAULT)
                }

                VoiceAction.NAVIGATE_SETTINGS -> {
                    viewModel.navigateToScreen(com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen.SETTINGS)
                }

                VoiceAction.NAVIGATE_AI_CHAT -> {
                    viewModel.navigateToScreen(com.runanywhere.startup_hackathon20.viewmodels.SafeSphereScreen.AI_CHAT)
                }

                else -> {}
            }
        }
    }
    
    // Auto-start listening when system is ready
    LaunchedEffect(isReady, hasPermission) {
        if (isReady && hasPermission && voiceSystem != null) {
            // Immediately start continuous mode when ready
            android.util.Log.d("VoiceAssistant", "System ready - Starting continuous listening NOW")
            voiceSystem.enableContinuousMode()
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            voiceSystem?.cleanup()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎤",
                        fontSize = 64.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Voice Assistant",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )

                    Text(
                        text = "Just speak naturally - I'm always listening",
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    // System Status
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!hasPermission) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFF9800).copy(alpha = 0.1f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Microphone permission required",
                                fontSize = 13.sp,
                                color = Color(0xFFFF9800),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else if (!android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SafeSphereColors.Error.copy(alpha = 0.1f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "❌",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Speech recognition not available on this device",
                                fontSize = 13.sp,
                                color = SafeSphereColors.Error,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else if (!isReady) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2196F3).copy(alpha = 0.1f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF2196F3)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Initializing voice system...",
                                fontSize = 13.sp,
                                color = Color(0xFF2196F3)
                            )
                        }
                    } else if (!ttsDataAvailable) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SafeSphereColors.Error.copy(alpha = 0.1f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "❌",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "No TTS engine found. Please install Google Text-to-Speech from Play Store.",
                                fontSize = 13.sp,
                                color = SafeSphereColors.Error,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.tts")
                                    )
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Install TTS")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SafeSphereColors.Success.copy(alpha = 0.1f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✅",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Voice system ready",
                                fontSize = 13.sp,
                                color = SafeSphereColors.Success
                            )
                        }
                    }
                }
            }

            // Permission Request Button (if needed)
            if (!hasPermission) {
                item {
                    GlassButton(
                        text = "Grant Microphone Permission",
                        onClick = {
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        },
                        primary = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Language Selector (only show if system is ready)
            if (hasPermission && android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLanguageSelector = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentLanguage.flag,
                                    fontSize = 32.sp
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = "Language",
                                        fontSize = 12.sp,
                                        color = SafeSphereColors.TextSecondary
                                    )
                                    Text(
                                        text = currentLanguage.displayName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SafeSphereColors.TextPrimary
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = "Change Language",
                                tint = SafeSphereColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Voice Visualization Card
                item {
                    VoiceVisualizationCard(
                        isListening = isListening,
                        voiceResponse = voiceResponse,
                        isReady = isReady,
                        onStartListening = {
                            // Manual start listening
                            android.util.Log.d("VoiceAssistant", "Manual start listening clicked")
                            if (voiceSystem != null && isReady) {
                                voiceSystem.startListening()
                            }
                        },
                        onStopListening = {
                            voiceSystem?.stopListening()
                        }
                    )
                }

                // Available Commands
                item {
                    AvailableCommandsCard(currentLanguage)
                }

                // Command History
                if (commandHistory.isNotEmpty()) {
                    item {
                        Text(
                            text = "Command History",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeSphereColors.TextPrimary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(commandHistory.take(10)) { command ->
                        CommandHistoryItem(command)
                    }
                }
            }
        }

        // Language Selector Dialog
        if (showLanguageSelector) {
            LanguageSelectorDialog(
                currentLanguage = currentLanguage,
                onLanguageSelected = { language ->
                    voiceSystem?.setLanguage(language)
                    showLanguageSelector = false
                },
                onDismiss = { showLanguageSelector = false }
            )
        }
    }
}

/**
 * Voice Visualization Card
 */
@Composable
fun VoiceVisualizationCard(
    isListening: Boolean,
    voiceResponse: String?,
    isReady: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    // Animated scale for pulse effect
    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Microphone Button with Animation
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(if (isListening) scale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (isListening) {
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE91E63),
                                    Color(0xFF9C27B0)
                                )
                            )
                        } else {
                            Brush.radialGradient(
                                colors = listOf(
                                    SafeSphereColors.Primary,
                                    SafeSphereColors.Secondary
                                )
                            )
                        }
                    )
                    .clickable {
                        if (isListening) onStopListening() else onStartListening()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isListening) {
                    // Stop icon (square)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White)
                    )
                } else {
                    // Microphone text
                    Text(
                        text = "🎤",
                        fontSize = 48.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status Text
            Text(
                text = if (isListening) "🎤 Listening for your command..." else "⏸️ Processing...",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isListening) Color(0xFFE91E63) else SafeSphereColors.TextPrimary
            )

            // Voice Response
            voiceResponse?.let { response ->
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SafeSphereColors.Primary.copy(alpha = 0.1f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = response,
                        fontSize = 14.sp,
                        color = SafeSphereColors.TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Available Commands Card
 */
@Composable
fun AvailableCommandsCard(language: SupportedLanguage) {
    var isExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "💡",
                        fontSize = 24.sp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Available Commands",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeSphereColors.TextPrimary
                    )
                }

                Text(
                    text = if (isExpanded) "-" else "+",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeSphereColors.Primary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CommandExampleItem(
                        icon = "🔓",
                        title = "Open Password",
                        examples = language.commandPatterns
                            .find { it.action == VoiceAction.OPEN_PASSWORD }
                            ?.patterns?.take(2) ?: emptyList()
                    )

                    CommandExampleItem(
                        icon = "🔐",
                        title = "Generate Password",
                        examples = language.commandPatterns
                            .find { it.action == VoiceAction.GENERATE_PASSWORD }
                            ?.patterns?.take(2) ?: emptyList()
                    )

                    CommandExampleItem(
                        icon = "🛡️",
                        title = "Check Security",
                        examples = language.commandPatterns
                            .find { it.action == VoiceAction.CHECK_SECURITY }
                            ?.patterns?.take(2) ?: emptyList()
                    )

                    CommandExampleItem(
                        icon = "🤖",
                        title = "AI Predictor",
                        examples = language.commandPatterns
                            .find { it.action == VoiceAction.AI_PREDICTOR }
                            ?.patterns?.take(2) ?: emptyList()
                    )

                    CommandExampleItem(
                        icon = "📋",
                        title = "List Passwords",
                        examples = language.commandPatterns
                            .find { it.action == VoiceAction.LIST_PASSWORDS }
                            ?.patterns?.take(2) ?: emptyList()
                    )
                }
            }
        }
    }
}

/**
 * Command Example Item
 */
@Composable
fun CommandExampleItem(
    icon: String,
    title: String,
    examples: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SafeSphereColors.Surface.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SafeSphereColors.TextPrimary
            )
        }

        examples.forEach { example ->
            Text(
                text = "• \"$example\"",
                fontSize = 12.sp,
                color = SafeSphereColors.TextSecondary,
                modifier = Modifier.padding(start = 28.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Command History Item
 */
@Composable
fun CommandHistoryItem(command: VoiceCommand) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (command.action) {
                            VoiceAction.OPEN_PASSWORD -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            VoiceAction.GENERATE_PASSWORD -> Color(0xFF2196F3).copy(alpha = 0.2f)
                            VoiceAction.CHECK_SECURITY -> Color(0xFFFF9800).copy(alpha = 0.2f)
                            VoiceAction.AI_PREDICTOR -> Color(0xFF9C27B0).copy(alpha = 0.2f)
                            else -> SafeSphereColors.Primary.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (command.action) {
                        VoiceAction.OPEN_PASSWORD -> "🔓"
                        VoiceAction.GENERATE_PASSWORD -> "🔐"
                        VoiceAction.CHECK_SECURITY -> "🛡️"
                        VoiceAction.AI_PREDICTOR -> "🤖"
                        VoiceAction.LIST_PASSWORDS -> "📋"
                        VoiceAction.SEARCH_PASSWORD -> "🔍"
                        VoiceAction.SECURITY_SCORE -> "📊"
                        VoiceAction.LOCK_APP -> "🔒"
                        VoiceAction.NAVIGATE_DASHBOARD -> "🏠"
                        VoiceAction.NAVIGATE_PASSWORDS -> "🗝️"
                        VoiceAction.NAVIGATE_VAULT -> "🔐"
                        VoiceAction.NAVIGATE_SETTINGS -> "⚙️"
                        VoiceAction.NAVIGATE_AI_CHAT -> "💬"
                        VoiceAction.HELP -> "❓"
                        else -> "💬"
                    },
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = command.rawText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = SafeSphereColors.TextPrimary
                )

                Text(
                    text = command.action.name.replace("_", " ").lowercase()
                        .replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    color = SafeSphereColors.TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Executed",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Language Selector Dialog
 */
@Composable
fun LanguageSelectorDialog(
    currentLanguage: SupportedLanguage,
    onLanguageSelected: (SupportedLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Language",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SupportedLanguage.values().toList()) { language ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (language == currentLanguage) SafeSphereColors.Primary.copy(
                                    alpha = 0.15f
                                )
                                else Color.Transparent
                            )
                            .clickable { onLanguageSelected(language) }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = language.flag,
                                fontSize = 28.sp
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = language.displayName,
                                fontSize = 16.sp,
                                fontWeight = if (language == currentLanguage) FontWeight.Bold else FontWeight.Normal,
                                color = if (language == currentLanguage) SafeSphereColors.Primary else SafeSphereColors.TextPrimary
                            )

                            if (language == currentLanguage) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = SafeSphereColors.Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = SafeSphereColors.Surface
    )
}
