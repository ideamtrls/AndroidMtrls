package com.ideamtrls.androidmtrls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

/**
 * How a floating surface fills its shape: the card behind `Modifier.amCard` (and the containers
 * that offer `fill`: sections, disclosure groups, list cards, tabs) and the filled button styles
 * take it, default [Solid], so cards and buttons draw frosted the same way.
 *
 * Solid or frosted is each app's choice, and an app doesn't mix them: its cards, tabs and page
 * buttons are all one or the other. Crucial controls (the +, back over art, the remote's tiles, a
 * held item) are always solid.
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
     * Glass over the art of the nearest [AMBackdrop]: the art under the surface blurred
     * ([AMTheme.Frosted.blurRadius], a pure Gaussian blur, no tint), [AMTheme.Frosted.fill] over
     * it, the light [AMTheme.Frosted.edge] in both modes, and the shadow outside the shape only.
     * Text on it uses the usual text colors, with [AMTheme.Frosted.secondaryText] for supporting
     * lines, which [AMTheme.secondaryText] gives text inside a container drawing it.
     *
     * With no backdrop behind it, or before the art is placed, the surface draws [Solid], never
     * unblurred translucency: sharp art under translucent text fails contrast.
     */
    Frosted,
}

/**
 * Marks [content] as lying on a surface of [fill] (a hand-built `Modifier.amCard(fill = …)` card,
 * which, being a modifier, can't say so itself): the secondary text inside resolves from it
 * ([AMTheme.secondaryText]), the frosted secondary on glass over art, and nothing inside lies on
 * art any more ([AMTheme.artTone] is null), so its text follows the mode. The library's containers
 * that draw a surface from a `fill` (sections, disclosure groups, list cards, tabs) do this for their
 * content themselves.
 *
 * ```kotlin
 * Column(Modifier.amCard(fill = AMSurfaceFill.Frosted)) {
 *     ProvideAMSurfaceFill(AMSurfaceFill.Frosted) { TrackRows() }   // their lengths in AMTheme.secondaryText
 * }
 * ```
 *
 * @param fill the surface the content lies on.
 * @param content what lies on it.
 */
@Composable
public fun ProvideAMSurfaceFill(fill: AMSurfaceFill, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAMSurfaceFill provides fill, LocalAMArtTone provides null, content = content)
}

/** The fill of the surface this composition lies on: [AMSurfaceFill.Solid] (the page, or a solid card) unless a container says otherwise. */
internal val LocalAMSurfaceFill = compositionLocalOf { AMSurfaceFill.Solid }
