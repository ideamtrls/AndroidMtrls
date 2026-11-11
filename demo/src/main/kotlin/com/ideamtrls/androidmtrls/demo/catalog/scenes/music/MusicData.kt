package com.ideamtrls.androidmtrls.demo

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp
import java.text.Collator
import java.util.UUID
import kotlin.math.roundToInt

// The Music scene's data: the value types and the owner's catalog from jmsdnns.com/music,
// hardcoded — artists, their albums, and each album's songs with the site's own MP3 URLs, which the
// model streams. An artist's photo and an album's project photo are images on the site; an album's
// own cover is the art embedded in its MP3s, read over the network (MusicCovers.kt). No views and
// no playback live here.

/** One song: its album's id, a hyphen, then the slug of its title, so a song on two records keeps two ids. */
@Immutable
internal data class MusicTrack(
    val id: String,
    /** As the site's player lists it. */
    val title: String,
    val artist: String,
    val albumId: String,
    /** Its place in the album, from 1. */
    val trackNumber: Int,
    /** The MP3's length, in whole seconds. */
    val durationSeconds: Int,
    /** The site's MP3, streamed. */
    val audioUrl: String,
    /** The album's project photo. */
    val coverUrl: String,
)

/** One record: its id is the slug of its title. */
@Immutable
internal data class MusicAlbum(
    val id: String,
    val title: String,
    /** The slug of [artist]. */
    val artistId: String,
    val artist: String,
    /** In track order. */
    val trackIds: List<String>,
    /** The project photo, shown wherever the embedded cover shows until it's read, and when it can't be. */
    val coverUrl: String,
    /** Its year; some are invented to set the artists' order (see [MusicSiteAlbums]). */
    val year: Int,
)

/** One project: its id is the slug of its name. */
@Immutable
internal data class MusicArtist(
    val id: String,
    val name: String,
    /** Their photo from the site, else their first album's project photo. */
    val imageUrl: String,
    /** The image's width over its height, so it always shows whole; 1 for an album's photo. */
    val imageAspectRatio: Float,
    /** In album title order. */
    val albumIds: List<String>,
)

/**
 * The library: artists by their newest album's year, newest first (a tie goes A–Z); albums A–Z;
 * tracks in album then track order.
 */
@Immutable
internal class MusicCatalog private constructor(
    val artists: List<MusicArtist>,
    val albums: List<MusicAlbum>,
    val tracks: List<MusicTrack>,
) {
    private val artistsById = artists.associateBy { it.id }
    private val albumsById = albums.associateBy { it.id }
    private val tracksById = tracks.associateBy { it.id }

    fun artist(id: String): MusicArtist? = artistsById[id]

    fun album(id: String): MusicAlbum? = albumsById[id]

    fun track(id: String): MusicTrack? = tracksById[id]

    fun albums(artist: MusicArtist): List<MusicAlbum> = artist.albumIds.mapNotNull(::album)

    fun tracks(album: MusicAlbum): List<MusicTrack> = album.trackIds.mapNotNull(::track)

    fun tracks(artist: MusicArtist): List<MusicTrack> = albums(artist).flatMap { tracks(it) }

    companion object {
        /**
         * Assembles a catalog from albums and their tracks, given in any order: albums A–Z,
         * grouped into artists by the album's artist (each artist's albums stay in title order),
         * the artists by their newest album's year, newest first (a tie goes A–Z), and the tracks
         * in album then track order. [artistImage] gives an artist's image and its aspect ratio by
         * the artist's id; an artist it has none for takes their first album's photo, square.
         */
        fun build(
            albums: List<MusicAlbum>,
            tracks: List<MusicTrack>,
            artistImage: (artistId: String) -> Pair<String, Float>?,
        ): MusicCatalog {
            val collator = Collator.getInstance()
            val sortedAlbums = albums.sortedWith { a, b -> collator.compare(a.title, b.title) }
            val byArtist = sortedAlbums.groupBy { it.artistId }
            val newest = byArtist.mapValues { (_, list) -> list.maxOf { it.year } }
            val artists = byArtist
                .map { (id, list) ->
                    val image = artistImage(id)
                    MusicArtist(
                        id = id,
                        name = list.first().artist,
                        imageUrl = image?.first ?: list.first().coverUrl,
                        imageAspectRatio = image?.second ?: 1f,
                        albumIds = list.map { it.id },
                    )
                }
                .sortedWith { a, b ->
                    val yearA = newest[a.id] ?: 0
                    val yearB = newest[b.id] ?: 0
                    if (yearA != yearB) yearB.compareTo(yearA) else collator.compare(a.name, b.name)
                }
            val byId = tracks.associateBy { it.id }
            return MusicCatalog(
                artists = artists,
                albums = sortedAlbums,
                tracks = sortedAlbums.flatMap { album -> album.trackIds.mapNotNull { byId[it] } },
            )
        }

        /**
         * The owner's music from jmsdnns.com/music: every project the page lists, American Food in
         * the present and six bands in the past, each album with its songs as the site's players
         * list them. Each artist's photo is one from their project page, with its aspect ratio, and
         * each album's [MusicAlbum.coverUrl] is the project page's header image, which shows until
         * [musicEmbeddedCover] reads the album's own cover: the site keeps its cover art inside the
         * MP3s' tags, with no image file of its own.
         */
        fun site(): MusicCatalog {
            val built = MusicSiteAlbums.map { it.build() }
            return build(built.map { it.first }, built.flatMap { it.second }) { artistId ->
                MusicSiteArtistPhotos[artistId]?.let { (path, ratio) -> musicSiteUrl(path) to ratio }
            }
        }
    }
}

/** Each artist's photo on the site, by artist id, with its width over its height (from its pixel size). */
private val MusicSiteArtistPhotos: Map<String, Pair<String, Float>> = mapOf(
    "american-food" to ("images/music/projects/amfoo.jpg" to 1000f / 500f),
    "adventure-capitalists" to ("music/projects/a19s/live-pianos.jpg" to 1000f / 500f),
    "sea-graves" to ("images/music/projects/seagraves.jpg" to 1000f / 500f),
    "medio" to ("music/projects/medio/band-dumbo.jpg" to 1000f / 500f),
    "shipyards" to ("images/music/projects/shipyards.jpg" to 1000f / 500f),
    "first-aid-kit" to ("images/music/projects/first-aid-kit.jpg" to 2000f / 1000f),
    "counterfit" to ("images/music/projects/counterfit.jpg" to 1000f / 451f),
)

/**
 * The albums in the site's order: American Food's three EPs (the demos in the journal's "Finished
 * Tracking 3 EPs"), then the past projects' records as each project page lists them. Lengths are
 * whole seconds.
 *
 * The years set the artists' order — each artist's newest album, newest first: American Food,
 * Adventure Capitalists, Sea Graves, Medio, Shipyards, First Aid Kit, Counterfit. Adventure
 * Capitalists', Medio's and First Aid Kit's are the site's, and American Food's 2024 is the journal
 * entry's date. Three are invented for the order: Sea Graves' Congratulations 2016 (the site says
 * 2013, which would put it after Medio), Shipyards' A Dream Is A Town 2008, and Counterfit's On The
 * Downside 2000 (the site dates neither). The years appear nowhere on screen.
 */
private val MusicSiteAlbums: List<MusicSiteAlbum> = listOf(
    MusicSiteAlbum(
        "Silent Artillery Of Time", "American Food", 2024, "images/music/projects/amfoo.jpg",
        MusicSong("Cant Sleep Crew", 375, "audio/American%20Food/Silent%20Artillery%20of%20Time/01%20Cant%20Sleep%20Crew.mp3"),
        MusicSong("Sigh Lens", 332, "audio/American%20Food/Silent%20Artillery%20of%20Time/02%20Sigh%20Lens.mp3"),
        MusicSong("Fade To Wack", 219, "audio/American%20Food/Silent%20Artillery%20of%20Time/03%20Fade%20To%20Wack.mp3"),
    ),
    MusicSiteAlbum(
        "Internet On The TV", "American Food", 2024, "images/music/projects/amfoo.jpg",
        MusicSong("Wanna Be A Cat", 129, "audio/American%20Food/Internet%20On%20The%20TV/01%20Wanna%20Be%20A%20Cat.mp3"),
        MusicSong("My Take On Take On Me", 232, "audio/American%20Food/Internet%20On%20The%20TV/02%20My%20Take%20On%20Take%20On%20Me.mp3"),
        MusicSong("Cane Spiders (misspoke)", 525, "audio/American%20Food/Internet%20On%20The%20TV/03%20Cane%20Spiders%20%28misspoke%29.mp3"),
    ),
    MusicSiteAlbum(
        "Street Fight", "American Food", 2024, "images/music/projects/amfoo.jpg",
        MusicSong("Cognitive Bosses", 200, "audio/American%20Food/Street%20Fight/01%20Cognitive%20Bosses.mp3"),
        MusicSong("You See Revolution", 285, "audio/American%20Food/Street%20Fight/02%20You%20See%20Revolution.mp3"),
        MusicSong("Never Forget pt Deux", 184, "audio/American%20Food/Street%20Fight/03%20Never%20Forget%20pt%20Deux.mp3"),
    ),
    MusicSiteAlbum(
        "Unicore", "Adventure Capitalists", 2018, "images/music/projects/adcap.jpg",
        MusicSong("Founder Not Found", 141, "audio/Adventure%20Capitalists/Unicore/01%20Founder%20Not%20Found.mp3"),
        MusicSong("Startup Douchebag", 141, "audio/Adventure%20Capitalists/Unicore/02%20Startup%20Douchebag.mp3"),
        MusicSong("Alone On Demo Day", 291, "audio/Adventure%20Capitalists/Unicore/03%20Alone%20On%20Demo%20Day.mp3"),
        MusicSong("You Can Take It Away", 245, "audio/Adventure%20Capitalists/Unicore/04%20You%20Can%20Take%20It%20Away.mp3"),
        MusicSong("Moving Mt Fuji", 290, "audio/Adventure%20Capitalists/Unicore/05%20Moving%20Mt%20Fuji.mp3"),
        MusicSong("Raise Another Round", 170, "audio/Adventure%20Capitalists/Unicore/06%20Raise%20Another%20Round.mp3"),
    ),
    MusicSiteAlbum(
        "MVP", "Adventure Capitalists", 2015, "images/music/projects/adcap.jpg",
        MusicSong("I'm Not Sick", 215, "audio/Adventure%20Capitalists/MVP/01%20I%27m%20Not%20Sick.mp3"),
        MusicSong("WFH", 175, "audio/Adventure%20Capitalists/MVP/02%20WFH.mp3"),
        MusicSong("Prosecuted", 167, "audio/Adventure%20Capitalists/MVP/03%20Prosecuted.mp3"),
        MusicSong("No Spoilers Allowed", 136, "audio/Adventure%20Capitalists/MVP/04%20No%20Spoilers%20Allowed.mp3"),
        MusicSong("Someone on the Internet (is wrong)", 166, "audio/Adventure%20Capitalists/MVP/05%20Someone%20on%20the%20Internet%20%28is%20wrong%29.mp3"),
        MusicSong("Heartbleed", 178, "audio/Adventure%20Capitalists/MVP/06%20Heartbleed.mp3"),
    ),
    MusicSiteAlbum(
        "No Spoilers Allowed", "Adventure Capitalists", 2013, "images/music/projects/adcap.jpg",
        MusicSong("No Spoilers Allowed", 136, "audio/Adventure%20Capitalists/No%20Spoilers%20Allowed/No%20Spoilers%20Allowed.mp3"),
    ),
    MusicSiteAlbum(
        "Congratulations", "Sea Graves", 2016, "images/music/projects/seagraves.jpg",
        MusicSong("Movement", 261, "audio/Sea%20Graves/Congratulations/01%20Movement.mp3"),
        MusicSong("Bad Decisions", 284, "audio/Sea%20Graves/Congratulations/02%20Bad%20Decisions.mp3"),
        MusicSong("Saw Yr Ghost", 228, "audio/Sea%20Graves/Congratulations/03%20Saw%20Yr%20Ghost.mp3"),
        MusicSong("Winter Hearts", 252, "audio/Sea%20Graves/Congratulations/04%20Winter%20Hearts.mp3"),
        MusicSong("Small Step", 216, "audio/Sea%20Graves/Congratulations/05%20Small%20Step.mp3"),
        MusicSong("Don't Try", 187, "audio/Sea%20Graves/Congratulations/06%20Don%27t%20Try.mp3"),
    ),
    MusicSiteAlbum(
        "Live @ Pianos 07.11.15", "Medio", 2015, "images/music/projects/medio.jpg",
        MusicSong("SCLD", 260, "audio/Medio/Live%2007.11.15/02%20SCLD.mp3"),
        MusicSong("Shot Down", 242, "audio/Medio/Live%2007.11.15/03%20Shot%20Down.mp3"),
        MusicSong("Just Watch Me", 138, "audio/Medio/Live%2007.11.15/04%20Just%20Watch%20Me.mp3"),
        MusicSong("Werewulfgang", 265, "audio/Medio/Live%2007.11.15/05%20Werewulfgang.mp3"),
        MusicSong("Shout", 216, "audio/Medio/Live%2007.11.15/06%20Shout.mp3"),
        MusicSong("PSA", 199, "audio/Medio/Live%2007.11.15/07%20PSA.mp3"),
        MusicSong("Roku", 205, "audio/Medio/Live%2007.11.15/08%20Roku.mp3"),
    ),
    MusicSiteAlbum(
        "A Dream Is A Town", "Shipyards", 2008, "images/music/projects/shipyards.jpg",
        MusicSong("Black And White", 249, "audio/Shipyards/A%20Dream%20Is%20A%20Town/01%20Black%20And%20White.mp3"),
        MusicSong("Eyes In Front", 207, "audio/Shipyards/A%20Dream%20Is%20A%20Town/02%20Eyes%20In%20Front.mp3"),
        MusicSong("Headfirst Dive", 207, "audio/Shipyards/A%20Dream%20Is%20A%20Town/03%20Headfirst%20Dive.mp3"),
        MusicSong("Nighty Night", 251, "audio/Shipyards/A%20Dream%20Is%20A%20Town/04%20Nighty%20Night.mp3"),
        MusicSong("Midnight Waltz", 410, "audio/Shipyards/A%20Dream%20Is%20A%20Town/05%20Midnight%20Waltz.mp3"),
    ),
    MusicSiteAlbum(
        "One Offs", "First Aid Kit", 2005, "images/music/projects/first-aid-kit.jpg",
        MusicSong("Ships", 320, "audio/First%20Aid%20Kit/Ships%20and%20One%20Offs/01%20Ships.mp3"),
        MusicSong("One Offs", 259, "audio/First%20Aid%20Kit/Ships%20and%20One%20Offs/02%20One%20Offs.mp3"),
    ),
    MusicSiteAlbum(
        "Forgot ID", "First Aid Kit", 2003, "images/music/projects/first-aid-kit.jpg",
        MusicSong("Living or Lively", 307, "audio/First%20Aid%20Kit/Forgot%20ID/01%20Living%20or%20Lively.mp3"),
        MusicSong("Sidewalk Antics", 249, "audio/First%20Aid%20Kit/Forgot%20ID/02%20Sidewalk%20Antics.mp3"),
        MusicSong("Return Retire", 275, "audio/First%20Aid%20Kit/Forgot%20ID/03%20Return%20Retire.mp3"),
    ),
    MusicSiteAlbum(
        "Frights And Shivers", "First Aid Kit", 2002, "images/music/projects/first-aid-kit.jpg",
        MusicSong("The Unpaid", 220, "audio/First%20Aid%20Kit/Frights%20+%20Shivers/01%20The%20Unpaid.mp3"),
        MusicSong("In Thread we Trust", 200, "audio/First%20Aid%20Kit/Frights%20+%20Shivers/02%20In%20Thread%20we%20Trust.mp3"),
        MusicSong("Contingency Plan", 222, "audio/First%20Aid%20Kit/Frights%20+%20Shivers/03%20Contingency%20Plan.mp3"),
        MusicSong("Cinematic Fix", 205, "audio/First%20Aid%20Kit/Frights%20+%20Shivers/04%20Cinematic%20Fix.mp3"),
        MusicSong("Well Aware", 298, "audio/First%20Aid%20Kit/Frights%20+%20Shivers/05%20Well%20Aware.mp3"),
        MusicSong("Untitled", 121, "audio/First%20Aid%20Kit/Frights%20+%20Shivers/06%20Untitled.mp3"),
        MusicSong("Planting Inches", 241, "audio/First%20Aid%20Kit/Frights%20+%20Shivers/07%20Planting%20Inches.mp3"),
        MusicSong("Here in the Electric Socket", 182, "audio/First%20Aid%20Kit/Frights%20+%20Shivers/08%20Here%20in%20the%20Electric%20Socket.mp3"),
        MusicSong("Frights and Shivers", 223, "audio/First%20Aid%20Kit/Frights%20+%20Shivers/09%20Frights%20and%20Shivers.mp3"),
        MusicSong("Wet Postered Walls", 333, "audio/First%20Aid%20Kit/Frights%20+%20Shivers/10%20Wet%20Postered%20Walls.mp3"),
        MusicSong("Driver Passenger", 343, "audio/First%20Aid%20Kit/Frights%20+%20Shivers/11%20Driver%20Passenger.mp3"),
    ),
    MusicSiteAlbum(
        "On The Downside", "Counterfit", 2000, "images/music/projects/counterfit.jpg",
        MusicSong("Regarding Regret", 204, "audio/Counterfit/On%20The%20Downside/01%20Regarding%20Regret.mp3"),
        MusicSong("Further On", 345, "audio/Counterfit/On%20The%20Downside/02%20Further%20On.mp3"),
        MusicSong("In Between The Black And White...", 196, "audio/Counterfit/On%20The%20Downside/03%20In%20Between%20The%20Black%20And%20White....mp3"),
        MusicSong("Shatter Proof", 235, "audio/Counterfit/On%20The%20Downside/04%20Shatter%20Proof.mp3"),
        MusicSong("Completely Incomplete", 235, "audio/Counterfit/On%20The%20Downside/05%20Completely%20Incomplete.mp3"),
    ),
)

/** One song as the site's player lists it: its title, its length in whole seconds, its MP3's path. */
private class MusicSong(val title: String, val seconds: Int, val audio: String)

/**
 * One album as the site lists it: its title, its artist, its year, its cover's path on the site,
 * and its songs in track order.
 */
private class MusicSiteAlbum(
    val title: String,
    val artist: String,
    val year: Int,
    val cover: String,
    vararg val songs: MusicSong,
) {
    /** The album and its tracks. A track's id leads with the album's. */
    fun build(): Pair<MusicAlbum, List<MusicTrack>> {
        val albumId = musicSlug(title)
        val coverUrl = musicSiteUrl(cover)
        val tracks = songs.mapIndexed { index, song ->
            MusicTrack(
                id = "$albumId-${musicSlug(song.title)}",
                title = song.title,
                artist = artist,
                albumId = albumId,
                trackNumber = index + 1,
                durationSeconds = song.seconds,
                audioUrl = musicSiteUrl(song.audio),
                coverUrl = coverUrl,
            )
        }
        val album = MusicAlbum(
            id = albumId,
            title = title,
            artistId = musicSlug(artist),
            artist = artist,
            trackIds = tracks.map { it.id },
            coverUrl = coverUrl,
            year = year,
        )
        return album to tracks
    }
}

/** A file on the owner's site by its path, percent-encoded the way the site's own pages link it. */
private fun musicSiteUrl(path: String): String = "https://jmsdnns.com/$path"

// Ids and labels

/** Lowercased slug of ASCII letters and digits with single hyphens: "Some Track (live)" → "some-track-live". */
internal fun musicSlug(text: String): String {
    val slug = StringBuilder()
    var pendingHyphen = false
    for (char in text.lowercase()) {
        if (char in 'a'..'z' || char in '0'..'9') {
            if (pendingHyphen && slug.isNotEmpty()) slug.append('-')
            pendingHyphen = false
            slug.append(char)
        } else {
            pendingHyphen = true
        }
    }
    return slug.toString()
}

/** A duration as "m:ss", such as "3:41". */
internal fun musicTimeLabel(seconds: Double): String {
    val total = seconds.roundToInt().coerceAtLeast(0)
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

/** A count and its noun, plural past one: "1 song", "9 songs", "3 albums". */
internal fun musicCount(n: Int, noun: String): String = "$n $noun${if (n == 1) "" else "s"}"

/** A running time in whole minutes, such as "11 min". */
internal fun musicLengthLabel(seconds: Int): String = "${(seconds / 60.0).roundToInt()} min"

/** "3 songs · 15 min": a list's song count and running time. */
internal fun musicSongsAndLength(tracks: List<MusicTrack>): String =
    "${musicCount(tracks.size, "song")} · ${musicLengthLabel(tracks.sumOf { it.durationSeconds })}"

/** "3 albums · 9 songs": an artist's album and song counts. */
internal fun musicArtistSummary(artist: MusicArtist, catalog: MusicCatalog): String =
    "${musicCount(artist.albumIds.size, "album")} · ${musicCount(catalog.tracks(artist).size, "song")}"

// The queue

/**
 * One place in the queue. Each entry has its own id, so the same song can be queued twice and the
 * two rows reorder and remove on their own.
 */
@Immutable
internal class MusicQueueEntry(val id: String, val track: MusicTrack) {
    /** A fresh entry for [track]. */
    constructor(track: MusicTrack) : this(UUID.randomUUID().toString(), track)
}

/** What a press and hold acts on. A held song carries the list it was held in, so Play plays that list from it. */
@Immutable
internal sealed interface MusicQueueTarget {
    data class Artist(val artist: MusicArtist) : MusicQueueTarget

    data class Album(val album: MusicAlbum) : MusicQueueTarget

    data class Track(val track: MusicTrack, val context: List<MusicTrack>) : MusicQueueTarget

    /** What Add to queue adds: the artist's or album's songs, or the one song. */
    fun tracks(catalog: MusicCatalog): List<MusicTrack> = when (this) {
        is Artist -> catalog.tracks(artist)
        is Album -> catalog.tracks(album)
        is Track -> listOf(track)
    }
}

/**
 * Room every Music scroll view leaves under its last entry, so it scrolls clear of the floating
 * action button: the button's 56 dp, with `spacing.lg` under it and the same over it.
 */
internal val MusicFabClearance = 88.dp
