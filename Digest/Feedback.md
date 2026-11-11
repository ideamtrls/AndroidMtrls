---
scope: Transient and status feedback — banners and their host, toasts and their host, the loading overlay, empty states, progress indicators, and skeletons and shimmer
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/feedback/**, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/feedback/**, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/scaffold/ScreenHost.kt]
keywords: [AMBanner, AMBannerItem, AMBannerHost, LocalAMBannerHost, AMToast, AMToastItem, AMToastHost, LocalAMToastHost, ToastHost, bottomClearance, scene root, AMScene, sheet toast host, Snackbar, SnackbarHost, SnackbarHostState, SnackbarVisuals, Indefinite, TransientHost, TransientCaller, TransientPosting, PostTransient, transientTimeout, calculateRecommendedTimeoutMillis, Time to take action, auto-dismiss, swipe up, draggable, scrim, live region, LiveRegionMode.Polite, dismiss action, severityLabel, window host, nearest host, AMLoadingOverlay, Dialog, DialogProperties, dismissOnBackPress, back consumed, CircularProgressIndicator, ReportOverlayUp, AMEmptyState, AMEmptyStateAction, empty state, on the page, heading, AMSkeleton, Rect, Circle, Text, text placeholder, redacted text, TextLayoutResult, drawTextBars, TextBarInset, placeholder, surfaceContainerHighest, Modifier.amShimmer, shimmer, ShimmerNode, MotionDurationScale, withInfiniteAnimationFrameNanos, CompositingStrategy.Offscreen, BlendMode.SrcAtop, AMProgressView, AMProgressViewStyle, Spinner, Bar, LinearProgressIndicator, valueLabel, amFlatSurface, CatalogScreenHost, BannerPage, ToastPage, LoadingOverlayPage, ProgressPage, SkeletonPage, EmptyStatePage, FeedbackPages]
updated: 2026-11-10
---

# Feedback

How the UI tells the user what's happening: `AMBanner` (a floating surface at the top of its window that dismisses itself and swipes up) with `AMBannerHost`, `AMToast` (Material's snackbar without an action) with `AMToastHost` (the scene root, `AMScene`, carries both hosts, and every `AMSheet` its own toast host), `AMLoadingOverlay` (a dialog window that blocks while work runs: Material's circular indicator on a card), `AMEmptyState` (a glyph, title, description and actions lying on the page), `AMSkeleton` and `Modifier.amShimmer()` (shimmering placeholder shapes, and a text's placeholder at its exact size), and `AMProgressView` (Material's circular or linear indicator, labeled, as `Spinner` or `Bar`).

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `feedback/Banner.kt` | `AMBannerItem`, `AMBanner`, `AMBannerHost`; internal `LocalAMBannerHost`; private `BannerLayer`, `BannerCard`. Also the plumbing banners and toasts share, all internal: `TransientHost`, `TransientCaller`, `TransientPosting`, `PostTransient`, `AccessibilityManager?.transientTimeout` |
| `feedback/Toast.kt` | `AMToastItem`, `AMToast`, `AMToastHost`; internal `ToastHost` (the host with a bottom clearance, which `AMToastHost` calls with 0 and `AMScene` with the floating action button's), `LocalAMToastHost`; private `ToastVisuals` |
| `feedback/LoadingOverlay.kt` | `AMLoadingOverlay` |
| `feedback/EmptyState.kt` | `AMEmptyStateAction`, `AMEmptyState` (string and slot forms); private `EmptyStateLayout` |
| `feedback/Skeleton.kt` | `AMSkeleton` (`Rect`, `Circle`, `Text` in its lines form and its text form), `Modifier.amShimmer`; private `drawTextBars`, `ShimmerElement`, `ShimmerNode` |
| `feedback/ProgressView.kt` | `AMProgressViewStyle` (`Spinner`, `Bar`), `AMProgressView`; private `SpinnerProgress`, `BarProgress` |

- Pieces from other areas it builds on: `AMSemantic` (its `icon`, `color` and internal `severityLabel`), the internal `Modifier.amElevatedSurface` and `Modifier.amHairline` (`Foundations.md`), `Modifier.amCard`, `Modifier.amRowPadding` and the internal `Modifier.amFlatSurface` (`Layout.md`), `AMButton` (`Actions.md`), and `ReportOverlayUp` (`internal/OverlayRegistry.kt`, `Overlays.md`).
- Catalog pages: `demo/.../catalog/pages/feedback/` (below); the catalog's screen host: `demo/.../catalog/scaffold/ScreenHost.kt` (`Demo.md`).

## Banners and toasts: a host per window

A banner or a toast belongs to its window, not to the composable that asks for it. So each kind comes as two pieces:

- **The host** (`AMBannerHost`, `AMToastHost`): `(modifier, content)`. One sits at the root of each window that shows them, filling it. **The library places them**: `AMScene` (`Navigation.md`) has an `AMBannerHost` and a toast host at its root, so a scene's banners come down at the top of the screen and its toasts up at the bottom; `AMSheet` (`Overlays.md`) wraps its title and content in an `AMToastHost`, so a toast posted inside a sheet shows in the sheet, over it. A screen built without `AMScene`, or a window of the app's own, puts its own. It's a `Box` with `propagateMinConstraints`, so the content is laid out exactly as the host is; the message is drawn in a `matchParentSize` layer over it, takes no space from it, and lets touches outside the message through. It provides itself to its content through an internal `CompositionLocal` (`LocalAMBannerHost`, `LocalAMToastHost`), so the **nearest** host wins: a toast posted inside a sheet with its own host shows in the sheet.
- **The call** (`AMBanner(item, onDismissRequest, onClick)`, `AMToast(item, onDismissRequest)`): state is hoisted, the caller holds an `Item?`. The call draws nothing where it is; it posts its item to the nearest host while the item is non-null and the call is composed. **With no host above it, reading the local fails at once** (an error naming the missing host), so a forgotten host never shows up as a silent no-op.
- **Asking to go.** The host calls `onDismissRequest` when the message's time is up, and on the user's dismissal (a swipe, a tap, a screen reader's dismiss action). The caller sets its item back to null there; until it does, the message stays (a toast whose time is up is already gone from the screen).
- **One at a time.** A host shows the most recent posting. A caller that posts a new item replaces its own posting in place; a posting covered by a newer caller's shows again if it's still up when the newer one goes, with its full time.
- **Items compare by identity** (`@Immutable` classes, no `equals`), so posting a new item with the same text shows it afresh and restarts its time.
- **Time.** Each item has a `duration` (banner 4 s, toast 2 s), which the host stretches to the user's accessibility timeout (Accessibility → Time to take action) through `LocalAccessibilityManager`'s `calculateRecommendedTimeoutMillis`, the way Material's snackbar host does. The time runs while the posting is the one shown.
- **Not overlays.** Banners and toasts aren't modal and don't report through `ReportOverlayUp`; the scene's floating action button stays. A banner is at the top, clear of it; a scene with `floatingActions` raises its toasts above the button's resting place (below).

The shared plumbing (internal, in `Banner.kt`): `TransientHost<T>` keeps a snapshot list of `TransientPosting(caller, item)`, `current` being the last; `post(caller, item)` removes the caller's posting and appends the new one (null withdraws). `PostTransient` remembers a `TransientCaller` per call site, updates its callbacks in a `SideEffect` (`actionable`, whether there's an `onClick`, is snapshot state), and posts in a `DisposableEffect` keyed on the item, withdrawing on dispose.

## `AMBanner` (`Banner.kt`)

```kotlin
var banner by remember { mutableStateOf<AMBannerItem?>(null) }
AMButton("Save", onClick = { save(); banner = AMBannerItem("Changes saved", message = "Your profile is up to date.", style = AMSemantic.Success) })
AMBanner(banner, onDismissRequest = { banner = null })
```

- **`AMBannerItem(title, message = null, style = AMSemantic.Info, duration = 4.seconds)`**. The glyph is the style's (`AMSemantic.icon`, filled, tinted `AMSemantic.color`); there's no other glyph.
- **Look.** The floating surface in `shapes.large`: `amElevatedSurface` at `Low` with its dark-only hairline off, then `amHairline` in both modes, as every always-outlined surface. Inside, `spacing.md` all round: a top-aligned row, `spacing.md` apart, of the glyph (24 dp) and a column of the title (`bodyLarge`, `onSurface`) over the message (`bodyMedium`, `onSurfaceVariant`), 2 dp apart. Full width, inset `spacing.lg` at the sides and `spacing.sm` under the status bar (`WindowInsets.safeDrawing`, top and horizontal sides, so the display cutout too).
- **The scrim.** Behind the card, from the window's top edge, a 220 dp vertical gradient of `AMTheme.Surface.primary`: solid to 20 %, then fading to clear. It backs the status bar while the banner is up, so the card reads as part of the screen's top and the status bar stays legible. It draws only, taking no touches.
- **Motion** (Material's motion scheme; Remove animations makes it instant): the scrim fades in on `defaultEffectsSpec` and out on `fastEffectsSpec`. The card is an `AnimatedContent` keyed by the caller: it slides down from above the window (its full height, insets included) with `defaultSpatialSpec` plus a fade, and leaves back up with `fastSpatialSpec`; a new item from the same caller changes the card in place, a different caller's slides the old out and the new in. No size animation.
- **Swipe up.** `Modifier.draggable` (vertical) moves the card 1:1 through an offset, clamped so it only goes up. On release it dismisses if dragged more than 30 dp or flung up faster than 125 dp/s (the removal then slides on from where the finger left it); otherwise it settles back with `fastSpatialSpec`, carrying the fling's velocity. A new posting starts at rest.
- **Tap.** With `onClick`, the card is `clickable` with `Role.Button` and Material's ripple, clipped to the shape: a tap calls `onDismissRequest`, then `onClick`, so the action can post a follow-up banner. Without it, a tap only dismisses (a tap gesture with no semantics, so the card doesn't read as a button). The swipe and the timer never call `onClick`.
- **Accessibility.** The card merges into one element with a **polite live region**, so TalkBack reads it once when it appears (or changes) without interrupting: the role's `severityLabel` as the glyph's description ("Success", "Warning", "Error"; none for `Info`), the title, then the message. It carries a `dismiss` semantics action. An actionable banner is one button. The timeout counts icons and, when actionable, controls.

## `AMToast` (`Toast.kt`)

```kotlin
var toast by remember { mutableStateOf<AMToastItem?>(null) }
AMButton("Copy link", onClick = { copy(); toast = AMToastItem("Link copied") })
AMToast(toast, onDismissRequest = { toast = null })
```

- **`AMToastItem(text, duration = 2.seconds)`**: neutral, no role, no glyph, no action. Status news with a role is a banner.
- **Material's snackbar as it is.** The host owns a `SnackbarHostState` and a Material `SnackbarHost` with its default `Snackbar(data)`: inverse surface, Material's shape, padding and motion (fade and scale), its polite live region and its dismiss action. `ToastVisuals` gives the text, no action label, no dismiss button and `SnackbarDuration.Indefinite`, since the toast host does the timing.
- **Placement.** The snackbar host sits at the bottom center of the host's layer, padded by `WindowInsets.safeDrawing` (bottom and horizontal sides), so it's above the navigation bar, and above the keyboard while it's open, then by the internal `ToastHost`'s `bottomClearance`: 0 for `AMToastHost` and a sheet, and in an `AMScene` with `floatingActions` the button's 56 dp plus its `spacing.lg` gap, so the toast shows just above the button's resting place, as Material's scaffold places a snackbar above its floating action button. The clearance follows the resting place, not a button the user has moved.
- **Timing.** A `LaunchedEffect` keyed on the current posting calls `showSnackbar` inside `withTimeoutOrNull(timeout)`, then calls `onDismissRequest`: the snackbar comes down when the time is up (the cancelled call removes it) or when a screen reader dismisses it, and the toast asks to go either way. A newer posting, or none, cancels the effect, which takes the snackbar down without asking.

## `AMLoadingOverlay` (`LoadingOverlay.kt`)

```kotlin
var saving by remember { mutableStateOf(false) }
AMButton("Save", onClick = { scope.launch { saving = true; save(); saving = false } })
AMLoadingOverlay(saving, message = "Saving…")
```

- **`AMLoadingOverlay(visible, message = null)`**: a window, the Android way. While `visible`, a Compose `Dialog` with `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)`: the platform's dialog dim behind it. `onDismissRequest` does nothing; a tap outside does nothing; the dialog's back callback consumes back (and predictive back), so nothing under it moves. Only the caller ends it, by turning `visible` false.
- **The card.** The dialog's content fills the window (so the card's shadow has room on every side) and centers a full-width card, inset `spacing.xl` at the sides: `Modifier.amCard(padding = spacing.lg)`, holding Material's indeterminate `CircularProgressIndicator` over the message (`bodyMedium`, `onSurfaceVariant`, centered), `spacing.md` apart. A null message shows "Loading", so the indicator always has a caption. The card merges into one element; the dialog is a modal window, so TalkBack stays inside it.
- **Reports itself.** It calls `ReportOverlayUp(visible)`, so a scene's floating action button fades while it's up.

## `AMEmptyState` (`EmptyState.kt`)

```kotlin
AMEmptyState(
    painterResource(R.drawable.ic_inbox), "No messages",
    description = "New messages will appear here.",
    action = AMEmptyStateAction("Compose", painterResource(R.drawable.ic_edit_square)) { compose() },
)
AMEmptyState(painterResource(R.drawable.ic_group), "No contacts", description = "Add people to start a conversation.") {
    AMButton("Add contact", onClick = ::addContact, style = AMButtonStyle.Primary())
    AMButton("Import contacts", onClick = ::importContacts, style = AMButtonStyle.Ghost)
}
```

- **Two forms.** String: `AMEmptyState(icon, title, modifier, description: String? = null, action: AMEmptyStateAction? = null)`; the one action shows as an `AMButton` forced to `AMButtonStyle.Primary()`, its glyph before its label. Slot: `AMEmptyState(icon, title, modifier, description, actions: ColumnScope slot)`, for several buttons or other styles; it doesn't restyle them (each takes its own `style`, or `LocalAMButtonStyle`). Both go through private `EmptyStateLayout`, which takes the actions as a nullable slot. The slot form's `actions` is required, so the two never overlap.
- **`AMEmptyStateAction(label, icon: Painter? = null, onClick)`**: an `@Immutable` action payload, like `AMSwipeAction`.
- **Look.** A column filling the width, padded `spacing.xl` above and below (no side padding: the caller places it), everything centered: the glyph at 48 dp in `onSurfaceVariant`, `spacing.md`, the title (`titleLarge`, `onSurface`), `spacing.xs`, the description (`bodyMedium`, `onSurfaceVariant`), all text centered; then `spacing.lg` and the actions in a centered column `spacing.sm` apart.
- **On the page, never on a card.** It draws no surface: the glyph and text lie on whatever is behind them (the page color, the app's backdrop, a search window), and only its buttons float. Put it where the content would have been, not in a card.
- **Accessibility.** The glyph is decorative (no description). The glyph, title and description merge into one element whose title carries heading semantics, so TalkBack reads the title and description together as a heading; each button is its own element.

## `AMSkeleton` and `Modifier.amShimmer()` (`Skeleton.kt`)

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
    AMSkeleton.Circle()
    AMSkeleton.Text(Modifier.weight(1f), lines = 2)
}
AMSkeleton.Rect(height = 160.dp, shape = MaterialTheme.shapes.medium)
AMSectionHeader { if (loading) AMSkeleton.Text(month.title) else Text(month.title) }
```

- **`AMSkeleton`** is an object holding the placeholder shapes, each a `Box` filled with the color scheme's `surfaceContainerHighest` in its shape and wearing `amShimmer`. A loading state is laid out from these shapes the way the content will sit, then swapped for the content.
  - `Rect(modifier, height = 14.dp, shape = shapes.small)`: fills the width it's given (`modifier` first, then `fillMaxWidth`), so the caller sizes it with `width`, `fillMaxWidth(fraction)` or a weight.
  - `Circle(modifier, diameter = 40.dp)`: an avatar's or glyph's place, `CircleShape`.
  - `Text(modifier, lines = 3, lineHeight = 12.dp, spacing = spacing.sm)`: full-width rounded bars (`CircleShape`), the last one 60 % as wide when there's more than one, so it reads as a paragraph. The shimmer sits on the whole column, so one band sweeps every line together.
  - **`Text(text, modifier, style = LocalTextStyle.current, minLines = 1, maxLines = Int.MAX_VALUE)`**, the text in hand: the loading layout's place for a real text (a heading, a name, a count). It's Compose's `BasicText` with that text, style and line limits (`TextOverflow.Clip`), so it takes the text's exact size and line height and wraps the same way, and swapping the real `Text` in moves nothing. A `drawWithContent` that never draws the glyphs draws instead, from the `TextLayoutResult` (`onTextLayout`, held in state), one rounded bar per line (`drawTextBars`): from the line's left to its right (`getLineLeft` / `getLineRight`, so as wide as its words), the line's height less 15 % above and below (`TextBarInset`), fully rounded, in `surfaceContainerHighest`. Lines `minLines` reserves past the text's own keep their room with no bar. It wears `amShimmer` and `clearAndSetSemantics {}`, so TalkBack hears nothing. The default style is the `LocalTextStyle` a container provides (a section header's slot, a list item's headline), so it matches a `Text` in the same place.
  - They take no input and carry no semantics.
- **`Modifier.amShimmer()`**: `graphicsLayer(compositingStrategy = Offscreen)` then private `ShimmerNode` (a `DrawModifierNode`). The node draws the content, then a white band over it with `BlendMode.SrcAtop`, so the band lights only the pixels the content drew (its shapes, not the gaps); the offscreen layer isolates the blend, cuts the content to its bounds (so it goes on placeholders inside a card, never on a card with a shadow) and is the layer the node re-records each frame, so nothing recomposes and nothing around it redraws.
- **The band**: a horizontal gradient, clear to white at 0.3 to clear, `max(40 % of the width, 60 dp)` wide; it enters fully off the start and leaves fully off the end, so the wrap never shows. The brush is cached per band width. The recipe (cycle, band, highlight) is private constants in `Skeleton.kt`, not theme values, so a loading rhythm doesn't drift when an app re-tunes its theme.
- **Motion.** The component's own: a linear sweep every 1.2 s, looping, a fixed loop like Material's own indeterminate indicators (the motion scheme's springs don't loop). The node launches it on attach in its `coroutineScope` (cancelled on detach) and reads each frame with `withInfiniteAnimationFrameNanos`; the phase is the frame time modulo the cycle, so every shimmer on screen sweeps in step. It reads `MotionDurationScale` from its coroutine context: the cycle stretches with the animator duration scale, and at 0 (**Remove animations**) the band goes away and the node waits (`snapshotFlow { scaleFactor }.first { it > 0 }`) until animations return, drawing the content still.

## `AMProgressView` (`ProgressView.kt`)

```kotlin
AMProgressView("Checking for new mail")                                          // indeterminate spinner in a card
AMProgressView("Uploading", progress = { 0.6f }, valueLabel = "60%", style = AMProgressViewStyle.Bar)
```

- `AMProgressView(label, modifier, progress: (() -> Float)? = null, valueLabel: String? = null, style = Spinner())`. `progress` is Material's deferred read (0 to 1); null runs the indicator indeterminate. The `label` is required: progress always says what's happening.
- **`AMProgressViewStyle`**, sealed: `Spinner(card = true)` (an `@Immutable` class with value equality) and `Bar` (a `data object`).
- **Spinner**: Material's `CircularProgressIndicator` (determinate or indeterminate) over the label as a caption (`bodySmall`, `onSurfaceVariant`, centered), `spacing.md` apart, centered in a column. With `card` (the default), the column fills the width in `amCard(padding = PaddingValues(horizontal = xl, vertical = lg))`, the way a loading view shows it on the page; inside another card that card lies flat by the nesting rule. `card = false` places it inline, for a card or strip that already holds it. It shows no value label.
- **Bar**: on the row recipe and the flat-row surface (`amFlatSurface` then `amRowPadding`: the floating surface on the page, nothing inside a card), a column `spacing.sm` apart of a row (the label in `bodyLarge` / `onSurface`, weighted, and the value label in `bodyMedium` / `onSurfaceVariant` at the end, `spacing.md` from it, both aligned by baseline) over Material's `LinearProgressIndicator`, full width. Bars stack in a card with `AMDivider(inset = spacing.lg)` between, like rows.
- **Native.** The indicators keep Material's size, colors (`primary` on its track), gap and stop indicator, and its indeterminate motion. Each view merges into one TalkBack element: the label, the value label and the indicator's progress.

## Catalog pages

`demo/.../catalog/pages/feedback/`, registered in `FeedbackPages.entries` in this order (ids): `banner`, `toast`, `loadingOverlay`, `progress`, `skeleton`, `emptyState`. Each demo sits inline on its page; the Banner, Toast and Loading overlay demos' own buttons present the message or overlay. Banners and toasts show in the page's screen host (`CatalogScreenHost`, which `ComponentPage` wraps every page in), so they cover the whole screen, top app bar included; the loading overlay is its own window.

| Page | Preview | Loaded |
|---|---|---|
| Banner | "Show banner" (full width): "Changes saved" / "Your profile is up to date.", Success | a "Project name" field (`AMTextField`, placeholder "Untitled") in a padded card, then "Suggest a name", which shows an Info banner "Try “Harbor Lights”" / "Tap to use this name." for 8 s; tapping it fills the field |
| Toast | "Show toast" (full width): "Link copied" | a card of two tracks (hand-built rows: Harbor Lights / The Lanterns, Low Tide / Marlow Fields), then "Add to queue": "Added to queue" for 4 s |
| Loading overlay | "Show loading overlay" (full width): "Loading…" for 2 s | a 240 dp `CatalogArt.Forest` photo in `shapes.large`, a "Caption" field ("Morning light") in a padded card, then "Share post": "Uploading photo…" for 2 s |
| Progress | `FullBleed` surface strip: an inline spinner "Checking for new mail" (padded `spacing.lg`) over a bar "Uploading" at 60 % | a sync card: bars Photos (done), Videos (35 %), Documents (waiting) with inset dividers, then an inline spinner "Checking for new files" |
| Skeleton | `FullBleed` surface strip padded `spacing.lg`: a contact row loading (the real avatar, "Ada Lovelace"'s initials on her color, wearing `amShimmer`, beside the text placeholders of "Ada Lovelace" in `bodyLarge` over "Mathematician and writer" in `bodyMedium`, `spacing.xs` apart) | a post card padded `spacing.lg` loading: the contact row, a 160 dp block in `shapes.medium`, three lines of text |
| Empty state | "No messages" / "New messages will appear here." with the inbox glyph and a Compose action | "No contacts" / "Add people to start a conversation." with the group glyph, Add contact (Primary) over Import contacts (Ghost) |

- The fields are `AMTextField`s on a `TextFieldState` in `amCard(padding = spacing.lg)`, as the Form actions page does.
- The stand-in async work is a private `suspend fun` that waits 2 s, launched from the button in a `rememberCoroutineScope`.
- **Empty states lie on the page.** The Empty state page's demos sit on the bare page in the gutters, with no strip or card behind their text: that's the pattern itself (`Patterns.md`). The catalog's search shows an `AMEmptyState` when nothing matches, and the Calendar page shows one for a day with no events.
- Demo glyphs these pages add: `ic_top_panel_open` (Banner), `ic_bottom_panel_open` (Toast), `ic_hourglass_empty` (Loading overlay), `ic_progress_activity` (Progress), `ic_segment` (Skeleton). Empty state uses `ic_inbox` (its entry icon and Preview), `ic_edit_square` and `ic_group`.
