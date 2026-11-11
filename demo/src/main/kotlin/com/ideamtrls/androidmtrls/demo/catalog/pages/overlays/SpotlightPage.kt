package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMSpotlight
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amHoldable
import com.ideamtrls.androidmtrls.amRowPadding

internal val SpotlightEntry = ComponentEntry(
    id = "spotlight",
    category = CatalogCategory.Overlays,
    title = "Spotlight",
    summary = "One item up close, solid, over the blur",
    icon = R.drawable.ic_center_focus_strong,
    keywords = listOf(
        "AMSpotlight", "amHoldable", "press and hold", "long press", "hold", "peek", "preview", "focus",
        "context menu", "haptic", "picker",
    ),
) { SpotlightPage() }

@Composable
private fun SpotlightPage() {
    ComponentPage(
        SpotlightEntry,
        preview = { ReadingList() },
    ) {
        ThisWeek()
    }
}

// Stand-ins for your own actions.
private fun share() {}

private fun delete() {}

private fun addPlan() {}

private fun openDay() {}

@Immutable
private class SpotlightArticle(val title: String, val source: String)

private val Articles = listOf(
    SpotlightArticle("Reasoning about reasoning", "Latent Notes"),
    SpotlightArticle("Cache-warm policies", "Distributed Diaries"),
    SpotlightArticle("Long context vs. retrieval", "The Inference Hour"),
)

@Immutable
private class SpotlightDay(val name: String, val plans: List<String>)

private val Week = listOf(
    SpotlightDay("Monday", listOf("Stand-up", "Design review")),
    SpotlightDay("Tuesday", listOf("Lunch with Priya")),
    SpotlightDay("Wednesday", listOf("Team lunch", "Piano recital")),
)

/** Hold an article to bring it up close, over Share and Delete. */
@Composable
private fun ReadingList() {
    var held by remember { mutableStateOf<SpotlightArticle?>(null) }
    val inset = AMTheme.tokens.spacing.lg
    Column(Modifier.fillMaxWidth().amCard()) {
        Articles.forEachIndexed { index, article ->
            if (index > 0) AMDivider(inset = inset)
            TitledRow(
                article.title,
                article.source,
                Modifier.amHoldable(
                    onHold = { held = article },
                    customActions = listOf(
                        CustomAccessibilityAction("Share") { share(); true },
                        CustomAccessibilityAction("Delete") { delete(); true },
                    ),
                ),
            )
        }
    }
    AMSpotlight(
        held,
        onDismissRequest = { held = null },
        actions = {
            AMActionGridRow(
                {
                    AMActionGridButton(painterResource(R.drawable.ic_share), "Share", onClick = {
                        share()
                        held = null
                    })
                },
                {
                    AMActionGridButton(painterResource(R.drawable.ic_delete), "Delete", onClick = {
                        delete()
                        held = null
                    }, destructive = true)
                },
            )
        },
    ) { article ->
        TitledRow(article.title, article.source)
    }
}

/** Hold a day to see its plans under the day's name. */
@Composable
private fun ThisWeek() {
    var held by remember { mutableStateOf<SpotlightDay?>(null) }
    val inset = AMTheme.tokens.spacing.lg
    Column(Modifier.fillMaxWidth().amCard()) {
        Week.forEachIndexed { index, day ->
            if (index > 0) AMDivider(inset = inset)
            TitledRow(
                day.name,
                if (day.plans.size == 1) "1 planned" else "${day.plans.size} planned",
                Modifier.amHoldable(
                    onHold = { held = day },
                    customActions = listOf(
                        CustomAccessibilityAction("Add plan") { addPlan(); true },
                        CustomAccessibilityAction("Open day") { openDay(); true },
                    ),
                ),
            )
        }
    }
    AMSpotlight(
        held,
        onDismissRequest = { held = null },
        title = { it.name },
        actions = {
            AMActionGridRow(
                {
                    AMActionGridButton(painterResource(R.drawable.ic_add), "Add plan", onClick = {
                        addPlan()
                        held = null
                    })
                },
                {
                    AMActionGridButton(painterResource(R.drawable.ic_calendar_today), "Open day", onClick = {
                        openDay()
                        held = null
                    })
                },
            )
        },
    ) { day ->
        Column(Modifier.fillMaxWidth()) {
            day.plans.forEach { plan ->
                Text(plan, Modifier.amRowPadding(), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** A hand-built row on the row recipe: a title over a supporting line. */
@Composable
private fun TitledRow(title: String, supporting: String, modifier: Modifier = Modifier) {
    Column(modifier.amRowPadding()) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
