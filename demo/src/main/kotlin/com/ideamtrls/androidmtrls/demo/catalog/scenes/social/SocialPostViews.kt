package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMAvatar
import com.ideamtrls.androidmtrls.AMAvatarSize
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMSheetSurface
import com.ideamtrls.androidmtrls.AMSurfaceFill
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

/**
 * Room every Social view leaves under its last card, so it scrolls clear of the floating action
 * button: the button's 56 dp, with `spacing.lg` under it and the same over it.
 */
internal val SocialFabClearance = 88.dp

// Surfaces

/**
 * Social's page surfaces, its cards and the profile's tabs: solid in light mode, frosted in dark
 * mode, where glass over the slate backdrop reads better than black cards.
 */
@Composable
@ReadOnlyComposable
internal fun socialFill(): AMSurfaceFill = if (AMTheme.isDark) AMSurfaceFill.Frosted else AMSurfaceFill.Solid

/** The sheets follow the cards: solid in light mode, frosted in dark mode. What they hold stays solid. */
@Composable
@ReadOnlyComposable
internal fun socialSheetSurface(): AMSheetSurface = if (AMTheme.isDark) AMSheetSurface.Frosted else AMSheetSurface.Primary

/** A card in the scene's fill for the appearance showing. */
@Composable
@ReadOnlyComposable
internal fun Modifier.socialCard(padding: PaddingValues = PaddingValues(0.dp)): Modifier =
    amCard(fill = socialFill(), padding = padding)

/**
 * The one background behind the whole scene, so it always looks like itself: a light
 * social-network blue running into a deeper one in light mode, and slate ink running into a
 * lighter slate in dark mode, from the top start corner to the bottom end corner. It's an
 * [AMBackdrop], so the scene's frosted cards and sheets blur it. Text on it uses the scheme's
 * `onSurface`.
 */
@Composable
internal fun SocialBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val dark = AMTheme.isDark
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val brush = remember(dark, rtl) {
        Brush.linearGradient(
            colors = if (dark) SocialDarkGradient else SocialLightGradient,
            start = if (rtl) Offset(Float.POSITIVE_INFINITY, 0f) else Offset.Zero,
            end = if (rtl) Offset(0f, Float.POSITIVE_INFINITY) else Offset.Infinite,
        )
    }
    AMBackdrop(
        art = { Box(Modifier.fillMaxSize().background(brush)) },
        modifier = modifier.fillMaxSize(),
        content = content,
    )
}

private val SocialLightGradient = listOf(Color(0xFFA8D6FF), Color(0xFF6BA8FA))
private val SocialDarkGradient = listOf(Color(0xFF1C2433), Color(0xFF2A3446))

// Identity

/**
 * Name, handle and time, shared by the feed card, the post menu and the post panel. On one line
 * (name, handle, then the time at the end) when it fits; otherwise, and always when [stacked], the
 * name over "handle · time". Every line is one line, ellipsized, so nothing breaks mid-word at
 * large text. TalkBack reads it as one line of text.
 */
@Composable
internal fun SocialIdentityHeader(
    author: SocialUser,
    timestamp: String,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
) {
    val spoken = "${author.name}, ${author.handle}, $timestamp"
    val described = modifier.clearAndSetSemantics { text = AnnotatedString(spoken) }
    if (stacked) {
        SocialIdentityStack(author, timestamp, described)
        return
    }
    Layout(
        contents = listOf({ SocialIdentityLine(author, timestamp) }, { SocialIdentityStack(author, timestamp) }),
        modifier = described,
    ) { (line, stack), constraints ->
        val fits = line.first().maxIntrinsicWidth(Constraints.Infinity) <= constraints.maxWidth
        val placeable = (if (fits) line else stack).first().measure(constraints)
        layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }
}

@Composable
private fun SocialIdentityLine(author: SocialUser, timestamp: String) {
    val spacing = AMTheme.tokens.spacing
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth()) {
        Text(author.name, Modifier.alignByBaseline(), style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Spacer(Modifier.width(spacing.xs))
        Text(
            author.handle,
            Modifier.alignByBaseline(),
            color = secondary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f).widthIn(min = spacing.sm))
        Text(timestamp, Modifier.alignByBaseline(), color = secondary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}

@Composable
private fun SocialIdentityStack(author: SocialUser, timestamp: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            author.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${author.handle} · $timestamp",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** An author's avatar that opens their profile, read as "Ada Lovelace profile". */
@Composable
internal fun SocialAvatarButton(
    user: SocialUser,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: AMAvatarSize = AMAvatarSize.Medium,
) {
    Box(
        modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = "${user.name} profile" },
    ) {
        AMAvatar(user.avatarUrl, user.name, size = size)
    }
}

// Post body

/**
 * A post's text and, when it has one, its photo filling the column at the post's shape, in
 * shapes.medium. Emitted as siblings so the enclosing column's spacing sets the gap.
 */
@Composable
internal fun SocialPostBody(post: SocialPost) {
    Text(post.text, style = MaterialTheme.typography.bodyLarge)
    if (post.imageUrl != null) {
        AMRemoteImage(
            post.imageUrl,
            contentDescription = post.imageDescription,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(post.imageAspectRatio)
                .clip(MaterialTheme.shapes.medium),
        )
    }
}

// Post row

/**
 * A post as a tappable list row: the author's avatar and name over two lines of the text, or,
 * without the author, the text over "2h · Chess". Bare, for a card of rows.
 */
@Composable
internal fun SocialPostRow(
    post: SocialPost,
    model: SocialModel,
    onClick: () -> Unit,
    showsAuthor: Boolean = true,
) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    if (showsAuthor) {
        val author = model.user(post.authorId)
        AMListRow(
            leadingContent = {
                AMAvatar(author.avatarUrl, author.name, Modifier.clearAndSetSemantics {}, size = AMAvatarSize.Small)
            },
            supportingContent = {
                Text(post.text, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            onClick = onClick,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xs)) {
                Text(
                    author.name,
                    Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    author.handle,
                    Modifier.alignByBaseline(),
                    color = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    } else {
        AMListRow(
            supportingContent = {
                Text(
                    listOfNotNull(post.timestamp, post.topic?.title).joinToString(" · "),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            onClick = onClick,
        ) {
            Text(post.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
