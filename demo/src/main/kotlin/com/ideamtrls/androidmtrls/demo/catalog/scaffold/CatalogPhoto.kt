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
 * and dark pictures show different artwork. The selection pages' photo grids are the exception:
 * like a photo library, they show the same photos in either set. The scenes keep their own art.
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
private class CatalogPick(val day: String, val night: String) {
    /** One file for both sets: a photo grid's photo, the same in either set. */
    constructor(file: String) : this(file, file)
}

/**
 * Every catalog seed, no file repeated within a set. A named person's portrait ("ada",
 * "spacing-ada") shows that person in both sets, another portrait of them at night. The selection
 * grids' seeds show photos from the Photos scene's library, the same file in both sets.
 */
private val CatalogPhotos: Map<String, CatalogPick> = mapOf(
    "orchard" to CatalogPick(
        day = "Schenna (IT), Apfelplantage -- 2026 -- 4066.jpg",
        night = "Hydrangea macrophylla in front of Seongsan Ilchulbong volcano at blue hour in Jeju Island South Korea.jpg",
    ),
    "cobalt" to CatalogPick(
        day = "Mountain lake, Mylgval mountain cirque, Western Caucasus.jpg",
        night = "Basilica Sancti Petri blue hour.jpg",
    ),
    "brooklyn" to CatalogPick(
        day = "Autumn in Prospect Park (14035p).jpg",
        night = "Brooklyn Bridge - 03.jpg",
    ),
    "coast" to CatalogPick(
        day = "Tidepools in Santa Cruz at low tide.jpg",
        night = "Latarnia morska w Kolobrzegu.jpg",
    ),
    "mountain" to CatalogPick(
        day = "Val de Funes cun la Odles d'auton Südtirol.jpg",
        night = "036 Milky Way during Perseids seen from Oeschinensee with water reflections Photo by Giles Laurent.jpg",
    ),
    "ada" to CatalogPick(
        day = "Ada_Lovelace_daguerreotype_by_Antoine_Claudet_1843_-_cropped.png",
        night = "Carpenter portrait of Ada Lovelace - detail.png",
    ),
    "spacing-ada" to CatalogPick(
        day = "Ada Lovelace portrait.jpg",
        night = "Ada Lovelace portrait circa 1840.jpg",
    ),
    "selection-tile-1" to CatalogPick("Torii path with lantern at Fushimi Inari Taisha Shrine, Kyoto, Japan.jpg"),
    "selection-tile-2" to CatalogPick("Azulejos Parque Eduardo VII-1.jpg"),
    "selection-tile-3" to CatalogPick("Lupinus nootkatensis - Iceland 20070706a.jpg"),
    "selection-picker-0" to CatalogPick("Eikan-do Zenrin-ji, November 2016 -03.jpg"),
    "selection-picker-1" to CatalogPick("Skógafoss Waterfall 2019.jpeg"),
    "selection-picker-2" to CatalogPick("Bushwick Collective 036.jpg"),
    "selection-picker-3" to CatalogPick("Eldfell, Heimaey, Islas Vestman, Suðurland, Islandia, 2014-08-17, DD 069.jpg"),
    "selection-picker-4" to CatalogPick("Clouds at sunset, Alameda Dom Afonso Henriques, Lisbon, Portugal (approx.. GPS location) julesvernex2.jpg"),
    "selection-picker-5" to CatalogPick("Fratercula arctica, Borgarfjörður eystri, Iceland, 20240717 1152 1838.jpg"),
    "selection-picker-6" to CatalogPick("Water reflection of Kinkaku-ji Temple a sunny day, Kyoto, Japan.jpg"),
    "selection-picker-7" to CatalogPick("View from Castelo de S. Jorge - Lisbon - Portugal.jpg"),
    "selection-picker-8" to CatalogPick("Wonder Wheel Coney Island 013.jpg"),
    "morning-walk" to CatalogPick(
        day = "Tübingen - Neckarinsel - Blick entlang Neckar im Herbst.jpg",
        night = "Münster, Prinzipalmarkt, Arkaden -- 2021 -- 8931-3.jpg",
    ),
    "long-weekend" to CatalogPick(
        day = "Canoes on Seyhan Dam, Adana 05.jpg",
        night = "Stuttgart - West - Johanneskirche im Feuersee zur blauen Stunde.jpg",
    ),
    "field-trip" to CatalogPick(
        day = "Hortus Haren (Groningen) 13-12-2025. (actm.) 02.jpg",
        night = "Kimono Forest at night, Arashiyama Station, Arashiyama, Kyoto, Japan.jpg",
    ),
    "low-tide" to CatalogPick(
        day = "Chapel Rock July 2026 04.jpg",
        night = "Illuminated Woljeonggyo Bridge at blue hour in Gyeongju South Korea.jpg",
    ),
    "night-out" to CatalogPick(
        day = "Illuminated facade of a 3-storey restaurant with Japanese signs and red paper lanterns, Chiyoda, Tokyo.jpg",
        night = "Buildings with colorful neon street signs at blue hour, Shinjuku, Tokyo.jpg",
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
 * A wide photo for a catalog page, picked per appearance: lakes, valleys, peaks, a forest and a
 * garden in daylight color in light mode, deep-blue dusk and blue-hour scenes in dark mode, all
 * vetted Wikimedia Commons files. A view reads which appearance is showing and asks [url], so the
 * art follows the appearance itself.
 */
@Immutable
internal class CatalogArt private constructor(val day: String, val night: String) {

    fun url(dark: Boolean): String = commonsPhoto(if (dark) night else day)

    companion object {
        val Mountain = CatalogArt(
            day = "Lago di Braies South Tyrol 9.jpg",
            night = "Mount Everest Peak Rising High Above Dusk Clouds.jpg",
        )
        val Valley = CatalogArt(
            day = "Bergtocht van parkeerplaats bij centrale Malga Mare naar Lago Lungo. Uitzicht over Val Venezia vanaf Posto di Avvistamento Pian Venezia 04.jpg",
            night = "Neckarsulm - Scheuerberg - Blick vom SW-Hang kurz nach Sonnenuntergang.jpg",
        )
        val Forest = CatalogArt(
            day = "Evening sunlight through the trees, Epping Forest - geograph.org.uk - 7865603.jpg",
            night = "Loblolly Pine during blue hour LR, Virginia Beach VA.jpg",
        )
        val Peaks = CatalogArt(
            day = "Mastle cun Odles Gherdëina.jpg",
            night = "Lower Manhattan from Jersey City November 2014 panorama 3.jpg",
        )
        val Garden = CatalogArt(
            day = "2020 Japanese Garden in Moscow - Autumn Colors 02.jpg",
            night = "Water reflection of Donggung Palace in Wolji Pond at blue hour in Gyeongju South Korea.jpg",
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
