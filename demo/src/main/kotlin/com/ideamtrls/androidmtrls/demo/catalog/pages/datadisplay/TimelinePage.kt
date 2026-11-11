package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMTag
import com.ideamtrls.androidmtrls.AMTagTint
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMTimeline
import com.ideamtrls.androidmtrls.AMTimelineDot
import com.ideamtrls.androidmtrls.AMTimelineLabel
import com.ideamtrls.androidmtrls.AMTimelinePosition
import com.ideamtrls.androidmtrls.AMTimelineRow
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amTimelineFirstLine

internal val TimelineEntry = ComponentEntry(
    id = "timeline",
    category = CatalogCategory.DataDisplay,
    title = "Timeline",
    summary = "Vertical activity rail with markers",
    icon = R.drawable.ic_timeline,
    keywords = listOf(
        "AMTimeline", "AMTimelineRow", "AMTimelineDot", "AMTimelineLabel", "AMTimelinePosition",
        "amTimelineFirstLine", "activity", "history", "steps", "rail", "tracking",
    ),
) { TimelinePage() }

@Composable
private fun TimelinePage() {
    ComponentPage(
        TimelineEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { DraftHistory() },
    ) {
        DeliveryTracking()
    }
}

@Immutable
private class TimelineStep(val id: Int, val title: String, val time: String)

private val DraftSteps = listOf(
    TimelineStep(0, "Draft created", "7:58 AM"),
    TimelineStep(1, "Shared with the team", "8:20 AM"),
    TimelineStep(2, "Comments resolved", "9:05 AM"),
    TimelineStep(3, "Published", "9:30 AM"),
)

/** A draft's history, one step per row, on a surface strip. */
@Composable
private fun DraftHistory() {
    AMTimeline(
        DraftSteps,
        Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(AMTheme.tokens.spacing.lg),
        key = { it.id },
    ) { step ->
        AMTimelineLabel(step.title, subtitle = step.time)
    }
}

// Stand-in for your own action.
private fun openDeliveryWindow() {}

/**
 * A parcel's progress in a card: status dots, a delay with its reason, where it is now (a small
 * status line over the title), and the delivery window to open.
 */
@Composable
private fun DeliveryTracking() {
    val spacing = AMTheme.tokens.spacing
    Column(Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg))) {
        AMTimelineRow("Order placed", AMTimelinePosition.First, subtitle = "Yesterday, 4:10 PM")
        AMTimelineRow(
            "Payment confirmed",
            AMTimelinePosition.Middle,
            subtitle = "Yesterday, 4:12 PM",
            dotColor = AMSemantic.Info.color,
        )
        AMTimelineRow(AMTimelinePosition.Middle, marker = { AMTimelineDot(color = AMSemantic.Warning.color) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                AMTimelineLabel("Delayed at depot", Modifier.weight(1f), subtitle = "Yesterday, 8:05 PM")
                AMTag("Weather", icon = painterResource(R.drawable.ic_rainy), tint = AMTagTint.Warning)
            }
        }
        AMTimelineRow(AMTimelinePosition.Middle, marker = { AMTimelineDot(color = AMSemantic.Success.color) }) {
            Text(
                "Now · 9:41 AM",
                Modifier.amTimelineFirstLine(),
                color = AMSemantic.Success.color,
                style = MaterialTheme.typography.labelMedium,
            )
            Text("Out for delivery", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
        }
        AMTimelineRow(AMTimelinePosition.Last) {
            AMTimelineLabel(
                "Delivery window",
                Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = ::openDeliveryWindow),
                subtitle = "11:00 AM – 1:00 PM",
            )
        }
    }
}
