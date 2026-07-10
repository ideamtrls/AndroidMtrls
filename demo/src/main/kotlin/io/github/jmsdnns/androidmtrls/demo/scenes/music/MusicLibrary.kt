package io.github.jmsdnns.androidmtrls.demo.scenes.music

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMAvatar
import io.github.jmsdnns.androidmtrls.content.AMAvatarSize
import io.github.jmsdnns.androidmtrls.content.AMPageHeader
import io.github.jmsdnns.androidmtrls.demo.scenes.common.sceneFabClearance
import io.github.jmsdnns.androidmtrls.lists.AMListRow
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.amRipple

// ---------------------------------------------------------------------------
// Shared row pieces
// ---------------------------------------------------------------------------

/** The leading cover thumbnail recipe shared by album and track rows. */
@Composable
private fun MusicRowThumb(coverArt: ImageBitmap?, albumId: String) {
    MusicCoverArt(
        coverArt = coverArt,
        palette = musicPalette(albumId),
        modifier = Modifier.size(musicCoverThumbSize),
        cornerRadius = AMTheme.Radius.sm.dp,
        glyphSize = 18.dp,
    )
}

// ---------------------------------------------------------------------------
// MusicLibraryView — the library root
// ---------------------------------------------------------------------------

/**
 * Full-screen library view: an [AMPageHeader] over the single artist row (the
 * catalog's sole artist = the whole library). Tap opens the artist's albums
 * panel; long-press opens the `[Play, Add to Queue]` menu. A "normal" page
 * surface — the gradient belongs to the player. Ports iOS `MusicLibraryView`.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicLibraryView(state: MusicState) {
    val isDark = LocalAMIsDarkTheme.current
    val catalog = state.catalog
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AMTheme.Surface.page.resolve(isDark))
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = sceneFabClearance()),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = AMTheme.Spacing.lg.dp)
                .padding(top = AMTheme.Spacing.lg.dp),
        ) {
            AMPageHeader("Library")
        }

        Spacer(modifier = Modifier.height(AMTheme.Spacing.md.dp))

        AMListRow(
            title = catalog.artistName,
            subtitle = "${catalog.tracks.size} songs · ${catalog.albums.size} albums",
            showsChevron = true,
            leading = { AMAvatar(url = null, initials = catalog.artistName, size = AMAvatarSize.SM) },
            modifier = Modifier.combinedClickable(
                interactionSource = interaction,
                indication = amRipple(),
                onClick = { state.openArtist() },
                onLongClick = { state.queueMenuTarget = MusicQueueTarget.Artist },
            ),
        )
    }
}

// ---------------------------------------------------------------------------
// MusicArtistPanel — the artist's albums (panel body)
// ---------------------------------------------------------------------------

/**
 * Artist panel body: the artist's albums as rows with gradient thumbs. Tap opens
 * an album's track list; long-press opens the `[Play, Add to Queue]` menu. Ports
 * iOS `MusicArtistPanelContent`.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicArtistPanel(state: MusicState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = sceneFabClearance()),
    ) {
        state.catalog.albums.forEach { album ->
            val interaction = remember(album.id) { MutableInteractionSource() }
            AMListRow(
                title = album.title,
                subtitle = "${album.trackIds.size} songs",
                showsChevron = true,
                leading = { MusicRowThumb(album.coverArt, album.id) },
                modifier = Modifier.combinedClickable(
                    interactionSource = interaction,
                    indication = amRipple(),
                    onClick = { state.openAlbum(album.id) },
                    onLongClick = { state.queueMenuTarget = MusicQueueTarget.Album(album) },
                ),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// MusicAlbumPanel — the album's track list (panel body)
// ---------------------------------------------------------------------------

/**
 * Album panel body: the album's tracks. The now-playing track carries the
 * selection fill (`isSelected`). Tap plays the track from the album; long-press
 * opens the `[Play, Add to Queue]` menu. Ports iOS `MusicAlbumPanelContent`.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicAlbumPanel(state: MusicState, album: MusicAlbum) {
    val tracks = state.catalog.tracks(album)
    val nowPlayingId = state.currentTrack?.id

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = sceneFabClearance()),
    ) {
        tracks.forEach { track ->
            val interaction = remember(track.id) { MutableInteractionSource() }
            AMListRow(
                title = track.title,
                subtitle = track.artist,
                isSelected = track.id == nowPlayingId,
                leading = { MusicRowThumb(track.coverArt, track.albumId) },
                trailing = { MusicDurationLabel(track.durationSeconds) },
                modifier = Modifier.combinedClickable(
                    interactionSource = interaction,
                    indication = amRipple(),
                    onClick = {
                        state.play(tracks, tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0))
                    },
                    onLongClick = { state.queueMenuTarget = MusicQueueTarget.Track(track) },
                ),
            )
        }
    }
}
