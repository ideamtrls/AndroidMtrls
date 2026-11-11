package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Wrapping chips for many choices: one Material filter chip per item in an [AMFlowLayout], a tap
 * toggling the item in or out of [selection].
 *
 * ```kotlin
 * var genres by rememberSaveable { mutableStateOf(setOf("Jazz")) }
 * AMChipGroup(Genres, label = { it }, selection = genres, onSelectionChange = { genres = it })
 * ```
 *
 * The chips keep Material's look: outlined at rest, the secondary-container fill when chosen, a
 * check mark leading a chosen chip (in place of its [icon], when it has one), ripple, and a
 * checkbox reading for TalkBack. They wrap from the start edge, [spacing] apart on a line; lines
 * stack touch target to touch target, so the 48 dp targets part them. The group draws no surface
 * of its own: put it on the page or in a padded card.
 *
 * For one choice among the chips, use the overload taking `selected`. Name the arguments
 * (`selection =`, `selected =`), so the call reads as one or the other.
 *
 * Disabled (its own [enabled], or a disabling container around it), every chip greys and ignores
 * taps.
 *
 * @param items the chips, in reading order. Each must be distinct.
 * @param label what a chip says for its item.
 * @param selection the chosen items.
 * @param onSelectionChange called with the new set when a tap toggles an item in or out.
 * @param modifier the group's placement and width.
 * @param icon a glyph leading each item's chip while it isn't chosen. Null shows none.
 * @param spacing the gap between neighbors on a line: `spacing.sm` by default.
 * @param enabled whether the chips take taps.
 */
@Composable
public fun <T> AMChipGroup(
    items: List<T>,
    label: (item: T) -> String,
    selection: Set<T>,
    onSelectionChange: (Set<T>) -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable (item: T) -> Painter)? = null,
    spacing: Dp = AMTheme.tokens.spacing.sm,
    enabled: Boolean = true,
) {
    ChipFlow(
        items = items,
        label = label,
        isChosen = { it in selection },
        onChoose = { item -> onSelectionChange(if (item in selection) selection - item else selection + item) },
        modifier = modifier,
        icon = icon,
        spacing = spacing,
        enabled = enabled,
    )
}

/**
 * Wrapping chips for one choice: one Material filter chip per item in an [AMFlowLayout], a tap
 * choosing its item. Tapping the chosen chip keeps it chosen, so exactly one always is.
 *
 * ```kotlin
 * var city by rememberSaveable { mutableStateOf("All") }
 * AMChipGroup(Cities, label = { it }, selected = city, onSelectedChange = { city = it })
 * ```
 *
 * Everything else is the many-choice group's: Material's chips, the check mark on the chosen one,
 * the wrapping and the spacing. TalkBack hears the chips as one group.
 *
 * @param items the chips, in reading order. Each must be distinct.
 * @param label what a chip says for its item.
 * @param selected the chosen item.
 * @param onSelectedChange called with the item a tap chooses.
 * @param modifier the group's placement and width.
 * @param icon a glyph leading each item's chip while it isn't chosen. Null shows none.
 * @param spacing the gap between neighbors on a line: `spacing.sm` by default.
 * @param enabled whether the chips take taps.
 */
@Composable
public fun <T> AMChipGroup(
    items: List<T>,
    label: (item: T) -> String,
    selected: T,
    onSelectedChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable (item: T) -> Painter)? = null,
    spacing: Dp = AMTheme.tokens.spacing.sm,
    enabled: Boolean = true,
) {
    ChipFlow(
        items = items,
        label = label,
        isChosen = { it == selected },
        onChoose = { item -> if (item != selected) onSelectedChange(item) },
        modifier = modifier.selectableGroup(),
        icon = icon,
        spacing = spacing,
        enabled = enabled,
    )
}

/** The chips both groups draw: what a tap does is the group's. */
@Composable
private fun <T> ChipFlow(
    items: List<T>,
    label: (item: T) -> String,
    isChosen: (item: T) -> Boolean,
    onChoose: (item: T) -> Unit,
    modifier: Modifier,
    icon: (@Composable (item: T) -> Painter)?,
    spacing: Dp,
    enabled: Boolean,
) {
    val isEnabled = resolveEnabled(enabled)
    // A chip's 48 dp touch target already reaches past its 32 dp face, so lines need no gap of
    // their own.
    AMFlowLayout(modifier, spacing = spacing, lineSpacing = 0.dp) {
        items.forEach { item ->
            val chosen = isChosen(item)
            val glyph: (@Composable () -> Unit)? = when {
                chosen -> {
                    { ChipGlyph(painterResource(R.drawable.am_ic_check)) }
                }
                icon != null -> {
                    { ChipGlyph(icon(item)) }
                }
                else -> null
            }
            FilterChip(
                selected = chosen,
                onClick = { onChoose(item) },
                label = { Text(label(item)) },
                enabled = isEnabled,
                leadingIcon = glyph,
            )
        }
    }
}

/** A chip's leading glyph at Material's chip icon size. */
@Composable
private fun ChipGlyph(painter: Painter) {
    Icon(painter, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
}
