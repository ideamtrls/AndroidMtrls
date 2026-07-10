package io.github.jmsdnns.androidmtrls.demo.pages.lists

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Reorder
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
import io.github.jmsdnns.androidmtrls.content.AMTag
import io.github.jmsdnns.androidmtrls.content.AMTagTint
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.lists.AMListCard
import io.github.jmsdnns.androidmtrls.lists.AMReorderableCardList
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMReorderableCardList].
 *
 * id: "lists-reorderablecardlist", section: [ComponentSection.Lists].
 * tileTitle "Move" per demo spec §3.7 tile override (row 2 in Lists 2×2 grid).
 *
 * **Registration:** add this entry to `CatalogRegistry.all` in the Lists
 * slot. This file intentionally does not touch any shared registry —
 * registration is the task owner's responsibility per the task brief.
 */
val reorderableCardListEntry: ComponentEntry = ComponentEntry(
    id = "lists-reorderablecardlist",
    section = ComponentSection.Lists,
    title = "Reorderable Card List",
    tileTitle = "Move",
    subtitle = "Drag-to-reorder cards",
    intro = "UICollectionView-backed reorderable list for card-shaped content. Long-press " +
        "lifts a card; neighbors animate to make room; cards never shift to accommodate chrome.",
    icon = Icons.Default.Reorder,
    content = { ReorderableCardListPage() },
)

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

private data class TaskItem(
    val id: Int,
    val title: String,
    val due: String,
    val tagTint: AMTagTint,
)

private val initialTasks = listOf(
    TaskItem(1, "Draft inference-hour outline", "Today",  AMTagTint.DESTRUCTIVE),
    TaskItem(2, "Review beta build cutlist",    "Mon",    AMTagTint.WARNING),
    TaskItem(3, "Coffee with Grace",            "Wed",    AMTagTint.INFO),
    TaskItem(4, "Send conference abstract",     "Fri",    AMTagTint.INFO),
    TaskItem(5, "Reply to Marcus",              "Aug 14", AMTagTint.INFO),
)

// ---------------------------------------------------------------------------
// ReorderableCardListPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMReorderableCardList].
 *
 * One group per demo spec §3.7:
 *
 * - **Default** — 5 task cards held in [remember] state, each rendered as an
 *   [AMListCard] containing a task title ([BasicText] 17sp SemiBold) and an
 *   [AMTag] showing the task category. A long-press hint caption appears below.
 *
 * The [AMReorderableCardList] is given `Modifier.heightIn(max = 460.dp)` so it
 * has a bounded height inside the outer `verticalScroll` [Column] used by
 * [ComponentPage]. Without a bound, Compose cannot measure an unbounded-height
 * lazy list inside a scrollable container. 460dp comfortably shows all 5 cards
 * (each card ~72dp + 8dp margins) with room to scroll if content grows.
 */
@Composable
fun ReorderableCardListPage() {

    val isDark = LocalAMIsDarkTheme.current
    var tasks by remember { mutableStateOf(initialTasks) }

    // ── Default ──────────────────────────────────────────────────────────────
    catalogGroup(title = "Default") {
        // Long-press hint caption — above the list, mirroring iOS placement.
        BasicText(
            text = "Long-press a card, then drag to reorder.",
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

        AMReorderableCardList(
            items = tasks,
            key = { it.id },
            onReorder = { tasks = it },
            modifier = Modifier
                .fillMaxWidth()
                // Bounded height is required: ComponentPage wraps content in
                // verticalScroll; LazyColumn inside an unbounded-height scroll
                // container throws a crash. 460dp fits 5 task cards comfortably.
                .heightIn(max = 460.dp),
        ) { task ->
            AMListCard(modifier = Modifier.fillMaxWidth()) {
                BasicText(
                    text = task.title,
                    style = TextStyle(
                        color = AMTheme.Text.primary.resolve(isDark),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                ) {
                    AMTag(
                        text = task.due,
                        icon = Icons.Default.CalendarToday,
                        tint = task.tagTint,
                    )
                    Spacer(modifier = Modifier.width(AMTheme.Spacing.md.dp))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
