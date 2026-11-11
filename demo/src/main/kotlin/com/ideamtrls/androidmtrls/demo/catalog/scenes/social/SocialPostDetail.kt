package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.ideamtrls.androidmtrls.AMAvatarSize
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMPanel
import com.ideamtrls.androidmtrls.AMSection
import com.ideamtrls.androidmtrls.AMSectionHeader
import com.ideamtrls.androidmtrls.AMSectionStyle
import com.ideamtrls.androidmtrls.AMTag
import com.ideamtrls.androidmtrls.AMTheme

/**
 * The post's panel over the scene's backdrop: the post in a card (the author, the text and photo,
 * its topic as a tag, and reply, repost and like), then its replies in a card under "Replies", or a
 * line saying there are none. Reply opens the compose sheet in reply mode. The post is looked up
 * live, so likes and replies update; a deleted post draws nothing while its panel leaves.
 */
@Composable
internal fun SocialPostDetail(model: SocialModel, postId: String, onBack: () -> Unit) {
    val spacing = AMTheme.tokens.spacing
    SocialBackdrop {
        AMPanel("Post", onBack = onBack, containerColor = Color.Transparent) { padding ->
            val post = model.post(postId) ?: return@AMPanel
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = SocialFabClearance),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                SocialPostDetailCard(post, model)
                SocialReplies(post, model)
            }
        }
    }
}

@Composable
private fun SocialPostDetailCard(post: SocialPost, model: SocialModel) {
    val spacing = AMTheme.tokens.spacing
    val author = model.user(post.authorId)
    Column(Modifier.fillMaxWidth().socialCard()) {
        Column(
            Modifier.padding(start = spacing.lg, top = spacing.lg, end = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                SocialAvatarButton(author, onClick = { model.openProfile(author.id) })
                SocialIdentityHeader(author, post.timestamp, Modifier.weight(1f), stacked = true)
            }
            SocialPostBody(post)
            post.topic?.let { AMTag(it.title) }
        }
        SocialPostCounts(
            post,
            model,
            onReply = { model.beginReply(post.id) },
            modifier = Modifier.padding(start = spacing.xs, top = spacing.xs, end = spacing.xs, bottom = spacing.xs),
        )
    }
}

/** The replies in a card of rows under their header, or a quiet line when there are none. */
@Composable
private fun SocialReplies(post: SocialPost, model: SocialModel) {
    val replies = model.replies(post.id)
    if (replies.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.sm)) {
            AMSectionHeader("Replies")
            Text("No replies yet.", style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    AMSection("Replies", style = AMSectionStyle.Rows, fill = socialFill(), inset = false) {
        replies.forEach { reply ->
            val author = model.user(reply.authorId)
            AMListRow(
                leadingContent = {
                    SocialAvatarButton(author, onClick = { model.openProfile(author.id) }, size = AMAvatarSize.Small)
                },
                supportingContent = { Text(reply.text, color = MaterialTheme.colorScheme.onSurface) },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xs)) {
                    Text(
                        author.name,
                        Modifier.alignByBaseline().weight(1f, fill = false),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        reply.timestamp,
                        Modifier.alignByBaseline(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
