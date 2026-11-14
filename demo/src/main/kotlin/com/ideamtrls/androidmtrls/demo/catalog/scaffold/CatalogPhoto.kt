package com.ideamtrls.androidmtrls.demo

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * A sample photo for a catalog page: the Wikimedia Commons file [seed] names in [CatalogPhotos],
 * wide enough to cover a [width] × [height] frame (square when [height] is null). Every catalog
 * page asks for its photos here, so the `artwork night` debug extra, passed for dark-mode
 * screenshots, swaps the whole catalog to its second set, night scenes, and a component's light
 * and dark pictures show different artwork. The scenes keep their own art.
 *
 * Commons serves a file at its own shape and the frame crops it, so the width asked for is
 * enough for a 3:2 landscape to cover the frame's height too.
 */
internal fun catalogPhoto(seed: String, width: Int, height: Int? = null): String {
    val pick = CatalogPhotos.getValue(seed)
    val file = if (LaunchOptions.artwork != null) pick.night else pick.day
    return commonsPhoto(file, maxOf(width, (height ?: width) * 3 / 2))
}

/** A catalog seed's two Commons files: one for the default set, one for `artwork night`. */
private class CatalogPick(val day: String, val night: String)

/** Every catalog seed, each a different photo in each set. */
private val CatalogPhotos: Map<String, CatalogPick> = mapOf(
    "orchard" to CatalogPick(
        day = "Schenna (IT), Apfelplantage, Apfelernte -- 2026 -- 4097.jpg",
        night = "Sternenhimmel blühender Kirschbaum P4214798PS.jpg",
    ),
    "cobalt" to CatalogPick(
        day = "2018 01 (Blue) - Chaouen.jpg",
        night = "Basilica Sancti Petri blue hour.jpg",
    ),
    "brooklyn" to CatalogPick(
        day = "Brooklyn Heights Promenade NY1.jpg",
        night = "Manhattan Bridge at Night.JPG",
    ),
    "salt" to CatalogPick(
        day = "Wendover (UT, USA), Bonneville Salt Flats -- 2022 -- 2702.jpg",
        night = "Joshua Tree Milky Way.jpg",
    ),
    "mountain" to CatalogPick(
        day = "Matterhorn, March 2019 (01).jpg",
        night = "008 Milky way aligned with the Matterhorn reflecting in Stellisee Photo by Giles Laurent.jpg",
    ),
    "ada" to CatalogPick(
        day = "Ada_Lovelace_daguerreotype_by_Antoine_Claudet_1843_-_cropped.png",
        night = "Commodore_Grace_M._Hopper,_USN_(covered)_head_and_shoulders_crop.jpg",
    ),
    "spacing-ada" to CatalogPick(
        day = "Ada Lovelace portrait.jpg",
        night = "Margaret_Hamilton_1995.jpg",
    ),
    "selection-tile-1" to CatalogPick(
        day = "Amiens France Field-of-tulips-01.jpg",
        night = "Illuminated facade of a 3-storey restaurant with Japanese signs and red paper lanterns, Chiyoda, Tokyo.jpg",
    ),
    "selection-tile-2" to CatalogPick(
        day = "Dülmen, Weddern, Sonnenblumen -- 2026 -- 2005.jpg",
        night = "Colorful neon street signs in Kabukichō, Shinjuku, Tokyo.jpg",
    ),
    "selection-tile-3" to CatalogPick(
        day = "Snowshil-Lavender.jpg",
        night = "1 singapore national day parade 2011 fireworks.jpg",
    ),
    "selection-picker-0" to CatalogPick(
        day = "Great Sand Dunes NP 1.jpg",
        night = "Aurora in Abisko near Torneträsk.jpg",
    ),
    "selection-picker-1" to CatalogPick(
        day = "Point Reyes Lighthouse in December 2019.jpg",
        night = "042 Full Moon rising behind Chasseral during sunset Photo by Giles Laurent.jpg",
    ),
    "selection-picker-2" to CatalogPick(
        day = "Rice terraces, Bali.jpg",
        night = "Night Market Chiang Mai.jpg",
    ),
    "selection-picker-3" to CatalogPick(
        day = "Sofia Massif and Sofia Glacier, Karachay-Cherkessia, Caucasus Mountains.jpg",
        night = "Kabukicho red gate and colorful neon street signs at night, Shinjuku, Tokyo, Japan.jpg",
    ),
    "selection-picker-4" to CatalogPick(
        day = "Bryce Canyon Hoodoos.jpg",
        night = "Lightning Pritzerbe 01 (MK).jpg",
    ),
    "selection-picker-5" to CatalogPick(
        day = "Alpine barn near Obernberger See 01.jpg",
        night = "San Diego Skyline Night JD081107.jpg",
    ),
    "selection-picker-6" to CatalogPick(
        day = "Reflection Lake, Lassen Volcanic National Park, in June 2022-6049.jpg",
        night = "Campfire and sparks in Anttoora 3.jpg",
    ),
    "selection-picker-7" to CatalogPick(
        day = "Autumn forest near Avenue du Bourg-Royal, Quebec City.jpg",
        night = "Candle light projected through a glass orb.jpg",
    ),
    "selection-picker-8" to CatalogPick(
        day = "Havelock Island, Mangrove tree on the beach, Andaman Islands.jpg",
        night = "Paris Night.jpg",
    ),
    "morning-walk" to CatalogPick(
        day = "Path Mist Daribokgre Nokrek West Garo Oct24 A7CR 03594.jpg",
        night = "Shin Kobe Station across Ikuta River at night Kobe 2026 dllu.jpg",
    ),
    "long-weekend" to CatalogPick(
        day = "Åmotdalshytta tourist cabin under Snøhetta.jpg",
        night = "Bontecou Lake Milky Way panorama.jpg",
    ),
    "field-trip" to CatalogPick(
        day = "Hikers overlooking confluence of Firth River and Joe Creek, Ivvavik National Park, YT.jpg",
        night = "Assy-Turgen Observatory against the backdrop of the center of the Milky Way and the star-forming region around Antares.jpg",
    ),
    "low-tide" to CatalogPick(
        day = "Tidepools in Santa Cruz at low tide.jpg",
        night = "Baltimore Inner Harbor Skyline Night Panorama.jpg",
    ),
    "night-out" to CatalogPick(
        day = "Chateau Frontenac illuminated at night in Quebec City.jpg",
        night = "Taichung 2013 nightmarket 2 amk.jpg",
    ),
)

/**
 * A picture of a chosen subject, from Wikimedia Commons by its file name (the file's page there
 * credits its author and license), at [width] px: Commons serves the nearest of its standard
 * thumbnail widths at or above it, at the file's own shape.
 */
internal fun commonsPhoto(file: String, width: Int = 1000): String =
    "https://commons.wikimedia.org/wiki/Special:FilePath/${Uri.encode(file)}?width=$width"

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

/**
 * Solid white and solid black pictures, for showing art at its extremes: a page over an all-white
 * or an all-black cover. One bitmap each for the process, so the art made from them is kept.
 */
internal object SolidArt {
    val White: ImageBitmap = solid(android.graphics.Color.WHITE)
    val Black: ImageBitmap = solid(android.graphics.Color.BLACK)

    private fun solid(color: Int): ImageBitmap =
        Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }.asImageBitmap()
}
