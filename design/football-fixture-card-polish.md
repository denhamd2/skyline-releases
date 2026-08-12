# FixtureCard polish pass

## Summary

Dedicated visual-polish review of `FixtureCard`/`FixtureChannelChip`
(`ui/components/Components.kt:415-572`), used for both the "Man Utd next"
spotlight and the "Football today" rail on David's Home tab
(`ui/home/HomeScreen.kt:808-869`). The post-implementation review in
`design/david-football-fixtures.md` already confirmed the shipped feature
matches its original brief closely and flagged one functional gap (kickoff
date qualifier, since fixed). This pass is purely visual polish — no
functional or behavioural change — covering five findings.

## Grounding

Checked `docs/skyline-screenshots/` for a live capture first, per the
live-screenshot-capture workflow. `HomeScreenshotTest.kt` covers the
assembled Home screen in the David-selected state
(`docs/skyline-screenshots/home_david.png`, now committed by CI as of
commit `133526d`), but its own setup deliberately keeps the Football
section in its `Hidden` state — `FOOTBALL_DATA_API_KEY` is a CI-only
`BuildConfig` field, blank in the local/test build the screenshot test
runs against, so the section never renders in that capture. No other
live screenshot of the rendered section exists to fall back to.

Grounded instead in direct source reads: `Components.kt:395-572`
(`FixtureCard`, `FixtureChannelChip`), `HomeScreen.kt:808-869` (both call
sites), and `docs/COMPONENT_LIBRARY.md`/`ui/theme/Theme.kt` for token
reference — same fallback path the original brief and its review used.

## Findings

### 1. Spotlight and rail cards aren't visually differentiated

Both the "Man Utd next" spotlight and every rail card use the identical
flat `SkyPalette.Surface` background and `SkyRadius.card` (16dp) corners
(`Components.kt:437-438`) — the only difference between the two call
sites is `width` (`HomeScreen.kt:844` vs. the rail default). Compare
`ChannelCard`, which signals higher visual weight for its "now playing"
tiles with a `SkyPalette.SurfaceElevated → SkyPalette.Indigo` gradient
(`Components.kt:177-184`). The spotlight is Home's single most prominent
piece of football content (David's own next match) and currently reads
as just a wider version of the same card that repeats six more times in
the rail below it.

**Recommendation**: add an `isSpotlight: Boolean = false` parameter to
`FixtureCard`. When true, use `SkyRadius.hero` (22dp — defined in
`Theme.kt`, not yet used anywhere in the codebase) instead of
`SkyRadius.card`, and the same `SurfaceElevated → Indigo` gradient
background `ChannelCard` already establishes, rather than flat `Surface`.
Pass `isSpotlight = true` only at the spotlight call site
(`HomeScreen.kt:835`).

### 2. Away-team name misaligns with its crest

Both team-name `Text` composables default to `TextAlign.Start`
(`Components.kt:452-459` and `461-468`). The home name correctly hugs its
crest on the left. The away name, left-aligned inside its own
`weight(1f)` slot, hugs the "v" separator instead of its crest on the
right — so the row reads asymmetrically even though the layout intends a
mirrored "crest · name · v · name · crest" composition.

**Recommendation**: add `textAlign = TextAlign.End` to the away-team
`Text` at `Components.kt:461-468`. One-line change, no new tokens.

### 3. Status row has inconsistent visual weight across states

`Live` gets a red `LiveBadge()` (`Components.kt:493`) and `Finished` gets
a muted "FT" tag (`Components.kt:511`) — both have a small anchor/icon
before the text. `Scheduled`, the most common state in a "today's
fixtures" rail, is bare `Text` with no anchor (`Components.kt:482-486`).
In a rail mixing all three states, the two anchored states catch the eye
and the un-anchored `Scheduled` cards read as visually incomplete next to
them, even though nothing is actually missing.

**Recommendation**: give `Scheduled` a small leading icon to match the
other two states' visual pattern — e.g. `Icons.Default.Schedule` (or
similar clock icon) at 12-14dp, `SkyPalette.TextMuted`, in a `Row` with
`Arrangement.spacedBy(SkySpacing.xs)` ahead of the existing kickoff-time
`Text`, mirroring the `Row`/spacing structure already used for `Live`
and `Finished` (`Components.kt:489-491`, `507-509`).

### 4. Nested horizontal scroll on the channel-chip row

The channel-chip row uses its own `Modifier.horizontalScroll(...)`
(`Components.kt:530-532`) *inside* a `FixtureCard` that itself sits
inside `Rail`'s horizontally-scrolling `LazyRow` (for the rail call
site). Two independently-scrollable horizontal regions nested inside each
other is a known gesture-conflict pattern — a drag starting on a chip
can be captured by the inner scroll, fighting the rail's own horizontal
swipe.

**Recommendation**: replace the scrolling `Row` with a wrapping layout —
`FlowRow` (Compose's built-in, already available via the same Foundation
layout APIs used elsewhere in this file) with
`horizontalArrangement = Arrangement.spacedBy(SkySpacing.xs)` and
`verticalArrangement = Arrangement.spacedBy(SkySpacing.xs)`. Most fixtures
match 0-2 channels per the EPG-matching design (kickoff ±90 min window),
so wrapping to a second line is the expected case, not an edge case, and
removes the nested-scroll conflict entirely. This card is phone-only
today per its own doc comment (`Components.kt:407-412`), so no TV D-pad
implications from this change.

### 5. `FixtureCard`/`FixtureChannelChip` missing from `docs/COMPONENT_LIBRARY.md`

Minor documentation gap, not a visual defect: neither component is listed
in the component library doc, so a future design pass has no catalogued
reference for them and risks re-deriving or duplicating the pattern.

**Recommendation**: add both to `docs/COMPONENT_LIBRARY.md` following the
existing entry format for other cards (`ChannelCard`, `PosterCard`), once
finding 1-4's changes land — document the shipped shape, not the
pre-polish one.

## Checked, not a problem

- The crest's raw `4.dp` corner radius (`Components.kt:450, 474`) —
  inherited convention already reviewed and approved in
  `design/david-football-fixtures.md`'s post-implementation review, not a
  new defect to re-flag.
- Competition-badge lowercasing via `ProviderBadge` — existing shared
  component behaviour, out of scope for this card-specific pass.

## Tokens used (self-check)

All recommendations above use only existing `SkyPalette`/`SkySpacing`
tokens already in use elsewhere in this file, plus `SkyRadius.hero` —
defined in `Theme.kt` but not yet referenced anywhere in the codebase
until finding 1 adopts it. No new tokens, no raw hex or off-grid literals
introduced.

## Visuals

**Skipped.** `KIE_AI_API_KEY` was checked for reachability three separate
times today, each in a genuinely fresh session/container on this
environment (two via `mcp__Claude_Code_Remote__create_session`, one via
this same in-process agent's own container), after the user added it to
this environment's settings. All three reported the variable **unset** at
check time. Rather than keep re-attempting an API call with no key to
authenticate it, this brief proceeds without a mockup — the five findings
above are small, source-precise, and don't need a generated visual to be
actionable. Regenerate a mockup once the key is confirmed reachable (a
separate, live football-data.org key added the same way did reach a
fresh session, so this looks solvable — likely just needs one more fresh
container after the latest save, or the container-reuse issue this
investigation kept running into with resumed/in-process sessions rather
than genuinely new ones).

## Ready for implementation

- **Brief**: `design/football-fixture-card-polish.md` (this file).
- **Visuals**: skipped — see "Visuals" section above; `KIE_AI_API_KEY` not
  reachable across three fresh-session checks despite being added to
  environment settings.
- **Summary**: five polish findings for `FixtureCard`/`FixtureChannelChip`
  — spotlight/rail differentiation (new `isSpotlight` param, `SkyRadius.hero`
  + gradient), away-team crest alignment (`TextAlign.End`), a leading icon
  for the `Scheduled` status row to match `Live`/`Finished`'s visual
  weight, replacing the nested-scroll channel-chip row with a wrapping
  `FlowRow`, and adding both components to `docs/COMPONENT_LIBRARY.md`.
  All purely visual — no behavioural or functional change, no new spec
  delta needed against `openspec/changes/football-fixtures-home`.
- **Reuse/tokens**: no new components. One newly-adopted existing token
  (`SkyRadius.hero`). `FlowRow` from Compose Foundation, already available,
  not currently used elsewhere in this file but not a new dependency.
- **Open questions**: none blocking implementation. Visuals should be
  regenerated once `KIE_AI_API_KEY` is confirmed reachable, but that's a
  documentation nice-to-have, not a blocker to shipping these five
  changes.
