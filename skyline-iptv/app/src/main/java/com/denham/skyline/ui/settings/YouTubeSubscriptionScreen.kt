package com.denham.skyline.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denham.skyline.data.repo.YouTubeSearchResult
import com.denham.skyline.ui.components.ArtworkImage
import com.denham.skyline.ui.theme.SkyPalette

/**
 * Per-family-member YouTube subscriptions: search for channels and playlists,
 * or paste a URL when no API key is configured (search needs one; the RSS
 * feeds that fetch videos do not).
 */
@Composable
fun YouTubeSubscriptionScreen(
    viewModel: YouTubeSubscriptionViewModel,
    onBack: () -> Unit,
) {
    val member by viewModel.selectedMember.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val query by viewModel.query.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val message by viewModel.message.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.Canvas),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = SkyPalette.TextPrimary)
            }
            Text(
                "YouTube channels",
                style = MaterialTheme.typography.headlineSmall,
                color = SkyPalette.TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            listOf("David", "Anne", "Ava", "Sophie").forEach { name ->
                FilterChip(
                    selected = member == name,
                    onClick = { viewModel.selectMember(name) },
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

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            label = {
                Text(
                    if (viewModel.searchEnabled) "Search channels or paste a link"
                    else "Paste a channel or playlist link",
                )
            },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = viewModel::submit) {
                    Icon(
                        if (viewModel.searchEnabled) Icons.Default.Search else Icons.Default.Add,
                        contentDescription = "Go",
                        tint = SkyPalette.Accent,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.submit() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        message?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = SkyPalette.TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (busy) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SkyPalette.Accent)
            }
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            if (results.isNotEmpty()) {
                item {
                    SubHeader("Results")
                }
                items(results.size, key = { "r_" + results[it].id }) { index ->
                    val result = results[index]
                    SubscriptionRow(
                        title = result.title,
                        subtitle = if (result.isPlaylist) "Playlist" else "Channel",
                        imageUrl = result.thumbnailUrl,
                        actionIcon = Icons.Default.Add,
                        actionLabel = "Subscribe",
                        onAction = { viewModel.subscribe(result) },
                    )
                }
            }

            item {
                SubHeader(
                    if (subscriptions.isEmpty()) "No subscriptions for ${member.orEmpty()}"
                    else "${member.orEmpty()}'s subscriptions",
                )
            }
            items(subscriptions.size, key = { "s_" + subscriptions[it].id }) { index ->
                val sub = subscriptions[index]
                SubscriptionRow(
                    title = sub.title,
                    subtitle = if (sub.isPlaylist) "Playlist" else "Channel",
                    imageUrl = sub.thumbnailUrl,
                    actionIcon = Icons.Default.Delete,
                    actionLabel = "Remove",
                    onAction = { viewModel.unsubscribe(sub.id) },
                )
            }
        }
    }
}

@Composable
private fun SubHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = SkyPalette.TextPrimary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SubscriptionRow(
    title: String,
    subtitle: String,
    imageUrl: String?,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        ArtworkImage(
            url = imageUrl,
            contentDescription = title,
            fallbackIcon = Icons.Default.PlayArrow,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = SkyPalette.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = SkyPalette.TextSecondary,
            )
        }
        IconButton(onClick = onAction) {
            Icon(actionIcon, actionLabel, tint = SkyPalette.Accent)
        }
    }
}
