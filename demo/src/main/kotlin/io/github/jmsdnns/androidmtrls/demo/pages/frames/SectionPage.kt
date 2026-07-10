package io.github.jmsdnns.androidmtrls.demo.pages.frames

import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMSection
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMSectionStyle
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Section catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMSection].
 *
 * id: "frames-section", section: Frames.
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the Frames slot.
 * This file intentionally does not touch any shared registry — registration is
 * the task owner's responsibility per the task brief.
 */
val sectionEntry: ComponentEntry = ComponentEntry(
    id = "frames-section",
    section = ComponentSection.Frames,
    title = "Section",
    subtitle = "Header, card & footer in one",
    intro = "Bundles AMSectionHeader + AMCard + an optional footer caption into one call. " +
        "The escape hatch for the every-screen \"header + card + footnote\" pattern.",
    icon = Icons.Default.Layers,
    content = { SectionPage() },
)

// ---------------------------------------------------------------------------
// SectionPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMSection].
 *
 * Shows two styles per the iOS demo (no catalogGroup wrappers — sections carry
 * their own headers):
 *
 * - **Flat style** — [AMSectionStyle.Flat]: body copy on a flat surface fill.
 * - **Card style** — [AMSectionStyle.Card] (default): AMCard with block padding
 *   and an optional footer caption.
 */
@Composable
fun SectionPage() {
    val isDark = LocalAMIsDarkTheme.current

    // ── Flat style ────────────────────────────────────────────────────────────
    // AMSection carries its own header (and optional footer), so the
    // sections sit directly under the page title. Wrapping them in
    // another titled group would stack a second header on top.
    AMSection("Flat", style = AMSectionStyle.Flat) {
        BasicText(
            text = "Body copy sits below the header on a flat surface fill — no card chrome " +
                "to compete with the reading rhythm.",
            style = TextStyle(
                color = AMTheme.Text.primary.resolve(isDark),
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
        )
    }

    AMSection(
        "Card",
        footer = "AMSection bundles AMSectionHeader + AMCard + footer.",
    ) {
        BasicText(
            text = "Used for any titled group of related content. The header sits inset from " +
                "the screen edge; the card stretches inside that inset so it lines up with " +
                "other AMCard blocks on the page.",
            style = TextStyle(
                color = AMTheme.Text.primary.resolve(isDark),
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
        )
    }
}
