package com.ideamtrls.androidmtrls

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap

/**
 * The art at [url] as a card: the picture cropped to cover the frame [modifier] gives (usually a
 * square), in the large shape (`shapes.large`) with the [AMElevation.Low] shadow, and the page
 * color in its place while it loads, when it fails and for a null [url]. An album's or a show's
 * hero, a tile in a grid of them.
 *
 * ```kotlin
 * AMArtCard(album.coverUrl, contentDescription = null, Modifier.fillMaxWidth().aspectRatio(1f))
 * ```
 *
 * Like an [AMRemoteImage], it fills the size its [modifier] gives it, so give it one: a size, or a
 * width and an aspect ratio. The picture comes through the image pipeline and shows on the first
 * frame when it's in memory. For art the app already holds as a bitmap, use the `ImageBitmap`
 * form.
 *
 * The card takes no input. When it belongs to a larger control (a tile whose art and title are one
 * button), pass that control's [interactionSource] and the control's ripple shows on the card,
 * inside its shape, with the control's own indication set to none.
 *
 * @param url where the picture lives; null shows the page color.
 * @param contentDescription what a screen reader says for the art, or null when what's beside it
 *   names it, as a title does. It's one image whatever shows.
 * @param modifier the card's frame: its size and placement.
 * @param interactionSource the interactions of the control the card belongs to, whose ripple the
 *   card shows; null shows none.
 */
@Composable
public fun AMArtCard(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    interactionSource: InteractionSource? = null,
) {
    AMRemoteImage(
        url,
        contentDescription,
        artCardFrame(modifier, interactionSource),
        fallback = { ArtCardLoading() },
    )
}

/**
 * [image], art the app already holds as a bitmap (a cover embedded in a music file), as a card,
 * the way the URL form shows a picture it loads: cropped to the frame [modifier] gives, in the
 * large shape with the [AMElevation.Low] shadow, and the page color while [image] is null.
 *
 * ```kotlin
 * val cover: ImageBitmap? = album.embeddedCover   // null until it's read
 * AMArtCard(cover, contentDescription = null, Modifier.size(240.dp))
 * ```
 *
 * @param image the picture; null (no art yet) shows the page color.
 * @param contentDescription what a screen reader says for the art, or null when what's beside it
 *   names it. It's one image whatever shows.
 * @param modifier the card's frame: its size and placement.
 * @param interactionSource the interactions of the control the card belongs to, whose ripple the
 *   card shows; null shows none.
 */
@Composable
public fun AMArtCard(
    image: ImageBitmap?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    interactionSource: InteractionSource? = null,
) {
    HeldImage(
        image,
        contentDescription,
        artCardFrame(modifier, interactionSource),
        fallback = { ArtCardLoading() },
    )
}

/**
 * The card around the art: the low shadow and the large shape, then the ripple of
 * [interactionSource], if any, clipped inside it.
 */
@Composable
private fun artCardFrame(modifier: Modifier, interactionSource: InteractionSource?): Modifier {
    val shape = MaterialTheme.shapes.large
    val ripple = if (interactionSource != null) Modifier.indication(interactionSource, LocalIndication.current) else Modifier
    return modifier
        .amElevation(AMElevation.Low, shape)
        .clip(shape)
        .then(ripple)
}

/** The page color, filling the card until its art is there. */
@Composable
private fun ArtCardLoading() {
    Box(
        Modifier
            .fillMaxSize()
            .background(AMTheme.Surface.page),
    )
}
