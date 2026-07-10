package io.github.jmsdnns.androidmtrls.lists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [amSwipeOpenOffset] in [SwipeActions.kt].
 *
 * Pure anchor math — no Compose runtime needed. The function computes the
 * "Open" anchor offset for [AMSwipeRow]: a **negative** pixel offset equal to
 * `-(coercedCount × paneWidthPx)`, where the action count is coerced to **at
 * least 1** before multiplying (otherwise uncapped, mirroring iOS).
 *
 * Cases covered:
 *  - one / two / three actions → −1 / −2 / −3 pane widths
 *  - coerce below one (0 and negative counts) → treated as one pane
 *  - counts above three are uncapped → −N pane widths (no clamp)
 *  - zero pane width → zero offset
 *  - the result is never positive (content only ever slides left / start-ward)
 */
class SwipeActionsTest {

    private val paneWidth = 80f

    // -------------------------------------------------------------------------
    // Honored counts 1..3
    // -------------------------------------------------------------------------

    @Test
    fun `open offset for one action is minus one pane width`() {
        assertEquals(-80f, amSwipeOpenOffset(count = 1, paneWidthPx = paneWidth), 0f)
    }

    @Test
    fun `open offset for two actions is minus two pane widths`() {
        assertEquals(-160f, amSwipeOpenOffset(count = 2, paneWidthPx = paneWidth), 0f)
    }

    @Test
    fun `open offset for three actions is minus three pane widths`() {
        assertEquals(-240f, amSwipeOpenOffset(count = 3, paneWidthPx = paneWidth), 0f)
    }

    // -------------------------------------------------------------------------
    // Clamp below one → one pane
    // -------------------------------------------------------------------------

    @Test
    fun `count of zero clamps up to one pane`() {
        assertEquals(-80f, amSwipeOpenOffset(count = 0, paneWidthPx = paneWidth), 0f)
    }

    @Test
    fun `negative count clamps up to one pane`() {
        assertEquals(-80f, amSwipeOpenOffset(count = -2, paneWidthPx = paneWidth), 0f)
    }

    // -------------------------------------------------------------------------
    // Uncapped above three (iOS parity — no upper clamp)
    // -------------------------------------------------------------------------

    @Test
    fun `count above three is uncapped`() {
        assertEquals(-400f, amSwipeOpenOffset(count = 5, paneWidthPx = paneWidth), 0f)
        assertEquals(-560f, amSwipeOpenOffset(count = 7, paneWidthPx = paneWidth), 0f)
    }

    // -------------------------------------------------------------------------
    // Zero pane width
    // -------------------------------------------------------------------------

    @Test
    fun `zero pane width yields zero offset`() {
        assertEquals(0f, amSwipeOpenOffset(count = 2, paneWidthPx = 0f), 0f)
    }

    // -------------------------------------------------------------------------
    // Invariant: offset is never positive
    // -------------------------------------------------------------------------

    @Test
    fun `open offset is never positive`() {
        assertTrue(amSwipeOpenOffset(count = 1, paneWidthPx = paneWidth) <= 0f)
        assertTrue(amSwipeOpenOffset(count = 3, paneWidthPx = paneWidth) <= 0f)
    }
}
