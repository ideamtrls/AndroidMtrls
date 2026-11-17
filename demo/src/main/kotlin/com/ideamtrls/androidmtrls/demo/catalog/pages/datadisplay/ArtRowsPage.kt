package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMArtThumbnail
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amArtRow
import com.ideamtrls.androidmtrls.amArtRows
import com.ideamtrls.androidmtrls.amArtText
import com.ideamtrls.androidmtrls.amRowPadding

internal val ArtRowsEntry = ComponentEntry(
    id = "artRows",
    category = CatalogCategory.DataDisplay,
    title = "Art rows",
    summary = "Each row on its own art, blending down the page",
    icon = R.drawable.ic_art_track,
    keywords = listOf(
        "amArtRows", "amArtRow", "AMArtRowsBackdrop", "art rows", "LazyColumn", "lazy", "feed", "episodes",
        "podcast", "cover", "blend", "wash", "long list",
    ),
) { ArtRowsPage() }

@Composable
private fun ArtRowsPage() {
    // The feed is a small screen of its own: an edge-to-edge band of its height.
    ComponentPage(ArtRowsEntry, loadedLayout = StageLayout.Fill) {
        EpisodeFeed()
    }
}

@Immutable
private class DataDisplayShow(val name: String, val art: CatalogArt)

private val Shows = listOf(
    DataDisplayShow("Latent Notes", CatalogArt.Peaks),
    DataDisplayShow("Distributed Diaries", CatalogArt.Forest),
    DataDisplayShow("Field Recordings", CatalogArt.Garden),
    DataDisplayShow("The Long Build", CatalogArt.Valley),
    DataDisplayShow("Small Hours", CatalogArt.Mountain),
)

private val FeedTopics = listOf(
    "Reasoning about reasoning", "Cache-warm policies", "Shipping on a Friday", "The quiet refactor",
    "What the logs remember", "Designing for one hand", "A year of small releases", "Naming things, again",
    "When the tests lie", "Latency you can feel", "The second draft", "Reading old code",
)

@Immutable
private class DataDisplayEpisode(val number: Int, val title: String, val show: DataDisplayShow)

private const val FeedEpisodeCount = 240

/** Every episode, newest first, its topic and show cycling. */
private val FeedEpisodes = List(FeedEpisodeCount) { index ->
    val number = FeedEpisodeCount - index
    DataDisplayEpisode(number, "$number. ${FeedTopics[index % FeedTopics.size]}", Shows[index % Shows.size])
}

private val FeedHeight = 560.dp

/**
 * A feed of every episode in a lazy list of its own height, which composes only the rows on
 * screen: each sits on its show's art, blending into the next.
 */
@Composable
private fun EpisodeFeed() {
    val dark = AMTheme.isDark
    LazyColumn(Modifier.fillMaxWidth().height(FeedHeight).clipToBounds().amArtRows()) {
        itemsIndexed(FeedEpisodes, key = { _, episode -> episode.number }) { index, episode ->
            val art = episode.show.art.url(dark)
            Column(Modifier.fillMaxWidth().amArtRow(art)) {
                if (index > 0) AMDivider(inset = AMTheme.tokens.spacing.lg)
                EpisodeRow(episode, art)
            }
        }
    }
}

/** An episode's row, straight on its art: the show's art beside the episode's title over the show's name. */
@Composable
private fun EpisodeRow(episode: DataDisplayEpisode, art: String) {
    Row(
        Modifier.fillMaxWidth().amRowPadding().semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AMArtThumbnail(art, contentDescription = null, icon = painterResource(R.drawable.ic_graphic_eq))
        Column {
            Text(episode.title, Modifier.amArtText(), style = MaterialTheme.typography.bodyLarge)
            Text(episode.show.name, Modifier.amArtText(secondary = true), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
