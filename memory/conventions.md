---
scope: Cross-cutting public-API, naming & authoring conventions for every AndroidMtrls component — read before adding, changing, or authoring ANY public API
globs:
  - mtrls/src/**
keywords: [AM prefix, am prefix, parameter order, contentDescription, trailing lambda, composable slot, String overload, Modifier extension, CompositionLocal, composable vs Modifier, Immutable, Stable, no Material3, monochrome ripple, amRipple, indication, state layer, press feedback, ImageVector icons, KDoc, Preview, quiet defaults, visual-only, pre-release, flat widget, self-chromed, filled field, standing label, bare AMCard, AMTheme.Type, type scale, typography, toTextStyle, Medium emphasis, letterSpacing, reduce motion, LocalAMReduceMotion, resolved, resolvedOrNull, resolvedFinite, animated spec routing, same look platform-native feel, Android norms, iOS as look/API-shape reference]
updated: 2026-07-10
---

# Conventions

The cross-cutting rules every public API in `mtrls/src/**` must follow —
Kotlin/Compose rules in their own right. They share intent with the iOSMtrls
conventions (`../iOSMtrls/Memory/Conventions.md`), a LOOK/API-shape reference
only (see "Same look, platform-native feel").

## Project-wide facts

- The library shares iOSMtrls's component roster, API shape, and the pair's
  signature look — but it is a Kotlin/Compose design system in its own right,
  Android-native under the hood (see "Same look, platform-native feel"). When a
  look value or API shape needs a reference, `../iOSMtrls/Sources/**` shows the
  shared intent; it is never a behavioral or implementation spec to copy exactly.
- **Pre-release.** Breaking parameter reorders are allowed — fix param order to
  match the rules below rather than appending new params for compatibility.
- `:mtrls` has **no Material3 dependency**. Press feedback is the monochrome
  `amRipple()` state layer (`tokens/Ripple.kt`): wire `indication = amRipple()`
  on every `clickable`/`toggleable`/`selectable` alongside a hoisted
  `remember { MutableInteractionSource() }` — never an opacity dim, never
  `indication = null` on an affordance. Bounded ripples require the node
  `Modifier.clip(shape)`'d to its visual shape so the sweep honors the AM corner
  radius. The only indication-free surfaces are tap-catchers/input blockers,
  transient-surface tap-to-dismiss (Toast/Banner/Sheet), text-field
  tap-to-focus, and drag surfaces. Internal glyphs may come from
  `material-icons-core`; public APIs take `icon: ImageVector` so consumers
  choose their icon set.
- Config enums and data carriers crossing recomposition are annotated
  `@Immutable` (or `@Stable` where mutation is intended) — the Compose
  counterpart of the iOS `Sendable` rule.

## Same look, platform-native feel

AndroidMtrls and iOSMtrls share the pair's signature look (inverted white/black
floating surfaces, two-layer shadows, hairlines) and API shape
(component-for-component, matching parameter names) — a *similar* look, never a
pixel-for-pixel copy. Each library is native under the hood and must *feel*
native on its own platform; iOS is never a spec to reproduce exactly. This split
governs every component:

- **iOS is the reference for LOOK and API shape only.** Visual values (colors,
  corner radii, shadow layers, dimensions), the component roster, and parameter
  naming come from the iOS source at `../iOSMtrls/Sources/**` and its memory
  docs at `../iOSMtrls/Memory/*.md` — *similar*, not identical, and never the
  authority for behavior, implementation, gesture, fling, or motion feel, which
  follow Android platform norms.
- **Gesture, fling, and motion feel follow Android norms, never iOS.** Compose
  fling/overscroll, the monochrome `amRipple()` state layer (never an iOS-style
  opacity dim), `rememberSaveable` state, and the animator-duration-scale
  Reduce-Motion signal are Android-native — there is no iOS behavior to copy for
  these.
- **An iOS primitive with no Android analog is dropped, not emulated.** Example:
  iOS `imInstantSwitch` has no port — Compose has no implicit animations, so a
  plain `state = …` write is already an instant switch.

## The AM / am prefix rule

Everything AndroidMtrls exposes carries an AM/am mark. No bare public symbol.

- Public **types and composables** get uppercase `AM`: `AMTheme`, `AMTokens`,
  `AMCard`, `AMTag`, `AMIconButton`. Supporting public types too
  (`AMElevation`, `AMBadge.Content` stays un-re-prefixed inside an `AM` type).
- Public **Modifier extensions** use lowercase `am`: `Modifier.amElevation(...)`,
  `Modifier.amHairline(...)`. **CompositionLocals** use `LocalAM*`:
  `LocalAMTokens`, `LocalAMSurfaceStyle`, `LocalAMIsDarkTheme`.
- **Filenames stay concept-only**, no prefix: `Tag.kt` holds `AMTag`,
  `Tokens.kt` holds `AMTokens`.

## Composable function vs Modifier extension (the rule)

Decoration that does not communicate with a subtree = `Modifier` extension
(`amElevation`, `amHairline`). Anything that must provide a CompositionLocal to
its content = `@Composable` function (`AMCard` provides
`LocalAMSurfaceStyle = Flat` so nested cards demote themselves — a Modifier
cannot do that).

## Parameter order (the 5 rules, Kotlin form)

1. **Primary content/value first, in visual order** — `icon` before label text;
   text-input `value`/`onValueChange` immediately after `placeholder`.
2. **Configuration next** — sizes, ranges, enabled flags, tints; required
   before optional-with-default where possible.
3. **`contentDescription` is the last non-lambda param** (metadata, not
   content — the iOS `accessibilityLabel` rule).
4. **Lambdas last.** Callbacks (`onClick`, `onSubmit`, `onCancel`, `onBack`)
   after config; the main content slot is the **trailing lambda**, with
   `modifier: Modifier = Modifier` immediately before it (Compose idiom —
   wins over any spec draft showing a slot before `modifier`).
5. **Naming.** `icon` for `ImageVector` params; `autofocus` (not `autoFocus`);
   callbacks are `onX`.

### Enabled/disabled and role flags

- Enabled flags are always **`enabled: Boolean = true`** — never
  `disabled: Boolean`, even where a controls-spec draft shows `disabled`.
  Components without a spec'd enabled state simply omit the param.
- **`role: AMButtonRole?` exists only on String-convenience overloads.**
  Composable-slot overloads omit it: slot content is caller-styled and
  `:mtrls` has no content-color CompositionLocal, so a slot-side `role`
  would be silently ignored. Slot-overload KDoc points destructive callers
  at `AMTheme.Semantic.destructive`. `AMButtonRole` is declared once in
  `surfaces/frames/Toolbar.kt` — reuse it, never redeclare.

### Gesture & state authoring rules (house patterns)

- **pointerInput accumulators are plain `var`s inside the `pointerInput`
  block** — never remember-state written per pointer-move event
  (precedent: FAB, Slider, `amDragDismiss`).
- `remember { MutableInteractionSource() }` never sits inside a loop with a
  variable trip count — hoist a shared source, or key a list on the count
  (`remember(items.size) { List(items.size) { MutableInteractionSource() } }`).
- Self-managed open/expanded/value state uses `rememberSaveable`
  (text fields use `rememberSaveable(stateSaver = TextFieldValue.Saver)`
  with the inline compare-and-set realign against the `value` prop).
- iOS `.easeOut` converts to `LinearOutSlowInEasing` (deceleration), NOT
  `FastOutLinearInEasing`; `pressFade = tween(120, LinearOutSlowInEasing)`.
- House motion tokens are `AnimationSpec<Float>`; `animateDpAsState` needs
  `AnimationSpec<Dp>` — mirror the token as a file-level
  `private val ... = spring<Dp>(...)` with a lockstep comment naming the
  token (precedent: Tabs.kt `snapSpringDp`, PageIndicator.kt `modalSpringDp`).
- **Every animated spec routes through
  `tokens.motion.resolved`/`resolvedOrNull`/`resolvedFinite`/`resolvedFiniteOrNull`
  (`tokens/Motion.kt`) with `LocalAMReduceMotion.current`** — the single
  Reduce-Motion collapse point; never let a site re-decide its own fallback.
  `resolved`/`resolvedOrNull` return `AnimationSpec<T>`; the `*Finite` pair
  returns `FiniteAnimationSpec<T>` for the `AnimatedVisibility` slide/fade/
  expand builders. Use `resolved`/`resolvedFinite` to soften a spec to the
  gentle 180ms fade, and `resolvedOrNull`/`resolvedFiniteOrNull` (`?: snap()`)
  to drop a spatial transform to nothing under Reduce Motion (slides, alert
  pop, FAB lift). **Drag-follow 1:1 tracking is exempt** (the offset written
  per pointer-move has no spec); auto-dismiss timers are unaffected;
  **indeterminate loading indicators are exempt** (`AMSpinner` keeps spinning —
  a frozen spinner reads as a hang). Read the flag at composable
  scope — inside a `Modifier.composed {}` for gesture modifiers (precedent:
  `amDragDismiss`).

## Content slots with String overloads (the slot pattern)

Components that render text/icons take a composable slot in the designated
overload; a `String` (+ optional `icon`) convenience overload applies the same
fixed visual treatment (font, color, shape) over a standard label. Both
overloads stay in convention order. Applies to tags, section/page headers,
card/grid buttons, tab labels, chip labels, list-row titles.

## Typography (the house type scale)

Component text styles come from the locked `AMTheme.Type` scale
(`tokens/Type.kt`) — never ad-hoc `TextStyle` sizes. Render an entry with
`AMTheme.Type.<role>.toTextStyle(color)`. The six roles:

- `title` — 16 / Medium / 0.15 — row headlines, dialog & banner titles.
- `body` — 16 / Normal / 0.5 — input text, body copy, un-emphasized labels.
- `secondary` — 14 / Normal / 0.25 — subtitles, picker values, toasts, footers.
- `label` — 14 / Medium / 0.1 — buttons, tabs, chips, segmented labels.
- `caption` — 12 / Normal / 0.4 — captions, section footers.
- `labelSmall` — 11 / Medium / 0.5 — badges, tile captions, tiny metadata.

Emphasis is **Medium**, never SemiBold — with one deliberate exception: the
filled/ghost buttons (`Buttons.kt`) render 17sp labels with weight-only
variation (primary = SemiBold, secondary = Normal, ghost = Medium), carrying
the pair's shared button look. Tracking rides on the style — explicit
`letterSpacing = 0` is never set. Deliberate literals stay: the display sizes
(22/28/34sp), monospaced/`tnum` numeric runs, and `AMSectionHeader`'s uppercase
12sp / Medium / 0.5-tracking treatment. Full scale table in `foundations.md`.

## Quiet defaults & visual-only primitives

- Primitives are **visual only**: list rows are non-tappable, pickers only
  write state up; interaction wiring is the caller's job.
- **Flat widgets vs self-chromed.** The data-entry ROWS (pickers, sliders,
  steppers, list rows) draw no fill/shadow/hairline/outline and expect a **bare
  `AMCard` (no `padding:`)** wrap for the 64dp row rhythm; a padded card
  double-counts insets. **The four text inputs are self-chromed filled fields:**
  `AMTextField`/`AMSecureField`/`AMTextEditor`/`AMSearchField` draw their own
  opaque `Surface.primary` container (via internal `AMFilledField`) — a
  56dp-min white/black filled box with a 1dp→2dp accent bottom indicator
  (search is a pill with an unconditional hairline and no indicator) — so they
  need no card for chrome, only a surface behind them. **Inputs carry NO label
  of their own — the label is external in the Mtrls UI:** a standing
  `AMFieldLabel` above the field/control is THE labeling pattern
  (`AMSegmentedControl` included). The self-chromed set: the filled
  buttons, `AMActionGridButton`, the FAB, the four filled text inputs,
  `AMStepper`'s square tonal ± buttons, and `AMSegmentedControl`'s outlined track.
- **Quiet defaults**: `autofocus = false`; overlays appear only when state
  flips; nothing grabs attention unprompted.
- Overlay APIs (`AMSheet`, `AMBanner`, `AMToast`, …) document "attach at the
  scene root" in their KDoc.
- **Overlay presenters are host composables that WRAP the scene content**
  as the trailing `hostContent` lambda (never sibling Modifier presenters —
  a Modifier can't compose content or blur behind it; precedent
  `AMPanelHost`). Hosts that blur apply the internal
  `Modifier.amBackdropBlur(radius)` to the wrapped content layer with an
  animated radius; hosts nest for stacking.

## Previews & docs (every component file)

Every component file ends with light + dark `@Preview` composables (the
counterpart of the iOS `#Preview` policy). Every public symbol carries KDoc.

## See also

- `build.md` — modules, commands, dependency decisions.
