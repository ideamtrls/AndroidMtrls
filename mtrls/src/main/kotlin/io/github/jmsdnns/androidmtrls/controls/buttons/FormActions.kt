package io.github.jmsdnns.androidmtrls.controls.buttons

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMButtonRole
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens

// ---------------------------------------------------------------------------
// AMFormActions — the paired action row that closes a form sheet
// ---------------------------------------------------------------------------

/**
 * The paired action row that closes a form sheet: a disable-aware primary commit
 * button ([AMPrimaryButton]) and — for edit-mode sheets — a destructive secondary
 * beside it ([AMSecondaryButton] carrying [AMButtonRole.Destructive]). Both fill
 * the surface body of a filled button style, so a single [Row] with
 * [Modifier.weight] splits them into equal widths.
 *
 * This create-mode overload renders the primary button alone — the new/create-sheet
 * case, with no destructive button. It stays full width and dims itself while
 * [primaryEnabled] is `false` (e.g. while the form is incomplete).
 *
 * The row carries a small (`spacing.sm`) top inset so it sits off the fields above
 * it — drop it straight into a form column as the last element.
 *
 * iOS equivalent: `IMFormActions(primaryTitle:primaryDisabled:onPrimary:)`. Per the
 * house `enabled`-not-`disabled` rule, iOS `primaryDisabled: Bool = false` becomes
 * `primaryEnabled: Boolean = true` here.
 *
 * @param primaryLabel   Label for the primary commit button (e.g. "Save").
 * @param primaryEnabled When `false` the primary button is non-interactive and
 *                       dims — use it to gate commit until the form is valid.
 * @param modifier       Modifier applied to the button's root layout node.
 * @param onPrimary      Required tap callback for the primary commit action.
 */
@Composable
fun AMFormActions(
    primaryLabel: String,
    primaryEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onPrimary: () -> Unit,
) {
    val tokens = LocalAMTokens.current

    AMPrimaryButton(
        label = primaryLabel,
        onClick = onPrimary,
        modifier = modifier.padding(top = tokens.spacing.sm),
        enabled = primaryEnabled,
    )
}

/**
 * Destructive-plus-primary [AMFormActions] — the edit-sheet case. The destructive
 * button renders on the leading side ([AMSecondaryButton] with
 * [AMButtonRole.Destructive] tinting its label) and the primary trails it; both
 * take `Modifier.weight(1f)` so they land at equal widths inside a
 * `Row(spacedBy(spacing.md))`.
 *
 * Like the create-mode overload, the row carries a `spacing.sm` top inset so it
 * sits off the fields above it — drop it in as the last element of a form column.
 *
 * iOS equivalent:
 * `IMFormActions(destructiveTitle:primaryTitle:primaryDisabled:onDestructive:onPrimary:)`.
 *
 * @param destructiveLabel Label for the leading destructive button (e.g. "Delete").
 * @param primaryLabel     Label for the trailing primary commit button (e.g. "Save").
 * @param primaryEnabled   When `false` the primary button is non-interactive and
 *                         dims; the destructive button stays enabled.
 * @param modifier         Modifier applied to the row's root layout node.
 * @param onDestructive    Required tap callback for the destructive action.
 * @param onPrimary        Required tap callback for the primary commit action.
 */
@Composable
fun AMFormActions(
    destructiveLabel: String,
    primaryLabel: String,
    primaryEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onDestructive: () -> Unit,
    onPrimary: () -> Unit,
) {
    val tokens = LocalAMTokens.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = tokens.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        AMSecondaryButton(
            label = destructiveLabel,
            onClick = onDestructive,
            modifier = Modifier.weight(1f),
            role = AMButtonRole.Destructive,
        )
        AMPrimaryButton(
            label = primaryLabel,
            onClick = onPrimary,
            modifier = Modifier.weight(1f),
            enabled = primaryEnabled,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun FormActionsPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(tokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xl),
    ) {
        // Create sheet — Save only, enabled.
        AMFormActions(primaryLabel = "Save", onPrimary = {})

        // Create sheet — Save only, disabled while the form is incomplete.
        AMFormActions(primaryLabel = "Save", primaryEnabled = false, onPrimary = {})

        // Edit sheet — destructive Delete beside an enabled Save, equal widths.
        AMFormActions(
            destructiveLabel = "Delete",
            primaryLabel = "Save",
            onDestructive = {},
            onPrimary = {},
        )
    }
}

@Preview(
    name = "AMFormActions · Light",
    group = "AMFormActions",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,   // AMTheme.Surface.page light
)
@Composable
private fun AMFormActionsPreviewLight() {
    AMThemeRoot {
        FormActionsPreviewContent()
    }
}

@Preview(
    name = "AMFormActions · Dark",
    group = "AMFormActions",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,   // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMFormActionsPreviewDark() {
    AMThemeRoot {
        FormActionsPreviewContent()
    }
}
