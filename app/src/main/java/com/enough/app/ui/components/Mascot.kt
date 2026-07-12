package com.enough.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * The Enough mascot: a single abstract, asymmetric organic blob in the success
 * color (DESIGN.md). No face, no character. Two states — still (default) and a
 * gentle scale-pulse when [pulsing] is true (a new nudge worth noticing). It's
 * decorative "a bit of life", so it carries a simple contentDescription.
 */
@Composable
fun Mascot(
    color: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
) {
    val scale = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "mascot-pulse")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "mascot-scale",
        ).value
    } else {
        1f
    }

    Canvas(
        modifier = modifier
            .size(40.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        scale(scale) {
            val w = size.width
            val h = size.height
            // An asymmetric rounded blob — deliberately not a circle.
            val path = Path().apply {
                moveTo(w * 0.50f, h * 0.06f)
                cubicTo(w * 0.86f, h * 0.02f, w * 1.02f, h * 0.40f, w * 0.88f, h * 0.66f)
                cubicTo(w * 0.76f, h * 0.90f, w * 0.40f, h * 1.00f, w * 0.18f, h * 0.82f)
                cubicTo(w * -0.02f, h * 0.66f, w * 0.02f, h * 0.28f, w * 0.24f, h * 0.14f)
                cubicTo(w * 0.32f, h * 0.08f, w * 0.41f, h * 0.07f, w * 0.50f, h * 0.06f)
                close()
            }
            drawPath(path, color)
        }
    }
}
