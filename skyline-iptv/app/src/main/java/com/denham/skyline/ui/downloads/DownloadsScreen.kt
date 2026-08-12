package com.denham.skyline.ui.downloads

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.denham.skyline.data.db.DownloadMetaEntity
import com.denham.skyline.di.AppContainer
import com.denham.skyline.ui.components.ArtworkImage
import com.denham.skyline.ui.components.scaledClickable
import com.denham.skyline.ui.theme.SkyPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DownloadRow(
    val meta: DownloadMetaEntity,
    val state: Int,
    val percent: Float,
    val bytes: Long,
)

@UnstableApi
class DownloadsViewModel(private val container: AppContainer) : ViewModel() {

    val settings = container.settingsStore.current

    private val _rows = MutableStateFlow<List<DownloadRow>>(emptyList())
    val rows: StateFlow<List<DownloadRow>> = _rows

    private val _storageUsed = MutableStateFlow(0L)
    val storageUsed: StateFlow<Long> = _storageUsed

    val metaFlow = container.db.downloadMetaDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Poll while visible: DownloadManager progress isn't observable as Flow.
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(1000)
            }
        }
    }

    private suspend fun refresh() {
        val metas = metaFlow.value.associateBy { it.contentId }
        val downloads = container.downloadsCenter.currentDownloads()
        _rows.value = downloads.mapNotNull { dl ->
            val meta = metas[dl.request.id]
                ?: DownloadMetaEntity(dl.request.id, dl.request.uri.lastPathSegment ?: "Download", null, null, 0)
            DownloadRow(
                meta = meta,
                state = dl.state,
                percent = dl.percentDownloaded.coerceIn(0f, 100f),
                bytes = dl.bytesDownloaded,
            )
        }
        _storageUsed.value = container.downloadsCenter.totalDownloadedBytes()
    }

    fun delete(contentId: String) {
        container.downloadsCenter.removeDownload(contentId)
        viewModelScope.launch { container.db.downloadMetaDao().delete(contentId) }
    }

    fun setSmartDownloads(value: Boolean) {
        viewModelScope.launch { container.settingsStore.setAutoDeleteWatched(value) }
    }
}

@UnstableApi
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    onBack: () -> Unit,
    onPlay: (url: String, title: String) -> Unit,
) {
    val rows by viewModel.rows.collectAsState()
    val storage by viewModel.storageUsed.collectAsState()
    val settings by viewModel.settings.collectAsState()
    // keep meta flow hot so refresh() sees titles
    viewModel.metaFlow.collectAsState()

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
            Text("Downloads", style = MaterialTheme.typography.headlineMedium)
        }

        // Smart Downloads / storage card, per the mock.
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SkyPalette.Surface)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Smart Downloads", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "We'll automatically delete downloads you've finished watching.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SkyPalette.TextSecondary,
                    )
                }
                Switch(
                    checked = settings.autoDeleteWatched,
                    onCheckedChange = viewModel::setSmartDownloads,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Storage used: ${formatBytes(storage)}",
                style = MaterialTheme.typography.labelMedium,
                color = SkyPalette.TextSecondary,
            )
        }
        Spacer(Modifier.height(8.dp))

        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Nothing downloaded yet.\nUse the download button on any film or episode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyPalette.TextSecondary,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(rows.size, key = { rows[it].meta.contentId }) { i ->
                    val row = rows[i]
                    DownloadRowItem(
                        row = row,
                        onClick = {
                            if (row.state == Download.STATE_COMPLETED) {
                                onPlay(row.meta.contentId, row.meta.title)
                            }
                        },
                        onDelete = { viewModel.delete(row.meta.contentId) },
                    )
                }
                item {
                    Text(
                        "Downloads play offline on this phone only.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyPalette.TextMuted,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadRowItem(
    row: DownloadRow,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .scaledClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        ArtworkImage(
            url = row.meta.imageUrl,
            contentDescription = row.meta.title,
            fallbackIcon = Icons.Default.Movie,
            modifier = Modifier
                .width(64.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                row.meta.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            row.meta.subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyPalette.TextSecondary,
                )
            }
            Text(
                formatBytes(row.bytes),
                style = MaterialTheme.typography.labelSmall,
                color = SkyPalette.TextMuted,
            )
            if (row.state == Download.STATE_DOWNLOADING) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { row.percent / 100f },
                    color = SkyPalette.Accent,
                    trackColor = SkyPalette.SurfaceElevated,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        when (row.state) {
            Download.STATE_COMPLETED -> Icon(
                Icons.Default.CheckCircle, "Downloaded",
                tint = SkyPalette.Accent, modifier = Modifier.size(24.dp),
            )
            Download.STATE_DOWNLOADING -> CircularProgressIndicator(
                progress = { row.percent / 100f },
                color = SkyPalette.Accent,
                trackColor = SkyPalette.SurfaceElevated,
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp,
            )
            else -> Unit
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, "Delete", tint = SkyPalette.TextSecondary)
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.0f MB".format(bytes / 1_000_000.0)
    bytes > 0 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "0 MB"
}
