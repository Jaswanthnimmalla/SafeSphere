package com.runanywhere.startup_hackathon20.share

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * System State Helper - Check and prompt for WiFi, Location, Bluetooth
 * Similar to Google Quick Share functionality
 */
object SystemStateHelper {

    /**
     * Check if WiFi is enabled
     */
    fun isWiFiEnabled(context: Context): Boolean {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        return wifiManager?.isWifiEnabled ?: false
    }

    /**
     * Check if Location is enabled
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isLocationEnabled ?: false
    }

    /**
     * Open WiFi settings
     */
    fun openWiFiSettings(context: Context) {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Open Location settings
     */
    fun openLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Get user-friendly error message
     */
    fun getRequirementsMessage(context: Context): String {
        val missing = mutableListOf<String>()

        if (!isWiFiEnabled(context)) {
            missing.add("WiFi")
        }

        if (!isLocationEnabled(context)) {
            missing.add("Location")
        }

        return when {
            missing.isEmpty() -> "All requirements met"
            missing.size == 1 -> "Please enable ${missing[0]}"
            else -> "Please enable ${missing.joinToString(" and ")}"
        }
    }
}

/**
 * Data class to hold system state
 */
data class SystemState(
    val isWiFiEnabled: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val isReady: Boolean = false
) {
    companion object {
        fun check(context: Context): SystemState {
            val wifiEnabled = SystemStateHelper.isWiFiEnabled(context)
            val locationEnabled = SystemStateHelper.isLocationEnabled(context)

            return SystemState(
                isWiFiEnabled = wifiEnabled,
                isLocationEnabled = locationEnabled,
                isReady = wifiEnabled && locationEnabled
            )
        }
    }
}
