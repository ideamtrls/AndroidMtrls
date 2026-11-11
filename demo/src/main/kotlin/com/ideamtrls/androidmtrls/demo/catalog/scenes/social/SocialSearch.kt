package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMChipGroup
import com.ideamtrls.androidmtrls.AMEmptyState
import com.ideamtrls.androidmtrls.AMSearchField
import com.ideamtrls.androidmtrls.AMTextFieldAutofocus
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

/**
 * The search sheet's content, under its "Search" title: the search field (focused as the sheet
 * opens) in a card, the topic chips (any number), then the matching posts in a card of rows, each
 * closing the sheet and opening its post. Before there's anything to search, and when nothing
 * matches, an empty state says so.
 */
@Composable
internal fun ColumnScope.SocialSearchSheet(model: SocialModel) {
    val spacing = AMTheme.tokens.spacing
    // The results scroll edge to edge, so their card's shadow isn't cut at the sides; the rest sits in the gutter.
    Column(
        Modifier
            .fillMaxWidth()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AMSearchField(
            model.searchQuery,
            Modifier
                .padding(horizontal = spacing.lg)
                .fillMaxWidth()
                .amCard(padding = PaddingValues(spacing.lg)),
            placeholder = "Search posts and people",
            autofocus = AMTextFieldAutofocus.CaretAtEnd,
        )
        AMChipGroup(
            SocialTopic.entries,
            label = { it.title },
            selection = model.searchTopics,
            onSelectionChange = { model.searchTopics = it },
            modifier = Modifier.padding(horizontal = spacing.lg),
        )
        SocialSearchResults(model)
    }
}

@Composable
private fun ColumnScope.SocialSearchResults(model: SocialModel) {
    val spacing = AMTheme.tokens.spacing
    val results = model.searchResults
    val search = painterResource(R.drawable.ic_search)
    when {
        model.isSearchIdle -> AMEmptyState(
            search,
            "Search posts",
            modifier = Modifier.padding(horizontal = spacing.lg),
            description = "Find posts by what they say, who wrote them, or their topic.",
        )
        results.isEmpty() -> AMEmptyState(
            search,
            "No matches",
            modifier = Modifier.padding(horizontal = spacing.lg),
            description = model.trimmedQuery.let { query ->
                if (query.isEmpty()) "No posts in these topics yet." else "No posts match “$query”."
            },
        )
        else -> Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.lg),
        ) {
            Column(Modifier.fillMaxWidth().amCard()) {
                results.forEach { post ->
                    SocialPostRow(post, model, onClick = { model.openSearchResult(post.id) })
                }
            }
        }
    }
}
