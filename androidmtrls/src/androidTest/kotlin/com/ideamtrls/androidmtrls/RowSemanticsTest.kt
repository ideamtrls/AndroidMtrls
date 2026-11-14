package com.ideamtrls.androidmtrls

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * What TalkBack finds on the library's rows: a named tap, and every action on a row in one list on
 * the row itself, a hold's first, then the moves, then the swipes.
 */
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

    @Test
    fun aHoldsActionsComeFirstThenTheSwipes() {
        val icon = ColorPainter(Color.Gray)
        var shared = 0
        val archive = AMSwipeAction("Archive", icon, AMSemantic.Info) {}
        rule.setContent {
            AMTheme {
                AMSwipeableRow(endAction = archive) {
                    AMListRow(
                        "Harbor Lights",
                        Modifier.amHoldable(
                            onHold = {},
                            actions = listOf(
                                AMHoldAction("Share", icon) { shared++ },
                                AMHoldAction("Queue", icon, contentDescription = "Add to queue") {},
                            ),
                        ),
                    )
                }
            }
        }
        rule.waitForIdle()
        val withActions = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)).fetchSemanticsNodes()
        assertEquals("one stop carries every action", 1, withActions.size)
        val actions = withActions.single().config[SemanticsActions.CustomActions]
        assertEquals(listOf("Share", "Add to queue", "Archive"), actions.map { it.label })
        rule.runOnUiThread { actions.first().action() }
        assertEquals("a hold's action runs from TalkBack", 1, shared)
    }

    @Test
    fun aHoldsActionsComeFirstThenTheMovesThenTheSwipes() {
        val icon = ColorPainter(Color.Gray)
        val remove = AMSwipeAction("Remove", icon, AMSemantic.Destructive) {}
        rule.setContent {
            AMTheme {
                AMReorderableRowList(
                    items = listOf("One", "Two", "Three"),
                    key = { it },
                    onMove = { _, _ -> },
                    modifier = Modifier.height(600.dp),
                    endAction = { remove },
                ) { item ->
                    AMListRow(item, Modifier.amHoldable(onHold = {}, actions = listOf(AMHoldAction("Play", icon) {})))
                }
            }
        }
        rule.waitForIdle()
        val two = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
            .fetchSemanticsNodes()
            .single { node -> node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == "Two" } == true }
        assertEquals(
            listOf("Play", "Move up", "Move down", "Remove"),
            two.config[SemanticsActions.CustomActions].map { it.label },
        )
    }
}
