package com.denham.skyline.ui.catchup

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.media3.common.util.UnstableApi
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.data.db.EpgProgrammeEntity
import com.denham.skyline.di.AppContainer
import com.denham.skyline.ui.components.ArtworkImage
import com.denham.skyline.ui.theme.SkyPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CatchUpState(
    val channels: List<ChannelEntity> = emptyList(),
    val selected: ChannelEntity? = null,
    val programmes: List<EpgProgrammeEntity> = emptyList(),
    val loading: Boolean = true,
)

/**
 * "Recordings" == the provider's catch-up / TV-archive window. Xtream has no
 * server DVR list, but archive-capable channels can replay past programmes via
 * a timeshift URL, which is what this screen surfaces.
 */
@UnstableApi
class CatchUpViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(CatchUpState())
    val state: StateFlow<CatchUpState> = _state

    init {
        viewModelScope.launch {
            container.guideRepository.refreshIfStale()
            val channels = container.db.channelDao().archiveChannels()
            _state.value = _state.value.copy(channels = channels, loading = false)
            channels.firstOrNull()?.let { select(it) }
        }
    }

    fun select(channel: ChannelEntity) {
        _state.value = _state.value.copy(selected = channel)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val windowMs = (channel.tvArchiveDuration.coerceAtLeast(1)) * 24 * 60 * 60 * 1000L
            val past = container.guideRepository
                .programmesFor(listOf(channel.streamId), now - windowMs, now)
                .filter { it.stopMs <= now }         // only finished programmes
                .sortedByDescending { it.startMs }
            _state.value = _state.value.copy(programmes = past)
        }
    }

    /** Build the timeshift URL for a past programme. */
    fun playbackUrl(channel: ChannelEntity, prog: EpgProgrammeEntity): String {
        val durationMin = ((prog.stopMs - prog.startMs) / 60_000L).toInt().coerceAtLeast(1)
        val start = TIMESHIFT_FORMAT.format(Date(prog.startMs))
        return container.urlBuilder().timeshift(channel.streamId, durationMin, start)
    }

    private companion object {
        // Xtream timeshift start token: YYYY-MM-DD:HH-MM
        val TIMESHIFT_FORMAT = SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US)
    }
}

@UnstableApi
@Composable
fun CatchUpScreen(
    viewModel: CatchUpViewModel,
    onBack: () -> Unit,
    onPlay: (url: String, title: String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val timeFormat = androidx.compose.runtime.remember {
        SimpleDateFormat("EEE d MMM, h:mma", Locale.getDefault())
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.Canvas),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Column {
                Text("Recordings", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Catch up TV from your provider's archive",
                    style = MaterialTheme.typography.labelMedium,
                    color = SkyPalette.TextSecondary,
                )
            }
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SkyPalette.Accent)
            }
            state.channels.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "None of your channels offer catch-up. If your provider supports " +
                        "recordings, they'll appear here once the guide has loaded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyPalette.TextSecondary,
                )
            }
            else -> {
                // Channel selector chips.
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp, vertical = 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.channels.size, key = { state.channels[it].streamId }) { i ->
                        val ch = state.channels[i]
                        FilterChip(
                            selected = state.selected?.streamId == ch.streamId,
                            onClick = { viewModel.select(ch) },
                            label = { Text(ch.name, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SkyPalette.Accent,
                                selectedLabelColor = SkyPalette.TextPrimary,
                                containerColor = SkyPalette.Surface,
                                labelColor = SkyPalette.TextSecondary,
                            ),
                        )
                    }
                }

                val channel = state.selected
                if (channel != null && state.programmes.isEmpty()) {
                    Text(
                        "No catch-up guide data for ${channel.name} yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyPalette.TextSecondary,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.programmes.size, key = { state.programmes[it].startMs }) { i ->
                        val prog = state.programmes[i]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    channel?.let {
                                        onPlay(viewModel.playbackUrl(it, prog), prog.title)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            ArtworkImage(
                                url = channel?.icon,
                                contentDescription = channel?.name,
                                fallbackIcon = Icons.Default.LiveTv,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                            )
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                            ) {
                                Text(
                                    prog.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    timeFormat.format(Date(prog.startMs)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyPalette.TextMuted,
                                )
                            }
                            Icon(Icons.Default.PlayArrow, "Play", tint = SkyPalette.TextSecondary)
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}
