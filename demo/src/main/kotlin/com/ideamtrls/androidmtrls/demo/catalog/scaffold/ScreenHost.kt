package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMBannerHost
import com.ideamtrls.androidmtrls.AMToastHost

/**
 * The catalog's screen root: hosts the banners and toasts a page's demos post, over the whole
 * screen, top app bar included, the way an app's screen root hosts its own. [ComponentPage] wraps
 * every page in it, so a demo that sits inline on its page calls `AMBanner` or `AMToast` from its
 * own button and the message shows at the top or the bottom of the screen.
 *
 * Overlay windows (a dialog, a sheet) need no host here: they open over the screen by themselves.
 */
@Composable
internal fun CatalogScreenHost(content: @Composable () -> Unit) {
    AMBannerHost(Modifier.fillMaxSize()) {
        AMToastHost(Modifier.fillMaxSize(), content = content)
    }
}
