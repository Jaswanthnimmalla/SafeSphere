package com.runanywhere.startup_hackathon20.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

/**
 * Advanced Voice Control System
 * 
 * Features:
 * - Speech-to-Text with continuous listening
 * - Text-to-Speech feedback
 * - Advanced NLP processing
 * - Visual feedback and confidence scores
 * - Wake word detection
 * - Conversation management
 */
class AdvancedVoiceControlSystem(private val context: Context) {

    private val nlpEngine = AdvancedNLPEngine(context)
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTTSReady = false

    // State flows
    private val _listeningState = MutableStateFlow(AdvancedListeningState.IDLE)
    val listeningState: StateFlow<AdvancedListeningState> = _listeningState

    private val _lastCommand = MutableStateFlow<VoiceCommandWrapper?>(null)
    val lastCommand: StateFlow<VoiceCommandWrapper?> = _lastCommand

    private val _partialResults = MutableStateFlow("")
    val partialResults: StateFlow<String> = _partialResults

    private val _feedback = MutableStateFlow<AdvancedVoiceFeedback?>(null)
    val feedback: StateFlow<AdvancedVoiceFeedback?> = _feedback

    private val _nlpAnalysis = MutableStateFlow<NLPAnalysisDisplay?>(null)
    val nlpAnalysis: StateFlow<NLPAnalysisDisplay?> = _nlpAnalysis

    // Wake words
    private val wakeWords = listOf(
        "hey safesphere",
        "ok safesphere",
        "safesphere",
        "hey sphere"
    )

    private var isWakeWordMode = false
    private var isEnabled = false

    init {
        initializeTTS()
    }

    /**
     * Initialize Text-to-Speech
     */
    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setSpeechRate(1.0f)
                textToSpeech?.setPitch(1.0f)
                isTTSReady = true

                // Set utterance progress listener
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        android.util.Log.d("AdvancedVoice", "TTS started: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        android.util.Log.d("AdvancedVoice", "TTS completed: $utteranceId")
                        // Resume listening after TTS completes
                        if (isEnabled && utteranceId == "feedback") {
                            startListening()
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        android.util.Log.e("AdvancedVoice", "TTS error: $utteranceId")
                    }
                })

                android.util.Log.d("AdvancedVoice", "TTS initialized successfully")
            } else {
                android.util.Log.e("AdvancedVoice", "TTS initialization failed")
            }
        }
    }

    /**
     * Enable voice control with wake word mode
     */
    fun enable(wakeWordMode: Boolean = false) {
        isEnabled = true
        isWakeWordMode = wakeWordMode
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _feedback.value = AdvancedVoiceFeedback(
                message = "Speech recognition not available",
                type = AdvancedFeedbackType.ERROR
            )
            return
        }

        startListening()
        speak("Voice control enabled")
    }

    /**
     * Disable voice control
     */
    fun disable() {
        isEnabled = false
        stopListening()
        speak("Voice control disabled")
    }

    /**
     * Start listening for voice input
     */
    fun startListening() {
        if (!isEnabled) {
            android.util.Log.d("AdvancedVoice", "Cannot start listening - voice control disabled")
            return
        }

        android.util.Log.d("AdvancedVoice", "Starting listening...")

        stopListening() // Stop any existing listener

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        if (speechRecognizer == null) {
            android.util.Log.e("AdvancedVoice", "Failed to create SpeechRecognizer")
            _feedback.value = AdvancedVoiceFeedback(
                message = "Speech recognition not available on this device",
                type = AdvancedFeedbackType.ERROR
            )
            return
        }

        speechRecognizer?.setRecognitionListener(recognitionListener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                2000L
            )
        }

        try {
            speechRecognizer?.startListening(intent)
            _listeningState.value = AdvancedListeningState.LISTENING
            android.util.Log.d(
                "AdvancedVoice",
                "Speech recognizer started successfully - state set to LISTENING"
            )
        } catch (e: Exception) {
            android.util.Log.e("AdvancedVoice", "Failed to start listening: ${e.message}")
            _listeningState.value = AdvancedListeningState.IDLE
            _feedback.value = AdvancedVoiceFeedback(
                message = "Failed to start voice recognition",
                type = AdvancedFeedbackType.ERROR
            )
        }
    }

    /**
     * Stop listening
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _listeningState.value = AdvancedListeningState.IDLE
        _partialResults.value = ""
    }

    /**
     * Process voice input with NLP
     */
    private fun processVoiceInput(text: String) {
        android.util.Log.d("AdvancedVoice", "Processing input: $text")

        // Check for wake word if in wake word mode
        if (isWakeWordMode) {
            val hasWakeWord = wakeWords.any { text.lowercase().contains(it) }
            if (!hasWakeWord) {
                android.util.Log.d("AdvancedVoice", "No wake word detected, ignoring")
                // Continue listening for wake word
                if (isEnabled) {
                    startListening()
                }
                return
            }
            
            // Remove wake word from command
            var command = text.lowercase()
            wakeWords.forEach { wakeWord ->
                command = command.replace(wakeWord, "").trim()
            }
            
            if (command.isEmpty()) {
                speak("Yes? I'm listening")
                startListening()
                return
            }
            
            processCommand(command)
        } else {
            processCommand(text)
        }
    }

    /**
     * Process command with NLP engine
     */
    private fun processCommand(text: String) {
        _listeningState.value = AdvancedListeningState.PROCESSING

        // Parse with NLP engine
        val nlpResult = nlpEngine.parseCommand(text)

        // Create visual NLP analysis display
        _nlpAnalysis.value = NLPAnalysisDisplay(
            input = nlpResult.originalInput,
            intent = nlpResult.intent.toString().replace("_", " ").lowercase()
                .replaceFirstChar { it.uppercase() },
            confidence = nlpResult.confidence,
            entities = nlpResult.entities.flatMap { (type, values) ->
                values.map { EntityDisplay(type.toString(), it) }
            },
            sentiment = nlpResult.sentiment.sentiment.toString(),
            urgency = nlpResult.sentiment.urgency.toString(),
            contextUsed = nlpResult.contextUsed
        )

        val voiceAction = mapIntentToAction(nlpResult.intent)
        val parameters = extractParameters(nlpResult)

        // Create voice command
        val voiceCommand = VoiceCommand(
            action = voiceAction,
            parameter = parameters["app_name"] ?: parameters["website"],
            rawText = text
        )

        android.util.Log.d(
            "AdvancedVoice",
            "=== COMMAND PROCESSING ==="
        )
        android.util.Log.d("AdvancedVoice", "Input: $text")
        android.util.Log.d("AdvancedVoice", "Intent: ${nlpResult.intent}")
        android.util.Log.d("AdvancedVoice", "Action: ${voiceCommand.action}")
        android.util.Log.d("AdvancedVoice", "Parameter: ${voiceCommand.parameter}")
        android.util.Log.d("AdvancedVoice", "Confidence: ${nlpResult.confidence}")

        // Wrap command with timestamp to ensure it's always unique and triggers LaunchedEffect
        val commandWrapper = VoiceCommandWrapper(
            command = voiceCommand,
            timestamp = System.currentTimeMillis()
        )

        _lastCommand.value = commandWrapper

        android.util.Log.d(
            "AdvancedVoice",
            "Command emitted to flow with timestamp: ${commandWrapper.timestamp}"
        )

        // Generate feedback based on confidence
        val feedbackMessage = generateFeedback(nlpResult)
        _feedback.value = AdvancedVoiceFeedback(
            message = feedbackMessage,
            type = if (nlpResult.confidence >= 0.7) AdvancedFeedbackType.SUCCESS else AdvancedFeedbackType.WARNING,
            command = voiceCommand,
            nlpResult = nlpResult
        )

        // Speak feedback if high confidence
        if (nlpResult.confidence >= 0.7) {
            speak(feedbackMessage, "feedback")
        } else {
            speak("I'm not sure I understood that. $feedbackMessage", "feedback")
        }

        _listeningState.value = AdvancedListeningState.IDLE

        android.util.Log.d(
            "AdvancedVoice",
            "Command processed successfully: action=${voiceCommand.action}, param=${voiceCommand.parameter}"
        )
    }

    /**
     * Map NLP intent to voice action
     */
    private fun mapIntentToAction(intent: VoiceIntent): VoiceAction {
        return when (intent) {
            VoiceIntent.NAVIGATE_DASHBOARD -> VoiceAction.NAVIGATE_DASHBOARD
            VoiceIntent.NAVIGATE_PASSWORDS -> VoiceAction.NAVIGATE_PASSWORDS
            VoiceIntent.NAVIGATE_VAULT -> VoiceAction.NAVIGATE_VAULT
            VoiceIntent.NAVIGATE_SETTINGS -> VoiceAction.NAVIGATE_SETTINGS
            VoiceIntent.NAVIGATE_AI_CHAT -> VoiceAction.NAVIGATE_AI_CHAT
            VoiceIntent.CHECK_SECURITY -> VoiceAction.CHECK_SECURITY
            VoiceIntent.AI_PREDICTOR -> VoiceAction.AI_PREDICTOR
            VoiceIntent.ADD_PASSWORD -> VoiceAction.ADD_PASSWORD
            VoiceIntent.SEARCH_PASSWORD -> VoiceAction.SEARCH_PASSWORD
            VoiceIntent.GENERATE_PASSWORD -> VoiceAction.GENERATE_PASSWORD
            VoiceIntent.LOCK_APP -> VoiceAction.LOCK_APP
            VoiceIntent.BACKUP_DATA -> VoiceAction.VAULT_STATUS // Map to closest existing
            VoiceIntent.CHECK_BREACHES -> VoiceAction.CHECK_SECURITY // Map to closest existing
            VoiceIntent.VOICE_HELP -> VoiceAction.HELP // Map to existing
            VoiceIntent.READ_ALOUD -> VoiceAction.HELP // Map to closest existing
            VoiceIntent.ENABLE_VOICE_CONTROL -> VoiceAction.HELP // Map to closest existing
            VoiceIntent.DISABLE_VOICE_CONTROL -> VoiceAction.HELP // Map to closest existing
            else -> VoiceAction.UNKNOWN
        }
    }

    /**
     * Extract parameters from NLP result
     */
    private fun extractParameters(nlpResult: NLPResult): Map<String, String> {
        val params = mutableMapOf<String, String>()

        // Extract app names, websites, etc.
        nlpResult.entities[EntityType.APP_NAME]?.firstOrNull()?.let {
            params["app_name"] = it
        }

        nlpResult.entities[EntityType.WEBSITE]?.firstOrNull()?.let {
            params["website"] = it
        }

        nlpResult.entities[EntityType.EMAIL]?.firstOrNull()?.let {
            params["email"] = it
        }

        nlpResult.entities[EntityType.NUMBER]?.firstOrNull()?.let {
            params["number"] = it
        }

        return params
    }

    /**
     * Generate natural language feedback
     */
    private fun generateFeedback(nlpResult: NLPResult): String {
        return when (nlpResult.intent) {
            VoiceIntent.NAVIGATE_DASHBOARD -> "Opening dashboard"
            VoiceIntent.NAVIGATE_PASSWORDS -> "Opening password manager"
            VoiceIntent.NAVIGATE_VAULT -> "Opening privacy vault"
            VoiceIntent.NAVIGATE_SETTINGS -> "Opening settings"
            VoiceIntent.NAVIGATE_AI_CHAT -> "Opening Privacy AI"
            VoiceIntent.CHECK_SECURITY -> "Checking security status"
            VoiceIntent.AI_PREDICTOR -> "Opening AI security predictor"
            VoiceIntent.ADD_PASSWORD -> {
                val app = nlpResult.entities[EntityType.APP_NAME]?.firstOrNull()
                val website = nlpResult.entities[EntityType.WEBSITE]?.firstOrNull()
                when {
                    app != null -> "Adding password for $app"
                    website != null -> "Adding password for $website"
                    else -> "Opening password form"
                }
            }
            VoiceIntent.SEARCH_PASSWORD -> {
                val app = nlpResult.entities[EntityType.APP_NAME]?.firstOrNull()
                val website = nlpResult.entities[EntityType.WEBSITE]?.firstOrNull()
                when {
                    app != null -> "Finding password for $app"
                    website != null -> "Finding password for $website"
                    else -> "Searching passwords"
                }
            }
            VoiceIntent.GENERATE_PASSWORD -> {
                val length = nlpResult.entities[EntityType.NUMBER]?.firstOrNull()
                if (length != null) "Generating $length character password"
                else "Generating strong password"
            }
            VoiceIntent.LOCK_APP -> "Locking app"
            VoiceIntent.BACKUP_DATA -> "Creating backup"
            VoiceIntent.CHECK_BREACHES -> "Checking for breaches"
            VoiceIntent.VOICE_HELP -> "Here's what I can do"
            VoiceIntent.ENABLE_VOICE_CONTROL -> "Enabling voice control"
            VoiceIntent.DISABLE_VOICE_CONTROL -> "Disabling voice control"
            VoiceIntent.UNKNOWN -> "I didn't understand that. Try saying 'help' for commands"
            else -> "Processing request"
        }
    }

    /**
     * Speak text using TTS
     */
    fun speak(text: String, utteranceId: String = "feedback") {
        if (!isTTSReady) {
            android.util.Log.w("AdvancedVoice", "TTS not ready, cannot speak: $text")
            return
        }

        // Stop listening while speaking
        stopListening()

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        android.util.Log.d("AdvancedVoice", "Speaking: $text")
    }

    /**
     * Provide correction feedback to NLP engine
     */
    fun provideCorrection(originalInput: String, correctIntent: VoiceIntent) {
        // Get the current NLP analysis which has the interpreted intent
        val currentAnalysis = _nlpAnalysis.value
        if (currentAnalysis != null) {
            val interpretedIntent = try {
                VoiceIntent.valueOf(currentAnalysis.intent.uppercase().replace(" ", "_"))
            } catch (e: Exception) {
                VoiceIntent.UNKNOWN
            }

            nlpEngine.learnFromCorrection(
                originalInput = originalInput,
                interpretedIntent = interpretedIntent,
                correctIntent = correctIntent,
                feedback = "User correction"
            )
            speak("Thanks for the correction. I'll remember that")
        }
    }

    /**
     * Get command suggestions
     */
    fun getSuggestions(partialInput: String): List<VoiceSuggestion> {
        return nlpEngine.getSuggestions(partialInput)
    }

    /**
     * Clear conversation context
     */
    fun clearContext() {
        nlpEngine.clearContext()
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        isEnabled = false
        stopListening()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    // Recognition Listener
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            android.util.Log.d("AdvancedVoice", "Ready for speech")
            _listeningState.value = AdvancedListeningState.LISTENING
        }

        override fun onBeginningOfSpeech() {
            android.util.Log.d("AdvancedVoice", "Speech started")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Audio level changed
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Buffer received
        }

        override fun onEndOfSpeech() {
            android.util.Log.d("AdvancedVoice", "Speech ended")
            _listeningState.value = AdvancedListeningState.PROCESSING
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening... (no speech yet)"
                else -> "Unknown error: $error"
            }

            android.util.Log.d("AdvancedVoice", "Recognition error ($error): $errorMessage")

            // For permission errors, show persistent message
            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                _feedback.value = AdvancedVoiceFeedback(
                    message = "❌ Microphone permission required. Go to Settings → Apps → SafeSphere → Permissions → Microphone → Allow",
                    type = AdvancedFeedbackType.ERROR
                )
                _listeningState.value = AdvancedListeningState.IDLE
                isEnabled = false
                return
            }

            // For NO_MATCH and TIMEOUT, these are normal - just restart listening
            if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            ) {
                android.util.Log.d("AdvancedVoice", "No speech detected, restarting listener...")
                // Keep the LISTENING state visible - don't set to IDLE

                // Restart listening immediately
                if (isEnabled) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (isEnabled) {
                            android.util.Log.d(
                                "AdvancedVoice",
                                "Restarting listening after no speech..."
                            )
                            startListening()
                        }
                    }, 500) // Short delay before restart
                }
                return
            }

            // For other errors, show message but keep trying
            _feedback.value = AdvancedVoiceFeedback(
                message = errorMessage,
                type = AdvancedFeedbackType.WARNING
            )

            _listeningState.value = AdvancedListeningState.IDLE

            // Restart listening if enabled (continuous mode)
            if (isEnabled) {
                android.util.Log.d(
                    "AdvancedVoice",
                    "Restarting listening after error: $errorMessage"
                )
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (isEnabled) startListening()
                }, 1500)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            android.util.Log.d("AdvancedVoice", "Results: $matches")

            matches?.firstOrNull()?.let { text ->
                _partialResults.value = ""
                processVoiceInput(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { text ->
                _partialResults.value = text
                android.util.Log.d("AdvancedVoice", "Partial: $text")
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Additional events
        }
    }
}

// Data Models

enum class AdvancedListeningState {
    IDLE,
    LISTENING,
    PROCESSING
}

data class AdvancedVoiceFeedback(
    val message: String,
    val type: AdvancedFeedbackType,
    val command: VoiceCommand? = null,
    val nlpResult: NLPResult? = null
)

enum class AdvancedFeedbackType {
    SUCCESS,
    WARNING,
    ERROR,
    INFO
}

data class NLPAnalysisDisplay(
    val input: String,
    val intent: String,
    val confidence: Double,
    val entities: List<EntityDisplay>,
    val sentiment: String,
    val urgency: String,
    val contextUsed: Boolean
)

data class EntityDisplay(
    val type: String,
    val value: String
)

data class VoiceCommandWrapper(
    val command: VoiceCommand,
    val timestamp: Long = System.currentTimeMillis()
)
