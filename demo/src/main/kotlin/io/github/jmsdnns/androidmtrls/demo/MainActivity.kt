package io.github.jmsdnns.androidmtrls.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButton
import io.github.jmsdnns.androidmtrls.demo.catalog.CatalogRegistry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.navigation.DemoNavHost
import io.github.jmsdnns.androidmtrls.demo.navigation.ROUTE_HOME
import io.github.jmsdnns.androidmtrls.demo.navigation.ROUTE_PAGE_PREFIX
import io.github.jmsdnns.androidmtrls.demo.scenes.common.ScenePanelChrome
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

/**
 * Demo-shell immersive controller. A page presenting a full-screen overlay that
 * supplies its own title bar (e.g. the Panel) calls this with `true` to hide the
 * catalog top bar, so only one bar shows — mirroring iOS, where a pushed panel
 * covers the nav bar. The page resets it to `false` on dismiss and when it leaves
 * the back stack. Defaults to a no-op for pages that never go immersive.
 */
val LocalDemoImmersive = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DemoApp()
        }
    }
}

// ---------------------------------------------------------------------------
// DemoApp — root composable
// ---------------------------------------------------------------------------

/**
 * Root composable for the AndroidMtrls demo catalog app.
 *
 * Architecture:
 * - [AMThemeRoot] installs [LocalAMIsDarkTheme] and [LocalAMTokens] for the
 *   entire composition tree.
 * - An in-app [isDark] toggle is hoisted here — above the [NavHost] — so the
 *   borderless dark toggle riding the shell top bar's trailing slot flips the
 *   theme for the whole catalog simultaneously. [CompositionLocalProvider]
 *   overrides [LocalAMIsDarkTheme] with the user's choice (same pattern as the
 *   Phase 1 gate scene, now promoted to the real shell).
 * - The pushed-page top bar (see [DemoTopBarHost]) lives outside the [NavHost] so
 *   it persists across navigation; it renders only on pushed component pages (home
 *   and full-screen scenes show no bar). It is the Android panel-bar chrome,
 *   reusing [ScenePanelChrome] — a borderless back arrow, a left-aligned app-name
 *   title, and a hairline divider under the bar.
 *   Re-tinting the accent from the Material You wallpaper palette remains a
 *   library capability (`AMTokensProvider` accent transform) but the demo exposes
 *   no toggle for it (owner ruling, 2026-07-06 audit).
 * - [DemoNavHost] manages two destinations: home list and "page/{entryId}".
 */
@Composable
fun DemoApp() {
    // In-app light/dark override, seeded from the system default.
    // rememberSaveable survives process death and configuration changes.
    val systemDark = isSystemInDarkTheme()
    var isDark by rememberSaveable { mutableStateOf(systemDark) }

    val navController = rememberNavController()

    // Immersive mode: a page presenting a full-screen overlay that carries its
    // own title bar (the Panel) flips this true to hide the catalog top bar, so
    // only one bar shows — mirroring iOS, where a pushed panel covers the nav bar.
    // The remembered setter keeps a stable identity so providing it does not churn
    // the static-local subtree.
    var immersive by remember { mutableStateOf(false) }
    val setImmersive = remember { { value: Boolean -> immersive = value } }

    // The catalog shell: the manual dark override wraps the top bar + NavHost.
    // Kept as a local composable so it can be hosted with or without the
    // dynamic-accent provider without duplicating the tree.
    val shell = @Composable {
        // Override LocalAMIsDarkTheme with the user's toggle choice so every
        // component in the catalog — including draw-scope shadow lambdas —
        // sees the correct mode without querying the system.
        CompositionLocalProvider(
            LocalAMIsDarkTheme provides isDark,
            LocalDemoImmersive provides setImmersive,
        ) {
            val pageColor = AMTheme.Surface.page.resolve(isDark)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageColor),
            ) {
                // Hidden while a page is immersive (full-screen panel supplies its
                // own bar), so the panel presents with a single title bar.
                if (!immersive) {
                    DemoTopBarHost(
                        navController = navController,
                        isDark = isDark,
                        onToggleDark = { isDark = !isDark },
                    )
                }

                // NavHost fills the remaining space below the top bar.
                DemoNavHost(navController = navController)
            }
        }
    }

    AMThemeRoot {
        shell()
    }
}

// ---------------------------------------------------------------------------
// DemoTopBarHost — resolves title + back state from the nav back stack
// ---------------------------------------------------------------------------

/**
 * Observes the current back-stack entry and drives the shell's pushed-page top
 * bar — the Android panel-bar chrome, reusing [ScenePanelChrome] (borderless back
 * arrow, left-aligned app-name title, hairline divider) with the global dark
 * toggle in its trailing slot.
 *
 * - On [ROUTE_HOME]: no bar at all (the large title lives in the home body, which
 *   takes its own status-bar inset).
 * - On "page/{entryId}": the bar shows the app name "AndroidMtrls Components"
 *   left-aligned next to the back arrow, with a borderless dark-toggle
 *   [AMIconButton] in the trailing slot. Back arrow pops the stack.
 * - On Screens entries: top bar is hidden entirely (full-screen scenes manage
 *   their own chrome).
 *
 * The shell owns the status-bar inset ([ScenePanelChrome] does not apply it), so
 * the chrome is wrapped in [statusBarsPadding]; the shell [Column] already paints
 * the page background behind it.
 */
@Composable
private fun DemoTopBarHost(
    navController: NavHostController,
    isDark: Boolean,
    onToggleDark: () -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val isHome = currentRoute == ROUTE_HOME

    // Resolve the current entry only to check if it is a full-screen scene.
    val currentEntry = if (currentRoute?.startsWith(ROUTE_PAGE_PREFIX) == true) {
        val entryId = backStackEntry?.arguments?.getString("entryId") ?: ""
        CatalogRegistry.byId[entryId]
    } else null

    // Full-screen scenes (Phase 11) manage their own chrome — skip the top bar.
    if (currentEntry?.section == ComponentSection.Screens) return

    // Home shows NO bar at all (iOS home carries no nav buttons; the large title
    // lives in the home body). Pushed pages show the app name + back/sun chips.
    if (isHome) return

    // The Android panel-bar chrome, reused from the scenes' shared ScenePanelChrome:
    // borderless back arrow, left-aligned app-name title, hairline divider. The
    // shell owns the status-bar inset (the chrome does not apply it). The global
    // dark toggle rides the trailing slot as a borderless AMIconButton — default
    // Medium size, matching the chrome's back arrow.
    ScenePanelChrome(
        title = "AndroidMtrls Components",
        onBack = { navController.popBackStack() },
        modifier = Modifier.statusBarsPadding(),
        trailing = {
            AMIconButton(
                icon = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode",
                onClick = onToggleDark,
            )
        },
    )
}
