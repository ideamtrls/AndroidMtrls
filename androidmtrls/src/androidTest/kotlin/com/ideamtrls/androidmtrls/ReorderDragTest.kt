package com.ideamtrls.androidmtrls

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

/** Dragging in a reorderable list with real touches: what lifts a row, and how its steps reach the caller. */
class ReorderDragTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val names = listOf("One", "Two", "Three", "Four", "Five")

    private fun setList(onMove: (Int, Int) -> Unit) {
        rule.setContent {
            AMTheme {
                AMReorderableRowList(
                    items = names,
                    key = { it },
                    onMove = onMove,
                    modifier = Modifier.height(600.dp).testTag(List),
                ) { name ->
                    AMListRow(name)
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
        val handleX = with(rule.density) { rule.onNodeWithTag(List).fetchSemanticsNode().size.width - 28.dp.toPx() }
        rule.onNodeWithTag(List).performTouchInput { down(Offset(handleX, centerY("One"))) }
        dragDown(row * 3.4f)
        rule.onNodeWithTag(List).performTouchInput { up() }
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(listOf(0 to 1, 1 to 2, 2 to 3), moves)
        assertTrue("the list shows the drag's order", centerY("One") > centerY("Four"))
        assertTrue("and keeps the rest in place", centerY("One") < centerY("Five"))
    }

    private companion object {
        const val List = "list"
    }
}
