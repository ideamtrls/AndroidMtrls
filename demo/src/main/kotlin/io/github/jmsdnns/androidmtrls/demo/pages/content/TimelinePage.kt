package io.github.jmsdnns.androidmtrls.demo.pages.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMAvatar
import io.github.jmsdnns.androidmtrls.content.AMAvatarSize
import io.github.jmsdnns.androidmtrls.content.AMTimeline
import io.github.jmsdnns.androidmtrls.content.AMTimelinePosition
import io.github.jmsdnns.androidmtrls.content.AMTimelineRow
import io.github.jmsdnns.androidmtrls.content.AMTimelineRowLabel
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Timeline catalog entry
// ---------------------------------------------------------------------------

val timelineEntry: ComponentEntry = ComponentEntry(
    id = "content-timeline",
    section = ComponentSection.Content,
    title = "Timeline",
    subtitle = "Vertical activity rail",
    intro = "Vertical rail of markers connected by a line — dots by default, any view " +
        "(avatars, icons) via the marker slot. Visual-only; wrap in AMCard for the elevated surface.",
    icon = Icons.Default.Timeline,
    content = { TimelinePage() },
)

// ---------------------------------------------------------------------------
// TimelinePage — demo composable
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMTimeline] and [AMTimelineRow].
 *
 * Two groups mirroring iOS `IMTimelinePage`:
 * - **Default** — 4 order events (last in success tint).
 * - **Markers** — 3 team events with AMAvatar sm markers.
 */
@Composable
fun TimelinePage() {
    // ── Data ──────────────────────────────────────────────────────────────────

    data class OrderEvent(val id: Int, val title: String, val subtitle: String)

    val orderEvents = listOf(
        OrderEvent(0, "Order placed",      "9:41 AM"),
        OrderEvent(1, "Payment confirmed", "9:43 AM"),
        OrderEvent(2, "Shipped",           "2:15 PM"),
        OrderEvent(3, "Delivered",         "5:30 PM"),
    )

    data class TeamEvent(
        val id: Int,
        val initials: String,
        val name: String,
        val action: String,
        val time: String,
    )

    val teamEvents = listOf(
        TeamEvent(0, "JD", "Jane Doe",     "commented",        "2h ago"),
        TeamEvent(1, "AR", "Alex Rivera",  "merged a branch",  "4h ago"),
        TeamEvent(2, "TS", "Taylor Smith", "closed the issue", "6h ago"),
    )

    // ── Default ───────────────────────────────────────────────────────────────
    catalogGroup(title = "Default") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            AMCard(
                modifier = Modifier.fillMaxWidth(),
                padding = AMPadding.block,
            ) {
                Column {
                    AMTimelineRow(
                        title = orderEvents[0].title,
                        subtitle = orderEvents[0].subtitle,
                        position = AMTimelinePosition.FIRST,
                    )
                    AMTimelineRow(
                        title = orderEvents[1].title,
                        subtitle = orderEvents[1].subtitle,
                        position = AMTimelinePosition.MIDDLE,
                    )
                    AMTimelineRow(
                        title = orderEvents[2].title,
                        subtitle = orderEvents[2].subtitle,
                        position = AMTimelinePosition.MIDDLE,
                    )
                    AMTimelineRow(
                        title = orderEvents[3].title,
                        subtitle = orderEvents[3].subtitle,
                        position = AMTimelinePosition.LAST,
                        tint = AMTheme.Semantic.success.resolve(LocalAMIsDarkTheme.current),
                    )
                }
            }
        }
    }

    // ── Markers ───────────────────────────────────────────────────────────────
    catalogGroup(title = "Markers") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            AMCard(
                modifier = Modifier.fillMaxWidth(),
                padding = AMPadding.block,
            ) {
                AMTimeline(
                    data = teamEvents,
                    marker = { event ->
                        AMAvatar(url = null, initials = event.initials, size = AMAvatarSize.SM)
                    },
                    content = { event ->
                        AMTimelineRowLabel(
                            title = event.name,
                            subtitle = "${event.action} · ${event.time}",
                        )
                    },
                )
            }
        }
    }
}
