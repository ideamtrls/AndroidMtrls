package com.ideamtrls.androidmtrls

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

/**
 * The four status roles, and the one place each resolves to its colors and glyph, so every status
 * primitive (a tag, a banner) shows the same role the same way.
 *
 * Each role has Material's four color roles: [color], [onColor], [containerColor] and
 * [onContainerColor]. [Info] is the color scheme's `primary` group and [Destructive] its `error`
 * group; [Success] and [Warning] come from [AMTokens.semantic], green and amber tonal pairs built
 * the way Material builds `error`. All four follow the theme's appearance.
 *
 * ```kotlin
 * Icon(AMSemantic.Success.icon, contentDescription = null, tint = AMSemantic.Success.color)
 * ```
 */
public enum class AMSemantic {
    /** Neutral news worth noticing: the color scheme's primary. */
    Info,

    /** Completed, active, healthy: green. */
    Success,

    /** Needs a look, degraded: amber. */
    Warning,

    /** Failed, blocked, removal: the color scheme's error. */
    Destructive;

    /** The role's color: a glyph, a label, an accent. */
    public val color: Color
        @Composable @ReadOnlyComposable get() = when (this) {
            Info -> MaterialTheme.colorScheme.primary
            Destructive -> MaterialTheme.colorScheme.error
            Success, Warning -> tints().color
        }

    /** Content on [color]. */
    public val onColor: Color
        @Composable @ReadOnlyComposable get() = when (this) {
            Info -> MaterialTheme.colorScheme.onPrimary
            Destructive -> MaterialTheme.colorScheme.onError
            Success, Warning -> tints().onColor
        }

    /** A quieter fill for the role: a tag's or a banner's background. */
    public val containerColor: Color
        @Composable @ReadOnlyComposable get() = when (this) {
            Info -> MaterialTheme.colorScheme.primaryContainer
            Destructive -> MaterialTheme.colorScheme.errorContainer
            Success, Warning -> tints().container
        }

    /** Content on [containerColor]. */
    public val onContainerColor: Color
        @Composable @ReadOnlyComposable get() = when (this) {
            Info -> MaterialTheme.colorScheme.onPrimaryContainer
            Destructive -> MaterialTheme.colorScheme.onErrorContainer
            Success, Warning -> tints().onContainer
        }

    /**
     * The glyph that reads as this role at a glance, a filled Material Symbol, so the status shows
     * as a solid badge: `info`, `check_circle`, `warning`, `error`. A banner leads with it.
     */
    public val icon: Painter
        @Composable get() = painterResource(
            when (this) {
                Info -> R.drawable.am_ic_info
                Success -> R.drawable.am_ic_check_circle
                Warning -> R.drawable.am_ic_warning
                Destructive -> R.drawable.am_ic_error
            },
        )

    /**
     * What a screen reader says for the role's glyph ahead of a banner's title: nothing for
     * [Info], whose title alone carries plain news, then "Success", "Warning", "Error".
     */
    internal val severityLabel: String?
        get() = when (this) {
            Info -> null
            Success -> "Success"
            Warning -> "Warning"
            Destructive -> "Error"
        }

    @Composable
    @ReadOnlyComposable
    private fun tints(): AMSemanticColors {
        val tints = AMTheme.tokens.semantic
        val dark = AMTheme.isDark
        return when (this) {
            Success -> if (dark) tints.successDark else tints.successLight
            else -> if (dark) tints.warningDark else tints.warningLight
        }
    }
}
