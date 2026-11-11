package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.ideamtrls.androidmtrls.AMActionGrid
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow
import com.ideamtrls.androidmtrls.AMAlert
import com.ideamtrls.androidmtrls.AMAlertAction
import com.ideamtrls.androidmtrls.AMAlertItem
import com.ideamtrls.androidmtrls.AMBanner
import com.ideamtrls.androidmtrls.AMFloatingActions
import com.ideamtrls.androidmtrls.AMKeepsPlace
import com.ideamtrls.androidmtrls.AMLoadingOverlay
import com.ideamtrls.androidmtrls.AMPageHeader
import com.ideamtrls.androidmtrls.AMScene
import com.ideamtrls.androidmtrls.AMSheet
import com.ideamtrls.androidmtrls.AMSheetHeight
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMToast
import com.ideamtrls.androidmtrls.AMToolbarButton
import com.ideamtrls.androidmtrls.amCrossfade
import com.ideamtrls.androidmtrls.amInstantSwitch

internal val AccountSceneEntry = ComponentEntry(
    id = "accountScene",
    category = CatalogCategory.Showcase,
    title = "Account",
    summary = "Onboarding, sign-in, and settings",
    icon = R.drawable.ic_account_circle,
    keywords = listOf(
        "AMScene", "AMKeepsPlace", "onboarding", "sign in", "login", "two-factor", "2FA", "text field",
        "secure field", "buttons", "help", "profile", "settings", "scene",
    ),
) { AccountScene() }

/**
 * Room every Account view leaves under its last control, so it scrolls clear of the floating
 * action button: the button's 56 dp, with `spacing.lg` under it and the same over it.
 */
internal val AccountFabClearance = 88.dp

/**
 * Account: onboarding, sign-in and settings for Margin, a read-later app. One [AccountModel] owns
 * the flow and every overlay. Signed out, the phases (intro, credentials, two-factor, setup) fade
 * in place inside the plain [AMScene]; signed in, Profile and Settings are the roots of an
 * [AMKeepsPlace], which the remote swaps instantly. Nothing pushes.
 */
@Composable
internal fun AccountScene() {
    val scope = rememberCoroutineScope()
    val saver = remember(scope) { AccountModel.saver(scope) }
    val model = rememberSaveable(saver = saver) { AccountModel(scope) }
    val navigator = LocalCatalogNavigator.current
    var showsRemote by rememberSaveable { mutableStateOf(false) }

    // Leaving the scene abandons whatever is still running, so nothing lands after it's gone.
    DisposableEffect(model) { onDispose { model.cancelPendingWork() } }

    AMScene(
        floatingActions = {
            AMFloatingActions(showsRemote, onExpandedChange = { showsRemote = it }, contentDescription = "Actions") {
                AccountRemote(
                    model,
                    onClose = { showsRemote = false },
                    onExit = {
                        showsRemote = false
                        model.cancelPendingWork()
                        navigator.back()
                    },
                )
            }
        },
    ) {
        if (model.isSignedIn) AccountSignedIn(model) else AccountSignedOut(model)
        AccountOverlays(model)
    }
}

/**
 * The phase showing, fading in as the phase changes. System back walks the steps in reverse, as
 * each step's back button does; from the intro it leaves the scene.
 */
@Composable
private fun AccountSignedOut(model: AccountModel) {
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = model.phase != AccountPhase.Intro,
        onBackCompleted = { model.goBack() },
    )
    Box(Modifier.fillMaxSize().amCrossfade(model.phase), propagateMinConstraints = true) {
        when (val phase = model.phase) {
            AccountPhase.Intro -> AccountIntro(model)
            AccountPhase.SignIn, AccountPhase.CreateAccount -> AccountSignIn(model, phase)
            AccountPhase.TwoFactor -> AccountTwoFactor(model)
            AccountPhase.Setup -> AccountSetup(model)
        }
    }
}

/** Profile and Settings, each keeping its place while the other shows; back from Settings returns to Profile. */
@Composable
private fun AccountSignedIn(model: AccountModel) {
    AMKeepsPlace(
        current = model.currentView,
        landing = AccountView.Profile,
        onCurrentChange = { model.currentView = it },
        modifier = Modifier.fillMaxSize(),
    ) { view ->
        when (view) {
            AccountView.Profile -> AccountProfile(model)
            AccountView.Settings -> AccountSettings(model)
        }
    }
}

/**
 * The remote's menu. Signed out: Help, Exit. Signed in: Profile, Settings over Help, Exit, the view
 * showing ringed.
 */
@Composable
private fun AccountRemote(model: AccountModel, onClose: () -> Unit, onExit: () -> Unit) {
    // The menu closes outside the switch, so its fade plays over the new view.
    fun show(view: AccountView) {
        onClose()
        amInstantSwitch { model.currentView = view }
    }
    AMActionGrid {
        if (model.isSignedIn) {
            AMActionGridRow(
                {
                    AMActionGridButton(
                        painterResource(R.drawable.ic_account_circle), "Profile",
                        onClick = { show(AccountView.Profile) },
                        active = model.currentView == AccountView.Profile,
                    )
                },
                {
                    AMActionGridButton(
                        painterResource(R.drawable.ic_settings), "Settings",
                        onClick = { show(AccountView.Settings) },
                        active = model.currentView == AccountView.Settings,
                    )
                },
            )
        }
        AMActionGridRow(
            {
                AMActionGridButton(painterResource(R.drawable.ic_help), "Help", onClick = {
                    onClose()
                    model.showsHelp = true
                })
            },
            { AMActionGridButton(painterResource(R.drawable.ic_logout), "Exit", onClick = onExit) },
        )
    }
}

/**
 * Everything the scene presents over its views: the sheets, the sign-out alert, the loading
 * overlay, the texted code's banner (a tap fills it in and verifies) and the toasts.
 */
@Composable
private fun AccountOverlays(model: AccountModel) {
    AMSheet(
        model.showsHelp,
        onDismissRequest = { model.showsHelp = false },
        title = "Help",
        height = AMSheetHeight.Content,
    ) { AccountHelpSheet() }
    AMSheet(
        model.showsForgotPassword,
        onDismissRequest = { model.showsForgotPassword = false },
        title = "Reset password",
        height = AMSheetHeight.Content,
    ) { AccountForgotPasswordSheet(model) }
    AMSheet(
        model.showsEditProfile,
        onDismissRequest = { model.showsEditProfile = false },
        title = "Edit profile",
        height = AMSheetHeight.Content,
    ) { AccountEditProfileSheet(model) }

    val signOutAlert = remember(model) {
        AMAlertItem(
            "Sign out?",
            confirm = AMAlertAction("Sign out", destructive = true, onClick = model::signOut),
            message = "Your reading list stays saved to your account.",
            cancel = AMAlertAction("Cancel"),
        )
    }
    AMAlert(signOutAlert.takeIf { model.confirmsSignOut }, onDismissRequest = { model.confirmsSignOut = false })

    AMLoadingOverlay(model.isLoading, model.loadingMessage)
    AMBanner(
        model.banner,
        onDismissRequest = { model.banner = null },
        onClick = {
            model.fillCode()
            model.verify()
        },
    )
    AMToast(model.toast, onDismissRequest = { model.toast = null })
}

/**
 * The page every step after the intro sits on: one header, Material's large top app bar with the
 * back button ([onBack]) in its top row and the step's [title] under it, collapsing as the page
 * scrolls; then a scrolling column inset by the page gutter that starts with the [subtitle] and
 * ends clear of the floating action button, making room for the keyboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountStepPage(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    val spacing = AMTheme.tokens.spacing
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AMPageHeader(
                title,
                navigationIcon = {
                    AMToolbarButton(painterResource(R.drawable.ic_arrow_back), "Back", onClick = onBack)
                },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = AMTheme.Surface.page,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.union(WindowInsets.ime),
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = AccountFabClearance),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

/**
 * The page each signed-in view sits on: its header (collapsing as the page scrolls, with [actions]
 * at its end) over a scrolling column inset by the page gutter, blocks `spacing.xl` apart, ending
 * clear of the floating action button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountPage(
    title: String,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = AMTheme.tokens.spacing
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { AMPageHeader(title, actions = actions, scrollBehavior = scrollBehavior) },
        containerColor = AMTheme.Surface.page,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = AccountFabClearance),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
            content = content,
        )
    }
}
