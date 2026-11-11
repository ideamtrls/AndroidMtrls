package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMButtonStyle
import com.ideamtrls.androidmtrls.AMTextField
import com.ideamtrls.androidmtrls.AMTextFieldAutofocus
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

/**
 * The two-factor step: the code field, focused as the step opens so the number keyboard is up,
 * "Verify", and "Resend code" with its countdown. The code itself arrives a moment after the step
 * opens, as a banner the scene shows; tapping it fills the field and verifies.
 */
@Composable
internal fun AccountTwoFactor(model: AccountModel) {
    val spacing = AMTheme.tokens.spacing
    AccountStepPage(
        title = "Two-factor",
        onBack = model::goBack,
        subtitle = "We sent a six-digit code to your phone ending in 42.",
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            Box(Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg))) {
                AMTextField(
                    model.code,
                    label = "Code",
                    autofocus = AMTextFieldAutofocus.CaretAtEnd,
                    inputTransformation = AccountModel.CodeInput,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    onKeyboardAction = { model.verify() },
                )
            }
            AMButton(
                "Verify",
                onClick = model::verify,
                modifier = Modifier.fillMaxWidth(),
                style = AMButtonStyle.Primary(),
                enabled = model.isCodeComplete,
            )
            AMButton(
                accountResendTitle(model.resendSecondsLeft),
                onClick = model::resendCode,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = AMButtonStyle.Ghost,
                enabled = model.resendSecondsLeft == 0,
            )
        }
    }
}

/** "Resend code (0:30)" while the countdown runs, then "Resend code". */
private fun accountResendTitle(secondsLeft: Int): String =
    if (secondsLeft > 0) "Resend code (%d:%02d)".format(secondsLeft / 60, secondsLeft % 60) else "Resend code"
