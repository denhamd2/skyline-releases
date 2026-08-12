package com.denham.skyline.di

import android.content.Context
import android.util.Log
import androidx.media3.common.util.UnstableApi
import com.denham.skyline.core.StreamUrlBuilder
import com.denham.skyline.core.XtreamAccount
import com.denham.skyline.data.api.ApiClientFactory
import com.denham.skyline.data.api.XtreamApi
import com.denham.skyline.data.db.SkylineDatabase
import com.denham.skyline.data.prefs.CategoryPreferencesStore
import com.denham.skyline.data.prefs.CredentialStore
import com.denham.skyline.data.prefs.SettingsStore
import com.denham.skyline.data.repo.AuthRepository
import com.denham.skyline.data.repo.ContentRepository
import com.denham.skyline.data.repo.EpgRepository
import com.denham.skyline.data.repo.UpdateRepository
import com.denham.skyline.player.PlayerManager
import com.denham.skyline.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manual dependency container (single-module personal app — no DI framework
 * to fight during builds). Everything is lazy; the account can change at
 * runtime (sign out / sign in), which invalidates the cached Retrofit API.
 */
@UnstableApi
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val settingsStore = SettingsStore(context)
    val credentialStore = CredentialStore(context)
    val categoryPreferencesStore = CategoryPreferencesStore(context)

    private val apiClientFactory = ApiClientFactory(
        uaProvider = { settingsStore.current.value.userAgent },
    )

    /** Shared OkHttp stack: API calls, player data source, Coil images. */
    val okHttpClient by lazy { apiClientFactory.okHttpClient() }

    val db by lazy { SkylineDatabase.build(context) }

    private val _account = MutableStateFlow(loadInitialAccount())
    val account: StateFlow<XtreamAccount?> = _account

    /**
     * Runs at container construction, in Application.onCreate() -- before
     * any UI exists. EncryptedSharedPreferences touches the Android
     * Keystore, which can throw on some devices/states; a failure here must
     * not prevent the app from launching (it would crash before any screen,
     * including the crash-log screen, ever renders), so this degrades to
     * "no saved account" instead of crashing.
     *
     * There is deliberately no baked-in credential fallback: builds are
     * published to a public releases repo so the app can update itself, and
     * anything compiled into the APK can be extracted from it. Each device
     * signs in once; the account is then persisted in [CredentialStore], so
     * existing installs are unaffected by this and won't be asked again.
     */
    private fun loadInitialAccount(): XtreamAccount? = try {
        credentialStore.load().also { loaded ->
            Log.d("AppContainer", "Loaded credentials from store: ${loaded != null}")
        }
    } catch (e: Exception) {
        Log.e("AppContainer", "Failed to load credentials, starting signed out", e)
        null
    }

    private var cachedApi: Pair<XtreamAccount, XtreamApi>? = null

    /** Retrofit client for the signed-in account; rebuilt on account change. */
    fun api(): XtreamApi {
        val acct = _account.value
            ?: error("api() called with no signed-in account")
        cachedApi?.let { (cachedAccount, api) ->
            if (cachedAccount == acct) return api
        }
        return apiClientFactory.xtreamApi(okHttpClient, acct).also {
            cachedApi = acct to it
        }
    }

    fun urlBuilder(): StreamUrlBuilder {
        val acct = _account.value ?: error("urlBuilder() called with no signed-in account")
        return StreamUrlBuilder(acct)
    }

    fun signIn(account: XtreamAccount) {
        credentialStore.save(account)
        _account.value = account
    }

    suspend fun signOut() {
        playerManager.stopAndClear()
        credentialStore.clear()
        _account.value = null
        cachedApi = null
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.clearAllTables()
        }
    }

    val authRepository by lazy {
        AuthRepository { acct -> apiClientFactory.xtreamApi(okHttpClient, acct) }
    }

    val contentRepository by lazy { ContentRepository(db) { api() } }

    val epgRepository by lazy { EpgRepository(db.epgDao()) { api() } }

    val guideRepository by lazy {
        com.denham.skyline.data.repo.GuideRepository(db, settingsStore) { api() }
    }

    val youtubeRepository by lazy {
        com.denham.skyline.data.repo.YouTubeRepository(db, okHttpClient)
    }

    val footballRepository by lazy {
        com.denham.skyline.data.repo.FootballRepository(okHttpClient)
    }

    val updateRepository by lazy {
        UpdateRepository(appContext, BuildConfig.VERSION_NAME, okHttpClient)
    }

    val historyStore by lazy { com.denham.skyline.data.prefs.HistoryStore(appContext) }

    val downloadsCenter by lazy {
        com.denham.skyline.player.DownloadsCenter(appContext, okHttpClient)
    }

    val playerManager by lazy {
        PlayerManager(appContext, downloadsCenter.playbackDataSourceFactory)
    }

    // -- Transient UI coordination (single-activity app) --------------------

    /** VOD playback handoff between detail screens and the player route. */
    @Volatile
    var pendingVodPlayback: com.denham.skyline.ui.navigation.VodPlaybackRequest? = null

    /** YouTube playback handoff between home carousel and the player route. */
    @Volatile
    var pendingYoutubePlayback: com.denham.skyline.ui.navigation.YouTubePlaybackRequest? = null

    /** True while the player route is on screen; drives auto-PiP on Home press. */
    @Volatile
    var playerScreenVisible: Boolean = false

    /** Mirrors Activity.isInPictureInPictureMode for non-composable readers. */
    @Volatile
    var inPipMode: Boolean = false
}
