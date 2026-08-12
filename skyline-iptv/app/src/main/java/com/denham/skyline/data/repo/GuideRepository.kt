package com.denham.skyline.data.repo

import android.util.Xml
import com.denham.skyline.core.XmltvTime
import com.denham.skyline.data.api.XtreamApi
import com.denham.skyline.data.db.EpgProgrammeEntity
import com.denham.skyline.data.db.SkylineDatabase
import com.denham.skyline.data.prefs.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

sealed interface GuideImportState {
    data object Idle : GuideImportState
    data object Running : GuideImportState
    data class Done(val programmes: Int) : GuideImportState
    data class Failed(val message: String) : GuideImportState
}

/**
 * Imports the provider's full XMLTV guide into Room. The document can run to
 * hundreds of MB on big providers, so it is parsed as a stream with
 * XmlPullParser, mapped to channels via epg_channel_id, kept only within a
 * retention window, and written in chunked transactions.
 */
class GuideRepository(
    private val db: SkylineDatabase,
    private val settingsStore: SettingsStore,
    private val api: () -> XtreamApi,
) {

    private val _state = MutableStateFlow<GuideImportState>(GuideImportState.Idle)
    val state: StateFlow<GuideImportState> = _state

    suspend fun programmesFor(
        channelIds: List<Int>,
        fromMs: Long,
        toMs: Long,
    ): List<EpgProgrammeEntity> = db.guideDao().forChannelsBetween(channelIds, fromMs, toMs)

    suspend fun hasGuideData(): Boolean = db.guideDao().count() > 0

    /** Import if never imported or stale (default: older than 12h). */
    suspend fun refreshIfStale(maxAgeMs: Long = 12 * 60 * 60 * 1000L) {
        val last = settingsStore.current.value.lastEpgImportMs
        if (System.currentTimeMillis() - last > maxAgeMs) importXmltv()
    }

    suspend fun importXmltv() = withContext(Dispatchers.IO) {
        if (_state.value is GuideImportState.Running) return@withContext
        _state.value = GuideImportState.Running
        try {
            // xmltv channel ids -> our stream ids (case-insensitive).
            val channelMap = buildChannelMap()
            if (channelMap.isEmpty()) {
                _state.value = GuideImportState.Failed(
                    "No channels carry EPG ids — the guide isn't available from this provider."
                )
                return@withContext
            }

            val now = System.currentTimeMillis()
            val windowStart = now - RETAIN_PAST_MS
            val windowEnd = now + RETAIN_FUTURE_MS
            var total = 0

            api().xmltvRaw().use { body ->
                val parser = Xml.newPullParser()
                parser.setInput(body.byteStream(), null)

                db.guideDao().clear()
                val chunk = ArrayList<EpgProgrammeEntity>(CHUNK_SIZE)

                var event = parser.eventType
                var channelId: Int? = null
                var startMs: Long? = null
                var stopMs: Long? = null
                var title: String? = null
                var desc: String? = null
                var icon: String? = null
                var inProgramme = false
                var textTarget = 0 // 1=title 2=desc

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> when (parser.name) {
                            "programme" -> {
                                inProgramme = true
                                channelId = parser.getAttributeValue(null, "channel")
                                    ?.lowercase()?.let(channelMap::get)
                                startMs = parser.getAttributeValue(null, "start")
                                    ?.let(XmltvTime::parseToEpochMs)
                                stopMs = parser.getAttributeValue(null, "stop")
                                    ?.let(XmltvTime::parseToEpochMs)
                                title = null
                                desc = null
                                icon = null
                            }
                            "title" -> if (inProgramme) textTarget = 1
                            "desc" -> if (inProgramme) textTarget = 2
                            // Programme artwork, when the provider includes it.
                            "icon" -> if (inProgramme && icon == null) {
                                icon = parser.getAttributeValue(null, "src")
                                    ?.takeIf { it.isNotBlank() }
                            }
                        }
                        XmlPullParser.TEXT -> when (textTarget) {
                            1 -> title = (title ?: "") + parser.text
                            2 -> desc = (desc ?: "") + parser.text
                        }
                        XmlPullParser.END_TAG -> when (parser.name) {
                            "title", "desc" -> textTarget = 0
                            "programme" -> {
                                val cid = channelId
                                val s = startMs
                                val e = stopMs
                                if (inProgramme && cid != null && s != null && e != null &&
                                    e > windowStart && s < windowEnd && !title.isNullOrBlank()
                                ) {
                                    chunk.add(
                                        EpgProgrammeEntity(
                                            channelStreamId = cid,
                                            startMs = s,
                                            stopMs = e,
                                            title = title!!.trim(),
                                            description = desc?.trim()?.takeIf { it.isNotEmpty() },
                                            imageUrl = icon,
                                        )
                                    )
                                    if (chunk.size >= CHUNK_SIZE) {
                                        db.guideDao().insertChunk(ArrayList(chunk))
                                        total += chunk.size
                                        chunk.clear()
                                    }
                                }
                                inProgramme = false
                            }
                        }
                    }
                    event = parser.next()
                }
                if (chunk.isNotEmpty()) {
                    db.guideDao().insertChunk(chunk)
                    total += chunk.size
                }
            }

            settingsStore.setLastEpgImport(System.currentTimeMillis())
            _state.value = GuideImportState.Done(total)
        } catch (e: Exception) {
            _state.value = GuideImportState.Failed(
                "Guide import failed: ${e.message ?: e.javaClass.simpleName}"
            )
        }
    }

    private suspend fun buildChannelMap(): Map<String, Int> {
        val map = HashMap<String, Int>()
        // Channels table is already synced; walk it via paging-free query.
        var offset = 0
        while (true) {
            val page = db.channelDao().pageForEpgMap(PAGE, offset)
            if (page.isEmpty()) break
            for (ch in page) {
                ch.epgChannelId?.takeIf { it.isNotBlank() }?.let { map[it.lowercase()] = ch.streamId }
            }
            offset += page.size
        }
        return map
    }

    private companion object {
        const val CHUNK_SIZE = 500
        const val PAGE = 2000
        const val RETAIN_PAST_MS = 12 * 60 * 60 * 1000L       // 12h back
        const val RETAIN_FUTURE_MS = 4 * 24 * 60 * 60 * 1000L // 4 days ahead
    }
}
