package io.github.jmsdnns.androidmtrls.demo.scenes.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGrid
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridRow
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.scenes.common.LocalSceneExit
import io.github.jmsdnns.androidmtrls.demo.scenes.common.ScenePanelChrome
import io.github.jmsdnns.androidmtrls.demo.scenes.common.ScenePanelHost
import io.github.jmsdnns.androidmtrls.demo.scenes.common.sceneFabClearance
import io.github.jmsdnns.androidmtrls.demo.scenes.common.sceneFabLift
import io.github.jmsdnns.androidmtrls.lists.AMListRow
import io.github.jmsdnns.androidmtrls.lists.AMReorderableRowList
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMButtonRole
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMToolbar
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMToolbarButton
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMAlert
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMAlertAction
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMAlertItem
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMFloatingActionsHost
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMSheet
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMSheetHeight
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMToast
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMToastItem
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for the [GalleryScene] demo (Screens section). id `scene-gallery`.
 * Renders full-screen with no `ComponentPage` scaffold — the scene owns its
 * chrome and its Exit affordance ([LocalSceneExit]). Mirrors iOS `GalleryScene`
 * registration.
 */
val gallerySceneEntry: ComponentEntry = ComponentEntry(
    id = "scene-gallery",
    section = ComponentSection.Screens,
    title = "Gallery",
    subtitle = "Browse, favorite, and organize photos",
    icon = Icons.Filled.PhotoLibrary,
    content = { GalleryScene() },
)

// ---------------------------------------------------------------------------
// Navigation model
// ---------------------------------------------------------------------------

/** The two swappable scene roots. Mirrors iOS `GalleryScene.GalleryView`. */
enum class GalleryView { Library, Albums }

/**
 * A pushed panel in the Gallery scene's state-driven navigation stack. Drives
 * [GalleryState.path] through `ScenePanelHost`. Mirrors iOS `GalleryRoute`.
 */
sealed interface GalleryRoute {
    /** The album-grid panel, keyed by album name. */
    data class Album(val name: String) : GalleryRoute

    /**
     * The tabless detail panel. [ids] captures the photo ids the carousel pages
     * through at push time; the live page rides [GalleryState.detailIndex].
     */
    data class Detail(val ids: List<Int>) : GalleryRoute
}

// ---------------------------------------------------------------------------
// GalleryState — the plain @Stable state holder (no ViewModel)
// ---------------------------------------------------------------------------

/**
 * The single state holder driving the whole Gallery scene — the library and
 * albums roots, the pushed album-grid and detail panels, select mode (photos on
 * the library, whole albums on the albums screen), add-to-album, delete, and the
 * feedback overlays. Remembered once at the scene root (no `ViewModel`),
 * mirroring the iOS `@State`-driven `GalleryScene`.
 *
 * Favorites is never stored — [favoritePhotos] derives it from
 * [GalleryPhoto.isFavorite] so it can never drift out of sync.
 */
@Stable
class GalleryState {
    // Data
    val photos = mutableStateListOf<GalleryPhoto>().apply { addAll(galleryPhotos) }
    val albums = mutableStateListOf<GalleryAlbum>().apply { addAll(galleryAlbums) }

    // Lifecycle
    var isLoading by mutableStateOf(true)
    private var hasLoaded = false

    // Navigation
    var currentView by mutableStateOf(GalleryView.Library)

    /** State-driven stack of pushed panels (album grid, detail), owned here. */
    val path = mutableStateListOf<GalleryRoute>()

    /** Live carousel page for the detail panel; the panel reads/writes it. */
    var detailIndex by mutableIntStateOf(0)

    // Select mode — photos on the library, whole albums on the albums screen.
    var isSelecting by mutableStateOf(false)
    var selectedIDs by mutableStateOf<Set<Int>>(emptySet())
    var selectedAlbumNames by mutableStateOf<Set<String>>(emptySet())

    // Add-to-album sheet
    var showingAlbumSheet by mutableStateOf(false)
    private var albumTargetIDs: List<Int> = emptyList()

    // Feedback overlays
    var toast by mutableStateOf<AMToastItem?>(null)
    var alert by mutableStateOf<AMAlertItem?>(null)

    // ── Derived state ─────────────────────────────────────────────────────────

    /** Favorites — derived, never stored. */
    val favoritePhotos: List<GalleryPhoto>
        get() = photos.filter { it.isFavorite }

    /**
     * The library's photos in month order — grouping iterates [galleryMonths],
     * so the detail carousel context follows the on-screen order.
     */
    private val libraryOrderedPhotos: List<GalleryPhoto>
        get() = galleryMonths.flatMap { month -> photos.filter { it.month == month } }

    /**
     * The live selection is photos on the library and whole albums on the albums
     * screen — [currentView] decides which set the toolbar reports on.
     */
    val selectionCount: Int
        get() = if (currentView == GalleryView.Albums) selectedAlbumNames.size else selectedIDs.size

    val isSelectionEmpty: Boolean
        get() = if (currentView == GalleryView.Albums) selectedAlbumNames.isEmpty() else selectedIDs.isEmpty()

    fun photosInAlbum(name: String): List<GalleryPhoto> {
        val album = albums.firstOrNull { it.name == name } ?: return emptyList()
        return album.photoIDs.mapNotNull { id -> photos.firstOrNull { it.id == id } }
    }

    /** Cover for an album card: the first resolvable member photo, or null. */
    fun coverPhoto(album: GalleryAlbum): GalleryPhoto? =
        album.photoIDs.firstNotNullOfOrNull { id -> photos.firstOrNull { it.id == id } }

    /** The photo the detail carousel is currently showing, from the captured ids. */
    private fun currentDetailPhotoId(ids: List<Int>): Int? {
        if (ids.isEmpty()) return null
        return ids[detailIndex.coerceIn(0, ids.lastIndex)]
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** One-shot 0.9s first load — mirrors iOS `.task`. */
    suspend fun load() {
        if (hasLoaded) return
        hasLoaded = true
        delay(900)
        isLoading = false
    }

    /** Pull-to-refresh: reshuffle the library immediately (months never move —
     * grouping iterates [galleryMonths]); mirrors the iOS scene's instant shuffle. */
    suspend fun refresh() {
        val shuffled = photos.shuffled()
        photos.clear()
        photos.addAll(shuffled)
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun openAlbum(name: String) {
        path.add(GalleryRoute.Album(name))
    }

    /** Pushes the detail panel paging [context], opened on [id]. */
    fun openDetail(context: List<GalleryPhoto>, id: Int) {
        val idx = context.indexOfFirst { it.id == id }
        if (idx < 0) return
        detailIndex = idx
        path.add(GalleryRoute.Detail(context.map { it.id }))
    }

    fun pop() {
        if (path.isNotEmpty()) path.removeAt(path.lastIndex)
    }

    /**
     * A nav-tile root switch: lands on [view] and, tab-bar style, tears the panel
     * stack back to the root and exits select mode. Plain state writes.
     */
    fun switchTo(view: GalleryView) {
        path.clear()
        exitSelectMode()
        currentView = view
    }

    // ── Library / album taps ────────────────────────────────────────────────────

    fun handleLibraryTap(photo: GalleryPhoto) {
        if (isSelecting) {
            selectedIDs = if (selectedIDs.contains(photo.id)) {
                selectedIDs - photo.id
            } else {
                selectedIDs + photo.id
            }
        } else {
            openDetail(libraryOrderedPhotos, photo.id)
        }
    }

    fun toggleAlbumSelection(name: String) {
        selectedAlbumNames = if (selectedAlbumNames.contains(name)) {
            selectedAlbumNames - name
        } else {
            selectedAlbumNames + name
        }
    }

    // ── Favorite ────────────────────────────────────────────────────────────────

    fun toggleFavorite(id: Int) {
        val idx = photos.indexOfFirst { it.id == id }
        if (idx >= 0) photos[idx] = photos[idx].copy(isFavorite = !photos[idx].isFavorite)
    }

    // ── Select mode ───────────────────────────────────────────────────────────────

    fun exitSelectMode() {
        isSelecting = false
        selectedIDs = emptySet()
        selectedAlbumNames = emptySet()
    }

    // ── Add to album ────────────────────────────────────────────────────────────

    fun presentAlbumSheet(ids: List<Int>) {
        if (ids.isEmpty()) return
        albumTargetIDs = ids
        showingAlbumSheet = true
    }

    /**
     * Adds the pending targets to the named album, skipping ids already in it
     * (idempotent), then confirms with a toast. Reordering-in-place order matters,
     * so members are appended.
     */
    fun addTargets(albumName: String) {
        val idx = albums.indexOfFirst { it.name == albumName }
        if (idx < 0) return
        val existing = albums[idx].photoIDs
        val newIDs = albumTargetIDs.filter { it !in existing }
        albums[idx] = albums[idx].copy(photoIDs = existing + newIDs)
        showingAlbumSheet = false
        toast = AMToastItem(text = "Added to $albumName", icon = Icons.Filled.CheckCircle)
        if (isSelecting) exitSelectMode()
    }

    /** Album-picker reorder (the reorderable rows write the full list back). */
    fun reorderAlbums(reordered: List<GalleryAlbum>) {
        // Snapshot first: a no-op move can hand back the same `albums` reference,
        // and clearing it before addAll would then wipe the list.
        val next = reordered.toList()
        albums.clear()
        albums.addAll(next)
    }

    // ── Delete ──────────────────────────────────────────────────────────────────

    /** Routes the batch trash button to the confirm for whichever selection is live. */
    fun confirmDeleteSelection() {
        if (currentView == GalleryView.Albums) {
            confirmDeleteAlbums(selectedAlbumNames.toList())
        } else {
            confirmDelete(selectedIDs.toList())
        }
    }

    fun confirmDelete(ids: List<Int>) {
        if (ids.isEmpty()) return
        val count = ids.size
        alert = AMAlertItem(
            title = if (count == 1) "Delete Photo?" else "Delete $count Photos?",
            message = "This removes ${if (count == 1) "it" else "them"} from your library and all albums.",
            confirm = AMAlertAction(label = "Delete", role = AMButtonRole.Destructive) { performDelete(ids) },
            cancel = AMAlertAction(label = "Cancel"),
        )
    }

    /**
     * Genuinely removes photos: from the library, from every album's membership,
     * and — when the detail panel is open — from its carousel, clamping the page
     * or popping the panel once it empties.
     */
    private fun performDelete(ids: List<Int>) {
        val removed = ids.toSet()
        photos.removeAll { it.id in removed }
        for (i in albums.indices) {
            albums[i] = albums[i].copy(photoIDs = albums[i].photoIDs.filterNot { it in removed })
        }
        val top = path.lastOrNull()
        if (top is GalleryRoute.Detail) {
            val liveIDs = top.ids.filter { id -> photos.any { it.id == id } }
            if (liveIDs.isEmpty()) {
                path.removeAt(path.lastIndex)
            } else {
                detailIndex = detailIndex.coerceAtMost(liveIDs.lastIndex)
            }
        }
        if (isSelecting) exitSelectMode()
    }

    fun confirmDeleteAlbums(names: List<String>) {
        if (names.isEmpty()) return
        val count = names.size
        alert = AMAlertItem(
            title = if (count == 1) "Delete Album?" else "Delete $count Albums?",
            message = if (count == 1) {
                "This removes the album. Its photos stay in your library."
            } else {
                "This removes the albums. Their photos stay in your library."
            },
            confirm = AMAlertAction(label = "Delete", role = AMButtonRole.Destructive) { performDeleteAlbums(names) },
            cancel = AMAlertAction(label = "Cancel"),
        )
    }

    /** Removes the named albums; the photos are untouched (they live on [photos]). */
    private fun performDeleteAlbums(names: List<String>) {
        albums.removeAll { it.name in names }
        if (isSelecting) exitSelectMode()
    }

    /** The captured detail ids for the currently-open detail panel, or null. */
    fun currentDetailPhotoIdForMenu(): Int? {
        val top = path.lastOrNull()
        return if (top is GalleryRoute.Detail) currentDetailPhotoId(top.ids) else null
    }
}

// ---------------------------------------------------------------------------
// GalleryScene — the photo-management scene
// ---------------------------------------------------------------------------

/**
 * Gallery — a photo-management scene composed from AndroidMtrls primitives. One
 * remembered [GalleryState] drives the library / albums roots, the pushed
 * album-grid and detail panels, select mode, add-to-album, delete, and the
 * feedback overlays. Ports iOS `GalleryScene`.
 *
 * **Overlay host nesting** (outermost → innermost):
 * `AMToast → AMAlert → AMSheet(albumPicker) → AMFloatingActionsHost →
 * ScenePanelHost`. The sheet is an ancestor of the panel host so it covers pushed
 * panels; the FAB (innermost) fades under the toast, alert, sheet, or its own menu
 * via `LocalAMOverlayActive`, and rises above the batch toolbar in select mode via
 * [AMFloatingActionsHost.bottomInset]. Mirrors iOS attaching the overlays on the
 * navigation-stack container.
 */
@Composable
fun GalleryScene() {
    val state = remember { GalleryState() }
    val sceneExit = LocalSceneExit.current

    var showActions by remember { mutableStateOf(false) }
    val fabOffset = remember { mutableStateOf(Offset.Zero) }

    // One-shot 0.9s first load.
    LaunchedEffect(Unit) { state.load() }

    // ── Host stack: toast → alert → sheet → FAB → body ──
    AMToast(
        item = state.toast,
        onDismiss = { state.toast = null },
    ) {
        AMAlert(
            item = state.alert,
            onDismiss = { state.alert = null },
        ) {
            AMSheet(
                isPresented = state.showingAlbumSheet,
                onDismiss = { state.showingAlbumSheet = false },
                height = AMSheetHeight.Full,
                sheetContent = { AlbumPickerSheet(state) },
            ) {
                AMFloatingActionsHost(
                    isPresented = showActions,
                    onPresentChange = { showActions = it },
                    bottomInset = if (state.isSelecting) sceneFabClearance() else sceneFabLift(),
                    offset = fabOffset,
                    accessibilityLabel = "Actions",
                    menu = { FabMenu(state, sceneExit) { showActions = it } },
                ) {
                    SceneBody(state)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Scene body — panel stack over the library/albums root
// ---------------------------------------------------------------------------

@Composable
private fun SceneBody(state: GalleryState) {
    ScenePanelHost(
        path = state.path,
        onPop = { state.pop() },
        root = { RootContent(state) },
        panel = { route ->
            when (route) {
                is GalleryRoute.Album -> {
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        ScenePanelChrome(title = route.name, onBack = { state.pop() })
                        GalleryAlbumGridView(state, route.name)
                    }
                }

                is GalleryRoute.Detail -> {
                    GalleryDetailPanel(state, route.ids)
                }
            }
        },
    )
}

/**
 * The library/albums root plus the select toolbar. The FAB and every overlay
 * attach on the scene container (see [GalleryScene]), so they float above pushed
 * panels too. The select toolbar sits in the root, so a panel covers it — and
 * select mode is only reachable at the root, never with a panel open.
 */
@Composable
private fun RootContent(state: GalleryState) {
    val isDark = LocalAMIsDarkTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AMTheme.Surface.page.resolve(isDark)),
    ) {
        when (state.currentView) {
            GalleryView.Library -> GalleryLibraryView(state)
            GalleryView.Albums -> GalleryAlbumsView(state)
        }

        if (state.isSelecting) {
            SelectToolbar(
                state = state,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// SelectToolbar — the batch-action bar shown while selecting
// ---------------------------------------------------------------------------

/**
 * The batch-action bar shown while selecting. Its title counts the live selection
 * for whichever kind of select mode is active (photos on the library, albums on
 * the albums screen). Add-to-album applies to photo selection only, so it appears
 * for library selection alone; the trash button deletes whichever selection is
 * live; cancel exits select mode. Each chip greys via `enabled` — no hand-rolled
 * opacity. Ports iOS `GalleryScene.selectToolbar`.
 */
@Composable
private fun SelectToolbar(state: GalleryState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = AMTheme.Spacing.lg.dp, vertical = AMTheme.Spacing.lg.dp),
    ) {
        AMToolbar(
            title = "${state.selectionCount} selected",
            leading = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.currentView == GalleryView.Library) {
                        AMToolbarButton(
                            icon = Icons.Filled.AddToPhotos,
                            enabled = state.selectedIDs.isNotEmpty(),
                            contentDescription = "Add to album",
                            onClick = { state.presentAlbumSheet(state.selectedIDs.toList()) },
                        )
                    }
                    AMToolbarButton(
                        icon = Icons.Filled.Delete,
                        role = AMButtonRole.Destructive,
                        enabled = !state.isSelectionEmpty,
                        contentDescription = "Delete selected",
                        onClick = { state.confirmDeleteSelection() },
                    )
                }
            },
            trailing = {
                AMToolbarButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Cancel selection",
                    onClick = { state.exitSelectMode() },
                )
            },
        )
    }
}

// ---------------------------------------------------------------------------
// FAB menu — route-contextual actions over the [Library, Albums, Exit] nav row
// ---------------------------------------------------------------------------

/**
 * The FAB's actions grid, contextual to where the FAB floats (it is on top of
 * everything, so it is reachable inside panels too). The bottom row is always
 * `[Library, Albums, Exit]`; above it, the detail panel gets `[Add to Album,
 * Delete]` for the photo the carousel is showing, the root gets the current
 * view's `[Select, Delete]` (Select greys while already selecting, Delete greys
 * until items are selected), and an open album gets the nav row alone. Ports iOS
 * `GalleryScene.fabMenu`.
 */
@Composable
private fun FabMenu(
    state: GalleryState,
    sceneExit: () -> Unit,
    onPresentChange: (Boolean) -> Unit,
) {
    AMActionGrid {
        val top = state.path.lastOrNull()
        when {
            top is GalleryRoute.Detail -> {
                AMActionGridRow(
                    tileA = {
                        AMActionGridButton(
                            icon = Icons.Filled.AddToPhotos,
                            title = "Add to Album",
                            onClick = {
                                onPresentChange(false)
                                state.currentDetailPhotoIdForMenu()?.let { state.presentAlbumSheet(listOf(it)) }
                            },
                        )
                    },
                    tileB = {
                        AMActionGridButton(
                            icon = Icons.Filled.Delete,
                            title = "Delete",
                            role = AMButtonRole.Destructive,
                            onClick = {
                                onPresentChange(false)
                                state.currentDetailPhotoIdForMenu()?.let { state.confirmDelete(listOf(it)) }
                            },
                        )
                    },
                )
            }

            state.path.isEmpty() -> {
                AMActionGridRow(
                    tileA = {
                        AMActionGridButton(
                            icon = Icons.Filled.CheckCircle,
                            title = "Select",
                            enabled = !state.isSelecting,
                            onClick = {
                                onPresentChange(false)
                                state.isSelecting = true
                            },
                        )
                    },
                    tileB = {
                        AMActionGridButton(
                            icon = Icons.Filled.Delete,
                            title = "Delete",
                            role = AMButtonRole.Destructive,
                            enabled = !state.isSelectionEmpty,
                            onClick = {
                                onPresentChange(false)
                                state.confirmDeleteSelection()
                            },
                        )
                    },
                )
            }
            // Open album panel → nav row alone.
        }

        AMActionGridRow(
            tileA = {
                AMActionGridButton(
                    icon = Icons.Filled.PhotoLibrary,
                    title = "Library",
                    onClick = {
                        state.switchTo(GalleryView.Library)
                        onPresentChange(false)
                    },
                )
            },
            tileB = {
                AMActionGridButton(
                    icon = Icons.Filled.Collections,
                    title = "Albums",
                    onClick = {
                        state.switchTo(GalleryView.Albums)
                        onPresentChange(false)
                    },
                )
            },
            tileC = {
                AMActionGridButton(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Exit",
                    onClick = { sceneExit() },
                )
            },
        )
    }
}

// ---------------------------------------------------------------------------
// AlbumPickerSheet — the add-to-album row picker
// ---------------------------------------------------------------------------

/**
 * The add-to-album sheet: a title over the album rows. Each row taps to add the
 * pending selection to that album (idempotent) and confirms with a toast; a
 * long-press reorders the albums (an [AMReorderableRowList], mirroring iOS's
 * `IMReorderableRowList`). Ports iOS `GalleryScene.albumSheet`.
 */
@Composable
private fun AlbumPickerSheet(state: GalleryState) {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        BasicText(
            text = "Add to Album",
            style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            modifier = Modifier.padding(top = AMTheme.Spacing.sm.dp),
        )

        AMReorderableRowList(
            items = state.albums,
            key = { it.name },
            onReorder = { state.reorderAlbums(it) },
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { album, _ ->
            val interaction = remember(album.name) { MutableInteractionSource() }
            AMListRow(
                title = album.name,
                subtitle = galleryPhotoCount(album.photoIDs.size),
                modifier = Modifier.clickable(
                    interactionSource = interaction,
                    indication = amRipple(),
                    onClick = { state.addTargets(album.name) },
                ),
                leading = {
                    Image(
                        imageVector = Icons.Filled.Collections,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(AMTheme.Icon.muted.resolve(isDark)),
                        modifier = Modifier.size(24.dp),
                    )
                },
            )
        }
    }
}
