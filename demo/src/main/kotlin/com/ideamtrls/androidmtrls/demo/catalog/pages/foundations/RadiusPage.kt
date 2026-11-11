package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMElevation
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amElevation
import kotlin.math.roundToInt

internal val RadiusEntry = ComponentEntry(
    id = "radius",
    category = CatalogCategory.Foundations,
    title = "Radius",
    summary = "Material's small, medium and large corners",
    icon = R.drawable.ic_rounded_corner,
    keywords = listOf("MaterialTheme.shapes", "shapes", "corner radius", "RoundedCornerShape", "small", "medium", "large", "corners"),
) { RadiusPage() }

@Composable
private fun RadiusPage() {
    ComponentPage(
        RadiusEntry,
        preview = { CornerScale() },
    ) {
        WeekendTrip()
    }
}

/** The three corner sizes, each on a floating square, largest first. */
@Composable
private fun CornerScale() {
    val shapes = MaterialTheme.shapes
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xl),
    ) {
        CornerChip("Large", shapes.large)
        CornerChip("Medium", shapes.medium)
        CornerChip("Small", shapes.small)
    }
}

@Composable
private fun CornerChip(name: String, shape: CornerBasedShape) {
    val side = 76.dp
    val density = LocalDensity.current
    val radius = with(density) {
        val sidePx = side.toPx()
        shape.topStart.toPx(Size(sidePx, sidePx), density).toDp()
    }
    Column(
        Modifier
            .defaultMinSize(side, side)
            .amElevation(AMElevation.Low, shape)
            .background(AMTheme.Surface.primary, shape)
            .padding(AMTheme.tokens.spacing.sm),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(name, style = MaterialTheme.typography.labelLarge)
        Text(
            "${radius.value.roundToInt()} dp",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A trip card in the large corners, with a draft tag in the small and a check-in block in the medium. */
@Composable
private fun WeekendTrip() {
    val spacing = AMTheme.tokens.spacing
    Column(
        Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Weekend trip", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Lisbon · 3 nights",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "Draft",
                Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = spacing.md, vertical = spacing.xs),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_calendar_today), contentDescription = null, Modifier.size(20.dp))
            Text("Check-in Friday, 3 PM", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
