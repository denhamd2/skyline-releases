package com.denham.skyline.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.denham.skyline.data.db.CategoryEntity
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.ui.home.HomeViewModel
import com.denham.skyline.ui.theme.SkyPalette
import kotlinx.coroutines.delay

@Composable
fun CategoryCustomizationScreen(
    viewModel: HomeViewModel,
    familyMemberName: String,
    onBack: () -> Unit,
) {
    val allCategories by viewModel.allCategories.collectAsState()
    var selectedCategoryIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Who is being edited is switchable in-screen. Settings navigated here with
    // a hard-coded "David", so the other three could not be configured at all.
    var currentMember by remember(familyMemberName) { mutableStateOf(familyMemberName) }

    // Individually pinned channels, kept as whole entities so the current pins
    // can be listed by name without a second lookup.
    var pinnedChannels by remember { mutableStateOf<List<ChannelEntity>>(emptyList()) }
    var channelQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<ChannelEntity>>(emptyList()) }

    // Seed from what this person already has saved. Without this the screen
    // always opened with nothing ticked, so reopening it and saving silently
    // replaced their existing choice with whatever was picked that time.
    LaunchedEffect(currentMember) {
        selectedCategoryIds = viewModel.loadFamilyMemberPreferences(currentMember).toSet()
        pinnedChannels = viewModel.loadPinnedChannels(currentMember)
    }

    // Debounced so each keystroke does not fire an FTS query.
    LaunchedEffect(channelQuery) {
        if (channelQuery.isBlank()) {
            searchResults = emptyList()
        } else {
            delay(250)
            searchResults = viewModel.searchChannels(channelQuery)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.Canvas),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Customize categories",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
        }

        // Pick whose categories to edit.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            listOf("David", "Anne", "Ava", "Sophie").forEach { member ->
                FilterChip(
                    selected = currentMember == member,
                    onClick = { currentMember = member },
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

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            // Individual channels first: pinning is how a single channel
            // reaches this person's tab without its whole category coming too.
            item {
                Text(
                    "Pinned channels",
                    style = MaterialTheme.typography.titleSmall,
                    color = SkyPalette.TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            items(pinnedChannels.size, key = { pinnedChannels[it].streamId }) { index ->
                val channel = pinnedChannels[index]
                ChannelPinRow(
                    name = channel.name,
                    isPinned = true,
                    onToggle = { pinnedChannels = pinnedChannels.filterNot { it.streamId == channel.streamId } },
                )
            }

            item {
                OutlinedTextField(
                    value = channelQuery,
                    onValueChange = { channelQuery = it },
                    label = { Text("Search channels to pin") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // Already-pinned hits are filtered out: they are listed above with
            // their own remove control, so showing them twice would let the
            // same channel be ticked in two places.
            val unpinnedResults = searchResults.filterNot { r ->
                pinnedChannels.any { it.streamId == r.streamId }
            }
            items(unpinnedResults.size, key = { "result_${unpinnedResults[it].streamId}" }) { index ->
                val channel = unpinnedResults[index]
                ChannelPinRow(
                    name = channel.name,
                    isPinned = false,
                    onToggle = { pinnedChannels = pinnedChannels + channel },
                )
            }

            item {
                Text(
                    "Categories (up to 5)",
                    style = MaterialTheme.typography.titleSmall,
                    color = SkyPalette.TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            items(allCategories.size) { index ->
                val category = allCategories[index]
                val isSelected = category.categoryId in selectedCategoryIds
                CategoryCheckboxRow(
                    category = category,
                    isSelected = isSelected,
                    onToggle = {
                        selectedCategoryIds = if (isSelected) {
                            selectedCategoryIds - category.categoryId
                        } else if (selectedCategoryIds.size < 5) {
                            selectedCategoryIds + category.categoryId
                        } else {
                            selectedCategoryIds
                        }
                    },
                )
            }
        }

        // Save button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Button(
                onClick = {
                    // One call, not two. Saving these separately raced: each
                    // did its own read-modify-write of the same stored value,
                    // so whichever finished last discarded the other's change.
                    viewModel.saveMemberTab(
                        currentMember,
                        selectedCategoryIds.toList(),
                        pinnedChannels.map { it.streamId },
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                // Pins alone are a valid tab, so categories are no longer
                // required -- "just MUTV and my YouTube" is a legitimate
                // setup. Saving with neither is what stays blocked.
                enabled = selectedCategoryIds.isNotEmpty() || pinnedChannels.isNotEmpty(),
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Save for $currentMember (${pinnedChannels.size} pinned, ${selectedCategoryIds.size}/5 categories)")
            }
        }
    }
}

/**
 * One channel in the pinning editor. Used for both the current pins (ticked,
 * tapping removes) and search results (unticked, tapping pins), so the two
 * lists behave identically.
 */
@Composable
private fun ChannelPinRow(
    name: String,
    isPinned: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Checkbox(
            checked = isPinned,
            onCheckedChange = { onToggle() },
        )
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
    }
}

@Composable
private fun CategoryCheckboxRow(
    category: CategoryEntity,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
        )
        Text(
            category.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
    }
}
