package com.denham.skyline.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.paging.compose.collectAsLazyPagingItems
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.data.db.ContentType
import com.denham.skyline.data.db.MovieEntity
import com.denham.skyline.data.db.SeriesEntity
import com.denham.skyline.di.AppContainer
import com.denham.skyline.ui.browse.MoviesViewModel
import com.denham.skyline.ui.browse.SeriesViewModel
import com.denham.skyline.ui.components.ArtworkImage
import com.denham.skyline.ui.components.LiveBadge
import com.denham.skyline.ui.components.ShimmerBox
import com.denham.skyline.ui.components.SkylineWordmark
import com.denham.skyline.ui.components.YouTubeCard
import com.denham.skyline.ui.components.enterReveal
import com.denham.skyline.ui.components.revealDelay
import com.denham.skyline.ui.guide.GuideViewModel
import com.denham.skyline.ui.home.FootballSectionState
import com.denham.skyline.ui.home.HomeViewModel
import com.denham.skyline.ui.theme.SkyPalette
import com.denham.skyline.ui.theme.SkyRadius
import com.denham.skyline.ui.theme.SkySpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SPORT_WORDS = com.denham.skyline.core.CategoryKeywords.SPORT
private val KID_WORDS = com.denham.skyline.core.CategoryKeywords.KIDS

/**
 * Rolling release the CI pipeline refreshes on every change (same as phone).
 * Points at the public releases repo: the source repo is private, so this
 * download would fail on a TV, where the browser isn't signed in to GitHub.
 */
private const val TV_UPDATE_URL =
    "https://github.com/denhamd2/skyline-releases/releases/download/skyline-latest/Skyline.apk"

// ---------------------------------------------------------------------------
// Home
// ---------------------------------------------------------------------------

@UnstableApi
@Composable
fun TvHomeScreen(
    viewModel: HomeViewModel,
    onPlayChannel: (ChannelEntity) -> Unit,
    onPlayMovie: (MovieEntity) -> Unit,
    onResume: (com.denham.skyline.data.prefs.LastPlayed) -> Unit = {},
    onPlayYoutube: (videoId: String, title: String, thumbnailUrl: String?) -> Unit = { _, _, _ -> },
    onViewAllFixtures: () -> Unit = {},
) {
    val movies by viewModel.recentMovies.collectAsState()
    val popular by viewModel.popularChannels.collectAsState()
    val favorites by viewModel.favoriteChannels.collectAsState()
    val heroBackdrop by viewModel.heroBackdrop.collectAsState()
    val epg by viewModel.epg.collectAsState()
    val nowImages by viewModel.nowProgrammeImages.collectAsState()
    val resolvedImages by viewModel.resolvedProgrammeImages.collectAsState()
    val lastPlayed by viewModel.continueWatching.collectAsState()
    val categoryRails by viewModel.categoryRails.collectAsState()
    val youtubeVideos by viewModel.youtubeVideos.collectAsState()
    val selectedMember by viewModel.selectedFamilyMember.collectAsState()
    val footballSection by viewModel.footballSection.collectAsState()
    val fixtureChannels by viewModel.fixtureChannels.collectAsState()
    val hero = movies.firstOrNull()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.tvBackground),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(340.dp),
            ) {
                if (heroBackdrop != null || hero?.icon != null) {
                    ArtworkImage(
                        url = heroBackdrop ?: hero?.icon,
                        contentDescription = hero?.name,
                        fallbackIcon = Icons.Default.LiveTv,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(SkyPalette.heroFallback))
                }
                Box(Modifier.fillMaxSize().background(SkyPalette.heroScrim))
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 48.dp, vertical = 20.dp),
                ) {
                    Text(
                        hero?.name ?: "Welcome to Sky Go",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(10.dp))
                    if (hero != null) {
                        // Mock uses a white pill with dark text.
                        Box(
                            Modifier
                                .tvClickable(RoundedCornerShape(24.dp)) { onPlayMovie(hero) }
                                .background(Color.White)
                                .padding(horizontal = 26.dp, vertical = 12.dp),
                        ) {
                            Text(
                                "Watch now",
                                style = MaterialTheme.typography.titleMedium,
                                color = SkyPalette.Canvas,
                            )
                        }
                    }
                }
            }
        }

        item {
            TvRailHeader("Today's top picks")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(popular.size, key = { popular[it].streamId }) { i ->
                    val ch = popular[i]
                    val art = nowImages[ch.streamId]
                    val resolved = resolvedImages[ch.streamId]
                    TvLandscapeCard(
                        title = epg[ch.streamId]?.nowTitle ?: ch.name,
                        subtitle = ch.name,
                        imageUrl = ch.icon,
                        isChannelLogo = true,
                        programmeImageUrl = art ?: resolved,
                        badgeLogoUrl = if ((art ?: resolved) != null) ch.icon else null,
                        onClick = { onPlayChannel(ch) },
                    )
                }
            }
        }

        lastPlayed?.let { last ->
            item {
                TvRailHeader("Continue watching")
                Row(Modifier.padding(horizontal = 48.dp)) {
                    TvLandscapeCard(
                        title = last.title,
                        subtitle = if (last.type == "live") "Live TV" else "Resume",
                        imageUrl = last.imageUrl,
                        isChannelLogo = last.type == "live",
                        onClick = { onResume(last) },
                    )
                }
            }
        }

        if (favorites.isNotEmpty()) {
            item {
                TvRailHeader("My list")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(favorites.size, key = { favorites[it].streamId }) { i ->
                        val ch = favorites[i]
                        val art = nowImages[ch.streamId]
                        val resolved = resolvedImages[ch.streamId]
                        TvLandscapeCard(
                            title = epg[ch.streamId]?.nowTitle ?: ch.name,
                            subtitle = ch.name,
                            imageUrl = ch.icon,
                            isChannelLogo = true,
                            programmeImageUrl = art ?: resolved,
                            badgeLogoUrl = if ((art ?: resolved) != null) ch.icon else null,
                            onClick = { onPlayChannel(ch) },
                        )
                    }
                }
            }
        }

        item {
            TvRailHeader("New films")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(movies.size, key = { movies[it].streamId }) { i ->
                    val movie = movies[i]
                    TvPosterCard(movie.name, movie.icon, onClick = { onPlayMovie(movie) })
                }
            }
        }

        // Genre rails — one row per live category (Sky-style, like the mock's
        // Trending / Sports / Entertainment / Cinema rows).
        items(categoryRails.size, key = { categoryRails[it].first }) { i ->
            val (name, channels) = categoryRails[i]
            Column(Modifier.enterReveal(revealDelay(i))) {
                TvRailHeader(name)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(channels.size, key = { channels[it].streamId }) { j ->
                        val ch = channels[j]
                        val art = nowImages[ch.streamId]
                        val resolved = resolvedImages[ch.streamId]
                        TvLandscapeCard(
                            title = epg[ch.streamId]?.nowTitle ?: ch.name,
                            subtitle = ch.name,
                            imageUrl = ch.icon,
                            isChannelLogo = true,
                            programmeImageUrl = art ?: resolved,
                            badgeLogoUrl = if ((art ?: resolved) != null) ch.icon else null,
                            onClick = { onPlayChannel(ch) },
                        )
                    }
                }
            }
        }

        // David-only "Football" section: mirrors HomeScreen.kt's placement
        // and gating exactly (selectedMember == "David"), reusing the same
        // footballSection/fixtureChannels state -- ahead of YouTube for the
        // same "time-decaying, happening today" reasoning phone uses.
        val showFootball = selectedMember == "David" && when (val football = footballSection) {
            FootballSectionState.Hidden -> false
            FootballSectionState.Loading -> true
            is FootballSectionState.Loaded ->
                football.manUtdNext != null || football.roundFixtures.isNotEmpty()
        }
        if (showFootball) {
            item {
                Column(Modifier.enterReveal(revealDelay(categoryRails.size))) {
                    TvRailHeader("Football")
                    when (val football = footballSection) {
                        is FootballSectionState.Loading -> {
                            ShimmerBox(
                                Modifier
                                    .padding(horizontal = 48.dp)
                                    .width(320.dp)
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(SkyRadius.card)),
                            )
                        }
                        is FootballSectionState.Loaded -> {
                            football.manUtdNext?.let { fixture ->
                                Text(
                                    "Man Utd next",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SkyPalette.TextSecondary,
                                    modifier = Modifier.padding(horizontal = 48.dp, vertical = SkySpacing.s),
                                )
                                Row(Modifier.padding(horizontal = 48.dp)) {
                                    TvFixtureCard(
                                        competition = fixture.competition,
                                        homeTeam = fixture.homeTeam,
                                        awayTeam = fixture.awayTeam,
                                        homeCrestUrl = fixture.homeCrestUrl,
                                        awayCrestUrl = fixture.awayCrestUrl,
                                        status = fixture.status,
                                        channels = fixtureChannels[fixture.id] ?: emptyList(),
                                        onPlayChannel = onPlayChannel,
                                        width = 320.dp,
                                        isSpotlight = true,
                                    )
                                }
                            }
                            if (football.roundFixtures.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 48.dp, vertical = SkySpacing.s),
                                ) {
                                    Text(
                                        "This round",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SkyPalette.TextSecondary,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Box(Modifier.tvClickable(onClick = onViewAllFixtures).padding(6.dp)) {
                                        Text(
                                            "View all",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = SkyPalette.Accent,
                                        )
                                    }
                                }
                                // Vertical, matching phone Home's fixtures
                                // list -- a round's fixture cards are
                                // information-dense (crests, status, channel
                                // chips), the same reasoning that moved
                                // phone's Home rail from a horizontal
                                // carousel to a vertical list applies here.
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 48.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(SkySpacing.m),
                                ) {
                                    football.roundFixtures.forEach { fixture ->
                                        key(fixture.id) {
                                            TvFixtureCard(
                                                competition = fixture.competition,
                                                homeTeam = fixture.homeTeam,
                                                awayTeam = fixture.awayTeam,
                                                homeCrestUrl = fixture.homeCrestUrl,
                                                awayCrestUrl = fixture.awayCrestUrl,
                                                status = fixture.status,
                                                channels = fixtureChannels[fixture.id] ?: emptyList(),
                                                onPlayChannel = onPlayChannel,
                                                width = null,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        FootballSectionState.Hidden -> Unit
                    }
                }
            }
        }

        // YouTube carousel for selected family member (TV mode)
        if (youtubeVideos.isNotEmpty() && selectedMember != null) {
            item {
                Column(Modifier.enterReveal(revealDelay(categoryRails.size + 1))) {
                    TvRailHeader("YouTube for $selectedMember")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(youtubeVideos.size, key = { youtubeVideos[it].id }) { index ->
                            val video = youtubeVideos[index]
                            YouTubeCard(
                                title = video.title,
                                thumbnail = video.thumbnail,
                                publishedDate = video.publishedAt,
                                videoUrl = video.videoUrl,
                                thumbnailUrl = video.thumbnail,
                                onPlayVideo = { videoId, title, thumbnailUrl ->
                                    onPlayYoutube(videoId, title, thumbnailUrl)
                                },
                                isTv = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TvRailHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(horizontal = 48.dp, vertical = 14.dp),
    )
}

/** Focusable pill used by the TV guide's category filter. */
@Composable
private fun TvGuideChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .tvClickable(RoundedCornerShape(20.dp), onClick)
            .background(if (selected) SkyPalette.Accent else SkyPalette.Surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else SkyPalette.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------
// TV Guide — fixed 2h window, mock's aligned-slot look
// ---------------------------------------------------------------------------

@UnstableApi
@Composable
fun TvGuideScreen(
    viewModel: GuideViewModel,
    onPlayChannel: (ChannelEntity) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val now = System.currentTimeMillis()
    // The visible 2h window can be shifted earlier/later with D-pad buttons,
    // since the grid itself isn't horizontally scrollable on a TV.
    var windowOffset by remember { mutableStateOf(0L) }
    val base = now - (now % (30 * 60_000L)) // floor to half hour
    val windowStart = base + windowOffset
    val windowEnd = windowStart + 2 * 60 * 60_000L
    val timeFormat = remember { SimpleDateFormat("h:mma", Locale.getDefault()) }
    val twoHours = 2 * 60 * 60_000L
    // Only the selected day's programmes are loaded, so keep the window inside it.
    val dayStart = state.selectedDay
    val minOffset = dayStart - base
    val maxOffset = (dayStart + 22 * 60 * 60_000L) - base

    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.tvBackground)
            .padding(horizontal = 48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 16.dp),
        ) {
            Text(
                "TV guide",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f),
            )
            // -12h back to +4 days ahead, matching the guide's retention window.
            Button(
                onClick = { windowOffset = (windowOffset - twoHours).coerceAtLeast(minOffset) },
                modifier = Modifier.padding(end = 10.dp),
            ) { Text("◀ Earlier") }
            Button(
                onClick = { windowOffset = 0L },
                modifier = Modifier.padding(end = 10.dp),
            ) { Text("Now") }
            Button(
                onClick = { windowOffset = (windowOffset + twoHours).coerceAtMost(maxOffset) },
            ) { Text("Later ▶") }
        }
        // Channel-category filter (All + each live category).
        if (state.categories.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                item {
                    TvGuideChip("All", state.selectedCategoryId == null) {
                        viewModel.selectCategory(null)
                    }
                }
                items(state.categories.size, key = { state.categories[it].first }) { i ->
                    val (id, name) = state.categories[i]
                    TvGuideChip(name, state.selectedCategoryId == id) {
                        viewModel.selectCategory(id)
                    }
                }
            }
        }
        // Time slot header.
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(150.dp))
            repeat(4) { slot ->
                Text(
                    timeFormat.format(Date(windowStart + slot * 30 * 60_000L)).lowercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = SkyPalette.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        state.importMessage?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = SkyPalette.TextSecondary)
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val chanColW = 150.dp                     // channel cell (146) + spacer (4)
            val areaWidth = maxWidth - chanColW
            val dpPerMin = areaWidth / 120f           // 2-hour window
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
                items(state.channels.size, key = { state.channels[it].streamId }) { index ->
                    val channel = state.channels[index]
                    val programmes = state.programmes[channel.streamId].orEmpty()
                        .filter { it.stopMs > windowStart && it.startMs < windowEnd }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(vertical = 2.dp),
                    ) {
                        // Channel cell: logo + number.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .width(146.dp)
                                .fillMaxHeight()
                                .tvClickable(RoundedCornerShape(8.dp)) { onPlayChannel(channel) }
                                .background(SkyPalette.Surface)
                                .padding(horizontal = 8.dp),
                        ) {
                            ArtworkImage(
                                url = channel.icon,
                                contentDescription = channel.name,
                                fallbackIcon = Icons.Default.LiveTv,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(30.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${channel.num}",
                                style = MaterialTheme.typography.labelMedium,
                                color = SkyPalette.TextMuted,
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        // Programme cells positioned by real time so they line up
                        // with the 2:30/3:00/3:30/4:00 header columns.
                        Box(Modifier.width(areaWidth).fillMaxHeight()) {
                            if (programmes.isEmpty()) {
                                Text(
                                    "No guide data",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SkyPalette.TextMuted,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 10.dp),
                                )
                            } else {
                                programmes.forEach { prog ->
                                    val startMin =
                                        ((prog.startMs - windowStart) / 60_000f).coerceAtLeast(0f)
                                    val endMin =
                                        ((prog.stopMs - windowStart) / 60_000f).coerceAtMost(120f)
                                    val wMin = endMin - startMin
                                    if (wMin <= 0f) return@forEach
                                    val isNow = now in prog.startMs until prog.stopMs
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .offset(x = dpPerMin * startMin)
                                            .width((dpPerMin * wMin - 4.dp).coerceAtLeast(0.dp))
                                            .fillMaxHeight()
                                            .tvClickable(RoundedCornerShape(8.dp)) {
                                                onPlayChannel(channel)
                                            }
                                            .background(
                                                if (isNow) SkyPalette.Accent
                                                else SkyPalette.SurfaceElevated.copy(alpha = 0.7f)
                                            )
                                            .padding(horizontal = 10.dp),
                                    ) {
                                        Text(
                                            prog.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        if (isNow) {
                                            Spacer(Modifier.width(8.dp))
                                            LiveBadge()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Blue "now" marker down the grid (mock's now-line).
            if (now in windowStart..windowEnd) {
                val nowMin = (now - windowStart) / 60_000f
                Box(
                    Modifier
                        .offset(x = chanColW + dpPerMin * nowMin)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(SkyPalette.Accent),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Browse — big category tiles
// ---------------------------------------------------------------------------

data class TvBrowseTile(val label: String, val kind: String, val icon: ImageVector)

@Composable
fun TvBrowseScreen(onOpen: (TvBrowseTile) -> Unit) {
    val tiles = listOf(
        TvBrowseTile("Films", "films", Icons.Default.Movie),
        TvBrowseTile("TV shows", "series", Icons.Default.Tv),
        TvBrowseTile("Sports", "sports", Icons.Default.SportsSoccer),
        TvBrowseTile("Kids", "kids", Icons.Default.ChildFriendly),
        TvBrowseTile("Live TV", "live", Icons.Default.LiveTv),
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.tvBackground)
            .padding(horizontal = 48.dp),
    ) {
        Text(
            "Browse",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(vertical = 20.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(tiles.size, key = { tiles[it].kind }) { i ->
                val tile = tiles[i]
                Box(
                    Modifier
                        .width(210.dp)
                        .height(260.dp)
                        .tvClickable(RoundedCornerShape(14.dp)) { onOpen(tile) }
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    SkyPalette.SurfaceElevated,
                                    SkyPalette.Accent.copy(alpha = 0.7f),
                                )
                            )
                        ),
                ) {
                    Icon(
                        tile.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp),
                    )
                    // Bottom scrim keeps the label readable over the gradient.
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                )
                            )
                            .padding(16.dp),
                    ) {
                        Text(
                            tile.label,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Films / Series grids (paged)
// ---------------------------------------------------------------------------

@UnstableApi
@Composable
fun TvFilmsScreen(viewModel: MoviesViewModel, onPlayMovie: (MovieEntity) -> Unit) {
    val movies = viewModel.movies.collectAsLazyPagingItems()
    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.tvBackground)
            .padding(horizontal = 48.dp),
    ) {
        Text(
            "Films",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 30.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(movies.itemCount, key = { i -> movies[i]?.streamId ?: -i }) { index ->
                val movie = movies[index] ?: return@items
                TvPosterCard(movie.name, movie.icon, onClick = { onPlayMovie(movie) })
            }
        }
    }
}

@UnstableApi
@Composable
fun TvSeriesScreen(viewModel: SeriesViewModel, onOpenSeries: (SeriesEntity) -> Unit) {
    val series = viewModel.series.collectAsLazyPagingItems()
    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.tvBackground)
            .padding(horizontal = 48.dp),
    ) {
        Text(
            "TV shows",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 30.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(series.itemCount, key = { i -> series[i]?.seriesId ?: -i }) { index ->
                val show = series[index] ?: return@items
                TvPosterCard(show.name, show.cover, onClick = { onOpenSeries(show) })
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sports hub
// ---------------------------------------------------------------------------

@UnstableApi
@Composable
fun TvSportsScreen(
    container: AppContainer,
    onPlayChannel: (ChannelEntity) -> Unit,
) {
    val categories by container.db.categoryDao().observe(ContentType.LIVE)
        .collectAsState(initial = emptyList())
    val sportCategories = categories.filter { cat ->
        SPORT_WORDS.any { cat.name.contains(it, ignoreCase = true) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.tvBackground)
            .padding(horizontal = 48.dp),
    ) {
        Text(
            "Sports",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        if (sportCategories.isEmpty()) {
            Text(
                "No sports categories found in your subscription.",
                style = MaterialTheme.typography.bodyMedium,
                color = SkyPalette.TextSecondary,
            )
            return@Column
        }
        // One rail per sports category from the subscription (Sky Sports, BT/TNT,
        // Premier Sports, La Liga, etc.) — not just the first one.
        LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
            items(sportCategories.size, key = { sportCategories[it].categoryId }) { i ->
                val cat = sportCategories[i]
                val channels by container.db.channelDao()
                    .observeByCategory(cat.categoryId, 20)
                    .collectAsState(initial = emptyList())
                Column {
                    TvRailHeader(cat.name)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(channels.size, key = { channels[it].streamId }) { j ->
                            val ch = channels[j]
                            TvLandscapeCard(
                                title = ch.name,
                                subtitle = cat.name,
                                imageUrl = ch.icon,
                                onClick = { onPlayChannel(ch) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Kids — kid categories' channels + films
// ---------------------------------------------------------------------------

@UnstableApi
@Composable
fun TvKidsScreen(
    container: AppContainer,
    onPlayChannel: (ChannelEntity) -> Unit,
) {
    val categories by container.db.categoryDao().observe(ContentType.LIVE)
        .collectAsState(initial = emptyList())
    val kidCategories = categories.filter { cat ->
        KID_WORDS.any { cat.name.contains(it, ignoreCase = true) }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.tvBackground)
            .padding(horizontal = 48.dp),
    ) {
        Text(
            "Kids",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        if (kidCategories.isEmpty()) {
            Text(
                "No kids categories found in your subscription.",
                style = MaterialTheme.typography.bodyMedium,
                color = SkyPalette.TextSecondary,
            )
        }
        LazyColumn {
            items(kidCategories.size, key = { kidCategories[it].categoryId }) { i ->
                val cat = kidCategories[i]
                val channels by container.db.channelDao()
                    .observeByCategory(cat.categoryId, 15)
                    .collectAsState(initial = emptyList())
                Column {
                    TvRailHeader(cat.name)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(channels.size, key = { channels[it].streamId }) { j ->
                            val ch = channels[j]
                            TvLandscapeCard(
                                title = ch.name,
                                subtitle = null,
                                imageUrl = ch.icon,
                                onClick = { onPlayChannel(ch) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// My Sky — settings tiles + sign out
// ---------------------------------------------------------------------------

@UnstableApi
@Composable
fun TvMySkyScreen(
    container: AppContainer,
    onSignOut: () -> Unit,
    onRefreshLibrary: () -> Unit,
) {
    val account = container.account.collectAsState().value
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.tvBackground)
            .padding(horizontal = 48.dp),
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(vertical = 20.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TvSettingTile("Account", account?.username ?: "") {}
                TvSettingTile(
                    "Update Skyline",
                    "Download & install the newest build",
                ) {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(TV_UPDATE_URL),
                            )
                        )
                    }
                }
                TvSettingTile("Refresh channels & library", "Re-sync from your provider", onRefreshLibrary)
                TvSettingTile("Sign out", "Remove this account from the TV", onSignOut)
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                SkylineWordmark()
                Spacer(Modifier.height(8.dp))
                Text(
                    "Sky Go for TV",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyPalette.TextSecondary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Version ${com.denham.skyline.BuildConfig.VERSION_NAME} " +
                        "(${com.denham.skyline.BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyPalette.TextSecondary,
                )
                Text(
                    "Built ${com.denham.skyline.BuildConfig.BUILD_TIME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyPalette.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun TvSettingTile(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .tvClickable(RoundedCornerShape(12.dp), onClick)
            .background(SkyPalette.Surface)
            .padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SkyPalette.TextSecondary,
            )
        }
    }
}
