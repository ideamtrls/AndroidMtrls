package com.ideamtrls.androidmtrls.demo

import android.os.Bundle
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.ideamtrls.androidmtrls.AMToastItem
import com.ideamtrls.androidmtrls.amInstantSwitch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The three root views, swapped instantly from the remote. */
internal enum class ShopView { Shop, Bag, Orders }

/**
 * A place on one root view's back stack: the root view itself (always first), or a panel pushed
 * over it. Saved as text ([encode], [decode]).
 */
internal sealed interface ShopRoute {
    /** The root view at the bottom of its own stack. */
    data class Root(val view: ShopView) : ShopRoute

    /** A product's panel. */
    data class Product(val id: String) : ShopRoute

    /** An order's panel, by its number. */
    data class Order(val number: Int) : ShopRoute

    /** The bag as a panel, pushed by the shop's bag button so back returns to the shop. */
    data object Bag : ShopRoute

    fun encode(): String = when (this) {
        is Root -> "root:${view.name}"
        is Product -> "product:$id"
        is Order -> "order:$number"
        Bag -> "bag"
    }

    companion object {
        fun decode(text: String): ShopRoute? {
            val value = text.substringAfter(':', "")
            return when (text.substringBefore(':')) {
                "root" -> ShopView.entries.firstOrNull { it.name == value }?.let(::Root)
                "product" -> Product(value)
                "order" -> value.toIntOrNull()?.let(::Order)
                "bag" -> Bag
                else -> null
            }
        }
    }
}

/**
 * Owns the Shop scene: the catalog, the bag, the orders, each root view's back stack, and every
 * overlay. A plain class holding Compose state, remembered at the scene root and saved with
 * [saver]. Per-presentation drafts (a product's choices, the checkout's ship-to) live in their
 * views.
 *
 * The one request, placing an order, is a stand-in: a wait behind the loading overlay, run as a
 * coroutine in [scope], the scene's own, so leaving the scene cancels it ([cancelPendingWork]).
 */
@Stable
internal class ShopModel(private val scope: CoroutineScope) {
    private val samples = ShopSamples.make()
    val products: List<ShopProduct> = samples.products
    val featured: List<ShopFeature> = samples.featured

    /** Newest first. */
    var orders: List<ShopOrder> by mutableStateOf(samples.orders)
        private set

    var bag: List<ShopLine> by mutableStateOf(emptyList())
        private set

    /** The shop lands after a beat, once. */
    var isLoading: Boolean by mutableStateOf(true)
        private set

    // Navigation

    var currentView: ShopView by mutableStateOf(ShopView.Shop)

    private val stacks: Map<ShopView, SnapshotStateList<ShopRoute>> =
        ShopView.entries.associateWith { mutableStateListOf<ShopRoute>(ShopRoute.Root(it)) }

    var filter: ShopFilter by mutableStateOf(ShopFilter.All)
    var featuredIndex: Int by mutableIntStateOf(0)

    // Overlays

    /** The product the quick-add spotlight shows. Nothing clears it, so the card fades out intact. */
    var quickAddId: String? by mutableStateOf(null)
        private set
    var showsQuickAdd: Boolean by mutableStateOf(false)

    val searchQuery = TextFieldState()
    var showsSearch: Boolean by mutableStateOf(false)

    var showsCheckout: Boolean by mutableStateOf(false)

    /**
     * The bag as Checkout opened. Nothing clears it, so the sheet's summary holds while it slides
     * away after the order empties the bag.
     */
    var checkoutLines: List<ShopLine> by mutableStateOf(emptyList())
        private set
    var isPlacingOrder: Boolean by mutableStateOf(false)
        private set

    var toast: AMToastItem? by mutableStateOf(null)

    /** The order being placed. */
    private var request: Job? = null

    init {
        check(products.map { it.id }.toSet().size == products.size) { "Shop product ids must be unique" }
    }

    // Lookups

    fun product(id: String): ShopProduct? = products.firstOrNull { it.id == id }

    fun order(number: Int): ShopOrder? = orders.firstOrNull { it.number == number }

    /** [view]'s back stack, its root first. */
    fun stack(view: ShopView): SnapshotStateList<ShopRoute> = stacks.getValue(view)

    /** The filter applied, in catalog order. */
    val shownProducts: List<ShopProduct> get() = products.filter(filter::includes)

    /** The quantities summed: the bag button's badge. */
    val bagCount: Int get() = bag.sumOf { it.quantity }

    val bagSubtotal: Int get() = bag.sumOf { it.total }

    val checkoutSubtotal: Int get() = checkoutLines.sumOf { it.total }

    val quickAddProduct: ShopProduct? get() = quickAddId?.let(::product)

    /**
     * Every product while the query is blank; otherwise those whose name, label, category, detail
     * values or notes contain it, in catalog order.
     */
    val searchResults: List<ShopProduct>
        get() {
            val query = searchQuery.text.trim()
            if (query.isEmpty()) return products
            return products.filter { product ->
                val fields = listOf(product.name, product.labelTitle, product.category.title) +
                    product.details.map { it.value } + product.notes
                fields.any { it.contains(query, ignoreCase = true) }
            }
        }

    // Loading

    /** Lands the shop after a beat, once. Runs in the composition, which cancels it as the scene leaves. */
    suspend fun load() {
        if (!isLoading) return
        delay(1.seconds)
        isLoading = false
    }

    // Navigation

    /** Pushes [route] on the showing view's stack, unless it's already there (a double tap). */
    private fun push(route: ShopRoute) {
        val stack = stack(currentView)
        if (route !in stack) stack.add(route)
    }

    /** Pops [view]'s top panel; its root stays. */
    fun pop(view: ShopView) {
        val stack = stack(view)
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun openProduct(id: String) = push(ShopRoute.Product(id))

    fun openOrder(number: Int) = push(ShopRoute.Order(number))

    /**
     * The shop's bag button: pushes the bag as a panel over the shop, so back returns to where it
     * was pressed. The remote's Bag tile switches the root instead.
     */
    fun openBagPanel() = push(ShopRoute.Bag)

    // Bag

    /**
     * Adds [quantity] of a product, merging into the line with the same product, size and grind (up
     * to 99), and toasts what went in. Gear drops the size and grind.
     */
    fun add(productId: String, size: ShopSize?, grind: ShopGrind?, quantity: Int) {
        val product = product(productId) ?: return
        val lineSize = if (product.isCoffee) size else null
        val lineGrind = if (product.isCoffee) grind else null
        val index = bag.indexOfFirst { it.productId == productId && it.size == lineSize && it.grind == lineGrind }
        bag = if (index >= 0) {
            bag.toMutableList().also { it[index] = it[index].copy(quantity = minOf(it[index].quantity + quantity, MaxQuantity)) }
        } else {
            bag + ShopLine(
                id = UUID.randomUUID().toString(),
                productId = productId,
                size = lineSize,
                grind = lineGrind,
                quantity = minOf(quantity, MaxQuantity),
                unitPrice = product.price(lineSize),
            )
        }
        var text = "Added ${product.name}"
        if (lineSize != null) text += ", ${lineSize.title}"
        if (quantity > 1) text += " × $quantity"
        toast = AMToastItem(text)
    }

    /** Brings [productId] up in the quick-add spotlight. */
    fun showQuickAdd(productId: String) {
        quickAddId = productId
        showsQuickAdd = true
    }

    /** Closes the spotlight and adds one bag: Whole bean for coffee, no grind for gear. */
    fun quickAdd(productId: String, size: ShopSize?) {
        showsQuickAdd = false
        val isCoffee = product(productId)?.isCoffee ?: false
        add(productId, size, if (isCoffee) ShopGrind.WholeBean else null, quantity = 1)
    }

    /** Sets a line's quantity, kept to 1…99. The list shows the change, so nothing toasts. */
    fun setQuantity(lineId: String, quantity: Int) {
        bag = bag.map { if (it.id == lineId) it.copy(quantity = quantity.coerceIn(1, MaxQuantity)) else it }
    }

    fun remove(lineId: String) {
        bag = bag.filterNot { it.id == lineId }
    }

    // Search

    fun beginSearch() {
        searchQuery.clearText()
        showsSearch = true
    }

    /** Closes Search and shows the product over the showing view, in place of any panel there. */
    fun openSearchResult(productId: String) {
        showsSearch = false
        stack(currentView).resetTo(ShopRoute.Product(productId))
    }

    // Checkout

    fun beginCheckout() {
        checkoutLines = bag
        showsCheckout = true
    }

    /**
     * Places the order Checkout holds: a beat under "Placing order…", then the new order goes
     * first, the bag empties, the sheet closes (so its slide plays), a toast confirms, and the
     * order opens on Orders in the same frame. Ignored while an order is already being placed.
     */
    fun placeOrder(shipTo: ShopShipTo, delivery: ShopDelivery) {
        if (isPlacingOrder) return
        isPlacingOrder = true
        request = scope.launch {
            delay(1500.milliseconds)
            request = null
            val order = ShopOrder(
                number = (orders.maxOfOrNull { it.number } ?: FirstOrderNumber) + 1,
                placed = LocalDateTime.now(),
                lines = checkoutLines,
                delivery = delivery,
                shipTo = shipTo.trimmed(),
            )
            orders = listOf(order) + orders
            bag = emptyList()
            isPlacingOrder = false
            showsCheckout = false
            toast = AMToastItem("Order #${order.number} placed")
            amInstantSwitch {
                // The bag the order came from is empty now: close its panel, so the shop opens on
                // its home again.
                val from = stack(currentView)
                if (from.last() == ShopRoute.Bag) from.removeAt(from.lastIndex)
                currentView = ShopView.Orders
                stack(ShopView.Orders).resetTo(ShopRoute.Order(order.number))
            }
        }
    }

    /** Abandons the order in flight. Runs from the Exit tile and when the scene goes away. */
    fun cancelPendingWork() {
        request?.cancel()
        request = null
        isPlacingOrder = false
    }

    companion object {
        /** A line's largest quantity. */
        private const val MaxQuantity = 99

        /** The number before the samples' newest, should every order be gone. */
        private const val FirstOrderNumber = 1043

        /**
         * Saves the views and their back stacks, the bag, the orders placed here, the filter and
         * which overlay is up; restores them into a model on the restored scene's [scope]. The
         * sample orders are made again, relative to now, and nothing in flight is kept.
         */
        fun saver(scope: CoroutineScope): Saver<ShopModel, Any> = mapSaver(
            save = { model ->
                mapOf(
                    "view" to model.currentView.name,
                    "stacks" to ArrayList(ShopView.entries.map { view -> model.stack(view).joinToString("\n") { it.encode() } }),
                    "filter" to model.filter.name,
                    "featured" to model.featuredIndex,
                    "loaded" to !model.isLoading,
                    "bag" to ArrayList(model.bag.map { it.toBundle() }),
                    "placed" to ArrayList(model.orders.filter { it.number > FirstOrderNumber }.map { it.toBundle() }),
                    "quickAdd" to model.quickAddId,
                    "showsQuickAdd" to model.showsQuickAdd,
                    "query" to model.searchQuery.text.toString(),
                    "showsSearch" to model.showsSearch,
                    "checkoutLines" to ArrayList(model.checkoutLines.map { it.toBundle() }),
                    "showsCheckout" to model.showsCheckout,
                )
            },
            restore = { saved ->
                ShopModel(scope).apply {
                    currentView = ShopView.valueOf(saved["view"] as String)
                    (saved["stacks"] as List<*>).filterIsInstance<String>().zip(ShopView.entries).forEach { (text, view) ->
                        val routes = text.split("\n").mapNotNull(ShopRoute::decode)
                        val stack = stack(view)
                        stack.clear()
                        stack.add(ShopRoute.Root(view))
                        stack.addAll(routes.filter { it !is ShopRoute.Root }.distinct())
                    }
                    filter = ShopFilter.valueOf(saved["filter"] as String)
                    featuredIndex = saved["featured"] as Int
                    isLoading = !(saved["loaded"] as Boolean)
                    bag = (saved["bag"] as List<*>).filterIsInstance<Bundle>().mapNotNull(::shopLine)
                    orders = (saved["placed"] as List<*>).filterIsInstance<Bundle>().mapNotNull(::shopOrder) + orders
                    quickAddId = saved["quickAdd"] as String?
                    showsQuickAdd = saved["showsQuickAdd"] as Boolean
                    searchQuery.setTextAndPlaceCursorAtEnd(saved["query"] as String)
                    showsSearch = saved["showsSearch"] as Boolean
                    checkoutLines = (saved["checkoutLines"] as List<*>).filterIsInstance<Bundle>().mapNotNull(::shopLine)
                    showsCheckout = saved["showsCheckout"] as Boolean
                }
            },
        )
    }
}

/** Leaves the root alone on this stack, then pushes [route] over it. */
private fun SnapshotStateList<ShopRoute>.resetTo(route: ShopRoute) {
    retainAll { it is ShopRoute.Root }
    add(route)
}

// Saving lines and orders

private fun ShopLine.toBundle(): Bundle = Bundle().apply {
    putString("id", id)
    putString("product", productId)
    putString("size", size?.name)
    putString("grind", grind?.name)
    putInt("quantity", quantity)
    putInt("unitPrice", unitPrice)
}

private fun shopLine(bundle: Bundle): ShopLine? {
    val id = bundle.getString("id") ?: return null
    val product = bundle.getString("product") ?: return null
    return ShopLine(
        id = id,
        productId = product,
        size = bundle.getString("size")?.let(ShopSize::valueOf),
        grind = bundle.getString("grind")?.let(ShopGrind::valueOf),
        quantity = bundle.getInt("quantity"),
        unitPrice = bundle.getInt("unitPrice"),
    )
}

private fun ShopOrder.toBundle(): Bundle = Bundle().apply {
    putInt("number", number)
    putString("placed", placed.toString())
    putString("delivery", delivery.name)
    putString("name", shipTo.name)
    putString("street", shipTo.street)
    putString("city", shipTo.city)
    putString("zip", shipTo.zip)
    putParcelableArrayList("lines", ArrayList(lines.map { it.toBundle() }))
}

private fun shopOrder(bundle: Bundle): ShopOrder? {
    val placed = bundle.getString("placed") ?: return null
    return ShopOrder(
        number = bundle.getInt("number"),
        placed = LocalDateTime.parse(placed),
        lines = bundle.getParcelableArrayList("lines", Bundle::class.java).orEmpty().mapNotNull(::shopLine),
        delivery = ShopDelivery.valueOf(bundle.getString("delivery") ?: ShopDelivery.Standard.name),
        shipTo = ShopShipTo(
            bundle.getString("name").orEmpty(),
            bundle.getString("street").orEmpty(),
            bundle.getString("city").orEmpty(),
            bundle.getString("zip").orEmpty(),
        ),
    )
}
