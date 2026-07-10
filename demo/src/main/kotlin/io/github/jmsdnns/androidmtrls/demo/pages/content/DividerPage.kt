package io.github.jmsdnns.androidmtrls.demo.pages.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMDivider
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Divider catalog entry
// ---------------------------------------------------------------------------

val dividerEntry: ComponentEntry = ComponentEntry(
    id = "content-divider",
    section = ComponentSection.Content,
    title = "Divider",
    subtitle = "1pt hairline separator",
    intro = "A 1pt hairline at AMTheme.separator — for separating rows or groups within a " +
        "card. Supports a leading inset to line up with content under a row's leading icon.",
    icon = Icons.Default.HorizontalRule,
    content = { DividerPage() },
)

// ---------------------------------------------------------------------------
// DividerPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMDivider].
 *
 * One group — "Default & inset" — mirroring iOS `IMDividerPage`:
 * Text "Above", full-width divider, text "Between", 14dp-inset divider,
 * text "Below (inset 14)".
 */
@Composable
fun DividerPage() {
    val isDark = LocalAMIsDarkTheme.current

    catalogGroup(title = "Default & inset") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AMTheme.Surface.primary.resolve(isDark))
                .padding(AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            BasicText(
                text = "Above",
                style = androidx.compose.ui.text.TextStyle(
                    color = AMTheme.Text.primary.resolve(isDark),
                ),
            )
            AMDivider()
            BasicText(
                text = "Between",
                style = androidx.compose.ui.text.TextStyle(
                    color = AMTheme.Text.primary.resolve(isDark),
                ),
            )
            AMDivider(inset = 14.dp)
            BasicText(
                text = "Below (inset 14)",
                style = androidx.compose.ui.text.TextStyle(
                    color = AMTheme.Text.primary.resolve(isDark),
                ),
            )
        }
    }
}
