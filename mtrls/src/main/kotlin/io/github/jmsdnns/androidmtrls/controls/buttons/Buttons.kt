package io.github.jmsdnns.androidmtrls.controls.buttons

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMButtonRole
import io.github.jmsdnns.androidmtrls.tokens.AMElevation
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amCardHairline
import io.github.jmsdnns.androidmtrls.tokens.amElevation
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.amSquircle

// ---------------------------------------------------------------------------
// Shared label typography — one 17sp size, weight-only variation
// ---------------------------------------------------------------------------

// iOS renders primary/secondary/ghost labels at the SAME 17pt `.body` size and
// distinguishes them by WEIGHT alone (Buttons.swift: primary `.semibold`,
// secondary `.regular`, ghost `.medium`). This mirrors that: the style is derived
// from the `body` scale entry so color and tracking flow from the token, and only
// the 17sp size plus the per-style weight are overridden. tokens/Type.kt is
// intentionally not touched — the button size is a deliberate literal, like the
// display sizes and numeric runs already called out in the type-scale rules.
private val AM_BUTTON_LABEL_SIZE = 17.sp

internal fun amButtonLabelStyle(foreground: Color, weight: FontWeight): TextStyle =
    AMTheme.Type.body.toTextStyle(foreground).copy(
        fontSize = AM_BUTTON_LABEL_SIZE,
        fontWeight = weight,
    )

// ---------------------------------------------------------------------------
// AMFilledButtonBody — private shared body for Primary and Secondary
// ---------------------------------------------------------------------------

/**
 * Shared internal composable that renders the filled surface-card body used by
 * both [AMPrimaryButton] and [AMSecondaryButton].
 *
 * Visual recipe:
 * - Background: `AMTheme.Surface.primary` clipped to `amSquircle(tokens.radius.md)`
 * - Press feedback: monochrome `amRipple()` state layer, bounded to the clipped shape
 * - Dark-mode hairline: `amCardHairline` — `selectionRing` stroke, light mode suppressed
 * - Elevation: `AMElevation.Low` shadow
 * - Width: `fillMaxWidth()`; min height: `AMTheme.rowMinHeight`
 * - Label row centered both axes (`contentAlignment = Center` on the frame Box)
 * - Padding: vertical `tokens.spacing.md`, horizontal `tokens.spacing.lg`
 * - Content slot styling (color, weight) is the caller's responsibility — the
 *   String overloads apply it explicitly; composable-slot callers style freely.
 */
@Composable
private fun AMFilledButtonBody(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current
    val interactionSource = remember { MutableInteractionSource() }

    val shape = amSquircle(tokens.radius.md)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AMTheme.rowMinHeight.dp)
            .amElevation(level = AMElevation.Low, shape = shape)
            .clip(shape)
            .background(
                color = AMTheme.Surface.primary.resolve(isDark),
                shape = shape,
            )
            .amCardHairline(shape)
            .semantics { this.role = Role.Button }
            .clickable(
                interactionSource = interactionSource,
                indication = amRipple(),
                enabled = enabled,
                onClick = onClick,
            ),
        // Centers the label row inside the 64dp min-height frame; the row's
        // own verticalAlignment only centers content within the row.
        contentAlignment = Alignment.Center,
    ) {
        // Label row — the monochrome ripple sweeps behind it on press.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = tokens.spacing.md,
                    horizontal = tokens.spacing.lg,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// AMPrimaryButton
// ---------------------------------------------------------------------------

/**
 * Surface-card button with a 17sp semibold label. The primary
 * call-to-action in most contexts.
 *
 * Fills the available width (`fillMaxWidth`), carries a `AMElevation.Low` shadow,
 * and in dark mode adds a `selectionRing` hairline so the card reads against the
 * near-black page.
 *
 * Press feedback is the monochrome `amRipple()` state layer, bounded to the
 * clipped card shape — never an opacity dim.
 *
 * iOS equivalent: `Button { … }.buttonStyle(.imPrimary)` via `IMPrimaryButtonStyle`.
 *
 * @param onClick  Required tap callback.
 * @param modifier Modifier applied to the button's root layout node (outside
 *                 internal sizing and padding).
 * @param enabled  When `false` the button is non-interactive and the label
 *                 renders in [AMTheme.Icon.disabled] color.
 * @param content  Trailing composable slot for the button label, scoped to
 *                 [RowScope] so icon + text can be arranged naturally.
 *                 Callers wanting destructive custom content color it themselves with `AMTheme.Semantic.destructive`.
 */
@Composable
fun AMPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    AMFilledButtonBody(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = content,
    )
}

/**
 * String-label convenience overload for [AMPrimaryButton].
 *
 * Renders [label] at 17sp semibold with the standard foreground
 * color logic. Equivalent to calling the composable-slot overload with a single
 * [BasicText].
 *
 * @param label   The button label string.
 * @param onClick Required tap callback.
 * @param modifier Modifier applied to the button's root layout node.
 * @param enabled  When `false` the button is non-interactive and dims the label.
 * @param role     `null` or [AMButtonRole.Destructive].
 */
@Composable
fun AMPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    role: AMButtonRole? = null,
) {
    val isDark = LocalAMIsDarkTheme.current

    val foreground = when {
        !enabled -> AMTheme.Icon.disabled.resolve(isDark)
        role == AMButtonRole.Destructive -> AMTheme.destructive.resolve(isDark)
        else -> AMTheme.Text.primary.resolve(isDark)
    }

    AMPrimaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        BasicText(
            text = label,
            // 17sp semibold — iOS `.body.weight(.semibold)`.
            style = amButtonLabelStyle(foreground, FontWeight.SemiBold),
        )
    }
}

// ---------------------------------------------------------------------------
// AMSecondaryButton
// ---------------------------------------------------------------------------

/**
 * Surface-card button with a 17sp regular-weight label. Same filled surface
 * treatment as [AMPrimaryButton] — same shape, shadow, dark-mode hairline, and
 * press feedback — but regular weight (vs semibold) to signal a non-critical
 * action.
 *
 * iOS equivalent: `Button { … }.buttonStyle(.imSecondary)` via `IMSecondaryButtonStyle`.
 *
 * @param onClick  Required tap callback.
 * @param modifier Modifier applied to the button's root layout node.
 * @param enabled  When `false` the button is non-interactive and dims the label.
 * @param content  Trailing composable slot for the button label.
 *                 Callers wanting destructive custom content color it themselves with `AMTheme.Semantic.destructive`.
 */
@Composable
fun AMSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    AMFilledButtonBody(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = content,
    )
}

/**
 * String-label convenience overload for [AMSecondaryButton].
 *
 * @param label   The button label string.
 * @param onClick Required tap callback.
 * @param modifier Modifier applied to the button's root layout node.
 * @param enabled  When `false` the button is non-interactive and dims the label.
 * @param role     `null` or [AMButtonRole.Destructive].
 */
@Composable
fun AMSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    role: AMButtonRole? = null,
) {
    val isDark = LocalAMIsDarkTheme.current

    val foreground = when {
        !enabled -> AMTheme.Icon.disabled.resolve(isDark)
        role == AMButtonRole.Destructive -> AMTheme.destructive.resolve(isDark)
        else -> AMTheme.Text.primary.resolve(isDark)
    }

    AMSecondaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        BasicText(
            text = label,
            // 17sp regular — iOS `.body.weight(.regular)`.
            style = amButtonLabelStyle(foreground, FontWeight.Normal),
        )
    }
}

// ---------------------------------------------------------------------------
// AMGhostButton
// ---------------------------------------------------------------------------

/**
 * Label-only button — no fill, no elevation, no hairline.
 *
 * Use inside dialogs, panels, or alongside a filled button when the secondary
 * action should visually recede. Press feedback is the monochrome `amRipple()`
 * state layer, bounded to a small rounded rect clipped around the label row.
 *
 * iOS equivalent: `Button { … }.buttonStyle(.imGhost)` via `IMGhostButtonStyle`.
 *
 * @param onClick  Required tap callback.
 * @param modifier Modifier applied to the button's root layout node.
 * @param enabled  When `false` the button is non-interactive and renders the
 *                 label in [AMTheme.Icon.disabled] color.
 * @param content  Trailing composable slot for the button label, scoped to
 *                 [RowScope].
 *                 Callers wanting destructive custom content color it themselves with `AMTheme.Semantic.destructive`.
 */
@Composable
fun AMGhostButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalAMTokens.current
    val interactionSource = remember { MutableInteractionSource() }

    // Ghost has no fill; clip to a small rounded rect so the bounded ripple has
    // a sane bound around the label row (covers the tap padding below it).
    val shape = amSquircle(tokens.radius.sm)

    Row(
        modifier = modifier
            .clip(shape)
            .semantics { this.role = Role.Button }
            .clickable(
                interactionSource = interactionSource,
                indication = amRipple(),
                enabled = enabled,
                onClick = onClick,
            )
            .padding(
                vertical = tokens.spacing.sm,
                horizontal = tokens.spacing.md,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

/**
 * String-label convenience overload for [AMGhostButton].
 *
 * @param label   The button label string.
 * @param onClick Required tap callback.
 * @param modifier Modifier applied to the button's root layout node.
 * @param enabled  When `false` the button is non-interactive and dims the label.
 * @param role     `null` or [AMButtonRole.Destructive].
 */
@Composable
fun AMGhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    role: AMButtonRole? = null,
) {
    val isDark = LocalAMIsDarkTheme.current

    val foreground = when {
        !enabled -> AMTheme.Icon.disabled.resolve(isDark)
        role == AMButtonRole.Destructive -> AMTheme.destructive.resolve(isDark)
        else -> AMTheme.Text.primary.resolve(isDark)
    }

    AMGhostButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        BasicText(
            text = label,
            // 17sp medium — iOS `.body.weight(.medium)`.
            style = amButtonLabelStyle(foreground, FontWeight.Medium),
        )
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun ButtonsPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(tokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        // Primary — normal
        AMPrimaryButton(label = "Primary", onClick = {})

        // Secondary — normal
        AMSecondaryButton(label = "Secondary", onClick = {})

        // Ghost — normal
        AMGhostButton(label = "Ghost", onClick = {})

        // Primary — destructive
        AMPrimaryButton(
            label = "Delete",
            onClick = {},
            role = AMButtonRole.Destructive,
        )

        // Ghost — destructive
        AMGhostButton(
            label = "Remove",
            onClick = {},
            role = AMButtonRole.Destructive,
        )

        // Primary — disabled
        AMPrimaryButton(
            label = "Disabled Primary",
            onClick = {},
            enabled = false,
        )

        // Ghost — disabled
        AMGhostButton(
            label = "Disabled Ghost",
            onClick = {},
            enabled = false,
        )
    }
}

@Preview(
    name = "AMButtons · Light",
    group = "AMButtons",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,   // AMTheme.Surface.page light
)
@Composable
private fun AMButtonsPreviewLight() {
    AMThemeRoot {
        ButtonsPreviewContent()
    }
}

@Preview(
    name = "AMButtons · Dark",
    group = "AMButtons",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,   // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMButtonsPreviewDark() {
    AMThemeRoot {
        ButtonsPreviewContent()
    }
}
