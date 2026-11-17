---
scope: The style layer's values and the theme every component reads — AMTheme and its read-only style tier, AMTokens, AMSemantic, AMElevation and the shadow table, hairlines and the elevated-surface recipe, the app's surface setting (AMSurfaceFill — solid, frosted, or the tint where embedded), the two blurs (AMBlur — Frost and Mist), AMBackdrop and frosted sampling, the motion helpers, the status-bar edge (Modifier.amStatusBarEdge), and the page and section headers
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/foundations/**, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/internal/BackdropSource.kt, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/internal/FrostedSlice.kt, androidmtrls/src/main/res/drawable/am_ic_info.xml, androidmtrls/src/main/res/drawable/am_ic_check_circle.xml, androidmtrls/src/main/res/drawable/am_ic_warning.xml, androidmtrls/src/main/res/drawable/am_ic_error.xml, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/FrostedSampleRegionTest.kt, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/SurfaceLookTest.kt, androidmtrls/src/androidTest/kotlin/com/ideamtrls/androidmtrls/SystemBarsAppearanceTest.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/foundations/**]
keywords: [AMTheme, style tier, Style.kt, MaterialTheme, MotionScheme.standard, static color scheme, baseline scheme, nested theme, darkTheme, isDark, LocalAMDarkTheme, SystemBarsAppearance, bar icons, AMTokens, AMSpacing, xs, sm, md, lg, xl, xxl, AMSemanticTints, AMSemanticColors, tone 40, tone 80, AMSemantic, Info, Success, Warning, Destructive, severityLabel, Surface.primary, Surface.page, secondaryText, artTone, Frosted.fill, Frosted.embeddedFill, the tint, Frosted.edge, Frosted.highlight, Frosted.secondaryText, Frosted.tint, accent on frost, frostedTint, frostedGround, tintedGround, windowFrostGround, frostOverPage, surfaceAccent, AMBlur, Blur.kt, Frost, Mist, blur radius, sigma, frosted blur, overlay blur, hairlineWidth, selectionRing, selectionRingWidth, controlOutline, rowVerticalPadding, Overlay.wash, PageWashAlpha, ActionGrid.tileHeight, rowBottom, quiet chip, Style.Chip, AMElevation, None, Low, Medium, Text, shadow table, ShadowLevel, ShadowLayer, BarButton, ArtText, ArtTextLight, Modifier.amElevation, Modifier.amTextShadow, glyphShadow, artGlyphs, ShapeShadows, createDropShadowPainter, Modifier.amHairline, drawInsetOutline, amElevatedSurface, ElevatedSurfaceDrawer, drawSurface, drawSurfaceSlice, alwaysOutlined, lifted, solidFill, amControlSurface, AMSurfaceFill, Solid, Frosted, surfaceFill, LocalAMSurfaceFill, SurfaceLook, Tinted, surfaceLook, lookOf, isEmbedded, embedded, LiesOn, LiesOn.EmbeddedSurface, EmbeddedSurface, card on a sheet, LocalAMLiesOn, ProvideAMOnSurface, readOnSurface, liesOnFrost, FrostPath, frostPath, OverPage, OverWindowBlur, LocalAMWindowBlur, cross-window blur, backdropFor, fieldContainer, frostedContainerColor, AMBackdrop, AMBackdropSource, LocalAMBackdrop, backdropSource, backdropScreen, screen layer, sceneBackdrop, FrostedSlice, frostedSampleRegion, SampleRegion, BlurEffect, AMSurfaceStyle, ProvideAMSurfaceStyle, type roles, AMPageHeader, TopAppBar, pinnedScrollBehavior, PageHeaderHeight, AMSectionHeader, titleSmall, list subheader, amInstantSwitch, InstantSwitch, Modifier.amCrossfade, Remove animations, Modifier.amStatusBarEdge, status bar edge, headerless screen, WindowInsetsRulers, alpha mask, DstIn, SurfaceLookTest, FrostedSampleRegionTest, SystemBarsAppearanceTest]
updated: 2026-11-17
---

# Foundations

The style layer's values and the theme every component reads: `AMTheme` and its tokens, the status roles, shadows, hairlines and the elevated-surface recipe, the app's surface setting and frost, the motion helpers, the status-bar edge, and the page and section headers.

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `foundations/Style.kt` | internal `Style`: every raw style value, each color light and dark, and **the shadow table**; `ShadowLayer`, `ShadowLevel` |
| `foundations/Theme.kt` | `AMTheme(darkTheme, colorScheme, tokens, surfaceFill, content)`, `object AMTheme` (the read-only style tier, `tokens`, `surfaceFill`, `isDark`, `artTone`, `secondaryText`); internal `LocalAMDarkTheme`, `LocalAMTokens`, `surfaceAccent`, the grounds `frostedGround`, `tintedGround`, `windowFrostGround`, and `frostOverPage`, `frostedTint`; private `SystemBarsAppearance` |
| `foundations/Blur.kt` | `AMBlur` (`Frost`, `Mist`) |
| `foundations/Tokens.kt` | `AMTokens`, `AMSpacing`, `AMSemanticTints`, `AMSemanticColors` |
| `foundations/Semantic.kt` | `AMSemantic` |
| `foundations/Elevation.kt` | `AMElevation`, `Modifier.amElevation`, `Modifier.amTextShadow`; internal `glyphShadow`, `artGlyphs`, `ArtGlyphs`, `ShapeShadows` |
| `foundations/Hairline.kt` | `Modifier.amHairline`; internal `drawInsetOutline`, `Modifier.amElevatedSurface`, `ElevatedSurfaceDrawer` |
| `foundations/ControlSurface.kt` | internal `Modifier.amControlSurface`; private `ControlSurfaceNode` |
| `foundations/SurfaceStyle.kt` | `AMSurfaceStyle`, `LocalAMSurfaceStyle`, `ProvideAMSurfaceStyle`; internal `SurfaceStyleOrigin`, `LocalAMSurfaceStyleOrigin`, `NestingSurfaceKey`, `NestingSurfaceNode`, `resolveSurfaceStyle` |
| `foundations/SurfaceFill.kt` | `AMSurfaceFill` (the setting), `ProvideAMOnSurface`; internal `LocalAMSurfaceFill`, `readOnSurface` |
| `foundations/SurfaceLook.kt` | internal `SurfaceLook`, `LiesOn` (`Page`, `Surface`, `EmbeddedSurface`, `Sheet`), `LocalAMLiesOn`, `surfaceLook`, `isEmbedded`, `lookOf`, `FrostPath`, `frostPath`, `LocalAMWindowBlur`, `backdropFor`, `frostedContainerColor`; `FieldContainer`, `fieldContainer`, `fieldContainerKind`, `fieldContainerColor` (a text field's container, `Inputs.md`) |
| `foundations/Backdrop.kt` | `AMBackdrop` |
| `internal/BackdropSource.kt` | `AMBackdropSource` (the art's `layer` and the region's `screen`), `LocalAMBackdrop`, `Modifier.backdropSource`, `Modifier.backdropScreen`; `AMSceneBackdrops`, `LocalAMSceneBackdrops`, `sceneBackdrop` (a scene root's page backdrops) and `artStatusBar`, `ReportTopCover`, `AMBackdropSource.topCovers` (art deciding the status bar icons), both `Navigation.md`'s |
| `internal/FrostedSlice.kt` | `FrostedSlice`, `SampleRegion`, `frostedSampleRegion` |
| `foundations/PageHeader.kt` | `AMPageHeader` (string and slot forms) |
| `foundations/SectionHeader.kt` | `AMSectionHeader` (string and slot forms) |
| `foundations/Motion.kt` | `amInstantSwitch`, `Modifier.amCrossfade`, internal `InstantSwitch` |
| `foundations/ScrollEdge.kt` | `Modifier.amStatusBarEdge`; private `StatusBarEdgeNode`, `EdgeMaskStops`, `EdgeClear` |

- `foundations/Enabled.kt` (`LocalAMEnabled`, `resolveEnabled`, `ProvideAMEnabled`) sits in this folder but is `Actions.md`'s.
- Glyphs: `androidmtrls/src/main/res/drawable/am_ic_info.xml`, `am_ic_check_circle.xml`, `am_ic_warning.xml`, `am_ic_error.xml` (`AMSemantic.icon`), the filled Material Symbols (`<name>_fill1_24px`).
- Tests: `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/FrostedSampleRegionTest.kt` (the sampling rectangle), `SurfaceLookTest.kt` (the setting deciding the look, a field's container, none on a card on a sheet, the frost path in and out of a blurred window), `ArtWashTest.kt` (`DataDisplay.md`'s; it also pins frost over the page, the accent on frost, and text and chip labels on frost, the tint, the quiet chip and a blurred window's frost); instrumented, `androidmtrls/src/androidTest/kotlin/com/ideamtrls/androidmtrls/SystemBarsAppearanceTest.kt` (the outermost theme's bar icons, a nested theme leaving them, and art turning the status bar's over).
- From other areas: `Modifier.amArtText`, `LocalAMArtTone`, `artInk`, `moveToLuminance` (`datadisplay/ArtBackdrop.kt`, `datadisplay/ArtTone.kt`) and the sRGB helpers `srgbToLinear` / `linearToSrgb` (`datadisplay/ArtImages.kt`), `DataDisplay.md`; `FrostedBar`, `barScrolledState`, `barFill`, `barLineColor` (`navigation/FrostedBar.kt`), `Navigation.md`.
- Catalog pages: `demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/foundations/` (below).

## The theme — `AMTheme`

```kotlin
AMTheme { App() }                                                      // baseline scheme, following the system
AMTheme(darkTheme = dark, colorScheme = if (dark) BrandDark else BrandLight) { App() }
AMTheme(surfaceFill = AMSurfaceFill.Frosted) { App() }                 // a frosted app
AMTheme(tokens = AMTheme.tokens.copy(spacing = AMTheme.tokens.spacing.copy(lg = 20.dp))) { Feed() }
```

- **`AMTheme(darkTheme, colorScheme, tokens, surfaceFill, content)`** installs `MaterialTheme(colorScheme, MotionScheme.standard(), shapes, typography)`, keeping the current (default) shapes and type scale, then provides `LocalAMDarkTheme`, `LocalAMTokens` and `LocalAMSurfaceFill`. `darkTheme` comes first so `colorScheme`'s default can follow it.
- **Defaults.** `darkTheme`: the enclosing `AMTheme`'s flag, else `isSystemInDarkTheme()`. `colorScheme`: inside an `AMTheme` showing the same appearance, the current `MaterialTheme.colorScheme`; otherwise Material's baseline static scheme (`lightColorScheme()` / `darkColorScheme()`). `tokens`: the current `AMTheme.tokens` (`AMTokens.Default` at the root). `surfaceFill`: the current `AMTheme.surfaceFill` (`Solid` at the root). So a nested theme that only changes tokens keeps the app's colors, a forced appearance and its surface setting, the way a nested `MaterialTheme` keeps what it isn't given.
- **Never dynamic color.** The scheme is static. Type, color, shape and motion are overridden through Material: a `colorScheme` here, or a `MaterialTheme(shapes = …)` / `MaterialTheme(typography = …)` inside.
- **The appearance flag rules every style color.** Everything that differs by mode reads `LocalAMDarkTheme` (dynamic, so modifier nodes that read it while drawing redraw on a change), never `isSystemInDarkTheme()`. An app (or the demo's inspection menu) can force either appearance. Outside any `AMTheme` the style reads as light with `AMTokens.Default`, and surfaces as solid.
- **The system bars follow it.** The outermost `AMTheme` in a window (no `LocalAMDarkTheme` above it) composes private `SystemBarsAppearance(darkTheme)` ahead of its content: a `DisposableEffect` keyed on `LocalView` and the flag that sets `APPEARANCE_LIGHT_STATUS_BARS | APPEARANCE_LIGHT_NAVIGATION_BARS` on the view's `windowInsetsController` in light mode and clears both in dark, so a pinned appearance reaches the bar icons. Nested themes and overlay windows (which inherit the flag) leave the bars alone; previews (`LocalInspectionMode`) do nothing. Edge to edge stays the activity's (`enableEdgeToEdge()` once). Composed first, it runs before the art's status bar claims on a change, so they take the status bar again on top of it (`AMStatusBarIcons`, `Navigation.md`).

### The read-only style tier — `object AMTheme`

| Member | Value |
|---|---|
| `tokens` | the current `AMTokens` |
| `surfaceFill` | the app's surface setting, `AMSurfaceFill.Solid` or `Frosted` (below) |
| `isDark` | whether the theme shows its dark appearance |
| `Surface.primary` | the floating surface: `White` / `Black`. Solid cards, filled buttons, tiles, the floating action button and the banner fill with it; no tonal tint |
| `Surface.page` | the tuned page: `#F2F2F7` / `#141416`, far enough above black that black surfaces read as floating; also what art shows with no picture (`DataDisplay.md`) |
| `secondaryText` | secondary text for the surface the composition lies on (below): on art the tone's secondary ink (`#D1D1D6` on dark art, `#2C2C2E` on light, `DataDisplay.md`); on a sheet, or on a surface (a card on a sheet included) in a frosted app, `Frosted.secondaryText` (private `liesOnFrost`); else `onSurfaceVariant` |
| `artTone` | the tone (`AMArtTone`) of the nearest art surface (`AMBackdrop`, `AMArtRowsBackdrop`): the mode's unless the app gave the surface one; null off art and on a surface (`DataDisplay.md`) |
| `Frosted.fill` | white 0.6 / black 0.5 over the blur |
| `Frosted.embeddedFill` | white 0.6 / black 0.5, **the tint**: what a piece embedded in a frosted surface fills with, over the frost under it, with no blur of its own |
| `Frosted.edge` | white 0.6 / white 0.15, the outline of frost and of the tint in both modes |
| `Frosted.highlight` | white 0.5 / white 0.12, a lifted row on frost |
| `Frosted.secondaryText` | `#2C2C2E` / `#D1D1D6`, supporting text on frost, on the tint and on a sheet: 12.5:1 / 12.2:1 on the tint over frosted art (internal `tintedGround`, L 0.894 / 0.007), 13.4:1 / 13.0:1 on frost over the page (`frostOverPage`), 9.6:1 / 9.1:1 on frost over a blurred window's wash over the worst screen (`windowFrostGround`: black behind in light mode, white in dark; L 0.674 / 0.026). `ArtWashTest` pins each at 4.5:1 |
| `Frosted.tint` | the accent on frost: the scheme's `primary` moved along its own line just far enough to hold 4.5:1 on the frosted fill over the library's washed art at its hardest (internal `frostedTint(color, dark)` against `frostedGround(dark)`: the fill over `Style.ArtInk.LightArtGround` in light mode, L 0.749, or over `DarkArtCeiling` in dark, L 0.020), so it holds on the tint too. Material's baseline `primary` holds unchanged (4.90:1 light, 8.81:1 dark); a lighter brand accent comes back darker. Over art an app didn't wash it's the app's to check. Internal `surfaceAccent()` gives it wherever `secondaryText` gives the frosted secondary, `primary` elsewhere: an on `AMIconButton` toggle and an active `AMCardButton` in `Info` use it |
| `hairlineWidth` | 0.5 dp, drawn no thinner than 1 px |
| `selectionRing` | `#C7C7CC` / `#3A3A3C`, asymmetric on purpose; also the default hairline color |
| `selectionRingWidth` | 1.5 dp, a chosen row or tile |
| `controlOutline` | `#C7C7CC` / `#636366`, the dark-mode outline of a solid control embedded in a dark surface: inside a card, tabs or a section, or on a sheet (`isEmbedded`, below) |
| `rowVerticalPadding` | 28 dp, the row recipe's space above and below a row's content (`Style.Row`); the recipe and the row heights it makes are in `Layout.md` |
| `Overlay.wash` | the spotlight's recipe: the screen behind blurred by `AMBlur.Mist` (below) under white 0.6 / black 0.65, the mode's wash, so the spotlight's title, in the tone's fixed ink, holds 7.36:1 over sharp black art (light mode) and 7.00:1 over sharp white art (dark mode), blurred or not (`Overlays.md`) |
| `ActionGrid.tileHeight` | 76 dp, fixed whatever the font scale |
| `ActionGrid.sidePadding`, `ActionGrid.rowBottom(index)`, `ActionGrid.rowTop(index)` | where the remote's grid puts its rows (`Actions.md`): `spacing.lg` from the sides; row `index` (0 = the bottom row) has its bottom `spacing.xxl + index × (tileHeight + spacing.md)` above the navigation bar, its top a `tileHeight` higher |

Colors are `@Composable @ReadOnlyComposable` getters resolved by `isDark`; sizes are plain values. Every value comes from `Style.kt`, the one place each lives; no component keeps a literal of its own. Three have no public getter: `Style.ModalOverlay.PageWashAlpha` 0.55 (`AMPausedScrim.Page`, `Overlays.md`), `Style.ModalOverlay.ScrimDark`, black 0.4, the remote's cover over its blur in dark mode, which `AMBackdropBlur` and `AMPausedScrim.Page` lay over their blur in dark mode too (`Overlays.md`), and `Style.Chip`, the quiet chip, an unselected chip's fill in a solid app (`QuietFillLight` `#E5E5EA` / `QuietFillDark` `#2C2C2E`, opaque, one gray step off the page away from the surface; `Selection.md`). Android's blur radius isn't a sigma: the platform's sigma is about `0.577 × radius + 0.5`, so a design sigma of 20 is a radius of 34 and 24 a radius of 40. Shadows are read through `AMElevation`, blurs through `AMBlur`.

## Blurs — `AMBlur` (`Blur.kt`)

The library's two blurs of what lies behind, pure Gaussian blurs with no tint, a closed enum named by how much of the scene they keep; `radius` is Android's blur radius (a `Dp`, read in the library as `AMBlur.Frost.radius` / `AMBlur.Mist.radius`).

- **`Frost`**: 40 dp (`Style.Frosted.BlurRadius`, sigma ≈ 24), what frosted surfaces blur the art behind them by: cards, sections, bars, banners and sheets in a frosted app (the elevated surface's slice, `Hairline.kt`; the frosted bar, `navigation/FrostedBar.kt`; the sheet's frost, `overlays/SheetChrome.kt`).
- **`Mist`**: 34 dp (`Style.ModalOverlay.BlurRadius`, sigma ≈ 20), lighter: the remote's and the spotlight's window blur (`overlays/OverlayWindow.kt`), and the default of `AMBackdropBlur(modifier, blur = AMBlur.Mist)` and `Modifier.amPausedBackdrop(scrim, blur = AMBlur.Mist)`, which take either (`Overlays.md`).

The art behind a scene has blurs of its own (`amArtBackdrop`, `AMArtDefaults`, `DataDisplay.md`), set by how much of the picture it keeps.

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

`None`, `Low`, `Medium`, `Text`. Each is two layers, a tight near layer and a soft wide far layer, in the level's `color`: black, except the internal `ArtTextLight`, white.

- **`Modifier.amElevation(level, shape)`**: the shape casts both layers under the content, as the platform's drop shadow painters (`GraphicsContext.shadowContext.createDropShadowPainter`, what `Modifier.dropShadow` draws with), far layer first, rebuilt only when the shape, level or appearance changes (`ShapeShadows`). **A shadow is cast by a shape, never by content**, so glyphs and nested controls never cast smudges onto their own card. Put it ahead of the surface's fill.
- **`Modifier.amTextShadow()`**: the `Text` level cast by the glyphs themselves. Internal `glyphShadow(level, tint)` records the content into a layer, then draws it three times: blurred (`BlurEffect`, `TileMode.Decal`) and tinted the level's color at the layer's alpha and offset, for the far and then the near layer, each recorded with a margin of twice the blur so it isn't cut off; then the content itself, in `tint` when specified, its own colors otherwise. Internal `artGlyphs(ArtGlyphs(tone, secondary))` is the same node as art text (`amArtText`, `DataDisplay.md`), resolving its ink and level from the tone each time it draws.
- Levels: `None` keeps fill and shape and casts nothing (dense tile grids, horizontal card bands); `Low` is the default (cards, filled buttons, tiles; the toast is Material's snackbar and keeps Material's own shadow); `Medium` is a sheet, and a piece lifted under a drag (a reordered row, the floating action button); `Text` is glyph legibility over art. Shadows run much stronger in dark mode, so they register on the dark page.

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

The blur is Android's blur radius (sigma ≈ `0.58 × blur + 0.5`); the values put each layer's sigma at the design's softness (sigma 4 ≈ blur 6, 16 ≈ 27, 28 ≈ 48), tuned against rendered cards (the Elevation and Color pages).

`BarButton` is `Low` fitted inside a top app bar's row. Material clips each of a bar's rows to its bounds, and the 64 dp row leaves 12 dp around a toolbar chip's 40 dp circle, where `Low`'s far layer would be cut into a hard-edged band; `BarButton` keeps the near layer and a far layer that has faded under 1 % at that edge.

## Hairlines — `Modifier.amHairline`

- **`Modifier.amHairline(shape, color = Color.Unspecified, width = 0.5.dp)`**: an outline over the content, inside the bounds so it never grows the surface, never thinner than 1 px. Unspecified `color` draws `selectionRing` for the appearance. `width = AMTheme.selectionRingWidth` marks a chosen item.
- For outlines on a surface drawn by hand: avatars, the active tile's ring, a chosen list row's ring, and the frosted edge of a Material surface that follows the setting by container color (the toast, the alert). The floating surfaces draw theirs through the elevated surface (below); a sheet's chrome draws the frosted edge itself (`Overlays.md`).
- Internal `DrawScope.drawInsetOutline(shape, color, width)`: rectangles and rounded rectangles are stroked on an outline inset by half the stroke (corner radii shrink with it); any other shape is stroked twice as wide, clipped to the shape.

### The elevated surface — internal `Modifier.amElevatedSurface(shape, elevation = Low, hairline = Unspecified, alwaysOutlined = false, lifted = false)`

The recipe every floating surface wears (cards, filled buttons, toolbar chips, the remote's tiles, the floating action button, the banner, tabs in a frosted app), drawn around the content in one node so it never drifts. A second form takes a shadow-table row (`shadow: ShadowLevel`) in place of `elevation`, for a shadow fitted to a tight place (the toolbar chip's `Style.Shadow.BarButton`). It takes no fill: its look is the app's surface setting, read while it draws (below). It doesn't clip the content; whatever the content draws in its own background sits above the fill.

**One drawer.** The drawing lives in internal `ElevatedSurfaceDrawer` (the shape's shadows, the frosted slice, the surface's position), which each surface node owns one of: this node, the control surface's (below), and in `layout/Card.kt` the card's (`Modifier.amCard`, drawing with `clipContent = true`), the lazy card's slice (`Modifier.amCardSlice`, through `drawSurfaceSlice`: the card at the frame's `card` rect, cut to its `band`) and the flat strip's (`Modifier.amFlatSurface`: `RectangleShape`, `Style.Shadow.None`, `outline = false`) (`Layout.md`). A node attaches and detaches it with itself, reports `position(coordinates, backdrop)` from `onGloballyPositioned` (true when a frosted surface moved over its art, a scroll, so it redraws its slice), and draws `drawSurface(context, shape, shadow, look, hairline, dark, backdrop, windowBlur, clipContent, outline = true, alwaysOutlined = false, solidFill = Unspecified)`, passing `currentValueOf(LocalAMWindowBlur)` as `windowBlur`. `outline = false` draws no hairline or edge; `solidFill` replaces the surface color in the solid look.

**The look.** Each node reads the app's setting (internal `LocalAMSurfaceFill`) while it draws and while it's positioned and resolves its look, internal `lookOf(setting)` (`SurfaceLook.kt`), the pure `surfaceLook(setting, embedded)`: `Solid` in a solid app; in a frosted one, `Frosted` on the backdrop and `Tinted` where it's embedded (below), so nothing samples frost through frost. `lookOf` asks `isEmbedded()`, a walk up the tree, only in a frosted app, so a solid surface repositioned every scroll frame never walks; the control surface's node also asks it while drawing, for a solid control's dark outline. The setting, `LocalAMLiesOn` and the mode are dynamic locals read with `currentValueOf`, so a change redraws without a relaunch. Internal `backdropFor(look)` hands the art only to a frosted look.

- **Solid**: the shape's shadow, the shape filled with the surface color (or `solidFill`), the content, then a hairline in `selectionRing` (or `hairline`, which an embedded solid control sets to `controlOutline`): in dark mode only, where the shadow alone can't lift black off the page, or in both modes with `alwaysOutlined` (the floating action button, the remote's tiles, the banner).
- **Frosted**: the shadow clipped to outside the shape (under frost it would show through), the nearest `AMBackdrop`'s art under the shape blurred by `AMBlur.Frost` (40 dp, `FrostedSlice`), the frosted fill, the content, then the frosted edge in both modes. **With no art to sample** (no backdrop, or before the art and the surface are placed) the page color takes the art's place under the fill: frost over the page, opaque (`#FAFAFC` / `#0A0A0B`, internal `frostOverPage`), so it's **never unblurred translucency**. Over a flat color, blurred and unblurred are the same.
  - **Its path** is the pure internal `frostPath(artPlaced, windowBlur)` (`FrostPath`), taken before anything draws: outside a blurred window (`windowBlur` null) `Sampled` once the slice is placed (`FrostedSlice.place` records it, `drawPlaced` draws it), `OverPage` until then; in the spotlight's blurred window (internal `LocalAMWindowBlur`, a dynamic `compositionLocalOf<Boolean?>` the blurred window provides through `ProvideOverlayWindowLocals`, `Overlays.md`) `OverWindowBlur` while the system has cross-window blur on (the frosted fill straight over the window's blurred, washed screen, sampling nothing) and `Solid` while it's off (the solid look entire, never see-through).
- **Tinted**: the shadow clipped to outside the shape, `Frosted.embeddedFill` in the shape with no blur of its own, the content, then the frosted edge. It never samples.
- **`lifted`**: a surface lifted off what holds it, following a drag over its neighbors (a reorderable list's dragged row, `Lists.md`), resolves `surfaceLook(setting, embedded = false)` wherever it stands, so it's never the tint, which would let the rows it passes over show through: in a frosted app it's frosted even inside a card or on a sheet, over the art under it or, with none, frost over the page color.
- **A Material surface that follows by container color alone** (the toast's snackbar, the alert's dialog, `Feedback.md`, `Overlays.md`) takes internal `frostedContainerColor()` (`@Composable @ReadOnlyComposable`): in a frosted app `frostOverPage(isDark)`, opaque, wherever it shows; in a solid app `Color.Unspecified`, so the caller keeps Material's own. It's never the tint: such a surface samples no art and floats over content (a toast over a sheet's rows), which the tint, having no blur, would let show through.

### The control surface — internal `Modifier.amControlSurface(shape, elevation, solidFill = Unspecified, outline = true)` (`ControlSurface.kt`)

The surface a control with its own shape wears: the filled buttons (`Actions.md`); an unselected chip in either setting, its solid look the quiet chip's gray (`solidFill`); in a frosted app only, the segmented control's backing (`Selection.md`) and a tag's pill (`DataDisplay.md`); and the band behind a banner (`RectangleShape`, no shadow or edge, faded by an alpha mask; `Feedback.md`). Its node owns an `ElevatedSurfaceDrawer` and draws in `surfaceLook(setting, isEmbedded())`: solid in a solid app; in a frosted one frost on the backdrop and the tint where it's embedded. Embedded and solid, its dark-mode hairline is `controlOutline`; on the page, the selection ring. Frost and the tint ignore `solidFill`. `outline = false` draws neither the dark hairline nor the frosted edge, for a control whose Material outline stays (the chip, the segmented buttons). It goes after the control's `minimumInteractiveComponentSize()`, the control inside laid out under `LocalMinimumInteractiveComponentSize provides 0.dp`, so the 48 dp target is reserved outside the surface and the surface hugs the face at any font scale.

### Embedded — internal `LiesOn`, `LocalAMLiesOn`, `isEmbedded()` (`SurfaceLook.kt`)

A surface with its own shape (a card, a filled button, tabs) is **embedded** when it sits on another surface: a frosted one draws the tint, and a solid control takes `controlOutline` in dark mode, since the selection ring would vanish against the black around it. Internal `T.isEmbedded()` (for a `DelegatableNode` that's a `CompositionLocalConsumerModifierNode`, called while attached) finds two kinds of place:

- **A card**: any `NestingSurfaceKey` node above it in the layout tree (`findNearestAncestor`), flat or not. A card is a modifier, so it can't provide a local to its content; the tree lookup finds it.
- **What the composition says**: `LocalAMLiesOn` (`LiesOn.Page`, `Surface`, `EmbeddedSurface` or `Sheet`; dynamic, `Page` at the root) isn't `Page`. `ProvideAMOnSurface` puts content on a surface (tabs, which are no card, and what the library's containers hold): `Surface` on the page, `EmbeddedSurface` on a sheet (a floating surface there, tinted in a frosted app and solid in a solid one, whose content lies on the surface's own fill, so a field there draws none, `Inputs.md`), and what it was inside a surface already, so providing it twice over one surface changes nothing; `AMSheet` gives its content `Sheet` (a sheet is a window of its own, frosted in either setting), so on a sheet a card, a filled button or a self-filled row is solid in a solid app and tinted in a frosted one. An overlay window's fresh context (`ProvideOverlayWindowLocals`) resets it to `Page`, so a picker opened from a sheet starts clean; the spotlight puts its card's content back on a surface.

## The surface setting and frost — `AMSurfaceFill`, `AMBackdrop`

- **`AMSurfaceFill`**: `Solid` or `Frosted`, **the app's surface setting**, on the theme: `AMTheme(surfaceFill = …)`, read with `AMTheme.surfaceFill` (internal `LocalAMSurfaceFill`, dynamic, `Solid` at the root, provided by `AMTheme`, and set `Solid` inside only by the remote, `AMActionsOverlay`, and the + that opens it, `AMFloatingActionButton`; there's no other public way to set it). It's set once, at the app's root (every demo scene sets `Frosted` at its root, `DemoScenes.md`); a nested `AMTheme(surfaceFill = …)` sets it for a subtree, and a nested `AMTheme` that doesn't pass it keeps the outer one. It's the surface layer's material, never the backdrop's (art, a branded picture, the page). An overlay window carries it in.
- **The containers follow the setting.** `Modifier.amCard`, `AMSection`, `amSection`, `AMDisclosureGroup`, `AMListCard`, `AMTabs` and the flat blocks (`amFlatSurface`: `AMListRow`, `AMLabeledContent`, the bar form of `AMProgressView`, a flat section, a reorderable row) take no fill: their nodes resolve the look from the setting and where they stand (above). So do the controls on the elevated or control surface: the filled buttons, the action-grid tiles, `AMBanner`, the toolbar chips, chips, the segmented control and tags. The floating action button and everything in the remote are solid in either setting, so the app's main navigation is always easy to see (`Actions.md`, `Overlays.md`).
- **The tint** (`AMTheme.Frosted.embeddedFill`): the mode's surface color, translucent, drawn with no blur over the frost an embedded piece sits on, with the frosted edge, so it reads as a solid piece made frosty and stands out from the frost. A frosted piece with its own shape where it's embedded (a card on a sheet or lifted inside a card, a button in a card or tabs) draws it.
- **Secondary text comes from the surface** (`AMTheme.secondaryText`, table above), read from `LocalAMArtTone` and `LocalAMLiesOn`. **`ProvideAMOnSurface(content)`** says content lies on a surface: it provides `LocalAMLiesOn` `Surface`, or `EmbeddedSurface` on a sheet (above), and clears `LocalAMArtTone` (content on a surface doesn't lie on art, so it follows the mode). It takes no fill. The library's containers provide it for what they hold, inside their own surface (`AMSection`'s card styles and `amSection`'s rows, `AMDisclosureGroup`, `AMTabs`, the flat blocks, the spotlight's card); a block whose surface is `ListItem`'s own node (`AMListCard`, `AMListRow`, `AMLabeledContent`) puts each slot on it and reads its row colors through internal `readOnSurface { }` (`withCompositionLocals`), so the surface itself stays where it's placed. A hand-built `Modifier.amCard` card wraps its content in it, inside the card, since a modifier can't provide a local. Readers: `AMRowDefaults.colors()`'s supporting line (so every row on the recipe), `AMLabeledContent`'s value, `AMProgressView`'s caption and value label, `AMTimelineLabel`'s subtitle, an inactive `AMCardButton`.
- **`AMBackdrop(art, modifier, tone = null, content)`**: `art` draws behind `content`, filling the region (`matchParentSize`), and records itself into a `GraphicsLayer` each time it draws (`Modifier.backdropSource`: `onPlaced` keeps its coordinates, `drawWithContent` records then draws the layer). Its region's box records the region whole, the art and everything on it, into the source's second layer, `screen`, the same way (`Modifier.backdropScreen`), for a sheet over it to blur from its own window (`Overlays.md`); the art fills the box, so both layers share the art's coordinates. It provides `LocalAMBackdrop` and the art's tone (`LocalAMArtTone`: `tone`, or the mode's) to `content`, so it's an art surface (`DataDisplay.md`), and while its region reaches the top of the window it decides the status bar's icons for its tone (`artStatusBar`, `Navigation.md`). The surfaces in `content` sample only `art`, never the content, so a frosted surface never blurs itself or another frosted surface; a sheet, a window over the screen, samples `screen`. The nearest backdrop wins: a row's own art beats the page's. Inside an `AMScene`'s roots, the outermost backdrop reports its source to the scene while composed (`sceneBackdrop(source)`, which provides `LocalAMSceneBackdrops` null to `content`, so the backdrops inside it don't), for the scene's overlays to sample (`Navigation.md`). `AMArtRowsBackdrop` is the same kind of source over art rows (`DataDisplay.md`).
- **Sampling** (`FrostedSlice`): the surface's own layer records `drawLayer(art layer)` translated to the surface's offset in the art, over a region twice the blur wider than the surface on every side, cut to the art's bounds and widened to whole pixels (`frostedSampleRegion`, unit-tested); `BlurEffect(r, r, TileMode.Clamp)`, so at the art's edge real pixels repeat instead of transparency; drawn clipped to the shape. It holds a reference to the art's layer, so new art (a photo arriving) shows without re-recording.
- **Limits**: a frosted surface must draw after its art in the same frame (inside the backdrop's content); frosting over another frosted surface isn't possible; each visible frosted surface is a blurred layer the GPU re-renders while it moves, so the solid fill stays the fallback if a phone can't hold it.

## Nesting — `AMSurfaceStyle`

- `Elevated` (fill, corners, shadow, dark hairline) or `Flat` (nothing: the surface defers to its parent). **`LocalAMSurfaceStyle`** (public, `Elevated` at the root) carries what a container set; set it only through **`ProvideAMSurfaceStyle(style) { }`**, which also marks its subtree with a fresh internal `SurfaceStyleOrigin` (`LocalAMSurfaceStyleOrigin`), so a card re-lifted inside a card knows to draw.
- A card inside a card demotes by itself: `Modifier.amCard` resolves its style in its node (`resolveSurfaceStyle()`) against the nearest `NestingSurfaceNode` above it, so shadows never stack and padding never doubles. The library's containers provide `Flat` for their content; controls never read it. The resolution rule and the containers are `Layout.md`'s.

## Type roles

The Material type scale in the phone's font, sentence case everywhere. Page title = `headlineMedium` (`AMPageHeader`); section header = `titleSmall` in `primary` (`AMSectionHeader`); row title and body = `bodyLarge`; caption = `bodySmall`; code = `bodyMedium` monospace. Read them from `MaterialTheme.typography`; there is no separate role table.

## Headers

### `AMPageHeader` — `PageHeader.kt`

- `AMPageHeader(title, modifier, navigationIcon = null, actions = null, scrollBehavior = null, windowInsets = TopAppBarDefaults.windowInsets)`, and the slot form with `title: @Composable () -> Unit` last (the string form delegates to it).
- Material's one-row `TopAppBar` at `expandedHeight = PageHeaderHeight` (internal, 72 dp): the title in `headlineMedium` (through `ProvideTextStyle`; the string form one line, ellipsized), the navigation icon before it and the actions after it, on the same row. `containerColor = Transparent, scrolledContainerColor = AMTheme.Surface.page`: transparent at rest, so it lies on the page, and the page color, with Material's color animation, once its `scrollBehavior` reports content under it (the overlap past 0.01). The bar never moves or changes height: the scroll behavior is `pinnedScrollBehavior`, its `nestedScrollConnection` on the scaffold, or handed to an `AMPullToRefreshBox` under it, which installs the connection inside the pull so the pull never counts as scrolling (`Feedback.md`). Without one it stays transparent. Null slots show nothing; a title-only header is the same bar.
- **Over a picture** (inside an `AMBackdrop`, `LocalAMBackdrop` non-null, with a `scrollBehavior`): both Material colors are transparent and the bar sits in the internal `FrostedBar` (`Navigation.md`), whose frosted fill fades in once content scrolls under it (`barScrolledState`, Material's own overlap test, and `barFill`, animated with `motionScheme.defaultEffectsSpec()` as Material fades its scrolled color), exactly as `AMPanelChrome`'s: the art under the bar blurred under `Frosted.fill`, the frosted edge along the bottom, behind the status bar too; the solid surface until the art is placed. Never a page band over the art. While scrolled the bar tells its art it covers the status bar (`FrostedBar(covers = scrolled)`), so the art's tone stops deciding the icons there.
- **The title's color** is `barLineColor(scrolled, onSurface)`: on art (an art tone around it) the tone's fixed ink until content scrolls under the bar, then `onSurface`, animated with `defaultEffectsSpec` as the fill comes in; off art, `onSurface`. On the library's art the tone is the mode's, so the title barely changes (white on dark art, 8.42:1 over the brightest; black on light art, 9.98:1 over the darkest); art given the other tone fades over.
- A screen around it is a `Scaffold` with `containerColor = AMTheme.Surface.page`, or `Color.Transparent` inside an `AMBackdrop`, so the art shows behind the header.

### `AMSectionHeader` — `SectionHeader.kt`

- `AMSectionHeader(title, modifier)` and `AMSectionHeader(modifier) { RowScope content }` (the string form delegates).
- Android's list-subheader look: `titleSmall` through `ProvideTextStyle`, `LocalContentColor` = `primary`, so custom content (a glyph, a weighted title, a count) stays on-system. A `Row`, full width, parts spaced by `spacing.sm`, centered vertically. No padding of its own: place it at the row inset.
- Heading semantics, merged: a screen reader reads glyph, title and count as one heading.
- **On art** (`LocalAMArtTone` non-null: inside an `AMBackdrop` or `AMArtRowsBackdrop`, and the spotlight's title) it's art text for the tone: `LocalContentColor` the tone's ink (`artInk()`) and `Modifier.amArtText()` on the row, which tints glyphs and counts too and draws the tone's shadow (`DataDisplay.md`). On a surface (`ProvideAMOnSurface`, which the library's containers provide) the tone is cleared, so a header inside a card has the plain look.

## Motion helpers — `Motion.kt`

Components own their own motion; these two are for an app that swaps a view's content itself. The system's **Remove animations** (animator duration scale 0) makes them instant, as Compose does for all its animation; there is no extra machinery for it.

- **`amInstantSwitch { changes }`**: applies the changes in one `Snapshot.withMutableSnapshot`, so they land together in one frame, and bumps `InstantSwitch.generation` in the same snapshot. Keep the dismissal of whatever triggered the switch outside it, so that still animates.
- **`Modifier.amCrossfade(value)`**: when `value` changes, the old content leaves at once and the new fades in (a layer's alpha, 0 to 1) with `MaterialTheme.motionScheme.defaultEffectsSpec()`, read in the node through `MaterialTheme.LocalMaterialTheme`. The factory reads `InstantSwitch.generation` while composing, so every call site recomposes on a switch; a value change that arrives with a new generation (made inside `amInstantSwitch`) snaps instead of fading. Call it while composing.

## Status-bar edge — `Modifier.amStatusBarEdge()` (`ScrollEdge.kt`)

A top app bar takes content scrolling under it into its fill (`AMPageHeader`, `AMToolbar`, `AMPanelChrome`: the page color, or the frosted fill over art), status bar included. A screen with no top bar (a view whose header scrolls away with its content, a list that opens on its first section) would run its rows straight under the clock. **`Modifier.amStatusBarEdge()`**, on that screen's scrolling container, fades its content out over the status bar's height instead: whole at the bar's bottom edge, gone at the top of the screen, so whatever lies behind the content (the page, an `AMBackdrop`'s art) shows through under the icons. A screen with a top bar doesn't need it.

```kotlin
LazyColumn(
    Modifier.fillMaxSize().amStatusBarEdge(),
    contentPadding = WindowInsets.systemBars.asPaddingValues(),
) { … }
```

- **An alpha mask on the content, never a band of color.** It fades what the container draws, so art behind the screen shows through. Put it on the container that fills the screen from its top edge and stays put while its content scrolls.
- **It adds no inset and moves nothing.** The container still pads the status bar, so its content rests below the bar and nothing changes until content scrolls up under it.
- **It follows the inset.** `StatusBarEdgeNode` (a `LayoutModifierNode` and `DrawModifierNode`) reads `WindowInsetsRulers.StatusBars.current.top` in its placement block, in its own coordinates, so a change to the inset (an animation, the bar hiding) places it again; the height under the bar is that value clamped into the content's height, and a new height rebuilds the mask and invalidates the draw. With no status bar over the container (the bar hidden, a container that doesn't reach the top of the screen, a host providing no rulers) the height is 0 and it draws the content untouched.
- **Drawing**: the content draws into a `saveLayer` (the content's height, widened by its width at each side for what it draws past them, its cards' shadows), then the mask over the band with `BlendMode.DstIn`: a vertical gradient over the height, alpha 0 over the bar's upper half (`EdgeClear`, 0.5: the icons are centered in the bar), then Material's `FastOutLinearInEasing` from 0 to 1 at the bar's bottom edge over its lower half, sampled at 13 points (`EdgeMaskStops`). Nothing reads behind the icons: a list opened partway down keeps what lies above it tucked under the bar, at most a card's bottom edge showing just above the bar's own.

## Catalog pages

`demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/foundations/`, registered in `FoundationsPages.entries` in this order (ids): `color`, `typography`, `pageHeader`, `sectionHeader`, `spacing`, `radius`, `elevation`, `hairline`, `motion`, `theming`.

| Page | Preview | Loaded |
|---|---|---|
| Color | full-bleed swatch rows: floating surface, page, text (`onSurface`), secondary text, accent (`primary`), selection ring, divider (`outlineVariant`) | a backup card: rows tinted by `AMSemantic` (success, info, warning, destructive) and one disabled row |
| Typography | full-bleed: all fifteen Material roles, a sample over its name and size | an article card: label, headline, title and body, then a list row under a divider in the same card |
| Page header | full-bleed `AMPageHeader("Library")` on a surface strip: a title-only header | `Fill`: the top of a reading list, a title-only header with the list's count trailing, over four rows |
| Section header | full-bleed `AMSectionHeader("Recents")` | full-bleed settings strip: a slot header (glyph, title, count) over two rows, a string header over a switch row; each header sits right on its rows |
| Spacing | full-bleed bars for each step, 32 to 4 dp | a profile card spaced on the scale, then a card of three hand-built rows |
| Radius | floating squares in `shapes.large`, `medium`, `small`, each with its size | a trip card using all three |
| Elevation | floating chips at `Medium`, `Low`, `None` | two album tiles at `Low`, the playing track's card at `Medium` |
| Hairline | a floating surface outlined with `amHairline` | a close button and a restore button outlined, two plan tiles (the chosen one at `selectionRingWidth`) |
| Motion | full-bleed: the six springs of the standard motion scheme, read live, and whether Remove animations is on | a card with tabs over the chosen tab's name (centered, at least 56 dp), a crossfading sign-in row (`amCrossfade`, its line in `titleMedium`) and chips, then a full-width Secondary Replay; a carousel of three stories ("Morning light", "Harbor walk", "Night market": 88 dp cards in a 120 dp band, run edge to edge with the cards at the gutter plus `spacing.lg`) over its dots, and a full-width Replay that pages it; last, in an `AMButtonPair`, Press and hold (Primary) beside a Replay (Secondary) that posts the "Copied to clipboard" toast, led by the `ic_content_copy` glyph |
| Theming | two specimen cards, the second under a nested `AMTheme` (teal scheme, a wider `spacing.sm`) and `MaterialTheme(shapes)` (`shapes.large` at 4 dp, so its corners visibly differ) | a notebook card under the brand theme (teal, `shapes` 4/6/10 dp), its pinned note re-lifted inside with `ProvideAMSurfaceStyle(Elevated)` |

Every row on these pages follows the row recipe (`Layout.md`): hand-built rows carry `Modifier.amRowPadding()`, `ListItem` rows pass `AMRowDefaults`' colors, shapes and content padding, and rows stack with `AMDivider(inset = spacing.lg)` between. Cards are `Modifier.amCard()`.

## See also

- `Layout.md` — the card, the lazy card's slices and the flat strips drawn on this recipe, and the full nesting rule.
- `DataDisplay.md` — the art's tone and inks (`Style.ArtInk`), art text, `amArtBackdrop` and `AMArtRowsBackdrop`.
- `Overlays.md` — the blurred windows that set `LocalAMWindowBlur`, sheets, and the overlay recipe in use.
- `Navigation.md` — the frosted bar and the status bar's icons over art.
