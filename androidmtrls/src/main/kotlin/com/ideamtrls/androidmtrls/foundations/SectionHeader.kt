package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/**
 * A group's label: Android's list-subheader look, `titleSmall` in the color scheme's `primary`,
 * written as given in sentence case. A screen reader treats it as a heading.
 *
 * ```kotlin
 * AMSectionHeader("Recents", Modifier.padding(horizontal = 16.dp))
 * ```
 *
 * Over a picture (inside an [AMBackdrop]) it takes `onSurface` and the text legibility shadow
 * ([AMElevation.Text]), so it holds over the art.
 *
 * It fills the width it's given and adds no padding: place it at the row inset.
 *
 * @param title the label.
 * @param modifier its placement and padding.
 */
@Composable
public fun AMSectionHeader(title: String, modifier: Modifier = Modifier) {
    AMSectionHeader(modifier) { Text(title) }
}

/**
 * A group's label with custom content (a glyph before it, a count after it): Android's
 * list-subheader look, `titleSmall` in `primary`, applied to whatever [content] shows, laid out
 * in a row with the small spacing step between its parts. A screen reader reads it as one heading.
 *
 * ```kotlin
 * AMSectionHeader {
 *     Icon(painterResource(R.drawable.ic_queue_music), contentDescription = null)
 *     Text("Playlists", Modifier.weight(1f))
 *     Text("12")
 * }
 * ```
 *
 * Over a picture (inside an [AMBackdrop]) it takes `onSurface` and the text legibility shadow.
 *
 * @param modifier its placement and padding.
 * @param content the label's parts, in a row.
 */
@Composable
public fun AMSectionHeader(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val overArt = LocalAMBackdrop.current != null
    val color = if (overArt) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { heading() }
            .then(if (overArt) Modifier.glyphShadow(Style.Shadow.Text) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val row = this
        CompositionLocalProvider(LocalContentColor provides color) {
            ProvideTextStyle(MaterialTheme.typography.titleSmall) { row.content() }
        }
    }
}
