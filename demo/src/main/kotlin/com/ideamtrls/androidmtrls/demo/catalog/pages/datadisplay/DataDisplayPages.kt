package com.ideamtrls.androidmtrls.demo

/**
 * The Data display category's entries, in list order, flagship component first. The collection
 * pages (Card stack, Carousel) list last, from [CollectionsPages].
 */
internal object DataDisplayPages {
    val entries: List<ComponentEntry> = listOf(
        AvatarEntry,
        TagEntry,
        BadgeEntry,
        RemoteImageEntry,
        ArtThumbnailEntry,
        ArtCardEntry,
        ArtBackdropEntry,
        ArtRowsEntry,
        TimelineEntry,
    ) + CollectionsPages.entries
}
