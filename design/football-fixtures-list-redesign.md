# Football section: spacing fix + carousel-to-list redesign

## Retroactive process note

This change was implemented and merged/pushed directly, without a design
brief or mockup: PR #9 (`denhamd2/skyline-releases`), branch
`claude/fixture-spacing-carousel-x0s6qn`, commit `6760a60` ("Give the
round-fixtures list room and drop the carousel"), open against `main`. This
brief is written **after the fact**, following the normal edit-to-an-
existing-screen workflow exactly as if the change hadn't shipped yet, so it
gets a proper record. Verdict up front: **the shipped implementation matches
what a from-scratch brief would have specified — no blocking defects, one
positive finding worth calling out explicitly, two low-priority
observations for the backlog.**

## What shipped

File: `skyline-iptv/app/src/main/java/com/denham/skyline/ui/home/HomeScreen.kt`,
David-only "Football" section, `~line 794-907`. Confirmed by diffing
`origin/main` against `origin/claude/fixture-spacing-carousel-x0s6qn`
directly (not just reading the task description):

1. **Spacing fix**: `Spacer(Modifier.height(SkySpacing.l))` added between
   the "Man Utd next" spotlight `FixtureCard` and the round-fixtures list
   below it. The old code relied on `Rail`'s own title padding for
   separation, but `Rail` was called with an empty title (`Rail("",
   football.roundFixtures, ...)`{.kt}) — `Rail`'s title `Text` only renders
   `if (title.isNotBlank())` (`Components.kt:104`), so with an empty string
   nothing rendered and the spotlight card butted straight up against the
   first rail card with zero gap.
2. **Carousel dropped, replaced with a labelled vertical list**: the
   `Rail("", football.roundFixtures, ...)` horizontal `LazyRow` carousel is
   now a `"This round"` label (`titleMedium` / `SkyPalette.TextSecondary`,
   same treatment as the existing `"Man Utd next"` label immediately above
   it) followed by a plain `Column` of full-width `FixtureCard`s
   (`width = null`), `Arrangement.spacedBy(SkySpacing.m)`, wrapped
   individually in `key(fixture.id) { ... }`.
3. **Loading skeleton updated to match**: the horizontal `ShimmerRail()`
   was replaced with three vertically-stacked `ShimmerBox`es
   (`fillMaxWidth().height(120.dp)`, `SkyRadius.card` clip,
   `spacedBy(SkySpacing.m)`), matching the new vertical shape instead of the
   old horizontal-carousel shape.

`FixtureCard`, `Rail`, and `ShimmerRail` themselves were **not** modified —
correct, since `Rail`/`ShimmerRail` are still used by every other rail on
Home (pinned channels, category rails) and changing their shared behaviour
for one call site would have been a much larger blast radius than this fix
needed.

## Grounding

Followed the edit-to-an-existing-screen path: looked for a live screenshot
of the Football section first, before falling back to source.

**Live screenshot: not obtainable, for two independent reasons, both
confirmed directly in this session, not assumed from prior briefs:**

1. `skyline-iptv/app/src/test/java/com/denham/skyline/ui/home/HomeScreenshotTest.kt`
   is the only Roborazzi coverage of the assembled `HomeScreen`
   (`docs/skyline-screenshots/home_david.png`). Its own doc comment
   (lines 78-85) states it deliberately leaves the Football section in its
   `Hidden` state, because `BuildConfig.FOOTBALL_DATA_API_KEY` is empty in a
   local/test build (real value is a CI-only secret) and there's no seam to
   force it on. I opened `home_david.png` directly to confirm: it shows
   "Continue Watching" → "Who's watching?" → "David's channels" →
   "Sky Sports Football" category rail, with **no Football section at all**
   — consistent with the doc comment, not stale documentation.
2. Independently of (1), I ran
   `./gradlew testDebugUnitTest --tests "*HomeScreenshotTest*"` in this
   environment to check whether the constraint had changed. It fails fast
   (~34s) on Android Gradle Plugin resolution (`Plugin [id:
   'com.android.application', version: '8.11.1'] was not found`), matching
   `skyline-iptv/CLAUDE.md`'s standing note that this environment cannot
   resolve AGP at all. Even if (1) didn't block the Football section
   specifically, this would block capturing anything.

Per the brief workflow's fallback path for a non-Roborazzi-coverable
screen: read the Compose source directly instead of blocking. Grounded in:

- `HomeScreen.kt:794-907` (the section itself, plus its surrounding
  comment block explaining placement/scoping — unchanged by this PR).
- `HomeScreen.kt:122-128, 311-330` (`FootballSectionState`, confirming
  `roundFixtures` is sourced from `footballRepository
  .upcomingPremierLeagueRound(apiKey)` — a whole Premier League matchweek,
  not literally "today's" fixtures; relevant to the "This round" label
  review below).
- `Components.kt:415-598` (`FixtureCard`, `FixtureChannelChip`, unchanged
  by this PR) and `Components.kt:94-123, 688-712` (`Rail`, `ShimmerRail`,
  also unchanged — read to confirm *why* the old spacing bug happened and
  *why* `ShimmerRail` doesn't fit the new shape, see below).
- `ui/live/LiveScreen.kt:238-270` (`FeaturedLiveCard` + `"Up Next"` +
  `LazyColumn` of `ChannelRow`) — the pattern this PR's own commit message
  cites as precedent; read directly to check the claim rather than take it
  on faith (see "Reused LiveScreen.kt pattern" below).
- `design/david-football-fixtures.md` and
  `design/football-fixture-card-polish.md` — the two prior design docs for
  this feature, for continuity (open items, prior review verdicts).

No new opportunistic defect found in the parts of the render this fix
touches beyond what's already flagged below.

## Design-system review

### 1. Spacing token: `SkySpacing.l` — correct

16dp is the right size class here: it's a *section-internal* gap between
two related-but-distinct blocks within one "Football" section (spotlight
card → round list), not a full section-to-section gap (which would
typically be larger, e.g. via `SectionHeader`'s own spacing) and not a
tight internal-card gap (`SkySpacing.s`/`m`, used *within* a card and
*between* list items respectively). It matches the vertical rhythm already
established one level up: the "Man Utd next" label itself uses
`SkySpacing.s` vertical padding, and the section's items are separated by
`SkySpacing.m`; `SkySpacing.l` for the bigger break between the two
sub-blocks is a sensible next step up the 8pt scale (8 → 12 → 16), not an
arbitrary choice. On-grid, correctly sourced from `Theme.kt`, no raw dp.

### 2. "This round" label — correct, and fixes a real gap the carousel had

Reusing the exact `titleMedium` / `SkyPalette.TextSecondary` treatment
already established by the sibling `"Man Utd next"` label is the right
call: both are sub-labels *within* one `SectionHeader("Football")`, so they
need to read as siblings at the same weight class, not as two competing
headlines. This is a meaningfully better choice than literally copying
`LiveScreen.kt`'s `"Up Next"` header style, which is `headlineSmall`
(unstyled default `TextPrimary`) — heavier than would suit a second
sub-label under an existing `SectionHeader`. Good instinct to borrow the
*structural* pattern from `LiveScreen.kt` (featured card → label → vertical
list) without borrowing its *typographic* weight, which belongs to a
screen with no other header above it.

Beyond matching the token, the label closes an actual pre-existing gap:
before this PR, `Rail("", football.roundFixtures, ...)` had **no label at
all** — a blank `Rail` title. A user saw a spotlight card followed
immediately by an unlabelled row of fixture cards with no indication of
what they were looking at (Today's fixtures? This week's? All upcoming?).
`roundFixtures` is sourced from `upcomingPremierLeagueRound` (confirmed
above) — a full matchweek, which can legitimately include matches on
different days. "This round" is the accurate football-terminology label for
that (a "round" = a matchweek, standard usage), correctly distinct from
"today." This is a genuine UX fix bundled into what the task description
frames as a layout change, not just a restyle — worth surfacing explicitly
since it wasn't called out as intentional in the two-line change summary.

### 3. Vertical-list layout over a grid — correct for this content

`FixtureCard` at full width carries a lot per row: competition badge, two
crests, two team names, a "v" separator, a status row (badge/score/minute
or kickoff time), and a wrapping row of channel chips. A 2-column grid
would force all of that into roughly half the width, which the card's
existing internal layout (`Components.kt:450-565`) isn't built for — team
names would truncate far more aggressively and the channel-chip `FlowRow`
would have much less room to lay chips out without wrapping to 3+ rows per
card. A single-column vertical list is the right density for this content,
and it's exactly what `docs/COMPONENT_LIBRARY.md`'s own "Quick Start"
guidance says: *"Arrange in sections — use Rails for horizontal scrolls,
Columns for vertical"* (`docs/COMPONENT_LIBRARY.md:604`). No existing Grid
component (`LazyVerticalGrid`, used for Movies/Series browse) was reused
where a Column was actually correct — right call, not a missed-reuse
defect.

One implementation detail worth confirming as *also* correct, not just
incidental: the new list uses a plain `Column` with `.forEach`, **not** a
nested `LazyColumn`. That's the right choice given where this renders —
the whole Football section is already a single `item { }` inside
`HomeScreen`'s outer `LazyColumn` (`HomeScreen.kt:811`), and a `LazyColumn`
nested inside another `LazyColumn`'s item without a bounded/measured height
is a well-known Compose footgun (unbounded constraint crash or broken
scroll ownership). A plain `Column` sidesteps that entirely, and is
perfectly fine performance-wise here since `roundFixtures` is a bounded set
(one Premier League matchweek — single digits to low teens of fixtures),
not an open-ended list that needs lazy virtualization.

### 4. Reused `LiveScreen.kt` "featured + vertical list" pattern — structurally matched, appropriately not copied verbatim

Read `LiveScreen.kt:238-270` directly to check the claimed precedent
rather than take it on faith: `FeaturedLiveCard` → `"Up Next"` label
(`headlineSmall`, raw `16.dp`/`8.dp` padding, not `SkySpacing` tokens) →
`LazyColumn` of `ChannelRow`. The **shape** of the pattern (one featured
card, one label, one vertical list below it) is genuinely the same
structure this PR reuses for the Football section, and mirroring an
established structural pattern rather than inventing a new one is the
right instinct per the design system's reuse-first rule. Two details are
deliberately *not* copied 1:1, both correctly:

- Label typography differs (`titleMedium`/`TextSecondary` here vs.
  `headlineSmall`/default here) — already covered in finding 2, correct
  given the different context (LiveScreen's "Up Next" is the screen's only
  header; this "This round" is a second sub-label under an existing
  `SectionHeader`).
- List container differs (`Column` here vs. `LazyColumn` there) — already
  covered in finding 3, correct given the different nesting context
  (LiveScreen's `LazyColumn` is the screen's own top-level scroll
  container, not nested inside another one).

Not a defect that these two details diverge from the cited precedent —
flagging it so a future reviewer doesn't mistake selective adoption for an
incomplete copy.

**Note found while reading `LiveScreen.kt`, out of scope for this PR:**
`"Up Next"`'s padding is `16.dp`/`8.dp` as raw literals, not
`SkySpacing.gutter`/`SkySpacing.s` — same numeric values, so not a visual
or grid defect, but not sourced from the token object either. This
predates this PR, isn't touched by it, and isn't something this brief asks
to fix — noted for the record per the "flag defects you find" rule, not a
blocker.

### Not reusing `ShimmerRail()` for the loading state — correct, and worth defending explicitly

At first glance, replacing `ShimmerRail()` with three manually-stacked
`ShimmerBox`es could look like a "reuse before writing" violation — the
system explicitly calls out `Rail`/`ShimmerRail` as components to check
before writing new UI. It isn't one here: `ShimmerRail()`
(`Components.kt:690-712`) renders a title-bar skeleton *and a horizontal
row of three boxes* — it is shaped for a carousel, not a vertical list.
Keeping it would have shown a horizontal skeleton that then pops into a
vertical list once the real data loads — a layout-shift mismatch, which is
worse than the small amount of duplication avoided. Building the loading
state to match the shape it's actually loading into is the correct
priority here over blind component reuse. Worth stating explicitly in this
brief so a future pass doesn't "simplify" this back to `ShimmerRail()`
without understanding why it was deliberately not used.

### Contrast check (per general UX practice)

`SkyPalette.TextSecondary` (`#8FA0B5`) is used for both "Man Utd next" and
the new "This round" label directly on `SkyPalette.Canvas`
(`#05070A`) — the Home screen's page background, not a card surface.
`docs/SKY_DESIGN_SYSTEM.md`'s WCAG AAA table (line 219-224) only tabulates
`TextPrimary` on `Canvas`/`Surface` and doesn't cover `TextSecondary`;
computed the ratio directly (standard WCAG relative-luminance formula):
**≈7.55:1**, which clears the 7:1 AAA threshold for normal-size text with
room to spare. Not a new risk — this is the same token/background
combination already in production use for "Man Utd next" before this PR —
but flagging the doc gap: `docs/SKY_DESIGN_SYSTEM.md`'s contrast table
doesn't list `TextSecondary`/`TextMuted` against `Canvas`/`Surface`, which
is exactly the combination this rule instructs checking. Low-priority
documentation follow-up, not a defect in this PR.

## Findings summary

| # | Item | Verdict |
|---|---|---|
| 1 | `SkySpacing.l` for the spotlight-to-list gap | Correct token, correct size class |
| 2 | "This round" label, reusing "Man Utd next" styling | Correct — also fixes a real missing-context gap the carousel had |
| 3 | Vertical `Column` over a grid or nested `LazyColumn` | Correct for the content and the nesting context |
| 4 | Structural (not literal) reuse of `LiveScreen.kt`'s pattern | Correct — selective adoption, not an incomplete copy |
| 5 | Not reusing `ShimmerRail()` for the new loading state | Correct — shape must match what's loading, not just any existing shimmer |
| 6 | `TextSecondary` on `Canvas` contrast | Passes AAA (~7.55:1), pre-existing combination, doc gap noted |

No implementation-blocking defects. Nothing in this PR needs to be
reverted or reworked.

## Tokens used (self-check)

- Colour: `SkyPalette.TextSecondary` ("This round" label, matches "Man Utd
  next"). No raw hex introduced.
- Spacing: `SkySpacing.l` (spotlight→list gap), `SkySpacing.m`
  (inter-fixture-card gap, shimmer gap), `SkySpacing.gutter`/`SkySpacing.s`
  (horizontal padding / label vertical padding, both pre-existing patterns
  reused unchanged). No off-grid dp introduced.
- Corners: `SkyRadius.card` (shimmer boxes, matching `FixtureCard`'s own
  non-spotlight corner radius). No new radius introduced.
- Type: `MaterialTheme.typography.titleMedium` (label). No hardcoded
  `fontSize`.
- Reused components: `FixtureCard`, `ShimmerBox` (existing, unmodified).
  `Rail`/`ShimmerRail` correctly *not* reused at this call site, per
  finding 5 above.
- No new components added.

## Visuals

**Mockup regenerated as image-to-image, grounded in the real current
screenshot** (superseding an earlier text-to-image version — see "Revision
history" below):
`design/football-fixtures-list-redesign/mockups/home-football-section.png`
(`gpt-image-2-image-to-image`, 941×1672, `taskId`
`139b9efad7f3337d814662f1454b9cf6`). Input image was
`docs/skyline-screenshots/home_david.png` (1078×2399, the real committed
Roborazzi capture of `HomeScreen` with David selected — see "Grounding"
above), uploaded via kie.ai's `file-base64-upload` endpoint per
`docs/integrations/kie-ai-image-generation.md`. Prompt (saved at
`design/football-fixtures-list-redesign/mockups/prompt.txt`) instructed
preserving the screenshot's existing content pixel-faithfully (header
wordmark/icons, hero card, Continue Watching, Who's watching chips,
"David's channels" pinned rail, "Sky Sports Football" category rail) and
inserting the Football section — "Man Utd next" spotlight card, gap, "This
round" label, three vertically-stacked full-width fixture cards — directly
between the "MUTV" pinned-rail card and the "Sky Sports Football" category
rail, matching `HomeScreen.kt`'s actual render order (finding above). The
prompt also specified the real 5-item bottom nav (Home/Live/Films/Series/TV
Guide, `SkyPalette.Accent` blue on the selected "Home" icon+label only, no
pill indicator, `SkyPalette.Canvas` bar background) appended once at the
bottom of the composite, correcting the previous version's invented nav.

Visual result, checked against the prompt: header/hero/Continue
Watching/Who's watching chips/pinned rail are unchanged from the real
screenshot; the Football section renders in the correct position with the
right internal structure (spotlight card, gap, "This round" label, three
stacked fixture cards, no carousel); the "Sky Sports Football" category
rail still follows it; the bottom nav bar shows the correct 5 real items
with only "Home" highlighted blue. One minor model liberty: the "Opponent
FC" crest rendered as a generic placeholder badge rather than a blank/grey
crest — cosmetic, doesn't affect the layout review.

## Revision history

**v2 (current)**: image-to-image, grounded in `docs/skyline-screenshots/home_david.png`,
correct real bottom nav, full home-screen composite. Replaced v1 in place
(same file path) per the user's request after reviewing v1 and flagging its
nav bar as wrong.

**v1 (superseded, not kept in the repo)**: pure text-to-image
(`gpt-image-2-text-to-image`, `taskId` `b4ddf318b4667b6c444e35ffcb1f1e8d`),
cropped to just the Football section with no surrounding screen content.
Its biggest defect, caught by the user: it **invented** a bottom nav bar
("Home / TV / Sports / Downloads / Search" with a football icon) that does
not match the real app (`ui/navigation/SkylineNavHost.kt`'s actual 5 items
are Home/Live/Films/Series/TV Guide — no Sports, Downloads, or Search tab
exists on the bottom bar). Root cause: text-to-image has no ground truth to
work from, so the model guessed plausible-looking nav rather than the real
one — exactly the failure mode image-to-image grounding (this doc's v2,
and the source-of-truth hierarchy's general preference for a live
screenshot over guesswork) exists to prevent. v1's *first* generation
attempt had additionally been blocked by this session's network egress
policy (`api.kie.ai:443` denied with `403`); the user widened the
environment's network access to Full, which is what let v1 (and then v2)
actually reach kie.ai at all — recorded here since it's a real environment
constraint future sessions may hit again.

## Open items carried over from prior briefs (not part of this PR, for the record)

- `design/david-football-fixtures.md`'s kickoff-date-qualifier
  recommendation for the "Man Utd next" spotlight (bare "KO 17:30" with no
  date, misleading when the next fixture is days away) is still
  unaddressed — unrelated to this PR's scope, not reintroduced or worsened
  by it, just still outstanding.
- `design/football-fixture-card-polish.md`'s five polish findings
  (spotlight/rail differentiation, crest alignment, `Scheduled` status
  icon, chip-row nested-scroll, `COMPONENT_LIBRARY.md` entries) are
  reflected as **already shipped** in the current `FixtureCard` source read
  for this brief (`isSpotlight`, `TextAlign.End`, `Schedule` icon, `FlowRow`
  chips, and library entries all present) — confirming that pass landed
  since its own brief was written, good to note for continuity across these
  documents.

## Ready for implementation

- **Brief**: `design/football-fixtures-list-redesign.md` (this file) — this
  is a retroactive review; the change is already merged/pushed as PR #9,
  branch `claude/fixture-spacing-carousel-x0s6qn`, commit `6760a60`. No
  further implementation work is required for the two fixes described
  above.
- **Visuals**: generated —
  `design/football-fixtures-list-redesign/mockups/home-football-section.png`
  (v2, current), image-to-image grounded directly in the real committed
  screenshot `docs/skyline-screenshots/home_david.png`, with the real
  5-item bottom nav correctly rendered. Supersedes an earlier v1 that was
  pure text-to-image and had an invented, incorrect nav bar — see "Visuals"
  and "Revision history" above for the full account of both attempts and
  why v2 was necessary. The Football section's own content still can't be
  captured live end-to-end — `HomeScreenshotTest.kt` deliberately excludes
  it (`BuildConfig.FOOTBALL_DATA_API_KEY` empty locally, and the test's
  `OkHttpClient` always throws even with a key), and this environment
  cannot resolve the Android Gradle Plugin to run/extend that test
  regardless (confirmed by running it) — so the Football section itself is
  still source-grounded (from `HomeScreen.kt`/`Components.kt`) composited
  into the real screenshot, not literally screen-captured end to end.
- **Summary**: Reviewed the already-shipped spacing fix and carousel→
  vertical-list redesign of David's "Football" round-fixtures section on
  Home against the design system and general UX practice; both changes are
  correct, well-grounded in existing patterns, and the new "This round"
  label fixes a genuine missing-context gap the old blank-titled carousel
  had. No blocking defects found.
- **Reuse/tokens**: Confirmed correct use of `SkySpacing.l`/`SkySpacing.m`,
  `SkyPalette.TextSecondary`, `MaterialTheme.typography.titleMedium`,
  `SkyRadius.card`; correct reuse of `FixtureCard`; correct, deliberate
  non-reuse of `Rail`/`ShimmerRail` at this call site since neither fits a
  vertical list's shape. No new components or tokens introduced.
- **Open questions**: none blocking — this PR needs no changes. Two
  low-priority, non-blocking follow-ups for whoever picks up backlog items
  in this area: (1) add `TextSecondary`/`TextMuted` on `Canvas`/`Surface`
  to `docs/SKY_DESIGN_SYSTEM.md`'s WCAG contrast table (currently only
  lists `TextPrimary`) — computed ≈7.55:1 here, passes AAA, just
  undocumented; (2) the kickoff-date-qualifier gap on the "Man Utd next"
  spotlight from `design/david-football-fixtures.md` is still open and
  unrelated to this PR.
