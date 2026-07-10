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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMAvatar
import io.github.jmsdnns.androidmtrls.content.AMAvatarSize
import io.github.jmsdnns.androidmtrls.content.AMBadgeContent
import io.github.jmsdnns.androidmtrls.content.AMPageHeader
import io.github.jmsdnns.androidmtrls.content.AMRemoteImage
import io.github.jmsdnns.androidmtrls.content.AMSkeleton
import io.github.jmsdnns.androidmtrls.content.AMSpinner
import io.github.jmsdnns.androidmtrls.content.amShimmer
import io.github.jmsdnns.androidmtrls.controls.buttons.AMCardActionRow
import io.github.jmsdnns.androidmtrls.controls.buttons.AMCardButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButton
import io.github.jmsdnns.androidmtrls.demo.scenes.common.sceneFabClearance
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// SocialFeedView — the main feed root
// ---------------------------------------------------------------------------

/**
 * The main feed: page header with notification bell, skeleton first-load,
 * drag-to-refresh, and interactive post cards. Ports iOS `SocialFeedView`.
 *
 * First-load skeleton renders four redacted [PostCardSkeleton]s — real [AMCard]
 * chrome around neutral shimmering bars in the post silhouette — the Compose
 * twin of iOS's `.redacted(.placeholder)` PostCards. Pull-to-refresh is a simple
 * overscroll drag (the
 * spec-accepted Android tradeoff for iOS's `.refreshable`): dragging the feed
 * down past the threshold at the top runs [SocialState.refresh].
 */
@Composable
fun SocialFeedView(state: SocialState) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { 72.dp.toPx() }

    var refreshing by remember { mutableStateOf(false) }
    var pullPx by remember { mutableFloatStateOf(0f) }

    // Overscroll-drag pull-to-refresh: accumulate downward drag when the scroll
    // is already at the top (onPostScroll leftover), shrink it first on upward
    // drag (onPreScroll), and fire on release (onPreFling) past the threshold.
    val nestedConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0f && pullPx > 0f) {
                    val newPull = (pullPx + available.y).coerceAtLeast(0f)
                    val consumed = newPull - pullPx
                    pullPx = newPull
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!refreshing && source == NestedScrollSource.UserInput && available.y > 0f) {
                    // Damp the pull so the rubber-band lags the finger.
                    pullPx += available.y * 0.5f
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!refreshing && pullPx > thresholdPx) {
                    refreshing = true
                    scope.launch {
                        state.refresh()
                        refreshing = false
                    }
                }
                pullPx = 0f
                return Velocity.Zero
            }
        }
    }

    // Kick off the one-shot first load (1200ms) on first appearance.
    LaunchedEffect(Unit) { state.loadIfNeeded() }

    val contentShift = if (refreshing) thresholdPx else pullPx

    Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedConnection)) {
        // Pull indicator revealed as the content slides down beneath it.
        if (refreshing || pullPx > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = AMTheme.Spacing.md.dp),
                contentAlignment = Alignment.Center,
            ) {
                AMSpinner(compact = true)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, contentShift.roundToInt()) }
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(horizontal = AMTheme.Spacing.lg.dp)
                .padding(top = AMTheme.Spacing.lg.dp, bottom = sceneFabClearance()),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            FeedHeader(state)

            if (state.isLoading) {
                // Redacted first-load skeleton: real AMCard chrome wrapping neutral
                // separator bars in the PostCard silhouette, one amShimmer sweep per
                // card. Mirrors iOS's four `.redacted(.placeholder)` + shimmered
                // PostCards — the card reads as inert bars, never as live copy.
                // Varied body lengths (2–4 bars) keep the loading column from
                // reading uniform, the twin of iOS `socialPlaceholderPosts`.
                repeat(4) { i ->
                    PostCardSkeleton(bodyLines = 2 + i % 3)
                }
            } else {
                state.visibleFeed.forEach { post ->
                    PostCard(post = post, state = state)
                }
            }
        }
    }
}

@Composable
private fun FeedHeader(state: SocialState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            AMPageHeader("Feed")
        }
        AMIconButton(
            icon = Icons.Filled.Notifications,
            badge = if (state.unreadCount > 0) AMBadgeContent.Count(state.unreadCount) else null,
            contentDescription = "Notifications",
            onClick = { state.currentView = SocialView.Alerts },
        )
    }
}

// ---------------------------------------------------------------------------
// Shared post views — reused by the feed, post-menu preview, and detail block
// ---------------------------------------------------------------------------

/** Layout style for [PostIdentityHeader]. Mirrors iOS `PostIdentityHeader.Style`. */
internal enum class PostHeaderStyle { Compact, Detail }

/**
 * Name + handle + timestamp cluster, reused by the feed card, the post-menu
 * preview, and the post-detail block. [PostHeaderStyle.Compact] is the inline
 * feed/preview header; [PostHeaderStyle.Detail] stacks "handle · timestamp" under
 * the name in the panel. Ports iOS `PostIdentityHeader`.
 */
@Composable
internal fun PostIdentityHeader(
    author: SocialUser,
    timestamp: String,
    style: PostHeaderStyle = PostHeaderStyle.Compact,
) {
    val isDark = LocalAMIsDarkTheme.current
    val primary = AMTheme.Text.primary.resolve(isDark)
    val secondary = AMTheme.Text.secondary.resolve(isDark)

    when (style) {
        PostHeaderStyle.Compact -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp),
            ) {
                BasicText(
                    text = author.name,
                    style = AMTheme.Type.secondary.toTextStyle(primary)
                        .copy(fontWeight = FontWeight.SemiBold),
                )
                BasicText(text = author.handle, style = AMTheme.Type.secondary.toTextStyle(secondary))
                Spacer(modifier = Modifier.weight(1f))
                BasicText(text = timestamp, style = AMTheme.Type.secondary.toTextStyle(secondary))
            }
        }

        PostHeaderStyle.Detail -> {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = author.name,
                    style = AMTheme.Type.secondary.toTextStyle(primary)
                        .copy(fontWeight = FontWeight.SemiBold),
                )
                BasicText(
                    text = "${author.handle} · $timestamp",
                    style = AMTheme.Type.caption.toTextStyle(secondary),
                )
            }
        }
    }
}

/**
 * A post's text plus its optional image, emitted as a flat pair so the enclosing
 * Column's own `spacedBy` sets the text→image gap (sm in the feed/preview, md in
 * the detail). Shared by the feed card, post-menu preview, and detail block.
 * Ports iOS `postBodyContent`.
 */
@Composable
internal fun postBodyContent(
    post: SocialPost,
    textStyle: TextStyle,
    imageSize: Dp,
) {
    BasicText(text = post.text, style = textStyle, modifier = Modifier.fillMaxWidth())

    val url = post.imageUrl
    if (url != null) {
        AMRemoteImage(
            url = url,
            size = imageSize,
            cornerRadius = AMTheme.Radius.md.dp,
            contentDescription = null,
            fallback = {
                AMSkeleton.rect(
                    width = imageSize,
                    height = imageSize,
                    cornerRadius = AMTheme.Radius.md.dp,
                )
            },
        )
    }
}

/**
 * One `AMTheme.separator`-filled capsule bar — the redaction primitive for
 * [PostCardSkeleton]. Drawn plain (no `amShimmer` of its own) so the parent's
 * single sweep tracks the whole card silhouette; mirrors the "real placeholder
 * tree under one sweep" pattern documented on `amShimmer`.
 */
@Composable
private fun SkeletonBar(modifier: Modifier) {
    val base = AMTheme.separator.resolve(LocalAMIsDarkTheme.current)
    Box(modifier = modifier.background(base, RoundedCornerShape(50)))
}

/**
 * First-load skeleton for one feed card: the real [AMCard] chrome around neutral
 * [SkeletonBar]s laid out in the [PostCard] silhouette — avatar circle, an
 * identity line, [bodyLines] body bars, and a three-glyph action-row cluster —
 * all under one [amShimmer] sweep. The Compose twin of iOS's
 * `.redacted(reason: .placeholder)` PostCard: the card shape and lift stay, its
 * content reads as inert bars rather than live copy.
 *
 * @param bodyLines How many body bars to draw (2–4), varied per card so the
 *   loading column isn't uniform.
 */
@Composable
private fun PostCardSkeleton(bodyLines: Int) {
    val base = AMTheme.separator.resolve(LocalAMIsDarkTheme.current)

    Box(modifier = Modifier.fillMaxWidth().amShimmer()) {
        AMCard(modifier = Modifier.fillMaxWidth(), padding = AMPadding.block) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // Avatar placeholder — same 44dp MD circle the live card uses.
                Box(
                    modifier = Modifier
                        .size(AMAvatarSize.MD.diameterDp)
                        .background(base, CircleShape),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
                ) {
                    // Identity line (name + handle + timestamp collapsed to one bar).
                    SkeletonBar(Modifier.fillMaxWidth(0.5f).height(12.dp))
                    // Body bars — last one short, like a paragraph tail.
                    repeat(bodyLines) { i ->
                        val isLast = i == bodyLines - 1
                        SkeletonBar(Modifier.fillMaxWidth(if (isLast) 0.55f else 1f).height(11.dp))
                    }
                    // Action-row cluster — three small glyph bars.
                    Row(horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp)) {
                        repeat(3) {
                            SkeletonBar(Modifier.width(28.dp).height(11.dp))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PostCard — one feed card
// ---------------------------------------------------------------------------

/**
 * A single feed post card: avatar (tap → profile), identity + body (tap card →
 * detail), and the reply/repost/like/share/more action row. Ports iOS `PostCard`.
 * (The first-load skeleton is the separate [PostCardSkeleton], never this card.)
 */
@Composable
internal fun PostCard(
    post: SocialPost,
    state: SocialState,
) {
    val tokens = LocalAMTokens.current
    val author = state.user(post.authorId)
    val avatarInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    val cardModifier = Modifier
        .fillMaxWidth()
        // Clip to the card's corner radius so the monochrome ripple sweep
        // bounds to the AMCard shape (the card applies the caller modifier
        // before its own rounded fill).
        .clip(RoundedCornerShape(tokens.radius.lg))
        .clickable(
            interactionSource = cardInteraction,
            indication = amRipple(),
            onClick = { state.openPost(post.id) },
        )

    AMCard(modifier = cardModifier, padding = AMPadding.block) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
            verticalAlignment = Alignment.Top,
        ) {
            val avatarModifier = Modifier
                .clip(CircleShape)
                .clickable(
                    interactionSource = avatarInteraction,
                    indication = amRipple(),
                    onClick = { state.openProfile(author.id) },
                )
            Box(modifier = avatarModifier) {
                AMAvatar(
                    url = author.avatarUrl,
                    initials = author.initials,
                    size = AMAvatarSize.MD,
                    contentDescription = "${author.name} profile",
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
            ) {
                PostIdentityHeader(author = author, timestamp = post.timestamp)
                postBodyContent(
                    post = post,
                    // Feed body = iOS `.subheadline`. Rendered at the `body` (16sp)
                    // token so Roboto reads at the same visual weight as SF Pro's
                    // 15pt subheadline (14sp Roboto reads noticeably smaller and
                    // cramped against the card); the detail block stays `body` too.
                    textStyle = AMTheme.Type.body
                        .toTextStyle(AMTheme.Text.primary.resolve(LocalAMIsDarkTheme.current)),
                    imageSize = 240.dp,
                )
                PostActionRow(post = post, state = state)
            }
        }
    }
}

/**
 * The reply / repost / like / share / more action row shared by the feed card
 * and the post-detail block (the detail passes its own trailing layout). Repost
 * tints success when active; like tints destructive. Ports iOS `PostCard.actionRow`.
 */
@Composable
internal fun PostActionRow(post: SocialPost, state: SocialState) {
    AMCardActionRow {
        AMCardButton(
            icon = Icons.AutoMirrored.Filled.Comment,
            label = "${state.replyCount(post.id)}",
            accessibilityLabel = "Reply",
            onClick = { state.openPost(post.id) },
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
        AMCardButton(
            icon = Icons.Filled.Share,
            accessibilityLabel = "Share",
            onClick = {},
        )
        AMCardButton(
            icon = Icons.Filled.MoreHoriz,
            accessibilityLabel = "More",
            onClick = { state.postMenuId = post.id },
        )
    }
}
