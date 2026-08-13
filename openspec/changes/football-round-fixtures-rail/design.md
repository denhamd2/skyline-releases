## Context

`FootballRepository.todaysFixtures` currently calls `GET /v4/matches?
dateFrom=<today>&dateTo=<today>` with no competition filter, relying on
football-data.org's free tier covering ~13 major competitions to usually
return something. See proposal.md - Why for why that scope no longer holds.

## Goals / Non-Goals

**Goals:**
- Show the Premier League's current round of fixtures instead of today's
  cross-competition slice.
- Keep the two-call round-selection logic entirely inside
  `FootballRepository` -- `HomeViewModel`/`HomeScreen` only see the renamed
  `roundFixtures: List<Fixture>`, same shape as before.

**Non-Goals:**
- No change to the Man Utd next-fixture spotlight, `FixtureCard`, EPG
  channel-matching, or fixture-status/live-score mapping.
- No support for competitions other than the Premier League in the rail.

## Decisions

**Two-call round selection (`GET /v4/competitions/PL` then `GET /v4/
competitions/PL/matches?matchday=<n>`) over a single-call date-window +
client-side grouping heuristic.**

Considered: `dateFrom`/`dateTo` window sized to span a plausible round,
grouped client-side by taking the smallest `matchday` not yet
`FINISHED`. Rejected because:
- No safe window size reliably spans a Premier League round -- gaps around
  international breaks can exceed two weeks, so a fixed window either
  misses a sparse round or drags in an adjacent one.
- Treating "not `FINISHED`" as "round still current" is a correctness bug:
  `CANCELLED` and `AWARDED` are both real terminal statuses distinct from
  `FINISHED`, so a round containing either would never be judged complete,
  permanently pinning `roundFixtures` to a stale round.

The two-call approach costs one extra network round-trip per
`HomeViewModel` instantiation (not per recomposition), negligible against
the free tier's 10 req/min limit, and delegates round selection to the
API's own `currentSeason.currentMatchday` concept, eliminating this class
of bug entirely.

**Split `fetchFixtures` into `fetchMatches` (raw DTOs) + `fetchFixtures`
(thin `FootballMapping.toFixtures` wrapper).** `fetchCurrentMatchday` needs
a differently-shaped response (`FootballCompetitionDetailDto`, not
`FootballMatchesResponse`) from the same request/parse/failure-handling
scaffold; splitting keeps that scaffold reusable without a second copy of
the try/catch-and-log block.

**`matchday: Int? = null` added directly to the existing `FootballMatchDto`
rather than a new DTO.** The same match shape is decoded both by the
PL-round query (where `matchday` is always present) and by
`nextManUtdFixture`'s team-scoped query (which is not matchday-scoped) --
nullable keeps one DTO serving both call sites, consistent with this file's
existing "default rather than crash" convention.

## Risks / Trade-offs

- [Risk] `GET /v4/competitions/PL` could fail independently of the matches
  call, e.g. rate-limited after the Man Utd spotlight's call, blanking the
  rail even though matches would otherwise be available. -> Mitigation:
  `fetchCurrentMatchday` returns `null` on any failure (same
  never-throw contract as the rest of this file), which degrades to
  "rail doesn't render" -- consistent with this capability's existing
  graceful-degradation requirement, not a new failure mode.
- [Risk] Off-season (no active round) means `currentMatchday` may be absent
  or stale. -> Mitigation: covered by the "No current matchday available"
  spec scenario -- rail simply doesn't render, no fabricated data.
- [Risk] Not verifiable against a live key in this environment (see
  proposal.md - Impact). -> Mitigation: flagged for CI/QA confirmation once
  a real `FOOTBALL_DATA_API_KEY` call is possible.
