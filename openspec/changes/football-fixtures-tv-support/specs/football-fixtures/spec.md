## ADDED Requirements

### Requirement: TV Football section visible only for David

The TV Home screen SHALL show a "Football" section only when the selected
family member is "David", using the same `selectedFamilyMember` state
phone reads.

#### Scenario: David selected on TV

- **WHEN** the selected family member is "David"
- **THEN** the TV Football section (Man Utd spotlight + upcoming-round
  list) is eligible to render, subject to the same data/key availability
  as phone

#### Scenario: Any other member or no member selected on TV

- **WHEN** the selected family member is "Anne", "Ava", "Sophie", or none
- **THEN** the TV Football section does not render

### Requirement: TV Man Utd next-fixture spotlight

When David is selected and a key is configured, TV Home SHALL show a
spotlight card for Manchester United's next scheduled fixture, using the
same data as the phone spotlight and rendered with a D-pad-focusable TV
fixture card.

#### Scenario: Next fixture available on TV

- **WHEN** football-data.org returns a next scheduled fixture for
  Manchester United
- **THEN** the TV spotlight card shows competition, both teams'
  names/crests, and kickoff time (or live/finished state), and is
  D-pad-focusable

#### Scenario: No next fixture available on TV

- **WHEN** football-data.org returns no scheduled fixture for Manchester
  United
- **THEN** the spotlight sub-block is omitted, independent of whether the
  round list renders

### Requirement: TV upcoming Premier League round list

When David is selected and a key is configured, TV Home SHALL show a
vertically-stacked list of the upcoming Premier League round's fixtures
(full-width cards, not a horizontal rail), using the same `roundFixtures`
data phone's Home list uses -- matching phone Home's own carousel-to-list
change (fixture cards are information-dense; a horizontal rail undersells
them the way it doesn't for a simple channel-logo rail).

#### Scenario: Round fixtures available on TV

- **WHEN** `roundFixtures` is non-empty
- **THEN** each fixture renders as a D-pad-focusable TV fixture card,
  stacked vertically, showing competition, teams, status, and matched EPG
  channels, and a "View all" affordance is shown that opens the TV
  fixtures list

#### Scenario: No round fixtures on TV

- **WHEN** `roundFixtures` is empty
- **THEN** the list does not render (no empty-state card)

### Requirement: TV fixtures full list is vertical

The TV "view all" fixtures screen SHALL present fixtures as a single
vertically-scrolling column of full-width cards, matching the phone
`FixturesScreen` layout -- not a horizontal rail and not a multi-column
grid.

#### Scenario: Opening the TV fixtures list

- **WHEN** the user selects "View all" from the TV Football round list
- **THEN** a full-screen vertical list of the same `roundFixtures` opens,
  one fixture card per row, each independently D-pad-focusable, with the
  same staggered fade/slide-up entry motion the phone list uses

#### Scenario: Returning from the TV fixtures list

- **WHEN** the user presses Back while the TV fixtures list is open
- **THEN** the list closes and TV Home is shown again, without resetting
  the currently-selected TV tab

### Requirement: TV fixture cards are fully D-pad navigable

Every interactive element of a TV fixture card SHALL be independently
focusable and selectable by D-pad, without relying on touch-only
disambiguation.

#### Scenario: Single matched channel on TV

- **WHEN** exactly one EPG channel matches a fixture
- **THEN** the card is focusable and selecting it plays that channel

#### Scenario: Multiple matched channels on TV

- **WHEN** two or more EPG channels match a fixture
- **THEN** each channel appears as its own independently D-pad-focusable
  chip that plays that specific channel when selected

#### Scenario: No matched channels on TV

- **WHEN** no EPG channel matches a fixture
- **THEN** the card shows the same muted "Not on your channels" text as
  phone and is not focusable as a play target
