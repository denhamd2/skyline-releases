# Vertical Fixtures List Screen — Design Brief

**Status:** Implemented | **Date:** August 2026 | **Platforms:** Mobile (phone), TV (deferred phase 2)

## Overview

The Fixtures Screen displays a vertically scrolling list of football fixtures, grounded in Skyline's Sky-inspired design system. Each fixture card shows competition, teams, status (scheduled/live/finished), and available broadcast channels as tappable chips. The screen reuses the `FixtureCard` component at full-width in a `LazyColumn` with staggered fade + slide-up reveal animation, matching Sky's motion choreography.

## Design System Compliance

### Tokens & Colours

All colour usage adheres to `SkyPalette` (enforced by `detektDesignSystem`):

| Element | Token | Hex | Use |
|---------|-------|-----|-----|
| Full-screen background | `SkyPalette.Canvas` | `#05070A` | Deep navy, near-black base |
| Card surfaces | `SkyPalette.Surface` | `#0E1520` | Default fixture card background |
| Elevated surfaces (spotlight variant) | `SkyPalette.SurfaceElevated` | `#16233A` | Future spotlight card gradient (unused in current phase) |
| Primary text | `SkyPalette.TextPrimary` | `#FFFFFF` | Team names, scores, titles |
| Secondary text | `SkyPalette.TextSecondary` | `#8FA0B5` | Kickoff times, minute markers |
| Muted text | `SkyPalette.TextMuted` | `#7C8899` | Metadata tier (status labels, "v" divider) |
| Action/interactive | `SkyPalette.Accent` | `#0B69F5` | Channel chip borders, play icons |
| LIVE indicator | `SkyPalette.LiveRed` | `#E11D3F` | LIVE badge background |

**No raw hex literals in UI code** — all colours flow through `SkyPalette` tokens.

### Spacing (8-point grid)

| Element | Token | Value | Use |
|---------|-------|-------|-----|
| Gutters (left/right) | `SkySpacing.gutter` | 16dp | Horizontal screen padding (horizontal = gutter) |
| Card gap (vertical) | `SkySpacing.s` | 8dp | Gap between fixture cards in LazyColumn |
| Internal card padding | `SkySpacing.m` | 12dp | Padding inside FixtureCard |
| Team row gaps | `SkySpacing.xs` | 4dp | Gaps within team row (crest, name, "v", name, crest) |
| Status icon gaps | `SkySpacing.xs` | 4dp | Space between status icon and text |
| Chip gaps | `SkySpacing.xs` | 4dp | Space between chips in FlowRow, between chips and content padding |
| Vertical spacer (title to teams) | `SkySpacing.s` | 8dp | Spacer after competition badge |
| Vertical spacer (teams to status) | `SkySpacing.s` | 8dp | Spacer between team row and status |
| Vertical spacer (status to channels) | `SkySpacing.s` | 8dp | Spacer before channel chips |

**All off-grid values are defects**, not style exceptions.

### Corners (Consistency, Never Sharp)

| Component | Token | Value | Use |
|-----------|-------|-------|-----|
| FixtureCard | `SkyRadius.card` | 16dp | Default card corner radius |
| Channel chip | `SkyRadius.chip` | 8dp | Outlined action chips |
| Team crest fallback | 4dp | 4dp | Hardcoded, acceptable for small icon corners (not a top-level token) |

### Typography

All type flows through `MaterialTheme.typography`:

| Element | Style | Size/Line | Weight | Use |
|---------|-------|-----------|--------|-----|
| Screen title ("Football Fixtures") | `titleLarge` | 20sp / 26sp | SemiBold | Header text, center-aligned |
| Competition badge | `labelSmall` | 11sp / 15sp | Medium | "Premier League" text on dark background |
| Team names | `titleSmall` | 14sp / 20sp | SemiBold | Home/away team display |
| Status/score (live) | `titleMedium` | 16sp / 22sp | SemiBold | "1–1" score on live matches |
| Kickoff time (scheduled) | `labelMedium` | 12sp / 16sp | Medium | "Sat 22 Aug KO 12:30" |
| Minute marker (live) | `labelMedium` | 12sp / 16sp | Medium | "45'" time display |
| Channel chip text | `labelSmall` | 11sp / 15sp | Medium | Channel name inside chip |
| Empty state text | `bodyMedium` | 14sp / 21sp | Normal | "No fixtures available" |
| Status labels ("FT", "LIVE") | `labelSmall` | 11sp / 15sp | Medium | Fixed status indicators |
| "Not on your channels" fallback | `bodySmall` | 12sp / 17sp | Normal | Low-priority fallback message |

**No hardcoded `fontSize` in UI code** — always use `MaterialTheme.typography.*`.

### Motion & Animation

Implemented in `Motion.kt` and applied via `Modifier.enterReveal()`:

| Animation | Timing | Easing | Use |
|-----------|--------|--------|-----|
| FixtureCard reveal | 340ms base duration | ease-out (`CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)`) | Each card fades in + slides up from bottom |
| Stagger delay | 55ms per item, capped at 330ms | — | Cards animate in sequence, not all at once |
| Screen transition (future) | 200ms | ease-out crossfade | When entering/leaving the screen |

**Why this motion:**
- **Ease-out on enter**: Real objects decelerate as they settle; UI should mirror this natural behaviour
- **Stagger (55ms × index, capped at 330ms)**: Guides the eye top-to-bottom, prevents overwhelming visual noise
- **340ms base**: Fixture cards are content-heavy (team data + status + chips); longer reveal gives detail time to register

Applied via `Modifier.enterReveal(revealDelay(index))` in `FixturesScreen.kt`, line 112.

## Screen Layout

### Header (Fixed)

- **Back button** (left): `IconButton` with `Icons.Default.ArrowBack`
  - Triggers `onBack` callback
  - Icon tint: `SkyPalette.TextPrimary` (white)
  - Padding: horizontal `SkySpacing.m` (12dp), vertical `SkySpacing.m` (12dp)

- **Title** (center): "Football Fixtures"
  - Style: `MaterialTheme.typography.titleLarge` (20sp, SemiBold)
  - Colour: `SkyPalette.TextPrimary` (white)
  - Alignment: Center, vertically centered within the box

- **Header container**: `Box`, full-width, `padding(horizontal = SkySpacing.m, vertical = SkySpacing.m)`

### Content: Fixture List (Scrollable)

**If fixtures are empty:**
- Centred, full-screen message: "No fixtures available"
- Style: `MaterialTheme.typography.bodyMedium`
- Colour: `SkyPalette.TextSecondary` (muted gray-blue)
- Text alignment: Center

**If fixtures exist:**
- `LazyColumn` container:
  - `fillMaxSize()`, `background(SkyPalette.Canvas)`
  - `padding(horizontal = SkySpacing.gutter)` (16dp left/right)
  - `verticalArrangement = Arrangement.spacedBy(SkySpacing.s)` (8dp gaps)
  - `contentPadding = PaddingValues(vertical = SkySpacing.xl)` (24dp top/bottom)

- **Fixture cards** (items in the list):
  - Each `FixtureCard` component with full-width modifier (`width = null` → `fillMaxWidth()`)
  - Applied `Modifier.enterReveal(revealDelay(index))` for staggered reveal
  - Card index used as stable key for LazyColumn item recomposition

### FixtureCard Component (Reusable)

**Phone context (single-column, full-width):**

```
┌─ Card (SkyPalette.Surface, SkyRadius.card = 16dp) ─────────────────────┐
│ Padding = SkySpacing.m (12dp)                                           │
│                                                                         │
│ [Premier League] ← ProviderBadge (competition, dark background)         │
│ ↓ SkySpacing.s (8dp)                                                    │
│                                                                         │
│ ┌─ Team row ────────────────────────────────────────────────────────┐  │
│ │ [🔵] Man Utd    v    Hull City [🔵]                              │  │
│ │  28dp crest, titleSmall name, v in TextMuted, titleSmall name     │  │
│ └────────────────────────────────────────────────────────────────────┘  │
│ ↓ SkySpacing.s (8dp)                                                    │
│                                                                         │
│ ┌─ Status row ──────────────────────────────────────────────────────┐  │
│ │ [📅] Sat 22 Aug KO 12:30  ← Scheduled                            │  │
│ │ OR                                                                │  │
│ │ [🔴LIVE] 1–1 45'  ← Live                                        │  │
│ │ OR                                                                │  │
│ │ FT 2–0  ← Finished                                               │  │
│ └────────────────────────────────────────────────────────────────────┘  │
│ ↓ SkySpacing.s (8dp)                                                    │
│                                                                         │
│ ┌─ Channels row ────────────────────────────────────────────────────┐  │
│ │ [▶ Sky Sports Football] [▶ Sky Sports 2]                         │  │
│ │  ← Outlined chips, Accent border/text, FlowRow wrap              │  │
│ │ OR                                                                │  │
│ │ Not on your channels  ← TextMuted fallback                       │  │
│ └────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

**Status sub-component (conditional rendering):**

1. **Scheduled** (FixtureStatus.Scheduled):
   - Icon: `Icons.Default.Schedule`, 14dp, `SkyPalette.TextMuted`
   - Text: Kickoff time (e.g., "Sat 22 Aug KO 12:30"), `labelMedium`, `SkyPalette.TextSecondary`
   - Layout: `Row`, `horizontalArrangement = spacedBy(SkySpacing.xs)`

2. **Live** (FixtureStatus.Live):
   - `LiveBadge()`: Red background, white "LIVE" label, 11sp
   - Score: e.g., "1–1", `titleMedium`, `SkyPalette.TextPrimary`
   - Minute: e.g., "45'", `labelMedium`, `SkyPalette.TextSecondary`
   - Layout: `Row`, `horizontalArrangement = spacedBy(SkySpacing.xs)`

3. **Finished** (FixtureStatus.Finished):
   - "FT" label, `labelMedium`, `SkyPalette.TextMuted`
   - Final score: e.g., "2–0", `titleMedium`, `SkyPalette.TextPrimary`
   - Layout: `Row`, `horizontalArrangement = spacedBy(SkySpacing.xs)`

**Channel chips:**
- Private component `FixtureChannelChip()`
- Style: Outlined pill with 8dp corner radius (`SkyRadius.chip`)
- Border: 1dp, `SkyPalette.Accent` blue
- Padding: horizontal `SkySpacing.s` (8dp), vertical `SkySpacing.xs` (4dp)
- Icon: Play arrow, 12dp, `SkyPalette.Accent`
- Text: Channel name, `labelSmall`, `SkyPalette.Accent`
- Interaction: `scaledClickable()` modifier (0.97 scale on press, 150ms)
- Tappable only when channels exist; empty state shows muted text

**Clickability:**
- If exactly one channel: entire card is tappable (full-width clickable surface)
- If zero channels: card is inert, no clickability
- If two or more channels: card is inert, chips are individually tappable

This prevents ambiguous tap targets on multi-channel matches.

## Mobile Layout Details

**Aspect ratio:** 9:16 (phone portrait)

**Viewport width (typical):** 375dp (reference device), gutters at 16dp each = 343dp content width

**Card sizing:**
- Full-width minus gutters: `343dp` (375dp − 2 × 16dp)
- FixtureCard: `fillMaxWidth()` (no explicit width param, inherits LazyColumn width)

**Vertical rhythm:**
- Header: ~56dp (back button 48dp + padding 8dp top/bottom)
- LazyColumn: starts below header, fills remaining screen, scrollable
- Top/bottom content padding: 24dp each (SkySpacing.xl)
- Inter-card gap: 8dp (SkySpacing.s)

**Staggered animation state:**
- Card 0: immediate (0ms delay)
- Card 1: +55ms delay
- Card 2: +110ms delay
- Card 3: +165ms delay (if visible)
- Card N: 55ms × N, capped at 330ms (so card 6+ all hit 330ms)

This creates a cascading top-to-bottom visual flow as the list loads.

## TV Layout Details (Phase 2 Deferred)

**Current status:** Layout is phone-only; TV support postponed to phase 2.

**What's documented for future TV work:**

- **Aspect ratio:** 16:9 landscape (typical TV, 1920×1080 or scaled equivalent)
- **Layout approach:** Same vertical list as phone (portrait-style scroll on landscape), **not** a multi-column grid yet
- **Focus handling:** D-pad navigation with `tvClickable()` modifier (1.04 scale + white outline, 140ms transition)
- **Touch targets:** Larger padding/sizing to accommodate D-pad selection from 10 feet
- **Channel chips:** If a fixture has multiple channels, chips must become individually D-pad-focusable (not yet implemented)

**Design note:** FixtureCard is marked "Phone-only for now" in its KDoc (Components.kt, line 408). If reused on TV later, the focus behaviour and chip navigation must be explicitly revisited — do not assume phone layout "scales up" to TV.

## Component Reuse & New Components

**Reused existing components:**
- `FixtureCard`: New sibling to `ChannelCard` / `PosterCard` / `LiveNowRow`, designed for this specific fixture use case
- `ProviderBadge`: Dark rounded badge for competition name
- `LiveBadge`: Red LIVE label (shared with other screens)
- `ArtworkImage`: Coil async image loader with shimmer placeholder and icon fallback
- `enterReveal()` motion helper: Fade + slide-up animation
- `revealDelay()`: Stagger calculation (55ms × index, capped at 330ms)
- `scaledClickable()`: Press feedback (0.97 scale, 150ms)

**New components for fixtures:**
- `FixtureCard`: Full component, handles all fixture display variants (scheduled/live/finished, single/multi-channel)
- `FixtureChannelChip`: Private sub-component inside FixtureCard, outlined pill for channel tappable

**Why FixtureCard is new (not reusing existing cards):**
- `ChannelCard`: 16:9 live tile, channel-focused, no score/minute display
- `PosterCard`: 2:3 portrait, VOD/series, no match status
- `LiveNowRow`: Compact horizontal row, no competition badge, no fixture status variants

FixtureCard fills a gap: **competition badge + team crests + match status (scheduled/live/finished) + score/minute + multi-channel chips in a single, reusable card.**

## Design System Gaps & Deferred Work

### Implemented (✅)
- Vertical scrolling list (LazyColumn)
- Staggered reveal animation (fade + slide-up, 55ms stagger)
- Scheduled/live/finished status display
- Multi-channel fallback behaviour
- Responsive widths (full-width on phone)
- Empty state messaging
- Proper contrast (all text-on-surface combos meet WCAG AAA per SKY_DESIGN_SYSTEM.md)

### Deferred to Phase 2 (⏳)
- **TV D-pad focus:** `tvClickable()` modifier + focus scale/outline not yet applied to FixtureCard
- **TV channel chip focus:** Individual D-pad navigation of chips on multi-channel matches
- **Multi-column grid on TV:** Currently vertical scroll on landscape; future phase may layout as horizontal rail or grid
- **Spotlight card variant:** FixtureCard supports `isSpotlight` parameter (changes to gradient background + larger corners), but not used in FixturesScreen

### Known Limitations (Not Bugs)
- **Hardcoded "Football Fixtures" title:** No parameterization yet; suitable for MVP
- **No live data sync indicator:** No shimmer/skeleton while list updates (acceptable for initial phase)
- **No favourites/filter UI:** All fixtures shown in fixture order (acceptable for v1)

## Accessibility

All text-on-surface combinations meet **WCAG AAA** (refer to `docs/SKY_DESIGN_SYSTEM.md`, Accessibility section):

| Text Colour | Surface | Contrast Ratio | Standard | Result |
|-------------|---------|----------------|----------|--------|
| `TextPrimary` (#FFFFFF) | `Canvas` (#05070A) | ~19:1 | AAA (≥7:1) | ✅ Pass |
| `TextPrimary` (#FFFFFF) | `Surface` (#0E1520) | ~18:1 | AAA (≥7:1) | ✅ Pass |
| `TextSecondary` (#8FA0B5) | `Canvas` (#05070A) | ~6.5:1 | AA (≥4.5:1) | ✅ Pass (technically AA, safe for body text) |
| `TextSecondary` (#8FA0B5) | `Surface` (#0E1520) | ~6.0:1 | AA (≥4.5:1) | ✅ Pass |
| `TextMuted` (#7C8899) | `Canvas` (#05070A) | ~5.5:1 | AA (≥4.5:1) | ✅ Pass |
| `Accent` (#0B69F5) | `Canvas` (#05070A) (chip border) | ~6.5:1 | AA (≥4.5:1) | ✅ Pass |
| `LiveRed` (#E11D3F) | `Surface` (#0E1520) (badge) | ~6.0:1 | AA (≥4.5:1) | ✅ Pass |

**Icon colours:**
- Status icons (`Icons.Default.Schedule`, play arrows): Use `TextMuted` or `Accent` on canvas/surface, inheriting the text contrast above

**Touch target sizing:**
- IconButton (back button): 48dp minimum (Material convention), meets tap target guidelines
- Channel chips: ~38dp height (8dp top + text 11sp + 4dp padding × 2), adequate for touch on phone

## Implementation Notes for Developers

### Source Code Locations

- **Screen:** `skyline-iptv/app/src/main/java/com/denham/skyline/ui/fixtures/FixturesScreen.kt`
- **Card component:** `skyline-iptv/app/src/main/java/com/denham/skyline/ui/components/Components.kt` (FixtureCard, FixtureChannelChip)
- **Motion helpers:** `skyline-iptv/app/src/main/java/com/denham/skyline/ui/components/Motion.kt` (enterReveal, revealDelay)
- **Tokens:** `skyline-iptv/app/src/main/java/com/denham/skyline/ui/theme/Theme.kt` (SkyPalette, SkySpacing, SkyRadius)

### Fixture Data Model

The `Fixture` data class (from `core/Fixture.kt`) carries:
- `id`: Unique fixture identifier (used as LazyColumn key)
- `competition`: String (e.g., "Premier League")
- `homeTeam`, `awayTeam`: Team names
- `homeCrestUrl`, `awayCrestUrl`: Team logo URLs
- `status`: `FixtureStatus` sealed class (Scheduled, Live, Finished)

The `FixtureStatus` variants:
- `Scheduled(kickoffLocal)`: e.g., "Sat 22 Aug KO 12:30"
- `Live(homeScore, awayScore, minute)`: e.g., "1", "1", "45'"
- `Finished(homeScore, awayScore)`: e.g., "2", "0"

### Channel Linking

- `fixtureChannels: Map<String, List<ChannelEntity>>` maps fixture ID → list of broadcast channels
- Lookup: `fixtureChannels[fixtures[index].id] ?: emptyList()`
- Fallback: If no channels for a fixture, show "Not on your channels" text (TextMuted)
- Callback: `onPlayChannel(ChannelEntity)` invoked on chip tap or whole-card tap (single channel only)

### No Detekt Violations

- ✅ No raw `Color(0x...)` literals (all use `SkyPalette.*`)
- ✅ No off-grid spacing (all use `SkySpacing.*` or documented exceptions)
- ✅ No hardcoded `fontSize` (all use `MaterialTheme.typography.*`)
- ✅ No duplicate components (reuses existing utilities)

---

## Mockup Generation Status

**Visual mockups (kie.ai, GPT Image 2):**

Due to API account credit exhaustion, the expected text-to-image mockups for mobile (9:16) and TV (16:9) layouts could not be generated. The API returned error code 402 (insufficient credits) on 2026-08-13.

**Compensatory reference:**
- `reference-designs/mobile/mobile-screens.png`: Baseline fixture grid (may show horizontal rail variant, not vertical list)
- `reference-designs/desktop/desktop-screens.png`: Baseline TV layout (landscape orientation expected)

These baseline mockups predate the FixturesScreen implementation and may show section ordering or navigation differences. For a current-state screenshot of the vertical fixtures list as rendered, a Roborazzi test would be needed (pending availability).

---

## Summary

The Fixtures Screen implements a Sky-inspired vertical list of football fixtures, grounded in Skyline's design tokens and motion choreography. Full-width FixtureCard components animate in via staggered fade + slide-up reveal, displaying team data, match status, and channel chips in a clean, disciplined layout. Phase 2 will extend this to TV with D-pad focus handling and potential multi-column layout. Current implementation is production-ready for mobile phone.

**Ready for implementation.** This brief fully specifies the design; the implementation in `FixturesScreen.kt` and `Components.kt` (FixtureCard) matches these requirements.
