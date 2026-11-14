package com.ideamtrls.androidmtrls

/**
 * Settings for the pipeline that loads the library's pictures: [AMRemoteImage], [AMAvatar]'s
 * photos and the art backdrops' and art rows' pictures. One pipeline serves the whole process, so
 * these are set once, early, typically in `Application.onCreate`.
 *
 * ```kotlin
 * class HarborApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         AMImagePipeline.userAgent = "Harbor/1.4 (https://example.com/harbor; harbor@example.com)"
 *     }
 * }
 * ```
 */
public object AMImagePipeline {
    @Volatile
    private var configuredUserAgent: String? = null

    /**
     * The `User-Agent` header every picture request sends. Null, the default, sends one built from
     * the app: its package name and version, Android's release, then the library's name, such as
     * `com.example.harbor/1.4 (Android 16) AndroidMtrls`, so an image host can tell which app is
     * asking. Some hosts ask a client to name itself and a way to reach its makers; an app that
     * loads from them sets its own here.
     *
     * Read for each request, so a change applies from the next one. Pictures already cached aren't
     * fetched again.
     *
     * @throws IllegalArgumentException when set to text a header can't carry: blank, or with
     *   characters outside printable ASCII (a line break, an emoji).
     */
    public var userAgent: String?
        get() = configuredUserAgent
        set(value) {
            require(value == null || isUserAgentValue(value)) {
                "A User-Agent is printable ASCII and not blank: \"$value\""
            }
            configuredUserAgent = value
        }
}

/** Whether [value] can be sent as a `User-Agent` header: printable ASCII, not blank. */
internal fun isUserAgentValue(value: String): Boolean =
    value.isNotBlank() && value.all { it in ' '..'~' }

/**
 * The `User-Agent` the pipeline sends when the app sets none: the app's package and version, then
 * Android's release, then the library, as `com.example.harbor/1.4 (Android 16) AndroidMtrls`. A
 * version with characters a product token can't hold (a space, a slash) has them replaced by `-`,
 * and the release keeps only what a comment can; with no version, the package stands alone.
 */
internal fun defaultImageUserAgent(packageName: String, versionName: String?, androidRelease: String): String {
    val version = versionName?.takeIf { it.isNotBlank() }?.map { if (it.isTokenChar()) it else '-' }?.joinToString("")
    val product = if (version == null) packageName else "$packageName/$version"
    val release = androidRelease.filter { it in ' '..'~' && it != '(' && it != ')' && it != '\\' }.trim()
    return "$product (Android $release) AndroidMtrls"
}

/** Whether this character may sit in an HTTP token (a product name or version). */
private fun Char.isTokenChar(): Boolean =
    this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' || this in "!#$%&'*+-.^_`|~"
