package com.ideamtrls.androidmtrls

import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class DataDisplayMathTest {

    // The avatar's initials and hashed hue

    @Test
    fun initialsTakeTheFirstCharacterOfUpToTwoWords() {
        assertEquals("CK", avatarInitials("Christopher Knight"))
        assertEquals("J", avatarInitials("Jane"))
        assertEquals("MH", avatarInitials("Margaret Heafield Hamilton"))
        assertEquals("AL", avatarInitials("  ada   lovelace  "))
    }

    @Test
    fun initialsKeepWholeCharacters() {
        assertEquals("ÉZ", avatarInitials("émile zola"))
        assertEquals("😀S", avatarInitials("😀 smile"))
    }

    @Test
    fun noNameHasNoInitials() {
        assertEquals("", avatarInitials(null))
        assertEquals("", avatarInitials(""))
        assertEquals("", avatarInitials("   "))
    }

    @Test
    fun hueIsDjb2ModuloThreeSixty() {
        assertEquals(341f / 360f, avatarHue(""), 0f) // 5381 mod 360
        assertEquals(158f / 360f, avatarHue("A"), 0f) // 5381 × 33 + 65 = 177638, mod 360
    }

    @Test
    fun hueWrapsAtSixtyFourBitsLikeTheReference() {
        val names = listOf(
            "Grace Hopper",
            "Katherine Johnson",
            "Margaret Heafield Hamilton",
            "Apollo Guidance",
            "émile zola 😀",
            "A name long enough to overflow sixty-four bits many times over, and then some more",
        )
        for (name in names) assertEquals(name, referenceHue(name), avatarHue(name), 0f)
    }

    @Test
    fun hueIsStableAndInRange() {
        val hue = avatarHue("Ada Lovelace")
        assertEquals(hue, avatarHue("Ada Lovelace"), 0f)
        assertTrue(hue >= 0f && hue < 1f)
    }

    /** djb2 over code points in arbitrary precision, reduced mod 2^64 at each step. */
    private fun referenceHue(name: String): Float {
        val modulus = BigInteger.ONE.shiftLeft(64)
        var hash = BigInteger.valueOf(5381)
        name.codePoints().forEach { codePoint ->
            hash = (hash.multiply(BigInteger.valueOf(33)) + BigInteger.valueOf(codePoint.toLong())).mod(modulus)
        }
        return hash.mod(BigInteger.valueOf(360)).toInt() / 360f
    }

    // The timeline's positions

    @Test
    fun timelinePositionsFollowTheIndex() {
        assertEquals(AMTimelinePosition.Only, timelinePosition(0, 1))
        assertEquals(AMTimelinePosition.First, timelinePosition(0, 3))
        assertEquals(AMTimelinePosition.Middle, timelinePosition(1, 3))
        assertEquals(AMTimelinePosition.Last, timelinePosition(2, 3))
        assertEquals(AMTimelinePosition.First, timelinePosition(0, 2))
        assertEquals(AMTimelinePosition.Last, timelinePosition(1, 2))
    }

    // The art's working size, crop and blur

    @Test
    fun heavyBlursWorkSmallAtTheWorkingBlur() {
        val working = artWorkingSize(IntSize(1000, 2000), sigma = 160f)
        assertEquals(50, working.width)
        assertEquals(100, working.height)
        assertEquals(8f, working.sigma, 1e-4f)
        assertEquals(0.05f, working.scale, 1e-6f)
    }

    @Test
    fun lightBlursWorkAtHalfTheFrameAtMost() {
        val light = artWorkingSize(IntSize(1000, 2000), sigma = 4f)
        assertEquals(500, light.width)
        assertEquals(1000, light.height)
        assertEquals(2f, light.sigma, 1e-4f)
        val none = artWorkingSize(IntSize(1000, 2000), sigma = 0f)
        assertEquals(500, none.width)
        assertEquals(0f, none.sigma, 0f)
        val tiny = artWorkingSize(IntSize(10, 10), sigma = 1000f)
        assertEquals(1, tiny.width)
        assertEquals(1, tiny.height)
    }

    @Test
    fun blurRadiusConvertsToThePlatformSigma() {
        assertEquals(0f, blurSigma(0f), 0f)
        assertEquals(20.13f, blurSigma(34f), 0.01f)
        assertEquals(120.01f, blurSigma(207f), 0.01f)
    }

    @Test
    fun keysOfEqualRecipesAreEqual() {
        val url = ArtSource.Url("https://example.com/a.jpg")
        val a = artKey(url, IntSize(1080, 2340), 543f, 2.2f, dark = false, wash = 0.7f, washSaturation = 1.8f, fade = 0f)
        val b = artKey(url, IntSize(1080, 2340), 543f, 2.2f, dark = false, wash = 0.7f, washSaturation = 1.8f, fade = 0f)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals(220, a.saturation)
        assertEquals(70, a.wash)
        assertEquals(180, a.washSaturation)
        assertNotEquals(a, artKey(ArtSource.Url("https://example.com/b.jpg"), IntSize(1080, 2340), 543f, 2.2f, dark = false, wash = 0.7f, washSaturation = 1.8f, fade = 0f))
    }

    @Test
    fun theModeTheWashAndItsSaturationAreInTheKey() {
        val url = ArtSource.Url("https://example.com/a.jpg")
        val light = artKey(url, IntSize(1080, 2340), 543f, 2.2f, dark = false, wash = 0.7f, washSaturation = 1.8f, fade = 0f)
        assertNotEquals(light, artKey(url, IntSize(1080, 2340), 543f, 2.2f, dark = true, wash = 0.7f, washSaturation = 1.8f, fade = 0f))
        assertNotEquals(light, artKey(url, IntSize(1080, 2340), 543f, 2.2f, dark = false, wash = 0.5f, washSaturation = 1.8f, fade = 0f))
        assertNotEquals(light, artKey(url, IntSize(1080, 2340), 543f, 2.2f, dark = false, wash = 0.7f, washSaturation = 1.4f, fade = 0f))
    }

    // An app bitmap's identity, which keys its art pictures

    @Test
    fun identitiesOfTheSameObjectAreEqual() {
        val bitmap = Any()
        assertEquals(ArtIdentity(bitmap), ArtIdentity(bitmap))
        assertEquals(ArtIdentity(bitmap).hashCode(), ArtIdentity(bitmap).hashCode())
    }

    @Test
    fun identitiesOfEqualButDistinctObjectsDiffer() {
        // Two equal strings that are different objects: identity, not equality, decides.
        val first = StringBuilder("cover").toString()
        val second = StringBuilder("cover").toString()
        assertEquals(first, second)
        assertNotEquals(ArtIdentity(first), ArtIdentity(second))
    }

    @Test
    fun anIdentityNeverEqualsAUrl() {
        val url = "https://example.com/a.jpg"
        assertNotEquals(ArtIdentity(url), url)
        assertNotEquals(
            artKey(ArtSource.Url(url), IntSize(100, 100), 0f, 1f, dark = false, wash = 0f, washSaturation = 1f, fade = 0f),
            ArtKey(ArtIdentity(url), 50, 50, 0, 100, dark = false, wash = 0, washSaturation = 100, fade = 0),
        )
    }

    @Test
    fun coverCropIsTheFramesShapeCentered() {
        val wide = coverCrop(sourceWidth = 400, sourceHeight = 200, frameWidth = 100, frameHeight = 100)
        assertEquals(100f, wide.left, 1e-4f)
        assertEquals(0f, wide.top, 1e-4f)
        assertEquals(200f, wide.width, 1e-4f)
        assertEquals(200f, wide.height, 1e-4f)
        val tall = coverCrop(sourceWidth = 300, sourceHeight = 900, frameWidth = 60, frameHeight = 30)
        assertEquals(0f, tall.left, 1e-4f)
        assertEquals(375f, tall.top, 1e-4f)
        assertEquals(300f, tall.width, 1e-4f)
        assertEquals(150f, tall.height, 1e-4f)
    }

    @Test
    fun areaAverageIsPremultipliedMeans() {
        val red = 0xFFFF0000.toInt()
        val blue = 0xFF0000FF.toInt()
        val pixels = areaAverage(intArrayOf(red, blue, red, blue), 2, 2, ArtCrop(0f, 0f, 2f, 2f), 1, 1)
        assertEquals(0.5f, pixels[0], 1e-5f)
        assertEquals(0f, pixels[1], 1e-5f)
        assertEquals(0.5f, pixels[2], 1e-5f)
        assertEquals(1f, pixels[3], 1e-5f)
    }

    @Test
    fun blurKeepsAFlatPictureFlatUpToItsEdges() {
        val width = 5
        val height = 3
        val pixels = FloatArray(width * height * 4) { i -> floatArrayOf(0.2f, 0.4f, 0.6f, 1f)[i % 4] }
        gaussianBlur(pixels, width, height, sigma = 2f)
        for (i in pixels.indices) assertEquals(floatArrayOf(0.2f, 0.4f, 0.6f, 1f)[i % 4], pixels[i], 1e-5f)
    }

    @Test
    fun blurWeighsAnEdgePixelNoMoreThanItsNeighbors() {
        // One red pixel at the left edge of a row of three, blurred far wider than the row: every
        // pixel lands near a third red, the edge one included.
        val pixels = floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        gaussianBlur(pixels, width = 3, height = 1, sigma = 10f)
        for (pixel in 0 until 3) assertEquals(1f / 3f, pixels[pixel * 4], 0.01f)
        for (pixel in 0 until 3) assertEquals(1f, pixels[pixel * 4 + 3], 1e-5f)
    }

    @Test
    fun fadeInRampsTheTopRows() {
        val pixels = FloatArray(1 * 4 * 4) { 1f }
        fadeIn(pixels, width = 1, height = 4, fade = 2f)
        assertEquals(0.25f, pixels[3], 1e-5f)
        assertEquals(0.75f, pixels[7], 1e-5f)
        assertEquals(1f, pixels[11], 0f)
        assertEquals(1f, pixels[15], 0f)
    }

    @Test
    fun flipRowsTurnsThePixelsUpsideDown() {
        // Two pixels a row, three rows: each row's pixels hold its number.
        val pixels = FloatArray(2 * 3 * 4) { it / 8 + 1f }
        flipRows(pixels, width = 2, height = 3)
        assertEquals(3f, pixels[0], 0f)
        assertEquals(3f, pixels[7], 0f)
        assertEquals(2f, pixels[8], 0f)
        assertEquals(1f, pixels[16], 0f)
        assertEquals(1f, pixels[23], 0f)
    }

    @Test
    fun aFlippedBandHasAKeyOfItsOwn() {
        val url = ArtSource.Url("https://example.com/a.jpg")
        val upright = artKey(url, IntSize(1080, 600), 543f, 2.2f, dark = false, wash = 0.7f, washSaturation = 1.8f, fade = 240f)
        val flipped = artKey(url, IntSize(1080, 600), 543f, 2.2f, dark = false, wash = 0.7f, washSaturation = 1.8f, fade = 240f, flipped = true)
        assertNotEquals(upright, flipped)
    }

    // The art rows' bands

    @Test
    fun bandsMeetHalfwayAndRunCopiesPastTheEnds() {
        val bands = artRowBands(listOf(ArtSpan(0f, 100f), ArtSpan(200f, 300f)), height = 300f, blend = 50f, bleed = 1000f)
        assertEquals(5, bands.size)
        // A mirror of the first row, a pitch (its height and its gap) above it, fading in over the page.
        assertBand(bands[0], top = -300f, bottom = 0f, fade = 100f, row = 0, copy = true, flipped = true)
        // The rows: each from halfway to the frame above less the blend to halfway below plus it.
        assertBand(bands[1], top = -100f, bottom = 200f, fade = 100f, row = 0, copy = false, flipped = false)
        assertBand(bands[2], top = 100f, bottom = 400f, fade = 100f, row = 1, copy = false, flipped = false)
        assertBand(bands[3], top = 300f, bottom = 600f, fade = 100f, row = 1, copy = true, flipped = true)
        // Then the page color fades back in and runs the bleed past the end.
        assertBand(bands[4], top = 500f, bottom = 1300f, fade = 100f, row = ArtBand.Blank, copy = false, flipped = false)
    }

    @Test
    fun anEndRowsBandIsInItsOwnFrame() {
        val bands = artRowBands(listOf(ArtSpan(40f, 120f)), height = 500f, blend = 50f, bleed = 1000f)
        val row = bands.single { it.row == 0 && !it.copy }
        assertEquals(-10f, row.top, 0f)
        assertEquals(170f, row.bottom, 0f)
    }

    @Test
    fun copiesCoverTwiceTheBlendFlippedInTurn() {
        // An 80-pixel row needs two copies to cover 100 pixels on each side.
        val bands = artRowBands(listOf(ArtSpan(40f, 120f)), height = 500f, blend = 50f, bleed = 1000f)
        assertEquals(listOf(true, true, false, true, true, false), bands.map { it.copy })
        assertEquals(listOf(false, true, false, true, false, false), bands.map { it.flipped })
        assertEquals(ArtBand.Blank, bands.last().row)
        assertEquals(230f, bands.last().top, 0f)
        assertEquals(1500f, bands.last().bottom, 0f)
        // A short row's copies stop at six a side.
        assertEquals(12, artRowBands(listOf(ArtSpan(0f, 10f)), height = 10f, blend = 50f, bleed = 1000f).count { it.copy })
    }

    @Test
    fun aShortBandFadesOverItsWholeHeightAtMost() {
        val bands = artRowBands(
            listOf(ArtSpan(0f, 100f), ArtSpan(100f, 110f), ArtSpan(110f, 200f)),
            height = 200f,
            blend = 50f,
            bleed = 1000f,
        )
        val middle = bands.single { it.row == 1 && !it.copy }
        assertEquals(50f, middle.top, 0f)
        assertEquals(160f, middle.bottom, 0f)
        assertEquals(100f, middle.fade, 0f)
        assertTrue(artRowBands(listOf(ArtSpan(0f, 0f), ArtSpan(0f, 0f)), 0f, 0f, 0f).all { it.bottom - it.top >= 1f })
        assertTrue(artRowBands(emptyList(), 100f, 50f, 1000f).isEmpty())
    }

    private fun assertBand(band: ArtBand, top: Float, bottom: Float, fade: Float, row: Int, copy: Boolean, flipped: Boolean) {
        assertEquals(top, band.top, 0f)
        assertEquals(bottom, band.bottom, 0f)
        assertEquals(fade, band.fade, 0f)
        assertEquals(row, band.row)
        assertEquals(copy, band.copy)
        assertEquals(flipped, band.flipped)
    }
}
