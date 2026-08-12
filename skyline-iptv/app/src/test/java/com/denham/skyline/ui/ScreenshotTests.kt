package com.denham.skyline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.denham.skyline.tv.TvLandscapeCard
import com.denham.skyline.tv.TvNavTab
import com.denham.skyline.tv.TvPosterCard
import com.denham.skyline.ui.components.ChannelCard
import com.denham.skyline.ui.components.LiveBadge
import com.denham.skyline.ui.components.LiveNowRow
import com.denham.skyline.ui.components.PillButton
import com.denham.skyline.ui.components.PosterCard
import com.denham.skyline.ui.components.ProviderBadge
import com.denham.skyline.ui.components.SectionHeader
import com.denham.skyline.ui.components.SkylineWordmark
import com.denham.skyline.ui.theme.SkyPalette
import com.denham.skyline.ui.theme.SkylineTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the design-system pieces on the JVM and captures PNGs directly
 * into `docs/skyline-screenshots/` -- a tracked path, not
 * `build/outputs/roborazzi/`. `build/` is gitignored, so a path under it can
 * never be a real committed golden for `verifyRoborazziDebug` to compare
 * against; that was true of this file from the start, silently, since
 * nothing had ever run in verify mode to notice.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel7)
class PhoneComponentScreenshots {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun componentGallery() {
        compose.setContent {
            SkylineTheme {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .background(SkyPalette.Canvas)
                        .padding(16.dp)
                        .fillMaxWidth(),
                ) {
                    SkylineWordmark()
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LiveBadge()
                        ProviderBadge("sky sports")
                        ProviderBadge("premier league")
                    }
                    PillButton("Resume", onClick = {})
                    SectionHeader("Continue Watching", onViewAll = {})
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChannelCard(
                            name = "Sports One HD",
                            imageUrl = null,
                            subtitle = "Arsenal v Chelsea",
                            onClick = {},
                        )
                        PosterCard(
                            title = "A Dark Song",
                            imageUrl = null,
                            onClick = {},
                        )
                    }
                    LiveNowRow(
                        title = "Arsenal v Liverpool",
                        subtitle = "Premier League",
                        imageUrl = null,
                        onClick = {},
                    )
                }
            }
        }
        compose.onRoot().captureRoboImage("../../docs/skyline-screenshots/phone_components.png")
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w1280dp-h720dp-land-xhdpi")
class TvComponentScreenshots {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tvGallery() {
        compose.setContent {
            SkylineTheme {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .background(SkyPalette.Canvas)
                        .padding(24.dp)
                        .fillMaxWidth(),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkylineWordmark()
                        TvNavTab("Home", active = true, onClick = {})
                        TvNavTab("TV guide", active = false, onClick = {})
                        TvNavTab("Browse", active = false, onClick = {})
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        TvLandscapeCard(
                            title = "Live: Arsenal v Chelsea",
                            subtitle = "Sports One HD",
                            imageUrl = null,
                            onClick = {},
                        )
                        TvPosterCard(
                            title = "Dune: Part Two",
                            imageUrl = null,
                            onClick = {},
                            modifier = Modifier.width(140.dp),
                        )
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("../../docs/skyline-screenshots/tv_components.png")
    }
}
