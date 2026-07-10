package io.github.jmsdnns.androidmtrls.tokens

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Unit tests for the Reduce-Motion resolver pair on [AMTokensMotion]
 * ([resolved] / [resolvedOrNull]).
 *
 * Pure JVM tests — no Android framework or Compose runtime required.
 * [AMTokensMotion] is a plain data class and both resolvers are pure functions.
 * The reduce-motion fallback of [resolved] mirrors [AMTheme.Motion.overlayFade]
 * (`tween(180, EaseInOut)`); [TweenSpec] has value `equals`, so the
 * fallback can be asserted structurally.
 */
class MotionTest {

    private val motion = AMTokensMotion()

    /** The overlayFade mirror the resolver collapses to under Reduce Motion. */
    private val overlayFadeMirror = tween<Float>(durationMillis = 180, easing = EaseInOut)

    // -------------------------------------------------------------------------
    // resolved — full spring passes through when Reduce Motion is off
    // -------------------------------------------------------------------------

    @Test
    fun `resolved returns the same full spec when reduceMotion is false`() {
        val full: AnimationSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 384f)

        assertSame(full, motion.resolved(full, reduceMotion = false))
    }

    // -------------------------------------------------------------------------
    // resolved — collapses to the overlayFade mirror when Reduce Motion is on
    // -------------------------------------------------------------------------

    @Test
    fun `resolved returns the overlayFade mirror when reduceMotion is true`() {
        val full: AnimationSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 384f)

        assertEquals(overlayFadeMirror, motion.resolved(full, reduceMotion = true))
    }

    // -------------------------------------------------------------------------
    // resolvedOrNull — full spec passes through / becomes null
    // -------------------------------------------------------------------------

    @Test
    fun `resolvedOrNull returns the same full spec when reduceMotion is false`() {
        val full: AnimationSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 321f)

        assertSame(full, motion.resolvedOrNull(full, reduceMotion = false))
    }

    @Test
    fun `resolvedOrNull returns null when reduceMotion is true`() {
        val full: AnimationSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 321f)

        assertNull(motion.resolvedOrNull(full, reduceMotion = true))
    }

    // -------------------------------------------------------------------------
    // Generic over T — the resolvers route Float, Dp, and any typed spec alike
    // -------------------------------------------------------------------------

    @Test
    fun `resolved is generic over AnimationSpec of Dp`() {
        val full: AnimationSpec<Dp> = spring(dampingRatio = 0.86f, stiffness = 224f)

        assertSame(full, motion.resolved(full, reduceMotion = false))
        // Under Reduce Motion the Dp overload collapses to the Dp overlayFade mirror.
        assertEquals(
            tween<Dp>(durationMillis = 180, easing = EaseInOut),
            motion.resolved(full, reduceMotion = true)
        )
    }

    @Test
    fun `resolvedOrNull is generic over AnimationSpec of Dp`() {
        val full: AnimationSpec<Dp> = spring(dampingRatio = 0.86f, stiffness = 224f)

        assertSame(full, motion.resolvedOrNull(full, reduceMotion = false))
        assertNull(motion.resolvedOrNull(full, reduceMotion = true))
    }
}
