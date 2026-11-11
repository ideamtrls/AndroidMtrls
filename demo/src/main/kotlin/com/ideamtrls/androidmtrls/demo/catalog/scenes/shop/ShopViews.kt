package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMLabeledContent
import com.ideamtrls.androidmtrls.AMPageHeader
import com.ideamtrls.androidmtrls.AMSkeleton
import com.ideamtrls.androidmtrls.AMSurfaceFill
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amPageBackdrop
import java.util.Locale

// Shared sizes

/**
 * Room every Shop view leaves under its last control, so it scrolls clear of the floating action
 * button: the button's 56 dp, with `spacing.lg` under it and the same over it.
 */
internal val ShopFabClearance = 88.dp

/** A label thumbnail's edge in a row: a list row's avatar size. */
private val ShopThumbSize = 40.dp

/** The height of a "This week" carousel page. */
internal val ShopFeatureHeight = 180.dp

/** The width of the product card the quick-add spotlight shows. */
internal val ShopPreviewCardWidth = 220.dp

/** Tabular figures, so prices and counts line up. */
internal fun TextStyle.shopFigures(): TextStyle = copy(fontFeatureSettings = "tnum")

// The backdrop

/** Roasted coffee beans, public domain, from Wikimedia Commons. */
private val ShopBeansUrl = commonsPhoto("Roasted_coffee_beans.jpg")

/**
 * The scene's one background, so the whole app feels like a coffee shop: roasted beans as a page
 * backdrop, blurred only enough to soften them, so they still read as beans. Light mode's white
 * wash already softens them; dark mode's lighter wash would leave their highlights hard against
 * the dark, so dark blurs more. It fills what holds it: the art of an `AMBackdrop`.
 */
@Composable
internal fun ShopBackdrop() {
    Box(
        Modifier
            .fillMaxSize()
            .amPageBackdrop(
                ShopBeansUrl,
                blurRadius = if (AMTheme.isDark) 17.dp else 3.5.dp,
                lightWash = 0.55f,
                darkWash = 0.4f,
            ),
    )
}

// Root views

/**
 * The page each root view sits on: its header (collapsing as the page scrolls, with [actions] at
 * its end), transparent over the scene's backdrop. [content] gets the scaffold's padding, to apply
 * inside its scrolling container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShopRootPage(
    title: String,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { AMPageHeader(title, actions = actions, scrollBehavior = scrollBehavior) },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = content,
    )
}

// Label art

/** Where a product's label shows, which sets its size and what it says. */
@Immutable
internal sealed interface ShopLabelStyle {
    /** A row's square thumbnail with the label's first letter. */
    data object Thumb : ShopLabelStyle

    /** A grid card's square label. */
    data object Tile : ShopLabelStyle

    /** The product panel's 3:2 label. */
    data object Hero : ShopLabelStyle

    /** A "This week" carousel page: the [kicker] over the label title, the [blurb] and the price. */
    data class Banner(val kicker: String, val blurb: String) : ShopLabelStyle
}

/**
 * A product's typographic label: its color with its ink type on it, one composable for every
 * surface so the art matches everywhere. It's packaging, not UI text, so its type is set in fixed
 * sizes that don't follow the font scale, and TalkBack skips it: the card or row around it names
 * the product.
 */
@Composable
internal fun ShopLabelArt(product: ShopProduct, style: ShopLabelStyle, modifier: Modifier = Modifier) {
    val shapes = MaterialTheme.shapes
    val spacing = AMTheme.tokens.spacing
    val art = modifier.clearAndSetSemantics {}
    when (style) {
        ShopLabelStyle.Thumb -> Box(
            art.size(ShopThumbSize).background(product.color, shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            ShopLabelText(product.labelTitle.take(1), product.ink, size = 20f, weight = FontWeight.Black)
        }
        ShopLabelStyle.Tile -> Box(art.fillMaxWidth().aspectRatio(1f).background(product.color, shapes.small)) {
            ShopLabel(product, brand = 9f, title = 26f, footer = 10f, Modifier.padding(spacing.md))
        }
        ShopLabelStyle.Hero -> Box(art.fillMaxWidth().aspectRatio(3f / 2f).background(product.color, shapes.large)) {
            ShopLabel(product, brand = 11f, title = 48f, footer = 13f, Modifier.padding(spacing.xl))
        }
        is ShopLabelStyle.Banner -> Box(art.fillMaxSize().background(product.color, shapes.large)) {
            Column(Modifier.fillMaxSize().padding(spacing.lg)) {
                ShopLabelText(
                    style.kicker.uppercase(Locale.getDefault()), product.ink.copy(alpha = 0.8f),
                    size = 11f, weight = FontWeight.Bold, tracking = 1.5f,
                )
                Spacer(Modifier.weight(1f))
                ShopLabelText(product.labelTitle, product.ink, size = 36f, weight = FontWeight.Black, shrinks = true)
                ShopLabelText(style.blurb, product.ink.copy(alpha = 0.85f), size = 14f, weight = FontWeight.Medium, maxLines = 2)
                ShopLabelText(product.priceLabel, product.ink, size = 13f, weight = FontWeight.SemiBold)
            }
        }
    }
}

/** The tile and hero label: the brand over the label title and footer, at the given sizes (dp). */
@Composable
private fun ShopLabel(product: ShopProduct, brand: Float, title: Float, footer: Float, modifier: Modifier) {
    Column(modifier.fillMaxSize()) {
        ShopLabelText("FERRY LINE", product.ink.copy(alpha = 0.7f), size = brand, weight = FontWeight.Bold, tracking = 1.5f)
        Spacer(Modifier.weight(1f))
        // A word never breaks across lines: a one-word title shrinks to fit its line instead.
        val lines = minOf(2, product.labelTitle.split(' ').size)
        ShopLabelText(product.labelTitle, product.ink, size = title, weight = FontWeight.Black, maxLines = lines, shrinks = true)
        ShopLabelText(product.labelFooter, product.ink.copy(alpha = 0.8f), size = footer, weight = FontWeight.SemiBold, tracking = 1f)
    }
}

/**
 * One line of label type, [size] and [tracking] in dp so it never follows the font scale. A
 * [shrinks] line steps down to 60 % of its size to fit.
 */
@Composable
private fun ShopLabelText(
    text: String,
    color: Color,
    size: Float,
    weight: FontWeight,
    tracking: Float = 0f,
    maxLines: Int = 1,
    shrinks: Boolean = false,
) {
    val density = LocalDensity.current
    fun fixed(value: Float): TextUnit = with(density) { value.dp.toSp() }
    BasicText(
        text,
        style = TextStyle(color = color, fontSize = fixed(size), fontWeight = weight, letterSpacing = fixed(tracking)),
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        autoSize = if (shrinks) TextAutoSize.StepBased(minFontSize = fixed(size * 0.6f), maxFontSize = fixed(size)) else null,
    )
}

// Product card

/**
 * A grid card: the product's square label over its name (two lines reserved, so the cards in a row
 * match) and its price, in a frosted card. [interaction] goes after the card (its tap and hold),
 * so the ripple stays inside the card's shape. While [loading], the same card holds the shapes of
 * what it will show, shimmering: the label's square and a bar over each line of the real name and
 * price, so the card is the size it will be. It says nothing to TalkBack.
 */
@Composable
internal fun ShopProductCard(
    product: ShopProduct,
    modifier: Modifier = Modifier,
    interaction: Modifier = Modifier,
    loading: Boolean = false,
) {
    val spacing = AMTheme.tokens.spacing
    val nameStyle = MaterialTheme.typography.titleSmall
    val priceStyle = MaterialTheme.typography.bodyMedium.shopFigures()
    Column(
        modifier
            .fillMaxWidth()
            .amCard(fill = AMSurfaceFill.Frosted)
            .then(if (loading) Modifier.clearAndSetSemantics {} else interaction)
            .padding(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (loading) {
            AMSkeleton.Rect(Modifier.aspectRatio(1f), shape = MaterialTheme.shapes.small)
            AMSkeleton.Text(product.name, style = nameStyle, minLines = 2, maxLines = 2)
            AMSkeleton.Text(product.priceLabel, style = priceStyle, maxLines = 1)
        } else {
            ShopLabelArt(product, ShopLabelStyle.Tile)
            Text(product.name, style = nameStyle, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(product.priceLabel, style = priceStyle, color = AMTheme.Frosted.secondaryText, maxLines = 1)
        }
    }
}

// Totals

/**
 * Subtotal and Shipping ("Free" at 0), one inset divider, then the Total in semibold: bare rows
 * with tabular figures. The Bag, Checkout and the order panel share it.
 */
@Composable
internal fun ShopTotalsRows(subtotal: Int, shipping: Int) {
    ShopAmountRow("Subtotal", shopPrice(subtotal))
    ShopAmountRow("Shipping", if (shipping == 0) "Free" else shopPrice(shipping))
    AMDivider(inset = AMTheme.tokens.spacing.lg)
    AMLabeledContent("Total") {
        Text(
            shopPrice(subtotal + shipping),
            style = MaterialTheme.typography.bodyLarge.shopFigures(),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** A label and an amount, its figures tabular. */
@Composable
internal fun ShopAmountRow(label: String, amount: String) {
    AMLabeledContent(label) { Text(amount, style = MaterialTheme.typography.bodyLarge.shopFigures()) }
}
