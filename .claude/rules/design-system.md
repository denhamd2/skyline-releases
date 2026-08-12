---
paths:
  - "skyline-iptv/**/*.kt"
---

# Sky design system

Applies to all Skyline UI code. Full reference: `brain/design-system/SKY_DESIGN_SYSTEM.md`
and `brain/design-system/COMPONENT_LIBRARY.md`. Read the relevant section before adding or
restyling a component.

**Colour is machine-enforced.** `./gradlew detektDesignSystem` fails on any
`Color(0x…)` literal in UI code, at zero tolerance, and names the `SkyPalette`
token to use. It runs in CI after the APK publishes, so a violation marks the
run red without withholding a build. Spacing, typography, motion and
component reuse are **not** machine-checked — those rules were removed as
unusable and are reviewed by eye. See `brain/design-system/DESIGN_SYSTEM_ENFORCEMENT.md`.

## Tokens are the source of truth

`ui/theme/Theme.kt` defines `SkyPalette`, `SkySpacing` and `SkyRadius`. Use
them. Do not write raw hex colours, off-grid dp values, or hardcoded
`fontSize` in UI code.

- Colour → `SkyPalette.*` (e.g. `SkyPalette.Accent`, not `Color(0xFF0B69F5)`)
- Spacing → 8-point grid (`SkySpacing.s/m/l/xl`)
- Corners → `SkyRadius.chip/card/hero`
- Type → `MaterialTheme.typography.*`

## Reuse before writing

Check `ui/components/` first. `Rail`, `SectionHeader`, `ChannelCard`,
`PosterCard`, `ArtworkImage`, `PillButton` and the motion helpers in
`Motion.kt` already exist. A new component that duplicates one of these is a
defect, not a feature.

Follow the established pattern for repeated UI. Filter chips use
`FilterChip` with `FilterChipDefaults.filterChipColors(...)` bound to
`SkyPalette` — see `ui/live/LiveScreen.kt`. Match it rather than inventing a
new chip style.

## Motion

- Enter: fade + slide-up, ease-out, staggered via `Modifier.enterReveal(delay)`
- Screen transitions: 200ms crossfade
- TV focus: 1.04 scale + white outline, 140ms

## TV vs phone

TV is a 10-foot, D-pad interface — not the phone layout scaled up. Larger
targets, visible focus states, and no reliance on touch affordances. A
component used on both needs its focus behaviour checked, not assumed.

<!-- Enforcement note for maintainers: colour is machine-checked. The
SkyPaletteUsage rule in skyline-iptv/detekt-rules/ runs on every push as the
detektDesignSystem task at zero tolerance, and names the token to use. Spacing,
typography, motion and component reuse are NOT machine-checked -- those rules
were written against a detekt API that does not exist and were removed, so for
them this file and brain/design-system/SKY_DESIGN_SYSTEM.md remain the only guardrail. See
brain/design-system/DESIGN_SYSTEM_ENFORCEMENT.md. -->
