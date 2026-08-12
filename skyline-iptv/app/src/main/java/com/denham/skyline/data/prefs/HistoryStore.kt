package com.denham.skyline.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.historyStore by preferencesDataStore(name = "skyline_history")

/** The most recently played item — powers the Home "Continue Watching" card. */
data class LastPlayed(
    val type: String,          // "live" | "vod" | "episode"
    val id: Int,
    val title: String,
    val imageUrl: String?,
    val playbackUrl: String,
    val positionMs: Long,
    val durationMs: Long,
) {
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

class HistoryStore(private val context: Context) {

    val lastPlayed: Flow<LastPlayed?> = context.historyStore.data.map { prefs ->
        val type = prefs[KEY_TYPE] ?: return@map null
        val url = prefs[KEY_URL] ?: return@map null
        LastPlayed(
            type = type,
            id = prefs[KEY_ID]?.toInt() ?: 0,
            title = prefs[KEY_TITLE] ?: "",
            imageUrl = prefs[KEY_IMAGE],
            playbackUrl = url,
            positionMs = prefs[KEY_POSITION] ?: 0L,
            durationMs = prefs[KEY_DURATION] ?: 0L,
        )
    }

    suspend fun record(item: LastPlayed) {
        context.historyStore.edit { prefs ->
            prefs[KEY_TYPE] = item.type
            prefs[KEY_ID] = item.id.toLong()
            prefs[KEY_TITLE] = item.title
            item.imageUrl?.let { prefs[KEY_IMAGE] = it } ?: prefs.remove(KEY_IMAGE)
            prefs[KEY_URL] = item.playbackUrl
            prefs[KEY_POSITION] = item.positionMs
            prefs[KEY_DURATION] = item.durationMs
        }
    }

    suspend fun updatePosition(positionMs: Long, durationMs: Long) {
        context.historyStore.edit { prefs ->
            if (prefs[KEY_TYPE] != null) {
                prefs[KEY_POSITION] = positionMs
                prefs[KEY_DURATION] = durationMs
            }
        }
    }

    suspend fun clear() {
        context.historyStore.edit { it.clear() }
    }

    private companion object {
        val KEY_TYPE = stringPreferencesKey("type")
        val KEY_ID = longPreferencesKey("id")
        val KEY_TITLE = stringPreferencesKey("title")
        val KEY_IMAGE = stringPreferencesKey("image")
        val KEY_URL = stringPreferencesKey("url")
        val KEY_POSITION = longPreferencesKey("position_ms")
        val KEY_DURATION = longPreferencesKey("duration_ms")
    }
}
