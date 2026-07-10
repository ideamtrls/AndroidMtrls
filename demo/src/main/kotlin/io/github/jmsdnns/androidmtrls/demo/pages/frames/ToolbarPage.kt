package io.github.jmsdnns.androidmtrls.demo.pages.frames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMToolbar
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMToolbarButton
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// Toolbar catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMToolbar] and [AMToolbarButton].
 *
 * id: "frames-toolbar", section: Frames.
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the Frames slot.
 * This file intentionally does not touch any shared registry — registration is
 * the task owner's responsibility per the task brief.
 */
val toolbarEntry: ComponentEntry = ComponentEntry(
    id = "frames-toolbar",
    section = ComponentSection.Frames,
    title = "Toolbar",
    subtitle = "Top bar with title & actions",
    intro = "Top bar with a centered title and optional leading + trailing AMToolbarButtons. " +
        "Replaces the stock navigation bar when you want the AndroidMtrls look on a " +
        "non-navigation screen.",
    icon = Icons.Default.ViewAgenda,
    content = { ToolbarPage() },
)

// ---------------------------------------------------------------------------
// ToolbarPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMToolbar] and [AMToolbarButton].
 *
 * A "Variants" group showing three toolbar configurations:
 * - **title + leading + trailing** — full toolbar with menu, title, and compose button.
 * - **title only** — centered headline with no edge actions (balanced spacers used internally).
 * - **leading + trailing, no title** — back + more buttons with no center content.
 */
@Composable
fun ToolbarPage() {
    // ── Variants ──────────────────────────────────────────────────────────────
    catalogGroup(title = "Variants") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            // 1. Full toolbar: leading + title + trailing
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

            // 2. Title only — no leading or trailing slots
            AMToolbar(title = "Title only")

            // 3. Leading + trailing, no title
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
                        icon = Icons.Default.MoreHoriz,
                        contentDescription = "More",
                        onClick = {},
                    )
                },
            )
        }
    }
}
