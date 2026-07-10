package io.github.jmsdnns.androidmtrls.demo.scenes

import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.scenes.auth.authSceneEntry
import io.github.jmsdnns.androidmtrls.demo.scenes.calendar.calendarSceneEntry
import io.github.jmsdnns.androidmtrls.demo.scenes.gallery.gallerySceneEntry
import io.github.jmsdnns.androidmtrls.demo.scenes.music.nowPlayingSceneEntry
import io.github.jmsdnns.androidmtrls.demo.scenes.social.socialSceneEntry

/**
 * Registry list for the `Screens` section — the five iOS-parity demo scenes.
 *
 * In home **display order** (matches the iOS `.screens` band), the scenes that
 * fill this list are:
 *
 * | id               | title       | subtitle                               |
 * |------------------|-------------|----------------------------------------|
 * | `scene-social`   | Social      | A feed of cards with action rows       |
 * | `scene-nowplaying` | Now Playing | Player and library with a real queue |
 * | `scene-gallery`  | Gallery     | Browse, favorite, and organize photos  |
 * | `scene-calendar` | Calendar    | A month calendar with a day's events   |
 * | `scene-auth`     | Auth        | Sign-in, 2FA, and welcome flow         |
 *
 * The commented slots below hold each scene's place in display order; each is
 * uncommented as its SDD task lands. Prepended FIRST in
 * [io.github.jmsdnns.androidmtrls.demo.catalog.CatalogRegistry.all] so Scenes
 * lead the catalog (iOS section order).
 */
val scenesEntries: List<ComponentEntry> = listOf(
    socialSceneEntry,         // scene-social      — Social
    nowPlayingSceneEntry,     // scene-nowplaying  — Now Playing
    gallerySceneEntry,        // scene-gallery     — Gallery
    calendarSceneEntry,       // scene-calendar    — Calendar
    authSceneEntry,           // scene-auth        — Auth
)
