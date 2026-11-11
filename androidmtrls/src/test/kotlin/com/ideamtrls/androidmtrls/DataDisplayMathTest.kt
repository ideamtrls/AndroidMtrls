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
        val a = artKey(ArtSource.Url("https://example.com/a.jpg"), IntSize(1080, 2340), 543f, 2.2f, fade = 0f)
        val b = artKey(ArtSource.Url("https://example.com/a.jpg"), IntSize(1080, 2340), 543f, 2.2f, fade = 0f)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals(220, a.saturation)
        assertNotEquals(a, artKey(ArtSource.Url("https://example.com/b.jpg"), IntSize(1080, 2340), 543f, 2.2f, fade = 0f))
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
            artKey(ArtSource.Url(url), IntSize(100, 100), 0f, 1f, fade = 0f),
            ArtKey(ArtIdentity(url), 50, 50, 0, 100, 0),
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
    fun fadeInRampsTheTopRows() {
        val pixels = FloatArray(1 * 4 * 4) { 1f }
        fadeIn(pixels, width = 1, height = 4, fade = 2f)
        assertEquals(0.25f, pixels[3], 1e-5f)
        assertEquals(0.75f, pixels[7], 1e-5f)
        assertEquals(1f, pixels[11], 0f)
        assertEquals(1f, pixels[15], 0f)
    }

    // The art rows' bands

    @Test
    fun bandsMeetHalfwayAndBleedPastTheEnds() {
        val bands = artRowBands(listOf(ArtSpan(0f, 100f), ArtSpan(200f, 300f)), height = 300f, blend = 50f, bleed = 1000f)
        assertEquals(-1000f, bands[0].top, 0f)
        assertEquals(200f, bands[0].bottom, 0f) // halfway (150) plus the blend
        assertEquals(0f, bands[0].fade, 0f)
        assertEquals(100f, bands[1].top, 0f) // halfway less the blend
        assertEquals(1300f, bands[1].bottom, 0f)
        assertEquals(100f, bands[1].fade, 0f) // twice the blend
    }

    @Test
    fun aLoneRowCoversEverything() {
        val bands = artRowBands(listOf(ArtSpan(40f, 120f)), height = 500f, blend = 50f, bleed = 1000f)
        assertEquals(1, bands.size)
        assertEquals(-1000f, bands[0].top, 0f)
        assertEquals(1500f, bands[0].bottom, 0f)
        assertEquals(0f, bands[0].fade, 0f)
    }

    @Test
    fun aShortBandFadesOverItsWholeHeightAtMost() {
        val bands = artRowBands(
            listOf(ArtSpan(0f, 100f), ArtSpan(100f, 110f), ArtSpan(110f, 200f)),
            height = 200f,
            blend = 50f,
            bleed = 1000f,
        )
        assertEquals(50f, bands[1].top, 0f)
        assertEquals(160f, bands[1].bottom, 0f)
        assertEquals(100f, bands[1].fade, 0f)
        assertEquals(2, artRowBands(listOf(ArtSpan(0f, 0f), ArtSpan(0f, 0f)), 0f, 0f, 0f).count { it.bottom - it.top >= 1f })
    }
}
