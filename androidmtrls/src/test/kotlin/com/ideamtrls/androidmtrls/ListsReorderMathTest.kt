package com.ideamtrls.androidmtrls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** The reorderable lists' and the swipeable row's pure geometry, and the order a drag shows. */
class ListsReorderMathTest {

    private fun order(vararg keys: String) = ReorderOrder().apply { observe(keys.toList()) }

    @Test
    fun eachStepShowsAtOnceWhileTheCallersOrderIsBehind() {
        val order = order("a", "b", "c", "d")
        assertEquals(true, order.move(0, 1))
        assertEquals(true, order.move(1, 2))
        assertEquals(true, order.move(2, 3))
        assertEquals(listOf("b", "c", "d", "a"), order.shown)
        assertEquals(listOf("b", "c", "d", "a"), order.shownFor(listOf("a", "b", "c", "d")))
    }

    @Test
    fun theCallersStepsArriveLaterAndInOrder() {
        val order = order("a", "b", "c", "d")
        order.move(0, 1)
        order.move(1, 2)
        order.move(2, 3)
        // The caller's first step comes in: the drag's order holds.
        order.observe(listOf("b", "a", "c", "d"))
        assertEquals(listOf("b", "c", "d", "a"), order.shown)
        // Then its last, the one between conflated away: caught up, its own list shows.
        val caught = listOf("b", "c", "d", "a")
        assertSame(caught, order.shownFor(caught))
        order.observe(caught)
        assertSame(caught, order.shownFor(caught))
    }

    @Test
    fun aStepBeyondTheCallersShowsOnTopOfIt() {
        val order = order("a", "b", "c")
        order.move(0, 1)
        order.observe(listOf("b", "a", "c"))
        // Caught up, then a new step while the caller hasn't taken it.
        order.move(1, 2)
        assertEquals(listOf("b", "c", "a"), order.shownFor(listOf("b", "a", "c")))
        order.observe(listOf("b", "c", "a"))
        val caught = listOf("b", "c", "a")
        assertSame(caught, order.shownFor(caught))
    }

    @Test
    fun aDragThereAndBackWaitsForTheCallersWayBack() {
        val order = order("a", "b", "c")
        order.move(0, 1)
        order.move(1, 0)
        assertEquals(listOf("a", "b", "c"), order.shownFor(listOf("a", "b", "c")))
        // The caller's first step comes in: the drag's order holds.
        order.observe(listOf("b", "a", "c"))
        assertEquals(listOf("a", "b", "c"), order.shown)
        // And its second: caught up.
        val back = listOf("a", "b", "c")
        order.observe(back)
        assertSame(back, order.shownFor(back))
    }

    @Test
    fun aCallerThatMovesOnShowsItsOwnOrder() {
        val order = order("a", "b", "c")
        order.move(0, 2)
        val sorted = listOf("c", "b", "a")
        assertSame(sorted, order.shownFor(sorted))
        order.observe(sorted)
        assertEquals(sorted, order.shown)
        // A later step starts from the caller's order.
        order.move(0, 1)
        assertEquals(listOf("b", "c", "a"), order.shown)
    }

    @Test
    fun itemsTheCallerAddsOrRemovesMeanwhileKeepTheirPlaces() {
        val order = order("a", "b", "c")
        order.move(0, 2)
        // The caller adds "x" at the top and removes "b" before taking the step.
        val caller = listOf("x", "a", "c")
        assertEquals(listOf("x", "c", "a"), order.shownFor(caller))
        order.observe(caller)
        assertEquals(listOf("x", "c", "a"), order.shown)
        // Then takes it.
        val caught = listOf("x", "c", "a")
        order.observe(caught)
        assertSame(caught, order.shownFor(caught))
    }

    @Test
    fun aStepThatMovesNothingIsRefused() {
        val order = order("a", "b")
        assertEquals(false, order.move(1, 1))
        assertEquals(false, order.move(0, 2))
        assertEquals(false, order.move(-1, 0))
        val keys = listOf("a", "b")
        assertSame(keys, order.shownFor(keys))
    }

    @Test
    fun arrangeFillsTheSharedPlacesInTheGivenOrder() {
        assertEquals(listOf("c", "x", "a", "b"), reorderArrange(listOf("a", "x", "b", "c"), listOf("c", "a", "y", "b")))
        assertEquals(listOf("a"), reorderArrange(listOf("a"), emptyList()))
        assertEquals(emptyList<String>(), reorderArrange(emptyList(), listOf("a")))
    }

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
