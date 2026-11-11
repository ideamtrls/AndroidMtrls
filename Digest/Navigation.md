---
scope: Navigation chrome and the scene root — AMScene (each screen's root: the remote's slot, the scene-wide overlays slot, the overlay registry, the banner and toast hosts, back to the landing view), panels on Navigation 3 (AMPanel, AMPanelChrome) and over art (AMArtPanelChrome), light status bar icons over art (AMLightStatusBarIcons), keeping each root view's place (AMKeepsPlace) and its back-to-landing step, toolbars and their buttons (AMToolbar, AMToolbarButton, solid over art), in-page tabs (AMTabs) and page indicators (AMPageIndicator)
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/navigation/**, androidmtrls/src/main/res/drawable/am_ic_arrow_back.xml, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/KeepsPlaceBackTest.kt, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/LightStatusBarClaimsTest.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/navigation/**]
keywords: [AMPanel, AMPanelChrome, AMArtPanelChrome, panel, push, TopAppBar, pinnedScrollBehavior, overlappedFraction, scrolledContainerColor, arrow_back, Back, actions, share, Scaffold, containerColor, Navigation 3, NavDisplay, NavEntry, entryProvider, back stack, predictive back, art, album, frosted bar, FrostedSlice, LocalAMBackdrop, amArtText, amCrossfade, solid back button, amElevatedSurface, FrostedBar, frostedBarFill, FrostedBarFillNode, frosted scrolled bar, barScrolledState, oneRowBarFill, twoRowBarFill, status bar icons, AMLightStatusBarIcons, light status bar icons, claim, LightStatusBarClaims, APPEARANCE_LIGHT_STATUS_BARS, windowInsetsController, edge-to-edge, AMKeepsPlace, root views, landing view, SaveableStateHolder, rememberSaveableStateHolder, SaveableStateProvider, views you leave, back to landing, KeepsPlaceBackHandler, keepsPlaceBackTarget, NavigationBackHandler, navigationevent-compose, amInstantSwitch, AMScene, scene root, floatingActions, overlays, scene-wide overlays, containerColor, overlay registry, AMOverlayRegistry, LocalAMOverlayRegistry, ToastHost, toast clearance, back order, AMToolbar, title-less toolbar, toolbar without a title, CenterAlignedTopAppBar, titleHorizontalAlignment, subtitle, navigationIcon, AMToolbarButton, ToolbarButton, toolbar button, solid over art, destructive, tooltip, LocalMinimumInteractiveComponentSize, AMTabs, AMTabsScope, tab, SecondaryTabRow, Tab, LeadingIconTab, SecondaryIndicator, tabIndicatorOffset, Crossfade, AMSurfaceFill, frosted tabs, AMPageIndicator, page dots, pager, HorizontalPager, Page x of y, outlineVariant, PanelPage, CatalogPanels, openPanel, ToolbarPage, ToolbarButtonPage, TabsPage, PageIndicatorPage]
updated: 2026-11-10
---

# Navigation

Navigation covers moving between views. `AMScene` is each screen's root: it holds the remote (`AMFloatingActions` in its `floatingActions` slot), the overlay registry its overlays report to, the window's banner and toast hosts, and, in its roots form, the root views, their step back to the landing view and the overlays that belong to the whole scene. Panels are Navigation 3 entries: `AMPanel` builds one under `AMPanelChrome` (Material's top app bar with a back arrow, transparent at rest, the page color once scrolled; inside an `AMBackdrop` its back arrow is solid and it takes the frosted fill once scrolled), and `AMArtPanelChrome` is the bar for a panel over dark art (a solid back button, white art text at rest, the frosted fill once scrolled, light status bar icons over the art). `AMLightStatusBarIcons` is the claim on light status bar icons any view over dark art holds, counted with the art panels' own. `AMKeepsPlace` switches an app's root views, saving and restoring each one, and owns the back-to-landing step; each root's back stack stays in the app's own state. `AMToolbar` is Material's center-aligned top app bar for a screen's actions (titled, or its buttons alone), filled with `AMToolbarButton`s (Material's icon button, solid over art); `AMTabs` switches part of a page under Material's secondary tab row; `AMPageIndicator` shows which page of paged content is current.

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `navigation/Panel.kt` | `AMPanel`, `AMPanelChrome`; internal `PanelBackLabel` ("Back") |
| `navigation/ArtPanelChrome.kt` | `AMArtPanelChrome`; private `ArtBarText` |
| `navigation/StatusBarIcons.kt` | `AMLightStatusBarIcons`; internal `LightStatusBarClaims` (the counter), private `windowClaims`, `lightStatusBarClaims` |
| `navigation/FrostedBar.kt` | internal `FrostedBar`, `barScrolledState`, `oneRowBarFill`, `twoRowBarFill`, `BarScrolledFraction`, `Modifier.frostedBarFill()` (`FrostedBarFillNode`): the frosted fill of a top app bar over art, shared by the art panel's bar, `AMPanelChrome`, `AMToolbar` and `AMPageHeader` |
| `navigation/Scene.kt` | `AMScene` (plain and roots forms) |
| `navigation/KeepsPlace.kt` | `AMKeepsPlace`; internal `KeepsPlaceBackHandler`, `keepsPlaceBackTarget` |
| `navigation/Toolbar.kt` | `AMToolbar` (string, slot and title-less forms), `AMToolbarButton`; internal `ToolbarButton` (the button in a form the caller picks) |
| `navigation/Tabs.kt` | `AMTabs`, `AMTabsScope`; internal `TabEntry` |
| `navigation/PageIndicator.kt` | `AMPageIndicator`; private `PageDot` |

- Glyph: `androidmtrls/src/main/res/drawable/am_ic_arrow_back.xml` (Material Symbols `arrow_back`, auto-mirrored).
- Tests: `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/KeepsPlaceBackTest.kt` (the back-to-landing decision), `LightStatusBarClaimsTest.kt` (the claim counting: overlapping claims, out-of-order ends, what the last one puts back).
- Dependency: the library uses `navigationevent-compose` (`implementation`) for the back handler; it exposes no Navigation 3 types, so apps bring Navigation 3 themselves.
- Pieces from `foundations/` and `internal/` the area builds on (`Foundations.md`): `AMTheme.Surface.page`, `AMTheme.Frosted`, `Style.Frosted` / `Style.Surface` / `Style.Hairline` / `Style.SelectionRing` values, the internal `Modifier.amElevatedSurface` (the solid toolbar button), `FrostedSlice` and `LocalAMBackdrop` (the frosted bar), `amInstantSwitch`, `Modifier.amCrossfade`; `Modifier.amArtText` from `datadisplay/` (`DataDisplay.md`). The scene root builds on `AMOverlayRegistry` / `LocalAMOverlayRegistry` (`internal/OverlayRegistry.kt`, `Overlays.md`), `AMBannerHost` and the internal `ToastHost` (`Feedback.md`), and the internal `FloatingActionButtonDiameter` (`Actions.md`).
- Why `AMScene` lives here: it's the root the remote navigates from, and its roots form is `AMKeepsPlace` with the scene around it.
- The toolbar button builds on `AMIconButton` (`Actions.md`) for its glyph, tooltip and target, and on `LocalAMBackdrop` and `amElevatedSurface` for its solid form; the tabs on `AMSurfaceFill` and `amElevatedSurface` for their frosted surface.
- Catalog pages: `demo/.../catalog/pages/navigation/` (below).

## Panels — `AMPanel`, `AMPanelChrome` (`Panel.kt`)

A panel is a screen pushed over the one you were on: a **Navigation 3 entry**. The back stack is the app's hoisted state; the app pushes a key to show a panel and builds the panel from the key, so what a panel shows is fixed when it's pushed and stays put while it animates away.

```kotlin
NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider = entryProvider {
        entry<Projects> { ProjectList(onOpen = { backStack.add(ProjectPanel(it.id)) }) }
        entry<ProjectPanel> { key ->
            AMPanel(projects.getValue(key.id).name, onBack = { backStack.removeLastOrNull() }, actions = { ShareButton() }) { padding ->
                ProjectTasks(key, Modifier.verticalScroll(rememberScrollState()).padding(padding))
            }
        }
    },
)
```

- **Motion is Navigation 3's.** `NavDisplay` animates the push and the pop with its own transitions, follows predictive back, and keeps each covered entry's saved state (its scroll position) through its default entry decorators. The library writes no transition code for panels.
- **`AMPanel(title, onBack, modifier, actions = null, containerColor = AMTheme.Surface.page, content: (PaddingValues) -> Unit)`**: a `Scaffold` with `AMPanelChrome` as its top bar, driven by its own `TopAppBarDefaults.pinnedScrollBehavior()` (the `nestedScrollConnection` on the scaffold), on `containerColor` with `onSurface` content. `content` gets the scaffold's padding (the bar plus the system bars), to apply inside its scrolling container, so content scrolls under the bar edge to edge. Pass the same back action to `onBack` as to `NavDisplay`, so the arrow and system back pop alike. A panel that draws its own backdrop passes `Color.Transparent`.
- **`AMPanelChrome(title, onBack, modifier, actions = null, scrollBehavior = null, windowInsets = TopAppBarDefaults.windowInsets)`**: Material's small `TopAppBar`. The navigation icon is an `AMToolbarButton` with `am_ic_arrow_back`, read as "Back" (its tooltip too), calling `onBack`, so it follows the toolbar button's rule like the `actions` beside it: a bare glyph on a page, solid inside an `AMBackdrop`. The title is one line (ellipsized) in the bar's own type; `actions` are trailing `AMToolbarButton`s (null shows none). Use it directly for a panel laid out by hand.
- **On a page**: `containerColor = Transparent`, `scrolledContainerColor = AMTheme.Surface.page`, so it lies on the page at rest and takes the page color, with Material's own color animation, once its `scrollBehavior` reports content under it. Without a `scrollBehavior` it stays transparent.
- **Inside an `AMBackdrop`** (with a `scrollBehavior`): both Material colors are transparent and the bar sits in a `FrostedBar` whose fill (`frostedBarFill`, below) fades in once content is under it (`oneRowBarFill`: the overlap past 0.01, animated with `motionScheme.defaultEffectsSpec()`, as Material fades its one-row color), never a page band. The title keeps the bar's `onSurface`, which reads on the frosted fill; a bar over dark art that wants white titles at rest is `AMArtPanelChrome`.

## Panels over art — `AMArtPanelChrome` (`ArtPanelChrome.kt`)

`AMArtPanelChrome(onBack, modifier, title = null, subtitle = null, scrollBehavior = null, windowInsets = TopAppBarDefaults.windowInsets)`: the bar for a panel whose background is a picture (an album over its cover). It sits inside the `AMBackdrop` whose art it lies on, as a `Scaffold`'s top bar on a transparent container:

```kotlin
val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
AMBackdrop(art = { Box(Modifier.fillMaxSize().amArtBackdrop(album.coverUrl)) }, Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { AMArtPanelChrome(onBack = ::pop, title = album.title, subtitle = "Field Notes · 9 songs · 38 min", scrollBehavior = scrollBehavior) },
        containerColor = Color.Transparent,
    ) { padding -> … }
}
```

- **Layout.** Material's `TopAppBar` with transparent container colors; the subtitle overload when there's a `subtitle`, so the title stands over it in the bar's own title and subtitle type. A null `title` leaves the back button alone in the bar. The title carries heading semantics. Each line is one line, ellipsized.
- **The back button is solid**, a crucial control, wherever the bar sits: the internal `ToolbarButton(…, solid = true)`, the solid form of `AMToolbarButton` (below: the 40 dp circle wearing `amElevatedSurface(CircleShape)`, `arrow_back` in `onSurface`, the 48 dp target outside the circle, the long-press tooltip), read as "Back".
- **At rest** (nothing under the bar): the title and subtitle are white art text (`amArtText`), and the status bar's icons turn light.
- **Scrolled** (the scroll behavior's `overlappedFraction` past 0.01, Material's own test, read through `derivedStateOf` in `barScrolledState`): a fill fades in behind the whole bar, status bar included, its alpha animated with `motionScheme.defaultEffectsSpec()` (`FrostedBar` with `oneRowBarFill`, its alpha read in the fill's layer, so the fade redraws without recomposing). The fill (`frostedBarFill`, a draw node using the foundations' `FrostedSlice`) is the nearest backdrop's art under the bar blurred at `Frosted.blurRadius`, under `Frosted.fill` (white 0.6 / black 0.5), with a hairline of the frosted edge color along the bottom; it redraws when the bar moves over its art. With no `AMBackdrop`, or before the art is placed, it's the solid surface, with the dark-mode hairline along the bottom, never unblurred translucency. The lines cross over with `Modifier.amCrossfade`: the title to the bar's on-surface color, the subtitle to `Frosted.secondaryText`. The status bar's icons follow the app's appearance again.
- **Status bar icons.** While the bar is at rest it holds an `AMLightStatusBarIcons(enabled = overArt)` claim (below), so its icons count together with every other claim in the window: Navigation 3 composes the incoming entry before the covered one leaves (a push from one art panel to another, a pop, a predictive back that's cancelled), so the claims overlap and end out of order, and a covered panel leaving never undoes the icons of the panel on top. Edge to edge throughout; the bar keeps clear of `windowInsets` and its fill reaches behind them.

## Light status bar icons over art — `AMLightStatusBarIcons` (`StatusBarIcons.kt`)

```kotlin
@Composable
fun Player(track: Track) {
    AMLightStatusBarIcons()                              // art from top to bottom
    …
}
AMLightStatusBarIcons(enabled = headerOverArt)           // a root's header at rest on its art rows
```

`AMLightStatusBarIcons(enabled = true)`: an effect, no UI. While it's composed with `enabled` true it holds a claim on its window's status bar icons; `AMArtPanelChrome`'s bar at rest holds one through it too, so claims from anywhere in a window count together.

- **The claim.** A `DisposableEffect` keyed on `LocalView`, `enabled` and `AMTheme.isDark` finds the window's claims (`lightStatusBarClaims(view)`: one `LightStatusBarClaims` per window in a `WeakHashMap` keyed on the view's `rootView`, main thread only), calls `claim(found)` with the window's current `APPEARANCE_LIGHT_STATUS_BARS` bit and clears that bit on the `WindowInsetsController`; disposing calls `release()`, and when that returns a value (the last claim ended) it sets the bit back to it.
- **`LightStatusBarClaims`** (internal, pure): `held`, `claim(found)` (keeps `found` only for the first claim, since later ones find the icons the first set), `release(): Int?` (the bit the first claim found when the last one ends, null while others hold; ending a claim nobody holds fails). Claims overlap and end in any order; the icons go back to what the window had before the first only when the last ends.
- **The app's appearance wins back** once nothing claims. Keying on the appearance means an app that re-tints its bars when the appearance changes (a `DisposableEffect` at the root, composed first) runs first and the art still wins: on a change every claim ends (the last puts back the old bit), the app re-tints, then the claims take the icons again and the first records the new bit.
- In dark mode the icons are already light, so a claim changes nothing visible. Edge to edge, the content draws the art behind the status bar itself.
- Tested: `LightStatusBarClaimsTest`.

## The scene root — `AMScene` (`Scene.kt`)

Each screen of an app (each scene) has one at its root, around everything it shows.

```kotlin
var view by rememberSaveable { mutableStateOf(View.Inbox) }
var showsRemote by rememberSaveable { mutableStateOf(false) }
AMScene(
    current = view,
    landing = View.Inbox,
    onCurrentChange = { view = it },
    floatingActions = { AMFloatingActions(showsRemote, onExpandedChange = { showsRemote = it }) { Remote() } },
    overlays = {
        AMSheet(showsCompose, onDismissRequest = { showsCompose = false }, title = "New message") { Compose() }
        AMToast(toast, onDismissRequest = { toast = null })
    },
    backStack = { root -> if (root == View.Inbox) inbox else emptyList() },
) { root ->
    when (root) {
        View.Inbox -> NavDisplay(inbox, onBack = { inbox.removeLastOrNull() }) { … }
        View.Archive -> ArchiveView()
    }
}
```

- **Plain form**: `AMScene(modifier, floatingActions: slot? = null, containerColor = AMTheme.Surface.page, content)`, for a scene with one root view (or one that switches its content itself, as a sign-in flow's phases).
- **Roots form**: `AMScene<T : Any>(current, landing, onCurrentChange, modifier, floatingActions = null, overlays = null, backStack = { emptyList() }, containerColor, content: (root: T) -> Unit)`, the plain form with `AMKeepsPlace(current, landing, onCurrentChange, Modifier.fillMaxSize(), backStack, content)` and then `overlays` as its content: each root's place is saved and restored, and back steps to the landing view before it leaves the app. Roots are `Bundle` values; `current` lives in `rememberSaveable`.
- **`overlays`** (roots form) is what the whole scene presents, whichever root is showing: its sheets, alerts, spotlight, loading overlay and toasts. It's composed beside the roots, after `AMKeepsPlace` and before `floatingActions`, inside the scene's registry, hosts and whatever `AMBackdrop` is around the scene (so frosted sheets blur its art) and outside every root, so a switch of root never disposes an open sheet. Overlays one root owns can stay in that root. A scene on a backdrop wraps the whole `AMScene` in its `AMBackdrop` and passes `containerColor = Color.Transparent`. The plain form needs no slot: its content is already the whole scene.
- **What it builds**, outermost first: it remembers an `AMOverlayRegistry` and provides it as `LocalAMOverlayRegistry` (with `LocalContentColor` = `onSurface`); then `AMBannerHost(modifier.fillMaxSize().background(containerColor))`; inside it the internal `ToastHost(fillMaxSize, clearance)`; inside that a `Box(fillMaxSize, propagateMinConstraints = true)` holding `content` (in the roots form, the roots and then `overlays`) and then `floatingActions`, so the remote's button lies over the content and over the panels pushed inside it (pushes are navigation, not overlays), and banners and toasts lie over both.
- **The overlay registry.** Every overlay composed inside (a sheet, an alert, the remote's own menu, the spotlight, the loading overlay) reports while it's up, windows included, since a window inherits the locals of the composition that opens it; `AMFloatingActions` reads `anyUp` and fades the button. A scene nested inside another is its own nearest owner.
- **The hosts.** An `AMBanner` or `AMToast` posted anywhere inside shows in the scene's hosts, at the top and the bottom of the screen (`Feedback.md`). With `floatingActions`, the toast host's clearance is `FloatingActionButtonDiameter + spacing.lg` (56 + 16 dp above the navigation bar), so a toast shows above the button's resting place, as Material's scaffold places a snackbar above its floating action button; without, toasts sit on the navigation bar.
- **Edge to edge.** It fills what it's given in `containerColor` (the page by default; a scene that draws its own backdrop passes `Color.Transparent`) and pads nothing: the content runs behind the system bars and handles its own insets (`AMPanel`, `AMPageHeader` in a scaffold), as the button, the banner and the toast keep clear of theirs.
- **Back.** The order is by construction: overlays are windows, so an alert, then the spotlight or the remote, then a sheet take back first; then the current root's `NavDisplay` pops its panels; then `KeepsPlaceBackHandler` swaps to the landing root (roots form); then back falls through and the app closes. The scene itself registers no handler.
- **The remote's view tiles** close the menu, then set `current` inside `amInstantSwitch`, so the new root appears in the same frame, restored, while the menu fades over it; the current root's tile is `active` (`Actions.md`).

## Keeping each root view's place — `AMKeepsPlace` (`KeepsPlace.kt`)

```kotlin
var view by rememberSaveable { mutableStateOf(View.Feed) }
AMKeepsPlace(current = view, landing = View.Feed, onCurrentChange = { view = it }, backStack = { if (it == View.Feed) feed else emptyList() }) { root ->
    when (root) {
        View.Feed -> NavDisplay(feed, onBack = { feed.removeLastOrNull() }) { … }
        View.Alerts -> AlertsView()
    }
}
```

`AMKeepsPlace<T : Any>(current, landing, onCurrentChange, modifier, backStack: (root: T) -> List<*> = { emptyList() }, content: (root: T) -> Unit)`.

- **Views you leave are saved and restored, not kept running.** Only `current` is composed, inside `rememberSaveableStateHolder().SaveableStateProvider(current)`. Leaving a root disposes it and keeps its saved state (every `rememberSaveable`, lazy list and scroll positions, a `NavDisplay`'s own entry state); coming back restores it in the same frame. The holder is itself saveable, so every root's place survives configuration changes and process death. Roots are keys a `Bundle` holds (an enum entry, a `String`, an `Int`), and the app keeps `current` in `rememberSaveable`.
- **Switching is instant**: the container has no motion. An app (a remote's view tile) changes `current` inside `amInstantSwitch`.
- **Each root's back stack is the app's own state** (Navigation 3's model), kept above the container so it outlives a switch. `backStack(root)` returns it, the root's own entry first; the container only reads its size, while composing, so a push or pop recomposes it. A root without panels returns an empty list.
- **Back.** Overlays are windows, so they take back first; then the current root's `NavDisplay` pops its panels (its handler is enabled only while it has an entry to pop). Internal `KeepsPlaceBackHandler` is a `NavigationBackHandler` (`navigationevent-compose`, `NavigationEventInfo.None`) enabled exactly when `keepsPlaceBackTarget(current, landing, depth)` returns a target: `landing` when `current` isn't `landing` and its back stack holds at most one entry, else null. Completing back calls `onCurrentChange(landing)` inside `amInstantSwitch`, an instant swap. On the landing root it stands aside, so back falls through to whatever is below (the app closes, or a host's own back stack pops). It's registered before the content composes, so handlers inside a root (a search bar's) come first.
- **The scene root builds on it.** `AMScene`'s roots form is an `AMKeepsPlace` filling the scene, its remote's view tiles switching roots through the app's `current` inside `amInstantSwitch`. Use `AMKeepsPlace` directly for roots that switch inside part of a scene (signed-in views under a sign-in flow). The back order: alert → spotlight or remote → sheet → panel stack → landing view → exit.
- Tested: `KeepsPlaceBackTest` pins the decision (another root with no panel or no back stack swaps to the landing; a panel pops first; the landing root falls through).

## Toolbars — `AMToolbar`, `AMToolbarButton` (`Toolbar.kt`)

```kotlin
AMToolbar(
    "Now playing",
    subtitle = "Recently added",
    navigationIcon = { AMToolbarButton(painterResource(R.drawable.ic_arrow_back), "Back", onClick = ::pop) },
    actions = {
        AMToolbarButton(painterResource(R.drawable.ic_search), "Search", onClick = ::search)
        AMToolbarButton(painterResource(R.drawable.ic_more_vert), "More options", onClick = ::openMenu)
    },
)
```

- **`AMToolbar(title, modifier, subtitle: String? = null, navigationIcon = null, actions = null, scrollBehavior = null, windowInsets = TopAppBarDefaults.windowInsets)`**, and the slot form `AMToolbar(modifier, subtitle: slot? = null, navigationIcon, actions, scrollBehavior, windowInsets, title: slot)`; the string form delegates, its title and subtitle each one line, ellipsized. Null slots show nothing.
- **The title-less form** `AMToolbar(modifier, navigationIcon = null, actions = null, scrollBehavior = null, windowInsets)`: the bar's buttons alone, for a screen whose title is elsewhere or that has none (a welcome screen with only a light/dark switch: `AMToolbar(actions = { AMToolbarButton(modeIcon, "Switch to dark mode", onClick = ::flip) })`). It's the slot form with an empty title, so it keeps Material's 64 dp height, colors, scroll and frosted behavior, and TalkBack reads only its buttons. Its last parameter isn't a lambda, so a trailing lambda always picks the slot form.
- **Material's center-aligned top app bar**: `CenterAlignedTopAppBar`, or with a subtitle Material's `TopAppBar` with `subtitle` and `titleHorizontalAlignment = CenterHorizontally`, so the title stays centered whatever the slots hold, in the bar's own title and subtitle type and colors. Navigation icon at the start, actions at the end (⋮ last). Visual only: taps stay with the buttons in the slots.
- **Colors**: `containerColor = Transparent`, `scrolledContainerColor = AMTheme.Surface.page`, like `AMPanelChrome`: it lies on whatever is behind it and takes the page color, with Material's color animation, once its `scrollBehavior` reports content under it; without one it stays transparent. Inside an `AMBackdrop` (with a `scrollBehavior`) it takes the frosted fill instead, exactly as `AMPanelChrome` does (`FrostedBar`, `oneRowBarFill`). `windowInsets` defaults to the status bar; a toolbar that doesn't reach the top of the screen passes `WindowInsets(0)`.
- **On a picture** its buttons turn solid (below); the title keeps the bar's `onSurface`, so the app washes its art light or dark enough for it to read at rest (scrolled, the frosted fill under it does). A panel over dark art whose title is white at rest uses `AMArtPanelChrome` instead.

**`AMToolbarButton(icon, contentDescription: String, onClick, modifier, destructive = false, enabled = true)`**, one form (fixed content). It's the internal `ToolbarButton(…, solid)` with `solid = LocalAMBackdrop.current != null`; `AMArtPanelChrome`'s back passes `solid = true`. `AMPanelChrome`'s back arrow is one.
- **Plain** (the default): `AMIconButton` at its `Medium` size: Material's `IconButton`, 40 dp with a 24 dp glyph in the bar's content color, the round ripple, the 48 dp target, and the long-press tooltip naming it.
- **Solid over art**: inside an `AMBackdrop` (`LocalAMBackdrop` non-null) it's a crucial control, solid like `AMArtPanelChrome`'s back button: a `Box` with `minimumInteractiveComponentSize()` and then `amElevatedSurface(CircleShape)` (the floating surface's fill, the `Low` shadow, the dark-mode hairline) around the same `AMIconButton`, laid out with `LocalMinimumInteractiveComponentSize` at 0, so the circle hugs Material's 40 dp button and the 48 dp target sits outside it; the glyph is `onSurface` and the ripple is the button's own circle. Icon-only, so it stays round. Solid in every app, frosted or not.
- **`destructive`** colors the glyph `AMSemantic.Destructive` (the scheme's `error`). **Disabled** (its own `enabled` or `LocalAMEnabled`): the glyph greys to Material's 0.38 of the content color (never red), and a solid button keeps its fill but casts no shadow (`AMElevation.None`).
- For a glyph inside a card's action row use `AMCardButton` (`Actions.md`).

## In-page tabs — `AMTabs`, `AMTabsScope` (`Tabs.kt`)

```kotlin
var tab by rememberSaveable { mutableStateOf("posts") }
AMTabs(selection = tab, onSelectionChange = { tab = it }) {
    tab("posts", "Posts") { PostsList() }
    tab("photos", "Photos", icon = { Icon(painterResource(R.drawable.ic_image), null) }) { PhotoGrid() }
    if (showsDrafts) tab("drafts", "Drafts") { DraftsList() }
}
```

- **`AMTabs<T>(selection, onSelectionChange, modifier, fill = AMSurfaceFill.Solid, enabled = true, tabs: AMTabsScope<T>.() -> Unit)`**: the selection is hoisted; a tab's tap calls `onSelectionChange(id)`, so a change made elsewhere switches the tabs the same way. They switch part of a page, never the app's views (the remote does that).
- **`AMTabsScope<T>`** (internal constructor) declares the tabs, in order, as plain Kotlin, so `if` and loops work: `tab(id, label: String, icon: slot? = null, content)` (the label up to two lines, ellipsized) and the slot form `tab(id, label: slot, icon, content)`, which the string form delegates to. Each becomes an internal `TabEntry`; the scope is rebuilt each composition.
- **The strip** is Material's `SecondaryTabRow` on a transparent container: equal-width `Tab`s (`LeadingIconTab` when the tab has an `icon`, the glyph before the label), `selectedContentColor = onSurface`, `unselectedContentColor = onSurfaceVariant` (Material's secondary-tab colors), `enabled` resolved through `LocalAMEnabled`, Material's divider under the row, its ripple, and its tab semantics (selected or not). The indicator is `TabRowDefaults.SecondaryIndicator` at `tabIndicatorOffset(selected, matchContentSize = false)`, sliding with Material's own motion. A `selection` matching no tab draws no indicator and no content.
- **The content**: a `Crossfade` keyed on `selection` (`motionScheme.defaultEffectsSpec()`, instant under Remove animations) shows only the selected tab's content, so the other tabs' content isn't composed. It's `fillMaxWidth().fillMaxHeight()` under the strip: given a height, the tabs fill it; in a scrolling column they take their content's height. Content lies at the top start.
- **The surface** under strip and content is one flat shape in `shapes.large`, clipped, with no shadow: `Solid` fills it with `AMTheme.Surface.primary` (no hairline); `Frosted` draws `amElevatedSurface(shape, AMElevation.None, AMSurfaceFill.Frosted)`, glass over the nearest `AMBackdrop`'s art with the frosted edge (solid with no art). An app passes the fill its cards use. Inside a card (`Modifier.amCard()` on the tabs' modifier) the card supplies the lift.

## Page dots — `AMPageIndicator` (`PageIndicator.kt`)

```kotlin
val pagerState = rememberPagerState { slides.size }
HorizontalPager(pagerState) { page -> Slide(slides[page]) }
AMPageIndicator(count = slides.size, currentIndex = pagerState.currentPage)
```

- **`AMPageIndicator(count, currentIndex, modifier)`**: read-only, no gesture; paging is the pager's. Custom (Android has no stock one) in the scheme's colors.
- A `Row` of `count` dots `spacing.sm` apart, centered vertically. Each dot (private `PageDot`) is 6 dp tall, in `CircleShape`: the current one 18 dp wide in `onSurface`, the others 6 dp circles in `outlineVariant`, so the strip's height never changes. A page change animates the width with `motionScheme.defaultSpatialSpec()` and the color with `defaultEffectsSpec()`.
- `currentIndex` is clamped into `0 until count`, so an index briefly out of range still marks a dot.
- TalkBack reads the strip as one element (`clearAndSetSemantics`): "Page 3 of 5" (the clamped index plus one, of at least 1).
- It lies on the page, or on the card the pager is in, never on a card of its own.

## Catalog pages

`demo/.../catalog/pages/navigation/`, registered in `NavigationPages.entries` in this order (ids): `toolbar`, `toolbarButton`, `panel`, `tabs`, `pageIndicator`.

| Page | Preview | Loaded |
|---|---|---|
| Toolbar | `FullBleed`: "Inbox" on a surface strip, menu at the start, compose at the end | `FullBleed`: "Now playing" over "Recently added" on a surface strip, back at the start, search and ⋮ at the end |
| Toolbar button | `FullBleed`: one plain button (compose) on a surface strip | `FullBleed`: the top of a photo's screen, an `AMBackdrop` of the photo blurred under `AMTheme.Frosted.fill`, holding a "Brooklyn" toolbar whose buttons are solid (back; share disabled; delete destructive) over the photo itself (4:3) |
| Panel | a card of three project rows (Atlas, Beacon, Cinder: `ListItem` rows on `AMRowDefaults` with a chevron, dividers between); each pushes its project's `AMPanel`, titled by the project with a Share action, holding an `AMSection` of its open tasks over the project's summary | a card with one album row (the cover, "Cobalt", "Field Notes · 9 songs"), pushing the album over its art: an `AMBackdrop` whose art is the cover's `Modifier.amArtBackdrop` (`DataDisplay.md`), `AMArtPanelChrome` ("Cobalt", "Field Notes · 9 songs · 38 min"), the cover with the `Low` shadow, and a card of nine track rows with their lengths |
| Tabs | `FullBleed`: Overview, Activity, Files on a surface strip, each a line of text on the row recipe | tabs as a card (`amCard()`): Photos, Albums (selected), Shared, each with its glyph, showing three title-and-detail `ListItem` rows with chevrons (`AMRowDefaults`, dividers between) |
| Page indicator | `FullBleed`: five dots, the third current, on a surface strip | an onboarding card: a `HorizontalPager` of four slides (a glyph in `primary`, a `titleLarge` title, a `bodyMedium` line in `onSurfaceVariant`), the dots, and a full-width Next that pages on and reads Get started on the last slide |

The Panel page's pushes go onto the catalog's own back stack: a row calls `CatalogNavigator.openPanel(PanelEntry, route)` (the project's id, or `album`), and the page wraps itself in `CatalogPanels(page = { ComponentPage(…) }) { route -> … }`, which draws the page or the panel its route names (`app/CatalogRoot.kt`, `Demo.md`). The panels pop with back, predictive back or their back arrow (`navigator.back()`), and come back after process death.

The Page indicator's pager keeps every slide composed (`beyondViewportPageCount`), so it's as tall as its tallest slide and doesn't jump while swiping. Demo glyphs these pages add: `ic_toolbar`, `ic_radio_button_checked`, `ic_right_panel_open`, `ic_tab`, `ic_more_horiz` (entry icons), `ic_menu`, `ic_image`, `ic_photo_album`, `ic_group`, `ic_waving_hand`; they also use `ic_arrow_back`, `ic_edit_square`, `ic_search`, `ic_more_vert`, `ic_share`, `ic_delete`, `ic_chevron_right`, `ic_calendar_today` and `ic_notifications`.
