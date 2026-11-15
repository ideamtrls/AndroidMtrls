package com.ideamtrls.androidmtrls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.withCompositionLocals

/**
 * The app's surface setting: what its floating surfaces are made of. It's set once, on the theme
 * (`AMTheme(surfaceFill = …)`, read with [AMTheme.surfaceFill]); a nested `AMTheme(surfaceFill = …)`
 * sets it for a subtree. The library default is [Solid].
 *
 * A surface standing on the backdrop (a card, tabs, a button on the page) is [Solid] or
 * [Frosted]. A piece embedded in a frosted surface (inside a card, tabs or a section, or on a
 * sheet) draws the tint instead, [AMTheme.Frosted.embeddedFill], so nothing frosts over frost. The
 * backdrop itself (the art, a branded picture, the page) is never the setting's.
 *
 * The fill changes only what the surface paints; its size, corners, padding and ripple are the
 * same either way.
 */
public enum class AMSurfaceFill {
    /**
     * The opaque floating surface ([AMTheme.Surface.primary]) with its shadow, and a hairline in
     * dark mode.
     */
    Solid,

    /**
     * The frosted recipe on the backdrop: the art of the nearest [AMBackdrop] under the surface
     * blurred ([AMTheme.Frosted.blurRadius], a pure Gaussian blur, no tint), [AMTheme.Frosted.fill]
     * over it, the light [AMTheme.Frosted.edge] in both modes, and the shadow outside the shape
     * only. Text on it uses the usual text colors, with [AMTheme.Frosted.secondaryText] for
     * supporting lines, which [AMTheme.secondaryText] gives content lying on it.
     *
     * With no art behind it (no backdrop, or the art not placed yet) it's frost over the page
     * color: the page under the frosted fill, opaque, so it never shows unblurred content. In the
     * remote's and the spotlight's blurred windows the window's own blur is the blur: the fill lies
     * over it, sampling nothing, and while the system has cross-window blur off the surface draws
     * solid. A piece embedded in it draws the tint ([AMTheme.Frosted.embeddedFill]) with the same
     * edge and no blur of its own.
     */
    Frosted,
}

/**
 * Marks [content] as lying on a floating surface (a hand-built `Modifier.amCard` card, which,
 * being a modifier, can't say so itself): the secondary text inside resolves from the surface
 * ([AMTheme.secondaryText]), the frosted secondary in a frosted app; a frosted piece with its own
 * shape inside is embedded and draws the tint; and nothing inside lies on art any more
 * ([AMTheme.artTone] is null), so its text follows the mode. The fill itself is always the app's
 * setting ([AMTheme.surfaceFill]). The library's containers (sections, disclosure groups, list
 * cards, tabs) do this for their content themselves.
 *
 * ```kotlin
 * Column(Modifier.amCard()) {
 *     ProvideAMOnSurface { TrackRows() }   // their lengths in AMTheme.secondaryText
 * }
 * ```
 *
 * Put it inside the card, around what the card holds, never around the card itself: the card
 * stands where it's placed.
 *
 * @param content what lies on the surface.
 */
@Composable
public fun ProvideAMOnSurface(content: @Composable () -> Unit): Unit = readOnSurface(content)

/**
 * [content]'s result as content lying on a surface reads it ([ProvideAMOnSurface]), for a value a
 * container works out for what it holds while the container's own node, which carries the
 * surface, stays where it's placed: a list card's row colors, handed to the `ListItem` whose node
 * is the card.
 */
@Composable
internal inline fun <T> readOnSurface(content: @Composable () -> T): T =
    withCompositionLocals(LocalAMLiesOn provides LiesOn.Surface, LocalAMArtTone provides null, content = content)

/**
 * The app's surface setting, provided only by [AMTheme]: [AMSurfaceFill.Solid] at the root.
 * Dynamic, so modifier nodes that read it while drawing redraw on a change.
 */
internal val LocalAMSurfaceFill = compositionLocalOf { AMSurfaceFill.Solid }
