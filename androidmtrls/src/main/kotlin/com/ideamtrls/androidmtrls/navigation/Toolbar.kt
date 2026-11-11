package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A toolbar: Material's center-aligned top app bar, a navigation button at the start, the title
 * (and an optional subtitle under it) centered, and actions at the end. It lies on whatever is
 * behind it, with no fill or outline of its own, and once content scrolls under it takes the page
 * color, or over a picture (inside an [AMBackdrop]) the frosted fill.
 *
 * ```kotlin
 * AMToolbar(
 *     "Inbox",
 *     navigationIcon = { AMToolbarButton(painterResource(R.drawable.ic_menu), "Menu", onClick = ::openMenu) },
 *     actions = { AMToolbarButton(painterResource(R.drawable.ic_edit_square), "Compose", onClick = ::compose) },
 * )
 * ```
 *
 * Fill its slots with [AMToolbarButton]s. Visual only: taps stay with the buttons in the slots.
 * The title and subtitle each take one line, ellipsized, in the bar's own type; the title stays
 * centered whatever the slots hold. On a picture ([AMBackdrop]) the toolbar's buttons turn solid;
 * give the picture a wash light or dark enough for the title to read on it. Scrolled over the
 * picture, the bar fades in the picture under it blurred under [AMTheme.Frosted.fill], with the
 * frosted edge along its bottom (the solid surface until the art is placed), never a page band.
 *
 * @param title the toolbar's title, one line.
 * @param modifier the toolbar's placement.
 * @param subtitle a line under the title. Null shows none.
 * @param navigationIcon the button at the start, such as back or a menu. Null shows none.
 * @param actions the buttons at the end, ⋮ last. Null shows none.
 * @param scrollBehavior tells the toolbar when content has scrolled under it, so it takes the
 *   page color (the frosted fill over a picture): usually `TopAppBarDefaults.pinnedScrollBehavior()`,
 *   its `nestedScrollConnection` on the screen. Without one it stays transparent.
 * @param windowInsets the insets the toolbar keeps clear of: the status bar by default. Pass
 *   `WindowInsets(0)` for a toolbar that doesn't reach the top of the screen.
 */
@Composable
public fun AMToolbar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    AMToolbar(
        modifier = modifier,
        subtitle = subtitle?.let { line -> { Text(line, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        windowInsets = windowInsets,
    ) {
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * A toolbar with custom title content: Material's center-aligned top app bar, a navigation button
 * at the start, [title] centered, and actions at the end. It lies on whatever is behind it, with
 * no fill or outline of its own, and once content scrolls under it takes the page color, or over a
 * picture (inside an [AMBackdrop]) the frosted fill.
 *
 * ```kotlin
 * AMToolbar(navigationIcon = { AMToolbarButton(back, "Back", onClick = ::pop) }) {
 *     Row(verticalAlignment = Alignment.CenterVertically) { Icon(lock, null); Text("Vault") }
 * }
 * ```
 *
 * The bar sets the title's and the subtitle's type and color around what the slots emit, so
 * custom content stays on-system. Visual only: taps stay with the content in the slots. On a
 * picture ([AMBackdrop]) the toolbar's buttons turn solid, and scrolled content brings in the
 * frosted fill.
 *
 * @param modifier the toolbar's placement.
 * @param subtitle a line under the title. Null shows none.
 * @param navigationIcon the button at the start, such as back or a menu. Null shows none.
 * @param actions the buttons at the end, ⋮ last. Null shows none.
 * @param scrollBehavior tells the toolbar when content has scrolled under it, so it takes the
 *   page color (the frosted fill over a picture). Without one it stays transparent.
 * @param windowInsets the insets the toolbar keeps clear of: the status bar by default.
 * @param title the centered title content.
 */
@Composable
public fun AMToolbar(
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    title: @Composable () -> Unit,
) {
    // Over art, content scrolling under the bar brings in the frosted fill, never a page band.
    val frosted = LocalAMBackdrop.current != null && scrollBehavior != null
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = if (frosted) Color.Transparent else AMTheme.Surface.page,
    )
    val bar = @Composable { barModifier: Modifier ->
        if (subtitle == null) {
            CenterAlignedTopAppBar(
                title = title,
                modifier = barModifier,
                navigationIcon = navigationIcon ?: {},
                actions = actions ?: {},
                windowInsets = windowInsets,
                colors = colors,
                scrollBehavior = scrollBehavior,
            )
        } else {
            TopAppBar(
                title = title,
                subtitle = subtitle,
                modifier = barModifier,
                navigationIcon = navigationIcon ?: {},
                actions = actions ?: {},
                titleHorizontalAlignment = Alignment.CenterHorizontally,
                windowInsets = windowInsets,
                colors = colors,
                scrollBehavior = scrollBehavior,
            )
        }
    }
    if (frosted) {
        val fill by oneRowBarFill(scrollBehavior)
        FrostedBar(fill = { fill }, modifier) { bar(Modifier) }
    } else {
        bar(modifier)
    }
}

/**
 * A toolbar with no title: Material's top app bar holding only its buttons, a navigation button at
 * the start and actions at the end, for a screen whose title is elsewhere or that has none (a
 * welcome screen with only a light/dark switch in its bar). It lies on whatever is behind it, and
 * once content scrolls under it takes the page color, or over a picture (inside an [AMBackdrop])
 * the frosted fill, exactly as the titled toolbar does.
 *
 * ```kotlin
 * AMToolbar(
 *     actions = { AMToolbarButton(painterResource(R.drawable.ic_dark_mode), "Switch to dark mode", onClick = ::flip) },
 * )
 * ```
 *
 * The bar keeps Material's height whatever it holds, so content under it sits where it would under
 * a titled one, and TalkBack reads only its buttons. Fill its slots with [AMToolbarButton]s; on a
 * picture they turn solid.
 *
 * @param modifier the toolbar's placement.
 * @param navigationIcon the button at the start, such as back or a menu. Null shows none.
 * @param actions the buttons at the end, ⋮ last. Null shows none.
 * @param scrollBehavior tells the toolbar when content has scrolled under it, so it takes the
 *   page color (the frosted fill over a picture). Without one it stays transparent.
 * @param windowInsets the insets the toolbar keeps clear of: the status bar by default. Pass
 *   `WindowInsets(0)` for a toolbar that doesn't reach the top of the screen.
 */
@Composable
public fun AMToolbar(
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    AMToolbar(
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        windowInsets = windowInsets,
        title = {},
    )
}

/**
 * A toolbar's button: Material's icon button, with a long-press tooltip naming it and a 48 dp
 * touch target. On a picture (inside an [AMBackdrop]) it's solid, a crucial control: a circle the
 * size of Material's icon button container wearing the floating surface, its shadow and its
 * dark-mode hairline, the glyph in `onSurface`, so it stays the clearest thing over any art.
 * Everywhere else it's a bare glyph in the bar's content color.
 *
 * ```kotlin
 * AMToolbarButton(painterResource(R.drawable.ic_arrow_back), "Back", onClick = ::pop)
 * AMToolbarButton(painterResource(R.drawable.ic_delete), "Delete photo", onClick = ::delete, destructive = true)
 * ```
 *
 * For a glyph inside a card's action row use [AMCardButton]; the card supplies the surface.
 *
 * @param icon the glyph.
 * @param contentDescription what a screen reader says for the button, and what its tooltip shows.
 * @param onClick called when the button is tapped.
 * @param modifier the button's placement.
 * @param destructive colors the glyph in [AMSemantic.Destructive]'s color, for a removal.
 * @param enabled whether the button takes taps. Disabled (here, or by a disabling container
 *   around it), the glyph greys to Material's disabled color, and a solid button keeps its fill
 *   but casts no shadow.
 */
@Composable
public fun AMToolbarButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    ToolbarButton(icon, contentDescription, onClick, solid = LocalAMBackdrop.current != null, modifier, destructive, enabled)
}

/**
 * [AMToolbarButton] in the form the caller picks: [solid] for the crucial-control form, plain for
 * the bare glyph. The public button is solid exactly inside an [AMBackdrop]; a bar that is always
 * over art (the art panel's back) asks for solid everywhere.
 */
@Composable
internal fun ToolbarButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    solid: Boolean,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    val isEnabled = resolveEnabled(enabled)
    val destructiveColor = if (destructive && isEnabled) AMSemantic.Destructive.color else Color.Unspecified
    if (!solid) {
        val color = destructiveColor.takeOrElse { LocalContentColor.current }
        CompositionLocalProvider(LocalContentColor provides color) {
            AMIconButton(icon, contentDescription, onClick, modifier, enabled = isEnabled)
        }
    } else {
        // The 48 dp target is reserved outside the circle, so the surface hugs Material's 40 dp
        // button inside it and taps still land across the whole target.
        Box(
            modifier
                .minimumInteractiveComponentSize()
                .amElevatedSurface(CircleShape, if (isEnabled) AMElevation.Low else AMElevation.None),
            propagateMinConstraints = true,
        ) {
            CompositionLocalProvider(
                LocalMinimumInteractiveComponentSize provides 0.dp,
                LocalContentColor provides destructiveColor.takeOrElse { MaterialTheme.colorScheme.onSurface },
            ) {
                AMIconButton(icon, contentDescription, onClick, enabled = isEnabled)
            }
        }
    }
}
