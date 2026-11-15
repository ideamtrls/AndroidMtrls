package com.ideamtrls.androidmtrls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The root of a scene: one app screen, holding the views its remote reaches. Put it at the root of
 * the window, around everything the screen shows.
 *
 * ```kotlin
 * var showsRemote by rememberSaveable { mutableStateOf(false) }
 * AMScene(
 *     floatingActions = {
 *         AMFloatingActions(showsRemote, onExpandedChange = { showsRemote = it }) {
 *             AMActionGrid { AMActionGridRow({ … }, { … }) }
 *         }
 *     },
 * ) {
 *     Notes()
 * }
 * ```
 *
 * A scene with several root views takes them in the other form, which keeps each one's place and
 * steps back to the landing view ([AMKeepsPlace]).
 *
 * What it gives the screen:
 *
 * - **The remote's place.** [floatingActions] (an [AMFloatingActions]) lies over the content,
 *   panels pushed inside it included, since pushes are navigation, not overlays.
 * - **The overlay registry.** Every overlay opened inside it (a sheet, an alert, the remote, the
 *   spotlight, the loading overlay) reports to the scene while it's up, and the scene's floating
 *   action button fades out of its way.
 * - **The window's hosts.** An [AMBannerHost] and an [AMToastHost] at its root, so an [AMBanner]
 *   or [AMToast] posted anywhere inside shows at the top or the bottom of the screen. With
 *   [floatingActions], toasts show above the button's resting place, as Material's scaffold puts a
 *   snackbar above its floating action button.
 * - **Edge to edge.** It fills the space it's given in [containerColor], the page by default, and
 *   lays the content out behind the system bars: the content handles its own insets (an [AMPanel],
 *   an [AMPageHeader] in a scaffold), and the floating action button, banners and toasts keep clear
 *   of them themselves. Content takes `onSurface` as its content color.
 *
 * **Back** goes to the topmost window first, so an alert, then the spotlight or the remote, then a
 * sheet, each a window of its own; then the screen: a root's panels, the landing view, and out of
 * the app.
 *
 * @param modifier the scene's size and placement: usually the whole window.
 * @param floatingActions the remote's button and menu, an [AMFloatingActions]. Null shows none.
 * @param containerColor the color behind the content: [AMTheme.Surface.page] by default. A scene
 *   whose content draws its own backdrop passes `Color.Transparent`.
 * @param content the screen.
 */
@Composable
public fun AMScene(
    modifier: Modifier = Modifier,
    floatingActions: (@Composable () -> Unit)? = null,
    containerColor: Color = AMTheme.Surface.page,
    content: @Composable () -> Unit,
) {
    SceneRoot(modifier, floatingActions, containerColor, sceneBackdrops = null, content)
}

/**
 * The scene's root, [AMScene]'s body: the overlay registry, the window's banner and toast hosts,
 * and the content with the remote over it. The roots form passes its [sceneBackdrops], so the
 * scene's banners lie on the showing root's art while no backdrop is around the scene.
 */
@Composable
private fun SceneRoot(
    modifier: Modifier,
    floatingActions: (@Composable () -> Unit)?,
    containerColor: Color,
    sceneBackdrops: AMSceneBackdrops?,
    content: @Composable () -> Unit,
) {
    val registry = remember { AMOverlayRegistry() }
    // Toasts clear the button's resting place: its height and its gap above the navigation bar.
    val toastClearance = if (floatingActions != null) {
        floatingActionButtonTop()
    } else {
        0.dp
    }
    CompositionLocalProvider(
        LocalAMOverlayRegistry provides registry,
        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
    ) {
        BannerHost(modifier.fillMaxSize().background(containerColor), overScreen = false, sceneBackdrops = sceneBackdrops) {
            ToastHost(Modifier.fillMaxSize(), toastClearance) {
                Box(Modifier.fillMaxSize(), propagateMinConstraints = true) {
                    content()
                    floatingActions?.invoke()
                }
            }
        }
    }
}

/**
 * The root of a scene with several root views, the ones its remote reaches: an [AMScene] whose
 * content is an [AMKeepsPlace] of [current]. Each root's place is saved when you leave it and
 * restored when you come back, and back steps through the scene's views before it leaves the app.
 *
 * ```kotlin
 * enum class View { Feed, Alerts }
 *
 * var view by rememberSaveable { mutableStateOf(View.Feed) }
 * var showsRemote by rememberSaveable { mutableStateOf(false) }
 *
 * AMScene(
 *     current = view,
 *     landing = View.Feed,
 *     onCurrentChange = { view = it },
 *     floatingActions = {
 *         AMFloatingActions(showsRemote, onExpandedChange = { showsRemote = it }) {
 *             AMActionGrid {
 *                 AMActionGridRow(
 *                     {
 *                         AMActionGridButton(feedIcon, "Feed", active = view == View.Feed, onClick = {
 *                             showsRemote = false
 *                             amInstantSwitch { view = View.Feed }
 *                         })
 *                     },
 *                     { … "Alerts" … },
 *                 )
 *             }
 *         }
 *     },
 *     overlays = {
 *         AMSheet(showsCompose, onDismissRequest = { showsCompose = false }, title = "New post") { Compose() }
 *         AMToast(toast, onDismissRequest = { toast = null })
 *     },
 *     backStack = { root -> if (root == View.Feed) feed else emptyList() },
 * ) { root ->
 *     when (root) {
 *         View.Feed -> NavDisplay(feed, onBack = { feed.removeLastOrNull() }) { … }
 *         View.Alerts -> AlertsView()
 *     }
 * }
 * ```
 *
 * - **The remote switches the roots.** A view tile closes the remote, then changes [current] inside
 *   [amInstantSwitch], so the new root appears at once, restored where you left it, while the
 *   remote fades away over it. The current view's tile is `active`, ringed.
 * - **What belongs to the whole scene goes in [overlays]**: the sheets, alerts, spotlight, loading
 *   overlay and toasts any root can bring up. They're composed beside the roots, inside the scene
 *   (so they report to its overlay registry and show in its hosts) and outside every root, so a
 *   switch of root never takes down an open sheet, nor cuts one sliding away. Overlays a single
 *   root owns can stay in that root.
 * - **Overlays lie on the showing root's art.** A sheet in [overlays] frosts the backdrop the
 *   showing root draws, the outermost [AMBackdrop] or [AMArtRowsBackdrop] of the root or of the
 *   panel on top, and follows it as roots switch, panels come and go and the art changes. While
 *   the root draws none, it samples whatever backdrop is around the scene, or with none it's frost
 *   over the page color.
 * - **The remote's button lies on art too.** In a scene inside an [AMBackdrop] it frosts that
 *   backdrop's art. A scene with none, whose art is each page's own, lays it on the showing root's
 *   art, as it does [overlays], so the frosted + blurs the page under it.
 * - **So do the scene's banners.** The band behind a banner and the banner's card frost the same
 *   art the + does: the backdrop around the scene, or with none the showing root's. Only the banner
 *   layer lies on it; the roots keep their own backdrops.
 * - **Back to the landing view.** Back goes to the topmost window first (an alert, the spotlight or
 *   the remote, a sheet), then to the current root's panels (its `NavDisplay` pops them). On a root
 *   other than [landing] with no panel left, back calls [onCurrentChange] with [landing], an
 *   instant swap; on the landing root it leaves the app.
 *
 * Roots are values a `Bundle` can hold (an enum entry, a `String`, an `Int`); keep [current] in
 * `rememberSaveable`. Everything else is as in the plain [AMScene].
 *
 * @param current the root view showing.
 * @param landing the root the scene opens on, which back returns to before leaving the app.
 * @param onCurrentChange called with [landing] when back swaps to it. Apply it to [current].
 * @param modifier the scene's size and placement: usually the whole window.
 * @param floatingActions the remote's button and menu, an [AMFloatingActions], on the scene's
 *   backdrop or, with none, on the showing root's art. Null shows none.
 * @param overlays what the whole scene presents over its roots, whichever is showing: its sheets,
 *   alerts, spotlight, loading overlay and toasts. Composed after the roots and before the remote,
 *   on the showing root's art. Null composes nothing.
 * @param backStack a root's Navigation 3 back stack, its own view's entry first, kept in the app's
 *   state: the root shows a panel while it holds more than one entry. A root without panels
 *   returns an empty list, the default.
 * @param containerColor the color behind the content: [AMTheme.Surface.page] by default. A scene
 *   inside an [AMBackdrop] passes `Color.Transparent`.
 * @param content the root view for a root name.
 */
@Composable
public fun <T : Any> AMScene(
    current: T,
    landing: T,
    onCurrentChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    floatingActions: (@Composable () -> Unit)? = null,
    overlays: (@Composable () -> Unit)? = null,
    backStack: (root: T) -> List<*> = { emptyList<Any>() },
    containerColor: Color = AMTheme.Surface.page,
    content: @Composable (root: T) -> Unit,
) {
    val backdrops = remember { AMSceneBackdrops() }
    val actions = floatingActions?.let { slot -> @Composable { SceneFloatingActions(backdrops, slot) } }
    SceneRoot(modifier, actions, containerColor, backdrops) {
        CompositionLocalProvider(LocalAMSceneBackdrops provides backdrops) {
            AMKeepsPlace(current, landing, onCurrentChange, Modifier.fillMaxSize(), backStack, content)
        }
        // Beside the roots, never inside one: a switch of root leaves these up.
        if (overlays != null) SceneOverlays(backdrops, overlays)
    }
}

/**
 * The scene's overlays, on the art the showing root draws, or on the scene's own while it draws
 * none: the sheets among them sample it, from their own windows, by screen position.
 */
@Composable
private fun SceneOverlays(backdrops: AMSceneBackdrops, overlays: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAMBackdrop provides (backdrops.showing ?: LocalAMBackdrop.current),
        content = overlays,
    )
}

/**
 * The scene's remote, on the backdrop around the scene, or, in a scene with none, on the art the
 * showing root draws: its frosted button samples it in the scene's own window, by layout position.
 * The scene's backdrop comes first because it lies still under the button while Navigation 3 fades
 * and scales a root's pages.
 */
@Composable
private fun SceneFloatingActions(backdrops: AMSceneBackdrops, floatingActions: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAMBackdrop provides (LocalAMBackdrop.current ?: backdrops.showing),
        content = floatingActions,
    )
}
