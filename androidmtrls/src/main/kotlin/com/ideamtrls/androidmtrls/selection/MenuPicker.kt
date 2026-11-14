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
 * - **The options** come up in the spotlight under [title] ([AMMenuSpotlight], which a control
 *   that isn't a row opens itself): one list item per option ([AMMenuOptions]), each led by a
 *   Material radio button, the current one on, on the spotlight's solid card. A tap picks that
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
    AMMenuSpotlight(
        visible = choosing,
        onDismissRequest = { choosing = false },
        title = title,
        options = options,
        label = label,
        selected = selected,
        onSelectedChange = onSelectedChange,
    )
}

/**
 * A menu's options in the spotlight, opened from any control: [title] over one row per option, the
 * current one's radio button on, on the spotlight's solid card, with no tiles. Picking an option
 * applies it and closes the spotlight; back or a tap outside closes it unchanged. It's what
 * [AMMenuPicker]'s row opens, for a control that isn't a row (a speed or sleep-timer button).
 *
 * ```kotlin
 * var choosingSpeed by rememberSaveable { mutableStateOf(false) }
 * AMIconButton("${speed}×", "Playback speed", onClick = { choosingSpeed = true })
 * AMMenuSpotlight(
 *     choosingSpeed,
 *     onDismissRequest = { choosingSpeed = false },
 *     title = "Speed",
 *     options = speeds,
 *     label = { "$it×" },
 *     selected = speed,
 *     onSelectedChange = { speed = it },
 * )
 * ```
 *
 * It's a window like every overlay: opened from the remote's menu or a sheet, it sits above them,
 * and while it's up the scene's floating action button fades out of its way.
 *
 * @param visible whether the options are up.
 * @param onDismissRequest called when they close: a pick, back, or a tap outside. Set [visible]
 *   to false.
 * @param title the menu's heading over the options.
 * @param options the choices, in order. Each must be distinct.
 * @param label what an option's row says.
 * @param selected the current option, its radio button on.
 * @param onSelectedChange called with the option picked, unless it's the current one; the
 *   spotlight then closes.
 * @param modifier the spotlight's placement: the whole window.
 */
@Composable
public fun <T> AMMenuSpotlight(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    options: List<T>,
    label: (option: T) -> String,
    selected: T,
    onSelectedChange: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    AMSpotlight(visible, onDismissRequest = onDismissRequest, modifier = modifier, title = title) {
        AMMenuOptions(
            options,
            label,
            selected,
            onSelectedChange = { option ->
                if (option != selected) onSelectedChange(option)
                onDismissRequest()
            },
        )
    }
}

/**
 * A menu's option rows, for a card of your own (a spotlight with other content, a sheet): one
 * Material list item per option on the row recipe, each led by a radio button, the current one on,
 * in one selectable group, so TalkBack reads them as one set of choices. A tap on an option calls
 * [onSelectedChange] with it, the current one included, so a menu can close on any pick. The rows
 * lie flat on whatever holds them.
 *
 * ```kotlin
 * AMSpotlight(choosing, onDismissRequest = { choosing = false }, title = "Sleep timer") {
 *     AMMenuOptions(timers, label = { it.title }, selected = timer, onSelectedChange = { timer = it; choosing = false })
 * }
 * ```
 *
 * @param options the choices, in order. Each must be distinct.
 * @param label what an option's row says.
 * @param selected the current option, its radio button on.
 * @param onSelectedChange called with the option tapped, the current one included.
 * @param modifier the rows' placement. They fill the width they're given.
 */
@Composable
public fun <T> AMMenuOptions(
    options: List<T>,
    label: (option: T) -> String,
    selected: T,
    onSelectedChange: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().selectableGroup()) {
        options.forEach { option ->
            val isCurrent = option == selected
            ListItem(
                selected = isCurrent,
                onClick = { onSelectedChange(option) },
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
