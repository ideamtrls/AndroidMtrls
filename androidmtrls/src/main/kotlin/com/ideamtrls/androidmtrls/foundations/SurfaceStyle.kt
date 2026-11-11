package com.ideamtrls.androidmtrls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor

/**
 * How a surface-bearing container draws itself, so a card nested in a card demotes itself:
 * shadows never stack and padding never doubles.
 *
 * It's [Elevated] at the root of a screen. A card ([Modifier.amCard][amCard]) inside another card
 * draws [Flat] on its own; the library's containers (a section, a disclosure card) also set
 * [Flat] for their content. Controls (fields, pickers, sliders, switches) don't read it: they
 * never draw a surface, and their container supplies one.
 */
public enum class AMSurfaceStyle {
    /**
     * The floating surface on the page: the fill, the corners, the two-layer shadow, and the
     * dark-mode hairline.
     */
    Elevated,

    /** Nested: no fill, no corners, no shadow, no hairline. The surface defers to its parent. */
    Flat,
}

/**
 * The [AMSurfaceStyle] set for the content below: [AMSurfaceStyle.Elevated] unless a container
 * set it. Set it with [ProvideAMSurfaceStyle], which also marks where it was set, so a card
 * re-lifted inside another card knows to draw.
 */
public val LocalAMSurfaceStyle: ProvidableCompositionLocal<AMSurfaceStyle> =
    compositionLocalOf { AMSurfaceStyle.Elevated }

/**
 * Sets the [AMSurfaceStyle] for [content]: [AMSurfaceStyle.Flat] to flatten the containers
 * inside, or [AMSurfaceStyle.Elevated] to lift one again inside a card.
 *
 * ```kotlin
 * ProvideAMSurfaceStyle(AMSurfaceStyle.Elevated) { PinnedNote(Modifier.amCard()) }
 * ```
 *
 * A custom container joins the nesting contract by drawing its surface with
 * [Modifier.amCard][amCard], which demotes inside another card by itself, or by reading
 * [LocalAMSurfaceStyle] and setting [AMSurfaceStyle.Flat] for its own content.
 *
 * @param style the style the surfaces in [content] take.
 * @param content the content it applies to.
 */
@Composable
public fun ProvideAMSurfaceStyle(style: AMSurfaceStyle, content: @Composable () -> Unit) {
    val origin = remember { SurfaceStyleOrigin() }
    CompositionLocalProvider(
        LocalAMSurfaceStyle provides style,
        LocalAMSurfaceStyleOrigin provides origin,
        content = content,
    )
}

/** One [ProvideAMSurfaceStyle] call, told apart from others by identity. */
internal class SurfaceStyleOrigin

/** The [ProvideAMSurfaceStyle] call that set the style below, or null where none did. */
internal val LocalAMSurfaceStyleOrigin = compositionLocalOf<SurfaceStyleOrigin?> { null }

/** The traverse key of a node that draws a surface others nest in: a card. */
internal object NestingSurfaceKey

/**
 * A modifier node that draws a surface other surfaces nest in. A surface below it in the layout
 * tree finds it as its nearest ancestor by [NestingSurfaceKey].
 */
internal interface NestingSurfaceNode : TraversableNode, CompositionLocalConsumerModifierNode {
    override val traverseKey: Any get() = NestingSurfaceKey
}

/**
 * The style a surface drawn by this node shows:
 *
 * - [AMSurfaceStyle.Flat] where [LocalAMSurfaceStyle] says so;
 * - otherwise [AMSurfaceStyle.Flat] inside another card ([NestingSurfaceNode]), unless a
 *   [ProvideAMSurfaceStyle] call sits between that card and this node, which re-lifts it;
 * - [AMSurfaceStyle.Elevated] on the page.
 *
 * Call it only while the node is attached (in measure, layout or draw).
 */
internal fun <T> T.resolveSurfaceStyle(): AMSurfaceStyle
    where T : DelegatableNode, T : CompositionLocalConsumerModifierNode {
    if (currentValueOf(LocalAMSurfaceStyle) == AMSurfaceStyle.Flat) return AMSurfaceStyle.Flat
    val outer = findNearestAncestor(NestingSurfaceKey) as? NestingSurfaceNode ?: return AMSurfaceStyle.Elevated
    val origin = currentValueOf(LocalAMSurfaceStyleOrigin)
    return if (outer.currentValueOf(LocalAMSurfaceStyleOrigin) === origin) AMSurfaceStyle.Flat else AMSurfaceStyle.Elevated
}
