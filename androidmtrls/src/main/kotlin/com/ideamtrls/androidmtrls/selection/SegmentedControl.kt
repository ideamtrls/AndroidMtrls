package com.ideamtrls.androidmtrls

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * Picks one of a few short options: Material's single-choice segmented buttons, one segment per
 * option, the chosen one filled and checked.
 *
 * ```kotlin
 * var range by rememberSaveable { mutableStateOf(Range.Week) }
 * AMSegmentedControl(Range.entries, label = { it.title }, selected = range, onSelectedChange = { range = it }, modifier = Modifier.fillMaxWidth())
 * ```
 *
 * It keeps Material's look entirely: the outlined row with rounded ends, the secondary-container
 * fill and check mark on the chosen segment, ripple, and a radio-group reading for TalkBack. Every
 * segment is as wide as the widest, and the row hugs them; give it `Modifier.fillMaxWidth()` to
 * spread the segments across the width. It draws no surface of its own and goes straight on a
 * card or the page; a card around it would frame the outline twice. Labels stay on one line,
 * ellipsized when a segment is too narrow, so keep them to a word or two and the options to
 * about five.
 *
 * Disabled (its own [enabled], or a disabling container around it), every segment greys and
 * ignores taps.
 *
 * @param options the choices, in order. Each must be distinct.
 * @param label what a segment says for its option.
 * @param selected the chosen option.
 * @param onSelectedChange called with the option a segment's tap picks.
 * @param modifier the row's placement and width.
 * @param enabled whether the segments take taps.
 */
@Composable
public fun <T> AMSegmentedControl(
    options: List<T>,
    label: (option: T) -> String,
    selected: T,
    onSelectedChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isEnabled = resolveEnabled(enabled)
    SingleChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { if (option != selected) onSelectedChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                enabled = isEnabled,
                label = { Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}
