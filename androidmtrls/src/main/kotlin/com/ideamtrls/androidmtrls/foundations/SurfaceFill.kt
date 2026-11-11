package com.ideamtrls.androidmtrls

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
     * lines.
     *
     * With no backdrop behind it, or before the art is placed, the surface draws [Solid], never
     * unblurred translucency: sharp art under translucent text fails contrast.
     */
    Frosted,
}
