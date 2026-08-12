## Purpose

Defines how Skyline is built, signed and published, so that a build can be
installed over its predecessor and identified once on a device.

## Requirements

### Requirement: Stable debug signing key

The debug build type SHALL be signed with the keystore committed at
`skyline-iptv/keystore/skyline-debug.keystore`.

Without an explicit `signingConfig`, the Android Gradle Plugin generates a
random debug keystore on any machine lacking `~/.android/debug.keystore`.
CI runners are ephemeral, so every build was signed with a different key and
Android refused to install one over another with "package conflicts with an
existing package". This made in-app updating impossible.

#### Scenario: Installing over a previous build

- **WHEN** a user installs a newer CI build over an existing one
- **THEN** the signatures match and installation proceeds without uninstalling

### Requirement: Builds are individually identifiable on device

`versionName` SHALL incorporate the CI run number, as `1.1.<run>`.

Every build previously reported `1.1`, so Android's App info screen could not
distinguish a freshly installed APK from the one already present, and "it
still does X" reports were unattributable to a build.

#### Scenario: Confirming which build is installed

- **WHEN** a user opens Settings → Apps → Sky Go
- **THEN** the version shown identifies the exact CI run that produced it

### Requirement: APKs are published to this repository's rolling release

CI SHALL publish each APK to this repository's `skyline-latest` release,
using the workflow's own `GITHUB_TOKEN`.

Source and distribution live in the same public repository, so no
cross-repo credential is needed to publish.

#### Scenario: Publishing a build

- **WHEN** a build succeeds on the development branch
- **THEN** `Skyline.apk` is uploaded to the `skyline-latest` release

### Requirement: Release notes carry the build version

CI SHALL write a `build-version: <versionName>` marker into the release notes
on every run, not only when creating the release.

The release is created once and thereafter only its asset is replaced, so
notes written solely at creation would pin the marker to the first published
version forever while the APK changed underneath it.

#### Scenario: Publishing to an existing release

- **WHEN** the `skyline-latest` release already exists
- **THEN** its notes are refreshed with the current build's marker

### Requirement: Nothing secret is compiled into the APK

The APK SHALL NOT contain credentials or tokens.

Builds are published publicly and can be unpacked trivially. The IPTV
password was previously compiled in via a `BakedCredentials` object; it was
removed when publishing moved to a public repository.

#### Scenario: Adding a capability that needs a secret

- **WHEN** a feature requires a credential to reach a third party
- **THEN** either it is scoped so exposure is capped at quota use, or the
  feature is designed not to need the secret on-device

### Requirement: CI is the only compiler

Work SHALL NOT be reported as complete on the strength of a push.

The Android Gradle Plugin cannot be resolved in the agent environment, so
code cannot be compiled or run there. Several changes were reported as
shipped while failing to compile, and a broken detekt config silently
skipped the APK build for weeks while runs still appeared to "finish".

#### Scenario: Completing a change

- **WHEN** a change is pushed
- **THEN** the workflow run is checked to completion
- **AND** the APK build and publish steps are confirmed to have run rather
  than been skipped

### Requirement: Design-system violations are machine-checked

CI SHALL run the project's custom design-system rules against the app
sources, so that the conventions they cover are checked mechanically rather
than by review alone.

The rules previously existed as source files that no build script referenced,
so they never compiled or ran, while documentation asserted an enforcement
pipeline that was not in effect.

Only colour-token usage is machine-checked. Of the six original rule files,
five had been written against a detekt API that does not exist and had never
compiled; they were removed rather than kept as dead files. Spacing,
typography, animation duration and component reuse are therefore **not**
machine-checked, and documentation must not claim otherwise until they are
reimplemented.

#### Scenario: A change introduces a hardcoded colour

- **WHEN** UI code adds a raw colour literal instead of a palette token
- **THEN** the lint step reports it against the design-system rules

#### Scenario: A change conforms to the design system

- **WHEN** UI code uses palette tokens
- **THEN** the design-system rules raise nothing for it

### Requirement: A reported violation states its remedy

A design-system violation SHALL be reported with the specific correction to
make, where the rule can determine it, and SHALL surface on the CI run's own
summary rather than only in the build log.

A report that restates the rule leaves the reader to find the remedy by hand.
The colour rule knows both the offending literal and the palette, so it can
name the token outright; where a literal matches no token it must say so
rather than guess.

#### Scenario: The violating value maps to a known token

- **WHEN** UI code hardcodes a colour that exists in the palette
- **THEN** the report names that token as the replacement

#### Scenario: The violating value maps to no token

- **WHEN** UI code hardcodes a colour that is not in the palette
- **THEN** the report instructs that a token be added rather than naming one

### Requirement: Enforcement is proven, not inferred from a green run

A design-system rule SHALL be verified against a deliberate violation, and
the violation SHALL NOT be left committed.

These rules report zero findings on a clean codebase, so a passing run looks
identical whether the rule is working or not running at all. That is not a
hypothetical: the gate was configured, appeared configured, and had never
executed once — `buildUponDefaultConfig.set(false)` in `app/build.gradle.kts`
called `.set()` on what is a plain `Boolean` on detekt's `Detekt` task type,
which broke build-script compilation and therefore every Gradle task,
including the APK build and both publish steps.

The proving violation must not change rendered output. A red run still
publishes an APK, and that APK is what the in-app updater installs on the
family's devices.

#### Scenario: Verifying a rule works

- **WHEN** a design-system rule is added or repaired
- **THEN** a deliberate violation is used to confirm it fails the build and
  names the remedy
- **AND** the violation is reverted once the evidence is recorded

### Requirement: Design-system findings are held separately from code debt

Design-system rules SHALL be evaluated against their own zero-tolerance
threshold, separately from the general-purpose lint rules and their allowance
for pre-existing debt.

Zero tolerance is only reasonable because the design-system rules report no
findings: the only hardcoded colours in the app are the token definitions in
`ui/theme/Theme.kt`, which is excluded. The general rules are a different
matter — they report several hundred findings accumulated while the gate was
broken. Holding both to one threshold would mean either absorbing new design
violations into that allowance or failing a build over an unrelated magic
number.

#### Scenario: Pre-existing general lint debt

- **WHEN** the codebase carries findings from the general-purpose rules
- **THEN** they are counted against their own allowance and do not fail the
  build

#### Scenario: New design-system violation is introduced

- **WHEN** a change introduces any design-system violation
- **THEN** the build fails, rather than the violation being absorbed by a
  global issue allowance

### Requirement: Detekt configuration is validated

Detekt SHALL run with configuration validation enabled, and a new
configuration key SHALL be checked against the bundled default config for the
pinned detekt version.

Validation was disabled to work around a configuration block referencing a
ruleset that could never load. With validation off, a mistyped or stale entry
passes silently and the rule it configures simply never applies. Enabling it
exposed four such keys that had been dead for as long as they had existed:
`empty` (the ruleset is `empty-blocks`), `CyclomaticComplexity` (the rule is
`CyclomaticComplexMethod`), `style/FinalNewline` (a `formatting` ktlint rule
not on the classpath) and `naming/PropertyNaming` (no such rule).

A ruleset supplied through `detektPlugins` satisfies validation by
registration alone; it needs no `config/config.yml` resource in its jar.

#### Scenario: Configuration references an unknown rule

- **WHEN** detekt configuration names a rule or ruleset that is not loaded
- **THEN** the configuration is rejected rather than silently ignored

#### Scenario: Configuration matches the loaded rulesets

- **WHEN** every configured ruleset is registered
- **THEN** validation passes and the lint step proceeds

### Requirement: A lint failure does not withhold a build

Lint SHALL NOT be able to prevent the APK from being built and published. It
SHALL still fail the run, so the violation is visible.

Lint previously ran before the build, so a lint failure skipped every step
below it. When the configuration referenced an unloadable ruleset, that
withheld every build for about three weeks while the app on the device
appeared merely unchanged — the failure was indistinguishable from nothing
having been done.

This ordering does not protect against a broken build script, which fails
`assembleDebug` as readily as it fails lint. It protects only against a
failing check.

#### Scenario: Lint fails

- **WHEN** a lint step reports a violation or its configuration is rejected
- **THEN** the APK has already been built and published
- **AND** the run is reported as failed

### Requirement: Documentation reflects the enforcement that runs

Documentation describing design-system enforcement SHALL describe only
mechanisms that are actually in effect, and SHALL mark anything aspirational
as such.

`DESIGN_SYSTEM_ENFORCEMENT.md` described pre-commit hooks and IDE integration
that CI never invokes, which made enforcement appear stronger than it was. A
documented check that does not run is worse than no check, because it is
trusted.

#### Scenario: Reading the enforcement documentation

- **WHEN** a contributor reads the enforcement documentation
- **THEN** each mechanism listed is either running in CI, or explicitly
  identified as optional or not yet implemented
