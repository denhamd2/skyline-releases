package com.denham.skyline.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denham.skyline.data.repo.UpdateState
import com.denham.skyline.ui.theme.SkyPalette

@Composable
fun UpdatePrompt(
    viewModel: UpdateViewModel,
    buildTime: String,
) {
    val updateState by viewModel.updateState.collectAsState()

    when (val state = updateState) {
        is UpdateState.Available -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismiss() },
                title = {
                    Text("Update Available")
                },
                text = {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Version: ${state.version}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                        Text(
                            state.notes,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = SkyPalette.TextSecondary,
                            maxLines = 10,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "Current: $buildTime",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = SkyPalette.TextSecondary,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.downloadAndInstall(state.version) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkyPalette.Accent,
                        ),
                    ) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text("Download & Install")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismiss() }) {
                        Text("Later")
                    }
                },
            )
        }

        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = { /* Block dismissal during download */ },
                title = { Text("Downloading Update") },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "${state.progress}%",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {},
            )
        }

        is UpdateState.Downloaded -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismiss() },
                title = { Text("Ready to Install") },
                text = {
                    Text(
                        "Update has been downloaded. The installer will open next.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismiss() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkyPalette.Accent,
                        ),
                    ) {
                        Text("Got It")
                    }
                },
            )
        }

        is UpdateState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismiss() },
                title = { Text("Update Failed") },
                text = {
                    Text(
                        state.message,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = SkyPalette.TextSecondary,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismiss() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkyPalette.Accent,
                        ),
                    ) {
                        Text("Dismiss")
                    }
                },
            )
        }

        else -> {}
    }
}
