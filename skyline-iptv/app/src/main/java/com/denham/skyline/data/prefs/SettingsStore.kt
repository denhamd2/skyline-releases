package com.denham.skyline.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.denham.skyline.core.LiveContainer
import com.denham.skyline.data.api.UserAgentInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore by preferencesDataStore(name = "skyline_settings")

data class AppSettings(
    val userAgent: String = UserAgentInterceptor.DEFAULT_USER_AGENT,
    val preferredContainer: LiveContainer = LiveContainer.TS,
    val backgroundAudio: Boolean = true,
    /** "Smart Downloads": delete a download once it's been fully watched. */
    val autoDeleteWatched: Boolean = true,
    /** Epoch ms of the last successful xmltv guide import (0 = never). */
    val lastEpgImportMs: Long = 0L,
)

class SettingsStore(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            userAgent = prefs[KEY_USER_AGENT] ?: UserAgentInterceptor.DEFAULT_USER_AGENT,
            preferredContainer = if (prefs[KEY_CONTAINER] == LiveContainer.HLS.extension) {
                LiveContainer.HLS
            } else {
                LiveContainer.TS
            },
            backgroundAudio = prefs[KEY_BACKGROUND_AUDIO] ?: true,
            autoDeleteWatched = prefs[KEY_AUTO_DELETE_WATCHED] ?: true,
            lastEpgImportMs = prefs[KEY_LAST_EPG_IMPORT] ?: 0L,
        )
    }

    /**
     * Hot snapshot for synchronous readers (the UA interceptor runs on OkHttp
     * threads and can't suspend).
     */
    val current = settings.stateIn(scope, SharingStarted.Eagerly, AppSettings())

    suspend fun setUserAgent(value: String) {
        context.dataStore.edit { it[KEY_USER_AGENT] = value.trim() }
    }

    suspend fun setPreferredContainer(value: LiveContainer) {
        context.dataStore.edit { it[KEY_CONTAINER] = value.extension }
    }

    suspend fun setBackgroundAudio(value: Boolean) {
        context.dataStore.edit { it[KEY_BACKGROUND_AUDIO] = value }
    }

    suspend fun setAutoDeleteWatched(value: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_DELETE_WATCHED] = value }
    }

    suspend fun setLastEpgImport(epochMs: Long) {
        context.dataStore.edit { it[KEY_LAST_EPG_IMPORT] = epochMs }
    }

    private companion object {
        val KEY_USER_AGENT = stringPreferencesKey("user_agent")
        val KEY_CONTAINER = stringPreferencesKey("preferred_container")
        val KEY_BACKGROUND_AUDIO = booleanPreferencesKey("background_audio")
        val KEY_AUTO_DELETE_WATCHED = booleanPreferencesKey("auto_delete_watched")
        val KEY_LAST_EPG_IMPORT = longPreferencesKey("last_epg_import_ms")
    }
}
