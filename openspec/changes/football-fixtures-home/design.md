## Context

`HomeScreen.kt`/`HomeViewModel` already has a per-family-member selector
(`selectedFamilyMember`), a pinned-channels rail, and a YouTube carousel
scoped the same way — the football section follows that exact structural
precedent (`combine(_selectedFamilyMember, ...)`, `stateIn` with
`WhileSubscribed(5000)`). `YouTubeRepository` is the closest precedent for
"raw OkHttp against a keyed third-party JSON API, keyless-degradable,
cached in Room." `data/db/Daos.kt`'s `GuideDao.forChannelsBetween` is the
closest precedent for a windowed EPG lookup, but takes an explicit
channel-id list rather than a title search.

Full behavioural contract: `openspec/changes/football-fixtures-home/specs/
football-fixtures/spec.md`. Full visual/placement spec:
`design/david-football-fixtures.md`.

## Goals / Non-Goals

**Goals:**
- Implement the design brief precisely: placement, composition, `FixtureCard`
  contract, states table, and coverage-caveat copy.
- Keep football-data.org's API key out of the repository layer and scoped
  through `BuildConfig`, matching `YOUTUBE_API_KEY`.
- Keep DTOs and status-mapping pure Kotlin in `core/`, unit-testable on the
  JVM without a device.

**Non-Goals:**
- No dedicated fixtures list screen / `onViewAll` deep link (brief:
  explicitly out of scope).
- No TV port of `FixtureCard` (brief: phone-only for this change; TV would
  need D-pad-navigable chips, flagged not implemented).
- No caching/persistence layer (Room table) for fixtures in this pass —
  see Decisions below for why an in-memory/StateFlow cache is enough here.
- No changes to `epg_now_next`'s existing schema or Xtream sync path.

## Decisions

**Networking: raw OkHttp, not a second Retrofit service.**
Matches `YouTubeRepository`'s precedent exactly (`okHttpClient.newCall(...)`,
`org.json.JSONObject` parsing at the repo boundary, DTOs/pure-mapping in
`core/`). A second Retrofit service interface is equally valid per the task
brief but would be the first Retrofit definition outside `data/api`'s
Xtream client, adding a second HTTP client pattern to learn for a single
external call shape. Raw OkHttp keeps one pattern for "second/etc.
third-party API integration" in the codebase.

**DTO layer: `core/FootballModels.kt`, pure Kotlin, kotlinx.serialization.**
Per `README.md`'s architecture table, `core/` carries defensive DTOs unit
tested on the JVM (`XtreamParsingTest` is the precedent). football-data.org
JSON is well-typed and consistent (unlike Xtream's 0/1-boolean, string-or-int
mess), so the DTOs here are plainer than `XtreamJson`'s `Flex*` serializers —
still `core/`, still no Android imports, still tested the same way.

**Status mapping: a small sealed `FixtureStatus` derived in `core/`, not
passed the raw API enum string to the UI.**
The API's status enum has more values than the UI needs
(`SCHEDULED`/`TIMED`/`IN_PLAY`/`PAUSED`/`FINISHED`/`POSTPONED`/`CANCELLED`/
`SUSPENDED`/`AWARDED`/…). Mapping in `core/` means: `SCHEDULED`+`TIMED` →
`Scheduled`, `IN_PLAY`+`PAUSED` → `Live`, `FINISHED` → `Finished`, anything
else → excluded from the rendered list (per spec's "unrecognised status"
scenario) rather than guessed at. This keeps `HomeViewModel`/`HomeScreen`
free of API-specific enum knowledge, matching how Xtream's own status codes
never leak past `core/`/`data/repo`.

**Live score source: `score.fullTime`, not a separate live-score field.**
Public documentation confirms football-data.org sets `score.fullTime` to
`0-0` the moment a match goes `IN_PLAY` and keeps it current until
`FINISHED` — it doubles as the running score, there is no separate
in-progress field. This resolves the brief's flagged "likely a live
in-progress score field — verify" open question: no separate field exists
to verify.

**Team id 66 for Manchester United: proceed, but flag for a live-key check.**
Corroborated via public sources (well-established, long-cited id across the
football-data.org community/tooling ecosystem) but not verified against a
live authenticated call in this environment (no key obtainable in-session,
egress to football-data.org's docs domain is blocked here). Wiring uses a
single named constant (`MANCHESTER_UNITED_TEAM_ID = 66`) so a wrong value is
a one-line fix once a maintainer confirms with a real key/call.

**No local Room cache/persistence for fixtures in this change.**
`HomeViewModel` already re-syncs on every load (`syncAll()` in `init`), and
fixtures are inherently time-decaying (today only) — persisting them past
the current process life adds a cache-invalidation surface for content
that's worthless tomorrow. `FootballRepository` returns fresh `StateFlow`s
backed by a coroutine fetch + in-memory `MutableStateFlow`, refreshed once
per `HomeViewModel` instantiation (same cadence as `youtubeVideos`/hero
backdrop). This is a deliberate scope cut versus `YouTubeRepository`'s Room
cache — YouTube videos persist meaningfully across sessions (a subscription
feed), fixtures for "today" do not carry over to tomorrow's session in any
useful way. If poll-while-open (live score refresh) is wanted later, it's an
additive `viewModelScope` ticker, not a schema change.

**EPG matching: new `GuideDao` title-LIKE query, not `epg_now_next`.**
Brief flags this as the developer's call with no UI difference. Chose the
LIKE-query path because it covers the Scheduled and Finished states too
(kickoff ± 90 min window), not just "is this live right now" — `epg_now_next`
only knows current programme, so a fixture kicking off in two hours would
never resolve a channel until it starts, but the design's Scheduled-state
card is meant to be actionable/informative before kickoff as well.

## Risks / Trade-offs

- [Free-tier rate limit (10 req/min) could be hit if `HomeViewModel` re-fires
  on every recomposition] → guarded the same way `youtubeVideos`/EPG refresh
  already are: fetch triggered once in a `combine`/`flatMapLatest` chain
  keyed on `selectedFamilyMember`, not on every recomposition; `stateIn`
  with `WhileSubscribed(5000)` caches across quick navigation.
- [Team id 66 unverified against a live call] → flagged above and in the
  proposal; a wrong id degrades to "Man Utd next fixture never appears"
  (caught by the spec's "no next fixture" scenario), not a crash.
- [Title-LIKE EPG matching can false-positive/false-negative on team-name
  variants ("Man Utd" vs "Manchester United" vs "Man United") in provider
  guide data] → accepted per brief ("no UI-visible difference" either way);
  not a correctness contract this change can guarantee against arbitrary
  provider guide text.
- [No live device/CI run in this environment] → explicitly named as
  unverified in the final summary; CI is the actual compiler per
  `skyline-iptv/CLAUDE.md`.

## Open Questions

- Whether `FOOTBALL_DATA_API_KEY` should be added as a CI secret now or
  left for a maintainer to add later — the `BuildConfig` wiring degrades
  safely either way, so this can be resolved after merge without touching
  specs/approach.
