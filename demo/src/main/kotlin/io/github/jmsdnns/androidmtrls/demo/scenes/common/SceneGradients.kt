package io.github.jmsdnns.androidmtrls.demo.scenes.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

/**
 * A two-stop vertical scene backdrop that carries its own light/dark palette.
 *
 * Scenes (Now Playing's album wash, Social's backdrop, …) paint a soft
 * corner-to-corner gradient behind their content; each keeps a distinct pair of
 * stops per scheme so the wash stays legible in both. Resolve to a paintable
 * [Brush] with [brush].
 *
 * @param lightTop    Top stop in light mode.
 * @param lightBottom Bottom stop in light mode.
 * @param darkTop     Top stop in dark mode.
 * @param darkBottom  Bottom stop in dark mode.
 */
@Immutable
data class SceneGradient(
    val lightTop: Color,
    val lightBottom: Color,
    val darkTop: Color,
    val darkBottom: Color,
)

/**
 * Resolves this [SceneGradient] into a **diagonal** linear [Brush] (top-leading →
 * bottom-trailing) for the current light/dark scheme, read from
 * [LocalAMIsDarkTheme]. Mirrors the iOS scene washes, which every use
 * `startPoint: .topLeading, endPoint: .bottomTrailing`.
 */
@Composable
fun SceneGradient.brush(): Brush {
    val isDark = LocalAMIsDarkTheme.current
    val top = if (isDark) darkTop else lightTop
    val bottom = if (isDark) darkBottom else lightBottom
    // Corner-to-corner: Offset.Zero → Offset.Infinite spans the drawn size's
    // top-left to bottom-right, the Compose twin of SwiftUI's topLeading →
    // bottomTrailing LinearGradient.
    return Brush.linearGradient(
        colors = listOf(top, bottom),
        start = Offset.Zero,
        end = Offset.Infinite,
    )
}

/**
 * The floating FAB's resting safe-area lift: the navigation-bar inset. The scene
 * [AMFloatingActionsHost][io.github.jmsdnns.androidmtrls.surfaces.overlays.AMFloatingActionsHost]
 * draws edge-to-edge and the FAB rests flush against its bottom edge, so passing
 * this as the hub's `bottomInset` rests the button on the gesture-nav safe-area
 * bottom — the Android twin of iOS anchoring the FAB layer to the safe-area
 * bottom.
 */
@Composable
fun sceneFabLift(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/**
 * Bottom padding every scene's scrolling content reserves so the floating FAB
 * never covers the last row: the safe-area lift ([sceneFabLift]) that raises the
 * FAB, plus the FAB diameter (56dp), plus one `spacing.xxl` gap. iOS
 * `galleryFabClearance` parity — iOS reserves the fixed clearance *above* the
 * safe-area bottom the FAB rests on, so the Android reservation folds the
 * navigation-bar inset in.
 */
@Composable
fun sceneFabClearance(): Dp =
    sceneFabLift() + 56.dp + AMTheme.Spacing.xxl.dp
