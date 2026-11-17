package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * A search input that is its own card: Material's text field on a [TextFieldState], drawn on the
 * floating card ([amCard]) with no outline or indicator line, the search glyph at its start, the
 * placeholder naming what's searched, and a clear button at its end while it holds text.
 *
 * ```kotlin
 * val query = rememberTextFieldState()
 * Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
 *     AMSearchField(query, placeholder = "Search cities")
 *     Column(Modifier.amCard()) { cities.filter { it.contains(query.text, ignoreCase = true) }.forEach { … } }
 * }
 * ```
 *
 * - **A card, not a field in one.** The search is the screen's first surface: the card's surface,
 *   shape and shadow in the app's surface setting, 64 dp tall, spanning the width it's given. Put
 *   it straight on the page or sheet, in the gutter, above the card of results; inside another
 *   card it demotes as any card does.
 * - **The keyboard's action key is Search** and calls [onSearch] with the query.
 * - **Back closes it**: the first back hides the keyboard, and the next one takes the focus away,
 *   before back leaves the screen. The query stays; the clear button empties it. There is no
 *   Cancel button.
 * - Quiet by default: it takes focus only when tapped, unless [autofocus] says otherwise.
 *
 * Disabled, its content greys to Material's disabled look and it ignores taps.
 *
 * @param state the query and its selection, usually `rememberTextFieldState()`. Filter as it
 *   changes, reading `state.text`.
 * @param modifier the field's placement. It fills the width it's offered.
 * @param placeholder the hint shown while the query is empty, naming what's searched.
 * @param autofocus whether the field takes focus as it appears, and where the caret goes.
 * @param onSearch called with the query when the keyboard's Search key is pressed.
 * @param enabled whether the field takes input.
 */
@Composable
public fun AMSearchField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    autofocus: AMTextFieldAutofocus = AMTextFieldAutofocus.Off,
    onSearch: (query: String) -> Unit = {},
    enabled: Boolean = true,
) {
    val isEnabled = resolveEnabled(enabled)
    val hasText by remember(state) { derivedStateOf { state.text.isNotEmpty() } }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val keyboardUp by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
    // Back takes the focus away from a focused field, standing aside while the keyboard is up so
    // the keyboard's own back hides it first.
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = focused && !keyboardUp,
        onBackCompleted = { focusManager.clearFocus() },
    )
    // The card is the field's surface, so Material draws no container and no line of its own.
    val clear = Color.Transparent
    TextField(
        state = state,
        modifier = modifier
            .fillMaxWidth()
            .amCard()
            .heightIn(min = SearchFieldHeight)
            .onFocusChanged { focused = it.isFocused }
            .then(inputAutofocus(state, autofocus)),
        enabled = isEnabled,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(painterResource(R.drawable.am_ic_search), contentDescription = null) },
        trailingIcon = if (isEnabled && hasText) { { InputClearButton(state) } } else null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { onSearch(state.text.toString()) },
        lineLimits = TextFieldLineLimits.SingleLine,
        shape = MaterialTheme.shapes.large,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = clear,
            unfocusedContainerColor = clear,
            disabledContainerColor = clear,
            errorContainerColor = clear,
            focusedIndicatorColor = clear,
            unfocusedIndicatorColor = clear,
            disabledIndicatorColor = clear,
            errorIndicatorColor = clear,
        ),
    )
}

/** The search field's height: a step taller than Material's 56 dp field, as the screen's first card. */
internal val SearchFieldHeight = 64.dp
