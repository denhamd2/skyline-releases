## Context

See proposal.md - Why/What Changes. Key constraints from exploration: no
screen has a stateless variant today; no `Clock` abstraction exists anywhere
(9+ inline `System.currentTimeMillis()` call sites); `AppContainer` is a
concrete class but `ui/ContainerViewModel.kt`'s
`containerViewModel { container -> VM }` pattern means ViewModels are
constructed from individually-passed dependencies, not the container as a
whole; repositories wrap Room DAOs directly, no repository interfaces exist;
Coil is a single global singleton (`SkylineApp: ImageLoaderFactory`); minSdk
is 26 (Android 8.0), which matters for the Clock decision below.

## Goals / Non-Goals

**Goals:**
- Every real screen (except the player) captured deterministically at phone
  and TV qualifiers, verified in CI.
- Reuse existing patterns and libraries wherever one already fits, rather
  than inventing new abstractions — matches this project's own design-system
  rule ("reuse before writing") and its established manual-DI style.

**Non-Goals:**
- A DI framework, or turning `AppContainer`/repositories into
  interface-based abstractions. Both would be larger, riskier changes than
  this one needs; the fixture approach below works against the concrete
  classes as they are.
- Screenshotting `PlayerScreen` (see proposal).
- Changing any actual "now"-dependent *behavior* — the Clock change is a
  refactor of how the current time is obtained, not what the app does with
  it. Production still reads the system clock.

## Decisions

### Clock: `java.time.Clock`, not a custom interface

minSdk 26 means `java.time.Clock` is natively available (Android 8.0
supports `java.time` without desugaring) — no new dependency, no new
abstraction to maintain. `Clock.systemUTC()` replaces
`System.currentTimeMillis()` at production call sites (via `.millis()`,
a near 1:1 swap); `Clock.fixed(instant, zoneId)` supplies a deterministic
time in tests. Threaded the same way every other dependency already is in
this codebase: a constructor parameter defaulting to `Clock.systemUTC()`,
added to the ViewModels and repositories that currently call
`System.currentTimeMillis()` directly (`GuideRepository`, `EpgRepository`,
`YouTubeRepository`, and the relevant screen ViewModels). Existing callers
need no changes since the default preserves current behavior; tests pass
`Clock.fixed(...)` explicitly.

Alternative considered: a hand-rolled `interface Clock { fun nowMillis():
Long }`. Rejected — `java.time.Clock` already does this, is standard, and
needs zero new code to define.

### Fixture data: one in-memory Room database, real repositories against it

`Room.inMemoryDatabaseBuilder(context, SkylineDatabase::class.java)
.allowMainThreadQueries().build()`, seeded with literal fixture rows (a
handful of channels, movies, series, EPG entries, categories — enough for
each screen to render non-empty state) via a single shared test helper.
Repositories are the *real* classes, constructed against this database
instead of the production one; ViewModels are constructed directly (the
`containerViewModel` lambda pattern already supports this) rather than
resolved through a live `AppContainer`.

Alternative considered: hand-write a fake implementation of each of the six
repository classes. Rejected — repositories are concrete, not behind
interfaces, so "faking" one means either subclassing/overriding methods
(fragile, drifts from real query behavior) or duplicating their logic
against an in-memory data structure (more new code than seeding a real
database, and exercises nothing real). The in-memory-Room approach is less
code and tests real Room query logic, not a re-implementation of it.

### Images: Coil's own `coil-test` fake engine, not a hand-rolled fetcher

Coil publishes `io.coil-kt:coil-test` (matching the pinned `coil` version)
with `FakeImageLoaderEngine`, built for exactly this — intercepting image
requests in tests and returning a fixed placeholder deterministically.
Swapped in via `Coil.setImageLoader(...)` at test setup, reset after. No
production code change, since Coil's global-singleton wiring already
supports being overridden. Exact API surface to confirm against the pinned
Coil version during implementation — a code-level detail, not a design one.

Alternative considered: a hand-written no-op `ImageLoader`/`Fetcher`.
Rejected for the same reuse-over-reinvention reason as the Clock decision.

### Stateless extraction: `<Name>ScreenContent`, existing UiState classes reused

Each `XScreen(viewModel: XViewModel, ...)` becomes a thin wrapper that keeps
owning the `ViewModel`, calling `.collectAsState()`, and delegates to a new
`XScreenContent(state: XUiState, <same callbacks>)` composable that does the
actual rendering. Where a screen already has a `UiState` data class (e.g.
`GuideUiState`), reuse it directly as the new composable's parameter rather
than inventing a parallel type. This is a widely-used Compose convention
(state hoisting into a "Content" composable), not a local invention, so it
should read familiarly rather than as a one-off pattern.

The three TV screens with no `ViewModel` seam
(`TvSportsScreen`/`TvKidsScreen`/`TvMySkyScreen`) need a `ViewModel`
introduced first — same shape as every other screen's, reading what they
currently read inline from `AppContainer`/the database — before the same
`Content`-extraction applies. This is the one place in the change that adds
a `ViewModel` where none existed, rather than only extracting from one that
already does.

### Test file organization

New tests live under a new
`app/src/test/java/com/denham/skyline/ui/screenshots/` package — kept
separate from the existing `ScreenshotTests.kt` (component gallery),
`ClickBehaviourTest.kt` (interaction assertions, no images), and `core/`'s
pure unit tests, so screen-level screenshot coverage is easy to find as its
own thing. One shared fixture-database helper and one shared Coil-fake
setup, reused across all of them, rather than duplicated per screen.

## Risks / Trade-offs

- **Large surface (~17 files) in one change** → Mitigated by the per-screen
  build order in tasks.md: each screen's extraction and tests are pushed and
  confirmed green in CI individually, not written all at once and pushed at
  the end. Same discipline as the rest of this project's CI-only-compiler
  constraint, applied at file granularity instead of change granularity.
- **Stateless extraction touches production screen code, not just tests** →
  Mechanical by construction (move existing rendering code into the new
  `Content` composable, thread the existing state/callbacks through
  unchanged); the project's existing unit and click-behavior tests continue
  to exercise the same screens as an independent safety net.
- **`Clock` threading reaches into repository-layer freshness checks, not
  only UI** → Default-parameter approach means no call site is forced to
  change; only the sites that need deterministic tests receive an explicit
  fixed clock.
- **`TvSportsScreen`/`TvKidsScreen`/`TvMySkyScreen` need a real structural
  change (introducing a `ViewModel`), not just extraction** → Isolated to
  the last step of the build order specifically because it's the one place
  carrying more risk than the rest of the change.
- **In-memory Room under Robolectric needs `allowMainThreadQueries`, which
  is a deliberate test-only relaxation** → Confined to the single shared
  fixture helper, not a pattern that spreads into production code.

## Migration Plan

Matches the build order already agreed with the user: scaffolding (Clock,
fixture database, Coil fake) → `LoginScreen` → `AccountScreen`/
`TvBrowseScreen` → `GuideScreen` → `HomeScreen`/`LiveScreen`/`SettingsScreen`
→ remaining screens → the three TV screens needing a new `ViewModel`, last.
Each step is its own commit, pushed and confirmed green before the next.
No rollback plan needed beyond git revert — nothing here is a data
migration or a deploy that can partially apply.
