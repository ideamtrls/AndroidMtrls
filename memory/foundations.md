---
scope: Design tokens (locked AMTheme + overridable AMTokens), type scale, elevation/shadows, hairlines, squircle shapes, ripple, reduce-motion, and surface-style nesting — the tokens/ source area
globs:
  - mtrls/src/**/tokens/**
keywords: [AMTheme, AMTokens, AMTokensProvider, AMThemeRoot, AMAdaptiveColor, AMSemantic, AMTextStyle, AMTheme.Type, toTextStyle, LocalAMTokens, LocalAMIsDarkTheme, LocalAMSurfaceStyle, LocalAMReduceMotion, reduce motion, resolved, resolvedOrNull, resolvedFinite, resolvedFiniteOrNull, AMCrossfade, ANIMATOR_DURATION_SCALE, imInstantSwitch, imCrossfade, AMElevation, amElevation, amHairline, amCardHairline, AMSurfaceStyle, AMSurfaceStyleProvider, AMPadding, rowInsets, blockInsets, listRowInsets, titleBarInsets, Surface.primary, Surface.page, Shadow.near, Shadow.far, selectionRing, selectionRingWidth, separator, hairlineWidth, rowMinHeight, type scale, BlurMaskFilter, two-layer shadow, inverted layering, dark mode, draw scope, snapSpring, pressFade, dismissThreshold, amRipple, ripple, RippleAlpha, IndicationNodeFactory, createRippleModifierNode, material-ripple, indication, state layer, amSquircle, Shapes, squircle, continuous corners, RoundedPolygon, CornerRounding, graphics-shapes, AMSquircleShape, plain RoundedCornerShape, filled field shape, amFilledFieldShape]
updated: 2026-07-10
---

# Foundations

Design tokens and the signature inverted-layering aesthetic. Files under
`mtrls/src/main/kotlin/io/github/jmsdnns/androidmtrls/tokens/`: `Theme.kt`,
`Tokens.kt`, `Semantic.kt`, `Type.kt`, `Elevation.kt`, `Hairline.kt`,
`SurfaceStyle.kt`, `Shapes.kt`, `Ripple.kt`, `Motion.kt`. iOS counterpart:
`../iOSMtrls/Sources/Tokens/**`.

## Two-tier token system (the central architecture)

1. **Signature tokens — locked `object AMTheme`** (`Theme.kt`). The aesthetic
   point of the library; NOT themable, never routed through a CompositionLocal.
   Includes `Surface` (inverted white/black), `Text`, `Type` (the type scale),
   `Icon`, `Shadow`, `selectionRing`, `separator`, the press recipe (`press()`,
   `neutralFill()`), `Overlay`, `hairlineWidth`, `rowMinHeight` — plus
   the canonical copies of `Semantic`/`Spacing`/`Radius`/`Input`/`Motion` that
   seed `AMTokens`.
2. **Style tokens — overridable `data class AMTokens`** (`Tokens.kt`), flowing
   through `LocalAMTokens`. Five `@Immutable` sub-data-classes:
   `AMTokensSemantics`, `AMTokensSpacing`, `AMTokensRadius`, `AMTokensInput`,
   `AMTokensMotion`. Every constructor field defaults to its `AMTheme`
   counterpart, so `AMTokens.standard` (= `AMTokens()`) is in lockstep with the
   statics and apps that never override render stock AndroidMtrls.

**How components read tokens:** overridable values via
`val tokens = LocalAMTokens.current` → `tokens.spacing.lg`, `tokens.radius.md`,
`tokens.semantic.info`, `tokens.motion.snapSpring`. Signature values read
`AMTheme` directly: `AMTheme.Surface.primary.resolve(isDark)`,
`AMTheme.hairlineWidth.dp`. A single component mixes both.

**Dimension tokens in `AMTheme` are `Float` constants meaning dp** — apply
`.dp` at call sites (`AMTheme.Spacing.lg.dp`). The `AMTokens` mirrors are
already typed `Dp` (`tokens.spacing.lg` is a `Dp`).

**Rule:** tuned per-interaction constants (fling geometry, shimmer timing,
lift choreography) stay `private` in their component files, never tokens — a
token override must not be able to distort a tuned interaction.

## Type scale (`Type.kt` + `AMTheme.Type`)

Typography is signature-locked — part of the library's voice like `Surface`, so
it lives under `AMTheme` and is absent from `AMTokens`. `Type.kt` declares the
`@Immutable data class AMTextStyle(fontSize, fontWeight, letterSpacing)` (its
`fontSize`/`letterSpacing` are `Float`s meaning **sp**, matching the dp-Float
convention) with a member `fun toTextStyle(color: Color): TextStyle` which
applies `.sp` and the resolved text color (a member, not a bare top-level
extension — the no-bare-public-symbol rule; `AMBadge.Content` precedent).
`AMTheme.Type` (in `Theme.kt`) holds the locked Material-metric ramp:

| Entry | size / weight / tracking (sp) | Role |
|---|---|---|
| `title` | 16 / Medium / 0.15 | row headlines, dialog + banner titles |
| `body` | 16 / Normal / 0.5 | input text, body copy, un-emphasized button labels |
| `secondary` | 14 / Normal / 0.25 | subtitles, picker values, toasts, footers |
| `label` | 14 / Medium / 0.1 | buttons, tabs, chips, segmented labels (Material label-large) |
| `caption` | 12 / Normal / 0.4 | captions, section footers |
| `labelSmall` | 11 / Medium / 0.5 | badges, tile captions, tiny metadata |

Components read a style directly and render it with
`AMTheme.Type.body.toTextStyle(AMTheme.Text.primary.resolve(isDark))`. This
scale is the Material-metric counterpart of the iOS 17/15/13-SemiBold ramp.

## CompositionLocals & providers

| Local | Kind | Default | Provided by |
|---|---|---|---|
| `LocalAMTokens` | `staticCompositionLocalOf` | `AMTokens.standard` | `AMThemeRoot`, `AMTokensProvider` |
| `LocalAMIsDarkTheme` | `compositionLocalOf` (dynamic) | `false` | `AMThemeRoot` |
| `LocalAMReduceMotion` | `staticCompositionLocalOf` | `false` | `AMThemeRoot` |
| `LocalAMSurfaceStyle` | `staticCompositionLocalOf` | `AMSurfaceStyle.Elevated` | `AMSurfaceStyleProvider`, `AMCard` |

- **`AMThemeRoot(content)`** (`Theme.kt`) is the required root composable for
  every AndroidMtrls tree (`setContent { AMThemeRoot { … } }`). Installs
  `LocalAMIsDarkTheme = isSystemInDarkTheme()`,
  `LocalAMTokens = AMTokens.standard`, AND
  `LocalAMReduceMotion` (snapshot read — see the Motion section). **Gotcha:** because it hard-installs
  `standard`, never nest `AMThemeRoot` inside an `AMTokensProvider` subtree —
  it clobbers the override. Root goes outermost, providers inside.
- **`AMTokensProvider`** (`Tokens.kt`) has two overloads resolved by parameter
  type: `(tokens: AMTokens, content)` installs a whole set;
  `(transform: (AMTokens) -> AMTokens, content)` reads the inherited set and
  installs `transform(inherited)` — the Compose form of iOS
  `.imTokens(_ transform:)`, implemented as a `.copy(...)` chain since
  `AMTokens` is immutable. **Both overloads provide only `LocalAMTokens`** — a
  nested per-subtree token override that layers *inside* `AMThemeRoot` (the
  owner of `LocalAMIsDarkTheme`). Dark-theme state flows through untouched, so a
  manual light/dark override placed *above* an `AMTokensProvider` (e.g. the
  demo's theme toggle) reaches the provider's content unchanged.

## AMAdaptiveColor — the draw-scope bridge

`AMAdaptiveColor(light: Color, dark: Color)` (`Theme.kt`, `@Immutable` data
class) is how locked color tokens reach non-composable draw lambdas:

- `resolve(isDark: Boolean): Color` — pure function; capture
  `LocalAMIsDarkTheme.current` once at the composable boundary, pass the flag
  into `drawBehind {}` / `Canvas`.
- `@Composable resolve(): Color` — convenience that reads
  `LocalAMIsDarkTheme.current` itself (NOT `isSystemInDarkTheme()`, so manual
  overrides are respected).

`AMTheme.press()` and `neutralFill()` are `@Composable fun`s (not
`AMAdaptiveColor`) returning `Black/White × 0.08` per `LocalAMIsDarkTheme`.
`press()` is declared but no component applies it — press feedback is the
`amRipple()` state layer (see the Ripple section). `neutralFill()` (same
recipe, delegates to `press()`) is the resting fill for unselected chips/tags.

## Ripple — `amRipple` (`Ripple.kt`)

`@Composable fun amRipple(bounded: Boolean = true, radius: Dp = Dp.Unspecified):
IndicationNodeFactory` is the house press idiom — a monochrome state layer wired
as the `indication` of a `clickable`/`toggleable`/`selectable` alongside a
hoisted `MutableInteractionSource`. It wraps the low-level
`androidx.compose.material.ripple.createRippleModifierNode` (the platform ripple
node, NOT Material3 — `material-ripple` is the primitive every design system
builds on). The layer is pure monochrome: `Color.Black` on light, `Color.White`
on dark, keyed off `LocalAMIsDarkTheme.current` (so the demo's manual toggle is
honored — never `isSystemInDarkTheme()`). Alphas are the file-level
`AMRippleAlpha = RippleAlpha(pressed 0.12, focused 0.12, dragged 0.16, hovered
0.08)` — the 0.12-family opacity is tuned to match the press-overlay recipe, so
the ripple reads consistently on the inverted white/black surfaces.

- Returns a `@Stable private class AMRippleNodeFactory` with value
  `equals`/`hashCode` over `(bounded, radius, color)`, `remember`ed on that
  triple so recomposition reuses one indication.
- **Bounded ripples clip to a rectangle only** — the caller must
  `Modifier.clip(shape)` the target so the ripple respects the AM corner radius.
  `bounded = false` gives an unbounded circular ripple sized by `radius`, for
  icon-sized targets.
- The signature-locked recipe lives in `AMTheme`-adjacent token space (no
  `AMTokens` mirror); it is not overridable.

## Motion & Reduce Motion (`Motion.kt`)

`Motion.kt` adds the *behavior* layer on top of the `AMTheme.Motion` /
`AMTokensMotion` *values*: the single Reduce-Motion collapse policy and the one
app-facing keyed crossfade. **Every new animating primitive must route its spec
through the resolvers here** so the whole system softens/stills by one policy
instead of each site re-deciding its fallback.

- **`LocalAMReduceMotion`** — `staticCompositionLocalOf { false }`. Installed by
  `AMThemeRoot` from a **snapshot read** of the Android-native "remove
  animations" toggle: `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`, read once
  via `remember {}` (no live listener — toggling the OS setting mid-run doesn't
  re-derive until the composition is recreated; matches the platform contract
  and stays off the recomposition hot path). Default `false` so unhosted
  previews/tests render full motion. Override for a subtree with
  `CompositionLocalProvider(LocalAMReduceMotion provides true) { … }`. Components
  don't branch on it directly — they pass their full spec to the resolvers with
  `reduceMotion = LocalAMReduceMotion.current`.
- **The collapse resolvers** — extension funs on `AMTokensMotion`, generic over
  `T` so `spring<Dp>`/`spring<Float>`/`spring<IntOffset>` all route through one
  policy:
  - `fun <T> AMTokensMotion.resolved(full: AnimationSpec<T>, reduceMotion): AnimationSpec<T>`
    — returns the exact `full` instance when off; under Reduce Motion softens to
    a fresh `tween(180, EaseInOut)` that **mirrors `AMTheme.Motion.overlayFade`**
    (the `<Float>` token can't assign into a generic `<T>`, so the params are
    reproduced with a lockstep comment — retune both together).
  - `fun <T> AMTokensMotion.resolvedOrNull(full, reduceMotion): AnimationSpec<T>?`
    — same passthrough when off, but returns `null` when on for sites that
    animate to *nothing* rather than softening (underline slide, FAB lift).
    Callers treat `null` as "don't animate" (e.g. `spec ?: snap()`).
  - `resolvedFinite` / `resolvedFiniteOrNull` — `FiniteAnimationSpec`-typed twins
    of the two above (finite *by construction*, no cast; same `EaseInOut`
    mirror / `null`), for `AnimatedVisibility`'s `fadeIn`/`slideIn*`/`expand*`
    builders and `Crossfade`, which demand a `FiniteAnimationSpec` the generic
    resolvers can't promise.
- **`@Composable fun <T> AMCrossfade(targetState, modifier, content)`** — the
  app-facing keyed crossfade (Compose translation of iOS `imCrossfade(value:)`),
  for root/phase swaps that legitimately want motion. Rides the shared
  `AMTheme.Motion.overlayFade` token (180ms `EaseInOut`) via
  `tokens.motion.resolvedFiniteOrNull(AMTheme.Motion.overlayFade, reduceMotion) ?:
  snap()` — same fade iOS's `imCrossfade` rides, so retuning `overlayFade` moves
  the crossfade; under `LocalAMReduceMotion` the swap is **instant** (`snap()`, no
  fade). Prefer over a hand-rolled `AnimatedContent`.
- **iOS mapping:** `imInstantSwitch(_:)` is deliberately NOT ported — SwiftUI
  needs `withTransaction(\.disablesAnimations)` to suppress implicit animations,
  but Compose has none, so `imInstantSwitch` ≙ a plain `state = …` state write.

## Inverted layering — exact values

Floating surfaces sit on top of brighter content; shadows cast onto the page
substrate, which is tuned (not the cards):

- `AMTheme.Surface.primary` = pure `Color.White` light / pure `Color.Black`
  dark — the pill/card/circle fill.
- `AMTheme.Surface.page` = `0xFFF2F2F7` light (iOS secondarySystemBackground)
  / `0xFF0C0C0D` dark — a custom near-black deliberately darker than
  Material's ~RGB(18,18,18) so pure-black cards still register via shadow.

## Elevation — `AMElevation` + `Modifier.amElevation` (`Elevation.kt`)

Two stacked blurred shadows per level: far layer drawn first (under), near on
top. dp table (verbatim):

| Level | near (r / y, color) | far (r / y, color) | Use |
|---|---|---|---|
| `None` | 0 / 0, transparent | 0 / 0, transparent | tile grids, card bands — keeps fill/shape, casts nothing |
| `Low` | 4 / 2, `Shadow.near` | 16 / 10, `Shadow.far` | default: row cards, grid buttons, text fields |
| `Medium` | 6 / 4, `Shadow.near` | 28 / 18, `Shadow.far` | sheets, prominent overlays |
| `Text` | 3 / 1, `Shadow.textNear` | 10 / 4, `Shadow.textFar` | spec-table values for the text-legibility recipe — see warning below |

Shadow colors (`AMTheme.Shadow`): `near` = black 0.10 light / **0.55 dark**;
`far` = 0.08 / **0.45** (dark values must register on the RGB 12,12,13 page).
`textNear` = fixed 0.12, `textFar` = fixed 0.10 — plain `Color`, NOT adaptive.
Only the elevation modifier reads `AMTheme.Shadow` directly.

**Do NOT apply `amElevation(AMElevation.Text)` to text labels.** The modifier
draws the SHAPE silhouette (a blurred rounded rect of the composable's
bounds) — correct for opaque cards, but behind a transparent label it renders
as a gray band. iOS `.imElevation(.text)` shadows the glyph alpha mask; the
Compose equivalent is the glyph-level `AMTextLegibilityLabel`
(`content/SectionHeader.kt`, internal) used by `AMSectionHeader`/
`AMPageHeader` — it draws the text twice via `TextStyle.shadow` with the
Text-level near/far values. The `Text` enum case stays for the recipe table
and the Foundations demo's elevation-chip specimen (an opaque rect, where the
silhouette draw is fine).

`Modifier.amElevation(level = AMElevation.Low, shape = RoundedCornerShape(AMTheme.Radius.md.dp))`:

- **Default shape is `Radius.md` (12dp) — cards must pass
  `RoundedCornerShape(AMTheme.Radius.lg.dp)` explicitly.**
- Implementation is `Modifier.composed {}` capturing `LocalAMIsDarkTheme` via
  `rememberUpdatedState`, then `drawBehind` + `drawIntoCanvas` with an Android
  `BlurMaskFilter(radiusPx, NORMAL)` on `paint.asFrameworkPaint()` (the
  deprecation is suppressed — no stable alternative exists). The ARGB color
  must be set on the **framework** paint; the Compose `Paint` color is ignored
  when a maskFilter is active. NOT `Modifier.shadow` — Android elevation
  semantics don't map to the `(radius, y)` recipe and `Modifier.shadow` takes
  no custom colors.
- `None` still runs the draw pass (layers are skipped per-layer when
  `radiusPx == 0`) so view identity stays stable across animated elevation
  transitions — no composition branch.
- Shadow offset = `canvas.translate(0, yOffsetPx)`; `Outline.Rounded` draws
  with the **top-left** corner radius only — shapes with non-uniform corners
  get an approximated shadow silhouette (hairline handles non-uniform corners
  correctly via Path; elevation does not).
- Order matters at call sites: `.amElevation(...)` BEFORE `.background(...)`
  so the blur bleeds onto the page behind the fill.

**Full elevated treatment** = fill + `amElevation` + dark-mode-only hairline
(`amCardHairline`) — applied by container components (Card, Sheet, Toolbar,
buttons), not by this modifier. `AMPanel` is NOT in this set: it is a
full-screen `Surface.page`-backed surface, not a floating card, so it casts no
shadow and draws no card hairline.

## Hairlines (`Hairline.kt`)

- `AMTheme.hairlineWidth = 0.5f` (dp); `AMTheme.separator` = RGBA(60,60,67,
  0.29) light / RGBA(84,84,88, 0.65) dark (iOS `UIColor.separator`).
- `Modifier.amHairline(shape, color = Color.Unspecified, width = Dp.Unspecified)`
  — strokes **inside** the bounds (SwiftUI `strokeBorder` semantics: outline
  built at `size - strokeWidth`, canvas translated by `strokeWidth/2`; never
  grows the surface). `Color.Unspecified` resolves to
  `AMTheme.selectionRing.resolve(LocalAMIsDarkTheme)`; `Dp.Unspecified`
  resolves to `max(0.5f, 1f / density).dp` — 0.5dp floored at 1 physical px
  (1dp at density 1, 0.5dp at density ≥ 2). Pass
  `color = AMTheme.separator.resolve(isDark)` for divider-toned outlines.
  Drawn via `drawWithContent` (content first, stroke on top); non-uniform
  rounded corners go through a `Path`, uniform through `drawRoundRect`.
  Uses `Modifier.composed`. **Unconditional** — for sheets, banners, FAB,
  alerts, toasts, avatars, chip groups, the segmented control's outline +
  dividers, and the search field's pill (drawn `separator`-toned in both
  themes).
- `Modifier.amCardHairline(shape)` — applies `amHairline(shape)` **only in
  dark mode**, no-op in light. Light mode: shadows define the edge; dark mode:
  a pure-black card on the RGB(12,12,13) page needs the ring. Consumers:
  Card, Buttons, Toolbar, Sheet, Alert, `AMStepper`'s square ± buttons,
  the filled text fields (`AMFilledField` default), MenuPicker, Carousel, Tabs.
- `internal @Composable amHairlineWidth(): Dp` — the same width formula as a
  helper for in-module callers that draw their own lines (Divider, Tabs).

## Squircle shapes (`Shapes.kt`)

`fun amSquircle(radius: Dp, smoothing: Float = 0.6f): Shape` — the
continuous-corner rounded rectangle, the Compose counterpart of iOS
`RoundedRectangle(cornerRadius:, style: .continuous)`. Built on
`androidx.graphics:graphics-shapes` (`RoundedPolygon.rectangle(...)` +
`CornerRounding(radiusPx, smoothing)` → `toPath().asComposePath()` →
`Outline.Generic`). `smoothing 0.6` approximates Apple's `.continuous`; `0f` is a
plain circular-arc corner.

- Builds the polygon **in pixel space at the actual `createOutline` size**
  (centered at `size/2`, spanning `[0,w]×[0,h]`), so corners stay uniform at any
  aspect ratio — no distortion from stretching a normalized unit square. Radius
  is clamped to `min(w,h)/2`.
- Per-`(size, density)` **path memo** inside the `AMSquircleShape` instance; a
  fresh `Path` per rebuild so a returned `Outline` never sees its path mutated.
  `equals`/`hashCode` over `(radius, smoothing)` make it a stable Compose param,
  so passing `amSquircle(r)` inline still lets `Modifier.clip` skip.
- Works as clip/background/border shape and casts a matching squircle shadow via
  `amElevation` and a matching hairline via `amHairline` — both draw
  `shape.createOutline(...)`'s `Outline.Generic` path via `canvas.drawPath`, so
  the squircle silhouette is exact (no top-left-radius approximation, which only
  affects the `Outline.Rounded` branch used by `CircleShape`/capsule callers).
- `internal fun amSquircleRectanglePolygon(width, height, radiusPx, smoothing)`
  is extracted so the size/aspect-ratio/clamp behavior is pure-JVM testable
  (`RoundedPolygon` + `calculateBounds()` are framework-free; only `toPath()`
  needs Android — so `createOutline` itself is Robolectric-only).

**Per-corner overload:** `fun amSquircle(topStart, topEnd, bottomEnd,
bottomStart: Dp, smoothing = 0.6f): Shape` — the counterpart of iOS
`UnevenRoundedRectangle(..., style: .continuous)`, for surfaces that round only
some corners (`AMSheet`'s top-only shape). Backed by `RoundedPolygon.rectangle`'s
`perVertexRounding` (a size-4 `List<CornerRounding>`). Radii are named
start/end/clockwise-from-top like `RoundedCornerShape`'s per-corner constructor
and are **layout-direction aware** — `topStart`/`bottomStart` resolve to the left
edge in LTR, the right in RTL, so the `AMSquirclePerCornerShape` memo also keys on
`layoutDirection`. `0.dp` yields a sharp corner; each radius clamps to `min(w,h)/2`.
The pure-JVM `internal fun amSquircleRectanglePolygonPerCorner(width, height,
topLeftPx, topRightPx, bottomRightPx, bottomLeftPx, smoothing)` takes **already
resolved visual** (left/right) radii — the start/end→left/right resolution lives
in the `Shape`, keeping the polygon helper framework-free (vertex order
`[bottomRight, bottomLeft, topLeft, topRight]`, visual top at `y=0`).

**Wired everywhere:** every rounded-rect `RoundedCornerShape(...)` in `:mtrls`
(cards, buttons, pickers, overlays, sheets, tabs, carousels, and the
`amElevation` default shape, `amSquircle(Radius.md)`) uses `amSquircle`.
Capsules/circles stay `RoundedCornerShape(50)`/`RoundedCornerShape(percent = 50)`/
`CircleShape` (already round; a squircle would not match a capsule track) — the
segmented-control track, search field (`amSearchFieldShape`), badge count/dot,
toast, page-indicator, and chip shapes are all-capsule.
**Deliberate plain circular-arc `RoundedCornerShape` (NOT `amSquircle`)** at the
small-radius input/control chrome: the filled text fields — `amFilledFieldShape`
(4dp top / square bottom) and `amFilledEditorShape` (4dp all corners) — and the
square `AMStepper` ± buttons (`RoundedCornerShape(tokens.radius.sm)`); at these
tight radii the continuous-corner curvature wouldn't read, so they skip the
squircle. The `AMSlider` track/handle and `AMToggleRow` switch are Canvas-drawn
round rects (no `Shape` at all).

## Semantic axis — `AMSemantic` (`Semantic.kt`)

The one canonical role→color + role→glyph axis (the iOS twin is
`IMSemantic`): `@Immutable enum class AMSemantic { Info, Success, Warning,
Destructive }`. Two members:

- `fun color(semantics: AMTokensSemantics, isDark: Boolean): Color` — resolves the
  role's tint against a **supplied** token set (pass `LocalAMTokens.current.semantic`),
  so `AMTokensProvider` overrides flow through, picking the light/dark hue via
  `isDark` (each `AMTokensSemantics` field is an `AMAdaptiveColor` pair). Pure and
  draw-scope safe — capture `LocalAMIsDarkTheme.current` at the composable boundary,
  exactly as `accent` is consumed.
- `val icon: ImageVector` — the filled `material-icons-core` glyph: Info→`Info`,
  Success→`CheckCircle`, Warning→`Warning`, Destructive→`Close` (the substitute
  for the absent `xmark.octagon`).

**Consumers delegate here instead of re-deciding:** `AMBannerStyle` is a
`typealias AMBannerStyle = AMSemantic` (surfaces/overlays/Banner.kt) — a banner
style *is* a semantic role, and the banner reads `item.style.icon` /
`item.style.color(tokens.semantic)`. `AMTagTint` (content/Tag.kt) routes its four
charged cases' base color through `AMSemantic.color(...)` while keeping its
`NEUTRAL` case and per-hue fill-opacity table local.

## Selection ring — `AMTheme.selectionRing` / `selectionRingWidth`

Asymmetric adaptive by design: light = `0xFFC7C7CC` (iOS systemGray3, soft on
white); dark = `0xFF2C2C2E` (iOS systemGray5 — defines against pure-black
`Surface.primary` without glowing). Also the default `amHairline` color.

`AMTheme.selectionRingWidth` = `hairlineWidth * 3` = **1.5f** (dp) — the active
ring's stroke width (a locked `const val`, `Float` meaning dp). Consumer: the
ActionGridButton active ring. iOS: `IMTheme.selectionRingWidth`.

## Surface style nesting (`SurfaceStyle.kt`)

`AMSurfaceStyle` (`@Immutable` enum): `Elevated` (rounded fill + `Low` shadow
+ dark-mode hairline, the root-of-screen default) / `Flat` (no fill, shape,
shadow, or hairline — nested mode). Propagates via `LocalAMSurfaceStyle`
(default `Elevated`); override per-subtree with
`AMSurfaceStyleProvider(style) { … }`.

**Who reads it:** only container surfaces — the sole in-library reader is
`AMCard` (`surfaces/frames/Card.kt`). `AMCard` provides `Flat` to its content
slot, so nested cards demote themselves automatically (no shadow-on-shadow, no
double padding); `AMDisclosure` participates in the contract by wrapping an
`AMCard` rather than reading the local itself. Widgets (pickers, list rows) do NOT
read it — they are flat primitives whose container supplies the surface; the
filled text fields draw their own opaque `Surface.primary` container but
likewise never read the local. This is the canonical example of the
composable-vs-Modifier rule in conventions.md.

## Padding presets — two parallel families

1. **`AMPadding`** (`SurfaceStyle.kt`) — static `PaddingValues` locked to
   `AMTheme`; not overridable.
2. **`AMTokens` extension properties** (`Tokens.kt`) — same recipes computed
   from the live token set, so spacing overrides propagate. Canonical names
   are the `*Insets` forms. Components inside cards use
   `tokens.rowInsets`/`blockInsets`; components that ARE their own surface use
   `tokens.listRowInsets`.

| Static | Token-derived | Horizontal | Vertical | Use |
|---|---|---|---|---|
| `AMPadding.row` | `AMTokens.rowInsets` | `spacing.lg` 16dp | `spacing.md` 12dp | row content inside a card |
| `AMPadding.block` | `AMTokens.blockInsets` | `spacing.lg` 16dp | 16dp | uniform prose/block cards |
| `AMPadding.listRow` | `AMTokens.listRowInsets` | `input.inset` 14dp | `spacing.lg` 16dp | full-bleed rows that are their own surface |
| `AMPadding.titleBar` | `AMTokens.titleBarInsets` | `input.inset` 14dp | **10dp literal** | Panel/Toolbar title strips |

The titleBar vertical 10dp is a hardcoded literal, not a spacing token;
`listRow` horizontal matches `Input.inset` so stacked inputs and
list rows align at their leading edges.

## Token group structure (under `AMTheme`)

Nested `object`s for multi-variant roles; flat `val`s for one-offs.

- `Surface` { primary, page } — see inverted layering above.
- `Text` { primary = Black/White; secondary = RGBA(60,60,67,0.60) light /
  RGBA(235,235,245,0.60) dark } — `AMAdaptiveColor` pairs (NOT Material
  colorScheme; manager ruling — no Material3 in `:mtrls`).
- `Type` { title, body, secondary, label, caption, labelSmall } — locked
  `AMTextStyle` entries; see the type-scale section above.
- `Icon` { muted = same values as `Text.secondary`; disabled = same RGB at
  alpha 0.30 (secondaryLabel × 0.5) }.
- `Shadow` { near, far adaptive; textNear 0.12, textFar 0.10 fixed `Color` }.
- `Semantic` { info, success, warning, destructive } — the iOS system palette
  (SwiftUI `.blue/.green/.yellow/.red`) as **`AMAdaptiveColor` light/dark pairs**:
  info `007AFF`/`0A84FF`, success `34C759`/`30D158`, warning `FFCC00`/`FFD60A`,
  destructive `FF3B30`/`FF453A` — brighter in dark mode to stay legible on the
  inverted surfaces. Every consumer resolves with `.resolve(isDark)` (or
  `AMSemantic.color(semantics, isDark)`); `:mtrls` consumers (buttons, toolbar,
  tags, banner, badges, swipe actions) all thread `isDark` from
  `LocalAMIsDarkTheme.current`. success/warning/destructive also exist as top-level
  `AMTheme.success/warning/destructive` `AMAdaptiveColor`s (the `Semantic` members
  delegate to them); `info` exists ONLY inside `Semantic`. These seed
  `AMTokensSemantics` (all four charged fields are `AMAdaptiveColor`); status primitives
  (Banner, Tag) resolve their role through the `AMSemantic` axis against
  `LocalAMTokens.current.semantic`, so overrides re-tint them.
- `accent` — flat top-level `AMAdaptiveColor(light = 0xFF007AFF, dark =
  0xFF0A84FF)`, the iOS system blue (brighter in dark mode). The
  primary-affordance tint, consumed by the progress-bar fill, action-grid active
  tile, and calendar "today" marker — all via `.resolve(isDark)`. Overridable
  per-app/subtree via `AMTokensSemantics.accent` (the demo's dynamic-accent
  toggle drives it from the Material You palette; an `AMAdaptiveColor` light/dark
  pair like the other four `AMTokensSemantics` fields — its consumers resolve it
  via `.resolve(isDark)`).
- `Overlay` { blurRadius 20f (dp), scrimOpacity 0.40f } — shared modal
  backdrop recipe.
- `Spacing` (4pt grid, `Float` dp) { xs 4, sm 8, md 12, lg 16, xl 24, xxl 32 }.
- `Radius` { sm 8 chips/badges, md 12 inputs/toggles, lg 16 cards/panels }.
- `Input` { inset 14, radius 14 } — off-grid by design (between md and lg),
  pill-ish input silhouette.
- `Motion` — `AnimationSpec<Float>` springs converted from iOS
  `spring(response:dampingFraction:)` via `stiffness = (2π/response)²`:
  `snapSpring` (0.86, 384), `presentSpring` (0.85, 321), `modalSpring`
  (0.86, 224); tweens `overlayFade` (180ms **EaseInOut**), `pressFade`
  (120ms **LinearOutSlowIn** — iOS `.easeOut` is deceleration; see
  conventions.md), `selectionFade` (150ms **EaseInOut**); `dismissThreshold`
  60f dp, `dismissThresholdCompact` 30f dp (edge-anchored banner/toast).
  `overlayFade`/`selectionFade` use `EaseInOut` (the symmetric CSS ease-in-out
  cubic — the same curve SwiftUI's `.easeInOut` uses; RULING 2); only `pressFade`
  keeps the Material `LinearOutSlowInEasing`. **Lockstep gotcha:** the Dp
  `overlayFadeDp` mirrors in `surfaces/overlays/` (`ActionsOverlay`,
  `LoadingOverlay`) are `tween<Dp>(180, EaseInOut)`, in lockstep with this
  token — retune both mirrors together if the overlay easing changes.
  Specs are `<Float>`-typed; `Dp`/`IntOffset` call sites reconstruct a typed
  twin (e.g. `spring<Dp>(dampingRatio = 0.86f, stiffness = 384f)`) with a
  lockstep comment — see conventions.md.
- Flat one-offs: `selectionRing`, `selectionRingWidth` 1.5f (= `hairlineWidth`
  × 3; active-ring stroke), `separator`, `hairlineWidth` 0.5f, `rowMinHeight`
  64f (min height pinning single-row card widgets to list-row rhythm),
  `press()`, `neutralFill()`.

`Semantic` (plus `accent`)/`Spacing`/`Radius`/`Input`/`Motion` are mirrored in
`AMTokens` — `accent` rides inside `AMTokensSemantics` alongside the four
`Semantic` tints. Everything else is signature-locked and absent from `AMTokens`.

## Tests

`mtrls/src/test/kotlin/.../tokens/` — pure JVM, no Compose runtime:

- `ThemeTest.kt` — `AMAdaptiveColor.resolve(isDark)` semantics and every
  locked constant's exact value (incl. `selectionRingWidth == 1.5f ==
  hairlineWidth * 3`). Color alpha assertions use a `1f/255f` delta (Compose
  quantizes to 8-bit ARGB).
- `SemanticTest.kt` — each `AMSemantic` case's `color(...)` against
  `AMTokens.standard` (and an overridden set) plus each case's `icon` identity.
- `TokensTest.kt` — `AMTokens.standard` lockstep with `AMTheme`, data-class
  copy/transform semantics (the `AMTokensProvider(transform)` contract), and
  the four `*Insets` extensions via `calculateLeftPadding(LayoutDirection.Ltr)`
  etc.
- `MotionTest.kt` — the Reduce-Motion resolver pair (`resolved`/`resolvedOrNull`):
  passthrough (`assertSame` the exact instance) when off, the `tween(180,
  EaseInOut)` `overlayFade` mirror / `null` when on, and the generic-over-`T`
  routing (asserted with an `AnimationSpec<Dp>`).
- `ShapesTest.kt` — `amSquircle` value semantics (equal `(radius, smoothing)`
  compare equal; default smoothing 0.6; differs by radius) and the pure-JVM
  `amSquircleRectanglePolygon` geometry via `RoundedPolygon.calculateBounds()`
  (the `createOutline` → `toPath()` path is Robolectric-only, so it is not
  exercised).

## See also

- `conventions.md` — AM/am prefix, composable-vs-Modifier rule, press
  feedback, easing translation table, Dp-mirror spring pattern.
- `build.md` — no-Material3 dependency policy, `BasicText` rule, test commands.
