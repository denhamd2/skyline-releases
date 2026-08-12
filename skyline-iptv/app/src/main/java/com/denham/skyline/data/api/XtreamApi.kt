package com.denham.skyline.data.api

import com.denham.skyline.core.CategoryDto
import com.denham.skyline.core.LiveStreamDto
import com.denham.skyline.core.PlayerApiResponse
import com.denham.skyline.core.SeriesDto
import com.denham.skyline.core.ShortEpgResponse
import com.denham.skyline.core.VodStreamDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Everything the client needs runs through `player_api.php` with an
 * `action` query parameter. Credentials are added by [CredentialsInterceptor]
 * so they never appear in call sites (or logs).
 */
interface XtreamApi {

    @GET("player_api.php")
    suspend fun authenticate(): PlayerApiResponse

    @GET("player_api.php?action=get_live_categories")
    suspend fun liveCategories(): List<CategoryDto>

    @GET("player_api.php?action=get_vod_categories")
    suspend fun vodCategories(): List<CategoryDto>

    @GET("player_api.php?action=get_series_categories")
    suspend fun seriesCategories(): List<CategoryDto>

    /** Full payload can be tens of MB on big providers — stream it. */
    @Streaming
    @GET("player_api.php?action=get_live_streams")
    suspend fun liveStreamsRaw(): ResponseBody

    @Streaming
    @GET("player_api.php?action=get_vod_streams")
    suspend fun vodStreamsRaw(): ResponseBody

    @Streaming
    @GET("player_api.php?action=get_series")
    suspend fun seriesRaw(): ResponseBody

    /** Tolerant parsing needed (`info` may be `[]`): fetch raw, parse in repo. */
    @GET("player_api.php?action=get_vod_info")
    suspend fun vodInfoRaw(@Query("vod_id") vodId: Int): ResponseBody

    @GET("player_api.php?action=get_series_info")
    suspend fun seriesInfoRaw(@Query("series_id") seriesId: Int): ResponseBody

    @GET("player_api.php?action=get_short_epg")
    suspend fun shortEpg(
        @Query("stream_id") streamId: Int,
        @Query("limit") limit: Int = 4,
    ): ShortEpgResponse

    /** Full XMLTV guide — can be enormous; always stream-parse. */
    @Streaming
    @GET("xmltv.php")
    suspend fun xmltvRaw(): ResponseBody
}

/** DTO list types the streaming sync uses. */
typealias LiveStreams = List<LiveStreamDto>
typealias VodStreams = List<VodStreamDto>
typealias SeriesList = List<SeriesDto>
