package io.github.jmsdnns.androidmtrls.demo.pages.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMGhostButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMPrimaryButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMSecondaryButton
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMButtonRole
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// Button Styles catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for the three button styles: [AMPrimaryButton], [AMSecondaryButton],
 * and [AMGhostButton].
 *
 * id: "buttonStyles", section: Buttons.
 */
val buttonStylesEntry: ComponentEntry = ComponentEntry(
    id = "buttonStyles",
    section = ComponentSection.Buttons,
    title = "Button Styles",
    tileTitle = "Styles",
    subtitle = "Button style variants",
    intro = "Three button styles for the three weight levels: .amPrimary (filled), " +
        ".amSecondary (outlined), .amGhost (no fill). Apply via the matching button " +
        "composable on any click target.",
    icon = Icons.Default.TouchApp,
    content = { ButtonStylesPage() },
)

// ---------------------------------------------------------------------------
// ButtonStylesPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMPrimaryButton], [AMSecondaryButton], and [AMGhostButton].
 *
 * A single "Variants" group with two horizontal rows:
 * - Row 1: Primary / Secondary / Ghost
 * - Row 2: Disabled / Delete (destructive primary) / Delete (destructive ghost)
 */
@Composable
fun ButtonStylesPage() {
    // ── Variants ─────────────────────────────────────────────────────────────
    catalogGroup(title = "Variants") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            // Row 1: normal styles. CenterVertically mirrors the iOS HStack
            // default — the compact ghost button centers against the 64dp
            // filled buttons.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AMPrimaryButton(label = "Primary", onClick = {}, modifier = Modifier.weight(1f))
                AMSecondaryButton(label = "Secondary", onClick = {}, modifier = Modifier.weight(1f))
                AMGhostButton(label = "Ghost", onClick = {}, modifier = Modifier.weight(1f))
            }

            // Row 2: disabled + destructive
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AMPrimaryButton(
                    label = "Disabled",
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f),
                )
                AMPrimaryButton(
                    label = "Delete",
                    onClick = {},
                    role = AMButtonRole.Destructive,
                    modifier = Modifier.weight(1f),
                )
                AMGhostButton(
                    label = "Delete",
                    onClick = {},
                    role = AMButtonRole.Destructive,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
