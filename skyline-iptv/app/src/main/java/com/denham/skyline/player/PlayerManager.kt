package com.denham.skyline.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What Skyline is currently playing (drives the player screen + retries). */
data class NowPlaying(
    val url: String,
    val title: String,
    val subtitle: String? = null,
    val isLive: Boolean,
    /** For live items: ids needed to retry with the other container. */
    val liveStreamId: Int? = null,
)

sealed interface PlayerUiError {
    data class Message(val text: String) : PlayerUiError
}

/**
 * One ExoPlayer for the whole app. Reusing the instance (setMediaItem +
 * prepare on channel change, never rebuild) is what makes zapping fast, and
 * releasing/clearing promptly is what frees the provider connection slot —
 * exceeding `max_connections` is the #1 cause of mystery playback failures.
 */
@UnstableApi
class PlayerManager(
    private val context: Context,
    private val dataSourceFactory: DataSource.Factory,
) {

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying

    private val _error = MutableStateFlow<PlayerUiError?>(null)
    val error: StateFlow<PlayerUiError?> = _error

    /** Last observed HTTP error code from the data source, if any. */
    @Volatile
    var lastHttpErrorCode: Int? = null
        private set

    val player: ExoPlayer by lazy { buildPlayer() }

    /** The Cast SDK context, present only on devices with Google Play Services. */
    private val castContext: CastContext? by lazy {
        val available = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        if (!available) null
        else runCatching { CastContext.getSharedInstance(context) }.getOrNull()
    }

    /** True when casting is even possible (Play Services present). */
    val castAvailable: Boolean get() = castContext != null

    /** Player that mirrors playback onto a connected Chromecast, when one exists. */
    private val castPlayer: CastPlayer? by lazy {
        castContext?.let { ctx ->
            CastPlayer(ctx).apply {
                setSessionAvailabilityListener(object : SessionAvailabilityListener {
                    override fun onCastSessionAvailable() = transferTo(this@apply)
                    override fun onCastSessionUnavailable() = transferTo(player)
                })
            }
        }
    }

    /**
     * Playback target the UI binds to: the local ExoPlayer normally, the
     * CastPlayer while a Chromecast session is connected. Exposed as a flow so
     * the PlayerView can rebind the moment it switches.
     */
    private val _currentPlayer by lazy { MutableStateFlow<Player>(player) }
    val currentPlayer: StateFlow<Player> by lazy { _currentPlayer }

    /** The last item we prepared, replayed onto whichever player takes over. */
    private var lastItem: MediaItem? = null

    /** Hand the current stream + position from the old player to [target]. */
    private fun transferTo(target: Player) {
        val from = _currentPlayer.value
        if (from === target) return
        val item = lastItem
        val position = from.currentPosition
        val wasPlaying = from.playWhenReady
        from.pause()
        if (item != null) {
            target.setMediaItem(item, position)
            target.prepare()
            target.playWhenReady = wasPlaying
        }
        _currentPlayer.value = target
    }

    private fun buildPlayer(): ExoPlayer {
        // NextLib adds FFmpeg software decoders (AC3/EAC3/DTS/MP2/TrueHD…);
        // EXTENSION_RENDERER_MODE_ON prefers hardware, falls back to FFmpeg.
        val renderersFactory = NextRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        // Start playback after ~1s of buffer instead of the default ~2.5s —
        // that's most of the perceived zap latency. Modest max buffer keeps
        // memory flat; near-zero buffers would cause constant rebuffering.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        lastHttpErrorCode = error.httpErrorCode()
                    }
                })
            }
    }

    fun playLive(streamId: Int, url: String, title: String, subtitle: String?) {
        _nowPlaying.value = NowPlaying(
            url = url, title = title, subtitle = subtitle,
            isLive = true, liveStreamId = streamId,
        )
        start(url, title, subtitle, live = true)
    }

    fun playVod(url: String, title: String, subtitle: String? = null) {
        _nowPlaying.value = NowPlaying(url = url, title = title, subtitle = subtitle, isLive = false)
        start(url, title, subtitle, live = false)
    }

    /** Re-prepare the current item (used by error retry, incl. container switch). */
    fun retry(withUrl: String? = null) {
        val current = _nowPlaying.value ?: return
        val url = withUrl ?: current.url
        if (withUrl != null) _nowPlaying.value = current.copy(url = withUrl)
        start(url, current.title, current.subtitle, current.isLive)
    }

    private fun start(url: String, title: String, subtitle: String?, live: Boolean) {
        _error.value = null
        lastHttpErrorCode = null
        // Touch the CastPlayer so its session listener is registered — without
        // this a Chromecast connection would never take over playback.
        castPlayer?.let { }
        val item = MediaItem.Builder()
            .setUri(url)
            .setMimeType(
                when {
                    url.endsWith(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
                    else -> null // DefaultMediaSourceFactory infers (TS/MP4/MKV…)
                }
            )
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(title).setArtist(subtitle).build()
            )
            .setLiveConfiguration(
                if (live) MediaItem.LiveConfiguration.Builder().build()
                else MediaItem.LiveConfiguration.UNSET
            )
            .build()
        lastItem = item
        with(_currentPlayer.value) {
            setMediaItem(item)
            prepare()
            playWhenReady = true
        }
    }

    fun reportError(message: String) {
        _error.value = PlayerUiError.Message(message)
    }

    /** Attach [l] to both the local and cast players so UI state stays correct
     *  across a Chromecast hand-off. */
    fun addListener(l: Player.Listener) {
        player.addListener(l)
        castPlayer?.addListener(l)
    }

    fun removeListener(l: Player.Listener) {
        player.removeListener(l)
        castPlayer?.removeListener(l)
    }

    /**
     * Stop and clear the current stream. Always called when leaving playback —
     * a lingering prepared stream holds one of the account's connections.
     */
    fun stopAndClear() {
        _currentPlayer.value.stop()
        _currentPlayer.value.clearMediaItems()
        lastItem = null
        _nowPlaying.value = null
        _error.value = null
    }

    fun release() {
        castPlayer?.setSessionAvailabilityListener(null)
        castPlayer?.release()
        player.release()
    }
}

/** Digs the HTTP status code out of a PlaybackException, when there is one. */
fun PlaybackException.httpErrorCode(): Int? {
    var cause: Throwable? = cause
    while (cause != null) {
        if (cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
            return cause.responseCode
        }
        cause = cause.cause
    }
    return null
}
