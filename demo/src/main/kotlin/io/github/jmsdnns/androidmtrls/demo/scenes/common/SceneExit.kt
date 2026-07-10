package io.github.jmsdnns.androidmtrls.demo.scenes.common

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The "exit this scene" action, provided by the navigation shell around a
 * scene's content.
 *
 * `DemoNavHost` installs this in its `Screens` branch, wrapping the scene in
 * `CompositionLocalProvider(LocalSceneExit provides { navController.popBackStack() })`
 * so a scene stays parameterless: its FAB **Exit** tile just calls
 * `LocalSceneExit.current()` to return to the catalog. System predictive-back
 * also pops the scene — an accepted Android divergence (iOS covers have no edge
 * exit).
 *
 * The default is a no-op so previews and tests that render a scene outside the
 * nav shell stay safe. Mirrors iOS `@Environment(\.dismiss)` on the scene root.
 */
val LocalSceneExit = staticCompositionLocalOf<() -> Unit> { {} }
