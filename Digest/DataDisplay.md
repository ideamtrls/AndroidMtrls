---
scope: Read-only display primitives — avatars, tags, badges, timelines, remote images and the image pipeline, art thumbnails and art cards, the art backdrop, its washes toward the mode and the art's tone, art rows and art text — and their catalog pages
globs: [androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/datadisplay/**, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/internal/ImagePipeline.kt, androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/internal/ImageDiskCache.kt, androidmtrls/src/main/res/drawable/am_ic_person.xml, androidmtrls/src/main/res/drawable/am_ic_image.xml, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/ImagePipelineTest.kt, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/ImageDiskCacheTest.kt, androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/DataDisplayMathTest.kt, demo/src/main/kotlin/com/ideamtrls/androidmtrls/demo/catalog/pages/datadisplay/**]
keywords: [AMArtDefaults, LightWash, DarkWash, lightWash, darkWash, wash toward the mode, floor, ceiling, washTowardMode, washToLuminance, artWashFloor, artWashCeiling, AMArtTone, LocalAMArtTone, artTone, artInk, artInkHolding, ArtTextLight, lastPicture, page color fallback, drawPageBand, artBottom, AMImagePipeline, userAgent, User-Agent, defaultImageUserAgent, loader, app's own store, offline store, allowsLocalSources, local sources opt-in, untrusted URL, ImageSource, resolveImageSource, imageSourceOf, localFileOf, file URL, canonical path, regular file, content URI, ContentResolver, openAssetFileDescriptor, CancellationSignal, local picture, AMAvatar, AMAvatarSize, diameter, initials, avatarInitials, BreakIterator, hashed hue, avatarHue, djb2, person glyph, AMTag, AMTagTint, Neutral, pill, labelMedium, AMBadge, AMBadgedBox, Badge, BadgedBox, dot, count, 99+, notifications, badge contentDescription, items, AMTimeline, AMTimelineRow, AMTimelineDot, AMTimelineLabel, tabularFigures, tnum, fontFeatureSettings, AMTimelinePosition, timelinePosition, amTimelineFirstLine, TimelineFirstLine, alignment line, marker centering, rail, AMRemoteImage, AMRemoteImageFallback, fallback, AMArtThumbnail, thumbnail, placeholder glyph, am_ic_image, AMArtCard, art card, interactionSource, indication, HeldImage, ImageBitmap, contentDescription, decorative image, ContentScale, Crop, Fit, image pipeline, ImagePipeline, HttpURLConnection, redirects, User-Agent, ImageDecoder, setTargetSize, decodeSize, decode caps, 16 megapixels, 8192, decodeCovers, ImageFit, ImageKey, DecodedImage, ImageMemoryCache, LruCache, latest, InFlightLoads, request deduplication, readBounded, body limit, 32 MB, contentLengthLong, deadline, runAbortable, AbortSignal, disconnect, ImageDiskCache, am_image_cache, SHA-256, imageCacheFileName, imageCacheEvictions, ImageCacheFile, least recently used, useCaches, ResponseCache, immutable per URL, no flash, Modifier.amArtBackdrop, Modifier.Modifier.amArtRows, Modifier.amArtRow, AMArtRowsBackdrop, ArtRowsArt, ArtRowsArtNode, LocalArtRowsArt, ArtFallback, dark neutral, amArtText, readable text over art, ImageBitmap, bitmap art, embedded cover, ArtSource, ArtIdentity, identity key, WeakReference, art recipe, blur, saturation, ColorMatrix, wash, crossfade, ArtImages, ArtKey, artWorkingSize, blurSigma, ArtCrop, coverCrop, areaAverage, gaussianBlur, artRowBands, bands, blend, bleed, AMBackdrop, frosted, color under cards, DataDisplayPages, catalog pages]
updated: 2026-11-14
---

# Data display

Read-only display: `AMAvatar` (a photo, or initials on a hashed hue), `AMTag` (a small tinted pill), `AMBadge` and `AMBadgedBox` on Material's `Badge`, `AMTimeline` and its rows, `AMRemoteImage` over the in-house image pipeline, `AMArtThumbnail` and `AMArtCard` (art in a row and as a card, from a URL or a bitmap), and the art pieces that put color under cards: `Modifier.amArtBackdrop`, `Modifier.amArtRows` / `Modifier.amArtRow` with `AMArtRowsBackdrop` (the rows as the art frosted surfaces sample), all washed toward the mode (`AMArtDefaults`), the art's tone (`AMArtTone`) and `Modifier.amArtText`. Everything here draws and owns no data; the only state is what a picture needs to load.

## Where things live

Library sources under `androidmtrls/src/main/kotlin/com/ideamtrls/androidmtrls/` (package `com.ideamtrls.androidmtrls`):

| File | What |
|---|---|
| `datadisplay/Avatar.kt` | `AMAvatarSize`, `AMAvatar`; internal `avatarInitials`, `avatarHue` |
| `datadisplay/Tag.kt` | `AMTagTint`, `AMTag` (string and slot forms) |
| `datadisplay/Badge.kt` | `AMBadge` (dot and count forms), `AMBadgedBox` (dot and count forms) |
| `datadisplay/Timeline.kt` | `AMTimelinePosition`, `AMTimeline`, `AMTimelineRow` (slot and string forms), `AMTimelineDot`, `AMTimelineLabel`, `Modifier.amTimelineFirstLine`; internal `timelinePosition` |
| `datadisplay/RemoteImage.kt` | `AMRemoteImage`, `AMRemoteImageFallback`; internal `HeldImage` (the same frame for a bitmap); private `imageNode` |
| `datadisplay/ArtThumbnail.kt` | `AMArtThumbnail` (a URL and an `ImageBitmap` form) |
| `datadisplay/ArtCard.kt` | `AMArtCard` (a URL and an `ImageBitmap` form); private `artCardFrame`, `ArtCardLoading` |
| `datadisplay/ImagePipelineSettings.kt` | `AMImagePipeline` (the pipeline's process-wide settings: `userAgent`, `loader`, `allowsLocalSources`); internal `isUserAgentValue`, `defaultImageUserAgent` |
| `datadisplay/ArtBackdrop.kt` | `AMArtDefaults`, `Modifier.amArtBackdrop`, `Modifier.amArtRow` (each in a URL and an `ImageBitmap` form), `Modifier.amArtRows`, `AMArtRowsBackdrop`, `Modifier.amArtText`; the art recipe's constants, the private nodes that draw them, and the art-rows backdrop's private `ArtRowsArt`, `LocalArtRowsArt` and `ArtRowsArtNode` |
| `datadisplay/ArtTone.kt` | `AMArtTone`; internal `LocalAMArtTone`, `modeArtTone`, `artToneOf`, `artInk` (plain and composable), `artInkShadow`, `artInkHolding` |
| `datadisplay/ArtImages.kt` | internal `ArtSource` (`Url`, `Image`), `ArtIdentity`, `ArtImages` (the processed-picture cache and loader), `ArtKey`, `ArtWorkingSize`, `artWorkingSize`, `blurSigma`, `artKey`, `ArtCrop`, `coverCrop`, `areaAverage`, `gaussianBlur`, `saturate`, `srgbToLinear`, `linearToSrgb`, `linearLuminance`, `artWashFloor`, `artWashCeiling`, `washToLuminance`, `washTowardMode`, `fadeIn`, `ArtSpan`, `ArtBand`, `artRowBands` |
| `internal/ImagePipeline.kt` | internal `ImagePipeline`, `ImageSource`, `resolveImageSource`, `imageSourceOf`, `ImageFit`, `ImageKey`, `DecodedImage`, `decodeSize`, `decodeCovers`, `readBounded`, `AbortSignal`, `runAbortable`, `ImageMemoryCache`, `InFlightLoads` |
| `internal/ImageDiskCache.kt` | internal `ImageDiskCache`, `imageCacheFileName`, `isImageCacheFileName`, `ImageCacheFile`, `imageCacheEvictions` |

- Glyphs: `androidmtrls/src/main/res/drawable/am_ic_person.xml` (the filled `person`, an avatar with no name) and `am_ic_image.xml` (`image`, an art thumbnail's default placeholder). Tags in the status tints use `AMSemantic.icon`'s glyphs (`Foundations.md`).
- Unit tests, in `androidmtrls/src/test/kotlin/com/ideamtrls/androidmtrls/`: `ImagePipelineTest` (the pipeline: its sources, local ones only when allowed, and the loader's place before them, the User-Agent, decode sizes, bounded reads, aborts, the memory cache, shared loads), `ImageDiskCacheTest` (its disk cache's file names and trimming) and `ArtWashTest` (the wash toward the mode: the floor and ceiling, black and white covers, colors kept on the right side and their hue, the defaults holding secondary text at 4.5:1 over every blend of a palette, colored ink moved to hold) and `DataDisplayMathTest` (the avatar's initials and hue, the timeline's positions, the art's keys (a bitmap's identity, the mode and the wash among them), working size, crop, average, blur and fade, and the art rows' bands) (`Testing.md`).
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
- **`AMTimelineLabel(title, modifier, subtitle = null, tabularFigures = false)`**: the title in `bodyLarge` / `onSurface` over the subtitle in `bodyMedium` / `AMTheme.secondaryText` (the frosted secondary on glass), the list row's pair. Use it inside custom content (a clickable entry). **`tabularFigures`** adds the OpenType `tnum` feature to both styles' `fontFeatureSettings` (kept alongside any features the style already sets, private `withFigures`), so every digit is as wide as the next and an agenda's times line up from entry to entry; text without digits looks the same either way. Off by default, the type scale's proportional figures.
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

A row's leading art (a song, an album, an episode): `size` square (40 dp by default, a `Medium` avatar's; `AMAvatarSize.Large`'s 56 dp for a larger row) in `shapes.small`, the picture cropped to cover it. While there's no picture (loading, a failed load, a null `url`, a null `image`) it shows `AMRemoteImageFallback(icon = icon)`: the quiet tonal fill with the glyph at half the side in `onSurfaceVariant`. The default glyph is the library's `image`; a media app passes its own (a music note, a podcast). The URL form is an `AMRemoteImage` (the pipeline, no flash on a row scrolled back); the bitmap form is for art the app already holds, null until it's there, like `amArtRow(image)`. A literal null first argument names its parameter (`url = null`), since both forms take one. Each is one image node, decorative when `contentDescription` is null (the row's title names it).

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
- **`loader: ((url: String) -> ByteArray?)?`**, null by default: the app's own picture store. Asked first for every picture not in memory, whatever the URL's scheme (an app's own included); it returns the picture's encoded bytes, or null to let the pipeline load the URL itself. Its bytes go through the same decode and memory cache, keyed by the URL, and are never copied to the disk cache. It runs on the IO dispatcher, never the main thread, inside `runInterruptible`, so it may block and is interrupted when every view waiting leaves; it may run on several threads at once, so it must be thread-safe; an exception it throws fails that load. `@Volatile` and read for each load. **The library owns no lasting offline store**: its disk cache is bounded, trimmed and in `cacheDir`, which the system may clear; keeping pictures offline is the app's job, handed over through `loader`, as `AMImagePipeline`'s and `loader`'s KDoc say.
- **`allowsLocalSources: Boolean`**, false by default: whether `file` and `content` URLs are read. Off, they fail like any unsupported scheme. It's opt-in because apps pass untrusted URLs to the pipeline (a podcast feed's artwork), and a `file` or `content` URL from there could make the app decode and show its own private files or content it may read. The KDoc says to serve the app's own pictures through `loader` (asked first whatever this says, under a scheme of the app's own) and to turn this on only when the app controls every picture URL it shows. `@Volatile` and read for each load; pictures already in memory still show.

- **Sources.** `resolveImageSource(url, allowsLocalSources, loader)`, one blocking call on the IO dispatcher per load: the loader's bytes (`ImageSource.App`) when it returns any, whatever the flag, otherwise `imageSourceOf(url, allowsLocalSources)` by the URL's scheme, matched without regard to case: `http` and `https` are `ImageSource.Network` (the disk cache, else the network, below). Only with the flag on: `file` is `ImageSource.LocalFile` (private `localFileOf`), the path from `File(URI(url))` with its escapes decoded, made canonical (`..` and symbolic links resolved), and refused unless that canonical path `isFile`, a regular file, not missing, a directory or a special file such as a pipe or a device; `content` is `ImageSource.Content`. Both are read on the device under a fetch's limits (below) and decoded from the bytes, never copied to the disk cache: a file through a `FileInputStream`, its length checked first; content through `ContentResolver.openAssetFileDescriptor(uri, "r", cancellationSignal)`, its declared length checked first. Any other scheme (`file` and `content` with the flag off), a URL with none, a `file` URL with a host, a query or a fragment, and a `file` path that isn't a regular file throw an `IOException`, a failure like any other.
- **Bytes.** For a network picture, `HttpURLConnection` on the IO dispatcher, 15 s to connect and 30 s to read, with a `User-Agent` read for each fetch: the app's `AMImagePipeline.userAgent`, or when it's null the one naming the app (below). Some hosts, Wikimedia among them, refuse requests without one or ask a client to name itself. Redirects are followed: same-protocol ones by the connection itself (Wikimedia's `Special:FilePath` and picsum both redirect https to https), and a change of protocol by the pipeline, http up to https only, never down, at most five. Anything but a 2xx throws. Every connection sets `useCaches = false`, and nothing installs a process-wide `ResponseCache`, so the app's own HTTP traffic is untouched.
- **Limits on a fetch**, and on a `file` or `content` read on the device. A body is at most 32 MB: a `contentLengthLong` over that is refused before reading, and `readBounded` reads the stream in a loop that throws an `IOException` as soon as the running total passes the limit, holding at most one byte past it, so a server that omits or understates the length is held to it too (the announced length only sizes the first buffer). Each fetch, redirects included, and each read on the device has 60 s in all (`runAbortable`, a `withTimeoutOrNull` around `runInterruptible`). When the deadline passes, or every view waiting on the load leaves, the fetch's thread is interrupted and a watcher coroutine disconnects its connection through its `AbortSignal`, since an interrupt doesn't stop a read blocked on a socket; an abort that lands before the connection is under way is caught right after `connect()`. A read on the device is cut off the same way: a content provider's open through its `CancellationSignal` (its `OperationCanceledException` becomes an `InterruptedIOException`), then the file's or content's stream by closing it. A missed deadline throws an `IOException`, a failure like any other, never a cancellation.
- **Disk.** `ImageDiskCache`, the pipeline's own, in `cacheDir/am_image_cache/`: one file per URL, named by the SHA-256 of the URL in lowercase hex (`imageCacheFileName`). A load reads it before fetching (a stored body over 32 MB, or one that no longer decodes, is dropped and the picture fetched); a fetched body is written once it decodes, in the pipeline's own scope so a view leaving doesn't stop it, to a temporary file renamed into place. 64 MB in all, least recently used first: a read touches the file's modified time, and after each write `imageCacheEvictions` picks the files to delete, oldest use first (ties by name) until the rest fit. Opening deletes temporary files a write left behind. All of it runs on the IO dispatcher, and no failure is fatal: a broken cache misses and the picture comes from the network. **Pictures are immutable per URL**, as every cache here is keyed: a stored body serves its URL until it's trimmed, whatever the server's cache headers say, so a new picture belongs at a new URL.
- **Decoding.** `ImageDecoder` on `Dispatchers.Default`, off the main thread, from the bytes in hand (fetched, from disk, from the loader or read on the device), at `decodeSize(source, frame, fit)`: the source scaled down, keeping its aspect, until it just covers the frame (`ImageFit.Cover`, for every `ContentScale` but `Fit` and `Inside`) or just fits inside it (`ImageFit.Contain`); never larger than the source; an empty frame asks for the source size. Over that rule sit absolute caps, whatever the fit and frame: at most 16 megapixels (16,000,000) and no side over 8192 px. A size past either becomes the source scaled down uniformly to the largest that meets both, each side at least a pixel, so an extreme aspect covering a square (1,000,000 × 100 → 8192 × 1) or a huge picture with no frame (20,000 square → 4000 square) never decodes whole. The capped size depends on the source alone, so every frame that needs more than the caps asks for the same one, and `DecodedImage.covers` (`decodeCovers`, against `decodeSize` for the new frame) counts a capped decode as covering them all: it's never reloaded for them. Undecodable bytes throw.
- **Memory.** `ImageMemoryCache`: an `androidx.collection.LruCache` bounded by the decoded bitmaps' bytes (64 MB), keyed by `ImageKey(url, width, height, fit)`, the frame in pixels. Beside it, an index of the most recent key stored per URL, so `latest(url)` finds a picture by URL alone; evicting that entry clears it from the index. Reads are synchronous and safe during composition. Failures are never stored.
- **One load per key.** `InFlightLoads`: callers asking for a key while it loads all await the same load, which runs in the pipeline's own scope. It's cancelled only once every caller waiting on it has been cancelled, so one image leaving never starves another showing the same picture. A finished load is never handed to a new caller, so after a failure the next call starts fresh.

## The art — `ArtBackdrop.kt`, `ArtImages.kt`, `ArtTone.kt`

Color under cards and readable text over art (`Patterns.md`). **The recipe**: a blur of Android blur radius 207 dp (`AMArtDefaults.BlurRadius`, a Gaussian sigma of about 120 dp, so a picture reads as its colors), saturation 2.2 (private `ArtSaturation`, fading with a lighter blur), then **a wash toward the mode**, baked into the processed picture: in light mode every color darker than black washed with white at `lightWash` is lifted to that luminance, in dark mode every color brighter than white washed with black at `darkWash` is lowered to it. **No picture means the page color**: loading, failed, none, between pictures, and an art-rows region with no rows all show `AMTheme.Surface.page` for the mode, never a fixed dark color.

### `AMArtDefaults` and the wash — the numbers

`AMArtDefaults` (public object): `BlurRadius` 207 dp, `LightWash` 0.75, `DarkWash` 0.68. They're tuned so text in the mode's own colors holds 4.5:1 over **any** art, the secondary color included, against Material's baseline static scheme (an app with its own scheme checks its `onSurfaceVariant` against them; the defaults are fixed, not computed from the scheme). Measured (WCAG, Material 3 1.5.0-rc01 baseline: light `onSurface` `#1D1B20`, `onSurfaceVariant` `#49454F`; dark `#E6E0E9`, `#CAC4D0`):

| | washed ground | `onSurfaceVariant` | `onSurface` |
|---|---|---|---|
| light, black cover | `#BFBFBF` (L 0.5225) | 5.09 | 9.31 |
| light, worst blend of two lifted colors (a bright green into a pink, 50/50 in sRGB) | L 0.4646 | 4.58 | 8.37 |
| light, white cover | `#FFFFFF` | 9.34 | 17.1 |
| dark, white cover | `#525252` (L 0.0835) | 4.61 | 6.07 |
| dark, any blend | never brighter than the ceiling | 4.61 | 6.07 |
| dark, black cover | `#000000` | 12.3 | 16.2 |

- **Why a floor and a ceiling, not a flat wash.** A flat white wash reaching the same light target (0.705) grays every color, the light ones too; the floor moves only the colors darker than it and keeps their hue: red → `#FFA8A8`, mid blue `#2F6FD6` → `#A8BEFF`, navy `#1A237E` → `#B7BAFF`, green `#2E9E4F` → `#7AD28E`, yellow stays. In dark mode the ceiling scales a color down whole: white → `#525252`, red → `#A80000`, mid blue → `#204F9C`, yellow → `#5A530E`, navy stays.
- **Blends.** Bands fade into each other, and the GPU stretches pictures, in sRGB, where a mix of two lifted colors can dip below the floor (luminance is convex in sRGB values). The ceiling can't be exceeded by a mix. So the light wash keeps a margin: over a palette of primaries, secondaries, mids and darks, each also saturated 2.2× as the art is, every pair at every 1/40 mix, the darkest blend at 0.75 is L 0.4646 (4.58:1); at 0.74 it's 4.42:1. The lift grows a color's own color by the square root of the lift (at most 4×, so a near-black's faint tint doesn't bloom): a full lift kept more color but dipped to 4.25:1. `ArtWashTest` pins all of it.
- **Lower washes are the app's call** (KDoc on every wash parameter): they show more of the picture and give up the guarantee, so the app checks its text and gives the art surface the tone the art then has (`AMArtTone`). Frosted glass over art the app chose not to wash, or drew itself, is the app's to check too.

### `Modifier.amArtBackdrop(url | image, blurRadius = AMArtDefaults.BlurRadius, lightWash = AMArtDefaults.LightWash, darkWash = AMArtDefaults.DarkWash)`

Each art modifier comes in two forms: one takes a `url: String?` the image pipeline loads, the other an `image: ImageBitmap?` the app already holds (a cover embedded in a music file; null means no art yet), and both go through one node and one recipe. The same holds for `Modifier.amArtRow` below. A literal `null` matches both forms, so pass a typed value (or name the parameter). It's the one backdrop modifier: an album's page over its cover and a whole app's background alike.

```kotlin
Box(Modifier.fillMaxSize().amArtBackdrop(album.coverUrl)) { AlbumHeader(album) }
AMBackdrop(art = { Box(Modifier.fillMaxSize().amArtBackdrop(month.photoUrl)) }, Modifier.fillMaxSize()) { MonthView(month) }
Box(Modifier.fillMaxSize().amArtBackdrop(beansUrl, blurRadius = 4.dp)) { Shop() }   // its subject shows
Box(Modifier.fillMaxSize().amArtBackdrop(track.cover)) { NowPlaying(track) }   // cover: ImageBitmap?, null until read
```

- **A background.** It draws behind the element's content, filling it (private `ArtBackdropNode`: a draw, layout-aware, composition-local-consuming and observer node), in order: the page color, the picture leaving (during a fade), the picture showing. It adds no semantics and takes no input.
- **The recipe** (private `ArtRecipe(blurRadius, saturation, lightWash, darkWash)`, `artRecipe`): the saturation fades with the blur, `1 + 1.2 × min(blurRadius / 207 dp, 1)`, so a sharp picture keeps its own color; the washes are clamped to 0…1.
- **With frosted surfaces and components.** Frosted surfaces sample an `AMBackdrop`'s art, and the components in it take their over-art look for its tone, so a screen with frosted cards or headers makes the backdrop that `AMBackdrop`'s art (a `Box(Modifier.fillMaxSize().amArtBackdrop(url))` in `art`). The Panel page's album panel does this under `AMArtPanelChrome`.
- **Loading.** Once the node knows its size (`onRemeasured`) it asks `ArtImages` for its key (`artKey`, with the mode and that mode's wash); a picture in memory shows at once, otherwise it loads in the node's coroutine scope. It observes `LocalAMDarkTheme` (`ObserverModifierNode`), so a change of mode loads the picture washed for the new one, the old one showing until it's ready, then swapping without a fade. Until the first picture, on a failure of a new picture, and for a null source, the region shows the page color, and the first picture fades in over it; a failed resize keeps the picture showing.
- **Changing pictures.** A new source's picture (a new URL or a different bitmap) fades in over the old one (an `Animatable` from 0 to 1 with `MaterialTheme.motionScheme.defaultEffectsSpec()`, read in the node); the old one draws under it until the fade ends. It snaps instead when nothing has drawn yet, when only the size or the mode changed, and when the source changed inside `amInstantSwitch` (the factory reads `InstantSwitch.generation`, as `amCrossfade` does). Remove animations makes the fade instant. A fade cut short by detaching ends in full on reattach.

### `Modifier.amArtRows(lightWash, darkWash)` and `Modifier.amArtRow(url)` / `Modifier.amArtRow(image)`

```kotlin
Column(Modifier.verticalScroll(rememberScrollState()).amArtRows()) {
    artists.forEach { ArtistEntry(it, Modifier.padding(horizontal = 16.dp).amArtRow(it.photoUrl)) }
}
```

- **The container** (`amArtRows`, private `ArtRowsNode`, a `TraversableNode`, holding the washes) goes on the scrolling content outside its side gutters. **Each row** (`amArtRow`, private `ArtRowNode`, a `GlobalPositionAwareModifierNode`) finds the nearest container when it attaches (`findNearestAncestor`) and registers; it tells the container to redraw only when its bounds within the container change (or its picture does); one page can mix URL rows and bitmap rows, so scrolling the whole content doesn't redraw it. A row outside a container draws nothing extra.
- **Drawing**, behind the content: the page color, from `bleed` above the content to `bleed` below it, then one band per row in page order (sorted by top, measured with `localBoundingBoxOf`). The pictures carry their wash; nothing lies over them.
- **Bands** (`artRowBands`, unit-tested): each reaches `blend` (120 dp) past the midpoint to its neighbor on both sides, so neighbors meet halfway; each but the first fades in over its top `2 × blend` (capped at its height), over the band above; the first and last run `bleed` (1000 dp) past the content, for overscroll and short pages, so the scroll container's clip bounds them. Each band is its row's picture through the art recipe, cropped to the band's own frame (full width, band height), washed for the mode, with its fade baked in (`ArtRowsNode.drawBands(contentSize)`, which draws the bands alone in the container's frame and reads the mode, so a change of mode redraws them). **A band with no picture** (a null source, a failed picture, one loading with no last picture) is the page color as a band like any other (private `drawPageBand`): the page color over the band, faded in over its top as a picture's baked fade is (a vertical gradient from clear), so the band above blends into it and never ends in a line. It draws only where a picture drawn before it reaches below its top (`artBottom`); over the page color alone it has nothing to cover, so a page whose bands have no pictures is exactly the page color. A failed band isn't retried while the container lives.
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

- **What it is.** An `AMBackdrop` whose art is the `amArtRows` page inside it: a `Box(modifier)` holding a `matchParentSize` art box (`backdropSource` then private `ArtRowsArtNode`) behind `content`, which gets `LocalAMBackdrop` (the art's `AMBackdropSource`, `Foundations.md`), private `LocalArtRowsArt` (its `ArtRowsArt` registry) and `LocalAMArtTone` (`tone`, or the mode's). So every frosted surface in `content` samples the rows: a collapsing `AMPageHeader`'s frosted fill, frosted cards and buttons on the rows, a frosted `AMSheet` opened from here; and what lies on the rows reads the tone.
- **Handing over.** An `ArtRowsNode` that attaches where `LocalArtRowsArt` is set and its source is the nearest backdrop (`LocalAMBackdrop` is that same source) registers with it and draws nothing itself. Inside a sheet or an overlay window (which resets `LocalAMBackdrop`), or under an `AMBackdrop` of its own, a content keeps drawing its own bands. Rows still register with their nearest `amArtRows` container, as on any art-row page.
- **Drawing** (`ArtRowsArtNode`, cut to the region with `clipRect`): the page color over the whole region, then each registered content's bands translated to the content's place in the region (`localPositionOf`). The art box records it into the source's layer, so the colors also lie behind the top app bar's place and the system bars. **A region with no rows** (loading, empty: nothing registers) is the page color by itself, so an app keeps one `AMArtRowsBackdrop` whether or not it has rows, its scroll, header and an open sheet intact.
- **Following the scroll.** The content node is a `GlobalPositionAwareModifierNode`: when the content moves (a scroll), and when a row moves or its picture arrives (`changed()`), it invalidates the art node, which redraws the bands where they now are in the same frame. The art box itself stays put, so a frosted surface outside the content (the header) keeps its slice and shows the new colors through the referenced layer, and surfaces inside the content resample as they move.

### Making the pictures — `ArtImages`

- **Source** (`ArtSource`): `Url(url)`, or `Image(image)` for an app's `ImageBitmap`. Each has an `id` the pictures are keyed by: the URL, or an `ArtIdentity` of the Android bitmap under the `ImageBitmap` (`asAndroidBitmap()`), so re-wrapping the same bitmap (`asImageBitmap()` on each recomposition) finds the same picture. `ArtIdentity` holds its object through a `WeakReference` with its `System.identityHashCode`: equal to another identity of the same object while it lives, only to itself once it's gone, so a cached picture never keeps an app's bitmap alive. Two `Image` sources are equal when their identities are. The nodes hold the current source, and with it the bitmap, strongly. A bitmap is read as it stands: new art goes in a new bitmap.
- **Key** (`artKey(source, frame, blurRadius, saturation, dark, wash, fade)`, `ArtKey`): the source's `id`, the working size, the working blur, the saturation, the mode and its wash, and the band's fade, the fractional ones in hundredths. The blur radius converts to the platform's sigma (`blurSigma`: `0.57735 × radius + 0.5`, 0 for no blur).
- **Working size** (`artWorkingSize`): the frame scaled so the blur is 8 working pixels, never above half the frame, never below a pixel. A heavy blur works a full screen at a few dozen pixels across; the GPU stretches it back (`FilterQuality.Low`, bilinear), which a picture blurred that much survives without seams.
- **The photo** (`ArtImages.load(pipeline, source, key)`): for a URL, from the image pipeline: `latest(url)` when that decode already covers the working size (so nothing new is indexed for the URL), otherwise `load(ImageKey(url, working size, Cover))`, a small decode. For an app's bitmap, the bitmap itself at its full size, which the area average below brings down to the working size; it's never changed or recycled.
- **Processing**, on `Dispatchers.Default` inside `ArtImages`' own `InFlightLoads`: a hardware bitmap is copied to software; the frame's shape is cut from its center (`coverCrop`) and box-averaged to the working size (`areaAverage`, premultiplied), never scaled up (a smaller photo works at its own size, the blur and fade scaled with it); a separable Gaussian with clamped edges (`gaussianBlur`); saturation through Compose's `ColorMatrix.setToSaturation` (`saturate`, clamped to alpha); **the wash toward the mode** (`washTowardMode`, below); the band's fade (`fadeIn`); then an `ARGB_8888` bitmap, `prepareToDraw()`.
- **The wash** (internal, unit-tested in `ArtWashTest`): `artWashFloor(lightWash)` is the luminance of gray `lightWash` (`srgbToLinear`), `artWashCeiling(darkWash)` that of gray `1 − darkWash`. `washTowardMode(pixels, dark, wash)` unpremultiplies each pixel, takes it to linear light and calls `washToLuminance(rgb, target, raise)`: a color on the wrong side of the target moves along its own line through gray, `target + k × (color − luminance)`, landing on the target exactly (its luminance part is the target, its color part has none). Lowered, `k = target / luminance`: the color scaled down whole, its chromaticity exact. Raised, `k = √(target / luminance)`, at most `MaxLift` (4), cut so no channel leaves 0…1. A wash of 0 leaves the picture as it is.
- **Cache**: an `LruCache` of 16 MB by bitmap bytes. `cached(key)` is safe while drawing. Failures aren't kept.

### The art's tone — `AMArtTone`, `AMTheme.artTone` (`ArtTone.kt`)

- **`AMArtTone { Light, Dark }`**, named for the art (`Dark` art takes light text). The washes put the library's art in the mode's tone, so **the tone is the mode's** unless an art surface is given one. Nothing measures pictures.
- **Who provides it.** `AMBackdrop(art, modifier, tone = null, content)` and `AMArtRowsBackdrop(modifier, tone = null, content)` provide internal `LocalAMArtTone` (`compositionLocalOf`, so nodes reading it while drawing redraw on a change) to their content: `tone ?: modeArtTone()`. An app gives a surface a tone for art it washes less or draws itself (an unwashed photo: `tone = AMArtTone.Dark`). Off art it's null; an overlay window's fresh context resets it to null (`ProvideOverlayWindowLocals`). Public read: `AMTheme.artTone`.
- **The inks** (`artInk(tone, dark, scheme, secondary)`, and a composable `artInk(secondary, tone)` reading the locals): on art in the mode's tone, the scheme's `onSurface` and `onSurfaceVariant` (which hold over the washed art, above); on art of the other tone, `Style.ArtInk`'s fixed inks: white and `#D1D1D6` on dark art (light mode), `#1C1C1E` and `#48484A` on light art (dark mode). `artInkShadow(tone)`: `Style.Shadow.ArtText` (black) under ink on dark art, `ArtTextLight` (the same layers in white) under ink on light art.
- **Colored text on art** (`artInkHolding(color, tone)`): the color moved along its own line, keeping its hue, just far enough to hold 4.5:1 on the library's art of that tone: darker on light art, against `Style.ArtInk.LightArtGround` (L 0.4646, the light wash's darkest blend), lighter on dark art, against the dark wash's ceiling. Light mode's `error` `#B3261E` (3.56:1 on the floor, 3.20:1 on the worst blend) becomes `#8C1B15`, 4.5:1 against the blend and 5.01:1 on the floor; dark mode's `#F2B8B5` already holds (4.61:1) and stays.

### `Modifier.amArtText(tone = null, secondary = false)`

```kotlin
Text("Albums", Modifier.amArtText(), style = MaterialTheme.typography.titleLarge)
Text(album.artist, Modifier.amArtText(secondary = true))
Text("Paused", Modifier.amArtText(AMArtTone.Dark))   // white, on a scrim over a photo
```

Text that lies right on art, not on a card. Everything it covers, glyphs included, draws in the ink for the tone (keeping its alpha), with the tone's shadow drawn from the glyphs themselves. It's the foundations' glyph-shadow node (`Foundations.md`) given an `ArtGlyphs(tone, secondary)` request (internal `artGlyphs`), which resolves the tone (`tone`, else `LocalAMArtTone`, else the mode's), the ink and the shadow each time it draws, so a change of tone or mode redraws without recomposing. An explicit `tone` is for a ground the library can't know: a scrim over a photo (Card stack), an unwashed photo (Paused backdrop). Put it on a text or on a group of them.

## Catalog pages

`demo/.../catalog/pages/datadisplay/`, registered in `DataDisplayPages.entries` in this order (ids): `avatar`, `tag`, `badge`, `remoteImage`, `artThumbnail`, `artCard`, `artBackdrop`, `timeline`, followed by `CollectionsPages.entries` (`Collections.md`).

| Page | Preview | Loaded |
|---|---|---|
| Avatar (`AvatarPage.kt`) | `FullBleed`: three `Large` avatars on a surface strip: a photo, "Grace Hopper", "Alan Turing" | a card of five `AMListRow`s: "Apollo Guidance" with a `Large` rounded-square avatar (`shapes.medium`), Ada Lovelace's photo, Katherine Johnson with a count badge of 2 (`AMBadgedBox`), Margaret Heafield Hamilton, and "Invite pending" with the person glyph |
| Tag (`TagPage.kt`) | `FullBleed`: "Draft", "Active" (success) and "Blocked" (destructive, the filled `dangerous` octagon) in an `AMFlowLayout` on a surface strip | a release card: rows with trailing tags (Live, a monospace "v2.4.1" slot tag, Delayed, Failed, In review), then a row of six topic tags in an `AMFlowLayout` |
| Badge (`BadgePage.kt`) | `FullBleed`: three bells on a surface strip: a dot, 5, 128 ("99+") | a card of five shortcuts, each a mark over a `labelSmall` name, merged for TalkBack: Home, Alerts (dot), Inbox (7), Updates (128, info), Profile (a `Small` avatar with a success dot) |
| Remote image (`RemoteImagePage.kt`) | `FullBleed`: one 160 dp picture in `shapes.medium` on a surface strip | a card: the trip's cover (`CatalogArt.Peaks`, 160 dp tall, labeled), three 3:4 tiles (two photos, one empty with a `landscape` fallback glyph), then a "Coast" row with a 40 dp thumbnail and an "Untitled album" row with a `stacks` fallback |
| Art thumbnail (`ArtThumbnailPage.kt`) | `FullBleed`: one 40 dp thumbnail on a surface strip | a queue card of three `AMListRow`s with leading thumbnails: Harbor Lights / The Lanterns and Low Tide / Marlow Fields with covers, Paper Boats / Juniper Row with no cover, showing the `music_note` placeholder |
| Art card (`ArtCardPage.kt`) | a 160 dp art card floating on the page | an album's header: a region in `shapes.large` over the "orchard" cover's art (`amArtBackdrop`), the cover as a 200 dp art card over "Orchard" and its details in art text |
| Art backdrop (`ArtBackdropPage.kt`) | a 220 dp (at least) region in `shapes.large` over the "cobalt" cover's art, its title and details (secondary) in art text | a 340 dp (at least) region over the chosen album's art: the cover as a 120 dp art card, the title and details in art text, and an `AMSegmentedControl` of four albums in the mode's own look (Orchard and Cobalt photos, Snow all white and Ink all black, `SolidArt`, the extremes the wash is tuned for); switching fades the art across. Under it, "Unwashed": the black cover at `lightWash = 0f, darkWash = 0f` in an `AMBackdrop(tone = AMArtTone.Dark)`, its art text white in both modes |
| Timeline (`TimelinePage.kt`) | `FullBleed`: a draft's four steps (`AMTimeline` of `AMTimelineLabel`s) on a surface strip | a parcel's tracking in a card padded `spacing.lg`: placed, payment (info dot), delayed with a "Weather" warning tag, "Now · 9:41 AM" in success over "Out for delivery" (`amTimelineFirstLine` on the status line), and a clickable "Delivery window" |

Demo glyphs these pages add: `ic_notifications_unread`, `ic_queue_music`, `ic_photo_album`, `ic_wallpaper`, `ic_timeline` (entry icons), `ic_home`, `ic_landscape`, `ic_rainy`, `ic_dangerous_filled` (Tag's "Blocked"). Sample photos come from `catalogPhoto` and `CatalogArt`.
