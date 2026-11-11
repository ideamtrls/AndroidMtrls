package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMReorderableRowList
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMSwipeAction
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val ReorderableRowListEntry = ComponentEntry(
    id = "reorderableRowList",
    category = CatalogCategory.Lists,
    title = "Reorderable row list",
    summary = "Drag-to-reorder list of flat rows",
    icon = R.drawable.ic_reorder,
    keywords = listOf(
        "AMReorderableRowList", "reorder", "drag", "drag handle", "move", "onMove", "LazyColumn", "queue",
        "AMSwipeAction", "swipe", "remove",
    ),
) { ReorderableRowListPage() }

@Composable
private fun ReorderableRowListPage() {
    ComponentPage(
        ReorderableRowListEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { Queue() },
    ) {
        UpNext()
    }
}

// Stand-in for your own action.
private fun favorite() {}

@Immutable
private class ListsEpisode(val id: Int, val title: String, val show: String)

private val Episodes = listOf(
    ListsEpisode(1, "Reasoning about reasoning", "Latent Notes"),
    ListsEpisode(2, "Cache-warm policies", "Distributed Diaries"),
    ListsEpisode(3, "What “agentic” really means", "Latent Notes"),
)

private val UpNextEpisodes = Episodes + ListsEpisode(4, "Shipping on a Friday", "Distributed Diaries")

/** A queue of three episodes on a surface strip, in the order you drag them to. */
@Composable
private fun Queue() {
    val queue = rememberListsItems(Episodes) { it.id }
    AMReorderableRowList(
        queue,
        key = { it.id },
        onMove = { from, to -> queue.add(to, queue.removeAt(from)) },
        modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).background(AMTheme.Surface.primary),
    ) { episode ->
        AMListRow(episode.title, subtitle = episode.show)
    }
}

/** What plays next, in a card: drag to reorder, swipe to favorite or remove. */
@Composable
private fun UpNext() {
    val queue = rememberListsItems(UpNextEpisodes) { it.id }
    val star = painterResource(R.drawable.ic_star)
    val delete = painterResource(R.drawable.ic_delete)
    AMReorderableRowList(
        queue,
        key = { it.id },
        onMove = { from, to -> queue.add(to, queue.removeAt(from)) },
        modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).amCard(),
        startAction = { AMSwipeAction("Favorite", star, AMSemantic.Warning, onClick = ::favorite) },
        endAction = { episode ->
            AMSwipeAction("Remove", delete, AMSemantic.Destructive, removesRow = true) { queue.remove(episode) }
        },
    ) { episode ->
        AMListRow(
            episode.title,
            subtitle = episode.show,
            leadingContent = {
                Icon(painterResource(R.drawable.ic_graphic_eq), contentDescription = null, tint = AMSemantic.Info.color)
            },
        )
    }
}
