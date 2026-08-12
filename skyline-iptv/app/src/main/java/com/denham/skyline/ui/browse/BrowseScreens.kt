package com.denham.skyline.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import com.denham.skyline.data.db.ContentType
import com.denham.skyline.data.db.MovieEntity
import com.denham.skyline.data.db.SeriesEntity
import com.denham.skyline.di.AppContainer
import com.denham.skyline.ui.components.PosterCard
import com.denham.skyline.ui.theme.SkyPalette
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class MoviesViewModel(private val container: AppContainer) : ViewModel() {

    val categories = container.db.categoryDao().observe(ContentType.VOD)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly

    val favoriteIds = container.db.favoriteDao().observeIds(ContentType.VOD)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasFavorites = favoriteIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val movies: Flow<PagingData<MovieEntity>> = kotlin.run {
        val categoryAndFavorites = _selectedCategory.flatMapLatest { categoryId ->
            _showFavoritesOnly.map { showFavoritesOnly ->
                Pair(categoryId, showFavoritesOnly)
            }
        }
        categoryAndFavorites.flatMapLatest { (categoryId, showFavoritesOnly) ->
            Pager(PagingConfig(pageSize = 60, maxSize = 400, enablePlaceholders = false)) {
                when {
                    showFavoritesOnly -> container.db.movieDao().pagingByFavorites()
                    categoryId == null -> container.db.movieDao().pagingAll()
                    else -> container.db.movieDao().pagingByCategory(categoryId)
                }
            }.flow
        }.cachedIn(viewModelScope)
    }

    fun selectCategory(id: String?) {
        _selectedCategory.value = id
    }

    fun setShowFavoritesOnly(show: Boolean) {
        _showFavoritesOnly.value = show
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SeriesViewModel(private val container: AppContainer) : ViewModel() {

    val categories = container.db.categoryDao().observe(ContentType.SERIES)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly

    val favoriteIds = container.db.favoriteDao().observeIds(ContentType.SERIES)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasFavorites = favoriteIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val series: Flow<PagingData<SeriesEntity>> = kotlin.run {
        val categoryAndFavorites = _selectedCategory.flatMapLatest { categoryId ->
            _showFavoritesOnly.map { showFavoritesOnly ->
                Pair(categoryId, showFavoritesOnly)
            }
        }
        categoryAndFavorites.flatMapLatest { (categoryId, showFavoritesOnly) ->
            Pager(PagingConfig(pageSize = 60, maxSize = 400, enablePlaceholders = false)) {
                when {
                    showFavoritesOnly -> container.db.seriesDao().pagingByFavorites()
                    categoryId == null -> container.db.seriesDao().pagingAll()
                    else -> container.db.seriesDao().pagingByCategory(categoryId)
                }
            }.flow
        }.cachedIn(viewModelScope)
    }

    fun selectCategory(id: String?) {
        _selectedCategory.value = id
    }

    fun setShowFavoritesOnly(show: Boolean) {
        _showFavoritesOnly.value = show
    }
}

@Composable
fun MoviesScreen(viewModel: MoviesViewModel, onOpenMovie: (MovieEntity) -> Unit) {
    val categories by viewModel.categories.collectAsState()
    val selected by viewModel.selectedCategory.collectAsState()
    val hasFavorites by viewModel.hasFavorites.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val movies = viewModel.movies.collectAsLazyPagingItems()

    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.Canvas),
    ) {
        CategoryChipsRow(
            categories = categories.map { it.categoryId to it.name },
            selected = selected,
            onSelect = viewModel::selectCategory,
            hasFavorites = hasFavorites,
            showFavoritesOnly = showFavoritesOnly,
            onToggleFavorites = { viewModel.setShowFavoritesOnly(!showFavoritesOnly) },
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(movies.itemCount, key = { i -> movies[i]?.streamId ?: -i }) { index ->
                val movie = movies[index] ?: return@items
                PosterCard(
                    title = movie.name,
                    imageUrl = movie.icon,
                    onClick = { onOpenMovie(movie) },
                    width = 110.dp,
                )
            }
        }
    }
}

@Composable
fun SeriesScreen(viewModel: SeriesViewModel, onOpenSeries: (SeriesEntity) -> Unit) {
    val categories by viewModel.categories.collectAsState()
    val selected by viewModel.selectedCategory.collectAsState()
    val hasFavorites by viewModel.hasFavorites.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val series = viewModel.series.collectAsLazyPagingItems()

    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.Canvas),
    ) {
        CategoryChipsRow(
            categories = categories.map { it.categoryId to it.name },
            selected = selected,
            onSelect = viewModel::selectCategory,
            hasFavorites = hasFavorites,
            showFavoritesOnly = showFavoritesOnly,
            onToggleFavorites = { viewModel.setShowFavoritesOnly(!showFavoritesOnly) },
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(series.itemCount, key = { i -> series[i]?.seriesId ?: -i }) { index ->
                val show = series[index] ?: return@items
                PosterCard(
                    title = show.name,
                    imageUrl = show.cover,
                    onClick = { onOpenSeries(show) },
                    width = 110.dp,
                )
            }
        }
    }
}

@Composable
fun CategoryChipsRow(
    categories: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String?) -> Unit,
    hasFavorites: Boolean = false,
    showFavoritesOnly: Boolean = false,
    onToggleFavorites: () -> Unit = {},
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (hasFavorites) {
            item {
                FilterChip(
                    selected = showFavoritesOnly,
                    onClick = {
                        onToggleFavorites()
                    },
                    label = { Text("❤ Favorites", maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SkyPalette.Accent,
                        selectedLabelColor = SkyPalette.TextPrimary,
                        containerColor = SkyPalette.Surface,
                        labelColor = SkyPalette.TextSecondary,
                    ),
                )
            }
        }
        item {
            BrowseChip("All", selected == null && !showFavoritesOnly) {
                onSelect(null)
                if (showFavoritesOnly) onToggleFavorites()
            }
        }
        items(categories.size, key = { categories[it].first }) { i ->
            val (id, name) = categories[i]
            BrowseChip(name, selected == id && !showFavoritesOnly) {
                onSelect(id)
                if (showFavoritesOnly) onToggleFavorites()
            }
        }
    }
}

@Composable
private fun BrowseChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SkyPalette.Accent,
            selectedLabelColor = SkyPalette.TextPrimary,
            containerColor = SkyPalette.Surface,
            labelColor = SkyPalette.TextSecondary,
        ),
    )
}
