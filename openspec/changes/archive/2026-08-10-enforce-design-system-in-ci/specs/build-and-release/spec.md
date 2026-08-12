## ADDED Requirements

### Requirement: Design-system violations are machine-checked

CI SHALL run the project's custom design-system rules against the app
sources, so that the conventions they cover are checked mechanically rather
than by review alone.

The rules previously existed as source files that no build script referenced,
so they never compiled or ran. Documentation asserted an enforcement pipeline
that was not in effect.

Scope narrowed during implementation. Of the six rule files, five had been
written against a detekt API that does not exist and had never compiled;
colour-token usage is the one rule that survives. Spacing, typography,
animation duration and component reuse are therefore *not* machine-checked,
and documentation must not claim otherwise until they are reimplemented.

#### Scenario: A change introduces a hardcoded colour

- **WHEN** UI code adds a raw colour literal instead of a palette token
- **THEN** the lint step reports it against the design-system rules

#### Scenario: A change conforms to the design system

- **WHEN** UI code uses palette, spacing and typography tokens
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

### Requirement: Design-system findings are held separately from code debt

Design-system rules SHALL be evaluated against their own zero-tolerance
threshold, separately from the general-purpose lint rules and their allowance
for pre-existing debt.

This replaces a requirement to introduce the rules against a recorded
baseline. That requirement's stated justification was that the volume of
existing design-system violations was unknown and might be large enough to
stop builds reaching devices. The first run that executed the rules disproved
it: they report zero findings, because the only hardcoded colours in the app
are the token definitions themselves, which are excluded. There is nothing to
record, so no baseline exists.

The general rules are a different matter — they report several hundred
findings accumulated while the gate was broken. Holding both to one threshold
would mean either absorbing new design violations into that allowance or
failing a build over an unrelated magic number.

#### Scenario: Pre-existing general lint debt

- **WHEN** the codebase carries findings from the general-purpose rules
- **THEN** they are counted against their own allowance and do not fail the
  build

#### Scenario: New design-system violation is introduced

- **WHEN** a change introduces any design-system violation
- **THEN** the build fails, rather than the violation being absorbed by a
  global issue allowance

### Requirement: Detekt configuration is validated

Detekt SHALL run with configuration validation enabled.

Validation was disabled to work around a configuration block referencing a
ruleset that could never load. With validation off, a future mistyped or
stale configuration entry passes silently. Validation is safe once every
referenced ruleset is actually registered.

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
having been done. Enforcement is worth having; withholding the artefact is
not the mechanism by which it should be enforced here, where the same person
writes the code and reads the runs.

#### Scenario: Lint fails

- **WHEN** a lint step reports a violation or its configuration is rejected
- **THEN** the APK has already been built and published
- **AND** the run is reported as failed

### Requirement: Documentation reflects the enforcement that runs

Documentation describing design-system enforcement SHALL describe only
mechanisms that are actually in effect, and SHALL mark anything aspirational
as such.

`DESIGN_SYSTEM_ENFORCEMENT.md` described pre-commit hooks and IDE integration
that CI never invokes, which made enforcement appear stronger than it was.

#### Scenario: Reading the enforcement documentation

- **WHEN** a contributor reads the enforcement documentation
- **THEN** each mechanism listed is either running in CI, or explicitly
  identified as optional or not yet implemented
