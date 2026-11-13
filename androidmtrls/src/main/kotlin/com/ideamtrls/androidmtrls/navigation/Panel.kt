package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.dropUnlessStarted

/**
 * A panel: a screen pushed over the one you were on, under its own top app bar ([AMPanelChrome])
 * with a back arrow, its title and optional actions. The bar lies on the page at rest and takes
 * the page color once the content scrolls under it; inside an [AMBackdrop] its back arrow is solid
 * and it takes the frosted fill instead.
 *
 * A panel is a Navigation 3 entry. The back stack is the app's own state; push a key to show a
 * panel and build the panel from the key, so what it shows is fixed when it's pushed and stays
 * put while it animates away:
 *
 * ```kotlin
 * NavDisplay(
 *     backStack = backStack,
 *     onBack = { backStack.removeLastOrNull() },
 *     entryProvider = entryProvider {
 *         entry<Projects> { ProjectList(onOpen = { backStack.add(ProjectPanel(it.id)) }) }
 *         entry<ProjectPanel> { key ->
 *             val project = projects.getValue(key.id)
 *             AMPanel(project.name, onBack = { backStack.removeLastOrNull() }, actions = { ShareButton(project) }) { padding ->
 *                 ProjectTasks(project, Modifier.verticalScroll(rememberScrollState()).padding(padding))
 *             }
 *         }
 *     },
 * )
 * ```
 *
 * `NavDisplay` animates the push and the pop, follows predictive back, and keeps each covered
 * view's saved state (its scroll position) until it shows again; the panel adds no motion of its
 * own. Pass the same back action to [onBack] as to `NavDisplay`, so the arrow and system back
 * pop alike. The arrow acts only while its panel is on the back stack: once popped, while it
 * animates away, a second tap on it does nothing, so it never pops the view under it or empties
 * the back stack.
 *
 * [content] gets the padding that clears the bar and the system bars: apply it inside the
 * scrolling container, so the content scrolls under the bar edge to edge and the bar picks up the
 * page color as it does.
 *
 * @param title the panel's title, one line.
 * @param onBack pops the panel: the back arrow calls it.
 * @param modifier the panel's placement.
 * @param actions trailing icon buttons in the bar, such as share. Null shows none.
 * @param containerColor what the panel lies on: the page color by default. Pass
 *   [Color.Transparent] for a panel inside an [AMBackdrop], so the art shows behind the bar.
 * @param content the panel's content, given the padding that clears the bar and the system bars.
 */
@Composable
public fun AMPanel(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    containerColor: Color = AMTheme.Surface.page,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { AMPanelChrome(title, onBack, actions = actions, scrollBehavior = scrollBehavior) },
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = content,
    )
}

/**
 * A panel's top app bar: Material's top app bar with a back arrow, the panel's title and optional
 * actions, transparent at rest so it lies on the page, and the page color once content scrolls
 * under it. [AMPanel] builds a panel around it; use it directly for a panel laid out by hand.
 *
 * ```kotlin
 * val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
 * Scaffold(
 *     modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
 *     topBar = { AMPanelChrome("Atlas", onBack = ::pop, scrollBehavior = scrollBehavior) },
 *     containerColor = AMTheme.Surface.page,
 * ) { padding -> … }
 * ```
 *
 * The back arrow is an [AMToolbarButton] with Material's `arrow_back`, mirrored in right-to-left
 * layouts, read as "Back", so it follows the same rule as the toolbar buttons in [actions]: a bare
 * glyph on a page, and solid inside an [AMBackdrop], a crucial control over the art. It calls
 * [onBack] only while the bar's lifecycle is at least started: Navigation 3 lowers a popped
 * entry's lifecycle to created while it animates away, so the leaving panel's arrow takes no
 * second tap, and a panel still sliding in takes the first. The title
 * takes the top app bar's own type, on one line. Without a [scrollBehavior] the bar stays
 * transparent.
 *
 * **Over a picture** (inside an [AMBackdrop], on a transparent screen) the bar lies on the art at
 * rest, and once content scrolls under it fades in the frosted fill instead of the page color:
 * the art under the bar blurred under [AMTheme.Frosted.fill], with the frosted edge along its
 * bottom, reaching behind the status bar (the solid surface until the art is placed, never
 * unblurred translucency). The title keeps the bar's own color, which reads on the frosted fill;
 * for a bar over dark art with white titles use [AMArtPanelChrome].
 *
 * @param title the panel's title, one line.
 * @param onBack pops the panel: the back arrow calls it while the panel is on the back stack.
 * @param modifier the bar's placement.
 * @param actions trailing icon buttons, such as share: [AMToolbarButton]s. Null shows none.
 * @param scrollBehavior tells the bar when content has scrolled under it, so it takes the page
 *   color (the frosted fill over a picture): usually `TopAppBarDefaults.pinnedScrollBehavior()`,
 *   its `nestedScrollConnection` on the screen.
 * @param windowInsets the insets the bar keeps clear of: the status bar by default. The bar's
 *   color reaches behind them.
 */
@Composable
public fun AMPanelChrome(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    // Over art, content scrolling under the bar brings in the frosted fill, never a page band.
    val frosted = LocalAMBackdrop.current != null && scrollBehavior != null
    val bar = @Composable { barModifier: Modifier ->
        TopAppBar(
            title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = barModifier,
            // A toolbar button, so it follows the toolbar's rule: solid over art, plain on a page.
            navigationIcon = { AMToolbarButton(painterResource(R.drawable.am_ic_arrow_back), PanelBackLabel, panelBack(onBack)) },
            actions = actions ?: {},
            windowInsets = windowInsets,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = if (frosted) Color.Transparent else AMTheme.Surface.page,
            ),
            scrollBehavior = scrollBehavior,
        )
    }
    if (frosted) {
        val fill by oneRowBarFill(scrollBehavior)
        FrostedBar(fill = { fill }, modifier) { bar(Modifier) }
    } else {
        bar(modifier)
    }
}

/** What a screen reader says for a panel's back arrow. */
internal const val PanelBackLabel: String = "Back"

/**
 * [onBack] for a panel bar's back button, acting only while the bar's lifecycle is at least
 * started. Navigation 3 lowers a popped entry's lifecycle to created at once, but keeps the entry
 * drawn on top, and touchable, while it animates away. Without this a second tap on the button
 * would reach the leaving panel and pop again: the view under it, or, from a panel one deep, the
 * root's own entry, which empties the back stack. System back never reaches a popped entry, so
 * this makes the button and system back agree. Started, not resumed: an entry still sliding in is
 * held below resumed until its transition ends, and its button still pops it, as system back does.
 */
@Composable
internal fun panelBack(onBack: () -> Unit): () -> Unit = dropUnlessStarted(block = onBack)
