package com.ideamtrls.androidmtrls.demo

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.text.style.TextOverflow
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow
import com.ideamtrls.androidmtrls.AMAvatarSize
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.amHoldable

// The press-and-hold menu: the hold itself, and the two halves the scene brings into the
// spotlight — the held artist, album or song up close, and its tiles. A song gets a 2-up
// "Play, Add to queue"; an album or an artist a 3-up "Play, Shuffle, Queue", one word a tile, with
// Queue read as "Add to queue".

/**
 * Press and hold (the library's hold, with its haptic) opens the queue menu on [target] and
 * cancels the tap underneath; a vertical drag that starts on the item still scrolls. TalkBack gets
 * the menu's actions on the item itself: Play, Shuffle and Add to queue for artists and albums, Add
 * to queue for songs (a song's tap already plays it).
 */
internal fun Modifier.musicQueueMenu(target: MusicQueueTarget, model: MusicModel): Modifier {
    val actions = buildList {
        if (target !is MusicQueueTarget.Track) {
            add(CustomAccessibilityAction("Play") { model.play(target); true })
            add(CustomAccessibilityAction("Shuffle") { model.play(target, shuffled = true); true })
        }
        add(CustomAccessibilityAction("Add to queue") { model.addToQueue(target); true })
    }
    return amHoldable(onHold = { model.queueMenuTarget = target }, customActions = actions)
}

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

/** The held target's tiles; each closes the spotlight, then acts. */
@Composable
internal fun MusicQueueTiles(target: MusicQueueTarget, model: MusicModel) {
    val play = painterResource(R.drawable.ic_play_arrow_filled)
    val addToQueue = painterResource(R.drawable.ic_playlist_add)
    when (target) {
        is MusicQueueTarget.Track -> AMActionGridRow(
            { AMActionGridButton(play, "Play", onClick = { model.play(target) }) },
            { AMActionGridButton(addToQueue, "Add to queue", onClick = { model.addToQueue(target) }) },
        )
        else -> AMActionGridRow(
            { AMActionGridButton(play, "Play", onClick = { model.play(target) }) },
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_shuffle), "Shuffle",
                    onClick = { model.play(target, shuffled = true) },
                )
            },
            {
                AMActionGridButton(
                    addToQueue, "Queue",
                    onClick = { model.addToQueue(target) },
                    contentDescription = "Add to queue",
                )
            },
        )
    }
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
