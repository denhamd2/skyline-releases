## Context

Football fixtures ship as three pieces of UI, all phone-only:
1. A Home-screen "Football" section (Man Utd spotlight + a vertically-
   stacked list of the upcoming Premier League round), gated on
   `selectedMember == "David"`. The round list was itself a horizontal
   `Rail` carousel until PR #9 (`claude/fixture-spacing-carousel-x0s6qn`)
   converted it to a vertical list for the same content-density reason
   this change applies to TV -- see "Decisions" below.
2. A "View all" full-screen vertical list (`FixturesScreen.kt`), reached
   from the Home rail.
3. `FixtureCard`/`FixtureChannelChip` (`ui/components/Components.kt`), the
   shared card powering both.

TV has none of this. `TvHomeScreen` has no Football section, and TV has no
"view all" screen. This is the deferred "phase 2" every prior fixtures
design brief flagged but never scheduled.

## Goals / Non-Goals

**Goals:**
- TV gets the same three pieces phone has: Home section, "view all" list,
  and a fixture card, with the same data (reusing
  `HomeViewModel.footballSection`/`fixtureChannels` as-is).
- The TV "view all" list is a **vertical** `LazyColumn`, matching phone
  exactly -- not a horizontal rail extension and not a multi-column grid.
- Every interactive element (card, chip) is genuinely D-pad-focusable, not
  just visually present -- TV has no touch fallback.

**Non-Goals:**
- No changes to fixture data, EPG channel-matching, or round selection --
  all inherited from the existing `football-fixtures` capability as-is.
- No changes to the phone `FixtureCard`/`FixturesScreen.kt`.
- No TV D-pad remote testing in this environment (no device/emulator here;
  flagged for CI/QA).

## Decisions

**New `TvFixtureCard`, not a shared/conditional `FixtureCard`.**
Every other content type on TV already has its own card
(`TvLandscapeCard`, `TvPosterCard`) rather than a phone card branching on
platform. Following that established pattern keeps `FixtureCard` simple
(phone-only, as its own doc comment already says) and keeps TV focus
logic (`Modifier.tvClickable`) out of a component phone doesn't need it in.

**Every channel chip is individually focusable on TV, unconditionally --
not gated by channel count.**
Phone's tap-disambiguation rule (whole card clickable only when exactly one
channel matches; chips only when 2+) exists because a touch card can't
otherwise tell "tap the card" from "tap a chip" apart when both would do
something. A D-pad has no such ambiguity -- each element is discretely
focused one at a time -- so `TvFixtureCard` makes every chip
`tvClickable` and reserves the card's own focus/click for a sensible
default action (e.g. the single-match case), rather than porting the
touch-specific single/multi split verbatim.

**TV "view all" list is vertical, resolving the open TV-layout question
every prior brief left as "TBD."**
Considered a 2-column grid (the other option those briefs floated) --
rejected because it's the one part of this feature area with an explicit
product decision already made (vertical, matching phone) rather than an
open design question; a grid would also complicate D-pad focus traversal
(up/down AND left/right within the list) for no stated benefit over a
single-column vertical scroll, which every other full-list surface on TV
already avoids.

**TV Home's round section is also a vertical list, not a horizontal rail --
matching phone Home, not TV's other rails.**
Every other TV Home rail (top picks, favourites, genre rails) is a
horizontal `LazyRow`, and this change's own earlier draft assumed the
Football round section should follow that convention. Revised once PR #9
(`claude/fixture-spacing-carousel-x0s6qn`) was found already applying the
opposite conclusion to phone: a fixture card carries far more information
per card (competition badge, two crests, two names, a status row, a row of
channel chips) than a channel-logo rail card, so a horizontal carousel
undersells it the same way it would on TV -- narrower cards, more
truncation, less room for the channel-chip row before wrapping. Consistency
with phone (and with this feature's own dedicated vertical "view all"
list) outweighs consistency with TV's other, lower-density rails here.

**Navigate via local `Boolean` state in `TvRoot`, not a new route.**
TV has no `NavHost`; `TvMainActivity.kt`'s `TvRoot` already handles a
full-screen destination outside its tab `Crossfade` this way (`playing:
Boolean` overlaying `TvPlayerOverlay`). Reusing that shape for
`showFixtures` is more consistent than introducing routing machinery TV
doesn't otherwise use.

## Risks / Trade-offs

- [Risk] D-pad focus order across "Man Utd spotlight -> rail -> View all"
  and within the vertical list (each card's internal chip row) needs to
  feel natural, and can't be verified without a device.
  -> Mitigation: follow Compose's default focus traversal (declaration
  order) exactly as every existing TV screen already does, rather than
  inventing custom focus-order logic; flagged for CI/QA sanity check once
  built.
- [Risk] Reusing `HomeViewModel` state directly in `TvHomeScreen` (as every
  other TV rail already does) ties TV's Football section to the same
  `selectedMember` state phone sets -- if a user has never opened the phone
  app to pick "David", TV won't show the section either.
  -> Accepted: this is existing, unchanged behaviour (TV's YouTube carousel
  has the identical dependency on `selectedMember` today), not a new
  limitation introduced here.
