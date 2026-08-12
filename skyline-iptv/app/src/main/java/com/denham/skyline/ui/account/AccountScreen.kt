package com.denham.skyline.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.denham.skyline.ui.theme.SkyPalette

/** Account page per the mock: avatar card + settings-style menu list. */
@Composable
fun AccountScreen(
    username: String,
    onBack: () -> Unit,
    onMyList: () -> Unit,
    onRecordings: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit,
    onSignOut: () -> Unit,
) {
    var showAbout by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.Canvas)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text("Account", style = MaterialTheme.typography.headlineMedium)
        }

        // Profile card.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SkyPalette.Surface)
                .clickable(onClick = onSettings)
                .padding(16.dp),
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .background(SkyPalette.Accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    username.take(2).uppercase().ifBlank { "SK" },
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(username.ifBlank { "Sky Go" }, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Manage account",
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyPalette.TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SkyPalette.Surface),
        ) {
            MenuRow(Icons.Default.List, "My List", onMyList)
            MenuRow(Icons.Default.Radio, "Recordings", onRecordings)
            MenuRow(Icons.Default.Download, "Downloads", onDownloads)
            MenuRow(Icons.Default.Settings, "Settings", onSettings)
            MenuRow(Icons.Default.HelpOutline, "Help & Info") { showAbout = true }
            MenuRow(Icons.AutoMirrored.Filled.Logout, "Sign Out", onSignOut)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Close") }
            },
            title = { Text("Sky Go") },
            text = {
                Text(
                    "A personal IPTV player for your own Xtream Codes subscription. " +
                        "Sky Go ships with no channels or content.\n\n" +
                        "Typeface: Manrope (SIL Open Font License). " +
                        "Playback: Media3/ExoPlayer with FFmpeg decoders (NextLib)."
                )
            },
        )
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(icon, null, tint = SkyPalette.Accent, modifier = Modifier.size(22.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
            tint = SkyPalette.TextMuted,
        )
    }
}
