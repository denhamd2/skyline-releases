## Purpose

Governs how Skyline's screens are captured as screenshots, what makes those
captures deterministic, and what actually fails a CI run versus what is
documentation-only.

## ADDED Requirements

### Requirement: Screenshot verification gates CI

CI SHALL run screenshot tests in verify mode, failing the run when a
rendered screen no longer matches its committed golden image.

Before this, CI only ran record mode, which unconditionally overwrites the
committed PNGs on every push. No visual regression has ever failed a build
under that mechanism — the committed images were documentation, not a gate,
regardless of what they looked like they were. `docs/skyline-screenshots/README.md`
described this as a reviewed golden-history workflow, which was not true of
what actually ran. A documented check that does not run is worse than no
check, because it is trusted.

#### Scenario: A screen's rendered output changes unintentionally

- **WHEN** a code change alters what a covered screen renders
- **THEN** the screenshot verification step fails and the run is reported red

#### Scenario: A screen renders identically to its golden

- **WHEN** no covered screen's rendered output has changed
- **THEN** the screenshot verification step passes

### Requirement: Golden images can be deliberately updated

An intentional visual change SHALL be able to update the committed goldens
without being blocked by the verification gate that exists to catch
unintentional ones.

#### Scenario: Updating goldens after a deliberate redesign

- **WHEN** a contributor triggers the workflow's record mode explicitly
- **THEN** the committed golden images are replaced with the current render
- **AND** verification then passes against the newly recorded goldens

### Requirement: Every real screen is captured, not only isolated components

Screenshot coverage SHALL include every top-level screen in the app, not
only the design-system component gallery.

The component gallery alone answers "do these tokens and primitives look
right in isolation" but not "does the Home screen, as actually composed,
look right" — the question a change to a real screen actually raises.

#### Scenario: A screen has no existing coverage

- **WHEN** a top-level screen composable exists in the app
- **THEN** it has a corresponding screenshot test, unless explicitly excluded

#### Scenario: The player screen is excluded

- **WHEN** screenshot coverage is being added or reviewed
- **THEN** the video playback screen is not included
- **AND** the reason is recorded as its video surface not being meaningfully
  renderable under the test framework, not an oversight

### Requirement: Captures are deterministic

A screenshot test SHALL NOT depend on the wall-clock time or on live network
image loading at the moment it runs.

Any screen showing "now"-relative state (a live-programme highlight, a
now/next indicator, a recency badge) or remote artwork would otherwise
render differently between runs or environments, making its golden
meaningless — every run would either need re-recording or would fail for a
reason unrelated to an actual code change.

#### Scenario: A screen depends on the current time

- **WHEN** a screen's rendered output depends on "now"
- **THEN** the test supplies a fixed, injected time rather than reading the
  system clock

#### Scenario: A screen loads remote artwork

- **WHEN** a screen renders an image loaded over the network in production
- **THEN** the test substitutes a fixed local image source
- **AND** the test does not depend on network access succeeding

### Requirement: Coverage spans phone and TV form factors

Each covered screen SHALL be captured at both a phone and an Android TV
qualifier, for any screen that has a distinct rendering on each form factor.

#### Scenario: A screen renders on both form factors

- **WHEN** a screen is used on both phone and Android TV
- **THEN** its screenshot coverage includes both a phone-qualifier and a
  TV-qualifier capture
