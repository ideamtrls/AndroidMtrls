package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.painterResource
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.ideamtrls.androidmtrls.AMActionGrid
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMFloatingActions
import com.ideamtrls.androidmtrls.AMLoadingOverlay
import com.ideamtrls.androidmtrls.AMScene
import com.ideamtrls.androidmtrls.AMSheet
import com.ideamtrls.androidmtrls.AMSheetSurface
import com.ideamtrls.androidmtrls.AMSpotlight
import com.ideamtrls.androidmtrls.AMToast
import com.ideamtrls.androidmtrls.amInstantSwitch

internal val ShopSceneEntry = ComponentEntry(
    id = "shopScene",
    category = CatalogCategory.Showcase,
    title = "Shop",
    summary = "A coffee shop with a bag, checkout, and orders",
    icon = R.drawable.ic_storefront,
    keywords = listOf(
        "AMScene", "AMKeepsPlace", "AMPanel", "Navigation 3", "AMCarousel", "AMChipGroup", "AMSpotlight",
        "amHoldable", "quick add", "press and hold", "skeleton", "shimmer", "AMSheet", "frosted", "discard guard",
        "AMTimeline", "AMSwipeableRow", "AMStepper", "coffee", "bag", "checkout", "orders", "scene",
    ),
) { ShopScene() }

/**
 * Shop: Ferry Line, a coffee roaster in Red Hook. One [ShopModel] owns the catalog, the bag, the
 * orders, each view's back stack and every overlay. The Shop, Bag and Orders views are the roots
 * of the [AMScene], which the remote swaps instantly; each root is a Navigation 3 back stack whose
 * panels (a product, an order, the bag over the shop) push over it.
 *
 * One backdrop of roasted beans runs behind everything, so the roots, the frosted cards and the
 * frosted sheets all sit on it: an [AMBackdrop] holds the whole scene, which lies transparent on
 * it. Panels bring their own backdrop, so they cover the root they push over.
 */
@Composable
internal fun ShopScene() {
    val scope = rememberCoroutineScope()
    val saver = remember(scope) { ShopModel.saver(scope) }
    val model = rememberSaveable(saver = saver) { ShopModel(scope) }
    val navigator = LocalCatalogNavigator.current
    var showsRemote by rememberSaveable { mutableStateOf(false) }

    // Leaving the scene abandons the order in flight, so nothing lands after it's gone.
    DisposableEffect(model) { onDispose { model.cancelPendingWork() } }
    LaunchedEffect(model) { model.load() }

    AMBackdrop(art = { ShopBackdrop() }, Modifier.fillMaxSize()) {
        AMScene(
            current = model.currentView,
            landing = ShopView.Shop,
            onCurrentChange = { model.currentView = it },
            floatingActions = {
                AMFloatingActions(showsRemote, onExpandedChange = { showsRemote = it }, contentDescription = "Actions") {
                    ShopRemote(
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
            overlays = { ShopOverlays(model) },
            backStack = { model.stack(it) },
            containerColor = Color.Transparent,
        ) { view ->
            ShopRootStack(model, view)
        }
    }
}

/**
 * One root view's back stack: the root view first, then the panels pushed over it. Back and
 * predictive back pop them with Navigation 3's own motion.
 */
@Composable
private fun ShopRootStack(model: ShopModel, view: ShopView) {
    val pop = { model.pop(view) }
    NavDisplay(
        backStack = model.stack(view),
        onBack = pop,
        entryProvider = { route ->
            NavEntry(route) {
                when (route) {
                    is ShopRoute.Root -> when (route.view) {
                        ShopView.Shop -> ShopHome(model)
                        ShopView.Bag -> ShopBag(model, onBack = null)
                        ShopView.Orders -> ShopOrders(model)
                    }
                    is ShopRoute.Product -> ShopProductPanel(model, route.id, onBack = pop)
                    is ShopRoute.Order -> ShopOrderPanel(model, route.number, onBack = pop)
                    ShopRoute.Bag -> ShopBag(model, onBack = pop)
                }
            }
        },
    )
}

/**
 * The remote's menu, the same wherever the scene stands: Search and Bag over Shop, Orders and
 * Exit, the view showing ringed. Search opens the Search sheet; Bag, Shop and Orders swap the root,
 * each coming back as it was left. Nothing greys: an empty bag is a real screen.
 */
@Composable
private fun ShopRemote(model: ShopModel, onClose: () -> Unit, onExit: () -> Unit) {
    // The menu closes outside the switch, so its fade plays over the new view.
    fun show(view: ShopView) {
        onClose()
        amInstantSwitch { model.currentView = view }
    }
    AMActionGrid {
        AMActionGridRow(
            {
                AMActionGridButton(painterResource(R.drawable.ic_search), "Search", onClick = {
                    onClose()
                    model.beginSearch()
                })
            },
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_shopping_bag), "Bag",
                    onClick = { show(ShopView.Bag) },
                    active = model.currentView == ShopView.Bag,
                )
            },
        )
        AMActionGridRow(
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_storefront), "Shop",
                    onClick = { show(ShopView.Shop) },
                    active = model.currentView == ShopView.Shop,
                )
            },
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_package_2), "Orders",
                    onClick = { show(ShopView.Orders) },
                    active = model.currentView == ShopView.Orders,
                )
            },
            { AMActionGridButton(painterResource(R.drawable.ic_logout), "Exit", onClick = onExit) },
        )
    }
}

/**
 * Everything the scene presents over its views, whichever is showing, so a root swap never takes
 * one down: the Search and Checkout sheets (frosted, blurring the beans around the scene), the
 * quick-add spotlight, the loading overlay over the checkout it places, and the toasts.
 */
@Composable
private fun ShopOverlays(model: ShopModel) {
    AMSheet(
        model.showsSearch,
        onDismissRequest = { model.showsSearch = false },
        title = "Search",
        surface = AMSheetSurface.Frosted,
    ) { ShopSearchSheet(model) }
    AMSheet(
        model.showsCheckout,
        onDismissRequest = { model.showsCheckout = false },
        title = "Checkout",
        surface = AMSheetSurface.Frosted,
    ) { ShopCheckoutSheet(model) }

    AMSpotlight(
        item = if (model.showsQuickAdd) model.quickAddProduct else null,
        onDismissRequest = { model.showsQuickAdd = false },
        actions = { product -> ShopQuickAddTiles(product, model) },
    ) { product -> ShopQuickAddPreview(product) }

    AMLoadingOverlay(model.isPlacingOrder, "Placing order…")
    AMToast(model.toast, onDismissRequest = { model.toast = null })
}
