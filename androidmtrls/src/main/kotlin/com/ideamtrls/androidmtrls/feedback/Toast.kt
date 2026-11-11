package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * One toast: a short, neutral confirmation ("Saved", "Link copied", "Added to queue").
 *
 * A toast has no status role and no action; status news with a role is an [AMBannerItem]. Items
 * compare by identity, so posting a new item, even one with the same text, shows it afresh and
 * starts its time again.
 *
 * ```kotlin
 * AMToastItem("Link copied")
 * ```
 *
 * @property text the confirmation, in sentence case.
 * @property duration how long the toast stays up once it shows: 2 seconds by default, longer when
 *   the user has asked Android for more time to act (Accessibility, Time to take action).
 */
@Immutable
public class AMToastItem(
    public val text: String,
    public val duration: Duration = 2.seconds,
)

/**
 * Shows [item] as a toast at the bottom of the window while it's non-null: Material's snackbar,
 * with its own look and motion and no action, above the navigation bar and the keyboard.
 *
 * It draws nothing where it's called: the nearest [AMToastHost] shows it. Each window has its own
 * host (a screen, a sheet), so a toast posted from inside a sheet shows in the sheet, over it.
 * Without a host above it, it fails at once.
 *
 * ```kotlin
 * var toast by remember { mutableStateOf<AMToastItem?>(null) }
 * AMButton("Copy link", onClick = { copy(); toast = AMToastItem("Link copied") })
 * AMToast(toast, onDismissRequest = { toast = null })
 * ```
 *
 * The state is the caller's. The toast asks to go through [onDismissRequest] when its time is up
 * (the item's `duration`, stretched to the user's accessibility timeout) or on a screen reader's
 * dismiss action; set the item back to null there. A screen reader hears it once, politely.
 *
 * A host shows one toast at a time: the most recently posted one.
 *
 * @param item the toast to show, or null for none.
 * @param onDismissRequest called when the toast asks to go.
 */
@Composable
public fun AMToast(
    item: AMToastItem?,
    onDismissRequest: () -> Unit,
) {
    PostTransient(LocalAMToastHost.current, item, onDismissRequest)
}

/**
 * A window's toast layer: draws [content], and over its bottom edge the toast any [AMToast] inside
 * it posts, above the navigation bar and the keyboard (Material's snackbar host).
 *
 * Put one at the root of each window that shows toasts (a screen, a sheet), filling it, so its
 * toasts show at the bottom of that window. The content is laid out as the host itself is; the
 * toast takes no space from it and lets touches outside it through. [AMScene] and [AMSheet] carry
 * one already.
 *
 * ```kotlin
 * AMToastHost(Modifier.fillMaxSize()) { Screen() }
 * ```
 *
 * @param modifier the host's size and placement: usually the whole window.
 * @param content the window's content.
 */
@Composable
public fun AMToastHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ToastHost(modifier, bottomClearance = 0.dp, content)
}

/**
 * [AMToastHost] with its toasts raised [bottomClearance] above the navigation bar: a scene with a
 * floating action button shows its toasts above the button's resting place, as Material's scaffold
 * does.
 */
@Composable
internal fun ToastHost(
    modifier: Modifier,
    bottomClearance: Dp,
    content: @Composable () -> Unit,
) {
    val host = remember { TransientHost<AMToastItem>() }
    val snackbars = remember { SnackbarHostState() }
    val posting = host.current
    val accessibility = LocalAccessibilityManager.current

    // A newer posting, or none, cancels this one's snackbar, which takes it down. The snackbar
    // comes down on its own when its time is up, or when a screen reader dismisses it; either way
    // the toast then asks its caller to go.
    LaunchedEffect(posting) {
        val shown = posting ?: return@LaunchedEffect
        val timeout = accessibility.transientTimeout(shown.item.duration, containsIcons = false, containsControls = false)
        withTimeoutOrNull(timeout) { snackbars.showSnackbar(ToastVisuals(shown.item.text)) }
        shown.caller.onDismissRequest()
    }

    Box(modifier, propagateMinConstraints = true) {
        CompositionLocalProvider(LocalAMToastHost provides host, content = content)
        Box(
            Modifier
                .matchParentSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(bottom = bottomClearance),
            contentAlignment = Alignment.BottomCenter,
        ) {
            SnackbarHost(snackbars)
        }
    }
}

/** The nearest [AMToastHost]. Reading it with no host above fails. */
internal val LocalAMToastHost = staticCompositionLocalOf<TransientHost<AMToastItem>> {
    error("AMToast shows in the nearest AMToastHost: put one at the root of the window")
}

/** A toast as Material's snackbar sees it: the text alone, no action, timed by the toast host. */
private class ToastVisuals(override val message: String) : SnackbarVisuals {
    override val actionLabel: String? get() = null
    override val withDismissAction: Boolean get() = false
    override val duration: SnackbarDuration get() = SnackbarDuration.Indefinite
}
