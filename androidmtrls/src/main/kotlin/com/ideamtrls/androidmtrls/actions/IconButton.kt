package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How large an [AMIconButton] draws. Every size takes taps across at least 48 dp.
 */
public enum class AMIconButtonSize {
    /** A 20 dp glyph on Material's 40 dp button: compact controls beside larger ones. */
    Small,

    /** A 24 dp glyph on Material's 40 dp button: Material's own icon button, the default. */
    Medium,

    /** A 32 dp glyph on a 56 dp button: the one control a row is about (play and pause). */
    Large,
}

/** The glyph's size. */
private val AMIconButtonSize.glyph: Dp
    get() = when (this) {
        AMIconButtonSize.Small -> 20.dp
        AMIconButtonSize.Medium -> 24.dp
        AMIconButtonSize.Large -> 32.dp
    }

/** The button's size when it's larger than Material's own, or null for Material's 40 dp. */
private val AMIconButtonSize.container: Dp?
    get() = if (this == AMIconButtonSize.Large) 56.dp else null

/**
 * A bare glyph you tap: Material's `IconButton`, with no fill or outline, in the content color
 * around it (`onSurface` on a card or the page), Material's circular ripple and a 48 dp touch
 * target.
 *
 * ```kotlin
 * AMIconButton(painterResource(R.drawable.ic_skip_next), "Next", onClick = ::next)
 * AMIconButton(painterResource(R.drawable.ic_pause), "Pause", onClick = ::pause, size = AMIconButtonSize.Large)
 * AMIconButton(painterResource(R.drawable.ic_notifications), "Notifications", onClick = ::open, badge = { Badge { Text("3") } })
 * ```
 *
 * A long press shows [contentDescription] in Material's plain tooltip above the glyph, so a glyph
 * that isn't obvious can still be named. A [badge] sits at the glyph's top end corner (Material's
 * `BadgedBox`), not the button's, so it marks the glyph itself.
 *
 * Something the button turns on and off (shuffle, repeat) takes the toggle form, with `checked`
 * and `onCheckedChange`, so a screen reader hears its state. Disabled (its own [enabled], or a
 * disabling container around it), the glyph greys to Material's disabled color and the button
 * ignores taps.
 *
 * @param icon the glyph.
 * @param contentDescription what a screen reader says for the button, and what its tooltip shows.
 *   Null for a button whose meaning is given elsewhere; it then shows no tooltip.
 * @param onClick called when the button is tapped.
 * @param modifier the button's placement.
 * @param size the glyph's and the button's size.
 * @param badge a badge on the glyph's corner, usually Material's `Badge`. Null shows none.
 * @param enabled whether the button takes taps.
 */
@Composable
public fun AMIconButton(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: AMIconButtonSize = AMIconButtonSize.Medium,
    badge: (@Composable BoxScope.() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val isEnabled = resolveEnabled(enabled)
    IconButtonTooltip(contentDescription, modifier) { buttonModifier ->
        IconButton(onClick = onClick, modifier = size.sized(buttonModifier), enabled = isEnabled) {
            IconButtonGlyph(icon, contentDescription, size, badge)
        }
    }
}

/**
 * A bare glyph that turns something on and off: Material's `IconToggleButton`, so a screen reader
 * hears it as a toggle, named by [contentDescription], and says whether it's on. Otherwise the same
 * button as the tap form: no fill or outline, Material's circular ripple, a 48 dp touch target,
 * the long-press tooltip and the optional [badge].
 *
 * ```kotlin
 * AMIconButton(
 *     painterResource(if (shuffles) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle),
 *     "Shuffle",
 *     checked = shuffles,
 *     onCheckedChange = { shuffles = it },
 * )
 * ```
 *
 * The glyph keeps Material's look: the content color around it while off, the color scheme's
 * checked color (`primary`) while on. Swap the glyph too where Material Symbols has an `_on`
 * variant. A control with more than two states (repeat off, all, one) passes `checked` for any
 * state but off, steps its own state in [onCheckedChange], and names the state in
 * [contentDescription] ("Repeat one"). Disabled, the glyph greys to Material's disabled color and
 * the button ignores taps.
 *
 * @param icon the glyph.
 * @param contentDescription what a screen reader says for the button, and what its tooltip shows.
 *   Name the thing it turns on ("Shuffle"), not its state: the state is read from [checked].
 * @param checked whether the thing is on.
 * @param onCheckedChange called with the new state when the button is tapped.
 * @param modifier the button's placement.
 * @param size the glyph's and the button's size.
 * @param badge a badge on the glyph's corner, usually Material's `Badge`. Null shows none.
 * @param enabled whether the button takes taps.
 */
@Composable
public fun AMIconButton(
    icon: Painter,
    contentDescription: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: AMIconButtonSize = AMIconButtonSize.Medium,
    badge: (@Composable BoxScope.() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val isEnabled = resolveEnabled(enabled)
    IconButtonTooltip(contentDescription, modifier) { buttonModifier ->
        IconToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = size.sized(buttonModifier),
            enabled = isEnabled,
        ) {
            IconButtonGlyph(icon, contentDescription, size, badge)
        }
    }
}

/** [modifier] at the button's size when it's larger than Material's own. */
private fun AMIconButtonSize.sized(modifier: Modifier): Modifier = container?.let { modifier.size(it) } ?: modifier

/**
 * Puts the button in Material's plain tooltip, showing [contentDescription] above it on a long
 * press; with no description, the button alone. [button] takes the modifier to put on itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconButtonTooltip(contentDescription: String?, modifier: Modifier, button: @Composable (Modifier) -> Unit) {
    if (contentDescription == null) {
        button(modifier)
    } else {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(contentDescription) } },
            state = rememberTooltipState(),
            modifier = modifier,
        ) {
            button(Modifier)
        }
    }
}

/** The glyph at its size, with the badge on its corner when there is one. */
@Composable
private fun IconButtonGlyph(
    icon: Painter,
    contentDescription: String?,
    size: AMIconButtonSize,
    badge: (@Composable BoxScope.() -> Unit)?,
) {
    val glyph = @Composable { Icon(icon, contentDescription, Modifier.size(size.glyph)) }
    if (badge != null) BadgedBox(badge = badge) { glyph() } else glyph()
}
