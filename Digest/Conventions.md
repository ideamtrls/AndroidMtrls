---
scope: Cross-cutting public-API, naming, parameter-order, state, theming, native-behavior, glyph and motion conventions for every AndroidMtrls component — read before adding or changing ANY public API
globs: [androidmtrls/build.gradle.kts, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/**, androidmtrls/src/main/res/drawable/**]
keywords: [AM prefix, Modifier.am, LocalAM, amInstantSwitch, rememberAMFloatingActionButtonState, ProvideAMSurfaceStyle, remember, Provide, explicitApi, public API, internal, one package, area folders, filenames, parameter order, Compose component guidelines, modifier parameter, content lambda, trailing lambda, contentDescription, icon, label, title, onClick, onValueChange, onDismissRequest, string overload, slot overload, nullable slot, sealed style, AMButtonStyle, CompositionLocal default, hoisted state, visible, rememberSaveable, enabled, LocalAMEnabled, Immutable, Stable, action payload, visual-only, quiet defaults, autofocus, flat widgets, enforcement ladder, MaterialTheme, static color scheme, dynamic color, type roles, sentence case, row recipe, amRowPadding, AMRowDefaults, ripple, Indication, 48 dp, minimumInteractiveComponentSize, predictive back, edge-to-edge, insets, SwipeToDismissBox, SaveableStateHolder, long press, HapticFeedbackType, TalkBack, font scale, sp, Material Symbols, am_ic_, vector drawable, autoMirrored, colorControlNormal, motion policy, MotionScheme, Remove animations, third-party libraries]
updated: 2026-11-10
---

# Conventions

The cross-cutting rules every public API in the library follows: naming, parameter order, state, theming, native behavior, glyphs and motion. They hold across all of the library, so auditing this file means reading all of it; component digest files layer on top.

## Where things live

- Library module: `androidmtrls/`. Build settings, including `explicitApi()`: `androidmtrls/build.gradle.kts`.
- Sources: one package, `com.ideamtrls.androidmtrls`, like Material 3's single package. Files are grouped in area folders under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/`: `foundations`, `actions`, `inputs`, `selection`, `navigation`, `layout`, `lists`, `feedback`, `overlays`, `datadisplay`, `collections`, and `internal` for shared non-public plumbing. A folder exists once it has a file. The folder is organization only; every file declares `package com.ideamtrls.androidmtrls`.
- Library glyphs: `androidmtrls/src/main/res/drawable/am_ic_<name>.xml`.
- The style layer's raw values: `foundations/Style.kt` (`Foundations.md`).

## Project-wide facts (apply to EVERY task)

- Kotlin 2.4 with **`explicitApi()`**: every declaration states its visibility, and every public one its return type. Anything not meant for apps is `internal` or `private`.
- **minSdk 36 (Android 16)**, compileSdk 37 (the demo targets 37). **Phones only**: no tablet, foldable or landscape designs.
- **Pre-release.** Breaking parameter reorders are allowed: fix the parameter order to match the rules below rather than appending parameters to keep source compatibility.
- **Jetpack and Google only.** No third-party libraries in the library or the demo. Image loading and blur are built in-house.
- The library exposes Compose UI, Foundation and Material 3 to apps (`api`), because its theme installs `MaterialTheme` and its components hand Material types through.

## The AM prefix rule

**Everything AndroidMtrls exposes carries an `AM` / `am` mark.** No bare public symbol.

- Public **types and composables** get uppercase `AM`: `AMTheme`, `AMTokens`, `AMButton`, `AMButtonStyle`, `AMIconButton`, `AMTag`, `AMSheet`, `AMSwipeAction`. Nested types inside an `AM` type aren't prefixed again: `AMButtonStyle.Primary`, `AMTheme.Surface`.
- Public **`Modifier` extensions** use lowercase `am`: `Modifier.amCard()`, `Modifier.amRowPadding()`, `Modifier.amHairline()`, `Modifier.amHoldable()`.
- **Other functions take Kotlin's and Compose's own name shapes, with the mark inside**: a plain (non-composable) function is lowercase like a modifier, `amInstantSwitch { }`; a state holder's factory is `remember` plus its type, `rememberAMFloatingActionButtonState()` (Compose's `rememberDrawerState`); a composable that provides a CompositionLocal starts with `Provide`, `ProvideAMSurfaceStyle { }` (Compose's `ProvideTextStyle`).
- **CompositionLocals** are `LocalAM…`: the public `LocalAMButtonStyle` and `LocalAMSurfaceStyle`, and internal ones such as `LocalAMEnabled`.
- **Resources** in the library carry `am_`: glyphs are `am_ic_<name>`. Resource names merge into the app's namespace, so the prefix keeps them from colliding with an app's own.
- **Filenames are concept-only**, no prefix: `Card.kt` holds `Modifier.amCard()`, `Theme.kt` holds `AMTheme`, `IconButton.kt` holds `AMIconButton`.
- **Mirroring Material's names is fine** behind the prefix (`AMTextField`, `AMSlider`, `AMDivider`, `AMTabs`). No bare `TextField` or `Slider` symbol or typealias exists, so nothing collides with Material and call sites never qualify.
- Why: it avoids collisions with Material and Compose, which share the names of the concepts, and it marks "this came from AndroidMtrls" at every call site.

## Parameter order

Every public composable follows the Compose component guidelines:

1. **Required parameters first**, then **`modifier: Modifier = Modifier`**, then **optional parameters** with defaults, then **the content lambda last**, so it's the trailing lambda.
2. **Within each group, content before configuration**, in visual order (a glyph before the text it leads). **`contentDescription` comes before callbacks**: it describes the content, so it sits with it.
3. A required callback (`onClick`, `onValueChange`, `onDismissRequest`) is a required parameter, so it comes before `modifier`.
4. **Naming.** `icon` for a glyph; `label` for a control's or an option's own string; `title` for the heading of a row, tile, section, toolbar or alert; callbacks are `onX`, with Material's spellings where Material has the concept (`onClick`, `onValueChange`, `onDismissRequest`, `onCheckedChange`); `contentDescription` for what a screen reader says; `autofocus` for focus on appear.

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
    enabled: Boolean = true,
)
```

## String and slot overloads

A component with a customizable content slot comes as **two plain overloads**: a string form for the common case, and a slot form with the slot as the trailing lambda.

```kotlin
AMTag("Pro", icon = painterResource(R.drawable.ic_star), tint = AMTagTint.Info)
AMTag(tint = AMTagTint.Info) { /* custom content */ }
```

- The component applies its fixed treatment (type style, content color, shape) around whatever the slot emits, through `ProvideTextStyle` and `LocalContentColor`, so custom content stays on-system.
- The string form delegates to the slot form, so the two never drift.
- An optional slot is a nullable lambda defaulting to `null` (`trailingContent: (@Composable () -> Unit)? = null`), Material's own pattern, so a caller never writes an empty lambda to mean "none".
- Controls with fixed content take plain values and have no slot form: `AMIconButton` (its tap and toggle forms), `AMCardButton`, `AMFloatingActionButton`, the options of `AMSegmentedControl` and `AMMenuPicker`.

## Sealed style types

- **A style is a sealed type, never a loose flag set.** Button styles are `AMButtonStyle.Primary()`, `.Secondary()`, `.Ghost`, so a meaningless combination can't be written.
- **The default comes from a `CompositionLocal`**, so a container styles the buttons in its slots: `AMFormActions` sets the style its buttons pick up, and a caller passes `style =` only to override.

```kotlin
AMButton("Save", onClick = ::save, style = AMButtonStyle.Primary())
```

## State and presentation

- **State is hoisted.** A control takes its value and an `onValueChange` (or `checked` / `onCheckedChange`, `selected` / `onClick`), Material's way; it never owns the value.
- **Presentation is a composable** that state drives: `AMSheet(visible, onDismissRequest) { … }`. An overlay is on screen exactly while its state says so.
- **Views you leave are saved and restored**, not kept running: screen state that must survive a switch, a configuration change or process death uses `rememberSaveable` (and `SaveableStateHolder` for whole views).
- **Payloads and tokens are `@Immutable`**; state holders are `@Stable`. An action payload (`AMAlertAction`, `AMSwipeAction`) is an `@Immutable` class carrying its label, its glyph where it shows one, its role (`destructive`, a tint) and an `onClick: () -> Unit` that runs on the main thread from the component's own control.

## `enabled` on every control

- Every control takes `enabled: Boolean = true`. The gesture-owning collections (`AMCardStack`, `AMCarousel`) don't.
- An internal **`LocalAMEnabled`** lets a container disable everything inside it: `AMFormActions(enabled = false)` greys its buttons without the caller touching each one. A control is enabled when its own `enabled` and `LocalAMEnabled.current` are both true.
- A disabled control keeps its place, size and fill; its content greys to Material's disabled color, it stops taking touches, and a floating surface (a filled button, a tile) drops its shadow.

## Visual-only primitives and quiet defaults

- **Visual only.** Interaction (taps, swipes, selection) is the caller's job, so primitives compose with any container. A list row draws; it doesn't handle taps unless it's the clickable form. The exceptions own the interaction because it's their whole job: swipeable rows, the reorderable lists, the carousel, the card stack, and chip groups' tap-to-toggle.
- **Quiet defaults.** Anything attention-grabbing is opt-in or state-driven: text fields' `autofocus` defaults off; banners, sheets and overlays appear only when their state flips; nothing moves on its own. A primitive sits still until the user acts.
- **Controls are widgets with one flat rendering; the container supplies the surface.** Text fields, pickers, sliders, toggles and steppers never draw an AM surface fill, shadow or hairline of their own (Material's own outline on a text field is its native look, not a surface). Place them inside `Modifier.amCard()` for the floating treatment, or on the surface color for a flat strip.

## Enforcement ladder — make the opinions hard to break

A design rule lives at the **highest rung of this ladder it can reach** (weakest to strongest), so a caller falls into the right behavior instead of having to remember it.

1. **KDoc**: documentation, not enforcement.
2. **Quiet defaults**: the safe value is the default (`autofocus` off, overlays only on a state flip).
3. **Containers**: a purpose-built composable encodes a rule so call sites can't drift: `AMFormActions` (the paired form-commit row), `AMCardActionRow` (counts first, ⋮ last), `AMActionGrid` (the bottom-anchored menu), `Modifier.amRowPadding()` (the one row rhythm).
4. **Type-level rules**: invalid states don't compile. `AMActionGridRow` has exactly 2-, 3- and 4-tile overloads and no list form; style types are sealed.
5. **Nearest-owner signals**: child-to-parent signals reach the nearest owner, through a `CompositionLocal` registry it provides or a layout alignment line, so no caller hand-tracks them: "an overlay is up" fades the floating action button at scene level (`LocalAMOverlayRegistry`), the discard guard stops at its sheet (`LocalAMDiscardGuard`), a timeline entry's first line (`Modifier.amTimelineFirstLine`, an alignment line) places the marker in its own row.
6. **`internal` plus `explicitApi()`** (strongest): where the library must own the choreography, the raw piece is `internal` and the composable or modifier that presents it is the only public entry.

There is no lint rung.

## The native layer: what the library takes from Android as-is

| Topic | Android behavior |
|---|---|
| Theming | `AMTheme` installs `MaterialTheme`: a static color scheme (Material's baseline by default, or the app's own; never dynamic color), the default type scale and the default shapes (8/12/16 dp small/medium/large). Every Material component reads it unchanged. Type, color, shape and motion are overridden the Android way, through `MaterialTheme` |
| Color | Text and accent colors come from the color scheme. The style layer owns only the surfaces, the page, shadows, hairlines, the selection ring and the frosted fill |
| Type | The Material type scale in the phone's own font, sentence case everywhere. Roles: page title = `headlineMedium` (the large top app bar); section header = `titleSmall` in `primary` (Android's list-subheader look; over a picture, `onSurface` with the text shadow); row title and body = `bodyLarge`; caption = `bodySmall`; code = `bodyMedium` monospace |
| Corners | Circular corners from the shape scale; a card is `shapes.large`, a button (filled or text) `shapes.medium`, never a pill; the floating action button is a circle |
| Rows | The row recipe: the content plus 28 dp above and below and 16 dp at the sides, with no fixed height (one line about 80 dp, two lines about 100 dp). `Modifier.amRowPadding()` applies it to hand-built rows and `AMRowDefaults` to Material `ListItem` rows; rows stay flat on their surface in every state (`Layout.md`) |
| Press feedback | Ripple (Material's `ripple()` through `LocalIndication`), which also brings the hover and focus states. No press dim, no custom press overlay |
| Touch targets | 48 × 48 dp. Material components size themselves; custom AM controls apply `Modifier.minimumInteractiveComponentSize()`. Nothing in the library turns the minimum off |
| Back | System back and predictive back everywhere. Overlays are windows, so the topmost one takes back first |
| Edge-to-edge | Always. Every component handles its insets; status bar icons flip over dark art |
| Swipe on rows | `SwipeToDismissBox`: a full swipe performs the action over a colored background with an icon. Other actions go in ⋮ (`more_vert`) or a long press |
| Views you leave | Saved and restored (`SaveableStateHolder`), not kept running |
| Long press and haptics | The system's long-press timeout (holds and the floating action button's lift); `HapticFeedbackType` for the feedback |
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
- **Every component that animates owns its motion** and takes its specs from Material's motion scheme (`MaterialTheme.motionScheme`, the standard scheme, not the expressive one). Callers never pass animation specs into a component.
- **Remove animations** (the system's animator duration scale at 0) makes component motion instant, as Compose does by default. Drag tracking stays 1:1.
- **Apps may animate freely.** Nothing in the build checks or blocks an app's own animation.
