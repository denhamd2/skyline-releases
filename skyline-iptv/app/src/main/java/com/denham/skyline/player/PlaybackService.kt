package com.denham.skyline.player

import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.denham.skyline.SkylineApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Wraps the shared player in a MediaSession so playback keeps running with a
 * notification (lock screen / headset controls) when the app is backgrounded.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    // Main.immediate: MediaSession.setPlayer() asserts it is called on the
    // players' own application looper, which PlayerManager pins to main.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val playerManager = (application as SkylineApp).container.playerManager
        val session = MediaSession.Builder(this, playerManager.currentPlayer.value).build()
        mediaSession = session
        // MediaSession takes a fixed Player at build time, so bound to the local
        // player alone the notification, lock screen and headset controls drove a
        // stopped player for the whole duration of a Chromecast session. Follow
        // the hand-off instead.
        scope.launch {
            playerManager.currentPlayer.collect { active ->
                if (session.player !== active) {
                    // setPlayer asserts same-looper; never crash the service over
                    // it, but never swallow it silently either.
                    runCatching { session.player = active }
                        .onFailure { Log.w("PlaybackService", "setPlayer failed", it) }
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        // Release the session but NOT the shared player — it belongs to the app.
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
