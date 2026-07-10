# AndroidMtrls

A Jetpack Compose design system for Android. It provides visual components
(surfaces, controls, content, and collections) and the design tokens behind
them. It contains no app logic or data.

It mirrors **iOSMtrls** (`../iOSMtrls`) component-for-component and carries the
same signature look: floating surfaces such as cards and pills render white in
light mode and black in dark mode, sitting on a tuned page background, with lift
from a two-layer drop shadow rather than a lighter fill, plus hairline borders
and a soft selection ring. That aesthetic is translated into idiomatic Android:
a monochrome ripple for press feedback, the Material type scale, M3 component
anatomy (switches, sliders, sheets, chips), predictive-back gestures, and
reduce-motion support. The library uses Compose foundation/animation only — **no
Material3** — so the design system owns every pixel.

## Requirements

- Android 12+ (minSdk 31 — for RenderEffect backdrop blur)
- compileSdk 36, Kotlin 2.4, Compose BOM 2026.05.01, AGP 9.1.1

## Adding it to a project

AndroidMtrls is a Gradle module. Include `:mtrls` in your `settings.gradle.kts`
and add it to your app module's dependencies:

```kotlin
dependencies {
    implementation(project(":mtrls"))
}
```

## Using it

Wrap your content in `AMThemeRoot` once at the top of the tree — it installs the
tokens, dark-mode, and reduce-motion CompositionLocals every component reads:

```kotlin
import io.github.jmsdnns.androidmtrls.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AMThemeRoot { ProfileCard() } }
    }
}
```

Components are prefixed `AM`; Modifier extensions and CompositionLocals use
`am`/`LocalAM`. Design tokens are defined on `AMTheme`. Surface treatments are
Modifier extensions (`Modifier.amElevation`, `Modifier.amHairline`); everything
that must configure a subtree (like `AMCard`) is a composable.

```kotlin
@Composable
fun ProfileCard() {
    AMCard(padding = AMPadding.block) {
        Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp)) {
            BasicText("Jane Appleseed",
                style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve()))
            BasicText("Designer",
                style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve()))
            AMPrimaryButton("Follow", onClick = {})
        }
    }
}
```

## Theming

Tokens come in two tiers. The signature look — the inverted white/black floating
surfaces, the two-layer shadow recipe, hairlines, the type scale, the selection
ring — is locked on the `AMTheme` object and not themable; it is the point of the
library. Everything else (semantic tints, the spacing scale, corner radii, input
metrics, motion) lives on `AMTokens` and flows through `LocalAMTokens`, so apps
can re-tune it globally or per subtree with `AMTokensProvider`:

```kotlin
// Tweak one token for a subtree:
AMTokensProvider({ it.copy(semantic = it.semantic.copy(info = Color(0xFF00897B))) }) {
    FeedView()
}

// Or install a custom set at the app root:
val brand = AMTokens.standard.copy(radius = AMTokensRadius(sm = 4.dp, md = 6.dp, lg = 10.dp))
AMTokensProvider(brand) { RootView() }
```

Components that display text or icons also accept custom content: every `String`
overload (`AMTag("Active", …)`, `AMSectionHeader("Recents")`, tab labels, chip
labels, list-row titles) has a composable-slot counterpart that keeps the
component's visual treatment while you supply the content.

## Components

**Tokens.** `AMTheme` defines the locked signature tokens; `AMTokens` carries
the overridable layer through `LocalAMTokens`. `AMSemantic` is the one
role→color+icon axis; `AMElevation` / `Modifier.amElevation` and
`Modifier.amHairline` apply the shadow and stroke recipes; `amRipple` is the
monochrome press state layer. The reduce-motion layer — `LocalAMReduceMotion`,
the `tokens.motion.resolved` / `resolvedOrNull` resolvers, and `AMCrossfade` —
softens or stills every animation by one policy.

**Surfaces, Frames.** Static surfaces that frame content: `AMCard`, `AMPanel` /
`AMPanelHost`, `AMSection`, `AMToolbar` / `AMToolbarButton`, and `AMDisclosure`.

**Surfaces, Overlays.** Transient layers driven by state: `AMSheet`, `AMBanner`,
`AMToast`, `AMAlert`, `AMLoadingOverlay`, `AMActionsOverlay`, the scene FAB hub
`AMFloatingActionsHost`, the decorative `AMPausedSubstrate` /
`Modifier.amPausedBackdrop`, and the backdrop blur behind them.

**Controls, Buttons.** `AMPrimaryButton`, `AMSecondaryButton`, `AMGhostButton`,
plus `AMCardButton` / `AMCardActionRow`, `AMActionGridButton` / `AMActionGrid` /
`AMActionGridRow`, `AMFormActions`, `AMFloatingActionButton`, and `AMIconButton`.

**Controls, Inputs.** `AMTextField`, `AMSecureField`, `AMSearchField`,
`AMTextEditor`, and the standing `AMFieldLabel`.

**Controls, Pickers.** `AMSegmentedControl`, `AMMenuPicker`, `AMColorPicker`,
`AMDatePickerRow`, `AMSlider`, `AMStepper`, and `AMToggleRow`.

**Content.** `AMSectionHeader`, `AMPageHeader`, `AMTag`, `AMAvatar`, `AMBadge` /
`AMBadgedBox`, `AMCalendar`, `AMSelectionMark`, `AMRemoteImage`, `AMEmptyState`,
`AMDivider`, `AMSpinner` / `AMProgressBar`, `AMRowContent`, `AMTimeline`, and
`AMSkeleton` / `Modifier.amShimmer`.

**Lists.** `AMListRow`, `AMListCard`, the drag-reorderable `AMReorderableRowList`
/ `AMReorderableCardList`, and the swipe-to-reveal `AMSwipeRow`.

**Collections.** `AMCardStack`, `AMCarousel`, `AMChipGroup`, `AMPageIndicator`,
and `AMTabs`.

## What's where

```
mtrls/src/main/kotlin/io/github/jmsdnns/androidmtrls/
  tokens/        theme, tokens, semantic, type, elevation, hairlines, ripple, motion
  surfaces/      frames/ (cards, panels, toolbars) and overlays/ (sheets, toasts, banners)
  controls/      buttons/, inputs/, pickers/
  content/       avatars, calendars, tags, badges, empty states, skeletons
  lists/         list rows/cards, reorderable lists, swipe actions
  collections/   carousels, card stacks, chip groups, tabs
```

## Documentation

Every component file carries `@Preview` blocks (light and dark) so the Android
Studio preview pane doubles as a component reference, and every public symbol has
a KDoc comment for inline Quick Documentation.

## Demo app

`:demo` is a catalog app. Five **Scenes** lead — realistic screens (Social feed,
Now Playing, Gallery, Calendar, Auth flow) composed entirely from the library,
showing how the parts fit together. Below them, every component gets a page with
state variants, plus a light/dark toggle and a Material You dynamic-accent toggle
in the top bar.

Open the project in Android Studio and run the `demo` configuration on an API 31+
device or emulator, or install it from the command line:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :demo:installDebug
```
</content>
</invoke>
