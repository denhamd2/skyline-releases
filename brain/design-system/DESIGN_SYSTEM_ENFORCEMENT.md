# Sky Design System Enforcement

What actually checks design-system compliance, and what does not.

An earlier version of this document described six layers of enforcement as
though all six were active. Only one of them gated anything, and the mechanism
it described did not exist: five of the six custom detekt rules had never been
compiled, and the ruleset they belonged to could not be loaded at all.
`detekt.yml` referenced it anyway, which hard-failed every CI run and skipped
the APK build for about three weeks without anyone noticing.

Each layer below is therefore labelled **Enforced**, **Optional**, or **Not
implemented**. If you add a mechanism, label it honestly — a documented check
that does not run is worse than no check, because it is trusted.

---

## Enforced: the design-system gate in CI

**Runs:** every push, via `.github/workflows/build-skyline-apk.yml`
**Command:** `./gradlew detektDesignSystem`
**Fails on:** any violation. Zero tolerance, no baseline.

One rule is implemented today:

| Rule | Flags | Proposes |
|---|---|---|
| `SkyPaletteUsage` | `Color(0x…)` literals in UI code | the `SkyPalette` token for that exact colour |

Zero tolerance is only reasonable because the codebase is already clean: this
rule reports **no** findings on `main`. Every hardcoded colour in the app lives
in `ui/theme/Theme.kt`, which is excluded — that file is where the tokens are
*defined*, so its literals are the source of truth, not violations.
`ui/components/` is excluded for the same reason.

When it fails, the run's summary page lists each violation with the token to
use, so the fix is stated rather than looked up:

```
Color(0xFF0B69F5) is SkyPalette.Accent. Replace the literal with the token: SkyPalette.Accent.
```

A colour that matches no token gets the general instruction instead — add a
token to `SkyPalette` rather than inlining a hex. The rule never guesses.

**The gate runs after the APK is built and published, deliberately.** A
violation still fails the job and shows the run as red, but it can no longer
stop a build reaching a phone. That ordering exists because the opposite
ordering is what caused the three-week outage.

### This was proven, not assumed

A rule that reports zero findings cannot be shown to work by a green run —
silence is the same whether it is passing or not running at all. So it was
tested against a deliberate violation (run 78):

```
PlayerScreen.kt:96:50: Color(0xFF0B69F5) is SkyPalette.Accent.
  Replace the literal with the token: SkyPalette.Accent. [SkyPaletteUsage]
> Analysis failed with 1 weighted issues.
```

In that same run, "Build debug APK" and both publish steps **succeeded**. The
gate bit, named the fix, marked the run red, and still shipped an installable
APK — all four at once. The violation was reverted immediately afterwards.

Worth repeating the reason this proof was necessary: until run 76 the gate had
**never executed even once**. `app/build.gradle.kts` called
`buildUponDefaultConfig.set(false)`, but that property is a plain `Boolean` on
detekt's `Detekt` *task* type, so the call did not resolve and Kotlin matched
`StringBuilder.set` instead. That broke build-script compilation, which fails
every Gradle task — so the APK build and both publish steps were skipped too.
Same lesson as before, in a new place: the step was configured, looked
configured, and did nothing.

### Adding a rule

Rules live in `skyline-iptv/detekt-rules/`, a plain JVM module consumed via
`detektPlugins(project(":detekt-rules"))`. A new rule needs three things, and
missing any one of them makes it silently inert:

1. the rule class, written against the real detekt 1.23.6 API
   (`Severity.Warning`, `visitCallExpression` — not the names an LLM or an
   older tutorial will suggest);
2. registration in `SkyDesignSystemRuleProvider`;
3. its id added to `sky-design-system` in `detekt-design-system.yml`.

Config validation is on in **both** `detekt.yml` and
`detekt-design-system.yml`, so step 3 without step 2 fails the build loudly.
That is the intended behaviour — it is exactly the mistake that went
undetected before.

A custom ruleset needs no `config/config.yml` resource in its jar to satisfy
validation; registration through `detektPlugins` is enough. Verified in run 76.

Turning validation on also exposed four keys in `detekt.yml` that had been
silently dead — accepted while validation was off, and simply never applied:
`empty:` (the ruleset is `empty-blocks:`), `CyclomaticComplexity` (the rule is
`CyclomaticComplexMethod`), `style/FinalNewline` (a `formatting` ktlint rule,
not on the classpath) and `naming/PropertyNaming` (no such rule). Check a new
key against detekt's bundled `default-detekt-config.yml` for the pinned
version rather than trusting that it was accepted.

Spacing, typography, animation-duration and component-reuse rules were removed
rather than kept as dead files. They had been written against an invented API
and had never compiled. They are recoverable from git history if reimplemented.

---

## Enforced: code quality lint in CI

**Command:** `./gradlew detekt` (config: `skyline-iptv/detekt.yml`)
**Fails on:** exceeding `maxIssues: 450`

This is the broad, built-in detekt ruleset — `MagicNumber`, `LongMethod`,
`FunctionNaming` and so on. It reports ~393 findings, debt accumulated while
the lint step was broken. The allowance is deliberately loose: this is generic
code debt, not design-system debt, and holding the two to one threshold would
mean either tolerating new design violations or failing the build over an
unrelated magic number. Tighten it as the debt is paid down.

---

## Optional: pre-commit hooks (local only)

**Config:** `.pre-commit-config.yaml`
**Runs:** only if you install it yourself — `pre-commit install`
**CI never invokes it.** `git commit --no-verify` skips it.

A faster local echo of the CI gate, plus generic hygiene (trailing whitespace,
YAML syntax, private-key detection). It is convenience, not the gate.

Note that the colour hook was unusable as written: it had no exclusion for
`ui/theme/`, so it fired on `Theme.kt` — the token definitions themselves — and
would have blocked every commit. Nothing ever ran it, so nobody found out. Its
exclusions now match `detekt-design-system.yml`; keep the two in step.

---

## Optional: IDE inspection

The detekt plugin for Android Studio reads `detekt.yml` and shows findings
inline. Useful, entirely local, and configured per-developer. Nothing depends
on it.

---

## Optional: PR checklist

`.github/pull_request_template.md` carries a design-system checklist for
things a rule cannot judge — whether a component should have been reused,
whether motion feels right. This repo is usually pushed to directly rather
than through PRs, so in practice the checklist applies only when a PR is
opened.

---

## Not implemented: visual regression testing

CI runs `./gradlew recordRoborazziDebug`, which **records** screenshots and
commits the PNGs to `docs/skyline-screenshots/`. It does not compare them
against a baseline, and no threshold fails a build. Visual drift is caught by
looking at the images, if someone looks.

Making this real would mean adding a `compareRoborazzi` step and a committed
baseline. Until that exists, do not describe screenshots as a regression gate.

---

## Reference

- Tokens and patterns: `docs/SKY_DESIGN_SYSTEM.md`
- Reusable components: `docs/COMPONENT_LIBRARY.md`
- Principles and research: `docs/sky-design-language.md`
- Working agreement for UI code: `.claude/rules/design-system.md`

## Common violations

### Hardcoded colour

```kotlin
// Wrong
Box(modifier = Modifier.background(Color(0xFF05070A)))

// Right
Box(modifier = Modifier.background(SkyPalette.Canvas))
```

Tokens: `Canvas`, `Surface`, `SurfaceElevated`, `Accent`, `AccentBright`,
`Brand`, `Indigo`, `LiveRed`, `TextPrimary`, `TextSecondary`, `TextMuted`,
`Error`. Adding one means adding it to `SkyPalette` in `ui/theme/Theme.kt` and
to `TOKENS` in `SkyPaletteUsageRule.kt`, so the rule can name it.

### Off-grid spacing

```kotlin
// Wrong
Button(modifier = Modifier.padding(15.dp))

// Right
Button(modifier = Modifier.padding(SkySpacing.l))
```

Not machine-checked — the spacing rule was removed as unusable. Reviewed by
eye, and by the optional pre-commit hook.

### Hardcoded typography

```kotlin
// Wrong
Text(text = "Hello", fontSize = 16.sp)

// Right
Text(text = "Hello", style = MaterialTheme.typography.bodyLarge)
```

Not machine-checked either, for the same reason.
