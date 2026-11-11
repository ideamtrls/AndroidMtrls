package com.ideamtrls.androidmtrls

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The style layer's raw values: the page, the floating surfaces, their shadows and outlines,
 * the frosted recipe, the remote's tile height and the modal overlay recipe.
 *
 * These values are tuned, and this is the one place each lives. [AMTheme] and [AMElevation] are
 * the public way to read them; no component keeps a literal of its own instead.
 *
 * Each color comes as a light and a dark value. Text and accent colors aren't here: they come
 * from the Material color scheme.
 */
internal object Style {

    /** The tuned page that floating surfaces sit on, a step off the surfaces in both modes. */
    object Page {
        val Light = Color(0xFFF2F2F7)
        val Dark = Color(0xFF141416)
    }

    /**
     * The floating surface: pure white in light mode, pure black in dark mode. Cards, filled
     * buttons, sheets, tiles, the floating action button and the banner fill with it. Its
     * tonal elevation is always 0; it lifts by shadow, never by tint.
     */
    object Surface {
        val Light = Color.White
        val Dark = Color.Black

        /** The dark neutral standing in for art before its picture lands, in both modes. */
        val Art = Color(0xFF333333)
    }

    /**
     * The shadow table: every level's near and far layer, as (blur, y, light alpha, dark alpha).
     * Tune shadows here and nowhere else.
     *
     * A tight near layer sits close to the shape and a soft, wide far layer falls further out,
     * both black. Dark mode runs far stronger, so a black shadow still registers on the
     * near-black page. The text rows stay light in both modes; they keep glyphs legible.
     *
     * The blur is Android's blur radius, which the platform turns into a Gaussian sigma of about
     * `0.58 × blur + 0.5`. The values are set by sigma, the shadow's softness: a sigma of 4 is a
     * blur of about 6, 16 about 27, 28 about 48. They are tuned against rendered cards.
     */
    object Shadow {
        //                                   blur      y      light  dark
        val None = ShadowLevel(
            near = ShadowLayer(0.dp, 0.dp, 0f, 0f),
            far = ShadowLayer(0.dp, 0.dp, 0f, 0f),
        )

        /** The default lift: cards, filled buttons, toolbar buttons, tiles, the toast. */
        val Low = ShadowLevel(
            near = ShadowLayer(6.dp, 2.dp, 0.10f, 0.55f),
            far = ShadowLayer(27.dp, 10.dp, 0.08f, 0.45f),
        )

        /** The deeper lift of a sheet, which sits higher off the page than a card. */
        val Medium = ShadowLevel(
            near = ShadowLayer(10.dp, 4.dp, 0.10f, 0.55f),
            far = ShadowLayer(48.dp, 18.dp, 0.08f, 0.45f),
        )

        /** Glyph legibility over art and blur: headers and marks over a picture. */
        val Text = ShadowLevel(
            near = ShadowLayer(4.dp, 1.dp, 0.12f, 0.12f),
            far = ShadowLayer(16.dp, 4.dp, 0.10f, 0.10f),
        )

        /** White art text on a darkened picture: a tight and a soft layer, stronger than [Text]. */
        val ArtText = ShadowLevel(
            near = ShadowLayer(1.dp, 1.dp, 0.30f, 0.30f),
            far = ShadowLayer(10.dp, 2.dp, 0.28f, 0.28f),
        )
    }

    /**
     * The resting outline of a floating surface. It renders no thinner than one pixel
     * (`max(0.5.dp, 1px)`) and is drawn inset, so it never grows the surface.
     */
    object Hairline {
        val Width = 0.5.dp
    }

    /**
     * The ring around a chosen row or tile, and the default hairline color. Asymmetric on
     * purpose: soft on white in light mode, bright enough to define black on the dark page.
     */
    object SelectionRing {
        val Light = Color(0xFFC7C7CC)
        val Dark = Color(0xFF3A3A3C)
        val Width = 1.5.dp
    }

    /**
     * The dark-mode outline of a solid control inside a black card or sheet, a step brighter than
     * the selection ring so the control still reads as raised. Light mode draws no outline; the
     * light value matches the selection ring.
     */
    object ControlOutline {
        val Light = Color(0xFFC7C7CC)
        val Dark = Color(0xFF636366)
    }

    /**
     * The frosted surface over art: a pure Gaussian blur of what's behind, under a translucent
     * surface fill, with a light edge in both modes. Where there's nothing to blur it draws the
     * solid surface, never unblurred translucency.
     */
    object Frosted {
        /** The fill over the blur: the surface color, translucent. */
        val FillLight = Color.White.copy(alpha = 0.6f)
        val FillDark = Color.Black.copy(alpha = 0.5f)

        /** The outline, in place of the solid surface's dark-mode hairline. */
        val EdgeLight = Color.White.copy(alpha = 0.6f)
        val EdgeDark = Color.White.copy(alpha = 0.15f)

        /** A lifted row on the glass, over its fill. */
        val HighlightLight = Color.White.copy(alpha = 0.5f)
        val HighlightDark = Color.White.copy(alpha = 0.12f)

        /** Secondary text on the glass, which holds its contrast over the tint. */
        val SecondaryTextLight = Color(0xFF2C2C2E)
        val SecondaryTextDark = Color(0xFFD1D1D6)

        /** The blur under the fill, as Android's blur radius: a Gaussian sigma of about 24. */
        val BlurRadius = 40.dp
    }

    /**
     * The row recipe's space above and below a row's content. Rows run taller than Material's
     * list item: one line of `bodyLarge` makes an 80 dp row, a title over a supporting line a
     * 100 dp row. There's no fixed or minimum height; taller content makes a taller row.
     */
    object Row {
        val VerticalPadding = 28.dp
    }

    /** The remote's tiles. */
    object ActionGrid {
        /** Every tile's height, fixed so rows stay even; it doesn't grow with font scale. */
        val TileHeight = 76.dp
    }

    /**
     * The overlay behind the remote, the spotlight and other modal layers: the screen blurs, and
     * in dark mode only, a black dim lies over the blur. The same blur stills a picture in place
     * (`AMBackdropBlur`, `amPausedBackdrop`).
     */
    object ModalOverlay {
        /**
         * The blur, as Android's blur radius: a Gaussian sigma of about 20 (the platform's sigma is
         * about `0.577 × radius + 0.5`), the same softness the frosted recipe's 40 sets for its 24.
         */
        val BlurRadius = 34.dp

        /** The black dim over the blur, in dark mode only. */
        const val DarkDimAlpha = 0.4f

        /**
         * The dim that stands in for the blur while the system has cross-window blur off: Material's
         * scrim strength in light mode, the platform's dialog dim in dark mode.
         */
        const val FallbackDimLight = 0.32f
        const val FallbackDimDark = 0.6f

        /**
         * The page wash over a paused picture (`AMPausedScrim.Page`), in both modes: enough to demote
         * the picture while its color shows through.
         */
        const val PageWashAlpha = 0.55f
    }
}

/**
 * One layer of a shadow: its blur radius, how far it falls below the shape, and the black's
 * alpha in light and in dark mode.
 */
@Immutable
internal data class ShadowLayer(val blur: Dp, val offsetY: Dp, val lightAlpha: Float, val darkAlpha: Float) {
    fun alpha(dark: Boolean): Float = if (dark) darkAlpha else lightAlpha
}

/** One elevation level: its near and far layers. */
@Immutable
internal data class ShadowLevel(val near: ShadowLayer, val far: ShadowLayer)
