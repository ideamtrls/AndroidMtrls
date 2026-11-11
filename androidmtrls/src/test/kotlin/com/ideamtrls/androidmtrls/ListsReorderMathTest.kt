package com.ideamtrls.androidmtrls

import org.junit.Assert.assertEquals
import org.junit.Test

/** The reorderable lists' and the swipeable row's pure geometry. */
class ListsReorderMathTest {

    @Test
    fun stepsDownOncePastTheMiddleOfTheItemBelow() {
        val below = ReorderSpan(100f, 200f)
        assertEquals(0, reorderStep(liftedTop = 49f, liftedBottom = 149f, above = null, below = below))
        assertEquals(1, reorderStep(liftedTop = 51f, liftedBottom = 151f, above = null, below = below))
    }

    @Test
    fun stepsUpOncePastTheMiddleOfTheItemAbove() {
        val above = ReorderSpan(0f, 100f)
        assertEquals(0, reorderStep(liftedTop = 51f, liftedBottom = 151f, above = above, below = null))
        assertEquals(-1, reorderStep(liftedTop = 49f, liftedBottom = 149f, above = above, below = null))
    }

    @Test
    fun aTallItemPassingAShortOneDoesNotTradeBack() {
        // A 100 tall item at 0 drags down past a 40 tall item at 100 (middle 120).
        val top = 21f
        assertEquals(1, reorderStep(top, top + 100f, above = null, below = ReorderSpan(100f, 140f)))
        // After the trade the short item sits at 0 (middle 20) and the tall one's slot is 40..140.
        assertEquals(0, reorderStep(top, top + 100f, above = ReorderSpan(0f, 40f), below = null))
    }

    @Test
    fun aShortItemPassingATallOneDoesNotTradeBack() {
        // A 40 tall item at 100 drags up past a 100 tall item at 0 (middle 50).
        val top = 49f
        assertEquals(-1, reorderStep(top, top + 40f, above = ReorderSpan(0f, 100f), below = null))
        // After the trade the tall item sits at 40..140 (middle 90).
        assertEquals(0, reorderStep(top, top + 40f, above = null, below = ReorderSpan(40f, 140f)))
    }

    @Test
    fun theEndsOfTheListHaveNoNeighborToPass() {
        assertEquals(0, reorderStep(liftedTop = -500f, liftedBottom = -400f, above = null, below = null))
        assertEquals(0, reorderStep(liftedTop = 900f, liftedBottom = 1000f, above = null, below = null))
    }

    @Test
    fun aLiftedItemStaysInsideTheWindow() {
        assertEquals(0f, clampLiftedTop(top = -30f, size = 100f, viewportStart = 0f, viewportEnd = 500f))
        assertEquals(400f, clampLiftedTop(top = 470f, size = 100f, viewportStart = 0f, viewportEnd = 500f))
        assertEquals(120f, clampLiftedTop(top = 120f, size = 100f, viewportStart = 0f, viewportEnd = 500f))
        // The window starts above zero under top content padding.
        assertEquals(-16f, clampLiftedTop(top = -40f, size = 100f, viewportStart = -16f, viewportEnd = 500f))
    }

    @Test
    fun anItemTallerThanTheWindowIsNotHeld() {
        assertEquals(-80f, clampLiftedTop(top = -80f, size = 600f, viewportStart = 0f, viewportEnd = 500f))
    }

    @Test
    fun theListScrollsOnlyTowardTheEdgeTheItemIsDraggedTo() {
        // The item sits 30 into the bottom edge zone of 60.
        val velocity = { travel: Float ->
            reorderAutoScrollVelocity(370f, 470f, 0f, 500f, edge = 60f, maxVelocity = 1000f, travel = travel)
        }
        assertEquals(500f, velocity(10f), 0.001f)
        assertEquals(0f, velocity(-10f), 0.001f)
        assertEquals(0f, velocity(0f), 0.001f)
    }

    @Test
    fun theScrollSpeedsUpWithDepthAndCaps() {
        val down = { bottom: Float ->
            reorderAutoScrollVelocity(bottom - 100f, bottom, 0f, 500f, edge = 50f, maxVelocity = 800f, travel = 1f)
        }
        assertEquals(0f, down(450f), 0.001f)
        assertEquals(400f, down(475f), 0.001f)
        assertEquals(800f, down(500f), 0.001f)
        assertEquals(800f, down(560f), 0.001f)

        val up = { top: Float ->
            reorderAutoScrollVelocity(top, top + 100f, 0f, 500f, edge = 50f, maxVelocity = 800f, travel = -1f)
        }
        assertEquals(0f, up(50f), 0.001f)
        assertEquals(-400f, up(25f), 0.001f)
        assertEquals(-800f, up(-20f), 0.001f)
    }

    @Test
    fun noEdgeZoneNoScroll() {
        assertEquals(0f, reorderAutoScrollVelocity(400f, 500f, 0f, 500f, edge = 0f, maxVelocity = 800f, travel = 1f), 0.001f)
    }

    @Test
    fun aSwipeUncoversTheSideTheRowLeaves() {
        // Left to right: toward the end is to the right, uncovering the left.
        assertEquals(0f..120f, swipeRevealSpan(offset = 120f, width = 400f, rtl = false))
        assertEquals(280f..400f, swipeRevealSpan(offset = -120f, width = 400f, rtl = false))
        // Right to left: toward the end is to the left, uncovering the right.
        assertEquals(280f..400f, swipeRevealSpan(offset = 120f, width = 400f, rtl = true))
        assertEquals(0f..120f, swipeRevealSpan(offset = -120f, width = 400f, rtl = true))
    }

    @Test
    fun aRowAtRestUncoversNothingAndAFullSwipeEverything() {
        assertEquals(0f..0f, swipeRevealSpan(offset = 0f, width = 400f, rtl = false))
        assertEquals(0f..400f, swipeRevealSpan(offset = 900f, width = 400f, rtl = false))
        assertEquals(0f..400f, swipeRevealSpan(offset = -900f, width = 400f, rtl = false))
    }
}
