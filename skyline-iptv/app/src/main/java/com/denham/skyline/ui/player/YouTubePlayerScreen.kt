package com.denham.skyline.ui.player

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Hands a YouTube video to the YouTube app rather than playing it inside
 * Skyline.
 *
 * ExoPlayer cannot be used here: "youtube.com/watch?v=..." serves an HTML
 * page, not a media stream, and YouTube does not publish direct stream URLs.
 *
 * Three attempts at embedded playback were tried and abandoned, each
 * confirmed broken on device rather than assumed: a [android.webkit.WebView]
 * loading a local HTML wrapper with the video as a nested iframe and an
 * intercepted request carrying a `Referer` header; the same `WebView`
 * loading the embed URL directly as a real navigation with that header
 * (the standard, documented way to attach one); and Chrome Custom Tabs. The
 * first two both produced YouTube's own "Error code: 152 - 4"; investigation
 * pointed at a WebView identifying itself to YouTube's embed backend via the
 * `; wv)` User-Agent marker regardless of headers sent alongside it. Custom
 * Tabs — a real browser instance, not a WebView component, which should not
 * carry that marker — also did not work on device; the exact failure mode
 * there is unconfirmed.
 *
 * This is the same conclusion Android TV already reached for the same
 * reason: see `TvMainActivity.playYoutube`, which hands off to the YouTube
 * app rather than trying to embed, and has been doing so without needing a
 * fix. This mirrors that pattern for phone.
 */
@Composable
fun YouTubePlayerScreen(
    videoId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(videoId) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
            )
        }
        onBack()
    }
}
