## 1. Build config / secret wiring

- [x] 1.1 Add `FOOTBALL_DATA_API_KEY` env-driven `buildConfigField` to
      `app/build.gradle.kts`, mirroring `YOUTUBE_API_KEY` exactly (empty
      string when the env var is absent).

## 2. Core DTOs (pure Kotlin, `core/`)

- [x] 2.1 Add `core/FootballModels.kt`: kotlinx.serialization DTOs for
      football-data.org's competition/team/score/match shapes.
- [x] 2.2 Add a sealed `FixtureStatus` (`Scheduled`/`Live`/`Finished`) and a
      pure mapping function from the API's raw status string to it (or
      `null` for unrecognised/non-played statuses, per spec).
- [x] 2.3 Add a plain `Fixture` model (competition, home/away team name +
      crest, status, kickoff instant) that `data/repo` maps DTOs into and
      `ui/` consumes — no football-data.org JSON shape leaking into `ui/`.
- [x] 2.4 Unit tests (`app/src/test/java/com/denham/skyline/core/
      FootballParsingTest.kt`): status mapping for all documented values
      including unrecognised ones, score parsing during `IN_PLAY`, match
      JSON → `Fixture` mapping, malformed/missing-field tolerance.

## 3. Repository (`data/repo`)

- [x] 3.1 Add `data/repo/FootballRepository.kt` following
      `YouTubeRepository`'s raw-OkHttp pattern: `okHttpClient` from
      `AppContainer`, `X-Auth-Token` header, no key stored on the class
      (passed at call time from `BuildConfig`).
- [x] 3.2 Implement `todaysFixtures(apiKey: String): List<Fixture>` via
      `GET /v4/matches?dateFrom=<today>&dateTo=<today>`.
- [x] 3.3 Implement `nextManUtdFixture(apiKey: String): Fixture?` via
      `GET /v4/teams/66/matches?status=SCHEDULED&limit=1`, with the team id
      as a named constant flagged for live-key confirmation.
- [x] 3.4 Both calls return empty/`null` (not throw) on blank key, non-2xx
      response, or parse failure — matches the "graceful degradation"
      requirement; log failures without leaking the key.

## 4. EPG matching

- [x] 4.1 Add a new `GuideDao` query: title `LIKE` match across all
      channels' programmes within a caller-supplied `fromMs`/`toMs` window
      (kickoff ± 90 min), returning matched `channelStreamId`s.
- [x] 4.2 Add a repository/viewmodel-level helper that takes a `Fixture`,
      runs the windowed query, and resolves ids through
      `ChannelDao.byIds` to real `ChannelEntity`s.

## 5. `FixtureCard` component

- [x] 5.1 Add `FixtureCard` to `ui/components/Components.kt` per the design
      brief's exact prop contract, layout order, and token usage
      (`SkyPalette.Surface`/`SkyRadius.card`/`SkySpacing.m`,
      `ProviderBadge`, `ArtworkImage` @28dp crests, `LiveBadge`).
- [x] 5.2 Implement the private channel-chip sub-composable: outlined
      `SkyPalette.Accent`, `SkyRadius.chip`, `labelSmall`,
      `Icons.Default.PlayArrow` @12dp, calls `onPlayChannel(channel)`.
- [x] 5.3 Implement tap behaviour: whole-card `scaledClickable` only when
      `channels.size == 1`; card inert (chips-only) when 2+; card inert, no
      chips row, when 0 (muted "Not on your channels" text instead).
- [x] 5.4 Implement all three status-row renderings (Scheduled/Live/
      Finished) exactly per the brief's typography/colour spec.

## 6. `HomeViewModel` state

- [x] 6.1 Add `FootballRepository` to `AppContainer` (`by lazy`, same
      pattern as `youtubeRepository`).
- [x] 6.2 Add `HomeViewModel` state: `manUtdNextFixture`,
      `todaysFixtures`, both scoped to `selectedFamilyMember == "David"`
      (via `combine`/`flatMapLatest`, mirroring `youtubeVideos`), plus a
      loading flag for the shimmer states.
- [x] 6.3 Add a per-fixture EPG-channels lookup exposed to `HomeScreen`
      (map fixture id -> matched `ChannelEntity` list), built from the
      task-4 helper.

## 7. `HomeScreen` UI

- [x] 7.1 Insert the "Football" section between the pinned-channels rail
      and the YouTube carousel, guarded by `selectedMember == "David"`
      specifically, wrapped in `Modifier.enterReveal(revealDelay(0))`
      exactly as the brief specifies.
- [x] 7.2 Render `SectionHeader("Football")`, the "Man Utd next" sub-heading
      + spotlight `FixtureCard` (full width minus gutter), then
      `Rail("", todaysFixtures, ...)` of `FixtureCard`s.
- [x] 7.3 Render loading states: `ShimmerBox` sized to the spotlight
      footprint, `ShimmerRail()` for the today's rail, while first-fetch is
      in flight.
- [x] 7.4 Confirm both sub-blocks fail independently (no spotlight fixture
      doesn't blank the rail and vice versa) per spec.

## 8. Verification

- [ ] 8.1 Run `./gradlew testDebugUnitTest` — confirm new Football parsing
      tests and all existing tests pass.
- [ ] 8.2 Run `./gradlew detekt` and `./gradlew detektDesignSystem` —
      confirm no raw `Color(0x…)` literals and no other detekt violations
      introduced.
- [x] 8.3 Static self-review against the design brief's token/reuse
      checklist and the spec's scenarios.
- [ ] 8.4 Push and confirm the CI workflow run is green (APK build,
      publish, and `detektDesignSystem` steps all actually ran, not
      silently skipped) — a push alone is not "done" per
      `skyline-iptv/CLAUDE.md`.

## 9. Archive

- [ ] 9.1 Once implemented and verified, archive this change
      (`openspec-archive-change`) to fold the `football-fixtures` delta
      spec into `openspec/specs/`.

## 10. Open items unresolved as of QA review (2026-08-12)

Neither of these could be checked from this environment (no
`.github/workflows` directory in this checkout, and no live/authenticated
football-data.org key available). Do not mark either resolved without
independent confirmation.

- [ ] 10.1 Confirm `FOOTBALL_DATA_API_KEY` is actually wired as a CI
      secret/`BuildConfig` field the way `YOUTUBE_API_KEY` is (see design
      brief open question #2) -- unverifiable without CI workflow access
      from this checkout.
- [ ] 10.2 Confirm football-data.org team id 66 really resolves to
      Manchester United via a live authenticated call (see design brief
      open question #1 and `FootballRepository.manUtdTeamId`'s doc
      comment) -- unverifiable without a live API key.

## 11. Merge-time compile break (found 2026-08-12, post-merge)

PR #4 merged this change to `main` before any agent in this environment
had actually compiled it (still can't -- AGP doesn't resolve here). CI on
the merge commit (`7b08560`, run 31609842763) failed:
`HomeViewModel.footballSection`'s `if/else` returned mismatched flow types
-- `flowOf(FootballSectionState.Hidden)` inferred `Flow<FootballSectionState.Hidden>`
instead of `Flow<FootballSectionState>`, so the `else` branch's `emit(Loading)`/
`emit(Loaded(...))` failed to type-check against it. `main` was red from
14:59:30 until this was found and fixed.

- [ ] 11.1 Fix pushed on `fix/football-section-compile-error`: pin both
      branches to the sealed interface explicitly (`flowOf<FootballSectionState>(...)`,
      `flow<FootballSectionState> { ... }`). Not yet confirmed green by CI --
      see PR for this branch and task 8.4 above once it lands.
