---
scope: Moving between views — the scene root (AMScene) and its slots, panels on Navigation 3 and over art, the status bar's icons following the art, root views that keep their place and step back to the landing view, toolbars and their buttons, in-page tabs and page dots — the library's navigation/ folder and the Navigation catalog pages
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/navigation/**, androidmtrls/src/main/res/drawable/am_ic_arrow_back.xml, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/KeepsPlaceBackTest.kt, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/StatusBarClaimsTest.kt, androidmtrls/src/androidTest/kotlin/com/ideamtrls/androidmtrls/PanelBackTest.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/navigation/**]
keywords: [AMScene, scene root, floatingActions, overlays, scene-wide overlays, SceneRoot, SceneOverlays, SceneFloatingActions, AMSceneBackdrops, LocalAMSceneBackdrops, sceneBackdrop, showing root, overlay registry, toast clearance, back order, AMPanel, AMPanelChrome, AMArtPanelChrome, panel, push, panelBack, dropUnlessStarted, LocalLifecycleOwner, PanelBackTest, Navigation 3, NavDisplay, NavEntry, entryProvider, back stack, predictive back, TopAppBar, pinnedScrollBehavior, overlappedFraction, scrolledContainerColor, arrow_back, back chip, art, album, FrostedBar, frostedBarFill, oneRowBarFill, twoRowBarFill, barLineColor, frosted bar, AMStatusBarIcons, status bar icons, StatusBarClaims, StatusBarClaimsTest, topCovers, ReportTopCover, artStatusBar, APPEARANCE_LIGHT_STATUS_BARS, edge-to-edge, AMKeepsPlace, root views, landing view, rememberSaveableStateHolder, KeepsPlaceBackHandler, keepsPlaceBackTarget, KeepsPlaceBackTest, NavigationBackHandler, navigationevent-compose, amInstantSwitch, AMToolbar, title-less toolbar, CenterAlignedTopAppBar, titleHorizontalAlignment, subtitle, navigationIcon, AMToolbarButton, ToolbarButton, chip over art, BarButton, destructive, tooltip, AMTabs, AMTabsScope, SecondaryTabRow, LeadingIconTab, tabIndicatorOffset, Crossfade, frosted tabs, AMPageIndicator, page dots, HorizontalPager, Page x of y, ToolbarPage, ToolbarButtonPage, PanelPage, TabsPage, PageIndicatorPage, CatalogPanels, openPanel]
updated: 2026-11-15
---

# Navigation

Moving between views: the scene root (`AMScene`: the remote's slot, the scene-wide overlays, the registry and hosts it provides), panels on Navigation 3 (`AMPanel`, `AMPanelChrome`, `AMArtPanelChrome` over art), the status bar's icons following the art (`AMStatusBarIcons`), root views that keep their place and step back to the landing view (`AMKeepsPlace`), toolbars and their buttons (`AMToolbar`, `AMToolbarButton`), in-page tabs (`AMTabs`) and page dots (`AMPageIndicator`).

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `navigation/Panel.kt` | `AMPanel`, `AMPanelChrome`; internal `PanelBackLabel` ("Back"), `panelBack` (the back button's action, live only while its panel is on the back stack) |
| `navigation/ArtPanelChrome.kt` | `AMArtPanelChrome`; private `ArtBarText` |
| `navigation/StatusBarIcons.kt` | `AMStatusBarIcons`; internal `StatusBarClaims` (the counter), private `windowClaims`, `statusBarClaims` |
| `navigation/FrostedBar.kt` | internal `FrostedBar`, `barScrolledState`, `oneRowBarFill`, `twoRowBarFill`, `twoRowBarFilledState`, `BarScrolledFraction`, `BarLinesFilledFill`, `barLineColor`, `Modifier.frostedBarFill()` (private `FrostedBarFillNode`): the frosted fill of a top app bar over art, and the color its lines take on art and on the fill, shared by `AMArtPanelChrome`, `AMPanelChrome`, `AMToolbar` and `AMPageHeader` (`foundations/PageHeader.kt`) |
| `navigation/Scene.kt` | `AMScene` (plain and roots forms); private `SceneRoot` (the plain form's body, which the roots form calls with its backdrops), `SceneOverlays` (the overlays on the showing root's art), `SceneFloatingActions` (the remote on the scene's backdrop, or on the showing root's art) |
| `navigation/KeepsPlace.kt` | `AMKeepsPlace`; internal `KeepsPlaceBackHandler`, `keepsPlaceBackTarget` |
| `navigation/Toolbar.kt` | `AMToolbar` (string, slot and title-less forms), `AMToolbarButton`; internal `ToolbarButton` (the button in a form the caller picks) |
| `navigation/Tabs.kt` | `AMTabs`, `AMTabsScope`; internal `TabEntry` |
| `navigation/PageIndicator.kt` | `AMPageIndicator`; private `PageDot` |

- Glyph: `androidmtrls/src/main/res/drawable/am_ic_arrow_back.xml` (Material Symbols `arrow_back`, auto-mirrored).
- Tests: `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/KeepsPlaceBackTest.kt` (the back-to-landing decision), `StatusBarClaimsTest.kt` (the claim counting: overlapping claims, out-of-order ends, what the last one puts back); instrumented, `androidmtrls/src/androidTest/kotlin/com/ideamtrls/androidmtrls/PanelBackTest.kt` (a panel's back button under a real `NavDisplay`: tapped again while its panel pops, `AMPanel`'s and `AMArtPanelChrome`'s pop once; tapped while the panel slides in, it pops).
- Dependencies: the library uses `navigationevent-compose` (`implementation`) for the back handler and `lifecycle-runtime-compose` (`implementation`) for the panel back button's lifecycle gate; it exposes no Navigation 3 types, so apps bring Navigation 3 themselves (its tests use Navigation 3 UI).
- Pieces from other areas it builds on. `Foundations.md`: `AMTheme.Surface.page` / `.primary`, `AMTheme.Frosted`, `AMTheme.surfaceFill` (the tabs), `Style.Frosted` / `Style.Surface` / `Style.Hairline` / `Style.SelectionRing` (the bar fill), `Style.Shadow.BarButton` (the toolbar chip), the internal `Modifier.amElevatedSurface` (the toolbar chip, the frosted tabs), `ProvideAMOnSurface`, `FrostedSlice`, `LocalAMDarkTheme`, `amInstantSwitch`, and from `internal/BackdropSource.kt` `LocalAMBackdrop`, `AMSceneBackdrops` / `LocalAMSceneBackdrops`, `ReportTopCover`; `ProvideAMSurfaceStyle(AMSurfaceStyle.Flat)` (the tabs, `Layout.md`); `LocalAMArtTone`, `artInk`, `artToneOf` (`datadisplay/ArtTone.kt`, `DataDisplay.md`); `AMIconButton` (the toolbar button's glyph, tooltip and target), `resolveEnabled` and the internal `floatingActionButtonTop()` (`Actions.md`); `AMOverlayRegistry` / `LocalAMOverlayRegistry` (`internal/OverlayRegistry.kt`, `Overlays.md`); the internal `BannerHost` and `ToastHost` (`Feedback.md`).
- Why `AMScene` lives here: it's the root the remote navigates from, and its roots form is `AMKeepsPlace` with the scene around it.
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
- **`AMPanelChrome(title, onBack, modifier, actions = null, scrollBehavior = null, windowInsets = TopAppBarDefaults.windowInsets)`**: Material's small `TopAppBar`. The navigation icon is an `AMToolbarButton` with `am_ic_arrow_back`, read as "Back" (its tooltip too), calling `onBack` through `panelBack`, so it follows the toolbar button's rule like the `actions` beside it: a bare glyph on a page, a chip inside an `AMBackdrop`. The title is one line (ellipsized) in the bar's own type; `actions` are trailing `AMToolbarButton`s (null shows none). Use it directly for a panel laid out by hand.
- **The back button acts once per panel** (`panelBack`, both bars): `dropUnlessStarted(block = onBack)`, so it calls `onBack` only while the bar's `LocalLifecycleOwner` is at least started. Navigation 3 gives each entry its own lifecycle and lowers a popped entry's to created at once, while still drawing it on top, touchable, as it animates away; so a second tap on the leaving panel's button does nothing, where it would otherwise pop the view under it, or, one panel deep with `removeLastOrNull()`, the root's own entry (an empty back stack, which `NavDisplay` rejects by throwing). System back never reaches a popped entry, so the button and system back agree. Started, not resumed: an entry sliding in is held below resumed until its transition ends, and its button still pops it. Outside Navigation 3 the gate is the host's lifecycle, started while the screen shows.
- **On a page**: `containerColor = Transparent`, `scrolledContainerColor = AMTheme.Surface.page`, so it lies on the page at rest and takes the page color, with Material's own color animation, once its `scrollBehavior` reports content under it. Without a `scrollBehavior` it stays transparent.
- **Inside an `AMBackdrop`** (with a `scrollBehavior`): both Material colors are transparent and the bar sits in a `FrostedBar` whose fill (`frostedBarFill`, below) fades in once content is under it (`oneRowBarFill`: the overlap past 0.01, animated with `motionScheme.defaultEffectsSpec()`, as Material fades its one-row color), never a page band; while it's in, it covers the status bar (`FrostedBar(covers = scrolled)`). Without a `scrollBehavior` it stays transparent there too, its back arrow still a chip.
- **The title** is `barLineColor(scrolled, onSurface)`, as `AMToolbar`'s: on art (an art tone around it) the ink of the art's tone until content scrolls under the bar, then `onSurface`; off art, `onSurface` throughout. On the library's art the tone is the mode's, so the title doesn't change. Unlike `AMArtPanelChrome` it has `actions`, no subtitle, and a plain back arrow off a backdrop.

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
- **The back button is a chip** wherever the bar sits: the internal `ToolbarButton(…, chip = true)`, the chip form of `AMToolbarButton` (below: the 40 dp circle wearing `amElevatedSurface(CircleShape)` in the app's surface setting, with the bar-fitted shadow, `arrow_back` in `onSurface`, the 48 dp target outside the circle, the long-press tooltip), read as "Back", calling `onBack` through `panelBack` like `AMPanelChrome`'s arrow.
- **At rest** (nothing under the bar), on art: the title and subtitle take the ink of the art's tone, the subtitle the secondary ink (`Style.ArtInk`: white and `#D1D1D6` on dark art, black and `#2C2C2E` on light art). Off art (no art tone around it) they're the scrolled colors throughout.
- **Scrolled** (the scroll behavior's `overlappedFraction` past 0.01, Material's own test, read through `derivedStateOf` in `barScrolledState`): a fill fades in behind the whole bar, status bar included, its alpha animated with `motionScheme.defaultEffectsSpec()` (`FrostedBar` with `oneRowBarFill`, its alpha read in the fill's layer, so the fade redraws without recomposing). The fill (`frostedBarFill`, a draw node using the foundations' `FrostedSlice`) is the nearest backdrop's art under the bar blurred at `Style.Frosted.BlurRadius` (40 dp), under the frosted fill (white 0.6 / black 0.5), with a hairline of the frosted edge color along the bottom; it redraws when the bar moves over its art. With no `AMBackdrop`, or before the art is placed, it's the solid surface, with the dark-mode hairline along the bottom, never unblurred translucency. The lines' colors move to the bar's: the title to `onSurface`, the subtitle to `AMTheme.Frosted.secondaryText` (private `ArtBarText` through `barLineColor(scrolled, frostedColor, secondary)`, animated with `defaultEffectsSpec`). The bar is always in a `FrostedBar`, inside an `AMBackdrop` or not; without a `scrollBehavior` it stays at rest.
- **Status bar icons** are the art's to decide (`AMStatusBarIcons`, below): the bar holds no claim of its own. On the library's art, washed toward the mode, they're the mode's; on art its `AMBackdrop` says is the other tone, the backdrop turns them over while it reaches the top. Once scrolled, the bar's frosted fill covers the status bar (`FrostedBar(covers = scrolled)` reports it to the backdrop), so the icons are the mode's. Edge to edge throughout; the bar keeps clear of `windowInsets` and its fill reaches behind them.

## The status bar follows the art — `AMStatusBarIcons` (`StatusBarIcons.kt`)

```kotlin
@Composable
fun PhotoViewer(photo: Photo) {
    AMStatusBarIcons(AMArtTone.Dark)                     // a raw photo, top to bottom
    …
}
AMBackdrop(art = { Photo() }, Modifier.fillMaxSize(), tone = AMArtTone.Dark) { … }   // says so itself while it's at the top
```

The status bar's icons follow the tone of what lies under them. The outermost `AMTheme` sets the mode's icons (`Foundations.md`); art in the mode's tone (all the library's art, washed toward the mode) changes nothing; art of the other tone turns them over.

- **`AMStatusBarIcons(tone, enabled = true)`**: an effect, no UI: "what lies under the status bar here is art of `tone`". A tone matching the mode does nothing. A contrary one holds a claim on its window's status bar icons while composed with `enabled` true: light icons on dark art in light mode, dark icons on light art in dark mode. For what the library doesn't draw (a photo shown as it is, video).
- **Art surfaces say it themselves.** `AMBackdrop` and `AMArtRowsBackdrop` put internal `artStatusBar(source, tone)` on their box: an `AMStatusBarIcons(tone, enabled = atTop && source.topCovers == 0)`, where `atTop` (an `onGloballyPositioned` on the box) is whether the region's top lies at or above the window's top with some of it below, and `topCovers` (snapshot state on `AMBackdropSource`) counts the bars on it whose frosted fill is in: `FrostedBar(fill, covers, …)` reports through `ReportTopCover` while `covers` (a one-row bar scrolled, a large header's fill past `BarLinesFilledFill`). So a surface given the other tone turns the icons over while its art is under the status bar, and gives them back once a bar's fill covers them. With the mode's tone (the default) nothing claims. Art panels and scenes hold no claims of their own.
- **The claim.** A `DisposableEffect` keyed on `LocalView`, whether the claim is wanted, and `AMTheme.isDark` finds the window's claims (`statusBarClaims(view)`: one `StatusBarClaims` per window in a `WeakHashMap` keyed on the view's `rootView`, main thread only), calls `claim(found)` with the window's current `APPEARANCE_LIGHT_STATUS_BARS` bit and sets that bit to the contrary icons (cleared in light mode, set in dark mode); disposing calls `release()`, and when that returns a value (the last claim ended) it sets the bit back to it. In one mode every claim wants the same icons.
- **`StatusBarClaims`** (internal, pure): `held`, `claim(found)` (keeps `found` only for the first claim, since later ones find the icons the first set), `release(): Int?` (the bit the first claim found when the last one ends, null while others hold; ending a claim nobody holds fails). Claims overlap and end in any order (Navigation 3 composes the incoming entry before the covered one leaves; predictive back brings one in and may take it out again); the icons go back to what the window had before the first only when the last ends.
- **The mode's icons hold** while nothing claims. Keying on the appearance means the outermost `AMTheme`, which re-tints the window's bars when the appearance changes (a `DisposableEffect` composed ahead of its content, `Foundations.md`), runs first and the art still wins: on a change every claim ends (the last puts back the old bit), the theme re-tints, then the claims still wanted take the icons again and the first records the new bit.
- Tested: `StatusBarClaimsTest` (the counting), `SystemBarsAppearanceTest` (instrumented: a claim in each mode, and a backdrop at the top and below it).

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
- **Roots form**: `AMScene<T : Any>(current, landing, onCurrentChange, modifier, floatingActions = null, overlays = null, backStack = { emptyList() }, containerColor, content: (root: T) -> Unit)`, the plain form's body (private `SceneRoot`, which takes the scene's backdrops too) with `AMKeepsPlace(current, landing, onCurrentChange, Modifier.fillMaxSize(), backStack, content)` and then `overlays` as its content: each root's place is saved and restored, and back steps to the landing view before it leaves the app. Roots are `Bundle` values; `current` lives in `rememberSaveable`.
- **`overlays`** (roots form) is what the whole scene presents, whichever root is showing: its sheets, alerts, spotlight, loading overlay and toasts. It's composed beside the roots, after `AMKeepsPlace` and before `floatingActions`, inside the scene's registry and hosts and outside every root, so a switch of root never disposes an open sheet or cuts one sliding away. Overlays one root owns can stay in that root. A scene on a backdrop wraps the whole `AMScene` in its `AMBackdrop` and passes `containerColor = Color.Transparent`. The plain form needs no slot: its content is already the whole scene.
- **The overlays lie on the showing root's art.** The roots form remembers an internal `AMSceneBackdrops` (`internal/BackdropSource.kt`) and provides it as `LocalAMSceneBackdrops` around `AMKeepsPlace`. Each root's outermost `AMBackdrop` or `AMArtRowsBackdrop` reports its source there while composed (`sceneBackdrop`, a `DisposableEffect`; it provides null to its content, so nested backdrops don't report, and so does `ProvideOverlayWindowLocals`, so art inside an overlay window never does). The sources are a snapshot list and `showing` is the last one composed: the showing root's page, or the panel Navigation 3 brings to the top (it composes only the visible entries; a pop composes the revealed page last). The private `SceneOverlays` provides `LocalAMBackdrop` = `showing`, or the `LocalAMBackdrop` around the scene while the root draws none, to `overlays`, so an `AMSheet` there (frosted in either setting) samples the showing page's art and follows it as roots switch, panels come and go and the art records anew (`Overlays.md`); with neither, it's frost over the page color, opaque.
- **The remote's button lies on art too.** The roots form hands the plain form its `floatingActions` wrapped in the private `SceneFloatingActions`, which provides `LocalAMBackdrop` = the `LocalAMBackdrop` around the scene, or `showing` while there's none, so the frosted + (`amElevatedSurface`, in the scene's own window) samples the art under it by layout position. The order is the reverse of `SceneOverlays`': a scene on one backdrop keeps the + on that backdrop, which lies still under the button while Navigation 3 fades a root's pages and scales one away on predictive back; a scene with none, whose art is each page's own (Music, Photos), lays it on the showing page's art and follows it as roots switch and panels come and go. With neither, it's frost over the page color, opaque.
- **So do the scene's banners.** `SceneRoot` hands the roots form's `AMSceneBackdrops` to its banner host (`BannerHost(…, sceneBackdrops)`), which lays its banner layer alone (the band and the card) on the + route's backdrop: the one around the scene, else `showing` (`Feedback.md`). Never the content, since a toolbar button there picks its chip form by whether it lies on art. The plain form passes none, so its banners lie on the backdrop around the scene.
- **What it builds**, outermost first: it remembers an `AMOverlayRegistry` and provides it as `LocalAMOverlayRegistry` (with `LocalContentColor` = `onSurface`); then the banner host (`BannerHost(modifier.fillMaxSize().background(containerColor), overScreen = false, sceneBackdrops)`, `AMBannerHost`'s own); inside it the internal `ToastHost(fillMaxSize, clearance)`; inside that a `Box(fillMaxSize, propagateMinConstraints = true)` holding `content` (in the roots form, the roots and then `overlays`) and then `floatingActions`, so the remote's button lies over the content and over the panels pushed inside it (pushes are navigation, not overlays), and banners and toasts lie over both.
- **The registry** is what the overlays inside report to and the remote's button fades by (`Overlays.md`, `Actions.md`). A scene nested inside another provides its own, so it's its overlays' nearest owner.
- **The hosts** take every `AMBanner` and `AMToast` posted inside, until a sheet's own take over (`Feedback.md`). With `floatingActions`, the toast clearance is `floatingActionButtonTop()` (the button's 56 dp and the `spacing.lg` it rests above the navigation bar), so a toast shows above the button's resting place, as Material's scaffold places a snackbar above its floating action button; without, it's 0 and toasts sit on the navigation bar.
- **Edge to edge.** It fills what it's given in `containerColor` (the page by default; a scene that draws its own backdrop passes `Color.Transparent`) and pads nothing: the content runs behind the system bars and handles its own insets (`AMPanel`, `AMPageHeader` in a scaffold), as the button, the banner and the toast keep clear of theirs.
- **Back.** The order is by construction (alert → spotlight or remote → sheet → panel stack → landing view → exit): overlays are windows, so they take back first; then the current root's `NavDisplay` pops its panels; then `KeepsPlaceBackHandler` swaps to the landing root (roots form); then back falls through and the app closes. The scene itself registers no handler.
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
- **The scene root builds on it.** `AMScene`'s roots form is an `AMKeepsPlace` filling the scene. Use `AMKeepsPlace` directly for roots that switch inside part of a scene (signed-in views under a sign-in flow).
- Tested: `KeepsPlaceBackTest` pins the decision (another root with no panel or no back stack swaps to the landing; a panel pops first; the landing root, with panels or without, is never this handler's).

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
- **On a picture** its buttons turn into chips (below); on art (an art tone around it) the title is the ink of the art's tone until content scrolls under the bar, then the bar's `onSurface` (`titleContentColor = barLineColor(scrolled, onSurface)`): at rest white on dark art and black on light art (the mode's tone on the library's art), and white on art given the dark tone in light mode.

**`AMToolbarButton(icon, contentDescription: String, onClick, modifier, destructive = false, enabled = true)`**, one form (fixed content). It's the internal `ToolbarButton(…, chip)` with `chip = LocalAMBackdrop.current != null`; `AMArtPanelChrome`'s back passes `chip = true`. `AMPanelChrome`'s back arrow is one.
- **Plain** (the default): `AMIconButton` at its `Medium` size: Material's `IconButton`, 40 dp with a 24 dp glyph in the bar's content color, the round ripple, the 48 dp target, and the long-press tooltip naming it.
- **A chip over art**: inside an `AMBackdrop` (`LocalAMBackdrop` non-null), like `AMArtPanelChrome`'s back button, a `Box` with `minimumInteractiveComponentSize()` and then `amElevatedSurface(CircleShape, Style.Shadow.BarButton)` around the same `AMIconButton`, laid out with `LocalMinimumInteractiveComponentSize` at 0, so the circle hugs Material's 40 dp button and the 48 dp target sits outside it; the glyph is `onSurface` and the ripple is the button's own circle. Its look is the app's surface setting, resolved where the chip stands (`Foundations.md`): the floating surface's fill with the dark-mode hairline in a solid app; frost over the backdrop's art with the frosted edge, or the tint where it's embedded, in a frosted app. `BarButton` is `Low` with a tighter far layer: Material clips each row of a top app bar to its bounds, 12 dp around the circle, where `Low`'s far layer would be cut into a hard edge. On the plain page there's no chip: it's the bare glyph.
- **`destructive`** colors the glyph `AMSemantic.Destructive` (the scheme's `error`). **Disabled** (its own `enabled` or `LocalAMEnabled`): the glyph greys to Material's 0.38 of the content color (never red), and a chip keeps its fill but casts no shadow (`Style.Shadow.None`).
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

- **`AMTabs<T>(selection, onSelectionChange, modifier, enabled = true, tabs: AMTabsScope<T>.() -> Unit)`**: the selection is hoisted; a tab's tap calls `onSelectionChange(id)`, so a change made elsewhere switches the tabs the same way. They switch part of a page, never the app's views (the remote does that).
- **`AMTabsScope<T>`** (internal constructor) declares the tabs, in order, as plain Kotlin, so `if` and loops work: `tab(id, label: String, icon: slot? = null, content)` (the label up to two lines, ellipsized) and the slot form `tab(id, label: slot, icon, content)`, which the string form delegates to. Each becomes an internal `TabEntry`; the scope is rebuilt each composition.
- **The strip** is Material's `SecondaryTabRow` on a transparent container: equal-width `Tab`s (`LeadingIconTab` when the tab has an `icon`, the glyph before the label), `selectedContentColor = onSurface`, `unselectedContentColor = onSurfaceVariant` (Material's secondary-tab colors), `enabled` resolved through `LocalAMEnabled`, Material's divider under the row, its ripple, and its tab semantics (selected or not). The indicator is `TabRowDefaults.SecondaryIndicator` at `tabIndicatorOffset(selected, matchContentSize = false)`, sliding with Material's own motion. A `selection` matching no tab draws no indicator and no content.
- **The content**, lying on the tabs' surface (`ProvideAMOnSurface`, so its secondary text resolves from it and a frosted piece with its own shape in it is embedded and draws the tint, and `ProvideAMSurfaceStyle(AMSurfaceStyle.Flat)`, as inside a card: a card in a tab draws nothing and a flat row paints no fill, so a caller puts rows straight in a tab with `AMDivider(inset = spacing.lg)` between them, never a card): a `Crossfade` keyed on `selection` (`motionScheme.defaultEffectsSpec()`, instant under Remove animations) shows only the selected tab's content, so the other tabs' content isn't composed. It's `fillMaxWidth().fillMaxHeight()` under the strip: given a height, the tabs fill it; in a scrolling column they take their content's height. Content lies at the top start.
- **The surface** under strip and content is one flat shape in `shapes.large`, clipped, with no shadow, in the app's surface setting (`AMTheme.surfaceFill`, read while composing), so the tabs always match the app's cards: in a solid app `background(AMTheme.Surface.primary, shape)` (no hairline); in a frosted app `amElevatedSurface(shape, AMElevation.None)`, whose node reads the setting while it draws and resolves the look where the tabs stand: frost over the nearest `AMBackdrop`'s art with the frosted edge (frost over the page color with no art to sample), and the tint where the tabs are embedded (inside a card or on a sheet). Inside a card (`Modifier.amCard()` on the tabs' modifier) the card supplies the lift.

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
| Toolbar button | `FullBleed`: one plain button (compose) on a surface strip | `FullBleed`: the top of a photo's screen, an `AMBackdrop` of the photo blurred under `AMTheme.Frosted.fill`, holding a "Brooklyn" toolbar whose buttons are chips in the page's surface setting (back; share disabled; delete destructive) over the photo itself (4:3); then the same toolbar (share enabled) in a 160 dp `CatalogFrostedStage`, the chips frosted whatever the setting |
| Panel | a card of three project rows (Atlas, Beacon, Cinder: `ListItem` rows on `AMRowDefaults` with a chevron, dividers between); each pushes its project's `AMPanel`, titled by the project with a Share action, holding an `AMSection` of its open tasks over the project's summary | a card with one album row (the cover, "Cobalt", "Field Notes · 9 songs"), pushing the album over its art: an `AMBackdrop` whose art is the cover's `Modifier.amArtBackdrop` (`DataDisplay.md`), `AMArtPanelChrome` ("Cobalt", "Field Notes · 9 songs · 38 min"), the cover with the `Low` shadow, and a card of nine track rows with their lengths |
| Tabs | `FullBleed`: Overview, Activity, Files on a surface strip, each a line of text on the row recipe | tabs as a card (`amCard()`): Photos, Albums (selected), Shared, each with its glyph, showing three title-and-detail `ListItem` rows with chevrons (`AMRowDefaults`, dividers between); then the same tabs (`libraryTabs()`, declared once) as a card in a `CatalogFrostedStage`, frosted over the photo |
| Page indicator | `FullBleed`: five dots, the third current, on a surface strip | an onboarding card: a `HorizontalPager` of four slides (a glyph in `primary`, a `titleLarge` title, a `bodyMedium` line in `onSurfaceVariant`), the dots, and a full-width Next that pages on and reads Get started on the last slide |

The Panel page's pushes go onto the catalog's own back stack: a row calls `CatalogNavigator.openPanel(PanelEntry, route)` (the project's id, or `album`), and the page wraps itself in `CatalogPanels(page = { ComponentPage(…) }) { route -> … }`, which draws the page or the panel its route names (`app/CatalogRoot.kt`, `Demo.md`). The panels pop with back, predictive back or their back arrow (`navigator.back()`), and come back after process death.

The Page indicator's pager keeps every slide composed (`beyondViewportPageCount`), so it's as tall as its tallest slide and doesn't jump while swiping. Entry icons: `ic_toolbar`, `ic_radio_button_checked`, `ic_right_panel_open`, `ic_tab`, `ic_more_horiz`.

## See also

- `Overlays.md` — the windows the scene's `overlays` present and the registry they report to.
- `Feedback.md` — the banner and toast hosts the scene places.
- `Actions.md` — the remote that goes in `floatingActions`.
