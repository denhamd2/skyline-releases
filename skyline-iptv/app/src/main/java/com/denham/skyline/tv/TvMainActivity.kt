package com.denham.skyline.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.denham.skyline.SkylineApp
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.data.db.MovieEntity
import com.denham.skyline.di.AppContainer
import com.denham.skyline.ui.browse.MoviesViewModel
import com.denham.skyline.ui.browse.SeriesViewModel
import com.denham.skyline.ui.components.SkylineWordmark
import com.denham.skyline.ui.guide.GuideViewModel
import com.denham.skyline.ui.home.HomeViewModel
import com.denham.skyline.ui.login.LoginScreen
import com.denham.skyline.ui.login.LoginViewModel
import com.denham.skyline.ui.player.PlayerViewModel
import com.denham.skyline.ui.theme.SkyPalette
import com.denham.skyline.ui.theme.SkylineTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class TvTab(val label: String) {
    HOME("Home"),
    GUIDE("TV guide"),
    BROWSE("Browse"),
    FILMS("Films"),
    SERIES("TV shows"),
    SPORTS("Sports"),
    KIDS("Kids"),
    MY_SKY("My Sky"),
}

/** Ten-foot entry point: same engine, remote-first shell. */
@UnstableApi
class TvMainActivity : ComponentActivity() {

    private val container get() = (application as SkylineApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkylineTheme {
                val account by container.account.collectAsState()
                if (account == null) {
                    val vm = tvViewModel { LoginViewModel(it) }
                    LoginScreen(vm) { /* account flow flips the gate */ }
                } else {
                    TvRoot(container)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) container.playerManager.stopAndClear()
    }
}

@UnstableApi
@Composable
private fun tvContainer(): AppContainer =
    (androidx.compose.ui.platform.LocalContext.current.applicationContext as SkylineApp).container

@UnstableApi
@Composable
private inline fun <reified VM : androidx.lifecycle.ViewModel> tvViewModel(
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = tvContainer()
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}

@UnstableApi
@Composable
private fun TvRoot(container: AppContainer) {
    var tab by remember { mutableStateOf(TvTab.HOME) }
    var playing by remember { mutableStateOf(false) }
    val playerVm = tvViewModel { PlayerViewModel(it) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    fun playChannel(channel: ChannelEntity) {
        playerVm.playChannel(channel)
        playing = true
    }

    fun playMovie(movie: MovieEntity) {
        val url = container.urlBuilder().movie(movie.streamId, movie.containerExtension)
        playerVm.playVod(url, movie.name, movie.icon)
        playing = true
    }

    fun playYoutube(videoId: String, title: String, thumbnailUrl: String?) {
        // Hand off to the YouTube app on TV rather than the ExoPlayer path,
        // which never worked (a watch URL is a web page, not a stream). Most
        // Android TV devices ship the YouTube app, which is a far better
        // 10-foot experience than an embedded WebView driven by a D-pad.
        // Launched from a composable, not the Activity, so the context comes
        // from LocalContext and needs NEW_TASK in case it is not an Activity.
        runCatching {
            context.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.youtube.com/watch?v=$videoId"),
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun resume(last: com.denham.skyline.data.prefs.LastPlayed) {
        if (last.type == "live") {
            scope.launch {
                container.db.channelDao().byId(last.id)?.let(::playChannel)
            }
        } else {
            playerVm.playVod(last.playbackUrl, last.title, last.imageUrl, last.positionMs)
            playing = true
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.tvBackground),
    ) {
        Column(Modifier.fillMaxSize()) {
            TvTopBar(tab, onSelect = { tab = it })
            Crossfade(
                targetState = tab,
                animationSpec = tween(220),
                label = "tvTab",
            ) { current ->
                when (current) {
                TvTab.HOME -> {
                    val vm = tvViewModel { HomeViewModel(it) }
                    TvHomeScreen(vm, ::playChannel, ::playMovie, ::resume, ::playYoutube)
                }
                TvTab.GUIDE -> {
                    val vm = tvViewModel { GuideViewModel(it) }
                    TvGuideScreen(vm, ::playChannel)
                }
                TvTab.BROWSE -> TvBrowseScreen { tile ->
                    tab = when (tile.kind) {
                        "films" -> TvTab.FILMS
                        "series" -> TvTab.SERIES
                        "sports" -> TvTab.SPORTS
                        "kids" -> TvTab.KIDS
                        else -> TvTab.GUIDE
                    }
                }
                TvTab.FILMS -> {
                    val vm = tvViewModel { MoviesViewModel(it) }
                    TvFilmsScreen(vm, ::playMovie)
                }
                TvTab.SERIES -> {
                    val vm = tvViewModel { SeriesViewModel(it) }
                    TvSeriesScreen(vm) { show ->
                        // Detail pages are phone-only for now: play episode 1.
                        scope.launch {
                            val info = runCatching {
                                container.contentRepository.seriesInfo(show.seriesId)
                            }.getOrNull() ?: return@launch
                            val firstSeason = info.episodes.keys
                                .sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
                                .firstOrNull() ?: return@launch
                            val episode = info.episodes[firstSeason]?.firstOrNull() ?: return@launch
                            val url = container.urlBuilder()
                                .seriesEpisode(episode.id, episode.containerExtension)
                            playerVm.playVod(url, "${show.name} — E${episode.episodeNum}", show.cover)
                            playing = true
                        }
                    }
                }
                TvTab.SPORTS -> TvSportsScreen(container, ::playChannel)
                TvTab.KIDS -> TvKidsScreen(container, ::playChannel)
                TvTab.MY_SKY -> {
                    val settingsScope = androidx.compose.runtime.rememberCoroutineScope()
                    TvMySkyScreen(
                        container = container,
                        onSignOut = { settingsScope.launch { container.signOut() } },
                        onRefreshLibrary = {
                            settingsScope.launch {
                                container.contentRepository.syncAll(force = true)
                            }
                        },
                    )
                }
                }
            }
        }

        if (playing) {
            TvPlayerOverlay(
                viewModel = playerVm,
                onDismiss = {
                    playerVm.stop()
                    playing = false
                },
            )
        }
    }

    BackHandler(enabled = playing) {
        playerVm.stop()
        playing = false
    }
    BackHandler(enabled = !playing && tab != TvTab.HOME) {
        tab = TvTab.HOME
    }
}

@Composable
private fun TvTopBar(active: TvTab, onSelect: (TvTab) -> Unit) {
    val clockFormat = remember { SimpleDateFormat("h:mma", Locale.getDefault()) }
    var clock by remember { mutableStateOf(clockFormat.format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = clockFormat.format(Date())
            kotlinx.coroutines.delay(30_000)
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 12.dp),
    ) {
        SkylineWordmark(showGo = false)
        Spacer(Modifier.size(20.dp))
        TvTab.entries.forEach { tab ->
            TvNavTab(tab.label, active == tab) { onSelect(tab) }
        }
        Spacer(Modifier.weight(1f))
        // Settings shortcut beside the clock (mock's top-right gear).
        Box(
            Modifier
                .tvClickable { onSelect(TvTab.MY_SKY) }
                .padding(6.dp),
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = if (active == TvTab.MY_SKY) Color.White else SkyPalette.TextSecondary,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Text(
            clock.lowercase(),
            style = MaterialTheme.typography.titleSmall,
            color = SkyPalette.TextSecondary,
        )
    }
}

/** Fullscreen playback: center = pause/play, up/down = channel zap, back = exit. */
@UnstableApi
@Composable
private fun TvPlayerOverlay(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var hudVisible by remember { mutableStateOf(true) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // Scrubber ticker.
    LaunchedEffect(Unit) {
        while (true) {
            positionMs = viewModel.player.currentPosition
            durationMs = viewModel.player.duration
                .takeIf { it != androidx.media3.common.C.TIME_UNSET } ?: 0L
            kotlinx.coroutines.delay(500)
        }
    }
    LaunchedEffect(hudVisible, state.title) {
        if (hudVisible) {
            kotlinx.coroutines.delay(4000)
            hudVisible = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter -> {
                        hudVisible = true
                        viewModel.togglePlayPause()
                        true
                    }
                    Key.DirectionUp, Key.ChannelUp -> {
                        hudVisible = true
                        viewModel.zap(forward = true)
                        true
                    }
                    Key.DirectionDown, Key.ChannelDown -> {
                        hudVisible = true
                        viewModel.zap(forward = false)
                        true
                    }
                    Key.DirectionLeft, Key.MediaRewind -> {
                        hudVisible = true
                        viewModel.seekBy(-10_000)
                        true
                    }
                    Key.DirectionRight, Key.MediaFastForward -> {
                        hudVisible = true
                        viewModel.seekBy(10_000)
                        true
                    }
                    else -> false
                }
            }
            .clickable { hudVisible = !hudVisible },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    keepScreenOn = true
                }
            },
            update = { it.player = viewModel.player },
            modifier = Modifier.fillMaxSize(),
        )

        if (state.isBuffering && state.error == null) {
            CircularProgressIndicator(
                Modifier
                    .align(Alignment.Center)
                    .size(52.dp),
                color = SkyPalette.Accent,
            )
        }

        state.error?.let { message ->
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(message, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.size(12.dp))
                Box(
                    Modifier
                        .tvClickable { viewModel.retryFromUi() }
                        .background(SkyPalette.Accent)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) { Text("Try again") }
            }
        }

        if (hudVisible && state.error == null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 40.dp, vertical = 16.dp),
            ) {
                // Scrub bar for on-demand playback: elapsed / progress / duration.
                if (!state.isLive && durationMs > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatPlayerTime(positionMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                        )
                        LinearProgressIndicator(
                            progress = { (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) },
                            color = SkyPalette.Accent,
                            trackColor = SkyPalette.SurfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp),
                        )
                        Text(
                            formatPlayerTime(durationMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null, tint = Color.White, modifier = Modifier.size(28.dp),
                    )
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(
                            state.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
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
                    }
                    Text(
                        if (state.isLive) "CH ▲▼ to zap · OK to pause"
                        else "◀ ▶ to seek 10s · OK to pause",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyPalette.TextMuted,
                    )
                }
            }
        }
    }
}

/** mm:ss or h:mm:ss for the TV scrub bar. */
private fun formatPlayerTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
