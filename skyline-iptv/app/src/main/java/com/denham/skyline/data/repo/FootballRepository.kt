package com.denham.skyline.data.repo

import android.util.Log
import com.denham.skyline.core.Fixture
import com.denham.skyline.core.FootballMapping
import com.denham.skyline.core.FootballMatchesResponse
import com.denham.skyline.core.XtreamJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * football-data.org v4 client for the David-only Home "Football" section.
 * Follows [YouTubeRepository]'s pattern: raw OkHttp against the shared
 * client from `AppContainer`, no Retrofit service, no key stored on the
 * class -- callers pass `BuildConfig.FOOTBALL_DATA_API_KEY` at call time so
 * this repository never becomes a second place a key could be cached or
 * logged.
 *
 * Free tier: ~13 major competitions, no friendlies/lower-tier football, and
 * a 10 req/min rate limit -- both calls below are only expected to run once
 * per `HomeViewModel` instantiation, not on every recomposition.
 */
class FootballRepository(
    private val okHttpClient: OkHttpClient,
) {
    /** Manchester United's football-data.org team id. Long-cited/stable
     *  across the API's community tooling, but not verified here against a
     *  live authenticated call -- flag for confirmation once a real key is
     *  available (see design.md open item). */
    private val manUtdTeamId = 66

    /** Today's fixtures across football-data.org's covered competitions.
     *  Returns an empty list -- never throws -- on a blank key, a non-2xx
     *  response, or a parse failure, so a repository/API problem degrades
     *  to "section doesn't render" rather than crashing Home. */
    suspend fun todaysFixtures(apiKey: String): List<Fixture> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()
        val today = LocalDate.now().format(DATE_FORMAT)
        val url = "$BASE_URL/matches?dateFrom=$today&dateTo=$today"
        fetchFixtures(url, apiKey)
    }

    /** Manchester United's next scheduled fixture, independent of whether
     *  it also appears in [todaysFixtures] -- the spotlight card answers
     *  "when do they play next" even outside a match day. */
    suspend fun nextManUtdFixture(apiKey: String): Fixture? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        val url = "$BASE_URL/teams/$manUtdTeamId/matches?status=SCHEDULED&limit=1"
        fetchFixtures(url, apiKey).firstOrNull()
    }

    private fun fetchFixtures(url: String, apiKey: String): List<Fixture> {
        val request = Request.Builder()
            .url(url)
            .addHeader("X-Auth-Token", apiKey)
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "football-data.org request failed: ${response.code}")
                    return@use emptyList()
                }
                val body = response.body?.string() ?: return@use emptyList()
                val parsed = XtreamJson.decodeFromString(FootballMatchesResponse.serializer(), body)
                FootballMapping.toFixtures(parsed.matches)
            }
        } catch (e: Exception) {
            // Credentials never appear in this URL (auth is a header, not a
            // query param, per the Redact.kt precedent for Xtream), so the
            // raw exception is safe to log -- no redaction needed here.
            Log.e(TAG, "football-data.org request failed", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "FootballRepository"
        private const val BASE_URL = "https://api.football-data.org/v4"
        private val DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
