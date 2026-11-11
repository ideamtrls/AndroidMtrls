package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMTag
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amRowPadding
import kotlin.math.roundToInt

internal val SpacingEntry = ComponentEntry(
    id = "spacing",
    category = CatalogCategory.Foundations,
    title = "Spacing",
    summary = "A 4 dp scale, from 4 to 32",
    icon = R.drawable.ic_space_bar,
    keywords = listOf("AMSpacing", "AMTokens", "spacing scale", "padding", "gap", "4 dp grid", "xs", "sm", "md", "lg", "xl", "xxl"),
) { SpacingPage() }

@Composable
private fun SpacingPage() {
    ComponentPage(
        SpacingEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { SpacingScale() },
    ) {
        ProfileAndSettings()
    }
}

/** Each step of the scale as a bar that wide, largest first, labeled with its size. */
@Composable
private fun SpacingScale() {
    val spacing = AMTheme.tokens.spacing
    FlowRow(
        Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        listOf(spacing.xxl, spacing.xl, spacing.lg, spacing.md, spacing.sm, spacing.xs).forEach { SpacingBar(it) }
    }
}

@Composable
private fun SpacingBar(value: Dp) {
    Column(
        Modifier.widthIn(min = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xs),
    ) {
        Box(
            Modifier
                .size(value, 24.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(AMSemantic.Info.color),
        )
        Text(
            "${value.value.roundToInt()} dp",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A profile card spaced on the scale, then a card of three settings rows. */
@Composable
private fun ProfileAndSettings() {
    val spacing = AMTheme.tokens.spacing
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.xl)) {
        Column(
            Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                AMRemoteImage(
                    catalogPhoto("spacing-ada", 144),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                )
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text("Ada Lovelace", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.ic_location_on),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("Lisbon", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                AMTag("Design")
                AMTag("Travel")
                AMTag("Photography")
            }
        }
        Column(Modifier.fillMaxWidth().amCard()) {
            listOf("Wi‑Fi", "Bluetooth", "Cellular").forEachIndexed { index, title ->
                if (index > 0) AMDivider(inset = spacing.lg)
                Text(title, Modifier.amRowPadding(), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
