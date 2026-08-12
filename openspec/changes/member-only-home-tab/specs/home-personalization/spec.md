## MODIFIED Requirements

### Requirement: Family member selector

The home page SHALL present a selector for the four family members, placed
immediately above the rails it controls.

Selecting a member SHALL restrict the page to that member's own content:
Continue Watching, their pinned channels, their category rails, and their
YouTube rail. The generic rails — Live Now, Favourites, Live TV, New films
and New series — SHALL be hidden while a member is selected.

Selecting an already-selected member deselects it and returns to the default
rails.

Selecting a member previously only appended rails below the selector, leaving
all six generic rails above it in place, so a member's own content sat at the
bottom of a page mostly made of things they had not chosen. Continue Watching
is the one general rail kept, because it reflects what that person was
actually watching rather than what the provider is promoting.

#### Scenario: Selecting a member

- **WHEN** a member is selected
- **THEN** the page shows that member's pinned channels, category rails and
  YouTube rail
- **AND** Live Now, Favourites, Live TV, New films and New series are hidden

#### Scenario: Continue Watching survives selection

- **WHEN** a member is selected and a partly-watched item exists
- **THEN** Continue Watching is still shown

#### Scenario: Deselecting

- **WHEN** the currently selected member is tapped again
- **THEN** selection clears and the provider's default rails return

### Requirement: Categories are editable per member

Settings SHALL provide an editor covering all four members, not a fixed one,
and it SHALL edit both the member's categories and their pinned channels.

The entry point previously navigated with a hard-coded "David", so the other
three could not be configured at all.

#### Scenario: Editing a member's categories

- **WHEN** the editor is opened
- **THEN** the member being edited can be switched
- **AND** that member's existing selection is pre-ticked

#### Scenario: Saving a selection

- **WHEN** a selection is saved
- **THEN** it replaces that member's stored categories and is reflected on
  the home page

## ADDED Requirements

### Requirement: Channels can be pinned per member

A member's home tab SHALL be able to include named individual channels,
independent of which categories that member has selected.

Personalisation was expressible only as whole categories. A request for one
channel — MUTV — had no way to be satisfied except by selecting the category
containing it, which would have brought in every other channel in that
category too.

Pinned channels are per member and are NOT the same thing as favourites. The
`favorites` table stays global and shared, and the Favourites rail continues
to reflect it; pinning adds a channel to one member's tab only. The two must
not be conflated.

#### Scenario: A pinned channel appears for its member

- **WHEN** a member with pinned channels is selected
- **THEN** those channels are shown as a rail on that member's tab

#### Scenario: Pinning is independent of categories

- **WHEN** a channel is pinned to a member whose selected categories do not
  contain it
- **THEN** it still appears on that member's tab

#### Scenario: Pinning does not affect other members

- **WHEN** a channel is pinned to one member
- **THEN** it does not appear on another member's tab, and the shared
  Favourites rail is unchanged

#### Scenario: Choosing a channel to pin

- **WHEN** pinning a channel in the per-member editor
- **THEN** channels can be searched by name, rather than requiring a scroll
  through the provider's full channel list

#### Scenario: A pinned channel is no longer in the catalogue

- **WHEN** a pinned channel id is not present after a catalogue sync
- **THEN** it is omitted from the rail rather than rendering as a broken entry
