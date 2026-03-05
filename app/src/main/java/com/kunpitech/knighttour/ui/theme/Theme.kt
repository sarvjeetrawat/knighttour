package com.kunpitech.knighttour.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════════
//  KNIGHT TOUR — MATERIAL3 COLOR SCHEME
// ═══════════════════════════════════════════════════════════════

private val KnightTourDarkColorScheme = darkColorScheme(
    primary              = md_primary,
    onPrimary            = md_onPrimary,
    primaryContainer     = md_primaryContainer,
    onPrimaryContainer   = md_onPrimaryContainer,

    secondary            = md_secondary,
    onSecondary          = md_onSecondary,
    secondaryContainer   = md_secondaryContainer,
    onSecondaryContainer = md_onSecondaryContainer,

    tertiary             = md_tertiary,
    onTertiary           = md_onTertiary,
    tertiaryContainer    = md_tertiaryContainer,
    onTertiaryContainer  = md_onTertiaryContainer,

    error                = md_error,
    onError              = md_onError,
    errorContainer       = md_errorContainer,
    onErrorContainer     = md_onErrorContainer,

    background           = md_background,
    onBackground         = md_onBackground,

    surface              = md_surface,
    onSurface            = md_onSurface,
    surfaceVariant       = md_surfaceVariant,
    onSurfaceVariant     = md_onSurfaceVariant,

    outline              = md_outline,
    outlineVariant       = md_outlineVariant,

    inverseSurface       = md_inverseSurface,
    inverseOnSurface     = md_inverseOnSurface,
    inversePrimary       = md_inversePrimary,

    surfaceTint          = md_surfaceTint,
    scrim                = md_scrim,
)

// ═══════════════════════════════════════════════════════════════
//  EXTENDED COLORS — game-specific tokens not in Material3
//  Access via: MaterialTheme.extendedColors.cellKnight
// ═══════════════════════════════════════════════════════════════

data class KnightTourExtendedColors(
    // Board cells
    val cellLight         : Color,
    val cellDark          : Color,
    val cellVisited       : Color,
    val cellVisitedBorder : Color,
    val cellKnight        : Color,
    val cellValidMove     : Color,
    val cellHint          : Color,

    // Path & trail
    val pathTrail         : Color,
    val pathNode          : Color,

    // Game-specific status
    val victoryGreen      : Color,
    val victoryGreenDim   : Color,
    val devilPurple       : Color,
    val devilPurpleDim    : Color,
    val onlineTeal        : Color,
    val onlineTealDim     : Color,
    val warningAmber      : Color,

    // Gold variants
    val knightGold        : Color,
    val crownGold         : Color,
    val paleGold          : Color,
    val goldGlow          : Color,
    val goldDim           : Color,

    // Red variants
    val devilRed          : Color,
    val bloodRedBright    : Color,
    val redGlow           : Color,
    val redDim            : Color,

    // Surfaces
    val abyssBlack        : Color,
    val surfaceElevated   : Color,
    val surfaceHighest    : Color,
    val borderSubtle      : Color,
    val borderDefault     : Color,
    val borderStrong      : Color,

    // Text
    val textPrimary       : Color,
    val textSecondary     : Color,
    val textTertiary      : Color,
    val textInverse       : Color,

    // Overlay
    val scrim             : Color,
    val scanLine          : Color,
)

private val KnightTourExtendedColorsDark = KnightTourExtendedColors(
    cellLight          = CellLight,
    cellDark           = CellDark,
    cellVisited        = CellVisited,
    cellVisitedBorder  = CellVisitedBorder,
    cellKnight         = CellKnight,
    cellValidMove      = CellValidMove,
    cellHint           = CellHint,

    pathTrail          = PathTrail,
    pathNode           = PathNode,

    victoryGreen       = VictoryGreen,
    victoryGreenDim    = VictoryGreenDim,
    devilPurple        = DevilPurple,
    devilPurpleDim     = DevilPurpleDim,
    onlineTeal         = OnlineTeal,
    onlineTealDim      = OnlineTealDim,
    warningAmber       = WarningAmber,

    knightGold         = KnightGold,
    crownGold          = CrownGold,
    paleGold           = PaleGold,
    goldGlow           = GoldGlow,
    goldDim            = GoldDim,

    devilRed           = DevilRed,
    bloodRedBright     = BloodRedBright,
    redGlow            = RedGlow,
    redDim             = RedDim,

    abyssBlack         = AbyssBlack,
    surfaceElevated    = SurfaceElevated,
    surfaceHighest     = SurfaceHighest,
    borderSubtle       = BorderSubtle,
    borderDefault      = BorderDefault,
    borderStrong       = BorderStrong,

    textPrimary        = TextPrimary,
    textSecondary      = TextSecondary,
    textTertiary       = TextTertiary,
    textInverse        = TextInverse,

    scrim              = Scrim,
    scanLine           = ScanLine,
)

// ── CompositionLocal for extended colors ────────────────────────

val LocalExtendedColors = staticCompositionLocalOf {
    KnightTourExtendedColorsDark
}

// ── Convenience extension on MaterialTheme ───────────────────────
val MaterialTheme.extendedColors: KnightTourExtendedColors
    @Composable get() = LocalExtendedColors.current

// ═══════════════════════════════════════════════════════════════
//  KNIGHT TOUR — EXTENDED TYPOGRAPHY LOCAL
//  Access via: MaterialTheme.knightType.GameTitle
// ═══════════════════════════════════════════════════════════════

val LocalKnightType = staticCompositionLocalOf { KnightTourType }

val MaterialTheme.knightType: KnightTourType
    @Composable get() = LocalKnightType.current

// ═══════════════════════════════════════════════════════════════
//  MAIN THEME COMPOSABLE
// ═══════════════════════════════════════════════════════════════

/**
 * KnightTourTheme
 *
 * Usage:
 * ```kotlin
 * KnightTourTheme {
 *     // Your composables here
 *     // Access colors:  MaterialTheme.colorScheme.primary
 *     //                 MaterialTheme.extendedColors.cellKnight
 *     // Access type:    MaterialTheme.knightType.GameTitle
 *     //                 MaterialTheme.typography.headlineLarge
 * }
 * ```
 *
 * @param darkTheme       Force dark theme (default: system preference — always dark for this game)
 * @param dynamicColor    Use Material You dynamic color on Android 12+ (disabled by default to preserve brand)
 * @param content         Your composable content
 */
@Composable
fun KnightTourTheme(
    darkTheme    : Boolean = true,            // Game is always dark
    dynamicColor : Boolean = false,           // Keep brand gold — disable dynamic
    content      : @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic color available on Android 12+ but disabled for brand consistency
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // Always use our dark color scheme
        else -> KnightTourDarkColorScheme
    }

    // Make status bar transparent + use light icons on dark bg
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Transparent status + navigation bar
            WindowCompat.setDecorFitsSystemWindows(window, false)

            @Suppress("DEPRECATION")
            window.statusBarColor     = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()

            // Light icons (white) on our dark background
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalExtendedColors provides KnightTourExtendedColorsDark,
        LocalKnightType     provides KnightTourType,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = KnightTourTypography,
            shapes      = KnightTourShapes,
            content     = content,
        )
    }
}