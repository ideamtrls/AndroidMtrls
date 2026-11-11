package com.ideamtrls.androidmtrls.demo

import android.icu.text.ListFormatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.rememberTextMeasurer
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMChipGroup
import com.ideamtrls.androidmtrls.AMDisclosureGroup
import com.ideamtrls.androidmtrls.AMFlowLayout
import com.ideamtrls.androidmtrls.AMPanel
import com.ideamtrls.androidmtrls.AMSection
import com.ideamtrls.androidmtrls.AMSectionHeader
import com.ideamtrls.androidmtrls.AMSegmentedControl
import com.ideamtrls.androidmtrls.AMStepper
import com.ideamtrls.androidmtrls.AMSurfaceFill
import com.ideamtrls.androidmtrls.AMTag
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

/**
 * A product's panel, titled with its name, on the beans: the label large, the name and price, the
 * details and notes, the grind and size for coffee, the quantity and Add to bag, then the guide.
 * Adding leaves the panel as it is. Every product opens on Whole bean, 250 g, a quantity of 1 and a
 * closed guide; the choices are the panel's own saved state. Draws nothing for an unknown id.
 */
@Composable
internal fun ShopProductPanel(model: ShopModel, id: String, onBack: () -> Unit) {
    val product = model.product(id) ?: return
    val spacing = AMTheme.tokens.spacing
    var grind by rememberSaveable { mutableStateOf(ShopGrind.WholeBean) }
    var size by rememberSaveable { mutableStateOf(ShopSize.Small) }
    var quantity by rememberSaveable { mutableIntStateOf(1) }
    var showsGuide by rememberSaveable { mutableStateOf(false) }

    AMBackdrop(art = { ShopBackdrop() }, Modifier.fillMaxSize()) {
        AMPanel(product.name, onBack = onBack, containerColor = Color.Transparent) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = ShopFabClearance),
                verticalArrangement = Arrangement.spacedBy(spacing.xl),
            ) {
                ShopProductHead(product, size)
                ShopProductDetails(product)
                if (product.isCoffee) {
                    ShopChoice("Grind") {
                        AMSegmentedControl(
                            ShopGrind.entries,
                            label = { it.title },
                            selected = grind,
                            onSelectedChange = { grind = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    ShopChoice("Size") {
                        AMChipGroup(ShopSize.entries, label = { it.title }, selected = size, onSelectedChange = { size = it })
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    // The stepper shows no value of its own, so its label spells the count out, as
                    // the bag does; the buttons carry the name, so TalkBack skips the label.
                    Box(Modifier.fillMaxWidth().amCard(fill = AMSurfaceFill.Frosted)) {
                        AMStepper(
                            value = quantity,
                            onValueChange = { quantity = it },
                            valueRange = 1..9,
                            contentDescription = "Quantity",
                        ) {
                            Text("Qty $quantity", Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.bodyLarge.shopFigures())
                        }
                    }
                    AMButton(
                        "Add to bag",
                        onClick = {
                            model.add(
                                product.id,
                                size = if (product.isCoffee) size else null,
                                grind = if (product.isCoffee) grind else null,
                                quantity = quantity,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                AMDisclosureGroup(
                    product.guide.title,
                    expanded = showsGuide,
                    onExpandedChange = { showsGuide = it },
                    fill = AMSurfaceFill.Frosted,
                ) {
                    product.guide.steps.forEachIndexed { index, step ->
                        Text(
                            "${index + 1}. $step",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AMTheme.Frosted.secondaryText,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The hero label in a solid card (the label covers the card's fill, so it floats with the card's
 * shadow), over the name and the price line: "$41.00 · 500 g" for coffee, following the size; the
 * price alone for gear.
 */
@Composable
private fun ShopProductHead(product: ShopProduct, size: ShopSize) {
    val spacing = AMTheme.tokens.spacing
    val price = if (product.isCoffee) "${shopPrice(product.price(size))} · ${size.title}" else shopPrice(product.price(null))
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        ShopLabelArt(product, ShopLabelStyle.Hero, Modifier.amCard())
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(product.name, style = MaterialTheme.typography.titleLarge)
            Text(price, style = MaterialTheme.typography.titleMedium.shopFigures())
        }
    }
}

/**
 * The facts as two columns, the labels as wide as the widest, then the notes as tags, read as one
 * element: "Tasting notes" for coffee, "Features" for gear.
 */
@Composable
private fun ShopProductDetails(product: ShopProduct) {
    val spacing = AMTheme.tokens.spacing
    val style = MaterialTheme.typography.bodyMedium
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelWidth = with(density) {
        product.details.maxOf { measurer.measure(it.label, style).size.width }.toDp()
    }
    AMSection("Details", fill = AMSurfaceFill.Frosted, inset = false) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                product.details.forEach { detail ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                        Text(detail.label, Modifier.width(labelWidth), style = style, color = AMTheme.Frosted.secondaryText)
                        Text(detail.value, Modifier.weight(1f), style = style)
                    }
                }
            }
            val heading = if (product.isCoffee) "Tasting notes" else "Features"
            val notes = ListFormatter.getInstance().format(product.notes)
            AMFlowLayout(Modifier.clearAndSetSemantics { contentDescription = "$heading: $notes" }) {
                product.notes.forEach { note -> AMTag(note) }
            }
        }
    }
}

/** A choice on the beans: its header over its control. */
@Composable
private fun ShopChoice(title: String, control: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.md)) {
        AMSectionHeader(title)
        control()
    }
}
