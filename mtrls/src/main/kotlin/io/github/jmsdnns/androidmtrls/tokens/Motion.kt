package io.github.jmsdnns.androidmtrls.tokens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

// ---------------------------------------------------------------------------
// The app-facing motion behavior layer.
//
// AndroidMtrls owns motion the way it owns spacing and color: the *values*
// (springs, tweens, thresholds) live on `AMTheme.Motion` / `AMTokensMotion`;
// this file adds the *behavior* on top of them — the single Reduce-Motion
// collapse policy and the one app-facing keyed crossfade. Every animating
// primitive routes its spec through the resolvers here so the whole system
// softens, stills, and re-tunes by one policy instead of each site
// re-deciding its own fallback.
//
// The iOS `imInstantSwitch(_:)` primitive is deliberately NOT ported: SwiftUI
// needs `withTransaction(\.disablesAnimations)` to suppress its implicit
// animations, but Compose has no implicit animations — a plain state
// assignment is already an instant switch. Reach for a plain `state = …` write
// where iOS would reach for `imInstantSwitch`.
// ---------------------------------------------------------------------------

/**
 * `CompositionLocal` carrying whether the system has asked for reduced motion.
 *
 * Installed by [AMThemeRoot] from the platform animator-duration-scale signal
 * (`Settings.Global.ANIMATOR_DURATION_SCALE == 0f`, the Android-native "remove
 * animations" toggle). Defaults to `false` so unhosted previews and tests
 * render full motion unless they opt in.
 *
 * The value is a `staticCompositionLocalOf`, so a test or demo can override it
 * for a subtree by wrapping content in
 * `CompositionLocalProvider(LocalAMReduceMotion provides true) { … }` — e.g. to
 * exercise the reduced-motion path without changing a device setting.
 *
 * Components do not read this directly to pick a spec; they pass their full
 * spring to [AMTokensMotion.resolved] / [AMTokensMotion.resolvedOrNull] with
 * `reduceMotion = LocalAMReduceMotion.current`, keeping the collapse in one
 * place.
 */
val LocalAMReduceMotion = staticCompositionLocalOf { false }

// ---------------------------------------------------------------------------
// Reduce-Motion policy — the single collapse point
// ---------------------------------------------------------------------------

/**
 * The one place the Reduce-Motion collapse lives. A component passes its own
 * full-motion spec; when [reduceMotion] is on the animation softens to the
 * gentle overlay fade, otherwise the full spec passes through unchanged.
 *
 * Every animating primitive routes its spec through here so the whole system
 * softens by one policy instead of each site re-deciding the fallback. Read
 * the flag from [LocalAMReduceMotion]:
 *
 * ```kotlin
 * val spec = tokens.motion.resolved(tokens.motion.snapSpring, LocalAMReduceMotion.current)
 * ```
 *
 * Generic over `T` so `spring<Dp>`, `spring<Float>`, `spring<IntOffset>` (and
 * every other typed spec) all route through the same policy.
 *
 * When [reduceMotion] is off this returns the exact instance passed in. When on
 * it returns a fresh reduce-motion fallback that mirrors
 * [AMTheme.Motion.overlayFade] (`tween(180, EaseInOut)`).
 *
 * @param full         The component's full-motion spec.
 * @param reduceMotion Whether reduced motion is active — pass
 *                     [LocalAMReduceMotion].current.
 */
fun <T> AMTokensMotion.resolved(
    full: AnimationSpec<T>,
    reduceMotion: Boolean
): AnimationSpec<T> =
    if (reduceMotion) {
        // Reduce-Motion fallback — the typed twin of AMTheme.Motion.overlayFade
        // (tween(180, EaseInOut)). That token is declared `<Float>` (the house
        // token type); this resolver is generic over `T`, which cannot be
        // assigned from the Float token directly, so the identical parameters
        // are reproduced here. If overlayFade is ever retuned, this must be
        // updated in lockstep.
        tween(durationMillis = 180, easing = EaseInOut)
    } else {
        full
    }

/**
 * Reduce-Motion resolver for sites that should animate to *nothing* under
 * Reduce Motion rather than softening to the overlay fade — an underline
 * slide, a FAB lift, a crossfade that simply snaps.
 *
 * Returns the full spec when [reduceMotion] is off (the exact instance passed
 * in), or `null` when it is on. Callers treat `null` as "do not animate":
 *
 * ```kotlin
 * val spec = tokens.motion.resolvedOrNull(tokens.motion.snapSpring, LocalAMReduceMotion.current)
 * val offset by animateDpAsState(target, animationSpec = spec ?: snap())
 * ```
 *
 * Generic over `T` so every typed spec routes through the same policy.
 *
 * @param full         The component's full-motion spec.
 * @param reduceMotion Whether reduced motion is active — pass
 *                     [LocalAMReduceMotion].current.
 */
fun <T> AMTokensMotion.resolvedOrNull(
    full: AnimationSpec<T>,
    reduceMotion: Boolean
): AnimationSpec<T>? =
    if (reduceMotion) null else full

/**
 * The `AnimatedVisibility`-compatible form of [resolved].
 *
 * `AnimatedVisibility`'s `fadeIn`/`fadeOut`/`slideIn*`/`slideOut*`/`expand*`/
 * `shrink*` builders require a [FiniteAnimationSpec], which the generic [resolved]
 * cannot promise (its result type is the wider [AnimationSpec]). This variant keeps
 * the finite type end-to-end **by construction — no cast**: when [reduceMotion] is
 * off it returns the exact [full] finite spec; when on it returns a fresh
 * `tween(180, EaseInOut)`, and `tween` is itself a [FiniteAnimationSpec],
 * so both branches are finite. Behavior is identical to [resolved]; only the static
 * type is narrowed. Route a transition's spec through here whenever the site needs a
 * finite spec.
 *
 * The reduce-motion fallback mirrors [AMTheme.Motion.overlayFade]
 * (`tween(180, EaseInOut)`) — retune it in lockstep with [resolved].
 *
 * @param full         The component's full-motion finite spec.
 * @param reduceMotion Whether reduced motion is active — pass
 *                     [LocalAMReduceMotion].current.
 */
fun <T> AMTokensMotion.resolvedFinite(
    full: FiniteAnimationSpec<T>,
    reduceMotion: Boolean
): FiniteAnimationSpec<T> =
    if (reduceMotion) {
        // Finite by construction — tween IS a FiniteAnimationSpec, so no cast is
        // needed. Lockstep twin of AMTheme.Motion.overlayFade (see `resolved`).
        tween(durationMillis = 180, easing = EaseInOut)
    } else {
        full
    }

/**
 * The `AnimatedVisibility`-compatible form of [resolvedOrNull] — for a transition
 * whose spec should collapse to *nothing* (a `snap`) under Reduce Motion rather than
 * softening to the overlay fade: a spatial slide, an alert pop, a full-screen panel
 * present.
 *
 * Returns the exact [full] finite spec when [reduceMotion] is off, or `null` when it
 * is on. Callers treat `null` as "do not animate" and substitute a finite `snap()`:
 *
 * ```kotlin
 * enter = slideInVertically(
 *     animationSpec = tokens.motion.resolvedFiniteOrNull(modalSpring, LocalAMReduceMotion.current) ?: snap(),
 * )
 * ```
 *
 * Like [resolvedFinite], the finite type is preserved by construction with no cast —
 * the non-null branch is the finite [full] itself.
 *
 * @param full         The component's full-motion finite spec.
 * @param reduceMotion Whether reduced motion is active — pass
 *                     [LocalAMReduceMotion].current.
 */
fun <T> AMTokensMotion.resolvedFiniteOrNull(
    full: FiniteAnimationSpec<T>,
    reduceMotion: Boolean
): FiniteAnimationSpec<T>? =
    if (reduceMotion) null else full

// ---------------------------------------------------------------------------
// AMCrossfade — the app-facing keyed crossfade
// ---------------------------------------------------------------------------

/**
 * Crossfade [content] whenever [targetState] changes — the app-facing
 * primitive for a root/phase swap that legitimately *wants* motion (a sign-in
 * wizard advancing a step, a screen changing mode).
 *
 * The Compose translation of iOS `imCrossfade(value:)`. Prefer this over a
 * hand-rolled `AnimatedContent`/transition: the fade rides the
 * [AMTheme.Motion.overlayFade] token (180ms [EaseInOut]) — the same shared fade
 * iOS's `imCrossfade` uses — so retuning that token moves the crossfade too. It
 * honors Reduce Motion via the house resolver — under [LocalAMReduceMotion] the
 * swap is instant (a `snap()`) with no fade at all. Keying is by [targetState],
 * so give it whatever drives the content (the wizard phase, the selected mode):
 *
 * ```kotlin
 * AMCrossfade(targetState = phase) { current ->
 *     when (current) {
 *         Phase.Email    -> EmailStep()
 *         Phase.Password -> PasswordStep()
 *         Phase.Done     -> DoneStep()
 *     }
 * }
 * ```
 *
 * @param targetState The keyed value that drives the swap; changing it
 *                    crossfades from the old content to the new.
 * @param modifier    Modifier applied to the crossfade container.
 * @param content     Renders the content for a given [targetState] value.
 */
@Composable
fun <T> AMCrossfade(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current
    Crossfade(
        targetState = targetState,
        modifier = modifier,
        // Rides the shared overlayFade token (180ms EaseInOut), routed through the
        // house Reduce-Motion resolver: full fade normally, `null → snap()`
        // (instant, no fade) under Reduce Motion. Mirrors iOS `imCrossfade`, which
        // animates through `resolvedOrNil(tokens.motion.overlayFade)`.
        // AMTheme.Motion.overlayFade is a TweenSpec (a FiniteAnimationSpec), the
        // type Crossfade requires.
        animationSpec = tokens.motion.resolvedFiniteOrNull(
            AMTheme.Motion.overlayFade,
            reduceMotion
        ) ?: snap(),
        content = content
    )
}
