package com.denham.skyline.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denham.skyline.BuildConfig
import com.denham.skyline.data.repo.YouTubeRepository
import com.denham.skyline.data.repo.YouTubeSearchResult
import com.denham.skyline.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** A subscription as shown in the list, resolved to a display title. */
data class SubscriptionItem(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val isPlaylist: Boolean,
)

class YouTubeSubscriptionViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val apiKey: String = BuildConfig.YOUTUBE_API_KEY

    /** Search needs a Data API key; adding by URL never does. */
    val searchEnabled: Boolean get() = apiKey.isNotBlank()

    private val _selectedMember = MutableStateFlow<String?>("Sophie")
    val selectedMember: StateFlow<String?> = _selectedMember

    private val _subscriptions = MutableStateFlow<List<SubscriptionItem>>(emptyList())
    val subscriptions: StateFlow<List<SubscriptionItem>> = _subscriptions

    private val _searchResults = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val searchResults: StateFlow<List<YouTubeSearchResult>> = _searchResults

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        reloadSubscriptions()
    }

    fun selectMember(name: String) {
        _selectedMember.value = name
        _searchResults.value = emptyList()
        _message.value = null
        reloadSubscriptions()
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    /**
     * One entry point for the text field: a pasted link or id is added
     * directly, an @handle is resolved via the API, and anything else is
     * treated as a search term. Avoids making the user choose the right mode.
     */
    fun submit() {
        val input = _query.value.trim()
        if (input.isBlank()) return

        viewModelScope.launch {
            _busy.value = true
            _message.value = null
            try {
                val repo = container.youtubeRepository

                val pastedId = repo.extractSubscriptionId(input)
                if (pastedId != null) {
                    addSubscription(pastedId, title = null)
                    return@launch
                }

                if (input.contains('@')) {
                    val resolved = repo.resolveHandle(input, apiKey)
                    if (resolved != null) {
                        addSubscription(resolved, title = null)
                        return@launch
                    }
                    if (!searchEnabled) {
                        _message.value =
                            "Handles need a search key. Paste the channel's /channel/UC... link instead."
                        return@launch
                    }
                }

                if (!searchEnabled) {
                    _message.value =
                        "Search is unavailable in this build. Paste a channel or playlist link."
                    return@launch
                }

                val results = repo.search(input, apiKey)
                _searchResults.value = results
                if (results.isEmpty()) _message.value = "No channels or playlists found."
            } catch (e: Exception) {
                _message.value = e.message ?: "Something went wrong."
            } finally {
                _busy.value = false
            }
        }
    }

    fun subscribe(result: YouTubeSearchResult) {
        viewModelScope.launch {
            _busy.value = true
            try {
                addSubscription(result.id, result.title)
                _searchResults.value = emptyList()
                _query.value = ""
            } finally {
                _busy.value = false
            }
        }
    }

    fun unsubscribe(id: String) {
        val member = _selectedMember.value ?: return
        viewModelScope.launch {
            val accountId = container.account.value?.username ?: return@launch
            runCatching {
                val current = container.categoryPreferencesStore
                    .getYoutubeSubscriptions(accountId, member)
                container.categoryPreferencesStore
                    .setYoutubeSubscriptions(accountId, member, current - id)
            }
            reloadSubscriptions()
        }
    }

    private suspend fun addSubscription(id: String, title: String?) {
        val member = _selectedMember.value ?: return
        val accountId = container.account.value?.username ?: return
        val store = container.categoryPreferencesStore

        val current = store.getYoutubeSubscriptions(accountId, member)
        if (id in current) {
            _message.value = "Already subscribed."
            return
        }
        store.setYoutubeSubscriptions(accountId, member, current + id)

        // Fetching now both populates the home rail and, for pasted ids with
        // no known title, lets the feed supply one.
        runCatching {
            container.youtubeRepository.subscribeToChannel(
                id,
                title ?: defaultTitleFor(id),
            )
        }
        _query.value = ""
        _message.value = "Added."
        reloadSubscriptions()
    }

    private fun defaultTitleFor(id: String): String =
        if (YouTubeRepository.isPlaylistId(id)) "Playlist $id" else "Channel $id"

    private fun reloadSubscriptions() {
        val member = _selectedMember.value ?: return
        viewModelScope.launch {
            val accountId = container.account.value?.username ?: return@launch
            val ids = runCatching {
                container.categoryPreferencesStore.getYoutubeSubscriptions(accountId, member)
            }.getOrDefault(emptyList())

            // Titles come from whatever the feed refresh stored; fall back to
            // the id so a subscription is never invisible in the list.
            _subscriptions.value = ids.map { id ->
                SubscriptionItem(
                    id = id,
                    title = titleFor(id),
                    thumbnailUrl = null,
                    isPlaylist = YouTubeRepository.isPlaylistId(id),
                )
            }
        }
    }

    private suspend fun titleFor(id: String): String =
        runCatching { container.db.youtubeChannelDao().byId(id)?.title }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: defaultTitleFor(id)
}
