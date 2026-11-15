package com.ideamtrls.androidmtrls

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * The wash toward the mode, a frost: every pixel mixed evenly toward white or black, only where
 * there's picture; and the whole recipe at the defaults (saturated, frosted, saturated again)
 * against the art's measured extremes and the mode's text, blends between bands included.
 */
class ArtWashTest {

    @Test
    fun blackAndWhiteCoversBecomeTheFrostedGrays() {
        // Black in light mode mixed 70 % of the way to white, white in dark mode 70 % of the way to black.
        assertEquals(Color(0xFFB3B3B3), washed(Color.Black, dark = false))
        assertEquals(Color(0xFF4D4D4D), washed(Color.White, dark = true))
        assertEquals(Color.White, washed(Color.White, dark = false))
        assertEquals(Color.Black, washed(Color.Black, dark = true))
    }

    @Test
    fun everyPixelIsMixedEvenly() {
        val light = floatArrayOf(0.2f, 0.4f, 0.6f, 1f).also { washTowardMode(it, dark = false, wash = 0.5f) }
        assertEquals(listOf(0.6f, 0.7f, 0.8f, 1f), light.toList().map { Math.round(it * 1000f) / 1000f })
        val dark = floatArrayOf(0.2f, 0.4f, 0.6f, 1f).also { washTowardMode(it, dark = true, wash = 0.5f) }
        assertEquals(listOf(0.1f, 0.2f, 0.3f, 1f), dark.toList().map { Math.round(it * 1000f) / 1000f })
    }

    @Test
    fun theFrostLandsOnlyWhereThereIsPicture() {
        // Premultiplied: a clear pixel stays clear, a half-clear one takes half the white.
        val clear = floatArrayOf(0f, 0f, 0f, 0f).also { washTowardMode(it, dark = false, wash = 0.7f) }
        assertEquals(listOf(0f, 0f, 0f, 0f), clear.toList())
        val half = floatArrayOf(0f, 0f, 0f, 0.5f).also { washTowardMode(it, dark = false, wash = 0.7f) }
        assertEquals(0.35f, half[0], 1e-6f)
        assertEquals(0.5f, half[3], 0f)
    }

    @Test
    fun frostedColorsKeepTheirHue() {
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
    fun theRecipeStaysWithinTheArtsExtremesBlendsIncluded() {
        val light = Palette.map { recipe(it, dark = false) }
        val dark = Palette.map { recipe(it, dark = true) }
        // Bands blend, and pictures stretch, in sRGB: every mix of two finished colors.
        var darkestLight = 1f
        var brightestDark = 0f
        for (a in light.indices) for (b in a until light.size) for (step in 0..40) {
            darkestLight = min(darkestLight, luminanceOf(linear(mix(light[a], light[b], step / 40f))))
            brightestDark = max(brightestDark, luminanceOf(linear(mix(dark[a], dark[b], step / 40f))))
        }
        assertTrue("light: $darkestLight", Style.ArtInk.LightArtGround <= darkestLight + 1e-3f)
        // The extremes are the art's own: a black picture in light mode, a saturated yellow in dark.
        println("ART-FROST darkest light $darkestLight, brightest dark $brightestDark, black ${recipe(Color.Black, dark = false)}")
        assertEquals(Style.ArtInk.LightArtGround, darkestLight, 2e-3f)
        assertEquals(Style.ArtInk.DarkArtCeiling, brightestDark, 2e-3f)
        assertTrue("dark: $brightestDark", Style.ArtInk.DarkArtCeiling >= brightestDark - 1e-3f)
    }

    @Test
    fun textOnArtTakesTheFixedInkOfItsTone() {
        assertEquals(Color.White, artInk(AMArtTone.Dark, secondary = false))
        assertEquals(Color(0xFFD1D1D6), artInk(AMArtTone.Dark, secondary = true))
        assertEquals(Color.Black, artInk(AMArtTone.Light, secondary = false))
        assertEquals(Color(0xFF2C2C2E), artInk(AMArtTone.Light, secondary = true))
    }

    @Test
    fun theFixedInksHoldOverAnyArt() {
        val ground = Style.ArtInk.LightArtGround
        val ceiling = Style.ArtInk.DarkArtCeiling
        fun on(ink: Color, art: Float) = contrast(luminanceOf(linear(ink)), art)
        assertTrue(on(Color.White, ceiling) >= 7f)
        assertEquals(8.42f, on(Color.White, ceiling), 0.01f)
        assertTrue(on(Color(0xFFD1D1D6), ceiling) >= 4.5f)
        assertEquals(5.54f, on(Color(0xFFD1D1D6), ceiling), 0.01f)
        assertTrue(on(Color.Black, ground) >= 7f)
        assertEquals(9.98f, on(Color.Black, ground), 0.01f)
        assertTrue(on(Color(0xFF2C2C2E), ground) >= 4.5f)
        assertEquals(6.62f, on(Color(0xFF2C2C2E), ground), 0.01f)
    }

    @Test
    fun theFixedSecondaryInksAlreadyHold() {
        assertEquals(Color(0xFFD1D1D6), artInkHolding(Color(0xFFD1D1D6), AMArtTone.Dark))
        assertEquals(Color(0xFF2C2C2E), artInkHolding(Color(0xFF2C2C2E), AMArtTone.Light))
    }

    @Test
    fun inkMovedToALuminanceLandsOnItAndKeepsItsHue() {
        val rgb = linear(Color(0xFFB3261E)).also { moveToLuminance(it, 0.05f, raise = false) }
        assertEquals(0.05f, luminanceOf(rgb), 1e-4f)
        assertTrue(rgb[0] > rgb[1] && rgb[0] > rgb[2])
        val raised = linear(Color(0xFF1A237E)).also { moveToLuminance(it, 0.4f, raise = true) }
        assertEquals(0.4f, luminanceOf(raised), 1e-3f)
        assertTrue(raised[2] > raised[0])
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
    fun theAccentOnFrostHoldsOverTheWashedArt() {
        // Material's baseline primary already holds on the frost in both modes, and stays.
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

    @Test
    fun frostOverThePageIsThePageMadeTranslucent() {
        assertEquals(Color(0xFFFAFAFC), quantized(frostOverPage(dark = false)))
        assertEquals(Color(0xFF0A0A0B), quantized(frostOverPage(dark = true)))
    }

    @Test
    fun textHoldsOnTheTint() {
        for (dark in listOf(false, true)) {
            val secondary = if (dark) Color(0xFFD1D1D6) else Color(0xFF2C2C2E)   // Frosted.secondaryText
            val onSurface = if (dark) Color(0xFFE6E0E9) else Color(0xFF1D1B20)   // baseline onSurface
            assertTrue(contrast(luminanceOf(linear(secondary)), tintedGround(dark)) >= 4.5f)
            assertTrue(contrast(luminanceOf(linear(onSurface)), tintedGround(dark)) >= 7f)
            assertTrue(contrast(luminanceOf(linear(secondary)), luminanceOf(linear(frostOverPage(dark)))) >= 4.5f)
        }
    }

    @Test
    fun textHoldsOnFrostOverAWindowsBlur() {
        assertTrue(contrast(luminanceOf(linear(Color(0xFF2C2C2E))), windowFrostGround(dark = false)) >= 4.5f)
        assertTrue(contrast(luminanceOf(linear(Color(0xFFD1D1D6))), windowFrostGround(dark = true)) >= 4.5f)
    }

    @Test
    fun chipLabelsHold() {
        // Baseline onSurfaceVariant (unselected label), onPrimary on primary (selected).
        assertTrue(contrast(luminanceOf(linear(Color(0xFF49454F))), luminanceOf(linear(Style.Chip.QuietFillLight))) >= 4.5f)
        assertTrue(contrast(luminanceOf(linear(Color(0xFFCAC4D0))), luminanceOf(linear(Style.Chip.QuietFillDark))) >= 4.5f)
        assertTrue(contrast(luminanceOf(linear(Color(0xFF49454F))), tintedGround(dark = false)) >= 4.5f)
        assertTrue(contrast(luminanceOf(linear(Color(0xFFCAC4D0))), tintedGround(dark = true)) >= 4.5f)
        assertTrue(contrast(luminanceOf(linear(Color(0xFF49454F))), frostedGround(dark = false)) >= 4.5f)
        assertTrue(contrast(luminanceOf(linear(Color(0xFFCAC4D0))), frostedGround(dark = true)) >= 4.5f)
        assertTrue(contrast(luminanceOf(linear(Color.White)), luminanceOf(linear(Color(0xFF6750A4)))) >= 4.5f)
        assertTrue(contrast(luminanceOf(linear(Color(0xFF381E72))), luminanceOf(linear(Color(0xFFD0BCFF)))) >= 4.5f)
    }

    @Test
    fun tagLabelsHoldOnFrostAndTint() {
        // Baseline surfaceContainerHighest (the neutral tag) and onSurface.
        for (dark in listOf(false, true)) {
            val container = if (dark) Color(0xFF36343B) else Color(0xFFE6E0E9)
            val label = if (dark) Color(0xFFE6E0E9) else Color(0xFF1D1B20)
            val fill = frostedTagFill(container, dark)
            for (ground in listOf(frostedGround(dark), tintedGround(dark))) {
                assertTrue(contrast(luminanceOf(linear(label)), luminanceOf(over(fill, ground))) >= 4.5f)
            }
        }
    }

    private fun washed(color: Color, dark: Boolean): Color {
        val pixels = floatArrayOf(color.red, color.green, color.blue, 1f)
        washTowardMode(pixels, dark, if (dark) AMArtDefaults.DarkWash else AMArtDefaults.LightWash)
        return Color(quantize(pixels[0]), quantize(pixels[1]), quantize(pixels[2]))
    }

    /** The full recipe at the default blur and washes: saturated, frosted, saturated again. */
    private fun recipe(color: Color, dark: Boolean): Color {
        val pixels = floatArrayOf(color.red, color.green, color.blue, 1f)
        saturate(pixels, 2.2f)
        washTowardMode(pixels, dark, if (dark) AMArtDefaults.DarkWash else AMArtDefaults.LightWash)
        saturate(pixels, if (dark) 1.6f else 1.8f)
        return Color(quantize(pixels[0]), quantize(pixels[1]), quantize(pixels[2]))
    }

    private fun quantize(value: Float): Float = Math.round(value.coerceIn(0f, 1f) * 255f) / 255f

    private fun quantized(color: Color) = Color(quantize(color.red), quantize(color.green), quantize(color.blue), quantize(color.alpha))

    private fun linear(color: Color) = floatArrayOf(srgbToLinear(color.red), srgbToLinear(color.green), srgbToLinear(color.blue))

    private fun luminanceOf(rgb: FloatArray) = linearLuminance(rgb[0], rgb[1], rgb[2])

    private fun mix(a: Color, b: Color, t: Float) = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t)

    /** [fill] composited over a gray of [groundLuminance] in sRGB, as the GPU blends, in linear light. */
    private fun over(fill: Color, groundLuminance: Float): FloatArray {
        val gray = linearToSrgb(groundLuminance)
        return linear(Color(
            fill.red * fill.alpha + gray * (1f - fill.alpha),
            fill.green * fill.alpha + gray * (1f - fill.alpha),
            fill.blue * fill.alpha + gray * (1f - fill.alpha),
        ))
    }

    private fun contrast(a: Float, b: Float): Float = (max(a, b) + 0.05f) / (min(a, b) + 0.05f)

    private companion object {
        /** Primaries, secondaries, mids and darks, each also saturated as the art saturates them. */
        val Palette: List<Color> = listOf(
            0xFF000000, 0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF,
            0xFF808080, 0xFF2F6FD6, 0xFF1A237E, 0xFF2E9E4F, 0xFFFF8C1A, 0xFF6A2C91, 0xFFFFEB3B, 0xFF7F7F00,
            0xFF004040, 0xFF400040, 0xFF804000, 0xFF202020, 0xFFC0C0C0, 0xFFB03060, 0xFF3C1414, 0xFF14283C,
            0xFFF0F000,
        ).map { Color(it) }.let { base -> base + base.map(::saturated) }

        /** [color] saturated 2.2× as the art does, through Material's saturation matrix. */
        fun saturated(color: Color): Color {
            val pixels = floatArrayOf(color.red, color.green, color.blue, 1f)
            saturate(pixels, 2.2f)
            return Color(pixels[0], pixels[1], pixels[2])
        }
    }
}
