package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType

/** What a screen reader says for the reveal button while the password is hidden. */
private const val ShowPasswordLabel = "Show password"

/** What a screen reader says for the reveal button while the password shows. */
private const val HidePasswordLabel = "Hide password"

/** A password keyboard without autocorrection: what a secure field's keyboard options fall back to. */
private val PasswordKeyboard = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)

/**
 * A password input: Material's outlined secure text field on a [TextFieldState], its characters
 * hidden (each shows briefly as it's typed), with an eye at its end that shows and hides them.
 *
 * ```kotlin
 * val password = rememberTextFieldState()
 * AMSecureField(password, label = "New password")
 * ```
 *
 * - The eye (`visibility`, "Show password") reveals the text in place; tapped again
 *   (`visibility_off`, "Hide password") it hides it. With [showRevealButton] false the field
 *   shows the clear button instead, while it holds text.
 * - Everything else is [AMTextField]'s: a widget that spans its width inside a padded card, named by
 *   its label for TalkBack, quiet unless [autofocus] says otherwise, moving through a form with the
 *   keyboard's action key, filtering what's typed with [inputTransformation] (a numeric PIN).
 * - The keyboard is a password keyboard without autocorrection; values set in [keyboardOptions]
 *   (an action key, say) are kept and the rest fall back to that.
 *
 * Disabled, it greys to Material's disabled look, ignores taps and its eye is disabled too.
 *
 * @param state the field's text and selection, usually `rememberTextFieldState()`.
 * @param modifier the field's placement. It fills the width it's offered.
 * @param label the field's name, floating in its outline.
 * @param placeholder a hint shown while the field is focused and empty. Null shows none.
 * @param supportingText a line under the field: a hint, or what's wrong while [isError]. Null shows
 *   none.
 * @param isError whether the value is invalid (confirmation doesn't match): the field takes the
 *   scheme's error color.
 * @param showRevealButton whether the eye shows; false shows the clear button instead.
 * @param autofocus whether the field takes focus as it appears, and where the caret goes.
 * @param inputTransformation filters or changes each edit the user makes before the field takes
 *   it, such as digits only for a PIN. Null accepts every edit.
 * @param keyboardOptions the keyboard's action key and any other option to set over the password
 *   keyboard.
 * @param onKeyboardAction what the keyboard's action key does. Null keeps the key's own behavior.
 * @param enabled whether the field takes input.
 */
@Composable
public fun AMSecureField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    label: String = "Password",
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    showRevealButton: Boolean = true,
    autofocus: AMTextFieldAutofocus = AMTextFieldAutofocus.Off,
    inputTransformation: InputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    enabled: Boolean = true,
) {
    val isEnabled = resolveEnabled(enabled)
    var revealed by rememberSaveable { mutableStateOf(false) }
    val hasText by remember(state) { derivedStateOf { state.text.isNotEmpty() } }
    val trailingIcon: (@Composable () -> Unit)? = when {
        showRevealButton -> {
            {
                IconButton(onClick = { revealed = !revealed }, enabled = isEnabled) {
                    Icon(
                        painterResource(if (revealed) R.drawable.am_ic_visibility_off else R.drawable.am_ic_visibility),
                        contentDescription = if (revealed) HidePasswordLabel else ShowPasswordLabel,
                    )
                }
            }
        }
        isEnabled && hasText -> { { InputClearButton(state) } }
        else -> null
    }
    OutlinedSecureTextField(
        state = state,
        modifier = modifier.fillMaxWidth().then(inputAutofocus(state, autofocus)),
        enabled = isEnabled,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        trailingIcon = trailingIcon,
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        inputTransformation = inputTransformation,
        textObfuscationMode = if (showRevealButton && revealed) {
            TextObfuscationMode.Visible
        } else {
            TextObfuscationMode.RevealLastTyped
        },
        keyboardOptions = PasswordKeyboard.merge(keyboardOptions),
        onKeyboardAction = onKeyboardAction,
    )
}
