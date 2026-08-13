## Why

The David-only "Football" Home section's fixtures rail currently shows
today's fixtures across every competition football-data.org's free tier
covers -- a scope chosen to compensate for the tier not covering
international friendlies, casting a wide net so there was usually
*something* to show. The user has confirmed directly against the API key
that the tier doesn't support friendlies, so that justification no longer
holds. A single day's cross-competition slice is also a thin view; the
user wants the rail to show the upcoming round of Premier League fixtures
instead -- a more useful "what's on this week" view, with live scores for
any fixture currently in progress (already-existing behaviour, unchanged).

## What Changes

- Replace the fixtures rail's data source: instead of `GET /v4/matches?
  dateFrom=<today>&dateTo=<today>` across all covered competitions, fetch
  the Premier League's current round via two calls -- `GET /v4/competitions/
  PL` for `currentSeason.currentMatchday`, then `GET /v4/competitions/PL/
  matches?matchday=<n>` for that round's fixtures.
- Add `matchday` to the shared match DTO and two new DTOs
  (`FootballSeasonDto`, `FootballCompetitionDetailDto`) for decoding the
  competition-detail response.
- Rename `FootballRepository.todaysFixtures` to `upcomingPremierLeagueRound`
  and `FootballSectionState.Loaded.todaysFixtures` to `roundFixtures`
  throughout `HomeScreen.kt` -- naming only, no other behavioural change to
  how fixtures render.
- No change to the Man Utd next-fixture spotlight, `FixtureCard`, EPG
  channel-matching, or the live-score status mapping (`IN_PLAY`/`PAUSED`) --
  all of that is reused as-is.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `football-fixtures`: the "Today's fixtures rail" requirement changes
  scope from "today's fixtures across football-data.org's free-tier
  competitions" to "the upcoming round of Premier League fixtures,
  selected via `currentSeason.currentMatchday`". The fixture-status/
  live-score, channel-linking, David-only-visibility, and no-API-key
  requirements are unchanged.

## Impact

- **Modified files**: `core/FootballModels.kt` (new DTOs + `matchday`
  field), `data/repo/FootballRepository.kt` (split `fetchFixtures`, new
  `fetchCurrentMatchday`/`upcomingPremierLeagueRound`), `ui/home/
  HomeScreen.kt` (rename `todaysFixtures` -> `roundFixtures`), `app/src/
  test/java/com/denham/skyline/core/FootballParsingTest.kt` (new decoding
  tests).
- **API usage**: adds one extra football-data.org request
  (`GET /v4/competitions/PL`) per `HomeViewModel` instantiation -- well
  within the free tier's 10 req/min limit since this doesn't fire per
  recomposition.
- **Not verifiable in this environment**: no device/emulator and the AGP
  cannot be resolved here, so this cannot be compiled or run on-device, nor
  can a live call confirm `GET /v4/competitions/PL`'s actual
  `currentSeason.currentMatchday` shape or that a matchday-filtered query
  returns a sane round size -- flagged for CI/QA to confirm against a real
  key.
