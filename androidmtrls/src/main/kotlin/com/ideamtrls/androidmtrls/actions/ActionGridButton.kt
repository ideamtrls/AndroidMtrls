package com.ideamtrls.androidmtrls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow

/**
 * A tile of the remote: a glyph over its title on a floating surface, a big target for a view or an
 * action. Tiles go in an [AMActionGridRow], which makes each one as wide as its column.
 *
 * ```kotlin
 * AMActionGridButton(painterResource(R.drawable.ic_feed), "Feed", onClick = ::openFeed, active = current == Feed)
 * AMActionGridButton(painterResource(R.drawable.ic_delete), "Delete", onClick = ::delete, destructive = true)
 * ```
 *
 * The tile is [AMTheme.ActionGrid.tileHeight] (76 dp) tall whatever the font scale, so rows stay
 * even; its title takes one line and ends in an ellipsis when it's too long. It wears the floating
 * surface in `shapes.large` (white in light mode, black in dark mode, the shape's two-layer
 * shadow), always outlined by a hairline, and always solid: the remote's tiles stay obvious over
 * any backdrop. The glyph is Material's 24 dp; the title is `labelMedium`, `spacing.xs` below it.
 * A tap ripples inside the shape.
 *
 * The title is `onSurface`, or [AMSemantic.Destructive] for an action that removes something. An
 * [active] tile (the view you're on, the filter that's chosen) is ringed in the color scheme's
 * primary at [AMTheme.selectionRingWidth], its glyph and title in primary, and casts no shadow, so
 * it reads pressed into the grid; screen readers hear it as selected. Disabled (its own [enabled],
 * or a disabling container around it), the glyph and title grey to Material's disabled color and
 * the tile casts no shadow, keeping its place, size and fill; a disabled active tile keeps its
 * ring.
 *
 * A screen reader says the title, or [contentDescription] when a short title needs a fuller
 * name: a "Queue" tile that adds to the queue says "Add to queue".
 *
 * @param icon the glyph.
 * @param title the title under the glyph, also what a screen reader says unless
 *   [contentDescription] is set.
 * @param onClick called when the tile is tapped.
 * @param modifier the tile's placement.
 * @param contentDescription what a screen reader says for the tile, in place of [title]. Null says
 *   the title.
 * @param active whether this is the current view or the chosen option.
 * @param destructive whether the action removes something.
 * @param elevation the tile's shadow at rest: [AMElevation.Low] by default, [AMElevation.None] for
 *   a dense grid where every tile's shadow would stack into noise.
 * @param enabled whether the tile takes taps.
 */
@Composable
public fun AMActionGridButton(
    icon: Painter,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    active: Boolean = false,
    destructive: Boolean = false,
    elevation: AMElevation = AMElevation.Low,
    enabled: Boolean = true,
) {
    // A fuller name replaces the title for screen readers: the tile carries it, the title is hidden.
    val tileModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }
    AMActionGridButton(onClick, tileModifier, active, destructive, elevation, enabled) {
        Icon(icon, contentDescription = null)
        Text(
            title,
            if (contentDescription != null) Modifier.clearAndSetSemantics {} else Modifier,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A tile with custom content: [content] stacked and centered on the tile, taking the tile's label
 * type and color, so a custom face (a figure over its caption) stays on-system. Otherwise the same
 * tile as the string form: the fixed height, the surface and hairline, the ring when [active].
 *
 * @param onClick called when the tile is tapped.
 * @param modifier the tile's placement.
 * @param active whether this is the current view or the chosen option.
 * @param destructive whether the action removes something.
 * @param elevation the tile's shadow at rest: [AMElevation.Low] by default, [AMElevation.None] for
 *   a dense grid.
 * @param enabled whether the tile takes taps.
 * @param content the tile's face, in a centered column spaced `spacing.xs`. Give a screen reader
 *   what it needs from it: its text is read as it stands.
 */
@Composable
public fun AMActionGridButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    destructive: Boolean = false,
    elevation: AMElevation = AMElevation.Low,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isEnabled = resolveEnabled(enabled)
    val scheme = MaterialTheme.colorScheme
    val spacing = AMTheme.tokens.spacing
    val shape = MaterialTheme.shapes.large
    val contentColor = when {
        active && isEnabled -> scheme.primary
        !isEnabled -> scheme.onSurface.copy(alpha = DisabledContentAlpha)
        destructive -> AMSemantic.Destructive.color
        else -> scheme.onSurface
    }
    val casts = isEnabled && !active
    Column(
        modifier
            .height(AMTheme.ActionGrid.tileHeight)
            // The hairline below is drawn in both modes, so the surface's own dark-mode one is off.
            .amElevatedSurface(shape, if (casts) elevation else AMElevation.None, hairline = Color.Transparent)
            .amHairline(
                shape,
                color = if (active) scheme.primary else Color.Unspecified,
                width = if (active) AMTheme.selectionRingWidth else AMTheme.hairlineWidth,
            )
            .clip(shape)
            .clickable(enabled = isEnabled, role = Role.Button, onClick = onClick)
            .semantics { if (active) selected = true }
            .padding(horizontal = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val column = this
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(MaterialTheme.typography.labelMedium) { column.content() }
        }
    }
}

/** Material's alpha for disabled content. */
private const val DisabledContentAlpha = 0.38f
