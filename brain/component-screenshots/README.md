# Screenshot Testing

What Roborazzi actually captures, and what CI actually does with it. An
earlier version of this document described PR comment diffs, per-screen
naming, and dark/tablet variants that were never built, and screens (Movies,
Series, Player, TV Browse) that were never captured — aspirational content
that made the mechanism look stronger than it was. This version describes
only what runs.

## What's captured today

`skyline-iptv/app/src/test/java/com/denham/skyline/ui/ScreenshotTests.kt`
renders a gallery of design-system components (buttons, badges, cards) at a
phone qualifier and a TV qualifier, producing:

```
docs/skyline-screenshots/phone_components.png
docs/skyline-screenshots/tv_components.png
```

This is component-level coverage, not per-screen coverage — it answers "do
these tokens and primitives look right in isolation," not "does the Home
screen, as actually composed, look right." Per-screen coverage is being
added; see `openspec/changes/screenshot-regression-testing/` (or, once
archived, `openspec/specs/screenshot-testing/spec.md`) for what's covered
and what's deliberately excluded (the player screen — its video surface
isn't meaningfully renderable under Robolectric).

## What CI does with it

`.github/workflows/build-skyline-apk.yml` runs `verifyRoborazziDebug` by
default: it fails the run if a covered screen's rendered output no longer
matches its committed golden PNG. This is a real gate — a visual regression
turns the run red — not documentation.

To update the goldens after a deliberate visual change, trigger the
workflow manually (`workflow_dispatch`) with `record_screenshots` checked.
That runs `recordRoborazziDebug` instead, overwriting the committed PNGs
with the current render. A normal push never records; it only verifies.

Before this, CI only ever ran record mode — every push silently overwrote
the goldens regardless of whether anything had changed, so no visual
regression had ever once failed a build.

## Reviewing a change

```bash
git diff --stat -- docs/skyline-screenshots/*.png   # which images changed
open docs/skyline-screenshots/phone_components.png  # macOS; view directly
```

A changed PNG in a diff is expected only when the commit intentionally
records new goldens. Outside of that, the PNGs shouldn't move — if one does
and the commit wasn't a deliberate `record_screenshots` run, that's the gate
having done its job during CI, not something to review after the fact; the
run failed and the PNGs weren't updated.

## See also

- CI config: `.github/workflows/build-skyline-apk.yml`
- Design system: `docs/SKY_DESIGN_SYSTEM.md`
- Design-system enforcement (a different, separately-gated mechanism):
  `docs/DESIGN_SYSTEM_ENFORCEMENT.md`
