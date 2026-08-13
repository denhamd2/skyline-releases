## 1. Core DTOs (`core/FootballModels.kt`)

- [x] 1.1 Add `matchday: Int? = null` to `FootballMatchDto`.
- [x] 1.2 Add `FootballSeasonDto(currentMatchday: Int? = null)` and
      `FootballCompetitionDetailDto(currentSeason: FootballSeasonDto =
      FootballSeasonDto())`, following the file's existing
      default-rather-than-crash convention.

## 2. Repository (`data/repo/FootballRepository.kt`)

- [x] 2.1 Split `fetchFixtures` into `fetchMatches` (request + tolerant
      parse, returns `List<FootballMatchDto>`) and a thin `fetchFixtures`
      wrapper (`FootballMapping.toFixtures(fetchMatches(...))`), used
      unchanged by `nextManUtdFixture`.
- [x] 2.2 Add `fetchCurrentMatchday(apiKey): Int?` -- `GET $BASE_URL/
      competitions/PL`, decode `FootballCompetitionDetailDto`, return
      `currentSeason.currentMatchday`, `null` on any failure (never
      throws).
- [x] 2.3 Replace `todaysFixtures(apiKey)` with
      `upcomingPremierLeagueRound(apiKey): List<Fixture>`: call
      `fetchCurrentMatchday`, return `emptyList()` if `null`, else
      `fetchFixtures("$BASE_URL/competitions/PL/matches?matchday=$n",
      apiKey)`.
- [x] 2.4 Add `PREMIER_LEAGUE_CODE = "PL"` constant; update the class doc
      comment (currently describes "today's fixtures across covered
      competitions").

## 3. `HomeScreen.kt`

- [x] 3.1 Rename `FootballSectionState.Loaded.todaysFixtures` ->
      `roundFixtures`.
- [x] 3.2 Update `HomeViewModel.footballSection`'s fetch block to call
      `upcomingPremierLeagueRound(apiKey)`; update the failure log message.
- [x] 3.3 Update `showFootball`, `fixtureChannels`'s fixture-collection
      line, and the `Rail("", football.todaysFixtures, ...)` call site to
      the renamed field. Leave `FixtureCard`/`FixtureStatus.Live`
      rendering untouched.
- [x] 3.4 Update the "Football section" doc comments referencing
      "today's-fixtures rail".

## 4. Tests (`core/FootballParsingTest.kt`)

- [x] 4.1 `matchday` decoding on `FootballMatchDto`: present, absent, and
      explicit `null`.
- [x] 4.2 `FootballCompetitionDetailDto`/`FootballSeasonDto` decoding:
      normal case, missing `currentSeason`, missing/null
      `currentMatchday` -- each degrades to `null` rather than throwing.

## 5. Verification

- [ ] 5.1 Run `./gradlew testDebugUnitTest` locally -- confirm new and
      existing tests pass (AGP/on-device build itself cannot be verified
      locally in this environment).
      *Attempted: fails at plugin resolution (`com.android.application`
      8.11.1 not resolvable), the pre-existing AGP limitation in this
      checkout -- not a result of this change. New test cases in
      `FootballParsingTest.kt` verified only by static read, not executed.
      Needs CI.*
- [ ] 5.2 Run `./gradlew detekt` / `./gradlew detektDesignSystem` locally
      if they resolve; otherwise note as expected-to-fail-here per this
      repo's AGP-resolution limitation.
      *Attempted: same plugin-resolution failure as 5.1. No raw
      `Color(0x…)` literals introduced (confirmed by grep) -- this change
      touches no UI colour code. Needs CI to confirm no other detekt
      violations.*
- [x] 5.3 Static self-review against this change's spec scenarios and the
      design's decisions.
      *Confirmed: `upcomingPremierLeagueRound` returns `emptyList()` on
      blank key or missing `currentMatchday` (spec's "No current matchday
      available"/"Current round has no fixtures" scenarios); `matchday`
      nullable so `nextManUtdFixture`'s team-scoped query is unaffected;
      `FixtureCard`/`FixtureStatus.Live` rendering untouched, so the
      inherited live-score scenario is unchanged; all `todaysFixtures`
      references renamed to `roundFixtures` with no stray references left
      (grepped repo-wide).*
- [ ] 5.4 Push and confirm the CI workflow run is green (unit tests, APK
      build/publish, `detektDesignSystem` all actually ran, not silently
      skipped) -- a push alone is not "done" per `skyline-iptv/CLAUDE.md`.
      *Not yet pushed from this session -- branch left for review before
      push, per task instructions.*
- [ ] 5.5 Once CI is confirmed green, archive this change
      (`openspec-archive-change`) to fold the delta spec into
      `openspec/specs/football-fixtures/spec.md`.
      *Deliberately left unarchived pending CI confirmation, per this
      repo's "CI is the only compiler" discipline (same as
      `football-fixtures-home`'s task 8/9 pattern).*
