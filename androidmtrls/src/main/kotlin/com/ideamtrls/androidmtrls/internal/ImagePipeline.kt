package com.ideamtrls.androidmtrls

import android.content.Context
import android.graphics.ImageDecoder
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Loads, decodes and caches the pictures behind [AMRemoteImage] (avatars included) and the art
 * backdrops' photos. Not public API.
 *
 * - **Bytes** come from `HttpURLConnection` on the IO dispatcher, following redirects (https to
 *   https, and http up to https, never down). A body is at most 32 MB: a longer announced length
 *   is refused before reading, and the read stops as soon as more arrives ([readBounded]), since
 *   a server can omit the length or lie about it. Each fetch has 60 s to finish on top of its
 *   per-read timeouts; past that, or when every view waiting on it leaves, it's cut off at once,
 *   the connection disconnected under a blocked read ([runAbortable]).
 * - **Disk**: the pipeline's own [ImageDiskCache] in `cacheDir/am_image_cache/`, read before
 *   fetching and written once a fetched body decodes. Fetches neither read nor fill the app's HTTP
 *   cache (`useCaches = false`), and nothing here installs a process-wide `ResponseCache`, so the
 *   app's other connections are untouched.
 * - **Decoding** runs off the main thread through [ImageDecoder], at the size the view needs
 *   ([decodeSize]): never larger than the picture itself, and never more than 16 megapixels or
 *   8192 pixels on a side, whatever the frame.
 * - **Memory**: decoded bitmaps sit in an [ImageMemoryCache] keyed by URL, size and fit, which a
 *   view reads synchronously during composition, so a view that comes back (a scrolled row, a
 *   popped page) shows its picture on its first frame instead of the fallback.
 * - **One load per picture**: views asking for the same key while it loads share one request and
 *   one decode ([InFlightLoads]).
 *
 * A picture is treated as immutable per URL, the way every cache here is keyed: once a URL's bytes
 * are stored they serve that URL until they're trimmed, whatever the server's cache headers say. A
 * new picture belongs at a new URL. Failures are never cached; the next request retries.
 */
internal class ImagePipeline private constructor(context: Context, memoryBytes: Int) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val memory = ImageMemoryCache<DecodedImage>(memoryBytes) { it.byteCount }
    private val loads = InFlightLoads<ImageKey, DecodedImage>(scope)

    /** The disk cache, opened on the IO dispatcher as the pipeline starts; null when it can't be. */
    private val disk: Deferred<ImageDiskCache?> = scope.async { openDisk(context) }

    /** The decode stored for [key], if any. Safe to call during composition. */
    fun cached(key: ImageKey): DecodedImage? = memory[key]

    /**
     * The most recent decode of [url] at any size, if any: what a view shows on its first frame,
     * before it knows its own size. Safe to call during composition.
     */
    fun latest(url: String): DecodedImage? = memory.latest(url)

    /**
     * The picture for [key], from memory when it's there, otherwise joining or starting the one
     * load for it: from the disk cache, else from the network. Throws when the request fails,
     * misses its deadline, or the bytes don't decode.
     */
    suspend fun load(key: ImageKey): DecodedImage {
        memory[key]?.let { return it }
        return loads.load(key) {
            val image = fromDisk(key) ?: fromNetwork(key)
            memory.put(key, image)
            image
        }
    }

    /** [key]'s picture from the disk cache, or null on a miss. A stored body that doesn't decode is dropped. */
    private suspend fun fromDisk(key: ImageKey): DecodedImage? {
        val disk = disk.await() ?: return null
        val bytes = withContext(Dispatchers.IO) { disk.read(key.url) } ?: return null
        return try {
            withContext(Dispatchers.Default) { decode(bytes, key) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            withContext(Dispatchers.IO) { disk.remove(key.url) }
            null
        }
    }

    /** [key]'s picture from the network; its body goes to the disk cache once it decodes. */
    private suspend fun fromNetwork(key: ImageKey): DecodedImage {
        val bytes = runAbortable(FetchDeadlineMillis) { signal -> fetch(key.url, signal) }
        val image = withContext(Dispatchers.Default) { decode(bytes, key) }
        // In the pipeline's own scope, so a view leaving doesn't stop the write. It never throws.
        scope.launch { disk.await()?.write(key.url, bytes) }
        return image
    }

    companion object {
        /** The decoded-bitmap budget of the memory cache. */
        private const val MemoryBytes = 64 * 1024 * 1024

        /** The disk cache's budget. */
        private const val DiskBytes = 64L * 1024 * 1024

        /** The disk cache's directory, in the app's cache directory. */
        private const val DiskDirectory = "am_image_cache"

        /** The largest response body a fetch reads, and the largest body the disk cache serves. */
        private const val MaxBodyBytes = 32 * 1024 * 1024

        /** How long a fetch has in all, redirects included, on top of the per-read timeouts. */
        private const val FetchDeadlineMillis = 60_000L

        /** Identifies the library to image hosts, some of which refuse requests without one. */
        private const val UserAgent = "AndroidMtrls/0.1 (Android demo)"

        private const val MaxRedirects = 5
        private const val ConnectTimeoutMillis = 15_000
        private const val ReadTimeoutMillis = 30_000

        @Volatile
        private var instance: ImagePipeline? = null

        /** The shared pipeline. The first call starts it, which opens the disk cache off the main thread. */
        fun shared(context: Context): ImagePipeline =
            instance ?: synchronized(this) {
                instance ?: ImagePipeline(context.applicationContext, MemoryBytes).also { instance = it }
            }

        /** Opens the disk cache in [context]'s cache directory. Blocking; null when there's no cache directory to use. */
        private fun openDisk(context: Context): ImageDiskCache? = try {
            ImageDiskCache.open(File(context.cacheDir, DiskDirectory), DiskBytes, MaxBodyBytes)
        } catch (_: Exception) {
            null
        }

        /**
         * The response body of [url], following redirects. Throws on anything but a 2xx, on a body
         * over [MaxBodyBytes], and once [signal] has cut it off. Blocking.
         */
        private fun fetch(url: String, signal: AbortSignal): ByteArray {
            var current = URL(url)
            repeat(MaxRedirects + 1) {
                val connection = current.openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = ConnectTimeoutMillis
                    connection.readTimeout = ReadTimeoutMillis
                    connection.instanceFollowRedirects = true
                    connection.useCaches = false
                    connection.setRequestProperty("User-Agent", UserAgent)
                    signal.onAbort { connection.disconnect() }
                    connection.connect()
                    // Disconnecting does nothing until the connection is under way, so an abort
                    // that landed before then is caught here.
                    signal.throwIfAborted()
                    val code = connection.responseCode
                    when (code) {
                        in 200..299 -> {
                            val length = connection.contentLengthLong
                            if (length > MaxBodyBytes) throw IOException("Body of $length bytes is over $MaxBodyBytes")
                            return connection.inputStream.use { readBounded(it, MaxBodyBytes, length) }
                        }
                        in 300..399 -> {
                            // Same-protocol redirects are followed by the connection itself;
                            // what's left is a change of protocol. Upgrade only.
                            val location = connection.getHeaderField("Location") ?: throw IOException("HTTP $code with no Location")
                            val next = URL(current, location)
                            if (next.protocol != "https") throw IOException("Refusing redirect to ${next.protocol}")
                            current = next
                        }
                        else -> throw IOException("HTTP $code")
                    }
                } finally {
                    connection.disconnect()
                }
            }
            throw IOException("Too many redirects")
        }

        private fun decode(bytes: ByteArray, key: ImageKey): DecodedImage {
            var source = IntSize.Zero
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
                source = IntSize(info.size.width, info.size.height)
                val size = decodeSize(source, IntSize(key.width, key.height), key.fit)
                if (size != source) decoder.setTargetSize(size.width, size.height)
            }
            return DecodedImage(bitmap.asImageBitmap(), source, bitmap.allocationByteCount)
        }
    }
}

/** How a decode is sized against the frame it fills. */
internal enum class ImageFit {
    /** The picture covers the frame, cropping what overflows: it's at least the frame's size on both axes. */
    Cover,

    /** The whole picture shows inside the frame: it's at most the frame's size on both axes. */
    Contain,
}

/** What the memory cache keys a decode by: the picture and the frame it was decoded for, in pixels. */
internal data class ImageKey(val url: String, val width: Int, val height: Int, val fit: ImageFit)

/** A decoded picture, with the size of the original it came from. */
internal class DecodedImage(val bitmap: ImageBitmap, val sourceSize: IntSize, val byteCount: Int) {

    /** Whether this decode is sharp enough for a [target] frame, so a bigger one isn't needed ([decodeCovers]). */
    fun covers(target: IntSize, fit: ImageFit): Boolean =
        decodeCovers(IntSize(bitmap.width, bitmap.height), sourceSize, target, fit)
}

/** The most pixels a decode holds, whatever its frame: 16 megapixels. */
private const val MaxDecodePixels = 16_000_000L

/** The longest side a decode has, whatever its frame. */
private const val MaxDecodeSide = 8192

/**
 * The size to decode a [source] picture at for a [target] frame: scaled down, keeping its aspect,
 * until it just covers the frame ([ImageFit.Cover]) or just fits inside it ([ImageFit.Contain]),
 * never larger than the source; an empty target asks for the source size.
 *
 * Whatever the fit and the frame, a decode is held to [MaxDecodePixels] in all and [MaxDecodeSide]
 * on a side: a size past either becomes the source scaled down uniformly to the largest that meets
 * both, each side at least a pixel. That capped size depends on the source alone, so every frame
 * that would need more than the caps asks for the same one. An empty source is left as it is.
 */
internal fun decodeSize(source: IntSize, target: IntSize, fit: ImageFit): IntSize {
    if (source.width <= 0 || source.height <= 0) return source
    val fitted = fittedSize(source, target, fit)
    return if (withinDecodeCaps(fitted)) fitted else cappedSize(source)
}

/**
 * Whether a decode of [decoded] pixels from a [source] picture is sharp enough for a [target]
 * frame: at least what [decodeSize] asks for that frame on both axes. A decode held to the caps
 * covers every frame that would need more, since [decodeSize] asks the same capped size for them
 * all, so it's never reloaded for them.
 */
internal fun decodeCovers(decoded: IntSize, source: IntSize, target: IntSize, fit: ImageFit): Boolean {
    val needed = decodeSize(source, target, fit)
    return decoded.width >= needed.width && decoded.height >= needed.height
}

/** The frame-fitting rule under the caps: [source] scaled down until it covers or fits [target]. */
private fun fittedSize(source: IntSize, target: IntSize, fit: ImageFit): IntSize {
    if (target.width <= 0 || target.height <= 0) return source
    val scaleX = target.width.toDouble() / source.width
    val scaleY = target.height.toDouble() / source.height
    val scale = when (fit) {
        ImageFit.Cover -> max(scaleX, scaleY)
        ImageFit.Contain -> min(scaleX, scaleY)
    }
    if (scale >= 1.0) return source
    // Round up, less a hair of floating-point error, so the cover side lands on the frame exactly.
    fun scaled(length: Int) = max(1, ceil(length * scale - 1e-6).toInt())
    return IntSize(scaled(source.width), scaled(source.height))
}

private fun withinDecodeCaps(size: IntSize): Boolean =
    size.width <= MaxDecodeSide && size.height <= MaxDecodeSide &&
        size.width.toLong() * size.height <= MaxDecodePixels

/** [source] scaled down uniformly to the largest size within both caps, each side at least a pixel. */
private fun cappedSize(source: IntSize): IntSize {
    val width = source.width.toDouble()
    val height = source.height.toDouble()
    val scale = min(1.0, min(MaxDecodeSide / max(width, height), sqrt(MaxDecodePixels / (width * height))))
    // Round down, plus a hair of floating-point error, so the caps hold and a side meant to land
    // on one exactly does.
    fun scaled(length: Double) = max(1, floor(length * scale + 1e-6).toInt())
    return IntSize(scaled(width), scaled(height))
}

/**
 * Reads [input] to its end, refusing a body over [maxBytes]: throws an [IOException] as soon as the
 * running total passes [maxBytes], whatever length the server announced, never holding more than
 * one byte past the limit. [expectedLength], when known (not negative), only sizes the first buffer,
 * so a server that lies about it or leaves it out is still held to the limit.
 */
internal fun readBounded(input: InputStream, maxBytes: Int, expectedLength: Long = -1L): ByteArray {
    // One byte of room past the expected end shows the end without growing; one past the limit
    // shows a body going over it.
    val room = maxBytes + 1L
    val first = if (expectedLength >= 0) expectedLength + 1 else FirstReadBytes.toLong()
    var buffer = ByteArray(first.coerceIn(1L, room).toInt())
    var size = 0
    while (true) {
        if (size == buffer.size) {
            if (size > maxBytes) throw IOException("Body over $maxBytes bytes")
            buffer = buffer.copyOf(min(buffer.size * 2L, room).toInt())
        }
        val read = input.read(buffer, size, buffer.size - size)
        if (read < 0) return buffer.copyOf(size)
        size += read
    }
}

/** The first buffer for a body of unknown length. */
private const val FirstReadBytes = 64 * 1024

/**
 * How a blocking step that an interrupt doesn't stop (a read blocked on a socket) is cut off from
 * another thread: the step registers how ([onAbort]), and [abort] runs it. Thread-safe.
 */
internal class AbortSignal {
    private val lock = Any()
    private var action: (() -> Unit)? = null
    private var aborted = false

    /**
     * Registers [action] as how to cut off the step about to block, in place of the last one.
     * Throws an [InterruptedIOException] once aborted, so nothing new starts.
     */
    fun onAbort(action: () -> Unit) {
        synchronized(lock) {
            if (aborted) throw InterruptedIOException("Aborted")
            this.action = action
        }
    }

    /** Throws an [InterruptedIOException] once aborted, for an abort its action couldn't act on yet. */
    fun throwIfAborted() {
        synchronized(lock) { if (aborted) throw InterruptedIOException("Aborted") }
    }

    /** Aborts: runs the registered action, once, outside the lock, and refuses new ones. */
    fun abort() {
        val cutOff = synchronized(lock) {
            aborted = true
            action.also { action = null }
        }
        cutOff?.invoke()
    }

    /** Forgets the registered action: the blocking work is over, so there's nothing to cut off. */
    fun finish() {
        synchronized(lock) { action = null }
    }
}

/**
 * Runs the blocking [block] on the IO dispatcher with [deadlineMillis] to finish. As soon as the
 * deadline passes or the caller is cancelled, [block]'s thread is interrupted, as [runInterruptible]
 * does, and the step it registered with its [AbortSignal] is cut off from another thread, for
 * blocking calls an interrupt doesn't stop. A missed deadline is a failure, not a cancellation: it
 * throws an [IOException] (an [InterruptedIOException], or whatever the cut-off step threw).
 */
internal suspend fun <T : Any> runAbortable(deadlineMillis: Long, block: (AbortSignal) -> T): T {
    val signal = AbortSignal()
    return withTimeoutOrNull(deadlineMillis) {
        // Waits for the deadline or the caller's cancellation, then cuts off whatever is blocked.
        val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } finally {
                signal.abort()
            }
        }
        try {
            runInterruptible(Dispatchers.IO) { block(signal) }
        } finally {
            signal.finish()
            watcher.cancel()
        }
    } ?: throw InterruptedIOException("Not done within $deadlineMillis ms")
}

/**
 * The memory cache: an [LruCache] bounded by the bytes its values hold, keyed by [ImageKey], plus
 * an index of the most recent key stored for each URL, so a view can find a picture by URL alone
 * before it knows its size ([latest]). Thread-safe.
 */
internal class ImageMemoryCache<V : Any>(maxBytes: Int, private val bytesOf: (V) -> Int) {

    private val lock = Any()
    private val latestKeys = HashMap<String, ImageKey>()

    private val entries = object : LruCache<ImageKey, V>(maxBytes) {
        override fun sizeOf(key: ImageKey, value: V): Int = bytesOf(value)

        override fun entryRemoved(evicted: Boolean, key: ImageKey, oldValue: V, newValue: V?) {
            if (newValue != null) return
            synchronized(lock) {
                if (latestKeys[key.url] == key) latestKeys.remove(key.url)
            }
        }
    }

    operator fun get(key: ImageKey): V? = entries[key]

    /** The value most recently stored for [url], whatever its size. */
    fun latest(url: String): V? {
        val key = synchronized(lock) { latestKeys[url] } ?: return null
        return entries[key]
    }

    fun put(key: ImageKey, value: V) {
        synchronized(lock) { latestKeys[key.url] = key }
        entries.put(key, value)
    }
}

/**
 * One shared load per key. Every caller for a key awaits the same load, which runs in [scope];
 * it's cancelled only once every caller waiting on it has been cancelled, so one view leaving
 * never starves another. A finished load is forgotten, so a failure is retried by the next call.
 */
internal class InFlightLoads<K : Any, V>(private val scope: CoroutineScope) {

    private class Load<V>(val result: Deferred<V>) {
        var waiters = 0
    }

    private val lock = Any()
    private val loads = HashMap<K, Load<V>>()

    suspend fun load(key: K, block: suspend () -> V): V {
        val load = synchronized(lock) {
            val running = loads[key]?.takeIf { !it.result.isCompleted }
            (running ?: Load(scope.async { block() }).also { loads[key] = it }).also { it.waiters++ }
        }
        try {
            return load.result.await()
        } finally {
            synchronized(lock) {
                load.waiters--
                if (load.waiters == 0) {
                    if (loads[key] === load) loads.remove(key)
                    load.result.cancel()
                }
            }
        }
    }
}
