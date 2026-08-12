package com.denham.skyline.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.denham.skyline.ui.components.LiveBadge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.data.db.ContentType
import com.denham.skyline.data.db.EpgNowNextEntity
import com.denham.skyline.data.db.FavoriteEntity
import com.denham.skyline.di.AppContainer
import com.denham.skyline.ui.components.ArtworkImage
import com.denham.skyline.ui.theme.SkyPalette
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LiveViewModel(
    private val container: AppContainer,
    /** Sports tab: restrict the chips to sports categories and default into one. */
    val sportsOnly: Boolean = false,
) : ViewModel() {

    val categories = container.db.categoryDao().observe(ContentType.LIVE)
        .map { cats ->
            if (sportsOnly) cats.filter { com.denham.skyline.core.CategoryKeywords.isSports(it.name) }
            else cats
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly

    init {
        // Sports has no "All" chip, so land on the first sports category as soon
        // as the catalogue is available.
        if (sportsOnly) {
            viewModelScope.launch {
                categories.collect { cats ->
                    if (_selectedCategory.value == null && cats.isNotEmpty()) {
                        _selectedCategory.value = cats.first().categoryId
                    }
                }
            }
        }
    }

    val favoriteIds = container.db.favoriteDao().observeIds(ContentType.LIVE)
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val hasFavorites = favoriteIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Bounded paging keeps memory flat on 10k+ channel providers. */
    val channels: Flow<PagingData<ChannelEntity>> = kotlin.run {
        val categoryAndFavorites = _selectedCategory.flatMapLatest { categoryId ->
            _showFavoritesOnly.map { showFavoritesOnly ->
                Pair(categoryId, showFavoritesOnly)
            }
        }
        categoryAndFavorites.flatMapLatest { (categoryId, showFavoritesOnly) ->
            Pager(
                PagingConfig(pageSize = 60, maxSize = 400, enablePlaceholders = false)
            ) {
                when {
                    showFavoritesOnly -> container.db.channelDao().pagingByFavorites()
                    categoryId == null -> container.db.channelDao().pagingAll()
                    else -> container.db.channelDao().pagingByCategory(categoryId)
                }
            }.flow
        }.cachedIn(viewModelScope)
    }

    /** Now/next EPG for the channel ids currently on screen. */
    private val visibleIds = MutableStateFlow<List<Int>>(emptyList())
    val epg: StateFlow<Map<Int, EpgNowNextEntity>> = visibleIds
        .flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyList<EpgNowNextEntity>())
            else container.epgRepository.observeForIds(ids)
        }
        .map { list -> list.associateBy { it.streamId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectCategory(id: String?) {
        _selectedCategory.value = id
    }

    fun setShowFavoritesOnly(show: Boolean) {
        _showFavoritesOnly.value = show
    }

    fun onChannelVisible(streamId: Int) {
        visibleIds.value = (visibleIds.value + streamId).takeLast(80).distinct()
        viewModelScope.launch { container.epgRepository.refresh(streamId) }
    }

    fun toggleFavorite(streamId: Int) {
        viewModelScope.launch {
            val dao = container.db.favoriteDao()
            if (dao.exists(ContentType.LIVE, streamId) > 0) {
                dao.delete(ContentType.LIVE, streamId)
            } else {
                dao.insert(FavoriteEntity(ContentType.LIVE, streamId, System.currentTimeMillis()))
            }
        }
    }
}

@Composable
fun LiveScreen(
    viewModel: LiveViewModel,
    onPlayChannel: (ChannelEntity) -> Unit,
    title: String = "Live",
) {
    val categories by viewModel.categories.collectAsState()
    val selected by viewModel.selectedCategory.collectAsState()
    val favorites by viewModel.favoriteIds.collectAsState()
    val hasFavorites by viewModel.hasFavorites.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val epg by viewModel.epg.collectAsState()
    val channels = viewModel.channels.collectAsLazyPagingItems()

    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.Canvas),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Center),
            )
            com.denham.skyline.ui.components.CastButton(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(32.dp),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (hasFavorites) {
                item {
                    FilterChip(
                        selected = showFavoritesOnly,
                        onClick = { viewModel.setShowFavoritesOnly(!showFavoritesOnly) },
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
            if (!viewModel.sportsOnly) {
                item {
                    CategoryChip("All", selected == null && !showFavoritesOnly) {
                        viewModel.selectCategory(null)
                        viewModel.setShowFavoritesOnly(false)
                    }
                }
            }
            items(categories.size, key = { categories[it].categoryId }) { i ->
                val cat = categories[i]
                CategoryChip(cat.name, selected == cat.categoryId && !showFavoritesOnly) {
                    viewModel.selectCategory(cat.categoryId)
                    viewModel.setShowFavoritesOnly(false)
                }
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {
            // Featured card for the first channel, mock "Watch Live" style.
            item {
                val featured = if (channels.itemCount > 0) channels.peek(0) else null
                if (featured != null) {
                    FeaturedLiveCard(
                        channel = featured,
                        epg = epg[featured.streamId],
                        onWatch = { onPlayChannel(featured) },
                    )
                }
            }
            item {
                Text(
                    "Up Next",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(channels.itemCount, key = { i -> channels[i]?.streamId ?: -i }) { index ->
                val channel = channels[index] ?: return@items
                LaunchedEffect(channel.streamId) {
                    viewModel.onChannelVisible(channel.streamId)
                }
                ChannelRow(
                    channel = channel,
                    epg = epg[channel.streamId],
                    isFavorite = channel.streamId in favorites,
                    onClick = { onPlayChannel(channel) },
                    onToggleFavorite = { viewModel.toggleFavorite(channel.streamId) },
                )
            }
        }
    }
}

/** Big rounded featured card: artwork, LIVE badge, titles, Watch Live button. */
@Composable
private fun FeaturedLiveCard(
    channel: ChannelEntity,
    epg: EpgNowNextEntity?,
    onWatch: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(SkyPalette.Surface),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(SkyPalette.SurfaceElevated, SkyPalette.Indigo.copy(alpha = 0.6f))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            ArtworkImage(
                url = channel.icon,
                contentDescription = channel.name,
                fallbackIcon = Icons.Default.LiveTv,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(36.dp),
            )
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                LiveBadge()
            }
        }
        Column(Modifier.padding(14.dp)) {
            Text(
                epg?.nowTitle ?: channel.name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (epg?.nowTitle != null) channel.name else "Live now",
                style = MaterialTheme.typography.bodySmall,
                color = SkyPalette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onWatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                Spacer(Modifier.size(6.dp))
                Text("Watch Live", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
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

@Composable
private fun ChannelRow(
    channel: ChannelEntity,
    epg: EpgNowNextEntity?,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(
            Modifier
                .width(72.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(SkyPalette.SurfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            ArtworkImage(
                url = channel.icon,
                contentDescription = channel.name,
                fallbackIcon = Icons.Default.LiveTv,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                channel.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val nowTitle = epg?.nowTitle
            if (!nowTitle.isNullOrBlank()) {
                Text(
                    nowTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val nextTitle = epg?.nextTitle
            if (!nextTitle.isNullOrBlank()) {
                Text(
                    "Next: $nextTitle",
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyPalette.TextSecondary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = if (isFavorite) "Remove favourite" else "Add favourite",
                tint = if (isFavorite) SkyPalette.Accent else SkyPalette.TextSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
