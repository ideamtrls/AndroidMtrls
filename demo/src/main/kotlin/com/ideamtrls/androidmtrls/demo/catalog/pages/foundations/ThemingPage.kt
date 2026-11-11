package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMSurfaceStyle
import com.ideamtrls.androidmtrls.AMTag
import com.ideamtrls.androidmtrls.AMTagTint
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.ProvideAMSurfaceStyle
import com.ideamtrls.androidmtrls.amCard

internal val ThemingEntry = ComponentEntry(
    id = "theming",
    category = CatalogCategory.Foundations,
    title = "Theming",
    summary = "Re-tune colors, shapes and spacing, keep the look",
    icon = R.drawable.ic_tune,
    keywords = listOf(
        "AMTheme", "AMTokens", "AMSpacing", "AMSemanticTints", "MaterialTheme", "colorScheme", "shapes",
        "brand", "override", "subtree", "dark mode", "AMSurfaceStyle",
    ),
) { ThemingPage() }

@Composable
private fun ThemingPage() {
    ComponentPage(
        ThemingEntry,
        preview = {
            Row(horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.lg), verticalAlignment = Alignment.Top) {
                TokenSpecimen(Modifier.weight(1f))
                val half = Modifier.weight(1f)
                BrandTheme(specimen = true) { TokenSpecimen(half) }
            }
        },
    ) {
        BrandTheme { FieldNotes() }
    }
}

/**
 * A brand's theme for a subtree: its own static teal scheme and re-tuned corners through Material.
 * The [specimen] beside the default one squares its cards' corners and widens the small spacing
 * step through the tokens, so the change shows side by side; the notebook takes the brand's
 * tighter corners. The surfaces, shadows and hairlines stay the same.
 */
@Composable
private fun BrandTheme(specimen: Boolean = false, content: @Composable () -> Unit) {
    val tokens = AMTheme.tokens
    val shapes = if (specimen) MaterialTheme.shapes.copy(large = RoundedCornerShape(4.dp)) else BrandShapes
    AMTheme(
        colorScheme = if (AMTheme.isDark) BrandDark else BrandLight,
        tokens = if (specimen) tokens.copy(spacing = tokens.spacing.copy(sm = 16.dp)) else tokens,
    ) {
        MaterialTheme(shapes = shapes, content = content)
    }
}

// A sample brand: a static teal scheme and tighter corners.
private val BrandLight = lightColorScheme(
    primary = Color(0xFF006A6A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF6FF7F6),
    onPrimaryContainer = Color(0xFF002020),
    secondaryContainer = Color(0xFFCCE8E7),
    onSecondaryContainer = Color(0xFF051F1F),
)

private val BrandDark = darkColorScheme(
    primary = Color(0xFF4CDADA),
    onPrimary = Color(0xFF003737),
    primaryContainer = Color(0xFF005050),
    onPrimaryContainer = Color(0xFF6FF7F6),
    secondaryContainer = Color(0xFF324B4B),
    onSecondaryContainer = Color(0xFFCCE8E7),
)

private val BrandShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(10.dp),
)

// Stand-in for your own action.
private fun pick() {}

/** A card of theme readers: an info tag (the accent), chips (corners and the small spacing step). */
@Composable
private fun TokenSpecimen(modifier: Modifier = Modifier) {
    val spacing = AMTheme.tokens.spacing
    Column(
        modifier.amCard(padding = PaddingValues(spacing.lg)),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AMTag("Info", icon = AMSemantic.Info.icon, tint = AMTagTint.Info)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            listOf("Kotlin", "UI", "Kit").forEachIndexed { index, label ->
                FilterChip(selected = index == 0, onClick = ::pick, label = { Text(label) })
            }
        }
    }
}

/** A shared notebook's card in the brand: its title and a beta tag, its shelves, a pinned note lifted inside. */
@Composable
private fun FieldNotes() {
    val spacing = AMTheme.tokens.spacing
    var shelves by remember { mutableStateOf(setOf("Sketches")) }
    Column(
        Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Field notes", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Shared with 4 people",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AMTag("Beta", icon = painterResource(R.drawable.ic_auto_awesome), tint = AMTagTint.Info)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            listOf("Sketches", "Maps", "Recipes").forEach { shelf ->
                FilterChip(
                    selected = shelf in shelves,
                    onClick = { shelves = if (shelf in shelves) shelves - shelf else shelves + shelf },
                    label = { Text(shelf) },
                )
            }
        }
        // Lifted again inside the notebook's card, so the pinned note floats on it.
        ProvideAMSurfaceStyle(AMSurfaceStyle.Elevated) {
            Row(
                Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_push_pin), contentDescription = null, Modifier.size(20.dp))
                Text("Harbor sketches, edited today", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
