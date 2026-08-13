package com.denham.skyline.ui.fixtures

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.denham.skyline.core.Fixture
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.ui.components.FixtureCard
import com.denham.skyline.ui.components.enterReveal
import com.denham.skyline.ui.components.revealDelay
import com.denham.skyline.ui.theme.SkyPalette
import com.denham.skyline.ui.theme.SkySpacing

/**
 * Full-screen vertical list of football fixtures.
 * Reuses FixtureCard at full-width in a LazyColumn with staggered reveal motion.
 * Phone-only; TV support (D-pad focus) deferred to phase 2.
 */
@Composable
fun FixturesScreen(
    fixtures: List<Fixture>,
    fixtureChannels: Map<String, List<ChannelEntity>>,
    onPlayChannel: (ChannelEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SkyPalette.Canvas)
    ) {
        // Header with back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SkySpacing.m, vertical = SkySpacing.m)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = SkyPalette.TextPrimary
                )
            }
            Text(
                "Football Fixtures",
                style = MaterialTheme.typography.titleLarge,
                color = SkyPalette.TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Fixtures list
        if (fixtures.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(SkySpacing.gutter),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No fixtures available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyPalette.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SkyPalette.Canvas)
                    .padding(horizontal = SkySpacing.gutter),
                verticalArrangement = Arrangement.spacedBy(SkySpacing.s),
                contentPadding = PaddingValues(vertical = SkySpacing.xl)
            ) {
                items(
                    count = fixtures.size,
                    key = { fixtures[it].id }
                ) { index ->
                    FixtureCard(
                        competition = fixtures[index].competition,
                        homeTeam = fixtures[index].homeTeam,
                        awayTeam = fixtures[index].awayTeam,
                        homeCrestUrl = fixtures[index].homeCrestUrl,
                        awayCrestUrl = fixtures[index].awayCrestUrl,
                        status = fixtures[index].status,
                        channels = fixtureChannels[fixtures[index].id] ?: emptyList(),
                        onPlayChannel = onPlayChannel,
                        width = null,  // Full-width, not rail-constrained
                        isSpotlight = false,  // All cards flat, no spotlight variant
                        modifier = Modifier.enterReveal(revealDelay(index))
                    )
                }
            }
        }
    }
}
