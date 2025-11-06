package com.runanywhere.startup_hackathon20.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * SafeSphere Color Scheme
 * Dark theme with glowing blue privacy tone and glass-morphism
 */
object SafeSphereColors {
    // Primary colors
    val Primary = Color(0xFF2196F3)  // Blue - privacy, security
    val Secondary = Color(0xFF00BCD4)  // Cyan - technology
    val Accent = Color(0xFF9C27B0)  // Purple - premium

    // Background colors (dark theme)
    val Background = Color(0xFF0A0E1A)  // Deep dark blue
    val BackgroundDark = Color(0xFF050812)  // Even darker
    val Surface = Color(0xFF1A1F2E)  // Surface color with slight transparency
    val SurfaceVariant = Color(0xFF2A2F3E)  // Lighter surface

    // Text colors
    val TextPrimary = Color(0xFFFFFFFF)  // White
    val TextSecondary = Color(0xFFB0B3C1)  // Light gray
    val TextTertiary = Color(0xFF6B6E7E)  // Darker gray

    // Status colors
    val Success = Color(0xFF4CAF50)  // Green
    val Error = Color(0xFFF44336)  // Red
    val Warning = Color(0xFFFF9800)  // Orange
    val Info = Color(0xFF2196F3)  // Blue
}

/**
 * SafeSphere Theme
 * Dark glass-morphism design for privacy-focused UI
 */
@Composable
fun SafeSphereTheme(
    darkTheme: Boolean = true,  // Always dark for SafeSphere
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = SafeSphereColors.Primary,
        secondary = SafeSphereColors.Secondary,
        tertiary = SafeSphereColors.Accent,
        background = SafeSphereColors.Background,
        surface = SafeSphereColors.Surface,
        surfaceVariant = SafeSphereColors.SurfaceVariant,
        error = SafeSphereColors.Error,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = SafeSphereColors.TextPrimary,
        onSurface = SafeSphereColors.TextPrimary,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
