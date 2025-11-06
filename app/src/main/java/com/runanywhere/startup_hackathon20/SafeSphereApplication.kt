package com.runanywhere.startup_hackathon20

import android.app.Application
import android.util.Log
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.data.models.SDKEnvironment
import com.runanywhere.sdk.public.extensions.addModelFromURL
import com.runanywhere.sdk.llm.llamacpp.LlamaCppServiceProvider
import com.runanywhere.startup_hackathon20.security.SecurityManager
import com.runanywhere.startup_hackathon20.security.AuthenticationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * SafeSphere Application
 *
 * Privacy-First, Offline-First Android Application
 * Demonstrates local AI computation, encrypted storage, and data privacy
 *
 * Core Principles:
 * - All data stored locally with AES-256 encryption
 * - AI inference runs entirely on-device
 * - No cloud dependencies or external API calls
 * - User has full control over their data
 */
class SafeSphereApplication : Application() {

    companion object {
        private const val TAG = "SafeSphere"
        lateinit var instance: SafeSphereApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize security infrastructure first
        SecurityManager.initialize(this)

        // Initialize authentication manager
        AuthenticationManager.getInstance(this)

        // Initialize SDK asynchronously for offline AI capabilities
        GlobalScope.launch(Dispatchers.IO) {
            initializeSDK()
        }
    }

    private suspend fun initializeSDK() {
        try {
            Log.i(TAG, "🔐 Initializing SafeSphere SDK...")

            // Step 1: Initialize RunAnywhere SDK
            RunAnywhere.initialize(
                context = this@SafeSphereApplication,
                apiKey = "safesphere_offline",  // Dev mode for offline operation
                environment = SDKEnvironment.DEVELOPMENT
            )

            // Step 2: Register LLM Service Provider for local AI
            LlamaCppServiceProvider.register()

            // Step 3: Register lightweight models optimized for privacy and offline use
            registerPrivacyModels()

            // Step 4: Scan for previously downloaded models
            RunAnywhere.scanForDownloadedModels()

            Log.i(TAG, "✅ SafeSphere SDK initialized - Secure offline AI ready")

        } catch (e: Exception) {
            Log.e(TAG, "❌ SDK initialization failed: ${e.message}", e)
        }
    }

    /**
     * Register privacy-focused AI models
     * These models run entirely on-device for maximum privacy
     */
    private suspend fun registerPrivacyModels() {
        // Ultra-lightweight model for privacy chatbot (119 MB)
        // Fast inference, minimal battery usage, perfect for offline security advisor
        addModelFromURL(
            url = "https://huggingface.co/prithivMLmods/SmolLM2-360M-GGUF/resolve/main/SmolLM2-360M.Q8_0.gguf",
            name = "SafeSphere Privacy Advisor",
            type = "LLM"
        )

        // Balanced model for enhanced responses (374 MB)
        // Better context understanding for security questions
        addModelFromURL(
            url = "https://huggingface.co/Triangle104/Qwen2.5-0.5B-Instruct-Q6_K-GGUF/resolve/main/qwen2.5-0.5b-instruct-q6_k.gguf",
            name = "SafeSphere Security Consultant",
            type = "LLM"
        )

        Log.i(TAG, "🤖 Privacy AI models registered")
    }
}
