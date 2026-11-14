package com.ideamtrls.androidmtrls.demo

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.ideamtrls.androidmtrls.AMAvatarSize
import com.ideamtrls.androidmtrls.AMHoldAction
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.amHoldable

// The press-and-hold menu: the hold itself, and the two halves the scene brings into the
// spotlight — the held artist, album or song up close, and its tiles. A song gets a 2-up
// "Play, Add to queue"; an album or an artist a 3-up "Play, Shuffle, Queue", one word a tile, with
// Queue read as "Add to queue".

/**
 * Press and hold (the library's hold, with its haptic) opens the queue menu on [target] and
 * cancels the tap underneath; a vertical drag that starts on the item still scrolls. TalkBack gets
 * the menu's tiles ([musicQueueActions]) as actions on the item itself.
 */
internal fun Modifier.musicQueueMenu(target: MusicQueueTarget, model: MusicModel, icons: MusicQueueIcons): Modifier =
    amHoldable(onHold = { model.queueMenuTarget = target }, actions = musicQueueActions(target, model, icons))

/**
 * The held target up close: its art a size up from the rows' thumbnails, then its name over one
 * line of what it holds — "Internet On The TV · 2:09" for a song, "American Food · 3 songs" for an
 * album, "3 albums · 9 songs" with the artist's photo for an artist.
 */
@Composable
internal fun MusicQueuePreview(target: MusicQueueTarget, model: MusicModel) {
    val summary = musicQueueSummary(target, model)
    AMListRow(
        leadingContent = { MusicThumb(summary.art, summary.cover, AMAvatarSize.Large.diameter) },
        supportingContent = { Text(summary.detail, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    ) {
        Text(summary.title)
    }
}

/** The queue menu's glyphs, read once for its tiles. */
@Immutable
internal class MusicQueueIcons(val play: Painter, val shuffle: Painter, val addToQueue: Painter)

/** The queue menu's glyphs. */
@Composable
internal fun rememberMusicQueueIcons(): MusicQueueIcons = MusicQueueIcons(
    play = painterResource(R.drawable.ic_play_arrow_filled),
    shuffle = painterResource(R.drawable.ic_shuffle),
    addToQueue = painterResource(R.drawable.ic_playlist_add),
)

/**
 * What the held target offers: Play and Add to queue for a song; Play, Shuffle and Queue (read as
 * "Add to queue") for an artist or album. A tile closes the spotlight, then acts.
 */
internal fun musicQueueActions(target: MusicQueueTarget, model: MusicModel, icons: MusicQueueIcons): List<AMHoldAction> = when (target) {
    is MusicQueueTarget.Track -> listOf(
        AMHoldAction("Play", icons.play) { model.play(target) },
        AMHoldAction("Add to queue", icons.addToQueue) { model.addToQueue(target) },
    )
    else -> listOf(
        AMHoldAction("Play", icons.play) { model.play(target) },
        AMHoldAction("Shuffle", icons.shuffle) { model.play(target, shuffled = true) },
        AMHoldAction("Queue", icons.addToQueue, contentDescription = "Add to queue") { model.addToQueue(target) },
    )
}

/** What the preview shows: its art (with a song's or album's embedded cover), its name and its line. */
private class MusicQueueSummary(val art: String?, val cover: ImageBitmap?, val title: String, val detail: String)

private fun musicQueueSummary(target: MusicQueueTarget, model: MusicModel): MusicQueueSummary = when (target) {
    is MusicQueueTarget.Track -> {
        val track = target.track
        val detail = listOfNotNull(
            model.catalog.album(track.albumId)?.title,
            musicTimeLabel(track.durationSeconds.toDouble()),
        ).joinToString(" · ")
        MusicQueueSummary(track.coverUrl, model.cover(track.albumId), track.title, detail)
    }
    is MusicQueueTarget.Album -> {
        val album = target.album
        MusicQueueSummary(
            album.coverUrl, model.cover(album.id), album.title,
            "${album.artist} · ${musicCount(album.trackIds.size, "song")}",
        )
    }
    is MusicQueueTarget.Artist -> {
        val artist = target.artist
        MusicQueueSummary(artist.imageUrl, null, artist.name, musicArtistSummary(artist, model.catalog))
    }
}
