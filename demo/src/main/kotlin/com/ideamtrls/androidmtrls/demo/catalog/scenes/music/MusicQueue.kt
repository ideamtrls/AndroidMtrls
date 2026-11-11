package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMReorderableRowList
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMSheet
import com.ideamtrls.androidmtrls.AMSheetSurface
import com.ideamtrls.androidmtrls.AMSwipeAction
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

/**
 * The Queue sheet, frosted like the scene's cards (glass over the art of the screen it opens on),
 * titled "Queue". The scene composes it inside that screen's backdrop ([MusicModel.hostsQueue], or
 * the Player's).
 */
@Composable
internal fun MusicQueueSheet(model: MusicModel) {
    AMSheet(
        model.showingQueue,
        onDismissRequest = { model.showingQueue = false },
        title = "Queue",
        surface = AMSheetSurface.Frosted,
    ) {
        MusicQueueCard(model)
    }
}

/**
 * One solid card holding one list: the playing song first, then up next in play order, each row a
 * cover, the title and the album. The playing row sits above up next's reorderable list, outside
 * it, so it never drags or swipes; up next reorders by its handles or press and hold and drag, and a
 * swipe toward the start removes a row. The card hugs a short queue and a long one fills the sheet
 * and scrolls. With nothing up next the playing row stands alone. Edits don't toast: the list shows
 * the change.
 */
@Composable
private fun ColumnScope.MusicQueueCard(model: MusicModel) {
    val spacing = AMTheme.tokens.spacing
    val upNext = model.upNext
    val removeIcon = painterResource(R.drawable.ic_playlist_remove)
    Column(
        Modifier
            .weight(1f, fill = false)
            .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.lg)
            .fillMaxWidth()
            .amCard(),
    ) {
        model.currentTrack?.let { MusicNowPlayingRow(it, model) }
        if (upNext.isNotEmpty()) {
            AMReorderableRowList(
                upNext,
                key = { it.id },
                onMove = model::moveUpNext,
                modifier = Modifier.weight(1f, fill = false),
                endAction = { entry ->
                    AMSwipeAction("Remove", removeIcon, AMSemantic.Destructive, removesRow = true) {
                        model.removeFromQueue(entry.id)
                    }
                },
            ) { entry ->
                val track = entry.track
                AMListRow(
                    track.title,
                    subtitle = model.catalog.album(track.albumId)?.title,
                    leadingContent = { MusicThumb(track.coverUrl, model.cover(track.albumId)) },
                )
            }
        }
    }
}

/** The playing song: its title in the now-playing tint over its album, and the speaker mark trailing. */
@Composable
private fun MusicNowPlayingRow(track: MusicTrack, model: MusicModel) {
    val album = model.currentAlbum
    AMListRow(
        modifier = Modifier.semantics { stateDescription = "Now playing" },
        leadingContent = { MusicThumb(track.coverUrl, model.cover(track.albumId)) },
        supportingContent = if (album != null) {
            { Text(album.title) }
        } else {
            null
        },
        trailingContent = { MusicSpeakerMark() },
    ) {
        Text(track.title, color = MusicNowPlayingTint)
    }
}
