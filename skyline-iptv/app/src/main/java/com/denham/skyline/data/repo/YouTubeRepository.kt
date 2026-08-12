package com.denham.skyline.data.repo

import android.util.Xml
import com.denham.skyline.data.db.SkylineDatabase
import com.denham.skyline.data.db.YouTubeChannelEntity
import com.denham.skyline.data.db.YouTubeVideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.xmlpull.v1.XmlPullParser
import java.time.Instant

data class YouTubeVideo(
    val id: String,
    val title: String,
    val thumbnail: String,
    val publishedAt: String,
    val videoUrl: String,
)

/** A channel or playlist that can be subscribed to. */
data class YouTubeSearchResult(
    /** Channel id (UC...) or playlist id (PL...); both work as a feed source. */
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String?,
    val isPlaylist: Boolean,
)

class YouTubeRepository(
    private val db: SkylineDatabase,
    private val okHttpClient: OkHttpClient,
) {
    private val channelDao = db.youtubeChannelDao()
    private val videoDao = db.youtubeVideoDao()

    private val defaultChannels = mapOf(
        "Ryan's World" to "UCLs_JK8qnG_-IqrmRcIReVA",
        "Most Maples" to "UCxGDGLb_wYvQVV8p7pFRvHg",
        "Moriah Elisabeth" to "UCrXTn0p3-w2K3qYXnfZdPWA",
    )

    /**
     * YouTube RSS feed URL. The same endpoint serves channels and playlists
     * under different parameters, which is what lets playlists be stored and
     * refreshed through exactly the same path as channels.
     */
    private fun rssUrl(sourceId: String): String =
        if (isPlaylistId(sourceId)) {
            "https://www.youtube.com/feeds/videos.xml?playlist_id=$sourceId"
        } else {
            "https://www.youtube.com/feeds/videos.xml?channel_id=$sourceId"
        }

    /**
     * Initialize default channels for a family member (typically Sophie on first run).
     */
    suspend fun initializeDefaultChannels() = withContext(Dispatchers.IO) {
        defaultChannels.forEach { (title, channelId) ->
            subscribeToChannel(channelId, title)
        }
    }

    /**
     * Fetch latest videos for a channel from RSS feed and update database.
     * Returns immediately if cache is fresh (< 30 minutes old).
     */
    suspend fun refreshChannelVideos(channelId: String, forceFresh: Boolean = false) = withContext(Dispatchers.IO) {
        val existing = channelDao.byId(channelId)
        val now = System.currentTimeMillis()
        val shouldRefresh = forceFresh || existing == null || (now - existing.lastFetchedAt) > 30 * 60 * 1000

        if (!shouldRefresh) return@withContext

        try {
            val feed = fetchRssFeed(channelId)
            if (feed.videos.isNotEmpty()) {
                // Prefer the feed's own name: subscriptions added by pasted
                // link arrive with only a placeholder title.
                val existingTitle = existing?.title?.takeUnless { it.isPlaceholderTitle() }
                val title = existingTitle
                    ?: feed.feedTitle
                    ?: existing?.title
                    ?: "Channel $channelId"
                channelDao.upsert(
                    YouTubeChannelEntity(
                        channelId = channelId,
                        title = title,
                        lastFetchedAt = now,
                    )
                )
                videoDao.insertAll(feed.videos)
                videoDao.pruneOlderThan20(channelId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Fetch videos from a YouTube channel's RSS feed. */
    private suspend fun fetchRssFeed(channelId: String): ParsedFeed = withContext(Dispatchers.IO) {
        val url = rssUrl(channelId)
        val request = okhttp3.Request.Builder().url(url).build()

        return@withContext okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use ParsedFeed(null, emptyList())

            response.body?.byteStream()?.use { stream ->
                parseYouTubeRssFeed(channelId, stream)
            } ?: ParsedFeed(null, emptyList())
        }
    }

    /** Parse YouTube RSS XML and extract videos. */
    private fun parseYouTubeRssFeed(
        channelId: String,
        inputStream: java.io.InputStream,
    ): ParsedFeed {
        val videos = mutableListOf<YouTubeVideoEntity>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        var currentEntry: YouTubeVideoBuilder? = null
        var feedTitle: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    // Match the local name, never the prefixed form.
                    // Xml.newPullParser() enables FEATURE_PROCESS_NAMESPACES,
                    // so parser.name returns "videoId", not "yt:videoId" --
                    // the prefixed cases this used to match never fired, so
                    // videoId stayed empty, every entry failed build()'s
                    // non-empty check, and no video was ever stored. Stripping
                    // any prefix keeps this correct whichever way namespace
                    // processing is configured.
                    when (parser.name?.substringAfterLast(':')) {
                        "entry" -> currentEntry = YouTubeVideoBuilder(channelId)
                        "videoId" -> currentEntry?.videoId = parser.nextText()
                        "title" -> {
                            val entry = currentEntry
                            if (entry == null) {
                                // The feed's own <title> precedes any <entry>,
                                // so this is the channel or playlist name --
                                // used to label subscriptions added by pasted
                                // link, where no title is known up front.
                                if (feedTitle == null) feedTitle = parser.nextText()
                            } else {
                                entry.title = parser.nextText()
                            }
                        }
                        "description" -> currentEntry?.description = parser.nextText()
                        "thumbnail" -> {
                            currentEntry?.thumbnailUrl = parser.getAttributeValue(null, "url")
                        }
                        "published" -> currentEntry?.publishedAt = parseRfc3339(parser.nextText())
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "entry" && currentEntry != null) {
                        val video = currentEntry.build()
                        if (video != null) videos.add(video)
                        currentEntry = null
                    }
                }
                else -> {}
            }
            eventType = parser.next()
        }

        return ParsedFeed(feedTitle, videos)
    }

    private data class ParsedFeed(
        val feedTitle: String?,
        val videos: List<YouTubeVideoEntity>,
    )

    /** Parse RFC 3339 datetime string (e.g., "2024-07-20T10:30:00Z") to milliseconds. */
    private fun parseRfc3339(timestamp: String): Long {
        return try {
            Instant.parse(timestamp).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    /** Subscribe to a channel (add it to the database). */
    suspend fun subscribeToChannel(channelId: String, title: String = "Channel $channelId") = withContext(Dispatchers.IO) {
        channelDao.upsert(
            YouTubeChannelEntity(
                channelId = channelId,
                title = title,
                lastFetchedAt = 0,
            )
        )
        refreshChannelVideos(channelId, forceFresh = true)
    }

    /** Unsubscribe from a channel (remove it and its videos). */
    suspend fun unsubscribeFromChannel(channelId: String) = withContext(Dispatchers.IO) {
        channelDao.delete(channelId)
    }

    /** Get all subscribed channels. */
    fun observeChannels(): Flow<List<YouTubeChannelEntity>> = channelDao.observeAll()

    /** Get all videos for a specific channel. */
    fun observeChannelVideos(channelId: String): Flow<List<YouTubeVideoEntity>> =
        videoDao.observeByChannel(channelId)

    /** Get latest videos from all subscribed channels. */
    fun observeLatestVideos(channelIds: List<String>, limit: Int = 30): Flow<List<YouTubeVideoEntity>> {
        return if (channelIds.isEmpty()) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            videoDao.observeLatestForChannels(channelIds, limit)
        }
    }

    /**
     * Search YouTube for channels and playlists.
     *
     * Needs a YouTube Data API key: the RSS feeds used elsewhere are keyless
     * but only resolve an id you already know, and have no search endpoint.
     * Returns empty when no key is configured so the UI can fall back to
     * pasting a URL rather than failing.
     */
    suspend fun search(query: String, apiKey: String): List<YouTubeSearchResult> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank() || query.isBlank()) return@withContext emptyList()
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "$DATA_API/search?part=snippet&type=channel,playlist" +
                "&maxResults=25&q=$encoded&key=$apiKey"
            val request = okhttp3.Request.Builder().url(url).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = response.body?.string() ?: return@use emptyList()
                val items = org.json.JSONObject(body).optJSONArray("items")
                    ?: return@use emptyList()

                (0 until items.length()).mapNotNull { index ->
                    val item = items.optJSONObject(index) ?: return@mapNotNull null
                    val idBlock = item.optJSONObject("id") ?: return@mapNotNull null
                    val snippet = item.optJSONObject("snippet") ?: return@mapNotNull null

                    val playlistId = idBlock.optString("playlistId").takeIf { it.isNotBlank() }
                    val channelId = idBlock.optString("channelId").takeIf { it.isNotBlank() }
                    val id = playlistId ?: channelId ?: return@mapNotNull null

                    YouTubeSearchResult(
                        id = id,
                        title = snippet.optString("title"),
                        description = snippet.optString("description"),
                        thumbnailUrl = snippet.optJSONObject("thumbnails")
                            ?.optJSONObject("medium")?.optString("url"),
                        isPlaylist = playlistId != null,
                    )
                }
            }
        }

    /**
     * Pull a subscribable id out of pasted text: a bare id, a channel URL, or
     * a playlist URL. Returns null for anything else (notably @handles, which
     * carry no id and need [resolveHandle]).
     */
    fun extractSubscriptionId(input: String): String? {
        val text = input.trim()
        if (CHANNEL_ID_PATTERN.matches(text) || PLAYLIST_ID_PATTERN.matches(text)) return text
        PLAYLIST_URL_PATTERN.find(text)?.let { return it.groupValues[1] }
        CHANNEL_URL_PATTERN.find(text)?.let { return it.groupValues[1] }
        return null
    }

    /**
     * Resolve an @handle (or bare name) to a channel id via the Data API.
     * Handles appear in modern YouTube URLs but contain no id, so there is no
     * keyless way to map one to a feed.
     */
    suspend fun resolveHandle(input: String, apiKey: String): String? =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext null
            val handle = HANDLE_PATTERN.find(input.trim())?.groupValues?.get(1)
                ?: return@withContext null
            val url = "$DATA_API/channels?part=id&forHandle=@$handle&key=$apiKey"
            val request = okhttp3.Request.Builder().url(url).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                org.json.JSONObject(body).optJSONArray("items")
                    ?.optJSONObject(0)?.optString("id")?.takeIf { it.isNotBlank() }
            }
        }

    /** Clear all YouTube data. */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        videoDao.clear()
        channelDao.clear()
    }

    private data class YouTubeVideoBuilder(
        val channelId: String,
        var videoId: String = "",
        var title: String = "",
        var description: String? = null,
        var thumbnailUrl: String? = null,
        var publishedAt: Long = 0,
    ) {
        fun build(): YouTubeVideoEntity? {
            return if (videoId.isNotEmpty() && title.isNotEmpty()) {
                YouTubeVideoEntity(
                    videoId = videoId,
                    channelId = channelId,
                    title = title,
                    description = description,
                    thumbnailUrl = thumbnailUrl,
                    publishedAt = publishedAt,
                )
            } else {
                null
            }
        }
    }

    companion object {
        private const val DATA_API = "https://www.googleapis.com/youtube/v3"

        /** Placeholder titles are replaced once the feed supplies a real one. */
        private fun String.isPlaceholderTitle(): Boolean =
            startsWith("Channel UC") || startsWith("Playlist ")

        /** Playlist ids start with PL/UU/LL/FL; channel ids always with UC. */
        fun isPlaylistId(id: String): Boolean =
            id.startsWith("PL") || id.startsWith("UU") ||
                id.startsWith("LL") || id.startsWith("FL")

        private val CHANNEL_ID_PATTERN = Regex("""^UC[\w-]{20,}$""")
        private val PLAYLIST_ID_PATTERN = Regex("""^(PL|UU|LL|FL)[\w-]{10,}$""")
        private val PLAYLIST_URL_PATTERN = Regex("""[?&]list=([\w-]+)""")
        private val CHANNEL_URL_PATTERN = Regex("""/channel/(UC[\w-]+)""")
        private val HANDLE_PATTERN = Regex("""@([A-Za-z0-9._-]+)""")
    }
}
