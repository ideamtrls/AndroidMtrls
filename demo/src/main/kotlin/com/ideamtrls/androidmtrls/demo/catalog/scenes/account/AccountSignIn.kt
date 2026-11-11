package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMButtonStyle
import com.ideamtrls.androidmtrls.AMFormActions
import com.ideamtrls.androidmtrls.AMSecureField
import com.ideamtrls.androidmtrls.AMTextField
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

/** The email keyboard: no capitals, no autocorrection. */
private val AccountEmailKeyboard = KeyboardOptions(
    capitalization = KeyboardCapitalization.None,
    autoCorrectEnabled = false,
    keyboardType = KeyboardType.Email,
)

/**
 * The credentials step in either mode, [AccountPhase.SignIn] or [AccountPhase.CreateAccount]: email
 * and password in one card, the main button, and the text-button links.
 */
@Composable
internal fun AccountSignIn(model: AccountModel, mode: AccountPhase) {
    val spacing = AMTheme.tokens.spacing
    val signsIn = mode == AccountPhase.SignIn
    AccountStepPage(
        title = if (signsIn) "Sign in" else "Create account",
        onBack = model::goBack,
        subtitle = if (signsIn) "Welcome back. Your reading list is waiting." else "Start free, with 5 GB for everything you save.",
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            // Outlined fields carry their own outline: stacked in a padded card, no divider.
            Column(
                Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                AMTextField(
                    model.email,
                    label = "Email",
                    keyboardOptions = AccountEmailKeyboard.copy(imeAction = ImeAction.Next),
                )
                AMSecureField(
                    model.password,
                    label = if (signsIn) "Password" else "Create a password",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    onKeyboardAction = { model.submitCredentials() },
                )
            }
            AMButton(
                if (signsIn) "Sign in" else "Continue",
                onClick = model::submitCredentials,
                modifier = Modifier.fillMaxWidth(),
                style = AMButtonStyle.Primary(),
                enabled = model.canSubmitCredentials,
            )
            if (signsIn) {
                Row(Modifier.fillMaxWidth()) {
                    AMButton(
                        "Forgot password?",
                        onClick = { model.showsForgotPassword = true },
                        style = AMButtonStyle.Ghost,
                    )
                    Spacer(Modifier.weight(1f))
                    AMButton(
                        "Create account",
                        onClick = { model.begin(AccountPhase.CreateAccount) },
                        style = AMButtonStyle.Ghost,
                    )
                }
            } else {
                AMButton(
                    "I have an account",
                    onClick = { model.begin(AccountPhase.SignIn) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = AMButtonStyle.Ghost,
                )
            }
        }
    }
}

/**
 * Sends a password reset link, under the sheet's "Reset password" title: a line on what happens,
 * the email (the account's, to edit), and "Send link".
 */
@Composable
internal fun AccountForgotPasswordSheet(model: AccountModel) {
    val spacing = AMTheme.tokens.spacing
    val email = rememberTextFieldState(model.email.text.toString())
    val canSend = email.text.isNotBlank()
    val send = { if (canSend) model.sendResetLink(email.text.trim().toString()) }
    Column(
        Modifier.fillMaxWidth().padding(start = spacing.lg, end = spacing.lg, bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Text(
            "Enter the email on your account and we'll send a link to choose a new password.",
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Box(Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg))) {
            AMTextField(
                email,
                label = "Email",
                keyboardOptions = AccountEmailKeyboard.copy(imeAction = ImeAction.Send),
                onKeyboardAction = { send() },
            )
        }
        AMFormActions {
            AMButton("Send link", onClick = send, enabled = canSend)
        }
    }
}
