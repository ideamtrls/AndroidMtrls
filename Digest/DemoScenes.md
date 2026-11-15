---
scope: The rules the demo's six Showcase scenes (Social, Music, Photos, Calendar, Shop, Account) share — scene code and entries, the scene root, backdrops and back, panels, headers and the page gutter, copy and sheets, the light/dark chip, the remote, motion, rows and cards, banners and toasts, state and cancellation, photos and art
globs: [demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/scenes/**, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/showcase/**]
keywords: [Scenes, scene rules, Showcase, ShowcasePages, socialScene, musicScene, photosScene, calendarScene, shopScene, accountScene, scene entry, AMScene, roots form, overlays slot, AMKeepsPlace, frosted app, AMTheme(surfaceFill), AMSurfaceFill.Frosted, AMBackdrop, containerColor, Color.Transparent, amStatusBarEdge, AMStatusBarIcons, landing view, back order, NavigationBackHandler, select mode, Exit tile, logout, LocalCatalogNavigator, panels, NavDisplay, route type, SocialRoute, MusicRoute, PhotosRoute, ShopRoute, AMPanel, AMPanelChrome, AMArtPanelChrome, AMPageHeader, Scaffold, exitUntilCollapsedScrollBehavior, page gutter, step screen, sentence case, AMSheet title, amDiscardGuard, SceneAppearanceButton, light/dark chip, remote, AMFloatingActions, AMActionGrid, AMActionGridRow, active tile, tile titles, amInstantSwitch, amCrossfade, motion, AMListRow, AMSection Rows, bare rows, AMDivider, inset divider, field card, ProvideAMOnSurface, AMBanner, AMToast, scene model, rememberSaveable, mapSaver, rememberCoroutineScope, cancelPendingWork, AMLoadingOverlay, FAB clearance, WindowInsets.amFloatingActions, AMRemoteImage, commonsPhoto, catalogPhoto, amArtBackdrop, amArtText]
updated: 2026-11-15
---

# Demo scenes

The rules every Showcase scene follows. A scene is a small, realistic app built only from the library, with its own data, art, copy and flows. Each scene's own model, surfaces and copy are in its `Scene<Name>.md` (`SceneSocial.md`, `SceneMusic.md`, `ScenePhotos.md`, `SceneCalendar.md`, `SceneShop.md`, `SceneAccount.md`).

## Where things live

- Scene code: `demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/scenes/<scene>/`, one file per surface, rooted at an internal `<Name>Scene()` composable in `<Name>Scene.kt`.
- The scene's entry, `<Name>SceneEntry`, is declared in `<Name>Scene.kt`: the `<scene>Scene` id, `CatalogCategory.Showcase`, the scene's title in sentence case, a one-line summary, a demo glyph and keywords, its page `{ <Name>Scene() }`.
- The entries are listed in `catalog/pages/showcase/ShowcasePages.kt`, in this order: Social, Music, Photos, Calendar, Shop, Account.
- The demo is one package, so every type a scene declares (private ones included) and every internal name carries the scene's name (`AccountModel`, `AccountStepPage`, `AccountArticle`, `rememberShopQuickAddIcons`). Private functions and values may go without, since they can't clash across files.
- The light/dark chip, `SceneAppearanceButton()`, lives in `app/Inspection.kt` (`Demo.md`).

## Scenes vs pages

- **Pages** (`catalog/pages/<category>/`) are the per-component reference, each on the `ComponentPage` scaffold (`Demo.md`).
- **Scenes** are whole screens composed from many components across categories, showing how they fit together. A scene's page is the scene itself, never `ComponentPage`: it owns its scrolling, its headers and its overlays.

## Presentation and back

- A scene is a catalog entry pushed over the list (`CatalogNavigator.open`), so Navigation 3 pushes and pops it with predictive back.
- **Its root is `AMScene`'s roots form** (Social, Music, Photos, Calendar, Shop): each root's place is saved and restored. The scene-wide overlays (sheets, alerts, the spotlight, the loading overlay, toasts) go in its `overlays` slot, composed beside the roots so a root switch never closes one (Music, Calendar, Shop; Social and Photos compose theirs inside the roots). Account uses the plain form: signed out its phases fade in place, signed in Profile and Settings are the roots of an `AMKeepsPlace`, and its overlays sit in the scene's content.
- **Every scene is a frosted app**: its root wraps everything, outside its backdrop, in `AMTheme(surfaceFill = AMSurfaceFill.Frosted)` (`Foundations.md`), so the catalog's Surfaces setting (and `--es surfaces solid`) doesn't reach a scene. No scene passes a fill to a card, section, disclosure group, list card, tabs or button: those, and the toolbar chips and the +, take their look from the setting.
- **Backdrops.** A scene with one backdrop behind everything wraps the whole `AMScene` in its `AMBackdrop` and passes `containerColor = Color.Transparent` (Social's gradient, Calendar's month art, Shop's beans, Account's books). Music and Photos give each page its own backdrop over the default page container.
- **Under the status bar.** A root with no top bar puts `Modifier.amStatusBarEdge()` on its scrolling content, so rows fade out under the status bar instead of being cut off (Calendar's months view). The scenes' art is washed toward the mode, so the status bar's icons are the appearance's everywhere and no scene claims them (`AMStatusBarIcons`).
- **Back follows the library's order**: an alert, then the spotlight or the remote, then a sheet, then the current root's panels, then the landing view (an instant swap back to it), then out of the scene to the catalog. A scene adds a back handler (`NavigationBackHandler` from navigationevent-compose) only for a flow or mode of its own: Account's sign-in steps (standing aside on the intro) and Photos' select mode (back ends it first).
- **Every remote keeps an Exit tile** (`ic_logout`, "Exit"), folded into the menu's last row, which closes the menu, cancels the scene's pending work (Music stops its playback; Calendar has none) and calls `LocalCatalogNavigator.current.back()`.

## Panels and route types

- Panels are Navigation 3 entries in a `NavDisplay` per root that pushes (every root of Social, Photos and Shop; Music's Artists), over a hoisted, saved back stack of a scene route type: `SocialRoute`, `MusicRoute`, `PhotosRoute`, `ShopRoute`. Each back stack is the scene's own state, handed to `AMScene`'s `backStack`, so back pops panels before it swaps to the landing view.
- A panel is an `AMPanel`, or `AMPanelChrome` in a `Scaffold` (Photos' detail); a panel over art (Music's artist and album) sits under `AMArtPanelChrome`. Every panel draws its own backdrop and lies transparent on it (`containerColor = Color.Transparent`), so it covers the root it pushes over.
- Account and Calendar push nothing.

## Headers

- **No navigation chrome on roots.** Every root view carries its own `AMPageHeader` (Material's large top app bar, transparent at rest and, once scrolled, the page color or the frosted fill over a backdrop, which keeps scrolled content from reading through the status bar), with `exitUntilCollapsedScrollBehavior()`, in a `Scaffold(containerColor = Color.Transparent, contentColor = onSurface)` over the scene's backdrop, the content scrolling under it.
- The content sits in the page gutter: `spacing.lg` at the sides, `spacing.sm` under a page header.
- The exceptions: Account's intro is a title-less `AMToolbar` holding the chip, over the Scrollies logo; Calendar's months view is titled only by its year section headers; the Music player is named by its cover and controls.
- **A step screen with a back button has one header**: `AMPageHeader(title, navigationIcon = { back }, scrollBehavior = exitUntilCollapsedScrollBehavior())`, the back arrow in the bar's top row and the title under it. A step's subtitle is the first line of its scrolling content (Account's `AccountStepPage`).

## Copy

- **Sentence case everywhere**: titles, sheet titles, buttons, tiles, alerts, section headers, toasts ("Sign in", "Create account", "Forgot password?", "Edit profile", "Sign out?").
- **Sheets take their title from `AMSheet(title = …)`**; a scene never draws its own sheet title.
- **The discard guard** (`Modifier.amDiscardGuard`) goes on a sheet holding a draft only where the scene's flow calls for it: Shop's checkout. Every other sheet closes without asking.

## The light/dark chip

- `SceneAppearanceButton()` flips the shared appearance setting (`Demo.md`). It sits in the landing view's header actions: Social's Feed (before the alerts bell), Music's Artists, Photos' Library (outside select mode; in select mode Done holds the slot), Calendar's month view, Shop's home (before the bag), Account's intro (alone in its top bar) and Profile (the signed-out and signed-in landing views).
- Other root views, panels, headerless views, the remote and its grid never carry it. Scenes never set their own color scheme.

## The remote

- `AMFloatingActions` in `AMScene`'s `floatingActions` slot, `contentDescription = "Actions"`. The scene never places the button itself; the scene root fades it under every overlay.
- The menu is an `AMActionGrid` of `AMActionGridRow`s, exactly 2, 3 or 4 tiles a row, with Exit folded into the last row (Music puts its controls card above its one row). Tiles carry no badges.
- **Tile titles are short**: one or two words ("Profile", "Artists"); short verb phrases only in 2-up rows ("Add to album").
- **View tiles**: the current view's tile is `active`; a tap closes the menu, then switches the root inside `amInstantSwitch`, so the new view appears at once, restored, while the menu fades over it. Action tiles close the menu and act.
- **Clearance**: every scrolling view ends clear of the button with the library's `WindowInsets.amFloatingActions`: a column in a scaffold consumes the scaffold's padding, then pads with it (`consumeWindowInsets(padding).windowInsetsPadding(WindowInsets.amFloatingActions)`); a lazy list takes its bottom content padding from it (`asPaddingValues()`) in place of the scaffold's.

## Motion

Motion comes only from library components, Navigation 3's pushes, `amInstantSwitch` and `Modifier.amCrossfade`. A scene that swaps its own content fades it with `amCrossfade` (Account's sign-in phases, the Music player's cover, Photos' detail page); root swaps are instant. No hand-rolled animation in scene code.

## Rows and cards

- Stacked `AMListRow`s (and `AMLabeledContent`, `AMToggle` and the other value rows) are flat and bare: no dividers between them.
- A card of rows under a header is `AMSection(…, style = AMSectionStyle.Rows, inset = false)` inside the page gutter, never a hand-built header over a card. Rows without a header sit in a `Modifier.amCard()` column, never bare on a backdrop.
- The exceptions, with an inset `AMDivider(inset = spacing.lg)`: Music's album tracks card, between its tracks; Calendar's months view, between the month rows of each year's card; Shop's totals, one before Total; Social's profile tabs (Posts and About), between rows that ride on the tabs' own surface with no card.
- **What a scene lays on a hand-built `amCard`** (text, fields, a progress bar, a stack of rows) goes inside `ProvideAMOnSurface`, since a modifier can't tell its content it lies on a surface (`Foundations.md`); `AMSection` does this for what it holds.
- **A card of text fields is not a row list**: outlined fields carry their own outline, so they stack `spacing.md` apart in a card padded `spacing.lg` (`Modifier.amCard(padding = PaddingValues(spacing.lg))`, or an `AMSection` in its default `Card` style), with no divider between them. A single field sits in the same padded card.

## Banners and toasts

- They go through `AMScene`'s own hosts: a scene posts `AMBanner` and `AMToast` from inside its `AMScene`, so banners come down at the top of the screen and toasts rise above the floating action button. While a sheet is up, a toast shows at the sheet's bottom.
- A toast is a short confirmation, text only (`AMToastItem(text)`). A banner carries news a tap acts on (Social's mute undo, Account's texted code).

## State and cancellation

- **A scene model**: a plain Kotlin class holding Compose state (`@Stable`), remembered at the scene root with `rememberSaveable` and a `Saver` (a `mapSaver` of Bundle values), so what must survive recreation (the current view, the phase, drafts, back stacks, which overlay is up) comes back after a configuration change or process death. Text input lives in `TextFieldState`s. Secrets (a password) and work in flight are never saved.
- **Stand-in requests** are coroutines in the scene's `rememberCoroutineScope`, held as `Job`s by the model. Leaving the scene, back, or a superseding action cancels them (the model's `cancelPendingWork()`, in Social, Photos, Shop and Account), and a `DisposableEffect` at the root cancels them as the scene leaves the composition, so no banner or toast ever lands on another screen; Music's root stops its playback the same way, and Calendar has no requests. A blocking request (Shop's order, Account's sign-in steps) waits behind `AMLoadingOverlay`, which takes back while it's up.
- Samples whose meaning depends on now are built from today (Calendar's events, Shop's orders, Social's "2h" timestamps); Photos' library keeps fixed capture dates.

## Photos and art

- Pictures load through the library's image pipeline (`AMRemoteImage`, `AMAvatar`, the art modifiers) from each scene's own URLs: Wikimedia Commons files through `commonsPhoto` (Social's people and posts, Calendar's month art, Shop's beans and bags, Photos' library, Account's books), and the owner's site (Music's artist and album photos). Music's covers are the exception: the scene reads them from its MP3s' tags itself and hands the bitmaps to the art pieces (`SceneMusic.md`). Scenes never use `catalogPhoto`, so `artwork night` leaves them as they are.
- Art backdrops use the data display modifiers (`amArtBackdrop`, `amArtRows` / `amArtRow`, `amArtText`, `DataDisplay.md`), washed toward the mode at the library's defaults (Shop's beans take a lighter blur and wash, `SceneShop.md`), so text on them takes the fixed ink of the art's tone (white on dark art, black on light art).

## See also

- `Navigation.md` — `AMScene`, its `overlays` and `backStack`, `AMKeepsPlace`, panels.
- `Demo.md` — the catalog, `SceneAppearanceButton`, `catalogPhoto` and `commonsPhoto`.
