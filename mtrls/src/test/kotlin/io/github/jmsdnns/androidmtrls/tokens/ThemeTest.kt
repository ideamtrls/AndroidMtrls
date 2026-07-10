package io.github.jmsdnns.androidmtrls.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [AMAdaptiveColor.resolve].
 *
 * These are pure JVM tests — no Android framework or Compose runtime required.
 * [AMAdaptiveColor] is a plain data class; [resolve(isDark)] is a pure function.
 */
class ThemeTest {

    // -------------------------------------------------------------------------
    // AMAdaptiveColor.resolve(isDark: Boolean) — light/dark resolution semantics
    // -------------------------------------------------------------------------

    @Test
    fun `resolve returns light color when isDark is false`() {
        val light = Color(0xFFFFFFFF)
        val dark  = Color(0xFF000000)
        val adaptive = AMAdaptiveColor(light = light, dark = dark)

        assertEquals(light, adaptive.resolve(isDark = false))
    }

    @Test
    fun `resolve returns dark color when isDark is true`() {
        val light = Color(0xFFFFFFFF)
        val dark  = Color(0xFF000000)
        val adaptive = AMAdaptiveColor(light = light, dark = dark)

        assertEquals(dark, adaptive.resolve(isDark = true))
    }

    @Test
    fun `resolve returns correct color for Surface primary`() {
        // Light mode: white; Dark mode: black — the inverted floating surface recipe
        val token = AMAdaptiveColor(light = Color.White, dark = Color.Black)

        assertEquals(Color.White, token.resolve(isDark = false))
        assertEquals(Color.Black, token.resolve(isDark = true))
    }

    @Test
    fun `resolve returns correct color for Surface page`() {
        val lightPage = Color(0xFFF2F2F7)   // iOS secondarySystemBackground
        val darkPage  = Color(0xFF0C0C0D)   // custom near-black
        val token = AMAdaptiveColor(light = lightPage, dark = darkPage)

        assertEquals(lightPage, token.resolve(isDark = false))
        assertEquals(darkPage,  token.resolve(isDark = true))
    }

    @Test
    fun `resolve returns correct alpha for Shadow near`() {
        // near: light 0.10, dark 0.55
        // Compose Color stores alpha in 8-bit ARGB, so values are quantized to
        // multiples of 1/255. Tolerance is 1/255 ≈ 0.004 to accommodate rounding.
        val token = AMAdaptiveColor(
            light = Color.Black.copy(alpha = 0.10f),
            dark  = Color.Black.copy(alpha = 0.55f)
        )
        val delta = 1f / 255f

        assertEquals(0.10f, token.resolve(isDark = false).alpha, delta)
        assertEquals(0.55f, token.resolve(isDark = true).alpha,  delta)
    }

    @Test
    fun `resolve returns correct alpha for Shadow far`() {
        // far: light 0.08, dark 0.45
        val token = AMAdaptiveColor(
            light = Color.Black.copy(alpha = 0.08f),
            dark  = Color.Black.copy(alpha = 0.45f)
        )
        val delta = 1f / 255f

        assertEquals(0.08f, token.resolve(isDark = false).alpha, delta)
        assertEquals(0.45f, token.resolve(isDark = true).alpha,  delta)
    }

    @Test
    fun `resolve returns correct color for selectionRing`() {
        // light: iOS systemGray3 = RGB 199,199,204; dark: iOS systemGray5 = RGB 44,44,46
        val token = AMAdaptiveColor(
            light = Color(0xFFC7C7CC),
            dark  = Color(0xFF2C2C2E)
        )

        assertEquals(Color(0xFFC7C7CC), token.resolve(isDark = false))
        assertEquals(Color(0xFF2C2C2E), token.resolve(isDark = true))
    }

    @Test
    fun `resolve is deterministic — same input always gives same output`() {
        val token = AMAdaptiveColor(
            light = Color(0xFFF2F2F7),
            dark  = Color(0xFF0C0C0D)
        )

        // Call resolve multiple times; result must be stable
        repeat(10) {
            assertEquals(Color(0xFFF2F2F7), token.resolve(isDark = false))
            assertEquals(Color(0xFF0C0C0D), token.resolve(isDark = true))
        }
    }

    // -------------------------------------------------------------------------
    // AMAdaptiveColor structural integrity
    // -------------------------------------------------------------------------

    @Test
    fun `AMAdaptiveColor is a data class with correct equality`() {
        val a = AMAdaptiveColor(light = Color.White, dark = Color.Black)
        val b = AMAdaptiveColor(light = Color.White, dark = Color.Black)
        val c = AMAdaptiveColor(light = Color.Red,   dark = Color.Black)

        assertEquals(a, b)
        assert(a != c) { "Different light colors must produce unequal instances" }
    }

    @Test
    fun `AMTheme Shadow textNear and textFar are non-adaptive fixed colors`() {
        // These are plain Color constants — not AMAdaptiveColor.
        // 8-bit ARGB quantization applies: tolerance = 1/255 ≈ 0.004.
        val delta = 1f / 255f
        assertEquals(0.12f, AMTheme.Shadow.textNear.alpha, delta)
        assertEquals(0.10f, AMTheme.Shadow.textFar.alpha,  delta)
    }

    @Test
    fun `AMTheme Semantic colors match the iOS system palette`() {
        // Ported from the SwiftUI system colors as adaptive light/dark pairs.
        assertEquals(Color(0xFF007AFF), AMTheme.Semantic.info.resolve(false))        // iOS system blue (light)
        assertEquals(Color(0xFF0A84FF), AMTheme.Semantic.info.resolve(true))         // brighter dark variant
        assertEquals(Color(0xFF34C759), AMTheme.Semantic.success.resolve(false))     // iOS system green (light)
        assertEquals(Color(0xFF30D158), AMTheme.Semantic.success.resolve(true))      // dark variant
        assertEquals(Color(0xFFFFCC00), AMTheme.Semantic.warning.resolve(false))     // iOS system yellow (light)
        assertEquals(Color(0xFFFFD60A), AMTheme.Semantic.warning.resolve(true))      // dark variant
        assertEquals(Color(0xFFFF3B30), AMTheme.Semantic.destructive.resolve(false)) // iOS system red (light)
        assertEquals(Color(0xFFFF453A), AMTheme.Semantic.destructive.resolve(true))  // dark variant
    }

    @Test
    fun `AMTheme flat semantic statics match Semantic group`() {
        assertEquals(AMTheme.Semantic.destructive, AMTheme.destructive)
        assertEquals(AMTheme.Semantic.success,     AMTheme.success)
        assertEquals(AMTheme.Semantic.warning,     AMTheme.warning)
    }

    @Test
    fun `AMTheme accent is the iOS system blue — 007AFF light, 0A84FF dark`() {
        // The accent token is the iOS system blue as an adaptive pair: #007AFF in
        // light mode, the brighter #0A84FF in dark mode. Apps override it via
        // AMTokens for a brand hue (e.g. the demo's Material You accent).
        assertEquals(Color(0xFF007AFF), AMTheme.accent.resolve(isDark = false))
        assertEquals(Color(0xFF0A84FF), AMTheme.accent.resolve(isDark = true))
    }

    @Test
    fun `AMTheme accent is an AMAdaptiveColor pair`() {
        assertEquals(
            AMAdaptiveColor(light = Color(0xFF007AFF), dark = Color(0xFF0A84FF)),
            AMTheme.accent
        )
    }

    // -------------------------------------------------------------------------
    // AMTheme.Text — AMAdaptiveColor resolution (UIColor.label / secondaryLabel)
    // -------------------------------------------------------------------------

    @Test
    fun `AMTheme Text primary resolves to black in light mode`() {
        // UIColor.label light = pure black
        assertEquals(Color.Black, AMTheme.Text.primary.resolve(isDark = false))
    }

    @Test
    fun `AMTheme Text primary resolves to white in dark mode`() {
        // UIColor.label dark = pure white
        assertEquals(Color.White, AMTheme.Text.primary.resolve(isDark = true))
    }

    @Test
    fun `AMTheme Text secondary resolves to RGBA 60-60-67-0_60 in light mode`() {
        // UIColor.secondaryLabel light = RGB(60,60,67) alpha 0.60
        val expected = Color(red = 60 / 255f, green = 60 / 255f, blue = 67 / 255f, alpha = 0.60f)
        val actual   = AMTheme.Text.secondary.resolve(isDark = false)
        val delta    = 1f / 255f
        assertEquals(expected.red,   actual.red,   delta)
        assertEquals(expected.green, actual.green, delta)
        assertEquals(expected.blue,  actual.blue,  delta)
        assertEquals(expected.alpha, actual.alpha, delta)
    }

    @Test
    fun `AMTheme Text secondary resolves to RGBA 235-235-245-0_60 in dark mode`() {
        // UIColor.secondaryLabel dark = RGB(235,235,245) alpha 0.60
        val expected = Color(red = 235 / 255f, green = 235 / 255f, blue = 245 / 255f, alpha = 0.60f)
        val actual   = AMTheme.Text.secondary.resolve(isDark = true)
        val delta    = 1f / 255f
        assertEquals(expected.red,   actual.red,   delta)
        assertEquals(expected.green, actual.green, delta)
        assertEquals(expected.blue,  actual.blue,  delta)
        assertEquals(expected.alpha, actual.alpha, delta)
    }

    // -------------------------------------------------------------------------
    // AMTheme.Icon — AMAdaptiveColor resolution (secondaryLabel / ×0.5 opacity)
    // -------------------------------------------------------------------------

    @Test
    fun `AMTheme Icon muted matches Text secondary in light mode`() {
        // Icon.muted = Color.secondary, same resolved values as Text.secondary
        val delta = 1f / 255f
        val text  = AMTheme.Text.secondary.resolve(isDark = false)
        val icon  = AMTheme.Icon.muted.resolve(isDark = false)
        assertEquals(text.red,   icon.red,   delta)
        assertEquals(text.green, icon.green, delta)
        assertEquals(text.blue,  icon.blue,  delta)
        assertEquals(text.alpha, icon.alpha, delta)
    }

    @Test
    fun `AMTheme Icon muted matches Text secondary in dark mode`() {
        val delta = 1f / 255f
        val text  = AMTheme.Text.secondary.resolve(isDark = true)
        val icon  = AMTheme.Icon.muted.resolve(isDark = true)
        assertEquals(text.red,   icon.red,   delta)
        assertEquals(text.green, icon.green, delta)
        assertEquals(text.blue,  icon.blue,  delta)
        assertEquals(text.alpha, icon.alpha, delta)
    }

    @Test
    fun `AMTheme Icon disabled resolves to RGBA 60-60-67-0_30 in light mode`() {
        // Color.secondary.opacity(0.5) → alpha 0.60 × 0.5 = 0.30
        val expected = Color(red = 60 / 255f, green = 60 / 255f, blue = 67 / 255f, alpha = 0.30f)
        val actual   = AMTheme.Icon.disabled.resolve(isDark = false)
        val delta    = 1f / 255f
        assertEquals(expected.red,   actual.red,   delta)
        assertEquals(expected.green, actual.green, delta)
        assertEquals(expected.blue,  actual.blue,  delta)
        assertEquals(expected.alpha, actual.alpha, delta)
    }

    @Test
    fun `AMTheme Icon disabled resolves to RGBA 235-235-245-0_30 in dark mode`() {
        // Color.secondary.opacity(0.5) → alpha 0.60 × 0.5 = 0.30
        val expected = Color(red = 235 / 255f, green = 235 / 255f, blue = 245 / 255f, alpha = 0.30f)
        val actual   = AMTheme.Icon.disabled.resolve(isDark = true)
        val delta    = 1f / 255f
        assertEquals(expected.red,   actual.red,   delta)
        assertEquals(expected.green, actual.green, delta)
        assertEquals(expected.blue,  actual.blue,  delta)
        assertEquals(expected.alpha, actual.alpha, delta)
    }

    @Test
    fun `AMTheme Overlay constants match spec`() {
        assertEquals(20f,   AMTheme.Overlay.blurRadius,   0.001f)
        assertEquals(0.40f, AMTheme.Overlay.scrimOpacity, 0.001f)
    }

    @Test
    fun `AMTheme Spacing constants match 4pt grid`() {
        assertEquals(4f,  AMTheme.Spacing.xs,  0.001f)
        assertEquals(8f,  AMTheme.Spacing.sm,  0.001f)
        assertEquals(12f, AMTheme.Spacing.md,  0.001f)
        assertEquals(16f, AMTheme.Spacing.lg,  0.001f)
        assertEquals(24f, AMTheme.Spacing.xl,  0.001f)
        assertEquals(32f, AMTheme.Spacing.xxl, 0.001f)
    }

    @Test
    fun `AMTheme Radius constants match spec`() {
        assertEquals(8f,  AMTheme.Radius.sm, 0.001f)
        assertEquals(12f, AMTheme.Radius.md, 0.001f)
        assertEquals(16f, AMTheme.Radius.lg, 0.001f)
    }

    @Test
    fun `AMTheme Input constants match spec`() {
        assertEquals(14f, AMTheme.Input.inset,  0.001f)
        assertEquals(14f, AMTheme.Input.radius, 0.001f)
    }

    @Test
    fun `AMTheme decoration metrics match spec`() {
        assertEquals(0.5f, AMTheme.hairlineWidth, 0.001f)
        assertEquals(64f,  AMTheme.rowMinHeight,  0.001f)
    }

    @Test
    fun `AMTheme selectionRingWidth is hairlineWidth times three`() {
        // iOS: IMTheme.selectionRingWidth = hairlineWidth * 3 = 1.5
        assertEquals(1.5f, AMTheme.selectionRingWidth, 0.001f)
        assertEquals(AMTheme.hairlineWidth * 3, AMTheme.selectionRingWidth, 0.001f)
    }

    // -------------------------------------------------------------------------
    // AMTheme.Type — locked Material-metric type scale
    // -------------------------------------------------------------------------

    @Test
    fun `AMTheme Type title matches spec`() {
        val style = AMTheme.Type.title
        assertEquals(16f, style.fontSize, 0.001f)
        assertEquals(FontWeight.Medium, style.fontWeight)
        assertEquals(0.15f, style.letterSpacing, 0.001f)
    }

    @Test
    fun `AMTheme Type body matches spec`() {
        val style = AMTheme.Type.body
        assertEquals(16f, style.fontSize, 0.001f)
        assertEquals(FontWeight.Normal, style.fontWeight)
        assertEquals(0.5f, style.letterSpacing, 0.001f)
    }

    @Test
    fun `AMTheme Type secondary matches spec`() {
        val style = AMTheme.Type.secondary
        assertEquals(14f, style.fontSize, 0.001f)
        assertEquals(FontWeight.Normal, style.fontWeight)
        assertEquals(0.25f, style.letterSpacing, 0.001f)
    }

    @Test
    fun `AMTheme Type label matches spec`() {
        val style = AMTheme.Type.label
        assertEquals(14f, style.fontSize, 0.001f)
        assertEquals(FontWeight.Medium, style.fontWeight)
        assertEquals(0.1f, style.letterSpacing, 0.001f)
    }

    @Test
    fun `AMTheme Type caption matches spec`() {
        val style = AMTheme.Type.caption
        assertEquals(12f, style.fontSize, 0.001f)
        assertEquals(FontWeight.Normal, style.fontWeight)
        assertEquals(0.4f, style.letterSpacing, 0.001f)
    }

    @Test
    fun `AMTheme Type labelSmall matches spec`() {
        val style = AMTheme.Type.labelSmall
        assertEquals(11f, style.fontSize, 0.001f)
        assertEquals(FontWeight.Medium, style.fontWeight)
        assertEquals(0.5f, style.letterSpacing, 0.001f)
    }

    @Test
    fun `AMTextStyle toTextStyle maps every field`() {
        val color = Color(0xFF123456)
        val text  = AMTheme.Type.title.toTextStyle(color)

        assertEquals(color, text.color)
        assertEquals(16f.sp, text.fontSize)
        assertEquals(FontWeight.Medium, text.fontWeight)
        assertEquals(0.15f.sp, text.letterSpacing)
    }

    @Test
    fun `AMTextStyle is a data class with correct equality`() {
        val a = AMTextStyle(16f, FontWeight.Medium, 0.15f)
        val b = AMTextStyle(16f, FontWeight.Medium, 0.15f)
        val c = AMTextStyle(14f, FontWeight.Medium, 0.15f)

        assertEquals(a, b)
        assert(a != c) { "Different sizes must produce unequal instances" }
    }
}
