package com.ideamtrls.androidmtrls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.BreakIterator

/**
 * An [AMAvatar]'s size: its diameter, with its initials sized to match.
 *
 * @property diameter the avatar's width and height. Public so a placeholder (a skeleton, a
 *   badge's host) can match it instead of repeating the number.
 */
public enum class AMAvatarSize(public val diameter: Dp) {
    /** 32 dp: a compact list, a stack of faces, a timeline's marker. */
    Small(32.dp),

    /** 40 dp: a list row's leading picture, Material's list avatar. The default. */
    Medium(40.dp),

    /** 56 dp: a profile header, a workspace's own picture. */
    Large(56.dp),
}

/** The initials' size: fixed with the circle, like the rest of the picture. */
private val AMAvatarSize.initialsSize: Dp
    get() = when (this) {
        AMAvatarSize.Small -> 13.dp
        AMAvatarSize.Medium -> 16.dp
        AMAvatarSize.Large -> 22.dp
    }

/**
 * A person's picture, or their initials when there's no picture: the photo at [url], cropped to
 * [shape]; while it loads, when it fails and when [url] is null, the initials of [name] in white
 * on a color picked from the name, so the same person always gets the same color.
 *
 * ```kotlin
 * AMAvatar(person.photoUrl, name = person.name)
 * AMAvatar(null, name = "Apollo Guidance", size = AMAvatarSize.Large, shape = MaterialTheme.shapes.medium)
 * ```
 *
 * - **Initials**: the first letter of up to two words of [name], upper-cased ("Christopher
 *   Knight" shows "CK", "Jane" shows "J"). A null or blank name shows a person glyph instead. Pass
 *   the full name, not pre-cut initials.
 * - **Color**: a hue hashed from the whole name, at a fixed mid-pastel saturation and brightness
 *   that keeps white initials legible, the same in light and dark mode.
 * - **Outline**: a hairline in [shape] ([Modifier.amHairline][amHairline]) in both modes, so a
 *   light photo still has an edge on a white card.
 * - **Size**: fixed by [size]. The initials scale with the circle, not with the font scale: an
 *   avatar is a picture.
 * - **Accessibility**: one image, labeled with [name] whether the photo or the initials show. A
 *   null or blank name leaves it decorative, and a screen reader skips it.
 *
 * @param url the photo; null shows the initials.
 * @param name the person's or the group's full name: the initials, the color and what a screen
 *   reader says. Null shows a person glyph and leaves the avatar decorative.
 * @param modifier the avatar's placement.
 * @param size the avatar's diameter.
 * @param shape the avatar's outline: a circle by default; a rounded square
 *   (`MaterialTheme.shapes.medium`) suits a workspace or a group.
 */
@Composable
public fun AMAvatar(
    url: String?,
    name: String?,
    modifier: Modifier = Modifier,
    size: AMAvatarSize = AMAvatarSize.Medium,
    shape: Shape = CircleShape,
) {
    val initials = remember(name) { avatarInitials(name) }
    val fill = remember(name) { Color.hsv(avatarHue(name.orEmpty()) * 360f, AvatarSaturation, AvatarBrightness) }
    val spokenName = name?.takeIf { it.isNotBlank() }
    Box(
        modifier
            .size(size.diameter)
            .clip(shape)
            .amHairline(shape)
            .clearAndSetSemantics {
                if (spokenName != null) {
                    contentDescription = spokenName
                    role = Role.Image
                }
            },
    ) {
        AMRemoteImage(url, contentDescription = null, fallback = { AvatarInitials(initials, fill, size) })
    }
}

/** The fallback: the initials, or a person glyph without them, in white on the name's color. */
@Composable
private fun AvatarInitials(initials: String, fill: Color, size: AMAvatarSize) {
    Box(Modifier.fillMaxSize().background(fill), contentAlignment = Alignment.Center) {
        if (initials.isEmpty()) {
            Icon(
                painterResource(R.drawable.am_ic_person),
                contentDescription = null,
                modifier = Modifier.size(size.diameter / 2),
                tint = Color.White,
            )
        } else {
            val fontSize = with(LocalDensity.current) { size.initialsSize.toSp() }
            Text(
                initials,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = fontSize, lineHeight = fontSize * 1.25f),
            )
        }
    }
}

/** The fallback fill's saturation and brightness: a mid-pastel under white initials. */
private const val AvatarSaturation = 0.55f
private const val AvatarBrightness = 0.85f

/**
 * The initials of [name]: the first character of each of its first two words, upper-cased.
 * "Christopher Knight" gives "CK", "Jane" gives "J", and a null, empty or blank name gives "".
 * A character is what a reader sees as one, so an accented letter or an emoji stays whole.
 */
internal fun avatarInitials(name: String?): String {
    if (name.isNullOrBlank()) return ""
    val words = name.trim().split(Whitespace).filter { it.isNotEmpty() }.take(2)
    return words.joinToString("") { firstCharacter(it) }.uppercase()
}

/** Any run of spaces, Unicode ones (a no-break space) included. */
private val Whitespace = Regex("[\\s\\p{Z}]+")

/** The first user-perceived character of [word]. */
private fun firstCharacter(word: String): String {
    val characters = BreakIterator.getCharacterInstance()
    characters.setText(word)
    val end = characters.next()
    return if (end == BreakIterator.DONE) word else word.substring(0, end)
}

/**
 * The hue for [name], in `[0, 1)`: a djb2 hash of the name's code points (start at 5381, then
 * `hash × 33 + code point` for each, wrapping at 64 bits), taken modulo 360 and scaled to a turn.
 * The whole name is hashed, not its initials, so two people who share initials usually differ.
 */
internal fun avatarHue(name: String): Float {
    var hash: ULong = 5381u
    var index = 0
    while (index < name.length) {
        val codePoint = name.codePointAt(index)
        hash = (hash shl 5) + hash + codePoint.toULong()
        index += Character.charCount(codePoint)
    }
    return (hash % 360u).toFloat() / 360f
}
