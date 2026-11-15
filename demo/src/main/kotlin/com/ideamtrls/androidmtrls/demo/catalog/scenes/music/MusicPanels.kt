package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMArtPanelChrome
import com.ideamtrls.androidmtrls.AMArtRowsBackdrop
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMButtonPair
import com.ideamtrls.androidmtrls.AMButtonStyle
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMSectionHeader
import com.ideamtrls.androidmtrls.AMSurfaceFill
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.ProvideAMOnSurface
import com.ideamtrls.androidmtrls.amArtRows
import com.ideamtrls.androidmtrls.amArtText
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amFloatingActions
import com.ideamtrls.androidmtrls.amRowPadding

// The two panels the Artists stack pushes: an artist and an album, each under the art panel bar,
// whose fill shows once content scrolls under it, and each leaving the floating action button's
// clearance at the bottom. Routes come from the catalog, which never changes, so a page stays
// resolved while it pops.

/**
 * An artist: their photo as a card at its own shape, so the whole of it shows, over their name
 * and counts; Play and Shuffle over their songs; then their albums as art entries. The hero carries
 * the photo's color and each album its cover's (the project photo until the cover is read),
 * blending down the page as its backdrop's art, so the bar's frosted fill, Play and Shuffle, and
 * the Queue sheet blur them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicArtistPage(artistId: String, model: MusicModel) {
    val catalog = model.catalog
    val artist = catalog.artist(artistId) ?: return
    val spacing = AMTheme.tokens.spacing
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val albums = catalog.albums(artist)
    AMArtRowsBackdrop(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { AMArtPanelChrome(onBack = model::back, scrollBehavior = scrollBehavior) },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .amArtRows()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(WindowInsets.amFloatingActions)
                    .padding(top = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xl),
            ) {
                MusicArtistHero(artist, catalog, Modifier.musicArtRow(null, artist.imageUrl))
                MusicPlayShuffleButtons(MusicQueueTarget.Artist(artist), model, Modifier.padding(horizontal = spacing.lg))
                Column(Modifier.padding(horizontal = spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    AMSectionHeader("Albums")
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xl)) {
                        albums.forEach { album ->
                            val cover = model.cover(album.id)
                            MusicArtEntry(
                                art = album.coverUrl,
                                cover = cover,
                                aspectRatio = 1f,
                                title = album.title,
                                detail = musicSongsAndLength(catalog.tracks(album)),
                                isPlaying = model.currentAlbum?.id == album.id,
                                target = MusicQueueTarget.Album(album),
                                model = model,
                                onClick = { model.openAlbum(album.id) },
                                modifier = Modifier.musicArtRow(cover, album.coverUrl),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The artist's photo as a card in the gutter at the artist's aspect ratio, so its height follows
 * its width and the whole of it shows, `spacing.md` above the name (the page title's type, a
 * heading) over the counts, centered in art text. The photo is decorative: the name names it.
 */
@Composable
private fun MusicArtistHero(artist: MusicArtist, catalog: MusicCatalog, modifier: Modifier) {
    val spacing = AMTheme.tokens.spacing
    Column(
        modifier.fillMaxWidth().padding(horizontal = spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        MusicArtCard(artist.imageUrl, cover = null, Modifier.fillMaxWidth().aspectRatio(artist.imageAspectRatio))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                artist.name,
                Modifier.amArtText().semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                musicArtistSummary(artist, catalog),
                Modifier.amArtText(secondary = true),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * An album: its cover as an art card, Play and Shuffle, then its tracks in one frosted card, all
 * over its cover's art (the project photo's until the cover is read). The bar names the album over
 * "American Food · 3 songs · 15 min". While it's the stack's top, the Queue sheet's glass samples
 * its art.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicAlbumPage(albumId: String, model: MusicModel) {
    val catalog = model.catalog
    val album = catalog.album(albumId) ?: return
    val tracks = catalog.tracks(album)
    val spacing = AMTheme.tokens.spacing
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val cover = model.cover(album.id)
    AMBackdrop(art = { Box(Modifier.fillMaxSize().musicArtBackdrop(cover, album.coverUrl)) }, Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AMArtPanelChrome(
                    onBack = model::back,
                    title = album.title,
                    subtitle = "${album.artist} · ${musicSongsAndLength(tracks)}",
                    scrollBehavior = scrollBehavior,
                )
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(WindowInsets.amFloatingActions)
                    .padding(start = spacing.lg, end = spacing.lg, top = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                MusicArtCard(album.coverUrl, cover, Modifier.fillMaxWidth().aspectRatio(1f))
                MusicPlayShuffleButtons(MusicQueueTarget.Album(album), model)
                // Bare rows with inset dividers between them, in a frosted card that clips them,
                // so the playing row's highlight rounds with it.
                Column(Modifier.fillMaxWidth().amCard()) {
                    ProvideAMOnSurface {
                        tracks.forEachIndexed { index, track ->
                            if (index > 0) AMDivider(inset = spacing.lg)
                            MusicAlbumTrackRow(track, index, tracks, model)
                        }
                    }
                }
            }
        }
    }
}

/**
 * A track on the album's card: its number in a fixed slot, the title on one line, then the
 * length, on the row recipe. The playing row sits on the frosted highlight with the speaker mark
 * in the number's slot and its title in the now-playing tint, and says "Now playing" to TalkBack.
 * A tap plays the album from this track (the shuffle setting kept); press and hold opens the queue
 * menu on it.
 */
@Composable
private fun MusicAlbumTrackRow(track: MusicTrack, index: Int, album: List<MusicTrack>, model: MusicModel) {
    val spacing = AMTheme.tokens.spacing
    val typography = MaterialTheme.typography
    val isCurrent = model.isCurrent(track)
    val duration = musicTimeLabel(track.durationSeconds.toDouble())
    val description = "${index + 1}, ${track.title}, $duration"
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (isCurrent) Modifier.background(AMTheme.Frosted.highlight) else Modifier)
            .musicQueueMenu(MusicQueueTarget.Track(track, album), model, rememberMusicQueueIcons())
            .clickable(role = Role.Button) { model.play(album, index) }
            .clearAndSetSemantics {
                contentDescription = description
                if (isCurrent) stateDescription = "Now playing"
            }
            .amRowPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Box(Modifier.width(MusicTrackNumberWidth), contentAlignment = Alignment.Center) {
            if (isCurrent) {
                MusicSpeakerMark(tint = AMTheme.Frosted.tint)
            } else {
                Text(
                    "${index + 1}",
                    style = typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                    color = AMTheme.secondaryText,
                )
            }
        }
        Text(
            track.title,
            Modifier.weight(1f),
            style = typography.bodyLarge,
            color = if (isCurrent) AMTheme.Frosted.tint else Color.Unspecified,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            duration,
            style = typography.bodySmall.copy(fontFeatureSettings = "tnum"),
            color = AMTheme.secondaryText,
        )
    }
}

/** The track number's slot, so one- and two-digit numbers and the speaker mark line up. */
private val MusicTrackNumberWidth = 28.dp

/**
 * Play and Shuffle, frosted, in an [AMButtonPair]: Play plays [target] from its first song with
 * shuffle off, Shuffle with it on. Side by side at equal widths, or stacked when a label wouldn't
 * fit on one line at half the width.
 */
@Composable
private fun MusicPlayShuffleButtons(target: MusicQueueTarget, model: MusicModel, modifier: Modifier = Modifier) {
    val play = painterResource(R.drawable.ic_play_arrow_filled)
    val shuffle = painterResource(R.drawable.ic_shuffle)
    AMButtonPair(
        {
            AMButton(
                "Play",
                onClick = { model.play(target, shuffled = false) },
                icon = play,
                style = AMButtonStyle.Primary(fill = AMSurfaceFill.Frosted),
            )
        },
        {
            AMButton(
                "Shuffle",
                onClick = { model.play(target, shuffled = true) },
                icon = shuffle,
                style = AMButtonStyle.Secondary(fill = AMSurfaceFill.Frosted),
            )
        },
        modifier,
    )
}
