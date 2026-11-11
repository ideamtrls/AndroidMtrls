---
scope: The demo's Photos scene — the library grid and filters, select mode and batch actions, albums, the detail carousel, and the Info and Add to album sheets — its model, samples, surfaces and copy
globs: [demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/scenes/photos/**]
keywords: [Photos, photosScene, PhotosSceneEntry, PhotosScene, PhotosModel, PhotosView, PhotosRoute, PhotosSelectScope, PhotosFilter, PhotosMenuContext, PhotosMenuState, PhotosMonth, PhotosAlert, PhotosItem, PhotosPlace, PhotosAlbum, PhotosSampleLibrary, PhotosSampleAlbums, PhotosMenu, PhotosLibrary, PhotosAlbums, PhotosAlbumPanel, PhotosAlbumCover, PhotosLastSeen, PhotosDetail, PhotosSheets, PhotosPageBackdrop, PhotosSelectBackHandler, PhotosCountLine, photosTiles, photosGridPadding, PhotosGridColumns, photosMonthTitle, photosCountLabel, PhotosFabClearance, library, grid, LazyVerticalGrid, filters, AMChipGroup, select mode, amSelectionCheck, batch actions, albums, AMReorderableCardList, AMListCard, frosted, detail, AMCarousel, AMPanel, AMPanelChrome, AMPageHeader, AMRemoteImage, amPausedBackdrop, amCrossfade, page backdrop, amPageBackdrop, AMBackdrop, Info sheet, Add to album, AMSheet, AMListRow, AMLabeledContent, AMAlert, AMToast, skeleton, AMSkeleton, shimmer, NavDisplay, NavigationBackHandler]
updated: 2026-11-10
---

# Photos scene

A photo library: the grid and its filters, select mode and batch actions, albums, the detail carousel, and the Info and Add to album sheets, over a backdrop of the latest photo with frosted cards. Its entry is `photosScene` ("Photos", "Browse, favorite, and organize photos", `ic_photo_library`), third in `ShowcasePages.entries`. Two roots, Library (the landing view) and Albums, each push their album and detail panels onto their own back stack. The shared scene rules are in `DemoScenes.md`.

## Where things live

`demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/scenes/photos/`, one file per surface:

| File | What |
|---|---|
| `PhotosScene.kt` | `PhotosSceneEntry`, `PhotosScene()` (the root), `PhotosFabClearance`; private `PhotosRoot` (a root's `NavDisplay`), `PhotosAlerts` |
| `PhotosData.kt` | `PhotosPlace`, `PhotosItem`, `PhotosAlbum`, `photosMonthTitle`, `photosCountLabel`, `PhotosSampleLibrary`, `PhotosSampleAlbums` |
| `PhotosModel.kt` | `PhotosView`, `PhotosRoute`, `PhotosSelectScope`, `PhotosFilter`, `PhotosMenuContext`, `PhotosMenuState`, `PhotosMonth`, `PhotosAlert`, `PhotosModel` and its `saver` |
| `PhotosMenu.kt` | `PhotosMenu` (the remote's grid); private `PhotosViewsRow`, `PhotosViewTile`, `PhotosFavoriteTile`, `PhotosExitTile` |
| `PhotosLibrary.kt` | `PhotosLibrary`, the shared grid pieces (`PhotosGridColumns`, `photosGridPadding`, `PhotosCountLine`, `LazyGridScope.photosTiles`), `PhotosPageBackdrop`, `PhotosSelectBackHandler`; private `PhotosTile` |
| `PhotosAlbums.kt` | `PhotosAlbums`, `PhotosAlbumCover`, `PhotosAlbumPanel`, `PhotosLastSeen`; private `photosAlbumCountLabel`, `PhotosAlbumCard`, `PhotosHeldSelection` |
| `PhotosDetail.kt` | `PhotosDetail`; private `PhotosDetailBackdrop`, `PhotosCaption`, `PhotosHeldPages` |
| `PhotosSheets.kt` | `PhotosSheets` (both sheets); private `PhotosAlbumSheet`, `PhotosInfoSheet` |

Glyphs it adds to the demo's `res/drawable/`: `ic_photo_library` (the entry, the Library tile, the "No photos" state) and `ic_check_circle_filled` (Select all). It also uses `ic_photo_album`, `ic_check_circle`, `ic_check`, `ic_info`, `ic_favorite`, `ic_favorite_filled`, `ic_library_add`, `ic_delete`, `ic_logout`, and `ic_light_mode` / `ic_dark_mode` through `SceneAppearanceButton`.

## The root — `PhotosScene()`

- `rememberCoroutineScope()`, then the model: `rememberSaveable(saver = PhotosModel.saver(scope)) { PhotosModel(scope) }`. A `LaunchedEffect` starts the library read; a `DisposableEffect` cancels it when the scene leaves the composition.
- **The roots form of `AMScene`**: `current = model.rootView`, `landing = Library`, `backStack = { model.stack(it) }`, the remote in `floatingActions` (`AMFloatingActions`, "Actions", expanded while `model.showsMenu`; opening calls `openMenu()`, which freezes the menu). Each root is `PhotosRoot`: a Navigation 3 `NavDisplay` over that root's `SnapshotStateList<PhotosRoute>` (`Root`, then `Album(id)` / `Detail(ids)` panels), `onBack` = `model.pop(root)`. Beside it in the root's content: the delete alert (`PhotosAlerts`) and the scene's `AMToast`, so toasts rise above the floating action button.
- **Back**: the alert, then the remote, then a sheet (windows); then select mode (`PhotosSelectBackHandler`, a `NavigationBackHandler` the Library and an album panel register while selecting, so it comes before the `NavDisplay`'s); then the root's panels; then Albums swaps to the Library; then out of the scene.
- **Backdrops.** Every view lies on an `AMBackdrop`, so frosted cards, the frosted sheets and the section headers find its art, and toolbar buttons on it come up solid: the Library and Albums on the latest photo (`PhotosPageBackdrop`: `amPageBackdrop` of `model.latestPhoto`'s thumbnail, whatever the filter), an album panel on its first photo, the detail on its photo blurred (`amPausedBackdrop(AMPausedScrim.Page)` under `amCrossfade(photo.id)`, so swiping fades the next photo in under a steady blur).
- **Sheets go with the view on top.** The view that is the current root's last entry (`model.isOnTop(root, route)`) composes `PhotosSheets` inside its backdrop, so a frosted sheet blurs the art of the view it opened over.

## The model — `PhotosModel`

A plain `@Stable` class of Compose state, built on the scene's coroutine scope; it owns the copy.

- **Library**: `photos` (`PhotosSampleLibrary`), `albums` (`PhotosSampleAlbums`), `isLoading` (the library lands 0.9 s after the scene opens, `finishLoading()`; `cancelPendingWork()` abandons the read).
- **Navigation**: `rootView: PhotosView` (`Library`, `Albums`); a back stack per root (`stack(view)`, starting with `PhotosRoute.Root`); a detail page per root (`detailIndex(view)` / `setDetailIndex`); `openDetail(id, context)` pushes `Detail(ids of context)` on the current root, opened on the photo; `openAlbum(id)`; both push only a route that isn't already on the stack, so a double tap opens one panel; `pop(view)` pops the top panel and ends an album's selection once its panel is gone; `switchTo(view)` closes the remote, then ends select mode and sets the root inside `amInstantSwitch`.
- **Filter**: `filter: PhotosFilter` (`All`, `Favorites`, `Place(place)`, each with a `title` and `includes`); `filters` is All, Favorites, then each place that has photos, in `PhotosPlace` order; `libraryPhotos` is the filter's photos, newest first; `libraryMonths` groups them into `PhotosMonth`s. Setting the filter prunes the selection.
- **Select mode**: `selecting: PhotosSelectScope?` (`Library` or `Album(id)`), `selectedIds`; `shownPhotos` (what the selecting surface shows), `orderedSelection` (the picks in on-screen order, which every batch action uses), `selectionTitle` ("Select photos" / "3 selected"), `isAllShownSelected`, `selectionIsAllFavorites`; `beginSelecting`, `endSelecting`, `toggleSelected`, `toggleSelectAll`. **The selection is always a subset of what is shown.**
- **The remote**: `menuContext` (select mode wins, then the open detail, an open album, the root view); `openMenu()` captures `menuState: PhotosMenuState` (the context and whether there's a selection, all picks are favorites or all shown are picked, photos are shown, albums exist), so the menu's rows hold still while it fades under the action it ran.
- **Actions**: `toggleFavorite(ids)` unfavorites when every photo already is one, else favorites them all; from select mode it ends select mode and toasts "Added 3 photos to Favorites" / "Removed 3 photos from Favorites" (counting the photos that changed); outside select mode (the detail's heart, the detail menu's Favorite) it changes the photo without a toast. `presentAddToAlbum(ids)` opens the sheet on those targets, which outlast the sheet; `add(toAlbum)` skips photos already there, toasts "Added to <album>" / "Added 3 photos to <album>" / "Already in <album>", closes the sheet and ends select mode. `confirmDelete(ids)` and `confirmDeleteAlbum(id)` raise a `PhotosAlert`. `delete(ids)` removes the photos from the library, every album and the selection, closes their Info sheet, and on each root whose top is a detail keeps the page (clamped) or pops the detail when none of its photos remain; a place left with no photos loses its chip, so its filter goes back to All; select mode ends; toast "Photo deleted" / "3 photos deleted". `deleteAlbum(id)` pops its panel (its photos stay) and toasts "Album deleted". `moveAlbum(from, to)` moves an album one step. `showInfo(id)` opens the Info sheet.
- **Saved** (`PhotosModel.saver(scope)`, a `mapSaver`): whether the library has landed, which photos remain and which are favorites, the albums' order and contents, the root, both back stacks and detail pages, the filter, select mode and the picks, and which sheet, alert or menu is up. A read in flight isn't kept; it starts over.

## The samples — `PhotosData.kt`

- `PhotosPlace`: Brooklyn ("Brooklyn, New York"), Kyoto, Lisbon, Reykjavík, each a `title` and a `detail`.
- `PhotosItem`: id, picsum image id (`thumbUrl` 360², `fullUrl` 1200²), `takenAt`, place, camera ("Pixel 10 Pro" or "Fujifilm X-T5"), lens, exposure ("ƒ/8 · 1/500 s"), ISO, pixel size, file size, keywords, `isFavorite`. Units use non-breaking spaces and every value fits one line. Dates follow the phone's locale: `captionDate` ("Saturday, October 24 · 8:12 AM"), `fullDate` ("October 24, 2026"), `photosMonthTitle` ("October 2026"); `accessibilityName` is the place and date ("Kyoto, October 10, 2026"), since photos have no titles.
- `PhotosSampleLibrary`: 30 distinct pictures from August to October 2026, newest first, 7 favorites. `PhotosSampleAlbums`: Trips 2026 (18), Around Brooklyn (12), Coast and Water (9), Quiet Corners (7); an album's first photo is its cover. Favorites is never an album.
- `photosCountLabel`: "1 photo" / "n photos".

## The surfaces

- **Library** (`PhotosLibrary`, the landing view): a `Scaffold` on the transparent container under `AMPageHeader("Library")` (collapsing, `exitUntilCollapsedScrollBehavior`) with `SceneAppearanceButton()` at its end; in select mode the header's title is the selection title and Done (`ic_check`, ends select mode) holds the chip's place, so nothing moves. A `LazyVerticalGrid` of three columns `spacing.sm` apart, inside the page gutter, ending `PhotosFabClearance` above the navigation bar: the count line (`bodyMedium`, `onSurfaceVariant`; at zero a blank line of the same height, silent), the single-choice `AMChipGroup` of `filters`, then per month an `AMSectionHeader` over its tiles (`photosTiles`), or under the chips `AMEmptyState` "No favorites" (heart) / "Photos you mark with the heart show up here." under Favorites, else "No photos" / "Photos you take show up here.". A tile is an `AMRemoteImage` square in `shapes.medium`, read as its `accessibilityName`: `clickable` (opens the detail paging `libraryPhotos`) or, in select mode, `toggleable` with the checkbox role, carrying `amSelectionCheck` while picked.
- **Loading** is the same layout: the count line (when there's a count; at zero it's the same blank line) and the month titles are `AMSkeleton.Text` of their own text in their own style (shimmering bars at the text's size, read as nothing), the tiles are `AMSkeleton.Rect` squares, and the chips stay, disabled and shimmering (`amShimmer`), so nothing moves when the library lands. The header stays as it is.
- **Albums** (`PhotosAlbums`): `AMPageHeader("Albums")` with the same scroll behavior (so its title sits where the Library's does), then a scrolling column: "4 albums" / "1 album", and an `AMReorderableCardList` of the albums (`fillMaxWidth()`, `spacing.md` more above it), as tall as its cards and scrolling with the page, its cells carrying the gutter. Each card (`PhotosAlbumCard`) is a frosted `AMListCard`: the 56 dp cover in `shapes.small` (`PhotosAlbumCover`: the first photo, or `ic_photo_album` on the image fallback when empty), the name over the count in `AMTheme.Frosted.secondaryText`; a tap opens the album, a hold or the handle drags it, and TalkBack offers Move up and Move down. No albums: `AMEmptyState` "No albums".
- **Album panel** (`PhotosAlbumPanel`): `AMPanel` on the transparent container over a backdrop of the album's first photo, titled by the album, or in its select mode by the selection title with Done at the end; the count line over the album's grid, or a centered `AMEmptyState` "No photos". A tap opens the detail paging the album, or in select mode checks the photo. It holds on to what it last showed while it pops (`PhotosLastSeen`): the deleted album's name and photos, and the selection's title and checks when the arrow pops it mid-selection.
- **Detail** (`PhotosDetail`): `AMPanelChrome` titled by the photo's place, with Info (`ic_info`) at its end, on the photo's blurred backdrop; under it the square `AMCarousel` (`sideInset = 0.dp`) of plain full-width photos (`AMRemoteImage` with a square skeleton fallback), on the root's detail page; then a frosted caption card (`amCard(fill = Frosted)`): the caption date in `bodyMedium` / `Frosted.secondaryText` and the heart `AMIconButton` (`ic_favorite` / `ic_favorite_filled`, "Add to Favorites" / "Remove from Favorites"). Its pages resolve against the live library; it keeps drawing the pages it last resolved while it pops after its last photo goes.
- **Add to album sheet** (`PhotosAlbumSheet`): `AMSheet` "Add to album", `AMSheetHeight.Content`, `AMSheetSurface.Frosted`: one solid card of `AMListRow`s (the 44 dp cover, the name over the count); a row whose album already holds every target shows a `primary` check and reads as selected. A tap calls `add`.
- **Info sheet** (`PhotosInfoSheet`): `AMSheet` "Info", full height, frosted: `AMSection` Rows "Details" (Date, Place, Dimensions, Size) and "Camera" (Camera, Lens, Exposure, ISO) of `AMLabeledContent`, then `AMSectionHeader("Keywords")` over neutral `AMTag`s in an `AMFlowLayout`, read as one element "Keywords: Coffee and Table". It keeps its photo while it slides away.
- **Delete alert** (`PhotosAlerts`): "Delete photo?" / "Delete 3 photos?" — "It's removed from your library and every album." / "They're removed from your library and every album."; "Delete album?" — "“Name” is removed. Its photos stay in your library."; Cancel and a destructive Delete.

## The remote — `PhotosMenu`

From the frozen `menuState`:

- **Library**: Select (`ic_check_circle`, greys with nothing shown), Library (`ic_photo_library`), Albums (`ic_photo_album`), Exit (`ic_logout`).
- **Select mode** (Library or an album): Add to album (`ic_library_add`; greys with no pick or no album), Favorite (`ic_favorite`, filled and active when every pick is a favorite; greys with no pick) / Select all or Deselect all (`ic_check_circle_filled`, greys with nothing shown), Delete (destructive, greys with no pick) / the views row.
- **Detail**: Info, Favorite (for the photo showing) / Add to album (greys with no album), Delete / the views row.
- **An open album**: Select (greys on an empty album), Delete album (destructive, asks first) / the views row.
- **Albums**: the views row alone.

The views row is Library, Albums, Exit; the view showing is `active`. A view tile closes the menu and swaps the root (`switchTo`, ending select mode); every action tile closes the menu, then acts. Exit closes the menu, cancels the library read and calls `LocalCatalogNavigator.current.back()`.

## Reaching each screen

From the Library (the landing view): the + ("Actions") → the menu. A chip (Favorites) → the filtered library. The menu's Select → select mode; tap photos to pick them; the + → the select-mode menu. The menu's Albums → Albums; an album card → its panel. A photo → the detail; Info (or the menu's Info) → the Info sheet; the menu's Add to album → its sheet; Delete → the alert.
