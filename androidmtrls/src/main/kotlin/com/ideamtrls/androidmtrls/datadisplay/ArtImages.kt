package com.ideamtrls.androidmtrls

import android.graphics.Bitmap
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.lang.ref.WeakReference
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// The pictures behind the art and page backdrops and the art rows, made once and kept: the photo
// (decoded small through the image pipeline, or a bitmap the app hands over) cropped to its frame,
// averaged down, blurred, saturated through a color matrix and cached. A heavy blur leaves nothing
// a small picture can't hold, so each is a few dozen pixels across and the GPU stretches it to its
// frame.

/**
 * Where an art picture's photo comes from: the picture at a URL, or a bitmap the app already holds
 * (a cover embedded in a music file).
 */
internal sealed interface ArtSource {
    /** What the processed pictures made from this photo are keyed by: equal for the same photo. */
    val id: Any

    /** The photo at [url], decoded small through the image pipeline. */
    data class Url(val url: String) : ArtSource {
        override val id: Any get() = url
    }

    /**
     * The app's [image], known by the bitmap under it, so two sources are equal for the same bitmap
     * however often the app wraps it. Its [id] holds the bitmap only weakly: a cached picture never
     * keeps an app's bitmap alive. The bitmap is read as it stands; new art belongs in a new bitmap.
     */
    class Image(val image: ImageBitmap) : ArtSource {
        override val id: ArtIdentity = ArtIdentity(image.asAndroidBitmap())

        override fun equals(other: Any?): Boolean = other is Image && other.id == id

        override fun hashCode(): Int = id.hashCode()
    }
}

/**
 * Stands for an object by its identity without keeping it alive: equal to another identity of the
 * same object while that object lives, and only to itself once it's gone.
 */
internal class ArtIdentity(target: Any) {
    private val target = WeakReference(target)
    private val hash = System.identityHashCode(target)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is ArtIdentity || other.hash != hash) return false
        val mine = target.get() ?: return false
        return mine === other.target.get()
    }

    override fun hashCode(): Int = hash
}

/**
 * One processed picture: the photo known by [photo] (its source's [ArtSource.id]: the URL, or the
 * app bitmap's identity), at a working size of [width] × [height] pixels in its frame's shape,
 * blurred by a sigma of [sigma] working pixels, saturated by [saturation] and, for an art row's
 * band, fading in over its top [fade] working pixels. The fractional values are kept in hundredths,
 * so equal recipes make equal keys.
 */
internal data class ArtKey(
    val photo: Any,
    val width: Int,
    val height: Int,
    val sigma: Int,
    val saturation: Int,
    val fade: Int,
)

/**
 * The size a frame's picture is worked at, [scale] times the frame, and its blur there, [sigma]
 * working pixels.
 */
internal class ArtWorkingSize(val width: Int, val height: Int, val sigma: Float, val scale: Float)

/**
 * The blur a picture is worked with: enough for a smooth picture once it's stretched back to its
 * frame. A heavier design blur works the picture smaller, at this blur.
 */
private const val ArtWorkingSigma = 8f

/** The largest a picture is worked at, against its frame: half, for the lightest blurs. */
private const val ArtMaxWorkingScale = 0.5f

/**
 * The working size for a [frame] of pixels blurred by a Gaussian of [sigma] pixels: scaled so the
 * blur is [ArtWorkingSigma] working pixels, never above half the frame, never below a pixel.
 */
internal fun artWorkingSize(frame: IntSize, sigma: Float): ArtWorkingSize {
    val scale = if (sigma <= 0f) ArtMaxWorkingScale else min(ArtMaxWorkingScale, ArtWorkingSigma / sigma)
    return ArtWorkingSize(
        width = max(1, (frame.width * scale).roundToInt()),
        height = max(1, (frame.height * scale).roundToInt()),
        sigma = sigma * scale,
        scale = scale,
    )
}

/**
 * The Gaussian sigma Android's blur [radius] stands for, as the platform converts it: about
 * `0.577 × radius + 0.5`. A radius of 0 is no blur.
 */
internal fun blurSigma(radius: Float): Float = if (radius <= 0f) 0f else 0.57735f * radius + 0.5f

/**
 * The key for [source]'s picture filling a [frame] of pixels, blurred by Android's blur
 * [blurRadius] in pixels, saturated by [saturation], its top [fade] pixels fading in.
 */
internal fun artKey(source: ArtSource, frame: IntSize, blurRadius: Float, saturation: Float, fade: Float): ArtKey {
    val working = artWorkingSize(frame, blurSigma(blurRadius))
    return ArtKey(
        photo = source.id,
        width = working.width,
        height = working.height,
        sigma = (working.sigma * 100f).roundToInt(),
        saturation = (saturation * 100f).roundToInt(),
        fade = (fade * working.scale * 100f).roundToInt(),
    )
}

/**
 * The processed pictures: made once per key, kept in memory, and made once however many
 * backdrops ask for the same one at the same time. Failures aren't kept; the next request retries.
 */
internal object ArtImages {
    /** The processed pictures' budget. Each is small; this holds hundreds. */
    private const val MaxBytes = 16 * 1024 * 1024

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val loads = InFlightLoads<ArtKey, ImageBitmap>(scope)
    private val cache = object : LruCache<ArtKey, ImageBitmap>(MaxBytes) {
        override fun sizeOf(key: ArtKey, value: ImageBitmap): Int = value.width * value.height * 4
    }

    /** The picture made for [key], if it's in memory. Safe to call while composing or drawing. */
    fun cached(key: ArtKey): ImageBitmap? = cache[key]

    /**
     * The picture for [key], made from [source] (the source [key] was made for): from memory, or
     * made now. A URL's photo comes from [pipeline]: a decode already in memory when it's large
     * enough, otherwise one decoded at the working size. An app's bitmap is used as it is. Throws
     * when the photo can't be loaded or read.
     */
    suspend fun load(pipeline: ImagePipeline, source: ArtSource, key: ArtKey): ImageBitmap {
        cache[key]?.let { return it }
        return loads.load(key) {
            val photo = when (source) {
                is ArtSource.Url -> {
                    val working = IntSize(key.width, key.height)
                    val decoded = pipeline.latest(source.url)?.takeIf { it.covers(working, ImageFit.Cover) }
                        ?: pipeline.load(ImageKey(source.url, key.width, key.height, ImageFit.Cover))
                    decoded.bitmap
                }
                is ArtSource.Image -> source.image
            }
            val image = processArt(photo, key)
            cache.put(key, image)
            image
        }
    }
}

/**
 * Makes [key]'s picture from a photo: cropped to the frame's shape around its center, averaged
 * down to the working size, blurred with its edges clamped, saturated, faded in at the top for a
 * band, then handed to the GPU. A photo smaller than the working size is worked at its own size,
 * never scaled up here. The photo itself is never changed.
 */
private fun processArt(photo: ImageBitmap, key: ArtKey): ImageBitmap {
    val decoded = photo.asAndroidBitmap()
    val readable = if (decoded.config == Bitmap.Config.HARDWARE) decoded.copy(Bitmap.Config.ARGB_8888, false) else decoded
    val sourceWidth = readable.width
    val sourceHeight = readable.height
    val colors = IntArray(sourceWidth * sourceHeight)
    readable.getPixels(colors, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight)
    if (readable !== decoded) readable.recycle()

    val crop = coverCrop(sourceWidth, sourceHeight, key.width, key.height)
    val shrink = min(1f, min(crop.width / key.width, crop.height / key.height))
    val width = max(1, (key.width * shrink).roundToInt())
    val height = max(1, (key.height * shrink).roundToInt())

    val pixels = areaAverage(colors, sourceWidth, sourceHeight, crop, width, height)
    gaussianBlur(pixels, width, height, key.sigma / 100f * shrink)
    saturate(pixels, key.saturation / 100f)
    fadeIn(pixels, width, height, key.fade / 100f * shrink)

    val bitmap = Bitmap.createBitmap(unpremultipliedColors(pixels), width, height, Bitmap.Config.ARGB_8888)
    bitmap.prepareToDraw()
    return bitmap.asImageBitmap()
}

/** A rectangle of a picture, in its pixels. */
internal class ArtCrop(val left: Float, val top: Float, val width: Float, val height: Float)

/**
 * The part of a [sourceWidth] × [sourceHeight] picture that covers a [frameWidth] × [frameHeight]
 * frame: the frame's shape, as large as the picture allows, centered, the way a cropping image
 * fills its frame.
 */
internal fun coverCrop(sourceWidth: Int, sourceHeight: Int, frameWidth: Int, frameHeight: Int): ArtCrop {
    val scale = max(frameWidth / sourceWidth.toFloat(), frameHeight / sourceHeight.toFloat())
    val width = min(frameWidth / scale, sourceWidth.toFloat())
    val height = min(frameHeight / scale, sourceHeight.toFloat())
    return ArtCrop((sourceWidth - width) / 2f, (sourceHeight - height) / 2f, width, height)
}

/**
 * [crop] of a picture of ARGB [colors], averaged down to [width] × [height]: each pixel the mean
 * of the source pixels under it. Returns premultiplied red, green, blue and alpha, 0 to 1, four
 * floats a pixel.
 */
internal fun areaAverage(
    colors: IntArray,
    sourceWidth: Int,
    sourceHeight: Int,
    crop: ArtCrop,
    width: Int,
    height: Int,
): FloatArray {
    val out = FloatArray(width * height * 4)
    val stepX = crop.width / width
    val stepY = crop.height / height
    for (y in 0 until height) {
        val y0 = floor(crop.top + y * stepY).toInt().coerceIn(0, sourceHeight - 1)
        val y1 = ceil(crop.top + (y + 1) * stepY).toInt().coerceIn(y0 + 1, sourceHeight)
        for (x in 0 until width) {
            val x0 = floor(crop.left + x * stepX).toInt().coerceIn(0, sourceWidth - 1)
            val x1 = ceil(crop.left + (x + 1) * stepX).toInt().coerceIn(x0 + 1, sourceWidth)
            var red = 0f
            var green = 0f
            var blue = 0f
            var alpha = 0f
            for (sy in y0 until y1) {
                for (sx in x0 until x1) {
                    val color = colors[sy * sourceWidth + sx]
                    val a = (color ushr 24) / 255f
                    red += ((color shr 16) and 0xFF) / 255f * a
                    green += ((color shr 8) and 0xFF) / 255f * a
                    blue += (color and 0xFF) / 255f * a
                    alpha += a
                }
            }
            val count = ((x1 - x0) * (y1 - y0)).toFloat()
            val index = (y * width + x) * 4
            out[index] = red / count
            out[index + 1] = green / count
            out[index + 2] = blue / count
            out[index + 3] = alpha / count
        }
    }
    return out
}

/**
 * Blurs four-float [pixels] in place with a Gaussian of [sigma] pixels, horizontally then
 * vertically, repeating the edge pixels past the picture's edges, so it never darkens toward
 * them. A sigma under a quarter pixel leaves the picture as it is.
 */
internal fun gaussianBlur(pixels: FloatArray, width: Int, height: Int, sigma: Float) {
    if (sigma < 0.25f) return
    val radius = ceil(sigma * 3f).toInt()
    val kernel = FloatArray(radius * 2 + 1) { i ->
        val distance = (i - radius).toFloat()
        exp(-distance * distance / (2f * sigma * sigma))
    }
    val total = kernel.sum()
    for (i in kernel.indices) kernel[i] /= total

    val scratch = FloatArray(pixels.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val out = (y * width + x) * 4
            for (k in -radius..radius) {
                val weight = kernel[k + radius]
                val from = (y * width + (x + k).coerceIn(0, width - 1)) * 4
                for (channel in 0 until 4) scratch[out + channel] += pixels[from + channel] * weight
            }
        }
    }
    pixels.fill(0f)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val out = (y * width + x) * 4
            for (k in -radius..radius) {
                val weight = kernel[k + radius]
                val from = ((y + k).coerceIn(0, height - 1) * width + x) * 4
                for (channel in 0 until 4) pixels[out + channel] += scratch[from + channel] * weight
            }
        }
    }
}

/**
 * Saturates premultiplied four-float [pixels] in place by [saturation] through Material's
 * saturation color matrix (1 leaves them as they are, 2 doubles the color), clamping each channel
 * to its alpha.
 */
internal fun saturate(pixels: FloatArray, saturation: Float) {
    if (saturation == 1f) return
    val m = ColorMatrix().apply { setToSaturation(saturation) }.values
    for (i in pixels.indices step 4) {
        val red = pixels[i]
        val green = pixels[i + 1]
        val blue = pixels[i + 2]
        val alpha = pixels[i + 3]
        pixels[i] = (m[0] * red + m[1] * green + m[2] * blue).coerceIn(0f, alpha)
        pixels[i + 1] = (m[5] * red + m[6] * green + m[7] * blue).coerceIn(0f, alpha)
        pixels[i + 2] = (m[10] * red + m[11] * green + m[12] * blue).coerceIn(0f, alpha)
    }
}

/** Fades premultiplied four-float [pixels] in from transparent over their top [fade] rows. */
internal fun fadeIn(pixels: FloatArray, width: Int, height: Int, fade: Float) {
    if (fade <= 0f) return
    for (y in 0 until height) {
        val opacity = (y + 0.5f) / fade
        if (opacity >= 1f) return
        for (i in y * width * 4 until (y + 1) * width * 4) pixels[i] *= opacity
    }
}

/** Packs premultiplied four-float pixels into the unpremultiplied ARGB colors a bitmap takes. */
private fun unpremultipliedColors(pixels: FloatArray): IntArray = IntArray(pixels.size / 4) { pixel ->
    val i = pixel * 4
    val alpha = pixels[i + 3].coerceIn(0f, 1f)
    if (alpha <= 0f) {
        0
    } else {
        fun channel(value: Float): Int = ((value / alpha).coerceIn(0f, 1f) * 255f).roundToInt()
        ((alpha * 255f).roundToInt() shl 24) or (channel(pixels[i]) shl 16) or
            (channel(pixels[i + 1]) shl 8) or channel(pixels[i + 2])
    }
}

/** One art row's place in its container: its top and bottom, in the container's pixels. */
internal class ArtSpan(val top: Float, val bottom: Float)

/**
 * Where each art row's color lies, given the rows' [spans] in page order in a container [height]
 * pixels tall: each band reaches [blend] past the midpoint to its neighbor on both sides, so two
 * rows' colors meet halfway between them, and fades in over its top `2 × blend` (none for the
 * first). The first and last bands run [bleed] past the container's ends, for overscroll and short
 * pages. Returns one band per span, each at least a pixel tall, with its fade in [ArtBand.fade].
 */
internal fun artRowBands(spans: List<ArtSpan>, height: Float, blend: Float, bleed: Float): List<ArtBand> =
    spans.indices.map { i ->
        val top = if (i == 0) -bleed else (spans[i - 1].bottom + spans[i].top) / 2f - blend
        val bottom = if (i == spans.lastIndex) height + bleed else (spans[i].bottom + spans[i + 1].top) / 2f + blend
        val bandHeight = max(bottom - top, 1f)
        ArtBand(top, top + bandHeight, fade = if (i == 0) 0f else min(2f * blend, bandHeight))
    }

/** One art row's band of color: its top and bottom, and how far down it fades in. */
internal class ArtBand(val top: Float, val bottom: Float, val fade: Float)
