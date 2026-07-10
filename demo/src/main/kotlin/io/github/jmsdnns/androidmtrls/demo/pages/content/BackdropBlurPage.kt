package io.github.jmsdnns.androidmtrls.demo.pages.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMRemoteImage
import io.github.jmsdnns.androidmtrls.content.AMSkeleton
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMBackdropBlur
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.amSquircle

// ---------------------------------------------------------------------------
// Backdrop Blur catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for the backdrop-blur demo page.
 *
 * id: "content-backdrop-blur", section: [ComponentSection.Content], tile
 * "Blur". Mirrors iOS `IMBackdropBlurPage`
 * (`Demo/Sources/Catalog/Pages/Content.swift`).
 */
val backdropBlurEntry: ComponentEntry = ComponentEntry(
    id       = "content-backdrop-blur",
    section  = ComponentSection.Content,
    title    = "Backdrop Blur",
    tileTitle = "Blur",
    subtitle = "Tint-free blur over content",
    intro    = "The raw Gaussian blur the modal overlays layer over whatever sits behind them — " +
        "the softened backdrop under a sheet, banner, or actions grid. A true blur of the content " +
        "below with no vibrancy tint, so the substrate's own color and brightness come through. " +
        "Default radius roughly matches the system's thin material.",
    icon     = Icons.Filled.BlurOn,
    content  = { BackdropBlurPage() },
)

// ---------------------------------------------------------------------------
// Photo source + metrics
// ---------------------------------------------------------------------------

// Same photo source the Gallery scene uses (the iOS `iosmtrls-` seed is kept
// verbatim so the softening is easy to read: the image is shown crisp, then
// again under the backdrop blur).
private const val PHOTO_URL = "https://picsum.photos/seed/iosmtrls-blur/600/600"
private val IMAGE_SIZE: Dp = 300.dp

// ---------------------------------------------------------------------------
// BackdropBlurPage
// ---------------------------------------------------------------------------

/**
 * Demo page for the backdrop blur.
 *
 * - **Default** — a crisp remote image.
 * - **Blurred** — the same image wrapped in [AMBackdropBlur], plus the caption
 *   copy.
 *
 * [AMBackdropBlur] is the public tint-free Gaussian blur — a pure frosted
 * softening of the content it wraps, with no vibrancy tint and no scrim in
 * either light or dark mode, matching iOS `IMBackdropBlur`.
 *
 * **Blur note:** the blur renders as a no-op in Android Studio preview; validate
 * on a physical device or emulator (API 31+).
 */
@Composable
fun BackdropBlurPage() {
    catalogGroup(title = "Default") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            contentAlignment = Alignment.Center,
        ) {
            PhotoBlock()
        }
    }

    catalogGroup(title = "Blurred") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
        ) {
            PhotoBlock(blurred = true)
            BlurCaption(
                "The same image with the backdrop blur layered over it — softened, no tint.",
            )
        }
    }
}

/**
 * The demo photo. When [blurred] is true the image is wrapped in [AMBackdropBlur]
 * (clipped to the squircle so the blur's edge bleed is contained) so it renders
 * softened; otherwise it renders crisp. Falls back to a shimmering skeleton while
 * the remote image loads.
 */
@Composable
private fun PhotoBlock(blurred: Boolean = false) {
    if (blurred) {
        AMBackdropBlur(
            modifier = Modifier
                .size(IMAGE_SIZE)
                .clip(amSquircle(AMTheme.Radius.lg.dp)),
        ) {
            DemoPhoto()
        }
    } else {
        Box(modifier = Modifier.size(IMAGE_SIZE)) {
            DemoPhoto()
        }
    }
}

/** The remote demo photo, sized to [IMAGE_SIZE], with a skeleton fallback. */
@Composable
private fun DemoPhoto() {
    AMRemoteImage(
        url = PHOTO_URL,
        size = IMAGE_SIZE,
        cornerRadius = AMTheme.Radius.lg.dp,
        contentDescription = "Sample photo",
        fallback = {
            AMSkeleton.rect(
                width = IMAGE_SIZE,
                height = IMAGE_SIZE,
                cornerRadius = AMTheme.Radius.lg.dp,
            )
        },
    )
}

/** Centered secondary caption under the blurred specimen. */
@Composable
private fun BlurCaption(text: String) {
    val isDark = LocalAMIsDarkTheme.current
    BasicText(
        text  = text,
        modifier = Modifier.fillMaxWidth(),
        style = AMTheme.Type.caption
            .toTextStyle(AMTheme.Text.secondary.resolve(isDark))
            .copy(textAlign = TextAlign.Center),
    )
}
