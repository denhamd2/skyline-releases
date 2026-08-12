package com.denham.skyline.ui.components

import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.denham.skyline.player.CastSupport
import com.google.android.gms.cast.framework.CastButtonFactory

/**
 * The standard Chromecast icon that opens the system route picker. Renders
 * nothing on devices without Google Play Services (so no dead button appears).
 *
 * MediaRouteButton needs an AppCompat-derived theme, which our Material3 app
 * theme isn't — so it's wrapped in a Theme.AppCompat context. The host Activity
 * is a FragmentActivity so the chooser dialog can actually show.
 */
@Composable
fun CastButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (!CastSupport.isAvailable(context)) return

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val themed = ContextThemeWrapper(ctx, androidx.appcompat.R.style.Theme_AppCompat)
            MediaRouteButton(themed).apply {
                runCatching {
                    CastButtonFactory.setUpMediaRouteButton(ctx.applicationContext, this)
                }
                // Show the icon even before a Cast device is discovered.
                setAlwaysVisible(true)
            }
        },
    )
}
