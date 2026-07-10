package io.github.jmsdnns.androidmtrls.collections

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for the pure math functions in [CardStackMath].
 *
 * All tested functions are internal top-level functions in `CardStackMath.kt`
 * and are testable without the Compose runtime.
 *
 * Math under test:
 *  - [amCardStackRotationForDx] — tilt angle from horizontal drag offset
 *  - [amCardStackShouldCommit] — commit-vs-snap-back decision
 *  - [amCardStackExitTargets] — fling-off destination coordinates
 *  - [amCardStackDepthScale] — per-depth scale factor
 *  - [amCardStackDepthYOffsetPx] — per-depth y translation in pixels
 *  - [amCardStackDepthAlpha] — per-depth alpha
 */
class CardStackMathTest {

    // -------------------------------------------------------------------------
    // amCardStackRotationForDx — tilt calculation
    // -------------------------------------------------------------------------

    @Test
    fun `rotationForDx returns 0 at dx=0`() {
        assertEquals(0f, amCardStackRotationForDx(dx = 0f, width = 400f), 0.0001f)
    }

    @Test
    fun `rotationForDx returns positive rotation for positive dx`() {
        val result = amCardStackRotationForDx(dx = 100f, width = 400f)
        assertTrue("Expected positive rotation for positive dx", result > 0f)
    }

    @Test
    fun `rotationForDx returns negative rotation for negative dx`() {
        val result = amCardStackRotationForDx(dx = -100f, width = 400f)
        assertTrue("Expected negative rotation for negative dx", result < 0f)
    }

    @Test
    fun `rotationForDx reaches maxTiltDegrees at half-width`() {
        // tiltFullAtWidthFraction = 0.5 → full tilt at dx = width * 0.5
        // maxTiltDegrees = 12f
        val width = 400f
        val dx = width * 0.5f // exactly at full-tilt point
        assertEquals(12f, amCardStackRotationForDx(dx = dx, width = width), 0.0001f)
    }

    @Test
    fun `rotationForDx clamps beyond half-width`() {
        // Beyond the tilt-full point the angle is clamped to ±maxTiltDegrees
        val width = 400f
        val dx = width * 0.9f // well past the 0.5 * width tilt point
        assertEquals(12f, amCardStackRotationForDx(dx = dx, width = width), 0.0001f)
    }

    @Test
    fun `rotationForDx clamps in negative direction`() {
        val width = 400f
        val dx = -(width * 0.9f)
        assertEquals(-12f, amCardStackRotationForDx(dx = dx, width = width), 0.0001f)
    }

    @Test
    fun `rotationForDx returns 0 when width is 0`() {
        assertEquals(0f, amCardStackRotationForDx(dx = 50f, width = 0f), 0.0001f)
    }

    @Test
    fun `rotationForDx returns 0 when width is negative`() {
        assertEquals(0f, amCardStackRotationForDx(dx = 50f, width = -100f), 0.0001f)
    }

    @Test
    fun `rotationForDx is linear before the tilt-full point`() {
        // At half of the tilt-full distance (width * 0.25) the tilt should be half of max
        val width = 400f
        val dx = width * 0.25f // half of the tiltFullAtWidthFraction * width = 200f
        assertEquals(6f, amCardStackRotationForDx(dx = dx, width = width), 0.0001f)
    }

    @Test
    fun `rotationForDx sign follows dx sign`() {
        val width = 400f
        val posResult = amCardStackRotationForDx(dx = 100f, width = width)
        val negResult = amCardStackRotationForDx(dx = -100f, width = width)
        assertEquals(posResult, -negResult, 0.0001f)
    }

    // -------------------------------------------------------------------------
    // amCardStackShouldCommit — commit decision
    //
    // commit when:   |dx| > width * 0.25  OR  |predictedDx| > width * 0.6
    // snap back when: both conditions are false
    // predictedDx = dx + velocity * 0.499  (AM_FLING_PROJECTION_SECONDS)
    // -------------------------------------------------------------------------

    @Test
    fun `shouldCommit returns true when distance threshold exceeded`() {
        val width = 400f
        // |dx| = 120 > 400 * 0.25 = 100 → commit
        assertTrue(amCardStackShouldCommit(dx = 120f, velocityX = 0f, width = width))
    }

    @Test
    fun `shouldCommit returns true when velocity threshold exceeded`() {
        val width = 400f
        // dx = 10 (below distance threshold of 100)
        // velocity = 500 → predictedDx = 10 + 500*0.499 = 259.5 > 400*0.6 = 240 → commit
        // (a modest flick now commits — under the old 0.1 window it would NOT)
        assertTrue(amCardStackShouldCommit(dx = 10f, velocityX = 500f, width = width))
    }

    @Test
    fun `shouldCommit returns false when both below thresholds`() {
        val width = 400f
        // |dx| = 50 < 100 (distance threshold)
        // predictedDx = 50 + 100*0.499 = 99.9 < 240 (velocity threshold) → snap back
        assertFalse(amCardStackShouldCommit(dx = 50f, velocityX = 100f, width = width))
    }

    @Test
    fun `shouldCommit returns false at exact distance boundary`() {
        val width = 400f
        // |dx| = 100 — NOT strictly greater than threshold → snap back
        // (predictedDx = 100 + 0*0.499 = 100 < 240) → snap back
        assertFalse(amCardStackShouldCommit(dx = 100f, velocityX = 0f, width = width))
    }

    @Test
    fun `shouldCommit returns true just above distance boundary`() {
        val width = 400f
        // |dx| = 100.01 > 100 → commit
        assertTrue(amCardStackShouldCommit(dx = 100.01f, velocityX = 0f, width = width))
    }

    @Test
    fun `shouldCommit is symmetric for negative dx`() {
        val width = 400f
        val positiveResult = amCardStackShouldCommit(dx = 120f, velocityX = 0f, width = width)
        val negativeResult = amCardStackShouldCommit(dx = -120f, velocityX = 0f, width = width)
        assertEquals(positiveResult, negativeResult)
    }

    @Test
    fun `shouldCommit uses predicted dx including velocity carry`() {
        val width = 400f
        // dx = 0 (no distance) — but flick: velocity carries
        // predictedDx = 0 + 600*0.499 = 299.4 > 240 → commit
        assertTrue(amCardStackShouldCommit(dx = 0f, velocityX = 600f, width = width))
    }

    // -------------------------------------------------------------------------
    // amCardStackExitTargets — fling destination calculation
    //
    // exitDx = ±width * 1.6 (sign = direction)
    // exitDy = currentDy + predictedDy * 0.3
    // predictedDy = dy + velocityY * 0.499 (AM_FLING_PROJECTION_SECONDS, as x)
    // -------------------------------------------------------------------------

    @Test
    fun `exitTargets exitDx is positive for right swipe`() {
        val width = 400f
        val (exitDx, _) = amCardStackExitTargets(
            dx = 100f, dy = 0f, velocityY = 0f, isRight = true, width = width
        )
        assertEquals(width * 1.6f, exitDx, 0.01f)
    }

    @Test
    fun `exitTargets exitDx is negative for left swipe`() {
        val width = 400f
        val (exitDx, _) = amCardStackExitTargets(
            dx = -100f, dy = 0f, velocityY = 0f, isRight = false, width = width
        )
        assertEquals(-(width * 1.6f), exitDx, 0.01f)
    }

    @Test
    fun `exitTargets exitDy includes vertical carry`() {
        // currentDy = 40, predictedDy = 40 + 100*0.499 = 89.9
        // exitDy = 40 + 89.9 * 0.3 = 66.97
        val (_, exitDy) = amCardStackExitTargets(
            dx = 100f, dy = 40f, velocityY = 100f, isRight = true, width = 400f
        )
        assertEquals(66.97f, exitDy, 0.01f)
    }

    @Test
    fun `exitTargets exitDy is currentDy when velocity is zero`() {
        // exitDy = currentDy + predictedDy * FLING_VERTICAL_CARRY
        // predictedDy = dy + velocityY * 0.499
        // with dy=40, velocityY=0: predictedDy=40; exitDy = 40 + 40*0.3 = 52
        val (_, exitDy) = amCardStackExitTargets(
            dx = 100f, dy = 40f, velocityY = 0f, isRight = true, width = 400f
        )
        assertEquals(52f, exitDy, 0.01f)
    }

    @Test
    fun `exitTargets exitDy is zero when dy and velocity are zero`() {
        val (_, exitDy) = amCardStackExitTargets(
            dx = 100f, dy = 0f, velocityY = 0f, isRight = true, width = 400f
        )
        assertEquals(0f, exitDy, 0.0001f)
    }

    // -------------------------------------------------------------------------
    // amCardStackDepthScale — per-depth visual scale
    //
    // scale = 0.96^depth
    // -------------------------------------------------------------------------

    @Test
    fun `depthScale is 1_0 at depth 0`() {
        assertEquals(1.0f, amCardStackDepthScale(depth = 0), 0.0001f)
    }

    @Test
    fun `depthScale is 0_96 at depth 1`() {
        assertEquals(0.96f, amCardStackDepthScale(depth = 1), 0.0001f)
    }

    @Test
    fun `depthScale is 0_9216 at depth 2`() {
        // 0.96^2 = 0.9216
        assertEquals(0.9216f, amCardStackDepthScale(depth = 2), 0.0001f)
    }

    // -------------------------------------------------------------------------
    // amCardStackDepthYOffsetPx — per-depth y offset
    //
    // yOffsetPx = DEPTH_Y_OFFSET_DP (8f) * depth * density
    // -------------------------------------------------------------------------

    @Test
    fun `depthYOffsetPx is 0 at depth 0`() {
        assertEquals(0f, amCardStackDepthYOffsetPx(depth = 0, density = 3f), 0.0001f)
    }

    @Test
    fun `depthYOffsetPx scales linearly with depth`() {
        val density = 2f
        val depth1 = amCardStackDepthYOffsetPx(depth = 1, density = density)
        val depth2 = amCardStackDepthYOffsetPx(depth = 2, density = density)
        assertEquals(depth1 * 2f, depth2, 0.0001f)
    }

    @Test
    fun `depthYOffsetPx converts dp to pixels correctly`() {
        // 8dp * 1 depth * density=3 = 24px
        assertEquals(24f, amCardStackDepthYOffsetPx(depth = 1, density = 3f), 0.0001f)
    }

    // -------------------------------------------------------------------------
    // amCardStackDepthAlpha — per-depth alpha
    //
    // alpha = 0.72^depth
    // -------------------------------------------------------------------------

    @Test
    fun `depthAlpha is 1_0 at depth 0`() {
        assertEquals(1.0f, amCardStackDepthAlpha(depth = 0), 0.0001f)
    }

    @Test
    fun `depthAlpha is 0_72 at depth 1`() {
        assertEquals(0.72f, amCardStackDepthAlpha(depth = 1), 0.0001f)
    }

    @Test
    fun `depthAlpha is 0_5184 at depth 2`() {
        // 0.72^2 = 0.5184
        assertEquals(0.5184f, amCardStackDepthAlpha(depth = 2), 0.0001f)
    }
}
