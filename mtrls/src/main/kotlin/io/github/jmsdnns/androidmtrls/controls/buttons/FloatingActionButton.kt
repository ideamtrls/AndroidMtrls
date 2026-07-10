package io.github.jmsdnns.androidmtrls.controls.buttons

import android.content.res.Configuration
import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amHairline
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.resolvedOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Shared shape — the circular FAB silhouette
// ---------------------------------------------------------------------------

// The FAB container is a 56dp circle — the signature floating-primary
// silhouette (iOS parity: `Circle()`). Shared by the fill, the hairline, the
// bounded-ripple clip, and the animated shadow silhouette so all four agree on
// one outline.
private val AMFabShape = CircleShape

// ---------------------------------------------------------------------------
// AMFABAlignment — horizontal anchor for the resting position
// ---------------------------------------------------------------------------

/**
 * Horizontal alignment for [AMFloatingActionButton]'s resting position within
 * its container row.
 *
 * iOS equivalent: `HorizontalAlignment` values (`leading`, `center`, `trailing`)
 * used in `IMFloatingActionButton.init(alignment:offset:accessibilityLabel:action:)`.
 */
@Immutable
enum class AMFABAlignment {
    /** Anchor the FAB to the leading (start) edge of the container. */
    Leading,

    /** Anchor the FAB to the horizontal center of the container. Default. */
    Center,

    /** Anchor the FAB to the trailing (end) edge of the container. */
    Trailing,
}

// ---------------------------------------------------------------------------
// AMFloatingActionButton
// ---------------------------------------------------------------------------

/**
 * 56dp floating action button — a circle ([CircleShape]) filled with
 * [AMTheme.Surface].primary and bordered by [amHairline].
 *
 * The icon is always [Icons.Rounded.Add] — the FAB represents **the primary
 * affordance for adding something**. For other glyphs use `AMIconButton`.
 *
 * **Always draggable (iOS parity).** Every FAB supports the tap / long-press-lift
 * / drag gesture out of the box — there is no static mode. Persisting the dropped
 * position is opt-in via [offset]: pass a caller-owned state and the committed
 * displacement is written back on lift release; leave it `null` and the drag is
 * still fully live, backed by internal remembered state that resets if the FAB
 * leaves and re-enters the composition (see **Offset ownership**).
 *
 * **Gesture contract:**
 * - Quick tap (pointer up within 10dp tap slop) → [onClick]; the ripple sweep
 *   plays because [PressInteraction] events are emitted into the interaction
 *   source from inside the pointer loop.
 * - Long press (hold ≥ 300ms) → lifts the button with a
 *   [HapticFeedbackConstants.LONG_PRESS][android.view.HapticFeedbackConstants.LONG_PRESS]
 *   haptic and enters drag mode. While lifted, the live translation is applied
 *   without animation so dragging tracks the finger without lag. On lift
 *   release the accumulated translation is committed to [offset] and a
 *   [HapticFeedbackConstants.GESTURE_END][android.view.HapticFeedbackConstants.GESTURE_END]
 *   haptic confirms the drop.
 *
 * **Delta-accumulation drag math:** the drag accumulator sums
 * `change.positionChange()` deltas — NEVER absolute `change.position - downPos`.
 * The node itself moves with the live drag, so absolute math in node-local
 * coordinates would feed the applied offset back into the next reading and the
 * FAB would oscillate between finger and anchor ("sputter"). `positionChange()`
 * localizes current and previous positions with the node's transform at
 * dispatch, so the node's own movement cancels out. (iOS gets away with
 * absolute math because its `DragGesture` uses `.global` coordinates.)
 *
 * **Offset ownership:** to persist the dropped position, hoist [offset] into an
 * ancestor that outlives overlay transitions, because the FAB is often
 * conditionally removed from the composition when overlays open. If the same
 * composable that conditionally mounts the FAB owns the state — or if [offset] is
 * left `null` so the internal remembered state is used — the position resets on
 * remount. Recommended for persistence:
 * hoist to the scene root with `rememberSaveable { mutableStateOf(Offset.Zero) }`.
 *
 * **Z-order (content < FAB < overlay):** the FAB draws in normal composition
 * order — Compose has no top-layer escape, and `Modifier.zIndex` only reorders
 * siblings within one parent. Compose the FAB (or its container) as the last
 * child of the scene root, or lift its container with `Modifier.zIndex`, so a
 * dragged FAB never slides under scene content. Overlay hosts
 * ([AMActionsOverlay][io.github.jmsdnns.androidmtrls.surfaces.overlays.AMActionsOverlay]
 * and friends) wrap the scene as their `hostContent`, so overlay chrome and
 * content always draw above the FAB.
 *
 * **Layout & insets:** the FAB is placed inside a `fillMaxWidth` container padded
 * `spacing.xl` horizontally, and rests flush against the container's bottom edge
 * (no built-in bottom inset — iOS parity; the hub's `bottomInset` owns any lift).
 * Callers still add their own `navigationBarsPadding()` (or equivalent) outside
 * this composable so the FAB clears the system navigation bar.
 *
 * **Lift animation:** `spring(dampingRatio = 0.82f, stiffness = 503f)` (the
 * dp/Float conversion of iOS `spring(response: 0.28, dampingFraction: 0.82)`;
 * stiffness = (2π/0.28)²) drives scale 1.0→1.15 plus the shadow opacity, blur,
 * and y-offset. In static mode `isLifted` never flips, so the button rests at
 * scale 1.0 with the resting shadow. Under Reduce Motion the lift routes through
 * the house motion policy to a snap, so the scale and shadow reach their lifted
 * values instantly with no springing; the drag itself is unaffected.
 *
 * iOS equivalent: `IMFloatingActionButton` in
 * `Sources/Controls/Buttons/FloatingActionButton.swift`.
 *
 * @param alignment        Horizontal anchor before any drag offset applies.
 *   Defaults to [AMFABAlignment.Center] (iOS parity).
 * @param offset           `null` (default) → drag is backed by internal remembered
 *   state (fully live, but the dropped position is not persisted across remounts).
 *   A caller-owned [MutableState] holding the persisted drag displacement from the
 *   [alignment] anchor → the component writes the committed offset back on lift
 *   release so the position survives remounts. This MutableState API (not
 *   value/onValueChange) is deliberate: commits originate inside the gesture
 *   coroutine.
 * @param accessibilityLabel Accessibility label announced for the button.
 * @param onClick          Invoked on a quick tap that did not become a lift.
 */
@Composable
fun AMFloatingActionButton(
    alignment: AMFABAlignment = AMFABAlignment.Center,
    offset: MutableState<Offset>? = null,
    accessibilityLabel: String,
    onClick: () -> Unit,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current
    val reduceMotion = LocalAMReduceMotion.current
    val density = LocalDensity.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // Shared press source: drives the ripple via explicit PressInteraction
    // emission from the pointer loop (the FAB has no `clickable`).
    val interactionSource = remember { MutableInteractionSource() }

    // Every FAB is draggable (iOS parity). If the caller hoisted an [offset],
    // commits are written to it so the position persists; otherwise this internal
    // state backs the live drag (resets across remounts). Remembered
    // unconditionally so the composition slot is stable if [offset] flips null.
    val internalOffset = remember { mutableStateOf(Offset.Zero) }
    val dragOffset: MutableState<Offset> = offset ?: internalOffset

    // The pointerInput(Unit) gesture loop below never restarts, so it must reach
    // the LATEST onClick and resolved offset state through updated-state holders.
    // A directly-captured closure/state object would freeze at the first value: a
    // stale onClick would fire on tap, and a null→hoisted [offset] flip would
    // commit the drop to the wrong (internal) store.
    val currentOnClick by rememberUpdatedState(onClick)
    val currentDragOffset by rememberUpdatedState(dragOffset)

    // ── Gesture state ─────────────────────────────────────────────────────────
    // isLifted drives the spring-animated scale + shadow transitions.
    var isLifted by remember { mutableStateOf(false) }
    // liveDrag accumulates raw pointer translation while dragging.
    // Updated via direct mutableStateOf assignment (no animation spec) so the
    // button tracks the finger without springiness during a live drag.
    var liveDrag by remember { mutableStateOf(Offset.Zero) }

    // Tap slop in pixels (10dp converted at composition time).
    val tapSlopPx = with(density) { 10.dp.toPx() }

    // ── Lift spring ───────────────────────────────────────────────────────────
    // iOS: spring(response: 0.28, dampingFraction: 0.82)
    // stiffness = (2π / 0.28)² ≈ 503.
    val liftSpring = spring<Float>(dampingRatio = 0.82f, stiffness = 503f)

    // Lift is a spatial affordance, so under Reduce Motion it animates to *nothing*
    // (resolvedOrNull → null → snap()): the lift state still flips but the scale and
    // shadow jump to their targets instantly instead of springing. This is the
    // canonical resolvedOrNull "FAB lift" case named in tokens/Motion.kt. The live
    // drag position tracking (liveDrag → offset) stays 1:1 and is never routed.
    val liftSpec = tokens.motion.resolvedOrNull(liftSpring, reduceMotion) ?: snap()

    // ── Animated shadow + scale ───────────────────────────────────────────────
    val scale by animateFloatAsState(
        targetValue = if (isLifted) 1.15f else 1.0f,
        animationSpec = liftSpec,
        label = "FAB scale",
    )
    val shadowOpacity by animateFloatAsState(
        targetValue = if (isLifted) 0.35f else 0.15f,
        animationSpec = liftSpec,
        label = "FAB shadow opacity",
    )
    val shadowBlurDp by animateFloatAsState(
        targetValue = if (isLifted) 20f else 8f,
        animationSpec = liftSpec,
        label = "FAB shadow blur",
    )
    val shadowYDp by animateFloatAsState(
        targetValue = if (isLifted) 10f else 2f,
        animationSpec = liftSpec,
        label = "FAB shadow y",
    )

    // ── Resolved colors ───────────────────────────────────────────────────────
    val surfaceColor = AMTheme.Surface.primary.resolve(isDark)
    val iconColor = AMTheme.Text.primary.resolve(isDark)

    // ── Press / interaction modifier ──────────────────────────────────────────
    // Draggable FAB: the ripple is driven by explicit PressInteraction emission
    // from the pointer loop below, so the sweep still plays on tap.
    val pressModifier: Modifier =
        Modifier
            .indication(interactionSource, amRipple())
            .pointerInput(Unit) {
                // One recogniser handles tap, long-press lift, and drag —
                // mirroring the iOS DragGesture(minimumDistance: 0, .global).
                //
                // Plain vars inside pointerInput for gesture-scoped accumulators
                // (house rule: no remember-state written per pointer move).
                //
                // State flow:
                //   down → emit ripple Press, launch 300ms lift job → watch Move
                //   300ms elapses → isLifted = true, LONG_PRESS haptic, Move updates liveDrag
                //   up before 300ms → cancel job, check tap slop → onClick or discard
                //   up after lift → commit translation to offset.value + GESTURE_END haptic
                awaitPointerEventScope {
                    while (true) {
                        // Wait for the next Press event; discard stale Move/Release.
                        val downEvent = awaitPointerEvent()
                        if (downEvent.type != PointerEventType.Press) continue
                        val downChange = downEvent.changes.firstOrNull() ?: continue

                        // Plain var — accumulates translation for this gesture cycle.
                        var dragAccum = Offset.Zero
                        // Plain var — reference to the in-flight lift job.
                        var liftJob: Job? = null
                        // Plain var — set true when the gesture is cancelled/aborted
                        // by a system event so the resolution block skips onClick.
                        var gestureCancelled = false

                        // Emit a ripple Press so the state layer sweeps on touch-down
                        // even though drag mode does not use a `clickable`.
                        val press = PressInteraction.Press(downChange.position)
                        interactionSource.tryEmit(press)

                        // Launch the 300ms lift delay.
                        liftJob = scope.launch {
                            delay(300L)
                            isLifted = true
                            // Long-press haptic on lift (fine on minSdk 31).
                            view.performHapticFeedback(
                                android.view.HapticFeedbackConstants.LONG_PRESS
                            )
                        }

                        // Track Move and Release until the pointer is released.
                        var gestureEnded = false
                        while (!gestureEnded) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: run {
                                // No change info — abort and mark cancelled so the
                                // resolution block skips onClick/offset-commit.
                                liftJob?.cancel()
                                isLifted = false
                                liveDrag = Offset.Zero
                                interactionSource.tryEmit(PressInteraction.Cancel(press))
                                gestureCancelled = true
                                gestureEnded = true
                                continue
                            }
                            when (event.type) {
                                PointerEventType.Move -> {
                                    // Accumulate RELATIVE deltas. positionChange()
                                    // localizes current and previous positions with the
                                    // node's transform at dispatch time, so the node's
                                    // own movement (the offset modifier tracking
                                    // liveDrag) cancels out. Absolute math
                                    // (change.position - downPos) feeds the applied
                                    // offset back into the local coordinates and makes
                                    // the drag oscillate between finger and anchor.
                                    dragAccum += change.positionChange()
                                    if (isLifted) {
                                        // Direct assignment — no animation during live drag
                                        // so the button tracks the finger.
                                        liveDrag = dragAccum
                                    }
                                    change.consume()
                                }
                                PointerEventType.Release -> {
                                    liftJob?.cancel()
                                    dragAccum += change.positionChange()
                                    // Fade the ripple out on release.
                                    interactionSource.tryEmit(PressInteraction.Release(press))
                                    gestureEnded = true
                                    change.consume()
                                }
                                else -> {
                                    // Cancel or unknown — abort gesture cleanly; mark
                                    // cancelled so the resolution block skips
                                    // onClick and offset-commit.
                                    liftJob?.cancel()
                                    isLifted = false
                                    liveDrag = Offset.Zero
                                    interactionSource.tryEmit(PressInteraction.Cancel(press))
                                    gestureCancelled = true
                                    gestureEnded = true
                                }
                            }
                        }

                        // Resolve gesture outcome (same logic as iOS .onEnded).
                        // Skip entirely on cancel/abort — neither onClick nor
                        // offset-commit should fire; accumulated drag is discarded.
                        if (!gestureCancelled) {
                            if (isLifted) {
                                currentDragOffset.value = currentDragOffset.value + dragAccum
                                isLifted = false
                                liveDrag = Offset.Zero
                                // Confirm the committed drop.
                                view.performHapticFeedback(
                                    android.view.HapticFeedbackConstants.GESTURE_END
                                )
                            } else if (hypot(dragAccum.x, dragAccum.y) < tapSlopPx) {
                                currentOnClick()
                            }
                        }
                        // Reset liveDrag unconditionally (matches iOS `liveDrag = .zero`).
                        liveDrag = Offset.Zero
                    }
                }
            }

    // ── Layout: alignment row + offset ───────────────────────────────────────
    // Rests flush against the container's bottom edge (no built-in bottom inset,
    // iOS parity); the hub's bottomInset owns any lift. Horizontal inset spacing.xl.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.xl),
    ) {
        Box(
            modifier = Modifier
                .offset {
                    // Resting at the anchor + persisted/internal offset + live drag.
                    val base = dragOffset.value
                    IntOffset(
                        x = (base.x + liveDrag.x).roundToInt(),
                        y = (base.y + liveDrag.y).roundToInt(),
                    )
                }
                .align(
                    when (alignment) {
                        AMFABAlignment.Leading  -> Alignment.CenterStart
                        AMFABAlignment.Center   -> Alignment.Center
                        AMFABAlignment.Trailing -> Alignment.CenterEnd
                    }
                ),
        ) {
            FabCircle(
                scale = scale,
                shadowOpacity = shadowOpacity,
                shadowBlurDp = shadowBlurDp,
                shadowYDp = shadowYDp,
                surfaceColor = surfaceColor,
                iconColor = iconColor,
                accessibilityLabel = accessibilityLabel,
                pressModifier = pressModifier,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Internal: FabCircle — the 56dp drawn circle
// ---------------------------------------------------------------------------

/**
 * The visual 56dp circle that forms the FAB body.
 *
 * Renders the [surfaceColor] fill, [amHairline] border, animatable single-layer
 * shadow via [Modifier.drawBehind] + [BlurMaskFilter], scale via [graphicsLayer],
 * and the fixed [Icons.Rounded.Add] glyph centred at 22dp. The node is
 * `clip`'d to [AMFabShape] so the caller's [pressModifier] ripple stays bounded
 * to the circular silhouette.
 *
 * The shadow is a **single-layer** custom draw rather than `Modifier.amElevation`
 * because the FAB's lift choreography requires independently animatable shadow
 * opacity, blur, and y-offset. The standard two-layer `amElevation` recipe does
 * not expose those as animatable floats.
 *
 * @param scale          Animated scale factor (1.0 resting → 1.15 lifted).
 * @param shadowOpacity  Animated shadow opacity (0.15 resting → 0.35 lifted).
 * @param shadowBlurDp   Animated shadow Gaussian blur radius in dp (8 → 20).
 * @param shadowYDp      Animated shadow y-offset in dp (2 → 10).
 * @param surfaceColor   Resolved circle background fill.
 * @param iconColor      Resolved add-icon tint ([AMTheme.Text.primary]).
 * @param accessibilityLabel Announced by accessibility services.
 * @param pressModifier  The indication + pointerInput press/drag wiring,
 *   inserted over the clipped fill.
 */
@Composable
private fun FabCircle(
    scale: Float,
    shadowOpacity: Float,
    shadowBlurDp: Float,
    shadowYDp: Float,
    surfaceColor: Color,
    iconColor: Color,
    accessibilityLabel: String,
    pressModifier: Modifier,
) {
    Box(
        modifier = Modifier
            // Scale from the circle centre (graphicsLayer applies after layout).
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // Animatable single-layer shadow: drawn behind the fill so the blur
            // bleeds onto the substrate and is visible — matches the iOS
            // `.shadow(color:radius:y:)` recipe. The silhouette is a circle.
            .drawBehind {
                val blurPx = shadowBlurDp.dp.toPx()
                val yPx = shadowYDp.dp.toPx()
                val radiusPx = size.minDimension / 2f
                val shadowColor = Color.Black.copy(alpha = shadowOpacity)

                drawIntoCanvas { canvas ->
                    val paint = Paint()
                    @Suppress("DEPRECATION")
                    paint.asFrameworkPaint().apply {
                        isAntiAlias = true
                        // Color must be set on the framework paint so the full
                        // ARGB value (including alpha) reaches BlurMaskFilter.
                        // asFrameworkPaint() is the only stable API for attaching
                        // a BlurMaskFilter; the deprecation is noted but no
                        // alternative exists for this specific use case.
                        color = shadowColor.toArgb()
                        maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.save()
                    canvas.translate(0f, yPx)
                    canvas.drawCircle(
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = radiusPx,
                        paint = paint,
                    )
                    canvas.restore()
                }
            }
            // Fixed 56dp circle: clip (so the ripple bounds honor the circle),
            // background fill, press wiring, then hairline border.
            .size(56.dp)
            .clip(AMFabShape)
            .background(color = surfaceColor, shape = AMFabShape)
            .then(pressModifier)
            .amHairline(AMFabShape)
            // Accessibility: expose as a Button with the caller-supplied label.
            .semantics {
                role = Role.Button
                contentDescription = accessibilityLabel
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = Icons.Rounded.Add,
            contentDescription = null, // label is on the outer Box semantics node
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(iconColor),
        )
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark pair (conventions: every component file ends with these)
// ---------------------------------------------------------------------------

@Composable
private fun FabPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val draggableOffset = remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(vertical = 40.dp),
    ) {
        // Default — center placement (iOS parity) inside the fillMaxWidth Box.
        // Draggable out of the box; no hoisted offset, so its drop does not persist.
        AMFloatingActionButton(
            accessibilityLabel = "Add item",
            onClick = {},
        )
        // Leading placement with a hoisted offset, showing the persisted-drag path.
        AMFloatingActionButton(
            alignment = AMFABAlignment.Leading,
            offset = draggableOffset,
            accessibilityLabel = "Add item (persisted drag)",
            onClick = {},
        )
    }
}

@Preview(
    name = "AMFloatingActionButton · Light",
    group = "AMFloatingActionButton",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
)
@Composable
private fun AMFloatingActionButtonPreviewLight() {
    AMThemeRoot {
        FabPreviewContent()
    }
}

@Preview(
    name = "AMFloatingActionButton · Dark",
    group = "AMFloatingActionButton",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMFloatingActionButtonPreviewDark() {
    AMThemeRoot {
        FabPreviewContent()
    }
}
