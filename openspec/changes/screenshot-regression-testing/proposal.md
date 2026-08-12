## Why

Roborazzi screenshot tests exist today but check almost nothing and gate
nothing. `ScreenshotTests.kt` renders two isolated component galleries, not
any real screen, and CI only ever runs `recordRoborazziDebug` — record mode,
which unconditionally overwrites the committed PNGs every push. There is no
`verify` step anywhere, so a visual regression has never once failed a
build, despite `docs/skyline-screenshots/README.md` describing this as a
reviewed golden-history workflow. That gap was found the same way the
design-system enforcement gap was found earlier: by reading what the
workflow file actually runs, not what the docs claim it runs.

Separately, this was originally requested as a standing ask — screenshots of
every change, so it's visible without opening the app on a device. Neither
piece has existed until now.

## What Changes

- Switch CI from `recordRoborazziDebug` to `verifyRoborazziDebug` for the
  existing component-gallery tests, so an unintended visual change actually
  fails the run. Record mode stays available via an explicit
  `workflow_dispatch` input for intentional visual updates.
- Correct `docs/skyline-screenshots/README.md` to describe the mechanism
  that actually runs.
- Introduce a `Clock`/now-provider abstraction. **BREAKING for internal call
  sites**: replaces ~9 inline `System.currentTimeMillis()` calls across UI
  and repository code (`GuideScreen.kt`, `HomeScreen.kt`, `CatchUpScreen.kt`,
  `LiveScreen.kt`, `DetailScreens.kt`, `TvScreens.kt`,
  `GuideRepository.kt`, `EpgRepository.kt`, `YouTubeRepository.kt`) with an
  injected time source, so "now"-dependent UI (the Guide's now-line, live
  badges, continue-watching timestamps) can render deterministically under
  test. Production behavior is unchanged — the real implementation still
  reads the system clock.
- Add a stateless/presentational composable for every real screen (~17
  files), separating rendering from the `ViewModel` that owns state today.
  The existing `XScreen(viewModel: XViewModel, ...)` wrapper keeps owning the
  `ViewModel` and delegates rendering to the new stateless variant.
- Introduce test fixtures: an in-memory `SkylineDatabase` builder seeded with
  literal fixture rows (real repository classes run against it, not fakes of
  the repositories themselves), and a fake Coil `ImageLoader` swapped in via
  `Coil.setImageLoader` at test setup.
- Add Roborazzi tests for every screen's stateless composable, phone and TV
  qualifiers, `verify`-mode from the start (not record-then-forget like the
  existing component gallery was).
- Refactor `TvSportsScreen`, `TvKidsScreen`, `TvMySkyScreen` to go through a
  `ViewModel` instead of reading the database directly in the composable
  body — a prerequisite for these three to reach the same testable shape as
  every other screen, not itself a behavior change.

Explicitly out of scope: `PlayerScreen`. Its video surface isn't
meaningfully screenshottable under Robolectric; a chrome-only capture with
the surface stubbed is a smaller, separate follow-up if ever pursued.

## Capabilities

### New Capabilities

- `screenshot-testing`: what is captured, on which form factors, what makes
  it deterministic (Clock, Coil, fixture data), and what gates CI versus
  what is documentation-only.

### Modified Capabilities

(none — no existing spec currently describes screenshot behavior; this is
new ground, not a change to a documented requirement)

## Impact

- `.github/workflows/build-skyline-apk.yml` — record→verify switch, new
  `workflow_dispatch` input.
- `docs/skyline-screenshots/README.md` — corrected.
- ~17 screen files across `ui/**` and `tv/TvScreens.kt` — each gains a
  stateless composable; three TV screens also gain a `ViewModel`.
- New: a `Clock` abstraction and its call sites; new test fixture code under
  `app/src/test/java/**`.
- Not verifiable in this environment: whether the rendered screens visually
  match the real app on a device. CI verifies compilation, that tests pass
  against their own committed goldens, and that the APK still builds and
  publishes — not visual correctness against a phone.
