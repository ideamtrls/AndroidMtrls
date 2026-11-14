package com.ideamtrls.androidmtrls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/** What a screen reader says for the remote's button unless the app names it. */
private const val ShowActionsLabel = "Show actions"

/**
 * The scene's remote: the floating action button, and the menu it opens over the blurred screen,
 * the app's main navigation. Put it in [AMScene]'s `floatingActions` slot.
 *
 * ```kotlin
 * var showsRemote by rememberSaveable { mutableStateOf(false) }
 * AMFloatingActions(showsRemote, onExpandedChange = { showsRemote = it }) {
 *     AMActionGrid {
 *         AMActionGridRow(
 *             { AMActionGridButton(composeIcon, "Compose", onClick = { showsRemote = false; compose() }) },
 *             { AMActionGridButton(searchIcon, "Search", onClick = { showsRemote = false; search() }) },
 *         )
 *         AMActionGridRow(
 *             {
 *                 AMActionGridButton(inboxIcon, "Inbox", active = view == View.Inbox, onClick = {
 *                     showsRemote = false
 *                     amInstantSwitch { view = View.Inbox }
 *                 })
 *             },
 *             { … "Archive" … },
 *         )
 *     }
 * }
 * ```
 *
 * - **The button** is an [AMFloatingActionButton] over the whole scene, its pushed panels
 *   included: the + at the bottom center, movable by a long press, where it stays ([state]). A tap
 *   calls [onExpandedChange] with true.
 * - **The menu** is [menu] in an [AMActionsOverlay], up while [expanded]: an [AMActionGrid] whose
 *   rows of tiles land where a thumb reaches them. Back and a tap on the blur call
 *   [onExpandedChange] with false.
 * - **Views and actions share it.** A tile that acts closes the menu itself, then acts. A view tile
 *   closes it, then switches the scene's root inside [amInstantSwitch], so the view appears at once
 *   and the menu fades over it; the current view's tile is `active`, ringed. A live control (a
 *   play button, a toggle) may act without closing it.
 * - **Overlays beat the button.** It fades out while its own menu is open and while any other
 *   overlay in the scene is up (a sheet, an alert, the spotlight, the loading overlay), and fades
 *   back when they're gone, with Material's motion (instant under Remove animations).
 *
 * The state is the app's: keep [expanded] and [state] in `rememberSaveable` (the default [state]
 * is), so the remote comes back as it was after a configuration change.
 *
 * @param expanded whether the menu is open.
 * @param onExpandedChange called with true when the button is tapped, and with false on back or a
 *   tap outside the menu. Apply it to [expanded].
 * @param modifier the button's layer, which fills the scene: pad its bottom to rest the button
 *   higher (above a bottom toolbar).
 * @param contentDescription what a screen reader says for the button: "Show actions" by default.
 * @param state where the button has been moved to.
 * @param enabled whether the button takes taps and drags. A disabled scene (one whose content is
 *   still loading) passes false.
 * @param menu the remote: an [AMActionGrid] of [AMActionGridRow]s.
 */
@Composable
public fun AMFloatingActions(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = ShowActionsLabel,
    state: AMFloatingActionButtonState = rememberAMFloatingActionButtonState(),
    enabled: Boolean = true,
    menu: @Composable () -> Unit,
) {
    val overlayUp = LocalAMOverlayRegistry.current?.anyUp == true
    AnimatedVisibility(
        visible = !expanded && !overlayUp,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        label = "floatingActions",
    ) {
        AMFloatingActionButton(
            contentDescription = contentDescription,
            onClick = { onExpandedChange(true) },
            state = state,
            enabled = enabled,
        )
    }
    AMActionsOverlay(expanded, onDismissRequest = { onExpandedChange(false) }, content = menu)
}

/**
 * The insets that keep the end of scrolling content clear of the scene's floating action button:
 * the navigation bar, then the room the button takes over it at rest (the `spacing.lg` it rests
 * above the bar and its 56 dp) and the same `spacing.lg` again, so the last item scrolls clear of
 * the button: 88 dp over the bar by default. Bottom only.
 *
 * ```kotlin
 * LazyColumn(contentPadding = WindowInsets.amFloatingActions.asPaddingValues()) { … }
 * ```
 *
 * Content that already pads the navigation bar, such as a scaffold's content, consumes that
 * padding first, so the bar counts once:
 *
 * ```kotlin
 * Column(
 *     Modifier.verticalScroll(rememberScrollState())
 *         .padding(padding)
 *         .consumeWindowInsets(padding)
 *         .windowInsetsPadding(WindowInsets.amFloatingActions),
 * ) { … }
 * ```
 */
public val WindowInsets.Companion.amFloatingActions: WindowInsets
    @Composable get() = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
        .add(WindowInsets(bottom = floatingActionButtonTop() + AMTheme.tokens.spacing.lg))

/**
 * How far above the navigation bar the scene's floating action button reaches at rest: the gap it
 * rests above the bar (`spacing.lg`) and its 56 dp. A scene's toasts show just over it; scrolling
 * content clears it by one more gap (`WindowInsets.amFloatingActions`).
 */
@Composable
@ReadOnlyComposable
internal fun floatingActionButtonTop(): Dp = FloatingActionButtonDiameter + AMTheme.tokens.spacing.lg
