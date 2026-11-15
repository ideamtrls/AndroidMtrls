package com.ideamtrls.androidmtrls

import android.view.ViewConfiguration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Dragging in a reorderable list with real touches: what lifts a row (the handle, and a hold unless
 * holds are the row's own), how its steps reach the caller, and the list in a scrolling page.
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

    private lateinit var pageScroll: ScrollState

    /**
     * A page of art rows, 600 dp tall, scrolling a frosted card that holds the list's twelve rows:
     * the list is measured without a height limit, so it takes its page form. Touches go to the
     * page, which stays put while it scrolls.
     */
    private fun setPage(onRemove: (String) -> Unit = {}, onMove: (Int, Int) -> Unit) {
        val art = ImageBitmap(4, 4)
        val remove = ColorPainter(Color.Gray)
        rule.setContent {
            AMTheme(surfaceFill = AMSurfaceFill.Frosted) {
                pageScroll = rememberScrollState()
                AMArtRowsBackdrop(Modifier.height(600.dp).testTag(Touched)) {
                    Column(Modifier.fillMaxSize().verticalScroll(pageScroll).amArtRows()) {
                        AMReorderableRowList(
                            items = pageNames,
                            key = { it },
                            onMove = onMove,
                            modifier = Modifier.amCard().testTag(List),
                            endAction = { name -> AMSwipeAction("Remove", remove, AMSemantic.Destructive) { onRemove(name) } },
                        ) { name ->
                            AMListRow(name, Modifier.amArtRow(art))
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    /** Where the touches land: the list itself, or the page holding it once there is one. */
    private var touched = List

    private fun centerY(name: String): Float {
        val frame = rule.onNodeWithTag(touched).fetchSemanticsNode().boundsInRoot
        return rule.onNodeWithText(name).fetchSemanticsNode().boundsInRoot.center.y - frame.top
    }

    /** Where a touch on [name]'s row lands: on its handle, or on its text. */
    private fun spot(name: String, onHandle: Boolean): Offset {
        val width = rule.onNodeWithTag(touched).fetchSemanticsNode().size.width
        val x = with(rule.density) { if (onHandle) width - 28.dp.toPx() else 48.dp.toPx() }
        return Offset(x, centerY(name))
    }

    /** Presses [name]'s row off its handle and holds it past the long-press timeout. */
    private fun hold(name: String) {
        val at = spot(name, onHandle = false)
        rule.onNodeWithTag(touched).performTouchInput { down(at) }
        rule.mainClock.advanceTimeBy(ViewConfiguration.getLongPressTimeout() + 200L)
    }

    private fun release() {
        rule.onNodeWithTag(touched).performTouchInput { up() }
        rule.mainClock.advanceTimeBy(1_000)
    }

    /** Moves the finger down by [distance] in small steps, a frame each. */
    private fun dragDown(distance: Float) {
        val steps = 40
        repeat(steps) {
            rule.onNodeWithTag(touched).performTouchInput { moveBy(Offset(0f, distance / steps)) }
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

    @Test
    fun inAPageTheListRunsItsLengthAndARowHeldAtTheEdgeScrollsThePage() {
        val moves = mutableListOf<Pair<Int, Int>>()
        touched = Touched
        setPage { from, to -> moves += from to to }
        val viewport = with(rule.density) { 600.dp.toPx() }
        assertTrue("the list is as tall as its rows", rule.onNodeWithTag(List).fetchSemanticsNode().size.height > viewport)
        rule.mainClock.autoAdvance = false
        val at = spot("One", onHandle = true)
        rule.onNodeWithTag(Touched).performTouchInput { down(at) }
        // Down to the page's bottom edge, then held there while the page scrolls under the row.
        dragDown(viewport - with(rule.density) { 24.dp.toPx() } - at.y)
        repeat(90) { rule.mainClock.advanceTimeByFrame() }
        release()
        assertTrue("the page scrolled", pageScroll.value > 0)
        assertEquals("the row stepped past every row, one at a time", (0 until pageNames.lastIndex).map { it to it + 1 }, moves)
    }

    @Test
    fun inAPageARowKeepsItsSwipeAndItsMoves() {
        val removed = mutableListOf<String>()
        touched = Touched
        setPage(onRemove = { removed += it }) { _, _ -> }
        val two = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
            .fetchSemanticsNodes()
            .single { node -> node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == "Two" } == true }
        assertEquals(listOf("Move up", "Move down", "Remove"), two.config[SemanticsActions.CustomActions].map { it.label })
        // From the row's end, short of the handle, across most of the row.
        rule.onNodeWithText("Two").performTouchInput { swipeLeft(startX = right - 24.dp.toPx(), endX = left) }
        rule.waitForIdle()
        assertEquals(listOf("Two"), removed)
    }

    private val pageNames = listOf(
        "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve",
    )

    private companion object {
        const val List = "list"
        const val Touched = "touched"
    }
}
