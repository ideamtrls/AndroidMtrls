package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow

/**
 * The held product in the quick-add spotlight: its grid card at a fixed width, read as one
 * element. Its frosted card nests in the spotlight's solid one and draws nothing of its own, so it
 * comes up solid.
 */
@Composable
internal fun ShopQuickAddPreview(product: ShopProduct) {
    ShopProductCard(product, Modifier.width(ShopPreviewCardWidth).semantics(mergeDescendants = true) {})
}

/**
 * The held product's tiles. A coffee gets its three sizes, each adding one Whole bean bag ("500 g",
 * read as "Add 500 g, $41.00"); gear gets Add to bag and Details, which pushes the product.
 */
@Composable
internal fun ShopQuickAddTiles(product: ShopProduct, model: ShopModel) {
    if (product.isCoffee) {
        AMActionGridRow(
            { ShopSizeTile(product, ShopSize.Small, model) },
            { ShopSizeTile(product, ShopSize.Medium, model) },
            { ShopSizeTile(product, ShopSize.Large, model) },
        )
    } else {
        AMActionGridRow(
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_add_shopping_cart), "Add to bag",
                    onClick = { model.quickAdd(product.id, size = null) },
                )
            },
            {
                AMActionGridButton(painterResource(R.drawable.ic_info), "Details", onClick = {
                    model.showsQuickAdd = false
                    model.openProduct(product.id)
                })
            },
        )
    }
}

/** One size's tile: "500 g", read as "Add 500 g, $41.00". */
@Composable
private fun ShopSizeTile(product: ShopProduct, size: ShopSize, model: ShopModel) {
    val spoken = "Add ${size.title}, ${shopPrice(product.price(size))}"
    AMActionGridButton(
        painterResource(R.drawable.ic_add_shopping_cart), size.title,
        onClick = { model.quickAdd(product.id, size) },
        modifier = Modifier.semantics { contentDescription = spoken },
    )
}
