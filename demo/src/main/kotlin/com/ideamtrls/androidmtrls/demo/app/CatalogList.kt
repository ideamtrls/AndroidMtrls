package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMEmptyState
import com.ideamtrls.androidmtrls.AMPageHeader
import com.ideamtrls.androidmtrls.AMSectionHeader
import com.ideamtrls.androidmtrls.AMTheme
import kotlinx.coroutines.launch

/**
 * The catalog list: every entry under its category's subheader, on the page. Categories with no
 * entries don't show. The top app bar carries search and the inspection menu.
 *
 * The search action opens Material's full-screen search. While it's open, a collapsed search bar
 * stands over the top app bar, so the search opens out of it and closes back into it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CatalogList() {
    val navigator = LocalCatalogNavigator.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val searchState = rememberSearchBarState()
    val query = rememberTextFieldState()
    val scope = rememberCoroutineScope()
    var searchRequested by rememberSaveable { mutableStateOf(false) }
    val searchShowing = searchRequested ||
        searchState.currentValue == SearchBarValue.Expanded ||
        searchState.isAnimating

    LaunchedEffect(searchRequested) {
        if (!searchRequested) return@LaunchedEffect
        searchState.animateToExpanded()
        searchRequested = false
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AMPageHeader(
                    title = "AndroidMtrls",
                    actions = {
                        IconButton(onClick = { searchRequested = true }) {
                            Icon(painterResource(R.drawable.ic_search), contentDescription = "Search")
                        }
                        InspectionMenu()
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            containerColor = AMTheme.Surface.page,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { padding ->
            CatalogSections(query = "", contentPadding = padding, onOpen = navigator::open)
        }

        val inputField = @Composable {
            CatalogSearchField(
                query = query,
                searchState = searchState,
                onClose = { scope.launch { searchState.animateToCollapsed() } },
            )
        }
        if (searchShowing) {
            SearchBar(
                state = searchState,
                inputField = inputField,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        ExpandedFullScreenSearchBar(state = searchState, inputField = inputField) {
            val text = query.text.toString()
            if (text.isNotBlank() && catalogSections(text).isEmpty()) {
                AMEmptyState(
                    painterResource(R.drawable.ic_search),
                    "No results",
                    Modifier.padding(horizontal = 16.dp),
                    description = "Nothing matches “${text.trim()}”. Try a component name or an API like AMButton.",
                )
            } else {
                CatalogSections(query = text, contentPadding = PaddingValues()) { entry ->
                    // The search is a window over the catalog, so it closes before the page opens.
                    scope.launch {
                        searchState.snapTo(0f)
                        navigator.open(entry)
                    }
                }
            }
        }
    }
}

/**
 * The search field, collapsed and expanded: back closes the search (or the system back gesture);
 * clear empties the query.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogSearchField(query: TextFieldState, searchState: SearchBarState, onClose: () -> Unit) {
    SearchBarDefaults.InputField(
        textFieldState = query,
        searchBarState = searchState,
        onSearch = {},
        placeholder = { Text("Components and APIs") },
        leadingIcon = {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Close search")
            }
        },
        trailingIcon = {
            if (query.text.isNotEmpty()) {
                IconButton(onClick = { query.clearText() }) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Clear")
                }
            }
        },
    )
}

/** The entries matching [query], grouped by category in list order; empty categories left out. */
private fun catalogSections(query: String): List<Pair<CatalogCategory, List<ComponentEntry>>> =
    CatalogRegistry.all
        .filter { it.matches(query) }
        .groupBy { it.category }
        .toList()

@Composable
private fun CatalogSections(query: String, contentPadding: PaddingValues, onOpen: (ComponentEntry) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        catalogSections(query).forEach { (category, entries) ->
            item(key = category.name, contentType = "subheader") {
                CatalogSubheader(category.title)
            }
            items(entries, key = { it.id }, contentType = { "row" }) { entry ->
                CatalogRow(entry, onClick = { onOpen(entry) })
            }
        }
    }
}

/** A category's subheader, at the row inset with room above it. */
@Composable
private fun CatalogSubheader(title: String) {
    AMSectionHeader(title, Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp))
}

/** An entry's row: its glyph, full title and one-line summary. */
@Composable
private fun CatalogRow(entry: ComponentEntry, onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        leadingContent = { Icon(painterResource(entry.icon), contentDescription = null) },
        supportingContent = { Text(entry.summary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        Text(entry.title)
    }
}
