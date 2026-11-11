package com.ideamtrls.androidmtrls.demo

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMListCard
import com.ideamtrls.androidmtrls.AMReorderableCardList
import com.ideamtrls.androidmtrls.AMTag
import com.ideamtrls.androidmtrls.AMTagTint
import com.ideamtrls.androidmtrls.AMTheme

internal val ReorderableCardListEntry = ComponentEntry(
    id = "reorderableCardList",
    category = CatalogCategory.Lists,
    title = "Reorderable card list",
    summary = "Drag-to-reorder list of cards",
    icon = R.drawable.ic_stacks,
    keywords = listOf(
        "AMReorderableCardList", "AMListCard", "reorder", "drag", "drag handle", "move", "onMove", "LazyColumn",
        "cards", "tasks", "page", "verticalScroll",
    ),
) { ReorderableCardListPage() }

@Composable
private fun ReorderableCardListPage() {
    // The list's cards carry the page gutter as their own margins, so it runs edge to edge; in the
    // page's scroll it's as tall as its cards and scrolls with the page.
    ComponentPage(
        ReorderableCardListEntry,
        previewLayout = StageLayout.Wide,
        loadedLayout = StageLayout.Wide,
        preview = { TaskList(Board, R.drawable.ic_calendar_today) },
    ) {
        TaskList(Today, R.drawable.ic_schedule)
    }
}

@Immutable
private class ListsTask(val id: Int, val title: String, val due: String, val tint: AMTagTint)

private val Board = listOf(
    ListsTask(1, "Draft inference-hour outline", "Today", AMTagTint.Destructive),
    ListsTask(2, "Review beta build cutlist", "Mon", AMTagTint.Warning),
    ListsTask(3, "Coffee with Grace", "Wed", AMTagTint.Info),
)

private val Today = listOf(
    ListsTask(1, "Send conference abstract", "9:00 AM", AMTagTint.Destructive),
    ListsTask(2, "Review beta build cutlist", "10:30 AM", AMTagTint.Warning),
    ListsTask(3, "Coffee with Grace", "Noon", AMTagTint.Info),
    ListsTask(4, "Draft inference-hour outline", "2:00 PM", AMTagTint.Info),
)

/** Tasks as cards, each with its due tag, in the order you drag them to, scrolling with the page. */
@Composable
private fun TaskList(tasks: List<ListsTask>, @DrawableRes dueIcon: Int) {
    val order = rememberListsItems(tasks) { it.id }
    val icon = painterResource(dueIcon)
    AMReorderableCardList(
        order,
        key = { it.id },
        onMove = { from, to -> order.add(to, order.removeAt(from)) },
        modifier = Modifier.fillMaxWidth(),
    ) { task ->
        AMListCard(
            supportingContent = {
                AMTag(task.due, Modifier.padding(top = AMTheme.tokens.spacing.sm), icon = icon, tint = task.tint)
            },
        ) {
            Text(task.title)
        }
    }
}
