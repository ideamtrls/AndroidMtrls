package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign

/**
 * How an [AMProgressView] shows its progress. Sealed, so only these two exist:
 *
 * - [Spinner]: Material's circular indicator over a caption, centered, framed in a card by
 *   default.
 * - [Bar]: Material's linear indicator under a title and a trailing value, on the row recipe.
 */
@Immutable
public sealed interface AMProgressViewStyle {

    /**
     * Material's circular progress indicator, centered over the label as a caption (`bodySmall`
     * in `onSurfaceVariant`), `spacing.md` apart: for work that holds up a view or a block.
     *
     * @property card whether the spinner floats in its own full-width card (`spacing.xl` at the
     *   sides, `spacing.lg` above and below), the way a view that's loading shows it on the page.
     *   Pass false to place the spinner inline, with no card, inside a card or on a strip of your
     *   own. A carded spinner inside a card lies flat by itself, as every nested card does.
     */
    @Immutable
    public class Spinner(public val card: Boolean = true) : AMProgressViewStyle {
        override fun equals(other: Any?): Boolean = other is Spinner && other.card == card

        override fun hashCode(): Int = card.hashCode()

        override fun toString(): String = "Spinner(card=$card)"
    }

    /**
     * Material's linear progress indicator, full width, under a row of the label (`bodyLarge` in
     * `onSurface`) and the value label at its end (`bodyMedium` in `onSurfaceVariant`),
     * `spacing.sm` apart, on the row recipe. Like a flat row, it paints the floating surface when it
     * stands on the page and nothing inside a card, so it stacks with other rows in one, divided
     * by [AMDivider].
     */
    @Immutable
    public data object Bar : AMProgressViewStyle
}

/**
 * Progress on Android's own indicators, labeled: Material's circular indicator over a caption
 * ([AMProgressViewStyle.Spinner]), or Material's linear indicator under a title and its value
 * ([AMProgressViewStyle.Bar]).
 *
 * ```kotlin
 * AMProgressView("Checking for new mail")                                     // an indeterminate spinner, in a card
 * AMProgressView("Uploading", progress = { uploaded }, valueLabel = "60%", style = AMProgressViewStyle.Bar)
 * ```
 *
 * The indicators keep Material's look, colors and motion: a [progress] fills them, and with none
 * they run Material's indeterminate animation, which follows the system's animator duration scale
 * as all Compose motion does. The view reads to TalkBack as one element: the label, the value
 * label and the indicator's progress.
 *
 * @param label what's in progress ("Uploading", "Checking for new mail"): the spinner's caption,
 *   the bar's title.
 * @param modifier the view's placement. The carded spinner and the bar fill the width they're
 *   given.
 * @param progress how far along the work is, 0 to 1, read while drawing so a changing value
 *   doesn't recompose. Null runs the indicator indeterminate.
 * @param valueLabel a readout at the end of the bar's title row ("60%", "Done", "Waiting"). The
 *   bar shows it; the spinner has no place for it and leaves it out.
 * @param style the spinner (the default, in a card) or the bar.
 */
@Composable
public fun AMProgressView(
    label: String,
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
    valueLabel: String? = null,
    style: AMProgressViewStyle = AMProgressViewStyle.Spinner(),
) {
    when (style) {
        is AMProgressViewStyle.Spinner -> SpinnerProgress(label, modifier, progress, style.card)
        AMProgressViewStyle.Bar -> BarProgress(label, modifier, progress, valueLabel)
    }
}

/** The circular indicator over its caption, centered, in a card unless [card] is false. */
@Composable
private fun SpinnerProgress(label: String, modifier: Modifier, progress: (() -> Float)?, card: Boolean) {
    val spacing = AMTheme.tokens.spacing
    val frame = if (card) {
        Modifier
            .fillMaxWidth()
            .amCard(padding = PaddingValues(horizontal = spacing.xl, vertical = spacing.lg))
    } else {
        Modifier
    }
    Column(
        modifier
            .then(frame)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (progress != null) CircularProgressIndicator(progress = progress) else CircularProgressIndicator()
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** The title and its value over the linear indicator, on the row recipe and a flat surface. */
@Composable
private fun BarProgress(label: String, modifier: Modifier, progress: (() -> Float)?, valueLabel: String?) {
    val spacing = AMTheme.tokens.spacing
    Column(
        modifier
            .fillMaxWidth()
            .amFlatSurface()
            .amRowPadding()
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                label,
                Modifier
                    .weight(1f)
                    .alignByBaseline(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (valueLabel != null) {
                Text(
                    valueLabel,
                    Modifier
                        .padding(start = spacing.md)
                        .alignByBaseline(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (progress != null) {
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}
