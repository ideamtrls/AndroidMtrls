package com.ideamtrls.androidmtrls.demo

import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.ideamtrls.androidmtrls.AMActionGrid
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow
import com.ideamtrls.androidmtrls.AMAlert
import com.ideamtrls.androidmtrls.AMAlertAction
import com.ideamtrls.androidmtrls.AMAlertItem
import com.ideamtrls.androidmtrls.AMBanner
import com.ideamtrls.androidmtrls.AMFloatingActions
import com.ideamtrls.androidmtrls.AMScene
import com.ideamtrls.androidmtrls.AMSheet
import com.ideamtrls.androidmtrls.AMToast
import kotlinx.coroutines.launch

internal val SocialSceneEntry = ComponentEntry(
    id = "socialScene",
    category = CatalogCategory.Showcase,
    title = "Social",
    summary = "A feed, alerts, and profiles",
    icon = R.drawable.ic_group,
    keywords = listOf(
        "AMScene", "AMKeepsPlace", "NavDisplay", "feed", "swipeable row", "segmented control", "chip group", "tabs",
        "search", "compose", "avatar", "badge", "spotlight", "scene",
    ),
) { SocialScene() }

/**
 * Social: computing's pioneers posting as if they had phones. One [SocialModel] owns the posts,
 * people, alerts, both root views' back stacks and every overlay. The Feed (the landing view) and
 * the Alerts are the roots of the [AMScene], which the remote swaps instantly, each keeping its
 * place; each root pushes its post, profile and alerts panels on its own Navigation 3 back stack.
 * The whole scene sits on one branded backdrop.
 */
@Composable
internal fun SocialScene() {
    val scope = rememberCoroutineScope()
    val saver = remember(scope) { SocialModel.saver(scope) }
    val model = rememberSaveable(saver = saver) { SocialModel(scope) }
    val navigator = LocalCatalogNavigator.current
    val clipboard = LocalClipboard.current
    var showsRemote by rememberSaveable { mutableStateOf(false) }
    val postActions = remember(model, clipboard, scope) {
        SocialPostActions(model) { post ->
            scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Post", post.text))) }
        }
    }

    // The first load runs once; leaving the scene abandons whatever is still running.
    LaunchedEffect(model) { model.load() }
    DisposableEffect(model) { onDispose { model.cancelPendingWork() } }

    SocialBackdrop {
        AMScene(
            current = model.currentView,
            landing = SocialView.Feed,
            onCurrentChange = { view ->
                model.cancelPendingWork()
                model.currentView = view
            },
            floatingActions = {
                AMFloatingActions(showsRemote, onExpandedChange = { showsRemote = it }, contentDescription = "Actions") {
                    SocialRemote(
                        model,
                        onClose = { showsRemote = false },
                        onExit = {
                            showsRemote = false
                            model.cancelPendingWork()
                            navigator.back()
                        },
                    )
                }
            },
            backStack = { view -> model.stack(view) },
            containerColor = Color.Transparent,
        ) { view ->
            SocialRootStack(model, view, postActions)
            SocialOverlays(model, postActions)
        }
    }
}

/**
 * One root view and the panels pushed over it, on its own back stack: Navigation 3 pushes and pops
 * them, back and predictive back included, and keeps each covered entry's place.
 */
@Composable
private fun SocialRootStack(model: SocialModel, view: SocialView, postActions: SocialPostActions) {
    val stack = model.stack(view)
    val pop = { model.pop(view) }
    NavDisplay(
        backStack = stack,
        modifier = Modifier,
        onBack = pop,
        entryProvider = { route ->
            when (route) {
                SocialRoute.Root -> NavEntry<SocialRoute>(route) {
                    when (view) {
                        SocialView.Feed -> SocialFeed(model, postActions)
                        SocialView.Alerts -> SocialAlerts(model)
                    }
                }
                is SocialRoute.Post -> NavEntry<SocialRoute>(route) { SocialPostDetail(model, route.postId, onBack = pop) }
                is SocialRoute.Profile -> NavEntry<SocialRoute>(route) { SocialProfile(model, route.userId, onBack = pop) }
                is SocialRoute.Alerts -> NavEntry<SocialRoute>(route) { SocialAlertsPanel(model, onBack = pop) }
            }
        },
    )
}

/**
 * The remote: Compose and Search over Feed, Alerts, Profile and Exit, the same wherever the scene
 * stands. Feed and Alerts swap the root, the showing one ringed; Profile leaves the root that
 * shows with only your own profile over it.
 */
@Composable
private fun SocialRemote(model: SocialModel, onClose: () -> Unit, onExit: () -> Unit) {
    // The menu closes outside the switch, so its fade plays over the new view.
    fun show(view: SocialView) {
        onClose()
        model.switchTo(view)
    }
    AMActionGrid {
        AMActionGridRow(
            {
                AMActionGridButton(painterResource(R.drawable.ic_edit_square), "Compose", onClick = {
                    onClose()
                    model.beginPost()
                })
            },
            {
                AMActionGridButton(painterResource(R.drawable.ic_search), "Search", onClick = {
                    onClose()
                    model.beginSearch()
                })
            },
        )
        AMActionGridRow(
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_feed), "Feed",
                    onClick = { show(SocialView.Feed) },
                    active = model.currentView == SocialView.Feed,
                )
            },
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_notifications), "Alerts",
                    onClick = { show(SocialView.Alerts) },
                    active = model.currentView == SocialView.Alerts,
                )
            },
            {
                AMActionGridButton(painterResource(R.drawable.ic_account_circle), "Profile", onClick = {
                    onClose()
                    model.openOwnProfile()
                })
            },
            { AMActionGridButton(painterResource(R.drawable.ic_logout), "Exit", onClick = onExit) },
        )
    }
}

/**
 * Everything the scene presents over its views: the compose and search sheets (frosted in dark
 * mode, like the cards), the "Delete post?" alert, the post menu in the spotlight, the mute banner
 * (a tap undoes it) and the toasts.
 */
@Composable
private fun SocialOverlays(model: SocialModel, postActions: SocialPostActions) {
    val sheetSurface = socialSheetSurface()
    AMSheet(
        model.composing,
        onDismissRequest = { model.composing = false },
        title = model.composeTitle,
        surface = sheetSurface,
    ) { SocialComposeSheet(model) }
    AMSheet(
        model.searching,
        onDismissRequest = { model.searching = false },
        title = "Search",
        surface = sheetSurface,
    ) { SocialSearchSheet(model) }

    val deleteId = model.confirmDeleteId
    val deleteAlert = remember(deleteId) {
        deleteId?.let { id ->
            AMAlertItem(
                "Delete post?",
                confirm = AMAlertAction("Delete", destructive = true) { model.delete(id) },
                message = "It's removed from your profile and the feed.",
                cancel = AMAlertAction("Cancel"),
            )
        }
    }
    AMAlert(deleteAlert, onDismissRequest = { model.confirmDeleteId = null })

    SocialPostMenu(model, postActions)

    AMBanner(model.banner, onDismissRequest = { model.banner = null }, onClick = model::undoMute)
    AMToast(model.toast, onDismissRequest = { model.toast = null })
}
