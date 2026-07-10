package io.github.jmsdnns.androidmtrls.surfaces.overlays

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMPrimaryButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMSecondaryButton
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMButtonRole
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amHairline
import io.github.jmsdnns.androidmtrls.tokens.amSquircle
import io.github.jmsdnns.androidmtrls.tokens.resolved
import io.github.jmsdnns.androidmtrls.tokens.resolvedFinite
import io.github.jmsdnns.androidmtrls.tokens.resolvedFiniteOrNull
import java.util.UUID

// ---------------------------------------------------------------------------
// AMAlertItem — payload that drives a single alert presentation
// ---------------------------------------------------------------------------

/**
 * Payload that drives a single alert presentation.
 *
 * Set [AMAlert]'s `item` parameter to a non-null [AMAlertItem] to present the
 * alert. The buttons inside the alert card call `onDismiss` automatically
 * after invoking [confirm] or [cancel] handlers — callers do not need to clear
 * the item from within the handlers.
 *
 * @param id       Stable identity for transition keying. Defaults to a fresh [UUID].
 * @param title    Title rendered in `AMTheme.Type.title`, centered at the top of the alert card.
 * @param message  Optional supporting copy below the title, centered. Omit for terse one-liners.
 * @param confirm  Primary action — always visible. Its [AMAlertAction.role] drives styling.
 * @param cancel   Optional secondary action shown alongside [confirm]. Pass `null` for
 *                 single-button (acknowledge-only) alerts.
 *
 * iOS equivalent: `IMAlert.Item`.
 */
@Immutable
data class AMAlertItem(
    val id:      UUID    = UUID.randomUUID(),
    val title:   String,
    val message: String? = null,
    val confirm: AMAlertAction,
    val cancel:  AMAlertAction? = null,
)

// ---------------------------------------------------------------------------
// AMAlertAction — a single button in an AMAlertItem
// ---------------------------------------------------------------------------

/**
 * A single button in an [AMAlertItem].
 *
 * @param label   Button label text.
 * @param role    Optional semantic role. [AMButtonRole.Destructive] tints the filled
 *                button label [AMTheme.destructive]; `null` renders the standard label.
 * @param handler Closure invoked when the button is tapped, before the alert dismisses.
 *                Defaults to a no-op.
 *
 * iOS equivalent: `IMAlert.Action`.
 */
@Immutable
data class AMAlertAction(
    val label:   String,
    val role:    AMButtonRole? = null,
    val handler: () -> Unit = {},
)

// ---------------------------------------------------------------------------
// snapSpringDp — Dp mirror of AMTheme.Motion.snapSpring for the blur ramp
// ---------------------------------------------------------------------------
// animateDpAsState needs AnimationSpec<Dp>; the token is <Float>. Driving the blur
// ramp on snapSpring (not overlayFade) makes it track the card pop, mirroring iOS
// Alert.swift where the fixed-radius blur group fades on snapSpring. Lockstep with
// AMTheme.Motion.snapSpring: dampingRatio 0.86, stiffness 384.

/** Mirror of [AMTheme.Motion.snapSpring] typed as [androidx.compose.animation.core.AnimationSpec]<[Dp]>. */
private val snapSpringDp = spring<Dp>(dampingRatio = 0.86f, stiffness = 384f)

// ---------------------------------------------------------------------------
// snapSpringFloat — scaleIn/scaleOut + fadeIn/fadeOut spec matching snapSpring
// ---------------------------------------------------------------------------
// scaleIn/scaleOut and fadeIn/fadeOut take FiniteAnimationSpec<Float>.
// AMTheme.Motion.snapSpring is spring<Float> which satisfies FiniteAnimationSpec<Float>.
// The card pop AND the backdrop-group fade both ride this spring (iOS animates
// scale + opacity on snapSpring), so present/dismiss read as one motion.

/** Scale/fade animation spec matching [AMTheme.Motion.snapSpring] for scaleIn/scaleOut/fadeIn/fadeOut. */
private val snapSpringFloat = spring<Float>(dampingRatio = 0.86f, stiffness = 384f)

// ---------------------------------------------------------------------------
// AMAlert — host composable: attaches an alert overlay above scene content
// ---------------------------------------------------------------------------

/**
 * Modal confirmation overlay host — wraps [hostContent] and presents an alert
 * card whenever [item] is non-null.
 *
 * **Attach at the scene root** so the overlay covers the entire screen:
 * ```kotlin
 * AMAlert(item = alertItem, onDismiss = { alertItem = null }) {
 *     // Your full screen content here
 * }
 * ```
 *
 * **No outside-tap dismissal.** Unlike sheets, alerts block all input until
 * the user taps a button. This is intentional: alerts exist to force an
 * explicit yes/no choice. A transparent input-blocker sits behind the card
 * to prevent taps from reaching content underneath.
 *
 * **System back dismisses.** Pressing the Android back button while presented
 * calls [onDismiss] directly — the platform-idiomatic cancel path. Back is
 * dismiss-*without*-action: neither the confirm nor cancel handler runs, so
 * treat it as an implicit cancel. (Outside taps still do nothing.)
 *
 * **Handler contract:** when the user taps a button, the [AMAlertAction.handler]
 * is called first, then [onDismiss]. Callers should clear their item state
 * inside [onDismiss] (e.g., `{ alertItem = null }`).
 *
 * **Exit animation retention:** the last non-null [item] is retained until
 * the exit animation completes so the card does not vanish mid-fade.
 *
 * **Reduce motion:** the blur ramp, scale pop, and fade route through
 * [io.github.jmsdnns.androidmtrls.tokens.resolved] /
 * [io.github.jmsdnns.androidmtrls.tokens.resolvedFinite] /
 * [io.github.jmsdnns.androidmtrls.tokens.resolvedFiniteOrNull] with
 * [LocalAMReduceMotion]. Under reduce motion the scale pop collapses to a snap so
 * the card appears at full scale while the fade stays the gentle 180ms fade.
 *
 * @param item        Non-null to present the alert; null to dismiss it.
 * @param onDismiss   Called after a button handler runs. Use this to set your item state to null.
 * @param modifier    Modifier applied to the outermost [Box].
 * @param hostContent The composable scene content the alert sits above.
 *
 * iOS equivalent: `View.imAlert(_:)` / `IMAlertModifier` in `Sources/Surfaces/Overlays/Alert.swift`.
 */
@Composable
fun AMAlert(
    item: AMAlertItem?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hostContent: @Composable () -> Unit,
) {
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current

    // Inherited overlay-active signal, read BEFORE providing it below so a nested
    // host ORs its own state with every ancestor's (see LocalAMOverlayActive).
    val inheritedOverlayActive = LocalAMOverlayActive.current

    // Animate blur radius: 0dp (dismissed) → blurRadius.dp (presented).
    // Driven on snapSpringDp (mirror of snapSpring) so the blur ramp tracks the
    // card pop as on iOS; resolved() collapses it to the gentle fade under reduce motion.
    val animatedRadius: Dp by animateDpAsState(
        targetValue   = if (item != null) AMTheme.Overlay.blurRadius.dp else 0.dp,
        animationSpec = tokens.motion.resolved(snapSpringDp, reduceMotion),
        label         = "AMAlert.blurRadius",
    )

    // Retain the last non-null item during the exit animation so the card
    // content does not vanish mid-fade. Pattern: update only when item is
    // non-null; the visible item persists inside AnimatedVisibility until
    // the exit animation completes and the composable is removed from the tree.
    val visibleItem = remember { mutableStateOf<AMAlertItem?>(null) }
    if (item != null) visibleItem.value = item

    // System back = dismiss-without-action (the implicit cancel path). Runs no
    // handler; the caller nulls its item in onDismiss. Enabled only while shown.
    BackHandler(enabled = item != null) { onDismiss() }

    Box(modifier = modifier.fillMaxSize()) {
        // Content layer — amBackdropBlur applied here so RenderEffect processes
        // this node's output. This is the canonical host pattern per AMENDED ruling 2.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .amBackdropBlur(animatedRadius),
        ) {
            // Reports "an overlay is up" (this alert OR any ancestor host) down to
            // hostContent so a wrapped FAB hub fades out of the way.
            CompositionLocalProvider(
                LocalAMOverlayActive provides (item != null || inheritedOverlayActive),
            ) {
                hostContent()
            }
        }

        // Chrome layer — dark scrim + input blocker + centered alert card.
        // The outer AnimatedVisibility owns the null↔non-null present/dismiss: the
        // card pop (scale) AND the backdrop-group fade both ride snapSpring, so
        // present/dismiss read as one motion (iOS animates scale + opacity on
        // snapSpring, Alert.swift:155,161,165). The scale is a spatial transform, so
        // under Reduce Motion it drops to a snap (resolvedFiniteOrNull → null → snap())
        // and the card appears at full scale; the fade softens via resolvedFinite.
        AnimatedVisibility(
            visible = item != null,
            enter = scaleIn(
                initialScale  = 0.92f,
                animationSpec = tokens.motion.resolvedFiniteOrNull(snapSpringFloat, reduceMotion) ?: snap(),
            ) + fadeIn(
                animationSpec = tokens.motion.resolvedFinite(snapSpringFloat, reduceMotion),
            ),
            exit = scaleOut(
                targetScale   = 0.92f,
                animationSpec = tokens.motion.resolvedFiniteOrNull(snapSpringFloat, reduceMotion) ?: snap(),
            ) + fadeOut(
                animationSpec = tokens.motion.resolvedFinite(snapSpringFloat, reduceMotion),
            ),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Dark scrim (dark mode only) — non-interactive, passes taps.
                AMDarkScrim()

                // Input blocker: consumes ALL pointer events — no outside-tap
                // dismiss. Alerts only dismiss via their buttons. This prevents
                // the scene content underneath from receiving any interaction
                // while the alert is presented.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .pointerInput(Unit) {
                            detectTapGestures { /* consume; no dismiss on outside tap */ }
                        },
                )

                // Alert card — centered on screen, capped at 320dp (iOS maxCardWidth).
                // Render from visibleItem so card content persists during exit animation.
                val renderItem = visibleItem.value
                if (renderItem != null) {
                    // AnimatedContent(contentKey = { it.id }) replays the scale/fade pop
                    // whenever the item id changes without a null in between — an A→B
                    // swap re-fires the pop, mirroring iOS's `.id(item.id)` re-identify.
                    // (The outer AnimatedVisibility above owns null↔non-null; this owns
                    // A→B.) Reduce Motion routes through the same resolvers.
                    AnimatedContent(
                        targetState    = renderItem,
                        contentKey     = { it.id },
                        transitionSpec = {
                            (scaleIn(
                                initialScale  = 0.92f,
                                animationSpec = tokens.motion.resolvedFiniteOrNull(snapSpringFloat, reduceMotion) ?: snap(),
                            ) + fadeIn(
                                animationSpec = tokens.motion.resolvedFinite(snapSpringFloat, reduceMotion),
                            )) togetherWith (scaleOut(
                                targetScale   = 0.92f,
                                animationSpec = tokens.motion.resolvedFiniteOrNull(snapSpringFloat, reduceMotion) ?: snap(),
                            ) + fadeOut(
                                animationSpec = tokens.motion.resolvedFinite(snapSpringFloat, reduceMotion),
                            ))
                        },
                        contentAlignment = Alignment.Center,
                        label            = "AMAlert.itemSwap",
                        modifier         = Modifier.align(Alignment.Center),
                    ) { current ->
                        AMAlertCard(
                            item      = current,
                            onDismiss = onDismiss,
                            modifier  = Modifier
                                .padding(horizontal = tokens.spacing.xl)
                                .widthIn(max = 320.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AMAlertCard — internal alert content card
// ---------------------------------------------------------------------------

/**
 * Internal alert card composable. Renders title, optional message, and one or
 * two **full-width filled buttons** inside an [AMCard] surface, matching the iOS
 * alert anatomy: **centered** title/message and side-by-side filled buttons.
 *
 * Two-button alerts lay a secondary (cancel) and a primary (confirm) button
 * side-by-side, each taking half the width; single-button (acknowledge) alerts
 * render one full-width primary button. `role == Destructive` on an action tints
 * its label [AMTheme.destructive] (handled inside the button String overloads).
 * The card is capped at 320dp (iOS `maxCardWidth`).
 *
 * Separate from [AMAlert] so the card's scale/opacity transition is scoped to
 * the card itself without coupling to the full-screen backdrop logic.
 *
 * iOS equivalent: `IMAlertCard` in `Sources/Surfaces/Overlays/Alert.swift`.
 */
@Composable
private fun AMAlertCard(
    item: AMAlertItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAMTokens.current
    val isDark  = LocalAMIsDarkTheme.current

    // Unconditional hairline ring — iOS applies .imHairline unconditionally on
    // the alert card. AMCard's built-in amCardHairline is dark-only, so light
    // mode would have no ring without this. Shape matches AMCard's default
    // corner radius (tokens.radius.lg) so the ring hugs the corners.
    val cardShape = amSquircle(tokens.radius.lg)
    AMCard(
        modifier = modifier.amHairline(cardShape),
        padding  = PaddingValues(tokens.spacing.lg),
    ) {
        // spacing.md between the text block and the button row (iOS outer VStack md).
        Column(
            modifier            = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            // Title + optional message block — both CENTERED; xs gap between them,
            // xs top inset above the block (iOS inner VStack xs + .padding(.top, xs)).
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(top = tokens.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
            ) {
                BasicText(
                    text  = item.title,
                    style = AMTheme.Type.title
                        .toTextStyle(AMTheme.Text.primary.resolve(isDark))
                        .copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (item.message != null) {
                    BasicText(
                        text  = item.message,
                        style = AMTheme.Type.secondary
                            .toTextStyle(AMTheme.Text.secondary.resolve(isDark))
                            .copy(textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Buttons — full-width filled. Two-button: secondary (cancel) + primary
            // (confirm) side-by-side, each weight(1f) so they split the width evenly,
            // cancel leftmost / confirm rightmost (iOS HStack .imSecondary + .imPrimary).
            // Single-button: one full-width primary (iOS single .imPrimary). Destructive
            // role tints the label red inside the button String overload.
            if (item.cancel != null) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
                ) {
                    AMSecondaryButton(
                        label    = item.cancel.label,
                        onClick  = {
                            item.cancel.handler()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        role     = item.cancel.role,
                    )
                    AMPrimaryButton(
                        label    = item.confirm.label,
                        onClick  = {
                            item.confirm.handler()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        role     = item.confirm.role,
                    )
                }
            } else {
                AMPrimaryButton(
                    label    = item.confirm.label,
                    onClick  = {
                        item.confirm.handler()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    role     = item.confirm.role,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

// Preview items — one per alert scenario.

private val previewTwoButtonItem = AMAlertItem(
    title   = "Add to queue?",
    message = "Episode 314 will start downloading immediately.",
    confirm = AMAlertAction(label = "Add"),
    cancel  = AMAlertAction(label = "Cancel"),
)

private val previewDestructiveItem = AMAlertItem(
    title   = "Delete this episode?",
    message = "This permanently removes it from your queue and downloads.",
    confirm = AMAlertAction(label = "Delete", role = AMButtonRole.Destructive),
    cancel  = AMAlertAction(label = "Cancel"),
)

private val previewOneButtonItem = AMAlertItem(
    title   = "Connection lost",
    message = "Reconnect to continue syncing your library.",
    confirm = AMAlertAction(label = "OK"),
)

@Composable
private fun AlertPreviewContent(isDark: Boolean, item: AMAlertItem) {
    val tokens = LocalAMTokens.current

    // Show the alert card directly (not through AMAlert host) in previews so
    // it renders even though RenderEffect is a no-op in the preview renderer.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(AMTheme.Surface.page.resolve(isDark)),
    ) {
        // Placeholder scene content rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            color = AMTheme.Surface.primary.resolve(isDark),
                            shape = amSquircle(AMTheme.Radius.lg.dp),
                        ),
                ) {
                    BasicText(
                        text  = "Placeholder row ${i + 1}",
                        style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                        modifier = Modifier
                            .align(Alignment.Center),
                    )
                }
            }
        }

        // Dark scrim (preview — isDark conditional handled inside AMDarkScrim)
        AMDarkScrim()

        // Alert card centered over placeholder content, capped at 320dp (iOS maxCardWidth)
        AMAlertCard(
            item = item,
            onDismiss = {},
            modifier  = Modifier
                .align(Alignment.Center)
                .padding(horizontal = AMTheme.Spacing.xl.dp)
                .widthIn(max = 320.dp),
        )
    }
}

@Preview(
    name            = "AMAlert · Two-button · Light",
    group           = "AMAlert",
    showBackground  = true,
    backgroundColor = 0xFFF2F2F7,
    widthDp         = 360,
    heightDp        = 400,
    uiMode          = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMAlertTwoButtonPreviewLight() {
    AMThemeRoot {
        AlertPreviewContent(isDark = false, item = previewTwoButtonItem)
    }
}

@Preview(
    name            = "AMAlert · Two-button · Dark",
    group           = "AMAlert",
    showBackground  = true,
    backgroundColor = 0xFF0C0C0D,
    widthDp         = 360,
    heightDp        = 400,
    uiMode          = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMAlertTwoButtonPreviewDark() {
    AMThemeRoot {
        AlertPreviewContent(isDark = true, item = previewTwoButtonItem)
    }
}

@Preview(
    name            = "AMAlert · Destructive · Light",
    group           = "AMAlert",
    showBackground  = true,
    backgroundColor = 0xFFF2F2F7,
    widthDp         = 360,
    heightDp        = 400,
    uiMode          = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMAlertDestructivePreviewLight() {
    AMThemeRoot {
        AlertPreviewContent(isDark = false, item = previewDestructiveItem)
    }
}

@Preview(
    name            = "AMAlert · Destructive · Dark",
    group           = "AMAlert",
    showBackground  = true,
    backgroundColor = 0xFF0C0C0D,
    widthDp         = 360,
    heightDp        = 400,
    uiMode          = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMAlertDestructivePreviewDark() {
    AMThemeRoot {
        AlertPreviewContent(isDark = true, item = previewDestructiveItem)
    }
}

@Preview(
    name            = "AMAlert · One-button · Light",
    group           = "AMAlert",
    showBackground  = true,
    backgroundColor = 0xFFF2F2F7,
    widthDp         = 360,
    heightDp        = 400,
    uiMode          = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMAlertOneButtonPreviewLight() {
    AMThemeRoot {
        AlertPreviewContent(isDark = false, item = previewOneButtonItem)
    }
}

@Preview(
    name            = "AMAlert · One-button · Dark",
    group           = "AMAlert",
    showBackground  = true,
    backgroundColor = 0xFF0C0C0D,
    widthDp         = 360,
    heightDp        = 400,
    uiMode          = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMAlertOneButtonPreviewDark() {
    AMThemeRoot {
        AlertPreviewContent(isDark = true, item = previewOneButtonItem)
    }
}
