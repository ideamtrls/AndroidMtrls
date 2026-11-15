package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * How an [AMProgressView] shows its progress. Sealed, so only these three exist:
 *
 * - [Spinner]: Material's circular indicator over a caption, centered, framed in a card in the
 *   app's surface setting by default.
 * - [Glyph]: Material's circular indicator at a glyph's size, with no caption, in a row's
 *   trailing place.
 * - [Bar]: Material's linear indicator under a title and a trailing value, on the row recipe.
 */
@Immutable
public sealed interface AMProgressViewStyle {

    /**
     * Material's circular progress indicator, centered over the label as a caption (`bodySmall`
     * in the surface's secondary text color, [AMTheme.secondaryText]), `spacing.md` apart: for work that holds up a view or a block.
     *
     * @property card whether the spinner floats in its own full-width card (`spacing.xl` at the
     *   sides, `spacing.lg` above and below), the way a view that's loading shows it on the page,
     *   in the app's surface setting ([Modifier.amCard][amCard]): solid in a solid app; in a
     *   frosted app frosted on the backdrop, frost over the page color with no art under it, and
     *   the tint on a sheet.
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
     * Material's circular progress indicator at a glyph's size, with no caption: for work on one
     * item in a list (a download, a refresh), in the place a row's trailing glyph or icon button
     * takes. It's a 24 dp glyph's box and no more: it takes no input, so it needs no touch
     * target, and at a row's end it lines up with the other rows' trailing content, as a trailing
     * icon does. Material's proportions at a glyph's size: a 20 dp ring 2 dp thick (Material's own
     * is 40 dp and 4 dp) in the 24 dp a glyph takes, its live area and padding. The label isn't
     * shown; it's what a screen reader says.
     */
    @Immutable
    public data object Glyph : AMProgressViewStyle

    /**
     * Material's linear progress indicator, full width, under a row of the label (`bodyLarge` in
     * `onSurface`, up to two lines, ellipsized) and the value label at its end (`bodyMedium` in
     * [AMTheme.secondaryText], one line), `spacing.sm` apart, on the row recipe. Like a flat row, it
     * paints a strip of the app's surface when it stands on its own (the floating surface's color
     * in a solid app; frosted, or the tint on a sheet, in a frosted app) and nothing inside a card,
     * its lines lying on the strip or the card ([ProvideAMOnSurface]), so it stacks with other rows
     * in one, divided by [AMDivider]. In a row container (an
     * [AMSwipeableRow], a reorderable list) it's the row: TalkBack finds the container's actions
     * on it, one stop.
     */
    @Immutable
    public data object Bar : AMProgressViewStyle
}

/**
 * Progress on Android's own indicators, labeled: Material's circular indicator over a caption
 * ([AMProgressViewStyle.Spinner]) or at a glyph's size with the label spoken only
 * ([AMProgressViewStyle.Glyph]), or Material's linear indicator under a title and its value
 * ([AMProgressViewStyle.Bar]).
 *
 * ```kotlin
 * AMProgressView("Checking for new mail")                                     // an indeterminate spinner, in a card
 * AMProgressView("Uploading", progress = { uploaded }, valueLabel = "60%", style = AMProgressViewStyle.Bar)
 * AMListRow(episode.title, trailingContent = { AMProgressView("Downloading", progress = { done }, style = AMProgressViewStyle.Glyph) })
 * ```
 *
 * The indicators keep Material's look, colors and motion: a [progress] fills them, and with none
 * they run Material's indeterminate animation, which follows the system's animator duration scale
 * as all Compose motion does. The view reads to TalkBack as one element: the label, the value
 * label and the indicator's progress.
 *
 * @param label what's in progress ("Uploading", "Checking for new mail"): the spinner's caption,
 *   the bar's title, what a screen reader says for the glyph.
 * @param modifier the view's placement. The carded spinner and the bar fill the width they're
 *   given; the glyph is 24 dp square.
 * @param progress how far along the work is, 0 to 1, read while drawing so a changing value
 *   doesn't recompose. Null runs the indicator indeterminate.
 * @param valueLabel a readout at the end of the bar's title row ("60%", "Done", "Waiting"). The
 *   bar shows it; the spinner and the glyph have no place for it and leave it out.
 * @param style the spinner (the default, in a card), the glyph or the bar.
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
        AMProgressViewStyle.Glyph -> GlyphProgress(label, modifier, progress)
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
            color = AMTheme.secondaryText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** The circular indicator in a glyph's 24 dp box, with [label] as its spoken name. */
@Composable
private fun GlyphProgress(label: String, modifier: Modifier, progress: (() -> Float)?) {
    val ring = Modifier
        .padding(GlyphRingInset)
        .size(GlyphRingSize)
    Box(modifier.semantics(mergeDescendants = true) { contentDescription = label }) {
        if (progress != null) {
            CircularProgressIndicator(progress = progress, modifier = ring, strokeWidth = GlyphRingStroke)
        } else {
            CircularProgressIndicator(modifier = ring, strokeWidth = GlyphRingStroke)
        }
    }
}

/**
 * The title and its value over the linear indicator, on the row recipe and a flat surface. It's a
 * row, so it claims a row container's actions onto its own merged node.
 */
@Composable
private fun BarProgress(label: String, modifier: Modifier, progress: (() -> Float)?, valueLabel: String?) {
    val spacing = AMTheme.tokens.spacing
    val barModifier = modifier
        .fillMaxWidth()
        .amFlatSurface()
        .then(claimRowActions())
        .amRowPadding()
        .semantics(mergeDescendants = true) {}
    CompositionLocalProvider(LocalAMRowActions provides null) {
        Column(barModifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            // The strip stands where the bar is placed; its lines lie on it, off any art around it.
            ProvideAMOnSurface {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        label,
                        Modifier
                            .weight(1f)
                            .alignByBaseline(),
                        color = MaterialTheme.colorScheme.onSurface,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = BarLabelMaxLines,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (valueLabel != null) {
                        Text(
                            valueLabel,
                            Modifier
                                .padding(start = spacing.md)
                                .alignByBaseline(),
                            color = AMTheme.secondaryText,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
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
    }
}

/** The glyph's ring: a glyph's 20 dp live area. */
private val GlyphRingSize = 20.dp

/** The padding around the ring that makes up a glyph's 24 dp. */
private val GlyphRingInset = 2.dp

/** The ring's thickness: Material's 4 dp on its 40 dp ring, at half the size. */
private val GlyphRingStroke = 2.dp

/** How many lines the bar's label takes before it ellipsizes, so a long title keeps rows even. */
private const val BarLabelMaxLines = 2
