## 1. Per-member pinned channel storage

- [x] 1.1 Add `setPinnedChannels(accountId, userName, streamIds: List<Int>)`
      and `getPinnedChannels(accountId, userName)` to
      `data/prefs/CategoryPreferencesStore.kt`, mirroring the existing
      `setUserCategories`/`getUserCategories` accessors. Reads must go through
      `loadPreferences` (which uses `first()`), never `stateIn(...).value` —
      that returns a placeholder and silently discards saved selections.
- [x] 1.2 Add `pinnedChannels` to the `CategoryPreferences` model, defaulting
      to empty so existing stored preferences deserialise unchanged
      (`Json { ignoreUnknownKeys = true }` is already configured).
- [x] 1.3 Add `byIds(ids: List<Int>): List<ChannelEntity>` to `ChannelDao`,
      copying the `WHERE streamId IN (:ids)` form used by `EpgDao`.
- [x] 1.4 Unit-test the store round-trip: save pins for one member, read them
      back, confirm a second member's pins are unaffected and that an empty
      list is returned for a member who has none.

## 2. Home filters to the selected member

- [x] 2.1 In `ui/home/HomeScreen.kt`, wrap the Live Now, Favourites, Live TV,
      New films and New series rails in `if (selectedMember == null)`. Leave
      Continue Watching outside the guard so it shows in both modes.
- [x] 2.2 Load the selected member's pinned channels in the ViewModel via
      `getPinnedChannels` + `channelDao().byIds`, and render them as a rail
      above the category rails, using the existing `Rail` + `ChannelCard`
      components rather than a new one.
- [x] 2.3 Confirm the sync status / shimmer / `SyncState.Failed` block still
      renders in both modes — it reports first-run progress and errors, and
      hiding it behind member selection would make a failed sync invisible.
- [x] 2.4 Confirm deselecting restores every generic rail (the existing
      "Deselecting" scenario must still hold).

## 3. Pinning UI in the per-member editor

- [x] 3.1 Add a channel section to `ui/settings/CategoryCustomizationScreen.kt`:
      search field backed by `channelDao().search`, tick-able results, and the
      member's current pins listed and removable.
- [x] 3.2 Wire pins into the existing save action so categories and pins are
      saved together for the member being edited.
- [x] 3.3 Confirm switching member mid-edit loads that member's pins, matching
      how categories already behave.

## 4. Tests

- [x] 4.1 Test that a pinned id absent from the catalogue is omitted rather
      than rendering a broken entry — `byIds` drops unknown ids naturally, so
      assert it rather than assume it.
      *`data/db/PinnedChannelQueryTest.kt`, 5 tests against an in-memory
      `SkylineDatabase`. First Room-backed test in the repo, so it also
      establishes the fixture pattern the test strategy calls for. Uses
      Robolectric's `RuntimeEnvironment.getApplication()` rather than
      androidx.test's `ApplicationProvider`, which is not a declared
      dependency here.*
- [ ] 4.2 Add a click/behaviour test that selecting a member hides the generic
      rails and deselecting restores them, following the existing
      `ClickBehaviourTest` pattern.
      **BLOCKED — not attempted, and deliberately not ticked.** `HomeScreen`
      takes a `HomeViewModel`, which needs a real `AppContainer` (Room,
      DataStore, Android Keystore, OkHttp), so it cannot be composed in a
      JVM test as it stands. `ClickBehaviourTest` only exercises individual
      components for this reason. Doing it properly needs the stateless
      `HomeScreenContent` extraction that is task 6.1 of
      `screenshot-regression-testing`; this test should be written there,
      against that seam, rather than bodged here. The rail-visibility logic
      shipped in this change is therefore covered by the spec scenarios and
      on-device confirmation only — stated plainly rather than implied by a
      ticked box.

## 5. Ship and verify

- [x] 5.1 Push and confirm the CI run is green **and** that the APK build and
      both publish steps ran rather than being skipped.
      *Already pushed — `0976b6f` is the tip code commit (the later
      `6da13dd` only touches this file). CI run 31413083853 on
      `claude/sky-iptv-android-player-s7h6kb` completed `success`: "Build
      debug APK", "Publish to skyline-latest release", "Publish APK to
      public releases repo" and "Verify UI screenshots" all succeeded (not
      skipped). Opened as PR #15 against `main`.*
- [x] 5.2 Ask the user to confirm on device: David's tab shows MUTV, the EPL
      category and his YouTube, and nothing else.
      *User confirmed on device.*
- [ ] 5.3 Rebase against `screenshot-regression-testing` if that change has
      landed first — its task 6.1 extracts a stateless `HomeScreenContent`
      from the same file.
      *Not yet applicable: `screenshot-regression-testing` is still on
      group 1 of 9 (not archived), so nothing to rebase against yet.*
