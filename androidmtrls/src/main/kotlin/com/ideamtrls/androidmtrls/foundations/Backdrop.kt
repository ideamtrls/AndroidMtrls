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
 * nearest backdrop wins: a row's own art beats the page's.
 *
 * **It's an art surface.** What lies on it takes its over-art look for its [tone]: section
 * headers and footers, empty states, ghost buttons, page headers, panel chrome and art text
 * ([amArtText], [AMTheme.artTone]). While it reaches the top of the window, the status bar's icons
 * follow its tone too, until a bar's frosted fill covers them ([AMStatusBarIcons]). The tone is the mode's by default,
 * which holds for the library's art ([amArtBackdrop], washed toward the mode). Art the app washes
 * less, or draws itself, gets the tone it has: `AMBackdrop(art = { Photo() }, tone = AMArtTone.Dark)`.
 *
 * @param art what sits behind the region: a photo, a blurred picture, a gradient.
 * @param modifier the region's size and placement.
 * @param tone how light [art] is, for what lies on it: null, the default, is the mode's.
 * @param content what sits on the art.
 */
@Composable
public fun AMBackdrop(
    art: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    tone: AMArtTone? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val source = remember(layer) { AMBackdropSource(layer) }
    val artTone = tone ?: modeArtTone()
    Box(modifier.then(artStatusBar(source, artTone))) {
        Box(Modifier.matchParentSize().backdropSource(source)) { art() }
        val scope = this
        CompositionLocalProvider(
            LocalAMBackdrop provides source,
            LocalAMArtTone provides artTone,
        ) { scope.content() }
    }
}
