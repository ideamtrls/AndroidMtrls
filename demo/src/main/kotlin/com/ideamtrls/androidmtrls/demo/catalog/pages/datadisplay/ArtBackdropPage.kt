package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMSegmentedControl
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amArtBackdrop
import com.ideamtrls.androidmtrls.amArtText

internal val ArtBackdropEntry = ComponentEntry(
    id = "artBackdrop",
    category = CatalogCategory.DataDisplay,
    title = "Art backdrop",
    summary = "Heavy, saturated blur of art behind a screen",
    icon = R.drawable.ic_wallpaper,
    keywords = listOf(
        "amArtBackdrop", "amPageBackdrop", "amArtRows", "amArtRow", "amArtText", "page backdrop", "art rows",
        "color under cards", "blur", "saturation", "wash", "cover", "album", "artwork", "backdrop",
    ),
) { ArtBackdropPage() }

@Composable
private fun ArtBackdropPage() {
    ComponentPage(
        ArtBackdropEntry,
        preview = { PreviewArt() },
    ) {
        CoverBackdrop()
    }
}

@Immutable
private class ArtAlbum(val seed: String, val title: String, val detail: String) {
    val cover: String
        get() = catalogPhoto(seed, 600)
}

private val ArtAlbums = listOf(
    ArtAlbum("orchard", "Orchard", "Field Notes · 9 songs · 38 min"),
    ArtAlbum("cobalt", "Cobalt", "Field Notes · 11 songs · 44 min"),
)

/** One cover behind an album's title in art text. */
@Composable
private fun PreviewArt() {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .clip(MaterialTheme.shapes.large)
            .amArtBackdrop(catalogPhoto("cobalt", 600)),
        contentAlignment = Alignment.Center,
    ) {
        ArtTitle("Cobalt", "Field Notes · 9 songs · 38 min", Modifier.padding(AMTheme.tokens.spacing.lg))
    }
}

/** An album over its own art; picking the other album fades the art across. */
@Composable
private fun CoverBackdrop() {
    val spacing = AMTheme.tokens.spacing
    var seed by rememberSaveable { mutableStateOf(ArtAlbums.first().seed) }
    val album = ArtAlbums.first { it.seed == seed }
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 340.dp)
            .clip(MaterialTheme.shapes.large)
            .amArtBackdrop(album.cover),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            AMRemoteImage(album.cover, contentDescription = null, Modifier.size(120.dp).clip(MaterialTheme.shapes.medium))
            ArtTitle(album.title, album.detail)
            // The art is washed dark in both modes, like its white text, so the control takes its
            // dark look: light labels over the art in light mode too.
            AMTheme(darkTheme = true) {
                AMSegmentedControl(
                    ArtAlbums,
                    label = { it.title },
                    selected = album,
                    onSelectedChange = { seed = it.seed },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** An album's title over its details, white on the art. */
@Composable
private fun ArtTitle(title: String, detail: String, modifier: Modifier = Modifier) {
    Column(
        modifier.amArtText(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xs),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(detail, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}
