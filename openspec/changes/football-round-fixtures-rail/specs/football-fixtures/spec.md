## RENAMED Requirements

- FROM: `### Requirement: Today's fixtures rail`
- TO: `### Requirement: Upcoming Premier League round rail`

## MODIFIED Requirements

### Requirement: Upcoming Premier League round rail

When David is selected, a key is configured, and the Premier League has a
current round with at least one fixture, the Home screen SHALL show a
horizontal rail of that round's fixtures.

The round is selected via football-data.org's own round semantics: the
Premier League competition's `currentSeason.currentMatchday`, not a
client-side date window or status-based grouping. This avoids two
correctness problems a date-window/grouping heuristic would have: no safe
window size reliably spans a round (Premier League gaps around
international breaks can exceed two weeks), and treating any non-`FINISHED`
status as "round still current" gets stuck forever on a round containing a
`CANCELLED` or `AWARDED` match -- both real terminal statuses distinct from
`FINISHED`.

Live-score rendering for an in-progress fixture (the "Fixture status
reflects match state" requirement's Live scenario: `IN_PLAY`/`PAUSED`
showing a live badge, current score, and minute) is unchanged and
inherited as-is -- this change only affects which fixtures populate the
rail, not how any individual fixture card renders.

#### Scenario: Fixtures today

<!-- Retained name for continuity with the prior "Today's fixtures rail"
requirement this replaces (renamed above); the trigger and content are
rewritten for round-based selection, not a date match. -->
- **WHEN** football-data.org's Premier League competition detail returns a
  `currentSeason.currentMatchday`, and the matchday-filtered matches query
  for that round returns one or more matches
- **THEN** each renders as a fixture card showing competition, teams,
  status (scheduled/live/finished), and any matched EPG channels

#### Scenario: No fixtures today

<!-- Retained name for continuity with the prior requirement; rewritten to
cover both round-selection failure modes below. -->
- **WHEN** football-data.org's Premier League competition detail returns no
  `currentSeason.currentMatchday` (e.g. off-season, API outage, or parse
  failure), **OR** a `currentMatchday` is returned but the matchday-filtered
  matches query returns no matches (e.g. all fixtures in that round were
  postponed/rescheduled out of it)
- **THEN** the rail does not render (no empty-state card), matching this
  capability's existing graceful-degradation behaviour for a missing key or
  failed request
