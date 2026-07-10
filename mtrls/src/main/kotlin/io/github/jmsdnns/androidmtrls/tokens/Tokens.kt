package io.github.jmsdnns.androidmtrls.tokens

import android.content.res.Configuration
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// AMTokensSemantics — overridable semantic color tokens
// ---------------------------------------------------------------------------

/**
 * Overridable semantic color tokens.
 *
 * Every field defaults to the matching [AMTheme.Semantic] / [AMTheme.accent]
 * constant, so [AMTokens.standard] renders stock AndroidMtrls. Override via
 * [AMTokensProvider] to substitute brand colors per-app or per-subtree.
 *
 * All five fields are [AMAdaptiveColor] light/dark pairs — the iOS system
 * palette, brighter in dark mode so the charged tints stay legible on both the
 * white and near-black inverted surfaces. Resolve each with `.resolve(isDark)`
 * at the call site (draw-scope safe). [accent] is the primary-affordance tint
 * (progress fills, primary-button backgrounds); the other four are the status
 * roles read through the [AMSemantic] axis.
 *
 * @param info        Informational tint pair. Default: [AMTheme.Semantic.info].
 * @param success     Success/confirmation tint pair. Default: [AMTheme.Semantic.success].
 * @param warning     Warning tint pair. Default: [AMTheme.Semantic.warning].
 * @param destructive Destructive-action tint pair. Default: [AMTheme.Semantic.destructive].
 * @param accent      Primary-affordance tint pair. Default: [AMTheme.accent].
 */
@Immutable
data class AMTokensSemantics(
    val info:        AMAdaptiveColor = AMTheme.Semantic.info,
    val success:     AMAdaptiveColor = AMTheme.Semantic.success,
    val warning:     AMAdaptiveColor = AMTheme.Semantic.warning,
    val destructive: AMAdaptiveColor = AMTheme.Semantic.destructive,
    val accent:      AMAdaptiveColor = AMTheme.accent
)

// ---------------------------------------------------------------------------
// AMTokensSpacing — overridable spacing scale tokens
// ---------------------------------------------------------------------------

/**
 * Overridable spacing-scale tokens (4pt grid).
 *
 * Fields mirror [AMTheme.Spacing] converted to [Dp]. Override selected
 * steps per-subtree via [AMTokensProvider] to accommodate information-dense
 * or compact-layout contexts.
 *
 * @param xs  Extra-small gap (default 4dp).
 * @param sm  Small gap (default 8dp).
 * @param md  Medium gap — intra-card vertical (default 12dp).
 * @param lg  Large gap — default card interior padding (default 16dp).
 * @param xl  Extra-large gap — section breathing room (default 24dp).
 * @param xxl Extra-extra-large gap — page-level margin (default 32dp).
 */
@Immutable
data class AMTokensSpacing(
    val xs:  Dp = AMTheme.Spacing.xs.dp,
    val sm:  Dp = AMTheme.Spacing.sm.dp,
    val md:  Dp = AMTheme.Spacing.md.dp,
    val lg:  Dp = AMTheme.Spacing.lg.dp,
    val xl:  Dp = AMTheme.Spacing.xl.dp,
    val xxl: Dp = AMTheme.Spacing.xxl.dp
)

// ---------------------------------------------------------------------------
// AMTokensRadius — overridable corner-radius tokens
// ---------------------------------------------------------------------------

/**
 * Overridable corner-radius tokens.
 *
 * Fields mirror [AMTheme.Radius] converted to [Dp]. Override to unify shape
 * language for brand-specific layouts.
 *
 * @param sm Small radius — chips, badges (default 8dp).
 * @param md Medium radius — inputs, toggles (default 12dp).
 * @param lg Large radius — cards, panels (default 16dp).
 */
@Immutable
data class AMTokensRadius(
    val sm: Dp = AMTheme.Radius.sm.dp,
    val md: Dp = AMTheme.Radius.md.dp,
    val lg: Dp = AMTheme.Radius.lg.dp
)

// ---------------------------------------------------------------------------
// AMTokensInput — overridable input-primitive metric tokens
// ---------------------------------------------------------------------------

/**
 * Overridable metrics for text-input primitives.
 *
 * Fields mirror [AMTheme.Input] converted to [Dp].
 *
 * @param inset  Leading and trailing inset for input primitives (default 14dp).
 * @param radius Corner radius for input primitives (default 14dp).
 */
@Immutable
data class AMTokensInput(
    val inset:  Dp = AMTheme.Input.inset.dp,
    val radius: Dp = AMTheme.Input.radius.dp
)

// ---------------------------------------------------------------------------
// AMTokensMotion — overridable animation token set
// ---------------------------------------------------------------------------

/**
 * Overridable animation tokens — springs, easing curves, and dismiss thresholds.
 *
 * Fields mirror [AMTheme.Motion]. All spring and tween specs are typed as
 * `AnimationSpec<Float>`; at call sites that animate [Dp] or [androidx.compose.ui.unit.IntOffset],
 * reconstruct the equivalent typed spec using the same parameters:
 * ```kotlin
 * spring<Dp>(dampingRatio = 0.86f, stiffness = 384f)
 * ```
 *
 * @param snapSpring             Spring for snap-to-rest interactions (default: dampingRatio=0.86, stiffness=384).
 * @param presentSpring          Spring for surface presentations (default: dampingRatio=0.85, stiffness=321).
 * @param modalSpring            Spring for modal long-distance travel (default: dampingRatio=0.86, stiffness=224).
 * @param overlayFade            Tween for overlay fade in/out (default: 180ms EaseInOut).
 * @param pressFade              Tween for press-feedback glyph fade (default: 120ms LinearOutSlowIn).
 * @param selectionFade          Tween for selection-commit pill fade (default: 150ms EaseInOut).
 * @param dismissThreshold       Drag distance past which a dismiss commits (default 60dp).
 * @param dismissThresholdCompact Compact dismiss threshold for edge-anchored surfaces (default 30dp).
 */
@Immutable
data class AMTokensMotion(
    val snapSpring:              AnimationSpec<Float> = AMTheme.Motion.snapSpring,
    val presentSpring:           AnimationSpec<Float> = AMTheme.Motion.presentSpring,
    val modalSpring:             AnimationSpec<Float> = AMTheme.Motion.modalSpring,
    val overlayFade:             AnimationSpec<Float> = AMTheme.Motion.overlayFade,
    val pressFade:               AnimationSpec<Float> = AMTheme.Motion.pressFade,
    val selectionFade:           AnimationSpec<Float> = AMTheme.Motion.selectionFade,
    val dismissThreshold:        Dp = AMTheme.Motion.dismissThreshold.dp,
    val dismissThresholdCompact: Dp = AMTheme.Motion.dismissThresholdCompact.dp
)

// ---------------------------------------------------------------------------
// AMTokens — root overridable style token set
// ---------------------------------------------------------------------------

/**
 * Overridable style tokens for AndroidMtrls.
 *
 * The second tier of the two-tier token architecture:
 *
 * | Tier | Type | Override? |
 * |------|------|-----------|
 * | Locked signature tokens | [AMTheme] (`object`) | Never |
 * | Overridable style tokens | [AMTokens] (`data class`, [LocalAMTokens]) | Yes |
 *
 * [standard] defaults every field to its [AMTheme] counterpart so that apps
 * not calling [AMTokensProvider] render stock AndroidMtrls automatically.
 *
 * ## Reading tokens in components
 * ```kotlin
 * val tokens = LocalAMTokens.current
 * tokens.spacing.lg          // overridable spacing
 * tokens.radius.md           // overridable radius
 * tokens.semantic.info       // overridable tint
 * tokens.motion.snapSpring   // overridable animation spec
 *
 * // Locked values — read AMTheme directly:
 * AMTheme.Surface.primary.resolve(LocalAMIsDarkTheme.current)
 * AMTheme.hairlineWidth.dp
 * ```
 *
 * ## Override APIs
 *
 * Install a whole set:
 * ```kotlin
 * AMTokensProvider(tokens = brandTokens) { RootContent() }
 * ```
 *
 * Transform the inherited set (Kotlin equivalent of `.imTokens(_ transform:)`):
 * ```kotlin
 * AMTokensProvider(transform = { it.copy(spacing = it.spacing.copy(lg = 20.dp)) }) {
 *     FeedContent()
 * }
 * ```
 *
 * @param semantic Overridable semantic color tokens.
 * @param spacing  Overridable spacing-scale tokens.
 * @param radius   Overridable corner-radius tokens.
 * @param input    Overridable input-primitive metric tokens.
 * @param motion   Overridable animation tokens.
 */
@Immutable
data class AMTokens(
    val semantic: AMTokensSemantics = AMTokensSemantics(),
    val spacing:  AMTokensSpacing   = AMTokensSpacing(),
    val radius:   AMTokensRadius    = AMTokensRadius(),
    val input:    AMTokensInput     = AMTokensInput(),
    val motion:   AMTokensMotion    = AMTokensMotion()
) {
    companion object {
        /**
         * The standard token set — every field at its [AMTheme] default.
         *
         * Used as the default value for [LocalAMTokens]. Apps that never call
         * [AMTokensProvider] render stock AndroidMtrls.
         */
        val standard = AMTokens()
    }
}

// ---------------------------------------------------------------------------
// LocalAMTokens — CompositionLocal for the current token set
// ---------------------------------------------------------------------------

/**
 * `CompositionLocal` providing the current [AMTokens] to the composition subtree.
 *
 * Default is [AMTokens.standard] — all fields at their [AMTheme] values.
 *
 * Uses `staticCompositionLocalOf` because token overrides are per-subtree
 * configuration, not frame-by-frame state. A change triggers full recomposition
 * of all consumers in the subtree, which is acceptable for infrequent overrides.
 *
 * Read at component boundaries:
 * ```kotlin
 * val tokens = LocalAMTokens.current
 * ```
 *
 * Provide via [AMTokensProvider] rather than raw [CompositionLocalProvider].
 */
val LocalAMTokens = staticCompositionLocalOf { AMTokens.standard }

// ---------------------------------------------------------------------------
// AMTokensProvider — two override overloads
// ---------------------------------------------------------------------------

/**
 * Installs a complete [AMTokens] set into the composition subtree.
 *
 * Layers **inside** [AMThemeRoot] (or any root that provides [LocalAMIsDarkTheme])
 * and installs only [LocalAMTokens]; dark-theme state flows through untouched, so
 * a manual light/dark override placed above this provider keeps working.
 *
 * Use this overload to supply a fully customised brand token set:
 * ```kotlin
 * AMTokensProvider(tokens = brandTokens) {
 *     RootContent()
 * }
 * ```
 *
 * @param tokens  The [AMTokens] set to install. Replaces the inherited set entirely.
 * @param content The composition subtree to host.
 */
@Composable
fun AMTokensProvider(
    tokens:  AMTokens,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAMTokens provides tokens,
        content       = content
    )
}

/**
 * Transforms the inherited [AMTokens] set and re-installs the result into the
 * composition subtree.
 *
 * Layers **inside** [AMThemeRoot] (or any root that provides [LocalAMIsDarkTheme])
 * and installs only [LocalAMTokens]; dark-theme state flows through untouched, so
 * a manual light/dark override placed above this provider keeps working.
 *
 * This is the Kotlin/Compose translation of `.imTokens(_ transform:)`. Because
 * [AMTokens] is an `@Immutable data class`, the transform lambda receives the
 * inherited set and returns a **new copy** via `.copy(...)` rather than mutating
 * the original (`inout` in Swift):
 *
 * ```kotlin
 * // Tweak one spacing step in a compact-layout subtree:
 * AMTokensProvider(transform = { it.copy(spacing = it.spacing.copy(lg = 20.dp)) }) {
 *     FeedContent()
 * }
 * ```
 *
 * Both overloads share the name `AMTokensProvider`. Kotlin resolves them by
 * parameter type: `(AMTokens)` vs. `(AMTokens) -> AMTokens`.
 *
 * @param transform Lambda that receives the inherited [AMTokens] and returns the
 *                  modified set. Implement as a `.copy(...)` chain; do not cache
 *                  the result outside the lambda — the inherited set is resolved
 *                  at composition time.
 * @param content   The composition subtree to host.
 */
@Composable
fun AMTokensProvider(
    transform: (AMTokens) -> AMTokens,
    content:   @Composable () -> Unit
) {
    val inherited = LocalAMTokens.current
    CompositionLocalProvider(
        LocalAMTokens provides transform(inherited),
        content       = content
    )
}

// ---------------------------------------------------------------------------
// Token-derived padding extension properties
// ---------------------------------------------------------------------------

/**
 * Row inset padding derived from the current [AMTokens].
 *
 * Horizontal: [AMTokensSpacing.lg], Vertical: [AMTokensSpacing.md].
 *
 * iOS equivalent: `IMTokens.rowInsets` / `EdgeInsets.imRow` (top=12, leading=16,
 * bottom=12, trailing=16). Use for row-style cards and list rows that live inside
 * a card surface.
 */
val AMTokens.rowInsets: PaddingValues
    get() = PaddingValues(horizontal = spacing.lg, vertical = spacing.md)

/**
 * Block inset padding derived from the current [AMTokens].
 *
 * All four edges: [AMTokensSpacing.lg].
 *
 * iOS equivalent: `IMTokens.blockInsets` / `EdgeInsets.imBlock` (all=16).
 * Use for padded card content that fills the card width.
 */
val AMTokens.blockInsets: PaddingValues
    get() = PaddingValues(all = spacing.lg)

/**
 * List-row inset padding derived from the current [AMTokens].
 *
 * Horizontal: [AMTokensInput.inset], Vertical: [AMTokensSpacing.lg].
 *
 * iOS equivalent: `IMTokens.listRowInsets` / `EdgeInsets.imListRow`
 * (top=16, leading=14, bottom=16, trailing=14). Use for full-bleed list rows
 * that are their own surface (not nested inside a card).
 */
val AMTokens.listRowInsets: PaddingValues
    get() = PaddingValues(horizontal = input.inset, vertical = spacing.lg)

/**
 * Title-bar inset padding derived from the current [AMTokens].
 *
 * Horizontal: [AMTokensInput.inset], Vertical: 10dp (hardcoded — matches the iOS
 * literal `10` in `EdgeInsets.imTitleBar`; not driven by a spacing token on iOS).
 *
 * iOS equivalent: `IMTokens.titleBarInsets` / `EdgeInsets.imTitleBar`
 * (top=10, leading=14, bottom=10, trailing=14). Use for toolbar and panel
 * title-bar padding.
 */
val AMTokens.titleBarInsets: PaddingValues
    get() = PaddingValues(horizontal = input.inset, vertical = 10.dp)

// ---------------------------------------------------------------------------
// Previews (light + dark, as required by conventions)
// ---------------------------------------------------------------------------

@Preview(
    name = "AMTokens · Light",
    group = "AMTokens",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7  // Surface.page light
)
@Composable
private fun AMTokensPreviewLight() {
    AMThemeRoot {
        AMTokensProvider(tokens = AMTokens.standard) {
            AMTokensPreviewContent()
        }
    }
}

@Preview(
    name = "AMTokens · Dark",
    group = "AMTokens",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,  // Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AMTokensPreviewDark() {
    AMThemeRoot {
        AMTokensProvider(tokens = AMTokens.standard) {
            AMTokensPreviewContent()
        }
    }
}

@Composable
private fun AMTokensPreviewContent() {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier.padding(tokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)
    ) {
        // Spacing scale swatches — each bar width proportional to its dp value
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs)
        ) {
            listOf(
                tokens.spacing.xs,
                tokens.spacing.sm,
                tokens.spacing.md,
                tokens.spacing.lg,
                tokens.spacing.xl,
                tokens.spacing.xxl
            ).forEach { spacingDp ->
                Box(
                    modifier = Modifier
                        .size(width = spacingDp, height = 24.dp)
                        .background(
                            color = AMTheme.Semantic.info.resolve(isDark),
                            shape = amSquircle(tokens.radius.sm)
                        )
                )
            }
        }

        // Semantic color swatches
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            listOf(
                tokens.semantic.info,
                tokens.semantic.success,
                tokens.semantic.warning,
                tokens.semantic.destructive
            ).forEach { adaptive ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = adaptive.resolve(isDark),
                            shape = amSquircle(tokens.radius.sm)
                        )
                )
            }
        }

        // Surface card demonstrating radius lg and rowInsets
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AMTheme.rowMinHeight.dp)
                .background(
                    color  = AMTheme.Surface.primary.resolve(isDark),
                    shape  = amSquircle(tokens.radius.lg)
                )
                .padding(tokens.rowInsets)
        )
    }
}
