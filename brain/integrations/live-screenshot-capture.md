# Capturing a live screenshot for an existing-screen edit

How `ux-design` grounds an edit to an **existing** screen (new section,
updated component, restyle, bug-driven UI fix) in what's actually rendered
today, rather than only the static doc hierarchy. Not used for brand-new
screens — those design from the style guide and reference baselines alone.

There is no device, emulator, or Playwright in this environment. The
closest thing to a "live screenshot" is **Roborazzi**, a JVM/Robolectric
test that renders Compose content headlessly to a PNG without a real
device. It's not a live device capture — see Caveats below — but it's the
only mechanism that actually reflects current code rather than a
potentially-stale doc.

## Workflow

1. **Find existing coverage.** Look in
   `skyline-iptv/app/src/test/java/com/denham/skyline/ui/ScreenshotTests.kt`
   and any other `*ScreenshotTests.kt` / `*Screenshots.kt` files for a test
   that already renders the target screen (not just its components).
   `HomeScreen` has a baseline: `HomeScreenshotTest.kt` (next to `HomeScreen.kt`
   in `ui/home/`) renders the assembled screen in the David-selected
   member-chip state to `brain/component-screenshots/home_david.png`. If the
   edit is to Home and the David tab's shape (its rails, its pinned-channel
   row, its layout) is close enough to what's needed, **extend that test's
   sample data or add a second `@Test` in the same class** rather than
   writing a whole new screen-rendering harness from scratch — the
   `AppContainer` seam it uses (in-memory Room DB, network-disabled
   OkHttpClient, `seedAccountForTesting`) is the reusable part.
2. **If none exists for the target screen, write a minimal scoped one** in
   the same style:
   `@RunWith(RobolectricTestRunner::class)`, `@GraphicsMode(NATIVE)`, a
   `@Config` qualifier matching phone (`RobolectricDeviceQualifiers.Pixel7`)
   or TV (`"w1280dp-h720dp-land-xhdpi"`) as relevant, `setContent {
   SkylineTheme { <the actual screen composable, with realistic sample
   data> } }`, then `compose.onRoot().captureRoboImage(<path>)`.
   - **Check the actual `captureRoboImage(...)` path argument you write (or
     that an existing test already uses) rather than assuming a
     convention** — this repo has drifted before (a prior version wrote
     into gitignored `build/` and silently produced no committed golden;
     the current component tests write to a `../../docs/skyline-screenshots/`
     relative path while the committed PNGs actually live in
     `brain/component-screenshots/`). Resolve the real output path from the
     test file itself, don't guess.
   - Use realistic sample content (existing rows/cards with real-looking
     titles), not empty/placeholder state, so the render is representative.
3. **Run it scoped**, not the whole suite:
   `./gradlew testDebugUnitTest --tests "*<ScreenName>*"` (fall back to the
   full `ScreenshotTests` class if no narrower filter matches).
4. **Locate the output PNG** using the path resolved in step 2, and copy or
   reference it as `design/<feature>/current-state/<screen>.png`.
5. **Read the PNG** as a direct visual input to the design step — alongside
   the doc hierarchy, not instead of it.

## Using the result

- Treat the screenshot as ground truth for "what's actually there right
  now," which may have drifted from `brain/reference-designs/` (already
  documented there as a stale baseline, not an exact spec).
- If the screenshot shows something wrong — a design-system violation
  (raw hex, off-grid spacing), a broken or awkward layout, poor contrast —
  call it out and fold the fix into the brief. Don't just replicate a flaw
  because it's what's currently rendered.
- In the brief, separate **requested-edit changes** from **opportunistic
  fixes found in the screenshot**, so the developer and reviewer can tell
  which is which and push back on either independently.

## Caveats — state these plainly if relevant to the brief

- Robolectric rendering can differ subtly from a real device: no live
  network/data (sample content only), no real touch/D-pad focus state, font
  rendering and anti-aliasing can differ slightly from an actual phone/TV.
- The player screen is explicitly excluded from Roborazzi coverage — its
  video surface isn't meaningfully renderable under Robolectric. For that
  screen (or any screen that turns out not to be Roborazzi-coverable), say
  so in the brief and fall back to reading the screen's Compose source
  directly instead of blocking on a screenshot.
- Any screen whose real screen composable is driven by a `ViewModel(container:
  AppContainer)` (most of them) needs `AppContainer` itself to be
  constructible under Robolectric without touching a real network, DB file,
  or the Android Keystore. `HomeScreenshotTest.kt` established the pattern:
  `AppContainer.createDatabase()`/`createOkHttpClient()` are `protected
  open` for exactly this, and `seedAccountForTesting(...)` sets the account
  without going through `CredentialStore` (Keystore-backed, unavailable
  under Robolectric). Reuse that seam for other screens rather than
  re-deriving it — it's on `AppContainer`, not `HomeViewModel`.
- `HomeScreen`'s David-only "Football" section is *not* covered by
  `home_david.png` — it's gated on `BuildConfig.FOOTBALL_DATA_API_KEY`,
  which is empty outside CI (real value is a CI secret, and it must never be
  baked into a local/test build — see the "nothing secret in the APK" rule).
  A design task specifically about that section still needs a source read,
  or a further seam, not this screenshot.
