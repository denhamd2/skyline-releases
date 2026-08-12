## Context

See proposal.md - Why. Relevant existing structure, verified by reading the
code:

- `ui/home/HomeScreen.kt` renders Continue Watching (~487), Live Now (~495),
  Favourites (~535), Live TV (~544), New films (~554), New series (~560), then
  the member selector (~568), then the YouTube rail (~599, already gated on
  `selectedMember != null`) and category rails (~627). Everything before the
  selector is unconditional.
- `data/prefs/CategoryPreferencesStore.kt` already stores per-member lists in
  DataStore with a settled shape: `setUserCategories`/`getUserCategories` and
  `setYoutubeSubscriptions`/`getYoutubeSubscriptions`, each keyed
  `(accountId, userName)`, plus add/remove helpers for subscriptions.
- `ChannelDao` has `search(query)` (FTS, LIMIT 100) and `byId(id)`, but **no**
  bulk `byIds`. `EpgDao` has the `WHERE streamId IN (:ids)` pattern to copy.
- `ui/settings/CategoryCustomizationScreen.kt` already switches member (~90)
  and saves (~140).

## Goals / Non-Goals

**Goals:**
- Member selection filters the page rather than extending it.
- One channel can reach one member's tab without its category.
- Reuse the per-member storage and editor that already exist.

**Non-Goals:**
- Changing the default (no member selected) page at all.
- Touching the global `favorites` table or the Favourites rail.
- De-duplicating the hardcoded `["David","Anne","Ava","Sophie"]` list, which
  appears in `HomeScreen.kt` (~576), `CategoryCustomizationScreen.kt` (~90)
  and `YouTubeSubscriptionScreen.kt` (~86). Worth fixing, but not here — this
  change must not add a fourth copy either.

## Decisions

### Filtering by wrapping, not by deleting

The six generic rail `item {}` blocks stay where they are, wrapped in
`if (selectedMember == null)`. Continue Watching stays outside that guard, so
it shows in both modes.

Alternative considered: split Home into two separate composables, one per
mode. Rejected — it duplicates every rail definition and doubles the surface
that the in-flight `screenshot-regression-testing` change has to extract.

### Pinned channels live in DataStore beside categories

Add `setPinnedChannels(accountId, userName, streamIds)` /
`getPinnedChannels(accountId, userName)` to `CategoryPreferencesStore`,
mirroring the existing category and subscription accessors exactly, storing
`Int` stream ids.

**Invariant to preserve:** reads must go through the existing
`loadPreferences`, which uses `observePreferences(...).first()`. An earlier
implementation used `stateIn(...).value`, which returns the placeholder
because DataStore emits asynchronously — every read came back empty and every
write rebuilt from an empty base, silently discarding saved selections. Any
new accessor that reaches for `.value` reintroduces that bug.

Alternative considered: a Room table for pins. Rejected — pins are small,
per-member preference data, exactly what the DataStore already holds, and a
new table would mean a migration (v7→v8) on a database whose migrations have
no test coverage and `fallbackToDestructiveMigration()` enabled. Not a risk
worth taking for a list of ids.

### Resolving pins to channels needs a new DAO query

Add `byIds(ids: List<Int>): List<ChannelEntity>` to `ChannelDao`, copying the
`WHERE streamId IN (:ids)` form already used by `EpgDao`. Ids no longer in the
catalogue simply do not come back, which satisfies the "pinned channel is no
longer in the catalogue" scenario without extra handling — worth an explicit
test rather than relying on it by accident.

### Pinning UI reuses the per-member editor

`CategoryCustomizationScreen` gains a channel section: a search field backed
by `channelDao().search`, results as tick-able rows, current pins listed and
removable. It already has member switching and a save action, so pins ride the
same flow.

Search is required, not optional: a provider carries thousands of channels and
`search` is already FTS-backed and capped at 100 results.

Long-press-to-pin on a `ChannelCard` is a reasonable second affordance later
(pin to the currently selected member), but it is not built here — a gesture
with no visible discoverability should not be the only route to a feature.

## Risks / Trade-offs

- **Hiding rails is destructive-feeling if a member has nothing configured** →
  A member with no pins, no categories and no subscriptions would see a nearly
  empty page. Keep the existing keyword-default behaviour for categories, so
  an unconfigured member still gets category rails; only a member configured
  to have nothing sees a sparse page, which is then their choice.
- **File overlap with `screenshot-regression-testing`** → That change's task
  6.1 extracts a stateless `HomeScreenContent` from this same file. Whichever
  lands second rebases. This change is smaller and user-facing; it should land
  first, and the extraction then picks up the conditional structure as-is.
- **Pins are ids, and ids are provider-assigned** → If the provider renumbers
  streams, pins silently drop (see the DAO decision). Acceptable: the same is
  already true of the global favourites table, which stores `refId`.

## Migration Plan

None required — no Room schema change, and DataStore keys are additive. A
member with no stored pins reads an empty list and the rail is simply absent,
so existing installs behave exactly as before until someone pins something.
