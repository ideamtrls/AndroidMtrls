---
scope: The minimal test policy and its mechanics — JUnit for pure logic in the library and the demo, instrumented Compose UI tests for what only a device shows, the test dependencies and Gradle tasks, and checking a change by building it and screenshotting the demo on an emulator in light and dark.
globs: [androidmtrls/build.gradle.kts, androidmtrls/src/test/**, androidmtrls/src/androidTest/**, demo/build.gradle.kts, demo/src/test/**]
keywords: [tests, testing, JUnit, JUnit 4, unit tests, pure logic, internal, testImplementation, testDebugUnitTest, src/test, runBlocking, TemporaryFolder, ImagePipelineTest, ImageDiskCacheTest, FrostedSampleRegionTest, LayoutCardSliceTest, KeepsPlaceBackTest, StatusBarClaimsTest, CalendarMonthCellsTest, ActionGridGeometryTest, ButtonPairTest, ButtonStyleTest, ListsReorderMathTest, ListsLabeledContentWidthTest, ArtWashTest, SurfaceLookTest, DataDisplayMathTest, CollectionsIndexClampTest, CollectionsCardStackRulesTest, MusicId3Test, demo tests, instrumented tests, androidTest, androidTestImplementation, connectedDebugAndroidTest, testInstrumentationRunner, AndroidJUnitRunner, androidx.test runner, ui-test-junit4, ui-test-manifest, espresso-core, Compose UI test, createAndroidComposeRule, ComponentActivity, mainClock, performTouchInput, SystemBarsAppearanceTest, TransientOverSheetTest, RowSemanticsTest, PanelBackTest, ReorderDragTest, verification, verify, emulator, screenshots, screencap, light, dark, artwork night, surfaces, font scale, RTL, no golden tests, no screenshot tests, no benchmarks]
updated: 2026-11-15
---

# Testing

Testing is minimal on purpose: a change is checked by building it and looking at it; unit tests pin pure logic, and a few instrumented tests pin what only a device shows.

## Where things live

- The library's unit tests: `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/`, with `selection/` and `collections/` subfolders, all in the library's package.
- The library's instrumented tests: `androidmtrls/src/androidTest/kotlin/com/ideamtrls/androidmtrls/`.
- The demo's unit test: `demo/src/test/kotlin/com/ideamtrls/androidmtrls/demo/MusicId3Test.kt`.
- Test dependencies and the runner: `androidmtrls/build.gradle.kts` (`testImplementation`, `androidTestImplementation`, `testInstrumentationRunner`) and `demo/build.gradle.kts` (`testImplementation`); their versions are in `gradle/libs.versions.toml` (`Build.md`).

## Unit tests — JUnit, for pure logic only

- JUnit 4 (`testImplementation(libs.junit)` in both modules), on the JVM: no device, no composition, no window. Compose's value types (`Dp`, `IntSize`, `Size`, `Rect`, `Color`) are fine in them.
- They cover **pure logic only**: math and rules factored out of the composables into `internal` functions and plain classes (`clampIndex`, `cardSlice`, `StatusBarClaims`, `surfaceLook`), which the tests call directly from the library's package. Logic worth pinning gets that shape first.
- Coroutine code runs under `runBlocking` from kotlinx-coroutines, which Compose brings; files come from JUnit's `TemporaryFolder` rule.
- The tests, in `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/`:
  - `ImagePipelineTest`: the image pipeline's sources (`http` and `https` to the network whatever their case; local sources off by default, a `file` URL and a `content` URI then refused like any unsupported scheme; with them on, a regular file by its canonical path, a `content` URI through the resolver, and a directory, a missing file, a link to a directory, a device or a `file` URL naming no local path refused; any other scheme or none refused; the app's loader first for every scheme, null falling through, its exception failing the load), the `User-Agent` (the default's form; an app's value printable ASCII and not blank, a bad one refused and the last kept), `decodeSize` and its caps (16 megapixels, 8192 a side) and `decodeCovers`, the bounded body read, the fetch deadline cutting off a step an interrupt doesn't stop (as a failure, not a cancellation), the memory cache's keys and per-URL latest, shared loads' cancellation, and a failed load's retry (`DataDisplay.md`).
  - `ImageDiskCacheTest`: the disk cache's file names (the URL's SHA-256 in lowercase hex, against reference digests; temporary and stray names aren't entries) and its trim choice (nothing while the files fit, least recently used first until the rest fit, ties by name) (`DataDisplay.md`).
  - `FrostedSampleRegionTest`: the rectangle a frosted surface samples from its art (`frostedSampleRegion`): grown by a margin on every side (its callers pass twice the blur radius), cut to the art's bounds, widened to whole pixels, null off the art (`Foundations.md`).
  - `LayoutCardSliceTest`: a lazy card's row slices — the card rect and the band each slice shows for a single, first, middle and last row, neighbors' bands meeting with no gap or overlap, and the reach clearing every elevation's shadow plus a card's corners (`cardSlice`, `cardSliceReach`, `Layout.md`).
  - `KeepsPlaceBackTest`: the back-to-landing decision behind `AMKeepsPlace` and `AMScene` — when back swaps to the landing root and when it falls through (`Navigation.md`).
  - `StatusBarClaimsTest`: the per-window claim count behind `AMStatusBarIcons` and the art surfaces' own claims — the first claim records the window's icons, the last one out restores them, so a covered view leaving never undoes the one on top (`Navigation.md`).
  - `selection/CalendarMonthCellsTest`: the month grid's cells — blanks before the 1st back to the week's first day (for every first weekday), whole weeks after, each day exactly once, from four-week to six-week months (`Selection.md`).
  - `ActionGridGeometryTest`: where the remote's grid puts its rows — the bottom row on the bottom padding, each row above a tile and a gap higher, rows counted from 0 (`Actions.md`).
  - `ButtonPairTest`: when a button pair splits — each button half the row less the gap, rounding down; two labels that fit split, one that fits exactly splits, either too wide stacks (`Actions.md`).
  - `ButtonStyleTest`: a filled `AMButtonStyle`'s equality — the same kind at the same elevation is the same style, hash included; another elevation or kind isn't; `toString` names only the elevation (`Actions.md`).
  - `ListsReorderMathTest`: the order a drag shows while the caller's catches up, the reorderable lists' move step, the lifted item's clamp to the list window, auto-scroll speed, and the swipe strip in both layout directions (`Lists.md`).
  - `ListsLabeledContentWidthTest`: how wide a labeled row's label is laid out beside its value — both on one line when they fit, else the shorter (up to half the row) keeps its line and the other wraps, half each when both are long, neither broken inside a word, never wider than the room (`Lists.md`).
  - `ArtWashTest`: the art's frost toward the mode (black and white covers becoming `#B3B3B3` and `#4D4D4D`, every pixel mixed evenly, only where there's picture, hue kept, a wash of 0 left alone); the full recipe's extremes over every sRGB blend of a palette (each color also saturated as the art is) pinned to `Style.ArtInk.LightArtGround` and `DarkArtCeiling`; the fixed inks per tone and their contrast over them (8.42:1 and 5.54:1 on dark art, 9.98:1 and 6.62:1 on light art); ink moved to a luminance landing on it, and colored ink moved just far enough to hold on art while the fixed secondaries and a color that holds stay; the accent on frost (Material's baseline `primary` unchanged in both modes, a lighter brand accent moved to 4.5:1 keeping its hue); frost over the page being the page made translucent (`#FAFAFC` light, `#0A0A0B` dark); text holding on the tint, on frost over the page and on frost over a blurred window's wash over the worst screen (`tintedGround`, `windowFrostGround`); chip labels on the quiet chip, frost and the tint, and a chosen chip's on `primary`; and the neutral tag's label on frost and the tint (`frostedTagFill`) (`DataDisplay.md`, `Foundations.md`, `Selection.md`).
  - `SurfaceLookTest`: the app's setting deciding a surface's look — solid in a solid app wherever it stands, frosted on the backdrop and tinted where it's embedded in a frosted app (`surfaceLook`); a text field's container — the tint off the page in a frosted app, solid on a sheet in a solid app, otherwise the field's own (`fieldContainer`, `Inputs.md`); and a frosted surface's path — the art sampled once placed and frost over the page until then outside a blurred window, frost over the window's blur in one, and solid there while cross-window blur is off (`frostPath`) (`Foundations.md`).
  - `DataDisplayMathTest`: the avatar's hashed hue (against an exact 64-bit reference) and initials, timeline marker positions, and the art pictures' keys (an app bitmap's identity, the mode and the wash among them), working size, crop, area average, blur, fade and row bands (`DataDisplay.md`).
  - `collections/CollectionsIndexClampTest`: the shared index clamp (`clampIndex`), including empty collections (`Collections.md`).
  - `collections/CollectionsCardStackRulesTest`: the card stack's rules — when a release commits, the drawn window, stepping and wrapping, the identities cards are drawn with and the bookkeeping that carries them between compositions, and that no identity is ever drawn twice across a long walk over many deck shapes (`Collections.md`).
- Run them with `./gradlew :androidmtrls:testDebugUnitTest --console=plain` (environment in `Build.md`).
- The demo has one, for its own pure logic: `MusicId3Test`, the Music scene's ID3v2 cover reader over tags built byte by byte — v2.2/v2.3/v2.4 frames, syncsafe sizes, unsynchronisation, extended headers, picture types and description terminators (`SceneMusic.md`). Run it with `./gradlew :demo:testDebugUnitTest --console=plain`.

## Instrumented tests — Compose UI tests, for what only a device shows

- Jetpack's Compose UI tests on the `androidx.test` runner: the library's `defaultConfig` sets `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`, and its `androidTestImplementation` brings `androidx.compose.ui:ui-test-junit4` and `ui-test-manifest` (which declares the host `ComponentActivity`) through the Compose BOM, the `androidx.test` runner, `espresso-core` 3.7.0, and Navigation 3 UI to host panels. Compose's test rule idles through Espresso, and the 3.5.0 it brings calls an input API current Android lacks; hence the pin.
- They're for behavior pure logic can't reach: real input, real windows, the semantics TalkBack reads, and a real `NavDisplay`.
- Each test hosts the composable in `createAndroidComposeRule<ComponentActivity>()` (the `v2` rule), drives time with `mainClock` where timing matters, and taps by screen position (`onRoot().performTouchInput { click(spot) }`) when what's under the finger is the point.
- The tests, in `androidmtrls/src/androidTest/kotlin/com/ideamtrls/androidmtrls/`:
  - `SystemBarsAppearanceTest`: the window's bar icons under `AMTheme` — the outermost theme sets both light-icon bits for its appearance and again when it changes, a nested theme leaves them alone, `AMStatusBarIcons` turns the status bar's icons over for art of the other tone in each mode (and does nothing for the mode's tone), and an `AMBackdrop` given the dark tone turns them light in light mode while it reaches the top, not when it lies below it (`Foundations.md`, `Navigation.md`).
  - `TransientOverSheetTest`: toasts and banners while an `AMSheet` is up in an `AMScene` — the screen's toast shows in the sheet's window and back in the screen's when the sheet goes; the screen's banner and the sheet's own come down in a popup window over the sheet; the newest posting shows, wherever it was posted (`Feedback.md`).
  - `RowSemanticsTest`: what TalkBack finds on rows — an `AMListRow`'s and an `AMListCard`'s tap carries its `onClickLabel` (TalkBack's action and a touch each tap once; without a label Material's tap stays), a bar `AMProgressView` in an `AMSwipeableRow` is the one stop carrying the swipe actions, reading its label, and a held row's actions come first in one list on the row: hold then swipe in an `AMSwipeableRow`, hold then "Move up", "Move down" then swipe in an `AMReorderableRowList`, a hold's action running from TalkBack (`Feedback.md`, `Lists.md`, `Overlays.md`).
  - `PanelBackTest`: a panel's back button under a real `NavDisplay` — tapped twice at the same spot while its panel pops, `AMPanel`'s arrow and `AMArtPanelChrome`'s button pop once (one panel deep, a second pop would empty the back stack); tapped while the panel slides in, it pops (`Navigation.md`).
  - `ReorderDragTest`: dragging in an `AMReorderableRowList` with real touches, frames advanced by hand while a row is lifted (its auto-scroll waits on every frame) — a drag from the handle steps past rows the caller hasn't moved yet; a hold off the handle lifts a row by default, and with `holdToLift` false is the row's `amHoldable`'s alone while the handle still lifts; in a scrolling page of art rows, in a frosted card, the list runs its length, a row held at the page's edge scrolls the page past every row, and a row keeps its moves and its swipe (`Lists.md`, `Overlays.md`).
- Run them with an emulator or device attached: `./gradlew :androidmtrls:connectedDebugAndroidTest --console=plain` (environment in `Build.md`).

## What there isn't

No golden or screenshot tests, no benchmarks, and no instrumented tests in the demo. JUnit is the one test library from outside AndroidX; there are no mocking or assertion libraries.

## Verifying a change

1. **Build**: `./gradlew :androidmtrls:assembleDebug :demo:assembleDebug --console=plain` succeeds with no warnings.
2. **Run the demo on an emulator**: install it (`./gradlew :demo:installDebug --console=plain`), then open the page the change touches:
   ```bash
   adb shell am start -S -n com.ideamtrls.androidmtrls.demo/.MainActivity --es entry <id>
   ```
3. **Screenshots in light and dark**, and look at them:
   ```bash
   adb shell am start -S -n com.ideamtrls.androidmtrls.demo/.MainActivity --es entry <id> --es appearance light
   adb exec-out screencap -p > <path>/<id>-light.png
   adb shell am start -S -n com.ideamtrls.androidmtrls.demo/.MainActivity --es entry <id> --es appearance dark --es artwork night
   adb exec-out screencap -p > <path>/<id>-dark.png
   ```
   Add `--es scrollTo loaded` for the Loaded composition, `--es surfaces frosted` for the frosted surface setting (the catalog's default is solid), `--es fontScale 2` for a large font scale, and `--es rtl true` for right to left when the change could affect them.

## See also

- `Build.md`: the environment variables and the versions.
- `Demo.md`: every launch extra (`LaunchOptions`).
