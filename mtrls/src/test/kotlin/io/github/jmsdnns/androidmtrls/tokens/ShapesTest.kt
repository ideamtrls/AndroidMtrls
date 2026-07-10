package io.github.jmsdnns.androidmtrls.tokens

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for [amSquircle] and its backing polygon builder.
 *
 * Pure JVM: only the factory's value semantics and the framework-free polygon
 * math ([amSquircleRectanglePolygon] + `RoundedPolygon.calculateBounds()`) are
 * exercised here. The full [androidx.compose.ui.graphics.Shape.createOutline]
 * path is *not* — it calls `RoundedPolygon.toPath()` → `android.graphics.Path`,
 * which is unavailable without Robolectric — so outline geometry is verified via
 * the polygon bounds instead.
 */
class ShapesTest {

    // -------------------------------------------------------------------------
    // amSquircle factory — value semantics (stable Compose shape parameter)
    // -------------------------------------------------------------------------

    @Test
    fun `amSquircle shapes with equal params are equal`() {
        assertEquals(amSquircle(16.dp), amSquircle(16.dp))
        assertEquals(amSquircle(16.dp).hashCode(), amSquircle(16.dp).hashCode())
    }

    @Test
    fun `amSquircle default smoothing is 0_6`() {
        assertEquals(amSquircle(16.dp), amSquircle(16.dp, smoothing = 0.6f))
        assertNotEquals(amSquircle(16.dp), amSquircle(16.dp, smoothing = 0.3f))
    }

    @Test
    fun `amSquircle differs by radius`() {
        assertNotEquals(amSquircle(16.dp), amSquircle(8.dp))
    }

    // -------------------------------------------------------------------------
    // amSquircleRectanglePolygon — outline geometry sanity
    // -------------------------------------------------------------------------

    @Test
    fun `squircle polygon is non-empty and fills the requested size`() {
        val polygon = amSquircleRectanglePolygon(200f, 100f, radiusPx = 20f, smoothing = 0.6f)

        // Non-empty: a rounded rectangle has cubic segments.
        assert(polygon.cubics.isNotEmpty()) { "Squircle polygon must have cubics" }

        // Bounds [left, top, right, bottom] span exactly [0, 0, width, height].
        val bounds = polygon.calculateBounds()
        assertEquals(0f,   bounds[0], 1f)
        assertEquals(0f,   bounds[1], 1f)
        assertEquals(200f, bounds[2], 1f)
        assertEquals(100f, bounds[3], 1f)
    }

    @Test
    fun `squircle polygon keeps uniform corners at any aspect ratio`() {
        // A tall box with the same radius still spans its exact size — the radius
        // is applied in pixel space, not by stretching a unit square, so corners
        // never distort into ellipses with aspect ratio.
        val bounds = amSquircleRectanglePolygon(60f, 300f, radiusPx = 24f, smoothing = 0.6f)
            .calculateBounds()
        assertEquals(60f,  bounds[2], 1f)
        assertEquals(300f, bounds[3], 1f)
    }

    @Test
    fun `squircle polygon clamps an over-large radius to half the shorter side`() {
        // A radius larger than the box must not blow past its bounds; clamped to
        // min(w, h) / 2 it still yields a valid pill spanning the full size.
        val bounds = amSquircleRectanglePolygon(80f, 40f, radiusPx = 999f, smoothing = 0.6f)
            .calculateBounds()
        assertEquals(80f, bounds[2], 1f)
        assertEquals(40f, bounds[3], 1f)
    }

    // -------------------------------------------------------------------------
    // Per-corner amSquircle factory — value semantics
    // -------------------------------------------------------------------------

    @Test
    fun `per-corner amSquircle shapes with equal params are equal`() {
        val a = amSquircle(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
        val b = amSquircle(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `per-corner amSquircle differs by any corner`() {
        val base = amSquircle(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
        assertNotEquals(base, amSquircle(topStart = 16.dp, topEnd = 8.dp, bottomEnd = 0.dp, bottomStart = 0.dp))
        assertNotEquals(base, amSquircle(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 4.dp, bottomStart = 0.dp))
        assertNotEquals(base, amSquircle(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 0.dp, bottomStart = 0.dp, smoothing = 0.3f))
    }

    @Test
    fun `per-corner amSquircle default smoothing is 0_6`() {
        assertEquals(
            amSquircle(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 0.dp, bottomStart = 0.dp),
            amSquircle(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 0.dp, bottomStart = 0.dp, smoothing = 0.6f),
        )
    }

    @Test
    fun `a uniform per-corner shape does not equal the single-radius shape`() {
        // Distinct types even with matching geometry — the per-corner form is a
        // separate Shape implementation, so equality never crosses the two.
        assertNotEquals(
            amSquircle(16.dp) as Any,
            amSquircle(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp) as Any,
        )
    }

    // -------------------------------------------------------------------------
    // amSquircleRectanglePolygonPerCorner — outline geometry sanity
    // -------------------------------------------------------------------------

    @Test
    fun `per-corner squircle polygon fills the requested size`() {
        val polygon = amSquircleRectanglePolygonPerCorner(
            width = 200f, height = 100f,
            topLeftPx = 20f, topRightPx = 20f,
            bottomRightPx = 0f, bottomLeftPx = 0f,
            smoothing = 0.6f,
        )
        assert(polygon.cubics.isNotEmpty()) { "Per-corner squircle polygon must have cubics" }
        val bounds = polygon.calculateBounds()
        assertEquals(0f,   bounds[0], 1f)
        assertEquals(0f,   bounds[1], 1f)
        assertEquals(200f, bounds[2], 1f)
        assertEquals(100f, bounds[3], 1f)
    }

    @Test
    fun `per-corner squircle polygon clamps over-large radii per corner`() {
        // Even with a huge top radius and square bottom, the outline stays within
        // the requested bounds (each corner clamps to min(w, h) / 2).
        val bounds = amSquircleRectanglePolygonPerCorner(
            width = 80f, height = 40f,
            topLeftPx = 999f, topRightPx = 999f,
            bottomRightPx = 0f, bottomLeftPx = 0f,
            smoothing = 0.6f,
        ).calculateBounds()
        assertEquals(80f, bounds[2], 1f)
        assertEquals(40f, bounds[3], 1f)
    }
}
