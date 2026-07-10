package io.github.jmsdnns.androidmtrls.collections

import kotlin.math.abs

// ---------------------------------------------------------------------------
// AMCardStack pure math helpers — internal, Compose-runtime-free
// ---------------------------------------------------------------------------
//
// These functions encode the fling geometry for [AMCardStack]. They are
// extracted here so they can be JVM-unit-tested without a Compose runtime.
//
// The private tuning constants (DEPTH_SCALE_FACTOR, MAX_TILT_DEGREES, etc.)
// live in CardStack.kt alongside the composable that owns them; each function
// accepts those values as explicit Float parameters so the math can be tested
// with the literal expected values and the composable can pass its private
// constants without exposing them.

/**
 * Predicted-fling projection window in seconds — the iOS-equivalent of
 * SwiftUI's `DragGesture.Value.predictedEndTranslation`.
 *
 * SwiftUI's prediction follows the UIScrollView deceleration model. The
 * canonical WWDC-2018 ("Designing Fluid Interfaces") projection with
 * `decelerationRate.normal = 0.998` gives
 * `projection = velocity × decel / (1000 × (1 − decel)) = velocity × 0.499` —
 * an effective ~0.5s window. [AMCardStack] extrapolates
 * `predicted = offset + velocity × this` so a quick flick commits exactly as
 * it does on iOS. The constant belongs to the card stack alone: [AMCarousel]
 * is HorizontalPager-backed and does no fling projection of its own (its
 * paging physics are the platform pager's native ones).
 *
 * Chosen over `androidx.compose.animation.splineBasedDecay` deliberately: the
 * spline decay needs a `Density` and the Compose runtime, which would break
 * the Compose-free JVM math test classpath these functions are extracted for.
 * A plain linear projection matches iOS `predictedEndTranslation` 1:1 and
 * stays deterministically unit-testable.
 */
internal const val AM_FLING_PROJECTION_SECONDS = 0.499f

/**
 * Computes the tilt rotation angle (in degrees) for the top card based on
 * its horizontal drag offset.
 *
 * The card tilts linearly from 0° at dx=0 up to ±[maxTiltDegrees] once
 * the drag reaches [tiltFullAtWidthFraction] × [width]. Beyond that
 * fraction the angle is clamped.
 *
 * iOS equivalent: `rotation(for:width:)` in `CardStack.swift`.
 *
 * @param dx                    Current horizontal drag offset in pixels.
 * @param width                 Card width in pixels. Returns 0 when ≤ 0.
 * @param maxTiltDegrees        Maximum tilt angle (default 12°).
 * @param tiltFullAtWidthFraction Fraction of [width] at which maximum tilt
 *                              is reached (default 0.5).
 * @return Rotation angle in degrees; sign matches [dx].
 */
internal fun amCardStackRotationForDx(
    dx: Float,
    width: Float,
    maxTiltDegrees: Float = 12f,
    tiltFullAtWidthFraction: Float = 0.5f,
): Float {
    if (width <= 0f) return 0f
    val normalized = (dx / (width * tiltFullAtWidthFraction)).coerceIn(-1f, 1f)
    return normalized * maxTiltDegrees
}

/**
 * Decides whether a drag release should commit (fling off) or snap back.
 *
 * A commit fires when **either** threshold is crossed:
 * - Distance: `|dx| > width * commitDistanceFraction`
 * - Velocity: `|predictedDx| > width * commitVelocityFraction`
 *
 * `predictedDx` mirrors iOS's `predictedEndTranslation.width` by projecting
 * the current velocity over the iOS deceleration window
 * ([AM_FLING_PROJECTION_SECONDS], ~0.5s):
 * `predictedDx = dx + velocityX * AM_FLING_PROJECTION_SECONDS`
 *
 * iOS equivalent: commit-or-snap logic in `dragGesture(width:topID:).onEnded`.
 *
 * @param dx                       Current horizontal drag offset in pixels.
 * @param velocityX                Horizontal fling velocity in pixels/second.
 * @param width                    Card width in pixels.
 * @param commitDistanceFraction   Distance threshold as a fraction of width
 *                                 (default 0.25).
 * @param commitVelocityFraction   Predicted-distance threshold as a fraction
 *                                 of width (default 0.6).
 * @return `true` → commit (fling); `false` → snap back.
 */
internal fun amCardStackShouldCommit(
    dx: Float,
    velocityX: Float,
    width: Float,
    commitDistanceFraction: Float = 0.25f,
    commitVelocityFraction: Float = 0.6f,
): Boolean {
    val predictedDx = dx + velocityX * AM_FLING_PROJECTION_SECONDS
    val beyondDistance = abs(dx) > width * commitDistanceFraction
    val beyondVelocity = abs(predictedDx) > width * commitVelocityFraction
    return beyondDistance || beyondVelocity
}

/**
 * Computes the fling exit target (exitDx, exitDy) for a committed swipe.
 *
 * - `exitDx = ±width * flingDistanceFraction` (sign from [isRight])
 * - `exitDy = currentDy + predictedDy * flingVerticalCarry`
 *   where `predictedDy = dy + velocityY * AM_FLING_PROJECTION_SECONDS`
 *   (the same iOS deceleration projection as the horizontal commit test)
 *
 * iOS equivalent: `flingOff(direction:width:predictedY:)` in `CardStack.swift`.
 *
 * @param dx                   Current horizontal drag offset in pixels
 *                             (used indirectly through [isRight]).
 * @param dy                   Current vertical drag offset in pixels.
 * @param velocityY            Vertical fling velocity in pixels/second.
 * @param isRight              `true` for a rightward swipe, `false` for left.
 * @param width                Card width in pixels.
 * @param flingDistanceFraction How far off-screen (as fraction of width)
 *                             the card travels (default 1.6).
 * @param flingVerticalCarry   Fraction of predicted vertical travel carried
 *                             into the fling trajectory (default 0.3).
 * @return Pair of (exitDx, exitDy) in pixels.
 */
internal fun amCardStackExitTargets(
    dx: Float,
    dy: Float,
    velocityY: Float,
    isRight: Boolean,
    width: Float,
    flingDistanceFraction: Float = 1.6f,
    flingVerticalCarry: Float = 0.3f,
): Pair<Float, Float> {
    val exitDx = if (isRight) width * flingDistanceFraction else -(width * flingDistanceFraction)
    val predictedDy = dy + velocityY * AM_FLING_PROJECTION_SECONDS
    val exitDy = dy + predictedDy * flingVerticalCarry
    return Pair(exitDx, exitDy)
}

/**
 * Per-depth scale factor for the card stack presentation.
 *
 * `scale = depthScaleFactor ^ depth`
 *
 * @param depth           Stack depth (0 = top card).
 * @param depthScaleFactor Per-step shrink factor (default 0.96).
 * @return Scale multiplier in (0, 1].
 */
internal fun amCardStackDepthScale(
    depth: Int,
    depthScaleFactor: Float = 0.96f,
): Float = Math.pow(depthScaleFactor.toDouble(), depth.toDouble()).toFloat()

/**
 * Per-depth vertical translation in pixels for the card stack presentation.
 *
 * `yOffsetPx = depthYOffsetDp * depth * density`
 *
 * @param depth          Stack depth (0 = top card).
 * @param density        Display density (pixels per dp) from [LocalDensity].
 * @param depthYOffsetDp Per-step y-offset in dp (default 8).
 * @return Vertical translation in pixels.
 */
internal fun amCardStackDepthYOffsetPx(
    depth: Int,
    density: Float,
    depthYOffsetDp: Float = 8f,
): Float = depthYOffsetDp * depth * density

/**
 * Per-depth alpha for the card stack presentation.
 *
 * `alpha = depthOpacityFactor ^ depth`
 *
 * @param depth              Stack depth (0 = top card).
 * @param depthOpacityFactor Per-step opacity multiplier (default 0.72).
 * @return Alpha value in (0, 1].
 */
internal fun amCardStackDepthAlpha(
    depth: Int,
    depthOpacityFactor: Float = 0.72f,
): Float = Math.pow(depthOpacityFactor.toDouble(), depth.toDouble()).toFloat()
