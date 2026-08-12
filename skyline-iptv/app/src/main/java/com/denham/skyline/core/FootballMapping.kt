package com.denham.skyline.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** A football-data.org match, mapped to what the UI actually needs -- no
 *  raw API JSON shape leaks past this file. */
data class Fixture(
    val id: Long,
    val competition: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeCrestUrl: String?,
    val awayCrestUrl: String?,
    val status: FixtureStatus,
    val kickoffMs: Long,
)

/**
 * The three states a fixture card renders. football-data.org's raw status
 * string carries more values than this (POSTPONED, CANCELLED, SUSPENDED,
 * AWARDED, ...); anything outside SCHEDULED/TIMED, IN_PLAY/PAUSED, FINISHED
 * maps to no [FixtureStatus] at all -- see [FootballMapping.toFixture] --
 * rather than being guessed at as one of the three.
 */
sealed interface FixtureStatus {
    data class Scheduled(val kickoffLocal: String) : FixtureStatus
    data class Live(val minute: String, val homeScore: Int, val awayScore: Int) : FixtureStatus
    data class Finished(val homeScore: Int, val awayScore: Int) : FixtureStatus
}

/** DTO -> domain mapping for football-data.org matches. Pure Kotlin, no
 *  Android imports, unit-tested on the JVM (see FootballParsingTest). */
object FootballMapping {

    private val kickoffFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Maps one match DTO to a [Fixture], or null when the match cannot be
     * placed in a renderable state -- a malformed/missing kickoff date, or a
     * status this UI doesn't have a state for (postponed/cancelled/etc).
     * Callers filter nulls out rather than rendering a guessed state.
     */
    fun toFixture(dto: FootballMatchDto): Fixture? {
        val kickoffMs = parseUtcDate(dto.utcDate) ?: return null
        val status = mapStatus(dto, kickoffMs) ?: return null
        return Fixture(
            id = dto.id,
            competition = dto.competition.name,
            homeTeam = dto.homeTeam.shortName?.takeIf { it.isNotBlank() } ?: dto.homeTeam.name,
            awayTeam = dto.awayTeam.shortName?.takeIf { it.isNotBlank() } ?: dto.awayTeam.name,
            homeCrestUrl = dto.homeTeam.crest,
            awayCrestUrl = dto.awayTeam.crest,
            status = status,
            kickoffMs = kickoffMs,
        )
    }

    fun toFixtures(dtos: List<FootballMatchDto>): List<Fixture> =
        dtos.mapNotNull { toFixture(it) }

    private fun mapStatus(dto: FootballMatchDto, kickoffMs: Long): FixtureStatus? =
        when (dto.status.uppercase()) {
            "SCHEDULED", "TIMED" -> FixtureStatus.Scheduled(formatKickoffLocal(kickoffMs))
            "IN_PLAY", "PAUSED" -> FixtureStatus.Live(
                minute = "${dto.minute ?: 0}′", // prime symbol, e.g. "63′"
                homeScore = dto.score.fullTime.home ?: 0,
                awayScore = dto.score.fullTime.away ?: 0,
            )
            "FINISHED" -> FixtureStatus.Finished(
                homeScore = dto.score.fullTime.home ?: 0,
                awayScore = dto.score.fullTime.away ?: 0,
            )
            else -> null
        }

    private fun parseUtcDate(utcDate: String): Long? =
        try {
            Instant.parse(utcDate).toEpochMilli()
        } catch (e: DateTimeParseException) {
            null
        }

    private fun formatKickoffLocal(kickoffMs: Long): String =
        "KO " + Instant.ofEpochMilli(kickoffMs)
            .atZone(ZoneId.systemDefault())
            .format(kickoffFormatter)
}
