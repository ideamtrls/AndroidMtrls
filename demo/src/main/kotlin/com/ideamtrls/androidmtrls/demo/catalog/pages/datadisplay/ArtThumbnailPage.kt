package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
        UpNext()
    }
}

/** One thumbnail on a surface strip. */
@Composable
private fun PreviewArtThumbnail() {
    Box(Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(AMTheme.tokens.spacing.lg)) {
        AMArtThumbnail(catalogPhoto("orchard", 200), contentDescription = null)
    }
}

/** A queue card: two songs with their covers, then one whose cover isn't there, showing the note. */
@Composable
private fun UpNext() {
    val inset = AMTheme.tokens.spacing.lg
    Column(Modifier.fillMaxWidth().amCard()) {
        AMListRow(
            "Harbor Lights",
            subtitle = "The Lanterns",
            leadingContent = { AMArtThumbnail(catalogPhoto("salt", 200), contentDescription = null) },
        )
        AMDivider(inset = inset)
        AMListRow(
            "Low Tide",
            subtitle = "Marlow Fields",
            leadingContent = { AMArtThumbnail(catalogPhoto("mountain", 200), contentDescription = null) },
        )
        AMDivider(inset = inset)
        AMListRow(
            "Paper Boats",
            subtitle = "Juniper Row",
            leadingContent = {
                AMArtThumbnail(url = null, contentDescription = null, icon = painterResource(R.drawable.ic_music_note))
            },
        )
    }
}
