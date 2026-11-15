---
scope: Cross-cutting conventions for every AndroidMtrls public API — the AM prefix, parameter order and names, string and slot overloads, closed style types, hoisted state, enabled, public types and modifiers, the native behavior kept, glyphs and motion — read before adding or changing ANY public API
globs: [androidmtrls/build.gradle.kts, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/**, androidmtrls/src/main/res/drawable/**]
keywords: [AM prefix, Modifier.am, LocalAM, amInstantSwitch, rememberAMFloatingActionButtonState, ProvideAMSurfaceStyle, ProvideAMOnSurface, LazyListScope.amSection, WindowInsets.amFloatingActions, remember, Provide, explicitApi, public API, internal, one package, area folders, filenames, parameter order, Compose component guidelines, modifier parameter, no modifier, content lambda, trailing lambda, enabled last, contentDescription, icon, Painter, label, text, title, subtitle, supportingContent, onClick, onValueChange, onDismissRequest, onSelectedChange, onExpandedChange, onCurrentIndexChange, visible, item, presentation state, string overload, slot overload, nullable slot, default slot, sealed style, enum, closed type, AMButtonStyle, AMTextFieldAutofocus, CompositionLocal default, LocalAMButtonStyle, hoisted state, TextFieldState, rememberSaveable, enabled, LocalAMEnabled, resolveEnabled, Immutable, Stable, action payload, data class, data object, equals, copy, Modifier.Node, ModifierNodeElement, currentValueOf, composed, string literals, no string resources, visual-only, quiet defaults, autofocus, flat widgets, enforcement ladder, MaterialTheme, static color scheme, dynamic color, type roles, sentence case, row recipe, amRowPadding, AMRowDefaults, ripple, Modifier.indication, 48 dp, minimumInteractiveComponentSize, LocalMinimumInteractiveComponentSize, predictive back, edge-to-edge, insets, SwipeToDismissBox, SaveableStateHolder, long press, HapticFeedbackType, TalkBack, font scale, sp, Material Symbols, am_ic_, vector drawable, autoMirrored, colorControlNormal, motion policy, MotionScheme, Remove animations, looping motion, third-party libraries]
updated: 2026-11-15
---

# Conventions

The cross-cutting rules every public API in the library follows: naming, parameter order, state, public types, theming, native behavior, glyphs and motion. Component digest files layer on top.

## Where things live

- Library module: `androidmtrls/`. Build settings, including `explicitApi()` and the `api` dependencies: `androidmtrls/build.gradle.kts`.
- Sources: one package, `com.ideamtrls.androidmtrls`, like Material 3's single package. Files are grouped in area folders under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/`: `foundations`, `actions`, `inputs`, `selection`, `navigation`, `layout`, `lists`, `feedback`, `overlays`, `datadisplay`, `collections`, and `internal` for shared non-public plumbing. The folder is organization only; every file declares `package com.ideamtrls.androidmtrls`.
- Library glyphs, the library's only resources: `androidmtrls/src/main/res/drawable/am_ic_<name>.xml`.
- The style layer's raw values: `foundations/Style.kt` (`Foundations.md`).
- The container-disabling local: `foundations/Enabled.kt` (internal `LocalAMEnabled`, `resolveEnabled`, `ProvideAMEnabled`; `Actions.md`).

## Project-wide facts (apply to EVERY task)

- Kotlin 2.4 with **`explicitApi()`**: every declaration states its visibility, and every public one its return type. Anything not meant for apps is `internal` or `private`.
- **minSdk 36 (Android 16)**, never lowered, with no `SDK_INT` checks, `@RequiresApi` or older-release fallbacks; compileSdk 37 (the demo targets 37). **Phones only**: no tablet, foldable or landscape designs.
- **Pre-release.** Breaking parameter reorders are allowed: fix the parameter order to match the rules below rather than appending parameters to keep source compatibility.
- **Jetpack and Google only.** No third-party libraries in the library or the demo; JUnit runs the unit tests. Image loading and blur are built in-house.
- The library exposes Compose UI, Foundation and Material 3 to apps (`api`), because its theme installs `MaterialTheme` and its components hand Material types through.
- **The library's own words are English literals in code**: the stepper's "Decrease", the secure field's "Show password", the loading overlay's "Loading", a panel's "Back". There are no string resources; everything else a component shows or speaks comes in through its parameters.

## The AM prefix rule

**Everything AndroidMtrls exposes carries an `AM` / `am` mark.** No bare public symbol.

- Public **types and composables** get uppercase `AM`: `AMTheme`, `AMTokens`, `AMButton`, `AMButtonStyle`, `AMIconButton`, `AMTag`, `AMSheet`, `AMSwipeAction`. Nested types inside an `AM` type aren't prefixed again: `AMButtonStyle.Primary`, `AMTheme.Surface`.
- Public **`Modifier` extensions** use lowercase `am`: `Modifier.amCard()`, `Modifier.amRowPadding()`, `Modifier.amHairline()`, `Modifier.amHoldable()`. So does an extension on any other receiver: `LazyListScope.amSection(…)`, `WindowInsets.amFloatingActions`.
- **Other functions take Kotlin's and Compose's own name shapes, with the mark inside**: a plain (non-composable) function is lowercase like a modifier, `amInstantSwitch { }`; a state holder's factory is `remember` plus its type, `rememberAMFloatingActionButtonState()` (Compose's `rememberDrawerState`); a composable that provides a CompositionLocal starts with `Provide`, `ProvideAMSurfaceStyle { }`, `ProvideAMOnSurface { }` (Compose's `ProvideTextStyle`).
- **CompositionLocals** are `LocalAM…`: the public `LocalAMButtonStyle` and `LocalAMSurfaceStyle`, and internal ones such as `LocalAMEnabled`.
- **Resources** in the library carry `am_`: glyphs are `am_ic_<name>`. Resource names merge into the app's namespace, so the prefix keeps them from colliding with an app's own.
- **Filenames are concept-only**, no prefix: `Card.kt` holds `Modifier.amCard()`, `Theme.kt` holds `AMTheme`, `IconButton.kt` holds `AMIconButton`.
- **Mirroring Material's names is fine** behind the prefix (`AMTextField`, `AMSlider`, `AMDivider`, `AMTabs`). No bare `TextField` or `Slider` symbol or typealias exists, so nothing collides with Material and call sites never qualify.
- Why: it avoids collisions with Material and Compose, which share the names of the concepts, and it marks "this came from AndroidMtrls" at every call site.

## Parameter order

Every public composable follows the Compose component guidelines:

1. **Required parameters first**, then **`modifier: Modifier = Modifier`**, then **optional parameters** with defaults, then **the content lambda last**, so it's the trailing lambda. Among the optional parameters, optional callbacks (`onClick: (() -> Unit)? = null`, `onSearch`, `onValueChangeFinished`) follow the configuration, and **`enabled` is always the last one**, right before the content lambda.
2. **Within each group, content before configuration**, in visual order (a glyph before the text it leads). **`contentDescription` comes before callbacks**: it describes the content, so it sits with it.
3. A required callback (`onClick`, `onValueChange`, `onDismissRequest`) is a required parameter, so it comes before `modifier`.
4. **An overlay leads with its presentation state**: `visible: Boolean`, or a nullable `item` (null shows none), then `onDismissRequest`: `AMSheet(visible, onDismissRequest)`, `AMAlert(item, onDismissRequest)`, `AMMenuSpotlight(visible, onDismissRequest, title, …)`.
5. **No `modifier` where the call site has nothing to place**: `AMBanner` and `AMToast` (their host shows them), `AMStatusBarIcons` (an effect), `AMTheme` and the `ProvideAM…` providers, the `LazyListScope.amSection` DSL, and `AMLoadingOverlay`, the one overlay without one.
6. **Naming.** `icon` for a glyph, a `Painter`; `label` for a control's or an option's own string, except a button's, which is `text` (`AMButton`, the text form of `AMIconButton`); `title` for the heading of a row, tile, section, toolbar or alert, and `subtitle` for the line under it (`supportingContent` in the slot form; `AMToggle`'s is `supportingText`); callbacks are `onX`, with Material's spellings where Material has the concept (`onClick`, `onValueChange`, `onDismissRequest`, `onCheckedChange`); `contentDescription` for what a screen reader says; `autofocus` for focus on appear.

`AMIconButton` shows the order the rules produce:

```kotlin
@Composable
public fun AMIconButton(
    icon: Painter,                     // content (1, 2)
    contentDescription: String?,       // describes the content, before callbacks (2)
    onClick: () -> Unit,               // required callback (3)
    modifier: Modifier = Modifier,     // (1)
    size: AMIconButtonSize = AMIconButtonSize.Medium,
    badge: (@Composable BoxScope.() -> Unit)? = null,
    enabled: Boolean = true,           // last optional (1)
)
```

## String and slot overloads

A component with a customizable content slot comes as **two plain overloads**: a string form for the common case, and a slot form with the slot as the trailing lambda.

```kotlin
AMTag("Pro", icon = painterResource(R.drawable.ic_star), tint = AMTagTint.Info)
AMTag(tint = AMTagTint.Info) { /* custom content */ }
```

- The component applies its fixed treatment (type style, content color, shape) around whatever the slot emits, through `ProvideTextStyle` and `LocalContentColor`, so custom content stays on-system.
- The string form delegates to the slot form, or both call one private layout (`AMSection`, `AMEmptyState`), so the two never drift.
- An optional slot is a nullable lambda defaulting to `null` (`trailingContent: (@Composable () -> Unit)? = null`), Material's own pattern, so a caller never writes an empty lambda to mean "none". A slot that always shows something defaults to the library's own piece instead: `fallback = { AMRemoteImageFallback() }`, `marker = { AMTimelineDot() }`.
- Controls with fixed content take plain values and have no slot form: `AMIconButton` (its tap, toggle and text forms), `AMCardButton`, `AMToolbarButton`, `AMFloatingActionButton`, and the options of `AMSegmentedControl`, `AMMenuPicker` and `AMChipGroup`, named by a `label: (T) -> String`.

## Closed style types

- **A style or variant is a closed type, never a loose flag set**, so a meaningless combination can't be written: a sealed interface when a case carries options (`AMButtonStyle.Primary()`, `.Secondary()`, `.Ghost`; `AMProgressViewStyle`; `AMSliderDecoration`), an enum when none does (`AMSectionStyle`, `AMIconButtonSize`, `AMTagTint`, and `AMTextFieldAutofocus`, one value where two flags would allow "caret at the start without focus").
- **Where a container styles the pieces in its slots, the default comes from a `CompositionLocal`**: `AMButton`'s `style` defaults to `LocalAMButtonStyle.current`, which `AMFormActions` sets for its buttons, and a caller passes `style =` only to override. `LocalAMSurfaceStyle` does the same for nested surfaces (`Layout.md`).

```kotlin
AMButton("Save", onClick = ::save, style = AMButtonStyle.Primary())
```

## State and presentation

- **State is hoisted**, Material's way; a control never owns its value. A value and its callback pair as `x` / `onXChange`: `value` / `onValueChange`, `checked` / `onCheckedChange`, `selected` / `onSelectedChange`, `selection` / `onSelectionChange`, `expanded` / `onExpandedChange`, `currentIndex` / `onCurrentIndexChange`, `current` / `onCurrentChange`. Text inputs take a hoisted `TextFieldState` (`rememberTextFieldState()`), Material's state-based field.
- **Presentation is a composable** that state drives: `AMSheet(visible, onDismissRequest) { … }`, `AMBanner(item, onDismissRequest)`. An overlay is on screen exactly while its state says so.
- **Views you leave are saved and restored**, not kept running: screen state that must survive a switch, a configuration change or process death uses `rememberSaveable` (and `SaveableStateHolder` for whole views).

## Public types and modifiers

- **Payloads, tokens and styles are `@Immutable`; state holders are `@Stable`**, each made by its `rememberAM…State()` (`AMFloatingActionButtonState` is saved through its `Saver`; `AMSpotlightState` has an internal constructor).
- **An action payload** (`AMAlertAction`, `AMSwipeAction`, `AMHoldAction`, `AMEmptyStateAction`) is a class carrying its `label`, its `icon` where it shows one, its role (`destructive`, a `tint`) and an `onClick: () -> Unit` that runs on the main thread from the component's own control.
- **Public value types are plain classes with `public val` properties, not data classes.** Tokens and styles hand-write `equals`, `hashCode` and `toString` (tokens add `copy`); a case with no options is a `data object` (`AMButtonStyle.Ghost`). `AMSliderDecoration.Text` is the one public `data class`.
- **A `Modifier.am…` is a plain function, never `@Composable` or `composed`**: it appends a `Modifier.Node` element (`this then CardSurfaceElement(…)`), and the node reads composition locals with `currentValueOf` (`CompositionLocalConsumerModifierNode`). Being a modifier, it can't provide a CompositionLocal to what it wraps.

## `enabled` on every control

- Every control takes `enabled: Boolean = true` and passes it through internal `resolveEnabled(enabled)`, true only when its own `enabled` and `LocalAMEnabled.current` both are, so a container can disable everything inside it: `AMFormActions(enabled = false)` greys its buttons without the caller touching each one (the mechanism: `Actions.md`).
- **No `enabled`**: the gesture-owning collections (`AMCardStack`, `AMCarousel`), and overlays and what goes in them (`AMMenuOptions`), since an overlay window starts enabled. `Modifier.amHoldable`'s `enabled` is its own alone: it doesn't read `LocalAMEnabled`. `AMStatusBarIcons(enabled)` says whether its claim on the bar is held, not whether a control takes touches.
- A disabled control keeps its place, size and fill; its content greys to Material's disabled color, it stops taking touches, and a floating surface (a filled button, a tile) drops its shadow.

## Visual-only primitives and quiet defaults

- **Visual only.** Interaction (taps, swipes, selection) is the caller's job, so primitives compose with any container. A list row draws; it handles taps only when it's given an `onClick`. `Modifier.amSelectionCheck` draws the mark and leaves the tile's `toggleable` to the caller. The exceptions own the interaction because it's their whole job: swipeable rows, the reorderable lists, the carousel, the card stack, and chip groups' tap-to-toggle.
- **Quiet defaults.** Anything attention-grabbing is opt-in or state-driven: text inputs' `autofocus` defaults to `AMTextFieldAutofocus.Off`; banners, sheets and overlays appear only when their state flips; nothing moves on its own. A primitive sits still until the user acts.
- **Controls are widgets with one flat rendering; the container supplies the surface.** Text fields, pickers, chips, segmented controls, sliders, toggles and steppers keep Material's shapes, outlines and behavior and draw no shadow of their own; only their container color follows the app's surface setting (`AMTheme(surfaceFill = …)`; per control in `Inputs.md` and `Selection.md`). A selected chip takes `primary`; switches, sliders and stepper buttons keep their own colors. Place them inside `Modifier.amCard()` for the floating treatment, or on the surface color for a flat strip.

## Enforcement ladder — make the opinions hard to break

A design rule lives at the **highest rung of this ladder it can reach** (weakest to strongest), so a caller falls into the right behavior instead of having to remember it.

1. **KDoc**: documentation, not enforcement.
2. **Quiet defaults**: the safe value is the default (`autofocus` off, overlays only on a state flip).
3. **Containers**: a purpose-built composable encodes a rule so call sites can't drift: `AMFormActions` (the paired form-commit row), `AMCardActionRow` (counts at the start, glyph actions at the end), `AMActionGrid` (the bottom-anchored menu), `Modifier.amRowPadding()` (the one row rhythm).
4. **Type-level rules**: invalid states don't compile. `AMActionGridRow` has exactly 2-, 3- and 4-tile overloads and no list form; style types are closed.
5. **Nearest-owner signals**: child-to-parent signals reach the nearest owner, through a `CompositionLocal` registry it provides or a layout alignment line, so no caller hand-tracks them: "an overlay is up" fades the floating action button at scene level (`LocalAMOverlayRegistry`), the discard guard stops at its sheet (`LocalAMDiscardGuard`), a timeline entry's first line (`Modifier.amTimelineFirstLine`, an alignment line) places the marker in its own row.
6. **`internal` plus `explicitApi()`** (strongest): where the library must own the choreography, the raw piece is `internal` and the composable or modifier that presents it is the only public entry.

There is no lint rung.

## The native layer: what the library takes from Android as-is

| Topic | Android behavior |
|---|---|
| Theming | `AMTheme` installs `MaterialTheme`: a static color scheme (Material's baseline by default, or the app's own; never dynamic color), Material's standard motion scheme, and the current type scale and shapes (Material's defaults at the root: 8/12/16 dp small/medium/large). Every Material component reads it unchanged. Type, color, shape and motion are overridden the Android way, through `MaterialTheme` |
| Color | Text and accent colors come from the color scheme. The style layer owns the surfaces and the container colors native controls take from the app's surface setting (solid, frosted or the tint), the page, shadows, hairlines and the selection ring |
| Type | The Material type scale in the phone's own font, sentence case everywhere; the roles (page title, section header, row, caption, code) are in `Foundations.md` |
| Corners | Circular corners from the shape scale; a card is `shapes.large`, a button (filled or text) `shapes.medium`, never a pill; the floating action button is a circle |
| Rows | The row recipe (`Layout.md`): padding around the content, never a fixed height. `Modifier.amRowPadding()` applies it to hand-built rows and `AMRowDefaults` to Material `ListItem` rows; rows stay flat on their surface in every state (`Layout.md`) |
| Press feedback | Ripple, which also brings the hover and focus states: Material's own in Material components; on a custom control, `ripple()` placed on its shape (`Modifier.indication`, as the floating action button and a calendar day do). No press dim, no custom press overlay |
| Touch targets | 48 × 48 dp. Material components size themselves; custom AM controls apply `Modifier.minimumInteractiveComponentSize()`. Where a surface wraps a Material control (filled buttons, the toolbar chip, chips, the frosted segmented control's backing), the wrapper reserves the 48 dp and the control inside lays out with `LocalMinimumInteractiveComponentSize` at 0 dp, so the surface hugs it while taps land across the whole target. The target never shrinks |
| Back | System back and predictive back everywhere. Overlays are windows, so the topmost one takes back first |
| Edge-to-edge | Always (`enableEdgeToEdge()` in the activity). Every component handles its insets; the outermost `AMTheme` sets the bar icons for its mode, and art of the other tone turns the status bar's over (`AMStatusBarIcons`) |
| Swipe on rows | `SwipeToDismissBox`: a full swipe performs the action over a colored background with an icon. Other actions go in ⋮ (`more_vert`) or a long press |
| Long press and haptics | The system's long-press timeout (holds, the reorderable lists' hold to lift, the floating action button's lift); `HapticFeedbackType` for the feedback |
| Accessibility | TalkBack semantics from Material; dialogs and sheets are modal windows; banners and toasts announce politely; tooltips on long-pressed icon buttons |
| Font scale | `sp` with the platform's nonlinear scaling; Material components wrap as they grow. Fixed-height pieces (the remote's tiles) say so in their KDoc |

## Glyphs

- **Material Symbols Outlined**, Android's default, as vector drawables copied into the project from Google's own repository (`https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android/<name>/materialsymbolsoutlined/<name>_24px.xml`). No `material-icons` Compose artifacts.
- **Only glyphs in use** are copied. Library-owned glyphs go in `androidmtrls/src/main/res/drawable/` as `am_ic_<name>.xml`; demo-only glyphs go in the demo's own `res/drawable/` as `ic_<name>.xml`.
- The files keep Google's content, except that the tint points at the platform attribute, `?android:attr/colorControlNormal`, since there's no AppCompat theme to define the app-namespace one. `Icon` tints with `LocalContentColor` either way.
- Directional glyphs (`arrow_back`, `chevron_right`) carry `android:autoMirrored="true"`, so they flip in right-to-left layouts.
- The shared glyph vocabulary: `arrow_back` for back, ⋮ `more_vert` for more actions, Android's `share`, and `logout`.

## Motion policy — components own their motion; apps animate freely

- **Native containers first.** Navigation pushes, sheets, dialogs and pickers animate the way Material and Navigation 3 animate them, predictive back included, so the library writes no motion code for them.
- **Every component that animates owns its motion** and takes its specs from Material's motion scheme (`MaterialTheme.motionScheme`, the standard scheme, not the expressive one; a modifier node reads it through `currentValueOf(MaterialTheme.LocalMaterialTheme)`). Callers never pass animation specs into a component. A looping motion (the skeleton shimmer) is a fixed linear loop, as Material's indeterminate indicators are, since the scheme's springs don't loop.
- **Remove animations** (the system's animator duration scale at 0) makes component motion instant, as Compose does by default, and holds a looping motion still. Drag tracking stays 1:1.
- **Apps may animate freely.** Nothing in the build checks or blocks an app's own animation. An app that swaps content itself has `amInstantSwitch { }` and `Modifier.amCrossfade(value)` (`Foundations.md`).

## See also

- `Foundations.md` — the theme, the style values and the motion helpers these rules lean on.
- `Actions.md` — the `LocalAMEnabled` mechanism, owned with `foundations/Enabled.kt`.
- `Patterns.md` — the screen-level opinions (the two layers, quiet defaults) these API rules serve.
