package io.github.jmsdnns.androidmtrls.content

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amSquircle

// ---------------------------------------------------------------------------
// AMRemoteImageFallback — public fallback slot fill
// ---------------------------------------------------------------------------

/**
 * Default fallback content for [AMRemoteImage] when no image is available.
 *
 * Renders a light-gray tinted box with a centered [icon] at 50% of [size].
 * Mirrors iOS `IMRemoteImageFallback`: secondary fill + photo SF Symbol glyph
 * at half the slot dimension.
 *
 * Used internally by the default [AMRemoteImage] overload, and available for
 * callers that want the standard fallback in other contexts.
 *
 * @param size     The slot size — both fill and icon are sized relative to this.
 * @param modifier Modifier applied to the fallback's root box.
 * @param icon     The glyph to center in the fallback slot.
 *                 Default: [Icons.Default.AccountBox] (closest available core-icon analog
 *                 to the iOS `"photo"` SF Symbol). Callers with `material-icons-extended`
 *                 may pass `Icons.Default.Image` for a closer match.
 */
@Composable
fun AMRemoteImageFallback(
    size: Dp,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.AccountBox,
) {
    val isDark = LocalAMIsDarkTheme.current
    val fillColor = AMTheme.Text.secondary.resolve(isDark).copy(alpha = 0.10f)
    val iconColor = AMTheme.Text.secondary.resolve(isDark)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fillColor),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(size * 0.5f),
            colorFilter = ColorFilter.tint(iconColor),
        )
    }
}

// ---------------------------------------------------------------------------
// AMRemoteImage — default fallback overload
// ---------------------------------------------------------------------------

/**
 * Async remote image with a built-in icon fallback.
 *
 * Loads [url] via Coil 3. While loading or on error, and when [url] is null,
 * renders [AMRemoteImageFallback] with [fallbackIcon] inside the clipped slot.
 *
 * ## Loading / error treatments
 * - **Loading** (`placeholder` state): fallback immediately — skeleton shimmer
 *   is the caller's responsibility if desired.
 * - **Error** (`error` state): fallback — gray tinted box with glyph icon.
 * - **url == null**: fallback without attempting any network request.
 *
 * ## Corner radius
 * Pass [cornerRadius] explicitly, or leave null to inherit [LocalAMTokens].radius.sm
 * (8dp by default). Passing `0.dp` yields a square slot.
 *
 * ## iOS mapping
 * `IMRemoteImage` / `IMRemoteImageFallback` → `AMRemoteImage` / `AMRemoteImageFallback`.
 * `ContentMode.fill` → [ContentScale.Crop]; `ContentMode.fit` → [ContentScale.Fit].
 * iOS `"photo"` SF Symbol → default [fallbackIcon] ([Icons.Default.AccountBox] from core;
 * callers with `material-icons-extended` may pass `Icons.Default.Image`).
 *
 * @param url          Remote image URL. Null → fallback immediately, no network request.
 * @param size         Slot dimension (square). Both width and height are set to this value.
 * @param modifier     Modifier applied to the image's root slot (size + clip).
 * @param cornerRadius Corner radius applied to the clip shape.
 *                     Null → [LocalAMTokens].radius.sm (8dp).
 * @param contentScale How the image fills the slot. Default: [ContentScale.Crop] (.fill analog).
 * @param contentDescription Accessibility label for the image. Pass null when decorative.
 * @param fallbackIcon [ImageVector] centered in the fallback slot at 50% of [size].
 *                     Default: [Icons.Default.AccountBox].
 */
@Composable
fun AMRemoteImage(
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp? = null,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    fallbackIcon: ImageVector = Icons.Default.AccountBox,
) {
    AMRemoteImage(
        url = url,
        size = size,
        modifier = modifier,
        cornerRadius = cornerRadius,
        contentScale = contentScale,
        contentDescription = contentDescription,
        fallback = { AMRemoteImageFallback(size = size, icon = fallbackIcon) },
    )
}

// ---------------------------------------------------------------------------
// AMRemoteImage — custom fallback slot overload
// ---------------------------------------------------------------------------

/**
 * Async remote image with a custom composable fallback slot.
 *
 * Same behavior as the icon-fallback overload but lets callers provide any
 * composable as the fallback content — useful for skeleton shimmer, branded
 * illustrations, or initials-style placeholders.
 *
 * The [fallback] slot fills the clipped [size] × [size] box; use
 * `Modifier.fillMaxSize()` inside [fallback] to fill the slot edge-to-edge.
 *
 * @param url          Remote image URL. Null → [fallback] immediately.
 * @param size         Slot dimension (square).
 * @param modifier     Modifier applied to the image's root slot (size + clip).
 * @param cornerRadius Corner radius applied to the clip shape.
 *                     Null → [LocalAMTokens].radius.sm (8dp).
 * @param contentScale How the image fills the slot. Default: [ContentScale.Crop].
 * @param contentDescription Accessibility label for the image. Pass null when decorative.
 * @param fallback     Composable content shown while loading, on error, or when url is null.
 */
@Composable
fun AMRemoteImage(
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp? = null,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    fallback: @Composable () -> Unit,
) {
    val tokens = LocalAMTokens.current
    val radius = cornerRadius ?: tokens.radius.sm
    val shape = amSquircle(radius)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape),
    ) {
        if (url != null) {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                loading = { fallback() },
                error = { fallback() },
                success = {
                    SubcomposeAsyncImageContent(
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale,
                    )
                },
            )
        } else {
            fallback()
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (shows placeholder/error states since no network)
// ---------------------------------------------------------------------------

@Composable
private fun AMRemoteImagePreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Box(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
        ) {
            // Sizes row — null URL → immediate fallback (no network in preview)
            Row(
                horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AMRemoteImage(
                    url = null,
                    size = 48.dp,
                    contentDescription = "48dp slot",
                )
                AMRemoteImage(
                    url = null,
                    size = 80.dp,
                    contentDescription = "80dp slot",
                )
                AMRemoteImage(
                    url = null,
                    size = 120.dp,
                    contentDescription = "120dp slot",
                )
            }

            // Explicit corner radii row
            Row(
                horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 0dp radius → square
                AMRemoteImage(
                    url = null,
                    size = 64.dp,
                    cornerRadius = 0.dp,
                    contentDescription = "Square",
                )
                // md radius
                AMRemoteImage(
                    url = null,
                    size = 64.dp,
                    cornerRadius = AMTheme.Radius.md.dp,
                    contentDescription = "Radius md",
                )
                // lg radius
                AMRemoteImage(
                    url = null,
                    size = 64.dp,
                    cornerRadius = AMTheme.Radius.lg.dp,
                    contentDescription = "Radius lg",
                )
            }

            // Error state fallback — broken URL (network in emulator/preview won't reach this)
            Row(
                horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Standalone fallback component
                AMRemoteImageFallback(size = 64.dp)
            }
        }
    }
}

@Preview(
    name = "AMRemoteImage · Light",
    group = "AMRemoteImage",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
)
@Composable
private fun AMRemoteImagePreviewLight() {
    AMThemeRoot {
        AMRemoteImagePreviewContent()
    }
}

@Preview(
    name = "AMRemoteImage · Dark",
    group = "AMRemoteImage",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMRemoteImagePreviewDark() {
    AMThemeRoot {
        AMRemoteImagePreviewContent()
    }
}
