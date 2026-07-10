package io.github.jmsdnns.androidmtrls.content

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.amSquircle

// ---------------------------------------------------------------------------
// Pure logic — JVM-testable, no Compose dependencies
// ---------------------------------------------------------------------------

/**
 * Saturation used for avatar color generation. HSV saturation = 0.55.
 *
 * Matches iOS `IMAvatar`'s `sat = 0.55`.
 */
const val AM_AVATAR_SATURATION: Float = 0.55f

/**
 * Brightness (value) used for avatar color generation. HSV value = 0.85.
 *
 * Matches iOS `IMAvatar`'s `bri = 0.85`.
 */
const val AM_AVATAR_BRIGHTNESS: Float = 0.85f

/**
 * djb2 hash of [name] with seed 5381, UInt64 (ULong) wrap semantics.
 *
 * Iterates the string as Unicode code points via [String.codePoints] so that
 * emoji (e.g. U+1F600 "😀") and CJK characters (e.g. U+674E "李") are each
 * treated as a single value — matching Swift's `name.unicodeScalars` iteration.
 *
 * **Do not** iterate `.toCharArray()` — emoji are 2 UTF-16 chars but only 1
 * Unicode scalar/code point, so char-based iteration produces a different hash
 * to the Swift reference.
 *
 * Algorithm: `hash = ((hash << 5) + hash + codePoint)` — equivalent to
 * `hash * 33 + codePoint`. ULong arithmetic wraps silently on overflow,
 * faithfully replicating Swift `UInt64` truncating multiply.
 *
 * @param name The raw string to hash (typically the avatar's `initials` param,
 *             passed before initials extraction so that hue is stable across
 *             name variants).
 * @return     ULong hash.
 */
internal fun avatarDjb2Hash(name: String): ULong {
    var hash: ULong = 5381uL
    val iter = name.codePoints().iterator()
    while (iter.hasNext()) {
        val cp = iter.nextInt().toULong()
        hash = (hash shl 5) + hash + cp
        // ULong arithmetic in Kotlin wraps at 2^64 automatically (no mask needed)
    }
    return hash
}

/**
 * Maps [name] to a hue bucket in `[0, 360)`.
 *
 * Returns `avatarDjb2Hash(name) % 360u` as a [UInt].
 */
internal fun avatarHueIndex(name: String): UInt =
    (avatarDjb2Hash(name) % 360uL).toUInt()

/**
 * Maps [name] to a hue value in `[0f, 1f)` for use with [Color.hsv].
 *
 * The float is `hueIndex / 360f`. Cached in `remember(name)` at the
 * composable call site; this function itself is pure and cheap.
 */
internal fun avatarHue(name: String): Float =
    avatarHueIndex(name).toFloat() / 360f

/**
 * Extracts up to two uppercase initials from [raw].
 *
 * Split by whitespace; take the first character of the first two parts.
 * Returns `""` if [raw] is null or blank.
 *
 * Examples:
 * - `"Christopher Knight"` → `"CK"`
 * - `"Jane"` → `"J"`
 * - `""` → `""`
 */
internal fun extractInitials(raw: String?): String {
    if (raw.isNullOrEmpty()) return ""
    val parts = raw.trim().split(Regex("\\s+"))
    return parts.take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
}

// ---------------------------------------------------------------------------
// AMAvatarSize — SM / MD / LG
// ---------------------------------------------------------------------------

/**
 * Three canonical avatar sizes.
 *
 * [diameterDp] is the circle/square diameter. [fontSizeSp] is the initials
 * text size rendered inside the avatar. Both mirror `IMAvatar` values exactly.
 */
@Immutable
enum class AMAvatarSize {
    /** 32dp diameter, 13sp initials. */
    SM,

    /** 44dp diameter (default), 17sp initials. */
    MD,

    /** 64dp diameter, 24sp initials. */
    LG;

    /** Diameter of the avatar frame in dp. */
    val diameterDp: Dp
        get() = when (this) {
            SM -> 32.dp
            MD -> 44.dp
            LG -> 64.dp
        }

    /** Initials text size in sp. */
    val fontSizeSp: Float
        get() = when (this) {
            SM -> 13f
            MD -> 17f
            LG -> 24f
        }
}

// ---------------------------------------------------------------------------
// AMAvatarShape — Circle or rounded-square
// ---------------------------------------------------------------------------

/**
 * Clip shape for the avatar frame.
 *
 * - [Circle]: full circle (default).
 * - [Square]: rectangle with an explicit corner radius.
 */
@Immutable
sealed class AMAvatarShape {
    /** Full circular clip (default shape). */
    @Immutable
    object Circle : AMAvatarShape()

    /**
     * Rounded-rectangle clip.
     *
     * @param cornerRadius Corner radius for all four corners.
     */
    @Immutable
    data class Square(val cornerRadius: Dp) : AMAvatarShape()
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

/**
 * Resolves an [AMAvatarShape] to a Compose [Shape].
 */
private fun AMAvatarShape.toShape(): Shape = when (this) {
    is AMAvatarShape.Circle -> CircleShape
    is AMAvatarShape.Square -> amSquircle(cornerRadius)
}

// ---------------------------------------------------------------------------
// AMAvatar — public composable
// ---------------------------------------------------------------------------

/**
 * Circular or rounded-square avatar that displays a remote image, color-coded
 * initials, or a fallback icon.
 *
 * ## Rendering priority
 * 1. If [url] is non-null and loads successfully → remote image, cropped to fill.
 * 2. If [initials] is non-null and non-blank after extraction → colored background
 *    with up to two uppercase initials.
 * 3. Otherwise → colored background with [fallbackIcon].
 *
 * ## Color generation
 * The fallback background color is generated by hashing [initials] with djb2
 * (seed 5381, UInt64 semantics, code-point iteration) to a hue index in
 * `[0, 360)`, then creating `Color.hsv(hue, sat=0.55f, value=0.85f)`.
 *
 * ## Hairline border
 * A 0.5dp `AMTheme.separator`-colored border is applied over the clip shape,
 * matching iOS `.imHairline(Circle(), color: IMTheme.separator)`.
 *
 * ## iOS mapping
 * iOS `IMAvatar` → `AMAvatar`. Same parameter semantics; `AsyncImage`
 * (Coil 3) replaces SwiftUI `AsyncImage`; `Icons.Default.Person` replaces the
 * `person.fill` SF Symbol.
 *
 * @param url          Remote image URL. Null → fallback immediately.
 * @param initials     Raw name string for initials extraction and hue hash.
 *                     Null → icon fallback.
 * @param size         [AMAvatarSize] controlling diameter and font size.
 *                     Default: [AMAvatarSize.MD] (44dp).
 * @param shape        [AMAvatarShape] controlling clip geometry.
 *                     Default: [AMAvatarShape.Circle].
 * @param fallbackIcon [ImageVector] shown when initials is null or blank.
 *                     Default: [Icons.Default.Person].
 * @param contentDescription Accessibility label for the avatar image.
 *                     Pass null when the avatar is decorative.
 */
@Composable
fun AMAvatar(
    url: String?,
    initials: String? = null,
    size: AMAvatarSize = AMAvatarSize.MD,
    shape: AMAvatarShape = AMAvatarShape.Circle,
    fallbackIcon: ImageVector = Icons.Default.Person,
    contentDescription: String? = null,
) {
    val isDark = LocalAMIsDarkTheme.current
    val clipShape = shape.toShape()

    // Hue is computed from the raw `initials` string (not extracted initials)
    // and memoized so recompositions with the same name skip the hash.
    val hue = remember(initials) { avatarHue(initials ?: "") }
    val extractedInitials = remember(initials) { extractInitials(initials) }
    val fallbackBg = remember(hue) { Color.hsv(hue * 360f, AM_AVATAR_SATURATION, AM_AVATAR_BRIGHTNESS) }

    val separatorColor = AMTheme.separator.resolve(isDark)

    Box(
        modifier = Modifier
            .size(size.diameterDp)
            .clip(clipShape)
            .border(width = AMTheme.hairlineWidth.dp, color = separatorColor, shape = clipShape),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    AMAvatarFallback(
                        extractedInitials = extractedInitials,
                        fallbackBg = fallbackBg,
                        fallbackIcon = fallbackIcon,
                        size = size,
                    )
                },
                error = {
                    AMAvatarFallback(
                        extractedInitials = extractedInitials,
                        fallbackBg = fallbackBg,
                        fallbackIcon = fallbackIcon,
                        size = size,
                    )
                },
                success = {
                    SubcomposeAsyncImageContent(
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                },
            )
        } else {
            AMAvatarFallback(
                extractedInitials = extractedInitials,
                fallbackBg = fallbackBg,
                fallbackIcon = fallbackIcon,
                size = size,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Internal — fallback content (initials text or icon on colored background)
// ---------------------------------------------------------------------------

/**
 * Internal fallback rendered when the remote image is unavailable or absent.
 *
 * Fills the parent box with the djb2-derived HSV background color, then
 * overlays either [extractedInitials] text (if non-empty) or [fallbackIcon].
 */
@Composable
private fun AMAvatarFallback(
    extractedInitials: String,
    fallbackBg: Color,
    fallbackIcon: ImageVector,
    size: AMAvatarSize,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fallbackBg),
        contentAlignment = Alignment.Center,
    ) {
        if (extractedInitials.isNotEmpty()) {
            BasicText(
                text = extractedInitials,
                style = TextStyle(
                    color = Color.White,
                    fontSize = size.fontSizeSp.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        } else {
            Image(
                imageVector = fallbackIcon,
                contentDescription = null,
                modifier = Modifier.size(size.fontSizeSp.dp),
                colorFilter = ColorFilter.tint(Color.White),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark as required by conventions
// ---------------------------------------------------------------------------

@Composable
private fun AMAvatarPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Box(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // LG — initials
            AMAvatar(url = null, initials = "Alice Johnson", size = AMAvatarSize.LG)
            // MD — initials (default size)
            AMAvatar(url = null, initials = "Bob Smith")
            // SM — initials
            AMAvatar(url = null, initials = "Jane", size = AMAvatarSize.SM)
            // MD — icon fallback (no initials)
            AMAvatar(url = null, initials = null)
            // MD — square shape
            AMAvatar(
                url = null,
                initials = "CK",
                shape = AMAvatarShape.Square(AMTheme.Radius.md.dp),
            )
            // CJK
            AMAvatar(url = null, initials = "李雷", size = AMAvatarSize.MD)
        }
    }
}

@Preview(
    name = "AMAvatar · Light",
    group = "AMAvatar",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    backgroundColor = 0xFFF2F2F7,
)
@Composable
private fun AMAvatarPreviewLight() {
    AMThemeRoot {
        AMAvatarPreviewContent()
    }
}

@Preview(
    name = "AMAvatar · Dark",
    group = "AMAvatar",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMAvatarPreviewDark() {
    AMThemeRoot {
        AMAvatarPreviewContent()
    }
}
