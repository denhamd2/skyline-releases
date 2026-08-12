package com.denham.skyline.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.data.db.ContentType
import com.denham.skyline.data.db.MovieEntity
import com.denham.skyline.data.db.SeriesEntity
import com.denham.skyline.di.AppContainer
import com.denham.skyline.ui.components.ArtworkImage
import com.denham.skyline.ui.theme.SkyPalette
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchResults(
    val channels: List<ChannelEntity> = emptyList(),
    val movies: List<MovieEntity> = emptyList(),
    val series: List<SeriesEntity> = emptyList(),
) {
    val isEmpty get() = channels.isEmpty() && movies.isEmpty() && series.isEmpty()
}

@OptIn(FlowPreview::class)
class SearchViewModel(private val container: AppContainer) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _contentTypeFilter = MutableStateFlow("all") // all, live, movies, series
    val contentTypeFilter: StateFlow<String> = _contentTypeFilter

    val categories = container.db.categoryDao().observe(com.denham.skyline.data.db.ContentType.LIVE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _results = MutableStateFlow(SearchResults())
    val results: StateFlow<SearchResults> = _results

    init {
        viewModelScope.launch {
            _query.debounce(250).collect { q ->
                performSearch(q, _selectedCategory.value, _contentTypeFilter.value)
            }
        }
        viewModelScope.launch {
            _selectedCategory.collect { _ ->
                performSearch(_query.value, _selectedCategory.value, _contentTypeFilter.value)
            }
        }
        viewModelScope.launch {
            _contentTypeFilter.collect { _ ->
                performSearch(_query.value, _selectedCategory.value, _contentTypeFilter.value)
            }
        }
    }

    private suspend fun performSearch(query: String, categoryId: String?, contentType: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _results.value = SearchResults()
            return
        }
        val ftsQuery = "\"${trimmed.replace("\"", "")}\"*"

        var channels = if (contentType == "all" || contentType == "live") {
            runCatching {
                val all = container.db.channelDao().search(ftsQuery)
                if (categoryId != null) all.filter { it.categoryId == categoryId } else all
            }.getOrDefault(emptyList())
        } else emptyList()

        val movies = if (contentType == "all" || contentType == "movies") {
            runCatching { container.db.movieDao().search(ftsQuery) }.getOrDefault(emptyList())
        } else emptyList()

        val series = if (contentType == "all" || contentType == "series") {
            runCatching { container.db.seriesDao().search(ftsQuery) }.getOrDefault(emptyList())
        } else emptyList()

        _results.value = SearchResults(channels = channels, movies = movies, series = series)
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun selectCategory(id: String?) {
        _selectedCategory.value = id
    }

    fun selectContentType(type: String) {
        _contentTypeFilter.value = type
    }
}

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onPlayChannel: (ChannelEntity) -> Unit,
    onOpenMovie: (MovieEntity) -> Unit,
    onOpenSeries: (SeriesEntity) -> Unit,
) {
    val query by viewModel.query.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val contentTypeFilter by viewModel.contentTypeFilter.collectAsState()
    val results by viewModel.results.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.Canvas),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            placeholder = { Text("Search channels, films, series…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )

        if (query.trim().length >= 2) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = contentTypeFilter == "all",
                        onClick = { viewModel.selectContentType("all") },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SkyPalette.Accent,
                            selectedLabelColor = SkyPalette.TextPrimary,
                            containerColor = SkyPalette.Surface,
                            labelColor = SkyPalette.TextSecondary,
                        ),
                    )
                }
                item {
                    FilterChip(
                        selected = contentTypeFilter == "live",
                        onClick = { viewModel.selectContentType("live") },
                        label = { Text("Live") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SkyPalette.Accent,
                            selectedLabelColor = SkyPalette.TextPrimary,
                            containerColor = SkyPalette.Surface,
                            labelColor = SkyPalette.TextSecondary,
                        ),
                    )
                }
                item {
                    FilterChip(
                        selected = contentTypeFilter == "movies",
                        onClick = { viewModel.selectContentType("movies") },
                        label = { Text("Films") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SkyPalette.Accent,
                            selectedLabelColor = SkyPalette.TextPrimary,
                            containerColor = SkyPalette.Surface,
                            labelColor = SkyPalette.TextSecondary,
                        ),
                    )
                }
                item {
                    FilterChip(
                        selected = contentTypeFilter == "series",
                        onClick = { viewModel.selectContentType("series") },
                        label = { Text("Series") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SkyPalette.Accent,
                            selectedLabelColor = SkyPalette.TextPrimary,
                            containerColor = SkyPalette.Surface,
                            labelColor = SkyPalette.TextSecondary,
                        ),
                    )
                }
            }

            if (contentTypeFilter == "all" || contentTypeFilter == "live") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("All categories") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SkyPalette.Accent,
                                selectedLabelColor = SkyPalette.TextPrimary,
                                containerColor = SkyPalette.Surface,
                                labelColor = SkyPalette.TextSecondary,
                            ),
                        )
                    }
                    items(categories.size, key = { categories[it].categoryId }) { i ->
                        val cat = categories[i]
                        FilterChip(
                            selected = selectedCategory == cat.categoryId,
                            onClick = { viewModel.selectCategory(cat.categoryId) },
                            label = { Text(cat.name, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SkyPalette.Accent,
                                selectedLabelColor = SkyPalette.TextPrimary,
                                containerColor = SkyPalette.Surface,
                                labelColor = SkyPalette.TextSecondary,
                            ),
                        )
                    }
                }
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {
            if (results.channels.isNotEmpty()) {
                item { SectionHeader("Live channels") }
                items(results.channels.size, key = { "c${results.channels[it].streamId}" }) { i ->
                    val ch = results.channels[i]
                    ResultRow(ch.name, ch.icon, Icons.Default.LiveTv) { onPlayChannel(ch) }
                }
            }
            if (results.movies.isNotEmpty()) {
                item { SectionHeader("Films") }
                items(results.movies.size, key = { "m${results.movies[it].streamId}" }) { i ->
                    val m = results.movies[i]
                    ResultRow(m.name, m.icon, Icons.Default.Movie) { onOpenMovie(m) }
                }
            }
            if (results.series.isNotEmpty()) {
                item { SectionHeader("Series") }
                items(results.series.size, key = { "s${results.series[it].seriesId}" }) { i ->
                    val s = results.series[i]
                    ResultRow(s.name, s.cover, Icons.Default.Tv) { onOpenSeries(s) }
                }
            }
            if (query.trim().length >= 2 && results.isEmpty) {
                item {
                    Text(
                        "No results for \"${query.trim()}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyPalette.TextSecondary,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ResultRow(
    name: String,
    imageUrl: String?,
    fallbackIcon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        ArtworkImage(
            url = imageUrl,
            contentDescription = name,
            fallbackIcon = fallbackIcon,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(64.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(SkyPalette.SurfaceElevated)
                .padding(4.dp),
        )
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
