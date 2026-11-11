---
scope: The rules the demo's six Showcase scenes (Social, Music, Photos, Calendar, Shop, Account) share — where scene code lives, scenes vs pages, presentation and back, panels and route types, page headers, sentence case, sheets and the discard guard, the light/dark chip, the remote, motion, rows, banners and toasts, state and cancellation, photos
globs: [demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/scenes/**, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/showcase/**]
keywords: [Scenes, scene rules, Showcase, ShowcasePages, socialScene, musicScene, photosScene, calendarScene, shopScene, accountScene, catalog/scenes, scene entry, AMScene, roots form, overlays slot, AMKeepsPlace, amStatusBarEdge, AMLightStatusBarIcons, landing view, back order, Exit tile, logout, LocalCatalogNavigator, panels, NavDisplay, route type, SocialRoute, MusicRoute, PhotosRoute, ShopRoute, AMPanel, AMPanelChrome, AMArtPanelChrome, AMPageHeader, navigationIcon, exitUntilCollapsedScrollBehavior, AMBackdrop, NavigationBackHandler, select mode, step screen, field card, outlined fields, sentence case, AMSheet title, discard guard, amDiscardGuard, SceneAppearanceButton, light/dark chip, remote, AMFloatingActions, Actions, AMActionGrid, AMActionGridRow, active tile, no badges, tile titles, amInstantSwitch, amCrossfade, motion, AMListRow, AMSection Rows, bare rows, inset divider, AMBanner, AMToast, toast above the button, scene model, rememberSaveable, Saver, rememberCoroutineScope, cancellation, cancelPendingWork, FAB clearance, AMRemoteImage, AMAvatar, commonsPhoto, picsum]
updated: 2026-11-10
---

# Demo scenes

The rules every Showcase scene follows. A scene is a small, realistic app built only from the library, with its own data, art, copy and flows. Each scene's own model, surfaces and copy are in its `Scene<Name>.md` (`SceneSocial.md`, `SceneMusic.md`, `ScenePhotos.md`, `SceneCalendar.md`, `SceneShop.md`, `SceneAccount.md`).

## Where things live

- Scene code: `demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/scenes/<scene>/`, one file per surface, rooted at an internal `<Name>Scene()` composable in `<Name>Scene.kt`.
- The scene's entry, `<Name>SceneEntry`, is declared in that folder: the `<scene>Scene` id, `CatalogCategory.Showcase`, the scene's title in sentence case, a one-line summary, a demo glyph and keywords, its page `{ <Name>Scene() }`.
- The entries are listed in `catalog/pages/showcase/ShowcasePages.kt`, in this order: Social, Music, Photos, Calendar, Shop, Account.
- The demo is one package, so every type a scene declares (private ones included) and every internal name carries the scene's name (`AccountModel`, `AccountStepPage`, `AccountArticle`). Private functions and values may go without, since they can't clash across files.
- The light/dark chip, `SceneAppearanceButton()`, lives in `app/Inspection.kt` (`Demo.md`).

## Scenes vs pages

- **Pages** (`catalog/pages/<category>/`) are the per-component reference, each on the `ComponentPage` scaffold (`Demo.md`).
- **Scenes** are whole screens composed from many components across categories, showing how they fit together. A scene's page is the scene itself, never `ComponentPage`: it owns its scrolling, its headers and its overlays.

## Presentation and back

- A scene is a catalog entry pushed over the list (`CatalogNavigator.open`), so Navigation 3 pushes and pops it with predictive back.
- **Its root is `AMScene`'s roots form** (Social, Music, Photos, Calendar, Shop): each root's place is saved and restored, and the scene's own overlays (sheets, alerts, the spotlight, toasts) go in its `overlays` slot, composed beside the roots so a root switch never closes one; a scene with art behind everything wraps the whole `AMScene` in its `AMBackdrop` (Shop's beans). Account uses the plain form, with `AMKeepsPlace` directly under its sign-in flow for the signed-in views.
- **Under the status bar.** A root with no top bar puts `Modifier.amStatusBarEdge()` on its scrolling content, so rows fade out under the status bar instead of being cut off (Calendar's months view). Anything showing dark art at rest outside an art panel claims light status-bar icons with `AMLightStatusBarIcons` (Music's Artists header at rest, the Player); art panels claim them themselves.
- **Back follows the library's order**: an alert, then the spotlight or the remote, then a sheet, then the current root's panels, then the landing view (an instant swap back to it), then out of the scene to the catalog. A scene adds a back handler (`NavigationBackHandler` from navigationevent-compose) only for a flow or mode of its own: Account's sign-in steps (standing aside on the flow's first step) and Photos' select mode (back ends it first).
- **Every remote keeps an Exit tile** (`logout`, "Exit"), folded into the menu's last row, which closes the menu, cancels the scene's pending work (Music stops its playback) and calls `LocalCatalogNavigator.current.back()`.

## Panels and route types

- Panels are Navigation 3 entries in a `NavDisplay` per root that pushes (every root of Social, Photos and Shop; Music's Artists), over a hoisted, saved back stack of a scene route type: `SocialRoute`, `MusicRoute`, `PhotosRoute`, `ShopRoute`. Each back stack is the scene's own state, handed to the `backStack` of `AMScene` or `AMKeepsPlace`, so back pops panels before it swaps to the landing view.
- A panel is built with `AMPanel` / `AMPanelChrome`; a panel over art (Music's artist and album) sits on its art backdrop under `AMArtPanelChrome`.
- Account and Calendar push nothing.

## Headers

- **No navigation chrome on roots.** Every scene view carries its own `AMPageHeader` (Material's large top app bar, transparent at rest and, once scrolled, the page color or the frosted fill over a backdrop, which keeps scrolled content from reading through the status bar), in a `Scaffold` with the content scrolling under it: on the page color (Account), or transparent over the scene's `AMBackdrop` (Social, Music, Photos, Calendar, Shop).
- The exceptions: Account's intro leads with the company's logo; Calendar's months view is titled only by its year section headers; the Music player is named by its cover and controls.
- **A step screen with a back button has one header**: `AMPageHeader(title, navigationIcon = { back }, scrollBehavior = exitUntilCollapsedScrollBehavior())`, Material's large top app bar with the back arrow in its top row and the title under it, collapsing as the page scrolls. A step's subtitle is the first line of its scrolling content.

## Copy

- **Sentence case everywhere**: titles, sheet titles, buttons, tiles, alerts, section headers ("Sign in", "Create account", "Forgot password?", "Edit profile", "Sign out?").
- **Sheets take their title from `AMSheet(title = …)`**; a scene never draws its own sheet title.
- **The discard guard** (`Modifier.amDiscardGuard`) goes on a sheet holding a draft only where the scene's flow calls for it: Shop's checkout. Every other sheet closes without asking.

## The light/dark chip

- `SceneAppearanceButton()` flips the shared appearance setting (`Demo.md`). It sits in the landing view's header actions: Social's Feed (before the alerts bell), Music's Artists, Photos' Library (outside select mode; in select mode Done holds the slot), Calendar's month view, Shop's home (before the bag), Account's intro (alone in its top bar) and Profile (the signed-out and signed-in landing views).
- Other root views, panels, headerless views, the remote and its grid never carry it. Scenes never set their own color scheme.

## The remote

- `AMFloatingActions` in `AMScene`'s `floatingActions` slot, `contentDescription = "Actions"`. The scene never places the button itself; the scene root fades it under every overlay.
- The menu is an `AMActionGrid` of `AMActionGridRow`s, exactly 2, 3 or 4 tiles a row, with Exit folded into the last row (Music puts its controls card above its one row). Tiles carry no badges.
- **Tile titles are short**: one or two words ("Profile", "Artists"); short verb phrases only in 2-up rows ("Add to album").
- **View tiles**: the current view's tile is `active`; a tap closes the menu, then switches the root inside `amInstantSwitch`, so the new view appears at once, restored, while the menu fades over it. Action tiles close the menu, then act.
- **Clearance**: every scrolling view ends with room for the button above the navigation bar, the scene's `<Name>FabClearance` (88 dp: the button's 56 dp with `spacing.lg` under and over it).

## Motion

Motion comes only from library components, Navigation 3's pushes, `amInstantSwitch` and `Modifier.amCrossfade`. A scene that swaps its own content fades it with `amCrossfade` (Account's sign-in phases); root swaps are instant. No hand-rolled animation in scene code.

## Rows

- Stacked `AMListRow`s (and `AMLabeledContent`, `AMToggle` and the other value rows) are flat and bare: no dividers between them.
- A card of rows under a header is `AMSection(…, style = AMSectionStyle.Rows, inset = false)` inside the page gutter, never a hand-built header over a card. On an art backdrop a stack of rows sits in a card.
- Three exceptions: Music's album tracks card puts inset `AMDivider`s between its tracks, Calendar's months view puts them between the month rows of each year's card, and Shop's totals put one before Total.
- **A card of text fields is not a row list**: outlined fields carry their own outline, so they stack `spacing.md` apart in a card padded `spacing.lg` (`Modifier.amCard(padding = PaddingValues(spacing.lg))`), with no divider between them. A single field sits in the same padded card.

## Banners and toasts

- They go through `AMScene`'s own hosts: a scene calls `AMBanner` and `AMToast` from inside its `AMScene`, so banners come down at the top of the screen and toasts rise above the floating action button. A toast posted inside a sheet shows in the sheet.
- Toasts are Material's snackbar: text only, no glyph.

## State and cancellation

- **A scene model**: a plain Kotlin class holding Compose state (`@Stable`), remembered at the scene root with `rememberSaveable` and a `Saver` (a `mapSaver` of Bundle values), so what must survive recreation (the current view, the phase, drafts, back stacks, which overlay is up) comes back after a configuration change or process death. Text input lives in `TextFieldState`s. Secrets (a password) and work in flight are never saved.
- **Stand-in requests** are coroutines in the scene's `rememberCoroutineScope`, held as `Job`s by the model. Leaving the scene, back, or a superseding action cancels them (the model's `cancelPendingWork()`, in Social, Photos, Shop and Account), and a `DisposableEffect` at the root cancels them as the scene leaves the composition, so no banner or toast ever lands on another screen; Music's root stops its playback the same way, and Calendar has no requests. A blocking request (Shop's order, Account's sign-in steps) waits behind `AMLoadingOverlay`, which takes back while it's up.
- Dates are relative to today.

## Photos and art

- Pictures load through the library's image pipeline (`AMRemoteImage`, `AMAvatar`, the art modifiers) from each scene's own URLs: Wikimedia Commons files through `commonsPhoto` (Social's people and posts, Calendar's month art, Shop's beans), picsum's images by id (Photos), and the owner's site (Music). Scenes never use `catalogPhoto`, so `artwork night` leaves them as they are.
- Art backdrops use the data display modifiers (`amArtBackdrop`, `amPageBackdrop`, `amArtRows` / `amArtRow`, `amArtText`, `DataDisplay.md`).
