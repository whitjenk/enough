package com.enough.app.ui.components

import kotlin.math.cos
import kotlin.math.sin

/**
 * The mascot's outline, as normalised points in a 0..1 box (§7.10 C1).
 *
 * Pulled out of the drawing code and made pure so the shape itself can be
 * unit-tested. It needed to be: the outline has now been "fixed" twice by
 * nudging Bézier control points by hand, and both times it still rendered as a
 * plain dot, because eyeballed deviations look far larger while zoomed in than
 * they measure. A test can check the thing an eye is bad at — how far from a
 * circle this actually is — and will fail if someone flattens it again.
 *
 * The outline is a circle whose radius is modulated by two harmonics, which
 * produces a few large, clearly-different lobes rather than uniform noise:
 *
 *     r(t) = 1 + 0.17·sin(2t + 0.6) + 0.11·sin(3t − 1.1)
 *
 * It stays abstract — no face, no eyes, nothing that reads as a creature — and
 * is deliberately not leaf-shaped, since DESIGN.md rules out a literal leaf.
 */
internal object MascotOutline {

    /** Points around the outline, normalised so they exactly fill a 0..1 box. */
    fun points(count: Int = 12): List<Pair<Float, Float>> {
        require(count >= 3) { "an outline needs at least 3 points, got $count" }
        val raw = (0 until count).map { i ->
            val t = 2.0 * Math.PI * i / count
            val r = radiusAt(t)
            (r * cos(t)) to (r * sin(t))
        }
        val minX = raw.minOf { it.first }
        val maxX = raw.maxOf { it.first }
        val minY = raw.minOf { it.second }
        val maxY = raw.maxOf { it.second }
        return raw.map { (x, y) ->
            ((x - minX) / (maxX - minX)).toFloat() to ((y - minY) / (maxY - minY)).toFloat()
        }
    }

    /** The modulated radius at angle [t] radians. Mean radius is 1. */
    fun radiusAt(t: Double): Double =
        1.0 + 0.17 * sin(2 * t + 0.6) + 0.11 * sin(3 * t - 1.1)

    /**
     * How far the outline departs from a circle, as (smallest, largest) radius.
     * A circle would be (1, 1); anything close to that renders as a dot.
     */
    fun radiusExtremes(samples: Int = 720): Pair<Double, Double> {
        val radii = (0 until samples).map { radiusAt(2.0 * Math.PI * it / samples) }
        return radii.min() to radii.max()
    }
}
