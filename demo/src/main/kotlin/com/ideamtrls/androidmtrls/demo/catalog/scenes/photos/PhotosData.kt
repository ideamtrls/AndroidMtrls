package com.ideamtrls.androidmtrls.demo

import android.text.format.DateFormat
import androidx.compose.runtime.Immutable
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// The Photos scene's value types and sample library: pure data, no views. Favorites is never
// stored as an album: it's the Library's Favorites filter over `PhotosItem.isFavorite`.

/** The four places the library was shot in: Brooklyn at home, and a trip each to the other three. */
internal enum class PhotosPlace(val title: String, val detail: String) {
    Brooklyn("Brooklyn", "Brooklyn, New York"),
    Kyoto("Kyoto", "Kyoto, Japan"),
    Lisbon("Lisbon", "Lisbon, Portugal"),
    Reykjavik("Reykjavík", "Reykjavík, Iceland"),
}

/**
 * One photo. Photos carry no titles: a photo is named by its place and date. Units inside the
 * strings use non-breaking spaces, and every Info value fits on one line.
 */
@Immutable
internal data class PhotosItem(
    val id: Int,
    /** The picture's Wikimedia Commons file name. */
    val file: String,
    val takenAt: LocalDateTime,
    val place: PhotosPlace,
    val camera: String,
    val lens: String,
    /** "ƒ/2.8 · 1/250 s" */
    val exposure: String,
    val iso: String,
    /** "5000 × 3333" */
    val pixelSize: String,
    /** "6.1 MB" */
    val fileSize: String,
    val keywords: List<String>,
    val isFavorite: Boolean,
) {
    /** The grid's square thumbnail. */
    val thumbUrl: String get() = commonsPhoto(file, 500)

    /** The detail's full picture. */
    val fullUrl: String get() = commonsPhoto(file, 1280)

    /** The Library section this photo falls in. */
    val month: YearMonth get() = YearMonth.from(takenAt)

    /** The detail's caption: "Saturday, October 24 · 8:12 AM", in the phone's own format. */
    val captionDate: String
        get() {
            val locale = Locale.getDefault()
            val day = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"), locale)
            val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            return "${takenAt.format(day)} · ${takenAt.format(time)}"
        }

    /** The Info sheet's date: "October 24, 2026". */
    val fullDate: String
        get() = takenAt.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault()))

    /** What TalkBack calls the photo: its place and date, "Kyoto, October 10, 2026". */
    val accessibilityName: String get() = "${place.title}, $fullDate"
}

/** A month's section title: "October 2026", in the phone's own format. */
internal fun photosMonthTitle(month: YearMonth): String {
    val locale = Locale.getDefault()
    return month.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMyyyy"), locale))
}

/** A manual album: its name and its photos in album order. The first photo is its cover. */
@Immutable
internal data class PhotosAlbum(val id: String, val name: String, val photoIds: List<Int>)

/** "1 photo" / "n photos": the count lines, album cards and album rows. */
internal fun photosCountLabel(count: Int): String = if (count == 1) "1 photo" else "$count photos"

private const val Pixel = "Pixel 10 Pro"
private const val PixelLens = "24 mm ƒ/1.78"
private const val Fujifilm = "Fujifilm X-T5"
private const val FujifilmLens = "XF 23mm F1.4"

private fun photo(
    id: Int,
    file: String,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    place: PhotosPlace,
    camera: String,
    lens: String,
    exposure: String,
    iso: String,
    pixelSize: String,
    fileSize: String,
    keywords: List<String>,
    isFavorite: Boolean,
): PhotosItem = PhotosItem(
    id = id,
    file = file,
    takenAt = LocalDateTime.of(2026, month, day, hour, minute),
    place = place,
    camera = camera,
    lens = lens,
    exposure = exposure,
    iso = iso,
    pixelSize = pixelSize,
    fileSize = fileSize,
    keywords = keywords,
    isFavorite = isFavorite,
)

/** 30 photos, newest first (August to October 2026), 7 of them favorites. */
internal val PhotosSampleLibrary: List<PhotosItem> = listOf(
    photo(1, "Autumn in Prospect Park (14035p).jpg", 10, 24, 8, 12, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/120 s", "80", "7095 × 4110", "29.7 MB", listOf("Park", "Autumn", "Lake"), true),
    photo(2, "Silouette of the Welcome sign near the Brooklyn Bridge.jpg", 10, 21, 19, 48, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/30 s", "640", "7908 × 5239", "11.6 MB", listOf("Sunset", "Sign", "Evening"), false),
    photo(3, "Torii path with lantern at Fushimi Inari Taisha Shrine, Kyoto, Japan.jpg", 10, 17, 17, 31, PhotosPlace.Kyoto, Fujifilm, FujifilmLens, "ƒ/5.6 · 1/125 s", "400", "5765 × 3843", "9.1 MB", listOf("Torii", "Shrine", "Lantern"), false),
    photo(4, "Wooden and bamboo facades of dwellings with sudare in a cobbled street of Gion, perspective effect with vanishing point, Kyoto, Japan.jpg", 10, 15, 11, 5, PhotosPlace.Kyoto, Fujifilm, FujifilmLens, "ƒ/2 · 1/500 s", "200", "5722 × 3815", "10.9 MB", listOf("Street", "Wood", "Bamboo"), false),
    photo(5, "Water reflection of Kinkaku-ji Temple a sunny day, Kyoto, Japan.jpg", 10, 13, 10, 22, PhotosPlace.Kyoto, Fujifilm, FujifilmLens, "ƒ/8 · 1/250 s", "160", "5928 × 3335", "9.7 MB", listOf("Temple", "Pond", "Gold"), true),
    photo(6, "Moss lawn in Kyoto, Japan.jpg", 10, 12, 14, 40, PhotosPlace.Kyoto, Fujifilm, FujifilmLens, "ƒ/2 · 1/320 s", "125", "3088 × 2056", "4.2 MB", listOf("Moss", "Garden"), false),
    photo(7, "Dry-sand Zen garden made of beige stripes a sunny day at Higashiyama Jisho-ji Buddhist temple Ginkaku-ji Kyoto Japan.jpg", 10, 10, 9, 17, PhotosPlace.Kyoto, Fujifilm, FujifilmLens, "ƒ/8 · 1/500 s", "125", "6616 × 4416", "18.3 MB", listOf("Sand", "Garden", "Temple"), false),
    photo(8, "Eikan-do Zenrin-ji, November 2016 -03.jpg", 10, 9, 15, 53, PhotosPlace.Kyoto, Fujifilm, FujifilmLens, "ƒ/4 · 1/60 s", "800", "5816 × 3877", "23.3 MB", listOf("Maples", "Autumn", "Pond"), true),
    photo(9, "Brooklyn Bridge - 03.jpg", 10, 4, 21, 6, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/60 s", "400", "2000 × 3000", "4.9 MB", listOf("Bridge", "Tower", "Cables"), false),
    photo(10, "Bushwick Collective 036.jpg", 10, 2, 13, 26, PhotosPlace.Brooklyn, Fujifilm, FujifilmLens, "ƒ/5.6 · 1/250 s", "200", "3264 × 2448", "2.9 MB", listOf("Street Art", "Mural"), false),
    photo(11, "Bluebells in the Brooklyn Botanic Garden (42915p).jpg", 9, 28, 9, 44, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/60 s", "250", "7532 × 3121", "25.9 MB", listOf("Garden", "Bluebells"), false),
    photo(12, "Azulejos Parque Eduardo VII-1.jpg", 9, 26, 16, 2, PhotosPlace.Lisbon, Fujifilm, FujifilmLens, "ƒ/8 · 1/500 s", "125", "2867 × 2496", "3.5 MB", listOf("Tiles", "Blue"), true),
    photo(13, "Tram overhead lines, Elevador da Glória, Lisbon, Portugal julesvernex2.jpg", 9, 25, 11, 37, PhotosPlace.Lisbon, Fujifilm, FujifilmLens, "ƒ/4 · 1/250 s", "200", "3751 × 2451", "0.8 MB", listOf("Tram Lines", "Sky"), false),
    photo(14, "View from Castelo de S. Jorge - Lisbon - Portugal.jpg", 9, 23, 18, 15, PhotosPlace.Lisbon, Fujifilm, FujifilmLens, "ƒ/8 · 1/320 s", "125", "6112 × 4075", "24.1 MB", listOf("Rooftops", "Old Town", "River"), false),
    photo(15, "Clouds at sunset, Alameda Dom Afonso Henriques, Lisbon, Portugal (approx.. GPS location) julesvernex2.jpg", 9, 22, 19, 3, PhotosPlace.Lisbon, Pixel, PixelLens, "ƒ/1.78 · 1/250 s", "50", "6240 × 4160", "9.8 MB", listOf("Sunset", "Clouds"), false),
    photo(16, "Brooklyn Botanic Garden New York May 2015 panorama 2.jpg", 9, 20, 20, 19, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/60 s", "800", "9600 × 4800", "27.2 MB", listOf("Garden", "Pond", "Spring"), false),
    photo(17, "Jerónimos Monastery in Belém - Lisbon - Portugal - 04.jpg", 9, 18, 12, 48, PhotosPlace.Lisbon, Pixel, PixelLens, "ƒ/1.78 · 1/1600 s", "50", "4082 × 6192", "16.7 MB", listOf("Monastery", "Stone"), false),
    photo(18, "View of Lisbon and Tejo - 01.jpg", 9, 16, 15, 10, PhotosPlace.Lisbon, Fujifilm, FujifilmLens, "ƒ/8 · 1/1000 s", "160", "5806 × 3871", "15.7 MB", listOf("City", "River"), false),
    photo(19, "Brooklyn Botanic Garden - 20200911 - 18 - Japanese Hill-and-Pond Garden.jpg", 9, 11, 8, 31, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/500 s", "64", "4032 × 3024", "5.2 MB", listOf("Japanese Garden", "Pond"), false),
    photo(20, "Mute swan cygnet pontoon in Prospect Park (80364).jpg", 9, 5, 17, 22, PhotosPlace.Brooklyn, Fujifilm, FujifilmLens, "ƒ/2.8 · 1/250 s", "200", "4605 × 3014", "16.0 MB", listOf("Swan", "Pond"), true),
    photo(21, "Prospect Park Lake (14063p).jpg", 8, 30, 18, 40, PhotosPlace.Brooklyn, Fujifilm, FujifilmLens, "ƒ/8 · 1/500 s", "125", "5691 × 3920", "21.2 MB", listOf("Lake", "Willow", "Park"), false),
    photo(22, "Thunderbolt Coney Island 2.jpg", 8, 27, 12, 14, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/2000 s", "50", "4566 × 2962", "7.4 MB", listOf("Roller Coaster", "Coney Island"), false),
    photo(23, "Jökulgil from Skalli 2012 panoramic.jpg", 8, 22, 13, 56, PhotosPlace.Reykjavik, Fujifilm, FujifilmLens, "ƒ/8 · 1/400 s", "160", "8192 × 2324", "8.6 MB", listOf("Canyon", "Rhyolite", "Mountains"), false),
    photo(24, "Skógafoss Waterfall 2019.jpeg", 8, 21, 11, 9, PhotosPlace.Reykjavik, Fujifilm, FujifilmLens, "ƒ/11 · 1/60 s", "100", "4320 × 2100", "7.6 MB", listOf("Waterfall", "Rainbow", "Cliffs"), true),
    photo(25, "Lupinus nootkatensis - Iceland 20070706a.jpg", 8, 20, 9, 35, PhotosPlace.Reykjavik, Pixel, PixelLens, "ƒ/1.78 · 1/500 s", "50", "2048 × 1536", "0.8 MB", listOf("Lupines", "Wildflowers"), false),
    photo(26, "Remote view of Dyrhólaey from road number 215 in Iceland, 20240720 0824 2821.jpg", 8, 19, 16, 24, PhotosPlace.Reykjavik, Fujifilm, FujifilmLens, "ƒ/8 · 1/320 s", "100", "5220 × 3485", "16.3 MB", listOf("Coast", "Headland", "Sea"), false),
    photo(27, "Eldfell, Heimaey, Islas Vestman, Suðurland, Islandia, 2014-08-17, DD 069.jpg", 8, 18, 14, 2, PhotosPlace.Reykjavik, Fujifilm, FujifilmLens, "ƒ/11 · 1/250 s", "100", "5304 × 2941", "6.1 MB", listOf("Volcano", "Island", "Sea"), true),
    photo(28, "Fratercula arctica, Borgarfjörður eystri, Iceland, 20240717 1152 1838.jpg", 8, 17, 10, 47, PhotosPlace.Reykjavik, Pixel, PixelLens, "ƒ/1.78 · 1/1250 s", "50", "3080 × 3560", "10.6 MB", listOf("Puffin", "Bird"), false),
    photo(29, "Pond sliders in Prospect Park (03861).jpg", 8, 9, 20, 28, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/60 s", "200", "3510 × 2701", "5.8 MB", listOf("Turtles", "Pond"), false),
    photo(30, "Wonder Wheel Coney Island 013.jpg", 8, 3, 19, 52, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/30 s", "500", "3264 × 2448", "2.1 MB", listOf("Ferris Wheel", "Coney Island"), false),
)

/** The four manual albums. Favorites isn't one: it's derived, so it can never drift out of sync. */
internal val PhotosSampleAlbums: List<PhotosAlbum> = listOf(
    PhotosAlbum("trips", "Trips 2026", listOf(7, 3, 4, 5, 6, 8, 12, 13, 14, 15, 17, 18, 23, 24, 25, 26, 27, 28)),
    PhotosAlbum("brooklyn", "Around Brooklyn", listOf(9, 10, 19, 22, 21, 30, 2, 1, 11, 16, 29, 20)),
    PhotosAlbum("coast", "Coast and Water", listOf(27, 24, 25, 26, 28, 18, 17, 30, 9)),
    PhotosAlbum("corners", "Quiet Corners", listOf(11, 1, 12, 13, 6, 20, 29)),
)
