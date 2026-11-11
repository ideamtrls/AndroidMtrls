package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription

/**
 * A dot that says "there's something new here": Material's small badge in [tint]'s color, for an
 * unread state or general attention.
 *
 * ```kotlin
 * AMIconButton(painterResource(R.drawable.ic_notifications), "Alerts", onClick = ::openAlerts, badge = { AMBadge() })
 * ```
 *
 * Usually it sits on a glyph's corner: pass it as [AMIconButton]'s `badge`, or wrap the glyph in
 * [AMBadgedBox]. A screen reader skips the dot; say what it means on the thing it marks.
 *
 * @param modifier the badge's placement.
 * @param tint the badge's color: [AMSemantic.Destructive] (the scheme's error red) by default.
 */
@Composable
public fun AMBadge(modifier: Modifier = Modifier, tint: AMSemantic = AMSemantic.Destructive) {
    Badge(modifier.clearAndSetSemantics {}, containerColor = tint.color, contentColor = tint.onColor)
}

/**
 * A count on a corner: Material's badge with a number, in [tint]'s color, for notifications or
 * queued items. Past 99 it shows "99+"; at zero or below it shows nothing, so a live count can be
 * passed straight in without hiding the badge by hand.
 *
 * ```kotlin
 * AMIconButton(painterResource(R.drawable.ic_mail), "Inbox", onClick = ::openInbox, badge = { AMBadge(unread) })
 * ```
 *
 * The number takes [tint]'s on-color, so it reads on any role. A screen reader says
 * [contentDescription], by default the full count of notifications ("128 notifications"); name
 * what's counted when it's something else:
 *
 * ```kotlin
 * AMBadge(bag.count, contentDescription = if (bag.count == 1) "1 item" else "${bag.count} items")
 * ```
 *
 * @param count the number to show.
 * @param modifier the badge's placement.
 * @param contentDescription what a screen reader says for the badge: the full count and what it
 *   counts. By default "1 notification" or "[count] notifications".
 * @param tint the badge's color: [AMSemantic.Destructive] (the scheme's error red) by default.
 */
@Composable
public fun AMBadge(
    count: Int,
    modifier: Modifier = Modifier,
    contentDescription: String = notificationCount(count),
    tint: AMSemantic = AMSemantic.Destructive,
) {
    if (count <= 0) return
    Badge(
        modifier.clearAndSetSemantics { this.contentDescription = contentDescription },
        containerColor = tint.color,
        contentColor = tint.onColor,
    ) {
        Text(if (count > MaxShownCount) "$MaxShownCount+" else "$count")
    }
}

/**
 * Marks [content] with a dot ([AMBadge]) at its top end corner, Material's `BadgedBox` placement:
 * for a glyph, an avatar or any small thing that has news.
 *
 * ```kotlin
 * AMBadgedBox { Icon(painterResource(R.drawable.ic_notifications), contentDescription = "Alerts, unread") }
 * ```
 *
 * @param modifier the box's placement.
 * @param tint the dot's color: [AMSemantic.Destructive] by default.
 * @param content what the dot marks.
 */
@Composable
public fun AMBadgedBox(
    modifier: Modifier = Modifier,
    tint: AMSemantic = AMSemantic.Destructive,
    content: @Composable BoxScope.() -> Unit,
) {
    BadgedBox(badge = { AMBadge(tint = tint) }, modifier = modifier, content = content)
}

/**
 * Marks [content] with a count ([AMBadge]) at its top end corner, Material's `BadgedBox`
 * placement. At zero or below the badge hides and [content] stays exactly where it was, so a live
 * count can be passed straight in.
 *
 * ```kotlin
 * AMBadgedBox(count = unread) { Icon(painterResource(R.drawable.ic_mail), contentDescription = "Inbox") }
 * ```
 *
 * @param count the number to show.
 * @param modifier the box's placement.
 * @param contentDescription what a screen reader says for the count, as [AMBadge] takes it. By
 *   default "1 notification" or "[count] notifications".
 * @param tint the badge's color: [AMSemantic.Destructive] by default.
 * @param content what the count marks.
 */
@Composable
public fun AMBadgedBox(
    count: Int,
    modifier: Modifier = Modifier,
    contentDescription: String = notificationCount(count),
    tint: AMSemantic = AMSemantic.Destructive,
    content: @Composable BoxScope.() -> Unit,
) {
    BadgedBox(badge = { AMBadge(count, contentDescription = contentDescription, tint = tint) }, modifier = modifier, content = content)
}

/** The largest count a badge shows as a number; anything above it reads "99+". */
private const val MaxShownCount = 99

/** What a count badge says by default: the full count of notifications. */
private fun notificationCount(count: Int): String = if (count == 1) "1 notification" else "$count notifications"
