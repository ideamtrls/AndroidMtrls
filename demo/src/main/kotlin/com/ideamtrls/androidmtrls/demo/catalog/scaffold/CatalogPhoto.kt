package com.ideamtrls.androidmtrls.demo

import android.net.Uri
import androidx.compose.runtime.Immutable

/**
 * A sample photo for a catalog page: picsum's fixed image for [seed], at [width] × [height]
 * (square when [height] is null). Every catalog page asks for its photos here, so the
 * `artwork night` debug extra, passed for dark-mode screenshots, swaps the whole catalog to a
 * second set (each seed with `-night` appended), and a component's light and dark pictures show
 * different artwork. The scenes keep their own art.
 *
 * A photo that fills a wide frame asks for that frame's shape, 800 wide by the matching height,
 * so the whole picture shows instead of a square's middle slice.
 */
internal fun catalogPhoto(seed: String, width: Int, height: Int? = null): String {
    val size = if (height == null) "$width" else "$width/$height"
    val suffix = LaunchOptions.artwork?.let { "-$it" } ?: ""
    return "https://picsum.photos/seed/$seed$suffix/$size"
}

/**
 * A picture of a chosen subject for a catalog page, from Wikimedia Commons by its file name (the
 * file's page there credits its author and license), 1000 px wide: for a page whose art has to
 * show something in particular, which picsum's seeds can't promise.
 */
internal fun commonsPhoto(file: String): String =
    "https://commons.wikimedia.org/wiki/Special:FilePath/${Uri.encode(file)}?width=1000"

/**
 * A wide photo for a catalog page, picked per appearance: snowy mountains and forests in light
 * mode, New York at night in dark mode, all vetted Wikimedia Commons files. A view reads which
 * appearance is showing and asks [url], so the art follows the appearance itself.
 */
@Immutable
internal class CatalogArt private constructor(val day: String, val night: String) {

    fun url(dark: Boolean): String = commonsPhoto(if (dark) night else day)

    companion object {
        val Mountain = CatalogArt(
            day = "Cheops Mountain seen the Sir Donald Trail.jpg",
            night = "New-York-City-night-skyline-September-2014.jpg",
        )
        val Valley = CatalogArt(
            day = "Snowy Landscape - Ftan, Scuol.jpg",
            night = "Manhattan from Weehawken, NJ.jpg",
        )
        val Forest = CatalogArt(
            day = "Evening sunlight through the trees, Epping Forest - geograph.org.uk - 7865603.jpg",
            night = "New York City Skyline from Brooklyn.jpg",
        )
        val Peaks = CatalogArt(
            day = "Vihren & Kutelo Pirin Mountains Bulgaria 2025.jpg",
            night = "Lower Manhattan from Jersey City November 2014 panorama 3.jpg",
        )
        val Winter = CatalogArt(
            day = "Winter in Tatry Mountains - Poland.jpg",
            night = "Manhattan from Weehawken, NJ.jpg",
        )
    }
}
