package com.ideamtrls.androidmtrls.demo

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The Music scene's ID3v2 cover reader (`MusicCovers.kt`), over tags built byte by byte. */
class MusicId3Test {
    private val jpeg = bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0xFF, 0x00, 0xFF, 0xD9)
    private val png = bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    @Test
    fun tagLengthReadsTheSyncsafeSizeAndTheFooter() {
        assertEquals(10 + 257, musicId3TagLength(bytes(0x49, 0x44, 0x33, 3, 0, 0, 0, 0, 2, 1)))
        val wannaBeACat = bytes(0x49, 0x44, 0x33, 2, 0, 0, 0x02, 0x39, 0x57, 0x58)
        assertEquals(10 + (0x02 shl 21) + (0x39 shl 14) + (0x57 shl 7) + 0x58, musicId3TagLength(wannaBeACat))
        assertEquals(10 + 100 + 10, musicId3TagLength(bytes(0x49, 0x44, 0x33, 4, 0, 0x10, 0, 0, 0, 100)))
        assertNull(musicId3TagLength(bytes(0xFF, 0xFB, 0x90, 0x64, 0, 0, 0, 0, 0, 0))) // An MP3 frame, no tag.
        assertNull(musicId3TagLength(bytes(0x49, 0x44, 0x33, 5, 0, 0, 0, 0, 0, 0))) // A version it doesn't know.
        assertNull(musicId3TagLength(bytes(0x49, 0x44, 0x33, 3, 0, 0, 0, 0x80, 0, 0))) // Not syncsafe.
        assertNull(musicId3TagLength(bytes(0x49, 0x44, 0x33, 3, 0)))
    }

    @Test
    fun v22TakesThePicFrameAfterItsFormatAndDescription() {
        val tag = tag(2, frame22("TT2", latin1("\u0000Cant Sleep Crew")) + frame22("PIC", pic22("PNG", 0, png)) + ByteArray(32))
        assertArrayEquals(png, musicId3Cover(tag))
    }

    @Test
    fun aFrontCoverWinsOverAnEarlierPicture() {
        val tag = tag(2, frame22("PIC", pic22("PNG", 0, png)) + frame22("PIC", pic22("JPG", 3, jpeg)))
        assertArrayEquals(jpeg, musicId3Cover(tag))
    }

    @Test
    fun v22CompressedTagIsSkipped() {
        assertNull(musicId3Cover(tag(2, frame22("PIC", pic22("JPG", 3, jpeg)), flags = 0x40)))
    }

    @Test
    fun v23TakesTheApicFrameAfterItsMimeTypeAndDescription() {
        val tag = tag(3, frame23("TIT2", latin1("\u0000Living or Lively")) + frame23("APIC", apic("image/jpeg", 3, jpeg, description = latin1("Cover\u0000"))))
        assertArrayEquals(jpeg, musicId3Cover(tag))
    }

    @Test
    fun aUtf16DescriptionEndsOnATwoByteBoundary() {
        // "A" in UTF-16LE is 41 00; the terminator's two zeros follow it, so the first 00 00 in the
        // bytes (41 [00 00] 00) isn't the end.
        val description = bytes(0xFF, 0xFE, 0x41, 0x00, 0x00, 0x00)
        val tag = tag(3, frame23("APIC", apic("image/png", 3, png, encoding = 1, description = description)))
        assertArrayEquals(png, musicId3Cover(tag))
    }

    @Test
    fun v23UnsynchronisedTagIsResynchronisedFirst() {
        val frames = frame23("APIC", apic("image/jpeg", 3, jpeg))
        val tag = tag(3, unsynchronise(frames), flags = 0x80)
        assertArrayEquals(jpeg, musicId3Cover(tag))
    }

    @Test
    fun v23ExtendedHeaderAndGroupByteArePassedOver() {
        val extended = be32(6) + ByteArray(6)
        val grouped = frame23("APIC", byteArrayOf(7) + apic("image/jpeg", 3, jpeg), format = 0x20)
        assertArrayEquals(jpeg, musicId3Cover(tag(3, extended + grouped, flags = 0x40)))
    }

    @Test
    fun v23CompressedOrEncryptedPicturesAreSkipped() {
        val compressed = frame23("APIC", apic("image/jpeg", 3, jpeg), format = 0x80)
        val plain = frame23("APIC", apic("image/png", 0, png))
        assertArrayEquals(png, musicId3Cover(tag(3, compressed + plain)))
        assertNull(musicId3Cover(tag(3, frame23("APIC", apic("image/jpeg", 3, jpeg), format = 0x40))))
    }

    @Test
    fun v24ReadsSyncsafeFrameSizes() {
        val image = ByteArray(300) { (it % 251).toByte() }
        val tag = tag(4, frame24("TIT2", latin1("\u0000Black And White")) + frame24("APIC", apic("image/jpeg", 0, image)) + ByteArray(64))
        assertArrayEquals(image, musicId3Cover(tag))
    }

    @Test
    fun v24ReadsAPlainSizeThatIsNotSyncsafe() {
        val image = ByteArray(200) { 1 } // A 214-byte frame: 00 00 00 D6, not syncsafe.
        val tag = tag(4, frame24("APIC", apic("image/jpeg", 3, image), plainSize = true))
        assertArrayEquals(image, musicId3Cover(tag))
    }

    @Test
    fun v24UnsynchronisedFrameWithDataLengthIsResynchronised() {
        val data = apic("image/jpeg", 3, jpeg)
        val frame = frame24("APIC", be32(data.size) + unsynchronise(data), format = 0x02 or 0x01)
        assertArrayEquals(jpeg, musicId3Cover(tag(4, frame)))
    }

    @Test
    fun v24TagWideUnsynchronisationCoversEveryFrame() {
        val frame = frame24("APIC", unsynchronise(apic("image/jpeg", 3, jpeg)))
        assertArrayEquals(jpeg, musicId3Cover(tag(4, frame, flags = 0x80)))
    }

    @Test
    fun aLinkIsNotAPicture() {
        val link = frame23("APIC", apic("-->", 3, latin1("https://example.com/cover.jpg")))
        val image = frame23("APIC", apic("image/png", 0, png))
        assertArrayEquals(png, musicId3Cover(tag(3, link + image)))
    }

    @Test
    fun noPictureOrPaddingFirstGivesNull() {
        assertNull(musicId3Cover(tag(3, frame23("TIT2", latin1("\u0000Movement")))))
        assertNull(musicId3Cover(tag(3, ByteArray(20) + frame23("APIC", apic("image/png", 3, png)))))
        assertNull(musicId3Cover(bytes(0x49, 0x44, 0x33, 3, 0)))
    }

    @Test
    fun aFrameRunningPastTheTagIsDropped() {
        val whole = frame23("APIC", apic("image/png", 3, png))
        assertNull(musicId3Cover(tag(3, whole.copyOf(whole.size - 3))))
    }

    @Test
    fun resyncDropsTheZeroAfterEachFF() {
        assertArrayEquals(bytes(0xFF, 0xE0, 0xFF, 0x00, 0x01), musicId3Resync(bytes(0xFF, 0x00, 0xE0, 0xFF, 0x00, 0x00, 0x01)))
        assertArrayEquals(bytes(0x01, 0xFF), musicId3Resync(bytes(0x01, 0xFF)))
    }

    // Building tags

    private fun tag(version: Int, body: ByteArray, flags: Int = 0): ByteArray =
        bytes(0x49, 0x44, 0x33, version, 0, flags) + syncsafe(body.size) + body

    private fun frame22(id: String, data: ByteArray): ByteArray = latin1(id) + be32(data.size).copyOfRange(1, 4) + data

    private fun frame23(id: String, data: ByteArray, format: Int = 0): ByteArray = latin1(id) + be32(data.size) + bytes(0, format) + data

    private fun frame24(id: String, data: ByteArray, format: Int = 0, plainSize: Boolean = false): ByteArray =
        latin1(id) + (if (plainSize) be32(data.size) else syncsafe(data.size)) + bytes(0, format) + data

    /** A v2.2 picture: encoding, the 3-letter format, the type, an empty description, the image. */
    private fun pic22(format: String, type: Int, image: ByteArray): ByteArray = bytes(0) + latin1(format) + bytes(type, 0) + image

    /** A v2.3/v2.4 picture: encoding, the MIME type and its zero, the type, the description, the image. */
    private fun apic(mime: String, type: Int, image: ByteArray, encoding: Int = 0, description: ByteArray = bytes(0)): ByteArray =
        bytes(encoding) + latin1(mime) + bytes(0, type) + description + image

    /** Unsynchronisation: a zero after every 0xFF. */
    private fun unsynchronise(data: ByteArray): ByteArray =
        data.flatMap { if (it == 0xFF.toByte()) listOf(it, 0) else listOf(it) }.toByteArray()

    private fun syncsafe(n: Int): ByteArray = bytes(n shr 21 and 0x7F, n shr 14 and 0x7F, n shr 7 and 0x7F, n and 0x7F)

    private fun be32(n: Int): ByteArray = bytes(n ushr 24 and 0xFF, n shr 16 and 0xFF, n shr 8 and 0xFF, n and 0xFF)

    private fun latin1(text: String): ByteArray = text.toByteArray(Charsets.ISO_8859_1)

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }
}
