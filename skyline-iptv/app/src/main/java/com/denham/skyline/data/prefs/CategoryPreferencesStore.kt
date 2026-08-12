package com.denham.skyline.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private val Context.categoryPreferencesDataStore by preferencesDataStore(name = "skyline_category_prefs")

/**
 * Must stay @Serializable: this type is round-tripped through
 * Json.encodeToString / decodeFromString below. Without the annotation the
 * reified serializer lookup falls back to a runtime search and throws
 * SerializationException on the first write, which crashed the app on
 * launch (HomeViewModel seeds default YouTube subscriptions during init).
 */
@Serializable
data class CategoryPreferences(
    val preferences: Map<String, List<String>> = emptyMap(),  // {familyMember: [categoryId1, categoryId2, ...]}
    val youtubeSubscriptions: Map<String, List<String>> = emptyMap(),  // {familyMember: [channelId1, channelId2, ...]}
    /**
     * Live channels pinned to one member's home tab, by streamId, independent
     * of that member's category selection -- the only way to say "just this
     * one channel" without pulling in the rest of its category.
     *
     * Deliberately not the global `favorites` table: favouriting is shared by
     * everyone, pinning belongs to one member. Defaults to empty so
     * preferences saved before this field existed still decode.
     */
    val pinnedChannels: Map<String, List<Int>> = emptyMap()  // {familyMember: [streamId1, streamId2, ...]}
)

/**
 * Both halves of one member's tab in a single value, so saving them is one
 * transform rather than two writes that can clobber each other. Pulled out as
 * a pure function so the merge is unit-testable without a DataStore.
 *
 * Only [userName]'s entries change; every other member's categories, pins and
 * subscriptions are carried through untouched.
 */
internal fun CategoryPreferences.withMemberTab(
    userName: String,
    categoryIds: List<String>,
    streamIds: List<Int>,
): CategoryPreferences = copy(
    preferences = preferences + (userName to categoryIds),
    pinnedChannels = pinnedChannels + (userName to streamIds),
)

class CategoryPreferencesStore(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    fun observePreferences(accountId: String): Flow<CategoryPreferences> {
        return context.categoryPreferencesDataStore.data.map { prefs ->
            val key = stringPreferencesKey("prefs_$accountId")
            val stored = prefs[key] ?: return@map CategoryPreferences()
            try {
                json.decodeFromString<CategoryPreferences>(stored)
            } catch (e: Exception) {
                CategoryPreferences()
            }
        }
    }

    val current = observePreferences("default").stateIn(scope, SharingStarted.Eagerly, CategoryPreferences())

    /**
     * Reads the stored value with [first] rather than `stateIn(...).value`.
     * The latter returns the *initial placeholder* because DataStore emits
     * asynchronously, so every read came back empty and every write built on
     * an empty base -- silently discarding previously saved preferences.
     */
    private suspend fun loadPreferences(accountId: String): CategoryPreferences =
        observePreferences(accountId).first()

    suspend fun setPreferences(accountId: String, preferences: CategoryPreferences) {
        val key = stringPreferencesKey("prefs_$accountId")
        val encoded = json.encodeToString(preferences)
        context.categoryPreferencesDataStore.edit { it[key] = encoded }
    }

    /**
     * The only way anything in here mutates. Both the read and the write
     * happen inside DataStore's [edit], which serialises writers, so a
     * read-modify-write cannot interleave with another one.
     *
     * Every setter used to read via [loadPreferences] and then write via
     * [setPreferences], with nothing holding the two together. Two saves
     * running at once therefore both built on the same base and the later
     * write silently dropped the earlier one's changes. That is exactly what
     * happened when the category editor started saving categories and pinned
     * channels as two separate coroutines: the pins landed, the categories
     * vanished, and the home tab fell back to keyword defaults as though
     * nothing had been configured.
     */
    private suspend fun update(
        accountId: String,
        transform: (CategoryPreferences) -> CategoryPreferences,
    ) {
        val key = stringPreferencesKey("prefs_$accountId")
        context.categoryPreferencesDataStore.edit { mutable ->
            val current = mutable[key]
                ?.let { runCatching { json.decodeFromString<CategoryPreferences>(it) }.getOrNull() }
                ?: CategoryPreferences()
            mutable[key] = json.encodeToString(transform(current))
        }
    }

    suspend fun setUserCategories(accountId: String, userName: String, categoryIds: List<String>) {
        update(accountId) { it.copy(preferences = it.preferences + (userName to categoryIds)) }
    }

    /**
     * Saves both halves of a member's tab in one transaction. They are one
     * user action -- pressing Save in the editor -- and must not be able to
     * half-land.
     */
    suspend fun setMemberTab(
        accountId: String,
        userName: String,
        categoryIds: List<String>,
        streamIds: List<Int>,
    ) {
        update(accountId) { it.withMemberTab(userName, categoryIds, streamIds) }
    }

    suspend fun getUserCategories(accountId: String, userName: String): List<String> =
        loadPreferences(accountId).preferences[userName] ?: emptyList()

    suspend fun getYoutubeSubscriptions(accountId: String, userName: String): List<String> =
        loadPreferences(accountId).youtubeSubscriptions[userName] ?: emptyList()

    suspend fun setYoutubeSubscriptions(accountId: String, userName: String, channelIds: List<String>) {
        update(accountId) {
            it.copy(youtubeSubscriptions = it.youtubeSubscriptions + (userName to channelIds))
        }
    }

    suspend fun getPinnedChannels(accountId: String, userName: String): List<Int> =
        loadPreferences(accountId).pinnedChannels[userName] ?: emptyList()

    suspend fun setPinnedChannels(accountId: String, userName: String, streamIds: List<Int>) {
        update(accountId) { it.copy(pinnedChannels = it.pinnedChannels + (userName to streamIds)) }
    }

    // add/remove read the current list and write it back, so they do the
    // whole thing inside one transaction rather than get-then-set.
    suspend fun addPinnedChannel(accountId: String, userName: String, streamId: Int) {
        update(accountId) { prefs ->
            val current = prefs.pinnedChannels[userName] ?: emptyList()
            if (current.contains(streamId)) prefs
            else prefs.copy(pinnedChannels = prefs.pinnedChannels + (userName to current + streamId))
        }
    }

    suspend fun removePinnedChannel(accountId: String, userName: String, streamId: Int) {
        update(accountId) { prefs ->
            val current = prefs.pinnedChannels[userName] ?: emptyList()
            prefs.copy(pinnedChannels = prefs.pinnedChannels + (userName to current - streamId))
        }
    }

    suspend fun addYoutubeSubscription(accountId: String, userName: String, channelId: String) {
        update(accountId) { prefs ->
            val current = prefs.youtubeSubscriptions[userName] ?: emptyList()
            if (current.contains(channelId)) prefs
            else prefs.copy(
                youtubeSubscriptions = prefs.youtubeSubscriptions + (userName to current + channelId)
            )
        }
    }

    suspend fun removeYoutubeSubscription(accountId: String, userName: String, channelId: String) {
        update(accountId) { prefs ->
            val current = prefs.youtubeSubscriptions[userName] ?: emptyList()
            prefs.copy(
                youtubeSubscriptions = prefs.youtubeSubscriptions + (userName to current - channelId)
            )
        }
    }
}
