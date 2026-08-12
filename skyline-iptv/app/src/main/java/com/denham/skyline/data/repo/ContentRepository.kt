package com.denham.skyline.data.repo

import android.util.Log
import androidx.room.withTransaction
import com.denham.skyline.core.CategoryDto
import com.denham.skyline.core.LiveStreamDto
import com.denham.skyline.core.SeriesDto
import com.denham.skyline.core.SeriesInfoResponse
import com.denham.skyline.core.VodInfoResponse
import com.denham.skyline.core.VodStreamDto
import com.denham.skyline.core.XtreamJson
import com.denham.skyline.data.api.XtreamApi
import com.denham.skyline.data.db.CategoryEntity
import com.denham.skyline.data.db.ChannelEntity
import com.denham.skyline.data.db.ContentType
import com.denham.skyline.data.db.MovieEntity
import com.denham.skyline.data.db.SeriesEntity
import com.denham.skyline.data.db.SkylineDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.decodeToSequence
import okhttp3.ResponseBody

sealed interface SyncState {
    data object Idle : SyncState
    data class Running(val step: String) : SyncState
    data class Done(val channels: Int, val movies: Int, val series: Int) : SyncState
    data class Failed(val message: String) : SyncState
}

/**
 * Pulls the provider's full catalogue into Room. Payloads on big providers
 * run to tens of thousands of items, so list endpoints are stream-parsed
 * (never fully buffered as a string) and written in chunked transactions.
 */
class ContentRepository(
    private val db: SkylineDatabase,
    private val api: () -> XtreamApi,
) {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    suspend fun syncAll(force: Boolean = false) = withContext(Dispatchers.IO) {
        if (_syncState.value is SyncState.Running) return@withContext
        if (!force && db.channelDao().count() > 0) {
            Log.d("ContentRepository", "Channels already in DB, skipping sync")
            _syncState.value = SyncState.Done(
                db.channelDao().count(), db.movieDao().count(), db.seriesDao().count()
            )
            return@withContext
        }
        try {
            Log.d("ContentRepository", "Starting sync")
            _syncState.value = SyncState.Running("Loading categories…")
            runCatching { syncCategories() } // best-effort; channels still load

            // Channels are what the user actually notices as "the channels" —
            // a flaky provider can drop the connection mid-stream, so retry once
            // before giving up. A failure here (and only here) fails the sync.
            Log.d("ContentRepository", "About to load live channels")
            _syncState.value = SyncState.Running("Loading live channels…")
            withRetry {
                Log.d("ContentRepository", "Calling liveStreamsRaw()")
                syncStreaming(
                    body = api().liveStreamsRaw(),
                    serializer = LiveStreamDto.serializer(),
                    clear = { db.channelDao().clear() },
                    insert = { chunk -> db.channelDao().insertChunk(chunk.map { it.toEntity() }) },
                )
            }
            Log.d("ContentRepository", "Live channels loaded")

            // Movies and series are best-effort: if the provider hiccups on
            // these, keep the channels we already have rather than failing the
            // whole first-run sync and showing an error.
            _syncState.value = SyncState.Running("Loading movies…")
            runCatching {
                Log.d("ContentRepository", "Calling vodStreamsRaw()")
                syncStreaming(
                    body = api().vodStreamsRaw(),
                    serializer = VodStreamDto.serializer(),
                    clear = { db.movieDao().clear() },
                    insert = { chunk -> db.movieDao().insertChunk(chunk.map { it.toEntity() }) },
                )
                Log.d("ContentRepository", "Movies loaded")
            }

            _syncState.value = SyncState.Running("Loading series…")
            runCatching {
                Log.d("ContentRepository", "Calling seriesRaw()")
                syncStreaming(
                    body = api().seriesRaw(),
                    serializer = SeriesDto.serializer(),
                    clear = { db.seriesDao().clear() },
                    insert = { chunk -> db.seriesDao().insertChunk(chunk.map { it.toEntity() }) },
                )
                Log.d("ContentRepository", "Series loaded")
            }

            Log.d("ContentRepository", "Sync completed successfully")
            _syncState.value = SyncState.Done(
                db.channelDao().count(), db.movieDao().count(), db.seriesDao().count()
            )
        } catch (e: Exception) {
            Log.e("ContentRepository", "Sync failed", e)
            // If a previous sync already loaded channels, keep showing them
            // rather than replacing the whole screen with an error.
            if (db.channelDao().count() > 0) {
                _syncState.value = SyncState.Done(
                    db.channelDao().count(), db.movieDao().count(), db.seriesDao().count()
                )
            } else {
                _syncState.value = SyncState.Failed(
                    "Couldn't load your channels. Check your connection and tap Try again."
                )
            }
        }
    }

    /** Runs [block], retrying once after a short pause on the first failure. */
    private suspend fun <T> withRetry(block: suspend () -> T): T =
        try {
            block()
        } catch (first: Exception) {
            kotlinx.coroutines.delay(1_500)
            block()
        }

    private suspend fun syncCategories() {
        val live = runCatching { api().liveCategories() }.getOrDefault(emptyList())
        val vod = runCatching { api().vodCategories() }.getOrDefault(emptyList())
        val series = runCatching { api().seriesCategories() }.getOrDefault(emptyList())
        db.withTransaction {
            db.categoryDao().clear(ContentType.LIVE)
            db.categoryDao().insertAll(live.toEntities(ContentType.LIVE))
            db.categoryDao().clear(ContentType.VOD)
            db.categoryDao().insertAll(vod.toEntities(ContentType.VOD))
            db.categoryDao().clear(ContentType.SERIES)
            db.categoryDao().insertAll(series.toEntities(ContentType.SERIES))
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun <T> syncStreaming(
        body: ResponseBody,
        serializer: KSerializer<T>,
        clear: suspend () -> Unit,
        insert: suspend (List<T>) -> Unit,
    ) {
        body.use { rb ->
            val sequence = XtreamJson.decodeToSequence(
                rb.byteStream(), serializer, DecodeSequenceMode.ARRAY_WRAPPED
            )
            db.withTransaction { clear() }
            val chunk = ArrayList<T>(CHUNK_SIZE)
            for (item in sequence) {
                chunk.add(item)
                if (chunk.size >= CHUNK_SIZE) {
                    val batch = ArrayList(chunk)
                    db.withTransaction { insert(batch) }
                    chunk.clear()
                }
            }
            if (chunk.isNotEmpty()) db.withTransaction { insert(chunk) }
        }
    }

    suspend fun vodInfo(vodId: Int): VodInfoResponse = withContext(Dispatchers.IO) {
        VodInfoResponse.parse(api().vodInfoRaw(vodId).use { it.string() })
    }

    suspend fun seriesInfo(seriesId: Int): SeriesInfoResponse = withContext(Dispatchers.IO) {
        SeriesInfoResponse.parse(api().seriesInfoRaw(seriesId).use { it.string() })
    }

    private companion object {
        const val CHUNK_SIZE = 500
    }
}

private fun List<CategoryDto>.toEntities(type: String): List<CategoryEntity> =
    mapIndexed { index, dto ->
        CategoryEntity(
            type = type,
            categoryId = dto.categoryId,
            name = dto.categoryName.ifBlank { "Unnamed" },
            sortOrder = index,
        )
    }

private fun LiveStreamDto.toEntity() = ChannelEntity(
    streamId = streamId,
    num = num,
    name = name,
    icon = streamIcon?.takeIf { it.isNotBlank() },
    epgChannelId = epgChannelId?.takeIf { it.isNotBlank() },
    categoryId = categoryId,
    tvArchive = tvArchive,
    tvArchiveDuration = tvArchiveDuration,
    added = added,
)

private fun VodStreamDto.toEntity() = MovieEntity(
    streamId = streamId,
    name = name,
    icon = streamIcon?.takeIf { it.isNotBlank() },
    rating5 = rating5,
    containerExtension = containerExtension,
    categoryId = categoryId,
    added = added,
)

private fun SeriesDto.toEntity() = SeriesEntity(
    seriesId = seriesId,
    name = name,
    cover = cover?.takeIf { it.isNotBlank() },
    plot = plot,
    genre = genre,
    rating5 = rating5,
    backdrop = backdropPath.firstOrNull(),
    categoryId = categoryId,
    lastModified = lastModified,
)
