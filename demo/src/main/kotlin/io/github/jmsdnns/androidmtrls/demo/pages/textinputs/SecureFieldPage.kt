package io.github.jmsdnns.androidmtrls.demo.pages.textinputs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.inputs.AMFieldLabel
import io.github.jmsdnns.androidmtrls.controls.inputs.AMSecureField
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// SecureField catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMSecureField].
 *
 * id: "textinputs-securefield", section: [ComponentSection.TextInputs].
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the TextInputs
 * slot. This file intentionally does not touch any shared registry —
 * registration is the task owner's responsibility per the task brief.
 */
val secureFieldEntry: ComponentEntry = ComponentEntry(
    id = "textinputs-securefield",
    section = ComponentSection.TextInputs,
    title = "Secure Field",
    tileTitle = "Secure",
    subtitle = "Password entry with reveal",
    intro = "Password input with a trailing eye button that toggles masking on and off. Shares " +
        "the filled anatomy of AMTextField — a white floating container with an accent bottom " +
        "indicator on focus; label it with a standing Field Label. The hand-drawn eye/eye-slash " +
        "glyph reveals or conceals the password.",
    icon = Icons.Default.Lock,
    content = { SecureFieldPage() },
)

// ---------------------------------------------------------------------------
// SecureFieldPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMSecureField] (flat-first, card-second):
 *
 * - **Label** — the field under a standing [AMFieldLabel], both directly on the
 *   page (the label sits on the backdrop, outside the field's white surface),
 *   with the eye reveal toggle.
 * - **Card** — a placeholder-only field inset inside an [AMCard].
 */
@Composable
fun SecureFieldPage() {
    // Persist field state across recompositions so the demo stays interactive.
    var labeledPassword by rememberSaveable { mutableStateOf("") }
    var cardPassword by rememberSaveable { mutableStateOf("") }

    // ── Label ──────────────────────────────────────────────────────────────────
    catalogGroup(title = "Label") {
        Column(
            modifier = Modifier.padding(horizontal = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp),
        ) {
            AMFieldLabel("Password")
            AMSecureField(
                value = labeledPassword,
                onValueChange = { labeledPassword = it },
            )
        }
    }

    // ── Card ──────────────────────────────────────────────────────────────────
    catalogGroup(title = "Card") {
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            AMSecureField(
                value = cardPassword,
                onValueChange = { cardPassword = it },
                placeholder = "Password",
                modifier = Modifier.padding(AMTheme.Spacing.lg.dp),
            )
        }
    }

}
