package com.enough.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the one property an eye is bad at judging: how far the mascot's
 * outline actually departs from a circle (§7.10 C1).
 *
 * This exists because the shape was "fixed" twice by hand-nudging Bézier
 * control points, and both times still rendered as a plain dot at the size it
 * is drawn — deviations that look obvious while zoomed in measure as almost
 * nothing. A flattened outline now fails here instead of shipping.
 */
class MascotOutlineTest {

    @Test
    fun `the outline is clearly not a circle`() {
        val (min, max) = MascotOutline.radiusExtremes()
        // A circle is 1.0 to 1.0. Below roughly 1.4x the swing averages away at
        // small sizes and the blob reads as a dot — which is exactly the bug
        // this pins.
        assertTrue(
            "radius swing was ${"%.2f".format(max / min)}x — too round to read as a blob",
            max / min > 1.4,
        )
    }

    @Test
    fun `points exactly fill the unit box`() {
        val pts = MascotOutline.points()
        val xs = pts.map { it.first }
        val ys = pts.map { it.second }
        // Normalisation has to touch all four edges, or the mascot renders
        // smaller than the size it was asked for and off-centre in its slot.
        assertEquals(0f, xs.min(), 1e-4f)
        assertEquals(1f, xs.max(), 1e-4f)
        assertEquals(0f, ys.min(), 1e-4f)
        assertEquals(1f, ys.max(), 1e-4f)
    }

    @Test
    fun `the outline is asymmetric on both axes`() {
        // A shape symmetric about either axis reads as a lens or an egg, not as
        // the organic blob DESIGN.md asks for.
        val pts = MascotOutline.points(24)
        val centroidX = pts.map { it.first }.average()
        val centroidY = pts.map { it.second }.average()
        assertTrue("horizontally symmetric", kotlin.math.abs(centroidX - 0.5) > 0.01)
        assertTrue("vertically symmetric", kotlin.math.abs(centroidY - 0.5) > 0.01)
    }

    @Test
    fun `an outline needs at least three points`() {
        runCatching { MascotOutline.points(2) }
            .onSuccess { error("expected a require() failure for 2 points") }
    }
}
