---
scope: List row/card primitives, the two drag-reorderable list containers (sh.calvin.reorderable engine), and the swipe-to-reveal action row
globs:
  - mtrls/src/**/lists/**
keywords: [AMListRow, AMListCard, AMReorderableRowList, AMReorderableCardList, amMoveItem, drag handle, reorder, onReorder, onMove, isEditing, isDragging, LazyColumn, ReorderableItem, draggableHandle, longPressDraggableHandle, selection ring, selectionRing, chevron, height pinning, haptics, AMSwipeRow, AMSwipeAction, AMSwipeActionRole, role, tint, isOpen, onOpenChange, hoisted open state, tap to close, customActions, CustomAccessibilityAction, clearAndSetSemantics, settleSpec, LocalAMReduceMotion, amSwipeOpenOffset, swipe actions, swipe to reveal, trailing actions, anchoredDraggable, DraggableAnchors, AnchoredDraggableState]
updated: 2026-07-10
---

# Lists

`lists/` holds the list row/card primitives, their drag-reorderable containers,
and the swipe-to-reveal action row.

A deliberate 2×2: two visual-only shape primitives (`AMListRow` flat,
`AMListCard` card) and their matching reorderable containers
(`AMReorderableRowList`, `AMReorderableCardList`). There is **no static list
container** — stack rows with `Column(verticalArrangement = Arrangement.spacedBy(0.dp))`
(`AMListRow` paints its own `Surface.primary` fill, so no shared background);
stack cards with `Arrangement.spacedBy(12.dp)` (`spacing.md`). Those four are
visual-only per `conventions.md` — no `onClick` on the row/card/container
primitives; callers attach `Modifier.clickable(indication = null, …)` at the
call site. `AMSwipeRow` is the one exception: it wraps a row and wires
`onClick` on its own revealed action panes (the row body it wraps stays
tap-agnostic).

## AMListRow (`ListRow.kt`)

Full-bleed row: `fillMaxWidth()`, `AMTheme.Surface.primary` background, no
corners, no shadow — sits flush against neighbours. Root is a
`Row(verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(tokens.spacing.md))`
padded with `tokens.listRowInsets` (horizontal `input.inset` = 14dp, vertical
`spacing.lg` = 16dp). Slot order inside the Row:

1. `leading: (@Composable () -> Unit)?` — nullable slot, skipped when `null`
   (the Kotlin replacement for iOS `EmptyView` overload matrices).
2. Text-area `Box(contentAlignment = CenterStart)` holding the `content` slot.
3. Flexible `Spacer(Modifier.weight(1f).defaultMinSize(minWidth = 8.dp))` — a
   greedy weight (`fill = true`) that expands to pin `trailing` + chevron to the
   row's end edge; `defaultMinSize(8dp)` keeps an 8dp minimum gap when long
   content leaves no slack (iOS `Spacer(minLength: sm)`). **Must stay
   `fill = true`** — `fill = false` collapses the spacer to its 8dp min, so
   trailing content floats mid-width instead of pinning to the end.
4. `trailing: (@Composable () -> Unit)?` — nullable slot.
5. Chevron, when `showsChevron = true` — always to the *right* of `trailing`,
   so a row can pair a status tag with the drill-down glyph. Opt-in: the
   default is `false` (Android rows don't decorate navigation by default);
   set it per drill-down row that pushes a detail view.

Key facts:

- **Height pinning.** The text-area Box contains an invisible
  `Modifier.alpha(0f)` sizer Column — `BasicText(" ")` at title style over
  `BasicText(" ")` at subtitle style, `spacedBy(2.dp)` — so every row gets a
  two-line minimum height; subtitle-less rows don't shrink, short content
  centres vertically via the Box alignment.
- **Selection ring.** `isSelected = true` draws a 1.5dp
  (`AMTheme.selectionRingWidth`) `AMTheme.selectionRing` outline inside the row
  bounds via `Modifier.amHairline(RectangleShape, color = selectionRing, width =
  selectionRingWidth.dp)` — a `strokeBorder`-equivalent stroke (the analog of iOS
  `Rectangle().strokeBorder`). Applied before the insets padding so the ring
  frames the whole row over the untouched `Surface.primary` interior; selecting
  never changes the row's metrics. `amHairline` strokes fully inside bounds so
  the ring never clips against an adjacent row.
- **Chevron glyph.** `material-icons-core` has no `chevron.right`; the
  substitute is `Icons.AutoMirrored.Filled.KeyboardArrowRight` at 16dp
  (caption-equivalent), tinted `AMTheme.Icon.muted`, `contentDescription = null`
  (decorative). AutoMirrored handles RTL.
- **Text styles** are file-level constants (hoisted to avoid per-recomposition
  allocation): `AMListRowTitleStyle` = `AMTheme.Type.title`, `AMListRowSubtitleStyle`
  = `AMTheme.Type.caption`. Colors are theme-dependent, applied per-call via
  `style.copy(color = …)`.
- **String overload** (`title: String, subtitle: String? = null, …`) fills the
  content slot with a `Column(spacedBy(2.dp))` — title in `Text.primary`,
  optional subtitle in `Text.secondary`, both `maxLines = 1` with
  `TextOverflow.Ellipsis`. The 2dp gap is deliberately hardcoded (tighter than
  `spacing.xs` = 4dp) so the pair reads as one text block.
- Param order on both overloads: `isSelected, showsChevron, leading, trailing,
  modifier, content` (string overload puts `title, subtitle` first, no
  trailing `content`).

## AMListCard (`ListCard.kt`)

Floating card row: wraps `AMCard` (which provides the two-layer shadow chrome
and demotes nested cards via `LocalAMSurfaceStyle`) with a standardised inner
`Column(Modifier.fillMaxWidth(), spacedBy(tokens.spacing.sm), Alignment.Start)`.
The `content` slot is `@Composable ColumnScope.() -> Unit`, so callers can use
column-scoped modifiers per row. `padding: PaddingValues? = null` resolves to
`tokens.blockInsets` (all 16dp) when null; pass explicit `PaddingValues` to
override per call site.

## The two reorderable containers — shared wiring

Both wrap a transparent `LazyColumn` with the `sh.calvin.reorderable` 2.x API
(dependency facts in `build.md`). The exact incantation, identical in both
files:

```kotlin
val lazyListState = rememberLazyListState()
val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
    onReorder(amMoveItem(items, from.index, to.index))
}
LazyColumn(state = lazyListState, modifier = modifier) {
    items(items, key = { key(it) }) { item ->
        ReorderableItem(reorderableState, key = key(item)) { isDragging -> … }
    }
}
```

- **Stateless / caller-owned state.** `items: List<T>` is a snapshot, never
  mutated. The engine's `onMove` lambda fires on **every drag-over event**
  (live move-as-you-drag, not just on drop); each event calls `onReorder` with
  the fully reordered list. The caller MUST write the result back to their
  `MutableState` — if they don't, items snap back and the drag misbehaves.
- **`key` must be stable and unique** across recompositions; it is both the
  `LazyColumn` item key and the `ReorderableItem` key. Stable keys let Compose
  distinguish structural changes (add/remove/reorder, animated) from
  field-only mutations (cell recomposes in place).
- **Drag surfaces are Modifiers from the `ReorderableItem` receiver scope:**
  `Modifier.draggableHandle(onDragStarted, onDragStopped)` (handle-only drag)
  and `Modifier.longPressDraggableHandle(…)` (long-press anywhere). They only
  exist inside that scope.
- **Haptics, both lists:** `onDragStarted` fires
  `HapticFeedbackType.LongPress`, `onDragStopped` fires
  `HapticFeedbackType.GestureEnd`, via `LocalHapticFeedback.current`.

## AMReorderableRowList (`ReorderableRowList.kt`)

Pairs with `AMListRow`. Params: `items, key, onReorder, isEditing = false,
rowPadding = PaddingValues(0.dp), modifier, content: @Composable (T, isDragging: Boolean) -> Unit`.
`rowPadding` defaults to zero so full-bleed rows touch the list edges; the
content slot receives `isDragging` so callers can add their own lift feedback
(this list applies none itself).

**Edit-mode duality** (`isEditing`):

- `false` (default): no visible handle; the whole row body is one `Box` with
  `Modifier.longPressDraggableHandle` — long-press anywhere starts the drag.
- `true`: a `Row` of `[content in Box(weight(1f).padding(rowPadding))]` +
  trailing drawn drag handle. Only the handle is the drag surface
  (`Modifier.draggableHandle`). The handle is a 44dp `Box` (touch target) with
  `semantics { contentDescription = "Reorder" }`, containing a 24dp `Canvas`
  that draws **three** 14dp × 2dp round-rect horizontal bars with 4dp gaps (the
  iOS three-line reorder grip), tinted `AMTheme.Icon.muted` —
  `material-icons-core` lacks a DragHandle glyph (drawn-glyph precedent:
  `AMStepper`'s minus bar).

**Pairing rule:** do NOT put `AMListCard` in this list — card content (padded,
rounded, elevated) looks wrong beside the trailing drag handle. Use
`AMReorderableCardList` for cards.

## AMReorderableCardList (`ReorderableCardList.kt`)

Pairs with `AMListCard`. Params: `items, key, onReorder, modifier,
content: @Composable (T) -> Unit` — no `isEditing`, no `isDragging` exposed to
the slot, no visible handle ever: the entire cell is a
`longPressDraggableHandle` surface (the iOS card list's no-edit-chrome stance).

Each cell `Box` carries, in order:

1. `Modifier.padding(horizontal = 16.dp, vertical = 4.dp)` — cell margins
   (`spacing.lg` / `spacing.xs`), the inter-card gutter.
2. `graphicsLayer { shadowElevation = elevation.toPx(); shape = liftShape; clip = false }`
   — drag lift. `elevation` is `animateDpAsState(if (isDragging) 8.dp else 0.dp)`,
   its `spring<Dp>()` routed through `tokens.motion.resolved(…,
   LocalAMReduceMotion.current)` so the lift collapses to the overlay-fade tween
   under Reduce Motion (the same house policy `AMSwipeRow`'s settle uses).
   `liftShape = amSquircle(tokens.radius.lg)` so the lift shadow contour matches
   the card's squircle corners. This lift is separate from the two-layer shadow
   `AMCard` itself draws.
3. `longPressDraggableHandle(…)` with the standard haptics.

## AMSwipeRow (`SwipeActions.kt`)

Swipe-to-reveal wrapper: drag `content` toward the layout end edge to expose the
trailing action panes. Not a reorderable container — a single-row decorator that
typically wraps an `AMListRow`.

- **API.** `AMSwipeRow(trailingActions: List<AMSwipeAction>, isOpen: Boolean? =
  null, onOpenChange: ((Boolean) -> Unit)? = null, modifier, content:
  @Composable () -> Unit)`. **All** actions are honored — the list is uncapped;
  **≤3 is soft guidance** (each pane is a fixed 74dp, so >3 slides the open row
  nearly off a phone's width — no hard cap). An **empty list disables the
  gesture** (renders `content` unchanged, no drag).
- **AMSwipeAction + role.** `@Immutable class AMSwipeAction(icon: ImageVector,
  label: String, role: AMSwipeActionRole = Normal, tint: Color? = null,
  contentDescription: String, onClick: () -> Unit)`; `AMSwipeActionRole
  { Normal, Destructive }` (`@Immutable`). `contentDescription` sits after the
  optional `role`/`tint` (required metadata before the `onClick` lambda), so
  callers pass named args. Pane fill resolves via private
  `amSwipePaneFill(action, isDark)`: `tint ?: when(role) { Destructive ->
  AMTheme.Semantic.destructive; Normal -> AMSwipeNormalFill.resolve(isDark) }`,
  where `AMSwipeNormalFill` is a file-level private **adaptive**
  `AMAdaptiveColor(light = 0xFFAEAEB2, dark = 0xFF636366)` (systemGray2, both
  modes).
- **Open-state duality** (mirrors `AMReorderableRowList`'s `isEditing`): `isOpen
  = null` self-manages a `rememberSaveable` `internalOpen`; a non-null `isOpen`
  makes the row controlled. `setOpen(next)` writes `internalOpen` only when
  uncontrolled and always calls `onOpenChange` — so a parent stays in sync and
  can force-close. Two `LaunchedEffect`s reconcile the engine with the boolean:
  **external→engine** `LaunchedEffect(open, openOffset)` runs
  `state.animateTo(target, settleSpec)` when `state.currentValue` differs (this
  drives every programmatic/tap close animation); **engine→external**
  `LaunchedEffect(state) { snapshotFlow { state.settledValue }.collect { … } }`
  reports a drag that settles at a new anchor back through `setOpen`. Stale
  capture is avoided with `rememberUpdatedState`, and the equality guard
  (`nowOpen != currentOpen`) makes redundant emissions no-ops so the two
  directions can't loop.
- **Engine: Foundation `anchoredDraggable`** (stable in foundation 1.11.2 — no
  `@ExperimentalFoundationApi` opt-in). Two anchors via `DraggableAnchors`:
  `Closed at 0f`, `Open at amSwipeOpenOffset(count, paneWidthPx)`. State built
  with `AnchoredDraggableState(initialValue = if (open) Open else Closed,
  anchors = …)` (constructor form initializes the offset before layout, so
  `state.requireOffset()` in the content's `Modifier.offset { }` never throws),
  `remember`ed keyed on the open offset so a density change rebuilds it. Settle
  uses `AnchoredDraggableDefaults.flingBehavior(state, positionalThreshold =
  AMSwipePositionalThreshold, animationSpec = settleSpec)` — half the open width
  (hoisted `{ d -> d / 2f }` const for stable identity) plus the built-in
  **125 dp/s** velocity threshold. **Anim specs live on the fling behavior /
  `animateTo`, NOT on the state** (the pre-1.9 state-constructor spec params are
  deprecated).
- **Motion (house rule).** Every animated settle — the fling behavior and both
  `animateTo` reconciles — routes through `settleSpec = remember { tokens.motion
  .resolved(tokens.motion.snapSpring, LocalAMReduceMotion.current) }`: the house
  snap spring, softening to the overlay-fade tween under Reduce Motion.
- **Rubber-band overdrag.** `anchoredDraggable` hard-clamps travel to
  `[openOffset, 0]` (`AnchoredDraggableState.newOffsetForDelta` coerces to the
  anchor range), so elastic overdrag comes from a private
  `AMSwipeOverscrollEffect : OverscrollEffect` (non-experimental in 1.11.2). It's
  handed to `anchoredDraggable(overscrollEffect = …)` and rendered via
  `Modifier.overscroll(effect)` on the content Box; its `node` is a
  `LayoutModifierNode` that shifts the content by the accumulated overdrag. Excess
  drag past an anchor carries `AMSwipeOverdragResistance = 0.3f` into an
  `Animatable`, released with a pre-scroll relaxation on reversal (ported from the
  AndroidX `OverscrollSample`, horizontal axis) and sprung back to 0 on
  `applyToFling` with `settleSpec` (a `var` on the effect, updated each
  recomposition). **Reduce Motion drops the effect** (`overscrollEffect = null`)
  → plain hard clamp. iOS's `rubberBanded` (resistance 0.3) is the twin.
- **Tap-open-row-to-close.** While `open`, a transparent `matchParentSize()`
  catcher inside the (offset) content Box swallows its own taps and calls
  `setOpen(false)` (the external→engine effect then animates it closed).
  Indication-free (tap-to-dismiss exception) and `clearAndSetSemantics {}` so it
  adds no phantom activate action to the row.
- **Accessibility.** The content Box carries `semantics(mergeDescendants = true)
  { customActions = panes.map { CustomAccessibilityAction(it.label) {
  it.onClick(); setOpen(false); true } } }` — TalkBack can't perform the swipe,
  so each action is surfaced through the row's actions menu, and (like the
  pane-tap path, and iOS's `trigger`) the custom action **closes the row** after
  firing. The behind-tray `Row` is
  `clearAndSetSemantics {}`'d while closed (stable API, chosen over the
  experimental `hideFromAccessibility`/`invisibleToUser` to keep the
  no-experimental stance) so a resting row never announces phantom action
  buttons; the panes become accessible again when open.
- **Layout.** Root `Box`: behind sits a `Row(Modifier.matchParentSize(),
  Arrangement.End)` of **74dp-wide** (`AMSwipePaneWidth`) `fillMaxHeight()` panes
  — each fills `amSwipePaneFill(action, isDark)` and centers a 20dp icon over a
  `Type.labelSmall` label (`maxLines = 1`, ellipsis) in `Color.White`; each pane
  is `clickable(indication = amRipple())` with a per-pane interaction source
  (`remember(count) { List(count) { MutableInteractionSource() } }` — never
  remembered in a variable-trip-count loop), firing `onClick()` then
  `setOpen(false)`. On top, the content `Box` carries
  `Modifier.offset { IntOffset(requireOffset().roundToInt(), 0) }` then
  `.overscroll(effect)` then `.anchoredDraggable(…)` then the customActions
  `semantics`. `matchParentSize`
  makes the panes match the content's height; `content` **must paint an opaque
  background** or the panes bleed through at rest (KDoc says so — typically an
  `AMListRow`, which fills `Surface.primary`).
- **RTL.** "Trailing" = layout end; the no-`reverseDirection` `anchoredDraggable`
  overload resolves drag direction against the ambient layout direction, so RTL
  mirrors the reveal. LTR is the primary-tested direction (documented in KDoc).
- **Pure math:** `internal fun amSwipeOpenOffset(count: Int, paneWidthPx: Float):
  Float` coerces `count` to **at least 1** (`coerceAtLeast(1)`, uncapped above)
  and returns `-(coerced × paneWidthPx)` (always `<= 0`). Unit-tested in
  `SwipeActionsTest.kt` (counts 1/2/3, coerce 0/negative→1, uncapped 5→−400 /
  7→−560, zero pane width, non-positive invariant).

## ReorderSupport.kt — `amMoveItem`

`internal fun <T> amMoveItem(items: List<T>, fromIndex: Int, toIndex: Int): List<T>`
is the single shared write-back primitive both lists call from their `onMove`
lambda. Semantics (all unit-tested in
`mtrls/src/test/kotlin/io/github/jmsdnns/androidmtrls/lists/ReorderSupportTest.kt`):

- Pure: never mutates `items`; a real move returns a **new** list
  (`toMutableList` + `removeAt` + `add`).
- Empty list → returns `items` itself. Both indices are clamped via
  `coerceIn(0, items.lastIndex)` (defensive against engine edge indices —
  never throws); if the clamped indices are equal, returns `items` itself
  (same instance, copy skipped — asserted by test).

## See also

- `conventions.md` — visual-only rule, slot + String-overload pattern, param
  order, preview/KDoc policy.
- `build.md` — `sh.calvin.reorderable` 2.5.1 dependency facts, test policy.
- `collections.md` — grids/carousels (the non-list collection containers).
