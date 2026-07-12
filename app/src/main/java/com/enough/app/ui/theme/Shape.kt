package com.enough.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Expanded corner-radius scale from DESIGN.md — larger than the M3 defaults.
 * Cards and containers default to [Shapes.large] / [Shapes.extraLarge]; sharp
 * corners are avoided anywhere in the main flow.
 */
val EnoughShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
