package com.ideamtrls.androidmtrls

import android.view.View
import android.view.ViewTreeObserver
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
 * The sheet's frosted background, its shadow and its edge ([FrostNode]), drawn exactly where the
 * sheet is, over Material's transparent container. [backdrop] is the region the sheet blurs, and
 * [screen] the view of the window it's presented over, whose draws it follows.
 *
 * Material's sheet takes this as its `modifier`, which comes first in the sheet's chain: ahead of
 * the offset that moves the sheet to its drag position and ahead of its clip. What it draws isn't
 * clipped, but it would draw where the sheet's layout starts (the top of the window). So the first
 * offset moves the chrome to the sheet's current offset, and the last moves the rest of the chain
 * back by the same amount, leaving the sheet's own placement untouched. Material passes its sheet
 * no shadow of its own.
 *
 * The chrome sits outside Material's predictive back scaling, so while a back gesture shrinks the
 * sheet, the chrome keeps the sheet's resting size.
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun Modifier.sheetChrome(
    sheetState: SheetState,
    shape: Shape,
    backdrop: AMBackdropSource?,
    screen: View,
): Modifier =
    this
        .offset { IntOffset(0, sheetOffset(sheetState)) }
        .then(FrostElement(shape, backdrop, screen))
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

private data class FrostElement(
    val shape: Shape,
    val backdrop: AMBackdropSource?,
    val screen: View,
) : ModifierNodeElement<FrostNode>() {
    override fun create() = FrostNode(shape, backdrop, screen)

    override fun update(node: FrostNode) {
        node.shape = shape
        node.backdrop = backdrop
        node.follow(screen)
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "sheetFrost"
        properties["shape"] = shape
    }
}

/**
 * The sheet's frosted background: the screen under the sheet (the region of the [AMBackdrop] the
 * sheet was presented from, its art and everything on it), blurred by
 * [AMBlur.Frost], then the frosted fill, the sheet, and the frosted edge; the
 * `Medium` shadow falls outside the shape only, since under frost it would show through.
 *
 * The sheet is a window of its own, so it finds its slice of the region by screen position rather
 * than through the layout tree, and samples it again whenever the sheet moves and whenever the
 * window under it draws (a band's new picture, a scroll, the + fading out), which its own window
 * wouldn't otherwise redraw for. With no backdrop, or before the region and the sheet are placed,
 * the page color takes the screen's place under the fill: frost over the page, opaque, never
 * unblurred translucency.
 */
private class FrostNode(var shape: Shape, var backdrop: AMBackdropSource?, screen: View) :
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

    /** The view of the window under the sheet, whose draws it follows while attached. */
    private var screen: View = screen

    private val screenDrew = ViewTreeObserver.OnDrawListener { if (isAttached) invalidateDraw() }

    /** Follows [view]'s window from now on, instead of the one before. */
    fun follow(view: View) {
        if (view === screen) return
        if (isAttached) stopFollowing()
        screen = view
        if (isAttached) screen.viewTreeObserver.addOnDrawListener(screenDrew)
    }

    private fun stopFollowing() {
        val observer = screen.viewTreeObserver
        if (observer.isAlive) observer.removeOnDrawListener(screenDrew)
    }

    override fun onAttach() {
        layer = requireGraphicsContext().createGraphicsLayer()
        screen.viewTreeObserver.addOnDrawListener(screenDrew)
    }

    override fun onDetach() {
        stopFollowing()
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

        // Under frost the shadow would show through, so it falls outside the shape only.
        clipPath(path, ClipOp.Difference) { shadows.draw(this, context, shape, level, dark) }
        val frost = layer
        if (frost != null && sampleScreen(frost)) {
            clipPath(path) { drawLayer(frost) }
        } else {
            // Nothing to sample: frost over the page color, opaque.
            drawPath(path, if (dark) Style.Page.Dark else Style.Page.Light)
        }
        drawPath(path, if (dark) Style.Frosted.FillDark else Style.Frosted.FillLight)
        drawContent()
        drawInsetOutline(shape, if (dark) Style.Frosted.EdgeDark else Style.Frosted.EdgeLight, Style.Hairline.Width)
    }

    /**
     * Records the screen under the sheet, blurred, into [frost], placed over the sheet. Returns
     * false, recording nothing, while there's nothing to sample: no backdrop, the region or the
     * sheet not placed, or none of the region under the sheet.
     */
    private fun ContentDrawScope.sampleScreen(frost: GraphicsLayer): Boolean {
        val source = backdrop ?: return false
        val art = source.coordinates?.takeIf { it.isAttached } ?: return false
        val sheet = coordinates?.takeIf { it.isAttached } ?: return false
        // Two windows: through the screen, not the layout tree.
        val offset = art.screenToLocal(sheet.localToScreen(Offset.Zero))
        val radius = AMBlur.Frost.radius.toPx()
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
        frost.renderEffect = effect
        frost.topLeft = IntOffset((region.left - offset.x).roundToInt(), (region.top - offset.y).roundToInt())
        frost.record(IntSize(region.width, region.height)) {
            translate(-region.left.toFloat(), -region.top.toFloat()) { drawLayer(source.screen) }
        }
        return true
    }
}
