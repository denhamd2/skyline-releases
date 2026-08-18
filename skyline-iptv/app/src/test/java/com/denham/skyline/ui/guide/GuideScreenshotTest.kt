package com.denham.skyline.ui.guide

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
import com.denham.skyline.data.db.EpgProgrammeEntity
import com.denham.skyline.data.db.SkylineDatabase
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
import java.util.Calendar

/**
 * [AppContainer] test double for Robolectric, identical in shape to
 * `ScreenshotAppContainer` in `HomeScreenshotTest.kt`: an in-memory Room DB
 * in place of the real `skyline.db` file, and an OkHttpClient that fails
 * every request immediately rather than attempting a real socket connection
 * (this sandbox, and CI, may have no route to an Xtream portal at all --
 * letting a request hang on DNS/connect would make the test flaky or slow
 * rather than fail fast). Duplicated here rather than shared/extracted
 * because the original is file-private.
 *
 * This matters more for Guide than it did for Home: `GuideViewModel.init`
 * unconditionally calls `guideRepository.refreshIfStale()`, which (with a
 * never-imported `SettingsStore`) always attempts `importXmltv()`. Read
 * through `GuideRepository.importXmltv`: it builds its channel map from the
 * local DB first (fine, no network), then calls `api().xmltvRaw()` inside
 * the same try/catch that wraps the whole import -- the disabled
 * OkHttpClient's interceptor throws `IOException` there, which is caught
 * and turned into `GuideImportState.Failed`, not a crash or a hang. The
 * `db.guideDao().clear()` call that would otherwise wipe the seeded
 * programmes below never runs, because it lives inside the
 * `api().xmltvRaw().use { ... }` block, which the thrown `IOException`
 * never lets execution reach.
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
                Interceptor { throw IOException("network disabled in GuideScreenshotTest") }
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
 * Renders the assembled phone [GuideScreen] -- not just isolated components
 * -- with a populated day of full programme data, so `ux-design` has a real
 * screenshot of the grid state (frozen channel column, shared horizontal
 * time scroll, "now" line) to ground edits to this screen in.
 *
 * Deliberately covers the populated-grid state, not `NoGuideFallback` (the
 * "provider has no xmltv guide" list view) -- that's a genuinely different
 * screen shape and would need its own test/seed if it becomes a design
 * target.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel7)
@UnstableApi
class GuideScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun guideScreen_populatedDay() {
        val context = RuntimeEnvironment.getApplication()
        val container = ScreenshotAppContainer(context)
        seedSampleData(container)

        val viewModel = GuideViewModel(container)

        compose.setContent {
            SkylineTheme {
                GuideScreen(
                    viewModel = viewModel,
                    onPlayChannel = {},
                )
            }
        }

        // `GuideUiState.loading` starts `true` and the grid isn't shown at
        // all until it flips to `false` after `loadDay` returns (see
        // `GuideViewModel.loadDay`) -- wait for that plus non-empty channels
        // and programmes so the capture is the real grid, not the loading
        // spinner or (if seeding were wrong) `NoGuideFallback`.
        compose.waitUntil(timeoutMillis = 10_000) {
            val state = viewModel.state.value
            !state.loading && state.channels.isNotEmpty() && state.programmes.isNotEmpty()
        }
        compose.waitForIdle()

        // Same relative-path convention as HomeScreenshotTest/ScreenshotTests
        // -- resolved from the `app` module's test-task working directory.
        compose.onRoot().captureRoboImage("../../docs/skyline-screenshots/guide_phone.png")
    }
}

/**
 * A small, realistic lineup for the guide grid: a couple of live categories,
 * a handful of channels across them, and full-day [EpgProgrammeEntity] rows
 * (not [com.denham.skyline.data.db.EpgNowNextEntity] -- that table backs
 * Home's now/next tiles only; the guide grid reads the full schedule via
 * `GuideDao`/`GuideRepository.programmesFor`). Every channel gets a few
 * contiguous blocks spanning the whole day so `hasGuideData()` (a plain
 * `COUNT(*) FROM epg_programmes > 0`) is satisfied and the grid has
 * something to draw regardless of what time this test happens to run at,
 * plus one explicit programme anchored to the real "now" so the grid's
 * highlighted current-programme block is guaranteed to render rather than
 * relying on a coincidental overlap with a fixed day-relative block.
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
            icon = null, epgChannelId = "skysportsmain.uk", categoryId = "sport",
            tvArchive = false, added = null,
        ),
        ChannelEntity(
            streamId = 102, num = 2, name = "Sky Sports Premier League",
            icon = null, epgChannelId = "skysportspl.uk", categoryId = "sport",
            tvArchive = false, added = null,
        ),
        ChannelEntity(
            streamId = 201, num = 3, name = "WWE Network",
            icon = null, epgChannelId = "wwenetwork.uk", categoryId = "wrestling",
            tvArchive = false, added = null,
        ),
        ChannelEntity(
            streamId = 501, num = 4, name = "Sky Sports News",
            icon = null, epgChannelId = "skysportsnews.uk", categoryId = "news",
            tvArchive = false, added = null,
        ),
    )
    db.channelDao().insertChunk(channels)

    // Matches `GuideViewModel.init`'s own day-boundary calculation (default
    // Calendar, floored to midnight) so the seeded programmes fall inside
    // the exact window `loadDay` queries for `selectedDay`.
    val dayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val hour = 60 * 60_000L
    val now = System.currentTimeMillis()

    val programmes = mutableListOf<EpgProgrammeEntity>()
    // Four 6-hour blocks per channel spanning the full day, so the grid is
    // populated end to end no matter what time the test runs.
    listOf(
        101 to listOf("Sunrise Sport", "Live: Arsenal v Chelsea", "Soccer Saturday", "Late Kick Off"),
        102 to listOf("Premier League Preview", "Live: Man City v Spurs", "Match of the Day", "PL Review"),
        201 to listOf("WWE Classics", "WWE Raw Highlights", "SmackDown Replay", "NXT Late Night"),
        501 to listOf("Morning Headlines", "Sports News Live", "Transfer Deadline Day", "Evening Round-up"),
    ).forEach { (streamId, titles) ->
        titles.forEachIndexed { block, title ->
            programmes += EpgProgrammeEntity(
                channelStreamId = streamId,
                startMs = dayStart + block * 6 * hour,
                stopMs = dayStart + (block + 1) * 6 * hour,
                title = title,
                description = null,
                imageUrl = null,
            )
        }
    }
    // Explicit "now" programme for the hero channel, guaranteed to overlap
    // the current time regardless of the fixed blocks above.
    programmes += EpgProgrammeEntity(
        channelStreamId = 101,
        startMs = now - 30 * 60_000L,
        stopMs = now + 90 * 60_000L,
        title = "Live: Arsenal v Chelsea",
        description = "Premier League",
        imageUrl = null,
    )
    db.guideDao().insertChunk(programmes)
}
