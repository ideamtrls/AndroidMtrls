package io.github.jmsdnns.androidmtrls.demo.scenes.music

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.demo.scenes.common.SceneGradient
import io.github.jmsdnns.androidmtrls.demo.scenes.common.brush
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Value types — ports of the iOS MusicData structs/enums
// ---------------------------------------------------------------------------

/**
 * A single track, built at launch from a bundled MP3's tags. Mirrors iOS
 * `MusicTrack`: [assetPath] is the file under `assets/Audio/` (the real playback
 * source, read through [MediaMetadataRetriever]/`MediaPlayer`); [coverArt] is the
 * album's embedded artwork decoded from that MP3, or `null` when the file carries
 * no picture (then [MusicCoverArt] falls back to the album gradient + glyph).
 */
@Immutable
data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val albumId: String,
    val trackNumber: Int,
    val durationSeconds: Double,
    val assetPath: String,
    val coverArt: ImageBitmap?,
)

/** An album, its tracks referenced by id. The [id] doubles as the palette slug. */
@Immutable
data class MusicAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val trackIds: List<String>,
    val coverArt: ImageBitmap?,
)

/** The whole library, scanned from the bundled MP3s. Mirrors iOS `MusicCatalog` lookups. */
@Immutable
data class MusicCatalog(
    val albums: List<MusicAlbum>,
    val tracks: List<MusicTrack>,
    val artistName: String,
) {
    fun album(id: String): MusicAlbum = albums.firstOrNull { it.id == id } ?: albums[0]

    fun track(id: String): MusicTrack? = tracks.firstOrNull { it.id == id }

    fun tracks(album: MusicAlbum): List<MusicTrack> =
        album.trackIds.mapNotNull { id -> tracks.firstOrNull { it.id == id } }
}

/**
 * A queue slot — a [track] tagged with a process-unique [uid]. The playback queue
 * ([MusicState.playOrder]) is a list of these rather than bare tracks so the
 * player carousel and the reorderable queue sheet have **stable, unique keys**
 * even when the same track appears twice (e.g. after "Add to Queue" on the whole
 * artist). Bare `track.id` keys would collide and crash the reorderable
 * `LazyColumn`; the uid tag sidesteps that while keeping value equality on the
 * underlying [track].
 */
@Immutable
data class MusicQueueItem(
    val uid: Long,
    val track: MusicTrack,
)

/** Which top-level view the scene shows. Mirrors iOS `MusicView`. */
enum class MusicView { Player, Library }

/**
 * Scene-local drill-in route pushed onto the panel stack: the artist's album
 * list, then an album's track list (keyed by album id). Mirrors iOS `MusicRoute`.
 */
sealed interface MusicRoute {
    data object Artist : MusicRoute
    data class Album(val albumId: String) : MusicRoute
}

/** Repeat cycle off → all → one → off. Mirrors iOS `RepeatMode`. */
enum class RepeatMode {
    Off, All, One;

    val next: RepeatMode
        get() = when (this) {
            Off -> All
            All -> One
            One -> Off
        }

    val isActive: Boolean get() = this != Off
}

/**
 * Long-press target for the `[Play, Add to Queue]` menu. Mirrors iOS
 * `MusicQueueTarget`.
 */
sealed interface MusicQueueTarget {
    data object Artist : MusicQueueTarget
    data class Album(val album: MusicAlbum) : MusicQueueTarget
    data class Track(val track: MusicTrack) : MusicQueueTarget

    fun tracks(catalog: MusicCatalog): List<MusicTrack> = when (this) {
        Artist -> catalog.tracks
        is Album -> catalog.tracks(album)
        is Track -> listOf(track)
    }

    /** Display label. `Artist` resolves to the live catalog artist name. */
    fun label(catalog: MusicCatalog): String = when (this) {
        Artist -> catalog.artistName
        is Album -> album.title
        is Track -> track.title
    }
}

// ---------------------------------------------------------------------------
// Palettes — ported VERBATIM from iOS MusicData.swift `palettes`
// ---------------------------------------------------------------------------

/**
 * Default gradient for albums without a tuned palette and for the loading
 * background. RGB values copied verbatim from iOS `MusicPalette.neutral`.
 */
val musicNeutralPalette: SceneGradient = SceneGradient(
    lightTop = Color(0.90f, 0.90f, 0.93f),
    lightBottom = Color(0.82f, 0.83f, 0.89f),
    darkTop = Color(0.11f, 0.11f, 0.14f),
    darkBottom = Color(0.07f, 0.07f, 0.10f),
)

/**
 * Tuned light/dark gradients keyed by album slug, RGB values copied verbatim
 * from iOS `MusicData.swift`'s `palettes` table; unknown albums fall back to
 * [musicNeutralPalette]. The slugs match the bundled MP3s' ID3 album names
 * (`Internet On The TV`, `Silent Artillery of Time`, `Street Fight`).
 *
 * [SceneGradient.brush] (the shared scene helper) renders the same diagonal
 * sweep as iOS's `LinearGradient` (top-leading → bottom-trailing); stop colors
 * and axis both match.
 */
private val musicPalettes: Map<String, SceneGradient> = mapOf(
    "internet-on-the-tv" to SceneGradient(
        lightTop = Color(0.91f, 0.86f, 0.96f),
        lightBottom = Color(0.84f, 0.78f, 0.96f),
        darkTop = Color(0.16f, 0.10f, 0.24f),
        darkBottom = Color(0.10f, 0.08f, 0.18f),
    ),
    "silent-artillery-of-time" to SceneGradient(
        lightTop = Color(0.88f, 0.90f, 0.94f),
        lightBottom = Color(0.78f, 0.82f, 0.91f),
        darkTop = Color(0.10f, 0.11f, 0.15f),
        darkBottom = Color(0.07f, 0.08f, 0.11f),
    ),
    "street-fight" to SceneGradient(
        lightTop = Color(0.93f, 0.87f, 0.87f),
        lightBottom = Color(0.80f, 0.84f, 0.93f),
        darkTop = Color(0.13f, 0.08f, 0.10f),
        darkBottom = Color(0.07f, 0.09f, 0.16f),
    ),
)

/** The album's tuned palette, or [musicNeutralPalette] for an unknown slug. */
fun musicPalette(slug: String): SceneGradient = musicPalettes[slug] ?: musicNeutralPalette

// ---------------------------------------------------------------------------
// Shared layout constants + formatter (ports of MusicData.swift free values)
// ---------------------------------------------------------------------------

/** Standard leading cover-art thumbnail edge for track/album/queue rows. */
val musicCoverThumbSize: Dp = 40.dp

/**
 * Tight title↔artist spacing in the player's track meta — deliberately below
 * `Spacing.xs` (4) so the two lines read as one stacked label. Mirrors iOS
 * `trackMetaSpacing`.
 */
val trackMetaSpacing: Dp = 2.dp

/**
 * Shared "m:ss" duration label used by the player, queue rows, and library track
 * rows. Mirrors iOS `musicTimeLabel(seconds:)` — minutes are NOT zero-padded.
 */
fun musicTimeLabel(seconds: Double): String {
    val total = seconds.roundToInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

/**
 * Trailing tabular-digit duration label shared by the library track rows and the
 * queue rows. Tabular figures (`tnum`) keep digits column-aligned while the colon
 * keeps its natural proportional width — mirrors iOS `.caption.monospacedDigit()`
 * (a fully monospaced font would over-space the colon into "8 : 41").
 */
@Composable
internal fun MusicDurationLabel(seconds: Double) {
    val isDark = LocalAMIsDarkTheme.current
    BasicText(
        text = musicTimeLabel(seconds),
        style = AMTheme.Type.caption
            .toTextStyle(AMTheme.Text.secondary.resolve(isDark))
            .copy(fontFeatureSettings = "tnum"),
    )
}

// ---------------------------------------------------------------------------
// Placeholder + catalog loader (ports of MusicData.swift `load` + placeholder)
// ---------------------------------------------------------------------------

/**
 * Synthetic track shaping the loading skeleton — the real player layout is
 * rendered from this, then shimmered, so the loading state reuses the finished
 * layout. Mirrors iOS `placeholderTrack` (225s duration).
 */
val musicPlaceholderTrack: MusicTrack = MusicTrack(
    id = "placeholder",
    title = "Track Title",
    artist = "Artist Name",
    albumId = "",
    trackNumber = 0,
    durationSeconds = 225.0,
    assetPath = "",
    coverArt = null,
)

/**
 * The bundled-audio directory under `assets/` — the MP3s copied from the iOS demo.
 *
 * Android's [MediaMetadataRetriever] reads these files' **duration** but returns
 * `null` for every ID3 text frame and the embedded artwork (a device-extractor
 * limitation, independent of the ID3 version). So the title/artist/album/track and
 * cover art are parsed straight from each file's ID3 tag by [readId3Tags]; only
 * duration is taken from [MediaMetadataRetriever]. This mirrors iOS, whose
 * `AVMetadataItem` code likewise handles the frames explicitly.
 */
private const val musicAssetDir = "Audio"

/** One MP3's scanned tags, before albums are assembled and artwork decoded. */
private data class RawTrack(
    val id: String,
    val title: String,
    val artist: String,
    val albumTitle: String,
    val albumId: String,
    val trackNumber: Int,
    val durationSeconds: Double,
    val assetPath: String,
    val artBytes: ByteArray?,
)

/**
 * Scans `assets/Audio/` and builds the whole library from each MP3's tags —
 * titles, artist, album, track order, durations, and embedded artwork — via
 * [MediaMetadataRetriever]. Pure I/O; runs off the main thread (callers await it
 * from a coroutine). A missing directory or zero MP3s yields an empty catalog.
 * Mirrors iOS `MusicCatalog.load()`.
 */
suspend fun loadMusicCatalog(context: Context): MusicCatalog = withContext(Dispatchers.IO) {
    val assets = context.assets
    val empty = MusicCatalog(emptyList(), emptyList(), "Unknown Artist")

    val files = (assets.list(musicAssetDir) ?: emptyArray())
        .filter { it.endsWith(".mp3", ignoreCase = true) }
        .sorted()
    if (files.isEmpty()) return@withContext empty

    // Scan every file into a raw record (metadata as tagged, artwork bytes held
    // for later per-album decode).
    val raws = files.mapNotNull { name -> scanTrack(context, name) }
    if (raws.isEmpty()) return@withContext empty

    // Group into albums; each album's cover is its first track's artwork (decoded
    // once). Every track in the album shares that decoded cover.
    val byId = raws.associateBy { it.id }
    val albums = raws.groupBy { it.albumId }.map { (albumId, group) ->
        val sorted = group.sortedWith(compareBy({ it.trackNumber }, { it.title }))
        val cover = sorted.firstNotNullOfOrNull { it.artBytes }?.let(::decodeArt)
        MusicAlbum(
            id = albumId,
            title = sorted[0].albumTitle,
            artist = mostCommon(sorted.map { it.artist }) ?: "Unknown Artist",
            trackIds = sorted.map { it.id },
            coverArt = cover,
        )
    }.sortedBy { it.title }

    // Flatten to catalog order (album order, then track order), stamping the
    // album cover onto each track.
    val tracks = albums.flatMap { album ->
        album.trackIds.mapNotNull { id ->
            byId[id]?.let { raw ->
                MusicTrack(
                    id = raw.id,
                    title = raw.title,
                    artist = raw.artist,
                    albumId = album.id,
                    trackNumber = raw.trackNumber,
                    durationSeconds = raw.durationSeconds,
                    assetPath = raw.assetPath,
                    coverArt = album.coverArt,
                )
            }
        }
    }

    MusicCatalog(
        albums = albums,
        tracks = tracks,
        artistName = mostCommon(tracks.map { it.artist }) ?: "Unknown Artist",
    )
}

/**
 * Reads one MP3's tags + embedded artwork bytes, or `null` if it can't be read.
 * Tags/artwork come from the file's ID3 tag ([readId3Tags]); duration comes from
 * [MediaMetadataRetriever] (the one field it reads reliably here), streamed from
 * the asset's offset file descriptor — no temp copy needed.
 */
private fun scanTrack(context: Context, name: String): RawTrack? {
    val path = "$musicAssetDir/$name"
    val filenameTitle = name.substringBeforeLast('.')
    val id3 = runCatching {
        context.assets.open(path).use { readId3Tags(it) }
    }.getOrNull()

    val durationSeconds = runCatching {
        MediaMetadataRetriever().let { retriever ->
            try {
                context.assets.openFd(path).use { afd ->
                    retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L) / 1000.0
            } finally {
                retriever.release()
            }
        }
    }.getOrDefault(0.0)

    val album = id3?.album?.takeIf { it.isNotBlank() } ?: "Unknown Album"
    return RawTrack(
        id = musicSlug(filenameTitle),
        title = id3?.title?.takeIf { it.isNotBlank() } ?: filenameTitle,
        artist = id3?.artist?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
        albumTitle = album,
        albumId = musicSlug(album),
        trackNumber = id3?.track ?: 0,
        durationSeconds = durationSeconds,
        assetPath = path,
        artBytes = id3?.picture,
    )
}

// ---------------------------------------------------------------------------
// Minimal ID3v2 tag reader (v2.2 / v2.3 / v2.4)
// ---------------------------------------------------------------------------

/** The fields pulled from an ID3v2 tag. */
private class Id3Tags(
    val title: String?,
    val artist: String?,
    val album: String?,
    val track: Int?,
    val picture: ByteArray?,
)

/**
 * Parses the leading ID3v2 tag from [input] for title/artist/album/track and the
 * first embedded picture, or `null` when there is no tag. Handles ID3v2.2 (3-char
 * frame ids `TT2`/`TP1`/`TAL`/`TRK`/`PIC`), v2.3 and v2.4 (`TIT2`/`TPE1`/`TALB`/
 * `TRCK`/`APIC`), the four text encodings, tag-level unsynchronisation, and the
 * v2.3/v2.4 extended header. Only the tag region is read (its size is in the
 * header), never the whole MP3.
 */
private fun readId3Tags(input: java.io.InputStream): Id3Tags? {
    val header = readFully(input, 10) ?: return null
    if (header[0].toInt() != 'I'.code || header[1].toInt() != 'D'.code || header[2].toInt() != '3'.code) return null
    val major = header[3].toInt() and 0xFF
    val flags = header[5].toInt() and 0xFF
    val tagSize = syncSafe(header, 6, 4)
    if (tagSize <= 0) return null
    val raw = readFully(input, tagSize) ?: return null
    val data = if (flags and 0x80 != 0) deunsync(raw) else raw

    var pos = 0
    // Skip the v2.3/v2.4 extended header, if present.
    if (major >= 3 && (flags and 0x40 != 0) && data.size >= 4) {
        pos += if (major == 4) syncSafe(data, 0, 4) else be32(data, 0) + 4
    }

    var title: String? = null
    var artist: String? = null
    var album: String? = null
    var track: Int? = null
    var picture: ByteArray? = null

    val idLen = if (major == 2) 3 else 4
    val frameHeaderLen = if (major == 2) 6 else 10
    while (pos + frameHeaderLen <= data.size) {
        if (data[pos].toInt() == 0) break // padding
        val id = String(data, pos, idLen, Charsets.ISO_8859_1)
        val frameSize = when {
            major == 2 -> be24(data, pos + 3)
            major == 4 -> syncSafe(data, pos + 4, 4)
            else -> be32(data, pos + 4)
        }
        val body = pos + frameHeaderLen
        if (frameSize <= 0 || body + frameSize > data.size) break
        val frame = data.copyOfRange(body, body + frameSize)
        when (id) {
            "TT2", "TIT2" -> title = decodeId3Text(frame)
            "TP1", "TPE1" -> artist = decodeId3Text(frame)
            "TAL", "TALB" -> album = decodeId3Text(frame)
            "TRK", "TRCK" -> track = leadingInt(decodeId3Text(frame))
            "PIC", "APIC" -> if (picture == null) picture = parseId3Picture(frame, major)
        }
        pos = body + frameSize
    }
    return Id3Tags(title, artist, album, track, picture)
}

/** Decodes an ID3 text-frame body (leading encoding byte + text) to a String. */
private fun decodeId3Text(frame: ByteArray): String? {
    if (frame.isEmpty()) return null
    val charset = when (frame[0].toInt() and 0xFF) {
        1 -> Charsets.UTF_16     // UTF-16 with BOM
        2 -> Charsets.UTF_16BE   // UTF-16BE, no BOM
        3 -> Charsets.UTF_8
        else -> Charsets.ISO_8859_1
    }
    return String(frame, 1, frame.size - 1, charset).trim { it == ' ' || it == '\u0000' }.ifEmpty { null }
}

/** Extracts the image bytes from a PIC (v2.2) / APIC (v2.3+) picture frame. */
private fun parseId3Picture(frame: ByteArray, major: Int): ByteArray? {
    if (frame.isEmpty()) return null
    val enc = frame[0].toInt() and 0xFF
    var i = 1
    i += if (major == 2) {
        3 // 3-char image format, e.g. "JPG"
    } else {
        // MIME type, null-terminated ASCII
        var j = i
        while (j < frame.size && frame[j].toInt() != 0) j++
        (j + 1) - i
    }
    if (i >= frame.size) return null
    i += 1 // picture type byte
    // Description, null-terminated in the frame's encoding.
    if (enc == 1 || enc == 2) {
        while (i + 1 < frame.size && !(frame[i].toInt() == 0 && frame[i + 1].toInt() == 0)) i += 2
        i += 2
    } else {
        while (i < frame.size && frame[i].toInt() != 0) i++
        i += 1
    }
    return if (i in 0 until frame.size) frame.copyOfRange(i, frame.size) else null
}

/** Reads exactly [n] bytes from [input], or `null` if the stream ends early. */
private fun readFully(input: java.io.InputStream, n: Int): ByteArray? {
    val buf = ByteArray(n)
    var read = 0
    while (read < n) {
        val r = input.read(buf, read, n - read)
        if (r < 0) return null
        read += r
    }
    return buf
}

/** Removes ID3 unsynchronisation: every `0xFF 0x00` pair collapses to `0xFF`. */
private fun deunsync(data: ByteArray): ByteArray {
    val out = ByteArray(data.size)
    var n = 0
    var i = 0
    while (i < data.size) {
        out[n++] = data[i]
        if (data[i].toInt() and 0xFF == 0xFF && i + 1 < data.size && data[i + 1].toInt() == 0) i++
        i++
    }
    return out.copyOf(n)
}

/** Big-endian 24-bit integer at [off]. */
private fun be24(b: ByteArray, off: Int): Int =
    ((b[off].toInt() and 0xFF) shl 16) or ((b[off + 1].toInt() and 0xFF) shl 8) or (b[off + 2].toInt() and 0xFF)

/** Big-endian 32-bit integer at [off]. */
private fun be32(b: ByteArray, off: Int): Int =
    ((b[off].toInt() and 0xFF) shl 24) or ((b[off + 1].toInt() and 0xFF) shl 16) or
        ((b[off + 2].toInt() and 0xFF) shl 8) or (b[off + 3].toInt() and 0xFF)

/** Sync-safe integer of [n] bytes at [off] (7 significant bits per byte). */
private fun syncSafe(b: ByteArray, off: Int, n: Int): Int {
    var value = 0
    for (i in 0 until n) value = (value shl 7) or (b[off + i].toInt() and 0x7F)
    return value
}

/** Decodes embedded-artwork bytes into an [ImageBitmap], or `null` on failure. */
private fun decodeArt(bytes: ByteArray): ImageBitmap? =
    runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()

/** Leading run of digits parsed as an Int (e.g. "3/9" → 3), or null. */
private fun leadingInt(text: String?): Int? =
    text?.trim()?.takeWhile { it.isDigit() }?.toIntOrNull()

/** Most frequent element (ties broken by first appearance), or null when empty. */
private fun <T> mostCommon(values: List<T>): T? {
    if (values.isEmpty()) return null
    val counts = values.groupingBy { it }.eachCount()
    return values.maxByOrNull { counts.getValue(it) }
}

/**
 * Lowercased slug of ASCII alphanumerics and single hyphens, e.g.
 * "Some Track (live)" → "some-track-live". Mirrors iOS `musicSlug`.
 */
private fun musicSlug(text: String): String {
    val slug = StringBuilder()
    var pendingHyphen = false
    for (ch in text.lowercase()) {
        if (ch.code < 128 && ch.isLetterOrDigit()) {
            if (pendingHyphen && slug.isNotEmpty()) slug.append('-')
            pendingHyphen = false
            slug.append(ch)
        } else {
            pendingHyphen = true
        }
    }
    return slug.toString()
}

// ---------------------------------------------------------------------------
// MusicCoverArt — embedded artwork, with a gradient + glyph fallback
// ---------------------------------------------------------------------------

/**
 * Cover art for a track/album: the MP3's embedded artwork ([coverArt]) drawn
 * full-bleed / cropped to the square, or — when the file carried no picture — the
 * album's two-stop gradient wash with a centered translucent music-note glyph
 * watermark. Used full-bleed on the player and at [musicCoverThumbSize] in
 * library/queue rows. Mirrors iOS's extracted-artwork cover (`IMRemoteImage` over
 * the coverURL, gradient/skeleton fallback).
 *
 * @param coverArt     The decoded embedded artwork, or `null` to draw the fallback.
 * @param palette      The album gradient used by the fallback.
 * @param modifier     Sizing modifier (square).
 * @param cornerRadius Clip radius (0 for the full-bleed player cover).
 * @param glyphSize    Music-note fallback glyph edge.
 */
@Composable
fun MusicCoverArt(
    coverArt: ImageBitmap?,
    palette: SceneGradient,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    glyphSize: Dp = 96.dp,
) {
    val shape = RoundedCornerShape(cornerRadius)
    if (coverArt != null) {
        Image(
            bitmap = coverArt,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
        return
    }

    val isDark = LocalAMIsDarkTheme.current
    val glyphTint = AMTheme.Text.primary.resolve(isDark).copy(alpha = 0.30f)
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = palette.brush(), shape = shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            colorFilter = ColorFilter.tint(glyphTint),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(glyphSize),
        )
    }
}
