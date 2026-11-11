package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import com.ideamtrls.androidmtrls.AMBannerItem
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMToastItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Where the signed-out flow stands. The scene fades between phases. [SignIn] and [CreateAccount]
 * are the credentials step in its two modes.
 */
internal enum class AccountPhase { Intro, SignIn, CreateAccount, TwoFactor, Setup }

/** The signed-in root views, which the remote swaps instantly. */
internal enum class AccountView { Profile, Settings }

/** The typefaces the Settings "Reader font" picker offers. */
internal enum class AccountReaderFont(val title: String) {
    Serif("Serif"),
    Sans("Sans"),
    Mono("Mono"),
}

/**
 * Owns the Account scene: the signed-out flow, the account it signs in to, its settings, and every
 * overlay the scene presents. A plain class holding Compose state, remembered at the scene root and
 * saved with [saver]; what's in flight (a request, a code on its way, the resend countdown) isn't
 * saved, since it dies with [scope].
 *
 * Requests are stand-ins: a short wait behind the loading overlay, so the flow runs with no
 * network. Each runs as a coroutine in [scope], the scene's own, so leaving the scene cancels it,
 * and [cancelPendingWork] cancels it on Back and once a code verifies, so no banner or toast ever
 * lands on another screen.
 */
@Stable
internal class AccountModel(private val scope: CoroutineScope) {
    // Flow
    var phase by mutableStateOf(AccountPhase.Intro)
        private set

    /**
     * The mode the credentials step last ran in: two-factor finishes differently for a new account
     * (profile setup) than for a sign-in, and stepping back from two-factor returns to it.
     */
    var mode by mutableStateOf(AccountPhase.SignIn)
        private set

    var isSignedIn by mutableStateOf(false)
        private set

    var currentView by mutableStateOf(AccountView.Profile)

    // Account
    val email = TextFieldState("ada@example.com")
    val password = TextFieldState()
    val code = TextFieldState()
    val name = TextFieldState("Ada Lovelace")
    var interests by mutableStateOf(setOf("Science", "History", "Books"))

    // Settings
    var pushAlerts by mutableStateOf(true)
    var emailDigest by mutableStateOf(false)
    var mentionsOnly by mutableStateOf(false)
    var readerFont by mutableStateOf(AccountReaderFont.Serif)
    var privateAccount by mutableStateOf(false)
    var showActivity by mutableStateOf(true)

    // Two-factor
    /** Seconds until "Resend code" is available again; zero when it is. */
    var resendSecondsLeft by mutableIntStateOf(0)
        private set

    // Overlays
    var banner by mutableStateOf<AMBannerItem?>(null)
    var toast by mutableStateOf<AMToastItem?>(null)
    var isLoading by mutableStateOf(false)
        private set

    /** What the loading overlay says while a request runs. */
    var loadingMessage by mutableStateOf("")
        private set
    var showsHelp by mutableStateOf(false)
    var showsForgotPassword by mutableStateOf(false)
    var showsEditProfile by mutableStateOf(false)
    var confirmsSignOut by mutableStateOf(false)

    /** The request in flight: signing in, creating the account, or verifying a code. */
    private var request: Job? = null

    /** The texted code on its way and the resend countdown. */
    private var codeDelivery: Job? = null

    // Derived

    /** The first word of the name, for the welcome toast. */
    val firstName: String
        get() = name.text.trim().split(Whitespace).first()

    /** "Sign in" / "Continue" stays disabled until both fields hold something. */
    val canSubmitCredentials: Boolean
        get() = email.text.isNotBlank() && password.text.isNotEmpty()

    /** The code is complete at exactly six digits 0–9. */
    val isCodeComplete: Boolean
        get() = code.text.length == CodeLength && code.text.all { isCodeDigit(it) }

    /** The account's interests, in the order the interest picker lists them. */
    val orderedInterests: List<AccountInterest>
        get() = AccountInterest.All.filter { it.name in interests }

    // Signed-out flow

    /**
     * Opens the credentials step in [mode] ([AccountPhase.SignIn] or [AccountPhase.CreateAccount]),
     * from the intro or by switching between signing in and creating an account.
     */
    fun begin(mode: AccountPhase) {
        this.mode = mode
        phase = mode
    }

    /**
     * Signs in (or creates the account) behind the loading overlay, then moves on to two-factor.
     * Ignored while a request is already running, so a double tap advances once.
     */
    fun submitCredentials() {
        if (isLoading || !canSubmitCredentials) return
        loadingMessage = if (mode == AccountPhase.SignIn) "Signing in…" else "Creating account…"
        isLoading = true
        request = scope.launch {
            delay(1200.milliseconds)
            request = null
            isLoading = false
            // The password leaves the screen with the step; nothing keeps it.
            password.clearText()
            phase = AccountPhase.TwoFactor
            sendCode()
        }
    }

    /**
     * Steps back one phase, abandoning anything still running: credentials to the intro, and both
     * two-factor and setup to the credentials step. Setup's code is already used, so stepping back
     * to two-factor would only text another.
     */
    fun goBack() {
        when (phase) {
            AccountPhase.Intro -> Unit
            AccountPhase.SignIn, AccountPhase.CreateAccount -> {
                cancelPendingWork()
                password.clearText()
                phase = AccountPhase.Intro
            }
            AccountPhase.TwoFactor, AccountPhase.Setup -> {
                cancelPendingWork()
                code.clearText()
                phase = mode
            }
        }
    }

    // Two-factor

    /** Texts another code once the countdown has run out. */
    fun resendCode() {
        if (phase != AccountPhase.TwoFactor || isLoading || resendSecondsLeft > 0) return
        sendCode()
    }

    /** Fills in the code the banner shows. */
    fun fillCode() {
        code.setTextAndPlaceCursorAtEnd(SentCode)
    }

    /**
     * Checks the code behind the loading overlay. A sign-in lands on Profile; a new account goes on
     * to profile setup. Ignored off the two-factor step, once signed in, while a request is
     * running (so a double tap advances once), or while the code is incomplete.
     */
    fun verify() {
        if (isSignedIn || phase != AccountPhase.TwoFactor || isLoading || !isCodeComplete) return
        loadingMessage = "Verifying…"
        isLoading = true
        request = scope.launch {
            delay(800.milliseconds)
            request = null
            // The code is used: stop the countdown and any code still on its way.
            cancelPendingWork()
            code.clearText()
            if (mode == AccountPhase.SignIn) {
                signIn()
                toast = AMToastItem("Signed in")
            } else {
                phase = AccountPhase.Setup
            }
        }
    }

    /** Completes profile setup and signs the new account in. A second tap does nothing. */
    fun finishSetup() {
        if (isSignedIn || phase != AccountPhase.Setup) return
        signIn()
        toast = AMToastItem("Welcome, $firstName")
    }

    /** Emails a reset link to [address], which becomes the account's email, and closes the sheet. */
    fun sendResetLink(address: String) {
        email.setTextAndPlaceCursorAtEnd(address)
        showsForgotPassword = false
        toast = AMToastItem("Reset link sent to $address")
    }

    // Signed in

    /** Saves the "Edit profile" sheet's draft and closes the sheet. */
    fun saveProfile(name: String, interests: Set<String>) {
        this.name.setTextAndPlaceCursorAtEnd(name)
        this.interests = interests
        showsEditProfile = false
        toast = AMToastItem("Profile updated")
    }

    /** Asks before signing out. */
    fun confirmSignOut() {
        confirmsSignOut = true
    }

    /**
     * Signs out and returns to the intro. The profile and settings stay, so signing back in shows
     * them as they were.
     */
    fun signOut() {
        cancelPendingWork()
        password.clearText()
        code.clearText()
        isSignedIn = false
        phase = AccountPhase.Intro
        toast = AMToastItem("Signed out")
    }

    /**
     * Abandons any request in flight, stops the resend countdown and any code still on its way, and
     * clears the banner. Runs when the scene goes away (the Exit tile, or leaving the composition),
     * when Back leaves a step, once a code is verified, and on sign-out.
     */
    fun cancelPendingWork() {
        request?.cancel()
        request = null
        codeDelivery?.cancel()
        codeDelivery = null
        isLoading = false
        banner = null
    }

    private fun signIn() {
        currentView = AccountView.Profile
        isSignedIn = true
    }

    /**
     * Texts a code to the phone: "Resend code" starts counting down, and a moment later the code
     * arrives as a banner the scene shows. Both stop when [cancelPendingWork] cancels them.
     */
    private fun sendCode() {
        codeDelivery?.cancel()
        resendSecondsLeft = ResendWaitSeconds
        codeDelivery = scope.launch {
            launch {
                delay(1500.milliseconds)
                banner = AMBannerItem(
                    "Your code is ${SentCode.take(3)} ${SentCode.takeLast(3)}",
                    message = "Tap to fill it in",
                    style = AMSemantic.Info,
                    duration = 8.seconds,
                )
            }
            while (resendSecondsLeft > 0) {
                delay(1.seconds)
                resendSecondsLeft -= 1
            }
        }
    }

    companion object {
        /** The code the stand-in text message carries. */
        private const val SentCode = "482913"
        private const val CodeLength = 6

        /** How long "Resend code" waits after each code. */
        private const val ResendWaitSeconds = 30

        private val Whitespace = Regex("\\s+")

        /** A code digit is 0–9 only, not another script's digits. */
        private fun isCodeDigit(char: Char): Boolean = char in '0'..'9'

        /**
         * The code field's filter: whatever is typed or pasted, the field keeps its first six digits
         * 0–9 and drops everything else before it shows, the rule [isCodeComplete] checks.
         */
        val CodeInput: InputTransformation = InputTransformation {
            val digits = asCharSequence().filter(::isCodeDigit).take(CodeLength)
            if (!asCharSequence().contentEquals(digits)) replace(0, length, digits)
        }

        /**
         * Saves the flow, the account, the settings and which overlay is up; restores them into a
         * model on the restored scene's [scope]. The password and anything in flight aren't kept.
         */
        fun saver(scope: CoroutineScope): Saver<AccountModel, Any> = mapSaver(
            save = { model ->
                mapOf(
                    "phase" to model.phase.name,
                    "mode" to model.mode.name,
                    "signedIn" to model.isSignedIn,
                    "view" to model.currentView.name,
                    "email" to model.email.text.toString(),
                    "code" to model.code.text.toString(),
                    "name" to model.name.text.toString(),
                    "interests" to ArrayList(model.interests),
                    "pushAlerts" to model.pushAlerts,
                    "emailDigest" to model.emailDigest,
                    "mentionsOnly" to model.mentionsOnly,
                    "readerFont" to model.readerFont.name,
                    "privateAccount" to model.privateAccount,
                    "showActivity" to model.showActivity,
                    "showsHelp" to model.showsHelp,
                    "showsForgotPassword" to model.showsForgotPassword,
                    "showsEditProfile" to model.showsEditProfile,
                    "confirmsSignOut" to model.confirmsSignOut,
                )
            },
            restore = { saved ->
                AccountModel(scope).apply {
                    phase = AccountPhase.valueOf(saved["phase"] as String)
                    mode = AccountPhase.valueOf(saved["mode"] as String)
                    isSignedIn = saved["signedIn"] as Boolean
                    currentView = AccountView.valueOf(saved["view"] as String)
                    email.setTextAndPlaceCursorAtEnd(saved["email"] as String)
                    code.setTextAndPlaceCursorAtEnd(saved["code"] as String)
                    name.setTextAndPlaceCursorAtEnd(saved["name"] as String)
                    interests = (saved["interests"] as List<*>).filterIsInstance<String>().toSet()
                    pushAlerts = saved["pushAlerts"] as Boolean
                    emailDigest = saved["emailDigest"] as Boolean
                    mentionsOnly = saved["mentionsOnly"] as Boolean
                    readerFont = AccountReaderFont.valueOf(saved["readerFont"] as String)
                    privateAccount = saved["privateAccount"] as Boolean
                    showActivity = saved["showActivity"] as Boolean
                    showsHelp = saved["showsHelp"] as Boolean
                    showsForgotPassword = saved["showsForgotPassword"] as Boolean
                    showsEditProfile = saved["showsEditProfile"] as Boolean
                    confirmsSignOut = saved["confirmsSignOut"] as Boolean
                }
            },
        )
    }
}
