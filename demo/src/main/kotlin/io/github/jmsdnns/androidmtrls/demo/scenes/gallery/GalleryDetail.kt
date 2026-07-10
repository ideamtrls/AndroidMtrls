package io.github.jmsdnns.androidmtrls.demo.scenes.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.collections.AMCarousel
import io.github.jmsdnns.androidmtrls.content.AMRemoteImage
import io.github.jmsdnns.androidmtrls.content.AMSkeleton
import io.github.jmsdnns.androidmtrls.content.AMTag
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButton
import io.github.jmsdnns.androidmtrls.demo.scenes.common.ScenePanelChrome
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMPausedScrim
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMPausedSubstrate
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// GalleryDetailPanel — the tabless photo detail, pushed as a panel
// ---------------------------------------------------------------------------

/**
 * Tabless photo detail, pushed as a panel. A paging [AMCarousel] (`sideInset = 0`)
 * drives [GalleryState.detailIndex] over an [AMPausedSubstrate] that crossfades
 * the current photo behind a stable blur + page wash. The transparent
 * [ScenePanelChrome] reserves the top bar above the photo — like the iOS panel's
 * top safe-area inset — so the square photo sits below the bar, never under it,
 * with the blurred backdrop showing through the transparent chrome. Below the
 * photo is a quiet metadata caption block — one date·location line, keyword
 * [AMTag]s, one camera·size caption — plus the favorite heart; the substrate
 * shows through the trailing Spacer. The chrome title tracks the paged photo.
 * Ports iOS `GalleryDetailView`.
 *
 * Photos resolve from the captured [ids] against the live [GalleryState.photos],
 * so a deletion elsewhere simply drops the photo from the carousel; the scene
 * pops the panel when the last one goes.
 */
@Composable
internal fun GalleryDetailPanel(state: GalleryState, ids: List<Int>) {
    val resolvedPhotos = ids.mapNotNull { id -> state.photos.firstOrNull { it.id == id } }

    if (resolvedPhotos.isEmpty()) {
        // The scene pops this panel on delete once it empties; render the page fill
        // in the interim frame so nothing bleeds through.
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val index = state.detailIndex.coerceIn(0, resolvedPhotos.lastIndex)
    val current = resolvedPhotos[index]

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-bleed blurred + page-washed substrate behind everything; crossfades
        // on the current id. Shows through the transparent chrome (and the status
        // bar) and the trailing Spacer, exactly as iOS's `.imPausedBackdrop`
        // full-bleed wash shows behind the safe-area-inset title bar.
        DetailBackdrop(resolvedPhotos, current)

        // Foreground: the transparent chrome reserves the top bar (the backdrop
        // shows behind it, mirroring iOS's top safe-area inset), then the panel
        // body — the square carousel over the metadata block, with the substrate
        // showing through the trailing Spacer. Same chrome-above-content Column as
        // every other scene panel (social post detail, gallery album, music).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // Transparent chrome — no fill, no hairline; the title tracks the paged
            // photo and the backdrop shows behind it. It sits ABOVE the photo so the
            // back arrow and title never collide with the image.
            ScenePanelChrome(
                title = current.title,
                onBack = { state.pop() },
                divider = false,
            )

            // panelBody top gap — iOS `.padding(.top, md)` between the title bar and
            // the photo.
            Spacer(modifier = Modifier.height(AMTheme.Spacing.md.dp))

            AMCarousel(
                items = resolvedPhotos,
                key = { it.id },
                currentIndex = index,
                onIndexChange = { state.detailIndex = it },
                sideInset = 0.dp,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            ) { photo ->
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val side = maxWidth
                    AMRemoteImage(
                        url = photo.fullUrl,
                        size = side,
                        cornerRadius = 0.dp,
                        contentScale = ContentScale.Crop,
                        contentDescription = photo.title,
                        fallback = { AMSkeleton.rect(width = side, height = side, cornerRadius = 0.dp) },
                    )
                }
            }

            DetailMetadata(state, current)

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Full-bleed blurred substrate. [AMPausedSubstrate] with [AMPausedScrim.Page]
 * bundles the blur, the translucent page wash, the full-bleed sizing, and the
 * cross-dissolve between photos — so nothing here animates by hand. The image is
 * a square sized to the screen's larger edge, cropped to fill, so the blurred
 * wash covers the whole panel regardless of aspect. Ports iOS
 * `GalleryDetailView.backdrop`.
 */
@Composable
private fun DetailBackdrop(resolvedPhotos: List<GalleryPhoto>, current: GalleryPhoto) {
    val isDark = LocalAMIsDarkTheme.current
    val config = LocalConfiguration.current
    val side = maxOf(config.screenWidthDp, config.screenHeightDp).dp

    AMPausedSubstrate(
        key = current.id,
        scrim = AMPausedScrim.Page,
        modifier = Modifier.fillMaxSize(),
    ) { id ->
        val photo = resolvedPhotos.firstOrNull { it.id == id } ?: current
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AMRemoteImage(
                url = photo.fullUrl,
                size = side,
                cornerRadius = 0.dp,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                fallback = {
                    Box(modifier = Modifier.fillMaxSize().background(AMTheme.Surface.page.resolve(isDark)))
                },
            )
        }
    }
}

/**
 * The quiet metadata caption block on `Surface.primary`: date·location, keyword
 * [AMTag]s, camera·fileSize, with the favorite heart overlaid at the trailing
 * center. The text column reserves a 44dp trailing gutter (the [AMIconButton] tap
 * target) so it never runs under the heart. Ports iOS `GalleryDetailView.metadata`.
 */
@Composable
private fun DetailMetadata(state: GalleryState, current: GalleryPhoto) {
    val isDark = LocalAMIsDarkTheme.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.primary.resolve(isDark))
            .padding(horizontal = AMTheme.Spacing.lg.dp, vertical = AMTheme.Spacing.lg.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(end = 44.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
        ) {
            BasicText(
                text = "${current.date} · ${current.location}",
                style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
                current.tags.forEach { tag -> AMTag(text = tag) }
            }
            BasicText(
                text = "${current.camera} · ${current.fileSize}",
                style = AMTheme.Type.caption.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
            )
        }

        // 44 = AMIconButton medium tap target — keeps the text clear of the heart.
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            AMIconButton(
                icon = if (current.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                active = current.isFavorite,
                contentDescription = if (current.isFavorite) "Remove favorite" else "Add favorite",
                onClick = { state.toggleFavorite(current.id) },
            )
        }
    }
}
