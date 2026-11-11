package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.ideamtrls.androidmtrls.AMAvatar
import com.ideamtrls.androidmtrls.AMAvatarSize
import com.ideamtrls.androidmtrls.AMBadgedBox
import com.ideamtrls.androidmtrls.AMEmptyState
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMPageHeader
import com.ideamtrls.androidmtrls.AMPanel
import com.ideamtrls.androidmtrls.AMSegmentedControl
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMSwipeAction
import com.ideamtrls.androidmtrls.AMSwipeableRow
import com.ideamtrls.androidmtrls.AMTheme

/**
 * The Alerts root view, which the remote's Alerts tile swaps in for the feed: its header over the
 * All / Mentions control and the alert rows. It carries no back button; back swaps to the feed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SocialAlerts(model: SocialModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { AMPageHeader("Alerts", scrollBehavior = scrollBehavior) },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { padding -> SocialAlertsBody(model, padding) }
}

/** The alerts as a panel over the feed, pushed by its bell: the panel's bar titles it, and back returns to the feed. */
@Composable
internal fun SocialAlertsPanel(model: SocialModel, onBack: () -> Unit) {
    SocialBackdrop {
        AMPanel("Alerts", onBack = onBack, containerColor = Color.Transparent) { padding ->
            SocialAlertsBody(model, padding)
        }
    }
}

/** The All / Mentions control over a card of alert rows, or an empty state on the backdrop. */
@Composable
private fun SocialAlertsBody(model: SocialModel, padding: PaddingValues) {
    val spacing = AMTheme.tokens.spacing
    val notifications = model.shownNotifications
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = SocialFabClearance),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AMSegmentedControl(
            SocialAlertFilter.entries,
            label = { it.title },
            selected = model.alertFilter,
            onSelectedChange = { model.alertFilter = it },
            modifier = Modifier.fillMaxWidth(),
        )
        if (notifications.isEmpty()) {
            when (model.alertFilter) {
                SocialAlertFilter.All -> AMEmptyState(
                    painterResource(R.drawable.ic_notifications),
                    "No alerts",
                    description = "Likes, replies, and new followers show up here.",
                )
                SocialAlertFilter.Mentions -> AMEmptyState(
                    painterResource(R.drawable.ic_alternate_email),
                    "No mentions",
                    description = "When someone mentions or replies to you, it shows up here.",
                )
            }
        } else {
            Column(Modifier.fillMaxWidth().socialCard()) {
                notifications.forEach { notification ->
                    key(notification.id) { SocialAlertRow(notification, model) }
                }
            }
        }
    }
}

/**
 * One alert: the sender's avatar (with a dot while unread), what they did over when (and the words
 * that mention or reply to you), and the kind's glyph. A tap opens it. A full swipe toward the start
 * marks it read or unread; toward the end it mutes the sender, which hides their alerts.
 */
@Composable
private fun SocialAlertRow(notification: SocialNotification, model: SocialModel) {
    val actor = model.user(notification.actorId)
    val read = model.isRead(notification)
    AMSwipeableRow(
        startAction = AMSwipeAction(
            "Mute",
            painterResource(R.drawable.ic_volume_off),
            tint = null,
            removesRow = true,
        ) { model.mute(actor.id) },
        endAction = AMSwipeAction(
            if (read) "Mark unread" else "Mark read",
            painterResource(if (read) R.drawable.ic_mark_email_unread else R.drawable.ic_mark_email_read),
        ) { model.toggleRead(notification.id) },
    ) {
        AMListRow(
            modifier = Modifier.semantics { if (!read) stateDescription = "Unread" },
            leadingContent = { SocialAlertAvatar(actor, unread = !read) },
            supportingContent = {
                Text(
                    notification.snippet?.let { "${notification.timestamp} · “$it”" } ?: notification.timestamp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingContent = {
                Icon(painterResource(notification.kind.icon), contentDescription = null)
            },
            onClick = { model.open(notification) },
        ) {
            Text(
                "${actor.name} ${notification.kind.verb}",
                fontWeight = if (read) null else FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SocialAlertAvatar(actor: SocialUser, unread: Boolean) {
    val avatar: @Composable () -> Unit = {
        AMAvatar(actor.avatarUrl, actor.name, Modifier.clearAndSetSemantics {}, size = AMAvatarSize.Small)
    }
    if (unread) AMBadgedBox(tint = AMSemantic.Info) { avatar() } else avatar()
}
