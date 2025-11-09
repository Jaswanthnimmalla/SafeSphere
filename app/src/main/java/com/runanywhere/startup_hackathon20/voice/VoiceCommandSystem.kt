package com.runanywhere.startup_hackathon20.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

/**
 * Comprehensive Voice Command System with Real-Time NLP
 *
 * Fixed Issues:
 * - Proper permission handling
 * - Real-time speech recognition
 * - Continuous listening with auto-restart
 * - Better error handling
 * - Indian English TTS voice
 */
class VoiceCommandSystem(private val context: Context) {

    companion object {
        private const val TAG = "VoiceCommand"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening
    
    private val _lastCommand = MutableStateFlow<VoiceCommand?>(null)
    val lastCommand: StateFlow<VoiceCommand?> = _lastCommand
    
    private val _voiceResponse = MutableStateFlow<String?>(null)
    val voiceResponse: StateFlow<String?> = _voiceResponse
    
    private val _currentLanguage = MutableStateFlow(SupportedLanguage.ENGLISH)
    val currentLanguage: StateFlow<SupportedLanguage> = _currentLanguage
    
    private val _continuousMode = MutableStateFlow(false)
    val continuousMode: StateFlow<Boolean> = _continuousMode

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private var isTtsReady = false
    private var isSpeaking = false

    init {
        Log.d(TAG, "Initializing VoiceCommandSystem")
        initializeSpeechRecognizer()
        initializeTextToSpeech()
    }
    
    /**
     * Check if microphone permission is granted
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Initialize Speech Recognizer
     */
    private fun initializeSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                // Destroy old one if exists
                speechRecognizer?.destroy()
                
                // Create fresh instance
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                Log.d(TAG, "SpeechRecognizer initialized successfully")
                checkReady()
            } else {
                Log.e(TAG, "Speech recognition not available on this device")
                _voiceResponse.value = "Speech recognition not available"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SpeechRecognizer", e)
            _voiceResponse.value = "Error: ${e.message}"
        }
    }
    
    /**
     * Initialize Text-to-Speech with Indian female voice
     */
    private fun initializeTextToSpeech() {
        try {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "TTS initialization successful")
                    
                    // Set language to Indian English
                    var result = textToSpeech?.setLanguage(Locale("en", "IN"))

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        // Fallback to US English
                        result = textToSpeech?.setLanguage(Locale.US)
                        Log.w(TAG, "Indian English not supported, using US English")
                    } else {
                        Log.d(TAG, "Indian English set successfully")
                    }
                    
                    // Log the result
                    Log.d(TAG, "Language set result: $result")

                    // Try to set female voice
                    val voices = textToSpeech?.voices
                    Log.d(TAG, "Available voices: ${voices?.size}")
                    
                    // First try: Indian/British female voice
                    var selectedVoice = voices?.firstOrNull { voice ->
                        voice.locale.language == "en" &&
                                (voice.locale.country == "IN" || voice.locale.country == "GB") &&
                                !voice.name.contains("male", ignoreCase = true)
                    }
                    
                    // Second try: Any English female voice
                    if (selectedVoice == null) {
                        selectedVoice = voices?.firstOrNull { voice ->
                            voice.locale.language == "en" &&
                                    !voice.name.contains("male", ignoreCase = true)
                        }
                    }
                    
                    // Third try: Any voice (don't be picky)
                    if (selectedVoice == null) {
                        selectedVoice = voices?.firstOrNull()
                    }

                    if (selectedVoice != null) {
                        textToSpeech?.voice = selectedVoice
                        Log.d(TAG, "Voice set: ${selectedVoice.name} (${selectedVoice.locale})")
                    } else {
                        Log.d(TAG, "Using default system voice")
                    }

                    // Set speech parameters
                    textToSpeech?.setPitch(1.0f)
                    textToSpeech?.setSpeechRate(0.9f) // Slightly slower for clarity

                    isTtsReady = true
                    Log.d(TAG, "TTS initialized and ready to speak")
                    
                    // DON'T test TTS - it interferes with speech recognition startup
                    checkReady()
                } else {
                    Log.e(TAG, "TTS initialization failed with status: $status")
                    _voiceResponse.value = "Text-to-speech unavailable"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TTS", e)
            _voiceResponse.value = "TTS Error: ${e.message}"
        }
    }

    private fun checkReady() {
        _isReady.value = (speechRecognizer != null && isTtsReady)
        Log.d(TAG, "System ready: ${_isReady.value}")
    }

    /**
     * Start listening for voice commands
     */
    fun startListening(language: SupportedLanguage = _currentLanguage.value) {
        Log.d(TAG, "startListening() called")

        if (!hasPermission()) {
            Log.e(TAG, "No microphone permission")
            _voiceResponse.value = "Microphone permission required"
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available")
            _voiceResponse.value = "Speech recognition not available"
            return
        }

        if (_isListening.value) {
            Log.d(TAG, "Already listening, stopping first")
            stopListening()
            handler.postDelayed({
                startListeningInternal(language)
            }, 300)
            return
        }

        startListeningInternal(language)
    }

    private fun startListeningInternal(language: SupportedLanguage) {
        try {
            Log.d(TAG, "Starting speech recognition in ${language.displayName}")

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.locale.toString())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.locale.toString())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            speechRecognizer?.setRecognitionListener(createRecognitionListener())
            speechRecognizer?.startListening(intent)

            _isListening.value = true
            _voiceResponse.value = language.responses.listening

            Log.d(TAG, "Speech recognition started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            _voiceResponse.value = "Error: ${e.message}"
            _isListening.value = false
        }
    }

    /**
     * Stop listening
     */
    fun stopListening() {
        Log.d(TAG, "stopListening() called")
        try {
            speechRecognizer?.stopListening()
            _isListening.value = false
            _continuousMode.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition", e)
        }
    }
    
    /**
     * Enable continuous listening mode
     */
    fun enableContinuousMode() {
        Log.d(TAG, "enableContinuousMode() called")
        _continuousMode.value = true
        
        // IMMEDIATELY start listening - no delays, no conditions
        if (!_isListening.value && !isSpeaking) {
            Log.d(TAG, "Starting listening in continuous mode")
            startListening()
        } else {
            Log.d(TAG, "Already listening or speaking: isListening=${_isListening.value}, isSpeaking=$isSpeaking")
        }
    }
    
    /**
     * Change language
     */
    fun setLanguage(language: SupportedLanguage) {
        Log.d(TAG, "setLanguage() to ${language.displayName}")
        _currentLanguage.value = language
        
        // Update TTS language
        try {
            when (language) {
                SupportedLanguage.HINDI -> textToSpeech?.setLanguage(Locale("hi", "IN"))
                SupportedLanguage.TELUGU -> textToSpeech?.setLanguage(Locale("te", "IN"))
                SupportedLanguage.SPANISH -> textToSpeech?.setLanguage(Locale("es", "ES"))
                SupportedLanguage.FRENCH -> textToSpeech?.setLanguage(Locale.FRENCH)
                SupportedLanguage.GERMAN -> textToSpeech?.setLanguage(Locale.GERMAN)
                SupportedLanguage.CHINESE -> textToSpeech?.setLanguage(Locale.CHINESE)
                SupportedLanguage.JAPANESE -> textToSpeech?.setLanguage(Locale.JAPANESE)
                else -> textToSpeech?.setLanguage(Locale("en", "IN"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error changing TTS language", e)
        }
    }
    
    /**
     * Speak response with progress tracking
     */
    fun speak(text: String) {
        if (textToSpeech == null) {
            Log.e(TAG, "TTS is null, cannot speak")
            return
        }
        
        if (!isTtsReady) {
            Log.w(TAG, "TTS not ready yet, cannot speak: $text")
            // Try to speak anyway in case it's ready but flag not set
        }

        Log.d(TAG, "Speaking: \"$text\"")
        isSpeaking = true

        val utteranceId = "SafeSphere_${System.currentTimeMillis()}"
        
        // Set utterance progress listener
        try {
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS onStart: $utteranceId")
                    _voiceResponse.value = text
                }
                
                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS onDone: $utteranceId")
                    isSpeaking = false

                    // If continuous mode, restart listening after speaking
                    if (_continuousMode.value) {
                        // Give more time for audio to fully complete
                        handler.postDelayed({
                            if (_continuousMode.value && !_isListening.value && !isSpeaking) {
                                Log.d(TAG, "Restarting listening after TTS completion")
                                startListening()
                            } else {
                                Log.d(TAG, "Not restarting: continuousMode=${_continuousMode.value}, isListening=${_isListening.value}, isSpeaking=$isSpeaking")
                            }
                        }, 1000) // Longer delay to ensure audio is done
                    }
                }

                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS onError: $utteranceId")
                    isSpeaking = false
                }
                
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.e(TAG, "TTS onError with code $errorCode: $utteranceId")
                    isSpeaking = false
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error setting utterance progress listener", e)
        }
        
        // Prepare params
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        
        // Speak the text
        try {
            val result = textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            Log.d(TAG, "TTS speak() returned: $result (SUCCESS=${TextToSpeech.SUCCESS}, ERROR=${TextToSpeech.ERROR})")
            
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "TTS speak() returned ERROR")
                isSpeaking = false
                _voiceResponse.value = "Speech error"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling TTS speak()", e)
            isSpeaking = false
            _voiceResponse.value = "Speech exception: ${e.message}"
        }
    }
    
    /**
     * Create Recognition Listener with proper error handling
     */
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech - MICROPHONE IS ACTIVE")
                _voiceResponse.value = _currentLanguage.value.responses.listening
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech - USER STARTED SPEAKING")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Volume level changed - log occasionally to see if mic picks up audio
                if (rmsdB > 0 && Math.random() < 0.1) { // Log 10% of the time
                    Log.d(TAG, "onRmsChanged: $rmsdB dB - AUDIO DETECTED")
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.d(TAG, "onBufferReceived - Audio buffer received")
            }
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech - USER STOPPED SPEAKING, processing...")
                _isListening.value = false
            }
            
            override fun onError(error: Int) {
                Log.e(TAG, "onError: $error")
                _isListening.value = false

                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> {
                        Log.e(TAG, "ERROR_AUDIO: Audio recording error")
                        "Audio recording error"
                    }
                    SpeechRecognizer.ERROR_CLIENT -> {
                        Log.e(TAG, "ERROR_CLIENT: Client side error")
                        "Client error"
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        Log.e(TAG, "ERROR_INSUFFICIENT_PERMISSIONS (9): No microphone permission")
                        // This is error code 9
                        "Microphone permission required"
                    }
                    13 -> {
                        // ERROR 13 is also a permission issue on some Android versions
                        Log.e(TAG, "ERROR 13: Permission issue - Recreating speech recognizer")
                        
                        // Recreate the speech recognizer to fix permission state
                        handler.postDelayed({
                            Log.d(TAG, "Recreating speech recognizer due to permission error")
                            initializeSpeechRecognizer()
                            
                            // Retry listening after recreation
                            if (_continuousMode.value && !isSpeaking) {
                                handler.postDelayed({
                                    Log.d(TAG, "Retrying after permission fix")
                                    startListening()
                                }, 500)
                            }
                        }, 300)
                        
                        return // Don't show error message, just retry
                    }
                    SpeechRecognizer.ERROR_NETWORK -> {
                        Log.e(TAG, "ERROR_NETWORK: Network error")
                        // In continuous mode, retry silently
                        if (_continuousMode.value) {
                            handler.postDelayed({
                                if (_continuousMode.value && !isSpeaking) {
                                    Log.d(TAG, "Retrying after network error")
                                    startListening()
                                }
                            }, 500)
                            return
                        }
                        "Network error"
                    }
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        Log.e(TAG, "ERROR_NETWORK_TIMEOUT: Network timeout")
                        // In continuous mode, retry silently
                        if (_continuousMode.value) {
                            handler.postDelayed({
                                if (_continuousMode.value && !isSpeaking) {
                                    Log.d(TAG, "Retrying after network timeout")
                                    startListening()
                                }
                            }, 500)
                            return
                        }
                        "Network timeout"
                    }
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        Log.w(TAG, "ERROR_NO_MATCH: No speech matched")
                        // In continuous mode, retry silently
                        if (_continuousMode.value) {
                            handler.postDelayed({
                                if (_continuousMode.value && !isSpeaking) {
                                    Log.d(TAG, "Retrying after no match")
                                    startListening()
                                }
                            }, 500)
                            return
                        }
                        _currentLanguage.value.responses.noMatch
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        Log.e(TAG, "ERROR_RECOGNIZER_BUSY: Recognizer is busy")
                        // Recognizer busy, retry
                        if (_continuousMode.value) {
                            handler.postDelayed({
                                if (_continuousMode.value && !isSpeaking) {
                                    Log.d(TAG, "Retrying after busy")
                                    startListening()
                                }
                            }, 1000)
                            return
                        }
                        "Recognizer busy"
                    }
                    SpeechRecognizer.ERROR_SERVER -> {
                        Log.e(TAG, "ERROR_SERVER: Server error")
                        // In continuous mode, retry silently
                        if (_continuousMode.value) {
                            handler.postDelayed({
                                if (_continuousMode.value && !isSpeaking) {
                                    Log.d(TAG, "Retrying after server error")
                                    startListening()
                                }
                            }, 1000)
                            return
                        }
                        "Server error"
                    }
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        Log.w(TAG, "ERROR_SPEECH_TIMEOUT: No speech input timeout")
                        // In continuous mode, retry silently
                        if (_continuousMode.value) {
                            handler.postDelayed({
                                if (_continuousMode.value && !isSpeaking) {
                                    Log.d(TAG, "Retrying after timeout")
                                    startListening()
                                }
                            }, 500)
                            return
                        }
                        _currentLanguage.value.responses.timeout
                    }
                    9 -> {
                        // ERROR_INSUFFICIENT_PERMISSIONS = 9
                        Log.e(TAG, "ERROR 9: Insufficient permissions")
                        "Microphone permission required"
                    }
                    else -> {
                        Log.e(TAG, "UNKNOWN ERROR CODE: $error")
                        // In continuous mode, retry instead of showing error
                        if (_continuousMode.value) {
                            handler.postDelayed({
                                if (_continuousMode.value && !isSpeaking) {
                                    Log.d(TAG, "Retrying after unknown error $error")
                                    startListening()
                                }
                            }, 1000)
                            return
                        }
                        "Error $error - Please try again"
                    }
                }

                // Only show error message if NOT in continuous mode
                if (!_continuousMode.value) {
                    Log.w(TAG, "Recognition error: $errorMessage")
                    _voiceResponse.value = errorMessage
                }
            }
            
            override fun onResults(results: Bundle?) {
                Log.d(TAG, "onResults")
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    Log.d(TAG, "Recognized: $spokenText")
                    processVoiceCommand(spokenText)
                } else {
                    Log.w(TAG, "No results")
                }
                _isListening.value = false
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d(TAG, "Partial: ${matches[0]}")
                    _voiceResponse.value = "${_currentLanguage.value.responses.processing} \"${matches[0]}\""
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d(TAG, "onEvent: $eventType")
            }
        }
    }
    
    /**
     * Process voice command with flexible NLP
     */
    fun processVoiceCommand(spokenText: String) {
        Log.d(TAG, "Processing command: $spokenText")

        val command = parseCommandFlexible(spokenText, _currentLanguage.value)
        _lastCommand.value = command
        
        val response = when (command.action) {
            VoiceAction.OPEN_PASSWORD -> _currentLanguage.value.responses.openPassword(command.parameter ?: "password")
            VoiceAction.GENERATE_PASSWORD -> _currentLanguage.value.responses.generatePassword
            VoiceAction.CHECK_SECURITY -> _currentLanguage.value.responses.checkSecurity
            VoiceAction.ADD_PASSWORD -> _currentLanguage.value.responses.addPassword
            VoiceAction.DELETE_PASSWORD -> _currentLanguage.value.responses.deletePassword(command.parameter ?: "password")
            VoiceAction.SEARCH_PASSWORD -> _currentLanguage.value.responses.searchPassword(command.parameter ?: "")
            VoiceAction.LIST_PASSWORDS -> _currentLanguage.value.responses.listPasswords
            VoiceAction.SECURITY_SCORE -> _currentLanguage.value.responses.securityScore
            VoiceAction.AI_PREDICTOR -> _currentLanguage.value.responses.aiPredictor
            VoiceAction.VAULT_STATUS -> _currentLanguage.value.responses.vaultStatus
            VoiceAction.LOCK_APP -> _currentLanguage.value.responses.lockApp
            VoiceAction.NAVIGATE_DASHBOARD -> _currentLanguage.value.responses.navigateDashboard
            VoiceAction.NAVIGATE_PASSWORDS -> _currentLanguage.value.responses.navigatePasswords
            VoiceAction.NAVIGATE_VAULT -> _currentLanguage.value.responses.navigateVault
            VoiceAction.NAVIGATE_SETTINGS -> _currentLanguage.value.responses.navigateSettings
            VoiceAction.NAVIGATE_AI_CHAT -> _currentLanguage.value.responses.navigateAiChat
            VoiceAction.HELP -> _currentLanguage.value.responses.help
            VoiceAction.UNKNOWN -> _currentLanguage.value.responses.unknown
        }

        Log.d(TAG, "Response: $response")
        speak(response)
    }
    
    /**
     * Enhanced flexible command parsing with natural language understanding
     */
    private fun parseCommandFlexible(text: String, language: SupportedLanguage): VoiceCommand {
        val lowerText = text.lowercase().trim()
        Log.d(TAG, "Parsing: $lowerText")

        // Navigation keywords (flexible)
        val navigationKeywords = listOf(
            "open", "go", "goto", "go to", "show", "navigate", "navigate to",
            "take me", "take me to", "move", "move to", "switch", "switch to",
            "kholo", "jao", "dikhao", "chalo", // Hindi
            "terichey", "vellu", "chupinchu", "velutu" // Telugu
        )
        
        // Check for navigation patterns
        for (keyword in navigationKeywords) {
            if (lowerText.contains(keyword)) {
                val parts = lowerText.split(keyword)
                if (parts.size > 1) {
                    val target = parts[1].trim()
                    val action = matchToScreen(target, language)
                    if (action != VoiceAction.UNKNOWN) {
                        Log.d(TAG, "Matched navigation: $action")
                        return VoiceCommand(action, target, text)
                    }
                }
            }
        }
        
        // Check traditional command patterns
        for (pattern in language.commandPatterns) {
            val match = pattern.patterns.firstOrNull { lowerText.contains(it.lowercase()) }
            if (match != null) {
                val parameter = extractParameter(lowerText, match, pattern.action)
                Log.d(TAG, "Matched pattern: ${pattern.action}")
                return VoiceCommand(pattern.action, parameter, text)
            }
        }
        
        // Fuzzy match for screen names
        val screenAction = matchToScreenFuzzy(lowerText, language)
        if (screenAction != VoiceAction.UNKNOWN) {
            Log.d(TAG, "Matched fuzzy: $screenAction")
            return VoiceCommand(screenAction, lowerText, text)
        }

        Log.d(TAG, "No match found")
        return VoiceCommand(VoiceAction.UNKNOWN, null, text)
    }
    
    /**
     * Match text to screen navigation
     */
    private fun matchToScreen(text: String, language: SupportedLanguage): VoiceAction {
        return when {
            text.contains("dashboard") || text.contains("home") ||
                    text.contains("main") || text.contains("ghar") || text.contains("mukhya") ||
                    text.contains("డాష్") || text.contains("హోమ్") ->
                VoiceAction.NAVIGATE_DASHBOARD
            
            text.contains("password") || text.contains("passwords") ||
                    text.contains("pass") || text.contains("passcode") ||
                    text.contains("పాస్") ->
                VoiceAction.NAVIGATE_PASSWORDS
            
            text.contains("vault") || text.contains("privacy") ||
                    text.contains("secure") || text.contains("tijori") ||
                    text.contains("వాల్ట్") ->
                VoiceAction.NAVIGATE_VAULT
            
            text.contains("setting") || text.contains("config") ||
                    text.contains("preferences") || text.contains("options") ||
                    text.contains("సెట్టింగ్") ->
                VoiceAction.NAVIGATE_SETTINGS
            
            text.contains("chat") || text.contains("ai") ||
                    text.contains("assistant") || text.contains("help") ||
                    text.contains("చాట్") ->
                VoiceAction.NAVIGATE_AI_CHAT
            
            text.contains("predict") || text.contains("security predict") ||
                    text.contains("future") || text.contains("risk") ||
                    text.contains("అంచనా") ->
                VoiceAction.AI_PREDICTOR
            
            text.contains("security") || text.contains("safe") ||
                    text.contains("check") || text.contains("status") ||
                    text.contains("భద్రత") ->
                VoiceAction.CHECK_SECURITY
            
            else -> VoiceAction.UNKNOWN
        }
    }
    
    /**
     * Fuzzy matching for screen names
     */
    private fun matchToScreenFuzzy(text: String, language: SupportedLanguage): VoiceAction {
        val screenKeywords = mapOf(
            VoiceAction.NAVIGATE_DASHBOARD to listOf("dash", "board", "home", "main", "ghar"),
            VoiceAction.NAVIGATE_PASSWORDS to listOf("pass", "word", "password", "code"),
            VoiceAction.NAVIGATE_VAULT to listOf("vault", "privacy", "secure", "safe", "tijori"),
            VoiceAction.NAVIGATE_SETTINGS to listOf("set", "config", "prefer", "option"),
            VoiceAction.NAVIGATE_AI_CHAT to listOf("chat", "talk", "ai", "assist"),
            VoiceAction.AI_PREDICTOR to listOf("predict", "future", "forecast", "risk"),
            VoiceAction.CHECK_SECURITY to listOf("secure", "safety", "check", "status")
        )
        
        for ((action, keywords) in screenKeywords) {
            if (keywords.any { text.contains(it) }) {
                return action
            }
        }
        
        return VoiceAction.UNKNOWN
    }
    
    /**
     * Extract parameter from command
     */
    private fun extractParameter(text: String, matchedPattern: String, action: VoiceAction): String? {
        return when (action) {
            VoiceAction.OPEN_PASSWORD,
            VoiceAction.DELETE_PASSWORD,
            VoiceAction.SEARCH_PASSWORD -> {
                val words = text.split(" ")
                val patternWords = matchedPattern.split(" ")
                val extraWords = words.filter { word -> 
                    patternWords.none { it.lowercase() == word.lowercase() }
                }
                extraWords.joinToString(" ").takeIf { it.isNotBlank() }
            }
            else -> null
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up VoiceCommandSystem")
        _continuousMode.value = false
        isSpeaking = false

        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying SpeechRecognizer", e)
        }

        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }

        handler.removeCallbacksAndMessages(null)

        _isReady.value = false
        isTtsReady = false
    }
}


/**
 * Voice Command Data Class
 */
data class VoiceCommand(
    val action: VoiceAction,
    val parameter: String?,
    val rawText: String
)

/**
 * Voice Actions - Enhanced with navigation actions
 */
enum class VoiceAction {
    OPEN_PASSWORD,
    GENERATE_PASSWORD,
    CHECK_SECURITY,
    ADD_PASSWORD,
    DELETE_PASSWORD,
    SEARCH_PASSWORD,
    LIST_PASSWORDS,
    SECURITY_SCORE,
    AI_PREDICTOR,
    VAULT_STATUS,
    LOCK_APP,
    NAVIGATE_DASHBOARD,
    NAVIGATE_PASSWORDS,
    NAVIGATE_VAULT,
    NAVIGATE_SETTINGS,
    NAVIGATE_AI_CHAT,
    HELP,
    UNKNOWN
}

/**
 * Supported Languages
 */
enum class SupportedLanguage(
    val displayName: String,
    val locale: Locale,
    val flag: String,
    val commandPatterns: List<CommandPattern>,
    val responses: LanguageResponses
) {
    ENGLISH(
        displayName = "English",
        locale = Locale.US,
        flag = "🇺🇸",
        commandPatterns = listOf(
            CommandPattern(
                VoiceAction.OPEN_PASSWORD,
                listOf("open", "show", "get", "retrieve", "find password for", "password for")
            ),
            CommandPattern(
                VoiceAction.GENERATE_PASSWORD,
                listOf("generate password", "create password", "new password", "make password")
            ),
            CommandPattern(
                VoiceAction.CHECK_SECURITY,
                listOf("check security", "security status", "how secure", "security check")
            ),
            CommandPattern(
                VoiceAction.ADD_PASSWORD,
                listOf("add password", "save password", "store password", "new entry")
            ),
            CommandPattern(
                VoiceAction.DELETE_PASSWORD,
                listOf("delete", "remove", "erase password")
            ),
            CommandPattern(VoiceAction.SEARCH_PASSWORD, listOf("search for", "find", "look for")),
            CommandPattern(
                VoiceAction.LIST_PASSWORDS,
                listOf("list passwords", "show all passwords", "all passwords")
            ),
            CommandPattern(
                VoiceAction.SECURITY_SCORE,
                listOf("security score", "what's my score", "how safe")
            ),
            CommandPattern(
                VoiceAction.AI_PREDICTOR,
                listOf("ai predictor", "predict security", "future risk", "prediction")
            ),
            CommandPattern(
                VoiceAction.VAULT_STATUS,
                listOf("vault status", "how many passwords", "password count")
            ),
            CommandPattern(
                VoiceAction.LOCK_APP,
                listOf("lock app", "lock safesphere", "secure app")
            ),
            CommandPattern(
                VoiceAction.HELP,
                listOf("help", "what can you do", "commands", "assistant")
            )
        ),
        responses = LanguageResponses(
            listening = "Listening...",
            processing = "Processing",
            noMatch = "Sorry, I didn't understand that",
            timeout = "No speech detected",
            openPassword = { service -> "Opening password for $service" },
            generatePassword = "Generating a strong password",
            checkSecurity = "Checking your security status",
            addPassword = "Ready to add a new password",
            deletePassword = { service -> "Deleting password for $service" },
            searchPassword = { query -> "Searching for $query" },
            listPasswords = "Showing all your passwords",
            securityScore = "Displaying your security score",
            aiPredictor = "Opening AI Security Predictor",
            vaultStatus = "Checking vault status",
            lockApp = "Locking SafeSphere",
            navigateDashboard = "Navigating to dashboard",
            navigatePasswords = "Navigating to passwords",
            navigateVault = "Navigating to vault",
            navigateSettings = "Navigating to settings",
            navigateAiChat = "Navigating to AI chat",
            help = "I can help you manage passwords, check security, generate passwords, and more. Just speak naturally!",
            unknown = "I didn't understand that command. Try saying 'help' for available commands."
        )
    ),

    HINDI(
        displayName = "हिंदी (Hindi)",
        locale = Locale("hi", "IN"),
        flag = "🇮🇳",
        commandPatterns = listOf(
            CommandPattern(
                VoiceAction.OPEN_PASSWORD,
                listOf("खोलो", "दिखाओ", "पासवर्ड दिखाओ", "password kholo")
            ),
            CommandPattern(
                VoiceAction.GENERATE_PASSWORD,
                listOf("password बनाओ", "नया password", "password generate karo")
            ),
            CommandPattern(
                VoiceAction.CHECK_SECURITY,
                listOf("security check karo", "सुरक्षा देखो", "kitna secure hai")
            ),
            CommandPattern(
                VoiceAction.ADD_PASSWORD,
                listOf("password add karo", "password save karo", "नया entry")
            ),
            CommandPattern(
                VoiceAction.DELETE_PASSWORD,
                listOf("delete karo", "हटाओ", "password hatao")
            ),
            CommandPattern(VoiceAction.SEARCH_PASSWORD, listOf("ढूंढो", "खोजो", "search karo")),
            CommandPattern(
                VoiceAction.LIST_PASSWORDS,
                listOf("सब passwords दिखाओ", "list passwords", "सारे passwords")
            ),
            CommandPattern(
                VoiceAction.SECURITY_SCORE,
                listOf("security score", "मेरा score क्या है", "kitna safe hai")
            ),
            CommandPattern(
                VoiceAction.AI_PREDICTOR,
                listOf("ai predictor", "भविष्य देखो", "prediction")
            ),
            CommandPattern(VoiceAction.LOCK_APP, listOf("app lock karo", "safesphere lock karo")),
            CommandPattern(VoiceAction.HELP, listOf("help", "मदद", "क्या कर सकते हो"))
        ),
        responses = LanguageResponses(
            listening = "सुन रहा हूँ...",
            processing = "Process हो रहा है",
            noMatch = "माफ़ करें, समझ नहीं आया",
            timeout = "आवाज़ नहीं सुनाई दी",
            openPassword = { service -> "$service का password खोल रहा हूँ" },
            generatePassword = "Strong password बना रहा हूँ",
            checkSecurity = "Security status check कर रहा हूँ",
            addPassword = "नया password जोड़ने के लिए तैयार",
            deletePassword = { service -> "$service का password delete कर रहा हूँ" },
            searchPassword = { query -> "$query ढूंढ रहा हूँ" },
            listPasswords = "सभी passwords दिखा रहा हूँ",
            securityScore = "आपका security score दिखा रहा हूँ",
            aiPredictor = "AI Security Predictor खोल रहा हूँ",
            vaultStatus = "Vault status check कर रहा हूँ",
            lockApp = "SafeSphere lock कर रहा हूँ",
            navigateDashboard = "डैशबोर्ड पर जा रहा हूँ",
            navigatePasswords = "पासवर्ड पर जा रहा हूँ",
            navigateVault = "वॉल्ट पर जा रहा हूँ",
            navigateSettings = "सेटिंग्स पर जा रहा हूँ",
            navigateAiChat = "AI चैट पर जा रहा हूँ",
            help = "मैं passwords manage करने, security check करने में मदद कर सकता हूँ। आराम से बोलें!",
            unknown = "Command समझ नहीं आया। 'help' बोलें commands के लिए।"
        )
    ),

    TELUGU(
        displayName = "తెలుగు (Telugu)",
        locale = Locale("te", "IN"),
        flag = "🇮🇳",
        commandPatterns = listOf(
            CommandPattern(
                VoiceAction.OPEN_PASSWORD,
                listOf("తెరిచేయి", "చూపించు", "పాస్‌వర్డ్ చూపించు")
            ),
            CommandPattern(
                VoiceAction.GENERATE_PASSWORD,
                listOf("పాస్‌వర్డ్ సృష్టించు", "కొత్త పాస్‌వర్డ్")
            ),
            CommandPattern(
                VoiceAction.CHECK_SECURITY,
                listOf("భద్రత తనిఖీ చేయండి", "భద్రత స్థితి")
            ),
            CommandPattern(
                VoiceAction.ADD_PASSWORD,
                listOf("పాస్‌వర్డ్‌ను జోడించండి", "పాస్‌వర్డ్‌ను సేవ్ చేయండి")
            ),
            CommandPattern(
                VoiceAction.DELETE_PASSWORD,
                listOf("తొలగించు", "పాస్‌వర్డ్‌ను తొలగించు")
            ),
            CommandPattern(VoiceAction.SEARCH_PASSWORD, listOf("వెతకండి", "కనుగొనండి")),
            CommandPattern(
                VoiceAction.LIST_PASSWORDS,
                listOf("పాస్‌వర్డ్‌లను జాబితా చేయండి", "అన్ని పాస్‌వర్డ్‌లు")
            ),
            CommandPattern(
                VoiceAction.SECURITY_SCORE,
                listOf("భద్రత స్కోరు", "మా స్కోరు ఏమిటి")
            ),
            CommandPattern(
                VoiceAction.AI_PREDICTOR,
                listOf("AI అంచనాదారు", "భవిష్యత్ ప్రమాదం")
            ),
            CommandPattern(VoiceAction.LOCK_APP, listOf("యాప్‌ను లాక్ చేయండి")),
            CommandPattern(VoiceAction.HELP, listOf("సహాయం", "నేను ఏమి చేయగలను"))
        ),
        responses = LanguageResponses(
            listening = "వినడం...",
            processing = "ప్రాసెసింగ్",
            noMatch = "మన్నించండి, అర్థం కాలేదు",
            timeout = "వాయిస్ గుర్తించబడలేదు",
            openPassword = { service -> "$service పాస్‌వర్డ్‌ను తెరిచినాను" },
            generatePassword = "బలమైన పాస్‌వర్డ్‌ను సృష్టిస్తున్నాను",
            checkSecurity = "భద్రత స్థితిని తనిఖీ చేస్తున్నాను",
            addPassword = "కొత్త పాస్‌వర్డ్‌ను జోడించడానికి సిద్ధం",
            deletePassword = { service -> "$service పాస్‌వర్డ్‌ను తొలగిస్తున్నాను" },
            searchPassword = { query -> "$query ను వెతకడం" },
            listPasswords = "అన్ని పాస్‌వర్డ్‌లను చూపిస్తున్నాను",
            securityScore = "మీ భద్రత స్కోరును చూపిస్తున్నాను",
            aiPredictor = "AI భద్రత అంచనాదారును తెరిచినాను",
            vaultStatus = "వాల్ట్ స్థితిని తనిఖీ చేస్తున్నాను",
            lockApp = "సేఫ్‌స్ఫియర్‌ను లాక్ చేస్తున్నాను",
            navigateDashboard = "డాష్‌బోర్డుకు వెళ్తున్నాను",
            navigatePasswords = "పాస్‌వర్డ్‌లకు వెళ్తున్నాను",
            navigateVault = "వాల్ట్‌కు వెళ్తున్నాను",
            navigateSettings = "సెట్టింగ్స్‌కు వెళ్తున్నాను",
            navigateAiChat = "AI చాట్‌కు వెళ్తున్నాను",
            help = "పాస్‌వర్డ్‌లను నిర్వహించడంలో, భద్రతను తనిఖీ చేయడంలో, పాస్‌వర్డ్‌లను సృష్టించడంలో సహాయం చేస్తాను. సులభంగా మాట్లాడండి!",
            unknown = "ఆజ్ఞను అర్థం చేసుకోలేకపోయాను. 'సహాయం' అని చెప్పండి ఆజ్ఞల కోసం."
        )
    ),

    SPANISH(
        displayName = "Español (Spanish)",
        locale = Locale("es", "ES"),
        flag = "🇪🇸",
        commandPatterns = listOf(
            CommandPattern(
                VoiceAction.OPEN_PASSWORD,
                listOf("abrir", "mostrar", "contraseña de", "obtener contraseña")
            ),
            CommandPattern(
                VoiceAction.GENERATE_PASSWORD,
                listOf("generar contraseña", "crear contraseña", "nueva contraseña")
            ),
            CommandPattern(
                VoiceAction.CHECK_SECURITY,
                listOf("verificar seguridad", "estado de seguridad", "qué tan seguro")
            ),
            CommandPattern(
                VoiceAction.ADD_PASSWORD,
                listOf("agregar contraseña", "guardar contraseña", "nueva entrada")
            ),
            CommandPattern(VoiceAction.DELETE_PASSWORD, listOf("eliminar", "borrar contraseña")),
            CommandPattern(VoiceAction.SEARCH_PASSWORD, listOf("buscar", "encontrar")),
            CommandPattern(
                VoiceAction.LIST_PASSWORDS,
                listOf("listar contraseñas", "todas las contraseñas")
            ),
            CommandPattern(
                VoiceAction.SECURITY_SCORE,
                listOf("puntuación de seguridad", "mi puntuación")
            ),
            CommandPattern(
                VoiceAction.AI_PREDICTOR,
                listOf("predictor ai", "predecir seguridad", "predicción")
            ),
            CommandPattern(VoiceAction.LOCK_APP, listOf("bloquear app", "bloquear safesphere")),
            CommandPattern(VoiceAction.HELP, listOf("ayuda", "qué puedes hacer", "comandos"))
        ),
        responses = LanguageResponses(
            listening = "Escuchando...",
            processing = "Procesando",
            noMatch = "Lo siento, no entendí eso",
            timeout = "No se detectó voz",
            openPassword = { service -> "Abriendo contraseña para $service" },
            generatePassword = "Generando una contraseña fuerte",
            checkSecurity = "Verificando tu estado de seguridad",
            addPassword = "Listo para agregar una nueva contraseña",
            deletePassword = { service -> "Eliminando contraseña para $service" },
            searchPassword = { query -> "Buscando $query" },
            listPasswords = "Mostrando todas tus contraseñas",
            securityScore = "Mostrando tu puntuación de seguridad",
            aiPredictor = "Abriendo Predictor de Seguridad AI",
            vaultStatus = "Verificando estado de la bóveda",
            lockApp = "Bloqueando SafeSphere",
            navigateDashboard = "Navegando al panel de control",
            navigatePasswords = "Navegando a contraseñas",
            navigateVault = "Navegando al cofre",
            navigateSettings = "Navegando a la configuración",
            navigateAiChat = "Navegando al chat de IA",
            help = "Puedo ayudarte a administrar contraseñas, verificar seguridad y más. ¡Habla naturalmente!",
            unknown = "No entendí ese comando. Intenta decir 'ayuda' para ver los comandos disponibles."
        )
    ),

    FRENCH(
        displayName = "Français (French)",
        locale = Locale.FRENCH,
        flag = "🇫🇷",
        commandPatterns = listOf(
            CommandPattern(
                VoiceAction.OPEN_PASSWORD,
                listOf("ouvrir", "montrer", "mot de passe de", "obtenir mot de passe")
            ),
            CommandPattern(
                VoiceAction.GENERATE_PASSWORD,
                listOf("générer mot de passe", "créer mot de passe", "nouveau mot de passe")
            ),
            CommandPattern(
                VoiceAction.CHECK_SECURITY,
                listOf("vérifier sécurité", "état de sécurité", "sécurisé")
            ),
            CommandPattern(
                VoiceAction.ADD_PASSWORD,
                listOf("ajouter mot de passe", "enregistrer mot de passe")
            ),
            CommandPattern(
                VoiceAction.DELETE_PASSWORD,
                listOf("supprimer", "effacer mot de passe")
            ),
            CommandPattern(VoiceAction.SEARCH_PASSWORD, listOf("chercher", "trouver")),
            CommandPattern(
                VoiceAction.LIST_PASSWORDS,
                listOf("lister mots de passe", "tous les mots de passe")
            ),
            CommandPattern(VoiceAction.SECURITY_SCORE, listOf("score de sécurité", "mon score")),
            CommandPattern(VoiceAction.AI_PREDICTOR, listOf("prédicteur ai", "prédire sécurité")),
            CommandPattern(
                VoiceAction.LOCK_APP,
                listOf("verrouiller app", "verrouiller safesphere")
            ),
            CommandPattern(VoiceAction.HELP, listOf("aide", "que peux-tu faire", "commandes"))
        ),
        responses = LanguageResponses(
            listening = "À l'écoute...",
            processing = "Traitement",
            noMatch = "Désolé, je n'ai pas compris",
            timeout = "Aucune parole détectée",
            openPassword = { service -> "Ouverture du mot de passe pour $service" },
            generatePassword = "Génération d'un mot de passe fort",
            checkSecurity = "Vérification de votre état de sécurité",
            addPassword = "Prêt à ajouter un nouveau mot de passe",
            deletePassword = { service -> "Suppression du mot de passe pour $service" },
            searchPassword = { query -> "Recherche de $query" },
            listPasswords = "Affichage de tous vos mots de passe",
            securityScore = "Affichage de votre score de sécurité",
            aiPredictor = "Ouverture du Prédicteur de Sécurité AI",
            vaultStatus = "Vérification de l'état du coffre",
            lockApp = "Verrouillage de SafeSphere",
            navigateDashboard = "Navigation vers le tableau de bord",
            navigatePasswords = "Navigation vers les mots de passe",
            navigateVault = "Navigation vers le coffre",
            navigateSettings = "Navigation vers les paramètres",
            navigateAiChat = "Navigation vers le chat IA",
            help = "Je peux vous aider à gérer les mots de passe, vérifier la sécurité et plus encore!",
            unknown = "Je n'ai pas compris cette commande. Dites 'aide' pour les commandes disponibles."
        )
    ),

    GERMAN(
        displayName = "Deutsch (German)",
        locale = Locale.GERMAN,
        flag = "🇩🇪",
        commandPatterns = listOf(
            CommandPattern(
                VoiceAction.OPEN_PASSWORD,
                listOf("öffnen", "zeigen", "passwort für", "passwort anzeigen")
            ),
            CommandPattern(
                VoiceAction.GENERATE_PASSWORD,
                listOf("passwort generieren", "passwort erstellen", "neues passwort")
            ),
            CommandPattern(
                VoiceAction.CHECK_SECURITY,
                listOf("sicherheit prüfen", "sicherheitsstatus", "wie sicher")
            ),
            CommandPattern(
                VoiceAction.ADD_PASSWORD,
                listOf("passwort hinzufügen", "passwort speichern")
            ),
            CommandPattern(VoiceAction.DELETE_PASSWORD, listOf("löschen", "passwort entfernen")),
            CommandPattern(VoiceAction.SEARCH_PASSWORD, listOf("suchen", "finden")),
            CommandPattern(
                VoiceAction.LIST_PASSWORDS,
                listOf("passwörter auflisten", "alle passwörter")
            ),
            CommandPattern(
                VoiceAction.SECURITY_SCORE,
                listOf("sicherheitspunktzahl", "meine punktzahl")
            ),
            CommandPattern(
                VoiceAction.AI_PREDICTOR,
                listOf("ai prädiktor", "sicherheit vorhersagen")
            ),
            CommandPattern(VoiceAction.LOCK_APP, listOf("app sperren", "safesphere sperren")),
            CommandPattern(VoiceAction.HELP, listOf("hilfe", "was kannst du", "befehle"))
        ),
        responses = LanguageResponses(
            listening = "Höre zu...",
            processing = "Verarbeitung",
            noMatch = "Entschuldigung, das habe ich nicht verstanden",
            timeout = "Keine Sprache erkannt",
            openPassword = { service -> "Öffne Passwort für $service" },
            generatePassword = "Generiere ein starkes Passwort",
            checkSecurity = "Überprüfe deinen Sicherheitsstatus",
            addPassword = "Bereit, ein neues Passwort hinzuzufügen",
            deletePassword = { service -> "Lösche Passwort für $service" },
            searchPassword = { query -> "Suche nach $query" },
            listPasswords = "Zeige alle deine Passwörter",
            securityScore = "Zeige deine Sicherheitspunktzahl",
            aiPredictor = "Öffne AI Sicherheits-Prädiktor",
            vaultStatus = "Überprüfe Tresor-Status",
            lockApp = "Sperre SafeSphere",
            navigateDashboard = "Navigiere zum Dashboard",
            navigatePasswords = "Navigiere zu Passwörtern",
            navigateVault = "Navigiere zum Tresor",
            navigateSettings = "Navigiere zu Einstellungen",
            navigateAiChat = "Navigiere zum AI-Chat",
            help = "Ich kann dir helfen, Passwörter zu verwalten, Sicherheit zu überprüfen und mehr!",
            unknown = "Ich habe diesen Befehl nicht verstanden. Sag 'Hilfe' für verfügbare Befehle."
        )
    ),

    CHINESE(
        displayName = "中文 (Chinese)",
        locale = Locale.CHINESE,
        flag = "🇨🇳",
        commandPatterns = listOf(
            CommandPattern(VoiceAction.OPEN_PASSWORD, listOf("打开", "显示", "密码", "打开密码")),
            CommandPattern(VoiceAction.GENERATE_PASSWORD, listOf("生成密码", "创建密码", "新密码")),
            CommandPattern(VoiceAction.CHECK_SECURITY, listOf("检查安全", "安全状态", "安全吗")),
            CommandPattern(VoiceAction.ADD_PASSWORD, listOf("添加密码", "保存密码")),
            CommandPattern(VoiceAction.DELETE_PASSWORD, listOf("删除", "删除密码")),
            CommandPattern(VoiceAction.SEARCH_PASSWORD, listOf("搜索", "查找")),
            CommandPattern(VoiceAction.LIST_PASSWORDS, listOf("列出密码", "所有密码")),
            CommandPattern(VoiceAction.SECURITY_SCORE, listOf("安全分数", "我的分数")),
            CommandPattern(VoiceAction.AI_PREDICTOR, listOf("ai预测器", "预测安全", "预测")),
            CommandPattern(VoiceAction.LOCK_APP, listOf("锁定应用", "锁定safesphere")),
            CommandPattern(VoiceAction.HELP, listOf("帮助", "你能做什么", "命令"))
        ),
        responses = LanguageResponses(
            listening = "正在听...",
            processing = "处理中",
            noMatch = "抱歉，我没听懂",
            timeout = "未检测到语音",
            openPassword = { service -> "正在打开 $service 的密码" },
            generatePassword = "正在生成强密码",
            checkSecurity = "正在检查您的安全状态",
            addPassword = "准备添加新密码",
            deletePassword = { service -> "正在删除 $service 的密码" },
            searchPassword = { query -> "正在搜索 $query" },
            listPasswords = "显示所有密码",
            securityScore = "显示您的安全分数",
            aiPredictor = "打开AI安全预测器",
            vaultStatus = "检查保险库状态",
            lockApp = "锁定SafeSphere",
            navigateDashboard = "导航到仪表板",
            navigatePasswords = "导航到密码",
            navigateVault = "导航到保险库",
            navigateSettings = "导航到设置",
            navigateAiChat = "导航到AI聊天",
            help = "我可以帮助您管理密码、检查安全等！",
            unknown = "我不明白这个命令。说'帮助'查看可用命令。"
        )
    ),

    JAPANESE(
        displayName = "日本語 (Japanese)",
        locale = Locale.JAPANESE,
        flag = "🇯🇵",
        commandPatterns = listOf(
            CommandPattern(
                VoiceAction.OPEN_PASSWORD,
                listOf("開く", "表示", "パスワード", "パスワードを開く")
            ),
            CommandPattern(
                VoiceAction.GENERATE_PASSWORD,
                listOf("パスワード生成", "パスワード作成", "新しいパスワード")
            ),
            CommandPattern(
                VoiceAction.CHECK_SECURITY,
                listOf("セキュリティチェック", "セキュリティ状態", "安全か")
            ),
            CommandPattern(VoiceAction.ADD_PASSWORD, listOf("パスワード追加", "パスワード保存")),
            CommandPattern(VoiceAction.DELETE_PASSWORD, listOf("削除", "パスワード削除")),
            CommandPattern(VoiceAction.SEARCH_PASSWORD, listOf("検索", "探す")),
            CommandPattern(
                VoiceAction.LIST_PASSWORDS,
                listOf("パスワード一覧", "全てのパスワード")
            ),
            CommandPattern(VoiceAction.SECURITY_SCORE, listOf("セキュリティスコア", "私のスコア")),
            CommandPattern(VoiceAction.AI_PREDICTOR, listOf("ai予測", "セキュリティ予測")),
            CommandPattern(VoiceAction.LOCK_APP, listOf("アプリロック", "safesphereロック")),
            CommandPattern(VoiceAction.HELP, listOf("ヘルプ", "何ができる", "コマンド"))
        ),
        responses = LanguageResponses(
            listening = "聞いています...",
            processing = "処理中",
            noMatch = "すみません、理解できませんでした",
            timeout = "音声が検出されませんでした",
            openPassword = { service -> "$service のパスワードを開いています" },
            generatePassword = "強力なパスワードを生成しています",
            checkSecurity = "セキュリティ状態を確認しています",
            addPassword = "新しいパスワードを追加する準備ができました",
            deletePassword = { service -> "$service のパスワードを削除しています" },
            searchPassword = { query -> "$query を検索しています" },
            listPasswords = "全てのパスワードを表示しています",
            securityScore = "セキュリティスコアを表示しています",
            aiPredictor = "AIセキュリティ予測を開いています",
            vaultStatus = "保管庫の状態を確認しています",
            lockApp = "SafeSphereをロックしています",
            navigateDashboard = "ダッシュボードに移動しています",
            navigatePasswords = "パスワードに移動しています",
            navigateVault = "保管庫に移動しています",
            navigateSettings = "設定に移動しています",
            navigateAiChat = "AIチャットに移動しています",
            help = "パスワード管理、セキュリティチェックなどをお手伝いできます！",
            unknown = "そのコマンドは理解できませんでした。'ヘルプ'と言って利用可能なコマンドを確認してください。"
        )
    )
}

/**
 * Command Pattern for each language
 */
data class CommandPattern(
    val action: VoiceAction,
    val patterns: List<String>
)

/**
 * Language-specific responses
 */
data class LanguageResponses(
    val listening: String,
    val processing: String,
    val noMatch: String,
    val timeout: String,
    val openPassword: (String) -> String,
    val generatePassword: String,
    val checkSecurity: String,
    val addPassword: String,
    val deletePassword: (String) -> String,
    val searchPassword: (String) -> String,
    val listPasswords: String,
    val securityScore: String,
    val aiPredictor: String,
    val vaultStatus: String,
    val lockApp: String,
    val navigateDashboard: String,
    val navigatePasswords: String,
    val navigateVault: String,
    val navigateSettings: String,
    val navigateAiChat: String,
    val help: String,
    val unknown: String
)
