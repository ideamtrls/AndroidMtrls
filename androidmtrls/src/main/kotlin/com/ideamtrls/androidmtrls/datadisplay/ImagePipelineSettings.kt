package com.ideamtrls.androidmtrls

/**
 * Settings for the pipeline that loads the library's pictures: [AMRemoteImage], [AMAvatar]'s
 * photos and the art backdrops' and art rows' pictures. One pipeline serves the whole process, so
 * these are set once, early, typically in `Application.onCreate`.
 *
 * The pipeline reads `http` and `https` URLs from the network, and `file` and `content` URIs on the
 * device only once [allowsLocalSources] is on; every picture is decoded at the size its view needs
 * and kept in memory. Network pictures also go to a bounded disk cache in the app's cache directory,
 * which is trimmed as it fills and which the system may clear: it isn't an offline store. Keeping
 * pictures for offline use is the app's job, in its own store, which it hands the pipeline through
 * [loader].
 *
 * ```kotlin
 * class HarborApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         AMImagePipeline.userAgent = "Harbor/1.4 (https://example.com/harbor; harbor@example.com)"
 *         AMImagePipeline.loader = { url -> coverStore.bytesFor(url) }
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

    /**
     * The app's own picture store, asked first for every picture the pipeline doesn't hold in
     * memory: given a picture's URL, it returns the picture's encoded bytes (any format Android
     * decodes, as a file holds them), or null to let the pipeline load the URL itself. Null, the
     * default, asks nothing. It's asked for any URL, so it can serve a scheme of the app's own.
     *
     * Bytes it returns are decoded at the size the view needs and kept in memory like any other
     * picture, keyed by the URL, but never copied to the pipeline's disk cache, since the app's
     * store already keeps them. The library holds no lasting offline store of its own; keeping
     * pictures for offline use is the app's job, and this is how the app hands them over.
     *
     * Runs on one of the pipeline's background threads (the IO dispatcher), never the main thread,
     * so it may block on a file or a database. It may run on several threads at once, for different
     * pictures or for one picture at different sizes, so it must be thread-safe. When no view wants
     * the picture any more, its thread is interrupted. An exception it throws fails that load: the
     * view shows its fallback and tries again the next time it appears.
     *
     * Read for each load, so a change applies from the next one. Pictures already in memory aren't
     * asked for again.
     */
    @Volatile
    public var loader: ((url: String) -> ByteArray?)? = null

    /**
     * Whether the pipeline reads `file` and `content` URLs on the device. False, the default, fails
     * them like any scheme the pipeline doesn't read: the view shows its fallback.
     *
     * It's off because picture URLs often come from outside the app, such as a podcast feed's
     * artwork, and a `file` or `content` URL from there could make the app decode and show its own
     * private files, or content it has permission to read. To show the app's own pictures, use
     * [loader], which is asked first for every URL whatever this says: the app serves its store
     * under a scheme of its own and needs no local URLs at all. Turn this on only when every picture
     * URL the app shows is one the app controls.
     *
     * When on, a `file` URL must name a regular file, by its canonical path (its `..` and symbolic
     * links resolved): a directory, a pipe or a device fails. Both schemes are read under a network
     * fetch's limits, at most 32 MB in 60 s, and neither is copied to the disk cache.
     *
     * Read for each load, so a change applies from the next one. Pictures already in memory still
     * show.
     */
    @Volatile
    public var allowsLocalSources: Boolean = false
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
