package io.github.jmsdnns.androidmtrls.content

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amSquircle

// ---------------------------------------------------------------------------
// Shimmer constants — hardcoded loading-affordance identity, NOT tokens.
//
// The shimmer's rhythm and highlight are a fixed part of the loading
// affordance; they must NOT drift when an app re-tunes its motion or color
// tokens. Deliberately not derived from LocalAMTokens.
// ---------------------------------------------------------------------------

/** Shimmer sweep cycle duration in milliseconds. */
private const val SHIMMER_DURATION_MS = 1200

/**
 * Peak highlight opacity at the band centre.
 *
 * The band is composited additively ([BlendMode.Plus]) over the content and
 * masked to the content silhouette (see [amShimmer]), reproducing iOS's
 * `.plusLighter` gleam. The white highlight *adds* to the drawn pixels by up to
 * this fraction at the band centre. On the near-black dark `Surface.page` the
 * additive lift punches clearly — see [amShimmer] for the dark-mode note.
 */
private const val SHIMMER_PEAK_ALPHA = 0.55f

/**
 * Highlight band width as a fraction of the container width.
 * Clamped to a minimum of [SHIMMER_BAND_MIN_DP].
 */
private const val SHIMMER_BAND_FRACTION = 0.40f

/** Minimum highlight band width in dp. */
private val SHIMMER_BAND_MIN_DP = 60.dp

// ---------------------------------------------------------------------------
// Modifier.amShimmer() — the one public shimmer
// ---------------------------------------------------------------------------

/**
 * Sweep a shimmer highlight across this content — a moving band masked to
 * whatever the content draws, so it brightens the drawn pixels and leaves the
 * gaps alone. The single shimmer implementation used across every AndroidMtrls
 * loading state (`AMSkeleton` is built on it).
 *
 * ### Preferred pattern: shimmer a real placeholder tree
 * SwiftUI's `.redacted(reason: .placeholder)` has no Compose equivalent, so
 * rather than redacting the finished view you build the *shape* you'll show
 * once data arrives — real composables filled with placeholder washes — and put
 * one `amShimmer()` over the whole tree. A single sweep then travels across the
 * entire silhouette (avatar + text bars together), and the gaps between the
 * shapes stay untouched:
 *
 * ```kotlin
 * Row(
 *     modifier = Modifier.amShimmer(),
 *     horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
 *     verticalAlignment = Alignment.CenterVertically,
 * ) {
 *     Box(Modifier.size(40.dp).background(AMTheme.separator.resolve(isDark), CircleShape))
 *     Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
 *         Box(Modifier.fillMaxWidth(0.7f).height(14.dp).background(base, RoundedCornerShape(50)))
 *         Box(Modifier.fillMaxWidth(0.4f).height(10.dp).background(base, RoundedCornerShape(50)))
 *     }
 * }
 * ```
 *
 * When there's no bespoke tree to build, reach for the [AMSkeleton]
 * primitives — they wrap this modifier around a `separator`-filled shape.
 *
 * ### How the additive masking works
 * The gleam is a two-pass draw that reproduces iOS's `.plusLighter` band clipped
 * with `.mask(content)`:
 * 1. `drawContent()` paints the content as the base.
 * 2. A transparency layer is opened with a [BlendMode.Plus] paint so its result
 *    composites *additively* onto the base. Inside it the content is drawn again
 *    (supplying the destination alpha), then the white band is drawn with
 *    [BlendMode.SrcIn] — clipping the gleam to the content's own alpha so it
 *    never bleeds into the transparent gaps. On layer restore, the masked gleam
 *    is Plus-added onto the base, brightening the drawn pixels only.
 *
 * The second `drawContent()` captures the content's silhouette without the
 * modifier having to know the placeholder shape, so the additive-and-masked
 * gleam works over an arbitrary placeholder tree.
 *
 * **Dark mode:** `Plus` *adds* the band toward white by up to
 * [SHIMMER_PEAK_ALPHA]. On the near-black dark `Surface.page`, a `separator`
 * base (translucent grey) lifts to a clearly lighter grey at the band centre —
 * the sweep stays legible in both schemes without washing the surface out.
 *
 * ### Reduce Motion
 * Under [LocalAMReduceMotion] the content renders **static**: the modifier
 * returns before any infinite transition is created, so no animation object is
 * allocated at all (the Compose analog of iOS skipping its `TimelineView`
 * timer). The placeholder shapes still show — only the sweep is gone.
 *
 * The sweep is composition-scoped (driven by [rememberInfiniteTransition]), so
 * it stops the instant the content leaves the tree.
 *
 * iOS equivalent: `View.imShimmer()`.
 */
@Composable
fun Modifier.amShimmer(): Modifier {
    // Reduce Motion → render content static. Return BEFORE creating any infinite
    // transition so no animation object is allocated at all — mirrors iOS not
    // starting a TimelineView timer under Reduce Motion.
    if (LocalAMReduceMotion.current) return this

    val transition = rememberInfiniteTransition(label = "amShimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "amShimmerPhase",
    )

    // Phase-independent gleam objects, allocated once and reused every ~60fps
    // draw so the per-frame work is limited to the phase-dependent band geometry:
    // - the Plus-blend layer paint that composites the gleam additively, and
    // - the white gradient color stops (peak alpha at the band centre).
    val gleamPaint = remember { Paint().apply { blendMode = BlendMode.Plus } }
    val gleamStops = remember {
        arrayOf(
            0f to Color.White.copy(alpha = 0f),
            0.5f to Color.White.copy(alpha = SHIMMER_PEAK_ALPHA),
            1f to Color.White.copy(alpha = 0f),
        )
    }

    return this.drawWithContent {
        // Base: the content itself.
        drawContent()
        // Additive white gleam, masked to the content silhouette — iOS's
        // `.plusLighter` band clipped with `.mask(content)`.
        drawShimmerGleam(phase = phase, layerPaint = gleamPaint, colorStops = gleamStops)
    }
}

/**
 * Draws the moving highlight gleam over already-drawn content, additively and
 * masked to the content's own silhouette (the iOS `.plusLighter` + `.mask`
 * recipe).
 *
 * A transparency layer is opened with a [BlendMode.Plus] paint so it composites
 * additively onto the base content on restore. Inside the layer the content is
 * drawn again to supply the destination alpha, then the white band is drawn with
 * [BlendMode.SrcIn] so the gleam is clipped to the content's alpha and never
 * bleeds into the transparent gaps.
 *
 * @param phase      Normalized phase in `0f..1f` — 0 = band fully off-screen left,
 *                   1 = band fully off-screen right.
 * @param layerPaint The [BlendMode.Plus] transparency-layer paint (hoisted and
 *                   reused across frames — phase-independent).
 * @param colorStops The white gradient stops for the band (hoisted and reused
 *                   across frames — only the band's `startX`/`endX` shift per phase).
 */
private fun ContentDrawScope.drawShimmerGleam(
    phase: Float,
    layerPaint: Paint,
    colorStops: Array<Pair<Float, Color>>,
) {
    val bandWidth = maxOf(size.width * SHIMMER_BAND_FRACTION, SHIMMER_BAND_MIN_DP.toPx())
    // startX: band starts fully off-screen left (-bandWidth) and moves to fully
    // off-screen right (size.width). Phase 0 = left edge, phase 1 = right edge.
    val startX = -bandWidth + (size.width + bandWidth) * phase

    val canvas = drawContext.canvas
    canvas.saveLayer(Rect(Offset.Zero, size), layerPaint)
    // Content copy → supplies the destination alpha that clips the band below.
    drawContent()
    // White band with SrcIn → kept only where the content is opaque, so the
    // additive gleam tracks the content silhouette and never fills the gaps.
    // Only startX/endX shift per frame; the color stops are reused.
    drawRect(
        brush = Brush.horizontalGradient(
            colorStops = colorStops,
            startX = startX,
            endX = startX + bandWidth,
        ),
        blendMode = BlendMode.SrcIn,
    )
    canvas.restore()
}

// ---------------------------------------------------------------------------
// AMSkeletonShape — private separator-filled shape, shimmered via amShimmer
// ---------------------------------------------------------------------------

/**
 * A [AMTheme.separator]-filled [shape] with the shared [amShimmer] sweep.
 *
 * The opaque separator fill is the shimmer's masking target: since the fill
 * covers the whole shape, the sweep covers the whole shape (there are no gaps
 * to mask out within a single primitive). All three [AMSkeleton] factories
 * render through this one composable so there is exactly one shimmer path.
 *
 * @param shape    Clip and draw shape for this skeleton slot.
 * @param modifier Size and positioning modifier applied to the [Box].
 */
@Composable
private fun AMSkeletonShape(
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalAMIsDarkTheme.current
    val baseColor = AMTheme.separator.resolve(isDark)

    Box(
        modifier = modifier
            // amShimmer is OUTER of the fill so its drawContent() captures the
            // separator fill and masks the sweep to it.
            .amShimmer()
            .background(color = baseColor, shape = shape),
    )
}

// ---------------------------------------------------------------------------
// AMSkeleton — public object with composable factory functions
// ---------------------------------------------------------------------------

/**
 * Skeleton placeholder components for loading states.
 *
 * Provides three factory functions ([rect], [circle], [text]) that render
 * animated shimmer placeholders matching the corresponding content shape. Each
 * variant is a [AMTheme.separator]-filled shape carrying the shared
 * [Modifier.amShimmer] sweep.
 *
 * These primitives are the fallback for a *bespoke* loading arrangement. When
 * you can build the shape of the finished layout instead, prefer wrapping one
 * [Modifier.amShimmer] around a real placeholder tree so a single sweep tracks
 * the whole silhouette — see [amShimmer].
 *
 * **Dark mode:** the shimmer *adds* the band toward white via [BlendMode.Plus]
 * (masked to the content silhouette); on the near-black dark
 * [AMTheme.Surface.page] the `separator` base lifts to a clearly lighter grey
 * under the band, staying legible without washing the surface out (see
 * [amShimmer]).
 *
 * iOS equivalent: `IMSkeleton` (enum with static builders).
 *
 * ### Shimmer constants (hardcoded — not overridable via [LocalAMTokens])
 * | Parameter | Value |
 * |-----------|-------|
 * | Cycle duration | 1200ms |
 * | Peak highlight alpha | 0.55 |
 * | Blend mode | `BlendMode.Plus` (additive, masked to content) |
 * | Band width | max(containerWidth × 40%, 60dp) |
 */
object AMSkeleton {

    /**
     * Rectangular skeleton placeholder.
     *
     * Defaults to full-width fill when [width] is null. Corner radius defaults
     * to [LocalAMTokens]`.radius.sm` (8dp) when [cornerRadius] is null.
     *
     * iOS equivalent: `IMSkeleton.rect(width:height:cornerRadius:)`.
     *
     * @param width        Explicit width in dp. Pass `null` to fill the parent width.
     * @param height       Height in dp (default 14dp).
     * @param cornerRadius Corner radius in dp. Pass `null` to use `tokens.radius.sm`.
     */
    @Composable
    fun rect(
        width: Dp? = null,
        height: Dp = 14.dp,
        cornerRadius: Dp? = null,
    ) {
        val tokens = LocalAMTokens.current
        val radius = cornerRadius ?: tokens.radius.sm
        val shape = amSquircle(radius)

        val modifier = if (width != null) {
            Modifier.width(width).height(height)
        } else {
            Modifier.fillMaxWidth().height(height)
        }

        AMSkeletonShape(shape = shape, modifier = modifier)
    }

    /**
     * Circular skeleton placeholder.
     *
     * iOS equivalent: `IMSkeleton.circle(diameter:)`.
     *
     * @param diameter Circle diameter in dp (default 40dp).
     */
    @Composable
    fun circle(diameter: Dp = 40.dp) {
        AMSkeletonShape(
            shape = CircleShape,
            modifier = Modifier.size(diameter),
        )
    }

    /**
     * Multi-line text block skeleton placeholder.
     *
     * Renders [lines] capsule-shaped skeleton rows. The last row shortens to
     * 60% of the full width when `lines > 1` — matching the iOS behaviour
     * where the final line mimics a natural paragraph end.
     *
     * iOS equivalent: `IMSkeleton.text(lines:lineHeight:spacing:)`.
     *
     * @param lines      Number of text lines to render (default 3).
     * @param lineHeight Height of each line in dp (default 12dp).
     * @param spacing    Vertical gap between lines in dp. Pass `null` to use
     *                   `tokens.spacing.sm` (8dp).
     */
    @Composable
    fun text(
        lines: Int = 3,
        lineHeight: Dp = 12.dp,
        spacing: Dp? = null,
    ) {
        val tokens = LocalAMTokens.current
        val lineSpacing = spacing ?: tokens.spacing.sm

        // BoxWithConstraints gives us the container width so we can derive the
        // 60%-width last line without a nested measurement pass.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(lineSpacing),
            ) {
                repeat(lines) { index ->
                    val isLast = index == lines - 1
                    val widthFraction = if (isLast && lines > 1) 0.6f else 1f
                    AMSkeletonShape(
                        shape = RoundedCornerShape(50),  // capsule — 50% radius
                        modifier = Modifier
                            .fillMaxWidth(widthFraction)
                            .height(lineHeight),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun SkeletonPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current
    val base = AMTheme.separator.resolve(isDark)

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(tokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.lg),
    ) {
        // rect — full-width
        AMSkeleton.rect()

        // rect — explicit width + height
        AMSkeleton.rect(width = 120.dp, height = 20.dp)

        // circle — default diameter
        AMSkeleton.circle()

        // circle — custom diameter
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            AMSkeleton.circle(diameter = 32.dp)
            AMSkeleton.circle(diameter = 44.dp)
            AMSkeleton.circle(diameter = 64.dp)
        }

        // text — 3 lines (default)
        AMSkeleton.text()

        // Preferred pattern: a real placeholder tree under ONE amShimmer sweep.
        // One sweep tracks the whole silhouette (avatar + text bars together);
        // the gaps between the shapes stay untouched.
        Row(
            modifier = Modifier.fillMaxWidth().amShimmer(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(44.dp).background(base, CircleShape))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            ) {
                Box(Modifier.fillMaxWidth(0.7f).height(14.dp).background(base, RoundedCornerShape(50)))
                Box(Modifier.fillMaxWidth(0.4f).height(10.dp).background(base, RoundedCornerShape(50)))
            }
        }

        // Spacer so the bottom doesn't feel clipped
        Spacer(modifier = Modifier.height(tokens.spacing.lg))
    }
}

@Preview(
    name = "AMSkeleton · Light",
    group = "AMSkeleton",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,  // AMTheme.Surface.page light
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMSkeletonPreviewLight() {
    AMThemeRoot {
        SkeletonPreviewContent()
    }
}

@Preview(
    name = "AMSkeleton · Dark",
    group = "AMSkeleton",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,  // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMSkeletonPreviewDark() {
    AMThemeRoot {
        SkeletonPreviewContent()
    }
}
