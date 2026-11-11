package com.ideamtrls.androidmtrls

import android.content.Context
import android.graphics.ImageDecoder
import android.net.http.HttpResponseCache
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Loads, decodes and caches the pictures behind [AMRemoteImage] (avatars included) and the art
 * backdrops' photos. Not public API.
 *
 * - **Bytes** come from `HttpURLConnection` on the IO dispatcher, following redirects (https to
 *   https, and http up to https, never down), with the platform's [HttpResponseCache] on disk,
 *   installed once in the app's cache directory unless the app installed its own.
 * - **Decoding** runs off the main thread through [ImageDecoder], at the size the view needs
 *   ([decodeSize]), never larger than the picture itself.
 * - **Memory**: decoded bitmaps sit in an [ImageMemoryCache] keyed by URL, size and fit, which a
 *   view reads synchronously during composition, so a view that comes back (a scrolled row, a
 *   popped page) shows its picture on its first frame instead of the fallback.
 * - **One load per picture**: views asking for the same key while it loads share one request and
 *   one decode ([InFlightLoads]).
 *
 * Failures are never cached; the next request retries.
 */
internal class ImagePipeline private constructor(memoryBytes: Int) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val memory = ImageMemoryCache<DecodedImage>(memoryBytes) { it.byteCount }
    private val loads = InFlightLoads<ImageKey, DecodedImage>(scope)

    /** The decode stored for [key], if any. Safe to call during composition. */
    fun cached(key: ImageKey): DecodedImage? = memory[key]

    /**
     * The most recent decode of [url] at any size, if any: what a view shows on its first frame,
     * before it knows its own size. Safe to call during composition.
     */
    fun latest(url: String): DecodedImage? = memory.latest(url)

    /**
     * The picture for [key], from memory when it's there, otherwise joining or starting the one
     * load for it. Throws when the request fails or the bytes don't decode.
     */
    suspend fun load(key: ImageKey): DecodedImage {
        memory[key]?.let { return it }
        return loads.load(key) {
            val bytes = runInterruptible(Dispatchers.IO) { fetch(key.url) }
            val image = withContext(Dispatchers.Default) { decode(bytes, key) }
            memory.put(key, image)
            image
        }
    }

    companion object {
        /** The decoded-bitmap budget of the memory cache. */
        private const val MemoryBytes = 64 * 1024 * 1024

        /** The on-disk HTTP cache, used when the app hasn't installed one of its own. */
        private const val DiskBytes = 64L * 1024 * 1024

        /** Identifies the library to image hosts, some of which refuse requests without one. */
        private const val UserAgent = "AndroidMtrls/0.1 (Android demo)"

        private const val MaxRedirects = 5
        private const val ConnectTimeoutMillis = 15_000
        private const val ReadTimeoutMillis = 30_000

        @Volatile
        private var instance: ImagePipeline? = null

        /** The shared pipeline. The first call installs the disk cache. */
        fun shared(context: Context): ImagePipeline =
            instance ?: synchronized(this) {
                instance ?: run {
                    installDiskCache(context.applicationContext)
                    ImagePipeline(MemoryBytes).also { instance = it }
                }
            }

        private fun installDiskCache(context: Context) {
            if (HttpResponseCache.getInstalled() != null) return
            try {
                HttpResponseCache.install(File(context.cacheDir, "am_images"), DiskBytes)
            } catch (_: IOException) {
                // No disk cache: requests still work, they just aren't kept.
            }
        }

        /** The response body of [url], following redirects. Throws on anything but a 2xx. */
        private fun fetch(url: String): ByteArray {
            var current = URL(url)
            repeat(MaxRedirects + 1) {
                val connection = current.openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = ConnectTimeoutMillis
                    connection.readTimeout = ReadTimeoutMillis
                    connection.instanceFollowRedirects = true
                    connection.setRequestProperty("User-Agent", UserAgent)
                    val code = connection.responseCode
                    when (code) {
                        in 200..299 -> return connection.inputStream.use { it.readBytes() }
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

    /** Whether this decode is sharp enough for a [target] frame, so a bigger one isn't needed. */
    fun covers(target: IntSize, fit: ImageFit): Boolean {
        val needed = decodeSize(sourceSize, target, fit)
        return bitmap.width >= needed.width && bitmap.height >= needed.height
    }
}

/**
 * The size to decode a [source] picture at for a [target] frame: scaled down, keeping its aspect,
 * until it just covers the frame ([ImageFit.Cover]) or just fits inside it ([ImageFit.Contain]).
 * Never larger than the source; an empty source or target leaves the source size.
 */
internal fun decodeSize(source: IntSize, target: IntSize, fit: ImageFit): IntSize {
    if (source.width <= 0 || source.height <= 0 || target.width <= 0 || target.height <= 0) return source
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
