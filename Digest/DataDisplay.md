---
scope: Read-only display primitives — avatars, tags, badges, timelines, remote images and the image pipeline, art thumbnails and art cards, the art backdrop, its washes toward the mode and the art's tone, art rows and the art-rows backdrop, art text — and their catalog pages
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/datadisplay/**, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/internal/ImagePipeline.kt, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/internal/ImageDiskCache.kt, androidmtrls/src/main/res/drawable/am_ic_person.xml, androidmtrls/src/main/res/drawable/am_ic_image.xml, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/ImagePipelineTest.kt, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/ImageDiskCacheTest.kt, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/DataDisplayMathTest.kt, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/ArtWashTest.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/datadisplay/**]
keywords: [AMAvatar, AMAvatarSize, diameter, initials, avatarInitials, BreakIterator, avatarHue, djb2, hashed hue, person glyph, AMTag, AMTagTint, frostedTagFill, pill, AMBadge, AMBadgedBox, BadgedBox, 99+, badge contentDescription, AMTimeline, AMTimelineRow, AMTimelineDot, AMTimelineLabel, AMTimelinePosition, timelinePosition, amTimelineFirstLine, TimelineFirstLine, marker centering, rail, tabularFigures, tnum, AMRemoteImage, AMRemoteImageFallback, fallback, no flash, HeldImage, AMArtThumbnail, am_ic_image, AMArtCard, interactionSource, decorative image, image pipeline, ImagePipeline, AMImagePipeline, userAgent, User-Agent, defaultImageUserAgent, loader, offline store, allowsLocalSources, file URL, content URI, ImageSource, resolveImageSource, imageSourceOf, HttpURLConnection, redirects, useCaches, readBounded, body limit, 32 MB, deadline, runAbortable, AbortSignal, ImageDecoder, decodeSize, decode caps, decodeCovers, ImageKey, DecodedImage, ImageMemoryCache, latest, InFlightLoads, ImageDiskCache, am_image_cache, imageCacheFileName, imageCacheEvictions, immutable per URL, Modifier.amArtBackdrop, AMArtDefaults, BlurRadius, LightWash, DarkWash, wash toward the mode, washTowardMode, frost, artBoost, saturation, LightArtGround, DarkArtCeiling, Modifier.amArtRows, Modifier.amArtRow, artRowBands, bands, blend, bleed, lastPicture, drawPageBand, AMArtRowsBackdrop, ArtRowsArt, page color fallback, ArtImages, ArtSource, ArtIdentity, ArtKey, artWorkingSize, blurSigma, ImageBitmap, embedded cover, crossfade, amInstantSwitch, AMArtTone, LocalAMArtTone, AMTheme.artTone, artInk, artInkShadow, artInkHolding, moveToLuminance, Style.ArtInk, amArtText, art text, color under cards, ArtWashTest, DataDisplayMathTest, ImagePipelineTest, ImageDiskCacheTest, DataDisplayPages, catalog pages]
updated: 2026-11-17
---

# Data display

Read-only display: avatars, tags, badges, timelines, `AMRemoteImage` over the library's own image pipeline, art thumbnails and art cards, and the art that puts color under cards (`amArtBackdrop`, art rows and `AMArtRowsBackdrop`, washed toward the mode, with the art's tone and `amArtText`). Everything here draws and owns no data; the only state is what a picture needs to load.

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `datadisplay/Avatar.kt` | `AMAvatarSize`, `AMAvatar`; internal `avatarInitials`, `avatarHue` |
| `datadisplay/Tag.kt` | `AMTagTint`, `AMTag` (string and slot forms); internal `frostedTagFill` |
| `datadisplay/Badge.kt` | `AMBadge` (dot and count forms), `AMBadgedBox` (dot and count forms) |
| `datadisplay/Timeline.kt` | `AMTimelinePosition`, `AMTimeline`, `AMTimelineRow` (slot and string forms), `AMTimelineDot`, `AMTimelineLabel`, `Modifier.amTimelineFirstLine`; internal `timelinePosition` |
| `datadisplay/RemoteImage.kt` | `AMRemoteImage`, `AMRemoteImageFallback`; internal `HeldImage` (the same frame for a bitmap); private `imageNode` |
| `datadisplay/ArtThumbnail.kt` | `AMArtThumbnail` (a URL and an `ImageBitmap` form) |
| `datadisplay/ArtCard.kt` | `AMArtCard` (a URL and an `ImageBitmap` form); private `artCardFrame`, `ArtCardLoading` |
| `datadisplay/ImagePipelineSettings.kt` | `AMImagePipeline` (the pipeline's process-wide settings: `userAgent`, `loader`, `allowsLocalSources`); internal `isUserAgentValue`, `defaultImageUserAgent` |
| `datadisplay/ArtBackdrop.kt` | `AMArtDefaults`, `Modifier.amArtBackdrop`, `Modifier.amArtRow` (each in a URL and an `ImageBitmap` form), `Modifier.amArtRows`, `AMArtRowsBackdrop`, `Modifier.amArtText`; the art recipe's constants, the private nodes that draw them, and the art-rows backdrop's private `ArtRowsArt`, `LocalArtRowsArt` and `ArtRowsArtNode` |
| `datadisplay/ArtTone.kt` | `AMArtTone`; internal `LocalAMArtTone`, `modeArtTone`, `artToneOf`, `artInk` (pure per tone, and composable), `artInkShadow`, `artInkHolding`, `moveToLuminance` |
| `datadisplay/ArtImages.kt` | internal `ArtSource` (`Url`, `Image`), `ArtIdentity`, `ArtImages` (the processed-picture cache and loader), `ArtKey`, `ArtWorkingSize`, `artWorkingSize`, `blurSigma`, `artKey`, `ArtCrop`, `coverCrop`, `areaAverage`, `gaussianBlur`, `saturate`, `srgbToLinear`, `linearToSrgb`, `linearLuminance`, `washTowardMode`, `fadeIn`, `ArtSpan`, `ArtBand`, `artRowBands` |
| `internal/ImagePipeline.kt` | internal `ImagePipeline`, `ImageSource`, `resolveImageSource`, `imageSourceOf`, `ImageFit`, `ImageKey`, `DecodedImage`, `decodeSize`, `decodeCovers`, `readBounded`, `AbortSignal`, `runAbortable`, `ImageMemoryCache`, `InFlightLoads` |
| `internal/ImageDiskCache.kt` | internal `ImageDiskCache`, `imageCacheFileName`, `isImageCacheFileName`, `ImageCacheFile`, `imageCacheEvictions` |

- Glyphs: `androidmtrls/src/main/res/drawable/am_ic_person.xml` (the filled `person`, an avatar with no name) and `am_ic_image.xml` (`image`, an art thumbnail's default placeholder). Tags in the status tints use `AMSemantic.icon`'s glyphs (`Foundations.md`).
- Unit tests, in `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/` (`Testing.md`): `ImagePipelineTest` (sources and the loader, the User-Agent, decode sizes and caps, bounded reads, aborts, the memory cache, shared loads), `ImageDiskCacheTest` (file names, trimming), `DataDisplayMathTest` (initials and hue, timeline positions, art keys and identities, working size, crop, average, blur, fade, row bands) and `ArtWashTest` (the wash, the recipe's extremes pinned to `LightArtGround` and `DarkArtCeiling`, the inks over them, `moveToLuminance` and `artInkHolding`, the tag's label on frost and the tint; its other frost and chip checks are `Foundations.md`'s and `Selection.md`'s).
- Catalog pages: `demo/.../catalog/pages/datadisplay/` (below).

## `AMAvatar` — `Avatar.kt`

```kotlin
AMAvatar(person.photoUrl, name = person.name)
AMAvatar(null, name = "Apollo Guidance", size = AMAvatarSize.Large, shape = MaterialTheme.shapes.medium)
```

`AMAvatar(url, name, modifier, size = AMAvatarSize.Medium, shape = CircleShape)`: a person's photo, or their initials. `name` is required and nullable, like a `contentDescription`, because it's also what a screen reader says.

- **Sizes.** `AMAvatarSize.Small` 32 dp, `Medium` 40 dp (Material's list avatar, the default), `Large` 56 dp. `diameter` is public, so a skeleton or a badge's host can match it. The initials are 13, 16 and 22 dp, set in dp (`Dp.toSp()`), so they scale with the circle and not with the font scale: an avatar is a picture.
- **Photo or initials.** The frame is an `AMRemoteImage` whose fallback is the initials, so loading, a failure and a null `url` all show them, and a photo in memory shows on the first frame.
- **Initials** (`avatarInitials`): the first user-perceived character (`java.text.BreakIterator`) of each of the first two words, split on any run of whitespace including Unicode spaces, upper-cased: "Christopher Knight" → "CK", "Jane" → "J". A null or blank name has none and shows the filled `person` glyph at half the diameter. Pass the full name, not pre-cut initials.
- **Color** (`avatarHue`): djb2 over the name's code points (start 5381, `hash × 33 + code point`, wrapping at 64 bits as a `ULong`), modulo 360, as a fraction of a turn; the whole name is hashed, so two people with the same initials usually differ. The fill is `Color.hsv(hue × 360, 0.55, 0.85)`, a mid-pastel under white initials, the same in both modes. A null name hashes the empty string.
- **Outline.** Clipped to `shape`, then `Modifier.amHairline(shape)` in both modes (`Foundations.md`).
- **Accessibility.** One node (`clearAndSetSemantics`): with a non-blank name it's an image (`Role.Image`) labeled with the name, photo or initials alike; otherwise it's decorative.

## `AMTag` — `Tag.kt`

```kotlin
AMTag("Live", icon = AMSemantic.Success.icon, tint = AMTagTint.Success)
AMTag { Text("v2.4.1", fontFamily = FontFamily.Monospace) }
```

`AMTag(label, modifier, icon: Painter? = null, tint = AMTagTint.Neutral)` and the slot form `AMTag(modifier, tint, content: RowScope.() -> Unit)`; the string form delegates.

- **`AMTagTint`**: `Neutral` (`onSurface` on `surfaceContainerHighest`) and `Info`, `Success`, `Warning`, `Destructive`, which take their `AMSemantic` role's `containerColor` and `onContainerColor`, the same colors a banner of that role uses.
- **The pill.** A full pill (`CircleShape`), padded `spacing.sm` at the sides and `spacing.xs` above and below, content `spacing.xs` apart and centered vertically, `labelMedium` through `ProvideTextStyle` and the content color through `LocalContentColor`, so custom content stays on-system. The glyph is 16 dp and decorative; the tag merges its semantics, so it reads as one piece.
- **Frosted app.** With `AMTheme.surfaceFill == Frosted` the pill is `amControlSurface(CircleShape, AMElevation.None)` (frost with its edge on the backdrop, the tint when embedded) with `frostedTagFill(container, dark)` over it: the tint's container at `Style.Frosted.FillLight` / `FillDark`'s alpha, so the frost or the tint shows through (`ArtWashTest.tagLabelsHoldOnFrostAndTint` holds the neutral label at 4.5:1 on both). A solid app clips and fills as the container color.
- **One line.** The string form's label is `maxLines = 1`, `softWrap = false`: a title beside it wraps instead. A row of tags wraps itself in an `AMFlowLayout` (`Selection.md`).
- It only draws; tapping and toggling are Material's chips.

## `AMBadge` and `AMBadgedBox` — `Badge.kt`

```kotlin
AMIconButton(painterResource(R.drawable.ic_mail), "Inbox", onClick = ::openInbox, badge = { AMBadge(unread) })
AMBadgedBox(count = 7) { Icon(painterResource(R.drawable.ic_mail), contentDescription = "Inbox") }
AMBadgedBox(tint = AMSemantic.Success) { AMAvatar(null, name = "Grace Hopper", size = AMAvatarSize.Small) }
```

- **`AMBadge(modifier, tint = AMSemantic.Destructive)`**: Material's small badge, a dot, in `tint.color`. Screen readers skip it (`clearAndSetSemantics {}`); what it means belongs on the thing it marks.
- **`AMBadge(count, modifier, contentDescription = "1 notification" / "N notifications", tint)`**: Material's badge with a number in `tint.onColor` on `tint.color`; past 99 it shows "99+"; at zero or below it emits nothing, so a live count goes straight in. A screen reader hears `contentDescription` (`clearAndSetSemantics`), which defaults to the full count of notifications, computed from `count`; a badge that counts something else names it (`contentDescription = "3 items"`).
- **`AMBadgedBox`**, dot and count forms (`(modifier, tint, content)` and `(count, modifier, contentDescription, tint, content)`, the count form handing its description to its badge): Material's `BadgedBox`, placing the badge at the content's top end corner. The badge slot is always there, so a count dropping to zero leaves the content exactly where it was.
- Material's look and sizes throughout (no ring, Material's own text scaling); the default tint is the scheme's error red. `AMIconButton`'s `badge` slot takes an `AMBadge` directly (`Actions.md`).

## `AMTimeline` — `Timeline.kt`

```kotlin
AMTimeline(steps, Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)), key = { it.id }) { step ->
    AMTimelineLabel(step.title, subtitle = step.time)
}
```

A vertical rail of entries: a marker per row, joined by a continuous line, the content beside it. Visual only, no surface: put it in a card padded `spacing.lg` or on a surface strip.

- **`AMTimeline(items, modifier, key = null, marker = { AMTimelineDot() }, content)`**: a `Column` of rows, each row's `AMTimelinePosition` from its index (`timelinePosition`: one item `Only`, then `First`, `Middle`…, `Last`), each keyed by `key` when given.
- **`AMTimelineRow(position, modifier, marker = { AMTimelineDot() }, content)`**, and the string form `AMTimelineRow(title, position, modifier, subtitle = null, dotColor = AMTheme.selectionRing, tabularFigures = false)` (marker and `AMTimelineLabel`, which takes `tabularFigures`), for rows built by hand; stack them in a column with no spacing.
- **`AMTimelinePosition`**: `First` (line below), `Middle` (above and below), `Last` (above), `Only` (none).
- **Layout** (one custom `Layout` of four slots: the line above, the marker in a `Box`, the line below, the content in a `Column`): the rail is as wide as the marker; the content sits `spacing.md` after it and fills the rest of the width; a row with a line below keeps `spacing.lg` under its content, which the line bridges into the next row; the last row keeps none. Lines are 2 dp in `outlineVariant`, with 2 dp of room at the marker, and are placed only where the position calls for them. Right to left mirrors it (`placeRelative`).
- **Marker centering.** The marker centers on the content's first line: the `TimelineFirstLine` alignment line (merged with `min`, so the topmost wins) when the content has one, otherwise half of `bodyLarge`'s line height. A marker taller than the line clamps to the row's top. **`Modifier.amTimelineFirstLine()`** marks a text as the entry's first line: a layout modifier that reports the center of the text's own first line, its height less the span between its first and last baselines, halved, so it follows the font scale and any style. `AMTimelineLabel`'s title marks itself.
- **`AMTimelineDot(modifier, color = AMTheme.selectionRing)`**: a 10 dp circle, decorative; a status timeline passes `AMSemantic.*.color`.
- **`AMTimelineLabel(title, modifier, subtitle = null, tabularFigures = false)`**: the title in `bodyLarge` / `onSurface` over the subtitle in `bodyMedium` / `AMTheme.secondaryText` (resolved from the surface it lies on, `Foundations.md`), the list row's pair. Use it inside custom content (a clickable entry). **`tabularFigures`** adds the OpenType `tnum` feature to both styles' `fontFeatureSettings` (kept alongside any features the style already sets, private `withFigures`), so every digit is as wide as the next and an agenda's times line up from entry to entry; text without digits looks the same either way. Off by default, the type scale's proportional figures.
- Markers in one timeline should share a width so the rail stays straight.

## `AMRemoteImage` — `RemoteImage.kt`

```kotlin
AMRemoteImage(url, contentDescription, modifier = Modifier, contentScale = ContentScale.Crop, fallback = { AMRemoteImageFallback() })
```

A picture loaded from the web, with a fallback slot, so no call site writes its own loading states.

- **Sizing.** It fills the size its `modifier` gives it, like a resizable picture (`fillMaxSize` after the caller's modifier): give it a size, a width and a height, or an aspect ratio. In a scrolling column it needs a height or an aspect ratio from outside; unbounded, it's zero long in that direction. The frame never changes when the picture arrives.
- **Shape.** It draws none; callers clip with `Modifier.clip(shape)`, which clips the fallback too.
- **Fallback.** Loading, a failed load and a null `url` all show `fallback`, laid over the whole frame (a `Box` with `propagateMinConstraints`). Only a loaded picture draws through Compose's `Image` with `contentScale`. The default `contentScale` is `Crop`, so slots never letterbox; `Fit` shows all of it.
- **No flash.** A decode of the URL already in memory shows on the first frame, before layout says how big the frame is (`ImagePipeline.latest`). After layout, the image asks for a decode at its frame's size only when the one showing isn't sharp enough for it (`DecodedImage.covers`), so a restored view (a row scrolled back, a page popped back to) never shows its fallback and doesn't reload. The picture state is keyed by the URL, so a changed URL never shows the previous picture. A failure is dropped and the fallback stays; the next time the image appears it tries again.
- **Accessibility.** The frame is one node whatever is showing (`clearAndSetSemantics`): with a `contentDescription` it's an image (`Role.Image`) with that description; with null it's decorative and screen readers skip it. A caller that wraps it in a clickable labels the clickable instead.
- **`AMRemoteImageFallback(modifier, icon = null)`**: the default fallback, public so a custom `fallback` can reuse it. A quiet tonal fill (`colorScheme.surfaceContainerHighest`) over the frame, with an optional glyph centered at half the frame's shorter side in `onSurfaceVariant`. The default has no glyph.

## `AMArtThumbnail` — `ArtThumbnail.kt`

```kotlin
AMArtThumbnail(url, contentDescription, modifier = Modifier, icon = painterResource(R.drawable.am_ic_image), size = AMAvatarSize.Medium.diameter)
AMArtThumbnail(image: ImageBitmap?, contentDescription, modifier, icon, size)
```

A row's leading art (a song, an album, an episode): `size` square (40 dp by default, a `Medium` avatar's; `AMAvatarSize.Large`'s 56 dp for a larger row) in `shapes.small`, the picture cropped to cover it. While there's no picture (loading, a failed load, a null `url`, a null `image`) it shows `AMRemoteImageFallback(icon = icon)`. The default glyph is the library's `image`; a media app passes its own (a music note, a podcast). The URL form is an `AMRemoteImage` (the pipeline, no flash on a row scrolled back); the bitmap form is for art the app already holds, null until it's there, like `amArtRow(image)`. A literal null first argument names its parameter (`url = null`), since both forms take one; the same holds for `AMArtCard`, `amArtBackdrop` and `amArtRow`. Each is one image node, decorative when `contentDescription` is null (the row's title names it).

## `AMArtCard` — `ArtCard.kt`

```kotlin
AMArtCard(url, contentDescription, modifier = Modifier, interactionSource: InteractionSource? = null)
AMArtCard(image: ImageBitmap?, contentDescription, modifier, interactionSource)
```

Art as a card, an album's or a show's hero or a grid tile: the picture cropped to the frame its `modifier` gives (it fills it, like `AMRemoteImage`, so callers give a size, or a width and an aspect ratio), in `shapes.large` with the `AMElevation.Low` shadow (`amElevation` then `clip`, so the shadow falls outside the shape), and the page color (`AMTheme.Surface.page`) in its place while there's no picture. The bitmap form takes art the app holds, null until it's there. The card takes no input: given the `interactionSource` of the control it belongs to (a tile whose art and title are one `clickable` with `indication = null`), it draws that control's ripple (`LocalIndication`) inside its shape, over the art. One image node, decorative when `contentDescription` is null.

Both pieces share `HeldImage` (`RemoteImage.kt`): `AMRemoteImage`'s frame for a bitmap (fill the frame, `propagateMinConstraints`, the picture with `ContentScale.Crop` or the fallback, one image node through `imageNode`).

## The image pipeline — `ImagePipeline.kt`

`ImagePipeline.shared(context)` is the one pipeline every `AMRemoteImage`, avatar and art picture uses. The first call starts it, which opens its disk cache on the IO dispatcher, so nothing touches the disk on the main thread.

**Its settings — `AMImagePipeline` (`ImagePipelineSettings.kt`)**, a public object, one per process, set early (`Application.onCreate`):

```kotlin
AMImagePipeline.userAgent = "Harbor/1.4 (https://example.com/harbor; harbor@example.com)"
AMImagePipeline.loader = { url -> coverStore.bytesFor(url) }
```

- **`userAgent: String?`**, null by default. Null sends the app's own, `defaultImageUserAgent(packageName, versionName, Build.VERSION.RELEASE)`: `com.example.harbor/1.4 (Android 16) AndroidMtrls` (a version's characters a product token can't hold become `-`, the release keeps what a comment can, no version leaves the package alone), built once, lazily on the first fetch, from the app's `PackageInfo`. A set value is checked (`isUserAgentValue`: printable ASCII, not blank) and a bad one throws `IllegalArgumentException`, leaving the last; it's `@Volatile` and read for each fetch, so a change applies from the next request. Cached pictures aren't refetched.
- **`loader: ((url: String) -> ByteArray?)?`**, null by default: the app's own picture store. Asked first for every picture not in memory, whatever the URL's scheme (an app's own included); it returns the picture's encoded bytes, or null to let the pipeline load the URL itself. Its bytes go through the same decode and memory cache, keyed by the URL, and are never copied to the disk cache. It runs on the IO dispatcher, never the main thread, inside `runInterruptible`, so it may block and is interrupted when every view waiting leaves; it may run on several threads at once, so it must be thread-safe; an exception it throws fails that load. `@Volatile` and read for each load. **The library owns no lasting offline store**: its disk cache is bounded, trimmed and in `cacheDir`, which the system may clear; keeping pictures offline is the app's job, handed over through `loader`.
- **`allowsLocalSources: Boolean`**, false by default: whether `file` and `content` URLs are read. Off, they fail like any unsupported scheme. It's opt-in because apps pass untrusted URLs to the pipeline (a podcast feed's artwork), and a `file` or `content` URL from there could make the app decode and show its own private files or content it may read. An app's own pictures go through `loader` (asked first whatever this says, under a scheme of the app's own); this is for an app that controls every picture URL it shows. `@Volatile` and read for each load; pictures already in memory still show.

- **Sources.** `resolveImageSource(url, allowsLocalSources, loader)`, one blocking call on the IO dispatcher per load: the loader's bytes (`ImageSource.App`) when it returns any, whatever the flag, otherwise `imageSourceOf(url, allowsLocalSources)` by the URL's scheme, matched without regard to case: `http` and `https` are `ImageSource.Network` (the disk cache, else the network, below). Only with the flag on: `file` is `ImageSource.LocalFile` (private `localFileOf`: `File(URI(url))`, escapes decoded, made canonical so `..` and symbolic links resolve, refused unless it's a regular file, never a directory, a pipe or a device); `content` is `ImageSource.Content`. Both are read on the device under a fetch's limits (below), never copied to the disk cache: a file through a `FileInputStream`, content through `ContentResolver.openAssetFileDescriptor(uri, "r", cancellationSignal)`, each length checked first. Any other scheme (`file` and `content` with the flag off), a URL with none, a `file` URL with a host, a query or a fragment, and a `file` path that isn't a regular file throw an `IOException`, a failure like any other.
- **Bytes.** For a network picture, `HttpURLConnection` on the IO dispatcher, 15 s to connect and 30 s to read, with a `User-Agent` read for each fetch: the app's `AMImagePipeline.userAgent`, or when it's null the one naming the app (above). Some hosts, Wikimedia among them, refuse requests without one or ask a client to name itself. Redirects are followed: same-protocol ones by the connection itself (Wikimedia's `Special:FilePath` redirects https to https), and a change of protocol by the pipeline, http up to https only, never down, at most five. Anything but a 2xx throws. Every connection sets `useCaches = false`, and nothing installs a process-wide `ResponseCache`, so the app's own HTTP traffic is untouched.
- **Limits on a fetch**, and on a `file` or `content` read on the device. A body is at most 32 MB: a `contentLengthLong` over that is refused before reading, and `readBounded` reads the stream in a loop that throws an `IOException` as soon as the running total passes the limit, holding at most one byte past it, so a server that omits or understates the length is held to it too (the announced length only sizes the first buffer). Each fetch, redirects included, and each read on the device has 60 s in all (`runAbortable`, a `withTimeoutOrNull` around `runInterruptible`). When the deadline passes, or every view waiting on the load leaves, the fetch's thread is interrupted and a watcher coroutine disconnects its connection through its `AbortSignal`, since an interrupt doesn't stop a read blocked on a socket; an abort that lands before the connection is under way is caught right after `connect()`. A read on the device is cut off the same way: a content provider's open through its `CancellationSignal` (its `OperationCanceledException` becomes an `InterruptedIOException`), then the file's or content's stream by closing it. A missed deadline throws an `IOException`, a failure like any other, never a cancellation.
- **Disk.** `ImageDiskCache`, the pipeline's own, in `cacheDir/am_image_cache/`: one file per URL, named by the SHA-256 of the URL in lowercase hex (`imageCacheFileName`). A load reads it before fetching (a stored body over 32 MB, or one that fails to decode, is dropped and the picture fetched); a fetched body is written once it decodes, in the pipeline's own scope so a view leaving doesn't stop it, to a temporary file renamed into place. 64 MB in all, least recently used first: a read touches the file's modified time, and after each write `imageCacheEvictions` picks the files to delete, oldest use first (ties by name) until the rest fit. Opening deletes temporary files a write left behind. All of it runs on the IO dispatcher, and no failure is fatal: a broken cache misses and the picture comes from the network. **Pictures are immutable per URL**, as every cache here is keyed: a stored body serves its URL until it's trimmed, whatever the server's cache headers say, so a new picture belongs at a new URL.
- **Decoding.** `ImageDecoder` on `Dispatchers.Default`, off the main thread, from the bytes in hand (fetched, from disk, from the loader or read on the device), at `decodeSize(source, frame, fit)`: the source scaled down, keeping its aspect, until it just covers the frame (`ImageFit.Cover`, for every `ContentScale` but `Fit` and `Inside`) or just fits inside it (`ImageFit.Contain`); never larger than the source; an empty frame asks for the source size. Over that rule sit absolute caps, whatever the fit and frame: at most 16,000,000 pixels and no side over 8192. A size past either becomes the source scaled down uniformly to the largest that meets both, each side at least a pixel, so an extreme aspect covering a square (1,000,000 × 100 → 8192 × 1) or a huge picture with no frame (20,000 square → 4000 square) never decodes whole. The capped size depends on the source alone, so every frame that needs more than the caps asks for the same one, and `DecodedImage.covers` (`decodeCovers`, against `decodeSize` for the new frame) counts a capped decode as covering them all: it's never reloaded for them. Undecodable bytes throw.
- **Memory.** `ImageMemoryCache`: an `androidx.collection.LruCache` bounded by the decoded bitmaps' bytes (64 MB), keyed by `ImageKey(url, width, height, fit)`, the frame in pixels. Beside it, an index of the most recent key stored per URL, so `latest(url)` finds a picture by URL alone; evicting that entry clears it from the index. Reads are synchronous and safe during composition. Failures are never stored.
- **One load per key.** `InFlightLoads`: callers asking for a key while it loads all await the same load, which runs in the pipeline's own scope. It's cancelled only once every caller waiting on it has been cancelled, so one image leaving never starves another showing the same picture. A finished load is never handed to a new caller, so after a failure the next call starts fresh.

## The art — `ArtBackdrop.kt`, `ArtImages.kt`, `ArtTone.kt`

Color under cards and readable text over art (`Patterns.md`). **The recipe**, baked into the processed picture: a blur of Android blur radius 207 dp (`AMArtDefaults.BlurRadius`, a Gaussian sigma of about 120 dp, so a picture reads as its colors); saturation 2.2 (private `ArtSaturation`); then **a frost toward the mode**: every pixel mixed evenly toward white in light mode by `lightWash`, toward black in dark mode by `darkWash`, in sRGB; then saturation again to win back the color the frost took, 1.8 after the light wash and 1.6 after the dark (private `ArtLightWashSaturation`, `ArtDarkWashSaturation`). Every boost fades with a lighter blur the same way (private `artBoost`: `1 + (boost − 1) × share`, `share` the blur over 207 dp, clamped to 0…1), so a sharp picture keeps its own color. **No picture means the page color**: loading, failed, none, and an art-rows region with no rows all show `AMTheme.Surface.page` for the mode (the nodes draw `Style.Page`'s value for `LocalAMDarkTheme`), never a fixed dark color. Between pictures the last one stays.

### `AMArtDefaults` and the frost — the numbers

`AMArtDefaults` (public object): `BlurRadius` 207 dp, `LightWash` 0.7, `DarkWash` 0.7. Text on art takes fixed inks by the art's tone, in either mode (`Style.ArtInk`), so it never depends on the color scheme. Measured over the full recipe (WCAG):

| | art | primary ink | secondary ink |
|---|---|---|---|
| dark art, brightest (a saturated yellow, blends included; a white cover lands on `#4D4D4D`) | `DarkArtCeiling` | white 8.42 | `#D1D1D6` 5.54 |
| light art, darkest (a black cover, frosted to `#B3B3B3`, and the blends beside it) | `LightArtGround` | black 9.98 | `#2C2C2E` 6.62 |

- **The extremes are internal constants** (`foundations/Style.kt`) the inks hold against, as relative luminance: `Style.ArtInk.LightArtGround` (0.4490) and `Style.ArtInk.DarkArtCeiling` (0.0747), measured over every color and every sRGB blend of two finished colors (bands fade into each other, and the GPU stretches pictures, in sRGB). `ArtWashTest` pins them to the recipe and holds the primary inks at 7:1 or more and the secondary at 4.5:1 or more against them.
- **Lower washes are the app's call** (KDoc on every wash parameter): they show more of the picture and give up the guarantee, so the app checks its text and gives the art surface the tone the art then has (`AMArtTone`). Frost over art the app chose not to wash, or drew itself, is the app's to check too.

### `Modifier.amArtBackdrop(url | image, blurRadius = AMArtDefaults.BlurRadius, lightWash = AMArtDefaults.LightWash, darkWash = AMArtDefaults.DarkWash)`

Each art modifier comes in two forms: one takes a `url: String?` the image pipeline loads, the other an `image: ImageBitmap?` the app already holds (a cover embedded in a music file; null means no art yet), and both go through one node and one recipe. The same holds for `Modifier.amArtRow` below. It's the one backdrop modifier: an album's page over its cover and a whole app's background alike.

```kotlin
Box(Modifier.fillMaxSize().amArtBackdrop(album.coverUrl)) { AlbumHeader(album) }
AMBackdrop(art = { Box(Modifier.fillMaxSize().amArtBackdrop(month.photoUrl)) }, Modifier.fillMaxSize()) { MonthView(month) }
Box(Modifier.fillMaxSize().amArtBackdrop(beansUrl, blurRadius = 4.dp)) { Shop() }   // its subject shows
Box(Modifier.fillMaxSize().amArtBackdrop(track.cover)) { NowPlaying(track) }   // cover: ImageBitmap?, null until read
```

- **A background.** It draws behind the element's content, filling it (private `ArtBackdropNode`), in order: the page color, the picture leaving (during a fade), the picture showing. It adds no semantics and takes no input.
- **The recipe** (private `ArtRecipe(blurRadius, saturation, lightWash, darkWash, lightWashSaturation, darkWashSaturation)`, made by `artRecipe(blurRadius, lightWash, darkWash)`): every boost through `artBoost` for the blur, the washes clamped to 0…1.
- **With frosted surfaces and components.** Frosted surfaces sample an `AMBackdrop`'s art, and the components in it take their over-art look for its tone, so a screen with frosted cards or headers makes the backdrop that `AMBackdrop`'s art (a `Box(Modifier.fillMaxSize().amArtBackdrop(url))` in `art`). The Panel page's album panel does this under `AMArtPanelChrome`.
- **Loading.** Once the node knows its size (`onRemeasured`) it asks `ArtImages` for its key (`artKey`, with the mode and that mode's wash); a picture in memory shows at once, otherwise it loads in the node's coroutine scope. It observes `LocalAMDarkTheme` (`ObserverModifierNode`), so a change of mode loads the picture washed for the new one, the old one showing until it's ready, then swapping without a fade. Until the first picture, on a failure of a new picture, and for a null source, the region shows the page color, and the first picture fades in over it; a failed resize keeps the picture showing.
- **Changing pictures.** A new source's picture (a new URL or a different bitmap) fades in over the old one, and a null source fades the old one out to the page color (an `Animatable` from 0 to 1 with `MaterialTheme.motionScheme.defaultEffectsSpec()`, read in the node); the old one draws under it until the fade ends. It snaps instead when nothing has drawn yet, when only the size or the mode changed, and when the source changed inside `amInstantSwitch` (the factory reads `InstantSwitch.generation`, as `amCrossfade` does). Remove animations makes the fade instant. A fade cut short by detaching ends in full on reattach.

### `Modifier.amArtRows(lightWash, darkWash)` and `Modifier.amArtRow(url)` / `Modifier.amArtRow(image)`

```kotlin
Column(Modifier.verticalScroll(rememberScrollState()).amArtRows()) {
    artists.forEach { ArtistEntry(it, Modifier.padding(horizontal = 16.dp).amArtRow(it.photoUrl)) }
}
```

- **The container** (`amArtRows`, private `ArtRowsNode`, a `TraversableNode`, holding the washes) goes on the scrolling content outside its side gutters. **Each row** (`amArtRow`, private `ArtRowNode`, a `GlobalPositionAwareModifierNode`) finds the nearest container when it attaches (`findNearestAncestor`) and registers; it tells the container to redraw only when its bounds within the container change (or its picture does), so scrolling the whole content doesn't redraw it. One page can mix URL rows and bitmap rows. A row outside a container draws nothing extra.
- **Rows take only the washes.** Bands always use the default blur (`AMArtDefaults.BlurRadius`) and the full boosts; `amArtRows` has no `blurRadius`.
- **Drawing**, behind the content: the page color, from `bleed` above the content to `bleed` below it, then one band per row in page order (sorted by top, measured with `localBoundingBoxOf`). The pictures carry their wash; nothing lies over them.
- **Bands** (`artRowBands`, unit-tested): each reaches `blend` (120 dp) past the midpoint to its neighbor on both sides, so neighbors meet halfway; each but the first fades in over its top `2 × blend` (capped at its height), over the band above; the first and last run `bleed` (1000 dp) past the content, for overscroll and short pages, so the scroll container's clip bounds them. Each band is its row's picture through the art recipe, cropped to the band's own frame (full width, band height), washed for the mode, with its fade baked in (`ArtRowsNode.drawBands(contentSize)`, which draws the bands alone in the container's frame and reads the mode, so a change of mode redraws them). **A band with no picture** (a null source, a failed picture, one loading with no last picture) is the page color as a band like any other (private `drawPageBand`): the page color over the band, faded in over its top as a picture's baked fade is (a vertical gradient from clear), so the band above blends into it and never ends in a line. It draws only where a picture drawn before it reaches below its top (`artBottom`); over the page color alone it has nothing to cover, so a page whose bands have no pictures is exactly the page color. A failed band isn't retried while the container stays attached.
- **A band keeps its last picture.** Each row keeps the picture its band showed last (`ArtRowNode.lastPicture`). When the band's key is new (a new source, a new band height as rows move, a new mode) and its picture isn't ready, the band draws the last one stretched to its new frame until it is, so a band never flashes the page color between pictures; only a band that never had a picture shows it. A null source clears it.
- On its own, the bands are the content's own background, which frosted surfaces can't sample; inside an `AMArtRowsBackdrop` they're that backdrop's art (below).

### `AMArtRowsBackdrop(modifier, tone = null, content)` — art rows as the backdrop

```kotlin
AMArtRowsBackdrop(Modifier.fillMaxSize()) {
    Scaffold(topBar = { AMPageHeader("Artists", scrollBehavior = scrollBehavior) }, containerColor = Color.Transparent) { padding ->
        Column(Modifier.verticalScroll(rememberScrollState()).amArtRows().padding(padding)) {
            artists.forEach { ArtistEntry(it, Modifier.padding(horizontal = 16.dp).amArtRow(it.photoUrl)) }
        }
    }
}
```

- **What it is.** An `AMBackdrop` whose art is the `amArtRows` page inside it: a `Box(modifier)` (recording the region whole into the source's screen layer, `backdropScreen`) holding a `matchParentSize` art box (`backdropSource` then private `ArtRowsArtNode`) behind `content`, which gets `LocalAMBackdrop` (the art's `AMBackdropSource`, `Foundations.md`), private `LocalArtRowsArt` (its `ArtRowsArt` registry) and `LocalAMArtTone` (`tone`, or the mode's). Like `AMBackdrop`, its box carries `artStatusBar(source, tone)` (the status bar's icons follow `tone` while the region reaches the top) and it reports itself to the scene through `sceneBackdrop(source)`, both `Navigation.md`. So every frosted surface in `content` samples the rows: a scrolled `AMPageHeader`'s frosted fill, frosted cards and buttons on the rows; an `AMSheet` opened from here blurs the page whole, the rows and what's on them (`Overlays.md`); and what lies on the rows reads the tone.
- **Handing over.** An `ArtRowsNode` that attaches where `LocalArtRowsArt` is set and its source is the nearest backdrop (`LocalAMBackdrop` is that same source) registers with it and draws nothing itself. Inside a sheet or an overlay window (which resets `LocalAMBackdrop`), or under an `AMBackdrop` of its own, a content keeps drawing its own bands. Rows still register with their nearest `amArtRows` container, as on any art-row page.
- **Drawing** (`ArtRowsArtNode`, cut to the region with `clipRect`): the page color over the whole region, then each registered content's bands translated to the content's place in the region (`localPositionOf`). The art box records it into the source's layer, so the colors also lie behind the top app bar's place and the system bars. **A region with no rows** (loading, empty: nothing registers) is the page color by itself, so an app keeps one `AMArtRowsBackdrop` whether or not it has rows, its scroll, header and an open sheet intact.
- **Following the scroll.** The content node is a `GlobalPositionAwareModifierNode`: when the content moves (a scroll), and when a row moves or its picture arrives (`changed()`), it invalidates the art node, which redraws the bands where they now are in the same frame. The art box itself stays put, so a frosted surface outside the content (the header) keeps its slice and shows the new colors through the referenced layer, and surfaces inside the content resample as they move.

### Making the pictures — `ArtImages`

- **Source** (`ArtSource`): `Url(url)`, or `Image(image)` for an app's `ImageBitmap`. Each has an `id` the pictures are keyed by: the URL, or an `ArtIdentity` of the Android bitmap under the `ImageBitmap` (`asAndroidBitmap()`), so re-wrapping the same bitmap (`asImageBitmap()` on each recomposition) finds the same picture. `ArtIdentity` holds its object through a `WeakReference` with its `System.identityHashCode`: equal to another identity of the same object while it lives, only to itself once it's gone, so a cached picture never keeps an app's bitmap alive. Two `Image` sources are equal when their identities are. The nodes hold the current source, and with it the bitmap, strongly. A bitmap is read as it stands: new art goes in a new bitmap.
- **Key** (`artKey(source, frame, blurRadius, saturation, dark, wash, washSaturation, fade)`, `ArtKey`): the source's `id`, the working size, the working blur, the saturation, the mode and its wash, the saturation after the wash, and the band's fade, the fractional ones in hundredths. The blur radius converts to the platform's sigma (`blurSigma`: `0.57735 × radius + 0.5`, 0 for no blur).
- **Working size** (`artWorkingSize`): the frame scaled so the blur is 8 working pixels, never above half the frame, never below a pixel. A heavy blur works a full screen at a few dozen pixels across; the GPU stretches it back (`FilterQuality.Low`, bilinear), which a picture blurred that much survives without seams.
- **The photo** (`ArtImages.load(pipeline, source, key)`): for a URL, from the image pipeline: `latest(url)` when that decode already covers the working size (so nothing new is indexed for the URL), otherwise `load(ImageKey(url, working size, Cover))`, a small decode. For an app's bitmap, the bitmap itself at its full size, which the area average below brings down to the working size; it's never changed or recycled.
- **Processing**, on `Dispatchers.Default` inside `ArtImages`' own `InFlightLoads`: a hardware bitmap is copied to software; the frame's shape is cut from its center (`coverCrop`) and box-averaged to the working size (`areaAverage`, premultiplied), never scaled up (a smaller photo works at its own size, the blur and fade scaled with it); a separable Gaussian with clamped edges (`gaussianBlur`); saturation through Compose's `ColorMatrix.setToSaturation` (`saturate`, clamped to alpha); **the frost toward the mode** (`washTowardMode`, below); saturation again (`saturate`); the band's fade (`fadeIn`); then an `ARGB_8888` bitmap, `prepareToDraw()`.
- **The wash** (internal, unit-tested in `ArtWashTest`): `washTowardMode(pixels, dark, wash)` mixes each premultiplied sRGB pixel evenly toward the mode, where the picture is: `color × (1 − wash) + wash × alpha` in light mode, `color × (1 − wash)` in dark, as the GPU composites a translucent white or black over the picture; a clear pixel stays clear, so the page shows exactly where there's no picture. A wash of 0 leaves the picture as it is.
- **Cache**: an `LruCache` of 16 MB by bitmap bytes. `cached(key)` is safe while drawing. Failures aren't kept.

### The art's tone — `AMArtTone`, `AMTheme.artTone` (`ArtTone.kt`)

- **`AMArtTone { Light, Dark }`**, named for the art (`Dark` art takes light text). The washes put the library's art in the mode's tone, so **the tone is the mode's** unless an art surface is given one. Nothing measures pictures.
- **Who provides it.** `AMBackdrop(art, modifier, tone = null, content)` and `AMArtRowsBackdrop(modifier, tone = null, content)` provide internal `LocalAMArtTone` (`compositionLocalOf`, so nodes reading it while drawing redraw on a change) to their content: `tone ?: modeArtTone()`. An app gives a surface a tone for art it washes less or draws itself (an unwashed photo: `tone = AMArtTone.Dark`). The spotlight provides the mode's tone around its title. Off art it's null; an overlay window's fresh context resets it to null (`ProvideOverlayWindowLocals`). Public read: `AMTheme.artTone`.
- **Who reads it**: art text, `AMTheme.secondaryText`, section headers and footers, empty states, ghost buttons, page headers, panel chrome and the status bar's icons, each described in its own area's digest.
- **The inks** (internal pure `artInk(tone, secondary)`, and a composable `artInk(secondary, tone)` that resolves the tone from the locals and calls it): `Style.ArtInk`'s fixed inks by tone, in either mode and whatever the scheme: `OnDark` white and `OnDarkSecondary` `#D1D1D6` on dark art, `OnLight` black and `OnLightSecondary` `#2C2C2E` on light art. `artInkShadow(tone)`: `Style.Shadow.ArtText` (black) under ink on dark art, `ArtTextLight` (the same layers in white) under ink on light art.
- **Colored text on art** (`artInkHolding(color, tone)`): the color moved along its own line (`moveToLuminance`: scaled down whole when lowered; raised, its own color grown by the square root of the lift as far as the gamut lets it), keeping its hue, just far enough to hold 4.5:1 on the library's art of that tone: darker on light art, against `LightArtGround`, lighter on dark art, against `DarkArtCeiling`. A destructive ghost button's label uses it (`Actions.md`). Light mode's `error` `#B3261E` (3.11:1 on the ground) comes back darker at 4.5:1; dark mode's `#F2B8B5` already holds (4.93:1) and stays.

### `Modifier.amArtText(tone = null, secondary = false)`

```kotlin
Text("Albums", Modifier.amArtText(), style = MaterialTheme.typography.titleLarge)
Text(album.artist, Modifier.amArtText(secondary = true))
Text("Paused", Modifier.amArtText(AMArtTone.Dark))   // white, on a scrim over a photo
```

Text that lies right on art, not on a card. Everything it covers, glyphs included, draws in the ink for the tone (keeping its alpha), with the tone's shadow drawn from the glyphs themselves. It's the foundations' glyph-shadow node (`Foundations.md`) given an `ArtGlyphs(tone, secondary)` request (internal `artGlyphs`), which resolves the tone (`tone`, else `LocalAMArtTone`, else the mode's), the ink and the shadow each time it draws, so a change of tone or mode redraws without recomposing. An explicit `tone` is for a ground the library can't know: a scrim over a photo (Card stack), an unwashed photo (Paused backdrop). Put it on a text or on a group of them.

## Catalog pages

`demo/.../catalog/pages/datadisplay/`, registered in `DataDisplayPages.entries` in this order (ids): `avatar`, `tag`, `badge`, `remoteImage`, `artThumbnail`, `artCard`, `artBackdrop`, `artRows`, `timeline`, followed by `CollectionsPages.entries` (`Collections.md`).

| Page | Preview | Loaded |
|---|---|---|
| Avatar (`AvatarPage.kt`) | `FullBleed`: three `Large` avatars on a surface strip: a photo, "Grace Hopper", "Alan Turing" | a card of five `AMListRow`s: "Apollo Guidance" with a `Large` rounded-square avatar (`shapes.medium`), Ada Lovelace's photo, Katherine Johnson with a count badge of 2 (`AMBadgedBox`), Margaret Heafield Hamilton, and "Invite pending" with the person glyph |
| Tag (`TagPage.kt`) | `FullBleed`: "Draft", "Active" (success) and "Blocked" (destructive, the filled `dangerous` octagon) in an `AMFlowLayout` on a surface strip | a frosted card over a photo (`CatalogFrostedStage`, padded `spacing.lg`): the Live, Delayed and Failed ("Search index") rows and four topic tags; then a release card: rows with trailing tags (Live, a monospace "v2.4.1" slot tag, Delayed, Failed, In review), then a row of six topic tags in an `AMFlowLayout` |
| Badge (`BadgePage.kt`) | `FullBleed`: three bells on a surface strip: a dot, 5, 128 ("99+") | a card of five shortcuts, each a mark over a `labelSmall` name, merged for TalkBack: Home, Alerts (dot), Inbox (7), Updates (128, info), Profile (a `Small` avatar with a success dot) |
| Remote image (`RemoteImagePage.kt`) | `FullBleed`: one 160 dp picture in `shapes.medium` on a surface strip | a card: the trip's cover (`CatalogArt.Peaks`, 160 dp tall, labeled), three 3:4 tiles (two photos, one empty with a `landscape` fallback glyph), then a "Coast" row with a 40 dp thumbnail and an "Untitled album" row with a `stacks` fallback |
| Art thumbnail (`ArtThumbnailPage.kt`) | `FullBleed`: one 40 dp thumbnail of `CatalogArt.Mountain` on a surface strip | a card of four recent tracks, `AMListRow`s with leading thumbnails: Above the Treeline / The Low Tides, Evening Light / Marigold and First Snow / Neon Atlas on `CatalogArt.Peaks`, `Forest` and `Garden` (following the appearance), then Untitled Demo / Voice Memo with no cover, showing the `music_note` placeholder |
| Art card (`ArtCardPage.kt`) | a 160 dp art card of `CatalogArt.Mountain` floating on the page | an album's header: a region in `shapes.large` over the "orchard" cover's art (`amArtBackdrop`), padded `spacing.xl`, the cover as a 200 dp art card over "Orchard" and "Field Notes · 9 songs · 38 min" (secondary) in art text |
| Art backdrop (`ArtBackdropPage.kt`) | two regions side by side, `spacing.md` apart, each at least 220 dp tall in `shapes.large` over a photo's art (`commonsPhoto`), its title and detail (secondary) in art text: "Pastel" / "Burano" over a bright one, "Blue hour" / "Islamabad" over a deep one | a 340 dp (at least) region over the chosen album's art: the cover, 120 dp in `shapes.medium` with the `Low` shadow (an `AMRemoteImage`, or the solid bitmap in an `Image`), the title and details in art text, and an `AMSegmentedControl` of four albums in the mode's own look (Orchard and Cobalt photos, Snow all white and Ink all black, `SolidArt`, the extremes the wash is tuned for); switching fades the art across. Under it, "Unwashed": the black cover at `lightWash = 0f, darkWash = 0f` in an `AMBackdrop(tone = AMArtTone.Dark)`, its art text white in both modes |
| Art rows (`ArtRowsPage.kt`) | none | `Fill`: a podcast feed of 240 episodes, newest first, in a 560 dp `LazyColumn` (clipped, `amArtRows()`) that composes only the rows on screen. Each row sits on its show's art (`amArtRow`, `CatalogArt` following the appearance: Latent Notes on `Peaks`, Distributed Diaries on `Forest`, Field Recordings on `Garden`, The Long Build on `Valley`, Small Hours on `Mountain`), blending into the next, inset dividers between: the show's art as an `AMArtThumbnail` (`ic_graphic_eq` until it loads) beside "240. Reasoning about reasoning" (the number, then one of twelve topics; the topics and the shows each cycle down the feed) in `bodyLarge` over the show's name in `bodyMedium`, both in art text (the name secondary), merged for TalkBack |
| Timeline (`TimelinePage.kt`) | `FullBleed`: a draft's four steps (`AMTimeline` of `AMTimelineLabel`s) on a surface strip | a parcel's tracking in a card padded `spacing.lg`: placed, payment (info dot), delayed with a "Weather" warning tag, "Now · 9:41 AM" in success over "Out for delivery" (`amTimelineFirstLine` on the status line), and a clickable "Delivery window" |

Demo glyphs only these pages use: `ic_notifications_unread`, `ic_wallpaper`, `ic_art_track`, `ic_timeline` (entry icons), `ic_home`, `ic_landscape`, `ic_rainy`, `ic_dangerous_filled` (Tag's "Blocked"). Art rows' placeholder, `ic_graphic_eq`, is shared with the Lists pages. Sample photos come from `catalogPhoto`, `commonsPhoto`, `CatalogArt` and `SolidArt`.

## See also

- `Foundations.md` — `AMBackdrop` and its source, the frosted fill, the glyph shadow art text draws with, `Style.ArtInk`.
- `Navigation.md` — the status bar following an art surface's tone, `AMArtPanelChrome`, the scene's page backdrops.
- `Patterns.md` — color under cards and readable text over art, the opinions this art serves.
