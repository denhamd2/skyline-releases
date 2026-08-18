package com.denham.skyline.tv

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.media3.common.util.UnstableApi
import androidx.room.Room
import com.denham.skyline.core.PortalAddress
import com.denham.skyline.core.XtreamAccount
import com.denham.skyline.data.db.CategoryEntity
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.data.db.ContentType
import com.denham.skyline.data.db.EpgNowNextEntity
import com.denham.skyline.data.db.MovieEntity
import com.denham.skyline.data.db.SkylineDatabase
import com.denham.skyline.data.prefs.LastPlayed
import com.denham.skyline.di.AppContainer
import com.denham.skyline.ui.home.HomeViewModel
import com.denham.skyline.ui.theme.SkylineTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.IOException

/**
 * [AppContainer] test double for Robolectric, identical in shape to
 * `ScreenshotAppContainer` in `HomeScreenshotTest.kt`: an in-memory Room DB
 * in place of the real `skyline.db` file, and an OkHttpClient that fails
 * every request immediately rather than attempting a real socket connection
 * (this sandbox, and CI, may have no route to an Xtream portal at all --
 * letting a request hang on DNS/connect would make the test flaky or slow
 * rather than fail fast). Duplicated here rather than shared/extracted
 * because the original is file-private -- see the reasoning on
 * `ScreenshotAppContainer` itself for why each of these seams exists.
 *
 * The account is seeded directly via `seedAccountForTesting` rather than
 * `signIn`, because `signIn` writes through `CredentialStore`, which needs
 * the Android Keystore -- unavailable under Robolectric.
 */
@UnstableApi
private class ScreenshotAppContainer(private val context: Context) : AppContainer(context) {

    override fun createDatabase(): SkylineDatabase =
        Room.inMemoryDatabaseBuilder(context, SkylineDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    override fun createOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                Interceptor { throw IOException("network disabled in TvHomeScreenshotTest") }
            )
            .build()

    init {
        seedAccountForTesting(
            XtreamAccount(
                portal = PortalAddress(scheme = "http", host = "screenshot.invalid", port = 80),
                username = "preview",
                password = "preview",
            )
        )
    }
}

/**
 * Renders [TvHomeScreen] -- the TV-shaped counterpart of the phone
 * [com.denham.skyline.ui.home.HomeScreen] covered by `HomeScreenshotTest` --
 * in the David-selected member-chip state, using the same [HomeViewModel]
 * and the same seed shape, so `ux-design` has a real TV render to ground
 * edits to this screen in.
 *
 * Note for the design review: TV currently has no UI of its own to choose a
 * family member (no equivalent of the phone's member-chip row) -- selecting
 * "David" here is done directly on the ViewModel, standing in for a control
 * that doesn't exist yet on TV. That's a known gap being surfaced by this
 * screenshot, not something this test is fixing.
 *
 * What this deliberately does NOT cover: the David-only "Football" section
 * on phone Home has no TV equivalent read here either way; and TV's own
 * "My list" rail (driven by `favoriteChannels`, the `favorites` table --
 * distinct from the phone's per-member pinned-channel rail) stays empty
 * because the seed data below only populates category-tab pins, not
 * favorites, matching what `HomeScreenshotTest`'s seed already does.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w1280dp-h720dp-land-xhdpi")
@UnstableApi
class TvHomeScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tvHomeScreen_davidSelected() {
        val context = RuntimeEnvironment.getApplication()
        val container = ScreenshotAppContainer(context)
        seedSampleData(container)

        val viewModel = HomeViewModel(container)
        // Pre-select David's tab directly on the ViewModel -- see the class
        // doc comment above on why TV has no chip UI to do this via gesture.
        viewModel.selectFamilyMember("David")

        compose.setContent {
            SkylineTheme {
                TvHomeScreen(
                    viewModel = viewModel,
                    onPlayChannel = {},
                    onPlayMovie = {},
                    onResume = {},
                    onPlayYoutube = { _, _, _ -> },
                )
            }
        }

        // Same rationale as HomeScreenshotTest: the rails driving this screen
        // arrive asynchronously off Room's Flow queries, so wait for them
        // rather than capturing whatever's landed by the first frame.
        // TvHomeScreen itself reads recentMovies, popularChannels and
        // categoryRails unconditionally (favoriteChannels/continueWatching
        // are read too, but both render conditionally and are allowed to
        // stay empty here).
        compose.waitUntil(timeoutMillis = 10_000) {
            viewModel.recentMovies.value.isNotEmpty() &&
                viewModel.popularChannels.value.isNotEmpty() &&
                viewModel.categoryRails.value.isNotEmpty()
        }
        compose.waitForIdle()

        // Same relative-path convention as HomeScreenshotTest/ScreenshotTests
        // -- resolved from the `app` module's test-task working directory.
        compose.onRoot().captureRoboImage("../../docs/skyline-screenshots/tv_home.png")
    }
}

/**
 * Same shape as `HomeScreenshotTest`'s `seedSampleData`: a couple of
 * sport/wrestling categories David matches by keyword (see
 * `familyKeywordDefaults` in `HomeScreen.kt`), one channel individually
 * pinned to David outside those categories, now/next EPG for the tiles, and
 * one recent movie for the hero. All artwork URLs are left null -- Coil
 * would otherwise attempt real image loads that this sandbox/CI has no
 * network path for; `ArtworkImage` renders a placeholder icon for a null
 * URL.
 */
private fun seedSampleData(container: AppContainer) = runBlocking {
    val db = container.db

    db.categoryDao().insertAll(
        listOf(
            CategoryEntity(ContentType.LIVE, "sport", "Sky Sports Football", 0),
            CategoryEntity(ContentType.LIVE, "wrestling", "WWE Network", 1),
            CategoryEntity(ContentType.LIVE, "news", "Sky Sports News", 2),
        )
    )

    val channels = listOf(
        ChannelEntity(
            streamId = 101, num = 1, name = "Sky Sports Main Event",
            icon = null, epgChannelId = null, categoryId = "sport",
            tvArchive = false, added = null,
        ),
        ChannelEntity(
            streamId = 102, num = 2, name = "Sky Sports Premier League",
            icon = null, epgChannelId = null, categoryId = "sport",
            tvArchive = false, added = null,
        ),
        ChannelEntity(
            streamId = 201, num = 3, name = "WWE Network",
            icon = null, epgChannelId = null, categoryId = "wrestling",
            tvArchive = false, added = null,
        ),
        ChannelEntity(
            streamId = 202, num = 4, name = "AEW Dynamite",
            icon = null, epgChannelId = null, categoryId = "wrestling",
            tvArchive = false, added = null,
        ),
        // David's individually pinned channel -- deliberately in a category
        // ("news") he hasn't selected, so the pinned rail is demonstrably
        // independent of the category rails, matching the real behaviour.
        ChannelEntity(
            streamId = 501, num = 5, name = "MUTV",
            icon = null, epgChannelId = null, categoryId = "news",
            tvArchive = false, added = null,
        ),
    )
    db.channelDao().insertChunk(channels)

    val now = System.currentTimeMillis()
    db.epgDao().upsert(
        EpgNowNextEntity(
            streamId = 101, nowTitle = "Arsenal v Chelsea", nowDescription = null,
            nowStart = now - 600_000, nowStop = now + 2_400_000,
            nextTitle = "Soccer Saturday", nextStart = now + 2_400_000, nextStop = now + 6_000_000,
            fetchedAt = now,
        )
    )
    db.epgDao().upsert(
        EpgNowNextEntity(
            streamId = 501, nowTitle = "Man Utd Classics", nowDescription = null,
            nowStart = now - 300_000, nowStop = now + 1_800_000,
            nextTitle = null, nextStart = null, nextStop = null,
            fetchedAt = now,
        )
    )

    db.movieDao().insertChunk(
        listOf(
            MovieEntity(
                streamId = 9001, name = "A Dark Song", icon = null, rating5 = 4.2,
                containerExtension = "mp4", categoryId = "movies", added = now,
            )
        )
    )

    // David's tab: two categories (sport, wrestling) plus the individually
    // pinned MUTV -- one write, matching `saveMemberTab`'s real call shape.
    container.categoryPreferencesStore.setMemberTab(
        accountId = "preview",
        userName = "David",
        categoryIds = listOf("sport", "wrestling"),
        streamIds = listOf(501),
    )

    // Continue Watching, shown regardless of the selected member.
    container.historyStore.record(
        LastPlayed(
            type = "vod",
            id = 9001,
            title = "A Dark Song",
            imageUrl = null,
            playbackUrl = "http://screenshot.invalid/vod/9001",
            positionMs = 1_800_000,
            durationMs = 5_400_000,
        )
    )
}
