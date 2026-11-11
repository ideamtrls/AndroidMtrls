package com.ideamtrls.androidmtrls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

/**
 * Where a row sits in its timeline, which decides the rail's line segments: a line runs up from
 * the marker unless the row is first, and down from it unless the row is last.
 */
public enum class AMTimelinePosition {
    /** The first row: a line below the marker only. */
    First,

    /** A row between others: a line above and below the marker. */
    Middle,

    /** The last row: a line above the marker only. */
    Last,

    /** The only row: no line. */
    Only,
}

private val AMTimelinePosition.hasLineAbove: Boolean
    get() = this == AMTimelinePosition.Middle || this == AMTimelinePosition.Last

private val AMTimelinePosition.hasLineBelow: Boolean
    get() = this == AMTimelinePosition.First || this == AMTimelinePosition.Middle

/** The position of the row at [index] in a timeline of [count] rows. */
internal fun timelinePosition(index: Int, count: Int): AMTimelinePosition = when {
    count <= 1 -> AMTimelinePosition.Only
    index == 0 -> AMTimelinePosition.First
    index == count - 1 -> AMTimelinePosition.Last
    else -> AMTimelinePosition.Middle
}

/** The default marker's diameter. */
private val TimelineDotSize = 10.dp

/** The rail's line: heavier than a hairline, so the rail reads at a glance. */
private val TimelineLineWidth = 2.dp

/** The room between the line's ends and the marker. */
private val TimelineMarkerGap = 2.dp

/**
 * A vertical rail of entries, one row per item: a marker on the rail (a dot by default, or any
 * small view: an avatar, a glyph), joined by a continuous line, with the item's content beside
 * it. Each row's position on the rail (first, middle, last, only) comes from its index, so the
 * line starts at the first marker and ends at the last.
 *
 * ```kotlin
 * Column(Modifier.fillMaxWidth().amCard(padding = PaddingValues(AMTheme.tokens.spacing.lg))) {
 *     AMTimeline(steps, key = { it.id }) { step -> AMTimelineLabel(step.title, subtitle = step.time) }
 * }
 * ```
 *
 * It only draws, with no surface of its own: put it in a card padded `spacing.lg`
 * (`Modifier.amCard(padding = …)`) for the floating treatment, or on a surface strip. Rows sit
 * `spacing.lg` apart, the space the line bridges. For rows built by hand, use [AMTimelineRow].
 *
 * Markers in one timeline should share a width, so the rail stays straight.
 *
 * @param items the entries, top to bottom.
 * @param modifier the timeline's placement.
 * @param key a stable key for each item, so a row keeps its state when the list changes. Null
 *   keys rows by their place.
 * @param marker the rail's marker for an item: [AMTimelineDot] by default.
 * @param content an item's content beside its marker, usually an [AMTimelineLabel].
 */
@Composable
public fun <T> AMTimeline(
    items: List<T>,
    modifier: Modifier = Modifier,
    key: ((item: T) -> Any)? = null,
    marker: @Composable (item: T) -> Unit = { AMTimelineDot() },
    content: @Composable (item: T) -> Unit,
) {
    Column(modifier) {
        items.forEachIndexed { index, item ->
            val position = timelinePosition(index, items.size)
            if (key == null) {
                AMTimelineRow(position, marker = { marker(item) }) { content(item) }
            } else {
                androidx.compose.runtime.key(key(item)) {
                    AMTimelineRow(position, marker = { marker(item) }) { content(item) }
                }
            }
        }
    }
}

/**
 * One timeline entry, built by hand: [marker] on the rail and [content] beside it, with the
 * rail's line drawn above and below the marker as [position] says. Stack rows in a column with no
 * spacing; the line runs on through each row's bottom space into the next.
 *
 * ```kotlin
 * Column {
 *     AMTimelineRow("Order placed", AMTimelinePosition.First, subtitle = "Yesterday, 4:10 PM")
 *     AMTimelineRow(AMTimelinePosition.Last, marker = { AMTimelineDot(color = AMSemantic.Success.color) }) {
 *         Text("Now · 9:41 AM", Modifier.amTimelineFirstLine(), style = MaterialTheme.typography.labelMedium)
 *         Text("Out for delivery", style = MaterialTheme.typography.bodyLarge)
 *     }
 * }
 * ```
 *
 * - **Marker alignment.** The marker centers on the content's first line of text: a line marked
 *   with [Modifier.amTimelineFirstLine][amTimelineFirstLine] ([AMTimelineLabel]'s title marks
 *   itself), or else a line of `bodyLarge`. A marker taller than that line (an avatar) sits flush
 *   with the row's top instead of sagging below the text. It tracks the font scale.
 * - **Spacing.** The content sits `spacing.md` after the rail. A row with a line below it keeps
 *   `spacing.lg` under its content for the line to bridge; the last row keeps none, so its card's
 *   padding is the only space under it.
 * - **Lines.** 2 dp wide in `outlineVariant`, the divider color, with 2 dp of room at the marker.
 *
 * @param position where the row sits on the rail.
 * @param modifier the row's placement. The row fills the width it's given.
 * @param marker the rail's marker: [AMTimelineDot] by default. Its width sets the rail's.
 * @param content what the entry says, stacked in a column beside the marker.
 */
@Composable
public fun AMTimelineRow(
    position: AMTimelinePosition,
    modifier: Modifier = Modifier,
    marker: @Composable () -> Unit = { AMTimelineDot() },
    content: @Composable () -> Unit,
) {
    val spacing = AMTheme.tokens.spacing
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val bodyLarge = MaterialTheme.typography.bodyLarge
    val defaultLineHeight = when {
        bodyLarge.lineHeight.isSp -> bodyLarge.lineHeight
        bodyLarge.fontSize.isSp -> bodyLarge.fontSize * 1.5f
        else -> 24.sp
    }
    val line = @Composable { Box(Modifier.background(lineColor)) }
    val markerSlot = @Composable { Box { marker() } }
    val contentSlot = @Composable { Column { content() } }
    Layout(
        contents = listOf(line, markerSlot, line, contentSlot),
        modifier = modifier,
    ) { (above, markers, below, contents), constraints ->
        val railGap = spacing.md.roundToPx()
        val lineWidth = max(TimelineLineWidth.roundToPx(), 1)
        val markerGap = TimelineMarkerGap.roundToPx()
        val bottomPadding = if (position.hasLineBelow) spacing.lg.roundToPx() else 0

        val markerPlaceable = markers.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
        val railWidth = max(markerPlaceable.width, lineWidth)
        val contentConstraints = if (constraints.hasBoundedWidth) {
            val width = max(constraints.maxWidth - railWidth - railGap, 0)
            Constraints(minWidth = width, maxWidth = width)
        } else {
            Constraints()
        }
        val contentPlaceable = contents.single().measure(contentConstraints)

        val firstLine = contentPlaceable[TimelineFirstLine]
            .takeUnless { it == AlignmentLine.Unspecified }
            ?: (defaultLineHeight.roundToPx() / 2)
        val markerTop = max(firstLine - markerPlaceable.height / 2, 0)
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else railWidth + railGap + contentPlaceable.width
        val height = max(contentPlaceable.height + bottomPadding, markerTop + markerPlaceable.height)
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        val aboveHeight = markerTop - markerGap
        val abovePlaceable = if (position.hasLineAbove && aboveHeight > 0) {
            above.single().measure(Constraints.fixed(lineWidth, aboveHeight))
        } else {
            null
        }
        val belowTop = markerTop + markerPlaceable.height + markerGap
        val belowPlaceable = if (position.hasLineBelow && height > belowTop) {
            below.single().measure(Constraints.fixed(lineWidth, height - belowTop))
        } else {
            null
        }

        layout(width, height) {
            val lineX = (railWidth - lineWidth) / 2
            abovePlaceable?.placeRelative(lineX, 0)
            markerPlaceable.placeRelative((railWidth - markerPlaceable.width) / 2, markerTop)
            belowPlaceable?.placeRelative(lineX, belowTop)
            contentPlaceable.placeRelative(railWidth + railGap, 0)
        }
    }
}

/**
 * One timeline entry with the standard text: [title] over an optional [subtitle], beside a dot in
 * [dotColor]. Otherwise the same row as the slot form.
 *
 * ```kotlin
 * AMTimelineRow("Payment confirmed", AMTimelinePosition.Middle, subtitle = "Yesterday, 4:12 PM", dotColor = AMSemantic.Info.color)
 * ```
 *
 * @param title what happened, in `bodyLarge`.
 * @param position where the row sits on the rail.
 * @param modifier the row's placement.
 * @param subtitle a supporting line, such as a time. Null or empty shows none.
 * @param dotColor the dot's color: [AMTheme.selectionRing], a quiet gray, by default; a status
 *   role's color ([AMSemantic.color]) for a status timeline.
 * @param tabularFigures whether the text's digits take tabular figures (`tnum`), each as wide as
 *   the next, so times and counts line up from row to row. Off by default.
 */
@Composable
public fun AMTimelineRow(
    title: String,
    position: AMTimelinePosition,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    dotColor: Color = AMTheme.selectionRing,
    tabularFigures: Boolean = false,
) {
    AMTimelineRow(position, modifier, marker = { AMTimelineDot(color = dotColor) }) {
        AMTimelineLabel(title, subtitle = subtitle, tabularFigures = tabularFigures)
    }
}

/**
 * The default timeline marker: a 10 dp dot. Decorative; a screen reader skips it.
 *
 * @param modifier the dot's placement.
 * @param color the dot's color: [AMTheme.selectionRing], a quiet gray, by default; a status
 *   role's color ([AMSemantic.color]) for a status timeline.
 */
@Composable
public fun AMTimelineDot(modifier: Modifier = Modifier, color: Color = AMTheme.selectionRing) {
    Box(modifier.size(TimelineDotSize).background(color, CircleShape))
}

/**
 * A timeline entry's standard text: [title] in `bodyLarge` and `onSurface`, over an optional
 * [subtitle] in `bodyMedium` and `onSurfaceVariant`, the pair a list row uses. The title marks
 * itself as the entry's first line ([Modifier.amTimelineFirstLine][amTimelineFirstLine]), so the
 * marker centers on it.
 *
 * Use it inside custom content that wraps the standard text, such as a clickable entry:
 *
 * ```kotlin
 * AMTimelineRow(AMTimelinePosition.Last) {
 *     AMTimelineLabel("Delivery window", Modifier.fillMaxWidth().clickable(onClick = ::openWindow), subtitle = "11:00 AM – 1:00 PM")
 * }
 * ```
 *
 * An agenda whose subtitles are times turns on [tabularFigures], so the digits line up from entry
 * to entry ("9:00 – 9:45 AM" over "11:30 AM – 1:00 PM"); text without digits looks the same
 * either way.
 *
 * @param title the entry's text.
 * @param modifier the label's placement.
 * @param subtitle a supporting line under the title. Null or empty shows none.
 * @param tabularFigures whether the title's and the subtitle's digits take tabular figures
 *   (`fontFeatureSettings` `tnum`), each digit as wide as the next. Off by default, the type
 *   scale's proportional figures.
 */
@Composable
public fun AMTimelineLabel(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tabularFigures: Boolean = false,
) {
    val typography = MaterialTheme.typography
    Column(modifier) {
        Text(
            title,
            Modifier.amTimelineFirstLine(),
            color = MaterialTheme.colorScheme.onSurface,
            style = typography.bodyLarge.withFigures(tabularFigures),
        )
        if (!subtitle.isNullOrEmpty()) {
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = typography.bodyMedium.withFigures(tabularFigures),
            )
        }
    }
}

/**
 * This style with tabular figures added to its font features when [tabular], unchanged otherwise.
 * Features the style already sets are kept.
 */
private fun TextStyle.withFigures(tabular: Boolean): TextStyle {
    if (!tabular) return this
    val features = fontFeatureSettings
    return copy(fontFeatureSettings = if (features.isNullOrBlank()) TabularFigures else "$features, $TabularFigures")
}

/** The OpenType feature for tabular figures: every digit the same width. */
private const val TabularFigures = "tnum"

/**
 * Marks this text as its timeline entry's first line, so the row's marker centers on it: for
 * custom content that opens with a line other than a `bodyLarge` title, such as a small status
 * line over the title.
 *
 * ```kotlin
 * AMTimelineRow(AMTimelinePosition.Middle) {
 *     Text("Now · 9:41 AM", Modifier.amTimelineFirstLine(), style = MaterialTheme.typography.labelMedium)
 *     Text("Out for delivery", style = MaterialTheme.typography.bodyLarge)
 * }
 * ```
 *
 * Put it on the text itself; the nearest enclosing [AMTimelineRow] reads it, wherever the text
 * sits in the content, and the topmost marked line wins. It measures the text's own first line,
 * so it tracks the font scale and works for any style.
 */
public fun Modifier.amTimelineFirstLine(): Modifier = this then TimelineFirstLineElement

/** Where the content's first line of text centers, for the row's marker. The topmost wins. */
private val TimelineFirstLine = HorizontalAlignmentLine(::min)

private data object TimelineFirstLineElement : ModifierNodeElement<TimelineFirstLineNode>() {
    override fun create() = TimelineFirstLineNode()

    override fun update(node: TimelineFirstLineNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "amTimelineFirstLine"
    }
}

/**
 * Reports the center of its text's first line as [TimelineFirstLine]. A text's lines are evenly
 * spaced, so its first line's height is its height less the span between its first and last
 * baselines.
 */
private class TimelineFirstLineNode : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        val first = placeable[FirstBaseline]
        val last = placeable[LastBaseline]
        val lineHeight = if (first == AlignmentLine.Unspecified || last == AlignmentLine.Unspecified) {
            placeable.height
        } else {
            placeable.height - (last - first)
        }
        return layout(placeable.width, placeable.height, mapOf(TimelineFirstLine to lineHeight / 2)) {
            placeable.place(0, 0)
        }
    }
}
