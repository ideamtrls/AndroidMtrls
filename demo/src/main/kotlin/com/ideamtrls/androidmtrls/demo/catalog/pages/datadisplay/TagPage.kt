package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMFlowLayout
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMTag
import com.ideamtrls.androidmtrls.AMTagTint
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amRowPadding

internal val TagEntry = ComponentEntry(
    id = "tag",
    category = CatalogCategory.DataDisplay,
    title = "Tag",
    summary = "Small pill for a status or a category",
    icon = R.drawable.ic_sell,
    keywords = listOf("AMTag", "AMTagTint", "AMFlowLayout", "pill", "status", "label", "category", "semantic", "wrap"),
) { TagPage() }

@Composable
private fun TagPage() {
    ComponentPage(
        TagEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { PreviewTags() },
    ) {
        ReleaseStatus()
    }
}

/** A neutral tag and two statuses on a surface strip; the row wraps when the text is large. */
@Composable
private fun PreviewTags() {
    AMFlowLayout(Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(AMTheme.tokens.spacing.lg)) {
        AMTag("Draft")
        AMTag("Active", icon = AMSemantic.Success.icon, tint = AMTagTint.Success)
        AMTag("Blocked", icon = painterResource(R.drawable.ic_dangerous_filled), tint = AMTagTint.Destructive)
    }
}

private val TagTopics = listOf("Payments", "Search", "Onboarding", "Android", "Web", "API")

/** A release's status board: each item's state as a tag, the build in monospace, then its topics. */
@Composable
private fun ReleaseStatus() {
    val inset = AMTheme.tokens.spacing.lg
    Column(Modifier.fillMaxWidth().amCard()) {
        AMListRow(
            "Spring launch",
            subtitle = "Ships Friday",
            trailingContent = { AMTag("Live", icon = AMSemantic.Success.icon, tint = AMTagTint.Success) },
        )
        AMDivider(inset = inset)
        AMListRow("Build", trailingContent = { AMTag { Text("v2.4.1", fontFamily = FontFamily.Monospace) } })
        AMDivider(inset = inset)
        AMListRow(
            "Checkout flow",
            subtitle = "Payments",
            trailingContent = { AMTag("Delayed", icon = painterResource(R.drawable.ic_schedule), tint = AMTagTint.Warning) },
        )
        AMDivider(inset = inset)
        AMListRow(
            "Search index",
            subtitle = "Search",
            trailingContent = { AMTag("Failed", icon = painterResource(R.drawable.ic_close), tint = AMTagTint.Destructive) },
        )
        AMDivider(inset = inset)
        AMListRow(
            "Welcome screens",
            subtitle = "Onboarding",
            trailingContent = { AMTag("In review", icon = painterResource(R.drawable.ic_visibility), tint = AMTagTint.Info) },
        )
        AMDivider(inset = inset)
        AMFlowLayout(Modifier.amRowPadding()) {
            TagTopics.forEach { topic -> AMTag(topic) }
        }
    }
}
