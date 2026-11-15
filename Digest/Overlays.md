---
scope: Presented overlays and their shared rules — sheets and the discard guard, alerts, the actions overlay, the spotlight and holds, backdrop blur and the paused backdrop; overlay windows, blur-behind and its wash, back order, and the overlay-up registry — the library's overlays/ folder and the Overlays catalog pages
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/overlays/**, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/internal/OverlayRegistry.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/overlays/**]
keywords: [AMSpotlightState, frostedContainerColor, alert container, alert frosted edge, amHairline, rememberAMSpotlightState, amHoldable state form, hold actions, LocalAMRowHold, AMHoldAction, HoldActionTiles, actions per item, Overlay.wash, WashLight, WashDark, AMSheet, AMSheetHeight, Full, Content, ModalBottomSheet, rememberBottomSheetState, confirmValueChange, ModalBottomSheetProperties, shouldDismissOnBackPress, transparent scrim, no dimming, drag handle, ExpandedShape, sheetChrome, requireOffset, sheet shadow, sheet edge, GlassNode, frosted sheet, frost over the page, screenToLocal, scene overlays sheet, Modifier.amDiscardGuard, AMDiscardGuardState, LocalAMDiscardGuard, DiscardBackHandler, OnBackInvokedCallback, keyboard back, Discard changes, Keep editing, AMAlert, AMAlertItem, AMAlertAction, AlertDialog, destructive, cancel, AMActionsOverlay, AMOverlayWindow, Dialog window, DialogWindowProvider, FLAG_BLUR_BEHIND, blurBehindRadius, FLAG_DIM_BEHIND, setDimAmount, isCrossWindowBlurEnabled, addCrossWindowBlurEnabledListener, wash, ProvideOverlayWindowLocals, LocalAMWindowBlur, FrostPath, OverWindowBlur, frost over the window's blur, cross-window blur off, disable_window_blurs, blur lag, DismissCatcher, Close, AMSpotlight, item form, SpotlightLayout, spotlight elevation, flat spotlight card, Modifier.amHoldable, long press, longPressTimeoutMillis, touch slop, HapticFeedbackType.LongPress, CustomAccessibilityAction, onLongClick, AMBackdropBlur, FrostedSlice, Modifier.amPausedBackdrop, AMPausedScrim, Modal, page wash, Modifier.blur, BlurredEdgeTreatment, back order, predictive back, ReportOverlayUp, AMOverlayRegistry, LocalAMOverlayRegistry, AMScene, sheet toast host, AMToastHost, LocalAMLiesOn, LiesOn.Sheet, embedded, the tint, controlOutline, 34 dp, sigma, FallbackDimLight, FallbackDimDark, PageWashAlpha, OverlaysPages]
updated: 2026-11-14
---

# Overlays

What presents over a screen. Each overlay is its own window, the Android way: `AMSheet` on Material's `ModalBottomSheet` (a frosted background over a transparent scrim, with the discard guard, `Modifier.amDiscardGuard`), `AMAlert` on Material's `AlertDialog`, and `AMActionsOverlay` (the remote's grid) and `AMSpotlight` (with `Modifier.amHoldable`) in a full-screen dialog window with the platform's blur-behind. Two non-window pieces blur pictures in place: `AMBackdropBlur` and `Modifier.amPausedBackdrop`. Every window reports to the overlay-up registry, and back takes the topmost window first.

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `overlays/OverlayWindow.kt` | internal `AMOverlayWindow` (the blurred window), `ProvideOverlayWindowLocals`, `OverlayCloseLabel`; private `BlurBehind`, `rememberCrossWindowBlurEnabled`, `DismissCatcher`, `InputBlocker` |
| `overlays/ActionsOverlay.kt` | `AMActionsOverlay` |
| `overlays/Spotlight.kt` | `AMSpotlight` (visible, item and state forms), `AMSpotlightState`, `rememberAMSpotlightState`; private `SpotlightLayout`, `LastItem` |
| `overlays/HoldAction.kt` | `AMHoldAction`; internal `spokenName`, `HoldActionTiles` (a held item's tiles from its actions) |
| `overlays/Holdable.kt` | `Modifier.amHoldable` (action and spotlight-state forms); private `HoldableElement`, `HoldableNode`, `awaitLetGo` |
| `overlays/Sheet.kt` | `AMSheetHeight`, `AMSheet`; private `SheetTitle` |
| `overlays/SheetChrome.kt` | internal `Modifier.sheetChrome`; private `sheetOffset`, `GlassElement`, `GlassNode` |
| `overlays/DiscardGuard.kt` | `Modifier.amDiscardGuard`; internal `AMDiscardGuardState`, `LocalAMDiscardGuard`, `DiscardBackHandler`, the prompt's words; private `DiscardGuardNode` |
| `overlays/Alert.kt` | `AMAlertAction`, `AMAlertItem`, `AMAlert`; private `AlertButton` |
| `overlays/BackdropBlur.kt` | `AMBackdropBlur`; private `BackdropBlurNode` |
| `overlays/PausedBackdrop.kt` | `AMPausedScrim`, `Modifier.amPausedBackdrop`; private `PausedScrimNode` |
| `internal/OverlayRegistry.kt` | `AMOverlayRegistry`, `LocalAMOverlayRegistry`, `ReportOverlayUp` |

- Pieces from other areas it builds on: `Style.ModalOverlay` (the blur, the mode's wash and the page wash), `Style.Frosted`, `Style.Page`, `Style.Shadow`, `ShapeShadows`, `drawInsetOutline`, `ProvideAMSurfaceStyle`, `LocalAMBackdrop` / `AMBackdropSource`, `FrostedSlice` and `frostedSampleRegion` (`Foundations.md`); `LocalAMEnabled` (`Actions.md`); `Modifier.amCard` (`Layout.md`); `AMSectionHeader`; `LocalAMLiesOn`, `LiesOn`, `LocalAMWindowBlur` and `FrostPath` (`SurfaceLook.kt`); `AMToastHost` (`Feedback.md`). The scene root that owns the registry, `AMScene`, is in `Navigation.md`.
- No library glyphs and no string resources: the few words the library says ("Close", "Discard changes?", "Your changes won't be saved.", "Discard", "Keep editing") are internal constants.
- Catalog pages: `demo/.../catalog/pages/overlays/` (below).

## Shared rules

### Every overlay is a window; back takes the topmost first

- A sheet is Material's sheet window, an alert Material's dialog window, the remote and the spotlight a Compose `Dialog` window. A window covers the whole screen from wherever it's composed, so nothing is attached to a scene root, and a page can present one straight from its own button. The scene root (`AMScene`) only owns the registry the windows report to, and the remote's button that they fade.
- **Back order, by construction.** Windows stack in the order they open, and back and predictive back go to the topmost. An alert opens over whatever is up, so it goes first; the spotlight or the remote opens over a sheet, so it goes next; then the sheet; then the activity (panels, the landing view, out of the app). A sheet's own discard prompt is an alert composed inside the sheet, so it sits over the sheet. The order holds as long as each overlay opens over the ones below it, as tiles that open something close the remote first.
- Dialog windows are modal for TalkBack: content behind is skipped while one is up.

### The overlay-up registry

`internal/OverlayRegistry.kt`: a scene (`AMScene`, `Navigation.md`) provides an `AMOverlayRegistry` through `LocalAMOverlayRegistry`; every overlay calls `ReportOverlayUp(up)`, which registers a key while `up` and withdraws it when `up` turns false or it leaves the composition, so the scene's `AMFloatingActions` fades its button while `anyUp` (`Actions.md`). With no owner, reporting does nothing. Reports: `AMSheet` while `visible` and composed, `AMAlert` while its item isn't null, `AMActionsOverlay`, `AMSpotlight` and `AMLoadingOverlay` (`Feedback.md`) while `visible`. Windows inherit the presenting composition's locals, so an overlay opened inside another still reports to the scene.

### The blurred window — internal `AMOverlayWindow(visible, onDismissRequest, content: BoxScope)`

The one window behind `AMActionsOverlay` and `AMSpotlight`, so the paused screen reads the same behind both. Nothing about it is a public knob.

- **The window.** A Compose `Dialog` with `DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)`: full screen, edge to edge. Its `Window` comes from `(LocalView.current.parent as DialogWindowProvider).window`. Its status and navigation bar icons are dark in light mode and light in dark mode (`WindowCompat.getInsetsController`), as the washed screen under them is.
- **Blur-behind and the wash.** No window dim (`FLAG_DIM_BEHIND` cleared). With cross-window blur on: `FLAG_BLUR_BEHIND`, `blurBehindRadius` = `AMTheme.Overlay.blurRadius` (34 dp, Android's blur radius for a Gaussian sigma of about 20) in px; with it off, no blur. Over it, the window's content box draws **the mode's wash**, `AMTheme.Overlay.wash` (`Style.ModalOverlay.WashLight` white 0.6, `WashDark` black 0.65), full screen, so the paused screen is light in light mode and dark in dark mode, and the spotlight's title (the tone's fixed ink) holds over any screen, sharp art included, blur or not: over black art in light mode the washed ground is gray 0.6 (L 0.318), the black ink 7.36:1; over white art in dark mode gray 0.35 (L 0.100), the white ink 7.00:1. `rememberCrossWindowBlurEnabled` reads `WindowManager.isCrossWindowBlurEnabled` and follows `addCrossWindowBlurEnabledListener` (battery saver flips it live), removing the listener on dispose. `AMOverlayWindow` reads it at the top of the `Dialog`'s content and hands it to both `BlurBehind(progress, blurEnabled)` and the window's content (`ProvideOverlayWindowLocals(windowBlur = blurEnabled)`, below).
- **Frost over its own blur.** The window's blur-behind is the blur, so in a frosted app a frosted surface in it (a tile, the spotlight's card, a card or button the remote holds) draws the frosted fill, the frosted edge and the shadow outside its shape straight over the blurred, washed screen, and samples nothing (`FrostPath.OverWindowBlur`, `Foundations.md`). While the system has cross-window blur off (battery saver; to try it on the emulator, `adb shell settings put global disable_window_blurs 1`, and `0` to bring blur back) it draws the solid look instead (`FrostPath.Solid`), switching live with the setting, since internal `LocalAMWindowBlur` (`SurfaceLook.kt`, a dynamic `compositionLocalOf<Boolean?>`, null outside a blurred window) is read while drawing. Blur turning off is safe at once: the surfaces go solid in the next frame. Blur coming back, the platform's blur lags the flag (0.1 to 2.5 s on the emulator), so for that moment the frost lies over the washed, still-sharp screen and a faint ghost of it shows through (about 16 %, under the wash and the fill); text still holds, since `windowFrostGround` is the worst pixel, blurred or not. The platform gives no signal when its blur is back, so the library doesn't guess a delay. Pieces embedded in those surfaces draw the tint over them as anywhere else. Secondary text holds 4.5:1 on that frost over the worst screen (`windowFrostGround`, pinned by `ArtWashTest.textHoldsOnFrostOverAWindowsBlur`).
- **Motion.** `visible` drives an `Animatable` from 0 to 1 with `motionScheme.defaultEffectsSpec()` and back with `fastEffectsSpec()`; the window stays composed until it reaches 0. The content's alpha (the wash with it) and the blur radius both follow it (the radius updated on the window's attributes as it changes, the platform's way to fade a blur). Remove animations makes it instant. While fading out, an `InputBlocker` on top swallows every touch and dismissal is ignored.
- **Dismissal.** The `Dialog`'s `onDismissRequest` (back, predictive back) and a full-screen `DismissCatcher` behind the content (`detectTapGestures`, so a tap anywhere the content takes no input, between tiles included, dismisses) call `onDismissRequest`. For TalkBack the catcher is one button, "Close", with `traversalIndex = 1f` so it's read after the content.
- **A fresh context** (`ProvideOverlayWindowLocals(windowBlur: Boolean? = null, content)`, also used by the sheet's content, an alert's dialog, the loading overlay's dialog and a sheet's banner window, `Feedback.md`): `LocalAMBackdrop` and `LocalAMArtTone` null (a window can't sample the screen behind it, and nothing in it lies on art), `LocalAMWindowBlur` `windowBlur` (the blurred window passes its blur state; every other window passes nothing, so a frosted surface in it is frost over the page color), `LocalAMEnabled` true, `LocalAMLiesOn` `Page` (no surface under its content from the presenting screen; a sheet sets its own inside), `LocalContentColor` `onSurface`, and `ProvideAMSurfaceStyle(Elevated)`, so cards and tiles float whatever surface style the presenting screen had. The app's surface setting (`LocalAMSurfaceFill`) carries in, so text in a window resolves as the app's does.

## `AMActionsOverlay` (`ActionsOverlay.kt`)

```kotlin
AMActionsOverlay(showsActions, onDismissRequest = { showsActions = false }) {
    AMActionGrid { AMActionGridRow({ … }, { … }, { … }) }
}
```

`AMActionsOverlay(visible, onDismissRequest, modifier, content)`: the blurred window holding `content` in a full-window `Box` anchored to its bottom (`modifier` goes on that box). The content is usually an `AMActionGrid`, which pads itself clear of the navigation bar and stacks its rows at the bottom; tiles that act set the state false themselves. `AMFloatingActions`, the scene's remote, presents its menu through it (`Actions.md`).

## `AMSpotlight` (`Spotlight.kt`)

One thing up close over the blurred screen: a held item over what to do with it, or a field's picker under the field's name.

```kotlin
AMSpotlight(picking, onDismissRequest = { picking = false }, title = "Starts") { DatePicker(state) }   // a picker
AMSpotlight(held, onDismissRequest = { held = null }, actions = { article -> listOf(AMHoldAction("Share", share) { share(article) }) }) { article -> ArticleRow(article) }   // a hold
```

- **Visible form**: `AMSpotlight(visible, onDismissRequest, modifier, title: String? = null, actions: slot? = null, elevation = AMElevation.Low, content)`, for pickers and anything driven by a flag.
- **Item form**: `AMSpotlight<T : Any>(item: T?, onDismissRequest, modifier, title: (T) -> String? = { null }, actions: (T) -> List<AMHoldAction> = { emptyList() }, elevation = AMElevation.Low, content: (T) slot)`, up while `item` isn't null. It delegates to the visible form, keeping the last non-null item (`LastItem`) so the card fades out intact. **Tiles per item**: `actions` gives each item its own list, so one spotlight serves held items with tiles and pickers without; an empty list shows no tiles at all (the visible form gets a null `actions`). The list becomes tiles through internal `HoldActionTiles`: one `AMActionGridButton` per action in order (its `label`, `icon`, `contentDescription`, `destructive`), in the action grid's equal columns (internal `ActionTileRow`, `Actions.md`), a single action at half the row, at most four (more fails at once). **A tile closes the spotlight first** (`onDismissRequest`), then runs its action, so callers don't hide it in each action.
- **`AMHoldAction(label, icon: Painter, contentDescription: String? = null, destructive = false, onClick)`** (`HoldAction.kt`): an `@Immutable` action payload, like `AMSwipeAction`: the tile's title and glyph, the fuller spoken name when the title is short ("Add to queue" for "Queue"; `spokenName`), its role, and what it does. Build the list in a plain function from painters read while composing.
- **Layout** (`SpotlightLayout`), inside the blurred window: a `Column` filling the window, padded by `WindowInsets.safeDrawing` (system bars and the keyboard) and `spacing.xxl` at top and bottom, its children `spacing.md` apart and pushed to the bottom: the optional `title` as an `AMSectionHeader` at the `spacing.lg` gutter, in its over-art look for the mode's tone (`LocalAMArtTone` set to the mode's around it: the tone's fixed ink, white on dark and black on light, with the tone's shadow, over the window's wash); the card; the optional `actions` (one `AMActionGridRow`) at the same gutter, so the tiles land where the remote's do.
- **The card** is `Modifier.amCard(elevation = elevation)` (in the app's surface setting, whatever the item was on the screen behind: solid in a solid app; in a frosted app frost over the window's own blur, sampling nothing, and solid while the system has cross-window blur off) around a `verticalScroll`, the content inside it in `ProvideAMOnSurface`, so its secondary text resolves from the card and, in a frosted app, a control with its own shape inside (a date chip, a button) draws the tint, centered in a full-width box with `weight(1f, fill = false)`: it hugs its content's width (a row on the row recipe fills the gutters, a picker sits centered), takes its own height, and scrolls when it's taller than the room above the tiles. A card the content brings nests and draws nothing. The empty space above stays the catcher's, so a tap there dismisses.
- **Its shadow** (`elevation`): `Low` by default; an app whose cards are flat passes `AMElevation.None`, and the card in focus keeps its fill, corners and outline but casts nothing, like the cards it came from. The tiles keep their shadow, as the remote's do.

## `Modifier.amHoldable` (`Holdable.kt`)

`Modifier.amHoldable(onHold, actions: List<AMHoldAction> = emptyList(), enabled = true)`: press and hold, the spotlight's trigger; `actions` are the held item's spotlight tiles. **The state form** `Modifier.amHoldable(state: AMSpotlightState<T>, item: T, enabled = true)` is it with `onHold = { state.show(item) }` and `actions = state.actionsFor(item)`.

```kotlin
val spotlight = rememberAMSpotlightState<Article> { article -> listOf(AMHoldAction("Share", share) { share(article) }) }
ArticleRow(article, Modifier.amHoldable(spotlight, article))   // the hold opens it; TalkBack gets Share on the row
AMSpotlight(spotlight) { article -> ArticleRow(article) }
```

- **The gesture** (`HoldableNode`, a `DelegatingNode` over a `SuspendingPointerInputModifierNode`): it watches each touch in the **Initial** pass, so it sees the touch before the item's own click and a scrolling parent's drag, whatever the modifier order, without consuming anything until the hold. After the first down it waits `viewConfiguration.longPressTimeoutMillis` (`withTimeoutOrNull`) for the touch to let go (`awaitLetGo`: lifted, moved past `touchSlop`, or consumed by someone else). If it outlasts the timeout, it performs `HapticFeedbackType.LongPress` (from `LocalHapticFeedback`), calls `onHold` while the finger is still down, then consumes the rest of that touch, so the item's click never fires (its ripple shows while held, then cancels).
- So a tap still does the item's main thing, and a vertical drag that starts on the item still scrolls.
- **TalkBack** (`SemanticsModifierNode`): an `onLongClick` action that calls `onHold` (double-tap and hold), and the hold's `actions` as custom actions in tile order, each named by its `spokenName` and running its `onClick`, so the spotlight's tiles are on the item directly. The node rebuilds its `CustomAccessibilityAction`s only when the names change (each one calls the current action at its index), so recomposition with fresh lambdas costs nothing.
- **One list on a row.** On attach the node reads internal `LocalAMRowHold` (`Lists.md`): inside a row container (`AMSwipeableRow`, a reorderable list's cell) it puts its actions in the container's `RowActions.held` instead of on its own node, and the row carries them all in one list, **the hold's first (tile order), then the moves, then the swipes**; detaching clears them. Outside a container they go on its own node. Nothing is lost when a row swipes, holds and reorders at once.
- `enabled = false` ignores touches and drops both semantics.

### `AMSpotlightState<T>` (`Spotlight.kt`)

- `@Stable`: `item: T?` (read-only from outside), `show(item)`, `dismiss()`, `actionsFor(item): List<AMHoldAction>`; internal `actions`, the latest function (snapshot state). `rememberAMSpotlightState(actions = { emptyList() })` remembers one and updates `actions` in a `SideEffect`, so the function can read the screen's current state. In memory only, like a hold.
- `AMSpotlight(state, modifier, title, elevation, content)`: the item form with `item = state.item`, `onDismissRequest = state::dismiss`, `actions = state::actionsFor`. One definition of the actions feeds the tiles and every held item's TalkBack actions.

## `AMSheet` (`Sheet.kt`, `SheetChrome.kt`)

```kotlin
AMSheet(editing, onDismissRequest = { editing = false }, title = "Edit profile") { ProfileForm() }
AMSheet(choosingAlbum, onDismissRequest = { choosingAlbum = false }, title = "Add to album", height = AMSheetHeight.Content) { … }
```

`AMSheet(visible, onDismissRequest, modifier, title: String? = null, height = Full, content: ColumnScope)`.

- **Material's sheet.** `ModalBottomSheet` with a `rememberBottomSheetState(Hidden, setOf(Hidden, Expanded), confirm)` (no half-expanded stop), `BottomSheetDefaults.ExpandedShape`, `BottomSheetDefaults.DragHandle()`, **`containerColor = Color.Transparent`** (the chrome draws the frost, below), `tonalElevation = 0.dp`, `contentColor = onSurface`, **`scrimColor = Color.Transparent`** (no dimming; the scrim still takes a tap outside), and Material's content window insets (navigation bar and keyboard). Drag, fling, tap outside, back and predictive back are Material's.
- **Presence.** `composed` starts false; when `visible` turns true it becomes true and Material's sheet slides itself in on entering (reopened mid-hide, the effect calls `show()`). When `visible` turns false from code, the guard's `closing` is set (never asks) and the effect `hide()`s, then removes it. When the user closes it, Material calls back after its own hide: `composed = false`, then `onDismissRequest`.
- **`AMSheetHeight`**: `Full` gives the content column `fillMaxHeight()`, so content can take the room with a weight; `Content` hugs the content.
- **The background is frosted in either setting** (`AMTheme.surfaceFill` never changes it): frost over the art of the `AMBackdrop` it's presented from, or over the page color with no art to sample (the chrome, below). It takes no fill of its own.
- **Title** (`SheetTitle`): `titleLarge`, start-aligned, full width, `spacing.lg` at the sides and `spacing.md` below, heading semantics, under the drag handle and above the content in one column.
- **Content context**: `LocalAMDiscardGuard` (the sheet's own guard), then `ProvideOverlayWindowLocals`, so what the sheet holds is enabled and elevated whatever the screen set; cards in a sheet float as on a page. Then `LocalAMLiesOn` = `Sheet`, always: its pieces are embedded, as inside a card (the sheet is a window, so they can't find it in the layout tree; `Foundations.md`). **What the sheet holds follows the app's setting: solid in a solid app, tinted in a frosted app.** A card, a filled button or a self-filled row (`AMListRow` on its own) on it draws the tint in a frosted app; in a solid app they're solid and a filled `AMButton` takes the brighter `AMTheme.controlOutline` in dark mode. Secondary text and accents take the frosted ones (`Frosted.secondaryText`, `Frosted.tint`), since they lie on frost.
- **Its own hosts, which take over while it's up**: the title and content column sit in the internal `BannerHost` and `ToastHost` with `overScreen = true`, sized like the column (full width, and full height for `Full`). While the sheet is up they show every banner and toast posted to the screen's hosts or from inside the sheet: a toast at the sheet's bottom, over its content, above the navigation bar and the keyboard; a banner in a popup window of its own at the top of the screen, over the sheet, which never takes focus and lets touches below its band through. When the sheet goes, a message still up goes back to the screen's host (`Feedback.md`).

### The chrome — internal `Modifier.sheetChrome(sheetState, shape, backdrop)`

Material passes its sheet surface no shadow, and the only slot outside the sheet's clip is its `modifier`, which comes first in the chain, ahead of the drag offset and the predictive back scaling. So the chrome is `offset { +sheetOffset }` → the chrome → `offset { −sheetOffset }`: the two offsets cancel for the sheet itself, and the chrome draws at the sheet's current position. `sheetOffset` reads `sheetState.requireOffset()` in placement (far below the screen before it's known), so only placement reruns while the sheet moves.

- **The frost** (`GlassNode`): the sheet is a window of its own, so it finds its slice of the art its `LocalAMBackdrop` names (the presenting screen's `AMBackdrop`; in an `AMScene`'s `overlays`, the showing root's, `Navigation.md`) by screen position (`art.screenToLocal(sheet.localToScreen(Offset.Zero))`) instead of the layout tree, cuts it with `frostedSampleRegion` (2× the blur of margin), records it into its own layer under `BlurEffect(Frosted.blurRadius, Clamp)`, and draws: the `Medium` shadow clipped outside the shape, the blurred slice clipped to it, `Frosted.fill`, the sheet, the frosted edge. It resamples whenever its screen position changes, and whenever the art records itself anew: it listens on the source (`AMBackdropSource.addRecordListener`, registered while attached and moved by `follow` when the backdrop changes) and invalidates its draw, since its own window isn't redrawn when the presenting window's art changes (a band's new picture, rows scrolling under it), so an open sheet never keeps a stale sample. With no backdrop, or nothing placed or overlapping yet, the page color (`Style.Page`) takes the slice's place under the fill: the shadow outside the shape, the page color, `Frosted.fill`, the sheet, the frosted edge, so it's frost over the page, opaque (`#FAFAFC` / `#0A0A0B`), never unblurred translucency and never the solid surface.
- The chrome sits outside Material's predictive back scaling and overscroll stretch, so it keeps the sheet's resting size during those gestures.
- It depends on `modifier` coming ahead of the drag offset in Material's chain (Material 3 1.5.0-rc01); recheck by eye on a Material update.

### The discard guard — `Modifier.amDiscardGuard(hasChanges)` (`DiscardGuard.kt`)

- **Report.** `DiscardGuardNode` reads `LocalAMDiscardGuard` when it attaches and reports `hasChanges` to that guard under its own identity, again on every update, and withdraws on detach. Each sheet provides its own `AMDiscardGuardState` to its content only, so the nearest sheet owns the report and an outer sheet never hears an inner one; outside a sheet the modifier does nothing. `hasChanges` is true while any report is.
- **Drag down and tap outside.** The sheet state's `confirmValueChange` is `guard.confirm`: refusing `Hidden` while there are changes (unless `closing`) makes the sheet settle back and sets `prompting`.
- **Back.** While there are changes the sheet passes `ModalBottomSheetProperties(shouldDismissOnBackPress = false)`, which takes Material's own back handling off, and `DiscardBackHandler` registers an `OnBackInvokedCallback` (`PRIORITY_DEFAULT`) on the sheet window's dispatcher (`LocalView.findOnBackInvokedDispatcher()`) that sets `prompting`. **With the keyboard up the first back only hides the keyboard**: the handler unregisters while `WindowInsets.ime` has height, so the keyboard's own back handling takes it, and if a back still reaches the handler with the keyboard showing (`rootWindowInsets.isVisible(ime)`), it hides the keyboard instead of asking. A dirty sheet doesn't preview the predictive back gesture; the prompt comes on release.
- **The prompt.** `AMAlert` with `AMAlertItem("Discard changes?", confirm = Discard (destructive), message = "Your changes won't be saved.", cancel = Keep editing)`, composed inside the sheet (over it). Keep editing, back and a tap outside close the alert only. Discard sets `closing`, hides the sheet, then calls `onDismissRequest`.
- Closing from code never asks; closing clears `prompting`.

## `AMAlert` (`Alert.kt`)

```kotlin
alert = AMAlertItem("Delete project?", confirm = AMAlertAction("Delete", destructive = true) { delete() }, message = "This can't be undone.", cancel = AMAlertAction("Keep"))
AMAlert(alert, onDismissRequest = { alert = null })
```

- **`AMAlertAction(label, destructive = false, onClick = {})`** and **`AMAlertItem(title, confirm, message = null, cancel = null)`**: `@Immutable` payloads with identity equality (they carry lambdas). `confirm` alone acknowledges; with `cancel` the alert asks.
- **`AMAlert(item: AMAlertItem?, onDismissRequest, modifier)`**: Material's `AlertDialog` while `item` isn't null, in its native shape, motion and dim: title, optional message as its text, `TextButton`s with the cancel as the dismiss button before the confirm, all in Material's colors. A destructive label takes `AMSemantic.Destructive` through `textButtonColors(contentColor)`.
- **Its container follows the app's setting** by container color alone: `containerColor = frostedContainerColor().takeOrElse { AlertDialogDefaults.containerColor }` (`Foundations.md`), Material's own in a solid app and frost over the page color in a frosted one, opaque (`#FAFAFC` / `#0A0A0B`), since the dialog samples no art. Frosted, it's outlined by the frosted edge as other frosted surfaces are: `modifier.amHairline(AlertDialogDefaults.shape, AMTheme.Frosted.edge)` (the dialog's own shape, passed as its `shape` too), so its shape holds against the platform's dim in dark mode, where the container (`#0A0A0B`) and the dimmed page (`#080809`) are all but the same. Solid, it's Material's dialog as it is, with no outline. The call starts inside `ProvideOverlayWindowLocals` (above), so the dialog is a window's fresh context wherever it's opened from: lying on the page (never the tint, from a sheet's discard prompt either) and outside any blurred window's state (opened from the remote or the spotlight).
- A button runs its `onClick`, then `onDismissRequest`. Back and a tap outside run `cancel.onClick` when there is a cancel, then `onDismissRequest`; dismissing never runs `confirm`, so it never runs a destructive action.

## `AMBackdropBlur` (`BackdropBlur.kt`)

`AMBackdropBlur(modifier, radius = AMTheme.Overlay.blurRadius)` (34 dp): a pure Gaussian blur, no tint, of the art behind an area. A `Spacer` sized by `modifier` whose `BackdropBlurNode` draws the nearest `AMBackdrop`'s art under it through the foundations' `FrostedSlice` (the slice plus 2× the blur of margin, clamped at the art's edge, so it never darkens toward transparency), clipped to its bounds, then its content. It tracks its offset in the art and redraws as it moves; the art's layer is referenced, so an arriving photo shows without re-recording. Outside an `AMBackdrop` it draws nothing. For a now-playing background, a strip over art, or any region that blurs part of a picture; clip it for corners.

## `Modifier.amPausedBackdrop` (`PausedBackdrop.kt`)

`Modifier.amPausedBackdrop(scrim = AMPausedScrim.Modal, blurRadius = AMTheme.Overlay.blurRadius)` (34 dp): stills a picture into a backdrop that other content stacks above.

- The chain: `PausedScrimNode` (draws the content, then the scrim), `clearAndSetSemantics {}` (decoration, skipped by screen readers), a pointer input that consumes every event in the Initial pass (nothing inside reacts), then Compose's `Modifier.blur(blurRadius, BlurredEdgeTreatment.Rectangle)` (the content's own layer blurred, clipped and clamped at its edges).
- **`AMPausedScrim`**: `Modal`, the mode's wash (`Style.ModalOverlay.WashLight` / `WashDark`), the modal backdrop's look; `Page`, the page color at 0.55 in both modes (`Style.ModalOverlay.PageWashAlpha`), so the picture's color bleeds through while the page reads as itself (a photo's detail behind the photo).
- To swap the picture under a steady blur, chain `Modifier.amCrossfade(key)` after it: the new picture fades in under the scrim, which stays put.

## Catalog pages

`demo/.../catalog/pages/overlays/`, registered in `OverlaysPages.entries` in this order (ids): `sheet`, `alert`, `actionsOverlay`, `spotlight`, `backdropBlur`, `pausedBackdrop`. Every overlay demo presents its window straight from its own button or hold, over the whole screen.

| Page | Preview | Loaded |
|---|---|---|
| Sheet | "Show sheet" (full width): a full-height "Edit profile" sheet, a "Display name" `AMTextField` in a padded card at the top and Save at the bottom; the form carries `amDiscardGuard` while the draft differs from the saved name; Save with no name keeps the sheet up and posts a Warning banner ("Add a display name") from the page, which comes down over the sheet | "Notification settings": a full-height "Notifications" sheet with two cards at `AMElevation.None`, switch rows (Mentions, Replies, New followers) and value rows (Quiet hours, Sound, Badge) |
| Alert | "Show alert" (full width): "Publish draft?" with Not now and Publish | a "Harbor survey" card over a destructive "Delete project" button: "Delete project?", Keep and Delete (destructive), which marks the project "Deleted" |
| Actions overlay | "Show actions" (full width): Message, Mail, Copy link / Cast, Save, Print | a 320 dp photo over "Photo actions": Favorite (active), Add to album / Crop, Adjust, Delete (destructive) |
| Spotlight | a card of three articles; holding one brings it up with Share and Delete tiles; one `AMSpotlightState` gives the holds and the spotlight the same actions, so they're TalkBack actions on each row too | a card of three days; holding one brings up its plans under the day's name, with Add plan and Open day |
| Backdrop blur | a 140 dp photo beside the same art blurred in an `AMBackdrop` | a 280 dp `AMBackdrop` blurred by `AMBackdropBlur`, a 140 dp cover and art-text title and artist over it |
| Paused backdrop | a 180 dp photo with `amPausedBackdrop()` and "Paused" in art text | a 280 dp photo with the `Page` scrim, the photo itself at 160 dp and its title and date over it |

- Tiles in these demos close their overlay; stand-in actions are file-private functions.
- Demo glyphs these pages add: `ic_bottom_sheets`, `ic_feedback`, `ic_apps`, `ic_center_focus_strong`, `ic_blur_on`, `ic_pause_presentation` (entry icons), `ic_mail`, `ic_link`, `ic_download`, `ic_print`, `ic_crop`, `ic_add`. They also use `ic_chat_bubble`, `ic_cast`, `ic_favorite`, `ic_library_add`, `ic_tune`, `ic_delete`, `ic_share`, `ic_calendar_today`.
- The Action grid page (`Actions.md`) opens its full menu in `AMActionsOverlay` from a full-width "Show menu" button, and the Floating action button page opens its scenes' menus through `AMFloatingActions`.
