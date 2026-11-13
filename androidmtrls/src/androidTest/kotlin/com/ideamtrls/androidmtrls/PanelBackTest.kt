package com.ideamtrls.androidmtrls

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * A panel's back button against Navigation 3: it pops its panel once, even when it's tapped again
 * while that panel animates away, and it works while the panel is still sliding in.
 */
class PanelBackTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var pops = 0

    /** A root and its panels on [stack], each panel popping with the documented recipe. */
    @Composable
    private fun Panels(stack: SnapshotStateList<String>, panel: @Composable (title: String, pop: () -> Unit) -> Unit) {
        val pop = {
            pops++
            stack.removeLastOrNull()
            Unit
        }
        NavDisplay(
            backStack = stack,
            onBack = { stack.removeLastOrNull() },
            entryProvider = { key ->
                NavEntry(key) { if (key == Root) Text("Root") else panel(key, pop) }
            },
        )
    }

    /** Where the back button is: the same spot on every panel. */
    private fun backSpot(): Offset {
        val bounds = rule.onNodeWithContentDescription(PanelBackLabel).fetchSemanticsNode().boundsInRoot
        return bounds.center
    }

    /** Taps the back button, then the same spot again while the panel pops. */
    private fun tapBackTwice() {
        val spot = backSpot()
        rule.mainClock.autoAdvance = false
        rule.onRoot().performTouchInput { click(spot) }
        rule.mainClock.advanceTimeBy(DuringTheTransition)
        rule.onRoot().performTouchInput { click(spot) }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
    }

    @Test
    fun panelBackTappedAgainWhileItsPanelPopsPopsOnce() {
        val stack = mutableStateListOf(Root, "Atlas")
        rule.setContent {
            AMTheme { Panels(stack) { title, pop -> AMPanel(title, onBack = pop) { Text("Tasks") } } }
        }
        rule.waitForIdle()

        tapBackTwice()

        assertEquals(1, pops)
        assertEquals(listOf(Root), stack.toList())
    }

    @Test
    fun artPanelBackTappedAgainWhileItsPanelPopsPopsOnce() {
        val stack = mutableStateListOf(Root, "Cobalt")
        rule.setContent {
            AMTheme {
                Panels(stack) { title, pop ->
                    AMArtPanelChrome(onBack = pop, title = title, scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior())
                }
            }
        }
        rule.waitForIdle()

        tapBackTwice()

        assertEquals(1, pops)
        assertEquals(listOf(Root), stack.toList())
    }

    @Test
    fun panelBackWorksWhileThePanelSlidesIn() {
        val stack = mutableStateListOf(Root)
        rule.setContent {
            AMTheme { Panels(stack) { title, pop -> AMPanel(title, onBack = pop) { Text("Tasks") } } }
        }
        rule.waitForIdle()

        rule.mainClock.autoAdvance = false
        rule.runOnUiThread { stack.add("Atlas") }
        rule.mainClock.advanceTimeBy(DuringTheTransition)
        rule.onRoot().performTouchInput { click(backSpot()) }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertEquals(1, pops)
        assertEquals(listOf(Root), stack.toList())
    }

    private companion object {
        const val Root = "root"

        /** Well inside Navigation 3's push and pop transitions. */
        const val DuringTheTransition = 80L
    }
}
