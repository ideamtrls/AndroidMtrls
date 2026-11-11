---
scope: The minimal test policy — JUnit for pure logic in the library and the demo, and verifying a change by building it and looking at the demo on an emulator in light and dark.
globs: [androidmtrls/build.gradle.kts, androidmtrls/src/test/**, demo/build.gradle.kts, demo/src/test/**]
keywords: [tests, testing, JUnit, JUnit 4, unit tests, pure logic, testDebugUnitTest, src/test, ImagePipelineTest, ImageDiskCacheTest, FrostedSampleRegionTest, KeepsPlaceBackTest, LightStatusBarClaimsTest, CalendarMonthCellsTest, ListsReorderMathTest, ListsLabeledContentWidthTest, CollectionsIndexClampTest, CollectionsCardStackRulesTest, DataDisplayMathTest, MusicId3Test, demo tests, runBlocking, verification, verify, emulator, screenshots, screencap, light, dark, artwork night, font scale, RTL, no golden tests, no screenshot tests, no benchmarks, no semantics tests]
updated: 2026-11-10
---

# Testing

Testing is minimal on purpose. A change is checked by building it and looking at it.

## Unit tests — JUnit, for pure logic only

- The library's unit tests are JUnit 4 (`testImplementation` in `androidmtrls/build.gradle.kts`), in `androidmtrls/src/test/kotlin/`, in the library's package. Coroutine code is tested with `runBlocking` from kotlinx-coroutines, which Compose already brings.
- They cover **pure logic only**: math and rules that run without an Android device or a Compose runtime, such as index clamping, an avatar's hashed hue, a card stack's commit rule, or an image cache's bookkeeping.
- The tests that exist, in `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/`:
  - `ImagePipelineTest`: the image pipeline's decode sizing and its caps (an extreme aspect both ways, a huge square under both fits, a zero frame, ordinary sizes unchanged, the caps holding across a sweep of sources, frames and fits) and when a decode covers a frame (a capped one covers every frame that needs more), the bounded body read (up to the limit, one byte over, a wrong announced length, an endless body stopping one byte past the limit), the fetch deadline cutting off a blocking step an interrupt doesn't stop (as a failure, not a cancellation) and leaving one done in time alone, its memory cache's keys and URL index, its shared loads' cancellation, and a failed load's retry (`DataDisplay.md`).
  - `ImageDiskCacheTest`: the disk cache's file names (the URL's SHA-256 in lowercase hex, against reference digests; temporary and stray names aren't entries) and its trim choice (nothing while the files fit, least recently used first until the rest fit, ties by name) (`DataDisplay.md`).
  - `FrostedSampleRegionTest`: the rectangle a frosted surface samples from its art, widened by twice the blur, cut to the art's bounds and rounded out to whole pixels (`frostedSampleRegion`, `Foundations.md`).
  - `KeepsPlaceBackTest`: the back-to-landing decision behind `AMKeepsPlace` and `AMScene` — when back swaps to the landing root and when it falls through (`Navigation.md`).
  - `LightStatusBarClaimsTest`: the per-window claim count behind `AMLightStatusBarIcons` and `AMArtPanelChrome` — the first claim records the window's icons, the last one out restores them, so a covered panel leaving never undoes the one on top (`Navigation.md`).
  - `selection/CalendarMonthCellsTest`: the month grid's cells — blanks before the 1st back to the week's first day (for every first weekday), whole weeks after, each day exactly once, from four-week to six-week months (`Selection.md`).
  - `ListsReorderMathTest`: the reorderable lists' move step, the lifted item's clamp to the list window, auto-scroll speed, and the swipe strip in both layout directions (`Lists.md`).
  - `ListsLabeledContentWidthTest`: how wide a labeled row's label is laid out beside its value — its own width, never below its longest word, never wider than the room (`Lists.md`).
  - `DataDisplayMathTest`: the avatar's hashed hue (against an exact 64-bit reference) and initials, timeline marker positions, and the art pictures' keys (an app bitmap's identity key among them), sizing, crop, blur, fade and row bands (`DataDisplay.md`).
  - `collections/CollectionsIndexClampTest`: the shared index clamp (`clampIndex`), including empty collections (`Collections.md`).
  - `collections/CollectionsCardStackRulesTest`: the card stack's rules — when a drag commits, the drawn window, stepping and wrapping, and that no card identity is ever drawn twice across a long walk over many deck shapes (`Collections.md`).
- Run them with `./gradlew :androidmtrls:testDebugUnitTest --console=plain` (environment in `Build.md`).
- The demo has one, for its own pure logic: `MusicId3Test` (`demo/src/test/kotlin/com/ideamtrls/androidmtrls/demo/`), the Music scene's ID3v2 cover reader over tags built byte by byte — v2.2/v2.3/v2.4 frames, syncsafe sizes, unsynchronisation, extended headers, picture types and description terminators (`SceneMusic.md`). Run it with `./gradlew :demo:testDebugUnitTest --console=plain`.

## What there isn't

No golden or screenshot tests, no benchmarks, no semantics or Compose UI test suites, and no third-party test tools.

## Verifying a change

1. **Build**: `./gradlew :androidmtrls:assembleDebug :demo:assembleDebug --console=plain` succeeds with no warnings.
2. **Run the demo on an emulator**: install it, then open the page the change touches (`Build.md`):
   ```bash
   adb shell am start -S -n com.ideamtrls.androidmtrls.demo/.MainActivity --es entry <id>
   ```
3. **Screenshots in light and dark**, and look at them:
   ```bash
   adb shell am start -S -n com.ideamtrls.androidmtrls.demo/.MainActivity --es entry <id> --es appearance light
   adb exec-out screencap -p > .tmp/<id>-light.png
   adb shell am start -S -n com.ideamtrls.androidmtrls.demo/.MainActivity --es entry <id> --es appearance dark --es artwork night
   adb exec-out screencap -p > .tmp/<id>-dark.png
   ```
   Add `--es scrollTo loaded` for the Loaded composition, `--es fontScale 2` for a large font scale, and `--es rtl true` for right to left when the change could affect them.
