package io.github.jmsdnns.androidmtrls.lists

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.blockInsets

// ---------------------------------------------------------------------------
// AMListCard — card container for vertical list rows
// ---------------------------------------------------------------------------

/**
 * A floating-surface card that hosts a vertical column of content rows.
 *
 * `AMListCard` wraps [AMCard] with a standardised inner [Column] that arranges
 * its children with 8dp vertical spacing (`spacing.sm`) and leading alignment.
 * This matches `IMListCard` from iOSMtrls, which applies `.imCard(padding:)` and
 * hosts a `VStack` with the same alignment and spacing.
 *
 * **Pairing with [AMReorderableCardList]:** `AMListCard` is the canonical cell
 * content type for drag-reorder card lists. Pass an `AMListCard` as the content
 * slot when building an `AMReorderableCardList`.
 *
 * **Stacking guidance:** When showing multiple `AMListCard` instances in a
 * vertical column, use `Arrangement.spacedBy(12.dp)` (`spacing.md`) between
 * cards — the same inter-card gap used in the iOS `IMReorderableCardList`
 * hosting context.
 *
 * **Interaction:** `AMListCard` is visual-only. It does not handle taps or
 * presses. Callers requiring a tappable card should add `Modifier.clickable`
 * (with `indication = amRipple()` for the house monochrome ripple) at the
 * call site.
 *
 * iOS equivalent: `IMListCard` in `Sources/Lists/ListCard.swift`.
 *
 * Usage:
 * ```kotlin
 * AMListCard {
 *     BasicText("Row 1")
 *     BasicText("Row 2")
 * }
 *
 * // Custom padding:
 * AMListCard(padding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
 *     BasicText("Custom inset")
 * }
 *
 * // Stacked cards (12dp gap):
 * Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
 *     AMListCard { /* … */ }
 *     AMListCard { /* … */ }
 * }
 * ```
 *
 * @param padding  Interior padding applied inside the card surface. Defaults to
 *   `tokens.blockInsets` (all 16dp = `spacing.lg`) when `null`. Pass an explicit
 *   [PaddingValues] to override for a single call site.
 * @param modifier Modifier applied to the card's root layout node.
 * @param content  The composable rows to host inside the card's inner [Column].
 *   Receives [ColumnScope] so callers can use `Modifier.weight` and other
 *   column-scoped modifiers on individual rows.
 */
@Composable
fun AMListCard(
    padding: PaddingValues? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAMTokens.current
    val effectivePadding = padding ?: tokens.blockInsets

    AMCard(
        modifier = modifier,
        padding = effectivePadding,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            horizontalAlignment = Alignment.Start,
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun ListCardPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        // Note-style card — headline + body text
        AMListCard(modifier = Modifier.fillMaxWidth()) {
            BasicText(
                text = "Meeting notes",
                style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            )
            BasicText(
                text = "Review the Q3 roadmap and confirm ownership before Thursday's sync.",
                style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
            )
        }

        // Article-style card — headline + metadata caption
        AMListCard(modifier = Modifier.fillMaxWidth()) {
            BasicText(
                text = "Designing for Accessibility",
                style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            )
            BasicText(
                text = "Published · 5 min read",
                style = AMTheme.Type.caption.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
            )
        }

        // Tag-style card — two lines of caption-weight metadata
        AMListCard(modifier = Modifier.fillMaxWidth()) {
            BasicText(
                text = "Sprint 14 — Design Review",
                style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            )
            BasicText(
                text = "In Progress",
                style = AMTheme.Type.caption.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
            )
        }
    }
}

@Preview(
    name = "AMListCard · Light",
    group = "AMListCard",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,  // AMTheme.Surface.page light
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMListCardPreviewLight() {
    AMThemeRoot {
        ListCardPreviewContent()
    }
}

@Preview(
    name = "AMListCard · Dark",
    group = "AMListCard",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,  // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMListCardPreviewDark() {
    AMThemeRoot {
        ListCardPreviewContent()
    }
}
