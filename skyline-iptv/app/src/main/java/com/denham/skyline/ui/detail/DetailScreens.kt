package com.denham.skyline.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denham.skyline.core.EpisodeDto
import com.denham.skyline.core.VodDetailInfo
import com.denham.skyline.data.db.MovieEntity
import com.denham.skyline.data.db.SeriesEntity
import com.denham.skyline.di.AppContainer
import com.denham.skyline.ui.components.ArtworkImage
import com.denham.skyline.ui.theme.SkyPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Movie detail
// ---------------------------------------------------------------------------

data class MovieDetailState(
    val movie: MovieEntity? = null,
    val info: VodDetailInfo? = null,
    val loading: Boolean = true,
)

class MovieDetailViewModel(
    private val container: AppContainer,
    private val movieId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(MovieDetailState())
    val state: StateFlow<MovieDetailState> = _state

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite

    private val _isDownloaded = MutableStateFlow(false)
    val isDownloaded: StateFlow<Boolean> = _isDownloaded

    fun toggleFavorite() {
        viewModelScope.launch {
            val dao = container.db.favoriteDao()
            if (dao.exists(com.denham.skyline.data.db.ContentType.VOD, movieId) > 0) {
                dao.delete(com.denham.skyline.data.db.ContentType.VOD, movieId)
                _isFavorite.value = false
            } else {
                dao.insert(
                    com.denham.skyline.data.db.FavoriteEntity(
                        com.denham.skyline.data.db.ContentType.VOD, movieId,
                        System.currentTimeMillis(),
                    )
                )
                _isFavorite.value = true
            }
        }
    }

    /** Progressive files only — HLS VOD can't be downloaded. */
    fun isDownloadable(): Boolean {
        val ext = _state.value.movie?.containerExtension?.lowercase() ?: return false
        return ext != "m3u8" && ext != "ts"
    }

    fun download() {
        val movie = _state.value.movie ?: return
        val url = playbackUrl() ?: return
        viewModelScope.launch {
            container.db.downloadMetaDao().upsert(
                com.denham.skyline.data.db.DownloadMetaEntity(
                    contentId = url,
                    title = movie.name,
                    imageUrl = movie.icon,
                    subtitle = null,
                    addedAt = System.currentTimeMillis(),
                )
            )
            container.downloadsCenter.startDownload(url)
            _isDownloaded.value = true
        }
    }

    init {
        viewModelScope.launch {
            val movie = container.db.movieDao().byId(movieId)
            _state.value = MovieDetailState(movie = movie, loading = true)
            _isFavorite.value = container.db.favoriteDao()
                .exists(com.denham.skyline.data.db.ContentType.VOD, movieId) > 0
            playbackUrl()?.let { _isDownloaded.value = container.downloadsCenter.isDownloaded(it) }
            val info = runCatching { container.contentRepository.vodInfo(movieId) }.getOrNull()
            _state.value = MovieDetailState(
                movie = movie,
                info = info?.info,
                loading = false,
            )
        }
    }

    /** Playable URL for this movie (container_extension from the catalogue). */
    fun playbackUrl(): String? {
        val movie = _state.value.movie ?: return null
        return container.urlBuilder().movie(movie.streamId, movie.containerExtension)
    }
}

@Composable
fun MovieDetailScreen(
    viewModel: MovieDetailViewModel,
    onBack: () -> Unit,
    onPlay: (url: String, title: String, imageUrl: String?) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val movie = state.movie

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.Canvas),
    ) {
        item {
            DetailHero(
                title = movie?.name ?: "",
                imageUrl = state.info?.backdropPath?.firstOrNull()
                    ?: state.info?.movieImage ?: movie?.icon,
                onBack = onBack,
            )
        }
        item {
            Column(Modifier.padding(16.dp)) {
                MetaRow(
                    rating5 = movie?.rating5 ?: 0.0,
                    extras = listOfNotNull(
                        state.info?.genre,
                        state.info?.releaseDate?.take(4),
                        state.info?.duration?.takeIf { it.isNotBlank() },
                    ),
                )
                Spacer(Modifier.height(14.dp))
                val isFavorite by viewModel.isFavorite.collectAsState()
                val isDownloaded by viewModel.isDownloaded.collectAsState()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            val url = viewModel.playbackUrl() ?: return@Button
                            onPlay(url, movie?.name ?: "", movie?.icon)
                        },
                        enabled = movie != null,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(22.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Play", style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "My List",
                            tint = if (isFavorite) SkyPalette.Accent else SkyPalette.TextSecondary,
                        )
                    }
                    if (viewModel.isDownloadable()) {
                        IconButton(onClick = viewModel::download, enabled = !isDownloaded) {
                            Icon(
                                if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                "Download",
                                tint = if (isDownloaded) SkyPalette.Accent else SkyPalette.TextSecondary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (state.loading) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                } else {
                    state.info?.plot?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = SkyPalette.TextSecondary)
                        Spacer(Modifier.height(12.dp))
                    }
                    state.info?.cast?.takeIf { it.isNotBlank() }?.let {
                        Text("Cast", style = MaterialTheme.typography.titleSmall)
                        Text(it, style = MaterialTheme.typography.bodySmall, color = SkyPalette.TextSecondary)
                        Spacer(Modifier.height(8.dp))
                    }
                    state.info?.director?.takeIf { it.isNotBlank() }?.let {
                        Text("Director", style = MaterialTheme.typography.titleSmall)
                        Text(it, style = MaterialTheme.typography.bodySmall, color = SkyPalette.TextSecondary)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Series detail
// ---------------------------------------------------------------------------

data class SeriesDetailState(
    val series: SeriesEntity? = null,
    val seasons: Map<String, List<EpisodeDto>> = emptyMap(),
    val selectedSeason: String? = null,
    val loading: Boolean = true,
)

class SeriesDetailViewModel(
    private val container: AppContainer,
    private val seriesId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(SeriesDetailState())
    val state: StateFlow<SeriesDetailState> = _state

    init {
        viewModelScope.launch {
            val series = container.db.seriesDao().byId(seriesId)
            _state.value = SeriesDetailState(series = series, loading = true)
            val info = runCatching { container.contentRepository.seriesInfo(seriesId) }.getOrNull()
            val seasons = info?.episodes.orEmpty()
            _state.value = SeriesDetailState(
                series = series,
                seasons = seasons,
                selectedSeason = seasons.keys.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }.firstOrNull(),
                loading = false,
            )
        }
    }

    fun selectSeason(season: String) {
        _state.value = _state.value.copy(selectedSeason = season)
    }

    fun episodeUrl(episode: EpisodeDto): String =
        container.urlBuilder().seriesEpisode(episode.id, episode.containerExtension)
}

@Composable
fun SeriesDetailScreen(
    viewModel: SeriesDetailViewModel,
    onBack: () -> Unit,
    onPlay: (url: String, title: String, imageUrl: String?) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val series = state.series
    val seasonKeys = state.seasons.keys.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.Canvas),
    ) {
        item {
            DetailHero(
                title = series?.name ?: "",
                imageUrl = series?.backdrop ?: series?.cover,
                onBack = onBack,
            )
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                MetaRow(
                    rating5 = series?.rating5 ?: 0.0,
                    extras = listOfNotNull(series?.genre),
                )
                series?.plot?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyPalette.TextSecondary,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (state.loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                }
            }
        }

        if (seasonKeys.size > 1) {
            item {
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(seasonKeys.size, key = { seasonKeys[it] }) { i ->
                        val season = seasonKeys[i]
                        androidx.compose.material3.FilterChip(
                            selected = state.selectedSeason == season,
                            onClick = { viewModel.selectSeason(season) },
                            label = { Text("Season $season") },
                        )
                    }
                }
            }
        }

        val episodes = state.seasons[state.selectedSeason].orEmpty()
        items(episodes.size, key = { episodes[it].id }) { i ->
            val episode = episodes[i]
            EpisodeRow(
                episode = episode,
                onClick = {
                    val title = buildString {
                        append(series?.name ?: "")
                        append(" — S${state.selectedSeason}E${episode.episodeNum}")
                        if (episode.title.isNotBlank()) append(" ${episode.title}")
                    }
                    onPlay(viewModel.episodeUrl(episode), title, series?.cover ?: series?.backdrop)
                },
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun EpisodeRow(episode: EpisodeDto, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Surface(
            color = SkyPalette.SurfaceElevated,
            shape = RoundedCornerShape(8.dp),
        ) {
            Box(Modifier.size(width = 56.dp, height = 40.dp), contentAlignment = Alignment.Center) {
                Text(
                    "${episode.episodeNum}",
                    style = MaterialTheme.typography.titleMedium,
                    color = SkyPalette.Accent,
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                episode.title.ifBlank { "Episode ${episode.episodeNum}" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            episode.info?.plot?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyPalette.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(Icons.Default.PlayArrow, "Play", tint = SkyPalette.TextSecondary)
    }
}

// ---------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------

@Composable
private fun DetailHero(title: String, imageUrl: String?, onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            ArtworkImage(
                url = imageUrl,
                contentDescription = title,
                fallbackIcon = Icons.Default.Movie,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Box(Modifier.matchParentSize().background(SkyPalette.heroFallback))
        }
        Box(Modifier.matchParentSize().background(SkyPalette.heroScrim))
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SkyPalette.TextPrimary)
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }
}

@Composable
private fun MetaRow(rating5: Double, extras: List<String>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (rating5 > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star, null,
                    tint = SkyPalette.Accent, modifier = Modifier.size(16.dp),
                )
                Text(
                    " %.1f".format(rating5),
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyPalette.TextPrimary,
                )
            }
        }
        extras.forEach {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = SkyPalette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
