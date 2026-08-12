## Context

See `proposal.md` — Why. Requirements are in `specs/build-and-release/spec.md`.

Constraints that shape the approach:

- The rules already exist and target the detekt 1.23.x API (`Rule(config)`,
  `Issue(id, severity, description, debt)`, `RuleSetProvider`), matching the
  1.23.6 the project uses. This is a wiring job, not a rewrite.
- **The Android Gradle Plugin cannot be resolved in the agent environment, so
  nothing can be compiled locally.** CI is the only compiler, and each
  iteration costs a full ~9 minute run.
- The detekt step runs before the APK build in the workflow. If it fails, the
  APK build and publish steps are skipped while the run still looks like it
  ran to completion — the exact shape of the original three-week outage.
- The rules have never executed, so the number of violations they will report
  is unknown.

## Goals / Non-Goals

**Goals**

- The custom rules compile, register, and run against app sources in CI.
- New design-system violations are reported; existing ones do not block.
- Detekt configuration validation is on.

**Non-Goals**

- Fixing existing violations, custom or built-in (~382 findings, mostly
  `MagicNumber`). They go in the baseline.
- Rewriting or improving the rule logic. It is adopted as written; tuning
  follows once its real output is visible.
- Making pre-commit hooks work. They are not run by CI and are out of scope;
  documentation is corrected to stop implying otherwise.

## Decisions

### Separate JVM module over `buildSrc` or a composite build

`skyline-iptv/detekt-rules/` as a plain `kotlin("jvm")` module, included in
`settings.gradle.kts` and consumed via `detektPlugins(project(":detekt-rules"))`.

Alternatives: `buildSrc` couples rule changes to build-script invalidation and
recompiles the whole build on edit; a composite build adds configuration
complexity for one small artifact. A sibling module is the conventional detekt
layout and keeps the rules out of the Android module's compile path.

The module must not apply the Android plugin — it is a JVM library consumed by
the detekt tool, not app code.

### Baseline instead of a global issue allowance

Generate a detekt baseline capturing current findings, then reduce
`build.maxIssues` to 0.

`maxIssues: 450` was set to unblock CI and is a blunt instrument: it permits
450 *new* problems as readily as it tolerates existing ones. A baseline
records existing findings specifically, so anything new fails. This is what
makes the "new violations are reported" requirement meaningful rather than
nominal.

Trade-off: a baseline hides existing debt behind a file that is easy to
regenerate and forget. Accepted deliberately, and named as a non-goal above.

### Two CI-verified stages, not one

Wiring and validation are landed and verified separately:

- **Stage A** — add the module, `detektPlugins`, and the baseline. Leave
  `config.validation: false` and `maxIssues` as-is.
- **Stage B** — enable `config.validation`, set `maxIssues: 0`.

Splitting them means a failure is unambiguous. If both landed together and the
lint step broke, it would not be clear whether the module failed to load or
validation rejected the config — and each CI round trip is ~9 minutes. Stage A
also proves the ruleset loads, which is the precondition that makes enabling
validation safe at all.

### Validation is enabled only after the ruleset demonstrably loads

Re-enabling `config.validation` while `sky-design-system` is still unknown to
detekt reproduces the original outage exactly. Stage B is therefore gated on
Stage A being green, not merely pushed.

## Risks / Trade-offs

- **Lint gates APK delivery; a mistake stops builds reaching devices again.**
  → Verify each stage by confirming the APK build and publish steps actually
  ran, not that the workflow "succeeded". Rollback for each stage is a
  one-line revert (`validation: false`, or dropping the `detektPlugins` line).

- **Rule output is unknown; rules have never run.** They may be noisy or throw
  on real code. → Stage A surfaces this with the build still passing. A rule
  that proves unusable is switched off in `detekt.yml` rather than blocking
  the change.

- **`ComponentReuse` is the likeliest offender**, since "looks like a
  duplicate component" is inherently fuzzy. → Treated as the first candidate
  for deactivation if it produces noise.

- **Baseline staleness.** A regenerated baseline silently absolves new
  violations. → Regenerate only deliberately; never as a fix for a failing
  build.

- **Every iteration costs a full CI run.** → Keep each stage minimal and
  independently revertible.

## Migration Plan

1. Land Stage A. Confirm green *and* that the APK published.
2. Inspect the run's detekt output to see what the custom rules actually
   report.
3. Deactivate any rule that is unusable, with the reason recorded.
4. Land Stage B. Confirm green *and* that the APK published.
5. Correct `docs/DESIGN_SYSTEM_ENFORCEMENT.md` to match what now runs.

Rollback at any point is a revert of the offending commit; the app source is
untouched by this change, so nothing on-device is affected.

## Open Questions

- Whether `ComponentReuse` and `AnimationDurationCompliance` produce
  acceptable signal. Answerable from Stage A's output, and handled by
  deactivating a rule in `detekt.yml` — it changes neither the specs, the
  approach, nor the task breakdown.
