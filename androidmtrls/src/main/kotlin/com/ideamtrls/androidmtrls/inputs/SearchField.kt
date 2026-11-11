package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch

/**
 * A search input: Material's search bar input field on a [TextFieldState], with the search glyph
 * at its start and a clear button at its end while it holds text.
 *
 * ```kotlin
 * val query = rememberTextFieldState()
 * AMSearchField(query, placeholder = "Search cities")
 * cities.filter { it.contains(query.text, ignoreCase = true) }.forEach { … }
 * ```
 *
 * - **Material's look**: the search field's own filled container and shape, 56 dp tall, spanning
 *   the width it's given. In a card, pad it `spacing.lg`, as any field.
 * - **Open while focused.** Tapping it focuses it and raises the keyboard; the keyboard's action key
 *   is Search and calls [onSearch]. Back closes it: the first back hides the keyboard, and the next
 *   one takes the focus away, before back leaves the screen. The query stays; the clear button
 *   empties it. There is no Cancel button.
 * - Quiet by default: it takes focus only when tapped, unless [autofocus] says otherwise.
 * - TalkBack reads it as a search field with its placeholder or query.
 *
 * Disabled, it greys to Material's disabled look and ignores taps.
 *
 * @param state the query and its selection, usually `rememberTextFieldState()`. Filter as it
 *   changes, reading `state.text`.
 * @param modifier the field's placement. It fills the width it's offered.
 * @param placeholder the hint shown while the query is empty, naming what's searched.
 * @param autofocus whether the field takes focus as it appears, and where the caret goes.
 * @param onSearch called with the query when the keyboard's Search key is pressed.
 * @param enabled whether the field takes input.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val barState = rememberSearchBarState()
    val scope = rememberCoroutineScope()
    val open = barState.targetValue == SearchBarValue.Expanded
    val hasText by remember(state) { derivedStateOf { state.text.isNotEmpty() } }
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val keyboardUp by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
    // Back closes an open field (Material's field then takes its focus away), standing aside while
    // the keyboard is up so the keyboard's own back hides it first.
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = open && !keyboardUp,
        onBackCompleted = { scope.launch { barState.animateToCollapsed() } },
    )
    SearchBarDefaults.InputField(
        textFieldState = state,
        searchBarState = barState,
        onSearch = onSearch,
        modifier = modifier
            .fillMaxWidth()
            // Focus taken elsewhere closes it too.
            .onFocusChanged {
                if (!it.isFocused && barState.targetValue == SearchBarValue.Expanded) {
                    scope.launch { barState.snapTo(0f) }
                }
            }
            .then(inputAutofocus(state, autofocus)),
        enabled = isEnabled,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(painterResource(R.drawable.am_ic_search), contentDescription = null) },
        trailingIcon = if (isEnabled && hasText) { { InputClearButton(state) } } else null,
    )
}
