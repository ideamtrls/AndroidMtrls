package io.github.jmsdnns.androidmtrls.surfaces.overlays

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.resolved
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Snap-back spring (mirrors AMTheme.Motion.snapSpring)
//
// Conventions: callers that need a typed spring for a non-Float target
// (Dp, IntOffset) reproduce the token as a file-level val with a lockstep
// comment. Here the Animatable target IS Float, so a file-level val suffices
// for both uses inside this file. Values are identical to Theme.kt:
//   dampingRatio = 0.86, stiffness = 384 (response=0.32s → (2π/0.32)² ≈ 384).
// ---------------------------------------------------------------------------

/** Spring that mirrors [AMTheme.Motion.snapSpring] for Float targets. */
private val snapSpringFloat = spring<Float>(dampingRatio = 0.86f, stiffness = 384f)

// ---------------------------------------------------------------------------
// Modifier.amTapCatcher
// ---------------------------------------------------------------------------

/**
 * Makes the receiver an invisible tap target across its full bounds.
 *
 * Fills the available space and installs a [detectTapGestures] handler so any
 * tap that lands inside is forwarded to [action]. The typical call site is a
 * `Color.clear`-equivalent `Box` layered behind a sheet or overlay so taps
 * that miss the foreground content dismiss it.
 *
 * Used by: `AMSheet` (tap-outside dismiss), `AMActionsOverlay`
 * (tap-outside dismiss).
 *
 * iOS equivalent: `View.imTapCatcher(action:)` in `Dismissal.swift`
 * (access level: `public` — exposed slightly wider there because iOS overlay
 * primitives live in the same module as callers; here `internal` matches the
 * package-internal scope needed by the overlays package).
 */
internal fun Modifier.amTapCatcher(action: () -> Unit): Modifier =
    this
        .fillMaxSize()
        .pointerInput(action) {
            detectTapGestures(onTap = { action() })
        }

// ---------------------------------------------------------------------------
// AMDragDismissDirection
// ---------------------------------------------------------------------------

/**
 * Axis along which [Modifier.amDragDismiss] recognizes drags.
 *
 * - [Up]   — used by top-anchored surfaces such as `AMBanner`
 *            (threshold: `tokens.motion.dismissThresholdCompact`, default 30 dp,
 *            overridable via AMTokensProvider).
 * - [Down] — used by bottom-anchored surfaces such as `AMToast` and `AMSheet`
 *            (threshold: `tokens.motion.dismissThreshold`, default 60 dp,
 *            overridable via AMTokensProvider).
 *
 * iOS equivalent: `IMDragDismissDirection` in `Dismissal.swift`.
 */
internal enum class AMDragDismissDirection {
    /** Upward drag — top-anchored overlays (`AMBanner`). */
    Up,

    /** Downward drag — bottom-anchored overlays (`AMToast`, `AMSheet`). */
    Down,
}

// ---------------------------------------------------------------------------
// Modifier.amDragDismiss
// ---------------------------------------------------------------------------

/**
 * Single-axis drag-to-dismiss gesture for transient overlay surfaces.
 *
 * Tracks vertical drags in [direction] and writes the signed **net** translation
 * (down minus up, since the gesture start) into [dragOffset] while the user is
 * dragging, so the caller can apply
 * `.offset { IntOffset(0, dragOffset.value.roundToInt()) }` to the surface for
 * live movement feedback. The surface follows the finger **both ways**: reversing
 * the drag lets the offset return toward `0`.
 *
 * On drag end the **net** translation (not a positive-only ratchet) is tested
 * against [threshold]:
 * - **Committed** (net past [threshold] in [direction]): [onDismiss] is called and
 *   [dragOffset] is reset to `0f` **without animation**, so only the surface's exit
 *   transition plays. Springing the offset home here would fight the removal
 *   transition — two curves, opposite directions, on the same view (iOS
 *   `Dismissal.swift:95-102`).
 * - **Rejected** (short drag or cancel): [dragOffset] is spring-animated back to
 *   `0f` using [AMTheme.Motion.snapSpring] via an [Animatable] bridge, routed
 *   through the house Reduce-Motion policy so it softens to the gentle overlay
 *   fade under Reduce Motion (see "Snap-back bridge" note below).
 *
 * **Net-translation tracking:** a signed running total accumulates every delta,
 * and [dragOffset] is written as that total clamped to the dismiss direction. This
 * mirrors the iOS `IMDragDismissModifier` `onChanged` handler, which assigns the
 * cumulative `value.translation.height`:
 * ```swift
 * case .up:   if h < 0 { dragOffset = h }
 * case .down: if h > 0 { dragOffset = h }
 * ```
 * A drag down to +70 then back up to net +40 leaves `dragOffset == 40` and does
 * **not** dismiss (net 40 < 60), matching iOS — a positive-only ratchet would have
 * dismissed on the stale 70.
 *
 * **Snap-back bridge:** iOS uses `withAnimation(snapSpring) { dragOffset = 0 }`.
 * In Compose the equivalent is:
 * 1. Create an `Animatable<Float>` seeded to the current [dragOffset] value.
 * 2. Animate it to `0f` with [AMTheme.Motion.snapSpring].
 * 3. Each frame, write `Animatable.value` back into [dragOffset] — the caller's
 *    offset modifier reads that state and recomposes automatically.
 * The animation runs inside the [pointerInput] coroutine scope via `launch { }`.
 *
 * **Minimum activation distance:** [minActivationDistance] (default `0.dp`)
 * gates the gesture — drag travel is silently discarded until total vertical
 * travel exceeds this value. No in-library overlay currently passes a non-zero
 * value; the parameter remains for hosts whose surfaces mix taps with drags.
 *
 * Used by:
 * - `AMSheet` — direction `Down`, threshold `tokens.motion.dismissThreshold`
 *   (default 60 dp, overridable via AMTokensProvider).
 * - `AMBanner` — direction `Up`, threshold `tokens.motion.dismissThresholdCompact`
 *   (default 30 dp, overridable via AMTokensProvider).
 * - `AMToast` — direction `Down`, threshold `tokens.motion.dismissThresholdCompact`
 *   (default 30 dp, overridable via AMTokensProvider).
 *
 * iOS equivalent: `View.imDragDismiss(direction:threshold:dragOffset:onDismiss:)`
 * and `IMDragDismissModifier` in `Dismissal.swift` (access level: package-internal).
 *
 * @param direction        Axis on which the dismiss drag is recognized.
 * @param threshold        Travel past which [onDismiss] is invoked on drag end.
 * @param dragOffset       Caller-owned [MutableState] written during dragging and
 *                         animated back to `0f` on drag end. Apply as
 *                         `Modifier.offset { IntOffset(0, dragOffset.value.roundToInt()) }`
 *                         on the surface.
 * @param onDismiss        Called once when the drag crosses [threshold]. Not called on cancel.
 * @param minActivationDistance Minimum total vertical travel before deltas are accumulated.
 *                         Default `0.dp` means no gating.
 */
internal fun Modifier.amDragDismiss(
    direction: AMDragDismissDirection,
    threshold: Dp,
    dragOffset: MutableState<Float>,
    onDismiss: () -> Unit,
    minActivationDistance: Dp = 0.dp,
): Modifier = composed {
    // Snap-back animations outlive the gesture callbacks, so they run in the
    // composition-scoped coroutine scope (house precedent: FAB, CardStack).
    val scope = rememberCoroutineScope()
    // House rule: route the snap-back spring through the Reduce-Motion policy so it
    // softens to the gentle overlay fade under Reduce Motion. Read at composable
    // scope (inside composed{}) and captured by the gesture callbacks below; the
    // live drag-follow tracking itself stays 1:1 and unrouted.
    val reduceMotion = LocalAMReduceMotion.current
    val tokens = LocalAMTokens.current
    val settleSpec = remember(reduceMotion) {
        tokens.motion.resolved(snapSpringFloat, reduceMotion)
    }
    pointerInput(direction, threshold, onDismiss, minActivationDistance) {
        // Convert Dp thresholds to pixels once per pointerInput block.
        val thresholdPx = threshold.toPx()
        val minActivationPx = minActivationDistance.toPx()

        // House gesture rule: accumulators are plain vars inside the pointerInput
        // block — never MutableState written per pointer-move event.
        // (Precedent: FAB and Slider gesture loops; rule in conventions.md.)
        //
        // `translation` is the signed cumulative net translation since the gesture
        // start (the Compose twin of iOS `value.translation.height`); `totalTravel`
        // is absolute travel, used only for the activation gate.
        var translation = 0f
        var totalTravel = 0f

        detectVerticalDragGestures(
            onDragStart = {
                translation = 0f
                totalTravel = 0f
            },
            onDragEnd = {
                // Commit test reads the NET translation, exactly like iOS:
                //   case .up:   value.translation.height < -threshold
                //   case .down: value.translation.height >  threshold
                val crossed = when (direction) {
                    AMDragDismissDirection.Up   -> translation < -thresholdPx
                    AMDragDismissDirection.Down -> translation > thresholdPx
                }
                if (crossed) {
                    // Committed dismissal — hand off entirely to the surface's exit
                    // transition. Reset the offset WITHOUT animation (iOS
                    // Dismissal.swift:95-102): an animated snap-back would fight the
                    // removal transition (two curves, opposite directions, one view).
                    onDismiss()
                    dragOffset.value = 0f
                } else {
                    // Rejected drag — spring the surface back to rest on snapSpring,
                    // softened to the gentle overlay fade under Reduce Motion. The
                    // Animatable is seeded from the current dragOffset so the spring
                    // starts from wherever the user released; each frame writes back
                    // into dragOffset.value so the caller's `.offset {}` recomposes.
                    val anim = Animatable(dragOffset.value)
                    scope.launch {
                        anim.animateTo(targetValue = 0f, animationSpec = settleSpec) {
                            dragOffset.value = value
                        }
                    }
                }

                translation = 0f
                totalTravel = 0f
            },
            onDragCancel = {
                // Cancel is a non-commit release — spring back to rest, no onDismiss.
                val anim = Animatable(dragOffset.value)
                scope.launch {
                    anim.animateTo(targetValue = 0f, animationSpec = settleSpec) {
                        dragOffset.value = value
                    }
                }

                translation = 0f
                totalTravel = 0f
            },
        ) { change, dragAmount ->
            change.consume()

            // Signed net translation (all deltas) + absolute travel (activation gate).
            translation += dragAmount
            totalTravel += kotlin.math.abs(dragAmount)

            // Ignore drags until total travel exceeds minActivationDistance.
            // Mirrors DragGesture(minimumDistance:) semantics from iOS.
            if (minActivationPx > 0f && totalTravel < minActivationPx) return@detectVerticalDragGestures

            // The surface follows the finger both ways: write the NET translation
            // clamped to the dismiss direction, mirroring iOS
            //   case .up:   if h < 0 { dragOffset = h }
            //   case .down: if h > 0 { dragOffset = h }
            // Reversing the drag returns the offset toward 0; it clamps at rest once
            // the net crosses back past the start (a positive-only ratchet would not).
            dragOffset.value = when (direction) {
                AMDragDismissDirection.Up   -> translation.coerceAtMost(0f)
                AMDragDismissDirection.Down -> translation.coerceAtLeast(0f)
            }
        }
    }
}
