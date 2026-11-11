---
scope: List rows and cards, labeled content, the reorderable lists and their engine, and swipeable rows — the library's lists/ folder and the Lists catalog pages
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/lists/**, androidmtrls/src/main/res/drawable/am_ic_drag_handle.xml, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/ListsReorderMathTest.kt, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/ListsLabeledContentWidthTest.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/lists/**]
keywords: [AMListRow, ListItem, subtitle, supportingContent, leadingContent, trailingContent, selected, selection ring, onClick, amFlatSurface, AMListCard, amCard, AMLabeledContent, LabeledContentLayout, labeledContentLabelWidth, label keeps its width, value wraps, end-aligned, first baseline, bodyLarge, onSurfaceVariant, AMReorderableRowList, AMReorderableCardList, ReorderableColumn, ReorderablePageColumn, ReorderState, ReorderLayout, LazyReorderLayout, PageReorderLayout, ReorderItem, ReorderCell, BoxWithConstraints, hasBoundedHeight, scrolling page, verticalScroll, NestedScrollDispatcher, dispatchPreScroll, dispatchPostScroll, drift, slide, LazyColumn, animateItem, onMove, key, drag handle, drag_handle, ReorderHandle, long press, hold, touch slop, haptics, SegmentTick, auto-scroll, requestScrollToItem, reorderStep, clampLiftedTop, reorderAutoScrollVelocity, settle, lift, LocalAMReorderLifted, LocalAMReorderHandleInset, Move up, Move down, TalkBack, CustomAccessibilityAction, RowActions, RowActionsContainer, claimRowActions, LocalAMRowActions, AMSwipeableRow, AMSwipeAction, neutral tint, surfaceContainerHighest, removesRow, SwipeBox, SwipeToDismissBox, full swipe, positional threshold, swipeRevealSpan, band, GestureThresholdActivate, ListsPages, rememberListsItems]
updated: 2026-11-10
---

# Lists

Rows and the lists made of them, all on Material's `ListItem` at the row recipe's rhythm (`Layout.md`): `AMListRow` (a flat row), `AMListCard` (a list item that is a card), `AMLabeledContent` (a label and its value), `AMSwipeableRow` with `AMSwipeAction` (one full-swipe action per direction, on `SwipeToDismissBox`), and the two reorderable lists, `AMReorderableRowList` and `AMReorderableCardList`, one engine with drag handles, a press-and-hold lift, auto-scroll and TalkBack's "Move up" and "Move down", over a `LazyColumn` that scrolls itself or (the card list in a scrolling page) a plain column that scrolls with the page.

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/lists/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `ListRow.kt` | `AMListRow` (string and slot forms) |
| `ListCard.kt` | `AMListCard` (string and slot forms) |
| `LabeledContent.kt` | `AMLabeledContent` (string and slot forms); private `LabeledContentLayout`; internal `labeledContentLabelWidth` |
| `SwipeableRow.kt` | `AMSwipeAction`, `AMSwipeableRow`; internal `SwipeBox`, `swipeAccessibilityActions`, `swipeRevealSpan`; private `SwipeBand` |
| `ReorderableLists.kt` | `AMReorderableRowList`, `AMReorderableCardList` |
| `ReorderEngine.kt` | internal `ReorderableColumn` (lazy), `ReorderablePageColumn` (in a page), `ReorderCell`, `ReorderItem`, `ReorderLayout`, `ReorderState`, `ReorderHandle`, `ReorderHandleSize`, `LocalAMReorderLifted`, `LocalAMReorderHandleInset`, `reorderAccessibilityActions`, and the pure math `ReorderSpan`, `reorderStep`, `clampLiftedTop`, `reorderAutoScrollVelocity`; private `rememberReorderState`, `reorderPlacement`, `LazyReorderLayout`, `PageReorderLayout`, `reorderGestures`, `awaitSlop`, `awaitLetGo` |
| `RowActions.kt` | internal `RowActions`, `LocalAMRowActions`, `RowActionsContainer`, `claimRowActions` |

- Glyph: `androidmtrls/src/main/res/drawable/am_ic_drag_handle.xml` (Material's `drag_handle`).
- Tests: `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/ListsReorderMathTest.kt`, `ListsLabeledContentWidthTest.kt`.
- Pieces from elsewhere: `AMRowDefaults` and internal `Modifier.amFlatSurface()` (`layout/Card.kt`), `Modifier.amCard`, internal `Modifier.amElevatedSurface` (`foundations/Hairline.kt`), `Modifier.amHairline`, `AMSemantic`, `resolveEnabled`.

## Rows on `ListItem`

All three row kinds pass `AMRowDefaults.colors()`, `shapes()` and `contentPadding` (or the card's variant of it), so they're content plus 28 dp above and below and 16 dp at the sides: one line makes an 80 dp row, a title over a subtitle a 100 dp row. Text wraps and the row grows; Material's list item centers the leading and trailing content beside short content and top-aligns it beside tall content. Stack rows with `AMDivider(inset = spacing.lg)` between them. Each row claims its container's custom actions onto its own node (below) and provides `LocalAMRowActions` null to its slots.

### `AMListRow` — `ListRow.kt`

- String: `AMListRow(title, modifier, subtitle: String? = null, leadingContent?, trailingContent?, selected = false, onClick: (() -> Unit)? = null, enabled = true)`. Slot: `AMListRow(modifier, leadingContent?, supportingContent?, trailingContent?, selected, onClick?, enabled, content)`, the headline last. The string form delegates; a null or empty subtitle shows no supporting line.
- **Surface**: `fillMaxWidth().amFlatSurface()`: `AMTheme.Surface.primary` edge to edge when it stands on the page, nothing inside a card, so the card's rounded surface shows.
- **Taps**: no `onClick` → Material's non-interactive `ListItem` (it only draws); with one → the clickable `ListItem`, ripple and button semantics. Long-press actions come from `Modifier.amHoldable` on the row; swipe actions from wrapping it in `AMSwipeableRow`. There is no drill-down chevron: Android rows signal a tap by their ripple, and a caller who wants a trailing glyph passes it in `trailingContent`.
- **`selected`** draws the selection ring, `amHairline(RectangleShape, width = AMTheme.selectionRingWidth)` in `selectionRing`, inside the row's edges, and sets `selected` semantics. The row never tints.
- `enabled` goes through `resolveEnabled` to the list item, which dims a disabled row and stops its taps.

### `AMListCard` — `ListCard.kt`

- String: `AMListCard(title, modifier, subtitle?, leadingContent?, trailingContent?, fill = Solid, onClick?, enabled)`. Slot: `AMListCard(modifier, leadingContent?, supportingContent?, trailingContent?, fill, onClick?, enabled, content)`. `supportingContent` takes a row of tags or a card's actions as well as text.
- The `ListItem` wears `fillMaxWidth().amCard(fill, elevation)`: `shapes.large`, the `Low` shadow, the dark hairline, its ripple clipped to the corners. Content padding is the row recipe's, plus `LocalAMReorderHandleInset` at the end.
- **In a reorderable card list** it reads `LocalAMReorderHandleInset` (the handle's 24 dp plus `spacing.md`), so its text stops short of the list's handle, and `LocalAMReorderLifted`, raising its card to `AMElevation.Medium` while it's dragged or settling. It resets both for its own slots.
- Stack list cards `spacing.md` apart.

### `AMLabeledContent` — `LabeledContent.kt`

- String: `AMLabeledContent(label, value: String, modifier)`. Slot: `AMLabeledContent(label, modifier, value: @Composable () -> Unit)` for a value that isn't text (a status tag).
- A non-interactive `ListItem` whose headline slot holds both, laid out by private `LabeledContentLayout` (a two-content `Layout`), so the list item reads label and value as one node: the label (`bodyLarge`, `onSurface`) at the start, then `spacing.lg`, then the value under `ProvideTextStyle(bodyLarge, TextAlign.End)` and `LocalContentColor = onSurfaceVariant`, so it reads as a value, not as Material's small trailing note.
- **The label keeps its width; the value takes the rest.** The label is laid out at `labeledContentLabelWidth` (unit-tested): its one-line width (max intrinsic), given up only as far as the value's longest word (its min intrinsic) needs, never below the label's own longest word, and never wider than the room. The value gets the rest of the row, is placed at the end, and wraps onto end-aligned lines ("214 Van Brunt St, Brooklyn, NY 11231" beside "Address"). Nothing stacks at large text.
- **Vertical alignment**: the two first baselines line up when both have one (text, a tag); otherwise they center on each other.
- `fillMaxWidth().amFlatSurface()`, like `AMListRow`. It takes no taps and has no `enabled`.

## Swipe actions — `SwipeableRow.kt`

```kotlin
AMSwipeableRow(
    startAction = AMSwipeAction("Archive", archiveIcon, removesRow = true) { archive(message) },
    endAction = AMSwipeAction("Delete", deleteIcon, AMSemantic.Destructive, removesRow = true) { delete(message) },
) { AMListRow(message.sender, subtitle = message.subject) }
```

- **`AMSwipeAction(label, icon: Painter, tint: AMSemantic? = Info, removesRow = false, onClick)`**, `@Immutable`. `label` is TalkBack's custom action; `icon` shows on the band; `tint` colors it (the band in `tint.color`, the glyph in `tint.onColor`; `Destructive` is the scheme's `error`). `tint = null` is the neutral action, neither a status nor a removal (mute, snooze): the band in `surfaceContainerHighest`, the glyph in `onSurface`, the neutral tag's pair, gray in both modes. `removesRow` says the action takes the row away.
- **`AMSwipeableRow(modifier, startAction?, endAction?, enabled, content)`**: one action per direction, the Android way. `startAction` sits at the row's start and a swipe toward the end reveals it (rightward in left to right); `endAction` the reverse. A null side doesn't swipe; with neither, gestures are off. More actions belong in a ⋮ menu or a press and hold.
- Built on internal `SwipeBox`, which the reorderable row list shares: Material's `SwipeToDismissBox` on `rememberSwipeToDismissBoxState(positionalThreshold = half the row)`, so a release past half the row or a fling acts and anything shorter springs back. `enableDismissFromStartToEnd` / `EndToStart` follow which actions exist; `gesturesEnabled` is `enabled` and at least one action.
- **After the swipe**: `onDismiss` (after the slide-away settles) runs the action's `onClick`; an action that `removesRow` leaves the row swiped off, showing its band, until the caller drops the item; any other `reset()`s the row back.
- **The band** (`SwipeBand`): drawn only while the row is off center, for the action on that side, in its tint's colors or the neutral pair, filling the row but clipped to the strip the content has uncovered (`swipeRevealSpan(offset, width, rtl)`: the content's physical shift is the offset in left to right and its negation in right to left). So a row lying transparent on a card shows nothing behind it at rest, and the card's clip rounds the band at its corners. The glyph sits `spacing.lg` in from the edge the swipe started from. No label: the glyph carries it.
- **Haptic**: `GestureThresholdActivate` each time the target value leaves `Settled` (the release would act).
- **TalkBack**: `swipeAccessibilityActions` turns each action into a `CustomAccessibilityAction` running its `onClick`, offered through `RowActionsContainer`. None while disabled.

## The reorderable lists — `ReorderableLists.kt`, `ReorderEngine.kt`

```kotlin
val queue = rememberSaveable(saver = …) { mutableStateListOf(*episodes) }
AMReorderableRowList(queue, key = { it.id }, onMove = { from, to -> queue.add(to, queue.removeAt(from)) }) { episode ->
    AMListRow(episode.title, subtitle = episode.show)
}
```

- **`AMReorderableRowList(items, key, onMove, modifier, state = rememberLazyListState(), startAction: (T) -> AMSwipeAction? = { null }, endAction = { null }, enabled, content: @Composable (T) -> Unit)`**. Each row: a `RowActionsContainer` (move actions plus the swipe actions) around a `SwipeBox` around a `Row(fillMaxWidth().amFlatSurface())` of the content (`weight(1f)`) and the handle, `spacing.lg` from the end, so the handle travels with a swipe. Rows lie flat on whatever holds the list; a lifted row comes up as a floating surface: `amElevatedSurface(shapes.large, AMElevation.Medium).clip(shapes.large)`, solid, with the dark hairline. Its swipe is off while lifted. Pair it with `AMListRow`.
- **`AMReorderableCardList(items, key, onMove, modifier, state, enabled, content)`**. Each cell carries `spacing.lg` side and `spacing.xs` vertical margins (the page gutter, so the list runs edge to edge and its cards land on the gutters; in the catalog it sits on a `Wide` stage). The handle overlays the card's trailing edge, `spacing.lg` in; the cell provides `LocalAMReorderHandleInset` and `LocalAMReorderLifted` for `AMListCard`. Custom card content keeps its trailing edge free. It's a `BoxWithConstraints(modifier, propagateMinConstraints = true)` that picks its form from the height it's measured with:
  - **Bounded** (a screen, a weight, `heightIn(max = …)`): the lazy `ReorderableColumn` on `state`, scrolling itself, with `spacing.md` content padding above and below for the first and last cards' shadows.
  - **Unbounded** (inside `Modifier.verticalScroll`, or a lazy list's item): `ReorderablePageColumn`, a plain column as tall as its cards (no extra padding: nothing clips their shadows) that scrolls with the page. `state` isn't used. Every card is composed, so it's for lists of a page's length.
- **State is the caller's.** `onMove(from, to)` asks for one step at a time, as the drag passes each neighbor, so the caller's list always matches the screen; `key` must be stable and `Bundle`-saveable. `enabled` goes through `resolveEnabled`; a disabled list takes no drags, offers no actions and dims its handles to 0.38.
- **Height.** The row list is a `LazyColumn`, so it needs a bounded height: a screen's, or `heightIn(max = …)` inside a scrolling page, where it takes its content's height up to the cap and scrolls beyond it. The card list takes either (above).

### The engine — `ReorderState` over a `ReorderLayout`

- **Two layouts, one state machine.** `ReorderState` reads the items through internal `ReorderLayout`: `item(key)`, `itemAt(index)`, `itemUnder(y)` (each a `ReorderItem(key, index, offset, size)` in the list's frame), `window()` (the span a lifted item stays inside), `canScroll(velocity)`, `scrollBy(delta)`, `drift()`, `pointerSynced()` and `beforeMove(from, to)`. `rememberReorderState` writes its settings (`onMove`, `enabled`, `rtl`, haptics, the settle spec `motionScheme.defaultSpatialSpec()`, the zones in pixels) in a `SideEffect`.
  - **`LazyReorderLayout`** (the `LazyColumn`): positions from `layoutInfo.visibleItemsInfo`, in the frame from the start of the content area (a touch converts by `viewportStartOffset`); the window is the viewport; `scrollBy` scrolls the list (the items move, the frame doesn't); `beforeMove` pins the scroll by index (`requestScrollToItem`) when the first visible item takes part.
  - **`PageReorderLayout`** (the page column): positions from the column's own measure, in the column's frame, behind a version counter (state) so a layer that reads them follows the layout; the window is what's visible of the column, its bounds inside every clipping parent (`findRootCoordinates().localBoundingBoxOf(column, clipBounds = true)`, the page's viewport among them); `canScroll` only toward an edge where more of the list is hidden; `scrollBy` goes to the page through a `NestedScrollDispatcher` (`dispatchPreScroll` then `dispatchPostScroll`, `NestedScrollSource.UserInput`, so a collapsing top app bar takes its share first) and returns what was consumed, which is how far the whole column moved under the still finger; `drift()` measures the column's real move in the root against that expectation each frame and returns the difference, so the lifted card stays under the finger whatever took part in the scroll.
- `ReorderableColumn(items, key, onMove, modifier, listState, enabled, contentPadding, handleZone, cell)` is the `LazyColumn` (`itemsIndexed` keyed by `key`) both lists build on; `ReorderablePageColumn(items, key, onMove, modifier, enabled, handleZone, cell)` is the card list's page form, a `Layout` stacking the items (each in `key(itemKey)`) with `nestedScroll(…, dispatcher)`. `cell(item, lifted, moveActions)` (`ReorderCell`) draws an item.
- **Placement per item** (`reorderPlacement`): the lifted item `zIndex(2f)` with `graphicsLayer { translationY = liftedOffset() }`; the settling item `zIndex(1f)` with the settle `Animatable`'s value. Every other item: in the lazy list, `animateItem` (placement `defaultSpatialSpec`, fade in and out `defaultEffectsSpec`), so neighbors slide out of the way and removed items fade as the rest close up; in the page column, a slide of its own (`PageReorderLayout.place`: an item whose slot moved since the last measure starts from where it was, an `Animatable` to 0 on `defaultSpatialSpec`, kept as state read in placement), so neighbors slide aside; items that come and go just appear and leave as the rest slide.
- **Gestures** (`reorderGestures`, a `pointerInput` on the list itself, so positions stay in the list's frame while items move and the list scrolls): each touch is watched in the **Initial** pass, ahead of the items' clicks and swipes and the list's scroll, and nothing is taken until a drag starts. A down in the trailing `handleZone` (row list: `lg + 24 dp + lg`; card list: `lg + lg + 24 dp + lg`, the end mirrored in right to left) lifts once the touch passes the touch slop (`GestureThresholdActivate` haptic); anywhere else it lifts once held still for the long-press timeout (`LongPress` haptic), and moving past the slop first leaves the touch to scrolling and swiping. From the lift on, every change is consumed until the finger lifts, and `drop()` runs in a `finally`.
- **Following the finger**: the item's top is its slot's offset at the lift plus the finger's travel, kept inside the list's window (`clampLiftedTop`), minus its current slot's offset: a move or a scroll changes the slot, never the item's place under the finger. In the page column a scroll moves the whole frame, so the finger's place in it grows by the scroll consumed (and any `drift()`), set back to the true place on each touch event.
- **Trading places** (`moveIfPassed`, after each drag event and each auto-scroll step): `reorderStep` returns +1 once the lifted item's bottom passes the middle of the item below, −1 once its top passes the middle of the item above. Each neighbor's own middle keeps a short and a tall item from trading back and forth. Nothing moves while the last move hasn't reached the layout (`expectedIndex`). When the first visible item takes part, `requestScrollToItem(first, offset)` pins the scroll by index so the list doesn't jump. Each step calls `onMove` and ticks `SegmentTick`.
- **Auto-scroll**: while lifted, a frame loop (`withFrameNanos`) scrolls by `reorderAutoScrollVelocity` × frame time: only toward the edge the item has traveled to, once it's within the edge zone (64 dp, at most a quarter of the window), faster with depth up to 800 dp/s. The lazy list scrolls itself; the page column scrolls the page, and only while the list runs on past that edge of the visible window.
- **Drop**: the lifted item becomes the settling one, its `Animatable` snapped (undispatched, in the same frame) to where it was and animated to its slot; `GestureEnd` haptic. Lifting the settling item again picks it up where it is.
- **TalkBack**: `reorderAccessibilityActions` gives each item "Move up" (not the first) and "Move down" (not the last), calling `onMove(index, index ∓ 1)`. The handle (`ReorderHandle`, `am_ic_drag_handle` in `onSurfaceVariant`, no description) is skipped.

## Row actions for TalkBack — `RowActions.kt`

A container's custom actions (the swipe actions, the move actions) belong on the row's own focusable node, the one reading its text, so the row stays one stop with its actions one step away. The container can't reach that node, so it hands them down:

- `RowActionsContainer(actions, modifier, content)` provides `LocalAMRowActions` = `RowActions(actions, claims)`. Nested in another container it takes the outer actions first and claims them. While no row has claimed its list, it carries the actions itself, `semantics(mergeDescendants = true)` around its content.
- `claimRowActions()`, called by `AMListRow`, `AMListCard` and `AMLabeledContent`, counts a claim (a `DisposableEffect`) and returns `Modifier.semantics { customActions = … }` for the list item, joining its merged node.

## Catalog pages

`demo/.../catalog/pages/lists/`, registered in `ListsPages.entries` in this order (ids): `listRow`, `listCard`, `reorderableRowList`, `reorderableCardList`, `swipeableRow`, `labeledContent`.

| Page | Preview | Loaded |
|---|---|---|
| List row (`ListRowPage.kt`) | `FullBleed`: Notifications and Storage rows (status-colored glyphs, subtitles, tappable) on a surface strip | an account card (Ada Lovelace, Storage with an "82% full" tag, About, all tappable) over a card of three plans, the chosen one ringed and changed by a tap |
| List card (`ListCardPage.kt`) | "Write the release notes" over "Due Friday · Docs" | three task cards `spacing.md` apart: one with a card action row (mark done, a "Mon" tag, ⋮), one with three tags, one with a wrapping note |
| Reorderable row list (`ReorderableRowListPage.kt`) | `FullBleed`: three episodes on a surface strip | four episodes in a card with a waveform glyph; swipe right to favorite (settles back), left to remove |
| Reorderable card list (`ReorderableCardListPage.kt`) | `Wide`: three task cards with due-day tags, as tall as its cards and scrolling with the page | `Wide`: four timed task cards with clock tags, scrolling with the page |
| Swipeable row (`SwipeableRowPage.kt`) | `FullBleed`: "Weekly digest", swipe right to pin, left to mark as read (both settle back) | an inbox card: a comment (archive or delete), a build (flag, settles back), a storage warning (delete); archived and deleted rows leave the card |
| Labeled content (`LabeledContentPage.kt`) | `FullBleed`: Plan and Renews on a surface strip | a subscription card: Plan, Status (an "Active" tag), Renews, Seats |

- `ListsSupport.kt`: `rememberListsItems` (a `SnapshotStateList` saved as its items' ids, so a reordered or trimmed demo list survives leaving the page).
- The pages' sample classes carry a `Lists` prefix (`ListsPlan`, `ListsEpisode`, `ListsTask`, `ListsMessage`), since the demo is one package and two private classes of one name collide.
- Demo glyphs these pages add: `ic_table_rows`, `ic_view_day`, `ic_reorder`, `ic_stacks`, `ic_swipe_left`, `ic_format_align_left` (entry icons), and `ic_hard_drive`, `ic_radio_button_unchecked`, `ic_graphic_eq`, `ic_schedule`, `ic_flag`, `ic_verified`, `ic_mark_email_read`.

## Tests

`ListsLabeledContentWidthTest` (JUnit): `labeledContentLabelWidth` (the label at its own width beside a long value, a long label giving up only what the value's longest word needs, never below its own longest word, never wider than the room).

`ListsReorderMathTest` (JUnit): `reorderStep` (passing a neighbor's middle each way, a tall and a short item not trading back, the list's ends), `clampLiftedTop` (inside the window, under content padding, an item taller than the window), `reorderAutoScrollVelocity` (only toward the traveled edge, speed by depth, the cap, no edge zone), and `swipeRevealSpan` (both directions in left to right and right to left, rest and overshoot).
