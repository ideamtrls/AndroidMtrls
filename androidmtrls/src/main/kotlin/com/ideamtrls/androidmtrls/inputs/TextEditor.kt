package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization

/** A keyboard that starts each sentence with a capital: what the editor writes with. */
private val SentenceKeyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

/**
 * A multiline text input: Material's outlined text field on a [TextFieldState] that grows with its
 * text from [minLines] to [maxLines] lines, then scrolls inside. Named by its own floating [label].
 *
 * ```kotlin
 * val summary = rememberTextFieldState()
 * AMTextEditor(summary, label = "This week", placeholder = "What shipped, what's next")
 * ```
 *
 * - A widget that spans the width it's given: put it in a card padded `spacing.lg`, as with
 *   [AMTextField].
 * - The keyboard's Enter key starts a new line, and each sentence starts with a capital.
 * - Quiet by default: it takes focus only when tapped, unless [autofocus] says otherwise (a compose
 *   sheet that opens ready to type).
 * - [inputTransformation] filters each edit the user makes before the editor shows it:
 *   `InputTransformation.maxLength(280)` caps a post's length.
 * - TalkBack reads the label as the editor's name, then its text.
 *
 * Disabled, it greys to Material's disabled look and ignores taps.
 *
 * @param state the editor's text and selection, usually `rememberTextFieldState()`.
 * @param label the editor's name, floating in its outline once it holds text or focus.
 * @param modifier the editor's placement. It fills the width it's offered.
 * @param placeholder a hint shown while the editor is focused and empty. Null shows none.
 * @param minLines the height it starts at, in lines; at least 1.
 * @param maxLines the height it grows to, in lines, before its text scrolls; at least [minLines].
 * @param autofocus whether the editor takes focus as it appears, and where the caret goes.
 * @param inputTransformation filters or changes each edit the user makes before the editor shows
 *   it, such as a length limit. Null accepts every edit.
 * @param enabled whether the editor takes input.
 */
@Composable
public fun AMTextEditor(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    minLines: Int = 3,
    maxLines: Int = 8,
    autofocus: AMTextFieldAutofocus = AMTextFieldAutofocus.Off,
    inputTransformation: InputTransformation? = null,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        state = state,
        modifier = modifier.fillMaxWidth().then(inputAutofocus(state, autofocus)),
        enabled = resolveEnabled(enabled),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        inputTransformation = inputTransformation,
        keyboardOptions = SentenceKeyboard,
        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = minLines, maxHeightInLines = maxLines),
    )
}
