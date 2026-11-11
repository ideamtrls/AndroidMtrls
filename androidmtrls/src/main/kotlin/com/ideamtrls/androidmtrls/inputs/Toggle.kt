package com.ideamtrls.androidmtrls

import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A settings row with a switch at its end: Material's toggleable list item on the row recipe,
 * holding Material's `Switch` in its own shape and colors.
 *
 * ```kotlin
 * var pushAlerts by rememberSaveable { mutableStateOf(true) }
 * AMToggle("Push notifications", pushAlerts, onCheckedChange = { pushAlerts = it })
 * AMToggle("Low data mode", lowData, onCheckedChange = { lowData = it }, supportingText = "Pause automatic updates and downloads")
 * ```
 *
 * The string form; the slot form takes any [label] content. Everything else is the slot form's.
 *
 * @param title the row's title, in the row title's type (`bodyLarge`).
 * @param checked whether the switch is on.
 * @param onCheckedChange called with the new value when the row or the switch is tapped.
 * @param modifier the row's placement.
 * @param supportingText a line under the title in Material's supporting style. Null shows none.
 * @param enabled whether the row takes taps.
 */
@Composable
public fun AMToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
    AMToggle(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        supportingContent = supportingText?.let { { Text(it) } },
        enabled = enabled,
    ) {
        Text(title)
    }
}

/**
 * A settings row with a switch at its end: Material's toggleable list item on the row recipe,
 * holding Material's `Switch` in its own shape and colors.
 *
 * ```kotlin
 * AMToggle(syncOverCellular, onCheckedChange = { syncOverCellular = it }) { Text("Sync over cellular") }
 * ```
 *
 * - **A row, not a surface.** It follows the row recipe (`AMRowDefaults`: 28 dp above and below,
 *   16 dp at the sides) and lies flat on its card or strip in every state: checked, pressed or
 *   disabled, the row never tints; the switch shows the state. Stack toggles in a card with
 *   `AMDivider(inset = spacing.lg)` between them.
 * - **The whole row is the toggle**, with Material's ripple across it; the switch sits at the end
 *   at every text size, while the label wraps.
 * - TalkBack reads the row as one switch: its label, then on or off.
 *
 * Disabled (its own [enabled], or a disabling container around it), the label and the switch take
 * Material's disabled colors and the row ignores taps.
 *
 * @param checked whether the switch is on.
 * @param onCheckedChange called with the new value when the row or the switch is tapped.
 * @param modifier the row's placement.
 * @param supportingContent a line under the label, in Material's supporting style. Null shows none.
 * @param enabled whether the row takes taps.
 * @param label the row's title, in the row title's type (`bodyLarge`) and color.
 */
@Composable
public fun AMToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    label: @Composable () -> Unit,
) {
    val isEnabled = resolveEnabled(enabled)
    ListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = isEnabled,
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = isEnabled) },
        supportingContent = supportingContent,
        colors = AMRowDefaults.colors(),
        shapes = AMRowDefaults.shapes(),
        contentPadding = AMRowDefaults.contentPadding,
        content = label,
    )
}
