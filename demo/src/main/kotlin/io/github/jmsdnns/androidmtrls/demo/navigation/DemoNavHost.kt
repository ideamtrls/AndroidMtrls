package io.github.jmsdnns.androidmtrls.demo.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.jmsdnns.androidmtrls.demo.catalog.CatalogRegistry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentPage
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.home.CatalogHomeScreen
import io.github.jmsdnns.androidmtrls.demo.scenes.common.LocalSceneExit

// ---------------------------------------------------------------------------
// Route constants
// ---------------------------------------------------------------------------

/** Route string for the catalog home (section list). */
const val ROUTE_HOME = "home"

/** Route prefix for individual component pages. Full route: "page/{entryId}". */
const val ROUTE_PAGE_PREFIX = "page"

/** Full parametrized route for a component page. */
const val ROUTE_PAGE = "$ROUTE_PAGE_PREFIX/{entryId}"

/** NavBackStackEntry argument key for the entry id. */
const val ARG_ENTRY_ID = "entryId"

// ---------------------------------------------------------------------------
// DemoNavHost
// ---------------------------------------------------------------------------

/**
 * Root [NavHost] for the demo catalog app.
 *
 * Two destinations:
 * - [ROUTE_HOME]: The [CatalogHomeScreen] — a grouped tile grid of all sections
 *   and their entries.
 * - [ROUTE_PAGE]: A single component page, looked up from [CatalogRegistry.byId] by
 *   [ARG_ENTRY_ID]. Scenes (section == [ComponentSection.Screens]) render without the
 *   [ComponentPage] scaffold; all other entries render inside [ComponentPage].
 *
 * Transitions follow the Material shared-axis X pattern: the incoming and outgoing
 * destinations slide a short distance along the horizontal axis (1/10 of the width)
 * while cross-fading, with the fade-in delayed so the outgoing content clears first.
 * Push moves forward (enter from the right, exit to the left); pop mirrors it (enter
 * from the left, exit to the right). Predictive back drives the pop from the gesture.
 *
 * **Registry lookup pattern:** entry lambdas cannot travel through NavBackStackEntry
 * arguments (not serializable). Instead, the destination extracts [ARG_ENTRY_ID] from
 * the back-stack entry and looks up the [ComponentEntry] in [CatalogRegistry.byId].
 * All composable factories live in memory — the lookup is O(1).
 *
 * @param navController The [NavHostController] shared with the activity shell.
 */
@Composable
fun DemoNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME,
    ) {
        // Home is the "back" destination: it exits to the left on push and
        // re-enters from the left on pop — the mirror of a page's own exit/pop-enter.
        composable(
            route = ROUTE_HOME,
            exitTransition = {
                slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 10 } +
                    fadeOut(tween(90))
            },
            popEnterTransition = {
                slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 10 } +
                    fadeIn(tween(210, delayMillis = 90))
            },
        ) {
            CatalogHomeScreen(
                onEntryClick = { entry ->
                    navController.navigate("$ROUTE_PAGE_PREFIX/${entry.id}")
                },
            )
        }

        // Page enters from the right on push and exits to the right on pop —
        // the forward half of the shared-axis X transition.
        composable(
            route = ROUTE_PAGE,
            enterTransition = {
                slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 10 } +
                    fadeIn(tween(210, delayMillis = 90))
            },
            exitTransition = {
                slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 10 } +
                    fadeOut(tween(90))
            },
            popEnterTransition = {
                slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 10 } +
                    fadeIn(tween(210, delayMillis = 90))
            },
            popExitTransition = {
                slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 10 } +
                    fadeOut(tween(90))
            },
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString(ARG_ENTRY_ID) ?: return@composable
            val entry = CatalogRegistry.byId[entryId] ?: return@composable

            if (entry.section == ComponentSection.Screens) {
                // Scene entries render full-screen — no ComponentPage scaffold,
                // no top bar. The scene itself manages its own chrome and exit FAB.
                // LocalSceneExit lets a parameterless scene's Exit tile pop back to
                // the catalog without knowing about the nav controller.
                CompositionLocalProvider(
                    LocalSceneExit provides { navController.popBackStack() },
                ) {
                    entry.content()
                }
            } else if (entry.ownsScaffold) {
                // The page composes ComponentPage itself so it can wrap the
                // scaffold in an overlay host (full-page blur/scrim coverage).
                // The top bar stays — only the scaffold ownership moves.
                entry.content()
            } else {
                ComponentPage(entry = entry, showsTitle = entry.showsTitle) {
                    entry.content()
                }
            }
        }
    }
}
