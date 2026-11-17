---
scope: Static layout frames — Modifier.amCard(), the row recipe (Modifier.amRowPadding() for hand-built rows, AMRowDefaults for Material ListItem rows), AMSection and its lazy form (LazyListScope.amSection, a card drawn a row at a time), AMDisclosureGroup, AMDivider, the flat strip self-filled blocks wear (amFlatSurface), and how a card demotes inside another card (AMSurfaceStyle)
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/layout/**, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/LayoutCardSliceTest.kt, androidmtrls/src/main/res/drawable/am_ic_expand_more.xml, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/layout/**]
keywords: [Modifier.amCard, card, CardSurfaceNode, CardPaddingNode, measuredStyle, draw inside own layout, shapes.large, padding, clip, the app's setting, surfaceFill, LocalAMSurfaceFill, lookOf, Solid, Frosted, Tinted, the tint, AMBackdrop, ProvideAMOnSurface, elevation, AMElevation.None, nested cards, AMSurfaceStyle, Elevated, Flat, ProvideAMSurfaceStyle, re-lift, NestingSurfaceNode, TraversableNode, findNearestAncestor, resolveSurfaceStyle, SurfaceStyleOrigin, ElevatedSurfaceDrawer, row recipe, rowVerticalPadding, 80 dp, 100 dp, Modifier.amRowPadding, AMRowDefaults, contentPadding, colors, shapes, ListItem, ListItemDefaults, minimum height, vertical alignment, selectedContainerColor, flat strip, amFlatSurface, FlatSurfaceNode, AMSection, header-less AMSection, SectionLayout, SectionSurface, AMSectionStyle, Rows, Card, header, footer, art text, amSection, LazyListScope, LazyColumn, lazy section, long list, amCardSlice, CardSliceNode, CardSlice, cardSlice, cardSliceReach, drawSurfaceSlice, band, LayoutCardSliceTest, inset, AMDisclosureGroup, folding, expand_more, expand, collapse, AnimatedVisibility, motionScheme, AMDivider, HorizontalDivider, inset divider, CatalogFrostedStage, CardPage, SectionPage, DisclosurePage, DividerPage, LayoutPages]
updated: 2026-11-17
---

# Layout

The static frames a screen lives inside: `Modifier.amCard()` (the floating card, in the app's surface setting), the row recipe (`Modifier.amRowPadding()` for hand-built rows, `AMRowDefaults` for rows built on Material's `ListItem`), `AMSection` (a header, a card or flat block, a footer) and its lazy form `amSection` (a card of rows a `LazyColumn` draws a row at a time), `AMDisclosureGroup` (a card that folds open in place) and `AMDivider` (Material's divider with an inset). Cards nested in cards demote themselves to flat (`AMSurfaceStyle`). None of them takes a fill: every card, section, disclosure group and flat block draws in the app's setting (`AMTheme.surfaceFill`, `Foundations.md`), solid in a solid app and frosted (or the tint, where embedded) in a frosted one.

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `layout/Card.kt` | `Modifier.amCard` (private `CardSurfaceNode`, `CardPaddingNode` and their elements), `Modifier.amRowPadding` (private `RowPaddingNode`), `AMRowDefaults`, internal `Modifier.amFlatSurface` (private `FlatSurfaceNode`), internal `Modifier.amCardSlice` (private `CardSliceNode`), internal `CardSlice`, `cardSlice`, `cardSliceReach` |
| `layout/Section.kt` | `AMSectionStyle`, `AMSection` (string, slot and header-less forms), `LazyListScope.amSection` (string and slot forms), private `SectionLayout`, `SectionSurface`, `SectionFooter`, `lazySection` |
| `layout/Disclosure.kt` | `AMDisclosureGroup` (string and slot forms) |
| `layout/Divider.kt` | `AMDivider` |

Pieces in `foundations/` the area builds on (`Foundations.md`): `AMTheme.rowVerticalPadding` (`Theme.kt`, from `Style.Row` in `Style.kt`); `AMSurfaceStyle`, `LocalAMSurfaceStyle`, `ProvideAMSurfaceStyle` and the internal nesting resolver (`NestingSurfaceNode`, `resolveSurfaceStyle`, `SurfaceStyleOrigin`) in `SurfaceStyle.kt`; the internal `ElevatedSurfaceDrawer` in `Hairline.kt`; the app's setting (`AMTheme.surfaceFill` in `Theme.kt`; internal `LocalAMSurfaceFill` and `ProvideAMOnSurface` in `SurfaceFill.kt`), internal `lookOf` and `backdropFor` (`SurfaceLook.kt`) and `AMBackdrop` (`Backdrop.kt`); `AMSectionHeader` (`SectionHeader.kt`); `resolveEnabled` (`Enabled.kt`). From `datadisplay/` (`DataDisplay.md`): internal `LocalAMArtTone` and `artInk` (`ArtTone.kt`) and `Modifier.amArtText` (`ArtBackdrop.kt`), for a section's header and footer on art.

- Glyph: `androidmtrls/src/main/res/drawable/am_ic_expand_more.xml` (the disclosure chevron).
- Catalog pages: `demo/.../catalog/pages/layout/` (below).
- Tests: `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/LayoutCardSliceTest.kt` (the slice math).

## The row recipe

**A row is its content plus `AMTheme.rowVerticalPadding` above and below and `spacing.lg` at the sides, with no fixed or minimum height.** One line of `bodyLarge` makes an 80 dp row; a `bodyLarge` title over a `bodyMedium` supporting line makes a 100 dp row; a switch, a picture or a larger font scale makes it taller by its own height. Rows run taller than Material's list item (56/72/88) on purpose: about 1.4 times the platform's row, so a list reads airy and each row is an easy target.

- **The values.** The vertical padding is one read-only constant in the style tier (`Style.Row.VerticalPadding`, read as `AMTheme.rowVerticalPadding`); its value and its tuning belong to `Foundations.md`. The sides are the spacing scale's `lg`, so an app that re-tunes `spacing.lg` moves the sides but not the vertical padding.
- **Every row uses it**: rows in a card and rows on a flat strip alike, hand-built or built on `ListItem`, the disclosure card's header included.
- **Hand-built rows: `Modifier.amRowPadding()`.** It fills the width (`fillMaxWidth`) and pads the content by `spacing.lg` at the sides (read from `LocalAMTokens` in the node) and the style tier's vertical padding above and below. It draws no surface. Inside a nested (flat) card it still applies: it is the row's own inset, not the card's.
  ```kotlin
  Column(Modifier.fillMaxWidth().amCard()) {
      Column(Modifier.amRowPadding()) {
          Text("Weekly report", style = MaterialTheme.typography.bodyLarge)
          Text("Sent every Monday at 9:00", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
  }
  ```
- **Rows on Material's `ListItem`** (kept for its semantics, ripple, slots and its click, toggle and selection forms) pass all three of `AMRowDefaults`:
  ```kotlin
  ListItem(
      checked = sendsReport,
      onCheckedChange = { sendsReport = it },
      trailingContent = { Switch(checked = sendsReport, onCheckedChange = null) },
      colors = AMRowDefaults.colors(),
      shapes = AMRowDefaults.shapes(),
      contentPadding = AMRowDefaults.contentPadding,
  ) { Text("Send weekly report") }
  ```
  - `contentPadding` = `PaddingValues(horizontal = spacing.lg, vertical = rowVerticalPadding)`. Material's list item takes its content padding out of its own minimum height: it applies the padding around its layout, then gives the content a minimum of (56, 72 or 88 dp by line count) minus the top and bottom padding, floored at 0. With the recipe's padding that minimum always falls below real content, so nothing stacks and the row is exactly content plus the padding. Material aligns the leading content, the text and the trailing content alike: centered while the content area (inside the padding) is under 60 dp tall, top-aligned from there (its breakpoint: the 80 dp between its two- and three-line heights, less its own 10 dp default padding above and below). So a one- or two-line row centers its switch; three lines, or two at a large font scale, top-align it.
  - `colors()` = Material's list item colors on a transparent container at rest, disabled and selected (only a dragged row keeps Material's container), and a checked or selected row keeps its resting content colors. **A row never tints itself when checked or selected**: it stays flat on its card or strip, and its control (a switch, a radio button) shows the state in Material's own look. The supporting line takes `AMTheme.secondaryText` (`ListItemDefaults.colors(supportingContentColor = …)`), the frosted secondary on frost (`Foundations.md`).
  - `shapes()` = Material's list item shapes made square at rest, pressed, focused, hovered and selected (dragged keeps Material's), so a row never rounds into a pill; a card's clip rounds the rows at its corners, ripple included.
  - The recipe fits the `ListItem` overloads that take `contentPadding`, `colors` and `shapes` (the plain, `onClick`, `selected` and `checked` forms, none experimental). The `headlineContent` overload (with `tonalElevation`) has fixed metrics and takes neither `shapes` nor `contentPadding`, so it's never used.
- **Between rows**: `AMDivider(inset = AMTheme.tokens.spacing.lg)`, so the line starts where the rows' text does.

## `Modifier.amCard()` — `Card.kt`

```kotlin
@Composable @ReadOnlyComposable
public fun Modifier.amCard(
    elevation: AMElevation = AMElevation.Low,
    padding: PaddingValues = PaddingValues(0.dp),
    shape: Shape = MaterialTheme.shapes.large,
): Modifier
```

The one floating card every container builds on. It reads the theme only for its default shape, and adds two modifier nodes, in this order: `CardSurfaceNode` (draw, position-aware, the card's `NestingSurfaceNode`) and then `CardPaddingNode` (layout). The order is the one `background` then `padding` uses: Compose draws a node that also lays out inside its own layout, at its content's bounds, so one combined node would paint the surface inside the padding, hugging the content. Split, the surface is drawn by the padding's coordinator, at the card's full size.

- **Its fill is the app's setting.** The card takes no fill of its own: its surface node reads the setting (internal `LocalAMSurfaceFill`, dynamic) while it draws and while it's positioned, and resolves the look where the card stands, `lookOf(setting)` (`surfaceLook(setting, isEmbedded())`, `Foundations.md`), so a change to the setting, the mode or where the card lies redraws it without a relaunch. A solid app's cards are solid wherever they stand; a frosted app's are frosted on the backdrop and tinted where they're embedded (on a sheet, or lifted again inside another card). A part of a screen that wants the other look sets it with a nested `AMTheme(surfaceFill = …)` around it.
- **Elevated** (on the page) **in a solid app**: it pads the content by `padding`, then draws the elevated surface around it through the shared `ElevatedSurfaceDrawer` (`Foundations.md`): the shape's two-layer shadow at `elevation`, the shape filled with `AMTheme.Surface.primary`, the content **clipped to the shape** (Material's card clips, so ripples and full-bleed children keep the corners), and in dark mode the `selectionRing` hairline. `AMElevation.None` keeps fill, corners and hairline and casts nothing.
- **Elevated in a frosted app, on the backdrop**: frost over the nearest `AMBackdrop`'s art — the shadow outside the shape only, the blurred slice of the art, `AMTheme.Frosted.fill`, the content clipped, the frosted edge in both modes. With no backdrop, or before the art is placed, it's frost over the page color (the page under the fill, opaque); in the remote's or the spotlight's blurred window it's the fill straight over the window's own blur, sampling nothing, and solid while the system has cross-window blur off (`Foundations.md`). A frosted card redraws when it moves over its art. **Embedded** (on a sheet, or lifted again inside another card), it draws the tint instead (`AMTheme.Frosted.embeddedFill`, no blur of its own, the frosted edge), so it never samples through frost. Supporting text on it uses `AMTheme.secondaryText`, which is the frosted secondary in a frosted app once the content says it lies on the card (`ProvideAMOnSurface`, inside the card, which the library's containers provide; `Foundations.md`).
- **Flat** (nested): no fill, shadow, hairline, clip or `padding`; the content lies on the outer card, whose own padding is the only inset. Shadows never stack and padding never doubles.
- **Padding.** None by default: a card of rows takes none, since each row carries the recipe. A block of prose or a composite block takes `PaddingValues(spacing.lg)`.
- **Order.** After the modifiers that size the card, before the content's own: `Modifier.fillMaxWidth().amCard()`. A clickable card puts `clickable` after `amCard`, so the ripple stays inside the shape. The card itself handles no input.

### How a card knows it's nested

A modifier can't set a `CompositionLocal` for content composed elsewhere, so the card resolves its style in its nodes (internal `resolveSurfaceStyle()` in `SurfaceStyle.kt`). The padding node asks its own surface node (the nearest `NestingSurfaceNode` above it, in the same chain) while it measures; the surface node resolves, remembers the style the layout was measured with, redraws if it changed, and draws exactly that style, so the padding and the surface always agree. The resolution reads its locals inside that measure, so a change to them measures the card again. The rule:

1. `LocalAMSurfaceStyle` at the card's own place says `Flat` → **Flat**.
2. Otherwise it looks up the layout tree for the nearest `NestingSurfaceNode` (a `TraversableNode` keyed `NestingSurfaceKey`; every `amCard` and every lazy row's slice, `amCardSlice`, is one; the flat strip isn't). None → **Elevated**.
3. Found one: each `ProvideAMSurfaceStyle` call marks its subtree with a fresh `SurfaceStyleOrigin` (internal `LocalAMSurfaceStyleOrigin`). If the outer card and this card see the same origin, nothing re-styled between them → **Flat**. If they differ, a `ProvideAMSurfaceStyle(AMSurfaceStyle.Elevated)` sits between → **Elevated** again (re-lift: a pinned note floating inside a notebook card).

The library's containers (`AMSection`'s card or strip, `amSection`'s rows, `AMDisclosureGroup`'s body, `AMTabs`' content) also provide `Flat` for their content, so anything that reads `LocalAMSurfaceStyle` while composing sees it. Set the style with `ProvideAMSurfaceStyle`, never by providing `LocalAMSurfaceStyle` directly, or re-lifting won't see the change. Controls never read any of this.

**Flat blocks** (`AMSection`'s flat style; the self-filled rows `AMListRow`, `AMLabeledContent` and the reorderable row list's rows; the bar form of `AMProgressView`) use internal `Modifier.amFlatSurface()`. Its node (`FlatSurfaceNode`, draw and position-aware) resolves its style as a card does: when the block resolves to Elevated (it stands on its own) it draws a full-width strip through its own `ElevatedSurfaceDrawer`, `RectangleShape` at `Style.Shadow.None` with `outline = false` (no corners, shadow, hairline or edge), in the setting's look where it stands (`lookOf`): `Surface.primary` in a solid app; in a frosted app a frosted strip on the backdrop (redrawn as it moves over its art) and the tint on a sheet. When Flat (inside a card) it draws nothing, so a square strip never covers a card's rounded corners.

- **What a block holds lies on its strip.** The strip is no card for nesting (`FlatSurfaceNode` is no `NestingSurfaceNode`), and a modifier can't provide locals, so each block puts its content on the strip itself, inside the strip's node, never around it (the strip stands where the block is placed, so it never counts itself embedded): `AMListRow` and `AMLabeledContent` put each `ListItem` slot in `ProvideAMOnSurface` and read their row colors through internal `readOnSurface { AMRowDefaults.colors() }`, as `AMListCard` does (`Lists.md`); the bar form of `AMProgressView` wraps its lines in `ProvideAMOnSurface`. So the text on a strip takes the surface's secondary text (`Frosted.secondaryText` on a frosted strip, `onSurfaceVariant` on a solid one), nothing on it reads as on art, and a piece with its own shape on it is embedded (the tint in a frosted app).
- **Strips never stack.** A block that holds other blocks (the flat section, a reorderable row list's row) also provides `ProvideAMSurfaceStyle(Flat)` for its content, as a card's containers do, so an `AMListRow`, `AMLabeledContent` or bar inside resolves Flat and draws no strip of its own: one tint or one frosted layer per row, never two.

## `AMSection` — `Section.kt`

```kotlin
AMSection("Notifications", footer = "Email digest sends one summary a day.", style = AMSectionStyle.Rows) { … }
AMSection(header = { Text("Team", Modifier.weight(1f)); Text("2") }, style = AMSectionStyle.Rows) { … }
AMSection(footer = "Downloads play offline.", style = AMSectionStyle.Rows) { … }   // no header
```

A titled group: `AMSectionHeader` on top, the content block, an optional footer. String form `(title, modifier, footer: String?, style, elevation, inset, content)`; slot form `(header: RowScope slot, modifier, footer: slot?, style, elevation, inset, content)`; **header-less form** `(modifier, footer: String? = null, style, elevation, inset, content)`, for a group under a header of its own (a page's first group under its title) or needing none. All three go through private `SectionLayout(header?, …)`, which leaves the header row out when there's none. `content` is a `ColumnScope` lambda.

- **`AMSectionStyle`**, one per kind of content:
  - `Rows` — a group of rows stacked bare in a card with no padding (`amCard(elevation)`); each row brings the recipe. `AMDivider(inset = spacing.lg)` between rows that want a line.
  - `Card` (default) — block content in a card padded `spacing.lg` all round.
  - `Flat` — a reading block padded `spacing.lg` on a full-width strip (`amFlatSurface`) in the app's setting (`Surface.primary` in a solid app; frosted on the backdrop and the tint on a sheet in a frosted one), no corners or shadow; inside a card it draws nothing.
- **Rows go in `Rows`, not `Card`**: the card style pads, so a row in it would sit inset twice.
- The header, block and footer stack `spacing.sm` apart. The header takes the section header's look (`titleSmall` in `primary`, heading semantics); the footer gets `bodySmall` in `onSurfaceVariant` through `ProvideTextStyle` and `LocalContentColor`.
- **On art** (an art tone around it, `LocalAMArtTone` non-null) the header and the footer are art text for the tone: the header the primary ink (`Foundations.md`), the footer the secondary ink (`artInk(secondary = true)` as its content color, `amArtText(secondary = true)` on its box), `#D1D1D6` on dark art and `#2C2C2E` on light art, which hold 4.5:1 over the library's washed art (`DataDisplay.md`).
- `elevation` goes to the card (`Rows`, `Card`); `Flat` ignores it. The card's fill is the app's setting: solid in a solid app; frosted on the backdrop and tinted on a sheet in a frosted one.
- `inset = true` (default) stands the header, footer and card in from the section's edges by `spacing.lg`; pass `false` where the column is already inset (a page gutter). The flat strip always runs the full width.
- Every style wraps its content, inside its card or strip (private `SectionSurface`), in `ProvideAMOnSurface` (so its secondary text resolves from the card or strip, its content lies on it, not on art, and a frosted piece with its own shape in it draws the tint) and `ProvideAMSurfaceStyle(Flat)` (so a card or a flat row in it draws nothing of its own).

## `LazyListScope.amSection` — `Section.kt`

```kotlin
LazyColumn(Modifier.fillMaxSize()) {
    amSection(episodes, key = { it.id }, title = "Episodes", footer = "${episodes.size} episodes") { episode ->
        AMListRow(episode.title, subtitle = episode.date)
    }
}
```

`AMSectionStyle.Rows` for a `LazyColumn`, so only the rows on screen compose (a podcast's hundreds of episodes). A plain function on `LazyListScope`, so lowercase `am`. String form `(items, key, title: String? = null, footer: String? = null, elevation, inset, dividers, content: @Composable (T) -> Unit)`; slot form `(items, key, header: RowScope slot, footer: slot? = null, elevation, inset, dividers, content)`. Both go through private `lazySection`.

- **Items.** A header item (`AMSectionHeader`, `spacing.sm` below it) when there's a header; one item per row, `key(item)` as its lazy key; a footer item (`SectionFooter`, the same caption and art-text look as `AMSection`'s, `spacing.sm` above it) when there's a footer. Each kind has its own content type. Header and footer take the list's default keys, so `key` must be unique across the whole list (other sections included) and saveable in a `Bundle`.
- **A row's item**: a column inset by `spacing.lg` at the sides (`inset = true`), carrying `Modifier.amCardSlice(elevation, shapes.large, first, last)`, its content inside `ProvideAMOnSurface` and `ProvideAMSurfaceStyle(Flat)` as `SectionSurface`'s is; above every row but the first, `AMDivider(inset = spacing.lg)` unless `dividers = false`.
- **The list stacks its items edge to edge**: a `verticalArrangement` gap would break the card at each row; sections are set apart by items of their own or content padding.
- **Over art** in a frosted app the card blurs the nearest backdrop under each row; rows marked `amArtRow` on an `amArtRows` list give their own colors (`DataDisplay.md`).

### How a row draws its slice — `Modifier.amCardSlice`

Internal, in `Card.kt`. `CardSliceNode` is a `NestingSurfaceNode` (so cards inside a row are flat, and the slice is flat inside another card, drawing only its content) and owns an `ElevatedSurfaceDrawer`, drawing through `drawSurfaceSlice` (`Foundations.md`) in the look the app's setting gives it where it stands (`lookOf`, as the card's surface node); position-aware, so a frosted slice redraws as it scrolls over its art.

- **`cardSlice(size, first, last, reach)`** gives the row's `CardSlice`: `card`, the whole card's stand-in in the row's frame, running `reach` past each inner edge (the first row's card starts at its top, the last row's ends at its bottom); and `band`, the part of the drawing the row shows, its own rect opened by `reach` at the sides and at an outer edge. Neighbors' bands meet exactly, so the shadow, fill, frost, frosted edge and dark-mode hairline run unbroken down the rows, the first row shows the top corners and the shadow above, the last the bottom corners and the shadow below, and each row's content is clipped to the card's shape (ripples included).
- **`cardSliceReach(level)`** = the level's farthest shadow (a blur reaches about twice its radius, plus its offset) + 48 dp of room for the corners: 112 dp at `Low`.

## `AMDisclosureGroup` — `Disclosure.kt`

```kotlin
var expanded by rememberSaveable { mutableStateOf(false) }
AMDisclosureGroup("Billing", expanded, onExpandedChange = { expanded = it }, icon = painterResource(R.drawable.ic_credit_card)) { … }
```

Folding: a card (`amCard()`, in the app's setting) whose header opens it in place. String form `(title, expanded, onExpandedChange, modifier, icon: Painter?, enabled, content)`; slot form `(label: RowScope slot, expanded, onExpandedChange, modifier, enabled, content)`. State is hoisted (`expanded` / `onExpandedChange`, Material's spelling).

- **Header**: a full-width row on the recipe (`amRowPadding`), `clickable` with Material's ripple and `Role.Button`. The label row (`weight(1f)`, parts spaced `spacing.md`) gets `bodyLarge` in `onSurface`; then the `expand_more` chevron (`am_ic_expand_more`) in `onSurfaceVariant`, which turns 180° to point up while open (`animateFloatAsState`, `motionScheme.defaultSpatialSpec()`). A tap anywhere on the header asks to toggle.
- **Semantics**: while enabled, the header carries the expand action while closed and the collapse action while open, so TalkBack announces it as collapsed or expanded.
- **On the card**: the header and the body lie on the card (`ProvideAMOnSurface`, inside the card's column, so the card itself stands where it's placed and draws frost, not the tint, on the backdrop).
- **Body**: under a full-width `AMDivider`, the content in a column inset `spacing.lg` at the sides and `spacing.md` above and below, parts `spacing.md` apart, inside `ProvideAMSurfaceStyle(Flat)`. A row placed there keeps its own row padding on top of that inset. Divider and body enter and leave as one: `AnimatedVisibility` with `expandVertically` (`defaultSpatialSpec`) plus `fadeIn` (`defaultEffectsSpec`), and `shrinkVertically` (`defaultSpatialSpec`) plus `fadeOut` (`fastEffectsSpec`). The card's clip keeps them inside its corners as it grows.
- **`enabled = false`**, or a disabled container (`resolveEnabled`, `LocalAMEnabled`): the header ignores taps and its label and chevron dim to `onSurface` at Material's 0.38 alpha, keeping their place.

## `AMDivider` — `Divider.kt`

`AMDivider(modifier = Modifier, inset = 0.dp)`: Material's `HorizontalDivider` in its own thickness and color (`outlineVariant`), with `inset` as start padding. Pass `AMTheme.tokens.spacing.lg` between rows of any kind; leave 0 for a full-width line (under a card's title row, above a disclosure's body).

## Catalog pages

`demo/.../catalog/pages/layout/`, registered in `LayoutPages.entries` in this order (ids): `card`, `section`, `disclosure`, `divider`.

| Page | Preview | Loaded |
|---|---|---|
| Card (`CardPage.kt`) | a two-line row ("Project Atlas") in a card | a frosted card ("Project Atlas", its content in `ProvideAMOnSurface`) in a 200 dp `CatalogFrostedStage` (`Demo.md`); then, `spacing.xl` below, a prose card padded `spacing.lg` and a card of a two-line row over a switch row, `spacing.lg` apart |
| Section (`SectionPage.kt`) | `FullBleed`: a flat "Account" section (label over value, footer) on a surface strip | `Wide`, all `spacing.xl` apart: a `CatalogFrostedStage` inset by the page gutter holding Notifications (`Rows`, two switch rows, `inset = false`, padded `spacing.lg`, so the card stands `spacing.lg` in from the photo's edges); Notifications again (`Rows`, footer); Team (`Rows`, two members); About (`Card` at `AMElevation.None`); then Episodes, a 560 dp `LazyColumn` holding one `amSection` of a podcast's 240 episodes (`AMListRow`s, newest first, footer) |
| Disclosure (`DisclosurePage.kt`) | a closed "Advanced" card holding a strict-mode switch row | a `CatalogFrostedStage` first, padded `spacing.lg`: "Order summary" open and "Shipping details" closed, `spacing.md` apart, frosted over the photo, each with its own state; then, `spacing.xl` below and `spacing.md` apart: "Order summary" open, "Billing" with a credit-card glyph, "Shipping details" |
| Divider (`DividerPage.kt`) | `FullBleed`: "Inbox" and "Archive" rows on a surface strip with a full-width divider | a card: an order total title row, a full-width divider, then subtotal, shipping and total rows (`ListItem` with the amount trailing) with inset dividers |

Demo glyphs these pages use: `ic_rectangle`, `ic_view_agenda`, `ic_expand_circle_down`, `ic_horizontal_rule` (entry icons) and `ic_credit_card`.

## See also

- `Foundations.md` — the style tier (the row padding's value), `AMSurfaceStyle` and its resolver, the elevated-surface drawer, the surface setting and how a surface resolves its look.
- `Lists.md` — the rows built on the recipe and the flat strip (`AMListRow`, `AMListCard`, `AMLabeledContent`, the reorderable rows).
