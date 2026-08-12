package com.denham.skyline

import android.app.Application
import android.content.Context
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.denham.skyline.di.AppContainer

@UnstableApi
class SkylineApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    /**
     * Set when [AppContainer] construction fails. A throw here would happen
     * before any Activity starts, so the crash screen in MainActivity would
     * never get a chance to render and the app would just vanish on launch
     * every time. Catching it lets MainActivity start and show the reason.
     */
    var startupError: Throwable? = null
        private set

    /**
     * Android initialises ContentProviders (FileProvider, androidx.startup,
     * etc.) *between* attachBaseContext and onCreate. Installing the crash
     * handler here rather than in onCreate is what makes a provider-init
     * crash visible -- otherwise the process dies before any handler exists
     * and the app just vanishes on launch with nothing recorded.
     */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        runCatching { CrashLogger.install(base) }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            container = AppContainer(this)
        } catch (t: Throwable) {
            startupError = t
            CrashLogger.record(this, t)
        }
    }

    /**
     * Coil shares the app's OkHttp stack so channel logos/posters are fetched
     * with the same (configurable) User-Agent — providers that filter players
     * by UA often filter image requests too.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .apply { if (::container.isInitialized) okHttpClient(container.okHttpClient) }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
}
