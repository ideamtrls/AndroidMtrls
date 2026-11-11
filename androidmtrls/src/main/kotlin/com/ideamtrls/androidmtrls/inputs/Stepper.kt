package com.ideamtrls.androidmtrls

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/** What a screen reader says for the stepper's minus button, before the stepper's name. */
private const val DecreaseLabel = "Decrease"

/** What a screen reader says for the stepper's plus button, before the stepper's name. */
private const val IncreaseLabel = "Increase"

/**
 * A row that steps a whole number: its title at the start, a minus and a plus button at the end.
 *
 * ```kotlin
 * var guests by rememberSaveable { mutableIntStateOf(2) }
 * AMStepper("Guests", guests, onValueChange = { guests = it }, valueRange = 1..12)
 * ```
 *
 * The string form, whose [title] also names the buttons for TalkBack ("Decrease Guests"); the slot
 * form takes any label content. Everything else is the slot form's.
 *
 * @param title the row's title, in the row title's type (`bodyLarge`).
 * @param value the current value, within [valueRange].
 * @param onValueChange called with the new value when a button is tapped.
 * @param valueRange the values the stepper moves between; a button stops at its end.
 * @param modifier the row's placement.
 * @param step how far one tap moves the value; at least 1.
 * @param enabled whether the buttons take taps.
 */
@Composable
public fun AMStepper(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    modifier: Modifier = Modifier,
    step: Int = 1,
    enabled: Boolean = true,
) {
    AMStepper(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        contentDescription = title,
        step = step,
        enabled = enabled,
    ) {
        Text(title)
    }
}

/**
 * A row that steps a whole number: its label at the start, a minus and a plus button at the end.
 *
 * ```kotlin
 * AMStepper(breakfasts, onValueChange = { breakfasts = it }, valueRange = 0..10, contentDescription = "Breakfasts") {
 *     Icon(painterResource(R.drawable.ic_coffee), contentDescription = null)
 *     Text("Breakfast × $breakfasts")
 * }
 * ```
 *
 * - **A row, not a surface.** It follows the row recipe (28 dp above and below, 16 dp at the sides)
 *   and draws no fill: stack steppers in a card with `AMDivider(inset = spacing.lg)` between them.
 * - **The buttons** are Material's filled tonal icon buttons with the `remove` and `add` glyphs, in
 *   the theme's medium shape (rounded rectangles, like every AM button), each with a 48 dp target.
 *   A tap moves the value by [step], stopping at the ends of [valueRange]; a button greys out once
 *   its end is reached.
 * - **The value isn't shown.** Put it in the label when the screen needs it ("Breakfast × 2").
 * - **Large text**: when the buttons and the label's longest word can't share the row, the
 *   buttons move under the label.
 * - **TalkBack** reads the label, then each button as "Decrease" or "Increase" followed by
 *   [contentDescription] ("Increase Breakfasts"), with the value as its state. Hide label parts
 *   that repeat it.
 *
 * Disabled (its own [enabled], or a disabling container around it), the label and buttons take
 * Material's disabled colors and the buttons ignore taps.
 *
 * @param value the current value, within [valueRange].
 * @param onValueChange called with the new value when a button is tapped.
 * @param valueRange the values the stepper moves between; a button stops at its end.
 * @param modifier the row's placement.
 * @param contentDescription the stepper's name for TalkBack, said after "Decrease" or "Increase" on
 *   each button. Null names the buttons by their action alone.
 * @param step how far one tap moves the value; at least 1.
 * @param enabled whether the buttons take taps.
 * @param label the row's label: usually a text, or a glyph then a text. It gets the row title's
 *   type (`bodyLarge`) and color, its parts `spacing.sm` apart.
 */
@Composable
public fun AMStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    step: Int = 1,
    enabled: Boolean = true,
    label: @Composable RowScope.() -> Unit,
) {
    require(step >= 1) { "A stepper's step must be at least 1, was $step." }
    val isEnabled = resolveEnabled(enabled)
    InputControlRow(enabled = isEnabled, modifier = modifier, label = label) {
        Row {
            StepperButton(
                glyph = R.drawable.am_ic_remove,
                action = DecreaseLabel,
                name = contentDescription,
                value = value,
                enabled = isEnabled && value > valueRange.first,
                onClick = { onValueChange((value.toLong() - step).coerceIn(valueRange.first.toLong(), valueRange.last.toLong()).toInt()) },
            )
            StepperButton(
                glyph = R.drawable.am_ic_add,
                action = IncreaseLabel,
                name = contentDescription,
                value = value,
                enabled = isEnabled && value < valueRange.last,
                onClick = { onValueChange((value.toLong() + step).coerceIn(valueRange.first.toLong(), valueRange.last.toLong()).toInt()) },
            )
        }
    }
}

/** One of the stepper's buttons: a tonal icon button in the medium shape, its value as its state. */
@Composable
private fun StepperButton(@DrawableRes glyph: Int, action: String, name: String?, value: Int, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.semantics { stateDescription = value.toString() },
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
    ) {
        Icon(painterResource(glyph), contentDescription = if (name != null) "$action $name" else action)
    }
}
