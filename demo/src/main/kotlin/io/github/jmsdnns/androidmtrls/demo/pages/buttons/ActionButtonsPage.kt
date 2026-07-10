package io.github.jmsdnns.androidmtrls.demo.pages.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridRow
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMButtonRole
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// Action Grid Button catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMActionGridButton].
 *
 * id: "imActionGridButton", section: Buttons.
 */
val actionGridButtonEntry: ComponentEntry = ComponentEntry(
    id = "imActionGridButton",
    section = ComponentSection.Buttons,
    title = "Action Buttons",
    tileTitle = "Actions",
    subtitle = "Tile button for action grids",
    intro = "Tile-shaped button for the grids that live inside a AMActionsOverlay. Composes " +
        "an icon, a short label, and an optional destructive role. Grid rows hold exactly " +
        "2, 3, or 4 tiles.",
    icon = Icons.Default.GridView,
    content = { ActionButtonsPage() },
)

// ---------------------------------------------------------------------------
// ActionButtonsPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMActionGridButton].
 *
 * - **Rows** — one row at each legal grid width (2, 3, and 4 tiles). Destructive
 *   and disabled states ride along in the wider rows. Matches the iOS demo
 *   (Share/Copy, Reply/Pin/Delete, Save/Duplicate/Locked/More).
 * - **Selected** — a single-select filter row where the chosen tile carries the
 *   accent selection ring; tapping another tile moves the ring (each tile
 *   doubles as a toggle).
 */
@Composable
fun ActionButtonsPage() {
    // ── Rows — one row at each legal width ───────────────────────────────────
    // Each row is an AMActionGridRow (2-, 3-, and 4-tile arity), which owns the
    // equal-width Box(weight) columns and the spacing.md gutters; this group's
    // Column only stacks the rows and insets them from the screen edges.
    catalogGroup(title = "Rows") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            // 2-up: Share / Copy
            AMActionGridRow(
                tileA = {
                    AMActionGridButton(
                        icon = Icons.Default.Share,
                        title = "Share",
                        onClick = {},
                    )
                },
                tileB = {
                    AMActionGridButton(
                        icon = Icons.Default.ContentCopy,
                        title = "Copy",
                        onClick = {},
                    )
                },
            )

            // 3-up: Reply / Pin / Delete (destructive)
            AMActionGridRow(
                tileA = {
                    AMActionGridButton(
                        icon = Icons.Default.Reply,
                        title = "Reply",
                        onClick = {},
                    )
                },
                tileB = {
                    AMActionGridButton(
                        icon = Icons.Default.PushPin,
                        title = "Pin",
                        onClick = {},
                    )
                },
                tileC = {
                    AMActionGridButton(
                        icon = Icons.Default.Delete,
                        title = "Delete",
                        role = AMButtonRole.Destructive,
                        onClick = {},
                    )
                },
            )

            // 4-up: Save / Duplicate / Locked (disabled) / More
            AMActionGridRow(
                tileA = {
                    AMActionGridButton(
                        icon = Icons.Default.SaveAlt,
                        title = "Save",
                        onClick = {},
                    )
                },
                tileB = {
                    AMActionGridButton(
                        icon = Icons.Default.FileCopy,
                        title = "Duplicate",
                        onClick = {},
                    )
                },
                tileC = {
                    AMActionGridButton(
                        icon = Icons.Default.Lock,
                        title = "Locked",
                        enabled = false,
                        onClick = {},
                    )
                },
                tileD = {
                    AMActionGridButton(
                        icon = Icons.Default.MoreHoriz,
                        title = "More",
                        onClick = {},
                    )
                },
            )
        }
    }

    // ── Selected — active ring marks the chosen filter tile ──────────────────
    catalogGroup(title = "Selected") {
        // Single-select filter: the active tile carries the accent ring; tapping
        // another tile moves the selection. Each tile doubles as a toggle.
        var selected by remember { mutableStateOf(0) }
        AMActionGridRow(
            modifier = Modifier.padding(horizontal = AMTheme.Spacing.lg.dp),
            tileA = {
                AMActionGridButton(
                    icon = Icons.Default.PushPin,
                    title = "Pinned",
                    active = selected == 0,
                    onClick = { selected = 0 },
                )
            },
            tileB = {
                AMActionGridButton(
                    icon = Icons.Default.Share,
                    title = "Shared",
                    active = selected == 1,
                    onClick = { selected = 1 },
                )
            },
            tileC = {
                AMActionGridButton(
                    icon = Icons.Default.SaveAlt,
                    title = "Saved",
                    active = selected == 2,
                    onClick = { selected = 2 },
                )
            },
        )
    }
}
