package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMSecureField
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val SecureFieldEntry = ComponentEntry(
    id = "secureField",
    category = CatalogCategory.Inputs,
    title = "Secure field",
    summary = "Password input with a show and hide toggle",
    icon = R.drawable.ic_password,
    keywords = listOf(
        "AMSecureField", "OutlinedSecureTextField", "password", "reveal", "eye", "visibility", "obscured",
        "secure entry",
    ),
) { SecureFieldPage() }

@Composable
private fun SecureFieldPage() {
    ComponentPage(
        SecureFieldEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { PasswordField() },
    ) {
        ChangePassword()
    }
}

// A stand-in for your own action.
private fun updatePassword() {}

/** An empty password field on a surface strip. */
@Composable
private fun PasswordField() {
    val password = rememberTextFieldState()
    Box(Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(AMTheme.tokens.spacing.lg)) {
        AMSecureField(password)
    }
}

/** Changing a password: the current one, a new one and its confirmation, then the button. */
@Composable
private fun ChangePassword() {
    val spacing = AMTheme.tokens.spacing
    val current = rememberTextFieldState("hunter2")
    val password = rememberTextFieldState("correct horse")
    val confirmation = rememberTextFieldState()
    val mismatch = confirmation.text.isNotEmpty() && confirmation.text.toString() != password.text.toString()
    val next = KeyboardOptions(imeAction = ImeAction.Next)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Column(
            Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            AMSecureField(current, label = "Current password", showRevealButton = false, keyboardOptions = next)
            AMSecureField(password, label = "New password", keyboardOptions = next)
            AMSecureField(
                confirmation,
                label = "Confirm password",
                supportingText = if (mismatch) "Passwords don't match" else null,
                isError = mismatch,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        }
        AMButton(
            "Update password",
            onClick = ::updatePassword,
            modifier = Modifier.fillMaxWidth(),
            enabled = current.text.isNotEmpty() && confirmation.text.isNotEmpty() && !mismatch,
        )
    }
}
