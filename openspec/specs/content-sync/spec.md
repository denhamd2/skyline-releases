## Purpose

Pulls the provider's catalogue — categories, live channels, movies, series —
into Room so the app reads from local storage rather than the panel.

## Requirements

### Requirement: Catalogue is synced into local storage

The app SHALL sync categories, live channels, movies and series from the
Xtream panel into Room, and SHALL serve screens from Room thereafter.

#### Scenario: First run

- **WHEN** the app starts with an empty database
- **THEN** a sync runs and progress is surfaced on the home page

#### Scenario: Subsequent runs

- **WHEN** channels are already stored
- **THEN** the sync is skipped unless explicitly forced

### Requirement: Large payloads are streamed

List endpoints SHALL be stream-parsed and written in chunked transactions,
never buffered whole.

Catalogues run to tens of thousands of items on large providers.

#### Scenario: Syncing a large catalogue

- **WHEN** a list endpoint returns tens of thousands of entries
- **THEN** entries are parsed as a stream and inserted in chunks

### Requirement: Channel sync is the only fatal stage

A failure fetching channels SHALL fail the sync; failures fetching
categories, movies or series SHALL be tolerated.

Channels are what the user perceives as "the app working". A provider
hiccup on VOD should not discard a good channel list.

#### Scenario: Movies endpoint fails

- **WHEN** the movies request fails but channels succeeded
- **THEN** the sync completes and channels remain usable

#### Scenario: Channels fail on a first run

- **WHEN** channels cannot be fetched and none are stored
- **THEN** an error with a retry action is shown

#### Scenario: Channels fail with data already present

- **WHEN** channels cannot be fetched but a previous sync stored some
- **THEN** the stored channels continue to be shown rather than an error

### Requirement: Schema changes preserve user data

Database version bumps SHALL ship an explicit migration.

Favourites and other user-owned data must survive updates; a destructive
fallback would discard them.

#### Scenario: Updating to a build with a new schema

- **WHEN** a build with a higher database version is installed
- **THEN** the migration runs and favourites survive
