---
scope: The style layer's values and the theme every component reads — AMTheme (installing MaterialTheme) and its read-only style tier (the row recipe's vertical padding included), AMTokens (spacing, semantic tints), AMSemantic, the shadow table and AMElevation, hairlines and the internal elevated-surface helper, AMSurfaceStyle nesting, the app's surface setting (AMSurfaceFill on AMTheme), the tint and how a surface resolves its look (solid, frosted, tinted), AMBackdrop (frosted sampling), the motion helpers, the status-bar edge for headerless scrolling views (Modifier.amStatusBarEdge), and the page and section headers
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/foundations/**, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/internal/BackdropSource.kt, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/internal/FrostedSlice.kt, androidmtrls/src/main/res/drawable/am_ic_info.xml, androidmtrls/src/main/res/drawable/am_ic_check_circle.xml, androidmtrls/src/main/res/drawable/am_ic_warning.xml, androidmtrls/src/main/res/drawable/am_ic_error.xml, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/FrostedSampleRegionTest.kt, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/SurfaceLookTest.kt, androidmtrls/src/androidTest/kotlin/com/ideamtrls/androidmtrls/SystemBarsAppearanceTest.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/foundations/**]
keywords: [Frosted.tint, frostedTint, frostedGround, surfaceAccent, liesOnFrost, accent on frost, secondaryText, AMTheme.secondaryText, surfaceFill, AMTheme.surfaceFill, the setting, LocalAMSurfaceFill, ProvideAMOnSurface, readOnSurface, LocalAMLiesOn, LiesOn, Page, Surface, Sheet, SurfaceLook, surfaceLook, lookOf, Tinted, the tint, embeddedFill, EmbeddedFillLight, EmbeddedFillDark, embedded, isEmbedded, backdropFor, frostOverPage, frost over the page, tintedGround, SurfaceLookTest, Style, Style.kt, shadow table, ShadowLevel, ArtTextLight, Style.ArtInk, artTone, artGlyphs, ShadowLayer, blur radius, sigma, AMTheme, AMTheme.Surface, primary, page, AMTheme.Frosted, fill, edge, highlight, secondaryText, blurRadius, hairlineWidth, selectionRing, selectionRingWidth, controlOutline, rowVerticalPadding, row recipe, AMTheme.Overlay, Overlay.wash, ModalOverlay, WashLight, WashDark, PageWashAlpha, 34 dp, AMTheme.ActionGrid, tileHeight, AMTheme.tokens, AMTheme.isDark, darkTheme, LocalAMDarkTheme, LocalAMTokens, MaterialTheme, MotionScheme.standard, static color scheme, baseline scheme, nested theme, AMTokens, AMSpacing, xs, sm, md, lg, xl, xxl, AMSemanticTints, AMSemanticColors, tone 40, tone 80, green, amber, AMSemantic, Info, Success, Warning, Destructive, icon, severityLabel, AMElevation, None, Low, Medium, Text, Modifier.amElevation, Modifier.amTextShadow, glyphShadow, ShapeShadows, createDropShadowPainter, Modifier.amHairline, drawInsetOutline, amElevatedSurface, alwaysOutlined, ElevatedSurfaceDrawer, drawSurface, controlSurface, LocalAMEnabled, ProvideAMEnabled, resolveEnabled, AMSurfaceStyle, LocalAMSurfaceStyle, ProvideAMSurfaceStyle, SurfaceStyleOrigin, LocalAMSurfaceStyleOrigin, NestingSurfaceKey, NestingSurfaceNode, resolveSurfaceStyle, re-lift, AMSurfaceFill, Solid, Frosted, AMBackdrop, AMArtRowsBackdrop, LocalAMBackdrop, AMBackdropSource, backdropSource, FrostedSlice, SampleRegion, frostedSampleRegion, BlurEffect, AMPageHeader, LargeTopAppBar, title-only header, collapsedHeight, frosted header, FrostedBar, twoRowBarFill, FastOutLinearInEasing, AMSectionHeader, titleSmall, list subheader, type roles, amInstantSwitch, InstantSwitch, Modifier.amCrossfade, Remove animations, amArtText, filled glyphs, amStatusBarEdge, Modifier.amStatusBarEdge, status bar edge, scroll edge, soft edge, headerless, bar-less screen, StatusBarEdgeNode, WindowInsetsRulers, StatusBars, ruler, alpha mask, DstIn, saveLayer, fading edge, FastOutLinearInEasing]
updated: 2026-11-14
---

# Foundations

The style layer's values and the theme every component reads. `AMTheme` installs Material's theme underneath and provides the style layer on top: the floating surface and the tuned page, the two-layer shadows, hairlines, the selection ring, the frosted recipe, the row recipe's vertical padding, the overlay recipe and the remote's tile height, all read-only; and `AMTokens`, the overridable spacing scale and semantic tints. Around them: `AMSemantic` (the four status roles), `AMElevation` and `Modifier.amElevation` / `Modifier.amTextShadow`, `Modifier.amHairline` and the internal elevated-surface helper every floating surface wears, `AMSurfaceStyle` nesting, the app's surface setting (`AMSurfaceFill`, set on `AMTheme`) with `AMBackdrop` (frosted surfaces over art) and the tint embedded pieces draw, the motion helpers `amInstantSwitch` and `Modifier.amCrossfade`, `Modifier.amStatusBarEdge` (content fading under the status bar on a screen with no top bar), and the page and section headers.

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `foundations/Style.kt` | internal `Style`: every raw style value, each color light and dark, and **the shadow table**; `ShadowLayer`, `ShadowLevel` |
| `foundations/Theme.kt` | `AMTheme(darkTheme, colorScheme, tokens, surfaceFill, content)`, `object AMTheme` (the read-only style tier, `tokens`, `surfaceFill`, `isDark`), internal `LocalAMDarkTheme`, `LocalAMTokens`, `surfaceAccent`, the grounds `frostedGround`, `tintedGround`, `frostOverPage`, and `frostedTint` |
| `foundations/Tokens.kt` | `AMTokens`, `AMSpacing`, `AMSemanticTints`, `AMSemanticColors` |
| `foundations/Semantic.kt` | `AMSemantic` |
| `foundations/Elevation.kt` | `AMElevation`, `Modifier.amElevation`, `Modifier.amTextShadow`; internal `glyphShadow`, `artGlyphs`, `ArtGlyphs`, `ShapeShadows` |
| `foundations/Hairline.kt` | `Modifier.amHairline`; internal `drawInsetOutline`, `Modifier.amElevatedSurface`, `ElevatedSurfaceDrawer` |
| `foundations/Enabled.kt` | internal `LocalAMEnabled`, `ProvideAMEnabled`, `resolveEnabled` — how a container disables the AM controls inside it |
| `foundations/SurfaceStyle.kt` | `AMSurfaceStyle`, `LocalAMSurfaceStyle`, `ProvideAMSurfaceStyle`; internal `SurfaceStyleOrigin`, `LocalAMSurfaceStyleOrigin`, `NestingSurfaceKey`, `NestingSurfaceNode`, `resolveSurfaceStyle` |
| `foundations/SurfaceFill.kt` | `AMSurfaceFill` (the setting), `ProvideAMOnSurface`; internal `LocalAMSurfaceFill`, `readOnSurface` |
| `foundations/SurfaceLook.kt` | internal `SurfaceLook`, `LiesOn`, `LocalAMLiesOn`, `surfaceLook`, `isEmbedded`, `lookOf`, `backdropFor` |
| `foundations/Backdrop.kt` | `AMBackdrop` |
| `internal/BackdropSource.kt` | `AMBackdropSource`, `LocalAMBackdrop`, `Modifier.backdropSource`; `AMSceneBackdrops`, `LocalAMSceneBackdrops`, `sceneBackdrop` (a scene root's page backdrops, `Navigation.md`) |
| `internal/FrostedSlice.kt` | `FrostedSlice`, `SampleRegion`, `frostedSampleRegion` |
| `foundations/PageHeader.kt` | `AMPageHeader` (string and slot forms) |
| `foundations/SectionHeader.kt` | `AMSectionHeader` (string and slot forms) |
| `foundations/Motion.kt` | `amInstantSwitch`, `Modifier.amCrossfade`, internal `InstantSwitch` |
| `foundations/ScrollEdge.kt` | `Modifier.amStatusBarEdge`; private `StatusBarEdgeElement`, `StatusBarEdgeNode`, `EdgeMaskStops` |

- Glyphs: `androidmtrls/src/main/res/drawable/am_ic_info.xml`, `am_ic_check_circle.xml`, `am_ic_warning.xml`, `am_ic_error.xml` (`AMSemantic.icon`), the filled Material Symbols (`<name>_fill1_24px`).
- Tests: `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/FrostedSampleRegionTest.kt` (the frosted sampling rectangle), `SurfaceLookTest.kt` (the setting deciding the look), `ArtWashTest.kt` (frost over the page, text on the tint, the accent on frost; `DataDisplay.md`); instrumented, `androidmtrls/src/androidTest/kotlin/com/ideamtrls/androidmtrls/SystemBarsAppearanceTest.kt` (the outermost theme's bar icons).
- `Modifier.amArtText(tone, secondary)` lives in `datadisplay/ArtBackdrop.kt`, the art's tone and inks in `datadisplay/ArtTone.kt` (`DataDisplay.md`); art text draws with this area's glyph shadow.
- Catalog pages: `demo/.../catalog/pages/foundations/` (below).

## The theme — `AMTheme`

```kotlin
AMTheme { App() }                                                      // baseline scheme, following the system
AMTheme(darkTheme = dark, colorScheme = if (dark) BrandDark else BrandLight) { App() }
AMTheme(surfaceFill = AMSurfaceFill.Frosted) { App() }                 // a frosted app
AMTheme(tokens = AMTheme.tokens.copy(spacing = AMTheme.tokens.spacing.copy(lg = 20.dp))) { Feed() }
```

- **`AMTheme(darkTheme, colorScheme, tokens, surfaceFill, content)`** installs `MaterialTheme(colorScheme, MotionScheme.standard(), shapes, typography)`, keeping the current (default) shapes and type scale, then provides `LocalAMDarkTheme`, `LocalAMTokens` and `LocalAMSurfaceFill`. `darkTheme` comes first so `colorScheme`'s default can follow it.
- **Defaults.** `darkTheme`: the enclosing `AMTheme`'s flag, else `isSystemInDarkTheme()`. `colorScheme`: inside an `AMTheme` showing the same appearance, the current `MaterialTheme.colorScheme`; otherwise Material's baseline static scheme (`lightColorScheme()` / `darkColorScheme()`). `tokens`: the current `AMTheme.tokens` (`AMTokens.Default` at the root). `surfaceFill`: the current `AMTheme.surfaceFill` (`Solid` at the root, the library's default). So a nested theme that only changes tokens keeps the app's colors, a forced appearance and its surface setting (a frosted scene stays frosted), the way a nested `MaterialTheme` keeps what it isn't given.
- **Never dynamic color.** The scheme is static. Type, color, shape and motion are overridden through Material: a `colorScheme` here, or a `MaterialTheme(shapes = …)` / `MaterialTheme(typography = …)` inside.
- **The appearance flag rules every style color.** Everything that differs by mode reads `LocalAMDarkTheme` (dynamic, so modifier nodes that read it while drawing redraw on a change), never `isSystemInDarkTheme()`. An app (or the demo's inspection menu) can force either appearance. Outside any `AMTheme` the style reads as light with `AMTokens.Default`, and surfaces as solid.
- **The system bars follow it.** The outermost `AMTheme` in a window (no `LocalAMDarkTheme` above it) composes private `SystemBarsAppearance(darkTheme)` ahead of its content: a `DisposableEffect` keyed on `LocalView` and the flag that sets `APPEARANCE_LIGHT_STATUS_BARS | APPEARANCE_LIGHT_NAVIGATION_BARS` on the view's `windowInsetsController` (both set in light mode, both cleared in dark), so a pinned appearance reaches the status and navigation bar icons. Nested themes (and overlay windows, which inherit the flag) leave the bars alone; previews (`LocalInspectionMode`) do nothing. Edge to edge stays the activity's (`enableEdgeToEdge()` once). Composed first, it runs before the art's status bar claims on a change, so they take the status bar again on top of it (`AMStatusBarIcons`, `Navigation.md`).

### The read-only style tier — `object AMTheme`

| Member | Value |
|---|---|
| `tokens` | the current `AMTokens` |
| `surfaceFill` | the app's surface setting, `AMSurfaceFill.Solid` or `Frosted` (below) |
| `isDark` | whether the theme shows its dark appearance |
| `Surface.primary` | the floating surface: `White` / `Black`. Cards, filled buttons, sheets, tiles, the floating action button and the banner fill with it; no tonal tint |
| `Surface.page` | the tuned page: `#F2F2F7` / `#141416`, far enough above black that black surfaces read as floating; also what art shows with no picture (`DataDisplay.md`) |
| `secondaryText` | secondary text for the surface the composition lies on: the art's secondary ink (`Style.ArtInk`: `OnDark` white / `OnDarkSecondary` `#D1D1D6` on dark art, `OnLight` black / `OnLightSecondary` `#2C2C2E` on light art, in either mode), `Frosted.secondaryText` on a sheet or on a surface in a frosted app, else `onSurfaceVariant` (below) |
| `artTone` | the tone (`AMArtTone`) of the art under the composition, from the nearest art surface (`AMBackdrop`, `AMArtRowsBackdrop`): the mode's unless the app gave the surface one; null off art (`DataDisplay.md`) |
| `Frosted.fill` | white 0.6 / black 0.5 over the blur |
| `Frosted.embeddedFill` | white 0.6 / black 0.5 (`Style.Frosted.EmbeddedFillLight` / `EmbeddedFillDark`), **the tint**: what a piece embedded in a frosted surface fills with, over the frost under it, with no blur of its own |
| `Frosted.edge` | white 0.6 / white 0.15, the outline of frost and of the tint in both modes |
| `Frosted.highlight` | white 0.5 / white 0.12, a lifted row on frost |
| `Frosted.secondaryText` | `#2C2C2E` / `#D1D1D6`, supporting text on frost, on the tint and on a sheet: 12.5:1 / 12.2:1 on the tint over the frosted ground (internal `tintedGround(dark)`, L 0.894 / 0.007) and 13.4:1 / 13.0:1 on frost over the page (`frostOverPage`), pinned at 4.5:1 in `ArtWashTest` |
| `Frosted.tint` | the accent on frost: the scheme's `primary` moved along its own line just far enough to hold 4.5:1 on the frosted fill over the library's washed art (internal `frostedTint(color, dark)` against `frostedGround(dark)`: the fill over the light art's darkest, `Style.ArtInk.LightArtGround`, L 0.749, or over the dark art's brightest, `Style.ArtInk.DarkArtCeiling`, L 0.020). Material's baseline `primary` holds unchanged, 4.90:1 light and 8.81:1 dark; a lighter brand accent comes back darker. Over art an app didn't wash it's the app's to check (2.26:1 over raw black). It holds on the tint too, which is lighter (light mode) or darker (dark mode) than the frost under it. Internal `surfaceAccent()` gives it on a sheet or on a surface in a frosted app (the same test as `secondaryText`), `primary` elsewhere: an on `AMIconButton` toggle and an active `AMCardButton` in `Info` use it |
| `Frosted.blurRadius` | 40 dp (Android's blur radius: a sigma of about 24) |
| `hairlineWidth` | 0.5 dp, drawn no thinner than 1 px |
| `selectionRing` | `#C7C7CC` / `#3A3A3C`, asymmetric on purpose; also the default hairline color |
| `selectionRingWidth` | 1.5 dp, a chosen row or tile |
| `controlOutline` | `#C7C7CC` / `#636366`, the dark-mode outline of a solid control embedded in a black surface: inside a card, tabs or a section, or on a primary or frosted sheet (`isEmbedded`, below) |
| `rowVerticalPadding` | 28 dp, the row recipe's space above and below a row's content (`Style.Row`): one line of `bodyLarge` makes an 80 dp row, two lines 100 dp (`Layout.md`) |
| `Overlay.blurRadius`, `Overlay.wash` | 34 dp (Android's blur radius: a Gaussian sigma of about 20), which the overlay window's blur-behind, `AMBackdropBlur` and `amPausedBackdrop` all blur by; white 0.6 / black 0.65, the mode's wash over the paused screen, so the spotlight's title, in the tone's fixed ink, holds 7.36:1 (black) over sharp black art in light mode and 7.00:1 (white) over sharp white art in dark mode, with or without the blur (`Overlays.md`) |
| `ActionGrid.tileHeight` | 76 dp, fixed whatever the font scale |
| `ActionGrid.sidePadding`, `ActionGrid.rowBottom(index)`, `ActionGrid.rowTop(index)` | where the remote's grid puts its rows (`Actions.md`): `spacing.lg` from the sides; row `index` (0 = the bottom row) has its bottom `spacing.xxl + index × (tileHeight + spacing.md)` above the navigation bar, its top a `tileHeight` higher |

Colors are `@Composable @ReadOnlyComposable` getters resolved by `isDark`; sizes are plain values. Every value comes from `Style.kt`, the one place each lives. `Style.ModalOverlay` also holds one value only the overlays read, with no public getter: `PageWashAlpha` 0.55 (`AMPausedScrim.Page`) (`Overlays.md`). Android's blur radius isn't a sigma: the platform's sigma is about `0.577 × radius + 0.5`, so a design blur of sigma 20 is a radius of 34 and the frosted sigma of 24 a radius of 40. Shadows are read through `AMElevation`.

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

`None`, `Low`, `Medium`, `Text`. Each is two layers, a tight near layer and a soft wide far layer, both black (a level's `color`; only the internal `ArtTextLight` is white).

- **`Modifier.amElevation(level, shape)`**: the shape casts both layers under the content, as the platform's drop shadow painters (`GraphicsContext.shadowContext.createDropShadowPainter`, what `Modifier.dropShadow` draws with), far layer first, rebuilt only when the shape, level or appearance changes (`ShapeShadows`). **A shadow is cast by a shape, never by content**, so glyphs and nested controls never cast smudges onto their own card. Put it ahead of the surface's fill.
- **`Modifier.amTextShadow()`**: the `Text` level cast by the glyphs themselves. Internal `glyphShadow(level, tint)` records the content into a layer, then draws it three times: blurred (`BlurEffect`, `TileMode.Decal`), tinted the level's color at the layer's alpha and offset, for the far and then the near layer, each recorded with a margin of twice the blur so it isn't cut off; then the content itself, in `tint` when specified, its own colors otherwise. Internal `artGlyphs(ArtGlyphs(tone, secondary))` is the same node as art text (`amArtText`, `DataDisplay.md`): it resolves its tint (the art's ink) and level (`ArtText` under ink on dark art, `ArtTextLight` on light art) from the tone and the mode each time it draws.
- Levels: `None` keeps fill and shape and casts nothing (dense tile grids, horizontal card bands); `Low` is the default (cards, filled buttons, tiles, the toast); `Medium` is a sheet; `Text` is glyph legibility over art. Shadows run much stronger in dark mode, so they register on the dark page.

### The shadow table — `Style.Shadow` in `Style.kt`

Tune shadows here and nowhere else: (blur, y, light alpha, dark alpha) per layer.

| Level | near | far |
|---|---|---|
| `None` | 0, 0, 0, 0 | 0, 0, 0, 0 |
| `Low` | 6 dp, 2 dp, 0.10, 0.55 | 27 dp, 10 dp, 0.08, 0.45 |
| `Medium` | 10 dp, 4 dp, 0.10, 0.55 | 48 dp, 18 dp, 0.08, 0.45 |
| `Text` | 4 dp, 1 dp, 0.12, 0.12 | 16 dp, 4 dp, 0.10, 0.10 |
| `ArtText` (internal, `amArtText` on dark art) | 1 dp, 1 dp, 0.30, 0.30 | 10 dp, 2 dp, 0.28, 0.28 |
| `ArtTextLight` (internal, `amArtText` on light art; white) | 1 dp, 1 dp, 0.30, 0.30 | 10 dp, 2 dp, 0.28, 0.28 |
| `BarButton` (internal, the toolbar chip) | 6 dp, 2 dp, 0.10, 0.55 | 8 dp, 3 dp, 0.08, 0.25 |

The blur is Android's blur radius, which the platform turns into a Gaussian sigma of about `0.58 × blur + 0.5`; the values put each layer's sigma at the design's shadow softness (sigma 4 ≈ blur 6, 16 ≈ 27, 28 ≈ 48). They are tuned against rendered cards (the Elevation and Color pages).

`BarButton` is `Low` fitted inside a top app bar's row. Material clips each of a bar's rows to its bounds (`clipToBounds` on the 64 dp row), which leaves 12 dp around a toolbar chip's 40 dp circle; `Low`'s far layer would still be at about 13 % black (dark mode) at the row's edge and show as a hard-edged band behind the button. `BarButton` keeps the near layer and a far layer that has faded under 1 % (dark) and 0.3 % (light) at that edge.

## Hairlines — `Modifier.amHairline`

- **`Modifier.amHairline(shape, color = Color.Unspecified, width = 0.5.dp)`**: an outline over the content, inside the bounds so it never grows the surface, never thinner than 1 px. Unspecified `color` draws `selectionRing` for the appearance. `width = AMTheme.selectionRingWidth` marks a chosen item.
- For outlines that always show on a surface drawn by hand: sheets, avatars, the active tile's ring. The floating surfaces draw theirs through the elevated-surface helper (below): a solid card or button in dark mode only; the floating action button, the remote's tiles and the banner in both modes (`alwaysOutlined`); a frosted or tinted one the frosted edge.
- Internal `DrawScope.drawInsetOutline(shape, color, width)`: rectangles and rounded rectangles are stroked on an outline inset by half the stroke (corner radii shrink with it); any other shape is stroked twice as wide, clipped to the shape.

### The elevated surface — internal `Modifier.amElevatedSurface(shape, elevation = Low, hairline = Unspecified, alwaysOutlined = false)`

A second form takes a shadow-table row instead of an `AMElevation`, `amElevatedSurface(shape, shadow: ShadowLevel, hairline, alwaysOutlined)`, for a shadow fitted to a tight place (the toolbar chip's `Style.Shadow.BarButton`); the `AMElevation` form delegates to it with `elevation.shadowLevel`, and `ElevatedSurfaceDrawer.drawSurface` takes the `ShadowLevel`.


The recipe every floating surface wears (cards, filled buttons, toolbar chips, the remote's tiles, the floating action button, the banner, tabs), drawn around the content in one node so it never drifts. It takes no fill: its look is the app's surface setting, read while it draws (below). It doesn't clip the content; whatever the content draws in its own background sits above the fill. The drawing lives in internal `ElevatedSurfaceDrawer` (shadows, frosted slice, position; `attach`/`detach`, `position`, `ContentDrawScope.drawSurface(context, shape, shadow, look, hairline, dark, backdrop, clipContent, outline = true, alwaysOutlined = false)`), which this node, the card's node (`Modifier.amCard`, `Layout.md`), the lazy card's slice node (`Modifier.amCardSlice`), the flat strip's node (`Modifier.amFlatSurface`, `RectangleShape` at `Style.Shadow.None` with `outline = false`, `Layout.md`) and the filled buttons' private `controlSurface` (`Actions.md`) each own one of; the card draws with `clipContent = true`. `ContentDrawScope.drawSurfaceSlice(context, frame: CardSlice, shape, shadow, look, dark, backdrop, outline = true)` draws the same recipe for one row's slice of a lazy card (`Modifier.amCardSlice`, `Layout.md`): the card at the frame's `card` rect, in that rect's own frame (an `inset`), cut to its `band`, the content clipped to the card's shape. `outline = false` draws no hairline or edge.

**The look.** Each node reads the app's setting (internal `LocalAMSurfaceFill`) and resolves its look from it while it draws and while it's positioned, internal `lookOf(setting)` (`SurfaceLook.kt`), which is the pure `surfaceLook(setting, embedded)`: `Solid` in a solid app; in a frosted one, `Frosted` on the backdrop and `Tinted` where it's embedded (below), so nothing samples frost through frost. It asks `isEmbedded()`, a walk up the tree, only in a frosted app, so a solid surface repositioned every scroll frame never walks; the filled buttons' node also asks it while drawing, for a solid button's dark outline. It reads the setting, `LocalAMLiesOn` and the mode then (dynamic locals, read with `currentValueOf`), so a change redraws it without a relaunch.

- **Solid**: the shape's shadow, the shape filled with the surface color, the content, then a hairline in `selectionRing` (or `hairline`, which an embedded solid control sets to `controlOutline`): in dark mode only, where the shadow alone can't lift black off the page, or in both modes with `alwaysOutlined` (the floating action button, the remote's tiles, the banner).
- **Frosted**: the shadow clipped to outside the shape (under frost it would show through), the nearest `AMBackdrop`'s art under the shape blurred at 40 dp (`FrostedSlice`), the frosted fill, the content, then the frosted edge in both modes. **With no art to sample** (no backdrop, or before the art and the surface are placed) the page color (`Style.Page`) takes the art's place under the fill: frost over the page, opaque (`#FAFAFC` / `#0A0A0B`, internal `frostOverPage`), so it's **never unblurred translucency**. Over a flat color, blurred and unblurred are the same.
- **Tinted**: the shadow clipped to outside the shape, `Frosted.embeddedFill` in the shape with no blur of its own, the content, then the frosted edge. It never samples.
- The node tracks its position (`GlobalPositionAwareModifierNode`); when a frosted surface's offset in the art changes (a scroll) it redraws, sampling the new slice. Internal `backdropFor(look)` gives the art only to a frosted look.

### Embedded — internal `LiesOn`, `LocalAMLiesOn`, `isEmbedded()` (`SurfaceLook.kt`)

A surface with its own shape (a card, a filled button, tabs) is **embedded** when it sits on another surface: a frosted one draws the tint, and a solid control takes `controlOutline` in dark mode, since the selection ring would vanish against the black around it. Two kinds of place count, found two ways, behind one internal test, `T.isEmbedded()` (for a node that's a `DelegatableNode` and a `CompositionLocalConsumerModifierNode`, called while attached):

- **A card**: any `NestingSurfaceKey` node above the surface in the layout tree (`findNearestAncestor`), flat or not. A card is a modifier, so it can't provide a local to its content; the tree lookup finds it.
- **What the composition says**: `LocalAMLiesOn` (`LiesOn.Page`, `Surface` or `Sheet`; a dynamic `compositionLocalOf`, `Page` at the root) isn't `Page`. Content on a surface the tree lookup can't see says so: `ProvideAMOnSurface` gives `Surface` (tabs, which are no card, and what the library's containers hold), and `AMSheet` gives `Sheet` to its content for its `Primary` and `Frosted` surfaces (a sheet is a window of its own) and leaves `Page` for `Page`. An overlay window's fresh context (`ProvideOverlayWindowLocals`) resets it to `Page`, so a picker opened from a sheet starts clean.

## The surface setting and frost — `AMSurfaceFill`, `AMBackdrop`

- **`AMSurfaceFill`**: `Solid` or `Frosted`, **the app's surface setting**, on the theme: `AMTheme(surfaceFill = …)`, read with `AMTheme.surfaceFill` (internal `LocalAMSurfaceFill`, a dynamic `compositionLocalOf`, `Solid` at the root, provided only by `AMTheme`). It's set once, at the app's root (the demo's Shop scene sets `Frosted` at its root, `DemoScenes.md`); a nested `AMTheme(surfaceFill = …)` sets it for a subtree, and a nested `AMTheme` that doesn't pass it keeps the outer one. It's the only public way to set the app's setting. The setting is the surface layer's material, never the backdrop's (art, a branded picture, the page). An overlay window carries it in.
- **The containers follow the setting.** `Modifier.amCard`, `AMSection`, `amSection`, `AMDisclosureGroup`, `AMListCard`, `AMTabs` and the flat blocks (`amFlatSurface`: `AMListRow`, `AMLabeledContent`, the bar form of `AMProgressView`, a flat section) take no fill: their nodes read the setting while they draw and resolve the look from it and where they stand, `lookOf(currentValueOf(LocalAMSurfaceFill))` (above), and `AMTabs` picks its surface from `AMTheme.surfaceFill`. So do the controls with a floating surface: the filled `AMButtonStyle`s (`Actions.md`), `AMFormActions`, `AMFloatingActionButton`, `AMActionGridButton`, `AMBanner` (`Feedback.md`) and the toolbar chips (`Navigation.md`), none of which takes a fill.
- **The tint** (`AMTheme.Frosted.embeddedFill`, `Style.Frosted.EmbeddedFillLight` / `EmbeddedFillDark`: white 0.6 / black 0.5): the mode's surface color, translucent, drawn with no blur over the frost an embedded piece sits on, with the frosted edge, so it reads as a solid piece made frosty and stands out from the frost. A frosted piece with its own shape where it's embedded (a card on a sheet or lifted inside a card, a button in a card or tabs) draws it.
- **Secondary text comes from the surface** (`AMTheme.secondaryText`, `@Composable @ReadOnlyComposable`): on art (`LocalAMArtTone` non-null) the secondary ink of its tone (`Style.ArtInk`: `#D1D1D6` on dark art, `#2C2C2E` on light art); on a sheet (`LiesOn.Sheet`), or on a surface (`LiesOn.Surface`) in a frosted app, `Frosted.secondaryText`, which holds on frost, on the tint and over art an app didn't wash; on the page or a solid card Material's `onSurfaceVariant`. **`ProvideAMOnSurface(content)`** says content lies on a surface: it provides `LocalAMLiesOn` `Surface` and clears `LocalAMArtTone` (content on a surface doesn't lie on art, so it follows the mode). It takes no fill: the fill is always the setting. The library's containers provide it for what they hold, inside their own surface (`AMSection`'s card styles and `amSection`'s rows, `AMDisclosureGroup`, `AMTabs`); `AMListCard`, whose card is `ListItem`'s own node, puts each slot on the card and reads its row colors through internal `readOnSurface { }` (`withCompositionLocals`), so the card itself stays where it's placed. A hand-built `Modifier.amCard` card wraps its content in it, inside the card, since a modifier can't provide a local. An overlay window's fresh context resets it to `Page`. Readers: `AMLabeledContent`'s value, `AMProgressView`'s caption and value label, `AMTimelineLabel`'s subtitle, and `AMRowDefaults.colors()`'s supporting line (so every `AMListRow`, `AMListCard` and row on the recipe).
- **`AMBackdrop(art, modifier, tone = null, content)`**: `art` draws behind `content`, filling the region (`matchParentSize`), and records itself into a `GraphicsLayer` each time it draws (`Modifier.backdropSource`: `onPlaced` keeps its coordinates, `drawWithContent` records then draws the layer, then calls the source's `recorded()`, which tells its record listeners: a frosted sheet in another window listens to resample, `Overlays.md`). It provides `LocalAMBackdrop` and the art's tone (`LocalAMArtTone`: `tone`, or the mode's) to `content`, so it's an art surface: what lies on it reads the tone (`DataDisplay.md`). Only `art` is sampled, never the content, so a frosted surface never blurs itself or another frosted surface. The nearest backdrop wins: a row's own art beats the page's. Inside an `AMScene`'s roots, the outermost backdrop also reports its source to the scene while composed (`sceneBackdrop(source)`, which provides `LocalAMSceneBackdrops` null to `content`, so the backdrops inside it don't), for the scene's overlays to sample (`Navigation.md`). An art backdrop (`amArtBackdrop`) is usually its `art`. `AMArtRowsBackdrop` (`DataDisplay.md`) is the same kind of source whose art is the `amArtRows` page inside it, drawn by its own art box behind the region, so frosted surfaces over art rows sample the rows' colors.
- **Sampling** (`FrostedSlice`): the surface's own layer records `drawLayer(art layer)` translated to the surface's offset in the art, over a region twice the blur wider than the surface on every side, cut to the art's bounds and widened to whole pixels (`frostedSampleRegion`, unit-tested); `BlurEffect(r, r, TileMode.Clamp)`, so at the art's edge real pixels repeat instead of transparency; drawn clipped to the shape. It holds a reference to the art's layer, so new art (a photo arriving) shows without re-recording.
- **Limits**: a frosted surface must draw after its art in the same frame (inside the backdrop's content); frosting over another frosted surface isn't possible; each visible frosted surface is a blurred layer the GPU re-renders while it moves, so the solid fill stays the fallback if a phone can't hold it.

## Nesting — `AMSurfaceStyle`

- `Elevated` (fill, corners, shadow, dark hairline) or `Flat` (nothing: the surface defers to its parent).
- **`LocalAMSurfaceStyle`** (public, `Elevated` by default) carries the style a container set; **`ProvideAMSurfaceStyle(style) { }`** sets it and marks its subtree with a fresh internal `SurfaceStyleOrigin` (`LocalAMSurfaceStyleOrigin`). Set it only through `ProvideAMSurfaceStyle`.
- **A card inside a card demotes by itself.** A modifier can't set a local for content composed elsewhere, so `Modifier.amCard` resolves its style in its node (`resolveSurfaceStyle()`): `Flat` where the local says so; otherwise `Flat` when the nearest `NestingSurfaceNode` above it in the layout tree (a `TraversableNode`, `NestingSurfaceKey`) sees the same origin, `Elevated` when a `ProvideAMSurfaceStyle` sits between them (re-lifting a card inside a card) or when there's no card above. Shadows never stack and padding never doubles. The full rule is in `Layout.md`.
- The library's containers (`AMSection`, `AMDisclosureGroup`, `AMTabs`) also provide `Flat` for their content. A custom container joins by drawing its surface with `Modifier.amCard`, or by reading the local and setting `Flat` for its content. Controls never read it.

## Type roles

The Material type scale in the phone's font, sentence case everywhere. Page title = `headlineMedium` (`AMPageHeader`, expanded); section header = `titleSmall` in `primary` (`AMSectionHeader`); row title and body = `bodyLarge`; caption = `bodySmall`; code = `bodyMedium` monospace. Read them from `MaterialTheme.typography`; there is no separate role table.

## Headers

### `AMPageHeader` — `PageHeader.kt`

- `AMPageHeader(title, modifier, navigationIcon = null, actions = null, scrollBehavior = null, windowInsets = TopAppBarDefaults.windowInsets)`, and the slot form with `title: @Composable () -> Unit` last (the string form delegates to it).
- Material's `LargeTopAppBar` with `TopAppBarDefaults.topAppBarColors(containerColor = Transparent, scrolledContainerColor = AMTheme.Surface.page)`: transparent at rest, so it lies on the page, and the page color once content scrolls under it. With a `scrollBehavior` (usually `exitUntilCollapsedScrollBehavior`, its `nestedScrollConnection` on the scaffold, or handed to an `AMPullToRefreshBox` under it, which installs the connection itself so a collapsed header reopens before the pull, `Feedback.md`) it collapses Material's way; without one it stays expanded. Null slots show nothing.
- **Over a picture** (inside an `AMBackdrop`, read through `LocalAMBackdrop`, with a `scrollBehavior`): both Material colors are transparent and the bar sits in the internal `FrostedBar` (`navigation/FrostedBar.kt`, `Navigation.md`), whose fill fades in as the bar collapses (`twoRowBarFill`: `FastOutLinearInEasing` of `collapsedFraction`, the rate Material fades a two-row bar's color, read in the fill's layer so it redraws without recomposing). The fill is the frosted recipe: the backdrop's art under the bar blurred at `Frosted.blurRadius` under `Frosted.fill`, the frosted edge along the bottom, behind the status bar too; the solid surface (dark-mode hairline at the bottom) until the art is placed, never unblurred translucency. Never a page band over the art. **On art** (an art tone around it, `AMTheme.artTone`) the title's color (`titleContentColor`) is `barLineColor(filled, onSurface)` (`navigation/FrostedBar.kt`): the fixed ink of the art's tone until the fill is in past `BarLinesFilledFill` (0.9, `twoRowBarFilledState`), where `onSurface` holds 4.5:1 on the fill even over black art, then the bar's `onSurface`, animated between with `defaultEffectsSpec`. On the library's art the tone is the mode's, so at rest the title is white on dark art (8.42:1 over the brightest) or black on light art (9.98:1 over the darkest); art given the other tone gets the other ink at rest, fading over the same way.
- **Title-only headers drop the top row.** The bar's top row holds the navigation icon, the actions and the collapsed title. A header with no navigation icon, no actions and no scroll behavior would keep it empty for good, so it passes `collapsedHeight = 0.dp` and `expandedHeight = LargeAppBarExpandedHeight − LargeAppBarCollapsedHeight` (88 dp): the large title's row alone, at Material's expanded title metrics. With any of the three, it's Material's full two-row bar.
- A screen around it is a `Scaffold` with `containerColor = AMTheme.Surface.page`, or `Color.Transparent` inside an `AMBackdrop`, so the art shows behind the header.

### `AMSectionHeader` — `SectionHeader.kt`

- `AMSectionHeader(title, modifier)` and `AMSectionHeader(modifier) { RowScope content }` (the string form delegates).
- Android's list-subheader look: `titleSmall` through `ProvideTextStyle`, `LocalContentColor` = `primary`, so custom content (a glyph, a weighted title, a count) stays on-system. A `Row`, full width, parts spaced by `spacing.sm`, centered vertically. No padding of its own: place it at the row inset.
- Heading semantics, merged: a screen reader reads glyph, title and count as one heading.
- **On art** (an art tone around it, `LocalAMArtTone` non-null: inside an `AMBackdrop` or `AMArtRowsBackdrop`, and the spotlight's title) it's art text for the tone: `LocalContentColor` the tone's ink (`artInk()`: `Style.ArtInk`'s white on dark art, black on light art) and `Modifier.amArtText()` on the row, which tints glyphs and counts too and draws the tone's shadow (`DataDisplay.md`). Inside a card on the art it still reads the art's tone.

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
- **Drawing**: with a height under the bar, the content draws into a `saveLayer` (the content's height, widened by its width at each side for what it draws past them, its cards' shadows; the canvas's clip bounds it), then the mask over the band with `BlendMode.DstIn`: a vertical gradient from 0 to the height, its alpha 0 over the bar's upper half (`EdgeClear`, 0.5: the icons are centered in the bar), then Material's `FastOutLinearInEasing` from 0 to 1 at the bar's bottom edge over its lower half, sampled at 13 stops (`EdgeMaskStops`). Nothing reads behind the icons: a list opened partway down keeps what lies above it tucked under the bar, at most a card's bottom edge showing just above the bar's own. Below the band the content is unchanged.
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
