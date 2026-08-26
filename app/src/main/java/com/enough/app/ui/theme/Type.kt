package com.enough.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The app's type scale (§7.10 B1).
 *
 * Phase 0 still uses the default M3 font family — `DESIGN.md` explicitly allows
 * that and says not to block shipping on Roboto Flex. What changed is the
 * *range*. Every screen used to sit between `bodyMedium` and `titleLarge`: a
 * narrow band in which seven things on Today all carried roughly one weight, so
 * nothing receded and nothing advanced. Hierarchy needs display-size next to
 * caption-size with comparatively little in between.
 *
 * Only the styles the app actually leans on are overridden; everything else
 * inherits the M3 default, so this stays a widening rather than a rewrite.
 *
 * Sizes are in `sp` throughout, so they scale with the system font setting —
 * §7.10 A3 is a standing reminder of what breaks when large scales aren't
 * respected.
 */
private val Default = Typography()

val EnoughTypography = Typography(
    // The one number that matters on a screen — the fiber ring's live grams.
    // Bigger and tighter than M3's default so it reads as the hero at a glance.
    displayMedium = Default.displayMedium.copy(
        fontSize = 57.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1).sp,
        fontWeight = FontWeight.Bold,
    ),
    // The single statement a screen is making: the greeting, a section's value.
    headlineSmall = Default.headlineSmall.copy(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
    ),
    // Section values on Progress ("Logged on 5 of the last 7 days").
    titleLarge = Default.titleLarge.copy(
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.1).sp,
    ),
    // Quiet scaffolding: the `SectionLabel` that names a section without
    // competing with its value. Small, spaced, and never shouty — deliberately
    // not uppercase, which reads as raised voice and is announced letter by
    // letter by some screen readers.
    labelSmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
        fontWeight = FontWeight.Medium,
    ),
)
