## 1. Stage A — make the ruleset load

- [x] 1.1 Create `skyline-iptv/detekt-rules/build.gradle.kts` as a
      `kotlin("jvm")` module depending on
      `io.gitlab.arturbosch.detekt:detekt-api:1.23.6`, with a JVM target
      matching the app (17). Do not apply the Android plugin.
- [x] 1.2 Move the six rule sources from `skyline-iptv/gradle/detekt/` to
      `skyline-iptv/detekt-rules/src/main/kotlin/com/denham/skyline/detekt/`,
      keeping the `com.denham.skyline.detekt` package. Delete the old
      directory so there is one copy.
- [x] 1.3 Add
      `src/main/resources/META-INF/services/io.gitlab.arturbosch.detekt.api.RuleSetProvider`
      containing `com.denham.skyline.detekt.SkyDesignSystemRuleProvider`.
      Without this the module compiles but detekt never discovers the rules.
- [x] 1.4 Add `include(":detekt-rules")` to `skyline-iptv/settings.gradle.kts`.
- [x] 1.5 Add `detektPlugins(project(":detekt-rules"))` to
      `skyline-iptv/app/build.gradle.kts`.
- [x] 1.6 Leave `config.validation: false` and `maxIssues: 450` untouched in
      this stage — changing them here would confuse a module-loading failure
      with a configuration failure.
- [x] 1.7 Push and wait for the workflow run to complete. Confirm it is green
      **and** that the "Build debug APK" and both publish steps ran rather
      than being skipped. A push is not done.
      *Run 71 (`b3d0492`): all 15 steps success, including
      `:detekt-rules:compileKotlin` → `:jar` → `:app:detekt`, the APK build and
      both publish steps.*

## 2. Assess what the rules actually report

> **Scope revised during apply.** The first Stage A run proved the module
> wires up correctly but that the rules themselves never compiled: they were
> written against a detekt API that does not exist (`Severity.WARNING` vs
> `Warning`, `visitKtCallExpression` vs `visitCallExpression`,
> `visitNamedArgument`, `KtNamedArgument`). The proposal's assumption that
> this was "a wiring job, not a rewrite" was wrong. Agreed course: ship
> `SkyPaletteUsage` alone, correctly; the other four are removed and
> recoverable from git history for later reimplementation.

- [x] 2.1 Read the detekt output from the Stage A run and record how many
      findings `SkyPaletteUsage` produced.
      *Zero, out of 393 total findings. The other 393 are built-in rules — 203
      `MagicNumber`, 77 `FunctionNaming`, 32 `LongMethod`, and so on. Every
      hardcoded `Color(0x…)` in the app is in `ui/theme/Theme.kt`, which the
      rule excludes because that is where the tokens are defined. Corroborated
      independently: the pre-commit colour grep, once given the same
      exclusions, also matches nothing.*
- [x] 2.2 Deactivate in `detekt.yml` any rule that is unusable (throws, or is
      overwhelmingly false-positive), recording the reason in the commit.
      *Superseded by the Stage A rewrite: the four unusable rules were removed
      outright rather than deactivated, since they had never compiled.*
- [x] 2.3 If any rule was deactivated, push and confirm the run is green with
      the APK published. *Covered by run 71.*

## 3. Stage B — enforce

> **Scope revised during apply, second time.** The plan was: baseline all
> current findings, then `maxIssues: 0` globally. Task 2.1 disproved its
> premise. There are no design-system findings to baseline, and applying
> `maxIssues: 0` to the *general* rules would fail builds on the 393 unrelated
> items and on every new magic number thereafter. Instead the design-system
> rules get their own task and their own zero threshold; general debt keeps
> its soft allowance. No baseline file is created, because there is nothing to
> record.

- [x] 3.0 Move the lint step after the APK build and both publish steps, so a
      lint failure marks the run red but cannot withhold a build. Do this
      first: it is what makes 3.3 safe to attempt at all.
- [x] 3.1 Add `detekt-design-system.yml`: `maxIssues: 0`, `validation: true`,
      every built-in ruleset explicitly `active: false`, `sky-design-system`
      active.
- [x] 3.2 Register a `detektDesignSystem` Detekt task in `app/build.gradle.kts`
      pointing at it, emitting a markdown report; run it in CI ahead of the
      broad `detekt` step, and append the report to `$GITHUB_STEP_SUMMARY` on
      failure so a violation states its fix on the run page.
> **Blocked first by a build break of the same family.** Registering the
> `detektDesignSystem` task in 3.2 used `buildUponDefaultConfig.set(false)`,
> but on the `Detekt` *task* type that property is a plain `Boolean`, not a
> Gradle `Property`. The call did not resolve and Kotlin reported it against
> `StringBuilder.set`. That failed build-script compilation, so *every* Gradle
> task failed: run 75 skipped the APK build, both publish steps and the
> design-system gate itself. No APK had published since run 74. Fixed by plain
> assignment; run 76 green with every step running. Note that no step
> reordering could have prevented this — `assembleDebug` fails too when the
> script does not compile.

- [x] 3.3 Set `config.validation: true` in `detekt.yml`. **Highest-risk step:**
      this is what caused the original three-week outage. It is now survivable
      because of 3.0, and it doubles as proof the ruleset loads — validation is
      precisely what rejects an unknown `sky-design-system` block.
      *Required fixing four keys that had been silently dead — accepted while
      validation was off and never applied: `empty:` → `empty-blocks:`,
      `CyclomaticComplexity` → `CyclomaticComplexMethod`, `style/FinalNewline`
      (a `formatting` ktlint rule, not on the classpath) and
      `naming/PropertyNaming` (no such rule) both removed. Every remaining
      ruleset, rule and property was checked against detekt 1.23.6's bundled
      `default-detekt-config.yml` before pushing. Also dropped `build.weights`,
      which only scaled counts against the soft allowance.*
- [x] 3.4 Push and confirm each run is green **and** the APK built and
      published. If validation rejects a stale key, fix the key rather than
      turning validation back off.
      *Run 77: green, including the broad `detekt` step now running under
      validation. APK built and both publish steps ran.*

## 4. Verify enforcement is real

> Zero findings means a green run is **not** evidence the rule ran. 4.1 as
> originally written cannot be satisfied — there is no rule id to find in a
> clean report — so 4.2 carries the whole proof.

- [x] 4.1 Confirm the ruleset loads, via 3.3: config validation accepts the
      `sky-design-system` block, which it can only do if the provider is
      registered.
      *Confirmed in runs 76 and 77 — both configs carry `validation: true` and
      a `sky-design-system` block, and both passed. Incidentally establishes
      that a plugin-provided ruleset needs no `config/config.yml` resource in
      its jar to satisfy validation; `detektPlugins` registration is enough.*
- [x] 4.2 Verify a new violation is actually caught: temporarily introduce a
      hardcoded colour in a UI file; confirm `detektDesignSystem` fails on
      `SkyPaletteUsage`, that the run summary names the token to use, and that
      the APK still published despite the red run. Then remove it. Do not
      leave it committed.
      *Run 78 (`5c6f4c3`), reverted in `e83c82e`:*
      ```
      PlayerScreen.kt:96:50: Color(0xFF0B69F5) is SkyPalette.Accent.
        Replace the literal with the token: SkyPalette.Accent. [SkyPaletteUsage]
      > Analysis failed with 1 weighted issues.
      ```
      *"Check design system compliance" failed, "Report design system
      violations" ran, and "Build debug APK" plus both publish steps
      succeeded — all four at once. Deviated from the literal wording by using
      an unused property rather than changing a rendered colour: the point of
      the test is that a red run still publishes an APK, and that APK is what
      the in-app updater installs on the family's devices, so changing a real
      colour would have shipped a visible regression to real phones. Run 79
      confirms green after the revert.*

## 5. Correct the documentation

- [x] 5.1 Update `docs/DESIGN_SYSTEM_ENFORCEMENT.md` so every mechanism it
      lists is either running in CI or explicitly marked as optional/not
      implemented. Pre-commit is not run by CI and needs local installation —
      say so.
      *It already labelled each layer honestly; what it lacked was evidence,
      which is its own stated standard. Added the run-78 proof and its actual
      failure text, why the proof was needed at all, the fact that the gate had
      never executed before run 76, and the four dead keys with the habit that
      catches them. Corrected the sample message, which omitted the token name
      the rule really emits.*
- [x] 5.2 Update the enforcement note in `.claude/rules/design-system.md`,
      which currently states the rules are not machine-checked.
      *Done, and `CLAUDE.md` carried the same stale claim — both now say colour
      is enforced while spacing, typography, motion and component reuse are
      not, rather than implying the whole design system is checked.*
- [x] 5.3 Update `openspec/specs/build-and-release/spec.md` by archiving this
      change, so the main spec reflects the new requirements.
