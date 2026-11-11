package com.ideamtrls.androidmtrls

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * The theme every AndroidMtrls screen sits in: Material's theme underneath, the style layer on top.
 *
 * It installs [MaterialTheme] with [colorScheme], Material's standard motion scheme and the
 * current type scale and shapes (Material's defaults at the root), so every Material component
 * reads it unchanged, and it provides
 * the style layer's values ([AMTheme]) and [tokens] to everything below.
 *
 * ```kotlin
 * AMTheme { App() }                                       // Material's baseline scheme, following the system
 * AMTheme(colorScheme = if (dark) BrandDark else BrandLight, darkTheme = dark) { App() }
 * AMTheme(tokens = AMTheme.tokens.copy(spacing = AMSpacing(lg = 20.dp))) { Feed() }   // a subtree
 * ```
 *
 * The color scheme is always static: Material's baseline by default, or the app's own. Type,
 * color, shape and motion are overridden the Android way, through [MaterialTheme] (pass a
 * [colorScheme] here, or wrap content in a `MaterialTheme(shapes = …)`); [AMTokens] carries only
 * what Material lacks.
 *
 * Nested inside another `AMTheme`, the defaults keep what the outer theme set: its appearance and,
 * in that appearance, its color scheme. So a subtree that only changes [tokens] keeps the app's
 * colors and a forced light or dark mode.
 *
 * @param darkTheme whether the dark appearance shows. Every style value that differs by mode
 *   follows this flag rather than the system's, so an app can force either appearance.
 * @param colorScheme the static Material color scheme. Defaults to Material's baseline scheme for
 *   [darkTheme]. Never pass a dynamic scheme: the style layer is tuned against static colors.
 * @param tokens the spacing scale and semantic tints.
 * @param content the themed content.
 */
@Composable
public fun AMTheme(
    darkTheme: Boolean = LocalAMDarkTheme.current ?: isSystemInDarkTheme(),
    colorScheme: ColorScheme = inheritedColorScheme(darkTheme),
    tokens: AMTokens = AMTheme.tokens,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.standard(),
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
    ) {
        CompositionLocalProvider(
            LocalAMDarkTheme provides darkTheme,
            LocalAMTokens provides tokens,
            content = content,
        )
    }
}

/**
 * The style layer and the tokens, read inside an [AMTheme]:
 *
 * - **The style tier**, read-only: the floating surface and the page, the frosted recipe, the
 *   hairline, the selection ring and control outline, the row recipe's vertical padding, the
 *   modal overlay recipe and the remote's tile height. These are the look itself; nothing
 *   overrides them. Shadows are read through [AMElevation].
 * - **[tokens]**, the overridable tier: the spacing scale and semantic tints ([AMTokens]).
 *
 * Colors that differ by mode follow the theme's dark flag ([isDark]), not the system's.
 * Outside an `AMTheme` they read as light mode with the default tokens.
 */
public object AMTheme {

    /** The current [AMTokens]: the spacing scale and semantic tints. */
    public val tokens: AMTokens
        @Composable @ReadOnlyComposable get() = LocalAMTokens.current

    /** Whether the theme shows its dark appearance, forced or following the system. */
    public val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalAMDarkTheme.current ?: false

    /** The floating surface and the page it floats on. */
    public object Surface {
        /**
         * The floating surface: pure white in light mode, pure black in dark mode. Cards, filled
         * buttons, sheets, tiles, the floating action button and the banner fill with it, with no
         * tonal tint; they lift by shadow, never by tint.
         */
        public val primary: Color
            @Composable @ReadOnlyComposable get() = if (isDark) Style.Surface.Dark else Style.Surface.Light

        /**
         * The tuned page behind floating surfaces: `#F2F2F7` in light mode, `#141416` in dark
         * mode, far enough above black that black surfaces read as floating. Screens, scaffolds
         * and a scrolled page header use it.
         */
        public val page: Color
            @Composable @ReadOnlyComposable get() = if (isDark) Style.Page.Dark else Style.Page.Light

        /**
         * The dark neutral that stands in for art until its picture lands: `#333333` in both
         * modes, so white art text stays legible over it. Art backdrops and art rows draw it under
         * their wash before the picture arrives; a screen that sits on art puts it behind its
         * transitions so a light page never flashes between pictures.
         */
        public val art: Color
            get() = Style.Surface.Art
    }

    /**
     * The frosted surface over art ([AMSurfaceFill.Frosted]) and the colors content on it uses: a
     * pure Gaussian blur of the art behind, under [fill], outlined by [edge] in both modes.
     */
    public object Frosted {
        /** The fill over the blur: white at 0.6 in light mode, black at 0.5 in dark mode. */
        public val fill: Color
            @Composable @ReadOnlyComposable get() = if (isDark) Style.Frosted.FillDark else Style.Frosted.FillLight

        /** The light edge around the glass: white at 0.6 in light mode, at 0.15 in dark mode. */
        public val edge: Color
            @Composable @ReadOnlyComposable get() = if (isDark) Style.Frosted.EdgeDark else Style.Frosted.EdgeLight

        /** A lifted row on the glass (the playing track), over the fill. */
        public val highlight: Color
            @Composable @ReadOnlyComposable get() = if (isDark) Style.Frosted.HighlightDark else Style.Frosted.HighlightLight

        /** Secondary text on the glass, which holds its contrast over the tint. */
        public val secondaryText: Color
            @Composable @ReadOnlyComposable get() =
                if (isDark) Style.Frosted.SecondaryTextDark else Style.Frosted.SecondaryTextLight

        /** The blur under the fill, as Android's blur radius. */
        public val blurRadius: Dp = Style.Frosted.BlurRadius
    }

    /**
     * The resting outline of a floating surface: half a dp, drawn inset and never thinner than
     * one pixel ([Modifier.amHairline][amHairline]).
     */
    public val hairlineWidth: Dp = Style.Hairline.Width

    /**
     * The selection ring and the default hairline color. Asymmetric on purpose: a soft gray on
     * white in light mode, a dark gray bright enough to define black on the dark page.
     */
    public val selectionRing: Color
        @Composable @ReadOnlyComposable get() = if (isDark) Style.SelectionRing.Dark else Style.SelectionRing.Light

    /** The ring around a chosen row or tile: three hairlines wide, so chosen reads as a quiet hint. */
    public val selectionRingWidth: Dp = Style.SelectionRing.Width

    /**
     * The dark-mode outline of a solid control inside a black card or sheet, a step brighter than
     * [selectionRing] so the control still reads as raised. On the page a control keeps the ring.
     */
    public val controlOutline: Color
        @Composable @ReadOnlyComposable get() = if (isDark) Style.ControlOutline.Dark else Style.ControlOutline.Light

    /**
     * The row recipe's space above and below a row's content: 28 dp. A row is its content plus
     * this above and below and the spacing scale's `lg` at the sides, with no fixed or minimum
     * height, so one line of `bodyLarge` makes an 80 dp row and a title over a supporting line a
     * 100 dp row. [Modifier.amRowPadding][amRowPadding] applies it to a hand-built row, and
     * [AMRowDefaults.contentPadding] to a Material `ListItem`.
     */
    public val rowVerticalPadding: Dp = Style.Row.VerticalPadding

    /**
     * The modal overlay recipe behind the remote and the spotlight: the screen behind blurs, and
     * in dark mode only, a black dim lies over the blur.
     */
    public object Overlay {
        /**
         * The blur behind the overlay window, as Android's blur radius: 34 dp, a Gaussian sigma of
         * about 20. [AMBackdropBlur] and [Modifier.amPausedBackdrop][amPausedBackdrop] blur by it
         * too.
         */
        public val blurRadius: Dp = Style.ModalOverlay.BlurRadius

        /** The black dim over the blur in dark mode. Light mode has none. */
        public const val darkDimAlpha: Float = Style.ModalOverlay.DarkDimAlpha
    }

    /** The remote's grid of tiles. */
    public object ActionGrid {
        /**
         * Every tile's height, so grid rows stay even whatever their labels. Fixed: it doesn't
         * grow with the font scale.
         */
        public val tileHeight: Dp = Style.ActionGrid.TileHeight
    }
}

/**
 * The appearance the nearest [AMTheme] set, or null outside any. Dynamic, so modifier nodes that
 * read it while drawing redraw when the appearance changes.
 */
internal val LocalAMDarkTheme = compositionLocalOf<Boolean?> { null }

/** The tokens the nearest [AMTheme] set. */
internal val LocalAMTokens = compositionLocalOf { AMTokens.Default }

/**
 * The default color scheme for an [AMTheme] in [darkTheme]: the outer theme's scheme when nested in
 * one showing the same appearance, otherwise Material's baseline static scheme.
 */
@Composable
@ReadOnlyComposable
private fun inheritedColorScheme(darkTheme: Boolean): ColorScheme =
    if (LocalAMDarkTheme.current == darkTheme) MaterialTheme.colorScheme else baselineColorScheme(darkTheme)

private val BaselineLight = lightColorScheme()
private val BaselineDark = darkColorScheme()

private fun baselineColorScheme(dark: Boolean): ColorScheme = if (dark) BaselineDark else BaselineLight
