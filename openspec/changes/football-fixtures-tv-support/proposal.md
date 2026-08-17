## Why

The David-only "Football" feature (Man Utd next-fixture spotlight, upcoming
Premier League round list, and a full "view all" vertical fixtures list) is
phone-only today. Every design brief that shaped it
(`design/football-fixtures-vertical-list.md`, `design/fixtures-screen.md`)
explicitly deferred TV to "phase 2" pending D-pad focus treatment and
individually focusable channel chips, and that follow-up was never picked
up: `TvHomeScreen` (`tv/TvScreens.kt`) has no Football section at all, and
there is no TV equivalent of `FixturesScreen.kt`. The user wants this gap
closed, with fixture lists kept **vertical** everywhere they appear — the
Home round list on both platforms, and the full "view all" list on TV —
not a horizontal rail or multi-column grid.

Note: this proposal originally assumed the phone Home round rail stayed
horizontal (matching its shipped state at the time). Mid-implementation,
PR #9 (`claude/fixture-spacing-carousel-x0s6qn`, already reviewed, no
blocking findings) was found already converting phone Home's round rail to
a vertical list for the same "fixture cards are information-dense" reason.
Per the user's direction, that design was adopted into this branch too
(superseding PR #9, which becomes redundant once this merges), and this
proposal's TV design was updated to match: the TV Home round section is
now also a vertical list, not a horizontal rail, for consistency across
both platforms.

## What Changes

- Add a new `TvFixtureCard` composable (`tv/TvComponents.kt`), a TV sibling
  of the existing `TvLandscapeCard`/`TvPosterCard` pattern — not a
  modification of the shared phone `FixtureCard`. D-pad-focusable via
  `Modifier.tvClickable`, with every channel chip individually focusable
  (TV has no touch fallback to disambiguate a multi-channel fixture the way
  phone's "chips only when 2+ channels" rule does).
- Add a "Football" section to `TvHomeScreen`, gated on
  `selectedMember == "David"` exactly like the phone Home section, reusing
  the same existing `HomeViewModel.footballSection`/`fixtureChannels`
  StateFlows: Man Utd spotlight + a vertically-stacked list of the upcoming
  round (matching phone Home's list, not a horizontal rail), with a
  "View all" affordance.
- Add `TvFixturesScreen`: the TV "view all" full list. Vertical `LazyColumn`
  of full-width `TvFixtureCard`s, matching phone's `FixturesScreen.kt`
  layout — explicitly not a grid. This resolves the "vertical list vs.
  2-column grid (TBD)" question every prior brief left open for TV.
- Wire navigation: TV has no `NavHost`/`Routes` — `TvRoot`
  (`tv/TvMainActivity.kt`) is a local `Crossfade`-over-`TvTab` state machine
  with a `playing: Boolean` that already overlays `TvPlayerOverlay` outside
  the tab crossfade. Add a `showFixtures: Boolean` the same way, and extend
  the existing `BackHandler` to close it.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `football-fixtures`: adds TV-specific requirements (TV Football section
  visibility, TV Man Utd spotlight, TV round rail, TV full-list is
  vertical, TV D-pad focus/chip focusability) alongside the existing
  phone-only requirements, which are unchanged.

## Impact

- **New files**: `tv/TvFixturesScreen.kt`.
- **Modified files**: `tv/TvComponents.kt` (new `TvFixtureCard`),
  `tv/TvScreens.kt` (`TvHomeScreen` Football section + `onViewAllFixtures`
  parameter), `tv/TvMainActivity.kt` (`showFixtures` state + overlay +
  `BackHandler`); `ui/home/HomeScreen.kt` (Football round rail -> vertical
  list, adopting PR #9's already-reviewed design, plus a "View all" link
  to the new TV/phone-shared `FixturesScreen`); `ui/fixtures/
  FixturesScreen.kt` (`fixtureChannels` parameter type fix -- see below).
- **Not modified**: `data/repo/FootballRepository.kt`,
  `ui/home/HomeViewModel.kt`'s `footballSection`/`fixtureChannels` state
  shape, `core/FootballModels.kt`/`FootballMapping.kt`, the phone
  `FixtureCard` component itself.
- **Bug fixes bundled in while implementing this change** (found, not
  introduced, by this work): `HomeScreen.kt`'s fixtures rail called
  `Rail(..., onViewAll = ...)`, a parameter `Rail` doesn't declare -- a
  compile error blocking the whole app build, fixed by moving "View all"
  onto a header `Text`/`SectionHeader`-style row instead. Separately,
  `FixturesScreen`'s `fixtureChannels` parameter was typed
  `Map<String, List<ChannelEntity>>` against an actual
  `Map<Long, List<ChannelEntity>>` (`Fixture.id` is `Long`) -- also a
  compile error, fixed in both the phone and new TV screens. Neither bug
  is specific to TV, but both were blocking this feature end-to-end on
  every platform, so fixing them was a prerequisite for this change
  actually being observable at all.
- **Not verifiable in this environment**: no device/emulator and the AGP
  cannot be resolved here, so D-pad focus traversal order and the visual
  result of the new TV cards cannot be checked on-device or via a live
  screenshot -- flagged for CI/QA review once built.
