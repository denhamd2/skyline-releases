package com.denham.skyline.data.repo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class Available(val version: String, val notes: String) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class Downloaded(val version: String) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class UpdateRepository(
    private val context: Context,
    private val currentVersion: String,
    private val okHttpClient: OkHttpClient,
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    private val cacheDir = File(context.cacheDir, "updates")

    init {
        cacheDir.mkdirs()
    }

    /**
     * [silent] suppresses the error dialog. The automatic check on app launch
     * passes true: the user did not ask for it, so a failure (offline, rate
     * limit, private-repo auth) must not greet them with a popup every start.
     * Explicit "check for updates" actions pass false and do surface errors.
     */
    suspend fun checkForUpdates(silent: Boolean = false) = withContext(Dispatchers.IO) {
        _state.value = UpdateState.Checking
        try {
            // Query the tag directly, not /releases/latest: that endpoint
            // excludes prereleases, and skyline-latest is published as one, so
            // it 404s even for an authenticated caller.
            val request = Request.Builder()
                .url("$RELEASES_API/tags/$RELEASE_TAG")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                fail(silent, "Failed to check for updates (HTTP ${response.code})")
                return@withContext
            }

            val json = JSONObject(response.body?.string() ?: "{}")
            val notes = json.optString("body", "")

            val assets = json.getJSONArray("assets")
            val apkAsset = (0 until assets.length()).asSequence()
                .mapNotNull { assets.optJSONObject(it) }
                .firstOrNull { it.getString("name") == APK_NAME }

            if (apkAsset == null) {
                fail(silent, "No APK found in the latest release")
                return@withContext
            }

            // The tag is rolling ("skyline-latest") and so is identical in
            // every release -- comparing it to anything reports an update
            // forever, which is exactly what it used to do. CI stamps the
            // build's versionName into the release notes instead; that is the
            // only field which actually differs between builds.
            val publishedVersion = BUILD_VERSION_PATTERN.find(notes)?.groupValues?.get(1)

            when {
                // Nothing to compare against (notes not stamped yet): stay
                // quiet rather than prompt for an update we can't verify.
                publishedVersion == null -> _state.value = UpdateState.Idle
                publishedVersion == currentVersion -> _state.value = UpdateState.Idle
                else -> _state.value = UpdateState.Available(publishedVersion, notes)
            }
        } catch (e: Exception) {
            fail(silent, e.message ?: "Unknown error")
        }
    }

    /** Errors from a user-initiated check are shown; background ones are not. */
    private fun fail(silent: Boolean, message: String) {
        _state.value = if (silent) UpdateState.Idle else UpdateState.Error(message)
    }

    suspend fun downloadAndInstall(version: String) = withContext(Dispatchers.IO) {
        try {
            _state.value = UpdateState.Downloading(0)

            // Always the rolling tag, never [version]: the asset lives under
            // /download/skyline-latest/, so using the version string here
            // (e.g. "1.1.65") would 404.
            val request = Request.Builder()
                .url("$RELEASES_DOWNLOAD/$RELEASE_TAG/$APK_NAME")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                _state.value = UpdateState.Error("Download failed: ${response.code}")
                return@withContext
            }

            val apkFile = File(cacheDir, "Skyline_$version.apk")
            response.body?.byteStream()?.use { input ->
                apkFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!apkFile.exists()) {
                _state.value = UpdateState.Error("Failed to save APK")
                return@withContext
            }

            _state.value = UpdateState.Downloaded(version)
            installAPK(apkFile)
        } catch (e: Exception) {
            _state.value = UpdateState.Error(e.message ?: "Download failed")
        }
    }

    private fun installAPK(file: File) {
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _state.value = UpdateState.Error("Failed to install: ${e.message}")
        }
    }

    fun dismiss() {
        _state.value = UpdateState.Idle
    }

    private companion object {
        // Public, binaries-only repo. The source repo is private, so the app
        // -- which sends no credentials -- cannot read its API or download its
        // assets. Publishing builds here is what makes self-updating possible
        // without shipping a token inside the APK, where it would be
        // extractable by anyone who downloads it.
        const val REPO = "denhamd2/skyline-releases"
        const val RELEASE_TAG = "skyline-latest"
        const val APK_NAME = "Skyline.apk"
        const val RELEASES_API = "https://api.github.com/repos/$REPO/releases"
        const val RELEASES_DOWNLOAD = "https://github.com/$REPO/releases/download"

        /** Matches the marker CI writes into the release notes, e.g.
         *  "build-version: 1.1.65". Kept in sync with the publish step in
         *  .github/workflows/build-skyline-apk.yml. */
        val BUILD_VERSION_PATTERN = Regex("""build-version:\s*(\S+)""")
    }
}
