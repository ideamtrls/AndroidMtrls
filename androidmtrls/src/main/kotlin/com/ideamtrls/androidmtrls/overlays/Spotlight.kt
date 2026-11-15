package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * One thing brought up close over the blurred screen: a held item over what you can do with it, or
 * a field's picker under the field's name. The trigger varies (a hold through
 * [Modifier.amHoldable][amHoldable], a tap on a picker's value); the presentation is one layout, so
 * everything in focus looks and behaves the same.
 *
 * ```kotlin
 * var picking by rememberSaveable { mutableStateOf(false) }
 * AMSpotlight(picking, onDismissRequest = { picking = false }, title = "Starts") {
 *     DatePicker(dateState, title = null, headline = null, showModeToggle = false)
 * }
 * ```
 *
 * For a held item, the item form ([AMSpotlight] taking `item`) is usually handier.
 *
 * - **Where it sits**: low on the screen, where the remote's tiles sit. [content] lies on one solid
 *   card at the page gutters, an optional [title] above it in the section header's look over art
 *   (the tone's fixed ink, which holds over the window's wash), and an
 *   optional row of tiles ([actions]) below it at the action grid's insets. When the card is taller
 *   than the room above the tiles (large text, a long list), it scrolls.
 * - **Always solid**: the card and the tiles are opaque whatever the screen behind uses, so a
 *   frosted card or a translucent row comes up as the clearest thing on the screen. A card the
 *   content brings nests in the spotlight's and draws nothing. Give [content] its own padding (a row
 *   on the row recipe brings its own); the card hugs the content's width and centers, so a
 *   full-width row fills the gutters and a narrower picker sits in the middle.
 * - **Its shadow**: the card casts the low shadow by default. An app whose cards are flat passes
 *   [AMElevation.None], so the card in focus keeps its fill, corners and dark-mode hairline but
 *   casts nothing, like the cards it came from; the tiles keep theirs, as the remote's do.
 * - **A window**: like the actions overlay, it covers the whole screen from wherever it's called,
 *   sheets included, blurs what's behind (a dim where the system has cross-window blur off), fades
 *   with Material's motion and takes back first.
 * - **Dismissal**: back, and a tap on the blur, call [onDismissRequest]. Nothing underneath
 *   changes unless a tile acts; each tile should hide the spotlight too. A picker that applies its
 *   value as it changes stays up until a tap outside; a menu closes when an option is picked.
 *
 * While [visible] it reports to the nearest scene, so the floating action button fades out of its
 * way.
 *
 * @param visible whether the spotlight is up.
 * @param onDismissRequest called on back and on a tap outside the card and tiles.
 * @param modifier the layout's placement: the whole window.
 * @param title a heading above the card, naming a field ("Starts") or a group (a day). Null shows
 *   none; a held item usually needs none, being the thing just touched.
 * @param actions what to do with it: one [AMActionGridRow] of two to four tiles. Null shows none, as
 *   for a picker.
 * @param elevation the card's shadow: [AMElevation.Low] by default, [AMElevation.None] for a flat
 *   card that keeps its fill and corners.
 * @param content the thing in focus.
 */
@Composable
public fun AMSpotlight(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    actions: (@Composable () -> Unit)? = null,
    elevation: AMElevation = AMElevation.Low,
    content: @Composable () -> Unit,
) {
    AMOverlayWindow(visible, onDismissRequest) {
        SpotlightLayout(modifier, title, actions, elevation, content)
    }
}

/**
 * The spotlight for one item: up while [item] isn't null, showing that item over its own tiles, as
 * for a press and hold.
 *
 * ```kotlin
 * var held by remember { mutableStateOf<Article?>(null) }
 * val share = painterResource(R.drawable.ic_share)
 * val delete = painterResource(R.drawable.ic_delete)
 * ArticleRow(article, Modifier.amHoldable(onHold = { held = article }))
 * AMSpotlight(
 *     held,
 *     onDismissRequest = { held = null },
 *     actions = { article ->
 *         listOf(
 *             AMHoldAction("Share", share) { share(article) },
 *             AMHoldAction("Delete", delete, destructive = true) { delete(article) },
 *         )
 *     },
 * ) { article -> ArticleRow(article) }
 * ```
 *
 * - **Tiles per item.** [actions] gives each item its own: two to four tiles in the remote's
 *   columns, one tile at half the row, or none, an empty list showing no tiles at all, so one
 *   spotlight serves held items with tiles and pickers without. A tile closes the spotlight
 *   ([onDismissRequest]), then acts.
 * - Everything else is the visible form's. While it fades out after [item] turns null, it keeps
 *   showing the last item, so the card leaves intact.
 *
 * @param item the item in focus, or null when nothing is.
 * @param onDismissRequest called on back, on a tap outside the card and tiles, and on a tile; set
 *   [item] to null.
 * @param modifier the layout's placement: the whole window.
 * @param title a heading above the card for the item (a day's name), or null for none (the
 *   default).
 * @param actions what can be done with the item: at most four [AMHoldAction]s, in tile order. An
 *   empty list, the default, shows no tiles.
 * @param elevation the card's shadow: [AMElevation.Low] by default, [AMElevation.None] for a flat
 *   card that keeps its fill and corners.
 * @param content the item up close.
 */
@Composable
public fun <T : Any> AMSpotlight(
    item: T?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: (T) -> String? = { null },
    actions: (item: T) -> List<AMHoldAction> = { emptyList() },
    elevation: AMElevation = AMElevation.Low,
    content: @Composable (item: T) -> Unit,
) {
    val last = remember { LastItem<T>() }
    if (item != null) last.value = item
    val shown = item ?: last.value
    val tiles = shown?.let(actions).orEmpty()
    AMSpotlight(
        visible = item != null,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = shown?.let(title),
        actions = if (tiles.isNotEmpty()) { { HoldActionTiles(tiles, onPicked = onDismissRequest) } } else null,
        elevation = elevation,
    ) {
        if (shown != null) content(shown)
    }
}

/**
 * A spotlight for held items and the actions each one offers, shared by the holds that open it
 * ([Modifier.amHoldable][amHoldable] with this state) and the spotlight that shows them
 * ([AMSpotlight] with this state): the hold opens it on its item, the spotlight shows that item
 * over its tiles, and TalkBack users get the same tiles as actions on each item, from one
 * definition.
 *
 * ```kotlin
 * val share = painterResource(R.drawable.ic_share)
 * val spotlight = rememberAMSpotlightState<Article> { article -> listOf(AMHoldAction("Share", share) { share(article) }) }
 * articles.forEach { ArticleRow(it, Modifier.amHoldable(spotlight, it)) }
 * AMSpotlight(spotlight) { article -> ArticleRow(article) }
 * ```
 *
 * Create it with [rememberAMSpotlightState]. The held item is kept in memory, not saved: like a
 * press and hold, it doesn't outlive the screen.
 */
@Stable
public class AMSpotlightState<T : Any> internal constructor(actions: (item: T) -> List<AMHoldAction>) {
    /** The item up in the spotlight, or null when nothing is held. */
    public var item: T? by mutableStateOf(null)
        private set

    /** What each item offers, in tile order. */
    internal var actions: (item: T) -> List<AMHoldAction> by mutableStateOf(actions)

    /** Brings [item] up in the spotlight. */
    public fun show(item: T) {
        this.item = item
    }

    /** Puts the spotlight away. */
    public fun dismiss() {
        item = null
    }

    /** What [item] offers: its spotlight's tiles, its TalkBack actions. */
    public fun actionsFor(item: T): List<AMHoldAction> = actions(item)
}

/**
 * Remembers an [AMSpotlightState] whose items offer [actions]: each item's spotlight tiles, in
 * order, and the same actions for TalkBack on the item. An empty list for an item shows no tiles.
 * The latest [actions] is used each time, so it can read the screen's current state.
 *
 * @param actions what an item offers, at most four [AMHoldAction]s. Build it from painters read
 *   while composing.
 */
@Composable
public fun <T : Any> rememberAMSpotlightState(actions: (item: T) -> List<AMHoldAction> = { emptyList() }): AMSpotlightState<T> {
    val state = remember { AMSpotlightState(actions) }
    SideEffect { state.actions = actions }
    return state
}

/**
 * The spotlight for [state]'s held item: up while one is held, that item over the tiles it offers,
 * as the item form shows them. A tile, back and a tap outside put it away.
 *
 * @param state the held item and what each item offers.
 * @param modifier the layout's placement: the whole window.
 * @param title a heading above the card for the item, or null for none (the default).
 * @param elevation the card's shadow: [AMElevation.Low] by default, [AMElevation.None] for a flat
 *   card that keeps its fill and corners.
 * @param content the item up close.
 */
@Composable
public fun <T : Any> AMSpotlight(
    state: AMSpotlightState<T>,
    modifier: Modifier = Modifier,
    title: (T) -> String? = { null },
    elevation: AMElevation = AMElevation.Low,
    content: @Composable (item: T) -> Unit,
) {
    AMSpotlight(
        item = state.item,
        onDismissRequest = state::dismiss,
        modifier = modifier,
        title = title,
        actions = state::actionsFor,
        elevation = elevation,
        content = content,
    )
}

/** The item a spotlight last showed, kept so it leaves intact once its item is gone. */
private class LastItem<T : Any> {
    var value: T? = null
}

/**
 * The spotlight's layout: the title, the card and the tiles stacked at the bottom of the window,
 * `spacing.md` apart, clear of the system bars and the keyboard by `spacing.xxl`, at the page
 * gutters. The card, casting [elevation], takes what height is left above the tiles and scrolls
 * inside it.
 */
@Composable
private fun SpotlightLayout(
    modifier: Modifier,
    title: String?,
    actions: (@Composable () -> Unit)?,
    elevation: AMElevation,
    content: @Composable () -> Unit,
) {
    val spacing = AMTheme.tokens.spacing
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = spacing.xxl, bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.md, Alignment.Bottom),
    ) {
        // The title lies on the paused screen under the mode's wash: art in the mode's tone.
        if (title != null) {
            CompositionLocalProvider(LocalAMArtTone provides modeArtTone()) {
                AMSectionHeader(title, Modifier.padding(horizontal = spacing.lg))
            }
        }
        Box(
            Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .padding(horizontal = spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.amCard(elevation = elevation).verticalScroll(rememberScrollState())) { content() }
        }
        if (actions != null) {
            Box(Modifier.fillMaxWidth().padding(horizontal = spacing.lg)) { actions() }
        }
    }
}
