package com.denham.skyline.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FootballParsingTest {

    private fun matchJson(
        status: String,
        minute: Int? = null,
        homeScore: Int? = null,
        awayScore: Int? = null,
        utcDate: String = "2026-08-12T19:00:00Z",
    ) = """
        {
          "id": 12345,
          "utcDate": "$utcDate",
          "status": "$status",
          "minute": ${minute ?: "null"},
          "competition": {"id": 2021, "name": "Premier League", "code": "PL", "emblem": "http://x/pl.png"},
          "homeTeam": {"id": 66, "name": "Manchester United FC", "shortName": "Man United", "tla": "MUN", "crest": "http://x/home.png"},
          "awayTeam": {"id": 65, "name": "Manchester City FC", "shortName": "Man City", "tla": "MCI", "crest": "http://x/away.png"},
          "score": {"fullTime": {"home": ${homeScore ?: "null"}, "away": ${awayScore ?: "null"}}}
        }
    """.trimIndent()

    private fun decode(json: String): FootballMatchDto =
        XtreamJson.decodeFromString(FootballMatchDto.serializer(), json)

    @Test
    fun `scheduled match maps to Scheduled status with local kickoff`() {
        val dto = decode(matchJson(status = "SCHEDULED"))
        val fixture = FootballMapping.toFixture(dto)!!
        assertTrue(fixture.status is FixtureStatus.Scheduled)
        assertEquals("Man United", fixture.homeTeam)
        assertEquals("Man City", fixture.awayTeam)
        assertEquals("Premier League", fixture.competition)
        val scheduled = fixture.status as FixtureStatus.Scheduled
        assertTrue(scheduled.kickoffLocal.startsWith("KO "))
    }

    @Test
    fun `timed status also maps to Scheduled`() {
        val dto = decode(matchJson(status = "TIMED"))
        val fixture = FootballMapping.toFixture(dto)!!
        assertTrue(fixture.status is FixtureStatus.Scheduled)
    }

    @Test
    fun `in_play match maps to Live with score and minute`() {
        val dto = decode(matchJson(status = "IN_PLAY", minute = 63, homeScore = 2, awayScore = 1))
        val fixture = FootballMapping.toFixture(dto)!!
        val live = fixture.status as FixtureStatus.Live
        assertEquals("63′", live.minute)
        assertEquals(2, live.homeScore)
        assertEquals(1, live.awayScore)
    }

    @Test
    fun `paused match also maps to Live`() {
        val dto = decode(matchJson(status = "PAUSED", minute = 45, homeScore = 0, awayScore = 0))
        val fixture = FootballMapping.toFixture(dto)!!
        assertTrue(fixture.status is FixtureStatus.Live)
    }

    @Test
    fun `finished match maps to Finished with final score`() {
        val dto = decode(matchJson(status = "FINISHED", homeScore = 3, awayScore = 1))
        val fixture = FootballMapping.toFixture(dto)!!
        val finished = fixture.status as FixtureStatus.Finished
        assertEquals(3, finished.homeScore)
        assertEquals(1, finished.awayScore)
    }

    @Test
    fun `unrecognised status maps to no fixture`() {
        for (status in listOf("POSTPONED", "CANCELLED", "SUSPENDED", "AWARDED")) {
            val dto = decode(matchJson(status = status))
            assertNull("status=$status should not map to a fixture", FootballMapping.toFixture(dto))
        }
    }

    @Test
    fun `missing score fields default to zero rather than crashing`() {
        val dto = decode(matchJson(status = "IN_PLAY", minute = 1))
        val fixture = FootballMapping.toFixture(dto)!!
        val live = fixture.status as FixtureStatus.Live
        assertEquals(0, live.homeScore)
        assertEquals(0, live.awayScore)
    }

    @Test
    fun `malformed kickoff date yields no fixture`() {
        val dto = decode(matchJson(status = "SCHEDULED", utcDate = "not-a-date"))
        assertNull(FootballMapping.toFixture(dto))
    }

    @Test
    fun `matches response with empty list parses`() {
        val resp = XtreamJson.decodeFromString(
            FootballMatchesResponse.serializer(),
            """{"matches": []}""",
        )
        assertTrue(resp.matches.isEmpty())
    }

    @Test
    fun `matches response with several matches maps and filters unrecognised`() {
        val json = """
            {"matches": [
              ${matchJson(status = "SCHEDULED")},
              ${matchJson(status = "POSTPONED")},
              ${matchJson(status = "FINISHED", homeScore = 1, awayScore = 0)}
            ]}
        """.trimIndent()
        val resp = XtreamJson.decodeFromString(FootballMatchesResponse.serializer(), json)
        val fixtures = FootballMapping.toFixtures(resp.matches)
        assertEquals(2, fixtures.size)
    }

    @Test
    fun `unknown keys in match json are ignored`() {
        val json = """
            {"id": 1, "utcDate": "2026-08-12T19:00:00Z", "status": "SCHEDULED",
             "someNewField": {"x": 1}, "group": "GROUP_A",
             "competition": {"id": 1, "name": "Champions League"},
             "homeTeam": {"id": 1, "name": "Team A"},
             "awayTeam": {"id": 2, "name": "Team B"},
             "score": {"fullTime": {"home": null, "away": null}}}
        """.trimIndent()
        val dto = decode(json)
        assertEquals("Champions League", dto.competition.name)
    }

    @Test
    fun `blank competition name decodes fine and maps to blank competition`() {
        val json = """
            {"id": 1, "utcDate": "2026-08-12T19:00:00Z", "status": "SCHEDULED",
             "competition": {"id": 1, "name": ""},
             "homeTeam": {"id": 1, "name": "Team A"},
             "awayTeam": {"id": 2, "name": "Team B"},
             "score": {"fullTime": {"home": null, "away": null}}}
        """.trimIndent()
        val dto = decode(json)
        val fixture = FootballMapping.toFixture(dto)!!
        assertEquals("", fixture.competition)
    }

    @Test
    fun `missing competition name defaults to blank rather than crashing`() {
        val json = """
            {"id": 1, "utcDate": "2026-08-12T19:00:00Z", "status": "SCHEDULED",
             "competition": {"id": 1},
             "homeTeam": {"id": 1, "name": "Team A"},
             "awayTeam": {"id": 2, "name": "Team B"},
             "score": {"fullTime": {"home": null, "away": null}}}
        """.trimIndent()
        val dto = decode(json)
        val fixture = FootballMapping.toFixture(dto)!!
        assertEquals("", fixture.competition)
    }

    @Test
    fun `team names with SQL LIKE wildcards pass through the domain model unchanged`() {
        val json = """
            {"id": 1, "utcDate": "2026-08-12T19:00:00Z", "status": "SCHEDULED",
             "competition": {"id": 1, "name": "Test League"},
             "homeTeam": {"id": 1, "name": "100% United_FC"},
             "awayTeam": {"id": 2, "name": "St._Mary%s"},
             "score": {"fullTime": {"home": null, "away": null}}}
        """.trimIndent()
        val dto = decode(json)
        val fixture = FootballMapping.toFixture(dto)!!
        // No shortName present, so the mapping falls back to the raw name --
        // confirms FootballMapping.toFixture doesn't escape/mangle these
        // characters. Any SQL LIKE-escaping happens in GuideDao, not here.
        assertEquals("100% United_FC", fixture.homeTeam)
        assertEquals("St._Mary%s", fixture.awayTeam)
    }

    @Test(expected = Exception::class)
    fun `non-integer minute value fails to decode FootballMatchDto`() {
        // QA-flagged behaviour: minute is typed Int? in FootballMatchDto, so
        // a provider sending "45+2" (a string, not an int) throws during
        // decode rather than gracefully defaulting -- this is exactly the
        // shape of malformed element TolerantMatchListSerializer guards
        // against at the list level (see the response-level test below).
        decode(matchJson(status = "IN_PLAY").replace("\"minute\": null", "\"minute\": \"45+2\""))
    }

    @Test
    fun `one malformed match among several valid ones does not blank the whole response`() {
        val malformed = matchJson(status = "IN_PLAY")
            .replace("\"minute\": null", "\"minute\": \"45+2\"")
        val json = """
            {"matches": [
              ${matchJson(status = "SCHEDULED")},
              $malformed,
              ${matchJson(status = "FINISHED", homeScore = 1, awayScore = 0)}
            ]}
        """.trimIndent()
        val resp = XtreamJson.decodeFromString(FootballMatchesResponse.serializer(), json)
        // The malformed element is skipped -- the two valid matches still
        // decode, rather than the whole `matches` list coming back empty.
        assertEquals(2, resp.matches.size)
        val fixtures = FootballMapping.toFixtures(resp.matches)
        assertEquals(2, fixtures.size)
    }
}
