---
scope: Building and running the AndroidMtrls library and demo — the Gradle setup and version catalog, the environment variables, SDK levels and modules, and installing and launching the demo with adb and its debug extras.
globs: [settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradle/wrapper/gradle-wrapper.properties, androidmtrls/build.gradle.kts, demo/build.gradle.kts, demo/src/main/AndroidManifest.xml, demo/src/main/res/values/themes.xml, demo/src/main/res/values-night/themes.xml, .gitignore]
keywords: [build, gradle, gradlew, wrapper, Gradle 9.6.1, AGP, Android Gradle Plugin 9.5.1, built-in Kotlin, Kotlin 2.4.21, compose compiler plugin, kotlin.compiler.execution.strategy, in-process, Kotlin daemon, JAVA_HOME, ANDROID_HOME, GRADLE_USER_HOME, .gradle-home, local.properties, version catalog, libs.versions.toml, Compose BOM, material3 1.5.0-rc01, Navigation 3, navigationevent, activity-compose, lifecycle-runtime-compose, core-ktx, JUnit, minSdk 36, compileSdk 37, targetSdk 37, JVM 17, explicitApi, modules, androidmtrls, demo, applicationId, namespace, BuildConfig, assembleDebug, installDebug, adb, am start, launch extras, screencap, portrait, edge-to-edge, INTERNET, Theme.Demo, .gitignore]
updated: 2026-11-10
---

# Build

How to build and run the library and its demo catalog app.

## Where things live

- `settings.gradle.kts`: repositories and the two modules, `:androidmtrls` and `:demo`.
- `build.gradle.kts` (root): declares the plugins with `apply false`.
- `gradle/libs.versions.toml`: every version, library and plugin.
- `gradle.properties`: Gradle's JVM heap, AndroidX on, and Kotlin compiling in-process.
- `androidmtrls/build.gradle.kts` and `demo/build.gradle.kts`: the modules.
- `gradle/wrapper/`: the Gradle 9.6.1 wrapper. Use it as it is; don't replace it.
- `.gitignore`: `build/`, `.gradle/`, `.gradle-home/`, `.kotlin/` (the Kotlin plugin's session state), `local.properties`, `.idea/`, `.tmp/`, `.notes/`.

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
- Scratch files (screenshots, logs) go in the gitignored `.tmp/`.

## Commands

```bash
./gradlew :androidmtrls:assembleDebug :demo:assembleDebug --console=plain   # build both modules
./gradlew :androidmtrls:testDebugUnitTest --console=plain                    # the library's JUnit tests
./gradlew :demo:testDebugUnitTest --console=plain                           # the demo's JUnit tests (Music's ID3 reader)
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
| JUnit (tests only) | 4.13.2 |

- **SDK levels**: minSdk 36 (Android 16), compileSdk 37, targetSdk 37. These library versions need compileSdk 37.
- **Java and JVM target 17** in both modules (`compileOptions` and `kotlin.compilerOptions.jvmTarget`).
- **Kotlin is AGP 9's built-in Kotlin**: the modules apply no `org.jetbrains.kotlin.android` plugin. The Compose compiler plugin, declared at the Kotlin version, puts that version of the Kotlin Gradle plugin on the build classpath.
- **No Kotlin daemon**: `kotlin.compiler.execution.strategy=in-process` compiles Kotlin inside the Gradle daemon, which is why `org.gradle.jvmargs` gives Gradle a 4 GB heap.
- **Jetpack and Google only.** Navigation 3 brings `kotlinx-serialization-core` in transitively; nothing applies the serialization plugin, and the demo's back stack saves through a plain `Saver`.
- **Repositories**: `google()` (limited to `com.android`, `com.google` and `androidx` groups for plugins), `mavenCentral()`, and the Gradle Plugin Portal for plugins. Projects can't declare their own (`FAIL_ON_PROJECT_REPOS`).

## Modules

**`:androidmtrls`**, the library:
- Android library, namespace `com.ideamtrls.androidmtrls`, Compose on, `explicitApi()`.
- One public package, `com.ideamtrls.androidmtrls`, with sources in area folders under `src/main/kotlin/com/ideamtrls/androidmtrls/` (`Conventions.md`).
- Dependencies: `api` the Compose BOM, Compose UI, Foundation and Material 3; `implementation` core-ktx and navigationevent-compose (the back handling behind `AMKeepsPlace`); `testImplementation` JUnit. The library exposes no Navigation 3 types.

**`:demo`**, the catalog app:
- Android app, applicationId and namespace `com.ideamtrls.androidmtrls.demo`, Compose on, `BuildConfig` on (the debug launch extras and the registry's debug checks read `BuildConfig.DEBUG`).
- Dependencies: `:androidmtrls` (which brings Compose and Material 3), activity-compose, lifecycle-runtime-compose, Navigation 3 runtime and UI, navigationevent-compose, core-ktx; `testImplementation` JUnit.
- Manifest: the `INTERNET` permission (sample photos stream from the web), one launcher activity, `.MainActivity`, portrait only.
- Theme: `Theme.Demo` (`res/values/themes.xml`, `res/values-night/themes.xml`) is the platform's Material theme without an action bar, its window background the page color, so the first frame matches the page. Compose draws edge to edge over it (`Demo.md`).

## Installing and launching the demo

```bash
./gradlew :demo:installDebug --console=plain
adb shell am start -S -n com.ideamtrls.androidmtrls.demo/.MainActivity
adb shell am start -S -n com.ideamtrls.androidmtrls.demo/.MainActivity --es entry card --es appearance dark
adb exec-out screencap -p > .tmp/shot.png
```

`-S` stops the app first, so each launch starts fresh and reads its extras. Debug builds read these string extras (`--es <name> <value>`); release builds ignore them:

| Extra | Values | Effect |
|---|---|---|
| `entry` | an entry id | opens that entry's page over the catalog list (a scene id opens the scene) |
| `scrollTo` | `preview`, `loaded` | scrolls the opened page to that section |
| `present` | `preview`, `loaded` | the `CatalogAutoPresent` demo in that section opens, once |
| `appearance` | `light`, `dark`, `system` | pins the appearance for this launch |
| `fontScale` | a float, such as `1.3` | pins the font scale for this launch |
| `rtl` | `true`, `false` | pins the layout direction for this launch |
| `artwork` | `night` | catalog photos switch to their second set |

A pinned setting doesn't write the stored inspection settings, and the inspection menu can't change it until a launch without the extra. `Demo.md` covers how each one works.
