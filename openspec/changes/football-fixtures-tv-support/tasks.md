## 1. TV fixture card (`tv/TvComponents.kt`)

- [ ] 1.1 Add `TvFixtureCard(competition, homeTeam, awayTeam, homeCrestUrl,
      awayCrestUrl, status: FixtureStatus, channels: List<ChannelEntity>,
      onPlayChannel, modifier, width: Dp? = 240.dp, isSpotlight: Boolean =
      false)` -- same prop shape as phone's `FixtureCard`
      (`ui/components/Components.kt:416-427`) for drop-in parity, reusing
      `ArtworkImage`/`LiveBadge`/`ProviderBadge` and
      `SkyPalette`/`SkySpacing`/`SkyRadius` tokens.
- [ ] 1.2 Wrap the card in `Modifier.tvClickable(...)`
      (`tv/TvComponents.kt:50-81`) for its default action (single-match
      case), matching the 1.04x/white-outline/140ms treatment every other
      TV card uses.
- [ ] 1.3 Add `TvFixtureChannelChip`, a TV counterpart to phone's private
      `FixtureChannelChip`: same visual (outlined pill, `SkyRadius.chip`,
      `Accent` border/text/icon) but individually `tvClickable`, rendered
      for every matched channel unconditionally (not gated by channel
      count the way phone's chip-vs-whole-card split is).
- [ ] 1.4 Status row (Scheduled/Live/Finished) and empty-channels fallback
      text: reuse the same layout/token choices as phone's `FixtureCard`
      (`Components.kt:496-554`) so the two platforms read as the same
      component family.

## 2. TV Home Football section (`tv/TvScreens.kt`)

- [ ] 2.1 Add `onViewAllFixtures: () -> Unit = {}` parameter to
      `TvHomeScreen`.
- [ ] 2.2 Collect `viewModel.footballSection`/`viewModel.fixtureChannels`
      (same StateFlows `HomeViewModel` already exposes) alongside
      `TvHomeScreen`'s existing `collectAsState()` calls.
- [ ] 2.3 Insert a "Football" section item, gated `selectedMember ==
      "David"`, mirroring `HomeScreen.kt:810-877`'s structure: "Man Utd
      next" label + spotlight `TvFixtureCard(isSpotlight = true)`, then
      `TvRailHeader` + horizontal `LazyRow` of `roundFixtures` using
      `TvFixtureCard`, with a "View all" control that calls
      `onViewAllFixtures`.
- [ ] 2.4 Loading state: reuse the existing `ShimmerBox`/`ShimmerRail`
      pattern already used elsewhere on TV/phone for this section's
      `FootballSectionState.Loading`.

## 3. TV fixtures full list

- [ ] 3.1 Add `TvFixturesScreen(fixtures: List<Fixture>, fixtureChannels:
      Map<String, List<ChannelEntity>>, onPlayChannel, onBack)` -- vertical
      `LazyColumn` of full-width `TvFixtureCard`s (`width = null`), one
      per row, `Modifier.enterReveal(revealDelay(index))` per card, same
      empty-state message as phone's `FixturesScreen.kt`. Not a grid.
- [ ] 3.2 Place in a new `tv/TvFixturesScreen.kt` file (or appended to
      `tv/TvScreens.kt`, developer's call) following whichever this
      codebase's file-per-screen convention favours for TV.

## 4. Navigation wiring (`tv/TvMainActivity.kt`)

- [ ] 4.1 Add `var showFixtures by remember { mutableStateOf(false) }` in
      `TvRoot`, alongside the existing `playing` state.
- [ ] 4.2 Pass `onViewAllFixtures = { showFixtures = true }` into
      `TvHomeScreen`'s call site (`TvMainActivity.kt:189-191`).
- [ ] 4.3 Render `TvFixturesScreen` as a full-screen overlay when
      `showFixtures` is true, outside the tab `Crossfade`, the same way
      `TvPlayerOverlay` is layered when `playing` is true
      (`TvMainActivity.kt:247-255`). Wire its `onBack` (and the "View all"
      selection callback for matched channels) to close the overlay and
      hand off to the player as appropriate.
- [ ] 4.4 Extend the existing `BackHandler(enabled = !playing && tab !=
      TvTab.HOME) { tab = TvTab.HOME }` block (`TvMainActivity.kt:262-264`)
      with a preceding `BackHandler(enabled = showFixtures) { showFixtures
      = false }`, matching the `playing` handler's precedence pattern
      immediately above it.

## 5. Verification

- [ ] 5.1 Run `./gradlew testDebugUnitTest` -- confirm it compiles and
      passes (cannot be run locally in this environment; needs CI).
- [ ] 5.2 Run `./gradlew detekt` / `detektDesignSystem` -- confirm no raw
      `Color(0x...)` literals or other design-system violations in the new
      TV files (cannot be run locally in this environment; needs CI).
- [ ] 5.3 Static self-review: every new TV composable traces its
      colour/spacing/corner/type choices back to an existing
      `SkyPalette`/`SkySpacing`/`SkyRadius`/`MaterialTheme.typography`
      token, matching the phone `FixtureCard`/`FixturesScreen` it mirrors.
- [ ] 5.4 Push and confirm the `Build Skyline APK` workflow run is green
      (unit tests, APK build/publish, `detektDesignSystem` all actually
      ran) -- a push alone is not "done" per `skyline-iptv/CLAUDE.md`.
- [ ] 5.5 Once CI is confirmed green, archive this change
      (`openspec-archive-change`) to fold the delta spec into
      `openspec/specs/football-fixtures/spec.md`.
