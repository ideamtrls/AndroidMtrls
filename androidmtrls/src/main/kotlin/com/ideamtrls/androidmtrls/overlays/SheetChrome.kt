package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * The sheet's shadow and hairline (or, frosted, its glass), drawn exactly where the sheet is.
 *
 * Material's sheet takes this as its `modifier`, which comes first in the sheet's chain: ahead of
 * the offset that moves the sheet to its drag position and ahead of its clip. What it draws isn't
 * clipped, but it would draw where the sheet's layout starts (the top of the window). So the first
 * offset moves the chrome to the sheet's current offset, and the last moves the rest of the chain
 * back by the same amount, leaving the sheet's own placement untouched. Material passes its sheet
 * no shadow of its own.
 *
 * - Solid (the primary or page surface, which Material's container paints): the `Medium`
 *   two-layer shadow under the sheet and the hairline over its edge, in both modes.
 * - Frosted ([frosted], over a transparent container): [GlassNode].
 *
 * The chrome sits outside Material's predictive back scaling, so while a back gesture shrinks the
 * sheet, the chrome keeps the sheet's resting size.
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun Modifier.sheetChrome(
    sheetState: SheetState,
    shape: Shape,
    frosted: Boolean,
    backdrop: AMBackdropSource?,
): Modifier =
    this
        .offset { IntOffset(0, sheetOffset(sheetState)) }
        .then(
            if (frosted) {
                GlassElement(shape, backdrop)
            } else {
                Modifier.amElevation(AMElevation.Medium, shape).amHairline(shape)
            },
        )
        .offset { IntOffset(0, -sheetOffset(sheetState)) }

/** The sheet's offset from the top of its window, or far below the screen before it's known. */
@OptIn(ExperimentalMaterial3Api::class)
private fun sheetOffset(sheetState: SheetState): Int {
    val offset = try {
        sheetState.requireOffset()
    } catch (_: IllegalStateException) {
        return OffscreenOffset
    }
    return if (offset.isNaN()) OffscreenOffset else offset.roundToInt()
}

private const val OffscreenOffset = 100_000

private data class GlassElement(val shape: Shape, val backdrop: AMBackdropSource?) : ModifierNodeElement<GlassNode>() {
    override fun create() = GlassNode(shape, backdrop)

    override fun update(node: GlassNode) {
        node.shape = shape
        node.backdrop = backdrop
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "sheetGlass"
        properties["shape"] = shape
    }
}

/**
 * A frosted sheet's surface: the art of the [AMBackdrop] the sheet was presented from, under the
 * sheet, blurred by [AMTheme.Frosted.blurRadius], then the frosted fill, the sheet, and the
 * frosted edge; the `Medium` shadow falls outside the shape only, since under glass it would show
 * through.
 *
 * The sheet is a window of its own, so it finds its slice of the art by screen position rather
 * than through the layout tree, and samples it again whenever the sheet moves. With no backdrop,
 * or before the art and the sheet are placed, it draws the solid sheet instead, never unblurred
 * translucency.
 */
private class GlassNode(var shape: Shape, var backdrop: AMBackdropSource?) :
    Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode {

    private val shadows = ShapeShadows()
    private var layer: GraphicsLayer? = null
    private var effect: BlurEffect? = null
    private var effectRadius = -1f
    private var coordinates: LayoutCoordinates? = null
    private var lastPosition = Offset.Unspecified

    override fun onAttach() {
        layer = requireGraphicsContext().createGraphicsLayer()
    }

    override fun onDetach() {
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = null
        coordinates = null
        lastPosition = Offset.Unspecified
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        if (!coordinates.isAttached) return
        val position = coordinates.localToScreen(Offset.Zero)
        if (position != lastPosition) {
            lastPosition = position
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        val dark = currentValueOf(LocalAMDarkTheme) ?: false
        val context = requireGraphicsContext()
        val level = AMElevation.Medium.shadowLevel
        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@draw)) }

        val glass = layer
        if (glass != null && sampleArt(glass)) {
            clipPath(path, ClipOp.Difference) { shadows.draw(this, context, shape, level, dark) }
            clipPath(path) { drawLayer(glass) }
            drawPath(path, if (dark) Style.Frosted.FillDark else Style.Frosted.FillLight)
            drawContent()
            drawInsetOutline(shape, if (dark) Style.Frosted.EdgeDark else Style.Frosted.EdgeLight, Style.Hairline.Width)
        } else {
            shadows.draw(this, context, shape, level, dark)
            drawPath(path, if (dark) Style.Surface.Dark else Style.Surface.Light)
            drawContent()
            drawInsetOutline(shape, if (dark) Style.SelectionRing.Dark else Style.SelectionRing.Light, Style.Hairline.Width)
        }
    }

    /**
     * Records the art under the sheet, blurred, into [glass], placed over the sheet. Returns false,
     * recording nothing, while there's nothing to sample: no backdrop, the art or the sheet not
     * placed, or no art under the sheet.
     */
    private fun ContentDrawScope.sampleArt(glass: GraphicsLayer): Boolean {
        val source = backdrop ?: return false
        val art = source.coordinates?.takeIf { it.isAttached } ?: return false
        val sheet = coordinates?.takeIf { it.isAttached } ?: return false
        // Two windows: through the screen, not the layout tree.
        val offset = art.screenToLocal(sheet.localToScreen(Offset.Zero))
        val radius = Style.Frosted.BlurRadius.toPx()
        val region = frostedSampleRegion(
            x = offset.x,
            y = offset.y,
            width = size.width,
            height = size.height,
            margin = radius * 2f,
            artWidth = art.size.width.toFloat(),
            artHeight = art.size.height.toFloat(),
        ) ?: return false

        if (radius != effectRadius) {
            effectRadius = radius
            effect = if (radius > 0f) BlurEffect(radius, radius, TileMode.Clamp) else null
        }
        glass.renderEffect = effect
        glass.topLeft = IntOffset((region.left - offset.x).roundToInt(), (region.top - offset.y).roundToInt())
        glass.record(IntSize(region.width, region.height)) {
            translate(-region.left.toFloat(), -region.top.toFloat()) { drawLayer(source.layer) }
        }
        return true
    }
}
