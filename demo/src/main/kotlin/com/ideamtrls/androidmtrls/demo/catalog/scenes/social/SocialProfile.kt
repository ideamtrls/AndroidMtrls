package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ideamtrls.androidmtrls.AMAvatar
import com.ideamtrls.androidmtrls.AMAvatarSize
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMButtonStyle
import com.ideamtrls.androidmtrls.AMEmptyState
import com.ideamtrls.androidmtrls.AMLabeledContent
import com.ideamtrls.androidmtrls.AMPanel
import com.ideamtrls.androidmtrls.AMTabs
import com.ideamtrls.androidmtrls.AMTheme
import java.text.NumberFormat

/** The profile's tabs. */
private enum class SocialProfileTab { Posts, About }

/**
 * A person's profile panel over the scene's backdrop: the header card (avatar, name, handle, bio,
 * the follower, following and post counts, and Follow for anyone but you) over the Posts and About
 * tabs. Opens from any avatar, a follow alert, or the remote's Profile tile.
 */
@Composable
internal fun SocialProfile(model: SocialModel, userId: String, onBack: () -> Unit) {
    val spacing = AMTheme.tokens.spacing
    val user = model.user(userId)
    var selectedTab by rememberSaveable { mutableStateOf(SocialProfileTab.Posts) }
    SocialBackdrop {
        AMPanel(user.name, onBack = onBack, containerColor = Color.Transparent) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = SocialFabClearance),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                SocialProfileHeader(user, model)
                AMTabs(selectedTab, onSelectionChange = { selectedTab = it }, fill = socialFill()) {
                    tab(SocialProfileTab.Posts, "Posts") { SocialProfilePosts(user, model) }
                    tab(SocialProfileTab.About, "About") { SocialProfileAbout(user) }
                }
            }
        }
    }
}

@Composable
private fun SocialProfileHeader(user: SocialUser, model: SocialModel) {
    val spacing = AMTheme.tokens.spacing
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.fillMaxWidth().socialCard(PaddingValues(spacing.lg)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        AMAvatar(user.avatarUrl, user.name, Modifier.clearAndSetSemantics {}, size = AMAvatarSize.Large)
        Text(
            user.name,
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(user.handle, color = secondary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        Text(user.bio, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Row(
            Modifier.padding(top = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            SocialStat(model.followerCount(user), "Followers")
            SocialStat(user.following, "Following")
            SocialStat(model.posts(user.id).size, "Posts")
        }
        if (!user.isSelf) {
            val following = model.isFollowed(user.id)
            AMButton(
                if (following) "Following" else "Follow",
                onClick = { model.toggleFollow(user.id) },
                modifier = Modifier.fillMaxWidth().padding(top = spacing.xs),
                style = if (following) AMButtonStyle.Secondary() else AMButtonStyle.Primary(),
            )
        }
    }
}

/** A count over its name, read as one element: "842 Followers". */
@Composable
private fun SocialStat(value: Int, label: String) {
    val formatted = NumberFormat.getIntegerInstance().format(value)
    Column(
        Modifier.clearAndSetSemantics { contentDescription = "$formatted $label" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(formatted, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

/** The person's posts as rows that open the post, or a note that there are none yet. */
@Composable
private fun SocialProfilePosts(user: SocialUser, model: SocialModel) {
    val posts = model.posts(user.id)
    if (posts.isEmpty()) {
        AMEmptyState(
            painterResource(R.drawable.ic_chat_bubble),
            "No posts yet",
            description = if (user.isSelf) "Posts you share show up here." else "When ${user.firstName} posts, it shows up here.",
        )
        return
    }
    Column(Modifier.padding(top = AMTheme.tokens.spacing.md).fillMaxWidth().socialCard()) {
        posts.forEach { post ->
            SocialPostRow(post, model, onClick = { model.openPost(post.id) }, showsAuthor = false)
        }
    }
}

/** When they joined, where they are, and their website. */
@Composable
private fun SocialProfileAbout(user: SocialUser) {
    Column(Modifier.padding(top = AMTheme.tokens.spacing.md).fillMaxWidth().socialCard()) {
        AMLabeledContent("Joined", user.joined)
        AMLabeledContent("Location", user.location)
        AMLabeledContent("Website", user.website)
    }
}
