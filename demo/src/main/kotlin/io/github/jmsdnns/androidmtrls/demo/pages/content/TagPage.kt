package io.github.jmsdnns.androidmtrls.demo.pages.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMTag
import io.github.jmsdnns.androidmtrls.content.AMTagTint
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens

// ---------------------------------------------------------------------------
// Tag catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for the [AMTag] demo page.
 *
 * iOS equivalent: `IMTagPage.entry` in `Demo/Sources/Catalog/Pages/Content.swift`.
 */
val tagEntry: ComponentEntry = ComponentEntry(
    id = "content-tag",
    section = ComponentSection.Content,
    title = "Tag",
    subtitle = "Tinted pill label",
    intro = "Pill-shaped label with an optional leading icon and a semantic tint " +
            "(NEUTRAL / INFO / SUCCESS / WARNING / DESTRUCTIVE).",
    icon = Icons.Default.Label,
    content = { TagPage() },
)

// ---------------------------------------------------------------------------
// TagPage — demo composable
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMTag].
 *
 * One group — Variants — mirroring iOS `IMTagPage`:
 * Row 1: Neutral, With icon (star), Info
 * Row 2: Success + checkmark, Warning + triangle, Error + xmark
 */
@Composable
fun TagPage() {
    val tokens = LocalAMTokens.current

    // ── Variants ─────────────────────────────────────────────────────────────
    catalogGroup(title = "Variants") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            // Row 1: Neutral, With icon, Info
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            ) {
                AMTag(text = "Neutral")
                AMTag(text = "With icon", icon = Icons.Default.Star)
                AMTag(text = "Info", tint = AMTagTint.INFO)
            }

            // Row 2: Success + checkmark, Warning + triangle, Error + xmark
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            ) {
                AMTag(
                    text = "Success",
                    icon = Icons.Default.Check,
                    tint = AMTagTint.SUCCESS,
                )
                AMTag(
                    text = "Warning",
                    icon = Icons.Default.Warning,
                    tint = AMTagTint.WARNING,
                )
                AMTag(
                    text = "Error",
                    icon = Icons.Default.Clear,
                    tint = AMTagTint.DESTRUCTIVE,
                )
            }
        }
    }
}
