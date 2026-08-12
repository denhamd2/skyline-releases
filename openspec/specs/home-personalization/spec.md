## Purpose

Lets each family member (David, Anne, Ava, Sophie) see a home page weighted
to what they watch, without separate accounts or profiles.

## Requirements

### Requirement: Family member selector

The home page SHALL present a selector for the four family members, placed
immediately above the rails it controls.

Selecting an already-selected member deselects it and returns to the default
rails.

#### Scenario: Selecting a member

- **WHEN** a member is selected
- **THEN** the category rails below change to that member's categories

#### Scenario: Deselecting

- **WHEN** the currently selected member is tapped again
- **THEN** selection clears and the provider's default rails return

### Requirement: Explicit category choices take precedence

When a member has categories saved, those SHALL be used verbatim.

#### Scenario: Member has saved categories

- **WHEN** a member with saved categories is selected
- **THEN** exactly those categories are shown as rails, skipping empty ones

### Requirement: Keyword defaults before any choice is made

When a member has no saved categories, the app SHALL match keywords against
the provider's live category names as a starting point.

Keywords must resemble live channel-genre naming, not film genres. Anne's
original list ("mystery", "crime", "thriller") were VOD genres and matched no
live category, so her tab silently fell through to the default rails and
appeared broken. Ava and Sophie had no keywords at all, with the same result.

These lists are best-effort guesses at one provider's naming and are expected
to be corrected by explicit selection.

#### Scenario: Keywords match categories

- **WHEN** a member with no saved categories is selected
- **THEN** categories whose names contain any of their keywords are shown

#### Scenario: No keyword matches

- **WHEN** no category name matches any keyword
- **THEN** the provider's default top categories are shown

### Requirement: Categories are editable per member

Settings SHALL provide an editor covering all four members, not a fixed one.

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

### Requirement: Preferences read the stored value

Preference reads SHALL await the stored value rather than sampling an
initial placeholder.

Reads previously used `stateIn(...).value`, which returns the placeholder
because DataStore emits asynchronously. Every read came back empty and every
write rebuilt from an empty base, silently discarding saved selections.

#### Scenario: Reading saved preferences

- **WHEN** stored preferences are read
- **THEN** the persisted value is returned, not an empty default
