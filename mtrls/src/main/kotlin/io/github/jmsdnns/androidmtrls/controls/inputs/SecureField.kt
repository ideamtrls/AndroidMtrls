package io.github.jmsdnns.androidmtrls.controls.inputs

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButtonSize
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// AMSecureField
// ---------------------------------------------------------------------------

/**
 * Single-line password input widget with an optional reveal toggle, drawn with the
 * classic Android filled anatomy.
 *
 * **Filled container:** the same floating [AMTheme.Surface] `.primary` box,
 * 4dp-top / square-bottom shape, 56dp minimum height, and animated 1dp→2dp
 * accent bottom indicator as [AMTextField]. No built-in label — pair with a
 * standing [AMFieldLabel] above the field (the Mtrls labeling pattern).
 *
 * **Masking:** text is hidden by [PasswordVisualTransformation] while
 * `isRevealed` is `false`. Toggling does not move the caret — selection is
 * preserved in the internal [TextFieldValue] state, kept in sync with the caller's
 * plain String binding via a standard realign pattern.
 *
 * **No persistence (by design):** neither the password text nor the reveal state
 * survives process death — both are plain `remember` (never `rememberSaveable`),
 * so the cleartext secret is never written into the Activity saved-instance
 * Bundle (which is persisted to disk). The caller owns durable storage if the
 * value must survive.
 *
 * **Trailing button:** when [showRevealButton] is `true`, a drawn eye glyph
 * (muted) toggles masking — a filled eye ("Show password") while concealed, a
 * slashed eye ("Hide password") while revealed. When `false`, an
 * [Icons.Filled.Close] clear button (muted) is shown while the field is focused
 * and holds text.
 *
 * **Return key:** the Done key fires [onSubmit] and then dismisses the keyboard.
 *
 * **Autofocus:** set [autofocus] to `true` for sheet/overlay patterns where the
 * field should grab the keyboard on first appearance. Defaults to `false`.
 *
 * Usage:
 * ```kotlin
 * var password by remember { mutableStateOf("") }
 * AMSecureField(
 *     value = password,
 *     onValueChange = { password = it },
 *     onSubmit = { /* submit form */ },
 * )
 * ```
 *
 * @param value              Current text content (the raw, unmasked password string).
 * @param onValueChange      Called on every keystroke with the updated string.
 * @param placeholder        Empty-state hint. Defaults to `"Password"`.
 * @param autofocus          If `true`, requests focus after the first composition.
 *   Defaults to `false` (quiet default).
 * @param showRevealButton   If `true`, shows the eye toggle so the user can
 *   reveal or conceal the password. If `false`, falls back to a × clear button
 *   while focused with text. Defaults to `true`.
 * @param onSubmit           Called when the Done / action key is pressed.
 * @param modifier           Modifier applied to the filled container.
 */
@Composable
fun AMSecureField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Password",
    autofocus: Boolean = false,
    showRevealButton: Boolean = true,
    onSubmit: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isDark = LocalAMIsDarkTheme.current
    val primaryColor = AMTheme.Text.primary.resolve(isDark)

    val focusManager = LocalFocusManager.current

    // Neither the secret nor the reveal state survives process death, by design:
    // the reveal flag re-conceals on restore.
    var isRevealed by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    // Internal TextFieldValue carries caret/selection state; selection is preserved
    // across the reveal toggle so the caret does not jump when the transformation
    // flips, and realigns on a programmatic value change. Held in plain `remember`,
    // NOT rememberSaveable — the cleartext password must never land in the
    // saved-instance Bundle (which persists to disk).
    var fieldValue by remember { mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length))) }
    if (fieldValue.text != value) {
        fieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
    }

    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    // Autofocus: request focus after first composition.
    LaunchedEffect(Unit) {
        if (autofocus) {
            focusRequester.requestFocus()
        }
    }

    AMFilledField(
        placeholder = placeholder,
        text = fieldValue.text,
        isFocused = isFocused,
        shape = amFilledFieldShape,
        interactionSource = interactionSource,
        onTap = { focusRequester.requestFocus() },
        modifier = modifier,
        trailing = {
            // Trailing affordance: drawn eye reveal toggle (muted, always visible)
            // when showRevealButton is true; otherwise the × clear button while
            // focused with text. active = false → Icon.muted (iOS tone).
            if (showRevealButton) {
                AMIconButton(
                    icon = if (isRevealed) AMInputEyeSlashGlyph else AMInputEyeGlyph,
                    size = AMIconButtonSize.Small,
                    active = false,
                    contentDescription = if (isRevealed) "Hide password" else "Show password",
                    onClick = { isRevealed = !isRevealed },
                )
            } else if (isFocused && fieldValue.text.isNotEmpty()) {
                AMIconButton(
                    icon = Icons.Filled.Close,
                    size = AMIconButtonSize.Small,
                    active = false,
                    contentDescription = "Clear",
                    onClick = { onValueChange("") },
                )
            }
        },
    ) {
        BasicTextField(
            value = fieldValue,
            onValueChange = { newFieldValue ->
                fieldValue = newFieldValue
                onValueChange(newFieldValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { state -> isFocused = state.isFocused },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            // Done submits and then dismisses the keyboard.
            keyboardActions = KeyboardActions(
                onDone = {
                    onSubmit()
                    focusManager.clearFocus()
                },
            ),
            textStyle = AMTheme.Type.body.toTextStyle(primaryColor),
            cursorBrush = SolidColor(primaryColor),
            singleLine = true,
            visualTransformation = if (isRevealed) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark
// ---------------------------------------------------------------------------

@Composable
private fun SecureFieldPreviewContent() {
    var empty by remember { mutableStateOf("") }
    var filled by remember { mutableStateOf("hunter2hunter2") }

    val isDark = LocalAMIsDarkTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // Standing label above the field — placeholder + eye reveal toggle.
        Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
            AMFieldLabel("Password")
            AMSecureField(
                value = empty,
                onValueChange = { empty = it },
            )
        }
        // Filled — masked value, eye reveal toggle.
        AMSecureField(
            value = filled,
            onValueChange = { filled = it },
        )
        // Filled with the clear-button fallback instead of the reveal toggle.
        AMSecureField(
            value = filled,
            onValueChange = { filled = it },
            showRevealButton = false,
        )
    }
}

@Preview(
    name = "AMSecureField · Light",
    group = "AMSecureField",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
)
@Composable
private fun AMSecureFieldPreviewLight() {
    AMThemeRoot {
        SecureFieldPreviewContent()
    }
}

@Preview(
    name = "AMSecureField · Dark",
    group = "AMSecureField",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMSecureFieldPreviewDark() {
    AMThemeRoot {
        SecureFieldPreviewContent()
    }
}
