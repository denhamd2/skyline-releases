package com.denham.skyline.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.denham.skyline.core.GuideMath
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.data.db.EpgProgrammeEntity
import com.denham.skyline.data.repo.GuideImportState
import com.denham.skyline.di.AppContainer
import com.denham.skyline.ui.components.ArtworkImage
import com.denham.skyline.ui.theme.SkyPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val DP_PER_MINUTE = 4
// Show the user's whole lineup (numbered order), not a tiny slice. Programme
// lookups are chunked so we never blow SQLite's ~999 bound-variable limit.
private const val VISIBLE_CHANNELS = 2000
private const val PROGRAMME_QUERY_CHUNK = 900

data class GuideUiState(
    val days: List<Long> = emptyList(),        // start-of-day epoch ms, Today..+4
    val selectedDay: Long = 0L,
    val channels: List<ChannelEntity> = emptyList(),
    val programmes: Map<Int, List<EpgProgrammeEntity>> = emptyMap(),
    val loading: Boolean = true,
    val hasGuide: Boolean = true,
    val importMessage: String? = null,
    /** Live categories the guide can be filtered by: (categoryId, name). */
    val categories: List<Pair<String, String>> = emptyList(),
    val selectedCategoryId: String? = null,    // null = All
)

@UnstableApi
class GuideViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(GuideUiState())
    val state: StateFlow<GuideUiState> = _state

    init {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val days = (0..4).map { today + it * DAY_MS }
        _state.value = GuideUiState(days = days, selectedDay = today)

        // Observe categories continuously so the guide's category filter appears
        // even when the catalogue finishes syncing after the guide is opened.
        viewModelScope.launch {
            container.db.categoryDao()
                .observe(com.denham.skyline.data.db.ContentType.LIVE)
                .collect { cats ->
                    _state.value = _state.value.copy(
                        categories = cats.map { it.categoryId to it.name },
                    )
                }
        }
        viewModelScope.launch {
            container.guideRepository.refreshIfStale()
            loadDay(_state.value.selectedDay)
        }
        viewModelScope.launch {
            container.guideRepository.state.collect { importState ->
                when (importState) {
                    is GuideImportState.Done -> {
                        _state.value = _state.value.copy(importMessage = null)
                        loadDay(_state.value.selectedDay)
                    }
                    is GuideImportState.Failed ->
                        _state.value = _state.value.copy(
                            importMessage = importState.message, loading = false,
                        )
                    is GuideImportState.Running ->
                        _state.value = _state.value.copy(importMessage = "Updating guide…")
                    else -> Unit
                }
            }
        }
    }

    fun selectDay(dayStartMs: Long) {
        _state.value = _state.value.copy(selectedDay = dayStartMs, loading = true)
        viewModelScope.launch { loadDay(dayStartMs) }
    }

    /** Force a fresh EPG import (the Done collector reloads the day). */
    fun refreshGuide() {
        _state.value = _state.value.copy(importMessage = "Updating guide…")
        viewModelScope.launch { container.guideRepository.importXmltv() }
    }

    /** Filter the guide to a single channel category (null = show all). */
    fun selectCategory(categoryId: String?) {
        _state.value = _state.value.copy(selectedCategoryId = categoryId, loading = true)
        viewModelScope.launch { loadDay(_state.value.selectedDay) }
    }

    private suspend fun loadDay(dayStartMs: Long) {
        // Show the lineup in the provider's channel-number order (what the user
        // expects), not by internal stream id — otherwise only a random slice of
        // low-id channels showed, none of which carried EPG.
        val catId = _state.value.selectedCategoryId
        val channels = if (catId == null) {
            container.db.channelDao().pageByNumber(VISIBLE_CHANNELS, 0)
        } else {
            container.db.channelDao().pageByCategory(catId, VISIBLE_CHANNELS, 0)
        }
        val hasGuide = container.guideRepository.hasGuideData()
        val programmes = if (hasGuide && channels.isNotEmpty()) {
            // Chunk the id list so the IN(...) query stays under SQLite's limit.
            channels.map { it.streamId }
                .chunked(PROGRAMME_QUERY_CHUNK)
                .flatMap { ids ->
                    container.guideRepository.programmesFor(ids, dayStartMs, dayStartMs + DAY_MS)
                }
                .groupBy { it.channelStreamId }
        } else emptyMap()
        _state.value = _state.value.copy(
            channels = channels,
            programmes = programmes,
            loading = false,
            hasGuide = hasGuide,
        )
    }

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}

@UnstableApi
@Composable
fun GuideScreen(
    viewModel: GuideViewModel,
    onPlayChannel: (ChannelEntity) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val dayFormat = androidx.compose.runtime.remember { SimpleDateFormat("EEE d", Locale.getDefault()) }

    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.Canvas),
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        ) {
            Text(
                "TV Guide",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
            )
            androidx.compose.material3.IconButton(
                onClick = viewModel::refreshGuide,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                androidx.compose.material3.Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh guide",
                    tint = SkyPalette.TextSecondary,
                )
            }
            com.denham.skyline.ui.components.CastButton(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(32.dp),
            )
        }

        // Day chips.
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.days.size, key = { state.days[it] }) { i ->
                val day = state.days[i]
                FilterChip(
                    selected = state.selectedDay == day,
                    onClick = { viewModel.selectDay(day) },
                    label = { Text(if (i == 0) "Today" else dayFormat.format(Date(day))) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SkyPalette.Accent,
                        selectedLabelColor = SkyPalette.TextPrimary,
                        containerColor = SkyPalette.Surface,
                        labelColor = SkyPalette.TextSecondary,
                    ),
                )
            }
        }

        // Category filter chips (All + each live category).
        if (state.categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategoryId == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SkyPalette.Accent,
                            selectedLabelColor = SkyPalette.TextPrimary,
                            containerColor = SkyPalette.Surface,
                            labelColor = SkyPalette.TextSecondary,
                        ),
                    )
                }
                items(state.categories.size, key = { state.categories[it].first }) { i ->
                    val (id, name) = state.categories[i]
                    FilterChip(
                        selected = state.selectedCategoryId == id,
                        onClick = { viewModel.selectCategory(id) },
                        label = { Text(name, maxLines = 1) },
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
        Spacer(Modifier.height(8.dp))

        state.importMessage?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = SkyPalette.TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SkyPalette.Accent)
            }
            !state.hasGuide || state.programmes.isEmpty() ->
                NoGuideFallback(state, onPlayChannel)
            else -> GuideGrid(state, onPlayChannel)
        }
    }
}

/** The mock's grid: frozen channel column, shared horizontal time scroll. */
@Composable
private fun GuideGrid(
    state: GuideUiState,
    onPlayChannel: (ChannelEntity) -> Unit,
) {
    val timeScroll = rememberScrollState()
    val now = System.currentTimeMillis()
    val windowStart = state.selectedDay
    val windowEnd = state.selectedDay + 24 * 60 * 60 * 1000L
    val totalWidth = (24 * 60 * DP_PER_MINUTE).dp
    val hourFormat = androidx.compose.runtime.remember { SimpleDateFormat("h a", Locale.getDefault()) }

    // Auto-scroll to roughly "now" on today's page.
    val density = androidx.compose.ui.platform.LocalDensity.current
    LaunchedEffect(windowStart) {
        if (now in windowStart..windowEnd) {
            val minutes = ((now - windowStart) / 60_000L).toInt() - 30
            val px = with(density) { (minutes.coerceAtLeast(0) * DP_PER_MINUTE).dp.toPx() }
            timeScroll.scrollTo(px.toInt())
        }
    }

    Column {
        // Timeline header row.
        Row {
            Spacer(Modifier.width(CHANNEL_COL))
            Row(
                Modifier
                    .horizontalScroll(timeScroll)
                    .width(totalWidth),
            ) {
                repeat(24) { hour ->
                    Text(
                        hourFormat.format(Date(windowStart + hour * 3_600_000L)),
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyPalette.TextSecondary,
                        modifier = Modifier.width((60 * DP_PER_MINUTE).dp),
                    )
                }
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(state.channels.size, key = { state.channels[it].streamId }) { index ->
                val channel = state.channels[index]
                val programmes = state.programmes[channel.streamId].orEmpty()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 2.dp),
                ) {
                    // Frozen channel cell: logo, then name + number.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .width(CHANNEL_COL)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SkyPalette.Surface)
                            .clickable { onPlayChannel(channel) }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    ) {
                        ArtworkImage(
                            url = channel.icon,
                            contentDescription = channel.name,
                            fallbackIcon = Icons.Default.LiveTv,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(26.dp),
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 6.dp),
                        ) {
                            Text(
                                channel.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = SkyPalette.TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified,
                            )
                            Text(
                                "${channel.num}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkyPalette.TextMuted,
                            )
                        }
                    }
                    // Programme blocks on the shared scroll.
                    Box(
                        Modifier
                            .horizontalScroll(timeScroll)
                            .width(totalWidth)
                            .fillMaxSize(),
                    ) {
                        programmes.forEach { prog ->
                            val offsetMin = GuideMath.offsetMinutes(windowStart, prog.startMs)
                            val widthMin = GuideMath.widthMinutes(
                                prog.startMs, prog.stopMs, windowStart, windowEnd
                            )
                            if (widthMin <= 0) return@forEach
                            val isNow = now in prog.startMs until prog.stopMs
                            Box(
                                Modifier
                                    .offset(x = (offsetMin * DP_PER_MINUTE).dp)
                                    .width((widthMin * DP_PER_MINUTE).dp - 2.dp)
                                    .fillMaxSize()
                                    .padding(end = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isNow) SkyPalette.Accent.copy(alpha = 0.35f)
                                        else SkyPalette.SurfaceElevated.copy(alpha = 0.6f)
                                    )
                                    .clickable { onPlayChannel(channel) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    prog.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SkyPalette.TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        // Now line.
                        if (now in windowStart..windowEnd) {
                            val nowMin = ((now - windowStart) / 60_000L).toInt()
                            Box(
                                Modifier
                                    .offset(x = (nowMin * DP_PER_MINUTE).dp)
                                    .width(2.dp)
                                    .fillMaxSize()
                                    .background(SkyPalette.Accent)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Provider has no xmltv guide: keep the screen useful with channel rows. */
@Composable
private fun NoGuideFallback(
    state: GuideUiState,
    onPlayChannel: (ChannelEntity) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            "Full guide data isn't available from your provider. Showing channels instead.",
            style = MaterialTheme.typography.bodySmall,
            color = SkyPalette.TextSecondary,
            modifier = Modifier.padding(16.dp),
        )
        LazyColumn {
            items(state.channels.size, key = { state.channels[it].streamId }) { i ->
                val channel = state.channels[i]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayChannel(channel) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    ArtworkImage(
                        url = channel.icon,
                        contentDescription = channel.name,
                        fallbackIcon = Icons.Default.LiveTv,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        channel.name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

private val CHANNEL_COL = 108.dp
