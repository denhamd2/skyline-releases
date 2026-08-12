package com.denham.skyline.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.denham.skyline.SkylineApp

/**
 * Wraps the shared player in a MediaSession so playback keeps running with a
 * notification (lock screen / headset controls) when the app is backgrounded.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val container = (application as SkylineApp).container
        mediaSession = MediaSession.Builder(this, container.playerManager.player).build()
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
        // Release the session but NOT the shared player — it belongs to the app.
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
