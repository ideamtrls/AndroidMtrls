---
scope: Option-choosing primitives — segmented control, menu picker, chip groups and the flow layout, the month calendar, and selection marks — the library's selection/ folder and the Selection catalog pages
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/selection/**, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/selection/**, androidmtrls/src/main/res/drawable/am_ic_check.xml, androidmtrls/src/main/res/drawable/am_ic_arrow_drop_down.xml, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/selection/**]
keywords: [AMSegmentedControl, SingleChoiceSegmentedButtonRow, SegmentedButton, itemShape, check icon, AMMenuPicker, ListItem, RadioButton, selectableGroup, spotlight, AMSpotlight, arrow_drop_down, AMChipGroup, FilterChip, check mark, leadingIcon, FilterChipDefaults.IconSize, multi-select, single choice, selection, onSelectionChange, selected, onSelectedChange, named arguments, AMFlowLayout, FlowRow, FlowRowScope, spacing, lineSpacing, touch target, AMCalendar, YearMonth, LocalDate, month grid, calendarMonthCells, WeekFields, firstDayOfWeek, markers, event dots, onHold, amHoldable, today, primary, secondaryContainer, tabular digits, tnum, font scale cap, MaxTextScale, TalkBack, contentDescription, AMSelectionMark, Modifier.amSelectionCheck, check glyph, selection ring, selectionRingWidth, AMElevation.Text, ShapeShadows, GraphicsLayer, vectorResource, toggleable, resolveEnabled, SelectionPages, CalendarMonthCellsTest]
updated: 2026-11-10
---

# Selection

Choosing among options: `AMSegmentedControl` (Material's single-choice segmented buttons), `AMMenuPicker` (a row whose options come up in the spotlight as radio-button list items), `AMChipGroup` (Material filter chips, many choices or one) on `AMFlowLayout` (Compose's `FlowRow` at the library's spacing), `AMCalendar` (a month grid with today, the selection and event dots, custom in Material's colors) and `AMSelectionMark` / `Modifier.amSelectionCheck` (the corner check of a selectable photo tile). The controls keep Material's look and colors; every one takes `enabled` and honors a disabling container (`resolveEnabled`, `Actions.md`).

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/selection/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `SegmentedControl.kt` | `AMSegmentedControl` |
| `MenuPicker.kt` | `AMMenuPicker` |
| `ChipGroup.kt` | `AMChipGroup` (many choices, one choice); private `ChipFlow`, `ChipGlyph` |
| `FlowLayout.kt` | `AMFlowLayout` |
| `Calendar.kt` | `AMCalendar`; internal `calendarMonthCells`; private `WeekdayHeader`, `DayCell`, `TextStyle.capped` |
| `SelectionMark.kt` | `AMSelectionMark`, `Modifier.amSelectionCheck`; private `SelectionMarkElement`, `SelectionMarkNode`, `CheckGlyph` |

- Glyphs: `androidmtrls/src/main/res/drawable/am_ic_check.xml` (a chosen chip's check, the selection mark's check) and `am_ic_arrow_drop_down.xml` (the menu picker's arrow).
- Test: `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/selection/CalendarMonthCellsTest.kt` (the month grid's layout, `calendarMonthCells`).
- Pieces from other areas it builds on: `AMSpotlight` (`Overlays.md`), `Modifier.amHoldable` (`Overlays.md`), `AMRowDefaults` (`Layout.md`), `resolveEnabled` (`foundations/Enabled.kt`), `ShapeShadows`, `Style.Shadow.Text`, `Style.SelectionRing.Width`, `drawInsetOutline` and `LocalAMTokens` (`Foundations.md`).
- Catalog pages: `demo/.../catalog/pages/selection/` (below).

## Naming the selection

The controls share two spellings, Compose's value-and-callback pair:

- **One choice**: `selected: T` with `onSelectedChange: (T) -> Unit` (`AMSegmentedControl`, `AMMenuPicker`, the one-choice `AMChipGroup`, and `AMCalendar` with a `LocalDate`). Re-picking the current option doesn't call back.
- **Many choices**: `selection: Set<T>` with `onSelectionChange: (Set<T>) -> Unit` (the many-choice `AMChipGroup`).
- Options are plain values (`options: List<T>` / `items: List<T>`, each distinct, compared with `==`) and `label: (T) -> String` says what a segment, chip or row shows for one.
- The two `AMChipGroup` overloads differ only in these names, so call them with named arguments (`selection =`, `selected =`): passed by position, a `Set` fits both.

## `AMSegmentedControl` (`SegmentedControl.kt`)

```kotlin
AMSegmentedControl(Range.entries, label = { it.title }, selected = range, onSelectedChange = { range = it }, modifier = Modifier.fillMaxWidth())
```

`AMSegmentedControl(options, label, selected, onSelectedChange, modifier, enabled)`: Material's `SingleChoiceSegmentedButtonRow` of `SegmentedButton`s, one per option, shaped by `SegmentedButtonDefaults.itemShape(index, count)`, with Material's default icon (the check on the chosen segment), colors, ripple and radio-group semantics. Labels are one line, ellipsized. Segments share the widest one's width and the row hugs them; `Modifier.fillMaxWidth()` spreads them across the width. No surface of its own: it goes on a card or the page bare (a card around it frames the outline twice).

## `AMMenuPicker` (`MenuPicker.kt`)

```kotlin
AMMenuPicker("Sort by", listOf("Newest", "Oldest", "A–Z"), label = { it }, selected = sort, onSelectedChange = { sort = it })
```

`AMMenuPicker(title, options, label, selected, onSelectedChange, modifier, icon: Painter? = null, enabled)`. One form: its content is fixed (glyph, title, value).

- **The row**: Material's clickable `ListItem` with `AMRowDefaults` (colors, shapes, content padding: the row recipe, 80 dp for one line). Leading: the optional `icon`. Headline: `title`. Trailing: the current option's label in `bodyMedium` and `am_ic_arrow_drop_down`, `spacing.xs` apart, both in the list item's trailing color (`onSurfaceVariant`; Material's disabled color when disabled). The whole row takes the tap with ripple, and TalkBack reads the title and value together. A flat row: callers put it in a card or on a surface strip, `AMDivider(inset = spacing.lg)` between rows.
- **The options**: a tap sets `choosing` (`rememberSaveable`) and `AMSpotlight(choosing, title = title)` comes up (`Overlays.md`): a `selectableGroup` column of selectable `ListItem`s on the row recipe, each led by a Material `RadioButton` (`onClick = null`, the row takes the tap), the current one on. Tapping an option calls `onSelectedChange` (unless it's already current) and closes; back and a tap outside close it unchanged.

## `AMChipGroup` (`ChipGroup.kt`)

```kotlin
AMChipGroup(Genres, label = { it }, selection = picked, onSelectionChange = { picked = it })
AMChipGroup(Cities, label = { it }, selected = city, onSelectedChange = { city = it })
AMChipGroup(Moods, label = { it.title }, selection = moods, onSelectionChange = { moods = it }, icon = { painterResource(it.icon) })
```

- `AMChipGroup(items, label, selection: Set<T>, onSelectionChange, modifier, icon, spacing, enabled)`: a tap toggles the item in or out.
- `AMChipGroup(items, label, selected: T, onSelectedChange, modifier, icon, spacing, enabled)`: a tap chooses the item; tapping the chosen one keeps it chosen. Its flow carries `selectableGroup()`.
- Both draw through private `ChipFlow`: one Material `FilterChip` per item (its look, colors, ripple and checkbox role untouched), label as `Text(label(item))`. The leading glyph, at `FilterChipDefaults.IconSize`: `am_ic_check` on a chosen chip, Material's filter-chip rule (it replaces the item's own glyph); otherwise `icon(item)` when an `icon` is given; otherwise none.
- `icon: (@Composable (T) -> Painter)? = null`, so a call can use `painterResource`.
- The chips wrap in `AMFlowLayout(spacing = spacing, lineSpacing = 0.dp)`: `spacing.sm` between neighbors by default; lines stack touch target to touch target, since each chip's 48 dp target already reaches 8 dp past its 32 dp face, leaving 16 dp between the faces of two lines.
- No surface of its own: on the page, or in a card padded `spacing.lg`.

## `AMFlowLayout` (`FlowLayout.kt`)

`AMFlowLayout(modifier, spacing = spacing.sm, lineSpacing = spacing, content: FlowRowScope)`: Compose's `FlowRow` with `Arrangement.spacedBy(spacing)` across and `spacedBy(lineSpacing)` down; children top-aligned in their line (`FlowRow`'s default). A child wider than the layout is measured at the layout's width, so text wraps. It hugs its widest line; `fillMaxWidth()` takes the width with the children at the start. For runs of tags and pills; `AMChipGroup` builds on it.

## `AMCalendar` (`Calendar.kt`)

```kotlin
AMCalendar(YearMonth.from(day), selected = day, onSelectedChange = { day = it }, markers = { events.countOn(it) }, onHold = { peek = it })
```

`AMCalendar(month: YearMonth, selected: LocalDate, onSelectedChange, modifier, markers: (LocalDate) -> Int = { 0 }, onHold: ((LocalDate) -> Unit)? = null, enabled)`. Custom: Android has no month-at-a-glance grid, so it's drawn in the scheme's colors and type.

- **The month is the caller's.** No month title, no paging controls, no swipe: a screen pages by passing another `month`. Days outside it are blank (no spillover numbers); a `selected` day outside it shows no highlight.
- **Layout.** A column filling the width, padded `spacing.lg` at the sides and `spacing.md` above and below (flat: it goes on a surface strip or in an unpadded card): the weekday header, `spacing.sm`, then week rows `spacing.xs` apart, seven equal columns each. `calendarMonthCells(month, firstDayOfWeek)` lays the month out: nulls back to the week's start, the days, nulls to the week's end, so 28 to 42 cells (four to six weeks; unit-tested).
- **Locale.** The configuration's first locale (`ConfigurationCompat.getLocales`): the week's first day from `WeekFields.of(locale)`, weekday names `TextStyle.SHORT` (spoken `FULL`), each day spoken as `FormatStyle.FULL`. Today is `LocalDate.now()`.
- **A day** (`DayCell`): a 48 dp tall cell (`minimumInteractiveComponentSize` too) holding a 36 dp circle with the number and, 2 dp below, three 4 dp dot slots 3 dp apart, so numbers line up across rows. Today: `primary` circle, `onPrimary` number at `FontWeight.Medium`. Selected: `secondaryContainer` circle, `onSecondaryContainer` number; a selected today shows today's circle alone. Others: no circle, `onSurface`. Dots: `primary`, `min(markers(day), 3)` of them. Disabled: the circles, numbers and dots at 0.38 alpha; the weekday header keeps its color.
- **Input.** The cell is `selectable` (indication off) and the ripple draws on the circle through a shared `MutableInteractionSource`. `Modifier.amHoldable(enabled = enabled && onHold != null)` sits on the cell, so a hold calls `onHold(day)` with the long-press haptic and doesn't select, a tap selects, and a drag that starts on a day still scrolls; TalkBack gets the hold as the day's long-press action.
- **TalkBack.** The cell's description is the full date, then "Today" and "1 event" / "N events" (uncapped) where they apply, joined by ", "; `selectable` adds the selected state. The circle's own text is cleared, and the weekday header reads full weekday names.
- **Type.** Numbers `bodyLarge` with `fontFeatureSettings = "tnum"`; weekday names `bodySmall` in `onSurfaceVariant`, one line. Both go through `TextStyle.capped()`: font size and line height follow the font scale (nonlinear, the platform's) up to 1.3 times their default size and stop there (`MaxTextScale`), where a two-digit day still clears its circle.
- `markers` runs for each day on every composition; keep it cheap.

## `AMSelectionMark` and `Modifier.amSelectionCheck` (`SelectionMark.kt`)

```kotlin
AMRemoteImage(url, contentDescription = "Photo 3", modifier = Modifier.aspectRatio(1f).toggleable(value = picked, role = Role.Checkbox) { … }.amSelectionCheck(picked))
```

- **The mark**: a 20 dp black disc, Material's `check` glyph in white inside it (its 960-unit viewport drawn in a box 0.7 of the disc), a white ring `selectionRingWidth` (1.5 dp) wide drawn inside the disc's edge (`drawInsetOutline`), and the `Text` shadow cast by the disc (`ShapeShadows`, `Style.Shadow.Text`). The same in light and dark mode: it sits on a picture. Fixed size whatever the font scale.
- **`AMSelectionMark(modifier)`**: a 20 dp `Spacer` that draws the mark and says "Selected" (`clearAndSetSemantics`).
- **`Modifier.amSelectionCheck(selected)`**: draws the content, then, while `selected`, the mark at the top end corner (start-aware) inset `spacing.xs` (read from `LocalAMTokens` in the node). Nothing otherwise; bind it to live state. It adds no semantics: the tile announces its own state (`toggleable` / `selectable`). Place it after the tile's clip and click, so the mark sits inside the corners and over the ripple.
- **Drawing** (`SelectionMarkNode`, both forms): the mark is recorded into a `GraphicsLayer` of its own size (so `ShapeShadows` casts from the disc), then drawn at the corner or centered. `CheckGlyph` loads `am_ic_check` once through `ImageVector.vectorResource(theme, resources, id)` and flattens its paths (`List<PathNode>.toPath()`), shared by every mark.

## Catalog pages

`demo/.../catalog/pages/selection/`, registered in `SelectionPages.entries` in this order (ids): `segmentedControl`, `menuPicker`, `chipGroup`, `selectionCheck`, `calendar`.

| Page | Preview | Loaded |
|---|---|---|
| Segmented control | Day, Week, Month, full width | Mine, Shared, Archived over a card of the chosen scope's documents (clickable `ListItem`s: title over when it was edited, who shared it, or when it was archived) |
| Menu picker | `FullBleed`: "Sort by" (Newest, Oldest, A–Z) on a surface strip | a card: "Streaming quality" with `ic_cell_tower` (Low, Standard, High), "Sleep timer" (Off to 1 hour), "Download quality" disabled at High |
| Chip group | eight genres, Jazz and Electronic chosen | a card padded `spacing.lg`: one city (All, Brooklyn, Kyoto, Lisbon, Reykjavík), a full-width divider, then any moods with glyphs (Quiet, Outdoors, Late night, Good coffee, Live music) |
| Selection check | three 96 dp photos in `shapes.medium`, the first and last marked | a 3 × 3 grid of square photos `spacing.sm` apart, each `toggleable` (checkbox role) and marked while picked |
| Calendar | `FullBleed`: this month on a surface strip, dots on six days | the month in a card over a card of the selected day's events (a clear day shows the "Nothing scheduled" / "No events on this day." `AMEmptyState` on the page, padded `spacing.lg`); holding a day brings up its events in the spotlight under its name ("Saturday, November 7", `getBestDateTimePattern`) with Add event and Open day, which selects it |

- Photos come from `catalogPhoto` (seeds `selection-tile-<n>`, `selection-picker-<n>`) through `AMRemoteImage`.
- Demo glyphs these pages add: `ic_view_week`, `ic_arrow_drop_down_circle`, `ic_label`, `ic_check_circle`, `ic_calendar_month` (entry icons), `ic_cell_tower`, `ic_eco`, `ic_coffee`, `ic_calendar_add_on`, `ic_event_available`. They also use `ic_light_mode`, `ic_dark_mode`, `ic_music_note` and `ic_calendar_today`.
