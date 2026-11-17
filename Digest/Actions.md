---
scope: Buttons and button styles, the button pair, the floating action button and its hub, icon, card and action-grid buttons, the action containers, and how a container disables the controls inside it — the tappable action primitives
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/actions/**, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/foundations/Enabled.kt, androidmtrls/src/main/res/drawable/am_ic_add.xml, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/ActionGridGeometryTest.kt, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/ButtonPairTest.kt, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/ButtonStyleTest.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/actions/**]
keywords: [AMButton, AMButtonStyle, ButtonStyleTest, Primary, Secondary, Ghost, LocalAMButtonStyle, TextButton, medium button, MediumContainerHeight, contentPaddingFor, destructive, ghost on art, artInkHolding, amControlSurface, controlOutline, LocalMinimumInteractiveComponentSize, 48 dp, AMButtonPair, buttonPairSplits, buttonPairHalf, ButtonPairTest, split or stack, AMIconButton, AMIconButtonSize, IconToggleButton, text form, TooltipBox, PlainTooltip, BadgedBox, badge, AMCardButton, activeTint, tabular figures, AMCardActionRow, more_vert, AMActionGrid, AMActionGridRow, ActionTileRow, actionGridRowBottom, ActionGridGeometryTest, rowBottom, AMActionGridButton, tileHeight, shapes.large, spoken name, selection ring, AMFormActions, LocalAMEnabled, resolveEnabled, ProvideAMEnabled, ProvideOverlayWindowLocals, disabled, AMFloatingActionButton, AMFloatingActionButtonState, rememberAMFloatingActionButtonState, floating action button, lift, long press, drag, systemGestureExclusion, absoluteOffset, FloatingActionButtonDiameter, AMFloatingActions, amFloatingActions, floatingActionButtonTop, consumeWindowInsets, hub, remote, LocalAMOverlayRegistry, shapes.medium, pill, isEmbedded, the tint, surfaceLook, surfaceFill, LocalAMSurfaceFill, always solid, alwaysOutlined, ActionsPages, ActionsPostCard, CatalogFrostedStage]
updated: 2026-11-17
---

# Actions

The tappable action primitives: `AMButton` and its sealed `AMButtonStyle` (Primary and Secondary as floating surfaces on Material's `Button`, Ghost as Material's `TextButton`), `AMButtonPair` (two page buttons split or stacked), `AMIconButton` (Material's `IconButton`, or `IconToggleButton` in its toggle form, with a long-press tooltip and an optional badge), `AMCardButton` and `AMCardActionRow`, the remote's grid (`AMActionGrid`, `AMActionGridRow`, `AMActionGridButton`), `AMFormActions`, `AMFloatingActionButton`, and the scene's remote, `AMFloatingActions`, which shows the button over an `AMScene` and opens the actions overlay from it. Every control here honors `LocalAMEnabled`, the internal local through which a container disables its children. Every AM button is a rounded rectangle in the theme's medium shape (12 dp), never a pill; the remote's tiles take the large shape, the floating action button is a circle, and glyph-only buttons (icon buttons, a card button with no count) keep Material's circular ripple.

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `actions/Buttons.kt` | `AMButtonStyle` (`Primary`, `Secondary`, `Ghost`; equality unit-tested in `ButtonStyleTest`), `LocalAMButtonStyle`, `AMButton` (string and slot forms), `AMButtonPair`; internal `buttonPairHalf`, `buttonPairSplits` (unit-tested in `ButtonPairTest`); private `ButtonBody`, `SurfaceButton` (wearing internal `Modifier.amControlSurface`, `foundations/ControlSurface.kt`, `Foundations.md`), `ghostLabelColor` |
| `actions/IconButton.kt` | `AMIconButtonSize`, `AMIconButton` (tap, text and toggle forms); private `IconButtonTooltip`, `IconButtonGlyph`, `textStyle` |
| `actions/CardButton.kt` | `AMCardButton` |
| `actions/CardActionRow.kt` | `AMCardActionRow` |
| `actions/ActionGrid.kt` | `AMActionGrid`, `AMActionGridRow` (two-, three- and four-tile overloads); internal `ActionTileRow` (every row's layout, the spotlight's held tiles too), `actionGridRowBottom` (behind `AMTheme.ActionGrid.rowBottom`, unit-tested in `ActionGridGeometryTest`) |
| `actions/ActionGridButton.kt` | `AMActionGridButton` (string and slot forms) |
| `actions/FormActions.kt` | `AMFormActions` |
| `actions/FloatingActionButton.kt` | `AMFloatingActionButtonState`, `rememberAMFloatingActionButtonState`, `AMFloatingActionButton`; internal `FloatingActionButtonDiameter` (56 dp) |
| `actions/FloatingActions.kt` | `AMFloatingActions` (the hub), `WindowInsets.amFloatingActions` (the button's clearance for scrolling content); internal `floatingActionButtonTop()` (which `AMScene` reads to lift its toasts over the button); private `ShowActionsLabel` |
| `foundations/Enabled.kt` | internal `LocalAMEnabled`, `resolveEnabled`, `ProvideAMEnabled` |

- Glyph: `androidmtrls/src/main/res/drawable/am_ic_add.xml` (the floating action button's +).
- Tests: `ButtonStyleTest`, `ButtonPairTest`, `ActionGridGeometryTest` in `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/`.
- Pieces from `foundations/` the area builds on (`Foundations.md`): the internal `Modifier.amElevatedSurface` and `ElevatedSurfaceDrawer` (`Hairline.kt`), `Modifier.amHairline`, `AMElevation`, the app's surface setting (internal `LocalAMSurfaceFill`, `SurfaceFill.kt`), `AMSemantic`, `AMTheme.ActionGrid` (`tileHeight`, `sidePadding`, `rowBottom`, `rowTop`), `AMTheme.selectionRingWidth`, `AMTheme.secondaryText`, internal `surfaceAccent()` (`Theme.kt`, the accent on frost), `surfaceLook` and `isEmbedded` (`SurfaceLook.kt`, the look a button draws and the card-tabs-or-sheet test behind its tint and its dark outline), `LocalAMBackdrop` (`internal/BackdropSource.kt`). From `datadisplay/ArtTone.kt` (`DataDisplay.md`): `LocalAMArtTone`, `artInk`, `artInkHolding`, for the ghost label on art. For the hub: `LocalAMOverlayRegistry` (`internal/OverlayRegistry.kt`), `AMActionsOverlay` (`Overlays.md`) and `AMScene` (`Navigation.md`).
- Catalog pages: `demo/.../catalog/pages/actions/` (below).

## Disabling — `LocalAMEnabled` (`foundations/Enabled.kt`)

- Internal `LocalAMEnabled` (a `compositionLocalOf`, `true` at the root) says whether the containers around a control let it be enabled.
- **Every AM control** passes its own `enabled` through `resolveEnabled(enabled)` (`enabled && LocalAMEnabled.current`) and hands the result to the Material component it wraps. Controls in other areas use the same helper.
- **A container** wraps its content in `ProvideAMEnabled(enabled) { }`, which provides `resolveEnabled(enabled)`, so containers combine: inside a disabled container nothing re-enables. `AMFormActions` is the only library container that provides it.
- **An overlay window starts enabled**: its fresh context (`ProvideOverlayWindowLocals`, `Overlays.md`) provides `LocalAMEnabled` `true`, so a sheet, an alert, the remote or the spotlight opened from inside a disabled container is live.
- A disabled control keeps its place, size and fill: its content greys to Material's disabled color (`onSurface` at 0.38), and a floating surface drops its shadow, as Material's elevated button does.

## Buttons — `AMButton`, `AMButtonStyle` (`Buttons.kt`)

```kotlin
AMButton("Continue", onClick = ::next, modifier = Modifier.fillMaxWidth())                 // Primary from LocalAMButtonStyle
AMButton("Shuffle", onClick = ::shuffle, icon = painterResource(R.drawable.ic_shuffle), style = AMButtonStyle.Secondary())
AMButton("Delete playlist", onClick = ::delete, style = AMButtonStyle.Secondary(), destructive = true)
AMButton("Cancel", onClick = ::cancel, style = AMButtonStyle.Ghost)
```

- **Forms.** String: `AMButton(text, onClick, modifier, icon: Painter? = null, contentDescription: String? = null, style, destructive, enabled)`, which delegates to the slot form with `Text(text)`. Slot: `AMButton(onClick, modifier, icon, contentDescription, style, destructive, enabled, content: RowScope)`, through one private `ButtonBody`, which picks the Material button and its size. `icon` sits before the content at the style's glyph size and spacing (`iconSizeFor` / `iconSpacingFor` the medium height for Primary and Secondary, `ButtonDefaults.IconSize` / `IconSpacing` for Ghost) and switches the padding to Material's leading-glyph padding (`contentPaddingFor(…, hasStartIcon = true)`, `TextButtonWithIconContentPadding`).
- **Spoken name.** `contentDescription` is what TalkBack reads in place of what the button shows: the glyph and content sit in a `Row` with `clearAndSetSemantics { contentDescription }`, which the button merges, so it shows "Cancel · 62%" and reads "Cancel download, 62%". Null reads the label.
- **`AMButtonStyle`** is a sealed interface: `Primary(elevation = Low)` and `Secondary(elevation = Low)` are `@Immutable` classes with value equality over `elevation` (a Primary never equals a Secondary; `toString()` reads `Primary(elevation=Low)`; `ButtonStyleTest`); `Ghost` is a `data object`. There's no other way to style a button, and no style picks a fill: the filled styles take the app's surface setting (`AMTheme.surfaceFill`, `Foundations.md`).
- **`LocalAMButtonStyle`** (public, `staticCompositionLocalOf`, `Primary()` at the root) is `style`'s default, so a container styles the buttons in its slots; a caller passes `style` only to override.
- **Primary and Secondary are Material's medium button**: `Button` with `Modifier.heightIn(min = ButtonDefaults.MediumContainerHeight)` (56 dp), `ButtonDefaults.contentPaddingFor(MediumContainerHeight, hasStartIcon)`, the label in `ButtonDefaults.textStyleFor(MediumContainerHeight)` (through `ProvideTextStyle`, so the slot form gets it too) and Material's ripple, in **`MaterialTheme.shapes.medium`** (12 dp), passed as the button's `shape` and the surface's, so the surface, the clip and the ripple are one rounded rectangle, never Material's stadium. They run taller than Material's standard 40 dp button on purpose, as rows run taller than Material's list items, so a filled button stands as tall as the rows around it. The button sits over a transparent container with no Material elevation, wearing internal `Modifier.amControlSurface(shape, elevation)` (`foundations/ControlSurface.kt`; its private `ControlSurfaceNode` owns an `ElevatedSurfaceDrawer`): the shape's two-layer shadow at `elevation`, then the look in the app's surface setting, resolved while it draws, so a change of setting or mode redraws it (the node, `lookOf` and the embedded test are in `Foundations.md`). The button is embedded (`isEmbedded()`) inside a card, flat or not, on tabs or in a library container (`LiesOn.Surface`, or `LiesOn.EmbeddedSurface` on a sheet), or on an `AMSheet` (`LiesOn.Sheet`). **In a solid app** it's the surface fill and in dark mode a hairline: `AMTheme.controlOutline` where it's embedded, the selection ring on the page. **In a frosted app** it's frost over the nearest `AMBackdrop`'s art (frost over the page color with no art to sample), redrawing as it moves over the art; embedded, the tint (`AMTheme.Frosted.embeddedFill`) with the frosted edge, never sampling. Label and glyph are `onSurface`; Primary keeps the medium label style's own medium weight, Secondary sets `FontWeight.Normal`.
- **The 48 dp target sits outside the surface.** Material's button reserves its touch target inside its own modifier, so a surface drawn around it would grow to the target. `SurfaceButton` applies `minimumInteractiveComponentSize()` itself, then the surface, then lays the `Button` out with `LocalMinimumInteractiveComponentSize` at 0 (restored for the label), so the surface always hugs the button and taps land across at least 48 dp. A `Box` with `propagateMinConstraints` passes a stretched width (a weight, `fillMaxWidth`) down to the button.
- **Ghost** is Material's `TextButton` with `shape = MaterialTheme.shapes.medium` (so its ripple is the same rounded rectangle): no surface, label in the scheme's `primary`, Material's own padding and target. **On art** (an art tone around it, `LocalAMArtTone` non-null) its label is the ink of the art's tone (white on dark art, black on light art: baseline `primary` reads about 3.1:1 on the darkest light art, `Style.ArtInk.LightArtGround`), through `textButtonColors(contentColor)`, so its ripple follows. **Destructive on art** it's `artInkHolding(error, tone)`: the scheme's red moved along its own line just far enough to hold 4.5:1 on that tone (baseline light `#B3261E` → about `#881B14` against `LightArtGround`; baseline dark `#F2B8B5` holds 4.93:1 against `Style.ArtInk.DarkArtCeiling` and stays). Private `ghostLabelColor(destructive)` picks it.
- **`destructive = true`** colors the label `AMSemantic.Destructive` (the scheme's `error`) in every style.
- **Disabled**: the label is `onSurface` at 0.38 and the surface casts no shadow (`AMElevation.None`); the fill stays.
- A button wraps its label, like Material's. Span a row with `fillMaxWidth()` or a weight; two page buttons that share a row go in `AMButtonPair`.

## `AMButtonPair` (`Buttons.kt`)

```kotlin
AMButtonPair(
    { AMButton("Play", onClick = ::play, icon = painterResource(R.drawable.ic_play_arrow_filled)) },
    { AMButton("Remove download", onClick = ::remove, icon = painterResource(R.drawable.ic_delete), style = AMButtonStyle.Secondary(), destructive = true) },
)
```

- `AMButtonPair(first, second, modifier)`, each slot a `@Composable () -> Unit` holding one `AMButton` with no size of its own; the pair fills the width it's given and sizes both.
- **Split or stack**, decided per measure by internal `buttonPairSplits(firstWidth, secondWidth, width, gap)`: each button's one-line width (`maxIntrinsicWidth`) against half the row less the `spacing.md` gap (`buttonPairHalf`, rounding down). Both fit → side by side, each exactly half, equally tall (the taller one's `minIntrinsicHeight` at half as both buttons' minimum height), `first` at the start (`placeRelative`, so it mirrors in right-to-left layouts). Either doesn't → stacked, `first` over `second`, each full width, `spacing.md` apart.
- **Labels never shrink or break to fit**: a longer label ("Remove download" with its glyph, on a 411 dp phone) or a larger font scale stacks the pair; a label wraps only if it outgrows the full width.
- It sets no style and no enabled state: each button keeps its own. The row that closes a form is `AMFormActions`.

## `AMIconButton` (`IconButton.kt`)

```kotlin
AMIconButton(painterResource(R.drawable.ic_pause_filled), "Pause", onClick = ::pause, size = AMIconButtonSize.Large)
AMIconButton(painterResource(R.drawable.ic_repeat), "Repeat one", onClick = ::repeatOne, badge = { Badge { Text("1") } })
AMIconButton(painterResource(if (shuffles) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle), "Shuffle", checked = shuffles, onCheckedChange = { shuffles = it })
```

- **Tap form**: `AMIconButton(icon, contentDescription: String?, onClick, modifier, size = Medium, badge: (BoxScope slot)? = null, enabled)`: Material's `IconButton` in the surrounding content color, its circular ripple and 48 dp target.
- **Text form**: `AMIconButton(text: String, contentDescription: String?, onClick, modifier, size = Medium, enabled)`, for a short value in an icon button's place (a playback speed, "1.25×"): the same `IconButton`, content color, ripple, 48 dp target and tooltip. The text is one line, never wrapping (`maxLines = 1`, `softWrap = false`), in the size's type (`labelMedium` / `labelLarge` / `titleMedium` for Small / Medium / Large) with tabular figures (`"tnum"`). The button is the glyph form's height (40 dp, 56 dp Large) and at least that wide, wider when the text measures more (`rememberTextMeasurer`, plus 8 dp each side, through `Modifier.size(width, height)` ahead of Material's own size): then its ripple's shape is `shapes.medium` instead of the circle. With a `contentDescription` the button carries it (`semantics`) and the text is cleared from TalkBack (`clearAndSetSemantics`), so it reads "Playback speed, 1.25 times" while showing "1.25×"; the tooltip shows it too. Null reads the text and shows no tooltip.
- **Toggle form**: `AMIconButton(icon, contentDescription: String?, checked, onCheckedChange: (Boolean) -> Unit, modifier, size, badge, enabled)`: Material's `IconToggleButton`, for something the button turns on and off (shuffle, repeat), so TalkBack hears a toggle named by the description ("Shuffle", not "Shuffle on") and its on or off state. The glyph is the content color while off, the accent while on (`IconButtonDefaults.iconToggleButtonColors(checkedContentColor = surfaceAccent())`: `primary`, or on frost (a sheet, or a surface in a frosted app) `AMTheme.Frosted.tint`, which holds 4.5:1 there, `Foundations.md`); the caller still swaps to the `_on` glyph where Material Symbols has one. A control with more states (repeat off, all, one) passes `checked` for any state but off, steps its own state in `onCheckedChange` and names the state in the description. Size, tooltip and badge as in the tap form.
- **`AMIconButtonSize`**: `Small` (20 dp glyph on Material's 40 dp button), `Medium` (24 dp, Material's own), `Large` (32 dp glyph on a 56 dp button, via `Modifier.size`; Small and Medium leave Material's size alone).
- **Tooltip** (`IconButtonTooltip`, every form): with a `contentDescription`, the button sits in Material's `TooltipBox` (`rememberTooltipPositionProvider(TooltipAnchorPosition.Above)`, a `PlainTooltip` of the description, `rememberTooltipState()`), so a long press names the glyph. `modifier` goes on the tooltip box. A null description shows no tooltip, and `modifier` goes on the button.
- **Badge** (tap and toggle forms): a nullable slot handed to Material's `BadgedBox` around the glyph, so the badge sits on the glyph's corner. Callers pass Material's `Badge`.

## `AMCardButton` (`CardButton.kt`)

- `AMCardButton(icon, contentDescription: String, onClick, modifier, count: String? = null, active = false, activeTint: AMSemantic = Info, enabled)`. One form: the content is fixed, a glyph and a count.
- **With a `count`**: Material's `TextButton` in `shapes.medium` (its ripple a rounded rectangle, like every AM button) with `TextButtonContentPadding`, the glyph at `ButtonDefaults.IconSize`, `IconSpacing`, then the count in `labelLarge` with tabular figures (`fontFeatureSettings = "tnum"`), one line. **Without**: Material's `IconButton` with the glyph at the same 18 dp. Both have no surface (the card supplies it), Material's ripple and 48 dp target.
- Color: `AMTheme.secondaryText` at rest (the frosted secondary on frost); `active` takes `activeTint.color` (`Info` = the scheme's primary, or on frost `AMTheme.Frosted.tint`; `Destructive` for a filled heart). TalkBack reads the glyph's description, then the count; the caller names the state ("Liked").

## `AMCardActionRow` (`CardActionRow.kt`)

- `AMCardActionRow(modifier, counts: (RowScope slot)? = null, actions: RowScope slot)`: a full-width `Row`, centered vertically, with the counted actions at the start, a weighted `Spacer`, and the glyph-only actions at the end. No extra spacing: the Material buttons' own padding spaces them.
- The rule it encodes: all of a card's actions on one row under the content, counts at the start, share then ⋮ (`more_vert`) last in `actions`. A card with no counts passes `actions` alone.
- **Placement**: inset the row `spacing.xs` from the card's edges (start, end, bottom), not the card's `spacing.lg`, so the first glyph lines up with the text above (4 + the text button's 12 = 16) and ⋮ with its end.

## The remote's grid — `AMActionGrid`, `AMActionGridRow` (`ActionGrid.kt`)

- **`AMActionGrid(modifier, windowInsets = WindowInsets.navigationBars, content: ColumnScope)`**: a `Column` that fills its space (`fillMaxSize`; with no height bound it wraps its rows), applies `windowInsetsPadding(windowInsets)`, then `spacing.lg` at the sides and `spacing.xxl` at the bottom, and stacks its rows `spacing.md` apart at the bottom (`Arrangement.spacedBy(md, Alignment.Bottom)`). Inside an overlay that already consumed the insets, the grid's own inset is zero; on a page that doesn't reach the screen's bottom, pass `WindowInsets(0)`.
- **The grid's geometry is public** (`AMTheme.ActionGrid`, `Foundations.md`): `sidePadding` (`spacing.lg`), `rowBottom(index)` and `rowTop(index)`, how far above the navigation bar row `index` (0 = the bottom row) has its bottom and top, from internal `actionGridRowBottom(index, tileHeight, bottomPadding, rowGap)` (unit-tested in `ActionGridGeometryTest`). Content that rests where the remote shows it (a controls card just above one row) pads `navigationBarsPadding()` then `bottom = rowBottom(1)`, never re-deriving it from the tokens.
- **`AMActionGridRow`** has exactly three overloads (a held item's tiles in the spotlight share their layout, internal `ActionTileRow`, `Overlays.md`), `(first, second, modifier)`, `(first, second, third, modifier)`, `(first, second, third, fourth, modifier)`, each slot a `@Composable () -> Unit`, and no list form: a row of one or of five doesn't compile. `ActionTileRow` lays them in a full-width `Row` `spacing.md` apart, each slot in a `Box(Modifier.weight(1f), propagateMinConstraints = true)`, so every tile is its column's width.

## `AMActionGridButton` (`ActionGridButton.kt`)

```kotlin
AMActionGridButton(painterResource(R.drawable.ic_feed), "Feed", onClick = ::openFeed, active = current == Feed)
AMActionGridButton(onClick = ::openStorage) { Text("80%", style = MaterialTheme.typography.titleMedium); Text("Storage") }
```

- String: `(icon, title, onClick, modifier, contentDescription: String? = null, active, destructive, elevation = Low, enabled)`, the glyph (Material's 24 dp, no description) over the title (one line, ellipsis). TalkBack reads the title, or `contentDescription` when a short title needs a fuller name (a "Queue" tile reads "Add to queue"): the tile carries the description and the title is hidden (`clearAndSetSemantics {}`). Slot: `(onClick, modifier, active, destructive, elevation, enabled, content: ColumnScope)`. The string form delegates to the slot form.
- **The tile**: a `Column` exactly `AMTheme.ActionGrid.tileHeight` (76 dp) tall at any font scale, in `shapes.large`: when `active`, `amHairline` for the ring (outside the surface in the chain, so it draws over everything the tile draws, its outline included), then `amElevatedSurface(shape, elevation, alwaysOutlined = true)` (shadow at `elevation`, in the app's surface setting: solid with the selection-ring hairline in both modes in a solid app, frosted with the frosted edge in a frosted one; in the remote and in the spotlight always solid, since `AMActionsOverlay` and the spotlight's tile row set their content solid, `Overlays.md`), then the clip and `clickable` (`Role.Button`, Material's ripple inside the shape), then `spacing.sm` side padding. Content is centered, `spacing.xs` apart, in `labelMedium` through `ProvideTextStyle` and `LocalContentColor`.
- **Color**: `onSurface`; `destructive` → `AMSemantic.Destructive`; disabled → `onSurface` at 0.38; `active` and enabled → the scheme's `primary`, as it is (not `Frosted.tint`). In the remote and the spotlight the tile is solid, so its accent and `error` sit on the solid surface, never on frost over a blurred window.
- **`active`** (the current view, the chosen filter): `amHairline(shape, primary, AMTheme.selectionRingWidth)` rings the tile over its outline, the tile casts no shadow, and the semantics say selected. A disabled active tile keeps its ring over greyed content. A disabled tile casts no shadow either.

## `AMFormActions` (`FormActions.kt`)

```kotlin
AMFormActions(destructive = { AMButton("Delete", onClick = ::delete, destructive = true) }) {
    AMButton("Save", onClick = ::save, enabled = title.isNotEmpty())
}
```

- `AMFormActions(modifier, destructive: slot? = null, elevation = Low, enabled = true, primary: slot)`: a full-width `Row`, `spacing.md` apart, the destructive slot first. Each slot sits in a `Box(Modifier.weight(1f), propagateMinConstraints = true)`, so two buttons split the row evenly and Save alone spans it.
- It styles its slots through `LocalAMButtonStyle`: `Secondary(elevation)` for the destructive slot, `Primary(elevation)` for the commit slot, both in the app's surface setting like every filled button (tinted on a sheet in a frosted app). The caller marks Delete `destructive = true`.
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
- **The button**: 56 dp, `CircleShape`, the + (`am_ic_add`) in `onSurface`; `amElevatedSurface(CircleShape, Low, alwaysOutlined = true)`; Material's ripple inside the circle. **It's always solid:** the whole layer sits inside `LocalAMSurfaceFill provides AMSurfaceFill.Solid`, so the button is white in light mode and black in dark, with the selection-ring hairline in both modes and its two-layer shadow, whatever the app's setting; like the remote's tiles (`Overlays.md`), the button that opens the app's main navigation never shows the screen through it.
- **Modifier order** (it matters): `absoluteOffset { state.offset + live drag }` → `systemGestureExclusion()` → `size(56.dp)` → `clickable(interactionSource, indication = null, Role.Button)` → the lift `pointerInput` → `graphicsLayer` scale → surface (with its outline) → clip → `indication(interactionSource, ripple())`. The offset is absolute, so the drag follows the finger in right-to-left layouts. The exclusion keeps the system back gesture off the button wherever it sits, which only matters at the screen's edges. The scale sits inside the pointer input, so drag deltas aren't scaled.
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
- **The button**: an `AMFloatingActionButton` (`state`, `enabled`, `contentDescription`) inside an `AnimatedVisibility` whose `modifier` is the layer (`modifier.fillMaxSize()`). Padding that `modifier`'s bottom rests the button higher, above a bottom toolbar. The + is solid in either setting (above), so it samples none of the art the slot lies on (`Navigation.md`).
- **The menu**: `menu` in `AMActionsOverlay(expanded, onDismissRequest = { onExpandedChange(false) })`, so it's a blurred window over the whole screen, sheets included, and takes back first.
- **Overlays beat the button.** It's visible only while `!expanded` and the nearest `LocalAMOverlayRegistry` reports nothing up (`anyUp`): its own menu, a sheet, an alert, the spotlight and the loading overlay all fade it out (`fadeOut(motionScheme.fastEffectsSpec())`) and back in (`fadeIn(defaultEffectsSpec())`); instant under Remove animations. Hidden, the button leaves the composition; its `state` lives in the hub, so it comes back where it was. Banners and toasts don't report, so the button stays; in a scene the toast sits above it.
- **Views and actions share the remote.** A tile that acts closes the menu (`onExpandedChange(false)`, or the app's own flag) and acts. A view tile closes it, then sets the scene's root inside `amInstantSwitch`, so the view appears at once and the menu fades over it; the current view's tile is `active`. A live control may act without closing it.
- Outside an `AMScene` it still works, but nothing fades it: there's no registry.
- **Clearing the button**: `WindowInsets.amFloatingActions` is what scrolling content keeps at its end so its last item scrolls clear of the button: the navigation bar's bottom, then `floatingActionButtonTop()` (the `spacing.lg` the button rests above the bar and its 56 dp) and one more `spacing.lg`, 88 dp over the bar by default. Bottom only. A lazy list takes it as content padding (`asPaddingValues()`); content that already pads the bar, such as a scaffold's content, consumes that padding first (`consumeWindowInsets(padding).windowInsetsPadding(WindowInsets.amFloatingActions)`), so the bar counts once. `AMScene` lifts its toasts by `floatingActionButtonTop()` (`Navigation.md`).

## Catalog pages

`demo/.../catalog/pages/actions/`, registered in `ActionsPages.entries` in this order (ids): `buttonStyles`, `iconButton`, `cardButton`, `cardActionRow`, `formActions`, `buttonPair`, `floatingActionButton`, `actionGrid`, `actionGridButton`.

| Page | Preview | Loaded |
|---|---|---|
| Button styles | `FullBleed` surface strip: Continue (Primary, full width), Save draft (Secondary, full width), Cancel (Ghost) | a 220 dp `CatalogFrostedStage` (an `AMBackdrop` of `CatalogArt.Mountain` under a nested `AMTheme(surfaceFill = AMSurfaceFill.Frosted)`) with Play and Shuffle (with glyphs) frosted over it, then, `spacing.xl` below in the page's own setting and `spacing.md` apart, Download (disabled) beside Share, Delete playlist (destructive), all Secondary but Play |
| Icon button | `FullBleed` strip: previous, play, next, all Large | a now-playing card: track over artist with the speed (the text form, "1×", read as "Playback speed, 1 times", opening "Playback speed" in `AMMenuSpotlight` with 1×, 1.25×, 1.5× and 2×), the sleep timer (Small, the toggle form, "Sleep timer", `ic_bedtime`) and cast disabled, then shuffle (Small, the toggle form, "Shuffle", swapping `shuffle`/`shuffle_on`), previous, pause and next (Large), repeat (Small) with a "1" badge in `onSurface` |
| Card button | `FullBleed` strip: reply 12, like 48, share | a post card: comments 1.2K, reposted 4 (active), liked 49 (active, `Destructive`, toggles), then share and ⋮ in an `AMCardActionRow` |
| Card action row | a post card: reply 12, like 48, share, ⋮ | two post cards: one with reply, reposted and liked; an announcement with only share and ⋮ |
| Form actions | Delete and Save | a `CatalogFrostedStage` first, padded `spacing.lg`: the edit-task form over the photo, its own card (`AMTextField` "Title", "Write the quarterly update", padded `spacing.lg`, the field tinted on the frosted card) over Delete and Save, frosted on the photo itself; then, `spacing.xl` below in the page's own setting, the same form. Each form keeps its own title, and its Save is disabled while the title is empty |
| Button pair | Play (Primary) and Shuffle (Secondary), each with its glyph, side by side | a `CatalogFrostedStage` first: Play (Primary, `ic_play_arrow_filled`) and Queue (Secondary, `ic_playlist_add`) frosted over the photo, padded `spacing.lg`. Then, `spacing.xl` below, `spacing.md` apart: a card of "The Long Way Round" (`titleMedium`) over "Episode 42 · 38 min · Downloaded" (`bodyMedium`, secondary text) on the row recipe, the same Play and Queue side by side, and Share (Secondary, `ic_share`) with Remove download (Secondary, destructive, `ic_delete`), stacked since Remove download won't fit at half a phone's width |
| Floating action button | `Fill`, 380 dp: a "Notes" `AMScene` (an `AMPageHeader` over "Last action: None yet") whose `AMFloatingActions` opens Compose and Search; a tile closes the menu and names itself in the line | `Fill`, 380 dp: a mail `AMScene` with two roots, Inbox (the landing view) and Archive, each an `AMPageHeader` over a scrolling list of message rows; the remote holds Compose, Search / Inbox, Archive, the current view ringed. A view tile switches the root (`amInstantSwitch`), each list keeps its scroll, and back from Archive returns to Inbox. Above it, `spacing.xl` apart, a 220 dp `CatalogFrostedStage` inset by the page gutter holding a third `AMScene` (transparent, the photo its screen) whose `AMFloatingActions`, solid in the frosted app too, rests at the stage's bottom center and opens Compose and Search; a tile closes the menu |
| Action grid | `Wide`: Compose, Search / Feed, Alerts, Exit | a full-width "Show menu" button opening the full menu in `AMActionsOverlay`: Pin, Duplicate, Share, Archive / Compose, Search, Alerts / Feed, Exit; each tile closes it |
| Action grid button | three tiles: Share, Copy, Pin | filter tiles All (active), Starred, Archived (tapping moves the ring), then a custom "80% / Storage" tile, Move (disabled), Delete (destructive) |

- The Action grid's inline Preview grid passes `windowInsets = WindowInsets(0)`, since on the page it doesn't reach the navigation bar; the menu in the overlay keeps its default.
- **The Floating action button page's scenes** are real `AMScene`s on `Fill` bands: each frame is `fillMaxWidth().height(380.dp).consumeWindowInsets(WindowInsets.safeDrawing)` (private `sceneFrame`), so the band is a screen with no system bars of its own and the button rests `spacing.lg` above its bottom edge; `containerColor = Color.Transparent` lets the band's surface show. Message rows are hand-built on the row recipe, flat on the band, with `WindowInsets.amFloatingActions` less the navigation bar the band doesn't have (`exclude(WindowInsets.navigationBars)`) as the list's content padding, so the last row scrolls clear of the button. The Notes and mail scenes call `CatalogAutoPresent { showsMenu = true }`, so `--es present preview` (or `--es scrollTo loaded --es present loaded`) opens its menu for a screenshot; the photo scene's menu opens from its button. The photo scene's stage consumes `WindowInsets.safeDrawing` too, so its button rests `spacing.lg` above the photo's bottom edge. The menu is a window, so it opens over the whole screen, its tiles at the screen's bottom.
- `ActionsSupport.kt` holds `ActionsPostCard(text, modifier, author, actionRow)`: a card with the author (`titleMedium`) and text (`bodyLarge`) padded `spacing.lg` at the top and sides, then the action row inset `spacing.xs`.
- `CatalogFrostedStage(modifier, art = CatalogArt.Valley, content)` (`catalog/scaffold/Stage.kt`, `Demo.md`) is how a page shows the frosted setting inside the page's own.
- Demo glyphs the pages use (`demo/src/main/res/drawable/`): `ic_smart_button`, `ic_play_circle`, `ic_favorite`, `ic_call_to_action`, `ic_fact_check`, `ic_view_column_2`, `ic_add_circle`, `ic_grid_view`, `ic_grid_view_filled` (entry icons); `ic_play_arrow_filled`, `ic_pause_filled`, `ic_skip_previous_filled`, `ic_skip_next_filled`, `ic_shuffle`, `ic_shuffle_on`, `ic_repeat`, `ic_cast`, `ic_bedtime`, `ic_playlist_add`; `ic_chat_bubble`, `ic_forum`, `ic_favorite_filled`, `ic_share`, `ic_more_vert`; `ic_edit_square`, `ic_search`, `ic_inbox`, `ic_archive`, `ic_feed`, `ic_notifications`, `ic_logout`, `ic_push_pin`, `ic_library_add`, `ic_content_copy`, `ic_filter_list`, `ic_star`, `ic_drive_file_move`, `ic_delete`.

## See also

- `Foundations.md` — the elevated and control surfaces, the embedded test and the tint every filled button and tile wear.
- `Overlays.md` — `AMActionsOverlay`, the overlay windows' fresh context, the spotlight's held tiles.
- `Navigation.md` — `AMScene`'s `floatingActions` slot, its overlay registry and the toasts it lifts over the button.
