package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMRowDefaults
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amRowPadding
import kotlin.math.roundToInt

internal val TypographyEntry = ComponentEntry(
    id = "typography",
    category = CatalogCategory.Foundations,
    title = "Typography",
    summary = "Material's type scale in the phone's font",
    icon = R.drawable.ic_text_fields,
    keywords = listOf(
        "MaterialTheme.typography", "type scale", "font", "display", "headline", "title", "body",
        "label", "text style", "type roles", "sentence case", "font scale",
    ),
) { TypographyPage() }

@Composable
private fun TypographyPage() {
    ComponentPage(
        TypographyEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { TypeScale() },
    ) {
        HarborArticle()
    }
}

private class TypeRole(val name: String, val style: (Typography) -> TextStyle)

private val TypeRoles = listOf(
    TypeRole("Display large") { it.displayLarge },
    TypeRole("Display medium") { it.displayMedium },
    TypeRole("Display small") { it.displaySmall },
    TypeRole("Headline large") { it.headlineLarge },
    TypeRole("Headline medium") { it.headlineMedium },
    TypeRole("Headline small") { it.headlineSmall },
    TypeRole("Title large") { it.titleLarge },
    TypeRole("Title medium") { it.titleMedium },
    TypeRole("Title small") { it.titleSmall },
    TypeRole("Body large") { it.bodyLarge },
    TypeRole("Body medium") { it.bodyMedium },
    TypeRole("Body small") { it.bodySmall },
    TypeRole("Label large") { it.labelLarge },
    TypeRole("Label medium") { it.labelMedium },
    TypeRole("Label small") { it.labelSmall },
)

/** Every role of the type scale: a sample in the role, over its name and size. */
@Composable
private fun TypeScale() {
    val typography = MaterialTheme.typography
    Column(Modifier.fillMaxWidth().background(AMTheme.Surface.primary)) {
        TypeRoles.forEachIndexed { index, role ->
            if (index > 0) AMDivider(inset = AMTheme.tokens.spacing.lg)
            TypeRow(role.name, role.style(typography))
        }
    }
}

@Composable
private fun TypeRow(name: String, style: TextStyle) {
    Column(Modifier.amRowPadding(), verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xs)) {
        // The larger roles shrink to keep the sample on one line.
        Text(
            "The quick brown fox",
            style = style,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = style.fontSize),
        )
        Text(
            "$name · ${style.fontSize.value.roundToInt()} sp",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A travel story's card: its kicker, title, standfirst and body, then an album row under it. */
@Composable
private fun HarborArticle() {
    val spacing = AMTheme.tokens.spacing
    val type = MaterialTheme.typography
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth().amCard()) {
        Column(
            Modifier.fillMaxWidth().padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text("Travel", style = type.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Harbor at dawn", style = type.headlineSmall)
            Text("A walk through the old port", style = type.titleSmall, color = secondary)
            Text(
                "Photographed over three mornings in March, from the fish market to the lighthouse at the end of the breakwater.",
                Modifier.padding(top = spacing.sm),
                style = type.bodyLarge,
            )
        }
        AMDivider(inset = spacing.lg)
        ListItem(
            supportingContent = { Text("12 photos · Shared") },
            trailingContent = { Text("Mar 14", style = type.bodySmall) },
            colors = AMRowDefaults.colors(),
            shapes = AMRowDefaults.shapes(),
            contentPadding = AMRowDefaults.contentPadding,
        ) {
            Text("Lisbon trip")
        }
    }
}
