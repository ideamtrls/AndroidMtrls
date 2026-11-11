package com.ideamtrls.androidmtrls

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/**
 * The selection mark of a selectable photo or album tile: a black disc with Material's white check
 * glyph, traced by a white ring at the disc's edge, casting the [AMElevation.Text] shadow.
 *
 * ```kotlin
 * AMSelectionMark()
 * ```
 *
 * Most tiles use [Modifier.amSelectionCheck][amSelectionCheck], which places the mark in the
 * tile's corner and shows it only while selected.
 *
 * It looks the same in light and dark mode, because it sits on a picture, not on the page: the
 * black disc carries the check over a light picture, the white ring sets the disc off a dark one.
 * The ring is [AMTheme.selectionRingWidth] wide, drawn inside the disc's edge. It's a fixed 20 dp
 * whatever the font scale, like a glyph. A screen reader says "Selected".
 *
 * @param modifier the mark's placement.
 */
@Composable
public fun AMSelectionMark(modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .size(SelectionMarkSize)
            .clearAndSetSemantics { contentDescription = "Selected" }
            .then(SelectionMarkElement(shown = true, inCorner = false)),
    )
}

/**
 * Draws [AMSelectionMark] over this content's top end corner while [selected], inset by
 * `spacing.xs`, and nothing otherwise: the mark of a selected tile in a photo or album picker. Bind
 * it to the live selection; there's no need to add or drop the modifier.
 *
 * ```kotlin
 * AMRemoteImage(
 *     photo.url,
 *     contentDescription = photo.title,
 *     modifier = Modifier
 *         .aspectRatio(1f)
 *         .toggleable(value = photo in picked, role = Role.Checkbox) { picked = picked.toggled(photo) }
 *         .amSelectionCheck(photo in picked),
 * )
 * ```
 *
 * It draws the mark only; it adds nothing for TalkBack, so the tile carries its own state (a
 * `toggleable` or `selectable` tile says whether it's checked or selected). Put it after any clip
 * of the tile, so the mark sits inside the tile's corners, and after the tile's own click, so it
 * draws over the ripple.
 *
 * @param selected whether the mark shows.
 */
public fun Modifier.amSelectionCheck(selected: Boolean): Modifier =
    this then SelectionMarkElement(shown = selected, inCorner = true)

private data class SelectionMarkElement(val shown: Boolean, val inCorner: Boolean) :
    ModifierNodeElement<SelectionMarkNode>() {
    override fun create() = SelectionMarkNode(shown, inCorner)

    override fun update(node: SelectionMarkNode) {
        if (node.shown != shown || node.inCorner != inCorner) {
            node.shown = shown
            node.inCorner = inCorner
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = if (inCorner) "amSelectionCheck" else "AMSelectionMark"
        properties["shown"] = shown
    }
}

/**
 * Draws the content, then the mark: at the content's top end corner, or filling the node for the
 * mark on its own. The mark is recorded into a layer of its own size, so its shadow is cast by the
 * disc wherever the mark sits.
 */
private class SelectionMarkNode(var shown: Boolean, var inCorner: Boolean) :
    Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val shadows = ShapeShadows()
    private var layer: GraphicsLayer? = null

    override fun onDetach() {
        layer?.let(requireGraphicsContext()::releaseGraphicsLayer)
        layer = null
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (!shown) return
        val glyph = CheckGlyph.get(currentValueOf(LocalContext))
        val markPx = SelectionMarkSize.toPx()
        val origin = if (inCorner) {
            val inset = currentValueOf(LocalAMTokens).spacing.xs.toPx()
            val x = if (layoutDirection == LayoutDirection.Ltr) size.width - inset - markPx else inset
            Offset(x, inset)
        } else {
            Offset((size.width - markPx) / 2f, (size.height - markPx) / 2f)
        }
        val context = requireGraphicsContext()
        val mark = layer ?: context.createGraphicsLayer().also { layer = it }
        val side = ceil(markPx).toInt()
        // A plain density, not this draw scope: a layer recorded with the node's own scope as its
        // density hands density back and forth between the two scopes forever once the block reads
        // it (the shadow's blur, the ring's width).
        mark.record(Density(density, fontScale), layoutDirection, IntSize(side, side)) {
            shadows.draw(this, context, CircleShape, Style.Shadow.Text, dark = false)
            drawCircle(Color.Black)
            drawGlyph(glyph)
            drawInsetOutline(CircleShape, Color.White, Style.SelectionRing.Width)
        }
        translate(origin.x, origin.y) { drawLayer(mark) }
    }

    /** The check, white, centered in the disc at [GlyphRatio] of its size. */
    private fun DrawScope.drawGlyph(glyph: CheckGlyph) {
        val box = size.minDimension * GlyphRatio
        val offset = (size.minDimension - box) / 2f
        translate(offset, offset) {
            scale(box / glyph.viewport, pivot = Offset.Zero) {
                drawPath(glyph.path, Color.White)
            }
        }
    }
}

/**
 * Material's check glyph (`am_ic_check`) as a path in its own square viewport, loaded once and
 * shared by every mark.
 */
private class CheckGlyph(val path: Path, val viewport: Float) {
    companion object {
        private var loaded: CheckGlyph? = null

        fun get(context: Context): CheckGlyph = loaded ?: load(context).also { loaded = it }

        private fun load(context: Context): CheckGlyph {
            val vector = ImageVector.vectorResource(context.theme, context.resources, R.drawable.am_ic_check)
            val path = Path()
            fun collect(group: VectorGroup) {
                group.forEach { node ->
                    when (node) {
                        is VectorPath -> path.addPath(node.pathData.toPath())
                        is VectorGroup -> collect(node)
                    }
                }
            }
            collect(vector.root)
            return CheckGlyph(path, vector.viewportWidth)
        }
    }
}

/** The mark's disc. */
private val SelectionMarkSize = 20.dp

/** The check glyph's box, as a share of the disc: the glyph's own margins leave the tick about half the disc wide. */
private const val GlyphRatio = 0.7f
