package com.ideamtrls.androidmtrls.demo

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.ideamtrls.androidmtrls.AMTagTint
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToInt

// Catalog

/** What a product is: one of the three coffee kinds, or brewing gear. */
internal enum class ShopCategory(val title: String) {
    Espresso("Espresso"),
    Filter("Filter"),
    Decaf("Decaf"),
    Gear("Gear"),
}

/** The shop's category chips: everything, or one category. */
internal enum class ShopFilter(val title: String, private val category: ShopCategory?) {
    All("All", null),
    Espresso("Espresso", ShopCategory.Espresso),
    Filter("Filter", ShopCategory.Filter),
    Decaf("Decaf", ShopCategory.Decaf),
    Gear("Gear", ShopCategory.Gear),
    ;

    fun includes(product: ShopProduct): Boolean = category == null || product.category == category
}

/** A coffee's roast: [title] "Light roast", and [level] "Light", the Roast detail's value. */
internal enum class ShopRoast(val title: String, val level: String) {
    Light("Light roast", "Light"),
    Medium("Medium roast", "Medium"),
    Dark("Dark roast", "Dark"),
}

/** A coffee bag's size. Prices follow from the 250 g bag. */
internal enum class ShopSize(val title: String, private val factor: Double) {
    Small("250 g", 1.0),
    Medium("500 g", 1.8),
    Large("1 kg", 3.3),
    ;

    /** The bag's price in cents from the 250 g price [base]: × 1, × 1.8 or × 3.3, to whole dollars. */
    fun price(base: Int): Int = (base * factor / 100).roundToInt() * 100
}

/** How a coffee is ground. */
internal enum class ShopGrind(val title: String) {
    WholeBean("Whole bean"),
    Espresso("Espresso"),
    Filter("Filter"),
}

/** The steps a product panel shows: a brewing guide for coffee, how to use a piece of gear. */
internal enum class ShopGuide(val title: String, val steps: List<String>) {
    PourOver(
        "Brewing guide",
        listOf(
            "Grind 15 g medium-fine, like sea salt.",
            "Rinse the filter and warm the dripper with hot water.",
            "Pour 50 g of water just off the boil, then wait 30 seconds.",
            "Pour to 250 g in slow circles and finish by 3:00.",
        ),
    ),
    Espresso(
        "Brewing guide",
        listOf(
            "Dose 18 g, ground fine.",
            "Tamp level and firm.",
            "Pull 36 g in 27 to 30 seconds.",
            "Sour means grind finer; bitter means coarser.",
        ),
    ),
    Dripper(
        "How to use",
        listOf(
            "Fold a #2 filter along its seams and seat it.",
            "Warm the dripper with hot water, then pour it out.",
            "Brew 15 g of coffee to 250 g of water.",
            "Rinse it by hand or run it through the dishwasher.",
        ),
    ),
    Grinder(
        "How to use",
        listOf(
            "Close the dial all the way, then count clicks back.",
            "About 12 clicks for espresso, 24 for pour-over.",
            "Fill up to 30 g and grind in steady turns.",
            "Brush the burrs clean every few weeks.",
        ),
    ),
    Kettle(
        "How to use",
        listOf(
            "Fill to the line inside, 0.9 L at most.",
            "Heat it on gas, electric or induction.",
            "Take it off at 94 °C on the lid's thermometer.",
            "Pour from the tip of the spout for a thin, steady stream.",
        ),
    ),
}

/** One labeled fact on a product panel: "Origin" / "Guji, Ethiopia". */
@Immutable
internal data class ShopDetail(val label: String, val value: String)

/**
 * A coffee or a piece of gear. Its label art is typographic: the label title and footer in [ink]
 * on [color].
 */
@Immutable
internal class ShopProduct(
    val id: String,
    val name: String,
    /** The big word on the label: "Ethiopia". */
    val labelTitle: String,
    /** The small line under it: "LIGHT ROAST", "CERAMIC". */
    val labelFooter: String,
    val category: ShopCategory,
    /** Null for gear. */
    val roast: ShopRoast?,
    /** Origin, Process and Roast for coffee; the gear's own facts. */
    val details: List<ShopDetail>,
    /** Tasting notes for coffee; the gear's features. */
    val notes: List<String>,
    val guide: ShopGuide,
    /** Cents: the 250 g bag, or the gear's price. */
    val basePrice: Int,
    val color: Color,
    val ink: Color,
) {
    val isCoffee: Boolean get() = category != ShopCategory.Gear

    /** The price in cents of one [size] bag (the 250 g bag when null); gear ignores the size. */
    fun price(size: ShopSize?): Int = if (isCoffee) (size ?: ShopSize.Small).price(basePrice) else basePrice

    /** "From $21.00" for coffee, "$95.00" for gear. */
    val priceLabel: String get() = if (isCoffee) "From ${shopPrice(basePrice)}" else shopPrice(basePrice)

    /** "Light roast · From $21.00" for coffee, "Gear · $95.00" for gear: a search result's line. */
    val searchLine: String get() = "${roast?.title ?: category.title} · $priceLabel"
}

/** A "This week" carousel page; [id] is the product's id. */
@Immutable
internal class ShopFeature(val id: String, val kicker: String, val blurb: String)

// Bag and orders

/**
 * One line of the bag or an order: a product in one size and grind (both null for gear), at the
 * unit price it was added at.
 */
@Immutable
internal data class ShopLine(
    val id: String,
    val productId: String,
    val size: ShopSize?,
    val grind: ShopGrind?,
    val quantity: Int,
    /** Cents. */
    val unitPrice: Int,
) {
    val total: Int get() = unitPrice * quantity

    /** "500 g · Whole bean"; empty for gear. */
    val detail: String get() = listOfNotNull(size?.title, grind?.title).joinToString(" · ")
}

/** How an order ships. */
internal enum class ShopDelivery(val title: String, val detail: String) {
    Standard("Standard", "Arrives in 3–5 days. Free over $50."),
    Express("Express", "Arrives in 1–2 days."),
    ;

    /** Cents. Standard is free from a $50 subtotal; Express is always $15. */
    fun shipping(subtotal: Int): Int = when (this) {
        Standard -> if (subtotal < 5000) 600 else 0
        Express -> 1500
    }
}

/** Where an order goes. */
@Immutable
internal data class ShopShipTo(val name: String, val street: String, val city: String, val zip: String) {
    /** All four fields are filled in. */
    val isComplete: Boolean get() = listOf(name, street, city, zip).all { it.isNotBlank() }

    /** "214 Van Brunt St, Brooklyn, NY 11231". */
    val summary: String get() = "$street, $city $zip"

    /** Every field without its surrounding whitespace. */
    fun trimmed(): ShopShipTo = ShopShipTo(name.trim(), street.trim(), city.trim(), zip.trim())

    companion object {
        val Empty = ShopShipTo("", "", "", "")
        val Sample = ShopShipTo("Ada Lovelace", "214 Van Brunt St", "Brooklyn, NY", "11231")
    }
}

/** Where an order stands, in order, with its tag's tint. */
internal enum class ShopOrderStatus(val title: String, val tint: AMTagTint) {
    Ordered("Ordered", AMTagTint.Neutral),
    Roasted("Roasted", AMTagTint.Warning),
    Shipped("Shipped", AMTagTint.Info),
    Delivered("Delivered", AMTagTint.Success),
}

/** One step of an order's tracking timeline: done once its [date] has passed. */
@Immutable
internal class ShopOrderStep(val status: ShopOrderStatus, val date: LocalDateTime, val isDone: Boolean)

/** A placed order, identified by its [number]. */
@Immutable
internal class ShopOrder(
    val number: Int,
    val placed: LocalDateTime,
    val lines: List<ShopLine>,
    val delivery: ShopDelivery,
    val shipTo: ShopShipTo,
) {
    val subtotal: Int get() = lines.sumOf { it.total }
    val shipping: Int get() = delivery.shipping(subtotal)
    val total: Int get() = subtotal + shipping

    /** The quantities summed. */
    val itemCount: Int get() = lines.sumOf { it.quantity }

    /**
     * The four steps, dated from [placed]: roasted the next day at 7:30 AM, shipped two days after
     * at 3:15 PM, delivered five days after at 1:20 PM (Standard) or three days after at 11:05 AM
     * (Express). A step is done once its date has passed [now].
     */
    fun steps(now: LocalDateTime): List<ShopOrderStep> {
        val delivered = when (delivery) {
            ShopDelivery.Standard -> shopDate(daysAfter = 5, hour = 13, minute = 20, from = placed)
            ShopDelivery.Express -> shopDate(daysAfter = 3, hour = 11, minute = 5, from = placed)
        }
        val dates = listOf(
            ShopOrderStatus.Ordered to placed,
            ShopOrderStatus.Roasted to shopDate(daysAfter = 1, hour = 7, minute = 30, from = placed),
            ShopOrderStatus.Shipped to shopDate(daysAfter = 2, hour = 15, minute = 15, from = placed),
            ShopOrderStatus.Delivered to delivered,
        )
        return dates.map { (status, date) -> ShopOrderStep(status, date, !date.isAfter(now)) }
    }

    /** The last done step. */
    fun status(now: LocalDateTime): ShopOrderStatus =
        steps(now).lastOrNull { it.isDone }?.status ?: ShopOrderStatus.Ordered
}

/** [days] after [from], at [hour]:[minute]. */
private fun shopDate(daysAfter: Int, hour: Int, minute: Int, from: LocalDateTime): LocalDateTime =
    from.toLocalDate().plusDays(daysAfter.toLong()).atTime(LocalTime.of(hour, minute))

// Samples

/** Ferry Line's catalog, its featured picks, and three past orders dated relative to [now]. */
internal class ShopSamples(val products: List<ShopProduct>, val featured: List<ShopFeature>, val orders: List<ShopOrder>) {
    companion object {
        fun make(now: LocalDateTime = LocalDateTime.now()): ShopSamples {
            val byId = ShopProducts.associateBy { it.id }

            fun line(id: String, size: ShopSize?, grind: ShopGrind?, quantity: Int) = ShopLine(
                id = "sample-$id-${size?.name}-${grind?.name}",
                productId = id,
                size = size,
                grind = grind,
                quantity = quantity,
                unitPrice = byId[id]?.price(size) ?: 0,
            )

            fun order(number: Int, daysAgo: Int, hour: Int, minute: Int, delivery: ShopDelivery, lines: List<ShopLine>) =
                ShopOrder(
                    number = number,
                    placed = shopDate(daysAfter = -daysAgo, hour = hour, minute = minute, from = now),
                    lines = lines,
                    delivery = delivery,
                    shipTo = ShopShipTo.Sample,
                )

            return ShopSamples(
                products = ShopProducts,
                featured = listOf(
                    ShopFeature("gatomboya", "Just landed", "This year's Nyeri harvest: blackcurrant and grapefruit."),
                    ShopFeature("night-ferry", "House espresso", "Dark chocolate and molasses, made for milk."),
                    ShopFeature("grinder", "Back in stock", "Steel burrs and 40 clicks, from espresso to French press."),
                ),
                orders = listOf(
                    order(
                        1043, daysAgo = 3, hour = 20, minute = 12, ShopDelivery.Standard,
                        listOf(line("night-ferry", ShopSize.Large, ShopGrind.WholeBean, 1), line("dripper", null, null, 1)),
                    ),
                    order(
                        1041, daysAgo = 9, hour = 9, minute = 40, ShopDelivery.Express,
                        listOf(line("hambela", ShopSize.Small, ShopGrind.Filter, 2)),
                    ),
                    order(
                        1038, daysAgo = 23, hour = 18, minute = 5, ShopDelivery.Standard,
                        listOf(
                            line("gatomboya", ShopSize.Medium, ShopGrind.WholeBean, 1),
                            line("decaf-mexico", ShopSize.Small, ShopGrind.Espresso, 1),
                        ),
                    ),
                ),
            )
        }
    }
}

/** Label ink on light colors. */
private val ShopInkDark = Color(0xFF1C1A17)

/** Label ink on dark colors. */
private val ShopInkLight = Color(0xFFFFF8EE)

/** The catalog, in grid order: ten coffees, then three pieces of gear. */
private val ShopProducts: List<ShopProduct> = listOf(
    shopCoffee(
        "hambela", "Ethiopia Hambela", "Ethiopia", ShopCategory.Filter, "Guji, Ethiopia", "Washed", ShopRoast.Light,
        listOf("Jasmine", "Peach", "Black tea"), dollars = 21, color = 0xFFF2A65A, ink = ShopInkDark, ShopGuide.PourOver,
    ),
    shopCoffee(
        "gatomboya", "Kenya Gatomboya", "Kenya", ShopCategory.Filter, "Nyeri, Kenya", "Washed", ShopRoast.Light,
        listOf("Blackcurrant", "Grapefruit", "Cane sugar"), dollars = 23, color = 0xFFA23B4C, ink = ShopInkLight, ShopGuide.PourOver,
    ),
    shopCoffee(
        "night-ferry", "Night Ferry", "Night Ferry", ShopCategory.Espresso, "Brazil and Colombia", "Natural and washed",
        ShopRoast.Dark, listOf("Dark chocolate", "Hazelnut", "Molasses"), dollars = 18, color = 0xFF1E2A44, ink = ShopInkLight,
        ShopGuide.Espresso,
    ),
    shopCoffee(
        "huila", "Colombia Huila", "Colombia", ShopCategory.Filter, "Huila, Colombia", "Washed", ShopRoast.Medium,
        listOf("Red apple", "Caramel", "Cocoa"), dollars = 19, color = 0xFF3F7A57, ink = ShopInkLight, ShopGuide.PourOver,
    ),
    shopCoffee(
        "harbor-light", "Harbor Light", "Harbor Light", ShopCategory.Espresso, "Brazil and Ethiopia", "Natural",
        ShopRoast.Medium, listOf("Cocoa", "Cherry", "Toffee"), dollars = 19, color = 0xFFB5552B, ink = ShopInkLight,
        ShopGuide.Espresso,
    ),
    shopCoffee(
        "kanzu", "Rwanda Kanzu", "Rwanda", ShopCategory.Filter, "Nyamasheke, Rwanda", "Natural", ShopRoast.Light,
        listOf("Hibiscus", "Raspberry", "Honey"), dollars = 22, color = 0xFFE8A0AE, ink = ShopInkDark, ShopGuide.PourOver,
    ),
    shopCoffee(
        "cerrado", "Brazil Cerrado", "Brazil", ShopCategory.Espresso, "Cerrado Mineiro, Brazil", "Natural", ShopRoast.Medium,
        listOf("Peanut", "Milk chocolate", "Brown sugar"), dollars = 17, color = 0xFFC9A27E, ink = ShopInkDark, ShopGuide.Espresso,
    ),
    shopCoffee(
        "atitlan", "Guatemala Atitlán", "Guatemala", ShopCategory.Filter, "Sololá, Guatemala", "Washed", ShopRoast.Medium,
        listOf("Milk chocolate", "Almond", "Orange"), dollars = 19, color = 0xFFE3B341, ink = ShopInkDark, ShopGuide.PourOver,
    ),
    shopCoffee(
        "decaf-colombia", "Decaf Colombia", "Colombia", ShopCategory.Decaf, "Tolima, Colombia", "Sugarcane decaf",
        ShopRoast.Medium, listOf("Plum", "Brown sugar", "Cocoa"), dollars = 19, color = 0xFF8E86C9, ink = ShopInkDark,
        ShopGuide.PourOver,
    ),
    shopCoffee(
        "decaf-mexico", "Decaf Mexico", "Mexico", ShopCategory.Decaf, "Chiapas, Mexico", "Mountain water decaf",
        ShopRoast.Dark, listOf("Baker's chocolate", "Walnut", "Raisin"), dollars = 18, color = 0xFF8DA38A, ink = ShopInkDark,
        ShopGuide.Espresso,
    ),
    shopGear(
        "dripper", "Ceramic Dripper", "Dripper", "CERAMIC",
        listOf(ShopDetail("Material", "Ceramic"), ShopDetail("Brews", "1–2 cups")),
        listOf("Pour-over", "Fits #2 filters", "Dishwasher safe"), dollars = 28, color = 0xFFEFE6D8, ink = ShopInkDark,
        ShopGuide.Dripper,
    ),
    shopGear(
        "grinder", "Hand Grinder", "Grinder", "STEEL BURRS",
        listOf(ShopDetail("Burrs", "Stainless steel"), ShopDetail("Holds", "30 g")),
        listOf("Steel burrs", "40 settings", "Travels well"), dollars = 95, color = 0xFF3A3A3C, ink = ShopInkLight,
        ShopGuide.Grinder,
    ),
    shopGear(
        "kettle", "Gooseneck Kettle", "Kettle", "0.9 L",
        listOf(ShopDetail("Capacity", "0.9 L"), ShopDetail("Heats on", "Gas, electric, induction")),
        listOf("Stovetop", "Thermometer lid", "0.9 L"), dollars = 65, color = 0xFFB4703C, ink = ShopInkLight,
        ShopGuide.Kettle,
    ),
)

/**
 * A coffee: details Origin, Process and Roast, the roast on the label's footer ("DECAF · MEDIUM
 * ROAST" for a decaf), priced by its 250 g bag.
 */
private fun shopCoffee(
    id: String,
    name: String,
    label: String,
    category: ShopCategory,
    origin: String,
    process: String,
    roast: ShopRoast,
    notes: List<String>,
    dollars: Int,
    color: Long,
    ink: Color,
    guide: ShopGuide,
): ShopProduct {
    val roastLine = roast.title.uppercase(Locale.ROOT)
    return ShopProduct(
        id = id,
        name = name,
        labelTitle = label,
        labelFooter = if (category == ShopCategory.Decaf) "DECAF · $roastLine" else roastLine,
        category = category,
        roast = roast,
        details = listOf(ShopDetail("Origin", origin), ShopDetail("Process", process), ShopDetail("Roast", roast.level)),
        notes = notes,
        guide = guide,
        basePrice = dollars * 100,
        color = Color(color),
        ink = ink,
    )
}

/** A piece of gear: no roast and no sizes. */
private fun shopGear(
    id: String,
    name: String,
    label: String,
    footer: String,
    details: List<ShopDetail>,
    notes: List<String>,
    dollars: Int,
    color: Long,
    ink: Color,
    guide: ShopGuide,
): ShopProduct = ShopProduct(
    id = id,
    name = name,
    labelTitle = label,
    labelFooter = footer,
    category = ShopCategory.Gear,
    roast = null,
    details = details,
    notes = notes,
    guide = guide,
    basePrice = dollars * 100,
    color = Color(color),
    ink = ink,
)

// Labels

/** Cents as dollars: "$21.00". */
internal fun shopPrice(cents: Int): String = NumberFormat.getCurrencyInstance(Locale.US).format(cents / 100.0)

/** "1 item" / "3 items". */
internal fun shopCount(items: Int): String = if (items == 1) "1 item" else "$items items"
