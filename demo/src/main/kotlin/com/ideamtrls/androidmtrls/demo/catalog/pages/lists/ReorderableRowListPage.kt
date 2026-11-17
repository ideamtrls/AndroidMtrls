package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMArtThumbnail
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
        "AMSwipeAction", "swipe", "remove", "page", "verticalScroll",
    ),
) { ReorderableRowListPage() }

@Composable
private fun ReorderableRowListPage() {
    // In the page's scroll each list is as tall as its rows and scrolls with the page.
    ComponentPage(
        ReorderableRowListEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { Queue() },
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xl)) {
            FrostedUpNext()
            UpNext()
        }
    }
}

// Stand-in for your own action.
private fun favorite() {}

@Immutable
private class ListsEpisode(val id: Int, val title: String, val show: String, val art: CatalogArt? = null)

private val Episodes = listOf(
    ListsEpisode(1, "Reasoning about reasoning", "Latent Notes"),
    ListsEpisode(2, "Cache-warm policies", "Distributed Diaries"),
    ListsEpisode(3, "What “agentic” really means", "Latent Notes"),
)

/** The first four episodes, each with its show's art. */
private val ArtEpisodes = listOf(
    ListsEpisode(1, "Reasoning about reasoning", "Latent Notes", CatalogArt.Peaks),
    ListsEpisode(2, "Cache-warm policies", "Distributed Diaries", CatalogArt.Forest),
    ListsEpisode(3, "What “agentic” really means", "Latent Notes", CatalogArt.Garden),
    ListsEpisode(4, "Shipping on a Friday", "Distributed Diaries", CatalogArt.Valley),
)

private val UpNextEpisodes = Episodes + listOf(
    ListsEpisode(4, "Shipping on a Friday", "Distributed Diaries"),
    ListsEpisode(5, "Tokens all the way down", "Latent Notes"),
    ListsEpisode(6, "The quorum that wasn’t", "Distributed Diaries"),
    ListsEpisode(7, "Evals before vibes", "Latent Notes"),
    ListsEpisode(8, "Retries, with feeling", "Distributed Diaries"),
)

/** A queue of three episodes on a surface strip, in the order you drag them to, scrolling with the page. */
@Composable
private fun Queue() {
    val queue = rememberListsItems(Episodes) { it.id }
    AMReorderableRowList(
        queue,
        key = { it.id },
        onMove = { from, to -> queue.add(to, queue.removeAt(from)) },
        modifier = Modifier.fillMaxWidth().background(AMTheme.Surface.primary),
    ) { episode ->
        AMListRow(episode.title, subtitle = episode.show)
    }
}

/**
 * The first four episodes in a frosted card over a photo, each led by its art: drag to reorder,
 * swipe to favorite or remove.
 */
@Composable
private fun FrostedUpNext() {
    val queue = rememberListsItems(ArtEpisodes) { it.id }
    val star = painterResource(R.drawable.ic_star)
    val delete = painterResource(R.drawable.ic_delete)
    val placeholder = painterResource(R.drawable.ic_graphic_eq)
    CatalogFrostedStage(art = CatalogArt.Mountain) {
        AMReorderableRowList(
            queue,
            key = { it.id },
            onMove = { from, to -> queue.add(to, queue.removeAt(from)) },
            modifier = Modifier.padding(AMTheme.tokens.spacing.lg).fillMaxWidth().amCard(),
            startAction = { AMSwipeAction("Favorite", star, AMSemantic.Warning, onClick = ::favorite) },
            endAction = { episode ->
                AMSwipeAction("Remove", delete, AMSemantic.Destructive, removesRow = true) { queue.remove(episode) }
            },
        ) { episode ->
            AMListRow(
                episode.title,
                subtitle = episode.show,
                leadingContent = {
                    AMArtThumbnail(episode.art?.url(AMTheme.isDark), contentDescription = null, icon = placeholder)
                },
            )
        }
    }
}

/**
 * What plays next, in one card as long as its rows, scrolling with the page: drag to reorder (the
 * page scrolls near its edges), swipe to favorite or remove.
 */
@Composable
private fun UpNext() {
    val queue = rememberListsItems(UpNextEpisodes) { it.id }
    val star = painterResource(R.drawable.ic_star)
    val delete = painterResource(R.drawable.ic_delete)
    AMReorderableRowList(
        queue,
        key = { it.id },
        onMove = { from, to -> queue.add(to, queue.removeAt(from)) },
        modifier = Modifier.fillMaxWidth().amCard(),
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
