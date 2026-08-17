## 1. TV fixture card (`tv/TvComponents.kt`)

- [x] 1.1 Add `TvFixtureCard(competition, homeTeam, awayTeam, homeCrestUrl,
      awayCrestUrl, status: FixtureStatus, channels: List<ChannelEntity>,
      onPlayChannel, modifier, width: Dp? = 240.dp, isSpotlight: Boolean =
      false)` -- same prop shape as phone's `FixtureCard`
      (`ui/components/Components.kt:416-427`) for drop-in parity, reusing
      `ArtworkImage`/`LiveBadge`/`ProviderBadge` and
      `SkyPalette`/`SkySpacing`/`SkyRadius` tokens.
- [x] 1.2 Wrap the card in `Modifier.tvClickable(...)`
      (`tv/TvComponents.kt:50-81`) for its default action (single-match
      case), matching the 1.04x/white-outline/140ms treatment every other
      TV card uses.
- [x] 1.3 Add `TvFixtureChannelChip`, a TV counterpart to phone's private
      `FixtureChannelChip`: same visual (outlined pill, `SkyRadius.chip`,
      `Accent` border/text/icon) but individually `tvClickable`, rendered
      for every matched channel unconditionally (not gated by channel
      count the way phone's chip-vs-whole-card split is).
- [x] 1.4 Status row (Scheduled/Live/Finished) and empty-channels fallback
      text: reuse the same layout/token choices as phone's `FixtureCard`
      (`Components.kt:496-554`) so the two platforms read as the same
      component family.

## 2. TV Home Football section (`tv/TvScreens.kt`)

- [x] 2.1 Add `onViewAllFixtures: () -> Unit = {}` parameter to
      `TvHomeScreen`.
- [x] 2.2 Collect `viewModel.footballSection`/`viewModel.fixtureChannels`
      (same StateFlows `HomeViewModel` already exposes) alongside
      `TvHomeScreen`'s existing `collectAsState()` calls.
- [x] 2.3 Insert a "Football" section item, gated `selectedMember ==
      "David"`, mirroring `HomeScreen.kt:810-877`'s structure: "Man Utd
      next" label + spotlight `TvFixtureCard(isSpotlight = true)`, then
      `TvRailHeader` + horizontal `LazyRow` of `roundFixtures` using
      `TvFixtureCard`, with a "View all" control that calls
      `onViewAllFixtures`.
- [x] 2.4 Loading state: reuse the existing `ShimmerBox`/`ShimmerRail`
      pattern already used elsewhere on TV/phone for this section's
      `FootballSectionState.Loading`.
      *Used `ShimmerBox` for the spotlight placeholder; did not add a
      second `ShimmerRail` for the round-rail loading state, since
      `FootballSectionState.Loading` doesn't distinguish which sub-block
      will end up populated -- matches how the section renders as one
      unit, not two independently-loading pieces.*

## 3. TV fixtures full list

- [x] 3.1 Add `TvFixturesScreen(fixtures: List<Fixture>, fixtureChannels:
      Map<Long, List<ChannelEntity>>, onPlayChannel, onBack)` -- vertical
      `LazyColumn` of full-width `TvFixtureCard`s (`width = null`), one
      per row, `Modifier.enterReveal(revealDelay(index))` per card, same
      empty-state message as phone's `FixturesScreen.kt`. Not a grid.
      *Map key corrected to `Long` during implementation -- see task 5.3.*
- [x] 3.2 Place in a new `tv/TvFixturesScreen.kt` file.

## 4. Navigation wiring (`tv/TvMainActivity.kt`)

- [x] 4.1 Add `var showFixtures by remember { mutableStateOf(false) }` in
      `TvRoot`, alongside the existing `playing` state.
- [x] 4.2 Pass `onViewAllFixtures = { showFixtures = true }` into
      `TvHomeScreen`'s call site.
- [x] 4.3 Render `TvFixturesScreen` as a full-screen overlay when
      `showFixtures` is true (and not `playing`), outside the tab
      `Crossfade`, the same way `TvPlayerOverlay` is layered when `playing`
      is true. Selecting a channel closes the fixtures overlay and hands
      off to the player via the same `playChannel` the rest of TV uses.
- [x] 4.4 Extend the `BackHandler` block with a preceding
      `BackHandler(enabled = !playing && showFixtures) { showFixtures =
      false }`, and gated the existing tab-reset handler on `!showFixtures`
      too, matching the `playing` handler's precedence pattern.

## 5. Verification

- [ ] 5.1 Run `./gradlew testDebugUnitTest` -- confirm it compiles and
      passes (cannot be run locally in this environment; needs CI).
      *Not yet run: this environment's manual `workflow_dispatch` attempt
      was rejected (`403 Resource not accessible by integration`), and the
      workflow only triggers on push to `main`, not this feature branch --
      needs a merge/PR or a manual run from someone with dispatch
      permission.*
- [ ] 5.2 Run `./gradlew detekt` / `detektDesignSystem` -- confirm no raw
      `Color(0x...)` literals or other design-system violations in the new
      TV files. *Not run for the same reason as 5.1; confirmed by grep
      instead (task 5.3) that no raw `Color(0x...)` literals were
      introduced.*
- [x] 5.3 Static self-review: every new TV composable traces its
      colour/spacing/corner/type choices back to an existing
      `SkyPalette`/`SkySpacing`/`SkyRadius`/`MaterialTheme.typography`
      token, matching the phone `FixtureCard`/`FixturesScreen` it mirrors.
      *Also found and fixed a second, independent pre-existing compile
      error while wiring this up: `FixturesScreen`'s `fixtureChannels`
      parameter was typed `Map<String, List<ChannelEntity>>`, but
      `HomeViewModel.fixtureChannels` is `Map<Long, List<ChannelEntity>>`
      (`Fixture.id` is `Long`) -- corrected in both the phone screen and
      this change's new TV screen.*
- [ ] 5.4 Push and confirm the `Build Skyline APK` workflow run is green
      (unit tests, APK build/publish, `detektDesignSystem` all actually
      ran) -- a push alone is not "done" per `skyline-iptv/CLAUDE.md`.
      *Pushed to `claude/concise-responses-token-savings-o67xyn`; CI has
      not run yet -- see 5.1. Needs confirmation once the branch is merged
      or the workflow is dispatched manually.*
- [ ] 5.5 Once CI is confirmed green, archive this change
      (`openspec-archive-change`) to fold the delta spec into
      `openspec/specs/football-fixtures/spec.md`.
