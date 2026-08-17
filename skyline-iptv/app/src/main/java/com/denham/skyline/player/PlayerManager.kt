package com.denham.skyline.player

import android.content.Context
import android.os.Looper
import androidx.media3.common.C
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
    /**
     * HLS variant of [url], for casting. Local live playback may be TS (and may
     * fall back to TS on error); the receiver can only be given HLS, so the cast
     * URL is held separately and the container fallback never touches it.
     */
    val castUrl: String? = null,
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

    /** Player that mirrors playback onto a connected Chromecast, when one exists. */
    private val castPlayer: CastPlayer? by lazy {
        castContext?.let { ctx ->
            CastPlayer(ctx).also { cast ->
                cast.setSessionAvailabilityListener(object : SessionAvailabilityListener {
                    override fun onCastSessionAvailable() = transferTo(cast)
                    override fun onCastSessionUnavailable() = transferTo(player)
                })
                // CastPlayer's constructor adopts an already-connected session
                // before this listener is attached, and attaching it does not
                // replay that -- so onCastSessionAvailable() never fires for a
                // session that existed before now. Catch it up by hand.
                // runCatching because a throw escaping a `by lazy` initialiser
                // leaves the lazy uninitialised, and the next access would then
                // build a second CastPlayer.
                runCatching { if (cast.isCastSessionAvailable) transferTo(cast) }
            }
        }
    }

    /**
     * Build the CastPlayer, registering its session listener, before playback
     * starts. Without this, connecting from the Live or Guide screen (where the
     * cast button also lives, but no playback has begun) leaves no CastPlayer in
     * existence to receive the session at all. Main thread only —
     * CastContext.getSharedInstance() requires it.
     */
    fun initCasting() {
        castPlayer
    }

    /** True while the Chromecast, not the phone, is the active playback target. */
    val isCasting: Boolean get() = _currentPlayer.value !== player

    /**
     * Playback target the UI binds to: the local ExoPlayer normally, the
     * CastPlayer while a Chromecast session is connected. Exposed as a flow so
     * the PlayerView can rebind the moment it switches.
     */
    private val _currentPlayer by lazy { MutableStateFlow<Player>(player) }
    val currentPlayer: StateFlow<Player> by lazy { _currentPlayer }

    private fun baseItem(np: NowPlaying) = MediaItem.Builder()
        .setUri(np.url)
        .setMediaMetadata(
            MediaMetadata.Builder().setTitle(np.title).setArtist(np.subtitle).build()
        )

    /**
     * Item for LOCAL playback. mimeType stays null except for HLS so that
     * DefaultMediaSourceFactory keeps inferring the container (TS/MP4/MKV…) —
     * some providers serve a redirect or an HLS payload from a `.ts` path, and
     * pinning the type here would silently break those channels.
     */
    private fun localItem(np: NowPlaying): MediaItem =
        baseItem(np)
            .setMimeType(
                if (np.url.endsWith(".m3u8", ignoreCase = true)) MimeTypes.APPLICATION_M3U8
                else null
            )
            .setLiveConfiguration(
                if (np.isLive) MediaItem.LiveConfiguration.Builder().build()
                else MediaItem.LiveConfiguration.UNSET
            )
            .build()

    /**
     * Item + start position for the Chromecast, or null when the receiver cannot
     * play this container at all. Deliberately a separate item from
     * [localItem]: the receiver needs an explicit content type and a different
     * URL for live, and local playback must keep its inference behaviour.
     */
    private fun castItem(np: NowPlaying, fromPositionMs: Long): Pair<MediaItem, Long>? {
        val plan = CastMedia.plan(np.url, np.castUrl, np.isLive, fromPositionMs) ?: return null
        return baseItem(np)
            .setUri(plan.url)
            .setMimeType(plan.mimeType)
            .build() to plan.startPositionMs
    }

    /** Hand the current stream + position from the old player to [target]. */
    private fun transferTo(target: Player) {
        val from = _currentPlayer.value
        if (from === target) return
        // Nothing playing yet (e.g. connected from the Live or Guide screen):
        // just retarget, with no item and no spurious "can't cast" message.
        val np = _nowPlaying.value
        if (np == null) {
            _currentPlayer.value = target
            return
        }

        val wasPlaying = from.playWhenReady
        val item: MediaItem
        val startPositionMs: Long
        if (target !== player) {
            // Decided before touching local playback, so an uncastable stream
            // leaves the phone playing rather than pausing it into a dead end.
            val cast = castItem(np, from.currentPosition) ?: run {
                reportError(CastMedia.UNSUPPORTED_MESSAGE)
                return
            }
            item = cast.first
            startPositionMs = cast.second
        } else {
            item = localItem(np)
            startPositionMs = if (np.isLive) C.TIME_UNSET else from.currentPosition
        }

        val started = runCatching {
            target.setMediaItem(item, startPositionMs)
            target.prepare()
            target.playWhenReady = wasPlaying
        }.onFailure { e ->
            reportError("Couldn't start casting (${e.javaClass.simpleName}). Still playing here.")
        }
        if (started.isFailure) {
            // _currentPlayer never moved, so the UI stays bound to a valid player.
            runCatching { target.stop() }
            return
        }

        _currentPlayer.value = target
        // Retarget the UI and MediaSession first, then release the outgoing
        // player's source. stop() rather than pause(): a merely paused ExoPlayer
        // still holds one of the account's max_connections open, and while
        // casting that slot is exactly the one the receiver needs.
        runCatching { from.stop() }
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
            // Pin the looper instead of taking whichever thread first touches
            // this lazy. MediaSession.setPlayer() asserts that the old and new
            // players share an application looper, so the cast hand-off in
            // PlaybackService depends on this being main, deterministically.
            .setLooper(Looper.getMainLooper())
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        lastHttpErrorCode = error.httpErrorCode()
                    }
                })
            }
    }

    /** [castUrl] is the HLS variant of [url] — see [NowPlaying.castUrl]. */
    fun playLive(streamId: Int, url: String, title: String, subtitle: String?, castUrl: String?) {
        _nowPlaying.value = NowPlaying(
            url = url, title = title, subtitle = subtitle,
            isLive = true, liveStreamId = streamId, castUrl = castUrl,
        )
        start()
    }

    fun playVod(url: String, title: String, subtitle: String? = null) {
        _nowPlaying.value = NowPlaying(
            url = url, title = title, subtitle = subtitle, isLive = false, castUrl = url,
        )
        start()
    }

    /** Re-prepare the current item (used by error retry, incl. container switch). */
    fun retry(withUrl: String? = null) {
        val current = _nowPlaying.value ?: return
        // Only `url` changes: the container fallback must not touch castUrl.
        if (withUrl != null) _nowPlaying.value = current.copy(url = withUrl)
        start()
    }

    private fun start() {
        val np = _nowPlaying.value ?: return
        _error.value = null
        lastHttpErrorCode = null
        // Registers the session listener; also adopts a session that connected
        // before playback started.
        initCasting()
        val target = _currentPlayer.value
        if (target === player) {
            // Left byte-for-byte as it was: this is the zapping path, and the
            // no-position overload is what has always been used here.
            with(player) {
                setMediaItem(localItem(np))
                prepare()
                playWhenReady = true
            }
            return
        }
        val cast = castItem(np, fromPositionMs = 0L) ?: run {
            // Leave the receiver idle rather than faking success.
            reportError(CastMedia.UNSUPPORTED_MESSAGE)
            return
        }
        with(target) {
            setMediaItem(cast.first, cast.second)
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
        // Only the active player needs stopping: transferTo() already stops the
        // one it hands off from, so the inactive player holds no connection.
        _currentPlayer.value.stop()
        _currentPlayer.value.clearMediaItems()
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
