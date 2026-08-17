package com.denham.skyline.tv

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denham.skyline.core.FixtureStatus
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.ui.components.ArtworkImage
import com.denham.skyline.ui.components.LiveBadge
import com.denham.skyline.ui.components.ProviderBadge
import com.denham.skyline.ui.theme.SkyPalette
import com.denham.skyline.ui.theme.SkyRadius
import com.denham.skyline.ui.theme.SkySpacing

/**
 * D-pad focus treatment for every interactive TV element: white outline +
 * slight scale, exactly like the mock's focused "top picks" tile.
 */
fun Modifier.tvClickable(
    shape: Shape = RoundedCornerShape(12.dp),
    onClick: () -> Unit,
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    // Sky-style focus "pop": a subtle scale + white outline. The scale is applied
    // via graphicsLayer (a draw-time transform), so — unlike a layout-affecting
    // scale — it never re-measures siblings or makes the row jump.
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.04f else 1f,
        animationSpec = tween(durationMillis = 140, easing = LinearOutSlowInEasing),
        label = "tvFocusPop",
    )
    this
        .onFocusChanged { focused = it.isFocused }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .border(
            width = if (focused) 3.dp else 0.dp,
            color = if (focused) Color.White else Color.Transparent,
            shape = shape,
        )
        .clip(shape)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
}

/** 16:9 landscape tile with bottom label, the TV rails' standard card. */
@Composable
fun TvLandscapeCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 220.dp,
    isChannelLogo: Boolean = true,
    /** Small channel-logo chip shown when the main image is programme artwork. */
    badgeLogoUrl: String? = null,
    /** Programme artwork URL (e.g. from XMLTV or resolved cache). Overrides channel logo. */
    programmeImageUrl: String? = null,
) {
    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(16f / 9f)
            .tvClickable(onClick = onClick)
            .background(
                Brush.linearGradient(
                    listOf(SkyPalette.Surface, SkyPalette.Indigo.copy(alpha = 0.5f))
                )
            ),
    ) {
        ArtworkImage(
            url = programmeImageUrl ?: imageUrl,
            contentDescription = title,
            fallbackIcon = if (isChannelLogo && programmeImageUrl == null) Icons.Default.LiveTv else Icons.Default.Movie,
            contentScale = if (isChannelLogo && programmeImageUrl == null) ContentScale.Fit else ContentScale.Crop,
            modifier = if (isChannelLogo && programmeImageUrl == null) {
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            } else {
                Modifier.fillMaxSize()
            },
        )
        if (programmeImageUrl != null || badgeLogoUrl != null) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                    .padding(4.dp),
            ) {
                ArtworkImage(
                    url = badgeLogoUrl ?: (if (programmeImageUrl != null) imageUrl else null),
                    contentDescription = null,
                    fallbackIcon = Icons.Default.LiveTv,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
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
    }
}

/** 2:3 poster tile for films/series on TV. */
@Composable
fun TvPosterCard(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 150.dp,
) {
    Column(modifier = modifier.width(width)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .tvClickable(onClick = onClick)
                .background(SkyPalette.Surface),
        ) {
            ArtworkImage(
                url = imageUrl,
                contentDescription = title,
                fallbackIcon = Icons.Default.Movie,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = SkyPalette.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Top navigation text tab, underlined when active (mock's header nav). */
@Composable
fun TvNavTab(label: String, active: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) SkyPalette.SurfaceElevated else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            ),
            color = if (active || focused) Color.White else SkyPalette.TextSecondary,
        )
        Box(
            Modifier
                .padding(top = 4.dp)
                .size(width = 34.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .then(
                    if (active) Modifier.background(SkyPalette.navUnderline)
                    else Modifier.background(Color.Transparent)
                )
        )
    }
}

// ---------------------------------------------------------------------------
// Football fixtures — TV sibling of ui/components/Components.kt's FixtureCard
// ---------------------------------------------------------------------------

/**
 * TV counterpart to the phone `FixtureCard`, not a modification of it — same
 * sibling relationship as `TvLandscapeCard`/`TvPosterCard` to their phone
 * equivalents. D-pad focusable via [tvClickable]; unlike phone (whole card
 * clickable only when exactly one channel matches, chips only when 2+),
 * every channel chip here is individually focusable regardless of count,
 * since a D-pad has no touch fallback to disambiguate an ambiguous tap.
 */
@Composable
fun TvFixtureCard(
    competition: String,
    homeTeam: String,
    awayTeam: String,
    homeCrestUrl: String?,
    awayCrestUrl: String?,
    status: FixtureStatus,
    channels: List<ChannelEntity>,
    onPlayChannel: (ChannelEntity) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = 240.dp,
    isSpotlight: Boolean = false,
) {
    val singleMatch = channels.singleOrNull()
    val sizedModifier = if (width != null) modifier.width(width) else modifier.fillMaxWidth()
    val shape = RoundedCornerShape(if (isSpotlight) SkyRadius.hero else SkyRadius.card)
    val cardModifier = if (singleMatch != null) {
        sizedModifier.tvClickable(shape) { onPlayChannel(singleMatch) }
    } else {
        sizedModifier.clip(shape)
    }
    val backgroundModifier = if (isSpotlight) {
        Modifier.background(
            Brush.linearGradient(
                listOf(SkyPalette.SurfaceElevated, SkyPalette.Indigo.copy(alpha = 0.55f))
            )
        )
    } else {
        Modifier.background(SkyPalette.Surface)
    }

    Column(cardModifier.then(backgroundModifier).padding(SkySpacing.m)) {
        ProviderBadge(competition)
        Spacer(Modifier.height(SkySpacing.s))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ArtworkImage(
                url = homeCrestUrl,
                contentDescription = homeTeam,
                fallbackIcon = Icons.Default.LiveTv,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)),
            )
            Text(
                homeTeam,
                style = MaterialTheme.typography.titleSmall,
                color = SkyPalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = SkySpacing.xs),
            )
            Text("v", style = MaterialTheme.typography.bodySmall, color = SkyPalette.TextMuted)
            Text(
                awayTeam,
                style = MaterialTheme.typography.titleSmall,
                color = SkyPalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f).padding(horizontal = SkySpacing.xs),
            )
            ArtworkImage(
                url = awayCrestUrl,
                contentDescription = awayTeam,
                fallbackIcon = Icons.Default.LiveTv,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)),
            )
        }

        Spacer(Modifier.height(SkySpacing.s))

        when (status) {
            is FixtureStatus.Scheduled -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SkySpacing.xs),
                ) {
                    Icon(
                        Icons.Default.Schedule, null,
                        tint = SkyPalette.TextMuted,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        status.kickoffLocal,
                        style = MaterialTheme.typography.labelMedium,
                        color = SkyPalette.TextSecondary,
                    )
                }
            }
            is FixtureStatus.Live -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SkySpacing.xs),
                ) {
                    LiveBadge()
                    Text(
                        "${status.homeScore}–${status.awayScore}",
                        style = MaterialTheme.typography.titleMedium,
                        color = SkyPalette.TextPrimary,
                    )
                    Text(
                        status.minute,
                        style = MaterialTheme.typography.labelMedium,
                        color = SkyPalette.TextSecondary,
                    )
                }
            }
            is FixtureStatus.Finished -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SkySpacing.xs),
                ) {
                    Text("FT", style = MaterialTheme.typography.labelMedium, color = SkyPalette.TextMuted)
                    Text(
                        "${status.homeScore}–${status.awayScore}",
                        style = MaterialTheme.typography.titleMedium,
                        color = SkyPalette.TextPrimary,
                    )
                }
            }
        }

        Spacer(Modifier.height(SkySpacing.s))

        if (channels.isEmpty()) {
            Text(
                "Not on your channels",
                style = MaterialTheme.typography.bodySmall,
                color = SkyPalette.TextMuted,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SkySpacing.xs),
                verticalArrangement = Arrangement.spacedBy(SkySpacing.xs),
            ) {
                channels.forEach { channel ->
                    TvFixtureChannelChip(channel = channel, onClick = { onPlayChannel(channel) })
                }
            }
        }
    }
}

/** D-pad-focusable channel chip, one per matched channel — every chip is
 *  independently focusable regardless of how many channels a fixture has,
 *  unlike phone's touch-only single/multi-channel tap split. */
@Composable
private fun TvFixtureChannelChip(
    channel: ChannelEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(SkyRadius.chip)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkySpacing.xs),
        modifier = modifier
            .tvClickable(shape, onClick)
            .border(1.dp, SkyPalette.Accent, shape)
            .padding(horizontal = SkySpacing.s, vertical = SkySpacing.xs),
    ) {
        Icon(
            Icons.Default.PlayArrow, null,
            tint = SkyPalette.Accent,
            modifier = Modifier.size(12.dp),
        )
        Text(
            channel.name,
            style = MaterialTheme.typography.labelSmall,
            color = SkyPalette.Accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
