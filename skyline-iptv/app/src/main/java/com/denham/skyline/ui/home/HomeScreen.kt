package com.denham.skyline.ui.home

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.denham.skyline.core.Fixture
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.data.db.ContentType
import com.denham.skyline.data.db.EpgNowNextEntity
import com.denham.skyline.data.db.MovieEntity
import com.denham.skyline.data.db.ProgrammeImageEntity
import com.denham.skyline.data.db.SeriesEntity
import com.denham.skyline.data.repo.SyncState
import com.denham.skyline.di.AppContainer
import com.denham.skyline.data.prefs.LastPlayed
import com.denham.skyline.BuildConfig
import com.denham.skyline.ui.components.ArtworkImage
import com.denham.skyline.ui.components.ChannelCard
import com.denham.skyline.ui.components.FixtureCard
import com.denham.skyline.ui.components.enterReveal
import com.denham.skyline.ui.components.revealDelay
import com.denham.skyline.ui.components.LiveNowRow
import com.denham.skyline.ui.components.PillButton
import com.denham.skyline.ui.components.PosterCard
import com.denham.skyline.ui.components.Rail
import com.denham.skyline.ui.components.SectionHeader
import com.denham.skyline.ui.components.ShimmerBox
import com.denham.skyline.ui.components.ShimmerRail
import com.denham.skyline.ui.components.SkylineWordmark
import com.denham.skyline.ui.components.YouTubeCard
import com.denham.skyline.ui.components.scaledClickable
import com.denham.skyline.ui.theme.SkyPalette
import com.denham.skyline.ui.theme.SkyRadius
import com.denham.skyline.ui.theme.SkySpacing
import com.denham.skyline.ui.update.UpdatePrompt
import com.denham.skyline.ui.update.UpdateViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

/** Keyword defaults for family members who haven't picked categories in Settings yet. */
/**
 * Matched as substrings against the provider's *live TV* category names
 * (e.g. "UK ~ Sky Sports+"), so the terms have to look like channel-genre
 * names rather than film genres. Anne's original list was VOD genres
 * ("mystery", "crime", "thriller") which match no live category, and Ava and
 * Sophie had no entries at all -- all three silently fell through to the
 * default rails, making their tabs look like they did nothing.
 *
 * These are still only starting points; anyone can override them per person
 * via Settings -> Customize home categories, which wins over this map.
 */
private val familyKeywordDefaults = mapOf(
    "David" to listOf("football", "soccer", "sport", "wwe", "wrestling", "aew", "comedy"),
    "Anne" to listOf(
        "crime", "drama", "documentar", "factual", "entertainment",
        "news", "lifestyle", "real",
    ),
    "Ava" to listOf(
        "entertainment", "music", "comedy", "lifestyle", "reality", "film", "movie",
    ),
    "Sophie" to listOf(
        "kid", "child", "cartoon", "family", "disney", "nick",
        "junior", "cbeebies", "boomerang", "toon",
    ),
)

/**
 * Loading/hidden/loaded wrapper for the David-only "Football" section.
 * [Hidden] covers both "not David" and "no API key configured" -- the
 * design brief's graceful-degradation contract renders nothing, not an
 * error, in either case, so callers don't need to distinguish them.
 */
sealed interface FootballSectionState {
    data object Hidden : FootballSectionState
    data object Loading : FootballSectionState
    data class Loaded(
        val manUtdNext: Fixture?,
        val roundFixtures: List<Fixture>,
    ) : FootballSectionState
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    val syncState: StateFlow<SyncState> = container.contentRepository.syncState

    val favoriteChannels = container.db.channelDao().observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val popularChannels = container.db.channelDao().observeFirst(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentMovies = container.db.movieDao().observeRecentlyAdded(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSeries = container.db.seriesDao().observeRecentlyAdded(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Widescreen artwork for the hero (get_vod_info backdrop, not the poster). */
    private val _heroBackdrop = MutableStateFlow<String?>(null)
    val heroBackdrop: StateFlow<String?> = _heroBackdrop

    /** Continue Watching: the last item the player recorded. */
    val continueWatching = container.historyStore.lastPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Family member selector: which person's preferences to show. */
    private val _selectedFamilyMember = MutableStateFlow<String?>(null)
    val selectedFamilyMember: StateFlow<String?> = _selectedFamilyMember

    /** All available categories from provider. */
    val allCategories: StateFlow<List<com.denham.skyline.data.db.CategoryEntity>> =
        container.db.categoryDao().observe(ContentType.LIVE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Sky-style genre rails: one rail per live category. Personalized per family member if selected. */
    val categoryRails: StateFlow<List<Pair<String, List<ChannelEntity>>>> =
        combine(_selectedFamilyMember, container.account, allCategories) { selectedMember, account, allCats ->
            // Determine which categories to display
            val categoriesToShow = if (selectedMember != null && account != null) {
                // Load user preferences from account
                container.categoryPreferencesStore.observePreferences(account.username)
                    .map { prefs ->
                        val userCategoryIds = prefs.preferences[selectedMember] ?: emptyList()
                        when {
                            userCategoryIds.isNotEmpty() ->
                                allCats.filter { it.categoryId in userCategoryIds }
                            else -> defaultCategoriesFor(selectedMember, allCats)
                        }
                    }
            } else {
                // Default: show top 4 categories
                flowOf(allCats.take(4))
            }
            categoriesToShow
        }
            .flatMapLatest { categoriesFlow ->
                categoriesFlow.flatMapLatest { categories ->
                    if (categories.isEmpty()) flowOf(emptyList())
                    else combine(
                        categories.map { category ->
                            container.db.channelDao()
                                .observeByCategory(category.categoryId, 12)
                                .map { channels -> category.name to channels }
                        }
                    ) { rails -> rails.filter { it.second.isNotEmpty() } }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Channels the selected member pinned individually, independent of their
     * categories -- how a single channel reaches one member's tab without
     * dragging in the rest of its category.
     *
     * Empty when nobody is selected: pins belong to a member's tab, not the
     * default page. Ids the provider has since dropped simply do not come
     * back from [byIds], so a stale pin disappears rather than rendering a
     * broken card.
     */
    val pinnedChannels: StateFlow<List<ChannelEntity>> =
        combine(_selectedFamilyMember, container.account) { member, account -> member to account }
            .flatMapLatest { (member, account) ->
                if (member == null || account == null) {
                    flowOf(emptyList())
                } else {
                    container.categoryPreferencesStore.observePreferences(account.username)
                        .map { prefs -> prefs.pinnedChannels[member] ?: emptyList() }
                        .map { ids ->
                            if (ids.isEmpty()) emptyList()
                            else container.db.channelDao().byIds(ids)
                        }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Now-playing titles for the channel tiles on this screen. */
    val epg: StateFlow<Map<Int, EpgNowNextEntity>> =
        combine(favoriteChannels, popularChannels) { favorites, popular ->
            (favorites + popular).map { it.streamId }.distinct()
        }
            .flatMapLatest { ids ->
                if (ids.isEmpty()) flowOf(emptyList())
                else container.epgRepository.observeForIds(ids)
            }
            .map { list -> list.associateBy { it.streamId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Current-programme artwork (from the XMLTV guide) for the channel tiles,
     *  so TV rails can show real imagery instead of just the channel logo. */
    val nowProgrammeImages: StateFlow<Map<Int, String>> =
        combine(favoriteChannels, popularChannels) { favorites, popular ->
            (favorites + popular).map { it.streamId }.distinct()
        }
            .transformLatest { ids ->
                if (ids.isEmpty()) emit(emptyMap())
                else emit(container.db.guideDao()
                    .nowImages(ids, System.currentTimeMillis())
                    .associate { it.channelStreamId to it.imageUrl })
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Resolved programme images (by title lookup against VOD) for channels without XMLTV artwork. */
    val resolvedProgrammeImages: StateFlow<Map<Int, String>> =
        epg.transformLatest { epgMap ->
            val resolved = mutableMapOf<Int, String>()
            for ((streamId, nowNext) in epgMap) {
                val title = nowNext.nowTitle
                val image = resolveProgrammeImage(title)
                if (image != null) {
                    resolved[streamId] = image
                }
            }
            emit(resolved)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** YouTube videos for selected family member's subscribed channels. */
    val youtubeVideos: StateFlow<List<com.denham.skyline.data.repo.YouTubeVideo>> =
        combine(_selectedFamilyMember, container.account) { familyMember, account ->
            if (familyMember == null || account == null) {
                return@combine emptyList<String>()
            }
            // Load subscribed channel IDs for this family member
            container.categoryPreferencesStore.getYoutubeSubscriptions(account.username, familyMember)
        }
            .flatMapLatest { channelIds ->
                if (channelIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    // Trigger refresh for all channels (cache-aware: skips if <30 min old)
                    viewModelScope.launch {
                        channelIds.forEach { channelId ->
                            container.youtubeRepository.refreshChannelVideos(channelId)
                        }
                    }
                    // Observe latest videos across all subscribed channels
                    container.youtubeRepository.observeLatestVideos(channelIds, limit = 30)
                        .map { entities ->
                            entities.map { entity ->
                                com.denham.skyline.data.repo.YouTubeVideo(
                                    id = entity.videoId,
                                    title = entity.title,
                                    thumbnail = entity.thumbnailUrl ?: "",
                                    publishedAt = formatDate(entity.publishedAt),
                                    videoUrl = "https://www.youtube.com/watch?v=${entity.videoId}"
                                )
                            }
                        }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * David-only "Football" section: Man Utd's next fixture + the upcoming
     * round of Premier League fixtures. Hidden for every other member/no
     * member, and hidden (not an error state) when `FOOTBALL_DATA_API_KEY`
     * is blank -- [FootballRepository] itself never throws on a bad key/
     * response, and this adds a defensive [runCatching] layer on top,
     * matching the rest of this ViewModel's "a fetch failure must never
     * blank the whole screen" pattern.
     */
    val footballSection: StateFlow<FootballSectionState> = _selectedFamilyMember
        .flatMapLatest { member ->
            val apiKey = BuildConfig.FOOTBALL_DATA_API_KEY
            if (member != "David" || apiKey.isBlank()) {
                flowOf<FootballSectionState>(FootballSectionState.Hidden)
            } else {
                kotlinx.coroutines.flow.flow<FootballSectionState> {
                    while (true) {
                        emit(FootballSectionState.Loading)
                        val manUtdNext = runCatching { container.footballRepository.nextManUtdFixture(apiKey) }
                            .onFailure { android.util.Log.e("HomeViewModel", "Man Utd fixture fetch failed", it) }
                            .getOrNull()
                        val roundFixtures = runCatching { container.footballRepository.upcomingPremierLeagueRound(apiKey) }
                            .onFailure { android.util.Log.e("HomeViewModel", "Upcoming PL round fetch failed", it) }
                            .getOrDefault(emptyList())
                        emit(FootballSectionState.Loaded(manUtdNext, roundFixtures))
                        delay(30_000)
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FootballSectionState.Hidden)

    /**
     * EPG channels matched per fixture (by database id), for the spotlight
     * card and every rail card. Looked up independently per fixture so one
     * failing lookup doesn't blank the others.
     */
    val fixtureChannels: StateFlow<Map<Long, List<ChannelEntity>>> = footballSection
        .flatMapLatest { state ->
            val fixtures = when (state) {
                is FootballSectionState.Loaded ->
                    (listOfNotNull(state.manUtdNext) + state.roundFixtures).distinctBy { it.id }
                else -> emptyList()
            }
            if (fixtures.isEmpty()) {
                flowOf(emptyMap())
            } else {
                kotlinx.coroutines.flow.flow {
                    val result = mutableMapOf<Long, List<ChannelEntity>>()
                    for (fixture in fixtures) {
                        result[fixture.id] = runCatching { channelsForFixture(fixture) }.getOrDefault(emptyList())
                    }
                    emit(result)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch { container.contentRepository.syncAll() }

        // Refresh EPG every app load for latest guide data. Guarded: a guide
        // fetch/parse failure shouldn't be able to kill the process.
        viewModelScope.launch {
            runCatching { container.guideRepository.refreshIfStale(maxAgeMs = 0) }
                .onFailure { android.util.Log.e("HomeViewModel", "Guide refresh failed", it) }
        }

        // Hero: once the first movie lands, fetch its detail for real backdrop art.
        viewModelScope.launch {
            val hero = recentMovies.first { it.isNotEmpty() }.firstOrNull() ?: return@launch
            val info = runCatching { container.contentRepository.vodInfo(hero.streamId) }
                .getOrNull()?.info
            _heroBackdrop.value = info?.backdropPath?.firstOrNull() ?: info?.movieImage
        }

        // Warm the now/next cache for visible tiles (TTL-guarded, sequential
        // so we stay far under the panel's rate limit).
        viewModelScope.launch {
            combine(favoriteChannels, popularChannels) { favorites, popular ->
                (favorites + popular).map { it.streamId }.distinct()
            }.collect { ids ->
                for (id in ids) {
                    runCatching { container.epgRepository.refresh(id) }
                }
            }
        }

        // Initialize default YouTube subscriptions for Sophie on first app launch.
        // Wrapped: this runs on every launch, and an exception escaping a
        // viewModelScope.launch reaches the default handler and kills the
        // process -- a seeding failure must never take the whole app down.
        viewModelScope.launch {
            runCatching {
                val account = container.account.value ?: return@runCatching
                val existing = container.categoryPreferencesStore
                    .getYoutubeSubscriptions(account.username, "Sophie")
                if (existing.isEmpty()) {
                    val sophieDefaults = listOf(
                        "UCLs_JK8qnG_-IqrmRcIReVA",  // Ryan's World
                        "UCxGDGLb_wYvQVV8p7pFRvHg",  // Most Maples
                        "UCrXTn0p3-w2K3qYXnfZdPWA"   // Moriah Elisabeth
                    )
                    container.categoryPreferencesStore.setYoutubeSubscriptions(
                        account.username, "Sophie", sophieDefaults
                    )
                    // Fetch videos for these channels
                    sophieDefaults.forEach { channelId ->
                        container.youtubeRepository.refreshChannelVideos(channelId, forceFresh = true)
                    }
                }
            }.onFailure {
                android.util.Log.e("HomeViewModel", "YouTube default seeding failed", it)
            }
        }
    }

    fun retrySync() {
        viewModelScope.launch { container.contentRepository.syncAll(force = true) }
    }

    fun selectFamilyMember(name: String?) {
        _selectedFamilyMember.value = name
    }

    /** Categories this person has explicitly chosen, for pre-ticking the editor. */
    suspend fun loadFamilyMemberPreferences(familyMemberName: String): List<String> {
        val accountId = container.account.value?.username ?: return emptyList()
        return runCatching {
            container.categoryPreferencesStore.getUserCategories(accountId, familyMemberName)
        }.getOrDefault(emptyList())
    }

    // saveFamilyMemberPreferences was removed in favour of saveMemberTab.
    // Saving categories on their own is what raced with saving pins, so
    // leaving it here would just be a loaded gun for the next call site.

    /** Channels this person pinned individually, for pre-ticking the editor. */
    suspend fun loadPinnedChannels(familyMemberName: String): List<ChannelEntity> {
        val accountId = container.account.value?.username ?: return emptyList()
        return runCatching {
            val ids = container.categoryPreferencesStore.getPinnedChannels(accountId, familyMemberName)
            if (ids.isEmpty()) emptyList() else container.db.channelDao().byIds(ids)
        }.getOrDefault(emptyList())
    }

    /**
     * Saves a member's categories and pinned channels together.
     *
     * Deliberately one call, not two: the editor originally saved each in its
     * own coroutine, and because each did a separate read-modify-write on the
     * same DataStore key, the second write dropped the first one's changes.
     * Pins landed, categories disappeared, and the tab fell back to keyword
     * defaults as though nothing had been saved.
     */
    fun saveMemberTab(
        familyMemberName: String,
        selectedCategoryIds: List<String>,
        streamIds: List<Int>,
    ) {
        viewModelScope.launch {
            val accountId = container.account.value?.username ?: return@launch
            container.categoryPreferencesStore.setMemberTab(
                accountId, familyMemberName, selectedCategoryIds, streamIds,
            )
        }
    }

    /**
     * Channel search for the pinning editor. FTS-backed and capped at 100 by
     * the DAO — a provider carries thousands of channels, so scrolling the
     * full list to find one is not a usable way to pin it.
     */
    suspend fun searchChannels(query: String): List<ChannelEntity> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()
        // Same FTS form SearchScreen already uses: quoted phrase with a
        // prefix wildcard, embedded quotes stripped. MATCH treats a stray
        // quote as syntax and throws, and this is the shape already proven
        // against this provider's channel names.
        val ftsQuery = "\"${trimmed.replace("\"", "")}\"*"
        return runCatching { container.db.channelDao().search(ftsQuery) }.getOrDefault(emptyList())
    }

    /**
     * Categories to show for a family member before they've picked any in
     * Settings — matched by keyword against the provider's category names so
     * tapping a name visibly changes the rails on first use.
     */
    private fun defaultCategoriesFor(
        member: String,
        allCats: List<com.denham.skyline.data.db.CategoryEntity>,
    ): List<com.denham.skyline.data.db.CategoryEntity> {
        val keywords = familyKeywordDefaults[member] ?: emptyList()
        if (keywords.isEmpty()) return allCats.take(4)
        val matched = allCats.filter { cat ->
            keywords.any { keyword -> cat.name.contains(keyword, ignoreCase = true) }
        }
        return matched.ifEmpty { allCats.take(4) }
    }

    suspend fun resolveProgrammeImage(title: String?): String? {
        if (title.isNullOrBlank()) return null

        val titleKey = title.lowercase().trim()

        val cached = container.db.programmeImageDao().get(titleKey)
        if (cached != null) return cached.imageUrl

        val movie = container.db.movieDao().findByName(title)
        if (movie != null) {
            container.db.programmeImageDao().upsert(
                com.denham.skyline.data.db.ProgrammeImageEntity(
                    titleKey = titleKey,
                    imageUrl = movie.icon,
                    resolvedAt = System.currentTimeMillis()
                )
            )
            return movie.icon
        }

        val series = container.db.seriesDao().findByName(title)
        if (series != null) {
            container.db.programmeImageDao().upsert(
                com.denham.skyline.data.db.ProgrammeImageEntity(
                    titleKey = titleKey,
                    imageUrl = series.cover,
                    resolvedAt = System.currentTimeMillis()
                )
            )
            return series.cover
        }

        container.db.programmeImageDao().upsert(
            com.denham.skyline.data.db.ProgrammeImageEntity(
                titleKey = titleKey,
                imageUrl = null,
                resolvedAt = System.currentTimeMillis()
            )
        )
        return null
    }

    /**
     * EPG channels carrying a fixture: title `LIKE` match against either
     * team name, windowed to kickoff +/- 90 min, resolved to real channels
     * via `ChannelDao.byIds` -- never a fabricated channel. A new
     * `GuideDao` query (not the favourites+popular-scoped ones this
     * ViewModel otherwise uses) because a fixture's carrier channel can be
     * any sports channel in the provider's catalogue, not just what's
     * already on Home.
     */
    private suspend fun channelsForFixture(fixture: Fixture): List<ChannelEntity> {
        val windowMs = 90 * 60 * 1000L
        val ids = container.db.guideDao().channelsForFixture(
            homeTeamLike = "%${fixture.homeTeam}%",
            awayTeamLike = "%${fixture.awayTeam}%",
            fromMs = fixture.kickoffMs - windowMs,
            toMs = fixture.kickoffMs + windowMs,
        )
        return if (ids.isEmpty()) emptyList() else container.db.channelDao().byIds(ids)
    }

    private fun formatDate(timestampMs: Long): String {
        return try {
            val instant = java.time.Instant.ofEpochMilli(timestampMs)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd")
            instant.atZone(java.time.ZoneId.systemDefault())
                .format(formatter)
        } catch (e: Exception) {
            ""
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    updateViewModel: UpdateViewModel? = null,
    onPlayChannel: (ChannelEntity) -> Unit,
    onOpenMovie: (MovieEntity) -> Unit,
    onOpenSeries: (SeriesEntity) -> Unit,
    onResume: (com.denham.skyline.data.prefs.LastPlayed) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAccount: () -> Unit,
    onViewAllLive: () -> Unit,
    onViewAllMovies: () -> Unit,
    onViewAllSeries: () -> Unit,
    onPlayVod: (url: String, title: String) -> Unit = { _, _ -> },
    onPlayYoutube: (videoId: String, title: String, thumbnailUrl: String?) -> Unit = { _, _, _ -> },
) {
    val sync by viewModel.syncState.collectAsState()
    val favorites by viewModel.favoriteChannels.collectAsState()
    val popular by viewModel.popularChannels.collectAsState()
    val movies by viewModel.recentMovies.collectAsState()
    val series by viewModel.recentSeries.collectAsState()
    val heroBackdrop by viewModel.heroBackdrop.collectAsState()
    val categoryRails by viewModel.categoryRails.collectAsState()
    val epg by viewModel.epg.collectAsState()
    val lastPlayed by viewModel.continueWatching.collectAsState()
    val selectedMember by viewModel.selectedFamilyMember.collectAsState()
    val youtubeVideos by viewModel.youtubeVideos.collectAsState()
    val pinned by viewModel.pinnedChannels.collectAsState()
    val footballSection by viewModel.footballSection.collectAsState()
    val fixtureChannels by viewModel.fixtureChannels.collectAsState()

    // Check for updates on first load. Silent: this is automatic, so a failure
    // should not interrupt the user with a dialog before they've done anything.
    LaunchedEffect(Unit) {
        updateViewModel?.checkForUpdates(silent = true)
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.Canvas),
    ) {
        // Hero spotlight with brand header, Sky "Top Picks" style.
        item {
            Box {
                val hero = movies.firstOrNull()
                HeroSpotlight(
                    title = hero?.name ?: "Welcome to Sky Go",
                    imageUrl = heroBackdrop ?: hero?.icon,
                    fallbackImageUrl = hero?.icon,
                    subtitle = hero?.let { "Newly added" }
                        ?: "Your channels, films and shows in one place",
                    actionLabel = hero?.let { "Watch now" },
                    onAction = hero?.let { { onOpenMovie(it) } },
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    SkylineWordmark()
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            Icons.Default.Search, "Search",
                            tint = SkyPalette.TextPrimary.copy(alpha = 0.85f),
                        )
                    }
                    IconButton(onClick = onOpenAccount) {
                        Icon(
                            Icons.Default.AccountCircle, "Account",
                            tint = SkyPalette.TextPrimary.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }

        // Continue Watching, top of the page like the mock.
        lastPlayed?.let { last ->
            item {
                SectionHeader("Continue Watching")
                ContinueWatchingCard(last, onClick = { onResume(last) })
            }
        }

        // Live Now: favourites with a programme currently on air.
        // Everything from here down to the selector is the *default* page,
        // shown only when nobody is selected. Picking a member switches to
        // their own tab -- their pins, categories and YouTube -- rather than
        // burying it under six rails they did not choose. Continue Watching
        // above is deliberately outside this guard: it reflects what that
        // person was actually watching, not what the provider is promoting.
        if (selectedMember == null) {
            item {
                val liveNow = favorites.filter { epg[it.streamId]?.nowTitle != null }.take(5)
                if (liveNow.isNotEmpty()) {
                    SectionHeader("Live Now", onViewAll = onViewAllLive)
                    liveNow.forEach { ch ->
                        LiveNowRow(
                            title = epg[ch.streamId]?.nowTitle ?: ch.name,
                            subtitle = ch.name,
                            imageUrl = ch.icon,
                            onClick = { onPlayChannel(ch) },
                        )
                    }
                }
            }
        }

        when (val s = sync) {
            is SyncState.Running -> {
                item {
                    Text(
                        s.step,
                        style = MaterialTheme.typography.labelMedium,
                        color = SkyPalette.TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                item { ShimmerRail() }
                item { ShimmerRail() }
            }
            is SyncState.Failed -> {
                item {
                    Column(Modifier.padding(16.dp)) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = viewModel::retrySync) { Text("Try again") }
                    }
                }
            }
            else -> Unit
        }

        // The provider's general browse rails: default page only, same
        // reasoning as Live Now above. The sync status/shimmer block stays
        // outside this guard on purpose -- it reports first-run progress and
        // sync failures, and hiding it behind member selection would make a
        // failed sync invisible to whoever happens to have a member picked.
        if (selectedMember == null) {
            item {
                Rail("Favourites", favorites, key = { it.streamId }) { ch ->
                    ChannelCard(
                        ch.name, ch.icon,
                        subtitle = epg[ch.streamId]?.nowTitle,
                        onClick = { onPlayChannel(ch) },
                    )
                }
            }
            item {
                if (popular.isNotEmpty()) SectionHeader("Live TV", onViewAll = onViewAllLive)
                Rail("", popular, key = { it.streamId }) { ch ->
                    ChannelCard(
                        ch.name, ch.icon,
                        subtitle = epg[ch.streamId]?.nowTitle,
                        onClick = { onPlayChannel(ch) },
                    )
                }
            }
            item {
                if (movies.isNotEmpty()) SectionHeader("New films", onViewAll = onViewAllMovies)
                Rail("", movies, key = { it.streamId }) { m ->
                    PosterCard(m.name, m.icon, onClick = { onOpenMovie(m) })
                }
            }
            item {
                if (series.isNotEmpty()) SectionHeader("New series", onViewAll = onViewAllSeries)
                Rail("", series, key = { it.seriesId }) { s ->
                    PosterCard(s.name, s.cover, onClick = { onOpenSeries(s) })
                }
            }
        }

        // Family member selector: picks whose personalized rails show below.
        item {
            SectionHeader("Who's watching?")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                listOf("David", "Anne", "Ava", "Sophie").forEach { member ->
                    val isSelected = selectedMember == member
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.selectFamilyMember(if (isSelected) null else member)
                        },
                        label = { Text(member, maxLines = 1) },
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

        // The selected member's individually pinned channels, directly under
        // the selector: these are the ones they named explicitly, so they
        // outrank both their categories and their YouTube.
        if (pinned.isNotEmpty() && selectedMember != null) {
            item {
                Column(Modifier.enterReveal(revealDelay(0))) {
                    Rail("$selectedMember's channels", pinned, key = { it.streamId }) { ch ->
                        ChannelCard(
                            ch.name, ch.icon,
                            subtitle = epg[ch.streamId]?.nowTitle,
                            onClick = { onPlayChannel(ch) },
                        )
                    }
                }
            }
        }

        // David-only "Football" section: Man Utd next-fixture spotlight +
        // upcoming-Premier-League-round rail, directly under his pinned
        // channels (which still outrank it -- an explicit pin beats
        // anything automatic) and ahead of YouTube/category rails, for the
        // same "time-decaying, happening today" reasoning that puts Live
        // Now near the top of the default page. Scoped to "David"
        // specifically (his personal sports interest), not
        // selectedMember != null.
        val showFootball = selectedMember == "David" && when (val football = footballSection) {
            FootballSectionState.Hidden -> false
            FootballSectionState.Loading -> true
            is FootballSectionState.Loaded ->
                football.manUtdNext != null || football.roundFixtures.isNotEmpty()
        }
        if (showFootball) {
            item {
                Column(Modifier.enterReveal(revealDelay(0))) {
                    SectionHeader("Football")
                    when (val football = footballSection) {
                        is FootballSectionState.Loading -> {
                            ShimmerBox(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = SkySpacing.gutter)
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(SkyRadius.card)),
                            )
                            Spacer(Modifier.height(SkySpacing.s))
                            ShimmerRail()
                        }
                        is FootballSectionState.Loaded -> {
                            football.manUtdNext?.let { fixture ->
                                Text(
                                    "Man Utd next",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SkyPalette.TextSecondary,
                                    modifier = Modifier.padding(
                                        horizontal = SkySpacing.gutter,
                                        vertical = SkySpacing.s,
                                    ),
                                )
                                FixtureCard(
                                    competition = fixture.competition,
                                    homeTeam = fixture.homeTeam,
                                    awayTeam = fixture.awayTeam,
                                    homeCrestUrl = fixture.homeCrestUrl,
                                    awayCrestUrl = fixture.awayCrestUrl,
                                    status = fixture.status,
                                    channels = fixtureChannels[fixture.id] ?: emptyList(),
                                    onPlayChannel = onPlayChannel,
                                    width = null,
                                    isSpotlight = true,
                                    modifier = Modifier
                                        .padding(horizontal = SkySpacing.gutter)
                                        .fillMaxWidth(),
                                )
                            }
                            if (football.roundFixtures.isNotEmpty()) {
                                Rail("", football.roundFixtures, key = { it.id }) { fixture ->
                                    FixtureCard(
                                        competition = fixture.competition,
                                        homeTeam = fixture.homeTeam,
                                        awayTeam = fixture.awayTeam,
                                        homeCrestUrl = fixture.homeCrestUrl,
                                        awayCrestUrl = fixture.awayCrestUrl,
                                        status = fixture.status,
                                        channels = fixtureChannels[fixture.id] ?: emptyList(),
                                        onPlayChannel = onPlayChannel,
                                    )
                                }
                            }
                        }
                        FootballSectionState.Hidden -> Unit
                    }
                }
            }
        }

        // YouTube carousel for the selected family member. Sits directly under
        // the selector that controls it: below the category rails it was at
        // the very bottom of a long page, so picking someone appeared to do
        // nothing even when their videos had loaded.
        if (youtubeVideos.isNotEmpty() && selectedMember != null) {
            item {
                Column(Modifier.enterReveal(revealDelay(0))) {
                    SectionHeader("YouTube for $selectedMember")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
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
                            )
                        }
                    }
                }
            }
        }

        // Sky-style genre rails, one per live category.
        items(categoryRails.size, key = { categoryRails[it].first }) { index ->
            val (categoryName, channels) = categoryRails[index]
            Column(Modifier.enterReveal(revealDelay(index))) {
                Rail(categoryName, channels, key = { it.streamId }) { ch ->
                    ChannelCard(
                        ch.name, ch.icon,
                        subtitle = epg[ch.streamId]?.nowTitle,
                        onClick = { onPlayChannel(ch) },
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    // Show update prompt if available
    updateViewModel?.let {
        UpdatePrompt(it, BuildConfig.BUILD_TIME)
    }
}

/** Mock-style Continue Watching card: artwork, Resume pill, progress line. */
@Composable
private fun ContinueWatchingCard(last: LastPlayed, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .scaledClickable(onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(170.dp),
        ) {
            if (!last.imageUrl.isNullOrBlank()) {
                ArtworkImage(
                    url = last.imageUrl,
                    contentDescription = last.title,
                    fallbackIcon = Icons.Default.Movie,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Box(
                    Modifier.matchParentSize().background(SkyPalette.heroFallback),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Movie,
                        contentDescription = null,
                        tint = SkyPalette.TextMuted,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Box(Modifier.matchParentSize().background(SkyPalette.heroScrim))
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
            ) {
                Text(
                    last.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                PillButton(
                    label = if (last.type == "live") "Watch" else "Resume",
                    onClick = onClick,
                )
            }
        }
        if (last.type != "live" && last.progress > 0f) {
            LinearProgressIndicator(
                progress = { last.progress },
                color = SkyPalette.Accent,
                trackColor = SkyPalette.SurfaceElevated,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HeroSpotlight(
    title: String,
    imageUrl: String?,
    subtitle: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    fallbackImageUrl: String? = null,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(320.dp),
    ) {
        if (imageUrl != null || fallbackImageUrl != null) {
            ArtworkImage(
                url = imageUrl,
                fallbackUrl = fallbackImageUrl,
                contentDescription = title,
                fallbackIcon = Icons.Default.Movie,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Box(Modifier.matchParentSize().background(SkyPalette.heroFallback))
        }
        Box(Modifier.matchParentSize().background(SkyPalette.heroScrim))
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.displayMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SkyPalette.TextSecondary,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onAction) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}
