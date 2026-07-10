package io.github.jmsdnns.androidmtrls.controls.inputs

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButtonSize
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// AMSearchField
// ---------------------------------------------------------------------------

/** Leading magnifier glyph size — a touch larger than the trailing icon. */
private val AMSearchGlyphSize = 20.dp

/**
 * Search-specialized text input in the Android search-bar idiom: a full pill.
 *
 * **Pill container:** a 56dp-tall capsule on the floating [AMTheme.Surface]
 * `.primary` fill (white in light, black in dark, with the dark-mode card
 * hairline) — search bars are the one pill-shaped field on Android. Unlike
 * [AMTextField] it carries no bottom indicator (search fields do not label
 * themselves).
 *
 * **Leading glyph:** a static decorative magnifier ([Icons.Rounded.Search],
 * [AMTheme.Icon] `.muted`) with no tap target.
 *
 * **Clear button:** a trailing [Icons.Filled.Close] icon button (muted) appears
 * whenever the field holds text. Tapping it calls `onValueChange("")`.
 *
 * **Keyboard:** `KeyboardType.Text`, `capitalization = None`, `autoCorrect = false`,
 * `imeAction = ImeAction.Search`. The Search key fires [onSubmit] and then
 * dismisses the keyboard.
 *
 * **Autofocus:** set [autofocus] to `true` for sheet/overlay patterns where the
 * field should grab the keyboard on first appearance. Defaults to `false`.
 *
 * To reset a search, host-side back/dismiss handling (or the trailing clear
 * button) is responsible for blanking the query — the field itself has no Cancel
 * affordance.
 *
 * Usage:
 * ```kotlin
 * var query by rememberSaveable { mutableStateOf("") }
 * AMSearchField(
 *     value = query,
 *     onValueChange = { query = it },
 *     onSubmit = { /* launch search */ },
 * )
 * ```
 *
 * @param value          Current text content.
 * @param onValueChange  Called on every keystroke with the updated string.
 * @param placeholder    Hint shown when the field is empty. Defaults to `"Search"`.
 * @param autofocus      If `true`, requests focus after the first composition.
 *   Defaults to `false` (quiet default).
 * @param onSubmit       Called when the search return key is pressed.
 * @param modifier       Modifier applied to the pill container.
 */
@Composable
fun AMSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search",
    autofocus: Boolean = false,
    onSubmit: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isDark = LocalAMIsDarkTheme.current
    val primaryColor = AMTheme.Text.primary.resolve(isDark)
    val mutedColor = AMTheme.Icon.muted.resolve(isDark)

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Internal TextFieldValue carries caret/selection state — mirrors AMTextField.
    var fieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length))) }
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
        isFocused = false,
        shape = amSearchFieldShape,
        interactionSource = interactionSource,
        onTap = { focusRequester.requestFocus() },
        modifier = modifier,
        alwaysHairline = true,
        showIndicator = false,
        leading = {
            // Static decorative magnifier — muted, no tap target.
            Image(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                colorFilter = ColorFilter.tint(mutedColor),
                modifier = Modifier.size(AMSearchGlyphSize),
            )
        },
        trailing = {
            // Trailing clear button — shown whenever the field holds text. Gate on
            // fieldValue.text (not value) to avoid a one-frame desync.
            if (fieldValue.text.isNotEmpty()) {
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
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Search,
            ),
            // Search submits and then dismisses the keyboard.
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSubmit()
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
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
private fun SearchFieldPreviewContent() {
    var empty by remember { mutableStateOf("") }
    var filled by remember { mutableStateOf("design tokens") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        // Inputs are widgets — the card supplies the surface.
        AMCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp)) {
                AMSearchField(
                    value = empty,
                    onValueChange = { empty = it },
                    placeholder = "Search",
                )
                AMSearchField(
                    value = filled,
                    onValueChange = { filled = it },
                    placeholder = "Search",
                )
            }
        }
    }
}

@Preview(
    name = "AMSearchField · Light",
    group = "AMSearchField",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
)
@Composable
private fun AMSearchFieldPreviewLight() {
    AMThemeRoot {
        SearchFieldPreviewContent()
    }
}

@Preview(
    name = "AMSearchField · Dark",
    group = "AMSearchField",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMSearchFieldPreviewDark() {
    AMThemeRoot {
        SearchFieldPreviewContent()
    }
}
