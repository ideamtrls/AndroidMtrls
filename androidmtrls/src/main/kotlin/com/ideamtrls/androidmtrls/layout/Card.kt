package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

/**
 * Makes this content a floating card: the surface fill in [shape], the shape's two-layer shadow at
 * [elevation], and in dark mode a hairline, with the content inset by [padding] and clipped to the
 * shape.
 *
 * ```kotlin
 * Column(Modifier.fillMaxWidth().amCard()) {
 *     Text("Project Atlas", Modifier.amRowPadding())
 *     AMDivider(inset = AMTheme.tokens.spacing.lg)
 *     Text("Harbor survey", Modifier.amRowPadding())
 * }
 * Text(story, Modifier.fillMaxWidth().amCard(padding = PaddingValues(AMTheme.tokens.spacing.lg)))
 * ```
 *
 * A card of rows takes no padding: every row carries the row recipe ([amRowPadding]), so its text
 * sits at the row inset. A block of prose or a composite block takes `spacing.lg` all round.
 *
 * **Nesting.** A card inside another card draws nothing of its own, whatever its [fill]: no fill,
 * shadow, hairline, clip or [padding]. Its content lies on the outer card, whose own padding is
 * the only inset, so shadows never stack and padding never doubles. [ProvideAMSurfaceStyle] with
 * [AMSurfaceStyle.Elevated] lifts a card inside a card again; [AMSurfaceStyle.Flat] flattens a
 * card anywhere.
 *
 * **Frosted.** With [AMSurfaceFill.Frosted] the card is glass over the art of the nearest
 * [AMBackdrop]; with no backdrop behind it, it draws solid. Put supporting text on it in
 * [AMTheme.Frosted.secondaryText].
 *
 * Order it after the modifiers that size the card and before the content's own: `Modifier
 * .fillMaxWidth().amCard()`. The card adds no click handling; a clickable card puts its
 * `clickable` after `amCard`, so the ripple stays inside the shape.
 *
 * @param fill solid (the default) or frosted over art.
 * @param elevation the card's shadow: [AMElevation.Low] by default, [AMElevation.None] for a card
 *   that keeps its fill and corners but casts nothing.
 * @param padding the space between the card's edge and its content. None by default.
 * @param shape the card's shape: the theme's `shapes.large` by default.
 */
@Composable
@ReadOnlyComposable
public fun Modifier.amCard(
    fill: AMSurfaceFill = AMSurfaceFill.Solid,
    elevation: AMElevation = AMElevation.Low,
    padding: PaddingValues = PaddingValues(0.dp),
    shape: Shape = MaterialTheme.shapes.large,
): Modifier = this then CardSurfaceElement(fill, elevation, shape) then CardPaddingElement(padding)

// The card is two nodes, the surface and then the padding, like a background before a padding.
// Compose draws a node that also lays out inside its own layout, at its content's bounds, so a
// single node would paint the surface inside the padding, hugging the content.

private data class CardSurfaceElement(
    val fill: AMSurfaceFill,
    val elevation: AMElevation,
    val shape: Shape,
) : ModifierNodeElement<CardSurfaceNode>() {
    override fun create() = CardSurfaceNode(fill, elevation, shape)

    override fun update(node: CardSurfaceNode) {
        node.fill = fill
        node.elevation = elevation
        node.shape = shape
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "amCard"
        properties["fill"] = fill
        properties["elevation"] = elevation
        properties["shape"] = shape
    }
}

/**
 * The card's surface: the elevated surface drawn at the card's full size, padding included, while
 * the card is [AMSurfaceStyle.Elevated]. It's the card's [NestingSurfaceNode], so a card below it
 * in the layout tree demotes itself.
 *
 * The card's padding node settles the style while it measures ([styleForLayout]), and the surface
 * draws the style the layout was measured with, so the padding and the surface always agree. The
 * style's inputs (the surface-style locals here and at the card above) are read inside that
 * measure, so a change to them measures the card again, and a changed style redraws it.
 */
private class CardSurfaceNode(
    var fill: AMSurfaceFill,
    var elevation: AMElevation,
    var shape: Shape,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    NestingSurfaceNode {

    private val surface = ElevatedSurfaceDrawer()

    /** The style the card was last measured with; null until it's measured. */
    private var measuredStyle: AMSurfaceStyle? = null

    /** Resolves the card's style for its padding node's measure, and redraws when it changed. */
    fun styleForLayout(): AMSurfaceStyle {
        val style = resolveSurfaceStyle()
        if (style != measuredStyle) {
            measuredStyle = style
            invalidateDraw()
        }
        return style
    }

    override fun onAttach() = surface.attach(requireGraphicsContext())

    override fun onDetach() {
        surface.detach(requireGraphicsContext())
        measuredStyle = null
    }

    // A frosted card that moves over its art (a scroll) samples a different slice, so it redraws.
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (surface.position(coordinates, backdrop())) invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        if ((measuredStyle ?: resolveSurfaceStyle()) == AMSurfaceStyle.Flat) {
            drawContent()
            return
        }
        with(surface) {
            drawSurface(
                context = requireGraphicsContext(),
                shape = shape,
                elevation = elevation,
                fill = fill,
                hairline = Color.Unspecified,
                dark = currentValueOf(LocalAMDarkTheme) ?: false,
                backdrop = backdrop(),
                clipContent = true,
            )
        }
    }

    private fun backdrop(): AMBackdropSource? =
        if (fill == AMSurfaceFill.Frosted) currentValueOf(LocalAMBackdrop) else null
}

private data class CardPaddingElement(val padding: PaddingValues) : ModifierNodeElement<CardPaddingNode>() {
    override fun create() = CardPaddingNode(padding)

    override fun update(node: CardPaddingNode) {
        node.padding = padding
        node.invalidateMeasurement()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "amCardPadding"
        properties["padding"] = padding
    }
}

/**
 * The card's padding: the content inset by the padding while the card is elevated, and not at all
 * when it's flat. It asks the card's own surface node, the nearest nesting surface above it in the
 * modifier chain, which style to lay out.
 */
private class CardPaddingNode(var padding: PaddingValues) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val card = findNearestAncestor(NestingSurfaceKey) as? CardSurfaceNode
        val elevated = card == null || card.styleForLayout() == AMSurfaceStyle.Elevated
        val insets = if (elevated) padding else NoPadding
        val left = insets.calculateLeftPadding(layoutDirection).roundToPx()
        val right = insets.calculateRightPadding(layoutDirection).roundToPx()
        val top = insets.calculateTopPadding().roundToPx()
        val bottom = insets.calculateBottomPadding().roundToPx()
        val placeable = measurable.measure(constraints.offset(-(left + right), -(top + bottom)))
        val width = constraints.constrainWidth(placeable.width + left + right)
        val height = constraints.constrainHeight(placeable.height + top + bottom)
        return layout(width, height) { placeable.place(left, top) }
    }
}

private val NoPadding = PaddingValues(0.dp)

/**
 * Pads a hand-built row to the row recipe, the one rhythm every row shares, so it lines up with
 * the library's rows and with Material list items in the same card or strip.
 *
 * The recipe is padding, not a height: the spacing scale's `lg` (16 dp) at the sides and
 * [AMTheme.rowVerticalPadding] (28 dp) above and below, across the full width. A row is its
 * tallest content plus that padding: one line of `bodyLarge` makes an 80 dp row, a `bodyLarge`
 * title over a `bodyMedium` line a 100 dp row, and a switch or a picture makes it taller by its own
 * height. It draws no surface: put rows in a bare [amCard], or on a strip in
 * [AMTheme.Surface.primary].
 *
 * ```kotlin
 * Column(Modifier.amRowPadding()) {
 *     Text("Weekly report")
 *     Text("Sent every Monday at 9:00", style = MaterialTheme.typography.bodyMedium)
 * }
 * ```
 *
 * A row built on Material's `ListItem` takes the same recipe through
 * [AMRowDefaults.contentPadding]. Inside a nested card the padding still applies: it is the
 * row's own inset, not the card's.
 */
public fun Modifier.amRowPadding(): Modifier = this.fillMaxWidth() then RowPaddingElement

private data object RowPaddingElement : ModifierNodeElement<RowPaddingNode>() {
    override fun create() = RowPaddingNode()

    override fun update(node: RowPaddingNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "amRowPadding"
    }
}

private class RowPaddingNode : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val side = currentValueOf(LocalAMTokens).spacing.lg.roundToPx()
        val vertical = Style.Row.VerticalPadding.roundToPx()
        val placeable = measurable.measure(constraints.offset(-2 * side, -2 * vertical))
        val width = constraints.constrainWidth(placeable.width + 2 * side)
        val height = constraints.constrainHeight(placeable.height + 2 * vertical)
        return layout(width, height) { placeable.placeRelative(side, vertical) }
    }
}

/**
 * The row recipe for rows built on Material's `ListItem`, kept for its semantics, ripple and
 * slots. Pass all three, so the list item lies flat on its card or strip at the recipe's rhythm:
 *
 * ```kotlin
 * ListItem(
 *     onClick = ::openReport,
 *     supportingContent = { Text("Sent every Monday") },
 *     colors = AMRowDefaults.colors(),
 *     shapes = AMRowDefaults.shapes(),
 *     contentPadding = AMRowDefaults.contentPadding,
 * ) { Text("Weekly report") }
 * ```
 */
public object AMRowDefaults {

    /**
     * The row recipe as a list item's content padding: `spacing.lg` at the sides and
     * [AMTheme.rowVerticalPadding] above and below. The list item takes its padding out of its own
     * minimum height, so nothing stacks: one line makes an 80 dp row, two lines a 100 dp row, and
     * taller content a taller row.
     */
    public val contentPadding: PaddingValues
        @Composable @ReadOnlyComposable get() =
            PaddingValues(horizontal = AMTheme.tokens.spacing.lg, vertical = AMTheme.rowVerticalPadding)

    /**
     * Material's list item colors on a transparent container in every state but dragging, so the
     * row lies on its card or strip. A checked or selected row keeps its resting colors: its
     * control (a switch, a radio button) shows the state, not a tinted row.
     */
    @Composable
    public fun colors(): ListItemColors {
        val colors = ListItemDefaults.colors()
        return colors.copy(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            selectedContainerColor = Color.Transparent,
            selectedContentColor = colors.contentColor,
            selectedLeadingContentColor = colors.leadingContentColor,
            selectedTrailingContentColor = colors.trailingContentColor,
            selectedOverlineContentColor = colors.overlineContentColor,
            selectedSupportingContentColor = colors.supportingContentColor,
        )
    }

    /**
     * Material's list item shapes made square at rest, pressed, focused, hovered and selected, so
     * a row never rounds into a pill of its own; a card's clip rounds the rows at its corners. A
     * dragged row keeps Material's shape.
     */
    @Composable
    public fun shapes(): ListItemShapes = ListItemDefaults.shapes().copy(
        shape = RectangleShape,
        selectedShape = RectangleShape,
        pressedShape = RectangleShape,
        focusedShape = RectangleShape,
        hoveredShape = RectangleShape,
    )
}

/**
 * The surface of a block that lies flat on the page (a section's flat block): a full-width fill in
 * [AMTheme.Surface.primary] when it stands on the page, and none inside a card, so the card's
 * rounded surface shows instead of a square fill over its corners.
 */
internal fun Modifier.amFlatSurface(): Modifier = this then FlatSurfaceElement

private data object FlatSurfaceElement : ModifierNodeElement<FlatSurfaceNode>() {
    override fun create() = FlatSurfaceNode()

    override fun update(node: FlatSurfaceNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "amFlatSurface"
    }
}

private class FlatSurfaceNode : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    override fun ContentDrawScope.draw() {
        if (resolveSurfaceStyle() == AMSurfaceStyle.Elevated) {
            val dark = currentValueOf(LocalAMDarkTheme) ?: false
            drawRect(if (dark) Style.Surface.Dark else Style.Surface.Light)
        }
        drawContent()
    }
}
