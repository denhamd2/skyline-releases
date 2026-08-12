# Sky Component Library

**Last updated:** 2026-07-23  
**Version:** 1.0 (Sky Design System v1.0)

Authoritative catalog of reusable components in Skyline IPTV. Each component is production-ready and follows Sky design standards. Use this library before building custom UI — the answer to "how should I build X?" is probably here.

**Golden Rule:** Before building a new component, check this library. If something similar exists, reuse it.

---

## Table of Contents

1. [Foundation Tokens](#foundation-tokens)
2. [Motion Primitives](#motion-primitives)
3. [Button & Action Components](#button--action-components)
4. [Cards & Tiles](#cards--tiles)
5. [Rails & Carousels](#rails--carousels)
6. [TV-Specific Components](#tv-specific-components)
7. [Media & Images](#media--images)
8. [Badges & Labels](#badges--labels)
9. [Patterns & Layouts](#patterns--layouts)

---

## Foundation Tokens

### Color Palette

**File:** `ui/theme/Theme.kt` → `SkyPalette` object

All interactive elements **must** use these tokens; hardcoding hex values is not allowed (Detekt lint will flag it).

```kotlin
// Backgrounds
SkyPalette.Canvas              // #05070A — deep navy, all screen backgrounds
SkyPalette.Surface             // #0E1520 — card/sheet containers
SkyPalette.SurfaceElevated     // #16233A — raised surface tier

// Interactive
SkyPalette.Accent              // #0B69F5 — Sky blue, buttons/focus/active
SkyPalette.AccentBright        // #2A9BE0 — hover state (lighter blue)
SkyPalette.Brand               // #000FF5 — electric brand (sparingly)

// Alert/Status
SkyPalette.LiveRed             // #E11D3F — LIVE badges, urgent indicators
SkyPalette.Error               // #FF6B6B — error messages

// Text
SkyPalette.TextPrimary         // #FFFFFF — primary text, 100% opacity
SkyPalette.TextSecondary       // #8FA0B5 — secondary/metadata text
SkyPalette.TextMuted           // #7C8899 — captions, deemphasized text
```

**Usage Rule:** Never write `Color(0x...` — always use `SkyPalette.TokenName`. This is enforced by Detekt linting.

---

### Spacing & Layout Grid

**File:** `ui/theme/Theme.kt` → `SkySpacing` object

All spacing is based on an 8-point grid. No arbitrary spacing allowed.

```kotlin
SkySpacing.xs      // 4.dp  — extra-tight gaps
SkySpacing.s       // 8.dp  — standard padding/margin
SkySpacing.m       // 12.dp — comfortable breathing room
SkySpacing.l       // 16.dp — major sections, screen edge safe area
SkySpacing.xl      // 24.dp — hero sections, large separators
SkySpacing.gutter  // 16.dp — horizontal padding on all screens
```

**Usage Rule:** Detekt will warn on padding/margin values that aren't 8-point multiples.

---

### Corner Radius

**File:** `ui/theme/Theme.kt` → `SkyRadius` object

```kotlin
SkyRadius.chip    // 8.dp   — chips, small badges
SkyRadius.card    // 16.dp  — most cards (default)
SkyRadius.hero    // 22.dp  — large hero sections
SkyRadius.sheet   // 28.dp  — bottom sheets, large modals
```

---

### Typography

**File:** `ui/theme/Theme.kt` → `SkyTypography`

All text must use Material3 typography styles (Detekt will flag hardcoded sizes).

```kotlin
MaterialTheme.typography.displayLarge    // 44sp, ExtraBold
MaterialTheme.typography.displayMedium   // 32sp, ExtraBold
MaterialTheme.typography.headlineSmall   // 20sp, Bold
MaterialTheme.typography.titleLarge      // 20sp, SemiBold (section headers)
MaterialTheme.typography.bodyLarge       // 16sp, Normal (prose)
MaterialTheme.typography.labelMedium     // 12sp, Medium (buttons, badges)
```

---

## Motion Primitives

### Staggered Enter Reveal (Sky Signature)

**File:** `ui/components/Motion.kt` → `Modifier.enterReveal()`

Fade-in + slide-up animation, the signature Sky motion. Used for all component reveals.

```kotlin
// Single element
Box(modifier = Modifier.enterReveal())

// Staggered reveal (list of items)
items(channels.size) { i ->
    ChannelCard(
        title = channels[i].name,
        modifier = Modifier.enterReveal(delayMs = revealDelay(i))
    )
}
```

**Spec:**
- Duration: 340ms
- Easing: `LinearOutSlowInEasing` (ease-out)
- Motion: Fade (0 → 1 opacity) + slide-up (22.dp)
- Stagger: `revealDelay(index)` = index × 55ms (capped at 330ms)

---

### Screen Crossfade Transition

**Usage:** When switching between navigation screens or tabs.

**Spec:**
- Duration: 200–220ms
- Easing: Linear
- No slide, only opacity change

---

## Button & Action Components

### Pill Button (Blue CTA with Play Icon)

**File:** `ui/components/Components.kt` → `PillButton()`

Standard action button: blue accent background, play icon, rounded.

```kotlin
PillButton(
    label = "Resume",
    onClick = { navController.navigate(...) },
)
```

**Appearance:**
- Background: `SkyPalette.Accent` (Sky blue)
- Text: White
- Icon: Play arrow (18dp)
- Padding: 18dp horizontal, 10dp vertical
- Corner radius: 50dp (fully rounded)
- Press feedback: Scale to 0.97x (100ms)

---

### Scaled Clickable

**File:** `ui/components/Components.kt` → `Modifier.scaledClickable()`

Reusable press feedback modifier — applies 0.97x scale on touch.

```kotlin
Box(
    modifier = Modifier
        .size(100.dp)
        .background(SkyPalette.Surface)
        .scaledClickable { onItemTap() }
)
```

**Spec:**
- Scale: 0.97x (touch down) → 1.0x (release)
- Duration: 150ms
- Applied via graphicsLayer (no layout re-measure)

---

## Cards & Tiles

### Poster Card (2:3 Portrait, VOD/Series)

**File:** `ui/components/Components.kt` → `PosterCard()`

Standard for films and series. Shows artwork as main element, title below.

```kotlin
PosterCard(
    title = "Dune",
    imageUrl = movie.posterUrl,
    onClick = { navController.navigate(Routes.movieDetail(movie.id)) },
    width = 120.dp,
)
```

**Appearance:**
- Aspect: 2:3 (portrait)
- Width: 120dp (mobile), 150dp (TV)
- Corners: 16dp
- Title below: labelMedium, 2-line max, ellipsis
- Press: scaledClickable (0.97x)

---

### Channel Card (16:9 Landscape, Live TV)

**File:** `ui/components/Components.kt` → `ChannelCard()`

For live channels. Shows channel logo and programme name over an indigo gradient.

```kotlin
ChannelCard(
    name = "BBC One",
    subtitle = "Policing Paradise",
    imageUrl = channel.logoUrl,
    onClick = { playChannel(channel) },
    width = 172.dp,
)
```

**Appearance:**
- Aspect: 16:9
- Background: Indigo gradient
- Channel logo: Top-left, 38dp
- Programme title + subtitle: Bottom-left over black scrim
- Press: scaledClickable

---

### TV Landscape Card (16:9 with Focus Treatment)

**File:** `tv/TvComponents.kt` → `TvLandscapeCard()`

TV-specific variant with focus treatment and optional programme artwork + channel badge.

```kotlin
TvLandscapeCard(
    title = "Policing Paradise",
    imageUrl = channel.logoUrl,
    programmeImageUrl = programme.artworkUrl,  // optional
    onClick = { playChannel(channel) },
    width = 240.dp,
)
```

**Appearance:**
- Aspect: 16:9
- Width: 220dp (mobile), 240dp (TV)
- Image: Programme imageUrl (if supplied) else channel logo
- If programme artwork: Shows channel logo as 34dp badge (top-left)
- Focus treatment: White outline (3dp) + 1.04x scale (140ms ease-out)

---

## Rails & Carousels

### Horizontal Rail with Snap Fling

**File:** `ui/components/Components.kt` → `Rail<T>()`

Generic carousel for displaying a list of items. Snap-to-start scrolling, staggered entry animation.

```kotlin
Rail(
    title = "Top Picks",
    items = channels,
    key = { it.streamId },
    card = { channel ->
        ChannelCard(
            name = channel.name,
            imageUrl = channel.logoUrl,
            onClick = { playChannel(channel) },
            modifier = Modifier.enterReveal(revealDelay(channels.indexOf(channel)))
        )
    }
)
```

**Appearance:**
- Title: headlineSmall, bold (optional)
- Horizontal scroll with snap fling
- Item spacing: 10dp
- Content padding: 16dp (left/right)

---

### Section Header with Optional "View All"

**File:** `ui/components/Components.kt` → `SectionHeader()`

Reusable section header with optional "View all" action link.

```kotlin
SectionHeader(
    title = "Films",
    onViewAll = { navController.navigate(Routes.MOVIES) }
)
```

**Appearance:**
- Title: headlineSmall, white
- "View all": labelMedium, Accent blue (right-aligned)
- Padding: 16dp horizontal, 8dp vertical

---

## TV-Specific Components

### TV Focus & D-Pad Navigation

**File:** `tv/TvComponents.kt` → `Modifier.tvClickable()`

Universal focus treatment for all TV interactive elements. White outline + 1.04x scale.

```kotlin
Box(
    modifier = Modifier
        .size(200.dp, 120.dp)
        .tvClickable(
            shape = RoundedCornerShape(16.dp),
            onClick = { playChannel(channel) }
        )
        .background(SkyPalette.Surface)
)
```

**Appearance:**
- Unfocused: No outline, scale 1.0x
- Focused: White outline (3dp), scale 1.04x
- Animation: 140ms ease-out tween
- Applied via graphicsLayer (non-layout transform)

**D-Pad Navigation:**
- Focus system: Automatic (Compose handles D-pad → focus traversal)
- Left/Right/Up/Down: Navigate between focusable siblings
- Center/OK: Triggers `onClick()`

---

## Media & Images

### Artwork Image with Fallback & Shimmer

**File:** `ui/components/Components.kt` → `ArtworkImage()`

Universal image loader with: shimmer placeholder, error handling, icon fallback, dual-URL fallback.

```kotlin
ArtworkImage(
    url = channel.logoUrl,
    fallbackIcon = Icons.Default.LiveTv,
    contentDescription = "Channel logo",
    contentScale = ContentScale.Fit,
    modifier = Modifier.size(40.dp),
    fallbackUrl = channel.placeholderLogoUrl,
)
```

**Behavior:**
1. Loading: Shimmer box (animated navy gradient)
2. Success: Image displayed at specified ContentScale
3. Error on primary URL: Try fallbackUrl (if supplied)
4. Error on both: Show fallbackIcon in SurfaceElevated box

---

### Shimmer Box (Loading Skeleton)

**File:** `ui/components/Components.kt` → `ShimmerBox()`

Animated loading placeholder for images or content.

```kotlin
if (isLoading) {
    ShimmerBox(
        modifier = Modifier
            .size(200.dp, 120.dp)
            .clip(RoundedCornerShape(16.dp))
    )
}
```

---

## Badges & Labels

### Live Badge (Red LIVE Chip)

**File:** `ui/components/Components.kt` → `LiveBadge()`

Red pill indicating live/active broadcast.

```kotlin
Row(verticalAlignment = Alignment.CenterVertically) {
    Text("BBC News")
    LiveBadge()
}
```

**Appearance:**
- Text: "LIVE" in labelSmall, white
- Background: `SkyPalette.LiveRed`
- Padding: 6dp horizontal, 2dp vertical
- Corners: 4dp

---

### Provider Badge (Dark Chip)

**File:** `ui/components/Components.kt` → `ProviderBadge()`

Small badge for provider/category names (e.g., "sky sports").

```kotlin
Column {
    ProviderBadge(text = "Sky Sports")
    Image(...)
}
```

**Appearance:**
- Text: Lowercase, labelSmall
- Background: `SkyPalette.SurfaceElevated`
- Padding: 6dp horizontal, 2dp vertical

---

## Patterns & Layouts

### Hero Spotlight (Full-Bleed with Scrim)

Full-width backdrop artwork with bottom scrim and overlay text.

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(240.dp)
        .clip(RoundedCornerShape(SkyRadius.card))
) {
    ArtworkImage(
        url = heroMovie.artworkUrl,
        contentDescription = heroMovie.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .background(SkyPalette.heroScrim)
            .padding(SkySpacing.l),
    ) {
        Text(
            heroMovie.name,
            style = MaterialTheme.typography.displaySmall,
            color = SkyPalette.TextPrimary,
        )
        PillButton(label = "Watch Now", onClick = { playMovie(heroMovie) })
    }
}
```

---

### Staggered List Layout

```kotlin
val revealDelayBase = 55

LazyColumn(contentPadding = PaddingValues(bottom = SkySpacing.xl)) {
    item {
        SectionHeader("Top Picks")
    }
    item {
        Rail(
            title = "",
            items = films,
            card = { film -> PosterCard(...) },
            modifier = Modifier.enterReveal(delayMs = revealDelayBase)
        )
    }
}
```

---

### Error State Pattern

Show centered error message with retry button.

```kotlin
if (state.error != null) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(SkySpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            state.error,
            style = MaterialTheme.typography.titleMedium,
            color = SkyPalette.Error,
        )
        Spacer(Modifier.height(SkySpacing.l))
        PillButton(label = "Try Again", onClick = { viewModel.retry() })
    }
}
```

---

## Design System Compliance Checklist

When building a new screen or component:

- [ ] **Colors:** All colors are from `SkyPalette.*` (Detekt checks this)
- [ ] **Spacing:** All padding/margin are 8-point multiples (Detekt checks this)
- [ ] **Typography:** All text uses `MaterialTheme.typography.*` styles (Detekt checks this)
- [ ] **Corners:** All rounded corners use `SkyRadius.*`
- [ ] **Motion:** Animations use correct durations and easing
- [ ] **Images:** All images use `ArtworkImage` with fallback
- [ ] **TV Focus:** All TV interactive elements use `Modifier.tvClickable()`
- [ ] **Reuse:** Check this library first

---

## Quick Start: Building a New Screen

1. **Pick a layout pattern** — hero + rails, grid, list, etc.
2. **Gather your data** — films, channels, categories
3. **Arrange in sections** — use Rails for horizontal scrolls, Columns for vertical
4. **Apply cards** — PosterCard for VOD, ChannelCard for live, TvLandscapeCard for TV
5. **Add motion** — wrap sections with `enterReveal(revealDelay(...))`
6. **TV variant** — use TV-specific components
7. **Run Detekt** — `./gradlew detekt` to check compliance

---

## Useful File References

| Component | File |
|-----------|------|
| Tokens | `ui/theme/Theme.kt` |
| Motion | `ui/components/Motion.kt` |
| Components | `ui/components/Components.kt` |
| TV components | `tv/TvComponents.kt` |
| Navigation | `ui/navigation/SkylineNavHost.kt` |

---

**Last Updated:** 2026-07-23 v1.0  
**Related Docs:** `docs/sky-design-language.md`, `.github/pull_request_template.md`

### Secondary Button

**File:** `ui/theme/Theme.kt` (Material3 defaults)  
**Usage:** Cancel, Back, or deprioritized actions  

**Code Example:**
```kotlin
OutlinedButton(
    onClick = { goBack() },
    modifier = Modifier.height(48.dp)
) {
    Text("Cancel")
}
```

### White Call-to-Action (Hero)

**File:** `ui/home/HomeScreen.kt` (see line ~450)  
**Usage:** Over dark hero artwork or gradient backgrounds  
**Design System:** [White CTA Button](docs/SKY_DESIGN_SYSTEM.md#white-call-to-action)

**Properties:**
- Container: Color.White
- Text: SkyPalette.Canvas (dark text on white)
- Corner radius: 16dp (larger for hero)
- Shadow: Elevated

**Code Example:**
```kotlin
Button(
    onClick = { playNow() },
    colors = ButtonDefaults.buttonColors(
        containerColor = Color.White,
        contentColor = SkyPalette.Canvas
    ),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
        .shadow(8.dp, RoundedCornerShape(16.dp))
        .padding(16.dp)
) {
    Text("Watch Now", fontWeight = FontWeight.SemiBold)
}
```

---

## Cards

### Standard Card (with image)

**File:** `tv/TvComponents.kt` → `TvLandscapeCard`  
**Usage:** Channel/movie/series tiles in horizontal rails  
**Aspect Ratio:** 16:9 (landscape)  
**Design System:** [Cards - Standard](docs/SKY_DESIGN_SYSTEM.md#cards)

**Properties:**
- Dimensions: Width from LazyRow, height maintains 16:9 aspect
- Image: Programme/movie artwork
- Overlay: Bottom gradient scrim (transparent to black)
- Title: White text at bottom, 12sp
- Badge: Optional channel logo or "Live" indicator

**Code Example:**
```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card

Card(
    modifier = Modifier
        .width(240.dp)
        .aspectRatio(16f / 9f)
        .clip(RoundedCornerShape(8.dp)),
    shape = RoundedCornerShape(8.dp)
) {
    Box {
        // Image
        ArtworkImage(
            url = moviePoster,
            modifier = Modifier.fillMaxSize()
        )
        
        // Bottom gradient scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )
        
        // Title overlay
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )
    }
}
```

### Vertical Card (Browse)

**File:** `ui/browse/MoviesScreen.kt` → Movie/Series cards  
**Usage:** Vertical grids of movies/series  
**Aspect Ratio:** Poster (27:40) or custom  

**Code Example:**
```kotlin
Card(
    modifier = Modifier
        .width(120.dp)
        .height(180.dp),
    shape = RoundedCornerShape(8.dp)
) {
    ArtworkImage(url = posterUrl)
}
```

---

## Layout & Containers

### Screen Container (with safe area padding)

**File:** `ui/home/HomeScreen.kt` (line ~200)  
**Usage:** Every screen root container  
**Design System:** [Spacing - Safe Areas](docs/SKY_DESIGN_SYSTEM.md#spacing)

**Properties:**
- Padding: 16dp on mobile, 20dp on TV
- Background: SkyPalette.Canvas
- Respect system insets (status bar, nav bar)

**Code Example:**
```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .background(SkyPalette.Canvas)
        .padding(horizontal = 16.dp)
        .verticalScroll(rememberScrollState())
) {
    // Content here
}
```

### Hero Spotlight Section

**File:** `ui/home/HomeScreen.kt` → Featured movie/show hero  
**Usage:** Full-bleed artwork with overlay title and CTA  
**Design System:** [Hero Spotlight](docs/SKY_DESIGN_SYSTEM.md#hero-spotlight)

**Properties:**
- Full width (fill screen edge to edge)
- Height: 40% of screen or 240dp minimum
- Image: Full-bleed, centered crop
- Scrim: Strong gradient (transparent to #000000 at 80%+ opacity)
- Text & Button: Overlaid, white text

**Code Example:**
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(240.dp)
        .clip(RoundedCornerShape(12.dp))
) {
    // Hero image
    ArtworkImage(
        url = heroArtwork,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
    
    // Bottom scrim gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.8f)
                    ),
                    startY = 100f
                )
            )
    )
    
    // Content overlay
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(16.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = SkyPalette.TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = { play() },
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Watch Now")
        }
    }
}
```

---

## Text & Typography

### Page Title

**Usage:** Screen header  
**Style:** `MaterialTheme.typography.headlineSmall` (24sp, semibold)

```kotlin
Text(
    "Home",
    style = MaterialTheme.typography.headlineSmall,
    color = SkyPalette.TextPrimary
)
```

### Section Header (Rail title)

**Usage:** "Films", "Series", "Recommended", etc.  
**Style:** `MaterialTheme.typography.titleMedium` or `bodyLarge`

```kotlin
Text(
    "Films",
    style = MaterialTheme.typography.bodyLarge,
    fontWeight = FontWeight.SemiBold,
    color = SkyPalette.TextPrimary
)
```

### Body Text

**Usage:** Descriptions, meta info (year, rating, etc.)  
**Style:** `MaterialTheme.typography.bodyMedium` or `bodySmall`

```kotlin
Text(
    "A heartwarming comedy about friendship and adventure.",
    style = MaterialTheme.typography.bodyMedium,
    color = SkyPalette.TextPrimary
)
```

### Secondary/Hint Text

**Usage:** Dates, durations, "Coming soon" labels  
**Style:** `MaterialTheme.typography.bodySmall`  
**Color:** `SkyPalette.TextSecondary`

```kotlin
Text(
    "Available on 3 Jun 2026",
    style = MaterialTheme.typography.bodySmall,
    color = SkyPalette.TextSecondary
)
```

---

## Lists & Carousels

### Horizontal Carousel (LazyRow)

**File:** `ui/home/HomeScreen.kt` → Category rails  
**Usage:** Movies, series, channels in rows  
**Design System:** [LazyRow / Rails](docs/SKY_DESIGN_SYSTEM.md#lazyrow-carousels)

**Properties:**
- Padding: 16dp (mobile), 20dp (TV)
- Item spacing: 12dp (mobile), 16dp (TV)
- Lazy loading (don't render off-screen)
- Animated reveal (enter-reveal stagger)

**Code Example:**
```kotlin
LazyRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(horizontal = 16.dp)
) {
    items(movies) { movie ->
        MovieCard(
            movie = movie,
            modifier = Modifier
                .width(200.dp)
                .enterReveal(delayMs = revealDelay(index))
        )
    }
}
```

### Vertical List (LazyColumn)

**File:** `ui/browse/SearchScreen.kt` → search results  
**Usage:** Results lists, settings options  

**Code Example:**
```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    items(results) { item ->
        ResultRow(item)
    }
}
```

### Grid Layout

**File:** `ui/browse/MoviesScreen.kt` (2-3 column grid)  
**Usage:** Movies/series browse  
**Columns:** 2 (mobile), 3-4 (TV)

**Code Example:**
```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    modifier = Modifier.fillMaxSize(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(16.dp)
) {
    items(movies) { movie ->
        MovieCard(movie)
    }
}
```

---

## Motion & Animation

### Staggered Enter Reveal

**File:** `ui/components/Motion.kt` → `Modifier.enterReveal(delayMs)`  
**Usage:** Rails and lists on first render  
**Design System:** [Enter Animations](docs/SKY_DESIGN_SYSTEM.md#enter-animations)

**Spec:**
- Duration: 340ms
- Easing: LinearOutSlowInEasing (ease-out)
- Animation: Fade + 22dp slide-up
- Stagger: 55ms per item (capped at 330ms delay)

**Code Example:**
```kotlin
import com.denham.skyline.ui.components.enterReveal
import com.denham.skyline.ui.components.revealDelay

LazyRow(...) {
    items(movies.size) { index ->
        MovieCard(
            modifier = Modifier.enterReveal(delayMs = revealDelay(index))
        )
    }
}
```

### Screen Transition (Crossfade)

**File:** `ui/navigation/SkylineNavHost.kt` (line ~165)  
**Usage:** Navigating between screens  
**Design System:** [Screen Transitions](docs/SKY_DESIGN_SYSTEM.md#screen-transitions)

**Spec:**
- Duration: 200ms
- Animation: Fade (no slide)
- Easing: LinearOutSlowInEasing

**Already configured in NavHost:**
```kotlin
NavHost(
    ...,
    enterTransition = { fadeIn(tween(200)) },
    exitTransition = { fadeOut(tween(200)) },
    popEnterTransition = { fadeIn(tween(200)) },
    popExitTransition = { fadeOut(tween(200)) },
)
```

### TV Focus Scale

**File:** `tv/TvComponents.kt` → `Modifier.tvClickable()`  
**Usage:** D-pad focus on TV  
**Design System:** [Focus Motion (TV)](docs/SKY_DESIGN_SYSTEM.md#focus-motion-tv-only)

**Spec:**
- Scale: 1.04x
- Outline: 3dp white stroke
- Duration: 140ms tween
- Applied via graphicsLayer

**Code Example:**
```kotlin
Box(
    modifier = Modifier
        .tvClickable()
        .size(240.dp, 135.dp)
) {
    // Card content
}
```

---

## Form Controls

### Text Input

**File:** `ui/login/LoginScreen.kt` → login form  
**Usage:** Username, password, search queries  

**Properties:**
- Container: SkyPalette.Surface
- Border: 1dp, SkyPalette.Surface
- Focus border: 2dp, SkyPalette.Accent
- Height: 48dp minimum
- Padding: 12dp

**Code Example:**
```kotlin
TextField(
    value = username,
    onValueChange = { username = it },
    label = { Text("Username") },
    modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
    shape = RoundedCornerShape(8.dp),
    colors = TextFieldDefaults.colors(
        focusedContainerColor = SkyPalette.Surface,
        unfocusedContainerColor = SkyPalette.Surface,
        focusedIndicatorColor = SkyPalette.Accent,
        unfocusedIndicatorColor = Color.Transparent
    )
)
```

---

## TV-Specific Components

### TV Navigation Focus Ring

**File:** `tv/TvComponents.kt`  
**Usage:** All interactive elements on TV  
**Design System:** [TV Focus Navigation](docs/SKY_DESIGN_SYSTEM.md#focus-navigation)

**Applied via `Modifier.tvClickable()`:**
- White 3dp outline on focus
- 1.04x scale
- 140ms smooth tween
- Haptic feedback on press

### TV Hero (Full-bleed with scrim)

**File:** `tv/TvScreens.kt` → TV Home hero  
**Usage:** Featured content at top of TV home screen  

**Enhancements over mobile:**
- Larger text (28sp+ for 10-foot viewing)
- Stronger scrim gradient (more opaque black)
- White "Watch Now" button instead of accent color
- Provider badge overlay (e.g., "Sky Cinema")

---

## When to Create a New Component

Only create a new component if **ALL** of these are true:

1. ✅ No existing component covers the use case
2. ✅ It will be reused in 2+ places
3. ✅ It encapsulates meaningful design logic (not just a wrapper)
4. ✅ You've documented it here after creating it

**Red flags:**
- ❌ "It's just a slightly different button color" → Use existing Button with colors parameter
- ❌ "It's a one-off for this screen" → Inline the code, don't create a component
- ❌ "It's a thin wrapper around Material3 component" → Use Material3 directly

---

## Accessibility

All components should support:

- **Dark mode:** Use SkyPalette tokens (automatic via theme)
- **Text scaling:** Use `sp` units for text, let Material3 scale
- **Touch targets:** Minimum 48dp on mobile (already built into Button, etc.)
- **Focus indicators:** TV components use white outline; mobile uses Material3 ripple
- **Content descriptions:** Use `contentDescription` parameter on images

**Example:**
```kotlin
Image(
    painter = painterResource(R.drawable.ic_play),
    contentDescription = "Play video: $movieTitle",
    modifier = Modifier.size(24.dp)
)
```

---

## How to Submit a New Component

1. Build the component in the appropriate folder:
   - `ui/components/` — shared mobile components
   - `tv/TvComponents.kt` — TV-specific components
   - `ui/theme/Theme.kt` — global defaults (Button, TextField, etc.)

2. Add it to this library with:
   - File path and line number
   - Usage description
   - Design system reference
   - Code example
   - Accessibility notes (if special)

3. Update `docs/SKY_DESIGN_SYSTEM.md` if it introduces new design tokens or patterns

4. Reference it in the Detekt ComponentReuse rule (so duplicates are flagged)

---

## See Also

- **Design System:** `docs/SKY_DESIGN_SYSTEM.md` — Colors, spacing, motion, principles
- **Enforcement:** `docs/DESIGN_SYSTEM_ENFORCEMENT.md` — How violations are caught
- **Sky Design Language:** `docs/sky-design-language.md` — Research & principles
