package com.ideamtrls.androidmtrls.demo

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.ideamtrls.androidmtrls.AMToastItem
import com.ideamtrls.androidmtrls.amInstantSwitch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.YearMonth
import kotlin.time.Duration.Companion.milliseconds

/** The scene's two root views, which the remote swaps instantly. */
internal enum class PhotosView { Library, Albums }

/**
 * A place on a root's back stack: the root view itself, then the panels it pushes. A detail
 * captures the photos it pages through when it opens; the page it shows is the model's
 * [PhotosModel.detailIndex] for its root.
 */
internal sealed interface PhotosRoute {
    /** The root view, the first entry of every back stack. */
    data object Root : PhotosRoute

    data class Album(val id: String) : PhotosRoute

    data class Detail(val ids: List<Int>) : PhotosRoute
}

/** Where select mode is choosing photos: the Library grid or one album's. */
internal sealed interface PhotosSelectScope {
    data object Library : PhotosSelectScope

    data class Album(val id: String) : PhotosSelectScope
}

/** The Library's filter chips: everything, the favorites, or one place. */
internal sealed interface PhotosFilter {
    val title: String

    fun includes(photo: PhotosItem): Boolean

    data object All : PhotosFilter {
        override val title: String get() = "All"

        override fun includes(photo: PhotosItem): Boolean = true
    }

    data object Favorites : PhotosFilter {
        override val title: String get() = "Favorites"

        override fun includes(photo: PhotosItem): Boolean = photo.isFavorite
    }

    data class Place(val place: PhotosPlace) : PhotosFilter {
        override val title: String get() = place.title

        override fun includes(photo: PhotosItem): Boolean = photo.place == place
    }
}

/** Which rows the remote shows, from where the scene stands. */
internal sealed interface PhotosMenuContext {
    data object Library : PhotosMenuContext

    data object Albums : PhotosMenuContext

    data class Album(val id: String) : PhotosMenuContext

    /** The open detail, carrying the photo it shows. */
    data class Detail(val photoId: Int) : PhotosMenuContext

    data object Selecting : PhotosMenuContext
}

/**
 * What the remote draws, captured when it opens, so its rows and tiles hold still while it fades
 * out under an action that changes the scene (Select, a batch action ending select mode).
 */
@Immutable
internal data class PhotosMenuState(
    val context: PhotosMenuContext = PhotosMenuContext.Library,
    val hasSelection: Boolean = false,
    val allFavorites: Boolean = false,
    val allSelected: Boolean = false,
    val hasShownPhotos: Boolean = false,
    val hasAlbums: Boolean = false,
)

/** One Library section: a month's photos, newest first. */
@Immutable
internal data class PhotosMonth(val month: YearMonth, val photos: List<PhotosItem>) {
    val title: String get() = photosMonthTitle(month)
}

/** The question the scene is asking before it deletes something. */
internal sealed interface PhotosAlert {
    data class DeletePhotos(val ids: List<Int>) : PhotosAlert

    data class DeleteAlbum(val id: String) : PhotosAlert
}

/**
 * Owns the Photos scene: the photos and albums, where the scene stands (the root view, each root's
 * panels, the detail's page), the Library's filter, select mode, and every overlay. A plain class
 * holding Compose state, remembered at the scene root and saved with [saver]. Every surface reads
 * and changes this one model, and the model owns the scene's copy: alert, toast and title text.
 *
 * Rules it keeps: the selection is always a subset of what the selecting surface shows (a filter
 * change or a delete drops the picks it hides, and popping an album's panel ends that album's
 * selection); every batch action ends select mode and toasts; deleting photos prunes them from
 * every album, the selection and an open detail, whose page stays put so the next photo shows (the
 * panel pops once none are left).
 *
 * Loading the library is a stand-in: a short wait as a coroutine in [scope], the scene's own, so
 * leaving the scene cancels it.
 */
@Stable
internal class PhotosModel(private val scope: CoroutineScope) {
    // Library
    var photos by mutableStateOf(PhotosSampleLibrary)
        private set
    var albums by mutableStateOf(PhotosSampleAlbums)
        private set

    /** True until the library lands; the Library shows its layout under the skeleton meanwhile. */
    var isLoading by mutableStateOf(true)
        private set

    // Navigation
    var rootView by mutableStateOf(PhotosView.Library)
    private val libraryStack = mutableStateListOf<PhotosRoute>(PhotosRoute.Root)
    private val albumsStack = mutableStateListOf<PhotosRoute>(PhotosRoute.Root)
    private var libraryDetailIndex by mutableIntStateOf(0)
    private var albumsDetailIndex by mutableIntStateOf(0)

    private var currentFilter by mutableStateOf<PhotosFilter>(PhotosFilter.All)

    /** The Library's filter. Changing it drops the picks it hides. */
    var filter: PhotosFilter
        get() = currentFilter
        set(value) {
            currentFilter = value
            pruneSelection()
        }

    // Select mode
    var selecting by mutableStateOf<PhotosSelectScope?>(null)
        private set
    var selectedIds by mutableStateOf<Set<Int>>(emptySet())
        private set

    // Overlays
    var showsAlbumSheet by mutableStateOf(false)

    /**
     * The photos the Add to album sheet adds. They outlast the sheet, which still draws them as it
     * slides away, until the next [presentAddToAlbum] replaces them.
     */
    var albumSheetTargets by mutableStateOf<List<Int>>(emptyList())
        private set

    /** The photo the Info sheet shows; null closes it. */
    var infoPhotoId by mutableStateOf<Int?>(null)
    var alert by mutableStateOf<PhotosAlert?>(null)
    var toast by mutableStateOf<AMToastItem?>(null)
    var showsMenu by mutableStateOf(false)
        private set

    /** The remote's rows and tile states, frozen when it opens. */
    var menuState by mutableStateOf(PhotosMenuState())
        private set

    /** The stand-in read of the library. */
    private var loading: Job? = null

    init {
        check(PhotosSampleLibrary.map { it.id }.toSet().size == PhotosSampleLibrary.size) { "Photo ids repeat" }
        check(PhotosSampleLibrary.map { it.imageId }.toSet().size == PhotosSampleLibrary.size) {
            "Two photos show the same picture"
        }
    }

    // Derived

    /** The chips: All, Favorites, then each place that has photos, in [PhotosPlace] order. */
    val filters: List<PhotosFilter>
        get() {
            val places = PhotosPlace.entries.filter { place -> photos.any { it.place == place } }
            return listOf(PhotosFilter.All, PhotosFilter.Favorites) + places.map(PhotosFilter::Place)
        }

    /** The most recent photo, whatever the filter: the Library's and Albums' backdrop. */
    val latestPhoto: PhotosItem?
        get() = photos.maxByOrNull { it.takenAt }

    /** What the Library shows: the filter's photos, newest first. */
    val libraryPhotos: List<PhotosItem>
        get() = photos.filter(filter::includes).sortedByDescending { it.takenAt }

    /** The Library's sections, one per month, newest first. */
    val libraryMonths: List<PhotosMonth>
        get() = libraryPhotos.groupBy { it.month }.map { (month, photos) -> PhotosMonth(month, photos) }

    /** "Select photos" / "1 selected" / "3 selected". */
    val selectionTitle: String
        get() = if (selectedIds.isEmpty()) "Select photos" else "${selectedIds.size} selected"

    /**
     * What the selecting surface shows, the Library's filtered photos or the album's, and nothing
     * outside select mode. The selection is always a subset of it.
     */
    val shownPhotos: List<PhotosItem>
        get() = when (val current = selecting) {
            PhotosSelectScope.Library -> libraryPhotos
            is PhotosSelectScope.Album -> album(current.id)?.let(::photosIn).orEmpty()
            null -> emptyList()
        }

    /** The picks in on-screen order, so every batch action acts in one stable order. */
    val orderedSelection: List<Int>
        get() = shownPhotos.map { it.id }.filter { it in selectedIds }

    val isAllShownSelected: Boolean
        get() = shownPhotos.let { shown -> shown.isNotEmpty() && shown.all { it.id in selectedIds } }

    /** True when something is selected and every pick is a favorite. */
    val selectionIsAllFavorites: Boolean
        get() = photos.filter { it.id in selectedIds }.let { chosen -> chosen.isNotEmpty() && chosen.all { it.isFavorite } }

    /** Where the remote opens from: select mode wins, then the open detail, an open album, the root. */
    val menuContext: PhotosMenuContext
        get() {
            if (selecting != null) return PhotosMenuContext.Selecting
            detailPhoto?.let { return PhotosMenuContext.Detail(it.id) }
            (stack(rootView).last() as? PhotosRoute.Album)?.let { return PhotosMenuContext.Album(it.id) }
            return if (rootView == PhotosView.Library) PhotosMenuContext.Library else PhotosMenuContext.Albums
        }

    /** The page the open detail shows, or null when no detail is on top. */
    val detailPhoto: PhotosItem?
        get() {
            val route = stack(rootView).last() as? PhotosRoute.Detail ?: return null
            val pages = route.ids.mapNotNull(::photo)
            if (pages.isEmpty()) return null
            return pages[detailIndex(rootView).coerceIn(0, pages.lastIndex)]
        }

    /** A root's back stack: the root's own entry, then its panels. */
    fun stack(view: PhotosView): SnapshotStateList<PhotosRoute> = when (view) {
        PhotosView.Library -> libraryStack
        PhotosView.Albums -> albumsStack
    }

    /** Whether [route] is the view on top of the scene: the current root's last entry. */
    fun isOnTop(view: PhotosView, route: PhotosRoute): Boolean = rootView == view && stack(view).last() == route

    /** The page a root's open detail shows, an index into its route's ids. */
    fun detailIndex(view: PhotosView): Int = when (view) {
        PhotosView.Library -> libraryDetailIndex
        PhotosView.Albums -> albumsDetailIndex
    }

    fun setDetailIndex(view: PhotosView, index: Int) {
        when (view) {
            PhotosView.Library -> libraryDetailIndex = index
            PhotosView.Albums -> albumsDetailIndex = index
        }
    }

    fun photo(id: Int): PhotosItem? = photos.firstOrNull { it.id == id }

    fun album(id: String): PhotosAlbum? = albums.firstOrNull { it.id == id }

    /** The album's photos, in album order. */
    fun photosIn(album: PhotosAlbum): List<PhotosItem> = album.photoIds.mapNotNull(::photo)

    /** The album's first photo, or null when it's empty. */
    fun cover(album: PhotosAlbum): PhotosItem? = album.photoIds.firstNotNullOfOrNull(::photo)

    // Loading

    /** Lands the library after a short wait, the stand-in for reading it. */
    fun finishLoading() {
        if (!isLoading || loading != null) return
        loading = scope.launch {
            delay(900.milliseconds)
            isLoading = false
            loading = null
        }
    }

    /** Abandons the library read; it starts again the next time the scene shows. */
    fun cancelPendingWork() {
        loading?.cancel()
        loading = null
    }

    // The remote

    /** Opens the remote, freezing what it shows. */
    fun openMenu() {
        menuState = currentMenuState()
        showsMenu = true
    }

    fun closeMenu() {
        showsMenu = false
    }

    // Navigation

    /** Pushes the detail paging [context] in the current root, opened on photo [id]. */
    fun openDetail(id: Int, context: List<PhotosItem>) {
        val index = context.indexOfFirst { it.id == id }
        if (index < 0) return
        setDetailIndex(rootView, index)
        push(PhotosRoute.Detail(context.map { it.id }))
    }

    /** Pushes album [id]'s panel in the current root. */
    fun openAlbum(id: String) {
        push(PhotosRoute.Album(id))
    }

    /** Pushes [route] on the current root's stack, unless it's already there (a double tap). */
    private fun push(route: PhotosRoute) {
        val stack = stack(rootView)
        if (route !in stack) stack.add(route)
    }

    /** Pops a root's top panel. Popping an album's panel ends a selection made in it. */
    fun pop(view: PhotosView) {
        val stack = stack(view)
        if (stack.size <= 1) return
        stack.removeAt(stack.lastIndex)
        val current = selecting
        if (current is PhotosSelectScope.Album && PhotosRoute.Album(current.id) !in stack) endSelecting()
    }

    /**
     * A root view's tile: closes the remote, then lands on [view] as it was left, with select mode
     * over, in the same frame.
     */
    fun switchTo(view: PhotosView) {
        closeMenu()
        amInstantSwitch {
            endSelecting()
            rootView = view
        }
    }

    // Select mode

    fun beginSelecting(scope: PhotosSelectScope) {
        closeMenu()
        selecting = scope
        selectedIds = emptySet()
    }

    fun endSelecting() {
        selecting = null
        selectedIds = emptySet()
    }

    fun toggleSelected(id: Int) {
        if (selecting == null) return
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    /** Selects everything shown, or clears the selection when it already is. */
    fun toggleSelectAll() {
        if (selecting == null) return
        closeMenu()
        selectedIds = if (isAllShownSelected) emptySet() else shownPhotos.map { it.id }.toSet()
    }

    // Actions

    /**
     * Unfavorites the photos when every one is already a favorite, otherwise favorites them all.
     * From select mode it's a batch: select mode ends and a toast says what changed.
     */
    fun toggleFavorite(ids: List<Int>) {
        val targets = ids.toSet()
        val chosen = photos.filter { it.id in targets }
        if (chosen.isEmpty()) return
        val favorite = !chosen.all { it.isFavorite }
        val changed = chosen.count { it.isFavorite != favorite }
        photos = photos.map { if (it.id in targets) it.copy(isFavorite = favorite) else it }
        closeMenu()
        if (selecting == null) return
        endSelecting()
        toast = AMToastItem(
            if (favorite) {
                "Added ${photosCountLabel(changed)} to Favorites"
            } else {
                "Removed ${photosCountLabel(changed)} from Favorites"
            },
        )
    }

    /** Opens the Add to album sheet for [ids]. */
    fun presentAddToAlbum(ids: List<Int>) {
        if (ids.isEmpty()) return
        closeMenu()
        albumSheetTargets = ids
        showsAlbumSheet = true
    }

    /** Adds the sheet's photos to the album, skipping any already in it, then closes the sheet and toasts. */
    fun add(toAlbum: String) {
        val album = album(toAlbum) ?: return
        if (albumSheetTargets.isEmpty()) return
        val added = albumSheetTargets.distinct().filter { it !in album.photoIds }
        albums = albums.map { if (it.id == toAlbum) it.copy(photoIds = it.photoIds + added) else it }
        toast = AMToastItem(
            when {
                added.isEmpty() -> "Already in ${album.name}"
                albumSheetTargets.size == 1 -> "Added to ${album.name}"
                else -> "Added ${photosCountLabel(added.size)} to ${album.name}"
            },
        )
        showsAlbumSheet = false
        if (selecting != null) endSelecting()
    }

    /** Asks before deleting photos from the library and every album. */
    fun confirmDelete(ids: List<Int>) {
        if (ids.isEmpty()) return
        closeMenu()
        alert = PhotosAlert.DeletePhotos(ids)
    }

    /** Asks before deleting an album; its photos stay in the library. */
    fun confirmDeleteAlbum(id: String) {
        if (album(id) == null) return
        closeMenu()
        alert = PhotosAlert.DeleteAlbum(id)
    }

    /** Moves an album one step, as a drag passes a neighbor or TalkBack's Move up and Move down ask. */
    fun moveAlbum(from: Int, to: Int) {
        if (from !in albums.indices || to !in albums.indices || from == to) return
        albums = albums.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** Opens the Info sheet for a photo. */
    fun showInfo(id: Int) {
        if (photo(id) == null) return
        closeMenu()
        infoPhotoId = id
    }

    /**
     * Removes photos from the library, every album, the selection and an open detail, whose page
     * stays put so it shows the next photo, or pops once none are left.
     */
    fun delete(ids: List<Int>) {
        val removed = ids.toSet().intersect(photos.map { it.id }.toSet())
        if (removed.isEmpty()) return
        photos = photos.filter { it.id !in removed }
        albums = albums.map { album -> album.copy(photoIds = album.photoIds.filter { it !in removed }) }
        selectedIds = selectedIds - removed
        if (infoPhotoId?.let { it in removed } == true) infoPhotoId = null
        PhotosView.entries.forEach { view ->
            val stack = stack(view)
            val route = stack.last() as? PhotosRoute.Detail ?: return@forEach
            val remaining = route.ids.count { photo(it) != null }
            if (remaining == 0) {
                stack.removeAt(stack.lastIndex)
            } else {
                setDetailIndex(view, detailIndex(view).coerceAtMost(remaining - 1))
            }
        }
        // A place whose last photo went loses its chip, so its filter lets go.
        if (filter !in filters) filter = PhotosFilter.All
        if (selecting != null) endSelecting()
        toast = AMToastItem(if (removed.size == 1) "Photo deleted" else "${photosCountLabel(removed.size)} deleted")
    }

    /** Removes the album and pops its panel; its photos stay in the library. */
    fun deleteAlbum(id: String) {
        albums = albums.filter { it.id != id }
        PhotosView.entries.forEach { view ->
            val stack = stack(view)
            val index = stack.indexOf(PhotosRoute.Album(id))
            if (index > 0) {
                while (stack.size > index) stack.removeAt(stack.lastIndex)
            }
        }
        if (selecting == PhotosSelectScope.Album(id)) endSelecting()
        toast = AMToastItem("Album deleted")
    }

    // Private

    private fun currentMenuState(): PhotosMenuState {
        val context = menuContext
        return PhotosMenuState(
            context = context,
            hasSelection = selectedIds.isNotEmpty(),
            allFavorites = if (context is PhotosMenuContext.Detail) detailPhoto?.isFavorite == true else selectionIsAllFavorites,
            allSelected = isAllShownSelected,
            hasShownPhotos = photosOnScreen(context).isNotEmpty(),
            hasAlbums = albums.isNotEmpty(),
        )
    }

    /**
     * The photos the remote's context puts on screen, for the tiles that grey with nothing shown:
     * the selecting surface's, the open album's, or the Library's.
     */
    private fun photosOnScreen(context: PhotosMenuContext): List<PhotosItem> = when (context) {
        PhotosMenuContext.Selecting -> shownPhotos
        is PhotosMenuContext.Album -> album(context.id)?.let(::photosIn).orEmpty()
        else -> libraryPhotos
    }

    /** Keeps the selection a subset of what the selecting surface shows. */
    private fun pruneSelection() {
        if (selecting == null) return
        val shown = shownPhotos.map { it.id }.toSet()
        selectedIds = selectedIds intersect shown
    }

    companion object {
        /**
         * Saves the library as it stands (which photos remain and which are favorites, the albums'
         * order and contents), where the scene stands (the root, both back stacks and detail pages),
         * the filter, select mode and which overlay is up; restores them into a model on the
         * restored scene's [scope]. The library read in flight isn't kept: it starts over.
         */
        fun saver(scope: CoroutineScope): Saver<PhotosModel, Any> = mapSaver(
            save = { model ->
                mapOf(
                    "loaded" to !model.isLoading,
                    "photos" to ArrayList(model.photos.map { it.id }),
                    "favorites" to ArrayList(model.photos.filter { it.isFavorite }.map { it.id }),
                    "albums" to ArrayList(model.albums.map { album -> album.id + ":" + album.photoIds.joinToString(",") }),
                    "root" to model.rootView.name,
                    "libraryStack" to ArrayList(model.libraryStack.map(::encodeRoute)),
                    "albumsStack" to ArrayList(model.albumsStack.map(::encodeRoute)),
                    "libraryIndex" to model.libraryDetailIndex,
                    "albumsIndex" to model.albumsDetailIndex,
                    "filter" to encodeFilter(model.filter),
                    "selecting" to when (val selecting = model.selecting) {
                        PhotosSelectScope.Library -> "library"
                        is PhotosSelectScope.Album -> "album:${selecting.id}"
                        null -> ""
                    },
                    "selected" to ArrayList(model.selectedIds),
                    "showsAlbumSheet" to model.showsAlbumSheet,
                    "albumSheetTargets" to ArrayList(model.albumSheetTargets),
                    "info" to (model.infoPhotoId ?: -1),
                    "alert" to when (val alert = model.alert) {
                        is PhotosAlert.DeletePhotos -> "photos:" + alert.ids.joinToString(",")
                        is PhotosAlert.DeleteAlbum -> "album:${alert.id}"
                        null -> ""
                    },
                    "showsMenu" to model.showsMenu,
                )
            },
            restore = { saved ->
                PhotosModel(scope).apply {
                    isLoading = saved["loaded"] != true
                    val present = ints(saved["photos"]).toSet()
                    val favorites = ints(saved["favorites"]).toSet()
                    photos = PhotosSampleLibrary.filter { it.id in present }.map { it.copy(isFavorite = it.id in favorites) }
                    albums = strings(saved["albums"]).mapNotNull { encoded ->
                        val id = encoded.substringBefore(":")
                        val sample = PhotosSampleAlbums.firstOrNull { it.id == id } ?: return@mapNotNull null
                        sample.copy(photoIds = idList(encoded.substringAfter(":")))
                    }
                    rootView = PhotosView.valueOf(saved["root"] as String)
                    restoreStack(libraryStack, strings(saved["libraryStack"]))
                    restoreStack(albumsStack, strings(saved["albumsStack"]))
                    libraryDetailIndex = saved["libraryIndex"] as Int
                    albumsDetailIndex = saved["albumsIndex"] as Int
                    currentFilter = decodeFilter(saved["filter"] as String)
                    val selectingCode = saved["selecting"] as String
                    selecting = when {
                        selectingCode == "library" -> PhotosSelectScope.Library
                        selectingCode.startsWith("album:") -> PhotosSelectScope.Album(selectingCode.removePrefix("album:"))
                        else -> null
                    }
                    selectedIds = ints(saved["selected"]).toSet()
                    showsAlbumSheet = saved["showsAlbumSheet"] as Boolean
                    albumSheetTargets = ints(saved["albumSheetTargets"])
                    infoPhotoId = (saved["info"] as Int).takeIf { it >= 0 }
                    val alertCode = saved["alert"] as String
                    alert = when {
                        alertCode.startsWith("photos:") -> PhotosAlert.DeletePhotos(idList(alertCode.removePrefix("photos:")))
                        alertCode.startsWith("album:") -> PhotosAlert.DeleteAlbum(alertCode.removePrefix("album:"))
                        else -> null
                    }
                    if (saved["showsMenu"] == true) openMenu()
                }
            },
        )

        private fun encodeRoute(route: PhotosRoute): String = when (route) {
            PhotosRoute.Root -> "root"
            is PhotosRoute.Album -> "album:${route.id}"
            is PhotosRoute.Detail -> "detail:" + route.ids.joinToString(",")
        }

        private fun decodeRoute(code: String): PhotosRoute? = when {
            code.startsWith("album:") -> PhotosRoute.Album(code.removePrefix("album:"))
            code.startsWith("detail:") -> PhotosRoute.Detail(idList(code.removePrefix("detail:")))
            else -> null
        }

        /** Puts back what a stack held over its root entry. */
        private fun restoreStack(stack: SnapshotStateList<PhotosRoute>, codes: List<String>) {
            stack.addAll(codes.mapNotNull(::decodeRoute))
        }

        private fun encodeFilter(filter: PhotosFilter): String = when (filter) {
            PhotosFilter.All -> "all"
            PhotosFilter.Favorites -> "favorites"
            is PhotosFilter.Place -> "place:${filter.place.name}"
        }

        private fun decodeFilter(code: String): PhotosFilter = when {
            code == "favorites" -> PhotosFilter.Favorites
            code.startsWith("place:") -> PhotosFilter.Place(PhotosPlace.valueOf(code.removePrefix("place:")))
            else -> PhotosFilter.All
        }

        private fun ints(value: Any?): List<Int> = (value as? List<*>).orEmpty().filterIsInstance<Int>()

        private fun strings(value: Any?): List<String> = (value as? List<*>).orEmpty().filterIsInstance<String>()

        private fun idList(code: String): List<Int> = code.split(",").mapNotNull(String::toIntOrNull)
    }
}
