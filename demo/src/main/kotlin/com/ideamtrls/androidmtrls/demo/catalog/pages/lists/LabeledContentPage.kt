package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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

/** A subscription's details in a card, its status as a tag. */
@Composable
private fun Subscription() {
    val inset = AMTheme.tokens.spacing.lg
    Column(Modifier.fillMaxWidth().amCard()) {
        AMLabeledContent("Plan", "Pro")
        AMDivider(inset = inset)
        AMLabeledContent("Status") {
            AMTag("Active", tint = AMTagTint.Success)
        }
        AMDivider(inset = inset)
        AMLabeledContent("Renews", "Jan 12, 2027")
        AMDivider(inset = inset)
        AMLabeledContent("Seats", "12 of 25")
    }
}
