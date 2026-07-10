package io.github.jmsdnns.androidmtrls.demo.pages.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMBadgeContent
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButtonSize
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// IconButton catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMIconButton].
 *
 * id: "imIconButton", section: Buttons.
 */
val iconButtonEntry: ComponentEntry = ComponentEntry(
    id = "imIconButton",
    section = ComponentSection.Buttons,
    title = "Icon Button",
    tileTitle = "Icon",
    subtitle = "Bare icon, full tap target",
    intro = "Bare icon with a proper 44pt tap target. Primary text color, no fill, no chrome — " +
        "use anywhere a tappable icon belongs (transport controls, inline affordances).",
    icon = Icons.Default.MoreHoriz,
    content = { IconButtonPage() },
)

// ---------------------------------------------------------------------------
// IconButtonPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMIconButton].
 *
 * Groups:
 * - **Default** — transport icon row (shuffle/prev/play/next/repeat) with
 *   Spacer between each button, mirroring iOS `HStack(spacing: 0)`.
 * - **Sizes** — play icon at Large, Medium, Small.
 * - **States** — single row: active / inactive / disabled.
 * - **Badges** — single row: count(1) repeat, dot bell, count(12) destructive envelope.
 */
@Composable
fun IconButtonPage() {
    // ── Default — transport icon row ──────────────────────────────────────────
    catalogGroup(title = "Default") {
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            padding = AMPadding.block,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AMIconButton(
                    icon = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    onClick = {},
                )
                Spacer(modifier = Modifier.weight(1f))
                AMIconButton(
                    icon = Icons.Default.FastRewind,
                    contentDescription = "Previous",
                    onClick = {},
                )
                Spacer(modifier = Modifier.weight(1f))
                AMIconButton(
                    icon = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    onClick = {},
                )
                Spacer(modifier = Modifier.weight(1f))
                AMIconButton(
                    icon = Icons.Default.FastForward,
                    contentDescription = "Next",
                    onClick = {},
                )
                Spacer(modifier = Modifier.weight(1f))
                AMIconButton(
                    icon = Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    onClick = {},
                )
            }
        }
    }

    // ── Sizes — Large → Medium → Small ───────────────────────────────────────
    catalogGroup(title = "Sizes") {
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            padding = AMPadding.block,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AMIconButton(
                    icon = Icons.Default.PlayArrow,
                    size = AMIconButtonSize.Large,
                    contentDescription = "Large",
                    onClick = {},
                )
                AMIconButton(
                    icon = Icons.Default.PlayArrow,
                    size = AMIconButtonSize.Medium,
                    contentDescription = "Medium",
                    onClick = {},
                )
                AMIconButton(
                    icon = Icons.Default.PlayArrow,
                    size = AMIconButtonSize.Small,
                    contentDescription = "Small",
                    onClick = {},
                )
            }
        }
    }

    // ── States — active / inactive / disabled ────────────────────────────────
    catalogGroup(title = "States") {
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            padding = AMPadding.block,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AMIconButton(
                    icon = Icons.Default.Shuffle,
                    active = true,
                    contentDescription = "Active",
                    onClick = {},
                )
                AMIconButton(
                    icon = Icons.Default.Shuffle,
                    active = false,
                    contentDescription = "Inactive",
                    onClick = {},
                )
                AMIconButton(
                    icon = Icons.Default.Shuffle,
                    enabled = false,
                    contentDescription = "Disabled",
                    onClick = {},
                )
            }
        }
    }

    // ── Badges ────────────────────────────────────────────────────────────────
    catalogGroup(title = "Badges") {
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            padding = AMPadding.block,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AMIconButton(
                    icon = Icons.Default.Repeat,
                    badge = AMBadgeContent.Count(1),
                    contentDescription = "Repeat one",
                    onClick = {},
                )
                AMIconButton(
                    icon = Icons.Default.Notifications,
                    badge = AMBadgeContent.Dot,
                    contentDescription = "Unread",
                    onClick = {},
                )
                AMIconButton(
                    icon = Icons.Default.Email,
                    badge = AMBadgeContent.Count(12),
                    badgeTint = AMTheme.destructive.resolve(LocalAMIsDarkTheme.current),
                    contentDescription = "Inbox",
                    onClick = {},
                )
            }
        }
    }
}
