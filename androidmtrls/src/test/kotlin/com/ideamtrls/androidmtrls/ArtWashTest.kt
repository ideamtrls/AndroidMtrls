package com.ideamtrls.androidmtrls

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * The wash toward the mode: where it moves colors, what it keeps, and that the defaults hold the
 * mode's secondary text at 4.5:1 over any art, blends between bands included.
 */
class ArtWashTest {

    @Test
    fun theFloorAndTheCeilingAreTheGraysOfTheWashes() {
        assertEquals(srgbToLinear(0.75f), artWashFloor(0.75f), 1e-6f)
        assertEquals(srgbToLinear(0.32f), artWashCeiling(0.68f), 1e-6f)
        assertEquals(0f, artWashFloor(0f), 0f)
        assertEquals(1f, artWashCeiling(0f), 1e-6f)
    }

    @Test
    fun blackAndWhiteCoversBecomeTheGraysOfTheWashes() {
        // Black in light mode becomes black washed with white at 0.75, white in dark mode white washed with black at 0.68.
        assertEquals(Color(0xFFBFBFBF), washed(Color.Black, dark = false))
        assertEquals(Color(0xFF525252), washed(Color.White, dark = true))
        assertEquals(Color.White, washed(Color.White, dark = false))
        assertEquals(Color.Black, washed(Color.Black, dark = true))
    }

    @Test
    fun movedColorsLandOnTheFloorOrTheCeilingExactly() {
        val floor = artWashFloor(AMArtDefaults.LightWash)
        val ceiling = artWashCeiling(AMArtDefaults.DarkWash)
        Palette.forEach { color ->
            val lifted = linear(color).also { washToLuminance(it, floor, raise = true) }
            assertTrue("$color lifted", luminanceOf(lifted) >= floor - 1e-4f)
            val lowered = linear(color).also { washToLuminance(it, ceiling, raise = false) }
            assertTrue("$color lowered", luminanceOf(lowered) <= ceiling + 1e-4f)
        }
    }

    @Test
    fun colorsOnTheRightSideStay() {
        val yellow = Color(0xFFFFEB3B)
        assertEquals(yellow, washed(yellow, dark = false))
        val navy = Color(0xFF1A237E)
        assertEquals(navy, washed(navy, dark = true))
    }

    @Test
    fun movedColorsKeepTheirHue() {
        // Lowered: scaled down whole, its chromaticity exact. Lifted: its color stays on its side of gray.
        val red = washed(Color.Red, dark = true)
        assertEquals(0f, red.green, 0.01f)
        assertEquals(0f, red.blue, 0.01f)
        val blue = washed(Color(0xFF2F6FD6), dark = false)
        assertTrue(blue.blue > blue.green && blue.green > blue.red)
    }

    @Test
    fun aWashOfNothingLeavesThePictureAsItIs() {
        val pixels = floatArrayOf(0.1f, 0.2f, 0.3f, 1f)
        washTowardMode(pixels, dark = false, wash = 0f)
        assertEquals(listOf(0.1f, 0.2f, 0.3f, 1f), pixels.toList())
    }

    @Test
    fun theDefaultsHoldSecondaryTextOverAnyArtBlendsIncluded() {
        val lightSecondary = luminanceOf(linear(Color(0xFF49454F)))
        val darkSecondary = luminanceOf(linear(Color(0xFFCAC4D0)))
        val lifted = Palette.map { washed(it, dark = false) }
        val lowered = Palette.map { washed(it, dark = true) }
        // Bands blend, and pictures stretch, in sRGB: every mix of two washed colors.
        var darkestLight = 1f
        var brightestDark = 0f
        for (a in lifted.indices) for (b in a + 1 until lifted.size) for (step in 0..40) {
            darkestLight = min(darkestLight, luminanceOf(linear(mix(lifted[a], lifted[b], step / 40f))))
            brightestDark = max(brightestDark, luminanceOf(linear(mix(lowered[a], lowered[b], step / 40f))))
        }
        assertTrue("light: ${contrast(lightSecondary, darkestLight)}", contrast(lightSecondary, darkestLight) >= 4.5f)
        assertTrue("dark: ${contrast(darkSecondary, brightestDark)}", contrast(darkSecondary, brightestDark) >= 4.5f)
        // The ground colored text on light art is checked against is no lighter than the darkest blend.
        assertTrue(Style.ArtInk.LightArtGround <= darkestLight + 1e-3f)
    }

    @Test
    fun colorOnArtIsMovedJustFarEnoughToHold() {
        val error = Color(0xFFB3261E)
        val onLight = artInkHolding(error, AMArtTone.Light)
        assertEquals(4.5f, contrast(luminanceOf(linear(onLight)), Style.ArtInk.LightArtGround), 0.05f)
        assertTrue(onLight.red > onLight.green && onLight.red > onLight.blue)
        val pink = Color(0xFFF2B8B5)
        assertEquals("a color that holds stays", pink, artInkHolding(pink, AMArtTone.Dark))
    }

    @Test
    fun theAccentOnGlassHoldsOverTheWashedArt() {
        // Material's baseline primary already holds on the glass in both modes, and stays.
        val lightPrimary = Color(0xFF6750A4)
        val darkPrimary = Color(0xFFD0BCFF)
        assertEquals(lightPrimary, frostedTint(lightPrimary, dark = false))
        assertEquals(darkPrimary, frostedTint(darkPrimary, dark = true))
        assertTrue(contrast(luminanceOf(linear(lightPrimary)), frostedGround(dark = false)) >= 4.5f)
        assertTrue(contrast(luminanceOf(linear(darkPrimary)), frostedGround(dark = true)) >= 4.5f)
        // A lighter brand accent is moved just far enough, keeping its hue.
        val brand = Color(0xFF7FA8FF)
        val held = frostedTint(brand, dark = false)
        assertEquals(4.5f, contrast(luminanceOf(linear(held)), frostedGround(dark = false)), 0.05f)
        assertTrue(held.blue > held.green && held.green > held.red)
    }

    private fun washed(color: Color, dark: Boolean): Color {
        val pixels = floatArrayOf(color.red, color.green, color.blue, 1f)
        washTowardMode(pixels, dark, if (dark) AMArtDefaults.DarkWash else AMArtDefaults.LightWash)
        return Color(quantize(pixels[0]), quantize(pixels[1]), quantize(pixels[2]))
    }

    private fun quantize(value: Float): Float = Math.round(value.coerceIn(0f, 1f) * 255f) / 255f

    private fun linear(color: Color) = floatArrayOf(srgbToLinear(color.red), srgbToLinear(color.green), srgbToLinear(color.blue))

    private fun luminanceOf(rgb: FloatArray) = linearLuminance(rgb[0], rgb[1], rgb[2])

    private fun mix(a: Color, b: Color, t: Float) = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t)

    private fun contrast(a: Float, b: Float): Float = (max(a, b) + 0.05f) / (min(a, b) + 0.05f)

    private companion object {
        /** Primaries, secondaries, mids and darks, each also saturated as the art saturates them. */
        val Palette: List<Color> = listOf(
            0xFF000000, 0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF,
            0xFF808080, 0xFF2F6FD6, 0xFF1A237E, 0xFF2E9E4F, 0xFFFF8C1A, 0xFF6A2C91, 0xFFFFEB3B, 0xFF7F7F00,
            0xFF004040, 0xFF400040, 0xFF804000, 0xFF202020, 0xFFC0C0C0, 0xFFB03060, 0xFF3C1414, 0xFF14283C,
        ).map { Color(it) }.let { base -> base + base.map(::saturated) }

        /** [color] saturated 2.2× as the art does, through Material's saturation matrix. */
        fun saturated(color: Color): Color {
            val pixels = floatArrayOf(color.red, color.green, color.blue, 1f)
            saturate(pixels, 2.2f)
            return Color(pixels[0], pixels[1], pixels[2])
        }
    }
}
