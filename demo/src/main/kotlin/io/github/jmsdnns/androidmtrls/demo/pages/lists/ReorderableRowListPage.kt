package io.github.jmsdnns.androidmtrls.demo.pages.lists

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.lists.AMListRow
import io.github.jmsdnns.androidmtrls.lists.AMReorderableRowList
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMReorderableRowList].
 *
 * id: "lists-reorderablerowlist", section: [ComponentSection.Lists].
 * tileTitle "Reorder" per demo spec §3.7 tile override.
 */
val reorderableRowListEntry: ComponentEntry = ComponentEntry(
    id = "lists-reorderablerowlist",
    section = ComponentSection.Lists,
    title = "Reorderable Row List",
    tileTitle = "Reorder",
    subtitle = "Drag-to-reorder rows",
    intro = "AndroidMtrls List + reorder for row-style content. Native Android edit-mode UX: " +
        "long-press anywhere to reorder, or drive an external Edit/Done button via " +
        "the editMode binding.",
    icon = Icons.Default.SwapVert,
    content = { ReorderableRowListPage() },
)

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

private data class QueueItem(val id: Int, val title: String, val show: String)

private val initialQueue = listOf(
    QueueItem(1, "Reasoning about reasoning",   "Latent Notes"),
    QueueItem(2, "Cache-warm policies",          "Distributed Diaries"),
    QueueItem(3, "What \"agentic\" really means", "Latent Notes"),
    QueueItem(4, "Long context vs. retrieval",   "Distributed Diaries"),
    QueueItem(5, "Speculative decoding, plain",  "The Inference Hour"),
)

// ---------------------------------------------------------------------------
// ReorderableRowListPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMReorderableRowList].
 *
 * One group per demo spec §3.7:
 *
 * - **Default** — 5 podcast queue items (title + show name) held in [remember]
 *   state, rendered via [AMReorderableRowList] with [AMListRow] title+subtitle
 *   cells. A long-press hint caption appears below the list.
 */
@Composable
fun ReorderableRowListPage() {

    var queue by remember { mutableStateOf(initialQueue) }

    // ── Default ──────────────────────────────────────────────────────────────
    catalogGroup(title = "Default") {
        val isDark = LocalAMIsDarkTheme.current

        // Long-press hint caption — above the list, mirroring iOS placement.
        BasicText(
            text = "Long-press a row, then drag to reorder.",
            style = TextStyle(
                color = AMTheme.Text.secondary.resolve(isDark),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        )

        Spacer(modifier = Modifier.height(AMTheme.Spacing.sm.dp))

        AMReorderableRowList(
            items = queue,
            key = { it.id },
            onReorder = { queue = it },
            isEditing = false,
            modifier = Modifier
                .fillMaxWidth()
                // Bounded height is required: ComponentPage wraps content in
                // verticalScroll; LazyColumn inside an unbounded-height scroll
                // container throws a crash. 320dp fits 5 rows comfortably.
                .heightIn(max = 320.dp),
        ) { item, _ ->
            AMListRow(
                title = item.title,
                subtitle = item.show,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
