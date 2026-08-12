package com.denham.skyline.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.denham.skyline.core.LiveContainer
import com.denham.skyline.core.PlayerApiResponse
import com.denham.skyline.data.repo.AuthResult
import com.denham.skyline.di.AppContainer
import com.denham.skyline.ui.theme.SkyPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** Rolling release the CI pipeline refreshes on every change. */
private const val UPDATE_URL =
    "https://github.com/denhamd2/pm-agent-mirror/releases/download/skyline-latest/Skyline.apk"

@UnstableApi
class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings = container.settingsStore.current

    private val _accountInfo = MutableStateFlow<PlayerApiResponse?>(null)
    val accountInfo: StateFlow<PlayerApiResponse?> = _accountInfo

    init {
        viewModelScope.launch {
            val account = container.account.value ?: return@launch
            when (val result = container.authRepository.authenticate(account)) {
                is AuthResult.Success -> _accountInfo.value = result.response
                is AuthResult.Failure -> Unit
            }
        }
    }

    fun setUserAgent(value: String) {
        viewModelScope.launch { container.settingsStore.setUserAgent(value) }
    }

    fun setContainer(value: LiveContainer) {
        viewModelScope.launch { container.settingsStore.setPreferredContainer(value) }
    }

    fun setBackgroundAudio(value: Boolean) {
        viewModelScope.launch { container.settingsStore.setBackgroundAudio(value) }
    }

    fun resync() {
        viewModelScope.launch { container.contentRepository.syncAll(force = true) }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            container.signOut()
            onDone()
        }
    }
}

@UnstableApi
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    homeViewModel: com.denham.skyline.ui.home.HomeViewModel? = null,
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    onCustomizeCategories: () -> Unit = {},
    onManageYouTube: () -> Unit = {},
) {
    val settings by viewModel.settings.collectAsState()
    val accountInfo by viewModel.accountInfo.collectAsState()
    var uaDraft by remember { mutableStateOf(settings.userAgent) }

    LaunchedEffect(settings.userAgent) { uaDraft = settings.userAgent }

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
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("Account", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val user = accountInfo?.userInfo
            if (user != null) {
                SettingRow("Status", user.status.ifBlank { "Unknown" })
                SettingRow(
                    "Expires",
                    user.expDate?.let { DateFormat.getDateInstance().format(Date(it * 1000)) }
                        ?: "Unlimited",
                )
                SettingRow("Connections", "${user.activeConnections} of ${user.maxConnections} in use")
                if (user.isTrial) SettingRow("Plan", "Trial")
            } else {
                Text(
                    "Account details unavailable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyPalette.TextSecondary,
                )
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = SkyPalette.SurfaceElevated)
            Spacer(Modifier.height(20.dp))

            Text("Home", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onCustomizeCategories) { Text("Customize home categories") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onManageYouTube) { Text("YouTube channels") }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = SkyPalette.SurfaceElevated)
            Spacer(Modifier.height(20.dp))

            Text("Playback", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Text(
                "Live stream format",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "TS starts faster on most providers; switch to HLS if channels stutter or fail.",
                style = MaterialTheme.typography.bodySmall,
                color = SkyPalette.TextSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Row {
                FilterChip(
                    selected = settings.preferredContainer == LiveContainer.TS,
                    onClick = { viewModel.setContainer(LiveContainer.TS) },
                    label = { Text("TS") },
                )
                Spacer(Modifier.padding(4.dp))
                FilterChip(
                    selected = settings.preferredContainer == LiveContainer.HLS,
                    onClick = { viewModel.setContainer(LiveContainer.HLS) },
                    label = { Text("HLS") },
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Keep audio playing in background", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = settings.backgroundAudio,
                    onCheckedChange = viewModel::setBackgroundAudio,
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("User agent", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Some providers only accept known players. Change this if streams are blocked.",
                style = MaterialTheme.typography.bodySmall,
                color = SkyPalette.TextSecondary,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = uaDraft,
                onValueChange = { uaDraft = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { viewModel.setUserAgent(uaDraft) },
                enabled = uaDraft != settings.userAgent,
            ) { Text("Save user agent") }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = SkyPalette.SurfaceElevated)
            Spacer(Modifier.height(20.dp))

            Text("Library", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = viewModel::resync) { Text("Refresh channels & library") }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = SkyPalette.SurfaceElevated)
            Spacer(Modifier.height(20.dp))

            Text("App", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            SettingRow(
                "Version",
                "${com.denham.skyline.BuildConfig.VERSION_NAME} " +
                    "(${com.denham.skyline.BuildConfig.VERSION_CODE})",
            )
            SettingRow("Built", com.denham.skyline.BuildConfig.BUILD_TIME)
            Spacer(Modifier.height(12.dp))
            Text(
                "Downloads the newest Skyline build in your browser — install it over the top, nothing is lost.",
                style = MaterialTheme.typography.bodySmall,
                color = SkyPalette.TextSecondary,
            )
            Spacer(Modifier.height(6.dp))
            val context = androidx.compose.ui.platform.LocalContext.current
            OutlinedButton(onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(UPDATE_URL),
                        )
                    )
                }
            }) { Text("Get latest version") }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { viewModel.signOut(onSignedOut) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text("Sign out") }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = SkyPalette.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
