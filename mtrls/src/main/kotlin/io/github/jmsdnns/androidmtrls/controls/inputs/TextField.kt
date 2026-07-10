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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButtonSize
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// AMTextField
// ---------------------------------------------------------------------------

/**
 * Single-line text input widget with the classic Android filled anatomy.
 *
 * **Filled container:** a floating [AMTheme.Surface] `.primary` box (white in
 * light, black in dark, with the dark-mode card hairline) with 4dp top corners
 * and a square bottom (the bottom-heavy filled shape), a 56dp minimum height,
 * and the [AMTheme.Input] `.inset` horizontal inset. A bottom indicator line
 * runs the full width — 1dp in the [AMTheme.separator] hairline color while
 * unfocused, thickening to 2dp in the accent while focused.
 *
 * **No built-in label.** In the Mtrls UI the label is external to the field —
 * pair the field with a standing [AMFieldLabel] above it.
 *
 * **Placeholder:** [placeholder] is the empty-state hint, shown whenever the
 * field is empty.
 *
 * **Autofocus:** set [autofocus] to `true` for sheet/overlay patterns where the
 * field should grab the keyboard on first appearance. Defaults to `false`.
 *
 * **Caret at start:** when both [autofocus] and [caretAtStart] are `true`, the
 * field positions the caret at the beginning of any pre-filled text.
 *
 * **Clear button:** a trailing [Icons.Filled.Close] icon button (muted) appears
 * while the field is focused and holds text. Tapping it calls `onValueChange("")`.
 *
 * **Return key:** [onSubmit] fires on the action key but the keyboard stays up so
 * multi-field forms can advance without the keyboard flickering.
 *
 * **Smart quotes/dashes:** Android does not expose an IME-level toggle for these.
 * This is a known gap.
 *
 * Usage:
 * ```kotlin
 * var text by rememberSaveable { mutableStateOf("") }
 * AMTextField(
 *     value = text,
 *     onValueChange = { text = it },
 *     placeholder = "Search…",
 *     imeAction = ImeAction.Search,
 *     onSubmit = { /* launch search */ },
 * )
 * ```
 *
 * @param value          Current text content.
 * @param onValueChange  Called on every keystroke with the updated string.
 * @param placeholder    Empty-state hint (see the placeholder rule above).
 * @param keyboardType   IME keyboard layout. Defaults to [KeyboardType.Text].
 * @param capitalization Auto-capitalization hint. Defaults to
 *   [KeyboardCapitalization.Sentences].
 * @param autoCorrect    Whether the IME should offer corrections. Defaults to
 *   `true`. Pass `false` for URL / email / code fields.
 * @param imeAction      Return-key label. Defaults to [ImeAction.Default].
 * @param autofocus      If `true`, requests focus after the first composition.
 *   Defaults to `false` (quiet default).
 * @param caretAtStart   If `true` and [autofocus] is also `true`, positions
 *   the caret at position 0 of any pre-filled text after focusing. Defaults to
 *   `false`.
 * @param onSubmit       Called when the return / action key is pressed.
 * @param modifier       Modifier applied to the filled container.
 */
@Composable
fun AMTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    autoCorrect: Boolean = true,
    imeAction: ImeAction = ImeAction.Default,
    autofocus: Boolean = false,
    caretAtStart: Boolean = false,
    onSubmit: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isDark = LocalAMIsDarkTheme.current
    val primaryColor = AMTheme.Text.primary.resolve(isDark)

    // Internal TextFieldValue carries caret/selection state not present in the
    // plain String binding, allowing caret-at-start positioning on autofocus.
    // Typed characters update fieldValue → onValueChange (maintaining selection);
    // a programmatic value change realigns fieldValue with the caret at the end.
    var fieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length))) }
    if (fieldValue.text != value) {
        fieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
    }

    // One-shot flag: consume the caret-at-start placement only once per focus event.
    var caretAtStartPending by remember { mutableStateOf(caretAtStart && autofocus && value.isNotEmpty()) }
    var isFocused by remember { mutableStateOf(false) }

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
            // Trailing clear button — shown while focused with text to clear. Gate
            // on fieldValue.text (not value) to avoid a one-frame desync.
            if (isFocused && fieldValue.text.isNotEmpty()) {
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
                .onFocusChanged { state ->
                    isFocused = state.isFocused
                    if (state.isFocused && caretAtStartPending) {
                        caretAtStartPending = false
                        fieldValue = fieldValue.copy(selection = TextRange(0))
                    }
                },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = capitalization,
                autoCorrectEnabled = autoCorrect,
                imeAction = imeAction,
            ),
            // Return submits but leaves the keyboard up so a multi-field form can
            // advance without the keyboard flickering.
            keyboardActions = KeyboardActions(
                onAny = { onSubmit() },
            ),
            textStyle = AMTheme.Type.body.toTextStyle(primaryColor),
            cursorBrush = SolidColor(primaryColor),
            singleLine = true,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark
// ---------------------------------------------------------------------------

@Composable
private fun TextFieldPreviewContent() {
    var empty by remember { mutableStateOf("") }
    var labeled by remember { mutableStateOf("Quarterly planning notes") }
    var plain by remember { mutableStateOf("") }

    val isDark = LocalAMIsDarkTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // Standing label above the field — the mtrls labeling pattern.
        Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
            AMFieldLabel("Email")
            AMTextField(
                value = empty,
                onValueChange = { empty = it },
                placeholder = "you@example.com",
            )
        }
        // Filled — the standing label keeps the field identifiable.
        Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
            AMFieldLabel("Title")
            AMTextField(
                value = labeled,
                onValueChange = { labeled = it },
                placeholder = "Title",
            )
        }
        // Placeholder-only field.
        AMTextField(
            value = plain,
            onValueChange = { plain = it },
            placeholder = "Unlabeled field",
        )
    }
}

@Preview(
    name = "AMTextField · Light",
    group = "AMTextField",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
)
@Composable
private fun AMTextFieldPreviewLight() {
    AMThemeRoot {
        TextFieldPreviewContent()
    }
}

@Preview(
    name = "AMTextField · Dark",
    group = "AMTextField",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMTextFieldPreviewDark() {
    AMThemeRoot {
        TextFieldPreviewContent()
    }
}
