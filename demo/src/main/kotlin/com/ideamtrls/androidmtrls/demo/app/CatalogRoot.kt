package com.ideamtrls.androidmtrls.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay

/** A place in the catalog: the list, one entry's page, or a panel that page pushed. */
internal sealed interface CatalogKey {
    data object Catalog : CatalogKey

    data class Entry(val id: String) : CatalogKey

    /** One of an entry page's panels: the entry's id and the panel's [route] on its page. */
    data class Panel(val id: String, val route: String) : CatalogKey
}

/**
 * Opens entries and their panels, and goes back. [CatalogRoot] provides it to the list and the
 * pages.
 */
@Stable
internal class CatalogNavigator(private val backStack: MutableList<CatalogKey>) {
    /** Pushes the entry's page (or, for Showcase, its scene). */
    fun open(entry: ComponentEntry) {
        push(CatalogKey.Entry(entry.id))
    }

    /**
     * Pushes one of [entry]'s panels, named by [route] (not empty), over its page. The entry's
     * page draws it through [CatalogPanels].
     */
    fun openPanel(entry: ComponentEntry, route: String) {
        require(route.isNotEmpty()) { "A panel route names the panel; it can't be empty" }
        push(CatalogKey.Panel(entry.id, route))
    }

    /** Pushes [key] unless it's already on the stack (a double tap). */
    private fun push(key: CatalogKey) {
        if (key !in backStack) backStack.add(key)
    }

    /** Pops the top page or panel. The catalog list stays; back from it leaves the app. */
    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}

internal val LocalCatalogNavigator = staticCompositionLocalOf<CatalogNavigator> { error("CatalogRoot provides the navigator") }

/** The panel route an entry's page is drawn for, or null where it's drawn as the page itself. */
private val LocalCatalogPanelRoute = staticCompositionLocalOf<String?> { null }

/**
 * A page that pushes panels onto the catalog's back stack ([CatalogNavigator.openPanel]): draws
 * [page] as the entry's page, and [panel] for each of its panels, by the route it was pushed with.
 * The route is all the back stack keeps, so a panel comes back after process death.
 */
@Composable
internal fun CatalogPanels(page: @Composable () -> Unit, panel: @Composable (route: String) -> Unit) {
    when (val route = LocalCatalogPanelRoute.current) {
        null -> page()
        else -> panel(route)
    }
}

/**
 * The catalog: a Navigation 3 back stack whose root is the catalog list, with each entry's page
 * pushed over it, and over a page any panels it pushes. Back and predictive back pop it. The back
 * stack survives configuration changes and process death, and each page keeps its own saved
 * state (its scroll position) while it's covered.
 *
 * The `entry` launch extra opens that entry over the list.
 */
@Composable
internal fun CatalogRoot() {
    val backStack = rememberSaveable(saver = BackStackSaver) { initialBackStack() }
    val navigator = remember(backStack) { CatalogNavigator(backStack) }

    CompositionLocalProvider(LocalCatalogNavigator provides navigator) {
        NavDisplay(
            backStack = backStack,
            onBack = navigator::back,
            entryProvider = { key ->
                when (key) {
                    CatalogKey.Catalog -> NavEntry(key) { CatalogList() }
                    is CatalogKey.Entry -> NavEntry(key) { CatalogRegistry.entry(key.id)?.page?.invoke() }
                    is CatalogKey.Panel -> NavEntry(key) {
                        CompositionLocalProvider(LocalCatalogPanelRoute provides key.route) {
                            CatalogRegistry.entry(key.id)?.page?.invoke()
                        }
                    }
                }
            },
        )
    }
}

private fun initialBackStack(): SnapshotStateList<CatalogKey> {
    val launched = LaunchOptions.entry?.let(CatalogRegistry::entry)
    return listOfNotNull<CatalogKey>(CatalogKey.Catalog, launched?.let { CatalogKey.Entry(it.id) }).toMutableStateList()
}

/**
 * Saves what's pushed over the list as pairs of strings, an entry's id and then a panel's route,
 * empty for the entry's page itself. The catalog list is always the root; entries no longer
 * registered drop out on restore, their panels with them.
 */
private val BackStackSaver = listSaver<SnapshotStateList<CatalogKey>, String>(
    save = { stack ->
        stack.flatMap { key ->
            when (key) {
                CatalogKey.Catalog -> emptyList()
                is CatalogKey.Entry -> listOf(key.id, "")
                is CatalogKey.Panel -> listOf(key.id, key.route)
            }
        }
    },
    restore = { saved ->
        val pushed = saved.chunked(2)
            .filter { (id, _) -> CatalogRegistry.entry(id) != null }
            .map { (id, route) -> if (route.isEmpty()) CatalogKey.Entry(id) else CatalogKey.Panel(id, route) }
        (listOf<CatalogKey>(CatalogKey.Catalog) + pushed).toMutableStateList()
    },
)
