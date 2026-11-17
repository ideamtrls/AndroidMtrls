package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMLabeledContent
import com.ideamtrls.androidmtrls.AMTag
import com.ideamtrls.androidmtrls.AMTagTint
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val LabeledContentEntry = ComponentEntry(
    id = "labeledContent",
    category = CatalogCategory.Lists,
    title = "Labeled content",
    summary = "A label and its value on one row",
    icon = R.drawable.ic_format_align_left,
    keywords = listOf("AMLabeledContent", "ListItem", "label", "value", "key value", "detail", "row"),
) { LabeledContentPage() }

@Composable
private fun LabeledContentPage() {
    ComponentPage(
        LabeledContentEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { PlanSummary() },
    ) {
        Subscription()
    }
}

/** A plan and its renewal on a surface strip. */
@Composable
private fun PlanSummary() {
    Column(Modifier.fillMaxWidth().background(AMTheme.Surface.primary)) {
        AMLabeledContent("Plan", "Pro")
        AMDivider(inset = AMTheme.tokens.spacing.lg)
        AMLabeledContent("Renews", "Jan 12, 2027")
    }
}

/**
 * A subscription's details: the plan, its status as a tag and its renewal in a frosted card over a
 * photo; then, in the page's own setting, the whole card. Its last two rows wrap: a short label
 * beside a long address, and a long label beside a short value that stays whole.
 */
@Composable
private fun Subscription() {
    val inset = AMTheme.tokens.spacing.lg
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xl)) {
        CatalogFrostedStage {
            Column(Modifier.padding(AMTheme.tokens.spacing.lg).fillMaxWidth().amCard()) {
                PlanRows()
            }
        }
        Column(Modifier.fillMaxWidth().amCard()) {
            PlanRows()
            AMDivider(inset = inset)
            AMLabeledContent("Seats", "12 of 25")
            AMDivider(inset = inset)
            AMLabeledContent("Billing address", "214 Van Brunt St, Brooklyn, NY 11231")
            AMDivider(inset = inset)
            AMLabeledContent("Charged automatically on the renewal date", "Visa ending 4242")
        }
    }
}

/** The plan, its status as a tag, and when it renews. */
@Composable
private fun PlanRows() {
    val inset = AMTheme.tokens.spacing.lg
    AMLabeledContent("Plan", "Pro")
    AMDivider(inset = inset)
    AMLabeledContent("Status") {
        AMTag("Active", tint = AMTagTint.Success)
    }
    AMDivider(inset = inset)
    AMLabeledContent("Renews", "Jan 12, 2027")
}
