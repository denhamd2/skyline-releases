# Design Review — Issue Checklist

**For:** Developer implementing review findings  
**From:** Design Review (August 2026)  
**Scope:** HomeScreen.kt, GuideScreen.kt

---

## Critical Issues (P1 — Do Before Release)

### [P1-1] TV Focus Treatment Missing on HomeScreen
- **File:** `skyline-iptv/app/src/main/java/com/denham/skyline/ui/home/HomeScreen.kt`
- **Components Affected:**
  - ChannelCard (all rails)
  - PosterCard (film/series rails)
  - FixtureCard (football section)
  - FilterChip (family member selector)
  - LiveNowRow items
  - ContinueWatchingCard
- **Fix Required:** Wrap each interactive component in `tvClickable()` modifier
  - Expected behaviour: 1.04x scale + white outline (3dp) + 140ms ease-out animation on D-pad focus
  - Reference: `tv/TvComponents.kt` → `Modifier.tvClickable()`
- **Test:** 
  - [ ] D-pad navigation works (left/right/up/down)
  - [ ] Focus ring visible on all interactive elements
  - [ ] Scale animation smooth (140ms)
  - [ ] Outline colour white (3dp)

### [P1-2] TV Focus Treatment Missing on GuideScreen
- **File:** `skyline-iptv/app/src/main/java/com/denham/skyline/ui/guide/GuideScreen.kt`
- **Components Affected:**
  - Channel rows (frozen column + programme blocks)
  - Day filter chips
  - Category filter chips
  - Programme blocks (clickable areas)
- **Fix Required:** Apply `tvClickable()` to all interactive elements
- **Additional:** Consider increasing CHANNEL_COL width from 108.dp to 140.dp on large screens
- **Test:**
  - [ ] D-pad focus works on channel rows
  - [ ] D-pad focus works on filter chips
  - [ ] Focus ring visible everywhere
  - [ ] No layout shift on focus change

---

## High Priority Issues (P2 — Fix Soon)

### [P2-1] Rail Item Spacing Off-Grid
- **File:** `skyline-iptv/app/src/main/java/com/denham/skyline/ui/home/HomeScreen.kt`
- **Location:** HomeScreen() composable, Rail definition (line ~116 in Components.kt)
- **Current:** `Arrangement.spacedBy(10.dp)`
- **Change to:** `Arrangement.spacedBy(SkySpacing.m)` (12.dp)
- **Reason:** 8-point grid alignment
- **Impact:** Low visual, high consistency
- **Test:** [ ] Spacing visually even between rail cards

### [P2-2] Continue Watching Card Internal Padding Off-Grid
- **File:** `skyline-iptv/app/src/main/java/com/denham/skyline/ui/home/HomeScreen.kt`
- **Location:** ContinueWatchingCard() composable, Column padding (line ~970)
- **Current:** `.padding(14.dp)`
- **Change to:** `SkySpacing.l` (16.dp)
- **Reason:** 8-point grid alignment
- **Impact:** Minor spacing adjustment
- **Test:** [ ] Padding visually consistent with other cards

### [P2-3] GuideScreen Header Padding Off-Grid
- **File:** `skyline-iptv/app/src/main/java/com/denham/skyline/ui/guide/GuideScreen.kt`
- **Location:** GuideScreen() composable, Box padding (line ~194)
- **Current:** `padding(vertical = 10.dp)`
- **Change to:** `padding(vertical = SkySpacing.s)` (8.dp)
- **Reason:** 8-point grid alignment
- **Impact:** Minor spacing change
- **Test:** [ ] Header spacing matches other sections

### [P2-4] GuideScreen Category Filter Padding Off-Grid
- **File:** `skyline-iptv/app/src/main/java/com/denham/skyline/ui/guide/GuideScreen.kt`
- **Location:** GuideScreen() composable, LazyRow modifier (line ~248)
- **Current:** `modifier = Modifier.padding(top = 6.dp)`
- **Change to:** `modifier = Modifier.padding(top = SkySpacing.s)` (8.dp)
- **Reason:** 8-point grid alignment
- **Impact:** Minor spacing
- **Test:** [ ] Visual alignment improved

### [P2-5] GuideScreen Programme Block Corner Radius Off-Standard
- **File:** `skyline-iptv/app/src/main/java/com/denham/skyline/ui/guide/GuideScreen.kt`
- **Location:** GuideGrid() composable, Box clip modifier (line ~412)
- **Current:** `.clip(RoundedCornerShape(6.dp))`
- **Change to:** `.clip(RoundedCornerShape(SkyRadius.chip))` (8.dp)
- **Reason:** Consistent with design system corners
- **Impact:** Minor visual consistency
- **Test:** [ ] Corner radius matches chip radius throughout app

### [P2-6] GuideScreen Off-Grid Internal Cell Padding
- **File:** `skyline-iptv/app/src/main/java/com/denham/skyline/ui/guide/GuideScreen.kt`
- **Location:** GuideGrid() composable, channel column and programme blocks (lines ~363, ~418)
- **Current Issues:**
  - Channel logo padding: `padding(horizontal = 6.dp, vertical = 4.dp)` (line 363)
  - Channel text padding: `padding(start = 6.dp)` (line 375)
  - Programme block padding: `padding(horizontal = 6.dp, vertical = 4.dp)` (line 418)
  - Row vertical padding: `padding(vertical = 2.dp)` (line 352)
- **Change to:**
  - Logo: `padding(horizontal = SkySpacing.s, vertical = SkySpacing.xs)` (8dp, 4dp)
  - Text: `padding(start = SkySpacing.s)` (8dp)
  - Block: `padding(horizontal = SkySpacing.s, vertical = SkySpacing.xs)` (8dp, 4dp)
  - Row: `padding(vertical = 0)` or default
- **Reason:** Consistent spacing grid
- **Impact:** Grid cell visual consistency
- **Test:** [ ] All spacing on 4dp or 8dp boundaries

### [P2-7] Verify Secondary Text Contrast (Accessibility Review)
- **Finding:** TextSecondary (#8FA0B5) on Surface (#0E1520) = 4.2:1 contrast (WCAG AA, not AAA)
- **Action:** 
  - [ ] Audit all secondary text usage
  - [ ] Test with WAVE or Axe accessibility tool
  - [ ] Collect user feedback on readability
  - [ ] If issues found, consider:
    - Using TextPrimary for critical secondary info
    - Increasing TextSecondary lightness
    - Adding visual separators
- **Files:** HomeScreen.kt (subtitle text), GuideScreen.kt (metadata)

---

## Medium Priority Issues (P3 — Before Next Release)

### [P3-1] TV Layout Responsiveness
- **Issue:** GuideScreen CHANNEL_COL width (108.dp) is tight on tablets/TV
- **Fix:** Create adaptive width based on screen size
  ```kotlin
  val CHANNEL_COL = if (isTV) 140.dp else 108.dp
  ```
- **Alternative:** Use max(108.dp, screenWidth * 0.25f) logic
- **Test:**
  - [ ] Channel column readable on 7" tablet
  - [ ] Channel column readable on 10" tablet
  - [ ] Channel column readable on TV (40"+ display)

### [P3-2] TV Text Size Scaling
- **Issue:** Body text (16sp) may be too small for 10-foot viewing
- **Fix:** Increase text sizes on TV displays
  - Consider 18sp for body on TV vs 16sp on mobile
  - Consider 26sp for headings on TV vs 20sp on mobile
- **Test:**
  - [ ] Text readable from 10 feet on TV
  - [ ] No wrapping issues with larger text

### [P3-3] Document TV Focus Treatment in Component Library
- **Action:** Update `docs/COMPONENT_LIBRARY.md` with TV focus requirements
  - Add section: "TV Focus Implementation"
  - List all components that need tvClickable()
  - Provide code examples
  - Include animation spec (1.04x scale, white outline 3dp, 140ms)
- **Test:** [ ] Documentation is accurate and usable

---

## Low Priority Issues (P4 — Polish)

### [P4-1] Ensure Consistent Corner Radius
- **Audit:** Search for hardcoded RoundedCornerShape values
- **Expected:** All should use SkyRadius tokens
- **Example:** `RoundedCornerShape(6.dp)` should be `RoundedCornerShape(SkyRadius.chip)`
- **Files:** Components.kt, HomeScreen.kt, GuideScreen.kt

### [P4-2] Create TV Focus State Mockups
- **Action:** Generate Figma mockups showing focused states on TV
  - Guide grid with focused channel row (outline + scale)
  - Home screen with focused rail card
  - Include before/after comparison
- **Purpose:** Reference for future TV screen designs

---

## Testing Checklist

### Before Marking Complete
- [ ] No hardcoded colours (run `./gradlew detektDesignSystem`)
- [ ] All spacing on 8-point grid (or 4dp for xs)
- [ ] All text uses MaterialTheme.typography.*
- [ ] All corners use SkyRadius tokens
- [ ] TV focus implemented (tvClickable on all interactive elements)
- [ ] D-pad navigation works (up/down/left/right)
- [ ] Focus ring visible on all interactive elements
- [ ] Contrast ratios verified (WCAG AA minimum, AAA preferred)
- [ ] Touch targets ≥48dp on mobile
- [ ] No layout shifts on focus change
- [ ] Animations smooth (no jank)
- [ ] Screenshots captured for all states
- [ ] Accessibility scan completed (WAVE/Axe)

### Device Testing Required
- [ ] Mobile portrait (phone, small)
- [ ] Mobile landscape (phone, wide)
- [ ] Tablet portrait (7-10 inches)
- [ ] Tablet landscape (7-10 inches)
- [ ] TV landscape (40+ inches)
- [ ] Test with D-pad navigation only (no touch)

---

## Files Modified

| File | Changes | Scope |
|------|---------|-------|
| HomeScreen.kt | Rail spacing, Continue Watching padding, TV focus | Major |
| GuideScreen.kt | Header/category padding, programme radius, cell padding, TV focus | Major |
| Components.kt | (No changes required — library is correct) | — |
| Motion.kt | (No changes required — animations are correct) | — |
| Theme.kt | (No changes required) | — |

---

## Estimated Effort

| Priority | Task | Effort |
|----------|------|--------|
| P1-1 | HomeScreen TV focus | 4-6 hours |
| P1-2 | GuideScreen TV focus | 4-6 hours |
| P2-1 to P2-6 | Spacing fixes | 2-3 hours |
| P2-7 | Contrast audit | 2-3 hours |
| P3-1 to P3-3 | TV responsiveness + docs | 6-8 hours |
| P4-1, P4-2 | Polish | 2-3 hours |
| **Total** | | **22-29 hours** |

---

## Sign-Off

**Design Review Completed:** August 13, 2026  
**Reviewed by:** UX/UI Design Agent  
**Status:** Ready for developer assignment  
**Expected Implementation Timeline:** 1-2 sprints

For questions or clarifications, refer to the full design review in `design/full-design-review-home-epg.md`.
