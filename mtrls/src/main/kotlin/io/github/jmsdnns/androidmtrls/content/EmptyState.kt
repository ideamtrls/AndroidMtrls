package io.github.jmsdnns.androidmtrls.content

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMPrimaryButton
import io.github.jmsdnns.androidmtrls.controls.buttons.amButtonLabelStyle
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens

// ---------------------------------------------------------------------------
// AMEmptyStateAction — data carrier for the single-action convenience overload
// ---------------------------------------------------------------------------

/**
 * Action descriptor for the single-action convenience overload of [AMEmptyState].
 *
 * Carries the label string, an optional leading icon, and the tap callback.
 *
 * The single-action overload renders it as an [AMPrimaryButton] that hugs its
 * label (the iOS intrinsic pill) and centers in the actions column — the caller
 * does not need to style the button. For multiple actions or custom button
 * styles, use the [AMEmptyState] multi-action slot overload instead.
 *
 * @param title   Button label text.
 * @param icon    Optional leading icon rendered before [title] at 16dp.
 * @param onClick Called when the user taps the action button.
 */
@Stable
data class AMEmptyStateAction(
    val title: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)

// ---------------------------------------------------------------------------
// AMEmptyState — multi-action slot overload (designated)
// ---------------------------------------------------------------------------

/**
 * Full-screen empty-state view with a large icon, title, optional description,
 * and a fully customisable actions slot.
 *
 * Android equivalent of iOS `ContentUnavailableView` (iOS 17). Renders a
 * centered column in document order: icon → title → description → actions.
 *
 * The [actions] trailing lambda is the designated init — callers supply their
 * own button composables and own the button styling. For a single primary action
 * with automatic primary-button styling, use the convenience overload that takes
 * [AMEmptyStateAction] as the last non-lambda parameter.
 *
 * Accessibility:
 * - Icon carries `contentDescription = null` — decorative in context of title.
 * - TalkBack reads title → description → actions in document order (Compose
 *   [Column] preserves this automatically).
 *
 * ```kotlin
 * AMEmptyState(
 *     icon        = Icons.Default.Email,
 *     title       = "Inbox Empty",
 *     description = "Nothing to see here.",
 * ) {
 *     MyPrimaryButton("Compose", onClick = { … })
 *     MySecondaryButton("Import", onClick = { … })
 * }
 * ```
 *
 * iOS equivalent: `IMEmptyState(title:systemImage:description:actions:)`.
 *
 * @param icon        Large centered glyph (56dp). Decorative — contentDescription is null.
 * @param title       Primary heading shown below the icon.
 * @param description Optional secondary body text shown below [title].
 * @param actions     Trailing composable slot for action buttons. Receives [ColumnScope].
 *                    Defaults to empty (no actions rendered).
 */
@Composable
fun AMEmptyState(
    icon: ImageVector,
    title: String,
    description: String? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current

    val primaryColor = AMTheme.Text.primary.resolve(isDark)
    val secondaryColor = AMTheme.Text.secondary.resolve(isDark)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = tokens.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.lg),
    ) {
        // Large centered icon — decorative, contentDescription = null
        Image(
            imageVector = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(secondaryColor),
            modifier = Modifier.size(56.dp),
        )

        // Title — display-tier headline; SemiBold matches the iOS
        // `ContentUnavailableView` title (title3 semibold).
        BasicText(
            text = title,
            style = TextStyle(
                color = primaryColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.xl),
        )

        // Description (optional)
        if (description != null) {
            BasicText(
                text = description,
                style = AMTheme.Type.secondary.toTextStyle(secondaryColor)
                    .copy(textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = tokens.spacing.xl),
            )
        }

        // Actions slot — callers own button styles here
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            content = actions,
        )
    }
}

// ---------------------------------------------------------------------------
// AMEmptyState — single-action convenience overload
// ---------------------------------------------------------------------------

/**
 * Full-screen empty-state view with a large icon, title, optional description,
 * and a single action rendered as an [AMPrimaryButton].
 *
 * The [action] is rendered with the standard [AMPrimaryButton] treatment
 * (`Surface.primary` card, `Text.primary` label, monochrome ripple), hugging its
 * label (the iOS intrinsic pill) and centered in the actions column. An optional
 * [AMEmptyStateAction.icon] renders as a 16dp leading glyph. For multiple actions
 * or custom button styles, use the trailing-lambda overload of [AMEmptyState].
 *
 * ```kotlin
 * AMEmptyState(
 *     icon        = Icons.Default.Search,
 *     title       = "No Results",
 *     description = "Try a different search term.",
 *     action      = AMEmptyStateAction("Clear search") { viewModel.clearSearch() }
 * )
 * ```
 *
 * iOS equivalent: `IMEmptyState(title:systemImage:description:action:)`.
 *
 * @param icon        Large centered glyph (56dp). Decorative — contentDescription is null.
 * @param title       Primary heading shown below the icon.
 * @param description Optional secondary body text shown below [title].
 * @param action      Single action. Rendered as an [AMPrimaryButton] that hugs
 *                    its label. Pass `null` to show no action.
 */
@Composable
fun AMEmptyState(
    icon: ImageVector,
    title: String,
    description: String? = null,
    action: AMEmptyStateAction?,
) {
    AMEmptyState(
        title = title,
        icon = icon,
        description = description,
    ) {
        if (action != null) {
            // iOS forces `.fixedSize(horizontal: true)` so the primary button
            // hugs its label rather than stretching. `AMPrimaryButton` fills
            // width internally, so drive it from an intrinsic-max width to hug
            // the single-line label; the actions column centers it.
            val buttonModifier = Modifier.width(IntrinsicSize.Max)
            val actionIcon = action.icon
            if (actionIcon != null) {
                val isDark = LocalAMIsDarkTheme.current
                val tokens = LocalAMTokens.current
                val foreground = AMTheme.Text.primary.resolve(isDark)
                AMPrimaryButton(
                    onClick = action.onClick,
                    modifier = buttonModifier,
                ) {
                    Image(
                        imageVector = actionIcon,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(foreground),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(tokens.spacing.xs))
                    BasicText(
                        text = action.title,
                        // Match the String-overload path exactly — the standard
                        // 17sp SemiBold `AMPrimaryButton` label treatment — so the
                        // icon and icon-less CTAs render identical typography.
                        style = amButtonLabelStyle(foreground, FontWeight.SemiBold),
                    )
                }
            } else {
                AMPrimaryButton(
                    label = action.title,
                    onClick = action.onClick,
                    modifier = buttonModifier,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun EmptyStatePreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(tokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxl),
    ) {
        // Icon-only: uses slot overload with empty trailing lambda
        AMEmptyState(
            title = "No Favourites",
            icon = Icons.Default.Favorite,
        )

        // With description only: uses slot overload with empty trailing lambda
        AMEmptyState(
            title = "No Results",
            icon = Icons.Default.Search,
            description = "Try adjusting your search or filters.",
        )

        // With single action: uses convenience overload via named param
        AMEmptyState(
            title = "Nothing Here Yet",
            icon = Icons.Default.Home,
            description = "Create something to get started.",
            action = AMEmptyStateAction(
                title = "Get Started",
                onClick = {},
            ),
        )
    }
}

@Preview(
    name = "AMEmptyState · Light",
    group = "AMEmptyState",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,   // AMTheme.Surface.page light
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMEmptyStatePreviewLight() {
    AMThemeRoot {
        EmptyStatePreviewContent()
    }
}

@Preview(
    name = "AMEmptyState · Dark",
    group = "AMEmptyState",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,   // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMEmptyStatePreviewDark() {
    AMThemeRoot {
        EmptyStatePreviewContent()
    }
}
