package com.denham.skyline.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.data.prefs.LastPlayed
import com.denham.skyline.di.AppContainer
import com.denham.skyline.ui.account.AccountScreen
import com.denham.skyline.ui.account.MyListScreen
import com.denham.skyline.ui.account.MyListViewModel
import com.denham.skyline.ui.browse.MoviesScreen
import com.denham.skyline.ui.browse.MoviesViewModel
import com.denham.skyline.ui.browse.SeriesScreen
import com.denham.skyline.ui.browse.SeriesViewModel
import com.denham.skyline.ui.containerViewModel
import com.denham.skyline.ui.detail.MovieDetailScreen
import com.denham.skyline.ui.detail.MovieDetailViewModel
import com.denham.skyline.ui.detail.SeriesDetailScreen
import com.denham.skyline.ui.detail.SeriesDetailViewModel
import com.denham.skyline.ui.downloads.DownloadsScreen
import com.denham.skyline.ui.downloads.DownloadsViewModel
import com.denham.skyline.ui.guide.GuideScreen
import com.denham.skyline.ui.guide.GuideViewModel
import com.denham.skyline.ui.home.HomeScreen
import com.denham.skyline.ui.home.HomeViewModel
import com.denham.skyline.ui.update.UpdateViewModel
import com.denham.skyline.ui.live.LiveScreen
import com.denham.skyline.ui.live.LiveViewModel
import com.denham.skyline.ui.login.LoginScreen
import com.denham.skyline.ui.login.LoginViewModel
import com.denham.skyline.ui.player.PlayerScreen
import com.denham.skyline.ui.player.PlayerViewModel
import com.denham.skyline.ui.player.YouTubePlayerScreen
import com.denham.skyline.ui.search.SearchScreen
import com.denham.skyline.ui.search.SearchViewModel
import com.denham.skyline.ui.settings.CategoryCustomizationScreen
import com.denham.skyline.ui.settings.SettingsScreen
import com.denham.skyline.ui.settings.SettingsViewModel
import com.denham.skyline.ui.settings.YouTubeSubscriptionScreen
import com.denham.skyline.ui.settings.YouTubeSubscriptionViewModel
import com.denham.skyline.ui.theme.SkyPalette

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val LIVE = "live"
    const val SPORTS = "sports"
    const val MOVIES = "movies"
    const val SERIES = "series"
    const val GUIDE = "guide"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val CATEGORY_CUSTOMIZATION = "category_customization/{familyMember}"
    const val YOUTUBE_SUBSCRIPTIONS = "youtube_subscriptions"
    const val ACCOUNT = "account"
    const val DOWNLOADS = "downloads"
    const val MY_LIST = "mylist"
    const val RECORDINGS = "recordings"
    const val PLAYER_LIVE = "player/live/{streamId}"
    const val PLAYER_VOD = "player/vod"
    const val PLAYER_YOUTUBE = "player/youtube"
    const val MOVIE_DETAIL = "movie/{movieId}"
    const val SERIES_DETAIL = "series/{seriesId}"

    fun playerLive(streamId: Int) = "player/live/$streamId"
    fun movieDetail(id: Int) = "movie/$id"
    fun seriesDetail(id: Int) = "series/$id"
    fun categoryCustomization(familyMember: String) = "category_customization/$familyMember"
}

/** Transient VOD playback handoff (URLs don't belong in nav args). */
data class VodPlaybackRequest(
    val url: String,
    val title: String,
    val imageUrl: String? = null,
    val resumeFromMs: Long = 0L,
)

/** Transient YouTube video playback handoff. */
data class YouTubePlaybackRequest(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String? = null,
)

// Bottom nav: Home · Live · Films · Series · TV Guide. Sports lives in Live's
// category chips; Downloads and Recordings are reached from the Account page.
private val bottomNavItems = listOf(
    Triple(Routes.HOME, "Home", Icons.Default.Home),
    Triple(Routes.LIVE, "Live", Icons.Default.LiveTv),
    Triple(Routes.MOVIES, "Films", Icons.Default.Movie),
    Triple(Routes.SERIES, "Series", Icons.Default.Tv),
    Triple(Routes.GUIDE, "TV Guide", Icons.Default.ViewList),
)

@UnstableApi
@Composable
fun SkylineNavHost(
    navController: NavHostController,
    container: AppContainer,
    isInPip: Boolean,
    signedIn: Boolean,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = !isInPip && currentRoute in bottomNavItems.map { it.first }

    fun playVod(request: VodPlaybackRequest) {
        container.pendingVodPlayback = request
        navController.navigate(Routes.PLAYER_VOD)
    }

    fun playYoutube(request: YouTubePlaybackRequest) {
        container.pendingYoutubePlayback = request
        navController.navigate(Routes.PLAYER_YOUTUBE)
    }

    Scaffold(
        containerColor = SkyPalette.Canvas,
        bottomBar = {
            if (showBottomBar) {
                // Mock style: black bar, blue selected icon+label, no pill.
                NavigationBar(containerColor = SkyPalette.Canvas) {
                    bottomNavItems.forEach { (route, label, icon) ->
                        val selected = currentRoute == route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, label) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SkyPalette.Accent,
                                selectedTextColor = SkyPalette.Accent,
                                unselectedIconColor = SkyPalette.TextSecondary,
                                unselectedTextColor = SkyPalette.TextSecondary,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (signedIn) Routes.HOME else Routes.LOGIN,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(200)) },
        ) {
            composable(Routes.LOGIN) {
                val vm = containerViewModel { LoginViewModel(it) }
                LoginScreen(vm) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            }

            composable(Routes.HOME) {
                val vm = containerViewModel { HomeViewModel(it) }
                val updateVm = containerViewModel { UpdateViewModel(it.updateRepository) }
                HomeScreen(
                    viewModel = vm,
                    updateViewModel = updateVm,
                    onPlayChannel = { navController.navigateToLivePlayer(it) },
                    onOpenMovie = { navController.navigate(Routes.movieDetail(it.streamId)) },
                    onOpenSeries = { navController.navigate(Routes.seriesDetail(it.seriesId)) },
                    onResume = { last: LastPlayed ->
                        if (last.type == "live") {
                            navController.navigate(Routes.playerLive(last.id))
                        } else {
                            playVod(
                                VodPlaybackRequest(
                                    url = last.playbackUrl,
                                    title = last.title,
                                    imageUrl = last.imageUrl,
                                    resumeFromMs = last.positionMs,
                                )
                            )
                        }
                    },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onOpenAccount = { navController.navigate(Routes.ACCOUNT) },
                    onViewAllLive = { navController.navigate(Routes.LIVE) },
                    onViewAllMovies = { navController.navigate(Routes.MOVIES) },
                    onViewAllSeries = { navController.navigate(Routes.SERIES) },
                    onPlayVod = { url, title ->
                        playVod(
                            VodPlaybackRequest(
                                url = url,
                                title = title,
                            )
                        )
                    },
                    onPlayYoutube = { videoId, title, thumbnailUrl ->
                        playYoutube(
                            YouTubePlaybackRequest(
                                videoId = videoId,
                                title = title,
                                thumbnailUrl = thumbnailUrl,
                            )
                        )
                    },
                )
            }

            composable(Routes.LIVE) {
                val vm = containerViewModel { LiveViewModel(it) }
                LiveScreen(
                    viewModel = vm,
                    onPlayChannel = { navController.navigateToLivePlayer(it) },
                )
            }

            composable(Routes.SPORTS) {
                val vm = containerViewModel { LiveViewModel(it, sportsOnly = true) }
                LiveScreen(
                    viewModel = vm,
                    onPlayChannel = { navController.navigateToLivePlayer(it) },
                    title = "Sports",
                )
            }

            composable(Routes.MOVIES) {
                val vm = containerViewModel { MoviesViewModel(it) }
                MoviesScreen(vm) { navController.navigate(Routes.movieDetail(it.streamId)) }
            }

            composable(Routes.SERIES) {
                val vm = containerViewModel { SeriesViewModel(it) }
                SeriesScreen(vm) { navController.navigate(Routes.seriesDetail(it.seriesId)) }
            }

            composable(Routes.GUIDE) {
                val vm = containerViewModel { GuideViewModel(it) }
                GuideScreen(
                    viewModel = vm,
                    onPlayChannel = { navController.navigateToLivePlayer(it) },
                )
            }

            composable(Routes.SEARCH) {
                val vm = containerViewModel { SearchViewModel(it) }
                SearchScreen(
                    viewModel = vm,
                    onPlayChannel = { navController.navigateToLivePlayer(it) },
                    onOpenMovie = { navController.navigate(Routes.movieDetail(it.streamId)) },
                    onOpenSeries = { navController.navigate(Routes.seriesDetail(it.seriesId)) },
                )
            }

            composable(Routes.ACCOUNT) {
                AccountScreen(
                    username = container.account.value?.username ?: "",
                    onBack = { navController.popBackStack() },
                    onMyList = { navController.navigate(Routes.MY_LIST) },
                    onRecordings = { navController.navigate(Routes.RECORDINGS) },
                    onDownloads = { navController.navigate(Routes.DOWNLOADS) },
                    onSettings = { navController.navigate(Routes.SETTINGS) },
                    onSignOut = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(Routes.RECORDINGS) {
                val vm = containerViewModel { com.denham.skyline.ui.catchup.CatchUpViewModel(it) }
                com.denham.skyline.ui.catchup.CatchUpScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onPlay = { url, title -> playVod(VodPlaybackRequest(url, title)) },
                )
            }

            composable(Routes.MY_LIST) {
                val vm = containerViewModel { MyListViewModel(it) }
                MyListScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onPlayChannel = { navController.navigateToLivePlayer(it) },
                    onOpenMovie = { navController.navigate(Routes.movieDetail(it.streamId)) },
                )
            }

            composable(Routes.DOWNLOADS) {
                val vm = containerViewModel { DownloadsViewModel(it) }
                DownloadsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onPlay = { url, title -> playVod(VodPlaybackRequest(url, title)) },
                )
            }

            composable(Routes.SETTINGS) {
                val vm = containerViewModel { SettingsViewModel(it) }
                val homeVm = containerViewModel { com.denham.skyline.ui.home.HomeViewModel(it) }
                SettingsScreen(
                    viewModel = vm,
                    homeViewModel = homeVm,
                    onBack = { navController.popBackStack() },
                    onSignedOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onCustomizeCategories = { navController.navigate(Routes.categoryCustomization("David")) },
                    onManageYouTube = { navController.navigate(Routes.YOUTUBE_SUBSCRIPTIONS) },
                )
            }

            composable(
                Routes.CATEGORY_CUSTOMIZATION,
                arguments = listOf(navArgument("familyMember") { type = NavType.StringType }),
            ) { entry ->
                val familyMember = entry.arguments?.getString("familyMember") ?: return@composable
                val homeVm = containerViewModel { com.denham.skyline.ui.home.HomeViewModel(it) }
                CategoryCustomizationScreen(
                    viewModel = homeVm,
                    familyMemberName = familyMember,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.YOUTUBE_SUBSCRIPTIONS) {
                val vm = containerViewModel { YouTubeSubscriptionViewModel(it) }
                YouTubeSubscriptionScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                Routes.PLAYER_LIVE,
                arguments = listOf(navArgument("streamId") { type = NavType.IntType }),
            ) { entry ->
                val streamId = entry.arguments?.getInt("streamId") ?: return@composable
                val vm = containerViewModel { PlayerViewModel(it) }
                androidx.compose.runtime.LaunchedEffect(streamId) {
                    val channel = container.db.channelDao().byId(streamId)
                    if (channel != null && vm.state.value.currentChannel?.streamId != streamId) {
                        vm.playChannel(channel)
                    }
                }
                PlayerHost(vm, container, isInPip, navController)
            }

            composable(Routes.PLAYER_VOD) {
                val vm = containerViewModel { PlayerViewModel(it) }
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    container.pendingVodPlayback?.let { request ->
                        container.pendingVodPlayback = null
                        vm.playVod(request.url, request.title, request.imageUrl, request.resumeFromMs)
                    }
                }
                PlayerHost(vm, container, isInPip, navController)
            }

            composable(Routes.PLAYER_YOUTUBE) {
                // Deliberately not PlayerHost/ExoPlayer: YouTube serves a web
                // page, not a stream, so it plays via the official embed.
                // The request is read once and kept, so a recomposition
                // (e.g. rotation) doesn't lose the video being played.
                val request = remember { container.pendingYoutubePlayback }
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    container.pendingYoutubePlayback = null
                }
                if (request == null) {
                    navController.popBackStack()
                } else {
                    YouTubePlayerScreen(
                        videoId = request.videoId,
                        onBack = { navController.popBackStack() },
                    )
                }
            }

            composable(
                Routes.MOVIE_DETAIL,
                arguments = listOf(navArgument("movieId") { type = NavType.IntType }),
            ) { entry ->
                val movieId = entry.arguments?.getInt("movieId") ?: return@composable
                val vm = containerViewModel { MovieDetailViewModel(it, movieId) }
                MovieDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onPlay = { url, title, image ->
                        playVod(VodPlaybackRequest(url, title, image))
                    },
                )
            }

            composable(
                Routes.SERIES_DETAIL,
                arguments = listOf(navArgument("seriesId") { type = NavType.IntType }),
            ) { entry ->
                val seriesId = entry.arguments?.getInt("seriesId") ?: return@composable
                val vm = containerViewModel { SeriesDetailViewModel(it, seriesId) }
                SeriesDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onPlay = { url, title, image ->
                        playVod(VodPlaybackRequest(url, title, image))
                    },
                )
            }
        }
    }
}

@UnstableApi
@Composable
private fun PlayerHost(
    vm: PlayerViewModel,
    container: AppContainer,
    isInPip: Boolean,
    navController: NavHostController,
) {
    androidx.compose.runtime.DisposableEffect(Unit) {
        container.playerScreenVisible = true
        onDispose {
            container.playerScreenVisible = false
            // Leaving the player (not into PiP): free the provider connection.
            if (!container.inPipMode) vm.stop()
        }
    }
    PlayerScreen(
        viewModel = vm,
        isInPip = isInPip,
        onBack = { navController.popBackStack() },
        onPlayChannel = { navController.navigateToLivePlayer(it) },
        onOpenMovie = { navController.navigate(Routes.movieDetail(it.streamId)) },
    )
}

@UnstableApi
private fun NavHostController.navigateToLivePlayer(channel: ChannelEntity) {
    navigate(Routes.playerLive(channel.streamId)) { launchSingleTop = true }
}
