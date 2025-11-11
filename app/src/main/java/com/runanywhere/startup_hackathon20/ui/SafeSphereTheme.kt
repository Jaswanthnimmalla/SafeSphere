package com.runanywhere.startup_hackathon20.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * SafeSphere Professional Theme System
 * Supports both Light and Dark themes with advanced colors
 */

// ==================== LIGHT THEME COLORS ====================
object SafeSphereLightColors {
    val Primary = Color(0xFF6366F1)
    val PrimaryVariant = Color(0xFF4F46E5)
    val Secondary = Color(0xFF8B5CF6)
    val SecondaryVariant = Color(0xFF7C3AED)
    val Accent = Color(0xFF06B6D4)

    val Background = Color(0xFFF8FAFC)
    val BackgroundSecondary = Color(0xFFFFFFFF)
    val BackgroundDark = Color(0xFFF1F5F9)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF1F5F9)
    val SurfaceElevated = Color(0xFFFFFFFF)

    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF475569)
    val TextTertiary = Color(0xFF94A3B8)

    val Success = Color(0xFF10B981)
    val SuccessContainer = Color(0xFFD1FAE5)
    val Error = Color(0xFFEF4444)
    val ErrorContainer = Color(0xFFFEE2E2)
    val Warning = Color(0xFFF59E0B)
    val WarningContainer = Color(0xFFFEF3C7)
    val Info = Color(0xFF3B82F6)
    val InfoContainer = Color(0xFFDBEAFE)

    val Border = Color(0xFFE2E8F0)
    val BorderLight = Color(0xFFF1F5F9)
    val BorderDark = Color(0xFFCBD5E1)

    val Overlay = Color(0x1A000000)
    val Shadow = Color(0x0F000000)
}

// ==================== DARK THEME COLORS ====================
object SafeSphereDarkColors {
    val Primary = Color(0xFF60A5FA)
    val PrimaryVariant = Color(0xFF3B82F6)
    val Secondary = Color(0xFFA78BFA)
    val SecondaryVariant = Color(0xFF8B5CF6)
    val Accent = Color(0xFF22D3EE)

    val Background = Color(0xFF0F172A)
    val BackgroundSecondary = Color(0xFF1E293B)
    val BackgroundDark = Color(0xFF1E293B)
    val Surface = Color(0xFF1E293B)
    val SurfaceVariant = Color(0xFF334155)
    val SurfaceElevated = Color(0xFF27374D)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFFCBD5E1)
    val TextTertiary = Color(0xFF64748B)

    val Success = Color(0xFF34D399)
    val SuccessContainer = Color(0xFF064E3B)
    val Error = Color(0xFFF87171)
    val ErrorContainer = Color(0xFF7F1D1D)
    val Warning = Color(0xFFFBBF24)
    val WarningContainer = Color(0xFF78350F)
    val Info = Color(0xFF60A5FA)
    val InfoContainer = Color(0xFF1E3A8A)

    val Border = Color(0xFF334155)
    val BorderLight = Color(0xFF475569)
    val BorderDark = Color(0xFF1E293B)

    val Overlay = Color(0x33000000)
    val Shadow = Color(0x4D000000)
}

// ==================== ADAPTIVE COLORS ====================
val LocalThemeIsDark = staticCompositionLocalOf { false }

/**
 * SafeSphereColors - Get theme-appropriate colors dynamically
 * This is now a composable that returns the correct colors based on theme
 */
@Composable
fun rememberSafeSphereColors(): ThemeColors {
    val isDark = LocalThemeIsDark.current
    return if (isDark) {
        ThemeColors(
            primary = SafeSphereDarkColors.Primary,
            primaryVariant = SafeSphereDarkColors.PrimaryVariant,
            secondary = SafeSphereDarkColors.Secondary,
            accent = SafeSphereDarkColors.Accent,
            background = SafeSphereDarkColors.Background,
            backgroundSecondary = SafeSphereDarkColors.BackgroundSecondary,
            backgroundDark = SafeSphereDarkColors.BackgroundDark,
            surface = SafeSphereDarkColors.Surface,
            surfaceVariant = SafeSphereDarkColors.SurfaceVariant,
            textPrimary = SafeSphereDarkColors.TextPrimary,
            textSecondary = SafeSphereDarkColors.TextSecondary,
            success = SafeSphereDarkColors.Success,
            error = SafeSphereDarkColors.Error,
            warning = SafeSphereDarkColors.Warning,
            info = SafeSphereDarkColors.Info,
            border = SafeSphereDarkColors.Border,
            overlay = SafeSphereDarkColors.Overlay
        )
    } else {
        ThemeColors(
            primary = SafeSphereLightColors.Primary,
            primaryVariant = SafeSphereLightColors.PrimaryVariant,
            secondary = SafeSphereLightColors.Secondary,
            accent = SafeSphereLightColors.Accent,
            background = SafeSphereLightColors.Background,
            backgroundSecondary = SafeSphereLightColors.BackgroundSecondary,
            backgroundDark = SafeSphereLightColors.BackgroundDark,
            surface = SafeSphereLightColors.Surface,
            surfaceVariant = SafeSphereLightColors.SurfaceVariant,
            textPrimary = SafeSphereLightColors.TextPrimary,
            textSecondary = SafeSphereLightColors.TextSecondary,
            success = SafeSphereLightColors.Success,
            error = SafeSphereLightColors.Error,
            warning = SafeSphereLightColors.Warning,
            info = SafeSphereLightColors.Info,
            border = SafeSphereLightColors.Border,
            overlay = SafeSphereLightColors.Overlay
        )
    }
}

/**
 * Static SafeSphereColors object - kept for backward compatibility
 * Now using dark theme as default
 */
object SafeSphereColors {
    // Dark theme colors (default static)
    val Primary = Color(0xFF60A5FA)
    val PrimaryVariant = Color(0xFF3B82F6)
    val Secondary = Color(0xFFA78BFA)
    val SecondaryVariant = Color(0xFF8B5CF6)
    val Accent = Color(0xFF22D3EE)

    val Background = Color(0xFF0F172A)
    val BackgroundSecondary = Color(0xFF1E293B)
    val BackgroundDark = Color(0xFF1E293B)
    val Surface = Color(0xFF1E293B)
    val SurfaceVariant = Color(0xFF334155)
    val SurfaceElevated = Color(0xFF27374D)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFFCBD5E1)
    val TextTertiary = Color(0xFF64748B)

    val Success = Color(0xFF34D399)
    val SuccessContainer = Color(0xFF064E3B)
    val Error = Color(0xFFF87171)
    val ErrorContainer = Color(0xFF7F1D1D)
    val Warning = Color(0xFFFBBF24)
    val WarningContainer = Color(0xFF78350F)
    val Info = Color(0xFF60A5FA)
    val InfoContainer = Color(0xFF1E3A8A)

    val Border = Color(0xFF334155)
    val BorderLight = Color(0xFF475569)
    val BorderDark = Color(0xFF1E293B)

    val Overlay = Color(0x33000000)
    val Shadow = Color(0x4D000000)
}

// ==================== LOCAL COMPOSITION FOR THEME COLORS ====================
val LocalSafeSphereColors = staticCompositionLocalOf<ThemeColors> {
    // Default to dark theme colors
    ThemeColors(
        primary = SafeSphereDarkColors.Primary,
        primaryVariant = SafeSphereDarkColors.PrimaryVariant,
        secondary = SafeSphereDarkColors.Secondary,
        accent = SafeSphereDarkColors.Accent,
        background = SafeSphereDarkColors.Background,
        backgroundSecondary = SafeSphereDarkColors.BackgroundSecondary,
        backgroundDark = SafeSphereDarkColors.BackgroundDark,
        surface = SafeSphereDarkColors.Surface,
        surfaceVariant = SafeSphereDarkColors.SurfaceVariant,
        textPrimary = SafeSphereDarkColors.TextPrimary,
        textSecondary = SafeSphereDarkColors.TextSecondary,
        success = SafeSphereDarkColors.Success,
        error = SafeSphereDarkColors.Error,
        warning = SafeSphereDarkColors.Warning,
        info = SafeSphereDarkColors.Info,
        border = SafeSphereDarkColors.Border,
        overlay = SafeSphereDarkColors.Overlay
    )
}

/**
 * Access current theme colors - use this in composables instead of SafeSphereColors
 */
val SafeSphereThemeColors: ThemeColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSafeSphereColors.current

/**
 * Extension to get theme-aware SafeSphere colors
 * Use MaterialTheme.safeSphereColors instead of SafeSphereColors in composables
 */
val MaterialTheme.safeSphereColors: ThemeColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSafeSphereColors.current

data class ThemeColors(
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val accent: Color,
    val background: Color,
    val backgroundSecondary: Color,
    val backgroundDark: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val success: Color,
    val error: Color,
    val warning: Color,
    val info: Color,
    val border: Color,
    val overlay: Color
)

@Composable
fun SafeSphereTheme(
    isDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = SafeSphereDarkColors.Primary,
            onPrimary = Color.White,
            primaryContainer = SafeSphereDarkColors.PrimaryVariant,
            onPrimaryContainer = SafeSphereDarkColors.TextPrimary,
            secondary = SafeSphereDarkColors.Secondary,
            onSecondary = Color.White,
            tertiary = SafeSphereDarkColors.Accent,
            background = SafeSphereDarkColors.Background,
            onBackground = SafeSphereDarkColors.TextPrimary,
            surface = SafeSphereDarkColors.Surface,
            onSurface = SafeSphereDarkColors.TextPrimary,
            error = SafeSphereDarkColors.Error,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = SafeSphereLightColors.Primary,
            onPrimary = Color.White,
            primaryContainer = SafeSphereLightColors.InfoContainer,
            onPrimaryContainer = SafeSphereLightColors.Primary,
            secondary = SafeSphereLightColors.Secondary,
            onSecondary = Color.White,
            tertiary = SafeSphereLightColors.Accent,
            background = SafeSphereLightColors.Background,
            onBackground = SafeSphereLightColors.TextPrimary,
            surface = SafeSphereLightColors.Surface,
            onSurface = SafeSphereLightColors.TextPrimary,
            error = SafeSphereLightColors.Error,
            onError = Color.White
        )
    }

    // Create theme-appropriate colors
    val safeSphereColors = if (isDark) {
        ThemeColors(
            primary = SafeSphereDarkColors.Primary,
            primaryVariant = SafeSphereDarkColors.PrimaryVariant,
            secondary = SafeSphereDarkColors.Secondary,
            accent = SafeSphereDarkColors.Accent,
            background = SafeSphereDarkColors.Background,
            backgroundSecondary = SafeSphereDarkColors.BackgroundSecondary,
            backgroundDark = SafeSphereDarkColors.BackgroundDark,
            surface = SafeSphereDarkColors.Surface,
            surfaceVariant = SafeSphereDarkColors.SurfaceVariant,
            textPrimary = SafeSphereDarkColors.TextPrimary,
            textSecondary = SafeSphereDarkColors.TextSecondary,
            success = SafeSphereDarkColors.Success,
            error = SafeSphereDarkColors.Error,
            warning = SafeSphereDarkColors.Warning,
            info = SafeSphereDarkColors.Info,
            border = SafeSphereDarkColors.Border,
            overlay = SafeSphereDarkColors.Overlay
        )
    } else {
        ThemeColors(
            primary = SafeSphereLightColors.Primary,
            primaryVariant = SafeSphereLightColors.PrimaryVariant,
            secondary = SafeSphereLightColors.Secondary,
            accent = SafeSphereLightColors.Accent,
            background = SafeSphereLightColors.Background,
            backgroundSecondary = SafeSphereLightColors.BackgroundSecondary,
            backgroundDark = SafeSphereLightColors.BackgroundDark,
            surface = SafeSphereLightColors.Surface,
            surfaceVariant = SafeSphereLightColors.SurfaceVariant,
            textPrimary = SafeSphereLightColors.TextPrimary,
            textSecondary = SafeSphereLightColors.TextSecondary,
            success = SafeSphereLightColors.Success,
            error = SafeSphereLightColors.Error,
            warning = SafeSphereLightColors.Warning,
            info = SafeSphereLightColors.Info,
            border = SafeSphereLightColors.Border,
            overlay = SafeSphereLightColors.Overlay
        )
    }

    CompositionLocalProvider(
        LocalThemeIsDark provides isDark,
        LocalSafeSphereColors provides safeSphereColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content
        )
    }
}
