---
scope: The data-entry primitives — text, secure and search fields, the text editor, toggles, sliders, steppers and the date picker — the library's inputs/ folder and the Inputs catalog pages
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/inputs/**, androidmtrls/src/main/res/drawable/am_ic_cancel.xml, androidmtrls/src/main/res/drawable/am_ic_visibility.xml, androidmtrls/src/main/res/drawable/am_ic_visibility_off.xml, androidmtrls/src/main/res/drawable/am_ic_search.xml, androidmtrls/src/main/res/drawable/am_ic_remove.xml, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/inputs/**]
keywords: [AMTextField, AMSecureField, AMTextEditor, AMSearchField, AMTextFieldAutofocus, Off, CaretAtEnd, CaretAtStart, autofocus, caret at start, OutlinedTextField, OutlinedSecureTextField, TextFieldState, rememberTextFieldState, InputTransformation, inputTransformation, maxLength, filtering input, digits only, label, placeholder, supportingText, isError, KeyboardOptions, KeyboardActionHandler, ImeAction, Next, Done, clear button, cancel, InputClearButton, inputAutofocus, FocusRequester, LocalWindowInfo, isWindowFocused, TextObfuscationMode, RevealLastTyped, Visible, reveal, visibility, visibility_off, password keyboard, merge, TextFieldLineLimits, MultiLine, minLines, maxLines, SearchBarDefaults.InputField, SearchBarState, rememberSearchBarState, animateToCollapsed, NavigationBackHandler, back closes, keyboard back, AMToggle, Switch, ListItem, AMRowDefaults, AMSlider, AMSliderDecoration, Icon, Text, SliderState, stateDescription, contentDescription, onValueChangeFinished, drag end, seek, AMStepper, FilledTonalIconButton, remove, add, valueRange, step, AMDatePicker, AMDatePickerComponents, Date, Time, DateAndTime, LocalDateTime, dateRange, AssistChip, FlowRow, DatePicker, TimePicker, clock dial, DatePickerSpotlightPart, DatePickerSwitchRow, Time row, Date row, no keyboard, SelectableDates, DatePickerDefaults, YearRange, UTC midnight, spotlight, AMSpotlight, InputControlRow, row recipe, amRowPadding, large text, stacked control, resolveEnabled, LocalAMEnabled, TextFieldPage, SecureFieldPage, SearchFieldPage, TextEditorPage, SliderPage, StepperPage, TogglePage, DatePickerPage, InputsPages]
updated: 2026-11-10
---

# Inputs

The data-entry primitives. Text goes in on Material's state-based fields: `AMTextField`, `AMSecureField` and `AMTextEditor` (outlined fields named by their own floating labels) and `AMSearchField` (Material's search input field), all quiet until tapped (`AMTextFieldAutofocus`). Values are set on rows that follow the row recipe: `AMToggle` (a list item with a `Switch`), `AMSlider`, `AMStepper` (two tonal icon buttons) and `AMDatePicker` (value chips that open Material's calendar or clock dial in the spotlight). Every control keeps Material's own shapes and colors, takes `enabled` through `resolveEnabled`, and draws no surface: a card supplies it.

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `inputs/TextField.kt` | `AMTextFieldAutofocus`, `AMTextField` |
| `inputs/SecureField.kt` | `AMSecureField`; private reveal labels, `PasswordKeyboard` |
| `inputs/SearchField.kt` | `AMSearchField` |
| `inputs/TextEditor.kt` | `AMTextEditor`; private `SentenceKeyboard` |
| `inputs/Toggle.kt` | `AMToggle` (string and slot forms) |
| `inputs/Slider.kt` | `AMSliderDecoration` (`Icon`, `Text`), `AMSlider`; private `SliderEnd` |
| `inputs/Stepper.kt` | `AMStepper` (string and slot forms); private `StepperButton` |
| `inputs/DatePicker.kt` | `AMDatePickerComponents`, `AMDatePicker`; private `DatePickerSpotlightPart`, chip formatters, the spotlight content and `DatePickerSwitchRow`, `DatePickerDayRange`, the UTC-day conversions |
| `inputs/InputSupport.kt` | internal `InputClearLabel`, `InputDisabledAlpha`, `InputClearButton`, `inputAutofocus`, `InputControlRow` |

- Glyphs (`androidmtrls/src/main/res/drawable/`): `am_ic_cancel` (the filled `cancel`, the clear button), `am_ic_visibility` and `am_ic_visibility_off` (the password eye), `am_ic_search`, `am_ic_remove` (the stepper's minus). The stepper's plus is the Actions area's `am_ic_add`.
- No string resources: the words the area says ("Clear", "Show password", "Hide password", "Decrease", "Increase", "Time", "Date", the defaults "Password" and "Search") are constants in the files.
- Pieces from other areas it builds on: `resolveEnabled` (`foundations/Enabled.kt`, `Actions.md`), `Modifier.amRowPadding` and `AMRowDefaults` (`Layout.md`), `AMSpotlight` (`Overlays.md`), `AMTheme.tokens.spacing` (`Foundations.md`); `navigationevent-compose`'s `NavigationBackHandler` for the search field's back.
- Catalog pages: `demo/.../catalog/pages/inputs/` (below).

## Two kinds of input

- **Text inputs are blocks.** `AMTextField`, `AMSecureField`, `AMTextEditor` and `AMSearchField` are Material's field and nothing else: no padding of their own, and they fill the width they're offered (`fillMaxWidth()` after the caller's modifier, so a size modifier there narrows them). Material's outline (or the search field's filled container) is the native look, not an AM surface. Put them in a card padded `spacing.lg` (`Modifier.amCard(padding = PaddingValues(spacing.lg))`), several stacked `spacing.md` apart; an outlined field leaves room above itself for its floating label.
- **Value rows are rows.** `AMToggle`, `AMSlider`, `AMStepper` and `AMDatePicker` follow the row recipe (28 dp above and below, 16 dp at the sides) and draw no fill: stack them bare in `Modifier.amCard()` (no padding) with `AMDivider(inset = spacing.lg)` between, or on a surface strip. They stay flat in every state; the control shows the state in Material's look.

## Text inputs

### Shared behavior

- **State** is a `TextFieldState` (`rememberTextFieldState()`), hoisted, the state-based Material field. The caller reads `state.text`.
- **Quiet by default — `AMTextFieldAutofocus`**: `Off` (default) leaves the field alone until tapped; `CaretAtEnd` and `CaretAtStart` focus it as it enters the composition and put the caret after or before the text it holds (the start for an editor whose value begins with what the user came to change, a web address). One enum, so "caret at the start without focus" can't be written. Internal `inputAutofocus(state, autofocus)` returns `Modifier.focusRequester` and a `LaunchedEffect` that waits until its window has focus (`LocalWindowInfo.isWindowFocused`), requests focus, then sets the selection in `state.edit { }` (`TextRange.Zero` or `placeCursorAtEnd()`). A tap puts the caret where the finger lands, Android's way. The wait matters in a sheet: focused before the sheet's window has focus, the keyboard would belong to the window underneath, and back would close the sheet and the keyboard together instead of the keyboard first.
- **Moving through a form** is the keyboard's: `keyboardOptions` with `ImeAction.Next` on each field and `Done` on the last, `onKeyboardAction` null, so the action key moves focus down and finally closes the keyboard. Code moves focus with a `Modifier.focusRequester` passed in `modifier`; there is no AM focus binding.
- **Filtering input — `inputTransformation`** (`AMTextField`, `AMSecureField`, `AMTextEditor`; null by default): Material's own `InputTransformation`, passed straight to the field. It runs on each edit the user makes (typing, pasting, the keyboard's suggestions) before the field takes it, so a character it drops never shows, not even for a frame: `InputTransformation.maxLength(n)` caps the length, a custom one (`InputTransformation { … }` over the `TextFieldBuffer`, `replace` or `revertAllChanges`) keeps digits only. Text the app writes to the state itself (`setTextAndPlaceCursorAtEnd`, `clearText`, the clear button, autofocus's caret) isn't filtered. `AMSearchField` has none: a query takes whatever is typed.
- **The clear button** (`InputClearButton`): Material's `IconButton` in the field's trailing slot with `am_ic_cancel`, "Clear", in Material's trailing icon color (`onSurfaceVariant`), emptying the state with `clearText()`. Shown while the field holds text and is enabled (`derivedStateOf { state.text.isNotEmpty() }`, so typing doesn't recompose the caller). `AMTextField` and `AMSearchField` have it, `AMSecureField` when its eye is off; `AMTextEditor` has none.
- **TalkBack** reads Material's semantics: the label as the field's name, then its text; the search field as a search field with its placeholder.
- **Disabled** (`resolveEnabled(enabled)`): Material's disabled field, no clear button.

### `AMTextField` (`TextField.kt`)

`AMTextField(state, label, modifier, placeholder: String?, supportingText: String?, isError, autofocus, inputTransformation: InputTransformation?, keyboardOptions, onKeyboardAction: KeyboardActionHandler?, enabled)`: Material's `OutlinedTextField` with `TextFieldLineLimits.SingleLine`, the label as `Text(label)`, the placeholder (shown while focused and empty), the clear button, and Material's supporting line and error state.

### `AMSecureField` (`SecureField.kt`)

`AMSecureField(state, modifier, label = "Password", placeholder, supportingText, isError, showRevealButton = true, autofocus, inputTransformation, keyboardOptions, onKeyboardAction, enabled)`: Material's `OutlinedSecureTextField`.

- **The eye** (`showRevealButton`): an `IconButton` in the trailing slot, `am_ic_visibility` "Show password" while hidden, `am_ic_visibility_off` "Hide password" while shown, toggling a `rememberSaveable` flag; `textObfuscationMode` is `Visible` while revealed, `RevealLastTyped` (each character shows briefly as typed) otherwise. Disabled with the field. With `showRevealButton = false` the slot holds the clear button instead.
- **Keyboard**: `PasswordKeyboard` (`KeyboardType.Password`, no autocorrection) `.merge(keyboardOptions)`, so the caller's specified options (an action key) win and the rest stay the password keyboard's.
- A mismatched confirmation shows in the field itself: `supportingText` plus `isError`.

### `AMTextEditor` (`TextEditor.kt`)

`AMTextEditor(state, label, modifier, placeholder, minLines = 3, maxLines = 8, autofocus, inputTransformation, enabled)`: Material's `OutlinedTextField` with `TextFieldLineLimits.MultiLine(minLines, maxLines)` (Material requires `1 ≤ minLines ≤ maxLines`): it starts `minLines` tall, grows with its text, and scrolls inside past `maxLines`. Enter makes a new line; the keyboard capitalizes sentences (`SentenceKeyboard`). No clear button.

### `AMSearchField` (`SearchField.kt`)

`AMSearchField(state, modifier, placeholder = "Search", autofocus, onSearch: (String) -> Unit = {}, enabled)`: Material's `SearchBarDefaults.InputField` on the state (`textFieldState`) and its own `rememberSearchBarState()`, with `am_ic_search` leading (no description) and the clear button trailing.

- **Material's look**: the search field's filled container (`surfaceContainerHigh`) in its full-round shape, 56 dp tall. Its keyboard action is Search, calling `onSearch` with the query.
- **Open while focused.** Material's field expands its bar state when focused or tapped, and clears its focus once the state collapses. The field collapses the state itself (`snapTo(0f)`) when focus leaves some other way (`onFocusChanged`), and on back.
- **Back closes it**: a `NavigationBackHandler`, enabled while the bar state's target is `Expanded` and the keyboard is down (`WindowInsets.ime` bottom at 0, read through `derivedStateOf`), calls `animateToCollapsed()`. With the keyboard up it stands aside, so the first back hides the keyboard and the next closes the field, before back leaves the screen. The query stays. There is no Cancel button.

## Value rows

### `InputControlRow` (`InputSupport.kt`, internal)

The labeled row `AMStepper` and `AMDatePicker` share: a multi-content `Layout` inside `Modifier.amRowPadding()`. The label is a `Row` (parts `spacing.sm` apart, `bodyLarge` through `ProvideTextStyle`, `onSurface` through `LocalContentColor`, at 0.38 alpha when disabled); the control is one layout.

- **Side by side** when the label's longest word plus `spacing.md` plus the control's widest (its max intrinsic width) fits: the control at its own width at the end, the label in the rest (wrapping), both centered vertically.
- **Stacked** otherwise (large text): the label across the row, then the control under it, `spacing.sm` below, start-aligned and offered the full width so it can wrap.

### `AMToggle` (`Toggle.kt`)

- String form `(title, checked, onCheckedChange, modifier, supportingText: String?, enabled)`; slot form `(checked, onCheckedChange, modifier, supportingContent: slot?, enabled, label: slot)`. The string form delegates.
- Material's toggleable `ListItem(checked, onCheckedChange, …)` with `trailingContent = Switch(checked, onCheckedChange = null, enabled)` and all three of `AMRowDefaults` (colors, shapes, content padding): the whole row is the toggle with Material's ripple, the switch keeps Material's look and colors, and the row never tints when checked, pressed or disabled. The switch stays at the end at every text size; the label wraps.
- TalkBack reads one switch: the label, then on or off. Disabled, Material's list item greys the label and the switch.

### `AMSlider` (`Slider.kt`)

- `AMSlider(value, onValueChange, modifier, title: String?, leading: AMSliderDecoration?, trailing: AMSliderDecoration?, contentDescription: String?, stateDescription: String?, valueRange = 0f..1f, onValueChangeFinished: (() -> Unit)? = null, enabled)`.
- **Arrangement**: a `Column` inside `amRowPadding()`, `spacing.sm` apart: the optional title (`bodyLarge`, `onSurface`), then a `Row` of the leading end, Material's `Slider` (weighted) and the trailing end, `spacing.sm` apart, centered.
- **`AMSliderDecoration`**, `@Immutable` sealed interface, one value per end so an end can't hold both: `Icon(icon: Painter)` (24 dp, `onSurfaceVariant`) and `Text(text: String)` (`bodySmall` with tabular figures, `onSurfaceVariant`, one line). Disabled ends take `onSurface` at 0.38.
- **Material's slider** on a `SliderState` built from `value` and `valueRange` (`remember(valueRange)`), its `value` synced from the caller's in a `SideEffect`; moves come back through `onValueChange` (the state-based `Slider(state, onValueChange, …)`), and `onValueChangeFinished` goes straight to Material's slider, which calls it when a change ends (the finger lifts after a drag or a tap): a scrubber seeks its stream there instead of on every move.
- **TalkBack reads one element, the slider**: `semantics { contentDescription = contentDescription ?: title; stateDescription }`, so a scrubber says "1:02 of 3:43" instead of a percentage. The visible title and the ends are `clearAndSetSemantics {}`.

### `AMStepper` (`Stepper.kt`)

- String form `(title, value, onValueChange, valueRange: IntRange, modifier, step = 1, enabled)`, whose title also names the buttons; slot form `(value, onValueChange, valueRange, modifier, contentDescription: String?, step, enabled, label: RowScope slot)`. The string form delegates. `step` must be at least 1.
- **On `InputControlRow`**: the label, then a `Row` of two Material `FilledTonalIconButton`s in `MaterialTheme.shapes.medium` (rounded rectangles, never circles), `am_ic_remove` and `am_ic_add`, each with Material's 48 dp target. A tap moves the value by `step`, clamped into `valueRange` (computed in `Long`, so it never overflows); minus is disabled at `valueRange.first`, plus at `valueRange.last`.
- **The value isn't shown**; a caller that needs it puts it in the label ("Breakfast × 2").
- **TalkBack**: each button is "Decrease" or "Increase" followed by the name (`"$action $contentDescription"`), with the value as its `stateDescription`. Hide label parts that repeat the name.
- Disabled: the label greys (0.38) and both buttons take Material's disabled colors.

### `AMDatePicker` (`DatePicker.kt`)

- `AMDatePicker(title, value: LocalDateTime, onValueChange, modifier, components = DateAndTime, dateRange: ClosedRange<LocalDate>? = null, enabled)`. **`AMDatePickerComponents`**: `Date`, `Time`, `DateAndTime`; the parts left out pass through `onValueChange` unchanged.
- **The row**: `InputControlRow` with the title, and a `FlowRow` (`spacing.sm` apart) of Material `AssistChip`s, each sized to its text: the date in the locale's medium style (`DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)`, "Nov 5, 2026"), the time in the locale's best hours-and-minutes pattern on the phone's 12- or 24-hour setting (`DateFormat.getBestDateTimePattern(locale, "hm" / "Hm")`, "9:41 AM"). At large text the chips move under the title and wrap.
- **A chip opens the spotlight** (`AMSpotlight(opened != null && enabled, …, title = title)`; `opened` is a saved `DatePickerSpotlightPart?`): the title over a solid card holding
  - **the calendar** (from the date chip): Material's `DatePicker` with `title = null`, `headline = null`, `showModeToggle = false` and `DatePickerDefaults.colors(containerColor = Color.Transparent)`, so it lies on the card instead of painting its dialog color; for `DateAndTime`, a row under it (`DatePickerSwitchRow`, on `InputControlRow`: "Time" and the time chip) switches the card to the dial;
  - **the clock dial** (from the time chip): Material's `TimePicker`, padded `spacing.lg` (at the sides and top when a switch row follows); for `DateAndTime`, a row under it ("Date" and the date chip) switches back to the calendar. A time-only row opens the dial alone.
- **Nothing takes focus, and the keyboard never comes up.** Material's `TimeInput` isn't used: it focuses its hour field as it appears (its text fields request focus while the state's selection names them), which would raise the keyboard over the card. The calendar and the dial are touch pickers with no text fields.
- **Changes apply as they're made**: the picker states start from `value` each time the spotlight opens (`rememberDatePickerState` / `rememberTimePickerState`, the time on the phone's 12- or 24-hour setting) and survive switching between calendar and dial; `snapshotFlow`s report each new day (keeping the time) or hour and minute (keeping the day, seconds cleared) through `rememberUpdatedState` copies of the value and callback, skipping values equal to the current one. The chips in the switch rows show the live value. The spotlight stays up until a tap outside or back. The spotlight's card scrolls if either part is ever taller than the room.
- **Days as Material holds them**: UTC-midnight milliseconds (`LocalDate.utcMillis()`, `Long.utcDate()`). `dateRange` becomes `DatePickerDayRange`, a `SelectableDates` that allows days in the range, and the year range (`pickerYears()`, clamped into `DatePickerDefaults.YearRange` so an open-ended range stays finite). Times aren't limited.
- TalkBack reads the title, then each chip as a button named by its value. Disabled: the title greys, the chips take Material's disabled look and don't open.

## Catalog pages

`demo/.../catalog/pages/inputs/`, registered in `InputsPages.entries` in this order (ids): `textField`, `secureField`, `searchField`, `textEditor`, `slider`, `stepper`, `toggle`, `datePicker`. Every Preview is `FullBleed`: the control alone on a `Surface.primary` strip (text inputs padded `spacing.lg` in a `Box`; rows carry their own recipe).

| Page | Preview | Loaded |
|---|---|---|
| Text field (`TextFieldPage.kt`) | an empty "Full name" field | a contact form in a padded card: "Full name" filled with Ada Lovelace, Email, Phone number, Website, Next moving down and Done on the last |
| Secure field (`SecureFieldPage.kt`) | an empty "Password" field with its eye | change password: current (no eye, so the clear button), new and confirm in a padded card, "Passwords don't match" as the confirm field's error line on a mismatch, then a full-width Update password button enabled once it can submit |
| Search field (`SearchFieldPage.kt`) | an empty "Search" field | a card: "Search cities" padded `spacing.lg` over six city rows (`amRowPadding`, inset dividers) that filter as you type |
| Text editor (`TextEditorPage.kt`) | an empty "Note" editor | a weekly update in a padded card: a filled "This week" summary (two lines minimum, grown to its text) and an empty "Blockers" editor |
| Slider (`SliderPage.kt`) | "Volume" between quiet and loud speakers | a card: a track scrubber between its elapsed and remaining times ("Track position", "1:02 of 3:43"), a volume slider with speaker ends ("Volume"), and "Crossfade" between Off and 12 s ("4 seconds") |
| Stepper (`StepperPage.kt`) | "Guests" | a room booking card: Adults, Children (at its minimum), Rooms, and a coffee glyph with "Breakfast × 2" (named "Breakfasts", the text hidden from TalkBack) |
| Toggle (`TogglePage.kt`) | "Notifications", on | a card: Push notifications (on), Email digest, Low data mode with its supporting line, and Sync over cellular (on, disabled) |
| Date picker (`DatePickerPage.kt`) | "Starts", now, date and time | a trip card: Departs (tomorrow 9:00, from today), Returns (date only, from the departure day), Wake-up alarm (time only), Booked (a week ago, disabled) |

- Demo glyphs these pages add: `ic_password`, `ic_notes`, `ic_exposure`, `ic_toggle_on`, `ic_calendar_clock` (entry icons), `ic_volume_mute`, `ic_volume_up`. They also use `ic_text_fields`, `ic_search`, `ic_tune` (entry icons) and `ic_coffee`.
- Other categories' pages hold `AMTextField` too: Form actions' task title (`Actions.md`), Banner's project name and Loading overlay's caption (`Feedback.md`), and the Sheet page's display name (`Overlays.md`), each in a card padded `spacing.lg`.
