package com.ideamtrls.androidmtrls

import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.gestures.detectTapGestures
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
// that blurs everything behind it with the platform's blur-behind, sheets included, with a black
// dim over the blur in dark mode only. Where the system has cross-window blur off (battery saver,
// some devices), it dims instead, the platform's own fallback.

/** What a screen reader says for the blurred area around the content, which closes the window. */
internal const val OverlayCloseLabel: String = "Close"

/**
 * The one modal window behind [AMActionsOverlay] and [AMSpotlight]: on screen while [visible], and
 * a moment longer while it fades away.
 *
 * - **The window**: a full-screen dialog window, edge to edge, so it sits above the app and above
 *   any sheet, and takes back first. It blurs what's behind it ([AMTheme.Overlay.blurRadius]) with
 *   [AMTheme.Overlay.darkDimAlpha] of black over the blur in dark mode only; while the system has
 *   cross-window blur off it dims instead, switching live when the setting changes.
 * - **Motion**: the blur, the dim and the content fade in together with Material's default effects
 *   spec and out with its fast effects spec. Input is ignored while it fades out.
 * - **Dismissal**: back, and a tap anywhere the content doesn't take, call [onDismissRequest].
 *   The area around the content is one "Close" button for TalkBack, after the content.
 * - **A fresh context**: the content is solid and enabled whatever surrounds the call: no backdrop
 *   to frost over, [AMSurfaceStyle.Elevated], enabled controls, `onSurface` content.
 * - It reports to the overlay registry while [visible].
 */
@Composable
internal fun AMOverlayWindow(
    visible: Boolean,
    onDismissRequest: () -> Unit,
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
        BlurBehind(progress)
        ProvideOverlayWindowLocals {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress.value }) {
                DismissCatcher(onDismiss = { if (visible) dismiss() })
                content()
                // Fading out, the window takes no more input.
                if (!visible) InputBlocker()
            }
        }
    }
}

/**
 * The fresh context an overlay window's content starts from: nothing to frost over (a window can't
 * sample the screen behind it), cards elevated and controls enabled whatever the presenting screen
 * set, no sheet's surface under the controls (a sheet sets its own inside), and `onSurface`
 * content.
 */
@Composable
internal fun ProvideOverlayWindowLocals(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAMBackdrop provides null,
        LocalAMEnabled provides true,
        LocalAMOnPrimarySurface provides false,
        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
    ) {
        ProvideAMSurfaceStyle(AMSurfaceStyle.Elevated, content)
    }
}

/**
 * The window's blur-behind and dim, following [progress] (0 hidden, 1 shown) and the system's
 * cross-window blur setting, and its bar icons for the appearance.
 */
@Composable
private fun BlurBehind(progress: Animatable<Float, AnimationVector1D>) {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: return
    val dark = AMTheme.isDark
    val blurEnabled by rememberCrossWindowBlurEnabled()
    val blurPx = with(LocalDensity.current) { Style.ModalOverlay.BlurRadius.roundToPx() }

    DisposableEffect(window, view, dark) {
        // In light mode the blurred page behind is light (or lightly dimmed); in dark mode, dark.
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        onDispose {}
    }

    LaunchedEffect(window, blurEnabled, dark, blurPx) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        val dim = if (blurEnabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            if (dark) Style.ModalOverlay.DarkDimAlpha else 0f
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            if (dark) Style.ModalOverlay.FallbackDimDark else Style.ModalOverlay.FallbackDimLight
        }
        // The blur radius isn't animated by the window itself, so it follows the fade here, the
        // platform's documented way to fade a blur in and out.
        snapshotFlow { progress.value }.collect { shown ->
            window.setDimAmount(dim * shown)
            if (blurEnabled) {
                val radius = (blurPx * shown).roundToInt()
                if (window.attributes.blurBehindRadius != radius) {
                    window.attributes = window.attributes.apply { blurBehindRadius = radius }
                }
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
