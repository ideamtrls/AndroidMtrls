package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import com.ideamtrls.androidmtrls.AMHoldAction

/**
 * The held product in the quick-add spotlight: its grid card at a fixed width, read as one
 * element. Its frosted card nests in the spotlight's solid one and draws nothing of its own, so it
 * comes up solid.
 */
@Composable
internal fun ShopQuickAddPreview(product: ShopProduct) {
    ShopProductCard(product, Modifier.width(ShopPreviewCardWidth).semantics(mergeDescendants = true) {})
}

/** The quick-add tiles' glyphs. */
@Immutable
internal class ShopQuickAddIcons(val add: Painter, val details: Painter)

/** The quick-add tiles' glyphs. */
@Composable
internal fun rememberShopQuickAddIcons(): ShopQuickAddIcons = ShopQuickAddIcons(
    add = painterResource(R.drawable.ic_add_shopping_cart),
    details = painterResource(R.drawable.ic_info),
)

/**
 * The held product's tiles. A coffee gets its three sizes, each adding one Whole bean bag ("500 g",
 * read as "Add 500 g, $41.00"); gear gets Add to bag and Details, which pushes the product.
 */
internal fun shopQuickAddActions(product: ShopProduct, model: ShopModel, icons: ShopQuickAddIcons): List<AMHoldAction> =
    if (product.isCoffee) {
        listOf(ShopSize.Small, ShopSize.Medium, ShopSize.Large).map { size ->
            AMHoldAction(size.title, icons.add, contentDescription = "Add ${size.title}, ${shopPrice(product.price(size))}") {
                model.quickAdd(product.id, size)
            }
        }
    } else {
        listOf(
            AMHoldAction("Add to bag", icons.add) { model.quickAdd(product.id, size = null) },
            AMHoldAction("Details", icons.details) { model.openProduct(product.id) },
        )
    }
