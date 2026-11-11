---
scope: Presented overlays and their shared rules — sheets and the discard guard, alerts, the actions overlay, the spotlight and holds, backdrop blur and the paused backdrop; overlay windows, blur-behind and its dim fallback, back order, and the overlay-up registry — the library's overlays/ folder and the Overlays catalog pages
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/overlays/**, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/internal/OverlayRegistry.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/overlays/**]
keywords: [AMSheet, AMSheetHeight, AMSheetSurface, Full, Content, Primary, Page, Frosted, ModalBottomSheet, rememberBottomSheetState, confirmValueChange, ModalBottomSheetProperties, shouldDismissOnBackPress, transparent scrim, no dimming, drag handle, ExpandedShape, sheetChrome, requireOffset, sheet shadow, sheet hairline, GlassNode, frosted sheet, screenToLocal, Modifier.amDiscardGuard, AMDiscardGuardState, LocalAMDiscardGuard, DiscardBackHandler, OnBackInvokedCallback, keyboard back, Discard changes, Keep editing, AMAlert, AMAlertItem, AMAlertAction, AlertDialog, destructive, cancel, AMActionsOverlay, AMOverlayWindow, Dialog window, DialogWindowProvider, FLAG_BLUR_BEHIND, blurBehindRadius, FLAG_DIM_BEHIND, setDimAmount, isCrossWindowBlurEnabled, addCrossWindowBlurEnabledListener, dim fallback, ProvideOverlayWindowLocals, DismissCatcher, Close, AMSpotlight, item form, SpotlightLayout, spotlight elevation, flat spotlight card, Modifier.amHoldable, long press, longPressTimeoutMillis, touch slop, HapticFeedbackType.LongPress, CustomAccessibilityAction, onLongClick, AMBackdropBlur, FrostedSlice, Modifier.amPausedBackdrop, AMPausedScrim, Modal, page wash, Modifier.blur, BlurredEdgeTreatment, back order, predictive back, ReportOverlayUp, AMOverlayRegistry, LocalAMOverlayRegistry, AMScene, sheet toast host, AMToastHost, LocalAMOnPrimarySurface, controlOutline, 34 dp, sigma, FallbackDimLight, FallbackDimDark, PageWashAlpha, OverlaysPages]
updated: 2026-11-10
---

# Overlays

What presents over a screen. Each overlay is its own window, the Android way: `AMSheet` on Material's `ModalBottomSheet` (the floating surface over a transparent scrim, with the discard guard, `Modifier.amDiscardGuard`), `AMAlert` on Material's `AlertDialog`, and `AMActionsOverlay` (the remote's grid) and `AMSpotlight` (with `Modifier.amHoldable`) in a full-screen dialog window with the platform's blur-behind. Two non-window pieces blur pictures in place: `AMBackdropBlur` and `Modifier.amPausedBackdrop`. Every window reports to the overlay-up registry, and back takes the topmost window first.

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `overlays/OverlayWindow.kt` | internal `AMOverlayWindow` (the blurred window), `ProvideOverlayWindowLocals`, `OverlayCloseLabel`; private `BlurBehind`, `rememberCrossWindowBlurEnabled`, `DismissCatcher`, `InputBlocker` |
| `overlays/ActionsOverlay.kt` | `AMActionsOverlay` |
| `overlays/Spotlight.kt` | `AMSpotlight` (visible and item forms); private `SpotlightLayout`, `LastItem` |
| `overlays/Holdable.kt` | `Modifier.amHoldable`; private `HoldableElement`, `HoldableNode`, `awaitLetGo` |
| `overlays/Sheet.kt` | `AMSheetHeight`, `AMSheetSurface`, `AMSheet`; private `SheetTitle` |
| `overlays/SheetChrome.kt` | internal `Modifier.sheetChrome`; private `sheetOffset`, `GlassElement`, `GlassNode` |
| `overlays/DiscardGuard.kt` | `Modifier.amDiscardGuard`; internal `AMDiscardGuardState`, `LocalAMDiscardGuard`, `DiscardBackHandler`, the prompt's words; private `DiscardGuardNode` |
| `overlays/Alert.kt` | `AMAlertAction`, `AMAlertItem`, `AMAlert`; private `AlertButton` |
| `overlays/BackdropBlur.kt` | `AMBackdropBlur`; private `BackdropBlurNode` |
| `overlays/PausedBackdrop.kt` | `AMPausedScrim`, `Modifier.amPausedBackdrop`; private `PausedScrimNode` |
| `internal/OverlayRegistry.kt` | `AMOverlayRegistry`, `LocalAMOverlayRegistry`, `ReportOverlayUp` |

- Pieces from other areas it builds on: `Style.ModalOverlay` (the blur, the dark dim, the fallback dims and the page wash), `Style.Frosted`, `Style.Shadow`, `ShapeShadows`, `drawInsetOutline`, `Modifier.amElevation`, `Modifier.amHairline`, `ProvideAMSurfaceStyle`, `LocalAMBackdrop` / `AMBackdropSource`, `FrostedSlice` and `frostedSampleRegion` (`Foundations.md`); `LocalAMEnabled` (`Actions.md`); `Modifier.amCard` (`Layout.md`); `AMSectionHeader`; `LocalAMOnPrimarySurface` (`Hairline.kt`); `AMToastHost` (`Feedback.md`). The scene root that owns the registry, `AMScene`, is in `Navigation.md`.
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

- **The window.** A Compose `Dialog` with `DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)`: full screen, edge to edge. Its `Window` comes from `(LocalView.current.parent as DialogWindowProvider).window`. Its status and navigation bar icons are dark in light mode and light in dark mode (`WindowCompat.getInsetsController`).
- **Blur-behind.** Always `FLAG_DIM_BEHIND`. With cross-window blur on: `FLAG_BLUR_BEHIND`, `blurBehindRadius` = `AMTheme.Overlay.blurRadius` (34 dp, Android's blur radius for a Gaussian sigma of about 20) in px, dim 0 in light mode and `AMTheme.Overlay.darkDimAlpha` (0.4) in dark mode. With it off: no blur flag, dim 0.32 in light mode (Material's scrim strength) and 0.6 in dark mode (the platform's dialog dim), `Style.ModalOverlay.FallbackDimLight` / `FallbackDimDark`. `rememberCrossWindowBlurEnabled` reads `WindowManager.isCrossWindowBlurEnabled` and follows `addCrossWindowBlurEnabledListener` (battery saver flips it live), removing the listener on dispose.
- **Motion.** `visible` drives an `Animatable` from 0 to 1 with `motionScheme.defaultEffectsSpec()` and back with `fastEffectsSpec()`; the window stays composed until it reaches 0. The content's alpha, the dim amount and the blur radius all follow it (the radius updated on the window's attributes as it changes, the platform's way to fade a blur). Remove animations makes it instant. While fading out, an `InputBlocker` on top swallows every touch and dismissal is ignored.
- **Dismissal.** The `Dialog`'s `onDismissRequest` (back, predictive back) and a full-screen `DismissCatcher` behind the content (`detectTapGestures`, so a tap anywhere the content takes no input, between tiles included, dismisses) call `onDismissRequest`. For TalkBack the catcher is one button, "Close", with `traversalIndex = 1f` so it's read after the content.
- **A fresh context** (`ProvideOverlayWindowLocals`, also used by the sheet's content): `LocalAMBackdrop` null (a window can't sample the screen behind it, so frosted surfaces inside draw solid), `LocalAMEnabled` true, `LocalAMOnPrimarySurface` false (no sheet surface under the controls; a sheet sets its own inside), `LocalContentColor` `onSurface`, and `ProvideAMSurfaceStyle(Elevated)`, so cards and tiles float whatever surface style the presenting screen had.

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
AMSpotlight(held, onDismissRequest = { held = null }, actions = { article -> AMActionGridRow(…) }) { article -> ArticleRow(article) }   // a hold
```

- **Visible form**: `AMSpotlight(visible, onDismissRequest, modifier, title: String? = null, actions: slot? = null, elevation = AMElevation.Low, content)`, for pickers and anything driven by a flag.
- **Item form**: `AMSpotlight<T : Any>(item: T?, onDismissRequest, modifier, title: (T) -> String? = { null }, actions: ((T) slot)? = null, elevation = AMElevation.Low, content: (T) slot)`, up while `item` isn't null. It delegates to the visible form, keeping the last non-null item (`LastItem`) so the card fades out intact.
- **Layout** (`SpotlightLayout`), inside the blurred window: a `Column` filling the window, padded by `WindowInsets.safeDrawing` (system bars and the keyboard) and `spacing.xxl` at top and bottom, its children `spacing.md` apart and pushed to the bottom: the optional `title` as an `AMSectionHeader` at the `spacing.lg` gutter; the card; the optional `actions` (one `AMActionGridRow`) at the same gutter, so the tiles land where the remote's do.
- **The card** is `Modifier.amCard(elevation = elevation)` (always solid, through the window's fresh context) around a `verticalScroll`, centered in a full-width box with `weight(1f, fill = false)`: it hugs its content's width (a row on the row recipe fills the gutters, a picker sits centered), takes its own height, and scrolls when it's taller than the room above the tiles. A card the content brings nests and draws nothing. The empty space above stays the catcher's, so a tap there dismisses.
- **Its shadow** (`elevation`): `Low` by default; an app whose cards are flat passes `AMElevation.None`, and the card in focus keeps its fill, corners and dark-mode hairline but casts nothing, like the cards it came from. The tiles keep their shadow, as the remote's do.

## `Modifier.amHoldable` (`Holdable.kt`)

`Modifier.amHoldable(onHold, enabled = true, customActions: List<CustomAccessibilityAction> = emptyList())`: press and hold, the spotlight's trigger.

- **The gesture** (`HoldableNode`, a `DelegatingNode` over a `SuspendingPointerInputModifierNode`): it watches each touch in the **Initial** pass, so it sees the touch before the item's own click and a scrolling parent's drag, whatever the modifier order, without consuming anything until the hold. After the first down it waits `viewConfiguration.longPressTimeoutMillis` (`withTimeoutOrNull`) for the touch to let go (`awaitLetGo`: lifted, moved past `touchSlop`, or consumed by someone else). If it outlasts the timeout, it performs `HapticFeedbackType.LongPress` (from `LocalHapticFeedback`), calls `onHold` while the finger is still down, then consumes the rest of that touch, so the item's click never fires (its ripple shows while held, then cancels).
- So a tap still does the item's main thing, and a vertical drag that starts on the item still scrolls.
- **TalkBack** (`SemanticsModifierNode`): an `onLongClick` action that calls `onHold` (double-tap and hold), and `customActions` set on the item, so the spotlight's actions are there directly.
- `enabled = false` ignores touches and drops both semantics.

## `AMSheet` (`Sheet.kt`, `SheetChrome.kt`)

```kotlin
AMSheet(editing, onDismissRequest = { editing = false }, title = "Edit profile") { ProfileForm() }
AMSheet(showsSettings, onDismissRequest = { showsSettings = false }, title = "Notifications", height = AMSheetHeight.Full, surface = AMSheetSurface.Page) { … }
```

`AMSheet(visible, onDismissRequest, modifier, title: String? = null, height = Full, surface = Primary, content: ColumnScope)`.

- **Material's sheet.** `ModalBottomSheet` with a `rememberBottomSheetState(Hidden, setOf(Hidden, Expanded), confirm)` (no half-expanded stop), `BottomSheetDefaults.ExpandedShape`, `BottomSheetDefaults.DragHandle()`, `tonalElevation = 0.dp`, `contentColor = onSurface`, **`scrimColor = Color.Transparent`** (no dimming; the scrim still takes a tap outside), and Material's content window insets (navigation bar and keyboard). Drag, fling, tap outside, back and predictive back are Material's.
- **Presence.** `composed` starts false; when `visible` turns true it becomes true and Material's sheet slides itself in on entering (reopened mid-hide, the effect calls `show()`). When `visible` turns false from code, the guard's `closing` is set (never asks) and the effect `hide()`s, then removes it. When the user closes it, Material calls back after its own hide: `composed = false`, then `onDismissRequest`.
- **`AMSheetHeight`**: `Full` gives the content column `fillMaxHeight()`, so content can take the room with a weight; `Content` hugs the content.
- **`AMSheetSurface`**: `Primary` (`Surface.primary`) and `Page` (`Surface.page`, for a sheet holding cards) as Material's container color; `Frosted` makes the container transparent and lets the chrome draw glass.
- **Title** (`SheetTitle`): `titleLarge`, start-aligned, full width, `spacing.lg` at the sides and `spacing.md` below, heading semantics, under the drag handle and above the content in one column.
- **Content context**: `LocalAMDiscardGuard` (the sheet's own guard), then `ProvideOverlayWindowLocals`, so what the sheet holds is solid, enabled and elevated whatever the screen set; cards in a sheet float as on a page. Then `LocalAMOnPrimarySurface` = `surface != Page`: on a primary or frosted sheet a filled `AMButton` takes the brighter `AMTheme.controlOutline` in dark mode, as inside a black card (the sheet is a window, so the button can't find it in the layout tree; `Foundations.md`); on a page-colored sheet it keeps the selection ring, as on a page.
- **Its own toast host**: the title and content column sit in an `AMToastHost` sized like the column (full width, and full height for `Full`), so an `AMToast` posted inside the sheet shows at the sheet's bottom, over its content, above the navigation bar and the keyboard (`Feedback.md`). The scene's toasts stay the scene's.

### The chrome — internal `Modifier.sheetChrome(sheetState, shape, frosted, backdrop)`

Material passes its sheet surface no shadow, and the only slot outside the sheet's clip is its `modifier`, which comes first in the chain, ahead of the drag offset and the predictive back scaling. So the chrome is `offset { +sheetOffset }` → the chrome → `offset { −sheetOffset }`: the two offsets cancel for the sheet itself, and the chrome draws at the sheet's current position. `sheetOffset` reads `sheetState.requireOffset()` in placement (far below the screen before it's known), so only placement reruns while the sheet moves.

- **Solid** (`Primary`, `Page`): `Modifier.amElevation(Medium, shape)` (the shadow under the sheet) then `Modifier.amHairline(shape)` (the selection-ring hairline over its edge, both modes).
- **Frosted** (`GlassNode`): the sheet is a window of its own, so it finds its slice of the presenting screen's `AMBackdrop` art by screen position (`art.screenToLocal(sheet.localToScreen(Offset.Zero))`) instead of the layout tree, cuts it with `frostedSampleRegion` (2× the blur of margin), records it into its own layer under `BlurEffect(Frosted.blurRadius, Clamp)`, and draws: the `Medium` shadow clipped outside the shape, the blurred slice clipped to it, `Frosted.fill`, the sheet, the frosted edge. It resamples whenever its screen position changes. With no backdrop, or nothing placed or overlapping yet, it draws the solid sheet (shadow, `Surface.primary`, the sheet, the hairline), never unblurred translucency.
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
- **`AMAlert(item: AMAlertItem?, onDismissRequest, modifier)`**: Material's `AlertDialog` while `item` isn't null, in its native look, motion and dim: title, optional message as its text, `TextButton`s with the cancel as the dismiss button before the confirm. A destructive label takes `AMSemantic.Destructive` through `textButtonColors(contentColor)`.
- A button runs its `onClick`, then `onDismissRequest`. Back and a tap outside run `cancel.onClick` when there is a cancel, then `onDismissRequest`; dismissing never runs `confirm`, so it never runs a destructive action.

## `AMBackdropBlur` (`BackdropBlur.kt`)

`AMBackdropBlur(modifier, radius = AMTheme.Overlay.blurRadius)` (34 dp): a pure Gaussian blur, no tint, of the art behind an area. A `Spacer` sized by `modifier` whose `BackdropBlurNode` draws the nearest `AMBackdrop`'s art under it through the foundations' `FrostedSlice` (the slice plus 2× the blur of margin, clamped at the art's edge, so it never darkens toward transparency), clipped to its bounds, then its content. It tracks its offset in the art and redraws as it moves; the art's layer is referenced, so an arriving photo shows without re-recording. Outside an `AMBackdrop` it draws nothing. For a now-playing background, a strip over art, or any region that blurs part of a picture; clip it for corners.

## `Modifier.amPausedBackdrop` (`PausedBackdrop.kt`)

`Modifier.amPausedBackdrop(scrim = AMPausedScrim.Modal, blurRadius = AMTheme.Overlay.blurRadius)` (34 dp): stills a picture into a backdrop that other content stacks above.

- The chain: `PausedScrimNode` (draws the content, then the scrim), `clearAndSetSemantics {}` (decoration, skipped by screen readers), a pointer input that consumes every event in the Initial pass (nothing inside reacts), then Compose's `Modifier.blur(blurRadius, BlurredEdgeTreatment.Rectangle)` (the content's own layer blurred, clipped and clamped at its edges).
- **`AMPausedScrim`**: `Modal`, nothing in light mode and black at `darkDimAlpha` in dark mode, the modal backdrop's look; `Page`, the page color at 0.55 in both modes (`Style.ModalOverlay.PageWashAlpha`), so the picture's color bleeds through while the page reads as itself (a photo's detail behind the photo).
- To swap the picture under a steady blur, chain `Modifier.amCrossfade(key)` after it: the new picture fades in under the scrim, which stays put.

## Catalog pages

`demo/.../catalog/pages/overlays/`, registered in `OverlaysPages.entries` in this order (ids): `sheet`, `alert`, `actionsOverlay`, `spotlight`, `backdropBlur`, `pausedBackdrop`. Every overlay demo presents its window straight from its own button or hold, over the whole screen.

| Page | Preview | Loaded |
|---|---|---|
| Sheet | "Show sheet" (full width): a full-height "Edit profile" sheet, a "Display name" `AMTextField` in a padded card at the top and Save at the bottom; the form carries `amDiscardGuard` while the draft differs from the saved name | "Notification settings": a full-height, page-colored "Notifications" sheet with two cards at `AMElevation.None`, switch rows (Mentions, Replies, New followers) and value rows (Quiet hours, Sound, Badge) |
| Alert | "Show alert" (full width): "Publish draft?" with Not now and Publish | a "Harbor survey" card over a destructive "Delete project" button: "Delete project?", Keep and Delete (destructive), which marks the project "Deleted" |
| Actions overlay | "Show actions" (full width): Message, Mail, Copy link / Cast, Save, Print | a 320 dp photo over "Photo actions": Favorite (active), Add to album / Crop, Adjust, Delete (destructive) |
| Spotlight | a card of three articles; holding one brings it up with Share and Delete tiles (and as custom actions on the row) | a card of three days; holding one brings up its plans under the day's name, with Add plan and Open day |
| Backdrop blur | a 140 dp photo beside the same art blurred in an `AMBackdrop` | a 280 dp `AMBackdrop` blurred by `AMBackdropBlur`, a 140 dp cover and art-text title and artist over it |
| Paused backdrop | a 180 dp photo with `amPausedBackdrop()` and "Paused" in art text | a 280 dp photo with the `Page` scrim, the photo itself at 160 dp and its title and date over it |

- Tiles in these demos close their overlay; stand-in actions are file-private functions.
- Demo glyphs these pages add: `ic_bottom_sheets`, `ic_feedback`, `ic_apps`, `ic_center_focus_strong`, `ic_blur_on`, `ic_pause_presentation` (entry icons), `ic_mail`, `ic_link`, `ic_download`, `ic_print`, `ic_crop`, `ic_add`. They also use `ic_chat_bubble`, `ic_cast`, `ic_favorite`, `ic_library_add`, `ic_tune`, `ic_delete`, `ic_share`, `ic_calendar_today`.
- The Action grid page (`Actions.md`) opens its full menu in `AMActionsOverlay` from a full-width "Show menu" button, and the Floating action button page opens its scenes' menus through `AMFloatingActions`.
