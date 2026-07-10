---
scope: Read-only display primitives — avatars, calendars, tags, badges, headers, empty states, remote images, dividers, progress/spinner, label/value rows, timelines, and skeleton shimmers
globs:
  - mtrls/src/**/content/**
keywords: [AMAvatar, AMCalendar, amMonthDayCells, amOrderedWeekdays, month grid, firstDayOfWeek, WeekFields, YearMonth, event dots, today circle, tnum, AMTag, AMTagTint, AMSemantic, AMBadge, AMBadgedBox, AMSelectionMark, AMSelectionCheckBox, selection check, glyph shadow, BlurredEdgeTreatment, amSquircle, AMSectionHeader, AMPageHeader, AMEmptyState, AMRemoteImage, AMRemoteImageFallback, AMDivider, AMSpinner, AMProgressBar, AMRowContent, AMTimeline, AMTimelineRow, AMTimelineDot, AMSkeleton, amShimmer, shimmer, redacted, placeholder tree, reduce motion, djb2, initials, hue, wash opacity, monospaced digits, BlendMode.Plus, BlendMode.SrcIn, plusLighter additive gleam]
updated: 2026-07-09
---

# Content

Non-interactive presentation primitives in
`mtrls/src/main/kotlin/io/github/jmsdnns/androidmtrls/content/`: they render
data, never own a binding or emit events (sole exception: the
empty-state CTA, which delegates to `AMPrimaryButton`). Text is `BasicText`;
colors come from the static
`AMTheme` (`separator`, `hairlineWidth`, `neutralFill`, `Surface.*`, `Text.*`)
and the overridable `LocalAMTokens` (`semantic.*`, `spacing`, `radius`,
`listRowInsets`). Async images go through Coil 3 `SubcomposeAsyncImage`
(`loading`/`error` subcompose slots both show the fallback). JVM tests:
`mtrls/src/test/kotlin/**/content/` (AvatarTest.kt — djb2/initials vectors).

## AMAvatar — `Avatar.kt`

Profile glyph, circle or rounded square. Fallback chain: `url` non-null and
Coil `success` → image (`ContentScale.Crop`); loading, error, AND `url == null`
→ initials fallback; initials empty after extraction → `fallbackIcon` (default
`Icons.Default.Person`) tinted white. `AMAvatarSize` enum: `SM`/`MD`/`LG` =
32/44/64dp diameter with 13/17/24sp initials font (`MD` default); the fallback
icon is sized `fontSizeSp.dp` (so 17dp at MD). `AMAvatarShape` sealed class:
`Circle` (default, `CircleShape`) or `Square(cornerRadius: Dp)` (clipped via
`amSquircle` — the house continuous-corner squircle, NOT a plain
`RoundedCornerShape`) — pass `AMTheme.Radius.sm/md/lg.dp` to stay on the
radius scale. A `AMTheme.hairlineWidth.dp` (0.5dp) `AMTheme.separator` border
draws over the clip shape (plain `Modifier.border`, NOT the px-clamped
`amHairlineWidth()` AMDivider uses).

- **Fallback color** is `Color.hsv(hue × 360, 0.55f, 0.85f)`; the sat/value
  constants are the **public top-level consts** `AM_AVATAR_SATURATION` /
  `AM_AVATAR_BRIGHTNESS` (screaming-case with the AM prefix, like every other
  public symbol).
- **Hue hash** (`internal fun avatarDjb2Hash`): djb2, seed `5381uL`,
  `hash = (hash shl 5) + hash + codePoint`, ULong wrap (no mask needed —
  Kotlin ULong wraps at 2^64, matching Swift UInt64 `&+`/`&*`). Iterates
  `String.codePoints()`, **never `.toCharArray()`** — emoji are 2 UTF-16
  chars but 1 code point; char iteration produces a different hash than the
  Swift `unicodeScalars` reference. `avatarHueIndex = hash % 360u`;
  `avatarHue = index / 360f`. Hashed from the **raw `initials` string**, not
  the extracted initials, so hue is stable across name variants. Hue,
  extracted initials, and background color are memoized via
  `remember(initials)`.
- **`extractInitials`** (internal): trim, split on `\s+`, first char of up to
  two parts, uppercased ("Christopher Knight" → "CK", "Jane" → "J",
  null/"" → ""). Initials render white 13/17/24sp Medium.
- Test vectors (AvatarTest.kt, Python-derived): "A"→hue 158, "Alice"→227,
  "李雷"→34, "😀"→85, "😀A"→350, "Christopher Knight"→45; "" → seed 5381 →
  hue index 341.

## AMCalendar — `Calendar.kt` (+ `CalendarMath.kt`)

Month-at-a-glance grid: a weekday header over rows of bare day numerals, one
month at a time. A **display primitive that also reports day taps** (not a date
picker — no confirm step, no popup):
`AMCalendar(month: YearMonth, selection: LocalDate, onSelectionChange: (LocalDate) -> Unit, markers: (LocalDate) -> Int = { 0 }, modifier)`.
Hoisted selection; `java.time` throughout. Flat (no card chrome), self-pads
horizontal `spacing.lg` / vertical `spacing.md`, `fillMaxWidth`; drop it on a
`Surface.primary` band or in a **bare `AMCard` (no `padding:`)** for the elevated
treatment. Root Column `spacedBy(spacing.sm)`.

- **Pure math** (`CalendarMath.kt`, internal, JVM-tested in `CalendarMathTest.kt`
  — the CardStackMath/FlowMath "extract the math" pattern):
  `amMonthDayCells(month, firstDayOfWeek): List<LocalDate?>` — leading nulls pad
  the 1st so column 0 = `firstDayOfWeek`
  (`(firstOfMonth.dayOfWeek.value − firstDayOfWeek.value + 7) % 7`; `DayOfWeek.value`
  is ISO MON=1…SUN=7), one entry per day, trailing nulls to a multiple of 7 (the
  composable `chunked(7)`s into week rows); `amOrderedWeekdays(firstDayOfWeek):
  List<DayOfWeek>` rotates the seven weekdays to start on `firstDayOfWeek`. Locale
  via `WeekFields.of(Locale.getDefault()).firstDayOfWeek` — the same recipe as
  `AMDatePickerRow`'s dialog (do not import the dialog).
- **Weekday header**: 7 equal `weight(1f)` columns of short day symbols
  (`DayOfWeek.getDisplayName(TextStyle.SHORT, locale)` — 3-letter "Sun"/"Mon", the
  analog of iOS `calendar.shortWeekdaySymbols`) in `AMTheme.Type.labelSmall`
  (11sp Medium — the house map of iOS `caption2.semibold`; emphasis is Medium,
  never SemiBold) / `Text.secondary`.
- **Day grid**: week rows `spacedBy(spacing.xs)`; out-of-month (`null`) cells are
  empty `Spacer`s of the cell height so geometry never shifts.
- **Day cell**: the whole 44dp × `weight(1f)` cell is the tap target
  (`Role.Button`) with an **unbounded** circular `amRipple(radius = cellHeight/2)` —
  the cell is a flexible-width column, so a *bounded* ripple would clip to a wide
  ellipse; the unbounded disc echoes the numeral badge. One shared stateless
  ripple, each cell `interactionSource = null` (the AMDatePickerDialog day-grid
  pattern). Numeral in a 30dp circular frame, `AMTheme.Type.secondary` (14sp — the house map
  of iOS `.subheadline`) + `fontFeatureSettings = "tnum"` (monospaced digits align
  across rows), today Medium / others Normal.
- **Background circle** (one circle carries both states, **today wins**): today →
  `tokens.semantic.accent` resolved (numeral inverts to `AMTheme.Surface.primary`
  resolved — white-on-black light, black-on-white dark); selected non-today →
  `AMTheme.neutralFill()` wash (numeral stays `Text.primary`); else transparent.
- **Event dots**: three fixed 4dp slots below the numeral, `spacedBy(3.dp)`,
  filled accent up to `min(markers(day), 3)`, else transparent — stable cell
  height whatever the count. File-level constants `CalendarCellHeight` 44,
  `CalendarNumberSize` 30, `CalendarDotSize` 4, `CalendarDotSpacing` 3,
  `CalendarMaxDots` 3 are hardcoded (fixed calendar geometry), not tokens.
- **A11y**: each cell `contentDescription` = full localized date
  (`DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)`); the numeral text is
  `clearAndSetSemantics {}` so TalkBack announces only the date (not "28"); the
  selected cell carries the `selected` state.
- Demo: `pages/content/CalendarPage.kt` (`calendarEntry`, id `content-calendar`)
  — Flat / In a card / With markers groups.

## AMTag — `Tag.kt`

Caption-semibold capsule pill (`RoundedCornerShape(50)`). Designated overload
takes `tint` + `RowScope` content slot (capsule, fill, inner
`spacedBy(spacing.xs)` are applied; the slot styles its own foreground —
nothing is provided downward). String overload `AMTag(text, icon?, tint)`
renders an optional 12dp leading icon (drawn with foundation `Image` +
`ColorFilter.tint` — no Material `Icon`) and `AMTheme.Type.caption` text at
`FontWeight.Medium` (12sp — the house map of iOS `.caption.semibold`; emphasis is
Medium, never SemiBold) in the tint foreground. Padding: horizontal `spacing.sm`,
vertical `spacing.xs`.

- `AMTagTint` is a **top-level** `@Immutable` enum (`NEUTRAL`/`INFO`/`SUCCESS`/
  `WARNING`/`DESTRUCTIVE`), pure cases — color resolution lives in the
  internal `@Composable` helpers `amTagForegroundColor` / `amTagFillColor` in
  Tag.kt (reusable by other components on the same tint axis).
- The four charged cases delegate their **base color** to the canonical
  `AMSemantic` axis (`tokens/Semantic.kt`) via
  `AMSemantic.<Role>.color(tokens.semantic)`; `NEUTRAL` and the per-hue
  fill-opacity table stay local to Tag.kt.
- Foreground: NEUTRAL = `AMTheme.Text.primary`; charged cases =
  `AMSemantic.<Role>.color(tokens.semantic)` (= `tokens.semantic.<role>`). Fill
  wash opacities are **per-hue tuned — do not normalise**: NEUTRAL
  `AMTheme.neutralFill()` (0.08), INFO 0.12, SUCCESS 0.15, WARNING 0.18 (yellow
  reads weaker at equal alpha), DESTRUCTIVE 0.12 — each applied as
  `AMSemantic.<Role>.color(tokens.semantic).copy(alpha = …)`.

## AMBadge / AMBadgedBox — `Badge.kt`

Notification-pip micro-indicator, **NOT a status pill** (that's `AMTag`).
`AMBadgeContent` sealed class: `Dot` (8dp pip, `invisibleToUser`) or
`Count(value)` (capsule, `defaultMinSize` 16×16dp, horizontal padding 5dp,
`AMTheme.Type.labelSmall` white with `fontFeatureSettings = "tnum"` for monospaced digits,
`"99+"` past 99, announces `"$value notifications"`). `value <= 0` renders a
**zero-size Spacer** — invisible but compositionally present, so a live count
binding never shifts the caller's layout. Default tint
`tokens.semantic.destructive`; both variants carry a 1.5dp
`AMTheme.Surface.primary` ring for legibility on non-white hosts.

There is **no `Modifier.amBadge`** — positioning is the host-wrapping
composable `AMBadgedBox(badge, content)`: badge `align(Alignment.TopEnd)` plus
a custom `layout` that places at `(width/2, -height/2)`, centering the badge
on the host's top-trailing corner whatever its measured size (the Compose
equivalent of the iOS `alignmentGuide` trick).

## AMSelectionMark / AMSelectionCheckBox — `SelectionMark.kt`

Top-trailing selection checkmark for selectable photo/album tiles (iOS
`IMSelectionMark` + `.imSelectionCheck`). **`AMSelectionMark(modifier)`** draws
`Icons.Filled.CheckCircle` at 17dp, `Color.White`, `contentDescription =
"Selected"`.

- **Legibility shadow is MANUAL and glyph-level** — the `.text` elevation recipe
  ([AMElevation.Text]), same problem as `AMTextLegibilityLabel` but for a vector
  glyph. **Never `Modifier.amElevation(Text)`** — that shadows a rounded-rect
  silhouette and renders a grey band behind the transparent parts of the glyph.
  Instead the vector is drawn three times in a stacked Box: two blurred shadow
  copies tinted with the fixed-black shadow tokens (`AMTheme.Shadow.textFar` far
  = 10dp blur / 4dp y / black α 0.10; `AMTheme.Shadow.textNear` near = 3dp blur /
  1dp y / black α 0.12), then the crisp white copy on top. `ColorFilter.tint`
  (SrcIn) masks the black to the glyph's alpha; each shadow uses
  `Modifier.blur(radius, BlurredEdgeTreatment.Unbounded)` — **Unbounded is
  required** so the blur bleeds past the 17dp glyph box instead of being clipped
  to it (RenderEffect blur, `minSdk 31`). Shadow copies are decorative
  (`contentDescription = null`); only the crisp glyph carries the label.
  **Deliberately no `Surface.primary` ring** (unlike `AMBadge`) — a white ring
  around a white glyph adds nothing.
- **`AMSelectionCheckBox(isSelected, modifier, content)`** is the positioner:
  overlays the mark at `Alignment.TopEnd` inset `tokens.spacing.xs`, draws
  nothing when `isSelected` is false (zero-state hiding, like `AMBadgedBox`, so
  live selection binds without a call-site conditional). iOS's
  `.imSelectionCheck(_:)` is a View modifier; on Android a `Modifier` cannot
  compose a composable child, so — matching the `AMBadgedBox` precedent (there is
  no `Modifier.amBadge`) — positioning is a host-wrapping composable. Put the
  tile's size/clip/background AND any `toggleable`/`selectable` on the wrapper's
  `modifier` so the mark overlays them.

## AMSectionHeader — `SectionHeader.kt`

Floating uppercase section label that stays legible over blur/busy backdrops.
The legibility shadow is GLYPH-level, like iOS's `.imElevation(.text)` (which
shadows the text alpha mask, never the bounding rect): the internal
`AMTextLegibilityLabel` draws the label twice via `TextStyle.shadow` — bottom
copy carries the far layer (10dp blur / y 4dp / black α 0.10), top copy the
near layer (3dp blur / y 1dp / black α 0.12); identical text+style means the
top fill exactly covers the bottom. **Never apply
`Modifier.amElevation(AMElevation.Text)` to a text label's Box** — the
rounded-rect silhouette renders as a gray band behind transparent labels.
String overload renders `title.uppercase()` at 12sp Medium, 0.5sp
letter-spacing, `AMTheme.Text.primary`, with the glyph shadow. Slot overload
is a bare `fillMaxWidth` Box — slot content styles itself and supplies its
own shadow (reuse `AMTextLegibilityLabel` for text).

## AMPageHeader — `PageHeader.kt`

Large screen-title counterpart to `AMSectionHeader`. String overload renders
28sp Bold, `AMTheme.Text.primary`, **as-is — no uppercasing** (page titles are
headings, not labels), through the same `AMTextLegibilityLabel` glyph shadow. No
`letterSpacing` — iOS `imHeaderTreatment` applies `.title.bold` with no
`.tracking`. The slot overload is a bare
`fillMaxWidth` Box; the caller's content applies its own font/color/shadow.

## AMEmptyState — `EmptyState.kt`

Centered icon → title → description → actions column (the
`ContentUnavailableView` analog; document order gives TalkBack the right
reading order for free). Geometry: vertical padding `spacing.xl`, items
`spacedBy(spacing.lg)`; icon 56dp tinted `Text.secondary`
(`contentDescription = null` — decorative); title 22sp SemiBold primary,
centered; description `AMTheme.Type.secondary`; actions sub-column `spacedBy(spacing.sm)`.

- Designated overload: trailing `actions: @Composable ColumnScope.() -> Unit`
  (default empty) — caller owns button styling.
- Convenience overload takes `action: AMEmptyStateAction?` (a `@Stable` data
  class: `title`, `icon: ImageVector?`, `onClick`) and renders it as the public
  `AMPrimaryButton` under `Modifier.width(IntrinsicSize.Max)`, so the CTA hugs its
  single-line label (the iOS `.fixedSize(horizontal:)` intrinsic pill) and centers
  in the actions column. `AMPrimaryButton` fills width internally, so the intrinsic
  width drives it to hug rather than stretch. No icon → the
  `AMPrimaryButton(label, …)` String overload; with an icon → the
  composable-slot overload rendering a 16dp `Text.primary`-tinted leading glyph
  + `spacing.xs` gap + a label styled by the shared `internal amButtonLabelStyle`
  (Buttons.kt, 17sp SemiBold) so both the icon and icon-less CTAs render the
  identical standard primary-button label typography.

## AMRemoteImage — `RemoteImage.kt`

Square rounded async image — one API instead of a per-call-site AsyncImage
switch. `size: Dp` sets both dimensions; clips to
`amSquircle(cornerRadius ?? tokens.radius.sm)` (the house continuous-corner
squircle, 8dp default; pass `0.dp` for square). Loading, error, and `url == null` all render the fallback (null URL
makes no network request); only success shows the image —
`ContentScale.Crop` default (iOS `.fill`), pass `ContentScale.Fit` to
letterbox. Two overloads: `fallbackIcon: ImageVector` (default
`Icons.Default.AccountBox` — closest core-icons analog to the iOS `"photo"`
symbol; callers with `material-icons-extended` can pass `Icons.Default.Image`)
or a custom `fallback` composable slot (use `Modifier.fillMaxSize()` inside it).
Both overloads take `modifier: Modifier = Modifier` (first optional param, after
`url`/`size`) applied to the root `size`+`clip` Box.

`AMRemoteImageFallback(size, modifier, icon)` is public: `Text.secondary` at α
0.10 wash, centered icon at `size × 0.5` tinted `Text.secondary`; its `modifier`
(first optional, after `size`) is applied to the fallback's root box.

## AMDivider — `Divider.kt`

Full-width hairline: height `amHairlineWidth()` (0.5dp clamped to ≥ 1 physical
px so it never vanishes at low density), fill `AMTheme.separator`. `inset: Dp`
is leading padding, default `0.dp` (full-bleed); **pass
`AMTheme.Input.inset.dp` (14dp) to align with `AMListRow`/`AMRowContent`
text**. Does not shadow Material3's `HorizontalDivider` (different name).

## AMSpinner / AMProgressBar — `ProgressView.kt`

SwiftUI's `ProgressViewStyle` injection has no Compose analog, so the two iOS
styles are standalone composables.

- **`AMSpinner(label?, compact = false)`** — indeterminate. Default wraps
  itself in `AMCard` (full-screen loader block); `compact = true` drops the
  card for inline use inside an existing card. 28dp arc, 3dp round-cap stroke,
  monochrome `Text.secondary` on a transparent track (no track ring). One
  `rememberInfiniteTransition` (stops when it leaves the tree) drives the
  recognizable Material indeterminate **breathe** from three values: a linear
  0→360° container **rotation** (1568ms), a `FastOutSlowInEasing` sweep **pulse**
  keyframing 30°→270°→30° (1400ms), and a per-cycle **start-angle chase** that
  holds through the grow leg then advances one full 360° revolution through the
  shrink leg — the tail races a lap to catch the head (drawn
  `startAngle = rotation + startAdvance`). The advance must stay a multiple of
  360°: it snaps back to 0 at each cycle seam (RepeatMode.Restart), and only a
  full revolution is continuous mod 360 there — anything else teleports the arc
  at the seam, right at the 30° sweep minimum where it is most visible. The motion
  constants (named `SPINNER_*`) **approximate the Material feel — not sourced iOS
  values**; verify on the demo (a static `@Preview` shows one instant only).
  Optional 12sp `caption`/secondary label below at `spacing.md` gap; content
  padding horizontal `spacing.xl`, vertical `spacing.lg`.
- **`AMProgressBar(progress, label?, valueLabel?)`** — determinate. **Full-bleed
  `Surface.primary` row, NOT card-framed** (unlike the spinner), padded by
  `tokens.listRowInsets` (14dp h / 16dp v). Track: **6dp** tall (iOS
  `barHeight` — slimmer than any spacing token so it reads as an indicator),
  `AMTheme.separator` (the same quiet channel the divider/skeleton/timeline use;
  iOS fills the track with `IMTheme.separator`), 3dp corner radius (capsule);
  fill `tokens.semantic.accent` (monochrome by default — the library-wide
  translation of iOS `Color.accentColor`) at `width × progress`. `progress` is
  `coerceIn(0f, 1f)`. Header row appears
  only when `label` or `valueLabel` is non-null: label `AMTheme.Type.title` primary
  with `weight(1f)`, valueLabel `AMTheme.Type.secondary` trailing.

## AMRowContent — `LabeledContent.kt`

**The composable is `AMRowContent`, not `AMLabeledContent`** — the filename
keeps the iOS concept name; the API drops "LabeledContent" because Compose has
no `LabeledContentStyle` injection. Full-bleed, non-tappable label/value row
(the visual cousin of `AMListRow`): `fillMaxWidth` Row on `Surface.primary`,
padded `tokens.listRowInsets`, `spacedBy(spacing.md)`, vertically centered.
Designated overload takes `label` + `content` slots — label sits in a
`weight(1f)` Box (fills leading width), content is intrinsic trailing. String
overload: label `AMTheme.Type.title` `Text.primary`, value `AMTheme.Type.body`
`Text.secondary`. Stack rows with `AMDivider(inset = AMTheme.Input.inset.dp)`
between them.

## AMTimeline — `Timeline.kt`

Vertical activity rail: markers connected by a continuous line, content beside
each. Draws **no surface fill** — wrap in `AMCard(padding = AMPadding.block)`
for the elevated treatment. Bespoke private constants (NOT tokens): dot 10dp,
line 2dp (heavier than hairline so the rail reads), first-line center 11dp
(half a ~22sp headline line), marker gap 2dp, title/subtitle spacing 2dp
(matches AMListRow).

- **`AMTimelinePosition`** `@Immutable` enum: `FIRST`/`MIDDLE`/`LAST`/`ONLY` with
  internal `hasLineAbove` (MIDDLE, LAST) / `hasLineBelow` (FIRST, MIDDLE).
- **`AMTimelineRow(position = MIDDLE, marker, content)`** — layout recipe
  (load-bearing): `Row(Modifier.height(IntrinsicSize.Max))`, top-aligned,
  `spacedBy(spacing.md)`. Rail column (`fillMaxHeight`): top connector with
  fixed height `markerTopInset` → 2dp gap → marker → 2dp gap → bottom
  connector with `weight(1f)` (fills through the content's bottom padding so
  the line bridges into the next row). `markerTopInset =
  max(0.dp, 11.dp − markerHeight/2 − 2.dp)`; marker height is measured via
  `onSizeChanged` into a `remember`ed `mutableIntStateOf` that defaults to
  the 10dp dot size — the standard dot is correct on the first frame,
  non-dot markers refine after one recompose. The clamp to 0 keeps tall
  markers (avatars) flush at the row top instead of sagging below the text.
  Hidden connectors render `Color.Transparent` (not removed) so geometry is
  identical across positions. Content column gets
  `padding(bottom = spacing.lg)` **only** when `hasLineBelow`.
- Convenience overloads: default-dot `(position, tint, content)` and string
  `(title, subtitle?, position, tint)` — title `AMTheme.Type.title` primary,
  subtitle `AMTheme.Type.caption` secondary via the public `AMTimelineRowLabel`.
- **`AMTimelineDot(tint = AMTheme.selectionRing.resolve())`** — 10dp Canvas
  circle, `invisibleToUser` (decorative); pass `tokens.semantic.*` for status
  rows. Markers within one timeline should share a width so the rail stays
  straight (the rail column centers on the widest marker).
- **`AMTimeline<T>(data, marker, content)`** container derives each row's
  position automatically (`size == 1` → ONLY, index 0 → FIRST, last → LAST,
  else MIDDLE); rows stack in a plain Column with `spacedBy(0.dp)` — breathing
  room comes from each row's bottom padding. There is **no `key` parameter**:
  rows are positional (fine — it is a plain Column, not a LazyColumn).
  Default-marker overload omits `marker`.

## AMSkeleton + Modifier.amShimmer — `Skeleton.kt`

**`Modifier.amShimmer()`** (public, `@Composable`) is the one shimmer: a white
horizontal-gradient band sweeping left-to-right, **additively brightened and
masked to whatever the content draws** — the Compose analog of iOS's
`.plusLighter` gleam clipped with `.mask(content)`. `drawWithContent` runs a
**two-pass draw**: (1) `drawContent()` paints the content as the base; (2) a
transparency layer is opened via `drawContext.canvas.saveLayer` with a
`BlendMode.Plus` `Paint`, and inside it the content is drawn **again** (supplying
the destination alpha) then the white band is drawn with **`BlendMode.SrcIn`** so
the gleam is clipped to the content's own alpha; on `restore()` the masked gleam
is `Plus`-added onto the base. The second `drawContent()` captures the silhouette
so the additive-and-masked gleam works over an **arbitrary placeholder tree**
without the modifier knowing the shape (no separate offscreen `graphicsLayer` is
needed — the `saveLayer` is the isolation, and `Plus` with premultiplied alpha 0
leaves the transparent gaps untouched). Band peak α **0.55** (white), sweeps
fully-off-left → fully-off-right every **1200ms** linear, band width `max(40% of
container, 60dp)`. These constants are **hardcoded, deliberately not tokens** —
the loading-affordance rhythm must not drift when an app re-tunes motion/color.
Driven by `rememberInfiniteTransition`, so the sweep stops when the content
leaves the tree. The phase-independent gleam objects (the `BlendMode.Plus` layer
`Paint` and the white gradient color-stop array) are `remember`ed once and passed
into `drawShimmerGleam`; only the phase-dependent band `startX`/`endX` are
recomputed per frame.

- **Reduce Motion** (`LocalAMReduceMotion.current`): renders **static** — the
  modifier returns `this` *before* `rememberInfiniteTransition` is called, so no
  animation object is allocated at all (like iOS, which skips its `TimelineView`
  timer). The placeholder shapes still show; only the sweep is gone.
- **Dark mode:** `Plus` *adds* the band toward white by up to 0.55 (masked to the
  content silhouette); on the near-black dark `Surface.page` a translucent
  `separator` base lifts to a clearly lighter grey under the band, staying legible
  in both schemes.
- **Preferred loading pattern** (Compose has no `.redacted()`): **shimmer a real
  placeholder tree** — build the shape of the finished layout from real
  composables filled with a `separator` wash and wrap ONE `amShimmer()` around
  the whole tree, so a single sweep tracks the entire silhouette (avatar + text
  bars together) with the gaps untouched. Reach for the `AMSkeleton` primitives
  when a bespoke arrangement has no real tree to mirror.

**`object AMSkeleton`** — three `@Composable` factory functions (call as
`AMSkeleton.rect()` etc., the analog of the iOS enum-with-static-builders). Each
renders through the private `AMSkeletonShape` (a `separator`-filled shape +
`amShimmer()`), so there is exactly one shimmer path — the opaque fill fills the
whole shape, so the sweep covers the whole shape with no gaps to mask:

- `rect(width? = null, height = 14.dp, cornerRadius? = null)` — null width
  fills the parent; null radius uses `tokens.radius.sm` (8dp).
- `circle(diameter = 40.dp)`.
- `text(lines = 3, lineHeight = 12.dp, spacing? = null)` — capsule rows
  (`RoundedCornerShape(50)`), null spacing uses `tokens.spacing.sm` (8dp);
  the **last line shortens to 60% width only when `lines > 1`** so the block
  reads as text. One outer `BoxWithConstraints` for the whole block.

## See also

- `conventions.md` — AM/am prefix, slot + String-overload pattern, param
  order, no-Material3 / amRipple press-feedback rules, preview/KDoc policy.
- `build.md` — Coil 3 and material-icons-core dependency decisions, test
  policy and Gradle commands.
