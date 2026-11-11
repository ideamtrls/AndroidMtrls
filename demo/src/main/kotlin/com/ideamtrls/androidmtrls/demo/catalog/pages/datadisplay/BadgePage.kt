package com.ideamtrls.androidmtrls.demo

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import com.ideamtrls.androidmtrls.AMAvatar
import com.ideamtrls.androidmtrls.AMAvatarSize
import com.ideamtrls.androidmtrls.AMBadgedBox
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val BadgeEntry = ComponentEntry(
    id = "badge",
    category = CatalogCategory.DataDisplay,
    title = "Badge",
    summary = "Notification dot or count on a corner",
    icon = R.drawable.ic_notifications_unread,
    keywords = listOf("AMBadge", "AMBadgedBox", "Badge", "BadgedBox", "notification", "unread", "count", "dot"),
) { BadgePage() }

@Composable
private fun BadgePage() {
    ComponentPage(
        BadgeEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { PreviewBadges() },
    ) {
        BadgedShortcuts()
    }
}

/** A dot, a count and an overflowing count, each on a bell, on a surface strip. */
@Composable
private fun PreviewBadges() {
    val spacing = AMTheme.tokens.spacing
    Row(
        Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        AMBadgedBox { Bell() }
        AMBadgedBox(count = 5) { Bell() }
        AMBadgedBox(count = 128) { Bell() }
    }
}

@Composable
private fun Bell() {
    Icon(painterResource(R.drawable.ic_notifications), contentDescription = "Notifications")
}

/** An app's shortcuts in a card, each marking its news its own way. */
@Composable
private fun BadgedShortcuts() {
    val spacing = AMTheme.tokens.spacing
    Row(
        Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Shortcut("Home") { Glyph(R.drawable.ic_home) }
        Shortcut("Alerts") { AMBadgedBox { Glyph(R.drawable.ic_notifications) } }
        Shortcut("Inbox") { AMBadgedBox(count = 7) { Glyph(R.drawable.ic_mail) } }
        Shortcut("Updates") { AMBadgedBox(count = 128, tint = AMSemantic.Info) { Glyph(R.drawable.ic_inbox) } }
        Shortcut("Profile") {
            AMBadgedBox(tint = AMSemantic.Success) { AMAvatar(null, name = "Grace Hopper", size = AMAvatarSize.Small) }
        }
    }
}

/** One shortcut: its mark over its name, read as one. */
@Composable
private fun RowScope.Shortcut(title: String, mark: @Composable () -> Unit) {
    Column(
        Modifier.weight(1f).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xs),
    ) {
        Box(Modifier.height(AMAvatarSize.Small.diameter), contentAlignment = Alignment.Center) { mark() }
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Glyph(@DrawableRes icon: Int) {
    Icon(painterResource(icon), contentDescription = null)
}
