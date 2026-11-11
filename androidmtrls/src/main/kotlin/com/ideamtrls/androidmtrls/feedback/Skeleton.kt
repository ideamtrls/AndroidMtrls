package com.ideamtrls.androidmtrls

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Placeholder shapes for content that's still loading: a block, a circle and lines of text, filled
 * in the color scheme's `surfaceContainerHighest`, each shimmering ([amShimmer]). Lay them out the
 * way the content will sit, then swap the real content in when it arrives.
 *
 * ```kotlin
 * if (post == null) {
 *     Column(Modifier.fillMaxWidth().amCard(padding = PaddingValues(16.dp)), verticalArrangement = Arrangement.spacedBy(12.dp)) {
 *         Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
 *             AMSkeleton.Circle()
 *             AMSkeleton.Text(Modifier.weight(1f), lines = 2)
 *         }
 *         AMSkeleton.Rect(height = 160.dp, shape = MaterialTheme.shapes.medium)
 *     }
 * } else {
 *     PostCard(post)
 * }
 * ```
 *
 * Where the loading layout has the real text in hand (a name, a heading, a count), [Text] with
 * that text holds its exact place: the text laid out as it will be, shown as a shimmering bar per
 * line, so the swap to the real text moves nothing.
 *
 * ```kotlin
 * if (loading) AMSkeleton.Text(month.title, style = MaterialTheme.typography.titleSmall) else Text(month.title, style = MaterialTheme.typography.titleSmall)
 * ```
 *
 * The shapes are placeholders and nothing else: they take no touches and say nothing to TalkBack.
 */
public object AMSkeleton {

    /**
     * A block: a line, a picture's or a button's place. It fills the width it's given, so size it
     * with [modifier] (`Modifier.width(120.dp)`, `Modifier.fillMaxWidth(0.4f)`, a weight).
     *
     * @param modifier the block's size and placement: its width, since the block fills the width
     *   it's given.
     * @param height the block's height: 14 dp by default, a line of text.
     * @param shape the block's corners: the theme's small shape by default.
     */
    @Composable
    public fun Rect(
        modifier: Modifier = Modifier,
        height: Dp = 14.dp,
        shape: Shape = MaterialTheme.shapes.small,
    ) {
        Box(
            modifier
                .fillMaxWidth()
                .height(height)
                .amShimmer()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, shape),
        )
    }

    /**
     * A circle: an avatar's or a glyph's place.
     *
     * @param modifier the circle's placement.
     * @param diameter the circle's size: 40 dp by default, an avatar's.
     */
    @Composable
    public fun Circle(
        modifier: Modifier = Modifier,
        diameter: Dp = 40.dp,
    ) {
        Box(
            modifier
                .size(diameter)
                .amShimmer()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
        )
    }

    /**
     * Lines of text: rounded bars filling the width they're given, the last one 60 % as wide when
     * there's more than one, so the block reads as a paragraph rather than as blocks. One sweep
     * runs across the whole block.
     *
     * @param modifier the block's size and placement. The lines fill the width it's given.
     * @param lines how many lines: 3 by default.
     * @param lineHeight each line's height: 12 dp by default.
     * @param spacing the space between lines: the spacing scale's `sm` by default.
     */
    @Composable
    public fun Text(
        modifier: Modifier = Modifier,
        lines: Int = 3,
        lineHeight: Dp = 12.dp,
        spacing: Dp = AMTheme.tokens.spacing.sm,
    ) {
        val color = MaterialTheme.colorScheme.surfaceContainerHighest
        Column(
            modifier
                .fillMaxWidth()
                .amShimmer(),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            repeat(lines) { line ->
                val short = lines > 1 && line == lines - 1
                Box(
                    Modifier
                        .fillMaxWidth(if (short) LastLineFraction else 1f)
                        .height(lineHeight)
                        .background(color, CircleShape),
                )
            }
        }
    }

    /**
     * The placeholder of a text in hand: [text] laid out exactly as `Text(text, style = style)`
     * would lay it out, the same size and line height, wrapping the same way, but drawn as one
     * rounded bar per line, as wide as that line's words, shimmering. Swap the real text in when
     * it's ready and nothing around it moves.
     *
     * ```kotlin
     * AMSectionHeader { if (loading) AMSkeleton.Text(month.title) else Text(month.title) }
     * AMSkeleton.Text(product.name, style = MaterialTheme.typography.titleSmall, minLines = 2, maxLines = 2)
     * ```
     *
     * The glyphs never draw, and TalkBack hears nothing: the text only sizes the placeholder.
     *
     * @param text the text the placeholder stands for.
     * @param modifier the placeholder's placement. It takes the text's own size, as a `Text` does.
     * @param style the text's style: the current `LocalTextStyle` by default, so it matches a text
     *   in the same place (a section header's slot, a list item's headline).
     * @param minLines the fewest lines the text takes, as for `Text`: lines past the text's own
     *   keep their room and show no bar.
     * @param maxLines the most lines the text takes, as for `Text`; past them it's cut off.
     */
    @Composable
    public fun Text(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = LocalTextStyle.current,
        minLines: Int = 1,
        maxLines: Int = Int.MAX_VALUE,
    ) {
        val color = MaterialTheme.colorScheme.surfaceContainerHighest
        val layout = remember { mutableStateOf<TextLayoutResult?>(null) }
        BasicText(
            text,
            modifier
                .clearAndSetSemantics {}
                .amShimmer()
                .drawWithContent {
                    // The bars in place of the glyphs: the text itself is never drawn.
                    layout.value?.let { drawTextBars(it, color) }
                },
            style = style,
            onTextLayout = { layout.value = it },
            overflow = TextOverflow.Clip,
            softWrap = true,
            maxLines = maxLines,
            minLines = minLines,
        )
    }
}

/** The last line of an [AMSkeleton.Text] block, as a fraction of the block's width. */
private const val LastLineFraction = 0.6f

/**
 * How much of each line's height a text placeholder's bar leaves clear above and below it, so the
 * bar sits on the line the way the glyphs do and neighboring lines' bars stay apart.
 */
private const val TextBarInset = 0.15f

/** A rounded bar over each laid-out line of [layout], from the line's start to its end. */
private fun DrawScope.drawTextBars(layout: TextLayoutResult, color: Color) {
    for (line in 0 until layout.lineCount) {
        val left = layout.getLineLeft(line)
        val right = layout.getLineRight(line)
        if (right - left <= 0f) continue
        val top = layout.getLineTop(line)
        val bottom = layout.getLineBottom(line)
        val inset = (bottom - top) * TextBarInset
        val height = bottom - top - inset * 2f
        if (height <= 0f) continue
        drawRoundRect(
            color,
            topLeft = Offset(left, top + inset),
            size = Size(right - left, height),
            cornerRadius = CornerRadius(height / 2f),
        )
    }
}

/**
 * Sweeps a shimmer across this content: a soft white band, left to right and over again, lighting
 * only what the content draws and leaving its gaps alone. The one shimmer of every loading
 * placeholder ([AMSkeleton]'s shapes wear it); put it on a placeholder layout of your own the same
 * way, ahead of the modifiers and children that draw it.
 *
 * ```kotlin
 * Row(Modifier.amShimmer()) { Placeholders() }
 * ```
 *
 * The sweep is the component's own motion. Every shimmer on screen runs in step, from the frame
 * clock, and stops when the content leaves the composition. Under the system's Remove animations
 * (the animator duration scale at 0) the content draws still, with no band; a slower or faster
 * scale slows or speeds the sweep.
 *
 * The content draws in a layer of its own, cut to its bounds, so the band can follow its shapes:
 * shimmer the placeholders inside a card, not the card itself, or its shadow would be cut off.
 */
public fun Modifier.amShimmer(): Modifier =
    this.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen) then ShimmerElement

private data object ShimmerElement : ModifierNodeElement<ShimmerNode>() {
    override fun create() = ShimmerNode()

    override fun update(node: ShimmerNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "amShimmer"
    }
}

/**
 * Draws the content, then the band over it, blended source-atop so it lights only the content's
 * pixels. The band's place comes from the frame clock, so the node redraws its own layer each
 * frame without recomposing anything.
 */
private class ShimmerNode : Modifier.Node(), DrawModifierNode {
    /** How far the band is through its sweep, 0 to 1, or [Still] while it shows none. */
    private var phase = Still
    private var brushWidth = -1f
    private var brush: Brush? = null

    override fun onAttach() {
        coroutineScope.launch { sweep() }
    }

    override fun onDetach() {
        phase = Still
    }

    private suspend fun sweep() {
        val scale = currentCoroutineContext()[MotionDurationScale]
        while (true) {
            if (scale != null && scale.scaleFactor == 0f) {
                // Remove animations: draw the content still until animations come back.
                if (phase != Still) {
                    phase = Still
                    invalidateDraw()
                }
                snapshotFlow { scale.scaleFactor }.first { it > 0f }
            }
            withInfiniteAnimationFrameNanos { frameNanos ->
                val factor = scale?.scaleFactor ?: 1f
                if (factor > 0f) {
                    val cycle = (ShimmerCycleNanos * factor).toLong().coerceAtLeast(1L)
                    phase = (frameNanos % cycle).toFloat() / cycle
                    invalidateDraw()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (phase == Still || size.width <= 0f) return
        val bandWidth = maxOf(size.width * BandFraction, BandMinWidth.toPx())
        val band = brush?.takeIf { brushWidth == bandWidth } ?: Brush.horizontalGradient(
            0f to Color.White.copy(alpha = 0f),
            0.5f to Color.White.copy(alpha = HighlightAlpha),
            1f to Color.White.copy(alpha = 0f),
            startX = 0f,
            endX = bandWidth,
        ).also {
            brush = it
            brushWidth = bandWidth
        }
        // Off the start at 0, off the end at 1, so the wrap from one sweep to the next never shows.
        val x = -bandWidth + (size.width + bandWidth) * phase
        translate(left = x) {
            drawRect(band, size = Size(bandWidth, size.height), blendMode = BlendMode.SrcAtop)
        }
    }
}

/** No band: the content draws as it is. */
private const val Still = -1f

/** One sweep across the content, at an animator duration scale of 1. */
private const val ShimmerCycleNanos = 1_200_000_000L

/** The band's width, as a fraction of the content's, and its floor on narrow content. */
private const val BandFraction = 0.4f
private val BandMinWidth = 60.dp

/** The band's peak opacity, at its center: white, so it brightens the placeholder in both appearances. */
private const val HighlightAlpha = 0.3f
