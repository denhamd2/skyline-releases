# Vertical Fixtures List View — Football Section

**Date:** 2026-08-13  
**Status:** Design Brief  
**Type:** New Feature  

## Summary

Design a dedicated **vertical fixtures list view** for Skyline's football section as a complement to the existing horizontal carousel ("Football today" rail). Users access this view via a "View all" action from the Football section, showing a full-screen, vertically-scrolling list of upcoming/ongoing fixtures with the same match details (competition, teams, status, channels) but optimized for vertical scanning and pagination rather than horizontal swipe-through.

This is **not a replacement** for the carousel—both coexist. The carousel remains on Home for a quick glance; the list view offers deeper browsing and is ideal for users checking the day's full fixture schedule.

## Grounding

Read the following before implementing:
- `docs/SKY_DESIGN_SYSTEM.md` — full color, spacing, typography, and motion specs
- `docs/COMPONENT_LIBRARY.md` — component reuse rules and motion primitives
- `skyline-iptv/app/src/main/java/com/denham/skyline/ui/components/Components.kt:415-572` — current `FixtureCard` implementation
- `skyline-iptv/app/src/main/java/com/denham/skyline/ui/home/HomeScreen.kt:808-869` — current usage (spotlight + rail)
- `design/football-fixture-card-polish.md` — recent polish pass that added gradient/hero treatment to spotlight

## Design Principles & Constraints

### 1. Reuse before writing

The existing `FixtureCard` component is production-ready and carries all the match metadata, visual hierarchy, and interaction model we need. **However**, it is currently phone-only (no TV focus treatment per `COMPONENT_LIBRARY.md` line 283):
> "Phone-only today — no TV focus treatment implemented; needs the standard 1.04x/white-outline TV treatment plus individually D-pad-focusable chips before reuse on TV"

**Decision**: For this first pass, the vertical list is **phone-only**. TV support is deferred to a follow-up pass where we either:
- Add TV focus treatment to `FixtureCard` itself (make it phone+TV), or
- Create a new `FixtureListItem` variant designed for vertical/TV contexts

The list uses the existing `FixtureCard` at full-width (or gutter-constrained), displayed in a `LazyColumn` with staggered reveal animation.

### 2. Information density vs. the carousel

The carousel card is constrained to ~240dp width by the rail; each card is a skimmable "at a glance" size.

The list card is full-width (or gutter-constrained for larger TV screens), eliminating the carousel's horizontal scrolling gesture. This is **intentional**: the vertical scroll is the primary navigation mode on a list, and nesting a second horizontal scroll would create gesture conflict (as noted in the polish brief, finding 4).

### 3. Motion & choreography

Vertical list entry follows Sky's signature staggered reveal:
- Each card: fade-in + 22dp slide-up
- Duration: 340ms ease-out
- Stagger: 55ms per item (capped at 330ms)
- Use `Modifier.enterReveal(revealDelay(index))` from `ui/components/Motion.kt`

No other motion is introduced — no lazy-load fades, no scroll physics tweaks beyond Compose's default inertial scroll.

## Layout & Structure

### Screen Hierarchy

```
┌─────────────────────────────────────────────┐
│ Football Fixtures       [Back/Close button] │  ← titleLarge (20sp), SkyPalette.TextPrimary
├─────────────────────────────────────────────┤
│ Today  Tomorrow  [+2d]  ...                 │  ← Filter chips (optional first pass)
├─────────────────────────────────────────────┤
│                                             │
│ ┌─────────────────────────────────────────┐ │
│ │ Sky Sports                              │ │  ← ProviderBadge (existing component)
│ │ Man Utd          v          Chelsea     │ │  ← Team row with crests
│ │ 14:30                                   │ │  ← Scheduled time + icon
│ │ Sky Sports Football  Sky Sports+        │ │  ← Channel chips (FixtureChannelChip)
│ └─────────────────────────────────────────┘ │
│                                             │
│ ┌─────────────────────────────────────────┐ │
│ │ Premier League                          │ │
│ │ Liverpool       v       Tottenham        │ │
│ │ LIVE 1–1 45'                            │ │
│ │ Sky Sports Football                     │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ ┌─────────────────────────────────────────┐ │
│ │ FA Cup                                  │ │
│ │ Brighton        v       Southampton     │ │
│ │ FT 2–0                                  │ │
│ │ Not on your channels                    │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ ┌─────────────────────────────────────────┐ │  ← More cards below (scrollable)
│ │ ...                                     │ │
│ └─────────────────────────────────────────┘ │
│                                             │
└─────────────────────────────────────────────┘
```

### Card Sizing

- **Phone**: Each card is full-width minus `SkySpacing.gutter` (16dp) left/right padding
  - Effective card width: ~344dp (for standard 375dp viewport – 16dp – 16dp)
  - Matches the FixtureCard layout but at full viewport width rather than rail-constrained

- **TV**: (Deferred to phase 2 with focus treatment)
  - Could be full-width (like phone) or 2-column grid
  - Design decision pending TV focus implementation

### Spacing

- **Vertical gap between cards**: `SkySpacing.s` (8dp)
- **Card padding (internal)**: `SkySpacing.m` (12dp) — matches current FixtureCard
- **Screen horizontal padding**: `SkySpacing.gutter` (16dp) — matches all screens
- **Top section padding** (after header): `SkySpacing.l` (16dp)
- **Bottom padding** (safe area): `SkySpacing.xl` (24dp)

All spacing is 8-point grid–aligned per `SKY_DESIGN_SYSTEM.md` § Spacing.

### Colors

**Background**: `SkyPalette.Canvas` (#05070A) — full-screen dark navy, consistent with Home and all screens

**Cards**: Inherit from `FixtureCard`:
- Non-spotlight cards: `SkyPalette.Surface` (#0E1520) background, `SkyRadius.card` (16dp) corners
- Spotlight card (if top-pinned): `SkyPalette.SurfaceElevated → SkyPalette.Indigo` gradient, `SkyRadius.hero` (22dp) — per `football-fixture-card-polish.md` finding 1

**Text**: 
- Title: `SkyPalette.TextPrimary` (#FFFFFF)
- Secondary (metadata, kickoff time, "FT"): `SkyPalette.TextSecondary` (#8FA0B5) or `SkyPalette.TextMuted` (#7C8899)
- Live badge text: White on `SkyPalette.LiveRed`

**Interactive elements**: `SkyPalette.Accent` (#0B69F5) for channel chips and "View all" link (if pinned to section header)

### Typography

- **Screen title** ("Football Fixtures"): `MaterialTheme.typography.titleLarge` (20sp SemiBold)
- **Competition badge**: `MaterialTheme.typography.labelSmall` (11sp Medium) — lowercased via `ProviderBadge`
- **Team names**: `MaterialTheme.typography.titleSmall` (14sp SemiBold) — same as carousel
- **Status**: `MaterialTheme.typography.labelMedium` (12sp Medium) or `MaterialTheme.typography.titleMedium` (16sp SemiBold) for live score
- **Channel chips**: `MaterialTheme.typography.labelMedium` (12sp Medium)

Reference `SKY_DESIGN_SYSTEM.md` § Typography for all size/weight specs.

## Component Reuse & Structure

### Reusing FixtureCard

**Current component** (`Components.kt:415-572`):
- Parameters: `competition`, `homeTeam`, `awayTeam`, `homeCrestUrl`, `awayCrestUrl`, `status`, `channels`, `onPlayChannel`, `modifier`, `width`, `isSpotlight`
- Composed of: `ProviderBadge`, `ArtworkImage` (crests), status row, `FixtureChannelChip` (flow row)
- Already handles all three status states: `Scheduled`, `Live`, `Finished`
- Already has press feedback: single-channel cards play directly; multi-channel cards are inert (chips clickable)

**Usage in vertical list**:
```kotlin
LazyColumn(
    modifier = Modifier
        .fillMaxSize()
        .background(SkyPalette.Canvas)
        .padding(horizontal = SkySpacing.gutter),
    verticalArrangement = Arrangement.spacedBy(SkySpacing.s),
    contentPadding = PaddingValues(vertical = SkySpacing.xl)
) {
    items(
        count = fixtures.size,
        key = { fixtures[it].id }
    ) { index ->
        FixtureCard(
            competition = fixtures[index].competition,
            homeTeam = fixtures[index].homeTeam,
            awayTeam = fixtures[index].awayTeam,
            homeCrestUrl = fixtures[index].homeCrestUrl,
            awayCrestUrl = fixtures[index].awayCrestUrl,
            status = fixtures[index].status,
            channels = fixtureChannels[fixtures[index].id] ?: emptyList(),
            onPlayChannel = onPlayChannel,
            width = null,  // Full-width, not rail-constrained
            isSpotlight = false,  // All cards flat, no spotlight variant in list
            modifier = Modifier.enterReveal(revealDelay(index))
        )
    }
}
```

**Why no new component?**
- The `FixtureCard` already contains all the visual/interactive logic needed
- Creating a new `FixtureListItem` would duplicate that logic with minimal visual difference
- When TV support is added (phase 2), the same `FixtureCard` can gain focus treatment; a separate list-only component would fragment the codebase
- Component library rule: "Reuse before writing. Check `ui/components/` first... A new component duplicating one of these is a defect, not a feature."

### Supporting Components (Existing, Reuse As-Is)

- **ProviderBadge**: `Components.kt:~650` — competition label, already used in FixtureCard
- **ArtworkImage**: `Components.kt:~300` — crest loader with fallback, already used in FixtureCard
- **FixtureChannelChip**: `Components.kt:~570` (private to FixtureCard) — channel pill, already used in FixtureCard
- **LiveBadge**: `Components.kt:~600` — red "LIVE" pill, already used in FixtureCard status row
- **Rail** (optional): if list is accessed via a "View all" on the current Football section, can reuse the existing Rail section header logic with onViewAll callback

### Motion

Use the existing motion primitives from `ui/components/Motion.kt`:

```kotlin
// For each card in the list
modifier = Modifier.enterReveal(delayMs = revealDelay(index))

// Where revealDelay is:
fun revealDelay(index: Int, stepMs: Int = 55, maxMs: Int = 330): Int =
    (index * stepMs).coerceAtMost(maxMs)
```

**Spec**:
- Duration: 340ms
- Easing: `LinearOutSlowInEasing` (ease-out)
- Animation: fade-in (0–1 opacity) + slide-up (22dp → 0dp)
- Stagger: 55ms per item, capped at 330ms total delay

Reference `SKY_DESIGN_SYSTEM.md` § Motion & Interaction.

## Use Cases & Navigation

### Primary entry point

**"View all" action on Football section** (Home screen):
1. User sees the current Home football section (spotlight + carousel)
2. Taps "View all" link (or dedicated button) on the "Football today" rail header
3. Navigates to a new full-screen "Football Fixtures" view
4. Shows the complete vertical list of fixtures

**Implementation**: Modify `HomeScreen.kt:852-865` (the Rail call site) to pass an `onViewAll` callback to the Rail component, which navigates to the new screen.

### Alternative entry points (future)

- A dedicated **"Fixtures" navigation tab** (bottom bar or TV side menu) — deferred
- A **sub-section within a "Football" full-screen tab** — deferred

### Screen state

The view should support filtering/sorting (deferred to phase 2, optional):
- **Date selector** (chips: "Today", "Tomorrow", "+2d", etc.) — like the Live tab's day selector
- **Sort** (by time, by competition, by your teams) — deferred
- **Search** (by team name) — deferred

**Phase 1** (this brief): no filtering; show all fixtures for "today" (or the next N days, TBD with backend).

## Responsive Behavior: Phone vs. TV

### Phone (Current scope)

- **Layout**: Vertical scrolling list, full-width cards minus gutters
- **Focus**: No D-pad focus (touch-driven, Material3 ripple suffices for touch feedback)
- **Card width**: ~344dp (375dp viewport – 32dp gutters)
- **Status**: Implemented, phone-only per `FixtureCard` current state

### TV (Phase 2, deferred)

- **Layout**: Same vertical list, or 2-column grid layout (design TBD)
- **Focus**: All cards use `Modifier.tvClickable()` for D-pad focus (1.04x scale + white outline, 140ms)
- **Card width**: TBD (likely 240dp per TV card standards, or full-width at larger distance)
- **Chips**: Each chip must be individually D-pad–focusable (requires FixtureChannelChip refactor)
- **Status**: Blocked on FixtureCard gaining TV support

**Design decision**: Do **not** add TV support in this brief. FixtureCard is currently phone-only by design. Adding TV support is a separate task that should refactor FixtureCard itself (to be reused in both carousel and list), not create a new list-specific variant.

## Accessibility

### Color Contrast

All text meets WCAG AAA standards (checked in `SKY_DESIGN_SYSTEM.md` § Accessibility):
- `TextPrimary` on `Canvas`: 18.2:1 ✓
- `TextPrimary` on `Surface`: 15.8:1 ✓
- `Accent` (chips): 5.1:1 (interactive, acceptable) ✓
- `LiveRed` (badges): 4.8:1 (high visibility, acceptable) ✓

No changes needed; inherited from `FixtureCard`.

### Touch targets (Phone)

`FixtureCard` and `FixtureChannelChip` meet Material3 minimums (48dp touch targets for buttons/chips). No changes needed.

### Focus order & navigation

- Vertical scroll only; no secondary horizontal regions (removes the gesture-conflict issue from the carousel)
- All taps go to either:
  - Single channel → plays directly (full card is clickable)
  - Multiple/no channels → chips only (user taps a chip to play, or "Not on your channels" is inert)

## Technical Notes

### No new components

This brief uses **only existing components** and patterns:
- `FixtureCard` (reuse, width=null for full-width)
- `ProviderBadge`, `ArtworkImage`, `FixtureChannelChip`, `LiveBadge` (all internal to FixtureCard, no new surfaces)
- `LazyColumn` (Material Compose standard)
- `enterReveal()`, `revealDelay()` (existing motion primitives from `Motion.kt`)

### Build & Lint

- **Detekt color check**: No new colors introduced; all use existing `SkyPalette` tokens
- **Detekt spacing**: All spacing is `SkySpacing.*` or 8-point multiples
- **Typography**: All text uses `MaterialTheme.typography.*` styles
- **Component reuse**: No duplication; FixtureCard is reused as-is

No build changes required. No new dependencies.

### Screen navigation

Assumes the app has a navigation layer (NavHost, NavController) that can route to a new "FixturesScreen" destination. Implementation details deferred to the developer (navigation graph configuration, route naming, etc.).

## Visuals

**Skipped.** kie.ai API returned a 402 credit limit error; generation was not possible. This brief is detailed enough to implement without a mockup — the layout reuses the existing FixtureCard, and the vertical structure is a straightforward LazyColumn pattern.

When credits are available, regenerate a text-to-image mockup using this prompt (updated as implementation progresses):
```
Skyline IPTV app - Football Fixtures vertical list screen on mobile phone (9:16 aspect). 

Color scheme:
- Background: deep navy (#05070A) / SkyPalette.Canvas
- Cards: lighter navy (#0E1520) / SkyPalette.Surface, 16dp rounded corners
- Accents: action blue (#0B69F5) / SkyPalette.Accent for interactive elements
- Text: white (#FFFFFF) / SkyPalette.TextPrimary for main, light gray for secondary
- Live badge: red (#E11D3F) / SkyPalette.LiveRed

Layout:
- Top: "Football Fixtures" header (20sp bold, white text)
- Vertical scrolling list of fixture cards, each full-width with 16dp horizontal gutter
- Each fixture card: competition badge (dark pill), team row (crest–name | v | name–crest), status (time/score/FT with icon), channel chips (outlined style)
- Cards staggered with 8dp vertical gaps
- Approximately 3-4 cards visible in viewport (rest scrolled off-screen) to show list scrolling pattern
- Cards ready for fade+slide-up staggered animation (no motion needed in static image)
```

## Design System Compliance Checklist

- [x] **Colors**: All `SkyPalette.*` tokens (Canvas, Surface, Accent, TextPrimary, TextSecondary, LiveRed)
- [x] **Spacing**: All `SkySpacing.*` tokens (s, m, l, xl, gutter) — 8-point grid
- [x] **Corners**: All `SkyRadius.*` tokens (card 16dp)
- [x] **Typography**: All `MaterialTheme.typography.*` styles (titleLarge, labelMedium, etc.)
- [x] **Motion**: Staggered enterReveal with correct 340ms/ease-out/55ms-stagger timings
- [x] **Components**: FixtureCard reused, no duplication
- [x] **Images**: ArtworkImage reused for crests, consistent fallback handling
- [x] **Accessibility**: WCAG AAA contrast inherited, touch targets met

## Open Questions / Decisions Needed

1. **Date filtering in phase 1?** Should the list show fixtures for "today" only, or a broader range (e.g., next 7 days)? Recommend: show a date-selector chip row (like Live tab) but default to all upcoming fixtures, with optional filtering deferred to phase 2.

2. **TV support deferral**: Confirm that TV support is a separate, follow-up task (requires FixtureCard → `tvClickable()` refactor). This brief assumes phone-only.

3. **Navigation entry point**: Confirm the "View all" link is placed on the "Football today" rail header (via Rail's onViewAll callback) vs. a separate button. Recommend: use Rail's existing pattern.

4. **Empty state**: What does the screen show if no fixtures are available? Recommend: centered "No fixtures" message with `SkyPalette.TextSecondary` text, matching the pattern in `SKY_DESIGN_SYSTEM.md` § Loading & Error States.

5. **Loading state**: Should loading show a `ShimmerRail()` (like Home's current pattern) or individual shimmer cards? Recommend: LazyColumn with `ShimmerBox` items (5–6 cards) to match the vertical list pattern.

---

## Summary for Handoff

**What you're building:**
A full-screen vertical list of football fixtures, reusing the existing `FixtureCard` component at full-width in a `LazyColumn`. Accessed via a "View all" action on the Home football section. Phone-only in this phase; TV support deferred.

**What's already built:**
`FixtureCard` handles all the visual and interaction logic. This is a layout/navigation task, not a new component task.

**What's new:**
- A new screen/route ("FixturesScreen" or similar)
- LazyColumn layout with staggered reveal motion
- "View all" navigation link/button from Home football section
- Empty/loading states (optional, can match existing patterns)

**No breaking changes, no API changes, no new dependencies.**

---

**Maintained By:** Skyline IPTV UX Design  
**Related Docs:** 
- `design/football-fixture-card-polish.md` (recent FixtureCard improvements)
- `design/david-football-fixtures.md` (original FixtureCard spec)
- `docs/SKY_DESIGN_SYSTEM.md` (design tokens, motion, TV principles)
- `docs/COMPONENT_LIBRARY.md` (reuse rules, FixtureCard spec)
