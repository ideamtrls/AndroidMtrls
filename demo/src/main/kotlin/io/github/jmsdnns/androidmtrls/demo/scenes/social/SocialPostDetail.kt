package io.github.jmsdnns.androidmtrls.demo.scenes.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMAvatar
import io.github.jmsdnns.androidmtrls.content.AMAvatarSize
import io.github.jmsdnns.androidmtrls.content.AMSectionHeader
import io.github.jmsdnns.androidmtrls.controls.buttons.AMCardActionRow
import io.github.jmsdnns.androidmtrls.controls.buttons.AMCardButton
import io.github.jmsdnns.androidmtrls.demo.scenes.common.sceneFabClearance
import io.github.jmsdnns.androidmtrls.lists.AMListRow
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import java.util.UUID

// ---------------------------------------------------------------------------
// SocialPostDetailView — the post-detail panel body
// ---------------------------------------------------------------------------

/**
 * Content for the post-detail panel: the full post on a flat, full-bleed
 * [AMTheme.Surface.primary] band with a working reply/repost/like row, then the
 * flat reply thread below. Tapping Reply opens the compose sheet in reply mode.
 * Ports iOS `SocialPostDetailView`.
 *
 * The post is looked up live by [postId] so likes/replies update; the captured
 * id means the panel never blanks while the pop runs. Rendered by the scene's
 * `ScenePanelHost` panel slot beneath a transparent [ScenePanelChrome] over the
 * full-bleed `SocialBackdrop`.
 */
@Composable
fun SocialPostDetailView(state: SocialState, postId: UUID) {
    val post = state.post(postId) ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = sceneFabClearance()),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        PostBlock(state, post)

        AMSectionHeaderInset("Replies")

        if (state.repliesFor(post.id).isEmpty()) {
            // A tight inline caption (not AMEmptyState): a one-line nudge directly
            // under the post, where a full centered empty-state block would be too heavy.
            val secondary = AMTheme.Text.secondary.resolve(LocalAMIsDarkTheme.current)
            BasicText(
                text = "No replies yet — add the first one.",
                style = AMTheme.Type.caption.toTextStyle(secondary),
                modifier = Modifier.padding(horizontal = AMTheme.Spacing.lg.dp),
            )
        } else {
            ReplyList(state, post)
        }
    }
}

@Composable
private fun AMSectionHeaderInset(title: String) {
    Box(modifier = Modifier.padding(horizontal = AMTheme.Spacing.lg.dp)) {
        AMSectionHeader(title)
    }
}

@Composable
private fun PostBlock(state: SocialState, post: SocialPost) {
    val isDark = LocalAMIsDarkTheme.current
    val author = state.user(post.authorId)
    val avatarInteraction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.primary.resolve(isDark))
            .padding(horizontal = AMTheme.Spacing.lg.dp, vertical = AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = avatarInteraction,
                        indication = amRipple(),
                        onClick = { state.openProfile(author.id) },
                    ),
            ) {
                AMAvatar(
                    url = author.avatarUrl,
                    initials = author.initials,
                    size = AMAvatarSize.MD,
                    contentDescription = "${author.name} profile",
                )
            }
            PostIdentityHeader(author = author, timestamp = post.timestamp, style = PostHeaderStyle.Detail)
            Spacer(modifier = Modifier.weight(1f))
        }

        postBodyContent(
            post = post,
            textStyle = AMTheme.Type.body.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            imageSize = 300.dp,
        )

        // Detail action row: reply (opens compose in reply mode), repost, like —
        // no share / more (those live on the feed card).
        AMCardActionRow {
            AMCardButton(
                icon = Icons.AutoMirrored.Filled.Comment,
                label = "${state.replyCount(post.id)}",
                accessibilityLabel = "Reply",
                onClick = { state.beginReply(post.id) },
            )
            AMCardButton(
                icon = Icons.Filled.Repeat,
                label = "${post.reposts}",
                active = post.repostedByMe,
                activeTint = AMTheme.Semantic.success.resolve(LocalAMIsDarkTheme.current),
                accessibilityLabel = "Repost",
                onClick = { state.toggleRepost(post.id) },
            )
            AMCardButton(
                icon = if (post.likedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = "${post.likes}",
                active = post.likedByMe,
                activeTint = AMTheme.Semantic.destructive.resolve(LocalAMIsDarkTheme.current),
                accessibilityLabel = "Like",
                onClick = { state.toggleLike(post.id) },
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReplyList(state: SocialState, post: SocialPost) {
    val isDark = LocalAMIsDarkTheme.current
    val primary = AMTheme.Text.primary.resolve(isDark)
    val secondary = AMTheme.Text.secondary.resolve(isDark)

    Column(modifier = Modifier.fillMaxWidth()) {
        state.repliesFor(post.id).forEach { reply ->
            val author = state.user(reply.authorId)
            val avatarInteraction = remember(reply.id) { MutableInteractionSource() }

            AMListRow(
                leading = {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = avatarInteraction,
                                indication = amRipple(),
                                onClick = { state.openProfile(author.id) },
                            ),
                    ) {
                        AMAvatar(
                            url = author.avatarUrl,
                            initials = author.initials,
                            size = AMAvatarSize.SM,
                            contentDescription = "${author.name} profile",
                        )
                    }
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
                                text = reply.timestamp,
                                style = AMTheme.Type.labelSmall.toTextStyle(secondary),
                            )
                        }
                        BasicText(
                            text = reply.text,
                            style = AMTheme.Type.caption.toTextStyle(primary),
                        )
                    }
                },
            )
        }
    }
}
