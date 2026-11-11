package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMAvatarSize
import com.ideamtrls.androidmtrls.AMElevation
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMRemoteImageFallback
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amArtBackdrop
import com.ideamtrls.androidmtrls.amArtRow
import com.ideamtrls.androidmtrls.amElevation

// The Music scene's artwork pieces: the backdrop fallback, the art and art rows from a cover or a
// URL, the album cover image, the art card, the cover thumbnail, and the now-playing marks — the
// tint, the speaker mark and the badge that carries it on an art card.


/**
 * An art backdrop from an album's art: its embedded [cover] once it's read, else the picture at
 * [url] (the project photo). The cover arriving fades in over the photo.
 */
internal fun Modifier.musicArtBackdrop(cover: ImageBitmap?, url: String?): Modifier =
    if (cover != null) amArtBackdrop(cover) else amArtBackdrop(url)

/** An art row's color from an album's art: its embedded [cover] once it's read, else the picture at [url]. */
internal fun Modifier.musicArtRow(cover: ImageBitmap?, url: String?): Modifier =
    if (cover != null) amArtRow(cover) else amArtRow(url)

/**
 * An album's art in [modifier]'s frame: [cover], the art embedded in its MP3s, once the model has
 * read it; until then, or when it can't be read, the picture at [url] (the project photo), with
 * [fallback] while that loads. Decorative: what's beside it names it.
 */
@Composable
internal fun MusicCoverImage(
    cover: ImageBitmap?,
    url: String?,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit,
) {
    if (cover != null) {
        Image(cover, contentDescription = null, modifier = modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    } else {
        AMRemoteImage(url, contentDescription = null, modifier = modifier, fallback = fallback)
    }
}

/**
 * Album or artist art as a card in [modifier]'s frame (callers give it a width and a shape: an
 * album's square, an artist's photo at its own aspect ratio, so the whole photo shows): the large
 * shape, the low shadow, the page color while the art loads. An album passes its embedded [cover]
 * too. With an [interactionSource], the ripple of the entry it belongs to shows on the card.
 */
@Composable
internal fun MusicArtCard(
    url: String?,
    cover: ImageBitmap?,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) {
    val shape = MaterialTheme.shapes.large
    val page = AMTheme.Surface.page
    MusicCoverImage(
        cover,
        url,
        modifier
            .amElevation(AMElevation.Low, shape)
            .clip(shape)
            .then(if (interactionSource != null) Modifier.indication(interactionSource, ripple()) else Modifier),
    ) {
        Box(Modifier.fillMaxSize().background(page))
    }
}

/**
 * A cover thumbnail, [size] square in the small shape: the embedded [cover] once it's read, else
 * the picture at [url], with the music note while there's none. Decorative: the row beside it names
 * the song.
 */
@Composable
internal fun MusicThumb(url: String?, cover: ImageBitmap?, size: Dp = AMAvatarSize.Medium.diameter) {
    MusicCoverImage(cover, url, Modifier.size(size).clip(MaterialTheme.shapes.small)) {
        AMRemoteImageFallback(icon = painterResource(R.drawable.ic_music_note))
    }
}

/** The now-playing tint: the playing song's title and the speaker mark, in the scheme's accent. */
internal val MusicNowPlayingTint: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary

/** The now-playing mark: the filled speaker at [size], in the now-playing tint. Decorative. */
@Composable
internal fun MusicSpeakerMark(size: Dp = 24.dp) {
    Icon(
        painterResource(R.drawable.ic_volume_up_filled),
        contentDescription = null,
        modifier = Modifier.size(size),
        tint = MusicNowPlayingTint,
    )
}

/**
 * The playing entry's badge on its art card: the speaker mark in a 32 dp floating-surface circle
 * with the low shadow. Callers set it in the card's top end corner, inset `spacing.md`.
 */
@Composable
internal fun MusicNowPlayingBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(32.dp)
            .amElevation(AMElevation.Low, CircleShape)
            .background(AMTheme.Surface.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        MusicSpeakerMark(18.dp)
    }
}
