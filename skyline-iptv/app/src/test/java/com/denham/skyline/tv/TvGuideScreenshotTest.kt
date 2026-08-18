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
import com.denham.skyline.data.db.EpgProgrammeEntity
import com.denham.skyline.data.db.SkylineDatabase
import com.denham.skyline.di.AppContainer
import com.denham.skyline.ui.guide.GuideViewModel
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
import java.util.Calendar

/**
 * [AppContainer] test double for Robolectric, identical in shape to
 * `ScreenshotAppContainer` in `HomeScreenshotTest.kt`/`GuideScreenshotTest.kt`:
 * an in-memory Room DB in place of the real `skyline.db` file, and an
 * OkHttpClient that fails every request immediately rather than attempting a
 * real socket connection (this sandbox, and CI, may have no route to an
 * Xtream portal at all -- letting a request hang on DNS/connect would make
 * the test flaky or slow rather than fail fast). Duplicated here rather than
 * shared/extracted because the original is file-private in each case.
 *
 * As with the phone Guide test: `GuideViewModel.init` unconditionally calls
 * `guideRepository.refreshIfStale()` -> `importXmltv()`, which fails fast
 * with a caught `IOException` from the disabled `OkHttpClient` (see
 * `GuideRepository.importXmltv`) rather than crashing or hanging, and never
 * reaches the `db.guideDao().clear()` call that would wipe the seeded
 * programmes below.
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
                Interceptor { throw IOException("network disabled in TvGuideScreenshotTest") }
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
 * Renders [TvGuideScreen] -- the TV-shaped counterpart of the phone
 * [com.denham.skyline.ui.guide.GuideScreen] covered by `GuideScreenshotTest`
 * -- with the same populated-day seed shape, so `ux-design` has a real TV
 * render of the fixed 2-hour-window grid (channel logos, D-pad "Earlier /
 * Now / Later" controls, the accent "now" line) to ground edits in.
 *
 * Uses the same [GuideViewModel] as phone -- there's no TV-specific
 * ViewModel here, only a TV-shaped composable reading the same `state`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w1280dp-h720dp-land-xhdpi")
@UnstableApi
class TvGuideScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tvGuideScreen_populatedDay() {
        val context = RuntimeEnvironment.getApplication()
        val container = ScreenshotAppContainer(context)
        seedSampleData(container)

        val viewModel = GuideViewModel(container)

        compose.setContent {
            SkylineTheme {
                TvGuideScreen(
                    viewModel = viewModel,
                    onPlayChannel = {},
                )
            }
        }

        // Same wait rationale as GuideScreenshotTest: `state.loading` only
        // flips to `false` once `loadDay` has returned with real channels
        // and programmes.
        compose.waitUntil(timeoutMillis = 10_000) {
            val state = viewModel.state.value
            !state.loading && state.channels.isNotEmpty() && state.programmes.isNotEmpty()
        }
        compose.waitForIdle()

        // Same relative-path convention as HomeScreenshotTest/ScreenshotTests
        // -- resolved from the `app` module's test-task working directory.
        compose.onRoot().captureRoboImage("../../docs/skyline-screenshots/tv_guide.png")
    }
}

/**
 * Same shape as `GuideScreenshotTest`'s `seedSampleData`: a couple of live
 * categories, a handful of channels across them, and full-day
 * [EpgProgrammeEntity] rows (not `EpgNowNextEntity`, which only backs Home's
 * now/next tiles). Every channel gets contiguous blocks spanning the whole
 * day so `hasGuideData()` is satisfied and the grid renders regardless of
 * what time this test runs, plus one explicit programme anchored to the
 * real "now" so the accent-highlighted current-programme block is
 * guaranteed to render.
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
