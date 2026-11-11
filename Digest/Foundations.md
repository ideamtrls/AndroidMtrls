---
scope: The style layer's values and the theme every component reads — AMTheme (installing MaterialTheme) and its read-only style tier (the row recipe's vertical padding included), AMTokens (spacing, semantic tints), AMSemantic, the shadow table and AMElevation, hairlines and the internal elevated-surface helper, AMSurfaceStyle nesting, AMSurfaceFill and AMBackdrop (frosted sampling), the motion helpers, the status-bar edge for headerless scrolling views (Modifier.amStatusBarEdge), and the page and section headers
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/foundations/**, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/internal/BackdropSource.kt, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/internal/FrostedSlice.kt, androidmtrls/src/main/res/drawable/am_ic_info.xml, androidmtrls/src/main/res/drawable/am_ic_check_circle.xml, androidmtrls/src/main/res/drawable/am_ic_warning.xml, androidmtrls/src/main/res/drawable/am_ic_error.xml, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/FrostedSampleRegionTest.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/foundations/**]
keywords: [Style, Style.kt, shadow table, ShadowLevel, ShadowLayer, blur radius, sigma, AMTheme, AMTheme.Surface, primary, page, AMTheme.Frosted, fill, edge, highlight, secondaryText, blurRadius, hairlineWidth, selectionRing, selectionRingWidth, controlOutline, rowVerticalPadding, row recipe, AMTheme.Overlay, darkDimAlpha, ModalOverlay, FallbackDimLight, FallbackDimDark, PageWashAlpha, 34 dp, LocalAMOnPrimarySurface, sitsOnPrimarySurface, on a primary surface, AMTheme.ActionGrid, tileHeight, AMTheme.tokens, AMTheme.isDark, darkTheme, LocalAMDarkTheme, LocalAMTokens, MaterialTheme, MotionScheme.standard, static color scheme, baseline scheme, nested theme, AMTokens, AMSpacing, xs, sm, md, lg, xl, xxl, AMSemanticTints, AMSemanticColors, tone 40, tone 80, green, amber, AMSemantic, Info, Success, Warning, Destructive, icon, severityLabel, AMElevation, None, Low, Medium, Text, Modifier.amElevation, Modifier.amTextShadow, glyphShadow, ShapeShadows, createDropShadowPainter, Modifier.amHairline, drawInsetOutline, amElevatedSurface, ElevatedSurfaceDrawer, drawSurface, controlSurface, LocalAMEnabled, ProvideAMEnabled, resolveEnabled, AMSurfaceStyle, LocalAMSurfaceStyle, ProvideAMSurfaceStyle, SurfaceStyleOrigin, LocalAMSurfaceStyleOrigin, NestingSurfaceKey, NestingSurfaceNode, resolveSurfaceStyle, re-lift, AMSurfaceFill, Solid, Frosted, AMBackdrop, AMArtRowsBackdrop, LocalAMBackdrop, AMBackdropSource, backdropSource, FrostedSlice, SampleRegion, frostedSampleRegion, BlurEffect, solid fallback, AMPageHeader, LargeTopAppBar, title-only header, collapsedHeight, frosted header, FrostedBar, twoRowBarFill, FastOutLinearInEasing, AMSectionHeader, titleSmall, list subheader, type roles, amInstantSwitch, InstantSwitch, Modifier.amCrossfade, Remove animations, amArtText, filled glyphs, amStatusBarEdge, Modifier.amStatusBarEdge, status bar edge, scroll edge, soft edge, headerless, bar-less screen, StatusBarEdgeNode, WindowInsetsRulers, StatusBars, ruler, alpha mask, DstIn, saveLayer, fading edge, FastOutLinearInEasing]
updated: 2026-11-10
---

# Foundations

The style layer's values and the theme every component reads. `AMTheme` installs Material's theme underneath and provides the style layer on top: the floating surface and the tuned page, the two-layer shadows, hairlines, the selection ring, the frosted recipe, the row recipe's vertical padding, the overlay recipe and the remote's tile height, all read-only; and `AMTokens`, the overridable spacing scale and semantic tints. Around them: `AMSemantic` (the four status roles), `AMElevation` and `Modifier.amElevation` / `Modifier.amTextShadow`, `Modifier.amHairline` and the internal elevated-surface helper every floating surface wears, `AMSurfaceStyle` nesting, `AMSurfaceFill` with `AMBackdrop` (frosted surfaces over art), the motion helpers `amInstantSwitch` and `Modifier.amCrossfade`, `Modifier.amStatusBarEdge` (content fading under the status bar on a screen with no top bar), and the page and section headers.

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `foundations/Style.kt` | internal `Style`: every raw style value, each color light and dark, and **the shadow table**; `ShadowLayer`, `ShadowLevel` |
| `foundations/Theme.kt` | `AMTheme(darkTheme, colorScheme, tokens, content)`, `object AMTheme` (the read-only style tier, `tokens`, `isDark`), internal `LocalAMDarkTheme`, `LocalAMTokens` |
| `foundations/Tokens.kt` | `AMTokens`, `AMSpacing`, `AMSemanticTints`, `AMSemanticColors` |
| `foundations/Semantic.kt` | `AMSemantic` |
| `foundations/Elevation.kt` | `AMElevation`, `Modifier.amElevation`, `Modifier.amTextShadow`; internal `glyphShadow`, `ShapeShadows` |
| `foundations/Hairline.kt` | `Modifier.amHairline`; internal `drawInsetOutline`, `Modifier.amElevatedSurface`, `ElevatedSurfaceDrawer`, `LocalAMOnPrimarySurface`, `sitsOnPrimarySurface` |
| `foundations/Enabled.kt` | internal `LocalAMEnabled`, `ProvideAMEnabled`, `resolveEnabled` — how a container disables the AM controls inside it |
| `foundations/SurfaceStyle.kt` | `AMSurfaceStyle`, `LocalAMSurfaceStyle`, `ProvideAMSurfaceStyle`; internal `SurfaceStyleOrigin`, `LocalAMSurfaceStyleOrigin`, `NestingSurfaceKey`, `NestingSurfaceNode`, `resolveSurfaceStyle` |
| `foundations/SurfaceFill.kt` | `AMSurfaceFill` |
| `foundations/Backdrop.kt` | `AMBackdrop` |
| `internal/BackdropSource.kt` | `AMBackdropSource`, `LocalAMBackdrop`, `Modifier.backdropSource` |
| `internal/FrostedSlice.kt` | `FrostedSlice`, `SampleRegion`, `frostedSampleRegion` |
| `foundations/PageHeader.kt` | `AMPageHeader` (string and slot forms) |
| `foundations/SectionHeader.kt` | `AMSectionHeader` (string and slot forms) |
| `foundations/Motion.kt` | `amInstantSwitch`, `Modifier.amCrossfade`, internal `InstantSwitch` |
| `foundations/ScrollEdge.kt` | `Modifier.amStatusBarEdge`; private `StatusBarEdgeElement`, `StatusBarEdgeNode`, `EdgeMaskStops` |

- Glyphs: `androidmtrls/src/main/res/drawable/am_ic_info.xml`, `am_ic_check_circle.xml`, `am_ic_warning.xml`, `am_ic_error.xml` (`AMSemantic.icon`), the filled Material Symbols (`<name>_fill1_24px`).
- Test: `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/FrostedSampleRegionTest.kt` (the frosted sampling rectangle).
- `Modifier.amArtText()` lives in `datadisplay/ArtBackdrop.kt` (`DataDisplay.md`) and draws with this area's glyph shadow.
- Catalog pages: `demo/.../catalog/pages/foundations/` (below).

## The theme — `AMTheme`

```kotlin
AMTheme { App() }                                                      // baseline scheme, following the system
AMTheme(darkTheme = dark, colorScheme = if (dark) BrandDark else BrandLight) { App() }
AMTheme(tokens = AMTheme.tokens.copy(spacing = AMTheme.tokens.spacing.copy(lg = 20.dp))) { Feed() }
```

- **`AMTheme(darkTheme, colorScheme, tokens, content)`** installs `MaterialTheme(colorScheme, MotionScheme.standard(), shapes, typography)`, keeping the current (default) shapes and type scale, then provides `LocalAMDarkTheme` and `LocalAMTokens`. `darkTheme` comes first so `colorScheme`'s default can follow it.
- **Defaults.** `darkTheme`: the enclosing `AMTheme`'s flag, else `isSystemInDarkTheme()`. `colorScheme`: inside an `AMTheme` showing the same appearance, the current `MaterialTheme.colorScheme`; otherwise Material's baseline static scheme (`lightColorScheme()` / `darkColorScheme()`). `tokens`: the current `AMTheme.tokens` (`AMTokens.Default` at the root). So a nested theme that only changes tokens keeps the app's colors and a forced appearance, the way a nested `MaterialTheme` keeps what it isn't given.
- **Never dynamic color.** The scheme is static. Type, color, shape and motion are overridden through Material: a `colorScheme` here, or a `MaterialTheme(shapes = …)` / `MaterialTheme(typography = …)` inside.
- **The appearance flag rules every style color.** Everything that differs by mode reads `LocalAMDarkTheme` (dynamic, so modifier nodes that read it while drawing redraw on a change), never `isSystemInDarkTheme()`. An app (or the demo's inspection menu) can force either appearance. Outside any `AMTheme` the style reads as light with `AMTokens.Default`.

### The read-only style tier — `object AMTheme`

| Member | Value |
|---|---|
| `tokens` | the current `AMTokens` |
| `isDark` | whether the theme shows its dark appearance |
| `Surface.primary` | the floating surface: `White` / `Black`. Cards, filled buttons, sheets, tiles, the floating action button and the banner fill with it; no tonal tint |
| `Surface.page` | the tuned page: `#F2F2F7` / `#141416`, far enough above black that black surfaces read as floating |
| `Surface.art` | the dark neutral standing in for art until its picture lands: `#333333` in both modes, so white art text stays legible; art backdrops and art rows draw it before their picture, and a screen on art puts it behind its transitions |
| `Frosted.fill` | white 0.6 / black 0.5 over the blur |
| `Frosted.edge` | white 0.6 / white 0.15, the outline in both modes |
| `Frosted.highlight` | white 0.5 / white 0.12, a lifted row on the glass |
| `Frosted.secondaryText` | `#2C2C2E` / `#D1D1D6`, supporting text on the glass |
| `Frosted.blurRadius` | 40 dp (Android's blur radius: a sigma of about 24) |
| `hairlineWidth` | 0.5 dp, drawn no thinner than 1 px |
| `selectionRing` | `#C7C7CC` / `#3A3A3C`, asymmetric on purpose; also the default hairline color |
| `selectionRingWidth` | 1.5 dp, a chosen row or tile |
| `controlOutline` | `#C7C7CC` / `#636366`, the dark-mode outline of a solid control inside a black card or on a black sheet (`sitsOnPrimarySurface`, below) |
| `rowVerticalPadding` | 28 dp, the row recipe's space above and below a row's content (`Style.Row`): one line of `bodyLarge` makes an 80 dp row, two lines 100 dp (`Layout.md`) |
| `Overlay.blurRadius`, `Overlay.darkDimAlpha` | 34 dp (Android's blur radius: a Gaussian sigma of about 20); 0.4 (dark mode only). The overlay window's blur-behind, `AMBackdropBlur` and `amPausedBackdrop` all blur by it |
| `ActionGrid.tileHeight` | 76 dp, fixed whatever the font scale |

Colors are `@Composable @ReadOnlyComposable` getters resolved by `isDark`; sizes are plain values. Every value comes from `Style.kt`, the one place each lives. `Style.ModalOverlay` also holds three values only the overlays read, with no public getter: `FallbackDimLight` 0.32 and `FallbackDimDark` 0.6 (the overlay window's dim while the system has cross-window blur off) and `PageWashAlpha` 0.55 (`AMPausedScrim.Page`) (`Overlays.md`). Android's blur radius isn't a sigma: the platform's sigma is about `0.577 × radius + 0.5`, so a design blur of sigma 20 is a radius of 34 and the frosted sigma of 24 a radius of 40. Shadows are read through `AMElevation`.

## Tokens — `AMTokens`

Only what Material lacks. `@Immutable` classes with `equals` and `hashCode`; `AMTokens`, `AMSpacing` and `AMSemanticTints` also have `copy`.

- **`AMTokens(spacing = AMSpacing(), semantic = AMSemanticTints())`**, `AMTokens.Default`.
- **`AMSpacing`**, a 4 dp grid: `xs` 4, `sm` 8, `md` 12, `lg` 16, `xl` 24, `xxl` 32.
- **`AMSemanticTints(successLight, successDark, warningLight, warningDark)`**, each an **`AMSemanticColors(color, onColor, container, onContainer)`**, built the way Material builds `error`: light tones 40 / 100 / 90 / 10, dark tones 80 / 20 / 30 / 90, from Material's own HCT tonal palettes:

| | color | onColor | container | onContainer |
|---|---|---|---|---|
| Success light (Green 500 `#4CAF50` palette) | `#006E1C` | `#FFFFFF` | `#94F990` | `#002204` |
| Success dark | `#78DC77` | `#00390A` | `#005313` | `#94F990` |
| Warning light (Amber 500 `#FFC107` palette) | `#795900` | `#FFFFFF` | `#FFDF9D` | `#261A00` |
| Warning dark | `#FABD00` | `#3F2E00` | `#5B4300` | `#FFDF9D` |

## Status roles — `AMSemantic`

`Info`, `Success`, `Warning`, `Destructive`: the one mapping from role to colors and glyph, shared by every status primitive.

- `color`, `onColor`, `containerColor`, `onContainerColor` (`@Composable`): `Info` is the scheme's `primary` group, `Destructive` its `error` group, `Success` and `Warning` the `AMTokens.semantic` pair for the appearance.
- `icon` (`@Composable` `Painter`): the filled Material Symbols `info`, `check_circle`, `warning`, `error` (`am_ic_…`), so a status reads as a solid badge; the mark inside is cut out and shows the surface under it.
- internal `severityLabel`: what a screen reader says for the glyph before a banner's title: none for `Info`, then "Success", "Warning", "Error".

## Elevation — `AMElevation`

`None`, `Low`, `Medium`, `Text`. Each is two layers, a tight near layer and a soft wide far layer, both black.

- **`Modifier.amElevation(level, shape)`**: the shape casts both layers under the content, as the platform's drop shadow painters (`GraphicsContext.shadowContext.createDropShadowPainter`, what `Modifier.dropShadow` draws with), far layer first, rebuilt only when the shape, level or appearance changes (`ShapeShadows`). **A shadow is cast by a shape, never by content**, so glyphs and nested controls never cast smudges onto their own card. Put it ahead of the surface's fill.
- **`Modifier.amTextShadow()`**: the `Text` level cast by the glyphs themselves. Internal `glyphShadow(level, tint)` records the content into a layer, then draws it three times: blurred (`BlurEffect`, `TileMode.Decal`), tinted black at the layer's alpha and offset, for the far and then the near layer, each recorded with a margin of twice the blur so it isn't cut off; then the content itself, in `tint` when specified (art text is white), its own colors otherwise.
- Levels: `None` keeps fill and shape and casts nothing (dense tile grids, horizontal card bands); `Low` is the default (cards, filled buttons, tiles, the toast); `Medium` is a sheet; `Text` is glyph legibility over art. Shadows run much stronger in dark mode, so they register on the dark page.

### The shadow table — `Style.Shadow` in `Style.kt`

Tune shadows here and nowhere else: (blur, y, light alpha, dark alpha) per layer.

| Level | near | far |
|---|---|---|
| `None` | 0, 0, 0, 0 | 0, 0, 0, 0 |
| `Low` | 6 dp, 2 dp, 0.10, 0.55 | 27 dp, 10 dp, 0.08, 0.45 |
| `Medium` | 10 dp, 4 dp, 0.10, 0.55 | 48 dp, 18 dp, 0.08, 0.45 |
| `Text` | 4 dp, 1 dp, 0.12, 0.12 | 16 dp, 4 dp, 0.10, 0.10 |
| `ArtText` (internal, `amArtText`) | 1 dp, 1 dp, 0.30, 0.30 | 10 dp, 2 dp, 0.28, 0.28 |

The blur is Android's blur radius, which the platform turns into a Gaussian sigma of about `0.58 × blur + 0.5`; the values put each layer's sigma at the design's shadow softness (sigma 4 ≈ blur 6, 16 ≈ 27, 28 ≈ 48). They are tuned against rendered cards (the Elevation and Color pages).

## Hairlines — `Modifier.amHairline`

- **`Modifier.amHairline(shape, color = Color.Unspecified, width = 0.5.dp)`**: an outline over the content, inside the bounds so it never grows the surface, never thinner than 1 px. Unspecified `color` draws `selectionRing` for the appearance. `width = AMTheme.selectionRingWidth` marks a chosen item.
- For outlines that always show: sheets, tiles, the floating action button, the banner, avatars. Solid cards draw theirs through the elevated-surface helper, in dark mode only.
- Internal `DrawScope.drawInsetOutline(shape, color, width)`: rectangles and rounded rectangles are stroked on an outline inset by half the stroke (corner radii shrink with it); any other shape is stroked twice as wide, clipped to the shape.

### The elevated surface — internal `Modifier.amElevatedSurface(shape, elevation = Low, fill = Solid, hairline = Unspecified)`

The recipe every floating surface wears (cards, filled buttons, toolbar buttons, tiles, sheets), drawn around the content in one node so it never drifts. It doesn't clip the content; whatever the content draws in its own background sits above the fill. The drawing lives in internal `ElevatedSurfaceDrawer` (shadows, frosted slice, position; `attach`/`detach`, `position`, `ContentDrawScope.drawSurface(…, clipContent)`), which this node, the card's node (`Modifier.amCard`, `Layout.md`) and the filled buttons' private `controlSurface` (`Actions.md`) each own one of; the card draws with `clipContent = true`.

- **Solid**: the shape's shadow, the shape filled with the surface color, the content, then in dark mode only a hairline in `selectionRing` (or `hairline`, which a solid control on a black card or sheet sets to `controlOutline`).
- **Frosted**: the shadow clipped to outside the shape (under glass it would show through), the nearest `AMBackdrop`'s art under the shape blurred at 40 dp (`FrostedSlice`), the frosted fill, the content, then the frosted edge in both modes. With no backdrop, or before the art and the surface are placed, it draws Solid, **never unblurred translucency**.
- The node tracks its position (`GlobalPositionAwareModifierNode`); when its offset in the art changes (a scroll) it redraws, sampling the new slice.

### On a primary surface — internal `LocalAMOnPrimarySurface`, `sitsOnPrimarySurface()`

A solid control (a filled `AMButton`) on a black surface takes `controlOutline` in dark mode, since the selection ring would vanish against the black around it. Two surfaces count, found two ways, behind one internal test, `T.sitsOnPrimarySurface()` (for a node that's a `DelegatableNode` and a `CompositionLocalConsumerModifierNode`, called while attached):

- **A card**: any `NestingSurfaceKey` node above the control in the layout tree (`findNearestAncestor`), flat or not. A card is a modifier, so it can't provide a local to its content; the tree lookup finds it.
- **A sheet**: `LocalAMOnPrimarySurface` (a dynamic `compositionLocalOf`, false at the root, so a node reading it while drawing redraws on a change). A sheet is a window of its own, so the lookup can't see it; `AMSheet` provides true to its content for its `Primary` and `Frosted` surfaces (frosted is near-black glass, solid without art) and false for `Page`. An overlay window's fresh context (`ProvideOverlayWindowLocals`) resets it to false, so a picker opened from a sheet starts clean.

## Frosted surfaces over art — `AMSurfaceFill`, `AMBackdrop`

- **`AMSurfaceFill`**: `Solid` (default) or `Frosted`. A parameter, never a theme setting: a component that offers it passes it to the elevated-surface helper's `fill`. An app picks one for its cards, tabs and page buttons and doesn't mix them; crucial controls stay solid.
- **`AMBackdrop(art, modifier, content)`**: `art` draws behind `content`, filling the region (`matchParentSize`), and records itself into a `GraphicsLayer` each time it draws (`Modifier.backdropSource`: `onPlaced` keeps its coordinates, `drawWithContent` records then draws the layer). It provides `LocalAMBackdrop` to `content`. Only `art` is sampled, never the content, so a frosted surface never blurs itself or another frosted surface. The nearest backdrop wins: a row's own art beats the page's. A page or row backdrop is a thin wrapper over it. `AMArtRowsBackdrop` (`DataDisplay.md`) is the same kind of source whose art is the `amArtRows` page inside it, drawn by its own art box behind the region, so frosted surfaces over art rows sample the rows' colors.
- **Sampling** (`FrostedSlice`): the surface's own layer records `drawLayer(art layer)` translated to the surface's offset in the art, over a region twice the blur wider than the surface on every side, cut to the art's bounds and widened to whole pixels (`frostedSampleRegion`, unit-tested); `BlurEffect(r, r, TileMode.Clamp)`, so at the art's edge real pixels repeat instead of transparency; drawn clipped to the shape. It holds a reference to the art's layer, so new art (a photo arriving) shows without re-recording.
- **Limits**: a frosted surface must draw after its art in the same frame (inside the backdrop's content); frosting over another frosted surface isn't possible; each visible frosted surface is a blurred layer the GPU re-renders while it moves, so the solid fill stays the fallback if a phone can't hold it.

## Nesting — `AMSurfaceStyle`

- `Elevated` (fill, corners, shadow, dark hairline) or `Flat` (nothing: the surface defers to its parent).
- **`LocalAMSurfaceStyle`** (public, `Elevated` by default) carries the style a container set; **`ProvideAMSurfaceStyle(style) { }`** sets it and marks its subtree with a fresh internal `SurfaceStyleOrigin` (`LocalAMSurfaceStyleOrigin`). Set it only through `ProvideAMSurfaceStyle`.
- **A card inside a card demotes by itself.** A modifier can't set a local for content composed elsewhere, so `Modifier.amCard` resolves its style in its node (`resolveSurfaceStyle()`): `Flat` where the local says so; otherwise `Flat` when the nearest `NestingSurfaceNode` above it in the layout tree (a `TraversableNode`, `NestingSurfaceKey`) sees the same origin, `Elevated` when a `ProvideAMSurfaceStyle` sits between them (re-lifting a card inside a card) or when there's no card above. Shadows never stack and padding never doubles. The full rule is in `Layout.md`.
- The library's containers (`AMSection`, `AMDisclosureGroup`) also provide `Flat` for their content. A custom container joins by drawing its surface with `Modifier.amCard`, or by reading the local and setting `Flat` for its content. Controls never read it.

## Type roles

The Material type scale in the phone's font, sentence case everywhere. Page title = `headlineMedium` (`AMPageHeader`, expanded); section header = `titleSmall` in `primary` (`AMSectionHeader`); row title and body = `bodyLarge`; caption = `bodySmall`; code = `bodyMedium` monospace. Read them from `MaterialTheme.typography`; there is no separate role table.

## Headers

### `AMPageHeader` — `PageHeader.kt`

- `AMPageHeader(title, modifier, navigationIcon = null, actions = null, scrollBehavior = null, windowInsets = TopAppBarDefaults.windowInsets)`, and the slot form with `title: @Composable () -> Unit` last (the string form delegates to it).
- Material's `LargeTopAppBar` with `TopAppBarDefaults.topAppBarColors(containerColor = Transparent, scrolledContainerColor = AMTheme.Surface.page)`: transparent at rest, so it lies on the page, and the page color once content scrolls under it. With a `scrollBehavior` (usually `exitUntilCollapsedScrollBehavior`, its `nestedScrollConnection` on the scaffold) it collapses Material's way; without one it stays expanded. Null slots show nothing.
- **Over a picture** (inside an `AMBackdrop`, read through `LocalAMBackdrop`, with a `scrollBehavior`): both Material colors are transparent and the bar sits in the internal `FrostedBar` (`navigation/FrostedBar.kt`, `Navigation.md`), whose fill fades in as the bar collapses (`twoRowBarFill`: `FastOutLinearInEasing` of `collapsedFraction`, the rate Material fades a two-row bar's color, read in the fill's layer so it redraws without recomposing). The fill is the frosted recipe: the backdrop's art under the bar blurred at `Frosted.blurRadius` under `Frosted.fill`, the frosted edge along the bottom, behind the status bar too; the solid surface (dark-mode hairline at the bottom) until the art is placed, never unblurred translucency. Never a page band over the art. The title keeps the bar's `onSurface`, which reads on the frosted fill; a screen over dark art gives its title the art-text look itself through the slot form while expanded.
- **Title-only headers drop the top row.** The bar's top row holds the navigation icon, the actions and the collapsed title. A header with no navigation icon, no actions and no scroll behavior would keep it empty for good, so it passes `collapsedHeight = 0.dp` and `expandedHeight = LargeAppBarExpandedHeight − LargeAppBarCollapsedHeight` (88 dp): the large title's row alone, at Material's expanded title metrics. With any of the three, it's Material's full two-row bar.
- A screen around it is a `Scaffold` with `containerColor = AMTheme.Surface.page`, or `Color.Transparent` inside an `AMBackdrop`, so the art shows behind the header.

### `AMSectionHeader` — `SectionHeader.kt`

- `AMSectionHeader(title, modifier)` and `AMSectionHeader(modifier) { RowScope content }` (the string form delegates).
- Android's list-subheader look: `titleSmall` through `ProvideTextStyle`, `LocalContentColor` = `primary`, so custom content (a glyph, a weighted title, a count) stays on-system. A `Row`, full width, parts spaced by `spacing.sm`, centered vertically. No padding of its own: place it at the row inset.
- Heading semantics, merged: a screen reader reads glyph, title and count as one heading.
- **Over a picture** (inside an `AMBackdrop`, read through `LocalAMBackdrop`) it takes `onSurface` and the `Text` glyph shadow.

## Motion helpers — `Motion.kt`

Components own their own motion; these two are for an app that swaps a view's content itself. Both use Material's standard motion scheme, and the system's **Remove animations** (animator duration scale 0) makes them instant, as Compose does for all its animation; there is no extra machinery for it.

- **`amInstantSwitch { changes }`**: applies the changes in one `Snapshot.withMutableSnapshot`, so they land together in one frame, and bumps `InstantSwitch.generation` in the same snapshot. Keep the dismissal of whatever triggered the switch outside it, so that still animates.
- **`Modifier.amCrossfade(value)`**: when `value` changes, the old content leaves at once and the new fades in (a layer's alpha, 0 to 1) with `MaterialTheme.motionScheme.defaultEffectsSpec()`, read in the node through `MaterialTheme.LocalMaterialTheme`. The factory reads `InstantSwitch.generation` while composing, so every call site recomposes on a switch; a value change that arrives with a new generation (made inside `amInstantSwitch`) snaps instead of fading. Call it while composing.

## Status-bar edge — `Modifier.amStatusBarEdge()` (`ScrollEdge.kt`)

A top app bar takes content scrolling under it into its fill (`AMPageHeader`, `AMToolbar`, `AMPanelChrome`: the page color, or the frosted fill over art), status bar included. A screen with no top bar (a view whose header scrolls away with its content, a list that opens on its first section) would run its rows straight under the clock. **`Modifier.amStatusBarEdge()`**, on that screen's scrolling container, fades its content out over the status bar's height instead: whole at the bar's bottom edge, gone at the top of the screen, so whatever lies behind the content (the page, an `AMBackdrop`'s art) shows through under the icons.

```kotlin
LazyColumn(
    Modifier.fillMaxSize().amStatusBarEdge(),
    contentPadding = WindowInsets.systemBars.asPaddingValues(),
) { … }
```

- **An alpha mask on the content, never a band of color.** It fades what the container draws, so art behind the screen shows through. Put it on the container that fills the screen from its top edge and stays put while its content scrolls.
- **It adds no inset and moves nothing.** The container still pads the status bar, so its content rests below the bar and nothing changes until content scrolls up under it.
- **It follows the inset.** `StatusBarEdgeNode` (a `LayoutModifierNode` and `DrawModifierNode`) reads `WindowInsetsRulers.StatusBars.current.top` in its placement block, in its own coordinates, so a change to the inset (an animation, the bar hiding) places it again; the height under the bar is that value clamped into the content's height, and a new height rebuilds the mask and invalidates the draw. With no status bar over the container (the bar hidden, a container that doesn't reach the top of the screen, a host providing no rulers) the height is 0 and it draws the content untouched.
- **Drawing**: with a height under the bar, the content draws into a `saveLayer` (the content's height, widened by its width at each side for what it draws past them, its cards' shadows; the canvas's clip bounds it), then the mask over the band with `BlendMode.DstIn`: a vertical gradient from 0 to the height, its alpha Material's `FastOutLinearInEasing` from 0 at the top to 1 at the bar's bottom edge, sampled at 13 stops (`EdgeMaskStops`), so the content is mostly gone behind the icons. Below the band the content is unchanged.
- A screen with a top bar doesn't need it.

## Catalog pages

`demo/.../catalog/pages/foundations/`, registered in `FoundationsPages.entries` in this order (ids): `color`, `typography`, `pageHeader`, `sectionHeader`, `spacing`, `radius`, `elevation`, `hairline`, `motion`, `theming`.

| Page | Preview | Loaded |
|---|---|---|
| Color | full-bleed swatch rows: floating surface, page, text (`onSurface`), secondary text, accent (`primary`), selection ring, divider (`outlineVariant`) | a backup card: rows tinted by `AMSemantic` (success, info, warning, destructive) and one disabled row |
| Typography | full-bleed: all fifteen Material roles, a sample over its name and size | an article card: label, headline, title and body, then a list row under a divider in the same card |
| Page header | full-bleed `AMPageHeader("Library")` on a surface strip: a title-only header, the title row alone | `Fill`: the top of a reading list, a title-only header with the list's count trailing, over four rows |
| Section header | full-bleed `AMSectionHeader("Recents")` | full-bleed settings strip: a slot header (glyph, title, count) over two rows, a string header over a switch row; each header sits right on its rows |
| Spacing | full-bleed bars for each step, 32 to 4 dp | a profile card spaced on the scale, then a card of three hand-built rows |
| Radius | floating squares in `shapes.large`, `medium`, `small`, each with its size | a trip card using all three |
| Elevation | floating chips at `Medium`, `Low`, `None` | two album tiles at `Low`, the playing track's card at `Medium` |
| Hairline | a floating surface outlined with `amHairline` | a close button and a restore button outlined, two plan tiles (the chosen one at `selectionRingWidth`) |
| Motion | full-bleed: the six springs of the standard motion scheme, read live, and whether Remove animations is on | a card with tabs, a crossfading sign-in row (`amCrossfade`) and chips, a Replay button, a snackbar button |
| Theming | two specimen cards, the second under a nested `AMTheme` (teal scheme, `shapes.large` at 4 dp so its corners visibly differ, a wider `spacing.sm`) | a notebook card under the brand theme (teal, `shapes` 4/6/10 dp), its pinned note re-lifted inside with `ProvideAMSurfaceStyle(Elevated)` |

Every row on these pages follows the row recipe (`Layout.md`): hand-built rows carry `Modifier.amRowPadding()`, `ListItem` rows pass `AMRowDefaults`' colors, shapes and content padding, and rows stack with `AMDivider(inset = spacing.lg)` between. Cards are `Modifier.amCard()`.
