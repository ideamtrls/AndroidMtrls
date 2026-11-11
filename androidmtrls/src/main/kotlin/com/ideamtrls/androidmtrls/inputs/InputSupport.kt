package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import kotlin.math.max
import kotlinx.coroutines.flow.first

/** What a screen reader says for the clear button of a text input. */
internal const val InputClearLabel: String = "Clear"

/** Material's alpha for disabled content (a disabled row's title). */
internal const val InputDisabledAlpha: Float = 0.38f

/**
 * The trailing clear button of a text input: Material's icon button with the filled `cancel`
 * glyph, in the field's trailing icon color, that empties [state]. Callers show it only while the
 * field holds text and is enabled.
 */
@Composable
internal fun InputClearButton(state: TextFieldState) {
    IconButton(onClick = { state.clearText() }) {
        Icon(painterResource(R.drawable.am_ic_cancel), contentDescription = InputClearLabel)
    }
}

/**
 * Focuses the text input this modifier goes on as it enters the composition, per [autofocus]:
 * nothing for [AMTextFieldAutofocus.Off]; otherwise the field takes focus (raising the keyboard)
 * and the caret goes to the end or the start of the text in [state]. Put it in the field's
 * modifier, ahead of the field's own focus target.
 */
@Composable
internal fun inputAutofocus(state: TextFieldState, autofocus: AMTextFieldAutofocus): Modifier {
    if (autofocus == AMTextFieldAutofocus.Off) return Modifier
    val requester = remember { FocusRequester() }
    val window = LocalWindowInfo.current
    LaunchedEffect(requester) {
        // A field in a new window (a sheet) waits until that window has focus, so the keyboard is
        // that window's and back closes the keyboard before the window.
        snapshotFlow { window.isWindowFocused }.first { it }
        requester.requestFocus()
        state.edit {
            if (autofocus == AMTextFieldAutofocus.CaretAtStart) selection = TextRange.Zero else placeCursorAtEnd()
        }
    }
    return Modifier.focusRequester(requester)
}

/**
 * A labeled row on the row recipe with a control at its end: [label] (in `bodyLarge`, `onSurface`,
 * or Material's disabled color when [enabled] is false) at the start, [control] at the end,
 * `spacing.md` apart and centered on each other. When the control and the label's longest word
 * can't share the width (large text), the control moves under the label, `spacing.sm` below it,
 * start-aligned, and is offered the full width so it can wrap.
 *
 * [label] is laid out in a row, its parts `spacing.sm` apart (a glyph, then the text). [control]
 * must emit exactly one layout.
 */
@Composable
internal fun InputControlRow(
    enabled: Boolean,
    modifier: Modifier,
    label: @Composable RowScope.() -> Unit,
    control: @Composable () -> Unit,
) {
    val spacing = AMTheme.tokens.spacing
    val labelColor = MaterialTheme.colorScheme.onSurface.let { if (enabled) it else it.copy(alpha = InputDisabledAlpha) }
    val gap = spacing.md
    val stackGap = spacing.sm
    Layout(
        contents = listOf(
            {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    CompositionLocalProvider(LocalContentColor provides labelColor) {
                        ProvideTextStyle(MaterialTheme.typography.bodyLarge) { label() }
                    }
                }
            },
            control,
        ),
        modifier = modifier.amRowPadding(),
    ) { (labelMeasurables, controlMeasurables), constraints ->
        val labelMeasurable = labelMeasurables.first()
        val controlMeasurable = controlMeasurables.first()
        val gapPx = gap.roundToPx()
        val controlWidth = controlMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        val labelMinWidth = labelMeasurable.minIntrinsicWidth(Constraints.Infinity)
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            labelMeasurable.maxIntrinsicWidth(Constraints.Infinity) + gapPx + controlWidth
        }
        if (labelMinWidth + gapPx + controlWidth <= width) {
            val controlPlaceable = controlMeasurable.measure(Constraints(maxWidth = controlWidth))
            val labelPlaceable = labelMeasurable.measure(
                Constraints(maxWidth = (width - gapPx - controlPlaceable.width).coerceAtLeast(0)),
            )
            val height = constraints.constrainHeight(max(labelPlaceable.height, controlPlaceable.height))
            layout(width, height) {
                labelPlaceable.placeRelative(0, (height - labelPlaceable.height) / 2)
                controlPlaceable.placeRelative(width - controlPlaceable.width, (height - controlPlaceable.height) / 2)
            }
        } else {
            val labelPlaceable = labelMeasurable.measure(Constraints(maxWidth = width))
            val controlPlaceable = controlMeasurable.measure(Constraints(maxWidth = width))
            val stackGapPx = stackGap.roundToPx()
            val height = constraints.constrainHeight(labelPlaceable.height + stackGapPx + controlPlaceable.height)
            layout(width, height) {
                labelPlaceable.placeRelative(0, 0)
                controlPlaceable.placeRelative(0, labelPlaceable.height + stackGapPx)
            }
        }
    }
}
