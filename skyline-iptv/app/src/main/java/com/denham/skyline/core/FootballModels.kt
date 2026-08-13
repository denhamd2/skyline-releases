package com.denham.skyline.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder

/**
 * football-data.org v4 JSON shapes (`GET /v4/matches`, `GET /v4/teams/{id}/
 * matches`). Unlike the Xtream panel DTOs in [XtreamModels], this API is
 * well-typed and consistent, so field-level serializers stay plain -- no
 * `Flex*` tolerance needed. Still kept defensive with default values so an
 * unexpected/missing field degrades to "fixture omitted" rather than a
 * decode crash (see [FootballMapping]).
 */
@Serializable
data class FootballMatchesResponse(
    @Serializable(with = TolerantMatchListSerializer::class)
    val matches: List<FootballMatchDto> = emptyList(),
)

/**
 * Decodes the `matches` array element-by-element instead of as one atomic
 * `List<FootballMatchDto>` -- the default kotlinx.serialization behaviour
 * for `List<T>` is all-or-nothing, so a single non-conformant match (e.g. an
 * unexpected type for `minute`) would otherwise fail the whole response and
 * blank today's entire fixtures list rather than just that one match.
 * Follows the same "tolerate one bad element from the provider" spirit as
 * the `Flex*`/[ObjectOrNull] serializers in `XtreamJson.kt`. Malformed
 * elements are silently skipped rather than logged -- this file is pure
 * Kotlin/`core` and has no Android `Log` import available; callers in
 * `data/repo` already log the outer request/parse failure.
 */
object TolerantMatchListSerializer : KSerializer<List<FootballMatchDto>> {
    private val elementSerializer = FootballMatchDto.serializer()
    override val descriptor: SerialDescriptor = ListSerializer(elementSerializer).descriptor

    override fun deserialize(decoder: Decoder): List<FootballMatchDto> {
        val input = decoder as JsonDecoder
        val array = input.decodeJsonElement() as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            runCatching { input.json.decodeFromJsonElement(elementSerializer, element) }.getOrNull()
        }
    }

    override fun serialize(encoder: Encoder, value: List<FootballMatchDto>) {
        val output = encoder as JsonEncoder
        output.encodeSerializableValue(ListSerializer(elementSerializer), value)
    }
}

@Serializable
data class FootballMatchDto(
    val id: Long = 0,
    @SerialName("utcDate") val utcDate: String = "",
    val status: String = "",
    val minute: Int? = null,
    // Present on every match returned by a matchday-filtered competition
    // query (e.g. `/v4/competitions/PL/matches?matchday=<n>`); nullable
    // since this same DTO also decodes team-scoped queries (e.g.
    // `nextManUtdFixture`'s `/v4/teams/{id}/matches`) that aren't
    // matchday-scoped.
    val matchday: Int? = null,
    val competition: FootballCompetitionDto = FootballCompetitionDto(),
    val homeTeam: FootballTeamDto = FootballTeamDto(),
    val awayTeam: FootballTeamDto = FootballTeamDto(),
    val score: FootballScoreDto = FootballScoreDto(),
)

@Serializable
data class FootballCompetitionDto(
    val id: Int = 0,
    val name: String = "",
    val code: String? = null,
    val emblem: String? = null,
)

@Serializable
data class FootballTeamDto(
    val id: Int = 0,
    val name: String = "",
    val shortName: String? = null,
    val tla: String? = null,
    val crest: String? = null,
)

@Serializable
data class FootballScoreDto(
    val fullTime: FootballScoreLineDto = FootballScoreLineDto(),
)

/**
 * football-data.org sets `score.fullTime` to `{home: 0, away: 0}` the moment
 * a match goes `IN_PLAY` and keeps it current until `FINISHED` -- it doubles
 * as the running score. There is no separate live-score field to decode.
 */
@Serializable
data class FootballScoreLineDto(
    val home: Int? = null,
    val away: Int? = null,
)

/**
 * `GET /v4/competitions/{code}` response shapes, used only to read off
 * `currentSeason.currentMatchday` for round selection (see
 * [FootballRepository.fetchCurrentMatchday]) -- kept minimal rather than
 * modelling the full competition-detail response.
 */
@Serializable
data class FootballCompetitionDetailDto(
    val currentSeason: FootballSeasonDto = FootballSeasonDto(),
)

@Serializable
data class FootballSeasonDto(
    val currentMatchday: Int? = null,
)
