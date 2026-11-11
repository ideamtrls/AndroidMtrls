package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMAvatarSize
import com.ideamtrls.androidmtrls.AMBadge
import com.ideamtrls.androidmtrls.AMCardActionRow
import com.ideamtrls.androidmtrls.AMCardButton
import com.ideamtrls.androidmtrls.AMIconButton
import com.ideamtrls.androidmtrls.AMPageHeader
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMSkeleton
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amHoldable

/**
 * The Feed, the scene's landing view: its header (the light/dark chip and the alerts bell, which
 * pushes the alerts as a panel) over the posts, newest first, each a card. Pulling the list down
 * refreshes it. Until the first load lands, the same feed shows as shimmering placeholders under
 * a live header.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SocialFeed(model: SocialModel, postActions: SocialPostActions) {
    val spacing = AMTheme.tokens.spacing
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AMPageHeader(
                "Feed",
                actions = {
                    SceneAppearanceButton()
                    AMIconButton(
                        painterResource(R.drawable.ic_notifications),
                        "Alerts",
                        onClick = model::openAlertsPanel,
                        badge = { AMBadge(model.unreadCount) },
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = model.isRefreshing,
            onRefresh = model::refresh,
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = spacing.lg,
                    end = spacing.lg,
                    top = spacing.sm,
                    bottom = padding.calculateBottomPadding() + SocialFabClearance,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                userScrollEnabled = !model.isLoading,
            ) {
                if (model.isLoading) {
                    items(model.feed, key = { "loading-${it.id}" }) { post -> SocialPostPlaceholder(post) }
                } else {
                    items(model.feed, key = { it.id }) { post -> SocialPostCard(post, model, postActions) }
                }
            }
        }
    }
}

/**
 * One post as a card: the author's avatar (opening the profile) beside the name, handle and time,
 * the text and photo, then the reply, repost and like counts and ⋮. A tap anywhere else opens the
 * post; a press and hold, or ⋮, brings it up in the spotlight with its menu.
 */
@Composable
private fun SocialPostCard(post: SocialPost, model: SocialModel, postActions: SocialPostActions) {
    val spacing = AMTheme.tokens.spacing
    val author = model.user(post.authorId)
    val menuActions = postActions.accessibilityActions(post)
    Column(
        Modifier
            .fillMaxWidth()
            .socialCard()
            .clickable(onClickLabel = "Open post") { model.openPost(post.id) }
            .amHoldable(onHold = { model.showPostMenu(post.id) }, customActions = menuActions),
    ) {
        Row(
            Modifier.padding(start = spacing.lg, top = spacing.lg, end = spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            SocialAvatarButton(author, onClick = { model.openProfile(author.id) })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                SocialIdentityHeader(author, post.timestamp)
                SocialPostBody(post)
            }
        }
        // The counts line up under the text: the first glyph sits a text button's padding in.
        SocialPostCounts(
            post,
            model,
            onReply = { model.openPost(post.id) },
            modifier = Modifier.padding(
                start = spacing.lg + AMAvatarSize.Medium.diameter + spacing.sm - spacing.md,
                top = spacing.xs,
                end = spacing.xs,
                bottom = spacing.xs,
            ),
            more = { AMCardButton(painterResource(R.drawable.ic_more_vert), "More options", onClick = { model.showPostMenu(post.id) }) },
        )
    }
}

/**
 * A post's action row: reply (its count), repost (green while yours) and like (a red heart while
 * yours), then [more] at the end when given. Shared by the feed card and the post panel.
 */
@Composable
internal fun SocialPostCounts(
    post: SocialPost,
    model: SocialModel,
    onReply: () -> Unit,
    modifier: Modifier = Modifier,
    more: (@Composable () -> Unit)? = null,
) {
    val reposted = model.isReposted(post)
    val liked = model.isLiked(post)
    AMCardActionRow(
        modifier,
        counts = {
            AMCardButton(
                painterResource(R.drawable.ic_chat_bubble),
                "Reply",
                onClick = onReply,
                count = model.replyCount(post.id).toString(),
            )
            AMCardButton(
                painterResource(R.drawable.ic_repeat),
                if (reposted) "Reposted" else "Repost",
                onClick = { model.toggleRepost(post.id) },
                count = model.reposts(post).toString(),
                active = reposted,
                activeTint = AMSemantic.Success,
            )
            AMCardButton(
                painterResource(if (liked) R.drawable.ic_favorite_filled else R.drawable.ic_favorite),
                if (liked) "Liked" else "Like",
                onClick = { model.toggleLike(post.id) },
                count = model.likes(post).toString(),
                active = liked,
                activeTint = AMSemantic.Destructive,
            )
        },
    ) {
        more?.invoke()
    }
}

/** A post's place while the feed loads: the card's shape, its avatar, name and lines shimmering. */
@Composable
private fun SocialPostPlaceholder(post: SocialPost) {
    val spacing = AMTheme.tokens.spacing
    Row(
        Modifier.fillMaxWidth().socialCard(PaddingValues(spacing.lg)),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        AMSkeleton.Circle(diameter = AMAvatarSize.Medium.diameter)
        Column(Modifier.weight(1f)) {
            AMSkeleton.Rect(Modifier.fillMaxWidth(0.6f))
            Spacer(Modifier.height(spacing.md))
            AMSkeleton.Text(lines = (post.text.length / PlaceholderCharactersPerLine).coerceIn(2, 4))
            Spacer(Modifier.height(spacing.lg))
            AMSkeleton.Rect(Modifier.fillMaxWidth(0.5f))
        }
    }
}

/** Roughly how much of a post fits on a line of a card, to size its placeholder. */
private const val PlaceholderCharactersPerLine = 40
