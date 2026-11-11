package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * Switches between an app's root views, the ones its remote reaches, and keeps each one's place:
 * the view you leave is saved, its scroll position and every `rememberSaveable` value included,
 * and restored when you come back, instead of being kept running. Only [current] is composed.
 *
 * ```kotlin
 * enum class View { Feed, Alerts }
 *
 * var view by rememberSaveable { mutableStateOf(View.Feed) }
 * val feed = rememberSaveable(saver = FeedStackSaver) { mutableStateListOf<FeedKey>(FeedKey.Home) }
 *
 * AMKeepsPlace(
 *     current = view,
 *     landing = View.Feed,
 *     onCurrentChange = { view = it },
 *     backStack = { root -> if (root == View.Feed) feed else emptyList() },
 * ) { root ->
 *     when (root) {
 *         View.Feed -> NavDisplay(feed, onBack = { feed.removeLastOrNull() }) { … }
 *         View.Alerts -> AlertsView()
 *     }
 * }
 * ```
 *
 * - **Switching is instant.** Change [current] (a remote's view tile does, inside
 *   [amInstantSwitch]) and the new root appears in the same frame, restored where you left it.
 * - **Each root's back stack is the app's own state**, Navigation 3's model: keep it above this
 *   container, so it outlives the switch, and drive the root's `NavDisplay` with it. The container
 *   only reads it, through [backStack], to know whether the root is showing a panel.
 * - **Back to the landing view.** Back goes first to whatever is on top: overlays (they're
 *   windows), then the current root's panels (its `NavDisplay` pops them). On a root other than
 *   [landing] with no panel left, back calls [onCurrentChange] with [landing], an instant swap.
 *   On the landing root back falls through, so the app closes.
 *
 * [current] and [landing] name the roots, and each root's saved state is stored under its name,
 * so a root must be a value a `Bundle` can hold: an enum entry, a `String`, an `Int`. Keep
 * [current] itself in `rememberSaveable`, so the app comes back on the same root after a
 * configuration change or process death.
 *
 * @param current the root view showing.
 * @param landing the root the app opens on, which back returns to before closing the app.
 * @param onCurrentChange called with [landing] when back swaps to it. Apply it to [current].
 * @param modifier the container's size and placement. The root fills it.
 * @param backStack a root's Navigation 3 back stack, its own view's entry first: the root shows a
 *   panel while it holds more than one entry. A root without panels returns an empty list, the
 *   default.
 * @param content the root view for a root name.
 */
@Composable
public fun <T : Any> AMKeepsPlace(
    current: T,
    landing: T,
    onCurrentChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    backStack: (root: T) -> List<*> = { emptyList<Any>() },
    content: @Composable (root: T) -> Unit,
) {
    val holder = rememberSaveableStateHolder()
    KeepsPlaceBackHandler(current, landing, backStack(current).size, onCurrentChange)
    Box(modifier, propagateMinConstraints = true) {
        holder.SaveableStateProvider(current) { content(current) }
    }
}

/**
 * The landing step of the back order: while [current] is a root other than [landing] showing no
 * panel ([depth], its back stack's size, at most 1), back swaps to [landing] instantly; otherwise
 * this handler stands aside, so a root's panels pop first and back on the landing root closes the
 * app. A scene root that owns the switch itself can host this directly.
 */
@Composable
internal fun <T : Any> KeepsPlaceBackHandler(current: T, landing: T, depth: Int, onCurrentChange: (T) -> Unit) {
    val target = keepsPlaceBackTarget(current, landing, depth)
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = target != null,
        onBackCompleted = { if (target != null) amInstantSwitch { onCurrentChange(target) } },
    )
}

/**
 * Where back goes from root [current] once its panels are gone: [landing] when [current] is
 * another root whose back stack holds at most its own entry ([depth] ≤ 1), or null when back
 * isn't this container's (a panel to pop, or the landing root itself, where back leaves the app).
 */
internal fun <T : Any> keepsPlaceBackTarget(current: T, landing: T, depth: Int): T? =
    if (current != landing && depth <= 1) landing else null
