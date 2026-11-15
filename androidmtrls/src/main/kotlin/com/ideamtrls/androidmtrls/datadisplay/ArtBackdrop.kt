package com.ideamtrls.androidmtrls

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

// The art recipe: a picture blurred until it reads as its colors, saturated so the averaged colors
// stay vivid, frosted toward the mode (every pixel mixed evenly toward white in light mode, black
// in dark mode), then saturated again to win back the color the frost took, so text in the mode's
// own colors holds over it. The values are the art's own, not tokens.

/** The saturation boost after the full blur, before the wash, so the averaged colors stay vivid. */
private const val ArtSaturation = 2.2f

/** The saturation boost after the light wash, which a white frost takes out of the colors. */
private const val ArtLightWashSaturation = 1.8f

/** The saturation boost after the dark wash, which a black frost takes out of the colors. */
private const val ArtDarkWashSaturation = 1.6f

/**
 * [boost] faded out as [blurRadius] comes down from the default: the boosts make up for the color
 * a heavy blur and a wash take out, so a sharp picture keeps its own color.
 */
private fun artBoost(boost: Float, blurRadius: Dp): Float {
    val share = (blurRadius / AMArtDefaults.BlurRadius).coerceIn(0f, 1f)
    return 1f + (boost - 1f) * share
}

/** How far an art row's color reaches past the midpoint to its neighbor: about the blur. */
private val ArtRowBlend = 120.dp

/** How far the first and last art rows' colors run past the content. */
private val ArtRowBleed = 1000.dp

/**
 * The art's defaults: its blur, and how far it's frosted toward the mode, so text in the mode's own
 * colors holds over any art, against Material's baseline static color scheme. An app with its own
 * scheme checks its colors against them.
 */
public object AMArtDefaults {
    /**
     * The blur, as Android's blur radius: a Gaussian sigma of about 120 dp, so a picture reads as
     * its colors, never its subject.
     */
    public val BlurRadius: Dp = 207.dp

    /**
     * How far art is frosted toward white in light mode: every pixel mixed 70 % of the way to
     * white, so a black picture lands on gray `#B2B3B3`, the darkest light art gets
     * ([Style.ArtInk.LightArtGround]). Over that, `onSurface` holds 8.18:1 and `onSurfaceVariant`
     * 4.44:1, which is 4.5:1 over all but the darkest pictures.
     */
    public const val LightWash: Float = 0.7f

    /**
     * How far art is frosted toward black in dark mode: every pixel mixed 70 % of the way to
     * black, so a white picture lands on gray `#4D4D4D`, and a saturated yellow, the brightest dark
     * art gets ([Style.ArtInk.DarkArtCeiling]), a little above it. Under that, `onSurfaceVariant`
     * holds 4.94:1 and `onSurface` 6.50:1.
     */
    public const val DarkWash: Float = 0.7f
}

/**
 * Draws a picture behind this content as an **art backdrop**: the picture at [url], blurred so
 * heavily it reads as its colors, never its subject, saturated so those colors stay vivid, then
 * washed toward the mode, light in light mode and dark in dark mode, so text in the mode's own
 * colors holds over it. For the color behind an artwork-led screen (an album's page over its
 * cover), or a whole app's background (a photo behind every view).
 *
 * ```kotlin
 * Box(Modifier.fillMaxSize().amArtBackdrop(album.coverUrl)) {
 *     Text(album.title, Modifier.amArtText(), style = MaterialTheme.typography.titleLarge)
 * }
 *
 * Box(Modifier.fillMaxSize().amArtBackdrop(beansUrl, blurRadius = 4.dp)) { Shop() }   // its subject shows
 * ```
 *
 * It draws like a background, filling the element it's on. Frosted surfaces sample an
 * [AMBackdrop]'s art, and the components on it take their over-art look, so for frosted cards and
 * headers on it, make it that backdrop's art:
 *
 * ```kotlin
 * AMBackdrop(art = { Box(Modifier.fillMaxSize().amArtBackdrop(album.coverUrl)) }, Modifier.fillMaxSize()) {
 *     AlbumPage(album)   // its frosted cards blur this art, and its section headers sit on it
 * }
 * ```
 *
 * - **The wash is a frost that follows the mode.** Every pixel is mixed evenly toward white in
 *   light mode, by [lightWash], or toward black in dark mode, by [darkWash], in sRGB, then
 *   saturated again to win back some of the color the frost took. At the defaults
 *   ([AMArtDefaults]) text in the mode's colors holds over any picture, so the art's tone is the
 *   mode's.
 * - **Lower washes are the app's call.** Lowering [lightWash] or [darkWash] shows more of the
 *   picture and gives up that guarantee: check every text on it, and give the [AMBackdrop] around
 *   it the tone the art then has ([AMArtTone]). Frosted glass over art an app chose not to wash
 *   (low washes, or a picture drawn without this modifier) is the app's to check too: the glass
 *   holds its contrast over the library's washed art only.
 * - **Blur.** By default the picture reads only as its colors. Lower [blurRadius] to keep its
 *   subject recognizable; both saturation boosts fade out with it, so a sharp picture keeps its
 *   own color.
 * - **Made once.** The picture comes through the library's image pipeline, decoded small; it's
 *   blurred, saturated, washed and saturated again once and kept, so drawing it costs a stretched
 *   bitmap. A picture
 *   in memory shows on the first frame.
 * - **Changing pictures.** When [url] changes, the new picture fades in over the old with
 *   Material's motion; a change made inside [amInstantSwitch], or while the system's Remove
 *   animations is on, lands at once. A resize or a change of mode swaps without a fade.
 * - **No picture means the page color.** Before it loads, on a failure and for a null [url], the
 *   region shows [AMTheme.Surface.page]; the picture fades in over it.
 *
 * It's decoration: it adds nothing for a screen reader and takes no input. For a picture the app
 * already holds as a bitmap, use the `ImageBitmap` form.
 *
 * @param url the picture; null shows the page color.
 * @param blurRadius the blur, as Android's blur radius: by default a Gaussian sigma of about
 *   120 dp, so the picture reads as its colors.
 * @param lightWash how far the picture is frosted toward white in light mode, from 0 (as it is) to
 *   1 (white). Below the default, text on it may not hold; check it.
 * @param darkWash how far the picture is frosted toward black in dark mode, from 0 (as it is) to 1
 *   (black). Below the default, text on it may not hold; check it.
 */
public fun Modifier.amArtBackdrop(
    url: String?,
    blurRadius: Dp = AMArtDefaults.BlurRadius,
    lightWash: Float = AMArtDefaults.LightWash,
    darkWash: Float = AMArtDefaults.DarkWash,
): Modifier = artBackdrop(url?.let { ArtSource.Url(it) }, artRecipe(blurRadius, lightWash, darkWash))

/**
 * Draws [image] behind this content as an **art backdrop**, the same way the URL form draws a
 * picture it loads: for art the app already holds as a bitmap, such as a cover embedded in a music
 * file.
 *
 * ```kotlin
 * val cover: ImageBitmap? = track.embeddedCover   // null until it's read
 * Box(Modifier.fillMaxSize().amArtBackdrop(cover)) { NowPlaying(track) }
 * ```
 *
 * The bitmap is averaged down, blurred, saturated, washed and saturated again once and kept, known by the bitmap
 * itself: wrapping the same bitmap again (`asImageBitmap()`) finds the same picture, and the kept
 * picture never keeps the bitmap alive. It's read as it stands, so put new art in a new bitmap
 * rather than drawing into one already shown. Changing bitmaps fades as changing URLs does. The
 * washes and their limits are the URL form's.
 *
 * @param image the picture; null (no art yet) shows the page color.
 * @param blurRadius the blur, as Android's blur radius: by default a Gaussian sigma of about
 *   120 dp, so the picture reads as its colors.
 * @param lightWash how far the picture is frosted toward white in light mode, from 0 to 1. Below
 *   the default, text on it may not hold; check it.
 * @param darkWash how far the picture is frosted toward black in dark mode, from 0 to 1. Below the
 *   default, text on it may not hold; check it.
 */
public fun Modifier.amArtBackdrop(
    image: ImageBitmap?,
    blurRadius: Dp = AMArtDefaults.BlurRadius,
    lightWash: Float = AMArtDefaults.LightWash,
    darkWash: Float = AMArtDefaults.DarkWash,
): Modifier = artBackdrop(image?.let { ArtSource.Image(it) }, artRecipe(blurRadius, lightWash, darkWash))

/**
 * Draws the colors of the [amArtRow] rows inside this content behind it, at its full width: color
 * at the row level. Each row's picture, blurred, saturated and washed toward the mode with the art
 * backdrop's recipe, lies behind that row and scrolls with it; neighbors' colors blend into each
 * other halfway between them, with no line. Text in the mode's own colors holds over them.
 *
 * ```kotlin
 * Column(Modifier.verticalScroll(rememberScrollState()).amArtRows()) {
 *     artists.forEach { artist ->
 *         ArtistEntry(artist, Modifier.padding(horizontal = 16.dp).amArtRow(artist.photoUrl))
 *     }
 * }
 * ```
 *
 * Put it on the scrolling content, right after the scroll and outside its side gutters, so the
 * color runs edge to edge and moves with the rows; nothing behind the page swaps as it scrolls.
 * The first and last colors run well past the content, for overscroll and short pages, so the
 * scrolling container's clip bounds them. Rows report in page order. Where no row's picture has
 * arrived yet, where it fails, and for a null one, the band is the page color, blending into its
 * neighbors as any band does; a row whose picture changes keeps its last one until the new one is
 * ready. A page with no rows is the page color by itself. A row's
 * picture is a URL or a bitmap the app holds, and one page can mix them.
 *
 * On its own, the colors are this content's background. For frosted surfaces over the rows (a
 * collapsing header's fill, frosted buttons, a frosted sheet) to blur them, and for the components
 * on them to take their over-art look, put the page in an [AMArtRowsBackdrop]: the colors then draw
 * as that backdrop's art, behind the whole page.
 *
 * @param lightWash how far the rows' pictures are frosted toward white in light mode, from 0 to 1,
 *   as on [amArtBackdrop]. Below the default, text on them may not hold, and frosted glass over
 *   them is the app's to check.
 * @param darkWash how far they're frosted toward black in dark mode, from 0 to 1. Below the
 *   default, the same holds.
 */
public fun Modifier.amArtRows(
    lightWash: Float = AMArtDefaults.LightWash,
    darkWash: Float = AMArtDefaults.DarkWash,
): Modifier = this then ArtRowsElement(lightWash, darkWash)

/**
 * A region whose art is the art rows inside it: the colors of the [amArtRows] page in [content]
 * draw behind the whole region, the top app bar's place included, moving with the rows as they
 * scroll, and every [AMSurfaceFill.Frosted] surface in [content] blurs the colors under it, as it
 * would an [AMBackdrop]'s art. Section headers, page headers and the other components on it take
 * their over-art look for its [tone].
 *
 * ```kotlin
 * AMArtRowsBackdrop(Modifier.fillMaxSize()) {
 *     Scaffold(
 *         topBar = { AMPageHeader("Artists", scrollBehavior = scrollBehavior) },   // its fill frosts the rows under it
 *         containerColor = Color.Transparent,
 *     ) { padding ->
 *         Column(Modifier.verticalScroll(rememberScrollState()).amArtRows().padding(padding)) {
 *             artists.forEach { ArtistEntry(it, Modifier.padding(horizontal = 16.dp).amArtRow(it.photoUrl)) }
 *         }
 *     }
 * }
 * ```
 *
 * - **The rows stay where they are.** The page marks its scrolling content with [amArtRows] and
 *   its rows with [amArtRow], as on any art-row page; inside this region the content draws no
 *   background of its own and hands its rows to the region, which draws them, blended and washed
 *   the same way, behind everything in it. The region's own fill, where no row reaches, is the
 *   page color: a page with no rows (loading, empty) sits on the page color by itself, with no
 *   switching.
 * - **Frosted surfaces** anywhere in [content] sample the rows: a header over them, cards and
 *   buttons on them, and a sheet opened from here. Only the colors are sampled, never the content,
 *   so a surface never blurs itself.
 * - It's decoration: it adds nothing for a screen reader and takes no input.
 *
 * @param modifier the region's size and placement: usually the whole screen.
 * @param tone how light the rows' art is, for what lies on it and for the status bar's icons
 *   while the region reaches the top of the window: null, the default, is the mode's, which the
 *   default washes guarantee. A page that lowers its rows' washes passes the tone its art then has.
 * @param content the page, holding one [amArtRows] content.
 */
@Composable
public fun AMArtRowsBackdrop(
    modifier: Modifier = Modifier,
    tone: AMArtTone? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val source = remember(layer) { AMBackdropSource(layer) }
    val art = remember(source) { ArtRowsArt(source) }
    val artTone = tone ?: modeArtTone()
    Box(modifier.then(artStatusBar(source, artTone))) {
        Box(Modifier.matchParentSize().backdropSource(source).then(ArtRowsArtElement(art)))
        val scope = this
        CompositionLocalProvider(
            LocalAMBackdrop provides source,
            LocalArtRowsArt provides art,
            LocalAMArtTone provides artTone,
            sceneBackdrop(source),
        ) { scope.content() }
    }
}

/**
 * Gives this row its own color on an [amArtRows] page: the picture at [url], through the art
 * backdrop's recipe, lies behind the row and scrolls with it, blending into its neighbors' colors.
 * A row outside an [amArtRows] draws nothing extra. For a picture the app already holds as a
 * bitmap, use the `ImageBitmap` form.
 *
 * @param url the row's picture; null leaves its band the page color.
 */
public fun Modifier.amArtRow(url: String?): Modifier = this then ArtRowElement(url?.let { ArtSource.Url(it) })

/**
 * Gives this row its own color on an [amArtRows] page from [image], a bitmap the app already holds
 * (a cover embedded in a music file), the same way the URL form does from a picture it loads. The
 * bitmap is processed once per band and kept, known by the bitmap itself, as [amArtBackdrop]'s
 * `ImageBitmap` form describes.
 *
 * @param image the row's picture; null (no art yet) leaves its band the page color.
 */
public fun Modifier.amArtRow(image: ImageBitmap?): Modifier = this then ArtRowElement(image?.let { ArtSource.Image(it) })

/**
 * Text that lies right on art, not on a card: drawn in the ink that reads on the art's tone, with
 * a soft shadow of the other tone under its glyphs, so its edges hold over the art's brightest
 * and darkest colors.
 *
 * ```kotlin
 * Text("Albums", Modifier.amArtText(), style = MaterialTheme.typography.titleLarge)
 * Column {
 *     Text(album.title, Modifier.amArtText())
 *     Text(album.artist, Modifier.amArtText(secondary = true))
 * }
 * Text("Paused", Modifier.amArtText(AMArtTone.Dark))   // white, on a scrim over a photo
 * ```
 *
 * - **The tone** is [tone] when given, else the nearest art surface's ([AMBackdrop],
 *   [AMArtRowsBackdrop]), which is the mode's unless that surface was given one, else the mode's.
 * - **On art in the mode's tone**, which the library's washes guarantee ([AMArtDefaults]), the ink
 *   is the scheme's own text color, `onSurface`, or `onSurfaceVariant` for [secondary], so text on
 *   art reads like text on the page and holds 4.5:1 over the washed art.
 * - **On art of the other tone** (a lowered wash, sharp art, a scrim over a photo), the ink is
 *   white, or a light gray for [secondary], on dark art, and near-black, or a dark gray, on light
 *   art. Contrast there depends on the art; check it.
 *
 * Put it on a text or on a group of them: everything it covers draws in the ink, glyphs included,
 * keeping their alpha, with the shadow drawn from the glyphs themselves.
 *
 * @param tone the art's tone under the text, for a ground the library can't know: null takes the
 *   nearest art surface's, or the mode's.
 * @param secondary the secondary ink, for a supporting line under a title.
 */
public fun Modifier.amArtText(tone: AMArtTone? = null, secondary: Boolean = false): Modifier =
    artGlyphs(ArtGlyphs(tone, secondary))

// The art backdrop

/**
 * A backdrop's recipe: its blur, its saturation before the wash, its washes toward the mode, and
 * the saturation after each.
 */
@Immutable
private data class ArtRecipe(
    val blurRadius: Dp,
    val saturation: Float,
    val lightWash: Float,
    val darkWash: Float,
    val lightWashSaturation: Float,
    val darkWashSaturation: Float,
) {
    fun wash(dark: Boolean): Float = if (dark) darkWash else lightWash

    fun washSaturation(dark: Boolean): Float = if (dark) darkWashSaturation else lightWashSaturation
}

/** A recipe for [blurRadius]: every saturation boost fades out with the blur ([artBoost]). */
private fun artRecipe(blurRadius: Dp, lightWash: Float, darkWash: Float): ArtRecipe = ArtRecipe(
    blurRadius = blurRadius,
    saturation = artBoost(ArtSaturation, blurRadius),
    lightWash = lightWash.coerceIn(0f, 1f),
    darkWash = darkWash.coerceIn(0f, 1f),
    lightWashSaturation = artBoost(ArtLightWashSaturation, blurRadius),
    darkWashSaturation = artBoost(ArtDarkWashSaturation, blurRadius),
)

private fun Modifier.artBackdrop(source: ArtSource?, recipe: ArtRecipe): Modifier =
    this then ArtBackdropElement(source, recipe, InstantSwitch.generation)

private data class ArtBackdropElement(val source: ArtSource?, val recipe: ArtRecipe, val generation: Int) :
    ModifierNodeElement<ArtBackdropNode>() {
    override fun create() = ArtBackdropNode(source, recipe, generation)

    override fun update(node: ArtBackdropNode) = node.update(source, recipe, generation)

    override fun InspectorInfo.inspectableProperties() {
        name = "amArtBackdrop"
        when (source) {
            is ArtSource.Url -> properties["url"] = source.url
            is ArtSource.Image -> properties["image"] = source.image
            null -> properties["url"] = null
        }
        properties["blurRadius"] = recipe.blurRadius
        properties["lightWash"] = recipe.lightWash
        properties["darkWash"] = recipe.darkWash
    }
}

/**
 * Loads its picture for its frame and the mode once it knows its size, and draws it behind the
 * content: the page color, the picture leaving (while a new one fades in), the picture showing.
 * A change of mode loads the picture washed for the new one, showing the old until it's ready.
 */
private class ArtBackdropNode(
    private var source: ArtSource?,
    private var recipe: ArtRecipe,
    private var generation: Int,
) : Modifier.Node(), DrawModifierNode, LayoutAwareModifierNode, CompositionLocalConsumerModifierNode, ObserverModifierNode {

    private var frame = IntSize.Zero
    private var requested: ArtKey? = null
    private var loading: Job? = null
    private var dark = false

    /** The picture showing, and the source it shows. */
    private var shown: ImageBitmap? = null
    private var shownSource: ArtSource? = null

    /** The picture under [shown] while [reveal] fades it in. */
    private var leaving: ImageBitmap? = null
    private val reveal = Animatable(1f)

    private var hasDrawn = false

    /** Whether the source changed inside [amInstantSwitch], so its picture lands without a fade. */
    private var instant = false

    fun update(source: ArtSource?, recipe: ArtRecipe, generation: Int) {
        if (source != this.source && generation != this.generation) instant = true
        this.source = source
        this.recipe = recipe
        this.generation = generation
        load()
    }

    override fun onAttach() {
        // A fade cut short by leaving ends where it was; coming back, the picture shows in full.
        if (reveal.value < 1f) coroutineScope.launch { reveal.snapTo(1f) }
        onObservedReadsChanged()
    }

    // The mode decides the wash, so a change of mode loads the picture again.
    override fun onObservedReadsChanged() {
        observeReads { dark = currentValueOf(LocalAMDarkTheme) ?: false }
        load()
    }

    override fun onDetach() {
        requested = null
        loading = null
        leaving = null
        hasDrawn = false
    }

    override fun onRemeasured(size: IntSize) {
        if (size == frame) return
        frame = size
        load()
    }

    private fun load() {
        if (!isAttached) return
        val source = source
        if (source == null) {
            loading?.cancel()
            loading = null
            requested = null
            show(null, null)
            return
        }
        if (frame.width <= 0 || frame.height <= 0) return
        val blurRadius = with(requireDensity()) { recipe.blurRadius.toPx() }
        val key = artKey(source, frame, blurRadius, recipe.saturation, dark, recipe.wash(dark), recipe.washSaturation(dark), fade = 0f)
        if (key == requested) return
        requested = key
        loading?.cancel()
        loading = null
        val cached = ArtImages.cached(key)
        if (cached != null) {
            show(cached, source)
            return
        }
        val pipeline = ImagePipeline.shared(currentValueOf(LocalContext))
        loading = coroutineScope.launch {
            val image = try {
                ArtImages.load(pipeline, source, key)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            // A failed resize keeps the picture showing; a failed new picture shows the page.
            if (image != null) show(image, source) else if (source != shownSource) show(null, source)
        }
    }

    private fun show(image: ImageBitmap?, source: ArtSource?) {
        if (image === shown && source == shownSource) return
        val previous = shown
        val sameArt = source == shownSource
        shown = image
        shownSource = source
        invalidateDraw()
        val snap = sameArt || !hasDrawn || instant || (previous == null && image == null)
        instant = false
        if (snap) {
            leaving = null
            coroutineScope.launch { reveal.snapTo(1f) }
            return
        }
        leaving = previous
        val spec = currentValueOf(MaterialTheme.LocalMaterialTheme).motionScheme.defaultEffectsSpec<Float>()
        coroutineScope.launch {
            reveal.snapTo(0f)
            reveal.animateTo(1f, spec)
            leaving = null
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        hasDrawn = true
        drawRect(pageColor(currentValueOf(LocalAMDarkTheme) ?: false))
        val opacity = reveal.value
        if (opacity < 1f) leaving?.let { drawArt(it, Offset.Zero, size, 1f) }
        shown?.let { drawArt(it, Offset.Zero, size, opacity) }
        drawContent()
    }
}

/** What art shows where it has no picture: the mode's page color. */
private fun pageColor(dark: Boolean): Color = if (dark) Style.Page.Dark else Style.Page.Light

/** Stretches a processed picture over [size] at [topLeft]; it's already in the frame's shape. */
private fun DrawScope.drawArt(image: ImageBitmap, topLeft: Offset, size: Size, alpha: Float) {
    drawImage(
        image,
        dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
        dstSize = IntSize(ceil(size.width).toInt(), ceil(size.height).toInt()),
        alpha = alpha,
        filterQuality = FilterQuality.Low,
    )
}

/**
 * A band with no picture: the page [color] across [width], fading in over the band's top as a
 * picture's baked fade does, so the band above blends into it.
 */
private fun DrawScope.drawPageBand(band: ArtBand, width: Float, color: Color) {
    val opaqueTop = band.top + band.fade
    if (band.fade > 0f) {
        drawRect(
            Brush.verticalGradient(listOf(color.copy(alpha = 0f), color), startY = band.top, endY = opaqueTop),
            Offset(0f, band.top),
            Size(width, band.fade),
        )
    }
    drawRect(color, Offset(0f, opaqueTop), Size(width, band.bottom - opaqueTop))
}

// The art rows

/** What an art row looks for among its ancestors: the nearest [amArtRows]. */
private object ArtRowsKey

private data class ArtRowsElement(val lightWash: Float, val darkWash: Float) : ModifierNodeElement<ArtRowsNode>() {
    override fun create() = ArtRowsNode(lightWash, darkWash)

    override fun update(node: ArtRowsNode) = node.update(lightWash, darkWash)

    override fun InspectorInfo.inspectableProperties() {
        name = "amArtRows"
        properties["lightWash"] = lightWash
        properties["darkWash"] = darkWash
    }
}

/**
 * The rows' container: the rows register with it, and it draws their bands behind its content,
 * loading each band's picture, washed for the mode, the first time it's needed. Inside an
 * [AMArtRowsBackdrop] (when that backdrop is the nearest one) it draws nothing itself and hands its
 * bands to the backdrop, telling it to redraw whenever a row or the content itself moves.
 */
private class ArtRowsNode(private var lightWash: Float, private var darkWash: Float) :
    Modifier.Node(),
    DrawModifierNode,
    TraversableNode,
    LayoutAwareModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {

    override val traverseKey: Any get() = ArtRowsKey

    private val rows = mutableListOf<ArtRowNode>()
    private val loading = HashSet<ArtKey>()
    private val failed = HashSet<ArtKey>()

    /** The backdrop that draws the bands, or null when the content draws them itself. */
    private var backdrop: ArtRowsArt? = null

    /** Where the container sits, for measuring its rows against. */
    var coordinates: LayoutCoordinates? = null
        private set

    fun update(lightWash: Float, darkWash: Float) {
        if (lightWash == this.lightWash && darkWash == this.darkWash) return
        this.lightWash = lightWash
        this.darkWash = darkWash
        changed()
    }

    fun register(row: ArtRowNode) {
        rows += row
        changed()
    }

    fun unregister(row: ArtRowNode) {
        rows -= row
        changed()
    }

    /** A row moved within the container, or changed its picture. */
    fun changed() {
        if (!isAttached) return
        val backdrop = backdrop
        if (backdrop != null) backdrop.invalidate() else invalidateDraw()
    }

    override fun onAttach() {
        // Only a backdrop that is the nearest one takes the bands: inside a sheet or an overlay,
        // or under an AMBackdrop of its own, the content keeps drawing them.
        val art = currentValueOf(LocalArtRowsArt)
        backdrop = art?.takeIf { currentValueOf(LocalAMBackdrop) === it.source }?.also { it.register(this) }
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        // The content scrolled or moved: the backdrop draws the bands where it is now.
        backdrop?.invalidate()
    }

    override fun onDetach() {
        backdrop?.unregister(this)
        backdrop = null
        coordinates = null
        loading.clear()
        failed.clear()
    }

    override fun ContentDrawScope.draw() {
        if (backdrop == null) {
            val bleed = ArtRowBleed.toPx()
            drawRect(pageColor(currentValueOf(LocalAMDarkTheme) ?: false), Offset(0f, -bleed), Size(size.width, size.height + bleed * 2f))
            drawBands(size)
        }
        drawContent()
    }

    /**
     * Draws each row's band, in this container's frame, for content of [contentSize]: the colors
     * alone, over the page color the caller drew. A band whose picture for its frame and the mode
     * isn't ready draws its row's last picture stretched to the band, until the new one is; a band
     * with no picture at all is the page color, faded in like a picture, so the band above blends
     * into it instead of ending in a line. Over nothing but the page color it has nothing to cover,
     * so a page whose bands have no pictures stays exactly the page color.
     */
    fun DrawScope.drawBands(contentSize: Size) {
        val container = coordinates?.takeIf { it.isAttached } ?: return
        val dark = currentValueOf(LocalAMDarkTheme) ?: false
        val wash = if (dark) darkWash else lightWash
        val placed = rows.mapNotNull { row ->
            val bounds = row.coordinates?.takeIf { it.isAttached }?.let { container.localBoundingBoxOf(it, clipBounds = false) }
            bounds?.let { row to it }
        }.sortedBy { it.second.top }
        val bands = artRowBands(
            placed.map { ArtSpan(it.second.top, it.second.bottom) },
            contentSize.height,
            ArtRowBlend.toPx(),
            ArtRowBleed.toPx(),
        )
        val blurRadius = AMArtDefaults.BlurRadius.toPx()
        val page = pageColor(dark)
        // How far down the pictures drawn so far reach: a band with no picture above it lies on
        // the page color alone.
        var artBottom = Float.NEGATIVE_INFINITY
        placed.forEachIndexed { index, (row, _) ->
            val band = bands[index]
            val bandSize = Size(contentSize.width, band.bottom - band.top)
            val frame = IntSize(ceil(bandSize.width).toInt(), ceil(bandSize.height).toInt())
            if (frame.width <= 0 || frame.height <= 0) return@forEachIndexed
            val source = row.source
            val picture = if (source == null) {
                null
            } else {
                val washSaturation = if (dark) ArtDarkWashSaturation else ArtLightWashSaturation
                val key = artKey(source, frame, blurRadius, ArtSaturation, dark, wash, washSaturation, band.fade)
                val image = ArtImages.cached(key)
                if (image == null) request(source, key) else row.lastPicture = image
                // Until its picture for this band is ready, a band keeps the one it showed last.
                image ?: row.lastPicture
            }
            if (picture != null) {
                drawArt(picture, Offset(0f, band.top), bandSize, 1f)
                artBottom = max(artBottom, band.bottom)
            } else if (band.top < artBottom) {
                drawPageBand(band, contentSize.width, page)
            }
        }
    }

    private fun request(source: ArtSource, key: ArtKey) {
        if (!isAttached || key in loading || key in failed) return
        loading += key
        val pipeline = ImagePipeline.shared(currentValueOf(LocalContext))
        coroutineScope.launch {
            val loaded = try {
                ArtImages.load(pipeline, source, key)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
            loading -= key
            if (!loaded) failed += key
            changed()
        }
    }
}

/**
 * An [AMArtRowsBackdrop]'s art: the [amArtRows] contents inside it register here, and the
 * backdrop's art node draws their bands. [source] is the backdrop the frosted surfaces sample.
 */
private class ArtRowsArt(val source: AMBackdropSource) {
    val contents = mutableListOf<ArtRowsNode>()
    var node: ArtRowsArtNode? = null

    fun register(content: ArtRowsNode) {
        contents += content
        invalidate()
    }

    fun unregister(content: ArtRowsNode) {
        contents -= content
        invalidate()
    }

    fun invalidate() {
        node?.takeIf { it.isAttached }?.invalidateDraw()
    }
}

/** The [ArtRowsArt] of the nearest [AMArtRowsBackdrop], or null outside one. */
private val LocalArtRowsArt = staticCompositionLocalOf<ArtRowsArt?> { null }

private class ArtRowsArtElement(val art: ArtRowsArt) : ModifierNodeElement<ArtRowsArtNode>() {
    override fun create() = ArtRowsArtNode(art)

    override fun update(node: ArtRowsArtNode) = node.update(art)

    override fun equals(other: Any?): Boolean = other is ArtRowsArtElement && other.art === art

    override fun hashCode(): Int = System.identityHashCode(art)

    override fun InspectorInfo.inspectableProperties() {
        name = "artRowsArt"
    }
}

/**
 * Draws an [AMArtRowsBackdrop]'s art across the region: the page color, then each registered
 * content's bands at the content's place in the region, all cut to the region.
 */
private class ArtRowsArtNode(private var art: ArtRowsArt) :
    Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    fun update(art: ArtRowsArt) {
        if (art === this.art) return
        this.art.node = null
        this.art = art
        art.node = this
        invalidateDraw()
    }

    override fun onAttach() {
        art.node = this
    }

    override fun onDetach() {
        if (art.node === this) art.node = null
    }

    override fun ContentDrawScope.draw() {
        val region = requireLayoutCoordinates()
        clipRect {
            drawRect(pageColor(currentValueOf(LocalAMDarkTheme) ?: false))
            art.contents.forEach { content ->
                val placed = content.coordinates?.takeIf { it.isAttached } ?: return@forEach
                val offset = region.localPositionOf(placed, Offset.Zero)
                translate(offset.x, offset.y) {
                    with(content) { drawBands(placed.size.toSize()) }
                }
            }
        }
        drawContent()
    }
}

private data class ArtRowElement(val source: ArtSource?) : ModifierNodeElement<ArtRowNode>() {
    override fun create() = ArtRowNode(source)

    override fun update(node: ArtRowNode) = node.update(source)

    override fun InspectorInfo.inspectableProperties() {
        name = "amArtRow"
        when (source) {
            is ArtSource.Url -> properties["url"] = source.url
            is ArtSource.Image -> properties["image"] = source.image
            null -> properties["url"] = null
        }
    }
}

/** One row: it registers with the nearest [ArtRowsNode] and tells it when it moves within it. */
private class ArtRowNode(source: ArtSource?) : Modifier.Node(), GlobalPositionAwareModifierNode {

    /** The row's picture: a URL or the app's bitmap, held for as long as the row shows it. */
    var source: ArtSource? = source
        private set

    /** Where the row sits, once it's placed. */
    var coordinates: LayoutCoordinates? = null
        private set

    /** The picture the row's band showed last, which it keeps until its next one is ready. */
    var lastPicture: ImageBitmap? = null

    private var container: ArtRowsNode? = null
    private var bounds: Rect? = null

    fun update(source: ArtSource?) {
        if (source == this.source) return
        this.source = source
        if (source == null) lastPicture = null
        container?.changed()
    }

    override fun onAttach() {
        container = (findNearestAncestor(ArtRowsKey) as? ArtRowsNode)?.also { it.register(this) }
    }

    override fun onDetach() {
        container?.unregister(this)
        container = null
        coordinates = null
        bounds = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        val container = container ?: return
        val outer = container.coordinates?.takeIf { it.isAttached }
        if (outer == null || !coordinates.isAttached) {
            container.changed()
            return
        }
        // Only a move within the container redraws it; scrolling the whole container doesn't.
        val inContainer = outer.localBoundingBoxOf(coordinates, clipBounds = false)
        if (inContainer != bounds) {
            bounds = inContainer
            container.changed()
        }
    }
}
