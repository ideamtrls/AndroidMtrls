package io.github.jmsdnns.androidmtrls.demo.pages.overlays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMPrimaryButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMSecondaryButton
import io.github.jmsdnns.androidmtrls.controls.inputs.AMTextField
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMSheet
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMSheetHeight
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.lists.AMListRow
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for the [AMSheet] host composable.
 *
 * id: "overlays-sheet", section: [ComponentSection.Overlays].
 */
val sheetEntry: ComponentEntry = ComponentEntry(
    id       = "overlays-sheet",
    section  = ComponentSection.Overlays,
    title    = "Sheet",
    subtitle = "Bottom sheet with drag dismiss",
    intro    = "AMSheet wraps scene content and presents a floating card from the bottom " +
        "of the screen. The underlying content stays visible — no blur, no dark scrim. " +
        "Full height (default) fills the screen below the status bar; Content height wraps " +
        "its content. Focus the text field to raise the keyboard — the sheet content rides " +
        "above it. Dismiss by tapping outside the card or dragging it down past 60dp. " +
        "Attach at the scene root so the card and tap-catcher cover the full screen.",
    icon     = Icons.Filled.WebAsset,
    content  = { SheetDemoPage() },
)

// ---------------------------------------------------------------------------
// SheetDemoPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMSheet].
 *
 * Wraps the page body in [AMSheet] so the sheet overlays the full content
 * area — the correct attachment pattern (attach at the scene root; comment
 * here mirrors the KDoc advice in Sheet.kt).
 *
 * The "Height" group shows two [AMSecondaryButton] triggers, one per
 * [AMSheetHeight] mode — "Show Content Sheet" wraps the card to its content,
 * "Show Full Sheet" fills the screen below the status bar. Both set
 * [isPresented] to `true` after selecting the [height]. The sheet body contains
 * a title, supporting text, an [AMTextField] (to exercise the keyboard IME
 * inset), and a [AMPrimaryButton] "Close" that sets [isPresented] back to
 * `false`.
 */
@Composable
fun SheetDemoPage() {
    // Sheet presentation + height state — rememberSaveable so both survive config
    // changes (screen rotation) while the sheet is open, matching the house rule
    // in conventions.md §Gesture rules (self-managed state uses rememberSaveable).
    var isPresented by rememberSaveable { mutableStateOf(false) }
    var height by rememberSaveable { mutableStateOf(AMSheetHeight.Full) }

    // AMSheet wraps hostContent — attach here (page level) so the overlay
    // covers the entire demo page content, matching the scene-root pattern.
    AMSheet(
        isPresented  = isPresented,
        onDismiss    = { isPresented = false },
        height       = height,
        sheetContent = { SheetDemoContent(height = height, onClose = { isPresented = false }) },
    ) {
        // ── Page body (hostContent) ───────────────────────────────────────
        // iOS group "Heights"; "Full height" then "Content height", inset by the
        // lg page margin (Pages/Overlays.swift).
        catalogGroup(title = "Heights") {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AMTheme.Spacing.lg.dp),
                verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
            ) {
                AMSecondaryButton(
                    label    = "Full height",
                    onClick  = {
                        height = AMSheetHeight.Full
                        isPresented = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                AMSecondaryButton(
                    label    = "Content height",
                    onClick  = {
                        height = AMSheetHeight.Content
                        isPresented = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// SheetDemoContent — content rendered inside the sheet card
// ---------------------------------------------------------------------------

/**
 * The content rendered inside the [AMSheet] card body for the demo page.
 *
 * Title (naming the active [height] mode), body text, an [AMTextField] to raise
 * the keyboard (demonstrating the sheet's IME inset), and a Close button.
 *
 * @param height  The active sheet height mode — named in the title so the two
 *                triggers read distinctly.
 * @param onClose Called when the "Close" button is tapped. Caller sets
 *                [isPresented] to `false` in response.
 */
@Composable
private fun SheetDemoContent(height: AMSheetHeight, onClose: () -> Unit) {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    // Caller-held field text — a plain String is saveable, so rememberSaveable
    // keeps what was typed across rotation while the sheet is open.
    var fieldText by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.lg),
    ) {
        Spacer(modifier = Modifier.height(tokens.spacing.md))

        // Title — house Type.title role
        BasicText(
            text  = when (height) {
                AMSheetHeight.Full    -> "Full-height sheet"
                AMSheetHeight.Content -> "Content-height sheet"
            },
            style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
        )

        Spacer(modifier = Modifier.height(tokens.spacing.sm))

        // Body text — house Type.caption role. Content mode uses the iOS
        // content-sheet copy; Full mode notes the keyboard-inset demo.
        BasicText(
            text  = when (height) {
                AMSheetHeight.Content -> "Sizes to its content and rides the bottom edge."
                AMSheetHeight.Full    -> "Focus the field to raise the keyboard — content rides " +
                    "above it. Drag down or tap outside to dismiss."
            },
            style = AMTheme.Type.caption.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
        )

        Spacer(modifier = Modifier.height(tokens.spacing.lg))

        // Text field — exercises the sheet's navigationBarsPadding + imePadding
        // so the field stays visible above the on-screen keyboard.
        AMTextField(
            value         = fieldText,
            onValueChange = { fieldText = it },
            placeholder   = "Type to test the keyboard inset",
        )

        Spacer(modifier = Modifier.height(tokens.spacing.lg))

        // Full mode only: a top-level scroll container. This exercises the
        // weighted-Box bound in AMSheet's Full mode — without it this
        // verticalScroll would crash with "measured with an infinity maximum
        // height" (the Wave 2R Sheet fix; Panel.kt precedent).
        if (height == AMSheetHeight.Full) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                repeat(20) { n ->
                    AMListRow(
                        title    = "Scrollable row ${n + 1}",
                        subtitle = "Full-height sheets host scroll containers",
                    )
                }
            }
            Spacer(modifier = Modifier.height(tokens.spacing.lg))
        }

        // Close button — primary style matches iOS `.buttonStyle(.imPrimary)`
        AMPrimaryButton(
            label    = "Close",
            onClick  = onClose,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
