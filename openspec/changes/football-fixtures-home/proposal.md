## Why

David's Home tab has no sports-fixtures content today, despite football/
soccer/sport leading his keyword defaults (`familyKeywordDefaults["David"]`
in `HomeScreen.kt`). His personalized rails only ever surface whatever the
Xtream provider's *category names* happen to match, which says nothing about
what's actually on today or when Man Utd play next. A `ux-design` brief
(`design/david-football-fixtures.md`) specifies a David-only "Football"
section on Home — a Man Utd next-fixture spotlight plus a rail of today's
covered fixtures, each linking to any EPG channel currently/soon carrying
the match.

## What Changes

- Add a new `data/repo` repository (`FootballRepository`) that fetches
  today's fixtures and Manchester United's next fixture from the
  football-data.org v4 API, following `YouTubeRepository`'s raw-OkHttp
  pattern (no second Retrofit service).
- Add pure-Kotlin `core/` DTOs for football-data.org's match/team/competition
  JSON shape, tolerant of the enum status values the API can return
  (`SCHEDULED`, `TIMED`, `IN_PLAY`, `PAUSED`, `FINISHED`, `POSTPONED`,
  `CANCELLED`, etc.), unit-tested on the JVM.
- Add a new `GuideDao` query to match a fixture (home/away team names) to
  EPG-carrying channels within a kickoff-window, reusing `ChannelDao.byIds`
  to resolve real `ChannelEntity`s — no new channel model.
- Add `FOOTBALL_DATA_API_KEY` to `app/build.gradle.kts` as a `BuildConfig`
  field, following `YOUTUBE_API_KEY`'s exact wiring (env var at build time,
  empty string when absent, never hardcoded, never stored on the
  repository).
- Add a new `FixtureCard` composable to `ui/components/Components.kt`
  (justified in the design brief as a legitimate new card type, not a
  duplicate of `LiveNowRow`/`ChannelCard`/`PosterCard`).
- Insert a David-only "Football" section into `HomeScreen.kt`, between the
  pinned-channels rail and the YouTube carousel, per the brief's placement
  and composition spec (spotlight card + `Rail` of `FixtureCard`s, shimmer
  loading states, graceful no-render when the key is blank or there's no
  data).
- Wire `FootballRepository` into `di/AppContainer.kt` (`by lazy`), same
  pattern as every other repository there.

## Capabilities

### New Capabilities
- `football-fixtures`: David-only Home section showing Manchester United's
  next fixture and today's fixtures across football-data.org's free-tier
  competitions, each linking to matched EPG channels; degrades to
  "section doesn't render" when the API key is absent or there's no data.

### Modified Capabilities
(none — this is additive to `HomeScreen`; no existing capability's
requirements change)

## Impact

- **New files**: `core/FootballModels.kt` (DTOs), `data/repo/
  FootballRepository.kt`, unit tests under `app/src/test/java/com/denham/
  skyline/core/`.
- **Modified files**: `app/build.gradle.kts` (new `FOOTBALL_DATA_API_KEY`
  build config field), `di/AppContainer.kt` (new repository wiring),
  `data/db/Daos.kt` (new `GuideDao` title-match query), `ui/components/
  Components.kt` (new `FixtureCard`), `ui/home/HomeScreen.kt` (new section +
  `HomeViewModel` state).
- **New third-party dependency**: football-data.org v4 REST API (free tier,
  ~13 major competitions, no friendlies/lower-tier coverage — a data-source
  limitation the section header copy ("Football", not "All football today")
  already accounts for per the brief).
- **Secrets/CI**: needs a `FOOTBALL_DATA_API_KEY` secret configured for CI
  the same way `YOUTUBE_API_KEY` is; this proposal wires the `BuildConfig`
  read but does not itself have a live key to test against in this
  environment — the feature must degrade cleanly (section omitted) with a
  blank key, which is directly testable.
- **Not verifiable in this environment**: no device/emulator and the AGP
  cannot be resolved here, so this cannot be compiled or run on-device.
  Also cannot make a live call to football-data.org to confirm the exact
  response shape or that team id 66 is Manchester United — implementing
  against the well-established public documentation of this long-stable API
  and flagging both as open items for CI/QA to confirm.
