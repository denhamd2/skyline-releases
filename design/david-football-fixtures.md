# Football fixtures on Home — David's profile only

## What this changes

Adds a football fixtures section to `HomeScreen` (`skyline-iptv/app/src/main/java/com/denham/skyline/ui/home/HomeScreen.kt`),
visible only when `selectedFamilyMember == "David"`. Two elements, in one
section:

1. **"Man Utd next"** — a single spotlight card for Manchester United's next
   fixture.
2. **"Football today"** — a horizontal rail of today's fixtures across the
   competitions football-data.org's free tier covers.

Each fixture links out to any channel(s) the EPG shows currently/soon
carrying it.

This is an edit to an existing screen, not a new screen — no kie.ai mockup
was generated (see workflow rules). Ground truth is the current `HomeScreen`
Compose source plus the reference baselines, per below.

## Grounding: current-state capture attempted, fell back to source reading

Per `brain/integrations/live-screenshot-capture.md`, I looked for existing
Roborazzi coverage of `HomeScreen` itself (not just component swatches) —
there is none; `skyline-iptv/app/src/test/java/com/denham/skyline/ui/ScreenshotTests.kt`
only renders a component gallery (`PhoneComponentScreenshots`,
`TvComponentScreenshots`), not the assembled screen. Writing a new scoped
test would require standing up `HomeViewModel`'s full `AppContainer`
dependency graph (Room DB, DataStore, YouTube/EPG/guide repos) — out of
scope for a design brief, and moot here regardless: this environment cannot
resolve the Android Gradle Plugin at all (`skyline-iptv/CLAUDE.md`,
confirmed by running `./gradlew testDebugUnitTest --tests
"*PhoneComponentScreenshots*"`, which hung/timed out rather than resolving
plugins). No screenshot could be captured.

Falling back to reading `HomeScreen.kt` directly, as the fallback path
instructs. Current structure when `selectedMember == "David"` (line numbers
from the file as read):

```
LazyColumn
 ├─ Hero spotlight + wordmark/search/account (unconditional)
 ├─ Continue Watching (unconditional, if lastPlayed != null)
 ├─ [Live Now / provider browse rails — hidden, selectedMember != null]
 ├─ "Who's watching?" member chip row (unconditional)      ← selector
 ├─ "David's channels" pinned rail (if pinned.isNotEmpty())
 ├─ "YouTube for David" carousel (if youtubeVideos.isNotEmpty())
 ├─ Category rails (Football/Sport/WWE/etc. rails, keyword-matched)
 └─ bottom spacer
```

No drift from `brain/reference-designs/` worth flagging here — the
mocks predate the member-selector/pinned-rail/YouTube pattern entirely, so
there's nothing in the baseline to compare a sports-fixtures section against.
`brain/component-screenshots/phone_components.png` is useful only at the
component level (it doesn't show `LiveNowRow` with a score or channel
chips, since those don't exist yet).

Nothing wrong was found in the existing David-selected render worth an
opportunistic fix — this brief is purely additive.

## Placement

Insert the new section **between the pinned-channels rail and the YouTube
carousel**, both still guarded by `selectedMember == "David"` specifically
(not the generic `selectedMember != null` guard used for pins/YouTube):

```
"Who's watching?" selector
"David's channels" pinned rail        (existing, unchanged — explicit pins outrank everything)
[NEW] "Man Utd next" + "Football today"   ← this brief, David only
"YouTube for David" carousel          (existing, unchanged)
category rails                         (existing, unchanged)
```

Reasoning:
- Pins stay first — the code comment at `HomeScreen.kt:681-683` is explicit
  that a member's individually pinned channels "outrank both their
  categories and their YouTube," and that reasoning holds here too: an
  explicit user choice outranks anything automatic.
- Football goes ahead of YouTube and category rails for the same reason the
  existing "Live Now" section sits near the top of the *default* page
  (`HomeScreen.kt:565-587`): it's time-decaying, "happening today" content.
  Burying it under YouTube/category rails would make selecting David look
  like it changed nothing today, which is exactly the failure mode the
  YouTube-carousel comment (`HomeScreen.kt:698-701`) already called out and
  fixed once for that section.
- Scoped to `"David"` specifically, not `selectedMember != null`: this is
  David's personal sports interest (`familyKeywordDefaults["David"]` already
  leads with `"football", "soccer", "sport"`), not a generic per-member
  feature. Anne/Ava/Sophie should never see it.

## Composition

### Section header

Reuse `SectionHeader` (`ui/components/Components.kt:239`), no `onViewAll` —
there's no dedicated fixtures list screen to deep-link to, and adding one
isn't in scope here.

```kotlin
if (selectedMember == "David") {
    item {
        Column(Modifier.enterReveal(revealDelay(0))) {
            SectionHeader("Football")
            // Man Utd spotlight, then today's rail — see below
        }
    }
}
```

Use `enterReveal(revealDelay(0))` exactly as the pinned-channels and
YouTube blocks already do immediately above/below this insertion point —
don't invent a new stagger value.

### 1. "Man Utd next" spotlight

A single, full-bleed-width card above the rail, labelled with a small
sub-heading rather than a second full `SectionHeader` (two headline-weight
headers stacked reads as two unrelated sections, when this is one "Football"
section with a headline pick and a list under it):

```kotlin
Text(
    "Man Utd next",
    style = MaterialTheme.typography.titleMedium,
    color = SkyPalette.TextSecondary,
    modifier = Modifier.padding(horizontal = SkySpacing.gutter, vertical = SkySpacing.s),
)
FixtureCard(fixture = nextManUtdFixture, epgMatches = ..., onPlayChannel = onPlayChannel, modifier = Modifier.padding(horizontal = SkySpacing.gutter).fillMaxWidth())
```

- Full width (minus gutter), not rail-card width — it's a single highlighted
  item, same visual weight class as `ContinueWatchingCard`
  (`HomeScreen.kt:753`), though shorter (no hero-height artwork needed; team
  crests are small).
- If Man Utd's next fixture is also live right now, this card and the "today"
  rail entry are the same match — that's fine, don't dedupe. The spotlight
  card answers "when is United playing next" even outside match windows,
  which is the whole point of pinning it separately from the general list.

### 2. "Football today" rail

Reuse `Rail` (`ui/components/Components.kt:86`) exactly as every other rail
on this screen does — pass `title = ""` since the "Football" `SectionHeader`
above already labels the whole block (same pattern the existing `Rail("",
popular, ...)` and `Rail("", movies, ...)` calls use at `HomeScreen.kt:631,
641`), and supply `FixtureCard` as the item composable:

```kotlin
Rail("", todaysFixtures, key = { it.id }) { fixture ->
    FixtureCard(
        fixture = fixture,
        epgMatches = epgMatchesFor(fixture),
        onPlayChannel = onPlayChannel,
    )
}
```

### New component: `FixtureCard`

No existing component covers this. `LiveNowRow` (`Components.kt:328`) is the
closest analogue but doesn't fit without changing its contract for every
existing caller:

- `LiveNowRow` always renders `LiveBadge()` unconditionally (`Components.kt:375`)
  — it has no non-live state, because every current call site is already
  filtered to `epg[streamId]?.nowTitle != null` before calling it. A fixture
  is very often *not* live (scheduled for later, or already finished) and
  still needs to render.
  A fixture is very often *not* live (scheduled for later, or already
  finished) and still needs to render.
- It has one artwork slot and one title/subtitle pair — no room for two team
  crests, a score, a minute clock, and a row of tappable channel chips.
- It's single-line-row shaped, sized for a vertical list. Fixtures need a
  rail-card footprint (used inside `Rail`) and a full-width footprint (the
  spotlight), so a fixed-height row doesn't fit either use.

This is judged a legitimate new sibling to `ChannelCard`/`PosterCard`/`LiveNowRow`
(a new card *type* for a new content type), not a duplicate — per the
"Reuse before writing" rule, reuses `ArtworkImage`, `LiveBadge`,
`ProviderBadge`, and `SkyPalette`/`SkySpacing`/`SkyRadius`/
`MaterialTheme.typography` tokens throughout rather than inventing new
visual language. Place it in `ui/components/Components.kt` alongside its
siblings, not in `ui/home/`.

**Props** (data-shape agnostic — name the actual fixture/EPG-match types to
match whatever repository layer the developer builds):

```kotlin
@Composable
fun FixtureCard(
    competition: String,               // "Premier League"
    homeTeam: String,
    awayTeam: String,
    homeCrestUrl: String?,
    awayCrestUrl: String?,
    status: FixtureStatus,             // sealed: Scheduled(kickoffLocal: String) | Live(minute: String, homeScore: Int, awayScore: Int) | Finished(homeScore: Int, awayScore: Int)
    channels: List<FixtureChannel>,    // matched EPG channels, may be empty
    onPlayChannel: (FixtureChannel) -> Unit,
    onClick: () -> Unit,               // whole-card tap when exactly one channel matches; see "Tap behaviour" below
    modifier: Modifier = Modifier,
    width: Dp = 240.dp,                // rail width; omit/ignore for the full-width spotlight usage
)
```

**Layout** (top to bottom, `SkyPalette.Surface` background, `SkyRadius.card`
(16dp) corners, `SkySpacing.m` (12dp) internal padding — a step down from
`ChannelCard`'s 8dp padding since this card carries more content and needs
breathing room, still on the 8pt grid):

1. `ProviderBadge(competition)` (existing component, `Components.kt:269`) —
   top-left, exactly as already used for provider/category names elsewhere.
2. Team row: `ArtworkImage` for each crest at 28dp (reuse, not a new image
   loader — same `fallbackIcon = Icons.Default.LiveTv` pattern as
   `ChannelCard`), team names in `MaterialTheme.typography.titleSmall` /
   `SkyPalette.TextPrimary`, "v" separator in `bodySmall` / `TextMuted`.
3. Status row, one of:
   - **Scheduled**: `"KO 17:30"` in `MaterialTheme.typography.labelMedium`,
     `SkyPalette.TextSecondary`. No `LiveBadge`.
   - **Live**: `LiveBadge()` (existing, unmodified) + score
     (`"2–1"`, `titleMedium`, `TextPrimary`) + minute (`"63′"`, `labelMedium`,
     `TextSecondary`). This is the one state where `LiveBadge` genuinely
     applies, unlike a blanket-always-live row.
   - **Finished**: `"FT"` (`labelMedium`, `TextMuted`) + final score
     (`titleMedium`, `TextPrimary`). Finished matches stay in "today's"
     list until midnight rather than disappearing in a David-selected
     session started before the match kicked off and left open past
     full-time — don't drop them from the list, just downgrade the
     visual weight (`TextMuted` label vs. `LiveBadge`+`TextPrimary` for
     live).
4. Channel chips row (new, small — see below), or muted fallback text if
   `channels.isEmpty()`: `"Not on your channels"`, `bodySmall`,
   `SkyPalette.TextMuted`. Don't render an empty chip row — an empty row of
   chip-shaped nothing reads as a loading glitch, not "no match found."

**Channel chips**: small pill per matched channel, `SkyPalette.Accent`-outlined
(not filled — filled chips are `PillButton`'s job for a single primary
action; a card can have 1–3 channel chips and only one is usually the
"right" one, so an outlined/lower-emphasis treatment fits standard
progressive-disclosure practice better than N equally-loud filled
buttons), `SkyRadius.chip` (8dp) corners, `labelSmall` text,
`Icons.Default.PlayArrow` at 12dp leading. Tapping a chip calls
`onPlayChannel(channel)` — resolve straight through to the same
`onPlayChannel: (ChannelEntity) -> Unit` callback `HomeScreen` already
threads through every other rail (`HomeScreen.kt:483`), not a new
navigation path.

**Tap behaviour**: reserve the *card's* own tap target for the single-match
case (0 or 1 channel: tapping anywhere on the card plays that channel
directly, or does nothing useful if zero matched — in that case don't make
the card clickable at all, `scaledClickable` only when `channels.size == 1`).
When 2+ channels match, the card itself is not clickable — only the
individual chips are — so a tap can't be ambiguous about which channel it
meant. This is the same reasoning `ChannelCard`/`PosterCard` don't need
(they always resolve to exactly one destination); fixtures are the first
component here that can legitimately resolve to more than one.

### States

| State | What renders |
|---|---|
| Loading (first fetch, key present) | `ShimmerRail()` (existing, `Components.kt:469`) in place of the "Football today" rail. For the Man Utd spotlight, a single `ShimmerBox()` (existing, `Components.kt:444`) sized to the spotlight card's footprint (`fillMaxWidth().height(120.dp)`, clipped to `SkyRadius.card`) — both are the same shimmer primitive already used for the sync-loading state at `HomeScreen.kt:599-600`, not a new loading treatment. |
| No API key (`BuildConfig.FOOTBALL_DATA_API_KEY` blank) | Section doesn't render at all — same "graceful degradation" contract as `YouTubeRepository`/the YouTube carousel (`HomeScreen.kt:702`, guarded by `youtubeVideos.isNotEmpty()`). Don't show an error card for a missing optional key; that's a developer/CI concern, not a user-facing one. |
| No fixtures today (covered competitions all idle) | Section doesn't render — same pattern as every other conditional rail on this screen (`if (x.isNotEmpty())`). Don't show an explicit empty state for "no matches today" — a football fan on an empty football day doesn't need a card telling them nothing's on. |
| Man Utd: no scheduled fixture returned (e.g. API outage, or genuinely nothing scheduled within the API's lookahead) | Omit the "Man Utd next" sub-block only; still show "Football today" if it has data. Independent failure states — one shouldn't blank the other. |
| Fixtures loaded, scheduled (not yet kicked off) | `FixtureCard` in `Scheduled` state as above. |
| Fixture live | `FixtureCard` in `Live` state — `LiveBadge` + score + minute, sourced from football-data.org's `IN_PLAY`/`PAUSED` status and `score.fullTime`/`minute` (per the task's confirmed data-shape notes) refreshed on the same poll cadence the developer chooses (design does not prescribe polling interval — that's a data-freshness/rate-limit tradeoff, not a UX one, though a live card should visibly update within a minute or two of a goal for the score to be trustworthy). |
| EPG match found (1 channel) | Whole card clickable, see "Tap behaviour." |
| EPG match found (2+ channels) | Chips row, card itself inert, see "Tap behaviour." |
| No EPG match | Muted "Not on your channels" text, card inert. |

### EPG matching (guidance for the developer, not a full spec)

`GuideDao.forChannelsBetween(channelIds, fromMs, toMs)`
(`data/db/Daos.kt:244`) takes an explicit channel-id list and has no title
search — matching a fixture to a channel needs a new query. Two viable
approaches, either is fine from a design standpoint since neither changes
what's on screen:

- A new `GuideDao` query doing `title LIKE '%homeTeam%' AND title LIKE
  '%awayTeam%'` (or `title LIKE '%homeTeam%' OR title LIKE '%awayTeam%'`,
  looser) across **all** channels, windowed to roughly the fixture's kickoff
  ± 90 minutes (`fromMs`/`toMs` bound the query already; reuse that
  parameter shape), rather than the pre-filtered per-tile approach `epg`/
  `nowProgrammeImages` use in `HomeViewModel` today (those are scoped to
  favourites+popular channel ids, which won't include every sports channel
  in the provider's catalogue).
- Or the lighter now/next path (`EpgRepository`/`epg_now_next`) for "is this
  live right now," if the live-only case is what matters most and a title
  LIKE scan is too slow/broad to run for every fixture on every Home load.

Either way: resolve matched `channelStreamId`s through `ChannelDao.byIds`
(existing, `data/db/Daos.kt`, already used at `HomeScreen.kt:199, 366`) to
get real `ChannelEntity`s for the chips — don't invent a parallel channel
model.

### TV focus

`FixtureCard` will render inside a phone-style `Rail`/`LazyRow` here — this
brief scopes phone only, matching the rest of `HomeScreen.kt` (there is no
TV variant of this screen's rails today; TV has its own `tv/` composables).
If a developer later ports this to the TV home surface, it needs the
standard TV treatment this design system requires for any shared component
— 1.04 scale + white outline focus at 140ms via the existing TV focus
pattern, and the channel chips need to become genuinely D-pad-navigable
individually (not just a scroll-in a single focusable card), since a fixture
can carry more than one channel and TV has no touch fallback to disambiguate
a tap. Flagging this now so it isn't silently assumed to "just work" if the
component gets reused on TV later, per the hard rule that TV focus must be
checked explicitly, never assumed.

### Football-data.org coverage caveat (must be visible in copy, not just this brief)

The free tier covers ~13 major competitions (Premier League, Champions
League, and the other top European domestic leagues) and does **not**
include international friendlies or lower-tier/non-European football.
"Football today" in this feature means "today's fixtures in the API's
covered competitions," not literally all football being played. This is a
data-source limitation, not a design choice to hide content — no UI
treatment can fully paper over it, but the section header stays plain
"Football" rather than "All football today" so it doesn't overclaim
completeness the data can't back up.

## Tokens used (self-check)

- Colour: `SkyPalette.Surface` (card background), `SkyPalette.TextPrimary`
  (scores, team names), `SkyPalette.TextSecondary` (kickoff time, minute
  clock), `SkyPalette.TextMuted` (FT label, "not on your channels"),
  `SkyPalette.Accent` (channel chip outline/icon), `SkyPalette.LiveRed` (via
  existing `LiveBadge`, unmodified).
- Spacing: `SkySpacing.gutter` (section/card horizontal padding),
  `SkySpacing.m` (card internal padding), `SkySpacing.s` (sub-heading
  vertical padding).
- Corners: `SkyRadius.card` (16dp, card), `SkyRadius.chip` (8dp, channel
  chips).
- Type: `MaterialTheme.typography.titleMedium` (sub-heading, team names),
  `titleSmall`/`labelMedium`/`labelSmall`/`bodySmall` as itemised above —
  never a hardcoded `fontSize`.
- Reused components: `SectionHeader`, `Rail`, `LiveBadge`, `ProviderBadge`,
  `ArtworkImage`, `ShimmerRail`, `ShimmerBox`, `enterReveal`/`revealDelay`.
- New component: `FixtureCard` (justified above) plus a small internal
  channel-chip composable (not a new top-level export — keep it private to
  `FixtureCard`'s file unless another screen needs it later).

## Visuals

No mockup generated (this is an edit to an existing screen, not a new
screen/flow — kie.ai generation is reserved for that path). No live
screenshot captured either: `HomeScreen` has no existing Roborazzi coverage
of the assembled screen, standing one up requires the full `AppContainer`
dependency graph, and this environment cannot resolve the Android Gradle
Plugin at all to run one regardless (`skyline-iptv/CLAUDE.md`; confirmed by
a timed-out `./gradlew testDebugUnitTest` run). Grounding is the
`HomeScreen.kt` source read directly, cited by line above, plus the token
and component references throughout this brief.

## Ready for implementation

- **Brief**: `design/david-football-fixtures.md`
- **Visuals**: skipped: edit to an existing screen (no new-screen mockup
  applies) and no Roborazzi coverage exists for `HomeScreen` to capture a
  live screenshot from; this environment also cannot run Gradle to create
  one. Grounded directly in `HomeScreen.kt` source instead (cited by line
  throughout the brief).
- **Summary**: Add a David-only "Football" section to Home (Man Utd next-fixture
  spotlight + today's-fixtures rail) between the pinned-channels rail and
  the YouTube carousel, each fixture linking to matched EPG channels.
- **Reuse/tokens**: Reuses `SectionHeader`, `Rail`, `LiveBadge`,
  `ProviderBadge`, `ArtworkImage`, `ShimmerRail`, `ShimmerBox`,
  `enterReveal`/`revealDelay`, and `ChannelDao.byIds`/`onPlayChannel`;
  adds one new component `FixtureCard` (justified — no existing component
  supports a non-live status state, score+minute, or multi-channel chip
  linking); all colour via `SkyPalette.*`, spacing via `SkySpacing.*`,
  corners via `SkyRadius.card`/`SkyRadius.chip`, type via
  `MaterialTheme.typography.*`.
- **Open questions**:
  1. Confirm football-data.org team id 66 = Manchester United before wiring
     (flagged as unconfirmed in the task's own notes).
  2. Whether `FOOTBALL_DATA_API_KEY` should follow `YOUTUBE_API_KEY`'s exact
     CI-secret/`BuildConfig` wiring pattern (`app/build.gradle.kts:38-45`) —
     assumed yes per the task notes, but not yet confirmed with a repo
     maintainer/CI owner.
  3. EPG title-matching approach (new `GuideDao` LIKE query vs. leaning on
     `epg_now_next`) is a developer implementation choice with no UI-visible
     difference; flagged in the brief for awareness, not blocking.
