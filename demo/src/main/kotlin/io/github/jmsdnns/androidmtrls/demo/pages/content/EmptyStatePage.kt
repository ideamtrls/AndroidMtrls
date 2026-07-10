package io.github.jmsdnns.androidmtrls.demo.pages.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMEmptyState
import io.github.jmsdnns.androidmtrls.content.AMEmptyStateAction
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// EmptyState catalog entry
// ---------------------------------------------------------------------------

val emptyStateEntry: ComponentEntry = ComponentEntry(
    id = "content-empty-state",
    section = ComponentSection.Content,
    title = "Empty State",
    tileTitle = "Empty",
    subtitle = "Icon + message for empty screens",
    intro = "Centered icon + title + description for empty list/search/error states, " +
        "with an optional primary action button.",
    icon = Icons.Default.Inbox,
    content = { EmptyStatePage() },
)

// ---------------------------------------------------------------------------
// EmptyStatePage — demo composable
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMEmptyState].
 *
 * One group "Variants" with iOS's two instances:
 * 1. No items yet — no action
 * 2. No results — with Reset action
 *
 * Mirrors iOS `IMEmptyStatePage` from `Demo/Sources/Catalog/Pages/Content.swift`.
 */
@Composable
fun EmptyStatePage() {
    catalogGroup(title = "Variants") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
        ) {
            // No items yet — no action
            AMEmptyState(
                title = "No items yet",
                icon = Icons.Default.Inbox,
                description = "Pull to refresh once you've added something.",
            )

            // No results — with Reset action
            AMEmptyState(
                title = "No results",
                icon = Icons.Default.Search,
                description = "Try a different query.",
                action = AMEmptyStateAction(
                    title = "Reset",
                    icon = Icons.Default.Refresh,
                    onClick = {},
                ),
            )
        }
    }
}
