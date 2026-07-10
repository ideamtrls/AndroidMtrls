---
scope: The :demo app's five full-screen app-like scenes (Auth, Social, Gallery, Now Playing, Calendar) and the shared scene toolkit they build on
globs:
  - demo/src/main/kotlin/**/scenes/**
  - demo/src/main/assets/**
keywords: [AuthScene, authSceneEntry, SocialScene, SocialState, SocialBackdrop, PostCard, PostCardSkeleton, PostActionMenu, socialSceneEntry, GalleryScene, GalleryState, GalleryPhoto, GalleryAlbum, GalleryPhotoGrid, GalleryLibraryView, GalleryAlbumsView, GalleryAlbumGridView, GalleryDetailPanel, galleryMonths, galleryPhotoCount, gallerySceneEntry, select mode, add to album, NowPlayingScene, MusicState, MusicCatalog, MusicCoverArt, MusicQueueItem, RepeatMode, MusicRoute, MusicView, playOrder, loadMusicCatalog, readId3Tags, ID3 tag reader, MediaPlayer playback, bundled audio, queue sheet, nowPlayingSceneEntry, CalendarScene, CalendarState, EventData, EventSheetContext, EventSheetView, CalendarView, CalendarLandingView, CalendarMonthsPicker, calendarSceneEntry, calendarSampleEvents, months picker, event sheet, LocalSceneExit, ScenePanelHost, ScenePanelChrome, snapNextPanel, imInstantSwitch, SceneGradient, sceneFabLift, sceneFabClearance, AMPausedSubstrate]
updated: 2026-07-10
---

# Scenes

The `:demo` app's Screens section — five full-screen composed demo experiences
that read as real apps, not catalog pages — plus the shared scene toolkit they
build on.

Five scenes ship (home display order): `scene-social` Social, `scene-nowplaying`
Now Playing, `scene-gallery` Gallery, `scene-calendar` Calendar, `scene-auth`
Auth. Each scene's `val <name>SceneEntry` lives beside its scene file; the demo
shell collects them into `scenesEntries` and renders each raw — full-screen, no
`ComponentPage` scaffold, no top bar, a scene owning its own chrome and exit
affordance. A scene attaches its overlays and reads its Exit affordance through
the shell's `ownsScaffold` / `LocalSceneExit` contract; see `demo.md` for the
registry wiring and the Screens navigation branch that provide them.

## Shared scene toolkit

- **`LocalSceneExit`** (`scenes/common/SceneExit.kt`):
  `staticCompositionLocalOf<() -> Unit> { {} }`. `DemoNavHost` provides
  `{ navController.popBackStack() }` around a scene's raw content in the Screens
  branch; a scene's FAB Exit tile calls `LocalSceneExit.current()`, keeping the
  scene parameterless. Default no-op → previews outside the nav shell are safe.
- **`ScenePanelHost` / `ScenePanelChrome`** (`scenes/common/ScenePanelHost.kt`):
  the state-driven, in-scene panel stack (no nested NavHost). `ScenePanelHost<R>(
  path, onPop, snapNext, modifier, root, panel)` always composes `root`; when `path` is
  non-empty the TOP route (`path.lastOrNull()`) renders over it through an
  `AnimatedContent` keyed on `top to path.size` so the slide is depth-aware: a
  push (depth grew) enters from the trailing edge / exits toward the leading
  edge, a pop (depth shrank) reverses it, and null↔route stays
  present-from-trailing / dismiss-toward-trailing. The panel sits in a full-size
  `Surface.page` box with a `pointerInput(Unit) { detectTapGestures {} }` tap
  sink (an empty `pointerInput {}` consumes nothing — same chrome-sink pattern as
  `AMActionsOverlay`/`AMSheet`) so blank panel taps do not fall through to the
  interactive scene root (FAB) beneath. A
  `BackHandler(enabled = path.isNotEmpty())` routes system/predictive back to
  `onPop` so back peels one panel before the scene exits. Slide/fade route
  through the `:mtrls` reduce-motion resolvers (`resolvedFiniteOrNull ?: snap()`
  for slide, `resolvedFinite` for fade) at 250ms — matching `AMPanelHost`. A
  one-shot `snapNext` (a scene sets its own `snapNextPanel` flag the recomposition
  it swaps root views) collapses the *next* transition to `snap()` — the FAB nav
  tiles use it so a stale panel vanishes instantly (iOS `imInstantSwitch`) rather
  than sliding out first; ordinary push/pop leave it `false` and keep the slide.
  `ScenePanelChrome(title, onBack, modifier, trailing = null, divider = true)`
  renders the same **Android-native top app bar** anatomy as the library `AMPanel`
  so scene panels read identically to the primitive: a `Column` of a 64dp `Row`
  bar — a **borderless back arrow** (`AMIconButton(Icons.AutoMirrored.Filled.ArrowBack,
  size = Medium, contentDescription = "Back")`, no chip), a **left-aligned title**
  (`Type.title` `Text.primary`, `weight(1f)`, single line / ellipsis), an optional
  trailing action — then an `AMDivider()` hairline gated by `divider`. Unlike
  `AMPanel` it applies **no** `statusBarsPadding()` (the scene call site owns the
  top inset) and carries no fill (the panel's substrate shows behind). Pass
  `divider = false` for chrome floating over full-bleed content (the gallery photo
  detail, the social post detail over the pastel backdrop) where an attached
  hairline would draw across the imagery.
- **`SceneGradient` / `sceneFabLift` / `sceneFabClearance`**
  (`scenes/common/SceneGradients.kt`):
  `@Immutable data class SceneGradient(lightTop, lightBottom, darkTop, darkBottom)`
  with a `@Composable SceneGradient.brush(): Brush` that resolves the two stops
  for the current scheme (`LocalAMIsDarkTheme`) into a diagonal
  (top-leading → bottom-trailing, `Offset.Zero → Offset.Infinite`) linear
  gradient — the two-stop scene backdrop. **`@Composable fun sceneFabLift(): Dp`** = the
  navigation-bar inset (`WindowInsets.navigationBars` bottom); **every scene hub
  passes `bottomInset = sceneFabLift()`** so the FAB rests on the gesture-nav
  safe-area bottom (the `AMFloatingActionsHost` draws edge-to-edge and the FAB
  rests flush against its bottom edge — the Android twin of iOS anchoring the FAB
  layer to the safe-area bottom). **`@Composable fun sceneFabClearance(): Dp`** =
  `sceneFabLift() + 56.dp + spacing.xxl` — the bottom padding every scene's scroll
  reserves so the FAB never covers the last row (the Android counterpart of iOS
  `galleryFabClearance`, folding the nav-bar inset in). Gallery's **select mode** is the one
  hub that raises the FAB above `sceneFabLift()`: it passes
  `bottomInset = sceneFabClearance()` to clear the batch toolbar.

## Auth scene (`scenes/auth/AuthScene.kt`)

`AuthScene()` — a parameterless three-phase sign-in flow (`private enum
AuthPhase { SignIn, TwoFA, Welcome }`); the iOS twin is `AuthScene`. One file;
its `authSceneEntry` (`scene-auth`, section Screens, title "Auth", icon
`Icons.Filled.Lock`) sits at the top and is registered in `scenesEntries`.

- **Host nesting** (outermost → innermost): `AMLoadingOverlay` (message
  "Signing in…") → `AMBanner` → `AMToast` → `AMFloatingActionsHost`. Each overlay
  host wraps the scene body and ORs its state into `LocalAMOverlayActive`, so the
  hub's FAB (innermost) fades under the loading overlay, banner, toast, or its
  own menu. Scene body = a `Surface.page` `Box` holding a `Column` with
  `statusBarsPadding()` + bottom `sceneFabClearance`: a top `AMToolbar` (leading
  back-chevron `AMToolbarButton` only when `phase != SignIn`) over an
  `AMCrossfade(targetState = phase)` of the three phase composables
  (`SignInPhase` / `TwoFAPhase` / `WelcomePhase`), each `padding(horizontal lg)`.
  The inputs are **placeholder-only with NO `AMFieldLabel`** — iOS `Auth.swift`
  labels neither phase, so the fields sit directly in the sign-in card's
  `Column(spacedBy md)`: `AMTextField(placeholder = "Email")`,
  `AMSecureField(placeholder = "Password")`, and the 2FA
  `AMTextField(placeholder = "123456")`. The filled anatomy, the inter-field
  bottom-indicator hairline, and the secure-field reveal/clear button are ruled
  Android characteristics, kept as-is.
- **State**: `phase` (`rememberSaveable` via an ordinal `Saver`), `email`/
  `password`/`otp` (`rememberSaveable` Strings), `attempts` (`rememberSaveable`
  Int); `loading`/`banner`/`toast`/`resendSecondsLeft`/`resendRestart`/
  `showActions` are plain `remember` (transient, mirror iOS `@State`); `fabOffset`
  is a `rememberSaveable` listSaver `Offset` (draggable FAB).
- **Choreography**: `attemptSignIn` increments `attempts`; every 3rd
  tap (`attempts % 3 == 0`) shows a destructive `AMBanner` ("Wrong password" /
  "Check your password and try again."), else sets `loading = true`. A
  `LaunchedEffect(loading)` waits 1.5s then clears loading and advances to
  `TwoFA` (Start Over cancels it by flipping loading off). The 2FA resend
  countdown is a `LaunchedEffect(phase, resendRestart)` that runs 30 → 0 at
  1/sec only while `phase == TwoFA` (cancels on phase change); the resend ghost
  button is enabled only at 0 and its tap fires a "Code re-sent" toast +
  `resendRestart++`. A `LaunchedEffect(phase)` fires the "Signed in" +
  `CheckCircle` toast on entering Welcome. Ghost buttons toast "Account creation
  isn't part of this demo" / "Password reset isn't part of this demo".
- **FAB menu**: one `AMActionGridRow` of two `AMActionGridButton`s at their
  default (self-carded) elevation — Start Over (`Icons.Filled.Refresh`, closes menu +
  `startOver()` resets everything but `email`) and Exit
  (`Icons.AutoMirrored.Filled.Logout`, closes menu + `LocalSceneExit.current()`).

## Social scene (`scenes/social/`)

The flagship scene — eight files: `SocialModel.kt`,
`SocialScene.kt`, `SocialFeed.kt`, `SocialPostDetail.kt`, `SocialProfile.kt`,
`SocialNotifications.kt`, `SocialCompose.kt`, `SocialSearch.kt`. `socialSceneEntry`
(`scene-social`, Screens, title "Social", subtitle "A feed of cards with action
rows", `Icons.Filled.People`) sits atop `SocialScene.kt` and leads `scenesEntries`.

- **State** (`SocialModel.kt`): `@Stable class SocialState`, `remember`ed once at
  the scene root (no ViewModel). `mutableStateListOf` for `users`/`posts`/
  `replies`/`notifications` (value-type elements replaced in place via private
  `withPost`/`withUser`); `mutableStateOf` for `currentView` (`SocialView`
  Feed/Alerts, swapped by plain writes), `postMenuId`, `composing`,
  `replyTargetId`, `searching`, `searchQuery`, `isLoading`, `toast`/`banner`/
  `alert`; `path = mutableStateListOf<SocialRoute>()` (PostDetail(UUID)/
  Profile(handle)) drives `ScenePanelHost`. Seed data is the sample set the pair
  shares — the iOS twin is `SocialModel.swift` (10 users with Wikimedia `Special:FilePath` avatar URLs,
  10 posts (3 carrying picsum images), 11 replies keyed by post index, 8 notifications,
  3-post `refreshPool`); UUIDs via `java.util.UUID`, and `seedReplies(posts)`/
  `seedNotifications(posts)` run AFTER `seedPosts()` so they key off `posts[i].id`.
  `loadIfNeeded` = 1200ms then `isLoading=false`; `refresh` = 1s then inserts a
  pooled post + "Feed updated" toast; compose limit 280; mute → info banner with
  hidden count; delete → alert confirm → removes post+replies+notifications and
  pops the detail if open; `copyText()` sets the toast while the scene writes the
  clipboard (`LocalClipboardManager`, since a plain holder can't read a Local).
- **Host nesting** (outermost → innermost): `AMToast → AMBanner → AMAlert →
  AMActionsOverlay → AMSheet(compose) → AMSheet(search) → AMFloatingActionsHost
  → ScenePanelHost`. Sheets/modals are ancestors of the panel host so they cover
  pushed panels; the FAB (innermost) fades under any of them via
  `LocalAMOverlayActive`.
- **Feed** (`SocialFeed.kt`): `AMPageHeader("Feed")` + a bell `AMIconButton`
  (unread-count `AMBadgeContent.Count`, `AMTheme.destructive` tint) → alerts root.
  First-load skeleton = four redacted `PostCardSkeleton`s (a real `AMCard` with
  an avatar circle + identity bar + `2 + i % 3` neutral body bars + a 3-glyph
  action cluster) under one `Modifier.amShimmer()` sweep — the Android twin of
  iOS's `.redacted(.placeholder)`. Pull-to-refresh is a
  `NestedScrollConnection` overscroll-drag (spec-accepted stand-in for iOS
  `.refreshable`): downward drag at the top past 72dp fires `refresh`, with an
  `AMSpinner(compact = true)` revealed as the content slides down. `PostCard` =
  `AMCard(padding = block)` (avatar tap → profile, card tap → detail) + shared
  `PostIdentityHeader`/`postBodyContent` + `AMCardActionRow` reply/repost(active
  success)/like(active destructive)/share/more(→`postMenuId`). The post body
  renders `AMTheme.Type.body` (16sp) — the SAME token the post-detail body uses,
  a deliberate drop of iOS's feed `.subheadline`/detail `.body` size step, chosen
  so the size reads similar to iOS's SF Pro rendering. `PostCard`/`PostActionRow` are
  always interactive — the first-load skeleton is its own `PostCardSkeleton`
  composable, and the post-menu preview hand-builds its non-interactive card
  from the shared `PostIdentityHeader`/`postBodyContent` pieces.
- **Post detail** (`SocialPostDetail.kt`): the post on a flat full-bleed
  `Surface.primary` band (avatar, detail identity, body, reply(→`beginReply`)/
  repost/like row — no share/more) over the full-bleed `SocialBackdrop`; replies
  as flat `AMListRow`s or a "No replies yet — add the first one." caption.
- **Profile** (`SocialProfile.kt`): an `AMCard` identity header (LG avatar, name,
  handle, bio, followers/following/posts stats, Follow `AMPrimaryButton` toggling)
  + `AMTabs` Posts/About; empty Posts → `AMEmptyState`; About = `AMRowContent`
  rows (Link tinted `Semantic.info` via the slot overload). A post row replaces
  the stack (`path.clear()` + push detail).
- **Alerts** (`SocialNotifications.kt`): `AMPageHeader("Alerts")` + notification
  `AMListRow`s — unread dot via `AMBadgedBox`(`Semantic.info`) on the avatar, a
  trailing kind glyph, name+verb bold when unread.
- **Compose sheet** (`SocialCompose.kt`, `AMSheetHeight.Full`): title, avatar +
  `AMTextEditor` (grows to fill; NO autofocus param on Android — keyboard rises on
  tap, an accepted divergence), 280-char counter (destructive over limit),
  Post/Reply `AMPrimaryButton` (disabled when empty or over limit).
- **Search sheet** (`SocialSearch.kt`, Full): `AMCard { AMSearchField(autofocus,
  onCancel → close) }` + live post filter (text/name/handle) with two
  `AMEmptyState` branches; a result opens the post detail.
- **Post-action menu**: `AMActionsOverlay` overlayContent = a non-interactive
  card preview over an `AMActionGridRow` Copy / Mute(`enabled = !isSelf`) /
  Delete(Destructive), tiles at their default (self-carded) elevation. Hand-built
  `Column(fillMaxSize)` with a leading `Spacer(weight(1f))` to bottom-anchor over
  the full-screen overlay (a stopgap until the deferred post-menu rewrite; the
  spec-blessed divergence from iOS's measured card pinning — no image in preview).
- **FAB menu** (`SocialScene.FabMenu`): a 2-up row (Compose, Search) over a 3-up
  row (Feed, Alerts, Exit), tiles at their default (self-carded) elevation;
  Feed/Alerts call `showFeed()`/`showAlerts()` (swap root + clear `path`), Exit →
  `LocalSceneExit`.
- **`SocialBackdrop`** (`SocialScene.kt`, `internal`): the two-stop
  `SceneGradient` wash (Social's own light/dark stops) painted via `drawBehind`
  plus three offset translucent circles drawn with `drawCircle` — the colors and
  offsets are Social's shared backdrop palette (offsets relative to the
  substrate center; iOS pt → dp). Behind the feed/alerts root and the post-detail
  panel; the profile panel keeps the plain `Surface.page` fill.
- Scene body follows Auth's `statusBarsPadding()` + bottom `sceneFabClearance`
  precedent on every scroll.

## Gallery scene (`scenes/gallery/`)

A photo-management scene — four files:
`GalleryData.kt`, `GalleryScene.kt`, `GalleryLibrary.kt`, `GalleryDetail.kt`.
`gallerySceneEntry` (`scene-gallery`, Screens, title "Gallery", subtitle
"Browse, favorite, and organize photos", `Icons.Filled.PhotoLibrary`) sits atop
`GalleryScene.kt` and holds the third `scenesEntries` slot.

- **Data** (`GalleryData.kt`): `@Immutable data class GalleryPhoto`
  (id/title/month/date/location/camera/fileSize/tags/isFavorite; `thumbUrl` =
  `picsum.photos/seed/iosmtrls-{id}/240/240`, `fullUrl` = `.../600/600`) and
  `@Immutable data class GalleryAlbum(name, photoIDs)`. Seed = 30 photos +
  4 albums (Travel 2026 9 / Family 4 / Inspiration 8 / Work 4), the sample set
  the pair shares (iOS twin `GalleryData.swift`). `galleryMonths = ["May 2026","April 2026",
  "March 2026"]` — grouping ITERATES this list, so a refresh shuffle reorders
  photos within a month but never reorders the months. `galleryPhotoCount(n)` =
  "1 photo" / "n photos". **Favorites is derived, never stored.**
- **State** (`GalleryScene.kt`): `@Stable class GalleryState`, `remember`ed once
  at the scene root. `mutableStateListOf` for `photos`/`albums`;
  `mutableStateOf`/`mutableIntStateOf` for `isLoading`, `currentView`
  (`GalleryView` Library/Albums), `detailIndex`, `isSelecting`,
  `selectedIDs`/`selectedAlbumNames` (plain `Set` in `mutableStateOf`),
  `showingAlbumSheet`, `toast`/`alert`; `path = mutableStateListOf<GalleryRoute>()`
  (`Album(name)` / `Detail(ids)`) drives `ScenePanelHost`. `favoritePhotos`
  derives from `isFavorite`; `libraryOrderedPhotos` flat-maps `galleryMonths`.
  `load()` = 0.9s first load; `refresh()` = 0.5s then reshuffle; `switchTo(view)`
  clears `path` + exits select mode + sets the view; `performDelete` also clamps
  the detail page or pops the panel once its carousel empties. Select-mode cleanup
  runs on every root switch.
- **Host nesting** (outermost → innermost): `AMToast → AMAlert →
  AMSheet(albumPicker, Full) → AMFloatingActionsHost → ScenePanelHost`. The sheet
  covers pushed panels; the FAB (innermost) fades under any of them and rises
  above the batch toolbar in select mode via `AMFloatingActionsHost.bottomInset =
  sceneFabClearance`.
- **Library root** (`GalleryLibrary.kt`): `AMPageHeader("Library")` + count,
  month-grouped 3-column `GalleryPhotoGrid` (a `chunked(3)`-row grid of square
  weighted `AMSelectionCheckBox` tiles — no `LazyVGrid`, so it nests in
  `verticalScroll`; `BoxWithConstraints` sizes each `AMRemoteImage`), a dedicated
  skeleton loading twin (`isLoading` 0.9s — a section header + 12 neutral
  separator-wash tiles in the grid's 3-column rhythm under one `amShimmer` sweep,
  never real images so it reads redacted even with a warm Coil cache, while a Coil
  warm-up preloads the first month's thumbs), `AMEmptyState` when empty, and
  pull-to-refresh (the Social
  feed `NestedScrollConnection` overscroll-drag) that reshuffles. Every scroll
  bottom-pads `sceneFabClearance`.
- **Albums root**: 2-column `AMCard` cards — Favorites (derived) first, then the
  four manual albums; cover `AMRemoteImage` (glyph `fallbackIcon`), name, count.
  A tappable inner Box (clipped to the card radius, `amRipple`) keeps the ripple
  rounded while `AMCard` owns the shadow. Select mode: manual cards toggle
  membership (top-trailing mark); the Favorites card dims and ignores taps.
- **Album-grid panel**: `photosInAlbum(name)` (or `favoritePhotos`) through the
  shared grid, or a per-album `AMEmptyState`.
- **Detail panel** (`GalleryDetail.kt`): a paging `AMCarousel(sideInset = 0)`
  drives `detailIndex` over an `AMPausedSubstrate(AMPausedScrim.Page, key =
  current.id)` that crossfades the current photo (a screen-sized square
  `AMRemoteImage`, `ContentScale.Crop`) behind a stable blur + page wash.
  Metadata block on `Surface.primary` (date·location, `AMTag` keywords,
  camera·fileSize, `padding(end = 44.dp)`) with the favorite heart `AMIconButton`
  (`active = isFavorite`) overlaid trailing-center. The transparent
  `ScenePanelChrome` (`divider = false`) sits ABOVE the photo in the foreground
  `Column(statusBarsPadding)` — the chrome-above-content pattern every scene panel
  uses — reserving the top bar like the iOS panel's safe-area inset, so the square
  photo sits below it (an `md` gap between) with the backdrop showing through the
  transparent chrome; its title tracks the paged photo.
- **Select toolbar** (`GalleryScene.SelectToolbar`): a bottom `AMToolbar` batch
  bar — add-to-album (library selection only) / delete (`AMButtonRole.Destructive`)
  / cancel; chips grey via the `AMToolbarButton(enabled = …)` param
  (`selection.isNotEmpty()`). Lives in the root, so a panel covers it — and select
  mode is only reachable at the root.
- **FAB menu**: route-contextual over the always-present `[Library, Albums, Exit]`
  nav row — detail → `[Add to Album, Delete]`; root → `[Select, Delete]` (Select
  greys while selecting, Delete greys until items are selected); open album → nav
  row alone. Nav tiles call `switchTo` (reset). Tiles at their default (self-carded) elevation.
- **Album-picker sheet** (`GalleryScene.AlbumPickerSheet`): title + an
  `AMReorderableRowList` of album `AMListRow`s (`amRipple` clickable → add to that
  album idempotently + toast; long-press reorders). Delete-confirm copy varies by count/kind, verbatim
  from iOS.
- Every tap affordance uses `indication = amRipple()` clipped to its shape
  (per conventions — never `indication = null` on an affordance).

## Now Playing scene (`scenes/music/`)

Three files: `MusicData.kt` (value types + the `loadMusicCatalog` scanner + ID3
tag reader + palettes + `MusicCoverArt`), `MusicLibrary.kt` (library root +
artist/album panel bodies), `NowPlaying.kt` (`MusicState` + the scene root).
`nowPlayingSceneEntry` (`scene-nowplaying`, Screens, title "Now Playing", subtitle
"Player and library with a real queue", `Icons.Filled.PlayCircle`) sits atop
`NowPlaying.kt`, second in `scenesEntries`. The scene runs on
**real bundled audio**: 9 MP3s in `demo/src/main/assets/Audio/` (the audio the
pair shares) drive a real `MediaPlayer`, and each track's metadata + embedded cover
art are read from its ID3 tag — the Android twin of iOS scanning its bundled MP3s.

- **Data** (`MusicData.kt`): `@Immutable` `MusicTrack` (adds `assetPath` — the
  file under `assets/Audio/`, the real playback source — and `coverArt:
  ImageBitmap?`, the embedded artwork or null) / `MusicAlbum` (also carries a
  `coverArt`) / `MusicCatalog` (albums + flat tracks, catalog-order = album-order
  then track-order); `MusicView` (Player/Library), `MusicRoute` (Artist /
  Album(albumId)), `RepeatMode` (Off→All→One cycle via `.next`, `.isActive`),
  `MusicQueueTarget` (Artist/Album/Track, with `.tracks(catalog)`/`.label(catalog)`).
  `MusicQueueItem(uid: Long, track)` tags each queue slot with a process-unique id
  so the player carousel and reorderable sheet keep STABLE keys even when a track
  repeats (bare `track.id` keys would collide and crash the reorderable
  `LazyColumn`). **`suspend fun loadMusicCatalog(context): MusicCatalog`**
  (`Dispatchers.IO`) scans the bundled MP3s: `MediaMetadataRetriever` yields only
  DURATION reliably here (it returns null for every ID3 text frame AND the embedded
  picture on this device, independent of ID3 version), so a small Kotlin
  **`readId3Tags`** parser reads title/artist/album/track + the first embedded
  picture straight from the tag — handling ID3v2.2 (`TT2`/`TP1`/`TAL`/`TRK`/`PIC`),
  v2.3/v2.4 (`TIT2`/`TPE1`/`TALB`/`TRCK`/`APIC`), the four text encodings, tag-level
  unsynchronisation, and the v2.3/v2.4 extended header. Palettes (`musicPalette(slug)` →
  `SceneGradient`, `musicNeutralPalette` fallback) are the shared per-album gradients
  keyed by album slug (`internet-on-the-tv`, `silent-artillery-of-time`,
  `street-fight`); the slugs match the MP3s' ID3 album names. `musicTimeLabel(seconds)`
  = "m:ss" (minutes NOT zero-padded); `musicCoverThumbSize = 40`, `trackMetaSpacing
  = 2`, `musicPlaceholderTrack` (225s) shapes the loading skeleton. `MusicCoverArt(…)`
  draws the track's embedded `coverArt` bitmap when present, else falls back to the
  album gradient wash + a centered translucent `MusicNote` watermark (full-bleed on
  the player, 40dp thumb in rows).
- **State** (`MusicState`, `@Stable`, `remember`ed once at the scene root, no
  ViewModel): `catalog` (loaded async — `isLoading` starts `true` until
  `loadMusicCatalog` returns), `playOrder: List<MusicQueueItem>`, `trackIndex`,
  `currentTime: Double` (the playback clock; a public var the tick pulls from the
  live player and seeks write), `isPlaying`/`isShuffling`/`repeatMode`,
  `currentView`, `showQueue`, `queueMenuTarget`, `toast`, `path: List<MusicRoute>`,
  `isLoading` (all `mutableStateOf`/`mutableIntStateOf`; most have `private set`
  and mutate via methods), plus a private `MediaPlayer? player`. Derived:
  `currentItem` (the current `MusicQueueItem` slot)/`currentTrack` (= `currentItem?.track`)/
  `currentAlbum`/`currentPalette`/`upNext` (everything after the
  current track). Methods: `load` (scans the catalog, then seeds the queue with the
  whole library, parked at track 0), `play(tracks, index)` (queue becomes those tracks, starts
  playing, returns to the player, resets `path`), `addToQueue` (append + "Added to
  queue" toast), `reorderQueue` (write Up Next back below the current — **silent**,
  fired per drag-over move; the "Queue updated" toast is `notifyQueueReordered`,
  raised once per settled drag), `toggleShuffle` (reshuffle everything after the current; restore
  sorts the tail back into catalog order while KEEPING each slot's `uid` and
  duplicates — a small improvement over iOS's id-set filter),
  `goTo`/`next`/`previous`/`advanceAtTrackEnd` (honor `RepeatMode`), `seek`,
  `togglePlay`, `cycleRepeat`; nav helpers `openArtist`/`openAlbum`/`pop` and the
  FAB `showLibrary`/`jumpToArtist`/`jumpToAlbum`/`showPlayer` (each resets `path` to
  its destination so a stale panel can't cover the chosen view). The audio engine:
  `reloadAudio(autoplay)` swaps `currentTrack`'s MP3 into a fresh `MediaPlayer`
  (streamed from the asset fd, seeking to `currentTime`, continuing playback) and is
  **generation-safe** — it bumps a private `loadGeneration` stamp on entry and
  re-checks it after the blocking `prepare()` returns; a superseded/cancelled reload
  releases its own freshly prepared player and assigns nothing, so exactly one player
  survives and the previous one is released exactly once,
  `syncTick()` pulls `currentTime` off the live player each 0.5s and calls
  `advanceAtTrackEnd` at end, `releasePlayer()` frees it. A `private var nextUid`
  mints `MusicQueueItem` uids; a `private var loadGeneration` stamps audio loads.
- **Host nesting** (outermost → innermost): `AMToast → AMActionsOverlay(long-press
  queue menu) → AMSheet(queue, `Full`) → AMFloatingActionsHost → ScenePanelHost`.
  The FAB host is `enabled = !isLoading` (iOS `disabled: catalog == nil`), draggable
  via a `remember { mutableStateOf(Offset.Zero) }`, label "Music menu".
- **Real playback** (`MediaPlayer` — the Android counterpart of iOS's
  `AVAudioPlayer` — plus a 0.5s sync tick): the scene composable runs a `LaunchedEffect(Unit)` that scans the catalog
  (`state.load()`, which calls `loadMusicCatalog` then seeds the queue), a `LaunchedEffect(currentItem?.uid)` that
  loads that slot's MP3 into the player and continues from `currentTime` (prev/next
  reload and keep playing) — keyed on the queue-slot `uid`, not `track.id`, so
  advancing between two adjacent copies of the SAME track still reloads/restarts —
  a play+tick effect that starts the player and runs the
  0.5s `syncTick`, an effect that applies scrubber drags to the player as `seekTo`,
  and a `DisposableEffect` that `releasePlayer()`s on teardown. Auto-advance at
  track end and repeat-one restart honor `RepeatMode`; a scrubber drag overwrites
  `currentTime` and re-seeks. **Crash guard:** `syncTick` reads the live player
  under `runCatching` and `reloadAudio` nulls the field before `release()`, so a
  rapid Next while playing can't touch a half-released player; a `loadGeneration`
  stamp additionally stops a cancelled reload from assigning a second player.
- **Root** (`RootContent`): `isLoading` → neutral-gradient box + `LoadingLayout`
  (the player layout from `musicPlaceholderTrack` — a `separator` cover block over
  a placeholder meta + a DISABLED transport silhouette + an inert scrubber, the
  whole thing `amShimmer`ed); Player → current-album-gradient box + `PlayerLayout`;
  Library → `MusicLibraryView` (its own `Surface.page` fill). The static catalog is
  never empty, so there is NO "No Music" branch. Panels (`ScenePanelHost` over the
  root): `MusicRoute.Artist` → `ScenePanelChrome(artistName)` + `MusicArtistPanel`;
  `MusicRoute.Album` → `ScenePanelChrome(album.title)` + `MusicAlbumPanel`.
- **Player** (`PlayerLayout`): a full-bleed square `AMCarousel` of `MusicCoverArt`
  (`sideInset = 0`, `key = { it.uid }`, `onIndexChange = goTo`) over a flat
  `Surface.primary` transport band, both pinned to the top with a bottom
  `Spacer(weight 1f)`; `statusBarsPadding()` + bottom `sceneFabClearance` reserves
  the FAB zone. `PlayerControls` (shared by the band and the queue card) = centered
  `TrackMeta` (20sp SemiBold title + secondary artist), `TransportRow` (shuffle
  `AMIconButton(active)` · prev `AMIconButton(enabled = index > 0)` · play/pause
  `AMToolbarButton(size = Prominent)` · next `AMIconButton(enabled = not last ||
  repeat All)` · repeat `AMIconButton(active, badge = AMBadgeContent.Count(1) when
  One)`), and a `Scrubber` `AMSlider` (`leadingText`/`trailingText` tabular-figure
  elapsed/`-remaining` via `musicTimeLabel`).
- **Library** (`MusicLibrary.kt`): `AMPageHeader("Library")` over one artist
  `AMListRow` ("N songs · M albums", chevron) → artist panel (album rows with
  `MusicCoverArt` thumbs — real embedded art, gradient fallback) → album panel
  (track rows, now-playing `isSelected`). Tap plays /
  drills; long-press (`combinedClickable`) sets `queueMenuTarget`.
- **Queue sheet** (`QueueSheet`, `AMSheetHeight.Full`): "Up Next" header over an
  `AMReorderableRowList` of `upNext` (`key = { it.uid }`; `onReorder` applies
  `reorderQueue` silently, long-press drag) or an `AMEmptyState` ("Nothing up next",
  `QueueMusic`) filling the weighted region, with the now-playing `PlayerControls`
  card (`AMCard`) pinned at the bottom. Because `onReorder` fires on every drag-over
  move, the sheet reflects the row slot's `isDragging` up into a `dragging` flag and
  records whether the drag `moved` anything; when the drag settles (`dragging` →
  false) it flushes a single `notifyQueueReordered` toast. The per-row
  `LaunchedEffect(isDragging)` is guarded by a per-row `sawDrag` flag — a
  `LaunchedEffect` runs on initial composition regardless of key, so a row that
  composes mid-drag (auto-scroll) with `isDragging = false` is a no-op; only the row
  that observed the drag start writes `dragging = false` at its end.
- **Long-press menu** (`QueueTargetMenu`, in `AMActionsOverlay`): a header naming
  the target over an `AMActionGridRow` [Play, Add to Queue] at their default
  (self-carded) elevation. Hand-built `Column(fillMaxSize)` with a leading
  `Spacer(weight(1f))` to bottom-anchor over the full-screen overlay.
- **FAB grid** (`FabMenu`): `[Library, Artist, Album]` (Artist/Album `enabled =
  currentTrack != null`) over `[Player, Queue, Exit]`; view tiles are plain state
  writes, Exit → `LocalSceneExit`. Icons: LibraryMusic, Mic, Album, PlayCircle,
  `AutoMirrored.QueueMusic`, `AutoMirrored.Logout`; transport uses Shuffle,
  SkipPrevious/SkipNext, PlayArrow/Pause, Repeat; Add-to-Queue = `AutoMirrored.
  PlaylistAdd`. Every affordance uses `amRipple`, `statusBarsPadding`, iOS copy
  verbatim.

## Calendar scene (`scenes/calendar/`)

A month-calendar scene — four files: `CalendarData.kt`,
`CalendarScene.kt`, `CalendarViews.kt`, `CalendarSheets.kt`. `calendarSceneEntry`
(`scene-calendar`, Screens, title "Calendar", subtitle "A month calendar with a
day's events", `Icons.Filled.CalendarMonth`) sits atop `CalendarScene.kt` and
holds the fourth `scenesEntries` slot. Two root views swapped instantly by plain
state writes (iOS `imInstantSwitch`); nothing pushes, so there is NO panel host.

- **Data** (`CalendarData.kt`): `@Immutable data class EventData(title, date:
  LocalDateTime, location, id: UUID)` with a derived `timeLabel` (localized SHORT
  time, rendered mono-digit). All `java.time`, relative to `LocalDate.now()`:
  `calendarSampleEvents()` seeds the VERBATIM 19 computing-history events (5
  today, 3 tomorrow, 4 this week at +2/+3/+3/+5, 3 back at −45/−30/−12, 4 forward
  at +10/+21/+40/+55) via a private `calendarDate(dayOffset, hour, minute)`.
  Formatters: `calendarMonthTitle` ("July 2026", `MMMM yyyy`), `calendarMonthName`
  ("July", `MMMM`), `calendarDayHeading` ("Today" or "Tuesday, Jul 7",
  `EEEE, MMM d`), `calendarPickerYears()` (current year −3..+3). Nothing persisted.
- **State** (`CalendarScene.kt`): `@Stable class CalendarState`, `remember`ed once
  at the scene root. `mutableStateListOf<EventData>` for `events`; `mutableStateOf`
  for `displayedMonth: YearMonth`, `selectedDate: LocalDate`, `currentView`
  (`CalendarView` Month/MonthsPicker), `eventSheet: EventSheetContext?`, `toast`.
  Derived: `eventCounts` (per-`LocalDate` `eachCount`), `eventsOnSelectedDate`
  (filter by `toLocalDate()`, sorted by time), `isViewingToday` (`displayedMonth
  == YearMonth.now() && selectedDate == LocalDate.now()`). Methods:
  `openCreateSheet`/`openEditSheet` (set `eventSheet`), `goToToday` (reset month +
  day + Month view), `showMonthsPicker`, `pickMonth` (set month, select its 1st,
  swap to Month), `saveEvent` (edit writes back by id + "Event saved"; else append
  + "Event added"), `deleteEvent` (remove by id + "Event deleted"). `EventSheetContext(
  event, defaultDate)` is a PLAIN class (identity equality) so it keys the sheet
  draft's `remember` — each present re-seeds, so reopening create on the same day
  starts blank.
- **Host nesting** (outermost → innermost): `AMToast → AMSheet(eventSheet) →
  AMFloatingActionsHost`. The toast + sheet wrap the scene body and OR into
  `LocalAMOverlayActive`, fading the FAB. The sheet holds a retained last-context
  (`remember { mutableStateOf<EventSheetContext?> }`) so its card keeps content
  through the slide-out while `isPresented` reads `eventSheet != null`. `SceneBody`
  is a `Surface.page` box that swaps `CalendarLandingView`/`CalendarMonthsPicker`
  on `currentView`.
- **Landing view** (`CalendarViews.kt`, `CalendarLandingView`): a `verticalScroll`
  Column (`statusBarsPadding`, top `lg`, bottom `xxl*2 + sceneFabClearance`,
  `spacedBy xl`) — `AMPageHeader(monthTitle)` (inset `lg`) over a flat full-bleed
  `Surface.primary` band holding `AMCalendar(month, selection, markers =
  eventCounts[day] ?: 0)`, then `CalendarEventsSection`: `AMSectionHeader(dayHeading)`
  (inset `lg`) over a bare `Column` of flat `AMListRow`s (title, location subtitle,
  trailing mono `timeLabel`; whole row `amRipple`-clickable → edit sheet) or
  `AMEmptyState(CalendarMonth, "Nothing scheduled", "No events on this day.")`.
- **Months picker** (`CalendarMonthsPicker`): headerless `LazyColumn`
  (`statusBarsPadding`, contentPadding top `md`/bottom `xxl`, `spacedBy xl`); one
  item per `calendarPickerYears()` year (`AMSectionHeader(year)` over 12 month
  `AMListRow`s, displayed month `isSelected`, `amRipple`-clickable → `pickMonth`).
  A `LaunchedEffect(Unit)` `scrollToItem` opens the list at the displayed year
  (pragmatic one-item-per-year scroll anchor).
- **Event sheet** (`CalendarSheets.kt`, `EventSheetView`, default `Full` height):
  a top-aligned Column (`spacedBy lg`) — "New Event"/"Edit Event" title, then
  `AMFieldLabel` over an `AMTextField` for title (fields carry no label of
  their own — the standing label is the Mtrls pattern), `AMDatePickerRow("When",
  epoch-millis ⇄ `LocalDateTime`, `AMDateComponents.DateAndTime`)`, the same
  label+field pair for location, then `AMFormActions` — create: Save only (gated
  on non-blank title); edit: destructive Delete + Save. Create drafts pre-fill
  9:00 on the selected day.
- **FAB menu** (`CalendarScene.FabMenu`): `[New Event (EditCalendar), Today
  (Today)]` over `[Months (DateRange), Exit (AutoMirrored.Logout)]`, tiles at their
  default (self-carded) elevation. Today greys via `enabled = !isViewingToday` and doubles as
  the way back from the picker to the month view (alongside picking a month —
  iOS's separate "Current"/"Years" tiles collapse into this 2×2). Exit →
  `LocalSceneExit`. Every affordance uses `amRipple`; iOS toast copy verbatim.

## See also

- `demo.md` — the catalog registry (`scenesEntries` wiring), the navigation
  shell's Screens branch (raw scene content + the `LocalSceneExit` provider), the
  `immersive` shell flag scenes hide the top bar with, and the home
  `SceneCarouselBand`/`SceneCard` band that links into these scenes (its
  edge-peek pager math is documented there).
- `surfaces.md` — the library overlay hosts (`AMToast`/`AMSheet`/`AMBanner`/
  `AMAlert`/`AMActionsOverlay`/`AMLoadingOverlay`) and `AMPanel` these scenes
  nest and mirror.
