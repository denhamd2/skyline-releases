# football-fixtures Specification

## Purpose
Gives David a Home-screen view of Manchester United's next fixture and
today's football across the competitions football-data.org's free tier
covers, each linking to any EPG channel currently/soon carrying the match.
## Requirements
### Requirement: Football section visible only for David

The Home screen SHALL show a "Football" section only when the selected
family member is "David". No other family member profile (Anne, Ava,
Sophie) or the default/no-member page SHALL show it.

#### Scenario: David selected

- **WHEN** the selected family member is "David"
- **THEN** the Football section (Man Utd spotlight + today's fixtures rail)
  is eligible to render, subject to data/key availability below

#### Scenario: Any other member or no member selected

- **WHEN** the selected family member is "Anne", "Ava", "Sophie", or none
- **THEN** the Football section does not render

### Requirement: Graceful degradation with no API key

The Football section SHALL NOT render, and SHALL NOT show an error, when no
football-data.org API key is configured.

#### Scenario: Blank API key

- **WHEN** `FOOTBALL_DATA_API_KEY` is blank at build time
- **THEN** neither the Man Utd spotlight nor the today's-fixtures rail
  renders, and no error/placeholder card is shown in their place

### Requirement: Man Utd next-fixture spotlight

When David is selected and a key is configured, the Home screen SHALL show
a single spotlight card for Manchester United's next scheduled fixture,
independent of whether that fixture also appears live in today's rail.

#### Scenario: Next fixture available

- **WHEN** football-data.org returns a next scheduled fixture for
  Manchester United
- **THEN** the spotlight card shows competition, both teams' names/crests,
  and kickoff time (or live/finished state if the fixture has since started)

#### Scenario: No next fixture available

- **WHEN** football-data.org returns no scheduled fixture for Manchester
  United (API outage or none within lookahead)
- **THEN** the spotlight sub-block is omitted, independent of whether the
  today's-fixtures rail renders

### Requirement: Today's fixtures rail

When David is selected, a key is configured, and at least one fixture is
scheduled/live/finished today in football-data.org's covered competitions,
the Home screen SHALL show a horizontal rail of those fixtures.

#### Scenario: Fixtures today

- **WHEN** football-data.org returns one or more matches for today's date
- **THEN** each renders as a fixture card showing competition, teams,
  status (scheduled/live/finished), and any matched EPG channels

#### Scenario: No fixtures today

- **WHEN** football-data.org returns no matches for today's date across
  covered competitions
- **THEN** the today's-fixtures rail does not render (no empty-state card)

### Requirement: Fixture status reflects match state

Each fixture card SHALL reflect one of three visual states derived from the
API status: Scheduled (not yet kicked off), Live (in progress), or Finished.

#### Scenario: Scheduled fixture

- **WHEN** a fixture's API status is `SCHEDULED` or `TIMED`
- **THEN** the card shows the local kickoff time and no live badge or score

#### Scenario: Live fixture

- **WHEN** a fixture's API status is `IN_PLAY` or `PAUSED`
- **THEN** the card shows a live badge, the current score, and the match
  minute

#### Scenario: Finished fixture

- **WHEN** a fixture's API status is `FINISHED`
- **THEN** the card shows "FT" and the final score, with lower visual
  emphasis than a live card, and remains in the list rather than
  disappearing

#### Scenario: Unrecognised or non-played status

- **WHEN** a fixture's API status is something other than the above (e.g.
  `POSTPONED`, `CANCELLED`, `SUSPENDED`)
- **THEN** the fixture is handled defensively (not crash, not misrepresented
  as scheduled/live/finished) — excluded from the rendered list rather than
  shown with a fabricated state

### Requirement: Fixture-to-channel linking

Each fixture SHALL link to zero, one, or more EPG channels found to be
carrying that match around its kickoff time, resolved to real channel
records — never a fabricated or placeholder channel.

#### Scenario: No channel match

- **WHEN** no EPG channel's programme title matches the fixture's teams
  within its kickoff window
- **THEN** the card shows muted "Not on your channels" text and is not
  clickable

#### Scenario: Exactly one channel match
- **WHEN** exactly one EPG channel matches
- **THEN** the whole card is clickable and plays that channel directly

#### Scenario: Multiple channel matches

- **WHEN** two or more EPG channels match
- **THEN** the card itself is not clickable; each matched channel appears as
  an individually tappable chip that plays that specific channel

