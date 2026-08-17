## Why

The David-only "Football" feature (Man Utd next-fixture spotlight, upcoming
Premier League round rail, and a full "view all" vertical fixtures list) is
phone-only today. Every design brief that shaped it
(`design/football-fixtures-vertical-list.md`, `design/fixtures-screen.md`)
explicitly deferred TV to "phase 2" pending D-pad focus treatment and
individually focusable channel chips, and that follow-up was never picked
up: `TvHomeScreen` (`tv/TvScreens.kt`) has no Football section at all, and
there is no TV equivalent of `FixturesScreen.kt`. The user wants this gap
closed, with the full "view all" list on TV kept **vertical** — the same
`LazyColumn` shape as phone, not a horizontal rail or multi-column grid.

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
  StateFlows: Man Utd spotlight + horizontal rail of the upcoming round,
  with a "View all" affordance.
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

- **New files**: `tv/TvFixturesScreen.kt` (or added to `tv/TvScreens.kt` --
  developer's call at implementation time).
- **Modified files**: `tv/TvComponents.kt` (new `TvFixtureCard`),
  `tv/TvScreens.kt` (`TvHomeScreen` Football section + `onViewAllFixtures`
  parameter), `tv/TvMainActivity.kt` (`showFixtures` state + overlay +
  `BackHandler`).
- **Not modified**: `data/repo/FootballRepository.kt`,
  `ui/home/HomeViewModel.kt`'s `footballSection`/`fixtureChannels` state
  shape, `core/FootballModels.kt`/`FootballMapping.kt`, the phone
  `FixtureCard`/`FixturesScreen.kt`. This change is purely additive TV UI
  reusing state that already exists.
- **Not verifiable in this environment**: no device/emulator and the AGP
  cannot be resolved here, so D-pad focus traversal order and the visual
  result of the new TV cards cannot be checked on-device or via a live
  screenshot -- flagged for CI/QA review once built.
