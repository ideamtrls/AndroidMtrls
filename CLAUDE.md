# AndroidMtrls

A Jetpack Compose design system for Android: visual components (actions, inputs, selection,
navigation, layout, lists, feedback, overlays, data display, collections) and the style values
behind them. No app logic, no data. Targets **Android 16 (API 36) and later, phones only**; the
library is **pre-release** (breaking parameter reorders are allowed), and **every public symbol
AndroidMtrls adds carries the `AM` prefix, or `Modifier.am…` for modifiers**.
Library sources live in one package, `com.ideamtrls.androidmtrls`, grouped in area folders under
`androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/<area>/`; the catalog app is `demo/`.

**Two layers.** The native layer is Android's own: Jetpack and Material 3 components with their own
shapes, type, motion and behavior. The style layer sits on top: floating surfaces on a tuned page,
two-layer shadows and hairlines, cards, the remote, the spotlight, no dimming behind sheets, folding
and quiet defaults. The screen is a backdrop layer (the app's backdrop, a sheet's background, the
blurred screen behind the remote and the spotlight: always frosty) and a surface layer on it, made
solid or frosted by one setting, `AMTheme(surfaceFill = …)`. Stateless surfaces wear the style;
native controls keep Material's shapes, outlines and behavior and take the model's container colors.
When in doubt, lean Android.

**Latest Android only.** minSdk 36 (Android 16) is the floor and is never lowered. No
`Build.VERSION.SDK_INT` checks, `@RequiresApi`, compat shims or fallbacks for older releases;
use the newest platform and Jetpack APIs the floor allows.

**Standalone.** AndroidMtrls is complete in itself and knows of no other library. Its code,
docs, digests, tests and commit messages never name, compare with or follow another
implementation; every decision is argued from Android and this library's own patterns.

<!-- digestmtrls:start -->

## Digest

This project's working context lives in `Digest/`, with one file per category of context. The table below maps task level concerns to the context file(s) that cover it.

**IMPORTANT: Before starting a task, scan this table. If any rows match the task, read the relevant digest files, and then start the task. If you're unsure about a row, read only the first 6 lines of its file, the frontmatter, and decide from those.** Read only the rows that match. Context window efficiency is gained by not reading irrelevant context; if nothing matches, proceed without reading any.

**IMPORTANT: Before finishing a task that changed how an area works or where its code lives, update that area's digest file.** Keep edits surgical and bump `updated`. Digest files describe the current state of the system — what *is*, never what *was*. Do not narrate the change ("previously", "no longer", "moved from…"); rewrite the affected text as if the current state had always been true. If the work created a new area with no matching file, tell the user rather than inventing a category.

| Digest file | Read this when… |
| ----------- | --------------- |
| `Digest/Conventions.md` | adding or changing ANY public API — naming, the `AM` / `Modifier.am…` prefix, parameter order and names, string and slot overloads, closed style types, hoisted state, `enabled` and `LocalAMEnabled`, public value types and `@Immutable`, public modifiers as `Modifier.Node`s, 48 dp touch targets, ripple, glyphs, the native behavior the library keeps, or the motion policy |
| `Digest/Patterns.md` | composing screens or building an app or scene — the framework's opinions: the two layers and the one surface setting, floating surfaces, color under cards and text over art, the status bar, the remote as main navigation, the spotlight and pickers, sheets without dimming, folding, press and hold, quiet defaults |
| `Digest/Foundations.md` | touching the style values, colors, spacing, shapes, type roles, elevation and the shadow table, hairlines and the elevated-surface recipe, the frosted fill, `AMBackdrop` and frosted sampling, the app's surface setting (`AMSurfaceFill`, `AMTheme.surfaceFill`) and the tint embedded pieces wear (`AMTheme.Frosted.embeddedFill`), `AMTheme`/`AMTokens`, `AMSemantic`, the motion helpers, the page and section headers, or the soft edge under the status bar (`Modifier.amStatusBarEdge`) — anything in the library's `foundations/` folder, or the Foundations catalog pages |
| `Digest/Actions.md` | working on buttons and button styles (`AMButton`, `AMButtonStyle`, `LocalAMButtonStyle`, their shape), the button pair (`AMButtonPair`), the floating action button and its hub (`AMFloatingActions`, the scene's remote), icon/card/action-grid buttons, `AMActionGrid`/`AMFormActions`/`AMCardActionRow`, or how a container disables the controls inside it (`LocalAMEnabled`, `foundations/Enabled.kt`) — anything in the library's `actions/` folder, or the Actions catalog pages |
| `Digest/Inputs.md` | working on text fields, the secure and search fields, the text editor, autofocus and the caret, input filtering (`inputTransformation`), toggles, sliders, steppers, the date picker and its spotlight, or how a field's container follows the app's surface setting (solid, or the tint) — the library's `inputs/` folder, or the Inputs catalog pages |
| `Digest/Selection.md` | working on choosing among options — the segmented control (`AMSegmentedControl`), the menu picker and its options in the spotlight (`AMMenuPicker`), chip groups and the flow layout (`AMChipGroup`, `AMFlowLayout`), the month calendar (`AMCalendar`), the selection mark on photo tiles (`AMSelectionMark`, `Modifier.amSelectionCheck`), or the backings of the segmented control and chips in the app's surface setting (the tint, the quiet unselected chip) — the library's `selection/` folder, or the Selection catalog pages |
| `Digest/Navigation.md` | working on the scene root (`AMScene`: the remote's slot, the scene-wide `overlays` slot, the overlay registry, the banner and toast hosts, back to the landing view), panels on Navigation 3 (`AMPanel`, `AMPanelChrome`, `AMArtPanelChrome` over art), `AMKeepsPlace` and back to the landing view, the status bar's icons following the art (`AMStatusBarIcons`), toolbars and their buttons (`AMToolbar`, `AMToolbarButton`, a chip over art in the app's surface setting), in-page tabs (`AMTabs`), or page dots (`AMPageIndicator`) — the library's `navigation/` folder, or the Navigation catalog pages |
| `Digest/Layout.md` | working on `Modifier.amCard()`, the row recipe (`Modifier.amRowPadding()`, `AMRowDefaults` for `ListItem` rows, row heights), `AMSection` and its lazy form (`LazyListScope.amSection`), `AMDisclosureGroup`, `AMDivider`, the flat strip self-filled blocks wear (`amFlatSurface`), or how a card demotes inside another card (`AMSurfaceStyle`, the nesting rule) — the library's `layout/` folder, or the Layout catalog pages |
| `Digest/Lists.md` | working on list rows and cards (`AMListRow`, `AMListCard`), labeled content, the reorderable lists and their drag engine (handles, hold to lift, auto-scroll, TalkBack move actions), swipeable rows and `AMSwipeAction`, or how a row container hands its TalkBack actions to the row — the library's `lists/` folder, or the Lists catalog pages |
| `Digest/Feedback.md` | working on banners and toasts and the per-window hosts that show them (`AMBannerHost`, `AMToastHost`, a screen's or a sheet's), the loading overlay, empty states (`AMEmptyState`), skeletons and shimmer (`AMSkeleton`, `Modifier.amShimmer`), labeled progress (`AMProgressView`: `Spinner`, `Bar` or `Glyph`), pull to refresh under a page header (`AMPullToRefreshBox`), or how banners, toasts and the loading overlay follow the app's surface setting (solid or frosted) — the library's `feedback/` folder, or the Feedback catalog pages |
| `Digest/Overlays.md` | working on sheets, alerts, the actions overlay, the spotlight and holds, backdrop blur, the paused backdrop, or any shared overlay rule — overlay windows, blur-behind and the mode's wash, back order, the discard guard, the overlay-up registry — the library's `overlays/` folder, or the Overlays catalog pages |
| `Digest/DataDisplay.md` | working on avatars (`AMAvatar`, the hashed hue), tags (`AMTag`), badges (`AMBadge`, `AMBadgedBox`), timelines (`AMTimeline`, `amTimelineFirstLine`), remote images and the image pipeline, art thumbnails and art cards (`AMArtThumbnail`, `AMArtCard`), or color under cards — the art backdrop and its washes toward the mode (`amArtBackdrop`, `AMArtDefaults`), the art's tone (`AMArtTone`), art rows (`amArtRows`/`amArtRow`) and the art-rows backdrop frosted surfaces sample (`AMArtRowsBackdrop`), how their pictures are blurred and cached, art text (`amArtText`) — the library's `datadisplay/` folder, or the Data display catalog pages |
| `Digest/Collections.md` | working on the carousel on Jetpack's pager (`AMCarousel`) or the swipeable deck (`AMCardStack`, `AMCardStackSwipeDirection`), the deck's release, window and identity rules, the shared index clamp (`clampIndex`), or the shared collection contract — the library's `collections/` folder, or the Card stack and Carousel catalog pages |
| `Digest/Demo.md` | adding a component page to the catalog (the exact recipe), or working on the catalog list and its search, the registry, the `ComponentPage` scaffold, stage layouts and the frosted stage (`CatalogFrostedStage`), the page's banner and toast host (`CatalogScreenHost`), inspection controls, the library theme at the root, sample photos and glyphs, or the debug launch extras (`LaunchOptions`) |
| `Digest/DemoScenes.md` | working on any of the demo's six scenes — the rules they share; read it alongside that scene's file |
| `Digest/SceneSocial.md` | working on the Social scene — feed, alerts, profiles, compose, search, its gradient backdrop |
| `Digest/SceneMusic.md` | working on the Music scene — streamed catalog, playback, covers from the MP3s' tags, queue and Queue sheet, Player, hold menu, art backdrops and art-row pages |
| `Digest/ScenePhotos.md` | working on the Photos scene — library, filters, select mode and batch actions, albums, detail carousel, the Info and Add to album sheets |
| `Digest/SceneCalendar.md` | working on the Calendar scene — events and repeats, agenda and its now marker, day peek, event form, months view, month art |
| `Digest/SceneShop.md` | working on the Shop scene — catalog, quick add, bag, checkout and its discard guard, order tracking |
| `Digest/SceneAccount.md` | working on the Account scene — onboarding, sign-in, two-factor, stand-in requests, profile, settings, help |
| `Digest/Build.md` | building or running the library or demo — Gradle and its wrapper, the version catalog, the environment variables, SDK levels, modules, the demo's manifest and window theme, installing and launching the demo with adb |
| `Digest/Testing.md` | writing or running tests, or verifying a change — the minimal test policy, JUnit, the instrumented Compose UI tests, the test dependencies and Gradle tasks, and the build-and-screenshot check |

<!-- digestmtrls:end -->
