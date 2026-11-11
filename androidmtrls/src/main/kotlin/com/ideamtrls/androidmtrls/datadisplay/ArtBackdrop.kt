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
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.invalidateDraw
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
import kotlin.math.roundToInt

// The art recipe: a picture blurred until it reads as its colors, saturated so the averaged colors
// stay vivid, then washed with black so white art text holds over it. The values are the art's
// own, not tokens.

/** The art's blur, as Android's blur radius: a Gaussian sigma of about 120 dp. */
private val ArtBlurRadius = 207.dp

/** The saturation boost after the blur, so the averaged colors stay vivid. */
private const val ArtSaturation = 2.2f

/**
 * The black wash over the art: enough in light mode for white text to hold 4.5:1 over bright
 * pictures, heavier in dark mode.
 */
private const val ArtWashLight = 0.14f
private const val ArtWashDark = 0.45f

/**
 * What lies under the art's wash while it has no picture (loading, failed, none): a dark neutral,
 * the same in both appearances, so white art text holds from the first frame instead of sitting
 * on the light page.
 */
private val ArtFallback = Style.Surface.Art

/** How far an art row's color reaches past the midpoint to its neighbor: about the blur. */
private val ArtRowBlend = 120.dp

/** How far the first and last art rows' colors run past the content. */
private val ArtRowBleed = 1000.dp

/**
 * Draws a picture behind this content as an **art backdrop**: the picture at [url], blurred so
 * heavily it reads as its colors, never its subject, saturated so those colors stay vivid, then
 * washed with black (more in dark mode) so white [amArtText] holds over it. For the color behind
 * an artwork-led screen: an album's page over its cover.
 *
 * ```kotlin
 * Box(Modifier.fillMaxSize().amArtBackdrop(album.coverUrl)) {
 *     Text(album.title, Modifier.amArtText(), style = MaterialTheme.typography.titleLarge)
 * }
 * ```
 *
 * It draws like a background, filling the element it's on. Frosted surfaces sample an
 * [AMBackdrop]'s art, so for frosted cards on it, make it that backdrop's art:
 *
 * ```kotlin
 * AMBackdrop(art = { Box(Modifier.fillMaxSize().amArtBackdrop(album.coverUrl)) }, Modifier.fillMaxSize()) {
 *     AlbumPage(album)   // its frosted cards blur this art, and its section headers sit on it
 * }
 * ```
 *
 * - **Made once.** The picture comes through the library's image pipeline, decoded small; it's
 *   blurred and saturated once and kept, so drawing it costs a stretched bitmap. A picture in
 *   memory shows on the first frame.
 * - **Changing pictures.** When [url] changes, the new picture fades in over the old with
 *   Material's motion, under the same wash; a change made inside [amInstantSwitch], or while the
 *   system's Remove animations is on, lands at once. A resize swaps without a fade.
 * - **Before it loads**, on a failure and for a null [url], the region shows a dark neutral under
 *   the wash, in both appearances, so white [amArtText] holds while there's no picture; the
 *   picture fades in over it.
 *
 * It's decoration: it adds nothing for a screen reader and takes no input. For a picture the app
 * already holds as a bitmap, use the `ImageBitmap` form.
 *
 * @param url the picture; null shows the dark neutral under the wash.
 */
public fun Modifier.amArtBackdrop(url: String?): Modifier = artBackdrop(url?.let { ArtSource.Url(it) }, ArtBackdropRecipe)

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
 * The bitmap is averaged down, blurred and saturated once and kept, known by the bitmap itself:
 * wrapping the same bitmap again (`asImageBitmap()`) finds the same picture, and the kept picture
 * never keeps the bitmap alive. It's read as it stands, so put new art in a new bitmap rather than
 * drawing into one already shown. Changing bitmaps fades as changing URLs does.
 *
 * @param image the picture; null (no art yet) shows the dark neutral under the wash.
 */
public fun Modifier.amArtBackdrop(image: ImageBitmap?): Modifier =
    artBackdrop(image?.let { ArtSource.Image(it) }, ArtBackdropRecipe)

/**
 * Draws a picture behind this content as a whole app's background: the art backdrop's recipe
 * ([amArtBackdrop]), then lifted toward white in light mode and pushed toward black in dark mode,
 * so it suits the appearance. Text on it uses the scheme's own colors (`onSurface`), not
 * [amArtText], and cards on it are usually frosted.
 *
 * ```kotlin
 * AMBackdrop(art = { Box(Modifier.fillMaxSize().amPageBackdrop(month.photoUrl)) }, Modifier.fillMaxSize()) { MonthView(month) }
 *
 * Box(Modifier.fillMaxSize().amPageBackdrop(beansUrl, blurRadius = 4.dp, lightWash = 0.55f, darkWash = 0.4f)) { Shop() }
 * ```
 *
 * By default the picture reads only as its colors. Lower [blurRadius] to keep its subject
 * recognizable (the saturation boost fades out with it, so a sharp picture keeps its own color),
 * and lower the washes to let more of it show through; check text on it when you do. As with the
 * art backdrop, make it an [AMBackdrop]'s art for frosted cards to sample it; it fades between
 * pictures the same way and shows the page color until its picture arrives. For a picture the app
 * already holds as a bitmap, use the `ImageBitmap` form.
 *
 * @param url the picture; null shows the page color under the washes.
 * @param blurRadius the blur, as Android's blur radius: by default the art backdrop's, a Gaussian
 *   sigma of about 120 dp.
 * @param lightWash how much white lies over the picture in light mode, from 0 to 1.
 * @param darkWash how much black lies over the picture in dark mode, from 0 to 1.
 */
public fun Modifier.amPageBackdrop(
    url: String?,
    blurRadius: Dp = ArtBlurRadius,
    lightWash: Float = 0.6f,
    darkWash: Float = 0.45f,
): Modifier = artBackdrop(url?.let { ArtSource.Url(it) }, pageRecipe(blurRadius, lightWash, darkWash))

/**
 * Draws [image] behind this content as a whole app's background, the same way the URL form draws
 * a picture it loads: for art the app already holds as a bitmap. The bitmap is processed once and
 * kept, known by the bitmap itself, as [amArtBackdrop]'s `ImageBitmap` form describes.
 *
 * @param image the picture; null (no art yet) shows the page color under the washes.
 * @param blurRadius the blur, as Android's blur radius: by default the art backdrop's, a Gaussian
 *   sigma of about 120 dp.
 * @param lightWash how much white lies over the picture in light mode, from 0 to 1.
 * @param darkWash how much black lies over the picture in dark mode, from 0 to 1.
 */
public fun Modifier.amPageBackdrop(
    image: ImageBitmap?,
    blurRadius: Dp = ArtBlurRadius,
    lightWash: Float = 0.6f,
    darkWash: Float = 0.45f,
): Modifier = artBackdrop(image?.let { ArtSource.Image(it) }, pageRecipe(blurRadius, lightWash, darkWash))

/**
 * Draws the colors of the [amArtRow] rows inside this content behind it, at its full width: color
 * at the row level. Each row's picture, blurred and saturated with the art backdrop's recipe, lies
 * behind that row and scrolls with it; neighbors' colors blend into each other halfway between
 * them, with no line, and one black wash lies over all of them, so white [amArtText] holds.
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
 * scrolling container's clip bounds them. Rows report in page order; until a row's picture
 * arrives, and for a null one, its band shows a dark neutral under the wash, so the art text
 * holds. A row's picture is a URL or a bitmap the app holds, and one page can mix them.
 *
 * On its own, the colors are this content's background. For frosted surfaces over the rows (a
 * collapsing header's fill, frosted buttons, a frosted sheet) to blur them, put the page in an
 * [AMArtRowsBackdrop]: the colors then draw as that backdrop's art, behind the whole page, and
 * every frosted surface samples them.
 */
public fun Modifier.amArtRows(): Modifier = this then ArtRowsElement

/**
 * A region whose art is the art rows inside it: the colors of the [amArtRows] page in [content]
 * draw behind the whole region, the top app bar's place included, moving with the rows as they
 * scroll, and every [AMSurfaceFill.Frosted] surface in [content] blurs the colors under it, as it
 * would an [AMBackdrop]'s art. Section headers and page headers take their over-art look.
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
 *   dark neutral under the wash.
 * - **Frosted surfaces** anywhere in [content] sample the rows: a header over them, cards and
 *   buttons on them, and a sheet opened from here. Only the colors are sampled, never the content,
 *   so a surface never blurs itself.
 * - It's decoration: it adds nothing for a screen reader and takes no input.
 *
 * @param modifier the region's size and placement: usually the whole screen.
 * @param content the page, holding one [amArtRows] content.
 */
@Composable
public fun AMArtRowsBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val source = remember(layer) { AMBackdropSource(layer) }
    val art = remember(source) { ArtRowsArt(source) }
    Box(modifier) {
        Box(Modifier.matchParentSize().backdropSource(source).then(ArtRowsArtElement(art)))
        val scope = this
        CompositionLocalProvider(LocalAMBackdrop provides source, LocalArtRowsArt provides art) { scope.content() }
    }
}

/**
 * Gives this row its own color on an [amArtRows] page: the picture at [url], through the art
 * backdrop's recipe, lies behind the row and scrolls with it, blending into its neighbors' colors.
 * A row outside an [amArtRows] draws nothing extra. For a picture the app already holds as a
 * bitmap, use the `ImageBitmap` form.
 *
 * @param url the row's picture; null leaves its band the dark neutral.
 */
public fun Modifier.amArtRow(url: String?): Modifier = this then ArtRowElement(url?.let { ArtSource.Url(it) })

/**
 * Gives this row its own color on an [amArtRows] page from [image], a bitmap the app already holds
 * (a cover embedded in a music file), the same way the URL form does from a picture it loads. The
 * bitmap is processed once per band and kept, known by the bitmap itself, as [amArtBackdrop]'s
 * `ImageBitmap` form describes.
 *
 * @param image the row's picture; null (no art yet) leaves its band the dark neutral.
 */
public fun Modifier.amArtRow(image: ImageBitmap?): Modifier = this then ArtRowElement(image?.let { ArtSource.Image(it) })

/**
 * Text that sits right on a darkened picture: white, with a tight and a soft black shadow under
 * its glyphs, so its edges hold over the brightest colors.
 *
 * ```kotlin
 * Text("Albums", Modifier.amArtText(), style = MaterialTheme.typography.titleLarge)
 * Column(Modifier.amArtText()) { Text(album.title); Text(album.artist) }
 * ```
 *
 * Put it on a text or on a group of them: everything it covers draws white, glyphs included,
 * with the shadow drawn from the glyphs themselves.
 */
public fun Modifier.amArtText(): Modifier = glyphShadow(Style.Shadow.ArtText, tint = Color.White)

// The art and page backdrops

/**
 * A backdrop's recipe: its blur, its saturation and, for a page backdrop ([page]), its washes. A
 * page backdrop shows the page color while it has no picture, since text on it takes the scheme's
 * colors; an art backdrop shows the dark neutral, since text on it is white.
 */
@Immutable
private data class ArtRecipe(
    val blurRadius: Dp,
    val saturation: Float,
    val lightWash: Float,
    val darkWash: Float,
    val page: Boolean,
)

/** The art backdrop's recipe: the full blur and saturation, no page washes. */
private val ArtBackdropRecipe = ArtRecipe(ArtBlurRadius, ArtSaturation, 0f, 0f, page = false)

/** A page backdrop's recipe: its saturation fades with its blur, so a sharp picture keeps its own color. */
private fun pageRecipe(blurRadius: Dp, lightWash: Float, darkWash: Float): ArtRecipe {
    val share = (blurRadius / ArtBlurRadius).coerceIn(0f, 1f)
    return ArtRecipe(blurRadius, 1f + (ArtSaturation - 1f) * share, lightWash, darkWash, page = true)
}

private fun Modifier.artBackdrop(source: ArtSource?, recipe: ArtRecipe): Modifier =
    this then ArtBackdropElement(source, recipe, InstantSwitch.generation)

private data class ArtBackdropElement(val source: ArtSource?, val recipe: ArtRecipe, val generation: Int) :
    ModifierNodeElement<ArtBackdropNode>() {
    override fun create() = ArtBackdropNode(source, recipe, generation)

    override fun update(node: ArtBackdropNode) = node.update(source, recipe, generation)

    override fun InspectorInfo.inspectableProperties() {
        name = if (recipe.page) "amPageBackdrop" else "amArtBackdrop"
        when (source) {
            is ArtSource.Url -> properties["url"] = source.url
            is ArtSource.Image -> properties["image"] = source.image
            null -> properties["url"] = null
        }
        properties["blurRadius"] = recipe.blurRadius
    }
}

/**
 * Loads its picture for its frame once it knows its size, and draws it behind the content: the
 * fallback (the page color for a page backdrop, the dark neutral for an art backdrop), the picture
 * leaving (while a new one fades in), the picture showing, the washes.
 */
private class ArtBackdropNode(
    private var source: ArtSource?,
    private var recipe: ArtRecipe,
    private var generation: Int,
) : Modifier.Node(), DrawModifierNode, LayoutAwareModifierNode, CompositionLocalConsumerModifierNode {

    private var frame = IntSize.Zero
    private var requested: ArtKey? = null
    private var loading: Job? = null

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
        val key = artKey(source, frame, blurRadius, recipe.saturation, fade = 0f)
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
            // A failed resize keeps the picture showing; a failed new picture shows the fallback.
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
        val dark = currentValueOf(LocalAMDarkTheme) ?: false
        drawRect(
            when {
                !recipe.page -> ArtFallback
                dark -> Style.Page.Dark
                else -> Style.Page.Light
            },
        )
        val opacity = reveal.value
        if (opacity < 1f) leaving?.let { drawArt(it, Offset.Zero, size, 1f) }
        shown?.let { drawArt(it, Offset.Zero, size, opacity) }
        drawRect(Color.Black, alpha = if (dark) ArtWashDark else ArtWashLight)
        val pageWash = if (dark) recipe.darkWash else recipe.lightWash
        if (pageWash > 0f) drawRect(if (dark) Color.Black else Color.White, alpha = pageWash.coerceIn(0f, 1f))
        drawContent()
    }
}

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

// The art rows

/** What an art row looks for among its ancestors: the nearest [amArtRows]. */
private object ArtRowsKey

private data object ArtRowsElement : ModifierNodeElement<ArtRowsNode>() {
    override fun create() = ArtRowsNode()

    override fun update(node: ArtRowsNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "amArtRows"
    }
}

/**
 * The rows' container: the rows register with it, and it draws their bands behind its content,
 * loading each band's picture the first time it's needed. Inside an [AMArtRowsBackdrop] (when that
 * backdrop is the nearest one) it draws nothing itself and hands its bands to the backdrop, telling
 * it to redraw whenever a row or the content itself moves.
 */
private class ArtRowsNode :
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
            val dark = currentValueOf(LocalAMDarkTheme) ?: false
            val bleed = ArtRowBleed.toPx()
            val extent = Size(size.width, size.height + bleed * 2f)
            drawRect(ArtFallback, Offset(0f, -bleed), extent)
            drawBands(size)
            drawRect(Color.Black, Offset(0f, -bleed), extent, alpha = if (dark) ArtWashDark else ArtWashLight)
        }
        drawContent()
    }

    /**
     * Draws each row's band, in this container's frame, for content of [contentSize]: the colors
     * alone, with no fallback or wash under or over them.
     */
    fun DrawScope.drawBands(contentSize: Size) {
        val container = coordinates?.takeIf { it.isAttached } ?: return
        val placed = rows.mapNotNull { row ->
            val bounds = row.coordinates?.takeIf { it.isAttached }?.let { container.localBoundingBoxOf(it, clipBounds = false) }
            bounds?.let { row.source to it }
        }.sortedBy { it.second.top }
        val bands = artRowBands(
            placed.map { ArtSpan(it.second.top, it.second.bottom) },
            contentSize.height,
            ArtRowBlend.toPx(),
            ArtRowBleed.toPx(),
        )
        val blurRadius = ArtBlurRadius.toPx()
        placed.forEachIndexed { index, (source, _) ->
            if (source == null) return@forEachIndexed
            val band = bands[index]
            val frame = IntSize(ceil(contentSize.width).toInt(), ceil(band.bottom - band.top).toInt())
            if (frame.width <= 0 || frame.height <= 0) return@forEachIndexed
            val key = artKey(source, frame, blurRadius, ArtSaturation, band.fade)
            val image = ArtImages.cached(key)
            if (image == null) {
                request(source, key)
            } else {
                drawArt(image, Offset(0f, band.top), Size(contentSize.width, band.bottom - band.top), 1f)
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
 * Draws an [AMArtRowsBackdrop]'s art across the region: the dark neutral, each registered
 * content's bands at the content's place in the region, then the wash, all cut to the region.
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
        val dark = currentValueOf(LocalAMDarkTheme) ?: false
        val region = requireLayoutCoordinates()
        clipRect {
            drawRect(ArtFallback)
            art.contents.forEach { content ->
                val placed = content.coordinates?.takeIf { it.isAttached } ?: return@forEach
                val offset = region.localPositionOf(placed, Offset.Zero)
                translate(offset.x, offset.y) {
                    with(content) { drawBands(placed.size.toSize()) }
                }
            }
            drawRect(Color.Black, alpha = if (dark) ArtWashDark else ArtWashLight)
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

    private var container: ArtRowsNode? = null
    private var bounds: Rect? = null

    fun update(source: ArtSource?) {
        if (source == this.source) return
        this.source = source
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
