package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow

/**
 * A panel's top app bar over art: for a panel whose background is a picture, such as an album
 * page over its cover. Material's top app bar with a solid back button, then the title over the
 * subtitle.
 *
 * ```kotlin
 * val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
 * AMBackdrop(art = { Box(Modifier.fillMaxSize().amArtBackdrop(album.coverUrl)) }, Modifier.fillMaxSize()) {
 *     Scaffold(
 *         modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
 *         topBar = {
 *             AMArtPanelChrome(
 *                 onBack = { backStack.removeLastOrNull() },
 *                 title = album.title,
 *                 subtitle = "${album.artist} · 9 songs · 38 min",
 *                 scrollBehavior = scrollBehavior,
 *             )
 *         },
 *         containerColor = Color.Transparent,
 *     ) { padding -> AlbumTracks(album, Modifier.verticalScroll(rememberScrollState()).padding(padding)) }
 * }
 * ```
 *
 * - **At rest** the bar lies on the art: the title and subtitle are white art text
 *   ([amArtText]), and the status bar's icons turn light, edge to edge, so they read over the
 *   darkened picture.
 * - **Once content scrolls under it** the bar fades in the frosted fill: the art under the bar,
 *   blurred, under [AMTheme.Frosted.fill], with a frosted edge along its bottom, reaching up
 *   behind the status bar. The title and subtitle fade over to the on-surface text and
 *   [AMTheme.Frosted.secondaryText], and the status bar's icons follow the app's appearance
 *   again. Outside an [AMBackdrop], or before the art is placed, the fill is the solid surface,
 *   never unblurred translucency.
 * - **The back button is always solid**, the solid form of [AMToolbarButton] (the floating
 *   surface with its shadow), so it stays the clearest thing over any picture, inside an
 *   [AMBackdrop] or not. It's Material's `arrow_back`, mirrored in right-to-left layouts, read as
 *   "Back", with a long-press tooltip naming it. It acts only while the panel is on the back
 *   stack, as [AMPanelChrome]'s arrow does: tapped again while its panel animates away, it does
 *   nothing.
 *
 * Place it inside the [AMBackdrop] whose art it sits on, so the frosted fill finds that art. The
 * fade uses Material's motion scheme. The panel itself is a Navigation 3 entry like any other
 * ([AMPanel]). Its light icons are an [AMLightStatusBarIcons] claim, counted with every other in
 * the window: leaving it puts the icons back as the app had them only once no other claim is
 * held, so a push from one art panel to another, or a pop to a view over art, keeps them light
 * throughout.
 *
 * @param onBack pops the panel: the back button calls it while the panel is on the back stack.
 * @param modifier the bar's placement.
 * @param title the bar's title, one line, read as a heading. Null shows the back button alone.
 * @param subtitle a line under the title, such as the artist and length. Null shows none.
 * @param scrollBehavior tells the bar when content has scrolled under it: usually
 *   `TopAppBarDefaults.pinnedScrollBehavior()`, its `nestedScrollConnection` on the screen.
 *   Without one the bar stays at rest.
 * @param windowInsets the insets the bar keeps clear of: the status bar by default. Its fill
 *   reaches behind them.
 */
@Composable
public fun AMArtPanelChrome(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    val scrolled by barScrolledState(scrollBehavior)
    val overArt = !scrolled
    val fill by oneRowBarFill(scrollBehavior)
    AMLightStatusBarIcons(enabled = overArt)

    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
    )
    val titleContent: @Composable () -> Unit = {
        if (title != null) ArtBarText(title, overArt, Color.Unspecified, Modifier.semantics { heading() })
    }
    // Solid wherever the bar sits: a crucial control over any picture.
    val backButton: @Composable () -> Unit = {
        ToolbarButton(painterResource(R.drawable.am_ic_arrow_back), PanelBackLabel, panelBack(onBack), solid = true)
    }

    FrostedBar(fill = { fill }, modifier) {
        if (subtitle == null) {
            TopAppBar(
                title = titleContent,
                navigationIcon = backButton,
                windowInsets = windowInsets,
                colors = colors,
                scrollBehavior = scrollBehavior,
            )
        } else {
            TopAppBar(
                title = titleContent,
                subtitle = { ArtBarText(subtitle, overArt, AMTheme.Frosted.secondaryText) },
                navigationIcon = backButton,
                windowInsets = windowInsets,
                colors = colors,
                scrollBehavior = scrollBehavior,
            )
        }
    }
}

/**
 * One line of the bar: white art text over the art, [frostedColor] on the frosted fill, fading
 * between the two with the fill.
 */
@Composable
private fun ArtBarText(text: String, overArt: Boolean, frostedColor: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.amCrossfade(overArt).then(if (overArt) Modifier.amArtText() else Modifier),
        color = if (overArt) Color.Unspecified else frostedColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
