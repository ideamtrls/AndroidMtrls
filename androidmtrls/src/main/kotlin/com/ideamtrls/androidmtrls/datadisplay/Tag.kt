package com.ideamtrls.androidmtrls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * What an [AMTag] says about its subject: a neutral category, or one of the four status roles
 * ([AMSemantic]), so a tag shows a status the same way a banner does.
 */
public enum class AMTagTint {
    /** A category with no charge: on-surface text on a quiet tonal fill. */
    Neutral,

    /** News worth noticing: [AMSemantic.Info]'s container colors. */
    Info,

    /** Completed, active, healthy: [AMSemantic.Success]'s container colors. */
    Success,

    /** Needs a look, delayed, degraded: [AMSemantic.Warning]'s container colors. */
    Warning,

    /** Failed, blocked, removal: [AMSemantic.Destructive]'s container colors. */
    Destructive,
}

/** The status role behind a charged tint; null for [AMTagTint.Neutral]. */
private val AMTagTint.semantic: AMSemantic?
    get() = when (this) {
        AMTagTint.Neutral -> null
        AMTagTint.Info -> AMSemantic.Info
        AMTagTint.Success -> AMSemantic.Success
        AMTagTint.Warning -> AMSemantic.Warning
        AMTagTint.Destructive -> AMSemantic.Destructive
    }

private val AMTagTint.containerColor: Color
    @Composable @ReadOnlyComposable get() = semantic?.containerColor ?: MaterialTheme.colorScheme.surfaceContainerHighest

private val AMTagTint.contentColor: Color
    @Composable @ReadOnlyComposable get() = semantic?.onContainerColor ?: MaterialTheme.colorScheme.onSurface

/** The glyph before a tag's label: small beside `labelMedium`. */
private val TagIconSize = 16.dp

/**
 * A small read-only pill for a status or a category: a label after an optional glyph, on a fill
 * in [tint]'s colors.
 *
 * ```kotlin
 * AMTag("Live", icon = AMSemantic.Success.icon, tint = AMTagTint.Success)
 * AMTag("Payments")
 * ```
 *
 * - **Colors.** [AMTagTint.Neutral] is on-surface text on the scheme's highest surface
 *   container; the four status tints take their [AMSemantic] role's container and on-container
 *   colors, the same colors a banner of that role uses.
 * - **One line.** The label never wraps: in a tight row, the text beside it wraps instead. A
 *   row of several tags wraps itself: put them in an [AMFlowLayout].
 * - **Shape.** A full pill, `labelMedium` type, padded `spacing.sm` at the sides and `spacing.xs`
 *   above and below. It reads as one piece to a screen reader; the glyph is decorative.
 *
 * Tags only draw; for something to tap or toggle, use Material's chips.
 *
 * @param label the tag's text.
 * @param modifier the tag's placement.
 * @param icon a glyph before the label, in the label's color. Null shows none.
 * @param tint the colors: neutral, or a status role.
 */
@Composable
public fun AMTag(
    label: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    tint: AMTagTint = AMTagTint.Neutral,
) {
    AMTag(modifier, tint) {
        if (icon != null) Icon(icon, contentDescription = null, Modifier.size(TagIconSize))
        Text(label, maxLines = 1, softWrap = false)
    }
}

/**
 * A small read-only pill with custom content, such as a version number in monospace. The tag
 * applies its fill, its shape, `labelMedium` type and [tint]'s content color around [content],
 * so text and glyphs inside stay on-system. Otherwise the same pill as the string form.
 *
 * ```kotlin
 * AMTag { Text("v2.4.1", fontFamily = FontFamily.Monospace) }
 * ```
 *
 * @param modifier the tag's placement.
 * @param tint the colors: neutral, or a status role.
 * @param content the pill's content, in a row whose parts sit `spacing.xs` apart, centered
 *   vertically. Keep text in it to one line.
 */
@Composable
public fun AMTag(
    modifier: Modifier = Modifier,
    tint: AMTagTint = AMTagTint.Neutral,
    content: @Composable RowScope.() -> Unit,
) {
    val spacing = AMTheme.tokens.spacing
    Row(
        modifier
            .semantics(mergeDescendants = true) {}
            .clip(CircleShape)
            .background(tint.containerColor)
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides tint.contentColor) {
            ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                content()
            }
        }
    }
}
