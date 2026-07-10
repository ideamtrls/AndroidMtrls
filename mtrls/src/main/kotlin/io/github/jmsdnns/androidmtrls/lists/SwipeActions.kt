package io.github.jmsdnns.androidmtrls.lists

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMAdaptiveColor
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.resolved
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Swipe metrics & anchor keys
// ---------------------------------------------------------------------------

/** Width of each trailing action pane. Also the per-action open-offset step. */
private val AMSwipePaneWidth = 74.dp

/**
 * Neutral fill for a [AMSwipeActionRole.Normal] action that supplies no explicit
 * [AMSwipeAction.tint] — the adaptive systemGray2-equivalent gray iOS falls back
 * to (`#AEAEB2` light / `#636366` dark). Resolved against [LocalAMIsDarkTheme]
 * in [amSwipePaneFill].
 */
private val AMSwipeNormalFill = AMAdaptiveColor(
    light = Color(0xFFAEAEB2),
    dark = Color(0xFF636366),
)

/**
 * Fraction of an overdrag past the open/closed rest points that carries through
 * to the visible offset — the rest is absorbed, so pulling further meets rising
 * elastic resistance (rubber band) instead of sliding 1:1. Matches iOS
 * `IMSwipeableRow.overdragResistance = 0.3`.
 */
private const val AMSwipeOverdragResistance = 0.3f

/**
 * Settle threshold: the drag commits open once past **half** the open width.
 * Hoisted to a stable identity so [AnchoredDraggableDefaults.flingBehavior]
 * doesn't re-`remember` on every recomposition. Matches the anchored-drag
 * default, spelled out to lock the AMSwipeRow settle threshold.
 */
private val AMSwipePositionalThreshold: (Float) -> Float = { distance -> distance / 2f }

/** The two rest positions of an [AMSwipeRow]: fully closed, or fully open. */
private enum class AMSwipeAnchor { Closed, Open }

// ---------------------------------------------------------------------------
// amSwipeOpenOffset — pure anchor math (unit-tested in SwipeActionsTest.kt)
// ---------------------------------------------------------------------------

/**
 * The "Open" anchor offset (in px) for an [AMSwipeRow], as a **negative** value
 * so the content slides toward the start edge to reveal the trailing panes.
 *
 * [count] is coerced to **at least 1** before multiplying by [paneWidthPx], so a
 * defensive `0`/negative list size never produces a zero or positive offset. The
 * count is otherwise **uncapped** — the offset scales linearly with the number
 * of panes, mirroring iOS's uncapped `openWidth = count × actionWidth`.
 *
 * @param count       Number of trailing action panes requested. Coerced to `>= 1`.
 * @param paneWidthPx Width of a single pane, in px (typically [AMSwipePaneWidth]
 *                    resolved for the current density).
 * @return `-(coercedCount × paneWidthPx)` — always `<= 0`.
 */
internal fun amSwipeOpenOffset(count: Int, paneWidthPx: Float): Float {
    val coerced = count.coerceAtLeast(1)
    return -(coerced * paneWidthPx)
}

// ---------------------------------------------------------------------------
// AMSwipeActionRole — semantic treatment of a swipe action
// ---------------------------------------------------------------------------

/**
 * The intent behind an [AMSwipeAction], which drives its default pane fill.
 *
 * - [Normal] — an ordinary action (Archive, Flag). Fills with the action's
 *   explicit [AMSwipeAction.tint], or a neutral gray when none is given.
 * - [Destructive] — a dangerous action (Delete, Remove). Fills with
 *   [AMTheme.Semantic.destructive] by default so it reads as the hazardous
 *   anchor; an explicit [AMSwipeAction.tint] still overrides.
 */
@Immutable
enum class AMSwipeActionRole { Normal, Destructive }

// ---------------------------------------------------------------------------
// AMSwipeAction — one trailing swipe action
// ---------------------------------------------------------------------------

/**
 * One trailing action revealed behind an [AMSwipeRow] when the row is swiped
 * toward the start edge.
 *
 * Renders as a 74dp-wide, full-height pane centering a 20dp [icon] over a
 * [label] in `Color.White`. The pane fill is resolved from [tint] and [role]:
 * an explicit [tint] always wins; otherwise a [AMSwipeActionRole.Destructive]
 * action fills [AMTheme.Semantic.destructive] and a [AMSwipeActionRole.Normal]
 * one fills a neutral gray. The whole pane is tappable — tapping fires [onClick]
 * and animates the row closed.
 *
 * @param icon               The pane glyph, drawn at 20dp in `Color.White`.
 * @param label              Short caption under the glyph
 *                           ([AMTheme.Type.labelSmall], `Color.White`).
 * @param role               Semantic role — drives the default fill. Defaults to
 *                           [AMSwipeActionRole.Normal].
 * @param tint               Optional fill override. When non-null it replaces the
 *                           role's default fill (e.g. an indigo Archive pane).
 * @param contentDescription Accessibility label for the tappable pane
 *                           (announced by TalkBack; the visual [label] may be
 *                           terse, so give this a full description).
 * @param onClick            Invoked when the pane is tapped or the action is
 *                           triggered via TalkBack. The row then animates back to
 *                           the closed rest position.
 */
@Immutable
class AMSwipeAction(
    val icon: ImageVector,
    val label: String,
    val role: AMSwipeActionRole = AMSwipeActionRole.Normal,
    val tint: Color? = null,
    val contentDescription: String,
    val onClick: () -> Unit,
)

/**
 * The pane fill for [action]: an explicit [AMSwipeAction.tint] if present, else
 * the [AMSwipeActionRole] default ([AMTheme.Semantic.destructive] for
 * [AMSwipeActionRole.Destructive], the neutral adaptive [AMSwipeNormalFill]
 * resolved for [isDark] otherwise).
 */
private fun amSwipePaneFill(action: AMSwipeAction, isDark: Boolean): Color =
    action.tint ?: when (action.role) {
        AMSwipeActionRole.Destructive -> AMTheme.Semantic.destructive.resolve(isDark)
        AMSwipeActionRole.Normal -> AMSwipeNormalFill.resolve(isDark)
    }

// ---------------------------------------------------------------------------
// AMSwipeOverscrollEffect — rubber-band overdrag past the swipe rest points
// ---------------------------------------------------------------------------

/**
 * A custom [OverscrollEffect] that gives [AMSwipeRow] its elastic rubber-band:
 * the Foundation `anchoredDraggable` engine hard-clamps travel to
 * `[openOffset, 0]`, so any drag past the outermost anchor is dispatched here as
 * "excess" delta. This effect carries [AMSwipeOverdragResistance] (0.3) of that
 * excess through to a visible offset — applied by [node], a [LayoutModifierNode]
 * that shifts the wrapped content — and springs it back to rest on release.
 * Mirrors iOS `IMSwipeableRow.rubberBanded(_)`.
 *
 * Adapted from the AndroidX `OverscrollSample` offset-overscroll pattern
 * (horizontal axis, house snap-back spring). The row is only handed this effect
 * when Reduce Motion is **off**; under Reduce Motion the drag keeps the plain
 * hard clamp.
 *
 * @param scope Coroutine scope (from `rememberCoroutineScope`) driving the
 *              tension snap/release animations off the composition thread.
 */
private class AMSwipeOverscrollEffect(
    private val scope: CoroutineScope,
) : OverscrollEffect {
    private val overscrollOffset = Animatable(0f)

    /**
     * Snap-back spring for the released overdrag. Updated from the composable to
     * the house `snapSpring` (routed through the motion policy) so the elastic
     * return matches the row's settle.
     */
    var settleSpec: AnimationSpec<Float> = spring()

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        // Pre-scroll relaxation: when a drag reverses against existing tension,
        // release the tension first before handing the remainder to the drag.
        val sameDirection = sign(delta.x) == sign(overscrollOffset.value)
        val consumedByPreScroll =
            if (abs(overscrollOffset.value) > 0.5f && !sameDirection) {
                val prev = overscrollOffset.value
                val next = prev + delta.x
                if (sign(prev) != sign(next)) {
                    // Delta more than cancels the tension → snap to rest, pass
                    // the remainder on to the drag.
                    scope.launch { overscrollOffset.snapTo(0f) }
                    Offset(x = delta.x + prev, y = 0f)
                } else {
                    // Delta partially reduces the tension → absorb it all.
                    scope.launch { overscrollOffset.snapTo(next) }
                    delta.copy(y = 0f)
                }
            } else {
                Offset.Zero
            }
        val leftForScroll = delta - consumedByPreScroll
        val consumedByScroll = performScroll(leftForScroll)
        val overscrollDelta = leftForScroll - consumedByScroll
        // Any drag delta the anchors couldn't consume becomes rubber-banded
        // overdrag (0.3 of the excess).
        if (abs(overscrollDelta.x) > 0.5f && source == NestedScrollSource.UserInput) {
            scope.launch {
                overscrollOffset.snapTo(
                    overscrollOffset.value + overscrollDelta.x * AMSwipeOverdragResistance,
                )
            }
        }
        return consumedByPreScroll + consumedByScroll
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        val consumed = performFling(velocity)
        val remaining = velocity - consumed
        // Spring the elastic overdrag back to rest; the anchored fling settles
        // the row to its nearest anchor in parallel.
        overscrollOffset.animateTo(
            targetValue = 0f,
            initialVelocity = remaining.x,
            animationSpec = settleSpec,
        )
    }

    override val isInProgress: Boolean
        get() = overscrollOffset.value != 0f

    override val node: DelegatableNode =
        object : Modifier.Node(), LayoutModifierNode {
            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints,
            ): MeasureResult {
                val placeable = measurable.measure(constraints)
                return layout(placeable.width, placeable.height) {
                    placeable.placeRelativeWithLayer(overscrollOffset.value.roundToInt(), 0)
                }
            }
        }
}

// ---------------------------------------------------------------------------
// AMSwipeRow — swipe-to-reveal trailing actions
// ---------------------------------------------------------------------------

/**
 * Wraps a row of [content] with swipe-to-reveal trailing actions.
 *
 * Dragging the content toward the start edge reveals the [trailingActions] as
 * action panes pinned to the end edge. The gesture snaps between two rest
 * positions via the Foundation `anchoredDraggable` engine: **Closed** at `0f`
 * and **Open** at `amSwipeOpenOffset(actions, paneWidthPx)` — a negative offset
 * equal to `-(n × 74dp)`. The settle commits open past a positional threshold of
 * **half the open width** (plus the standard **125 dp/s** velocity threshold, so
 * a short flick commits), and animates with the house `tokens.motion.snapSpring`
 * routed through [LocalAMReduceMotion] — under Reduce Motion the settle softens
 * to the gentle overlay-fade tween.
 *
 * **Rubber-band overdrag.** Dragging past either rest point carries only 30% of
 * the excess (a custom `OverscrollEffect`), meeting rising elastic resistance and
 * springing back on release — matching iOS `IMSwipeableRow.rubberBanded`. Reduce
 * Motion drops the elastic overdrag for a plain hard clamp.
 *
 * **Open-state duality.** By default the row self-manages its open state. Pass a
 * hoisted [isOpen] + [onOpenChange] pair to control or observe it from a
 * parent — mirroring [AMReorderableRowList]'s `isEditing`: `null` [isOpen] means
 * self-managed, a non-null value makes the row controlled (the drag settle and
 * every close report through [onOpenChange]; the caller writes the value back).
 * A parent can force-close a row (e.g. on navigation) by setting [isOpen] false.
 *
 * **Tap an open row to close.** While open, a transparent catcher over the
 * shifted content swallows its own taps and animates the row closed — so a stray
 * tap dismisses the tray instead of leaking through to the row body.
 *
 * **Any number of actions.** All of [trailingActions] are honored (iOS is
 * uncapped); **≤3 is the practical guidance** — each pane is a fixed 74dp, so
 * beyond three the open row slides nearly off a phone's width. An empty list
 * renders [content] unchanged with no swipe gesture attached.
 *
 * **Opaque content required.** The action panes sit *behind* [content], so
 * [content] must paint an opaque background or the panes bleed through at rest.
 * Typically [content] is an [AMListRow] (which fills `AMTheme.Surface.primary`)
 * inside a card, or a row on a flat `Surface.primary` strip.
 *
 * **Accessibility.** TalkBack can't perform the swipe, so each action is exposed
 * as a custom accessibility action on the row (announced through the actions
 * menu), routed through the same `onClick` a tap takes. The behind-tray panes
 * are hidden from accessibility while the row is closed so a resting row never
 * announces phantom "Delete"/"Archive" buttons.
 *
 * **RTL.** "Trailing" means the layout end edge. Foundation resolves the
 * `anchoredDraggable` direction against the ambient layout direction, so in RTL
 * the reveal mirrors to the start-visual edge; the panes still render end-most
 * within the row. LTR is the primary tested direction.
 *
 * **Visual only otherwise** — like the rest of `lists/`, tapping [content]
 * itself is the caller's job (attach `Modifier.clickable` inside the slot).
 * Only the revealed panes carry the [AMSwipeAction.onClick] wiring here.
 *
 * ```kotlin
 * AMSwipeRow(
 *     trailingActions = listOf(
 *         AMSwipeAction(Icons.Filled.Delete, "Delete", role = AMSwipeActionRole.Destructive,
 *             contentDescription = "Delete item") { … },
 *     ),
 * ) {
 *     AMListRow(title = "Weekly digest", subtitle = "Latent Notes")
 * }
 * ```
 *
 * @param trailingActions Actions revealed on swipe. All honored (≤3 recommended);
 *                        empty disables the gesture.
 * @param isOpen          Hoisted open state. `null` (default) self-manages it;
 *                        a non-null value makes the row controlled.
 * @param onOpenChange    Called with the new open state whenever the row opens or
 *                        closes (drag settle, action tap, or tap-to-close). Pair
 *                        it with [isOpen] to control the row.
 * @param modifier        Layout modifier applied to the root [Box].
 * @param content         The row content. Must carry an opaque background.
 */
@Composable
fun AMSwipeRow(
    trailingActions: List<AMSwipeAction>,
    isOpen: Boolean? = null,
    onOpenChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // Empty list: no panes to reveal — render content as-is, no drag.
    if (trailingActions.isEmpty()) {
        Box(modifier = modifier) { content() }
        return
    }

    // All actions are honored (iOS is uncapped; ≤3 is the practical guidance).
    val panes = trailingActions
    val count = panes.size
    val isDark = LocalAMIsDarkTheme.current

    // Open-state duality (mirrors AMReorderableRowList's isEditing): the row
    // owns internal state only when uncontrolled (isOpen == null); a supplied
    // isOpen makes it controlled. setOpen writes through to whichever owns it
    // and always notifies onOpenChange, so a controlled parent stays in sync.
    var internalOpen by rememberSaveable { mutableStateOf(false) }
    val open = isOpen ?: internalOpen
    val setOpen: (Boolean) -> Unit = { next ->
        if (isOpen == null) internalOpen = next
        onOpenChange?.invoke(next)
    }

    val paneWidthPx = with(LocalDensity.current) { AMSwipePaneWidth.toPx() }
    val openOffset = amSwipeOpenOffset(count, paneWidthPx)

    // House rule: route the animated settle through the motion policy so it
    // softens under Reduce Motion. Stable identity (remember) keeps the fling
    // behavior from re-remembering each recomposition.
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current
    val settleSpec = remember(tokens.motion.snapSpring, reduceMotion) {
        tokens.motion.resolved(tokens.motion.snapSpring, reduceMotion)
    }

    // Rubber-band overdrag (iOS-parity): elastic travel past the outermost anchor
    // at 0.3 resistance, springing back on release. The effect only receives the
    // drag's excess delta when Reduce Motion is off (see anchoredDraggable below);
    // under Reduce Motion the drag keeps the plain hard clamp. Snap-back reuses
    // the row's settle spring.
    val overscrollScope = rememberCoroutineScope()
    val overscroll = remember(overscrollScope) { AMSwipeOverscrollEffect(overscrollScope) }
    overscroll.settleSpec = settleSpec

    // Anchors are set in the constructor so the offset is initialized before
    // first layout — requireOffset() below never throws. Keyed on openOffset so
    // a density change rebuilds with the correct open anchor; the initial anchor
    // reflects the incoming open state.
    val state = remember(openOffset) {
        AnchoredDraggableState(
            initialValue = if (open) AMSwipeAnchor.Open else AMSwipeAnchor.Closed,
            anchors = DraggableAnchors {
                AMSwipeAnchor.Closed at 0f
                AMSwipeAnchor.Open at openOffset
            },
        )
    }

    // external → engine: reconcile the drag position with the logical open state
    // (drives the animated open/close on a controlled change, tap-to-close, and
    // action-tap close).
    LaunchedEffect(open, openOffset) {
        val target = if (open) AMSwipeAnchor.Open else AMSwipeAnchor.Closed
        if (state.currentValue != target) {
            state.animateTo(target, settleSpec)
        }
    }

    // engine → external: report a drag that settles at a different anchor than
    // the logical state. rememberUpdatedState avoids a stale capture in the
    // long-lived collector; the equality guard makes redundant emissions no-ops
    // (so the two directions can't loop).
    val currentOpen by rememberUpdatedState(open)
    val reportOpen by rememberUpdatedState(setOpen)
    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }.collect { settled ->
            val nowOpen = settled == AMSwipeAnchor.Open
            if (nowOpen != currentOpen) reportOpen(nowOpen)
        }
    }

    // Per-pane interaction sources, keyed on count — never remembered inside a
    // variable-trip-count loop (conventions.md gesture/state rules).
    val interactionSources = remember(count) {
        List(count) { MutableInteractionSource() }
    }
    // Indication-free tap-catcher source (tap-to-dismiss exception).
    val catcherInteraction = remember { MutableInteractionSource() }

    Box(modifier = modifier) {
        // Behind the content: right-aligned (end) action panes, full row height.
        // Hidden from accessibility while closed so a resting row doesn't
        // announce phantom action buttons (clearAndSetSemantics — a stable API,
        // matching the project's no-experimental stance; the row's custom
        // actions are the a11y path while closed).
        Row(
            modifier = Modifier
                .matchParentSize()
                .then(if (open) Modifier else Modifier.clearAndSetSemantics {}),
            horizontalArrangement = Arrangement.End,
        ) {
            panes.forEachIndexed { index, action ->
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(AMSwipePaneWidth)
                        .background(amSwipePaneFill(action, isDark))
                        .clickable(
                            interactionSource = interactionSources[index],
                            indication = amRipple(),
                        ) {
                            action.onClick()
                            setOpen(false)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Image(
                        imageVector = action.icon,
                        contentDescription = action.contentDescription,
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicText(
                        text = action.label,
                        style = AMTheme.Type.labelSmall.toTextStyle(Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Content on top — slides horizontally by the drag offset. The lambda
        // form of offset defers the read to placement, so requireOffset() is
        // read after anchors are initialized. Each action is surfaced as a
        // custom accessibility action on the merged row node.
        Box(
            modifier = Modifier
                .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                // Renders the rubber-band overdrag: the effect's LayoutModifierNode
                // shifts this content by the elastic excess. Paired with the
                // overscrollEffect handed to anchoredDraggable below.
                .overscroll(overscroll)
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal,
                    // Excess drag past the anchors is dispatched to the rubber-band
                    // effect — unless Reduce Motion is on, where the drag hard-clamps.
                    overscrollEffect = if (reduceMotion) null else overscroll,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                        state = state,
                        positionalThreshold = AMSwipePositionalThreshold,
                        animationSpec = settleSpec,
                    ),
                )
                .semantics(mergeDescendants = true) {
                    customActions = panes.map { action ->
                        CustomAccessibilityAction(action.label) {
                            // Match the pane-tap path (and iOS `trigger`): fire the
                            // action, then close the row around the call.
                            action.onClick()
                            setOpen(false)
                            true
                        }
                    }
                },
        ) {
            content()

            // While open, a transparent catcher over the shifted content
            // swallows its own taps and animates the row closed. Indication-free
            // is correct here (tap-to-dismiss exception); its semantics are
            // cleared so it adds no phantom "activate" action to the row.
            if (open) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = catcherInteraction,
                            indication = null,
                        ) { setOpen(false) }
                        .clearAndSetSemantics {},
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

// Preview icons come from material-icons-core (all :mtrls has): Delete for the
// destructive action, Email standing in for an "archive" normal action. Real
// callers (e.g. the demo, which has material-icons-extended) use richer glyphs.
// Delete leans on the Destructive role's default fill; Archive is a Normal
// action with an explicit info-tint override.
private val previewActions: List<AMSwipeAction> = listOf(
    AMSwipeAction(
        icon = Icons.Filled.Delete,
        label = "Delete",
        role = AMSwipeActionRole.Destructive,
        contentDescription = "Delete item",
        onClick = {},
    ),
    AMSwipeAction(
        icon = Icons.Filled.Email,
        label = "Archive",
        // Preview-only specimen (top-level val, no composable scope): pin the
        // light-mode hue. Real callers pass a resolved color per mode.
        tint = AMTheme.Semantic.info.light,
        contentDescription = "Archive item",
        onClick = {},
    ),
)

@Composable
private fun SwipeActionsPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.page.resolve())
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        // Closed rest state — panes hidden behind the opaque row.
        AMSwipeRow(trailingActions = previewActions) {
            AMListRow(
                title = "Weekly digest",
                subtitle = "Swipe left to reveal actions",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Static open-state approximation — a bare offset can't be previewed
        // (drag/AnchoredDraggable needs runtime), so the panes are shown next to
        // a shortened row so light/dark pane fills are visible in the preview.
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                AMListRow(
                    title = "Trip receipts",
                    subtitle = "Open-state pane approximation",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            previewActions.forEach { action ->
                Column(
                    modifier = Modifier
                        .height(64.dp)
                        .width(AMSwipePaneWidth)
                        .background(amSwipePaneFill(action, isDark)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Image(
                        imageVector = action.icon,
                        contentDescription = action.contentDescription,
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicText(
                        text = action.label,
                        style = AMTheme.Type.labelSmall.toTextStyle(Color.White),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(
    name = "AMSwipeRow · Light",
    group = "AMSwipeRow",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMSwipeRowPreviewLight() {
    AMThemeRoot {
        SwipeActionsPreviewContent()
    }
}

@Preview(
    name = "AMSwipeRow · Dark",
    group = "AMSwipeRow",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMSwipeRowPreviewDark() {
    AMThemeRoot {
        SwipeActionsPreviewContent()
    }
}
