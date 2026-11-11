package com.ideamtrls.androidmtrls.demo

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMLabeledContent
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMPanel
import com.ideamtrls.androidmtrls.AMSection
import com.ideamtrls.androidmtrls.AMSectionStyle
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMSurfaceFill
import com.ideamtrls.androidmtrls.AMTag
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMTimeline
import com.ideamtrls.androidmtrls.AMTimelineDot
import com.ideamtrls.androidmtrls.AMTimelineLabel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Orders root: the orders in progress, then the delivered ones, each group shown only while it
 * holds orders. A row pushes the order's panel.
 */
@Composable
internal fun ShopOrders(model: ShopModel) {
    val spacing = AMTheme.tokens.spacing
    val dates = rememberShopDates()
    val now = LocalDateTime.now()
    val (delivered, inProgress) = model.orders.partition { it.status(now) == ShopOrderStatus.Delivered }
    ShopRootPage("Orders") { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = ShopFabClearance),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            listOf("In progress" to inProgress, "Delivered" to delivered).forEach { (title, orders) ->
                if (orders.isNotEmpty()) {
                    AMSection(title, style = AMSectionStyle.Rows, fill = AMSurfaceFill.Frosted, inset = false) {
                        orders.forEach { order -> ShopOrderRow(order, model, dates, now) }
                    }
                }
            }
        }
    }
}

/** "Order #1043" over "Nov 7 · 2 items · $87.00", the first line's label leading and the status trailing. */
@Composable
private fun ShopOrderRow(order: ShopOrder, model: ShopModel, dates: ShopDates, now: LocalDateTime) {
    val status = order.status(now)
    val first = order.lines.firstOrNull()?.let { model.product(it.productId) }
    AMListRow(
        "Order #${order.number}",
        subtitle = "${dates.short(order.placed)} · ${shopCount(order.itemCount)} · ${shopPrice(order.total)}",
        leadingContent = first?.let { { ShopLabelArt(it, ShopLabelStyle.Thumb) } },
        trailingContent = { AMTag(status.title, tint = status.tint) },
        onClick = { model.openOrder(order.number) },
    )
}

/**
 * An order's panel, titled with its number, on the beans: when it was placed and where it stands,
 * the tracking timeline, the items, the delivery and the totals. Draws nothing for an unknown
 * number.
 */
@Composable
internal fun ShopOrderPanel(model: ShopModel, number: Int, onBack: () -> Unit) {
    val order = model.order(number) ?: return
    val spacing = AMTheme.tokens.spacing
    val dates = rememberShopDates()
    val now = LocalDateTime.now()
    val status = order.status(now)
    AMBackdrop(art = { ShopBackdrop() }, Modifier.fillMaxSize()) {
        AMPanel("Order #${order.number}", onBack = onBack, containerColor = Color.Transparent) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = ShopFabClearance),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Placed ${dates.long(order.placed)}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    AMTag(status.title, tint = status.tint)
                }
                AMSection("Tracking", fill = AMSurfaceFill.Frosted, inset = false) {
                    val success = AMSemantic.Success.color
                    val pending = AMTheme.selectionRing
                    AMTimeline(
                        order.steps(now),
                        key = { it.status },
                        marker = { step -> AMTimelineDot(color = if (step.isDone) success else pending) },
                    ) { step ->
                        AMTimelineLabel(
                            step.status.title,
                            subtitle = if (step.isDone) dates.stamp(step.date) else "Expected ${dates.long(step.date)}",
                        )
                    }
                }
                AMSection("Items", style = AMSectionStyle.Rows, fill = AMSurfaceFill.Frosted, inset = false) {
                    order.lines.forEach { line -> ShopOrderItemRow(line, model) }
                }
                AMSection("Delivery", style = AMSectionStyle.Rows, fill = AMSurfaceFill.Frosted, inset = false) {
                    AMLabeledContent("Method", order.delivery.title)
                    AMLabeledContent("Address", order.shipTo.summary)
                }
                AMSection("Summary", style = AMSectionStyle.Rows, fill = AMSurfaceFill.Frosted, inset = false) {
                    ShopTotalsRows(order.subtotal, order.shipping)
                }
            }
        }
    }
}

/** The product over "1 kg · Whole bean · Qty 1" ("Qty 1" for gear), its label leading and the line total trailing. */
@Composable
private fun ShopOrderItemRow(line: ShopLine, model: ShopModel) {
    val product = model.product(line.productId) ?: return
    val quantity = "Qty ${line.quantity}"
    AMListRow(
        product.name,
        subtitle = if (line.detail.isEmpty()) quantity else "${line.detail} · $quantity",
        leadingContent = { ShopLabelArt(product, ShopLabelStyle.Thumb) },
        trailingContent = {
            Text(
                shopPrice(line.total),
                style = MaterialTheme.typography.bodyMedium.shopFigures(),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}

/**
 * The order dates in the phone's locale and clock: [short] "Nov 7", [long] "Sat, Nov 7", and
 * [stamp] "Sat, Nov 7, 8:12 PM" (or the 24-hour time).
 */
@Immutable
private class ShopDates(locale: Locale, is24Hour: Boolean) {
    private val shortFormat = format(locale, "MMMd")
    private val longFormat = format(locale, "EEEMMMd")
    private val stampFormat = format(locale, if (is24Hour) "EEEMMMdHmm" else "EEEMMMdhmma")

    fun short(date: LocalDateTime): String = shortFormat.format(date)

    fun long(date: LocalDateTime): String = longFormat.format(date)

    fun stamp(date: LocalDateTime): String = stampFormat.format(date)

    private fun format(locale: Locale, skeleton: String): DateTimeFormatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

@Composable
private fun rememberShopDates(): ShopDates {
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(locale, is24Hour) { ShopDates(locale, is24Hour) }
}
