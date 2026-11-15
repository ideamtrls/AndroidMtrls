package com.ideamtrls.androidmtrls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlin.math.min
import kotlin.math.sqrt

/**
 * How light the art behind something is: what text lying right on the art reads against. Named
 * for the art, so [Dark] art takes light text.
 *
 * The library's art washes toward the mode ([AMArtDefaults]), so by default its tone is the mode's:
 * [Light] in light mode, [Dark] in dark mode. An art surface ([AMBackdrop], [AMArtRowsBackdrop])
 * takes another tone only when the app gives it one, for art it washes less or draws itself, and
 * then everything on that surface reads it: art text ([amArtText]), section headers and footers,
 * empty states, ghost buttons, page headers, panel chrome and the status bar's icons.
 */
public enum class AMArtTone {
    /** Light art: dark text on it. */
    Light,

    /** Dark art: light text on it. */
    Dark,
}

/**
 * The tone of the art under this composition: the nearest art surface's, or null off art. Dynamic,
 * so a node reading it while drawing redraws when it changes.
 */
internal val LocalAMArtTone = compositionLocalOf<AMArtTone?> { null }

/** The tone art has in this composition's mode: light in light mode, dark in dark mode. */
@Composable
@ReadOnlyComposable
internal fun modeArtTone(): AMArtTone = artToneOf(AMTheme.isDark)

/** The tone art washed for a mode has: dark for [dark] mode. */
internal fun artToneOf(dark: Boolean): AMArtTone = if (dark) AMArtTone.Dark else AMArtTone.Light

/**
 * The ink of text lying on art of [tone], whatever the mode or scheme: [Style.ArtInk]'s white and
 * light gray on dark art, black and dark gray on light art ([secondary] takes the gray).
 */
internal fun artInk(tone: AMArtTone, secondary: Boolean): Color = when {
    tone == AMArtTone.Dark -> if (secondary) Style.ArtInk.OnDarkSecondary else Style.ArtInk.OnDark
    else -> if (secondary) Style.ArtInk.OnLightSecondary else Style.ArtInk.OnLight
}

/** The shadow under art text on art of [tone]: black under light text, white under dark text. */
internal fun artInkShadow(tone: AMArtTone): ShadowLevel =
    if (tone == AMArtTone.Dark) Style.Shadow.ArtText else Style.Shadow.ArtTextLight

/**
 * The ink of text lying on the art here: on [tone] when given, else on the nearest art surface's
 * tone, else on the mode's.
 */
@Composable
@ReadOnlyComposable
internal fun artInk(secondary: Boolean = false, tone: AMArtTone? = null): Color =
    artInk(tone ?: LocalAMArtTone.current ?: modeArtTone(), secondary)

/**
 * [color] moved, keeping its hue, just far enough to hold 4.5:1 on the library's art of [tone]:
 * darker on light art (against its darkest, [Style.ArtInk.LightArtGround]), lighter on dark art
 * (against its brightest, [Style.ArtInk.DarkArtCeiling]). A color that already holds stays. For
 * colored text on art, such as a destructive action's red, which holds beside the fixed inks.
 */
internal fun artInkHolding(color: Color, tone: AMArtTone): Color {
    val rgb = floatArrayOf(srgbToLinear(color.red), srgbToLinear(color.green), srgbToLinear(color.blue))
    if (tone == AMArtTone.Light) {
        // At most the luminance that holds 4.5:1 against the light ground.
        moveToLuminance(rgb, (Style.ArtInk.LightArtGround + 0.05f) / MinimumContrast - 0.05f, raise = false)
    } else {
        // At least the luminance that holds 4.5:1 against the dark art's brightest.
        moveToLuminance(rgb, MinimumContrast * (Style.ArtInk.DarkArtCeiling + 0.05f) - 0.05f, raise = true)
    }
    return Color(linearToSrgb(rgb[0]), linearToSrgb(rgb[1]), linearToSrgb(rgb[2]), color.alpha)
}

/** The contrast text holds on art: WCAG's for normal text. */
private const val MinimumContrast = 4.5f

/**
 * Moves one linear-light ink color, [rgb] (red, green, blue, in place), to the luminance [target]
 * when it's on the wrong side of it: below it when [raise], above it otherwise. The color moves
 * along its own line through gray, so its hue holds. Lowered, it's scaled down whole, its
 * chromaticity exact. Raised, its own color (its distance from its gray) grows by the square root
 * of the lift, only as far as the gamut lets it, the rest of the way being gray. Either way it
 * lands on [target] exactly.
 */
internal fun moveToLuminance(rgb: FloatArray, target: Float, raise: Boolean) {
    val luminance = linearLuminance(rgb[0], rgb[1], rgb[2])
    if (if (raise) luminance >= target else luminance <= target) return
    var scale = when {
        !raise -> target / luminance
        luminance > 0f -> sqrt(target / luminance)
        else -> 0f
    }
    for (channel in 0 until 3) {
        val color = rgb[channel] - luminance
        if (color > 0f) scale = min(scale, (1f - target) / color)
        if (color < 0f) scale = min(scale, target / -color)
    }
    for (channel in 0 until 3) rgb[channel] = (target + scale * (rgb[channel] - luminance)).coerceIn(0f, 1f)
}
