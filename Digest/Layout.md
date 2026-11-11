---
scope: Static layout frames — Modifier.amCard(), the row recipe (Modifier.amRowPadding() for hand-built rows, AMRowDefaults for Material ListItem rows), AMSection, AMDisclosureGroup, AMDivider, and how nested surfaces demote (AMSurfaceStyle)
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/layout/**, androidmtrls/src/main/res/drawable/am_ic_expand_more.xml, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/layout/**]
keywords: [Modifier.amCard, card, CardSurfaceNode, CardPaddingNode, draw inside own layout, shapes.large, padding, clip, fill, Solid, Frosted, AMBackdrop, elevation, AMElevation.None, nested cards, AMSurfaceStyle, Flat, Elevated, ProvideAMSurfaceStyle, re-lift, NestingSurfaceNode, TraversableNode, findNearestAncestor, resolveSurfaceStyle, SurfaceStyleOrigin, ElevatedSurfaceDrawer, row recipe, rowVerticalPadding, 28 dp, 80 dp, 100 dp, Modifier.amRowPadding, AMRowDefaults, contentPadding, colors, shapes, ListItem, ListItemDefaults, minimum height, selectedContainerColor, flat rows, AMSection, AMSectionStyle, Rows, Card, Flat, header, footer, footer over art, LocalAMBackdrop, glyphShadow, inset, amFlatSurface, AMDisclosureGroup, folding, expand_more, expand, collapse, AnimatedVisibility, motionScheme, AMDivider, HorizontalDivider, inset divider, CardPage, SectionPage, DisclosureGroupPage, DividerPage, LayoutPages]
updated: 2026-11-10
---

# Layout

The static frames a screen lives inside: `Modifier.amCard()` (the floating card), the row recipe (`Modifier.amRowPadding()` for hand-built rows, `AMRowDefaults` for rows built on Material's `ListItem`), `AMSection` (a header, a card or flat block, a footer), `AMDisclosureGroup` (a card that folds open in place) and `AMDivider` (Material's divider with an inset). Cards nested in cards demote themselves to flat (`AMSurfaceStyle`).

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `layout/Card.kt` | `Modifier.amCard` (private `CardSurfaceNode`, `CardPaddingNode` and their elements), `Modifier.amRowPadding` (private `RowPaddingNode`), `AMRowDefaults`, internal `Modifier.amFlatSurface` |
| `layout/Section.kt` | `AMSectionStyle`, `AMSection` (string and slot forms), private `SectionCard` |
| `layout/Disclosure.kt` | `AMDisclosureGroup` (string and slot forms) |
| `layout/Divider.kt` | `AMDivider` |

Pieces in `foundations/` the area builds on (`Foundations.md`): `AMTheme.rowVerticalPadding` (`Theme.kt`, from `Style.Row` in `Style.kt`); `AMSurfaceStyle`, `LocalAMSurfaceStyle`, `ProvideAMSurfaceStyle` and the internal nesting resolver (`NestingSurfaceNode`, `resolveSurfaceStyle`, `SurfaceStyleOrigin`) in `SurfaceStyle.kt`; the internal `ElevatedSurfaceDrawer` in `Hairline.kt`; `AMSurfaceFill` and `AMBackdrop`.

- Glyph: `androidmtrls/src/main/res/drawable/am_ic_expand_more.xml` (the disclosure chevron).
- Catalog pages: `demo/.../catalog/pages/layout/` (below).

## The row recipe

**A row is its content plus 28 dp above and below and 16 dp at the sides, with no fixed or minimum height.** One line of `bodyLarge` makes an 80 dp row; a `bodyLarge` title over a `bodyMedium` supporting line makes a 100 dp row; a switch, a picture or a larger font scale makes it taller by its own height. Rows run taller than Material's list item (56/72/88) on purpose: about 1.45 times the platform's row, so a list reads airy and each row is an easy target.

- **The values.** The 28 dp is `AMTheme.rowVerticalPadding`, one read-only constant in the style tier (`Style.Row.VerticalPadding`), tuned there and nowhere else. The sides are the spacing scale's `lg` (16 dp), so an app that re-tunes `spacing.lg` moves the sides but not the 28.
- **Every row uses it**: rows in a card and rows on a flat strip alike, hand-built or built on `ListItem`, the disclosure card's header included.
- **Hand-built rows: `Modifier.amRowPadding()`.** It fills the width (`fillMaxWidth`) and pads the content by `spacing.lg` at the sides (read from `LocalAMTokens` in the node, start-aware in right to left) and 28 dp above and below. It draws no surface. Inside a nested (flat) card it still applies: it is the row's own inset, not the card's.
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
  - `contentPadding` = `PaddingValues(horizontal = spacing.lg, vertical = rowVerticalPadding)`. Material's list item takes its content padding out of its own minimum height: it applies the padding, then gives the content a minimum of (56, 72 or 88 dp by line count) minus the top and bottom padding, floored at 0. With 28 dp above and below that minimum is 0, 16 or 32 dp, always below real content, so nothing stacks and the row is exactly content plus 56 dp. Material centers the leading and trailing content while the content area is under 88 dp tall and top-aligns them beyond it.
  - `colors()` = Material's list item colors on a transparent container at rest, disabled and selected (only a dragged row keeps Material's container), and a checked or selected row keeps its resting content colors. **A row never tints itself when checked or selected**: it stays flat on its card or strip, and its control (a switch, a radio button) shows the state in Material's own look.
  - `shapes()` = Material's list item shapes made square at rest, pressed, focused, hovered and selected (dragged keeps Material's), so a row never rounds into a pill; a card's clip rounds the rows at its corners, ripple included.
  - Every `ListItem` overload used here is the current (non-experimental) one with `contentPadding`, `colors` and `shapes`; the old fixed-metric overload (`headlineContent`, `tonalElevation`) isn't used.
- **Between rows**: `AMDivider(inset = AMTheme.tokens.spacing.lg)`, so the line starts where the rows' text does.

## `Modifier.amCard()` — `Card.kt`

```kotlin
@Composable @ReadOnlyComposable
public fun Modifier.amCard(
    fill: AMSurfaceFill = AMSurfaceFill.Solid,
    elevation: AMElevation = AMElevation.Low,
    padding: PaddingValues = PaddingValues(0.dp),
    shape: Shape = MaterialTheme.shapes.large,
): Modifier
```

The one floating card every container builds on. It reads the theme only for its default shape, and adds two modifier nodes, in this order: `CardSurfaceNode` (draw, position-aware, the card's `NestingSurfaceNode`) and then `CardPaddingNode` (layout). The order is the one `background` then `padding` uses: Compose draws a node that also lays out inside its own layout, at its content's bounds, so one combined node would paint the surface inside the padding, hugging the content. Split, the surface is drawn by the padding's coordinator, at the card's full size.

- **Elevated** (on the page): it pads the content by `padding`, then draws the elevated surface around it through the shared `ElevatedSurfaceDrawer` (`Foundations.md`): the shape's two-layer shadow at `elevation`, the shape filled with `AMTheme.Surface.primary`, the content **clipped to the shape** (Material's card clips, so ripples and full-bleed children keep the corners), and in dark mode the `selectionRing` hairline. `AMElevation.None` keeps fill, corners and hairline and casts nothing.
- **Frosted** (`fill = AMSurfaceFill.Frosted`): glass over the nearest `AMBackdrop`'s art — the shadow outside the shape only, the blurred slice of the art, `AMTheme.Frosted.fill`, the content clipped, the frosted edge in both modes. With no backdrop, or before the art is placed, it draws solid. A frosted card redraws when it moves over its art. Supporting text on it uses `AMTheme.Frosted.secondaryText`.
- **Flat** (nested): no fill, shadow, hairline, clip or `padding`; the content lies on the outer card, whose own padding is the only inset. Shadows never stack and padding never doubles.
- **Padding.** None by default: a card of rows takes none, since each row carries the recipe. A block of prose or a composite block takes `PaddingValues(spacing.lg)`.
- **Order.** After the modifiers that size the card, before the content's own: `Modifier.fillMaxWidth().amCard()`. A clickable card puts `clickable` after `amCard`, so the ripple stays inside the shape. The card itself handles no input.

### How a card knows it's nested

A modifier can't set a `CompositionLocal` for content composed elsewhere, so the card resolves its style in its nodes (internal `resolveSurfaceStyle()` in `SurfaceStyle.kt`). The padding node asks its own surface node (the nearest `NestingSurfaceNode` above it, in the same chain) while it measures; the surface node resolves, remembers the style the layout was measured with, redraws if it changed, and draws exactly that style, so the padding and the surface always agree. The resolution reads its locals inside that measure, so a change to them measures the card again. The rule:

1. `LocalAMSurfaceStyle` at the card's own place says `Flat` → **Flat**.
2. Otherwise it looks up the layout tree for the nearest `NestingSurfaceNode` (a `TraversableNode` keyed `NestingSurfaceKey`; every `amCard` is one). None → **Elevated**.
3. Found one: each `ProvideAMSurfaceStyle` call marks its subtree with a fresh `SurfaceStyleOrigin` (internal `LocalAMSurfaceStyleOrigin`). If the outer card and this card see the same origin, nothing re-styled between them → **Flat**. If they differ, a `ProvideAMSurfaceStyle(AMSurfaceStyle.Elevated)` sits between → **Elevated** again (re-lift: a pinned note floating inside a notebook card).

The library's containers (`AMSection`'s cards, `AMDisclosureGroup`'s body) also provide `Flat` for their content, so anything that reads `LocalAMSurfaceStyle` while composing sees it. Set the style with `ProvideAMSurfaceStyle`, never by providing `LocalAMSurfaceStyle` directly, or re-lifting won't see the change. Controls never read any of this.

**Flat blocks** (`AMSection`'s flat style; the self-filled rows `AMListRow`, `AMLabeledContent` and the reorderable row list's rows; the bar form of `AMProgressView`) use internal `Modifier.amFlatSurface()`: a full-width `Surface.primary` fill when the block resolves to Elevated (it stands on the page), none when Flat (inside a card), so a square fill never covers a card's rounded corners.

## `AMSection` — `Section.kt`

```kotlin
AMSection("Notifications", footer = "Email digest sends one summary a day.", style = AMSectionStyle.Rows) { … }
AMSection(header = { Text("Team", Modifier.weight(1f)); Text("2") }, style = AMSectionStyle.Rows) { … }
```

A titled group: `AMSectionHeader` on top, the content block, an optional footer. String form `(title, modifier, footer: String?, style, fill, elevation, inset, content)`; slot form `(header: RowScope slot, modifier, footer: slot?, style, fill, elevation, inset, content)`. The string form delegates to the slot form. `content` is a `ColumnScope` lambda.

- **`AMSectionStyle`**, one per kind of content:
  - `Rows` — a group of rows stacked bare in a card with no padding (`amCard(fill, elevation)`); each row brings the recipe. `AMDivider(inset = spacing.lg)` between rows that want a line.
  - `Card` (default) — block content in a card padded `spacing.lg` all round.
  - `Flat` — a reading block padded `spacing.lg` on a full-width `Surface.primary` strip (`amFlatSurface`), no corners or shadow; inside a card it draws no fill.
- **Rows go in `Rows`, not `Card`**: the card style pads, so a row in it would sit inset twice.
- The header, block and footer stack `spacing.sm` apart. The header takes the section header's look (`titleSmall` in `primary`, heading semantics); the footer gets `bodySmall` in `onSurfaceVariant` through `ProvideTextStyle` and `LocalContentColor`.
- **Over a picture** (inside an `AMBackdrop`, `LocalAMBackdrop` non-null) the footer takes the header's over-art treatment: `onSurface` and the text legibility shadow (`glyphShadow(Style.Shadow.Text)`), so both hold over the art (`Foundations.md`).
- `fill` and `elevation` go to the card (`Rows`, `Card`); `Flat` ignores them.
- `inset = true` (default) stands the header, footer and card in from the section's edges by `spacing.lg`; pass `false` where the column is already inset (a page gutter). The flat strip always runs the full width.
- The card styles wrap their content in `ProvideAMSurfaceStyle(Flat)`.

## `AMDisclosureGroup` — `Disclosure.kt`

```kotlin
var expanded by rememberSaveable { mutableStateOf(false) }
AMDisclosureGroup("Billing", expanded, onExpandedChange = { expanded = it }, icon = painterResource(R.drawable.ic_credit_card)) { … }
```

Folding: a card (`amCard(fill = fill)`) whose header opens it in place. String form `(title, expanded, onExpandedChange, modifier, icon: Painter?, fill, enabled, content)`; slot form `(label: RowScope slot, expanded, onExpandedChange, modifier, fill, enabled, content)`. State is hoisted (`expanded` / `onExpandedChange`, Material's spelling).

- **Header**: a full-width row on the recipe (`amRowPadding`), `clickable` with Material's ripple and `Role.Button`. The label row (`weight(1f)`, parts spaced `spacing.md`) gets `bodyLarge` in `onSurface`; then Material's `expand_more` chevron in `onSurfaceVariant`, which turns 180° to point up while open (`animateFloatAsState`, `motionScheme.defaultSpatialSpec()`). A tap anywhere on the header asks to toggle.
- **Semantics**: the header carries the expand action while closed and the collapse action while open, so TalkBack announces it as collapsed or expanded.
- **Body**: under a full-width `AMDivider`, the content in a column inset `spacing.lg` at the sides and `spacing.md` above and below, parts `spacing.md` apart, inside `ProvideAMSurfaceStyle(Flat)`. A row placed there keeps its own row padding on top of that inset. Divider and body enter and leave as one: `AnimatedVisibility` with `expandVertically` (`defaultSpatialSpec`) plus `fadeIn` (`defaultEffectsSpec`), and `shrinkVertically` (`defaultSpatialSpec`) plus `fadeOut` (`fastEffectsSpec`). The card's clip keeps them inside its corners as it grows.
- **`enabled = false`**, or a disabled container (`resolveEnabled`, `LocalAMEnabled`): the header ignores taps and dims to Material's 0.38 alpha, keeping its place.

## `AMDivider` — `Divider.kt`

`AMDivider(modifier = Modifier, inset = 0.dp)`: Material's `HorizontalDivider` in its own thickness and color (`outlineVariant`), with `inset` as start padding. Pass `AMTheme.tokens.spacing.lg` between rows of any kind; leave 0 for a full-width line (under a card's title row, above a disclosure's body).

## Catalog pages

`demo/.../catalog/pages/layout/`, registered in `LayoutPages.entries` in this order (ids): `card`, `section`, `disclosureGroup`, `divider`.

| Page | Preview | Loaded |
|---|---|---|
| Card (`CardPage.kt`) | a two-line row ("Project Atlas") in a card | a frosted card ("Project Atlas") over a photo in a 200 dp `AMBackdrop` (`CatalogArt.Valley`: a snowy landscape, a city at night in dark mode; clipped to `shapes.large`), a prose card padded `spacing.lg`, and a card of a two-line row over a switch row |
| Section (`SectionPage.kt`) | `FullBleed`: a flat "Account" section (label over value, footer) on a surface strip | Notifications (`Rows`, two switch rows, footer), Team (`Rows`, two members), About (`Card` at `AMElevation.None`), all `inset = false`, `spacing.xl` apart |
| Disclosure group (`DisclosureGroupPage.kt`) | a closed "Advanced" card holding a strict-mode switch row | three cards `spacing.md` apart: "Order summary" open, "Billing" with a credit-card glyph, "Shipping details" |
| Divider (`DividerPage.kt`) | `FullBleed`: "Inbox" and "Archive" rows on a surface strip with a full-width divider | a card: an order total title row, a full-width divider, then subtotal, shipping and total rows (`ListItem` with the amount trailing) with inset dividers |

Demo glyphs these pages add: `ic_rectangle`, `ic_view_agenda`, `ic_expand_circle_down`, `ic_horizontal_rule` (entry icons) and `ic_credit_card`.
