package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMArtCard
import com.ideamtrls.androidmtrls.AMArtThumbnail
import com.ideamtrls.androidmtrls.AMAvatarSize
import com.ideamtrls.androidmtrls.AMElevation
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amArtBackdrop
import com.ideamtrls.androidmtrls.amArtRow
import com.ideamtrls.androidmtrls.amElevation

// The Music scene's artwork pieces: the backdrop fallback, the art and art rows from a cover or a
// URL, the art card, the cover thumbnail, and the now-playing marks — the
// tint, the speaker mark and the badge that carries it on an art card.


/**
 * An art backdrop from an album's art: its embedded [cover] once it's read, else the picture at
 * [url] (the project photo). The cover arriving fades in over the photo.
 */
internal fun Modifier.musicArtBackdrop(cover: ImageBitmap?, url: String?): Modifier {
    val art = MusicCoverOverride ?: cover
    return if (art != null) amArtBackdrop(art) else amArtBackdrop(url)
}

/** An art row's color from an album's art: its embedded [cover] once it's read, else the picture at [url]. */
internal fun Modifier.musicArtRow(cover: ImageBitmap?, url: String?): Modifier {
    val art = MusicCoverOverride ?: cover
    return if (art != null) amArtRow(art) else amArtRow(url)
}

/**
 * The solid picture the debug `cover` launch extra puts in place of every cover and photo in the
 * scene, art and cards alike (`--es cover white|black`), or null.
 */
private val MusicCoverOverride: ImageBitmap?
    get() = when (LaunchOptions.cover) {
        "white" -> SolidArt.White
        "black" -> SolidArt.Black
        else -> null
    }

/**
 * Album or artist art as a card ([AMArtCard]) in [modifier]'s frame (callers give it a width and
 * a shape: an album's square, an artist's photo at its own aspect ratio, so the whole photo shows):
 * an album's [cover], the art embedded in its MP3s, once the model has read it; until then, or when
 * it can't be read, the picture at [url] (the project photo). With an [interactionSource], the
 * ripple of the entry it belongs to shows on the card. Decorative: what's beside it names it.
 */
@Composable
internal fun MusicArtCard(
    url: String?,
    cover: ImageBitmap?,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) {
    val art = MusicCoverOverride ?: cover
    if (art != null) {
        AMArtCard(art, contentDescription = null, modifier, interactionSource)
    } else {
        AMArtCard(url, contentDescription = null, modifier, interactionSource)
    }
}

/**
 * A cover thumbnail ([AMArtThumbnail]), [size] square: the embedded [cover] once it's read, else
 * the picture at [url], with the music note while there's none. Decorative: the row beside it names
 * the song.
 */
@Composable
internal fun MusicThumb(url: String?, cover: ImageBitmap?, size: Dp = AMAvatarSize.Medium.diameter) {
    val art = MusicCoverOverride ?: cover
    if (art != null) {
        AMArtThumbnail(art, contentDescription = null, size = size)
    } else {
        AMArtThumbnail(url, contentDescription = null, icon = painterResource(R.drawable.ic_music_note), size = size)
    }
}

/** The now-playing tint: the playing song's title and the speaker mark, in the scheme's accent. */
internal val MusicNowPlayingTint: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary

/** The now-playing mark: the filled speaker at [size], in [tint] (the now-playing tint). Decorative. */
@Composable
internal fun MusicSpeakerMark(size: Dp = 24.dp, tint: Color = MusicNowPlayingTint) {
    Icon(
        painterResource(R.drawable.ic_volume_up_filled),
        contentDescription = null,
        modifier = Modifier.size(size),
        tint = tint,
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
