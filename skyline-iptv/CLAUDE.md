# Skyline — working agreement

Sky-Go-styled IPTV client for Android phone and TV. App source is in
`skyline-iptv/`. This repo also contains unrelated PM tooling and docs.

## Spec-driven development (default workflow)

This project uses OpenSpec. Feature work and behaviour changes start with a
spec, not with code.

1. `openspec` propose — create the change (proposal, design, delta specs, tasks)
2. Get the proposal agreed before writing code
3. Apply — implement against the tasks
4. Archive — fold the delta specs into `openspec/specs/`

Invoke the OpenSpec skills yourself; do not wait to be asked. If a request
is a new feature or a change in behaviour, propose first and say so.

Main specs live in `openspec/specs/<capability>/spec.md`. Read the relevant
one before changing that area, and treat it as authoritative. Where a spec
records *why* an invariant exists, that reason is usually a production
failure — do not regress it.

**When to skip the workflow** (do the work directly, still record it):

- Fixing a broken build or failing CI
- A one-line bug fix with no behaviour change
- Anything the user explicitly asks you to just do

If in doubt, ask rather than silently skipping it.

## Verification — CI is the only compiler

There is no device or emulator here, and the Android Gradle Plugin cannot be
resolved in this environment, so **the app cannot be compiled or run
locally**. Gradle will fail on plugin resolution; that is expected, not a
bug to fix.

Therefore:

- A push is not "done". Wait for the workflow run and confirm it is green.
- Confirm the APK build and publish steps actually **ran** rather than were
  skipped — a failing early step silently skips them.
- Never report a change as working on device. Say what was verified (it
  compiled, tests passed) and what was not.

This matters: CI was silently broken for three weeks while runs still
appeared to finish, and multiple changes were reported as shipped while
failing to compile.

## Design

Design rules load automatically when working on `skyline-iptv/**/*.kt` — see
`.claude/rules/design-system.md`.

Colour is machine-enforced: `detektDesignSystem` fails on any `Color(0x…)`
literal in UI code at zero tolerance and names the `SkyPalette` token to use.
Spacing, typography, motion and component reuse are not — those rules were
removed as unusable, so they are reviewed by eye. `brain/design-system/DESIGN_SYSTEM_ENFORCEMENT.md`
labels every mechanism Enforced, Optional or Not implemented; keep it honest,
because a documented check that does not run is worse than no check.

## Distribution constraints

- Source repo is **private**; APKs are mirrored to the **public**
  `denhamd2/skyline-releases` so the in-app updater can fetch without
  credentials.
- **Nothing secret may be compiled into the APK.** It is publicly
  downloadable and trivially unpacked. If a feature needs a third-party
  credential, scope it so exposure is capped (e.g. an API key restricted to
  one API), or design it not to need the secret on-device.
- Debug signing uses the committed keystore. Do not remove it: ephemeral CI
  runners would otherwise generate a new key per build and Android would
  refuse to install one build over another.
