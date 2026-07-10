package io.github.jmsdnns.androidmtrls.surfaces.overlays

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGrid
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridRow
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMButtonRole
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amSquircle
import io.github.jmsdnns.androidmtrls.tokens.resolved
import io.github.jmsdnns.androidmtrls.tokens.resolvedFinite
import kotlin.math.abs

// ---------------------------------------------------------------------------
// Dp animation spec mirror of AMTheme.Motion.overlayFade
//
// Motion.overlayFade is declared as AnimationSpec<Float>; animateDpAsState
// needs AnimationSpec<Dp>. Mirror the token as a file-level val with a
// lockstep comment — house convention (precedent: LoadingOverlay.kt,
// PageIndicator.kt / Tabs.kt snapSpringDp).
// ---------------------------------------------------------------------------

/**
 * Mirror of [AMTheme.Motion.overlayFade] typed as `AnimationSpec<Dp>`.
 *
 * Used to animate the blur radius 0dp → [AMTheme.Overlay.blurRadius] on
 * present and in reverse on dismiss, in lockstep with the single
 * [AnimatedVisibility] group fade so the blur ramp and the scrim/content
 * appear and disappear together.
 */
private val overlayFadeDp = tween<Dp>(durationMillis = 180, easing = EaseInOut)

// ---------------------------------------------------------------------------
// Drag-to-dismiss activation distance (mirrors iOS ModalBackdrop.swift)
//
// iOS recognizes the dismiss drag on the full-screen catcher with a
// `DragGesture(minimumDistance: 24)` — a stray touch must travel 24pt before
// the gesture engages, so it never steals a tap on a tile. The Android catcher
// mirrors this as an activation gate on `detectVerticalDragGestures`.
// ---------------------------------------------------------------------------

/** Drag travel before the dismiss gesture engages — iOS `dragActivationDistance`. */
private val dragActivationDistance = 24.dp

// ---------------------------------------------------------------------------
// AMActionsOverlay — free-floating actions overlay host
// ---------------------------------------------------------------------------

/**
 * Full-screen actions overlay host that wraps scene content — the share-sheet-like
 * "everything I can do here" layer.
 *
 * While [isPresented] is `true` the overlay paints, over [hostContent], the
 * signature "paused world" chrome: a full-screen tint-free backdrop blur, a
 * dark-mode-only scrim, a full-screen tap/drag catcher, and [overlayContent]
 * rendered **bare** — no card, no fill, no hairline, no drag handle. There is no
 * sheet and no slide: [overlayContent] positions itself and its tiles float free,
 * each self-carded over the blur, exactly like iOS. The canonical content is an
 * [AMActionGrid], whose leading weighted spacer bottom-anchors a stack of
 * [AMActionGridButton] rows above the gesture-nav zone.
 *
 * **One-group fade.** The scrim, catcher, and [overlayContent] fade in and out
 * together as a single opacity group on [AMTheme.Motion.overlayFade] (one
 * [AnimatedVisibility]) — mirroring iOS's single `.transition(.opacity)` over the
 * whole modal backdrop. The blur radius ramps 0 ↔ [AMTheme.Overlay.blurRadius] in
 * lockstep on the same timing. Nothing slides and nothing springs.
 *
 * **Attach at the scene root** so the blur and scrim cover the whole screen.
 * Wrapping a sub-composable clips the overlay to that node's measured size —
 * same constraint as iOS.
 *
 * **Dismiss modes** (all active only when [onDismiss] is non-null):
 * - **Tap outside** — the full-screen catcher sits *behind* [overlayContent]; a
 *   tap that misses the (self-carded, clickable) tiles falls through to the
 *   catcher and dismisses. Taps on the empty regions of [overlayContent] pass
 *   through because bare content installs no pointer input there.
 * - **Drag down** — an **end-evaluated** vertical drag on the catcher: once the
 *   gesture travels past [dragActivationDistance] (24dp) and releases with net
 *   downward translation past [AMTheme.Motion.dismissThreshold] (60dp), it
 *   dismisses. **Content never moves during the drag** — the catcher owns the
 *   gesture, not the content (iOS `DragGesture(minimumDistance: 24).onEnded`).
 * - **System back** — [BackHandler] dismisses while presented and dismissable.
 * - **TalkBack** — the catcher exposes the accessibility `dismiss` action (the
 *   Android analogue of iOS's VoiceOver/keyboard `.escape`).
 * - **Non-dismissable** — pass `null` for [onDismiss]: none of the catcher, drag
 *   gesture, back handler, or dismiss action is installed. (The iOS actions
 *   overlay is always dismissable; the `null` path exists for parity with the
 *   mandatory hosts [AMAlert] / [AMLoadingOverlay].)
 *
 * **Modal a11y.** While presented, the blurred [hostContent] subtree is marked
 * `invisibleToUser()` so TalkBack skips everything behind the blur, and the
 * overlay chrome is an `isTraversalGroup` so focus stays inside it — the Compose
 * translation of iOS's `.accessibilityAddTraits(.isModal)`.
 *
 * **Content elevation.** [overlayContent] tiles keep their **default**
 * `AMElevation.Low` — each tile self-cards (fill + hairline + shadow) and floats
 * over the blur. There is no sheet supplying a surface, so nothing forces
 * `AMElevation.None`.
 *
 * **Paused-world chrome is token-locked.** The backdrop blur radius comes from
 * [AMTheme.Overlay.blurRadius] and the dark scrim from
 * [AMTheme.Overlay.scrimOpacity] — there is no public knob to tune either, so the
 * "paused world" reads identically everywhere. (iOS locked these too.)
 *
 * **Reduce motion:** the blur ramp routes through
 * [io.github.jmsdnns.androidmtrls.tokens.resolved] and the group fade through
 * [io.github.jmsdnns.androidmtrls.tokens.resolvedFinite], both with
 * [LocalAMReduceMotion]. The presentation is a straight opacity fade already, so
 * under reduce motion it stays the same gentle 180ms fade — there is no spatial
 * transform to collapse.
 *
 * **Blur note:** `Modifier.amBackdropBlur` uses [android.graphics.RenderEffect]
 * via `graphicsLayer { renderEffect = … }`. Android Studio's interactive preview
 * renderer does not execute `RenderEffect` chains — the blur renders as a no-op
 * in preview. Validate on a physical device or emulator (API 31+).
 *
 * iOS equivalent: `IMActionsOverlay` + `.imActionsOverlay(isPresented:content:)`
 * (over the shared `IMModalBackdrop`) in
 * `Sources/Surfaces/Overlays/ActionsOverlay.swift`.
 *
 * @param isPresented    When `true`, the overlay layer is visible.
 * @param onDismiss      Tap-outside / drag-down / back / TalkBack dismiss. Pass
 *                       `null` to make the overlay non-dismissable by the user.
 * @param modifier       Modifier applied to the outer [Box] root.
 * @param overlayContent Views rendered bare over the blurred backdrop. Typically
 *                       an [AMActionGrid] of [AMActionGridButton] rows at their
 *                       default elevation.
 * @param hostContent    The scene content to wrap (and blur). The trailing lambda.
 */
@Composable
fun AMActionsOverlay(
    isPresented: Boolean,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
    overlayContent: @Composable () -> Unit,
    hostContent: @Composable () -> Unit,
) {
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current

    // Inherited overlay-active signal, read BEFORE providing it below so a nested
    // host ORs its own state with every ancestor's (see LocalAMOverlayActive). This
    // is also what makes AMFloatingActionsHost's FAB fade when its own menu opens:
    // that host wraps [scene + FAB] as this overlay's hostContent.
    val inheritedOverlayActive = LocalAMOverlayActive.current

    // System back dismisses the overlay when it is presented and dismissable.
    // Non-dismissable overlays (onDismiss == null) leave back-press to the host.
    BackHandler(enabled = isPresented && onDismiss != null) {
        onDismiss?.invoke()
    }

    // Animate the blur radius: 0dp when dismissed, the token radius when
    // presented (paused-world chrome is token-locked — no public knob).
    // overlayFadeDp mirrors AMTheme.Motion.overlayFade typed as AnimationSpec<Dp>;
    // routing it through resolved() keeps it the gentle fade under reduce motion.
    val animatedRadius: Dp by animateDpAsState(
        targetValue   = if (isPresented) AMTheme.Overlay.blurRadius.dp else 0.dp,
        animationSpec = tokens.motion.resolved(overlayFadeDp, reduceMotion),
        label         = "AMActionsOverlay.blurRadius",
    )

    Box(modifier = modifier) {
        // ── Content layer — blur applied here (AMENDED ruling 2) ─────────────
        // amBackdropBlur is applied to the hostContent node, not a sibling.
        // The animated radius ramps 0→blurRadius on present and back on dismiss.
        // While presented the whole subtree is hidden from TalkBack so the modal
        // chrome (below) is the only thing focus can reach — iOS `.isModal`.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .amBackdropBlur(animatedRadius)
                .then(
                    if (isPresented) Modifier.semantics { invisibleToUser() } else Modifier,
                ),
        ) {
            // Reports "an overlay is up" (this overlay OR any ancestor host) down to
            // hostContent so a wrapped FAB hub fades out of the way.
            CompositionLocalProvider(
                LocalAMOverlayActive provides (isPresented || inheritedOverlayActive),
            ) {
                hostContent()
            }
        }

        // ── Paused-world chrome — scrim + catcher + content, one opacity group ─
        // The whole group fades in/out together on overlayFade (a single
        // AnimatedVisibility), mirroring iOS's `.transition(.opacity)` over the
        // entire IMModalBackdrop. No slide, no spring. isTraversalGroup keeps
        // TalkBack focus inside the modal chrome.
        AnimatedVisibility(
            visible = isPresented,
            enter   = fadeIn(animationSpec = tokens.motion.resolvedFinite(AMTheme.Motion.overlayFade, reduceMotion)),
            exit    = fadeOut(animationSpec = tokens.motion.resolvedFinite(AMTheme.Motion.overlayFade, reduceMotion)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { isTraversalGroup = true },
            ) {
                // Dark scrim (dark mode only — AMDarkScrim reads LocalAMIsDarkTheme
                // per AMENDED ruling 3). Opacity is token-locked (AMTheme.Overlay).
                AMDarkScrim(opacity = AMTheme.Overlay.scrimOpacity)

                // Full-screen tap/drag catcher, BEHIND the bare content. Only taps
                // and drags that miss the self-carded tiles reach it. Installed
                // only when dismissable.
                if (onDismiss != null) {
                    Box(
                        modifier = Modifier
                            // Tap-outside dismiss.
                            .amTapCatcher { onDismiss() }
                            // Drag-down dismiss — END-EVALUATED on the catcher, iOS
                            // `DragGesture(minimumDistance: 24).onEnded`. Content
                            // never translates; only the release decides. Dismiss iff
                            // the gesture engaged past dragActivationDistance AND
                            // released with net downward travel past dismissThreshold.
                            .pointerInput(onDismiss) {
                                val activationPx = dragActivationDistance.toPx()
                                val thresholdPx = tokens.motion.dismissThreshold.toPx()
                                // House rule: gesture accumulators are plain vars
                                // inside the pointerInput block (precedent: FAB,
                                // Slider, amDragDismiss).
                                var netTranslation = 0f
                                var travel = 0f
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        netTranslation = 0f
                                        travel = 0f
                                    },
                                    onDragEnd = {
                                        if (travel >= activationPx && netTranslation > thresholdPx) {
                                            onDismiss()
                                        }
                                    },
                                ) { change, dragAmount ->
                                    change.consume()
                                    netTranslation += dragAmount
                                    travel += abs(dragAmount)
                                }
                            }
                            // TalkBack dismiss action — the Android analogue of iOS's
                            // accessibilityAction(.escape) on the catcher.
                            .semantics {
                                dismiss {
                                    onDismiss()
                                    true
                                }
                            },
                    )
                }

                // ── Overlay content — rendered BARE over the blur ─────────────
                // No card, no surface. The content (typically AMActionGrid) owns
                // its own bottom-anchoring; each tile self-cards. Sits on TOP of the
                // catcher so tile taps hit tiles and misses fall through to dismiss.
                overlayContent()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
//
// Note: Android Studio's preview renderer does not execute RenderEffect
// chains; Modifier.amBackdropBlur renders as a no-op in preview. The dark
// scrim (dark mode) and the free-floating, bottom-anchored, self-carded tiles
// still render correctly. Validate blur on a physical device or emulator (API 31+).
// ---------------------------------------------------------------------------

@Composable
private fun ActionsOverlayPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

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

    AMActionsOverlay(
        isPresented    = true,
        onDismiss      = {},
        overlayContent = {
            // The canonical content: an AMActionGrid whose leading weighted spacer
            // bottom-anchors the row over the blur. Tiles keep their default
            // (self-carded) elevation — no sheet supplies the surface.
            AMActionGrid {
                AMActionGridRow(
                    tileA = {
                        AMActionGridButton(
                            icon    = Icons.Default.Search,
                            title   = "Find",
                            onClick = {},
                        )
                    },
                    tileB = {
                        AMActionGridButton(
                            icon    = Icons.Default.Delete,
                            title   = "Clean",
                            role    = AMButtonRole.Destructive,
                            onClick = {},
                        )
                    },
                    tileC = {
                        AMActionGridButton(
                            icon    = Icons.Default.Settings,
                            title   = "Settings",
                            onClick = {},
                        )
                    },
                )
            }
        },
    ) {
        sceneContent()
    }
}

@Preview(
    name = "AMActionsOverlay · Light",
    group = "AMActionsOverlay",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    widthDp = 360,
    heightDp = 480,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMActionsOverlayPreviewLight() {
    AMThemeRoot {
        ActionsOverlayPreviewContent()
    }
}

@Preview(
    name = "AMActionsOverlay · Dark",
    group = "AMActionsOverlay",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    widthDp = 360,
    heightDp = 480,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMActionsOverlayPreviewDark() {
    AMThemeRoot {
        ActionsOverlayPreviewContent()
    }
}
