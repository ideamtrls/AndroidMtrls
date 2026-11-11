package com.ideamtrls.androidmtrls.demo

import android.graphics.ImageDecoder
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// The albums' own covers. The site keeps each album's cover only inside its MP3s' ID3v2 tags, so
// the scene reads it from there: the tag at the start of the album's first MP3, fetched with byte
// ranges, its attached picture found by a pure parser, then decoded and kept in memory for the app.

/**
 * The cover embedded in the MP3 at [url], or null when it can't be read. A cover read before (by
 * this scene or an earlier one) comes from memory; otherwise the MP3's ID3v2 tag is fetched off the
 * main thread ([musicFetchId3Tag]), its front cover found ([musicId3Cover]) and decoded at most
 * [MusicCoverMaxSide] on its longest side, which the largest art card still fills. Cancelling the
 * caller stops the read.
 */
internal suspend fun musicEmbeddedCover(url: String): ImageBitmap? {
    MusicCoverCache.get(url)?.let { return it }
    val tag = runInterruptible(Dispatchers.IO) { musicFetchId3Tag(url) } ?: return null
    val cover = withContext(Dispatchers.Default) { musicId3Cover(tag)?.let(::musicDecodeCover) } ?: return null
    MusicCoverCache.put(url, cover)
    return cover
}

/** The cover already read from the MP3 at [url], if memory still holds it. Safe on any thread. */
internal fun musicCachedCover(url: String): ImageBitmap? = MusicCoverCache.get(url)

/**
 * The covers read so far, by their MP3's URL, for the whole app: a scene made again (after a
 * configuration change, or entered anew) shows them on its first frame. Bounded by the decoded
 * pictures' bytes, room for every album's cover.
 */
private object MusicCoverCache : LruCache<String, ImageBitmap>(64 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}

/** The longest side, in pixels, a cover is decoded at. */
private const val MusicCoverMaxSide = 1024

/** The scene's requests say what they are; nothing personal is sent. */
private const val MusicUserAgent = "AndroidMtrls/0.1 (Android demo)"

/** The largest tag the reader fetches; a cover in a bigger one stays unread. */
private const val MusicId3MaxTagBytes = 16 * 1024 * 1024

/**
 * The ID3v2 tag at the start of the MP3 at [url], header first, or null when there's none or it
 * can't be fetched. Two range requests: the first 10 bytes, whose header gives the tag's length,
 * then the whole tag (and its footer, when it has one). A server that ignores ranges sends the
 * whole file instead; the reader then takes the tag from the start of that same response and stops,
 * reading no more than the tag. Blocking: call it off the main thread.
 */
internal fun musicFetchId3Tag(url: String): ByteArray? {
    val length: Int
    val first = musicOpenRange(url, MusicId3HeaderSize) ?: return null
    try {
        val header = first.inputStream.readExactly(MusicId3HeaderSize) ?: return null
        length = musicId3TagLength(header)?.takeIf { it <= MusicId3MaxTagBytes } ?: return null
        if (first.responseCode == HttpURLConnection.HTTP_OK) {
            val rest = first.inputStream.readExactly(length - MusicId3HeaderSize) ?: return null
            return header + rest
        }
    } catch (_: IOException) {
        return null
    } finally {
        first.disconnect()
    }
    if (Thread.currentThread().isInterrupted) return null
    val second = musicOpenRange(url, length) ?: return null
    return try {
        // A partial response starts at the first byte asked for; a full one at the file's start.
        second.inputStream.readExactly(length)
    } catch (_: IOException) {
        null
    } finally {
        second.disconnect()
    }
}

/**
 * A connection asking for the first [count] bytes of [url], once it has answered with the range
 * (206) or the whole file (200); null for anything else.
 */
private fun musicOpenRange(url: String, count: Int): HttpURLConnection? {
    val connection = try {
        URL(url).openConnection() as HttpURLConnection
    } catch (_: IOException) {
        return null
    }
    connection.connectTimeout = 15_000
    connection.readTimeout = 30_000
    connection.useCaches = false
    connection.setRequestProperty("User-Agent", MusicUserAgent)
    connection.setRequestProperty("Range", "bytes=0-${count - 1}")
    return try {
        when (connection.responseCode) {
            HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_PARTIAL -> connection
            else -> {
                connection.disconnect()
                null
            }
        }
    } catch (_: IOException) {
        connection.disconnect()
        null
    }
}

/** Exactly [count] bytes from the stream, or null when it ends first. */
private fun InputStream.readExactly(count: Int): ByteArray? = readNBytes(count).takeIf { it.size == count }

/** [bytes] as a bitmap, its longest side at most [MusicCoverMaxSide]; null when they aren't a picture. */
private fun musicDecodeCover(bytes: ByteArray): ImageBitmap? = try {
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(bytes)) { decoder, info, _ ->
        val longest = max(info.size.width, info.size.height)
        if (longest > MusicCoverMaxSide) {
            val scale = MusicCoverMaxSide.toFloat() / longest
            decoder.setTargetSize(
                (info.size.width * scale).roundToInt().coerceAtLeast(1),
                (info.size.height * scale).roundToInt().coerceAtLeast(1),
            )
        }
    }.asImageBitmap()
} catch (_: Exception) {
    null
}

// The ID3v2 tag. Pure functions over bytes, so they're unit-tested.

/** An ID3v2 tag's header: "ID3", the version and revision, the flags, the syncsafe size. */
internal const val MusicId3HeaderSize = 10

/** The front cover's picture type in an attached picture frame. */
private const val MusicId3FrontCover = 3

/**
 * The whole length of the ID3v2 tag whose first 10 bytes are [header]: the header, the tag's
 * extended header, frames and padding (the syncsafe size), and the footer when the flags say there
 * is one (v2.4). Null when [header] doesn't open a tag of version 2.2, 2.3 or 2.4.
 */
internal fun musicId3TagLength(header: ByteArray): Int? {
    if (header.size < MusicId3HeaderSize) return null
    if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) return null
    val version = header.u8(3)
    if (version !in 2..4 || header.u8(4) == 0xFF) return null
    val size = header.syncsafe(6) ?: return null
    val footer = if (version == 4 && header.u8(5) and 0x10 != 0) MusicId3HeaderSize else 0
    return MusicId3HeaderSize + size + footer
}

/**
 * The image bytes of the cover in [tag], a whole ID3v2 tag from its header on: the attached
 * picture whose type is the front cover, else the first attached picture, or null when it holds
 * none this reader can take.
 *
 * - **v2.2**: `PIC` frames with 6-byte headers (a 3-letter id, a 3-byte size), the picture's
 *   3-letter image format in place of a MIME type. A tag flagged compressed is skipped, as the
 *   version says to.
 * - **v2.3**: `APIC` frames with 10-byte headers, plain 32-bit sizes. Compressed and encrypted
 *   frames are skipped; a grouped frame's group byte is passed over.
 * - **v2.4**: `APIC` frames whose sizes are syncsafe (read plain when a size byte has its top bit
 *   set, as some writers do). Compressed and encrypted frames are skipped; the group byte and the
 *   data length indicator are passed over; a frame flagged unsynchronised, or every frame in a
 *   tag flagged so, is resynchronised.
 * - In v2.2 and v2.3 the tag's unsynchronisation flag covers the whole tag, which is
 *   resynchronised before its frames are read. An extended header (v2.3, v2.4) is skipped.
 * - A picture frame is its text encoding, the MIME type (or format), the picture type and the
 *   description, ended by one zero byte, or by two on a two-byte boundary in UTF-16, then the
 *   image. A picture that's only a link ("-->") is passed over.
 */
internal fun musicId3Cover(tag: ByteArray): ByteArray? {
    if (musicId3TagLength(tag) == null) return null
    val version = tag.u8(3)
    val flags = tag.u8(5)
    val size = tag.syncsafe(6) ?: return null
    if (version == 2 && flags and 0x40 != 0) return null
    var body = tag.copyOfRange(MusicId3HeaderSize, min(MusicId3HeaderSize + size, tag.size))
    if (version < 4 && flags and 0x80 != 0) body = musicId3Resync(body)

    var position = 0
    if (version >= 3 && flags and 0x40 != 0) {
        if (body.size < 4) return null
        position = if (version == 3) 4 + body.int32(0) else body.syncsafe(0) ?: return null
    }
    val frameHeader = if (version == 2) 6 else 10
    val idLength = if (version == 2) 3 else 4
    val pictureId = if (version == 2) "PIC" else "APIC"
    var first: ByteArray? = null
    while (position >= 0 && position.toLong() + frameHeader <= body.size) {
        if (!body.isFrameId(position, idLength)) break // Padding, or the tag's end.
        val frameSize: Long = when (version) {
            2 -> body.int24(position + 3).toLong()
            3 -> body.int32(position + 4).toLong() and 0xFFFFFFFFL
            else -> (body.syncsafe(position + 4) ?: body.int32(position + 4)).toLong() and 0xFFFFFFFFL
        }
        val start = position + frameHeader
        if (start + frameSize > body.size) break
        val end = (start + frameSize).toInt()
        if (String(body, position, idLength, Charsets.ISO_8859_1) == pictureId) {
            val data = if (version == 2) {
                body.copyOfRange(start, end)
            } else {
                musicId3FrameData(body, start, end, version, body.u8(position + 9), tagUnsync = version == 4 && flags and 0x80 != 0)
            }
            val picture = data?.let { musicId3Picture(it, version) }
            if (picture != null) {
                if (picture.first == MusicId3FrontCover) return picture.second
                if (first == null) first = picture.second
            }
        }
        position = end
    }
    return first
}

/**
 * A v2.3 or v2.4 frame's data between [start] and [end], past the bytes its format flags
 * ([format], the frame header's second flag byte) add, and resynchronised where v2.4 says it's
 * unsynchronised; null for a compressed or encrypted frame.
 */
private fun musicId3FrameData(body: ByteArray, start: Int, end: Int, version: Int, format: Int, tagUnsync: Boolean): ByteArray? {
    var from = start
    if (version == 3) {
        if (format and 0xC0 != 0) return null // Compressed or encrypted.
        if (format and 0x20 != 0) from += 1 // The group byte.
        return if (from <= end) body.copyOfRange(from, end) else null
    }
    if (format and 0x0C != 0) return null // Compressed or encrypted.
    if (format and 0x40 != 0) from += 1 // The group byte.
    if (format and 0x01 != 0) from += 4 // The data length indicator.
    if (from > end) return null
    val data = body.copyOfRange(from, end)
    return if (tagUnsync || format and 0x02 != 0) musicId3Resync(data) else data
}

/**
 * A picture frame's picture type and image, or null when it's malformed, only a link, or empty.
 * v2.2 gives a 3-letter image format where v2.3 and v2.4 give a zero-ended MIME type.
 */
private fun musicId3Picture(data: ByteArray, version: Int): Pair<Int, ByteArray>? {
    if (data.isEmpty()) return null
    val encoding = data.u8(0)
    var position = 1
    val format: String
    if (version == 2) {
        if (data.size < position + 3) return null
        format = String(data, position, 3, Charsets.ISO_8859_1)
        position += 3
    } else {
        val ends = data.indexOfZero(position, step = 1)
        if (ends < 0) return null
        format = String(data, position, ends - position, Charsets.ISO_8859_1)
        position = ends + 1
    }
    if (format == "-->" || position >= data.size) return null
    val type = data.u8(position)
    position += 1
    val step = when (encoding) {
        0, 3 -> 1 // ISO-8859-1, UTF-8
        1, 2 -> 2 // UTF-16 with a byte order mark, UTF-16BE
        else -> return null
    }
    val ends = data.indexOfZero(position, step)
    if (ends < 0) return null
    position = ends + step
    if (position >= data.size) return null
    return type to data.copyOfRange(position, data.size)
}

/** [bytes] with unsynchronisation undone: every 0xFF 0x00 back to 0xFF. */
internal fun musicId3Resync(bytes: ByteArray): ByteArray {
    val out = ByteArray(bytes.size)
    var count = 0
    var index = 0
    while (index < bytes.size) {
        val byte = bytes[index]
        out[count++] = byte
        index += if (byte == 0xFF.toByte() && index + 1 < bytes.size && bytes[index + 1] == 0.toByte()) 2 else 1
    }
    return out.copyOf(count)
}

private fun ByteArray.u8(at: Int): Int = this[at].toInt() and 0xFF

private fun ByteArray.int24(at: Int): Int = (u8(at) shl 16) or (u8(at + 1) shl 8) or u8(at + 2)

private fun ByteArray.int32(at: Int): Int = (u8(at) shl 24) or (u8(at + 1) shl 16) or (u8(at + 2) shl 8) or u8(at + 3)

/** Four bytes of seven bits each, or null when one has its top bit set (so they aren't syncsafe). */
private fun ByteArray.syncsafe(at: Int): Int? {
    var value = 0
    for (index in at until at + 4) {
        val byte = u8(index)
        if (byte and 0x80 != 0) return null
        value = (value shl 7) or byte
    }
    return value
}

/** Whether [length] bytes at [at] are a frame id: capital letters and digits. */
private fun ByteArray.isFrameId(at: Int, length: Int): Boolean =
    (at until at + length).all { u8(it) in 'A'.code..'Z'.code || u8(it) in '0'.code..'9'.code }

/** Where a terminator of [step] zero bytes starts, on a [step]-byte boundary from [from]; -1 when none. */
private fun ByteArray.indexOfZero(from: Int, step: Int): Int {
    var index = from
    while (index + step <= size) {
        if ((0 until step).all { this[index + it] == 0.toByte() }) return index
        index += step
    }
    return -1
}
