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
import androidx.compose.ui.unit.Dp
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
    // Named `diameter`, not `size`, so it doesn't shadow DrawScope.size inside
    // the Canvas block below. Matches FiberRing's parameter name.
    diameter: Dp = 40.dp,
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
            .size(diameter)
            .semantics { this.contentDescription = contentDescription },
    ) {
        scale(scale) {
            val w = size.width
            val h = size.height
            // An asymmetric rounded blob — deliberately not a circle.
            //
            // The first version of this path was asymmetric on paper but read as
            // a plain dot at 40dp on a real screen (§7.9), which is the only size
            // it is ever drawn at. The lobes below are exaggerated well past what
            // looks right zoomed in, because the small size averages them away:
            // a wide, low-shouldered left, a heavier drop bottom-right, and a
            // deliberately off-centre top so nothing reads as a diameter.
            val path = Path().apply {
                moveTo(w * 0.42f, h * 0.02f)
                // over the top and down the right — pushed out and low
                cubicTo(w * 0.82f, h * -0.04f, w * 1.06f, h * 0.30f, w * 0.94f, h * 0.58f)
                // the heavy bottom-right drop
                cubicTo(w * 0.86f, h * 0.80f, w * 0.66f, h * 1.04f, w * 0.42f, h * 0.97f)
                // a flatter, wider sweep back along the bottom-left
                cubicTo(w * 0.18f, h * 0.90f, w * -0.06f, h * 0.72f, w * 0.03f, h * 0.44f)
                // up the shallow left shoulder and back to the off-centre top
                cubicTo(w * 0.10f, h * 0.22f, w * 0.24f, h * 0.06f, w * 0.42f, h * 0.02f)
                close()
            }
            drawPath(path, color)
        }
    }
}
