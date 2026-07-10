---
scope: Gradle module layout, SDK levels, dependency decisions, and build/test/run commands for the library and demo app
globs:
  - "*.gradle.kts"
  - gradle.properties
  - gradle/**
  - mtrls/build.gradle.kts
  - demo/build.gradle.kts
keywords: [gradle, modules, mtrls, demo, version catalog, libs.versions.toml, minSdk, compileSdk, Compose BOM, AGP, Coil, Navigation Compose, graphics-shapes, material-icons-core, material-ripple, ripple, reorderable, assembleDebug, installDebug, JAVA_HOME, unit tests, emulator]
updated: 2026-07-10
---

# Build

Gradle layout, dependency policy, and commands.
`./gradlew :demo:assembleDebug` is the authoritative full-build check.

## Modules & coordinates

- Two modules: **`:mtrls`** (`com.android.library`, the design system) and
  **`:demo`** (`com.android.application`, the catalog app, depends on
  `project(":mtrls")`).
- The demo bundles 9 MP3s in `demo/src/main/assets/Audio/` — the Now Playing
  scene's real audio (see `demo.md`); they add ~150 MB to the debug APK.
- Package: `io.github.jmsdnns.androidmtrls` (library),
  `io.github.jmsdnns.androidmtrls.demo` (demo). Package names are frozen —
  never change them.
- `minSdk 31` (gives RenderEffect blur; in keeping with the pair's newest-OS stance),
  `compileSdk 36`, `targetSdk 36`.
- Versions live in `gradle/libs.versions.toml`.

## Pinned versions

| Artifact | Version | Notes |
|---|---|---|
| AGP | 9.2.1 | |
| Kotlin / kotlin.plugin.compose | 2.4.0 | `kotlin.android` plugin NOT used — AGP 9+ provides built-in Kotlin support and refuses the plugin |
| Gradle wrapper | 9.5.1 | |
| Compose BOM | 2026.05.01 | |
| activity-compose | 1.13.0 | `:mtrls` implementation dep — provides `androidx.activity.compose.BackHandler`, used by the bottom-sheet overlays (`AMSheet`, `AMActionsOverlay`) for system-back dismissal |
| navigation-compose | 2.9.8 | demo only |
| coil (coil-compose, coil-network-okhttp) | 3.5.0 | in BOTH `:mtrls` and `:demo` — the demo installs a `SingletonImageLoader` whose OkHttp interceptor sends a descriptive User-Agent (Wikimedia's avatar host 403s Coil/OkHttp's default UA) |
| graphics-shapes | 1.1.0 | corner smoothing |
| material-icons-core / -extended | 1.7.8 | NOT in BOM — explicit version; core in :mtrls (internal glyphs only), extended in :demo |
| material-ripple | 1.11.2 (BOM-managed) | :mtrls implementation dep — the low-level `createRippleModifierNode` primitive behind `amRipple` (tokens/Ripple.kt). NOT Material3; the platform ripple node every design system wraps |
| sh.calvin.reorderable:reorderable | 2.5.1 | Maven Central; :mtrls implementation dep — reorder engine for AMReorderableRowList/CardList; 2.x API (rememberReorderableLazyListState(lazyListState){}, ReorderableItem, draggableHandle/longPressDraggableHandle); wraps onMove in rememberUpdatedState internally |
| compileSdk / targetSdk | 36 | SDK platform 36 is installed locally (see Environment) |

## AGP 9 DSL notes

- `kotlinOptions { jvmTarget }` is removed in AGP 9. Use `compileOptions` (Java source/target) only; Kotlin JVM target follows automatically.
- `org.jetbrains.kotlin.android` plugin causes a fatal error in AGP 9+ ("no longer required"). Apply only `kotlin.plugin.compose` (KCP) for Compose support.
- `kotlinOptions` block does not exist in AGP 9 — omit entirely.

## Dependency policy

- `:mtrls`: Compose foundation/ui/animation only — **no Material3** (own
  design system), `androidx.activity:activity-compose` for
  `BackHandler` (system-back dismissal in `AMSheet` / `AMActionsOverlay`),
  `material-icons-core` for internal glyphs only,
  `androidx.compose.material:material-ripple` (BOM-managed; the low-level ripple
  node behind `amRipple`, NOT Material3 — every design system wraps it),
  Coil 3 (`AMRemoteImage`), `androidx.graphics:graphics-shapes` (corner
  smoothing), `sh.calvin.reorderable` 2.5.1 (reorder
  engine for the Lists module). `haze` is NOT used — overlay backdrop blur is
  plain RenderEffect (`Modifier.amBackdropBlur` applied by overlay hosts to
  their wrapped content layer; renders correctly on the Pixel_10 emulator).
- `:demo`: Navigation Compose, `material-icons-extended`, and Coil 3
  (`coil-compose` + `coil-network-okhttp`) — `:mtrls` carries Coil as
  `implementation`, so the demo needs its own compile-time Coil deps for
  `DemoApplication`'s `SingletonImageLoader.Factory` (the Wikimedia User-Agent
  OkHttp interceptor; see `demo.md`).
- `BasicText` (not Material3 `Text`) is the text primitive throughout — used
  anywhere a Material3 dependency is unacceptable, i.e. all of `:mtrls`.

## Commands

- Library compile: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :mtrls:compileDebugKotlin`
- Library unit tests: `JAVA_HOME=... ./gradlew :mtrls:testDebugUnitTest`
- Demo build: `JAVA_HOME=... ./gradlew :demo:assembleDebug`
- Demo on emulator: `JAVA_HOME=... ./gradlew :demo:installDebug` then launch the `androidmtrls.demo` app.
- Always prefix with `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` — no system Java installed.

## Environment

- JAVA_HOME: `/Applications/Android Studio.app/Contents/jbr/Contents/Home` (OpenJDK 21 bundled JBR)
- Android SDK: `/Users/jmsdnns/Library/Android/sdk` (recorded in `local.properties`)
- Installed SDK platforms: `android-36`, `android-36.1`
- Build tools: `36.0.0`, `36.1.0`, `37.0.0`

## Test policy

JVM unit tests only where logic is pure (avatar djb2 hash, chip flow-layout
math, card-stack swipe math, reorder displacement, token copy semantics) in
`mtrls/src/test/kotlin/`. Visual behavior is verified through `@Preview`s and
the demo app, like the iOS side. No instrumentation tests, no CI.

## See also

- `conventions.md` — authoring rules, preview/KDoc policy.
