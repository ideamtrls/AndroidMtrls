package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMEmptyState
import com.ideamtrls.androidmtrls.AMEmptyStateAction
import com.ideamtrls.androidmtrls.AMPanel
import com.ideamtrls.androidmtrls.AMSection
import com.ideamtrls.androidmtrls.AMSectionStyle
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMStepper
import com.ideamtrls.androidmtrls.AMSurfaceFill
import com.ideamtrls.androidmtrls.AMSwipeAction
import com.ideamtrls.androidmtrls.AMSwipeableRow
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amInstantSwitch

/**
 * The bag, in two forms. As the root (the remote's Bag tile) it carries its page header; as a
 * panel ([onBack] not null, pushed by the shop's bag button) the panel's bar titles it, back
 * returns to the shop, and it brings the beans' backdrop. Either way: a row per line (step the
 * quantity, swipe to remove), the Standard-shipping summary and Checkout; an empty bag shows an
 * empty state whose Browse goes back to the shop.
 */
@Composable
internal fun ShopBag(model: ShopModel, onBack: (() -> Unit)?) {
    if (onBack == null) {
        ShopRootPage("Bag") { padding -> ShopBagContent(model, padding, onBrowse = { amInstantSwitch { model.currentView = ShopView.Shop } }) }
    } else {
        AMBackdrop(art = { ShopBackdrop() }, Modifier.fillMaxSize()) {
            AMPanel("Bag", onBack = onBack, containerColor = Color.Transparent) { padding ->
                ShopBagContent(model, padding, onBrowse = onBack)
            }
        }
    }
}

@Composable
private fun ShopBagContent(model: ShopModel, padding: PaddingValues, onBrowse: () -> Unit) {
    val spacing = AMTheme.tokens.spacing
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = ShopFabClearance),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        if (model.bag.isEmpty()) {
            AMEmptyState(
                painterResource(R.drawable.ic_shopping_bag),
                "Your bag is empty",
                description = "Coffee and gear you add show up here.",
                action = AMEmptyStateAction("Browse", onClick = onBrowse),
            )
        } else {
            AMSection("Items", style = AMSectionStyle.Rows, fill = AMSurfaceFill.Frosted, inset = false) {
                model.bag.forEach { line ->
                    key(line.id) {
                        AMSwipeableRow(
                            endAction = AMSwipeAction(
                                "Remove",
                                painterResource(R.drawable.ic_delete),
                                AMSemantic.Destructive,
                                removesRow = true,
                            ) { model.remove(line.id) },
                        ) { ShopBagLineRow(line, model) }
                    }
                }
            }
            AMSection(
                "Summary",
                footer = "Standard shipping is free over $50.",
                style = AMSectionStyle.Rows,
                fill = AMSurfaceFill.Frosted,
                inset = false,
            ) {
                ShopTotalsRows(model.bagSubtotal, ShopDelivery.Standard.shipping(model.bagSubtotal))
            }
            AMButton("Checkout", onClick = model::beginCheckout, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * One bag line: the label thumb beside the name over "500 g · Whole bean" and "Qty 2 · $82.00"
 * (gear: the count line alone), with the quantity stepper at the end. The stepper shows no value,
 * so the count line spells it out; its buttons are named by the product ("Increase Kenya
 * Gatomboya"). The row reads its lines to TalkBack, with Remove as its action.
 */
@Composable
private fun ShopBagLineRow(line: ShopLine, model: ShopModel) {
    val product = model.product(line.productId) ?: return
    AMStepper(
        value = line.quantity,
        onValueChange = { model.setQuantity(line.id, it) },
        valueRange = 1..99,
        contentDescription = product.name,
    ) {
        ShopLabelArt(product, ShopLabelStyle.Thumb)
        Column {
            Text(product.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val caption = MaterialTheme.typography.bodySmall.shopFigures()
            val secondary = AMTheme.Frosted.secondaryText
            if (line.detail.isNotEmpty()) Text(line.detail, style = caption, color = secondary)
            Text("Qty ${line.quantity} · ${shopPrice(line.total)}", style = caption, color = secondary)
        }
    }
}
