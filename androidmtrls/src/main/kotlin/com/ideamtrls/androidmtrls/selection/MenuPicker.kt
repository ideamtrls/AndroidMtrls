package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

/**
 * A row for picking one of a set of options: it shows the field's [title] and the current option,
 * and a tap brings the options up in the spotlight, the field's name above them and the current
 * one's radio button on. Picking one applies it and closes the spotlight.
 *
 * ```kotlin
 * var sort by rememberSaveable { mutableStateOf("Newest") }
 * AMMenuPicker("Sort by", listOf("Newest", "Oldest", "A–Z"), label = { it }, selected = sort, onSelectedChange = { sort = it })
 * ```
 *
 * - **The row** is a Material list item on the row recipe: an optional leading [icon], the title
 *   in `bodyLarge`, and at its end the current option in `bodyMedium` with Material's drop-down
 *   arrow, both in `onSurfaceVariant`. The whole row takes the tap, with ripple, and TalkBack
 *   reads the title and the value together. It's a flat row: put it in a card or on a surface
 *   strip, with `AMDivider(inset = spacing.lg)` between it and its neighbors.
 * - **The options** come up in [AMSpotlight] under [title]: one list item per option, each led by
 *   a Material radio button, the current one on, on the spotlight's solid card. A tap picks that
 *   option and closes the spotlight; back or a tap outside closes it unchanged.
 *
 * Whether the options are up is saved, so they survive a configuration change.
 *
 * Disabled (its own [enabled], or a disabling container around it), the row greys and ignores
 * taps.
 *
 * @param title the field's name: the row's title and the spotlight's heading.
 * @param options the choices, in order. Each must be distinct.
 * @param label what the row and the spotlight say for an option.
 * @param selected the current option.
 * @param onSelectedChange called with the option picked in the spotlight.
 * @param modifier the row's placement.
 * @param icon a glyph leading the row. Null shows none.
 * @param enabled whether the row takes taps.
 */
@Composable
public fun <T> AMMenuPicker(
    title: String,
    options: List<T>,
    label: (option: T) -> String,
    selected: T,
    onSelectedChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    enabled: Boolean = true,
) {
    val isEnabled = resolveEnabled(enabled)
    var choosing by rememberSaveable { mutableStateOf(false) }
    ListItem(
        onClick = { choosing = true },
        modifier = modifier,
        enabled = isEnabled,
        leadingContent = icon?.let { glyph -> { Icon(glyph, contentDescription = null) } },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label(selected), style = MaterialTheme.typography.bodyMedium)
                Icon(painterResource(R.drawable.am_ic_arrow_drop_down), contentDescription = null)
            }
        },
        colors = AMRowDefaults.colors(),
        shapes = AMRowDefaults.shapes(),
        contentPadding = AMRowDefaults.contentPadding,
    ) {
        Text(title)
    }
    AMSpotlight(choosing, onDismissRequest = { choosing = false }, title = title) {
        Column(Modifier.fillMaxWidth().selectableGroup()) {
            options.forEach { option ->
                val isCurrent = option == selected
                ListItem(
                    selected = isCurrent,
                    onClick = {
                        if (!isCurrent) onSelectedChange(option)
                        choosing = false
                    },
                    leadingContent = { RadioButton(selected = isCurrent, onClick = null) },
                    colors = AMRowDefaults.colors(),
                    shapes = AMRowDefaults.shapes(),
                    contentPadding = AMRowDefaults.contentPadding,
                ) {
                    Text(label(option))
                }
            }
        }
    }
}
