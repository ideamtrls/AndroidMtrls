package com.ideamtrls.androidmtrls

import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import java.util.function.Consumer
import kotlin.math.roundToInt

// The paused-world window the remote and the spotlight present in: a full-screen dialog window
// that blurs everything behind it with the platform's blur-behind (mist), sheets included, under
// its cover: the mode's wash for the spotlight, the bare blur (a black scrim in dark mode) for the
// remote. Where the system has cross-window blur off (battery saver, some devices), the wash alone
// stills the screen.

/** What an overlay window lays over the blurred screen behind it ([AMOverlayWindow]). */
internal enum class OverlayCover {
    /**
     * The mode's wash ([AMTheme.Overlay.wash]), white in light mode and black in dark mode, so a
     * title set straight on the blur holds over any screen: the spotlight's.
     */
    Wash,

    /**
     * The bare blur in light mode and a black scrim in dark mode ([Style.ModalOverlay.ScrimDark]),
     * for content that brings its own surfaces: the remote's solid tiles. While the system has
     * cross-window blur off there's no blur to lie bare, so the mode's wash stills the screen.
     */
    Scrim,
}

/** What a screen reader says for the blurred area around the content, which closes the window. */
internal const val OverlayCloseLabel: String = "Close"

/**
 * The one modal window behind [AMActionsOverlay] and [AMSpotlight]: on screen while [visible], and
 * a moment longer while it fades away.
 *
 * - **The window**: a full-screen dialog window, edge to edge, so it sits above the app and above
 *   any sheet, and takes back first. It blurs what's behind it ([AMBlur.Mist]) and lays
 *   its [cover] over the blur: the mode's wash ([AMTheme.Overlay.wash]) so a title on it holds
 *   over any screen (the spotlight), or the bare blur with a black scrim in dark mode (the remote).
 *   While the system has cross-window blur off the wash alone stills the screen, switching live
 *   when the setting changes.
 * - **Motion**: the blur, the cover and the content fade in together with Material's default effects
 *   spec and out with its fast effects spec. Input is ignored while it fades out.
 * - **Dismissal**: back, and a tap anywhere the content doesn't take, call [onDismissRequest].
 *   The area around the content is one "Close" button for TalkBack, after the content.
 * - **A fresh context**: the content is elevated and enabled whatever surrounds the call: no art to
 *   sample, [AMSurfaceStyle.Elevated], enabled controls, `onSurface` content. The app's surface
 *   setting carries in.
 * - **Frost over its own blur**: the window's blur-behind is the blur, so in a frosted app a
 *   frosted surface in it (a tile, the spotlight's card) draws the frosted fill, edge and shadow
 *   straight over the blurred, washed screen and samples nothing; while the system has
 *   cross-window blur off it draws solid ([LocalAMWindowBlur]). For a moment after cross-window
 *   blur comes back the platform's blur lags the flag, and a faint ghost of the sharp screen shows
 *   through the frost while text on it still holds.
 * - It reports to the overlay registry while [visible].
 */
@Composable
internal fun AMOverlayWindow(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    cover: OverlayCover,
    content: @Composable BoxScope.() -> Unit,
) {
    ReportOverlayUp(visible)
    val progress = remember { Animatable(0f) }
    var showing by remember { mutableStateOf(false) }
    val enter = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val exit = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

    LaunchedEffect(visible) {
        if (visible) {
            showing = true
            progress.animateTo(1f, enter)
        } else if (showing) {
            progress.animateTo(0f, exit)
            showing = false
        }
    }
    if (!showing) return

    val dismiss by rememberUpdatedState(onDismissRequest)
    Dialog(
        onDismissRequest = { if (visible) dismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val blurEnabled by rememberCrossWindowBlurEnabled()
        BlurBehind(progress, blurEnabled)
        val coverColor = when {
            cover == OverlayCover.Wash || !blurEnabled -> AMTheme.Overlay.wash
            AMTheme.isDark -> Style.ModalOverlay.ScrimDark
            else -> Color.Transparent
        }
        ProvideOverlayWindowLocals(windowBlur = blurEnabled) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress.value }.background(coverColor)) {
                DismissCatcher(onDismiss = { if (visible) dismiss() })
                content()
                // Fading out, the window takes no more input.
                if (!visible) InputBlocker()
            }
        }
    }
}

/**
 * The fresh context an overlay window's content starts from: no art to sample (a window can't
 * sample the screen behind it), no art under it and no scene root its art could stand for, cards
 * elevated and controls enabled whatever the presenting screen set, content lying on the window's
 * own page rather than on any surface around the call (a sheet sets its own inside), and
 * `onSurface` content. The app's surface setting carries in.
 *
 * [windowBlur] says what a frosted surface in it lies over ([LocalAMWindowBlur]): the blurred
 * window passes whether its blur-behind is on, so the surface frosts over that blur, or draws solid
 * while it's off; any other window passes null, and a frosted surface in it is frost over the page
 * color.
 *
 * @param windowBlur the blurred window's blur-behind: true while the system allows cross-window
 *   blur, false while it doesn't, null for a window with no blur-behind (a sheet, a banner's
 *   window, an alert, the loading overlay's dialog).
 */
@Composable
internal fun ProvideOverlayWindowLocals(windowBlur: Boolean? = null, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAMBackdrop provides null,
        LocalAMSceneBackdrops provides null,
        LocalAMArtTone provides null,
        LocalAMEnabled provides true,
        LocalAMLiesOn provides LiesOn.Page,
        LocalAMWindowBlur provides windowBlur,
        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
    ) {
        ProvideAMSurfaceStyle(AMSurfaceStyle.Elevated, content)
    }
}

/**
 * The window's blur-behind, following [progress] (0 hidden, 1 shown) and whether the system allows
 * cross-window blur ([blurEnabled]), and its bar icons for the appearance. The cover over it (the
 * wash or the remote's scrim) is the window's content.
 */
@Composable
private fun BlurBehind(progress: Animatable<Float, AnimationVector1D>, blurEnabled: Boolean) {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: return
    val dark = AMTheme.isDark
    val blurPx = with(LocalDensity.current) { AMBlur.Mist.radius.roundToPx() }

    DisposableEffect(window, view, dark) {
        // The paused screen reads light in light mode and dark in dark mode, under either cover.
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        onDispose {}
    }

    LaunchedEffect(window, blurEnabled, blurPx) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        if (!blurEnabled) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            return@LaunchedEffect
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        // The blur radius isn't animated by the window itself, so it follows the fade here, the
        // platform's documented way to fade a blur in and out.
        snapshotFlow { progress.value }.collect { shown ->
            val radius = (blurPx * shown).roundToInt()
            if (window.attributes.blurBehindRadius != radius) {
                window.attributes = window.attributes.apply { blurBehindRadius = radius }
            }
        }
    }
}

/** Whether the system allows cross-window blur right now, following changes (battery saver). */
@Composable
private fun rememberCrossWindowBlurEnabled(): State<Boolean> {
    val context = LocalContext.current
    val windowManager = remember(context) { context.getSystemService(WindowManager::class.java) }
    val enabled = remember(windowManager) { mutableStateOf(windowManager.isCrossWindowBlurEnabled) }
    DisposableEffect(windowManager) {
        val listener = Consumer<Boolean> { enabled.value = it }
        windowManager.addCrossWindowBlurEnabledListener(context.mainExecutor, listener)
        onDispose { windowManager.removeCrossWindowBlurEnabledListener(listener) }
    }
    return enabled
}

/**
 * The blurred area: a tap anywhere the content above doesn't take (between tiles included) calls
 * [onDismiss]. For TalkBack it's one "Close" button, read after the content.
 */
@Composable
private fun DismissCatcher(onDismiss: () -> Unit) {
    val dismiss by rememberUpdatedState(onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { dismiss() } }
            .semantics {
                contentDescription = OverlayCloseLabel
                traversalIndex = 1f
                onClick {
                    dismiss()
                    true
                }
            },
    )
}

/** Swallows every touch, so a window fading out doesn't act on taps. */
@Composable
private fun InputBlocker() {
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            },
    )
}
