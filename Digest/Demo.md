---
scope: The demo catalog app — the activity and Navigation 3 root, the catalog list and its search, the CatalogCategory/ComponentEntry registry, the ComponentPage scaffold (preview, loaded — nothing else on a page), stage layouts, inspection controls, the library theme at the root, debug launch extras, sample photos, and the recipe for adding a component page.
globs: [demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/app/MainActivity.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/app/CatalogRoot.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/app/CatalogList.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/app/Inspection.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/scaffold/CatalogCategory.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/scaffold/ComponentEntry.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/scaffold/ComponentPage.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/scaffold/Stage.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/scaffold/LaunchOptions.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/scaffold/CatalogPhoto.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/scaffold/ScreenHost.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/**, demo/src/main/res/drawable/**, demo/src/main/AndroidManifest.xml]
keywords: [Demo, catalog, MainActivity, enableEdgeToEdge, SystemBarStyle, CatalogRoot, NavDisplay, Navigation 3, back stack, CatalogKey, CatalogKey.Panel, CatalogNavigator, openPanel, CatalogPanels, LocalCatalogNavigator, predictive back, listSaver, CatalogList, AMPageHeader, LargeTopAppBar, exitUntilCollapsedScrollBehavior, AMSectionHeader, search, SearchBarState, ExpandedFullScreenSearchBar, SearchBar, InputField, ListItem, subheader, CatalogCategory, componentEntries, ComponentEntry, CatalogRegistry, FoundationsPages, DataDisplayPages, CollectionsPages, ShowcasePages, entries, id convention, ComponentPage, NoPreview, Preview, Loaded, PageSection, Stage, StageLayout, Floating, FullBleed, Wide, Fill, PageGutter, bleed, LocalCatalogTarget, CatalogAutoPresent, CatalogScreenHost, AMBannerHost, AMToastHost, no launcher buttons, Inspection, Inspection.isDark, InspectionRoot, InspectionMenu, SceneAppearanceButton, InspectionAppearance, InspectionTextSize, InspectionLayoutDirection, LocalInspection, SharedPreferences, fontScale, LocalDensity, LocalLayoutDirection, AMTheme, darkTheme, AMTheme.isDark, AMTheme.Surface, LaunchOptions, entry, scrollTo, present, demoTime, LaunchOptions.clock, shopFillsCheckout, appearance, rtl, artwork, catalogPhoto, commonsPhoto, CatalogArt, picsum, Wikimedia Commons, ic_ glyphs, adding a component page]
updated: 2026-11-10
---

# Demo

`demo/` is the catalog app: a searchable list of every component grouped by category, a page per component (a simple live preview and one loaded composition, nothing else), and the six Showcase scenes. It dogfoods the library wherever a library piece fits: `AMTheme` at the root, `AMPageHeader` on the list and every page, `AMSectionHeader` over the list's categories. Each category's pages are listed in its own Digest file (`Foundations.md`, `Layout.md`, `Actions.md`, `Navigation.md`, `Feedback.md`, `Overlays.md`, …; the card stack and carousel pages in `Collections.md`); a category with no entries is hidden from the list. The scenes' shared rules are in `DemoScenes.md`, each scene's own in its `Scene<Name>.md`.

## Where things live

All Kotlin is in **one package, `com.ideamtrls.androidmtrls.demo`**; folders under `demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/` group the files. Being one package, every file-scope helper in a page is `private`, and two files' top-level classes with the same name collide even when both are private (private functions and values don't): give a page's private sample types a page or category prefix (`ListsMessage`, not `Message`).

| Path | What lives there | Who edits it |
|---|---|---|
| `app/MainActivity.kt` | `MainActivity`: reads the launch extras, edge to edge, system bar icons following the applied appearance, `InspectionRoot { CatalogRoot() }` | shared |
| `app/CatalogRoot.kt` | `CatalogKey`, `CatalogNavigator`, `LocalCatalogNavigator`, `CatalogRoot` (the Navigation 3 back stack) | shared |
| `app/CatalogList.kt` | `CatalogList` (the list screen and its search) and the private `CatalogSearchField`, `CatalogSections`, `CatalogSubheader`, `CatalogRow` | shared |
| `app/Inspection.kt` | `InspectionAppearance`, `InspectionTextSize`, `InspectionLayoutDirection`, `Inspection` and its `isDark()`, `LocalInspection`, `InspectionRoot` (which installs `AMTheme`), `InspectionMenu`, `SceneAppearanceButton` | shared |
| `catalog/scaffold/` | `CatalogCategory.kt`; `ComponentEntry.kt` (`ComponentEntry`, `CatalogRegistry`); `ComponentPage.kt` (`ComponentPage`); `Stage.kt` (`StageLayout`, `Stage`, `PageSection`, `LocalCatalogTarget`); `LaunchOptions.kt` (`LaunchOptions`, `CatalogAutoPresent`); `CatalogPhoto.kt` (`catalogPhoto`, `commonsPhoto`, `CatalogArt`); `ScreenHost.kt` (`CatalogScreenHost`) | shared |
| `catalog/pages/<category>/` | one `<Category>Pages.kt` (the category's `entries`) plus one `<Name>Page.kt` per component. Folders: `foundations`, `actions`, `inputs`, `selection`, `navigation`, `layout`, `lists`, `feedback`, `overlays`, `datadisplay`, `collections` (its `CollectionsPages` list at the end of Data display), `showcase` | that category only |
| `catalog/scenes/<scene>/` | each Showcase scene's files (`DemoScenes.md`) | scene work only |
| `demo/src/main/res/drawable/` | the demo's own glyphs, `ic_<name>.xml` | shared |

## The activity — `MainActivity`

- `onCreate` reads the launch extras (`LaunchOptions.read(intent)`), calls `enableEdgeToEdge()`, and builds an `Inspection` over the `inspection` SharedPreferences.
- Its content computes the applied dark flag (`inspection.isDark()`) and, whenever it changes, calls `enableEdgeToEdge` again with `SystemBarStyle.auto(TRANSPARENT, TRANSPARENT) { dark }` for both bars, so the status and navigation bar icons follow the inspection appearance, not only the system's.
- Then `InspectionRoot(inspection) { CatalogRoot() }`.

## Navigation — `CatalogRoot`

- A Navigation 3 `NavDisplay` over a `SnapshotStateList<CatalogKey>` back stack. The root is `CatalogKey.Catalog` (the list); each pushed page is `CatalogKey.Entry(id)`, a Showcase scene included (its entry's `page` is the scene itself).
- A page pushes its own panels as `CatalogKey.Panel(id, route)` through `CatalogNavigator.openPanel(entry, route)`; the entry's page is built through `CatalogPanels(page, panel)`, which draws the page itself or the panel its route names (the Panel page's project and album panels).
- `CatalogNavigator` (provided as `LocalCatalogNavigator`) has `open(entry)`, which pushes, and `back()`, which pops anything above the list. `open` and `openPanel` push a key only if it isn't already on the stack, so a double tap opens one page. `NavDisplay`'s `onBack` is `back()`, so system back and predictive back pop pages with Navigation 3's own transitions; back from the list leaves the app.
- The back stack is `rememberSaveable` through a `listSaver` of (id, route) string pairs (ids that aren't registered drop out on restore), so it survives configuration changes and process death. `NavDisplay`'s default entry decorators keep each covered page's saved state, its scroll position included.
- The `entry` extra starts the back stack as the list plus that entry's page; an unknown id opens the list alone.

## The list — `CatalogList`

- A `Scaffold` on the page color (`AMTheme.Surface.page`, content color `onSurface`) under **`AMPageHeader`** (Material's `LargeTopAppBar`) titled "AndroidMtrls", collapsing on scroll (`exitUntilCollapsedScrollBehavior`), transparent at rest and the page color once content scrolls under it. Actions: search, then `InspectionMenu`.
- A `LazyColumn` with, for each category that has entries, a **subheader** (`AMSectionHeader`: `titleSmall` in `colorScheme.primary`, Android's list-subheader look, heading semantics; 16 dp at the sides, 16 above, 8 below) and then one **Material `ListItem`** per entry, the interactive `ListItem(onClick)` with its ripple: the entry's glyph, its full title, its summary on one line (ellipsized), on a transparent container so it lies on the page. Categories with no entries don't show. Rows come from `CatalogRegistry.all`, so the registry's debug checks run on launch.
- **Search** is Material's: the search action expands a `SearchBarState` into an `ExpandedFullScreenSearchBar`. While search is showing, a collapsed Material `SearchBar` stands under the status bar, so the full-screen search opens out of it and closes back into it. The input field (`SearchBarDefaults.InputField` on a saved `TextFieldState`) has a back arrow that closes the search and a clear button while there's text; system back and predictive back close it too. Results use the same subheaders and rows, filtered by `ComponentEntry.matches` (title, summary, category title and keywords, ignoring case and accents); with no matches the search shows `AMEmptyState` at the top of the search window, 16 dp in from the sides: the search glyph, "No results", and "Nothing matches “query”. Try a component name or an API like AMButton.", so the keyboard never covers it. The search is a window over the catalog, so tapping a result snaps it closed, then opens the page; the query stays for the next search.

## Registry

- **`CatalogCategory`** (`scaffold/CatalogCategory.kt`): `Foundations`, `Actions`, `Inputs`, `Selection`, `Navigation`, `Layout`, `Lists`, `Feedback`, `Overlays`, `DataDisplay`, `Showcase`. Declaration order is list order. Each has a `title` in sentence case ("Data display") and an `icon` (a demo glyph). `componentEntries` switches to the category's `<Category>Pages.entries`; Data display's ends with `CollectionsPages.entries` (Card stack, Carousel).
- **`ComponentEntry`** (`scaffold/ComponentEntry.kt`): `id`, `category`, `title` (full component name in sentence case, no abbreviations; also the page's top bar title), `summary` (one line), `icon` (`@DrawableRes`), `keywords`, and `page`, the composable that builds the entry's page. Equality and hash are on `id`.
- **Id convention**: the lowerCamelCase of the title: "Button styles" → `buttonStyles`, "Floating action button" → `floatingActionButton`. Showcase scenes take a `Scene` suffix so they never collide with a component: "Calendar" → `calendarScene`, "Photos" → `photosScene`.
- **`CatalogRegistry`**: `all` (every category's entries, in category order) and `entry(id)`. Debug builds fail fast on an entry listed under the wrong category and on a duplicate id.
- **`<Category>Pages`** (`pages/<category>/<Category>Pages.kt`): an `internal object` with `val entries: List<ComponentEntry>`, flagship component first.

## The page scaffold — `ComponentPage`

```kotlin
ComponentPage(entry, previewLayout = StageLayout.Floating, loadedLayout = StageLayout.Floating, preview = { … }, loaded = { … })
```

Both layouts default to `Floating`; `loaded` is the trailing lambda. Top to bottom:

1. **The top app bar**: `AMPageHeader` titled `entry.title`, with an `arrow_back` navigation icon ("Back", `navigator.back()`) and `InspectionMenu` as its action; collapsing on scroll, transparent at rest, the page color when scrolled.
2. **The summary**: `entry.summary` in `bodyLarge` (regular weight), `onSurfaceVariant`, inset by the page gutter.
3. **Preview**: `PageSection("Preview") { Stage(previewLayout) { preview() } }`, the component once in its simplest default setup.
4. **Loaded**: `PageSection("Loaded") { Stage(loadedLayout) { loaded() } }`, one fuller composition showing most of the component's main ideas at once, as a believable fragment of an app screen.

The page is a `Scaffold` on the page color (`AMTheme.Surface.page`) with a vertically scrolling column; the scaffold's padding sits inside the scroll, so content passes under the top bar and the navigation bar. 8 dp above the summary, 32 dp between the blocks and below the last. A page that passes no `preview` shows the Loaded section alone (the default `preview` is a sentinel, `NoPreview`). The page holds no demo state.

**A page is the widget and nothing else.** A catalog page holds exactly the title, the summary, the Preview and the Loaded composition. It has no section headings of its own, no parameter tables, no accessibility notes, no related links, no captions or explanations, and no code: no code samples, no type or symbol names, no monospaced spans. The API reference is the library's KDoc, accessibility facts and every other reference live in `Digest/`, and navigation lives in the list. Anything a demo can't show stays off the page. Another section is rarely wanted: ask the owner before adding a section, footer or note to `ComponentPage` or any page.

Building blocks (`scaffold/Stage.kt`):
- **`PageSection(title, modifier) { }`**: content inset by `StageLayout.PageGutter` (16 dp), with no header. Its title only names it: it provides `LocalCatalogTarget` (the lowercased title, `preview` or `loaded`) to its content, and `ComponentPage` records where it sits for `scrollTo`.
- **`Stage(layout) { }`**: the content placed directly on the page per its `StageLayout`, in a column (start-aligned for `Floating`, centered otherwise), with no frame around it. Surfaces (buttons, cards) sit bare, the way an app places them; flat widgets (rows, inputs, pickers) get their container (`Modifier.amCard()`, `AMSection`, a surface-colored strip) inside the demo itself.

### Stage layouts — `StageLayout`

**The demo rule:** a demo is never inside a box: no hairline frame, no outlined or transparent container. Every demo (the Preview, the Loaded composition) is EITHER **(a) a flat region**, full-bleed and edge to edge, on the surface color, OR **(b) content floating directly on the page** within the page gutters: cards, filled buttons, chips and other self-surfaced components with nothing around them; flat content that isn't a surface goes in a card via `Modifier.amCard()`. Text inside a demo sits on a surface (a surface strip or a card), never on bare page; the exception is a field's supporting line beside a carded input. A component that can live either flat or in a card previews flat and shows the card inside its Loaded composition; components that are cards by nature (cards, list cards, the filled button styles, action grid tiles) show the card form only, and strips that only live flat (toolbars, page and section headers) show the flat form only, at `FullBleed`.

| Layout | Placement | Use it for |
|---|---|---|
| `Floating` (default) | full width within the page gutters, start-aligned, no padding: a row of tiles or chips starts at the start gutter | surfaces, and flat content wrapped in its card — option (b) |
| `FullBleed` | widened past the gutters by `PageGutter` on each side, so it runs edge to edge of the screen | option (a): flat strips whose content carries the surface color as its background — the Preview of a component that lives flat or in a card |
| `Wide` | widened past the gutters exactly like `FullBleed` | a component that carries its own side insets and floats its own surfaces: the action grid (its tiles land on the gutters), the reorderable card list (its cells' margins are the gutter), the carousel |
| `Fill` | edge to edge like `FullBleed`, on a band of `AMTheme.Surface.primary` the scaffold paints, clipped to it | content that is itself a small screen, such as a floating action hub's scene or the top of a list screen under its page header. The band is the screen |

The widening is done in layout (a private `bleed` layout modifier measures the content two gutters wider and places it a gutter to the start; its parent still sees its own width), never with negative padding or a frame around the demo. Only `Fill` clips, so shadows fall onto the page. A demo's own container (a surface background, `Modifier.amCard()`) belongs to its content, in its lambda.

### Overlays on a page

**No demo hides behind a button that opens a view.** Every demo sits inline on its page: no "Open … demo" launcher, no full-screen demo screen, no Close button. The only things that open over a page are a demo's own overlay (from its own button or hold) and a push from a row; an action overlay's tiles may open things. The library's overlays are windows, so a demo presents its sheet, alert, remote, spotlight or loading overlay straight from its own button, over the whole screen, top bar included. A Preview's button says plainly what it shows ("Show sheet", "Show alert", "Show actions") and never pretends to do app work; a Loaded demo sets its overlay in a small composition with its own context.

**The screen host — `CatalogScreenHost { }`** (`scaffold/ScreenHost.kt`). Banners and toasts aren't windows: they show in the nearest `AMBannerHost` / `AMToastHost`, at the root of their window (`Feedback.md`). In an app that root is the screen's; in the catalog it's the page's, since each page fills the screen. `ComponentPage` wraps its whole `Scaffold` in `CatalogScreenHost`, which is `AMBannerHost(fillMaxSize) { AMToastHost(fillMaxSize) { … } }`, so a demo that sits inline calls `AMBanner` or `AMToast` from its own button and the banner comes down over the top app bar, the toast up above the navigation bar, over the whole page. A demo needs nothing else: no cover, no screen of its own. A sheet the demo opens carries its own toast host (`AMSheet` has one), and a demo that is itself a small scene (`AMScene`, on a `Fill` band) has its own hosts.

**`CatalogAutoPresent(onPresent)`** (`scaffold/LaunchOptions.kt`) is the screenshot hook for a demo that shows something inline on its own (a hub opening its menu): placed in the demo, it calls `onPresent` once, 800 ms after it enters the composition, when the `present` extra names its section (`LocalCatalogTarget`). At most one demo per launch (`LaunchOptions.claimPresentation`). Overlay demos aren't opened this way; a screenshot taps their own button. The Floating action button page places one in each section, so `present preview` opens the hub's menu and `present loaded` opens the mail scene's remote.

## Inspection controls

`app/Inspection.kt`. Three settings, persisted in the `inspection` SharedPreferences as enum names:
- **Appearance** (`InspectionAppearance`): System, Light, Dark.
- **Text size** (`InspectionTextSize`): System, or one of the font scales the system's font size setting offers: 85%, 100% (default), 115%, 130%, 150%, 180%, 200%.
- **Layout direction** (`InspectionLayoutDirection`): Left to right, Right to left.

`Inspection` holds them as Compose state and writes each change through to SharedPreferences. Its `applied…` values are what the root applies: a launch extra's pinned value when there is one, the stored value otherwise. `isDefault` is whether everything applied is at its default.

**`InspectionRoot(inspection) { }`**, applied once at the root, applies them to everything below: the appearance as `AMTheme(darkTheme = …)` (System follows `isSystemInDarkTheme()`), which every style color follows; the text size through `LocalDensity`, a `Density` built from a configuration context with that font scale, so the platform's nonlinear scaling applies (System keeps the system's density); the layout direction through `LocalLayoutDirection`. It also provides `LocalInspection`. The tree keeps its shape whatever the settings, so a change never resets screen state.

**`InspectionMenu()`** is the top app bar action: an eye (`ic_visibility`, filled `ic_visibility_filled` while anything is off its default), "Inspection settings", opening a Material `DropdownMenu`. Its main list: an "Appearance" label over the three appearances (each with its glyph, a check on the applied one); "Text size" and "Layout direction" items showing their applied value with a chevron, each opening its own list of options in the same menu (with a back item at its top); and "Reset" while anything is off its default. Picking an option or Reset closes the menu. The menu shows the applied values and writes the stored ones.

**`SceneAppearanceButton()`** is the light/dark chip each Showcase scene carries in its landing view's header actions (`DemoScenes.md`): an `AMToolbarButton` that flips the appearance away from what is on screen (`inspection.isDark()`), from System too, writing Light or Dark to the stored setting the menu writes. Its glyph is the target appearance's (`ic_light_mode` / `ic_dark_mode`), read as "Switch to light mode" or "Switch to dark mode". A launch-pinned appearance keeps the screen as it is, as it does for the menu.

Only the activity (for the bar icons), the root, the menu and the scene chip read the settings; pages don't.

## The theme — `AMTheme`

The catalog runs inside the library's own theme: `InspectionRoot` wraps everything in `AMTheme(darkTheme = inspection.isDark())`, Material's baseline static scheme under the style layer, so a forced appearance reaches every style color. The scaffold reads `AMTheme.Surface.page` for backgrounds (the page headers take it when scrolled) and `AMTheme.Surface.primary` for `Fill` stages and surface strips; a demo reads `AMTheme.isDark` where its content follows the appearance (`CatalogArt.url(AMTheme.isDark)`).

## Debug launch extras — `LaunchOptions`

`scaffold/LaunchOptions.kt`, for screenshots and page checks. `LaunchOptions.read(intent)` reads string extras in debug builds only (`BuildConfig.DEBUG`), so release builds ignore them. The entry id matches exactly; the other values match case-insensitively.

- `entry <id>`: opens that entry's page over the list on launch; a Showcase id opens its scene.
- `scrollTo preview|loaded`: once the page has laid out and 400 ms have passed, the page collapses its top bar and scrolls the section to just under it. While it's set, a page that scrolls gets the viewport's height again as room at its end, so the section can always reach the top instead of the scroll stopping at the page's end; a page that doesn't scroll gets none and still doesn't.
- `present preview|loaded`: the `CatalogAutoPresent` demo in that section opens, once.
- `appearance light|dark|system`, `fontScale <float>`, `rtl true|false`: pin that inspection setting for the launch. A pinned setting doesn't write the stored one, and the inspection menu can't change it until a launch without the extra.
- `artwork night`: every catalog photo switches to a second set (below).
- `demoTime <H:MM>`: the clock-driven samples (the Date picker page's starting value, the Calendar scene's now marker) read the time through `LaunchOptions.clock()`, which pins the time of day (24-hour, today's date) so screenshots agree with a pinned status bar clock.
- `shopFillsCheckout yes`: the Shop scene's checkout opens with its form filled in (`LaunchOptions.shopFillsCheckout`).

```bash
adb shell am start -S -n com.ideamtrls.androidmtrls.demo/.MainActivity --es entry card --es appearance dark
adb shell am start -S -n com.ideamtrls.androidmtrls.demo/.MainActivity --es entry card --es scrollTo loaded --es fontScale 2
```

## Sample photos — `catalogPhoto`, `commonsPhoto`, `CatalogArt`

`scaffold/CatalogPhoto.kt` builds the URLs a page's sample pictures load from:
- **`catalogPhoto(seed, width, height)`**: picsum's fixed image per seed (square when `height` is null). Under `artwork night` each seed gets `-night` appended, so a component's light and dark screenshots show different artwork. A photo that fills a wide frame asks for that frame's shape, 800 wide by the matching height, so the whole picture shows instead of a square's middle slice.
- **`commonsPhoto(file)`**: a Wikimedia Commons file by name, 1000 px wide (`Special:FilePath`; the file's page credits its author and license), for art that has to show a particular subject.
- **`CatalogArt`**: vetted Commons pairs for wide hero photos, snowy mountains and forests by day and New York at night by dark: `Mountain`, `Valley`, `Forest`, `Peaks`, `Winter`, read with `url(dark)`.

## Glyphs

The demo's own glyphs (the catalog's search, back, close and inspection menu glyphs, the category icons, and the glyphs the pages and scenes use) are Material Symbols Outlined vector drawables in `demo/src/main/res/drawable/` as `ic_<name>.xml`, copied from Google's repository with the tint pointed at `?android:attr/colorControlNormal` (`Conventions.md`). Glyphs a library component owns come from the library (`am_ic_<name>`).

## Adding a component page

Follow these steps exactly; touch only your category's folder.

1. **File.** `catalog/pages/<category>/<Name>Page.kt`, where `<Name>` is the title in PascalCase (`IconButtonPage.kt`, `FloatingActionButtonPage.kt`). One component per file. It starts with `package com.ideamtrls.androidmtrls.demo` and imports what it uses from `com.ideamtrls.androidmtrls`.

2. **Entry.** At the top of the file:
   ```kotlin
   internal val IconButtonEntry = ComponentEntry(
       id = "iconButton",                        // lowerCamelCase of the title
       category = CatalogCategory.Actions,       // this folder's category
       title = "Icon button",                    // full name, sentence case, no abbreviations
       summary = "A bare glyph with a 48 dp touch target",  // one line, about 45 characters, no period
       icon = R.drawable.ic_more_vert,           // a demo glyph
       keywords = listOf("AMIconButton", "glyph", "toolbar"),  // API names and synonyms
   ) { IconButtonPage() }
   ```

3. **Register.** Append `IconButtonEntry` to `entries` in `pages/<category>/<Category>Pages.kt`, flagship component first. Nothing else changes.

4. **Body.**
   ```kotlin
   @Composable
   private fun IconButtonPage() {
       ComponentPage(
           IconButtonEntry,
           preview = { /* Preview: the simplest setup, live. */ },
       ) {
           /* Loaded: one fuller composition. */
       }
   }
   ```
   No state in the page function: state lives in the demo composables.

   A component that lives flat or in a card previews flat: the preview leaves out its container and sits on a surface strip.
   ```kotlin
   ComponentPage(
       FieldEntry,
       previewLayout = StageLayout.FullBleed,
       preview = { PreviewField(Modifier.background(AMTheme.Surface.primary)) },
   ) { … }
   ```

5. **Demos.** Two, and neither carries a title, caption, header or any code: the widgets speak for themselves. A page whose Preview would only repeat its Loaded composition passes `loaded` alone and shows no Preview.
   - **Preview**: the component in its simplest, default setup.
   - **Loaded**: one composition showing most of the component's main ideas at once, as a believable fragment of an app screen: a settings card mixing a row's options, a toolbar using several button kinds, a form using an input with a label, a value and a disabled field. Not a grid of variants and not every variation; nothing in it labels what it shows ("Disabled", "Large"). Roughly one screen tall or less. A behavior you can't see at rest (a date range, a callback) stays off the page.
   - A demo holds the component plus its container when it needs one (`Modifier.amCard()` around a flat row); sample data and small sample composables stay at file scope, `private`. A stateful demo is a `private` composable in the same file that the lambda calls.
   - Every row follows the row recipe (`Layout.md`). Hand-built rows (a title, a title and supporting line, a label and value) carry `Modifier.amRowPadding()`; rows built on Material's `ListItem` pass `colors = AMRowDefaults.colors()`, `shapes = AMRowDefaults.shapes()` and `contentPadding = AMRowDefaults.contentPadding`. Several stack in a `Column` with `AMDivider(inset = AMTheme.tokens.spacing.lg)` between.
   - Stand-in actions are `private fun save() {}` at file scope, as in `AMButton("Save", onClick = ::save)`.
   - Overlays: the demo sits inline, and its own button presents the overlay ("Overlays on a page").
   - Stage fit (the demo rule): self-surfaced content and carded flat content float (default); a flat strip that spans the screen → `FullBleed`; a self-inset component whose surfaces float edge to edge → `Wide`; content that is itself a small screen → `Fill`, through `previewLayout` / `loadedLayout`. Never negative padding, a frame or a box.
   - Sample pictures come from `catalogPhoto`, `commonsPhoto` or `CatalogArt`.
   - Use theme values (`AMTheme` for the style layer and spacing, `MaterialTheme.colorScheme`, `typography` and `shapes`), never raw colors or sizes. A demo whose content follows the appearance reads `AMTheme.isDark` (`CatalogArt.url(AMTheme.isDark)`).

6. **Verify.** Build, install, and launch with `--es entry <id>` (plus `--es scrollTo loaded`), and look at screenshots in light, dark (`--es appearance dark --es artwork night`) and a large font scale (`--es fontScale 2`) (`Testing.md`).

Rules for category work: don't edit `app/`, `catalog/scaffold/`, `catalog/pages/showcase/`, `catalog/scenes/`, or another category's folder. File-scope helpers are `private` (the demo is one package, so two pages may both declare a `save()`); a helper shared by several pages in one category goes in a `<Category>Support.kt` in that folder, `internal`, with a category-prefixed name. Missing scaffold capability → report it rather than patching shared files.

## Motion in the catalog

The catalog writes no animation code: pushes and predictive back are Navigation 3's, the top app bars collapse with Material's scroll behavior, the search opens and closes with Material's own motion, and the `scrollTo` jump is instant.
