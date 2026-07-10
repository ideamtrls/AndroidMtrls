package io.github.jmsdnns.androidmtrls.demo.pages.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.collections.AMChipGroup
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// ChipGroup catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMChipGroup].
 *
 * id: "collections-chipgroup", section: [ComponentSection.Collections].
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the Collections
 * slot. This file intentionally does not touch any shared registry —
 * registration is the task owner's responsibility per the task brief.
 */
val chipGroupEntry: ComponentEntry = ComponentEntry(
    id = "collections-chipgroup",
    section = ComponentSection.Collections,
    title = "Chip Group",
    tileTitle = "Chips",
    subtitle = "Multi-select wrapping chips",
    intro = "Multi-select chip row. Caller supplies a list of items and a Set<ID> binding " +
        "for the selection; the row wraps to fit the available width.",
    icon = Icons.Default.Tag,
    content = { ChipGroupPage() },
)

// ---------------------------------------------------------------------------
// ChipGroupPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMChipGroup].
 *
 * One group per demo spec §3.9:
 *
 * - **Default** — 9 genre chips; initial selection = ids 2 and 4;
 *   live "Selected: …" readout below listing selected labels.
 */
@Composable
fun ChipGroupPage() {

    // Genre data — mirrors iOS preview & demo spec §3.9
    data class Genre(val id: Int, val name: String)

    val genres = listOf(
        Genre(1, "Tech"),
        Genre(2, "Long-form"),
        Genre(3, "Interview"),
        Genre(4, "Daily"),
        Genre(5, "Science"),
        Genre(6, "Comedy"),
        Genre(7, "History"),
        Genre(8, "Design"),
        Genre(9, "Engineering"),
    )

    var selection by remember { mutableStateOf(setOf<Any>(2, 4)) }

    val isDark = LocalAMIsDarkTheme.current

    // ── Default ───────────────────────────────────────────────────────────────
    catalogGroup(title = "Default") {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Full-width Surface.primary strip with horizontal + vertical padding,
            // matching iOS: .padding(horizontal: 14, vertical: 16) + .background(Surface.primary)
            AMChipGroup(
                items = genres,
                key = { it.id },
                label = { it.name },
                selection = selection,
                onSelectionChange = { selection = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AMTheme.Surface.primary.resolve(isDark))
                    .padding(horizontal = 14.dp, vertical = 16.dp),
            )

            Spacer(modifier = Modifier.height(AMTheme.Spacing.md.dp))

            // Live "Selected: …" readout — sorted IDs as integers, 12sp secondary
            // caption with horizontal padding 14, matching iOS:
            // Text("Selected: \(picked.sorted().map(String.init).joined(separator: ", "))")
            val selectedIds = selection
                .filterIsInstance<Int>()
                .sorted()
                .joinToString(", ")
                .ifEmpty { "None" }

            BasicText(
                text = "Selected: $selectedIds",
                style = TextStyle(
                    color = AMTheme.Text.secondary.resolve(isDark),
                    fontSize = 12.sp,
                ),
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }
    }

}
