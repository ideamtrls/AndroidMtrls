package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The one call to action of an [AMEmptyState]: a label, an optional glyph before it, and what it
 * does. The empty state shows it as a primary [AMButton].
 *
 * ```kotlin
 * AMEmptyStateAction("Compose", icon = painterResource(R.drawable.ic_edit_square)) { compose() }
 * ```
 *
 * @property label the button's label, in sentence case.
 * @property icon a glyph before the label, or null for none.
 * @property onClick what the button does, run on the main thread when it's tapped.
 */
@Immutable
public class AMEmptyStateAction(
    public val label: String,
    public val icon: Painter? = null,
    public val onClick: () -> Unit,
)

/**
 * What a view shows when it has nothing to show: a large glyph, a title, an optional description
 * and an optional call to action, centered.
 *
 * ```kotlin
 * AMEmptyState(
 *     painterResource(R.drawable.ic_inbox),
 *     "No messages",
 *     description = "New messages will appear here.",
 *     action = AMEmptyStateAction("Compose", painterResource(R.drawable.ic_edit_square)) { compose() },
 * )
 * ```
 *
 * It lies on the page's own background, never on a card: the glyph and text draw no surface of
 * their own, so they sit on whatever is behind them (the page color, or the app's backdrop), and
 * only the button floats. Put it where the content would have been, not inside a card. Several
 * buttons, or buttons in a style of your own, go in the other form's `actions` slot.
 *
 * For TalkBack, the title is a heading, and the glyph, title and description read as one element;
 * the button is its own.
 *
 * @param icon the glyph, drawn large in `onSurfaceVariant`; it's decorative, so TalkBack skips it.
 * @param title what's missing ("No messages"), in sentence case.
 * @param modifier the empty state's placement. It fills the width it's given and centers its
 *   content there.
 * @param description why, or what will fill it, under the title. Null shows none.
 * @param action the one call to action, a primary [AMButton] under the text. Null shows none.
 */
@Composable
public fun AMEmptyState(
    icon: Painter,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: AMEmptyStateAction? = null,
) {
    val actions: (@Composable ColumnScope.() -> Unit)? = if (action == null) {
        null
    } else {
        { AMButton(action.label, onClick = action.onClick, icon = action.icon, style = AMButtonStyle.Primary()) }
    }
    EmptyStateLayout(icon, title, modifier, description, actions)
}

/**
 * An [AMEmptyState] with buttons of your own under its text, in the [actions] slot: several
 * actions, or a style other than primary.
 *
 * ```kotlin
 * AMEmptyState(painterResource(R.drawable.ic_group), "No contacts", description = "Add people to start a conversation.") {
 *     AMButton("Add contact", onClick = ::addContact, style = AMButtonStyle.Primary())
 *     AMButton("Import contacts", onClick = ::importContacts, style = AMButtonStyle.Ghost)
 * }
 * ```
 *
 * The slot is a column, centered under the text with `spacing.sm` between its items. Each button
 * keeps the style it's given ([LocalAMButtonStyle] when it passes none): the empty state doesn't
 * restyle them.
 *
 * @param icon the glyph, drawn large in `onSurfaceVariant`; it's decorative, so TalkBack skips it.
 * @param title what's missing, in sentence case.
 * @param modifier the empty state's placement. It fills the width it's given and centers its
 *   content there.
 * @param description why, or what will fill it, under the title. Null shows none.
 * @param actions the buttons, stacked and centered under the text.
 */
@Composable
public fun AMEmptyState(
    icon: Painter,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actions: @Composable ColumnScope.() -> Unit,
) {
    EmptyStateLayout(icon, title, modifier, description, actions)
}

/** Both forms of [AMEmptyState]: the glyph, the text as one heading element, then the actions. */
@Composable
private fun EmptyStateLayout(
    icon: Painter,
    title: String,
    modifier: Modifier,
    description: String?,
    actions: (@Composable ColumnScope.() -> Unit)?,
) {
    val spacing = AMTheme.tokens.spacing
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, Modifier.size(EmptyStateGlyphSize), tint = colors.onSurfaceVariant)
            Spacer(Modifier.height(spacing.md))
            Text(
                title,
                Modifier.semantics { heading() },
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )
            if (description != null) {
                Spacer(Modifier.height(spacing.xs))
                Text(
                    description,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (actions != null) {
            Spacer(Modifier.height(spacing.lg))
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = actions,
            )
        }
    }
}

/** The empty state's glyph: twice Material's icon size, so it reads as the view's picture. */
private val EmptyStateGlyphSize = 48.dp
