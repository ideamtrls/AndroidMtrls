package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amHairline

internal val HairlineEntry = ComponentEntry(
    id = "hairline",
    category = CatalogCategory.Foundations,
    title = "Hairline",
    summary = "A half-dp outline drawn inside a shape",
    icon = R.drawable.ic_crop_square,
    keywords = listOf("amHairline", "hairlineWidth", "selectionRing", "selectionRingWidth", "outline", "border", "ring"),
) { HairlinePage() }

@Composable
private fun HairlinePage() {
    ComponentPage(
        HairlineEntry,
        preview = {
            val shape = MaterialTheme.shapes.large
            Box(Modifier.size(160.dp, 84.dp).background(AMTheme.Surface.primary, shape).amHairline(shape))
        },
    ) {
        PlanPicker()
    }
}

// Stand-ins for your own actions.
private fun close() {}

private fun restore() {}

/** The top of a plan picker: a close button and a restore button, both outlined, over two plans. */
@Composable
private fun PlanPicker() {
    val spacing = AMTheme.tokens.spacing
    var yearly by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .background(AMTheme.Surface.primary, CircleShape)
                    .amHairline(CircleShape, color = MaterialTheme.colorScheme.outlineVariant)
                    .clip(CircleShape)
                    .clickable(onClickLabel = null, role = Role.Button, onClick = ::close),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = "Close")
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .minimumInteractiveComponentSize()
                    .background(AMTheme.Surface.primary, MaterialTheme.shapes.medium)
                    .amHairline(MaterialTheme.shapes.medium)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(role = Role.Button, onClick = ::restore)
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text("Restore", style = MaterialTheme.typography.labelLarge)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            PlanTile("Monthly", "$4.99", selected = !yearly, onClick = { yearly = false }, Modifier.weight(1f))
            PlanTile("Yearly", "$49.99", selected = yearly, onClick = { yearly = true }, Modifier.weight(1f))
        }
    }
}

/** A plan: a hairline at rest, the wider selection ring when chosen. */
@Composable
private fun PlanTile(name: String, price: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier
            .background(AMTheme.Surface.primary, shape)
            .amHairline(shape, width = if (selected) AMTheme.selectionRingWidth else AMTheme.hairlineWidth)
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(AMTheme.tokens.spacing.lg),
    ) {
        Text(name, style = MaterialTheme.typography.titleMedium)
        Text(price, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
