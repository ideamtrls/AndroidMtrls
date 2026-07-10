package io.github.jmsdnns.androidmtrls.controls.buttons

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMButtonRole
import io.github.jmsdnns.androidmtrls.tokens.AMElevation
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amElevation
import io.github.jmsdnns.androidmtrls.tokens.amHairline
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.amSquircle

// ---------------------------------------------------------------------------
// Layout constants (private — matches ActionGridButton.swift)
// ---------------------------------------------------------------------------

/** Fixed tile height so grid rows stay even regardless of label length. */
private val actionGridTileHeight = 76.dp

/**
 * Tile glyph size — matches `AMIconButton`'s medium glyph so icons read at one
 * scale across grids and icon rows.
 */
private val actionGridIconSize = 22.dp

/**
 * Icon-to-label gap. Off-grid by design — `spacing.xs` (4dp) crowds the
 * caption; `spacing.sm` (8dp) breaks the tile's compact read.
 */
private val actionGridIconLabelSpacing = 6.dp

/**
 * Breathing room so long labels don't touch the tile edges before single-line
 * truncation clips them.
 */
private val actionGridLabelInset = 6.dp

// ---------------------------------------------------------------------------
// AMActionGridButton — generic slot overload
// ---------------------------------------------------------------------------

/**
 * A single tile in an action grid. Full card chrome per tile: `AMTheme.Surface.primary`
 * fill, `amHairline` (unconditional — drawn in both light and dark mode),
 * and `amElevation`.
 *
 * The generic [content] slot replaces the fixed icon-over-title interior while
 * keeping every other visual treatment — tile height, card chrome, unconditional
 * hairline, disabled dim — applied by the generic body. Use the
 * `AMActionGridButton(icon, title, …)` convenience overload for the standard
 * icon-over-caption layout.
 *
 * Press feedback is the monochrome `amRipple()` state layer, bounded to the
 * clipped tile shape — never an opacity dim.
 *
 * **Active state** — defaults to `false` (the plain tile). Pass `active = true`
 * to overlay an accent selection ring (`AMTheme.selectionRingWidth`, at the
 * tile's `radius.lg` shape, tinted with the resolved `AMTokens.semantic.accent`)
 * marking the chosen tile in a filter grid — the tile doubles as a toggle. Since
 * `:mtrls` has no content-color CompositionLocal, the ring is all this overload
 * can apply; slot callers should tint their own content with the same accent
 * (`LocalAMTokens.current.semantic.accent.resolve(...)`). The
 * `AMActionGridButton(icon, title, …)` convenience overload does both — ring and
 * accent-tinted label — automatically.
 *
 * iOS equivalent: `IMActionGridButton<Label>` in `ActionGridButton.swift`.
 *
 * Pass `elevation = AMElevation.None` for shadowless grids where per-tile shadows
 * would stack into visual noise (e.g. the catalog root tile grid).
 *
 * @param active   When `true` overlays the accent selection ring. Defaults to
 *                 `false`. Slot callers should also tint their content with the
 *                 resolved [AMTokensSemantics.accent].
 * @param enabled  When `false` the tile ignores taps and dims. Callers using the
 *                 slot overload should also dim their content to
 *                 [AMTheme.Icon.disabled].
 * @param contentDescription Accessibility label announced for the tile. Required
 *                 for the slot overload because there is no visible title text to
 *                 merge into the clickable's semantics.
 * @param elevation Shadow depth. Defaults to [AMElevation.Low]. Pass
 *                  [AMElevation.None] to suppress shadows while keeping the fill
 *                  and hairline.
 * @param onClick  Required tap callback.
 * @param content  Generic label slot scoped to [BoxScope]. Centered inside the
 *                 tile with horizontal [actionGridLabelInset] padding applied
 *                 by the tile body.
 */
@Composable
fun AMActionGridButton(
    active: Boolean = false,
    enabled: Boolean = true,
    contentDescription: String,
    elevation: AMElevation = AMElevation.Low,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current
    val interactionSource = remember { MutableInteractionSource() }

    val shape = amSquircle(tokens.radius.lg)
    // Monochrome accent (Black light / White dark) — flows with the demo's
    // dynamic-accent override via AMTokensSemantics.accent.
    val accent = tokens.semantic.accent.resolve(isDark)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(actionGridTileHeight)
            .amElevation(level = elevation, shape = shape)
            .clip(shape)
            .background(
                color = AMTheme.Surface.primary.resolve(isDark),
                shape = shape,
            )
            // Accent selection ring, on top of the base hairline when active.
            // amHairline strokes INSIDE the bounds (SwiftUI strokeBorder-style),
            // so the 1.5dp ring never clips against the tile's clip(shape). This
            // sits BEFORE the base hairline so it draws last (over the gray one).
            .then(
                if (active) {
                    Modifier.amHairline(
                        shape = shape,
                        color = accent,
                        width = AMTheme.selectionRingWidth.dp,
                    )
                } else {
                    Modifier
                }
            )
            .amHairline(shape)
            .semantics {
                this.role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(
                interactionSource = interactionSource,
                indication = amRipple(),
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Content slot — inset horizontally so label text doesn't touch tile
        // edges; the monochrome ripple sweeps behind it on press.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = actionGridLabelInset),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------
// AMActionGridButtonStringLabel — private fixed icon-over-caption layout
// ---------------------------------------------------------------------------

/**
 * Private internal label view used by the icon + String convenience overload.
 *
 * Renders the fixed icon-above-caption layout: a 22dp [ImageVector] icon above a
 * single-line `12sp` [title]. [foreground] drives both the icon tint and text
 * color; it is pre-resolved by the convenience overload from the active / enabled
 * / role precedence cascade.
 */
@Composable
private fun AMActionGridButtonStringLabel(
    icon: ImageVector,
    title: String,
    foreground: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(actionGridIconLabelSpacing),
    ) {
        Image(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(actionGridIconSize),
            colorFilter = ColorFilter.tint(foreground),
        )
        BasicText(
            text = title,
            style = AMTheme.Type.caption.toTextStyle(foreground),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------
// AMActionGridButton — icon + String convenience overload
// ---------------------------------------------------------------------------

/**
 * Convenience overload for [AMActionGridButton] that renders a standard
 * [ImageVector] icon above a single-line [title] caption.
 *
 * Foreground color follows the iOS precedence cascade — an active (and enabled)
 * tile takes the accent; otherwise the shared control cascade:
 * - `active && enabled` → resolved [AMTokensSemantics.accent] (icon **and** text)
 * - `!enabled` → [AMTheme.Icon.disabled]
 * - `role == Destructive` → [AMTheme.destructive]
 * - Otherwise → [AMTheme.Text.primary]
 *
 * When [active] is `true` the accent selection ring is drawn and the label is
 * tinted with the same accent, marking the chosen tile in a filter grid (the
 * tile doubles as a toggle).
 *
 * All other visual treatment (tile height, surface fill, unconditional hairline,
 * elevation, ripple press feedback) is applied by the generic [AMActionGridButton]
 * overload.
 *
 * iOS equivalent: `IMActionGridButton(systemImage:title:role:active:disabled:elevation:action:)`
 * in `ActionGridButton.swift`.
 *
 * @param icon     Icon displayed at `22dp` above the [title].
 * @param title    Caption label (`12sp`, `maxLines = 1`, ellipsis overflow).
 * @param role     `null` or [AMButtonRole.Destructive]. Destructive colors icon and
 *                 text with [AMTheme.destructive].
 * @param active   When `true` overlays the accent selection ring and tints the
 *                 icon + caption with the resolved [AMTokensSemantics.accent].
 *                 Takes precedence over [role] and [enabled] styling while
 *                 enabled. Defaults to `false`.
 * @param enabled  When `false` the tile ignores taps and renders in
 *                 [AMTheme.Icon.disabled] color.
 * @param elevation Shadow depth. Defaults to [AMElevation.Low].
 * @param onClick  Required tap callback.
 */
@Composable
fun AMActionGridButton(
    icon: ImageVector,
    title: String,
    role: AMButtonRole? = null,
    active: Boolean = false,
    enabled: Boolean = true,
    elevation: AMElevation = AMElevation.Low,
    onClick: () -> Unit,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current

    // Foreground precedence mirrors iOS: an active (and enabled) tile takes the
    // accent; everything else follows the shared control cascade.
    val foreground = when {
        active && enabled -> tokens.semantic.accent.resolve(isDark)
        !enabled -> AMTheme.Icon.disabled.resolve(isDark)
        role == AMButtonRole.Destructive -> AMTheme.destructive.resolve(isDark)
        else -> AMTheme.Text.primary.resolve(isDark)
    }

    AMActionGridButton(
        active = active,
        enabled = enabled,
        contentDescription = title,
        elevation = elevation,
        onClick = onClick,
    ) {
        AMActionGridButtonStringLabel(
            icon = icon,
            title = title,
            foreground = foreground,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun ActionGridButtonPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(tokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        // 2-up row
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
            Box(modifier = Modifier.weight(1f)) {
                AMActionGridButton(icon = Icons.Default.Share, title = "Share", onClick = {})
            }
            Box(modifier = Modifier.weight(1f)) {
                AMActionGridButton(icon = Icons.Default.Star, title = "Favorite", onClick = {})
            }
        }

        // 3-up row — includes a destructive item
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
            Box(modifier = Modifier.weight(1f)) {
                AMActionGridButton(icon = Icons.Default.Share, title = "Share", onClick = {})
            }
            Box(modifier = Modifier.weight(1f)) {
                AMActionGridButton(icon = Icons.Default.Notifications, title = "Inbox", onClick = {})
            }
            Box(modifier = Modifier.weight(1f)) {
                AMActionGridButton(
                    icon = Icons.Default.Delete,
                    title = "Delete",
                    role = AMButtonRole.Destructive,
                    onClick = {},
                )
            }
        }

        // 4-up row — includes a disabled item
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
            Box(modifier = Modifier.weight(1f)) {
                AMActionGridButton(icon = Icons.Default.Share, title = "Share", onClick = {})
            }
            Box(modifier = Modifier.weight(1f)) {
                AMActionGridButton(icon = Icons.Default.Star, title = "Favorite", onClick = {})
            }
            Box(modifier = Modifier.weight(1f)) {
                AMActionGridButton(icon = Icons.Default.Notifications, title = "Inbox", onClick = {})
            }
            Box(modifier = Modifier.weight(1f)) {
                AMActionGridButton(
                    icon = Icons.Default.Delete,
                    title = "Delete",
                    enabled = false,
                    onClick = {},
                )
            }
        }

        // Active (selected) filter tiles — accent ring + accent-tinted label.
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
            Box(modifier = Modifier.weight(1f)) {
                AMActionGridButton(
                    icon = Icons.Default.Star,
                    title = "Starred",
                    active = true,
                    onClick = {},
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                AMActionGridButton(
                    icon = Icons.Default.Notifications,
                    title = "Inbox",
                    onClick = {},
                )
            }
        }
    }
}

@Preview(
    name = "AMActionGridButton · Light",
    group = "AMActionGridButton",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,  // AMTheme.Surface.page light
)
@Composable
private fun AMActionGridButtonPreviewLight() {
    AMThemeRoot {
        ActionGridButtonPreviewContent()
    }
}

@Preview(
    name = "AMActionGridButton · Dark",
    group = "AMActionGridButton",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,  // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMActionGridButtonPreviewDark() {
    AMThemeRoot {
        ActionGridButtonPreviewContent()
    }
}
