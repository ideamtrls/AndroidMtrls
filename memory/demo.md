---
scope: The :demo catalog app — registry-driven component pages, navigation shell, home tile grid, and the shared catalog helper composables
globs:
  - demo/src/main/kotlin/io/github/jmsdnns/androidmtrls/demo/catalog/**
  - demo/src/main/kotlin/io/github/jmsdnns/androidmtrls/demo/home/**
  - demo/src/main/kotlin/io/github/jmsdnns/androidmtrls/demo/navigation/**
  - demo/src/main/kotlin/io/github/jmsdnns/androidmtrls/demo/pages/**
  - demo/src/main/kotlin/io/github/jmsdnns/androidmtrls/demo/MainActivity.kt
  - demo/src/main/kotlin/io/github/jmsdnns/androidmtrls/demo/DemoApplication.kt
  - demo/src/main/AndroidManifest.xml
  - demo/src/main/res/**
keywords: [demo, catalog, ComponentEntry, CatalogRegistry, ComponentSection, ComponentPage, CatalogHomeScreen, EntryTileGrid, SceneCarouselBand, SceneCard, AppTitleHeader, DemoTopBarHost, ScenePanelChrome, DemoNavHost, MainActivity, navigation, entryId, catalogGroup, catalogFlatDemo, catalogSwatch, Scenes, scenesEntries, DemoApplication, SingletonImageLoader, Coil User-Agent interceptor, LocalDemoImmersive, immersive shell flag, dark mode toggle, Material You accent transform (no demo toggle), visual similarity, ownsScaffold, overlay attachment, AMSelectionCheckBox]
updated: 2026-07-10
---

# Demo

The `:demo` app is a data-driven catalog: a flat registry of `ComponentEntry`
values rendered as a home tile grid plus one navigable page per component.
It aims for a look similar to its iOS sibling
(`../iOSMtrls/Demo/Sources/**`) and shares the pair's signature look; it reuses
the catalog strings verbatim (except "iOSMtrls"→"AndroidMtrls" and Material icon
substitutions per the demo spec §4 table), treats iOS pt as dp/sp, and follows
the same per-page group structure — while its navigation, gestures, and
implementation are Android-native.
Build/run commands and the demo's dependencies live in `build.md`.

Package root: `demo/src/main/kotlin/io/github/jmsdnns/androidmtrls/demo/`.
Subpackages: `catalog/` (registry + scaffold + helpers), `home/` (home screen +
top bar), `navigation/` (NavHost), `pages/` (one subpackage per section, except
Foundations which is the single file `pages/Foundations.kt`), `scenes/`
(the five full-screen scenes and the shared scene toolkit in `scenes/common/`,
plus `scenes/ScenesEntries.kt` — the Screens-section registry list; the scenes
themselves are documented in `scenes.md`).
`AndroidManifest.xml` registers `.DemoApplication` (a
`SingletonImageLoader.Factory` installing a Coil `ImageLoader` whose OkHttp
interceptor sends a descriptive `User-Agent` — Wikimedia's avatar host returns
HTTP 403 to Coil/OkHttp's default UA, so the Social feed's portrait avatars need
it; app-level network config, library untouched), one activity (`MainActivity`,
`Theme.Material.NoActionBar`), `INTERNET` (Coil remote images), and
`android:enableOnBackInvokedCallback="true"` on `<application>` (predictive-back
opt-in; navigation-compose drives the pop transition from the back gesture);
there is no `res/` directory — all chrome is composed, no XML resources.

## App shell (MainActivity.kt)

`MainActivity` calls `enableEdgeToEdge()` then sets `DemoApp()`:

- `AMThemeRoot { AMTokensProvider(accent)? { CompositionLocalProvider(LocalAMIsDarkTheme provides isDark) { … } } }`
  — `isDark` is `rememberSaveable`, seeded from `isSystemInDarkTheme()`, and
  hoisted here, **above the NavHost**, so the top-bar toggle flips the whole
  catalog at once. There is no per-page scheme override (deliberate deviation
  from the iOS per-page `schemeOverride`).
- The Material You accent transform (`AMTokensProvider` re-tinting only
  `semantic.accent` from `android.R.color.system_accent1_*`) is a library
  capability but the demo exposes NO toggle for it (iOS has no such button).
  Stock accent is the adaptive iOS blue pair.
- A `Column` (page-color background) stacks `DemoTopBarHost` over `DemoNavHost`.
- `DemoTopBarHost` (private) observes `currentBackStackEntryAsState()` and
  renders the bar ONLY on pushed component pages: home shows no bar at all (the
  home body takes its own `statusBarsPadding`), Screens entries hide it too
  (scenes own their chrome), and pages show the app name "AndroidMtrls
  Components". It is ALSO hidden entirely while the shell `immersive` flag is set:
  `MainActivity` hoists an `immersive` boolean and exposes a stable remembered
  setter through `LocalDemoImmersive: (Boolean) -> Unit` (the `LocalSceneExit`
  demo-shell pattern). The Panel catalog page toggles it so its own "Panel" bar
  isn't stacked under the shell's catalog bar — `ownsScaffold` alone can't cover a
  sibling bar rendered ABOVE the NavHost, so the bar must be hidden, not covered.
- The pushed-page bar is the **Android panel-bar anatomy**, reusing
  `ScenePanelChrome` (scenes/common) — the same app bar shared by the library
  `AMPanel` and the scene panels: a borderless back arrow (`AMIconButton`,
  `AMIconButtonSize.Medium`, `ArrowBack`, "Back"), a left-aligned single-line
  ellipsized title (`AMTheme.Type.title`, `weight(1f)`, the app name "AndroidMtrls
  Components"), a 64dp bar, and a full-bleed `AMDivider()` hairline under it. The
  shell owns the top inset — `DemoTopBarHost` wraps the chrome in
  `Modifier.statusBarsPadding()` (`ScenePanelChrome` applies none) and the shell
  `Column` paints the page background behind it. The global LightMode/DarkMode
  dark toggle rides the chrome's trailing slot as a **borderless** `AMIconButton`
  (default Medium, matching the back arrow — no surface chip).

## Navigation (navigation/DemoNavHost.kt)

Two destinations, constants exported from this file:
`ROUTE_HOME = "home"`, `ROUTE_PAGE_PREFIX = "page"`,
`ROUTE_PAGE = "page/{entryId}"`, `ARG_ENTRY_ID = "entryId"`.

- Home composes `CatalogHomeScreen(onEntryClick = { navigate("page/${entry.id}") })`.
- Material shared-axis X transitions: each destination slides 1/10 of the
  width along the horizontal axis while cross-fading (fade-in delayed 90ms so
  the outgoing content clears first), all on `tween(300, FastOutSlowInEasing)`
  slides. Push moves forward (page enters from `it / 10`, exits to `-it / 10`);
  pop mirrors it (page popEnter from `-it / 10`, popExit to `it / 10`). Home
  carries the "back" half — exit to `-it / 10`, popEnter from `-it / 10`.
  Predictive back drives the pop from the gesture.
- The page destination extracts `entryId`, looks up `CatalogRegistry.byId`
  (silently no-ops on unknown id), then:
  - **Screens section**: `entry.content()` raw — full-screen, no `ComponentPage`
    scaffold, no top bar; a scene manages its own chrome and exit affordance. The
    raw content is wrapped in `CompositionLocalProvider(LocalSceneExit provides
    { navController.popBackStack() })` so a parameterless scene's Exit tile pops
    back to the catalog (the shell's half of the `LocalSceneExit` contract —
    scenes.md).
  - **`ownsScaffold` entries**: `entry.content()` raw, top bar stays — the page
    composable calls `ComponentPage` itself so an overlay host can wrap the
    scaffold (see "Overlay pages" below).
  - **Everything else**: `ComponentPage(entry, showsTitle = entry.showsTitle) { entry.content() }`.

Entries navigate **by id string only** — `ComponentEntry.content` lambdas never
travel through NavBackStackEntry arguments (not serializable); the registry
lookup is the bridge.

## Registry (catalog/)

`ComponentEntry` (`@Stable` data class): `id` (kebab-case
`"<section>-<component>"`, e.g. `"content-tag"`, `"buttons-fab"` — the nav
route key), `section: ComponentSection`, `title`, `tileTitle?`
(`effectiveTileTitle` falls back to `title`; tile titles are the short iOS strings — "Text",
"Segmented", "Reorder", "Move", "Stack", "Chips", "Indicator", "Loading",
"Actions", …), `subtitle?` (one-line pitch, iOS verbatim), `intro?` (carried
on the entry like iOS but NEVER rendered anywhere), `icon: ImageVector?`
(Material icon, tile glyph; `null` falls back to `AutoAwesome` — iOS
"sparkles"), `showsTitle: Boolean = true`, `ownsScaffold: Boolean = false`
(entry's page calls `ComponentPage` itself — see "Overlay pages" below),
`content: @Composable () -> Unit`.

`ComponentSection` is a 10-case enum with curated `allCases` order: Screens,
Foundations, TextInputs, Buttons, Content, Pickers, Frames, Overlays, Lists,
Collections. `displayName` special-cases `Screens` → "Scenes". The Screens
section is fed by `scenesEntries` (`scenes/ScenesEntries.kt`) — the five scene
entries in home display order (Social, Now Playing, Gallery, Calendar, Auth),
each scene's `val <name>SceneEntry` living beside its scene file, prepended FIRST
in the concatenation; the scenes themselves are documented in scenes.md. The
catalog shares the iOS grid's groups, order, and tile
titles, with no Android-only registry slots. Content carries the 12
iOS pages (Avatar … Backdrop Blur … Empty State); the backdrop blur rides the
Content "Blur" page (`BackdropBlurPage`, iOS `IMBackdropBlurPage`) via the
public `AMBackdropBlur` composable (a tint-free blur of its wrapped content; the
raw `amBackdropBlur` Modifier stays internal), and the selection checkmark rides
the Pickers "Select" page
(`SelectionCheckPage`, iOS `IMSelectionCheckPage`, `AMSelectionCheckBox`). There
are deliberately NO PageHeader/SectionHeader catalog pages (those components are
shown live on the Typography page instead). The library's `AMFloatingActionsHost`
and `AMPausedSubstrate` stay public and are exercised inside the Screens scenes
(scenes.md), not as standalone catalog pages.

`CatalogRegistry` (object) holds `val all: List<ComponentEntry> by lazy { … }`
— a concatenation of one per-section list, `scenesEntries` first (Screens leads)
then one per `pages/` package (`scenesEntries +
foundationsEntries + textInputsEntries + buttonsEntries + contentEntries +
pickersEntries + framesEntries + overlaysEntries + listsEntries +
collectionsEntries`) — and `val byId = all.associateBy { it.id }`.

## Adding a component page (the recipe)

1. Create `pages/<section>/<X>Page.kt`. It contains exactly two things:
   a top-level `val <x>Entry = ComponentEntry(id = "<section>-<x>", section = …,
   title, subtitle, intro, icon, content = { <X>Page() })` and
   `@Composable fun <X>Page()` whose body is just `catalogGroup` blocks (plus
   `Spacer`s). **Pages are body-only — never call `ComponentPage` inside a
   page**; `DemoNavHost` owns the scaffold and wraps `entry.content()` itself
   (calling it again double-renders the header and nests scrolls). Sole
   exception: `ownsScaffold = true` entries, where the page calls
   `ComponentPage` exactly once, wrapped in its overlay host.
2. Append `<x>Entry` to that section's `val <section>Entries:
   List<ComponentEntry>` in `pages/<section>/<Section>Entries.kt` (display
   order = list order). Foundations keeps entries and pages
   together in `pages/Foundations.kt`.
3. The iOS page (`../iOSMtrls/Demo/Sources/Catalog/Pages/<Section>.swift`) is a
   look/composition reference — follow its group breakdown and reuse its
   strings so the pages read as siblings; interaction and behavior follow
   Android norms.
4. When mirroring an iOS `HStack`, set `verticalAlignment =
   Alignment.CenterVertically` on the `Row` — SwiftUI HStacks center by
   default, Compose Rows top-align, and the mismatch shows whenever a row
   mixes compact and 64dp components (precedent: ButtonStylesPage ghost
   buttons).

**Overlay pages** come in two attachment shapes:

- **Wrap-content overlays** (Toast, Banner, Sheet) wrap their page body in the
  AM host at the top of the page content (`AMToast(item, onDismiss) {
  …groups… }`).
- **Full-coverage hosts** (ActionsOverlay, Alert, Loading, and
  Panel/`AMPanelHost` — any host whose overlay/blur/slide-in must fill the
  viewport) set `ownsScaffold = true` on the entry and wrap `ComponentPage`
  itself inside the page composable
  (`AMActionsOverlay(…) { ComponentPage(entry) { …groups… } }`, and likewise
  `AMAlert`, `AMLoadingOverlay`, and `AMPanelHost`); the iOS twin likewise
  applies its overlay modifier outside `ComponentPage`. Inside the scaffold's
  scroll column height constraints are unbounded, so a `fillMaxSize` host
  attached there collapses to wrap-content and the overlay clips to the page
  body — and a `fillMaxSize` panel measured under that unbounded height
  **crashes** (`AMPanelHost`), so a full-coverage host MUST own its scaffold.
  With the host around the scaffold, the deliberate `amBackdropBlur` bleed
  (surfaces.md) also washes the top bar. The **Panel** page additionally drives
  the shell `LocalDemoImmersive` flag (a `LaunchedEffect(showPanel)` sets it
  while the panel is open; a `DisposableEffect` resets it on leaving the back
  stack) so the shell's catalog top bar HIDES rather than stacking above the
  panel's own "Panel" bar; its numbered list renders solid
  `AMTheme.Semantic.info` circle badges (22dp `CircleShape` with a centered
  white bold 13sp numeral, white in both schemes), mirroring iOS's
  `\(id).circle.fill`.

## ComponentPage scaffold (catalog/ComponentPage.kt)

`ComponentPage(entry, showsTitle = true, content)` renders: page-color
full-size `Box` → vertically scrollable `Column` (`navigationBarsPadding`,
vertical lg padding, `spacedBy(xl)` between children — iOS VStack spacing) →
`catalogCategoryHeader(entry.title)` (only when `showsTitle`) → `content()`.
It does NOT render `entry.intro` (neither does iOS). `showsTitle = false`
is used only by the Typography entry (every header on that page IS a
specimen). It reads `LocalAMIsDarkTheme` — it never manages its own dark
override.

## Home screen (home/CatalogHomeScreen.kt)

Mirrors iOS `CatalogRoot`: a `LazyColumn` over a page-color box —
`AppTitleHeader` ("AndroidMtrls Components", 34sp Bold, the iOS large-nav-title
stand-in; no subtitle), then one `SectionBand` per `ComponentSection.allCases`
entry that has registered entries (empty sections skipped). Each band =
`AMSectionHeader(displayName)` padded
horizontal lg, then a body that special-cases the section: `Screens` renders
`SceneCarouselBand`, every other section renders `EntryTileGrid`.
`EntryTileGrid` builds rows via `entries.chunked(columns)` (no nested lazy
grid), 3 columns — **2 for Lists** (iOS "deliberate 2×2") — md gaps both
axes, horizontal lg padding, each cell a weight(1f) `AMActionGridButton(icon
?: AutoAwesome, effectiveTileTitle, elevation = AMElevation.None)`; incomplete
last rows are padded with weight(1f) Spacers.

`SceneCarouselBand` (this file, private) is the scenes band: an `AMCarousel`
of 140dp-tall cards with an `AMPageIndicator` beneath; carousel index is
`rememberSaveable { mutableIntStateOf(0) }`. The pager clips to its bounds,
so the band wraps the carousel in a `BoxWithConstraints(fillMaxWidth,
contentAlignment = Center)` to read the band width, then widens the pager to
`requiredWidth(maxWidth + 2·(xxl − lg))` and shifts it with `offset(x =
−(xxl − lg))` (Center places the oversized child at `−(xxl − lg)`; the offset
adds a second `−(xxl − lg)`) while `sideInset = xxl + (xxl − lg)` (= `2·xxl −
lg`) holds each card at its original size. Net: the resting card sits flush
with the `lg` page margin and the pager's right bound lands on the screen
edge, so the right-edge peek paints all the way out. `SceneCard` (same file,
private) = shadowless `AMCard(elevation = None)` + explicit unconditional
`amHairline` (both schemes), icon top-start (24dp, Text.primary),
`Spacer(weight 1f)`, title (`Type.title`), one-line subtitle
(`Type.secondary`, maxLines 1, ellipsis); a no-ripple `clickable` (indication
null) routes taps through the shared `onEntryClick`.

## Catalog helpers (catalog/Helpers.kt)

All public top-level `@Composable` functions, Foundation-only. The two
headers dogfood the real library components (like iOS):

- `catalogCategoryHeader(title)` — `AMPageHeader(title)` with top md +
  horizontal lg padding (iOS catalogCategoryHeader).
- `catalogGroup(title, content)` — `AMSectionHeader(title)` inset horizontal
  lg + md gap above the flush content slot; the workhorse on every page
  (content decides its own horizontal padding).
- `catalogFlatDemo(padding: Dp = Spacing.lg.dp, content)` — full-bleed
  `Surface.primary` strip; the padding applies HORIZONTALLY only (iOS
  EdgeInsets default has zero vertical); pass `0.dp` for self-padding
  widgets. Backs the "Flat"/"Card" page pattern on pickers/lists pages. The
  text-input pages do NOT use it for their labeled groups — the filled fields
  are their own white surface, so label+field pairs sit directly on the page
  (a white strip would swallow the field's silhouette and make the standing
  label read as if it were inside the field).
- `catalogSwatch(name, color, textColor = null, showAsText = false)` — Row:
  44×32dp chip leading, 15sp monospace primary-color name, trailing Spacer;
  `showAsText` renders "Aa" (20sp Bold) for text-color tokens.
- `catalogSpacingChip(name, value: Float)` / `catalogRadiusChip(name, value:
  Float)` — chip + two text nodes: name (11sp monospace, primary) and the
  bare integer value (11sp, secondary; no "dp" suffix — iOS shows none).
- `catalogElevationChip(name, level: AMElevation)` — 80×56dp surface rect,
  6dp label gap.
- `catalogTypeRow(name, style: TextStyle)` — 90dp label column + sample +
  "Roboto · NNsp" metadata line (iOS "FontName · NNpt").

There are no code-snippet/variant-row/playground helpers — iOS has no "Show
code" or playground chrome, and no demo page renders any.

## See also

- `scenes.md` — the five full-screen Screens scenes and the shared scene
  toolkit the shell hosts (the `scenesEntries` this file wires up).
- `build.md` — modules, demo build/install commands, demo dependencies.
- `conventions.md` — AM prefix and authoring rules for the library APIs the
  pages demonstrate.
