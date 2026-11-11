package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMBadge
import com.ideamtrls.androidmtrls.AMCarousel
import com.ideamtrls.androidmtrls.AMChipGroup
import com.ideamtrls.androidmtrls.AMIconButton
import com.ideamtrls.androidmtrls.AMPageIndicator
import com.ideamtrls.androidmtrls.AMSectionHeader
import com.ideamtrls.androidmtrls.AMSkeleton
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amHoldable

/**
 * The Shop root, the scene's landing view: the roaster's name with the light/dark chip and the bag
 * button, the "This week" carousel over its page dots, the category chips, then a two-column grid
 * of product cards. A tap on a card or a page pushes the product; a press and hold on a card brings
 * up quick add. While the shop loads, the carousel and the cards hold shimmering placeholders in
 * their own places, so nothing moves when it lands; the header and the chips stay live.
 */
@Composable
internal fun ShopHome(model: ShopModel) {
    val spacing = AMTheme.tokens.spacing
    ShopRootPage(
        "Ferry Line",
        actions = {
            SceneAppearanceButton()
            ShopBagButton(model.bagCount, onClick = model::openBagPanel)
        },
    ) { padding ->
        val loading = model.isLoading
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + spacing.sm,
                bottom = padding.calculateBottomPadding() + ShopFabClearance,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            item(key = "thisWeek", span = { GridItemSpan(maxLineSpan) }) {
                ShopThisWeek(model, loading)
            }
            item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                AMChipGroup(
                    items = ShopFilter.entries,
                    label = { it.title },
                    selected = model.filter,
                    onSelectedChange = { model.filter = it },
                    modifier = Modifier.padding(horizontal = spacing.lg),
                )
            }
            itemsIndexed(model.shownProducts, key = { _, product -> product.id }) { index, product ->
                // The grid runs edge to edge for the carousel, so each column keeps its own gutter.
                val start = index % 2 == 0
                val gutter = Modifier.padding(start = if (start) spacing.lg else 0.dp, end = if (start) 0.dp else spacing.lg)
                ShopProductCard(
                    product,
                    gutter,
                    interaction = if (loading) Modifier else rememberShopCardInput(product, model),
                    loading = loading,
                )
            }
        }
    }
}

/**
 * The bag button: Material's icon button with the bag's count badge on its corner, read as "Bag, 3
 * items". It pushes the bag over the shop, so back returns here.
 */
@Composable
private fun ShopBagButton(count: Int, onClick: () -> Unit) {
    AMIconButton(
        painterResource(R.drawable.ic_shopping_bag),
        "Bag",
        onClick = onClick,
        badge = { AMBadge(count, contentDescription = shopCount(count)) },
    )
}

/**
 * "This week": the featured picks as full-bleed carousel pages (the carousel insets its own),
 * each one button that pushes its product, over the page dots. Loading, the first page's place
 * shimmers.
 */
@Composable
private fun ShopThisWeek(model: ShopModel, loading: Boolean) {
    val spacing = AMTheme.tokens.spacing
    val shape = MaterialTheme.shapes.large
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        AMSectionHeader("This week", Modifier.padding(horizontal = spacing.lg))
        if (loading) {
            AMSkeleton.Rect(Modifier.padding(horizontal = spacing.lg), height = ShopFeatureHeight, shape = shape)
        } else {
            AMCarousel(
                model.featured,
                currentIndex = model.featuredIndex,
                onCurrentIndexChange = { model.featuredIndex = it },
                modifier = Modifier.fillMaxWidth().height(ShopFeatureHeight),
            ) { feature ->
                val product = model.product(feature.id)
                if (product != null) {
                    ShopLabelArt(
                        product,
                        ShopLabelStyle.Banner(feature.kicker, feature.blurb),
                        Modifier
                            .clip(shape)
                            .clickable(role = Role.Button) { model.openProduct(product.id) }
                            .semantics { contentDescription = "${feature.kicker}, ${product.name}, ${product.priceLabel}" },
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AMPageIndicator(count = model.featured.size, currentIndex = model.featuredIndex)
        }
    }
}

/**
 * A grid card's tap and hold: a tap pushes the product; a press and hold brings it up in the
 * quick-add spotlight, with its light haptic, and a vertical drag that starts on the card still
 * scrolls. TalkBack gets the spotlight's adds as the card's own actions: each size for coffee,
 * "Add to bag" for gear.
 */
@Composable
private fun rememberShopCardInput(product: ShopProduct, model: ShopModel): Modifier {
    val actions = remember(product, model) {
        if (product.isCoffee) {
            ShopSize.entries.map { size ->
                CustomAccessibilityAction("Add ${size.title}") {
                    model.quickAdd(product.id, size)
                    true
                }
            }
        } else {
            listOf(
                CustomAccessibilityAction("Add to bag") {
                    model.quickAdd(product.id, null)
                    true
                },
            )
        }
    }
    return Modifier
        .clickable(role = Role.Button) { model.openProduct(product.id) }
        .amHoldable(onHold = { model.showQuickAdd(product.id) }, customActions = actions)
}
