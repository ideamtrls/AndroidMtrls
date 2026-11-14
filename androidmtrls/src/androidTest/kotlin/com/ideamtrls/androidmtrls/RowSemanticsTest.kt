package com.ideamtrls.androidmtrls

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** What TalkBack finds on the library's rows: a named tap, and a row container's actions on the row itself. */
class RowSemanticsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun aListRowsTapCarriesItsLabel() {
        var taps = 0
        rule.setContent {
            AMTheme { AMListRow("Harbor Lights", onClickLabel = "Play", onClick = { taps++ }) }
        }
        val row = rule.onNodeWithText("Harbor Lights")
        assertEquals("Play", row.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        row.performSemanticsAction(SemanticsActions.OnClick)
        row.performClick()
        assertEquals("TalkBack's action and a touch both tap once", 2, taps)
    }

    @Test
    fun aListCardsTapCarriesItsLabel() {
        rule.setContent {
            AMTheme { AMListCard("Field notes", onClickLabel = "Open task", onClick = {}) }
        }
        assertEquals("Open task", rule.onNodeWithText("Field notes").fetchSemanticsNode().config[SemanticsActions.OnClick].label)
    }

    @Test
    fun aRowWithoutALabelKeepsMaterialsTap() {
        var taps = 0
        rule.setContent { AMTheme { AMListRow("About", onClick = { taps++ }) } }
        val row = rule.onNodeWithText("About")
        assertEquals(null, row.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        row.performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(1, taps)
    }

    @Test
    fun aProgressBarInASwipeableRowCarriesTheRowsActions() {
        val cancel = AMSwipeAction("Cancel", ColorPainter(Color.Red), AMSemantic.Destructive) {}
        rule.setContent {
            AMTheme {
                AMSwipeableRow(endAction = cancel) {
                    AMProgressView("Episode 12", progress = { 0.4f }, valueLabel = "40%", style = AMProgressViewStyle.Bar)
                }
            }
        }
        val withActions = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)).fetchSemanticsNodes()
        assertEquals("one TalkBack stop carries the actions", 1, withActions.size)
        val node = withActions.single()
        assertEquals(listOf("Cancel"), node.config[SemanticsActions.CustomActions].map { it.label })
        val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty()
        assertEquals("the stop is the bar, reading its label", true, text.contains("Episode 12"))
    }
}
