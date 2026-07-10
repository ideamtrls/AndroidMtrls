package io.github.jmsdnns.androidmtrls.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for AMAvatar pure logic: djb2 hash, hue mapping, and
 * initials extraction.
 *
 * ## djb2 algorithm being tested
 *
 * Replicates Swift `IMAvatar`'s hue hash exactly:
 *   seed = 5381 (UInt64)
 *   for each Unicode scalar in `name.unicodeScalars`:
 *       hash = (hash << 5) + hash + scalar.value   // UInt64 truncating arithmetic
 *   hueIndex = hash % 360
 *
 * Kotlin port: ULong arithmetic, whose natural wrap-around replicates UInt64
 * overflow semantics (no explicit mask). Iteration unit is `String.codePoints()`
 * (Java code points), NOT `.toCharArray()`, so emoji and CJK round-trip as single
 * code points matching Swift's `unicodeScalars`.
 *
 * ## Test vector derivation
 *
 * All expected hashes were computed by hand-tracing the Swift algorithm in Python 3:
 *
 * ```python
 * def djb2(s):
 *     h = 5381
 *     MASK = 0xFFFFFFFFFFFFFFFF
 *     for cp in (ord(c) for c in s):
 *         h = ((h << 5) + h + cp) & MASK
 *     return h
 * ```
 *
 * Python's `ord()` returns Unicode code points (same as Swift's scalar.value and
 * Java's codePoints()), so the derivation is cross-platform faithful.
 *
 * Verified results (name → hash → hash % 360):
 *   "A"                  → 177638           → 158
 *   "AB"                 → 5862120          → 240
 *   "Alice"              → 210668633507     → 227
 *   "Bob"                → 193452632        → 152
 *   "李雷"               → 6771274          → 34
 *   "😀"                 → 306085           → 85
 *   "😀A"               → 10100870         → 350
 *   "Christopher Knight" → 3965916288128951925 → 45
 */
class AvatarTest {

    // -------------------------------------------------------------------------
    // djb2 hash — UInt64 semantics with code-point iteration
    // -------------------------------------------------------------------------

    @Test
    fun `djb2 empty string returns seed 5381`() {
        // Swift: for loop over unicodeScalars of "" — never executes
        // Expected: seed unchanged = 5381
        val hash = avatarDjb2Hash("")
        assertEquals(5381uL, hash)
    }

    @Test
    fun `djb2 single ASCII char A`() {
        // 'A' = Unicode scalar 65
        // hash = (5381 << 5) + 5381 + 65 = 172192 + 5381 + 65 = 177638
        val hash = avatarDjb2Hash("A")
        assertEquals(177638uL, hash)
    }

    @Test
    fun `djb2 two ASCII chars AB`() {
        // After "A": hash = 177638
        // 'B' = 66
        // hash = (177638 << 5) + 177638 + 66 = 5684416 + 177638 + 66 = 5862120
        val hash = avatarDjb2Hash("AB")
        assertEquals(5862120uL, hash)
    }

    @Test
    fun `djb2 name Alice`() {
        // Python-derived: djb2("Alice") = 210668633507
        val hash = avatarDjb2Hash("Alice")
        assertEquals(210668633507uL, hash)
    }

    @Test
    fun `djb2 name Bob`() {
        // Python-derived: djb2("Bob") = 193452632
        val hash = avatarDjb2Hash("Bob")
        assertEquals(193452632uL, hash)
    }

    // -------------------------------------------------------------------------
    // CJK — each CJK character is a single Unicode scalar (same as Swift)
    // -------------------------------------------------------------------------

    @Test
    fun `djb2 CJK two chars Li Lei`() {
        // "李雷": '李' = U+674E = 26446, '雷' = U+96F7 = 38647
        // Python-derived: djb2("李雷") = 6771274
        // Iteration: Java codePoints() gives [26446, 38647] — same as Swift unicodeScalars
        val hash = avatarDjb2Hash("李雷")
        assertEquals(6771274uL, hash)
    }

    // -------------------------------------------------------------------------
    // Emoji — U+1F600 is a single code point (2 UTF-16 chars, 1 Swift scalar)
    // Must iterate .codePoints(), NOT .toCharArray(), to match Swift
    // -------------------------------------------------------------------------

    @Test
    fun `djb2 emoji single grinning face`() {
        // '😀' = U+1F600 = 128512 (single Unicode scalar)
        // hash = (5381 << 5) + 5381 + 128512 = 172192 + 5381 + 128512 = 306085
        // Python-derived: djb2("😀") = 306085
        val hash = avatarDjb2Hash("😀")
        assertEquals(306085uL, hash)
    }

    @Test
    fun `djb2 emoji followed by ASCII`() {
        // "😀A": after '😀' hash = 306085; then 'A' = 65
        // hash = (306085 << 5) + 306085 + 65 = 9794720 + 306085 + 65 = 10100870
        // Python-derived: djb2("😀A") = 10100870
        val hash = avatarDjb2Hash("😀A")
        assertEquals(10100870uL, hash)
    }

    @Test
    fun `djb2 multi-word name Christopher Knight`() {
        // Python-derived: djb2("Christopher Knight") = 3965916288128951925
        val hash = avatarDjb2Hash("Christopher Knight")
        assertEquals(3965916288128951925uL, hash)
    }

    // -------------------------------------------------------------------------
    // Hue index — hash % 360
    // -------------------------------------------------------------------------

    @Test
    fun `hue index A is 158`() {
        // djb2("A") = 177638; 177638 % 360 = 158
        val idx = avatarHueIndex("A")
        assertEquals(158u, idx)
    }

    @Test
    fun `hue index Alice is 227`() {
        // Python-derived: djb2("Alice") % 360 = 227
        val idx = avatarHueIndex("Alice")
        assertEquals(227u, idx)
    }

    @Test
    fun `hue index CJK Li Lei is 34`() {
        // Python-derived: djb2("李雷") % 360 = 34
        val idx = avatarHueIndex("李雷")
        assertEquals(34u, idx)
    }

    @Test
    fun `hue index emoji grinning face is 85`() {
        // Python-derived: djb2("😀") % 360 = 85
        val idx = avatarHueIndex("😀")
        assertEquals(85u, idx)
    }

    @Test
    fun `hue index emoji A is 350`() {
        // Python-derived: djb2("😀A") % 360 = 350
        val idx = avatarHueIndex("😀A")
        assertEquals(350u, idx)
    }

    @Test
    fun `hue index Christopher Knight is 45`() {
        // Python-derived: djb2("Christopher Knight") % 360 = 45
        val idx = avatarHueIndex("Christopher Knight")
        assertEquals(45u, idx)
    }

    // -------------------------------------------------------------------------
    // avatarHue — returns Float in [0f, 1f) mapping index/360f
    // -------------------------------------------------------------------------

    @Test
    fun `avatarHue empty string is seed mod 360 divided by 360`() {
        // seed=5381: 5381 % 360 = 5381 - 14*360 = 5381 - 5040 = 341
        val hue = avatarHue("")
        val expected = 341f / 360f
        assertEquals(expected, hue, 0.0001f)
    }

    @Test
    fun `avatarHue A is 158 divided by 360`() {
        val hue = avatarHue("A")
        val expected = 158f / 360f
        assertEquals(expected, hue, 0.0001f)
    }

    @Test
    fun `avatarHue result is always in 0 to 1 exclusive range`() {
        val names = listOf("Alice", "Bob", "李雷", "😀", "😀A", "", "Christopher Knight")
        for (name in names) {
            val hue = avatarHue(name)
            assertTrue("hue for $name should be >= 0f", hue >= 0f)
            assertTrue("hue for $name should be < 1f", hue < 1f)
        }
    }

    // -------------------------------------------------------------------------
    // Saturation and brightness constants
    // -------------------------------------------------------------------------

    @Test
    fun `saturation constant is 0_55`() {
        assertEquals(0.55f, AM_AVATAR_SATURATION, 0.0001f)
    }

    @Test
    fun `brightness constant is 0_85`() {
        assertEquals(0.85f, AM_AVATAR_BRIGHTNESS, 0.0001f)
    }

    // -------------------------------------------------------------------------
    // extractInitials — takes raw name, returns 0–2 uppercase initials
    // -------------------------------------------------------------------------

    @Test
    fun `extractInitials empty string returns empty`() {
        assertEquals("", extractInitials(""))
    }

    @Test
    fun `extractInitials null returns empty`() {
        assertEquals("", extractInitials(null))
    }

    @Test
    fun `extractInitials single name returns single initial`() {
        assertEquals("J", extractInitials("Jane"))
    }

    @Test
    fun `extractInitials two words returns two initials`() {
        assertEquals("CK", extractInitials("Christopher Knight"))
    }

    @Test
    fun `extractInitials three words returns two initials from first two`() {
        assertEquals("JD", extractInitials("John D. Smith"))
    }

    @Test
    fun `extractInitials extra whitespace is trimmed`() {
        assertEquals("AB", extractInitials("  Alice  Bob  "))
    }

    @Test
    fun `extractInitials all uppercase input stays uppercase`() {
        assertEquals("AB", extractInitials("ALICE BOB"))
    }

    @Test
    fun `extractInitials lowercase letters are uppercased`() {
        assertEquals("AB", extractInitials("alice bob"))
    }
}
