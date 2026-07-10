# AndroidMtrls

A Jetpack Compose design system for Android — the Android sibling of
**iOSMtrls** (`../iOSMtrls`). The two libraries share a similar signature look
(inverted white/black floating surfaces, two-layer shadows, hairlines) and
roughly parallel component sets, but this library is Android-native under the
hood: behavior, gestures, and implementation follow Android norms — iOS is a
look/API-shape reference, never a behavioral spec (see `memory/conventions.md`,
"Same look, platform-native feel"). No app logic, no data. **Every public
symbol carries the `AM`/`am` prefix.**

<!-- memorymtrls:start -->

`.notes/` should be used for **personal workflow scratch** and gitignored. Place artifacts produced while working in here, such as plans, specs, exploratory writeups, eg thinking out loud.

- Plans → `.notes/plans/YYYY-MM-DD-<slug>.md` — execution outlines for multi-step work. Stay as historical record after the work lands.
- Specs → `.notes/specs/YYYY-MM-DD-<slug>.md` — designs before implementation.
- Other → `.notes/scratch/`.

## Memory

This project's working context lives in `memory/`, with one file per category of context. The table below maps task level concerns to the context file(s) that cover it.

**IMPORTANT: Before starting a task, scan this table. If any rows match the task, read the relevant memory files, and then start the task.** Read only the rows that match. Context window efficiency is gained by not reading irrelevant context; if nothing matches, proceed without reading any.

**IMPORTANT: Before finishing a task that changed how an area works, update that area's memory file.** Keep edits surgical and bump `updated`. Memory files describe the current state of the system — what *is*, never what *was*. Do not narrate the change ("previously", "no longer", "moved from…"); rewrite the affected text as if the current state had always been true. If the work created a new area with no matching file, tell the user rather than inventing a category.

| Memory file | Read this when… |
| ----------- | --------------- |
| `memory/conventions.md` | adding, changing, or authoring ANY public API — the AM/am prefix rule, parameter order, `contentDescription` placement, composable-slot + String-overload pattern, composable-vs-Modifier rule, press feedback (monochrome `amRipple`, no Material ripple), `@Immutable`/`@Stable`, preview/KDoc policy, reduce-motion spec routing, and the "Same look, platform-native feel" rule (iOS at `../iOSMtrls` is a look/API-shape reference, never a behavioral spec) |
| `memory/build.md` | building, testing, or running the library or demo — Gradle modules (`:mtrls`/`:demo`), version catalog, SDK levels, dependency decisions (Coil, reorderable, graphics-shapes, material-ripple), `JAVA_HOME`, `./gradlew` commands |
| `memory/foundations.md` | touching `tokens/` — `AMTheme`/`AMTokens` values, type scale, `AMTokensProvider`/`AMThemeRoot`, elevation and two-layer shadows (`amElevation`), hairlines, squircle shapes (`amSquircle`), ripple (`amRipple`), reduce-motion resolvers (`resolved`/`resolvedOrNull`/`resolvedFinite`/`resolvedFiniteOrNull`), `LocalAM*` CompositionLocals, surface-style nesting, dark-mode/inverted layering |
| `memory/surfaces.md` | touching `surfaces/` — `AMCard` demotion, `AMToolbar` (declares `AMButtonRole`), `AMPanel`/`AMPanelHost`, `AMSection`, `AMDisclosure`, any overlay host (`AMToast`/`AMSheet`/`AMBanner`/`AMAlert`/`AMLoadingOverlay`/`AMActionsOverlay`), `AMFloatingActionsHost`/`LocalAMOverlayActive`, backdrop blur, drag-dismiss |
| `memory/controls.md` | touching `controls/` — buttons (primary/secondary/ghost, card, grid, FAB, icon), text inputs (`AMTextField`/`AMSecureField`/`AMSearchField`/`AMTextEditor`, caret/autofocus/realign), pickers (segmented, menu, color, date, slider, stepper, toggle) |
| `memory/content.md` | touching `content/` — `AMAvatar` (initials/djb2 hue), `AMCalendar` (month grid), `AMTag` tints, `AMBadge`/`AMBadgedBox`, selection marks, section/page headers, `AMEmptyState`, `AMRemoteImage`, `AMDivider`, spinner/progress, `AMRowContent`, `AMTimeline`, `AMSkeleton` shimmer |
| `memory/collections.md` | touching `collections/` — `AMCardStack` swipe deck, `AMCarousel` paging, `AMChipGroup` flow layout, `AMPageIndicator`, `AMTabs`, or the pure-math files (`CardStackMath`/`FlowMath`) and their JVM tests |
| `memory/lists.md` | touching `lists/` — `AMListRow`/`AMListCard` anatomy, the reorderable row/card lists, drag handles, `onReorder` wiring, `amMoveItem`, swipe-to-reveal (`AMSwipeRow`) |
| `memory/demo.md` | working in the demo catalog/shell (`demo/src/` outside `scenes/`) — catalog pages, the `ComponentEntry` registry, `DemoNavHost` navigation, home tile grid + `SceneCarouselBand`, catalog helper composables, Coil setup, dark-mode toggle, `ownsScaffold` overlay attachment |
| `memory/scenes.md` | touching `demo/src/**/scenes/` or `demo/src/main/assets/` — the five app-like scenes (Auth/Social/Gallery/NowPlaying/Calendar), the shared scene toolkit (`ScenePanelHost`/`ScenePanelChrome`/`LocalSceneExit`), bundled audio, ID3 reading, `MediaPlayer` playback |

<!-- memorymtrls:end -->
