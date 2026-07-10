package io.github.jmsdnns.androidmtrls.tokens

import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Adaptive color — the bridge between locked token values and draw-scope lambdas
// ---------------------------------------------------------------------------

/**
 * A light/dark color pair that can be resolved both inside and outside of
 * composable scope.
 *
 * The primary resolution path is [resolve], which reads [isDark] directly —
 * this overload is safe to call from `Modifier.drawBehind {}`, `Canvas` draw
 * lambdas, and any non-composable context where you have captured
 * [LocalAMIsDarkTheme.current] at the composable boundary.
 *
 * There is also a `@Composable` convenience overload [resolve] that reads
 * [LocalAMIsDarkTheme.current] automatically — safe to call from composable scope.
 *
 * @param light Color to return when the UI is in light mode.
 * @param dark  Color to return when the UI is in dark mode.
 */
@Immutable
data class AMAdaptiveColor(val light: Color, val dark: Color) {

    /**
     * Resolve this token using an explicit [isDark] flag.
     *
     * Use this overload inside draw lambdas that are not composable scope,
     * capturing [LocalAMIsDarkTheme.current] once at the composable boundary
     * and passing it here:
     *
     * ```kotlin
     * val isDark = LocalAMIsDarkTheme.current
     * Modifier.drawBehind {
     *     val color = AMTheme.Shadow.near.resolve(isDark)
     *     // …draw with color…
     * }
     * ```
     */
    fun resolve(isDark: Boolean): Color = if (isDark) dark else light

    /**
     * Resolve this token inside composable scope using [LocalAMIsDarkTheme].
     *
     * Reads [LocalAMIsDarkTheme.current] so that any manual light/dark
     * override installed by the demo app (or a test harness) is respected.
     * Convenience overload for callers that are already in `@Composable`
     * context and do not need to pass an explicit flag.
     */
    @Composable
    fun resolve(): Color = resolve(LocalAMIsDarkTheme.current)
}

// ---------------------------------------------------------------------------
// LocalAMIsDarkTheme — bridge CompositionLocal for draw-scope color resolution
// ---------------------------------------------------------------------------

/**
 * `CompositionLocal` bridge that exposes the current dark-theme state to
 * non-composable draw lambdas (`Modifier.drawBehind`, `Canvas`).
 *
 * **Must be provided before any AndroidMtrls component is composed.** Use
 * [AMThemeRoot] at your composition root (Activity or Fragment `setContent {}`)
 * to guarantee this.
 *
 * Default is `false` (light mode) so unhosted previews render a reasonable
 * fallback, but production roots must always install the correct value via
 * [AMThemeRoot] or an explicit [CompositionLocalProvider].
 *
 * Usage inside draw lambdas:
 * ```kotlin
 * val isDark = LocalAMIsDarkTheme.current          // captured in @Composable scope
 * Modifier.drawBehind {
 *     val nearColor = AMTheme.Shadow.near.resolve(isDark)   // used in draw lambda
 *     // …
 * }
 * ```
 */
val LocalAMIsDarkTheme = compositionLocalOf { false }

// ---------------------------------------------------------------------------
// AMThemeRoot — root composable that installs LocalAMIsDarkTheme
// ---------------------------------------------------------------------------

/**
 * Required root composable for every AndroidMtrls composition tree.
 *
 * Installs [LocalAMIsDarkTheme] with the current system dark-theme setting so
 * all draw-scope lambdas (elevation shadows, hairlines) can resolve adaptive
 * colors correctly, and [LocalAMReduceMotion] from the platform reduce-motion
 * signal so animating primitives can collapse their specs.
 *
 * Place this at the root of your `setContent {}` block:
 * ```kotlin
 * setContent {
 *     AMThemeRoot {
 *         // your app content
 *     }
 * }
 * ```
 *
 * **Reduce Motion is a snapshot read.** [LocalAMReduceMotion] is derived once,
 * at first composition, from `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`
 * (the Android-native "remove animations" toggle) via a `remember {}`. There is
 * no live listener: toggling the OS setting while the app is running does not
 * re-derive the flag until the composition is recreated (a process restart, or
 * an Activity/`setContent` recreation). This matches the platform contract — the
 * animator duration scale is a boot/settings-level value — and keeps the read
 * off the recomposition hot path. Tests and demos that need the reduced-motion
 * path without a device setting override [LocalAMReduceMotion] directly in a
 * nested `CompositionLocalProvider`.
 *
 * **Note:** This composable is structured so that `AMTokensProvider`
 * (introduced in `Tokens.kt`) can be layered inside or around it. All three
 * providers are installed inside this composable so they are available before
 * any consumer reads them.
 *
 * @param content The composition subtree to host.
 */
@Composable
fun AMThemeRoot(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    // Snapshot read of the Android-native "remove animations" toggle. A plain
    // remember {} so it is derived once at first composition and never re-run on
    // recomposition — there is no live listener (see KDoc).
    val reduceMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
    CompositionLocalProvider(
        LocalAMIsDarkTheme  provides isDark,
        LocalAMTokens       provides AMTokens.standard,
        LocalAMReduceMotion provides reduceMotion,
        content             = content
    )
}

// ---------------------------------------------------------------------------
// AMTheme — locked signature tokens (never overridable)
// ---------------------------------------------------------------------------

/**
 * Locked signature design tokens for AndroidMtrls.
 *
 * These constants define the **aesthetic identity** of the library: the
 * inverted white/black floating surfaces, two-layer shadow recipe, adaptive
 * color pairs, semantic tints, spacing scale, and motion curves. They are
 * **never overridable** — apps that want to customise style tokens use
 * `AMTokens` via `LocalAMTokens` (see `Tokens.kt`) instead.
 *
 * All values are ported value-for-value from `IMTheme` in iOSMtrls
 * (`Sources/Tokens/Theme.swift`).
 *
 * Dimension tokens (`Spacing.*`, `Radius.*`, `Input.*`, [hairlineWidth],
 * [rowMinHeight]) are `Float` constants representing **dp**; apply `.dp` at
 * call sites (`AMTheme.Spacing.lg.dp`).
 */
object AMTheme {

    // -----------------------------------------------------------------------
    // Surfaces
    // -----------------------------------------------------------------------

    /**
     * Surface color tokens for the two-layer floating-surface recipe.
     *
     * The design deliberately inverts the typical Apple layering: floating
     * surfaces ([primary]) sit **on top of** brighter content ([page]).
     * Shadows still cast onto the brighter substrate so they remain visible.
     */
    object Surface {
        /**
         * Pill / circle background for floating surfaces (URL bar, action
         * grid, panel buttons).
         *
         * Maps to `UIColor.systemBackground`: pure white in light mode, pure
         * black in dark mode. The inverted surface recipe is the aesthetic
         * identity of this library.
         */
        val primary = AMAdaptiveColor(light = Color.White, dark = Color.Black)

        /**
         * Page background behind floating surfaces.
         *
         * - Light: iOS `secondarySystemBackground` = RGB(242,242,247) = `0xFFF2F2F7`.
         * - Dark: custom near-black = RGB(12,12,13) = `0xFF0C0C0D`. Deliberately
         *   darker than Android's typical dark surface (Material dark ≈ RGB(18,18,18))
         *   so the pure-black [primary] cards still register via their drop shadow.
         */
        val page = AMAdaptiveColor(
            light = Color(0xFFF2F2F7),
            dark  = Color(0xFF0C0C0D)
        )
    }

    // -----------------------------------------------------------------------
    // Text
    // -----------------------------------------------------------------------

    /**
     * Text color tokens.
     *
     * [AMAdaptiveColor] pairs resolved from the UIKit dynamic colors that
     * SwiftUI `Color.primary` / `Color.secondary` alias. Resolvable outside
     * of composable scope via [AMAdaptiveColor.resolve(isDark)] — safe to use
     * in `Modifier.drawBehind {}` lambdas after capturing
     * [LocalAMIsDarkTheme.current] at the composable boundary.
     *
     * - [primary]: `UIColor.label` — pure black in light, pure white in dark.
     * - [secondary]: `UIColor.secondaryLabel` — RGBA(60,60,67,0.60) light /
     *   RGBA(235,235,245,0.60) dark.
     */
    object Text {
        /**
         * Primary text color on surfaces.
         *
         * iOS equivalent: `Color.primary` → `UIColor.label`.
         * Resolved values: light = `Color.Black`, dark = `Color.White`.
         */
        val primary = AMAdaptiveColor(
            light = Color.Black,
            dark  = Color.White
        )

        /**
         * Secondary text color for subtitles, captions, and supporting copy.
         *
         * iOS equivalent: `Color.secondary` → `UIColor.secondaryLabel`.
         * Resolved values: light = RGBA(60,60,67,0.60),
         *                  dark  = RGBA(235,235,245,0.60).
         */
        val secondary = AMAdaptiveColor(
            light = Color(red = 60 / 255f, green = 60 / 255f, blue = 67 / 255f,  alpha = 0.60f),
            dark  = Color(red = 235 / 255f, green = 235 / 255f, blue = 245 / 255f, alpha = 0.60f)
        )
    }

    // -----------------------------------------------------------------------
    // Type
    // -----------------------------------------------------------------------

    /**
     * Locked type scale — the Material-metric ramp that carries the library's
     * typographic voice.
     *
     * Signature-tier like [Surface]: typography is part of the aesthetic
     * identity, so these entries live under `AMTheme` and are **never**
     * overridable via `AMTokens`. Each entry is an [AMTextStyle] whose
     * `fontSize`/`letterSpacing` Floats mean **sp**; render one with
     * [AMTextStyle.toTextStyle] to apply a resolved text color.
     */
    object Type {
        /** Row headlines, dialog titles, banner titles. */
        val title = AMTextStyle(16f, FontWeight.Medium, 0.15f)

        /** Input text, body copy, un-emphasized button labels. */
        val body = AMTextStyle(16f, FontWeight.Normal, 0.5f)

        /** Subtitles, picker values, toasts, footers-at-size. */
        val secondary = AMTextStyle(14f, FontWeight.Normal, 0.25f)

        /** Buttons, tabs, chips, segmented labels — the Material label-large role. */
        val label = AMTextStyle(14f, FontWeight.Medium, 0.1f)

        /** Captions, section footers. */
        val caption = AMTextStyle(12f, FontWeight.Normal, 0.4f)

        /** Badges, tile captions, tiny metadata. */
        val labelSmall = AMTextStyle(11f, FontWeight.Medium, 0.5f)
    }

    // -----------------------------------------------------------------------
    // Icon
    // -----------------------------------------------------------------------

    /**
     * Icon tint tokens. iOS equivalent: `IMTheme.Icon.*`.
     *
     * [AMAdaptiveColor] pairs resolved from the same UIKit dynamic colors as
     * [Text]. Resolvable outside composable scope via
     * [AMAdaptiveColor.resolve(isDark)].
     */
    object Icon {
        /**
         * Muted glyph color (e.g., clear-X button in a text field).
         *
         * iOS equivalent: `Color.secondary` → `UIColor.secondaryLabel`.
         * Same resolved values as [Text.secondary].
         */
        val muted = AMAdaptiveColor(
            light = Color(red = 60 / 255f, green = 60 / 255f, blue = 67 / 255f,  alpha = 0.60f),
            dark  = Color(red = 235 / 255f, green = 235 / 255f, blue = 245 / 255f, alpha = 0.60f)
        )

        /**
         * Disabled-state icon color for action grid buttons.
         *
         * iOS equivalent: `Color.secondary.opacity(0.5)` — multiplies the
         * secondaryLabel alpha (0.60) by 0.5, yielding alpha 0.30 in both
         * light and dark modes.
         * Resolved values: light = RGBA(60,60,67,0.30),
         *                  dark  = RGBA(235,235,245,0.30).
         */
        val disabled = AMAdaptiveColor(
            light = Color(red = 60 / 255f, green = 60 / 255f, blue = 67 / 255f,  alpha = 0.30f),
            dark  = Color(red = 235 / 255f, green = 235 / 255f, blue = 245 / 255f, alpha = 0.30f)
        )
    }

    // -----------------------------------------------------------------------
    // Shadow
    // -----------------------------------------------------------------------

    /**
     * Drop shadow color tokens for the two-layer elevation recipe.
     *
     * [near] and [far] are [AMAdaptiveColor] because the dark-mode values
     * (0.55 / 0.45 alpha) must register on the near-black [Surface.page]
     * (RGB 12,12,13). [textNear] and [textFar] are **fixed** (not adaptive)
     * — lightweight shadows for floating labels that are intentionally mild
     * in both modes.
     *
     * Most callers should use `Modifier.amElevation(level, shape)` from
     * `Elevation.kt` rather than reading these directly.
     */
    object Shadow {
        /**
         * Near layer (tight, close to surface).
         *
         * Light: 10% black. Dark: 55% black (must register on RGB 12,12,13).
         */
        val near = AMAdaptiveColor(
            light = Color.Black.copy(alpha = 0.10f),
            dark  = Color.Black.copy(alpha = 0.55f)
        )

        /**
         * Far layer (soft, wider cast).
         *
         * Light: 8% black. Dark: 45% black.
         */
        val far = AMAdaptiveColor(
            light = Color.Black.copy(alpha = 0.08f),
            dark  = Color.Black.copy(alpha = 0.45f)
        )

        /**
         * Text-legibility shadow — near layer. Fixed 12% black (not adaptive).
         *
         * Paired with [textFar] by `amElevation(Text)` for floating labels.
         */
        val textNear: Color = Color.Black.copy(alpha = 0.12f)

        /**
         * Text-legibility shadow — far layer. Fixed 10% black (not adaptive).
         */
        val textFar: Color = Color.Black.copy(alpha = 0.10f)
    }

    // -----------------------------------------------------------------------
    // Decoration — adaptive one-offs
    // -----------------------------------------------------------------------

    /**
     * Selection-ring stroke for selected rows and cards.
     *
     * Asymmetric by design:
     * - Light: iOS `systemGray3` ≈ RGB(199,199,204) = `0xFFC7C7CC`.
     * - Dark:  iOS `systemGray5` ≈ RGB(44,44,46)   = `0xFF2C2C2E`.
     */
    val selectionRing = AMAdaptiveColor(
        light = Color(0xFFC7C7CC),
        dark  = Color(0xFF2C2C2E)
    )

    /**
     * Hairline divider matching iOS `UIColor.separator`.
     *
     * - Light: RGBA(60,60,67, 0.29).
     * - Dark:  RGBA(84,84,88, 0.65).
     */
    val separator = AMAdaptiveColor(
        light = Color(red = 60 / 255f, green = 60 / 255f, blue = 67 / 255f, alpha = 0.29f),
        dark  = Color(red = 84 / 255f, green = 84 / 255f, blue = 88 / 255f, alpha = 0.65f)
    )

    // -----------------------------------------------------------------------
    // Semantic colors
    // -----------------------------------------------------------------------

    /**
     * Tint for destructive actions. iOS system red — `#FF3B30` light,
     * `#FF453A` dark.
     *
     * Ported from the SwiftUI system palette (`IMTheme.destructive = .red`). An
     * [AMAdaptiveColor] pair so the charged tint stays legible on both the white
     * and near-black inverted surfaces; resolve with `.resolve(isDark)` at the
     * call site (draw-scope safe), exactly as [accent] is consumed.
     */
    val destructive = AMAdaptiveColor(light = Color(0xFFFF3B30), dark = Color(0xFFFF453A))

    /**
     * Tint for success states. iOS system green — `#34C759` light,
     * `#30D158` dark.
     *
     * Ported from the SwiftUI system palette (`IMTheme.success = .green`). An
     * [AMAdaptiveColor] pair; resolve with `.resolve(isDark)` — see [destructive].
     */
    val success = AMAdaptiveColor(light = Color(0xFF34C759), dark = Color(0xFF30D158))

    /**
     * Tint for warning states. iOS system yellow — `#FFCC00` light,
     * `#FFD60A` dark.
     *
     * Ported from the SwiftUI system palette (`IMTheme.warning = .yellow`). An
     * [AMAdaptiveColor] pair; resolve with `.resolve(isDark)` — see [destructive].
     */
    val warning = AMAdaptiveColor(light = Color(0xFFFFCC00), dark = Color(0xFFFFD60A))

    /**
     * Semantic tint group — useful when a component takes a semantic axis as
     * a parameter. Members mirror the top-level [AMAdaptiveColor] statics above.
     */
    object Semantic {
        /**
         * Informational tint. iOS system blue — `#007AFF` light, `#0A84FF` dark.
         *
         * Ported from the SwiftUI system palette (`IMTheme.Semantic.info =
         * .blue`). An [AMAdaptiveColor] pair; resolve with `.resolve(isDark)` —
         * see [AMTheme.destructive].
         */
        val info = AMAdaptiveColor(light = Color(0xFF007AFF), dark = Color(0xFF0A84FF))

        /** Success tint. Delegates to [AMTheme.success]. */
        val success: AMAdaptiveColor = AMTheme.success

        /** Warning tint. Delegates to [AMTheme.warning]. */
        val warning: AMAdaptiveColor = AMTheme.warning

        /** Destructive tint. Delegates to [AMTheme.destructive]. */
        val destructive: AMAdaptiveColor = AMTheme.destructive
    }

    /**
     * Accent tint for primary affordances — the progress-bar fill, the action
     * grid's active tile, the calendar's "today" marker, and the demo app's
     * dynamic-accent toggle (which overrides it from the Material You wallpaper
     * palette).
     *
     * The iOS system blue as an adaptive pair: `#007AFF` in light mode,
     * `#0A84FF` (the brighter dark-mode variant) in dark mode, so it stays
     * legible on both the white and near-black inverted surfaces. Apps that
     * want a different accent override `AMTokensSemantics.accent` via
     * `AMTokensProvider` — the demo's Material You transform still works
     * unchanged.
     *
     * An [AMAdaptiveColor] pair so it can resolve inside draw-scope lambdas via
     * [AMAdaptiveColor.resolve].
     */
    val accent = AMAdaptiveColor(light = Color(0xFF007AFF), dark = Color(0xFF0A84FF))

    // -----------------------------------------------------------------------
    // Press feedback
    // -----------------------------------------------------------------------

    /**
     * Hit-state overlay color for pressable surfaces while a press is active.
     *
     * iOS: `Color.primary.opacity(0.08)`. `Color.primary` resolves to
     * `UIColor.label`: pure black on light surfaces, pure white on dark
     * surfaces — so the 0.08 fill adapts naturally to AM's inverted
     * white/black floating surfaces. Reads [LocalAMIsDarkTheme.current] so
     * the demo app's manual light/dark toggle is respected.
     *
     * Resolved: light = `Color.Black × 0.08`, dark = `Color.White × 0.08`.
     */
    @Composable
    fun press(): Color = if (LocalAMIsDarkTheme.current) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    /**
     * Resting neutral fill for unselected affordances (chips, tags).
     *
     * Same recipe as [press] but a distinct semantic role: [press] is a
     * transient hit-state overlay; this is a resting fill. Reads
     * [LocalAMIsDarkTheme.current] so the demo app's manual light/dark
     * toggle is respected.
     *
     * iOS: `Color.primary.opacity(0.08)`.
     */
    @Composable
    fun neutralFill(): Color = press()

    // -----------------------------------------------------------------------
    // Overlay treatment
    // -----------------------------------------------------------------------

    /**
     * Shared recipe for full-screen modal overlays (ActionsOverlay,
     * LoadingOverlay, Alert). iOS: `IMTheme.Overlay`.
     */
    object Overlay {
        /** Default backdrop blur radius in dp. iOS equivalent: 20. */
        const val blurRadius: Float = 20f

        /** Dark-mode scrim opacity over the blur. iOS equivalent: 0.4. */
        const val scrimOpacity: Float = 0.40f
    }

    // -----------------------------------------------------------------------
    // Spacing scale (4pt grid)
    // -----------------------------------------------------------------------

    /**
     * Spatial scale for all primitives (4pt grid). Values are **dp**.
     *
     * Apply `.dp` at call sites: `AMTheme.Spacing.lg.dp`.
     * iOS equivalent: `IMTheme.Spacing`.
     */
    object Spacing {
        /**  4dp — tight gaps between related glyphs or inline chips. */
        const val xs: Float = 4f

        /**  8dp — compact inter-element spacing within a row. */
        const val sm: Float = 8f

        /** 12dp — standard intra-card vertical spacing. */
        const val md: Float = 12f

        /** 16dp — default card interior padding and inter-section spacing. */
        const val lg: Float = 16f

        /** 24dp — generous breathing room between distinct sections. */
        const val xl: Float = 24f

        /** 32dp — page-level vertical margins and large compositional gaps. */
        const val xxl: Float = 32f
    }

    // -----------------------------------------------------------------------
    // Corner radii
    // -----------------------------------------------------------------------

    /**
     * Corner-radius scale in dp.
     *
     * - Small pills / chips → [sm]
     * - Rows and inputs → [md]
     * - Cards and panels → [lg]
     *
     * iOS equivalent: `IMTheme.Radius`.
     */
    object Radius {
        /**  8dp — small affordances (chips, badges, compact buttons). */
        const val sm: Float = 8f

        /** 12dp — medium affordances (inputs, toggles, segmented controls). */
        const val md: Float = 12f

        /** 16dp — large floating surfaces (cards, panels, sheets). */
        const val lg: Float = 16f
    }

    // -----------------------------------------------------------------------
    // Input metrics
    // -----------------------------------------------------------------------

    /**
     * Metrics for text-input primitives (TextField, SecureField,
     * SearchField, TextEditor). iOS equivalent: `IMTheme.Input`.
     */
    object Input {
        /**
         * Leading inset and trailing breathing room for input primitives. 14dp.
         */
        const val inset: Float = 14f

        /**
         * Corner radius for input primitives. Matches [inset] for
         * consistent pill silhouette. 14dp.
         */
        const val radius: Float = 14f
    }

    // -----------------------------------------------------------------------
    // Decoration metrics
    // -----------------------------------------------------------------------

    /**
     * Hairline stroke width in dp (0.5). Used by `Modifier.amHairline` and
     * every primitive that draws a one-pixel outline.
     *
     * At xhdpi (2×) this produces exactly 1 physical pixel.
     * iOS equivalent: `IMTheme.hairlineWidth = 0.5`.
     */
    const val hairlineWidth: Float = 0.5f

    /**
     * Selection-ring stroke width in dp (1.5), defined as [hairlineWidth] × 3.
     *
     * The stroke width of the active-state ring drawn around a selected tile.
     * Consumer: the ActionGridButton active ring.
     * iOS equivalent: `IMTheme.selectionRingWidth = hairlineWidth * 3 = 1.5`.
     */
    const val selectionRingWidth: Float = hairlineWidth * 3

    /**
     * Minimum height for single-row card widgets in dp (64).
     *
     * Pins row widgets to approximately the height of a list row at default
     * Dynamic Type, so stacked row widgets share one visual rhythm.
     * iOS equivalent: `IMTheme.rowMinHeight = 64`.
     */
    const val rowMinHeight: Float = 64f

    // -----------------------------------------------------------------------
    // Motion
    // -----------------------------------------------------------------------

    /**
     * Animation curves and gesture thresholds. iOS equivalent: `IMTheme.Motion`.
     *
     * Springs are converted from iOS `spring(response:dampingFraction:)` via
     * `stiffness = (2π / response)²`. Values are approximations; empirical
     * tuning is expected at Phase 12.
     *
     * For `Dp`, `IntOffset`, or other typed animations, reconstruct a
     * matching spec at the call site:
     * ```kotlin
     * spring<Dp>(dampingRatio = 0.86f, stiffness = 384f)
     * ```
     */
    object Motion {
        /**
         * "Snap to rest" — settled and responsive.
         * response=0.32s → stiffness≈384; dampingFraction=0.86.
         */
        val snapSpring = spring<Float>(dampingRatio = 0.86f, stiffness = 384f)

        /**
         * "Present" — softer so a transient surface breathes.
         * response=0.35s → stiffness≈321; dampingFraction=0.85.
         */
        val presentSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 321f)

        /**
         * "Modal" — longer response for large-distance travel.
         * response=0.42s → stiffness≈224; dampingFraction=0.86.
         */
        val modalSpring = spring<Float>(dampingRatio = 0.86f, stiffness = 224f)

        /**
         * Overlay fade. iOS: `.easeInOut(duration: 0.18)`.
         *
         * Uses [EaseInOut] — the symmetric CSS ease-in-out cubic that maps
         * SwiftUI's `.easeInOut` exactly (project RULING 2). The reduce-motion
         * fallbacks in `Motion.kt` mirror this spec and must be retuned in lockstep.
         */
        val overlayFade = tween<Float>(durationMillis = 180, easing = EaseInOut)

        /**
         * Press-feedback fade for glyph buttons. iOS: `.easeOut(duration: 0.12)`.
         *
         * Keeps [LinearOutSlowInEasing] — the Material-native deceleration curve
         * that the house rules map `.easeOut` onto (see conventions.md).
         */
        val pressFade = tween<Float>(durationMillis = 120, easing = LinearOutSlowInEasing)

        /**
         * Selection-commit fade for toggleable pills. iOS: `.easeInOut(duration: 0.15)`.
         *
         * Uses [EaseInOut] to match SwiftUI's `.easeInOut` (project RULING 2).
         */
        val selectionFade = tween<Float>(durationMillis = 150, easing = EaseInOut)

        /**
         * Drag distance past which a dismiss commits. 60dp.
         * iOS equivalent: `IMTheme.Motion.dismissThreshold = 60`.
         */
        const val dismissThreshold: Float = 60f

        /**
         * Compact dismiss threshold for edge-anchored surfaces (banner, toast). 30dp.
         * iOS equivalent: `IMTheme.Motion.dismissThresholdCompact = 30`.
         */
        const val dismissThresholdCompact: Float = 30f
    }
}

// ---------------------------------------------------------------------------
// Previews (light + dark, as required by conventions)
// ---------------------------------------------------------------------------

@Preview(
    name = "AMTheme · Light",
    group = "AMTheme",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7  // Surface.page light
)
@Composable
private fun AMThemePreviewLight() {
    AMThemeRoot {
        AMThemePreviewContent()
    }
}

@Preview(
    name = "AMTheme · Dark",
    group = "AMTheme",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,  // Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AMThemePreviewDark() {
    AMThemeRoot {
        AMThemePreviewContent()
    }
}

@Composable
private fun AMThemePreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier.padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp)
    ) {
        // Surface.primary card swatch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AMTheme.rowMinHeight.dp)
                .background(
                    color = AMTheme.Surface.primary.resolve(isDark),
                    shape = amSquircle(AMTheme.Radius.lg.dp)
                )
        )

        // Shadow colors as opaque swatches
        Row(horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = AMTheme.Shadow.near.resolve(isDark),
                        shape = amSquircle(AMTheme.Radius.sm.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = AMTheme.Shadow.far.resolve(isDark),
                        shape = amSquircle(AMTheme.Radius.sm.dp)
                    )
            )
        }

        // Semantic color row
        Row(horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
            listOf(
                AMTheme.Semantic.info,
                AMTheme.Semantic.success,
                AMTheme.Semantic.warning,
                AMTheme.Semantic.destructive
            ).forEach { adaptive ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = adaptive.resolve(isDark),
                            shape = amSquircle(AMTheme.Radius.sm.dp)
                        )
                )
            }
        }

        // selectionRing + separator swatches
        Row(horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = AMTheme.selectionRing.resolve(isDark),
                        shape = amSquircle(AMTheme.Radius.sm.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = AMTheme.separator.resolve(isDark),
                        shape = amSquircle(AMTheme.Radius.sm.dp)
                    )
            )
        }
    }
}
