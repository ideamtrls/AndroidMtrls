---
scope: Buttons and button styles, the floating action button and its hub, icon, card and action-grid buttons, the action containers, and how a container disables the controls inside it — the tappable action primitives
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/actions/**, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/foundations/Enabled.kt, androidmtrls/src/main/res/drawable/am_ic_add.xml, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/actions/**]
keywords: [AMButton, AMButtonStyle, Primary, Secondary, Ghost, LocalAMButtonStyle, Button, TextButton, medium button, MediumContainerHeight, contentPaddingFor, textStyleFor, iconSizeFor, 56 dp, destructive, controlSurface, ControlSurfaceNode, controlOutline, LocalMinimumInteractiveComponentSize, frosted button, AMIconButton, AMIconButtonSize, IconToggleButton, toggle, checked, onCheckedChange, TooltipBox, PlainTooltip, BadgedBox, badge, AMCardButton, count, active, activeTint, tabular figures, AMCardActionRow, counts, actions, more_vert, AMActionGrid, windowInsets, AMActionGridRow, two three four tiles, AMActionGridButton, tile, tileHeight, contentDescription, spoken name, selection ring, AMFormActions, LocalAMEnabled, resolveEnabled, ProvideAMEnabled, AMFloatingActionButton, AMFloatingActionButtonState, initialOffset, DpOffset, Saver, rememberAMFloatingActionButtonState, floating action button, lift, long press, haptic, drag, systemGestureExclusion, absoluteOffset, FloatingActionButtonDiameter, AMFloatingActions, expanded, onExpandedChange, hub, remote, AnimatedVisibility, overlay registry, view tile, live control, rounded rectangle, shapes.medium, no pills, sitsOnPrimarySurface, ActionsPages, ActionsPostCard, FloatingActionButtonPage, sceneFrame, consumeWindowInsets]
updated: 2026-11-10
---

# Actions

The tappable action primitives: `AMButton` and its sealed `AMButtonStyle` (Primary and Secondary as floating surfaces on Material's `Button`, Ghost as Material's `TextButton`), `AMIconButton` (Material's `IconButton`, or `IconToggleButton` in its toggle form, with a long-press tooltip and an optional badge), `AMCardButton` and `AMCardActionRow`, the remote's grid (`AMActionGrid`, `AMActionGridRow`, `AMActionGridButton`), `AMFormActions`, `AMFloatingActionButton`, and the scene's remote, `AMFloatingActions`, which shows the button over an `AMScene` and opens the actions overlay from it. Every control here honors `LocalAMEnabled`, the internal local through which a container disables its children. Every AM button is a rounded rectangle in the theme's medium shape (12 dp), never a pill; only the floating action button is a circle, and icon buttons keep Material's circular ripple.

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `actions/Buttons.kt` | `AMButtonStyle` (`Primary`, `Secondary`, `Ghost`), `LocalAMButtonStyle`, `AMButton` (string and slot forms); private `ButtonBody`, `SurfaceButton`, `Modifier.controlSurface` (`ControlSurfaceNode`) |
| `actions/IconButton.kt` | `AMIconButtonSize`, `AMIconButton` (tap and toggle forms); private `IconButtonTooltip`, `IconButtonGlyph` |
| `actions/CardButton.kt` | `AMCardButton` |
| `actions/CardActionRow.kt` | `AMCardActionRow` |
| `actions/ActionGrid.kt` | `AMActionGrid`, `AMActionGridRow` (two-, three- and four-tile overloads), private `TileRow` |
| `actions/ActionGridButton.kt` | `AMActionGridButton` (string and slot forms) |
| `actions/FormActions.kt` | `AMFormActions` |
| `actions/FloatingActionButton.kt` | `AMFloatingActionButtonState`, `rememberAMFloatingActionButtonState`, `AMFloatingActionButton`; internal `FloatingActionButtonDiameter` (56 dp, which `AMScene` reads to lift its toasts over the button) |
| `actions/FloatingActions.kt` | `AMFloatingActions` (the hub); private `ShowActionsLabel` |
| `foundations/Enabled.kt` | internal `LocalAMEnabled`, `resolveEnabled`, `ProvideAMEnabled` |

- Glyph: `androidmtrls/src/main/res/drawable/am_ic_add.xml` (the floating action button's +).
- Pieces from `foundations/` the area builds on (`Foundations.md`): the internal `Modifier.amElevatedSurface` and `ElevatedSurfaceDrawer` (`Hairline.kt`), `Modifier.amHairline`, `AMElevation`, `AMSurfaceFill`, `AMSemantic`, `AMTheme.ActionGrid.tileHeight`, `AMTheme.selectionRingWidth`, `sitsOnPrimarySurface` (`Hairline.kt`, the card-or-black-sheet test behind a button's dark outline), `LocalAMBackdrop`; `LocalAMOverlayRegistry` (`internal/OverlayRegistry.kt`), `AMActionsOverlay` (`Overlays.md`) and `AMScene` (`Navigation.md`) for the hub.
- Catalog pages: `demo/.../catalog/pages/actions/` (below).

## Disabling — `LocalAMEnabled` (`foundations/Enabled.kt`)

- Internal `LocalAMEnabled` (a `compositionLocalOf`, `true` at the root) says whether the containers around a control let it be enabled.
- **Every AM control** passes its own `enabled` through `resolveEnabled(enabled)` (`enabled && LocalAMEnabled.current`) and hands the result to the Material component it wraps. Controls in other areas use the same helper.
- **A container** wraps its content in `ProvideAMEnabled(enabled) { }`, which provides `resolveEnabled(enabled)`, so containers combine: inside a disabled container nothing re-enables. `AMFormActions` is the one container that does so here.
- A disabled control keeps its place, size and fill: its content greys to Material's disabled color (`onSurface` at 0.38), and a floating surface drops its shadow, as Material's elevated button does.

## Buttons — `AMButton`, `AMButtonStyle` (`Buttons.kt`)

```kotlin
AMButton("Continue", onClick = ::next, modifier = Modifier.fillMaxWidth())                 // Primary from LocalAMButtonStyle
AMButton("Shuffle", onClick = ::shuffle, icon = painterResource(R.drawable.ic_shuffle), style = AMButtonStyle.Secondary(fill = AMSurfaceFill.Frosted))
AMButton("Delete playlist", onClick = ::delete, style = AMButtonStyle.Secondary(), destructive = true)
AMButton("Cancel", onClick = ::cancel, style = AMButtonStyle.Ghost)
```

- **Forms.** String: `AMButton(text, onClick, modifier, icon: Painter?, style, destructive, enabled)`. Slot: `AMButton(onClick, modifier, style, destructive, enabled, content: RowScope)`. Both go through one private `ButtonBody`, which picks the Material button and its size. The string form's `icon` sits before the label at the style's glyph size and spacing: `iconSizeFor` / `iconSpacingFor` the medium height for Primary and Secondary, `ButtonDefaults.IconSize` / `IconSpacing` for Ghost.
- **`AMButtonStyle`** is a sealed interface: `Primary(elevation = Low, fill = Solid)` and `Secondary(elevation = Low, fill = Solid)` are `@Immutable` classes with value equality; `Ghost` is a `data object`. There's no other way to style a button.
- **`LocalAMButtonStyle`** (public, `staticCompositionLocalOf`, `Primary()` at the root) is `style`'s default, so a container styles the buttons in its slots; a caller passes `style` only to override.
- **Primary and Secondary are Material's medium button**: `Button` with `Modifier.heightIn(min = ButtonDefaults.MediumContainerHeight)` (56 dp), `ButtonDefaults.contentPaddingFor(MediumContainerHeight, hasStartIcon)`, the label in `ButtonDefaults.textStyleFor(MediumContainerHeight)` (through `ProvideTextStyle`, so the slot form gets it too) and Material's ripple, in **`MaterialTheme.shapes.medium`** (12 dp), passed as the button's `shape` and the surface's, so the surface, the clip and the ripple are one rounded rectangle, never Material's stadium. They run taller than Material's standard 40 dp button on purpose, as rows run taller than Material's list items, so a filled button stands as tall as the rows around it. The button sits over a transparent container with no Material elevation, wearing the floating surface drawn by private `Modifier.controlSurface` (`ControlSurfaceNode`, which owns an `ElevatedSurfaceDrawer`): the shape's two-layer shadow at `elevation`, the surface fill, and in dark mode a hairline. The hairline is `AMTheme.controlOutline` when the button sits on a black surface, as `sitsOnPrimarySurface()` decides (`Foundations.md`): inside a card (any `NestingSurfaceNode` above it in the layout tree, flat or not) or on a primary or frosted `AMSheet` (internal `LocalAMOnPrimarySurface`, since a sheet is a window the layout tree can't see); the selection ring on the page and on a page-colored sheet. `fill = Frosted` draws glass over the nearest `AMBackdrop`'s art (solid with no backdrop), redrawing as it moves over the art. Label and glyph are `onSurface`; Primary keeps the medium label style's own medium weight, Secondary sets `FontWeight.Normal`.
- **The 48 dp target sits outside the surface.** Material's button reserves its touch target inside its own modifier, so a surface drawn around it would grow to the target. `SurfaceButton` applies `minimumInteractiveComponentSize()` itself, then the surface, then lays the `Button` out with `LocalMinimumInteractiveComponentSize` at 0 (restored for the label), so the surface always hugs the button and taps land across at least 48 dp. A `Box` with `propagateMinConstraints` passes a stretched width (a weight, `fillMaxWidth`) down to the button.
- **Ghost** is Material's `TextButton` with `shape = MaterialTheme.shapes.medium` (so its ripple is the same rounded rectangle): no surface, label in the scheme's `primary`, Material's own padding and target.
- **`destructive = true`** colors the label `AMSemantic.Destructive` (the scheme's `error`) in every style.
- **Disabled**: the label is `onSurface` at 0.38 and the surface casts no shadow (`AMElevation.None`); the fill stays.
- A button wraps its label, like Material's. Span a row with `fillMaxWidth()` or a weight.

## `AMIconButton` (`IconButton.kt`)

```kotlin
AMIconButton(painterResource(R.drawable.ic_pause_filled), "Pause", onClick = ::pause, size = AMIconButtonSize.Large)
AMIconButton(painterResource(R.drawable.ic_repeat), "Repeat one", onClick = ::repeatOne, badge = { Badge { Text("1") } })
AMIconButton(painterResource(if (shuffles) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle), "Shuffle", checked = shuffles, onCheckedChange = { shuffles = it })
```

- **Tap form**: `AMIconButton(icon, contentDescription: String?, onClick, modifier, size = Medium, badge: (BoxScope slot)? = null, enabled)`: Material's `IconButton` in the surrounding content color, its circular ripple and 48 dp target.
- **Toggle form**: `AMIconButton(icon, contentDescription: String?, checked, onCheckedChange: (Boolean) -> Unit, modifier, size, badge, enabled)`: Material's `IconToggleButton`, for something the button turns on and off (shuffle, repeat), so TalkBack hears a toggle named by the description ("Shuffle", not "Shuffle on") and its on or off state. Material's look: the content color while off, Material's checked color (`primary`) while on; the caller still swaps to the `_on` glyph where Material Symbols has one. A control with more states (repeat off, all, one) passes `checked` for any state but off, steps its own state in `onCheckedChange` and names the state in the description. Size, tooltip and badge as in the tap form.
- **`AMIconButtonSize`**: `Small` (20 dp glyph on Material's 40 dp button), `Medium` (24 dp, Material's own), `Large` (32 dp glyph on a 56 dp button, via `Modifier.size`; Small and Medium leave Material's size alone).
- **Tooltip** (`IconButtonTooltip`, both forms): with a `contentDescription`, the button sits in Material's `TooltipBox` (`rememberTooltipPositionProvider(TooltipAnchorPosition.Above)`, a `PlainTooltip` of the description, `rememberTooltipState()`), so a long press names the glyph. `modifier` goes on the tooltip box. A null description shows no tooltip, and `modifier` goes on the button.
- **Badge**: a nullable slot handed to Material's `BadgedBox` around the glyph, so the badge sits on the glyph's corner. Callers pass Material's `Badge`.

## `AMCardButton` (`CardButton.kt`)

- `AMCardButton(icon, contentDescription: String, onClick, modifier, count: String? = null, active = false, activeTint: AMSemantic = Info, enabled)`. One form: the content is fixed, a glyph and a count.
- **With a `count`**: Material's `TextButton` in `shapes.medium` (its ripple a rounded rectangle, like every AM button) with `TextButtonContentPadding`, the glyph at `ButtonDefaults.IconSize`, `IconSpacing`, then the count in `labelLarge` with tabular figures (`fontFeatureSettings = "tnum"`), one line. **Without**: Material's `IconButton` with the glyph at the same 18 dp. Both have no surface (the card supplies it), Material's ripple and 48 dp target.
- Color: `onSurfaceVariant` at rest; `active` takes `activeTint.color` (`Info` = the scheme's primary, `Destructive` for a filled heart). TalkBack reads the glyph's description, then the count; the caller names the state ("Liked").

## `AMCardActionRow` (`CardActionRow.kt`)

- `AMCardActionRow(modifier, counts: (RowScope slot)? = null, actions: RowScope slot)`: a full-width `Row`, centered vertically, with the counted actions at the start, a weighted `Spacer`, and the glyph-only actions at the end. No extra spacing: the Material buttons' own padding spaces them.
- The rule it encodes: all of a card's actions on one row under the content, counts at the start, share then ⋮ (`more_vert`) last in `actions`. A card with no counts passes `actions` alone.
- **Placement**: inset the row `spacing.xs` from the card's edges (start, end, bottom), not the card's `spacing.lg`, so the first glyph lines up with the text above (4 + the text button's 12 = 16) and ⋮ with its end.

## The remote's grid — `AMActionGrid`, `AMActionGridRow` (`ActionGrid.kt`)

- **`AMActionGrid(modifier, windowInsets = WindowInsets.navigationBars, content: ColumnScope)`**: a `Column` that fills its space (`fillMaxSize`; with no height bound it wraps its rows), applies `windowInsetsPadding(windowInsets)`, then `spacing.lg` at the sides and `spacing.xxl` at the bottom, and stacks its rows `spacing.md` apart at the bottom (`Arrangement.spacedBy(md, Alignment.Bottom)`). Inside an overlay that already consumed the insets, the grid's own inset is zero; on a page that doesn't reach the screen's bottom, pass `WindowInsets(0)`.
- **`AMActionGridRow`** has exactly three overloads, `(first, second, modifier)`, `(first, second, third, modifier)`, `(first, second, third, fourth, modifier)`, each slot a `@Composable () -> Unit`, and no list form: a row of one or of five doesn't compile. Private `TileRow` lays them in a full-width `Row` `spacing.md` apart, each slot in a `Box(Modifier.weight(1f), propagateMinConstraints = true)`, so every tile is its column's width.

## `AMActionGridButton` (`ActionGridButton.kt`)

```kotlin
AMActionGridButton(painterResource(R.drawable.ic_feed), "Feed", onClick = ::openFeed, active = current == Feed)
AMActionGridButton(onClick = ::openStorage) { Text("80%", style = MaterialTheme.typography.titleMedium); Text("Storage") }
```

- String: `(icon, title, onClick, modifier, contentDescription: String? = null, active, destructive, elevation = Low, enabled)`, the glyph (Material's 24 dp, no description) over the title (one line, ellipsis). TalkBack reads the title, or `contentDescription` when a short title needs a fuller name (a "Queue" tile reads "Add to queue"): the tile carries the description and the title is hidden (`clearAndSetSemantics {}`). Slot: `(onClick, modifier, active, destructive, elevation, enabled, content: ColumnScope)`. The string form delegates to the slot form.
- **The tile**: a `Column` exactly `AMTheme.ActionGrid.tileHeight` (76 dp) tall at any font scale, in `shapes.large`: `amElevatedSurface` (shadow at `elevation`, the surface fill, its dark-mode hairline turned off with `Color.Transparent`), then `amHairline` in both modes, then the clip and `clickable` (`Role.Button`, Material's ripple inside the shape), then `spacing.sm` side padding. Content is centered, `spacing.xs` apart, in `labelMedium` through `ProvideTextStyle` and `LocalContentColor`. Always solid: the remote's tiles are crucial controls.
- **Color**: `onSurface`; `destructive` → `AMSemantic.Destructive`; disabled → `onSurface` at 0.38; `active` and enabled → the scheme's `primary`.
- **`active`** (the current view, the chosen filter): the hairline becomes the `primary` ring at `AMTheme.selectionRingWidth`, the tile casts no shadow, and the semantics say selected. A disabled active tile keeps its ring over greyed content. A disabled tile casts no shadow either.

## `AMFormActions` (`FormActions.kt`)

```kotlin
AMFormActions(destructive = { AMButton("Delete", onClick = ::delete, destructive = true) }) {
    AMButton("Save", onClick = ::save, enabled = title.isNotEmpty())
}
```

- `AMFormActions(modifier, destructive: slot? = null, elevation = Low, enabled = true, primary: slot)`: a full-width `Row`, `spacing.md` apart, the destructive slot first. Each slot sits in a `Box(Modifier.weight(1f), propagateMinConstraints = true)`, so two buttons split the row evenly and Save alone spans it.
- It styles its slots through `LocalAMButtonStyle`: `Secondary(elevation)` for the destructive slot, `Primary(elevation)` for the commit slot, always solid. The caller marks Delete `destructive = true`.
- `enabled = false` wraps the row in `ProvideAMEnabled(false)`, disabling both buttons without touching either; each button's own `enabled` still works alone.
- No margin of its own: the form's column spaces it.

## `AMFloatingActionButton` (`FloatingActionButton.kt`)

```kotlin
val fab = rememberAMFloatingActionButtonState()
Box(Modifier.fillMaxSize()) {
    Screen()
    AMFloatingActionButton("Show actions", onClick = { showsActions = true }, state = fab)
}
```

- `AMFloatingActionButton(contentDescription, onClick, modifier, state = rememberAMFloatingActionButtonState(), enabled)`. Visual and gestures only: a scene shows it through `AMFloatingActions` (below), which opens the remote and fades it while an overlay is up.
- **A layer**: a `Box` that fills its space, applies `windowInsetsPadding(WindowInsets.navigationBars)` and `spacing.lg` at the bottom, and puts the button at `BottomCenter`. Touches anywhere else pass through.
- **The button**: 56 dp, `CircleShape`, the + (`am_ic_add`) in `onSurface`; `amElevatedSurface` at `Low` (dark hairline off) plus `amHairline` in both modes; always solid; Material's ripple inside the circle.
- **Modifier order** (it matters): `absoluteOffset { state.offset + live drag }` → `systemGestureExclusion()` → `size(56.dp)` → `clickable(interactionSource, indication = null, Role.Button)` → the lift `pointerInput` → `graphicsLayer` scale → surface → hairline → clip → `indication(interactionSource, ripple())`. The offset is absolute, so the drag follows the finger in right-to-left layouts. The exclusion keeps the system back gesture off the button wherever it sits, which only matters at the screen's edges. The scale sits inside the pointer input, so drag deltas aren't scaled.
- **Tap, lift, drag.** `clickable` handles the tap, ripple, focus and keys. The inner `pointerInput` (`awaitEachGesture`) waits for `awaitLongPressOrCancellation` (the system's long-press timeout; a release, consumption or leaving the bounds cancels it). On the long press it sets `lifted`, performs `HapticFeedbackType.LongPress`, then consumes every event of that pointer, adding its movement to the live drag. Consuming in the Main pass (the inner modifier sees events first) cancels `clickable`'s tap, so a lift never calls `onClick`. On release the drag is added to `state.offset` (in dp); a cancelled touch drops it. Either way `lifted` resets.
- **Lift look**: `animateFloatAsState` to 1.15 with `motionScheme.fastSpatialSpec()` (instant under Remove animations) and the shadow at `Medium`.
- **`AMFloatingActionButtonState(initialOffset = DpOffset.Zero)`** (`@Stable`): `offset: DpOffset`, screen-direction x, y down, from the resting place; `Saver` (`listSaver` of the two dp values). `rememberAMFloatingActionButtonState()` is `rememberSaveable`. Whoever outlives the button (the hub, the screen) holds it.
- Disabled: the + greys to 0.38 and the pointer input does nothing.

## `AMFloatingActions` (`FloatingActions.kt`)

The scene's remote: the floating action button over the scene, and the actions overlay it opens. It goes in `AMScene`'s `floatingActions` slot (`Navigation.md`).

```kotlin
var showsRemote by rememberSaveable { mutableStateOf(false) }
AMScene(
    current = view, landing = View.Inbox, onCurrentChange = { view = it },
    floatingActions = {
        AMFloatingActions(showsRemote, onExpandedChange = { showsRemote = it }) {
            AMActionGrid {
                AMActionGridRow({ /* Compose */ }, { /* Search */ })
                AMActionGridRow(
                    { AMActionGridButton(inbox, "Inbox", active = view == View.Inbox, onClick = { showsRemote = false; amInstantSwitch { view = View.Inbox } }) },
                    { /* Archive, the same way */ },
                )
            }
        }
    },
) { root -> … }
```

- `AMFloatingActions(expanded, onExpandedChange, modifier, contentDescription = "Show actions", state = rememberAMFloatingActionButtonState(), enabled = true, menu)`. Hoisted state, Material's menu spelling: the button's tap calls `onExpandedChange(true)`; back and a tap on the blur call `onExpandedChange(false)`.
- **The button**: an `AMFloatingActionButton` (`state`, `enabled`, `contentDescription`) inside an `AnimatedVisibility` whose `modifier` is the layer (`modifier.fillMaxSize()`). Padding that `modifier`'s bottom rests the button higher, above a bottom toolbar.
- **The menu**: `menu` in `AMActionsOverlay(expanded, onDismissRequest = { onExpandedChange(false) })`, so it's a blurred window over the whole screen, sheets included, and takes back first.
- **Overlays beat the button.** It's visible only while `!expanded` and the nearest `LocalAMOverlayRegistry` reports nothing up (`anyUp`): its own menu, a sheet, an alert, the spotlight and the loading overlay all fade it out (`fadeOut(motionScheme.fastEffectsSpec())`) and back in (`fadeIn(defaultEffectsSpec())`); instant under Remove animations. Hidden, the button leaves the composition; its `state` lives in the hub, so it comes back where it was. Banners and toasts don't report, so the button stays; in a scene the toast sits above it.
- **Views and actions share the remote.** A tile that acts closes the menu (`onExpandedChange(false)`, or the app's own flag) and acts. A view tile closes it, then sets the scene's root inside `amInstantSwitch`, so the view appears at once and the menu fades over it; the current view's tile is `active`. A live control may act without closing it.
- Outside an `AMScene` it still works, but nothing fades it: there's no registry.

## Catalog pages

`demo/.../catalog/pages/actions/`, registered in `ActionsPages.entries` in this order (ids): `buttonStyles`, `iconButton`, `cardButton`, `cardActionRow`, `formActions`, `floatingActionButton`, `actionGrid`, `actionGridButton`.

| Page | Preview | Loaded |
|---|---|---|
| Button styles | `FullBleed` surface strip: Continue (Primary, full width), Save draft (Secondary, full width), Cancel (Ghost) | a 220 dp `AMBackdrop` of `CatalogArt.Mountain` with Play and Shuffle frosted (with glyphs), Download (disabled) beside Share, Delete playlist (destructive), all Secondary but Play |
| Icon button | `FullBleed` strip: previous, play, next, all Large | a now-playing card: track over artist with cast disabled, then shuffle (Small, the toggle form, "Shuffle", swapping `shuffle`/`shuffle_on`), previous, pause and next (Large), repeat (Small) with a "1" badge in `onSurface` |
| Card button | `FullBleed` strip: reply 12, like 48, share | a post card: comments 1.2K, reposted 4 (active), liked 49 (active, `Destructive`, toggles), then share and ⋮ in an `AMCardActionRow` |
| Card action row | a post card: reply 12, like 48, share, ⋮ | two post cards: one with reply, reposted and liked; an announcement with only share and ⋮ |
| Form actions | Delete and Save | an edit-task card (`AMTextField` "Title", filled in, in a card padded `spacing.lg`) over Delete and Save, Save disabled while the title is empty |
| Floating action button | `Fill`, 380 dp: a "Notes" `AMScene` (an `AMPageHeader` over "Last action: None yet") whose `AMFloatingActions` opens Compose and Search; a tile closes the menu and names itself in the line | `Fill`, 380 dp: a mail `AMScene` with two roots, Inbox (the landing view) and Archive, each an `AMPageHeader` over a scrolling list of message rows; the remote holds Compose, Search / Inbox, Archive, the current view ringed. A view tile switches the root (`amInstantSwitch`), each list keeps its scroll, and back from Archive returns to Inbox |
| Action grid | `Wide`: Compose, Search / Feed, Alerts, Exit | a full-width "Show menu" button opening the full menu in `AMActionsOverlay`: Pin, Duplicate, Share, Archive / Compose, Search, Alerts / Feed, Exit; each tile closes it |
| Action grid button | three tiles: Share, Copy, Pin | filter tiles All (active), Starred, Archived (tapping moves the ring), then a custom "80% / Storage" tile, Move (disabled), Delete (destructive) |

- The Action grid's inline Preview grid passes `windowInsets = WindowInsets(0)`, since on the page it doesn't reach the navigation bar; the menu in the overlay keeps its default.
- **The Floating action button page's scenes** are real `AMScene`s on `Fill` bands: each frame is `fillMaxWidth().height(380.dp).consumeWindowInsets(WindowInsets.safeDrawing)` (private `sceneFrame`), so the band is a screen with no system bars of its own and the button rests `spacing.lg` above its bottom edge; `containerColor = Color.Transparent` lets the band's surface show. Message rows are hand-built on the row recipe, flat on the band, with 88 dp at the list's end so the last row scrolls clear of the button. Each scene calls `CatalogAutoPresent { showsMenu = true }`, so `--es present preview` (or `--es scrollTo loaded --es present loaded`) opens its menu for a screenshot. The menu is a window, so it opens over the whole screen, its tiles at the screen's bottom.
- `ActionsSupport.kt` holds `ActionsPostCard(text, modifier, author, actionRow)`: a card with the author (`titleMedium`) and text (`bodyLarge`) padded `spacing.lg` at the top and sides, then the action row inset `spacing.xs`.
- Demo glyphs these pages add: `ic_add_circle`, `ic_inbox` (the Floating action button page), `ic_smart_button`, `ic_play_circle`, `ic_favorite`, `ic_favorite_filled`, `ic_call_to_action`, `ic_fact_check`, `ic_grid_view`, `ic_grid_view_filled` (entry icons and hearts); `ic_play_arrow_filled`, `ic_pause_filled`, `ic_skip_previous_filled`, `ic_skip_next_filled`, `ic_shuffle`, `ic_shuffle_on`, `ic_repeat`, `ic_cast`; `ic_chat_bubble`, `ic_forum`; `ic_edit_square`, `ic_feed`, `ic_logout`, `ic_library_add`, `ic_archive`, `ic_content_copy`, `ic_filter_list`, `ic_star`, `ic_drive_file_move`, `ic_delete`. They also use `ic_share`, `ic_more_vert`, `ic_search`, `ic_notifications` and `ic_push_pin`.
