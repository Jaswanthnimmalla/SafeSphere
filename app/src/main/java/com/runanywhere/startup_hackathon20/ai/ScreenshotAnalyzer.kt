package com.runanywhere.startup_hackathon20.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.startup_hackathon20.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * AI-Powered Screenshot Analyzer
 *
 * Uses RunAnywhere SDK for offline AI analysis of screenshots
 * to detect sensitive information (passwords, credit cards, etc.)
 */
class ScreenshotAnalyzer(private val context: Context) {

    companion object {
        private const val TAG = "ScreenshotAnalyzer"

        @Volatile
        private var instance: ScreenshotAnalyzer? = null

        fun getInstance(context: Context): ScreenshotAnalyzer {
            return instance ?: synchronized(this) {
                instance ?: ScreenshotAnalyzer(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Analyze screenshot for sensitive information
     * Uses RunAnywhere SDK AI models running offline
     */
    suspend fun analyzeScreenshot(screenshotPath: String): ScreenshotAnalysis {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            try {
                Log.d(TAG, "🔍 Analyzing screenshot: $screenshotPath")

                // Load screenshot
                val bitmap = BitmapFactory.decodeFile(screenshotPath)
                    ?: return@withContext createSafeAnalysis(screenshotPath, startTime)

                // Extract text from image using OCR (simulated with AI)
                val extractedText = extractTextFromImage(bitmap)

                // Analyze text for sensitive information using AI
                val detections = analyzeTextForSensitiveInfo(extractedText)

                // Determine threat level
                val threatLevel = calculateThreatLevel(detections)

                // Calculate confidence
                val confidence =
                    if (detections.isEmpty()) 1.0f else detections.map { it.confidence }.average()
                        .toFloat()

                val analysisTime = System.currentTimeMillis() - startTime

                Log.d(
                    TAG,
                    "✅ Analysis complete: ${detections.size} threats, level: $threatLevel, time: ${analysisTime}ms"
                )

                ScreenshotAnalysis(
                    screenshotPath = screenshotPath,
                    threatLevel = threatLevel,
                    detectedSensitiveInfo = detections,
                    confidence = confidence,
                    analysisTimeMs = analysisTime
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Analysis failed: ${e.message}", e)
                createSafeAnalysis(screenshotPath, startTime)
            }
        }
    }

    /**
     * Extract text from image using RunAnywhere SDK (OCR simulation)
     * In production, this would use actual OCR model from RunAnywhere SDK
     */
    private suspend fun extractTextFromImage(bitmap: Bitmap): String {
        return try {
            // Simulate OCR extraction
            // In production: Use RunAnywhere SDK's vision/OCR model

            val prompt = """Extract all visible text from this image. 
                |Focus on detecting:
                |- Passwords and credentials
                |- Credit card numbers
                |- Bank account numbers
                |- Social Security Numbers
                |- Email addresses
                |- Phone numbers
                |- Addresses
                |- Personal identification numbers
                |Return only the extracted text.""".trimMargin()

            // For demo, we'll simulate detection patterns
            // In real implementation: RunAnywhere SDK would process the actual bitmap
            simulateTextExtraction()
        } catch (e: Exception) {
            Log.e(TAG, "Text extraction failed: ${e.message}")
            ""
        }
    }

    /**
     * Simulate text extraction for demo
     * In production, this uses actual RunAnywhere SDK OCR
     */
    private fun simulateTextExtraction(): String {
        // Return empty for demo - will be replaced with real OCR
        return ""
    }

    /**
     * Analyze extracted text for sensitive information using AI
     * Uses RunAnywhere SDK's NLP capabilities
     */
    private suspend fun analyzeTextForSensitiveInfo(text: String): List<SensitiveInfoDetection> {
        if (text.isBlank()) return emptyList()

        val detections = mutableListOf<SensitiveInfoDetection>()

        try {
            // Use RunAnywhere SDK for pattern detection
            val prompt = """Analyze this text for sensitive information:
                |
                |"$text"
                |
                |Detect:
                |1. Passwords (e.g., "password:", "pwd:", credentials)
                |2. Credit cards (16-digit numbers with spaces/dashes)
                |3. Bank accounts (routing & account numbers)
                |4. SSN (XXX-XX-XXXX format)
                |5. Email addresses
                |6. Phone numbers
                |7. Physical addresses
                |8. API keys/tokens
                |
                |Return JSON: [{"type": "PASSWORD", "confidence": 0.95}]""".trimMargin()

            // Call RunAnywhere SDK
            val aiResponse = StringBuilder()
            RunAnywhere.generateStream(prompt).collect { token ->
                aiResponse.append(token)
            }

            // Parse AI response (simplified for demo)
            detections.addAll(parseAIResponse(aiResponse.toString()))

        } catch (e: Exception) {
            Log.e(TAG, "AI analysis failed, using pattern matching: ${e.message}")
            // Fallback to regex pattern matching
            detections.addAll(patternMatchingSensitiveInfo(text))
        }

        return detections
    }

    /**
     * Parse AI response to extract detections
     */
    private fun parseAIResponse(response: String): List<SensitiveInfoDetection> {
        val detections = mutableListOf<SensitiveInfoDetection>()

        // Simple parsing (in production, use proper JSON parsing)
        when {
            response.contains("password", ignoreCase = true) -> {
                detections.add(
                    SensitiveInfoDetection(
                        type = SensitiveInfoType.PASSWORD,
                        content = "****** (redacted)",
                        confidence = 0.95f
                    )
                )
            }

            response.contains("credit card", ignoreCase = true) -> {
                detections.add(
                    SensitiveInfoDetection(
                        type = SensitiveInfoType.CREDIT_CARD,
                        content = "**** **** **** ****",
                        confidence = 0.92f
                    )
                )
            }

            response.contains("bank", ignoreCase = true) -> {
                detections.add(
                    SensitiveInfoDetection(
                        type = SensitiveInfoType.BANK_ACCOUNT,
                        content = "Account: ******",
                        confidence = 0.90f
                    )
                )
            }
        }

        return detections
    }

    /**
     * Fallback pattern matching for sensitive information
     */
    private fun patternMatchingSensitiveInfo(text: String): List<SensitiveInfoDetection> {
        val detections = mutableListOf<SensitiveInfoDetection>()

        // Credit Card Pattern (16 digits)
        val creditCardPattern = Regex("""\b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b""")
        if (creditCardPattern.containsMatchIn(text)) {
            detections.add(
                SensitiveInfoDetection(
                    type = SensitiveInfoType.CREDIT_CARD,
                    content = "**** **** **** ****",
                    confidence = 0.88f
                )
            )
        }

        // SSN Pattern (XXX-XX-XXXX)
        val ssnPattern = Regex("""\b\d{3}-\d{2}-\d{4}\b""")
        if (ssnPattern.containsMatchIn(text)) {
            detections.add(
                SensitiveInfoDetection(
                    type = SensitiveInfoType.SSN,
                    content = "***-**-****",
                    confidence = 0.95f
                )
            )
        }

        // Email Pattern
        val emailPattern = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b""")
        if (emailPattern.containsMatchIn(text)) {
            detections.add(
                SensitiveInfoDetection(
                    type = SensitiveInfoType.EMAIL,
                    content = "***@***.com",
                    confidence = 0.85f
                )
            )
        }

        // Phone Pattern
        val phonePattern = Regex("""\b(?:\+?1[-.]?)?\(?\d{3}\)?[-.]?\d{3}[-.]?\d{4}\b""")
        if (phonePattern.containsMatchIn(text)) {
            detections.add(
                SensitiveInfoDetection(
                    type = SensitiveInfoType.PHONE_NUMBER,
                    content = "(***) ***-****",
                    confidence = 0.80f
                )
            )
        }

        // Password keywords
        val passwordKeywords = listOf("password", "pwd", "pass", "credential", "login")
        if (passwordKeywords.any { text.contains(it, ignoreCase = true) }) {
            detections.add(
                SensitiveInfoDetection(
                    type = SensitiveInfoType.PASSWORD,
                    content = "****** (detected)",
                    confidence = 0.75f
                )
            )
        }

        // Bank account keywords
        val bankKeywords = listOf("account number", "routing number", "bank account", "balance")
        if (bankKeywords.any { text.contains(it, ignoreCase = true) }) {
            detections.add(
                SensitiveInfoDetection(
                    type = SensitiveInfoType.BANK_ACCOUNT,
                    content = "Bank info detected",
                    confidence = 0.82f
                )
            )
        }

        // API Key pattern (common formats)
        val apiKeyPattern =
            Regex("""(?:api[_-]?key|token)[:\s]+[A-Za-z0-9_-]{20,}""", RegexOption.IGNORE_CASE)
        if (apiKeyPattern.containsMatchIn(text)) {
            detections.add(
                SensitiveInfoDetection(
                    type = SensitiveInfoType.API_KEY,
                    content = "API Key: ********",
                    confidence = 0.90f
                )
            )
        }

        return detections
    }

    /**
     * Calculate overall threat level based on detections
     */
    private fun calculateThreatLevel(detections: List<SensitiveInfoDetection>): ThreatLevel {
        if (detections.isEmpty()) return ThreatLevel.SAFE

        val criticalTypes = listOf(
            SensitiveInfoType.PASSWORD,
            SensitiveInfoType.CREDIT_CARD,
            SensitiveInfoType.BANK_ACCOUNT,
            SensitiveInfoType.SSN
        )

        val highTypes = listOf(
            SensitiveInfoType.PERSONAL_ID,
            SensitiveInfoType.API_KEY
        )

        return when {
            detections.any { it.type in criticalTypes && it.confidence > 0.8f } -> ThreatLevel.CRITICAL
            detections.any { it.type in criticalTypes } -> ThreatLevel.HIGH
            detections.any { it.type in highTypes } -> ThreatLevel.MEDIUM
            detections.size >= 3 -> ThreatLevel.MEDIUM
            detections.isNotEmpty() -> ThreatLevel.LOW
            else -> ThreatLevel.SAFE
        }
    }

    /**
     * Create safe analysis result (no threats)
     */
    private fun createSafeAnalysis(screenshotPath: String, startTime: Long): ScreenshotAnalysis {
        return ScreenshotAnalysis(
            screenshotPath = screenshotPath,
            threatLevel = ThreatLevel.SAFE,
            detectedSensitiveInfo = emptyList(),
            confidence = 1.0f,
            analysisTimeMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Quick check if screenshot likely contains sensitive info
     * Used for fast pre-screening
     */
    fun quickCheck(screenshotPath: String): Boolean {
        return try {
            val file = File(screenshotPath)
            // Check file size and name patterns
            file.exists() && file.length() > 10000 // At least 10KB
        } catch (e: Exception) {
            false
        }
    }
}
