package com.enough.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import com.enough.app.domain.theme.TimeOfDay
import androidx.compose.ui.platform.LocalContext

/**
 * Provides the extra success/positive color role down the tree.
 * Access via [EnoughTheme.successColors].
 */
private val LocalSuccessColors = staticCompositionLocalOf { LightSuccessColors }

/** The time-of-day background wash, provided down the tree. */
private val LocalBackgroundWash = staticCompositionLocalOf { LightWash.getValue(TimeOfDayKey.DAY) }

/**
 * App theme. Defaults to the warm green seed scheme from DESIGN.md.
 *
 * **Dynamic color is deliberately OFF by default (2026-08-24), which is a
 * considered deviation from DESIGN.md's "respect Material You" line.** Material
 * You hands the app's entire emotional register to whatever wallpaper someone
 * happens to have — on a stock device that produced a cold slate blue, which is
 * the opposite of what this app is for. Warmth here is a product value, not a
 * decoration: it is most of what makes the difference between "a calm companion"
 * and "a health tracker". Losing the belongs-on-my-phone win is the cheaper
 * trade, and it also means the Play listing screenshots match what people
 * actually get — which matters more now that store search is the only passive
 * acquisition surface there is.
 *
 * @param dynamicColor opt back into wallpaper-derived color. Kept as a parameter
 *   so this is a one-line reversal, not a rewrite.
 */
@Composable
fun EnoughTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    timeOfDay: TimeOfDay = remember { TimeOfDay.now() },
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
    val wash = (if (darkTheme) DarkWash else LightWash).getValue(timeOfDay.toKey())

    CompositionLocalProvider(
        LocalSuccessColors provides successColors,
        LocalBackgroundWash provides wash,
    ) {
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

    /**
     * The page background: a soft vertical wash tinted by time of day (§7.10 B1).
     *
     * Apply it once at the root and leave the Scaffolds transparent, rather than
     * painting it per screen — it is one continuous field behind the whole app,
     * not a decoration on each page.
     */
    val backgroundBrush: Brush
        @Composable
        @ReadOnlyComposable
        get() = LocalBackgroundWash.current.let { Brush.verticalGradient(listOf(it.top, it.bottom)) }
}

private fun TimeOfDay.toKey(): TimeOfDayKey = when (this) {
    TimeOfDay.MORNING -> TimeOfDayKey.MORNING
    TimeOfDay.DAY -> TimeOfDayKey.DAY
    TimeOfDay.EVENING -> TimeOfDayKey.EVENING
    TimeOfDay.NIGHT -> TimeOfDayKey.NIGHT
}
