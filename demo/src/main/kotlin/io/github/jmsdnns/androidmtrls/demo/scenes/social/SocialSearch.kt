package io.github.jmsdnns.androidmtrls.demo.scenes.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMAvatar
import io.github.jmsdnns.androidmtrls.content.AMAvatarSize
import io.github.jmsdnns.androidmtrls.content.AMEmptyState
import io.github.jmsdnns.androidmtrls.controls.inputs.AMSearchField
import io.github.jmsdnns.androidmtrls.lists.AMListRow
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.amRipple

/**
 * Full-height search sheet: an autofocused [AMSearchField] over a live-filtered
 * list of posts. Tapping a result closes the sheet and opens that post's detail.
 * Presented from the scene's FAB action menu. Ports iOS `SocialSearchSheet`.
 */
@Composable
fun SocialSearchSheet(state: SocialState) {
    val isDark = LocalAMIsDarkTheme.current
    val primary = AMTheme.Text.primary.resolve(isDark)
    val secondary = AMTheme.Text.secondary.resolve(isDark)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        AMCard(modifier = Modifier.fillMaxWidth()) {
            AMSearchField(
                value = state.searchQuery,
                onValueChange = { state.searchQuery = it },
                placeholder = "Search posts",
                autofocus = true,
            )
        }
        // The search field has no Cancel affordance; closing the sheet (drag-down,
        // scrim tap, or opening a result) is what resets the query — the sheet's
        // onDismiss and `openSearchResult` both blank `searchQuery` + `searching`.
        // The trailing clear button empties the field in place.

        val posts = state.searchResults
        val trimmed = state.searchQuery.trim()

        when {
            trimmed.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    AMEmptyState(
                        icon = Icons.Filled.Search,
                        title = "Search posts",
                        description = "Find posts by their text or author.",
                    )
                }
            }

            posts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    AMEmptyState(
                        icon = Icons.Filled.Search,
                        title = "No matches",
                        description = "No posts match “$trimmed”.",
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    posts.forEach { post ->
                        ResultRow(state, post, primary, secondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(
    state: SocialState,
    post: SocialPost,
    primary: Color,
    secondary: Color,
) {
    val author = state.user(post.authorId)
    val interaction = remember(post.id) { MutableInteractionSource() }

    AMListRow(
        showsChevron = true,
        modifier = Modifier.clickable(
            interactionSource = interaction,
            indication = amRipple(),
            onClick = { state.openSearchResult(post.id) },
        ),
        leading = {
            AMAvatar(url = author.avatarUrl, initials = author.initials, size = AMAvatarSize.SM)
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp),
                ) {
                    BasicText(
                        text = author.name,
                        style = AMTheme.Type.caption.toTextStyle(primary)
                            .copy(fontWeight = FontWeight.SemiBold),
                    )
                    BasicText(
                        text = author.handle,
                        style = AMTheme.Type.labelSmall.toTextStyle(secondary),
                    )
                }
                BasicText(
                    text = post.text,
                    style = AMTheme.Type.caption.toTextStyle(primary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}
