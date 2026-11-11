package com.ideamtrls.androidmtrls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics

/**
 * Folding: a card whose header row opens it in place to show [content] under the header, so
 * secondary content never takes you away from what you were looking at.
 *
 * ```kotlin
 * var expanded by rememberSaveable { mutableStateOf(false) }
 * AMDisclosureGroup("Advanced", expanded, onExpandedChange = { expanded = it }) {
 *     StrictModeRow()
 * }
 * ```
 *
 * The header is a row at the row recipe's rhythm: the title in `bodyLarge`, an optional leading
 * [icon], and Material's `expand_more` chevron at the end, which turns to point up while the card
 * is open. Tapping anywhere on the header (ripple included) asks to toggle; a screen reader
 * hears it as collapsed or expanded and gets the expand and collapse actions. The content opens
 * below a divider, inset by `spacing.lg` at the sides and `spacing.md` above and below and spaced
 * by `spacing.md`, growing and fading in with Material's standard motion. A row placed there keeps
 * its own row padding on top of that inset.
 *
 * @param title the header's title.
 * @param expanded whether the card is open.
 * @param onExpandedChange called with the state the user asks for.
 * @param modifier the card's placement.
 * @param icon a glyph before the title. Null shows none.
 * @param fill the card's fill, solid or frosted over art.
 * @param enabled whether the header takes taps. A disabled header keeps its place and dims; a
 *   disabled container (`LocalAMEnabled`) disables it too.
 * @param content what the card shows while it's open.
 */
@Composable
public fun AMDisclosureGroup(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    fill: AMSurfaceFill = AMSurfaceFill.Solid,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    AMDisclosureGroup(
        label = {
            if (icon != null) Icon(icon, contentDescription = null)
            Text(title)
        },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        fill = fill,
        enabled = enabled,
        content = content,
    )
}

/**
 * Folding with a custom header: [label]'s parts (a glyph, a title, a count) in the header row, in
 * `bodyLarge` and `onSurface`, with the chevron after them. Otherwise the same card as the string
 * form.
 *
 * @param label the header's parts, in a row spaced by `spacing.md`.
 * @param expanded whether the card is open.
 * @param onExpandedChange called with the state the user asks for.
 * @param modifier the card's placement.
 * @param fill the card's fill, solid or frosted over art.
 * @param enabled whether the header takes taps. A disabled header keeps its place and dims; a
 *   disabled container (`LocalAMEnabled`) disables it too.
 * @param content what the card shows while it's open.
 */
@Composable
public fun AMDisclosureGroup(
    label: @Composable RowScope.() -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    fill: AMSurfaceFill = AMSurfaceFill.Solid,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    // A disabled container (LocalAMEnabled) disables the header too.
    val headerEnabled = resolveEnabled(enabled)
    val spacing = AMTheme.tokens.spacing
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val chevronTurn by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "chevron",
    )
    Column(modifier.fillMaxWidth().amCard(fill = fill)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = headerEnabled, role = Role.Button) { onExpandedChange(!expanded) }
                .semantics {
                    if (!headerEnabled) return@semantics
                    if (expanded) {
                        collapse { onExpandedChange(false); true }
                    } else {
                        expand { onExpandedChange(true); true }
                    }
                }
                .amRowPadding(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val color = if (headerEnabled) scheme.onSurface else scheme.onSurface.copy(alpha = DisabledAlpha)
            CompositionLocalProvider(LocalContentColor provides color) {
                ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        content = label,
                    )
                }
            }
            Icon(
                painterResource(R.drawable.am_ic_expand_more),
                contentDescription = null,
                modifier = Modifier.rotate(chevronTurn),
                tint = if (headerEnabled) scheme.onSurfaceVariant else scheme.onSurface.copy(alpha = DisabledAlpha),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
            exit = shrinkVertically(motion.defaultSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
        ) {
            // The divider and the body move as one.
            Column(Modifier.fillMaxWidth()) {
                AMDivider()
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    val body = this
                    ProvideAMSurfaceStyle(AMSurfaceStyle.Flat) { body.content() }
                }
            }
        }
    }
}

/** Material's alpha for disabled content. */
private const val DisabledAlpha = 0.38f
