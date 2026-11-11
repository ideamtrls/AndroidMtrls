package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The row of buttons that closes a form sheet: the commit button ([primary], Save) and, on a sheet
 * that edits something that exists, the destructive one ([destructive], Delete) before it, side by
 * side in equal widths `spacing.md` apart. Alone, the commit button spans the row.
 *
 * ```kotlin
 * // Creating: Save alone, off while the title is empty.
 * AMFormActions { AMButton("Save", onClick = ::save, enabled = title.isNotEmpty()) }
 *
 * // Editing: Delete, then Save.
 * AMFormActions(destructive = { AMButton("Delete", onClick = ::delete, destructive = true) }) {
 *     AMButton("Save", onClick = ::save)
 * }
 * ```
 *
 * Put plain [AMButton]s in the slots; the row owns their arrangement and their style, through
 * [LocalAMButtonStyle]: the commit button is [AMButtonStyle.Primary], the destructive one
 * [AMButtonStyle.Secondary], both solid at [elevation]. Mark the destructive button
 * `destructive = true` so its label takes the destructive color.
 *
 * Each button keeps its own `enabled`, so Save can wait for a valid form while Delete stays live;
 * [enabled] = false turns off both at once (while a save is in flight), without touching either.
 *
 * @param modifier the row's placement. The row fills the width it's given.
 * @param destructive the destructive button, before the commit button. Null for a form that
 *   creates something.
 * @param elevation both buttons' shadow: [AMElevation.Low] by default, [AMElevation.None] for flat
 *   buttons.
 * @param enabled whether the buttons in the row can be enabled at all.
 * @param primary the commit button.
 */
@Composable
public fun AMFormActions(
    modifier: Modifier = Modifier,
    destructive: (@Composable () -> Unit)? = null,
    elevation: AMElevation = AMElevation.Low,
    enabled: Boolean = true,
    primary: @Composable () -> Unit,
) {
    ProvideAMEnabled(enabled) {
        Row(
            modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (destructive != null) {
                Box(Modifier.weight(1f), propagateMinConstraints = true) {
                    CompositionLocalProvider(
                        LocalAMButtonStyle provides AMButtonStyle.Secondary(elevation),
                        content = destructive,
                    )
                }
            }
            Box(Modifier.weight(1f), propagateMinConstraints = true) {
                CompositionLocalProvider(
                    LocalAMButtonStyle provides AMButtonStyle.Primary(elevation),
                    content = primary,
                )
            }
        }
    }
}
