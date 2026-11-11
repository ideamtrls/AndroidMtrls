package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMRowDefaults
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amHairline

internal val ColorEntry = ComponentEntry(
    id = "color",
    category = CatalogCategory.Foundations,
    title = "Color",
    summary = "Surfaces, the page, text, accents and tints",
    icon = R.drawable.ic_palette,
    keywords = listOf(
        "AMTheme", "Surface", "primary", "page", "selectionRing", "controlOutline", "AMSemantic",
        "colorScheme", "onSurface", "success", "warning", "error", "dark mode", "tint",
    ),
) { ColorPage() }

@Composable
private fun ColorPage() {
    ComponentPage(
        ColorEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { ColorSwatches() },
    ) {
        BackupStatus()
    }
}

private class Swatch(val name: String, val color: Color, val note: String? = null, val glyph: Boolean = false)

/** The style layer's surfaces and selection ring, and the scheme's text, accent and divider, one row each. */
@Composable
private fun ColorSwatches() {
    val scheme = MaterialTheme.colorScheme
    val swatches = listOf(
        Swatch("Floating surface", AMTheme.Surface.primary, "Cards, buttons and sheets: white, then black"),
        Swatch("Page", AMTheme.Surface.page, "Behind them: gray, then off-black"),
        Swatch("Text", scheme.onSurface, glyph = true),
        Swatch("Secondary text", scheme.onSurfaceVariant, glyph = true),
        Swatch("Accent", scheme.primary, glyph = true),
        Swatch("Selection ring", AMTheme.selectionRing, "Hairlines and chosen items"),
        Swatch("Divider", scheme.outlineVariant, "Between rows"),
    )
    Column(Modifier.fillMaxWidth().background(AMTheme.Surface.primary)) {
        swatches.forEachIndexed { index, swatch ->
            if (index > 0) AMDivider(inset = AMTheme.tokens.spacing.lg)
            SwatchRow(swatch)
        }
    }
}

@Composable
private fun SwatchRow(swatch: Swatch) {
    ListItem(
        leadingContent = { SwatchChip(swatch) },
        supportingContent = swatch.note?.let { note -> { Text(note) } },
        colors = AMRowDefaults.colors(),
        shapes = AMRowDefaults.shapes(),
        contentPadding = AMRowDefaults.contentPadding,
    ) {
        Text(swatch.name)
    }
}

/** A chip of the color, or "Aa" written in it, outlined like a divider. */
@Composable
private fun SwatchChip(swatch: Swatch) {
    val shape = MaterialTheme.shapes.small
    Box(
        Modifier
            .size(48.dp, 34.dp)
            .background(if (swatch.glyph) AMTheme.Surface.primary else swatch.color, shape)
            .amHairline(shape, color = MaterialTheme.colorScheme.outlineVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (swatch.glyph) {
            Text("Aa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = swatch.color)
        }
    }
}

/** A backup settings card: each service's status in its semantic tint, one service switched off. */
@Composable
private fun BackupStatus() {
    val inset = AMTheme.tokens.spacing.lg
    Column(Modifier.fillMaxWidth().amCard()) {
        BackupRow(AMSemantic.Success.icon, AMSemantic.Success.color, "Photos", "Backed up 5 minutes ago")
        AMDivider(inset = inset)
        BackupRow(painterResource(R.drawable.ic_sync), AMSemantic.Info.color, "Contacts", "Syncing now")
        AMDivider(inset = inset)
        BackupRow(AMSemantic.Warning.icon, AMSemantic.Warning.color, "Documents", "Storage almost full")
        AMDivider(inset = inset)
        BackupRow(AMSemantic.Destructive.icon, AMSemantic.Destructive.color, "Messages", "Last backup failed")
        AMDivider(inset = inset)
        BackupRow(painterResource(R.drawable.ic_cloud_off), null, "Mail", "Backup off", enabled = false)
    }
}

@Composable
private fun BackupRow(icon: Painter, tint: Color?, title: String, status: String, enabled: Boolean = true) {
    ListItem(
        enabled = enabled,
        leadingContent = {
            if (tint != null && enabled) Icon(icon, contentDescription = null, tint = tint) else Icon(icon, contentDescription = null)
        },
        trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null) },
        supportingContent = { Text(status) },
        colors = AMRowDefaults.colors(),
        shapes = AMRowDefaults.shapes(),
        contentPadding = AMRowDefaults.contentPadding,
    ) {
        Text(title)
    }
}
