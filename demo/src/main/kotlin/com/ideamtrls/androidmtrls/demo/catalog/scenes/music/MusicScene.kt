package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.ideamtrls.androidmtrls.AMFloatingActions
import com.ideamtrls.androidmtrls.AMScene
import com.ideamtrls.androidmtrls.AMSpotlight
import com.ideamtrls.androidmtrls.AMSurfaceFill
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMToast

internal val MusicSceneEntry = ComponentEntry(
    id = "musicScene",
    category = CatalogCategory.Showcase,
    title = "Music",
    summary = "Artists, albums and a player over the art",
    icon = R.drawable.ic_play_circle,
    keywords = listOf(
        "music", "player", "art backdrop", "frosted", "slider", "queue", "reorder", "album", "artist", "streaming",
        "AMScene", "AMKeepsPlace", "AMArtPanelChrome", "AMBackdrop", "amArtBackdrop", "amArtRows", "AMArtRowsBackdrop",
        "AMArtTone", "AMSlider", "AMSpotlight", "amHoldable", "AMReorderableRowList", "AMSheet", "scene",
    ),
) { MusicScene() }

/**
 * Music: a player over the owner's catalog from jmsdnns.com, its songs streamed from the site. One
 * [MusicModel] owns the catalog, the queue and playback, navigation, and every overlay. The two
 * root views, Artists and the Player, swap instantly from the remote, each keeping its place
 * ([AMScene]'s roots form). Artists is a Navigation 3 stack on the model's back stack, pushing the
 * artist and album panels, each over its own art under [com.ideamtrls.androidmtrls.AMArtPanelChrome];
 * the Player sits outside it, so a trip to the Player leaves the stack as it was.
 *
 * The Queue sheet, the spotlight and the toasts are the scene's overlays, beside the roots, so the
 * remote's button fades under them, a switch of root never interrupts them, and the frosted Queue
 * sheet blurs the art of whichever page shows. Playback stops when the scene leaves.
 */
@Composable
internal fun MusicScene() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current.applicationContext
    val saver = remember(scope, context) { MusicModel.saver(scope, context) }
    val model = rememberSaveable(saver = saver) { MusicModel(scope, context) }
    val navigator = LocalCatalogNavigator.current

    LaunchedEffect(model) { model.load() }
    // Leaving the scene stops the music and lets go of the audio focus.
    DisposableEffect(model) { onDispose { model.stop() } }

    // The scene is a frosted app, from its root.
    AMTheme(surfaceFill = AMSurfaceFill.Frosted) {
        AMScene(
            current = model.rootView,
            landing = MusicRootView.Artists,
            onCurrentChange = { model.rootView = it },
            floatingActions = {
                AMFloatingActions(
                    model.showingMenu,
                    onExpandedChange = { model.showingMenu = it },
                    contentDescription = "Actions",
                ) {
                    MusicMenu(
                        model,
                        onExit = {
                            model.showingMenu = false
                            model.stop()
                            navigator.back()
                        },
                    )
                }
            },
            overlays = { MusicOverlays(model) },
            backStack = { root -> if (root == MusicRootView.Artists) model.backStack else emptyList<MusicRoute>() },
        ) { root ->
            when (root) {
                MusicRootView.Artists -> MusicArtistsStack(model)
                MusicRootView.Player -> MusicPlayerView(model)
            }
        }
    }
}

/**
 * The Artists root view: the Navigation 3 stack of the Artists page and the panels it pushes. Each
 * page sits on its own backdrop (the Artists page and the artist page on their art rows, the album
 * page on its cover's art), so its light/dark chip and toolbar buttons are chips and its frosted
 * surfaces blur its own art; the page color behind them shows while Navigation 3 moves between
 * pages.
 */
@Composable
private fun MusicArtistsStack(model: MusicModel) {
    NavDisplay(
        backStack = model.backStack,
        modifier = Modifier.fillMaxSize().background(AMTheme.Surface.page),
        onBack = model::back,
        entryProvider = { route ->
            when (route) {
                MusicRoute.Artists -> NavEntry(route) { MusicArtistsView(model) }
                is MusicRoute.Artist -> NavEntry(route) { MusicArtistPage(route.id, model) }
                is MusicRoute.Album -> NavEntry(route) { MusicAlbumPage(route.id, model) }
            }
        },
    )
}

/** The Queue sheet, the press-and-hold queue menu in the spotlight, and the scene's toasts. */
@Composable
private fun MusicOverlays(model: MusicModel) {
    MusicQueueSheet(model)
    val icons = rememberMusicQueueIcons()
    AMSpotlight(
        item = model.queueMenuTarget,
        onDismissRequest = { model.queueMenuTarget = null },
        actions = { target -> musicQueueActions(target, model, icons) },
    ) { target ->
        MusicQueuePreview(target, model)
    }
    AMToast(model.toast, onDismissRequest = { model.toast = null })
}
