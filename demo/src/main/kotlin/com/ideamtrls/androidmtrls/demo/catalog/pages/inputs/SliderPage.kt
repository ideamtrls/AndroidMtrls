package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMSlider
import com.ideamtrls.androidmtrls.AMSliderDecoration
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import kotlin.math.roundToInt

internal val SliderEntry = ComponentEntry(
    id = "slider",
    category = CatalogCategory.Inputs,
    title = "Slider",
    summary = "Row slider with glyph or readout ends",
    icon = R.drawable.ic_tune,
    keywords = listOf(
        "AMSlider", "AMSliderDecoration", "Slider", "range", "volume", "brightness", "scrubber", "seek",
    ),
) { SliderPage() }

@Composable
private fun SliderPage() {
    ComponentPage(
        SliderEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { VolumeSlider() },
    ) {
        PlaybackControls()
    }
}

/** A track's length, in seconds. */
private const val TrackSeconds = 223

/** A labeled volume slider between a quiet and a loud speaker, on a surface strip. */
@Composable
private fun VolumeSlider() {
    var volume by rememberSaveable { mutableFloatStateOf(0.6f) }
    AMSlider(
        volume,
        onValueChange = { volume = it },
        modifier = Modifier.background(AMTheme.Surface.primary),
        title = "Volume",
        leading = AMSliderDecoration.Icon(painterResource(R.drawable.ic_volume_mute)),
        trailing = AMSliderDecoration.Icon(painterResource(R.drawable.ic_volume_up)),
    )
}

/** A player's controls: the track position between its times, the volume, and the crossfade. */
@Composable
private fun PlaybackControls() {
    var position by rememberSaveable { mutableFloatStateOf(62f) }
    var volume by rememberSaveable { mutableFloatStateOf(0.6f) }
    var crossfade by rememberSaveable { mutableFloatStateOf(4f) }
    val elapsed = position.roundToInt()
    val inset = AMTheme.tokens.spacing.lg
    Column(Modifier.fillMaxWidth().amCard()) {
        AMSlider(
            position,
            onValueChange = { position = it },
            leading = AMSliderDecoration.Text(clock(elapsed)),
            trailing = AMSliderDecoration.Text("−" + clock(TrackSeconds - elapsed)),
            contentDescription = "Track position",
            stateDescription = "${clock(elapsed)} of ${clock(TrackSeconds)}",
            valueRange = 0f..TrackSeconds.toFloat(),
        )
        AMDivider(inset = inset)
        AMSlider(
            volume,
            onValueChange = { volume = it },
            leading = AMSliderDecoration.Icon(painterResource(R.drawable.ic_volume_mute)),
            trailing = AMSliderDecoration.Icon(painterResource(R.drawable.ic_volume_up)),
            contentDescription = "Volume",
        )
        AMDivider(inset = inset)
        AMSlider(
            crossfade,
            onValueChange = { crossfade = it },
            title = "Crossfade",
            leading = AMSliderDecoration.Text("Off"),
            trailing = AMSliderDecoration.Text("12 s"),
            stateDescription = "${crossfade.roundToInt()} seconds",
            valueRange = 0f..12f,
        )
    }
}

/** Seconds as minutes and seconds ("1:02"). */
private fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
