package com.enough.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Seed-derived Material 3 color scheme for Enough.
 *
 * Seed is the warm, muted green from DESIGN.md (#4E7A5C) — growth/fiber, never
 * clinical. These fixed schemes are the fallback used on devices that don't
 * support Material You dynamic color; when dynamic color is available the app
 * tints toward the user's wallpaper instead (see [EnoughTheme]).
 *
 * Note: [ColorScheme.error] is defined because Material requires it, but per
 * DESIGN.md it is never used to signal a missed goal or a low-fiber day — those
 * are neutral-toned. "You did it" states use [SuccessColors], not primary.
 */

// --- Light ---
internal val LightColors = lightColorScheme(
    primary = Color(0xFF3F6B4B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC1EFC7),
    onPrimaryContainer = Color(0xFF00210E),
    secondary = Color(0xFF52634F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E8CF),
    onSecondaryContainer = Color(0xFF101F0F),
    tertiary = Color(0xFF38656A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCEBF1),
    onTertiaryContainer = Color(0xFF002023),
    background = Color(0xFFFCFDF7),
    onBackground = Color(0xFF191D17),
    surface = Color(0xFFFCFDF7),
    onSurface = Color(0xFF191D17),
    surfaceVariant = Color(0xFFDDE5D8),
    onSurfaceVariant = Color(0xFF424940),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F7F1),
    surfaceContainer = Color(0xFFF0F1EB),
    surfaceContainerHigh = Color(0xFFEBECE5),
    surfaceContainerHighest = Color(0xFFE5E6E0),
    outline = Color(0xFF72796F),
    outlineVariant = Color(0xFFC1C9BD),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

// --- Dark ---
internal val DarkColors = darkColorScheme(
    primary = Color(0xFFA5D2AC),
    onPrimary = Color(0xFF0D3820),
    primaryContainer = Color(0xFF275234),
    onPrimaryContainer = Color(0xFFC1EFC7),
    secondary = Color(0xFFB9CCB4),
    onSecondary = Color(0xFF243424),
    secondaryContainer = Color(0xFF3A4B39),
    onSecondaryContainer = Color(0xFFD5E8CF),
    tertiary = Color(0xFFA0CFD5),
    onTertiary = Color(0xFF00363B),
    tertiaryContainer = Color(0xFF1E4D52),
    onTertiaryContainer = Color(0xFFBCEBF1),
    background = Color(0xFF101510),
    onBackground = Color(0xFFE1E4DB),
    surface = Color(0xFF101510),
    onSurface = Color(0xFFE1E4DB),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC1C9BD),
    surfaceContainerLowest = Color(0xFF0B0F0A),
    surfaceContainerLow = Color(0xFF191D17),
    surfaceContainer = Color(0xFF1D211B),
    surfaceContainerHigh = Color(0xFF272B25),
    surfaceContainerHighest = Color(0xFF323630),
    outline = Color(0xFF8C9388),
    outlineVariant = Color(0xFF424940),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * The extra "success/positive" color role Material 3 does not ship by default.
 * Used for fiber-goal-met and downward-weight-trend moments — never primary,
 * never a celebration overload (DESIGN.md).
 */
data class SuccessColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

internal val LightSuccessColors = SuccessColors(
    success = Color(0xFF3B6A3E),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFBCF0BB),
    onSuccessContainer = Color(0xFF06210A),
)

internal val DarkSuccessColors = SuccessColors(
    success = Color(0xFF9AD79E),
    onSuccess = Color(0xFF0A3912),
    successContainer = Color(0xFF234B27),
    onSuccessContainer = Color(0xFFB6F3BA),
)
