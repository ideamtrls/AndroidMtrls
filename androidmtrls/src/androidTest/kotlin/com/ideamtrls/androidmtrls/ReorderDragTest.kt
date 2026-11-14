package com.ideamtrls.androidmtrls

import android.view.ViewConfiguration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Dragging in a reorderable list with real touches: what lifts a row (the handle, and a hold unless
 * holds are the row's own), and how its steps reach the caller.
 */
class ReorderDragTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val names = listOf("One", "Two", "Three", "Four", "Five")

    private fun setList(holdToLift: Boolean = true, onHold: () -> Unit = {}, onMove: (Int, Int) -> Unit) {
        rule.setContent {
            AMTheme {
                AMReorderableRowList(
                    items = names,
                    key = { it },
                    onMove = onMove,
                    modifier = Modifier.height(600.dp).testTag(List),
                    holdToLift = holdToLift,
                ) { name ->
                    AMListRow(name, Modifier.amHoldable(onHold = onHold))
                }
            }
        }
        rule.waitForIdle()
        // A lifted row waits on every frame (its auto-scroll), so frames go by hand.
        rule.mainClock.autoAdvance = false
    }

    private fun centerY(name: String): Float {
        val list = rule.onNodeWithTag(List).fetchSemanticsNode().boundsInRoot
        return rule.onNodeWithText(name).fetchSemanticsNode().boundsInRoot.center.y - list.top
    }

    /** Where a touch on [name]'s row lands: on its handle, or on its text. */
    private fun spot(name: String, onHandle: Boolean): Offset {
        val width = rule.onNodeWithTag(List).fetchSemanticsNode().size.width
        val x = with(rule.density) { if (onHandle) width - 28.dp.toPx() else 48.dp.toPx() }
        return Offset(x, centerY(name))
    }

    /** Presses [name]'s row off its handle and holds it past the long-press timeout. */
    private fun hold(name: String) {
        val at = spot(name, onHandle = false)
        rule.onNodeWithTag(List).performTouchInput { down(at) }
        rule.mainClock.advanceTimeBy(ViewConfiguration.getLongPressTimeout() + 200L)
    }

    private fun release() {
        rule.onNodeWithTag(List).performTouchInput { up() }
        rule.mainClock.advanceTimeBy(1_000)
    }

    /** Moves the finger down by [distance] in small steps, a frame each. */
    private fun dragDown(distance: Float) {
        val steps = 40
        repeat(steps) {
            rule.onNodeWithTag(List).performTouchInput { moveBy(Offset(0f, distance / steps)) }
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun aDragStepsPastRowsTheCallerHasNotMovedYet() {
        val moves = mutableListOf<Pair<Int, Int>>()
        // The caller records the steps and never moves its list, as a slow store would.
        setList { from, to -> moves += from to to }
        val row = centerY("Two") - centerY("One")
        val at = spot("One", onHandle = true)
        rule.onNodeWithTag(List).performTouchInput { down(at) }
        dragDown(row * 3.4f)
        release()
        assertEquals(listOf(0 to 1, 1 to 2, 2 to 3), moves)
        assertTrue("the list shows the drag's order", centerY("One") > centerY("Four"))
        assertTrue("and keeps the rest in place", centerY("One") < centerY("Five"))
    }

    @Test
    fun aHoldLiftsARowByDefault() {
        val moves = mutableListOf<Pair<Int, Int>>()
        setList { from, to -> moves += from to to }
        val row = centerY("Two") - centerY("One")
        hold("One")
        dragDown(row * 1.4f)
        release()
        assertEquals(listOf(0 to 1), moves)
    }

    @Test
    fun withoutHoldToLiftAHoldIsTheRowsOwn() {
        val moves = mutableListOf<Pair<Int, Int>>()
        var holds = 0
        setList(holdToLift = false, onHold = { holds++ }) { from, to -> moves += from to to }
        val row = centerY("Two") - centerY("One")
        hold("One")
        dragDown(row * 1.4f)
        release()
        assertEquals("the row's hold registers", 1, holds)
        assertEquals("and lifts nothing", emptyList<Pair<Int, Int>>(), moves)
        assertTrue(centerY("One") < centerY("Two"))
    }

    @Test
    fun withoutHoldToLiftTheHandleStillLifts() {
        val moves = mutableListOf<Pair<Int, Int>>()
        var holds = 0
        setList(holdToLift = false, onHold = { holds++ }) { from, to -> moves += from to to }
        val row = centerY("Two") - centerY("One")
        val at = spot("One", onHandle = true)
        rule.onNodeWithTag(List).performTouchInput { down(at) }
        dragDown(row * 1.4f)
        release()
        assertEquals(listOf(0 to 1), moves)
        assertEquals(0, holds)
    }

    private companion object {
        const val List = "list"
    }
}
