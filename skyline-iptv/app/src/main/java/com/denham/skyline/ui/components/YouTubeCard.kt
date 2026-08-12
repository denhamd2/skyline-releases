package com.denham.skyline.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denham.skyline.ui.theme.SkyPalette
import com.denham.skyline.tv.tvClickable

@Composable
fun YouTubeCard(
    title: String,
    thumbnail: String,
    publishedDate: String,
    videoUrl: String,
    onPlayVideo: (videoId: String, title: String, thumbnailUrl: String?) -> Unit,
    modifier: Modifier = Modifier,
    thumbnailUrl: String? = null,
    isTv: Boolean = false,
) {
    val videoId = videoUrl.substringAfterLast("v=")
    val cardWidth = if (isTv) 240.dp else 160.dp
    val cardHeight = if (isTv) 135.dp else 90.dp

    Column(
        modifier = Modifier
            .width(cardWidth)
            .then(
                if (isTv) {
                    Modifier.tvClickable(
                        shape = RoundedCornerShape(8.dp),
                        onClick = { onPlayVideo(videoId, title, thumbnailUrl) }
                    )
                } else {
                    Modifier.clickable { onPlayVideo(videoId, title, thumbnailUrl) }
                }
            )
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(SkyPalette.SurfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            ArtworkImage(
                url = thumbnail,
                contentDescription = title,
                fallbackIcon = Icons.Default.PlayArrow,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop,
            )
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .padding(12.dp),
                tint = Color.White,
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = SkyPalette.TextPrimary,
        )
        Text(
            publishedDate,
            style = MaterialTheme.typography.labelSmall,
            color = SkyPalette.TextSecondary,
        )
    }
}
