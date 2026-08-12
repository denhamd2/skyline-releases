package com.denham.skyline.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denham.skyline.core.PortalAddress
import com.denham.skyline.core.XtreamAccount
import com.denham.skyline.data.repo.AuthResult
import com.denham.skyline.di.AppContainer
import com.denham.skyline.ui.theme.SkyPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

class LoginViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun update(server: String? = null, username: String? = null, password: String? = null) {
        _state.value = _state.value.copy(
            server = server ?: _state.value.server,
            username = username ?: _state.value.username,
            password = password ?: _state.value.password,
            error = null,
        )
    }

    fun signIn(onSuccess: () -> Unit) {
        val s = _state.value
        val portal = PortalAddress.parse(s.server)
        if (portal == null) {
            _state.value = s.copy(error = "Enter the server address your provider gave you, e.g. http://host:8080")
            return
        }
        if (s.username.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "Username and password are required.")
            return
        }
        val account = XtreamAccount(portal, s.username.trim(), s.password.trim())
        _state.value = s.copy(loading = true, error = null)
        viewModelScope.launch {
            when (val result = container.authRepository.authenticate(account)) {
                is AuthResult.Success -> {
                    container.signIn(account)
                    _state.value = _state.value.copy(loading = false)
                    onSuccess()
                }
                is AuthResult.Failure -> {
                    _state.value = _state.value.copy(loading = false, error = result.message)
                }
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: LoginViewModel, onSignedIn: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(SkyPalette.heroFallback),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            com.denham.skyline.ui.components.SkylineWordmark()
            Text(
                "Your TV, your way",
                style = MaterialTheme.typography.bodyLarge,
                color = SkyPalette.TextSecondary,
            )
            Spacer(Modifier.height(36.dp))

            OutlinedTextField(
                value = state.server,
                onValueChange = { viewModel.update(server = it) },
                label = { Text("Server (http://host:port)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.username,
                onValueChange = { viewModel.update(username = it) },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.update(password = it) },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.error != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.signIn(onSignedIn) },
                enabled = !state.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Sign in", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "This app ships with no channels. Enter the Xtream Codes details from your own IPTV subscription.",
                style = MaterialTheme.typography.bodySmall,
                color = SkyPalette.TextSecondary,
            )
        }
    }
}
