package com.ideamtrls.androidmtrls

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * How high a floating surface sits: a two-layer shadow, a tight near layer close to the shape
 * plus a soft, wide far layer, so the cast feels physical.
 *
 * A shadow is cast by a shape, never by content: [Modifier.amElevation][amElevation] takes the
 * surface's shape, so glyphs and nested controls never cast smudges onto their own card. The one
 * level meant for glyphs, [Text], goes on text with [Modifier.amTextShadow][amTextShadow].
 *
 * Shadows run much stronger in dark mode, so they register on the dark page.
 */
public enum class AMElevation {
    /**
     * No shadow: a surface that keeps its fill and shape but casts nothing, for dense tile grids
     * and horizontal card bands, where per-item shadows stack into noise.
     */
    None,

    /** The default lift: cards, filled buttons, tiles, the toast. */
    Low,

    /** The deeper lift of a sheet, which sits higher off the page than a card. */
    Medium,

    /**
     * Glyph legibility over art and blur: lighter, tighter layers that hold a label's edges over
     * a busy picture. On text, apply it with [Modifier.amTextShadow][amTextShadow].
     */
    Text,
}

/** This level's row in the shadow table. */
internal val AMElevation.shadowLevel: ShadowLevel
    get() = when (this) {
        AMElevation.None -> Style.Shadow.None
        AMElevation.Low -> Style.Shadow.Low
        AMElevation.Medium -> Style.Shadow.Medium
        AMElevation.Text -> Style.Shadow.Text
    }

/**
 * Casts [level]'s two-layer shadow from [shape], under the content: put it on a surface, ahead of
 * the surface's own fill.
 *
 * ```kotlin
 * Box(Modifier.amElevation(AMElevation.Low, MaterialTheme.shapes.large).background(AMTheme.Surface.primary, MaterialTheme.shapes.large))
 * ```
 *
 * The shape casts it, never the content, so what sits on the surface never smudges it. The
 * shadow follows the theme's appearance: much stronger in dark mode.
 *
 * @param level how high the surface sits.
 * @param shape the surface's shape, which casts the shadow.
 */
public fun Modifier.amElevation(level: AMElevation, shape: Shape): Modifier =
    this then ElevationElement(level, shape)

/**
 * The [AMElevation.Text] shadow cast by the glyphs themselves: the content drawn again under
 * itself twice, as the near and far layers, so a label stays legible over a picture.
 *
 * For text and glyphs, not surfaces: a surface's shadow comes from its shape
 * ([Modifier.amElevation][amElevation]).
 */
public fun Modifier.amTextShadow(): Modifier = glyphShadow(Style.Shadow.Text)

/**
 * The glyphs of this content casting [level]'s two layers, drawn in [tint] when it's specified
 * (art text is white), in their own colors otherwise.
 */
internal fun Modifier.glyphShadow(level: ShadowLevel, tint: Color = Color.Unspecified): Modifier =
    this then GlyphShadowElement(level, tint)

// The shape's shadow

private data class ElevationElement(val level: AMElevation, val shape: Shape) : ModifierNodeElement<ElevationNode>() {
    override fun create() = ElevationNode(level, shape)

    override fun update(node: ElevationNode) {
        node.level = level
        node.shape = shape
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "amElevation"
        properties["level"] = level
        properties["shape"] = shape
    }
}

private class ElevationNode(var level: AMElevation, var shape: Shape) :
    Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val shadows = ShapeShadows()

    override fun ContentDrawScope.draw() {
        val dark = currentValueOf(LocalAMDarkTheme) ?: false
        shadows.draw(this, requireGraphicsContext(), shape, level.shadowLevel, dark)
        drawContent()
    }
}

/**
 * A level's two layers cast by a shape, as the platform's drop shadow painters (what
 * `Modifier.dropShadow` draws with), far layer first. Rebuilt only when the shape, the level or
 * the appearance changes.
 */
internal class ShapeShadows {
    private var key: Triple<Shape, ShadowLevel, Boolean>? = null
    private var painters: List<Painter> = emptyList()

    fun draw(scope: DrawScope, context: GraphicsContext, shape: Shape, level: ShadowLevel, dark: Boolean) {
        val key = Triple(shape, level, dark)
        if (key != this.key) {
            this.key = key
            painters = listOf(level.far, level.near)
                .filter { it.alpha(dark) > 0f }
                .map { layer ->
                    context.shadowContext.createDropShadowPainter(
                        shape,
                        Shadow(
                            radius = layer.blur,
                            color = Color.Black.copy(alpha = layer.alpha(dark)),
                            offset = DpOffset(0.dp, layer.offsetY),
                        ),
                    )
                }
        }
        painters.forEach { painter -> with(painter) { scope.draw(scope.size) } }
    }
}

// The glyphs' shadow

private data class GlyphShadowElement(val level: ShadowLevel, val tint: Color) :
    ModifierNodeElement<GlyphShadowNode>() {
    override fun create() = GlyphShadowNode(level, tint)

    override fun update(node: GlyphShadowNode) {
        node.level = level
        node.tint = tint
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glyphShadow"
        properties["level"] = level
        properties["tint"] = tint
    }
}

/**
 * Records the content into a layer, then draws it three times: blurred and offset in black for
 * the far layer and the near layer, then the content itself on top. The shadow passes record a
 * margin around the content, so the blur spreads past its bounds instead of being cut off.
 */
private class GlyphShadowNode(var level: ShadowLevel, var tint: Color) :
    Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private var glyphs: GraphicsLayer? = null
    private val far = ShadowPass()
    private val near = ShadowPass()

    override fun onAttach() {
        val context = requireGraphicsContext()
        glyphs = context.createGraphicsLayer()
        far.layer = context.createGraphicsLayer()
        near.layer = context.createGraphicsLayer()
    }

    override fun onDetach() {
        val context = requireGraphicsContext()
        listOfNotNull(glyphs, far.layer, near.layer).forEach(context::releaseGraphicsLayer)
        glyphs = null
        far.layer = null
        near.layer = null
    }

    override fun ContentDrawScope.draw() {
        val glyphs = glyphs
        if (glyphs == null || size.width <= 0f || size.height <= 0f) {
            drawContent()
            return
        }
        val dark = currentValueOf(LocalAMDarkTheme) ?: false
        glyphs.colorFilter = if (tint.isSpecified) ColorFilter.tint(tint, BlendMode.SrcIn) else null
        glyphs.record { this@draw.drawContent() }
        far.draw(this, glyphs, level.far, dark)
        near.draw(this, glyphs, level.near, dark)
        drawLayer(glyphs)
    }
}

/** One shadow layer of [GlyphShadowNode]: the glyphs again, black, blurred and offset. */
private class ShadowPass {
    var layer: GraphicsLayer? = null
    private var effect: BlurEffect? = null
    private var effectRadius = -1f

    fun draw(scope: DrawScope, glyphs: GraphicsLayer, spec: ShadowLayer, dark: Boolean) = with(scope) {
        val layer = layer ?: return@with
        val alpha = spec.alpha(dark)
        if (alpha <= 0f) return@with
        val radius = spec.blur.toPx()
        if (radius != effectRadius) {
            effectRadius = radius
            effect = if (radius > 0f) BlurEffect(radius, radius, TileMode.Decal) else null
        }
        // The blur reaches about twice its radius; the margin gives it room.
        val margin = ceil(radius * 2f).toInt() + 2
        layer.renderEffect = effect
        layer.colorFilter = ColorFilter.tint(Color.Black.copy(alpha = alpha), BlendMode.SrcIn)
        layer.topLeft = IntOffset(-margin, -margin + spec.offsetY.toPx().roundToInt())
        val recorded = IntSize(ceil(size.width).toInt() + margin * 2, ceil(size.height).toInt() + margin * 2)
        layer.record(recorded) {
            translate(margin.toFloat(), margin.toFloat()) { drawLayer(glyphs) }
        }
        drawLayer(layer)
    }
}
