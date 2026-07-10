package io.github.jmsdnns.androidmtrls.content

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicText
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens

// ---------------------------------------------------------------------------
// Bespoke layout constants (hardcoded — NOT tokens)
// ---------------------------------------------------------------------------

/** Default dot diameter. */
private val timelineDotSize: Dp = 10.dp

/** Connector line width — heavier than hairline so the rail reads at a glance. */
private val timelineLineWidth: Dp = 2.dp

/**
 * Target y for a marker's vertical center — half a headline first line (~22sp).
 * Small markers (the dot) center on the first text line; markers taller than the
 * line (avatars) clamp to the row top instead of sagging below the text.
 */
private val timelineFirstLineCenter: Dp = 11.dp

/** Breathing room between connector ends and the marker. */
private val timelineMarkerGap: Dp = 2.dp

/** Matches AMListRow's title/subtitle block spacing. */
private val timelineTitleSubtitleSpacing: Dp = 2.dp

// ---------------------------------------------------------------------------
// AMTimelinePosition
// ---------------------------------------------------------------------------

/**
 * Where a row sits in its timeline — controls which connector segments draw.
 *
 * iOS equivalent: `IMTimelinePosition`.
 */
@Immutable
enum class AMTimelinePosition {
    /** First row: line below only. */
    FIRST,

    /** Middle row: line above and below. */
    MIDDLE,

    /** Last row: line above only. */
    LAST,

    /** Solo row: no lines. */
    ONLY;

    /** True when a connector segment draws above the marker. */
    internal val hasLineAbove: Boolean get() = this == MIDDLE || this == LAST

    /** True when a connector segment draws below the marker. */
    internal val hasLineBelow: Boolean get() = this == FIRST || this == MIDDLE
}

// ---------------------------------------------------------------------------
// AMTimelineDot — default rail marker
// ---------------------------------------------------------------------------

/**
 * Default rail marker: a small filled circle.
 *
 * Quiet gray by default; pass a semantic tint for status timelines.
 *
 * iOS equivalent: `IMTimelineDot`.
 *
 * @param tint Fill color for the circle. Defaults to [AMTheme.selectionRing] —
 *             a quiet gray that recedes on both light and dark surfaces.
 */
@Composable
fun AMTimelineDot(
    tint: Color = AMTheme.selectionRing.resolve(),
) {
    val resolvedTint = tint
    Canvas(
        modifier = Modifier
            .then(Modifier.semantics { invisibleToUser() })
            .width(timelineDotSize)
            .height(timelineDotSize),
    ) {
        drawCircle(color = resolvedTint)
    }
}

// ---------------------------------------------------------------------------
// AMTimelineRowLabel — standard title/subtitle label
// ---------------------------------------------------------------------------

/**
 * Standard title + optional subtitle label for a timeline row.
 *
 * Title renders at headline weight in [AMTheme.Text.primary]; subtitle at
 * caption size in [AMTheme.Text.secondary]. Spacing between title and subtitle
 * matches the `IMListRow` title/subtitle block (2dp).
 *
 * iOS equivalent: `IMTimelineRowStringLabel`.
 *
 * @param title    Primary text rendered at headline size.
 * @param subtitle Optional supporting text rendered at caption size.
 */
@Composable
fun AMTimelineRowLabel(
    title: String,
    subtitle: String? = null,
) {
    val isDark = LocalAMIsDarkTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(timelineTitleSubtitleSpacing)) {
        BasicText(
            text = title,
            style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
        )
        if (!subtitle.isNullOrEmpty()) {
            BasicText(
                text = subtitle,
                style = AMTheme.Type.caption.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Private connector segment
// ---------------------------------------------------------------------------

/**
 * A vertical 2dp-wide bar: [AMTheme.separator]-colored when [visible], fully
 * transparent when not. Keeping the invisible variant preserves identical
 * layout geometry across all [AMTimelinePosition] variants — same approach as
 * iOS `Color.clear`.
 */
@Composable
private fun AMConnector(
    visible: Boolean,
    modifier: Modifier = Modifier,
    height: Dp? = null,
    lineWidth: Dp = timelineLineWidth,
) {
    val isDark = LocalAMIsDarkTheme.current
    val color = if (visible) AMTheme.separator.resolve(isDark) else Color.Transparent
    Box(
        modifier = modifier
            .then(if (height != null) Modifier.height(height) else Modifier)
            .width(lineWidth)
            .background(color),
    )
}

// ---------------------------------------------------------------------------
// AMTimelineRow — designated init (custom marker + content)
// ---------------------------------------------------------------------------

/**
 * A single entry in a vertical timeline rail.
 *
 * Pairs a slot for the rail marker (left column) with a slot for arbitrary
 * content (right column), connected via a continuous vertical line. No surface
 * fill is applied — wrap the enclosing timeline in [AMCard] with
 * [AMPadding.block] for the elevated treatment.
 *
 * **Marker alignment:**
 * The rail column derives its top inset from the marker's measured height so
 * the marker center lands on the first text line; markers taller than the line
 * (avatars) clamp to the row top so they never sag below the text. The inset
 * is updated via [onSizeChanged] on the marker Box — this fires once after
 * layout and triggers a single recompose to refine the position. Default is
 * [timelineDotSize] (10dp) so the standard dot is correctly placed on the
 * first frame.
 *
 * **Row spacing:**
 * Bottom padding is added to the content column only when there is a connector
 * below ([AMTimelinePosition.hasLineBelow]). The last (or only) row has no
 * trailing padding — callers supply card padding instead.
 *
 * iOS equivalent: `IMTimelineRow` designated init.
 *
 * @param position Where this row sits in the rail. Controls which connector
 *                 segments are drawn. Defaults to [AMTimelinePosition.MIDDLE].
 * @param marker   Composable placed in the left rail column (dot, avatar…).
 * @param content  Composable placed in the right content column.
 */
@Composable
fun AMTimelineRow(
    position: AMTimelinePosition = AMTimelinePosition.MIDDLE,
    marker: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val tokens = LocalAMTokens.current
    val density = LocalDensity.current

    // Measured marker height in pixels, updated by onSizeChanged.
    // Default to the dot size so the first frame is correct for the standard dot.
    var markerHeightPx by remember { mutableIntStateOf(with(density) { timelineDotSize.roundToPx() }) }

    // Top inset that places the marker center on the first text line.
    // Clamped to 0 so large markers (avatars) sit flush at the row top.
    val markerTopInset: Dp = with(density) {
        val markerHeightDp = markerHeightPx.toDp()
        maxOf(0.dp, timelineFirstLineCenter - markerHeightDp / 2f - timelineMarkerGap)
    }

    Row(
        modifier = Modifier.height(IntrinsicSize.Max),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        // ── Rail column ───────────────────────────────────────────────────────
        // Fixed top segment → gap → marker → gap → flexible bottom segment.
        // Using Color.Transparent on the invisible connector (not removing it)
        // keeps layout geometry identical across all position variants.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight(),
        ) {
            // Top connector (fixed height = markerTopInset)
            AMConnector(
                visible = position.hasLineAbove,
                height = markerTopInset,
            )

            // Gap above marker
            Spacer(Modifier.height(timelineMarkerGap))

            // Marker — measured for height on each layout pass
            Box(
                modifier = Modifier.onSizeChanged { size ->
                    markerHeightPx = size.height
                },
            ) {
                marker()
            }

            // Gap below marker
            Spacer(Modifier.height(timelineMarkerGap))

            // Bottom connector (flexible — fills remaining content height)
            AMConnector(
                visible = position.hasLineBelow,
                modifier = Modifier.weight(1f),
            )
        }

        // ── Content column ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (position.hasLineBelow) tokens.spacing.lg else 0.dp),
        ) {
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// AMTimelineRow — default-dot convenience
// ---------------------------------------------------------------------------

/**
 * Creates a timeline row using the default [AMTimelineDot] marker.
 *
 * iOS equivalent: `IMTimelineRow` default-dot convenience init.
 *
 * @param position Rail position. Defaults to [AMTimelinePosition.MIDDLE].
 * @param tint     Dot fill color. Defaults to [AMTheme.selectionRing].
 * @param content  Composable placed in the right content column.
 */
@Composable
fun AMTimelineRow(
    position: AMTimelinePosition = AMTimelinePosition.MIDDLE,
    tint: Color = AMTheme.selectionRing.resolve(),
    content: @Composable () -> Unit,
) {
    AMTimelineRow(
        position = position,
        marker = { AMTimelineDot(tint = tint) },
        content = content,
    )
}

// ---------------------------------------------------------------------------
// AMTimelineRow — string convenience
// ---------------------------------------------------------------------------

/**
 * Creates a timeline row with the default dot marker and a standard
 * title/subtitle label.
 *
 * Content parameters lead; configuration follows — per the param-order rule.
 *
 * iOS equivalent: `IMTimelineRow` string convenience init.
 *
 * @param title    Primary text rendered at headline size.
 * @param subtitle Optional supporting text rendered at caption size.
 * @param position Rail position. Defaults to [AMTimelinePosition.MIDDLE].
 * @param tint     Dot fill color. Defaults to [AMTheme.selectionRing].
 */
@Composable
fun AMTimelineRow(
    title: String,
    subtitle: String? = null,
    position: AMTimelinePosition = AMTimelinePosition.MIDDLE,
    tint: Color = AMTheme.selectionRing.resolve(),
) {
    AMTimelineRow(
        position = position,
        marker = { AMTimelineDot(tint = tint) },
        content = { AMTimelineRowLabel(title = title, subtitle = subtitle) },
    )
}

// ---------------------------------------------------------------------------
// AMTimeline — data-driven container
// ---------------------------------------------------------------------------

/**
 * Vertical rail of timeline entries driven by a [List].
 *
 * Snapshots the list at call time and derives each row's [AMTimelinePosition]
 * automatically — callers never set FIRST / MIDDLE / LAST / ONLY by hand.
 *
 * Rows stack with `spacing = 0`; inter-row breathing room comes from the
 * bottom padding inside each row's content column.
 *
 * No surface fill is applied — wrap in [AMCard] with [AMPadding.block] for
 * the elevated card treatment.
 *
 * iOS equivalent: `IMTimeline` designated init.
 *
 * @param data    The list of items to display.
 * @param marker  Composable factory for the left-rail marker, given each item.
 * @param content Composable factory for the right content column, given each item.
 */
@Composable
fun <T> AMTimeline(
    data: List<T>,
    marker: @Composable (T) -> Unit,
    content: @Composable (T) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        data.forEachIndexed { index, item ->
            AMTimelineRow(
                position = positionFor(index, data.size),
                marker = { marker(item) },
                content = { content(item) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// AMTimeline — default-dot convenience
// ---------------------------------------------------------------------------

/**
 * Creates a data-driven timeline using the default [AMTimelineDot] marker for
 * every row.
 *
 * iOS equivalent: `IMTimeline` default-dot container convenience init.
 *
 * @param data    The list of items to display.
 * @param content Composable factory for the right content column, given each item.
 */
@Composable
fun <T> AMTimeline(
    data: List<T>,
    content: @Composable (T) -> Unit,
) {
    AMTimeline(
        data = data,
        marker = { AMTimelineDot() },
        content = content,
    )
}

// ---------------------------------------------------------------------------
// Position helper
// ---------------------------------------------------------------------------

private fun positionFor(index: Int, total: Int): AMTimelinePosition = when {
    total == 1  -> AMTimelinePosition.ONLY
    index == 0  -> AMTimelinePosition.FIRST
    index == total - 1 -> AMTimelinePosition.LAST
    else        -> AMTimelinePosition.MIDDLE
}

// ---------------------------------------------------------------------------
// Previews (light + dark, as required by conventions)
// ---------------------------------------------------------------------------

@Preview(
    name = "AMTimeline · Light",
    group = "AMTimeline",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
)
@Composable
private fun AMTimelinePreviewLight() {
    AMThemeRoot {
        AMTimelinePreviewContent()
    }
}

@Preview(
    name = "AMTimeline · Dark",
    group = "AMTimeline",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMTimelinePreviewDark() {
    AMThemeRoot {
        AMTimelinePreviewContent()
    }
}

@Composable
private fun AMTimelinePreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    data class Event(val id: Int, val title: String, val subtitle: String?)

    val events = listOf(
        Event(0, "Order placed",      "Jun 10, 9:02 AM"),
        Event(1, "Payment confirmed", "Jun 10, 9:03 AM"),
        Event(2, "Dispatched",        "Jun 10, 2:14 PM"),
        Event(3, "Out for delivery",  null),
    )

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // 1. Default-dot convenience — 4 events
        AMCard(
            modifier = Modifier.fillMaxWidth(),
            padding = AMPadding.block,
        ) {
            AMTimeline(
                data = events,
            ) { event ->
                AMTimelineRowLabel(title = event.title, subtitle = event.subtitle)
            }
        }

        // 2. Custom-marker timeline — per-row tint to show semantic color
        AMCard(
            modifier = Modifier.fillMaxWidth(),
            padding = AMPadding.block,
        ) {
            AMTimeline(
                data = events,
                marker = { event ->
                    AMTimelineDot(
                        tint = if (event.id == 1) AMTheme.Semantic.success.resolve(isDark)
                               else AMTheme.selectionRing.resolve(isDark),
                    )
                },
                content = { event ->
                    AMTimelineRowLabel(title = event.title, subtitle = event.subtitle)
                },
            )
        }

        // 3. Solo row — ONLY position (no lines)
        AMCard(
            modifier = Modifier.fillMaxWidth(),
            padding = AMPadding.block,
        ) {
            AMTimelineRow(
                title = "Solo event",
                subtitle = "Single entry — no rail lines",
                position = AMTimelinePosition.ONLY,
            )
        }
    }
}
