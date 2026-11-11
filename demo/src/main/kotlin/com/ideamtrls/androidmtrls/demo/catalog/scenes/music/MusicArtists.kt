package com.ideamtrls.androidmtrls.demo

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import com.ideamtrls.androidmtrls.AMArtRowsBackdrop
import com.ideamtrls.androidmtrls.AMEmptyState
import com.ideamtrls.androidmtrls.AMLightStatusBarIcons
import com.ideamtrls.androidmtrls.AMPageHeader
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amArtRow
import com.ideamtrls.androidmtrls.amArtRows
import com.ideamtrls.androidmtrls.amArtText
import com.ideamtrls.androidmtrls.amCrossfade

// The Artists root, and the art entry it shares with the artist page: an art card over its name,
// and an album's counts, in white art text. On both pages each entry carries its own color (an art
// row), which scrolls with it and blends into its neighbors', and which is the page's backdrop's
// art, so its frosted surfaces blur it (AMArtRowsBackdrop).

/**
 * The Artists root, the scene's landing view: the "Artists" header with the light/dark chip at its
 * end, over one art entry per artist, each on its photo's art row. A tap opens the artist; press
 * and hold opens the queue menu. While the header lies on the art its title is white art text and
 * the status bar's icons are light; as it collapses it fades in the frosted fill over the rows
 * under it, and once that's in far enough for the bar's own title color to read, the title and the
 * icons turn to the appearance's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicArtistsView(model: MusicModel) {
    val spacing = AMTheme.tokens.spacing
    val catalog = model.catalog
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val overArt by remember(scrollBehavior) {
        derivedStateOf { FastOutLinearInEasing.transform(scrollBehavior.state.collapsedFraction) < MusicHeaderFrostedEnough }
    }
    AMLightStatusBarIcons(enabled = overArt)
    AMArtRowsBackdrop(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AMPageHeader(actions = { SceneAppearanceButton() }, scrollBehavior = scrollBehavior) {
                    Text("Artists", Modifier.amCrossfade(overArt).then(if (overArt) Modifier.amArtText() else Modifier))
                }
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .amArtRows()
                    .padding(padding)
                    .padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = MusicFabClearance),
                verticalArrangement = Arrangement.spacedBy(spacing.xl),
            ) {
                if (catalog.tracks.isEmpty()) {
                    AMEmptyState(
                        painterResource(R.drawable.ic_music_note),
                        "No music",
                        description = "Songs you add to your library show up here.",
                    )
                } else {
                    catalog.artists.forEach { artist ->
                        MusicArtEntry(
                            art = artist.imageUrl,
                            cover = null,
                            aspectRatio = artist.imageAspectRatio,
                            title = artist.name,
                            detail = null,
                            isPlaying = model.currentArtist?.id == artist.id,
                            target = MusicQueueTarget.Artist(artist),
                            model = model,
                            onClick = { model.openArtist(artist.id) },
                            modifier = Modifier.amArtRow(artist.imageUrl),
                        )
                    }
                }
            }
        }
        if (model.hostsQueue(MusicRoute.Artists)) MusicQueueSheet(model)
    }
}

/**
 * How far in the header's frosted fill comes (it follows the collapse as Material eases a large
 * bar's color, fast out and linear in) before the bar's own title reads on it: from 0.9, the
 * title has 4.5:1 even where the rows under it are black. The title stays white art text until
 * then, and the status bar's icons light.
 */
private const val MusicHeaderFrostedEnough = 0.9f

/**
 * One artist or album: the art card — an artist's photo at its own [aspectRatio], an album's square
 * cover (its embedded [cover] once it's read) — carrying the now-playing badge while it's the
 * playing entry, over its name and any [detail] in art text, inset `spacing.lg`. A tap anywhere on
 * it runs [onClick], rippling the card; press and hold opens the queue menu on [target]. TalkBack
 * reads one button: the name, any detail, and "Now playing" while it plays.
 */
@Composable
internal fun MusicArtEntry(
    art: String?,
    cover: ImageBitmap?,
    aspectRatio: Float,
    title: String,
    detail: String?,
    isPlaying: Boolean,
    target: MusicQueueTarget,
    model: MusicModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AMTheme.tokens.spacing
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .musicQueueMenu(target, model)
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .semantics { if (isPlaying) stateDescription = "Now playing" },
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Box {
            MusicArtCard(art, cover, Modifier.fillMaxWidth().aspectRatio(aspectRatio), interactionSource)
            if (isPlaying) MusicNowPlayingBadge(Modifier.align(Alignment.TopEnd).padding(spacing.md))
        }
        Column(Modifier.padding(horizontal = spacing.lg).amArtText()) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            }
        }
    }
}
