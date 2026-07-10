package io.github.jmsdnns.androidmtrls.surfaces.frames

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import io.github.jmsdnns.androidmtrls.tokens.AMElevation
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amCardHairline
import io.github.jmsdnns.androidmtrls.tokens.amElevation
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.titleBarInsets

// ---------------------------------------------------------------------------
// AMButtonRole — destructive role marker for action buttons
// ---------------------------------------------------------------------------

/**
 * Role qualifier for action buttons in AndroidMtrls.
 *
 * Currently has one case: [Destructive], which colors the button's icon with
 * [AMTheme.destructive] (red) to signal a permanent or irreversible action.
 *
 * iOS equivalent: `SwiftUI.ButtonRole`. Shared by [AMToolbarButton] and
 * `AMAlertAction` — both reference this enum.
 */
@Immutable
enum class AMButtonRole {
    /** Action is permanent or destructive — icon is tinted [AMTheme.destructive]. */
    Destructive
}

// ---------------------------------------------------------------------------
// AMToolbarChipMetrics — internal geometry constants
// ---------------------------------------------------------------------------

/**
 * Round-chip geometry shared by [AMToolbarButton] and future panel accessories
 * so circle-chip affordances stay identical across the system.
 *
 * iOS equivalent: `IMToolbarChipMetrics` in `Toolbar.swift`.
 */
internal object AMToolbarChipMetrics {
    /** Chip diameter in dp — the [AMToolbarButton] circle. */
    val size: Dp = 36.dp

    /** Glyph size in dp, proportioned for the 36dp chip. */
    val iconSize: Dp = 18.dp
}

// ---------------------------------------------------------------------------
// AMToolbarButtonSize — chip size axis
// ---------------------------------------------------------------------------

/**
 * Size axis for [AMToolbarButton]. Both sizes share the identical surface-fill +
 * dark-only hairline + `amElevation(AMElevation.Low)` recipe — only the circle
 * diameter and glyph scale change.
 *
 * iOS equivalent: `IMToolbarButtonSize` in `Toolbar.swift`.
 */
@Immutable
enum class AMToolbarButtonSize {
    /** 36dp chip, 18dp glyph — the default toolbar/panel affordance. */
    Regular,

    /** 60dp circle, 30dp glyph — a hero action such as a media play/pause. */
    Prominent;

    /** Circle diameter for this size. */
    internal val diameter: Dp
        get() = when (this) {
            Regular -> AMToolbarChipMetrics.size
            Prominent -> 60.dp
        }

    /** Glyph size, proportioned to the [diameter]. */
    internal val iconSize: Dp
        get() = when (this) {
            Regular -> AMToolbarChipMetrics.iconSize
            Prominent -> 30.dp
        }
}

// ---------------------------------------------------------------------------
// AMToolbar — top action strip
// ---------------------------------------------------------------------------

/**
 * Top action strip that floats over the page substrate — no fill, no hairline,
 * just [leading] and [trailing] slots flanking an optionally centered title.
 * Pair with [AMToolbarButton] for the round surface-chip affordances that read
 * as part of the system.
 *
 * The center title/content is absolutely positioned inside a [Box] so it
 * remains optically centered regardless of the leading/trailing cluster widths.
 * The edge [Row] is overlaid on top in the same [Box] — matching the
 * `ZStack`–`HStack` trick in `IMToolbar`.
 *
 * **Visual only** — taps inside each slot stay with the caller's content.
 *
 * When [center] is non-null it takes precedence over [title]. When [title] is
 * provided and [center] is null, a standard headline [BasicText] is rendered
 * into the center slot.
 *
 * When one side is absent but the other is not, a [Spacer] sized to
 * [AMToolbarChipMetrics.size] preserves optical balance — matching iOS where
 * the absent `Leading == EmptyView` convenience inits produce the same balance.
 *
 * Padding is `tokens.titleBarInsets` (horizontal 14dp, vertical 10dp) — the
 * chrome-less floating title bar, distinct from `AMPanel`'s attached Android app
 * bar (a 64dp bar with a leading back arrow and a hairline under it).
 *
 * iOS equivalent: `IMToolbar` in `Sources/Surfaces/Frames/Toolbar.swift`.
 *
 * Usage:
 * ```kotlin
 * AMToolbar(
 *     title = "Inbox",
 *     leading = {
 *         AMToolbarButton(Icons.Default.Menu, contentDescription = "Menu", onClick = { openMenu() })
 *     },
 *     trailing = {
 *         AMToolbarButton(Icons.Default.Edit, contentDescription = "Compose", onClick = { compose() })
 *     }
 * )
 * ```
 *
 * @param modifier  Modifier applied to the root [Box] of the toolbar.
 * @param title     Title string rendered as a headline when [center] is null.
 *                  Pass null to suppress the center title.
 * @param leading   Composable slot for the leading (start) action cluster.
 *                  Pass null when no leading content is needed; a balancing
 *                  [Spacer] is inserted automatically when [trailing] is present.
 * @param trailing  Composable slot for the trailing (end) action cluster.
 *                  Pass null when no trailing content is needed; a balancing
 *                  [Spacer] is inserted automatically when [leading] is present.
 * @param center    Custom composable rendered at the center. When non-null,
 *                  overrides [title]. Pass null to use the [title] string path.
 */
@Composable
fun AMToolbar(
    modifier: Modifier = Modifier,
    title: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    center: (@Composable () -> Unit)? = null,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(tokens.titleBarInsets),
    ) {
        // Center content — absolutely positioned in the Box so it stays
        // optically centered independent of edge cluster widths.
        // Same trick as IMPanel's title bar.
        Box(
            modifier = Modifier.align(Alignment.Center),
        ) {
            if (center != null) {
                center()
            } else if (title != null) {
                BasicText(
                    text = title,
                    // iOS .headline analog
                    style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                )
            }
        }

        // Edge row — laid out on top of center in the same Box.
        // Default for each absent slot is Spacer(0.dp). A 36dp balancing spacer
        // is used only when exactly one side is absent and the other is present,
        // so the title stays optically centered — matching iOS EmptyView balance.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
            } else if (trailing != null) {
                // One side absent, other present — balance with chip-sized spacer.
                Spacer(Modifier.size(AMToolbarChipMetrics.size))
            } else {
                Spacer(Modifier.size(0.dp))
            }

            // Minimum inter-cluster gap floor — the Compose twin of iOS
            // `Spacer(minLength: tokens.spacing.md)` (Toolbar.swift:52). With
            // SpaceBetween this invisible spacer guarantees ≥ md between the
            // leading and trailing clusters even when they are wide enough to
            // otherwise abut; in the common case it floats unseen mid-row while
            // the edge clusters stay pinned to their edges.
            Spacer(Modifier.widthIn(min = tokens.spacing.md))

            if (trailing != null) {
                trailing()
            } else if (leading != null) {
                // One side absent, other present — balance with chip-sized spacer.
                Spacer(Modifier.size(AMToolbarChipMetrics.size))
            } else {
                Spacer(Modifier.size(0.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AMToolbarButton — round surface-chip button
// ---------------------------------------------------------------------------

/**
 * Round surface-chip button for [AMToolbar] action slots.
 *
 * A circle filled with [AMTheme.Surface.primary] and lifted by
 * `amElevation(AMElevation.Low)`. In dark mode a [AMTheme.selectionRing]
 * hairline is added so the chip reads against the near-black page — same
 * conditional pattern as `amCardHairline`. The [size] axis selects the circle
 * and glyph scale: [AMToolbarButtonSize.Regular] is the 36×36dp toolbar chip;
 * [AMToolbarButtonSize.Prominent] is a 60dp hero circle (e.g. a media
 * play/pause). The fill + hairline + elevation recipe is identical at both.
 *
 * Press feedback is the monochrome `amRipple()` state layer, bounded to the
 * clipped circle — no opacity dim, no Material3 dependency.
 *
 * When [enabled] is `false` the glyph greys to [AMTheme.Icon.disabled] and the
 * chip stops responding to taps (no press feedback either). The surface chrome
 * — fill, hairline, and shadow — stays **full-alpha**; only the glyph dims,
 * mirroring iOS `IMToolbarButton(disabled:)`.
 *
 * The [contentDescription] parameter is **required and non-nullable**, mirroring
 * iOS where `IMToolbarButton(systemImage:size:role:disabled:accessibilityLabel:action:)`
 * has no `nil` fallback for `accessibilityLabel`.
 *
 * For an icon-first button inside a card's action row use `AMCardButton` instead —
 * it is deliberately flat because the surrounding `.amCard()` supplies fill and shadow.
 *
 * iOS equivalent: `IMToolbarButton` in `Sources/Surfaces/Frames/Toolbar.swift`.
 *
 * Usage:
 * ```kotlin
 * AMToolbarButton(
 *     icon = Icons.Default.Menu,
 *     contentDescription = "Menu",
 *     onClick = { openMenu() },
 * )
 * ```
 *
 * @param icon               The glyph to render, scaled to the [size]'s glyph metric.
 * @param modifier           Modifier applied to the chip root.
 * @param size               Chip size axis — [AMToolbarButtonSize.Regular] (default) or
 *                           [AMToolbarButtonSize.Prominent].
 * @param role               Optional [AMButtonRole]. [AMButtonRole.Destructive] tints the
 *                           icon [AMTheme.destructive] (red) instead of [AMTheme.Text.primary].
 * @param enabled            When `false`, greys the glyph to [AMTheme.Icon.disabled] and
 *                           disables taps; the surface chrome stays full-alpha. Defaults to `true`.
 * @param contentDescription Accessibility label for this button. Required — no nullable default.
 * @param onClick            Invoked when the chip is tapped (only while [enabled]).
 */
@Composable
fun AMToolbarButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: AMToolbarButtonSize = AMToolbarButtonSize.Regular,
    role: AMButtonRole? = null,
    enabled: Boolean = true,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    val interactionSource = remember { MutableInteractionSource() }

    // Glyph color precedence: disabled greys out, then a destructive role reddens,
    // otherwise the standard primary text color — the shared control cascade so
    // toolbar chips can't drift from filled/ghost buttons.
    val iconTint = when {
        !enabled -> AMTheme.Icon.disabled.resolve(isDark)
        role == AMButtonRole.Destructive -> AMTheme.destructive.resolve(isDark)
        else -> AMTheme.Text.primary.resolve(isDark)
    }

    Box(
        modifier = modifier
            .size(size.diameter)
            .amElevation(level = AMElevation.Low, shape = CircleShape)
            .clip(CircleShape)
            .background(
                color = AMTheme.Surface.primary.resolve(isDark),
                shape = CircleShape,
            )
            .amCardHairline(CircleShape)
            .semantics {
                this.contentDescription = contentDescription
                // Announce the chip as a button (iOS's `Button` conveys the
                // button trait); the plain clickable below sets no role itself.
                this.role = Role.Button
            }
            // enabled = false drops the click (and its amRipple press feedback)
            // while keeping the chip in the layout at full-alpha chrome.
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = amRipple(),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Render the icon using the foundation Image composable — no Material3 dependency.
        androidx.compose.foundation.Image(
            imageVector = icon,
            contentDescription = null,   // accessibility handled by outer Box semantics
            colorFilter = ColorFilter.tint(iconTint),
            modifier = Modifier.size(size.iconSize),
        )
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun ToolbarPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(vertical = AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
    ) {
        // Full toolbar: leading + title + trailing
        AMToolbar(
            title = "Inbox",
            leading = {
                AMToolbarButton(
                    icon = Icons.Default.Menu,
                    contentDescription = "Menu",
                    onClick = {},
                )
            },
            trailing = {
                AMToolbarButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Compose",
                    onClick = {},
                )
            },
        )

        // Title only
        AMToolbar(title = "Library")

        // Leading + trailing only (no title)
        AMToolbar(
            leading = {
                AMToolbarButton(
                    icon = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    onClick = {},
                )
            },
            trailing = {
                AMToolbarButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit",
                    onClick = {},
                )
            },
        )

        // Disabled chip + prominent hero button (media transport play).
        Row(
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xl.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            AMToolbarButton(
                icon = Icons.Default.Edit,
                enabled = false,
                contentDescription = "Compose unavailable",
                onClick = {},
            )
            AMToolbarButton(
                icon = Icons.Default.PlayArrow,
                size = AMToolbarButtonSize.Prominent,
                contentDescription = "Play",
                onClick = {},
            )
        }
    }
}

@Preview(
    name = "AMToolbar · Light",
    group = "AMToolbar",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,   // AMTheme.Surface.page light
)
@Composable
private fun AMToolbarPreviewLight() {
    AMThemeRoot {
        ToolbarPreviewContent()
    }
}

@Preview(
    name = "AMToolbar · Dark",
    group = "AMToolbar",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,   // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMToolbarPreviewDark() {
    AMThemeRoot {
        ToolbarPreviewContent()
    }
}
