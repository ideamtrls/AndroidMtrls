package io.github.jmsdnns.androidmtrls.collections

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.resolved
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Private tuning constants — bespoke stack physicality, deliberately NOT
// tokenized (CLAUDE.md §1.4 note: "the stack's physicality is tuned as one
// piece and shouldn't drift under theme overrides").
// ---------------------------------------------------------------------------

/** Each card behind the top shrinks by this factor per depth step. */
private const val DEPTH_SCALE_FACTOR = 0.96f

/** Vertical drop per depth step in dp — makes layers peek below the top card. */
private const val DEPTH_Y_OFFSET_DP = 8f

/** Each card behind the top fades by this factor per depth step. */
private const val DEPTH_OPACITY_FACTOR = 0.72f

/** Maximum tilt angle ±12° — physical but restrained. */
private const val MAX_TILT_DEGREES = 12f

/** Fraction of card width at which the tilt reaches [MAX_TILT_DEGREES]. */
private const val TILT_FULL_AT_WIDTH_FRACTION = 0.5f

/** Drag distance, as a fraction of width, past which release commits. */
private const val COMMIT_DISTANCE_FRACTION = 0.25f

/** Predicted velocity-driven travel, as a fraction of width, past which a quick flick commits. */
private const val COMMIT_VELOCITY_FRACTION = 0.6f

/** How far off-screen the flung card travels (as a fraction of width). */
private const val FLING_DISTANCE_FRACTION = 1.6f

/** How much of the gesture's predicted vertical travel carries into the fling. */
private const val FLING_VERTICAL_CARRY = 0.3f

// ---------------------------------------------------------------------------
// Private motion constants — bespoke, not tokenized (same rationale)
// ---------------------------------------------------------------------------

/**
 * Fling-off animation.
 *
 * iOS `.easeOut(duration: 0.28)` maps to `LinearOutSlowInEasing` per
 * conventions.md ("iOS .easeOut converts to LinearOutSlowInEasing").
 * The spec sketch listed FastOutLinearInEasing — the conventions.md ruling
 * overrides the spec draft; LinearOutSlowInEasing is the correct port.
 */
private val flingAnimation = tween<Float>(
    durationMillis = 280,
    easing = LinearOutSlowInEasing, // corrected: conventions.md iOS .easeOut → LinearOutSlowInEasing
)

/**
 * Snap-back spring for a drag released short of commit thresholds.
 *
 * Slightly bouncier than [stackSettleSpring] — rejection should feel elastic.
 * iOS: `.spring(response: 0.35, dampingFraction: 0.78)`.
 * stiffness ≈ (2π / 0.35)² ≈ 320.
 */
private val snapBackSpring = spring<Float>(stiffness = 320f, dampingRatio = 0.78f)

/**
 * Depth-promotion spring for the stack after a swipe.
 *
 * iOS: `.spring(response: 0.42, dampingFraction: 0.82)`.
 * stiffness ≈ (2π / 0.42)² ≈ 224.
 */
private val stackSettleSpring = spring<Float>(stiffness = 224f, dampingRatio = 0.82f)

// ---------------------------------------------------------------------------
// AMSwipeDirection
// ---------------------------------------------------------------------------

/**
 * Direction the top card flew off in.
 *
 * Semantics are entirely the caller's responsibility — [AMCardStack] simply
 * reports which way the gesture went.
 *
 * iOS equivalent: `IMCardStack.SwipeDirection`.
 */
@Immutable
enum class AMSwipeDirection {
    /** Card swiped toward the leading (left) edge. */
    LEFT,

    /** Card swiped toward the trailing (right) edge. */
    RIGHT,
}

// ---------------------------------------------------------------------------
// AMCardStack
// ---------------------------------------------------------------------------

/**
 * Tinder-style swipeable card stack.
 *
 * Renders the top item plus up to [visibleDepth]-1 cards peeking behind it.
 * The top card is draggable and flings off screen on a horizontal swipe that
 * crosses the distance or velocity commit thresholds; it springs back on a
 * short/slow release.
 *
 * **Sizing:** [AMCardStack] requires an explicit height from the caller.
 * Wrap the call site in a modifier that supplies a fixed or intrinsic height:
 * ```kotlin
 * AMCardStack(…, modifier = Modifier.height(360.dp))
 * ```
 * Width is filled automatically via `Modifier.fillMaxWidth()` inside.
 *
 * **Swipe contract:** [onSwipe] fires the moment the exit animation completes
 * (the fling `animateTo` is joined, so the report self-adjusts to the real,
 * possibly Reduce-Motion-softened, duration — no hand-tuned delay). The caller
 * must remove the swiped item from the list inside [onSwipe] to advance the
 * stack. Failing to do so re-presents the same card.
 *
 * **Accessibility:** the top card exposes TalkBack custom actions
 * "Swipe left" / "Swipe right" that run the same fling → [onSwipe] path a drag
 * would, so the deck is fully operable without the gesture.
 *
 * **Reduce Motion:** a committed card fades out in place (no cross-screen
 * translation) rather than flinging; the depth-promotion and snap-back springs
 * soften to the gentle overlay fade.
 *
 * **Empty list:** renders nothing. Callers are responsible for showing their
 * own empty state when the list empties.
 *
 * **Card content:** wrap [content] in [AMCard] for the standard floating
 * surface treatment (hairline, shadow, rounded corners). The stack itself
 * renders no card chrome.
 *
 * iOS equivalent: `IMCardStack` in `Sources/Collections/CardStack.swift`.
 *
 * @param items        Ordered list of data items; index 0 is the top card.
 * @param key          Stable identity function — maps each item to a unique
 *                     key (`{ it.id }`). Used to tag the dragging card and
 *                     avoid new-top-card race conditions.
 * @param visibleDepth Number of cards rendered simultaneously (top + peeks).
 *                     Clamped to at least 1. Defaults to 3.
 * @param onSwipe      Callback invoked when the exit animation completes.
 *                     Receives the swiped item and [AMSwipeDirection].
 *                     Remove the item from your state to advance the stack.
 * @param modifier     Modifier applied to the stack container. Supply an
 *                     explicit height here; width fills automatically.
 * @param content      Composable slot for each card. Receives the item for
 *                     the card to render. Wrap with [AMCard] for surface chrome.
 */
@Composable
fun <T> AMCardStack(
    items: List<T>,
    key: (T) -> Any,
    visibleDepth: Int = 3,
    onSwipe: (item: T, direction: AMSwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    if (items.isEmpty()) return

    val clampedDepth = maxOf(1, visibleDepth)
    val scope = rememberCoroutineScope()
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current
    val density = LocalDensity.current.density

    // Keep the deck-mutation guard and the committed-swipe report reading the
    // LATEST props. The per-card `pointerInput(itemKey)` block (and the
    // `launchCommit` reference it captures) outlives recompositions of the same
    // top card, so a directly-captured `items`/`onSwipe` would freeze at
    // composition time — the mid-fling guard below could not see an external
    // mutation, and a committed swipe would call an outdated `onSwipe`. Routing
    // both through `rememberUpdatedState` (a stable State holder) keeps the
    // long-lived gesture closure reading current values (precedent: Stepper.kt
    // hold loop, Carousel.kt collector).
    val currentItems by rememberUpdatedState(items)
    val currentOnSwipe by rememberUpdatedState(onSwipe)

    // Motion routed through the house Reduce-Motion policy (tokens/Motion.kt).
    // Under RM each bespoke spring/tween collapses to the overlay-fade tween via
    // resolved(), so motion still MOVES to its end state — a rejected card still
    // returns to centre and the promoted stack still restyles, just without the
    // spring. The one exception is the fling: under RM a committed card does NOT
    // travel across the screen (that large translation is exactly what RM
    // removes) — it fades out in place instead (fadeAlpha below), then reports.
    // resolved (not resolvedOrNull): softened motion reads as "reduced", a
    // teleport would read as broken. The 1:1 drag tracking (snapTo per pointer
    // move) and the post-swipe reset snapTo stay instant regardless.
    val flingSpec = tokens.motion.resolved(flingAnimation, reduceMotion)
    val snapBackSpec = tokens.motion.resolved(snapBackSpring, reduceMotion)
    val depthSettleSpec = tokens.motion.resolved(stackSettleSpring, reduceMotion)

    // ── Drag animatables ──────────────────────────────────────────────────────
    // These are the bridge between the raw pointer accumulator (plain var) and
    // the UI layer (graphicsLayer). Only two Animatables are needed — they
    // always apply to whichever card is tagged by draggingKey.
    val dragOffsetX = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }
    // Reduce-Motion fade-out of the committed top card (1 → 0). Only the card
    // tagged by fadingOutKey reads it — mirrors iOS `fadingOutID`.
    val fadeAlpha = remember { Animatable(1f) }

    // ── State-tagging trick (ported verbatim from iOS) ────────────────────────
    // draggingKey is set to key(topCard) at drag start. After the exit animation
    // completes and onSwipe fires, draggingKey is reset to null. Because the
    // swiped item has been removed from `items` by then, no rendered card
    // matches the key — the new top card renders at rest without a fragile
    // reset-in-lockstep dance. fadingOutKey plays the same role for the RM
    // fade-out path.
    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var fadingOutKey by remember { mutableStateOf<Any?>(null) }

    // Measured stack width (px), captured from the root Box, so the TalkBack
    // custom actions can compute a fling exit target without a live gesture.
    var containerWidthPx by remember { mutableIntStateOf(0) }

    // ── Shared commit choreography ────────────────────────────────────────────
    // Runs the fling (or RM fade) → onSwipe → reset path for a committed swipe.
    // Shared by the drag gesture and the TalkBack custom actions so both advance
    // the deck identically. The report fires exactly when the exit animation
    // completes — the fling `animateTo`s are joined (not a fixed delay), so it
    // self-adjusts to the real (possibly RM-softened) duration. It re-reads the
    // current top and drops a stale report if the deck changed mid-fling, exactly
    // like iOS `report(topID:)`.
    fun launchCommit(itemKey: Any, direction: AMSwipeDirection, exitDx: Float, exitDy: Float) {
        scope.launch {
            if (reduceMotion) {
                // No cross-screen fling — fade in place from wherever the card
                // was released, then advance the moment the fade finishes.
                fadingOutKey = itemKey
                fadeAlpha.animateTo(0f, depthSettleSpec)
            } else {
                val x = launch { dragOffsetX.animateTo(exitDx, flingSpec) }
                val y = launch { dragOffsetY.animateTo(exitDy, flingSpec) }
                x.join()
                y.join()
            }
            // Report on completion, guarded against external mid-fling mutation.
            // Re-read the LIVE list (currentItems, tracked via rememberUpdatedState)
            // so a deck the caller mutated during the fling is actually seen here.
            val currentTop = currentItems.firstOrNull()
            if (currentTop != null && key(currentTop) == itemKey) {
                currentOnSwipe(currentTop, direction)
            }
            // State-tagging reset for the next card (changes no visible output —
            // the swiped item is already gone / off-screen).
            draggingKey = null
            fadingOutKey = null
            dragOffsetX.snapTo(0f)
            dragOffsetY.snapTo(0f)
            fadeAlpha.snapTo(1f)
        }
    }

    // TalkBack cannot perform the drag, so the two directions are exposed as
    // named custom actions that route into the same commit path a released
    // swipe takes — zero velocity (predicted Y = 0), flinging from the measured
    // width, exactly like iOS's `accessibilityAction(named:)`.
    fun performAccessibleSwipe(direction: AMSwipeDirection) {
        val item = currentItems.firstOrNull() ?: return
        val itemKey = key(item)
        val isRight = direction == AMSwipeDirection.RIGHT
        val (exitDx, exitDy) = amCardStackExitTargets(
            dx = if (isRight) 1f else -1f,
            dy = 0f,
            velocityY = 0f,
            isRight = isRight,
            width = containerWidthPx.toFloat(),
            flingDistanceFraction = FLING_DISTANCE_FRACTION,
            flingVerticalCarry = FLING_VERTICAL_CARRY,
        )
        draggingKey = itemKey
        launchCommit(itemKey, direction, exitDx, exitDy)
    }

    // ── Visible entries ───────────────────────────────────────────────────────
    val visibleItems = items.take(clampedDepth)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { containerWidthPx = it.width }
            .semantics {
                val total = items.size
                contentDescription = "Card stack, card 1 of $total"
            },
    ) {
        // Draw deepest card first so the top card is rendered last (on top).
        // Compose Box stacks children: last composable = highest z-order.
        for (depth in visibleItems.lastIndex downTo 0) {
            val item = visibleItems[depth]
            val itemKey = key(item)
            val isTop = depth == 0

            // ── Per-depth visual transforms animated with stackSettleSpring ─
            // animateFloatAsState is used here for depth-based properties
            // because each card's depth number can change as items are swiped
            // and the list advances — the spring animation is triggered by the
            // target value changing.
            val depthScaleTarget = amCardStackDepthScale(depth, DEPTH_SCALE_FACTOR)
            val depthAlphaTarget = amCardStackDepthAlpha(depth, DEPTH_OPACITY_FACTOR)

            val animatedDepthScale by animateFloatAsState(
                targetValue = depthScaleTarget,
                animationSpec = depthSettleSpec,
                label = "depthScale-$itemKey",
            )
            val animatedDepthAlpha by animateFloatAsState(
                targetValue = depthAlphaTarget,
                animationSpec = depthSettleSpec,
                label = "depthAlpha-$itemKey",
            )
            // Depth y-offset springs on the SAME stackSettleSpring as scale/alpha,
            // so a promoted card (depth 1 → 0) glides up smoothly instead of
            // snapping its vertical position while the scale/opacity spring (iOS
            // animates scale, yOffset, and opacity together).
            val animatedDepthYOffsetPx by animateFloatAsState(
                targetValue = amCardStackDepthYOffsetPx(depth, density, DEPTH_Y_OFFSET_DP),
                animationSpec = depthSettleSpec,
                label = "depthY-$itemKey",
            )

            val isDragTarget = isTop && (draggingKey == itemKey)
            val isFadingOut = fadingOutKey == itemKey

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Scale — from depth presentation (animated)
                        scaleX = animatedDepthScale
                        scaleY = animatedDepthScale
                        // Drag contribution — only for the tagged card; depth peek
                        // (animated) always applies.
                        translationX = if (isDragTarget) dragOffsetX.value else 0f
                        translationY = animatedDepthYOffsetPx + if (isDragTarget) dragOffsetY.value else 0f
                        // Tilt — only for the tagged top card
                        rotationZ = if (isDragTarget) {
                            amCardStackRotationForDx(
                                dx = dragOffsetX.value,
                                width = size.width,
                                maxTiltDegrees = MAX_TILT_DEGREES,
                                tiltFullAtWidthFraction = TILT_FULL_AT_WIDTH_FRACTION,
                            )
                        } else 0f
                        // Alpha — depth presentation, times the Reduce-Motion
                        // fade-out (1f unless this is the committed fading card).
                        alpha = animatedDepthAlpha * if (isFadingOut) fadeAlpha.value else 1f
                    }
                    .then(
                        // Gesture + a11y actions only on the top card
                        if (isTop) {
                            Modifier
                                .semantics {
                                    // TalkBack swipe equivalents — route into the same
                                    // commit path as a released drag (iOS named actions).
                                    customActions = listOf(
                                        CustomAccessibilityAction(label = "Swipe right") {
                                            performAccessibleSwipe(AMSwipeDirection.RIGHT)
                                            true
                                        },
                                        CustomAccessibilityAction(label = "Swipe left") {
                                            performAccessibleSwipe(AMSwipeDirection.LEFT)
                                            true
                                        },
                                    )
                                }
                                .pointerInput(itemKey) {
                                // Plain var accumulators — house rule: no remember-state
                                // written per pointer-move event (conventions.md).
                                var accDx = 0f
                                var accDy = 0f
                                var gestureCancelled = false
                                val velocityTracker = VelocityTracker()
                                // Generation counter — guards against a new drag starting
                                // mid-spring clearing the new gesture's draggingKey.
                                var gestureGeneration = 0

                                detectDragGestures(
                                    onDragStart = {
                                        gestureGeneration++
                                        draggingKey = itemKey
                                        accDx = 0f
                                        accDy = 0f
                                        gestureCancelled = false
                                        velocityTracker.resetTracking()
                                    },
                                    onDragEnd = {
                                        if (!gestureCancelled) {
                                            val velocity = velocityTracker.calculateVelocity()
                                            val cardWidth = size.width.toFloat()

                                            val shouldCommit = amCardStackShouldCommit(
                                                dx = accDx,
                                                velocityX = velocity.x,
                                                width = cardWidth,
                                                commitDistanceFraction = COMMIT_DISTANCE_FRACTION,
                                                commitVelocityFraction = COMMIT_VELOCITY_FRACTION,
                                            )

                                            if (shouldCommit) {
                                                val isRight = accDx >= 0f
                                                val direction = if (isRight) {
                                                    AMSwipeDirection.RIGHT
                                                } else {
                                                    AMSwipeDirection.LEFT
                                                }
                                                val (exitDx, exitDy) = amCardStackExitTargets(
                                                    dx = accDx,
                                                    dy = accDy,
                                                    velocityY = velocity.y,
                                                    isRight = isRight,
                                                    width = cardWidth,
                                                    flingDistanceFraction = FLING_DISTANCE_FRACTION,
                                                    flingVerticalCarry = FLING_VERTICAL_CARRY,
                                                )

                                                // draggingKey is already itemKey (set at drag
                                                // start). Run the shared fling (or RM fade) →
                                                // onSwipe → reset choreography; the report fires
                                                // on animation completion, not a fixed delay.
                                                launchCommit(itemKey, direction, exitDx, exitDy)
                                            } else {
                                                // Snap back — both axes spring home concurrently.
                                                // draggingKey must stay set while the spring plays so
                                                // the card's graphicsLayer continues reading dragOffset
                                                // and rotation. Clear only after both axes settle,
                                                // but only if no new gesture has started since.
                                                val gen = gestureGeneration
                                                scope.launch {
                                                    val x = launch { dragOffsetX.animateTo(0f, snapBackSpec) }
                                                    val y = launch { dragOffsetY.animateTo(0f, snapBackSpec) }
                                                    x.join(); y.join()
                                                    if (gen == gestureGeneration) draggingKey = null
                                                }
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        // Gesture cancelled by system (incoming call, scroll
                                        // conflict) — snap back immediately. FAB gestureCancelled
                                        // precedent (conventions.md).
                                        // draggingKey cleared inside the coroutine after both
                                        // axes settle so the card stays visible during the spring.
                                        gestureCancelled = true
                                        val gen = gestureGeneration
                                        scope.launch {
                                            val x = launch { dragOffsetX.animateTo(0f, snapBackSpec) }
                                            val y = launch { dragOffsetY.animateTo(0f, snapBackSpec) }
                                            x.join(); y.join()
                                            if (gen == gestureGeneration) draggingKey = null
                                        }
                                    },
                                ) { change, dragAmount ->
                                    // Plain var accumulation — house rule.
                                    change.consume()
                                    accDx += dragAmount.x
                                    accDy += dragAmount.y
                                    velocityTracker.addPosition(
                                        timeMillis = change.uptimeMillis,
                                        position = change.position,
                                    )
                                    // snapTo = no animation; card tracks finger without lag.
                                    scope.launch {
                                        dragOffsetX.snapTo(accDx)
                                        dragOffsetY.snapTo(accDy)
                                    }
                                }
                            }
                        } else {
                            Modifier
                        }
                    ),
            ) {
                content(item)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (conventions: every component file ends with these)
// ---------------------------------------------------------------------------

private data class PreviewCard(val id: Int, val title: String, val body: String)

@Composable
private fun CardStackPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    val cards = remember {
        listOf(
            PreviewCard(1, "The Long Way Round", "Episode 1 · 42 min"),
            PreviewCard(2, "Signals in the Noise", "Episode 2 · 38 min"),
            PreviewCard(3, "Quiet Machines", "Episode 3 · 45 min"),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(tokens.spacing.lg),
    ) {
        AMCardStack(
            items = cards,
            key = { it.id },
            visibleDepth = 3,
            onSwipe = { _, _ -> },
            modifier = Modifier.height(240.dp),
        ) { card ->
            AMCard(
                modifier = Modifier.fillMaxSize(),
                padding = AMPadding.block,
            ) {
                BasicText(
                    text = card.title,
                    style = AMTheme.Type.title.toTextStyle(
                        AMTheme.Text.primary.resolve(isDark),
                    ),
                )
                BasicText(
                    text = card.body,
                    style = AMTheme.Type.caption.toTextStyle(
                        AMTheme.Text.secondary.resolve(isDark),
                    ),
                )
            }
        }
    }
}

@Preview(
    name = "AMCardStack · Light",
    group = "AMCardStack",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    widthDp = 390,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMCardStackPreviewLight() {
    AMThemeRoot {
        CardStackPreviewContent()
    }
}

@Preview(
    name = "AMCardStack · Dark",
    group = "AMCardStack",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    widthDp = 390,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMCardStackPreviewDark() {
    AMThemeRoot {
        CardStackPreviewContent()
    }
}
