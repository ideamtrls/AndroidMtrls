package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMArtThumbnail
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val ArtThumbnailEntry = ComponentEntry(
    id = "artThumbnail",
    category = CatalogCategory.DataDisplay,
    title = "Art thumbnail",
    summary = "Square cover art with a placeholder glyph",
    icon = R.drawable.ic_queue_music,
    keywords = listOf("AMArtThumbnail", "cover", "album art", "artwork", "thumbnail", "placeholder", "row", "bitmap"),
) { ArtThumbnailPage() }

@Composable
private fun ArtThumbnailPage() {
    ComponentPage(
        ArtThumbnailEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { PreviewArtThumbnail() },
    ) {
        RecentTracksCard()
    }
}

/** One thumbnail on a surface strip. */
@Composable
private fun PreviewArtThumbnail() {
    Box(Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(AMTheme.tokens.spacing.lg)) {
        AMArtThumbnail(CatalogArt.Mountain.url(AMTheme.isDark), contentDescription = null)
    }
}

@Immutable
private class DataDisplayTrack(val title: String, val artist: String, val art: CatalogArt?)

private val RecentTracks = listOf(
    DataDisplayTrack("Above the Treeline", "The Low Tides", CatalogArt.Peaks),
    DataDisplayTrack("Evening Light", "Marigold", CatalogArt.Forest),
    DataDisplayTrack("First Snow", "Neon Atlas", CatalogArt.Garden),
    DataDisplayTrack("Untitled Demo", "Voice Memo", null),
)

/** A card of recent tracks: three with their covers, then a demo with none, showing the note. */
@Composable
private fun RecentTracksCard() {
    val inset = AMTheme.tokens.spacing.lg
    val note = painterResource(R.drawable.ic_music_note)
    Column(Modifier.fillMaxWidth().amCard()) {
        RecentTracks.forEachIndexed { index, track ->
            if (index > 0) AMDivider(inset = inset)
            AMListRow(
                track.title,
                subtitle = track.artist,
                leadingContent = {
                    AMArtThumbnail(track.art?.url(AMTheme.isDark), contentDescription = null, icon = note)
                },
            )
        }
    }
}
