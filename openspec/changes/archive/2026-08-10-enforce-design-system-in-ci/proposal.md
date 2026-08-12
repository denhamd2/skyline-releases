## Why

The project documents a design-system enforcement pipeline that does not run.
Six custom detekt rules exist at `skyline-iptv/gradle/detekt/`, but no build
script references them and there is no `detektPlugins` dependency, so they are
never compiled or registered. `docs/DESIGN_SYSTEM_ENFORCEMENT.md` describes
pre-commit hooks and CI gating; CI never invokes pre-commit.

This is worse than having no enforcement, because the gap already caused an
outage. Since the ruleset was never loaded, detekt's config validator rejected
the `sky-design-system` block in `detekt.yml` as unknown properties and
hard-failed the lint step. Every run then skipped the APK build and publish
steps while still appearing to finish, so no installable build was produced
for roughly three weeks and the failure went unnoticed. It was unblocked only
by disabling config validation and raising `maxIssues` from 50 to 450 — both
of which mask the problem rather than fix it.

## What Changes

- Add a JVM Gradle module that compiles the existing rules and registers them
  with detekt, so `SkyPaletteUsage`, `SpacingGridCompliance`,
  `TypographyCompliance`, `AnimationDurationCompliance` and `ComponentReuse`
  actually execute against the app sources.
- Re-enable detekt config validation, which becomes safe once the ruleset is
  known. This is the step that caused the original outage and is treated as
  the highest-risk task.
- Adopt a detekt baseline so the unknown volume of pre-existing violations is
  recorded rather than blocking the build, and **new** violations fail. The
  current `maxIssues: 450` allowance is a blunt instrument that lets new
  problems through; a baseline distinguishes existing debt from regressions.
- Correct `docs/DESIGN_SYSTEM_ENFORCEMENT.md` so it describes what actually
  runs. Remove or clearly mark the parts that do not (pre-commit is not run by
  CI and requires local installation).

**Recommendation: wire the rules up rather than delete them.** Deleting is
cheaper and was the alternative considered, but the rules already exist, are
written against the detekt 1.23.x API that the project uses, and encode
conventions the team wants. The only automatic guardrail today is a
path-scoped Claude rule, which is advisory and does not apply to human
contributors or other tools. Deleting would leave the design system enforced
by nothing at all.

**Non-goal:** fixing the ~382 pre-existing built-in detekt findings (mostly
`MagicNumber`). Those are recorded in the baseline and left for separate work.

## Capabilities

### New Capabilities

<!-- none -->

### Modified Capabilities

- `build-and-release`: adds a requirement that design-system violations are
  machine-checked in CI, and that the detekt configuration is validated rather
  than having validation disabled to work around an unloadable ruleset.

## Impact

- **New module**: `skyline-iptv/detekt-rules/` (JVM, depends on
  `io.gitlab.arturbosch.detekt:detekt-api:1.23.6`), containing the rule
  sources moved from `skyline-iptv/gradle/detekt/` plus a
  `META-INF/services/io.gitlab.arturbosch.detekt.api.RuleSetProvider` entry.
- **Modified**: `skyline-iptv/settings.gradle.kts` (include the module),
  `skyline-iptv/app/build.gradle.kts` (`detektPlugins`),
  `skyline-iptv/detekt.yml` (re-enable validation, baseline, thresholds),
  `docs/DESIGN_SYSTEM_ENFORCEMENT.md`.
- **New**: a detekt baseline file.
- **Risk**: the lint step gates the APK build. A misconfiguration here stops
  builds reaching the device again, which is exactly the failure this change
  exists to prevent. Verification must confirm the APK build and publish steps
  ran, not merely that the workflow reported success.
- **Build time**: one extra module to compile on every run; expected to be
  small relative to the existing ~9 minute build.
