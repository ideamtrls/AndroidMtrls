package io.github.jmsdnns.androidmtrls.demo.pages.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMFormActions
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Form Actions catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMFormActions].
 *
 * id: "buttons-formactions", section: Buttons.
 */
val formActionsEntry: ComponentEntry = ComponentEntry(
    id = "buttons-formactions",
    section = ComponentSection.Buttons,
    title = "Form Actions",
    tileTitle = "Form",
    subtitle = "Save / Delete form footer row",
    intro = "The paired action row that closes a form sheet: a disable-aware primary " +
        "commit button, plus — for edit sheets — a destructive secondary beside it. Both " +
        "fill the surface body of a filled button style, so one Row splits them into equal widths.",
    icon = Icons.Filled.FactCheck,
    content = { FormActionsPage() },
)

// ---------------------------------------------------------------------------
// FormActionsPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMFormActions], mirroring the iOS demo (Form Actions):
 * - **Save only** — the create-mode overload rendered enabled, then with
 *   `primaryEnabled = false` to show the disabled commit state.
 * - **Delete & Save** — the edit-mode overload with a leading destructive Delete
 *   and a trailing Save at equal widths.
 *
 * Both groups host the action row inside a padded [AMCard], matching a form sheet's
 * footer placement.
 */
@Composable
fun FormActionsPage() {
    // ── Save only (create mode) ──────────────────────────────────────────────
    catalogGroup(title = "Save only") {
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            padding = AMPadding.block,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
            ) {
                AMFormActions(primaryLabel = "Save", onPrimary = {})
                AMFormActions(primaryLabel = "Save", primaryEnabled = false, onPrimary = {})
                FormActionsCaption("Second row: primaryEnabled = false")
            }
        }
    }

    // ── Delete & Save (edit mode) ────────────────────────────────────────────
    catalogGroup(title = "Delete & Save") {
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            padding = AMPadding.block,
        ) {
            AMFormActions(
                destructiveLabel = "Delete",
                primaryLabel = "Save",
                onDestructive = {},
                onPrimary = {},
            )
        }
    }
}

// ---------------------------------------------------------------------------
// FormActionsCaption — 12sp monospaced secondary note
// ---------------------------------------------------------------------------

@Composable
private fun FormActionsCaption(text: String) {
    val isDark = LocalAMIsDarkTheme.current
    // Matches iOS .font(.caption.monospaced()).foregroundStyle(secondary).
    BasicText(
        text = text,
        style = AMTheme.Type.caption
            .toTextStyle(AMTheme.Text.secondary.resolve(isDark))
            .copy(fontFamily = FontFamily.Monospace),
    )
}
