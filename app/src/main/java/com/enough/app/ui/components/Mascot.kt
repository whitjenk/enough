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
 *
 * Drawn at 64dp by default (§7.10 C1). It spent two attempts at 40dp, where it
 * read as a plain dot in both themes no matter how the outline was pushed —
 * a small abstract shape simply cannot carry personality. It is bigger now and
 * used in fewer places, so each appearance has some presence.
 */
@Composable
fun Mascot(
    color: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
    // Named `diameter`, not `size`, so it doesn't shadow DrawScope.size inside
    // the Canvas block below. Matches FiberRing's parameter name.
    diameter: Dp = 64.dp,
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
            // Built from MascotOutline, which is pure and unit-tested — the
            // shape has twice been "fixed" by hand and twice still rendered as a
            // dot, so the departure from a circle is now something a test can
            // hold onto rather than something an eye has to judge while zoomed in.
            // Points are smoothed into a closed curve with Catmull-Rom cubics.
            val pts = MascotOutline.points()
            val n = pts.size
            fun px(i: Int) = pts[((i % n) + n) % n].first * w
            fun py(i: Int) = pts[((i % n) + n) % n].second * h
            val path = Path().apply {
                moveTo(px(0), py(0))
                for (i in 0 until n) {
                    // Catmull-Rom through p1..p2 expressed as a cubic Bézier.
                    val c1x = px(i) + (px(i + 1) - px(i - 1)) / 6f
                    val c1y = py(i) + (py(i + 1) - py(i - 1)) / 6f
                    val c2x = px(i + 1) - (px(i + 2) - px(i)) / 6f
                    val c2y = py(i + 1) - (py(i + 2) - py(i)) / 6f
                    cubicTo(c1x, c1y, c2x, c2y, px(i + 1), py(i + 1))
                }
                close()
            }
            drawPath(path, color)
        }
    }
}
