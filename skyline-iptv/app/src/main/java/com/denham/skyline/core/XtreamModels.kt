package com.denham.skyline.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject

// ---------------------------------------------------------------------------
// player_api.php (no action) — authentication / account info
// ---------------------------------------------------------------------------

@Serializable
data class PlayerApiResponse(
    @SerialName("user_info") val userInfo: UserInfo? = null,
    @SerialName("server_info") val serverInfo: ServerInfo? = null,
)

@Serializable
data class UserInfo(
    val username: String = "",
    @Serializable(with = FlexBool::class) val auth: Boolean = false,
    val status: String = "",
    @Serializable(with = FlexLongOrNull::class) @SerialName("exp_date") val expDate: Long? = null,
    @Serializable(with = FlexBool::class) @SerialName("is_trial") val isTrial: Boolean = false,
    @Serializable(with = FlexInt::class) @SerialName("active_cons") val activeConnections: Int = 0,
    @Serializable(with = FlexInt::class) @SerialName("max_connections") val maxConnections: Int = 0,
    @Serializable(with = FlexStringList::class) @SerialName("allowed_output_formats")
    val allowedOutputFormats: List<String> = emptyList(),
)

@Serializable
data class ServerInfo(
    val url: String = "",
    @Serializable(with = FlexString::class) val port: String = "",
    @Serializable(with = FlexString::class) @SerialName("https_port") val httpsPort: String = "",
    @SerialName("server_protocol") val serverProtocol: String = "",
    val timezone: String = "",
    @Serializable(with = FlexLongOrNull::class) @SerialName("timestamp_now") val timestampNow: Long? = null,
)

// ---------------------------------------------------------------------------
// Categories (live / vod / series share the shape)
// ---------------------------------------------------------------------------

@Serializable
data class CategoryDto(
    @Serializable(with = FlexString::class) @SerialName("category_id") val categoryId: String = "",
    @SerialName("category_name") val categoryName: String = "",
    @Serializable(with = FlexInt::class) @SerialName("parent_id") val parentId: Int = 0,
)

// ---------------------------------------------------------------------------
// get_live_streams
// ---------------------------------------------------------------------------

@Serializable
data class LiveStreamDto(
    @Serializable(with = FlexInt::class) val num: Int = 0,
    val name: String = "",
    @Serializable(with = FlexInt::class) @SerialName("stream_id") val streamId: Int = 0,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    @Serializable(with = FlexString::class) @SerialName("category_id") val categoryId: String = "",
    @Serializable(with = FlexBool::class) @SerialName("tv_archive") val tvArchive: Boolean = false,
    @Serializable(with = FlexInt::class) @SerialName("tv_archive_duration") val tvArchiveDuration: Int = 0,
    @Serializable(with = FlexLongOrNull::class) val added: Long? = null,
)

// ---------------------------------------------------------------------------
// get_vod_streams / get_vod_info
// ---------------------------------------------------------------------------

@Serializable
data class VodStreamDto(
    @Serializable(with = FlexInt::class) @SerialName("stream_id") val streamId: Int = 0,
    val name: String = "",
    @SerialName("stream_icon") val streamIcon: String? = null,
    @Serializable(with = FlexString::class) val rating: String = "",
    @Serializable(with = FlexDouble::class) @SerialName("rating_5based") val rating5: Double = 0.0,
    @SerialName("container_extension") val containerExtension: String = "mp4",
    @Serializable(with = FlexString::class) @SerialName("category_id") val categoryId: String = "",
    @Serializable(with = FlexLongOrNull::class) val added: Long? = null,
)

@Serializable
data class VodInfoResponse(
    val info: VodDetailInfo? = null,
    @SerialName("movie_data") val movieData: MovieData? = null,
) {
    // `info` can be `[]` on some panels; a custom top-level decode keeps that safe.
    companion object {
        fun parse(json: String): VodInfoResponse =
            XtreamJson.decodeFromString(TolerantSerializer, json)
    }

    private object TolerantSerializer : KSerializer<VodInfoResponse> {
        private val infoSer = ObjectOrNull(VodDetailInfo.serializer())
        private val movieSer = ObjectOrNull(MovieData.serializer())
        override val descriptor: SerialDescriptor = serializer().descriptor
        override fun deserialize(decoder: Decoder): VodInfoResponse {
            val input = decoder as JsonDecoder
            val obj = input.decodeJsonElement() as? JsonObject ?: return VodInfoResponse()
            return VodInfoResponse(
                info = obj["info"]?.let {
                    input.json.decodeFromJsonElement(infoSer, it)
                },
                movieData = obj["movie_data"]?.let {
                    input.json.decodeFromJsonElement(movieSer, it)
                },
            )
        }

        override fun serialize(encoder: Encoder, value: VodInfoResponse) =
            serializer().serialize(encoder, value)
    }
}

@Serializable
data class VodDetailInfo(
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    @SerialName("releasedate") val releaseDate: String? = null,
    @Serializable(with = FlexString::class) val duration: String = "",
    @Serializable(with = FlexString::class) val rating: String = "",
    @SerialName("movie_image") val movieImage: String? = null,
    @Serializable(with = FlexStringList::class) @SerialName("backdrop_path")
    val backdropPath: List<String> = emptyList(),
    @SerialName("youtube_trailer") val youtubeTrailer: String? = null,
)

@Serializable
data class MovieData(
    @Serializable(with = FlexInt::class) @SerialName("stream_id") val streamId: Int = 0,
    val name: String = "",
    @SerialName("container_extension") val containerExtension: String = "mp4",
    @Serializable(with = FlexString::class) @SerialName("category_id") val categoryId: String = "",
)

// ---------------------------------------------------------------------------
// get_series / get_series_info
// ---------------------------------------------------------------------------

@Serializable
data class SeriesDto(
    @Serializable(with = FlexInt::class) @SerialName("series_id") val seriesId: Int = 0,
    val name: String = "",
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val genre: String? = null,
    @Serializable(with = FlexString::class) val rating: String = "",
    @Serializable(with = FlexDouble::class) @SerialName("rating_5based") val rating5: Double = 0.0,
    @Serializable(with = FlexStringList::class) @SerialName("backdrop_path")
    val backdropPath: List<String> = emptyList(),
    @Serializable(with = FlexString::class) @SerialName("category_id") val categoryId: String = "",
    @SerialName("releaseDate") val releaseDate: String? = null,
    @Serializable(with = FlexLongOrNull::class) @SerialName("last_modified") val lastModified: Long? = null,
)

@Serializable
data class SeriesInfoResponse(
    val info: SeriesDto? = null,
    @Serializable(with = EpisodesSerializer::class)
    val episodes: Map<String, List<EpisodeDto>> = emptyMap(),
) {
    companion object {
        fun parse(json: String): SeriesInfoResponse =
            XtreamJson.decodeFromString(TolerantSerializer, json)
    }

    private object TolerantSerializer : KSerializer<SeriesInfoResponse> {
        private val infoSer = ObjectOrNull(SeriesDto.serializer())
        override val descriptor: SerialDescriptor = serializer().descriptor
        override fun deserialize(decoder: Decoder): SeriesInfoResponse {
            val input = decoder as JsonDecoder
            val obj = input.decodeJsonElement() as? JsonObject ?: return SeriesInfoResponse()
            return SeriesInfoResponse(
                info = obj["info"]?.let { input.json.decodeFromJsonElement(infoSer, it) },
                episodes = obj["episodes"]?.let {
                    input.json.decodeFromJsonElement(EpisodesSerializer, it)
                } ?: emptyMap(),
            )
        }

        override fun serialize(encoder: Encoder, value: SeriesInfoResponse) =
            serializer().serialize(encoder, value)
    }
}

@Serializable
data class EpisodeDto(
    @Serializable(with = FlexInt::class) val id: Int = 0,
    @Serializable(with = FlexInt::class) @SerialName("episode_num") val episodeNum: Int = 0,
    val title: String = "",
    @SerialName("container_extension") val containerExtension: String = "mp4",
    @Serializable(with = FlexInt::class) val season: Int = 0,
    @Serializable(with = EpisodeInfoOrNull::class) val info: EpisodeInfo? = null,
)

/** Concrete wrapper so the serialization plugin can reference it by class. */
object EpisodeInfoOrNull : KSerializer<EpisodeInfo?> by ObjectOrNull(EpisodeInfo.serializer())

@Serializable
data class EpisodeInfo(
    val plot: String? = null,
    @SerialName("movie_image") val movieImage: String? = null,
    @Serializable(with = FlexString::class) val duration: String = "",
    @Serializable(with = FlexDouble::class) val rating: Double = 0.0,
)

/**
 * `episodes` is usually a map keyed by season number ({"1":[...]}) but some
 * panels return an array of season arrays ([[...],[...]]). Accept both.
 */
object EpisodesSerializer : KSerializer<Map<String, List<EpisodeDto>>> {
    private val mapSerializer =
        MapSerializer(String.serializer(), ListSerializer(EpisodeDto.serializer()))
    override val descriptor: SerialDescriptor = mapSerializer.descriptor

    override fun deserialize(decoder: Decoder): Map<String, List<EpisodeDto>> {
        val input = decoder as JsonDecoder
        return when (val el = input.decodeJsonElement()) {
            is JsonObject -> input.json.decodeFromJsonElement(mapSerializer, el)
            is JsonArray -> el.mapIndexedNotNull { index, seasonEl ->
                runCatching {
                    input.json.decodeFromJsonElement(
                        ListSerializer(EpisodeDto.serializer()), seasonEl
                    )
                }.getOrNull()?.let { episodes ->
                    val season = episodes.firstOrNull()?.season?.takeIf { it > 0 } ?: (index + 1)
                    season.toString() to episodes
                }
            }.toMap()
            else -> emptyMap()
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, List<EpisodeDto>>) {
        (encoder as JsonEncoder).encodeSerializableValue(mapSerializer, value)
    }
}

// ---------------------------------------------------------------------------
// get_short_epg / get_simple_data_table
// ---------------------------------------------------------------------------

@Serializable
data class ShortEpgResponse(
    @SerialName("epg_listings") val epgListings: List<EpgListingDto> = emptyList(),
)

@Serializable
data class EpgListingDto(
    @Serializable(with = FlexString::class) val id: String = "",
    /** Base64-encoded on the JSON EPG endpoints; decode via [EpgText]. */
    val title: String = "",
    /** Base64-encoded on the JSON EPG endpoints; decode via [EpgText]. */
    val description: String = "",
    @Serializable(with = FlexLongOrNull::class) @SerialName("start_timestamp") val startTimestamp: Long? = null,
    @Serializable(with = FlexLongOrNull::class) @SerialName("stop_timestamp") val stopTimestamp: Long? = null,
    @Serializable(with = FlexBool::class) @SerialName("now_playing") val nowPlaying: Boolean = false,
    @Serializable(with = FlexBool::class) @SerialName("has_archive") val hasArchive: Boolean = false,
)
