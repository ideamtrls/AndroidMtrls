package com.ideamtrls.androidmtrls

import java.io.File
import java.security.MessageDigest

/**
 * The image pipeline's own disk cache of picture bytes: one file per URL in its [directory], named
 * by the URL's SHA-256 in hex ([imageCacheFileName]), at most [maxBytes] in all, trimmed least
 * recently used first ([imageCacheEvictions]). Only the pipeline's own fetches read and fill it;
 * no other connection in the app goes through it.
 *
 * A picture is treated as immutable per URL: a stored body serves its URL until it's trimmed,
 * whatever the server's cache headers said. A new picture belongs at a new URL.
 *
 * Every call blocks on the disk, so call it off the main thread, and none throws: a broken cache
 * just misses, and the picture comes from the network.
 */
internal class ImageDiskCache private constructor(
    private val directory: File,
    private val maxBytes: Long,
    private val maxEntryBytes: Int,
) {
    private val lock = Any()

    /** The stored body for [url], marked as just used; null when there's none or it can't be read. */
    fun read(url: String): ByteArray? {
        val file = File(directory, imageCacheFileName(url))
        return try {
            if (!file.isFile) return null
            val bytes = file.inputStream().use { readBounded(it, maxEntryBytes, file.length()) }
            file.setLastModified(System.currentTimeMillis())
            bytes
        } catch (_: Exception) {
            deleteQuietly(file)
            null
        }
    }

    /**
     * Stores [bytes] as [url]'s body: written to a temporary file and renamed into place, so a
     * reader never sees part of a body; then trims the cache to its budget.
     */
    fun write(url: String, bytes: ByteArray) {
        try {
            directory.mkdirs()
            val name = imageCacheFileName(url)
            val temporary = File.createTempFile(name, TemporarySuffix, directory)
            try {
                temporary.writeBytes(bytes)
                if (!temporary.renameTo(File(directory, name))) return
            } finally {
                temporary.delete() // gone already once renamed
            }
            trim()
        } catch (_: Exception) {
            // Not stored: the next load fetches it again.
        }
    }

    /** Drops [url]'s stored body, if any: one that no longer decodes. */
    fun remove(url: String) {
        deleteQuietly(File(directory, imageCacheFileName(url)))
    }

    /** Deletes the least recently used bodies until the rest fit in [maxBytes]. */
    private fun trim() {
        synchronized(lock) {
            val files = directory.listFiles()?.filter { isImageCacheFileName(it.name) } ?: return
            val stored = files.map { ImageCacheFile(it.name, it.length(), it.lastModified()) }
            for (file in imageCacheEvictions(stored, maxBytes)) deleteQuietly(File(directory, file.name))
        }
    }

    companion object {
        private const val TemporarySuffix = ".tmp"

        /**
         * The cache in [directory], at most [maxBytes] in all, serving bodies of at most
         * [maxEntryBytes]. Temporary files a write left behind (the process died mid-write) are
         * deleted. Blocking.
         */
        fun open(directory: File, maxBytes: Long, maxEntryBytes: Int): ImageDiskCache {
            directory.mkdirs()
            directory.listFiles()?.filter { it.name.endsWith(TemporarySuffix) }?.forEach(::deleteQuietly)
            return ImageDiskCache(directory, maxBytes, maxEntryBytes)
        }

        private fun deleteQuietly(file: File) {
            try {
                file.delete()
            } catch (_: Exception) {
                // Left in place; the next trim or read tries again.
            }
        }
    }
}

/** The name a URL's body is stored under: the SHA-256 of the URL's UTF-8 bytes, in lowercase hex. */
internal fun imageCacheFileName(url: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
    val hex = StringBuilder(digest.size * 2)
    for (byte in digest) {
        val value = byte.toInt() and 0xFF
        hex.append(HexDigits[value ushr 4]).append(HexDigits[value and 0x0F])
    }
    return hex.toString()
}

/** Whether [name] is a stored body's name ([imageCacheFileName]), not a temporary file or a stranger. */
internal fun isImageCacheFileName(name: String): Boolean =
    name.length == 64 && name.all { it in HexDigits }

private const val HexDigits = "0123456789abcdef"

/** A stored body as trimming sees it: its file's [name], its size in [bytes], and when it was [lastUsed], in milliseconds. */
internal data class ImageCacheFile(val name: String, val bytes: Long, val lastUsed: Long)

/**
 * Which of [files] to delete so the rest come to at most [maxBytes]: the least recently used first
 * (the oldest [ImageCacheFile.lastUsed], ties broken by name), until the rest fit. None when they
 * already fit.
 */
internal fun imageCacheEvictions(files: List<ImageCacheFile>, maxBytes: Long): List<ImageCacheFile> {
    var total = files.sumOf { it.bytes }
    if (total <= maxBytes) return emptyList()
    val evicted = ArrayList<ImageCacheFile>()
    for (file in files.sortedWith(compareBy({ it.lastUsed }, { it.name }))) {
        if (total <= maxBytes) break
        evicted += file
        total -= file.bytes
    }
    return evicted
}
