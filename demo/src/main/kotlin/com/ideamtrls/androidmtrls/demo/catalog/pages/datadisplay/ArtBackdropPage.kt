package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMArtDefaults
import com.ideamtrls.androidmtrls.AMArtTone
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMElevation
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMSegmentedControl
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amArtBackdrop
import com.ideamtrls.androidmtrls.amArtText
import com.ideamtrls.androidmtrls.amElevation

internal val ArtBackdropEntry = ComponentEntry(
    id = "artBackdrop",
    category = CatalogCategory.DataDisplay,
    title = "Art backdrop",
    summary = "Heavy, saturated blur of art behind a screen",
    icon = R.drawable.ic_wallpaper,
    keywords = listOf(
        "amArtBackdrop", "AMArtDefaults", "AMArtTone", "amArtRows", "amArtRow", "amArtText", "page backdrop", "art rows", "tone", "light mode", "dark mode",
        "color under cards", "blur", "saturation", "wash", "cover", "album", "artwork", "backdrop",
    ),
) { ArtBackdropPage() }

@Composable
private fun ArtBackdropPage() {
    ComponentPage(
        ArtBackdropEntry,
        preview = { PreviewArt() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.lg)) {
            CoverBackdrop()
            UnwashedBackdrop()
        }
    }
}

/** An album: its cover as a photo's URL, or a solid picture at one of the art's extremes. */
@Immutable
private class ArtAlbum(val title: String, val detail: String, val seed: String? = null, val solid: ImageBitmap? = null) {
    val cover: String?
        get() = seed?.let { catalogPhoto(it, 600) }
}

/** Two photos, then an all-white and an all-black cover, the extremes the wash is tuned for. */
private val ArtAlbums = listOf(
    ArtAlbum("Orchard", "Field Notes · 9 songs · 38 min", seed = "orchard"),
    ArtAlbum("Cobalt", "Field Notes · 11 songs · 44 min", seed = "cobalt"),
    ArtAlbum("Snow", "All white · 1 song · 3 min", solid = SolidArt.White),
    ArtAlbum("Ink", "All black · 1 song · 3 min", solid = SolidArt.Black),
)

/** An album's art backdrop: its solid picture, or its photo. */
private fun Modifier.albumArt(album: ArtAlbum, lightWash: Float = AMArtDefaults.LightWash, darkWash: Float = AMArtDefaults.DarkWash): Modifier =
    if (album.solid != null) {
        amArtBackdrop(album.solid, lightWash = lightWash, darkWash = darkWash)
    } else {
        amArtBackdrop(album.cover, lightWash = lightWash, darkWash = darkWash)
    }

/**
 * Two pictures behind their titles in art text, a bright one and a deep one: the wash keeps both
 * light in light mode and dark in dark mode, and the text follows.
 */
@Composable
private fun PreviewArt() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.md)) {
        ArtTile("Pastel", "Burano", commonsPhoto("Houses of Burano painted with complementary colours.jpg", 600), Modifier.weight(1f))
        ArtTile("Blue hour", "Islamabad", commonsPhoto("Blue Hour at Pakistan Monument.jpg", 600), Modifier.weight(1f))
    }
}

/** A picture's art behind its title, at least 220 dp tall, in the large shape. */
@Composable
private fun ArtTile(title: String, detail: String, art: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 220.dp)
            .clip(MaterialTheme.shapes.large)
            .amArtBackdrop(art),
        contentAlignment = Alignment.Center,
    ) {
        ArtTitle(title, detail, Modifier.padding(AMTheme.tokens.spacing.lg))
    }
}

/**
 * An album over its own art, washed toward the mode; picking another album fades the art across.
 * The control and the text keep the mode's look over every cover, white and black ones included.
 */
@Composable
private fun CoverBackdrop() {
    val spacing = AMTheme.tokens.spacing
    var title by rememberSaveable { mutableStateOf(ArtAlbums.first().title) }
    val album = ArtAlbums.first { it.title == title }
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 340.dp)
            .clip(MaterialTheme.shapes.large)
            .albumArt(album),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            AlbumCover(album)
            ArtTitle(album.title, album.detail)
            AMSegmentedControl(
                ArtAlbums,
                label = { it.title },
                selected = album,
                onSelectedChange = { title = it.title },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * The black cover with no wash, in both modes: dark art, so its backdrop says so
 * ([AMArtTone.Dark]) and the art text on it turns white even in light mode.
 */
@Composable
private fun UnwashedBackdrop() {
    val album = ArtAlbums.last()
    AMBackdrop(
        art = { Box(Modifier.fillMaxSize().albumArt(album, lightWash = 0f, darkWash = 0f)) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).clip(MaterialTheme.shapes.large),
        tone = AMArtTone.Dark,
    ) {
        ArtTitle("Unwashed", "The art as it is, its tone given: dark", Modifier.align(Alignment.Center).padding(AMTheme.tokens.spacing.lg))
    }
}

/** The album's cover as a small art card: its solid picture, or its photo. */
@Composable
private fun AlbumCover(album: ArtAlbum) {
    val modifier = Modifier
        .size(120.dp)
        .amElevation(AMElevation.Low, MaterialTheme.shapes.medium)
        .clip(MaterialTheme.shapes.medium)
    if (album.solid != null) {
        Image(album.solid, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        AMRemoteImage(album.cover, contentDescription = null, modifier)
    }
}

/** An album's title over its details in art text, the details in the secondary ink. */
@Composable
private fun ArtTitle(title: String, detail: String, modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xs),
    ) {
        Text(title, Modifier.amArtText(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(detail, Modifier.amArtText(secondary = true), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}
