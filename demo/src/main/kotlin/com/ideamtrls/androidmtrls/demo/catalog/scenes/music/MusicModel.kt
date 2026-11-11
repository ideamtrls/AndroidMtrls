package com.ideamtrls.androidmtrls.demo

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import com.ideamtrls.androidmtrls.AMToastItem
import com.ideamtrls.androidmtrls.amInstantSwitch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds

/** The two root views, swapped instantly from the remote: the Artists stack and the Player. */
internal enum class MusicRootView { Artists, Player }

/** The Artists stack's entries: the Artists root, then the artist and album panels it pushes, by id. */
internal sealed interface MusicRoute {
    data object Artists : MusicRoute

    data class Artist(val id: String) : MusicRoute

    data class Album(val id: String) : MusicRoute
}

/** The Repeat button's three states, in the order it cycles. */
internal enum class MusicRepeat {
    Off,
    All,
    One,
    ;

    val next: MusicRepeat
        get() = when (this) {
            Off -> All
            All -> One
            One -> Off
        }
}

/**
 * Owns the Music scene: the site's catalog and the covers read from it, the queue and playback,
 * where the scene stands (the root view and the Artists stack), and every overlay. A plain class
 * holding Compose state, remembered at the scene root and saved with [saver]; the player and
 * anything in flight aren't saved, and the covers come back from memory.
 *
 * The queue is history, the current entry, then up next. History is the played list's songs
 * before the one play began on, then what played after it, shuffle included, so stepping back walks
 * it in that order. Playback is one [MediaPlayer] per track, streaming the site's MP3; a
 * half-second tick in [scope] follows its position, and the player's completion moves the queue on,
 * wherever the scene is showing. Audio focus is asked for on play and playback pauses when it's
 * lost.
 */
@Stable
internal class MusicModel(private val scope: CoroutineScope, context: Context) {
    // Library
    val catalog: MusicCatalog = MusicCatalog.site()

    /**
     * Each album's own cover, read from its first track's tags, by album id. An album has none
     * while its cover loads or when it can't be read, and shows its project photo instead. Covers
     * already in memory (read by an earlier model) are here from the start.
     */
    private val covers = mutableStateMapOf<String, ImageBitmap>()

    // Navigation
    var rootView by mutableStateOf(MusicRootView.Artists)

    /**
     * The Artists stack, its root first. Only its own pushes and pops change it, so it holds while
     * the Player shows, and the Artists tile comes back to the same place.
     */
    val backStack: SnapshotStateList<MusicRoute> = mutableStateListOf(MusicRoute.Artists)

    // Queue and playback
    /** History, the current entry, then up next. */
    private val queue = mutableStateListOf<MusicQueueEntry>()

    var currentIndex by mutableIntStateOf(0)
        private set

    /** Where playback is in the current track, in seconds. */
    var currentTime by mutableDoubleStateOf(0.0)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var isShuffling by mutableStateOf(false)
        private set

    var repeatMode by mutableStateOf(MusicRepeat.Off)
        private set

    // Overlays
    var showingMenu by mutableStateOf(false)
    var showingQueue by mutableStateOf(false)

    /** The held artist, album or song the spotlight shows; null while it's down. */
    var queueMenuTarget by mutableStateOf<MusicQueueTarget?>(null)
    var toast by mutableStateOf<AMToastItem?>(null)

    // Engine
    /** Every entry in the order it was queued; turning shuffle off puts up next back in this order. */
    private var queuedOrder: List<MusicQueueEntry> = emptyList()
    private var player: MediaPlayer? = null

    /** Whether [player] has its stream ready, so it can start, pause and seek. */
    private var prepared = false

    /** Whether the scrubber is held, so the tick leaves the clock where it's dragged. */
    private var scrubbing = false

    /** Whether a seek is on its way, so the tick holds the clock at its target until the stream gets there. */
    private var seeking = false
    private var tick: Job? = null
    private var coversLoad: Job? = null

    /** Whether a short loss of audio focus paused playback, which comes back with the focus. */
    private var resumesOnFocusGain = false

    private val audioManager: AudioManager = context.getSystemService(AudioManager::class.java)
    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private val focusRequest: AudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(audioAttributes)
        .setOnAudioFocusChangeListener(::onFocusChange, Handler(Looper.getMainLooper()))
        .build()

    init {
        // Every song, paused on the first.
        val entries = catalog.tracks.map(::MusicQueueEntry)
        queue.addAll(entries)
        queuedOrder = entries
        for (album in catalog.albums) {
            val url = coverSource(album) ?: continue
            musicCachedCover(url)?.let { covers[album.id] = it }
        }
    }

    // Derived state

    val currentEntry: MusicQueueEntry?
        get() = queue.getOrNull(currentIndex)

    val currentTrack: MusicTrack?
        get() = currentEntry?.track

    val currentAlbum: MusicAlbum?
        get() = currentTrack?.let { catalog.album(it.albumId) }

    val currentArtist: MusicArtist?
        get() = currentAlbum?.let { catalog.artist(it.artistId) }

    /** The entries after the current one. */
    val upNext: List<MusicQueueEntry>
        get() = queue.drop(currentIndex + 1)

    /** Repeat all wraps Next from the last entry to the first, and Previous the other way. */
    private val wraps: Boolean
        get() = repeatMode == MusicRepeat.All

    val canGoBack: Boolean
        get() = previousIndex != null

    val canGoForward: Boolean
        get() = nextIndex != null

    /**
     * Whether the Queue sheet opens over the Artists stack's page at [route]: the page on top while
     * Artists shows. The sheet is composed inside the backdrop of the screen it opens over, so its
     * glass samples that art; the Player hosts it while it shows.
     */
    fun hostsQueue(route: MusicRoute): Boolean = rootView == MusicRootView.Artists && backStack.lastOrNull() == route

    fun isCurrent(track: MusicTrack): Boolean = currentTrack?.id == track.id

    /** The album's embedded cover, once it's read. */
    fun cover(albumId: String?): ImageBitmap? = albumId?.let { covers[it] }

    /** The MP3 an album's cover is read from: its first track's. */
    private fun coverSource(album: MusicAlbum): String? = catalog.tracks(album).firstOrNull()?.audioUrl

    // The step rule: Repeat all wraps, and a one-entry queue never steps.

    private val nextIndex: Int?
        get() = when {
            queue.size <= 1 -> null
            currentIndex < queue.size - 1 -> currentIndex + 1
            wraps -> 0
            else -> null
        }

    private val previousIndex: Int?
        get() = when {
            queue.size <= 1 -> null
            currentIndex > 0 -> currentIndex - 1
            wraps -> queue.size - 1
            else -> null
        }

    // Loading

    /**
     * Readies the current track's stream, paused, then reads every cover not yet in memory from its
     * album's first MP3, all at once, each landing as it's read. The scene's scope runs them, so
     * leaving the scene drops what's still loading.
     */
    fun load() {
        if (player == null && currentEntry != null) loadAudio()
        if (coversLoad != null) return
        coversLoad = scope.launch {
            for (album in catalog.albums) {
                if (covers.containsKey(album.id)) continue
                val url = coverSource(album) ?: continue
                launch { musicEmbeddedCover(url)?.let { covers[album.id] = it } }
            }
        }
    }

    /** Stops playback for good and lets go of the audio focus: the scene is going away. */
    fun stop() {
        stopTick()
        releasePlayer()
        isPlaying = false
        resumesOnFocusGain = false
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    // Transport

    /**
     * Makes the entry at [index] (clamped) current from 0:00, playing on if playback was on. The
     * same index starts the track over.
     */
    fun goTo(index: Int) {
        if (queue.isEmpty()) return
        currentIndex = index.coerceIn(0, queue.lastIndex)
        currentTime = 0.0
        loadAudio()
    }

    fun next() {
        nextIndex?.let(::goTo)
    }

    /** Steps back one entry; it never restarts the track. */
    fun previous() {
        previousIndex?.let(::goTo)
    }

    fun togglePlay() {
        if (currentEntry == null) return
        resumesOnFocusGain = false
        if (isPlaying) pause() else resume()
    }

    /** Shuffle on shuffles up next; off puts it back in queued order. The current entry and history never move. */
    fun toggleShuffle() {
        isShuffling = !isShuffling
        if (isShuffling) {
            setUpNext(upNext.shuffled())
        } else {
            val ids = upNext.mapTo(HashSet()) { it.id }
            setUpNext(queuedOrder.filter { it.id in ids })
        }
    }

    fun cycleRepeat() {
        repeatMode = repeatMode.next
    }

    /**
     * The scrubber moving: the clock follows it to [seconds], stopping [SeekEndMargin] short of the
     * track's end, so a scrubber held at its far end can't skip (only playback reaching the end
     * moves the queue on). The stream seeks when the scrubber is let go ([seekToClock]).
     */
    fun scrub(seconds: Double) {
        val end = max((currentTrack?.durationSeconds ?: 0) - SeekEndMargin, 0.0)
        currentTime = seconds.coerceIn(0.0, end)
        scrubbing = true
    }

    /** The scrubber let go (a drag's end, a tap, a TalkBack adjustment): the stream seeks to the clock. */
    fun seekToClock() {
        scrubbing = false
        val player = player ?: return
        if (!prepared) return // Once ready, the stream starts from the clock.
        seeking = true
        player.seekTo(currentTime.toMillis(), MediaPlayer.SEEK_CLOSEST)
    }

    // Playing and queueing

    /**
     * Replaces the queue with fresh entries for [tracks] and plays from [index] on the Player; the
     * Artists stack keeps its place. `true` / `false` turn shuffle on / off; null keeps the
     * setting, shuffling what follows while it's on.
     */
    fun play(tracks: List<MusicTrack>, index: Int, shuffled: Boolean? = null) {
        if (tracks.isEmpty()) return
        if (shuffled != null) isShuffling = shuffled
        resumesOnFocusGain = false
        isPlaying = requestFocus()
        replaceQueue(tracks, index)
        if (isPlaying) startTick()
        switchTo(MusicRootView.Player)
    }

    /**
     * The Play and Shuffle buttons and tiles, and a held song's Play: an artist's or album's songs
     * from the first, with shuffle on or off, or a song from its place in the list it was held in,
     * keeping the shuffle setting, as tapping its row does. Closes the spotlight.
     */
    fun play(target: MusicQueueTarget, shuffled: Boolean = false) {
        queueMenuTarget = null
        when (target) {
            is MusicQueueTarget.Track -> play(target.context, target.context.indexOf(target.track).coerceAtLeast(0))
            else -> play(target.tracks(catalog), 0, shuffled)
        }
    }

    /** Add to queue: fresh entries at the end of up next, then a toast. Closes the spotlight. */
    fun addToQueue(target: MusicQueueTarget) {
        val entries = target.tracks(catalog).map(::MusicQueueEntry)
        queue.addAll(entries)
        queuedOrder = queuedOrder + entries
        queueMenuTarget = null
        toast = AMToastItem(
            if (entries.size == 1) "Added to the queue" else "Added ${musicCount(entries.size, "song")} to the queue",
        )
    }

    /** Removes an up-next entry; history and the current entry stay. */
    fun removeFromQueue(id: String) {
        val index = queue.indexOfFirst { it.id == id }
        if (index > currentIndex) queue.removeAt(index)
    }

    /** Moves an up-next entry from one place in up next to another, as the reorderable list asks. */
    fun moveUpNext(from: Int, to: Int) {
        val base = currentIndex + 1
        if (base + from !in queue.indices || base + to !in queue.indices) return
        queue.add(base + to, queue.removeAt(base + from))
    }

    // Navigation

    fun openArtist(id: String) {
        backStack.add(MusicRoute.Artist(id))
    }

    fun openAlbum(id: String) {
        backStack.add(MusicRoute.Album(id))
    }

    /** Pops the top panel; the Artists root stays. */
    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    /**
     * A root-view tile: lands on [view], instantly. The menu closes outside the switch, so its
     * fade plays over the new view. The Artists stack is left alone, so it comes back exactly
     * where it was.
     */
    fun switchTo(view: MusicRootView) {
        showingMenu = false
        amInstantSwitch { rootView = view }
    }

    /** The Queue tile: closes the menu and opens the sheet. */
    fun openQueue() {
        showingMenu = false
        showingQueue = true
    }

    // Engine

    private fun setUpNext(entries: List<MusicQueueEntry>) {
        val kept = queue.take(currentIndex + 1)
        queue.clear()
        queue.addAll(kept + entries)
    }

    /** Fresh entries for [tracks], the one at [index] current; while shuffle is on, what follows it is shuffled. */
    private fun replaceQueue(tracks: List<MusicTrack>, index: Int) {
        val entries = tracks.map(::MusicQueueEntry)
        queue.clear()
        queue.addAll(entries)
        queuedOrder = entries
        goTo(index)
        if (isShuffling) setUpNext(upNext.shuffled())
    }

    private fun pause() {
        isPlaying = false
        if (prepared) player?.pause()
        stopTick()
    }

    private fun resume() {
        if (!requestFocus()) return
        isPlaying = true
        val player = player
        if (player == null) loadAudio() else if (prepared) player.start()
        startTick()
    }

    /** Streams the current track into a fresh player from [currentTime], playing it while playback is on. */
    private fun loadAudio() {
        releasePlayer()
        val track = currentTrack ?: return
        val player = MediaPlayer()
        this.player = player
        player.setAudioAttributes(audioAttributes)
        player.setOnPreparedListener { ready ->
            if (this.player !== ready) return@setOnPreparedListener
            prepared = true
            if (currentTime > 0) {
                seeking = true
                ready.seekTo(currentTime.toMillis(), MediaPlayer.SEEK_CLOSEST)
            }
            if (isPlaying) ready.start()
        }
        player.setOnSeekCompleteListener { done -> if (this.player === done) seeking = false }
        player.setOnCompletionListener { done -> if (this.player === done) trackEnded() }
        player.setOnErrorListener { failed, _, _ ->
            if (this.player === failed) streamFailed()
            true
        }
        try {
            player.setDataSource(track.audioUrl)
            player.prepareAsync()
        } catch (_: Exception) {
            streamFailed()
        }
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        prepared = false
        seeking = false
        scrubbing = false
    }

    /** A stream that fails (offline, say) stops playback and drops the player, so the next Play streams it again. */
    private fun streamFailed() {
        isPlaying = false
        stopTick()
        releasePlayer()
    }

    private fun startTick() {
        tick?.cancel()
        tick = scope.launch {
            while (true) {
                delay(TickInterval)
                advanceClock()
            }
        }
    }

    private fun stopTick() {
        tick?.cancel()
        tick = null
    }

    /** One tick: the clock follows the player while it plays, and holds while it buffers, seeks or is scrubbed. */
    private fun advanceClock() {
        val player = player ?: return
        if (!prepared || seeking || scrubbing || !player.isPlaying) return
        currentTime = player.currentPosition / 1000.0
    }

    /**
     * Repeat one plays the track again, and so does Repeat all on a one-entry queue, which never
     * steps; otherwise playback moves on, or parks paused at 0:00 when nothing follows.
     */
    private fun trackEnded() {
        val player = player
        if (repeatMode == MusicRepeat.One || (wraps && queue.size == 1)) {
            currentTime = 0.0
            player?.seekTo(0L, MediaPlayer.SEEK_CLOSEST)
            player?.start()
        } else if (canGoForward) {
            next()
        } else {
            isPlaying = false
            stopTick()
            currentTime = 0.0
            player?.seekTo(0L, MediaPlayer.SEEK_CLOSEST)
        }
    }

    private fun requestFocus(): Boolean =
        audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    /**
     * Losing the audio focus pauses playback: for good (another app plays) it lets the focus go;
     * for a moment (a call, a navigation prompt) it plays on when the focus comes back. A request
     * to duck is the system's to handle.
     */
    private fun onFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumesOnFocusGain = false
                if (isPlaying) pause()
                audioManager.abandonAudioFocusRequest(focusRequest)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (isPlaying) {
                resumesOnFocusGain = true
                pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> if (resumesOnFocusGain) {
                resumesOnFocusGain = false
                resume()
            }
        }
    }

    companion object {
        /** How far short of a track's end a seek stops, in seconds. */
        private const val SeekEndMargin = 1.0

        private val TickInterval = 500.milliseconds

        private fun Double.toMillis(): Long = (this * 1000).roundToLong()

        /**
         * Saves where the scene stands, the queue (each entry's id and song), the queued order,
         * the place in the current track, shuffle and repeat, and which overlay is up; restores
         * them into a model on the restored scene's [scope]. Playback and the player aren't kept: a
         * restored scene is paused where it was, its covers back from memory.
         */
        fun saver(scope: CoroutineScope, context: Context): Saver<MusicModel, Any> = mapSaver(
            save = { model ->
                mapOf(
                    "root" to model.rootView.name,
                    "stack" to ArrayList(model.backStack.mapNotNull(::encodeRoute)),
                    "queue" to ArrayList(model.queue.map { "${it.id}|${it.track.id}" }),
                    "order" to ArrayList(model.queuedOrder.map { it.id }),
                    "index" to model.currentIndex,
                    "time" to model.currentTime,
                    "shuffle" to model.isShuffling,
                    "repeat" to model.repeatMode.name,
                    "menu" to model.showingMenu,
                    "queueSheet" to model.showingQueue,
                    "held" to (model.queueMenuTarget?.let(::encodeTarget) ?: ""),
                )
            },
            restore = { saved ->
                MusicModel(scope, context).apply {
                    rootView = MusicRootView.valueOf(saved["root"] as String)
                    backStack.addAll((saved["stack"] as List<*>).mapNotNull { line -> (line as? String)?.let { decodeRoute(it) } })
                    val entries = (saved["queue"] as List<*>).mapNotNull { line ->
                        val (id, trackId) = (line as? String)?.split('|', limit = 2) ?: return@mapNotNull null
                        catalog.track(trackId)?.let { MusicQueueEntry(id, it) }
                    }
                    if (entries.isNotEmpty()) {
                        queue.clear()
                        queue.addAll(entries)
                        val byId = entries.associateBy { it.id }
                        queuedOrder = (saved["order"] as List<*>).mapNotNull { id -> (id as? String)?.let { byId[it] } }
                        currentIndex = (saved["index"] as Int).coerceIn(0, entries.lastIndex)
                        currentTime = saved["time"] as Double
                    }
                    isShuffling = saved["shuffle"] as Boolean
                    repeatMode = MusicRepeat.valueOf(saved["repeat"] as String)
                    showingMenu = saved["menu"] as Boolean
                    showingQueue = saved["queueSheet"] as Boolean
                    queueMenuTarget = decodeTarget(saved["held"] as String)
                }
            },
        )

        private fun encodeRoute(route: MusicRoute): String? = when (route) {
            MusicRoute.Artists -> null
            is MusicRoute.Artist -> "artist:${route.id}"
            is MusicRoute.Album -> "album:${route.id}"
        }

        private fun MusicModel.decodeRoute(saved: String): MusicRoute? {
            val (kind, id) = saved.split(':', limit = 2).takeIf { it.size == 2 } ?: return null
            return when (kind) {
                "artist" -> catalog.artist(id)?.let { MusicRoute.Artist(id) }
                "album" -> catalog.album(id)?.let { MusicRoute.Album(id) }
                else -> null
            }
        }

        /** A held song is held on its album's page, so its album's songs are its list. */
        private fun encodeTarget(target: MusicQueueTarget): String = when (target) {
            is MusicQueueTarget.Artist -> "artist:${target.artist.id}"
            is MusicQueueTarget.Album -> "album:${target.album.id}"
            is MusicQueueTarget.Track -> "track:${target.track.id}"
        }

        private fun MusicModel.decodeTarget(saved: String): MusicQueueTarget? {
            val (kind, id) = saved.split(':', limit = 2).takeIf { it.size == 2 } ?: return null
            return when (kind) {
                "artist" -> catalog.artist(id)?.let { MusicQueueTarget.Artist(it) }
                "album" -> catalog.album(id)?.let { MusicQueueTarget.Album(it) }
                "track" -> catalog.track(id)?.let { track ->
                    MusicQueueTarget.Track(track, catalog.album(track.albumId)?.let { catalog.tracks(it) } ?: listOf(track))
                }
                else -> null
            }
        }
    }
}
