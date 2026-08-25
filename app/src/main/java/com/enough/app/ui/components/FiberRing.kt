package com.enough.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.enough.app.R
import com.enough.app.ui.theme.EnoughTheme
import kotlin.math.roundToInt

/**
 * The fiber progress ring — the Today hero (DESIGN.md, §7.7 item 1). A single
 * Compose [Canvas] arc that fills in the `success` role (never primary, never
 * red), with the live gram number heavier/larger at its center so weight, not
 * color, carries the emphasis. The fill animates with the app's springy motion
 * signature (`DampingRatioMediumBouncy` / `StiffnessLow`, well under ~400ms).
 *
 * States handled:
 *  - normal: track + partial success arc, "X" over "of Yg".
 *  - over-target ([fiberSoFarG] >= [fiberTargetG]): arc completes the full ring
 *    and stays there; the a11y label notes the target is reached.
 *  - no target ([fiberTargetG] <= 0): a neutral, unfilled track with the grams
 *    logged so far in the center — no goal to fill toward yet.
 *
 * The whole ring exposes exactly one [contentDescription] ("X of Y grams…"); the
 * decorative center Texts are cleared from the tree so a screen reader hears one
 * clear statement, not three fragments.
 */
@Composable
fun FiberRing(
    fiberSoFarG: Int,
    fiberTargetG: Int,
    modifier: Modifier = Modifier,
    diameter: Dp = 176.dp,
    strokeWidth: Dp = 18.dp,
    hapticOnIncrease: Boolean = true,
) {
    val hasTarget = fiberTargetG > 0
    val met = hasTarget && fiberSoFarG >= fiberTargetG

    // One animated source of truth so the arc and the number move together: the
    // ring fills as the grams count up, rather than drifting out of sync.
    //
    // Seeded at the current value, so returning to a screen that already had
    // grams on it does NOT replay the fill — motion happens only on the thing
    // that just changed (DESIGN.md), never on every app open.
    val animatedGrams = remember { Animatable(fiberSoFarG.toFloat()) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(fiberSoFarG) {
        val previous = animatedGrams.targetValue
        if (previous == fiberSoFarG.toFloat()) return@LaunchedEffect
        // The payoff for a successful log: one gentle confirmation, and only on
        // an increase — removing a mis-logged meal is a correction, not a reward.
        if (hapticOnIncrease && fiberSoFarG > previous) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        }
        animatedGrams.animateTo(
            targetValue = fiberSoFarG.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }

    val displayedGrams = animatedGrams.value.roundToInt()
    val animatedFraction = if (hasTarget) {
        (animatedGrams.value / fiberTargetG).coerceIn(0f, 1f)
    } else {
        0f
    }

    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val fillColor = EnoughTheme.successColors.success

    val description = when {
        !hasTarget -> stringResource(R.string.cd_fiber_ring_no_target, fiberSoFarG)
        met -> stringResource(R.string.cd_fiber_ring_met, fiberSoFarG, fiberTargetG)
        else -> stringResource(R.string.cd_fiber_ring, fiberSoFarG, fiberTargetG)
    }

    Box(
        modifier = modifier
            .size(diameter)
            // Merge into one node and give it the single spoken label, so a
            // screen reader hears "18 of 28 grams of fiber today" rather than
            // "18" then "of 28g". Merging (rather than clearing) keeps the
            // rendered text in the unmerged tree, where tests can still see it.
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcSize = androidx.compose.ui.geometry.Size(
                size.width - strokeWidth.toPx(),
                size.height - strokeWidth.toPx(),
            )
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            // Full neutral track underneath — the "empty ring" preview even before
            // anything is logged (never red/gray-as-failure, just a quiet track).
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            // Success arc, starting at 12 o'clock and filling clockwise.
            if (animatedFraction > 0f) {
                drawArc(
                    color = fillColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedFraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        // Center: weight-not-color emphasis — the gram number is the heaviest,
        // largest thing here; the "of Yg" label recedes below it.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                // The animated value counts up; the contentDescription above uses
                // the settled one, so a screen reader never reads a mid-count number.
                text = displayedGrams.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = if (met) fillColor else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (hasTarget) {
                    stringResource(R.string.today_fiber_ring_target, fiberTargetG)
                } else {
                    stringResource(R.string.today_fiber_ring_grams)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun FiberRingPreview() {
    EnoughTheme(dynamicColor = false) {
        FiberRing(fiberSoFarG = 18, fiberTargetG = 28)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Target met")
@Composable
private fun FiberRingMetPreview() {
    EnoughTheme(dynamicColor = false) {
        FiberRing(fiberSoFarG = 30, fiberTargetG = 28)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "No target")
@Composable
private fun FiberRingNoTargetPreview() {
    EnoughTheme(dynamicColor = false) {
        FiberRing(fiberSoFarG = 12, fiberTargetG = 0)
    }
}
