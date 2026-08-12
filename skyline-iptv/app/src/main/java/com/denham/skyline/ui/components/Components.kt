package com.denham.skyline.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.denham.skyline.ui.theme.SkyPalette

/** Clickable with premium press feedback: scales to 0.97 while touched. */
fun Modifier.scaledClickable(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "pressScale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
}

/**
 * Sky-style horizontal rail: bold left-aligned title, snap-to-start cards.
 */
@Composable
fun <T> Rail(
    title: String,
    items: List<T>,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
    card: @Composable (T) -> Unit,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        val listState = rememberLazyListState()
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items.size, key = key?.let { k -> { i: Int -> k(items[i]) } }) { index ->
                card(items[index])
            }
        }
    }
}

/** 2:3 portrait poster (VOD/series convention). */
@Composable
fun PosterCard(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 120.dp,
) {
    Column(
        modifier = modifier
            .width(width)
            .clip(RoundedCornerShape(16.dp))
            .scaledClickable(onClick),
    ) {
        ArtworkImage(
            url = imageUrl,
            contentDescription = title,
            fallbackIcon = Icons.Default.Movie,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(16.dp)),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = SkyPalette.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
    }
}

/**
 * 16:9 live tile, Sky Glass style: indigo-tinted gradient, small channel
 * logo top-left, programme (now playing) named ON the tile over a scrim.
 */
@Composable
fun ChannelCard(
    name: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    width: androidx.compose.ui.unit.Dp = 172.dp,
) {
    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        SkyPalette.SurfaceElevated,
                        SkyPalette.Indigo.copy(alpha = 0.55f),
                    )
                )
            )
            .scaledClickable(onClick),
    ) {
        ArtworkImage(
            url = imageUrl,
            contentDescription = name,
            fallbackIcon = Icons.Default.LiveTv,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .size(38.dp)
                .clip(RoundedCornerShape(6.dp)),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = SkyPalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Brand mark (personal build, private distribution), extracted from the
 * canonical mocks: "sky go" lockup on phone, plain "sky" on TV.
 */
@Composable
fun SkylineWordmark(modifier: Modifier = Modifier, showGo: Boolean = true) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(
            if (showGo) com.denham.skyline.R.drawable.sky_go_logo
            else com.denham.skyline.R.drawable.sky_tv_logo
        ),
        contentDescription = if (showGo) "Sky Go" else "Sky",
        modifier = modifier.height(if (showGo) 28.dp else 34.dp),
    )
}

/** Section title with an optional accent "View all" action, per the mock. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onViewAll: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = SkyPalette.TextPrimary,
        )
        Spacer(Modifier.weight(1f))
        if (onViewAll != null) {
            Text(
                "View all",
                style = MaterialTheme.typography.labelMedium,
                color = SkyPalette.Accent,
                modifier = Modifier.clickable(onClick = onViewAll).padding(4.dp),
            )
        }
    }
}

/** Small dark rounded badge for provider/category names ("sky sports" style). */
@Composable
fun ProviderBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.lowercase(),
        style = MaterialTheme.typography.labelSmall,
        color = SkyPalette.TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SkyPalette.SurfaceElevated)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Red LIVE chip. */
@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    Text(
        text = "LIVE",
        style = MaterialTheme.typography.labelSmall,
        color = SkyPalette.TextPrimary,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SkyPalette.LiveRed)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Blue pill action button with a play glyph ("Resume" / "Watch Live"). */
@Composable
fun PillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(SkyPalette.Accent)
            .scaledClickable(onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Icon(
            Icons.Default.PlayArrow, null,
            tint = SkyPalette.TextPrimary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = SkyPalette.TextPrimary,
        )
    }
}

/** Compact "Live Now" row card: crest, badge + titles, LIVE chip. */
@Composable
fun LiveNowRow(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SkyPalette.Surface)
            .scaledClickable(onClick)
            .padding(10.dp),
    ) {
        ArtworkImage(
            url = imageUrl,
            contentDescription = title,
            fallbackIcon = Icons.Default.LiveTv,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp)),
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        LiveBadge()
    }
}

/** Coil image with shimmer placeholder and icon fallback. */
@Composable
fun ArtworkImage(
    url: String?,
    contentDescription: String?,
    fallbackIcon: ImageVector,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackUrl: String? = null,
) {
    // Choose the first non-blank source; on load error fall through to the
    // next one (e.g. hero backdrop -> poster) before the placeholder icon.
    val primary = url?.takeIf { it.isNotBlank() }
    val secondary = fallbackUrl?.takeIf { it.isNotBlank() && it != primary }
    if (primary == null && secondary == null) {
        Box(modifier.background(SkyPalette.SurfaceElevated), contentAlignment = Alignment.Center) {
            Icon(
                fallbackIcon, contentDescription = contentDescription,
                tint = SkyPalette.TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp),
            )
        }
        return
    }

    @Composable
    fun placeholder() {
        Box(
            Modifier.fillMaxSize().background(SkyPalette.SurfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                fallbackIcon, contentDescription = null,
                tint = SkyPalette.TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp),
            )
        }
    }

    SubcomposeAsyncImage(
        model = primary ?: secondary,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = { ShimmerBox(Modifier.fillMaxSize()) },
        error = {
            if (primary != null && secondary != null) {
                // Second attempt with the fallback URL.
                SubcomposeAsyncImage(
                    model = secondary,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                    loading = { ShimmerBox(Modifier.fillMaxSize()) },
                    error = { placeholder() },
                )
            } else {
                placeholder()
            }
        },
    )
}

/** Animated loading shimmer (skeleton) in the navy palette. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            SkyPalette.SurfaceElevated,
            SkyPalette.SurfaceElevated.copy(alpha = 0.4f),
            SkyPalette.SurfaceElevated,
        ),
        start = Offset(progress * 1200f - 600f, 0f),
        end = Offset(progress * 1200f, 200f),
    )
    Box(modifier.background(brush))
}

/** Full-width skeleton rail shown while content is syncing. */
@Composable
fun ShimmerRail(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        ShimmerBox(
            Modifier
                .padding(16.dp)
                .size(width = 140.dp, height = 20.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(3) {
                ShimmerBox(
                    Modifier
                        .size(width = 168.dp, height = 94.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
