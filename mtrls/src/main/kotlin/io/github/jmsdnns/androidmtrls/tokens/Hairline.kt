package io.github.jmsdnns.androidmtrls.tokens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.composed
import kotlin.math.max

// ---------------------------------------------------------------------------
// Width helper
// ---------------------------------------------------------------------------

// Formula: max(0.5dp, 1px) where 1px = (1f / density).dp.
// Clamps to at most 1 physical pixel — matches iOS 0.5pt @2x Retina behaviour.
@Composable
internal fun amHairlineWidth(): Dp {
    val density = LocalDensity.current.density
    return max(0.5f, 1f / density).dp
}

// ---------------------------------------------------------------------------
// Modifier.amHairline — unconditional hairline (always drawn)
// ---------------------------------------------------------------------------

/**
 * Draws a hairline stroke **inside** the bounds of [shape], matching the
 * iOS `strokeBorder` behaviour — the stroke never bleeds outside the layout
 * frame.
 *
 * Default [color] is `AMTheme.selectionRing` resolved for the current
 * light/dark theme. Default [width] is [AMTheme.hairlineWidth] (0.5dp)
 * clamped to at most 1 physical pixel via [amHairlineWidth].
 *
 * This modifier uses `Modifier.composed {}` so [color] and [width] can be
 * resolved from CompositionLocals (specifically [LocalAMIsDarkTheme]) without
 * requiring the call site to be in `@Composable` context.
 *
 * **Use this overload for:** sheets, banners, FAB, alerts, toasts, avatars,
 * chip groups — primitives that always show the hairline regardless of
 * light/dark mode.
 *
 * @param shape     Shape whose outline defines where the hairline is drawn.
 * @param color     Stroke color. Defaults to [AMTheme.selectionRing] resolved
 *                  via [LocalAMIsDarkTheme]. Pass [Color.Unspecified] explicitly
 *                  to keep the default.
 * @param width     Stroke width. Defaults to [AMTheme.hairlineWidth] (0.5dp)
 *                  clamped to at most 1 physical pixel.
 */
fun Modifier.amHairline(
    shape: Shape,
    color: Color = Color.Unspecified,
    width: Dp = Dp.Unspecified,
): Modifier = composed {
    val isDark = LocalAMIsDarkTheme.current
    val density = LocalDensity.current.density

    val resolvedColor: Color = if (color == Color.Unspecified) {
        AMTheme.selectionRing.resolve(isDark)
    } else {
        color
    }

    val resolvedWidth: Dp = if (width == Dp.Unspecified) {
        max(0.5f, 1f / density).dp
    } else {
        width
    }

    drawWithContent {
        drawContent()
        val strokeWidthPx = resolvedWidth.toPx()
        // Inset the outline so the stroke is fully inside bounds (mirrors
        // SwiftUI `strokeBorder` / `InsettableShape` behavior).
        val inset = strokeWidthPx / 2f
        val insetSize = Size(
            width  = size.width  - strokeWidthPx,
            height = size.height - strokeWidthPx,
        )

        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                style = PaintingStyle.Stroke
                strokeWidth = strokeWidthPx
                this.color = resolvedColor
                isAntiAlias = true
            }

            val outline = shape.createOutline(insetSize, layoutDirection, this)
            canvas.save()
            canvas.translate(inset, inset)
            when (outline) {
                is Outline.Rectangle -> canvas.drawRect(outline.rect, paint)
                is Outline.Rounded   -> {
                    val rr = outline.roundRect
                    val tl = rr.topLeftCornerRadius
                    val tr = rr.topRightCornerRadius
                    val br = rr.bottomRightCornerRadius
                    val bl = rr.bottomLeftCornerRadius
                    val uniform = tl == tr && tr == br && br == bl
                    if (uniform) {
                        canvas.drawRoundRect(
                            left    = rr.left,
                            top     = rr.top,
                            right   = rr.right,
                            bottom  = rr.bottom,
                            radiusX = tl.x,
                            radiusY = tl.y,
                            paint   = paint,
                        )
                    } else {
                        val path = Path().apply { addRoundRect(rr) }
                        canvas.drawPath(path, paint)
                    }
                }
                is Outline.Generic   -> canvas.drawPath(outline.path, paint)
            }
            canvas.restore()
        }
    }
}

// ---------------------------------------------------------------------------
// Modifier.amCardHairline — dark-mode-only hairline (for cards / filled buttons / toolbar)
// ---------------------------------------------------------------------------

/**
 * Applies [amHairline] only when the UI is in **dark mode**, otherwise returns
 * the receiver unchanged.
 *
 * In light mode, shadows provide enough contrast to define card edges; adding
 * a hairline on top would create unintended visual noise. In dark mode, a
 * pure-black card on the near-black page (`RGB 12,12,13`) needs the hairline
 * to remain readable — shadows alone cannot fully separate the two surfaces
 * at that contrast ratio.
 *
 * This is the exact behaviour of `CardModifier` in iOS iOSMtrls:
 * ```swift
 * if colorScheme == .dark {
 *     RoundedRectangle(…).strokeBorder(IMTheme.selectionRing, lineWidth: IMTheme.hairlineWidth)
 * }
 * ```
 *
 * **Use this overload for:** `AMCard`, `AMFilledButton` body, `AMToolbarButton`.
 *
 * @param shape Shape whose outline defines where the dark-mode hairline is drawn.
 */
fun Modifier.amCardHairline(shape: Shape): Modifier = composed {
    val isDark = LocalAMIsDarkTheme.current
    if (isDark) amHairline(shape) else this
}

// ---------------------------------------------------------------------------
// Previews (light + dark, as required by conventions)
// ---------------------------------------------------------------------------

@Preview(
    name = "Hairline · Light",
    group = "AMHairline",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7  // AMTheme.Surface.page light
)
@Composable
private fun AMHairlinePreviewLight() {
    AMThemeRoot {
        AMHairlinePreviewContent()
    }
}

@Preview(
    name = "Hairline · Dark",
    group = "AMHairline",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,  // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AMHairlinePreviewDark() {
    AMThemeRoot {
        AMHairlinePreviewContent()
    }
}

@Composable
private fun AMHairlinePreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val cardShape = amSquircle(AMTheme.Radius.lg.dp)

    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(AMTheme.Spacing.lg.dp)
    ) {
        // 1. Unconditional hairline — selectionRing default color, rounded rect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AMTheme.rowMinHeight.dp)
                .background(
                    color = AMTheme.Surface.primary.resolve(isDark),
                    shape = cardShape,
                )
                .amHairline(cardShape)
        )

        // 2. Unconditional hairline with explicit separator color
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AMTheme.rowMinHeight.dp)
                .background(
                    color = AMTheme.Surface.primary.resolve(isDark),
                    shape = cardShape,
                )
                .amHairline(cardShape, color = AMTheme.separator.resolve(isDark))
        )

        // 3. Circular unconditional hairline (avatar / FAB use case)
        Box(
            modifier = Modifier
                .height(64.dp)
                .let { mod ->
                    // fixed width circle
                    mod.then(
                        Modifier.padding(horizontal = (AMTheme.Spacing.lg.dp))
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .height(64.dp)
                    .fillMaxWidth()
                    .background(
                        color = AMTheme.Surface.primary.resolve(isDark),
                        shape = CircleShape,
                    )
                    .amHairline(CircleShape)
            )
        }

        // 4. Dark-mode-only card hairline — visible in dark, invisible in light
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AMTheme.rowMinHeight.dp)
                .background(
                    color = AMTheme.Surface.primary.resolve(isDark),
                    shape = cardShape,
                )
                .amCardHairline(cardShape)
        )
    }
}
