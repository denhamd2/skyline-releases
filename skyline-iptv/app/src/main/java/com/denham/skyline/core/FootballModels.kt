package com.denham.skyline.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * football-data.org v4 JSON shapes (`GET /v4/matches`, `GET /v4/teams/{id}/
 * matches`). Unlike the Xtream panel DTOs in [XtreamModels], this API is
 * well-typed and consistent, so these serializers stay plain -- no `Flex*`
 * tolerance needed. Still kept defensive with default values so an
 * unexpected/missing field degrades to "fixture omitted" rather than a
 * decode crash (see [FootballMapping]).
 */
@Serializable
data class FootballMatchesResponse(
    val matches: List<FootballMatchDto> = emptyList(),
)

@Serializable
data class FootballMatchDto(
    val id: Long = 0,
    @SerialName("utcDate") val utcDate: String = "",
    val status: String = "",
    val minute: Int? = null,
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
