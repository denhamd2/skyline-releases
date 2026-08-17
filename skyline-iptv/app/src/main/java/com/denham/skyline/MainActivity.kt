package com.denham.skyline

import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Bundle
import android.util.Rational
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.util.Consumer
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import com.denham.skyline.player.PlaybackService
import com.denham.skyline.ui.navigation.SkylineNavHost
import com.denham.skyline.ui.theme.SkylineTheme

@UnstableApi
class MainActivity : androidx.fragment.app.FragmentActivity() {

    private val container get() = (application as SkylineApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If startup already failed (AppContainer never built), or the last
        // launch crashed before rendering anything, show the reason on-screen
        // instead of silently repeating the same crash -- there's no
        // computer/adb involved, so this is the only way to see what went wrong.
        val startupFailed = (application as SkylineApp).startupError != null
        val lastCrash = CrashLogger.readLastCrash(this)
        if (startupFailed || lastCrash != null) {
            setContent {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Skyline crashed on last launch", color = Color.White)
                    Text(
                        lastCrash ?: "Startup failed before any screen could load.",
                        color = Color.Red,
                        fontFamily = FontFamily.Monospace,
                    )
                    Button(onClick = {
                        CrashLogger.clearLastCrash(this@MainActivity)
                        // Restart the process rather than recreate(): a failed
                        // AppContainer lives on the Application, which recreate()
                        // does not rebuild.
                        finish()
                        startActivity(Intent(this@MainActivity, MainActivity::class.java))
                        Runtime.getRuntime().exit(0)
                    }) { Text("Dismiss and try again") }
                }
            }
            return
        }

        enableEdgeToEdge()

        // Build the CastPlayer now, on the main thread, so its session listener
        // exists before the user can reach a cast button. Connecting from the
        // Live or Guide screen used to leave no CastPlayer to receive the
        // session, and playback silently stayed on the phone. Not done on TV —
        // TvMainActivity has no cast UI.
        runCatching { container.playerManager.initCasting() }

        setContent {
            var isInPip by remember { mutableStateOf(isInPictureInPictureMode) }

            androidx.compose.runtime.DisposableEffect(Unit) {
                val listener = Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
                    isInPip = info.isInPictureInPictureMode
                    container.inPipMode = info.isInPictureInPictureMode
                }
                addOnPictureInPictureModeChangedListener(listener)
                onDispose { removeOnPictureInPictureModeChangedListener(listener) }
            }

            SkylineTheme {
                val navController = rememberNavController()
                val account by container.account.collectAsState()
                SkylineNavHost(
                    navController = navController,
                    container = container,
                    isInPip = isInPip,
                    signedIn = account != null,
                )
            }
        }
    }

    /** Home press while watching: shrink into picture-in-picture. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (container.playerScreenVisible && container.playerManager.player.isPlaying) {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
            )
        }
    }

    override fun onStart() {
        super.onStart()
        // Keep the media session service alive for background/lock-screen
        // controls whenever something is (or may start) playing.
        if (container.settingsStore.current.value.backgroundAudio) {
            runCatching { startService(Intent(this, PlaybackService::class.java)) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            container.playerManager.stopAndClear()
        }
    }
}
