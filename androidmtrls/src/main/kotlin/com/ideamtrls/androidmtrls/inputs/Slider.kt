package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * What flanks one end of an [AMSlider]'s track: a glyph or a short readout. One value per end, so
 * an end can't carry both.
 */
@Immutable
public sealed interface AMSliderDecoration {

    /**
     * A glyph (a quiet speaker at the low end, a loud one at the high end), in the
     * `onSurfaceVariant` color at Material's 24 dp.
     */
    @Immutable
    public class Icon(
        /** The glyph. */
        public val icon: Painter,
    ) : AMSliderDecoration

    /**
     * A short readout ("1:02", "Off", "12 s"), in `bodySmall` with tabular figures, in the
     * `onSurfaceVariant` color.
     */
    @Immutable
    public data class Text(
        /** The readout. */
        public val text: String,
    ) : AMSliderDecoration
}

/**
 * A row slider: Material's `Slider` on the row recipe, with an optional title above the track and
 * an optional glyph or readout at each end.
 *
 * ```kotlin
 * AMSlider(
 *     volume,
 *     onValueChange = { volume = it },
 *     title = "Volume",
 *     leading = AMSliderDecoration.Icon(painterResource(R.drawable.ic_volume_mute)),
 *     trailing = AMSliderDecoration.Icon(painterResource(R.drawable.ic_volume_up)),
 * )
 * ```
 *
 * - **A row, not a surface.** It follows the row recipe (28 dp above and below, 16 dp at the sides)
 *   and draws no fill: stack sliders in a card with `AMDivider(inset = spacing.lg)` between them.
 *   The [title] sits above the track in the row title's type (`bodyLarge`), `spacing.sm` over it;
 *   the ends sit beside the track, `spacing.sm` from it.
 * - **Material's slider** in its own shape and colors, which takes taps and drags and keeps its
 *   48 dp target.
 * - **TalkBack reads the row as one element, the slider**: named [contentDescription], or [title]
 *   when that's null, and valued as a percentage, or [stateDescription] when it's set (a media
 *   scrubber's "1:02 of 3:43"). The visible title and the ends are hidden from it, since the slider
 *   speaks for them; a slider with no visible title needs a [contentDescription].
 *
 * Disabled (its own [enabled], or a disabling container around it), the slider, title and ends take
 * Material's disabled colors and the slider ignores touches.
 *
 * @param value the current value, within [valueRange].
 * @param onValueChange called with each new value while the slider is dragged or tapped.
 * @param modifier the row's placement.
 * @param title the row's title, above the track. Null shows none.
 * @param leading what sits before the track. Null shows nothing.
 * @param trailing what sits after the track. Null shows nothing.
 * @param contentDescription the slider's name for TalkBack, in place of [title]. Null uses [title].
 * @param stateDescription what TalkBack says for the value, in place of the percentage. Null says
 *   the percentage.
 * @param valueRange the values the slider spans.
 * @param onValueChangeFinished called when a change ends (the finger lifts after a drag or a tap),
 *   once the final value has come through [onValueChange]: Material's own callback, for work that
 *   should wait for that value (seeking a stream). Null for none.
 * @param enabled whether the slider takes touches.
 */
@Composable
public fun AMSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    leading: AMSliderDecoration? = null,
    trailing: AMSliderDecoration? = null,
    contentDescription: String? = null,
    stateDescription: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val isEnabled = resolveEnabled(enabled)
    val spacing = AMTheme.tokens.spacing
    val colors = MaterialTheme.colorScheme
    val disabled = colors.onSurface.copy(alpha = InputDisabledAlpha)
    val name = contentDescription ?: title
    // Material's slider reports moves through onValueChange, and the end of a change through
    // onValueChangeFinished, and draws its state's value, which follows the caller's value.
    val sliderState = remember(valueRange) { SliderState(value, steps = 0, valueRange) }
    SideEffect { sliderState.value = value }
    Column(modifier.amRowPadding(), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        if (title != null) {
            Text(
                title,
                Modifier.clearAndSetSemantics {},
                color = if (isEnabled) colors.onSurface else disabled,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            if (leading != null) SliderEnd(leading, isEnabled)
            Slider(
                state = sliderState,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        if (name != null) this.contentDescription = name
                        if (stateDescription != null) this.stateDescription = stateDescription
                    },
                enabled = isEnabled,
                onValueChangeFinished = onValueChangeFinished,
            )
            if (trailing != null) SliderEnd(trailing, isEnabled)
        }
    }
}

/** One end of the track: the glyph or readout, hidden from TalkBack. */
@Composable
private fun SliderEnd(decoration: AMSliderDecoration, enabled: Boolean) {
    val colors = MaterialTheme.colorScheme
    val color = if (enabled) colors.onSurfaceVariant else colors.onSurface.copy(alpha = InputDisabledAlpha)
    val hidden = Modifier.clearAndSetSemantics {}
    when (decoration) {
        is AMSliderDecoration.Icon -> Icon(decoration.icon, contentDescription = null, modifier = hidden, tint = color)
        is AMSliderDecoration.Text -> Text(
            decoration.text,
            hidden,
            color = color,
            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
            maxLines = 1,
        )
    }
}
