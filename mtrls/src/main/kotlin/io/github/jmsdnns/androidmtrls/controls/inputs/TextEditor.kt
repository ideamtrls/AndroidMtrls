package io.github.jmsdnns.androidmtrls.controls.inputs

import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// AMTextEditor
// ---------------------------------------------------------------------------

/**
 * Multiline text input widget with the filled block anatomy.
 *
 * **Filled container:** a filled block ([AMTheme.Surface.primary], adapting
 * light/dark) with all four corners rounded 4dp — a text block reads better
 * without a bottom-heavy shape — and a full-width bottom indicator line that
 * thickens from 1dp [AMTheme.separator] to 2dp accent on focus. No built-in
 * label — pair with a standing [AMFieldLabel] above the editor (the Mtrls
 * labeling pattern).
 *
 * Auto-grows between [minHeight] and [maxHeight] as content is entered; once
 * [maxHeight] is reached the inner field scrolls vertically. The [placeholder]
 * overlay is shown when [value] is empty.
 *
 * **Autofocus:** set [autofocus] to `true` to focus the editor and raise the
 * keyboard as soon as it appears (e.g. a compose sheet). Defaults to `false`.
 *
 * Usage:
 * ```kotlin
 * var notes by rememberSaveable { mutableStateOf("") }
 * AMTextEditor(
 *     value = notes,
 *     onValueChange = { notes = it },
 *     placeholder = "Add a description…",
 * )
 * ```
 *
 * @param value         Current text content.
 * @param onValueChange Called on every keystroke with the updated string.
 * @param placeholder   Empty-state hint. Defaults to `""`.
 * @param minHeight     Minimum height of the editor before auto-grow kicks in.
 *   Defaults to 100dp.
 * @param maxHeight     Maximum height before content scrolls. Defaults to 240dp.
 * @param autofocus     If `true`, requests focus after the first composition.
 *   Defaults to `false` (quiet default).
 * @param modifier      Modifier applied to the filled container.
 */
@Composable
fun AMTextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    minHeight: Dp = 100.dp,
    maxHeight: Dp = 240.dp,
    autofocus: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalAMIsDarkTheme.current
    val primaryColor = AMTheme.Text.primary.resolve(isDark)

    var isFocused by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val scrollState = rememberScrollState()

    // Autofocus: request focus after first composition.
    LaunchedEffect(Unit) {
        if (autofocus) {
            focusRequester.requestFocus()
        }
    }

    AMFilledField(
        placeholder = placeholder,
        text = value,
        isFocused = isFocused,
        shape = amFilledEditorShape,
        interactionSource = interactionSource,
        onTap = { focusRequester.requestFocus() },
        modifier = modifier,
        fieldAlignTop = true,
        minHeight = minHeight,
        maxHeight = maxHeight,
        fieldVerticalPadding = AMTheme.Spacing.sm.dp,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .focusRequester(focusRequester)
                .onFocusChanged { state -> isFocused = state.isFocused },
            textStyle = AMTheme.Type.body.toTextStyle(primaryColor),
            cursorBrush = SolidColor(primaryColor),
        )
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark
// ---------------------------------------------------------------------------

@Composable
private fun TextEditorPreviewContent() {
    var empty by remember { mutableStateOf("") }
    var filled by remember { mutableStateOf("Multiline content grows the editor between its min and max heights, then scrolls past the cap.") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AMTheme.Spacing.lg.dp),
    ) {
        // Inputs are widgets — the card supplies the surface.
        AMCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
                    AMFieldLabel("Notes")
                    AMTextEditor(
                        value = empty,
                        onValueChange = { empty = it },
                        placeholder = "Add a description…",
                    )
                }
                AMTextEditor(
                    value = filled,
                    onValueChange = { filled = it },
                    placeholder = "Add a description…",
                )
                AMTextEditor(
                    value = empty,
                    onValueChange = { empty = it },
                    placeholder = "Compact min-height",
                    minHeight = 60.dp,
                )
            }
        }
    }
}

@Preview(
    name = "AMTextEditor · Light",
    group = "AMTextEditor",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
)
@Composable
private fun AMTextEditorPreviewLight() {
    AMThemeRoot {
        TextEditorPreviewContent()
    }
}

@Preview(
    name = "AMTextEditor · Dark",
    group = "AMTextEditor",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMTextEditorPreviewDark() {
    AMThemeRoot {
        TextEditorPreviewContent()
    }
}
