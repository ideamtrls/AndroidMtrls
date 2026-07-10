package io.github.jmsdnns.androidmtrls.content

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amSquircle

// ---------------------------------------------------------------------------
// Metrics — glyph size + the two-layer `.text` glyph-shadow recipe
// ---------------------------------------------------------------------------

/** Checkmark glyph diameter — iOS `.font(.body)` at the mark's use size. */
private val GLYPH_SIZE: Dp = 17.dp

/** Far shadow layer blur radius (the `.text` elevation recipe). */
private val FAR_BLUR: Dp = 10.dp

/** Far shadow layer downward offset. */
private val FAR_OFFSET_Y: Dp = 4.dp

/** Near shadow layer blur radius. */
private val NEAR_BLUR: Dp = 3.dp

/** Near shadow layer downward offset. */
private val NEAR_OFFSET_Y: Dp = 1.dp

// ---------------------------------------------------------------------------
// AMSelectionMark
// ---------------------------------------------------------------------------

/**
 * The selection checkmark that sits in the top-trailing corner of a selectable
 * photo or album tile: a white `CheckCircle` glyph with a legibility shadow so
 * it stays readable over any image. Port of iOS `IMSelectionMark`.
 *
 * Most callers should reach for [AMSelectionCheckBox], which positions the mark
 * and shows it only while selected.
 *
 * ### Legibility (the manual two-layer glyph shadow)
 * Legibility over arbitrary imagery is the same problem [AMSectionHeader]
 * solves for text — a light glyph floating over a busy, unpredictable
 * substrate — and the answer is the same `.text` elevation recipe
 * ([AMElevation.Text]): a tight near layer over a softer far layer, both fixed
 * dark, applied to the **glyph alpha mask** rather than a bounding rectangle.
 *
 * The shadow is drawn **manually** here rather than via
 * `Modifier.amElevation(AMElevation.Text)`: that modifier shadows a rounded-rect
 * silhouette, which renders as a grey band behind the transparent parts of the
 * glyph. Instead, the vector is drawn three times in a stack — the two shadow
 * copies are tinted with the fixed-black `.text` shadow tokens
 * ([AMTheme.Shadow.textFar] far = 10dp blur / 4dp y / black α 0.10;
 * [AMTheme.Shadow.textNear] near = 3dp blur / 1dp y / black α 0.12) and blurred
 * with `Modifier.blur`, then the crisp white copy is laid on top. Each shadow
 * copy blurs with [BlurredEdgeTreatment.Unbounded] so the blur bleeds past the
 * 17dp glyph box instead of being clipped to it — `Modifier.blur` clips to the
 * layout bounds by default, which would shear the soft edge off at this size.
 * (RenderEffect blur is available because the library is `minSdk 31`.)
 *
 * Deliberately **no** `Surface.primary` ring (unlike [AMBadge]): that ring
 * exists to separate a colored badge from a colored host, and a white ring adds
 * nothing around a white glyph.
 *
 * @param modifier Positioning/size modifier for the mark. [AMSelectionCheckBox]
 *   passes the corner alignment + inset.
 */
@Composable
fun AMSelectionMark(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Far shadow first (underneath), then the near shadow on top of it —
        // the two-layer `.text` stacking, matching iOS `.imElevation(.text)`.
        SelectionMarkShadow(color = AMTheme.Shadow.textFar, blurRadius = FAR_BLUR, offsetY = FAR_OFFSET_Y)
        SelectionMarkShadow(color = AMTheme.Shadow.textNear, blurRadius = NEAR_BLUR, offsetY = NEAR_OFFSET_Y)
        // Crisp white glyph on top — carries the accessibility label; its opaque
        // fill exactly covers the un-offset centre of the shadow copies.
        Image(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Selected",
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(GLYPH_SIZE),
        )
    }
}

/**
 * One blurred, offset, tinted copy of the checkmark glyph — a single shadow
 * layer of [AMSelectionMark].
 *
 * `ColorFilter.tint` masks the fixed-black shadow [color] to the glyph's alpha
 * coverage (`BlendMode.SrcIn`), so the copy is a soft dark silhouette of the
 * glyph; [BlurredEdgeTreatment.Unbounded] lets the blur bleed past the glyph
 * box. Decorative — `contentDescription = null` keeps it off the a11y tree.
 */
@Composable
private fun SelectionMarkShadow(color: Color, blurRadius: Dp, offsetY: Dp) {
    Image(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = null,
        colorFilter = ColorFilter.tint(color),
        modifier = Modifier
            .size(GLYPH_SIZE)
            .offset(y = offsetY)
            .blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded),
    )
}

// ---------------------------------------------------------------------------
// AMSelectionCheckBox — host-wrapping selection affordance
// ---------------------------------------------------------------------------

/**
 * Wraps a selectable tile and overlays an [AMSelectionMark] at its top-trailing
 * corner while [isSelected] is true — the standard affordance for a selectable
 * photo or album tile in a picker grid. Nothing is drawn when [isSelected] is
 * false, so callers bind live selection state without a conditional at the call
 * site (the zero-state hiding of [AMBadgedBox]). The mark is inset from the
 * corner by `tokens.spacing.xs`, tracking a `LocalAMTokens` spacing override.
 *
 * iOS exposes this as the `View.imSelectionCheck(_:)` modifier. A Compose
 * `Modifier` cannot compose a composable child, so — matching the house
 * precedent set by [AMBadgedBox] (there is no `Modifier.amBadge`) — the
 * positioning is a host-wrapping composable instead. This iOS-modifier →
 * Android-wrapper translation is the established pattern for corner overlays.
 *
 * ```kotlin
 * AMSelectionCheckBox(
 *     isSelected = selected,
 *     modifier = Modifier.aspectRatio(1f).clip(shape).background(brush),
 * ) {
 *     // tile face
 * }
 * ```
 *
 * @param isSelected When true, the mark is overlaid; when false, nothing draws.
 * @param modifier   Modifier for the wrapping box — apply the tile's size, clip,
 *   background, and any `toggleable`/`selectable` here so the mark overlays them.
 * @param content    The tile face to badge with the selection mark.
 */
@Composable
fun AMSelectionCheckBox(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalAMTokens.current
    Box(modifier = modifier) {
        content()
        if (isSelected) {
            AMSelectionMark(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(tokens.spacing.xs),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun SelectionMarkPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val shape = amSquircle(AMTheme.Radius.md.dp)

    // Light, dark, and mid-tone "photo" tiles so the mark's legibility shadow
    // can be read over each. The last tile is unselected (no mark).
    val tiles: List<Pair<Brush, Boolean>> = listOf(
        Brush.linearGradient(listOf(Color(0xFFDDE0E6), Color(0xFFC3C9D2))) to true,   // light
        Brush.linearGradient(listOf(Color(0xFF2C303A), Color(0xFF15171E))) to true,   // dark
        Brush.linearGradient(listOf(Color(0xFF7FB8A6), Color(0xFF4E8FB0))) to false,  // mid-tone
    )

    Row(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
        horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        tiles.forEach { (brush, selected) ->
            AMSelectionCheckBox(
                isSelected = selected,
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(1f)
                    .clip(shape)
                    .background(brush),
            ) {}
        }
    }
}

@Preview(
    name = "AMSelectionMark · Light",
    group = "AMSelectionMark",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7, // AMTheme.Surface.page light
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMSelectionMarkPreviewLight() {
    AMThemeRoot {
        SelectionMarkPreviewContent()
    }
}

@Preview(
    name = "AMSelectionMark · Dark",
    group = "AMSelectionMark",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D, // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMSelectionMarkPreviewDark() {
    AMThemeRoot {
        SelectionMarkPreviewContent()
    }
}
