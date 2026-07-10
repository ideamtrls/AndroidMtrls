package io.github.jmsdnns.androidmtrls.content

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.listRowInsets

// ---------------------------------------------------------------------------
// AMRowContent — label/value row (LabeledContentStyle port)
// ---------------------------------------------------------------------------

/**
 * Full-bleed label/value row for displaying named data pairs.
 *
 * Port of iOS `IMRowLabeledContentStyle` / `.imRow`: `LabeledContent` with that
 * style becomes `AMRowContent(label, value)` in Compose, since Compose has no
 * `LabeledContentStyle` injection mechanism.
 *
 * Layout: label fills available width on the leading side; value sits trailing.
 * Both slots are vertically centered within the row. The row self-pads with
 * `tokens.listRowInsets` (horizontal 14dp, vertical 16dp) and fills its own
 * `AMTheme.Surface.primary` background — it is a full-bleed list row, not
 * card-framed.
 *
 * Stack multiple `AMRowContent` rows with `AMDivider(inset = AMTheme.Input.inset.dp)`
 * between them to form a settings or detail list.
 *
 * The custom-content-slot overload gives callers full layout control over both
 * the label and value areas while keeping the same outer row geometry.
 *
 * iOS equivalent: `LabeledContent("Plan", value: "Pro").labeledContentStyle(.imRow)`
 *
 * Token cross-references: [AMTheme.Surface.primary], [AMTheme.Text.primary],
 * [AMTheme.Text.secondary], [AMTokens.listRowInsets], [AMTokens.spacing.md],
 * [AMTheme.Input.inset].
 *
 * @param label   Composable rendered on the leading side; fills available width.
 * @param content Composable rendered on the trailing side; sized to its intrinsic width.
 */
@Composable
fun AMRowContent(
    label: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.primary.resolve(isDark))
            .padding(tokens.listRowInsets),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            label()
        }
        content()
    }
}

// ---------------------------------------------------------------------------
// AMRowContent — String/String convenience overload (primary usage)
// ---------------------------------------------------------------------------

/**
 * Full-bleed label/value row with standard String content.
 *
 * Convenience overload of [AMRowContent] for the common case where both the
 * label and value are plain strings. The label is rendered with
 * [AMTheme.Type.title] in `AMTheme.Text.primary`; the value is rendered with
 * [AMTheme.Type.body] in `AMTheme.Text.secondary`, matching the iOS
 * `.imRow` font assignments.
 *
 * ```kotlin
 * AMRowContent(label = "Plan", value = "Pro")
 * AMRowContent(label = "Renews", value = "Jan 12, 2027")
 * AMRowContent(label = "Seats",  value = "12 of 25")
 * ```
 *
 * iOS equivalent: `LabeledContent("Plan", value: "Pro").labeledContentStyle(.imRow)`
 *
 * @param label The name or key for the data pair; rendered headline-semibold and primary.
 * @param value The value for the data pair; rendered body and secondary.
 */
@Composable
fun AMRowContent(
    label: String,
    value: String,
) {
    val isDark = LocalAMIsDarkTheme.current

    AMRowContent(
        label = {
            BasicText(
                text = label,
                style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            )
        },
        content = {
            BasicText(
                text = value,
                style = AMTheme.Type.body.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
            )
        },
    )
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun RowContentPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(tokens.spacing.lg),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AMRowContent(label = "Plan", value = "Pro")
            AMDivider(inset = AMTheme.Input.inset.dp)
            AMRowContent(label = "Renews", value = "Jan 12, 2027")
            AMDivider(inset = AMTheme.Input.inset.dp)
            AMRowContent(label = "Seats", value = "12 of 25")
        }
    }
}

@Preview(
    name = "AMRowContent · Light",
    group = "AMRowContent",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,  // AMTheme.Surface.page light
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMRowContentPreviewLight() {
    AMThemeRoot {
        RowContentPreviewContent()
    }
}

@Preview(
    name = "AMRowContent · Dark",
    group = "AMRowContent",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,  // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMRowContentPreviewDark() {
    AMThemeRoot {
        RowContentPreviewContent()
    }
}
