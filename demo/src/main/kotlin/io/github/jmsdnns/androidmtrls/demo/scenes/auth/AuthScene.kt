package io.github.jmsdnns.androidmtrls.demo.scenes.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMAvatar
import io.github.jmsdnns.androidmtrls.content.AMAvatarSize
import io.github.jmsdnns.androidmtrls.content.AMPageHeader
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGrid
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridRow
import io.github.jmsdnns.androidmtrls.controls.buttons.AMGhostButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMPrimaryButton
import io.github.jmsdnns.androidmtrls.controls.inputs.AMSecureField
import io.github.jmsdnns.androidmtrls.controls.inputs.AMTextField
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.scenes.common.LocalSceneExit
import io.github.jmsdnns.androidmtrls.demo.scenes.common.sceneFabClearance
import io.github.jmsdnns.androidmtrls.demo.scenes.common.sceneFabLift
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMToolbar
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMToolbarButton
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMBanner
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMBannerItem
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMBannerStyle
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMFloatingActionsHost
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMLoadingOverlay
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMToast
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMToastItem
import io.github.jmsdnns.androidmtrls.tokens.AMCrossfade
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for the [AuthScene] demo (Screens section).
 *
 * id `scene-auth`. Renders full-screen with no [io.github.jmsdnns.androidmtrls.demo.catalog.ComponentPage]
 * scaffold — the scene owns its chrome and its Exit affordance
 * ([LocalSceneExit]). Mirrors iOS `AuthScene`'s registration.
 */
val authSceneEntry: ComponentEntry = ComponentEntry(
    id = "scene-auth",
    section = ComponentSection.Screens,
    title = "Auth",
    subtitle = "Sign-in, 2FA, and welcome flow",
    icon = Icons.Filled.Lock,
    content = { AuthScene() },
)

// ---------------------------------------------------------------------------
// AuthPhase — the three flow steps
// ---------------------------------------------------------------------------

/**
 * The three phases of the sign-in flow. Mirrors iOS `AuthPhase`.
 */
private enum class AuthPhase { SignIn, TwoFA, Welcome }

/**
 * [Saver] persisting [AuthPhase] as its ordinal so `rememberSaveable` survives
 * process death / configuration changes (an enum saves via ordinal here rather
 * than relying on default Bundle serialization).
 */
private val AuthPhaseSaver: Saver<AuthPhase, Int> = Saver(
    save = { it.ordinal },
    restore = { AuthPhase.entries[it] },
)

/**
 * [listSaver] that persists an [Offset] as `[x, y]` so the draggable FAB's
 * dropped position survives configuration changes (same pattern as the FAB
 * catalog page). Mirrors iOS `@State fabOffset`.
 */
private val OffsetStateSaver = listSaver<MutableState<Offset>, Float>(
    save = { listOf(it.value.x, it.value.y) },
    restore = { (x, y) -> mutableStateOf(Offset(x, y)) },
)

// ---------------------------------------------------------------------------
// AuthScene
// ---------------------------------------------------------------------------

/**
 * Three-phase sign-in flow: sign-in form → 2FA → welcome. Ports iOS `AuthScene`.
 *
 * Demonstrates the form-input cluster ([AMTextField], [AMSecureField]), the
 * button-style trio ([AMPrimaryButton] / [AMGhostButton]), and the transient
 * overlay primitives ([AMLoadingOverlay], [AMBanner], [AMToast]) inside a single
 * continuous flow, all under the scene floating-actions hub.
 *
 * **Overlay host nesting** (outermost → innermost):
 * `AMLoadingOverlay → AMBanner → AMToast → AMFloatingActionsHost`. Each overlay
 * host wraps the scene content and ORs its own presented state into
 * `LocalAMOverlayActive`, so the FAB (innermost) fades out of the way whenever
 * the loading overlay, banner, toast, or its own menu is up.
 *
 * **Mocked choreography** (iOS-exact): every 3rd sign-in tap fails with a
 * destructive banner; otherwise a 1.5s loading overlay advances to 2FA. The 2FA
 * resend countdown runs 30 → 0 at 1/sec while the 2FA phase is showing. Entering
 * Welcome fires a "Signed in" toast.
 */
@Composable
fun AuthScene() {
    val isDark = LocalAMIsDarkTheme.current
    val sceneExit = LocalSceneExit.current

    // ── Flow state ──────────────────────────────────────────────────────────
    // Phase, credentials, and the attempt counter persist (rememberSaveable);
    // the transient overlays and timer counters are plain remember (they mirror
    // iOS @State that does not survive a relaunch).
    // DELIBERATE DIVERGENCE from the sibling scenes' @Stable state-holder
    // pattern: Auth is the simplest scene (one linear flow, no navigation
    // stack, no collections) and mirrors iOS Auth.swift's inline @State
    // one-for-one — a holder class would only add indirection here.
    var phase by rememberSaveable(stateSaver = AuthPhaseSaver) { mutableStateOf(AuthPhase.SignIn) }
    var email by rememberSaveable { mutableStateOf("ada@example.com") }
    var password by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var attempts by rememberSaveable { mutableIntStateOf(0) }

    var loading by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf<AMBannerItem?>(null) }
    var toast by remember { mutableStateOf<AMToastItem?>(null) }
    var resendSecondsLeft by remember { mutableIntStateOf(30) }
    // Bumped to restart the resend countdown (a tap re-sends the code).
    var resendRestart by remember { mutableIntStateOf(0) }
    var showActions by remember { mutableStateOf(false) }

    val fabOffset = rememberSaveable(saver = OffsetStateSaver) { mutableStateOf(Offset.Zero) }

    // ── Flow helpers ────────────────────────────────────────────────────────

    // Advance to the next phase. Returning to sign-in clears the prior session.
    fun advance(next: AuthPhase) {
        if (next == AuthPhase.SignIn) {
            email = "ada@example.com"
            password = ""
            otp = ""
            attempts = 0
            resendSecondsLeft = 30
        }
        phase = next
    }

    // Toolbar back chevron: peel one phase toward sign-in.
    fun goBack() {
        when (phase) {
            AuthPhase.SignIn -> Unit
            AuthPhase.TwoFA -> {
                // The countdown effect cancels itself when phase leaves TwoFA.
                resendSecondsLeft = 30
                phase = AuthPhase.SignIn
            }
            AuthPhase.Welcome -> phase = AuthPhase.TwoFA
        }
    }

    // Sign-in is mocked: every third tap fails so the banner path is reachable
    // without typing a wrong password. Otherwise the loading effect advances.
    fun attemptSignIn() {
        attempts += 1
        if (attempts % 3 == 0) {
            banner = AMBannerItem(
                title = "Wrong password",
                message = "Check your password and try again.",
                style = AMBannerStyle.Destructive,
            )
            return
        }
        loading = true
    }

    // Reset the whole flow to the initial sign-in state (replayable in-place).
    // Mirrors iOS `startOver()` — leaves `email` as the user left it.
    fun startOver() {
        phase = AuthPhase.SignIn
        password = ""
        otp = ""
        attempts = 0
        loading = false
        banner = null
        toast = null
        resendSecondsLeft = 30
    }

    // ── Timers / phase-driven effects ───────────────────────────────────────

    // Sign-in mock: once loading turns on, wait 1.5s then advance to 2FA. Flips
    // loading off first so the overlay clears. Start Over / any loading = false
    // relaunches this with a no-op, cancelling the in-flight delay (no leak).
    LaunchedEffect(loading) {
        if (loading) {
            delay(1500)
            loading = false
            phase = AuthPhase.TwoFA
        }
    }

    // Resend countdown: 30 → 0 at 1/sec while the 2FA phase is showing. Keyed on
    // phase (starts on entry, cancels when phase leaves TwoFA) and on
    // resendRestart (a resend tap restarts it). No timer survives a phase change.
    LaunchedEffect(phase, resendRestart) {
        if (phase == AuthPhase.TwoFA) {
            resendSecondsLeft = 30
            while (resendSecondsLeft > 0) {
                delay(1000)
                resendSecondsLeft -= 1
            }
        }
    }

    // Welcome toast: fires once whenever the flow reaches the welcome phase.
    LaunchedEffect(phase) {
        if (phase == AuthPhase.Welcome) {
            toast = AMToastItem(text = "Signed in", icon = Icons.Filled.CheckCircle)
        }
    }

    // ── Host stack: loading → banner → toast → FAB hub → scene body ──────────
    AMLoadingOverlay(
        isPresented = loading,
        message = "Signing in…",
    ) {
        AMBanner(
            item = banner,
            onDismiss = { banner = null },
        ) {
            AMToast(
                item = toast,
                onDismiss = { toast = null },
            ) {
                AMFloatingActionsHost(
                    isPresented = showActions,
                    onPresentChange = { showActions = it },
                    bottomInset = sceneFabLift(),
                    offset = fabOffset,
                    accessibilityLabel = "Actions",
                    menu = {
                        AMActionGrid {
                            AMActionGridRow(
                                tileA = {
                                    AMActionGridButton(
                                        icon = Icons.Filled.Refresh,
                                        title = "Start Over",
                                        onClick = {
                                            showActions = false
                                            startOver()
                                        },
                                    )
                                },
                                tileB = {
                                    AMActionGridButton(
                                        icon = Icons.AutoMirrored.Filled.Logout,
                                        title = "Exit",
                                        onClick = {
                                            showActions = false
                                            sceneExit()
                                        },
                                    )
                                },
                            )
                        }
                    },
                ) {
                    // ── Scene body ──────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AMTheme.Surface.page.resolve(isDark)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(bottom = sceneFabClearance()),
                        ) {
                            AMToolbar(
                                leading = if (phase != AuthPhase.SignIn) {
                                    {
                                        AMToolbarButton(
                                            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Back",
                                            onClick = { goBack() },
                                        )
                                    }
                                } else {
                                    null
                                },
                            )

                            AMCrossfade(
                                targetState = phase,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = AMTheme.Spacing.lg.dp),
                            ) { current ->
                                when (current) {
                                    AuthPhase.SignIn -> SignInPhase(
                                        email = email,
                                        onEmailChange = { email = it },
                                        password = password,
                                        onPasswordChange = { password = it },
                                        onSignIn = { attemptSignIn() },
                                        onCreateAccount = {
                                            toast = AMToastItem(text = "Account creation isn't part of this demo")
                                        },
                                        onForgotPassword = {
                                            toast = AMToastItem(text = "Password reset isn't part of this demo")
                                        },
                                    )

                                    AuthPhase.TwoFA -> TwoFAPhase(
                                        otp = otp,
                                        onOtpChange = { otp = it },
                                        resendSecondsLeft = resendSecondsLeft,
                                        onVerify = { advance(AuthPhase.Welcome) },
                                        onResend = {
                                            toast = AMToastItem(text = "Code re-sent")
                                            resendRestart += 1
                                        },
                                    )

                                    AuthPhase.Welcome -> WelcomePhase(
                                        onContinue = { advance(AuthPhase.SignIn) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Phase views
// ---------------------------------------------------------------------------

/**
 * Sign-in form: header + subtitle, a card of email / password fields, and the
 * primary "Sign In" action over two ghost buttons. Ports iOS `signInView`.
 */
@Composable
private fun SignInPhase(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.padding(AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
        ) {
            AMPageHeader("Sign in")
            PhaseSubtitle("Welcome back. Enter your credentials to continue.")
        }

        AMCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
            ) {
                AMTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    placeholder = "Email",
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    imeAction = ImeAction.Next,
                )
                AMSecureField(
                    value = password,
                    onValueChange = onPasswordChange,
                    placeholder = "Password",
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
        ) {
            AMPrimaryButton(label = "Sign In", onClick = onSignIn)
            Row(modifier = Modifier.fillMaxWidth()) {
                AMGhostButton(label = "Create account", onClick = onCreateAccount)
                Spacer(modifier = Modifier.weight(1f))
                AMGhostButton(label = "Forgot password?", onClick = onForgotPassword)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Two-factor step: header + subtitle, a carded numeric OTP field, and the
 * primary "Verify" action over the resend ghost button (disabled while the
 * countdown runs). Ports iOS `twoFAView`.
 */
@Composable
private fun TwoFAPhase(
    otp: String,
    onOtpChange: (String) -> Unit,
    resendSecondsLeft: Int,
    onVerify: () -> Unit,
    onResend: () -> Unit,
) {
    val resendLabel = if (resendSecondsLeft > 0) {
        "Resend code (0:${resendSecondsLeft.toString().padStart(2, '0')})"
    } else {
        "Resend code"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.padding(AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
        ) {
            AMPageHeader("Two-factor")
            PhaseSubtitle("We sent a six-digit code to your phone. Enter it to continue.")
        }

        AMCard {
            AMTextField(
                value = otp,
                onValueChange = onOtpChange,
                placeholder = "123456",
                keyboardType = KeyboardType.Number,
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
                imeAction = ImeAction.Done,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
        ) {
            AMPrimaryButton(label = "Verify", onClick = onVerify)
            AMGhostButton(
                label = resendLabel,
                onClick = onResend,
                enabled = resendSecondsLeft == 0,
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Welcome step: the signed-in identity (avatar + greeting) and a "Continue"
 * action that resets the flow back to sign-in. Ports iOS `welcomeView`. The
 * "Signed in" toast is fired by the scene root on entering this phase.
 */
@Composable
private fun WelcomePhase(
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            AMAvatar(url = null, initials = "AL", size = AMAvatarSize.LG)
            Column(
                verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
            ) {
                AMPageHeader("Welcome back, Ada")
                PhaseSubtitle("You're all set.")
            }
        }

        AMPrimaryButton(
            label = "Continue",
            onClick = onContinue,
            modifier = Modifier.padding(horizontal = AMTheme.Spacing.xl.dp),
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

// ---------------------------------------------------------------------------
// Shared bits
// ---------------------------------------------------------------------------

/**
 * The secondary supporting line under a phase's [AMPageHeader] — leading-aligned
 * subheadline in [AMTheme.Text.secondary]. Mirrors iOS `.font(.subheadline)`.
 */
@Composable
private fun PhaseSubtitle(text: String) {
    val isDark = LocalAMIsDarkTheme.current
    BasicText(
        text = text,
        style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
        modifier = Modifier.fillMaxWidth(),
    )
}
