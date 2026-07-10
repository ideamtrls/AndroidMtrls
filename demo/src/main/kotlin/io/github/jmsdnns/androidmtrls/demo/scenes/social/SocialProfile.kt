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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.collections.AMTab
import io.github.jmsdnns.androidmtrls.collections.AMTabs
import io.github.jmsdnns.androidmtrls.content.AMAvatar
import io.github.jmsdnns.androidmtrls.content.AMAvatarSize
import io.github.jmsdnns.androidmtrls.content.AMEmptyState
import io.github.jmsdnns.androidmtrls.content.AMRowContent
import io.github.jmsdnns.androidmtrls.controls.buttons.AMPrimaryButton
import io.github.jmsdnns.androidmtrls.demo.scenes.common.sceneFabClearance
import io.github.jmsdnns.androidmtrls.lists.AMListRow
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.amRipple

// ---------------------------------------------------------------------------
// SocialProfileView — the profile panel body
// ---------------------------------------------------------------------------

/**
 * Content for the profile panel: an identity header card (avatar, name, handle,
 * bio, follower/following/posts stats, Follow button) over Posts / About tabs.
 * Opens from any avatar tap (feed, detail, alerts). Ports iOS `SocialProfileView`.
 *
 * Rendered by the scene's `ScenePanelHost` panel slot beneath a transparent
 * [ScenePanelChrome]; the panel supplies its own `Surface.page` fill.
 */
@Composable
fun SocialProfileView(state: SocialState, userId: String) {
    val user = state.user(userId)
    var tab by rememberSaveable(userId) { mutableStateOf("posts") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = AMTheme.Spacing.md.dp)
            .padding(bottom = sceneFabClearance()),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        HeaderBlock(state, user)

        AMTabs(
            tabs = listOf(
                AMTab(id = "posts", label = "Posts") { PostsTab(state, user) },
                AMTab(id = "about", label = "About") { AboutTab(user) },
            ),
            selection = tab,
            onSelectionChange = { tab = it },
            modifier = Modifier.padding(horizontal = AMTheme.Spacing.lg.dp),
        )
    }
}

@Composable
private fun HeaderBlock(state: SocialState, user: SocialUser) {
    val isDark = LocalAMIsDarkTheme.current
    val primary = AMTheme.Text.primary.resolve(isDark)
    val secondary = AMTheme.Text.secondary.resolve(isDark)

    Box(modifier = Modifier.padding(horizontal = AMTheme.Spacing.lg.dp)) {
        AMCard(modifier = Modifier.fillMaxWidth(), padding = AMPadding.block) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
            ) {
                AMAvatar(url = user.avatarUrl, initials = user.initials, size = AMAvatarSize.LG)

                BasicText(
                    text = user.name,
                    style = AMTheme.Type.title.toTextStyle(primary)
                        .copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                )
                BasicText(text = user.handle, style = AMTheme.Type.secondary.toTextStyle(secondary))
                BasicText(
                    text = user.bio,
                    style = AMTheme.Type.caption.toTextStyle(primary).copy(textAlign = TextAlign.Center),
                )

                Row(
                    modifier = Modifier.padding(top = AMTheme.Spacing.xs.dp),
                    horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xl.dp),
                ) {
                    Stat("${user.followers}", "Followers")
                    Stat("${user.following}", "Following")
                    Stat("${state.postsBy(user.id).size}", "Posts")
                }

                if (!user.isSelf) {
                    AMPrimaryButton(
                        label = if (user.isFollowed) "Following" else "Follow",
                        onClick = { state.toggleFollow(user.id) },
                        modifier = Modifier.padding(top = AMTheme.Spacing.xs.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    val isDark = LocalAMIsDarkTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText(
            text = value,
            style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.primary.resolve(isDark))
                .copy(fontWeight = FontWeight.SemiBold),
        )
        BasicText(
            text = label,
            style = AMTheme.Type.labelSmall.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
        )
    }
}

@Composable
private fun PostsTab(state: SocialState, user: SocialUser) {
    val isDark = LocalAMIsDarkTheme.current
    val primary = AMTheme.Text.primary.resolve(isDark)
    val secondary = AMTheme.Text.secondary.resolve(isDark)
    val userPosts = state.postsBy(user.id)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AMTheme.Spacing.sm.dp),
    ) {
        if (userPosts.isEmpty()) {
            AMEmptyState(
                icon = Icons.AutoMirrored.Filled.Chat,
                title = "No posts yet",
                description = "When ${user.name} posts, it'll show up here.",
            )
        } else {
            userPosts.forEach { post ->
                val interaction = remember(post.id) { MutableInteractionSource() }
                AMListRow(
                    showsChevron = true,
                    modifier = Modifier.clickable(
                        interactionSource = interaction,
                        indication = amRipple(),
                        onClick = {
                            // Replace the stack: pop this profile and push the detail.
                            state.path.clear()
                            state.path.add(SocialRoute.PostDetail(post.id))
                        },
                    ),
                    content = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            BasicText(
                                text = post.text,
                                style = AMTheme.Type.caption.toTextStyle(primary),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            BasicText(
                                text = post.timestamp,
                                style = AMTheme.Type.labelSmall.toTextStyle(secondary),
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AboutTab(user: SocialUser) {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AMTheme.Spacing.sm.dp),
    ) {
        AMRowContent(label = "Joined", value = user.joined)
        AMRowContent(label = "Location", value = user.location)
        // Link value tinted info — needs the slot overload to override the color.
        AMRowContent(
            label = {
                BasicText(
                    text = "Link",
                    style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                )
            },
            content = {
                BasicText(
                    text = user.link,
                    style = AMTheme.Type.body.toTextStyle(AMTheme.Semantic.info.resolve(LocalAMIsDarkTheme.current)),
                )
            },
        )
    }
}
