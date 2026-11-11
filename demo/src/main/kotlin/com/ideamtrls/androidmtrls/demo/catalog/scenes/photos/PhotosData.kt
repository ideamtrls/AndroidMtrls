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
    val imageId: Int,
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
    val thumbUrl: String get() = "https://picsum.photos/id/$imageId/360/360"

    /** The detail's full picture. */
    val fullUrl: String get() = "https://picsum.photos/id/$imageId/1200/1200"

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
    imageId: Int,
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
    imageId = imageId,
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
    photo(1, 63, 10, 24, 8, 12, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/120 s", "80", "5000 × 2813", "7.8 MB", listOf("Coffee", "Table"), true),
    photo(2, 56, 10, 21, 19, 48, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/30 s", "640", "2880 × 1920", "4.0 MB", listOf("Lights", "Evening"), false),
    photo(3, 83, 10, 17, 17, 31, PhotosPlace.Kyoto, Fujifilm, FujifilmLens, "ƒ/5.6 · 1/125 s", "400", "2560 × 1920", "3.7 MB", listOf("Forest", "Dusk"), false),
    photo(4, 80, 10, 15, 11, 5, PhotosPlace.Kyoto, Fujifilm, FujifilmLens, "ƒ/2 · 1/500 s", "200", "3888 × 2592", "6.0 MB", listOf("Pine Cones", "Wood"), false),
    photo(5, 28, 10, 13, 10, 22, PhotosPlace.Kyoto, Fujifilm, FujifilmLens, "ƒ/8 · 1/250 s", "160", "4928 × 3264", "8.7 MB", listOf("Stream", "Rocks", "Forest"), true),
    photo(6, 19, 10, 12, 14, 40, PhotosPlace.Kyoto, Fujifilm, FujifilmLens, "ƒ/2 · 1/320 s", "125", "2500 × 1667", "3.4 MB", listOf("Moss", "Garden"), false),
    photo(7, 17, 10, 10, 9, 17, PhotosPlace.Kyoto, Fujifilm, FujifilmLens, "ƒ/8 · 1/500 s", "125", "2500 × 1667", "3.4 MB", listOf("Path", "Garden", "Trees"), false),
    photo(8, 81, 10, 9, 15, 53, PhotosPlace.Kyoto, Fujifilm, FujifilmLens, "ƒ/4 · 1/60 s", "800", "5000 × 3250", "8.8 MB", listOf("Trees", "Path"), true),
    photo(9, 43, 10, 4, 21, 6, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/60 s", "400", "1280 × 831", "2.0 MB", listOf("Bridge", "Skyline", "Night"), false),
    photo(10, 57, 10, 2, 13, 26, PhotosPlace.Brooklyn, Fujifilm, FujifilmLens, "ƒ/5.6 · 1/250 s", "200", "2448 × 3264", "5.1 MB", listOf("Street", "Brick", "Cobblestones"), false),
    photo(11, 42, 9, 28, 9, 44, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/60 s", "250", "3456 × 2304", "5.1 MB", listOf("Cafe", "Coffee", "Table"), false),
    photo(12, 32, 9, 26, 16, 2, PhotosPlace.Lisbon, Fujifilm, FujifilmLens, "ƒ/8 · 1/500 s", "125", "4032 × 3024", "7.0 MB", listOf("Bench", "Tiles", "Wall"), true),
    photo(13, 78, 9, 25, 11, 37, PhotosPlace.Lisbon, Fujifilm, FujifilmLens, "ƒ/4 · 1/250 s", "200", "1584 × 2376", "3.2 MB", listOf("Door", "Stone"), false),
    photo(14, 61, 9, 23, 18, 15, PhotosPlace.Lisbon, Fujifilm, FujifilmLens, "ƒ/8 · 1/320 s", "125", "3264 × 2448", "5.1 MB", listOf("Rooftops", "Old Town", "River"), false),
    photo(15, 62, 9, 22, 19, 3, PhotosPlace.Lisbon, Pixel, PixelLens, "ƒ/1.78 · 1/250 s", "50", "2000 × 1333", "2.7 MB", listOf("Hills", "Haze", "Sunset"), false),
    photo(16, 23, 9, 20, 20, 19, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/60 s", "800", "3887 × 4899", "9.0 MB", listOf("Dinner", "Forks"), false),
    photo(17, 37, 9, 18, 12, 48, PhotosPlace.Lisbon, Pixel, PixelLens, "ƒ/1.78 · 1/1600 s", "50", "2000 × 1333", "2.7 MB", listOf("Coast", "Flowers", "Sea"), false),
    photo(18, 44, 9, 16, 15, 10, PhotosPlace.Lisbon, Fujifilm, FujifilmLens, "ƒ/8 · 1/1000 s", "160", "4272 × 2848", "7.0 MB", listOf("Beach", "Haze"), false),
    photo(19, 88, 9, 11, 8, 31, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/500 s", "64", "1280 × 1707", "2.5 MB", listOf("Avenue", "Traffic", "Rooftop"), false),
    photo(20, 76, 9, 5, 17, 22, PhotosPlace.Brooklyn, Fujifilm, FujifilmLens, "ƒ/2.8 · 1/250 s", "200", "4912 × 3264", "8.7 MB", listOf("Bicycle", "Shed", "Wood"), true),
    photo(21, 68, 8, 30, 18, 40, PhotosPlace.Brooklyn, Fujifilm, FujifilmLens, "ƒ/8 · 1/500 s", "125", "4608 × 3072", "7.9 MB", listOf("Pier", "Lamp", "Clouds"), false),
    photo(22, 52, 8, 27, 12, 14, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/2000 s", "50", "1280 × 853", "2.0 MB", listOf("Boardwalk", "Planks"), false),
    photo(23, 79, 8, 22, 13, 56, PhotosPlace.Reykjavik, Fujifilm, FujifilmLens, "ƒ/8 · 1/400 s", "160", "2000 × 3011", "4.2 MB", listOf("Mountain", "Trail", "Ridge"), false),
    photo(24, 15, 8, 21, 11, 9, PhotosPlace.Reykjavik, Fujifilm, FujifilmLens, "ƒ/11 · 1/60 s", "100", "2500 × 1667", "3.4 MB", listOf("Waterfall", "Rocks", "Moss"), true),
    photo(25, 11, 8, 20, 9, 35, PhotosPlace.Reykjavik, Pixel, PixelLens, "ƒ/1.78 · 1/500 s", "50", "2500 × 1667", "3.4 MB", listOf("Valley", "Fog", "River"), false),
    photo(26, 13, 8, 19, 16, 24, PhotosPlace.Reykjavik, Fujifilm, FujifilmLens, "ƒ/8 · 1/320 s", "100", "2500 × 1667", "3.4 MB", listOf("Coast", "Pebbles", "Sea"), false),
    photo(27, 12, 8, 18, 14, 2, PhotosPlace.Reykjavik, Fujifilm, FujifilmLens, "ƒ/11 · 1/250 s", "100", "2500 × 1667", "3.4 MB", listOf("Beach", "Rocks", "Island"), true),
    photo(28, 14, 8, 17, 10, 47, PhotosPlace.Reykjavik, Pixel, PixelLens, "ƒ/1.78 · 1/1250 s", "50", "2500 × 1667", "3.4 MB", listOf("Sea", "Rocks", "Horizon"), false),
    photo(29, 39, 8, 9, 20, 28, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/60 s", "200", "3456 × 2304", "5.1 MB", listOf("Records", "Turntable"), false),
    photo(30, 84, 8, 3, 19, 52, PhotosPlace.Brooklyn, Pixel, PixelLens, "ƒ/1.78 · 1/30 s", "500", "1280 × 848", "2.0 MB", listOf("Bridge", "Dusk"), false),
)

/** The four manual albums. Favorites isn't one: it's derived, so it can never drift out of sync. */
internal val PhotosSampleAlbums: List<PhotosAlbum> = listOf(
    PhotosAlbum("trips", "Trips 2026", listOf(7, 3, 4, 5, 6, 8, 12, 13, 14, 15, 17, 18, 23, 24, 25, 26, 27, 28)),
    PhotosAlbum("brooklyn", "Around Brooklyn", listOf(9, 10, 19, 22, 21, 30, 2, 1, 11, 16, 29, 20)),
    PhotosAlbum("coast", "Coast and Water", listOf(27, 24, 25, 26, 28, 18, 17, 30, 9)),
    PhotosAlbum("corners", "Quiet Corners", listOf(11, 1, 12, 13, 6, 20, 29)),
)
