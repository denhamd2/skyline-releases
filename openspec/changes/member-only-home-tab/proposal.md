## Why

Picking a family member on Home does not give that member a home page — it
appends two rails to the bottom of everyone's. Selecting "David" leaves
Live Now, Favourites, Live TV, New films and New series above the selector
untouched, so his own content sits below six rails he did not ask for.

Reported directly: *"for david I just want MUTV, the EPL channel category and
my YouTube channels to be in my tab, not all the other things there."*

That request also exposes a second gap. Personalisation is expressed only as
*categories*, and MUTV is a single channel. Selecting whatever category MUTV
belongs to drags in every other channel in it, so "just this one channel"
cannot be said at all today.

## What Changes

- **Selecting a member filters the page instead of extending it.** While a
  member is selected, Home shows only Continue Watching, that member's pinned
  channels, their category rails, and their YouTube rail. Live Now,
  Favourites, Live TV, New films and New series are hidden.
- **With no member selected, Home is unchanged** — every generic rail behaves
  exactly as it does today. This is a member-selected mode, not a redesign of
  the default page.
- **New: channels can be pinned per member.** A member's tab can include named
  individual channels independent of category selection, so one channel can
  appear without its category.
- Pinned channels are chosen in the existing per-member customisation screen,
  which already switches member and saves; it gains channel selection
  alongside category selection.

Deliberately unchanged: the global `favorites` table and the Favourites rail.
Pinning is per-member and additive to one member's tab; favouriting is global
and shared. They are different concepts and are kept separate.

## Capabilities

### New Capabilities

(none — this changes how an existing capability behaves)

### Modified Capabilities

- `home-personalization`: the member selector requirement changes from "the
  category rails below change" to "the page shows only that member's content";
  a new requirement covers per-member pinned channels and how they combine
  with category selection.

## Impact

- `ui/home/HomeScreen.kt` — the six unconditional rails become conditional on
  no member being selected; a pinned-channels rail is added.
- `ui/settings/CategoryCustomizationScreen.kt` — gains channel selection.
  Needs search, since a provider carries thousands of channels.
- `data/prefs/CategoryPreferencesStore.kt` — per-member pinned channel ids,
  following the same DataStore pattern already used for categories.
- No schema migration: pinned ids live in DataStore, not Room.

**Not verifiable in the agent environment:** there is no emulator or device
and the Android Gradle Plugin cannot be resolved, so none of this can be
compiled or run here. CI can confirm it compiles, that tests pass, and that
the APK builds and publishes. Whether the tab actually reads as "just my
stuff" on a phone — and whether MUTV and the EPL category are findable under
this provider's real category names — can only be confirmed on device by the
user. The provider's category list is not available here.
