---
scope: Sequence-driven primitives that own the gesture and animation of moving through items — the carousel on Android's pager and the swipeable card stack — plus the shared collection contract, the shared index clamp and the deck's release, window and identity rules
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/collections/**, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/internal/IndexClamp.kt, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/collections/**, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/collections/**, demo/src/main/res/drawable/ic_auto_awesome_motion.xml, demo/src/main/res/drawable/ic_view_carousel.xml]
keywords: [AMCarousel, pager, HorizontalPager, PagerState, rememberPagerState, currentPage, contentPadding, pageSpacing, PagerDefaults.flingBehavior, PagerSnapDistance, animateScrollToPage, no imposed surface, scrollableArea, clip, shadow, SnapPosition, stateDescription, Page x of y, sideInset, spacing, currentIndex, onCurrentIndexChange, CarouselIndexSync, AMPageIndicator, AMCardStack, AMCardStackSwipeDirection, StartToEnd, EndToStart, deck, peek, event mode, bound mode, onSwipe, wraps, visibleDepth, key, CardStackCommit, commit rule, splineBasedDecay, CardStackWindow, CardStackStep, CardStackPass, CardStackCard, laps, CardStackBook, CardStackRead, departures, fly off, fade, entry pose, tilt, graphicsLayer, zIndex, awaitHorizontalTouchSlopOrCancellation, VelocityTracker, custom actions, TalkBack, clampIndex, index clamp, collection contract, CollectionsPages, CardStackPage, CarouselPage]
updated: 2026-11-10
---

# Collections

Collections covers moving through a sequence of items: `AMCarousel`, a paging row with its neighbors peeking in on Jetpack's `HorizontalPager`, and `AMCardStack`, the swipeable deck, built in-house. Both own their gesture and motion, the exception to the visual-only rule (`Conventions.md`). They share an index contract, the internal index clamp, and Material's motion scheme.

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `collections/Carousel.kt` | `AMCarousel`; private `CarouselIndexSync`, `CarouselSeek` |
| `collections/CardStack.kt` | `AMCardStackSwipeDirection`, `AMCardStack` (event and bound forms); private `CardDeck`, `DeckCard`, `Deck`, `CardMotion`, `DrawnCard`, `DeckMode`, `CardRelease`, `CardReleases`, `CardAction`, `CardLeaving`, `LiveTop` and the per-fling constants |
| `collections/CardStackRules.kt` | internal, pure: `CardStackCommit`, `CardStackWindow`, `CardStackStep`, `CardStackCard`, `CardStackPass`, `CardStackRead`, `CardStackBook` |
| `internal/IndexClamp.kt` | internal `clampIndex(index, count)` |

- Tests: `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/collections/CollectionsIndexClampTest.kt` and `CollectionsCardStackRulesTest.kt` (below).
- Pieces from other areas: a page's or a card's surface is the caller's `Modifier.amCard()` (`Layout.md`); `AMPageIndicator` goes beside a carousel (`Navigation.md`).
- Catalog pages: `demo/.../catalog/pages/collections/` (below).

## The shared contract

- **Items and content.** Both take `items: List<T>` and a `content: @Composable (item: T) -> Unit` slot, last.
- **Identity.** The deck takes an optional `key: ((item: T) -> Any)?`; null uses the item itself, so items must be distinct. A card keeps its state (loaded pictures included) while its key stays drawn. The carousel keys its pages by position, the pager's default.
- **The index.** The carousel and the bound deck take the same hoisted pair, `currentIndex: Int` and `onCurrentIndexChange: (Int) -> Unit`, so a screen can swap one for the other on one index and hand it to `AMPageIndicator`. The event deck reports instead: `onSwipe(item, direction)`. An index out of range draws clamped through `clampIndex` (0 for an empty collection) and the clamp is never written back; both only ever report indices inside the collection.
- **Tunables** are `Dp` parameters whose defaults read `AMTheme.tokens.spacing`, so a theme re-tune moves them.
- **Motion** comes from `MaterialTheme.motionScheme` (the standard scheme); callers never pass specs. The system's Remove animations makes every release, settle and scroll instant, as Compose does; the drag still tracks the finger.
- **Surfaces.** Neither imposes one: a card is the caller's, surface included (`Modifier.amCard()`), so it casts its own shadow, and a page of bare photos is just as valid.
- **Controls under a carousel or deck** get no card: flat buttons (`AMElevation.None`) set a little apart, and the page dots on the page (`Patterns.md`).

## `AMCarousel` — `Carousel.kt`

```kotlin
var page by rememberSaveable { mutableIntStateOf(0) }
AMCarousel(stories, currentIndex = page, onCurrentIndexChange = { page = it }, Modifier.fillMaxWidth().height(186.dp)) { story -> StoryCard(story, Modifier.fillMaxSize().amCard()) }
AMPageIndicator(count = stories.size, currentIndex = page)
```

`AMCarousel(items, currentIndex, onCurrentIndexChange, modifier, sideInset = spacing.lg, spacing = spacing.md, content)`.

- **Jetpack's pager** (`androidx.compose.foundation.pager.HorizontalPager`), Android's own paging row. `contentPadding = PaddingValues(horizontal = sideInset)` with the default `PageSize.Fill`, so a page is the carousel's width less `sideInset` at each side; `pageSpacing = spacing`. Pages snap to the start of the padded area (`SnapPosition.Start`), so at rest a page stands `sideInset` in from the start and from the end, and the pages beside it peek in by `sideInset − spacing`. The modifier goes straight on the pager: a carousel runs edge to edge of whatever holds it; give it a height. A page's content is measured up to the page width and the carousel's height, so content that fills (`fillMaxSize`) is the whole page.
- **No imposed surface.** The carousel adds no fill, clip, hairline or surface style around a page: `content(item)` is the page as the caller draws it. A carousel of cards wears `Modifier.amCard()` itself (its own shadow, fill and dark-mode hairline); a full-bleed photo pager is `sideInset = 0.dp` with plain photos. The content color is whatever surrounds the carousel, as for any card.
- **Shadows fall free.** The pager clips only along its scroll axis (its `scrollableArea` clips at its start and end, and 30 dp past its top and bottom, Compose's room for an elevated item's shadow), and the carousel adds no clip of its own, so a card's two-layer shadow falls below it onto the page. To let the neighbors peek in from the screen's edges, run the carousel edge to edge and fold the page gutter into `sideInset`.
- **Paging**: `PagerDefaults.flingBehavior(state, pagerSnapDistance = PagerSnapDistance.atMost(1))`, one page per swipe or fling with the pager's own snap.
- **The index** (`CarouselIndexSync`): the state is `rememberPagerState(initialPage = clamped index) { items.size }` (saveable). One coroutine follows the caller's clamped index (`snapshotFlow` + `collectLatest`): when it differs from `state.currentPage` it scrolls there with `animateScrollToPage(…, animationSpec = motionScheme.defaultSpatialSpec())`, with `CarouselSeek.seeking` set so the pages it passes aren't reported. A swipe that interrupts that scroll cancels only the scroll (the coroutine stays active, so the cancellation is swallowed) and the carousel reports where it caught it. A second coroutine reports `state.currentPage` (the page nearest rest, which changes mid-swipe) whenever it differs from the caller's index and no seek is running.
- **TalkBack**: the pager's own scroll semantics (scroll and page actions, collection info) page it; the carousel adds `stateDescription` "Page x of y" from `state.currentPage`, clamped, and nothing for an empty carousel.

## `AMCardStack` — `CardStack.kt`

```kotlin
AMCardStack(suggestions, onSwipe = { song, direction -> … }, Modifier.fillMaxWidth().height(440.dp), key = { it.id }, startToEndActionLabel = "Save", endToStartActionLabel = "Skip") { SongCard(it) }
AMCardStack(albums, currentIndex = index, onCurrentIndexChange = { index = it }, Modifier.fillMaxWidth().height(280.dp), key = { it.id }, visibleDepth = 4) { AlbumCard(it) }
```

**`AMCardStackSwipeDirection`**: `StartToEnd`, `EndToStart` — the side the top card went toward, following the layout like Material's swipe-to-dismiss values (`StartToEnd` is a swipe to the right in left to right, to the left in right to left).

Two forms, one private `CardDeck`; the mode is data (`DeckMode.Event` / `DeckMode.Bound`), never a second composable:

- **Event form** `(items, onSwipe, modifier, key, visibleDepth = 3, startToEndActionLabel, endToStartActionLabel, content)`: the first item is the top card. A committed swipe either way flies the card off, then calls `onSwipe(item, direction)`; the caller removes the item to bring the next card up. A card the caller keeps comes back to rest.
- **Bound form** `(items, currentIndex, onCurrentIndexChange, modifier, key, visibleDepth = 3, wraps = false, content)`: the deck stays whole and the card at the clamped index is on top. **Toward the start is forward**: the top card flies off, then the deck reports the next index. **Toward the end is back**: the card springs home and the deck reports the previous index at once, while the previous card comes in from the start side, over it. A direction with nowhere to go springs home. `wraps` steps from the last card to the first and back; a single card never steps. The caller's own writes animate the same way (below).
- `visibleDepth` is clamped to at least 1. Every card fills the deck (`fillMaxSize` in a `Box`); give the deck a size.

### The look

Depth styling per step back `d` (animated as a float, so a card moving up rides the settle): scale `0.96^d`, a drop of `8 dp × d`, opacity `0.72^d`, `zIndex = −d`. The tilt follows the drag: `dx / (0.5 × width)` clamped to ±1, times 12°. A fling travels `1.6 × width` off the side, carrying `0.3 ×` the predicted vertical travel so the exit follows the finger's diagonal. These constants are deliberately not tokens: the deck's physicality is tuned as one piece. Everything is drawn in one `graphicsLayer` per card (rotation about the center, then translation), reading the card's own state while drawing, so a drag frame redraws that card alone and recomposes nothing. The entry and exit are each one pose added into that layer: tilted 12° toward a side and carried `1.6 × width` past it, scaled by a progress.

### The gesture

- On the top card only, as `pointerInput` placed **outside** the card's `graphicsLayer`, so the finger is read in the deck's own coordinates however the card has moved. Peeks and leaving cards take no touch of their own.
- `awaitFirstDown`, then `awaitHorizontalTouchSlopOrCancellation`: only a move that crosses the horizontal slop first takes the card; a vertical one goes to the scrolling screen around the deck. The over-slop amount is added, then the card follows the pointer 1:1 in both axes (`drag`), consuming the moves, with a `VelocityTracker`. A touch during a release spring catches the card where it is; a card flying off after a committed release (`CardMotion.committed`) takes no touch.
- **The release rule** (`CardStackCommit.side(dx, predictedDx, width)`, measured toward the end side): the predicted end picks the side; the release commits only past a threshold on that side — travel past `0.25 × width` (`DistanceFraction`) or predicted travel past `0.6 × width` (`VelocityFraction`). A drag that reverses before it's let go commits only if it clears its new side. The prediction is Android's fling coast: `splineBasedDecay(density).calculateTargetValue(0, velocity)` added to the travel. A cancelled drag, or no commit, springs home with the release velocity.
- What a release does each way is a `CardReleases` pair handed to the card: event mode `Fling` both ways; bound mode `Fling` toward the start when there's a next card, `Settle` toward the end when there's a previous one, `Reject` otherwise. `Fling` animates the offset off the side (`animate` with `Offset.VectorConverter`, `fastSpatialSpec`, the release velocity), then reports; `Settle` springs home (`defaultSpatialSpec`) and reports at once; `Reject` springs home.
- **Reports are guarded against the live top** (`LiveTop` through `rememberUpdatedState`): a release whose card is no longer the top when it reports is dropped, so the caller is never handed a card that has gone, and a bound step always writes the next or previous index of the card actually on top. A swipe back records `CardStackBook.swipedBackFrom` before reporting, so a two-card wrapping deck reads the change as a step back.
- After a fling reports, the card waits two frames for the composition to catch up. If it's still the live top (the caller kept it, or refused the step) it springs back home; otherwise it has become a leaving card, which carries it on from off-screen.

### Windows, steps and identities — `CardStackRules.kt`

- **`CardStackWindow.indices(count, currentIndex, visibleDepth, wraps)`**: from the clamped index, up to `visibleDepth` cards (at least 1), continuing from the start when `wraps`, never one card twice (`(4, 3, 3, wraps) → [3, 0, 1]`, `(2, 1, 3, wraps) → [1, 0]`); empty for an empty deck. Event mode reads index 0 without wrapping. A bound deck reads **at least two** cards and draws the first `visibleDepth`, so a one-deep deck can still tell a step back from a step forward. `next(index, count, wraps)` / `previous(…)` return null with nowhere to go.
- **`CardStackStep.between(old, new, preferBack)`**: `Back` when the new second card is the old top, `Forward` when the new top is the old second card, `Other` otherwise; when both hold (a two-card wrapping deck) `preferBack` decides.
- **Identities — `CardStackPass`.** A drawn card is a `CardStackCard(id, lap)`. A card keeps its lap while it stays drawn; every card entering the drawn window takes the next lap from a counter that only goes up, so **no identity is ever drawn twice** and a card coming back never meets an instance of itself still leaving. A card going round between the top and the back of a deck no deeper than the window takes a new lap too, without leaving: forward, its old instance leaves the top while a new one fades in at the back; back, the old instance fades from the back while the new one comes in on top. The pass names the old top when it isn't drawn any more (`leaving`) and its side (`leavesToward`: toward the end on a step back, which only a one-deep deck's top leaves on; toward the start otherwise).
- **Bookkeeping — `CardStackBook`.** A composition `read`s the window against the book (pure: the step, the pass, every previously drawn card not drawn now as `leaving`, whether the new top `enters` from the start side — a step back whose old top stays drawn — and `isNew` / `depthBefore` per card). The deck `commit`s the read in a `SideEffect` once that composition is applied, so a step holds for exactly the composition that drew it. Event mode never steps: its cards keep their laps, new ones take fresh laps, and removed ones leave.

### How cards come and go

- **One keyed list.** `CardDeck` emits `live + departures + leavingNow`, each in `key(card)`, so a card keeps its composition and its `CardMotion` as it moves up, and as it leaves (Compose moves keyed groups among siblings).
- **Arriving**: the deck's first cards appear at once. Afterwards a new card fades in (`fade` 0 → 1, `defaultEffectsSpec`), except the top coming back on a step back, which starts at the entry pose and slides in from the start side (`enter` 1 → 0, `defaultSpatialSpec`). A one-deep step back's new top fades in, since its old top is leaving.
- **Moving**: a card whose depth changes animates to it (`defaultSpatialSpec`).
- **Leaving**: a card that leaves the drawn window stays drawn under its own key as a departure, held in the deck's `departures` list (a snapshot list, added in the `SideEffect`), until its own animation ends and it removes itself. A bound deck's old top flies off toward `leavesToward` (`exit` 0 → 1, `defaultSpatialSpec`) above everything (`zIndex` 1), from wherever its release left it; every other card fades where it was (`fade` → 0), a peek just under the card taking its depth (`zIndex = −depth − 0.5`). So outside writes animate like swipes: forward sends the top off toward the start, back brings the previous card in, and a jump moves the cards that stay, fades the rest, and sends a top card that leaves off toward the start.

### Accessibility

- Only the top card is read: it's one element (`semantics(mergeDescendants = true)`) carrying the swipes as custom actions; every other card is `clearAndSetSemantics { }`.
- Event mode: the end-ward swipe, then the start-ward one, named by `startToEndActionLabel` / `endToStartActionLabel`, or "Swipe right" / "Swipe left" as they go on screen. Bound mode: "Next" (toward the start) and "Previous" (toward the end), each only when there's a card to move to. An action runs the same release as the drag, from rest, so a named fling flies off and then reports.

## Catalog pages

`demo/.../catalog/pages/collections/`, registered in `CollectionsPages.entries` in this order (ids): `cardStack`, `carousel`. `DataDisplayPages.entries` lists them at the end of the Data display category.

| Page | Preview | Loaded |
|---|---|---|
| Card stack | none | a bound deck of five album cards, 280 dp tall, four deep, opening on the second album so it steps both ways: each card an `amCard()` with the album's photo filling it (`catalogPhoto`) and its title (`titleLarge`) and count (`bodyMedium`) in `amArtText` on a scrim fade at the bottom. The deck alone: no buttons, no dots |
| Carousel | `Wide`: a carousel of four story cards, each wearing `amCard()` itself (a 120 dp photo across the top, the kicker in `bodySmall` / `onSurfaceVariant` over the title in `titleMedium`), with `sideInset = StageLayout.PageGutter + spacing.lg` at the default spacing, so the first card lands a gutter plus `spacing.lg` in and the next peeks in from the screen's edge; the page dots under it on the page | `Wide`: the same stories with `sideInset = PageGutter + spacing.xl` and `spacing = spacing.lg`, over their dots |

- The stories follow the appearance: snowy mountains and forests by day, New York and its jazz clubs by night (`commonsPhoto`), the same count both ways so the page holds across a switch. A story card's height is its photo plus its padding plus the two text lines' line heights, so it grows with the text size.
- Demo glyphs: `ic_auto_awesome_motion` (Card stack), `ic_view_carousel` (Carousel).

## Tests

In `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/collections/`:
- `CollectionsIndexClampTest`: inside, outside and empty.
- `CollectionsCardStackRulesTest`: the release rule (plain swipes, short drags, reversals short of and past their new side, flicks with no travel, no width); the window, wrapping and steps; the step reading; identities (a deep deck, the first window, a card coming back, wrapping decks going round, a two-card wrapping deck both ways, a jump and a still); the book (a bound deck's back and forward reads, a one-deep step back, a two-card wrapping deck after its own swipe back, an event deck letting cards go); and a 300-step walk over eight deck shapes checking that no identity is ever drawn twice.
