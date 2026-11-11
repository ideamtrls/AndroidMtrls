package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter

/**
 * A quiet action on a card: a glyph with an optional count, the parts of a card's action row
 * ([AMCardActionRow]): reply 12, like 48, share, ⋮.
 *
 * ```kotlin
 * AMCardButton(painterResource(R.drawable.ic_chat_bubble), "Reply", onClick = ::reply, count = "12")
 * AMCardButton(painterResource(R.drawable.ic_favorite_filled), "Liked", onClick = ::like, count = "49", active = true, activeTint = AMSemantic.Destructive)
 * AMCardButton(painterResource(R.drawable.ic_more_vert), "More options", onClick = ::more)
 * ```
 *
 * With a [count] it's Material's `TextButton` with a leading glyph, its ripple a rounded rectangle
 * in the theme's medium shape like every AM button; without one it's Material's `IconButton`, with
 * its circular ripple. Both draw no surface (the card supplies it), keep Material's ripple and 48 dp
 * touch target, and show the glyph at Material's button glyph size in `onSurfaceVariant`, so a row
 * of them stays quiet beside the card's content. Counts use tabular figures, so a changing count
 * doesn't shift the row.
 *
 * Card actions are a glyph first: the only text beside the glyph is a count. A verb ("Save",
 * "Share") belongs on an [AMButton].
 *
 * @param icon the glyph.
 * @param contentDescription what a screen reader says for the action, before the count ("Reply",
 *   "Liked"). Name the state it's in when [active] changes it.
 * @param onClick called when the button is tapped.
 * @param modifier the button's placement.
 * @param count the count beside the glyph, formatted ("12", "1.2K"). Null shows the glyph alone.
 * @param active whether the action is on (liked, reposted): the glyph and count take [activeTint]
 *   instead of the quiet color.
 * @param activeTint the role whose color an active action takes: [AMSemantic.Info] (the color
 *   scheme's primary) by default, [AMSemantic.Destructive] for a filled heart.
 * @param enabled whether the button takes taps.
 */
@Composable
public fun AMCardButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: String? = null,
    active: Boolean = false,
    activeTint: AMSemantic = AMSemantic.Info,
    enabled: Boolean = true,
) {
    val isEnabled = resolveEnabled(enabled)
    val tint = if (active) activeTint.color else MaterialTheme.colorScheme.onSurfaceVariant
    if (count == null) {
        IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = isEnabled,
            colors = IconButtonDefaults.iconButtonColors(contentColor = tint),
        ) {
            Icon(icon, contentDescription, Modifier.size(ButtonDefaults.IconSize))
        }
    } else {
        TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = isEnabled,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.textButtonColors(contentColor = tint),
            contentPadding = ButtonDefaults.TextButtonContentPadding,
        ) {
            Icon(icon, contentDescription, Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(count, maxLines = 1, style = LocalTextStyle.current.copy(fontFeatureSettings = TabularFigures))
        }
    }
}

/** Figures of one width, so a count that changes doesn't shift what follows it. */
private const val TabularFigures = "tnum"
