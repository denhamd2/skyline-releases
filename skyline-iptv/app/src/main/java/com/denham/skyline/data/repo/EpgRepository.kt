package com.denham.skyline.data.repo

import com.denham.skyline.core.EpgText
import com.denham.skyline.data.api.XtreamApi
import com.denham.skyline.data.db.EpgDao
import com.denham.skyline.data.db.EpgNowNextEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Now/next programme info from `get_short_epg`. Cached in Room with a TTL so
 * scrolling a channel list doesn't hammer the panel (stock Xtream nginx
 * rate-limits around 20 req/s per IP).
 */
class EpgRepository(
    private val epgDao: EpgDao,
    private val api: () -> XtreamApi,
) {

    private val inFlight = mutableSetOf<Int>()
    private val inFlightLock = Mutex()

    fun observeForIds(ids: List<Int>): Flow<List<EpgNowNextEntity>> =
        epgDao.observeForIds(ids)

    /** Fetch-and-cache now/next; no-op when fresh or already being fetched. */
    suspend fun refresh(streamId: Int) = withContext(Dispatchers.IO) {
        val cached = epgDao.byId(streamId)
        val now = System.currentTimeMillis()
        if (cached != null && now - cached.fetchedAt < TTL_MS) return@withContext

        inFlightLock.withLock {
            if (!inFlight.add(streamId)) return@withContext
        }
        try {
            val listings = runCatching { api().shortEpg(streamId).epgListings }
                .getOrDefault(emptyList())
            val nowSec = now / 1000
            val current = listings.firstOrNull { l ->
                l.nowPlaying || (l.startTimestamp ?: 0) <= nowSec && nowSec < (l.stopTimestamp ?: 0)
            } ?: listings.firstOrNull()
            val next = listings.firstOrNull { l ->
                (l.startTimestamp ?: 0) > (current?.startTimestamp ?: 0)
            }
            epgDao.upsert(
                EpgNowNextEntity(
                    streamId = streamId,
                    nowTitle = current?.title?.let(EpgText::decode),
                    nowDescription = current?.description?.let(EpgText::decode),
                    nowStart = current?.startTimestamp,
                    nowStop = current?.stopTimestamp,
                    nextTitle = next?.title?.let(EpgText::decode),
                    nextStart = next?.startTimestamp,
                    nextStop = next?.stopTimestamp,
                    fetchedAt = now,
                )
            )
        } finally {
            inFlightLock.withLock { inFlight.remove(streamId) }
        }
    }

    private companion object {
        const val TTL_MS = 5 * 60 * 1000L
    }
}
