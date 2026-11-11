package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.ideamtrls.androidmtrls.AMIconButton
import com.ideamtrls.androidmtrls.AMIconButtonSize
import com.ideamtrls.androidmtrls.AMSlider
import com.ideamtrls.androidmtrls.AMSliderDecoration
import com.ideamtrls.androidmtrls.AMSurfaceFill
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import kotlin.math.max

/**
 * The playback controls on a frosted card: the song's title over "Artist · Album", the scrubber,
 * then one row of Shuffle · Previous · Play/Pause · Next · Repeat. The Player places it with
 * [musicControlsPlacement], and the remote shows the same card as the first row of its grid; both
 * put its bottom edge in the same place, so opening the remote on the Player leaves the controls
 * where they were. In the remote's window there's no art to frost over, so there it's solid.
 *
 * The scrubber is a slider row that brings its own gutter and the row recipe's room above and
 * below it; the title takes `spacing.lg` at its sides, and the transport row is centered. With
 * nothing in the queue there's no card.
 */
@Composable
internal fun MusicControlsCard(model: MusicModel, modifier: Modifier = Modifier) {
    val track = model.currentTrack ?: return
    val spacing = AMTheme.tokens.spacing
    Column(
        modifier
            .fillMaxWidth()
            .amCard(fill = AMSurfaceFill.Frosted, padding = PaddingValues(top = spacing.lg, bottom = spacing.sm)),
    ) {
        MusicControlsHeading(track, model)
        MusicScrubber(track, model)
        MusicTransport(model)
    }
}

/** The title over "Artist · Album", one line each; TalkBack reads them as one element. */
@Composable
private fun MusicControlsHeading(track: MusicTrack, model: MusicModel) {
    val album = model.currentAlbum
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = AMTheme.tokens.spacing.lg)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(track.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (album != null) "${track.artist} · ${album.title}" else track.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = AMTheme.Frosted.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The elapsed and the remaining time flank the track; TalkBack reads it as one slider, "Track
 * position, 1:02 of 3:43". Dragging moves the clock; letting go seeks the stream there.
 */
@Composable
private fun MusicScrubber(track: MusicTrack, model: MusicModel) {
    val duration = track.durationSeconds.toDouble()
    val elapsed = musicTimeLabel(model.currentTime)
    AMSlider(
        value = model.currentTime.toFloat(),
        onValueChange = { model.scrub(it.toDouble()) },
        leading = AMSliderDecoration.Text(elapsed),
        trailing = AMSliderDecoration.Text("−" + musicTimeLabel(max(0.0, duration - model.currentTime))),
        contentDescription = "Track position",
        stateDescription = "$elapsed of ${musicTimeLabel(duration)}",
        valueRange = 0f..max(duration, 1.0).toFloat(),
        onValueChangeFinished = model::seekToClock,
    )
}

/**
 * Shuffle · Previous · Play/Pause · Next · Repeat, centered, `spacing.lg` apart; Play/Pause is the
 * large one. Previous and Next grey out at the queue's ends unless Repeat all wraps. Shuffle and
 * Repeat are toggles: on in Material's checked color with the "on" glyphs, their state read as
 * on or off; Repeat steps off, all, one, and names the one mode ("Repeat one").
 */
@Composable
private fun MusicTransport(model: MusicModel) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.lg, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AMIconButton(
            painterResource(if (model.isShuffling) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle),
            contentDescription = "Shuffle",
            checked = model.isShuffling,
            onCheckedChange = { model.toggleShuffle() },
        )
        AMIconButton(
            painterResource(R.drawable.ic_skip_previous_filled),
            contentDescription = "Previous",
            onClick = model::previous,
            enabled = model.canGoBack,
        )
        AMIconButton(
            painterResource(if (model.isPlaying) R.drawable.ic_pause_filled else R.drawable.ic_play_arrow_filled),
            contentDescription = if (model.isPlaying) "Pause" else "Play",
            onClick = model::togglePlay,
            size = AMIconButtonSize.Large,
        )
        AMIconButton(
            painterResource(R.drawable.ic_skip_next_filled),
            contentDescription = "Next",
            onClick = model::next,
            enabled = model.canGoForward,
        )
        AMIconButton(
            painterResource(
                when (model.repeatMode) {
                    MusicRepeat.Off -> R.drawable.ic_repeat
                    MusicRepeat.All -> R.drawable.ic_repeat_on
                    MusicRepeat.One -> R.drawable.ic_repeat_one_on
                },
            ),
            contentDescription = if (model.repeatMode == MusicRepeat.One) "Repeat one" else "Repeat",
            checked = model.repeatMode != MusicRepeat.Off,
            onCheckedChange = { model.cycleRepeat() },
        )
    }
}

/**
 * Places the controls card where the remote shows it: `spacing.lg` from the sides, and its bottom
 * edge where the remote's grid puts the bottom of the row above its one tile row — clear of the
 * navigation bar, then `spacing.xxl`, a tile and `spacing.md`. The tile's height is fixed, so the
 * two edges stay together at every text size (the card grows upward).
 */
@Composable
@ReadOnlyComposable
internal fun Modifier.musicControlsPlacement(): Modifier {
    val spacing = AMTheme.tokens.spacing
    return this
        .navigationBarsPadding()
        .padding(
            start = spacing.lg,
            end = spacing.lg,
            bottom = spacing.xxl + AMTheme.ActionGrid.tileHeight + spacing.md,
        )
}
