package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp

/**
 * A thumbnail of the art at [url]: a square [size] on a side in the small shape (`shapes.small`),
 * the picture cropped to cover it, with [icon] on a quiet fill while it loads, when it fails and
 * for a null [url]. The leading picture of a song's, an album's or an episode's row.
 *
 * ```kotlin
 * AMListRow(track.title, subtitle = track.artist, leadingContent = {
 *     AMArtThumbnail(track.coverUrl, contentDescription = null, icon = painterResource(R.drawable.ic_music_note))
 * })
 * ```
 *
 * The picture comes through the image pipeline as an [AMRemoteImage]'s does: kept in memory and
 * on disk, and there on the first frame when a row scrolls back. For art the app already holds as
 * a bitmap, use the `ImageBitmap` form.
 *
 * @param url where the picture lives; null shows the placeholder.
 * @param contentDescription what a screen reader says for the thumbnail, or null when what's
 *   beside it names it, as a row's title does. It's one image whatever shows.
 * @param modifier the thumbnail's placement.
 * @param icon the placeholder's glyph, centered at half the side in the muted content color
 *   ([AMRemoteImageFallback]). The default is the picture glyph.
 * @param size the side. The default is a [AMAvatarSize.Medium] avatar's, a row's leading picture;
 *   [AMAvatarSize.Large]'s suits a larger row.
 */
@Composable
public fun AMArtThumbnail(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    icon: Painter = painterResource(R.drawable.am_ic_image),
    size: Dp = AMAvatarSize.Medium.diameter,
) {
    AMRemoteImage(
        url,
        contentDescription,
        modifier
            .size(size)
            .clip(MaterialTheme.shapes.small),
        fallback = { AMRemoteImageFallback(icon = icon) },
    )
}

/**
 * A thumbnail of [image], art the app already holds as a bitmap (a cover embedded in a music
 * file), the way the URL form shows a picture it loads: a square [size] on a side in the small
 * shape, cropped, with [icon] on a quiet fill while [image] is null.
 *
 * ```kotlin
 * val cover: ImageBitmap? = track.embeddedCover   // null until it's read
 * AMArtThumbnail(cover, contentDescription = null)
 * ```
 *
 * @param image the picture; null (no art yet) shows the placeholder.
 * @param contentDescription what a screen reader says for the thumbnail, or null when what's
 *   beside it names it. It's one image whatever shows.
 * @param modifier the thumbnail's placement.
 * @param icon the placeholder's glyph, centered at half the side in the muted content color. The
 *   default is the picture glyph.
 * @param size the side. The default is a [AMAvatarSize.Medium] avatar's.
 */
@Composable
public fun AMArtThumbnail(
    image: ImageBitmap?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    icon: Painter = painterResource(R.drawable.am_ic_image),
    size: Dp = AMAvatarSize.Medium.diameter,
) {
    HeldImage(
        image,
        contentDescription,
        modifier
            .size(size)
            .clip(MaterialTheme.shapes.small),
        fallback = { AMRemoteImageFallback(icon = icon) },
    )
}
