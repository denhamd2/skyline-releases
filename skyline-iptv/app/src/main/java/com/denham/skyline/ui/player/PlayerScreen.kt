package com.denham.skyline.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Rational
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.denham.skyline.core.GuideMath
import com.denham.skyline.core.LiveContainer
import com.denham.skyline.core.XtreamError
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.data.db.MovieEntity
import com.denham.skyline.data.prefs.LastPlayed
import com.denham.skyline.di.AppContainer
import com.denham.skyline.player.httpErrorCode
import com.denham.skyline.ui.components.CastButton
import com.denham.skyline.ui.components.ChannelCard
import com.denham.skyline.ui.components.LiveBadge
import com.denham.skyline.ui.components.PosterCard
import com.denham.skyline.ui.components.ProviderBadge
import com.denham.skyline.ui.components.Rail
import com.denham.skyline.ui.theme.SkyPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val title: String = "",
    val subtitle: String? = null,
    val categoryName: String? = null,
    val isLive: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val error: String? = null,
    val currentChannel: ChannelEntity? = null,
    /** Live: EPG programme window for the progress bar. */
    val programmeStartMs: Long? = null,
    val programmeStopMs: Long? = null,
    /** Related content for the "More like this" rail. */
    val relatedChannels: List<ChannelEntity> = emptyList(),
    val relatedMovies: List<MovieEntity> = emptyList(),
)

/**
 * Owns the retry policy (backoff then TS<->HLS fallback for live) and the
 * Continue Watching bookkeeping.
 */
@UnstableApi
class PlayerViewModel(private val container: AppContainer) : ViewModel() {

    private val manager = container.playerManager

    /** Active playback target — the local player, or the Cast player while a
     *  Chromecast session is connected. */
    val player: Player get() = manager.currentPlayer.value
    val currentPlayer: StateFlow<Player> = manager.currentPlayer
    val castAvailable: Boolean get() = manager.castAvailable

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state

    private var retryCount = 0
    private var triedOtherContainer = false
    private var currentContainer: LiveContainer = LiveContainer.TS

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.value = _state.value.copy(
                isBuffering = playbackState == Player.STATE_BUFFERING,
            )
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }

        override fun onPlayerError(error: PlaybackException) {
            handleError(error)
        }
    }

    init {
        manager.addListener(listener)
        // Persist VOD position every few seconds for Continue Watching.
        viewModelScope.launch {
            while (isActive) {
                delay(5000)
                if (!_state.value.isLive && player.isPlaying) {
                    val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                    container.historyStore.updatePosition(player.currentPosition, duration)
                }
            }
        }
    }

    fun playChannel(channel: ChannelEntity) {
        val settings = container.settingsStore.current.value
        currentContainer = settings.preferredContainer
        retryCount = 0
        triedOtherContainer = false
        _state.value = PlayerUiState(
            title = channel.name,
            isLive = true,
            currentChannel = channel,
        )
        startLive(channel)
        viewModelScope.launch {
            container.epgRepository.refresh(channel.streamId)
            val epg = container.db.epgDao().byId(channel.streamId)
            val category = container.db.categoryDao()
                .nameFor("live", channel.categoryId)
            _state.value = _state.value.copy(
                subtitle = epg?.nowTitle,
                categoryName = category,
                programmeStartMs = epg?.nowStart?.times(1000),
                programmeStopMs = epg?.nowStop?.times(1000),
            )
            container.historyStore.record(
                LastPlayed(
                    type = "live", id = channel.streamId, title = channel.name,
                    imageUrl = channel.icon,
                    playbackUrl = container.urlBuilder().live(channel.streamId, currentContainer),
                    positionMs = 0, durationMs = 0,
                )
            )
            loadRelatedChannels(channel)
        }
    }

    fun playVod(url: String, title: String, imageUrl: String? = null, resumeFromMs: Long = 0L) {
        retryCount = 0
        triedOtherContainer = true // container fallback is live-only
        _state.value = PlayerUiState(title = title, isLive = false)
        manager.playVod(url, title)
        if (resumeFromMs > 0) player.seekTo(resumeFromMs)
        viewModelScope.launch {
            container.historyStore.record(
                LastPlayed(
                    type = "vod", id = 0, title = title, imageUrl = imageUrl,
                    playbackUrl = url, positionMs = resumeFromMs, durationMs = 0,
                )
            )
            val movies = container.db.movieDao().observeRecentlyAdded(12).first()
            _state.value = _state.value.copy(relatedMovies = movies.filter { it.name != title })
        }
    }

    // playYoutube() was removed: it passed a youtube.com/watch URL to the VOD
    // player, but that serves an HTML page rather than a media stream, so
    // ExoPlayer could never play it. YouTube playback now goes through
    // YouTubePlayerScreen, which hands the video to the YouTube app.

    private suspend fun loadRelatedChannels(channel: ChannelEntity) {
        val related = container.db.channelDao()
            .pageByCategory(channel.categoryId, 12, 0)
            .filter { it.streamId != channel.streamId }
        _state.value = _state.value.copy(relatedChannels = related)
    }

    private fun startLive(channel: ChannelEntity) {
        val url = container.urlBuilder().live(channel.streamId, currentContainer)
        manager.playLive(channel.streamId, url, channel.name, null)
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    /** Channel up/down within the current category (TV remote + phone). */
    fun zap(forward: Boolean) {
        val channel = _state.value.currentChannel ?: return
        viewModelScope.launch {
            val dao = container.db.channelDao()
            val next = if (forward) dao.nextInCategory(channel.categoryId, channel.num)
            else dao.previousInCategory(channel.categoryId, channel.num)
            next?.let { playChannel(it) }
        }
    }

    fun seekBy(deltaMs: Long) {
        player.seekTo((player.currentPosition + deltaMs).coerceAtLeast(0))
    }

    fun restart() {
        if (_state.value.isLive) player.seekToDefaultPosition() else player.seekTo(0)
        player.play()
    }

    fun jumpToLiveEdge() {
        player.seekToDefaultPosition()
        player.play()
    }

    /** Cycle subtitle tracks: off -> track1 -> track2 -> off. */
    fun cycleSubtitles() {
        val params = player.trackSelectionParameters
        val disabled = params.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
        player.trackSelectionParameters = params.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !disabled)
            .build()
    }

    /** Cycle audio language among the stream's audio tracks. */
    fun cycleAudio() {
        val groups = player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }
        if (groups.size < 2) return
        val selectedIndex = groups.indexOfFirst { it.isSelected }
        val next = groups[(selectedIndex + 1).mod(groups.size)]
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(
                androidx.media3.common.TrackSelectionOverride(next.mediaTrackGroup, 0)
            )
            .build()
    }

    fun switchContainer() {
        val channel = _state.value.currentChannel ?: return
        currentContainer = currentContainer.other()
        startLive(channel)
    }

    fun retryFromUi() {
        retryCount = 0
        triedOtherContainer = false
        _state.value = _state.value.copy(error = null)
        _state.value.currentChannel?.let { startLive(it) } ?: manager.retry()
    }

    private fun handleError(error: PlaybackException) {
        val httpCode = error.httpErrorCode()
        val s = _state.value

        if (httpCode in setOf(401, 403, 405, 406)) {
            _state.value = s.copy(error = XtreamError.messageFor(httpCode!!))
            return
        }

        viewModelScope.launch {
            when {
                retryCount < MAX_RETRIES -> {
                    retryCount++
                    delay(1000L * retryCount)
                    manager.retry()
                }
                s.isLive && !triedOtherContainer && s.currentChannel != null -> {
                    triedOtherContainer = true
                    retryCount = 0
                    currentContainer = currentContainer.other()
                    startLive(s.currentChannel)
                }
                else -> {
                    val message = when {
                        httpCode != null -> XtreamError.messageFor(httpCode)
                        else -> "Playback failed: ${error.errorCodeName}\n\nIf this keeps happening: ${XtreamError.CONNECTION_LIMIT}"
                    }
                    _state.value = s.copy(error = message)
                }
            }
        }
    }

    fun stop() {
        viewModelScope.launch {
            if (!_state.value.isLive) {
                val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                container.historyStore.updatePosition(player.currentPosition, duration)
            }
            manager.stopAndClear()
        }
    }

    override fun onCleared() {
        manager.removeListener(listener)
    }

    private companion object {
        const val MAX_RETRIES = 2
    }
}

@UnstableApi
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    isInPip: Boolean,
    onBack: () -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit,
    onOpenMovie: (MovieEntity) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // The Fullscreen control locks the activity to landscape; without this the
    // whole app stays sideways after you leave the player. Restore the default
    // (portrait-following) orientation whenever the player screen goes away.
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Position ticker for the scrubber.
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            positionMs = viewModel.player.currentPosition
            durationMs = viewModel.player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            delay(500)
        }
    }

    if (isLandscape || isInPip) {
        // Immersive: video fills the screen; tap for minimal overlay.
        FullscreenVideo(viewModel, state, isInPip, onBack)
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.Canvas)
            .systemBarsPadding(),
    ) {
        // --- Video area ---
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
        ) {
            VideoSurface(viewModel)
            IconButton(onClick = {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                viewModel.stop(); onBack()
            }) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
            // Cast to a Chromecast / Google TV — mock places this top-right.
            CastButton(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(40.dp),
            )
            if (state.isBuffering && state.error == null) {
                CircularProgressIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .size(42.dp),
                    color = SkyPalette.Accent,
                )
            }
            state.error?.let { message ->
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = viewModel::retryFromUi) { Text("Try again") }
                }
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(10.dp))
                    Scrubber(
                        isLive = state.isLive,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        programmeStartMs = state.programmeStartMs,
                        programmeStopMs = state.programmeStopMs,
                        onSeek = { viewModel.player.seekTo(it) },
                    )
                    Spacer(Modifier.height(10.dp))

                    // Big transport controls.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            32.dp, Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        IconButton(onClick = { viewModel.seekBy(-10_000) }) {
                            Icon(
                                Icons.Default.Replay10, "Back 10 seconds",
                                tint = Color.White, modifier = Modifier.size(34.dp),
                            )
                        }
                        Box(
                            Modifier
                                .size(72.dp)
                                .background(SkyPalette.SurfaceElevated, CircleShape)
                                .clickable(onClick = viewModel::togglePlayPause),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (state.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                        IconButton(onClick = { viewModel.seekBy(10_000) }) {
                            Icon(
                                Icons.Default.Forward10, "Forward 10 seconds",
                                tint = Color.White, modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    // Title block.
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            state.title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        state.subtitle?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SkyPalette.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        state.categoryName?.let {
                            Spacer(Modifier.height(6.dp))
                            ProviderBadge(it)
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    // Secondary controls per the mock.
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SecondaryControl(Icons.Default.Replay, "Restart", viewModel::restart)
                        if (state.isLive) {
                            SecondaryControl(Icons.Default.PlayArrow, "Live", viewModel::jumpToLiveEdge)
                        }
                        SecondaryControl(Icons.Default.Subtitles, "Subtitles", viewModel::cycleSubtitles)
                        SecondaryControl(Icons.Default.Audiotrack, "Audio", viewModel::cycleAudio)
                        SecondaryControl(Icons.Default.Fullscreen, "Fullscreen") {
                            activity?.requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                        SecondaryControl(Icons.Default.PictureInPictureAlt, "PiP") {
                            activity?.enterPictureInPictureMode(
                                PictureInPictureParams.Builder()
                                    .setAspectRatio(Rational(16, 9))
                                    .build()
                            )
                        }
                        if (state.isLive) {
                            SecondaryControl(Icons.Default.MoreVert, "Format", viewModel::switchContainer)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }

            if (state.relatedChannels.isNotEmpty()) {
                item {
                    Rail("More like this", state.relatedChannels, key = { it.streamId }) { ch ->
                        ChannelCard(ch.name, ch.icon, onClick = { onPlayChannel(ch) })
                    }
                }
            }
            if (state.relatedMovies.isNotEmpty()) {
                item {
                    Rail("More like this", state.relatedMovies, key = { it.streamId }) { m ->
                        PosterCard(m.name, m.icon, onClick = { onOpenMovie(m) })
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@UnstableApi
@Composable
private fun VideoSurface(viewModel: PlayerViewModel) {
    val currentPlayer by viewModel.currentPlayer.collectAsState()
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                keepScreenOn = true
            }
        },
        update = { view -> view.player = currentPlayer },
        modifier = Modifier.fillMaxSize(),
    )
}

@UnstableApi
@Composable
private fun FullscreenVideo(
    viewModel: PlayerViewModel,
    state: PlayerUiState,
    isInPip: Boolean,
    onBack: () -> Unit,
) {
    var controlsVisible by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { controlsVisible = !controlsVisible },
    ) {
        VideoSurface(viewModel)
        if (state.isBuffering && state.error == null) {
            CircularProgressIndicator(
                Modifier
                    .align(Alignment.Center)
                    .size(46.dp),
                color = SkyPalette.Accent,
            )
        }
        if (controlsVisible && !isInPip) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .systemBarsPadding()
                    .padding(8.dp),
            ) {
                IconButton(onClick = { viewModel.stop(); onBack() }) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }
                Text(
                    state.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                CastButton(Modifier.size(40.dp))
                IconButton(onClick = viewModel::togglePlayPause) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null, tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun SecondaryControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(6.dp),
    ) {
        Icon(icon, label, tint = SkyPalette.TextSecondary, modifier = Modifier.size(22.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = SkyPalette.TextSecondary,
        )
    }
}

@Composable
private fun Scrubber(
    isLive: Boolean,
    positionMs: Long,
    durationMs: Long,
    programmeStartMs: Long?,
    programmeStopMs: Long?,
    onSeek: (Long) -> Unit,
) {
    if (isLive) {
        // Programme progress (not seekable) + LIVE tag, per the mock.
        val now = System.currentTimeMillis()
        val progress = GuideMath.progress(programmeStartMs, programmeStopMs, now)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatClock(programmeStartMs, now),
                style = MaterialTheme.typography.labelSmall,
                color = SkyPalette.TextSecondary,
            )
            Slider(
                value = progress,
                onValueChange = {},
                enabled = false,
                colors = SliderDefaults.colors(
                    disabledThumbColor = SkyPalette.Accent,
                    disabledActiveTrackColor = SkyPalette.Accent,
                    disabledInactiveTrackColor = SkyPalette.SurfaceElevated,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            LiveBadge()
        }
    } else {
        var dragging by remember { mutableStateOf(false) }
        var dragValue by remember { mutableFloatStateOf(0f) }
        val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatTime(positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = SkyPalette.TextSecondary,
            )
            Slider(
                value = if (dragging) dragValue else progress.coerceIn(0f, 1f),
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = {
                    dragging = false
                    if (durationMs > 0) onSeek((dragValue * durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = SkyPalette.Accent,
                    activeTrackColor = SkyPalette.Accent,
                    inactiveTrackColor = SkyPalette.SurfaceElevated,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            Text(
                formatTime(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = SkyPalette.TextSecondary,
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatClock(programmeStartMs: Long?, nowMs: Long): String {
    val elapsed = if (programmeStartMs != null) nowMs - programmeStartMs else 0L
    return formatTime(elapsed.coerceAtLeast(0))
}
