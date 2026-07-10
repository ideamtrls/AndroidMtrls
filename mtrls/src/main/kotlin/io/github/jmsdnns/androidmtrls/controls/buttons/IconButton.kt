package io.github.jmsdnns.androidmtrls.controls.buttons

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMBadge
import io.github.jmsdnns.androidmtrls.content.AMBadgedBox
import io.github.jmsdnns.androidmtrls.content.AMBadgeContent
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.amRipple

// ---------------------------------------------------------------------------
// AMIconButtonSize — size variants
// ---------------------------------------------------------------------------

/**
 * Size variants for [AMIconButton].
 *
 * Each variant specifies a tap-target diameter and a glyph diameter (both dp).
 *
 * | Variant            | Tap target | Glyph size |
 * |--------------------|-----------|------------|
 * | [Small]            | 32dp      | 16dp       |
 * | [Medium] (default) | 44dp      | 22dp       |
 * | [Large]            | 56dp      | 28dp       |
 *
 * The medium 44dp tap target matches the iOS HIG minimum. The large 56dp
 * variant is suited to prominent media-control rows.
 *
 * iOS equivalent: `IMIconButtonSize` in `Sources/Controls/Buttons/IconButton.swift`.
 */
@Immutable
enum class AMIconButtonSize(
    /** Outer tap-target dimension (width and height) in dp. */
    val tapSize: Float,
    /** Icon glyph dimension in dp. */
    val glyphSize: Float,
) {
    /** 32dp tap target, 16dp glyph — compact toolbars and dense rows. */
    Small(tapSize = 32f, glyphSize = 16f),

    /** 44dp tap target, 22dp glyph — the HIG-minimum default. */
    Medium(tapSize = 44f, glyphSize = 22f),

    /** 56dp tap target, 28dp glyph — prominent media-control rows. */
    Large(tapSize = 56f, glyphSize = 28f),
}

// ---------------------------------------------------------------------------
// AMIconButton — icon-only tappable button
// ---------------------------------------------------------------------------

/**
 * Icon-only button with a proper tap target. No fill, no chrome. Three sizes.
 *
 * The glyph renders in `AMTheme.Text.primary` by default. `active = false`
 * dims the glyph to `AMTheme.Icon.muted` (toggled-off state — shuffle off,
 * repeat off) without disabling the button. `disabled = true` renders the
 * glyph in `AMTheme.Icon.disabled` and makes the button non-interactive.
 *
 * **Foreground precedence:** `!enabled` → `AMTheme.Icon.disabled`;
 * `!active` → `AMTheme.Icon.muted`; else → `AMTheme.Text.primary`.
 *
 * **Press feedback:** the monochrome `amRipple()` state layer, bounded to the
 * circular tap target (the Box is `clip(CircleShape)`'d so the ripple reads as a
 * disc) — never an opacity dim, never a Material3 ripple.
 *
 * **Badge:** when [badge] is non-null, an [AMBadge] is overlaid at the
 * glyph corner (not the outer tap-target frame) using [AMBadgedBox], so the
 * badge anchors to the glyph's edge rather than floating in dead space.
 * [badgeTint] defaults to `AMTheme.Text.primary` (resolved at runtime) —
 * icon-button badges are typically state markers (repeat-one, shuffle-on)
 * where red reads as an error. Pass an explicit `AMTheme.destructive` resolved
 * color for notification counts where urgency is intended.
 *
 * Sibling primitives, when to reach for which:
 * - [AMToolbarButton]: 36×36dp round chip with surface fill and elevation,
 *   for toolbars.
 *
 * iOS equivalent: `IMIconButton` in `Sources/Controls/Buttons/IconButton.swift`.
 *
 * Usage:
 * ```kotlin
 * AMIconButton(
 *     icon = Icons.Default.PlayArrow,
 *     size = AMIconButtonSize.Large,
 *     contentDescription = "Play",
 *     onClick = { play() },
 * )
 * ```
 *
 * @param icon               The glyph to render. Public APIs take [ImageVector] so
 *                           consumers choose their own icon set.
 * @param size               Tap-target and glyph diameter. Defaults to [AMIconButtonSize.Medium].
 * @param active             `false` renders the glyph in `AMTheme.Icon.muted` (toggled-off
 *                           state). The button remains tappable; use [enabled] to prevent taps.
 * @param enabled            `false` renders the glyph in `AMTheme.Icon.disabled` and makes
 *                           the button non-interactive.
 * @param badge              Optional badge content ([AMBadgeContent.Dot] or
 *                           [AMBadgeContent.Count]) overlaid at the glyph corner.
 * @param badgeTint          Fill color for the badge. `null` defaults to `AMTheme.Text.primary`
 *                           (state-marker intent). Pass `AMTheme.destructive` resolved color
 *                           explicitly for notification counts.
 * @param contentDescription Accessibility label for this button. Required — no nullable default.
 * @param onClick            Invoked when the button is tapped (not called when [disabled]).
 */
@Composable
fun AMIconButton(
    icon: ImageVector,
    size: AMIconButtonSize = AMIconButtonSize.Medium,
    active: Boolean = true,
    enabled: Boolean = true,
    badge: AMBadgeContent? = null,
    badgeTint: Color? = null,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current

    val iconTint: Color = when {
        !enabled -> AMTheme.Icon.disabled.resolve(isDark)
        !active  -> AMTheme.Icon.muted.resolve(isDark)
        else     -> AMTheme.Text.primary.resolve(isDark)
    }

    // badgeTint: null → AMTheme.Text.primary (state-marker default, matching iOS).
    // Callers pass an explicit resolved color (e.g. AMTheme.destructive) for counts.
    val resolvedBadgeTint: Color = badgeTint ?: AMTheme.Text.primary.resolve(isDark)

    val interactionSource = remember { MutableInteractionSource() }
    val indication = amRipple()

    // Outer Box is the full tap target (size.tapSize × size.tapSize), clipped to
    // a circle so the bounded ripple reads as a disc. Content is centered — the
    // glyph sits in the middle; the badge anchors to the glyph corner via
    // AMBadgedBox, not the outer frame.
    //
    // The clickable stays wired even when `!enabled`, passing `enabled = enabled`
    // so the node announces `Role.Button` + the disabled state to TalkBack (iOS
    // `.disabled()` propagates the disabled trait) instead of dropping out of the
    // semantics tree. A disabled clickable emits no press interaction, so the
    // `amRipple()` sweep and `onClick` never fire.
    Box(
        modifier = Modifier
            .size(size.tapSize.dp)
            .clip(CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                interactionSource = interactionSource,
                indication = indication,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // AMBadgedBox sizes to the glyph so the badge anchors to the glyph's
        // corner rather than floating in the dead space of the tap-target frame.
        AMBadgedBox(
            badge = {
                if (badge != null) {
                    AMBadge(content = badge, tint = resolvedBadgeTint)
                }
            },
        ) {
            Image(
                imageVector = icon,
                contentDescription = null, // accessibility is on the outer Box
                colorFilter = ColorFilter.tint(iconTint),
                modifier = Modifier.size(size.glyphSize.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun IconButtonPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // Size variants: small / medium / large
        Row(
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AMIconButton(
                icon = Icons.Default.PlayArrow,
                size = AMIconButtonSize.Small,
                contentDescription = "Play small",
                onClick = {},
            )
            AMIconButton(
                icon = Icons.Default.PlayArrow,
                size = AMIconButtonSize.Medium,
                contentDescription = "Play medium",
                onClick = {},
            )
            AMIconButton(
                icon = Icons.Default.PlayArrow,
                size = AMIconButtonSize.Large,
                contentDescription = "Play large",
                onClick = {},
            )
        }

        // States: active / inactive (muted) / disabled
        Row(
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AMIconButton(
                icon = Icons.Default.Refresh,
                active = true,
                contentDescription = "Shuffle on",
                onClick = {},
            )
            AMIconButton(
                icon = Icons.Default.Refresh,
                active = false,
                contentDescription = "Shuffle off",
                onClick = {},
            )
            AMIconButton(
                icon = Icons.Default.Refresh,
                enabled = false,
                contentDescription = "Shuffle unavailable",
                onClick = {},
            )
        }

        // Badges: state marker (default primary tint) and notification dot (destructive)
        Row(
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xl.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AMIconButton(
                icon = Icons.Default.Refresh,
                badge = AMBadgeContent.Count(1),
                contentDescription = "Repeat one",
                onClick = {},
            )
            AMIconButton(
                icon = Icons.Default.Notifications,
                badge = AMBadgeContent.Dot,
                badgeTint = AMTheme.destructive.resolve(isDark),
                contentDescription = "Notifications, new",
                onClick = {},
            )
            AMIconButton(
                icon = Icons.Default.Favorite,
                badge = AMBadgeContent.Count(12),
                badgeTint = AMTheme.destructive.resolve(isDark),
                contentDescription = "Favorites, 12 unread",
                onClick = {},
            )
        }
    }
}

@Preview(
    name = "AMIconButton · Light",
    group = "AMIconButton",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMIconButtonPreviewLight() {
    AMThemeRoot {
        IconButtonPreviewContent()
    }
}

@Preview(
    name = "AMIconButton · Dark",
    group = "AMIconButton",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMIconButtonPreviewDark() {
    AMThemeRoot {
        IconButtonPreviewContent()
    }
}
