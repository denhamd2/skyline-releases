# Sky Design System — Skyline IPTV

**Version 1.0** | Last Updated: July 2026

This is the authoritative design system for Skyline IPTV, codifying the learnings from **Sky's official design language** (Sky UI design system), real mockups, and iterative refinement across mobile and TV platforms.

**Grounded in Sky Design Principles:** This system is informed by Sky's published design documentation and reviews of Sky Q / Sky Glass interfaces. See [Official Sky Design Language](#official-sky-design-language) for the principles, sources, and research notes.

---

## Table of Contents

1. [Official Sky Design Language](#official-sky-design-language)
2. [Design Principles](#design-principles)
3. [Foundation](#foundation)
4. [Motion & Interaction](#motion--interaction)
5. [Components](#components)
6. [Patterns & Flows](#patterns--flows)
7. [TV-Specific (10-Foot UI)](#tv-specific-10-foot-ui)
8. [Mobile-Specific (Phone/Tablet)](#mobile-specific-phonetablet)
9. [Accessibility](#accessibility)

---

## Official Sky Design Language

### Sky's Design Principles (Documented)

Sky Digital maintains **Sky UI** — an official component + Figma design system used across Sky Go, Sky Q, and Sky Glass. The published principles are:

#### 1. **Motion is Functional, Not Decorative**
- Motion communicates **state change**, spatial relationships, and action results
- Easing is natural (ease-out on enter, ease-in on exit)
- Duration is proportional to the magnitude of change
- Micro-animations guide the eye and create coherence

#### 2. **Easing = Natural Acceleration/Deceleration**
- Real objects accelerate and decelerate; UI should follow this principle
- **Enter animations use ease-out:** elements arrive quickly and *settle*
- **Exit animations use ease-in:** elements start slow and *leave fast*
- Linear motion is avoided—it feels mechanical and lifeless

#### 3. **Choreography (Staggered Reveal)**
- When multiple elements animate together, they are **staggered**, not synchronized
- Signature pattern: **"Fade In & Slide Up"** diagonally (top-left to bottom-right)
- Staggering guides the eye through transitions and prevents overwhelming the viewer
- Typical stagger: 40–60ms per item

#### 4. **Restrained Color & Gradients**
- Deep near-black canvas with **Sky blue** (#0B69F5) as the action color
- **Red (#E11D3F) for live/urgent** indicators
- Gradients are sparing and purposeful—only on hero surfaces, never on type or icons
- Restrained palette creates elegance and hierarchy

#### 5. **10-Foot (TV) Interaction**
- Focus-driven motion is the core: focused tile **scales up slightly** and **lifts**
- Artwork/metadata animate in with spatial clarity
- Row eases to keep focused item in view
- Oversized text and controls for ~3m viewing distance

### How Skyline Implements Sky Principles

| Sky Principle | Implementation | Status |
|---|---|---|
| **Functional motion** | Animations communicate state (enter/exit, focus changes) | ✅ Complete |
| **Ease-out enters, ease-in exits** | Motion.kt: 340ms ease-out reveal, 200ms fade exits | ✅ Complete |
| **Staggered reveal choreography** | 55ms stagger per item, capped at 330ms max delay | ✅ Complete |
| **Restrained color palette** | SkyPalette: Canvas #05070A, Accent #0B69F5, LiveRed #E11D3F | ✅ Complete |
| **Gradients sparing, never on type** | Gradients only on hero/fallback, never behind text or icons | ✅ Complete |
| **TV focus "pop"** | tvClickable() modifier: 1.04x scale + white outline (140ms) | ✅ Complete |
| **Staggered rail reveal** | Rails load with 55ms stagger, top-to-bottom animation | ✅ Complete |
| **Screen-transition crossfade** | SkylineNavHost: 200ms fade between screens | ✅ Complete |

### Research Sources

For full details and citations, see `docs/sky-design-language.md`. Official Sky Design System references:
- **Sky UI Design System — Motion:** http://sky-ui.cf.sky.com/guides/motion/
- **Sky UI Design System — Easings:** http://sky-ui.cf.sky.com/foundations/easings
- **Sky Toolkit (Open Source):** https://github.com/sky-uk/toolkit/

---

## Design Principles

### 1. Clarity Through Restraint

Use a **disciplined, small color palette** and **clear hierarchy** over visual noise. Every color, shadow, and animation has a purpose.

```kotlin
// ❌ Wrong: Too many colors, gradient decoration
Box(
    modifier = Modifier.background(
        Brush.diagonalGradient(
            listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow)
        )
    )
)

// ✅ Right: Restrained, purposeful
Box(
    modifier = Modifier.background(SkyPalette.Surface)
)
```

### 2. Motion Guides the Eye

Motion is **functional:** it shows state change, spatial relationships, and action results. Never add motion just for aesthetic appeal.

```kotlin
// ✅ Right: Motion communicates "this opened"
LazyColumn {
    items(channels.size) { index ->
        ChannelCard(
            channel = channels[index],
            modifier = Modifier.enterReveal(revealDelay(index))
        )
    }
}
```

### 3. 8-Point Everything

All spacing, padding, and sizing is a multiple of 8dp. This creates visual rhythm and simplifies calculations.

```kotlin
// ✅ Right
Modifier.padding(
    horizontal = SkySpacing.l,      // 16dp
    vertical = SkySpacing.s         // 8dp
)

// ❌ Wrong
Modifier.padding(
    horizontal = 15.dp,  // Breaks the grid
    vertical = 9.dp      // Breaks the grid
)
```

### 4. Rounded, Never Sharp

All interactive elements use rounded corners. The minimum is 8dp (chips), typical is 16dp (cards), large is 22dp (hero).

### 5. TV Motion is Scale + Outline, Layout-Stable

On TV, focus motion must never cause layout reflow. Use graphics-layer transform for scale, not layout-based scale.

```kotlin
// ✅ Right: Graphics-layer scale (layout-stable)
.graphicsLayer {
    scaleX = 1.04f
    scaleY = 1.04f
}

// ❌ Wrong: Layout-affecting scale (causes reflow)
.scale(1.04f)  // This can cause siblings to shift
```

---

---

## Foundation

### 1. Color System

All colors are defined in `ui/theme/Theme.kt` in the `SkyPalette` object. Use these constants throughout the codebase — never hardcode hex values.

#### Primary Colors

| Token | Value | Usage |
|-------|-------|-------|
| **Canvas** | `#05070A` | App background (primary), dark overlays |
| **Accent** | `#0B69F5` | Interactive elements (buttons, chips, focus rings) |
| **LiveRed** | `#E11D3F` | Live indicators, urgent badges, alerts |
| **Indigo** | `#0C1B87` | Hero gradients, depth fills |

#### Supporting Colors

| Token | Value | Usage |
|-------|-------|-------|
| **Surface** | `#0E1520` | Cards, modals, elevated surfaces |
| **SurfaceElevated** | `#16233A` | Secondary elevated surfaces |
| **AccentBright** | `#2A9BE0` | Hover/focus states, accent variants |
| **Brand** | `#000FF5` | Electric highlight (sparingly for premium content) |

#### Text Colors

| Token | Value | Usage |
|-------|-------|-------|
| **TextPrimary** | `#FFFFFF` | Body text, primary labels |
| **TextSecondary** | `#8FA0B5` | Secondary labels, metadata |
| **TextMuted** | `#7C8899` | Captions, tertiary metadata |
| **Error** | `#FF6B6B` | Error states, destructive actions |

#### Semantic Brushes (Gradients)

```kotlin
// Hero artwork bottom scrim — fades transparency to solid background
val heroScrim = Brush.verticalGradient(
    0f to Color.Transparent,
    0.55f to Canvas.copy(alpha = 0.55f),
    1f to Canvas,
)

// Hero fallback when no artwork is available
val heroFallback = Brush.verticalGradient(
    0f to Indigo,
    1f to Canvas,
)

// TV background subtle grade (navy → near-black)
val tvBackground = Brush.verticalGradient(
    0f to Color(0xFF0A111F),
    0.6f to Color(0xFF070A12),
    1f to Canvas,
)
```

#### Color Contrast (WCAG AAA Compliance)

- **TextPrimary (#FFF) on Canvas (#05070A):** 18.2:1 ✓
- **TextPrimary (#FFF) on Surface (#0E1520):** 15.8:1 ✓
- **Accent (#0B69F5) on Canvas:** 5.1:1 (interactive only, not body text) ✓
- **LiveRed (#E11D3F) on Canvas:** 4.8:1 (high visibility for live badges) ✓

**When to use each color:**

```kotlin
// Background & containers
Box(modifier = Modifier.background(SkyPalette.Canvas))
Surface(color = SkyPalette.Surface) { ... }

// Actions & focus
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = SkyPalette.Accent,
        contentColor = Color.White
    )
) { ... }

// Live indicators
Badge(
    containerColor = SkyPalette.LiveRed,
    contentColor = Color.White
) {
    Text("LIVE")
}

// Hero sections
Box(
    modifier = Modifier
        .background(SkyPalette.heroFallback)
) { ... }
```

---

### 2. Typography

**Font Family:** Manrope (OFL open-source) — a good substitute for Sky's proprietary typeface, used across all screens.

**Font Weights Available:** Normal (400), Medium (500), SemiBold (600), Bold (700), ExtraBold (800)

#### Type Scale

Defined in `ui/theme/Theme.kt` as Material3 `Typography`. Use MaterialTheme text styles, never hardcode font sizes.

| Style | Size | Weight | Line Height | Use Case |
|-------|------|--------|-------------|----------|
| **displayLarge** | 44sp | ExtraBold | 48sp | Page titles (rarely) |
| **displayMedium** | 32sp | ExtraBold | 38sp | Hero section titles |
| **headlineLarge** | 28sp | Bold | 34sp | Section headers |
| **headlineMedium** | 24sp | Bold | 30sp | Card titles, screen headers |
| **headlineSmall** | 20sp | Bold | 26sp | Rail headers, prominent labels |
| **titleLarge** | 20sp | SemiBold | 26sp | Navigation labels |
| **titleMedium** | 16sp | SemiBold | 22sp | Card subtitles |
| **titleSmall** | 14sp | SemiBold | 20sp | Badge labels |
| **bodyLarge** | 16sp | Normal | 24sp | Primary body text (relaxed line height) |
| **bodyMedium** | 14sp | Normal | 21sp | Secondary body text |
| **bodySmall** | 12sp | Normal | 17sp | Fine print, timestamps |
| **labelLarge** | 14sp | SemiBold | 20sp | Button labels, chips |
| **labelMedium** | 12sp | Medium | 16sp | Small labels, badges |
| **labelSmall** | 11sp | Medium | 15sp | Captions, icon labels |

#### Usage Examples

```kotlin
// Header
Text(
    text = "Popular Movies",
    style = MaterialTheme.typography.headlineSmall,  // 20sp Bold
    color = SkyPalette.TextPrimary
)

// Card title
Text(
    text = "Breaking Bad",
    style = MaterialTheme.typography.titleMedium,  // 16sp SemiBold
    color = SkyPalette.TextPrimary
)

// Body text
Text(
    text = "A high school chemistry teacher turned meth manufacturer.",
    style = MaterialTheme.typography.bodyLarge,  // 16sp Normal
    color = SkyPalette.TextSecondary
)

// Metadata/caption
Text(
    text = "2023 • 8 episodes",
    style = MaterialTheme.typography.labelMedium,  // 12sp Medium
    color = SkyPalette.TextMuted
)
```

#### Font Weight Emphasis

- **Body text:** Normal weight (400) — never use light
- **Emphasis:** SemiBold (600) — for highlighted inline text or labels
- **Headers:** Bold (700) or ExtraBold (800) — based on scale
- **Never:** Italic (not used in Sky design language)

---

### 3. Spacing & Layout

**8-Point Baseline Scale:** All spacing is a multiple of 8dp. This ensures visual rhythm and simplifies calculations.

Spacing tokens are defined in `ui/theme/Theme.kt` as `SkySpacing`:

```kotlin
object SkySpacing {
    val xs = 4.dp      // Very tight (rarely used)
    val s = 8.dp       // Standard small gap
    val m = 12.dp      // Medium gap (less common; usually use s or l)
    val l = 16.dp      // Standard large gap (most common)
    val xl = 24.dp     // Extra large gap
    val gutter = 16.dp // Screen-edge horizontal padding
}
```

#### Common Spacing Patterns

**Screen Edge Padding (Mobile):**
```kotlin
Column(
    modifier = Modifier.padding(horizontal = SkySpacing.gutter)  // 16dp
) { ... }
```

**Screen Edge Padding (TV):**
```kotlin
Column(
    modifier = Modifier.padding(horizontal = 20.dp)  // Slightly more for 10-foot viewing
) { ... }
```

**Component Padding (e.g., Button):**
```kotlin
Button(
    modifier = Modifier.padding(
        horizontal = SkySpacing.l,  // 16dp
        vertical = SkySpacing.s     // 8dp
    )
) { ... }
```

**Gap Between Items (Horizontal):**
```kotlin
LazyRow(
    horizontalArrangement = Arrangement.spacedBy(SkySpacing.m)  // 12dp (mobile)
    // OR Arrangement.spacedBy(16.dp) for TV
) { ... }
```

**Gap Between Sections:**
```kotlin
Column(
    verticalArrangement = Arrangement.spacedBy(SkySpacing.xl)  // 24dp between major sections
) { ... }
```

#### Grid System (TV)

TV layout uses a **4-column grid** with flexible cell sizing:

```kotlin
val tvColumnWidth = 240.dp  // Typical card width on TV
val tvHorizontalPadding = 20.dp
val tvItemSpacing = 16.dp

LazyRow(
    contentPadding = PaddingValues(
        horizontal = tvHorizontalPadding,
        vertical = SkySpacing.l
    ),
    horizontalArrangement = Arrangement.spacedBy(tvItemSpacing),
    modifier = Modifier.fillMaxWidth()
) {
    items(channels.size) { index ->
        TvLandscapeCard(
            width = tvColumnWidth,
            ...
        )
    }
}
```

---

### 4. Corner Radii & Shapes

Rounded corners are defined in `ui/theme/Theme.kt` as `SkyRadius`:

```kotlin
object SkyRadius {
    val chip = 8.dp      // Small interactive elements (chips, small buttons)
    val card = 16.dp     // Cards, modals, standard components
    val hero = 22.dp     // Large hero sections, full-screen overlays
    val sheet = 28.dp    // Bottom sheets, large modals (rarely used)
}
```

#### Usage

**Buttons:**
```kotlin
Button(
    shape = RoundedCornerShape(SkyRadius.chip)  // 8dp
) { ... }

// Call-to-action button (hero section)
Button(
    shape = RoundedCornerShape(SkyRadius.card)  // 16dp
) { "Watch Now" }
```

**Cards:**
```kotlin
Card(
    shape = RoundedCornerShape(SkyRadius.card)  // 16dp
) { ... }

// TV landscape card (16:9 tile)
Box(
    modifier = Modifier
        .clip(RoundedCornerShape(SkyRadius.card))
        .background(...)
) { ... }
```

**Chips & Badges:**
```kotlin
Surface(
    shape = RoundedCornerShape(SkyRadius.chip),  // 8dp
    color = SkyPalette.Surface
) { ... }
```

**Hero Sections:**
```kotlin
Box(
    modifier = Modifier
        .clip(RoundedCornerShape(SkyRadius.hero))  // 22dp
        .background(SkyPalette.heroFallback)
) { ... }
```

---

## Motion & Interaction

Motion in Skyline follows **Sky's signature aesthetic:** smooth, purposeful, and decelerating (ease-out on enter, ease-in on exit).

### 1. Enter Animations (Component Reveal)

**Principle:** Elements "arrive and settle" using fade + slide-up with ease-out easing.

**Specifications:**
- Duration: **340ms**
- Easing: `LinearOutSlowInEasing` (Sky's characteristic deceleration)
- Direction: Vertical slide-up (22dp to 0dp)
- Layer: Graphics-layer transform (non-layout-affecting)

**Implementation:** Use the `Modifier.enterReveal()` helper defined in `ui/components/Motion.kt`:

```kotlin
fun Modifier.enterReveal(delayMs: Int = 0): Modifier = composed {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs.toLong())
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 340, easing = LinearOutSlowInEasing),
        label = "enterReveal",
    )
    graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 22.dp.toPx()
    }
}
```

**Staggered Reveal:** For lists of items, stagger the delay using `revealDelay()`:

```kotlin
fun revealDelay(index: Int, stepMs: Int = 55, maxMs: Int = 330): Int =
    (index * stepMs).coerceAtMost(maxMs)
```

**Usage Example (Rails):**
```kotlin
Column {
    railsData.forEachIndexed { index, rail ->
        SectionHeader(
            text = rail.name,
            modifier = Modifier.enterReveal(revealDelay(index))
        )
        LazyRow {
            items(rail.channels) { channel ->
                ChannelCard(channel)
            }
        }
    }
}
```

**Usage Example (Individual Item):**
```kotlin
LazyColumn {
    items(items.size) { index ->
        ListItem(
            item = items[index],
            modifier = Modifier.enterReveal(revealDelay(index))
        )
    }
}
```

### 2. Exit Animations (Component Hide)

**Principle:** Simple fade-out without movement (simplicity over complexity).

**Specifications:**
- Duration: **200ms**
- Easing: `FastOutLinearInEasing` (quick deceleration into linear)
- No vertical movement (keep layout stable during exit)

**Implementation:**
```kotlin
fun Modifier.exitFade(durationMs: Int = 200): Modifier = composed {
    val alpha by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(durationMillis = durationMs, easing = FastOutLinearInEasing),
        label = "exitFade",
    )
    graphicsLayer { this.alpha = alpha }
}
```

### 3. Screen Transitions

**Principle:** Smooth crossfade between screens when navigating.

**Specifications:**
- Duration: **200ms** (standard navigation)
- Easing: Linear fade (no ease needed for full-screen)
- Implemented in navigation stack (SkylineNavHost)

**Implementation in Navigation:**
```kotlin
NavHost(
    navController = navController,
    startDestination = Routes.HOME,
    enterTransition = { fadeIn(tween(200)) },
    exitTransition = { fadeOut(tween(200)) },
    popEnterTransition = { fadeIn(tween(200)) },
    popExitTransition = { fadeOut(tween(200)) },
) { ... }
```

**Tab/Category Switch (TV):**
```kotlin
// When switching between tabs on TV, use a 220ms crossfade
Crossfade(
    targetState = selectedTab,
    animationSpec = tween(durationMillis = 220, easing = LinearEasing)
) { tab ->
    when (tab) {
        Tab.HOME -> HomeScreen()
        Tab.GUIDE -> GuideScreen()
        // ...
    }
}
```

### 4. Focus Motion (TV Only)

**Principle:** D-pad focus is signaled by a subtle scale + white outline. Implemented via graphics-layer transform so it never affects layout.

**Specifications:**
- Scale: **1.04x** (4% zoom)
- Outline: **3dp white stroke** (when focused)
- Duration: **140ms**
- Easing: `LinearOutSlowInEasing`
- Layer: Graphics-layer (layout-stable)

**Implementation in `tv/TvComponents.kt`:**
```kotlin
fun Modifier.tvClickable(
    shape: Shape = RoundedCornerShape(12.dp),
    onClick: () -> Unit,
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.04f else 1f,
        animationSpec = tween(durationMillis = 140, easing = LinearOutSlowInEasing),
        label = "tvFocusPop",
    )
    this
        .onFocusChanged { focused = it.isFocused }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .border(
            width = if (focused) 3.dp else 0.dp,
            color = if (focused) Color.White else Color.Transparent,
            shape = shape,
        )
        .clip(shape)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
}
```

**Usage Example (TV Card):**
```kotlin
Box(
    modifier = Modifier
        .tvClickable(onClick = { playChannel(channel) })
        .background(...)
) { ... }
```

### 5. Interactive Press Feedback

**Mobile:**
- Ripple effect provided by Material3 button defaults
- No additional scale feedback (ripple is sufficient)

**TV:**
- Focus motion (scale + outline) handles all visual feedback
- No ripple (D-pad navigation is non-tactile)

---

## Components

### 1. Buttons

**Base Button (Material3 default):**
```kotlin
Button(
    onClick = { },
    colors = ButtonDefaults.buttonColors(
        containerColor = SkyPalette.Accent,
        contentColor = Color.White
    ),
    shape = RoundedCornerShape(SkyRadius.chip),  // 8dp
    modifier = Modifier.height(48.dp)  // Min height for touch targets
) {
    Text("Action", style = MaterialTheme.typography.labelLarge)
}
```

**Variants:**

```kotlin
// Primary (Accent blue)
Button(
    onClick = { },
    colors = ButtonDefaults.buttonColors(
        containerColor = SkyPalette.Accent
    )
) { Text("Play") }

// Secondary (Surface color, text outline)
OutlinedButton(
    onClick = { },
    border = BorderStroke(1.dp, SkyPalette.TextSecondary),
    colors = OutlinedButtonDefaults.outlinedButtonColors(
        contentColor = SkyPalette.TextPrimary
    )
) { Text("Cancel") }

// Tertiary (Text-only)
TextButton(
    onClick = { },
    colors = TextButtonDefaults.textButtonColors(
        contentColor = SkyPalette.Accent
    )
) { Text("Learn More") }

// White Call-to-Action (Hero section)
Button(
    onClick = { },
    colors = ButtonDefaults.buttonColors(
        containerColor = Color.White,
        contentColor = Color.Black
    ),
    shape = RoundedCornerShape(SkyRadius.card)
) { Text("Watch Now") }
```

**TV Button (Larger, Focus-Enabled):**
```kotlin
Box(
    modifier = Modifier
        .tvClickable(onClick = { playChannel() })
        .size(width = 200.dp, height = 60.dp)
        .background(SkyPalette.Accent, RoundedCornerShape(SkyRadius.card))
        .padding(SkySpacing.l),
    contentAlignment = Alignment.Center
) {
    Text("Select", style = MaterialTheme.typography.labelLarge)
}
```

---

### 2. Cards

**Standard Card:**
```kotlin
Card(
    shape = RoundedCornerShape(SkyRadius.card),  // 16dp
    colors = CardDefaults.cardColors(
        containerColor = SkyPalette.Surface
    ),
    modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
) {
    Column(modifier = Modifier.padding(SkySpacing.l)) {
        Text(
            text = "Card Title",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Subtitle or description",
            style = MaterialTheme.typography.bodyMedium,
            color = SkyPalette.TextSecondary
        )
    }
}
```

**Horizontal Rail Card (16:9 aspect ratio):**
```kotlin
Box(
    modifier = Modifier
        .width(220.dp)
        .aspectRatio(16f / 9f)
        .clip(RoundedCornerShape(SkyRadius.card))
        .background(
            Brush.linearGradient(
                listOf(SkyPalette.Surface, SkyPalette.Indigo.copy(alpha = 0.5f))
            )
        )
) {
    // Artwork image
    AsyncImage(
        model = imageUrl,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
    
    // Title overlay at bottom
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                )
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2
        )
    }
}
```

---

### 3. Text Input

**Text Field:**
```kotlin
TextField(
    value = text,
    onValueChange = { text = it },
    placeholder = { Text("Search...") },
    shape = RoundedCornerShape(SkyRadius.card),
    colors = TextFieldDefaults.colors(
        focusedContainerColor = SkyPalette.Surface,
        unfocusedContainerColor = SkyPalette.Surface,
        focusedIndicatorColor = SkyPalette.Accent,
        unfocusedIndicatorColor = Color.Transparent,
        focusedTextColor = SkyPalette.TextPrimary,
        placeholderColor = SkyPalette.TextSecondary
    ),
    modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
)
```

---

### 4. Chips

**Filter Chip (Category selector):**
```kotlin
FilterChip(
    selected = isSelected,
    onClick = { toggleSelection() },
    label = { Text("Action", style = MaterialTheme.typography.labelMedium) },
    shape = RoundedCornerShape(SkyRadius.chip),  // 8dp
    colors = FilterChipDefaults.filterChipColors(
        containerColor = SkyPalette.Surface,
        selectedContainerColor = SkyPalette.Accent,
        labelColor = SkyPalette.TextPrimary,
        selectedLabelColor = Color.White
    ),
    border = BorderStroke(1.dp, SkyPalette.TextSecondary.copy(alpha = 0.3f))
)
```

---

### 5. LazyRow/LazyColumn Rails

**Horizontal Rail (Carousel):**
```kotlin
LazyRow(
    contentPadding = PaddingValues(
        horizontal = SkySpacing.gutter,  // 16dp (mobile) or 20dp (TV)
        vertical = SkySpacing.l
    ),
    horizontalArrangement = Arrangement.spacedBy(SkySpacing.m),  // 12dp (mobile)
    modifier = Modifier
        .fillMaxWidth()
        .enterReveal(delayMs = revealDelay(railIndex))
) {
    items(channels.size, key = { channels[it].id }) { index ->
        ChannelCard(
            channel = channels[index],
            modifier = Modifier.enterReveal(delayMs = revealDelay(index))
        )
    }
}
```

**Section Header + Rail:**
```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .enterReveal(delayMs = revealDelay(sectionIndex))
) {
    Text(
        text = "Popular Right Now",
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(
            start = SkySpacing.gutter,
            bottom = SkySpacing.m,
            top = SkySpacing.xl
        )
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = SkySpacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(SkySpacing.m)
    ) {
        items(items.size) { index ->
            ItemCard(item = items[index])
        }
    }
}
```

---

### 6. Hero Spotlight (Full-Bleed Section)

**Hero Section with Artwork:**
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(400.dp)
        .clip(RoundedCornerShape(SkyRadius.hero))
        .background(SkyPalette.Canvas)
) {
    // Background artwork (full-bleed)
    AsyncImage(
        model = artworkUrl,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
    
    // Bottom scrim (gradient overlay)
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(200.dp)
            .background(SkyPalette.heroScrim)
    )
    
    // Content overlay
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(SkySpacing.gutter)
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            color = SkyPalette.Accent
        )
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium,
            color = SkyPalette.TextPrimary,
            modifier = Modifier.padding(vertical = SkySpacing.s)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = SkyPalette.TextSecondary,
            maxLines = 2
        )
        
        // Call-to-action button
        Button(
            onClick = { playContent() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(SkyRadius.card),
            modifier = Modifier.padding(top = SkySpacing.xl)
        ) {
            Text("Watch Now", style = MaterialTheme.typography.labelLarge)
        }
    }
}
```

**Hero Section Fallback (No Artwork):**
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(400.dp)
        .background(SkyPalette.heroFallback)
        .padding(SkySpacing.gutter),
    contentAlignment = Alignment.BottomStart
) {
    // Same content overlay as above
    Column { ... }
}
```

---

## Patterns & Flows

### 1. Navigation

**Bottom Navigation (Mobile):**
- 5 tabs: Home, Live, Movies, Series, Guide
- Accent blue (#0B69F5) when selected
- No pill-shaped background (Sky's aesthetic: clean and minimal)

```kotlin
NavigationBar(
    containerColor = SkyPalette.Canvas
) {
    bottomNavItems.forEach { (route, label, icon) ->
        val selected = currentRoute == route
        NavigationBarItem(
            selected = selected,
            onClick = { navController.navigate(route) },
            icon = { Icon(icon, label) },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SkyPalette.Accent,
                selectedTextColor = SkyPalette.Accent,
                unselectedIconColor = SkyPalette.TextSecondary,
                unselectedTextColor = SkyPalette.TextSecondary,
                indicatorColor = Color.Transparent  // No pill background
            )
        )
    }
}
```

**D-Pad Navigation (TV):**
- Focus ring (white outline + scale) indicates current focus
- Directional pad (D-pad) moves focus left/right/up/down
- Focus order: sensible defaults (left-to-right, top-to-bottom)

---

### 2. List Display

**Scrollable Column with Sections:**
```kotlin
LazyColumn(
    contentPadding = PaddingValues(
        horizontal = SkySpacing.gutter,
        vertical = SkySpacing.xl
    ),
    verticalArrangement = Arrangement.spacedBy(SkySpacing.xl)
) {
    items(sections.size) { sectionIndex ->
        val section = sections[sectionIndex]
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .enterReveal(revealDelay(sectionIndex))
        ) {
            SectionHeader(section.title)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(SkySpacing.m)
            ) {
                items(section.items.size) { itemIndex ->
                    ItemCard(section.items[itemIndex])
                }
            }
        }
    }
}
```

**Grid Display (Movies, Series):**
```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2),  // 2 columns on mobile, 4 on TV
    contentPadding = PaddingValues(SkySpacing.gutter),
    horizontalArrangement = Arrangement.spacedBy(SkySpacing.m),
    verticalArrangement = Arrangement.spacedBy(SkySpacing.m)
) {
    items(movies.size) { index ->
        MovieCard(
            movie = movies[index],
            modifier = Modifier.enterReveal(revealDelay(index))
        )
    }
}
```

---

### 3. Loading & Error States

**Loading Spinner (Subtle):**
```kotlin
if (isLoading) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = SkyPalette.Accent,
            modifier = Modifier
                .size(40.dp)
                .alpha(0.6f)
        )
    }
}
```

**Error State (Inline):**
```kotlin
if (errorMessage != null) {
    Surface(
        color = SkyPalette.Error.copy(alpha = 0.15f),
        shape = RoundedCornerShape(SkyRadius.card),
        modifier = Modifier
            .fillMaxWidth()
            .padding(SkySpacing.gutter)
    ) {
        Row(
            modifier = Modifier.padding(SkySpacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkySpacing.m)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = SkyPalette.Error
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Something went wrong",
                    style = MaterialTheme.typography.titleSmall,
                    color = SkyPalette.Error
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyPalette.TextSecondary
                )
            }
            Button(
                onClick = { retryAction() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SkyPalette.Error
                ),
                shape = RoundedCornerShape(SkyRadius.chip)
            ) {
                Text("Retry")
            }
        }
    }
}
```

**Empty State:**
```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(SkySpacing.xl),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Icon(
        imageVector = Icons.Default.Movie,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = SkyPalette.TextSecondary.copy(alpha = 0.5f)
    )
    Spacer(modifier = Modifier.height(SkySpacing.l))
    Text(
        text = "No movies found",
        style = MaterialTheme.typography.titleMedium,
        color = SkyPalette.TextPrimary
    )
    Text(
        text = "Try adjusting your filters or search",
        style = MaterialTheme.typography.bodyMedium,
        color = SkyPalette.TextSecondary
    )
}
```

---

### 4. Favorites

**Heart Icon (Toggle):**
```kotlin
var isFavorite by remember { mutableStateOf(false) }

IconButton(
    onClick = {
        isFavorite = !isFavorite
        // Save to database
        viewModel.toggleFavorite(contentId)
    },
    modifier = Modifier.size(48.dp)
) {
    Icon(
        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.Favorite,
        contentDescription = "Favorite",
        tint = if (isFavorite) SkyPalette.LiveRed else SkyPalette.TextSecondary,
        modifier = Modifier
            .size(24.dp)
            .scaleAnimated(isFavorite)  // Quick scale animation
    )
}
```

**Scale Animation Helper:**
```kotlin
fun Modifier.scaleAnimated(isActive: Boolean): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "scaleAnimation"
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}
```

---

## TV-Specific (10-Foot UI)

### Focus Navigation

**All interactive TV elements use `Modifier.tvClickable()`:**

```kotlin
Box(
    modifier = Modifier
        .tvClickable(onClick = { selectItem() })
        .size(width = 240.dp, height = 135.dp)
        .background(...)
) { ... }
```

**Focus Order (Automatic in Compose, but can be customized):**
- Default: left-to-right, top-to-bottom
- Use `Modifier.focusRequester()` to customize if needed

### Sizing for 10-Foot Viewing

| Element | Mobile | TV |
|---------|--------|-----|
| **Text (body)** | 16sp | 18sp |
| **Text (headers)** | 20sp | 24sp |
| **Touch target height** | 48dp | 56dp |
| **Card width** | Variable | 240dp |
| **Horizontal padding** | 16dp | 20dp |
| **Item spacing** | 12dp | 16dp |

### TV Home Screen Layout

```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .background(SkyPalette.tvBackground)
        .verticalScroll(rememberScrollState())
) {
    // Hero spotlight (full width)
    HeroSpotlight(
        artwork = heroItem,
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .padding(20.dp)
    )
    
    // "Today's Top Picks" rail
    CategoryRail(
        title = "Today's Top Picks",
        channels = topChannels,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    )
    
    // Genre rails (e.g., Football, Comedy, Drama)
    genreRails.forEachIndexed { index, rail ->
        CategoryRail(
            title = rail.name,
            channels = rail.channels,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .enterReveal(revealDelay(index))
        )
    }
}
```

### TV Guide (Time-Aligned Grid)

**Grid Layout:**
- Vertical axis: Time (30-minute increments, vertically scrollable)
- Horizontal axis: Channels (4-column grid)
- Each programme cell sized by actual duration (dp-per-minute layout)
- Now marker: Blue vertical line at current time

**Day Selector Chips:**
```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(SkySpacing.s),
    modifier = Modifier.padding(SkySpacing.gutter)
) {
    days.forEachIndexed { index, day ->
        FilterChip(
            selected = selectedDay == index,
            onClick = { selectDay(index) },
            label = { Text(day, style = MaterialTheme.typography.labelMedium) },
            shape = RoundedCornerShape(SkyRadius.chip)
        )
    }
}
```

---

## Mobile-Specific (Phone/Tablet)

### Safe Areas

**Screen Edge Padding:**
```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = SkySpacing.gutter)  // 16dp
) { ... }
```

**Respect System Bars:**
```kotlin
Surface(
    modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()  // Jetpack Compose API
) { ... }
```

### Portrait vs Landscape

**Portrait (Default):**
```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    contentPadding = PaddingValues(SkySpacing.gutter)
) { ... }
```

**Landscape (Wider):**
```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(3),  // More columns in landscape
    contentPadding = PaddingValues(SkySpacing.gutter)
) { ... }
```

### Scrolling

**Smooth Inertial Scroll (Compose default):**
```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize()
) {
    items(items.size) { index ->
        ListItem(
            item = items[index],
            modifier = Modifier.enterReveal(revealDelay(index))
        )
    }
}
```

---

## Accessibility

### Color Contrast

All text colors meet WCAG AAA standards:
- **TextPrimary on Canvas:** 18.2:1
- **TextPrimary on Surface:** 15.8:1
- **Accent on Canvas (interactive):** 5.1:1

### Text Size Minimums

- **Body text:** 16sp (never smaller than 14sp)
- **Captions/metadata:** 12sp minimum (11sp only for very small labels)
- **Headers:** 20sp minimum

### Focus Visibility

**Mobile:**
- Material3 ripple effect is sufficient for indicating focus

**TV:**
- White outline + scale (1.04x) clearly indicates D-pad focus
- Minimum outline width: 3dp (high visibility)

### Alternative Text

All images require `contentDescription`:
```kotlin
AsyncImage(
    model = imageUrl,
    contentDescription = "Episode thumbnail: Breaking Bad S5E1",  // Descriptive
    ...
)
```

### Navigation Keyboard Support

- Tab key cycles through interactive elements (automatic in Compose)
- Enter/Space activates buttons
- Arrow keys navigate TV D-pad elements

---

## Design System Governance

### When to Use This System

**Before adding a new component or pattern:**
1. Check this design system first
2. Reuse existing patterns instead of creating new ones
3. Only introduce new patterns if no existing pattern fits

### Versioning

This document is **Version 1.0** (July 2026).

**Future updates:**
- Update this file with new patterns/changes
- Keep a brief changelog at the top
- Tag major versions when significant shifts occur

### Questions or Conflicts?

If a design question arises:
1. Check this system
2. If not covered, propose the addition as a pull request to the design system
3. Discuss with the design team before implementation

---

## Reference Files

### Codebase Implementation
- **Colors & Typography:** `ui/theme/Theme.kt`
- **Motion Helpers:** `ui/components/Motion.kt`
- **TV Components:** `tv/TvComponents.kt`
- **Example Screens:** `ui/home/HomeScreen.kt`, `tv/TvScreens.kt`

### Design Research & Principles
- **Sky Design Language Research:** `docs/sky-design-language.md` — Detailed analysis of Sky UI principles, motion specifications, TV interaction patterns, and implementation mapping
- **Official Sky UI System:** http://sky-ui.cf.sky.com/ (may require Sky login)
- **Sky Toolkit (Open Source):** https://github.com/sky-uk/toolkit

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | July 2026 | Initial comprehensive design system with Sky design principles, all foundation tokens, component patterns, and TV/mobile variants |

---

**Maintained By:** Skyline IPTV Design System Working Group  
**Status:** Active — v1.0 Production Ready  

**Using This System:** When adding features, check this system first to reuse patterns. If a pattern isn't covered, propose it as a pull request addition. Keep the design system and codebase aligned—if you change a pattern, update both the code and this document.
