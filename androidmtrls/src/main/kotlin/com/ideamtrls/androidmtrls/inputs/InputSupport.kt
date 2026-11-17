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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlinx.coroutines.flow.first

/** What a screen reader says for the clear button of a text input. */
internal const val InputClearLabel: String = "Clear"

/** Material's alpha for disabled content (a disabled row's title). */
internal const val InputDisabledAlpha: Float = 0.38f

/**
 * The trailing clear button of a text input: Material's icon button with the filled `cancel`
 * glyph, in the field's trailing icon color, that empties [state]. A form field shows it only
 * while it's focused, enabled and holds text; the search field whenever it's enabled and holds a
 * query.
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
 * The container of an outlined text input ([AMTextField], [AMSecureField], [AMTextEditor]), filled
 * by the library under Material's outline rather than by Material. Material clips its own
 * container to the floating label's notch, which would cut a label-sized box out of a fill that
 * isn't the color behind the field (the tint, the surface color on a sheet), and the card or sheet
 * behind would show through it as a rectangle.
 *
 * - [colors]: Material's outlined colors with every container color clear, so Material draws the
 *   outline, the notch in it, and no fill.
 * - [modifier]: the fill, for the field's own modifier after the caller's. Where [fieldContainer]
 *   is [FieldContainer.Clear] it draws nothing; otherwise [fieldContainerColor] in the outlined
 *   shape over the container's rect, with no notch, under the field.
 * - [supporting]: for the supporting line's `Text`, so the fill ends where the container does,
 *   above that line.
 *
 * The container's rect rests on two facts of Material's outlined layout: the field pads its top by
 * half the floating label's line height (`bodySmall`'s, [FloatingLabelLineHeight] when that isn't
 * in sp), where the label rises above the outline; and the supporting line sits under the
 * container, [SupportingLineGap] below it. A field with no supporting line fills to its bottom.
 */
internal class OutlinedFieldContainer(
    val colors: TextFieldColors,
    val modifier: Modifier,
    val supporting: Modifier,
)

/** The [OutlinedFieldContainer] of an outlined text input here, per [fieldContainerKind]. */
@Composable
internal fun outlinedFieldContainer(): OutlinedFieldContainer {
    val line = remember { SupportingLine() }
    val supporting = remember(line) { Modifier.onPlaced { line.coordinates = it } }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
    )
    if (fieldContainerKind() == FieldContainer.Clear) return OutlinedFieldContainer(colors, Modifier, supporting)
    val labelLine = MaterialTheme.typography.bodySmall.lineHeight.takeIf { it.isSp } ?: FloatingLabelLineHeight
    val top = with(LocalDensity.current) { labelLine.toDp() / 2 }
    val fill = OutlinedFieldFillElement(fieldContainerColor(Color.Transparent), OutlinedTextFieldDefaults.shape, top, line)
    return OutlinedFieldContainer(colors, fill, supporting)
}

/** Material's line height for the floating label (`bodySmall`) when the theme's isn't in sp. */
private val FloatingLabelLineHeight = 16.sp

/** Material's gap between an outlined field's container and its supporting line. */
private val SupportingLineGap = 4.dp

/** Where an outlined field's supporting line is placed, once it's shown. */
private class SupportingLine {
    var coordinates: LayoutCoordinates? = null
}

private data class OutlinedFieldFillElement(
    val color: Color,
    val shape: Shape,
    val top: Dp,
    val line: SupportingLine,
) : ModifierNodeElement<OutlinedFieldFillNode>() {
    override fun create() = OutlinedFieldFillNode(color, shape, top, line)

    override fun update(node: OutlinedFieldFillNode) {
        node.color = color
        node.shape = shape
        node.top = top
        node.line = line
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "outlinedFieldFill"
        properties["color"] = color
        properties["shape"] = shape
        properties["top"] = top
    }
}

/**
 * Fills the outlined container under the field: [color] in [shape], from [top] (Material's room
 * for the floating label) down to the supporting line's top less [SupportingLineGap], or to the
 * field's bottom while no supporting line is placed.
 */
private class OutlinedFieldFillNode(
    var color: Color,
    var shape: Shape,
    var top: Dp,
    var line: SupportingLine,
) : Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        val top = top.roundToPx().toFloat()
        val supporting = line.coordinates?.takeIf { it.isAttached }
        val bottom = if (supporting != null) {
            requireLayoutCoordinates().localPositionOf(supporting, Offset.Zero).y - SupportingLineGap.roundToPx()
        } else {
            size.height
        }
        if (bottom > top) {
            val outline = shape.createOutline(Size(size.width, bottom - top), layoutDirection, this)
            translate(top = top) { drawOutline(outline, color) }
        }
        drawContent()
    }
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
