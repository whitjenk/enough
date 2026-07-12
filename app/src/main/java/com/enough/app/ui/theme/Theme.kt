package com.enough.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Provides the extra success/positive color role down the tree.
 * Access via [EnoughTheme.successColors].
 */
private val LocalSuccessColors = staticCompositionLocalOf { LightSuccessColors }

/**
 * App theme. Honors Material You dynamic color (Android 12+) so the app tints
 * toward the user's wallpaper — a real "belongs on my phone" win — and falls
 * back to the green-seed scheme everywhere else.
 *
 * @param dynamicColor allow wallpaper-derived color when the device supports it.
 *   Exposed mainly so previews/tests can pin the deterministic seed scheme.
 */
@Composable
fun EnoughTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val colorScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    val successColors = if (darkTheme) DarkSuccessColors else LightSuccessColors

    CompositionLocalProvider(LocalSuccessColors provides successColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = EnoughShapes,
            typography = EnoughTypography,
            content = content,
        )
    }
}

/** Access point for Enough-specific theme values that M3 doesn't ship. */
object EnoughTheme {
    val successColors: SuccessColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSuccessColors.current
}
