package com.ideamtrls.androidmtrls

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import kotlin.math.max

/**
 * Outlines this content with a hairline in [shape], over the content, drawn inside the bounds so it
 * never grows the surface, and never thinner than one pixel.
 *
 * ```kotlin
 * Modifier.background(AMTheme.Surface.primary, CircleShape).amHairline(CircleShape)
 * ```
 *
 * For outlines that always show: sheets, tiles, the floating action button, the banner, avatars.
 * Solid cards draw their own, in dark mode only, where the shadow alone can't lift black off the
 * page.
 *
 * @param shape the outline's shape: the surface's own.
 * @param color the line's color. Unspecified (the default) draws [AMTheme.selectionRing] for the
 *   theme's appearance.
 * @param width the line's width: [AMTheme.hairlineWidth] by default, or
 *   [AMTheme.selectionRingWidth] for a chosen item.
 */
public fun Modifier.amHairline(
    shape: Shape,
    color: Color = Color.Unspecified,
    width: Dp = Style.Hairline.Width,
): Modifier = this then HairlineElement(shape, color, width)

private data class HairlineElement(val shape: Shape, val color: Color, val width: Dp) :
    ModifierNodeElement<HairlineNode>() {
    override fun create() = HairlineNode(shape, color, width)

    override fun update(node: HairlineNode) {
        node.shape = shape
        node.color = color
        node.width = width
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "amHairline"
        properties["shape"] = shape
        properties["color"] = color
        properties["width"] = width
    }
}

private class HairlineNode(var shape: Shape, var color: Color, var width: Dp) :
    Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    override fun ContentDrawScope.draw() {
        drawContent()
        val dark = currentValueOf(LocalAMDarkTheme) ?: false
        drawInsetOutline(shape, if (color.isSpecified) color else selectionRing(dark), width)
    }
}

/**
 * Strokes [shape]'s outline at [width] (never thinner than one pixel) inside the bounds: the
 * stroke's outer edge lies on the outline.
 */
internal fun DrawScope.drawInsetOutline(shape: Shape, color: Color, width: Dp) {
    val stroke = max(width.toPx(), 1f)
    val half = stroke / 2f
    when (val outline = shape.createOutline(size, layoutDirection, this)) {
        is Outline.Rectangle -> drawRect(
            color = color,
            topLeft = Offset(half, half),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(stroke),
        )
        is Outline.Rounded -> {
            val box = outline.roundRect
            fun CornerRadius.inset() = CornerRadius(max(x - half, 0f), max(y - half, 0f))
            val inset = RoundRect(
                left = box.left + half,
                top = box.top + half,
                right = box.right - half,
                bottom = box.bottom - half,
                topLeftCornerRadius = box.topLeftCornerRadius.inset(),
                topRightCornerRadius = box.topRightCornerRadius.inset(),
                bottomRightCornerRadius = box.bottomRightCornerRadius.inset(),
                bottomLeftCornerRadius = box.bottomLeftCornerRadius.inset(),
            )
            drawPath(Path().apply { addRoundRect(inset) }, color, style = Stroke(stroke))
        }
        // Any other shape: a stroke twice as wide, clipped to the shape, leaves its inner half.
        is Outline.Generic -> clipPath(outline.path) {
            drawPath(outline.path, color, style = Stroke(stroke * 2f))
        }
    }
}

private fun selectionRing(dark: Boolean): Color = if (dark) Style.SelectionRing.Dark else Style.SelectionRing.Light

/**
 * Whether the content lies on a floating surface that isn't in its layout tree: a sheet's
 * [AMSheetSurface.Primary] or [AMSheetSurface.Frosted] surface, which is a window of its own, so a
 * control inside can't find it by looking up the tree. The sheet provides it to its content; an
 * overlay window's fresh context resets it. Read through [sitsOnPrimarySurface].
 */
internal val LocalAMOnPrimarySurface = compositionLocalOf { false }

/**
 * Whether a solid control at this node sits on a black floating surface in dark mode, where the
 * selection ring would vanish against the black around it, so its outline steps up to
 * [AMTheme.controlOutline]: inside a card (any [NestingSurfaceKey] node above it in the layout tree,
 * flat or not), or on a sheet that says so ([LocalAMOnPrimarySurface]).
 *
 * Call it only while the node is attached (in measure, layout or draw).
 */
internal fun <T> T.sitsOnPrimarySurface(): Boolean
    where T : DelegatableNode, T : CompositionLocalConsumerModifierNode =
    currentValueOf(LocalAMOnPrimarySurface) || findNearestAncestor(NestingSurfaceKey) != null

/**
 * The elevated surface every floating surface wears (cards, filled buttons, toolbar buttons,
 * tiles, sheets), drawn around the content in one place so the recipe never drifts:
 *
 * - [AMSurfaceFill.Solid]: [elevation]'s shadow cast by [shape], the shape filled with
 *   [AMTheme.Surface.primary], the content, then in dark mode only a hairline, where the shadow
 *   alone can't lift black off the page.
 * - [AMSurfaceFill.Frosted]: the shadow outside the shape only (under glass it would show
 *   through), the nearest [AMBackdrop]'s art under the shape blurred by
 *   [AMTheme.Frosted.blurRadius], the frosted fill, the content, then the frosted edge in both
 *   modes. With no backdrop, or before the art is placed, it draws the solid surface instead,
 *   never unblurred translucency.
 *
 * It doesn't clip the content. Anything the content draws in its own background (a press wash)
 * sits above the fill.
 *
 * @param hairline the dark-mode outline of the solid fill. Unspecified draws
 *   [AMTheme.selectionRing]; a solid control on a black card or sheet passes [AMTheme.controlOutline].
 */
internal fun Modifier.amElevatedSurface(
    shape: Shape,
    elevation: AMElevation = AMElevation.Low,
    fill: AMSurfaceFill = AMSurfaceFill.Solid,
    hairline: Color = Color.Unspecified,
): Modifier = this then ElevatedSurfaceElement(shape, elevation, fill, hairline)

private data class ElevatedSurfaceElement(
    val shape: Shape,
    val elevation: AMElevation,
    val fill: AMSurfaceFill,
    val hairline: Color,
) : ModifierNodeElement<ElevatedSurfaceNode>() {
    override fun create() = ElevatedSurfaceNode(shape, elevation, fill, hairline)

    override fun update(node: ElevatedSurfaceNode) {
        node.shape = shape
        node.elevation = elevation
        node.fill = fill
        node.hairline = hairline
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "amElevatedSurface"
        properties["shape"] = shape
        properties["elevation"] = elevation
        properties["fill"] = fill
        properties["hairline"] = hairline
    }
}

private class ElevatedSurfaceNode(
    var shape: Shape,
    var elevation: AMElevation,
    var fill: AMSurfaceFill,
    var hairline: Color,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode {

    private val surface = ElevatedSurfaceDrawer()

    override fun onAttach() = surface.attach(requireGraphicsContext())

    override fun onDetach() = surface.detach(requireGraphicsContext())

    // A frosted surface that moves over its art (a scroll) samples a different slice, so it redraws.
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val backdrop = if (fill == AMSurfaceFill.Frosted) currentValueOf(LocalAMBackdrop) else null
        if (surface.position(coordinates, backdrop)) invalidateDraw()
    }

    override fun ContentDrawScope.draw() = with(surface) {
        drawSurface(
            context = requireGraphicsContext(),
            shape = shape,
            elevation = elevation,
            fill = fill,
            hairline = hairline,
            dark = currentValueOf(LocalAMDarkTheme) ?: false,
            backdrop = if (fill == AMSurfaceFill.Frosted) currentValueOf(LocalAMBackdrop) else null,
            clipContent = false,
        )
    }
}

/**
 * The elevated surface's drawing, shared by every node that wears it ([Modifier.amElevatedSurface],
 * the card, a filled button's surface): the shape's shadows, the frosted slice of the art, and where the surface sits. A node
 * owns one, attaches and detaches it with itself, reports its position, and draws through it.
 */
internal class ElevatedSurfaceDrawer {
    private val shadows = ShapeShadows()
    private val slice = FrostedSlice()
    private var coordinates: LayoutCoordinates? = null
    private var lastOffset = Offset.Unspecified

    fun attach(context: GraphicsContext) {
        slice.layer = context.createGraphicsLayer()
    }

    fun detach(context: GraphicsContext) {
        slice.layer?.let { context.releaseGraphicsLayer(it) }
        slice.layer = null
        coordinates = null
        lastOffset = Offset.Unspecified
    }

    /**
     * Records where the surface sits. Returns true when a frosted surface (one with a [backdrop])
     * has moved over its art since it last drew, so it has to redraw its slice.
     */
    fun position(coordinates: LayoutCoordinates, backdrop: AMBackdropSource?): Boolean {
        this.coordinates = coordinates
        val art = backdrop?.coordinates?.takeIf { it.isAttached } ?: return false
        if (!coordinates.isAttached) return false
        val offset = art.localPositionOf(coordinates, Offset.Zero)
        if (offset == lastOffset) return false
        lastOffset = offset
        return true
    }

    /**
     * Draws the surface around the content: frosted over [backdrop] when there is one and its art
     * is placed, solid otherwise. With [clipContent] the content is clipped to [shape].
     */
    fun ContentDrawScope.drawSurface(
        context: GraphicsContext,
        shape: Shape,
        elevation: AMElevation,
        fill: AMSurfaceFill,
        hairline: Color,
        dark: Boolean,
        backdrop: AMBackdropSource?,
        clipContent: Boolean,
    ) {
        val level = elevation.shadowLevel
        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawSurface)) }

        if (fill == AMSurfaceFill.Frosted && backdrop != null) {
            clipPath(path, ClipOp.Difference) { shadows.draw(this, context, shape, level, dark) }
            val radius = Style.Frosted.BlurRadius.toPx()
            if (slice.draw(this, path, backdrop, coordinates, radius)) {
                drawPath(path, if (dark) Style.Frosted.FillDark else Style.Frosted.FillLight)
                drawClippedContent(path, clipContent)
                drawInsetOutline(shape, if (dark) Style.Frosted.EdgeDark else Style.Frosted.EdgeLight, Style.Hairline.Width)
                return
            }
        } else {
            shadows.draw(this, context, shape, level, dark)
        }

        // Solid, and frosted with nothing to sample yet.
        drawPath(path, if (dark) Style.Surface.Dark else Style.Surface.Light)
        drawClippedContent(path, clipContent)
        if (dark) drawInsetOutline(shape, if (hairline.isSpecified) hairline else selectionRing(true), Style.Hairline.Width)
    }

    private fun ContentDrawScope.drawClippedContent(shape: Path, clip: Boolean) {
        if (clip) {
            val scope = this
            clipPath(shape) { scope.drawContent() }
        } else {
            drawContent()
        }
    }
}
