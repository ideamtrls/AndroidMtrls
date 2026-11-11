package com.ideamtrls.androidmtrls.demo

/**
 * The Showcase scenes' entries, in list order: Social, Music, Photos, Calendar, Shop, Account. Each
 * scene declares its entry in its own folder under `catalog/scenes/`.
 */
internal object ShowcasePages {
    val entries: List<ComponentEntry> = listOf(
        SocialSceneEntry,
        MusicSceneEntry,
        PhotosSceneEntry,
        CalendarSceneEntry,
        ShopSceneEntry,
        AccountSceneEntry,
    )
}
