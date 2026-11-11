package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMFormActions
import com.ideamtrls.androidmtrls.AMLabeledContent
import com.ideamtrls.androidmtrls.AMSection
import com.ideamtrls.androidmtrls.AMSectionHeader
import com.ideamtrls.androidmtrls.AMSectionStyle
import com.ideamtrls.androidmtrls.AMSegmentedControl
import com.ideamtrls.androidmtrls.AMTextField
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amDiscardGuard

/**
 * The Checkout sheet's content, under the sheet's "Checkout" title: where it ships, how it ships
 * and what it costs, with Place order pinned under them. The ship-to and delivery are the sheet's
 * own draft, so each opening starts fresh (filled in under the `shopFillsCheckout` debug extra).
 * Once the draft changes, a drag, a tap outside or back asks before dropping it; Place order
 * closes from code and never asks. The totals read the bag as Checkout opened, so they hold while
 * the sheet slides away after the order is placed.
 */
@Composable
internal fun ColumnScope.ShopCheckoutSheet(model: ShopModel) {
    val spacing = AMTheme.tokens.spacing
    val openedWith = if (LaunchOptions.shopFillsCheckout) ShopShipTo.Sample else ShopShipTo.Empty
    val name = rememberTextFieldState(openedWith.name)
    val street = rememberTextFieldState(openedWith.street)
    val city = rememberTextFieldState(openedWith.city)
    val zip = rememberTextFieldState(openedWith.zip)
    var delivery by rememberSaveable { mutableStateOf(ShopDelivery.Standard) }
    val shipTo = ShopShipTo(name.text.toString(), street.text.toString(), city.text.toString(), zip.text.toString())

    Column(
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .amDiscardGuard(shipTo != openedWith || delivery != ShopDelivery.Standard),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            // A card of outlined fields, named by their labels; the keyboard's Next moves down.
            AMSection("Ship to", inset = false) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    ShopAddressField(name, "Full name")
                    ShopAddressField(street, "Street address")
                    ShopAddressField(city, "City and state")
                    AMTextField(
                        zip,
                        label = "ZIP code",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    )
                }
            }
            // The segmented buttons carry their own outline, so they sit bare, the promise under them.
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                AMSectionHeader("Delivery")
                AMSegmentedControl(
                    ShopDelivery.entries,
                    label = { it.title },
                    selected = delivery,
                    onSelectedChange = { delivery = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    delivery.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AMSection("Summary", style = AMSectionStyle.Rows, inset = false) {
                AMLabeledContent("Payment", "Card ending 4242")
                ShopTotalsRows(model.checkoutSubtotal, delivery.shipping(model.checkoutSubtotal))
            }
        }
        AMFormActions(Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.md, bottom = spacing.lg)) {
            AMButton(
                "Place order",
                onClick = { model.placeOrder(shipTo, delivery) },
                enabled = shipTo.isComplete,
            )
        }
    }
}

/** One of the address's words-capitalized fields, Next moving to the one below. */
@Composable
private fun ShopAddressField(state: TextFieldState, label: String) {
    AMTextField(
        state,
        label = label,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
    )
}
