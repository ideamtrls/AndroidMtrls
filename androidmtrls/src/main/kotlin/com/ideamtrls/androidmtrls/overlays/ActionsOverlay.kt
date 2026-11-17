package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The remote's window: everything you can do here, over the blurred screen. Fill it with an
 * [AMActionGrid], whose rows of tiles sit at the bottom where a thumb reaches them.
 *
 * ```kotlin
 * var showsActions by rememberSaveable { mutableStateOf(false) }
 * AMButton("Show actions", onClick = { showsActions = true })
 * AMActionsOverlay(showsActions, onDismissRequest = { showsActions = false }) {
 *     AMActionGrid {
 *         AMActionGridRow(
 *             { AMActionGridButton(painterResource(R.drawable.ic_share), "Share", onClick = { share(); showsActions = false }) },
 *             { AMActionGridButton(painterResource(R.drawable.ic_delete), "Delete", onClick = { delete(); showsActions = false }, destructive = true) },
 *         )
 *     }
 * }
 * ```
 *
 * It's a window of its own, so it covers the whole screen from wherever it's called, sheets
 * included, and takes back before anything under it. The screen behind blurs with the platform's
 * blur-behind ([AMBlur.Mist]), bare in light mode and under a black scrim in dark mode;
 * where the system has cross-window blur off (battery saver, some devices) the wash alone stills
 * it, following that setting live. Blur, scrim and content fade in and out together with Material's
 * motion.
 *
 * Back, and a tap on the blur (between tiles too), call [onDismissRequest]; set your state to hide
 * it. A tile that acts should hide it too. Opening and closing it changes nothing underneath.
 *
 * Its content is always enabled, whatever the screen behind it sets, and always solid, whatever
 * the app's surface setting: the tiles, and any card or button the content holds, wear the opaque
 * floating surface, so the remote's buttons never show the blurred screen through them. Screen
 * readers stay inside it while it's up. While [visible] it reports to the nearest scene, so the
 * floating action button fades out of its way.
 *
 * @param visible whether the overlay is up.
 * @param onDismissRequest called on back and on a tap outside the content.
 * @param modifier the content's placement: the whole window, the content anchored to its bottom.
 * @param content what you can do: usually an [AMActionGrid] of [AMActionGridRow]s. The grid keeps
 *   clear of the navigation bar itself; custom content handles its own insets.
 */
@Composable
public fun AMActionsOverlay(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AMOverlayWindow(visible, onDismissRequest, OverlayCover.Scrim) {
        // The remote's buttons are never transparent: solid in either surface setting.
        CompositionLocalProvider(LocalAMSurfaceFill provides AMSurfaceFill.Solid) {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { content() }
        }
    }
}
