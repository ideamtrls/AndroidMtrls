package io.github.jmsdnns.androidmtrls.surfaces.overlays

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMSpinner
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amSquircle
import io.github.jmsdnns.androidmtrls.tokens.resolved
import io.github.jmsdnns.androidmtrls.tokens.resolvedFinite

// ---------------------------------------------------------------------------
// Dp animation spec mirror of AMTheme.Motion.overlayFade
//
// Motion.overlayFade is declared as AnimationSpec<Float>; animateDpAsState
// needs AnimationSpec<Dp>. Mirror the token as a file-level val with a
// lockstep comment — house convention (precedent: ActionsOverlay.kt
// overlayFadeDp, PageIndicator.kt / Tabs.kt snapSpringDp).
// ---------------------------------------------------------------------------

/**
 * Mirror of [AMTheme.Motion.overlayFade] typed as `AnimationSpec<Dp>`.
 *
 * Used to animate the blur radius 0dp → [AMTheme.Overlay.blurRadius] on
 * present and in reverse on dismiss, in lockstep with the [AnimatedVisibility]
 * chrome fade so the blur ramp and the scrim/spinner appear together.
 */
private val overlayFadeDp = tween<Dp>(durationMillis = 180, easing = EaseInOut)

// ---------------------------------------------------------------------------
// AMLoadingOverlay — blocking full-screen loading overlay host
// ---------------------------------------------------------------------------

/**
 * Full-screen blocking loading overlay that wraps scene content.
 *
 * Presents a backdrop-blurred, scrimmed spinner panel over [hostContent]
 * while [isPresented] is `true`. The overlay is non-dismissible by the user:
 * the caller controls [isPresented] — flip it back to `false` when the
 * async work completes.
 *
 * **Attach at the scene root** so the blur and scrim cover the whole screen.
 * Wrapping a sub-composable clips the overlay to that node's measured size
 * — same constraint as iOS.
 *
 * **No dismiss gesture by design.** The loading overlay is mandatory while
 * it is up — there is no tap-to-dismiss, swipe-to-dismiss, or back-press
 * handler. The caller must flip [isPresented] to `false` when the work is done.
 *
 * **Card-framed spinner.** The centered content is a non-compact [AMSpinner],
 * which frames the arc and the [message] caption in an [AMCard] with
 * `caption / secondary` label styling — matching iOS's `.imSpinner`
 * progress-view style. `null` [message] falls back to `"Loading"`.
 *
 * **Modal a11y.** While presented the blurred [hostContent] subtree is
 * `invisibleToUser()` and the overlay chrome is an `isTraversalGroup`, so
 * TalkBack skips the paused world behind the blur and focus stays on the
 * spinner — the Compose translation of iOS's `.accessibilityAddTraits(.isModal)`.
 *
 * **Paused-world chrome is token-locked.** The backdrop blur radius comes from
 * [AMTheme.Overlay.blurRadius] and the dark scrim from
 * [AMTheme.Overlay.scrimOpacity] — there is no public knob to tune either, so
 * the "paused world" reads identically everywhere. (iOS locked these too.)
 *
 * **Blur note:** `Modifier.amBackdropBlur` uses [BlurEffect] via
 * `graphicsLayer { renderEffect = … }`. Android Studio's interactive preview
 * renderer does not execute `RenderEffect` chains — the blur renders as a
 * no-op in preview. The scrim (dark mode) and spinner still show. Validate
 * blur on a physical device or emulator (API 31+).
 *
 * **Animation:** the backdrop blur radius animates 0dp → [AMTheme.Overlay.blurRadius]
 * on present and back on dismiss, using [overlayFadeDp] (a
 * `tween<Dp>(180, EaseInOut)` mirror of [AMTheme.Motion.overlayFade]).
 * The overlay chrome (scrim + spinner) fades via `AnimatedVisibility` driven by
 * the same motion token, keeping the blur ramp and the chrome fade exactly in
 * lockstep. On dismiss the chrome fades out while the blur radius simultaneously
 * ramps back to 0dp — there is no pop. Both the blur ramp and the fade route
 * through [io.github.jmsdnns.androidmtrls.tokens.resolved] with
 * [LocalAMReduceMotion], so under reduce motion they stay the gentle 180ms fade.
 *
 * iOS equivalent: `View.imLoadingOverlay(isPresented:message:)` in
 * `Sources/Surfaces/Overlays/LoadingOverlay.swift`.
 *
 * @param isPresented  When `true`, the overlay is visible and input is blocked.
 * @param message      Optional caption shown below the spinner, in
 *                     [AMSpinner]'s `caption / secondary` label style. `null`
 *                     shows the default `"Loading"` label. Defaults to null.
 * @param modifier     Modifier applied to the outer [Box] root. Use this to
 *                     constrain the overlay host to a specific area.
 * @param hostContent  The scene content to wrap (and blur). The trailing lambda.
 */
@Composable
fun AMLoadingOverlay(
    isPresented: Boolean,
    message: String? = null,
    modifier: Modifier = Modifier,
    hostContent: @Composable () -> Unit,
) {
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current

    // Inherited overlay-active signal, read BEFORE providing it below so a nested
    // host ORs its own state with every ancestor's (see LocalAMOverlayActive).
    val inheritedOverlayActive = LocalAMOverlayActive.current

    // Animate the blur radius: 0dp when dismissed, the token radius when
    // presented (paused-world chrome is token-locked — no public knob).
    // overlayFadeDp mirrors AMTheme.Motion.overlayFade typed as AnimationSpec<Dp>;
    // routing it through resolved() collapses to the gentle fade under reduce motion.
    val animatedRadius: Dp by animateDpAsState(
        targetValue   = if (isPresented) AMTheme.Overlay.blurRadius.dp else 0.dp,
        animationSpec = tokens.motion.resolved(overlayFadeDp, reduceMotion),
        label         = "AMLoadingOverlay.blurRadius",
    )

    Box(modifier = modifier) {
        // ── Content layer — blur applied here (AMENDED ruling 2) ─────────────
        // amBackdropBlur is applied to the hostContent node, not a sibling.
        // The animated radius ramps 0→blurRadius on present and back on dismiss.
        // While presented the whole subtree is hidden from TalkBack so focus stays
        // in the overlay chrome — the Compose translation of iOS `.isModal`.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .amBackdropBlur(animatedRadius)
                .then(
                    if (isPresented) Modifier.semantics { invisibleToUser() } else Modifier,
                ),
        ) {
            // Reports "an overlay is up" (this loading overlay OR any ancestor host)
            // down to hostContent so a wrapped FAB hub fades out of the way.
            CompositionLocalProvider(
                LocalAMOverlayActive provides (isPresented || inheritedOverlayActive),
            ) {
                hostContent()
            }
        }

        // ── Overlay chrome — scrim + input blocker + spinner card ────────────
        // AnimatedVisibility drives the fade using AMTheme.Motion.overlayFade
        // routed through resolved() (same token as the Dp blur ramp above),
        // keeping chrome fade and blur ramp in lockstep and reduce-motion aware.
        // isTraversalGroup keeps TalkBack focus inside the modal chrome.
        AnimatedVisibility(
            visible = isPresented,
            enter   = fadeIn(animationSpec = tokens.motion.resolvedFinite(AMTheme.Motion.overlayFade, reduceMotion)),
            exit    = fadeOut(animationSpec = tokens.motion.resolvedFinite(AMTheme.Motion.overlayFade, reduceMotion)),
        ) {
            Box(modifier = Modifier.semantics { isTraversalGroup = true }) {
                // Dark scrim (dark mode only — AMDarkScrim reads LocalAMIsDarkTheme
                // per AMENDED ruling 3).
                AMDarkScrim()

                // Full-size input blocker. No dismiss — tap gestures are consumed
                // silently (the loading overlay is mandatory while up).
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { /* consume silently — no dismiss */ }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    // ── Spinner card ─────────────────────────────────────────
                    // The non-compact AMSpinner frames the arc + caption in an
                    // AMCard with the caption/secondary label styling — matching
                    // iOS's `.progressViewStyle(.imSpinner)` card. The message rides
                    // AMSpinner's own label slot; null falls back to "Loading".
                    AMSpinner(label = message ?: "Loading")
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews (light + dark — required by conventions)
//
// Note: Android Studio's preview renderer does not execute RenderEffect
// chains; Modifier.amBackdropBlur renders as a no-op in preview. The dark
// scrim (dark mode) and spinner still render correctly. Validate blur on a
// physical device or emulator (API 31+).
// ---------------------------------------------------------------------------

@Composable
private fun LoadingOverlayPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    // Scene content — placeholder rows so the blurred backdrop is visible
    // when rendered on device.
    val sceneContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AMTheme.Surface.page.resolve(isDark))
                .padding(AMTheme.Spacing.lg.dp),
        ) {
            repeat(5) { i ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = AMTheme.Spacing.sm.dp)
                        .background(
                            color = AMTheme.Surface.primary.resolve(isDark),
                            shape = amSquircle(AMTheme.Radius.lg.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text  = "Placeholder row ${i + 1}",
                        style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                    )
                }
            }
        }
    }

    AMLoadingOverlay(
        isPresented = true,
        message     = "Saving…",
    ) {
        sceneContent()
    }
}

@Preview(
    name = "AMLoadingOverlay · Light",
    group = "AMLoadingOverlay",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    widthDp = 360,
    heightDp = 480,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMLoadingOverlayPreviewLight() {
    AMThemeRoot {
        LoadingOverlayPreviewContent()
    }
}

@Preview(
    name = "AMLoadingOverlay · Dark",
    group = "AMLoadingOverlay",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    widthDp = 360,
    heightDp = 480,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMLoadingOverlayPreviewDark() {
    AMThemeRoot {
        LoadingOverlayPreviewContent()
    }
}
