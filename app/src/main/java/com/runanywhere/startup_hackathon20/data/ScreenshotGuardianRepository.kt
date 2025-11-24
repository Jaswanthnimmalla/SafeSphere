package com.runanywhere.startup_hackathon20.data

import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Screenshot Guardian Repository
 * Handles data persistence and screenshot monitoring
 */
class ScreenshotGuardianRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "screenshot_guardian_prefs",
        Context.MODE_PRIVATE
    )

    private val gson = Gson()

    // State flows for real-time updates
    private val _stats = MutableStateFlow(loadStats())
    val stats: StateFlow<GuardianStats> = _stats.asStateFlow()

    private val _recentScans = MutableStateFlow(loadRecentScans())
    val recentScans: StateFlow<List<ScreenshotAnalysis>> = _recentScans.asStateFlow()

    private val _isEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private var screenshotObserver: ContentObserver? = null

    companion object {
        private const val TAG = "ScreenshotGuardianRepo"
        private const val KEY_STATS = "guardian_stats"
        private const val KEY_RECENT_SCANS = "recent_scans"
        private const val KEY_ENABLED = "guardian_enabled"
        private const val MAX_RECENT_SCANS = 20

        @Volatile
        private var instance: ScreenshotGuardianRepository? = null

        fun getInstance(context: Context): ScreenshotGuardianRepository {
            return instance ?: synchronized(this) {
                instance ?: ScreenshotGuardianRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    /**
     * Load stats from persistent storage
     */
    private fun loadStats(): GuardianStats {
        val json = prefs.getString(KEY_STATS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, GuardianStats::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading stats", e)
                GuardianStats()
            }
        } else {
            GuardianStats()
        }
    }

    /**
     * Save stats to persistent storage
     */
    private fun saveStats(stats: GuardianStats) {
        prefs.edit().putString(KEY_STATS, gson.toJson(stats)).apply()
        _stats.value = stats
    }

    /**
     * Load recent scans from persistent storage
     */
    private fun loadRecentScans(): List<ScreenshotAnalysis> {
        val json = prefs.getString(KEY_RECENT_SCANS, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<ScreenshotAnalysis>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading recent scans", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * Save recent scans to persistent storage
     */
    private fun saveRecentScans(scans: List<ScreenshotAnalysis>) {
        val limited = scans.take(MAX_RECENT_SCANS)
        prefs.edit().putString(KEY_RECENT_SCANS, gson.toJson(limited)).apply()
        _recentScans.value = limited
    }

    /**
     * Add a new scan result
     */
    fun addScanResult(analysis: ScreenshotAnalysis) {
        // Update recent scans
        val scans = _recentScans.value.toMutableList()
        scans.add(0, analysis) // Add to beginning
        saveRecentScans(scans.take(MAX_RECENT_SCANS))

        // Update stats
        val currentStats = _stats.value
        val newStats = currentStats.copy(
            totalScreenshots = currentStats.totalScreenshots + 1,
            threatsDetected = currentStats.threatsDetected + analysis.detectedSensitiveInfo.size,
            threatsBlocked = currentStats.threatsBlocked + if (analysis.isProtected) analysis.detectedSensitiveInfo.size else 0,
            passwordsDetected = currentStats.passwordsDetected +
                    analysis.detectedSensitiveInfo.count { it.type == SensitiveInfoType.PASSWORD },
            creditCardsDetected = currentStats.creditCardsDetected +
                    analysis.detectedSensitiveInfo.count { it.type == SensitiveInfoType.CREDIT_CARD },
            personalInfoDetected = currentStats.personalInfoDetected +
                    analysis.detectedSensitiveInfo.count {
                        it.type == SensitiveInfoType.EMAIL ||
                                it.type == SensitiveInfoType.PHONE_NUMBER ||
                                it.type == SensitiveInfoType.ADDRESS
                    },
            screenshotsProtected = currentStats.screenshotsProtected + if (analysis.isProtected) 1 else 0,
            lastScanTime = System.currentTimeMillis(),
            avgAnalysisTimeMs = ((currentStats.avgAnalysisTimeMs * currentStats.totalScreenshots) +
                    analysis.analysisTimeMs) / (currentStats.totalScreenshots + 1)
        )
        saveStats(newStats)

        Log.d(TAG, "Scan result added: ${analysis.detectedSensitiveInfo.size} threats detected")
    }

    /**
     * Enable/Disable protection
     */
    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _isEnabled.value = enabled

        if (enabled) {
            startScreenshotMonitoring()
        } else {
            stopScreenshotMonitoring()
        }
    }

    /**
     * Start monitoring for screenshots
     */
    fun startScreenshotMonitoring() {
        if (screenshotObserver != null) return

        screenshotObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                uri?.let {
                    Log.d(TAG, "Screenshot detected: $uri")
                    // Notify that a screenshot was taken
                    // The actual analysis will be triggered by the UI/ViewModel
                }
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                screenshotObserver!!
            )
            Log.d(TAG, "Screenshot monitoring started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screenshot monitoring", e)
        }
    }

    /**
     * Stop monitoring for screenshots
     */
    fun stopScreenshotMonitoring() {
        screenshotObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
                Log.d(TAG, "Screenshot monitoring stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop screenshot monitoring", e)
            }
        }
        screenshotObserver = null
    }

    /**
     * Clear all data
     */
    fun clearAllData() {
        saveStats(GuardianStats())
        saveRecentScans(emptyList())
    }

    /**
     * Get the latest screenshot path
     */
    fun getLatestScreenshot(): String? {
        return try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_TAKEN
            )

            val selection = "${MediaStore.Images.Media.DATA} LIKE ?"
            val selectionArgs = arrayOf("%Screenshot%")
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    cursor.getString(dataIndex)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest screenshot", e)
            null
        }
    }
}
