package com.runanywhere.startup_hackathon20.ai

import android.util.Log
import com.runanywhere.sdk.public.RunAnywhere
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * OllamaAI - Offline AI Chat Assistant
 *
 * Simple, reliable offline AI using RunAnywhere SDK
 * Uses minimal prompt engineering for clean responses
 */
object OllamaAI {

    private const val TAG = "OllamaAI"

    /**
     * Generate a streaming response to the user's question
     * Simple approach - let the model do its job with minimal interference
     */
    fun generateResponse(userQuestion: String): Flow<String> = flow {
        try {
            Log.d(TAG, "User question: $userQuestion")

            // Very simple prompt - just the question
            val prompt = userQuestion.trim()

            var fullResponse = ""

            // Stream tokens from the model
            RunAnywhere.generateStream(prompt).collect { token ->
                fullResponse += token

                // Simple cleanup - just remove obvious artifacts
                val cleaned = simpleClean(fullResponse, userQuestion)

                // Emit the cleaned response
                if (cleaned.isNotEmpty() && cleaned.length >= 3) {
                    emit(cleaned)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error generating response: ${e.message}", e)
            throw e
        }
    }

    /**
     * Simple cleaning - just remove obvious artifacts, don't overthink it
     */
    private fun simpleClean(response: String, originalQuestion: String): String {
        var cleaned = response.trim()

        if (cleaned.isEmpty()) return ""

        // Remove template markers if they appear
        cleaned = cleaned.replace("### Instruction:", "", ignoreCase = true)
        cleaned = cleaned.replace("### Response:", "", ignoreCase = true)
        cleaned = cleaned.replace("### Answer:", "", ignoreCase = true)
        cleaned = cleaned.replace("### Question:", "", ignoreCase = true)

        // If response starts with "Question:" or "Answer:" remove it
        if (cleaned.startsWith("Question:", ignoreCase = true)) {
            cleaned = cleaned.substringAfter(":", "").trim()
        }
        if (cleaned.startsWith("Answer:", ignoreCase = true)) {
            cleaned = cleaned.substringAfter(":", "").trim()
        }
        if (cleaned.startsWith("Response:", ignoreCase = true)) {
            cleaned = cleaned.substringAfter(":", "").trim()
        }

        // If the response starts with the exact question, remove it
        val questionLower = originalQuestion.lowercase().trim()
        if (cleaned.lowercase().startsWith(questionLower)) {
            cleaned = cleaned.substring(originalQuestion.length).trim()
            // Remove leading : or - after question
            cleaned = cleaned.trimStart(':', '-', '?', ' ')
        }

        // Remove leading/trailing whitespace and special chars
        cleaned = cleaned.trim()

        // Capitalize first letter
        if (cleaned.isNotEmpty() && cleaned[0].isLowerCase()) {
            cleaned = cleaned[0].uppercase() + cleaned.substring(1)
        }

        return cleaned.trim()
    }

    /**
     * Privacy-focused system prompts for different contexts
     */
    object SystemPrompts {
        const val PRIVACY_EXPERT = """You are a privacy and security expert assistant. 
Provide clear, accurate information about data protection, encryption, and cybersecurity.
Keep responses concise and actionable."""

        const val PASSWORD_ADVISOR = """You are a password security expert.
Help users understand password security, encryption, and best practices.
Be specific and provide practical advice."""

        const val ENCRYPTION_GUIDE = """You are an encryption and cryptography expert.
Explain complex security concepts in simple terms.
Focus on practical privacy protection."""
    }
}
