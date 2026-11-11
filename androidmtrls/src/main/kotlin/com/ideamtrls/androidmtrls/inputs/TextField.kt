package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Whether a text input ([AMTextField], [AMSecureField], [AMSearchField], [AMTextEditor]) takes
 * focus as it appears, and where the caret lands in text it already holds. One value rather than
 * two flags, so "caret at the start without focus", which would do nothing, can't be written.
 */
public enum class AMTextFieldAutofocus {
    /**
     * The field sits quietly until it's tapped. The default, so several fields and other controls
     * share a screen without fighting over the keyboard.
     */
    Off,

    /** The field takes focus as it appears, the caret after any text it holds. */
    CaretAtEnd,

    /**
     * The field takes focus as it appears, the caret before any text it holds: an editor whose
     * value starts with what the user came to read or change (a web address).
     */
    CaretAtStart,
}

/**
 * A single-line text input: Material's outlined text field on a [TextFieldState], named by its own
 * floating [label], with a clear button at its end while it holds text.
 *
 * ```kotlin
 * val email = rememberTextFieldState()
 * AMTextField(
 *     email,
 *     label = "Email",
 *     keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
 * )
 * ```
 *
 * - **A widget, not a surface.** It draws Material's outline and nothing else, and spans the width
 *   it's given. Put it in a card padded `spacing.lg` (`Modifier.amCard(padding = …)`), several
 *   fields stacked `spacing.md` apart, so the card supplies the floating surface.
 * - **Quiet by default.** It takes focus only when tapped, unless [autofocus] says otherwise. A tap
 *   puts the caret where the finger lands, as on any Android field.
 * - **Moving through a form** is the keyboard's: give each field `imeAction = ImeAction.Next` and
 *   the last `ImeAction.Done`, and leave [onKeyboardAction] null, so the action key moves focus to
 *   the next field and finally closes the keyboard. To move focus from code, put a
 *   `Modifier.focusRequester` in [modifier].
 * - **Filtering what goes in** is Material's [inputTransformation]: it sees each edit the user
 *   makes (typing, pasting, the keyboard's suggestions) before the field shows it, so a character
 *   it drops never appears, not even for a frame. `InputTransformation.maxLength(6)` caps the
 *   length; a custom one keeps digits only. Text the app sets on [state] itself isn't filtered.
 * - TalkBack reads the label as the field's name, then its text.
 *
 * Disabled (its own [enabled], or a disabling container around it), it greys to Material's
 * disabled look, ignores taps and shows no clear button.
 *
 * @param state the field's text and selection, usually `rememberTextFieldState()`.
 * @param label the field's name, floating in its outline once it holds text or focus.
 * @param modifier the field's placement. It fills the width it's offered; size modifiers placed here
 *   narrow it.
 * @param placeholder a hint shown while the field is focused and empty. Null shows none.
 * @param supportingText a line under the field: a hint, or what's wrong while [isError]. Null shows
 *   none.
 * @param isError whether the value is invalid: the outline, label and supporting line turn the
 *   scheme's error color.
 * @param autofocus whether the field takes focus as it appears, and where the caret goes.
 * @param inputTransformation filters or changes each edit the user makes before the field shows
 *   it, such as `InputTransformation.maxLength(6)`. Null accepts every edit.
 * @param keyboardOptions the keyboard's type, capitalization, autocorrection and action key.
 * @param onKeyboardAction what the keyboard's action key does. Null keeps the key's own behavior
 *   (Next moves focus on, Done closes the keyboard).
 * @param enabled whether the field takes input.
 */
@Composable
public fun AMTextField(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    autofocus: AMTextFieldAutofocus = AMTextFieldAutofocus.Off,
    inputTransformation: InputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    enabled: Boolean = true,
) {
    val isEnabled = resolveEnabled(enabled)
    val hasText by remember(state) { derivedStateOf { state.text.isNotEmpty() } }
    OutlinedTextField(
        state = state,
        modifier = modifier.fillMaxWidth().then(inputAutofocus(state, autofocus)),
        enabled = isEnabled,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        trailingIcon = if (isEnabled && hasText) { { InputClearButton(state) } } else null,
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        inputTransformation = inputTransformation,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        lineLimits = TextFieldLineLimits.SingleLine,
    )
}
