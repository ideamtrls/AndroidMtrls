package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.rememberGraphicsLayer

/**
 * Art behind a region, for frosted surfaces to sit on: [art] draws behind [content], filling the
 * region, and every [AMSurfaceFill.Frosted] surface in [content] blurs its own slice of it.
 *
 * ```kotlin
 * AMBackdrop(art = { AMRemoteImage(cover, contentDescription = null) }) {
 *     LazyColumn { items(tracks) { TrackCard(it, Modifier.amCard(fill = AMSurfaceFill.Frosted)) } }
 * }
 * ```
 *
 * The region takes its size from [modifier] and [content]; [art] fills it. Only [art] is sampled,
 * never the content, so a frosted surface never blurs itself or another frosted surface. The
 * nearest backdrop wins: a row's own art beats the page's. Inside it, section headers sit on the
 * picture ([AMSectionHeader] switches to its over-art look).
 *
 * @param art what sits behind the region: a photo, a blurred picture, a gradient.
 * @param modifier the region's size and placement.
 * @param content what sits on the art.
 */
@Composable
public fun AMBackdrop(
    art: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val source = remember(layer) { AMBackdropSource(layer) }
    Box(modifier) {
        Box(Modifier.matchParentSize().backdropSource(source)) { art() }
        val scope = this
        CompositionLocalProvider(LocalAMBackdrop provides source) { scope.content() }
    }
}
