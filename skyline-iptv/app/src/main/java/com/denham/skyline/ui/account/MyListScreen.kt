package com.denham.skyline.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.data.db.MovieEntity
import com.denham.skyline.di.AppContainer
import com.denham.skyline.ui.components.ChannelCard
import com.denham.skyline.ui.components.PosterCard
import com.denham.skyline.ui.components.Rail
import com.denham.skyline.ui.theme.SkyPalette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@UnstableApi
class MyListViewModel(container: AppContainer) : ViewModel() {
    val channels = container.db.channelDao().observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val movies = container.db.movieDao().observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@UnstableApi
@Composable
fun MyListScreen(
    viewModel: MyListViewModel,
    onBack: () -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit,
    onOpenMovie: (MovieEntity) -> Unit,
) {
    val channels by viewModel.channels.collectAsState()
    val movies by viewModel.movies.collectAsState()

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
            Text("My List", style = MaterialTheme.typography.headlineMedium)
        }
        LazyColumn {
            item {
                Rail("Channels", channels, key = { it.streamId }) { ch ->
                    ChannelCard(ch.name, ch.icon, onClick = { onPlayChannel(ch) })
                }
            }
            item {
                Rail("Films", movies, key = { it.streamId }) { m ->
                    PosterCard(m.name, m.icon, onClick = { onOpenMovie(m) })
                }
            }
            item {
                if (channels.isEmpty() && movies.isEmpty()) {
                    Text(
                        "Nothing saved yet. Tap the star on any channel or the heart on a film.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyPalette.TextSecondary,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
