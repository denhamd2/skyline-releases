# Skyline full design review — August 2026

Branch reviewed: `claude/concise-responses-token-savings-o67xyn` (= main + an
XML comment fix). Every finding below cites a file that was read. **No app
code was changed.** There is no emulator here, so anything about visual
outcome is inferred from source unless it is backed by a committed golden
PNG, which is stated explicitly where it applies.

Mockup: `mockups/tv-home-corrected.png` — a text-to-image illustration of
the TV home screen with R1/R2/R4 applied (legible white rail headings, one
aligned left gutter, TV-scaled type, a focus ring that survives on the white
hero pill). It is an illustration of the target, not a pixel spec.

---

## 1. The design system as it actually is

### Tokens that exist (`ui/theme/Theme.kt`)

| Group | Members |
|---|---|
| `SkyPalette` | `Canvas` `Surface` `SurfaceElevated` `Accent` `AccentBright` `Brand` `Indigo` `LiveRed` `TextPrimary` `TextSecondary` `TextMuted` `Error` + brushes `heroScrim` `heroFallback` `tvBackground` `navUnderline` |
| `SkyRadius` | `chip` 8 · `card` 16 · `hero` 22 · `sheet` 28 |
| `SkySpacing` | `xs` 4 · `s` 8 · `m` 12 · `l` 16 · `xl` 24 · `gutter` 16 |
| Typography | 14 Manrope styles, `displayLarge` 44sp → `labelSmall` 11sp |

### Tokens that are missing

| Missing | Evidence | Cost of absence |
|---|---|---|
| **TV gutter** | `48.dp` literal ~20× in `tv/TvScreens.kt`; `tv/TvMainActivity.kt:304` uses `40.dp` for the same edge | Nav bar and page content are misaligned by 8dp on every TV screen |
| **TV type scale** | `SkylineTheme` is one `Typography`; `TvMainActivity.kt:96` applies it unchanged | Rail card titles are 14sp and subtitles 11sp at 10 feet |
| **TV focus radius** | `tvClickable` default shape is `RoundedCornerShape(12.dp)` (`TvComponents.kt:66`) — 12dp is not a `SkyRadius` value | TV cards are 12dp, phone cards 16dp, for the same content |
| **Rail item spacing** | `10.dp` phone (`Components.kt:116`), `14.dp` TV (`TvScreens.kt:175`) — neither on the 8pt grid, neither tokenised | Two rail rhythms, no single place to change them |
| **Motion tokens** | 150 / 140 / 200 / 220 / 340 / 1100 ms scattered as literals; `Motion.kt` exports only `enterReveal`/`revealDelay` | TV tab crossfade is 220ms (`TvMainActivity.kt:187`) vs the documented 200ms |
| **Default content colour** | Only one `Surface(` exists in the whole app (`DetailScreens.kt:370`) | See F1 — the single most severe finding |

### Enforced rule: clean, but with a hole

`grep -rn "Color(0x" --include="*.kt" .` outside `ui/theme/` returns **0**.
The machine-enforced rule genuinely holds.

It has a gap, though: it matches `Color(0x…)` constructor calls only, so
**named** constants pass. There are 25 of them, and `Color.White` is
byte-identical to `SkyPalette.TextPrimary`:

| Constant | Count | Worst offenders |
|---|---|---|
| `Color.White` | 19 | `PlayerScreen.kt` ×8, `TvScreens.kt` ×6, `TvMainActivity.kt` ×6 |
| `Color.Black` | 8 | `PlayerScreen.kt` ×3 |
| `Color.Red` | 1 | `MainActivity.kt:61` |

`Color.Transparent` (7×) is legitimate — there is no token for it.

### Unenforced drift, quantified

| Area | State | Numbers |
|---|---|---|
| Typography | **Clean** | 0 hardcoded `fontSize` anywhere |
| Spacing | **Heavily drifted** | ~470 raw `dp` literals vs 49 `SkySpacing` references; only 6 of 25 UI files reference `SkySpacing` at all |
| Off-grid values | **Widespread** | `6.dp` ×33, `14.dp` ×29, `10.dp` ×29, `2.dp` ×12, plus `22` `18` `30` `3` `26` `46` `34` `42` `50` |
| Component reuse | **Broken** | `filterChipColors(…)` copy-pasted **17×** across 8 files; `PillButton` used **1×** against 15 M3 `Button` sites; 9 hand-rolled back headers; 7 unstyled `OutlinedTextField`s |
| Colour by inheritance | **Systemic** | 107 of 232 `Text` composables (46%) omit `color` and rely on `LocalContentColor` |

### Documentation accuracy

`docs/SKY_DESIGN_SYSTEM.md` §"Color Contrast (WCAG AAA Compliance)" is
wrong. Recomputed per WCAG 2.x relative luminance:

| Pair | Documented | Actual | Verdict |
|---|---|---|---|
| TextPrimary on Canvas | 18.2:1 | 20.2:1 | understated |
| TextPrimary on Surface | 15.8:1 | 18.4:1 | understated |
| **Accent on Canvas** | **5.1:1 ✓** | **4.18:1** | **overstated, fails AA 4.5:1** |
| **LiveRed on Canvas** | **4.8:1 ✓** | **4.27:1** | **overstated** |

The line "All text colors meet WCAG AAA standards" is false: the table omits
`TextSecondary` and `TextMuted`, the two most-used secondary tiers, and
neither reaches AAA on `Surface` (6.90:1 and 5.13:1).

`docs/DESIGN_SYSTEM_ENFORCEMENT.md` §"Not implemented: visual regression
testing" is **stale**. It says CI runs `recordRoborazziDebug` and does not
compare. `.github/workflows/build-skyline-apk.yml:102-105` runs
`verifyRoborazziDebug` by default and only records on a `record_screenshots`
dispatch — a real gate, exactly as `docs/skyline-screenshots/README.md`
describes. Given that doc's own stated purpose ("a documented check that does
not run is worse than no check"), the inverse is worth fixing too.

---

## 2. Per-surface findings

### F1 — TV screen titles and rail headings render black on black *(TV, critical)*

`LocalContentColor` in Compose Material3 defaults to `Color.Black` and is
provided only by `Surface`. The phone path gets it: `SkylineNavHost.kt:149`
wraps everything in `Scaffold(containerColor = SkyPalette.Canvas)`, which is
a `Surface` internally, resolving content colour to `onBackground` = white.

**The TV path has no `Surface` anywhere.** `TvMainActivity.kt:96` is
`SkylineTheme { … TvRoot(container) }`, and `TvRoot` (line 178) is a bare
`Box`. So every `Text` on TV that omits `color` inherits `Color.Black`.

Affected, all with no `color` argument:
- `TvRailHeader` (`TvScreens.kt:409`) — **every rail heading on TV Home**
- 8 `headlineLarge` screen titles: `TvScreens.kt` lines 472, 659, 727, 757, 799, 860, 917 (TV guide, Browse, Films, TV shows, Sports, Kids, Settings) and 701
- `TvSettingTile` title (`TvScreens.kt:977`)

Direct evidence: `docs/skyline-screenshots/home_david.png`, a committed
golden. Its harness (`HomeScreenshotTest.kt:107-108`) wraps `HomeScreen` in
`SkylineTheme` with **no** Scaffold — the same structure as TV. In that PNG
the hero title, the Continue Watching title and the rail headings "David's
channels" and "Sky Sports Football" are near-black and effectively
invisible, while every `Text` with an explicit `SkyPalette.TextPrimary`
("Continue Watching", "Who's watching?", card labels) is white.

Why nobody has caught it: `tv_components.png` is component-level only and
contains no `TvRailHeader` or screen title, and there is no TV screen-level
golden at all.

Two bugs in one, and they should be fixed separately:
1. The TV surface has no content-colour provider.
2. `home_david.png` — the one screen the visual gate protects — is
   unrepresentative of the shipped screen, so the gate is verifying a
   rendering no user ever sees.

### F2 — TV typography is the phone scale, unchanged *(TV, high)*

`TvLandscapeCard` title `labelLarge` 14sp, subtitle `labelSmall` 11sp
(`TvComponents.kt:167,174`). `TvPosterCard` title `labelMedium` 12sp (:211).
`TvGuideScreen` programme titles `labelLarge` 14sp (:604), channel numbers
`labelMedium` 12sp (:561). `TvPlayerOverlay`'s only D-pad discoverability
hint — "CH ▲▼ to zap · OK to pause" — is `labelSmall` 11sp
(`TvMainActivity.kt:499`).

11–14sp is a phone-at-arm's-length scale. Visible in `tv_components.png`:
the card title occupies roughly 1% of frame height in a 2560px-wide capture.

### F3 — TV focus is under-specified *(TV, high)*

- **No initial focus, anywhere.** The only `FocusRequester` in the TV
  surface is in `TvPlayerOverlay` (`TvMainActivity.kt:345`). No screen
  requests focus on entry, and tab switches go through a `Crossfade`
  (:185) that destroys the focused subtree — focus falls back to the root
  and the next D-pad press is spent recovering it.
- **No `focusRestorer()` on any rail.** Leaving a rail and coming back
  restarts at card 1 rather than where you left.
- **The hero CTA's focus ring is invisible.** `TvScreens.kt:154-158` is a
  `tvClickable` **white** pill; `tvClickable` draws a **white** 3dp border
  on focus (`TvComponents.kt:87`). White on white.
- **The TV nav bar has the weakest focus state on screen.** `TvNavTab`
  (`TvComponents.kt:222`) uses plain `.clickable()`, not `tvClickable` — no
  1.04 scale, no white outline, just a `SurfaceElevated` fill. Its active
  underline is 34×3dp (:243), roughly 4mm on a 55" panel.
- **The TV guide's time controls are unstyled M3 `Button`s**
  (`TvScreens.kt:476-486`) with no TV focus treatment, weaker than the cards
  around them. `"◀ Earlier"` / `"Later ▶"` use text glyphs, not icons.
- **Dead focus stop:** `TvSettingTile("Account", …) {}` (`TvScreens.kt:922`)
  is focusable and does nothing.
- **Zero-width focus targets:** guide programme cells are sized
  `(dpPerMin * wMin - 4.dp).coerceAtLeast(0.dp)` (`TvScreens.kt:591`), so a
  short programme becomes a sliver — or a 0dp-wide — focusable cell.

### F4 — TV/phone feature divergence *(TV, medium)*

`TvHomeScreen` reads `selectedMember` and gates the Football and YouTube
sections on it (`TvScreens.kt:279, 378`), but **there is no "Who's
watching?" control on TV**. The ViewModel defaults to `"David"`
(`HomeScreen.kt:160`), so on a television Anne, Ava and Sophie can never
reach their rails and the Football section is permanently on.

Similarly, `TvGuideScreen` reads `state.selectedDay` (:457) but renders no
day selector, so the TV guide is locked to one day.

### F5 — Phone rail/header inconsistency *(phone, medium)*

`Rail` (`Components.kt:104`) renders its own title, while `SectionHeader`
(:248) renders a title plus an optional "View all". `HomeScreen` mixes both:
`Rail("Favourites", …)` at :723 gets no "View all", but `SectionHeader("Live
TV", onViewAll)` + `Rail("", …)` at :732 does — visually near-identical
headings with different affordances and two code paths. `Rail`'s title also
omits `color` (:105-108), which is what makes it black in F1.

### F6 — Phone Home motion is applied inconsistently *(phone, low)*

`Modifier.enterReveal` is on the pinned-channel, Football, YouTube and
category rails (`HomeScreen.kt:788, 816, 932, 960`) but **not** on the hero,
Continue Watching, Live Now, Favourites, Live TV, New films or New series.
The staggered reveal therefore only happens once a family member is
selected; the default page has no entrance motion at all.

### F7 — The Live screen duplicates its first item and has no empty/loading state *(phone, medium)*

`FeaturedLiveCard` renders `channels.peek(0)` (`LiveScreen.kt:241`) and the
"Up Next" list below starts at index 0 (:257) — the top channel always
appears twice.

Paging `loadState` is never consulted, so during load the user sees chips, a
bare "Up Next" heading and nothing else. Same for a category with no
channels or a favourites filter with none. `HomeScreen` has `ShimmerRail`;
`LiveScreen` has nothing.

Two more on the same screen:
- `"❤ Favorites"` (:211) is an emoji glyph in a label — renders in the
  platform emoji font rather than Manrope, and TalkBack announces "red
  heart". The same screen uses `Icons.Default.Star` for the same concept
  at :427.
- `ChannelRow` (:372) uses plain `.clickable()` while every other tappable
  row in the app uses `scaledClickable` — different press feedback for the
  same interaction.

### F8 — Login is the least designed screen in the app *(phone, medium)*

`ui/login/LoginScreen.kt`:
- Three `OutlinedTextField`s (:119, 128, 136) with **no** `SkyPalette`
  binding — pure M3 defaults over the indigo `heroFallback`.
- **No show-password toggle** (:141). Users type a long provider password
  into an obscured field with no way to verify it, and the failure message
  is generic.
- No autofill hints, no IME next/done actions.
- Off-token throughout: `28.dp` gutter, `36.dp`, `14.dp`, `52.dp`, `22.dp`.
- No `enterReveal`. The first screen a user ever sees has zero motion.

### F9 — Destructive actions have no confirmation *(both, medium)*

- `DownloadsScreen.kt:276` — delete a download, one tap, irreversible.
- `TvScreens.kt:937` — "Sign out" is a plain tile that signs out
  immediately.

### F10 — Misleading affordance in Downloads *(phone, low)*

`DownloadRowItem` (`DownloadsScreen.kt:217`) applies `scaledClickable` to
the whole row, but `onClick` only does anything when
`state == STATE_COMPLETED` (:187). A queued or downloading item gives full
press feedback and does nothing.

### F11 — Nine hand-rolled back headers *(phone, medium)*

No `TopAppBar` anywhere. Nine screens rebuild the same row, with four
different type styles and three different paddings:

| Screen | Style | Padding | Icon |
|---|---|---|---|
| Downloads :138, My List :67, Account :71, Settings :125, Recordings :131 | `headlineMedium` 24sp | `8.dp` | AutoMirrored |
| YouTube channels :76, Customize categories :98 | `headlineSmall` 20sp | `8.dp` / `16.dp` | **`Icons.Default`** |
| Football Fixtures :66 | `titleLarge` 20sp | `SkySpacing.m` | **`Icons.Default`** |
| Movie/Series detail :439 | `headlineLarge` 28sp | `8.dp` | AutoMirrored |

Four screens use the deprecated non-mirrored `Icons.Default.ArrowBack`,
which points the wrong way under RTL: `FixturesScreen.kt:59`,
`YouTubeSubscriptionScreen.kt:72`, `CategoryCustomizationScreen.kt:94`,
`TvFixturesScreen.kt:61`.

### F12 — Hero top bar has no scrim *(phone, low)*

`HomeScreen.kt:634-655` puts the wordmark, search and account icons over raw
hero artwork at `TextPrimary.copy(alpha = 0.85f)`. `SkyPalette.heroScrim` is
a *bottom* gradient (transparent → Canvas), so the top of the hero is
unprotected. Over a bright film backdrop those controls lose contrast.
Inferred from source — not observed, since the golden's artwork failed to
load.

### F13 — Suspected double status-bar inset on Home *(phone, low, needs device check)*

`SkylineNavHost.kt:186` applies the Scaffold's `padding` (which includes
system bars) to the `NavHost`, and `HomeScreen.kt:638` then applies
`statusBarsPadding()` again to the hero's top row. That reads as a
double inset pushing the wordmark row down. Cannot be confirmed without a
device.

---

## 3. Cross-cutting

### Accessibility

**Contrast** (computed, WCAG 2.x):

| Pair | Ratio | Verdict | Where |
|---|---|---|---|
| `TextMuted` on `SurfaceElevated` | **4.37:1** | **fails AA** | spotlight `FixtureCard` — "Not on your channels", the "v" separator, "FT" |
| `TextSecondary.copy(alpha=0.7f)` on `Canvas` | **4.12:1** | **fails AA**, at 11sp | `LiveScreen.kt:419` "Next: …" |
| `Accent` on `Canvas` | **4.18:1** | **fails AA** | selected bottom-nav label, 11sp (`SkylineNavHost.kt:171-172`) |
| `TextSecondary` on `Canvas` | 7.55:1 | AAA | — |
| `TextSecondary` on `Surface` | 6.90:1 | AA only | — |
| `TextMuted` on `Surface` | 5.13:1 | AA only | — |
| `TextPrimary` on `Accent` | 4.82:1 | AA only | guide "now" cell, `TvScreens.kt:597-605` |

The bottom-nav result is the perverse one: the **selected** item (4.18:1) is
lower contrast than the **unselected** item (7.55:1). Selection currently
reads as "gets dimmer".

`LiveScreen.kt:419` also invents a fourth text tier via
`TextSecondary.copy(alpha = 0.7f)` when `SkyPalette.TextMuted` already
exists for exactly that role — and lands below AA doing it.

**Touch targets:** generally fine — `IconButton` is 48dp by default and is
used for all icon actions. The exceptions are text-only tap targets:
`SectionHeader`'s "View all" is a bare `Text` with `.padding(4.dp)`
(`Components.kt:270`), and `HomeScreen.kt:890-892` repeats the same pattern
— roughly 20dp tall against a 48dp minimum.

**Content descriptions:** good. 41 explicit `contentDescription`s, only 11
deliberate nulls (all decorative). `ArtworkImage` threads it through
properly. `LiveScreen`'s favourite toggle even varies it by state (:428).
This is the strongest accessibility area.

**Focus order:** see F3. Nothing sets `focusGroup`, `focusProperties` or
`focusRestorer` anywhere in the app.

### Empty / loading / error states

There is no shared pattern. Empty states are bare centred `Text`, never with
an icon or a CTA:

| Screen | Empty state |
|---|---|
| Downloads :175, My List :83, Search :287, Recordings :182, YouTube subs :162, Fixtures :82, TV Fixtures :82, TV Sports :804, TV Kids :865, TV guide cell :571 | plain `Text`, no icon, no action |
| **Live** | **none at all** |
| **Films / Series grids (phone + TV)** | **none at all** |
| **Guide** | **none at all** |

Loading: `ShimmerRail`/`ShimmerBox` exist and are used on Home only. Every
paged screen (Live, Films, Series, TV Films, TV Series) ignores `loadState`.

Errors: three separate inline "Try again" `Button`s (`HomeScreen.kt:709`,
`PlayerScreen.kt:465`, `TvMainActivity.kt:437`), each styled differently.

### Component duplication, ranked by cost

1. **Filter chips — 17 call sites, 8 files.** `.claude/rules/design-system.md`
   names this exact pattern as the one to match; it is matched by
   copy-paste. `SearchScreen.kt` alone repeats the identical 6-line
   `filterChipColors(…)` block six times.
2. **Buttons.** `PillButton` — the design system's own named component — is
   used **once** (`HomeScreen.kt:1028`). There are 15 M3 `Button` sites,
   including three separate inline rebuilds of a play-glyph + label pill
   (`HomeScreen.kt:1089`, `LiveScreen.kt:331`, `TvScreens.kt:154`).
3. **Back headers — 9 hand-rolled copies.** See F11.
4. **Text fields — 7 sites, none themed.**
5. **`FixtureCard` / `TvFixtureCard`** — ~150 lines duplicated between
   `Components.kt:416` and `TvComponents.kt:266`. This one is *documented*
   as a deliberate sibling and is defensible; the status/score/chip body is
   still identical and could be shared.

### Motion

`Motion.kt` is well-built and barely used. Enter reveal on 4 of ~11 Home
sections (F6) and nothing on any other screen. Screen transitions are
correct on phone (`SkylineNavHost.kt:187-190`, 200ms crossfade) and 220ms on
TV (`TvMainActivity.kt:187`). TV focus scale/outline matches the documented
1.04 / 140ms (`TvComponents.kt:74-89`), where `tvClickable` is used at all.

---

## 4. Ranked recommendations

Ranked by impact per unit of effort. **Do R1–R3 first** — together they are
roughly a day and a half and fix the only findings that are plausibly
invisible-content bugs on a shipping surface.

### R1. Provide content colour on the TV surface, and fix the golden *(S — hours)*

*Problem:* F1. Every TV rail heading and all 8 screen titles inherit
`Color.Black` on a near-black canvas.

*Change:* wrap `TvRoot` in a `Surface(color = …, contentColor =
SkyPalette.TextPrimary)` or provide `LocalContentColor` at the `SkylineTheme`
level so no caller can forget it. Separately, wrap `HomeScreen` in
`HomeScreenshotTest.kt` in the same Scaffold the app uses, and re-record
`home_david.png`.

*Payoff:* TV screens stop having invisible headings. The one gated golden
starts representing the shipped screen. Fixing it inside `SkylineTheme` also
neutralises the other 107 uncoloured `Text`s permanently.

### R2. Add a TV type ramp *(S/M — half a day)*

*Problem:* F2. Rail titles 14sp, subtitles 11sp, D-pad hints 11sp, at 10 feet.

*Change:* add a `SkyTvTypography` in `Theme.kt` and a `SkylineTvTheme`
wrapper that swaps it in for `TvMainActivity`, roughly: rail heading
`headlineMedium`, card title 18sp, card subtitle 16sp, screen title
`displayMedium`. Same family, same tracking — a ramp, not new styles.

*Payoff:* the TV surface becomes readable from a sofa. Largest single
usability gain available on TV.

### R3. Make TV focus survivable *(M — one day)*

*Problem:* F3, all six sub-findings.

*Change, in priority order:*
1. Give `tvClickable` a `focusRingColor` parameter and pass `Accent` for
   the white hero pill (`TvScreens.kt:156`).
2. `FocusRequester` on the first content item of each TV screen; request it
   on entry and after the `Crossfade` settles.
3. `Modifier.focusRestorer()` on every TV `LazyRow`/`LazyVerticalGrid`.
4. Route `TvNavTab` through `tvClickable`; grow the active underline.
5. Replace the three M3 `Button`s in `TvGuideScreen` with `tvClickable`
   pills and real icons.
6. Give `TvSettingTile("Account", …)` an action or make it non-focusable;
   floor guide-cell width at a focusable minimum.

*Payoff:* the remote stops feeling broken. Items 1 and 2 alone are most of
the value.

### R4. Tokenise the TV gutter and align the nav bar *(XS — under an hour)*

*Problem:* `48.dp` ×20 in screens, `40.dp` in the top bar — an 8dp
misalignment on every TV screen, plus 20 magic numbers.

*Change:* add `SkySpacing.tvGutter = 48.dp`; replace all 21 sites.

*Payoff:* the wordmark and every rail heading finally share a left edge.
Best effort-to-payoff ratio in this document; visible in the mockup.

### R5. Extract `SkyFilterChip`, and actually use `PillButton` *(S — half a day)*

*Problem:* 17 chip call sites; `PillButton` used once against 15 `Button`s.

*Change:* add `SkyFilterChip(label, selected, onClick)` to
`ui/components/`; replace all 17. Replace the three inline play-pills
(`HomeScreen.kt:1089`, `LiveScreen.kt:331`, `TvScreens.kt:154` — the last
needs a TV variant) with `PillButton`.

*Payoff:* chip restyling becomes one edit instead of 17. Removes the largest
class of copy-paste in the codebase.

### R6. Fix the three sub-AA contrast pairs *(XS — under an hour)*

*Problem:* `TextMuted` on `SurfaceElevated` 4.37:1; `TextSecondary@70%` on
`Canvas` 4.12:1 at 11sp; `Accent` on `Canvas` 4.18:1 for the selected nav
label.

*Change:* replace `TextSecondary.copy(alpha = 0.7f)` at `LiveScreen.kt:419`
with `SkyPalette.TextMuted`. For the nav, use `AccentBright` for the
selected label/icon, or keep `Accent` for the icon and `TextPrimary` for the
label. Lift `TextMuted` on `SurfaceElevated` to `TextSecondary`. Then correct
the contrast table in `docs/SKY_DESIGN_SYSTEM.md` with the real numbers and
drop the false "All text colors meet WCAG AAA" claim.

*Payoff:* selection stops reading as "dimmer". Cheap, and the doc stops
asserting something untrue.

### R7. Add a `SkyScreenHeader` component *(S — half a day)*

*Problem:* F11 — 9 copies, 4 type styles, 3 paddings, 4 RTL-broken icons.

*Change:* one `SkyScreenHeader(title, onBack, subtitle = null, actions = {})`
on `headlineMedium` + `SkySpacing.s`, always `Icons.AutoMirrored`. Replace
all 9.

*Payoff:* screen titles stop changing size as you navigate; RTL fixed by
construction.

### R8. Give Live a loading and empty state, and stop duplicating item 1 *(S — half a day)*

*Problem:* F7. Blank screen during load and on empty categories; the top
channel renders twice.

*Change:* consult `channels.loadState.refresh` → `ShimmerRail`; add an empty
state; drop index 0 from the "Up Next" list, or drop `FeaturedLiveCard`
(it is arbitrary — just `peek(0)`). Also swap `"❤ Favorites"` for
`Icons.Default.Star` and move `ChannelRow` onto `scaledClickable`.

*Payoff:* Live is the second most-used tab and currently looks broken while
loading.

### R9. Rework Login *(S/M — half a day)*

*Problem:* F8.

*Change:* bind the three `OutlinedTextField`s to `SkyPalette` (a
`SkyTextField` wrapper covers all 7 sites in the app); add a show/hide
password toggle; add autofill hints and IME actions; move `28/36/14/52` onto
`SkySpacing`; add `enterReveal`.

*Payoff:* the show-password toggle alone removes the most likely first-run
failure, on the only screen every user must clear.

### R10. Standardise empty / error states *(M — one day)*

*Problem:* 10 bespoke empty states, 3 bespoke error blocks, 3 screens with
none.

*Change:* one `SkyEmptyState(icon, title, body, action = null)` and one
`SkyErrorState(message, onRetry)`; apply across the 13 sites, and add them
to Live, Films, Series and Guide.

*Payoff:* recovery paths stop being dead ends.

### R11. Confirmations on destructive actions *(XS)*

F9 — download delete and TV sign-out. An `AlertDialog` on each.

### R12. Add the TV "Who's watching?" control *(M)*

F4. Without it, three of four family members cannot reach their own content
on TV, and the Football section can never be turned off. Also add the guide
day selector. Worth an OpenSpec proposal — this is behaviour, not styling,
and `openspec/specs/home-personalization/spec.md` governs it.

### R13. Pay down spacing drift *(L — incremental)*

~470 raw `dp` against 49 `SkySpacing` refs. Do **not** do this as one sweep;
fold it into R1–R10 file by file. Start by normalising the off-grid
repeat-offenders: `6` → `SkySpacing.s`, `10`/`14` → `SkySpacing.m`,
`2`/`3` → `SkySpacing.xs`.

### R14. Extend `SkyPaletteUsage` to named colour constants *(S)*

The enforced rule misses 25 named constants, 19 of them `Color.White` =
`SkyPalette.TextPrimary`. Add `Color.White`/`Color.Black`/`Color.Red` to the
rule (allowing `Color.Transparent`, or adding a token for it). Per
`DESIGN_SYSTEM_ENFORCEMENT.md` the ruleset is live and extending it is a
known three-step process.

### R15. Motion consistency *(XS)*

F6 — apply `enterReveal`/`revealDelay` to the default Home page sections, not
just the personalised ones. Align the TV crossfade to 200ms
(`TvMainActivity.kt:187`).

---

## 5. What was not reviewed

- Any rendered output other than the three committed goldens. There is no
  emulator and CI is the only compiler.
- `PlayerScreen`'s landscape/PiP path beyond reading source — it is
  explicitly excluded from Roborazzi coverage.
- `reference-designs/*.png` are treated as a stale baseline per that
  folder's own README; divergences from them were not scored as defects.
