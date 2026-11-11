package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import kotlin.math.max
import kotlin.math.min

/**
 * A label and its value on one row: Material's `ListItem` at the row recipe's rhythm, [label] at
 * the start in `bodyLarge` and [value] at the end in `bodyLarge` and `onSurfaceVariant`, read as
 * a value rather than as a list item's small trailing note.
 *
 * ```kotlin
 * Column(Modifier.fillMaxWidth().amCard()) {
 *     AMLabeledContent("Plan", "Pro")
 *     AMDivider(inset = AMTheme.tokens.spacing.lg)
 *     AMLabeledContent("Address", "214 Van Brunt St, Brooklyn, NY 11231")
 * }
 * ```
 *
 * The label keeps its own width, on one line, and the value takes the rest of the row, at its
 * end, wrapping onto lines aligned to the end. Only when the value's longest word wouldn't fit
 * does the label give up width, wrapping between its words, never inside one. The two line up on
 * their first lines.
 *
 * The row only shows; it takes no taps. Like [AMListRow] it fills itself with
 * [AMTheme.Surface.primary] standing on the page and draws no fill inside a card. A screen reader
 * reads the label and the value together.
 *
 * @param label what the value is.
 * @param value the value.
 * @param modifier the row's placement.
 */
@Composable
public fun AMLabeledContent(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    AMLabeledContent(label, modifier) {
        Text(value)
    }
}

/**
 * A label and a custom value on one row, such as a status tag. The value gets `bodyLarge`, lines
 * aligned to the end and `onSurfaceVariant`, so text inside it reads as a value. Otherwise the
 * same row as the string form: the label at its own width, the value in the rest at the end.
 *
 * @param label what the value is.
 * @param modifier the row's placement.
 * @param value the value, at the row's end.
 */
@Composable
public fun AMLabeledContent(
    label: String,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
) {
    val rowModifier = modifier
        .fillMaxWidth()
        .amFlatSurface()
        .then(claimRowActions())
    CompositionLocalProvider(LocalAMRowActions provides null) {
        ListItem(
            modifier = rowModifier,
            colors = AMRowDefaults.colors(),
            shapes = AMRowDefaults.shapes(),
            contentPadding = AMRowDefaults.contentPadding,
        ) {
            LabeledContentLayout(label, value)
        }
    }
}

/**
 * The label and the value side by side, `spacing.lg` apart: the label at the width
 * [labeledContentLabelWidth] gives it, the value in the rest, placed at the end. Their first
 * baselines line up when both have one; otherwise they center on each other.
 */
@Composable
private fun LabeledContentLayout(label: String, value: @Composable () -> Unit) {
    val gap = AMTheme.tokens.spacing.lg
    val valueColor = MaterialTheme.colorScheme.onSurfaceVariant
    val valueStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End)
    Layout(
        contents = listOf(
            { Text(label) },
            {
                Box {
                    CompositionLocalProvider(LocalContentColor provides valueColor) {
                        ProvideTextStyle(valueStyle, value)
                    }
                }
            },
        ),
    ) { (labelMeasurables, valueMeasurables), constraints ->
        val labelMeasurable = labelMeasurables.first()
        val valueMeasurable = valueMeasurables.first()
        val gapPx = gap.roundToPx()
        val labelNatural = labelMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            labelNatural + gapPx + valueMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        }
        val room = (width - gapPx).coerceAtLeast(0)
        val labelWidth = labeledContentLabelWidth(
            labelNatural = labelNatural,
            labelLongestWord = labelMeasurable.minIntrinsicWidth(Constraints.Infinity),
            valueLongestWord = valueMeasurable.minIntrinsicWidth(Constraints.Infinity),
            room = room,
        )
        val labelPlaceable = labelMeasurable.measure(Constraints(maxWidth = labelWidth))
        val valuePlaceable = valueMeasurable.measure(Constraints(maxWidth = (room - labelPlaceable.width).coerceAtLeast(0)))

        val labelBaseline = labelPlaceable[FirstBaseline]
        val valueBaseline = valuePlaceable[FirstBaseline]
        val labelY: Int
        val valueY: Int
        val height: Int
        if (labelBaseline != AlignmentLine.Unspecified && valueBaseline != AlignmentLine.Unspecified) {
            val shift = valueBaseline - labelBaseline
            labelY = max(shift, 0)
            valueY = max(-shift, 0)
            height = constraints.constrainHeight(max(labelY + labelPlaceable.height, valueY + valuePlaceable.height))
        } else {
            height = constraints.constrainHeight(max(labelPlaceable.height, valuePlaceable.height))
            labelY = (height - labelPlaceable.height) / 2
            valueY = (height - valuePlaceable.height) / 2
        }
        layout(width, height) {
            labelPlaceable.placeRelative(0, labelY)
            valuePlaceable.placeRelative(width - valuePlaceable.width, valueY)
        }
    }
}

/**
 * How wide a labeled row's label is laid out, in pixels, beside a value, with [room] for both (the
 * row's width less the gap): its natural one-line width [labelNatural], given up only as far as the
 * value's longest word [valueLongestWord] needs, and never below the label's own longest word
 * [labelLongestWord], so it never breaks inside a word. Never wider than [room].
 */
internal fun labeledContentLabelWidth(labelNatural: Int, labelLongestWord: Int, valueLongestWord: Int, room: Int): Int =
    min(labelNatural, max(labelLongestWord, room - valueLongestWord)).coerceIn(0, max(room, 0))
