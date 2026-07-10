package io.github.jmsdnns.androidmtrls.demo

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient

/**
 * Demo [Application] that installs the process-wide Coil [ImageLoader] used by
 * every `AMAvatar` / `AMRemoteImage` in the catalog.
 *
 * ## Why this exists
 * The Social scene's avatars are Wikimedia Commons portraits fetched through
 * `Special:FilePath`. Wikimedia's User-Agent policy rejects requests bearing a
 * generic User-Agent — including OkHttp's default `okhttp/x.y.z` — with HTTP
 * 403. Coil's default network stack sends exactly that UA, so those portraits
 * failed and fell back to djb2 initials, while picsum-hosted post images (no
 * such policy) loaded normally. Sending a descriptive User-Agent satisfies the
 * policy and the portraits load.
 *
 * This is app-level network configuration, so it lives in the demo rather than
 * in the library's `AMAvatar` / `AMRemoteImage`, which deliberately delegate the
 * network client to the host app's singleton loader.
 */
class DemoApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "AndroidMtrls-Demo/1.0 (https://github.com/jmsdnns/AndroidMtrls; jdennis@gmail.com)",
                    )
                    .build()
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { client }))
            }
            .build()
    }
}
