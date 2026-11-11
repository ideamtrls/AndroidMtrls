# AndroidMtrls

AndroidMtrls is a Jetpack Compose design system for Android. It provides the visual components an
app is built from (actions, inputs, selection, navigation, layout, lists, feedback, overlays, data
display, collections) and the style values behind them. It has no app logic and no data.

- **Android 16 (API 36) and later, phones only.** There are no tablet, foldable or landscape
  designs.
- **Pre-release.** The API still changes, and parameter reorders that break callers are allowed.
- **Local, not published.** The library is the `:androidmtrls` module of this Gradle build, in one
  package, `com.ideamtrls.androidmtrls`. It isn't published to any repository.

## Two layers

- **The native layer is Android's own.** Jetpack and Material 3 components keep their own shapes,
  colors, type, motion and behavior: the Material type scale in the phone's font, a static Material
  color scheme (never dynamic color), ripple, Android gestures, system and predictive back, edge to
  edge, and saved and restored state.
- **The style layer sits on top.** White and black floating surfaces on a tuned page, two-layer
  shadows and hairlines, cards, the remote (the floating action button and its tiles) as the app's
  main navigation, the spotlight for press and hold and for pickers, no dimming behind sheets, color
  under cards, frosted surfaces over art, folding, and quiet defaults.
- **Where the line falls: stateless surfaces wear the style; stateful controls keep Material's
  look.** Cards, buttons, sheets, tiles and the floating action button get the floating surface,
  the shadow and the hairline. Switches, sliders, chips, text fields and pickers keep Material's
  shapes and colors. When in doubt, the library leans Android.

## What's in it

Sources live in area folders under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/`, all in
the one package.

| Area | Main components |
|---|---|
| `foundations` | `AMTheme` and its style values, `AMTokens` (spacing, semantic tints), `AMSemantic`, `AMElevation` and `Modifier.amElevation`, `Modifier.amHairline`, `Modifier.amTextShadow`, `AMSurfaceFill` and `AMBackdrop` (frosted glass over art), `AMSurfaceStyle`, `amInstantSwitch` and `Modifier.amCrossfade`, `Modifier.amStatusBarEdge`, `AMPageHeader`, `AMSectionHeader` |
| `actions` | `AMButton` and `AMButtonStyle`, `AMIconButton`, `AMCardButton`, `AMFloatingActionButton`, `AMFloatingActions` (the remote), `AMActionGrid`, `AMActionGridRow`, `AMActionGridButton`, `AMFormActions`, `AMCardActionRow` |
| `inputs` | `AMTextField`, `AMSecureField`, `AMSearchField`, `AMTextEditor`, `AMToggle`, `AMSlider`, `AMStepper`, `AMDatePicker` |
| `selection` | `AMSegmentedControl`, `AMMenuPicker`, `AMChipGroup`, `AMFlowLayout`, `AMCalendar`, `AMSelectionMark` and `Modifier.amSelectionCheck` |
| `navigation` | `AMScene` (a screen's root, with slots for the remote and scene-wide overlays), `AMKeepsPlace`, `AMLightStatusBarIcons`, `AMPanel`, `AMPanelChrome`, `AMArtPanelChrome`, `AMToolbar`, `AMToolbarButton`, `AMTabs`, `AMPageIndicator` |
| `layout` | `Modifier.amCard()`, the row recipe (`Modifier.amRowPadding()`, `AMRowDefaults`), `AMSection`, `AMDisclosureGroup`, `AMDivider` |
| `lists` | `AMListRow`, `AMListCard`, `AMLabeledContent`, `AMReorderableRowList`, `AMReorderableCardList`, `AMSwipeableRow` and `AMSwipeAction` |
| `feedback` | `AMBanner` and `AMBannerHost`, `AMToast` and `AMToastHost`, `AMLoadingOverlay`, `AMEmptyState`, `AMSkeleton` and `Modifier.amShimmer`, `AMProgressView` |
| `overlays` | `AMSheet`, `AMAlert`, `AMActionsOverlay`, `AMSpotlight` and `Modifier.amHoldable`, `AMBackdropBlur`, `Modifier.amPausedBackdrop`, `Modifier.amDiscardGuard` |
| `datadisplay` | `AMAvatar`, `AMTag`, `AMBadge` and `AMBadgedBox`, `AMTimeline`, `AMRemoteImage`, `Modifier.amArtBackdrop`, `Modifier.amPageBackdrop`, `Modifier.amArtRows` and `Modifier.amArtRow` inside `AMArtRowsBackdrop`, `Modifier.amArtText` |
| `collections` | `AMCarousel`, `AMCardStack` |

## Getting started

### Add the module

The library isn't published, so an app builds as a module of this Gradle build, the way `demo/`
does (`demo/build.gradle.kts`):

```kotlin
// settings.gradle.kts
include(":androidmtrls")
include(":app")

// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)  // AGP 9, with its built-in Kotlin
    alias(libs.plugins.kotlin.compose)       // the Compose compiler plugin
}

android {
    namespace = "com.example.notes"
    compileSdk = 37
    defaultConfig { minSdk = 36 }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":androidmtrls"))  // brings Compose UI, Foundation and Material 3
    implementation(libs.androidx.activity.compose)
}
```

### The theme

`AMTheme` installs `MaterialTheme` (Material's baseline static scheme by default, or the app's own
`colorScheme`) and provides the style layer to everything below. Draw edge to edge:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AMTheme {
                NotesApp()
            }
        }
    }
}
```

### A card and a button

A screen lies on the page color under its `AMPageHeader`. Flat content goes in a card,
`Modifier.amCard()`; rows inside it keep the row recipe. A filled `AMButton` floats on its own.

```kotlin
@Composable
fun NotesScreen() {
    var reminders by rememberSaveable { mutableStateOf(true) }
    val spacing = AMTheme.tokens.spacing
    Scaffold(
        topBar = { AMPageHeader("Notes") },
        containerColor = AMTheme.Surface.page,
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Column(Modifier.fillMaxWidth().amCard()) {
                AMListRow("Harbor survey", subtitle = "Edited today")
                AMDivider(inset = spacing.lg)
                AMToggle("Remind me", checked = reminders, onCheckedChange = { reminders = it })
            }
            AMButton("New note", onClick = ::newNote, modifier = Modifier.fillMaxWidth())
        }
    }
}
```

### A scene with the remote

Every screen's root is `AMScene`. Its `floatingActions` slot holds the remote: `AMFloatingActions`,
a floating action button that opens a grid of tiles over the blurred screen. With several root
views, the scene keeps each one's place and takes back to the landing view before leaving the app.

```kotlin
enum class Root { Notes, Archive }

@Composable
fun NotesApp() {
    var current by rememberSaveable { mutableStateOf(Root.Notes) }
    var showsRemote by rememberSaveable { mutableStateOf(false) }
    fun open(root: Root) {
        showsRemote = false                    // close the remote, then swap the root at once
        amInstantSwitch { current = root }
    }

    AMScene(
        current = current,
        landing = Root.Notes,
        onCurrentChange = { current = it },
        floatingActions = {
            AMFloatingActions(showsRemote, onExpandedChange = { showsRemote = it }) {
                AMActionGrid {
                    AMActionGridRow(
                        {
                            AMActionGridButton(
                                painterResource(R.drawable.ic_notes), "Notes",
                                onClick = { open(Root.Notes) }, active = current == Root.Notes,
                            )
                        },
                        {
                            AMActionGridButton(
                                painterResource(R.drawable.ic_archive), "Archive",
                                onClick = { open(Root.Archive) }, active = current == Root.Archive,
                            )
                        },
                    )
                }
            }
        },
    ) { root ->
        when (root) {
            Root.Notes -> NotesScreen()
            Root.Archive -> ArchiveScreen()
        }
    }
}
```

`ic_notes` and `ic_archive` are the app's own glyphs, Material Symbols Outlined vector drawables. The
scene fades the button while any overlay inside it is up, shows banners and toasts posted inside
it, and needs no back code: windows take back first, then a root's panels (a root with panels
passes its Navigation 3 back stack as `backStack`), then the landing view.

## Conventions in brief

- **The `AM` prefix.** Every public symbol carries an `AM` mark: `AM` on types and composables
  (`AMButton`, `AMTheme`), `Modifier.am…` on modifiers (`Modifier.amCard()`), `LocalAM…` on
  composition locals, `am_` on resources. Nested types aren't prefixed again
  (`AMButtonStyle.Primary`).
- **Compose's parameter order.** Required parameters, then `modifier`, then optional ones, then the
  content lambda last. Components with a content slot come as a string form and a slot form.
- **Hoisted state.** A control takes its value and a callback (`checked` and `onCheckedChange`);
  it never owns the value. An overlay is a composable its state drives (`AMSheet(visible,
  onDismissRequest) { … }`). Screen state uses `rememberSaveable`, so views you leave are saved
  and restored.
- **`enabled` on every control**, defaulting to true. A container can disable the controls inside
  it (`AMFormActions(enabled = false)`).
- **Sealed styles.** A style is a sealed type (`AMButtonStyle.Primary()`, `.Secondary()`,
  `.Ghost`), and its default comes from a composition local, so a container styles the buttons in
  its slots.
- **Quiet defaults.** Autofocus is off, overlays appear only when their state says so, and nothing
  moves on its own. Controls draw flat; the container supplies the surface.
- **Android's behavior.** Ripple, 48 dp touch targets, system and predictive back, TalkBack, font
  scale, and Material's motion scheme.
- **No third-party libraries.** Jetpack, AndroidX and Google only, in the library and the demo.
  Image loading and blur are built in-house.

## The catalog app

`demo/` is the catalog app: a page for every component, built with the library itself. Its
searchable list groups the pages by category: Foundations, Actions, Inputs, Selection, Navigation,
Layout, Lists, Feedback, Overlays, Data display, and Showcase. A component page shows the component
live: a simple Preview in its default setup (where it adds something), and one Loaded composition,
a fragment of a real screen. The inspection menu on the list and on every page switches the appearance, the text size
and the layout direction.

Showcase holds six scenes, small apps built on the library: Social (a feed, alerts and profiles),
Music (artists, albums and a player over the art), Photos (browse, favorite and organize photos),
Calendar (a month calendar with a day's events), Shop (a coffee shop with a bag, checkout and
orders) and Account (onboarding, sign-in and settings).

Debug builds read launch extras, for screenshots and page checks:

```bash
adb shell am start -S -n com.ideamtrls.androidmtrls.demo/.MainActivity --es entry card --es appearance dark
```

| Extra | Values | Effect |
|---|---|---|
| `entry` | an entry id (`card`, `buttonStyles`, `musicScene`) | opens that page or scene over the list |
| `scrollTo` | `preview`, `loaded` | scrolls the page to that section |
| `present` | `preview`, `loaded` | opens, once, the demo in that section that shows something inline |
| `appearance` | `light`, `dark`, `system` | pins the appearance |
| `fontScale` | a number, such as `1.3` | pins the font scale |
| `rtl` | `true`, `false` | pins the layout direction |
| `artwork` | `night` | switches the sample photos to their second set |
| `demoTime` | `H:MM`, such as `9:41` | pins the time of day the clock-driven samples show |
| `shopFillsCheckout` | `yes` | opens the Shop scene's checkout filled in |

## Building and testing

Gradle needs three variables, then `./gradlew` from the project root. `JAVA_HOME` is Android
Studio's bundled runtime, and `GRADLE_USER_HOME` keeps Gradle's caches in the gitignored
`.gradle-home/`:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
       ANDROID_HOME=~/Library/Android/sdk \
       GRADLE_USER_HOME="$PWD/.gradle-home"

./gradlew :androidmtrls:assembleDebug :demo:assembleDebug --console=plain   # build both modules
./gradlew :androidmtrls:testDebugUnitTest --console=plain                    # the library's JUnit tests
./gradlew :demo:testDebugUnitTest --console=plain                           # the demo's JUnit tests
./gradlew :demo:installDebug --console=plain                                 # build and install the demo
```

The build needs the Android 37 SDK platform (minSdk 36, compileSdk and targetSdk 37) and is
warning-free. Testing is minimal on purpose: JUnit covers pure logic that's easy to get wrong
(index clamping, the card stack's rules, the image pipeline's bookkeeping), and a change is checked
by building it and looking at its catalog page in light and dark. There are no screenshot, golden
or UI test suites.

## Documentation

- **KDoc** on every public symbol is the API reference.
- **`Digest/`** holds the detailed working docs, one file per area: `Conventions.md` (the rules
  every public API follows), `Patterns.md` (how to compose a screen), `Foundations.md`, one file per
  component area (`Actions.md` through `Collections.md`), `Demo.md` and the scene files for the
  catalog app, `Build.md` and `Testing.md`.
- **`CLAUDE.md`** holds the routing table that maps a task to the Digest files that cover it.
