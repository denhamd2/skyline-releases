package com.denham.skyline.ui.home

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
import com.denham.skyline.ui.theme.SkylineTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
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
 * [AppContainer] test double for Robolectric: an in-memory Room DB in place
 * of the real `skyline.db` file, and an OkHttpClient that fails every
 * request immediately rather than attempting a real socket connection (this
 * sandbox, and CI, may have no route to an Xtream portal at all -- letting a
 * request hang on DNS/connect would make the test flaky or slow rather than
 * fail fast). [AppContainer.createDatabase]/[createOkHttpClient] exist
 * specifically as this seam; see the comment on [AppContainer] itself.
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
                Interceptor { throw IOException("network disabled in HomeScreenshotTest") }
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
 * Renders the assembled [HomeScreen] -- not just isolated components (that's
 * [com.denham.skyline.ui.PhoneComponentScreenshots]) -- in the David-selected
 * member-chip state, so `ux-design` has a real screenshot to ground edits to
 * this screen in instead of falling back to reading the Compose source.
 *
 * What this deliberately does NOT cover: the David-only "Football" section
 * (`FootballSectionState`) stays `Hidden` here because it's gated on
 * `BuildConfig.FOOTBALL_DATA_API_KEY`, which is empty in a local/test build
 * (real value only exists as a CI secret) -- there is no way to force it on
 * from this test without either baking a key into the build (never, per the
 * "nothing secret in the APK" rule) or a further seam in `HomeViewModel`.
 * If a design task specifically concerns the Football section, that gap
 * still needs a source read or a follow-up seam, not this screenshot.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel7)
@UnstableApi
class HomeScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun homeScreen_davidSelected() {
        val context = RuntimeEnvironment.getApplication()
        val container = ScreenshotAppContainer(context)
        seedSampleData(container)

        val viewModel = HomeViewModel(container)
        // Pre-select David's tab directly on the ViewModel -- equivalent to
        // tapping the "David" filter chip, without needing a UI gesture.
        viewModel.selectFamilyMember("David")

        compose.setContent {
            SkylineTheme {
                HomeScreen(
                    viewModel = viewModel,
                    updateViewModel = null,
                    onPlayChannel = {},
                    onOpenMovie = {},
                    onOpenSeries = {},
                    onResume = {},
                    onOpenSearch = {},
                    onOpenAccount = {},
                    onViewAllLive = {},
                    onViewAllMovies = {},
                    onViewAllSeries = {},
                )
            }
        }

        // The category rails, pinned channels and hero movie all arrive
        // asynchronously off Room's Flow queries (real background threads
        // under Robolectric, not virtual time) -- wait for them rather than
        // capturing whatever happens to have landed by the first frame.
        compose.waitUntil(timeoutMillis = 10_000) {
            viewModel.pinnedChannels.value.isNotEmpty() &&
                viewModel.categoryRails.value.isNotEmpty() &&
                viewModel.recentMovies.value.isNotEmpty()
        }
        compose.waitForIdle()

        // Resolved relative to the `app` module directory (Gradle's default
        // test-task working directory), matching where
        // `docs/skyline-screenshots/*.png` already live -- verified against
        // Roborazzi's default `relativePathFromCurrentDirectory` file-path
        // strategy (no `roborazzi.record.filePathStrategy` / `outputDir`
        // override is set in this project). Same `../../docs/skyline-
        // screenshots/...` path as `PhoneComponentScreenshots`/
        // `TvComponentScreenshots` in `ScreenshotTests.kt` -- both resolve
        // from the same module working directory.
        compose.onRoot().captureRoboImage("../../docs/skyline-screenshots/home_david.png")
    }
}

/**
 * A small, realistic catalogue: a couple of sport/wrestling categories (David
 * matches "football"/"soccer"/"sport"/"wwe"/"wrestling" by keyword, see
 * `familyKeywordDefaults` in [HomeScreen.kt]), one channel individually
 * pinned to David outside those categories, now/next EPG for the tiles, and
 * one recent movie for the hero. All artwork URLs are left null -- Coil would
 * otherwise attempt real image loads that this sandbox/CI has no network
 * path for; [ArtworkImage] renders a placeholder icon for a null URL, which
 * is also what the existing component-gallery screenshots already do.
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
