package io.github.jmsdnns.androidmtrls.demo.scenes.gallery

import androidx.compose.runtime.Immutable

/**
 * Value types + curated sample data for the Gallery scene. Pure data — no
 * composables. Favorites is never stored: the scene derives it from
 * [GalleryPhoto.isFavorite]. Ports iOS `GalleryData.swift` VERBATIM (titles,
 * dates, locations, cameras, file sizes, tags, picsum seed URLs, and the four
 * album memberships).
 */

/**
 * A single library photo. [month] is both the grouping key and the section
 * header (e.g. `"May 2026"`); [thumbUrl] / [fullUrl] are the picsum seed URLs.
 * Immutable value type — favorite/delete edits replace the element in
 * [GalleryState.photos]. Mirrors iOS `GalleryPhoto`.
 */
@Immutable
data class GalleryPhoto(
    val id: Int,
    val title: String,
    val month: String,        // grouping key + section header, e.g. "May 2026"
    val date: String,         // display line, e.g. "May 14, 2026"
    val location: String,
    val camera: String,
    val fileSize: String,
    val tags: List<String>,   // plain keywords — no hashtags
    val isFavorite: Boolean = false,
) {
    /** 240px picsum thumbnail — the grid/cover source. */
    val thumbUrl: String get() = "https://picsum.photos/seed/iosmtrls-$id/240/240"

    /** 600px picsum full image — the detail carousel + substrate source. */
    val fullUrl: String get() = "https://picsum.photos/seed/iosmtrls-$id/600/600"
}

/**
 * A manual album — an ordered set of photo ids. Favorites is NOT here: it is
 * derived from [GalleryPhoto.isFavorite], so it can never be added to,
 * reordered against, or drift out of sync. Mirrors iOS `GalleryAlbum`.
 */
@Immutable
data class GalleryAlbum(
    val name: String,
    val photoIDs: List<Int>,
)

/**
 * Month display order, newest first. Grouping iterates THIS list, not the photo
 * array, so a refresh shuffle reorders photos within a month but can never
 * reorder the months themselves. Mirrors iOS `galleryMonths`.
 */
val galleryMonths: List<String> = listOf("May 2026", "April 2026", "March 2026")

/** The 30 seed photos — copied verbatim from iOS `galleryPhotos`. */
val galleryPhotos: List<GalleryPhoto> = listOf(
    // May 2026
    GalleryPhoto(1, "Morning", "May 2026", "May 26, 2026", "Kyoto, JP", "Fujifilm X-T5", "3.1 MB", listOf("travel", "film")),
    GalleryPhoto(2, "Drift", "May 2026", "May 24, 2026", "Lisbon, PT", "iPhone 17 Pro", "2.4 MB", listOf("street")),
    GalleryPhoto(3, "Stillness", "May 2026", "May 21, 2026", "Halifax, NS", "iPhone 17 Pro", "4.0 MB", listOf("family", "home")),
    GalleryPhoto(4, "Window", "May 2026", "May 18, 2026", "Kyoto, JP", "Fujifilm X-T5", "2.8 MB", listOf("travel", "light")),
    GalleryPhoto(5, "Departure", "May 2026", "May 14, 2026", "Kyoto, JP", "iPhone 17 Pro", "3.6 MB", listOf("travel")),
    GalleryPhoto(6, "Whiteboard", "May 2026", "May 12, 2026", "Brooklyn, NY", "iPhone 17 Pro", "1.9 MB", listOf("work")),
    GalleryPhoto(7, "Salt", "May 2026", "May 9, 2026", "Valparaíso, CL", "Fujifilm X-T5", "5.2 MB", listOf("texture", "film")),
    GalleryPhoto(8, "Field", "May 2026", "May 7, 2026", "Innsbruck, AT", "Fujifilm X-T5", "4.4 MB", listOf("hills", "light")),
    GalleryPhoto(9, "Bridge", "May 2026", "May 5, 2026", "Trieste, IT", "iPhone 17 Pro", "3.3 MB", listOf("travel", "water")),
    GalleryPhoto(10, "Quiet", "May 2026", "May 3, 2026", "Hobart, AU", "Fujifilm X-T5", "2.2 MB", listOf("quiet", "film")),
    GalleryPhoto(11, "Crowd", "May 2026", "May 1, 2026", "Brooklyn, NY", "iPhone 17 Pro", "2.9 MB", listOf("street", "friends")),
    // April 2026
    GalleryPhoto(12, "Switchback", "April 2026", "April 28, 2026", "Innsbruck, AT", "Fujifilm X-T5", "4.8 MB", listOf("hills", "travel")),
    GalleryPhoto(13, "Threshold", "April 2026", "April 25, 2026", "Marfa, TX", "iPhone 17 Pro", "2.1 MB", listOf("light")),
    GalleryPhoto(14, "Harbor", "April 2026", "April 22, 2026", "Stavanger, NO", "Fujifilm X-T5", "3.9 MB", listOf("water", "travel")),
    GalleryPhoto(15, "Sunday", "April 2026", "April 19, 2026", "Halifax, NS", "iPhone 17 Pro", "2.6 MB", listOf("family")),
    GalleryPhoto(16, "Ledger", "April 2026", "April 16, 2026", "Brooklyn, NY", "iPhone 17 Pro", "1.7 MB", listOf("work", "texture")),
    GalleryPhoto(17, "Fog", "April 2026", "April 13, 2026", "Reykjavík, IS", "Fujifilm X-T5", "4.1 MB", listOf("quiet", "light")),
    GalleryPhoto(18, "Porch", "April 2026", "April 10, 2026", "Marfa, TX", "iPhone 17 Pro", "3.0 MB", listOf("family", "home")),
    GalleryPhoto(19, "Signal", "April 2026", "April 7, 2026", "Lisbon, PT", "iPhone 17 Pro", "2.5 MB", listOf("street", "film")),
    GalleryPhoto(20, "Draft", "April 2026", "April 4, 2026", "Brooklyn, NY", "iPhone 17 Pro", "1.8 MB", listOf("work")),
    GalleryPhoto(21, "Shore", "April 2026", "April 2, 2026", "Hobart, AU", "Fujifilm X-T5", "5.0 MB", listOf("water", "quiet", "film")),
    // March 2026
    GalleryPhoto(22, "Thaw", "March 2026", "March 29, 2026", "Reykjavík, IS", "Fujifilm X-T5", "4.6 MB", listOf("travel", "water")),
    GalleryPhoto(23, "Passage", "March 2026", "March 25, 2026", "Kyoto, JP", "Fujifilm X-T5", "3.4 MB", listOf("travel", "street")),
    GalleryPhoto(24, "Ember", "March 2026", "March 21, 2026", "Marfa, TX", "iPhone 17 Pro", "2.7 MB", listOf("light", "quiet")),
    GalleryPhoto(25, "Kitchen", "March 2026", "March 18, 2026", "Halifax, NS", "iPhone 17 Pro", "2.3 MB", listOf("family", "home")),
    GalleryPhoto(26, "Grain", "March 2026", "March 14, 2026", "Valparaíso, CL", "Fujifilm X-T5", "5.5 MB", listOf("texture", "film")),
    GalleryPhoto(27, "Overlook", "March 2026", "March 11, 2026", "Innsbruck, AT", "Fujifilm X-T5", "4.2 MB", listOf("hills", "travel", "light")),
    GalleryPhoto(28, "Corner", "March 2026", "March 7, 2026", "Trieste, IT", "iPhone 17 Pro", "2.0 MB", listOf("street")),
    GalleryPhoto(29, "Study", "March 2026", "March 4, 2026", "Brooklyn, NY", "iPhone 17 Pro", "1.6 MB", listOf("work", "quiet")),
    GalleryPhoto(30, "Low Tide", "March 2026", "March 1, 2026", "Stavanger, NO", "Fujifilm X-T5", "4.9 MB", listOf("water", "film")),
)

/**
 * The four manual albums. Favorites is NOT here — it is derived, so it can never
 * be added to, reordered against, or drift out of sync. Mirrors iOS
 * `galleryAlbums`.
 */
val galleryAlbums: List<GalleryAlbum> = listOf(
    GalleryAlbum("Travel 2026", listOf(1, 4, 5, 9, 12, 14, 22, 23, 27)),
    GalleryAlbum("Family", listOf(3, 15, 18, 25)),
    GalleryAlbum("Inspiration", listOf(7, 8, 13, 17, 21, 24, 26, 30)),
    GalleryAlbum("Work", listOf(6, 16, 20, 29)),
)

/**
 * `"1 photo"` / `"n photos"` — the count label shown on the library header,
 * album cards, and album-picker rows. Mirrors iOS `galleryPhotoCount`.
 */
fun galleryPhotoCount(count: Int): String =
    if (count == 1) "1 photo" else "$count photos"
