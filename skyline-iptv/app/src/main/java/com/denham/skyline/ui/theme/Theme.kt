package com.denham.skyline.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.denham.skyline.R

/**
 * Sky-inspired, not Sky-cloned. Accent colours come from Sky's published UI
 * tokens (interaction-focus #007ECC, brand #000FF5, surface-blue #0C1B87);
 * the dark navy canvas is our own inference — Sky doesn't publish dark
 * background hexes. Typeface is Manrope (OFL), not the proprietary Sky Text.
 */
object SkyPalette {
    // Tokens matched to the Sky Go mock: black canvas, blue-grey cards,
    // action blue, red LIVE badge.
    //
    // These are the only hardcoded colours allowed in the app; the
    // SkyPaletteUsage detekt rule fails the build on any other Color(0x...)
    // literal. When adding a token here, add it to TOKENS in
    // detekt-rules/.../SkyPaletteUsageRule.kt too, so the rule can name it as
    // the fix instead of falling back to a generic message.
    val Canvas = Color(0xFF05070A)        // near-black background
    val Surface = Color(0xFF0E1520)       // cards, sheets
    val SurfaceElevated = Color(0xFF16233A)
    val Accent = Color(0xFF0B69F5)        // action blue (buttons/chips/nav)
    val AccentBright = Color(0xFF2A9BE0)  // hover/focus
    val Brand = Color(0xFF000FF5)         // electric brand blue — sparingly
    val Indigo = Color(0xFF0C1B87)        // hero depth gradient
    val LiveRed = Color(0xFFE11D3F)       // LIVE badge
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8FA0B5)
    val TextMuted = Color(0xFF7C8899)     // metadata/captions tier
    val Error = Color(0xFFFF6B6B)

    /** Bottom scrim used over hero artwork. */
    val heroScrim = Brush.verticalGradient(
        0f to Color.Transparent,
        0.55f to Canvas.copy(alpha = 0.55f),
        1f to Canvas,
    )

    /** Indigo-to-navy backdrop behind the hero when no artwork is available. */
    val heroFallback = Brush.verticalGradient(
        0f to Indigo,
        1f to Canvas,
    )

    /** Subtle navy → near-black wash behind the ten-foot (TV) screens, matching
     *  the Sky Q mock's gently graded background rather than a flat black. */
    val tvBackground = Brush.verticalGradient(
        0f to Color(0xFF0A111F),
        0.6f to Color(0xFF070A12),
        1f to Canvas,
    )

    /** The blue underline beneath the active TV nav item — a soft gradient, not
     *  a flat bar. */
    val navUnderline = Brush.horizontalGradient(
        listOf(AccentBright, Accent, Brand),
    )
}

/** Corner radii per the mock: rounded, consistent, never sharp. */
object SkyRadius {
    val card = 16.dp
    val hero = 22.dp
    val chip = 8.dp
    val sheet = 28.dp
}

/** 8pt spacing grid; `gutter` is the screen's horizontal padding. */
object SkySpacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val gutter = 16.dp
}

private val SkyColorScheme = darkColorScheme(
    primary = SkyPalette.Accent,
    onPrimary = Color.White,
    primaryContainer = SkyPalette.Indigo,
    onPrimaryContainer = Color.White,
    secondary = SkyPalette.AccentBright,
    onSecondary = Color.White,
    tertiary = SkyPalette.Brand,
    onTertiary = Color.White,
    background = SkyPalette.Canvas,
    onBackground = SkyPalette.TextPrimary,
    surface = SkyPalette.Surface,
    onSurface = SkyPalette.TextPrimary,
    surfaceVariant = SkyPalette.SurfaceElevated,
    onSurfaceVariant = SkyPalette.TextSecondary,
    surfaceContainer = SkyPalette.Surface,
    surfaceContainerHigh = SkyPalette.SurfaceElevated,
    surfaceContainerHighest = SkyPalette.SurfaceElevated,
    error = SkyPalette.Error,
    onError = Color.White,
    outline = SkyPalette.TextSecondary.copy(alpha = 0.4f),
)

/** Manrope variable font — weights via font-variation settings (API 26+). */
@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: FontWeight) = Font(
    resId = R.font.manrope,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Manrope = FontFamily(
    manrope(FontWeight.Normal),
    manrope(FontWeight.Medium),
    manrope(FontWeight.SemiBold),
    manrope(FontWeight.Bold),
    manrope(FontWeight.ExtraBold),
)

// Proportions mirror Sky's published type scale: bold display 24–56px with
// slight negative tracking; 16–20px body with generous line height.
private val SkyTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.02).em,
    ),
    displayMedium = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.02).em,
    ),
    headlineLarge = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.01).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.01).em,
    ),
    headlineSmall = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 15.sp,
    ),
)

@Composable
fun SkylineTheme(content: @Composable () -> Unit) {
    // Always dark: the Sky Glass look is a dark-canvas experience.
    MaterialTheme(
        colorScheme = SkyColorScheme,
        typography = SkyTypography,
        content = content,
    )
}
