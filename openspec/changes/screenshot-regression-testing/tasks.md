## 1. Make the existing gate real

- [x] 1.1 Add a `workflow_dispatch` input to `build-skyline-apk.yml` for
      recording golden updates explicitly (default off).
- [x] 1.2 Switch the workflow's screenshot step from `recordRoborazziDebug`
      to `verifyRoborazziDebug` when the record input is not set; keep
      `recordRoborazziDebug` on the explicit dispatch path.
- [x] 1.3 Correct `docs/skyline-screenshots/README.md` to describe the
      mechanism that actually runs (verify by default, record on demand) —
      it currently describes a reviewed golden-history workflow that was
      never true of what ran.
      *Went further than the verify/record sentence: the doc also described
      PR-comment diffs, a per-screen naming convention, and coverage of
      screens (Movies, Series, Player, TV Browse) that were never built —
      none of that existed, so a partial patch would have left it still
      mostly fictional. Rewritten to describe only what's real.*
- [x] 1.4 Push and confirm the run is green **and** that verification
      actually ran (not skipped), against the two existing component-gallery
      goldens.
      *First attempt (`cba851e`, run 84) failed on both screenshot tests --
      not a code regression. Root cause, confirmed by pulling the run's own
      `*_compare.png` diagnostic and looking at it: `ScreenshotTests.kt`
      wrote its captures to `build/outputs/roborazzi/...`, and `build/` is
      blanket-gitignored (`.gitignore:7`). That path has never once been
      committed, so `verifyRoborazziDebug` had no reference image to compare
      against on a fresh CI checkout -- not "stale," `docs/skyline-screenshots/*.png`
      was flatly never Roborazzi's own comparison baseline, only a cosmetic
      one-way copy a separate CI step made afterward. These two tests have
      never had a real committed golden, the entire time they've existed.
      Fixed by writing `captureRoboImage` output directly to
      `docs/skyline-screenshots/*.png` (already tracked, already the
      documented human-facing location) and reworking the workflow: the
      artifact upload now points at that real path so failure diagnostics
      (Roborazzi's `*_actual`/`*_compare` siblings) are reviewable, and the
      git-commit step now only fires on an explicit `record_screenshots`
      dispatch -- never on a plain verify pass (nothing changed) or fail
      (deliberately not auto-committed, so a red run can never quietly
      rewrite what it was supposed to be checking against).*
- [x] 1.5 Prove the gate: alter one component's spacing, confirm
      `verifyRoborazziDebug` fails and produces a diff, then revert. Do not
      leave the alteration committed.
      *Run 87 (`456aa62`, PosterCard title padding 6dp -> 12dp), reverted in
      `d777f89`. All three assertions held: "Verify UI screenshots" FAILED,
      "Build debug APK" and both publish steps SUCCEEDED, and "Publish
      recorded screenshots to branch" was SKIPPED so the `*_actual`/
      `*_compare` diagnostics were uploaded as an artifact rather than
      committed -- a red run cannot rewrite the baseline it was checking
      against. Note the trade-off this exposes: because publishing now comes
      first, the deliberate padding change did ship in run 87's APK before
      being reverted.*

## 2. Scaffolding: Clock, fixture database, Coil fake

- [ ] 2.1 Add a `Clock` constructor parameter (default `Clock.systemUTC()`)
      to `GuideRepository`, `EpgRepository`, `YouTubeRepository`, and every
      screen ViewModel that currently calls `System.currentTimeMillis()`
      directly; replace those call sites with `clock.millis()`.
- [ ] 2.2 Add a shared test helper building an in-memory `SkylineDatabase`
      (`Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries().build()`)
      seeded with literal fixture rows: a handful of channels, movies,
      series, EPG entries, and categories, covering what the screens below
      need to render non-empty state.
- [ ] 2.3 Add the `coil-test` dependency (matching the pinned `coil`
      version) and a shared test setup that swaps in
      `FakeImageLoaderEngine` via `Coil.setImageLoader(...)`, reset after
      each test.
- [ ] 2.4 Push and confirm CI is green. No screens depend on this yet, so
      this only needs to compile and not break existing tests.

## 3. `LoginScreen` — proves the pattern

- [ ] 3.1 Extract `LoginScreenContent(state: LoginUiState, ...)` from
      `LoginScreen`, keeping `LoginScreen` as the `ViewModel`-owning wrapper.
- [ ] 3.2 Add phone and TV Roborazzi tests for `LoginScreenContent` under
      `app/src/test/java/com/denham/skyline/ui/screenshots/`, `verify`-mode.
- [ ] 3.3 Push and confirm CI is green, including the new tests actually
      running (not silently skipped).

## 4. `AccountScreen`, `TvBrowseScreen` — already stateless

- [ ] 4.1 Add phone/TV Roborazzi tests directly against `AccountScreen` and
      `TvBrowseScreen` — no extraction needed, both already take plain
      params.
- [ ] 4.2 Push and confirm CI is green.

## 5. `GuideScreen` — exercises the Clock

- [ ] 5.1 Extract `GuideScreenContent(state: GuideUiState, ...)`, passing
      the fixed test `Clock` through to wherever the now-line and
      programme-highlight logic reads it.
- [ ] 5.2 Add phone/TV tests with a `Clock.fixed(...)` set to a known
      instant relative to fixture EPG data, so the now-line lands at a
      deterministic position.
- [ ] 5.3 Push and confirm CI is green.

## 6. `HomeScreen`, `LiveScreen`, `SettingsScreen`

- [ ] 6.1 Extract `HomeScreenContent`, add phone/TV tests, push, confirm
      green.
- [ ] 6.2 Extract `LiveScreenContent`, add phone/TV tests, push, confirm
      green.
- [ ] 6.3 Extract `SettingsScreenContent`, add phone/TV tests, push, confirm
      green.

## 7. Remaining phone/shared screens

- [ ] 7.1 `BrowseScreens.kt` (`MoviesScreen`, `SeriesScreen`): extract
      `Content` composables, add tests, push, confirm green.
- [ ] 7.2 `DetailScreens.kt` (`MovieDetailScreen`, `SeriesDetailScreen`):
      extract, test, push, confirm green.
- [ ] 7.3 `MyListScreen`: extract, test, push, confirm green.
- [ ] 7.4 `CatchUpScreen`: extract (threading the `Clock` through, per its
      existing `System.currentTimeMillis()` use), test, push, confirm green.
- [ ] 7.5 `DownloadsScreen`: extract, test, push, confirm green. Note its
      1-second polling loop — the test should capture a single fixed state,
      not exercise the live polling.
- [ ] 7.6 `SearchScreen`: extract, test, push, confirm green.
- [ ] 7.7 `YouTubeSubscriptionScreen`: extract, test, push, confirm green.
- [ ] 7.8 `CategoryCustomizationScreen`: extract, test, push, confirm green.

## 8. TV screens needing a `ViewModel` introduced first

- [ ] 8.1 Introduce `TvSportsViewModel`, `TvKidsViewModel`,
      `TvMySkyViewModel` reading what `TvSportsScreen`/`TvKidsScreen`/
      `TvMySkyScreen` currently read inline from `AppContainer`/the
      database — matching the shape every other screen's `ViewModel`
      already has. Behavior unchanged; this is a structural move, not a
      feature change.
- [ ] 8.2 Push and confirm CI is green before extracting `Content`
      composables from these three, since this step carries more risk than
      the others (introducing new types, not just moving rendering code).
- [ ] 8.3 Extract `Content` composables for all three, add TV tests (no
      phone equivalent — these are TV-only screens), push, confirm green.

## 9. Close out

- [ ] 9.1 Confirm every screen listed in the spec's requirement ("every
      real screen... unless explicitly excluded") has coverage, and that
      `PlayerScreen`'s exclusion is the only exception.
- [ ] 9.2 Update `docs/skyline-screenshots/README.md` to list what's now
      covered, replacing the component-gallery-only description.
- [ ] 9.3 Archive this change, syncing the new `screenshot-testing` capability
      into `openspec/specs/`.
