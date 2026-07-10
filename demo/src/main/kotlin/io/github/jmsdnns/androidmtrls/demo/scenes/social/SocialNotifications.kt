package io.github.jmsdnns.androidmtrls.demo.scenes.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMAvatar
import io.github.jmsdnns.androidmtrls.content.AMAvatarSize
import io.github.jmsdnns.androidmtrls.content.AMBadge
import io.github.jmsdnns.androidmtrls.content.AMBadgeContent
import io.github.jmsdnns.androidmtrls.content.AMBadgedBox
import io.github.jmsdnns.androidmtrls.content.AMPageHeader
import io.github.jmsdnns.androidmtrls.demo.scenes.common.sceneFabClearance
import io.github.jmsdnns.androidmtrls.lists.AMListRow
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.amRipple

// ---------------------------------------------------------------------------
// SocialNotificationsView — the Alerts scene root
// ---------------------------------------------------------------------------

/**
 * The Alerts view: a scene root (not a panel) of notification rows with unread
 * dot badges on the actor avatar and a trailing kind glyph. Swapped in for the
 * feed by the scene; you return to the feed through the FAB action menu, so it
 * carries no back button. Ports iOS `SocialNotificationsView`.
 */
@Composable
fun SocialNotificationsView(state: SocialState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(vertical = AMTheme.Spacing.lg.dp)
            .padding(bottom = sceneFabClearance()),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        Box(modifier = Modifier.padding(horizontal = AMTheme.Spacing.lg.dp)) {
            AMPageHeader("Alerts")
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            state.notifications.forEach { notification ->
                NotificationRow(state, notification)
            }
        }
    }
}

@Composable
private fun NotificationRow(state: SocialState, notification: SocialNotification) {
    val isDark = LocalAMIsDarkTheme.current
    val primary = AMTheme.Text.primary.resolve(isDark)
    val secondary = AMTheme.Text.secondary.resolve(isDark)
    val muted = AMTheme.Icon.muted.resolve(isDark)
    val actor = state.user(notification.actorId)
    val interaction = remember(notification.id) { MutableInteractionSource() }

    AMListRow(
        modifier = Modifier.clickable(
            interactionSource = interaction,
            indication = amRipple(),
            onClick = { state.open(notification) },
        ),
        leading = {
            if (notification.isRead) {
                AMAvatar(url = actor.avatarUrl, initials = actor.initials, size = AMAvatarSize.SM)
            } else {
                AMBadgedBox(
                    badge = { AMBadge(content = AMBadgeContent.Dot, tint = AMTheme.Semantic.info.resolve(LocalAMIsDarkTheme.current)) },
                ) {
                    AMAvatar(url = actor.avatarUrl, initials = actor.initials, size = AMAvatarSize.SM)
                }
            }
        },
        trailing = {
            Image(
                imageVector = notification.kind.glyph,
                contentDescription = null,
                colorFilter = ColorFilter.tint(muted),
                modifier = Modifier.size(16.dp),
            )
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = "${actor.name} ${notification.kind.verb}",
                    style = AMTheme.Type.caption.toTextStyle(primary).copy(
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                    ),
                )
                BasicText(
                    text = notification.timestamp,
                    style = AMTheme.Type.labelSmall.toTextStyle(secondary),
                )
            }
        },
    )
}

/** The trailing glyph for a notification kind. Mirrors iOS `SocialNotificationKind.systemImage`. */
private val SocialNotificationKind.glyph: ImageVector
    get() = when (this) {
        SocialNotificationKind.Like -> Icons.Filled.Favorite
        SocialNotificationKind.Repost -> Icons.Filled.Repeat
        SocialNotificationKind.Reply -> Icons.AutoMirrored.Filled.Comment
        SocialNotificationKind.Follow -> Icons.Filled.PersonAdd
    }
