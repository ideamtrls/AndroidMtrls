package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
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
import com.ideamtrls.androidmtrls.AMButtonStyle
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMElevation
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMSectionHeader
import com.ideamtrls.androidmtrls.AMSurfaceFill
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amArtRow
import com.ideamtrls.androidmtrls.amArtRows
import com.ideamtrls.androidmtrls.amArtText
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amElevation
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
 * the Queue sheet blur them. The Queue sheet opens here while this page is on top.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicArtistPage(artistId: String, model: MusicModel) {
    val catalog = model.catalog
    val artist = catalog.artist(artistId) ?: return
    val route = MusicRoute.Artist(artistId)
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
                    .padding(top = spacing.md, bottom = MusicFabClearance),
                verticalArrangement = Arrangement.spacedBy(spacing.xl),
            ) {
                MusicArtistHero(artist, catalog, Modifier.amArtRow(artist.imageUrl))
                MusicPlayShuffleButtons(MusicQueueTarget.Artist(artist), model, Modifier.padding(horizontal = spacing.lg))
                Column(Modifier.padding(horizontal = spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    AMSectionHeader { Text("Albums", Modifier.amArtText()) }
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
        if (model.hostsQueue(route)) MusicQueueSheet(model)
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
    val shape = MaterialTheme.shapes.large
    Column(
        modifier.fillMaxWidth().padding(horizontal = spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AMRemoteImage(
            artist.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(artist.imageAspectRatio)
                .amElevation(AMElevation.Low, shape)
                .clip(shape),
            fallback = {},
        )
        Column(Modifier.fillMaxWidth().amArtText(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                artist.name,
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                musicArtistSummary(artist, catalog),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * An album: its cover as an art card, Play and Shuffle, then its tracks in one frosted card, all
 * over its cover's art (the project photo's until the cover is read). The bar names the album over
 * "American Food · 3 songs · 15 min". The Queue sheet opens inside this page's art while it's the
 * stack's top, so its glass samples it.
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
                    .padding(start = spacing.lg, end = spacing.lg, top = spacing.md, bottom = MusicFabClearance),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                MusicArtCard(album.coverUrl, cover, Modifier.fillMaxWidth().aspectRatio(1f))
                MusicPlayShuffleButtons(MusicQueueTarget.Album(album), model)
                // Bare rows with inset dividers between them, in a frosted card that clips them,
                // so the playing row's highlight rounds with it.
                Column(Modifier.fillMaxWidth().amCard(fill = AMSurfaceFill.Frosted)) {
                    tracks.forEachIndexed { index, track ->
                        if (index > 0) AMDivider(inset = spacing.lg)
                        MusicAlbumTrackRow(track, index, tracks, model)
                    }
                }
            }
        }
        if (model.hostsQueue(MusicRoute.Album(albumId))) MusicQueueSheet(model)
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
            .musicQueueMenu(MusicQueueTarget.Track(track, album), model)
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
                MusicSpeakerMark()
            } else {
                Text(
                    "${index + 1}",
                    style = typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                    color = AMTheme.Frosted.secondaryText,
                )
            }
        }
        Text(
            track.title,
            Modifier.weight(1f),
            style = typography.bodyLarge,
            color = if (isCurrent) MusicNowPlayingTint else Color.Unspecified,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            duration,
            style = typography.bodySmall.copy(fontFeatureSettings = "tnum"),
            color = AMTheme.Frosted.secondaryText,
        )
    }
}

/** The track number's slot, so one- and two-digit numbers and the speaker mark line up. */
private val MusicTrackNumberWidth = 28.dp

/**
 * Play and Shuffle, frosted, side by side at equal widths: Play plays [target] from its first song
 * with shuffle off, Shuffle with it on. At the largest text sizes, where a half-width "Shuffle"
 * would break mid-word, Shuffle stacks under Play.
 */
@Composable
private fun MusicPlayShuffleButtons(target: MusicQueueTarget, model: MusicModel, modifier: Modifier = Modifier) {
    val spacing = AMTheme.tokens.spacing
    val play = painterResource(R.drawable.ic_play_arrow_filled)
    val shuffle = painterResource(R.drawable.ic_shuffle)
    val playButton = @Composable { buttonModifier: Modifier ->
        AMButton(
            "Play",
            onClick = { model.play(target, shuffled = false) },
            modifier = buttonModifier,
            icon = play,
            style = AMButtonStyle.Primary(fill = AMSurfaceFill.Frosted),
        )
    }
    val shuffleButton = @Composable { buttonModifier: Modifier ->
        AMButton(
            "Shuffle",
            onClick = { model.play(target, shuffled = true) },
            modifier = buttonModifier,
            icon = shuffle,
            style = AMButtonStyle.Secondary(fill = AMSurfaceFill.Frosted),
        )
    }
    if (LocalDensity.current.fontScale >= MusicStackedButtonsFontScale) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            playButton(Modifier.fillMaxWidth())
            shuffleButton(Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            playButton(Modifier.weight(1f))
            shuffleButton(Modifier.weight(1f))
        }
    }
}

/** The font scale from which Play and Shuffle stack. */
private const val MusicStackedButtonsFontScale = 1.5f
