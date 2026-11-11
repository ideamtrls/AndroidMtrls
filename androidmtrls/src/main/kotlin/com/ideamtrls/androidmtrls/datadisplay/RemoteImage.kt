package com.ideamtrls.androidmtrls

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.IntSize
import kotlin.coroutines.cancellation.CancellationException

/**
 * A picture loaded from the web, with a fallback while it loads: one call instead of a loading
 * state machine at every call site.
 *
 * The image fills the size its [modifier] gives it, like a resizable picture, so give it one: a
 * size, a width and a height, or an aspect ratio. In a scrolling column it needs a height or an
 * aspect ratio from outside; a sizeless image in an unbounded direction is zero long there.
 *
 * ```kotlin
 * AMRemoteImage(photo.url, contentDescription = photo.title, modifier = Modifier.size(96.dp).clip(MaterialTheme.shapes.small))
 *
 * AMRemoteImage(post.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f))
 * ```
 *
 * It draws no shape of its own; clip it with `Modifier.clip` for rounded corners, which clips the
 * fallback too.
 *
 * Loading, a failed load and a null [url] all show [fallback], laid over the whole frame. The
 * picture comes through the library's image pipeline: decoded at the size the frame needs, kept
 * in memory and on disk, and loaded once however many images show it. A picture that's already
 * in memory shows on the first frame, so a row scrolled back into view or a page returned to
 * never flashes its fallback. A failure isn't remembered; the image tries again the next time it
 * appears.
 *
 * @param url where the picture lives; null shows the fallback.
 * @param contentDescription what a screen reader says for the picture. The frame is one image
 *   whatever is showing, the fallback included. Null marks the picture decorative, and a screen
 *   reader skips it.
 * @param modifier the frame: its size, clip and placement.
 * @param contentScale how the picture fills the frame. The default crops it to cover the frame,
 *   so slots never letterbox; pass [ContentScale.Fit] to show all of it.
 * @param fallback shown while loading, on failure and for a null [url], filling the frame. The
 *   default is [AMRemoteImageFallback].
 */
@Composable
public fun AMRemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit = { AMRemoteImageFallback() },
) {
    val context = LocalContext.current
    val pipeline = remember(context) { ImagePipeline.shared(context) }
    val fit = if (contentScale == ContentScale.Fit || contentScale == ContentScale.Inside) ImageFit.Contain else ImageFit.Cover

    // Keyed by the URL, so a changed URL never shows the previous picture. A decode of this URL
    // already in memory shows on the first frame, before layout says how big the frame is.
    var image by remember(url) { mutableStateOf(url?.let(pipeline::latest)) }
    var frame by remember { mutableStateOf(IntSize.Zero) }

    if (url != null && frame != IntSize.Zero) {
        LaunchedEffect(url, frame, fit) {
            if (image?.covers(frame, fit) == true) return@LaunchedEffect
            val loaded = try {
                pipeline.load(ImageKey(url, frame.width, frame.height, fit))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (loaded != null) image = loaded
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { frame = it }
            .clearAndSetSemantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                    role = Role.Image
                }
            },
        propagateMinConstraints = true,
    ) {
        val current = image
        if (current != null) {
            Image(current.bitmap, contentDescription = null, contentScale = contentScale)
        } else {
            fallback()
        }
    }
}

/**
 * The default [AMRemoteImage] fallback: a quiet tonal fill over the image's whole frame, with an
 * optional glyph centered on it at half the frame's shorter side, in the muted content color.
 *
 * Pass it as an image's `fallback` to give it a glyph:
 *
 * ```kotlin
 * AMRemoteImage(url, contentDescription = null, fallback = { AMRemoteImageFallback(icon = painterResource(R.drawable.ic_person)) })
 * ```
 *
 * @param modifier its placement; it fills the frame it's given.
 * @param icon a glyph to center on the fill, or null for the fill alone.
 */
@Composable
public fun AMRemoteImageFallback(modifier: Modifier = Modifier, icon: Painter? = null) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.5f).aspectRatio(1f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
