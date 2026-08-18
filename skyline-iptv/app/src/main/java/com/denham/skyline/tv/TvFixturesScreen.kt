package com.denham.skyline.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.denham.skyline.core.Fixture
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.ui.components.enterReveal
import com.denham.skyline.ui.components.revealDelay
import com.denham.skyline.ui.theme.SkyPalette
import com.denham.skyline.ui.theme.SkySpacing

/**
 * TV counterpart to the phone `FixturesScreen` (ui/fixtures/FixturesScreen.kt)
 * -- same vertical LazyColumn shape, full-width [TvFixtureCard]s, not a
 * grid. Reached from the TV Football rail's "View all" and closed via the
 * system Back key (handled by the caller's BackHandler, same pattern as
 * TvPlayerOverlay).
 */
@Composable
fun TvFixturesScreen(
    fixtures: List<Fixture>,
    fixtureChannels: Map<Long, List<ChannelEntity>>,
    onPlayChannel: (ChannelEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SkyPalette.tvBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = SkySpacing.m)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .tvClickable(onClick = onBack)
                    .padding(6.dp),
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = SkyPalette.TextPrimary,
                )
            }
            Text(
                "Football Fixtures",
                style = MaterialTheme.typography.headlineSmall,
                color = SkyPalette.TextPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (fixtures.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No fixtures available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyPalette.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(SkySpacing.s),
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = SkySpacing.xl),
            ) {
                items(
                    count = fixtures.size,
                    key = { fixtures[it].id },
                ) { index ->
                    val fixture = fixtures[index]
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
                        isSpotlight = false,
                        modifier = Modifier.enterReveal(revealDelay(index)),
                    )
                }
            }
        }
    }
}
