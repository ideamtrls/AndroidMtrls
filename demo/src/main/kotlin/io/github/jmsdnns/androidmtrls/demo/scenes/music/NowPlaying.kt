package io.github.jmsdnns.androidmtrls.demo.scenes.music

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.collections.AMCarousel
import io.github.jmsdnns.androidmtrls.content.AMBadgeContent
import io.github.jmsdnns.androidmtrls.content.AMEmptyState
import io.github.jmsdnns.androidmtrls.content.amShimmer
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGrid
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridRow
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButton
import io.github.jmsdnns.androidmtrls.controls.pickers.AMSlider
import io.github.jmsdnns.androidmtrls.controls.pickers.AMSliderDecoration
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.scenes.common.LocalSceneExit
import io.github.jmsdnns.androidmtrls.demo.scenes.common.SceneGradient
import io.github.jmsdnns.androidmtrls.demo.scenes.common.ScenePanelChrome
import io.github.jmsdnns.androidmtrls.demo.scenes.common.ScenePanelHost
import io.github.jmsdnns.androidmtrls.demo.scenes.common.brush
import io.github.jmsdnns.androidmtrls.demo.scenes.common.sceneFabClearance
import io.github.jmsdnns.androidmtrls.demo.scenes.common.sceneFabLift
import io.github.jmsdnns.androidmtrls.lists.AMListRow
import io.github.jmsdnns.androidmtrls.lists.AMReorderableRowList
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMToolbarButton
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMToolbarButtonSize
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMActionsOverlay
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMFloatingActionsHost
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMSheet
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMSheetHeight
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMToast
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMToastItem
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for the [NowPlayingScene] demo (Screens section). id
 * `scene-nowplaying`. Renders full-screen with no `ComponentPage` scaffold — the
 * scene owns its chrome and its Exit affordance ([LocalSceneExit]). Mirrors iOS
 * `NowPlayingScene` registration.
 */
val nowPlayingSceneEntry: ComponentEntry = ComponentEntry(
    id = "scene-nowplaying",
    section = ComponentSection.Screens,
    title = "Now Playing",
    subtitle = "Player and library with a real queue",
    icon = Icons.Filled.PlayCircle,
    content = { NowPlayingScene() },
)

// ---------------------------------------------------------------------------
// Tuning constants
// ---------------------------------------------------------------------------

/** The playback sync cadence, in ms — every tick pulls `currentTime` from the player. */
private const val musicTickMs = 500L

/** Music-note glyph edge for the full-bleed player cover. */
private val playerCoverGlyphSize = 120.dp

// ---------------------------------------------------------------------------
// MusicState — the scene's remembered state holder + simulated playback engine
// ---------------------------------------------------------------------------

/**
 * The Now Playing scene's single state holder, remembered once at the scene root
 * (no ViewModel). Owns the [catalog] (scanned from the bundled MP3s at launch),
 * the live playback queue ([playOrder]), the transport state, the panel [path],
 * and the transient overlays; also drives a **real** [MediaPlayer] over the
 * bundled audio. Ports iOS `NowPlayingScene`'s `@State` cluster and its playback
 * methods (AVAudioPlayer → MediaPlayer).
 *
 * The queue is a list of [MusicQueueItem]s (track + process-unique `uid`) so the
 * player carousel and the reorderable queue sheet have stable, unique keys even
 * when the same track appears twice (see [MusicQueueItem]).
 *
 * @param appContext application context — the asset source for the catalog scan
 *                   and the [MediaPlayer] data source.
 */
@Stable
class MusicState(private val appContext: Context) {
    /** The library, scanned from `assets/Audio/`. Empty until [load] completes. */
    var catalog: MusicCatalog by mutableStateOf(MusicCatalog(emptyList(), emptyList(), "Library"))
        private set

    /** The live audio player for the current track, or `null` before the first load. */
    private var player: MediaPlayer? = null

    /**
     * Monotonic load-generation stamp. [reloadAudio] bumps it on entry and re-checks
     * it once the blocking `prepare()` returns; a reload whose stamp has since been
     * superseded lost the race and releases its own player instead of assigning it.
     */
    private var loadGeneration: Long = 0L

    /** Monotonic source of [MusicQueueItem.uid] values — never reused within a run. */
    private var nextUid: Long = 0L

    /** Tags a bare [track] with a fresh unique id for stable queue keys. */
    private fun wrap(track: MusicTrack): MusicQueueItem = MusicQueueItem(uid = nextUid++, track = track)

    /** `true` until the catalog scan completes — gates the shimmer + FAB. */
    var isLoading: Boolean by mutableStateOf(true)
        private set

    /** Which top-level view the scene shows. */
    var currentView: MusicView by mutableStateOf(MusicView.Player)
        private set

    /** The live playback queue. Replaced wholesale by play / shuffle / reorder / enqueue. */
    var playOrder: List<MusicQueueItem> by mutableStateOf(emptyList())
        private set

    /** Index of the currently playing slot within [playOrder]. */
    var trackIndex: Int by mutableIntStateOf(0)
        private set

    /** The simulated playback clock, in seconds. Written by the tick and by scrubber seeks. */
    var currentTime: Double by mutableStateOf(0.0)

    /** Whether the simulated clock is advancing. */
    var isPlaying: Boolean by mutableStateOf(false)
        private set

    /** Whether the upcoming queue is shuffled. */
    var isShuffling: Boolean by mutableStateOf(false)
        private set

    /** The repeat cycle. */
    var repeatMode: RepeatMode by mutableStateOf(RepeatMode.Off)
        private set

    /** Whether the full-height Up Next queue sheet is presented. */
    var showQueue: Boolean by mutableStateOf(false)

    /** The long-press `[Play, Add to Queue]` target, or `null` when the menu is closed. */
    var queueMenuTarget: MusicQueueTarget? by mutableStateOf(null)

    /** The transient confirmation toast. */
    var toast: AMToastItem? by mutableStateOf(null)

    /** The drill-in panel stack (Artist → Album). Empty → only the root shows. */
    var path: List<MusicRoute> by mutableStateOf(emptyList())
        private set

    /**
     * One-shot flag: `true` for the recomposition after a FAB view-switch (or a
     * fresh `play`) so a stale panel collapses instantly (no slide), tab-bar style
     * — the iOS `imInstantSwitch` twin. The scene clears it right after.
     */
    var snapNextPanel: Boolean by mutableStateOf(false)

    // ── Derived state ──────────────────────────────────────────────────────

    /** The currently playing queue slot, or `null` while loading / the queue is empty. */
    val currentItem: MusicQueueItem?
        get() {
            if (playOrder.isEmpty()) return null
            return playOrder[trackIndex.coerceIn(0, playOrder.lastIndex)]
        }

    /** The currently playing track, or `null` while still loading / the queue is empty. */
    val currentTrack: MusicTrack?
        get() = currentItem?.track

    /** The album the current track belongs to, or `null` while loading. */
    val currentAlbum: MusicAlbum?
        get() = currentTrack?.let { catalog.album(it.albumId) }

    /** The player gradient — the current album's palette, or neutral while loading. */
    val currentPalette: SceneGradient
        get() = currentAlbum?.let { musicPalette(it.id) } ?: musicNeutralPalette

    /** Everything after the current track — the queue sheet's contents. */
    val upNext: List<MusicQueueItem>
        get() = if (trackIndex + 1 < playOrder.size) {
            playOrder.subList(trackIndex + 1, playOrder.size).toList()
        } else {
            emptyList()
        }

    // ── Loading ──────────────────────────────────────────────────────────

    /**
     * Scans `assets/Audio/` for the catalog (metadata + embedded artwork), then
     * seeds the queue with the whole library, parked at track 0. Idempotent —
     * runs once. Mirrors iOS `NowPlayingScene`'s `.task` loader.
     */
    suspend fun load() {
        if (!isLoading) return
        val loaded = loadMusicCatalog(appContext)
        catalog = loaded
        if (loaded.tracks.isNotEmpty()) {
            playOrder = loaded.tracks.map { wrap(it) }
            trackIndex = 0
            currentTime = 0.0
        }
        isLoading = false
    }

    // ── Audio engine (MediaPlayer) ─────────────────────────────────────────

    /**
     * Loads [currentTrack]'s bundled MP3 into a fresh [MediaPlayer], replacing any
     * previous one, seeking to the current [currentTime], and starting playback
     * when [autoplay]. Runs the (blocking) prepare off the main thread. No current
     * track leaves the player `null` (the tick then simulates in silence). Mirrors
     * iOS `loadAudio(autoplay:)`.
     *
     * **Generation-safe.** A [LaunchedEffect] cancellation can't interrupt the
     * blocking `prepare()`/`start()`, so a superseded reload would otherwise finish
     * preparing a second player and assign it over the winner (two players, one
     * leaked). Each call stamps [loadGeneration] on entry and re-checks it once
     * `prepare()` returns: if a newer reload has claimed the stamp (or this coroutine
     * was cancelled) the freshly prepared player is released and nothing is
     * assigned/started, so exactly one player survives and the previous one is
     * released exactly once.
     */
    suspend fun reloadAudio(autoplay: Boolean) = withContext(Dispatchers.IO) {
        val myGeneration = ++loadGeneration

        // Null the field before releasing so the main-thread tick reads `null` and
        // skips (rather than touching a half-released player mid-swap). A concurrent
        // superseding reload then reads `null` here and releases nothing, so the
        // previous player is released exactly once — by the reload that claimed it.
        val previous = player
        player = null
        runCatching { previous?.release() }

        val track = currentTrack ?: return@withContext
        if (track.assetPath.isEmpty()) return@withContext
        val mp = MediaPlayer()
        val ok = runCatching {
            appContext.assets.openFd(track.assetPath).use { afd ->
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            mp.prepare()
        }.isSuccess
        if (!ok) {
            mp.release()
            return@withContext
        }

        // prepare() returned; if a newer reload has since claimed the generation (or
        // this coroutine was cancelled) we lost the race — release this freshly
        // prepared player and leave the winner's untouched instead of assigning over it.
        if (myGeneration != loadGeneration || !isActive) {
            mp.release()
            return@withContext
        }

        mp.seekTo((currentTime * 1000).roundToInt())
        if (autoplay) mp.start()
        player = mp
    }

    /** Resumes the loaded player (play/resume). No-op before the first load. */
    fun startPlayer() {
        runCatching { player?.start() }
    }

    /** Pauses the loaded player. No-op before the first load. */
    fun pausePlayer() {
        runCatching { player?.takeIf { it.isPlaying }?.pause() }
    }

    /** Releases the player (scene teardown). */
    fun releasePlayer() {
        runCatching { player?.release() }
        player = null
    }

    /**
     * Applies a scrubber seek to the player: only a jump larger than the tick's
     * own cadence (>1s from the live position) counts as a seek, so the tick's own
     * per-cycle `currentTime` writes never bounce the player. Mirrors iOS's
     * `.onChange(of: currentTime)` guard.
     */
    fun applySeekIfNeeded() {
        val mp = player ?: return
        val target = (currentTime * 1000).roundToInt()
        runCatching {
            if (abs(target - mp.currentPosition) > 1000) mp.seekTo(target)
        }
    }

    /**
     * One 0.5s tick: pull [currentTime] from the live player, and — when the player
     * has stopped on its own after making progress — auto-advance at track end. In
     * the (missing-resource) player-less branch the clock simulates instead.
     * Mirrors iOS `startPlaybackTick`'s body.
     */
    fun syncTick() {
        val mp = player
        val track = currentTrack ?: return
        if (mp != null) {
            // `mp` may be released concurrently by reloadAudio (IO thread) mid-swap;
            // a released player throws IllegalStateException on these reads, so guard
            // and skip this tick — the next one syncs against the fresh player.
            val playing = runCatching {
                currentTime = mp.currentPosition / 1000.0
                mp.isPlaying
            }.getOrElse { return }
            if (!playing && currentTime > 0.0) {
                advanceAtTrackEnd()
            }
        } else if (currentTime < track.durationSeconds) {
            currentTime += 0.5
        } else {
            advanceAtTrackEnd()
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    /** Pushes the artist panel (called from the library root). */
    fun openArtist() {
        path = path + MusicRoute.Artist
    }

    /** Pushes an album panel (called from the artist panel). */
    fun openAlbum(albumId: String) {
        path = path + MusicRoute.Album(albumId)
    }

    /** Pops the top panel (back button / predictive back). */
    fun pop() {
        if (path.isNotEmpty()) path = path.dropLast(1)
    }

    /** FAB "Library" tile — swap to the library root, clearing any pushed panel (instant). */
    fun showLibrary() {
        snapNextPanel = true
        currentView = MusicView.Library
        path = emptyList()
    }

    /** FAB "Artist" tile — jump straight to the artist panel (instant, tab-bar style). */
    fun jumpToArtist() {
        snapNextPanel = true
        currentView = MusicView.Library
        path = listOf(MusicRoute.Artist)
    }

    /** FAB "Album" tile — jump straight to the current track's album panel (instant). */
    fun jumpToAlbum() {
        val album = currentAlbum ?: return
        snapNextPanel = true
        currentView = MusicView.Library
        path = listOf(MusicRoute.Album(album.id))
    }

    /** FAB "Player" tile — swap to the player, clearing any pushed panel (instant). */
    fun showPlayer() {
        snapNextPanel = true
        currentView = MusicView.Player
        path = emptyList()
    }

    // ── Transport ──────────────────────────────────────────────────────────

    /**
     * Jump to [index] without stopping playback (carousel select, next/prev,
     * auto-advance). A real index change re-keys the scene's reload effect (new
     * track's audio loads); a no-op index (e.g. repeat-all wrapping a single-track
     * queue) can't re-key it, so the player is rewound directly here. Mirrors iOS
     * `goTo`'s `needsDirectReload` path.
     */
    fun goTo(index: Int) {
        if (playOrder.isEmpty()) return
        val target = index.coerceIn(0, playOrder.lastIndex)
        val sameIndex = target == trackIndex
        trackIndex = target
        currentTime = 0.0
        if (sameIndex) {
            runCatching {
                player?.seekTo(0)
                if (isPlaying) player?.start()
            }
        }
    }

    /** Scrubber seek — a drag overwrites the simulated clock (and so wins over the tick). */
    fun seek(time: Double) {
        val duration = currentTrack?.durationSeconds ?: return
        currentTime = time.coerceIn(0.0, duration)
    }

    /** Play/pause toggle — the tick effect keys off [isPlaying]. */
    fun togglePlay() {
        isPlaying = !isPlaying
    }

    /** Advance the repeat cycle off → all → one → off. */
    fun cycleRepeat() {
        repeatMode = repeatMode.next
    }

    /**
     * Reshuffle everything after the current track (restoring catalog order when
     * toggled off). Mirrors iOS `toggleShuffle`; the restore path sorts the tail
     * back into catalog order while keeping each slot's stable `uid` (and any
     * duplicates), a small improvement over iOS's id-set filter.
     */
    fun toggleShuffle() {
        isShuffling = !isShuffling
        if (trackIndex + 1 >= playOrder.size) return
        val head = playOrder.subList(0, trackIndex + 1).toList()
        val tail = playOrder.subList(trackIndex + 1, playOrder.size).toList()
        val newTail = if (isShuffling) {
            tail.shuffled()
        } else {
            val order = catalog.tracks.withIndex().associate { (i, t) -> t.id to i }
            tail.sortedBy { order[it.track.id] ?: Int.MAX_VALUE }
        }
        playOrder = head + newTail
    }

    /** Called by the tick when the player finishes the current track. */
    fun advanceAtTrackEnd() {
        when (repeatMode) {
            RepeatMode.One -> {
                currentTime = 0.0 // replay, stay playing
                runCatching {
                    player?.seekTo(0)
                    player?.start()
                }
            }
            RepeatMode.Off, RepeatMode.All -> {
                if (trackIndex < playOrder.lastIndex) {
                    goTo(trackIndex + 1)
                } else if (repeatMode == RepeatMode.All) {
                    goTo(0)
                } else {
                    isPlaying = false // parked at the end
                    pausePlayer()
                }
            }
        }
    }

    /** Skip to the next track, wrapping to 0 only under [RepeatMode.All]. */
    fun next() {
        if (trackIndex < playOrder.lastIndex) {
            goTo(trackIndex + 1)
        } else if (repeatMode == RepeatMode.All) {
            goTo(0)
        }
    }

    /** Skip to the previous track (no-op at the head). */
    fun previous() {
        if (trackIndex > 0) goTo(trackIndex - 1)
    }

    // ── Queue mutation ─────────────────────────────────────────────────────

    /**
     * Play a fresh collection from [index]: it becomes the queue, playback starts,
     * and the scene returns to the player. Mirrors iOS `play(_:startingAt:)`.
     */
    fun play(tracks: List<MusicTrack>, index: Int) {
        if (tracks.isEmpty()) return
        snapNextPanel = true
        playOrder = tracks.map { wrap(it) }
        isPlaying = true
        goTo(index)
        queueMenuTarget = null
        currentView = MusicView.Player
        path = emptyList()
    }

    /** Append a long-press target's tracks to the end of the queue. */
    fun addToQueue(target: MusicQueueTarget) {
        playOrder = playOrder + target.tracks(catalog).map { wrap(it) }
        queueMenuTarget = null
        toast = AMToastItem(text = "Added to queue")
    }

    /**
     * Write a reordered Up Next back into the queue (below the current track).
     * Silent: the reorderable list fires this on every drag-over move, so the
     * "Queue updated" toast is raised once per settled drag via [notifyQueueReordered],
     * not here.
     */
    fun reorderQueue(newUpNext: List<MusicQueueItem>) {
        val cut = (trackIndex + 1).coerceIn(0, playOrder.size)
        playOrder = playOrder.subList(0, cut).toList() + newUpNext
    }

    /** Raise the single "Queue updated" toast once a queue-reorder drag settles. */
    fun notifyQueueReordered() {
        toast = AMToastItem(text = "Queue updated")
    }
}

// ---------------------------------------------------------------------------
// NowPlayingScene — the scene root
// ---------------------------------------------------------------------------

/**
 * Two-view music scene — Player + Library — over a [MusicCatalog] scanned at
 * launch from the bundled MP3s under `assets/Audio/` (titles, artist, albums,
 * track order, durations, and embedded cover art), with a **real** [MediaPlayer]
 * backing playback. The **Player** is the gradient screen: a full-bleed
 * [AMCarousel] of cover art drives `trackIndex`, with a flat transport band
 * beneath ([AMSlider] scrubber, [AMIconButton] shuffle/prev/next/repeat, prominent
 * [AMToolbarButton] play/pause). The **Library** is the sibling view the FAB grid
 * swaps in; its single artist row drills into the artist's albums and then an
 * album's tracks via [ScenePanelHost]. A full-height [AMSheet] hosts the Up Next
 * queue as an [AMReorderableRowList]; a long-press in the library raises a
 * `[Play, Add to Queue]` [AMActionsOverlay]. Ports iOS `NowPlayingScene`.
 *
 * **Overlay host nesting** (outermost → innermost):
 * `AMToast → AMActionsOverlay(queue menu) → AMSheet(queue) → AMFloatingActionsHost
 * → ScenePanelHost`. Each host wraps the scene body and ORs its presented state
 * into `LocalAMOverlayActive`, so the hub's FAB (innermost) fades under any
 * feedback / modal, and the sheet + menu cover pushed panels too.
 *
 * **Playback**: one `LaunchedEffect` scans the catalog; another (keyed on the
 * current queue slot's uid) loads that track's MP3 into a [MediaPlayer], continuing
 * playback if it was already playing; a third (keyed on `isPlaying`) starts/pauses
 * the player and runs a 0.5s tick that pulls `currentTime` from it and
 * auto-advances at track end per [RepeatMode]; a fourth pushes scrubber drags to
 * the player as seeks. A [DisposableEffect] releases the player on teardown.
 */
@Composable
fun NowPlayingScene() {
    val context = LocalContext.current
    val state = remember(context) { MusicState(context.applicationContext) }
    val sceneExit = LocalSceneExit.current

    var showActions by remember { mutableStateOf(false) }
    val fabOffset = remember { mutableStateOf(Offset.Zero) }

    // Clear the one-shot instant-switch flag right after the frame that consumed it.
    LaunchedEffect(state.snapNextPanel) {
        if (state.snapNextPanel) state.snapNextPanel = false
    }

    // Scan the bundled MP3s once, then seed the queue (mirrors iOS `.task`).
    LaunchedEffect(Unit) { state.load() }

    // Load the current track's audio whenever the current queue slot changes —
    // including the null → first-slot transition once the catalog finishes loading.
    // Keyed on the slot's `uid`, not the track id, so advancing between two adjacent
    // copies of the SAME track (identical id, distinct uid) still reloads/restarts.
    // Continues playback if it was already playing (autoplay = isPlaying). Mirrors
    // iOS `.onChange(of: trackIndex)` + the `.task` initial load.
    LaunchedEffect(state.currentItem?.uid) {
        state.reloadAudio(autoplay = state.isPlaying)
    }

    // Play/pause + the 0.5s sync tick. Keyed on isPlaying: entering play starts the
    // player and pulls `currentTime` from it each cycle (auto-advancing at track
    // end); entering pause parks the player. Mirrors iOS `togglePlay` + the tick.
    LaunchedEffect(state.isPlaying) {
        if (!state.isPlaying) {
            state.pausePlayer()
            return@LaunchedEffect
        }
        state.startPlayer()
        while (isActive) {
            delay(musicTickMs)
            state.syncTick()
        }
    }

    // A scrubber drag writes `currentTime`; push it to the player as a seek (the
    // guard ignores the tick's own per-cycle writes). Mirrors iOS
    // `.onChange(of: currentTime)`.
    LaunchedEffect(state.currentTime) {
        state.applySeekIfNeeded()
    }

    // Release the player when the scene leaves composition (mirrors iOS `.onDisappear`).
    DisposableEffect(state) {
        onDispose { state.releasePlayer() }
    }

    // ── Host stack: toast → queue menu → queue sheet → FAB → body ──
    AMToast(
        item = state.toast,
        onDismiss = { state.toast = null },
    ) {
        AMActionsOverlay(
            isPresented = state.queueMenuTarget != null,
            onDismiss = { state.queueMenuTarget = null },
            overlayContent = { QueueTargetMenu(state) },
        ) {
            AMSheet(
                isPresented = state.showQueue,
                onDismiss = { state.showQueue = false },
                height = AMSheetHeight.Full,
                sheetContent = { QueueSheet(state) },
            ) {
                AMFloatingActionsHost(
                    isPresented = showActions,
                    onPresentChange = { showActions = it },
                    bottomInset = sceneFabLift(),
                    enabled = !state.isLoading,
                    offset = fabOffset,
                    accessibilityLabel = "Music menu",
                    menu = { FabMenu(state, sceneExit) { showActions = it } },
                ) {
                    NowPlayingBody(state)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Scene body — panel stack over the player/library root
// ---------------------------------------------------------------------------

@Composable
private fun NowPlayingBody(state: MusicState) {
    ScenePanelHost(
        path = state.path,
        onPop = { state.pop() },
        snapNext = state.snapNextPanel,
        root = { RootContent(state) },
        panel = { route ->
            when (route) {
                is MusicRoute.Artist -> {
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        ScenePanelChrome(title = state.catalog.artistName, onBack = { state.pop() })
                        MusicArtistPanel(state)
                    }
                }

                is MusicRoute.Album -> {
                    val album = state.catalog.album(route.albumId)
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        ScenePanelChrome(title = album.title, onBack = { state.pop() })
                        MusicAlbumPanel(state, album)
                    }
                }
            }
        },
    )
}

/**
 * Loading (scanning) → player / library. The album gradient backs the player and
 * the loading skeleton; the library is a "normal" page surface (it supplies its
 * own `Surface.page` fill). The bundled MP3s always yield a non-empty catalog, so
 * there is no dedicated "No Music" branch — a failed scan simply leaves the queue
 * empty and the player shows the neutral gradient.
 */
@Composable
private fun RootContent(state: MusicState) {
    when {
        state.isLoading -> SceneGradientBox(musicNeutralPalette) { LoadingLayout() }
        state.currentView == MusicView.Player -> SceneGradientBox(state.currentPalette) { PlayerLayout(state) }
        else -> MusicLibraryView(state)
    }
}

/** A full-size box washed with a [SceneGradient] for the current scheme. */
@Composable
private fun SceneGradientBox(palette: SceneGradient, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(palette.brush()),
        content = content,
    )
}

// ---------------------------------------------------------------------------
// Player layout
// ---------------------------------------------------------------------------

/**
 * The gradient screen: a full-bleed square cover-art carousel (drives
 * `trackIndex`) with the flat transport band connected directly beneath it, both
 * pinned to the top; a bottom spacer drops the leftover space and the bottom
 * padding reserves the FAB zone. Ports iOS `playerLayout` / `playerScreen`.
 */
@Composable
private fun PlayerLayout(state: MusicState) {
    val isDark = LocalAMIsDarkTheme.current
    val track = state.currentTrack ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = sceneFabClearance()),
    ) {
        AMCarousel(
            items = state.playOrder,
            key = { it.uid },
            currentIndex = state.trackIndex,
            onIndexChange = { state.goTo(it) },
            sideInset = 0.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { item ->
            MusicCoverArt(
                coverArt = item.track.coverArt,
                palette = musicPalette(item.track.albumId),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                cornerRadius = 0.dp,
                glyphSize = playerCoverGlyphSize,
            )
        }

        PlayerControls(
            state = state,
            track = track,
            modifier = Modifier.background(AMTheme.Surface.primary.resolve(isDark)),
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * The loading state IS the player layout rendered from [musicPlaceholderTrack]:
 * a `separator`-filled cover block over a placeholder meta + a disabled transport
 * silhouette + an inert scrubber, the whole thing shimmered. Ports iOS
 * `loadingLayout` (`.redacted` + `.imShimmer()` + `.disabled(true)`).
 */
@Composable
private fun LoadingLayout() {
    val isDark = LocalAMIsDarkTheme.current
    val track = musicPlaceholderTrack

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = sceneFabClearance())
            .amShimmer(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(AMTheme.separator.resolve(isDark)),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AMTheme.Surface.primary.resolve(isDark))
                .padding(AMTheme.Spacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            TrackMeta(track)
            LoadingTransportRow()
            AMSlider(
                value = 0f,
                onValueChange = {},
                valueRange = 0f..track.durationSeconds.toFloat(),
                leading = AMSliderDecoration.Text(musicTimeLabel(0.0)),
                trailing = AMSliderDecoration.Text("-${musicTimeLabel(track.durationSeconds)}"),
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

// ---------------------------------------------------------------------------
// Player controls — the shared band (player screen + queue sheet card)
// ---------------------------------------------------------------------------

/**
 * Track meta + transport + scrubber — the shared interior of the player screen's
 * flat band and the queue sheet's card-framed now-playing summary. Ports iOS
 * `playerControls`.
 */
@Composable
private fun PlayerControls(state: MusicState, track: MusicTrack, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AMTheme.Spacing.md.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        TrackMeta(track)
        TransportRow(state)
        Scrubber(state, track)
    }
}

/** Stacked, centered title over artist — reads as one label ([trackMetaSpacing]). */
@Composable
private fun TrackMeta(track: MusicTrack) {
    val isDark = LocalAMIsDarkTheme.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(trackMetaSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            text = track.title,
            style = TextStyle(
                color = AMTheme.Text.primary.resolve(isDark),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BasicText(
            text = track.artist,
            style = AMTheme.Type.secondary
                .toTextStyle(AMTheme.Text.secondary.resolve(isDark))
                .copy(textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** shuffle · prev · play/pause (prominent) · next · repeat. Ports iOS `transportRow`. */
@Composable
private fun TransportRow(state: MusicState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AMTheme.Spacing.lg.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AMIconButton(
            icon = Icons.Filled.Shuffle,
            active = state.isShuffling,
            contentDescription = "Shuffle",
            onClick = { state.toggleShuffle() },
        )
        Spacer(modifier = Modifier.weight(1f))
        AMIconButton(
            icon = Icons.Filled.SkipPrevious,
            enabled = state.trackIndex > 0,
            contentDescription = "Previous",
            onClick = { state.previous() },
        )
        Spacer(modifier = Modifier.weight(1f))
        AMToolbarButton(
            icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            size = AMToolbarButtonSize.Prominent,
            contentDescription = if (state.isPlaying) "Pause" else "Play",
            onClick = { state.togglePlay() },
        )
        Spacer(modifier = Modifier.weight(1f))
        AMIconButton(
            icon = Icons.Filled.SkipNext,
            enabled = state.trackIndex < state.playOrder.lastIndex || state.repeatMode == RepeatMode.All,
            contentDescription = "Next",
            onClick = { state.next() },
        )
        Spacer(modifier = Modifier.weight(1f))
        AMIconButton(
            icon = Icons.Filled.Repeat,
            active = state.repeatMode.isActive,
            badge = if (state.repeatMode == RepeatMode.One) AMBadgeContent.Count(1) else null,
            contentDescription = "Repeat",
            onClick = { state.cycleRepeat() },
        )
    }
}

/** The inert transport silhouette shown while loading — every control disabled. */
@Composable
private fun LoadingTransportRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AMTheme.Spacing.lg.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AMIconButton(icon = Icons.Filled.Shuffle, enabled = false, contentDescription = "Shuffle", onClick = {})
        Spacer(modifier = Modifier.weight(1f))
        AMIconButton(icon = Icons.Filled.SkipPrevious, enabled = false, contentDescription = "Previous", onClick = {})
        Spacer(modifier = Modifier.weight(1f))
        AMToolbarButton(
            icon = Icons.Filled.PlayArrow,
            size = AMToolbarButtonSize.Prominent,
            enabled = false,
            contentDescription = "Play",
            onClick = {},
        )
        Spacer(modifier = Modifier.weight(1f))
        AMIconButton(icon = Icons.Filled.SkipNext, enabled = false, contentDescription = "Next", onClick = {})
        Spacer(modifier = Modifier.weight(1f))
        AMIconButton(icon = Icons.Filled.Repeat, enabled = false, contentDescription = "Repeat", onClick = {})
    }
}

/** Elapsed / remaining scrubber. A drag writes `currentTime` (winning over the tick). */
@Composable
private fun Scrubber(state: MusicState, track: MusicTrack) {
    AMSlider(
        value = state.currentTime.toFloat(),
        onValueChange = { state.seek(it.toDouble()) },
        valueRange = 0f..track.durationSeconds.toFloat(),
        leading = AMSliderDecoration.Text(musicTimeLabel(state.currentTime)),
        trailing = AMSliderDecoration.Text("-${musicTimeLabel(max(0.0, track.durationSeconds - state.currentTime))}"),
    )
}

// ---------------------------------------------------------------------------
// Queue sheet — Up Next
// ---------------------------------------------------------------------------

/**
 * The full-height Up Next sheet: a header over the reorderable upcoming queue (or
 * an empty state), with the card-framed now-playing transport pinned at the
 * bottom within thumb reach. A drag writes the new order back live and raises a
 * single "Queue updated" toast when it settles. Ports iOS `queueSheet`.
 */
@Composable
private fun QueueSheet(state: MusicState) {
    val isDark = LocalAMIsDarkTheme.current
    val upNext = state.upNext

    // The reorderable list fires onReorder on every drag-over move, so applying it
    // is silent and the "Queue updated" toast fires once per settled drag instead.
    // `dragging` is reflected up from the row slot's isDragging (only one slot drags
    // at a time, and only the row that observed the drag start may write it back to
    // false — see the per-row guard below); `moved` records whether the in-flight
    // drag reordered anything. When the drag settles (dragging → false) a single
    // toast flushes if it moved.
    var dragging by remember { mutableStateOf(false) }
    var moved by remember { mutableStateOf(false) }
    LaunchedEffect(dragging) {
        if (!dragging && moved) {
            state.notifyQueueReordered()
            moved = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        BasicText(
            text = "Up Next",
            style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            modifier = Modifier.padding(top = AMTheme.Spacing.sm.dp),
        )

        if (upNext.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                AMEmptyState(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = "Nothing up next",
                )
            }
        } else {
            AMReorderableRowList(
                items = upNext,
                key = { it.uid },
                onReorder = {
                    state.reorderQueue(it)
                    moved = true
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) { item, isDragging ->
                // A LaunchedEffect runs its block on INITIAL composition regardless of
                // key, so a row composing mid-drag (the drag auto-scrolling a long
                // queue) with isDragging = false must be a no-op — only the row that
                // itself observed the drag start may report its end, or a fresh row
                // would flush the toast early and swallow the real drag-end.
                var sawDrag by remember { mutableStateOf(false) }
                LaunchedEffect(isDragging) {
                    if (isDragging) {
                        sawDrag = true
                        dragging = true
                    } else if (sawDrag) {
                        sawDrag = false
                        dragging = false
                    }
                }
                QueueRow(item.track)
            }
        }

        state.currentTrack?.let { track ->
            AMCard(modifier = Modifier.fillMaxWidth()) {
                PlayerControls(state, track)
            }
        }
    }
}

/** A queue row: cover thumb, title/artist, trailing duration. */
@Composable
private fun QueueRow(track: MusicTrack) {
    AMListRow(
        title = track.title,
        subtitle = track.artist,
        leading = { MusicCoverThumb(track.coverArt, track.albumId) },
        trailing = { MusicDurationLabel(track.durationSeconds) },
    )
}

// ---------------------------------------------------------------------------
// Overlays — long-press queue menu + FAB grid
// ---------------------------------------------------------------------------

/**
 * The long-press menu inside the actions overlay: a header naming the pressed
 * target over a `[Play, Add to Queue]` row. Ports iOS `queueTargetMenu`.
 */
@Composable
private fun QueueTargetMenu(state: MusicState) {
    val target = state.queueMenuTarget ?: return
    val catalog = state.catalog
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AMTheme.Spacing.lg.dp)
            .padding(bottom = AMTheme.Spacing.xxl.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Bottom-anchor the menu over the full-screen actions overlay (iOS parity):
        // a leading weighted spacer pushes the label + row to the bottom edge.
        Spacer(modifier = Modifier.weight(1f))

        BasicText(
            text = target.label(catalog),
            style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AMActionGridRow(
            tileA = {
                AMActionGridButton(
                    icon = Icons.Filled.PlayArrow,
                    title = "Play",
                    onClick = {
                        when (target) {
                            is MusicQueueTarget.Track -> {
                                val tracks = catalog.tracks(catalog.album(target.track.albumId))
                                state.play(tracks, tracks.indexOfFirst { it.id == target.track.id }.coerceAtLeast(0))
                            }

                            else -> state.play(target.tracks(catalog), 0)
                        }
                    },
                )
            },
            tileB = {
                AMActionGridButton(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    title = "Add to Queue",
                    onClick = { state.addToQueue(target) },
                )
            },
        )
    }
}

/**
 * The FAB grid: `[Library, Artist, Album]` (Artist/Album greyed while no track is
 * loaded) over `[Player, Queue, Exit]`. View tiles reset the panel path to their
 * destination (a stale panel would otherwise cover the chosen view). Ports iOS
 * `fabMenu`.
 */
@Composable
private fun FabMenu(
    state: MusicState,
    sceneExit: () -> Unit,
    onPresentChange: (Boolean) -> Unit,
) {
    val hasTrack = state.currentTrack != null
    AMActionGrid {
        AMActionGridRow(
            tileA = {
                AMActionGridButton(
                    icon = Icons.Filled.LibraryMusic,
                    title = "Library",
                    onClick = {
                        onPresentChange(false)
                        state.showLibrary()
                    },
                )
            },
            tileB = {
                AMActionGridButton(
                    icon = Icons.Filled.Mic,
                    title = "Artist",
                    enabled = hasTrack,
                    onClick = {
                        onPresentChange(false)
                        state.jumpToArtist()
                    },
                )
            },
            tileC = {
                AMActionGridButton(
                    icon = Icons.Filled.Album,
                    title = "Album",
                    enabled = hasTrack,
                    onClick = {
                        onPresentChange(false)
                        state.jumpToAlbum()
                    },
                )
            },
        )
        AMActionGridRow(
            tileA = {
                AMActionGridButton(
                    icon = Icons.Filled.PlayCircle,
                    title = "Player",
                    onClick = {
                        onPresentChange(false)
                        state.showPlayer()
                    },
                )
            },
            tileB = {
                AMActionGridButton(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = "Queue",
                    onClick = {
                        onPresentChange(false)
                        state.showQueue = true
                    },
                )
            },
            tileC = {
                AMActionGridButton(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Exit",
                    onClick = {
                        onPresentChange(false)
                        sceneExit()
                    },
                )
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Shared row pieces (queue sheet)
// ---------------------------------------------------------------------------

/** The leading cover thumbnail recipe for a queue row. */
@Composable
private fun MusicCoverThumb(coverArt: ImageBitmap?, albumId: String) {
    MusicCoverArt(
        coverArt = coverArt,
        palette = musicPalette(albumId),
        modifier = Modifier.size(musicCoverThumbSize),
        cornerRadius = AMTheme.Radius.sm.dp,
        glyphSize = 18.dp,
    )
}
