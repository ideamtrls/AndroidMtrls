---
scope: Building and running the library and the demo — Gradle and its wrapper, the version catalog, the environment variables, SDK levels, the two modules, the demo's manifest and window theme, and installing and launching the demo with adb.
globs: [README.md, screenshots/**, settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradle/wrapper/**, gradlew, gradlew.bat, androidmtrls/build.gradle.kts, demo/build.gradle.kts, demo/src/main/AndroidManifest.xml, demo/src/main/res/values/strings.xml, demo/src/main/res/values/themes.xml, demo/src/main/res/values-night/themes.xml, .gitignore]
keywords: [build, gradle, gradlew, wrapper, gradle-wrapper.jar, Gradle 9.6.1, AGP, Android Gradle Plugin 9.5.1, built-in Kotlin, Kotlin 2.4.21, compose compiler plugin, kotlin.compiler.execution.strategy, in-process, Kotlin daemon, JAVA_HOME, ANDROID_HOME, GRADLE_USER_HOME, .gradle-home, local.properties, repositories, FAIL_ON_PROJECT_REPOS, version catalog, libs.versions.toml, Compose BOM, material3 1.5.0-rc01, Navigation 3, navigationevent, activity-compose, lifecycle-runtime-compose, core-ktx, JUnit, espresso-core, testDebugUnitTest, connectedDebugAndroidTest, minSdk 36, compileSdk 37, targetSdk 37, JVM 17, explicitApi, modules, androidmtrls, demo, applicationId, namespace, BuildConfig, assembleDebug, installDebug, build outputs, adb, am start, launch extras, screencap, portrait, supportsRtl, app_name, edge-to-edge, INTERNET, Theme.Demo, .gitignore]
updated: 2026-11-17
---

# Build

How to build and run the library and its demo catalog app.

## Where things live

- `settings.gradle.kts`: repositories, the root project `AndroidMtrls` and its two modules, `:androidmtrls` and `:demo`.
- `build.gradle.kts` (root): declares the three plugins with `apply false`.
- `gradle/libs.versions.toml`: every version, library and plugin.
- `gradle.properties`: Gradle's JVM heap, AndroidX on, and Kotlin compiling in-process.
- `androidmtrls/build.gradle.kts` and `demo/build.gradle.kts`: the modules.
- `gradlew`, `gradlew.bat` and `gradle/wrapper/` (`gradle-wrapper.jar`, `gradle-wrapper.properties`): the Gradle 9.6.1 wrapper. Use it as it is; don't replace it.
- `demo/src/main/AndroidManifest.xml`: the demo's permission and its one activity; `demo/src/main/res/values/strings.xml`: its label, `app_name`; `demo/src/main/res/values/themes.xml` and `values-night/themes.xml`: its window theme, `Theme.Demo`.
- `README.md`: the human-facing front page, plain and built around screenshots: a one-line pitch; the six demo apps; what sets the library apart (the + and the remote of tiles it opens, overlays that blur and surfaces that frost, 80 dp rows); then requirements, install, a quick start, a theme override, running the demo, and the build and test commands. It holds no API reference. Its screenshots live in `screenshots/`, a light and a dark copy of each (`<name>.jpg` and `<name>-dark.jpg`, scaled to 1200 px tall with `sips -Z 1200 -s formatOptions 80`), captured from the demo's scenes; each shows as a `<picture>` that follows the reader's light or dark appearance, three to a row for the apps and two for each idea.
- `.gitignore`: `build/`, `.gradle/`, `.gradle-home/`, `.kotlin/` (the Kotlin plugin's session state), `local.properties`, `.idea/`, `.DS_Store`, `.claude/settings.local.json`.

## Environment

Every Gradle run needs three variables, then `./gradlew` from the project root:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
       ANDROID_HOME=~/Library/Android/sdk \
       GRADLE_USER_HOME="$PWD/.gradle-home"
```

- `JAVA_HOME` is Android Studio's bundled runtime.
- The SDK comes from `ANDROID_HOME`; the project has no `local.properties`.
- `GRADLE_USER_HOME` keeps Gradle's caches, the wrapper's distribution and the daemon inside the project, in the gitignored `.gradle-home/`.
- The build needs the Android 37 SDK platform. If a platform or tool is missing, the build fails: report it, rather than changing the SDK levels or turning on automatic SDK downloads.

## Commands

```bash
./gradlew :androidmtrls:assembleDebug :demo:assembleDebug --console=plain    # build both modules
./gradlew :androidmtrls:testDebugUnitTest --console=plain                    # the library's JUnit tests
./gradlew :androidmtrls:connectedDebugAndroidTest --console=plain            # the library's instrumented tests (an emulator attached)
./gradlew :demo:testDebugUnitTest --console=plain                            # the demo's JUnit tests (Music's ID3 reader)
./gradlew :demo:installDebug --console=plain                                 # build and install the demo
./gradlew --stop                                                             # stop the Gradle daemon
```

Outputs: `androidmtrls/build/outputs/aar/androidmtrls-debug.aar` and `demo/build/outputs/apk/debug/demo-debug.apk`. The build is warning-free; keep it that way.

## Toolchain and versions

| Piece | Version |
|---|---|
| Gradle (wrapper) | 9.6.1 |
| Android Gradle Plugin | 9.5.1 |
| Kotlin and the Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`) | 2.4.21 |
| Compose BOM | 2026.09.00 |
| Material 3 | 1.5.0-rc01, pinned over the BOM |
| Navigation 3 (`navigation3-runtime`, `navigation3-ui`) | 1.2.1 |
| `navigationevent-compose` | 1.2.0 |
| `activity-compose` | 1.13.0 |
| `lifecycle-runtime-compose` | 2.11.0 |
| `core-ktx` | 1.19.1 |
| JUnit | 4.13.2 |
| Compose UI test (`ui-test-junit4`, `ui-test-manifest`) | from the Compose BOM |
| `androidx.test` runner | 1.7.0 |
| `espresso-core`, pinned over the 3.5 Compose UI test brings | 3.7.0 |

The last four are test dependencies only (`Testing.md`).

- **SDK levels**: minSdk 36 (Android 16) and compileSdk 37 in both modules; targetSdk 37 in the demo (the library sets none). These library versions need compileSdk 37.
- **Java and JVM target 17** in both modules (`compileOptions` and `kotlin.compilerOptions.jvmTarget`).
- **Kotlin is AGP 9's built-in Kotlin**: the modules apply no `org.jetbrains.kotlin.android` plugin. The Compose compiler plugin, declared at the Kotlin version, puts that version of the Kotlin Gradle plugin on the build classpath.
- **No Kotlin daemon**: `kotlin.compiler.execution.strategy=in-process` compiles Kotlin inside the Gradle daemon, which is why `org.gradle.jvmargs` gives Gradle a 4 GB heap.
- **Jetpack only.** Every library in the catalog is AndroidX except JUnit, for tests. Navigation 3 brings `kotlinx-serialization-core` in transitively; nothing applies the serialization plugin, and the demo's back stack saves through a plain `Saver`.
- **Repositories**: `google()` (limited to `com.android`, `com.google` and `androidx` groups for plugins), `mavenCentral()`, and the Gradle Plugin Portal for plugins. Projects can't declare their own (`FAIL_ON_PROJECT_REPOS`).

## Modules

**`:androidmtrls`**, the library:
- Android library, namespace `com.ideamtrls.androidmtrls`, Compose on, `explicitApi()`.
- One public package, `com.ideamtrls.androidmtrls`, with sources in area folders under `src/main/kotlin/com/ideamtrls/androidmtrls/` (`Conventions.md`).
- Dependencies: `api` the Compose BOM, Compose UI, Foundation and Material 3; `implementation` core-ktx, navigationevent-compose (the back handling behind `AMKeepsPlace` and `AMSearchField`) and lifecycle-runtime-compose (the panel back button's lifecycle gate, `dropUnlessStarted`); `testImplementation` JUnit and `androidTestImplementation` the instrumented-test stack, run by `AndroidJUnitRunner` (`Testing.md`). Navigation 3 is a test dependency only: the library exposes no Navigation 3 types.

**`:demo`**, the catalog app:
- Android app, applicationId and namespace `com.ideamtrls.androidmtrls.demo`, versionCode 1, versionName `0.1`, Compose on, `BuildConfig` on (the debug launch extras and the registry's debug checks read `BuildConfig.DEBUG`).
- Dependencies: `:androidmtrls` (which brings Compose and Material 3), activity-compose, lifecycle-runtime-compose, Navigation 3 runtime and UI, navigationevent-compose, core-ktx; `testImplementation` JUnit.
- Manifest: the `INTERNET` permission (sample photos and the Music scene's catalog stream from the web), RTL supported, the label `@string/app_name` ("AndroidMtrls"), one exported launcher activity, `.MainActivity` (in the `com.ideamtrls.androidmtrls.demo` package, its file under `app/`), portrait only.
- Theme: `Theme.Demo` (`res/values/themes.xml`, `res/values-night/themes.xml`) is the platform's Material theme without an action bar, its window background the page color (`#F2F2F7` light, `#141416` dark, the values `foundations/Style.kt` holds), so the first frame matches the page. Compose draws edge to edge over it (`Demo.md`).

## Installing and launching the demo

```bash
./gradlew :demo:installDebug --console=plain
adb shell am start -S -n com.ideamtrls.androidmtrls.demo/.MainActivity
adb shell am start -S -n com.ideamtrls.androidmtrls.demo/.MainActivity --es entry card --es appearance dark
adb exec-out screencap -p > <path>.png
```

`-S` stops the app first, so each launch starts fresh and reads its extras. Debug builds read string extras (`--es <name> <value>`) that open a page, pin an inspection setting or pin a sample; release builds ignore them. The extras and what each does are in `Demo.md` (`LaunchOptions`).

## See also

- `Demo.md`: the debug launch extras (`LaunchOptions`).
- `Testing.md`: the test dependencies, and verifying a change with screenshots.
