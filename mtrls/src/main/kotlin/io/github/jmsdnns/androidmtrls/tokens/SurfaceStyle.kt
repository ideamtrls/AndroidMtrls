package io.github.jmsdnns.androidmtrls.tokens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues

// ---------------------------------------------------------------------------
// AMSurfaceStyle — visual treatment enum
// ---------------------------------------------------------------------------

/**
 * Visual treatment applied by surface-bearing containers to themselves.
 *
 * [Elevated] is the default at the root of a screen: the container renders
 * with a rounded fill, corner radius, two-layer drop shadow, and a dark-mode
 * hairline — the "floating surface on page" recipe.
 *
 * [Flat] is the nested mode: no fill, no rounded corners, no shadow, no
 * hairline. `AMCard` writes [Flat] into [LocalAMSurfaceStyle] for its content
 * slot, so any card nested inside another card automatically demotes itself.
 * Shadow-on-shadow stacking and double padding never occur.
 *
 * Widgets (text inputs, picker rows, list rows) do **not** read this value —
 * they have a single flat rendering and expect a container to supply the
 * surface. Override per-subtree with [AMSurfaceStyleProvider] only to flatten
 * or re-lift a *container* primitive.
 *
 * iOS equivalent: `IMSurfaceStyle` in `Sources/Tokens/SurfaceStyle.swift`.
 */
@Immutable
enum class AMSurfaceStyle {
    /**
     * Default at the root of a screen. Container renders as a floating
     * surface: rounded fill, `AMElevation.Low` shadow, and (dark mode only)
     * a hairline. Sits over [AMTheme.Surface.page].
     */
    Elevated,

    /**
     * Nested container mode. No fill, no shape, no shadow, no hairline.
     * Inherited automatically inside `AMCard` so containers never double-pad
     * or stack shadows when one lives inside another.
     */
    Flat
}

// ---------------------------------------------------------------------------
// LocalAMSurfaceStyle — CompositionLocal for surface nesting
// ---------------------------------------------------------------------------

/**
 * `CompositionLocal` that propagates the active [AMSurfaceStyle] through the
 * composition tree.
 *
 * The default at the root of every screen is [AMSurfaceStyle.Elevated]. `AMCard`
 * writes [AMSurfaceStyle.Flat] into this local for its content slot so nested
 * cards automatically demote themselves. Custom container primitives that
 * participate in the same nesting contract should read and write this local
 * via [AMSurfaceStyleProvider].
 *
 * **Why `staticCompositionLocalOf`:** surface-style overrides are infrequent,
 * structural changes (a card being added/removed), not frame-by-frame state.
 * `staticCompositionLocalOf` causes full recomposition of all consumers on
 * change, which is acceptable given the low frequency and provides better
 * skip-optimization for stable subtrees.
 *
 * Usage inside a composable:
 * ```kotlin
 * val surfaceStyle = LocalAMSurfaceStyle.current
 * val elevation = if (surfaceStyle == AMSurfaceStyle.Elevated) {
 *     AMElevation.Low
 * } else {
 *     AMElevation.None
 * }
 * ```
 */
val LocalAMSurfaceStyle = staticCompositionLocalOf<AMSurfaceStyle> { AMSurfaceStyle.Elevated }

// ---------------------------------------------------------------------------
// AMSurfaceStyleProvider — public override API
// ---------------------------------------------------------------------------

/**
 * Override the inherited surface style for a composable subtree.
 *
 * Provides [style] as [LocalAMSurfaceStyle] for [content] and all its
 * descendants. Use this to flatten or re-lift a container primitive, or to
 * opt a custom container into the same nesting contract as `AMCard`.
 *
 * ```kotlin
 * // Force a card to stay elevated even when nested:
 * AMSurfaceStyleProvider(AMSurfaceStyle.Elevated) {
 *     AMCard { … }
 * }
 * ```
 *
 * iOS equivalent: `View.imSurfaceStyle(_:)` in `Sources/Tokens/SurfaceStyle.swift`.
 *
 * @param style   The surface style to install for [content] and its descendants.
 * @param content The composition subtree that will inherit [style].
 */
@Composable
fun AMSurfaceStyleProvider(
    style: AMSurfaceStyle,
    content: @Composable () -> Unit
) = CompositionLocalProvider(LocalAMSurfaceStyle provides style, content = content)

// ---------------------------------------------------------------------------
// AMPadding — static EdgeInsets presets
// ---------------------------------------------------------------------------

/**
 * Static padding presets ported from `EdgeInsets` extensions in
 * `Sources/Tokens/SurfaceStyle.swift`. All values are locked to `AMTheme`
 * constants — they cannot be overridden.
 *
 * For token-derived (overridable) padding that reads `LocalAMTokens`, use
 * `AMTokens.rowPadding`, `AMTokens.blockPadding`, etc. (defined in
 * `Tokens.kt`) once that file is available.
 *
 * | Preset    | Horizontal       | Vertical           | iOS equivalent   |
 * |-----------|------------------|--------------------|------------------|
 * | [row]     | 16dp (`lg`)      | 12dp (`md`)        | `.imRow`         |
 * | [block]   | 16dp (`lg`)      | 16dp (`lg`)        | `.imBlock`       |
 * | [listRow] | 14dp (`Input.inset`) | 16dp (`lg`)    | `.imListRow`     |
 * | [titleBar]| 14dp (`Input.inset`) | 10dp (literal) | `.imTitleBar`    |
 */
object AMPadding {

    /**
     * Standard card-row interior padding: 16dp horizontal, 12dp vertical.
     *
     * Use as the `padding:` argument of `AMCard(padding = AMPadding.row)` for
     * custom row-style content inside a card. Distinct from [listRow] (14h/16v)
     * which is for primitives that ARE their own full-bleed surface.
     *
     * iOS equivalent: `EdgeInsets.imRow` — horizontal `.lg`, vertical `.md`.
     */
    val row: PaddingValues = PaddingValues(
        horizontal = AMTheme.Spacing.lg.dp,
        vertical   = AMTheme.Spacing.md.dp
    )

    /**
     * Uniform block card padding at 16dp on all edges.
     *
     * For prose or block cards (section containers, free-text blocks) where
     * horizontal and vertical padding should match.
     *
     * iOS equivalent: `EdgeInsets.imBlock` — all edges `.lg`.
     */
    val block: PaddingValues = PaddingValues(all = AMTheme.Spacing.lg.dp)

    /**
     * Full-bleed list-row padding for primitives that are their own surface.
     *
     * Horizontal matches `AMTheme.Input.inset` (14dp) so a stacked input and
     * list row align at their leading edges. Vertical is 16dp for the
     * breathing room a tappable row needs.
     *
     * iOS equivalent: `EdgeInsets.imListRow`.
     */
    val listRow: PaddingValues = PaddingValues(
        horizontal = AMTheme.Input.inset.dp,
        vertical   = AMTheme.Spacing.lg.dp
    )

    /**
     * Title-strip padding for `AMPanel` and `AMToolbar` headers.
     *
     * Tighter vertical (10dp literal — not a spacing token, matches iOS
     * literal `10`) than [listRow] so the strip reads as a header. Horizontal
     * matches [listRow] so a header above a list shares the same gutter.
     *
     * iOS equivalent: `EdgeInsets.imTitleBar`.
     */
    val titleBar: PaddingValues = PaddingValues(
        horizontal = AMTheme.Input.inset.dp,
        vertical   = 10.dp
    )
}

// ---------------------------------------------------------------------------
// Previews — light + dark showing padding presets applied to labeled boxes
// ---------------------------------------------------------------------------

@Preview(
    name = "AMSurfaceStyle & AMPadding · Light",
    group = "AMSurfaceStyle",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7  // Surface.page light
)
@Composable
private fun AMSurfaceStylePreviewLight() {
    AMThemeRoot {
        AMSurfaceStylePreviewContent()
    }
}

@Preview(
    name = "AMSurfaceStyle & AMPadding · Dark",
    group = "AMSurfaceStyle",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,  // Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AMSurfaceStylePreviewDark() {
    AMThemeRoot {
        AMSurfaceStylePreviewContent()
    }
}

@Composable
private fun AMSurfaceStylePreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val pageColor = AMTheme.Surface.page.resolve(isDark)
    val cardColor = AMTheme.Surface.primary.resolve(isDark)
    val labelColor = AMTheme.Text.primary.resolve(isDark)

    val labelStyle = AMTheme.Type.caption.toTextStyle(labelColor)

    Column(
        modifier = Modifier
            .background(pageColor)
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp)
    ) {
        // Display current surface style
        BasicText(
            text = "Surface style: ${LocalAMSurfaceStyle.current.name}",
            style = labelStyle
        )

        // row preset
        PaddingPreviewBox(
            label = "AMPadding.row  (16h / 12v)",
            padding = AMPadding.row,
            cardColor = cardColor,
            labelStyle = labelStyle
        )

        // block preset
        PaddingPreviewBox(
            label = "AMPadding.block  (16 all)",
            padding = AMPadding.block,
            cardColor = cardColor,
            labelStyle = labelStyle
        )

        // listRow preset
        PaddingPreviewBox(
            label = "AMPadding.listRow  (14h / 16v)",
            padding = AMPadding.listRow,
            cardColor = cardColor,
            labelStyle = labelStyle
        )

        // titleBar preset
        PaddingPreviewBox(
            label = "AMPadding.titleBar  (14h / 10v)",
            padding = AMPadding.titleBar,
            cardColor = cardColor,
            labelStyle = labelStyle
        )

        // Demonstrate Flat style via AMSurfaceStyleProvider
        AMSurfaceStyleProvider(AMSurfaceStyle.Flat) {
            BasicText(
                text = "Inside AMSurfaceStyleProvider(Flat) → ${LocalAMSurfaceStyle.current.name}",
                style = labelStyle
            )
        }
    }
}

/** Renders a labeled box that shows visible padding insets. */
@Composable
private fun PaddingPreviewBox(
    label: String,
    padding: PaddingValues,
    cardColor: Color,
    labelStyle: TextStyle
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = cardColor.copy(alpha = 0.3f),
                shape = amSquircle(AMTheme.Radius.md.dp)
            )
            .padding(padding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(
                    color = cardColor,
                    shape = amSquircle(AMTheme.Radius.sm.dp)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicText(
                text = label,
                style = labelStyle,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}
