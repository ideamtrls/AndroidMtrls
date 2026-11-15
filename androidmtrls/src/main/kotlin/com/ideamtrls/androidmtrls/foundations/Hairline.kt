package com.ideamtrls.androidmtrls

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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
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
 * For outlines that always show on a surface you draw yourself: avatars, a chosen item's ring.
 * The library's floating surfaces draw their own: a solid card or button in dark mode only, where
 * the shadow alone can't lift black off the page; the floating action button, the remote's tiles
 * and the banner in both modes; a frosted one, and a sheet, its frosted edge.
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
 * The elevated surface every floating surface wears (cards, filled buttons, toolbar chips, tiles,
 * the floating action button, the banner), drawn around the content in one place so the recipe
 * never drifts. Its look is the app's surface setting ([AMTheme.surfaceFill], read while it draws)
 * and where the surface stands ([surfaceLook]): solid in a solid app; in a frosted app frosted on
 * the backdrop and, embedded in another surface ([isEmbedded]), the tint, unless it's [lifted].
 *
 * - **Solid**: [elevation]'s shadow cast by [shape], the shape filled with
 *   [AMTheme.Surface.primary], the content, then a hairline: in dark mode only, where the shadow
 *   alone can't lift black off the page, or in both modes with [alwaysOutlined].
 * - **Frosted**: the shadow outside the shape only (under frost it would show through), the
 *   nearest [AMBackdrop]'s art under the shape blurred by [AMTheme.Frosted.blurRadius], the
 *   frosted fill, the content, then the frosted edge in both modes. With no backdrop, or before
 *   the art is placed, the page color takes the art's place under the fill: frost over the page,
 *   opaque, never unblurred translucency. In a blurred window (the remote's, the spotlight's) the
 *   window's own blur-behind is the blur: the fill lies straight over it and samples nothing, and
 *   while the system has cross-window blur off the surface draws solid ([frostPath]).
 * - **Tinted**: the shadow outside the shape only, [AMTheme.Frosted.embeddedFill] in the shape
 *   with no blur of its own, the content, then the frosted edge. A [lifted] surface is never
 *   tinted.
 *
 * It doesn't clip the content. Anything the content draws in its own background (a press wash)
 * sits above the fill.
 *
 * @param hairline the solid look's outline. Unspecified draws [AMTheme.selectionRing]; a solid
 *   control inside a black card or on a sheet passes [AMTheme.controlOutline].
 * @param alwaysOutlined whether the solid look's hairline shows in both modes, for a surface that
 *   always carries one (the floating action button, a tile, the banner). Frosted and tinted
 *   surfaces always draw the frosted edge.
 * @param lifted whether the surface is lifted off what holds it and follows a drag over its
 *   neighbors (a reorderable list's dragged row). It's never the tint, which would let the rows it
 *   passes over show through: in a frosted app it's frosted as on the backdrop even inside a card
 *   or on a sheet, over the art's slice under it or frost over the page color, opaque.
 */
internal fun Modifier.amElevatedSurface(
    shape: Shape,
    elevation: AMElevation = AMElevation.Low,
    hairline: Color = Color.Unspecified,
    alwaysOutlined: Boolean = false,
    lifted: Boolean = false,
): Modifier = amElevatedSurface(shape, elevation.shadowLevel, hairline, alwaysOutlined, lifted)

/**
 * [amElevatedSurface] casting a row of the shadow table that no [AMElevation] names: a shadow
 * fitted to a tight place, such as [Style.Shadow.BarButton] inside a top app bar's clipped row.
 */
internal fun Modifier.amElevatedSurface(
    shape: Shape,
    shadow: ShadowLevel,
    hairline: Color = Color.Unspecified,
    alwaysOutlined: Boolean = false,
    lifted: Boolean = false,
): Modifier = this then ElevatedSurfaceElement(shape, shadow, hairline, alwaysOutlined, lifted)

private data class ElevatedSurfaceElement(
    val shape: Shape,
    val shadow: ShadowLevel,
    val hairline: Color,
    val alwaysOutlined: Boolean,
    val lifted: Boolean,
) : ModifierNodeElement<ElevatedSurfaceNode>() {
    override fun create() = ElevatedSurfaceNode(shape, shadow, hairline, alwaysOutlined, lifted)

    override fun update(node: ElevatedSurfaceNode) {
        node.shape = shape
        node.shadow = shadow
        node.hairline = hairline
        node.alwaysOutlined = alwaysOutlined
        node.lifted = lifted
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "amElevatedSurface"
        properties["shape"] = shape
        properties["shadow"] = shadow
        properties["hairline"] = hairline
        properties["alwaysOutlined"] = alwaysOutlined
        properties["lifted"] = lifted
    }
}

private class ElevatedSurfaceNode(
    var shape: Shape,
    var shadow: ShadowLevel,
    var hairline: Color,
    var alwaysOutlined: Boolean,
    var lifted: Boolean,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode {

    private val surface = ElevatedSurfaceDrawer()

    override fun onAttach() = surface.attach(requireGraphicsContext())

    override fun onDetach() = surface.detach(requireGraphicsContext())

    // A frosted surface that moves over its art (a scroll) samples a different slice, so it redraws.
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (surface.position(coordinates, backdropFor(look()))) invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val look = look()
        with(surface) {
            drawSurface(
                context = requireGraphicsContext(),
                shape = shape,
                shadow = shadow,
                look = look,
                hairline = hairline,
                dark = currentValueOf(LocalAMDarkTheme) ?: false,
                backdrop = backdropFor(look),
                windowBlur = currentValueOf(LocalAMWindowBlur),
                clipContent = false,
                alwaysOutlined = alwaysOutlined,
            )
        }
    }

    /** The look here ([lookOf]); lifted, as on the backdrop wherever it stands, so never the tint. */
    private fun look(): SurfaceLook {
        val fill = currentValueOf(LocalAMSurfaceFill)
        return if (lifted) surfaceLook(fill, embedded = false) else lookOf(fill)
    }
}

/**
 * The elevated surface's drawing, shared by every node that wears it ([Modifier.amElevatedSurface],
 * the card, a control's surface ([amControlSurface])): the shape's shadows, the frosted slice of
 * the art, and where the surface sits. A node owns one, attaches and detaches it with itself,
 * reports its position, and draws the look it resolves through it.
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
     * Draws the surface around the content in [look]: solid, in [solidFill] (the surface color when
     * unspecified); frosted on the path [frostPath] gives it (over [backdrop]'s art, over the page
     * color while there's no art to sample, over the blur of the blurred window it's in, or, in
     * that window while [windowBlur] is false, solid); or tinted. With [clipContent] the content is
     * clipped to [shape]; with [outline] false it draws no hairline or edge. A solid surface's
     * [hairline] (the selection ring when unspecified) shows in dark mode only, or in both modes
     * with [alwaysOutlined].
     *
     * @param windowBlur the blurred window's blur-behind here ([LocalAMWindowBlur]): null outside
     *   a blurred window.
     */
    fun ContentDrawScope.drawSurface(
        context: GraphicsContext,
        shape: Shape,
        shadow: ShadowLevel,
        look: SurfaceLook,
        hairline: Color,
        dark: Boolean,
        backdrop: AMBackdropSource?,
        windowBlur: Boolean?,
        clipContent: Boolean,
        outline: Boolean = true,
        alwaysOutlined: Boolean = false,
        solidFill: Color = Color.Unspecified,
    ) {
        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawSurface)) }
        val frost = frostPathOf(look, backdrop, windowBlur)
        if (look == SurfaceLook.Solid || frost == FrostPath.Solid) {
            shadows.draw(this, context, shape, shadow, dark)
            drawPath(path, solidFill.takeOrElse { if (dark) Style.Surface.Dark else Style.Surface.Light })
            drawClippedContent(path, clipContent)
            if (outline && (dark || alwaysOutlined)) {
                drawInsetOutline(shape, if (hairline.isSpecified) hairline else selectionRing(dark), Style.Hairline.Width)
            }
            return
        }
        // Under frost or the tint the shadow would show through, so it falls outside the shape only.
        clipPath(path, ClipOp.Difference) { shadows.draw(this, context, shape, shadow, dark) }
        drawTranslucentFill(path, frost, dark)
        drawClippedContent(path, clipContent)
        if (outline) drawInsetOutline(shape, frostedEdge(dark), Style.Hairline.Width)
    }

    /**
     * Draws one row's [frame] of a card around the content, for a card a lazy list draws a row at
     * a time: the card at `frame.card`, drawn in [look] as [drawSurface] draws it with the content
     * clipped to its shape, and shown only inside `frame.band`, so slices laid edge to edge make
     * one card.
     */
    fun ContentDrawScope.drawSurfaceSlice(
        context: GraphicsContext,
        frame: CardSlice,
        shape: Shape,
        shadow: ShadowLevel,
        look: SurfaceLook,
        dark: Boolean,
        backdrop: AMBackdropSource?,
        windowBlur: Boolean?,
        outline: Boolean = true,
    ) {
        val card = frame.card
        val path = Path().apply {
            addOutline(shape.createOutline(card.size, layoutDirection, this@drawSurfaceSlice))
            translate(card.topLeft)
        }
        val frost = frostPathOf(look, backdrop, windowBlur)
        val content = this
        // Draws in the card's own frame, at the card's size, so its shadows and edges are the card's.
        fun DrawScope.inCard(block: DrawScope.() -> Unit) =
            inset(card.left, card.top, size.width - card.right, size.height - card.bottom, block)
        clipRect(frame.band.left, frame.band.top, frame.band.right, frame.band.bottom) {
            if (look == SurfaceLook.Solid || frost == FrostPath.Solid) {
                inCard { shadows.draw(this, context, shape, shadow, dark) }
                drawPath(path, if (dark) Style.Surface.Dark else Style.Surface.Light)
                clipPath(path) { content.drawContent() }
                if (outline && dark) inCard { drawInsetOutline(shape, selectionRing(true), Style.Hairline.Width) }
            } else {
                clipPath(path, ClipOp.Difference) { inCard { shadows.draw(this, context, shape, shadow, dark) } }
                drawTranslucentFill(path, frost, dark)
                clipPath(path) { content.drawContent() }
                if (outline) inCard { drawInsetOutline(shape, frostedEdge(dark), Style.Hairline.Width) }
            }
        }
    }

    /**
     * The path a surface of [look] frosts on ([frostPath]), or null when it isn't frosted. Outside
     * a blurred window it places the slice of [backdrop]'s art under the surface first, so
     * [FrostPath.Sampled] means the slice is ready to draw; in a blurred window it samples nothing.
     */
    private fun DrawScope.frostPathOf(
        look: SurfaceLook,
        backdrop: AMBackdropSource?,
        windowBlur: Boolean?,
    ): FrostPath? {
        if (look != SurfaceLook.Frosted) return null
        val placed = windowBlur == null && backdrop != null &&
            slice.place(this, backdrop, coordinates, Style.Frosted.BlurRadius.toPx())
        return frostPath(placed, windowBlur)
    }

    /**
     * The fill of a frosted or tinted surface in [shape]: the tint when it isn't frosted ([frost]
     * null); frosted, the frosted fill over the art's slice blurred, over the page color while
     * there's no art to sample, or straight over a blurred window's blur, which the platform's
     * blur-behind supplies.
     */
    private fun DrawScope.drawTranslucentFill(shape: Path, frost: FrostPath?, dark: Boolean) {
        when (frost) {
            null -> {
                drawPath(shape, if (dark) Style.Frosted.EmbeddedFillDark else Style.Frosted.EmbeddedFillLight)
                return
            }
            FrostPath.Sampled -> slice.drawPlaced(this, shape)
            FrostPath.OverPage -> drawPath(shape, if (dark) Style.Page.Dark else Style.Page.Light)
            // The window's blur-behind is already under it; a solid one draws the solid look instead.
            FrostPath.OverWindowBlur, FrostPath.Solid -> Unit
        }
        drawPath(shape, if (dark) Style.Frosted.FillDark else Style.Frosted.FillLight)
    }

    private fun frostedEdge(dark: Boolean): Color = if (dark) Style.Frosted.EdgeDark else Style.Frosted.EdgeLight

    private fun ContentDrawScope.drawClippedContent(shape: Path, clip: Boolean) {
        if (clip) {
            val scope = this
            clipPath(shape) { scope.drawContent() }
        } else {
            drawContent()
        }
    }
}
